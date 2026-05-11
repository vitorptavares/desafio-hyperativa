package com.hyperativa.desafio.service;

import com.hyperativa.desafio.exception.InvalidBatchFileException;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser para arquivo TXT de lote.
 *
 * Header (linha 1)
 *   [01-29] NOME           (29 chars, padded com espacos)
 *   [30-37] DATA           (8 chars, yyyyMMdd)
 *   [38-45] LOTE           (8 chars, ex: LOTE0001)
 *   [46-51] QTD REGISTROS  (6 chars, numerico)
 *
 * Linhas de cartao
 *   [01-01] IDENTIFICADOR  ('C')
 *   [02-07] SEQUENCIA      (6 chars)
 *   [08-26] CARTAO         (19 chars, numerico padded a esquerda com espacos a direita)
 *
 * Trailer (ultima linha)
 *   [01-08] LOTE
 *   [09-14] QTD REGISTROS
 */
@Component
public class BatchFileParser {

    private static final DateTimeFormatter HEADER_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    public ParsedBatch parse(InputStream input) {
        List<String> lines = readAllLines(input);
        if (lines.size() < 3) {
            throw new InvalidBatchFileException("Arquivo deve conter header, ao menos um cartao e trailer");
        }

        Header header = parseHeader(lines.getFirst());
        Trailer trailer = parseTrailer(lines.getLast());

        if (!header.batchNumber().equals(trailer.batchNumber())) {
            throw new InvalidBatchFileException(
                    "Numero do lote difere entre header (%s) e trailer (%s)"
                            .formatted(header.batchNumber(), trailer.batchNumber()));
        }
        if (header.recordCount() != trailer.recordCount()) {
            throw new InvalidBatchFileException(
                    "Quantidade de registros difere entre header (%d) e trailer (%d)"
                            .formatted(header.recordCount(), trailer.recordCount()));
        }

        List<String> bodyLines = lines.subList(1, lines.size() - 1);
        if (bodyLines.size() != header.recordCount()) {
            throw new InvalidBatchFileException(
                    "Quantidade declarada (%d) difere do numero de linhas de cartao (%d)"
                            .formatted(header.recordCount(), bodyLines.size()));
        }

        List<CardRecord> records = new ArrayList<>(bodyLines.size());
        for (int i = 0; i < bodyLines.size(); i++) {
            records.add(parseCardLine(bodyLines.get(i), i + 2));
        }
        return new ParsedBatch(header, records);
    }

    private List<String> readAllLines(InputStream input) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    lines.add(line);
                }
            }
            return lines;
        } catch (IOException e) {
            throw new InvalidBatchFileException("Falha ao ler arquivo: " + e.getMessage());
        }
    }

    private Header parseHeader(String line) {
        if (line.length() < 51) {
            throw new InvalidBatchFileException("Header deve possuir ao menos 51 colunas");
        }
        String name = line.substring(0, 29).trim();
        String rawDate = line.substring(29, 37).trim();
        String batchNumber = line.substring(37, 45).trim();
        String rawCount = line.substring(45, 51).trim();

        LocalDate batchDate;
        try {
            batchDate = LocalDate.parse(rawDate, HEADER_DATE);
        } catch (DateTimeParseException ex) {
            throw new InvalidBatchFileException("Data do header invalida: " + rawDate);
        }
        int count = parsePositiveInt(rawCount, "Quantidade de registros do header");
        return new Header(name, batchDate, batchNumber, count);
    }

    private Trailer parseTrailer(String line) {
        if (line.length() < 14) {
            throw new InvalidBatchFileException("Trailer deve possuir ao menos 14 colunas");
        }
        String batchNumber = line.substring(0, 8).trim();
        int count = parsePositiveInt(line.substring(8, 14).trim(), "Quantidade de registros do trailer");
        return new Trailer(batchNumber, count);
    }

    private CardRecord parseCardLine(String line, int lineNumber) {
        if (line.length() < 9) {
            throw new InvalidBatchFileException(
                    "Linha %d muito curta para conter numero de cartao".formatted(lineNumber));
        }
        char identifier = line.charAt(0);
        if (identifier != 'C') {
            throw new InvalidBatchFileException(
                    "Linha %d nao inicia com identificador 'C'".formatted(lineNumber));
        }
        String sequence = line.substring(1, Math.min(7, line.length())).trim();
        String card = line.substring(7, Math.min(26, line.length())).trim();
        if (card.isEmpty()) {
            throw new InvalidBatchFileException(
                    "Linha %d nao contem numero de cartao".formatted(lineNumber));
        }
        return new CardRecord(sequence, card);
    }

    private int parsePositiveInt(String value, String fieldDescription) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed <= 0) {
                throw new InvalidBatchFileException(fieldDescription + " deve ser positivo");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new InvalidBatchFileException(fieldDescription + " invalida: " + value);
        }
    }

    public record Header(String name, LocalDate date, String batchNumber, int recordCount) {}
    public record Trailer(String batchNumber, int recordCount) {}
    public record CardRecord(String sequenceNumber, String cardNumber) {}
    public record ParsedBatch(Header header, List<CardRecord> records) {}
}
