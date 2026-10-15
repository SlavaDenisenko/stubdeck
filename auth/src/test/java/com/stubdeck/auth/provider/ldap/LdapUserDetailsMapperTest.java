package com.stubdeck.auth.provider.ldap;

import com.stubdeck.auth.provider.ldap.mapper.LdapUserDetailsMapper;
import com.stubdeck.auth.provider.ldap.model.LdapUserDetails;
import com.stubdeck.config.AuthProperties;
import com.stubdeck.model.auth.AppRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LdapUserDetailsMapperTest {

    @Mock
    DirContextOperations ctx;

    AuthProperties authProperties;
    LdapUserDetailsMapper mapper;

    @BeforeEach
    void setUp() {
        authProperties = new AuthProperties();
        authProperties.getLdap().setDepartmentAttribute("department");
        authProperties.getLdap().setRoleMappings(Map.of(
                "ROLE_ADMIN", "cn=wiremock-admins,ou=groups,dc=test,dc=com",
                "ROLE_DEVELOPER", "cn=wiremock-devs,ou=groups,dc=test,dc=com"
        ));
        mapper = new LdapUserDetailsMapper(authProperties);
    }

    @Test
    void mapUserFromContext_mapsDisplayNameAndEmail() {
        when(ctx.getStringAttribute("displayName")).thenReturn("Alice Smith");
        when(ctx.getStringAttribute("mail")).thenReturn("alice@test.com");
        when(ctx.getStringAttribute("department")).thenReturn("Engineering");

        LdapUserDetails details = (LdapUserDetails) mapper.mapUserFromContext(
                ctx, "alice", List.of());

        assertThat(details.getAppUser().getUsername()).isEqualTo("alice");
        assertThat(details.getAppUser().getDisplayName()).isEqualTo("Alice Smith");
        assertThat(details.getAppUser().getEmail()).isEqualTo("alice@test.com");
        assertThat(details.getAppUser().getDepartment()).isEqualTo("Engineering");
    }

    @Test
    void mapUserFromContext_matchingGroupGrantsRole() {
        when(ctx.getStringAttribute("displayName")).thenReturn("Bob");
        when(ctx.getStringAttribute("mail")).thenReturn("");
        when(ctx.getStringAttribute("department")).thenReturn("");

        LdapUserDetails details = (LdapUserDetails) mapper.mapUserFromContext(
                ctx, "bob",
                List.of(new SimpleGrantedAuthority("ROLE_wiremock-admins")));

        assertThat(details.getAppUser().getRoles()).containsExactly(AppRole.ROLE_ADMIN);
    }

    @Test
    void mapUserFromContext_noMatchingGroup_defaultsToViewer() {
        when(ctx.getStringAttribute("displayName")).thenReturn("Carol");
        when(ctx.getStringAttribute("mail")).thenReturn("");
        when(ctx.getStringAttribute("department")).thenReturn("");

        LdapUserDetails details = (LdapUserDetails) mapper.mapUserFromContext(
                ctx, "carol", List.of(new SimpleGrantedAuthority("ROLE_other-group")));

        assertThat(details.getAppUser().getRoles()).containsExactly(AppRole.ROLE_VIEWER);
    }

    @Test
    void mapUserFromContext_multipleGroupsMultipleRoles() {
        when(ctx.getStringAttribute("displayName")).thenReturn("Dave");
        when(ctx.getStringAttribute("mail")).thenReturn("");
        when(ctx.getStringAttribute("department")).thenReturn("");

        LdapUserDetails details = (LdapUserDetails) mapper.mapUserFromContext(
                ctx, "dave",
                List.of(
                        new SimpleGrantedAuthority("ROLE_wiremock-admins"),
                        new SimpleGrantedAuthority("ROLE_wiremock-devs")
                ));

        assertThat(details.getAppUser().getRoles())
                .containsExactlyInAnyOrder(AppRole.ROLE_ADMIN, AppRole.ROLE_DEVELOPER);
    }

    @Test
    void mapUserFromContext_nullDisplayName_fallsBackToUsername() {
        when(ctx.getStringAttribute("displayName")).thenReturn(null);
        when(ctx.getStringAttribute("mail")).thenReturn(null);
        when(ctx.getStringAttribute("department")).thenReturn(null);

        LdapUserDetails details = (LdapUserDetails) mapper.mapUserFromContext(
                ctx, "eve", List.of());

        assertThat(details.getAppUser().getDisplayName()).isEqualTo("eve");
    }

    @Test
    void mapUserToContext_throwsUnsupported() {
        assertThatThrownBy(() -> mapper.mapUserToContext(null, null))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
