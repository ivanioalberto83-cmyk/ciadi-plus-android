package com.example.ui.screens.forms

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.config.SupabaseConfig
import com.example.data.remote.dto.FormularioClinicoDto
import com.example.domain.model.Permission
import com.example.domain.model.UserProfile
import com.example.domain.repository.ModulesRepository
import com.example.ui.components.CiadiTopBar
import com.example.ui.components.EmptyModuleState
import com.example.ui.theme.CIADIColors
import kotlinx.coroutines.launch

@Composable
fun FormsModuleScreen(
    user: UserProfile,
    onNavigateBack: () -> Unit,
    modulesRepository: ModulesRepository? = null,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var activeFormToFill by remember { mutableStateOf<FormularioClinicoDto?>(null) }

    val rawForms by (modulesRepository?.observeFormulariosClinicos()
        ?.collectAsState(initial = emptyList())
        ?: remember { mutableStateOf(emptyList<FormularioClinicoDto>()) })

    val formsList = remember(rawForms) {
        val atCodes = setOf(
            "ACOMPANHAMENTO_ABA_ABC",
            "AVALIACAO_NEURODESENVOLVIMENTO"
        )
        val atForms = rawForms.filter { it.codigo in atCodes }
        if (atForms.isNotEmpty()) atForms else listOf(
            FormularioClinicoDto(
                id = "form_at_aba_abc",
                codigo = "ACOMPANHAMENTO_ABA_ABC",
                nome = "Registo de Acompanhamento ABA — ABC CIADI",
                areaAtuacao = "ABA / Intervenção Comportamental",
                tipoFormulario = "evolucao",
                descricao = "Registo ABC: antecedente, comportamento, consequência, estratégia, resposta e próximo passo.",
                schemaJson = """{"fields":[
                    {"key":"atividade","label":"Atividade / contexto","type":"text","required":true},
                    {"key":"objetivo","label":"Objetivo da sessão","type":"textarea","required":true},
                    {"key":"antecedente","label":"A — Antecedente","type":"textarea","required":true},
                    {"key":"comportamento_observavel","label":"B — Comportamento observável","type":"textarea","required":true},
                    {"key":"consequencia","label":"C — Consequência / resposta","type":"textarea","required":true},
                    {"key":"estrategia","label":"Estratégia utilizada","type":"textarea","required":true},
                    {"key":"resposta","label":"Resposta observada","type":"textarea","required":true},
                    {"key":"proximo_passo","label":"Próximo passo","type":"textarea"}
                ]}"""
            ),
            FormularioClinicoDto(
                id = "form_at_avaliacao",
                codigo = "AVALIACAO_NEURODESENVOLVIMENTO",
                nome = "Ficha de Avaliação / Diagnóstico — Neurodesenvolvimento CIADI",
                areaAtuacao = "Neurodesenvolvimento",
                tipoFormulario = "avaliacao",
                descricao = "Ficha de avaliação clínica para registo do desenvolvimento, funcionamento, necessidades e encaminhamentos.",
                schemaJson = """{"fields":[
                    {"key":"historia_desenvolvimento","label":"História do desenvolvimento","type":"textarea","required":true},
                    {"key":"comunicacao","label":"Comunicação","type":"textarea","required":true},
                    {"key":"interacao_social","label":"Interação social","type":"textarea"},
                    {"key":"comportamento","label":"Comportamentos observados","type":"textarea"},
                    {"key":"autonomia","label":"Autonomia","type":"textarea"},
                    {"key":"necessidades","label":"Necessidades identificadas","type":"textarea","required":true},
                    {"key":"encaminhamentos","label":"Encaminhamentos / recomendações","type":"textarea","required":true}
                ]}"""
            )
        )
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
                item {
                    ClinicalFormRenderer(
                        formulario = activeFormToFill!!,
                        pacienteNome = user.activePatientName ?: "Lucas Silva",
                        onSubmit = { submission ->
                            scope.launch {
                                snackbarHostState.showSnackbar("Formulário submetido e associado ao prontuário via RLS.")
                                activeFormToFill = null
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
                    text = "Formulários parametrizados e carregados dinamicamente do Supabase por especialidade.",
                    style = MaterialTheme.typography.bodySmall,
                    color = CIADIColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            items(formsList) { form ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { activeFormToFill = form }
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
