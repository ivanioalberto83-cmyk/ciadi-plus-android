package com.example.ui.screens.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupervisedUserCircle
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import com.example.ui.screens.at.AtEnvironmentScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.remote.dto.PacienteDto
import com.example.data.remote.dto.PortalAgendamentoDto
import com.example.data.repository.SupabaseModulesRepositoryImpl
import com.example.domain.model.SupabaseStatus
import com.example.domain.model.UserProfile
import com.example.domain.model.UserRole
import com.example.domain.repository.ModulesRepository
import com.example.ui.components.CiadiDynamicBackground
import com.example.ui.components.CiadiTopBar
import com.example.ui.components.RoleBadge
import com.example.ui.designsystem.CIADIBadge
import com.example.ui.designsystem.CIADIBottomBar
import com.example.ui.designsystem.CIADIButtons
import com.example.ui.designsystem.CIADICards
import com.example.ui.designsystem.CIADIShapes
import com.example.ui.theme.CIADIColors

@Composable
fun MainDashboardScreen(
    user: UserProfile,
    supabaseStatus: SupabaseStatus,
    onRoleSwitch: (UserRole) -> Unit,
    onNavigateToModule: (String) -> Unit,
    onNavigateToJaneth: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSos: () -> Unit,
    onNavigateToVirtualClinic: (PortalAgendamentoDto?) -> Unit,
    modulesRepository: ModulesRepository? = null,
    modifier: Modifier = Modifier
) {
    val repoImpl = modulesRepository as? SupabaseModulesRepositoryImpl
    val scope = rememberCoroutineScope()

    val patientsFlow = remember(repoImpl) {
        repoImpl?.observeAuthorizedPatients() ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList<PacienteDto>())
    }
    val authorizedPatients by patientsFlow.collectAsState()

    val selectedPatientFlow = remember(repoImpl) {
        repoImpl?.observeSelectedPatient() ?: kotlinx.coroutines.flow.MutableStateFlow<PacienteDto?>(null)
    }
    val selectedPatient by selectedPatientFlow.collectAsState()

    val agendamentosFlow = remember(repoImpl) {
        repoImpl?.observePortalAgendamentos() ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList<PortalAgendamentoDto>())
    }
    val agendamentosPortal by agendamentosFlow.collectAsState()

    val adminStatsFlow = remember(repoImpl) {
        repoImpl?.observeAdminStats() ?: kotlinx.coroutines.flow.MutableStateFlow(com.example.domain.repository.AdminStatsState())
    }
    val adminStats by adminStatsFlow.collectAsState()

    val popupsFlow = remember(repoImpl) {
        repoImpl?.observePopups() ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())
    }
    val popupsList by popupsFlow.collectAsState(initial = emptyList())
    var dismissedPopupId by remember { mutableStateOf<String?>(null) }
    var showPublicacoesDialog by remember { mutableStateOf(false) }
    val activePopup = remember(popupsList, dismissedPopupId, user.role) {
        popupsList.firstOrNull { it.id != dismissedPopupId && it.ativo && (it.publico || it.perfilAutorizado == null || it.perfilAutorizado.equals(user.role.code, true)) }
    }

    // Carrega o ambiente específico do A.T. (Item 2 do Prompt Mestre)
    if (user.role == UserRole.AT) {
        AtEnvironmentScreen(
            user = user,
            modulesRepository = modulesRepository,
            onNavigateToChat = { onNavigateToModule("chat") },
            onNavigateToProfile = onNavigateToProfile,
            onNavigateToNotifications = { onNavigateToModule("notifications") },
            onNavigateToSos = onNavigateToSos,
            onNavigateToJaneth = onNavigateToJaneth,
            onNavigateToForms = { onNavigateToModule("forms") },
            onSignOut = onNavigateToProfile,
            modifier = modifier
        )
        return
    }

    LaunchedEffect(user.id, user.role) {
        if (user.role == UserRole.RESPONSAVEL) {
            repoImpl?.syncFamilyBonds()
        }
        repoImpl?.fetchPortalAgendamentos(user.id)
        repoImpl?.atualizarEstatisticasReais()
        repoImpl?.sincronizarPopups()
        repoImpl?.sincronizarCalendarioInteligente()
    }

    // Popup Oficial do Sistema (Item 9)
    if (activePopup != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { dismissedPopupId = activePopup.id },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(CIADIColors.OrangePrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = activePopup.titulo,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = CIADIColors.Brown)
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Categoria: ${activePopup.categoria.uppercase()}",
                        style = MaterialTheme.typography.labelSmall.copy(color = CIADIColors.OrangePrimary, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = activePopup.mensagem,
                        style = MaterialTheme.typography.bodyMedium.copy(color = CIADIColors.TextPrimary, lineHeight = 20.sp)
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = { dismissedPopupId = activePopup.id },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = CIADIColors.OrangePrimary)
                ) {
                    Text("Compreendido", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showPublicacoesDialog) {
        PublicacoesPopupsDialog(
            popups = popupsList,
            onDismiss = { showPublicacoesDialog = false },
            onSavePopup = { newPop ->
                scope.launch {
                    repoImpl?.criarPublicacaoPopup(newPop)
                }
            }
        )
    }

    val proximaConsulta: PortalAgendamentoDto? = remember(agendamentosPortal) {
        agendamentosPortal.firstOrNull { item ->
            item.estado?.equals("concluida", ignoreCase = true) != true &&
                    item.estado?.equals("cancelada", ignoreCase = true) != true &&
                    item.estado?.equals("finalizado", ignoreCase = true) != true
        }
    }

    Scaffold(
        topBar = {
            CiadiTopBar(
                title = "CIADI+",
                currentUser = user,
                onProfileClick = onNavigateToProfile,
                onJanethClick = onNavigateToJaneth
            )
        },
        bottomBar = {
            CIADIBottomBar(
                currentRoute = "dashboard",
                onNavigate = { route ->
                    when (route) {
                        "dashboard" -> Unit
                        "agenda" -> onNavigateToModule("agenda")
                        "tracking" -> onNavigateToModule("tracking")
                        "janeth" -> onNavigateToJaneth()
                        "profile" -> onNavigateToProfile()
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        CiadiDynamicBackground(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize().testTag("dashboard_role_${user.role.code}")
            ) {
                // Header Dinâmico de Identidade por Role
                item {
                    DashboardHeaderCard(user = user)
                }

                // Conteúdo específico por Perfil (Resolução de Role sem fallbacks incorretos)
                when (user.role) {
                    UserRole.ADMIN -> {
                        adminDashboardSection(
                            user = user,
                            adminStats = adminStats,
                            proximaConsulta = proximaConsulta,
                            onNavigateToModule = onNavigateToModule,
                            onNavigateToVirtualClinic = onNavigateToVirtualClinic,
                            onNavigateToJaneth = onNavigateToJaneth,
                            onNavigateToProfile = onNavigateToProfile,
                            onOpenPublicacoes = { showPublicacoesDialog = true }
                        )
                    }
                    UserRole.GESTOR -> {
                        gestorDashboardSection(
                            user = user,
                            onNavigateToModule = onNavigateToModule,
                            onNavigateToJaneth = onNavigateToJaneth
                        )
                    }
                    UserRole.PROFISSIONAL -> {
                        profissionalDashboardSection(
                            user = user,
                            proximaConsulta = proximaConsulta,
                            onNavigateToModule = onNavigateToModule,
                            onNavigateToVirtualClinic = onNavigateToVirtualClinic,
                            onNavigateToSos = onNavigateToSos,
                            onNavigateToJaneth = onNavigateToJaneth
                        )
                    }
                    UserRole.AT -> {
                        atDashboardSection(
                            user = user,
                            onNavigateToModule = onNavigateToModule,
                            onNavigateToSos = onNavigateToSos,
                            onNavigateToJaneth = onNavigateToJaneth
                        )
                    }
                    UserRole.RESPONSAVEL -> {
                        familiaDashboardSection(
                            user = user,
                            authorizedPatients = authorizedPatients,
                            selectedPatient = selectedPatient,
                            proximaConsulta = proximaConsulta,
                            onSelectPatient = { repoImpl?.selectPatient(it) },
                            onNavigateToModule = onNavigateToModule,
                            onNavigateToVirtualClinic = onNavigateToVirtualClinic,
                            onNavigateToSos = onNavigateToSos,
                            onNavigateToJaneth = onNavigateToJaneth
                        )
                    }
                    UserRole.PACIENTE -> {
                        pacienteDashboardSection(
                            user = user,
                            onNavigateToModule = onNavigateToModule,
                            onNavigateToSos = onNavigateToSos,
                            onNavigateToJaneth = onNavigateToJaneth
                        )
                    }
                    UserRole.OPERADOR -> {
                        operadorDashboardSection(
                            user = user,
                            onNavigateToModule = onNavigateToModule,
                            onNavigateToJaneth = onNavigateToJaneth
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// CABEÇALHO OFICIAL DO DASHBOARD
// -------------------------------------------------------------
@Composable
private fun DashboardHeaderCard(user: UserProfile) {
    CIADICards.Base(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_ciadi_logo_1790332105105),
                    contentDescription = "Logo Oficial CIADI",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (user.role) {
                            UserRole.ADMIN -> "Administração Central"
                            UserRole.GESTOR -> "Gestão & Coordenação"
                            UserRole.PROFISSIONAL -> "Área Clínica Especializada"
                            UserRole.AT -> "A.T. — Acompanhamento Terapêutico"
                            UserRole.RESPONSAVEL -> "Portal da Família"
                            UserRole.PACIENTE -> "Espaço do Assistido"
                            UserRole.OPERADOR -> "Recepção & Triagem"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = CIADIColors.Brown,
                            fontSize = 17.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${user.fullName} • ${user.unitName ?: "CIADI"}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = CIADIColors.TextSecondary,
                            fontSize = 12.sp
                        ),
                        maxLines = 1
                    )
                }
                RoleBadge(role = user.role)
            }
        }
    }
}

// -------------------------------------------------------------
// SEÇÃO ADMIN
// -------------------------------------------------------------
private fun androidx.compose.foundation.lazy.LazyListScope.adminDashboardSection(
    user: UserProfile,
    adminStats: com.example.domain.repository.AdminStatsState,
    proximaConsulta: PortalAgendamentoDto?,
    onNavigateToModule: (String) -> Unit,
    onNavigateToVirtualClinic: (PortalAgendamentoDto?) -> Unit,
    onNavigateToJaneth: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onOpenPublicacoes: () -> Unit
) {
    item {
        CIADICards.Warm(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CIADIColors.RoleAdmin),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = "Visão Global da Unidade", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CIADIColors.Brown))
                        Text(text = "Auditoria e Governança do Ecossistema", style = MaterialTheme.typography.labelSmall.copy(color = CIADIColors.TextSecondary))
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AdminMetricChip(number = if (adminStats.assistidosCount > 0) adminStats.assistidosCount.toString() else "—", label = "Assistidos")
                    AdminMetricChip(number = if (adminStats.profissionaisCount > 0) adminStats.profissionaisCount.toString() else "—", label = "Profissionais")
                    AdminMetricChip(number = if (adminStats.atAtivosCount > 0) adminStats.atAtivosCount.toString() else "—", label = "A.T.s Ativos")
                    AdminMetricChip(number = if (adminStats.consultasCount > 0) adminStats.consultasCount.toString() else "100%", label = if (adminStats.consultasCount > 0) "Consultas" else "RLS Ativo")
                }
            }
        }
    }

    item {
        Text(text = "Gestão Institucional", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = CIADIColors.Brown))
    }

    item {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigAccessCard(
                title = "Sala de Vídeo",
                subtitle = "Clínica Virtual",
                icon = Icons.Default.Videocam,
                iconColor = CIADIColors.TealPrimary,
                onClick = { onNavigateToVirtualClinic(proximaConsulta) },
                modifier = Modifier.weight(1f)
            )
            BigAccessCard(
                title = "Agenda Geral",
                subtitle = "Salas & Consultas",
                icon = Icons.Default.CalendarMonth,
                iconColor = CIADIColors.OrangePrimary,
                onClick = { onNavigateToModule("agenda") },
                modifier = Modifier.weight(1f)
            )
            BigAccessCard(
                title = "Equipe & Famílias",
                subtitle = "Vínculos e Perfis",
                icon = Icons.Default.Groups,
                iconColor = CIADIColors.TealPrimary,
                onClick = { onNavigateToModule("tracking") },
                modifier = Modifier.weight(1f)
            )
        }
    }

    item {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigAccessCard(
                title = "Documentos",
                subtitle = "Histórico Clínico",
                icon = Icons.Default.Folder,
                iconColor = CIADIColors.Brown,
                onClick = { onNavigateToModule("documents") },
                modifier = Modifier.weight(1f)
            )
            BigAccessCard(
                title = "Formulários",
                subtitle = "Protocolos & P.E.I.",
                icon = Icons.Default.Assignment,
                iconColor = CIADIColors.RoleGestor,
                onClick = { onNavigateToModule("forms") },
                modifier = Modifier.weight(1f)
            )
        }
    }

    item {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigAccessCard(
                title = "Publicações & Popups",
                subtitle = "Avisos & Comunicados",
                icon = Icons.Default.Notifications,
                iconColor = CIADIColors.OrangePrimary,
                onClick = onOpenPublicacoes,
                modifier = Modifier.weight(1f)
            )
            BigAccessCard(
                title = "Notificações",
                subtitle = "Central de Alertas",
                icon = Icons.Default.Notifications,
                iconColor = CIADIColors.TealPrimary,
                onClick = { onNavigateToModule("notifications") },
                modifier = Modifier.weight(1f)
            )
        }
    }

    item {
        Text(text = "Comunicação Institucional", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = CIADIColors.Brown))
    }

    item {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigAccessCard(
                title = "Chat Equipa",
                subtitle = "Canal interno seguro",
                icon = Icons.Default.Groups,
                iconColor = CIADIColors.RoleAdmin,
                onClick = { onNavigateToModule("chat") },
                modifier = Modifier.weight(1f)
            )
            BigAccessCard(
                title = "Chat Família",
                subtitle = "Suporte aos pais",
                icon = Icons.AutoMirrored.Filled.Chat,
                iconColor = CIADIColors.OrangePrimary,
                onClick = { onNavigateToModule("chat") },
                modifier = Modifier.weight(1f)
            )
        }
    }

    item {
        JanethBannerCard(onClick = onNavigateToJaneth)
    }
}

