package com.stubdeck.auth.provider.ldap.model;

import com.stubdeck.model.auth.AppUser;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@Getter
public class LdapUserDetails implements UserDetails {

    private final AppUser appUser;

    public LdapUserDetails(AppUser appUser) {
        this.appUser = appUser;
    }

    @Override
    public @NullMarked Collection<? extends GrantedAuthority> getAuthorities() {
        return appUser.getRoles().stream()
                .map(r -> new SimpleGrantedAuthority(r.name()))
                .toList();
    }

    @Override
    public @NonNull String getUsername() {
        return appUser.getUsername();
    }

    @Override
    public @Nullable String getPassword() {
        return "";
    }
}
