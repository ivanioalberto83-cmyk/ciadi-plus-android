package com.example.domain.repository

import com.example.data.remote.dto.PacienteDto
import com.example.data.remote.dto.VinculoFamiliarDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Repositório oficial para Família / Responsável no ecossistema CIADI+.
 * Consome exclusivamente as tabelas 'ciadi_vinculos_familiares' e 'pacientes' via RLS.
 */
interface FamilyRepository {
    fun observeVinculos(): StateFlow<List<VinculoFamiliarDto>>
    fun observeAuthorizedPatients(): StateFlow<List<PacienteDto>>
    fun observeSelectedPatient(): StateFlow<PacienteDto?>
    fun selectPatient(patient: PacienteDto)
    suspend fun syncFamilyBonds(): Result<Unit>
}
