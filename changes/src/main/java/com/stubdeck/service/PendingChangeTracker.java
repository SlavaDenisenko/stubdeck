package com.stubdeck.service;

import com.stubdeck.model.ChangeAction;
import com.stubdeck.model.WireMockMapping;
import com.stubdeck.model.auth.AppUser;
import com.stubdeck.model.changes.PendingChange;
import com.stubdeck.service.changes.ChangeLogService;
import com.stubdeck.service.changes.ChangeTracker;
import com.stubdeck.service.context.user.UserContext;
import com.stubdeck.service.context.instance.InstanceContext;
import com.stubdeck.service.source.MappingSource;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Tracks pending changes (WireMock memory vs disk) scoped per-instance.
 * A pending change IS the lock - whoever last saved a stub is the owner
 * until persist or discard. State survives restarts via a JSON file.
 */
@Service
@Slf4j
public class PendingChangeTracker implements ChangeTracker {

    private final MappingSource mappingSource;
    private final ChangeLogService changeLogService;
    private final UserContext userContext;
    private final InstanceContext instanceContext;
    private final ObjectMapper objectMapper;
    private final File stateFile;

    private final Map<String, LinkedHashMap<String, PendingChange>> pendingByInstance = new LinkedHashMap<>();
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = rwLock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = rwLock.writeLock();

    private final ExecutorService diskWriter = Executors.newSingleThreadExecutor(
            Thread.ofVirtual().name("pending-flush").factory());

    public PendingChangeTracker(MappingSource mappingSource,
                                ChangeLogService changeLogService,
                                UserContext userContext,
                                InstanceContext instanceContext,
                                ObjectMapper objectMapper,
                                @Value("${wiremock.pending.path:./pending-changes.json}") String path) {
        this.mappingSource = mappingSource;
        this.changeLogService = changeLogService;
        this.userContext = userContext;
        this.instanceContext = instanceContext;
        this.objectMapper = objectMapper;
        this.stateFile = new File(path);
    }

    @PostConstruct
    void load() {
        log.info("Pending changes file: {}", stateFile.getAbsolutePath());
        if (!stateFile.exists()) return;
        try {
            String raw = Files.readString(stateFile.toPath()).stripLeading();
            if (raw.startsWith("[")) {
                // Migrate legacy flat array to "default" instance
                List<PendingChange> list = objectMapper.readValue(stateFile, new TypeReference<>() {
                });
                LinkedHashMap<String, PendingChange> map = new LinkedHashMap<>();
                list.forEach(c -> map.put(c.getId(), c));
                pendingByInstance.put("default", map);
                log.info("Migrated {} legacy pending change(s) to 'default' instance", list.size());
            } else {
                Map<String, List<PendingChange>> byInstance = objectMapper.readValue(
                        stateFile, new TypeReference<>() {
                        });
                byInstance.forEach((instanceId, list) -> {
                    LinkedHashMap<String, PendingChange> map = new LinkedHashMap<>();
                    list.forEach(c -> map.put(c.getId(), c));
                    pendingByInstance.put(instanceId, map);
                });
                int total = pendingByInstance.values().stream().mapToInt(Map::size).sum();
                log.info("Loaded {} pending change(s) across {} instance(s)", total, pendingByInstance.size());
            }
        } catch (Exception e) {
            log.warn("Failed to load pending changes: {}", e.getMessage());
        }
    }