// -------------------------------------------------------------
// SEÇÃO GESTOR
// -------------------------------------------------------------
private fun androidx.compose.foundation.lazy.LazyListScope.gestorDashboardSection(
    user: UserProfile,
    onNavigateToModule: (String) -> Unit,
    onNavigateToJaneth: () -> Unit
) {
    item {
        CIADICards.Warm(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(CIADIColors.RoleGestor), contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.SupervisedUserCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = "Coordenação Clínica", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CIADIColors.Brown))
                        Text(text = "Supervisão de A.T. e Fluxo de Atendimento", style = MaterialTheme.typography.labelSmall.copy(color = CIADIColors.TextSecondary))
                    }
                }
            }
        }
    }

    item {
        Text(text = "Painel de Coordenação", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = CIADIColors.Brown))
    }

    item {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigAccessCard(
                title = "Supervisões A.T.",
                subtitle = "Acompanhamento em campo",
                icon = Icons.Default.Timeline,
                iconColor = CIADIColors.RoleAt,
                onClick = { onNavigateToModule("tracking") },
                modifier = Modifier.weight(1f)
            )
            BigAccessCard(
                title = "Agenda das Salas",
                subtitle = "Alocação e horários",
                icon = Icons.Default.CalendarMonth,
                iconColor = CIADIColors.OrangePrimary,
                onClick = { onNavigateToModule("agenda") },
                modifier = Modifier.weight(1f)
            )
        }
    }

    item {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigAccessCard(
                title = "Chat Equipa",
                subtitle = "Comunicação interna",
                icon = Icons.Default.Groups,
                iconColor = CIADIColors.TealPrimary,
                onClick = { onNavigateToModule("chat") },
                modifier = Modifier.weight(1f)
            )
            BigAccessCard(
                title = "Notificações",
                subtitle = "Alertas e pendências",
                icon = Icons.Default.Notifications,
                iconColor = CIADIColors.Brown,
                onClick = { onNavigateToModule("notifications") },
                modifier = Modifier.weight(1f)
            )
        }
    }

    item {
        JanethBannerCard(onClick = onNavigateToJaneth)
    }
}

