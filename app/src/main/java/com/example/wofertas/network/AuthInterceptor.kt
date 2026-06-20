package com.example.wofertas.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor legado mantido apenas para compatibilidade.
 *
 * O fluxo atual de autenticação já injeta o token diretamente em [ApiClient],
 * então este interceptor simplesmente encaminha a requisição sem modificações.
 */
class AuthInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        return chain.proceed(chain.request())
    }
}
