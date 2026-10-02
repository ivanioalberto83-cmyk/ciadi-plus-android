package com.example.domain.repository

import com.example.data.remote.dto.DocumentoClinicoDto
import com.example.data.remote.dto.DocumentoHistoricoDto
import kotlinx.coroutines.flow.StateFlow

/**
 * Repositório oficial para Documentos Clínicos, Laudos e P.E.I. no CIADI+.
 * Fluxo: Formulário → Registro → Documento → Revisão → Finalização → Assinatura → Publicação → Auditoria
 */
interface ClinicalDocumentsRepository {
    fun observeDocumentos(): StateFlow<List<DocumentoClinicoDto>>
    suspend fun emitirDocumento(documento: DocumentoClinicoDto): Result<DocumentoClinicoDto>
    suspend fun getHistoricoDocumento(documentoId: String): Result<List<DocumentoHistoricoDto>>
    suspend fun syncDocumentos(): Result<Unit>
}
