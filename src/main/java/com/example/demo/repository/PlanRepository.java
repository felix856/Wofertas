package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.demo.model.Plan;
import com.example.demo.model.PlanName;

public interface PlanRepository extends MongoRepository<Plan, String> {
    Optional<Plan> findByName(PlanName name);
    boolean existsByName(PlanName name);
}
