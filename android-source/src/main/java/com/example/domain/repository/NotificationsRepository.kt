package com.example.domain.repository

import com.example.data.remote.dto.NotificacaoDto
import kotlinx.coroutines.flow.StateFlow

/**
 * Repositório oficial para Notificações no CIADI+.
 * Consome 'notificacoes' do Supabase PostgreSQL com RLS.
 */
interface NotificationsRepository {
    fun observeNotificacoes(): StateFlow<List<NotificacaoDto>>
    suspend fun marcarLida(id: String): Result<Unit>
    suspend fun syncNotificacoes(): Result<Unit>
}
