package com.example.demo.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.demo.model.UsageCounter;

public interface UsageCounterRepository extends MongoRepository<UsageCounter, String> {
    Optional<UsageCounter> findByMercadoIdAndPeriodTypeAndPeriodStart(String mercadoId, String periodType, LocalDate periodStart);
}
