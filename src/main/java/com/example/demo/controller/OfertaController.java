package com.example.demo.controller;

import java.util.List;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.OfertaDTO;
import com.example.demo.dto.OfertaRequest;
import com.example.demo.security.CustomUserDetails;
import com.example.demo.service.OfertaService;

@RestController
@RequestMapping({"/ofertas", "/api/ofertas"})
public class OfertaController {

    private static final Logger logger = LoggerFactory.getLogger(OfertaController.class);

    @Autowired private OfertaService ofertaService;

    @PostMapping
    public ResponseEntity<OfertaDTO> criar(@Valid @RequestBody OfertaRequest dto, @AuthenticationPrincipal CustomUserDetails user) {
        try {
            if (user == null) {
                throw new IllegalArgumentException("Usuário não autenticado");
            }
            logger.info("Criando nova oferta para mercado: {}", user.getId());
            OfertaDTO criada = ofertaService.criar(dto, user.getId());
            logger.info("Oferta criada com sucesso: {}", criada.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(criada);
        } catch (IllegalArgumentException e) {
            logger.warn("Erro ao criar oferta: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Erro inesperado ao criar oferta", e);
            throw new RuntimeException("Erro ao criar oferta: " + e.getMessage(), e);
        }
    }

    @GetMapping
    public List<OfertaDTO> listar(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size,
                                  @RequestParam(required = false) Boolean ativo) {
        try {
            logger.debug("Listando todas as ofertas");
            return ofertaService.listar(ativo);
        } catch (Exception e) {
            logger.error("Erro ao listar ofertas", e);
            throw new RuntimeException("Erro ao listar ofertas: " + e.getMessage(), e);
        }
    }

    @GetMapping("/proximas")
    public List<OfertaDTO> listarProximas(@RequestParam double lat,
                                          @RequestParam double lng,
                                          @RequestParam(defaultValue = "10") double raioKm,
                                          @RequestParam(required = false) Boolean ativo) {
        return ofertaService.listarProximas(lat, lng, raioKm, ativo);
    }

    @GetMapping("/favoritas")
    public List<OfertaDTO> listarFavoritas(@AuthenticationPrincipal CustomUserDetails user) {
        try {
            if (user == null) {
                throw new IllegalArgumentException("Usuário não autenticado");
            }
            logger.debug("Listando ofertas favoritas para usuário: {}", user.getId());
            return ofertaService.listarPorFavoritos(user.getId());
        } catch (IllegalArgumentException e) {
            logger.error("Erro ao listar ofertas favoritas", e);
            throw e;
        }
    }

    @GetMapping("/historico")
    public List<OfertaDTO> listarMinhas(@AuthenticationPrincipal CustomUserDetails user) {
        try {
            if (user == null) {
                throw new IllegalArgumentException("Usuário não autenticado");
            }
            logger.debug("Listando ofertas do mercado: {}", user.getId());
            return ofertaService.listarPorMercado(user.getId());
        } catch (IllegalArgumentException e) {
            logger.error("Erro ao listar ofertas do mercado", e);
            throw e;
        }
    }

    @GetMapping("/mercado/{mercadoId}")
    public List<OfertaDTO> listarPorMercado(@PathVariable String mercadoId,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size,
                                            @RequestParam(required = false) Boolean ativo) {
        return ofertaService.listarPorMercado(mercadoId);
    }

    @GetMapping("/{id}")
    public OfertaDTO buscarPorId(@PathVariable String id) {
        try {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("ID da oferta não pode estar vazio");
            }
            logger.debug("Buscando oferta com ID: {}", id);
            OfertaDTO oferta = ofertaService.buscarPorId(id);
            if (oferta == null) {
                logger.warn("Oferta não encontrada: {}", id);
                throw new RuntimeException("Oferta não encontrada");
            }
            return oferta;
        } catch (RuntimeException e) {
            logger.error("Erro ao buscar oferta {}", id, e);
            throw e;
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable String id, @AuthenticationPrincipal CustomUserDetails user) {
        try {
            if (user == null) {
                throw new IllegalArgumentException("Usuário não autenticado");
            }
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("ID da oferta não pode estar vazio");
            }
            logger.info("Deletando oferta: {} para mercado: {}", id, user.getId());
            ofertaService.deletar(id, user.getId());
            logger.info("Oferta deletada com sucesso: {}", id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            logger.error("Erro ao deletar oferta {}", id, e);
            throw e;
        }
    }

    @PutMapping("/{id}")
    public OfertaDTO atualizar(@PathVariable String id, @Valid @RequestBody OfertaRequest dto, @AuthenticationPrincipal CustomUserDetails user) {
        try {
            if (user == null) {
                throw new IllegalArgumentException("Usuário não autenticado");
            }
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("ID da oferta não pode estar vazio");
            }
            logger.info("Atualizando oferta: {} para mercado: {}", id, user.getId());
            OfertaDTO atualizada = ofertaService.atualizar(id, dto, user.getId());
            logger.info("Oferta atualizada com sucesso: {}", id);
            return atualizada;
        } catch (IllegalArgumentException e) {
            logger.error("Erro ao atualizar oferta {}", id, e);
            throw e;
        }
    }

    @PostMapping("/{id}/imagem")
    public ResponseEntity<OfertaDTO> uploadImagem(@PathVariable String id,
                                                  @RequestParam(value = "foto", required = false) MultipartFile foto,
                                                  @RequestParam(value = "imagem", required = false) MultipartFile imagem) {
        try {
            MultipartFile file = foto != null ? foto : imagem;
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("ID da oferta não pode estar vazio");
            }
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("Arquivo de imagem não pode estar vazio");
            }
            logger.info("Upload de imagem para oferta: {}", id);
            OfertaDTO oferta = ofertaService.salvarImagem(id, file);
            logger.info("Imagem da oferta salva com sucesso: {}", id);
            return ResponseEntity.ok(oferta);
        } catch (IllegalArgumentException e) {
            logger.warn("Erro ao fazer upload de imagem: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Erro inesperado ao fazer upload de imagem para oferta {}", id, e);
            throw new RuntimeException("Erro ao fazer upload de imagem: " + e.getMessage(), e);
        }
    }
}
