package com.stubdeck.auth.provider;

import com.stubdeck.config.AuthProviderType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

public interface AuthProvider {
    AuthProviderType getType();

    void configure(HttpSecurity http) throws Exception;
}
