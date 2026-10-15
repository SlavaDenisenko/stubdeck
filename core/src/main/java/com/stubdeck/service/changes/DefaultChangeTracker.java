package com.stubdeck.service.changes;

import com.stubdeck.model.ChangeAction;
import com.stubdeck.model.WireMockMapping;
import com.stubdeck.model.auth.AppUser;
import com.stubdeck.model.changes.PendingChange;

import java.util.Map;
import java.util.Optional;

public class DefaultChangeTracker implements ChangeTracker {

    @Override
    public void record(WireMockMapping current, WireMockMapping previous, ChangeAction action, AppUser user) {
    }

    @Override
    public Optional<PendingChange> get(String id) {
        return Optional.empty();
    }

    @Override
    public Map<String, PendingChange> getSnapshot() {
        return Map.of();
    }

    @Override
    public boolean isLockedByOther(String id, String currentUsername) {
        return false;
    }

    @Override
    public void untrack(String id) {
    }
}
