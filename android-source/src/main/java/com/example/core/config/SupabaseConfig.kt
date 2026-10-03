package com.example.core.config

/**
 * Configuração central e oficial do Supabase para o ecossistema digital CIADI+.
 *
 * Utiliza o projeto oficial Supabase (GoTrue para Auth e PostgreSQL com Row-Level Security - RLS).
 * URL e chaves unificadas e normalizadas.
 */
object SupabaseConfig {

    const val SUPABASE_URL = "https://egpkkttbcaukqnnxyhjv.supabase.co"
    const val SUPABASE_PROJECT_ID = "egpkkttbcaukqnnxyhjv"
    const val SUPABASE_PUBLISHABLE_KEY = "sb_publishable_SSA6B6gfnaUxKhOzReE6HQ_utqQRebz"

    const val OFFICIAL_CIADI_SUPABASE_URL = SUPABASE_URL
    const val OFFICIAL_CIADI_PUBLISHABLE_KEY = SUPABASE_PUBLISHABLE_KEY
    const val OFFICIAL_CIADI_ANON_KEY = SUPABASE_PUBLISHABLE_KEY

    /**
     * Normaliza qualquer URL de entrada para evitar prefixos duplicados (ex: https://https://)
     * e garantir formatação canônica sem barra final.
     */
    fun normalizeUrl(rawUrl: String?): String {
        if (rawUrl.isNullOrBlank() || rawUrl.contains("placeholder", ignoreCase = true)) {
            return SUPABASE_URL
        }
        var url = rawUrl.trim()

        // Remove prefixos duplicados como https://https://, http://https:// ou variações
        while (url.contains("://https://") || url.contains("://http://")) {
            url = url.substring(url.indexOf("://") + 3)
        }
        if (url.startsWith("httpshttps://")) {
            url = "https://" + url.removePrefix("httpshttps://")
        } else if (url.startsWith("httphttp://")) {
            url = "http://" + url.removePrefix("httphttp://")
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }
        return url.removeSuffix("/")
    }

    val supabaseUrl: String
        get() = SUPABASE_URL

    val supabasePublishableKey: String
        get() = SUPABASE_PUBLISHABLE_KEY

    val supabaseAnonKey: String
        get() = supabasePublishableKey

    val isConfigured: Boolean
        get() = supabaseUrl.isNotBlank() &&
                supabasePublishableKey.isNotBlank() &&
                !supabasePublishableKey.contains("placeholder")

    // Endpoints padrão do Supabase GoTrue (Auth)
    val authEndpoint: String
        get() = "$supabaseUrl/auth/v1"

    val tokenUrl: String
        get() = "$authEndpoint/token?grant_type=password"

    val refreshTokenUrl: String
        get() = "$authEndpoint/token?grant_type=refresh_token"

    val recoverUrl: String
        get() = "$authEndpoint/recover"

    val userUrl: String
        get() = "$authEndpoint/user"

    val logoutUrl: String
        get() = "$authEndpoint/logout"

    // Endpoints padrão do Supabase PostgREST
    val restEndpoint: String
        get() = "$supabaseUrl/rest/v1"

    /**
     * Diagnóstico seguro sem expor tokens ou dados sensíveis.
     */
    fun getSafeDiagnostics(): Map<String, String> {
        val key = supabasePublishableKey
        val keyPrefix = if (key.length >= 15) key.take(15) + "..." else "definida"
        return mapOf(
            "url_valida" to "SIM",
            "url" to supabaseUrl,
            "publishable_key_presente" to if (key.isNotBlank()) "SIM" else "NAO",
            "tamanho_key" to "${key.length} caracteres",
            "prefixo_key" to keyPrefix
        )
    }

    /**
     * Tabelas oficiais do banco de dados PostgreSQL do CIADI no Supabase.
     * Mapeamento real sem nomenclaturas genéricas ou paralelas.
     */
    object Tables {
        const val PERFIS = "perfis"
        const val PACIENTES = "pacientes"
        const val VINCULOS_FAMILIARES = "ciadi_vinculos_familiares"
        const val ATRIBUICOES_AT = "ciadi_atribuicoes_at"
        const val AT_SESSOES = "ciadi_at_sessoes"
        const val AT_REGISTOS_OBJETIVOS = "ciadi_at_registos_objetivos"
        const val AT_INCIDENTES = "ciadi_at_incidentes"
        const val AT_SUPERVISOES = "ciadi_at_supervisoes"
        const val PROFISSIONAIS = "profissionais"
        const val AGENDAMENTOS = "agendamentos"
        const val ESPECIALIDADES = "especialidades"
        const val FORMULARIOS_CLINICOS = "ciadi_formularios_clinicos"
        const val FORMULARIOS_ESPECIALIDADES = "ciadi_formularios_especialidades"
        const val DOCUMENTOS_CLINICOS = "documentos_clinicos"
        const val DOCUMENTOS_HISTORICO = "ciadi_documentos_clinicos_historico"
        const val MODELOS_DOCUMENTOS = "ciadi_modelos_documentos"
        const val MODELOS_ESPECIALIDADES = "ciadi_modelos_documentos_especialidades"
        const val CHAT_GRUPOS = "ciadi_chat_grupos"
        const val CHAT_MEMBROS = "ciadi_chat_membros"
        const val CHAT_MENSAGENS = "ciadi_chat_mensagens"
        const val NOTIFICACOES = "notificacoes"
        const val BASE_CONHECIMENTO = "ciadi_base_conhecimento"
        const val AT_ATIVIDADES = "ciadi_at_atividades"
        const val ACOMPANHAMENTOS_DIARIOS_ABA = "ciadi_acompanhamentos_diarios_aba"
        const val V_AT_CALENDARIO = "v_ciadi_at_calendario"
        const val V_AT_ATIVIDADES = "v_ciadi_at_atividades"
        const val V_AT_CONTACTOS = "v_ciadi_at_contactos"
        const val V_AT_ORGANIZACAO = "v_ciadi_at_organizacao"
        const val RPC_CHAT_ABRIR_PRIVADO = "ciadi_chat_abrir_privado"
    }

    /**
     * Cabeçalhos HTTP padrão exigidos pelo Supabase
     */
    fun defaultHeaders(token: String? = null): Map<String, String> {
        val headers = mutableMapOf(
            "apikey" to supabaseAnonKey,
            "Content-Type" to "application/json",
            "Accept" to "application/json"
        )
        if (!token.isNullOrBlank()) {
            headers["Authorization"] = "Bearer $token"
        }
        return headers
    }
}
