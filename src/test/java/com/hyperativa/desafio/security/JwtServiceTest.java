package com.hyperativa.desafio.security;

import com.hyperativa.desafio.config.AppProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private final AppProperties props = new AppProperties(
            new AppProperties.Security(new AppProperties.Jwt(
                    "test-secret-test-secret-test-secret-test-secret-32bytes-min",
                    60,
                    "desafio-hyperativa-test")),
            new AppProperties.Card("test-pepper"));
    private final JwtService service = new JwtService(props);

    @Test
    void generatedTokenIsParseable() {
        String token = service.generateToken("admin", "ADMIN");
        var claims = service.parse(token);
        assertThat(claims).isPresent();
        assertThat(claims.get().getSubject()).isEqualTo("admin");
        assertThat(claims.get().get("role", String.class)).isEqualTo("ADMIN");
        assertThat(claims.get().getIssuer()).isEqualTo("desafio-hyperativa-test");
    }

    @Test
    void invalidTokenReturnsEmpty() {
        assertThat(service.parse("not-a-valid-token")).isEmpty();
    }

    @Test
    void tamperedTokenReturnsEmpty() {
        String token = service.generateToken("admin", "ADMIN");
        String tampered = token.substring(0, token.length() - 4) + "AAAA";
        assertThat(service.parse(tampered)).isEmpty();
    }
}
