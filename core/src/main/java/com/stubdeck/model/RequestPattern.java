package com.stubdeck.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequestPattern {
    private String method;
    private String url;
    private String urlPattern;
    private String urlPath;
    private String urlPathPattern;
    private String urlPathTemplate;

    private Map<String, Object> headers;
    private List<Map<String, Object>> bodyPatterns;
    private Map<String, Object> queryParameters;

    @JsonIgnore
    public String getEffectiveUrl() {
        if (url != null) return url;
        if (urlPath != null) return urlPath;
        if (urlPattern != null) return "~" + urlPattern;
        if (urlPathPattern != null) return "~" + urlPathPattern;
        if (urlPathTemplate != null) return urlPathTemplate;
        return "unknown";
    }

    @JsonIgnore
    public String getUrlType() {
        if (url != null) return "exact";
        if (urlPath != null) return "path";
        if (urlPattern != null) return "regex";
        if (urlPathPattern != null) return "pathRegex";
        if (urlPathTemplate != null) return "template";
        return "unknown";
    }
}
