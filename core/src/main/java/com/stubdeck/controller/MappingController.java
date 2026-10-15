package com.stubdeck.controller;

import com.stubdeck.config.WireMockProperties;
import com.stubdeck.model.*;
import com.stubdeck.model.changes.PendingChange;
import com.stubdeck.model.response.DeleteResponse;
import com.stubdeck.model.response.ReconcileResponse;
import com.stubdeck.model.response.SaveResponse;
import com.stubdeck.model.response.TemplateResponse;
import com.stubdeck.security.RequireDelete;
import com.stubdeck.security.RequireEdit;
import com.stubdeck.security.RequirePersist;
import com.stubdeck.service.ConflictDetector;
import com.stubdeck.service.MappingService;
import com.stubdeck.service.PriorityManager;
import com.stubdeck.service.changes.ChangeTracker;
import com.stubdeck.service.context.user.UserContext;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/mappings")
@RequiredArgsConstructor
@Slf4j
public class MappingController {

    private final WireMockProperties properties;
    private final MappingService mappingService;
    private final ConflictDetector conflictDetector;
    private final PriorityManager priorityManager;
    private final ChangeTracker tracker;
    private final UserContext userContext;

    @GetMapping
    public ResponseEntity<@NonNull List<WireMockMapping>> getAllMappings() {
        List<WireMockMapping> mappings = mappingService.getAllMappings();
        log.debug("Returning {} mapping(s)", mappings.size());
        return ResponseEntity.ok(mappings);
    }

    @GetMapping("/conflicts")
    public ResponseEntity<@NonNull Map<String, List<WireMockMapping>>> getConflicts() {
        List<WireMockMapping> mappings = mappingService.getAllMappings();
        Map<String, List<WireMockMapping>> conflicts = conflictDetector.findConflicts(mappings);
        log.debug("Found {} conflict groups", conflicts.size());
        return ResponseEntity.ok(conflicts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<@NonNull WireMockMapping> getById(@PathVariable("id") String id) {
        Optional<WireMockMapping> mapping = mappingService.getMappingById(id);
        return mapping.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @RequireEdit
    @PostMapping("/priority")
    public ResponseEntity<@NonNull Void> updatePriority(@RequestBody PriorityUpdateRequest request) {
        priorityManager.updatePriorities(request.getOrderedIds());
        return ResponseEntity.ok().build();
    }

    @RequireEdit
    @PostMapping("/priority/batch")
    public ResponseEntity<@NonNull Void> updatePriorityBatch(@RequestBody List<PriorityBatchItem> items) {
        priorityManager.updateBatch(items);
        return ResponseEntity.ok().build();
    }

    @RequireEdit
    @PostMapping
    public ResponseEntity<@NonNull SaveResponse> create(
            @RequestBody WireMockMapping mapping,
            @RequestParam(defaultValue = "false") boolean force
    ) {
        return handleSave(mapping, force);
    }

    @RequireEdit
    @PutMapping("/{id}/edit")
    public ResponseEntity<@NonNull SaveResponse> update(
            @PathVariable("id") String id,
            @RequestBody WireMockMapping mapping,
            @RequestParam(defaultValue = "false") boolean force
    ) {
        mapping.setId(id);
        return handleSave(mapping, force);
    }

    @RequireDelete
    @DeleteMapping("/{id}")
    public ResponseEntity<@NonNull DeleteResponse> delete(@PathVariable("id") String id) {
        if (tracker.isLockedByOther(id, userContext.getCurrentUsername())) {
            PendingChange lock = tracker.get(id).orElseThrow();
            return ResponseEntity.status(409).body(DeleteResponse.builder()
                    .error("locked")
                    .lockedBy(lock.getChangedByDisplay() != null ? lock.getChangedByDisplay() : lock.getChangedBy())
                    .lockedByEmail(lock.getChangedByEmail() != null ? lock.getChangedByEmail() : "")
                    .build()
            );
        }
        try {
            tracker.untrack(id);
            mappingService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/parse")
    public ResponseEntity<@NonNull WireMockMapping> parseJson(@RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(mappingService.parseRawJson(body.get("json")));
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/templates")
    public ResponseEntity<@NonNull List<TemplateResponse>> getTemplates() {
        List<TemplateResponse> templates = Arrays.stream(MappingTemplate.values())
                .map(t -> TemplateResponse.builder()
                        .id(t.name())
                        .label(t.label)
                        .description(t.description)
                        .json(t.json)
                        .build()
                )
                .toList();
        return ResponseEntity.ok(templates);
    }

    @RequirePersist
    @PostMapping("/persist")
    public ResponseEntity<@NonNull Void> persist() {
        mappingService.persist();
        return ResponseEntity.ok().build();
    }

    @RequireEdit
    @PostMapping("/reconcile")
    public ResponseEntity<@NonNull ReconcileResponse> reconcile() {
        Set<String> existingIds = mappingService.getAllMappings().stream()
                .map(WireMockMapping::getId)
                .collect(Collectors.toSet());
        Map<String, PendingChange> pending = tracker.getSnapshot();
        int removed = 0;
        for (PendingChange p : pending.values()) {
            if (p.getAction() != ChangeAction.DELETE && !existingIds.contains(p.getId())) {
                tracker.untrack(p.getId());
                removed++;
            }
        }
        return ResponseEntity.ok(new ReconcileResponse(removed));
    }

    private ResponseEntity<@NonNull SaveResponse> handleSave(WireMockMapping mapping, boolean force) {
        SaveResult result = mappingService.save(mapping, force);
        SaveResponse response = SaveResponse.builder()
                .saved(result.saved())
                .hasConflict(result.hasConflict())
                .conflictKey(result.conflictKey())
                .error(result.error())
                .strategy(properties.getConflict().getStrategy().name())
                .build();

        return result.error() != null
                ? ResponseEntity.internalServerError().body(response)
                : ResponseEntity.ok(response);
    }
}
