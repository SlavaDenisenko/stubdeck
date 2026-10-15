package com.stubdeck.service.context.user;

import com.stubdeck.auth.provider.ldap.model.LdapUserDetails;
import com.stubdeck.model.auth.AppRole;
import com.stubdeck.model.auth.AppUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class SecurityUserContext implements UserContext {

    @Override
    public Optional<AppUser> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return Optional.empty();

        Object principal = auth.getPrincipal();

        if (principal instanceof LdapUserDetails ldap) {
            return Optional.of(ldap.getAppUser());
        }

        if (principal instanceof UserDetails ud && !"anonymousUser".equals(ud.getUsername())) {
            Set<AppRole> roles = ud.getAuthorities().stream()
                    .map(a -> {
                        try {
                            return AppRole.valueOf(a.getAuthority());
                        } catch (IllegalArgumentException e) {
                            return AppRole.ROLE_VIEWER;
                        }
                    })
                    .collect(Collectors.toSet());

            return Optional.of(AppUser.builder()
                    .username(ud.getUsername())
                    .displayName(ud.getUsername())
                    .roles(roles)
                    .build());
        }

        return Optional.empty();
    }

    @Override
    public String getCurrentUsername() {
        return getCurrentUser().map(AppUser::getUsername).orElse("anonymous");
    }

    @Override
    public String getCurrentDepartment() {
        return getCurrentUser().map(AppUser::getDepartment).orElse("");
    }
}
