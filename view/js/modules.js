/**
 * Initialization Module Loader
 * Carrega todas as dependências JavaScript na ordem correta
 */

const moduleLoader = {
  /**
   * Módulos carregados
   */
  loaded: [],

  /**
   * Carrega um script de forma síncrona
   */
  loadScript(src) {
    return new Promise((resolve, reject) => {
      const script = document.createElement('script');
      script.src = src;
      script.async = false;
      script.onerror = () => {
        console.error(`Falha ao carregar: ${src}`);
        reject(new Error(`Falha ao carregar: ${src}`));
      };
      script.onload = () => {
        console.log(`✓ Carregado: ${src}`);
        this.loaded.push(src);
        resolve();
      };
      document.head.appendChild(script);
    });
  },

  /**
   * Carrega todos os módulos
   */
  async loadAll() {
    console.log('🚀 Iniciando carregamento de módulos...');

    try {
      // Fase 1: Configuração Global e Client HTTP (base)
      console.log('📦 Fase 1: Core');
      await this.loadScript('js/config.js');
      await this.loadScript('js/api/client.js');

      // Fase 2: Serviços utilitários
      console.log('📦 Fase 2: Serviços');
      await this.loadScript('js/services/validators.js');
      await this.loadScript('js/services/storage.js');
      await this.loadScript('js/services/formatters.js');
      await this.loadScript('js/services/errorHandler.js');
      await this.loadScript('js/services/notifications.js');

      // Fase 3: APIs
      console.log('📦 Fase 3: APIs');
      await this.loadScript('js/api/auth.js');
      await this.loadScript('js/api/usuarios.js');
      await this.loadScript('js/api/mercados.js');
      await this.loadScript('js/api/ofertas.js');
      await this.loadScript('js/api/favoritos.js');

      console.log('✅ Todos os módulos carregados com sucesso!');
      
      // Dispara evento de inicialização completa
      window.dispatchEvent(new CustomEvent('modulesLoaded'));
      return true;
    } catch (err) {
      console.error('❌ Erro ao carregar módulos:', err);
      if (window.errorHandler && typeof errorHandler.fatal === 'function') {
        errorHandler.fatal('Erro ao carregar módulos JavaScript', { erro: err.message });
      }
      return false;
    }
  },

  /**
   * Status de carregamento
   */
  getStatus() {
    return {
      loaded: this.loaded,
      count: this.loaded.length,
    };
  },
};

// Inicia carregamento quando o DOM estiver pronto
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', () => {
    moduleLoader.loadAll();
  });
} else {
  moduleLoader.loadAll();
}

// Export
window.moduleLoader = moduleLoader;
