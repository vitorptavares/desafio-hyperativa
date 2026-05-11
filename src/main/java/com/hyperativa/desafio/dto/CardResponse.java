package com.hyperativa.desafio.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CardResponse(UUID id, OffsetDateTime createdAt) {
}
