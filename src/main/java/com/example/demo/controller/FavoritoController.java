package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.Favorito;
import com.example.demo.security.CustomUserDetails;
import com.example.demo.service.FavoritoService;

@RestController
@RequestMapping({"/favoritos", "/api/favoritos"})
@CrossOrigin(origins = "*")
public class FavoritoController {

    @Autowired private FavoritoService favoritoService;

    @GetMapping("/check/{idMercado}")
    public boolean isFavorito(@PathVariable String idMercado,
                              @AuthenticationPrincipal CustomUserDetails principal) {
        return favoritoService.isFavorito(principal.getId(), idMercado);
    }

    @PostMapping("/toggle/{idMercado}")
    public Favorito toggleFavorito(@PathVariable String idMercado,
                                   @AuthenticationPrincipal CustomUserDetails principal) {
        String userId = principal.getId();
        if (favoritoService.isFavorito(userId, idMercado)) {
            favoritoService.desfavoritar(userId, idMercado);
            return new Favorito(userId, idMercado);
        }
        return favoritoService.favoritar(userId, idMercado);
    }

    @PostMapping("/{idMercado}")
    public Favorito adicionarFavorito(@PathVariable String idMercado,
                                      @AuthenticationPrincipal CustomUserDetails principal) {
        return favoritoService.favoritar(principal.getId(), idMercado);
    }

    @DeleteMapping("/{idMercado}")
    public ResponseEntity<Void> removerFavorito(@PathVariable String idMercado,
                                                @AuthenticationPrincipal CustomUserDetails principal) {
        favoritoService.desfavoritar(principal.getId(), idMercado);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public List<Favorito> meusFavoritos(@AuthenticationPrincipal CustomUserDetails principal) {
        return favoritoService.listarPorUsuario(principal.getId());
    }

    @GetMapping("/{idUsuario}")
    public List<Favorito> listar(@PathVariable String idUsuario) {
        return favoritoService.listarPorUsuario(idUsuario);
    }
}
