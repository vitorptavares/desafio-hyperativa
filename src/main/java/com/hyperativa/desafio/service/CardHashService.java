package com.hyperativa.desafio.service;

import com.hyperativa.desafio.config.AppProperties;
import com.hyperativa.desafio.exception.InvalidCardNumberException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class CardHashService {

    private static final int MIN_DIGITS = 13;
    private static final int MAX_DIGITS = 19;

    private final String pepper;

    public CardHashService(AppProperties props) {
        this.pepper = props.card().pepper();
    }

    public String normalize(String rawCardNumber) {
        if (rawCardNumber == null) {
            throw new InvalidCardNumberException("Numero do cartao nao informado");
        }
        String digits = rawCardNumber.replaceAll("\\D", "");
        if (digits.length() < MIN_DIGITS || digits.length() > MAX_DIGITS) {
            throw new InvalidCardNumberException(
                    "Numero do cartao deve conter entre %d e %d digitos".formatted(MIN_DIGITS, MAX_DIGITS));
        }
        return digits;
    }

    public String hash(String rawCardNumber) {
        String normalized = normalize(rawCardNumber);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(pepper.getBytes(StandardCharsets.UTF_8));
            byte[] result = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(result);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponivel na JVM", e);
        }
    }
}
