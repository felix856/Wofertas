// CORRIGIDO: status "SUSPENSO" → "INATIVO" para bater com o modelo backend (Oferta.java)

const BASE_URL = window.AppConfig?.API_URL || "https://wofertas.koyeb.app";
const token    = localStorage.getItem("token");

if (!token) { window.location.href = "login.html"; }

let todasOfertas  = [];
let editImagemB64 = null;

async function carregarHistorico() {
    const grid  = document.getElementById("listaOfertas");
    const vazia = document.getElementById("mensagemVazia");
    grid.innerHTML = `<p style="color:var(--text-2)"><span class="spinner" style="border-top-color:var(--blue);border-color:rgba(0,0,0,.1)"></span> Carregando...</p>`;
    vazia.style.display = "none";

    try {
        const res = await fetch(`${BASE_URL}/ofertas/historico`, {
            headers: { "Authorization": `Bearer ${token}` }
        });
        if (res.status === 401) { logout(); return; }
        todasOfertas = await res.json();
        filtrar();
    } catch (e) {
        grid.innerHTML = `<p style="color:var(--text-2)">Erro ao carregar. Verifique o servidor.</p>`;
    }
}

function filtrar() {
    const filtro = document.getElementById("filtroStatus").value;
    const lista  = filtro ? todasOfertas.filter(o => o.status === filtro) : todasOfertas;
    renderizar(lista);
}

function renderizar(ofertas) {
    const grid  = document.getElementById("listaOfertas");
    const vazia = document.getElementById("mensagemVazia");

    if (!ofertas.length) {
        grid.innerHTML = "";
        vazia.style.display = "block";
        return;
    }
    vazia.style.display = "none";

    grid.innerHTML = ofertas.map(o => {
        // CORRIGIDO: era "SUSPENSO" — backend usa "INATIVO"
        const ativo      = (o.status || "ATIVO") === "ATIVO";
        const badgeClass = ativo ? "badge-ativo"    : "badge-suspenso";
        const badgeTxt   = ativo ? "✅ Ativo"        : "🔴 Inativo";
        const dataFmt    = o.data ? o.data.split("T")[0].split("-").reverse().join("/") : "—";
        const imgTag     = o.imagemOferta
            ? `<img src="${o.imagemOferta}" alt="${o.nome}" loading="lazy" onclick="abrirModal('${encodeURIComponent(o.imagemOferta)}')">`
            : `<div class="oferta-thumb-placeholder">🏷️</div>`;
        return `
        <div class="oferta-card">
            <div class="oferta-thumb">${imgTag}</div>
            <div class="oferta-body">
                <div class="oferta-nome" title="${o.nome}">${o.nome}</div>
                <div class="oferta-meta">📅 ${dataFmt}</div>
                <span class="badge ${badgeClass}">${badgeTxt}</span>
                <div class="oferta-actions">
                    <button class="btn-action btn-edit"   onclick="abrirEditar('${o.id}')">✏️ Editar</button>
                    <button class="btn-action btn-delete" onclick="confirmarExclusao('${o.id}','${o.nome.replace(/'/g,"\\'")}')">🗑 Excluir</button>
                </div>
            </div>
        </div>`;
    }).join("");
}

// ── Editar ──────────────────────────────────────────────────────────
function abrirEditar(id) {
    const o = todasOfertas.find(x => x.id === id);
    if (!o) return;

    document.getElementById("editId").value     = o.id;
    document.getElementById("editNome").value   = o.nome;
    document.getElementById("editStatus").value = o.status || "ATIVO";
    document.getElementById("editData").value   = (o.data || "").split("T")[0];
    
    // Reseta a variável da nova imagem
    editImagemB64 = null;

    const preview = document.getElementById("editPreview");
    const hint    = document.getElementById("editUploadHint");
    const area    = document.getElementById("editUploadArea");

    // SE JÁ EXISTIR IMAGEM: Mostra ela no preview do modal
    if (o.imagemOferta) {
        preview.src = o.imagemOferta;
        preview.style.display = "block";
        hint.style.display = "none";
        area.classList.add("has-preview");
    } else {
        preview.style.display = "none";
        hint.style.display = "block";
        area.classList.remove("has-preview");
    }

    document.getElementById("editImagemInput").value = "";
    document.getElementById("modalEditar").classList.add("open");
}


function fecharModal() {
    document.getElementById("modalEditar").classList.remove("open");
}

document.getElementById("modalEditar").addEventListener("click", e => {
    if (e.target === document.getElementById("modalEditar")) fecharModal();
});

function handleEditUpload(input) {
    const file = input.files[0];
    if (!file) return;
    if (file.size > 5 * 1024 * 1024) { toast("Imagem muito grande (máx 5MB)", "error"); return; }
    const reader = new FileReader();
    reader.onload = e => {
        editImagemB64 = e.target.result;
        document.getElementById("editPreview").src = editImagemB64;
        document.getElementById("editPreview").style.display    = "block";
        document.getElementById("editUploadHint").style.display = "none";
        document.getElementById("editUploadArea").classList.add("has-preview");
    };
    reader.readAsDataURL(file);
}

async function salvarEdicao() {
    const id     = document.getElementById("editId").value;
    const nome   = document.getElementById("editNome").value.trim();
    const status = document.getElementById("editStatus").value;
    const data   = document.getElementById("editData").value;
    if (!nome) { toast("Informe o nome", "error"); return; }

    const ofertaAtual = todasOfertas.find(x => x.id === id);
    const imagemFinal = editImagemB64 || ofertaAtual?.imagemOferta || null;

    const btn = document.getElementById("btnSalvarEdit");
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner"></span>';

    try {
        const res = await fetch(`${BASE_URL}/ofertas/${id}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json", "Authorization": `Bearer ${token}` },
            body: JSON.stringify({ nome, status, data, imagemOferta: imagemFinal })
        });
        if (res.ok) {
            toast("Oferta atualizada!");
            fecharModal();
            carregarHistorico();
        } else {
            toast("Erro ao salvar.", "error");
        }
    } catch (e) {
        toast("Falha ao conectar.", "error");
    } finally {
        btn.disabled = false;
        btn.innerHTML = "Salvar";
    }
}

// ── Excluir ─────────────────────────────────────────────────────────
function confirmarExclusao(id, nome) {
    if (!confirm(`Excluir "${nome}"?\n\nEssa ação não pode ser desfeita.`)) return;
    excluir(id);
}

async function excluir(id) {
    try {
        const res = await fetch(`${BASE_URL}/ofertas/${id}`, {
            method: "DELETE",
            headers: { "Authorization": `Bearer ${token}` }
        });
        if (res.ok || res.status === 204) {
            toast("Oferta excluída.");
            carregarHistorico();
        } else {
            toast("Erro ao excluir.", "error");
        }
    } catch (e) {
        toast("Falha ao conectar.", "error");
    }
}

// ── Modal imagem ─────────────────────────────────────────────────────
function abrirModal(src) {
    document.getElementById("modalFoto").src = decodeURIComponent(src);
    document.getElementById("modalImagem").style.display = "flex";
}

// ── Toast ─────────────────────────────────────────────────────────────
function toast(msg, type = "success") {
    const el = document.createElement("div");
    el.className = `toast toast-${type}`;
    el.textContent = msg;
    document.getElementById("toastContainer").appendChild(el);
    setTimeout(() => el.remove(), 3200);
}

carregarHistorico();
