/**
 * Notifications Service
 * Sistema de notificações (toasts) na UI
 */

const notificationsService = {
  /**
   * Tipos de notificação
   */
  TYPES: {
    SUCCESS: 'success',
    ERROR: 'error',
    INFO: 'info',
    WARNING: 'warning',
  },

  /**
   * Container de notificações
   */
  container: null,

  /**
   * Inicializa o container de notificações
   */
  init() {
    if (this.container) return;

    this.container = document.createElement('div');
    this.container.id = 'notifications-container';
    this.container.style.cssText = `
      position: fixed;
      top: 20px;
      right: 20px;
      z-index: 10000;
      max-width: 400px;
      max-height: 80vh;
      overflow-y: auto;
      font-family: inherit;
    `;
    document.body.appendChild(this.container);
  },

  /**
   * Cria elemento de notificação
   */
  createNotificationElement(message, type = 'info') {
    const notif = document.createElement('div');
    const colors = {
      success: { bg: '#d4edda', border: '#c3e6cb', text: '#155724' },
      error: { bg: '#f8d7da', border: '#f5c6cb', text: '#721c24' },
      info: { bg: '#d1ecf1', border: '#bee5eb', text: '#0c5460' },
      warning: { bg: '#fff3cd', border: '#ffeeba', text: '#856404' },
    };

    const color = colors[type] || colors.info;

    notif.style.cssText = `
      background-color: ${color.bg};
      border: 1px solid ${color.border};
      color: ${color.text};
      padding: 12px 16px;
      margin-bottom: 10px;
      border-radius: 4px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      animation: slideIn 0.3s ease-out;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    `;

    notif.innerHTML = `
      <span>${message}</span>
      <button style="
        background: none;
        border: none;
        color: ${color.text};
        cursor: pointer;
        font-size: 18px;
        padding: 0;
        margin-left: 12px;
      ">×</button>
    `;

    notif.querySelector('button').addEventListener('click', () => {
      notif.style.animation = 'slideOut 0.3s ease-out';
      setTimeout(() => notif.remove(), 300);
    });

    return notif;
  },

  /**
   * Mostra notificação genérica
   */
  show(message, type = 'info', duration = 4000) {
    this.init();

    const element = this.createNotificationElement(message, type);
    this.container.appendChild(element);

    if (duration > 0) {
      setTimeout(() => {
        if (element.parentElement) {
          element.style.animation = 'slideOut 0.3s ease-out';
          setTimeout(() => element.remove(), 300);
        }
      }, duration);
    }

    return element;
  },

  /**
   * Mostra sucesso
   */
  success(message, duration = 3000) {
    return this.show(message, this.TYPES.SUCCESS, duration);
  },

  /**
   * Mostra erro
   */
  error(message, duration = 5000) {
    return this.show(message, this.TYPES.ERROR, duration);
  },

  /**
   * Mostra info
   */
  info(message, duration = 4000) {
    return this.show(message, this.TYPES.INFO, duration);
  },

  /**
   * Mostra aviso
   */
  warning(message, duration = 4000) {
    return this.show(message, this.TYPES.WARNING, duration);
  },

  /**
   * Mostra loading
   */
  loading(message = 'Carregando...') {
    this.init();

    const notif = document.createElement('div');
    notif.style.cssText = `
      background-color: #e2e3e5;
      border: 1px solid #d3d3d4;
      color: #383d41;
      padding: 12px 16px;
      margin-bottom: 10px;
      border-radius: 4px;
      display: flex;
      align-items: center;
      gap: 12px;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    `;

    notif.innerHTML = `
      <div style="
        display: inline-block;
        width: 16px;
        height: 16px;
        border: 2px solid #383d41;
        border-top-color: transparent;
        border-radius: 50%;
        animation: spin 0.8s linear infinite;
      "></div>
      <span>${message}</span>
    `;

    this.container.appendChild(notif);
    return notif;
  },

  /**
   * Mostra confirmação
   */
  confirm(message, onConfirm, onCancel) {
    this.init();

    const notif = document.createElement('div');
    notif.style.cssText = `
      background-color: #fff3cd;
      border: 1px solid #ffeeba;
      color: #856404;
      padding: 12px 16px;
      margin-bottom: 10px;
      border-radius: 4px;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    `;

    notif.innerHTML = `
      <div style="margin-bottom: 10px;">${message}</div>
      <div style="display: flex; gap: 8px;">
        <button id="confirm-btn" style="
          padding: 6px 12px;
          background-color: #856404;
          color: white;
          border: none;
          border-radius: 3px;
          cursor: pointer;
        ">Confirmar</button>
        <button id="cancel-btn" style="
          padding: 6px 12px;
          background-color: #ccc;
          border: none;
          border-radius: 3px;
          cursor: pointer;
        ">Cancelar</button>
      </div>
    `;

    this.container.appendChild(notif);

    notif.querySelector('#confirm-btn').addEventListener('click', () => {
      notif.remove();
      if (onConfirm) onConfirm();
    });

    notif.querySelector('#cancel-btn').addEventListener('click', () => {
      notif.remove();
      if (onCancel) onCancel();
    });

    return notif;
  },

  /**
   * Limpa todas as notificações
   */
  clearAll() {
    if (this.container) {
      this.container.innerHTML = '';
    }
  },
};

// Adiciona estilos de animação ao documento
const style = document.createElement('style');
style.textContent = `
  @keyframes slideIn {
    from {
      transform: translateX(400px);
      opacity: 0;
    }
    to {
      transform: translateX(0);
      opacity: 1;
    }
  }

  @keyframes slideOut {
    from {
      transform: translateX(0);
      opacity: 1;
    }
    to {
      transform: translateX(400px);
      opacity: 0;
    }
  }

  @keyframes spin {
    to {
      transform: rotate(360deg);
    }
  }
`;
document.head.appendChild(style);

// Export
window.notificationsService = notificationsService;
