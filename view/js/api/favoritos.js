/**
 * Favorites API Endpoints
 * /api/favoritos/* endpoints
 */

const favoritosAPI = {
  /**
   * Lista favoritos do usuário
   * @returns {Promise<Object>}
   */
  async listMeus() {
    return await apiClient.get('/favoritos');
  },

  /**
   * Adiciona oferta aos favoritos
   * @param {string} ofertaId - ID da oferta
   * @returns {Promise<Object>}
   */
  async adicionar(ofertaId) {
    return await apiClient.post('/favoritos', { ofertaId });
  },

  /**
   * Remove oferta dos favoritos
   * @param {string} ofertaId - ID da oferta
   * @returns {Promise<Object>}
   */
  async remover(ofertaId) {
    return await apiClient.delete(`/favoritos/${ofertaId}`);
  },

  /**
   * Verifica se oferta está nos favoritos
   * @param {string} ofertaId - ID da oferta
   * @returns {Promise<Object>}
   */
  async isFavorito(ofertaId) {
    const result = await apiClient.get(`/favoritos/${ofertaId}`);
    return result;
  },

  /**
   * Limpa todos os favoritos
   * @returns {Promise<Object>}
   */
  async limparTodos() {
    return await apiClient.delete('/favoritos/limpar');
  },

  /**
   * Obtém contagem de favoritos
   * @returns {Promise<Object>}
   */
  async contar() {
    return await apiClient.get('/favoritos/count');
  },

  /**
   * Exporta favoritos como JSON
   * @returns {Promise<Object>}
   */
  async exportar() {
    return await apiClient.get('/favoritos/export');
  },

  /**
   * Importa favoritos de um arquivo JSON
   * @param {Object} dados - Dados de favoritos
   * @returns {Promise<Object>}
   */
  async importar(dados) {
    return await apiClient.post('/favoritos/import', dados);
  },
};

// Export para uso global
window.favoritosAPI = favoritosAPI;
