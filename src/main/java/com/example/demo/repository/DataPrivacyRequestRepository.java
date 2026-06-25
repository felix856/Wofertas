package com.example.demo.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.demo.model.DataPrivacyRequest;

public interface DataPrivacyRequestRepository extends MongoRepository<DataPrivacyRequest, String> {
    List<DataPrivacyRequest> findByRequesterIdOrderByRequestedAtDesc(String requesterId);
    List<DataPrivacyRequest> findByEmailOrderByRequestedAtDesc(String email);
}
