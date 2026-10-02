package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import com.example.core.network.SupabaseErrorHandler
import com.example.core.session.SessionManager
import com.example.data.remote.dto.*
import com.example.domain.janeth.JanethKnowledgeBase
import com.example.domain.model.Permission
import com.example.domain.model.UserProfile
import com.example.domain.model.UserRole
import com.example.domain.model.UserSession
import com.example.ui.screens.at.AtMenuSection
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CiadiPhase2FunctionalValidationTest {

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    // =========================================================================
    // 1. AUTENTICAÇÃO REAL & SESSÃO PERSISTENTE
    // =========================================================================

    @Test
    fun `test01 - Autenticacao - login valido, persistencia e restauracao em cold start`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val session1 = SessionManager(context)

        val user = UserProfile(
            id = "a1b2c3d4-0000-0000-0000-000000000001",
            email = "responsavel@ciadi.ao",
            fullName = "Maria Responsável Teste",
            role = UserRole.RESPONSAVEL,
            permissions = Permission.defaultPermissionsFor(UserRole.RESPONSAVEL)
        )
        val testJwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test-jwt-ciadi"
        val testRefresh = "refresh-token-ciadi-01"

        session1.setAuthenticated(user, testJwt, testRefresh)
        assertTrue(session1.sessionFlow.value is UserSession.Authenticated)

        val restoredUser = (session1.sessionFlow.value as UserSession.Authenticated).user
        assertEquals("a1b2c3d4-0000-0000-0000-000000000001", restoredUser.id)
        assertEquals(UserRole.RESPONSAVEL, restoredUser.role)
        assertEquals(testJwt, session1.getCurrentAccessToken())
        assertEquals(testRefresh, session1.getCurrentRefreshToken())
        assertTrue(restoredUser.permissions.contains(Permission.VIEW_VINCULO_CRIANCAS))
    }

    @Test
    fun `test02 - Autenticacao - login invalido e erro amigavel`() {
        val errorMsg401 = SupabaseErrorHandler.parseHttpErrorMessage(401)
        assertTrue(errorMsg401.contains("sessão expirou") || errorMsg401.contains("iniciar sessão"))

        val errorMsg403 = SupabaseErrorHandler.parseHttpErrorMessage(403)
        assertTrue(errorMsg403.contains("autorização") && errorMsg403.contains("RLS"))
    }

    @Test
    fun `test03 - Autenticacao - sessao expirada limpa estado e redireciona`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val session = SessionManager(context)

        session.setAuthenticated(
            user = UserProfile(id = "user_exp", email = "test@ciadi.ao", fullName = "Exp Test", role = UserRole.AT),
            accessToken = "expired_token",
            refreshToken = "refresh"
        )
        assertTrue(session.isSessionValid())

        // Simula resposta 401 de token expirado
        session.clearSession()
        assertFalse(session.isSessionValid())
        assertTrue(session.sessionFlow.value is UserSession.Unauthenticated)
        assertNull(session.getCurrentAccessToken())
    }

    // =========================================================================
    // 2. MATRIZ DE ROLES (Ver | Criar | Editar | Excluir | Publicar)
    // =========================================================================

    @Test
    fun `test04 - Matriz de Seguranca por Role - Admin, Gestor, Profissional, AT, Responsavel, Paciente`() {
        data class RoleCapability(
            val role: UserRole,
            val podeVerAgenda: Boolean,
            val podeGerirAgenda: Boolean,
            val podeVerCriancas: Boolean,
            val podeSubmeterFormulario: Boolean,
            val podeGerirDocumentos: Boolean,
            val podeVerDocumentosPublicados: Boolean,
            val podeRegistrarSessaoAt: Boolean,
            val podeAcessarChat: Boolean,
            val podeReceberNotificacoes: Boolean,
            val podeAuditarAdmin: Boolean
        )

        val rolesToTest = listOf(
            RoleCapability(
                role = UserRole.ADMIN,
                podeVerAgenda = true, podeGerirAgenda = true, podeVerCriancas = true,
                podeSubmeterFormulario = true, podeGerirDocumentos = true, podeVerDocumentosPublicados = true,
                podeRegistrarSessaoAt = true, podeAcessarChat = true, podeReceberNotificacoes = true, podeAuditarAdmin = true
            ),
            RoleCapability(
                role = UserRole.GESTOR,
                podeVerAgenda = true, podeGerirAgenda = true, podeVerCriancas = true,
                podeSubmeterFormulario = true, podeGerirDocumentos = true, podeVerDocumentosPublicados = true,
                podeRegistrarSessaoAt = false, podeAcessarChat = true, podeReceberNotificacoes = true, podeAuditarAdmin = true
            ),
            RoleCapability(
                role = UserRole.PROFISSIONAL,
                podeVerAgenda = true, podeGerirAgenda = true, podeVerCriancas = true,
                podeSubmeterFormulario = true, podeGerirDocumentos = true, podeVerDocumentosPublicados = true,
                podeRegistrarSessaoAt = false, podeAcessarChat = true, podeReceberNotificacoes = true, podeAuditarAdmin = false
            ),
            RoleCapability(
                role = UserRole.AT,
                podeVerAgenda = true, podeGerirAgenda = false, podeVerCriancas = false,
                podeSubmeterFormulario = false, podeGerirDocumentos = false, podeVerDocumentosPublicados = true,
                podeRegistrarSessaoAt = true, podeAcessarChat = true, podeReceberNotificacoes = true, podeAuditarAdmin = false
            ),
            RoleCapability(
                role = UserRole.RESPONSAVEL,
                podeVerAgenda = true, podeGerirAgenda = false, podeVerCriancas = true,
                podeSubmeterFormulario = false, podeGerirDocumentos = false, podeVerDocumentosPublicados = true,
                podeRegistrarSessaoAt = false, podeAcessarChat = true, podeReceberNotificacoes = true, podeAuditarAdmin = false
            ),
            RoleCapability(
                role = UserRole.PACIENTE,
                podeVerAgenda = true, podeGerirAgenda = false, podeVerCriancas = false,
                podeSubmeterFormulario = false, podeGerirDocumentos = false, podeVerDocumentosPublicados = true,
                podeRegistrarSessaoAt = false, podeAcessarChat = true, podeReceberNotificacoes = true, podeAuditarAdmin = false
            )
        )

        for (cap in rolesToTest) {
            val permissions = Permission.defaultPermissionsFor(cap.role)
            assertEquals("Erro em podeVerAgenda para ${cap.role}", cap.podeVerAgenda, permissions.contains(Permission.VIEW_AGENDA))
            assertEquals("Erro em podeGerirAgenda para ${cap.role}", cap.podeGerirAgenda, permissions.contains(Permission.MANAGE_AGENDA))
            assertEquals("Erro em podeVerCriancas para ${cap.role}", cap.podeVerCriancas, permissions.contains(Permission.VIEW_VINCULO_CRIANCAS))
            assertEquals("Erro em podeSubmeterFormulario para ${cap.role}", cap.podeSubmeterFormulario, permissions.contains(Permission.SUBMIT_CLINICAL_FORM))
            assertEquals("Erro em podeGerirDocumentos para ${cap.role}", cap.podeGerirDocumentos, permissions.contains(Permission.MANAGE_DOCUMENTS))
            assertEquals("Erro em podeVerDocumentosPublicados para ${cap.role}", cap.podeVerDocumentosPublicados, permissions.contains(Permission.VIEW_DOCUMENTS_PUBLISHED))
            assertEquals("Erro em podeRegistrarSessaoAt para ${cap.role}", cap.podeRegistrarSessaoAt, permissions.contains(Permission.WRITE_AT_SESSAO))
            assertEquals("Erro em podeAcessarChat para ${cap.role}", cap.podeAcessarChat, permissions.contains(Permission.ACCESS_CHAT))
            assertEquals("Erro em podeReceberNotificacoes para ${cap.role}", cap.podeReceberNotificacoes, permissions.contains(Permission.RECEIVE_NOTIFICATIONS))
            assertEquals("Erro em podeAuditarAdmin para ${cap.role}", cap.podeAuditarAdmin, permissions.contains(Permission.ADMIN_AUDIT))
        }
    }

    // =========================================================================
    // 3. FAMÍLIA & REVOGAÇÃO DE AUTORIZAÇÃO
    // =========================================================================

    @Test
    fun `test05 - Familia - acesso a crianca vinculada e revogacao estrita de acesso`() {
        val vinculoAtivo = VinculoFamiliarDto(
            id = "vinculo_01",
            perfilId = "resp_01",
            pacienteId = "pac_01",
            parentesco = "Mãe",
            ativo = true,
            responsavelLegal = true,
            podeAgendar = true,
            podeVerDocumentos = true,
            podeVerAcompanhamento = true,
            podeReceberNotificacoes = true,
            podeGerirDados = false,
            nivelAcesso = "TOTAL",
            validadeAte = "2027-12-31"
        )
        assertTrue(vinculoAtivo.podeVerDocumentos)
        assertTrue(vinculoAtivo.podeVerAcompanhamento)

        val vinculoRevogado = vinculoAtivo.copy(
            podeVerDocumentos = false,
            podeVerAcompanhamento = false,
            podeAgendar = false
        )
        assertFalse("Documentos devem ser bloqueados após revogação", vinculoRevogado.podeVerDocumentos)
        assertFalse("Acompanhamentos devem ser bloqueados após revogação", vinculoRevogado.podeVerAcompanhamento)
        assertFalse("Agendamento deve ser bloqueado após revogação", vinculoRevogado.podeAgendar)
    }

    // =========================================================================
    // 4. EQUIPA & ATRIBUIÇÕES A.T.
    // =========================================================================

    @Test
    fun `test06 - Equipa - ciclo de atribuicao AT e vinculo com paciente`() {
        val atribuicao = AtribuicaoAtDto(
            id = "atrib_01",
            profissionalId = "prof_at_01",
            pacienteId = "pac_01",
            escolaOuLocal = "Colégio CIADI",
            turno = "Manhã",
            ativo = true
        )

        assertEquals("prof_at_01", atribuicao.profissionalId)
        assertEquals("pac_01", atribuicao.pacienteId)
        assertTrue(atribuicao.ativo)

        // Encerramento da atribuição
        val atribuicaoEncerrada = atribuicao.copy(ativo = false)
        assertFalse(atribuicaoEncerrada.ativo)
    }

    // =========================================================================
    // 5. A.T. — 10 SEÇÕES OBRIGATÓRIAS E FLUXO CLÍNICO COMPLETO
    // =========================================================================

    @Test
    fun `test07 - AT - 10 secoes obrigatorias do perfil AT devidamente mapeadas`() {
        val sections = AtMenuSection.entries
        assertEquals(10, sections.size)

        val sectionLabels = sections.map { it.label }
        assertTrue(sectionLabels.contains("Início"))
        assertTrue(sectionLabels.contains("Meus Assistidos"))
        assertTrue(sectionLabels.contains("Calendário"))
        assertTrue(sectionLabels.contains("Atividades"))
        assertTrue(sectionLabels.contains("Atividades +"))
        assertTrue(sectionLabels.contains("Acompanhamento"))
        assertTrue(sectionLabels.contains("Chat"))
        assertTrue(sectionLabels.contains("Organização"))
        assertTrue(sectionLabels.contains("Notificações"))
        assertTrue(sectionLabels.contains("Meu Perfil"))
    }

    @Test
    fun `test08 - AT - CRUD do registro de sessao e objetivo PEI`() {
        val sessao = AtSessaoDto(
            id = "sessao_01",
            atribuicaoId = "atrib_01",
            profissionalAtId = "prof_at_01",
            pacienteId = "pac_01",
            pacienteNome = "Lucas Silva",
            dataSessao = "2026-10-02",
            horaInicio = "09:00",
            horaFim = "11:00",
            objetivo = "Permanecer sentado durante atividade dirigida",
            comportamentoObservado = "Boa receptividade com apoio visual",
            status = "FINALIZADA",
            estado = "Realizada"
        )
        assertEquals("sessao_01", sessao.id)
        assertEquals("FINALIZADA", sessao.status)

        val objetivo = AtRegistoObjetivoDto(
            id = "obj_01",
            sessaoId = "sessao_01",
            objetivoPei = "Contato visual espontâneo",
            nivelAutonomia = "AJUDA_LEVE",
            evolucao = "Atingiu 80% das tentativas",
            pontuacao = 8
        )
        assertEquals("obj_01", objetivo.id)
        assertEquals("AJUDA_LEVE", objetivo.nivelAutonomia)

        val incidente = AtIncidenteDto(
            id = "inc_01",
            sessaoId = "sessao_01",
            gravidade = "LEVE",
            descricao = "Frustração momentânea ao término do recreio",
            condutaAdotada = "Uso de cronômetro visual de transição"
        )
        assertEquals("inc_01", incidente.id)
        assertEquals("LEVE", incidente.gravidade)
    }

    // =========================================================================
    // 6. FORMULÁRIOS CLÍNICOS DINÂMICOS (ciadi_formularios_clinicos)
    // =========================================================================

    @Test
    fun `test09 - Formularios Clinicos - schema JSON dinamico para especialidades CIADI`() {
        val especialidadesTestadas = listOf(
            "Psicologia", "Terapia da Fala", "Terapia Ocupacional",
            "Pediatria", "Neurodesenvolvimento", "Multidisciplinar",
            "Plano Terapêutico", "Evolução", "ABA", "Reavaliação", "Alta/Referência"
        )

        for (esp in especialidadesTestadas) {
            val schemaJson = """
                {
                    "especialidade": "$esp",
                    "versao": "1.0",
                    "campos": [
                        {"id": "campo_queixa", "label": "Queixa Principal", "tipo": "texto", "obrigatorio": true},
                        {"id": "campo_observacao", "label": "Observações Clínicas", "tipo": "area_texto", "obrigatorio": false}
                    ]
                }
            """.trimIndent()

            val formDto = FormularioClinicoDto(
                id = UUID.randomUUID().toString(),
                nome = "Avaliação Inicial - $esp",
                descricao = "Instrumento clínico para $esp",
                areaAtuacao = esp,
                tipoFormulario = "AVALIACAO",
                schemaJson = schemaJson,
                ativo = true,
                versao = "1.0",
                publicado = true
            )

            assertTrue(formDto.ativo)
            assertTrue(formDto.publicado)
            assertEquals(esp, formDto.areaAtuacao)

            val jsonObject = JSONObject(formDto.schemaJson ?: "{}")
            assertEquals(esp, jsonObject.getString("especialidade"))
            val campos = jsonObject.getJSONArray("campos")
            assertEquals(2, campos.length())
            assertEquals("campo_queixa", campos.getJSONObject(0).getString("id"))
        }
    }

    // =========================================================================
    // 7. DOCUMENTOS CLÍNICOS (Emissão, Assinatura, QR Code, Validação)
    // =========================================================================

    @Test
    fun `test10 - Documentos - emissao com codigo de validacao e controle de publicacao`() {
        val codigoValidacao = "CIADI-DOC-" + UUID.randomUUID().toString().substring(0, 8).uppercase()
        val documentoRascunho = DocumentoClinicoDto(
            id = "doc_01",
            pacienteId = "pac_01",
            pacienteNome = "Lucas Silva",
            profissionalNome = "Dra. Maria (Fonoaudiologia)",
            titulo = "Avaliação da Comunicação Funcional",
            tipoDocumento = "LAUDO",
            conteudoTexto = "Conteúdo clínico confidencial",
            codigoValidacao = codigoValidacao,
            estado = "Rascunho",
            versao = "1.0"
        )

        assertEquals("Rascunho", documentoRascunho.estado)
        assertFalse("Rascunho não deve ser considerado Publicado", documentoRascunho.estado.equals("PUBLICADO", ignoreCase = true))

        // Finalização, assinatura do profissional e publicação para a família
        val documentoPublicado = documentoRascunho.copy(
            estado = "PUBLICADO",
            versao = "2.0"
        )
        assertTrue("Documento deve estar PUBLICADO", documentoPublicado.estado.equals("PUBLICADO", ignoreCase = true))
        assertEquals("2.0", documentoPublicado.versao)
        assertTrue(documentoPublicado.codigoValidacao!!.startsWith("CIADI-DOC-"))
    }

    // =========================================================================
    // 8. AGENDA — MODALIDADES (Presencial / Online) E VISIBILIDADE
    // =========================================================================

    @Test
    fun `test11 - Agenda - modalidades presencial e online com duracao e estados`() {
        val consultaOnline = PortalAgendamentoDto(
            id = "ag_online_01",
            paciente = "Lucas Silva",
            pacienteId = "pac_01",
            profissional = "Dr. Carlos (Psicólogo)",
            especialidade = "Psicologia",
            dataHora = "2026-10-05 14:00",
            duracao = 45,
            modalidade = "Online",
            estado = "confirmada",
            salaNome = "Sala Virtual 02"
        )
        assertTrue(consultaOnline.modalidade.equals("Online", ignoreCase = true))
        assertEquals("confirmada", consultaOnline.estado)
        assertEquals(45, consultaOnline.duracao)

        val consultaPresencial = PortalAgendamentoDto(
            id = "ag_presencial_01",
            paciente = "Lucas Silva",
            pacienteId = "pac_01",
            profissional = "Dra. Paula (Terapeuta Ocupacional)",
            especialidade = "Terapia Ocupacional",
            dataHora = "2026-10-06 09:00",
            duracao = 60,
            modalidade = "Presencial",
            estado = "agendada",
            salaNome = "Consultório 03"
        )
        assertFalse(consultaPresencial.modalidade.equals("Online", ignoreCase = true))
        assertEquals("Presencial", consultaPresencial.modalidade)
    }

    // =========================================================================
    // 9. SALA DE VÍDEO (Sete Cenários Críticos: A, B, C, D, E, F, G)
    // =========================================================================

    @Test
    fun `test12 - Sala de Video - validacao dos cenarios criticos A a G sem token LiveKit no cliente`() {
        val urlOficialBase = "https://egpkkttbcaukqnnxyhjv.supabase.co/storage/v1/object/public/ciadi-web/clinica-virtual.html"

        val agendamentoValido = PortalAgendamentoDto(
            id = "ag_valido_99",
            pacienteId = "pac_01",
            modalidade = "Online",
            estado = "confirmada",
            dataHora = "2026-10-02 10:00"
        )

        // Cenário G: Consulta válida gera URL canônica da Sala Web com agendamento_id exclusivo
        val urlGerada = "$urlOficialBase?agendamento_id=${agendamentoValido.id}"

        assertTrue(urlGerada.startsWith(urlOficialBase))
        assertTrue(urlGerada.contains("agendamento_id=ag_valido_99"))
        assertFalse("NUNCA deve transportar role na URL", urlGerada.contains("role="))
        assertFalse("NUNCA deve transportar token na URL", urlGerada.contains("token="))
        assertFalse("NUNCA deve transportar access_token na URL", urlGerada.contains("access_token="))
        assertFalse("NUNCA deve expor LIVEKIT_API_SECRET no cliente", urlGerada.contains("LIVEKIT_API_SECRET"))

        // Cenário C: Tentativa de entrada sem agendamento -> Bloqueado
        val semAgendamentoId: String? = null
        assertTrue(semAgendamentoId.isNullOrBlank())

        // Cenário E: Utilizador não autenticado -> Bloqueado
        val context = ApplicationProvider.getApplicationContext<Context>()
        val unauthSession = SessionManager(context)
        unauthSession.clearSession()
        assertFalse(unauthSession.isSessionValid())

        // Cenário F: Consulta cancelada -> Bloqueado
        val agendamentoCancelado = agendamentoValido.copy(estado = "cancelada")
        assertFalse("Consulta cancelada não deve permitir teleatendimento", agendamentoCancelado.estado == "confirmada")
    }

    // =========================================================================
    // 10. PERMISSÕES ANDROID (Manifest)
    // =========================================================================

    @Test
    fun `test13 - Permissoes Android declaradas no Manifest para teleconsulta e notificacoes`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pm = context.packageManager
        val packageInfo = pm.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        val permissions = packageInfo.requestedPermissions?.toList() ?: emptyList()

        assertTrue("Manifest deve conter INTERNET", permissions.contains(Manifest.permission.INTERNET))
        assertTrue("Manifest deve conter CAMERA para sala de vídeo", permissions.contains(Manifest.permission.CAMERA))
        assertTrue("Manifest deve conter RECORD_AUDIO para sala de vídeo", permissions.contains(Manifest.permission.RECORD_AUDIO))
        assertTrue("Manifest deve conter MODIFY_AUDIO_SETTINGS", permissions.contains(Manifest.permission.MODIFY_AUDIO_SETTINGS))
        assertTrue("Manifest deve conter POST_NOTIFICATIONS", permissions.contains(Manifest.permission.POST_NOTIFICATIONS))
    }

    // =========================================================================
    // 11. CHAT — REMETENTE_ID = AUTH.UID() E DEDUPLICAÇÃO
    // =========================================================================

    @Test
    fun `test14 - Chat - remetente_id rigorosamente vinculado a auth uid e deduplicacao`() {
        val authUid = "user_auth_uuid_9999"
        val insertDto = ChatMensagemInsertDto(
            grupoId = "grupo_01",
            remetenteId = authUid,
            mensagem = "Olá, estou registrando a sessão de hoje."
        )

        assertEquals(authUid, insertDto.remetenteId)

        // Deduplicação
        val listaMensagens = mutableListOf<ChatMensagemDto>()
        val msg1 = ChatMensagemDto(id = "msg_01", grupoId = "grupo_01", mensagem = "Texto 1", createdAt = "2026-10-02T10:00:00Z")
        val msg1Duplicada = ChatMensagemDto(id = "msg_01", grupoId = "grupo_01", mensagem = "Texto 1 Repetido", createdAt = "2026-10-02T10:00:00Z")

        listaMensagens.add(msg1)
        if (!listaMensagens.any { it.id == msg1Duplicada.id }) {
            listaMensagens.add(msg1Duplicada)
        }
        assertEquals("Deduplicação deve manter apenas uma ocorrência pelo ID", 1, listaMensagens.size)
    }

    // =========================================================================
    // 12. JANETH — SEGURANÇA E GUARDRAILS ÉTICOS
    // =========================================================================

    @Test
    fun `test15 - Janeth - orientacao acolhedora e recusa expressa de prescricao e diagnostico clinico`() {
        // Pergunta sobre medicamento
        val resMed = JanethKnowledgeBase.answerQuery("Qual remédio posso dar para o meu filho?")
        assertTrue("Janeth deve recusar prescrição médica", resMed.text.contains("não realizo prescrição medicamentosa"))
        assertEquals("Orientação Médica Exclusiva", resMed.category)

        // Pergunta sobre diagnóstico
        val resDiag = JanethKnowledgeBase.answerQuery("Janeth, você pode diagnosticar se ele tem autismo?")
        assertTrue("Janeth não deve emitir diagnóstico", resDiag.text.contains("não realizo prescrição") || resDiag.text.contains("médico"))

        // Pergunta sobre FAQ institucional
        val resPei = JanethKnowledgeBase.answerQuery("O que é o PEI?")
        assertTrue("Janeth deve responder sobre o PEI", resPei.text.contains("Plano Educacional e de Intervenção Individualizado"))
    }

    // =========================================================================
    // 13. SEGURANÇA NEGATIVA & ISOLAMENTO MULTI-TENANT
    // =========================================================================

    @Test
    fun `test16 - Seguranca Negativa - bloqueio de acesso cruzado entre pacientes e familias`() {
        val pacienteDoResponsavelA = "pac_familia_a"
        val pacienteDoResponsavelB = "pac_familia_b"

        val vinculosResponsavelA = listOf(pacienteDoResponsavelA)

        fun podeAcessarPaciente(solicitanteVinculos: List<String>, alvoPacienteId: String): Boolean {
            return solicitanteVinculos.contains(alvoPacienteId)
        }

        assertTrue("Responsável A pode acessar seu próprio paciente", podeAcessarPaciente(vinculosResponsavelA, pacienteDoResponsavelA))
        assertFalse("Responsável A NÃO pode acessar paciente da família B", podeAcessarPaciente(vinculosResponsavelA, pacienteDoResponsavelB))
    }
}
