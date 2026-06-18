function logout() {
    // Remove token e outros dados
    localStorage.removeItem("token");
    localStorage.removeItem("usuario");
    localStorage.clear(); // opcional, limpa tudo

    // Redireciona para login
    window.location.href = "login.html";
}
