package com.stubdeck.service.source;

import com.stubdeck.config.SourceType;
import com.stubdeck.config.WireMockProperties;
import com.stubdeck.model.WireMockMapping;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static com.stubdeck.support.CoreUtils.nullOrBlank;

@Slf4j
@RequiredArgsConstructor
public class FileMappingSource implements MappingSource {

    private final WireMockProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public List<WireMockMapping> loadAll() {
        Path dir = Path.of(properties.getMappings().getPath());

        if (!Files.exists(dir)) {
            log.warn("Mappings directory not found: {}", dir.toAbsolutePath());
            return Collections.emptyList();
        }

        try (Stream<Path> files = Files.walk(dir)) {
            return files
                    .filter(p -> p.toString().endsWith(".json"))
                    .map(this::parseFile)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();
        } catch (IOException e) {
            log.error("Failed to read mappings directory: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public Optional<WireMockMapping> load(String id) {
        // Fast path: mappings saved through this app always use {id}.json as filename
        Path direct = Path.of(properties.getMappings().getPath()).resolve(id + ".json");
        if (Files.exists(direct)) {
            Optional<WireMockMapping> found = parseFile(direct);
            if (found.isPresent() && id.equals(found.get().getId())) return found;
        }
        // Fallback: linear scan handles files with non-standard names (placed directly on disk)
        return loadAll().stream().filter(m -> id.equals(m.getId())).findFirst();
    }

    @Override
    public void save(WireMockMapping mapping) {
        if (nullOrBlank(mapping.getId())) {
            mapping.setId(UUID.randomUUID().toString());
        }

        if (nullOrBlank(mapping.getFileName())) {
            mapping.setFileName(mapping.getId() + ".json");
        }

        Path dir = Path.of(properties.getMappings().getPath());
        Path target = dir.resolve(mapping.getFileName());
        Path tmp = dir.resolve(mapping.getFileName() + ".tmp");

        try {
            objectMapper.writeValue(tmp.toFile(), mapping);
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            log.info("Saved mapping: {}", mapping.getFileName());
        } catch (IOException e) {
            log.error("Failed to save {}: {}", mapping.getFileName(), e.getMessage());
            try {
                Files.deleteIfExists(tmp);
            } catch (IOException ignored) {
            }
            throw new RuntimeException("Failed to save mapping: " + mapping.getFileName(), e);
        }
    }

    @Override
    public void delete(String id) {
        WireMockMapping target = load(id)
                .orElseThrow(() -> new IllegalArgumentException("Mapping not found: " + id));

        Path file = Path.of(properties.getMappings().getPath()).resolve(target.getFileName());
        try {
            Files.deleteIfExists(file);
            log.info("Deleted mapping: {}", target.getFileName());
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete: " + target.getFileName(), e);
        }
    }

    @Override
    public SourceType getSourceType() {
        return SourceType.FILE;
    }

    private Optional<WireMockMapping> parseFile(Path file) {
        try {
            WireMockMapping mapping = objectMapper.readValue(file.toFile(), WireMockMapping.class);
            mapping.setFileName(file.getFileName().toString());
            return Optional.of(mapping);
        } catch (JacksonException e) {
            log.warn("Skipping invalid mapping file: {} - {}", file.getFileName(), e.getMessage());
            return Optional.empty();
        }
    }
}
