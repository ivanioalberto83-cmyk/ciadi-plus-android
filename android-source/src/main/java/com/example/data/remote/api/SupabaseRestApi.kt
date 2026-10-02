package com.example.data.remote.api

import com.example.data.remote.dto.AcompanhamentoDiarioAbaDto
import com.example.data.remote.dto.AgendamentoDto
import com.example.data.remote.dto.AtAtividadeDto
import com.example.data.remote.dto.AtCalendarioEventoDto
import com.example.data.remote.dto.AtContactoDto
import com.example.data.remote.dto.AtIncidenteDto
import com.example.data.remote.dto.AtOrganizacaoDto
import com.example.data.remote.dto.AtRegistoObjetivoDto
import com.example.data.remote.dto.AtSessaoDto
import com.example.data.remote.dto.AtSupervisaoDto
import com.example.data.remote.dto.AtribuicaoAtDto
import com.example.data.remote.dto.ChatGrupoDto
import com.example.data.remote.dto.ChatMembroDto
import com.example.data.remote.dto.ChatMensagemDto
import com.example.data.remote.dto.ChatMensagemInsertDto
import com.example.data.remote.dto.DocumentoClinicoDto
import com.example.data.remote.dto.EspecialidadeDto
import com.example.data.remote.dto.FormularioClinicoDto
import com.example.data.remote.dto.NotificacaoDto
import com.example.data.remote.dto.PacienteDto
import com.example.data.remote.dto.PerfilDto
import com.example.data.remote.dto.ProfissionalDto
import com.example.data.remote.dto.VideoSalaResponseDto
import com.example.data.remote.dto.VinculoFamiliarDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

/**
 * Interface Retrofit oficial para as tabelas do PostgreSQL no Supabase do CIADI.
 *
 * Todas as chamadas para esta interface passam pelo SupabaseAuthInterceptor,
 * transmitindo o token JWT no cabeçalho Authorization.
 * A segurança e filtragem é garantida pelo Row Level Security (RLS) do PostgreSQL.
 */
interface SupabaseRestApi {

    // --- 1. Perfis (tabela: perfis) ---
    @GET("rest/v1/perfis")
    suspend fun getPerfis(
        @Header("Authorization") authHeader: String? = null,
        @Query("select") select: String = "*",
        @Query("id") idFilter: String? = null
    ): Response<List<PerfilDto>>

    // --- 2. Pacientes / Crianças (tabela: pacientes) ---
    @GET("rest/v1/pacientes")
    suspend fun getPacientes(
        @Query("select") select: String = "*",
        @Query("id") idFilter: String? = null,
        @Query("order") order: String = "nome.asc"
    ): Response<List<PacienteDto>>

    // --- 3. Vínculos Familiares (tabela: ciadi_vinculos_familiares) ---
    @GET("rest/v1/ciadi_vinculos_familiares")
    suspend fun getVinculosFamiliares(
        @Query("select") select: String = "*,pacientes(*)",
        @Query("perfil_id") perfilIdFilter: String? = null
    ): Response<List<VinculoFamiliarDto>>

    // --- 4. Atribuições de A.T. (tabela: ciadi_atribuicoes_at) ---
    @GET("rest/v1/ciadi_atribuicoes_at")
    suspend fun getAtribuicoesAt(
        @Query("select") select: String = "*,pacientes(*)",
        @Query("profissional_id") profissionalIdFilter: String? = null,
        @Query("ativo") ativoFilter: String? = "eq.true"
    ): Response<List<AtribuicaoAtDto>>

    // --- 5. Sessões de A.T. (tabela: ciadi_at_sessoes) ---
    @GET("rest/v1/ciadi_at_sessoes")
    suspend fun getAtSessoes(
        @Query("select") select: String = "*",
        @Query("atribuicao_id") atribuicaoIdFilter: String? = null,
        @Query("order") order: String = "data_sessao.desc"
    ): Response<List<AtSessaoDto>>

    @POST("rest/v1/ciadi_at_sessoes")
    suspend fun createAtSessao(
        @Body sessao: AtSessaoDto
    ): Response<List<AtSessaoDto>>

    // --- 6. Registros de Objetivos do P.E.I. em Sessão (tabela: ciadi_at_registos_objetivos) ---
    @GET("rest/v1/ciadi_at_registos_objetivos")
    suspend fun getAtRegistosObjetivos(
        @Query("select") select: String = "*",
        @Query("sessao_id") sessaoIdFilter: String? = null
    ): Response<List<AtRegistoObjetivoDto>>

    @POST("rest/v1/ciadi_at_registos_objetivos")
    suspend fun createAtRegistoObjetivo(
        @Body registro: AtRegistoObjetivoDto
    ): Response<List<AtRegistoObjetivoDto>>

