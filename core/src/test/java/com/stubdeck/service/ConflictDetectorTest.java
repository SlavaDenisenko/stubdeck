package com.stubdeck.service;

import com.stubdeck.model.RequestPattern;
import com.stubdeck.model.WireMockMapping;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class ConflictDetectorTest {

    @Autowired
    private ConflictDetector conflictDetector;

    @Test
    void shouldDetectConflicts() {
        List<WireMockMapping> mappings = List.of(
                mapping("GET", "/api/users", null, 1),
                mapping("GET", "/api/users", null, 2),
                mapping("POST", "/api/users", null, 1)
        );

        Map<String, List<WireMockMapping>> conflicts = conflictDetector.findConflicts(mappings);

        assertThat(conflicts).hasSize(1);
        assertThat(conflicts).containsKey("GET /api/users");
        assertThat(conflicts.get("GET /api/users")).hasSize(2);
    }

    @Test
    void shouldNotDetectConflictsForDifferentMethods() {
        List<WireMockMapping> mappings = List.of(
                mapping("GET", "/api/users", null, 1),
                mapping("POST", "/api/users", null, 1)
        );

        Map<String, List<WireMockMapping>> conflicts = conflictDetector.findConflicts(mappings);

        assertThat(conflicts).isEmpty();
    }

    @Test
    void shouldHandleRegexUrls() {
        List<WireMockMapping> mappings = List.of(
                mapping("GET", null, "/api/users/.*", 1),
                mapping("GET", null, "/api/users/.*", 2)
        );

        Map<String, List<WireMockMapping>> conflicts = conflictDetector.findConflicts(mappings);

        assertThat(conflicts).hasSize(1);
        assertThat(conflicts).containsKey("GET ~/api/users/.*");
    }

    private WireMockMapping mapping(String method, String url, String urlPattern, int priority) {
        RequestPattern request = new RequestPattern();
        request.setMethod(method);
        request.setUrl(url);
        request.setUrlPattern(urlPattern);

        WireMockMapping mapping = new WireMockMapping();
        mapping.setRequest(request);
        mapping.setPriority(priority);
        return mapping;
    }
}
