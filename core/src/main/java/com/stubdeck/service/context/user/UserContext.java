package com.stubdeck.service.context.user;

import com.stubdeck.model.auth.AppUser;

import java.util.Optional;

public interface UserContext {
    String getCurrentUsername();

    String getCurrentDepartment();

    Optional<AppUser> getCurrentUser();
}
