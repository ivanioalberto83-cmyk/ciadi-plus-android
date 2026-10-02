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
        if (rawForms.isNotEmpty()) rawForms else listOf(
            FormularioClinicoDto(
                id = "form_at_diario",
                codigo = "AT-01",
                nome = "Registro Diário de Sessão A.T. (Campo/Escola)",
                areaAtuacao = "Acompanhamento Terapêutico",
                tipoFormulario = "ACOMPANHAMENTO",
                descricao = "Protocolo de observação de metas, engajamento e incidentes em ambiente escolar/social.",
                schemaJson = """{"fields":[{"key":"nivel_autonomia","label":"Nível de Autonomia","type":"radio","options":["Totalmente Independente","Ajuda Leve","Ajuda Moderada","Ajuda Total"]},{"key":"intervencao_comportamental","label":"Manejo Comportamental Realizado","type":"textarea"},{"key":"houve_desregulacao","label":"Houve desregulação sensorial/emocional","type":"checkbox"}]}"""
            ),
            FormularioClinicoDto(
                id = "form_fala_triagem",
                codigo = "TF-01",
                nome = "Avaliação Inicial de Comunicação & Fala",
                areaAtuacao = "Terapia da Fala",
                tipoFormulario = "AVALIACAO",
                descricao = "Rastreio de linguagem expressiva, compreensiva e comunicação alternativa aumentativa (CAA).",
                schemaJson = """{"fields":[{"key":"modalidade_comunicacao","label":"Modalidade Predominante","type":"radio","options":["Verbal / Vocal","Gestual / Apontamento","PECS / Pranchas","Sem Comunicação Funcional"]},{"key":"compreensao_comandos","label":"Compreensão de Comandos Simples","type":"checkbox"},{"key":"resumo_fonoaudiologico","label":"Parecer Fonoaudiológico","type":"textarea"}]}"""
            ),
            FormularioClinicoDto(
                id = "form_to_sensorial",
                codigo = "TO-01",
                nome = "Perfil e Rastreio de Processamento Sensorial",
                areaAtuacao = "Terapia Ocupacional",
                tipoFormulario = "SENSORIAL",
                descricao = "Identificação de hiper ou hiporresponsividade tátil, vestibular e proprioceptiva.",
                schemaJson = """{"fields":[{"key":"reacao_auditiva","label":"Sensibilidade a Sons Altos","type":"radio","options":["Tolerante","Incomodado","Desorganiza com Choro"]},{"key":"atividades_vida_diaria","label":"Autonomia em AVDs (Alimentação / Vestuário)","type":"textarea"}]}"""
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
