package com.example.data.remote.interceptor

import com.example.core.config.SupabaseConfig
import com.example.core.session.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor do OkHttp responsável por configurar todos os cabeçalhos
 * de segurança obrigatórios para o Supabase e Row Level Security (RLS).
 *
 * Funcionamento com RLS:
 * 1. PostgREST valida a chave pública `apikey`.
 * 2. O cabeçalho `Authorization: Bearer <JWT>` transmite o token assinado da sessão GoTrue.
 *    No PostgreSQL, as funções `auth.uid()` e `auth.jwt()` extraem a identidade e as claims do usuário,
 *    permitindo que as políticas RLS filtrem automaticamente apenas os dados clínicos aos quais
 *    o usuário autenticado tem direito (ex: Família vê apenas seu assistido, A.T. vê seus pacientes).
 * 3. Se não houver token de usuário ativo, utiliza a anonKey como fallback para permitir
 *    regras públicas/anônimas autorizadas pelo RLS.
 */
class SupabaseAuthInterceptor(
    private val sessionManager: SessionManager? = null
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        val publishableKey = SupabaseConfig.supabasePublishableKey
        val path = originalRequest.url.encodedPath

        // 1. Cabeçalho 'apikey' obrigatório com a publishable key do Supabase em todas as requisições
        if (publishableKey.isNotBlank()) {
            requestBuilder.header("apikey", publishableKey)
        }

        // 2. Cabeçalho 'Authorization'
        // Durante endpoints de autenticação de entrada (ex: token com password ou refresh_token, recover),
        // NÃO enviar Authorization: Bearer <publishable_key>! Apenas o header apikey deve ser enviado.
        val isAuthTokenEndpoint = path.contains("auth/v1/token") || path.contains("auth/v1/recover")
        if (!isAuthTokenEndpoint) {
            val userToken = sessionManager?.getCurrentAccessToken()
            // Envia o cabeçalho Authorization APENAS se houver um access_token real de usuário autenticado
            // NUNCA enviar a publishable_key como access token!
            if (!userToken.isNullOrBlank() && originalRequest.header("Authorization") == null) {
                requestBuilder.header("Authorization", "Bearer $userToken")
            }
        }

        // 3. Cabeçalhos de tipo de conteúdo e retorno PostgREST
        if (originalRequest.header("Content-Type") == null) {
            requestBuilder.header("Content-Type", "application/json")
        }
        if (originalRequest.header("Accept") == null) {
            requestBuilder.header("Accept", "application/json")
        }

        // Header padrão para PostgREST retornar o registro inserido/atualizado
        if (path.contains("rest/v1/") && (originalRequest.method == "POST" || originalRequest.method == "PATCH")) {
            if (originalRequest.header("Prefer") == null) {
                requestBuilder.header("Prefer", "return=representation")
            }
        }

        return chain.proceed(requestBuilder.build())
    }
}
