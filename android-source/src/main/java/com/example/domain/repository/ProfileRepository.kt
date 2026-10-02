package com.example.domain.repository

import com.example.data.remote.dto.PerfilDto
import kotlinx.coroutines.flow.StateFlow

/**
 * Repositório oficial para Perfis no CIADI+.
 * Consome 'perfis' do Supabase PostgreSQL através do UUID autenticado.
 */
interface ProfileRepository {
    fun observeCurrentProfile(): StateFlow<PerfilDto?>
    suspend fun fetchProfile(userId: String): Result<PerfilDto>
}
