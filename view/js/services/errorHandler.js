/**
 * Error Handler Service
 * Tratamento centralizado de erros
 */

const errorHandler = {
  /**
   * Log levels
   */
  LEVELS: {
    DEBUG: 0,
    INFO: 1,
    WARN: 2,
    ERROR: 3,
    FATAL: 4,
  },

  /**
   * Nível mínimo de log
   */
  minLevel: 0,

  /**
   * Histórico de erros
   */
  history: [],

  /**
   * Máximo de erros no histórico
   */
  maxHistory: 100,

  /**
   * Mapeia código de erro para mensagem amigável
   */
  errorMessages: {
    400: 'Requisição inválida. Verifique os dados enviados.',
    401: 'Não autorizado. Faça login novamente.',
    403: 'Acesso proibido. Você não tem permissão.',
    404: 'Recurso não encontrado.',
    409: 'Conflito. O recurso já existe ou está em uso.',
    500: 'Erro interno do servidor. Tente mais tarde.',
    503: 'Serviço indisponível. Tente mais tarde.',
  },

  /**
   * Log genérico
   */
  log(level, message, data = null) {
    if (level < this.minLevel) return;

    const timestamp = new Date().toISOString();
    const levelName = Object.keys(this.LEVELS).find(key => this.LEVELS[key] === level);

    const logEntry = {
      timestamp,
      level: levelName,
      message,
      data,
    };

    this.history.push(logEntry);
    if (this.history.length > this.maxHistory) {
      this.history.shift();
    }

    // Log no console também
    console[levelName.toLowerCase()](
      `[${timestamp}] ${levelName}: ${message}`,
      data || ''
    );

    // Se é erro crítico, pode disparar evento
    if (level >= this.LEVELS.ERROR) {
      this.dispatchErrorEvent(logEntry);
    }
  },

  /**
   * Log de debug
   */
  debug(message, data = null) {
    this.log(this.LEVELS.DEBUG, message, data);
  },

  /**
   * Log de info
   */
  info(message, data = null) {
    this.log(this.LEVELS.INFO, message, data);
  },

  /**
   * Log de warning
   */
  warn(message, data = null) {
    this.log(this.LEVELS.WARN, message, data);
  },

  /**
   * Log de erro
   */
  error(message, data = null) {
    this.log(this.LEVELS.ERROR, message, data);
  },

  /**
   * Log de erro fatal
   */
  fatal(message, data = null) {
    this.log(this.LEVELS.FATAL, message, data);
  },

  /**
   * Formata erro HTTP para mensagem amigável
   */
  getErrorMessage(statusCode, fallback = 'Ocorreu um erro. Tente novamente.') {
    return this.errorMessages[statusCode] || fallback;
  },

  /**
   * Trata resultado de API
   */
  handleApiResult(result, context = '') {
    if (result.error) {
      this.error(`Erro na API (${context})`, result);
      return false;
    }
    return true;
  },

  /**
   * Dispara evento de erro global
   */
  dispatchErrorEvent(logEntry) {
    const event = new CustomEvent('apiError', { detail: logEntry });
    window.dispatchEvent(event);
  },

  /**
   * Recupera histórico de logs
   */
  getHistory(level = null) {
    if (level === null) return this.history;
    return this.history.filter(log => log.level === level);
  },

  /**
   * Limpa histórico
   */
  clearHistory() {
    this.history = [];
  },

  /**
   * Exporta logs como JSON
   */
  exportLogs() {
    return JSON.stringify(this.history, null, 2);
  },

  /**
   * Trata erro JavaScript global
   */
  handleGlobalError(message, source, lineno, colno, error) {
    this.fatal('Erro JavaScript global', {
      message,
      source,
      lineno,
      colno,
      stack: error?.stack,
    });
  },

  /**
   * Trata rejeição de promise não capturada
   */
  handleUnhandledRejection(reason) {
    this.fatal('Promise rejeitada não capturada', {
      reason: reason?.message || reason,
    });
  },
};

// Registra handlers globais
window.addEventListener('error', (e) => {
  errorHandler.handleGlobalError(
    e.message,
    e.filename,
    e.lineno,
    e.colno,
    e.error
  );
});

window.addEventListener('unhandledrejection', (e) => {
  errorHandler.handleUnhandledRejection(e.reason);
});

// Export
window.errorHandler = errorHandler;
