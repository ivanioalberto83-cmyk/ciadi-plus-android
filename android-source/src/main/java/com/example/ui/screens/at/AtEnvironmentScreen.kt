package com.example.ui.screens.at

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CorporateFare
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.dto.AtAtividadeDto
import com.example.data.remote.dto.AtCalendarioEventoDto
import com.example.data.remote.dto.AtContactoDto
import com.example.data.remote.dto.AtOrganizacaoDto
import com.example.data.remote.dto.AtSessaoDto
import com.example.data.remote.dto.PacienteDto
import com.example.data.repository.SupabaseModulesRepositoryImpl
import com.example.domain.model.UserProfile
import com.example.domain.repository.ModulesRepository
import com.example.ui.components.CiadiTopBar
import com.example.ui.components.RoleBadge
import com.example.ui.designsystem.CIADIButtons
import com.example.ui.designsystem.CIADICards
import com.example.ui.theme.CIADIColors
import kotlinx.coroutines.launch

/**
 * Menu Oficial do Perfil A.T. (Item 2 do Prompt Mestre):
 * 1. Início
 * 2. Meus Assistidos
 * 3. Calendário
 * 4. Atividades
 * 5. Atividades +
 * 6. Acompanhamento
 * 7. Chat
 * 8. Organização
 * 9. Notificações
 * 10. Meu Perfil
 * 11. Formulários Clínicos (ABC • ABA • Diagnóstico / Avaliação)
 */
enum class AtMenuSection(val label: String, val icon: ImageVector, val tag: String) {
    INICIO("Início", Icons.Default.Home, "menu_at_inicio"),
    ASSISTIDOS("Meus Assistidos", Icons.Default.School, "menu_at_assistidos"),
    CALENDARIO("Calendário", Icons.Default.CalendarMonth, "menu_at_calendario"),
    ATIVIDADES("Atividades", Icons.Default.EventNote, "menu_at_atividades"),
    ATIVIDADES_PLUS("Atividades +", Icons.Default.Add, "menu_at_atividades_plus"),
    ACOMPANHAMENTO("Acompanhamento", Icons.Default.AssignmentTurnedIn, "menu_at_acompanhamento"),
    CHAT("Chat", Icons.AutoMirrored.Filled.Chat, "menu_at_chat"),
    ORGANIZACAO("Organização", Icons.Default.CorporateFare, "menu_at_organizacao"),
    NOTIFICACOES("Notificações", Icons.Default.Notifications, "menu_at_notificacoes"),
    PERFIL("Meu Perfil", Icons.Default.Person, "menu_at_perfil"),
    FORMULARIOS("Formulários", Icons.Default.Assignment, "menu_at_formularios")
}

