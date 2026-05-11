package com.hyperativa.desafio.service;

import com.hyperativa.desafio.domain.entity.Batch;
import com.hyperativa.desafio.domain.entity.Card;
import com.hyperativa.desafio.domain.repository.BatchRepository;
import com.hyperativa.desafio.domain.repository.CardRepository;
import com.hyperativa.desafio.dto.BatchUploadResponse;
import com.hyperativa.desafio.dto.CardResponse;
import com.hyperativa.desafio.exception.BatchAlreadyExistsException;
import com.hyperativa.desafio.exception.CardNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Service
public class CardService {

    private static final Logger log = LoggerFactory.getLogger(CardService.class);

    private final CardRepository cards;
    private final BatchRepository batches;
    private final CardHashService hashService;
    private final BatchFileParser parser;

    public CardService(CardRepository cards,
                       BatchRepository batches,
                       CardHashService hashService,
                       BatchFileParser parser) {
        this.cards = cards;
        this.batches = batches;
        this.hashService = hashService;
        this.parser = parser;
    }

    @Transactional
    public CardResponse registerSingle(String rawCardNumber) {
        String hash = hashService.hash(rawCardNumber);
        Card card = cards.findByCardHash(hash).orElseGet(() -> {
            Card newCard = Card.builder().cardHash(hash).build();
            Card saved = cards.saveAndFlush(newCard);
            log.info("Cartao registrado id={}", saved.getId());
            return saved;
        });
        log.info("Cartao retornado id={}", card.getId());
        return new CardResponse(card.getId(), card.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public CardResponse find(String rawCardNumber) {
        String hash = hashService.hash(rawCardNumber);
        Card card = cards.findByCardHash(hash).orElseThrow(CardNotFoundException::new);
        log.info("Cartao consultado id={}", card.getId());
        return new CardResponse(card.getId(), card.getCreatedAt());
    }

    @Transactional
    public BatchUploadResponse uploadFile(InputStream input) {
        var parsed = parser.parse(input);
        var header = parsed.header();

        batches.findByBatchNumber(header.batchNumber()).ifPresent(b -> {
            throw new BatchAlreadyExistsException(header.batchNumber());
        });

        Batch batch = batches.save(Batch.builder()
                .name(header.name())
                .batchDate(header.date() != null ? header.date() : LocalDate.now())
                .batchNumber(header.batchNumber())
                .recordCount(header.recordCount())
                .source(Batch.Source.FILE)
                .build());

        Set<String> seenInThisFile = new HashSet<>();
        int inserted = 0;
        int duplicates = 0;

        for (var record : parsed.records()) {
            String hash = hashService.hash(record.cardNumber());
            if (!seenInThisFile.add(hash) || cards.existsByCardHash(hash)) {
                duplicates++;
                continue;
            }
            cards.save(Card.builder()
                    .cardHash(hash)
                    .sequenceNumber(record.sequenceNumber())
                    .batch(batch)
                    .build());
            inserted++;
        }
        log.info("Lote {} processado: total={} inseridos={} duplicados={}",
                header.batchNumber(), parsed.records().size(), inserted, duplicates);
        return new BatchUploadResponse(
                batch.getId(),
                batch.getBatchNumber(),
                parsed.records().size(),
                inserted,
                duplicates);
    }
}
