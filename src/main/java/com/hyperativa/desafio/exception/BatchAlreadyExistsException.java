package com.hyperativa.desafio.exception;

public class BatchAlreadyExistsException extends RuntimeException {
    public BatchAlreadyExistsException(String batchNumber) {
        super("Lote ja registrado: " + batchNumber);
    }
}
