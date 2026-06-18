const apiClient = (function () {

  function resolveServerOrigin() {
    const { protocol, hostname, port, origin } = window.location;

    if (protocol === 'file:') {
      return window.AppConfig?.API_URL || 'https://wofertas.koyeb.app';
    }

    if (port && port !== '8080') {
      return `${protocol}//${hostname}:8080`;
    }

    return origin;
  }

  const SERVER_ORIGIN = resolveServerOrigin();
  const API_BASE_URL = `${SERVER_ORIGIN}/api`;

  let _token = localStorage.getItem('token');

  function setToken(token) {
    _token = token;
    try {
      localStorage.setItem('token', token);
    } catch (e) {
      console.warn('Nao foi possivel salvar token no localStorage', e);
    }
  }

  function clearToken() {
    _token = null;
    try {
      localStorage.removeItem('token');
      localStorage.removeItem('tipo');
      localStorage.removeItem('authUser');
      localStorage.removeItem('mercado');
    } catch (e) {
      console.warn('Erro ao limpar localStorage', e);
    }
  }

  function getHeaders(includeJson = true) {
    const headers = {};

    if (includeJson) {
      headers['Content-Type'] = 'application/json';
    }

    if (_token) {
      headers.Authorization = `Bearer ${_token}`;
    }

    return headers;
  }

  function serializeBody(data) {
    if (typeof FormData !== 'undefined' && data instanceof FormData) {
      return data;
    }

    return JSON.stringify(data);
  }

  async function request(endpoint, options = {}) {
    const hasFormDataBody = typeof FormData !== 'undefined' && options.body instanceof FormData;

    const headers = {
      ...getHeaders(!hasFormDataBody),
      ...options.headers,
    };

    try {
      const response = await fetch(API_BASE_URL + endpoint, {
        ...options,
        headers,
      });

      const text = await response.text();
      let data = null;

      try {
        data = text ? JSON.parse(text) : null;
      } catch (e) {
        data = text;
      }

      return {
        data,
        status: response.status,
        error: !response.ok ? data?.message ?? data?.error ?? data ?? `Erro ${response.status}` : null,
      };

    } catch (error) {
      console.error('[apiClient] Erro de rede:', error);
      return {
        data: null,
        status: 0,
        error: error.message,
      };
    }
  }

  return {
    setToken,
    clearToken,
    getHeaders,
    getServerOrigin: () => SERVER_ORIGIN,
    getBaseUrl: () => API_BASE_URL,

    get:        (url)       => request(url),
    post:       (url, data) => request(url, { method: 'POST',   body: serializeBody(data) }),
    postPublic: (url, data) => request(url, { method: 'POST',   body: serializeBody(data) }),
    put:        (url, data) => request(url, { method: 'PUT',    body: serializeBody(data) }),
    patch:      (url, data) => request(url, { method: 'PATCH',  body: serializeBody(data) }),
    delete:     (url)       => request(url, { method: 'DELETE' }),
  };

})();
