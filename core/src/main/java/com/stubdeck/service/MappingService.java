package com.stubdeck.service;

import com.stubdeck.config.ConflictStrategy;
import com.stubdeck.config.SourceType;
import com.stubdeck.config.WireMockProperties;
import com.stubdeck.model.ChangeAction;
import com.stubdeck.model.SaveResult;
import com.stubdeck.model.WireMockMapping;
import com.stubdeck.model.auth.AppUser;
import com.stubdeck.model.pagination.MappingQuery;
import com.stubdeck.model.pagination.PageResult;
import com.stubdeck.service.changes.ChangeLogService;
import com.stubdeck.service.changes.ChangeTracker;
import com.stubdeck.service.context.user.UserContext;
import com.stubdeck.service.source.MappingSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

import static com.stubdeck.support.CoreUtils.*;
import static com.stubdeck.support.PaginationUtils.paginate;

@Service
@Slf4j
@RequiredArgsConstructor
public class MappingService {

    private final WireMockProperties properties;
    private final MappingSource mappingSource;
    private final ChangeLogService changeLogService;
    private final ChangeTracker tracker;
    private final ConflictDetector conflictDetector;
    private final UserContext userContext;
    private final ObjectMapper objectMapper;

    public PageResult<WireMockMapping> findMappings(MappingQuery query) {
        List<WireMockMapping> mappings = mappingSource.loadAll();

        String search = normalize(query.search());
        String method = normalize(query.method());

        List<WireMockMapping> filtered = mappings.stream()
                .filter(mapping -> matchesSearch(mapping, search))
                .filter(mapping -> matchesMethod(mapping, method))
                .sorted(Comparator.comparing(
                        WireMockMapping::getEffectiveUrl,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                ))
                .toList();

        return paginate(filtered, query.page(), query.size());
    }

    public List<WireMockMapping> getAllMappings() {
        return mappingSource.loadAll();
    }

    public Optional<WireMockMapping> getMappingById(String id) {
        return mappingSource.load(id);
    }

    public SaveResult save(WireMockMapping mapping, boolean forceOnConflict) {
        boolean isNew = nullOrBlank(mapping.getId());

        // Load previous snapshot before overwriting (needed for discard support)
        WireMockMapping previous = isNew ? null : mappingSource.load(mapping.getId()).orElse(null);
        if (previous == null) isNew = true;

        String conflictKey = detectConflict(mapping);
        ConflictStrategy strategy = properties.getConflict().getStrategy();
        if (conflictKey != null) {
            if (strategy == ConflictStrategy.BLOCK) return SaveResult.conflict(conflictKey);
            if (strategy == ConflictStrategy.WARN && !forceOnConflict) return SaveResult.conflict(conflictKey);
        }

        try {
            ChangeAction action = isNew ? ChangeAction.CREATE : ChangeAction.UPDATE;
            mappingSource.save(mapping);
            AppUser user = currentUser();
            tracker.record(mapping, previous, action, user);

            // For FILE source the save is already on disk — log immediately
            if (mappingSource.getSourceType() == SourceType.FILE) {
                changeLogService.log(action, mapping);
            }

            return conflictKey != null ? SaveResult.warnAndSave(conflictKey) : SaveResult.ok();
        } catch (Exception e) {
            return SaveResult.error(e.getMessage());
        }
    }

    public void delete(String id) throws IOException {
        WireMockMapping target = mappingSource.load(id)
                .orElseThrow(() -> new IllegalArgumentException("Mapping not found: " + id));

        mappingSource.delete(id);
        AppUser user = currentUser();
        tracker.record(null, target, ChangeAction.DELETE, user);

        // For FILE source the file is already gone — log immediately
        if (mappingSource.getSourceType() == SourceType.FILE) {
            changeLogService.log(ChangeAction.DELETE, target);
        }

        log.info("Deleted mapping: {}", target.getFileName());
    }

    public WireMockMapping parseRawJson(String json) throws IOException {
        return objectMapper.readValue(json, WireMockMapping.class);
    }

    public String toRawJson(WireMockMapping mapping) {
        return objectMapper.writeValueAsString(mapping);
    }

    public String getSourceType() {
        return mappingSource.getSourceType().name();
    }

    public void persist() {
        mappingSource.persist();
    }

    private AppUser currentUser() {
        return userContext.getCurrentUser().orElseGet(() ->
                AppUser.builder().username("anonymous").displayName("Anonymous").build());
    }

    private String detectConflict(WireMockMapping incoming) {
        List<WireMockMapping> existing = mappingSource.loadAll().stream()
                .filter(m -> !Objects.equals(m.getId(), incoming.getId()))
                .toList();

        Map<String, List<WireMockMapping>> conflicts = conflictDetector.findConflicts(
                Stream.concat(existing.stream(), Stream.of(incoming)).toList()
        );

        String incomingKey = conflictDetector.buildKeyPublic(incoming);
        return conflicts.containsKey(incomingKey) ? incomingKey : null;
    }

    private static boolean matchesSearch(WireMockMapping mapping, String search) {
        if (search == null) return true;
        return containsIgnoreCase(mapping.getEffectiveUrl(), search);
    }

    private static boolean matchesMethod(WireMockMapping mapping, String method) {
        if (method == null) return true;
        return containsIgnoreCase(mapping.getMethod(), method);
    }
}
