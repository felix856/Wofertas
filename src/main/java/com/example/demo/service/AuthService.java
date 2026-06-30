package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
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
    @Value("${app.mail.enabled:true}") private boolean mailEnabled;
    @Value("${app.mail.from:${spring.mail.username:no-reply@wofertas.com}}") private String mailFrom;

    public CustomUserDetails authenticateByEmailAndPassword(String email, String rawPassword) {
        String normalizedEmail = normalizeEmail(email);
        logger.debug("Tentando autenticar usuario com email: {}", normalizedEmail);

        Usuario u = findUsuarioByEmailFlexible(email);
        if (u != null) {
            if (matches(u.getSenha(), rawPassword)) {
                logger.info("Usuario autenticado com sucesso: {}", normalizedEmail);
                return new CustomUserDetails(
                    Objects.requireNonNullElse(u.getId(), ""),
                    u.getEmail(),
                    u.getSenha(),
                    "USUARIO"
                );
            }
            logger.warn("Senha incorreta para usuario: {}", normalizedEmail);
            throw new RuntimeException("Credenciais inválidas");
        }

        Mercado m = findMercadoByEmailFlexible(email);
        if (m != null) {
            if (matches(m.getSenha(), rawPassword)) {
                logger.info("Mercado autenticado com sucesso: {}", normalizedEmail);
                return new CustomUserDetails(
                    Objects.requireNonNullElse(m.getId(), ""),
                    m.getEmail(),
                    m.getSenha(),
                    "MERCADO"
                );
            }
            logger.warn("Senha incorreta para mercado: {}", normalizedEmail);
            throw new RuntimeException("Credenciais inválidas");
        }

        throw new RuntimeException("Usuário/Mercado não encontrado");
    }

    public void solicitarRecuperacaoSenha(String email) {
        String normalizedEmail = normalizeEmail(email);
        String token = String.format("%06d", (int) (Math.random() * 1000000));
        LocalDateTime expiration = LocalDateTime.now().plusMinutes(30);

        Usuario u = findUsuarioByEmailFlexible(email);
        if (u != null) {
            u.setResetToken(token);
            u.setResetTokenExpiration(expiration);
            usuarioRepository.save(u);
            enviarEmail(resolveRecipientEmail(u.getEmail(), normalizedEmail), token);
            return;
        }

        Mercado m = findMercadoByEmailFlexible(email);
        if (m != null) {
            m.setResetToken(token);
            m.setResetTokenExpiration(expiration);
            mercadoRepository.save(m);
            enviarEmail(resolveRecipientEmail(m.getEmail(), normalizedEmail), token);
            return;
        }

        throw new RuntimeException("E-mail não encontrado.");
    }

    public void redefinirSenha(String email, String token, String novaSenha) {
        Usuario u = findUsuarioByEmailFlexible(email);
        if (u != null) {
            validarToken(u.getResetToken(), u.getResetTokenExpiration(), token);
            u.setSenha(passwordEncoder.encode(novaSenha));
            u.setResetToken(null);
            u.setResetTokenExpiration(null);
            usuarioRepository.save(u);
            return;
        }

        Mercado m = findMercadoByEmailFlexible(email);
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
        if (!mailEnabled || mailSender == null) {
            logger.warn("Servico de e-mail desabilitado. Token de recuperacao gerado para {}: {}", email, token);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(email);
        message.setSubject("Código de Recuperação - Wofertas");
        message.setText("Olá!\n\nSeu código de recuperação de senha é:\n\n" +
                token + "\n\n" +
                "Este código expira em 30 minutos.\n\n" +
                "Se você não solicitou isso, ignore este e-mail.");
        try {
            mailSender.send(message);
        } catch (MailException e) {
            logger.error("Falha ao enviar e-mail de recuperacao para {}", email, e);
            throw new RuntimeException("Servico de e-mail indisponivel. Configure o SMTP e tente novamente.");
        }
    }

    private boolean matches(String stored, String raw) {
        if (stored == null) return false;
        if (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$")) {
            return passwordEncoder.matches(raw, stored);
        }
        return stored.equals(raw);
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new RuntimeException("E-mail e obrigatorio.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private Usuario findUsuarioByEmailFlexible(String email) {
        String trimmed = email == null ? "" : email.trim();
        String normalized = normalizeEmail(email);
        Usuario usuario = usuarioRepository.findByEmail(normalized);
        if (usuario == null && !normalized.equals(trimmed)) {
            usuario = usuarioRepository.findByEmail(trimmed);
        }
        return usuario;
    }

    private Mercado findMercadoByEmailFlexible(String email) {
        String trimmed = email == null ? "" : email.trim();
        String normalized = normalizeEmail(email);
        Mercado mercado = mercadoRepository.findByEmail(normalized);
        if (mercado == null && !normalized.equals(trimmed)) {
            mercado = mercadoRepository.findByEmail(trimmed);
        }
        return mercado;
    }

    private String resolveRecipientEmail(String storedEmail, String fallbackEmail) {
        return storedEmail == null || storedEmail.isBlank() ? fallbackEmail : storedEmail.trim();
    }
}
