package com.stubdeck.model.response;

public record SettingsResponse(boolean ok, String message) {

    public static SettingsResponse success(String message) {
        return new SettingsResponse(true, message);
    }

    public static SettingsResponse error(String message) {
        return new SettingsResponse(false, message);
    }
}