// -------------------------------------------------------------
// SEÇÃO PROFISSIONAL ESPECIALIZADO
// -------------------------------------------------------------
private fun androidx.compose.foundation.lazy.LazyListScope.profissionalDashboardSection(
    user: UserProfile,
    proximaConsulta: PortalAgendamentoDto?,
    onNavigateToModule: (String) -> Unit,
    onNavigateToVirtualClinic: (PortalAgendamentoDto?) -> Unit,
    onNavigateToSos: () -> Unit,
    onNavigateToJaneth: () -> Unit
) {
    item {
        CIADICards.Base(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(text = "Próximo Atendimento Clínico", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = CIADIColors.Brown))
                Spacer(modifier = Modifier.height(10.dp))
                if (proximaConsulta != null) {
                    val isOnline = proximaConsulta.modalidade?.equals("Online", ignoreCase = true) == true || !proximaConsulta.linkVideo.isNullOrBlank()
                    Text(text = "Assistido: ${proximaConsulta.paciente}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CIADIColors.TextPrimary))
                    Text(text = "${proximaConsulta.especialidade} • ${proximaConsulta.dataHora}", style = MaterialTheme.typography.bodySmall.copy(color = CIADIColors.TextSecondary))
                    Spacer(modifier = Modifier.height(12.dp))
                    if (isOnline) {
                        CIADIButtons.Primary(
                            text = "ENTRAR NA CONSULTA VIRTUAL",
                            onClick = { onNavigateToVirtualClinic(proximaConsulta) },
                            icon = Icons.Default.Videocam
                        )
                    }
                } else {
                    Text(text = "Nenhum paciente agendado para o momento.", style = MaterialTheme.typography.bodyMedium.copy(color = CIADIColors.TextSecondary))
                }
            }
        }
    }

    item {
        Text(text = "Menu Clínico", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = CIADIColors.Brown))
    }

    item {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigAccessCard(
                title = "Minha Agenda",
                subtitle = "Meus horários",
                icon = Icons.Default.CalendarMonth,
                iconColor = CIADIColors.OrangePrimary,
                onClick = { onNavigateToModule("agenda") },
                modifier = Modifier.weight(1f)
            )
            BigAccessCard(
                title = "Clínica Virtual",
                subtitle = "Sala de vídeo",
                icon = Icons.Default.Videocam,
                iconColor = CIADIColors.TealPrimary,
                onClick = { onNavigateToVirtualClinic(proximaConsulta) },
                modifier = Modifier.weight(1f)
            )
        }
    }

    item {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigAccessCard(
                title = "Pacientes & PEI",
                subtitle = "Evolução e metas",
                icon = Icons.Default.Timeline,
                iconColor = CIADIColors.TealPrimary,
                onClick = { onNavigateToModule("tracking") },
                modifier = Modifier.weight(1f)
            )
            BigAccessCard(
                title = "Formulários",
                subtitle = "Avaliações clínicas",
                icon = Icons.Default.Assignment,
                iconColor = CIADIColors.Brown,
                onClick = { onNavigateToModule("forms") },
                modifier = Modifier.weight(1f)
            )
        }
    }

    item {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigAccessCard(
                title = "Documentos Clínicos",
                subtitle = "Relatórios e registos",
                icon = Icons.Default.Folder,
                iconColor = CIADIColors.Brown,
                onClick = { onNavigateToModule("documents") },
                modifier = Modifier.weight(1f)
            )
            BigAccessCard(
                title = "Chat Equipa",
                subtitle = "Comunicação segura",
                icon = Icons.Default.Groups,
                iconColor = CIADIColors.RoleProfessional,
                onClick = { onNavigateToModule("chat") },
                modifier = Modifier.weight(1f)
            )
        }
    }

    item {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigAccessCard(
                title = "Notificações",
                subtitle = "Alertas e pendências",
                icon = Icons.Default.Notifications,
                iconColor = CIADIColors.OrangePrimary,
                onClick = { onNavigateToModule("notifications") },
                modifier = Modifier.weight(1f)
            )
            BigAccessCard(
                title = "SOS CIADI",
                subtitle = "Alerta e assistência",
                icon = Icons.Default.Warning,
                iconColor = CIADIColors.ErrorRed,
                onClick = onNavigateToSos,
                modifier = Modifier.weight(1f)
            )
        }
    }

    item {
        JanethBannerCard(onClick = onNavigateToJaneth)
    }
}

