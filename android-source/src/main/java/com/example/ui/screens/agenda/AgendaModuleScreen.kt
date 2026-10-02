package com.example.ui.screens.agenda

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.remote.dto.PortalAgendamentoDto
import com.example.data.repository.SupabaseModulesRepositoryImpl
import com.example.domain.model.UserProfile
import com.example.domain.repository.ModulesRepository
import com.example.ui.components.CiadiTopBar
import com.example.ui.theme.CIADIColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaModuleScreen(
    user: UserProfile,
    onNavigateBack: () -> Unit,
    onNavigateToVirtualClinic: ((PortalAgendamentoDto) -> Unit)? = null,
    onNavigateToRoute: ((String) -> Unit)? = null,
    modulesRepository: ModulesRepository? = null,
    modifier: Modifier = Modifier
) {
    val repoImpl = modulesRepository as? SupabaseModulesRepositoryImpl
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Hoje", "Próximas", "Histórico")

    var isLoading by remember { mutableStateOf(false) }
    val agendaFlow = remember(repoImpl) {
        repoImpl?.observePortalAgendamentos() ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList<PortalAgendamentoDto>())
    }
    val agendamentosPortal by agendaFlow.collectAsState()

    var activeVirtualMeeting by remember { mutableStateOf<PortalAgendamentoDto?>(null) }

    fun refreshAgenda() {
        scope.launch {
            isLoading = true
            repoImpl?.fetchPortalAgendamentos(user.id)
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshAgenda()
    }

    // Classificação em abas: Hoje, Próximas, Histórico
    val hojeList: List<PortalAgendamentoDto> = remember(agendamentosPortal) {
        agendamentosPortal.filter { item ->
            item.dataHora.contains("2026-09-27") ||
                    item.estado?.equals("em_atendimento", ignoreCase = true) == true ||
                    item.estado?.equals("hoje", ignoreCase = true) == true
        }.ifEmpty {
            // Se nenhuma explícita de hoje, destaca a primeira agendada se houver
            agendamentosPortal.take(1)
        }
    }

    val proximasList: List<PortalAgendamentoDto> = remember(agendamentosPortal, hojeList) {
        agendamentosPortal.filter { item ->
            !hojeList.contains(item) &&
                    item.estado?.equals("concluida", ignoreCase = true) != true &&
                    item.estado?.equals("finalizado", ignoreCase = true) != true &&
                    item.estado?.equals("cancelada", ignoreCase = true) != true
        }
    }

    val historicoList: List<PortalAgendamentoDto> = remember(agendamentosPortal) {
        agendamentosPortal.filter { item ->
            item.estado?.equals("concluida", ignoreCase = true) == true ||
                    item.estado?.equals("finalizado", ignoreCase = true) == true ||
                    item.estado?.equals("cancelada", ignoreCase = true) == true ||
                    item.estado?.equals("faltou", ignoreCase = true) == true
        }
    }

    val currentList = when (selectedTabIndex) {
        0 -> hojeList
        1 -> proximasList
        else -> historicoList
    }

    Scaffold(
        topBar = {
            CiadiTopBar(
                title = "Calendário Inteligente",
                currentUser = user,
                onNavigateBack = onNavigateBack
            )
        },
        bottomBar = {
            onNavigateToRoute?.let { navigate ->
                com.example.ui.designsystem.CIADIBottomBar(
                    currentRoute = "agenda",
                    onNavigate = navigate
                )
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
        ) {
            // Abas de Navegação Temporal
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = CIADIColors.SurfaceWhite,
                contentColor = CIADIColors.OrangePrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = CIADIColors.OrangePrimary,
                        height = 3.dp
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTabIndex == index) CIADIColors.OrangePrimary else CIADIColors.TextSecondary
                                )
                            )
                        },
                        modifier = Modifier.testTag("agenda_tab_$index")
                    )
                }
            }

            // Barra de Atualização Rápida
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Fonte: ciadi_portal_agendamentos_autorizados",
                    style = MaterialTheme.typography.labelSmall.copy(color = CIADIColors.TextMuted)
                )
                IconButton(
                    onClick = { refreshAgenda() },
                    modifier = Modifier.size(32.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = CIADIColors.OrangePrimary, strokeWidth = 2.dp)
                    } else {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Atualizar", tint = CIADIColors.Brown)
                    }
                }
            }

            // Lista de Compromissos
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (currentList.isEmpty() && !isLoading) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CIADIColors.SurfaceWhite),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = CIADIColors.TextMuted,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Nenhuma consulta encontrada nesta categoria.",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = CIADIColors.TextPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Os compromissos agendados no Dashboard CIADI aparecerão aqui conforme as permissões RLS.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = CIADIColors.TextSecondary),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(currentList, key = { it.id }) { agendamento ->
                        AgendamentoCard(
                            item = agendamento,
                            onJoinVirtual = {
                                if (onNavigateToVirtualClinic != null) {
                                    onNavigateToVirtualClinic.invoke(agendamento)
                                } else {
                                    activeVirtualMeeting = agendamento
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal da Clínica Virtual se acionada
    activeVirtualMeeting?.let { meeting ->
        ClinicaVirtualDialog(
            agendamento = meeting,
            repository = repoImpl,
            onDismiss = { activeVirtualMeeting = null }
        )
    }
}

@Composable
fun AgendamentoCard(
    item: PortalAgendamentoDto,
    onJoinVirtual: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOnline = item.modalidade.equals("Online", ignoreCase = true) || !item.linkVideo.isNullOrBlank()

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CIADIColors.SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("agendamento_card_${item.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Topo do Card: Especialidade + Badge de Modalidade
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.especialidade ?: "Atendimento Especializado",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = CIADIColors.Brown
                    )
                )

                // Crachá de Modalidade: Presencial vs Online
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isOnline) Color(0xFFE0F2F1) else CIADIColors.CreamLight)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isOnline) Icons.Default.Videocam else Icons.Default.Business,
                        contentDescription = null,
                        tint = if (isOnline) CIADIColors.TealPrimary else CIADIColors.OrangePrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isOnline) "Consulta Online" else "Presencial",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isOnline) CIADIColors.TealPrimary else CIADIColors.Brown
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Profissional e Criança
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = CIADIColors.OrangePrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Profissional: ${item.profissional ?: "Equipe Multidisciplinar CIADI"}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = CIADIColors.TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                )
                if (!item.tituloProfissional.isNullOrBlank()) {
                    Text(
                        text = " (${item.tituloProfissional})",
                        style = MaterialTheme.typography.bodySmall.copy(color = CIADIColors.TextSecondary)
                    )
                }
            }

            if (!item.paciente.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Assistido: ${item.paciente}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = CIADIColors.TextSecondary,
                        fontSize = 12.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Linha com Data, Horário e Duração
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFAFBFC))
                    .border(1.dp, CIADIColors.Outline, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = CIADIColors.Brown,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.dataHora,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = CIADIColors.TextPrimary
                        )
                    )
                }

                Text(
                    text = "${item.duracao ?: item.duracaoMinutos ?: 50} min",
                    style = MaterialTheme.typography.labelSmall.copy(color = CIADIColors.TextSecondary)
                )
            }

            // Localização ou Acesso Online
            if (!item.salaNome.isNullOrBlank() && !isOnline) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Local: ${item.salaNome} (CIADI)",
                    style = MaterialTheme.typography.labelSmall.copy(color = CIADIColors.TextSecondary)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Ações / Estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Status Badge
                val estado = item.estado?.lowercase().orEmpty()
                val (statusColor, statusBg) = when {
                    estado.contains("confirm") || estado.contains("conclu") -> Pair(CIADIColors.SuccessGreen, Color(0xFFE8F5E9))
                    estado.contains("cancel") || estado.contains("falt") -> Pair(CIADIColors.ErrorRed, Color(0xFFFFEBEE))
                    estado.contains("atend") -> Pair(CIADIColors.TealPrimary, Color(0xFFE0F2F1))
                    else -> Pair(CIADIColors.OrangePrimary, CIADIColors.CreamLight)
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.estado?.replaceFirstChar { char -> char.uppercase() } ?: "Agendada",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    )
                }

                // Botão de Entrada se for Consulta Online
                if (isOnline) {
                    Button(
                        onClick = onJoinVirtual,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CIADIColors.TealPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Entrar na Consulta", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
