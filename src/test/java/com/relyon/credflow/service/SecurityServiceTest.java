package com.relyon.credflow.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.relyon.credflow.model.user.AuthenticatedUser;
import com.relyon.credflow.model.user.UserRole;
import com.relyon.credflow.repository.AccountRepository;
import com.relyon.credflow.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SecurityService securityService;

    @Test
    void canAccessAccount_whenUserBelongsToAccount_returnsTrue() {
        var user = createAuthenticatedUser(1L, 10L, UserRole.MEMBER);
        when(accountRepository.existsByIdAndUsersId(10L, 1L)).thenReturn(true);

        var result = securityService.canAccessAccount(user, 10L);

        assertTrue(result);
        verify(accountRepository).existsByIdAndUsersId(10L, 1L);
    }

    @Test
    void canAccessAccount_whenUserDoesNotBelongToAccount_returnsFalse() {
        var user = createAuthenticatedUser(1L, 10L, UserRole.MEMBER);
        when(accountRepository.existsByIdAndUsersId(99L, 1L)).thenReturn(false);

        var result = securityService.canAccessAccount(user, 99L);

        assertFalse(result);
    }

    @Test
    void canAccessAccount_whenUserIsNull_returnsFalse() {
        var result = securityService.canAccessAccount(null, 10L);

        assertFalse(result);
        verifyNoInteractions(accountRepository);
    }

    @Test
    void canAccessAccount_whenAccountIdIsNull_returnsFalse() {
        var user = createAuthenticatedUser(1L, 10L, UserRole.MEMBER);

        var result = securityService.canAccessAccount(user, null);

        assertFalse(result);
        verifyNoInteractions(accountRepository);
    }

    @Test
    void canModifyAccount_whenMemberWithAccess_returnsTrue() {
        var user = createAuthenticatedUser(1L, 10L, UserRole.MEMBER);
        when(accountRepository.existsByIdAndUsersId(10L, 1L)).thenReturn(true);

        var result = securityService.canModifyAccount(user, 10L);

        assertTrue(result);
    }

    @Test
    void canModifyAccount_whenOwnerWithAccess_returnsTrue() {
        var user = createAuthenticatedUser(1L, 10L, UserRole.OWNER);
        when(accountRepository.existsByIdAndUsersId(10L, 1L)).thenReturn(true);

        var result = securityService.canModifyAccount(user, 10L);

        assertTrue(result);
    }

    @Test
    void canModifyAccount_whenReadonlyWithAccess_returnsFalse() {
        var user = createAuthenticatedUser(1L, 10L, UserRole.READONLY);
        when(accountRepository.existsByIdAndUsersId(10L, 1L)).thenReturn(true);

        var result = securityService.canModifyAccount(user, 10L);

        assertFalse(result);
    }

    @Test
    void canModifyAccount_whenMemberWithoutAccess_returnsFalse() {
        var user = createAuthenticatedUser(1L, 10L, UserRole.MEMBER);
        when(accountRepository.existsByIdAndUsersId(99L, 1L)).thenReturn(false);

        var result = securityService.canModifyAccount(user, 99L);

        assertFalse(result);
    }

    @Test
    void canModifyAccount_whenUserIsNull_returnsFalse() {
        var result = securityService.canModifyAccount(null, 10L);

        assertFalse(result);
    }

    @Test
    void isOwnerOfAccount_whenOwnerWithAccess_returnsTrue() {
        var user = createAuthenticatedUser(1L, 10L, UserRole.OWNER);
        when(accountRepository.existsByIdAndUsersId(10L, 1L)).thenReturn(true);

        var result = securityService.isOwnerOfAccount(user, 10L);

        assertTrue(result);
    }

    @Test
    void isOwnerOfAccount_whenMemberWithAccess_returnsFalse() {
        var user = createAuthenticatedUser(1L, 10L, UserRole.MEMBER);
        when(accountRepository.existsByIdAndUsersId(10L, 1L)).thenReturn(true);

        var result = securityService.isOwnerOfAccount(user, 10L);

        assertFalse(result);
    }

    @Test
    void isOwnerOfAccount_whenReadonlyWithAccess_returnsFalse() {
        var user = createAuthenticatedUser(1L, 10L, UserRole.READONLY);
        when(accountRepository.existsByIdAndUsersId(10L, 1L)).thenReturn(true);

        var result = securityService.isOwnerOfAccount(user, 10L);

        assertFalse(result);
    }

    @Test
    void isOwnerOfAccount_whenOwnerWithoutAccess_returnsFalse() {
        var user = createAuthenticatedUser(1L, 10L, UserRole.OWNER);
        when(accountRepository.existsByIdAndUsersId(99L, 1L)).thenReturn(false);

        var result = securityService.isOwnerOfAccount(user, 99L);

        assertFalse(result);
    }

    @Test
    void isOwnerOfAccount_whenUserIsNull_returnsFalse() {
        var result = securityService.isOwnerOfAccount(null, 10L);

        assertFalse(result);
    }

    @Test
    void isOwnerOfAccount_whenAccountIdIsNull_returnsFalse() {
        var user = createAuthenticatedUser(1L, 10L, UserRole.OWNER);

        var result = securityService.isOwnerOfAccount(user, null);

        assertFalse(result);
    }

    @Test
    void canAccessUser_whenUserInSameAccount_returnsTrue() {
        var authenticatedUser = createAuthenticatedUser(1L, 10L, UserRole.MEMBER);
        when(userRepository.existsByIdAndAccountId(5L, 10L)).thenReturn(true);

        var result = securityService.canAccessUser(authenticatedUser, 5L);

        assertTrue(result);
        verify(userRepository).existsByIdAndAccountId(5L, 10L);
    }

    @Test
    void canAccessUser_whenUserInDifferentAccount_returnsFalse() {
        var authenticatedUser = createAuthenticatedUser(1L, 10L, UserRole.MEMBER);
        when(userRepository.existsByIdAndAccountId(5L, 10L)).thenReturn(false);

        var result = securityService.canAccessUser(authenticatedUser, 5L);

        assertFalse(result);
    }

    @Test
    void canAccessUser_whenAuthenticatedUserIsNull_returnsFalse() {
        var result = securityService.canAccessUser(null, 5L);

        assertFalse(result);
        verifyNoInteractions(userRepository);
    }

    @Test
    void canAccessUser_whenTargetUserIdIsNull_returnsFalse() {
        var authenticatedUser = createAuthenticatedUser(1L, 10L, UserRole.MEMBER);

        var result = securityService.canAccessUser(authenticatedUser, null);

        assertFalse(result);
        verifyNoInteractions(userRepository);
    }

    @Test
    void canModify_whenMember_returnsTrue() {
        var user = createAuthenticatedUser(1L, 10L, UserRole.MEMBER);

        var result = securityService.canModify(user);

        assertTrue(result);
    }

    @Test
    void canModify_whenOwner_returnsTrue() {
        var user = createAuthenticatedUser(1L, 10L, UserRole.OWNER);

        var result = securityService.canModify(user);

        assertTrue(result);
    }

    @Test
    void canModify_whenReadonly_returnsFalse() {
        var user = createAuthenticatedUser(1L, 10L, UserRole.READONLY);

        var result = securityService.canModify(user);

        assertFalse(result);
    }

    @Test
    void canModify_whenUserIsNull_returnsFalse() {
        var result = securityService.canModify(null);

        assertFalse(result);
    }

    private AuthenticatedUser createAuthenticatedUser(Long userId, Long accountId, UserRole role) {
        return new AuthenticatedUser(
                userId,
                accountId,
                "test@example.com",
                "Test User",
                "password",
                role
        );
    }
}
