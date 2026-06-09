package com.example.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HomeController {

    @GetMapping("/favicon.ico")
    @ResponseBody
    public ResponseEntity<Void> favicon() {
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/")
    public String home() {
        return "forward:/index.html";
    }

    @GetMapping("/login.html")
    public String login() {
        return "forward:/login.html";
    }

    @GetMapping("/mercadoHome.html")
    public String mercadoHome() {
        return "forward:/mercadoHome.html";
    }

    @GetMapping("/criar-oferta.html")
    public String criarOferta() {
        return "forward:/criar-oferta.html";
    }

    @GetMapping("/dashboard-analytics.html")
    public String dashboardAnalytics() {
        return "forward:/dashboard-analytics.html";
    }

    @GetMapping("/dashboard-pro.html")
    public String dashboardPro() {
        return "forward:/dashboard-pro.html";
    }

    @GetMapping("/historico.html")
    public String historico() {
        return "forward:/historico.html";
    }

    @GetMapping("/mercado-cadastro.html")
    public String mercadoCadastro() {
        return "forward:/mercado-cadastro.html";
    }

    @GetMapping("/perfil_mercado.html")
    public String perfilMercado() {
        return "forward:/perfil_mercado.html";
    }

    @GetMapping("/reset-senha.html")
    public String resetSenha() {
        return "forward:/reset-senha.html";
    }
}
