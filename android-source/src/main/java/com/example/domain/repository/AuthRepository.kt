package com.example.domain.repository

import com.example.domain.model.SupabaseStatus
import com.example.domain.model.UserProfile
import com.example.domain.model.UserRole
import com.example.domain.model.UserSession
import kotlinx.coroutines.flow.StateFlow

/**
 * Contrato de repositório de autenticação e sessão com o Supabase GoTrue.
 */
interface AuthRepository {
    val sessionFlow: StateFlow<UserSession>

    suspend fun signInWithEmail(email: String, password: String): Result<UserProfile>

    suspend fun signOut(): Result<Unit>

    suspend fun requestPasswordReset(email: String): Result<Unit>

    suspend fun refreshSession(): Result<UserProfile>

    fun getSupabaseStatus(): SupabaseStatus

    /**
     * Alterna o perfil ativo na sessão para demonstração/validação dos módulos e permissões RLS.
     */
    fun switchRoleForPreview(role: UserRole)
}
