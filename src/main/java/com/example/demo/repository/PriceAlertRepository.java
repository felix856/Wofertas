package com.example.demo.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.demo.model.PriceAlert;

public interface PriceAlertRepository extends MongoRepository<PriceAlert, String> {
    List<PriceAlert> findByUserIdAndActiveTrue(String userId);
}
