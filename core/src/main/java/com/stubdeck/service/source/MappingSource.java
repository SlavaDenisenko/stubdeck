package com.stubdeck.service.source;

import com.stubdeck.config.SourceType;
import com.stubdeck.model.WireMockMapping;

import java.util.List;
import java.util.Optional;

public interface MappingSource {
    List<WireMockMapping> loadAll();

    Optional<WireMockMapping> load(String id);

    void save(WireMockMapping mapping);

    void delete(String id);

    SourceType getSourceType();

    default void persist() {
        // no-op for FILE source
    }
}
