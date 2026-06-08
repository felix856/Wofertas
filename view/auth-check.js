/**
 * auth-check.js
 * Protege as páginas web do Wofertas.
 * Inclua em todas as páginas que exigem login.
 *
 * CORRIGIDO:
 *   - clientePages removidas páginas inexistentes (ofertas.html, favoritos.html)
 *   - Redireciona USUARIO para index.html em vez de página inexistente
 */
(function () {
    const token       = localStorage.getItem("token");
    const tipo        = localStorage.getItem("tipo");
    const currentPage = window.location.pathname.split("/").pop();

    // 1. Verifica se está logado
    if (!token) {
        const publicPages = ["index.html", "login.html", "mercado-cadastro.html", "reset-senha.html", ""];
        if (!publicPages.includes(currentPage)) {
            alert("Acesso restrito. Por favor, faça login.");
            window.location.href = "login.html";
        }
        return;
    }

    // 2. Proteção por tipo de conta
    // Páginas exclusivas de MERCADO
    const mercadoPages = [
        "mercadoHome.html",
        "criar-oferta.html",
        "perfil_mercado.html",
        "historico.html",
        "dashboard-pro.html"
    ];

    // CORRIGIDO: USUARIO não possui páginas próprias neste view (painel é para mercados).
    // Se criar páginas de consumidor no futuro, adicione aqui.
    const clientePages = [];   // ex.: ["ofertas.html", "favoritos.html"]

    if (tipo === "USUARIO" && mercadoPages.includes(currentPage)) {
        alert("Página exclusiva para mercados cadastrados.");
        localStorage.clear();
        window.location.href = "index.html";
    }

    if (tipo === "MERCADO" && clientePages.length && clientePages.includes(currentPage)) {
        alert("Página exclusiva para clientes.");
        window.location.href = "mercadoHome.html";
    }
})();

/**
 * Logout global — disponível em qualquer página que inclua este script.
 */
function logout() {
    localStorage.clear();
    window.location.href = "login.html";
}