    // --- 7. Incidentes e Ocorrências de A.T. (tabela: ciadi_at_incidentes) ---
    @GET("rest/v1/ciadi_at_incidentes")
    suspend fun getAtIncidentes(
        @Query("select") select: String = "*",
        @Query("sessao_id") sessaoIdFilter: String? = null,
        @Query("order") order: String = "criado_em.desc"
    ): Response<List<AtIncidenteDto>>

    @POST("rest/v1/ciadi_at_incidentes")
    suspend fun createAtIncidente(
        @Body incidente: AtIncidenteDto
    ): Response<List<AtIncidenteDto>>

    // --- 8. Supervisões de A.T. (tabela: ciadi_at_supervisoes) ---
    @GET("rest/v1/ciadi_at_supervisoes")
    suspend fun getAtSupervisoes(
        @Query("select") select: String = "*",
        @Query("profissional_at_id") profissionalAtIdFilter: String? = null,
        @Query("order") order: String = "data_supervisao.desc"
    ): Response<List<AtSupervisaoDto>>

    // --- 9. Profissionais (tabela: profissionais) ---
    @GET("rest/v1/profissionais")
    suspend fun getProfissionais(
        @Query("select") select: String = "*",
        @Query("perfil_id") perfilIdFilter: String? = null,
        @Query("ativo") ativoFilter: String? = null,
        @Query("order") order: String = "nome_completo.asc"
    ): Response<List<ProfissionalDto>>

    // --- 10. Agenda / Agendamentos (tabela: agendamentos) ---
    @GET("rest/v1/agendamentos")
    suspend fun getAgendamentos(
        @Query("select") select: String = "*",
        @Query("paciente_id") pacienteIdFilter: String? = null,
        @Query("profissional_id") profissionalIdFilter: String? = null,
        @Query("order") order: String = "data_hora.asc"
    ): Response<List<AgendamentoDto>>

    @POST("rest/v1/agendamentos")
    suspend fun createAgendamento(
        @Body agendamento: AgendamentoDto
    ): Response<List<AgendamentoDto>>

    // --- 11. Especialidades (tabela: especialidades) ---
    @GET("rest/v1/especialidades")
    suspend fun getEspecialidades(
        @Query("select") select: String = "*",
        @Query("ativo") ativoFilter: String? = "eq.true"
    ): Response<List<EspecialidadeDto>>

    // --- 12. Formulários Clínicos (tabela: ciadi_formularios_clinicos) ---
    @GET("rest/v1/ciadi_formularios_clinicos")
    suspend fun getFormulariosClinicos(
        @Query("select") select: String = "*",
        @Query("ativo") ativoFilter: String? = "eq.true",
        @Query("order") order: String = "titulo.asc"
    ): Response<List<FormularioClinicoDto>>

    // --- 12.1 Avaliações clínicas preenchidas ---
    @Headers("Prefer: return=representation")
    @POST("rest/v1/ciadi_avaliacoes_clinicas")
    suspend fun createAvaliacaoClinica(
        @Body avaliacao: com.example.data.remote.dto.AvaliacaoClinicaCreateDto
    ): Response<List<com.example.data.remote.dto.AvaliacaoClinicaDto>>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/ciadi_avaliacoes_respostas")
    suspend fun createAvaliacaoResposta(
        @Body resposta: com.example.data.remote.dto.AvaliacaoRespostaCreateDto
    ): Response<List<com.example.data.remote.dto.AvaliacaoRespostaCreateDto>>

    // --- 12.2 Denúncias de bullying/proteção ---
    @Headers("Prefer: return=representation")
    @POST("rest/v1/ciadi_sos_denuncias")
    suspend fun createBullyingDenuncia(
        @Body denuncia: com.example.data.remote.dto.BullyingDenunciaCreateDto
    ): Response<List<com.example.data.remote.dto.BullyingDenunciaCreateDto>>

    // --- 13. Documentos Clínicos / Laudos / P.E.I. (tabela: documentos_clinicos) ---
    @GET("rest/v1/documentos_clinicos")
    suspend fun getDocumentosClinicos(
        @Query("select") select: String = "*",
        @Query("paciente_id") pacienteIdFilter: String? = null,
        @Query("estado") estadoFilter: String? = null,
        @Query("order") order: String = "criado_em.desc"
    ): Response<List<DocumentoClinicoDto>>

    @POST("rest/v1/documentos_clinicos")
    suspend fun createDocumentoClinico(
        @Body documento: DocumentoClinicoDto
    ): Response<List<DocumentoClinicoDto>>

    // --- 13.1 Histórico de Documentos (tabela: ciadi_documentos_clinicos_historico) ---
    @GET("rest/v1/ciadi_documentos_clinicos_historico")
    suspend fun getDocumentoHistorico(
        @Query("select") select: String = "*",
        @Query("documento_id") documentoIdFilter: String? = null,
        @Query("order") order: String = "data_registro.desc"
    ): Response<List<com.example.data.remote.dto.DocumentoHistoricoDto>>

