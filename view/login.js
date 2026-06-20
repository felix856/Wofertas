/**
 * Login Handler
 * Wofertas SPA + Spring + Mongo + JWT
 * Compatível Web + Mobile
 */

document.addEventListener("DOMContentLoaded", () => {

    console.log("Inicializando Login Handler...");

    if (window.authAPI) {
        initLoginHandler();
    } else {

        window.addEventListener(
            "modulesLoaded",
            initLoginHandler,
            { once: true }
        );

    }

});

function initLoginHandler() {

    console.log("✓ Login Handler carregado");

    const loginBtn =
        document.getElementById("login-btn");

    const emailInput =
        document.getElementById("email");

    const senhaInput =
        document.getElementById("senha");

    if (
        !loginBtn ||
        !emailInput ||
        !senhaInput
    ) {

        console.warn(
            "Campos login não encontrados"
        );

        return;
    }

    /*
     Evita registrar eventos duplicados
    */
    loginBtn.replaceWith(
        loginBtn.cloneNode(true)
    );

    const novoBotao =
        document.getElementById("login-btn");

    /*
      ENTER
    */
    [emailInput, senhaInput]
        .forEach(input => {

            input.addEventListener(
                "keypress",
                e => {

                    if (
                        e.key === "Enter"
                    ) {

                        handleLogin();

                    }

                }
            );

        });

    /*
       CLICK
    */

    novoBotao.addEventListener(
        "click",
        handleLogin
    );

    /*
      LOGIN PRINCIPAL
    */

    async function handleLogin() {

        const email =
            emailInput.value.trim();

        const senha =
            senhaInput.value.trim();

        /*
         validações
        */

        if (!email || !senha) {

            notificationsService.warning(
                "Preencha email e senha"
            );

            return;
        }

        if (
            window.validators &&
            !validators.email(email)
        ) {

            notificationsService.warning(
                "Email inválido"
            );

            return;
        }

        loadingButton();

        try {

            console.log(
                "Tentando login..."
            );

            const result =
                await authAPI.login(
                    email,
                    senha
                );

            console.log(
                "Resposta login:",
                result
            );

            if (
                !result ||
                result.error
            ) {

                notificationsService.error(
                    result?.error ||
                    "Falha no login"
                );

                resetButton();

                return;
            }

            /*
              Estrutura backend:
              
              {
                 token:"",
                 usuario:{}
              }
            */

            const token =
                result.data?.token ||
                result.token;

            const usuario =
                result.data?.usuario ||
                result.usuario ||
                result.data;

            if (!token) {

                notificationsService.error(
                    "Token não recebido"
                );

                resetButton();

                return;
            }

            /*
             Salva JWT
            */

            try {

                localStorage.setItem(
                    "token",
                    token
                );

                apiClient.setToken(
                    token
                );

            }
            catch (e) {

                console.error(
                    "Erro salvando token",
                    e
                );

            }

            /*
             salva usuário
            */

            try {

                localStorage.setItem(
                    "authUser",
                    JSON.stringify(
                        usuario
                    )
                );

                localStorage.setItem(
                    "tipo",
                    usuario.tipo || ""
                );

                localStorage.setItem(
                    "mercado",
                    JSON.stringify(
                        usuario
                    )
                );

                if (
                    window.storageService
                ) {

                    storageService.set(
                        "authUser",
                        usuario
                    );

                    storageService.set(
                        "mercado",
                        usuario
                    );

                }

            }
            catch (e) {

                console.error(
                    "Erro localStorage:",
                    e
                );

            }

            notificationsService.success(
                "Login realizado com sucesso"
            );

            /*
             redirecionamento
            */

            setTimeout(() => {

                const origin =
                    window.location.origin;

                switch (
                    usuario.tipo
                ) {

                    case "MERCADO":

                        window.location.href =
                            `${origin}/mercadoHome.html`;

                        break;

                    case "ADMIN":

                        window.location.href =
                            `${origin}/dashboard-admin.html`;

                        break;

                    case "USUARIO":

                        window.location.href =
                            `${origin}/`;

                        break;

                    default:

                        console.warn(
                            "Tipo desconhecido:",
                            usuario.tipo
                        );

                        window.location.href =
                            `${origin}/`;

                }

            }, 1200);

        }
        catch (err) {

            console.error(
                "Erro login:",
                err
            );

            if (
                window.errorHandler
            ) {

                errorHandler.error(
                    "Erro login",
                    err
                );

            }

            notificationsService.error(
                "Erro ao conectar ao servidor"
            );

            resetButton();

        }

    }

    /*
      UI BOTÃO
    */

    function loadingButton() {

        novoBotao.disabled =
            true;

        novoBotao.innerHTML =
            `
            <span class="spinner-border spinner-border-sm"></span>
            Entrando...
            `;

    }

    function resetButton() {

        novoBotao.disabled =
            false;

        novoBotao.innerHTML =
            "Entrar";

    }

}
