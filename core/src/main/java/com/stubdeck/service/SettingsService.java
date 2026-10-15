package com.stubdeck.service;

import com.stubdeck.config.SourceType;
import com.stubdeck.config.WireMockProperties;
import com.stubdeck.model.request.SettingsRequest;
import com.stubdeck.model.response.SettingsResponse;
import com.stubdeck.service.source.ApiMappingSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import static com.stubdeck.support.CoreUtils.nullOrBlank;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private final WireMockProperties properties;
    private final RestClient wireMockRestClient;

    public SettingsResponse testConnection(String url) {
        try {
            wireMockRestClient.get()
                    .uri(url + ApiMappingSource.MAPPINGS_PATH + "?limit=1")
                    .retrieve()
                    .toBodilessEntity();
            return SettingsResponse.success("WireMock Admin API responded at " + url);
        } catch (Exception e) {
            return SettingsResponse.error(e.getMessage());
        }
    }

    public boolean testAdminApi() {
        return testConnection(properties.getAdmin().getBaseUrl()).ok();
    }

    public SettingsResponse saveSourceType(String sourceType) {
        try {
            SourceType type = SourceType.valueOf(sourceType);
            properties.getSource().setType(type);
            return SettingsResponse.success("Source type set to " + type.name() + ". Restart required to take full effect.");
        } catch (IllegalArgumentException e) {
            return SettingsResponse.error("Unknown source type: " + sourceType);
        }
    }

    public SettingsResponse saveSettings(SettingsRequest req) {
        try {
            WireMockProperties.Admin admin = properties.getAdmin();
            if (!nullOrBlank(req.getScheme())) admin.setScheme(req.getScheme());
            if (!nullOrBlank(req.getHost())) admin.setHost(req.getHost());
            admin.setPort(req.getPort());
            if (!nullOrBlank(req.getUsername())) admin.setUsername(req.getUsername());
            if (!nullOrBlank(req.getPassword())) admin.setPassword(req.getPassword());
            return SettingsResponse.success("Settings saved. Changes apply immediately to runtime only.");
        } catch (Exception e) {
            return SettingsResponse.error(e.getMessage());
        }
    }
}
