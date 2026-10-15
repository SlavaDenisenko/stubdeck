package com.stubdeck.service.changes;

import com.stubdeck.model.ChangeAction;
import com.stubdeck.model.WireMockMapping;
import com.stubdeck.model.auth.AppUser;
import com.stubdeck.model.changes.PendingChange;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ChangeTracker {

    void record(WireMockMapping current, WireMockMapping previous, ChangeAction action, AppUser user);

    Optional<PendingChange> get(String id);

    Map<String, PendingChange> getSnapshot();

    default List<PendingChange> getAllOrdered() {
        return List.of();
    }

    boolean isLockedByOther(String id, String currentUsername);

    void untrack(String id);

    default boolean isAvailable() {
        return false;
    }
}
