package com.example.core.network

import retrofit2.Response

/**
 * Tratamento centralizado de erros HTTP do Supabase PostgREST e GoTrue.
 * Fornece mensagens amigáveis em português e sem jargões técnicos para a UI do CIADI+.
 */
object SupabaseErrorHandler {

    fun parseHttpErrorMessage(statusCode: Int, rawBody: String? = null): String {
        return when (statusCode) {
            401 -> "Sua sessão expirou ou credenciais são inválidas. Por favor, autentique-se novamente."
            403 -> "Você não tem autorização para consultar este conteúdo (política de segurança RLS do CIADI)."
            404 -> "O recurso solicitado não foi encontrado no CIADI."
            409 -> "Conflito nos dados: este registro ou horário já existe ou foi modificado."
            422 -> "Dados incompletos ou inválidos para submissão. Verifique os campos preenchidos."
            429 -> "Muitas solicitações ao servidor. Aguarde alguns instantes e tente novamente."
            in 500..599 -> "Instabilidade temporária nos servidores do CIADI. Tente novamente em alguns minutos."
            else -> "Ocorreu uma falha ao comunicar com o servidor (Código $statusCode)."
        }
    }

    fun <T> handleResponseError(response: Response<T>): Exception {
        val errorText = parseHttpErrorMessage(response.code(), response.errorBody()?.string())
        return SupabaseApiException(response.code(), errorText)
    }
}

class SupabaseApiException(
    val statusCode: Int,
    override val message: String
) : Exception(message)
