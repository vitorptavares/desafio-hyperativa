package com.hyperativa.desafio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CardRequest(
        @NotBlank
        @Pattern(regexp = "^[\\d\\s-]{13,25}$", message = "Numero do cartao invalido")
        String cardNumber) {
}
