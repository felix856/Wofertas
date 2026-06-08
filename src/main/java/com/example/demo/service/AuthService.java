package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.model.Mercado;
import com.example.demo.model.Usuario;
import com.example.demo.repository.MercadoRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.security.CustomUserDetails;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private MercadoRepository mercadoRepository;
    @Autowired private PasswordEncoder   passwordEncoder;
    @Autowired(required = false) private JavaMailSender    mailSender;

    public CustomUserDetails authenticateByEmailAndPassword(String email, String rawPassword) {
        logger.debug("Tentando autenticar usuario com email: {}", email);

        Usuario u = usuarioRepository.findByEmail(email);
        if (u != null) {
            if (matches(u.getSenha(), rawPassword)) {
                logger.info("Usuario autenticado com sucesso: {}", email);
                return new CustomUserDetails(
                    Objects.requireNonNullElse(u.getId(), ""),
                    u.getEmail(),
                    u.getSenha(),
                    "USUARIO"
                );
            }
            logger.warn("Senha incorreta para usuario: {}", email);
            throw new RuntimeException("Credenciais inválidas");
        }

        Mercado m = mercadoRepository.findByEmail(email);
        if (m != null) {
            if (matches(m.getSenha(), rawPassword)) {
                logger.info("Mercado autenticado com sucesso: {}", email);
                return new CustomUserDetails(
                    Objects.requireNonNullElse(m.getId(), ""),
                    m.getEmail(),
                    m.getSenha(),
                    "MERCADO"
                );
            }
            logger.warn("Senha incorreta para mercado: {}", email);
            throw new RuntimeException("Credenciais inválidas");
        }

        throw new RuntimeException("Usuário/Mercado não encontrado");
    }

    public void solicitarRecuperacaoSenha(String email) {
        String token = String.format("%06d", (int) (Math.random() * 1000000));
        LocalDateTime expiration = LocalDateTime.now().plusMinutes(30);

        Usuario u = usuarioRepository.findByEmail(email);
        if (u != null) {
            u.setResetToken(token);
            u.setResetTokenExpiration(expiration);
            usuarioRepository.save(u);
            enviarEmail(email, token);
            return;
        }

        Mercado m = mercadoRepository.findByEmail(email);
        if (m != null) {
            m.setResetToken(token);
            m.setResetTokenExpiration(expiration);
            mercadoRepository.save(m);
            enviarEmail(email, token);
            return;
        }

        throw new RuntimeException("E-mail não encontrado.");
    }

    public void redefinirSenha(String email, String token, String novaSenha) {
        Usuario u = usuarioRepository.findByEmail(email);
        if (u != null) {
            validarToken(u.getResetToken(), u.getResetTokenExpiration(), token);
            u.setSenha(passwordEncoder.encode(novaSenha));
            u.setResetToken(null);
            u.setResetTokenExpiration(null);
            usuarioRepository.save(u);
            return;
        }

        Mercado m = mercadoRepository.findByEmail(email);
        if (m != null) {
            validarToken(m.getResetToken(), m.getResetTokenExpiration(), token);
            m.setSenha(passwordEncoder.encode(novaSenha));
            m.setResetToken(null);
            m.setResetTokenExpiration(null);
            mercadoRepository.save(m);
            return;
        }

        throw new RuntimeException("Usuário não encontrado.");
    }

    private void validarToken(String savedToken, LocalDateTime expiration, String providedToken) {
        if (savedToken == null || !savedToken.equals(providedToken)) {
            throw new RuntimeException("Código de recuperação inválido.");
        }
        if (expiration == null || LocalDateTime.now().isAfter(expiration)) {
            throw new RuntimeException("Código de recuperação expirado.");
        }
    }

    private void enviarEmail(String email, String token) {
        if (mailSender == null) {
            System.out.println("Email não configurado. Token: " + token);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("wofertas1@gmail.com");
        message.setTo(email);
        message.setSubject("Código de Recuperação - Wofertas");
        message.setText("Olá!\n\nSeu código de recuperação de senha é:\n\n" +
                token + "\n\n" +
                "Este código expira em 30 minutos.\n\n" +
                "Se você não solicitou isso, ignore este e-mail.");
        mailSender.send(message);
    }

    private boolean matches(String stored, String raw) {
        if (stored == null) return false;
        if (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$")) {
            return passwordEncoder.matches(raw, stored);
        }
        return stored.equals(raw);
    }
}