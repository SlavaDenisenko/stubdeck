package com.stubdeck.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import static com.stubdeck.support.CoreUtils.nullOrBlank;

@Configuration
public class MappingSourceConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .build();
    }

    @Bean
    public RestClient wireMockRestClient(WireMockProperties properties) {
        WireMockProperties.Admin admin = properties.getAdmin();

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(admin.getConnectTimeoutSeconds()));
        factory.setReadTimeout(Duration.ofSeconds(admin.getReadTimeoutSeconds()));

        RestClient.Builder builder = RestClient.builder()
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        if (!nullOrBlank(admin.getUsername())) {
            String credentials = admin.getUsername() + ":"
                    + (admin.getPassword() != null ? admin.getPassword() : "");
            String encoded = Base64.getEncoder()
                    .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoded);
        }

        return builder.build();
    }
}
