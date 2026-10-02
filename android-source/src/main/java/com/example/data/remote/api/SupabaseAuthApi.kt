package com.example.data.remote.api

import com.example.data.remote.dto.SupabaseAuthResponse
import com.example.data.remote.dto.SupabaseRefreshTokenRequest
import com.example.data.remote.dto.SupabaseResetPasswordRequest
import com.example.data.remote.dto.SupabaseSignInRequest
import com.example.data.remote.dto.SupabaseUserDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Interface Retrofit para a API de Autenticação Supabase (GoTrue).
 *
 * O GoTrue gerencia as sessões com JSON Web Tokens (JWT).
 * Os tokens gerados aqui são utilizados pelo SupabaseAuthInterceptor
 * nas requisições ao PostgREST para fazer cumprir as políticas de Row Level Security (RLS).
 */
interface SupabaseAuthApi {

    /**
     * Autenticação via e-mail e senha.
     * Endpoint: /auth/v1/token?grant_type=password
     */
    @POST("auth/v1/token")
    suspend fun signInWithPassword(
        @Query("grant_type") grantType: String = "password",
        @Body request: SupabaseSignInRequest
    ): Response<SupabaseAuthResponse>

    /**
     * Renovação de sessão via refresh token.
     * Endpoint: /auth/v1/token?grant_type=refresh_token
     */
    @POST("auth/v1/token")
    suspend fun refreshToken(
        @Query("grant_type") grantType: String = "refresh_token",
        @Body request: SupabaseRefreshTokenRequest
    ): Response<SupabaseAuthResponse>

    /**
     * Solicitação de recuperação de senha por e-mail.
     * Endpoint: /auth/v1/recover
     */
    @POST("auth/v1/recover")
    suspend fun recoverPassword(
        @Body request: SupabaseResetPasswordRequest
    ): Response<Unit>

    /**
     * Consulta aos dados do usuário autenticado no GoTrue.
     * Endpoint: /auth/v1/user
     */
    @GET("auth/v1/user")
    suspend fun getCurrentUser(
        @Header("Authorization") bearerToken: String? = null
    ): Response<SupabaseUserDto>

    /**
     * Encerramento da sessão no GoTrue.
     * Endpoint: /auth/v1/logout
     */
    @POST("auth/v1/logout")
    suspend fun logout(
        @Header("Authorization") bearerToken: String? = null
    ): Response<Unit>
}