    // --- 14. Chat: Grupos (tabela: ciadi_chat_grupos) ---
    @GET("rest/v1/ciadi_chat_grupos")
    suspend fun getChatGrupos(
        @Query("select") select: String = "*",
        @Query("order") order: String? = null
    ): Response<List<ChatGrupoDto>>

    // --- Chat: Membros (tabela: ciadi_chat_membros) ---
    @GET("rest/v1/ciadi_chat_membros")
    suspend fun getChatMembros(
        @Query("select") select: String = "*",
        @Query("grupo_id") grupoIdFilter: String? = null,
        @Query("perfil_id") perfilIdFilter: String? = null
    ): Response<List<ChatMembroDto>>

    // --- 15. Chat: Mensagens (tabela: ciadi_chat_mensagens) ---
    @GET("rest/v1/ciadi_chat_mensagens")
    suspend fun getChatMensagens(
        @Query("select") select: String = "*",
        @Query("grupo_id") grupoIdFilter: String? = null,
        @Query("order") order: String = "created_at.asc"
    ): Response<List<ChatMensagemDto>>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/ciadi_chat_mensagens")
    suspend fun sendChatMensagem(
        @Body mensagem: ChatMensagemInsertDto
    ): Response<List<ChatMensagemDto>>

    // --- 16. Notificações (tabela: notificacoes) ---
    @GET("rest/v1/notificacoes")
    suspend fun getNotificacoes(
        @Query("select") select: String = "*",
        @Query("order") order: String = "criado_em.desc"
    ): Response<List<NotificacaoDto>>

    @PATCH("rest/v1/notificacoes")
    suspend fun markNotificacaoRead(
        @Query("id") idFilter: String,
        @Body body: Map<String, Boolean>
    ): Response<List<NotificacaoDto>>

    // --- 17. SOS CIADI+ (RPC: ciadi_criar_alerta_sos) ---
    @POST("rest/v1/rpc/ciadi_criar_alerta_sos")
    suspend fun criarAlertaSos(
        @Body request: com.example.data.remote.dto.CriarAlertaSosRequest
    ): Response<okhttp3.ResponseBody>

    // --- 18. Agenda Autorizada do Portal (RPC: ciadi_portal_agendamentos_autorizados) ---
    @POST("rest/v1/rpc/ciadi_portal_agendamentos_autorizados")
    suspend fun getPortalAgendamentosAutorizados(
        @Body body: Map<String, String>
    ): Response<List<com.example.data.remote.dto.PortalAgendamentoDto>>

    // --- 19. Clínica Virtual (RPCs de Vídeo) ---
    @POST("rest/v1/rpc/ciadi_preparar_sala_video")
    suspend fun prepararSalaVideo(
        @Body request: com.example.data.remote.dto.VideoSalaRequest
    ): Response<okhttp3.ResponseBody>

    @POST("rest/v1/rpc/ciadi_registrar_entrada_video")
    suspend fun registrarEntradaVideo(
        @Body request: com.example.data.remote.dto.VideoSalaRequest
    ): Response<okhttp3.ResponseBody>

    @POST("rest/v1/rpc/ciadi_registrar_saida_video")
    suspend fun registrarSaidaVideo(
        @Body request: com.example.data.remote.dto.VideoSalaRequest
    ): Response<okhttp3.ResponseBody>

    @POST("rest/v1/rpc/ciadi_encerrar_sala_video")
    suspend fun encerrarSalaVideo(
        @Body request: com.example.data.remote.dto.VideoSalaRequest
    ): Response<okhttp3.ResponseBody>

    // --- 20. Efemérides Oficiais (tabela: ciadi_efemerides) ---
    @GET("rest/v1/ciadi_efemerides")
    suspend fun getEfemerides(
        @Query("select") select: String = "*",
        @Query("ativo") ativoFilter: String? = "eq.true",
        @Query("order") order: String = "data_evento.asc"
    ): Response<List<com.example.data.remote.dto.EfemerideDto>>

    // --- 21. Aniversários & Lembretes (tabela: ciadi_aniversarios) ---
    @GET("rest/v1/ciadi_aniversarios")
    suspend fun getAniversarios(
        @Query("select") select: String = "*",
        @Query("mostrar_no_calendario") mostrarFilter: String? = "eq.true",
        @Query("order") order: String = "data_aniversario.asc"
    ): Response<List<com.example.data.remote.dto.AniversarioDto>>

    // --- 22. Sistema de Popups & Comunicados (tabelas: ciadi_popups, ciadi_anuncios) ---
    @GET("rest/v1/ciadi_popups")
    suspend fun getPopups(
        @Query("select") select: String = "*",
        @Query("ativo") ativoFilter: String? = "eq.true"
    ): Response<List<com.example.data.remote.dto.PopupComunicadoDto>>

