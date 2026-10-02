package com.example.domain.model

/**
 * Perfil do usuário autenticado no Supabase do CIADI+.
 *
 * Mapeado para a tabela 'perfis' e 'auth.users' do Supabase.
 */
data class UserProfile(
    val id: String,
    val email: String,
    val fullName: String,
    val role: UserRole,
    val specialty: String? = null,
    val activePatientId: String? = null,
    val activePatientName: String? = null,
    val telefone: String? = null,
    val avatarUrl: String? = null,
    val unitName: String = "CIADI — Centro Integrado",
    val permissions: Set<Permission> = Permission.defaultPermissionsFor(role),
    val ativo: Boolean = true,
    val statusAprovacao: String = "aprovado" // "pendente", "aprovado", "rejeitado"
) {
    fun hasPermission(permission: Permission): Boolean {
        return permissions.contains(permission) || role == UserRole.ADMIN
    }

    val isAuthorized: Boolean
        get() = ativo && statusAprovacao.equals("aprovado", ignoreCase = true)

    val isFamily: Boolean get() = role == UserRole.RESPONSAVEL
    val isAT: Boolean get() = role == UserRole.AT
    val isProfessional: Boolean get() = role == UserRole.PROFISSIONAL || role == UserRole.GESTOR
    val isAdmin: Boolean get() = role == UserRole.ADMIN || role == UserRole.GESTOR
    val isSecretary: Boolean get() = role == UserRole.OPERADOR
}
