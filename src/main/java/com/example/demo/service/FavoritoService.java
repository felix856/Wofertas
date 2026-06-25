package com.example.demo.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.Favorito;
import com.example.demo.repository.FavoritoRepository;

@Service
public class FavoritoService {

    private static final Logger logger = LoggerFactory.getLogger(FavoritoService.class);

    @Autowired private FavoritoRepository favoritoRepository;

    public boolean isFavorito(String idUsuario, String idMercado) {
        if (idUsuario == null || idUsuario.isBlank() || idMercado == null || idMercado.isBlank()) {
            throw new IllegalArgumentException("IDs de usuário e mercado não podem estar vazios");
        }
        return favoritoRepository.existsByIdUsuarioAndIdMercado(idUsuario, idMercado);
    }

    @Transactional
    public Favorito favoritar(String idUsuario, String idMercado) {
        if (idUsuario == null || idUsuario.isBlank() || idMercado == null || idMercado.isBlank()) {
            throw new IllegalArgumentException("IDs de usuário e mercado não podem estar vazios");
        }

        if (favoritoRepository.existsByIdUsuarioAndIdMercado(idUsuario, idMercado)) {
            logger.warn("Favorito já existe: usuário {} para mercado {}", idUsuario, idMercado);
            throw new IllegalArgumentException("Este mercado já está nos seus favoritos");
        }

        try {
            logger.info("Favoritar: usuário {} marcou mercado {} como favorito", idUsuario, idMercado);
            Favorito novoFavorito = new Favorito(idUsuario, idMercado);
            return favoritoRepository.save(novoFavorito);
        } catch (Exception e) {
            logger.error("Erro ao favoritar mercado {} para usuário {}", idMercado, idUsuario, e);
            throw new RuntimeException("Erro ao adicionar aos favoritos: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void desfavoritar(String idUsuario, String idMercado) {
        if (idUsuario == null || idUsuario.isBlank() || idMercado == null || idMercado.isBlank()) {
            throw new IllegalArgumentException("IDs de usuário e mercado não podem estar vazios");
        }

        try {
            logger.info("Desfavoritar: usuário {} removeu mercado {} dos favoritos", idUsuario, idMercado);
            favoritoRepository.deleteByIdUsuarioAndIdMercado(idUsuario, idMercado);
        } catch (Exception e) {
            logger.error("Erro ao desfavoritar mercado {} para usuário {}", idMercado, idUsuario, e);
            throw new RuntimeException("Erro ao remover dos favoritos: " + e.getMessage(), e);
        }
    }

    public List<Favorito> listarPorUsuario(String idUsuario) {
        if (idUsuario == null || idUsuario.isBlank()) {
            throw new IllegalArgumentException("ID do usuário não pode estar vazio");
        }

        try {
            logger.debug("Listando favoritos para usuário: {}", idUsuario);
            List<Favorito> favoritos = favoritoRepository.findByIdUsuario(idUsuario);
            logger.debug("Total de favoritos: {} para usuário {}", favoritos.size(), idUsuario);
            return favoritos;
        } catch (Exception e) {
            logger.error("Erro ao listar favoritos do usuário {}", idUsuario, e);
            throw new RuntimeException("Erro ao listar favoritos: " + e.getMessage(), e);
        }
    }
}