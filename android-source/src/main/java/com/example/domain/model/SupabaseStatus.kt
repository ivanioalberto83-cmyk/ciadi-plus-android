package com.example.domain.model

/**
 * Estado da conexão com o backend Supabase do CIADI.
 */
data class SupabaseStatus(
    val url: String,
    val isConfigured: Boolean,
    val isConnected: Boolean,
    val rlsEnforced: Boolean = true,
    val statusMessage: String
)
