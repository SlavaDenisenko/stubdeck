package com.stubdeck.model.auth;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class AppUser {
    private String username;
    private String displayName;
    private String email;
    private String department;
    private Set<AppRole> roles;

    public boolean isAdmin() {
        return roles != null && roles.contains(AppRole.ROLE_ADMIN);
    }

    public boolean isDeveloper() {
        return roles != null && roles.contains(AppRole.ROLE_DEVELOPER);
    }

    public boolean isQA() {
        return roles != null && roles.contains(AppRole.ROLE_QA);
    }
}
