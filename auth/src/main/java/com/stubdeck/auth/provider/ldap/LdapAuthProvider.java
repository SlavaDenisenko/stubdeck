package com.stubdeck.auth.provider.ldap;

import com.stubdeck.auth.provider.AuthProvider;
import com.stubdeck.config.AuthProperties;
import com.stubdeck.auth.provider.ldap.mapper.LdapUserDetailsMapper;
import com.stubdeck.config.AuthProviderType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;

@Configuration
@ConditionalOnProperty(name = "wiremock.auth.provider-type", havingValue = "LDAP")
@RequiredArgsConstructor
public class LdapAuthProvider implements AuthProvider {

    private final AuthProperties authProperties;

    @Override
    public AuthProviderType getType() {
        return AuthProviderType.LDAP;
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.authenticationProvider(ldapAuthenticationProvider());
    }

    @Bean
    public DefaultSpringSecurityContextSource ldapContextSource() {
        AuthProperties.Ldap ldap = authProperties.getLdap();
        DefaultSpringSecurityContextSource ctx = new DefaultSpringSecurityContextSource(
                ldap.getUrl() + "/" + ldap.getBaseDn()
        );
        if (!ldap.getBindDn().isBlank()) {
            ctx.setUserDn(ldap.getBindDn());
            ctx.setPassword(ldap.getPassword());
        }
        return ctx;
    }

    @Bean
    public LdapAuthenticationProvider ldapAuthenticationProvider() {
        AuthProperties.Ldap ldap = authProperties.getLdap();
        DefaultSpringSecurityContextSource contextSource = ldapContextSource();

        FilterBasedLdapUserSearch userSearch = new FilterBasedLdapUserSearch(
                ldap.getUserSearchBase(),
                ldap.getUserSearchFilter(),
                contextSource
        );
        userSearch.setSearchSubtree(true);

        DefaultLdapAuthoritiesPopulator populator = new DefaultLdapAuthoritiesPopulator(
                contextSource,
                ldap.getGroupSearchBase()
        );
        populator.setGroupSearchFilter(ldap.getGroupSearchFilter());
        populator.setSearchSubtree(true);
        populator.setIgnorePartialResultException(true);
        populator.setConvertToUpperCase(false);

        BindAuthenticator authenticator = new BindAuthenticator(contextSource);
        authenticator.setUserSearch(userSearch);

        LdapAuthenticationProvider provider = new LdapAuthenticationProvider(authenticator, populator);
        provider.setUserDetailsContextMapper(new LdapUserDetailsMapper(authProperties));
        return provider;
    }
}
