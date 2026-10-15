package com.stubdeck.service;

import com.stubdeck.model.WireMockMapping;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = "wiremock.source.type=FILE")
public class PriorityManagerTest {

    @Autowired
    private PriorityManager priorityManager;

    @Autowired
    private MappingService mappingService;

    @TempDir
    private static Path TEMP_DIR;

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("wiremock.mappings.path", TEMP_DIR::toString);
    }

    @Test
    void shouldUpdatePrioritiesAndPersist() throws IOException {
        copyTestMapping(TEMP_DIR, "mapping-a.json", "id-001");
        copyTestMapping(TEMP_DIR, "mapping-b.json", "id-002");

        priorityManager.updatePriorities(List.of("id-002", "id-001"));

        List<WireMockMapping> updated = mappingService.getAllMappings();
        Map<String, Integer> priorities = updated.stream()
                .collect(Collectors.toMap(WireMockMapping::getId, WireMockMapping::getPriority));

        assertThat(priorities.get("id-002")).isEqualTo(1);
        assertThat(priorities.get("id-001")).isEqualTo(2);
    }

    private void copyTestMapping(Path dir, String fileName, String id) throws IOException {
        String json = """
                {
                  "id": "%s",
                  "name": "Test mapping",
                  "request": { "method": "GET", "url": "/api/test" },
                  "response": { "status": 200 }
                }
                """.formatted(id);
        Files.writeString(dir.resolve(fileName), json);
    }
}
