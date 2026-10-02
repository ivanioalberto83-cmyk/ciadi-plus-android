package com.example.data.remote.client

import com.example.core.config.SupabaseConfig
import com.example.core.session.SessionManager
import com.example.data.remote.api.SupabaseAuthApi
import com.example.data.remote.api.SupabaseRestApi
import com.example.data.remote.interceptor.SupabaseAuthInterceptor
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Fábrica e configurador de clientes Retrofit para o ecossistema Supabase CIADI+.
 *
 * Configura:
 * 1. Base URL com validação e formatação com barra final exigida pelo Retrofit.
 * 2. SupabaseAuthInterceptor para injeção automática de apikey e JWT do usuário para RLS.
 * 3. Conversor Moshi com suporte a reflexão Kotlin.
 * 4. Timeout robusto e interceptor de log para auditoria de tráfego.
 */
class SupabaseClientFactory(
    val sessionManager: SessionManager? = null,
    private val customBaseUrl: String? = null,
    private val customOkHttpClient: OkHttpClient? = null
) {

    private val normalizedBaseUrl: String
        get() {
            val url = (customBaseUrl ?: SupabaseConfig.supabaseUrl).trim()
            return if (url.endsWith("/")) url else "$url/"
        }

    val okHttpClient: OkHttpClient by lazy {
        customOkHttpClient ?: run {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(SupabaseAuthInterceptor(sessionManager))
                .addInterceptor(loggingInterceptor)
                .build()
        }
    }

    val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(normalizedBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    val authApi: SupabaseAuthApi by lazy {
        retrofit.create(SupabaseAuthApi::class.java)
    }

    val restApi: SupabaseRestApi by lazy {
        retrofit.create(SupabaseRestApi::class.java)
    }

    /**
     * Verifica a prontidão da infraestrutura para conexão ao Supabase com políticas RLS ativas.
     */
    fun isReadyForConnection(): Boolean {
        return SupabaseConfig.isConfigured
    }

    /**
     * Retorna a URL base normalizada configurada.
     */
    fun getBaseUrl(): String = normalizedBaseUrl
}
