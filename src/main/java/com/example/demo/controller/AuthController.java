package com.example.demo.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.AuthRequest;
import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.ForgotPasswordRequest;
import com.example.demo.dto.ResetPasswordRequest;
import com.example.demo.model.Mercado;
import com.example.demo.model.Usuario;
import com.example.demo.security.CustomUserDetails;
import com.example.demo.security.JwtUtil;
import com.example.demo.service.AuthService;
import com.example.demo.service.MercadoService;
import com.example.demo.service.UsuarioService;

@RestController
@RequestMapping({"/auth", "/api/auth"})
public class AuthController {

    @Autowired private AuthService authService;
    @Autowired private JwtUtil     jwtUtil;
    @Autowired private UsuarioService usuarioService;
    @Autowired private MercadoService mercadoService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest req) {
        CustomUserDetails user  = authService.authenticateByEmailAndPassword(req.getEmail(), req.getSenha());
        String            token = jwtUtil.generateToken(user.getId(), user.getTipo(), user.getUsername());
        AuthResponse      resp  = new AuthResponse(token, user.getId(), user.getTipo(), user.getUsername());
        return ResponseEntity.ok(resp);
    }
    

    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> signup(@RequestBody Map<String, String> req) {
        String tipo = req.getOrDefault("tipo", "USUARIO").toUpperCase();
        Map<String, Object> resp = new LinkedHashMap<>();

        if ("MERCADO".equals(tipo)) {
            Mercado mercado = new Mercado(
                    req.get("nome"),
                    req.get("cnpj"),
                    req.getOrDefault("endereco", ""),
                    req.get("telefone"),
                    req.get("email"),
                    req.get("senha"),
                    req.get("imagemLogo")
            );
            Mercado criado = mercadoService.criar(mercado);
            resp.put("id", criado.getId());
            resp.put("email", criado.getEmail());
            resp.put("nome", criado.getNome());
            resp.put("tipo", "MERCADO");
        } else {
            Usuario usuario = new Usuario(req.get("nome"), req.get("email"), req.get("senha"));
            Usuario criado = usuarioService.criar(usuario);
            resp.put("id", criado.getId());
            resp.put("email", criado.getEmail());
            resp.put("nome", criado.getNome());
            resp.put("tipo", "USUARIO");
            resp.put("imagemPerfil", criado.getImagemPerfil());
        }

        resp.put("ativo", true);
        return ResponseEntity.status(201).body(resp);
    }

    @GetMapping("/validar-token")
    public ResponseEntity<String> validarToken(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(user != null ? "VALIDO" : "INVALIDO");
    }

    @PostMapping(value = "/forgot-password", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> forgotPasswordJson(@Valid @RequestBody ForgotPasswordRequest req) {
        authService.solicitarRecuperacaoSenha(req.getEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/forgot-password", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> forgotPasswordForm(@RequestParam String email) {
        authService.solicitarRecuperacaoSenha(email);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/forgot-password", params = "email")
    public ResponseEntity<Void> forgotPasswordQuery(@RequestParam String email) {
        authService.solicitarRecuperacaoSenha(email);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.redefinirSenha(req.getEmail(), req.getToken(), req.getNovaSenha());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-senha")
    public ResponseEntity<String> resetSenha(@Valid @RequestBody ResetPasswordRequest req) {
        authService.redefinirSenha(req.getEmail(), req.getToken(), req.getNovaSenha());
        return ResponseEntity.ok("Senha redefinida com sucesso");
    }
    @PostMapping("/refresh-token")
public ResponseEntity<AuthResponse> refreshToken(
        @AuthenticationPrincipal CustomUserDetails user) {
    String token = jwtUtil.generateToken(
        user.getId(), user.getTipo(), user.getUsername()
    );
    return ResponseEntity.ok(
        new AuthResponse(token, user.getId(), user.getTipo(), user.getUsername())
    );
}
}
