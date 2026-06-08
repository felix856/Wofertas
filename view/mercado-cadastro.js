const logoInput = document.getElementById("logoInput");
const preview   = document.getElementById("previewLogo");

let logoBase64 = null;   // ← era caminhoImagem (path local), agora é base64

// Valida se o input de arquivo existe na página antes de escutar o evento
if (logoInput) {
    logoInput.addEventListener("change", () => {
        const file = logoInput.files[0];
        if (!file) return;

        if (file.size > 5 * 1024 * 1024) {
            alert("Imagem muito grande! Máximo permitido: 5 MB.");
            logoInput.value = "";
            return;
        }

        const reader = new FileReader();
        reader.onload = (e) => {
            logoBase64 = e.target.result;          // data:image/png;base64,...
            
            // Valida se a tag de imagem de preview existe antes de alterar o atributo src
            if (preview) {
                preview.src = logoBase64;
                preview.style.display = "block";
            }
        };
        reader.readAsDataURL(file);
    });
}

const btnCadastrar = document.getElementById("btnCadastrar");
if (btnCadastrar) {
    btnCadastrar.addEventListener("click", async () => {

        const nome     = document.getElementById("nome")?.value.trim() || "";
        const cnpj     = (document.getElementById("cnpj")?.value.trim() || "").replace(/\D/g, ''); // Garante só números
        const endereco = document.getElementById("endereco")?.value.trim() || "";
        const email    = document.getElementById("email")?.value.trim() || "";
        const senha    = document.getElementById("senha")?.value.trim() || "";
        const telefone = (document.getElementById("telefone")?.value.trim() || "").replace(/\D/g, ''); // Garante só números

        // Validação de campos obrigatórios incluindo o telefone
        if (!nome || !cnpj || !endereco || !email || !senha || !telefone) {
            alert("Preencha todos os campos obrigatórios!");
            return;
        }

        // Logo é opcional: se não selecionada, envia string vazia
        const mercado = {
            nome,
            cnpj,
            endereco,
            email,
            senha,
            telefone,
            imagemLogo: logoBase64 || ""   // ← base64 ou vazio
        };

        try {
            // Mantido o endpoint "/mercados" que você configurou no seu backend
            const response = await fetch("http://localhost:8080/mercados", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(mercado)
            });

            if (!response.ok) {
                const err = await response.json().catch(() => ({}));
                alert("Erro ao cadastrar: " + (err.message || response.status));
                return;
            }

            alert("Mercado cadastrado com sucesso!");
            window.location.href = "login.html";

        } catch (error) {
            console.error(error);
            alert("Falha ao conectar com o servidor.");
        }
    });
}
