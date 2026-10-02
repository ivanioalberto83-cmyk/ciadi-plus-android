package com.example.data.repository

import android.util.Log
import com.example.core.config.SupabaseConfig
import com.example.core.network.SupabaseErrorHandler
import com.example.core.session.SessionManager
import com.example.data.remote.client.SupabaseClientFactory
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
import com.example.data.remote.dto.ChatMensagemDto
import com.example.data.remote.dto.ChatMensagemInsertDto
import com.example.data.remote.dto.DocumentoClinicoDto
import com.example.data.remote.dto.DocumentoHistoricoDto
import com.example.data.remote.dto.FormularioClinicoDto
import com.example.data.remote.dto.NotificacaoDto
import com.example.data.remote.dto.PacienteDto
import com.example.data.remote.dto.PerfilDto
import com.example.data.remote.dto.PopupComunicadoDto
import com.example.data.remote.dto.ProfissionalDto
import com.example.data.remote.dto.VinculoFamiliarDto
import com.example.data.remote.realtime.SupabaseRealtimeManager
import com.example.domain.repository.ATRepository
import com.example.domain.repository.AgendaRepository
import com.example.domain.repository.ChatRepository
import com.example.domain.repository.ClinicalDocumentsRepository
import com.example.domain.repository.ClinicalFormsRepository
import com.example.domain.repository.FamilyRepository
import com.example.domain.repository.ModulesRepository
import com.example.domain.repository.NotificationsRepository
import com.example.domain.repository.ProfileRepository
import com.example.domain.repository.SosRepository
import com.example.data.remote.dto.PortalAgendamentoDto
import com.example.data.remote.dto.SosAlertaResultDto
import com.example.data.remote.dto.VideoSalaResponseDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Implementação real dos módulos do CIADI conectada ao PostgreSQL do Supabase via PostgREST.
 *
 * Implementa tanto a interface agregada ModulesRepository quanto as interfaces especializadas
 * (FamilyRepository, ATRepository, AgendaRepository, ClinicalFormsRepository, ClinicalDocumentsRepository,
 * ChatRepository, NotificationsRepository, ProfileRepository).
 *
 * Todas as requisições utilizam o token JWT do usuário ativo (injetado pelo SupabaseAuthInterceptor),
 * de modo que o RLS filtre no próprio banco de dados as linhas correspondentes a cada perfil.
 */
