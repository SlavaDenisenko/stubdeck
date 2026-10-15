package com.stubdeck.controller;

import com.stubdeck.config.SourceType;
import com.stubdeck.config.WireMockProperties;
import com.stubdeck.security.RequireAdmin;
import com.stubdeck.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.nio.file.Path;

import static com.stubdeck.constants.SettingsConstants.*;

@Controller
@RequiredArgsConstructor
public class SettingsController {

    private final WireMockProperties properties;
    private final SettingsService settingsService;

    @RequireAdmin
    @GetMapping("/settings")
    public String settings(Model model) {
        WireMockProperties.Admin admin = properties.getAdmin();
        SourceType sourceType = properties.getSource().getType();

        // Always populate all attributes — template uses th:if to show/hide panels,
        // but Thymeleaf still evaluates all SpEL expressions regardless of visibility.
        model.addAttribute(SOURCE_TYPE, sourceType.name());
        model.addAttribute(CHANGELOG_PATH, Path.of(properties.getChangelog().getPath()).toAbsolutePath().toString());
        model.addAttribute(MAPPINGS_PATH, Path.of(properties.getMappings().getPath()).toAbsolutePath().toString());
        model.addAttribute(SCHEME, admin.getScheme());
        model.addAttribute(HOST, admin.getHost());
        model.addAttribute(PORT, admin.getPort());
        model.addAttribute(USERNAME, admin.getUsername());
        model.addAttribute(PASSWORD, "");
        model.addAttribute(ADMIN_URL, admin.getBaseUrl());
        model.addAttribute(CONNECTION_OK, sourceType == SourceType.API && settingsService.testAdminApi());

        return "settings";
    }
}
