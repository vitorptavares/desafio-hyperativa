package com.hyperativa.desafio.domain.repository;

import com.hyperativa.desafio.domain.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CardRepository extends JpaRepository<Card, UUID> {
    Optional<Card> findByCardHash(String cardHash);
    boolean existsByCardHash(String cardHash);
}
