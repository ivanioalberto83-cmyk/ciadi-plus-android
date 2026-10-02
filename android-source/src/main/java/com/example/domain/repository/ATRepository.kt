package com.example.domain.repository

import com.example.data.remote.dto.AcompanhamentoDiarioAbaDto
import com.example.data.remote.dto.AtAtividadeDto
import com.example.data.remote.dto.AtCalendarioEventoDto
import com.example.data.remote.dto.AtContactoDto
import com.example.data.remote.dto.AtIncidenteDto
import com.example.data.remote.dto.AtOrganizacaoDto
import com.example.data.remote.dto.AtRegistoObjetivoDto
import com.example.data.remote.dto.AtSessaoDto
import com.example.data.remote.dto.AtSupervisaoDto
import com.example.data.remote.dto.AtribuicaoAtDto
import com.example.data.remote.dto.PacienteDto
import com.example.data.remote.dto.ProfissionalDto
import kotlinx.coroutines.flow.StateFlow

/**
 * Repositório oficial para Acompanhante Terapêutico (A.T.) no CIADI+.
 * Fluxo: Atribuição → Paciente → Agenda → Atividades → Sessão → Objetivos → Registo → Incidente → Supervisão
 */
interface ATRepository {
    fun observeAtribuicoes(): StateFlow<List<AtribuicaoAtDto>>
    fun observeAssistidos(): StateFlow<List<PacienteDto>>
    fun observeSessoes(): StateFlow<List<AtSessaoDto>>
    fun observeAtividades(): StateFlow<List<AtAtividadeDto>>
    fun observeCalendario(): StateFlow<List<AtCalendarioEventoDto>>
    fun observeContactos(): StateFlow<List<AtContactoDto>>
    fun observeOrganizacao(): StateFlow<List<AtOrganizacaoDto>>
    fun observeAcompanhamentosAba(): StateFlow<List<AcompanhamentoDiarioAbaDto>>
    fun observeRegistosObjetivos(): StateFlow<List<AtRegistoObjetivoDto>>
    fun observeIncidentes(): StateFlow<List<AtIncidenteDto>>
    fun observeSupervisoes(): StateFlow<List<AtSupervisaoDto>>
    fun observeProfissionalLogado(): StateFlow<ProfissionalDto?>

    suspend fun registrarAtividade(atividade: AtAtividadeDto): Result<AtAtividadeDto>
    suspend fun registrarSessao(sessao: AtSessaoDto): Result<AtSessaoDto>
    suspend fun registrarAcompanhamentoAba(registro: AcompanhamentoDiarioAbaDto): Result<AcompanhamentoDiarioAbaDto>
    suspend fun registrarObjetivo(objetivo: AtRegistoObjetivoDto): Result<AtRegistoObjetivoDto>
    suspend fun registrarIncidente(incidente: AtIncidenteDto): Result<AtIncidenteDto>
    suspend fun syncATData(): Result<Unit>
    suspend fun sincronizarAtividades(): Result<Unit>
    suspend fun sincronizarCalendarioAt(): Result<Unit>
    suspend fun sincronizarContactos(): Result<Unit>
    suspend fun sincronizarOrganizacao(): Result<Unit>
}
