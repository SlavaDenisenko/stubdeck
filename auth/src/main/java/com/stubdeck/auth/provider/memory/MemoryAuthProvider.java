package com.stubdeck.auth.provider.memory;

import com.stubdeck.auth.provider.AuthProvider;
import com.stubdeck.config.AuthProperties;
import com.stubdeck.config.AuthProviderType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import java.util.List;

@Configuration
@ConditionalOnProperty(name = "wiremock.auth.provider-type", havingValue = "MEMORY")
@RequiredArgsConstructor
public class MemoryAuthProvider implements AuthProvider {

    private final AuthProperties authProperties;

    @Override
    public AuthProviderType getType() {
        return AuthProviderType.MEMORY;
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.userDetailsService(userDetailsManager());
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsManager() {
        List<UserDetails> users = authProperties.getMemory().getUsers().stream()
                .map(u -> User.withDefaultPasswordEncoder()
                        .username(u.getUsername())
                        .password(u.getPassword())
                        .roles(u.getRoles().toArray(String[]::new))
                        .build())
                .toList();
        return new InMemoryUserDetailsManager(users);
    }
}
