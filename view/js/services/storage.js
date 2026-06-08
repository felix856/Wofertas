/**
 * Storage Service
 * Abstração sobre localStorage e sessionStorage
 */

const storageService = {
  /**
   * Define tipo padrão de storage (localStorage ou sessionStorage)
   */
  defaultStorage: localStorage,

  /**
   * Salva valor no storage
   * @param {string} key
   * @param {any} value
   * @param {boolean} session - Se true, usa sessionStorage; senão localStorage
   */
  set(key, value, session = false) {
    const storage = session ? sessionStorage : this.defaultStorage;
    try {
      const json = typeof value === 'string' ? value : JSON.stringify(value);
      storage.setItem(key, json);
      return true;
    } catch (err) {
      console.error('Erro ao salvar no storage:', err);
      return false;
    }
  },

  /**
   * Recupera valor do storage
   * @param {string} key
   * @param {boolean} session - Se true, usa sessionStorage; senão localStorage
   * @returns {any} Valor ou null se não encontrado
   */
  get(key, session = false) {
    const storage = session ? sessionStorage : this.defaultStorage;
    try {
      const valor = storage.getItem(key);
      if (valor === null) return null;
      
      // Tenta parsear como JSON
      try {
        return JSON.parse(valor);
      } catch {
        return valor; // Retorna como string se não for JSON válido
      }
    } catch (err) {
      console.error('Erro ao recuperar do storage:', err);
      return null;
    }
  },

  /**
   * Remove valor do storage
   * @param {string} key
   * @param {boolean} session
   */
  remove(key, session = false) {
    const storage = session ? sessionStorage : this.defaultStorage;
    try {
      storage.removeItem(key);
      return true;
    } catch (err) {
      console.error('Erro ao remover do storage:', err);
      return false;
    }
  },

  /**
   * Limpa todo o storage
   * @param {boolean} session
   */
  clear(session = false) {
    const storage = session ? sessionStorage : this.defaultStorage;
    try {
      storage.clear();
      return true;
    } catch (err) {
      console.error('Erro ao limpar storage:', err);
      return false;
    }
  },

  /**
   * Obtém todas as chaves
   * @param {boolean} session
   */
  keys(session = false) {
    const storage = session ? sessionStorage : this.defaultStorage;
    try {
      return Object.keys(storage);
    } catch (err) {
      console.error('Erro ao listar chaves:', err);
      return [];
    }
  },

  /**
   * Verifica se chave existe
   * @param {string} key
   * @param {boolean} session
   */
  has(key, session = false) {
    const storage = session ? sessionStorage : this.defaultStorage;
    try {
      return storage.getItem(key) !== null;
    } catch (err) {
      return false;
    }
  },

  /**
   * Salva com expiração (em segundos)
   * @param {string} key
   * @param {any} value
   * @param {number} segundos - Tempo de expiração em segundos
   * @param {boolean} session
   */
  setWithExpiry(key, value, segundos, session = false) {
    const expiryTime = Date.now() + segundos * 1000;
    const data = {
      value,
      expiry: expiryTime,
    };
    return this.set(key, data, session);
  },

  /**
   * Recupera valor com verificação de expiração
   * @param {string} key
   * @param {boolean} session
   */
  getWithExpiry(key, session = false) {
    const data = this.get(key, session);
    if (!data || !data.expiry) return data; // Compatibilidade com dados antigos

    if (Date.now() > data.expiry) {
      this.remove(key, session);
      return null;
    }

    return data.value;
  },
};

// Export
window.storageService = storageService;
