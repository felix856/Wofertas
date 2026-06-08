/**
 * Marketplace API Endpoints
 * /api/mercados/* endpoints
 */

const mercadosAPI = {
  /**
   * Lista todos os mercados
   * @param {Object} filters - { ativo, skip, limit, search }
   * @returns {Promise<Object>}
   */
  async listAll(filters = {}) {
    const params = new URLSearchParams();
    if (filters.ativo !== undefined) params.append('ativo', filters.ativo);
    if (filters.skip !== undefined) params.append('skip', filters.skip);
    if (filters.limit !== undefined) params.append('limit', filters.limit);
    if (filters.search) params.append('search', filters.search);
    
    const query = params.toString() ? `?${params.toString()}` : '';
    return await apiClient.get(`/mercados${query}`);
  },

  /**
   * Obtém mercado por ID
   * @param {string} id - ID do mercado
   * @returns {Promise<Object>}
   */
  async getById(id) {
    return await apiClient.get(`/mercados/${id}`);
  },

  /**
   * Cria novo mercado
   * @param {Object} dados - Dados do mercado
   * @returns {Promise<Object>}
   */
  async create(dados) {
    return await apiClient.post('/mercados', dados);
  },

  /**
   * Atualiza mercado
   * @param {string} id - ID do mercado
   * @param {Object} dados - Dados a atualizar
   * @returns {Promise<Object>}
   */
  async update(id, dados) {
    return await apiClient.put(`/mercados/${id}`, dados);
  },

  /**
   * Troca senha do mercado
   * @param {string} id - ID do mercado
   * @param {string} senhaAtual
   * @param {string} novaSenha
   * @param {string} confirmacao
   * @returns {Promise<Object>}
   */
  async trocaSenha(id, senhaAtual, novaSenha, confirmacao) {
    return await apiClient.put(`/mercados/${id}/senha`, {
      senhaAtual,
      novaSenha,
      confirmacao,
    });
  },

  /**
   * Faz upload de logo
   * @param {string} id - ID do mercado
   * @param {File} file - Arquivo de imagem
   * @returns {Promise<Object>}
   */
  async uploadLogo(id, file) {
    const formData = new FormData();
    formData.append('file', file);
    return await apiClient.post(`/mercados/${id}/logo`, formData, true);
  },

  /**
   * Deleta mercado
   * @param {string} id - ID do mercado
   * @returns {Promise<Object>}
   */
  async delete(id) {
    return await apiClient.delete(`/mercados/${id}`);
  },

  /**
   * Obtém mercados próximos (geolocalização)
   * @param {number} raioKm - Raio em km
   * @param {number} latitude
   * @param {number} longitude
   * @returns {Promise<Object>}
   */
  async getProximos(raioKm = 10, latitude, longitude) {
    const params = new URLSearchParams({
      raioKm,
      latitude,
      longitude,
    });
    return await apiClient.get(`/mercados/proximos?${params.toString()}`);
  },

  /**
   * Obtém dashboard do mercado (analytics)
   * @param {string} id - ID do mercado
   * @returns {Promise<Object>}
   */
  async getDashboard(id) {
    return await apiClient.get(`/mercados/${id}/dashboard`);
  },

  /**
   * Obtém dashboard do mercado logado (endereço /analytics/dashboard)
   * Útil para frontend que não precisa passar ID (token determina o mercado)
   */
  async getDashboardCurrent() {
    // Primeiro tenta via API base (ex: http://localhost:8080/api/analytics/dashboard)
    try {
      const res = await apiClient.get(`/analytics/dashboard`);
      // Se não encontrou ou erro 500, tenta fallback sem o prefixo /api
      if (res.status === 500 || res.status === 404) {
        try {
          const fallbackUrl = `${apiClient.getServerOrigin()}/analytics/dashboard`;
          const headers = apiClient.getHeaders();
          const r = await fetch(fallbackUrl, { method: 'GET', headers });
          const text = await r.text();
          if (!r.ok) {
            return { data: null, status: r.status, error: text || `Fallback failed: ${r.status}` };
          }
          const data = JSON.parse(text);
          return { data, status: r.status, error: null };
        } catch (e) {
          return { data: null, status: res.status, error: res.error || e.message };
        }
      }
      return res;
    } catch (e) {
      return { data: null, status: 0, error: e.message };
    }
  },

  /**
   * Ativa/desativa mercado
   * @param {string} id - ID do mercado
   * @param {boolean} ativo
   * @returns {Promise<Object>}
   */
  async setActive(id, ativo) {
    return await apiClient.patch(`/mercados/${id}`, { ativo });
  },
};

// Export para uso global
window.mercadosAPI = mercadosAPI;
