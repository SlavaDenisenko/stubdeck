package com.stubdeck.service;

import com.stubdeck.model.ChangeAction;
import com.stubdeck.model.RequestPattern;
import com.stubdeck.model.WireMockMapping;
import com.stubdeck.model.auth.AppUser;
import com.stubdeck.model.changes.PendingChange;
import com.stubdeck.service.changes.ChangeLogService;
import com.stubdeck.service.context.user.UserContext;
import com.stubdeck.service.context.instance.InstanceContext;
import com.stubdeck.service.source.MappingSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PendingChangeTrackerTest {

    @TempDir
    Path tempDir;

    @Mock
    MappingSource mappingSource;
    @Mock
    ChangeLogService changeLogService;
    @Mock
    UserContext userContext;
    @Mock
    InstanceContext instanceContext;

    PendingChangeTracker tracker;

    @BeforeEach
    void setUp() {
        lenient().when(instanceContext.getCurrentInstanceId()).thenReturn("default");
        tracker = new PendingChangeTracker(mappingSource, changeLogService, userContext, instanceContext,
                new ObjectMapper(), tempDir.resolve("pending.json").toString());
    }

    @Test
    void isAvailable_returnsTrue() {
        assertThat(tracker.isAvailable()).isTrue();
    }

    @Test
    void record_create_storesPendingChange() {
        WireMockMapping mapping = mapping("stub-1", "GET", "/api/users");
        AppUser user = user("alice");

        tracker.record(mapping, null, ChangeAction.CREATE, user);

        Optional<PendingChange> change = tracker.get("stub-1");
        assertThat(change).isPresent();
        assertThat(change.get().getAction()).isEqualTo(ChangeAction.CREATE);
        assertThat(change.get().getChangedBy()).isEqualTo("alice");
        assertThat(change.get().getPreviousSnapshot()).isNull();
        assertThat(change.get().getCurrentSnapshot()).isEqualTo(mapping);
    }

    @Test
    void record_delete_storesPreviousSnapshot() {
        WireMockMapping mapping = mapping("stub-2", "POST", "/api/orders");
        AppUser user = user("bob");

        tracker.record(null, mapping, ChangeAction.DELETE, user);

        Optional<PendingChange> change = tracker.get("stub-2");
        assertThat(change).isPresent();
        assertThat(change.get().getAction()).isEqualTo(ChangeAction.DELETE);
        assertThat(change.get().getCurrentSnapshot()).isNull();
        assertThat(change.get().getPreviousSnapshot()).isEqualTo(mapping);
    }

    @Test
    void getAllOrdered_returnsNewestFirst() {
        AppUser user = user("alice");
        tracker.record(mapping("id-1", "GET", "/first"), null, ChangeAction.CREATE, user);
        tracker.record(mapping("id-2", "GET", "/second"), null, ChangeAction.CREATE, user);
        tracker.record(mapping("id-3", "GET", "/third"), null, ChangeAction.CREATE, user);

        List<PendingChange> ordered = tracker.getAllOrdered();
        assertThat(ordered).hasSize(3);
        assertThat(ordered.get(0).getId()).isEqualTo("id-3");
        assertThat(ordered.get(2).getId()).isEqualTo("id-1");
    }

    @Test
    void isLockedByOther_ownerNotLocked() {
        tracker.record(mapping("stub-1", "GET", "/url"), null, ChangeAction.CREATE, user("alice"));
        assertThat(tracker.isLockedByOther("stub-1", "alice")).isFalse();
    }

    @Test
    void isLockedByOther_otherUserIsLocked() {
        tracker.record(mapping("stub-1", "GET", "/url"), null, ChangeAction.CREATE, user("alice"));
        assertThat(tracker.isLockedByOther("stub-1", "bob")).isTrue();
    }

    @Test
    void isLockedByOther_unknownId_notLocked() {
        assertThat(tracker.isLockedByOther("nonexistent", "alice")).isFalse();
    }

    @Test
    void discardSelected_ownerCanDiscard_create() {
        WireMockMapping mapping = mapping("stub-1", "GET", "/url");
        tracker.record(mapping, null, ChangeAction.CREATE, user("alice"));
        when(userContext.getCurrentUsername()).thenReturn("alice");

        tracker.discardSelected(List.of("stub-1"));

        assertThat(tracker.get("stub-1")).isEmpty();
        verify(mappingSource).delete("stub-1");
    }

    @Test
    void discardSelected_ownerCanDiscard_update() {
        WireMockMapping previous = mapping("stub-1", "GET", "/url");
        WireMockMapping current = mapping("stub-1", "GET", "/url-updated");
        tracker.record(current, previous, ChangeAction.UPDATE, user("alice"));
        when(userContext.getCurrentUsername()).thenReturn("alice");

        tracker.discardSelected(List.of("stub-1"));

        assertThat(tracker.get("stub-1")).isEmpty();
        verify(mappingSource).save(previous);
    }

    @Test
    void discardSelected_nonOwnerCannotDiscard() {
        WireMockMapping mapping = mapping("stub-1", "GET", "/url");
        tracker.record(mapping, null, ChangeAction.CREATE, user("alice"));
        when(userContext.getCurrentUsername()).thenReturn("bob");

        tracker.discardSelected(List.of("stub-1"));

        assertThat(tracker.get("stub-1")).isPresent();
        verify(mappingSource, never()).delete(any());
    }

    @Test
    void untrack_removesEntry() {
        tracker.record(mapping("stub-1", "GET", "/url"), null, ChangeAction.CREATE, user("alice"));
        tracker.untrack("stub-1");
        assertThat(tracker.get("stub-1")).isEmpty();
    }

    @Test
    void persistAll_logsAndClears() {
        AppUser user = user("alice");
        tracker.record(mapping("id-1", "GET", "/one"), null, ChangeAction.CREATE, user);
        tracker.record(mapping("id-2", "POST", "/two"), null, ChangeAction.CREATE, user);

        tracker.persistAll();

        assertThat(tracker.getSnapshot()).isEmpty();
        verify(mappingSource).persist();
        verify(changeLogService, times(2)).log(any(), any());
    }

    @Test
    void discardSelected_delete_restoresPreviousSnapshot() {
        WireMockMapping previous = mapping("stub-1", "GET", "/original");
        tracker.record(null, previous, ChangeAction.DELETE, user("alice"));
        when(userContext.getCurrentUsername()).thenReturn("alice");

        tracker.discardSelected(List.of("stub-1"));

        assertThat(tracker.get("stub-1")).isEmpty();
        verify(mappingSource).save(previous);
    }

    @Test
    void persistSelected_logsAndRemovesSelected() {
        AppUser user = user("alice");
        tracker.record(mapping("id-1", "GET", "/one"), null, ChangeAction.CREATE, user);
        tracker.record(mapping("id-2", "POST", "/two"), null, ChangeAction.CREATE, user);

        tracker.persistSelected(List.of("id-1"));

        assertThat(tracker.get("id-1")).isEmpty();
        assertThat(tracker.get("id-2")).isPresent();
        verify(changeLogService, times(1)).log(any(), any());
    }

    @Test
    void persistSelected_unknownId_ignoredSilently() {
        tracker.persistSelected(List.of("nonexistent"));
        verify(changeLogService, never()).log(any(), any());
    }

    @Test
    void clear_removesAllEntriesWithoutReverting() {
        AppUser user = user("alice");
        tracker.record(mapping("id-1", "GET", "/one"), null, ChangeAction.CREATE, user);
        tracker.record(mapping("id-2", "GET", "/two"), null, ChangeAction.CREATE, user);

        tracker.clear();

        assertThat(tracker.getSnapshot()).isEmpty();
        verify(mappingSource, never()).delete(any());
        verify(mappingSource, never()).save(any());
    }

    @Test
    void statePersistedToDisk_andReloadable() {
        tracker.record(mapping("stub-1", "GET", "/url"), null, ChangeAction.CREATE, user("alice"));

        PendingChangeTracker reloaded = new PendingChangeTracker(
                mappingSource, changeLogService, userContext, instanceContext,
                new ObjectMapper(), tempDir.resolve("pending.json").toString());
        reloaded.load();

        assertThat(reloaded.get("stub-1")).isPresent();
        assertThat(reloaded.get("stub-1").get().getChangedBy()).isEqualTo("alice");
    }

    private WireMockMapping mapping(String id, String method, String url) {
        RequestPattern req = new RequestPattern();
        req.setMethod(method);
        req.setUrl(url);
        WireMockMapping m = new WireMockMapping();
        m.setId(id);
        m.setFileName(id + ".json");
        m.setRequest(req);
        return m;
    }

    private AppUser user(String username) {
        return AppUser.builder().username(username).displayName(username).build();
    }
}
