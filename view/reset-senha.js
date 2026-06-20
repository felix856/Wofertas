document.addEventListener("DOMContentLoaded", () => {
    const BASE_URL = window.AppConfig?.API_URL || localStorage.getItem("wof_base_url") || "https://wofertas-production.up.railway.app";

    const btnSolicitar = document.querySelector(".btnSolicitar");
    const btnRedefinir = document.querySelector(".btnRedefinir");

    const emailSolicitar = document.querySelector("#emailSolicitar");
    const emailRedefinir = document.querySelector("#emailRedefinir");
    const codigoRecuperacao = document.querySelector("#codigoRecuperacao");
    const novaSenha = document.querySelector("#novaSenha");
    const confirmarSenha = document.querySelector("#confirmarSenha");

    const stage1 = document.querySelector(".stage-1");
    const stage2 = document.querySelector(".stage-2");
    const successMessage = document.querySelector("#successMessage");

    // STAGE 1: Solicitar Código de Recuperação
    btnSolicitar.addEventListener("click", async () => {
        const email = emailSolicitar.value.trim();

        if (!email) {
            alert("Preencha o email!");
            return;
        }

        if (!isValidEmail(email)) {
            alert("Email inválido!");
            return;
        }

        try {
            btnSolicitar.disabled = true;
            btnSolicitar.textContent = "Enviando...";

            const response = await fetch(`${BASE_URL}/auth/forgot-password`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded"
                },
                body: new URLSearchParams({
                    email: email
                })
            });

            if (!response.ok) {
                alert("Email não cadastrado no sistema!");
                btnSolicitar.disabled = false;
                btnSolicitar.textContent = "Solicitar Código";
                return;
            }

            // Sucesso: transição para stage 2
            emailRedefinir.value = email;
            stage1.classList.remove("active");
            stage2.classList.add("active");
            successMessage.classList.add("show");

            // Remove a mensagem de sucesso após 5 segundos
            setTimeout(() => {
                successMessage.classList.remove("show");
            }, 5000);

        } catch (error) {
            console.error("Erro ao solicitar código:", error);
            alert("Erro ao conectar ao servidor.");
            btnSolicitar.disabled = false;
            btnSolicitar.textContent = "Solicitar Código";
        }
    });

    // STAGE 2: Redefinir Senha
    btnRedefinir.addEventListener("click", async () => {
        const email = emailRedefinir.value;
        const codigo = codigoRecuperacao.value.trim();
        const senha = novaSenha.value;
        const confirmacao = confirmarSenha.value;

        if (!codigo || !senha || !confirmacao) {
            alert("Preencha todos os campos!");
            return;
        }

        if (senha !== confirmacao) {
            alert("As senhas não coincidem!");
            return;
        }

        if (senha.length < 6) {
            alert("A senha deve ter no mínimo 6 caracteres!");
            return;
        }

        try {
            btnRedefinir.disabled = true;
            btnRedefinir.textContent = "Redefinindo...";

            const response = await fetch(`${BASE_URL}/auth/reset-password`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    email: email,
                    token: codigo,
                    novaSenha: senha
                })
            });

            if (!response.ok) {
                alert("Código inválido ou expirado!");
                btnRedefinir.disabled = false;
                btnRedefinir.textContent = "Redefinir Senha";
                return;
            }

            // Sucesso!
            alert("Senha redefinida com sucesso! Você será redirecionado para o login.");
            window.location.href = "login.html";

        } catch (error) {
            console.error("Erro ao redefinir senha:", error);
            alert("Erro ao conectar ao servidor.");
            btnRedefinir.disabled = false;
            btnRedefinir.textContent = "Redefinir Senha";
        }
    });

    // Função auxiliar para validar email
    function isValidEmail(email) {
        const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return regex.test(email);
    }

    // Permitir enter para enviar
    emailSolicitar.addEventListener("keypress", (e) => {
        if (e.key === "Enter") btnSolicitar.click();
    });

    confirmarSenha.addEventListener("keypress", (e) => {
        if (e.key === "Enter") btnRedefinir.click();
    });

});
