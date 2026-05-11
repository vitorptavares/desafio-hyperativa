package com.hyperativa.desafio.service;

import com.hyperativa.desafio.config.AppProperties;
import com.hyperativa.desafio.domain.entity.Batch;
import com.hyperativa.desafio.domain.entity.Card;
import com.hyperativa.desafio.domain.repository.BatchRepository;
import com.hyperativa.desafio.domain.repository.CardRepository;
import com.hyperativa.desafio.exception.BatchAlreadyExistsException;
import com.hyperativa.desafio.exception.CardNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CardServiceTest {

    private CardRepository cards;
    private BatchRepository batches;
    private CardService service;

    @BeforeEach
    void setUp() {
        cards = mock(CardRepository.class);
        batches = mock(BatchRepository.class);
        AppProperties props = new AppProperties(
                new AppProperties.Security(new AppProperties.Jwt("x", 1, "i")),
                new AppProperties.Card("test-pepper"));
        CardHashService hashService = new CardHashService(props);
        service = new CardService(cards, batches, hashService, new BatchFileParser());

        Map<String, Card> persisted = new HashMap<>();
        when(cards.findByCardHash(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(inv -> Optional.ofNullable(persisted.get(inv.getArgument(0, String.class))));
        when(cards.existsByCardHash(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(inv -> persisted.containsKey(inv.getArgument(0, String.class)));
        when(cards.save(org.mockito.ArgumentMatchers.any(Card.class)))
                .thenAnswer(inv -> {
                    Card c = inv.getArgument(0, Card.class);
                    if (c.getId() == null) c.setId(UUID.randomUUID());
                    if (c.getCreatedAt() == null) c.setCreatedAt(OffsetDateTime.now());
                    persisted.put(c.getCardHash(), c);
                    return c;
                });
        when(batches.save(org.mockito.ArgumentMatchers.any(Batch.class)))
                .thenAnswer(inv -> {
                    Batch b = inv.getArgument(0, Batch.class);
                    if (b.getId() == null) b.setId(UUID.randomUUID());
                    return b;
                });
        when(batches.findByBatchNumber(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.empty());
    }

    @Test
    void registerSingleIsIdempotent() {
        var first = service.registerSingle("4456897999999999");
        var second = service.registerSingle("4456 8979 9999 9999");
        assertThat(first.id()).isEqualTo(second.id());
    }

    @Test
    void findReturnsExistingCard() {
        var registered = service.registerSingle("4456897999999999");
        var found = service.find("4456897999999999");
        assertThat(found.id()).isEqualTo(registered.id());
    }

    @Test
    void findThrowsWhenAbsent() {
        assertThatThrownBy(() -> service.find("4456897900000000"))
                .isInstanceOf(CardNotFoundException.class);
    }

    @Test
    void uploadFileInsertsAndDeduplicates() {
        String content = String.join("\n",
                pad("DESAFIO-HYPERATIVA", 29) + "20180524" + "LOTE0099" + "000003",
                "C" + pad("1", 6) + pad("4456897999999999", 19),
                "C" + pad("2", 6) + pad("4456897999999999", 19),
                "C" + pad("3", 6) + pad("4456897922969999", 19),
                "LOTE0099" + "000003");
        var response = service.uploadFile(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
        assertThat(response.totalRecords()).isEqualTo(3);
        assertThat(response.inserted()).isEqualTo(2);
        assertThat(response.duplicates()).isEqualTo(1);
        assertThat(response.batchNumber()).isEqualTo("LOTE0099");
    }

    @Test
    void uploadFileRejectsDuplicateBatch() {
        when(batches.findByBatchNumber("LOTE0099"))
                .thenReturn(Optional.of(Batch.builder().batchNumber("LOTE0099").build()));
        String content = String.join("\n",
                pad("DESAFIO-HYPERATIVA", 29) + "20180524" + "LOTE0099" + "000001",
                "C" + pad("1", 6) + pad("4456897999999999", 19),
                "LOTE0099" + "000001");
        assertThatThrownBy(() ->
                service.uploadFile(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(BatchAlreadyExistsException.class);
    }

    private static String pad(String value, int length) {
        return value + " ".repeat(Math.max(0, length - value.length()));
    }
}
