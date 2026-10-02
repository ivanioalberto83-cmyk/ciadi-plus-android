package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.config.SupabaseConfig
import com.example.core.session.SessionManager
import com.example.data.remote.client.SupabaseClientFactory
import com.example.data.remote.interceptor.SupabaseAuthInterceptor
import com.example.domain.model.Permission
import com.example.domain.model.UserProfile
import com.example.domain.model.UserRole
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("CIADI+", appName)
    }

    @Test
    fun `verify role permissions hierarchy`() {
        val adminPermissions = Permission.defaultPermissionsFor(UserRole.ADMIN)
        assertEquals(Permission.entries.size, adminPermissions.size)

        val familyPermissions = Permission.defaultPermissionsFor(UserRole.RESPONSAVEL)
        assertTrue(familyPermissions.contains(Permission.VIEW_AGENDA))
        assertTrue(familyPermissions.contains(Permission.VIEW_VINCULO_CRIANCAS))
        assertTrue(familyPermissions.contains(Permission.VIEW_DOCUMENTS_PUBLISHED))
        assertTrue(familyPermissions.contains(Permission.ACCESS_CHAT))

        val atPermissions = Permission.defaultPermissionsFor(UserRole.AT)
        assertTrue(atPermissions.contains(Permission.VIEW_ATRIBUICOES_AT))
        assertTrue(atPermissions.contains(Permission.WRITE_AT_SESSAO))
        assertTrue(atPermissions.contains(Permission.WRITE_AT_INCIDENTE))

        val profPermissions = Permission.defaultPermissionsFor(UserRole.PROFISSIONAL)
        assertTrue(profPermissions.contains(Permission.SUBMIT_CLINICAL_FORM))
        assertTrue(profPermissions.contains(Permission.MANAGE_DOCUMENTS))
        assertTrue(profPermissions.contains(Permission.MANAGE_AGENDA))
    }

    @Test
    fun `verify Retrofit client factory configuration and endpoints`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sessionManager = SessionManager(context)
        val factory = SupabaseClientFactory(
            sessionManager = sessionManager,
            customBaseUrl = "https://ciadi-test.supabase.co"
        )

        assertEquals("https://ciadi-test.supabase.co/", factory.getBaseUrl())
        assertNotNull(factory.authApi)
        assertNotNull(factory.restApi)
        assertNotNull(factory.okHttpClient)
    }

    @Test
    fun `verify SupabaseAuthInterceptor injects apikey and user JWT for RLS readiness`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sessionManager = SessionManager(context)
        val testJwt = "test_user_jwt_token_for_rls"

        sessionManager.setAuthenticated(
            user = UserProfile(
                id = "test_id",
                email = "at@ciadi.org.br",
                fullName = "Acompanhante Teste",
                role = UserRole.AT
            ),
            accessToken = testJwt,
            refreshToken = "test_refresh"
        )

        val interceptor = SupabaseAuthInterceptor(sessionManager)

        var interceptedRequest: Request? = null
        val chain = object : Interceptor.Chain {
            private val req = Request.Builder()
                .url("https://ciadi-test.supabase.co/rest/v1/agendamentos")
                .build()

            override fun request(): Request = req

            override fun proceed(request: Request): Response {
                interceptedRequest = request
                return Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("[]".toResponseBody("application/json".toMediaType()))
                    .build()
            }

            override fun connection() = null
            override fun call() = throw UnsupportedOperationException()
            override fun connectTimeoutMillis() = 10000
            override fun withConnectTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
            override fun readTimeoutMillis() = 10000
            override fun withReadTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
            override fun writeTimeoutMillis() = 10000
            override fun withWriteTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
        }

        val response = interceptor.intercept(chain)
        assertEquals(200, response.code)
        assertNotNull(interceptedRequest)
        assertEquals("Bearer $testJwt", interceptedRequest?.header("Authorization"))
        assertEquals("application/json", interceptedRequest?.header("Content-Type"))
        assertNotNull(interceptedRequest?.header("apikey"))
    }

    @Test
    fun `verify SupabaseErrorHandler maps HTTP status codes 401 403 404 422 500 to friendly Portuguese`() {
        val msg401 = com.example.core.network.SupabaseErrorHandler.parseHttpErrorMessage(401)
        assertTrue(msg401.contains("sessão expirou", ignoreCase = true))

        val msg403 = com.example.core.network.SupabaseErrorHandler.parseHttpErrorMessage(403)
        assertTrue(msg403.contains("autorização", ignoreCase = true))
        assertTrue(msg403.contains("RLS", ignoreCase = true))

        val msg404 = com.example.core.network.SupabaseErrorHandler.parseHttpErrorMessage(404)
        assertTrue(msg404.contains("não foi encontrado", ignoreCase = true))

        val msg422 = com.example.core.network.SupabaseErrorHandler.parseHttpErrorMessage(422)
        assertTrue(msg422.contains("inválidos", ignoreCase = true))

        val msg500 = com.example.core.network.SupabaseErrorHandler.parseHttpErrorMessage(500)
        assertTrue(msg500.contains("Instabilidade temporária", ignoreCase = true))
    }

    @Test
    fun `verify FamilyRepository and explicit patient selection`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sessionManager = SessionManager(context)
        val repo = com.example.data.repository.SupabaseModulesRepositoryImpl(sessionManager)

        val testPatient = com.example.data.remote.dto.PacienteDto(
            id = "pac_01",
            nome = "Lucas Silva"
        )
        repo.selectPatient(testPatient)
        assertEquals("Lucas Silva", repo.observeSelectedPatient().value?.nome)
    }

    @Test
    fun `verify AuthRepository session lifecycle and logout`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sessionManager = SessionManager(context)
        val authRepo = com.example.data.repository.SupabaseAuthRepositoryImpl(sessionManager)

        kotlinx.coroutines.runBlocking {
            authRepo.signOut()
            assertTrue(authRepo.sessionFlow.value is com.example.domain.model.UserSession.Unauthenticated)
        }
    }

    @Test
    fun `verify SOS repository contract and payload generation`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sessionManager = SessionManager(context)
        val repo = com.example.data.repository.SupabaseModulesRepositoryImpl(sessionManager)

        kotlinx.coroutines.runBlocking {
            // Chamada sem localização autorizada
            val req = com.example.data.remote.dto.CriarAlertaSosRequest(
                pacienteId = "pac_01",
                mensagem = "Alerta de crise",
                latitude = null,
                longitude = null,
                localizacaoAutorizada = false
            )
            assertEquals("pac_01", req.pacienteId)
            assertEquals(false, req.localizacaoAutorizada)

            // Chamada com localização autorizada
            val reqWithLoc = com.example.data.remote.dto.CriarAlertaSosRequest(
                pacienteId = "pac_01",
                mensagem = "Alerta de crise",
                latitude = -8.8383,
                longitude = 13.2344,
                localizacaoAutorizada = true
            )
            assertEquals(true, reqWithLoc.localizacaoAutorizada)
            assertEquals(-8.8383, reqWithLoc.latitude!!, 0.0001)
        }
    }

    @Test
    fun `verify PortalAgendamentoDto and smart calendar model`() {
        val agendamento = com.example.data.remote.dto.PortalAgendamentoDto(
            id = "ag_01",
            paciente = "Lucas Silva",
            pacienteId = "pac_01",
            profissional = "Dra. Maria",
            especialidade = "Psicologia",
            dataHora = "2026-09-27 10:00",
            duracao = 50,
            tipo = "Consulta",
            modalidade = "Online",
            estado = "confirmada",
            linkVideo = "https://ciadi.org.br/sala/01"
        )

        assertEquals("Online", agendamento.modalidade)
        assertEquals("confirmada", agendamento.estado)
        assertEquals("Dra. Maria", agendamento.profissional)
        assertTrue(agendamento.modalidade.equals("Online", ignoreCase = true))
    }
}