@Composable
fun AtEnvironmentScreen(
    user: UserProfile,
    modulesRepository: ModulesRepository?,
    onNavigateToChat: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToSos: () -> Unit,
    onNavigateToJaneth: () -> Unit,
    onNavigateToForms: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val repoImpl = modulesRepository as? SupabaseModulesRepositoryImpl
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedSection by remember { mutableStateOf(AtMenuSection.INICIO) }
    var showNovaAtividadeDialog by remember { mutableStateOf(false) }
    var showNovoAcompanhamentoDialog by remember { mutableStateOf(false) }
    var selectedEventoParaDetalhes by remember { mutableStateOf<AtCalendarioEventoDto?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Observa dados reais do repositório A.T.
    val assistidosFlow = remember(repoImpl) {
        repoImpl?.observeAssistidos() ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList<PacienteDto>())
    }
    val assistidos by assistidosFlow.collectAsState()

    val atribuicoesFlow = remember(repoImpl) {
        repoImpl?.observeAtribuicoes() ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList<com.example.data.remote.dto.AtribuicaoAtDto>())
    }
    val atribuicoes by atribuicoesFlow.collectAsState()

    val atividadesFlow = remember(repoImpl) {
        repoImpl?.observeAtividades() ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList<AtAtividadeDto>())
    }
    val atividades by atividadesFlow.collectAsState()

    val calendarioFlow = remember(repoImpl) {
        repoImpl?.observeCalendario() ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList<AtCalendarioEventoDto>())
    }
    val calendarioEventos by calendarioFlow.collectAsState()

    val sessoesFlow = remember(repoImpl) {
        repoImpl?.observeSessoes() ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList<AtSessaoDto>())
    }
    val sessoes by sessoesFlow.collectAsState()

    val abaFlow = remember(repoImpl) {
        repoImpl?.observeAcompanhamentosAba() ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList<com.example.data.remote.dto.AcompanhamentoDiarioAbaDto>())
    }
    val acompanhamentosAba by abaFlow.collectAsState()

    val contactosFlow = remember(repoImpl) {
        repoImpl?.observeContactos() ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList<AtContactoDto>())
    }
    val contactos by contactosFlow.collectAsState()

    val organizacaoFlow = remember(repoImpl) {
        repoImpl?.observeOrganizacao() ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList<AtOrganizacaoDto>())
    }
    val organizacao by organizacaoFlow.collectAsState()

    val notificacoesFlow = remember(repoImpl) {
        repoImpl?.observeNotificacoes() ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList<com.example.data.remote.dto.NotificacaoDto>())
    }
    val notificacoes by notificacoesFlow.collectAsState()

    val profissionalFlow = remember(repoImpl) {
        repoImpl?.observeProfissionalLogado() ?: kotlinx.coroutines.flow.MutableStateFlow<com.example.data.remote.dto.ProfissionalDto?>(null)
    }
    val profissionalLogado by profissionalFlow.collectAsState()

    fun carregarDadosReais() {
        scope.launch {
            isRefreshing = true
            repoImpl?.syncATData()
            isRefreshing = false
        }
    }

    LaunchedEffect(Unit) {
        carregarDadosReais()
    }

    // Diálogo "+ Atividade" (Item 5)
    if (showNovaAtividadeDialog) {
        AtNovaAtividadeDialog(
            user = user,
            assistidos = assistidos,
            atribuicoes = atribuicoes,
            profissionalId = profissionalLogado?.id,
            onDismiss = { showNovaAtividadeDialog = false },
            onSalvar = { novaAtividade ->
                repoImpl?.registrarAtividade(novaAtividade)
                    ?: Result.failure(IllegalStateException("Repositório indisponível"))
            },
            onSalvaComSucesso = {
                scope.launch {
                    snackbarHostState.showSnackbar("Atividade registrada com sucesso!")
                }
            }
        )
    }

    // Diálogo "+ Novo acompanhamento" (Item 4)
    if (showNovoAcompanhamentoDialog) {
        AtNovoAcompanhamentoDialog(
            user = user,
            assistidos = assistidos,
            atribuicoes = atribuicoes,
            profissionalId = profissionalLogado?.id,
            onDismiss = { showNovoAcompanhamentoDialog = false },
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

    // Diálogo Detalhes do Evento do Calendário (Item 6)
    if (selectedEventoParaDetalhes != null) {
        AtDetalhesEventoDialog(
            evento = selectedEventoParaDetalhes!!,
            onDismiss = { selectedEventoParaDetalhes = null }
        )
    }

    Scaffold(
        topBar = {
            CiadiTopBar(
                title = "CIADI+ • A.T.",
                currentUser = user,
                onProfileClick = onNavigateToProfile,
                onJanethClick = onNavigateToJaneth
            )
        },
        floatingActionButton = {
            when (selectedSection) {
                AtMenuSection.ATIVIDADES, AtMenuSection.CALENDARIO, AtMenuSection.INICIO -> {
                    FloatingActionButton(
                        onClick = { showNovaAtividadeDialog = true },
                        containerColor = CIADIColors.RoleAt,
                        contentColor = Color.White,
                        modifier = Modifier.testTag("fab_nova_atividade")
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Nova Atividade")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("+ Atividade", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                AtMenuSection.ACOMPANHAMENTO -> {
                    FloatingActionButton(
                        onClick = { showNovoAcompanhamentoDialog = true },
                        containerColor = CIADIColors.TealPrimary,
                        contentColor = Color.White,
                        modifier = Modifier.testTag("fab_novo_acompanhamento")
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Novo Acompanhamento")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("+ Novo acompanhamento", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                else -> Unit
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // Barra de Navegação Inferior de Acesso Rápido para o A.T.
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                listOf(
                    AtMenuSection.INICIO,
                    AtMenuSection.ASSISTIDOS,
                    AtMenuSection.CALENDARIO,
                    AtMenuSection.ATIVIDADES,
                    AtMenuSection.CHAT
                ).forEach { sec ->
                    NavigationBarItem(
                        selected = selectedSection == sec,
                        onClick = {
                            if (sec == AtMenuSection.CHAT) {
                                onNavigateToChat()
                            } else {
                                selectedSection = sec
                            }
                        },
                        icon = {
                            Icon(imageVector = sec.icon, contentDescription = sec.label)
                        },
                        label = { Text(sec.label, fontSize = 11.sp, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CIADIColors.RoleAt,
                            selectedTextColor = CIADIColors.RoleAt,
                            indicatorColor = CIADIColors.CreamLight
                        ),
                        modifier = Modifier.testTag(sec.tag)
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(CIADIColors.BackgroundLight)
        ) {
            // Cabeçalho de Identidade A.T. + Abas dos 10 Itens Obrigatórios
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(top = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CIADIColors.RoleAt),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.School, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = profissionalLogado?.nomeCompleto ?: user.fullName,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CIADIColors.Brown)
                            )
                            Text(
                                text = "Acompanhante Terapêutico (A.T.) • CIADI+",
                                style = MaterialTheme.typography.labelSmall.copy(color = CIADIColors.TextSecondary)
                            )
                        }
                    }

                    IconButton(onClick = { carregarDadosReais() }) {
                        if (isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = CIADIColors.RoleAt)
                        } else {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Atualizar", tint = CIADIColors.Brown)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ScrollableTabRow com os 10 Menus Exigidos
                ScrollableTabRow(
                    selectedTabIndex = selectedSection.ordinal,
                    edgePadding = 16.dp,
                    containerColor = Color.White,
                    contentColor = CIADIColors.RoleAt
                ) {
                    AtMenuSection.values().forEach { sec ->
                        Tab(
                            selected = selectedSection == sec,
                            onClick = {
                                when (sec) {
                                    AtMenuSection.ATIVIDADES_PLUS -> {
                                        showNovaAtividadeDialog = true
                                    }
                                    AtMenuSection.CHAT -> {
                                        onNavigateToChat()
                                    }
                                    AtMenuSection.PERFIL -> {
                                        onNavigateToProfile()
                                    }
                                    AtMenuSection.NOTIFICACOES -> {
                                        onNavigateToNotifications()
                                    }
                                    AtMenuSection.FORMULARIOS -> {
                                        onNavigateToForms()
                                    }
                                    else -> {
                                        selectedSection = sec
                                    }
                                }
                            },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = sec.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(sec.label, fontWeight = if (selectedSection == sec) FontWeight.Bold else FontWeight.Normal)
                                }
                            },
                            modifier = Modifier.testTag("tab_${sec.tag}")
                        )
                    }
                }
            }

            // Conteúdo dinâmico da seção selecionada
            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
                when (selectedSection) {
                    AtMenuSection.INICIO -> {
                        AtInicioContent(
                            assistidos = assistidos,
                            atividades = atividades,
                            sessoes = sessoes,
                            calendarioEventos = calendarioEventos,
                            onNavigateToSection = { selectedSection = it },
                            onAbrirNovaAtividade = { showNovaAtividadeDialog = true },
                            onAbrirNovoAcompanhamento = { showNovoAcompanhamentoDialog = true },
                            onAbrirEvento = { selectedEventoParaDetalhes = it },
                            onNavigateToSos = onNavigateToSos
                        )
                    }
                    AtMenuSection.ASSISTIDOS -> {
                        AtMeusAssistidosContent(assistidos = assistidos)
                    }
                    AtMenuSection.CALENDARIO -> {
                        AtCalendarioContent(
                            eventos = calendarioEventos,
                            onAbrirEvento = { selectedEventoParaDetalhes = it },
                            onNovaAtividade = { showNovaAtividadeDialog = true }
                        )
                    }
                    AtMenuSection.ATIVIDADES, AtMenuSection.ATIVIDADES_PLUS -> {
                        AtAtividadesContent(
                            atividades = atividades,
                            onNovaAtividade = { showNovaAtividadeDialog = true }
                        )
                    }
                    AtMenuSection.ACOMPANHAMENTO -> {
                        AtAcompanhamentoContent(
                            sessoes = sessoes,
                            acompanhamentosAba = acompanhamentosAba,
                            onNovoAcompanhamento = { showNovoAcompanhamentoDialog = true }
                        )
                    }
                    AtMenuSection.ORGANIZACAO -> {
                        AtOrganizacaoContent(organizacao = organizacao)
                    }
                    AtMenuSection.NOTIFICACOES -> {
                        AtNotificacoesContent(notificacoes = notificacoes)
                    }
                    AtMenuSection.PERFIL -> {
                        AtPerfilResumoContent(user = user, profissional = profissionalLogado, onSignOut = onSignOut)
                    }
                    AtMenuSection.CHAT -> {
                        // Navega direto ou mostra mensagem
                        LaunchedEffect(Unit) { onNavigateToChat() }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// 1. INÍCIO DO A.T.
// -------------------------------------------------------------------------------------
@Composable
private fun AtInicioContent(
    assistidos: List<PacienteDto>,
    atividades: List<AtAtividadeDto>,
    sessoes: List<AtSessaoDto>,
    calendarioEventos: List<AtCalendarioEventoDto>,
    onNavigateToSection: (AtMenuSection) -> Unit,
    onAbrirNovaAtividade: () -> Unit,
    onAbrirNovoAcompanhamento: () -> Unit,
    onAbrirEvento: (AtCalendarioEventoDto) -> Unit,
    onNavigateToSos: () -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Card de Resumo de Campo
        item {
            CIADICards.Warm(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Painel Operacional do A.T.",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = CIADIColors.Brown)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CIADIColors.RoleAt.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${assistidos.size} Assistidos",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = CIADIColors.RoleAt)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Acompanhamento em campo com registro em tempo real de atividades, metas P.E.I. e ocorrências.",
                        style = MaterialTheme.typography.bodySmall.copy(color = CIADIColors.TextSecondary)
                    )
                }
            }
        }

        // Ações Rápidas Oficiais
        item {
            Text("Ações Rápidas de Campo", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CIADIColors.Brown))
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AtActionCard(
                    title = "+ Atividade",
                    subtitle = "Registrar no banco",
                    icon = Icons.Default.Add,
                    color = CIADIColors.RoleAt,
                    onClick = onAbrirNovaAtividade,
                    modifier = Modifier.weight(1f).testTag("card_action_nova_atividade")
                )
                AtActionCard(
                    title = "+ Acompanhamento",
                    subtitle = "Sessão & evolução",
                    icon = Icons.Default.AssignmentTurnedIn,
                    color = CIADIColors.TealPrimary,
                    onClick = onAbrirNovoAcompanhamento,
                    modifier = Modifier.weight(1f).testTag("card_action_novo_acompanhamento")
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AtActionCard(
                    title = "Calendário",
                    subtitle = "${calendarioEventos.size} compromissos",
                    icon = Icons.Default.CalendarMonth,
                    color = CIADIColors.OrangePrimary,
                    onClick = { onNavigateToSection(AtMenuSection.CALENDARIO) },
                    modifier = Modifier.weight(1f)
                )
                AtActionCard(
                    title = "SOS em Campo",
                    subtitle = "Alerta de crise",
                    icon = Icons.Default.Warning,
                    color = CIADIColors.ErrorRed,
                    onClick = onNavigateToSos,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Próximos Eventos do Calendário
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Próximos Agendamentos & Atividades", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CIADIColors.Brown))
                Text(
                    text = "Ver todos",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = CIADIColors.RoleAt),
                    modifier = Modifier.clickable { onNavigateToSection(AtMenuSection.CALENDARIO) }
                )
            }
        }

        if (calendarioEventos.isEmpty()) {
            item {
                CIADICards.Base(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Não existem atividades ou agendamentos para este período.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = CIADIColors.TextSecondary)
                        )
                    }
                }
            }
        } else {
            items(calendarioEventos.take(3)) { evento ->
                AtEventoCard(evento = evento, onClick = { onAbrirEvento(evento) })
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// 2. MEUS ASSISTIDOS (Item 3 do Prompt Mestre)
// Fonte de verdade: ciadi_atribuicoes_at (profissional_id = profissionais.id e ativo = true)
// -------------------------------------------------------------------------------------
@Composable
private fun AtMeusAssistidosContent(assistidos: List<PacienteDto>) {
    if (assistidos.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    modifier = Modifier.size(54.dp),
                    tint = CIADIColors.RoleAt.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Não existem assistidos atribuídos ao seu perfil.",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CIADIColors.Brown),
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "As atribuições são cadastradas na coordenação clínica via ciadi_atribuicoes_at.",
                    style = MaterialTheme.typography.bodySmall.copy(color = CIADIColors.TextSecondary),
                    lineHeight = 18.sp
                )
            }
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().testTag("lista_meus_assistidos")
        ) {
            item {
                Text(
                    text = "Assistidos Atribuídos (${assistidos.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CIADIColors.Brown)
                )
            }

            items(assistidos) { paciente ->
                CIADICards.Base(modifier = Modifier.fillMaxWidth().testTag("assistido_card_${paciente.id}")) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(CIADIColors.RoleAt),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = paciente.displayName.take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = paciente.displayName,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CIADIColors.Brown)
                            )
                            if (!paciente.codigoPaciente.isNullOrBlank()) {
                                Text(
                                    text = "Código: ${paciente.codigoPaciente}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = CIADIColors.TextSecondary)
                                )
                            }
                            if (!paciente.dataNascimento.isNullOrBlank()) {
                                Text(
                                    text = "Nascimento: ${paciente.dataNascimento}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = CIADIColors.TextSecondary)
                                )
                            }
                            if (!paciente.diagnosticoResumo.isNullOrBlank()) {
                                Text(
                                    text = paciente.diagnosticoResumo,
                                    style = MaterialTheme.typography.bodySmall.copy(color = CIADIColors.TextPrimary),
                                    maxLines = 2
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFE8F5E9))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Ativo", fontSize = 11.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// 3. CALENDÁRIO A.T. (Item 6 do Prompt Mestre)
// Fonte: v_ciadi_at_calendario
// -------------------------------------------------------------------------------------
@Composable
private fun AtCalendarioContent(
    eventos: List<AtCalendarioEventoDto>,
    onAbrirEvento: (AtCalendarioEventoDto) -> Unit,
    onNovaAtividade: () -> Unit
) {
    if (eventos.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(54.dp),
                    tint = CIADIColors.OrangePrimary.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Não existem atividades ou agendamentos para este período.",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CIADIColors.Brown),
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                CIADIButtons.Primary(
                    text = "+ Nova atividade",
                    onClick = onNovaAtividade,
                    modifier = Modifier.testTag("btn_adicionar_atividade_calendario")
                )
            }
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().testTag("lista_calendario_at")
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Eventos e Agendamentos (${eventos.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CIADIColors.Brown)
                    )
                    IconButton(onClick = onNovaAtividade) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Nova Atividade", tint = CIADIColors.RoleAt)
                    }
                }
            }

            items(eventos) { evento ->
                AtEventoCard(evento = evento, onClick = { onAbrirEvento(evento) })
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// 4. ATIVIDADES A.T. (Item 5 do Prompt Mestre)
// Fonte: ciadi_at_atividades
// -------------------------------------------------------------------------------------
@Composable
private fun AtAtividadesContent(
    atividades: List<AtAtividadeDto>,
    onNovaAtividade: () -> Unit
) {
    if (atividades.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.EventNote,
                    contentDescription = null,
                    modifier = Modifier.size(54.dp),
                    tint = CIADIColors.RoleAt.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Não existem atividades registradas.",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CIADIColors.Brown),
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                CIADIButtons.Primary(
                    text = "+ Adicionar atividade",
                    onClick = onNovaAtividade,
                    modifier = Modifier.testTag("btn_adicionar_atividade_empty")
                )
            }
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().testTag("lista_atividades_at")
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Atividades de Campo (${atividades.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CIADIColors.Brown)
                    )
                    CIADIButtons.Primary(
                        text = "+ Atividade",
                        onClick = onNovaAtividade,
                        modifier = Modifier.testTag("btn_adicionar_atividade_topo")
                    )
                }
            }

            items(atividades) { ativ ->
                CIADICards.Base(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = ativ.titulo,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CIADIColors.Brown),
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        when (ativ.estado.lowercase()) {
                                            "realizada" -> Color(0xFFE8F5E9)
                                            "cancelada" -> Color(0xFFFFEBEE)
                                            else -> Color(0xFFFFF3E0)
                                        }
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = ativ.estado,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (ativ.estado.lowercase()) {
                                        "realizada" -> Color(0xFF2E7D32)
                                        "cancelada" -> Color(0xFFC62828)
                                        else -> Color(0xFFE65100)
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = "Data: ${ativ.dataAtividade} • ${ativ.horaInicio ?: ""} - ${ativ.horaFim ?: ""}",
                                style = MaterialTheme.typography.labelSmall.copy(color = CIADIColors.TextSecondary)
                            )
                            if (!ativ.tipo.isNullOrBlank()) {
                                Text(
                                    text = ativ.tipo,
                                    style = MaterialTheme.typography.labelSmall.copy(color = CIADIColors.RoleAt, fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        if (!ativ.descricao.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = ativ.descricao, style = MaterialTheme.typography.bodySmall.copy(color = CIADIColors.TextPrimary))
                        }

                        if (!ativ.objetivo.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Objetivo: ${ativ.objetivo}", style = MaterialTheme.typography.bodySmall.copy(color = CIADIColors.TextSecondary))
                        }

                        if (!ativ.resultado.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Resultado: ${ativ.resultado}", style = MaterialTheme.typography.bodySmall.copy(color = CIADIColors.SuccessGreen, fontWeight = FontWeight.Medium))
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// 5. ACOMPANHAMENTO A.T. (Item 4 do Prompt Mestre)
// Fonte: ciadi_at_sessoes, ciadi_at_registos_objetivos, ciadi_at_incidentes, ciadi_acompanhamentos_diarios_aba
// -------------------------------------------------------------------------------------
@Composable
private fun AtAcompanhamentoContent(
    sessoes: List<AtSessaoDto>,
    acompanhamentosAba: List<com.example.data.remote.dto.AcompanhamentoDiarioAbaDto>,
    onNovoAcompanhamento: () -> Unit
) {
    if (sessoes.isEmpty() && acompanhamentosAba.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
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
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CIADIColors.Brown),
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                CIADIButtons.Primary(
                    text = "+ Novo acompanhamento",
                    onClick = onNovoAcompanhamento,
                    modifier = Modifier.testTag("btn_novo_acompanhamento_empty")
                )
            }
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().testTag("lista_acompanhamentos_at")
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Registros de Acompanhamento (${sessoes.size + acompanhamentosAba.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CIADIColors.Brown)
                    )
                    CIADIButtons.Primary(
                        text = "+ Novo",
                        onClick = onNovoAcompanhamento,
                        modifier = Modifier.testTag("btn_novo_acompanhamento_topo")
                    )
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
                                text = "Sessão • ${sessao.dataSessao}",
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
                            Spacer(modifier = Modifier.height(4.dp))
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
                            Text(text = "Obs: ${sessao.observacoes ?: sessao.resumoObservacoes}", style = MaterialTheme.typography.bodySmall.copy(color = CIADIColors.TextSecondary))
                        }
                    }
                }
            }

            // Exibe também acompanhamentos diários ABA se houver
            items(acompanhamentosAba) { aba ->
                CIADICards.Base(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Acompanhamento Diário ABA • ${aba.dataRegistro ?: "Hoje"}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CIADIColors.Brown)
                            )
                            Text(
                                text = aba.estado ?: "Concluído",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                        if (!aba.comportamentoObservado.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Comportamento: ${aba.comportamentoObservado}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (!aba.progresso.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Progresso: ${aba.progresso}", style = MaterialTheme.typography.bodySmall.copy(color = CIADIColors.SuccessGreen))
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// 6. ORGANIZAÇÃO A.T. (Item 7 do Prompt Mestre)
// Fonte: v_ciadi_at_organizacao (Supervisor, Outros A.T., Profissionais, Família autorizada)
// -------------------------------------------------------------------------------------
@Composable
private fun AtOrganizacaoContent(organizacao: List<AtOrganizacaoDto>) {
    if (organizacao.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.CorporateFare,
                    contentDescription = null,
                    modifier = Modifier.size(54.dp),
                    tint = CIADIColors.RoleProfessional.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Não existem membros da organização associados ao seu perfil.",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CIADIColors.Brown),
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "A estrutura inclui Supervisor, outros A.T.s, profissionais e famílias autorizadas vinculadas aos seus assistidos.",
                    style = MaterialTheme.typography.bodySmall.copy(color = CIADIColors.TextSecondary),
                    lineHeight = 18.sp
                )
            }
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().testTag("lista_organizacao_at")
        ) {
            item {
                Text(
                    text = "Estrutura Relacionada (${organizacao.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CIADIColors.Brown)
                )
            }

            items(organizacao) { pessoa ->
                CIADICards.Base(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    when (pessoa.categoria?.lowercase()) {
                                        "supervisor", "coordenação" -> CIADIColors.RoleGestor
                                        "família", "familia" -> CIADIColors.RoleFamily
                                        "a.t." -> CIADIColors.RoleAt
                                        else -> CIADIColors.RoleProfessional
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (pessoa.nome ?: "C").take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = pessoa.nome ?: "Profissional CIADI",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CIADIColors.Brown)
                            )
                            Text(
                                text = pessoa.funcao ?: pessoa.categoria ?: "Membro da Equipe",
                                style = MaterialTheme.typography.labelSmall.copy(color = CIADIColors.RoleAt, fontWeight = FontWeight.SemiBold)
                            )
                            if (!pessoa.assistidoRelacionado.isNullOrBlank()) {
                                Text(
                                    text = "Assistido: ${pessoa.assistidoRelacionado}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = CIADIColors.TextSecondary)
                                )
                            }
                            if (!pessoa.telefone.isNullOrBlank()) {
                                Text(
                                    text = "Tel: ${pessoa.telefone}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = CIADIColors.TextSecondary)
                                )
                            }
                            if (!pessoa.email.isNullOrBlank()) {
                                Text(
                                    text = pessoa.email,
                                    style = MaterialTheme.typography.bodySmall.copy(color = CIADIColors.TextSecondary)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// 7. NOTIFICAÇÕES A.T.
// -------------------------------------------------------------------------------------
@Composable
private fun AtNotificacoesContent(notificacoes: List<com.example.data.remote.dto.NotificacaoDto>) {
    if (notificacoes.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    modifier = Modifier.size(54.dp),
                    tint = CIADIColors.TextSecondary.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Nenhuma notificação no momento.",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CIADIColors.Brown),
                    fontSize = 16.sp
                )
            }
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
            items(notificacoes) { notif ->
                CIADICards.Base(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = notif.titulo, fontWeight = FontWeight.Bold, color = CIADIColors.Brown)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = notif.mensagem, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// 8. PERFIL RESUMO A.T.
// -------------------------------------------------------------------------------------
@Composable
private fun AtPerfilResumoContent(
    user: UserProfile,
    profissional: com.example.data.remote.dto.ProfissionalDto?,
    onSignOut: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CIADICards.Base(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(50.dp).clip(CircleShape).background(CIADIColors.RoleAt),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(user.fullName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(profissional?.nomeCompleto ?: user.fullName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = CIADIColors.Brown)
                        Text(user.email, fontSize = 13.sp, color = CIADIColors.TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        RoleBadge(role = user.role)
                    }
                }

                if (profissional != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    if (!profissional.registroProfissional.isNullOrBlank()) {
                        Text("Registro: ${profissional.registroProfissional}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (!profissional.telefoneProfissional.isNullOrBlank()) {
                        Text("Telefone: ${profissional.telefoneProfissional}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        CIADIButtons.Secondary(
            text = "Terminar Sessão",
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth().testTag("btn_logout_at")
        )
    }
}

// Card de Ação Rápida
@Composable
private fun AtActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, fontWeight = FontWeight.Bold, color = CIADIColors.Brown, fontSize = 14.sp)
            Text(text = subtitle, fontSize = 11.sp, color = CIADIColors.TextSecondary)
        }
    }
}

// Card de Evento do Calendário
@Composable
private fun AtEventoCard(
    evento: AtCalendarioEventoDto,
    onClick: () -> Unit
) {
    CIADICards.Base(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("evento_calendario_${evento.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CIADIColors.OrangePrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, tint = CIADIColors.OrangePrimary, modifier = Modifier.size(22.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = evento.titulo ?: "Compromisso",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CIADIColors.Brown)
                )
                if (!evento.assistido.isNullOrBlank()) {
                    Text(
                        text = "Assistido: ${evento.assistido}",
                        style = MaterialTheme.typography.labelSmall.copy(color = CIADIColors.RoleAt, fontWeight = FontWeight.Bold)
                    )
                }
                Text(
                    text = "${evento.data ?: ""} • ${evento.hora ?: ""}",
                    style = MaterialTheme.typography.bodySmall.copy(color = CIADIColors.TextSecondary)
                )
                if (!evento.modalidade.isNullOrBlank()) {
                    Text(
                        text = "Modalidade: ${evento.modalidade}",
                        style = MaterialTheme.typography.bodySmall.copy(color = CIADIColors.TealPrimary, fontSize = 11.sp)
                    )
                }
            }

            if (!evento.estado.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when (evento.estado.lowercase()) {
                                "realizada", "concluida", "confirmado" -> Color(0xFFE8F5E9)
                                "cancelada", "cancelado" -> Color(0xFFFFEBEE)
                                else -> Color(0xFFFFF3E0)
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = evento.estado,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (evento.estado.lowercase()) {
                            "realizada", "concluida", "confirmado" -> Color(0xFF2E7D32)
                            "cancelada", "cancelado" -> Color(0xFFC62828)
                            else -> Color(0xFFE65100)
                        }
                    )
                }
            }
        }
    }
}
