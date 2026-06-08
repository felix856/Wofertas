package com.example.demo.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder   passwordEncoder;
    private final String UPLOAD_DIR = "uploads/perfis/";

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Usuario criar(Usuario usuario) {
        if (usuarioRepository.existsByEmail(usuario.getEmail())) {
            throw new RuntimeException("E-mail já cadastrado.");
        }
        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        return usuarioRepository.save(usuario);
    }

    public List<Usuario> listar() {
        return usuarioRepository.findAll();
    }

    public Usuario buscarPorId(String id) {
        Objects.requireNonNull(id, "ID do usuário não pode ser nulo");
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + id));
    }

    public Usuario atualizar(String id, Usuario novo) {
        Usuario u = buscarPorId(id);
        u.setNome(novo.getNome());
        if (novo.getEmail() != null) u.setEmail(novo.getEmail());
        if (novo.getSenha() != null && !novo.getSenha().isBlank()) {
            u.setSenha(passwordEncoder.encode(novo.getSenha()));
        }
        return usuarioRepository.save(u);
    }

    public void deletar(String id) {
        Objects.requireNonNull(id, "ID do usuário não pode ser nulo");
        usuarioRepository.deleteById(id);
    }

    public Usuario salvarFoto(String id, MultipartFile file) {
        Usuario u = buscarPorId(id);
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);

            // A URL que o App e o Web vão usar para acessar a imagem
            String fileUrl = "/uploads/perfis/" + fileName;
            u.setImagemPerfil(fileUrl);
            return usuarioRepository.save(u);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao salvar a foto: " + e.getMessage());
        }
    }

    public Usuario login(String email, String senha) {
        Usuario u = usuarioRepository.findByEmail(email);
        if (u == null) throw new RuntimeException("Usuário não encontrado");
        if (!passwordEncoder.matches(senha, u.getSenha())) {
            throw new RuntimeException("Credenciais inválidas");
        }
        return u;
    }

    /**
     * Altera a senha do usuário após validar a senha atual
     * @param id ID do usuário
     * @param senhaAtual Senha atual para validação
     * @param novaSenha Nova senha
     * @param confirmacao Confirmação da nova senha
     * @return Usuário com senha atualizada
     */
    public Usuario alterarSenha(String id, String senhaAtual, String novaSenha, String confirmacao) {
        Objects.requireNonNull(id, "ID do usuário não pode ser nulo");
        
        if (!novaSenha.equals(confirmacao)) {
            throw new IllegalArgumentException("Nova senha e confirmação não conferem");
        }
        
        if (novaSenha.length() < 6) {
            throw new IllegalArgumentException("Senha deve ter no mínimo 6 caracteres");
        }
        
        Usuario u = buscarPorId(id);
        
        if (!passwordEncoder.matches(senhaAtual, u.getSenha())) {
            throw new IllegalArgumentException("Senha atual está incorreta");
        }
        
        u.setSenha(passwordEncoder.encode(novaSenha));
        return usuarioRepository.save(u);
    }
}
