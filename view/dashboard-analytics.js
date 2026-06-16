/*
 * Dashboard analytics.
 * Recebe dados de GET /analytics/dashboard e renderiza no HTML.
 */

const BASE_URL = window.AppConfig?.API_URL || "https://wofertas.koyeb.app";
const token = localStorage.getItem("token");
const tipo = localStorage.getItem("tipo");

console.log("Dashboard Analytics:", {
    BASE_URL,
    token: token ? "presente" : "ausente",
    tipo
});

if (!token) {
    window.location.href = "login.html";
}

let refreshTimer = null;

function byId(id) {
    const element = document.getElementById(id);
    if (!element) {
        console.warn(`[dashboard] Elemento ausente no HTML: #${id}`);
    }
    return element;
}

function setText(id, value) {
    const element = byId(id);
    if (element) {
        element.textContent = value == null ? "" : String(value);
    }
}

function setDisplay(id, value) {
    const element = byId(id);
    if (element) {
        element.style.display = value;
    }
}

function setWidth(id, value) {
    const element = byId(id);
    if (element) {
        element.style.width = value;
    }
}

function mostrarErro(msg, detalhe) {
    setDisplay("loading", "none");
    setDisplay("content", "none");
    setDisplay("error-box", "block");
    setText("error-msg", msg || "Erro desconhecido.");
    setText("error-detail", detalhe || "");
    setDisplay("error-detail", detalhe ? "block" : "none");
}

function mostrarLoading(texto) {
    setText("loading-text", texto || "Carregando dados...");
    setDisplay("loading", "block");
    setDisplay("content", "none");
    setDisplay("error-box", "none");
}

function formatNumber(num) {
    const value = Number(num);
    if (!Number.isFinite(value)) return "0";
    if (value >= 1000000) return (value / 1000000).toFixed(1) + "M";
    if (value >= 1000) return (value / 1000).toFixed(1) + "K";
    return String(value);
}

function escapeHtml(text) {
    const div = document.createElement("div");
    div.textContent = text == null ? "" : String(text);
    return div.innerHTML;
}

async function fetchComTimeout(url, opcoes, timeoutMs = 15000) {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), timeoutMs);

    try {
        return await fetch(url, { ...opcoes, signal: controller.signal });
    } catch (err) {
        if (err.name === "AbortError") {
            throw new Error("O servidor demorou muito para responder.");
        }
        throw err;
    } finally {
        clearTimeout(timer);
    }
}

async function loadDashboard() {
    if (!token) return;

    mostrarLoading("Sincronizando com o servidor...");

    try {
        const [dashResp, rankResp] = await Promise.all([
            fetchComTimeout(
                `${BASE_URL}/analytics/dashboard`,
                { headers: { Authorization: `Bearer ${token}` } },
                15000
            ),
            fetchComTimeout(
                `${BASE_URL}/analytics/ranking-mercados`,
                { headers: { Authorization: `Bearer ${token}` } },
                15000
            ).catch(err => {
                console.warn("[dashboard] Erro ao buscar ranking de mercados:", err);
                return null;
            })
        ]);

        console.log("[dashboard] Respostas obtidas");

        if (dashResp.status === 401 || dashResp.status === 403) {
            mostrarErro("Sessao expirada. Faca login novamente.", `HTTP ${dashResp.status}`);
            setTimeout(() => {
                localStorage.clear();
                window.location.href = "login.html";
            }, 2500);
            return;
        }

        if (!dashResp.ok) {
            mostrarErro(`Erro no servidor (HTTP ${dashResp.status})`, dashResp.statusText);
            return;
        }

        const data = await dashResp.json();
        let rankingMercados = [];
        if (rankResp && rankResp.ok) {
            rankingMercados = await rankResp.json();
        }

        console.log("[dashboard] Dados e Ranking carregados:", { data, rankingMercados });

        let mercadoIdLogado = "";
        try {
            const merc = JSON.parse(localStorage.getItem("mercado"));
            if (merc && merc.id) mercadoIdLogado = merc.id;
        } catch (e) {
            console.warn("Nao foi possivel obter mercado logado do localStorage:", e);
        }

        renderDashboard(data || {});
        renderCompetidoresRanking(rankingMercados, mercadoIdLogado);
    } catch (err) {
        console.error("[dashboard] Falha:", err);
        mostrarErro(
            "Falha ao carregar o dashboard.",
            `Verifique se o backend esta rodando em ${BASE_URL}. Detalhe: ${err.message}`
        );
    }
}

function renderDashboard(data) {
    setText("metricVisualizations", formatNumber(data.totalVisualizacoes));
    const totalCurtidas = Number(data.totalCurtidas || 0);
    const totalFavoritos = Number(data.totalFavoritos || 0);
    setText("metricLikes", formatNumber(totalCurtidas + totalFavoritos));
    setText("metricCart", formatNumber(data.totalItensCarrinho));
    setText("metricOffers", formatNumber(data.totalEncartes));

    const insight = data.insight || {};
    setText("insightTitle", insight.encarteMelhorPerformance || insight.melhorEncarte || "Sem destaques ainda");
    setText("insightText", insight.recomendacao || "Continue publicando ofertas para gerar dados.");
    setText("clientsActive", formatNumber(insight.clientesAtivos));

    const taxaCurtidas = safePercent(data.taxaConversaoVisualizacoesCurtidas);
    const taxaCarrinho = safePercent(data.taxaConversaoVisualizacoesCarrinho);

    setText("conversionLikesPercent", taxaCurtidas.toFixed(1) + "%");
    setText("conversionCartPercent", taxaCarrinho.toFixed(1) + "%");
    setWidth("conversionLikesFill", Math.min(taxaCurtidas, 100) + "%");
    setWidth("conversionCartFill", Math.min(taxaCarrinho, 100) + "%");

    renderRanking(data.encartesRanking, "rankingPerformance", "engajamento");
    renderRanking(data.encartesComMaiorCurtidas, "rankingLikes", "curtidas");
    renderRanking(data.encartesComMaiorCarrinho, "rankingCart", "itensCarrinho");

    renderPreferencias(data.produtosComMaiorCurtidas, "topProductsLikes");
    renderPreferencias(data.produtosComMaiorCarrinho, "topProductsCart");

    setDisplay("loading", "none");
    setDisplay("error-box", "none");
    setDisplay("content", "block");
}

