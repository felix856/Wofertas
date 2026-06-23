package com.example.demo.controller;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Controller que serve as páginas HTML estáticas.
 *
 * CORRIGIDO: as rotas anteriores apontavam para subpastas
 * (pages/auth/, pages/protected/, pages/admin/) que não existem no view.
 * Agora apontam para os arquivos na raiz do static/, onde o view está flat.
 *
 * Estrutura esperada em src/main/resources/static/:
 *   index.html
 *   login.html
 *   mercado-cadastro.html
 *   mercadoHome.html
 *   criar-oferta.html
 *   historico.html
 *   perfil_mercado.html
 *   reset-senha.html
 *   dashboard-pro.html
 *   styles.css
 *   auth-check.js  login.js  logout.js  etc.
 *   imagens/
 */
@Controller
public class PageController {

    // ── Páginas públicas ──────────────────────────────────────────────────────

    @GetMapping("/")
    public String index() {
        return "forward:/index.html";
    }

    @GetMapping("/login")
    public String login() {
        return "forward:/login.html";
    }

    @GetMapping("/mercado-cadastro")
    public String mercadoCadastro() {
        return "forward:/mercado-cadastro.html";
    }

    @GetMapping("/reset-senha")
    public String resetSenha() {
        return "forward:/reset-senha.html";
    }

    // ── Páginas protegidas (mercado) ──────────────────────────────────────────

    @GetMapping("/mercado-home")
    public String mercadoHome() {
        return "forward:/mercadoHome.html";
    }

    @GetMapping("/criar-oferta")
    public String criarOferta() {
        return "forward:/criar-oferta.html";
    }

    @GetMapping("/historico")
    public String historico() {
        return "forward:/historico.html";
    }

    @GetMapping({"/perfil-mercado", "/perfil_mercado"})
    public String perfilMercado() {
        return "forward:/perfil_mercado.html";
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public String dashboard() {
        return "forward:/dashboard-pro.html";
    }
    // Adiciona dentro do PageController, antes do fechamento da classe

@GetMapping("/dashboard-analytics")
public String dashboardAnalytics() {
    return "forward:/dashboard-analytics.html";
}

@GetMapping("/privacy-policy")
public String privacyPolicy() {
    return "forward:/privacy-policy.html";
}

@GetMapping("/termos")
public String termos() {
    return "forward:/termos.html";
}

@GetMapping("/excluir-conta")
public String excluirConta() {
    return "forward:/excluir-conta.html";
}

// Corrige o favicon (500 error)
@GetMapping("/favicon.ico")
@ResponseBody
public org.springframework.http.ResponseEntity<Void> favicon() {
    return org.springframework.http.ResponseEntity.noContent().build();
}
}
