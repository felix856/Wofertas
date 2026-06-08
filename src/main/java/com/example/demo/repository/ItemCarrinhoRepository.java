package com.example.demo.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.demo.model.ItemCarrinho;

public interface ItemCarrinhoRepository extends MongoRepository<ItemCarrinho, String> {
    List<ItemCarrinho> findByMercadoId(String mercadoId);
    List<ItemCarrinho> findByIdOferta(String idOferta);
    List<ItemCarrinho> findByIdUsuario(String idUsuario);
    long countByMercadoId(String mercadoId);
    long countByIdOferta(String idOferta);
}
