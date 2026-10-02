package com.example.ui.screens.documents

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.dto.DocumentoClinicoDto
import com.example.data.remote.dto.DocumentoHistoricoDto
import com.example.domain.model.UserProfile
import com.example.domain.model.UserRole
import com.example.domain.repository.ClinicalDocumentsRepository
import com.example.ui.theme.CIADIColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailsScreen(
    documento: DocumentoClinicoDto,
    currentUser: UserProfile,
    documentsRepository: ClinicalDocumentsRepository?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showHistory by remember { mutableStateOf(false) }
    var historicoList by remember { mutableStateOf<List<DocumentoHistoricoDto>>(emptyList()) }

    LaunchedEffect(documento.id) {
        if (documentsRepository != null) {
            val result = documentsRepository.getHistoricoDocumento(documento.id)
            if (result.isSuccess) {
                historicoList = result.getOrNull().orEmpty()
            }
        }
    }

    val isPublished = documento.estado.equals("PUBLICADO", ignoreCase = true) ||
            documento.estado.equals("ASSINADO", ignoreCase = true)

    val statusColor = when (documento.estado?.uppercase()) {
        "PUBLICADO" -> CIADIColors.SuccessGreen
        "ASSINADO" -> CIADIColors.TealPrimary
        "FINALIZADO" -> CIADIColors.RoleProfessional
        "REVISAO", "REVISÃO" -> CIADIColors.OrangePrimary
        "RASCUNHO" -> Color.Gray
        else -> CIADIColors.Brown
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalhes do Documento", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = CIADIColors.Brown
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(CIADIColors.BackgroundLight)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Card Principal
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = documento.tipoDocumento ?: "RELATÓRIO",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = CIADIColors.TealPrimary
                            )
                        )

                        Box(
                            modifier = Modifier
                                .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = documento.estado ?: "PUBLICADO",
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = documento.titulo,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = CIADIColors.Brown
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    documento.pacienteNome?.let {
                        Text(
                            text = "Assistido: $it",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }

                    documento.profissionalNome?.let {
                        Text(
                            text = "Profissional: $it (${documento.especialidadeNome ?: "Equipe Multidisciplinar"})",
                            style = MaterialTheme.typography.bodySmall,
                            color = CIADIColors.TextSecondary
                        )
                    }

                    documento.registroProfissional?.let {
                        Text(
                            text = "Registro: $it",
                            style = MaterialTheme.typography.labelSmall,
                            color = CIADIColors.TextSecondary
                        )
                    }

                    documento.codigoValidacao?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = CIADIColors.SuccessGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Código de Validação: $it",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = CIADIColors.SuccessGreen
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Conteúdo do Documento (DocumentViewer)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Conteúdo do Parecer / P.E.I.",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = CIADIColors.Brown
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = documento.conteudoTexto ?: "Documento clínico assinado digitalmente. Para visualização completa com cabeçalho oficial e carimbo de registro, utilize o botão abaixo para abrir o PDF seguro A4.",
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp
                    )

                    documento.pdfUrl?.let { url ->
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = { /* Abrir PDF no visualizador seguro */ },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, tint = CIADIColors.OrangePrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Visualizar PDF Seguro A4", color = CIADIColors.Brown)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Ações de Compartilhamento e Histórico
            if (isPublished) {
                Button(
                    onClick = {
                        DocumentShareController.shareViaWhatsApp(context, documento)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enviar pelo WhatsApp", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            OutlinedButton(
                onClick = { showHistory = !showHistory },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.History, contentDescription = null, tint = CIADIColors.TealPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (showHistory) "Ocultar Histórico" else "Ver Histórico de Versões RLS")
            }

            // Histórico de Versões (DocumentHistoryScreen)
            if (showHistory) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CIADIColors.CreamLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Auditoria de Versões (ciadi_documentos_clinicos_historico)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = CIADIColors.Brown
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (historicoList.isEmpty()) {
                            Text(
                                text = "Nenhuma alteração registrada após a publicação inicial.",
                                style = MaterialTheme.typography.bodySmall,
                                color = CIADIColors.TextSecondary
                            )
                        } else {
                            historicoList.forEach { hist ->
                                Text(
                                    text = "• ${hist.estadoAnterior ?: "Criação"} → ${hist.estadoNovo ?: "Atualização"}: ${hist.alteracaoResumo ?: "Alteração clínica registrada"}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
