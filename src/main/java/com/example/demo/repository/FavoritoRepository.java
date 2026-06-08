package com.example.demo.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Favorito;

@Repository
public interface FavoritoRepository extends MongoRepository<Favorito, String> {

    // Nomes corrigidos para bater com 'idUsuario' e 'idMercado'
    List<Favorito> findByIdUsuario(String idUsuario);
    List<Favorito> findByIdMercado(String idMercado);

    boolean existsByIdUsuarioAndIdMercado(String idUsuario, String idMercado);

    void deleteByIdUsuarioAndIdMercado(String idUsuario, String idMercado);

    long countByIdMercado(String idMercado);
}
