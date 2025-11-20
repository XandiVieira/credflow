package com.relyon.credflow.service;

import com.relyon.credflow.configuration.JwtUtil;
import com.relyon.credflow.model.user.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private AuthenticationManager authManager;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthenticationService authenticationService;

    @BeforeEach
    void clearSecurityContextBefore() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void clearSecurityContextAfter() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void register_delegatesToUserServiceCreate_andReturnsCreatedUser() {
        var toCreate = new UserRequestDTO();
        toCreate.setEmail("new@user.com");

        var created = new User();
        created.setId(1L);
        created.setEmail("new@user.com");

        when(userService.create(same(toCreate))).thenReturn(created);

        var result = authenticationService.register(toCreate);

        assertSame(created, result);
        verify(userService, times(1)).create(same(toCreate));
        verifyNoMoreInteractions(userService, authManager, jwtUtil);
    }

    @Test
    void login_success_authenticates_setsSecurityContext_generatesJwt_andReturnsToken() {
        var req = new AuthRequest();
        req.setEmail("alex@example.com");
        req.setPassword("s3cr3t");

        var principal = new AuthenticatedUser(42L, 1L, "alex@example.com", "Alex", "myPassword", UserRole.OWNER);

        var authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(principal);

        var tokenCaptor = ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        when(authManager.authenticate(tokenCaptor.capture())).thenReturn(authentication);

        when(jwtUtil.generateToken(eq("alex@example.com"))).thenReturn("JWT_TOKEN");

        var result = authenticationService.login(req);

        var passedToken = tokenCaptor.getValue();
        assertEquals("alex@example.com", passedToken.getPrincipal());
        assertEquals("s3cr3t", passedToken.getCredentials());

        var ctxAuth = SecurityContextHolder.getContext().getAuthentication();
        assertSame(authentication, ctxAuth);

        verify(jwtUtil, times(1)).generateToken("alex@example.com");

        assertNotNull(result);
        assertEquals(Map.of("token", "JWT_TOKEN"), result);

        verify(authManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verifyNoMoreInteractions(authManager, jwtUtil, userService);
    }

    @Test
    void login_whenAuthenticateFails_propagatesException_andDoesNotSetContext_orGenerateJwt() {
        var req = new AuthRequest();
        req.setEmail("bad@example.com");
        req.setPassword("wrong");

        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new RuntimeException("Auth failed"));

        var ex = assertThrows(RuntimeException.class, () -> authenticationService.login(req));
        assertTrue(ex.getMessage().contains("Auth failed"));

        assertNull(SecurityContextHolder.getContext().getAuthentication());

        verify(jwtUtil, never()).generateToken(anyString());

        verify(authManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verifyNoMoreInteractions(authManager, jwtUtil, userService);
    }
}
