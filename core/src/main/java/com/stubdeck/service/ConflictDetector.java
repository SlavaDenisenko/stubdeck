package com.stubdeck.service;

import com.stubdeck.model.WireMockMapping;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ConflictDetector {

    public Map<String, List<WireMockMapping>> findConflicts(List<WireMockMapping> mappings) {
        return groupAll(mappings)
                .entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public Map<String, List<WireMockMapping>> groupAll(List<WireMockMapping> mappings) {
        return mappings.stream()
                .filter(m -> m.getRequest() != null)
                .collect(Collectors.groupingBy(this::buildKey));
    }

    public String buildKeyPublic(WireMockMapping mapping) {
        return buildKey(mapping);
    }

    private String buildKey(WireMockMapping mapping) {
        String method = Optional.ofNullable(mapping.getRequest().getMethod())
                .orElse("ANY")
                .toUpperCase();
        String url = mapping.getRequest().getEffectiveUrl();
        return method + " " + url;
    }
}
