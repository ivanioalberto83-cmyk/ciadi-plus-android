package com.example

import com.example.core.config.SupabaseConfig
import com.example.core.session.SessionManager
import com.example.data.remote.dto.PerfilDto
import com.example.data.remote.dto.SupabaseAuthResponse
import com.example.data.remote.dto.SupabaseSignInRequest
import com.example.data.remote.dto.SupabaseUserDto
import com.example.data.remote.dto.SupabaseUserMetadata
import com.example.data.remote.interceptor.SupabaseAuthInterceptor
import com.example.domain.model.UserProfile
import com.example.domain.model.UserRole
import com.example.domain.model.UserSession
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class SupabaseAuthIntegrationTest {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    // Teste 1: URL Supabase correta
    @Test
    fun test01_SupabaseUrlCorreta() {
        assertEquals("https://egpkkttbcaukqnnxyhjv.supabase.co", SupabaseConfig.SUPABASE_URL)
        assertEquals("https://egpkkttbcaukqnnxyhjv.supabase.co", SupabaseConfig.supabaseUrl)
        assertFalse(SupabaseConfig.supabaseUrl.contains("httpshttps://"))
        assertFalse(SupabaseConfig.supabaseUrl.endsWith("/"))
    }

    // Teste 2: Publishable key presente
    @Test
    fun test02_PublishableKeyPresente() {
        assertEquals("sb_publishable_SSA6B6gfnaUxKhOzReE6HQ_utqQRebz", SupabaseConfig.SUPABASE_PUBLISHABLE_KEY)
        assertEquals("sb_publishable_SSA6B6gfnaUxKhOzReE6HQ_utqQRebz", SupabaseConfig.supabasePublishableKey)
        assertTrue(SupabaseConfig.supabasePublishableKey.startsWith("sb_publishable_"))
        assertTrue(SupabaseConfig.isConfigured)
    }

    // Teste 3: Interceptor não envia Bearer <publishable_key> no endpoint de login
    @Test
    fun test03_InterceptorNaoEnviaBearerNoEndpointDeLogin() {
        val interceptor = SupabaseAuthInterceptor(sessionManager = null)
        var capturedRequest: Request? = null

        val chain = object : Interceptor.Chain {
            private val req = Request.Builder()
                .url("${SupabaseConfig.supabaseUrl}/auth/v1/token?grant_type=password")
                .post("{\"email\":\"teste@ciadi.ao\",\"password\":\"123456\"}".toRequestBody("application/json".toMediaType()))
                .build()

            override fun request(): Request = req
            override fun proceed(request: Request): Response {
                capturedRequest = request
                return Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("{}".toResponseBody("application/json".toMediaType()))
                    .build()
            }
            override fun connection() = null
            override fun call() = throw UnsupportedOperationException()
            override fun connectTimeoutMillis() = 5000
            override fun withConnectTimeout(timeout: Int, unit: TimeUnit) = this
            override fun readTimeoutMillis() = 5000
            override fun withReadTimeout(timeout: Int, unit: TimeUnit) = this
            override fun writeTimeoutMillis() = 5000
            override fun withWriteTimeout(timeout: Int, unit: TimeUnit) = this
        }

        interceptor.intercept(chain)
        assertNotNull(capturedRequest)
        assertEquals(SupabaseConfig.supabasePublishableKey, capturedRequest?.header("apikey"))
        // No header Authorization no endpoint de login!
        assertNull(capturedRequest?.header("Authorization"))
    }

    // Teste 4 & 5 & 6 & 7: Deserialização de resposta GoTrue (access_token, refresh_token, user.id)
    @Test
    fun test04_07_SupabaseAuthResponseParsing() {
        val json = """
            {
                "access_token": "mock_jwt_access_token_123",
                "token_type": "bearer",
                "expires_in": 3600,
                "expires_at": 1790586240,
                "refresh_token": "mock_refresh_token_456",
                "user": {
                    "id": "uuid-usr-789-ciadi",
                    "email": "teste.familia@ciadi.ao",
                    "role": "authenticated",
                    "user_metadata": {
                        "full_name": "Família Teste CIADI",
                        "role": "responsavel",
                        "email_verified": false
                    },
                    "created_at": "2026-09-28T10:00:00Z"
                }
            }
        """.trimIndent()

        val adapter = moshi.adapter(SupabaseAuthResponse::class.java)
        val response = adapter.fromJson(json)

        assertNotNull(response)
        // Teste 5: access_token recebido
        assertEquals("mock_jwt_access_token_123", response?.accessToken)
        // Teste 6: refresh_token recebido
        assertEquals("mock_refresh_token_456", response?.refreshToken)
        // Teste 7: user.id recebido
        assertNotNull(response?.user)
        assertEquals("uuid-usr-789-ciadi", response?.user?.id)
        assertEquals("teste.familia@ciadi.ao", response?.user?.email)
        assertEquals("Família Teste CIADI", response?.user?.userMetadata?.fullName)
        assertEquals("responsavel", response?.user?.userMetadata?.role)
    }

    // Teste 8: Perfil encontrado na tabela 'perfis'
    @Test
    fun test08_PerfilDtoParsing() {
        val json = """
            [
                {
                    "id": "uuid-usr-789-ciadi",
                    "nome_completo": "Ana Paula Silva",
                    "nome_exibicao": "Ana Paula",
                    "email": "teste.familia@ciadi.ao",
                    "tipo": "responsavel",
                    "funcao_at": null,
                    "unidade_nome": "CIADI — Luanda",
                    "ativo": true
                }
            ]
        """.trimIndent()

        val adapter = moshi.adapter<List<PerfilDto>>(
            com.squareup.moshi.Types.newParameterizedType(List::class.java, PerfilDto::class.java)
        )
        val perfis = adapter.fromJson(json)
        assertNotNull(perfis)
        assertEquals(1, perfis?.size)
        val p = perfis!!.first()
        assertEquals("uuid-usr-789-ciadi", p.id)
        assertEquals("Ana Paula Silva", p.nomeCompleto)
        assertEquals("responsavel", p.tipo)
    }

    // Teste 9: Role encontrada e mapeamento de A.T. e outros
    @Test
    fun test09_RoleMapping() {
        assertEquals(UserRole.RESPONSAVEL, UserRole.fromCode("responsavel"))
        assertEquals(UserRole.ADMIN, UserRole.fromCode("admin"))
        assertEquals(UserRole.GESTOR, UserRole.fromCode("gestor"))
        assertEquals(UserRole.PROFISSIONAL, UserRole.fromCode("profissional"))
        assertEquals(UserRole.PACIENTE, UserRole.fromCode("paciente"))
        assertEquals(UserRole.OPERADOR, UserRole.fromCode("operador"))
        assertEquals(UserRole.AT, UserRole.fromCode("at"))
        assertEquals("A.T. — Acompanhante Terapêutico", UserRole.AT.label)

        // Resolução para as contas oficiais de teste sem depender de fallback cego
        assertEquals(UserRole.ADMIN, UserRole.resolve(code = null, email = "teste.admin@ciadi.ao"))
        assertEquals(UserRole.AT, UserRole.resolve(code = null, email = "teste.at@ciadi.ao"))
        assertEquals(UserRole.RESPONSAVEL, UserRole.resolve(code = null, email = "teste.familia@ciadi.ao"))
        assertEquals(UserRole.AT, UserRole.resolve(code = "profissional", email = "qualquer@ciadi.ao", funcao = "A.T. — Acompanhante Terapêutico"))
    }

    // Teste 10: RLS enforcement na tabela perfis do Supabase oficial
    @Test
    fun test10_PerfisTableEnforcesRowLevelSecurity() {
        val request = Request.Builder()
            .url("${SupabaseConfig.supabaseUrl}/rest/v1/perfis?select=*")
            .header("apikey", SupabaseConfig.supabaseAnonKey)
            .get()
            .build()

        val response = client.newCall(request).execute()
        assertEquals(200, response.code)
        val body = response.body?.string()
        assertNotNull(body)
        // RLS retorna array vazio quando não autenticado
        assertEquals("[]", body!!.trim())
    }

    // Teste 11: Bloqueio estrito de perfil desativado ou pendente de aprovação (Item 40)
    @Test
    fun test11_BloqueioDePerfilNaoAutorizado() {
        val profileAtivo = UserProfile(
            id = "usr_ativo",
            email = "aprovado@ciadi.ao",
            fullName = "Utilizador Aprovado",
            role = UserRole.PROFISSIONAL,
            ativo = true,
            statusAprovacao = "aprovado"
        )
        assertTrue(profileAtivo.isAuthorized)

        val profileDesativado = profileAtivo.copy(ativo = false)
        assertFalse(profileDesativado.isAuthorized)

        val profilePendente = profileAtivo.copy(statusAprovacao = "pendente")
        assertFalse(profilePendente.isAuthorized)

        val profileRejeitado = profileAtivo.copy(statusAprovacao = "rejeitado")
        assertFalse(profileRejeitado.isAuthorized)
    }

    // Teste 12: Secretaria mapeada para a role oficial OPERADOR (Item 39)
    @Test
    fun test12_SecretariaMapeadaParaOperador() {
        val roleSecretaria = UserRole.resolve(code = "secretaria", email = null)
        assertEquals(UserRole.OPERADOR, roleSecretaria)
        val profileSecretaria = UserProfile(
            id = "usr_sec",
            email = "secretaria@ciadi.ao",
            fullName = "Secretaria CIADI",
            role = roleSecretaria!!
        )
        assertTrue(profileSecretaria.isSecretary)
    }
}
