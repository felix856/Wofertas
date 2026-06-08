package com.example.demo.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.demo.model.Curtida;

public interface CurtidaRepository extends MongoRepository<Curtida, String> {
    List<Curtida> findByIdOferta(String idOferta);
    List<Curtida> findByIdUsuario(String idUsuario);
    long countByIdOferta(String idOferta);
    boolean existsByIdOfertaAndIdUsuario(String idOferta, String idUsuario);
    void deleteByIdOfertaAndIdUsuario(String idOferta, String idUsuario);
}
