package com.example.wofertas

import java.io.Serializable

/**
 * Modelo de Usuário para integração com MongoDB via API.
 * As propriedades são mutáveis (var) e opcionais (?) para facilitar
 * o mapeamento do JSON vindo do backend.
 */
class Usuario : Serializable {
    var id: String? = null
    var nome: String? = null
    var nomeLoja: String? = null
    var email: String? = null
    var perfil: String? = null // "cliente" ou "supermercado"
    var telefone: String? = null
    var cnpj: String? = null
    var endereco: String? = null
    var latitude: Double? = null
    var longitude: Double? = null
    var urlLogo: String? = null

    // Inicializado como uma lista vazia mutável para evitar o aviso "Condition is always false"
    // e garantir que o Adapter nunca receba um valor nulo.
    var supermercadosSalvos: List<String> = mutableListOf()

    // Construtor vazio para o serializador (Gson/Jackson)
    constructor()

    // Construtor de conveniência
    constructor(id: String?, email: String?) {
        this.id = id
        this.email = email
    }
}