class SupabaseModulesRepositoryImpl(
    private val sessionManager: SessionManager,
    private val clientFactory: SupabaseClientFactory = SupabaseClientFactory(sessionManager)
) : ModulesRepository,
    FamilyRepository,
    ATRepository,
    AgendaRepository,
    ClinicalFormsRepository,
    ClinicalDocumentsRepository,
    ChatRepository,
    NotificationsRepository,
    ProfileRepository,
    SosRepository {

    private val _vinculosFamiliares = MutableStateFlow<List<VinculoFamiliarDto>>(emptyList())
    private val _pacientes = MutableStateFlow<List<PacienteDto>>(emptyList())
    private val _selectedPatient = MutableStateFlow<PacienteDto?>(null)

    private val _atribuicoesAt = MutableStateFlow<List<AtribuicaoAtDto>>(emptyList())
    private val _atSessoes = MutableStateFlow<List<AtSessaoDto>>(emptyList())
    private val _atRegistosObjetivos = MutableStateFlow<List<AtRegistoObjetivoDto>>(emptyList())
    private val _atIncidentes = MutableStateFlow<List<AtIncidenteDto>>(emptyList())
    private val _atSupervisoes = MutableStateFlow<List<AtSupervisaoDto>>(emptyList())

    // Estruturas Oficiais A.T. (Prompt Mestre)
    private val realtimeManager = SupabaseRealtimeManager(clientFactory)
    private val _profissionalLogado = MutableStateFlow<ProfissionalDto?>(null)
    private val _assistidosAt = MutableStateFlow<List<PacienteDto>>(emptyList())
    private val _atAtividades = MutableStateFlow<List<AtAtividadeDto>>(emptyList())
    private val _atCalendario = MutableStateFlow<List<AtCalendarioEventoDto>>(emptyList())
    private val _atContactos = MutableStateFlow<List<AtContactoDto>>(emptyList())
    private val _atOrganizacao = MutableStateFlow<List<AtOrganizacaoDto>>(emptyList())
    private val _atAcompanhamentosAba = MutableStateFlow<List<AcompanhamentoDiarioAbaDto>>(emptyList())

    private val _agendamentos = MutableStateFlow<List<AgendamentoDto>>(emptyList())
    private val _portalAgendamentos = MutableStateFlow<List<PortalAgendamentoDto>>(emptyList())
    private val _formulariosClinicos = MutableStateFlow<List<FormularioClinicoDto>>(emptyList())
    private val _documentosClinicos = MutableStateFlow<List<DocumentoClinicoDto>>(emptyList())

    private val _chatGrupos = MutableStateFlow<List<ChatGrupoDto>>(emptyList())
    private val _chatMensagens = MutableStateFlow<Map<String, List<ChatMensagemDto>>>(emptyMap())
    private val groupMessagesFlows = java.util.concurrent.ConcurrentHashMap<String, MutableStateFlow<List<ChatMensagemDto>>>()

    private fun getOrCreateGroupFlow(grupoId: String): MutableStateFlow<List<ChatMensagemDto>> {
        return groupMessagesFlows.getOrPut(grupoId) {
            MutableStateFlow(_chatMensagens.value[grupoId].orEmpty())
        }
    }

    private fun updateGroupMessages(grupoId: String, messages: List<ChatMensagemDto>) {
        val sorted = messages
            .filter { it.id.isNotBlank() }
            .distinctBy { it.id }
            .sortedWith(compareBy({ it.createdAt.orEmpty() }, { it.id }))
        _chatMensagens.value = _chatMensagens.value + (grupoId to sorted)
        getOrCreateGroupFlow(grupoId).value = sorted
    }
    private val _notificacoes = MutableStateFlow<List<NotificacaoDto>>(emptyList())
    private val _currentProfile = MutableStateFlow<PerfilDto?>(null)

    // Calendário Inteligente & Popups & Modelos de Documentos Oficiais
    private val _efemerides = MutableStateFlow<List<com.example.data.remote.dto.EfemerideDto>>(emptyList())
    private val _aniversarios = MutableStateFlow<List<com.example.data.remote.dto.AniversarioDto>>(emptyList())
    private val _popups = MutableStateFlow<List<com.example.data.remote.dto.PopupComunicadoDto>>(emptyList())
    private val _modelosDocumentos = MutableStateFlow<List<com.example.data.remote.dto.ModeloDocumentoDto>>(
        listOf(
            com.example.data.remote.dto.ModeloDocumentoDto("1", "REL_AVAL_PSICOPED", "Relatório de Avaliação Psicopedagógica", "Avaliação"),
            com.example.data.remote.dto.ModeloDocumentoDto("2", "PLANO_DESENV_INDIVIDUAL", "Plano Educacional Individualizado (P.E.I.)", "Planeamento"),
            com.example.data.remote.dto.ModeloDocumentoDto("3", "REL_EVOLUCAO_CLINICA", "Relatório de Evolução Clínica Multidisciplinar", "Acompanhamento"),
            com.example.data.remote.dto.ModeloDocumentoDto("4", "LAUDO_NEUROPSICOLOGICO", "Laudo de Avaliação Neuropsicológica", "Avaliação"),
            com.example.data.remote.dto.ModeloDocumentoDto("5", "REL_FONOAUDIOLOGICO", "Relatório de Triagem e Terapia Fonoaudiológica", "Fonoaudiologia"),
            com.example.data.remote.dto.ModeloDocumentoDto("6", "REL_TERAPIA_OCUPACIONAL", "Relatório de Terapia Ocupacional & Sensorial", "Terapia"),
            com.example.data.remote.dto.ModeloDocumentoDto("7", "PLANO_ACOMPANHAMENTO_AT", "Plano de Intervenção em Campo do A.T.", "Acompanhamento"),
            com.example.data.remote.dto.ModeloDocumentoDto("8", "REGISTRO_SESSAO_CAMPO", "Ficha de Registro de Sessão Escolar/Rotina", "A.T."),
            com.example.data.remote.dto.ModeloDocumentoDto("9", "REL_ENCAMINHAMENTO_MEDICO", "Declaração e Encaminhamento Clínico", "Encaminhamento"),
            com.example.data.remote.dto.ModeloDocumentoDto("10", "DECLARACAO_COMPARECIMENTO", "Declaração de Comparecimento e Frequência", "Declarações"),
            com.example.data.remote.dto.ModeloDocumentoDto("11", "RELATORIO_ALTA_CLINICA", "Relatório de Alta e Transição Terapêutica", "Alta")
        )
    )
    private val _adminStats = MutableStateFlow(com.example.domain.repository.AdminStatsState())

    override fun observeEfemerides(): Flow<List<com.example.data.remote.dto.EfemerideDto>> = _efemerides.asStateFlow()
    override fun observeAniversarios(): Flow<List<com.example.data.remote.dto.AniversarioDto>> = _aniversarios.asStateFlow()
    override fun observePopups(): Flow<List<com.example.data.remote.dto.PopupComunicadoDto>> = _popups.asStateFlow()
    override fun observeModelosDocumentos(): Flow<List<com.example.data.remote.dto.ModeloDocumentoDto>> = _modelosDocumentos.asStateFlow()
    override fun observeAdminStats(): StateFlow<com.example.domain.repository.AdminStatsState> = _adminStats.asStateFlow()

    // --- ModulesRepository / FamilyRepository Impl ---
    override fun observeCriancasVinculadas(): Flow<List<VinculoFamiliarDto>> = _vinculosFamiliares.asStateFlow()
    override fun observePacientes(): Flow<List<PacienteDto>> = _pacientes.asStateFlow()

    override fun observeVinculos(): StateFlow<List<VinculoFamiliarDto>> = _vinculosFamiliares.asStateFlow()
    override fun observeAuthorizedPatients(): StateFlow<List<PacienteDto>> = _pacientes.asStateFlow()
    override fun observeSelectedPatient(): StateFlow<PacienteDto?> = _selectedPatient.asStateFlow()

    override fun selectPatient(patient: PacienteDto) {
        _selectedPatient.value = patient
    }

    override suspend fun syncFamilyBonds(): Result<Unit> = sincronizarTabela(SupabaseConfig.Tables.VINCULOS_FAMILIARES)

    // --- ATRepository Impl ---
    override fun observeAtribuicoesAt(): Flow<List<AtribuicaoAtDto>> = _atribuicoesAt.asStateFlow()
    override fun observeAtSessoes(atribuicaoId: String?): Flow<List<AtSessaoDto>> = _atSessoes.asStateFlow()
    override fun observeAtRegistosObjetivos(sessaoId: String?): Flow<List<AtRegistoObjetivoDto>> = _atRegistosObjetivos.asStateFlow()
    override fun observeAtIncidentes(sessaoId: String?): Flow<List<AtIncidenteDto>> = _atIncidentes.asStateFlow()
    override fun observeAtSupervisoes(): Flow<List<AtSupervisaoDto>> = _atSupervisoes.asStateFlow()

    override fun observeAtribuicoes(): StateFlow<List<AtribuicaoAtDto>> = _atribuicoesAt.asStateFlow()
    override fun observeAssistidos(): StateFlow<List<PacienteDto>> = _assistidosAt.asStateFlow()
    override fun observeSessoes(): StateFlow<List<AtSessaoDto>> = _atSessoes.asStateFlow()
    override fun observeAtividades(): StateFlow<List<AtAtividadeDto>> = _atAtividades.asStateFlow()
    override fun observeCalendario(): StateFlow<List<AtCalendarioEventoDto>> = _atCalendario.asStateFlow()
    override fun observeContactos(): StateFlow<List<AtContactoDto>> = _atContactos.asStateFlow()
    override fun observeOrganizacao(): StateFlow<List<AtOrganizacaoDto>> = _atOrganizacao.asStateFlow()
    override fun observeAcompanhamentosAba(): StateFlow<List<AcompanhamentoDiarioAbaDto>> = _atAcompanhamentosAba.asStateFlow()
    override fun observeRegistosObjetivos(): StateFlow<List<AtRegistoObjetivoDto>> = _atRegistosObjetivos.asStateFlow()
    override fun observeIncidentes(): StateFlow<List<AtIncidenteDto>> = _atIncidentes.asStateFlow()
    override fun observeSupervisoes(): StateFlow<List<AtSupervisaoDto>> = _atSupervisoes.asStateFlow()
    override fun observeProfissionalLogado(): StateFlow<ProfissionalDto?> = _profissionalLogado.asStateFlow()

    override suspend fun registrarAtividade(atividade: AtAtividadeDto): Result<AtAtividadeDto> = withContext(Dispatchers.IO) {
        if (!clientFactory.isReadyForConnection()) {
            return@withContext Result.failure(IllegalStateException("Supabase não configurado."))
        }
        try {
            val response = clientFactory.restApi.createAtAtividade(atividade)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val created = response.body()!!.first()
                _atAtividades.value = listOf(created) + _atAtividades.value.filter { it.id != created.id }
                sincronizarCalendarioAt()
                Result.success(created)
            } else {
                val errorMsg = SupabaseErrorHandler.parseHttpErrorMessage(response.code())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun registrarSessao(sessao: AtSessaoDto): Result<AtSessaoDto> = registrarSessaoAt(sessao)
    override suspend fun registrarAcompanhamentoAba(registro: AcompanhamentoDiarioAbaDto): Result<AcompanhamentoDiarioAbaDto> = withContext(Dispatchers.IO) {
        if (!clientFactory.isReadyForConnection()) {
            return@withContext Result.failure(IllegalStateException("Supabase não configurado."))
        }
        try {
            val response = clientFactory.restApi.createAcompanhamentoAba(registro)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val created = response.body()!!.first()
                _atAcompanhamentosAba.value = listOf(created) + _atAcompanhamentosAba.value.filter { it.id != created.id }
                Result.success(created)
            } else {
                val errorMsg = SupabaseErrorHandler.parseHttpErrorMessage(response.code())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun registrarObjetivo(objetivo: AtRegistoObjetivoDto): Result<AtRegistoObjetivoDto> = registrarObjetivoAt(objetivo)
    override suspend fun registrarIncidente(incidente: AtIncidenteDto): Result<AtIncidenteDto> = registrarIncidenteAt(incidente)

    override suspend fun syncATData(): Result<Unit> = withContext(Dispatchers.IO) {
        if (!clientFactory.isReadyForConnection()) {
            return@withContext Result.failure(IllegalStateException("Supabase não configurado."))
        }
        try {
            // 1. Identificação do Profissional via auth.uid() e profissionais.ativo = true
            val authUserId = (sessionManager.sessionFlow.value as? com.example.domain.model.UserSession.Authenticated)?.user?.id ?: _currentProfile.value?.id
            var profissional = _profissionalLogado.value
            if (profissional == null && !authUserId.isNullOrBlank()) {
                val profResp = clientFactory.restApi.getProfissionais(
                    perfilIdFilter = "eq.$authUserId",
                    ativoFilter = "eq.true"
                )
                if (profResp.isSuccessful && !profResp.body().isNullOrEmpty()) {
                    profissional = profResp.body()!!.first()
                    _profissionalLogado.value = profissional
                }
            }

            // 2. Meus Assistidos: ciadi_atribuicoes_at.profissional_id = profissionais.id e ativo = true
            val profId = profissional?.id
            val atribResp = if (!profId.isNullOrBlank()) {
                clientFactory.restApi.getAtribuicoesAt(
                    select = "*,pacientes(*)",
                    profissionalIdFilter = "eq.$profId",
                    ativoFilter = "eq.true"
                )
            } else {
                clientFactory.restApi.getAtribuicoesAt(
                    select = "*,pacientes(*)",
                    ativoFilter = "eq.true"
                )
            }
            if (atribResp.isSuccessful) {
                val atribuicoes = atribResp.body().orEmpty()
                _atribuicoesAt.value = atribuicoes
                val assistidos = atribuicoes.mapNotNull { it.paciente ?: it.pacientes }
                _assistidosAt.value = assistidos
                if (_selectedPatient.value == null && assistidos.isNotEmpty()) {
                    _selectedPatient.value = assistidos.first()
                }
            }

            // 3. Acompanhamentos reais: ciadi_at_sessoes
            val sessResp = clientFactory.restApi.getAtSessoes()
            if (sessResp.isSuccessful) {
                _atSessoes.value = sessResp.body().orEmpty()
            }

            // 4. Incidentes, Supervisões e Acompanhamentos Diários ABA
            val incResp = clientFactory.restApi.getAtIncidentes()
            if (incResp.isSuccessful) {
                _atIncidentes.value = incResp.body().orEmpty()
            }

            val abaResp = clientFactory.restApi.getAcompanhamentosAba()
            if (abaResp.isSuccessful) {
                _atAcompanhamentosAba.value = abaResp.body().orEmpty()
            }

            // 5. Atividades, Calendário, Contactos e Organização
            sincronizarAtividades()
            sincronizarCalendarioAt()
            sincronizarContactos()
            sincronizarOrganizacao()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sincronizarAtividades(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val resp = clientFactory.restApi.getAtAtividades()
            if (resp.isSuccessful) {
                _atAtividades.value = resp.body().orEmpty()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sincronizarCalendarioAt(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val resp = clientFactory.restApi.getAtCalendario()
            if (resp.isSuccessful) {
                _atCalendario.value = resp.body().orEmpty()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sincronizarContactos(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val resp = clientFactory.restApi.getAtContactos()
            if (resp.isSuccessful) {
                _atContactos.value = resp.body().orEmpty()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sincronizarOrganizacao(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val resp = clientFactory.restApi.getAtOrganizacao()
            if (resp.isSuccessful) {
                _atOrganizacao.value = resp.body().orEmpty()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun registrarSessaoAt(sessao: AtSessaoDto): Result<AtSessaoDto> = withContext(Dispatchers.IO) {
        if (!clientFactory.isReadyForConnection()) {
            return@withContext Result.failure(IllegalStateException("Supabase não configurado."))
        }
        try {
            val response = clientFactory.restApi.createAtSessao(sessao)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val created = response.body()!!.first()
                _atSessoes.value = listOf(created) + _atSessoes.value
                Result.success(created)
            } else {
                val errorMsg = SupabaseErrorHandler.parseHttpErrorMessage(response.code())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun registrarIncidenteAt(incidente: AtIncidenteDto): Result<AtIncidenteDto> = withContext(Dispatchers.IO) {
        if (!clientFactory.isReadyForConnection()) {
            return@withContext Result.failure(IllegalStateException("Supabase não configurado."))
        }
        try {
            val response = clientFactory.restApi.createAtIncidente(incidente)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val created = response.body()!!.first()
                _atIncidentes.value = listOf(created) + _atIncidentes.value
                Result.success(created)
            } else {
                val errorMsg = SupabaseErrorHandler.parseHttpErrorMessage(response.code())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun registrarObjetivoAt(registro: AtRegistoObjetivoDto): Result<AtRegistoObjetivoDto> = withContext(Dispatchers.IO) {
        if (!clientFactory.isReadyForConnection()) {
            return@withContext Result.failure(IllegalStateException("Supabase não configurado."))
        }
        try {
            val response = clientFactory.restApi.createAtRegistoObjetivo(registro)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val created = response.body()!!.first()
                _atRegistosObjetivos.value = listOf(created) + _atRegistosObjetivos.value
                Result.success(created)
            } else {
                val errorMsg = SupabaseErrorHandler.parseHttpErrorMessage(response.code())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- AgendaRepository Impl ---
    override fun observeAgendamentos(pacienteId: String?, profissionalId: String?): Flow<List<AgendamentoDto>> = _agendamentos.asStateFlow()
    override fun observeAgendamentos(): StateFlow<List<AgendamentoDto>> = _agendamentos.asStateFlow()
    override fun observePortalAgendamentos(): StateFlow<List<PortalAgendamentoDto>> = _portalAgendamentos.asStateFlow()

    override suspend fun fetchPortalAgendamentos(pacienteId: String?): Result<List<PortalAgendamentoDto>> = withContext(Dispatchers.IO) {
        if (!clientFactory.isReadyForConnection()) {
            return@withContext Result.failure(IllegalStateException("Supabase não configurado."))
        }
        try {
            val body = if (pacienteId != null) mapOf("p_paciente_id" to pacienteId) else emptyMap()
            val resp = clientFactory.restApi.getPortalAgendamentosAutorizados(body)
            if (resp.isSuccessful) {
                val lista = resp.body().orEmpty()
                _portalAgendamentos.value = lista
                Result.success(lista)
            } else {
                // Fallback gracioso para a tabela regular agendamentos
                val regularResp = clientFactory.restApi.getAgendamentos(pacienteIdFilter = pacienteId?.let { "eq.$it" })
                if (regularResp.isSuccessful) {
                    val fallbackList = regularResp.body().orEmpty().map { a ->
                        PortalAgendamentoDto(
                            id = a.id,
                            paciente = a.pacienteNome,
                            pacienteId = a.pacienteId,
                            profissional = a.profissionalNome,
                            profissionalId = a.profissionalId,
                            especialidade = a.especialidadeNome,
                            dataHora = a.dataHora,
                            duracao = a.duracaoMinutos,
                            tipo = a.tipo ?: "Consulta",
                            modalidade = a.modalidade ?: "Presencial",
                            estado = a.estado ?: "agendada",
                            motivo = a.motivo,
                            observacoes = a.observacoes,
                            linkVideo = a.linkVideo,
                            salaNome = a.salaNome
                        )
                    }
                    _portalAgendamentos.value = fallbackList
                    Result.success(fallbackList)
                } else {
                    val errorMsg = SupabaseErrorHandler.parseHttpErrorMessage(resp.code())
                    Result.failure(Exception(errorMsg))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncAgenda(): Result<Unit> = sincronizarTabela(SupabaseConfig.Tables.AGENDAMENTOS)

    override suspend fun criarAgendamento(agendamento: AgendamentoDto): Result<AgendamentoDto> = withContext(Dispatchers.IO) {
        if (!clientFactory.isReadyForConnection()) {
            return@withContext Result.failure(IllegalStateException("Supabase não configurado."))
        }
        try {
            val response = clientFactory.restApi.createAgendamento(agendamento)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val created = response.body()!!.first()
                _agendamentos.value = _agendamentos.value + created
                Result.success(created)
            } else {
                val errorMsg = SupabaseErrorHandler.parseHttpErrorMessage(response.code())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Clínica Virtual (RPCs de Vídeo) ---
    override suspend fun prepararSalaVideo(agendamentoId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val resp = clientFactory.restApi.prepararSalaVideo(com.example.data.remote.dto.VideoSalaRequest(agendamentoId))
            if (resp.isSuccessful) {
                Result.success("sala-$agendamentoId")
            } else {
                Result.failure(Exception(SupabaseErrorHandler.parseHttpErrorMessage(resp.code())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun registrarEntradaVideo(agendamentoId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val resp = clientFactory.restApi.registrarEntradaVideo(com.example.data.remote.dto.VideoSalaRequest(agendamentoId))
            if (resp.isSuccessful) Result.success(Unit) else Result.failure(Exception(SupabaseErrorHandler.parseHttpErrorMessage(resp.code())))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun registrarSaidaVideo(agendamentoId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val resp = clientFactory.restApi.registrarSaidaVideo(com.example.data.remote.dto.VideoSalaRequest(agendamentoId))
            if (resp.isSuccessful) Result.success(Unit) else Result.failure(Exception(SupabaseErrorHandler.parseHttpErrorMessage(resp.code())))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun encerrarSalaVideo(agendamentoId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val resp = clientFactory.restApi.encerrarSalaVideo(com.example.data.remote.dto.VideoSalaRequest(agendamentoId))
            if (resp.isSuccessful) Result.success(Unit) else Result.failure(Exception(SupabaseErrorHandler.parseHttpErrorMessage(resp.code())))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- SosRepository Impl (RPC: ciadi_criar_alerta_sos) ---
    override suspend fun enviarAlertaSos(
        pacienteId: String,
        mensagem: String,
        latitude: Double?,
        longitude: Double?,
        localizacaoAutorizada: Boolean
    ): Result<SosAlertaResultDto> = withContext(Dispatchers.IO) {
        if (!clientFactory.isReadyForConnection()) {
            return@withContext Result.failure(IllegalStateException("Supabase não configurado."))
        }
        try {
            val req = com.example.data.remote.dto.CriarAlertaSosRequest(
                pacienteId = pacienteId,
                mensagem = mensagem,
                latitude = latitude,
                longitude = longitude,
                localizacaoAutorizada = localizacaoAutorizada
            )
            val resp = clientFactory.restApi.criarAlertaSos(req)
            if (resp.isSuccessful) {
                val protocoloGerado = "SOS-${System.currentTimeMillis() % 100000}"
                Result.success(
                    SosAlertaResultDto(
                        id = pacienteId,
                        sucesso = true,
                        mensagem = "Alerta transmitido à equipe responsável do CIADI.",
                        protocolo = protocoloGerado
                    )
                )
            } else {
                val errorMsg = SupabaseErrorHandler.parseHttpErrorMessage(resp.code())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- ClinicalFormsRepository Impl ---
    override fun observeFormulariosClinicos(): Flow<List<FormularioClinicoDto>> = _formulariosClinicos.asStateFlow()
    override fun observeFormularios(): StateFlow<List<FormularioClinicoDto>> = _formulariosClinicos.asStateFlow()
    override suspend fun syncFormularios(): Result<Unit> = sincronizarTabela(SupabaseConfig.Tables.FORMULARIOS_CLINICOS)

    // --- ClinicalDocumentsRepository Impl ---
    override fun observeDocumentosClinicos(pacienteId: String?): Flow<List<DocumentoClinicoDto>> = _documentosClinicos.asStateFlow()
    override fun observeDocumentos(): StateFlow<List<DocumentoClinicoDto>> = _documentosClinicos.asStateFlow()
    override suspend fun emitirDocumento(documento: DocumentoClinicoDto): Result<DocumentoClinicoDto> = emitirDocumentoClinico(documento)
    override suspend fun syncDocumentos(): Result<Unit> = sincronizarTabela(SupabaseConfig.Tables.DOCUMENTOS_CLINICOS)

    override suspend fun emitirDocumentoClinico(documento: DocumentoClinicoDto): Result<DocumentoClinicoDto> = withContext(Dispatchers.IO) {
        if (!clientFactory.isReadyForConnection()) {
            return@withContext Result.failure(IllegalStateException("Supabase não configurado."))
        }
        try {
            val response = clientFactory.restApi.createDocumentoClinico(documento)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val created = response.body()!!.first()
                _documentosClinicos.value = listOf(created) + _documentosClinicos.value
                Result.success(created)
            } else {
                val errorMsg = SupabaseErrorHandler.parseHttpErrorMessage(response.code())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getHistoricoDocumento(documentoId: String): Result<List<DocumentoHistoricoDto>> = withContext(Dispatchers.IO) {
        if (!clientFactory.isReadyForConnection()) {
            return@withContext Result.failure(IllegalStateException("Supabase não configurado."))
        }
        try {
            val response = clientFactory.restApi.getDocumentoHistorico(documentoIdFilter = "eq.$documentoId")
            if (response.isSuccessful) {
                Result.success(response.body().orEmpty())
            } else {
                val errorMsg = SupabaseErrorHandler.parseHttpErrorMessage(response.code())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- ChatRepository Impl ---
    override fun observeChatGrupos(): Flow<List<ChatGrupoDto>> = _chatGrupos.asStateFlow()
    override fun observeGrupos(): StateFlow<List<ChatGrupoDto>> = _chatGrupos.asStateFlow()
    override fun observeContactosAutorizados(): StateFlow<List<AtContactoDto>> = _atContactos.asStateFlow()
    override fun observeRealtimeStatus(grupoId: String): StateFlow<String> = realtimeManager.getStatusFlow(grupoId)

    override fun observeChatMensagens(grupoId: String): Flow<List<ChatMensagemDto>> {
        return getOrCreateGroupFlow(grupoId).asStateFlow()
    }
    override fun observeMensagens(grupoId: String): StateFlow<List<ChatMensagemDto>> {
        return getOrCreateGroupFlow(grupoId).asStateFlow()
    }
    override suspend fun syncChat(): Result<Unit> = sincronizarTabela(SupabaseConfig.Tables.CHAT_GRUPOS)
    override suspend fun syncContactos(): Result<Unit> = sincronizarContactos()

    override fun iniciarRealtime(grupoId: String) {
        val uuidRegex = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
        if (!grupoId.matches(uuidRegex)) {
            Log.w("CIADI_CHAT", "CHAT_GROUP: grupoId '$grupoId' não é um UUID válido para subscrição Realtime.")
            return
        }
        Log.d("CIADI_CHAT", "CHAT_REALTIME_STATUS: Iniciando escuta Realtime para grupo $grupoId")
        realtimeManager.startListening(grupoId) { novaMsg ->
            if (novaMsg.id.isBlank()) return@startListening
            val current = getOrCreateGroupFlow(grupoId).value
            if (current.any { it.id == novaMsg.id }) {
                Log.d("CIADI_CHAT", "CHAT_DUPLICATE_IGNORED: Mensagem ${novaMsg.id} já existe localmente")
            } else {
                Log.d("CIADI_CHAT", "CHAT_REALTIME_INSERT: Nova mensagem recebida via Realtime: ${novaMsg.id}")
                updateGroupMessages(grupoId, current + novaMsg)
            }
        }
    }

    override fun pararRealtime(grupoId: String) {
        realtimeManager.stopListening(grupoId)
    }

    override suspend fun carregarMensagensGrupo(grupoId: String): Result<List<ChatMensagemDto>> = withContext(Dispatchers.IO) {
        Log.d("CIADI_CHAT", "CHAT_SESSION: Validando conexão e sessão para carregar histórico do grupo $grupoId")
        if (!clientFactory.isReadyForConnection() || !sessionManager.isSessionValid()) {
            return@withContext Result.failure(IllegalStateException("Supabase não configurado ou sessão inválida."))
        }
        val uuidRegex = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
        if (!grupoId.matches(uuidRegex)) {
            Log.w("CIADI_CHAT", "CHAT_GROUP: grupoId '$grupoId' não é um UUID válido do Supabase. Ignorando consulta.")
            return@withContext Result.success(emptyList())
        }
        Log.d("CIADI_CHAT", "CHAT_GROUP: $grupoId")
        try {
            val resp = clientFactory.restApi.getChatMensagens(
                grupoIdFilter = "eq.$grupoId",
                order = "created_at.asc"
            )
            if (resp.isSuccessful) {
                val msgs = resp.body().orEmpty()
                Log.d("CIADI_CHAT", "CHAT_HISTORY_LOADED: ${msgs.size} mensagens carregadas do banco para grupo $grupoId")
                updateGroupMessages(grupoId, msgs)
                Result.success(msgs)
            } else {
                val errBody = resp.errorBody()?.string().orEmpty()
                Log.e("CIADI_CHAT", "CHAT_INSERT_ERROR: Falha HTTP ${resp.code()} ao carregar histórico: $errBody")
                val errorMsg = SupabaseErrorHandler.parseHttpErrorMessage(resp.code())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("CIADI_CHAT", "Erro ao carregar mensagens do grupo $grupoId: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun abrirConversaPrivada(destinatarioId: String, pacienteId: String?): Result<String> = withContext(Dispatchers.IO) {
        Log.d("CIADI_CHAT", "CHAT_SESSION: Abrindo conversa privada com $destinatarioId (paciente=$pacienteId)")
        if (!clientFactory.isReadyForConnection() || !sessionManager.isSessionValid()) {
            return@withContext Result.failure(IllegalStateException("Supabase não configurado ou sessão inválida."))
        }
        try {
            val params = mutableMapOf<String, Any?>("p_destinatario" to destinatarioId)
            if (!pacienteId.isNullOrBlank()) {
                params["p_paciente_id"] = pacienteId
            }
            val resp = clientFactory.restApi.abrirChatPrivado(params)
            if (resp.isSuccessful) {
                val rawBody = resp.body()?.string()?.trim().orEmpty()
                val uuidRegex = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
                val match = uuidRegex.find(rawBody)
                val grupoId = match?.value ?: rawBody.removeSurrounding("\"").trim()
                if (grupoId.isNotBlank() && grupoId.matches(uuidRegex)) {
                    Log.d("CIADI_CHAT", "CHAT_GROUP: Conversa privada associada com sucesso ao grupo $grupoId")
                    // 1. Confirmar membro (Item 2)
                    try {
                        val currentUid = sessionManager.getCurrentUserId()
                        if (!currentUid.isNullOrBlank()) {
                            clientFactory.restApi.getChatMembros(
                                grupoIdFilter = "eq.$grupoId",
                                perfilIdFilter = "eq.$currentUid"
                            )
                        }
                    } catch (e: Exception) {
                        Log.w("CIADI_CHAT", "Confirmação de membro aviso: ${e.message}")
                    }
                    // 2. Carregar histórico (Item 2)
                    carregarMensagensGrupo(grupoId)
                    // 3. Iniciar Realtime (Item 2)
                    iniciarRealtime(grupoId)
                    syncChat()
                    // 4. Permitir envio (Item 2)
                    Result.success(grupoId)
                } else {
                    Log.e("CIADI_CHAT", "CHAT_GROUP: RPC ciadi_chat_abrir_privado não retornou grupo_id válido: $rawBody")
                    Result.failure(Exception("Não foi possível obter o identificador da conversa no Supabase."))
                }
            } else {
                val errBody = resp.errorBody()?.string().orEmpty()
                Log.e("CIADI_CHAT", "CHAT_INSERT_ERROR: Falha ao chamar ciadi_chat_abrir_privado: HTTP ${resp.code()} - $errBody")
                val errorMsg = SupabaseErrorHandler.parseHttpErrorMessage(resp.code())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("CIADI_CHAT", "Exceção em ciadi_chat_abrir_privado: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun enviarMensagemTexto(grupoId: String, texto: String, userId: String): Result<ChatMensagemDto> = withContext(Dispatchers.IO) {
        val authUid = sessionManager.getCurrentUserId()?.takeIf { it.isNotBlank() } ?: userId
        Log.d("CIADI_CHAT", "CHAT_SESSION: Validando sessão do remetente $authUid para grupo $grupoId")
        if (!clientFactory.isReadyForConnection() || !sessionManager.isSessionValid()) {
            Log.e("CIADI_CHAT", "CHAT_INSERT_ERROR: Supabase não conectado ou sessão inválida")
            return@withContext Result.failure(IllegalStateException("Não foi possível enviar a mensagem. Verifique a ligação e tente novamente."))
        }
        val uuidRegex = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
        if (grupoId.isBlank() || !grupoId.matches(uuidRegex) || texto.isBlank() || authUid.isBlank()) {
            Log.e("CIADI_CHAT", "CHAT_INSERT_ERROR: Parâmetros insuficientes ou inválidos (grupo=$grupoId, texto=${texto.length}, authUid=$authUid)")
            return@withContext Result.failure(IllegalArgumentException("Não foi possível enviar a mensagem. Verifique a ligação e tente novamente."))
        }

        Log.d("CIADI_CHAT", "CHAT_GROUP: $grupoId")
        Log.d("CIADI_CHAT", "CHAT_INSERT_START: Iniciando INSERT de mensagem em ciadi_chat_mensagens (remetente=$authUid)")

        try {
            val insertDto = ChatMensagemInsertDto(
                grupoId = grupoId,
                remetenteId = authUid,
                mensagem = texto.trim()
            )
            val resp = clientFactory.restApi.sendChatMensagem(insertDto)
            if (resp.isSuccessful) {
                val createdList = resp.body().orEmpty()
                if (createdList.isNotEmpty()) {
                    val created = createdList.first()
                    Log.d("CIADI_CHAT", "CHAT_INSERT_SUCCESS: Mensagem gravada no Supabase com sucesso. ID=${created.id}, created_at=${created.createdAt}")
                    val current = getOrCreateGroupFlow(grupoId).value
                    if (current.none { it.id == created.id }) {
                        updateGroupMessages(grupoId, current + created)
                    }
                    Result.success(created)
                } else {
                    Log.e("CIADI_CHAT", "CHAT_INSERT_ERROR: Retorno vazio do Supabase no INSERT")
                    Result.failure(Exception("Não foi possível enviar a mensagem. Verifique a ligação e tente novamente."))
                }
            } else {
                val errBody = resp.errorBody()?.string().orEmpty()
                Log.e("CIADI_CHAT", "CHAT_INSERT_ERROR: Falha HTTP ${resp.code()} no INSERT: $errBody")
                Result.failure(Exception("Não foi possível enviar a mensagem. Verifique a ligação e tente novamente."))
            }
        } catch (e: Exception) {
            Log.e("CIADI_CHAT", "CHAT_INSERT_ERROR: Exceção durante envio: ${e.message}", e)
            Result.failure(Exception("Não foi possível enviar a mensagem. Verifique a ligação e tente novamente."))
        }
    }

    suspend fun obterVideoToken(agendamentoId: String): Result<VideoSalaResponseDto> = withContext(Dispatchers.IO) {
        try {
            val resp = clientFactory.restApi.getVideoToken(mapOf("agendamento_id" to agendamentoId))
            if (resp.isSuccessful && resp.body() != null) {
                Result.success(resp.body()!!)
            } else {
                Result.failure(Exception("Não foi possível obter o token de vídeo da Edge Function."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun enviarMensagem(mensagem: ChatMensagemDto): Result<ChatMensagemDto> = withContext(Dispatchers.IO) {
        val texto = mensagem.mensagem.ifBlank { mensagem.text }
        val remetente = mensagem.remetenteId ?: mensagem.senderId
        if (remetente.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Remetente não informado."))
        }
        val insertDto = ChatMensagemInsertDto(
            grupoId = mensagem.grupoId,
            remetenteId = remetente,
            mensagem = texto,
            anexoUrl = mensagem.anexoUrl,
            anexoNome = mensagem.anexoNome
        )
        Log.d("CIADI_CHAT", "CHAT_INSERT_START: Enviando mensagem objeto para grupo ${mensagem.grupoId}")
        try {
            val response = clientFactory.restApi.sendChatMensagem(insertDto)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val created = response.body()!!.first()
                Log.d("CIADI_CHAT", "CHAT_INSERT_SUCCESS: Mensagem persistida. ID=${created.id}")
                val current = getOrCreateGroupFlow(mensagem.grupoId).value
                if (current.none { it.id == created.id }) {
                    updateGroupMessages(mensagem.grupoId, current + created)
                }
                Result.success(created)
            } else {
                val errBody = response.errorBody()?.string().orEmpty()
                Log.e("CIADI_CHAT", "CHAT_INSERT_ERROR: Falha HTTP ${response.code()}: $errBody")
                Result.failure(Exception("Não foi possível enviar a mensagem. Verifique a ligação e tente novamente."))
            }
        } catch (e: Exception) {
            Log.e("CIADI_CHAT", "CHAT_INSERT_ERROR: Exceção: ${e.message}", e)
            Result.failure(Exception("Não foi possível enviar a mensagem. Verifique a ligação e tente novamente."))
        }
    }

    // --- NotificationsRepository Impl ---
    override fun observeNotificacoes(): StateFlow<List<NotificacaoDto>> = _notificacoes.asStateFlow()
    override suspend fun syncNotificacoes(): Result<Unit> = sincronizarTabela(SupabaseConfig.Tables.NOTIFICACOES)

    override suspend fun marcarNotificacaoLida(id: String): Result<Unit> = marcarLida(id)

    override suspend fun marcarLida(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!clientFactory.isReadyForConnection()) return@withContext Result.success(Unit)
        try {
            clientFactory.restApi.markNotificacaoRead(id, mapOf("lida" to true))
            _notificacoes.value = _notificacoes.value.map {
                if (it.id == id) it.copy(lida = true) else it
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- ProfileRepository Impl ---
    override fun observeCurrentProfile(): StateFlow<PerfilDto?> = _currentProfile.asStateFlow()

    override suspend fun fetchProfile(userId: String): Result<PerfilDto> = withContext(Dispatchers.IO) {
        if (!clientFactory.isReadyForConnection()) {
            return@withContext Result.failure(IllegalStateException("Supabase não configurado."))
        }
        try {
            val response = clientFactory.restApi.getPerfis(idFilter = "eq.$userId")
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val perfil = response.body()!!.first()
                _currentProfile.value = perfil
                Result.success(perfil)
            } else {
                val errorMsg = SupabaseErrorHandler.parseHttpErrorMessage(response.code())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Sincronização Geral com PostgREST ---
    override suspend fun sincronizarTabela(nomeTabela: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!clientFactory.isReadyForConnection()) {
            return@withContext Result.failure(
                IllegalStateException("Supabase aguardando Anon Key válida no arquivo .env")
            )
        }

        try {
            when (nomeTabela) {
                SupabaseConfig.Tables.VINCULOS_FAMILIARES -> {
                    val resp = clientFactory.restApi.getVinculosFamiliares()
                    if (resp.isSuccessful) {
                        val vinculos = resp.body().orEmpty()
                        _vinculosFamiliares.value = vinculos
                        val pacientesExtraidos = vinculos.mapNotNull { it.paciente }
                        _pacientes.value = pacientesExtraidos
                        // Regra CIADI+: Se houver exatamente 1 criança autorizada, apresenta diretamente.
                        // Se houver várias crianças, NÃO seleciona automaticamente para exigir confirmação do usuário.
                        if (pacientesExtraidos.size == 1) {
                            _selectedPatient.value = pacientesExtraidos.first()
                        } else if (pacientesExtraidos.isEmpty()) {
                            _selectedPatient.value = null
                        }
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception(SupabaseErrorHandler.parseHttpErrorMessage(resp.code())))
                    }
                }
                SupabaseConfig.Tables.PACIENTES -> {
                    val resp = clientFactory.restApi.getPacientes()
                    if (resp.isSuccessful) {
                        _pacientes.value = resp.body().orEmpty()
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception(SupabaseErrorHandler.parseHttpErrorMessage(resp.code())))
                    }
                }
                SupabaseConfig.Tables.ATRIBUICOES_AT -> {
                    val resp = clientFactory.restApi.getAtribuicoesAt()
                    if (resp.isSuccessful) {
                        _atribuicoesAt.value = resp.body().orEmpty()
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception(SupabaseErrorHandler.parseHttpErrorMessage(resp.code())))
                    }
                }
                SupabaseConfig.Tables.AT_SESSOES -> {
                    val resp = clientFactory.restApi.getAtSessoes()
                    if (resp.isSuccessful) {
                        _atSessoes.value = resp.body().orEmpty()
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception(SupabaseErrorHandler.parseHttpErrorMessage(resp.code())))
                    }
                }
                SupabaseConfig.Tables.AGENDAMENTOS -> {
                    val resp = clientFactory.restApi.getAgendamentos()
                    if (resp.isSuccessful) {
                        _agendamentos.value = resp.body().orEmpty()
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception(SupabaseErrorHandler.parseHttpErrorMessage(resp.code())))
                    }
                }
                SupabaseConfig.Tables.FORMULARIOS_CLINICOS -> {
                    val resp = clientFactory.restApi.getFormulariosClinicos()
                    if (resp.isSuccessful) {
                        _formulariosClinicos.value = resp.body().orEmpty()
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception(SupabaseErrorHandler.parseHttpErrorMessage(resp.code())))
                    }
                }
                SupabaseConfig.Tables.DOCUMENTOS_CLINICOS -> {
                    val resp = clientFactory.restApi.getDocumentosClinicos()
                    if (resp.isSuccessful) {
                        _documentosClinicos.value = resp.body().orEmpty()
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception(SupabaseErrorHandler.parseHttpErrorMessage(resp.code())))
                    }
                }
                SupabaseConfig.Tables.CHAT_GRUPOS -> {
                    val resp = clientFactory.restApi.getChatGrupos()
                    if (resp.isSuccessful) {
                        _chatGrupos.value = resp.body().orEmpty()
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception(SupabaseErrorHandler.parseHttpErrorMessage(resp.code())))
                    }
                }
                SupabaseConfig.Tables.NOTIFICACOES -> {
                    val resp = clientFactory.restApi.getNotificacoes()
                    if (resp.isSuccessful) {
                        _notificacoes.value = resp.body().orEmpty()
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception(SupabaseErrorHandler.parseHttpErrorMessage(resp.code())))
                    }
                }
                SupabaseConfig.Tables.AT_ATIVIDADES -> sincronizarAtividades()
                SupabaseConfig.Tables.V_AT_CALENDARIO -> sincronizarCalendarioAt()
                SupabaseConfig.Tables.V_AT_CONTACTOS -> sincronizarContactos()
                SupabaseConfig.Tables.V_AT_ORGANIZACAO -> sincronizarOrganizacao()
                SupabaseConfig.Tables.ACOMPANHAMENTOS_DIARIOS_ABA -> {
                    val resp = clientFactory.restApi.getAcompanhamentosAba()
                    if (resp.isSuccessful) {
                        _atAcompanhamentosAba.value = resp.body().orEmpty()
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception(SupabaseErrorHandler.parseHttpErrorMessage(resp.code())))
                    }
                }
                else -> Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sincronizarCalendarioInteligente(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val efResp = clientFactory.restApi.getEfemerides()
            if (efResp.isSuccessful) {
                _efemerides.value = efResp.body().orEmpty()
            }
            val anResp = clientFactory.restApi.getAniversarios()
            if (anResp.isSuccessful) {
                _aniversarios.value = anResp.body().orEmpty()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sincronizarPopups(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val resp = clientFactory.restApi.getPopups()
            if (resp.isSuccessful) {
                _popups.value = resp.body().orEmpty()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun criarPublicacaoPopup(popup: PopupComunicadoDto): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _popups.value = _popups.value + popup
            registrarAcaoAuditoria("CRIAR_PUBLICACAO_POPUP", "Título: ${popup.titulo}, Categoria: ${popup.categoria}")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun atualizarEstatisticasReais(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _adminStats.value = _adminStats.value.copy(isLoading = true)
            val pacResp = clientFactory.restApi.getPacientes(select = "id")
            val pacCount = if (pacResp.isSuccessful) pacResp.body()?.size ?: 0 else _pacientes.value.size

            val profResp = clientFactory.restApi.getProfissionais(select = "id")
            val profCount = if (profResp.isSuccessful) profResp.body()?.size ?: 0 else 0

            val atResp = clientFactory.restApi.getAtribuicoesAt(select = "id")
            val atCount = if (atResp.isSuccessful) atResp.body()?.size ?: 0 else _atribuicoesAt.value.size

            val agResp = clientFactory.restApi.getAgendamentos(select = "id")
            val agCount = if (agResp.isSuccessful) agResp.body()?.size ?: 0 else _agendamentos.value.size

            _adminStats.value = com.example.domain.repository.AdminStatsState(
                assistidosCount = pacCount,
                profissionaisCount = profCount,
                atAtivosCount = atCount,
                consultasCount = agCount,
                isLoading = false
            )
            Result.success(Unit)
        } catch (e: Exception) {
            _adminStats.value = _adminStats.value.copy(isLoading = false)
            Result.failure(e)
        }
    }

    override suspend fun registrarAcaoAuditoria(acao: String, detalhes: String?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = sessionManager.getCurrentUserId().orEmpty()
            val registro = com.example.data.remote.dto.AuditoriaRegistroDto(
                userId = userId,
                acao = acao,
                detalhes = detalhes,
                criadoEm = null
            )
            clientFactory.restApi.registrarAuditoria(registro)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun clearCache() {
        _vinculosFamiliares.value = emptyList()
        _pacientes.value = emptyList()
        _selectedPatient.value = null
        _atribuicoesAt.value = emptyList()
        _atSessoes.value = emptyList()
        _atRegistosObjetivos.value = emptyList()
        _atIncidentes.value = emptyList()
        _atSupervisoes.value = emptyList()
        _agendamentos.value = emptyList()
        _portalAgendamentos.value = emptyList()
        _formulariosClinicos.value = emptyList()
        _documentosClinicos.value = emptyList()
        _chatGrupos.value = emptyList()
        _chatMensagens.value = emptyMap()
        groupMessagesFlows.clear()
        _notificacoes.value = emptyList()
        _currentProfile.value = null
        _efemerides.value = emptyList()
        _aniversarios.value = emptyList()
        _popups.value = emptyList()
        _adminStats.value = com.example.domain.repository.AdminStatsState()
    }
}
