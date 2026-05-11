package com.hyperativa.desafio.domain.repository;

import com.hyperativa.desafio.domain.entity.Batch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BatchRepository extends JpaRepository<Batch, UUID> {
    Optional<Batch> findByBatchNumber(String batchNumber);
}
