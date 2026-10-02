package com.example.domain.repository

import com.example.data.remote.dto.AtContactoDto
import com.example.data.remote.dto.ChatGrupoDto
import com.example.data.remote.dto.ChatMensagemDto
import kotlinx.coroutines.flow.StateFlow

/**
 * Repositório oficial para Chat & Mensagens no CIADI+.
 * Consome 'ciadi_chat_grupos', 'ciadi_chat_membros', 'ciadi_chat_mensagens',
 * 'v_ciadi_at_contactos' e 'ciadi_chat_abrir_privado' do Supabase.
 */
interface ChatRepository {
    fun observeGrupos(): StateFlow<List<ChatGrupoDto>>
    fun observeMensagens(grupoId: String): StateFlow<List<ChatMensagemDto>>
    fun observeContactosAutorizados(): StateFlow<List<AtContactoDto>>
    fun observeRealtimeStatus(grupoId: String): StateFlow<String>

    suspend fun abrirConversaPrivada(destinatarioId: String, pacienteId: String? = null): Result<String>
    suspend fun enviarMensagem(mensagem: ChatMensagemDto): Result<ChatMensagemDto>
    suspend fun enviarMensagemTexto(grupoId: String, texto: String, userId: String): Result<ChatMensagemDto>
    suspend fun carregarMensagensGrupo(grupoId: String): Result<List<ChatMensagemDto>>
    suspend fun syncChat(): Result<Unit>
    suspend fun syncContactos(): Result<Unit>
    fun iniciarRealtime(grupoId: String)
    fun pararRealtime(grupoId: String)
}
