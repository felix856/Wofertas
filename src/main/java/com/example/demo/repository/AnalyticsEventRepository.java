package com.example.demo.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.demo.model.AnalyticsEvent;

public interface AnalyticsEventRepository extends MongoRepository<AnalyticsEvent, String> {
    long countByEventTypeAndStoreId(String eventType, String storeId);
    long countByStoreId(String storeId);
    List<AnalyticsEvent> findTop100ByStoreIdOrderByCreatedAtDesc(String storeId);
}
