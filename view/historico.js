const APP_CONFIG = window.AppConfig || {};
const BASE_URL = APP_CONFIG.API_URL || localStorage.getItem("wof_base_url") || "https://wofertas-production.up.railway.app";
const token = localStorage.getItem("token");

if (!token) {
    window.location.href = "login.html";
}

let todasOfertas = [];
let editImagemB64 = null;

async function carregarHistorico() {
    const grid = document.getElementById("listaOfertas");
    const vazia = document.getElementById("mensagemVazia");

    grid.innerHTML = `<p style="color:var(--text-2)"><span class="spinner" style="border-top-color:var(--blue);border-color:rgba(0,0,0,.1)"></span> Carregando...</p>`;
    vazia.style.display = "none";

    const res = await apiFetch("/ofertas/historico");

    if (res.status === 401) {
        logout();
        return;
    }

    if (!res.ok) {
        grid.innerHTML = `<p style="color:var(--text-2)">Erro ao carregar: ${escaparHtml(res.error || "verifique o servidor.")}</p>`;
        return;
    }

    todasOfertas = Array.isArray(res.data) ? res.data : [];
    filtrar();
}

function filtrar() {
    const filtro = document.getElementById("filtroStatus").value;
    const lista = filtro ? todasOfertas.filter(o => (o.status || "ATIVO") === filtro) : todasOfertas;
    renderizar(lista);
}

function renderizar(ofertas) {
    const grid = document.getElementById("listaOfertas");
    const vazia = document.getElementById("mensagemVazia");

    if (!ofertas.length) {
        grid.innerHTML = "";
        vazia.style.display = "block";
        return;
    }

    vazia.style.display = "none";

    grid.innerHTML = ofertas.map(o => {
        const ativo = (o.status || "ATIVO") === "ATIVO";
        const badgeClass = ativo ? "badge-ativo" : "badge-suspenso";
        const badgeTxt = ativo ? "Ativo" : "Inativo";
        const dataFmt = formatarData(o.data);
        const imagemOferta = normalizarUrlArquivo(o.imagemOferta);
        const nome = escaparHtml(o.nome || "Oferta");
        const id = escaparAtributo(o.id || "");

        const imgTag = imagemOferta
            ? `<img src="${imagemOferta}" alt="${nome}" loading="lazy" onclick="abrirModal('${encodeURIComponent(imagemOferta)}')">`
            : `<div class="oferta-thumb-placeholder">Oferta</div>`;

        return `
        <div class="oferta-card">
            <div class="oferta-thumb">${imgTag}</div>
            <div class="oferta-body">
                <div class="oferta-nome" title="${nome}">${nome}</div>
                <div class="oferta-meta">${dataFmt}</div>
                <span class="badge ${badgeClass}">${badgeTxt}</span>
                <div class="oferta-actions">
                    <button class="btn-action btn-edit" onclick="abrirEditar('${id}')">Editar</button>
                    <button class="btn-action btn-delete" onclick="confirmarExclusao('${id}','${escaparAtributo(o.nome || "Oferta")}')">Excluir</button>
                </div>
            </div>
        </div>`;
    }).join("");
}

