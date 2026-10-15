package com.stubdeck.service;

import com.stubdeck.config.WireMockProperties;
import com.stubdeck.model.ChangeAction;
import com.stubdeck.model.ChangelogEntry;
import com.stubdeck.model.WireMockMapping;
import com.stubdeck.service.changes.ChangeLogService;
import com.stubdeck.service.context.user.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.List;

import static com.stubdeck.support.TestUtils.mapping;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChangeLogServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    UserContext userContext;

    ChangeLogService service;

    @BeforeEach
    void setUp() {
        WireMockProperties props = new WireMockProperties();
        props.getChangelog().setPath(tempDir.resolve("changelog.log").toString());
        service = new ChangeLogService(props, userContext);
    }

    @Test
    void readChangelog_emptyWhenFileAbsent() {
        List<ChangelogEntry> entries = service.readChangelog();
        assertThat(entries).isEmpty();
    }

    @Test
    void log_and_readChangelog_singleEntry() {
        when(userContext.getCurrentUsername()).thenReturn("alice");

        WireMockMapping mapping = mapping("stub-1", "GET", "/api/users");
        service.log(ChangeAction.CREATE, mapping);

        List<ChangelogEntry> entries = service.readChangelog();
        assertThat(entries).hasSize(1);
        assertThat(entries.getFirst().action()).isEqualTo("CREATE");
        assertThat(entries.getFirst().id()).isEqualTo("stub-1");
        assertThat(entries.getFirst().username()).isEqualTo("alice");
        assertThat(entries.getFirst().endpoint()).contains("GET /api/users");
    }

    @Test
    void log_multiple_readReturnsMostRecentFirst() {
        when(userContext.getCurrentUsername()).thenReturn("bob");

        service.log(ChangeAction.CREATE, mapping("id-1", "GET", "/first"));
        service.log(ChangeAction.UPDATE, mapping("id-2", "POST", "/second"));

        List<ChangelogEntry> entries = service.readChangelog();
        assertThat(entries).hasSize(2);
        assertThat(entries.getFirst().id()).isEqualTo("id-2");
        assertThat(entries.get(1).id()).isEqualTo("id-1");
    }

    @Test
    void log_mappingWithNoRequest_writesUnknownEndpoint() {
        when(userContext.getCurrentUsername()).thenReturn("system");
        WireMockMapping mapping = new WireMockMapping();
        mapping.setId("no-req");
        mapping.setFileName("no-req.json");

        service.log(ChangeAction.DELETE, mapping);

        List<ChangelogEntry> entries = service.readChangelog();
        assertThat(entries).hasSize(1);
        assertThat(entries.getFirst().endpoint()).isEqualTo("unknown");
    }
}
