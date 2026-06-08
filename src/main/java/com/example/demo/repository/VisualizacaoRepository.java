package com.example.demo.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.demo.model.Visualizacao;

public interface VisualizacaoRepository extends MongoRepository<Visualizacao, String> {
    List<Visualizacao> findByIdOferta(String idOferta);
    long countByIdOferta(String idOferta);
    List<Visualizacao> findByIdOfertaAndOrigem(String idOferta, String origem);
}