    @PreDestroy
    void shutdown() {
        diskWriter.shutdown();
        try {
            if (!diskWriter.awaitTermination(10, TimeUnit.SECONDS)) {
                log.warn("Disk writer did not finish within 10 seconds during shutdown");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private LinkedHashMap<String, PendingChange> currentPending() {
        String instanceId = instanceContext.getCurrentInstanceId();
        return pendingByInstance.computeIfAbsent(instanceId, _ -> new LinkedHashMap<>());
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void record(WireMockMapping current, WireMockMapping previous, ChangeAction action, AppUser user) {
        writeLock.lock();
        try {
            WireMockMapping ref = current != null ? current : previous;
            String id = ref.getId();
            String display = user.getDisplayName() != null ? user.getDisplayName() : user.getUsername();
            String label = ref.getName() != null ? ref.getName()
                    : (ref.getRequest() != null ? ref.getRequest().getEffectiveUrl() : id);
            String method = ref.getRequest() != null ? ref.getRequest().getMethod() : null;
            String url = ref.getRequest() != null ? ref.getRequest().getEffectiveUrl() : null;

            currentPending().put(id, new PendingChange(
                    id, label, method, url, action,
                    previous,
                    action == ChangeAction.DELETE ? null : current,
                    user.getUsername(), display, user.getEmail(),
                    Instant.now().toString()
            ));
            scheduleFlush();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Optional<PendingChange> get(String id) {
        readLock.lock();
        try {
            return Optional.ofNullable(currentPending().get(id));
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Map<String, PendingChange> getSnapshot() {
        readLock.lock();
        try {
            return Map.copyOf(currentPending());
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public List<PendingChange> getAllOrdered() {
        readLock.lock();
        try {
            List<PendingChange> list = new ArrayList<>(currentPending().values());
            Collections.reverse(list);
            return list;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean isLockedByOther(String id, String currentUsername) {
        readLock.lock();
        try {
            PendingChange c = currentPending().get(id);
            return c != null && !c.getChangedBy().equals(currentUsername);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void untrack(String id) {
        writeLock.lock();
        try {
            if (currentPending().remove(id) != null) scheduleFlush();
        } finally {
            writeLock.unlock();
        }
    }

    public void discardSelected(List<String> ids) {
        String currentUser = userContext.getCurrentUsername();
        writeLock.lock();
        try {
            LinkedHashMap<String, PendingChange> pending = currentPending();
            for (String id : ids) {
                PendingChange change = pending.get(id);
                if (change == null) continue;
                if (!change.getChangedBy().equals(currentUser)) {
                    log.warn("User '{}' attempted to discard change owned by '{}' for stub {} - denied",
                            currentUser, change.getChangedBy(), id);
                    continue;
                }
                try {
                    switch (change.getAction()) {
                        case CREATE -> mappingSource.delete(id);
                        case UPDATE, DELETE -> {
                            if (change.getPreviousSnapshot() != null) {
                                mappingSource.save(change.getPreviousSnapshot());
                            }
                        }
                    }
                    pending.remove(id);
                    log.info("Discarded {} change for stub {}", change.getAction(), id);
                } catch (Exception e) {
                    log.error("Failed to discard {} {}: {}", change.getAction(), id, e.getMessage());
                }
            }
            scheduleFlush();
        } finally {
            writeLock.unlock();
        }
    }

    public void persistSelected(List<String> ids) {
        writeLock.lock();
        try {
            LinkedHashMap<String, PendingChange> pending = currentPending();
            for (String id : ids) {
                PendingChange change = pending.get(id);
                if (change == null) continue;
                logChange(change);
                pending.remove(id);
            }
            scheduleFlush();
        } finally {
            writeLock.unlock();
        }
    }

    public void persistAll() {
        writeLock.lock();
        try {
            LinkedHashMap<String, PendingChange> pending = currentPending();
            if (!pending.isEmpty()) {
                mappingSource.persist();
                pending.values().forEach(this::logChange);
                pending.clear();
                scheduleFlush();
            }
        } finally {
            writeLock.unlock();
        }
    }

    public void clear() {
        writeLock.lock();
        try {
            currentPending().clear();
            scheduleFlush();
        } finally {
            writeLock.unlock();
        }
    }

    private void scheduleFlush() {
        Map<String, List<PendingChange>> snapshot = new LinkedHashMap<>();
        pendingByInstance.forEach((instanceId, map) -> {
            if (!map.isEmpty()) snapshot.put(instanceId, new ArrayList<>(map.values()));
        });
        diskWriter.submit(() -> writeToDisk(snapshot));
    }

    private void writeToDisk(Map<String, List<PendingChange>> data) {
        try {
            stateFile.getParentFile().mkdirs();
            objectMapper.writeValue(stateFile, data);
        } catch (Exception e) {
            log.error("Failed to flush pending changes to {}: {}", stateFile.getAbsolutePath(), e.getMessage());
        }
    }

    private void logChange(PendingChange change) {
        WireMockMapping mapping = change.getCurrentSnapshot() != null
                ? change.getCurrentSnapshot() : change.getPreviousSnapshot();
        if (mapping != null) changeLogService.log(change.getAction(), mapping);
    }
}
