package com.stubdeck.controller;

import com.stubdeck.config.WireMockProperties;
import com.stubdeck.model.ChangeAction;
import com.stubdeck.model.WireMockMapping;
import com.stubdeck.model.changes.PendingChange;
import com.stubdeck.model.pagination.MappingQuery;
import com.stubdeck.model.pagination.PageResult;
import com.stubdeck.security.RequireEdit;
import com.stubdeck.service.ConflictDetector;
import com.stubdeck.service.MappingService;
import com.stubdeck.service.context.user.UserContext;
import com.stubdeck.service.changes.ChangeTracker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import tools.jackson.core.JacksonException;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.stubdeck.constants.MappingConstants.*;
import static com.stubdeck.support.PaginationUtils.createPageNumbers;

@Controller
@RequiredArgsConstructor
public class MappingViewController {

    private final WireMockProperties properties;
    private final MappingService mappingService;
    private final ChangeTracker changeTracker;
    private final ConflictDetector conflictDetector;
    private final UserContext userContext;

    @GetMapping("/")
    public String index(Model model) {
        List<WireMockMapping> mappings = mappingService.getAllMappings();
        Map<String, List<WireMockMapping>> grouped = conflictDetector.groupAll(mappings);
        Map<String, List<WireMockMapping>> conflicts = conflictDetector.findConflicts(mappings);
        Map<String, PendingChange> locks = changeTracker.getSnapshot();

        Set<String> existingIds = mappings.stream().map(WireMockMapping::getId).collect(Collectors.toSet());
        long orphanedPendingCount = locks.values().stream()
                .filter(p -> p.getAction() != ChangeAction.DELETE)
                .filter(p -> !existingIds.contains(p.getId()))
                .count();

        model.addAttribute(TOTAL_COUNT, mappings.size());
        model.addAttribute(CONFLICT_COUNT, conflicts.size());
        model.addAttribute(ENDPOINT_COUNT, grouped.size());
        model.addAttribute(ORPHANED_PENDING_COUNT, orphanedPendingCount);
        return "index";
    }

    @GetMapping("/mappings")
    public String mappings(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String method,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model
    ) {
        MappingQuery query = new MappingQuery(search, method, page, size);

        PageResult<WireMockMapping> result = mappingService.findMappings(query);

        Map<String, PendingChange> locks = changeTracker.getSnapshot();
        String currentUsername = userContext.getCurrentUsername();

        Set<String> lockedByOther = locks.entrySet().stream()
                .filter(e -> !Objects.equals(e.getValue().getChangedBy(), currentUsername))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        model.addAttribute(MAPPINGS, result.content());
        model.addAttribute(PAGE, result);
        model.addAttribute(SEARCH, search);
        model.addAttribute(METHOD, method);
        model.addAttribute(PAGE_NUMBERS, createPageNumbers(result.page(), result.totalPages()));
        model.addAttribute(LOCKS, locks);
        model.addAttribute(LOCKED_BY_OTHER, lockedByOther);
        return "fragments/mappings :: mappingBlock";
    }

    @GetMapping("/conflicts")
    public String conflicts(Model model) {
        List<WireMockMapping> mappings = mappingService.getAllMappings();
        Map<String, List<WireMockMapping>> conflicts = conflictDetector.findConflicts(mappings);
        model.addAttribute(CONFLICTS, conflicts);
        return "conflicts";
    }

    @RequireEdit
    @GetMapping("/mappings/new")
    public String newMapping(Model model) {
        model.addAttribute(MAPPING, new WireMockMapping());
        model.addAttribute(MAPPING_JSON, null);
        model.addAttribute(CONFLICT_STRATEGY, properties.getConflict().getStrategy());
        return "editor";
    }

    @GetMapping("/mappings/{id}")
    public String viewMapping(@PathVariable("id") String id, Model model) throws JacksonException {
        WireMockMapping mapping = mappingService.getMappingById(id)
                .orElseThrow(() -> new IllegalArgumentException("Not found: " + id));

        Optional<PendingChange> pending = changeTracker.get(id);
        boolean canEdit = pending.isEmpty()
                || pending.get().getChangedBy().equals(userContext.getCurrentUsername());

        model.addAttribute(MAPPING, mapping);
        model.addAttribute(MAPPING_JSON, mappingService.toRawJson(mapping));
        model.addAttribute(PENDING, pending.orElse(null));
        model.addAttribute(CAN_EDIT, canEdit);
        return "view";
    }

    @RequireEdit
    @GetMapping("/mappings/{id}/edit")
    public String editMapping(@PathVariable("id") String id, Model model,
                              RedirectAttributes redirectAttrs) throws JacksonException {

        WireMockMapping mapping = mappingService.getMappingById(id)
                .orElseThrow(() -> new IllegalArgumentException("Not found: " + id));

        if (changeTracker.isLockedByOther(id, userContext.getCurrentUsername())) {
            PendingChange lock = changeTracker.get(id).orElseThrow();
            redirectAttrs.addFlashAttribute(LOCK_ERROR,
                    "This stub is currently being edited by " + lock.getChangedByDisplay());
            return "redirect:/mappings/" + id;
        }

        model.addAttribute(MAPPING, mapping);
        model.addAttribute(MAPPING_JSON, mappingService.toRawJson(mapping));
        model.addAttribute(CONFLICT_STRATEGY, properties.getConflict().getStrategy());
        return "editor";
    }
}
