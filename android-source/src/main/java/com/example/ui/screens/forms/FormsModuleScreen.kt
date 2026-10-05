package com.example.ui.screens.forms

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
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
import com.example.data.remote.dto.FormularioClinicoDto
import com.example.data.remote.dto.PacienteDto
import com.example.domain.model.Permission
import com.example.domain.model.UserProfile
import com.example.domain.repository.ModulesRepository
import com.example.ui.components.CiadiTopBar
import com.example.ui.components.EmptyModuleState
import com.example.ui.theme.CIADIColors
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun FormsModuleScreen(
    user: UserProfile,
    onNavigateBack: () -> Unit,
    modulesRepository: ModulesRepository? = null,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var activeFormToFill by remember { mutableStateOf<FormularioClinicoDto?>(null) }
    var selectedPatient by remember { mutableStateOf<PacienteDto?>(null) }
    var lastSubmission by remember { mutableStateOf<ClinicalFormSubmission?>(null) }

    val patients by (modulesRepository?.observePacientes()
        ?.collectAsState(initial = emptyList())
        ?: remember { mutableStateOf(emptyList<PacienteDto>()) })

    val rawForms by (modulesRepository?.observeFormulariosClinicos()
        ?.collectAsState(initial = emptyList())
        ?: remember { mutableStateOf(emptyList<FormularioClinicoDto>()) })

    var selectedSpecialty by remember(user.specialty, rawForms) {
        mutableStateOf(
            user.specialty?.takeIf { specialty ->
                rawForms.any { it.areaAtuacao?.equals(specialty, ignoreCase = true) == true }
            } ?: "Todas"
        )
    }

    val specialtyOptions = remember(rawForms) {
        listOf("Todas") + rawForms
            .mapNotNull { it.areaAtuacao?.trim()?.takeIf(String::isNotBlank) }
            .distinctBy { it.lowercase() }
            .sorted()
    }

    val formsList = remember(rawForms, selectedSpecialty) {
        rawForms
            .filter { it.ativo != false }
            .filter {
                selectedSpecialty == "Todas" ||
                    it.areaAtuacao?.equals(selectedSpecialty, ignoreCase = true) == true
            }
            .sortedWith(compareBy({ it.areaAtuacao ?: "" }, { it.nome }))
    }

    if (activeFormToFill != null) {
        Scaffold(
            topBar = {
                CiadiTopBar(
                    title = "Preenchimento de Protocolo",
                    currentUser = user,
                    onNavigateBack = { activeFormToFill = null }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(CIADIColors.BackgroundLight)
                    .padding(16.dp)
            ) {
                if (lastSubmission != null) {
                    item {
                        SubmissionActions(context, activeFormToFill!!, lastSubmission!!, onDone = { activeFormToFill = null })
                    }
                }
                item {
                    ClinicalFormRenderer(
                        formulario = activeFormToFill!!,
                        pacienteId = selectedPatient?.id.orEmpty(),
                        pacienteNome = selectedPatient?.nome ?: user.activePatientName ?: "Assistido",
                        onSubmit = { submission ->
                            scope.launch {
                                val result = modulesRepository?.submeterFormularioClinico(submission)
                                    ?: Result.failure(Exception("Módulo clínico não está ligado ao Supabase."))
                                if (!user.hasPermission(Permission.SUBMIT_CLINICAL_FORM)) {
                                    snackbarHostState.showSnackbar("O perfil A.T. não possui autorização para submeter formulários clínicos.")
                                } else if (activeFormToFill?.id?.startsWith("form_at_") == true) {
                                    snackbarHostState.showSnackbar("Este formulário está em modo de contingência. Sincronize os formulários do CIADI antes de submeter.")
                                } else if (result.isSuccess) {
                                    lastSubmission = submission
                                    snackbarHostState.showSnackbar("Formulário enviado e registado no prontuário CIADI.")
                                } else {
                                    snackbarHostState.showSnackbar(result.exceptionOrNull()?.message ?: "Não foi possível enviar o formulário.")
                                }
                            }
                        }
                    )
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            CiadiTopBar(
                title = "Formulários Clínicos",
                currentUser = user,
                onNavigateBack = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(CIADIColors.BackgroundLight)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Protocolos & Anamneses CIADI",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = CIADIColors.Brown
                    )
                )
                Text(
                    text = if (user.hasPermission(Permission.SUBMIT_CLINICAL_FORM))
                        "Formulários parametrizados do Supabase. O A.T. pode preencher, submeter, imprimir e partilhar os registos autorizados."
                    else
                        "O seu perfil pode consultar os formulários, mas não possui autorização para submeter.",
                    style = MaterialTheme.typography.bodySmall,
                    color = CIADIColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            item {
                Text(
                    text = "Especialidade",
                    fontWeight = FontWeight.Bold,
                    color = CIADIColors.Brown
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    specialtyOptions.forEach { specialty ->
                        androidx.compose.material3.FilterChip(
                            selected = selectedSpecialty.equals(specialty, ignoreCase = true),
                            onClick = { selectedSpecialty = specialty },
                            label = { Text(specialty, fontSize = 12.sp) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Text("Assistido selecionado", fontWeight = FontWeight.Bold, color = CIADIColors.Brown)
                if (patients.isEmpty()) {
                    Text("Nenhum assistido autorizado foi carregado.", color = CIADIColors.TextSecondary, fontSize = 12.sp)
                } else {
                    patients.forEach { patient ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = if (selectedPatient?.id == patient.id) Color(0xFFE2F4F7) else Color.White),
                            modifier = Modifier.fillMaxWidth().clickable { selectedPatient = patient }
                        ) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) { Text(patient.nome, fontWeight = FontWeight.Bold, color = CIADIColors.Brown) }
                                Text(if (selectedPatient?.id == patient.id) "Selecionado" else "Selecionar", color = CIADIColors.OrangePrimary, fontSize = 12.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            if (formsList.isEmpty()) {
                item {
                    EmptyModuleState(
                        icon = Icons.AutoMirrored.Filled.Assignment,
                        moduleTitle = "Nenhum formulário disponível",
                        targetTable = "ciadi_formularios_clinicos",
                        requiredPermission = Permission.SUBMIT_CLINICAL_FORM,
                        onRefresh = {}
                    )
                }
            }

            items(formsList) { form ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            when {
                                selectedPatient == null -> scope.launch { snackbarHostState.showSnackbar("Selecione primeiro o assistido.") }
                                !user.hasPermission(Permission.SUBMIT_CLINICAL_FORM) -> scope.launch { snackbarHostState.showSnackbar("O seu perfil não está autorizado a preencher e submeter formulários clínicos.") }
                                else -> {
                                    activeFormToFill = form
                                    lastSubmission = null
                                }
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = form.areaAtuacao ?: "Especialidade",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = CIADIColors.OrangePrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = form.nome,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = CIADIColors.Brown
                                )
                            )
                            form.descricao?.let {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CIADIColors.TextSecondary
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Preencher",
                            tint = CIADIColors.Brown
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun SubmissionActions(
    context: Context,
    form: FormularioClinicoDto,
    submission: ClinicalFormSubmission,
    onDone: () -> Unit
) {
    val body = remember(submission) {
        buildString {
            append("CIADI+ — ${form.nome}\n")
            append("Assistido ID: ${submission.pacienteId}\n\n")
            val json = JSONObject(submission.respostasJson)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                append("$key: ${json.optString(key)}\n")
            }
        }
    }
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("Formulário registado", fontWeight = FontWeight.Bold, color = CIADIColors.TealPrimary)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(modifier = Modifier.weight(1f), onClick = {
                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, body)
                        putExtra(Intent.EXTRA_SUBJECT, form.nome)
                    }, "Ver / enviar formulário"))
                }) { Text("Ver / Enviar", fontSize = 12.sp) }
                Button(modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = CIADIColors.TealPrimary), onClick = {
                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                    printManager.print("CIADI+_${form.codigo ?: "FORM"}", ClinicalFormPrintAdapter(form.nome, body), PrintAttributes.Builder().build())
                }) { Text("Imprimir", fontSize = 12.sp) }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(modifier = Modifier.weight(1f), onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; setPackage("com.whatsapp"); putExtra(Intent.EXTRA_TEXT, body) }
                    try { context.startActivity(intent) } catch (_: Exception) { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, body) }, "Enviar formulário")) }
                }) { Text("WhatsApp", fontSize = 12.sp) }
                OutlinedButton(modifier = Modifier.weight(1f), onClick = {
                    context.startActivity(Intent(Intent.ACTION_SENDTO).apply { data = Uri.parse("mailto:"); putExtra(Intent.EXTRA_SUBJECT, form.nome); putExtra(Intent.EXTRA_TEXT, body) })
                }) { Text("Email", fontSize = 12.sp) }
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Voltar aos formulários") }
        }
    }
}
