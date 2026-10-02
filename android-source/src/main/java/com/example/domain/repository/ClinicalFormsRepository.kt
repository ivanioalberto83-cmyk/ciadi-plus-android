package com.example.domain.repository

import com.example.data.remote.dto.FormularioClinicoDto
import kotlinx.coroutines.flow.StateFlow

/**
 * Repositório oficial para Formulários Clínicos no CIADI+.
 * Consome 'ciadi_formularios_clinicos' e 'ciadi_formularios_especialidades' dinamicamente via PostgREST.
 */
interface ClinicalFormsRepository {
    fun observeFormularios(): StateFlow<List<FormularioClinicoDto>>
    suspend fun syncFormularios(): Result<Unit>
}
