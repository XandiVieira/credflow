package com.relyon.credflow.model.user;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AuthenticatedUserTest {

    @Test
    void constructor_setsAllFields() {
        var user = new AuthenticatedUser(1L, 2L, "test@example.com", "Test User", "password123", UserRole.OWNER);

        assertEquals(1L, user.getUserId());
        assertEquals(2L, user.getAccountId());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("Test User", user.getName());
        assertEquals("password123", user.getPassword());
        assertEquals(UserRole.OWNER, user.getRole());
    }

    @Test
    void getUsername_returnsEmail() {
        var user = new AuthenticatedUser(1L, 1L, "user@test.com", "Name", "pass", UserRole.MEMBER);

        assertEquals("user@test.com", user.getUsername());
    }

    @Test
    void getAuthorities_owner_returnsRoleOwner() {
        var user = new AuthenticatedUser(1L, 1L, "owner@test.com", "Owner", "pass", UserRole.OWNER);

        var authorities = user.getAuthorities();

        assertEquals(1, authorities.size());
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_OWNER")));
    }

    @Test
    void getAuthorities_member_returnsRoleMember() {
        var user = new AuthenticatedUser(1L, 1L, "member@test.com", "Member", "pass", UserRole.MEMBER);

        var authorities = user.getAuthorities();

        assertEquals(1, authorities.size());
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_MEMBER")));
    }

    @Test
    void getAuthorities_readonly_returnsRoleReadonly() {
        var user = new AuthenticatedUser(1L, 1L, "readonly@test.com", "ReadOnly", "pass", UserRole.READONLY);

        var authorities = user.getAuthorities();

        assertEquals(1, authorities.size());
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_READONLY")));
    }

    @Test
    void isOwner_ownerRole_returnsTrue() {
        var user = new AuthenticatedUser(1L, 1L, "test@test.com", "Name", "pass", UserRole.OWNER);

        assertTrue(user.isOwner());
    }

    @Test
    void isOwner_memberRole_returnsFalse() {
        var user = new AuthenticatedUser(1L, 1L, "test@test.com", "Name", "pass", UserRole.MEMBER);

        assertFalse(user.isOwner());
    }

    @Test
    void isOwner_readonlyRole_returnsFalse() {
        var user = new AuthenticatedUser(1L, 1L, "test@test.com", "Name", "pass", UserRole.READONLY);

        assertFalse(user.isOwner());
    }

    @Test
    void isMember_ownerRole_returnsTrue() {
        var user = new AuthenticatedUser(1L, 1L, "test@test.com", "Name", "pass", UserRole.OWNER);

        assertTrue(user.isMember());
    }

    @Test
    void isMember_memberRole_returnsTrue() {
        var user = new AuthenticatedUser(1L, 1L, "test@test.com", "Name", "pass", UserRole.MEMBER);

        assertTrue(user.isMember());
    }

    @Test
    void isMember_readonlyRole_returnsFalse() {
        var user = new AuthenticatedUser(1L, 1L, "test@test.com", "Name", "pass", UserRole.READONLY);

        assertFalse(user.isMember());
    }

    @Test
    void isReadOnly_readonlyRole_returnsTrue() {
        var user = new AuthenticatedUser(1L, 1L, "test@test.com", "Name", "pass", UserRole.READONLY);

        assertTrue(user.isReadOnly());
    }

    @Test
    void isReadOnly_ownerRole_returnsFalse() {
        var user = new AuthenticatedUser(1L, 1L, "test@test.com", "Name", "pass", UserRole.OWNER);

        assertFalse(user.isReadOnly());
    }

    @Test
    void isReadOnly_memberRole_returnsFalse() {
        var user = new AuthenticatedUser(1L, 1L, "test@test.com", "Name", "pass", UserRole.MEMBER);

        assertFalse(user.isReadOnly());
    }

    @Test
    void isAccountNonExpired_alwaysReturnsTrue() {
        var user = new AuthenticatedUser(1L, 1L, "test@test.com", "Name", "pass", UserRole.MEMBER);

        assertTrue(user.isAccountNonExpired());
    }

    @Test
    void isAccountNonLocked_alwaysReturnsTrue() {
        var user = new AuthenticatedUser(1L, 1L, "test@test.com", "Name", "pass", UserRole.MEMBER);

        assertTrue(user.isAccountNonLocked());
    }

    @Test
    void isCredentialsNonExpired_alwaysReturnsTrue() {
        var user = new AuthenticatedUser(1L, 1L, "test@test.com", "Name", "pass", UserRole.MEMBER);

        assertTrue(user.isCredentialsNonExpired());
    }

    @Test
    void isEnabled_alwaysReturnsTrue() {
        var user = new AuthenticatedUser(1L, 1L, "test@test.com", "Name", "pass", UserRole.MEMBER);

        assertTrue(user.isEnabled());
    }

    @Test
    void implementsUserDetails() {
        var user = new AuthenticatedUser(1L, 1L, "test@test.com", "Name", "pass", UserRole.MEMBER);

        assertInstanceOf(org.springframework.security.core.userdetails.UserDetails.class, user);
    }
}
