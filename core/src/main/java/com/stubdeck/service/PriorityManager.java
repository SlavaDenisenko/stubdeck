package com.stubdeck.service;

import com.stubdeck.model.PriorityBatchItem;
import com.stubdeck.model.WireMockMapping;
import com.stubdeck.service.source.MappingSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PriorityManager {

    private final MappingSource mappingSource;

    public void updatePriorities(List<String> orderedIds) {
        Map<String, WireMockMapping> byId = mappingSource.loadAll().stream()
                .filter(m -> m.getId() != null)
                .collect(Collectors.toMap(WireMockMapping::getId, m -> m));

        for (int i = 0; i < orderedIds.size(); i++) {
            if (!byId.containsKey(orderedIds.get(i)))
                continue;

            WireMockMapping mapping = byId.get(orderedIds.get(i));
            mapping.setPriority(i + 1);
            mappingSource.save(mapping);
        }
    }

    public void updateBatch(List<PriorityBatchItem> items) {
        Map<String, WireMockMapping> byId = mappingSource.loadAll().stream()
                .filter(m -> m.getId() != null)
                .collect(Collectors.toMap(WireMockMapping::getId, m -> m));

        items.forEach(item -> {
            if (!byId.containsKey(item.getId()))
                return;

            WireMockMapping mapping = byId.get(item.getId());
            mapping.setPriority(item.getPriority());
            mappingSource.save(mapping);
        });
    }
}
