package com.stubdeck.auth.provider.noop;

import com.stubdeck.auth.provider.AuthProvider;
import com.stubdeck.config.AuthProviderType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "wiremock.auth.provider-type", havingValue = "NONE", matchIfMissing = true)
public class NoOpAuthProvider implements AuthProvider {

    @Override
    public AuthProviderType getType() {
        return AuthProviderType.NONE;
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
    }
}
