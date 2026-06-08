/**
 * Dashboard page script
 * Carrega mercados e ofertas próximas e renderiza listas simples
 */

async function getPositionOrDefault() {
  return new Promise((resolve) => {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (pos) => resolve({ lat: pos.coords.latitude, lng: pos.coords.longitude }),
        () => resolve({ lat: -23.561414, lng: -46.655881 })
      );
    } else {
      resolve({ lat: -23.561414, lng: -46.655881 });
    }
  });
}

function renderList(title, items, mapper) {
  const section = document.createElement('section');
  section.className = 'card';
  const h = document.createElement('h3');
  h.textContent = title;
  section.appendChild(h);

  const ul = document.createElement('ul');
  ul.style.listStyle = 'none';
  ul.style.padding = '0';

  if (!items || items.length === 0) {
    const li = document.createElement('li');
    li.textContent = 'Nenhum resultado';
    ul.appendChild(li);
  } else {
    items.forEach(it => {
      const li = document.createElement('li');
      li.style.padding = '8px 0';
      li.textContent = mapper(it);
      ul.appendChild(li);
    });
  }

  section.appendChild(ul);
  return section;
}

async function initDashboardPage() {
  try {
    const loading = document.getElementById('loading');
    const content = document.getElementById('content');
    const errorBox = document.getElementById('error-box');

    // Mostra loading
    if (loading) loading.style.display = '';
    if (content) content.style.display = 'none';
    if (errorBox) errorBox.style.display = 'none';

    // Pega dashboard do mercado logado
    const res = await mercadosAPI.getDashboardCurrent();
    if (res.status === 401) {
      throw new Error('Não autorizado. Faça login novamente (401)');
    }
    if (res.status === 403) {
      throw new Error('Acesso negado. Sua conta não tem permissão para acessar o dashboard (403)');
    }
    if (res.error) {
      throw new Error(res.error || 'Erro ao buscar dashboard');
    }

    const data = res.data;

    // Preenche KPIs
    document.getElementById('metricVisualizations').textContent = data.totalVisualizacoes ?? 0;
    document.getElementById('metricLikes').textContent = data.totalCurtidas ?? 0;
    document.getElementById('metricCart').textContent = data.totalItensCarrinho ?? 0;
    document.getElementById('metricOffers').textContent = data.totalEncartes ?? 0;

    // Conversões
    const likePct = data.taxaConversaoVisualizacoesCurtidas ?? 0;
    const cartPct = data.taxaConversaoVisualizacoesCarrinho ?? 0;
    document.getElementById('conversionLikesPercent').textContent = `${likePct}%`;
    document.getElementById('conversionCartPercent').textContent = `${cartPct}%`;
    document.getElementById('conversionLikesFill').style.width = `${likePct}%`;
    document.getElementById('conversionCartFill').style.width = `${cartPct}%`;

    // Rankings
    function fillRanking(listId, items) {
      const ul = document.getElementById(listId);
      if (!ul) return;
      ul.innerHTML = '';
      if (!items || items.length === 0) {
        const li = document.createElement('li'); li.textContent = 'Sem dados'; ul.appendChild(li); return;
      }
      items.forEach((it, idx) => {
        const li = document.createElement('li'); li.className = 'ranking-item';
        li.innerHTML = `<div style="display:flex;align-items:center;"><div class="ranking-position">${idx+1}</div><div class="ranking-name">${it.nome}</div></div><div class="ranking-value">${it.curtidas ?? it.itensCarrinho ?? it.visualizacoes ?? ''}</div>`;
        ul.appendChild(li);
      });
    }

    fillRanking('rankingPerformance', data.encartesRanking);
    fillRanking('rankingLikes', data.encartesComMaiorCurtidas);
    fillRanking('rankingCart', data.encartesComMaiorCarrinho);

    // Top products
    function fillTopProducts(id, mapObj) {
      const ul = document.getElementById(id);
      if (!ul) return;
      ul.innerHTML = '';
      if (!mapObj || Object.keys(mapObj).length === 0) {
        const li = document.createElement('li'); li.textContent = 'Sem dados'; ul.appendChild(li); return;
      }
      Object.entries(mapObj).forEach(([k,v]) => {
        const li = document.createElement('li'); li.className = 'ranking-item'; li.innerHTML = `<div class="ranking-name">${k}</div><div class="ranking-value">${v}</div>`; ul.appendChild(li);
      });
    }

    fillTopProducts('topProductsLikes', data.produtosComMaiorCurtidas);
    fillTopProducts('topProductsCart', data.produtosComMaiorCarrinho);

    // Insight
    if (data.insight) {
      document.getElementById('insightTitle').textContent = data.insight.encarteMelhorPerformance ?? data.insight.melhorEncarte ?? '—';
      document.getElementById('insightText').textContent = data.insight.recomendacao ?? '';
      document.getElementById('clientsActive').textContent = data.insight.clientesAtivos ?? 0;
    }

    // Exibe conteúdo
    if (loading) loading.style.display = 'none';
    if (content) content.style.display = '';
  } catch (err) {
    console.error('Erro ao inicializar dashboard:', err);
    const errorBox = document.getElementById('error-box');
    if (errorBox) {
      errorBox.style.display = '';
      document.getElementById('error-msg').textContent = 'Falha ao carregar dados';
      document.getElementById('error-detail').textContent = err.message || String(err);
    }
    notificationsService.error('Erro ao carregar dados do dashboard');
  }
}

// Inicia quando módulos estiverem prontos
window.addEventListener('modulesLoaded', initDashboardPage);
if (window.mercadosAPI && window.ofertasAPI) initDashboardPage();
