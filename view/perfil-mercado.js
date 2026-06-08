const BASE_URL = localStorage.getItem("wof_base_url") || "http://localhost:8080";
const token    = localStorage.getItem("token");
const mercId   = localStorage.getItem("id");

if (!token || !mercId) { window.location.href = "login.html"; }

let mercadoAtual = null;

async function carregarPerfil() {
    try {
        const res  = await fetch(`${BASE_URL}/mercados/${mercId}`, {
            headers: { "Authorization": `Bearer ${token}` }
        });
        const m = await res.json();
        mercadoAtual = m;

        // Preenche os campos do formulário
        document.getElementById("nome").value      = m.nome     || "";
        document.getElementById("telefone").value  = m.telefone || "";
        document.getElementById("endereco").value  = m.endereco || "";
        document.getElementById("email").value     = m.email    || "";
        document.getElementById("cnpj").value      = m.cnpj     || "";
        // Limpa o campo de senha ao carregar
        if(document.getElementById("senha")) document.getElementById("senha").value = "";

        // Atualiza elementos visuais do topo
        document.getElementById("nomeDisplay").textContent  = m.nome || "Mercado";
        document.getElementById("emailDisplay").textContent = m.email || "—";
        document.getElementById("avatarLetter").textContent = (m.nome || "S")[0].toUpperCase();

        if (m.imagemLogo && m.imagemLogo.trim() !== "") {
            const img = document.getElementById("avatarImg");
            img.src = m.imagemLogo.startsWith("data:") ? m.imagemLogo : `data:image/jpeg;base64,${m.imagemLogo}`;
            img.style.display  = "block";
            document.getElementById("avatarLetter").style.display = "none";
        }
    } catch (e) {
        toast("Erro ao carregar perfil.", "error");
    }
}

function handleLogoUpload(input) {
    const file = input.files[0];
    if (!file) return;
    if (file.size > 5 * 1024 * 1024) { toast("Imagem muito grande (máx 5MB)", "error"); return; }
    
    const reader = new FileReader();
    reader.onload = e => {
        const b64 = e.target.result;
        document.getElementById("logoB64").value = b64;
        const img = document.getElementById("avatarImg");
        img.src = b64;
        img.style.display = "block";
        document.getElementById("avatarLetter").style.display = "none";
        toast("Logo selecionada — clique em Salvar para confirmar", "info");
    };
    reader.readAsDataURL(file);
}

async function salvarPerfil() {
    const nome     = document.getElementById("nome").value.trim();
    const telefone = document.getElementById("telefone").value.trim();
    const endereco = document.getElementById("endereco").value.trim();
    const b64      = document.getElementById("logoB64").value;
    
    // CAPTURA DA SENHA: Se estiver vazia, enviamos null para o Spring não validar o @Size
    const senhaInput = document.getElementById("senha");
    const senhaRaw   = senhaInput ? senhaInput.value.trim() : "";
    const senhaFinal = senhaRaw === "" ? null : senhaRaw;

    if (!nome)     { toast("Nome da loja obrigatório", "error"); return; }
    if (!endereco) { toast("Endereço obrigatório", "error");     return; }
    
    // Validação local de senha apenas se o usuário digitou algo
    if (senhaFinal !== null && senhaFinal.length < 6) {
        toast("A nova senha deve ter no mínimo 6 caracteres", "error");
        return;
    }

    const btn = document.getElementById("btnSalvar");
    btn.disabled = true;
    btn.innerHTML = 'Salvando...';

    try {
        const res = await fetch(`${BASE_URL}/mercados/${mercId}`, {
            method: "PUT",
            headers: { 
                "Content-Type": "application/json", 
                "Authorization": `Bearer ${token}` 
            },
            body: JSON.stringify({
                nome,
                telefone,
                endereco,
                email:      mercadoAtual?.email || "",
                cnpj:       mercadoAtual?.cnpj  || "",
                senha:      senhaFinal, // Envia null ou a senha nova
                imagemLogo: b64 || mercadoAtual?.imagemLogo || "",
                latitude:   mercadoAtual?.latitude  || null,
                longitude:  mercadoAtual?.longitude || null
            })
        });

        if (res.ok) {
            toast("Perfil atualizado com sucesso! ✅");
            if(senhaInput) senhaInput.value = ""; // Limpa campo de senha
            carregarPerfil();
        } else {
            const errData = await res.json().catch(() => ({}));
            toast(errData.message || "Erro ao salvar.", "error");
        }
    } catch (e) {
        toast("Falha ao conectar.", "error");
    } finally {
        btn.disabled = false;
        btn.innerHTML = "Salvar alterações";
    }
}

function toast(msg, type = "success") {
    const container = document.getElementById("toastContainer");
    if(!container) return;
    const el = document.createElement("div");
    el.className = `toast toast-${type}`;
    el.textContent = msg;
    container.appendChild(el);
    setTimeout(() => el.remove(), 3200);
}

carregarPerfil();
