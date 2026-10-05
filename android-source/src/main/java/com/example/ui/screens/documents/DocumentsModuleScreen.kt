package com.example.ui.screens.documents

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.config.SupabaseConfig
import com.example.data.remote.dto.DocumentoClinicoDto
import com.example.domain.model.Permission
import com.example.domain.model.UserProfile
import com.example.domain.model.UserRole
import com.example.domain.repository.ClinicalDocumentsRepository
import com.example.domain.repository.ModulesRepository
import com.example.ui.components.CiadiTopBar
import com.example.ui.components.EmptyModuleState
import com.example.ui.theme.CIADIColors
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun DocumentsModuleScreen(
    user: UserProfile,
    onNavigateBack: () -> Unit,
    modulesRepository: ModulesRepository? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var selectedDocument by remember { mutableStateOf<DocumentoClinicoDto?>(null) }
    var showEmissionDialog by remember { mutableStateOf(false) }
    var documentTitle by remember { mutableStateOf("") }
    var documentType by remember { mutableStateOf("RELATORIO") }
    var documentContent by remember { mutableStateOf("") }
    var isEmitting by remember { mutableStateOf(false) }

    val rawDocs by (modulesRepository?.observeDocumentosClinicos()
        ?.collectAsState(initial = emptyList())
        ?: remember { mutableStateOf(emptyList<DocumentoClinicoDto>()) })

    // Se a família estiver visualizando, filtrar apenas documentos autorizados / publicados pelo RLS
    val docsList = remember(rawDocs, user.role) {
        if (user.role == UserRole.RESPONSAVEL) {
            rawDocs.filter { it.estado.equals("PUBLICADO", ignoreCase = true) }
        } else {
            rawDocs
        }
    }

    if (selectedDocument != null) {
        DocumentDetailsScreen(
            documento = selectedDocument!!,
            currentUser = user,
            documentsRepository = modulesRepository as? ClinicalDocumentsRepository,
            onNavigateBack = { selectedDocument = null },
            modifier = modifier
        )
        return
    }

    val canUpload = user.hasPermission(Permission.MANAGE_DOCUMENTS)

    if (showEmissionDialog) {
        AlertDialog(
            onDismissRequest = { if (!isEmitting) showEmissionDialog = false },
            title = { Text("Emitir documento clínico") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Assistido: " + (user.activePatientName ?: "Não identificado"))
                    OutlinedTextField(documentTitle, { documentTitle = it }, label = { Text("Título") }, enabled = !isEmitting, singleLine = true)
                    OutlinedTextField(documentType, { documentType = it.uppercase() }, label = { Text("Tipo: RELATORIO / LAUDO / PEI") }, enabled = !isEmitting, singleLine = true)
                    OutlinedTextField(documentContent, { documentContent = it }, label = { Text("Conteúdo clínico") }, enabled = !isEmitting, minLines = 5)
                }
            },
            confirmButton = {
                TextButton(enabled = !isEmitting && modulesRepository != null && user.activePatientId != null && documentTitle.isNotBlank() && documentContent.isNotBlank(), onClick = {
                    val repo = modulesRepository ?: return@TextButton
                    val pacienteId = user.activePatientId ?: return@TextButton
                    isEmitting = true
                    scope.launch {
                        val result = repo.emitirDocumentoClinico(DocumentoClinicoDto(
                            id = UUID.randomUUID().toString(),
                            pacienteId = pacienteId,
                            profissionalId = null,
                            pacienteNome = user.activePatientName,
                            profissionalNome = user.fullName,
                            especialidadeNome = user.specialty,
                            titulo = documentTitle.trim(),
                            tipoDocumento = documentType.trim().ifBlank { "RELATORIO" },
                            conteudoTexto = documentContent.trim(),
                            estado = "PUBLICADO"
                        ))
                        isEmitting = false
                        if (result.isSuccess) {
                            showEmissionDialog = false
                            documentTitle = ""
                            documentContent = ""
                            snackbarHostState.showSnackbar("Documento emitido e gravado no Supabase.")
                        } else {
                            snackbarHostState.showSnackbar(result.exceptionOrNull()?.message ?: "Falha ao emitir documento no Supabase.")
                        }
                    }
                }) { Text(if (isEmitting) "A gravar..." else "Emitir") }
            },
            dismissButton = { TextButton(enabled = !isEmitting, onClick = { showEmissionDialog = false }) { Text("Cancelar") } }
        )
    }

    Scaffold(
        topBar = {
            CiadiTopBar(
                title = "Documentos & Relatórios",
                currentUser = user,
                onNavigateBack = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (docsList.isEmpty()) {
            EmptyModuleState(
                icon = Icons.Default.Description,
                moduleTitle = "Laudos, Relatórios e P.E.I.",
                targetTable = "${SupabaseConfig.Tables.DOCUMENTOS_CLINICOS} (Storage: ciadi-documentos)",
                requiredPermission = Permission.VIEW_DOCUMENTS_PUBLISHED,
                onRefresh = {
                    scope.launch {
                        if (modulesRepository != null) {
                            val result = modulesRepository.sincronizarTabela(SupabaseConfig.Tables.DOCUMENTOS_CLINICOS)
                            if (result.isSuccess) {
                                snackbarHostState.showSnackbar("Documentos clínicos sincronizados via PostgREST com RLS.")
                            } else {
                                snackbarHostState.showSnackbar(result.exceptionOrNull()?.message ?: "Falha ao conectar")
                            }
                        } else {
                            snackbarHostState.showSnackbar("Consultando '${SupabaseConfig.Tables.DOCUMENTOS_CLINICOS}' autorizados para seu perfil...")
                        }
                    }
                },
                primaryActionLabel = if (canUpload) "Emitir Relatório / Laudo" else "Solicitar Documento",
                onPrimaryAction = {
                    if (canUpload) {
                        if (user.activePatientId == null) {
                            scope.launch { snackbarHostState.showSnackbar("Selecione primeiro um assistido para emitir o documento.") }
                        } else {
                            showEmissionDialog = true
                        }
                    } else {
                        scope.launch { snackbarHostState.showSnackbar("Solicitação de documento encaminhada para a secretaria e coordenação do CIADI.") }
                    }
                },
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(CIADIColors.BackgroundLight)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(docsList) { doc ->
                    DocumentCardItem(
                        doc = doc,
                        onViewDetails = { selectedDocument = doc },
                        onShareWhatsApp = {
                            DocumentShareController.shareViaWhatsApp(context, doc)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DocumentCardItem(
    doc: DocumentoClinicoDto,
    onViewDetails: () -> Unit,
    onShareWhatsApp: () -> Unit
) {
    val isPublished = doc.estado.equals("PUBLICADO", ignoreCase = true) ||
            doc.estado.equals("ASSINADO", ignoreCase = true)

    val statusColor = when (doc.estado?.uppercase()) {
        "PUBLICADO" -> CIADIColors.SuccessGreen
        "ASSINADO" -> CIADIColors.TealPrimary
        "REVISAO", "REVISÃO" -> CIADIColors.OrangePrimary
        "RASCUNHO" -> Color.Gray
        else -> CIADIColors.Brown
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewDetails() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = doc.tipoDocumento ?: "RELATÓRIO",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = CIADIColors.TealPrimary
                    )
                )

                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = doc.estado ?: "PUBLICADO",
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = doc.titulo,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = CIADIColors.Brown)
            )

            doc.pacienteNome?.let {
                Text(text = "Assistido: $it", style = MaterialTheme.typography.bodySmall)
            }

            doc.profissionalNome?.let {
                Text(
                    text = "Por: $it (${doc.especialidadeNome ?: "CIADI"})",
                    style = MaterialTheme.typography.labelSmall,
                    color = CIADIColors.TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onViewDetails,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Ver Detalhes", fontSize = 12.sp)
                }

                if (isPublished) {
                    Button(
                        onClick = onShareWhatsApp,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WhatsApp", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
