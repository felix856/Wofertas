package com.example.demo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.EncarteDTO;
import com.example.demo.security.CustomUserDetails;
import com.example.demo.service.EncarteService;

@RestController
@RequestMapping({"/encartes", "/api/encartes"})
@CrossOrigin(origins = "*")
public class EncarteController {

    private final EncarteService encarteService;

    public EncarteController(EncarteService encarteService) {
        this.encarteService = encarteService;
    }

    @PostMapping
    public ResponseEntity<EncarteDTO> upload(
            @RequestParam String mercadoId,
            @RequestParam String titulo,
            @RequestParam("pdf") MultipartFile pdf,
            @AuthenticationPrincipal CustomUserDetails principal) {
        String mercadoLogadoId = mercadoAutenticadoId(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(encarteService.salvar(mercadoLogadoId, titulo, pdf));
    }

    @GetMapping("/mercado/{mercadoId}")
    public List<EncarteDTO> listarPorMercado(@PathVariable String mercadoId) {
        return encarteService.listarPorMercado(mercadoId);
    }

    @GetMapping("/{id}")
    public EncarteDTO buscarPorId(@PathVariable String id) {
        return encarteService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EncarteDTO> atualizar(@PathVariable String id,
                                                @RequestParam String titulo,
                                                @RequestParam(value = "pdf", required = false) MultipartFile pdf,
                                                @AuthenticationPrincipal CustomUserDetails principal) {
        String mercadoLogadoId = mercadoAutenticadoId(principal);
        return ResponseEntity.ok(encarteService.atualizar(id, titulo, pdf, mercadoLogadoId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable String id,
                                        @AuthenticationPrincipal CustomUserDetails principal) {
        String mercadoLogadoId = mercadoAutenticadoId(principal);
        encarteService.deletar(id, mercadoLogadoId);
        return ResponseEntity.ok().build();
    }

    private String mercadoAutenticadoId(CustomUserDetails principal) {
        if (principal == null || !"MERCADO".equalsIgnoreCase(principal.getTipo())) {
            throw new RuntimeException("Permissao negada para gerenciar encartes");
        }
        return principal.getId();
    }
}