function abrirEditar(id) {
    const o = todasOfertas.find(x => x.id === id);
    if (!o) return;

    document.getElementById("editId").value = o.id;
    document.getElementById("editNome").value = o.nome || "";
    document.getElementById("editStatus").value = o.status || "ATIVO";
    document.getElementById("editData").value = (o.data || "").split("T")[0];

    editImagemB64 = null;

    const preview = document.getElementById("editPreview");
    const hint = document.getElementById("editUploadHint");
    const area = document.getElementById("editUploadArea");
    const imagemOferta = normalizarUrlArquivo(o.imagemOferta);

    if (imagemOferta) {
        preview.src = imagemOferta;
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

    if (file.size > 5 * 1024 * 1024) {
        toast("Imagem muito grande (max 5MB)", "error");
        return;
    }

    const reader = new FileReader();
    reader.onload = e => {
        editImagemB64 = e.target.result;
        document.getElementById("editPreview").src = editImagemB64;
        document.getElementById("editPreview").style.display = "block";
        document.getElementById("editUploadHint").style.display = "none";
        document.getElementById("editUploadArea").classList.add("has-preview");
    };
    reader.readAsDataURL(file);
}

async function salvarEdicao() {
    const id = document.getElementById("editId").value;
    const nome = document.getElementById("editNome").value.trim();
    const status = document.getElementById("editStatus").value;
    const data = document.getElementById("editData").value;

    if (!nome) {
        toast("Informe o nome", "error");
        return;
    }

    const ofertaAtual = todasOfertas.find(x => x.id === id);
    const imagemFinal = editImagemB64 || ofertaAtual?.imagemOferta || null;
    const btn = document.getElementById("btnSalvarEdit");

    btn.disabled = true;
    btn.innerHTML = '<span class="spinner"></span>';

    try {
        const res = await apiFetch(`/ofertas/${id}`, {
            method: "PUT",
            body: JSON.stringify({ nome, status, data, imagemOferta: imagemFinal })
        });

        if (res.ok) {
            toast("Oferta atualizada!");
            fecharModal();
            carregarHistorico();
        } else {
            toast("Erro ao salvar: " + (res.error || res.status), "error");
        }
    } finally {
        btn.disabled = false;
        btn.innerHTML = "Salvar";
    }
}

function confirmarExclusao(id, nome) {
    if (!confirm(`Excluir "${nome}"?\n\nEssa acao nao pode ser desfeita.`)) return;
    excluir(id);
}

async function excluir(id) {
    const res = await apiFetch(`/ofertas/${id}`, { method: "DELETE" });

    if (res.ok || res.status === 204) {
        toast("Oferta excluida.");
        carregarHistorico();
    } else {
        toast("Erro ao excluir: " + (res.error || res.status), "error");
    }
}

async function apiFetch(endpoint, options = {}) {
    if (typeof APP_CONFIG.safeFetch === "function") {
        return APP_CONFIG.safeFetch(endpoint, options);
    }

    try {
        const response = await fetch(`${BASE_URL}${endpoint}`, {
            ...options,
            headers: {
                "Content-Type": "application/json",
                ...(token ? { "Authorization": `Bearer ${token}` } : {}),
                ...(options.headers || {})
            }
        });
        const text = await response.text();
        let data = null;
        try {
            data = text ? JSON.parse(text) : null;
        } catch {
            data = text;
        }
        return {
            ok: response.ok,
            status: response.status,
            data,
            error: response.ok ? null : (data?.message || data?.error || data || `Erro ${response.status}`)
        };
    } catch {
        return {
            ok: false,
            status: 0,
            data: null,
            error: "Falha de conexao com o servidor."
        };
    }
}

function abrirModal(src) {
    document.getElementById("modalFoto").src = decodeURIComponent(src);
    document.getElementById("modalImagem").style.display = "flex";
}

function toast(msg, type = "success") {
    if (typeof APP_CONFIG.toast === "function") {
        APP_CONFIG.toast(msg, type);
        return;
    }

    const container = document.getElementById("toastContainer");
    if (!container) {
        console[type === "error" ? "error" : "log"](msg);
        return;
    }

    const el = document.createElement("div");
    el.className = `toast toast-${type}`;
    el.textContent = msg;
    container.appendChild(el);
    setTimeout(() => el.remove(), 3200);
}

function normalizarUrlArquivo(url) {
    if (!url) return "";
    if (/^(https?:|data:|blob:)/i.test(url)) return url;
    return `${BASE_URL}${url.startsWith("/") ? url : `/${url}`}`;
}

function formatarData(data) {
    if (!data) return "-";
    const [yyyy, mm, dd] = data.split("T")[0].split("-");
    return dd && mm && yyyy ? `${dd}/${mm}/${yyyy}` : data;
}

function escaparHtml(value) {
    return String(value).replace(/[&<>"']/g, char => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        "\"": "&quot;",
        "'": "&#039;"
    }[char]));
}

function escaparAtributo(value) {
    return escaparHtml(value).replace(/`/g, "&#096;");
}

window.carregarHistorico = carregarHistorico;
window.filtrar = filtrar;
window.abrirEditar = abrirEditar;
window.fecharModal = fecharModal;
window.handleEditUpload = handleEditUpload;
window.salvarEdicao = salvarEdicao;
window.confirmarExclusao = confirmarExclusao;
window.abrirModal = abrirModal;

carregarHistorico();
