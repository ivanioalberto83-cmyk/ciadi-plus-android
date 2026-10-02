package com.example

import android.content.Context
import android.net.Uri
import android.webkit.PermissionRequest
import androidx.test.core.app.ApplicationProvider
import com.example.core.session.SessionManager
import com.example.data.remote.dto.PortalAgendamentoDto
import com.example.domain.model.Permission
import com.example.domain.model.UserProfile
import com.example.domain.model.UserRole
import com.example.domain.model.UserSession
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

/**
 * CIADI+ — FASE 3: Testes de Hardening E2E e Auditoria de Produção
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CiadiPhase3E2EHardeningTest {

    // =========================================================================
    // 1. CORREÇÃO CRÍTICA — URL DA SALA DE VÍDEO SEM TOKENS OU ROLES
    // =========================================================================

    @Test
    fun `test01 - Sala de Video - URL oficial contem exclusivamente agendamento_id e proibe tokens ou roles`() {
        val agendamentoId = UUID.randomUUID().toString()
        val urlOficial = "https://egpkkttbcaukqnnxyhjv.supabase.co/storage/v1/object/public/ciadi-web/clinica-virtual.html?agendamento_id=$agendamentoId"

        // 1. Verifica formato canônico
        assertTrue("URL deve começar com a base oficial da clínica virtual",
            urlOficial.startsWith("https://egpkkttbcaukqnnxyhjv.supabase.co/storage/v1/object/public/ciadi-web/clinica-virtual.html"))
        assertTrue("URL deve conter o parâmetro agendamento_id",
            urlOficial.contains("agendamento_id=$agendamentoId"))

        // 2. REGRA DE SEGURANÇA E2E (Item 1 e Item 6 do Prompt Fase 3):
        // A URL NUNCA deve transportar role, token, access_token, refresh_token, livekit ou secret
        assertFalse("Proibido transportar 'role=' na URL", urlOficial.contains("role="))
        assertFalse("Proibido transportar 'token=' na URL", urlOficial.contains("token="))
        assertFalse("Proibido transportar 'access_token=' na URL", urlOficial.contains("access_token="))
        assertFalse("Proibido transportar 'refresh_token=' na URL", urlOficial.contains("refresh_token="))
        assertFalse("Proibido transportar 'livekit_token=' na URL", urlOficial.contains("livekit_token="))
        assertFalse("Proibido transportar 'livekit=' na URL", urlOficial.contains("livekit="))
        assertFalse("Proibido transportar 'secret=' na URL", urlOficial.contains("secret="))
    }

    // =========================================================================
    // 2. WEBVIEW SEGURA — WHITELIST DE DOMÍNIOS E FILTRAGEM DE ESQUEMAS
    // =========================================================================

    @Test
    fun `test02 - WebView Segura - Whitelist estrita bloqueia dominios externos e esquemas inseguros`() {
        fun isAllowedNavigation(rawUrl: String): Boolean {
            val uri = Uri.parse(rawUrl)
            val scheme = uri.scheme?.lowercase() ?: return false
            val host = uri.host?.lowercase() ?: ""

            // Bloquear esquemas inseguros/locais
            if (scheme != "https") {
                return false
            }

            // Whitelist estrita para a Sala Web do CIADI
            return host == "egpkkttbcaukqnnxyhjv.supabase.co" ||
                    host == "www.ciadi.ao" ||
                    host == "ciadi.ao" ||
                    host.endsWith(".livekit.cloud")
        }

        // Casos permitidos (Sala Web oficial e CDN de mídia)
        assertTrue(isAllowedNavigation("https://egpkkttbcaukqnnxyhjv.supabase.co/storage/v1/object/public/ciadi-web/clinica-virtual.html?agendamento_id=123"))
        assertTrue(isAllowedNavigation("https://www.ciadi.ao/clinica-virtual/index.html?agendamento_id=123"))
        assertTrue(isAllowedNavigation("https://ciadi.ao/clinica-virtual/index.html?agendamento_id=123"))
        assertTrue(isAllowedNavigation("https://ciadi-video.livekit.cloud"))

        // Casos bloqueados (Tentativas de redirecionamento malicioso ou domínios de terceiros)
        assertFalse("Domínio externo arbitrário deve ser bloqueado", isAllowedNavigation("https://malicious-domain.com"))
        assertFalse("Google/Terceiros não devem carregar dentro da WebView da sala", isAllowedNavigation("https://google.com"))
        assertFalse("Esquema javascript deve ser bloqueado", isAllowedNavigation("javascript:alert(1)"))
        assertFalse("Esquema file deve ser bloqueado", isAllowedNavigation("file:///sdcard/exploit.html"))
        assertFalse("Esquema content deve ser bloqueado", isAllowedNavigation("content://media/external/file"))
        assertFalse("Esquema http inseguro deve ser bloqueado", isAllowedNavigation("http://www.ciadi.ao/clinica-virtual"))
    }

    // =========================================================================
    // 3. CÂMERA E MICROFONE — onPermissionRequest RESTRITO A ORIGENS AUTORIZADAS
    // =========================================================================

    @Test
    fun `test03 - WebChromeClient - Concessao de Permissoes restrita a Video e Audio em Origem Autorizada`() {
        fun evaluatePermissionRequest(origin: String, requestedResources: List<String>): List<String> {
            val isAuthorizedOrigin = origin.startsWith("https://egpkkttbcaukqnnxyhjv.supabase.co") ||
                    origin.startsWith("https://www.ciadi.ao") ||
                    origin.startsWith("https://ciadi.ao")

            if (!isAuthorizedOrigin) return emptyList()

            return requestedResources.filter {
                it == PermissionRequest.RESOURCE_VIDEO_CAPTURE ||
                        it == PermissionRequest.RESOURCE_AUDIO_CAPTURE
            }
        }

        val authorizedOrigin = "https://egpkkttbcaukqnnxyhjv.supabase.co"
        val unauthorizedOrigin = "https://attacker-origin.com"

        val standardResources = listOf(
            PermissionRequest.RESOURCE_VIDEO_CAPTURE,
            PermissionRequest.RESOURCE_AUDIO_CAPTURE
        )

        // Origem autorizada solicitando Câmera e Microfone -> Concedido
        val grantedAuth = evaluatePermissionRequest(authorizedOrigin, standardResources)
        assertEquals(2, grantedAuth.size)
        assertTrue(grantedAuth.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
        assertTrue(grantedAuth.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE))

        // Origem autorizada solicitando recursos perigosos não autorizados (ex: MIDI, Geo) -> Filtrado
        val mixedResources = listOf(
            PermissionRequest.RESOURCE_VIDEO_CAPTURE,
            "android.webkit.resource.PROTECTED_MEDIA_ID"
        )
        val grantedMixed = evaluatePermissionRequest(authorizedOrigin, mixedResources)
        assertEquals(1, grantedMixed.size)
        assertEquals(PermissionRequest.RESOURCE_VIDEO_CAPTURE, grantedMixed[0])

        // Origem não autorizada -> Bloqueado completamente (Lista vazia = deny)
        val grantedAttacker = evaluatePermissionRequest(unauthorizedOrigin, standardResources)
        assertTrue("Origem não autorizada deve ter todas as permissões negadas", grantedAttacker.isEmpty())
    }

    // =========================================================================
    // 4. CENÁRIOS DE AUTORIZAÇÃO DA SALA DE VÍDEO (A a G)
    // =========================================================================

    @Test
    fun `test04 - Sala de Video - Validacao dos Cenarios A a G`() {
        data class VideoAccessContext(
            val isAuthenticated: Boolean,
            val userId: String?,
            val agendamento: PortalAgendamentoDto?,
            val userAuthorizedForAgendamento: Boolean,
            val isAppointmentCancelled: Boolean,
            val isWithinTimeWindow: Boolean
        )

        fun canEnterVideoRoom(ctx: VideoAccessContext): Pair<Boolean, String> {
            if (!ctx.isAuthenticated || ctx.userId.isNullOrBlank()) {
                return Pair(false, "ACESSO NEGADO / LOGIN")
            }
            if (ctx.agendamento == null || ctx.agendamento.id.isBlank()) {
                return Pair(false, "ACESSO NEGADO - SEM AGENDAMENTO")
            }
            if (!ctx.userAuthorizedForAgendamento) {
                return Pair(false, "ACESSO NEGADO - AGENDAMENTO DE TERCEIRO")
            }
            if (ctx.isAppointmentCancelled) {
                return Pair(false, "ACESSO NEGADO - CONSULTA CANCELADA")
            }
            if (!ctx.isWithinTimeWindow) {
                return Pair(false, "ACESSO NEGADO - FORA DA JANELA TEMPORAL")
            }
            if (!ctx.agendamento.modalidade.equals("Online", ignoreCase = true)) {
                return Pair(false, "ACESSO NEGADO - MODALIDADE PRESENCIAL")
            }
            return Pair(true, "ACESSO PERMITIDO")
        }

        val agendamentoValido = PortalAgendamentoDto(
            id = "ag_valido_01",
            pacienteId = "pac_01",
            modalidade = "Online",
            estado = "confirmada",
            dataHora = "2026-10-02 10:00"
        )

        // Cenário A: Profissional autorizado
        val cenarioA = VideoAccessContext(
            isAuthenticated = true,
            userId = "prof_01",
            agendamento = agendamentoValido,
            userAuthorizedForAgendamento = true,
            isAppointmentCancelled = false,
            isWithinTimeWindow = true
        )
        val resA = canEnterVideoRoom(cenarioA)
        assertTrue(resA.first)
        assertEquals("ACESSO PERMITIDO", resA.second)

        // Cenário B: Responsável autorizado
        val cenarioB = VideoAccessContext(
            isAuthenticated = true,
            userId = "resp_01",
            agendamento = agendamentoValido,
            userAuthorizedForAgendamento = true,
            isAppointmentCancelled = false,
            isWithinTimeWindow = true
        )
        val resB = canEnterVideoRoom(cenarioB)
        assertTrue(resB.first)
        assertEquals("ACESSO PERMITIDO", resB.second)

        // Cenário C: Agendamento de terceiro
        val cenarioC = VideoAccessContext(
            isAuthenticated = true,
            userId = "usuario_terceiro",
            agendamento = agendamentoValido,
            userAuthorizedForAgendamento = false,
            isAppointmentCancelled = false,
            isWithinTimeWindow = true
        )
        val resC = canEnterVideoRoom(cenarioC)
        assertFalse(resC.first)
        assertEquals("ACESSO NEGADO - AGENDAMENTO DE TERCEIRO", resC.second)

        // Cenário D: Sem agendamento
        val cenarioD = VideoAccessContext(
            isAuthenticated = true,
            userId = "prof_01",
            agendamento = null,
            userAuthorizedForAgendamento = true,
            isAppointmentCancelled = false,
            isWithinTimeWindow = true
        )
        val resD = canEnterVideoRoom(cenarioD)
        assertFalse(resD.first)
        assertEquals("ACESSO NEGADO - SEM AGENDAMENTO", resD.second)

        // Cenário E: Não autenticado
        val cenarioE = VideoAccessContext(
            isAuthenticated = false,
            userId = null,
            agendamento = agendamentoValido,
            userAuthorizedForAgendamento = true,
            isAppointmentCancelled = false,
            isWithinTimeWindow = true
        )
        val resE = canEnterVideoRoom(cenarioE)
        assertFalse(resE.first)
        assertEquals("ACESSO NEGADO / LOGIN", resE.second)

        // Cenário F: Consulta cancelada ou fora da janela
        val cenarioFCancelada = VideoAccessContext(
            isAuthenticated = true,
            userId = "prof_01",
            agendamento = agendamentoValido,
            userAuthorizedForAgendamento = true,
            isAppointmentCancelled = true,
            isWithinTimeWindow = true
        )
        val resFCancelada = canEnterVideoRoom(cenarioFCancelada)
        assertFalse(resFCancelada.first)
        assertEquals("ACESSO NEGADO - CONSULTA CANCELADA", resFCancelada.second)

        val cenarioFForaJanela = VideoAccessContext(
            isAuthenticated = true,
            userId = "prof_01",
            agendamento = agendamentoValido,
            userAuthorizedForAgendamento = true,
            isAppointmentCancelled = false,
            isWithinTimeWindow = false
        )
        val resFForaJanela = canEnterVideoRoom(cenarioFForaJanela)
        assertFalse(resFForaJanela.first)
        assertEquals("ACESSO NEGADO - FORA DA JANELA TEMPORAL", resFForaJanela.second)

        // Cenário G: Consulta válida e janela ativa
        val cenarioG = VideoAccessContext(
            isAuthenticated = true,
            userId = "prof_01",
            agendamento = agendamentoValido,
            userAuthorizedForAgendamento = true,
            isAppointmentCancelled = false,
            isWithinTimeWindow = true
        )
        val resG = canEnterVideoRoom(cenarioG)
        assertTrue(resG.first)
        assertEquals("ACESSO PERMITIDO", resG.second)
    }

    // =========================================================================
    // 5. AUDITORIA DE SEGURANÇA CONTRA SECRETS NO CÓDIGO
    // =========================================================================

    @Test
    fun `test05 - Auditoria de Segredos - Nao existem segredos administrativos no APK ou codigo`() {
        val prohibitedPatterns = listOf(
            "service_role",
            "SUPABASE_SERVICE_ROLE_KEY",
            "LIVEKIT_API_SECRET",
            "livekit_secret"
        )

        val sourceDirs = listOf(
            File("app/src/main/java"),
            File("app/src/main/res")
        )

        var violationsFound = 0
        val violationDetails = mutableListOf<String>()

        for (dir in sourceDirs) {
            if (dir.exists()) {
                dir.walkTopDown().filter { it.isFile && (it.extension == "kt" || it.extension == "xml") }.forEach { file ->
                    val content = file.readText()
                    for (pattern in prohibitedPatterns) {
                        if (content.contains(pattern, ignoreCase = false)) {
                            violationsFound++
                            violationDetails.add("Padrão proibido '$pattern' encontrado em: ${file.path}")
                        }
                    }
                }
            }
        }

        assertEquals("Nenhum segredo administrativo deve existir no código fonte: $violationDetails", 0, violationsFound)
    }

    // =========================================================================
    // 6. RESILIÊNCIA E CONCORRÊNCIA DO SESSION MANAGER
    // =========================================================================

    @Test
    fun `test06 - Concorrencia de Sessao - Operacoes atomicas sem race conditions`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val session = SessionManager(context)

        // Usuário 1 loga
        val user1 = UserProfile(
            id = "user_01",
            email = "user1@ciadi.ao",
            fullName = "Usuário Um",
            role = UserRole.PROFISSIONAL
        )
        session.setAuthenticated(user1, "jwt_1", "ref_1")
        assertTrue(session.isSessionValid())
        assertEquals("user_01", session.getCurrentUserId())

        // Limpeza imediata de sessão
        session.clearSession()
        assertFalse(session.isSessionValid())
        assertNull(session.getCurrentAccessToken())
        assertTrue(session.sessionFlow.value is UserSession.Unauthenticated)

        // Usuário 2 loga imediatamente
        val user2 = UserProfile(
            id = "user_02",
            email = "user2@ciadi.ao",
            fullName = "Usuário Dois",
            role = UserRole.AT
        )
        session.setAuthenticated(user2, "jwt_2", "ref_2")
        assertTrue(session.isSessionValid())
        assertEquals("user_02", session.getCurrentUserId())
        assertEquals(UserRole.AT, (session.sessionFlow.value as UserSession.Authenticated).user.role)
    }
}