// -------------------------------------------------------------
// SEÇÃO A.T. (ACOMPANHANTE TERAPÊUTICO)
// -------------------------------------------------------------
private fun androidx.compose.foundation.lazy.LazyListScope.atDashboardSection(
    user: UserProfile,
    onNavigateToModule: (String) -> Unit,
    onNavigateToSos: () -> Unit,
    onNavigateToJaneth: () -> Unit
) {
    item {
        CIADICards.Warm(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(CIADIColors.RoleAt), contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.School, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = "Área de Atuação A.T.", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CIADIColors.Brown))
                        Text(text = "Acompanhamento em Ambiente Escolar e Rotina", style = MaterialTheme.typography.labelSmall.copy(color = CIADIColors.TextSecondary))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Atribuições ativas carregadas via ciadi_atribuicoes_at", style = MaterialTheme.typography.bodySmall.copy(color = CIADIColors.TextPrimary, fontWeight = FontWeight.Medium))
            }
        }
    }

    item {
        Text(text = "Ferramentas do A.T.", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = CIADIColors.Brown))
    }

    item {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigAccessCard(
                title = "Registros A.T.",
                subtitle = "Sessões e P.E.I.",
                icon = Icons.Default.Assignment,
                iconColor = CIADIColors.RoleAt,
                onClick = { onNavigateToModule("tracking") },
                modifier = Modifier.weight(1f)
            )
            BigAccessCard(
                title = "Minha Agenda",
                subtitle = "Horários em campo",
                icon = Icons.Default.CalendarMonth,
                iconColor = CIADIColors.OrangePrimary,
                onClick = { onNavigateToModule("agenda") },
                modifier = Modifier.weight(1f)
            )
        }
    }

    item {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigAccessCard(
                title = "Chat com Família",
                subtitle = "Contato com os pais",
                icon = Icons.AutoMirrored.Filled.Chat,
                iconColor = CIADIColors.TealPrimary,
                onClick = { onNavigateToModule("chat") },
                modifier = Modifier.weight(1f)
            )
            BigAccessCard(
                title = "Chat Equipa",
                subtitle = "Suporte supervisor",
                icon = Icons.Default.Groups,
                iconColor = CIADIColors.Brown,
                onClick = { onNavigateToModule("chat") },
                modifier = Modifier.weight(1f)
            )
        }
    }

    item {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigAccessCard(
                title = "SOS em Campo",
                subtitle = "Alerta de crise",
                icon = Icons.Default.Warning,
                iconColor = CIADIColors.ErrorRed,
                onClick = onNavigateToSos,
                modifier = Modifier.weight(1f)
            )
            BigAccessCard(
                title = "Notificações",
                subtitle = "Recados e avisos",
                icon = Icons.Default.Notifications,
                iconColor = CIADIColors.RoleProfessional,
                onClick = { onNavigateToModule("notifications") },
                modifier = Modifier.weight(1f)
            )
        }
    }

    item {
        JanethBannerCard(onClick = onNavigateToJaneth)
    }
}

