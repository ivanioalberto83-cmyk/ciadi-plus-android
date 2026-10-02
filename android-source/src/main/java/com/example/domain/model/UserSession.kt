package com.example.domain.model

/**
 * Estado da sessão de autenticação do CIADI+.
 */
sealed interface UserSession {
    data object Initializing : UserSession
    data object Unauthenticated : UserSession
    data object Authenticating : UserSession
    data class Authenticated(
        val user: UserProfile,
        val accessToken: String,
        val refreshToken: String,
        val expiresAt: Long? = null
    ) : UserSession
    data class Error(val message: String) : UserSession
}
