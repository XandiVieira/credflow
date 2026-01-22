package com.relyon.credflow.configuration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.relyon.credflow.model.user.AuthenticatedUser;
import com.relyon.credflow.model.user.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtFilter jwtFilter;

    @BeforeEach
    void setUp() {
        jwtFilter = new JwtFilter(jwtUtil, userDetailsService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_noAuthHeader_continuesFilterChain() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtUtil);
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void doFilterInternal_nonBearerHeader_continuesFilterChain() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void doFilterInternal_invalidToken_continuesWithoutAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
        when(jwtUtil.isTokenValid("invalid-token")).thenReturn(false);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtUtil).isTokenValid("invalid-token");
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void doFilterInternal_validToken_setsAuthentication() throws Exception {
        var token = "valid-jwt-token";
        var email = "user@example.com";
        var userDetails = new AuthenticatedUser(1L, 1L, email, "Test User", "password", UserRole.OWNER);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.isTokenValid(token)).thenReturn(true);
        when(jwtUtil.extractUsername(token)).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals(userDetails, authentication.getPrincipal());
        assertTrue(authentication.isAuthenticated());
    }

    @Test
    void doFilterInternal_validToken_setsCorrectAuthorities() throws Exception {
        var token = "valid-jwt-token";
        var email = "user@example.com";
        var userDetails = new AuthenticatedUser(1L, 1L, email, "Test User", "password", UserRole.MEMBER);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.isTokenValid(token)).thenReturn(true);
        when(jwtUtil.extractUsername(token)).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);

        jwtFilter.doFilterInternal(request, response, filterChain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals(userDetails.getAuthorities(), authentication.getAuthorities());
    }

    @Test
    void doFilterInternal_emptyBearerToken_continuesWithoutAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer ");
        when(jwtUtil.isTokenValid("")).thenReturn(false);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_bearerWithOnlySpace_extractsEmptyToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer  ");
        when(jwtUtil.isTokenValid(" ")).thenReturn(false);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtUtil).isTokenValid(" ");
    }

    @Test
    void doFilterInternal_alwaysCallsFilterChain() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_validToken_setsWebAuthenticationDetails() throws Exception {
        var token = "valid-jwt-token";
        var email = "user@example.com";
        var userDetails = new AuthenticatedUser(1L, 1L, email, "Test User", "password", UserRole.OWNER);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.isTokenValid(token)).thenReturn(true);
        when(jwtUtil.extractUsername(token)).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getSession(false)).thenReturn(null);

        jwtFilter.doFilterInternal(request, response, filterChain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertNotNull(authentication.getDetails());
    }
}
