package com.example.domain.repository

import com.example.data.remote.dto.AgendamentoDto
import com.example.data.remote.dto.PortalAgendamentoDto
import kotlinx.coroutines.flow.StateFlow

/**
 * Repositório oficial para Agenda & Agendamentos no CIADI+.
 * Consome exclusivamente a tabela 'agendamentos' e a RPC 'ciadi_portal_agendamentos_autorizados' do Supabase com RLS.
 * Também integra o fluxo da Clínica Virtual (ciadi_preparar_sala_video, etc.).
 */
interface AgendaRepository {
    fun observeAgendamentos(): StateFlow<List<AgendamentoDto>>
    fun observePortalAgendamentos(): StateFlow<List<PortalAgendamentoDto>>
    suspend fun fetchPortalAgendamentos(pacienteId: String?): Result<List<PortalAgendamentoDto>>
    suspend fun criarAgendamento(agendamento: AgendamentoDto): Result<AgendamentoDto>
    suspend fun syncAgenda(): Result<Unit>

    // Clínica Virtual
    suspend fun prepararSalaVideo(agendamentoId: String): Result<String>
    suspend fun registrarEntradaVideo(agendamentoId: String): Result<Unit>
    suspend fun registrarSaidaVideo(agendamentoId: String): Result<Unit>
    suspend fun encerrarSalaVideo(agendamentoId: String): Result<Unit>
}
