package com.stubdeck.controller;

import com.stubdeck.model.auth.AppUser;
import com.stubdeck.service.access.AccessGuard;
import com.stubdeck.service.context.instance.InstanceContext;
import com.stubdeck.service.context.user.UserContext;
import com.stubdeck.service.changes.ChangeTracker;
import com.stubdeck.service.MappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributes {

    private final MappingService mappingService;
    private final UserContext userContext;
    private final ChangeTracker changeTracker;
    private final AccessGuard accessGuard;
    private final InstanceContext instanceContext;

    @ModelAttribute("sourceType")
    public String getSourceType() {
        return mappingService.getSourceType();
    }

    @ModelAttribute("currentUser")
    public AppUser currentUser() {
        return userContext.getCurrentUser().orElse(null);
    }

    @ModelAttribute("changesEnabled")
    public boolean changesEnabled() {
        return changeTracker.isAvailable();
    }

    @ModelAttribute("accessGuard")
    public AccessGuard accessGuard() {
        return accessGuard;
    }

    @ModelAttribute("instancesEnabled")
    public boolean instancesEnabled() {
        return instanceContext.isAvailable();
    }
}