// -------------------------------------------------------------
// SEÇÃO RESPONSÁVEL / FAMÍLIA (PORTAL DA FAMÍLIA)
// -------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
private fun androidx.compose.foundation.lazy.LazyListScope.familiaDashboardSection(
    user: UserProfile,
    authorizedPatients: List<PacienteDto>,
    selectedPatient: PacienteDto?,
    proximaConsulta: PortalAgendamentoDto?,
    onSelectPatient: (PacienteDto) -> Unit,
    onNavigateToModule: (String) -> Unit,
    onNavigateToVirtualClinic: (PortalAgendamentoDto?) -> Unit,
    onNavigateToSos: () -> Unit,
    onNavigateToJaneth: () -> Unit
) {
    item {
        CIADICards.Warm(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CIADIColors.OrangePrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.ChildCare, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = "Criança Acompanhada", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CIADIColors.Brown))
                            Text(text = "Vinculada no Supabase via RLS", style = MaterialTheme.typography.labelSmall.copy(color = CIADIColors.TextSecondary, fontSize = 11.sp))
                        }
                    }

                    if (authorizedPatients.size > 1) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(CIADIColors.Cream)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = "${authorizedPatients.size} assistidos", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = CIADIColors.Brown))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (authorizedPatients.isEmpty() && user.activePatientName.isNullOrBlank()) {
                    Text(
                        text = "O seu acesso foi autenticado, mas ainda não existe uma criança/paciente associado ao seu perfil. Contacte a Secretaria do CIADI.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = CIADIColors.TextPrimary,
                            lineHeight = 20.sp
                        )
                    )
                } else if (authorizedPatients.isEmpty()) {
                    val fallback = user.activePatientName ?: "Assistido vinculado"
                    Text(text = fallback, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = CIADIColors.TextPrimary))
                } else if (authorizedPatients.size == 1) {
                    val child = authorizedPatients.first()
                    Text(text = child.nome, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = CIADIColors.TextPrimary, fontSize = 20.sp))
                    if (!child.diagnosticoResumo.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = child.diagnosticoResumo, style = MaterialTheme.typography.bodySmall.copy(color = CIADIColors.TextSecondary))
                    }
                } else {
                    Text(text = "Selecionar criança:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = CIADIColors.Brown))
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        authorizedPatients.forEach { paciente ->
                            val isSelected = selectedPatient?.id == paciente.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectPatient(paciente) },
                                label = {
                                    Text(text = paciente.nome, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CIADIColors.OrangePrimary, selectedLabelColor = Color.White),
                                modifier = Modifier.testTag("child_chip_${paciente.id}")
                            )
                        }
                    }
                }
            }
        }
    }

    item {
        CIADICards.Base(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Próxima Consulta", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = CIADIColors.Brown))
                    Text(
                        text = "Ver Calendário",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = CIADIColors.OrangePrimary),
                        modifier = Modifier.clickable { onNavigateToModule("agenda") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (proximaConsulta != null) {
                    val isOnline = proximaConsulta.modalidade?.equals("Online", ignoreCase = true) == true || !proximaConsulta.linkVideo.isNullOrBlank()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = proximaConsulta.especialidade ?: "Consulta Clínica", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = CIADIColors.TextPrimary))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = "Profissional: ${proximaConsulta.profissional ?: "Equipe CIADI"}", style = MaterialTheme.typography.bodySmall.copy(color = CIADIColors.TextSecondary))
                        }
                        CIADIBadge.Modalidade(isOnline = isOnline)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(CIADIColors.CreamLight).padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.AccessTime, contentDescription = null, tint = CIADIColors.Brown, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = proximaConsulta.dataHora, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = CIADIColors.TextPrimary))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "(${proximaConsulta.estado ?: "Confirmada"})", style = MaterialTheme.typography.labelSmall.copy(color = CIADIColors.SuccessGreen))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isOnline) {
                        CIADIButtons.Primary(
                            text = "ENTRAR NA CONSULTA",
                            onClick = { onNavigateToVirtualClinic(proximaConsulta) },
                            icon = Icons.Default.Videocam,
                            modifier = Modifier.testTag("home_enter_virtual_consultation")
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Business, contentDescription = null, tint = CIADIColors.TextSecondary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Atendimento presencial na sede do CIADI.", style = MaterialTheme.typography.bodySmall.copy(color = CIADIColors.TextSecondary))
                        }
                    }
                } else {
                    Text(text = "Nenhuma consulta pendente no momento.", style = MaterialTheme.typography.bodyMedium.copy(color = CIADIColors.TextSecondary))
                }
            }
        }
    }

    item {
        Text(text = "Acesso Rápido — Família", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = CIADIColors.Brown))
    }

    item {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigAccessCard(
                title = "Calendário",
                subtitle = "Consultas e P.E.I.",
                icon = Icons.Default.CalendarMonth,
                iconColor = CIADIColors.OrangePrimary,
                onClick = { onNavigateToModule("agenda") },
                modifier = Modifier.weight(1f)
            )
            BigAccessCard(
                title = "Clínica Virtual",
                subtitle = "Teleconsulta online",
                icon = Icons.Default.Videocam,
                iconColor = CIADIColors.TealPrimary,
                onClick = { onNavigateToVirtualClinic(proximaConsulta) },
                modifier = Modifier.weight(1f)
            )
        }
    }

    item {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigAccessCard(
                title = "Acompanhamento",
                subtitle = "Metas e evolução",
                icon = Icons.Default.Timeline,
                iconColor = CIADIColors.Brown,
                onClick = { onNavigateToModule("tracking") },
                modifier = Modifier.weight(1f)
            )
            BigAccessCard(
                title = "SOS Família",
                subtitle = "Ajuda emergencial",
                icon = Icons.Default.Warning,
                iconColor = CIADIColors.ErrorRed,
                onClick = onNavigateToSos,
                modifier = Modifier.weight(1f)
            )
        }
    }

    item {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigAccessCard(
                title = "Chat Família",
                subtitle = "Contato com equipe",
                icon = Icons.AutoMirrored.Filled.Chat,
                iconColor = CIADIColors.OrangePrimary,
                onClick = { onNavigateToModule("chat") },
                modifier = Modifier.weight(1f)
            )
            BigAccessCard(
                title = "Documentos",
                subtitle = "Relatórios clínicos",
                icon = Icons.Default.Folder,
                iconColor = CIADIColors.TealPrimary,
                onClick = { onNavigateToModule("documents") },
                modifier = Modifier.weight(1f)
            )
        }
    }

    item {
        JanethBannerCard(onClick = onNavigateToJaneth)
    }
}

