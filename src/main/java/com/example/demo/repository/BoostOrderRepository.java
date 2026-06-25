package com.example.demo.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.demo.model.BoostOrder;

public interface BoostOrderRepository extends MongoRepository<BoostOrder, String> {
    List<BoostOrder> findByStoreIdOrderByCreatedAtDesc(String storeId);
}
