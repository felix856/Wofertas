const authAPI = {

  async login(email, senha) {
    const result = await apiClient.postPublic('/auth/login', { email, senha });

    if (result.error) return result;

    if (result.data?.token) {
      apiClient.setToken(result.data.token);
    }

    if (result.data?.usuario?.tipo) {
      try {
        localStorage.setItem('tipo', result.data.usuario.tipo);
      } catch (e) {
        console.warn('Não foi possível setar localStorage.tipo', e);
      }
    }

    return result;
  },

  logout() {
    apiClient.clearToken();
    return { data: { message: 'Logout realizado' }, status: 200, error: null };
  },

  async register(usuarioData) {
    // ✅ era /auth/register → correto é /auth/signup
    const result = await apiClient.postPublic('/auth/signup', usuarioData);

    if (result.error) return result;

    if (result.data?.token) {
      apiClient.setToken(result.data.token);
    }

    return result;
  },

  async requestPasswordReset(email) {
    // ✅ era /auth/solicitar-reset-senha → correto é /auth/forgot-password
    return await apiClient.postPublic('/auth/forgot-password', { email });
  },

  async resetPassword(token, novaSenha, confirmacao) {
    // ✅ esse estava correto
    return await apiClient.postPublic('/auth/reset-senha', {
      token,
      novaSenha,
      confirmacao,
    });
  },

  async validateToken() {
    // ✅ era /auth/validate-token → correto é /auth/validar-token
    return await apiClient.get('/auth/validar-token');
  },

  async refreshToken() {
    // ⚠️ /auth/refresh-token NÃO existe no backend
    // Retorna sucesso simulado para não quebrar quem chama
    console.warn('refreshToken: endpoint não implementado no backend');
    return { data: null, status: 501, error: 'Não implementado' };
  },

};

window.authAPI = authAPI;