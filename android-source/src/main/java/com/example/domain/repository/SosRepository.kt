package com.example.domain.repository

import com.example.data.remote.dto.SosAlertaResultDto

/**
 * Repositório oficial para a infraestrutura SOS do CIADI+.
 * Consome a RPC segura do Supabase: 'ciadi_criar_alerta_sos'.
 */
interface SosRepository {
    suspend fun registrarDenunciaBullying(
        pacienteId: String?,
        titulo: String,
        descricao: String,
        gravidade: String = "MEDIA"
    ): Result<String>

    suspend fun enviarAlertaSos(
        pacienteId: String,
        mensagem: String,
        latitude: Double?,
        longitude: Double?,
        localizacaoAutorizada: Boolean
    ): Result<SosAlertaResultDto>
}