    @GET("rest/v1/ciadi_anuncios")
    suspend fun getAnuncios(
        @Query("select") select: String = "*",
        @Query("ativo") ativoFilter: String? = "eq.true"
    ): Response<List<com.example.data.remote.dto.PopupComunicadoDto>>

    // --- 23. Coleção de 11 Modelos de Documentos Oficiais (tabela: ciadi_modelos_documentos) ---
    @GET("rest/v1/ciadi_modelos_documentos")
    suspend fun getModelosDocumentos(
        @Query("select") select: String = "*",
        @Query("estado") estadoFilter: String? = "eq.ATIVO",
        @Query("order") order: String = "codigo.asc"
    ): Response<List<com.example.data.remote.dto.ModeloDocumentoDto>>

    // --- 24. Registro de Auditoria do Ecossistema (tabela: ciadi_auditoria) ---
    @POST("rest/v1/ciadi_auditoria")
    suspend fun registrarAuditoria(
        @Body registro: com.example.data.remote.dto.AuditoriaRegistroDto
    ): Response<okhttp3.ResponseBody>

    // --- 25. Atividades de A.T. (tabela: ciadi_at_atividades) ---
    @GET("rest/v1/ciadi_at_atividades")
    suspend fun getAtAtividades(
        @Query("select") select: String = "*",
        @Query("profissional_at_id") profissionalAtIdFilter: String? = null,
        @Query("paciente_id") pacienteIdFilter: String? = null,
        @Query("order") order: String = "data_atividade.desc"
    ): Response<List<AtAtividadeDto>>

    @POST("rest/v1/ciadi_at_atividades")
    suspend fun createAtAtividade(
        @Body atividade: AtAtividadeDto
    ): Response<List<AtAtividadeDto>>

    // --- 26. Calendário Unificado A.T. (view: v_ciadi_at_calendario) ---
    @GET("rest/v1/v_ciadi_at_calendario")
    suspend fun getAtCalendario(
        @Query("select") select: String = "*",
        @Query("order") order: String = "data.asc"
    ): Response<List<AtCalendarioEventoDto>>

    // --- 27. View Atividades A.T. (view: v_ciadi_at_atividades) ---
    @GET("rest/v1/v_ciadi_at_atividades")
    suspend fun getVAtAtividades(
        @Query("select") select: String = "*",
        @Query("order") order: String = "data_atividade.desc"
    ): Response<List<AtAtividadeDto>>

    // --- 28. Contactos Autorizados A.T. (view: v_ciadi_at_contactos) ---
    @GET("rest/v1/v_ciadi_at_contactos")
    suspend fun getAtContactos(
        @Query("select") select: String = "*",
        @Query("order") order: String = "nome.asc"
    ): Response<List<AtContactoDto>>

    // --- 29. Organização A.T. (view: v_ciadi_at_organizacao) ---
    @GET("rest/v1/v_ciadi_at_organizacao")
    suspend fun getAtOrganizacao(
        @Query("select") select: String = "*",
        @Query("order") order: String = "nome.asc"
    ): Response<List<AtOrganizacaoDto>>

    // --- 30. Acompanhamentos Diários ABA (tabela: ciadi_acompanhamentos_diarios_aba) ---
    @GET("rest/v1/ciadi_acompanhamentos_diarios_aba")
    suspend fun getAcompanhamentosAba(
        @Query("select") select: String = "*",
        @Query("paciente_id") pacienteIdFilter: String? = null,
        @Query("profissional_id") profissionalIdFilter: String? = null,
        @Query("order") order: String = "data_registro.desc"
    ): Response<List<AcompanhamentoDiarioAbaDto>>

    @POST("rest/v1/ciadi_acompanhamentos_diarios_aba")
    suspend fun createAcompanhamentoAba(
        @Body registro: AcompanhamentoDiarioAbaDto
    ): Response<List<AcompanhamentoDiarioAbaDto>>

    // --- 31. RPC Abrir Chat Privado (função: ciadi_chat_abrir_privado) ---
    @POST("rest/v1/rpc/ciadi_chat_abrir_privado")
    suspend fun abrirChatPrivado(
        @Body request: Map<String, @JvmSuppressWildcards Any?>
    ): Response<okhttp3.ResponseBody>

    // --- 32. Edge Function: ciadi-video-token (LiveKit) ---
    @POST("functions/v1/ciadi-video-token")
    suspend fun getVideoToken(
        @Body body: Map<String, String>
    ): Response<VideoSalaResponseDto>

    // --- Consulta genérica com RLS ---
    @GET("rest/v1/{table}")
    suspend fun queryTable(
        @Path("table") table: String,
        @QueryMap filters: Map<String, String>
    ): Response<okhttp3.ResponseBody>
}
