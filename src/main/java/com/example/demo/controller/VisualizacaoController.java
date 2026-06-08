package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.service.VisualizacaoService;
import com.example.demo.model.Visualizacao;

@RestController
@RequestMapping({"/visualizacoes", "/api/visualizacoes"})
@CrossOrigin(origins = "*")
public class VisualizacaoController {

    @Autowired private VisualizacaoService visualizacaoService;

    @PostMapping("/registrar/{idOferta}")
    public ResponseEntity<Visualizacao> registrarVisualizacao(
            @PathVariable String idOferta,
            @RequestParam(required = false) String idUsuario,
            @RequestParam(defaultValue = "DASHBOARD") String origem) {
        Visualizacao visualizacao = visualizacaoService.registrarVisualizacao(idOferta, idUsuario, origem);
        return ResponseEntity.ok(visualizacao);
    }

    @GetMapping("/count/{idOferta}")
    public long contagemVisualizacoes(@PathVariable String idOferta) {
        return visualizacaoService.contagemVisualizacoes(idOferta);
    }
}