function renderCompetidoresRanking(ranking, currentMercadoId) {
    const list = byId("rankingConcorrentes");
    if (!list) return;

    list.innerHTML = "";
    if (!Array.isArray(ranking) || ranking.length === 0) {
        list.innerHTML = '<li class="empty-list" style="padding: 20px; text-align: center; color: rgba(255,255,255,0.5);">Nenhum concorrente cadastrado ainda.</li>';
        return;
    }

    ranking.forEach((merc, index) => {
        const isCurrent = merc.id === currentMercadoId;
        const li = document.createElement("li");
        li.className = "ranking-item";
        if (isCurrent) {
            li.style.background = "rgba(249,115,22,0.15)";
            li.style.borderLeft = "4px solid #f97316";
            li.style.fontWeight = "bold";
        }
        
        const logoUrl = merc.imagemLogo ? (merc.imagemLogo.startsWith("http") ? merc.imagemLogo : `${BASE_URL}${merc.imagemLogo}`) : "imagens/imagemLogo.png";
        
        li.innerHTML = `
            <div style="display:flex;align-items:center;flex:1;">
                <span class="ranking-position" style="${isCurrent ? 'background:#f97316;color:#ffffff;' : ''}">#${index + 1}</span>
                <img src="${logoUrl}" alt="Logo" style="width:32px;height:32px;border-radius:50%;margin-right:12px;object-fit:cover;border:1px solid rgba(255,255,255,0.2);">
                <span class="ranking-name" style="${isCurrent ? 'color:#ffffff;' : ''}">${escapeHtml(merc.nome)} ${isCurrent ? ' <span style="background:#f97316;color:#ffffff;font-size:10px;padding:2px 6px;border-radius:10px;margin-left:8px;font-weight:bold;">VOCÊ</span>' : ''}</span>
            </div>
            <div style="text-align:right;">
                <span class="ranking-value" style="${isCurrent ? 'color:#f97316;' : ''}">${formatNumber(merc.totalCurtidas)} curtidas</span>
                <div style="font-size:11px;color:rgba(255,255,255,0.4);">${formatNumber(merc.totalFavoritos)} favoritos</div>
            </div>
        `;
        list.appendChild(li);
    });
}

function safePercent(value) {
    const number = Number(value);
    if (!Number.isFinite(number) || number < 0) return 0;
    return Math.round(number * 10) / 10;
}

function renderRanking(encartes, elementId, campo) {
    const list = byId(elementId);
    if (!list) return;

    list.innerHTML = "";
    const items = Array.isArray(encartes) ? encartes : [];

    if (items.length === 0) {
        list.innerHTML = '<li class="empty-list">Sem dados disponiveis ainda.</li>';
        return;
    }

    items.slice(0, 5).forEach((encarte, index) => {
        const li = document.createElement("li");
        li.className = "ranking-item";
        li.innerHTML = `
            <div style="display:flex;align-items:center;flex:1;">
                <span class="ranking-position">#${index + 1}</span>
                <span class="ranking-name">${escapeHtml(encarte.nome || "Oferta sem nome")}</span>
            </div>
            <span class="ranking-value">${formatRankingValue(encarte, campo)}</span>
        `;
        list.appendChild(li);
    });
}

function formatRankingValue(encarte, campo) {
    if (campo === "engajamento") {
        return safePercent(encarte.engajamento).toFixed(1) + "%";
    }

    if (campo === "curtidas") {
        return formatNumber(encarte.curtidas);
    }

    if (campo === "itensCarrinho") {
        return formatNumber(encarte.itensCarrinho);
    }

    return "0";
}

function renderPreferencias(products, elementId) {
    const list = byId(elementId);
    if (!list) return;

    list.innerHTML = "";
    const entries = products && typeof products === "object" ? Object.entries(products) : [];

    if (entries.length === 0) {
        list.innerHTML = '<li class="empty-list">Sem registros ainda.</li>';
        return;
    }

    entries
        .sort((a, b) => Number(b[1] || 0) - Number(a[1] || 0))
        .slice(0, 5)
        .forEach(([nome, count], index) => {
            const li = document.createElement("li");
            li.className = "ranking-item";
            li.innerHTML = `
                <div style="display:flex;align-items:center;flex:1;">
                    <span class="ranking-position">#${index + 1}</span>
                    <span class="ranking-name">${escapeHtml(nome)}</span>
                </div>
                <span class="ranking-value">${formatNumber(count)}</span>
            `;
            list.appendChild(li);
        });
}

function recarregar() {
    loadDashboard();
}

function logout() {
    localStorage.clear();
    window.location.href = "login.html";
}

window.recarregar = recarregar;
window.logout = logout;

document.addEventListener("DOMContentLoaded", () => {
    loadDashboard();

    if (!refreshTimer) {
        refreshTimer = setInterval(loadDashboard, 60000);
    }
});
