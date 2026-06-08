package com.example.demo.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.demo.model.Oferta;

public interface OfertaRepository extends MongoRepository<Oferta, String> {

    // Busca ofertas de um mercado específico
    List<Oferta> findByMercadoId(String mercadoId);

    // Busca ofertas de vários mercados (para ofertas favoritas)
    List<Oferta> findByMercadoIdIn(List<String> mercadoIds);
}