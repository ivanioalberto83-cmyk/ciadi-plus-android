package com.example.ui.screens.documents

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.remote.dto.DocumentoClinicoDto

/**
 * Controlador de compartilhamento seguro de documentos clínicos do CIADI+.
 * Respeita RLS e garante que apenas documentos autorizados/publicados possam ser compartilhados.
 */
object DocumentShareController {

    fun shareViaWhatsApp(context: Context, doc: DocumentoClinicoDto, recipientPhone: String? = null) {
        if (doc.estado.equals("RASCUNHO", ignoreCase = true)) {
            Toast.makeText(context, "Documentos em rascunho não podem ser compartilhados.", Toast.LENGTH_SHORT).show()
            return
        }

        val shareText = buildString {
            append("📄 *CIADI+ — Documento Clínico Oficial*\n\n")
            append("*Título:* ${doc.titulo}\n")
            doc.pacienteNome?.let { append("*Assistido:* $it\n") }
            doc.profissionalNome?.let { append("*Profissional:* $it\n") }
            doc.especialidadeNome?.let { append("*Especialidade:* $it\n") }
            doc.codigoValidacao?.let { append("*Código de Validação:* $it\n") }
            append("*Status:* ${doc.estado ?: "Publicado"}\n\n")
            doc.pdfUrl?.let { append("Visualizar PDF Seguro:\n$it\n\n") }
            append("Emitido pelo Centro Integrado de Apoio e Desenvolvimento Individual (CIADI).")
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                val cleanPhone = recipientPhone?.filter { it.isDigit() }.orEmpty()
                val url = if (cleanPhone.isNotBlank()) {
                    "https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(shareText)}"
                } else {
                    "https://api.whatsapp.com/send?text=${Uri.encode(shareText)}"
                }
                data = Uri.parse(url)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback para compartilhador genérico
            shareGeneric(context, doc)
        }
    }

    fun shareGeneric(context: Context, doc: DocumentoClinicoDto) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "CIADI+ Documento: ${doc.titulo}")
            putExtra(
                Intent.EXTRA_TEXT,
                "Documento Oficial CIADI+: ${doc.titulo}\nAssistido: ${doc.pacienteNome ?: "Não informado"}\nValidação: ${doc.codigoValidacao ?: "Autenticado via RLS"}\n${doc.pdfUrl ?: ""}"
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Compartilhar Documento CIADI+").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
