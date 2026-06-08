package com.example.demo.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de debug local para inspeção de autenticação.
 * NÃO expor em produção.
 */
@RestController
@RequestMapping("/api/debug")
@CrossOrigin("*")
public class DebugController {

    @GetMapping("/whoami")
    public ResponseEntity<Object> whoami(Authentication auth) {
        if (auth == null) {
            return ResponseEntity.ok(Map.of(
                "authenticated", false,
                "principal", null,
                "authorities", List.of()
            ));
        }

        Object principal = auth.getPrincipal();
        var authorities = auth.getAuthorities();

        return ResponseEntity.ok(Map.of(
            "authenticated", auth.isAuthenticated(),
            "principal", principal,
            "authorities", authorities
        ));
    }
}
