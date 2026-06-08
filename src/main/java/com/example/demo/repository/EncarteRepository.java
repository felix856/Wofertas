package com.example.demo.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.demo.model.Encarte;

public interface EncarteRepository extends MongoRepository<Encarte, String> {
    List<Encarte> findByMercadoIdOrderByDataCriacaoDesc(String mercadoId);
}
