package com.example.demo.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

import com.example.demo.dto.PasswordChangeRequest;
import com.example.demo.dto.UsuarioDTO;
import com.example.demo.model.Usuario;
import com.example.demo.service.UsuarioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping({"/usuarios", "/api/usuarios"})
public class UsuarioController {

    private static final Logger logger = LoggerFactory.getLogger(UsuarioController.class);

    @Autowired private UsuarioService usuarioService;

    @PostMapping
    public ResponseEntity<Usuario> criar(@Valid @RequestBody UsuarioDTO dto) {
        try {
            logger.info("Criando novo usuário com email: {}", dto.getEmail());
            Usuario u = new Usuario(dto.getNome(), dto.getEmail(), dto.getSenha());
            Usuario criado = usuarioService.criar(u);
            logger.info("Usuário criado com sucesso: {}", criado.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(criado);
        } catch (IllegalArgumentException e) {
            logger.warn("Erro ao criar usuário: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Erro inesperado ao criar usuário", e);
            throw new RuntimeException("Erro ao criar usuário: " + e.getMessage(), e);
        }
    }

    @GetMapping
    public List<Usuario> listar() {
        try {
            logger.debug("Listando todos os usuários");
            return usuarioService.listar();
        } catch (Exception e) {
            logger.error("Erro ao listar usuários", e);
            throw new RuntimeException("Erro ao listar usuários: " + e.getMessage(), e);
        }
    }

    @GetMapping("/{id}")
    public Usuario buscarPorId(@PathVariable String id) {
        try {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("ID do usuário não pode estar vazio");
            }
            logger.debug("Buscando usuário com ID: {}", id);
            Usuario usuario = usuarioService.buscarPorId(id);
            if (usuario == null) {
                logger.warn("Usuário não encontrado: {}", id);
                throw new RuntimeException("Usuário não encontrado");
            }
            return usuario;
        } catch (RuntimeException e) {
            logger.error("Erro ao buscar usuário {}", id, e);
            throw e;
        }
    }

@PutMapping("/{id}")
public ResponseEntity<Usuario> atualizar(@PathVariable String id, @Valid @RequestBody UsuarioDTO dto) {
    logger.info("Iniciando atualização do usuário: {}", id);
    
    // Converte DTO para Entidade (Pode ser feito via Mapper para ficar mais limpo)
    Usuario usuarioDados = new Usuario(dto.getNome(), dto.getEmail(), dto.getSenha());
    
    Usuario atualizado = usuarioService.atualizar(id, usuarioDados);
    
    logger.info("Usuário {} atualizado com sucesso", id);
    return ResponseEntity.ok(atualizado);
}

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable String id) {
        try {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("ID do usuário não pode estar vazio");
            }
            logger.info("Deletando usuário: {}", id);
            usuarioService.deletar(id);
            logger.info("Usuário deletado com sucesso: {}", id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            logger.error("Erro ao deletar usuário {}", id, e);
            throw e;
        }
    }

    @PostMapping("/{id}/foto")
    public ResponseEntity<Usuario> uploadFoto(@PathVariable String id, @RequestParam("foto") MultipartFile file) {
        try {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("ID do usuário não pode estar vazio");
            }
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("Arquivo de foto não pode estar vazio");
            }
            logger.info("Upload de foto para usuário: {}", id);
            Usuario usuario = usuarioService.salvarFoto(id, file);
            logger.info("Foto do usuário salva com sucesso: {}", id);
            return ResponseEntity.ok(usuario);
        } catch (IllegalArgumentException e) {
            logger.warn("Erro ao fazer upload de foto: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Erro inesperado ao fazer upload de foto para usuário {}", id, e);
            throw new RuntimeException("Erro ao fazer upload de foto: " + e.getMessage(), e);
        }
    }

    @PutMapping("/{id}/senha")
    public ResponseEntity<Void> alterarSenha(@PathVariable String id, @Valid @RequestBody PasswordChangeRequest dto) {
        try {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("ID do usuário não pode estar vazio");
            }
            logger.info("Iniciando alteração de senha para usuário: {}", id);
            usuarioService.alterarSenha(id, dto.getSenhaAtual(), dto.getNovaSenha(), dto.getConfirmacao());
            logger.info("Senha do usuário {} alterada com sucesso", id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Erro ao alterar senha do usuário {}: {}", id, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Erro inesperado ao alterar senha do usuário {}", id, e);
            throw new RuntimeException("Erro ao alterar senha: " + e.getMessage(), e);
        }
    }
}
