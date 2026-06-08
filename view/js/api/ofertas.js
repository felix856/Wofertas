/**
 * Offers API Endpoints
 * /api/ofertas/* endpoints
 */

const ofertasAPI = {
  /**
   * Lista todas as ofertas
   * @param {Object} filters - { ativo, mercadoId, skip, limit, search }
   * @returns {Promise<Object>}
   */
  async listAll(filters = {}) {
    const params = new URLSearchParams();
    if (filters.ativo !== undefined) params.append('ativo', filters.ativo);
    if (filters.mercadoId) params.append('mercadoId', filters.mercadoId);
    if (filters.skip !== undefined) params.append('skip', filters.skip);
    if (filters.limit !== undefined) params.append('limit', filters.limit);
    if (filters.search) params.append('search', filters.search);
    
    const query = params.toString() ? `?${params.toString()}` : '';
    return await apiClient.get(`/ofertas${query}`);
  },

  /**
   * Obtém oferta por ID
   * @param {string} id - ID da oferta
   * @returns {Promise<Object>}
   */
  async getById(id) {
    return await apiClient.get(`/ofertas/${id}`);
  },

  /**
   * Cria nova oferta
   * @param {Object} dados - Dados da oferta
   * @returns {Promise<Object>}
   */
  async create(dados) {
    return await apiClient.post('/ofertas', dados);
  },

  /**
   * Atualiza oferta
   * @param {string} id - ID da oferta
   * @param {Object} dados - Dados a atualizar
   * @returns {Promise<Object>}
   */
  async update(id, dados) {
    return await apiClient.put(`/ofertas/${id}`, dados);
  },

  /**
   * Deleta oferta
   * @param {string} id - ID da oferta
   * @returns {Promise<Object>}
   */
  async delete(id) {
    return await apiClient.delete(`/ofertas/${id}`);
  },

  /**
   * Obtém ofertas próximas (geolocalização)
   * @param {number} raioKm - Raio em km
   * @param {number} latitude
   * @param {number} longitude
   * @returns {Promise<Object>}
   */
  async getProximas(raioKm = 10, latitude, longitude) {
    const params = new URLSearchParams({
      raioKm,
      latitude,
      longitude,
    });
    return await apiClient.get(`/ofertas/proximas?${params.toString()}`);
  },

  /**
   * Obtém ofertas de um mercado específico
   * @param {string} mercadoId - ID do mercado
   * @returns {Promise<Object>}
   */
  async getByMercado(mercadoId) {
    return await apiClient.get(`/ofertas/mercado/${mercadoId}`);
  },

  /**
   * Busca ofertas por categoria
   * @param {string} categoria
   * @returns {Promise<Object>}
   */
  async getByCategoria(categoria) {
    return await apiClient.get(`/ofertas/categoria/${categoria}`);
  },

  /**
   * Ativa/desativa oferta
   * @param {string} id - ID da oferta
   * @param {boolean} ativo
   * @returns {Promise<Object>}
   */
  async setActive(id, ativo) {
    return await apiClient.patch(`/ofertas/${id}`, { ativo });
  },

  /**
   * Incrementa visualizações da oferta
   * @param {string} id - ID da oferta
   * @returns {Promise<Object>}
   */
  async recordView(id) {
    return await apiClient.post(`/ofertas/${id}/view`, {});
  },

  /**
   * Obtém estatísticas de uma oferta
   * @param {string} id - ID da oferta
   * @returns {Promise<Object>}
   */
  async getStats(id) {
    return await apiClient.get(`/ofertas/${id}/stats`);
  },
};

// Export para uso global
window.ofertasAPI = ofertasAPI;
