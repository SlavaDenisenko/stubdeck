package com.stubdeck.auth.provider.ldap.mapper;

import com.stubdeck.auth.provider.ldap.model.LdapUserDetails;
import com.stubdeck.config.AuthProperties;
import com.stubdeck.model.auth.AppRole;
import com.stubdeck.model.auth.AppUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public class LdapUserDetailsMapper implements UserDetailsContextMapper {

    private final AuthProperties authProperties;

    @Override
    public UserDetails mapUserFromContext(DirContextOperations ctx, String username,
                                          Collection<? extends GrantedAuthority> authorities) {
        String displayName = getAttr(ctx, "displayName", username);
        String email = getAttr(ctx, "mail", "");
        String department = getAttr(ctx, authProperties.getLdap().getDepartmentAttribute(), "");

        Set<AppRole> roles = mapRoles(authorities);

        AppUser user = AppUser.builder()
                .username(username)
                .displayName(displayName)
                .email(email)
                .department(department)
                .roles(roles)
                .build();

        log.info("User logged in: {} | dept {} | roles {}", username, department, roles);
        return new LdapUserDetails(user);
    }

    @Override
    public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
        throw new UnsupportedOperationException("Read-only LDAP");
    }

    private Set<AppRole> mapRoles(Collection<? extends GrantedAuthority> authorities) {
        Map<String, String> mappings = authProperties.getLdap().getRoleMappings();
        Set<AppRole> roles = new HashSet<>();

        authorities.forEach(auth -> {
            String group = auth.getAuthority(); // e.g. "ROLE_wiremock-admins"
            mappings.forEach((role, dnOrCn) -> {
                // extract CN from full DN: "cn=wiremock-admins,ou=..." => "wiremock-admins"
                String cn = dnOrCn.replaceFirst("(?i)^cn=([^,]+).*", "$1");
                if (group.equalsIgnoreCase("ROLE_" + cn)) {
                    try {
                        roles.add(AppRole.valueOf(role));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            });
        });

        if (roles.isEmpty()) roles.add(AppRole.ROLE_VIEWER);
        return roles;
    }

    private String getAttr(DirContextOperations ctx, String attr, String fallback) {
        try {
            String val = ctx.getStringAttribute(attr);
            return val != null ? val : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }
}
