package com.example.domain.repository

import com.example.data.remote.dto.AgendamentoDto
import com.example.data.remote.dto.AniversarioDto
import com.example.data.remote.dto.AtIncidenteDto
import com.example.data.remote.dto.AtRegistoObjetivoDto
import com.example.data.remote.dto.AtSessaoDto
import com.example.data.remote.dto.AtSupervisaoDto
import com.example.data.remote.dto.AtribuicaoAtDto
import com.example.data.remote.dto.ChatGrupoDto
import com.example.data.remote.dto.ChatMensagemDto
import com.example.data.remote.dto.DocumentoClinicoDto
import com.example.data.remote.dto.EfemerideDto
import com.example.data.remote.dto.FormularioClinicoDto
import com.example.ui.screens.forms.ClinicalFormSubmission
import com.example.data.remote.dto.ModeloDocumentoDto
import com.example.data.remote.dto.NotificacaoDto
import com.example.data.remote.dto.PacienteDto
import com.example.data.remote.dto.PopupComunicadoDto
import com.example.data.remote.dto.VinculoFamiliarDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

data class AdminStatsState(
    val assistidosCount: Int = 0,
    val profissionaisCount: Int = 0,
    val atAtivosCount: Int = 0,
    val consultasCount: Int = 0,
    val isLoading: Boolean = false
)

/**
 * Contrato do repositório dos módulos do ecossistema CIADI+.
 * Consome exclusivamente o projeto Supabase real com RLS.
 */
interface ModulesRepository {

    // 1. Família / Vínculos
    fun observeCriancasVinculadas(): Flow<List<VinculoFamiliarDto>>
    fun observePacientes(): Flow<List<PacienteDto>>

    // 2. Acompanhante Terapêutico (A.T.)
    fun observeAtribuicoesAt(): Flow<List<AtribuicaoAtDto>>
    fun observeAtSessoes(atribuicaoId: String? = null): Flow<List<AtSessaoDto>>
    fun observeAtRegistosObjetivos(sessaoId: String? = null): Flow<List<AtRegistoObjetivoDto>>
    fun observeAtIncidentes(sessaoId: String? = null): Flow<List<AtIncidenteDto>>
    fun observeAtSupervisoes(): Flow<List<AtSupervisaoDto>>

    suspend fun registrarSessaoAt(sessao: AtSessaoDto): Result<AtSessaoDto>
    suspend fun registrarIncidenteAt(incidente: AtIncidenteDto): Result<AtIncidenteDto>
    suspend fun registrarObjetivoAt(registro: AtRegistoObjetivoDto): Result<AtRegistoObjetivoDto>

    // 3. Agenda & Agendamentos
    fun observeAgendamentos(pacienteId: String? = null, profissionalId: String? = null): Flow<List<AgendamentoDto>>
    suspend fun criarAgendamento(agendamento: AgendamentoDto): Result<AgendamentoDto>

    // 4. Formulários Clínicos
    fun observeFormulariosClinicos(): Flow<List<FormularioClinicoDto>>
    suspend fun submeterFormularioClinico(submissao: ClinicalFormSubmission): Result<String>

    // 5. Documentos Clínicos & A4 & 11 Modelos Oficiais
    fun observeDocumentosClinicos(pacienteId: String? = null): Flow<List<DocumentoClinicoDto>>
    suspend fun emitirDocumentoClinico(documento: DocumentoClinicoDto): Result<DocumentoClinicoDto>
    fun observeModelosDocumentos(): Flow<List<ModeloDocumentoDto>>

    // 6. Chat CIADI+
    fun observeChatGrupos(): Flow<List<ChatGrupoDto>>
    fun observeChatMensagens(grupoId: String): Flow<List<ChatMensagemDto>>
    suspend fun enviarMensagem(mensagem: ChatMensagemDto): Result<ChatMensagemDto>

    // 7. Notificações
    fun observeNotificacoes(): StateFlow<List<NotificacaoDto>>
    suspend fun marcarNotificacaoLida(id: String): Result<Unit>

    // 8. Calendário Inteligente (Efemérides & Aniversários)
    fun observeEfemerides(): Flow<List<EfemerideDto>>
    fun observeAniversarios(): Flow<List<AniversarioDto>>
    suspend fun sincronizarCalendarioInteligente(): Result<Unit>

    // 9. Popups & Anúncios Oficiais
    fun observePopups(): Flow<List<PopupComunicadoDto>>
    suspend fun sincronizarPopups(): Result<Unit>
    suspend fun criarPublicacaoPopup(popup: PopupComunicadoDto): Result<Unit>

    // 10. Métricas Reais do Dashboard
    fun observeAdminStats(): StateFlow<AdminStatsState>
    suspend fun atualizarEstatisticasReais(): Result<Unit>

    // 11. Auditoria
    suspend fun registrarAcaoAuditoria(acao: String, detalhes: String? = null): Result<Unit>

    // Sincronização geral
    suspend fun sincronizarTabela(nomeTabela: String): Result<Unit>

    // Limpeza de cache e dados na troca de usuário/logout
    fun clearCache()
}