// -------------------------------------------------------------
// SEÇÃO PACIENTE
// -------------------------------------------------------------
private fun androidx.compose.foundation.lazy.LazyListScope.pacienteDashboardSection(
    user: UserProfile,
    onNavigateToModule: (String) -> Unit,
    onNavigateToSos: () -> Unit,
    onNavigateToJaneth: () -> Unit
) {
    item {
        CIADICards.Base(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(text = "Olá, ${user.fullName}!", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = CIADIColors.Brown))
                Text(text = "Veja suas atividades e sessões de desenvolvimento.", style = MaterialTheme.typography.bodySmall.copy(color = CIADIColors.TextSecondary))
            }
        }
    }
    item {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigAccessCard(title = "Meus Horários", subtitle = "Agenda", icon = Icons.Default.CalendarMonth, iconColor = CIADIColors.OrangePrimary, onClick = { onNavigateToModule("agenda") }, modifier = Modifier.weight(1f))
            BigAccessCard(title = "SOS", subtitle = "Preciso de ajuda", icon = Icons.Default.Warning, iconColor = CIADIColors.ErrorRed, onClick = onNavigateToSos, modifier = Modifier.weight(1f))
        }
    }
    item { JanethBannerCard(onClick = onNavigateToJaneth) }
}

// -------------------------------------------------------------
// SEÇÃO OPERADOR
// -------------------------------------------------------------
private fun androidx.compose.foundation.lazy.LazyListScope.operadorDashboardSection(
    user: UserProfile,
    onNavigateToModule: (String) -> Unit,
    onNavigateToJaneth: () -> Unit
) {
    item {
        CIADICards.Base(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(text = "Recepção & Triagem", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = CIADIColors.Brown))
                Text(text = "Controle de entrada e agendamentos de salas", style = MaterialTheme.typography.bodySmall.copy(color = CIADIColors.TextSecondary))
            }
        }
    }
    item {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigAccessCard(title = "Agenda de Salas", subtitle = "Horários", icon = Icons.Default.CalendarMonth, iconColor = CIADIColors.OrangePrimary, onClick = { onNavigateToModule("agenda") }, modifier = Modifier.weight(1f))
            BigAccessCard(title = "Notificações", subtitle = "Avisos da unidade", icon = Icons.Default.Notifications, iconColor = CIADIColors.Brown, onClick = { onNavigateToModule("notifications") }, modifier = Modifier.weight(1f))
        }
    }
    item { JanethBannerCard(onClick = onNavigateToJaneth) }
}

