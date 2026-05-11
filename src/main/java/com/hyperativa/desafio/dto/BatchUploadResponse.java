package com.hyperativa.desafio.dto;

import java.util.UUID;

public record BatchUploadResponse(
        UUID batchId,
        String batchNumber,
        int totalRecords,
        int inserted,
        int duplicates) {
}
