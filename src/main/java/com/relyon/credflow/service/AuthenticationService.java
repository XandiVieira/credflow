package com.relyon.credflow.service;

import com.relyon.credflow.configuration.JwtUtil;
import com.relyon.credflow.model.user.AuthRequest;
import com.relyon.credflow.model.user.User;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserService userService;
    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;

    public User register(User user) {
        log.info("Registering user: {}", user.getEmail());
        return userService.create(user);
    }

    public Map<String, String> login(AuthRequest authRequest) {
        log.info("Authenticating user: {}", authRequest.getEmail());
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.getEmail(), authRequest.getPassword())
        );
        var token = jwtUtil.generateToken(authRequest.getEmail());
        return Map.of("token", token);
    }
}