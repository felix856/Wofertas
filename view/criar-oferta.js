const BASE_URL = localStorage.getItem("wof_base_url") || "http://localhost:8080";
const token    = localStorage.getItem("token");

if (!token) { window.location.href = "login.html"; }

// Data padrão: hoje
document.getElementById("data").value = new Date().toISOString().split("T")[0];

let imagemBase64 = null;

function handleUpload(input) {
    const file = input.files[0];
    if (!file) return;
    if (file.size > 5 * 1024 * 1024) { toast("Imagem muito grande (máx 5MB)", "error"); return; }
    const reader = new FileReader();
    reader.onload = e => {
        imagemBase64 = e.target.result;
        document.getElementById("previewImagem").src    = imagemBase64;
        document.getElementById("previewImagem").style.display = "block";
        document.getElementById("uploadHint").style.display    = "none";
        document.getElementById("uploadArea").classList.add("has-preview");
    };
    reader.readAsDataURL(file);
}

function limpar() {
    document.getElementById("nome").value  = "";
    document.getElementById("status").value = "ATIVO";
    document.getElementById("data").value  = new Date().toISOString().split("T")[0];
    imagemBase64 = null;
    document.getElementById("previewImagem").style.display = "none";
    document.getElementById("uploadHint").style.display    = "block";
    document.getElementById("uploadArea").classList.remove("has-preview");
    document.getElementById("imagemInput").value = "";
}

async function criarOferta() {
    const nome   = document.getElementById("nome").value.trim();
    const status = document.getElementById("status").value;
    const data   = document.getElementById("data").value;

    if (!nome) { toast("Informe o nome da oferta", "error"); return; }
    if (!data) { toast("Informe a data da oferta", "error"); return; }

    const btn = document.getElementById("btnCriar");
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner"></span> Publicando...';

    try {
        const res = await fetch(`${BASE_URL}/ofertas`, {
            method: "POST",
            headers: { "Content-Type": "application/json", "Authorization": `Bearer ${token}` },
            body: JSON.stringify({ nome, status, data, imagemOferta: imagemBase64 })
        });

        if (res.ok) {
            toast("Oferta publicada com sucesso! 🎉");
            setTimeout(() => window.location.href = "historico.html", 1400);
        } else {
            const err = await res.json().catch(() => ({}));
            toast("Erro: " + (err.message || res.status), "error");
        }
    } catch (e) {
        toast("Falha ao conectar ao servidor.", "error");
    } finally {
        btn.disabled = false;
        btn.innerHTML = "Publicar oferta";
    }
}

// ── Toast ──────────────────────────────────────────────────────────
function toast(msg, type = "success") {
    const el = document.createElement("div");
    el.className = `toast toast-${type}`;
    el.textContent = msg;
    document.getElementById("toastContainer").appendChild(el);
    setTimeout(() => el.remove(), 3200);
}