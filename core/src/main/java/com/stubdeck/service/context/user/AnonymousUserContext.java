package com.stubdeck.service.context.user;

import com.stubdeck.model.auth.AppUser;
import org.springframework.context.annotation.Fallback;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Fallback
public class AnonymousUserContext implements UserContext {

    @Override
    public String getCurrentUsername() {
        return "anonymous";
    }

    @Override
    public String getCurrentDepartment() {
        return "";
    }

    @Override
    public Optional<AppUser> getCurrentUser() {
        return Optional.empty();
    }
}
