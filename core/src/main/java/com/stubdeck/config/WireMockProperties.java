package com.stubdeck.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static com.stubdeck.support.CoreUtils.prepareUrl;

@ConfigurationProperties(prefix = "wiremock")
@Data
@Component
public class WireMockProperties {

    private Mappings mappings = new Mappings();
    private Conflict conflict = new Conflict();
    private Changelog changelog = new Changelog();
    private Source source = new Source();
    private Admin admin = new Admin();

    @Data
    public static class Mappings {
        private String path = "./mappings";
    }

    @Data
    public static class Conflict {
        private ConflictStrategy strategy = ConflictStrategy.WARN;
    }

    @Data
    public static class Changelog {
        private String path = "./changelog.log";
    }

    @Data
    public static class Source {
        private SourceType type = SourceType.FILE;
    }

    @Data
    public static class Admin {
        private String scheme = "http";
        private String host = "localhost";
        private int port = 8080;
        private String username;
        private String password;
        private int connectTimeoutSeconds = 5;
        private int readTimeoutSeconds = 30;
        private int apiLimit = 10_000;

        public String getBaseUrl() {
            return prepareUrl(scheme, host, port);
        }
    }
}
