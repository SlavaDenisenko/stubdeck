package com.stubdeck.service;

import com.stubdeck.model.WireMockMapping;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = "wiremock.source.type=FILE")
public class FileMappingSourceTest {

    @Autowired
    private MappingService mappingService;

    @Nested
    @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
    class ValidTests {

        @DynamicPropertySource
        static void dynamicProperties(DynamicPropertyRegistry registry) {
            registry.add("wiremock.mappings.path", () -> Objects.requireNonNull(ValidTests.class.getClassLoader().getResource("test-mappings")).getPath());
        }

        @Test
        void shouldLoadValidMappings() {
            List<WireMockMapping> mappings = mappingService.getAllMappings();

            assertThat(mappings).hasSize(1);
            assertThat(mappings.getFirst().getId()).isEqualTo("abc-123");
            assertThat(mappings.getFirst().getFileName()).isEqualTo("valid-mapping.json");
        }
    }

    @Nested
    @TestPropertySource(properties = "wiremock.mappings.path=/nonexistent/path")
    @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
    class InvalidTests {

        @Test
        void shouldReturnEmptyListWhenDirectoryNotFound() {
            List<WireMockMapping> mappings = mappingService.getAllMappings();
            assertThat(mappings).isEmpty();
        }
    }
}
