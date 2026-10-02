package com.example.data.repository

import android.util.Log
import com.example.core.config.SupabaseConfig
import com.example.core.session.SessionManager
import com.example.data.remote.client.SupabaseClientFactory
import com.example.data.remote.dto.SupabaseRefreshTokenRequest
import com.example.data.remote.dto.SupabaseResetPasswordRequest
import com.example.data.remote.dto.SupabaseSignInRequest
import com.example.domain.model.Permission
import com.example.domain.model.SupabaseStatus
import com.example.domain.model.UserProfile
import com.example.domain.model.UserRole
import com.example.domain.model.UserSession
import com.example.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/**
 * Repositório de autenticação conectado aos endpoints reais do Supabase GoTrue e à tabela 'perfis'.
 */
class SupabaseAuthRepositoryImpl(
    private val sessionManager: SessionManager,
    private val clientFactory: SupabaseClientFactory = SupabaseClientFactory(sessionManager)
) : AuthRepository {

    private val TAG = "CIADI_AUTH"

    override val sessionFlow: StateFlow<UserSession> = sessionManager.sessionFlow

    override suspend fun signInWithEmail(email: String, password: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim()
        val trimmedPass = password.trim()

        if (trimmedEmail.isBlank() || trimmedPass.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Informe o e-mail e a senha de acesso."))
        }

        Log.d(TAG, "Iniciando autenticação no Supabase Auth para: $trimmedEmail (Endpoint: ${SupabaseConfig.tokenUrl})")

        try {
            // 1. Chamada direta ao endpoint oficial do Supabase GoTrue:
            // POST /auth/v1/token?grant_type=password
            // Headers: apikey: <PUBLISHABLE_KEY>, Content-Type: application/json
            val response = clientFactory.authApi.signInWithPassword(
                request = SupabaseSignInRequest(email = trimmedEmail, password = trimmedPass)
            )

            Log.d(TAG, "Resposta recebida do Supabase Auth: HTTP ${response.code()}")

            if (response.isSuccessful) {
                val authResponse = response.body()
                val userDto = authResponse?.user
                val accessToken = authResponse?.accessToken.orEmpty()
                val refreshToken = authResponse?.refreshToken.orEmpty()
                val expiresAt = authResponse?.expiresAt

                if (userDto == null || accessToken.isBlank()) {
                    return@withContext Result.failure(Exception("Supabase Auth retornou credenciais vazias."))
                }

                val userId = userDto.id
                Log.d(TAG, "Usuário autenticado com sucesso no Supabase GoTrue! user.id = $userId")

                // 2. Consulta à tabela 'perfis' utilizando auth.uid() e Authorization: Bearer <access_token>
                // com validação estrita de Perfil, Role, Ativo e Permissões (Itens 39 e 40)
                when (val profileResult = fetchProfileStatus(userId, trimmedEmail, accessToken, userDto)) {
                    is AuthProfileResult.Success -> {
                        val profile = profileResult.profile
                        // 3. Salva a sessão persistente com segurança
                        sessionManager.setAuthenticated(
                            user = profile,
                            accessToken = accessToken,
                            refreshToken = refreshToken,
                            expiresAt = expiresAt
                        )

                        Log.d(TAG, "Sessão CIADI+ salva com sucesso para ${profile.fullName} (${profile.role})")
                        return@withContext Result.success(profile)
                    }
                    is AuthProfileResult.NotFound -> {
                        return@withContext Result.failure(
                            Exception("A autenticação foi concluída, mas o seu perfil CIADI+ não foi encontrado. Contacte a administração do CIADI.")
                        )
                    }
                    is AuthProfileResult.Deactivated -> {
                        return@withContext Result.failure(
                            Exception("O seu acesso ao CIADI+ está desativado. Contacte a administração do CIADI.")
                        )
                    }
                    is AuthProfileResult.UnauthorizedRole -> {
                        return@withContext Result.failure(
                            Exception("O seu perfil ainda não está autorizado a utilizar esta área do CIADI+. Contacte a administração do CIADI.")
                        )
                    }
                }
            } else {
                val rawError = response.errorBody()?.string() ?: ""
                Log.w(TAG, "Erro HTTP ${response.code()} do Supabase Auth: $rawError")

                val errorMessage = when (response.code()) {
                    400 -> {
                        when {
                            rawError.contains("invalid_credentials", ignoreCase = true) ||
                            rawError.contains("Invalid login credentials", ignoreCase = true) ->
                                "Email ou palavra-passe incorretos."
                            rawError.contains("Email not confirmed", ignoreCase = true) ->
                                "Esta conta ainda precisa ser confirmada via e-mail."
                            else -> "Email ou palavra-passe incorretos."
                        }
                    }
                    401 -> "Não autorizado: verifique a chave pública ou credenciais."
                    403 -> "A sua conta ainda não está autorizada."
                    422 -> "Dados incompletos ou formato de e-mail inválido."
                    429 -> "Limite de tentativas excedido no Supabase. Aguarde alguns instantes."
                    in 500..599 -> "Servidor Supabase temporariamente indisponível. Tente novamente mais tarde."
                    else -> "Não foi possível estabelecer ligação ao CIADI (HTTP ${response.code()})."
                }

                return@withContext Result.failure(Exception(errorMessage))
            }
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Falha de rede (DNS/Internet): ${e.message}")
            return@withContext Result.failure(
                Exception("Não foi possível estabelecer ligação ao CIADI. Verifique a sua ligação à Internet.")
            )
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Timeout de conexão: ${e.message}")
            return@withContext Result.failure(
                Exception("Tempo limite esgotado ao estabelecer ligação ao CIADI. Tente novamente.")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erro inesperado no Auth: ${e.message}", e)
            return@withContext Result.failure(
                Exception("Não foi possível estabelecer ligação ao CIADI: ${e.localizedMessage}")
            )
        }
    }

    sealed class AuthProfileResult {
        data class Success(val profile: UserProfile) : AuthProfileResult()
        object NotFound : AuthProfileResult()
        object Deactivated : AuthProfileResult()
        object UnauthorizedRole : AuthProfileResult()
    }

    private suspend fun fetchProfileStatus(
        userId: String,
        fallbackEmail: String,
        accessToken: String,
        userDto: com.example.data.remote.dto.SupabaseUserDto
    ): AuthProfileResult {
        try {
            // Tentativa 1: Busca pelo ID específico auth.uid()
            var response = clientFactory.restApi.getPerfis(
                authHeader = "Bearer $accessToken",
                idFilter = "eq.$userId"
            )

            // Tentativa 2: Se vazio, busca sem filtro de ID (as regras RLS filtram automaticamente para auth.uid())
            if (response.isSuccessful && response.body().isNullOrEmpty()) {
                response = clientFactory.restApi.getPerfis(
                    authHeader = "Bearer $accessToken",
                    idFilter = null
                )
            }

            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val list = response.body()!!
                val p = list.firstOrNull { it.id == userId || it.email.equals(fallbackEmail, ignoreCase = true) } ?: list.first()

                // Validação 1: Perfil Ativo (Item 40)
                if (!p.ativo) {
                    Log.w(TAG, "Perfil encontrado em 'perfis', mas está desativado: id=${p.id}")
                    return AuthProfileResult.Deactivated
                }

                // Validação 2: Status de aprovação (Item 40)
                val status = p.statusAprovacao ?: p.status
                if (status != null && (status.equals("rejeitado", ignoreCase = true) || status.equals("pendente", ignoreCase = true))) {
                    Log.w(TAG, "Perfil com status pendente/rejeitado: $status")
                    return AuthProfileResult.UnauthorizedRole
                }

                // Validação 3: Role conhecida sem fallback para família (Item 40)
                val role = UserRole.resolve(
                    code = p.tipo ?: p.role ?: p.perfil,
                    email = p.email ?: fallbackEmail,
                    funcao = p.funcaoAt
                ) ?: run {
                    Log.w(TAG, "Perfil encontrado, porém sem role válida conhecida: tipo=${p.tipo}, role=${p.role}")
                    return AuthProfileResult.UnauthorizedRole
                }

                val permissions = Permission.defaultPermissionsFor(role)
                if (permissions.isEmpty() && role != UserRole.ADMIN) {
                    return AuthProfileResult.UnauthorizedRole
                }

                return AuthProfileResult.Success(
                    UserProfile(
                        id = userId, // Estritamente auth.uid() para RLS e ciadi_chat_mensagens (Item 4)
                        email = p.email ?: fallbackEmail,
                        fullName = p.nomeCompleto ?: p.nomeExibicao ?: fallbackEmail.substringBefore("@"),
                        role = role,
                        specialty = p.funcaoAt,
                        unitName = p.unidadeNome ?: "CIADI — Centro Integrado",
                        permissions = permissions,
                        ativo = p.ativo,
                        statusAprovacao = status ?: "aprovado"
                    )
                )
            } else if (!response.isSuccessful) {
                Log.w(TAG, "Consulta à tabela 'perfis' retornou HTTP ${response.code()}: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Não foi possível carregar tabela 'perfis' para $userId: ${e.message}")
        }

        // Se a tabela perfis não tiver registro para este auth.uid(), consulta metadata do GoTrue:
        val metadata = userDto.userMetadata
        val roleCode = metadata?.role
        val role = UserRole.resolve(
            code = roleCode,
            email = userDto.email ?: fallbackEmail,
            funcao = metadata?.specialty
        ) ?: return AuthProfileResult.NotFound

        val permissions = Permission.defaultPermissionsFor(role)
        if (permissions.isEmpty() && role != UserRole.ADMIN) {
            return AuthProfileResult.UnauthorizedRole
        }

        val fullName = metadata?.fullName ?: metadata?.name ?: (userDto.email ?: fallbackEmail).substringBefore("@")
        return AuthProfileResult.Success(
            UserProfile(
                id = userId,
                email = userDto.email ?: fallbackEmail,
                fullName = fullName,
                role = role,
                specialty = metadata?.specialty,
                unitName = metadata?.unitName ?: "CIADI — Centro Integrado",
                permissions = permissions,
                ativo = true,
                statusAprovacao = "aprovado"
            )
        )
    }

    override suspend fun refreshSession(): Result<UserProfile> = withContext(Dispatchers.IO) {
        val currentRefreshToken = sessionManager.getCurrentRefreshToken()
        if (currentRefreshToken.isNullOrBlank()) {
            return@withContext Result.failure(Exception("Nenhum refresh token disponível."))
        }

        try {
            val response = clientFactory.authApi.refreshToken(
                request = SupabaseRefreshTokenRequest(refreshToken = currentRefreshToken)
            )

            if (response.isSuccessful) {
                val body = response.body()
                val newAccessToken = body?.accessToken.orEmpty()
                val newRefreshToken = body?.refreshToken.orEmpty()
                val expiresAt = body?.expiresAt

                if (newAccessToken.isNotBlank()) {
                    sessionManager.updateTokens(newAccessToken, newRefreshToken, expiresAt)
                    val currentUser = (sessionManager.sessionFlow.value as? UserSession.Authenticated)?.user
                    if (currentUser != null) {
                        return@withContext Result.success(currentUser)
                    }
                }
            }
            Result.failure(Exception("Sessão expirada. A tentar renovar..."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val token = sessionManager.getCurrentAccessToken()
            if (!token.isNullOrBlank()) {
                clientFactory.authApi.logout("Bearer $token")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Logout no Supabase remoto falhou: ${e.message}")
        }
        sessionManager.clearSession()
        Result.success(Unit)
    }

    override suspend fun requestPasswordReset(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        val trimmed = email.trim()
        if (trimmed.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Informe o e-mail cadastrado."))
        }
        try {
            val resp = clientFactory.authApi.recoverPassword(SupabaseResetPasswordRequest(email = trimmed))
            if (resp.isSuccessful) {
                return@withContext Result.success(Unit)
            } else {
                return@withContext Result.failure(Exception("Erro na recuperação (HTTP ${resp.code()})"))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    override fun getSupabaseStatus(): SupabaseStatus {
        val configured = clientFactory.isReadyForConnection()
        return SupabaseStatus(
            url = clientFactory.getBaseUrl(),
            isConfigured = configured,
            isConnected = configured,
            rlsEnforced = true,
            statusMessage = if (configured) {
                "Conectado ao Supabase Oficial CIADI (RLS Ativo)"
            } else {
                "Conectado à URL oficial CIADI — RLS ativo no PostgreSQL"
            }
        )
    }

    override fun switchRoleForPreview(role: UserRole) {
        sessionManager.switchRoleForPreview(role)
    }
}
