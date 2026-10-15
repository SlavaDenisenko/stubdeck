package com.stubdeck.auth.provider.ldap;

import com.stubdeck.auth.provider.ldap.model.LdapUserDetails;
import com.stubdeck.model.auth.AppRole;
import com.stubdeck.model.auth.AppUser;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class LdapUserDetailsTest {

    @Test
    void getAuthorities_mapsRolesToGrantedAuthorities() {
        AppUser user = AppUser.builder()
                .username("alice")
                .roles(Set.of(AppRole.ROLE_ADMIN, AppRole.ROLE_DEVELOPER))
                .build();
        LdapUserDetails details = new LdapUserDetails(user);

        Set<String> authorities = details.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        assertThat(authorities).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_DEVELOPER");
    }

    @Test
    void getAuthorities_emptyRoles_returnsEmptyList() {
        AppUser user = AppUser.builder().username("bob").roles(Set.of()).build();
        LdapUserDetails details = new LdapUserDetails(user);
        assertThat(details.getAuthorities()).isEmpty();
    }

    @Test
    void getUsername_delegatesToAppUser() {
        AppUser user = AppUser.builder().username("carol").roles(Set.of()).build();
        assertThat(new LdapUserDetails(user).getUsername()).isEqualTo("carol");
    }

    @Test
    void getPassword_returnsEmpty() {
        AppUser user = AppUser.builder().username("dave").roles(Set.of()).build();
        assertThat(new LdapUserDetails(user).getPassword()).isEqualTo("");
    }

    @Test
    void getAppUser_returnsOriginalUser() {
        AppUser user = AppUser.builder().username("eve").roles(Set.of(AppRole.ROLE_QA)).build();
        assertThat(new LdapUserDetails(user).getAppUser()).isSameAs(user);
    }
}
