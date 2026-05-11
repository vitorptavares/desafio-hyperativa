package com.hyperativa.desafio.security;

import com.hyperativa.desafio.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {

    private final AppProperties.Jwt jwtProps;
    private final SecretKey signingKey;

    public JwtService(AppProperties props) {
        this.jwtProps = props.security().jwt();
        this.signingKey = Keys.hmacShaKeyFor(jwtProps.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String username, String role) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(jwtProps.expirationMinutes(), ChronoUnit.MINUTES);
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuer(jwtProps.issuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    public Optional<Claims> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public long expirationSeconds() {
        return jwtProps.expirationMinutes() * 60L;
    }
}
