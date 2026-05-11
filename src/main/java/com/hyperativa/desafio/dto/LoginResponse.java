package com.hyperativa.desafio.dto;

public record LoginResponse(String accessToken, String tokenType, long expiresIn) {
}
