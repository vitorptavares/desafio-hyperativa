package com.hyperativa.desafio.service;

import com.hyperativa.desafio.domain.repository.AppUserRepository;
import com.hyperativa.desafio.dto.LoginRequest;
import com.hyperativa.desafio.dto.LoginResponse;
import com.hyperativa.desafio.exception.InvalidCredentialsException;
import com.hyperativa.desafio.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AppUserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        var user = users.findByUsername(request.username())
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        String token = jwtService.generateToken(user.getUsername(), user.getRole());
        return new LoginResponse(token, "Bearer", jwtService.expirationSeconds());
    }
}
