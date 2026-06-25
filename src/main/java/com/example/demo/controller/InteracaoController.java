package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.Visualizacao;
import com.example.demo.repository.VisualizacaoRepository;

@RestController
@RequestMapping("/api/interacoes")
@CrossOrigin(origins = "*")
public class InteracaoController {

    @Autowired private VisualizacaoRepository vizRepo;

    @PostMapping("/visualizar/{ofertaId}")
    public ResponseEntity<Void> registrarVisualizacao(
            @PathVariable String ofertaId,
            @RequestParam(required = false) String idUsuario,
            @RequestParam(defaultValue = "DASHBOARD") String origem) {
        // Salva no banco que alguém viu a oferta X vindo da origem Y (Android/Web)
        vizRepo.save(new Visualizacao(ofertaId, idUsuario, origem));
        return ResponseEntity.ok().build();
    }
}
