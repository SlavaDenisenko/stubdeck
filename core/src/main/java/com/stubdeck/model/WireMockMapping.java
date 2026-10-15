package com.stubdeck.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WireMockMapping {
    private String id;
    private String name;
    private Integer priority;
    private RequestPattern request;
    private ResponseDefinition response;

    @JsonIgnore
    private String fileName;

    public String getEffectiveUrl() {
        return request == null ? null : request.getEffectiveUrl();
    }

    public String getMethod() {
        return request == null ? null : request.getMethod();
    }
}
