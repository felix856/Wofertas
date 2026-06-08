/**
 * User API Endpoints
 * /api/usuarios/* endpoints
 */

const usuariosAPI = {
  /**
   * Obtém perfil do usuário autenticado
   * @returns {Promise<Object>}
   */
  async getPerfil() {
    return await apiClient.get('/usuarios/perfil');
  },

  /**
   * Obtém usuário por ID
   * @param {string} id - ID do usuário
   * @returns {Promise<Object>}
   */
  async getById(id) {
    return await apiClient.get(`/usuarios/${id}`);
  },

  /**
   * Atualiza dados do usuário
   * @param {string} id - ID do usuário
   * @param {Object} dados - Dados a atualizar
   * @returns {Promise<Object>}
   */
  async update(id, dados) {
    return await apiClient.put(`/usuarios/${id}`, dados);
  },

  /**
   * Troca senha do usuário
   * @param {string} id - ID do usuário
   * @param {string} senhaAtual
   * @param {string} novaSenha
   * @param {string} confirmacao
   * @returns {Promise<Object>}
   */
  async trocaSenha(id, senhaAtual, novaSenha, confirmacao) {
    return await apiClient.put(`/usuarios/${id}/senha`, {
      senhaAtual,
      novaSenha,
      confirmacao,
    });
  },

  /**
   * Faz upload de foto de perfil
   * @param {string} id - ID do usuário
   * @param {File} file - Arquivo de imagem
   * @returns {Promise<Object>}
   */
  async uploadFoto(id, file) {
    const formData = new FormData();
    formData.append('file', file);
    return await apiClient.post(`/usuarios/${id}/foto`, formData, true);
  },

  /**
   * Lista todos os usuários (admin)
   * @returns {Promise<Object>}
   */
  async listAll() {
    return await apiClient.get('/usuarios');
  },

  /**
   * Deleta usuário
   * @param {string} id - ID do usuário
   * @returns {Promise<Object>}
   */
  async delete(id) {
    return await apiClient.delete(`/usuarios/${id}`);
  },

  /**
   * Busca usuário por email
   * @param {string} email
   * @returns {Promise<Object>}
   */
  async searchByEmail(email) {
    return await apiClient.get(`/usuarios/search?email=${encodeURIComponent(email)}`);
  },

  /**
   * Obtém histórico de compras do usuário
   * @returns {Promise<Object>}
   */
  async getHistorico() {
    return await apiClient.get('/usuarios/historico');
  },

  /**
   * Obtém notificações do usuário
   * @returns {Promise<Object>}
   */
  async getNotificacoes() {
    return await apiClient.get('/usuarios/notificacoes');
  },
};

// Export para uso global
window.usuariosAPI = usuariosAPI;
