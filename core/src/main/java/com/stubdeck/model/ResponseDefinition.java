package com.stubdeck.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.Data;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseDefinition {
    private Integer status;

    @JsonDeserialize(using = RawJsonDeserializer.class)
    @JsonRawValue
    private String jsonBody;

    private String body;
    private String bodyFileName;
    private Map<String, String> headers;
    private List<String> transformers;
}
