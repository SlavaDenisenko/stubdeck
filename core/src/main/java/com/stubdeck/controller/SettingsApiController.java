package com.stubdeck.controller;

import com.stubdeck.model.request.SettingsRequest;
import com.stubdeck.model.response.SettingsResponse;
import com.stubdeck.security.RequireAdmin;
import com.stubdeck.service.SettingsService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.stubdeck.support.CoreUtils.prepareUrl;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsApiController {

    private final SettingsService settingsService;

    @PostMapping("/test-connection")
    public ResponseEntity<@NonNull SettingsResponse> testConnection(@RequestBody SettingsRequest request) {
        return ResponseEntity.ok(settingsService.testConnection(prepareUrl(
                request.getScheme(), request.getHost(), request.getPort()
        )));
    }

    @RequireAdmin
    @PostMapping("/save")
    public ResponseEntity<@NonNull SettingsResponse> save(@RequestBody SettingsRequest request) {
        return ResponseEntity.ok(settingsService.saveSettings(request));
    }

    @RequireAdmin
    @PostMapping("/source-type")
    public ResponseEntity<@NonNull SettingsResponse> saveSourceType(@RequestBody SettingsRequest request) {
        return ResponseEntity.ok(settingsService.saveSourceType(request.getSourceType()));
    }
}
