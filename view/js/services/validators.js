/**
 * Form Validators Service
 * Validações reutilizáveis para formulários
 */

const validators = {
  /**
   * Valida email
   */
  email(valor) {
    const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return regex.test(valor);
  },

  /**
   * Valida senha (mínimo 8 caracteres, 1 maiúscula, 1 número, 1 caractere especial)
   */
  senha(valor) {
    const regex = /^(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/;
    return regex.test(valor);
  },

  /**
   * Valida telefone brasileiro
   */
  telefone(valor) {
    const regex = /^(\d{2})?\s?9?\d{4}-?\d{4}$/;
    return regex.test(valor);
  },

  /**
   * Valida CPF
   */
  cpf(valor) {
    valor = valor.replace(/\D/g, '');
    if (valor.length !== 11) return false;

    let sum = 0;
    let remainder;

    for (let i = 1; i <= 9; i++) {
      sum += parseInt(valor.substring(i - 1, i)) * (11 - i);
    }

    remainder = (sum * 10) % 11;
    if (remainder === 10 || remainder === 11) remainder = 0;
    if (remainder !== parseInt(valor.substring(9, 10))) return false;

    sum = 0;
    for (let i = 1; i <= 10; i++) {
      sum += parseInt(valor.substring(i - 1, i)) * (12 - i);
    }

    remainder = (sum * 10) % 11;
    if (remainder === 10 || remainder === 11) remainder = 0;
    if (remainder !== parseInt(valor.substring(10, 11))) return false;

    return true;
  },

  /**
   * Valida CNPJ
   */
  cnpj(valor) {
    valor = valor.replace(/\D/g, '');
    if (valor.length !== 14) return false;

    let size = valor.length - 2;
    let numbers = valor.substring(0, size);
    let digits = valor.substring(size);
    let sum = 0;
    let pos = size - 7;

    for (let i = size; i >= 1; i--) {
      sum += numbers.charAt(size - i) * pos--;
      if (pos < 2) pos = 9;
    }

    let result = sum % 11 < 2 ? 0 : 11 - (sum % 11);
    if (result !== parseInt(digits.charAt(0))) return false;

    size = size + 1;
    numbers = valor.substring(0, size);
    sum = 0;
    pos = size - 7;

    for (let i = size; i >= 1; i--) {
      sum += numbers.charAt(size - i) * pos--;
      if (pos < 2) pos = 9;
    }

    result = sum % 11 < 2 ? 0 : 11 - (sum % 11);
    if (result !== parseInt(digits.charAt(1))) return false;

    return true;
  },

  /**
   * Valida URL
   */
  url(valor) {
    try {
      new URL(valor);
      return true;
    } catch {
      return false;
    }
  },

  /**
   * Valida se está vazio
   */
  required(valor) {
    return valor != null && valor.toString().trim() !== '';
  },

  /**
   * Valida comprimento mínimo
   */
  minLength(valor, min) {
    return valor.toString().length >= min;
  },

  /**
   * Valida comprimento máximo
   */
  maxLength(valor, max) {
    return valor.toString().length <= max;
  },

  /**
   * Valida faixa de números
   */
  range(valor, min, max) {
    const num = parseFloat(valor);
    return num >= min && num <= max;
  },

  /**
   * Valida data (DD/MM/YYYY)
   */
  data(valor) {
    const regex = /^(\d{2})\/(\d{2})\/(\d{4})$/;
    if (!regex.test(valor)) return false;

    const [, dia, mes, ano] = valor.match(regex);
    const data = new Date(ano, mes - 1, dia);

    return data.getDate() === parseInt(dia) &&
           data.getMonth() === parseInt(mes) - 1 &&
           data.getFullYear() === parseInt(ano);
  },

  /**
   * Valida arquivo por tipo MIME
   */
  fileType(file, allowedTypes) {
    return allowedTypes.includes(file.type);
  },

  /**
   * Valida tamanho de arquivo (em MB)
   */
  fileSize(file, maxMB) {
    return file.size <= maxMB * 1024 * 1024;
  },
};

// Export
window.validators = validators;
