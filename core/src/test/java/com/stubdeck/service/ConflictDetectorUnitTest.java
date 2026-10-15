package com.stubdeck.service;

import com.stubdeck.model.RequestPattern;
import com.stubdeck.model.WireMockMapping;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.stubdeck.support.TestUtils.mapping;
import static org.assertj.core.api.Assertions.assertThat;

class ConflictDetectorUnitTest {

    private final ConflictDetector detector = new ConflictDetector();

    @Test
    void findConflicts_singleMapping_noConflicts() {
        List<WireMockMapping> mappings = List.of(mapping("id-1", "GET", "/api/users"));

        Map<String, List<WireMockMapping>> conflicts = detector.findConflicts(mappings);

        assertThat(conflicts).isEmpty();
    }

    @Test
    void findConflicts_duplicateExactUrl_detectedAsConflict() {
        List<WireMockMapping> mappings = List.of(
                mapping("id-1", "GET", "/api/orders"),
                mapping("id-2", "GET", "/api/orders")
        );

        Map<String, List<WireMockMapping>> conflicts = detector.findConflicts(mappings);

        assertThat(conflicts).containsKey("GET /api/orders");
        assertThat(conflicts.get("GET /api/orders")).hasSize(2);
    }

    @Test
    void findConflicts_differentMethods_noConflict() {
        List<WireMockMapping> mappings = List.of(
                mapping("id-1", "GET", "/api/resource"),
                mapping("id-2", "DELETE", "/api/resource")
        );

        assertThat(detector.findConflicts(mappings)).isEmpty();
    }

    @Test
    void findConflicts_mappingWithNullRequest_filteredOut() {
        WireMockMapping withoutRequest = new WireMockMapping();
        withoutRequest.setId("no-req");

        List<WireMockMapping> mappings = List.of(withoutRequest, mapping("id-1", "GET", "/api/resource"));

        Map<String, List<WireMockMapping>> conflicts = detector.findConflicts(mappings);

        assertThat(conflicts).isEmpty();
    }

    @Test
    void groupAll_groupsByMethodAndUrl() {
        List<WireMockMapping> mappings = List.of(
                mapping("id-1", "GET", "/api/a"),
                mapping("id-2", "GET", "/api/a"),
                mapping("id-3", "POST", "/api/a")
        );

        Map<String, List<WireMockMapping>> groups = detector.groupAll(mappings);

        assertThat(groups).containsKey("GET /api/a");
        assertThat(groups.get("GET /api/a")).hasSize(2);
        assertThat(groups).containsKey("POST /api/a");
        assertThat(groups.get("POST /api/a")).hasSize(1);
    }

    @Test
    void groupAll_nullMethod_defaultsToAny() {
        RequestPattern req = new RequestPattern();
        req.setUrl("/api/resource");
        WireMockMapping m = new WireMockMapping();
        m.setRequest(req);

        Map<String, List<WireMockMapping>> groups = detector.groupAll(List.of(m));

        assertThat(groups).containsKey("ANY /api/resource");
    }

    @Test
    void buildKeyPublic_urlPattern_prefixedWithTilde() {
        WireMockMapping m = mapping("GET", "/api/users/.*");

        assertThat(detector.buildKeyPublic(m)).isEqualTo("GET ~/api/users/.*");
    }

    @Test
    void buildKeyPublic_exactUrl_noPrefix() {
        WireMockMapping m = mapping("id-1", "POST", "/api/data");

        assertThat(detector.buildKeyPublic(m)).isEqualTo("POST /api/data");
    }
}
