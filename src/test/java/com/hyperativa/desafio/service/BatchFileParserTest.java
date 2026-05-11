package com.hyperativa.desafio.service;

import com.hyperativa.desafio.exception.InvalidBatchFileException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BatchFileParserTest {

    private final BatchFileParser parser = new BatchFileParser();

    private static String pad(String value, int length) {
        if (value.length() >= length) return value.substring(0, length);
        return value + " ".repeat(length - value.length());
    }

    private static String header(String name, String date, String batchNumber, String count) {
        return pad(name, 29) + date + batchNumber + count;
    }

    private static String cardLine(String seq, String card) {
        return "C" + pad(seq, 6) + pad(card, 19);
    }

    private static String trailer(String batchNumber, String count) {
        return batchNumber + count;
    }

    private static ByteArrayInputStream toStream(String... lines) {
        return new ByteArrayInputStream(String.join("\n", lines).getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void parsesValidFile() {
        var input = toStream(
                header("DESAFIO-HYPERATIVA", "20180524", "LOTE0001", "000002"),
                cardLine("1", "4456897999999999"),
                cardLine("2", "4456897922969999"),
                trailer("LOTE0001", "000002"));

        var parsed = parser.parse(input);

        assertThat(parsed.header().name()).isEqualTo("DESAFIO-HYPERATIVA");
        assertThat(parsed.header().date()).isEqualTo(LocalDate.of(2018, 5, 24));
        assertThat(parsed.header().batchNumber()).isEqualTo("LOTE0001");
        assertThat(parsed.header().recordCount()).isEqualTo(2);
        assertThat(parsed.records()).hasSize(2);
        assertThat(parsed.records().getFirst().cardNumber()).isEqualTo("4456897999999999");
        assertThat(parsed.records().getFirst().sequenceNumber()).isEqualTo("1");
    }

    @Test
    void rejectsMismatchedBatchNumber() {
        var input = toStream(
                header("DESAFIO-HYPERATIVA", "20180524", "LOTE0001", "000001"),
                cardLine("1", "4456897999999999"),
                trailer("LOTE0002", "000001"));
        assertThatThrownBy(() -> parser.parse(input))
                .isInstanceOf(InvalidBatchFileException.class)
                .hasMessageContaining("Numero do lote difere");
    }

    @Test
    void rejectsMismatchedCount() {
        var input = toStream(
                header("DESAFIO-HYPERATIVA", "20180524", "LOTE0001", "000005"),
                cardLine("1", "4456897999999999"),
                trailer("LOTE0001", "000005"));
        assertThatThrownBy(() -> parser.parse(input))
                .isInstanceOf(InvalidBatchFileException.class)
                .hasMessageContaining("Quantidade declarada");
    }

    @Test
    void rejectsLineWithoutCIdentifier() {
        var input = toStream(
                header("DESAFIO-HYPERATIVA", "20180524", "LOTE0001", "000001"),
                "X" + pad("1", 6) + pad("4456897999999999", 19),
                trailer("LOTE0001", "000001"));
        assertThatThrownBy(() -> parser.parse(input))
                .isInstanceOf(InvalidBatchFileException.class)
                .hasMessageContaining("identificador 'C'");
    }

    @Test
    void rejectsInvalidHeaderDate() {
        var input = toStream(
                header("DESAFIO-HYPERATIVA", "ABCDEFGH", "LOTE0001", "000001"),
                cardLine("1", "4456897999999999"),
                trailer("LOTE0001", "000001"));
        assertThatThrownBy(() -> parser.parse(input))
                .isInstanceOf(InvalidBatchFileException.class)
                .hasMessageContaining("Data");
    }

    @Test
    void rejectsTooFewLines() {
        var input = toStream("apenas uma linha");
        assertThatThrownBy(() -> parser.parse(input))
                .isInstanceOf(InvalidBatchFileException.class);
    }
}