// -------------------------------------------------------------
// COMPONENTES AUXILIARES
// -------------------------------------------------------------
@Composable
private fun AdminMetricChip(number: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = number, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = CIADIColors.Brown))
        Text(text = label, style = MaterialTheme.typography.labelSmall.copy(color = CIADIColors.TextSecondary, fontSize = 11.sp))
    }
}

@Composable
private fun JanethBannerCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("dashboard_janeth_banner"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CIADIColors.Cream)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.janeth_avatar_1790333481531),
                contentDescription = "Janeth Assistente",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(52.dp).clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Converse com a Janeth", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CIADIColors.Brown))
                Text(text = "Orientação institucional e dúvidas frequentes", style = MaterialTheme.typography.bodySmall.copy(color = CIADIColors.TextSecondary, fontSize = 12.sp))
            }
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = CIADIColors.Brown)
        }
    }
}

@Composable
private fun BigAccessCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable { onClick() }
            .testTag("big_access_${title.lowercase().replace(" ", "_")}"),
        shape = CIADIShapes.Large,
        colors = CardDefaults.cardColors(containerColor = CIADIColors.SurfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, CIADIColors.Outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(text = title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CIADIColors.TextPrimary, fontSize = 14.sp), maxLines = 1)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall.copy(color = CIADIColors.TextSecondary, fontSize = 11.sp), maxLines = 1)
            }
        }
    }
}

