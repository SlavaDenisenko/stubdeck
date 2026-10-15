package com.stubdeck.service;

import com.stubdeck.config.ConflictStrategy;
import com.stubdeck.config.SourceType;
import com.stubdeck.config.WireMockProperties;
import com.stubdeck.model.ChangeAction;
import com.stubdeck.model.SaveResult;
import com.stubdeck.model.WireMockMapping;
import com.stubdeck.model.auth.AppUser;
import com.stubdeck.service.changes.ChangeLogService;
import com.stubdeck.service.changes.ChangeTracker;
import com.stubdeck.service.context.user.UserContext;
import com.stubdeck.service.source.MappingSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.stubdeck.support.TestUtils.mapping;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MappingServiceTest {

    @Mock
    MappingSource mappingSource;

    @Mock
    ChangeLogService changeLogService;

    @Mock
    ChangeTracker tracker;

    @Mock
    ConflictDetector conflictDetector;

    @Mock
    UserContext userContext;

    WireMockProperties properties;
    MappingService service;

    @BeforeEach
    void setUp() {
        properties = new WireMockProperties();
        service = new MappingService(properties, mappingSource, changeLogService,
                tracker, conflictDetector, userContext, new ObjectMapper());
    }

    @Test
    void save_newMapping_noConflict_returnsOk() {
        WireMockMapping mapping = mapping(null, "GET", "/api/v1");
        when(mappingSource.loadAll()).thenReturn(List.of());
        when(conflictDetector.findConflicts(any())).thenReturn(Map.of());
        when(conflictDetector.buildKeyPublic(any())).thenReturn("GET /api/v1");
        when(userContext.getCurrentUser()).thenReturn(Optional.of(user("alice")));
        when(mappingSource.getSourceType()).thenReturn(SourceType.FILE);

        SaveResult result = service.save(mapping, false);

        assertThat(result.saved()).isTrue();
        assertThat(result.hasConflict()).isFalse();
        verify(mappingSource).save(mapping);
        verify(tracker).record(eq(mapping), isNull(), eq(ChangeAction.CREATE), any());
    }

    @Test
    void save_existingMapping_logsUpdate() {
        WireMockMapping existing = mapping("id-1", "GET", "/api/v1");
        WireMockMapping updated = mapping("id-1", "GET", "/api/v1");
        when(mappingSource.load("id-1")).thenReturn(Optional.of(existing));
        when(mappingSource.loadAll()).thenReturn(List.of(existing));
        when(conflictDetector.findConflicts(any())).thenReturn(Map.of());
        when(conflictDetector.buildKeyPublic(any())).thenReturn("GET /api/v1");
        when(userContext.getCurrentUser()).thenReturn(Optional.of(user("bob")));
        when(mappingSource.getSourceType()).thenReturn(SourceType.FILE);

        SaveResult result = service.save(updated, false);

        assertThat(result.saved()).isTrue();
        verify(tracker).record(eq(updated), eq(existing), eq(ChangeAction.UPDATE), any());
        verify(changeLogService).log(eq(ChangeAction.UPDATE), any());
    }

    @Test
    void save_conflict_strategyBlock_returnsConflict() {
        properties.getConflict().setStrategy(ConflictStrategy.BLOCK);
        WireMockMapping mapping = mapping(null, "GET", "/api/v1");
        WireMockMapping other = mapping("other-id", "GET", "/api/v1");
        when(mappingSource.loadAll()).thenReturn(List.of(other));
        when(conflictDetector.findConflicts(any())).thenReturn(Map.of("GET /api/v1", List.of(other, mapping)));
        when(conflictDetector.buildKeyPublic(any())).thenReturn("GET /api/v1");

        SaveResult result = service.save(mapping, false);

        assertThat(result.saved()).isFalse();
        assertThat(result.hasConflict()).isTrue();
        verify(mappingSource, never()).save(any());
    }

    @Test
    void save_conflict_strategyWarn_forceTrue_saves() {
        properties.getConflict().setStrategy(ConflictStrategy.WARN);
        WireMockMapping mapping = mapping(null, "GET", "/api/v1");
        WireMockMapping other = mapping("other-id", "GET", "/api/v1");
        when(mappingSource.loadAll()).thenReturn(List.of(other));
        when(conflictDetector.findConflicts(any())).thenReturn(Map.of("GET /api/v1", List.of(other, mapping)));
        when(conflictDetector.buildKeyPublic(any())).thenReturn("GET /api/v1");
        when(userContext.getCurrentUser()).thenReturn(Optional.of(user("alice")));
        when(mappingSource.getSourceType()).thenReturn(SourceType.FILE);

        SaveResult result = service.save(mapping, true);

        assertThat(result.saved()).isTrue();
        assertThat(result.hasConflict()).isTrue();
        verify(mappingSource).save(mapping);
    }

    @Test
    void delete_existingMapping_callsSourceAndTracker() throws IOException {
        WireMockMapping mapping = mapping("del-1", "DELETE", "/api/v1");
        when(mappingSource.load("del-1")).thenReturn(Optional.of(mapping));
        when(userContext.getCurrentUser()).thenReturn(Optional.of(user("carol")));
        when(mappingSource.getSourceType()).thenReturn(SourceType.FILE);

        service.delete("del-1");

        verify(mappingSource).delete("del-1");
        verify(tracker).record(isNull(), eq(mapping), eq(ChangeAction.DELETE), any());
        verify(changeLogService).log(eq(ChangeAction.DELETE), any());
    }

    @Test
    void delete_notFound_throws() {
        when(mappingSource.load("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete("missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void getSourceType_delegatesToSource() {
        when(mappingSource.getSourceType()).thenReturn(SourceType.API);
        assertThat(service.getSourceType()).isEqualTo("API");
    }

    private AppUser user(String username) {
        return AppUser.builder().username(username).displayName(username).build();
    }
}
