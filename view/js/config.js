/**
 * Configuração Global de Ambiente (Vercel + Koyeb)
 * Resolve automaticamente para onde o frontend deve apontar a API.
 */
const AppConfig = (() => {
  // Define a URL base da API
 const getApiUrl = () => {

  const stored = localStorage.getItem("wof_base_url");
  if (stored) return stored;

  return "https://wofertas-production.up.railway.app";
};

  const API_URL = getApiUrl();

  // Utilitário de fetch resiliente (Nunca crasha a tela, sempre retorna um formato previsível)
  const safeFetch = async (endpoint, options = {}) => {
    try {
      // Configurações padrão
      const defaultOptions = {
        headers: {
          "Content-Type": "application/json",
          "Authorization": localStorage.getItem("token") ? `Bearer ${localStorage.getItem("token")}` : ""
        }
      };

      // Mescla os headers e options
      const finalOptions = {
        ...defaultOptions,
        ...options,
        headers: { ...defaultOptions.headers, ...(options.headers || {}) }
      };

      // Se for FormData, remove o Content-Type para o browser gerar o boundary
      if (options.body instanceof FormData) {
        delete finalOptions.headers["Content-Type"];
      }

      // Se Authorization ficou vazio, remove
      if (!finalOptions.headers.Authorization) {
        delete finalOptions.headers.Authorization;
      }

      const response = await fetch(`${API_URL}${endpoint}`, finalOptions);

      let data;
      const text = await response.text();
      try { data = text ? JSON.parse(text) : null; } catch (e) { data = text; }

      if (!response.ok) {
        return {
          ok: false,
          status: response.status,
          data: null,
          error: data?.message || data?.error || (typeof data === 'string' ? data : "Erro interno no servidor")
        };
      }

      return {
        ok: true,
        status: response.status,
        data: data,
        error: null
      };

    } catch (err) {
      console.error(`[Network Error] API indisponível ou erro de CORS em: ${endpoint}`, err);
      return {
        ok: false,
        status: 0,
        data: null,
        error: "Falha de conexão com a nuvem. O serviço pode estar indisponível ou sem internet."
      };
    }
  };

  return {
    API_URL,
    safeFetch,
    toast: (msg, type = "success") => {
      const container = document.getElementById("toastContainer");
      if(!container) return;
      const el = document.createElement("div");
      el.className = `toast toast-${type}`;
      el.textContent = msg;
      container.appendChild(el);
      setTimeout(() => el.remove(), 4000);
    }
  };
})();

window.AppConfig = AppConfig;
