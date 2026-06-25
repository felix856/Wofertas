package com.example.demo.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.demo.model.ConsumerSubscription;

public interface ConsumerSubscriptionRepository extends MongoRepository<ConsumerSubscription, String> {
    List<ConsumerSubscription> findByUserIdOrderByCreatedAtDesc(String userId);
}
