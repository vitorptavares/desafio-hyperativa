package com.hyperativa.desafio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Security security, Card card) {

    public record Security(Jwt jwt) {}

    public record Jwt(String secret, long expirationMinutes, String issuer) {}

    public record Card(String pepper) {}
}
