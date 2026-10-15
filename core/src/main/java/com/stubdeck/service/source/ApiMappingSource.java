package com.stubdeck.service.source;

import com.stubdeck.config.SourceType;
import com.stubdeck.config.WireMockProperties;
import com.stubdeck.model.WireMockAdminResponse;
import com.stubdeck.model.WireMockMapping;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.stubdeck.support.CoreUtils.nullOrBlank;

@Slf4j
@RequiredArgsConstructor
public class ApiMappingSource implements MappingSource {

    private final WireMockProperties properties;
    private final RestClient restClient;

    private static final int PAGE_SIZE = 200;
    public static final String MAPPINGS_PATH = "/__admin/mappings";

    private String baseUrl() {
        return properties.getAdmin().getBaseUrl();
    }

    @Override
    public List<WireMockMapping> loadAll() {
        try {
            List<WireMockMapping> result = new ArrayList<>();
            int offset = 0;

            while (true) {
                WireMockAdminResponse page = restClient.get()
                        .uri(baseUrl() + MAPPINGS_PATH + "?limit=" + PAGE_SIZE + "&offset=" + offset)
                        .retrieve()
                        .body(WireMockAdminResponse.class);

                if (page == null || page.getMappings() == null || page.getMappings().isEmpty()) break;

                result.addAll(page.getMappings());

                Integer total = page.getMeta() != null ? page.getMeta().getTotal() : null;
                if (total != null && result.size() >= total) break;
                if (page.getMappings().size() < PAGE_SIZE) break;

                offset += PAGE_SIZE;

                if (result.size() >= properties.getAdmin().getApiLimit()) {
                    log.warn("Reached apiLimit of {} — some mappings may be omitted", properties.getAdmin().getApiLimit());
                    break;
                }
            }

            return result;
        } catch (Exception e) {
            log.error("Failed to load mappings from Admin API: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public Optional<WireMockMapping> load(String id) {
        try {
            WireMockMapping response = restClient.get()
                    .uri(baseUrl() + MAPPINGS_PATH + "/" + id)
                    .retrieve()
                    .body(WireMockMapping.class);
            return Optional.ofNullable(response);
        } catch (Exception e) {
            log.error("Failed to load mapping from Admin API: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void save(WireMockMapping mapping) {
        try {
            if (nullOrBlank(mapping.getId())) {
                restClient.post()
                        .uri(baseUrl() + MAPPINGS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapping)
                        .retrieve()
                        .toBodilessEntity();
            } else {
                restClient.put()
                        .uri(baseUrl() + MAPPINGS_PATH + "/" + mapping.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapping)
                        .retrieve()
                        .toBodilessEntity();
            }
            log.info("Saved mapping via Admin API: {}", mapping.getId());
        } catch (Exception e) {
            log.error("Failed to save mapping via Admin API: {}", e.getMessage());
            throw new RuntimeException("Admin API save failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String id) {
        try {
            restClient.delete()
                    .uri(baseUrl() + MAPPINGS_PATH + "/" + id)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Deleted mapping via Admin API: {}", id);
        } catch (Exception e) {
            log.error("Failed to delete mapping via Admin API: {}", e.getMessage());
            throw new RuntimeException("Admin API delete failed: " + e.getMessage(), e);
        }
    }

    @Override
    public SourceType getSourceType() {
        return SourceType.API;
    }

    @Override
    public void persist() {
        try {
            restClient.post()
                    .uri(baseUrl() + MAPPINGS_PATH + "/save")
                    .retrieve()
                    .toBodilessEntity();
            log.info("Mappings persisted to disk");
        } catch (Exception e) {
            log.error("Failed to persist: {}", e.getMessage());
            throw new RuntimeException("Persist failed: " + e.getMessage(), e);
        }
    }
}
