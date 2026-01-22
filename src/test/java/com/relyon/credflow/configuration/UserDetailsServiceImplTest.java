package com.relyon.credflow.configuration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.user.AuthenticatedUser;
import com.relyon.credflow.model.user.User;
import com.relyon.credflow.model.user.UserRole;
import com.relyon.credflow.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void loadUserByUsername_existingUser_returnsAuthenticatedUser() {
        var account = new Account();
        account.setId(1L);
        account.setName("Test Account");

        var user = new User();
        user.setId(10L);
        user.setEmail("user@example.com");
        user.setName("Test User");
        user.setPassword("encoded-password");
        user.setRole(UserRole.OWNER);
        user.setAccount(account);

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        var result = userDetailsService.loadUserByUsername("user@example.com");

        assertNotNull(result);
        assertInstanceOf(AuthenticatedUser.class, result);
        var authenticatedUser = (AuthenticatedUser) result;
        assertEquals(10L, authenticatedUser.getUserId());
        assertEquals(1L, authenticatedUser.getAccountId());
        assertEquals("user@example.com", authenticatedUser.getEmail());
        assertEquals("Test User", authenticatedUser.getName());
        assertEquals("encoded-password", authenticatedUser.getPassword());
        assertEquals(UserRole.OWNER, authenticatedUser.getRole());
    }

    @Test
    void loadUserByUsername_nonExistingUser_throwsUsernameNotFoundException() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        var exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("unknown@example.com")
        );

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void loadUserByUsername_memberRole_returnsCorrectAuthorities() {
        var account = new Account();
        account.setId(1L);

        var user = new User();
        user.setId(1L);
        user.setEmail("member@example.com");
        user.setName("Member");
        user.setPassword("password");
        user.setRole(UserRole.MEMBER);
        user.setAccount(account);

        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));

        var result = userDetailsService.loadUserByUsername("member@example.com");
        var authenticatedUser = (AuthenticatedUser) result;

        assertEquals(UserRole.MEMBER, authenticatedUser.getRole());
        assertTrue(authenticatedUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MEMBER")));
    }

    @Test
    void loadUserByUsername_readOnlyRole_returnsCorrectAuthorities() {
        var account = new Account();
        account.setId(1L);

        var user = new User();
        user.setId(1L);
        user.setEmail("readonly@example.com");
        user.setName("ReadOnly User");
        user.setPassword("password");
        user.setRole(UserRole.READONLY);
        user.setAccount(account);

        when(userRepository.findByEmail("readonly@example.com")).thenReturn(Optional.of(user));

        var result = userDetailsService.loadUserByUsername("readonly@example.com");
        var authenticatedUser = (AuthenticatedUser) result;

        assertEquals(UserRole.READONLY, authenticatedUser.getRole());
        assertTrue(authenticatedUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_READONLY")));
    }

    @Test
    void loadUserByUsername_verifiesRepositoryIsCalled() {
        var account = new Account();
        account.setId(1L);

        var user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setName("Test");
        user.setPassword("password");
        user.setRole(UserRole.OWNER);
        user.setAccount(account);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        userDetailsService.loadUserByUsername("test@example.com");

        verify(userRepository, times(1)).findByEmail("test@example.com");
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void loadUserByUsername_caseMattersForEmail() {
        when(userRepository.findByEmail("User@Example.com")).thenReturn(Optional.empty());

        assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("User@Example.com")
        );

        verify(userRepository).findByEmail("User@Example.com");
    }
}
