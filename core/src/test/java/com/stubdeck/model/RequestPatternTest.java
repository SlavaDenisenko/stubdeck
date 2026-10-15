package com.stubdeck.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RequestPatternTest {

    @Test
    void effectiveUrl_url_returnsExact() {
        RequestPattern p = new RequestPattern();
        p.setUrl("/api/users");
        assertThat(p.getEffectiveUrl()).isEqualTo("/api/users");
        assertThat(p.getUrlType()).isEqualTo("exact");
    }

    @Test
    void effectiveUrl_urlPath_returnsPath() {
        RequestPattern p = new RequestPattern();
        p.setUrlPath("/api/users");
        assertThat(p.getEffectiveUrl()).isEqualTo("/api/users");
        assertThat(p.getUrlType()).isEqualTo("path");
    }

    @Test
    void effectiveUrl_urlPattern_returnsPrefixed() {
        RequestPattern p = new RequestPattern();
        p.setUrlPattern("/api/users/.*");
        assertThat(p.getEffectiveUrl()).isEqualTo("~/api/users/.*");
        assertThat(p.getUrlType()).isEqualTo("regex");
    }

    @Test
    void effectiveUrl_urlPathPattern_returnsPrefixed() {
        RequestPattern p = new RequestPattern();
        p.setUrlPathPattern("/api/users/.*");
        assertThat(p.getEffectiveUrl()).isEqualTo("~/api/users/.*");
        assertThat(p.getUrlType()).isEqualTo("pathRegex");
    }

    @Test
    void effectiveUrl_urlPathTemplate_returnsTemplate() {
        RequestPattern p = new RequestPattern();
        p.setUrlPathTemplate("/api/users/{id}");
        assertThat(p.getEffectiveUrl()).isEqualTo("/api/users/{id}");
        assertThat(p.getUrlType()).isEqualTo("template");
    }

    @Test
    void effectiveUrl_noUrl_returnsUnknown() {
        RequestPattern p = new RequestPattern();
        assertThat(p.getEffectiveUrl()).isEqualTo("unknown");
        assertThat(p.getUrlType()).isEqualTo("unknown");
    }

    @Test
    void effectiveUrl_urlTakesPrecedenceOverUrlPath() {
        RequestPattern p = new RequestPattern();
        p.setUrl("/exact");
        p.setUrlPath("/path");
        assertThat(p.getEffectiveUrl()).isEqualTo("/exact");
        assertThat(p.getUrlType()).isEqualTo("exact");
    }
}
