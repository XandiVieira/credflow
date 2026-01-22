package com.relyon.credflow.service;

import com.relyon.credflow.configuration.JwtUtil;
import com.relyon.credflow.model.user.AuthRequest;
import com.relyon.credflow.model.user.AuthenticatedUser;
import com.relyon.credflow.model.user.User;
import com.relyon.credflow.model.user.UserRequestDTO;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserService userService;
    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;

    public User register(UserRequestDTO user) {
        log.info("Registering user: {}", user.getEmail());
        return userService.create(user);
    }

    public Map<String, String> login(AuthRequest authRequest) {
        log.info("Iniciando autenticação para o e-mail: {}", authRequest.getEmail());

        var authInput = new UsernamePasswordAuthenticationToken(authRequest.getEmail(), authRequest.getPassword());
        var authentication = authManager.authenticate(authInput);
        log.info("Autenticação realizada com sucesso para: {}", authRequest.getEmail());

        var authenticatedUser = (AuthenticatedUser) authentication.getPrincipal();
        log.info("Usuário autenticado: id={}, email={}, nome={}",
                authenticatedUser.getUserId(),
                authenticatedUser.getEmail(),
                authenticatedUser.getUsername());

        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.info("Contexto de segurança atualizado com a autenticação atual");

        var token = jwtUtil.generateToken(authenticatedUser.getUsername());
        log.info("Token JWT gerado com sucesso para: {}", authenticatedUser.getUsername());

        return Map.of("token", token);
    }
}