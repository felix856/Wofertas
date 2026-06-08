/**
 * Formatters Service
 * Formatação de dados para exibição
 */

const formatters = {
  /**
   * Formata moeda (R$)
   */
  moeda(valor) {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    }).format(valor);
  },

  /**
   * Formata porcentagem
   */
  percentual(valor, casas = 2) {
    return (valor * 100).toFixed(casas) + '%';
  },

  /**
   * Formata data
   */
  data(valor, formato = 'DD/MM/YYYY') {
    const date = new Date(valor);
    const dia = String(date.getDate()).padStart(2, '0');
    const mes = String(date.getMonth() + 1).padStart(2, '0');
    const ano = date.getFullYear();
    const horas = String(date.getHours()).padStart(2, '0');
    const minutos = String(date.getMinutes()).padStart(2, '0');
    const segundos = String(date.getSeconds()).padStart(2, '0');

    const replaces = {
      'DD': dia,
      'MM': mes,
      'YYYY': ano,
      'HH': horas,
      'mm': minutos,
      'ss': segundos,
    };

    let resultado = formato;
    Object.entries(replaces).forEach(([key, val]) => {
      resultado = resultado.replace(key, val);
    });

    return resultado;
  },

  /**
   * Formata número com separadores
   */
  numero(valor, casas = 2) {
    return new Intl.NumberFormat('pt-BR', {
      minimumFractionDigits: casas,
      maximumFractionDigits: casas,
    }).format(valor);
  },

  /**
   * Formata telefone
   */
  telefone(valor) {
    valor = valor.replace(/\D/g, '');
    if (valor.length === 11) {
      return `(${valor.substring(0, 2)}) ${valor.substring(2, 7)}-${valor.substring(7)}`;
    } else if (valor.length === 10) {
      return `(${valor.substring(0, 2)}) ${valor.substring(2, 6)}-${valor.substring(6)}`;
    }
    return valor;
  },

  /**
   * Formata CPF
   */
  cpf(valor) {
    valor = valor.replace(/\D/g, '');
    return valor.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, '$1.$2.$3-$4');
  },

  /**
   * Formata CNPJ
   */
  cnpj(valor) {
    valor = valor.replace(/\D/g, '');
    return valor.replace(/(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})/, '$1.$2.$3/$4-$5');
  },

  /**
   * Formata CEP
   */
  cep(valor) {
    valor = valor.replace(/\D/g, '');
    return valor.replace(/(\d{5})(\d{3})/, '$1-$2');
  },

  /**
   * Trunca texto com reticências
   */
  truncar(texto, limite = 50) {
    if (texto.length <= limite) return texto;
    return texto.substring(0, limite) + '...';
  },

  /**
   * Formata tamanho de arquivo
   */
  tamanhoArquivo(bytes) {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const tamanhos = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + tamanhos[i];
  },

  /**
   * Formata tempo (horas:minutos:segundos)
   */
  tempo(segundos) {
    const horas = Math.floor(segundos / 3600);
    const minutos = Math.floor((segundos % 3600) / 60);
    const segs = segundos % 60;

    const partes = [];
    if (horas > 0) partes.push(`${horas}h`);
    if (minutos > 0) partes.push(`${minutos}m`);
    if (segs > 0 || partes.length === 0) partes.push(`${segs}s`);

    return partes.join(' ');
  },

  /**
   * Capitaliza primeira letra
   */
  capitalize(texto) {
    return texto.charAt(0).toUpperCase() + texto.slice(1).toLowerCase();
  },

  /**
   * Converte para slug (URL-friendly)
   */
  slug(texto) {
    return texto
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[^\w\s-]/g, '')
      .replace(/[\s_-]+/g, '-')
      .replace(/^-+|-+$/g, '');
  },

  /**
   * Formata tempo relativo (ex: "há 2 horas")
   */
  tempoRelativo(data) {
    const agora = new Date();
    const diferenca = agora - new Date(data);
    const segundos = Math.floor(diferenca / 1000);
    const minutos = Math.floor(segundos / 60);
    const horas = Math.floor(minutos / 60);
    const dias = Math.floor(horas / 24);

    if (segundos < 60) return 'agora mesmo';
    if (minutos < 60) return `há ${minutos} minuto${minutos > 1 ? 's' : ''}`;
    if (horas < 24) return `há ${horas} hora${horas > 1 ? 's' : ''}`;
    if (dias < 30) return `há ${dias} dia${dias > 1 ? 's' : ''}`;

    return this.data(data);
  },
};

// Export
window.formatters = formatters;
