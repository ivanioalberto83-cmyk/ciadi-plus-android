package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SupabaseSignInRequest(
    @param:Json(name = "email") val email: String,
    @param:Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class SupabaseResetPasswordRequest(
    @param:Json(name = "email") val email: String
)

@JsonClass(generateAdapter = true)
data class SupabaseRefreshTokenRequest(
    @param:Json(name = "refresh_token") val refreshToken: String
)

@JsonClass(generateAdapter = true)
data class SupabaseUserMetadata(
    @param:Json(name = "full_name") val fullName: String? = null,
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "role") val role: String? = null,
    @param:Json(name = "specialty") val specialty: String? = null,
    @param:Json(name = "unit_name") val unitName: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseUserDto(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "email") val email: String? = null,
    @param:Json(name = "role") val role: String? = null,
    @param:Json(name = "user_metadata") val userMetadata: SupabaseUserMetadata? = null,
    @param:Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseAuthResponse(
    @param:Json(name = "access_token") val accessToken: String? = null,
    @param:Json(name = "token_type") val tokenType: String? = null,
    @param:Json(name = "expires_in") val expiresIn: Long? = null,
    @param:Json(name = "expires_at") val expiresAt: Long? = null,
    @param:Json(name = "refresh_token") val refreshToken: String? = null,
    @param:Json(name = "user") val user: SupabaseUserDto? = null,
    @param:Json(name = "error") val error: String? = null,
    @param:Json(name = "error_description") val errorDescription: String? = null,
    @param:Json(name = "error_code") val errorCode: String? = null,
    @param:Json(name = "msg") val msg: String? = null,
    @param:Json(name = "message") val message: String? = null,
    @param:Json(name = "code") val code: Int? = null
)
