const BASE_URL = window.AppConfig?.API_URL || "https://wofertas.koyeb.app";
const token    = localStorage.getItem("token");

if (!token) { window.location.href = "login.html"; }

// Saudação
const h = new Date().getHours();
document.getElementById("saudacao").textContent = (h < 12 ? "Bom dia" : h < 18 ? "Boa tarde" : "Boa noite") + "! 👋";

async function carregarDashboard() {
    const res = await window.AppConfig.safeFetch(`/ofertas/historico`);

    if (!res.ok) {
        window.AppConfig.toast("Não foi possível carregar o dashboard: " + res.error, "error");
        document.getElementById("ultimasOfertas").innerHTML = `<div class="msg-vazia" style="color:var(--danger)">Erro de conexão com o servidor.</div>`;
        return;
    }

    const ofertas = Array.isArray(res.data) ? res.data : [];

    document.getElementById("statTotal").textContent    = ofertas.length;
    document.getElementById("statAtivas").textContent   = ofertas.filter(o => o.status === "ATIVO").length;
    document.getElementById("statSuspensas").textContent = ofertas.filter(o => o.status === "SUSPENSO").length;

        const grid = document.getElementById("ultimasOfertas");
        const lista = ofertas.slice(0, 8); 

        if (lista.length === 0) {
            grid.innerHTML = `<div class="msg-vazia">Nenhuma oferta publicada ainda.</div>`;
            return;
        }

        grid.innerHTML = lista.map(o => {
            const statusTxt = (o.status || "ATIVO") === "ATIVO" ? "✅ Ativo" : "🔴 Suspenso";
            const precoFmt  = o.preco ? `R$ ${parseFloat(o.preco).toFixed(2).replace('.',',')}` : "Consulte";

            const imgTag = o.imagemOferta
                ? `<img src="${o.imagemOferta}" alt="${o.nome}" onclick="abrirModal('${encodeURIComponent(o.imagemOferta)}')" style="cursor:pointer">`
                : `<div style="height:160px; display:grid; place-items:center; background:var(--surface-2)">🏷️</div>`;

            return `
            <div class="oferta-card">
                ${imgTag}
                <div class="oferta-card-body">
                    <h4 title="${o.nome}">${o.nome}</h4>
                    <div class="preco">${precoFmt}</div>
                    <div style="font-size: 12px; color: var(--muted); margin-top: auto;">${statusTxt}</div>
                </div>
            </div>`;
        }).join("");
}

// Lógica de Zoom e Modal
function abrirModal(src) {
    const modal = document.getElementById("modalImagem");
    const foto = document.getElementById("modalFoto");

    foto.src = decodeURIComponent(src);
    foto.classList.remove("expandida"); // Reseta zoom ao abrir nova imagem
    modal.style.display = "flex";
}

// Clique na FOTO: Alterna Expansão (Zoom)
document.getElementById("modalFoto").onclick = function(e) {
    e.stopPropagation(); // Impede que o clique na foto feche o modal
    this.classList.toggle("expandida");
};

// Clique no FUNDO: Fecha o modal
document.getElementById("modalImagem").onclick = function() {
    this.style.display = "none";
    document.getElementById("modalFoto").classList.remove("expandida");
};

carregarDashboard();
