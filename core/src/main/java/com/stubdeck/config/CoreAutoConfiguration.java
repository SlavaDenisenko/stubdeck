package com.stubdeck.config;

import com.stubdeck.service.access.AccessGuard;
import com.stubdeck.service.access.OpenAccessGuard;
import com.stubdeck.service.changes.ChangeTracker;
import com.stubdeck.service.changes.DefaultChangeTracker;
import com.stubdeck.service.source.ApiMappingSource;
import com.stubdeck.service.source.FileMappingSource;
import com.stubdeck.service.source.MappingSource;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

@AutoConfiguration
@RequiredArgsConstructor
public class CoreAutoConfiguration {

    private final WireMockProperties properties;

    @Bean("accessGuard")
    @ConditionalOnMissingBean(name = "accessGuard")
    public AccessGuard openAccessGuard() {
        return new OpenAccessGuard();
    }

    @Bean
    @ConditionalOnMissingBean(ChangeTracker.class)
    public ChangeTracker defaultChangeTracker() {
        return new DefaultChangeTracker();
    }

    @Bean
    @ConditionalOnMissingBean(MappingSource.class)
    public MappingSource mappingSource(ObjectMapper objectMapper, RestClient wireMockRestClient) {
        return switch (properties.getSource().getType()) {
            case FILE -> new FileMappingSource(properties, objectMapper);
            case API -> new ApiMappingSource(properties, wireMockRestClient);
        };
    }
}
