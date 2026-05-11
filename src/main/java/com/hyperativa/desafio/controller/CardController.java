package com.hyperativa.desafio.controller;

import com.hyperativa.desafio.dto.BatchUploadResponse;
import com.hyperativa.desafio.dto.CardRequest;
import com.hyperativa.desafio.dto.CardResponse;
import com.hyperativa.desafio.exception.InvalidBatchFileException;
import com.hyperativa.desafio.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;

@RestController
@RequestMapping("/api/v1/cards")
@Tag(name = "Cartoes")
@SecurityRequirement(name = "bearerAuth")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @PostMapping
    @Operation(summary = "Cadastra um unico cartao")
    public ResponseEntity<CardResponse> create(@Valid @RequestBody CardRequest request) {
        CardResponse response = cardService.registerSingle(request.cardNumber());
        return ResponseEntity
                .created(URI.create("/api/v1/cards/" + response.id()))
                .body(response);
    }

    @PostMapping(value = "/batch", consumes = "multipart/form-data")
    @Operation(summary = "Cadastra cartoes a partir de arquivo TXT")
    public ResponseEntity<BatchUploadResponse> uploadBatch(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidBatchFileException("Arquivo nao informado ou vazio");
        }
        try {
            BatchUploadResponse response = cardService.uploadFile(file.getInputStream());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IOException e) {
            throw new InvalidBatchFileException("Falha ao ler arquivo: " + e.getMessage());
        }
    }

    @GetMapping("/{cardNumber}")
    @Operation(summary = "Consulta a existencia de um cartao e retorna seu identificador")
    public CardResponse find(@PathVariable String cardNumber) {
        return cardService.find(cardNumber);
    }
}
