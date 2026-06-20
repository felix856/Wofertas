const APP_CONFIG = window.AppConfig || {};
const BASE_URL = APP_CONFIG.API_URL || localStorage.getItem("wof_base_url") || "https://wofertas-production.up.railway.app";
const token = localStorage.getItem("token");

if (!token) {
    window.location.href = "login.html";
}

const h = new Date().getHours();
document.getElementById("saudacao").textContent =
    (h < 12 ? "Bom dia" : h < 18 ? "Boa tarde" : "Boa noite") + "!";

async function carregarDashboard() {
    const res = await apiFetch("/ofertas/historico");
    const grid = document.getElementById("ultimasOfertas");

    if (!res.ok) {
        showToast("Nao foi possivel carregar o dashboard: " + res.error, "error");
        grid.innerHTML = `<div class="msg-vazia" style="color:var(--danger)">Erro de conexao com o servidor.</div>`;
        return;
    }

    const ofertas = Array.isArray(res.data) ? res.data : [];

    document.getElementById("statTotal").textContent = ofertas.length;
document.getElementById("statAtivas").textContent =
        ofertas.filter(o => (o.status || "ATIVO") === "ATIVO").length;
    document.getElementById("statSuspensas").textContent =
        ofertas.filter(o => (o.status || "ATIVO") !== "ATIVO").length;

    const lista = ofertas.slice(0, 8);

    if (lista.length === 0) {
        grid.innerHTML = `<div class="msg-vazia">Nenhuma oferta publicada ainda.</div>`;
        return;
    }

    grid.innerHTML = lista.map(o => {
        const status = (o.status || "ATIVO") === "ATIVO" ? "Ativo" : "Inativo";
        const precoFmt = o.preco ? `R$ ${parseFloat(o.preco).toFixed(2).replace(".", ",")}` : "Consulte";
        const imagemOferta = normalizarUrlArquivo(o.imagemOferta);
        const nome = escaparHtml(o.nome || "Oferta");

        const imgTag = imagemOferta
            ? `<img src="${imagemOferta}" alt="${nome}" onclick="abrirModal('${encodeURIComponent(imagemOferta)}')" style="cursor:pointer">`
            : `<div style="height:160px;display:grid;place-items:center;background:var(--surface-2)">Oferta</div>`;

        return `
            <div class="oferta-card">
                ${imgTag}
                <div class="oferta-card-body">
                    <h4 title="${nome}">${nome}</h4>
                    <div class="preco">${precoFmt}</div>
                    <div style="font-size:12px;color:var(--muted);margin-top:auto;">${status}</div>
                </div>
            </div>`;
    }).join("");
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

function normalizarUrlArquivo(url) {
    if (!url) return "";
    if (/^(https?:|data:|blob:)/i.test(url)) return url;
    return `${BASE_URL}${url.startsWith("/") ? url : `/${url}`}`;
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

function showToast(msg, type = "success") {
    if (typeof APP_CONFIG.toast === "function") {
        APP_CONFIG.toast(msg, type);
        return;
    }
    console[type === "error" ? "error" : "log"](msg);
}

function abrirModal(src) {
    const modal = document.getElementById("modalImagem");
    const foto = document.getElementById("modalFoto");

    foto.src = decodeURIComponent(src);
    foto.classList.remove("expandida");
    modal.style.display = "flex";
}

document.getElementById("modalFoto").onclick = function(e) {
    e.stopPropagation();
    this.classList.toggle("expandida");
};

document.getElementById("modalImagem").onclick = function() {
    this.style.display = "none";
    document.getElementById("modalFoto").classList.remove("expandida");
};

window.carregarDashboard = carregarDashboard;
window.abrirModal = abrirModal;

carregarDashboard();
