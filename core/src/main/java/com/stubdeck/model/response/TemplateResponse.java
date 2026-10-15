package com.stubdeck.model.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TemplateResponse {
    private String id;
    private String label;
    private String description;
    private String json;
}
