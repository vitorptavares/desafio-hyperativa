package com.hyperativa.desafio.exception;

public class CardNotFoundException extends RuntimeException {
    public CardNotFoundException() {
        super("Cartao nao encontrado");
    }
}
