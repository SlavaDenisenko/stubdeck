package com.stubdeck.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

@Data
@ToString(exclude = "password")
public class SettingsRequest {
    private String sourceType;
    private String scheme;
    private String host;
    private int port;
    private String username;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
}