// -------------------------------------------------------------
// DIÁLOGO DE GESTÃO DE PUBLICAÇÕES E POPUPS (Itens 9 e 10)
// -------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PublicacoesPopupsDialog(
    popups: List<com.example.data.remote.dto.PopupComunicadoDto>,
    onDismiss: () -> Unit,
    onSavePopup: (com.example.data.remote.dto.PopupComunicadoDto) -> Unit
) {
    var isCreating by remember { mutableStateOf(false) }
    var titulo by remember { mutableStateOf("") }
    var mensagem by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf("anuncio") }
    var publicoAlvo by remember { mutableStateOf("todos") }
    var prioridade by remember { mutableStateOf("NORMAL") }
    var ativo by remember { mutableStateOf(true) }
    var imageUrl by remember { mutableStateOf("") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CIADIColors.OrangePrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isCreating) "Nova Publicação / Popup" else "Publicações & Popups",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = CIADIColors.Brown)
                    )
                    Text(
                        text = "Gestão de Comunicados e Alertas do CIADI+",
                        style = MaterialTheme.typography.labelSmall.copy(color = CIADIColors.TextSecondary)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                if (isCreating) {
                    OutlinedTextField(
                        value = titulo,
                        onValueChange = { titulo = it },
                        label = { Text("Título da Publicação") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_popup_titulo")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = mensagem,
                        onValueChange = { mensagem = it },
                        label = { Text("Mensagem / Descrição") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth().testTag("input_popup_mensagem")
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Categoria:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(4.dp))
                    val categorias = listOf("anuncio", "efemeride", "aniversario", "comunicado", "seguranca", "janeth", "sos", "atualizacao")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        categorias.forEach { cat ->
                            FilterChip(
                                selected = categoria == cat,
                                onClick = { categoria = cat },
                                label = { Text(cat.uppercase(), fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CIADIColors.CreamLight,
                                    selectedLabelColor = CIADIColors.OrangePrimary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Público-Alvo:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(4.dp))
                    val publicos = listOf("todos", "responsavel", "profissional", "at", "operador")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        publicos.forEach { pub ->
                            FilterChip(
                                selected = publicoAlvo == pub,
                                onClick = { publicoAlvo = pub },
                                label = { Text(pub.replace("responsavel", "família").uppercase(), fontSize = 10.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = { imageUrl = it },
                        label = { Text("URL da Imagem (Supabase Storage)") },
                        placeholder = { Text("https://.../storage/v1/object/public/...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Publicação Ativa", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = ativo,
                            onCheckedChange = { ativo = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = CIADIColors.TealPrimary)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Publicações Ativas (${popups.size})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Button(
                            onClick = { isCreating = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CIADIColors.OrangePrimary)
                        ) {
                            Text("➕ Nova", fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    if (popups.isEmpty()) {
                        Text(
                            text = "Nenhuma publicação ou popup configurado no momento.",
                            style = MaterialTheme.typography.bodySmall.copy(color = CIADIColors.TextSecondary)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            popups.forEach { p ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = CIADIColors.CreamLight),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = p.titulo,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CIADIColors.Brown)
                                            )
                                            Text(
                                                text = p.categoria.uppercase(),
                                                style = MaterialTheme.typography.labelSmall.copy(color = CIADIColors.OrangePrimary, fontWeight = FontWeight.Bold)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = p.mensagem, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isCreating) {
                Button(
                    onClick = {
                        if (titulo.isNotBlank() && mensagem.isNotBlank()) {
                            val newPop = com.example.data.remote.dto.PopupComunicadoDto(
                                id = "pop_${System.currentTimeMillis()}",
                                titulo = titulo.trim(),
                                mensagem = mensagem.trim(),
                                categoria = categoria,
                                prioridade = prioridade,
                                ativo = ativo,
                                publico = publicoAlvo == "todos",
                                perfilAutorizado = if (publicoAlvo == "todos") null else publicoAlvo
                            )
                            onSavePopup(newPop)
                            isCreating = false
                            titulo = ""
                            mensagem = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CIADIColors.TealPrimary)
                ) {
                    Text("Publicar no Supabase")
                }
            } else {
                Button(onClick = onDismiss) {
                    Text("Fechar")
                }
            }
        },
        dismissButton = {
            if (isCreating) {
                androidx.compose.material3.TextButton(onClick = { isCreating = false }) {
                    Text("Cancelar")
                }
            }
        }
    )
}