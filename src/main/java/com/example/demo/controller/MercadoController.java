package com.example.demo.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.MercadoDTO;
import com.example.demo.dto.MercadoUpdateDTO;
import com.example.demo.dto.PasswordChangeRequest;
import com.example.demo.model.Mercado;
import com.example.demo.service.MercadoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping({"/mercados", "/api/mercados"})
@CrossOrigin("*") // Libera o acesso para o seu Front-end HTML
public class MercadoController {

    private static final Logger logger = LoggerFactory.getLogger(MercadoController.class);

    @Autowired 
    private MercadoService mercadoService;

    @PostMapping
    public ResponseEntity<Mercado> cadastrar(@Valid @RequestBody MercadoDTO dto) {
        try {
            logger.info("Cadastro de novo mercado: {}", dto.getNome());
            
            Mercado m = new Mercado(
                    dto.getNome(),
                    dto.getCnpj(),
                    dto.getEndereco(),
                    dto.getTelefone(),
                    dto.getEmail(),
                    dto.getSenha(),
                    dto.getImagemLogo()
            );
            
            if (dto.getLatitude()  != null) m.setLatitude(dto.getLatitude());
            if (dto.getLongitude() != null) m.setLongitude(dto.getLongitude());
            
            Mercado criado = mercadoService.criar(m);
            logger.info("Mercado cadastrado com sucesso: {}", criado.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(criado);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Erro de validação ao cadastrar: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Erro inesperado ao cadastrar mercado", e);
            throw new RuntimeException("Erro ao cadastrar mercado: " + e.getMessage());
        }
    }

    @GetMapping
    public List<Mercado> listar() {
        return mercadoService.listar();
    }

    @GetMapping("/proximos")
    public List<Mercado> listarProximos(@RequestParam double lat,
                                        @RequestParam double lng,
                                        @RequestParam(defaultValue = "10") double raioKm) {
        return mercadoService.listarProximos(lat, lng, raioKm);
    }

    @GetMapping("/{id}")
    public Mercado buscar(@PathVariable String id) {
        return mercadoService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    public Mercado atualizar(@PathVariable String id, @Valid @RequestBody MercadoUpdateDTO dto) {
        try {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("ID do mercado não pode estar vazio");
            }
            
            logger.info("Atualizando mercado ID: {}", id);
            
            // Passa o DTO de atualização direto para o Service
            Mercado atualizado = mercadoService.atualizar(id, dto);
            
            logger.info("Mercado {} atualizado com sucesso", id);
            return atualizado;
            
        } catch (IllegalArgumentException e) {
            logger.error("Erro ao atualizar mercado {}", id, e);
            throw new RuntimeException("Erro ao atualizar: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable String id) {
        logger.info("Deletando mercado: {}", id);
        mercadoService.deletar(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/logo")
    public ResponseEntity<Mercado> uploadLogo(@PathVariable String id, @RequestParam("logo") MultipartFile file) {
        try {
            logger.info("Upload de logo para mercado: {}", id);
            Mercado mercado = mercadoService.salvarLogo(id, file);
            return ResponseEntity.ok(mercado);
        } catch (Exception e) {
            logger.error("Erro no upload de logo para {}", id, e);
            throw new RuntimeException("Erro no upload: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/senha")
    public ResponseEntity<Void> alterarSenha(@PathVariable String id, @Valid @RequestBody PasswordChangeRequest dto) {
        try {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("ID do mercado não pode estar vazio");
            }
            logger.info("Iniciando alteração de senha para mercado: {}", id);
            mercadoService.alterarSenha(id, dto.getSenhaAtual(), dto.getNovaSenha(), dto.getConfirmacao());
            logger.info("Senha do mercado {} alterada com sucesso", id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Erro ao alterar senha do mercado {}: {}", id, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Erro inesperado ao alterar senha do mercado {}", id, e);
            throw new RuntimeException("Erro ao alterar senha: " + e.getMessage(), e);
        }
    }
}
