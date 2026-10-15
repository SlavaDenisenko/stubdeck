package com.stubdeck.service.access;

import com.stubdeck.config.AuthProperties;
import com.stubdeck.model.auth.AppRole;
import com.stubdeck.model.auth.AppUser;
import com.stubdeck.service.context.user.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("accessGuard")
@RequiredArgsConstructor
public class RoleBasedAccessGuard implements AccessGuard {

    private final AuthProperties authProperties;
    private final UserContext userContext;

    @Override
    public boolean canEdit() {
        return openOrHasRole(AppRole.ROLE_ADMIN, AppRole.ROLE_DEVELOPER);
    }

    @Override
    public boolean canDelete() {
        return openOrHasRole(AppRole.ROLE_ADMIN, AppRole.ROLE_DEVELOPER);
    }

    @Override
    public boolean canPersist() {
        return openOrHasRole(AppRole.ROLE_ADMIN);
    }

    @Override
    public boolean canDiscard() {
        return openOrHasRole(AppRole.ROLE_ADMIN, AppRole.ROLE_DEVELOPER);
    }

    @Override
    public boolean canManageSettings() {
        return openOrHasRole(AppRole.ROLE_ADMIN);
    }

    private boolean openOrHasRole(AppRole... roles) {
        if (!authProperties.isEnabled()) return true;
        return userContext.getCurrentUser()
                .map(AppUser::getRoles)
                .map(userRoles -> {
                    for (AppRole role : roles) {
                        if (userRoles.contains(role)) return true;
                    }
                    return false;
                })
                .orElse(false);
    }
}
