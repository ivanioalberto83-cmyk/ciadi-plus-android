package com.example.data.remote

import com.example.core.config.SupabaseConfig
import com.example.data.remote.dto.SupabaseAuthResponse
import com.example.data.remote.dto.SupabaseResetPasswordRequest
import com.example.data.remote.dto.SupabaseSignInRequest
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class SupabaseAuthClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val signInAdapter = moshi.adapter(SupabaseSignInRequest::class.java)
    private val resetAdapter = moshi.adapter(SupabaseResetPasswordRequest::class.java)
    private val responseAdapter = moshi.adapter(SupabaseAuthResponse::class.java)

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun signIn(email: String, pass: String): Result<SupabaseAuthResponse> = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured) {
            return@withContext Result.failure(
                IllegalStateException("SUPABASE_URL ou SUPABASE_ANON_KEY ainda não configurados no arquivo .env.")
            )
        }

        try {
            val bodyJson = signInAdapter.toJson(SupabaseSignInRequest(email = email, password = pass))
            val requestBuilder = Request.Builder()
                .url(SupabaseConfig.tokenUrl)
                .post(bodyJson.toRequestBody(jsonMediaType))

            SupabaseConfig.defaultHeaders().forEach { (k, v) ->
                requestBuilder.header(k, v)
            }

            val response = client.newCall(requestBuilder.build()).execute()
            val rawBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val parsed = responseAdapter.fromJson(rawBody)
                if (parsed != null && !parsed.accessToken.isNullOrBlank()) {
                    Result.success(parsed)
                } else {
                    Result.failure(Exception("Resposta inválida do Supabase: $rawBody"))
                }
            } else {
                val parsedError = try { responseAdapter.fromJson(rawBody) } catch (_: Exception) { null }
                val errorMsg = parsedError?.errorDescription ?: parsedError?.message ?: "Falha na autenticação (HTTP ${response.code})"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Não foi possível conectar ao servidor Supabase: ${e.localizedMessage}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recoverPassword(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured) {
            return@withContext Result.failure(
                IllegalStateException("SUPABASE_URL não configurada no .env.")
            )
        }

        try {
            val bodyJson = resetAdapter.toJson(SupabaseResetPasswordRequest(email = email))
            val requestBuilder = Request.Builder()
                .url(SupabaseConfig.recoverUrl)
                .post(bodyJson.toRequestBody(jsonMediaType))

            SupabaseConfig.defaultHeaders().forEach { (k, v) ->
                requestBuilder.header(k, v)
            }

            val response = client.newCall(requestBuilder.build()).execute()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Erro na solicitação de recuperação de senha (HTTP ${response.code})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
