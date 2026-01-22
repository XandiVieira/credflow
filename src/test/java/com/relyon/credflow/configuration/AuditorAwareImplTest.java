package com.relyon.credflow.configuration;

import static org.junit.jupiter.api.Assertions.*;

import com.relyon.credflow.model.user.AuthenticatedUser;
import com.relyon.credflow.model.user.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

class AuditorAwareImplTest {

    private AuditorAwareImpl auditorAware;

    @BeforeEach
    void setUp() {
        auditorAware = new AuditorAwareImpl();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentAuditor_authenticatedUser_returnsUserId() {
        var authenticatedUser = new AuthenticatedUser(
                42L, 1L, "user@example.com", "Test User", "password", UserRole.OWNER
        );
        var authentication = new UsernamePasswordAuthenticationToken(
                authenticatedUser, null, authenticatedUser.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        var result = auditorAware.getCurrentAuditor();

        assertTrue(result.isPresent());
        assertEquals(42L, result.get());
    }

    @Test
    void getCurrentAuditor_noAuthentication_returnsEmpty() {
        var result = auditorAware.getCurrentAuditor();

        assertTrue(result.isEmpty());
    }

    @Test
    void getCurrentAuditor_nullAuthentication_returnsEmpty() {
        SecurityContextHolder.getContext().setAuthentication(null);

        var result = auditorAware.getCurrentAuditor();

        assertTrue(result.isEmpty());
    }

    @Test
    void getCurrentAuditor_notAuthenticated_returnsEmpty() {
        var authentication = new UsernamePasswordAuthenticationToken("user", "pass");
        authentication.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        var result = auditorAware.getCurrentAuditor();

        assertTrue(result.isEmpty());
    }

    @Test
    void getCurrentAuditor_principalNotAuthenticatedUser_returnsEmpty() {
        var authentication = new UsernamePasswordAuthenticationToken(
                "string-principal", null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        var result = auditorAware.getCurrentAuditor();

        assertTrue(result.isEmpty());
    }

    @Test
    void getCurrentAuditor_anonymousAuthentication_returnsEmpty() {
        var authentication = new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        var result = auditorAware.getCurrentAuditor();

        assertTrue(result.isEmpty());
    }

    @Test
    void getCurrentAuditor_differentUserIds_returnsCorrectId() {
        var user1 = new AuthenticatedUser(100L, 1L, "user1@example.com", "User 1", "pass", UserRole.OWNER);
        var auth1 = new UsernamePasswordAuthenticationToken(user1, null, user1.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth1);

        var result1 = auditorAware.getCurrentAuditor();
        assertEquals(100L, result1.orElseThrow());

        var user2 = new AuthenticatedUser(200L, 2L, "user2@example.com", "User 2", "pass", UserRole.MEMBER);
        var auth2 = new UsernamePasswordAuthenticationToken(user2, null, user2.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth2);

        var result2 = auditorAware.getCurrentAuditor();
        assertEquals(200L, result2.orElseThrow());
    }

    @Test
    void getCurrentAuditor_memberRole_returnsUserId() {
        var authenticatedUser = new AuthenticatedUser(
                5L, 1L, "member@example.com", "Member User", "password", UserRole.MEMBER
        );
        var authentication = new UsernamePasswordAuthenticationToken(
                authenticatedUser, null, authenticatedUser.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        var result = auditorAware.getCurrentAuditor();

        assertTrue(result.isPresent());
        assertEquals(5L, result.get());
    }

    @Test
    void getCurrentAuditor_readOnlyRole_returnsUserId() {
        var authenticatedUser = new AuthenticatedUser(
                10L, 1L, "readonly@example.com", "ReadOnly User", "password", UserRole.READONLY
        );
        var authentication = new UsernamePasswordAuthenticationToken(
                authenticatedUser, null, authenticatedUser.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        var result = auditorAware.getCurrentAuditor();

        assertTrue(result.isPresent());
        assertEquals(10L, result.get());
    }

    @Test
    void getCurrentAuditor_nullPrincipal_returnsEmpty() {
        var authentication = new UsernamePasswordAuthenticationToken(
                null, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        var result = auditorAware.getCurrentAuditor();

        assertTrue(result.isEmpty());
    }
}
