package com.stubdeck.service;

import com.stubdeck.auth.provider.ldap.model.LdapUserDetails;
import com.stubdeck.model.auth.AppRole;
import com.stubdeck.model.auth.AppUser;
import com.stubdeck.service.context.user.SecurityUserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityUserContextTest {

    SecurityUserContext context = new SecurityUserContext();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUser_noAuthentication_returnsEmpty() {
        assertThat(context.getCurrentUser()).isEmpty();
    }

    @Test
    void getCurrentUser_ldapPrincipal_returnsAppUser() {
        AppUser appUser = AppUser.builder()
                .username("alice")
                .displayName("Alice Smith")
                .roles(Set.of(AppRole.ROLE_ADMIN))
                .build();
        LdapUserDetails details = new LdapUserDetails(appUser);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));

        Optional<AppUser> result = context.getCurrentUser();
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("alice");
        assertThat(result.get().getRoles()).containsExactly(AppRole.ROLE_ADMIN);
    }

    @Test
    void getCurrentUser_plainUserDetails_buildsAppUser() {
        User springUser = new User("bob", "",
                List.of(new SimpleGrantedAuthority("ROLE_DEVELOPER")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(springUser, null, springUser.getAuthorities()));

        Optional<AppUser> result = context.getCurrentUser();
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("bob");
        assertThat(result.get().getRoles()).containsExactly(AppRole.ROLE_DEVELOPER);
    }

    @Test
    void getCurrentUser_anonymousUser_returnsEmpty() {
        User anonymous = new User("anonymousUser", "",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(anonymous, null, anonymous.getAuthorities()));

        assertThat(context.getCurrentUser()).isEmpty();
    }

    @Test
    void getCurrentUser_unknownAuthority_defaultsToViewer() {
        User springUser = new User("carol", "",
                List.of(new SimpleGrantedAuthority("ROLE_UNKNOWN_THING")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(springUser, null, springUser.getAuthorities()));

        Optional<AppUser> result = context.getCurrentUser();
        assertThat(result).isPresent();
        assertThat(result.get().getRoles()).containsExactly(AppRole.ROLE_VIEWER);
    }

    @Test
    void getCurrentUsername_noAuth_returnsAnonymous() {
        assertThat(context.getCurrentUsername()).isEqualTo("anonymous");
    }

    @Test
    void getCurrentUsername_authenticated_returnsUsername() {
        AppUser appUser = AppUser.builder().username("dave").roles(Set.of()).build();
        LdapUserDetails details = new LdapUserDetails(appUser);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));

        assertThat(context.getCurrentUsername()).isEqualTo("dave");
    }

    @Test
    void getCurrentDepartment_ldapUser_returnsDepartment() {
        AppUser appUser = AppUser.builder()
                .username("eve")
                .department("QA")
                .roles(Set.of())
                .build();
        LdapUserDetails details = new LdapUserDetails(appUser);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));

        assertThat(context.getCurrentDepartment()).isEqualTo("QA");
    }
}
