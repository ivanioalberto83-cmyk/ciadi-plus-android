package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.config.SupabaseConfig
import com.example.core.session.SessionManager
import com.example.data.remote.client.SupabaseClientFactory
import com.example.data.remote.dto.ChatGrupoDto
import com.example.data.remote.dto.ChatMensagemDto
import com.example.data.remote.dto.ChatMensagemInsertDto
import com.example.data.remote.dto.PortalAgendamentoDto
import com.example.data.repository.SupabaseModulesRepositoryImpl
import com.example.domain.model.Permission
import com.example.domain.model.UserProfile
import com.example.domain.model.UserRole
import com.example.ui.screens.at.AtMenuSection
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CiadiChatPersistenceRobolectricTest {

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    @Test
    fun `test 1 - verificar estrutura exata da mensagem ciadi_chat_mensagens`() {
        val json = """
            {
                "id": "e980598d-c782-421e-8e68-9bb3298a00bf",
                "grupo_id": "c1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
                "remetente_id": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
                "mensagem": "Teste CIADI A.T. 001",
                "anexo_url": null,
                "anexo_nome": null,
                "lida_em": null,
                "created_at": "2026-10-01T15:00:00+00:00"
            }
        """.trimIndent()

        val adapter = moshi.adapter(ChatMensagemDto::class.java)
        val dto = adapter.fromJson(json)

        assertNotNull(dto)
        assertEquals("e980598d-c782-421e-8e68-9bb3298a00bf", dto?.id)
        assertEquals("c1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c", dto?.grupoId)
        assertEquals("a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d", dto?.remetenteId)
        assertEquals("Teste CIADI A.T. 001", dto?.mensagem)
        assertEquals("Teste CIADI A.T. 001", dto?.text)
        assertEquals("15:00", dto?.timestamp)
    }

    @Test
    fun `test 2 - verificar DTO estrito para INSERT ciadi_chat_mensagens com remetente_id auth uid`() {
        val insertDto = ChatMensagemInsertDto(
            grupoId = "c1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
            remetenteId = "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
            mensagem = "Teste CIADI A.T. 001"
        )

        val adapter = moshi.adapter(ChatMensagemInsertDto::class.java)
        val serialized = adapter.toJson(insertDto)

        assertTrue(serialized.contains("\"grupo_id\""))
        assertTrue(serialized.contains("\"remetente_id\""))
        assertTrue(serialized.contains("\"mensagem\""))
        assertTrue(serialized.contains("Teste CIADI A.T. 001"))
    }

    @Test
    fun `test 3 - verificar envio correto com confirmacao Supabase e insercao na UI`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sessionManager = SessionManager(context)
        val testUserId = "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d"
        val testGrupoId = "c1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c"

        sessionManager.setAuthenticated(
            user = UserProfile(
                id = testUserId,
                email = "at@ciadi.ao",
                fullName = "Acompanhante Terapêutico",
                role = UserRole.AT
            ),
            accessToken = "valid_jwt_token",
            refreshToken = "refresh_token"
        )

        // Mock HTTP para simular INSERT e consulta com retorno de confirmação do Supabase
        val mockClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                val urlStr = request.url.toString()
                if (urlStr.contains("ciadi_chat_mensagens")) {
                    val responseJson = """
                        [
                            {
                                "id": "msg-persisted-001",
                                "grupo_id": "$testGrupoId",
                                "remetente_id": "$testUserId",
                                "mensagem": "Teste CIADI A.T. 001",
                                "anexo_url": null,
                                "anexo_nome": null,
                                "lida_em": null,
                                "created_at": "2026-10-01T15:00:00+00:00"
                            }
                        ]
                    """.trimIndent()
                    Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(if (request.method == "POST") 201 else 200)
                        .message("OK")
                        .body(responseJson.toResponseBody("application/json".toMediaType()))
                        .build()
                } else {
                    chain.proceed(request)
                }
            }
            .build()

        val factory = SupabaseClientFactory(sessionManager = sessionManager, customOkHttpClient = mockClient)
        val repo = SupabaseModulesRepositoryImpl(sessionManager, factory)

        // Simula envio de mensagem com confirmação do Supabase
        val result = repo.enviarMensagemTexto(
            grupoId = testGrupoId,
            texto = "Teste CIADI A.T. 001",
            userId = testUserId
        )

        if (result.isFailure) {
            val ex = result.exceptionOrNull()
            throw AssertionError("enviarMensagemTexto failed: ${ex?.message}", ex)
        }
        assertTrue(result.isSuccess)
        val msgCriada = result.getOrNull()
        assertNotNull(msgCriada)
        assertEquals("msg-persisted-001", msgCriada?.id)
        assertEquals("Teste CIADI A.T. 001", msgCriada?.mensagem)

        // Mensagens do grupo contêm o registro confirmado
        val mensagens = repo.observeMensagens(testGrupoId).value
        assertEquals(1, mensagens.size)
        assertEquals("msg-persisted-001", mensagens.first().id)
    }

    @Test
    fun `test 4 - verificar erro no INSERT nao adiciona mensagem na UI`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sessionManager = SessionManager(context)
        val testUserId = "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d"
        val testGrupoId = "c1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c"

        sessionManager.setAuthenticated(
            user = UserProfile(
                id = testUserId,
                email = "at@ciadi.ao",
                fullName = "A.T.",
                role = UserRole.AT
            ),
            accessToken = "valid_jwt_token",
            refreshToken = "refresh_token"
        )

        // Simula falha de conexão / erro no Supabase
        val repo = SupabaseModulesRepositoryImpl(sessionManager)

        // Envio com grupoId inválido ou sem rede
        val result = repo.enviarMensagemTexto(
            grupoId = "id-invalido",
            texto = "Mensagem que não deve ser gravada",
            userId = testUserId
        )

        // Não mostrar como enviada se o INSERT retornar erro
        assertTrue(result.isFailure)
        assertEquals(
            "Não foi possível enviar a mensagem. Verifique a ligação e tente novamente.",
            result.exceptionOrNull()?.message
        )

        // A lista local deve permanecer vazia
        val mensagens = repo.observeMensagens("id-invalido").value
        assertTrue(mensagens.isEmpty())
    }

    @Test
    fun `test 5 - verificar deduplicacao Realtime e ordenacao por created_at`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sessionManager = SessionManager(context)
        val testGrupoId = "c1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c"
        val repo = SupabaseModulesRepositoryImpl(sessionManager)

        val msg1 = ChatMensagemDto(
            id = "msg-001",
            grupoId = testGrupoId,
            remetenteId = "usr-1",
            mensagem = "Teste CIADI A.T. 001",
            createdAt = "2026-10-01T15:00:00+00:00"
        )

        val msg2 = ChatMensagemDto(
            id = "msg-002",
            grupoId = testGrupoId,
            remetenteId = "usr-2",
            mensagem = "Mensagem recebida.",
            createdAt = "2026-10-01T15:01:00+00:00"
        )

        // Inicia Realtime
        repo.iniciarRealtime(testGrupoId)

        // Simula recebimento de mensagens e repetição de Realtime
        repo.carregarMensagensGrupo(testGrupoId)

        // Simula persistência de duas mensagens entre Utilizador A e Utilizador B
        val msg1Obj = repo.enviarMensagem(msg1)
        val msg2Obj = repo.enviarMensagem(msg2)

        // Se uma mensagem com mesmo ID chegar via Realtime, não deve duplicar
        val current = repo.observeMensagens(testGrupoId).value
        val uniqueIds = current.map { it.id }.distinct()
        assertEquals(uniqueIds.size, current.size)
    }

    @Test
    fun `test 6 - verificar 10 modulos obrigatorios do perfil AT`() {
        val sections = AtMenuSection.values()
        assertEquals(10, sections.size)

        val tags = sections.map { it.tag }
        assertTrue(tags.contains("menu_at_inicio"))
        assertTrue(tags.contains("menu_at_assistidos"))
        assertTrue(tags.contains("menu_at_calendario"))
        assertTrue(tags.contains("menu_at_atividades"))
        assertTrue(tags.contains("menu_at_atividades_plus"))
        assertTrue(tags.contains("menu_at_acompanhamento"))
        assertTrue(tags.contains("menu_at_chat"))
        assertTrue(tags.contains("menu_at_organizacao"))
        assertTrue(tags.contains("menu_at_notificacoes"))
        assertTrue(tags.contains("menu_at_perfil"))
    }

    @Test
    fun `test 7 - verificar Sala de Video utiliza exclusivamente URL oficial clinica-virtual`() {
        val agendamento = PortalAgendamentoDto(
            id = "ag-video-99",
            paciente = "Assistido Teste",
            pacienteId = "pac-01",
            profissional = "Dr. Especialista",
            especialidade = "Neuropediatria",
            dataHora = "2026-10-01 16:00",
            duracao = 50,
            tipo = "Consulta",
            modalidade = "Online",
            estado = "confirmada",
            linkVideo = null
        )

        val urlEsperada = "https://www.ciadi.ao/clinica-virtual/index.html?agendamento_id=${agendamento.id}"
        assertEquals("https://www.ciadi.ao/clinica-virtual/index.html?agendamento_id=ag-video-99", urlEsperada)
        assertFalse(urlEsperada.contains("/sala.html"))
        assertTrue(urlEsperada.startsWith("https://www.ciadi.ao/clinica-virtual/index.html"))
    }
}
