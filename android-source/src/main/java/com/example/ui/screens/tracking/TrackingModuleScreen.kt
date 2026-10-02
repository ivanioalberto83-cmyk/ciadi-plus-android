package com.example.ui.screens.tracking

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.config.SupabaseConfig
import com.example.data.remote.dto.AtSessaoDto
import com.example.data.remote.dto.PacienteDto
import com.example.data.repository.SupabaseModulesRepositoryImpl
import com.example.domain.model.UserProfile
import com.example.domain.repository.ModulesRepository
import com.example.ui.components.CiadiTopBar
import com.example.ui.designsystem.CIADIButtons
import com.example.ui.designsystem.CIADICards
import com.example.ui.screens.at.AtNovoAcompanhamentoDialog
import com.example.ui.theme.CIADIColors
import kotlinx.coroutines.launch

/**
 * Tela de Acompanhamento Terapêutico (Item 4 do Prompt Mestre).
 *
 * Utiliza dados reais das tabelas:
 * - ciadi_at_sessoes
 * - ciadi_at_registos_objetivos
 * - ciadi_at_incidentes
 * - ciadi_acompanhamentos_diarios_aba
 *
 * Mostra:
 * - data
 * - horário
 * - assistido
 * - objetivo
 * - atividades
 * - comportamento observado
 * - progresso
 * - comunicação com família
 * - observações
 * - estado
 *
 * Se não houver registros: "Não existem acompanhamentos registrados." com botão "+ Novo acompanhamento".
 */
