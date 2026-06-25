package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.demo.model.Subscription;

public interface SubscriptionRepository extends MongoRepository<Subscription, String> {
    Optional<Subscription> findTopByMercadoIdOrderByCreatedAtDesc(String mercadoId);
    List<Subscription> findByMercadoIdOrderByCreatedAtDesc(String mercadoId);
}
