(function () {
  if (window.AppConfig) return;

  function getApiUrl() {
    const stored = localStorage.getItem("wof_base_url");
    if (stored) return stored.replace(/\/+$/, "");

    return "https://wofertas-production.up.railway.app";
  }

  const API_URL = getApiUrl();

  function getEndpointUrl(endpoint) {
    if (/^https?:\/\//i.test(endpoint)) return endpoint;
    return `${API_URL}${endpoint.startsWith("/") ? endpoint : `/${endpoint}`}`;
  }

  function getErrorMessage(data) {
    if (data?.fieldErrors && typeof data.fieldErrors === "object") {
      return Object.values(data.fieldErrors).filter(Boolean).join("\n");
    }

    return data?.message ||
      data?.error ||
      (typeof data === "string" ? data : "Erro interno no servidor");
  }

  async function safeFetch(endpoint, options = {}) {
    try {
      const defaultOptions = {
        headers: {
          "Content-Type": "application/json",
          "Authorization": localStorage.getItem("token") ? `Bearer ${localStorage.getItem("token")}` : ""
        }
      };

      const finalOptions = {
        ...defaultOptions,
        ...options,
        headers: { ...defaultOptions.headers, ...(options.headers || {}) }
      };

      if (options.body instanceof FormData) {
        delete finalOptions.headers["Content-Type"];
      }

      if (!finalOptions.headers.Authorization) {
        delete finalOptions.headers.Authorization;
      }

      const response = await fetch(getEndpointUrl(endpoint), finalOptions);
      const text = await response.text();

      let data;
      try {
        data = text ? JSON.parse(text) : null;
      } catch {
        data = text;
      }

      if (!response.ok) {
        return {
          ok: false,
          status: response.status,
          data: null,
          error: getErrorMessage(data)
        };
      }

      return {
        ok: true,
        status: response.status,
        data,
        error: null
      };
    } catch (err) {
      console.error(`[Network Error] API indisponivel ou erro de CORS em: ${endpoint}`, err);
      return {
        ok: false,
        status: 0,
        data: null,
        error: "Falha de conexao com a nuvem. O servico pode estar indisponivel ou sem internet."
      };
    }
  }

  window.AppConfig = {
    API_URL,
    safeFetch,
    toast: (msg, type = "success") => {
      const container = document.getElementById("toastContainer");
      if (!container) return;

      const el = document.createElement("div");
      el.className = `toast toast-${type}`;
      el.textContent = msg;
      container.appendChild(el);
      setTimeout(() => el.remove(), 4000);
    }
  };
})();