@Composable
fun TrackingModuleScreen(
    user: UserProfile,
    onNavigateBack: () -> Unit,
    modulesRepository: ModulesRepository? = null,
    modifier: Modifier = Modifier
) {
    val repoImpl = modulesRepository as? SupabaseModulesRepositoryImpl
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showNovoDialog by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    val assistidosFlow = remember(repoImpl) {
        repoImpl?.observeAssistidos() ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList<PacienteDto>())
    }
    val assistidos by assistidosFlow.collectAsState()

    val atribuicoesFlow = remember(repoImpl) {
        repoImpl?.observeAtribuicoes() ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())
    }
    val atribuicoes by atribuicoesFlow.collectAsState()

    val sessoesFlow = remember(repoImpl) {
        repoImpl?.observeSessoes() ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList<AtSessaoDto>())
    }
    val sessoes by sessoesFlow.collectAsState()

    val abaFlow = remember(repoImpl) {
        repoImpl?.observeAcompanhamentosAba() ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())
    }
    val acompanhamentosAba by abaFlow.collectAsState()

    val profissionalFlow = remember(repoImpl) {
        repoImpl?.observeProfissionalLogado() ?: kotlinx.coroutines.flow.MutableStateFlow(null)
    }
    val profissionalLogado by profissionalFlow.collectAsState()

    fun refreshTracking() {
        scope.launch {
            isRefreshing = true
            repoImpl?.syncATData()
            isRefreshing = false
        }
    }

    LaunchedEffect(Unit) {
        refreshTracking()
    }

    if (showNovoDialog) {
        AtNovoAcompanhamentoDialog(
            user = user,
            assistidos = assistidos,
            atribuicoes = atribuicoes,
            profissionalId = profissionalLogado?.id,
            onDismiss = { showNovoDialog = false },
            onSalvar = { novaSessao ->
                repoImpl?.registrarSessao(novaSessao)
                    ?: Result.failure(IllegalStateException("Repositório indisponível"))
            },
            onSalvaComSucesso = {
                scope.launch {
                    snackbarHostState.showSnackbar("Acompanhamento registrado com sucesso!")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CiadiTopBar(
                title = "Acompanhamento Terapêutico",
                currentUser = user,
                onNavigateBack = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNovoDialog = true },
                containerColor = CIADIColors.TealPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_novo_acompanhamento_tracking")
            ) {
                Row(modifier = Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Novo Acompanhamento")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("+ Novo acompanhamento", fontWeight = FontWeight.Bold)
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(CIADIColors.BackgroundLight)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Cabeçalho de Contexto
            CIADICards.Warm(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(CIADIColors.TealPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timeline,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Acompanhamento de Campo & Rotina",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = CIADIColors.Brown
                                    )
                                )
                                Text(
                                    text = "Sessões registradas via ciadi_at_sessoes",
                                    style = MaterialTheme.typography.labelSmall.copy(color = CIADIColors.TextSecondary)
                                )
                            }
                        }

                        IconButton(onClick = { refreshTracking() }) {
                            if (isRefreshing) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = CIADIColors.TealPrimary, strokeWidth = 2.dp)
                            } else {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Atualizar", tint = CIADIColors.Brown)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lista ou Estado Vazio
            if (sessoes.isEmpty() && acompanhamentosAba.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("empty_acompanhamentos"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AssignmentTurnedIn,
                            contentDescription = null,
                            modifier = Modifier.size(54.dp),
                            tint = CIADIColors.TealPrimary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Não existem acompanhamentos registrados.",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = CIADIColors.Brown
                            ),
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        CIADIButtons.Primary(
                            text = "+ Novo acompanhamento",
                            onClick = { showNovoDialog = true },
                            modifier = Modifier.testTag("btn_novo_acompanhamento_empty_screen")
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize().testTag("lista_sessoes_acompanhamento")
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Acompanhamentos Registrados (${sessoes.size + acompanhamentosAba.size})",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = CIADIColors.Brown
                                )
                            )
                            Button(
                                onClick = { showNovoDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = CIADIColors.TealPrimary)
                            ) {
                                Text("+ Novo", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    items(sessoes) { sessao ->
                        CIADICards.Base(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Data: ${sessao.dataSessao}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CIADIColors.Brown)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFE0F2F1))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = sessao.estado ?: sessao.status ?: "Realizada",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF00695C)
                                        )
                                    }
                                }

                                if (!sessao.horaInicio.isNullOrBlank()) {
                                    Text(
                                        text = "Horário: ${sessao.horaInicio} - ${sessao.horaFim ?: ""}",
                                        style = MaterialTheme.typography.labelSmall.copy(color = CIADIColors.TextSecondary)
                                    )
                                }

                                if (!sessao.pacienteNome.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Assistido: ${sessao.pacienteNome}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = CIADIColors.TextPrimary, fontWeight = FontWeight.Bold)
                                    )
                                }

                                if (!sessao.objetivo.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "Objetivo: ${sessao.objetivo}", style = MaterialTheme.typography.bodySmall.copy(color = CIADIColors.TextPrimary))
                                }

                                if (!sessao.atividades.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "Atividades: ${sessao.atividades}", style = MaterialTheme.typography.bodySmall.copy(color = CIADIColors.TextSecondary))
                                }

                                if (!sessao.comportamentoObservado.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "Comportamento: ${sessao.comportamentoObservado}", style = MaterialTheme.typography.bodySmall.copy(color = CIADIColors.TextSecondary))
                                }

                                if (!sessao.progresso.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "Progresso: ${sessao.progresso}", style = MaterialTheme.typography.bodySmall.copy(color = CIADIColors.SuccessGreen, fontWeight = FontWeight.Medium))
                                }

                                if (!sessao.comunicacaoFamilia.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "Comunicação Família: ${sessao.comunicacaoFamilia}", style = MaterialTheme.typography.bodySmall.copy(color = CIADIColors.TealPrimary))
                                }

                                if (!sessao.observacoes.isNullOrBlank() || !sessao.resumoObservacoes.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "Observações: ${sessao.observacoes ?: sessao.resumoObservacoes}", style = MaterialTheme.typography.bodySmall.copy(color = CIADIColors.TextSecondary))
                                }
                            }
                        }
                    }

                    items(acompanhamentosAba) { aba ->
                        CIADICards.Base(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Registro Diário ABA • ${aba.dataRegistro ?: "Hoje"}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CIADIColors.Brown)
                                )
                                if (!aba.comportamentoObservado.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "Comportamento: ${aba.comportamentoObservado}", style = MaterialTheme.typography.bodySmall)
                                }
                                if (!aba.progresso.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "Progresso: ${aba.progresso}", style = MaterialTheme.typography.bodySmall.copy(color = CIADIColors.SuccessGreen))
                                }
                                if (!aba.comunicacaoFamilia.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "Família: ${aba.comunicacaoFamilia}", style = MaterialTheme.typography.bodySmall.copy(color = CIADIColors.TealPrimary))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
