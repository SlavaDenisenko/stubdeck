package com.stubdeck.controller;

import com.stubdeck.security.RequireDiscard;
import com.stubdeck.security.RequirePersist;
import com.stubdeck.service.PendingChangeTracker;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/changes")
@RequiredArgsConstructor
public class ChangesApiController {

    private final PendingChangeTracker tracker;

    @RequirePersist
    @PostMapping("/save-selected")
    public ResponseEntity<@NonNull Void> saveSelected(@RequestBody List<String> ids) {
        tracker.persistSelected(ids);
        return ResponseEntity.ok().build();
    }

    @RequirePersist
    @PostMapping("/save-all")
    public ResponseEntity<@NonNull Void> saveAll() {
        tracker.persistAll();
        return ResponseEntity.ok().build();
    }

    @RequireDiscard
    @PostMapping("/discard-selected")
    public ResponseEntity<@NonNull Void> discardSelected(@RequestBody List<String> ids) {
        tracker.discardSelected(ids);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<@NonNull Void> untrack(@PathVariable("id") String id) {
        tracker.untrack(id);
        return ResponseEntity.noContent().build();
    }
}
