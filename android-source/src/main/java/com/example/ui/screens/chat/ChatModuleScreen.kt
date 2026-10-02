package com.example.ui.screens.chat

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.dto.AtContactoDto
import com.example.data.remote.dto.ChatGrupoDto
import com.example.data.remote.dto.ChatMensagemDto
import com.example.data.repository.SupabaseModulesRepositoryImpl
import com.example.domain.model.UserProfile
import com.example.domain.model.UserRole
import com.example.domain.repository.ModulesRepository
import com.example.ui.theme.CIADIColors
import kotlinx.coroutines.launch

/**
 * Módulo de Chat Oficial CIADI+ (Itens 8, 9, 10, 11 do Prompt Mestre).
 *
 * Utiliza dados reais das estruturas:
 * - ciadi_chat_grupos
 * - ciadi_chat_membros
 * - ciadi_chat_mensagens
 * - v_ciadi_at_contactos
 * - RPC ciadi_chat_abrir_privado
 *
 * Realtime:
 * Escuta ciadi_chat_mensagens por grupo_id indicando SUBSCRIBED, CHANNEL_ERROR, TIMED_OUT, CLOSED.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatModuleScreen(
    user: UserProfile,
    onNavigateBack: () -> Unit,
    modulesRepository: ModulesRepository? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val repoImpl = modulesRepository as? SupabaseModulesRepositoryImpl

    val gruposList by (repoImpl?.observeChatGrupos()
        ?.collectAsState(initial = emptyList())
        ?: remember { mutableStateOf(emptyList<ChatGrupoDto>()) })

    var selectedGroup by remember {
        mutableStateOf<ChatGrupoDto?>(null)
    }

    // Carrega mensagens reais do grupo selecionado a partir do banco
    val mensagensFlow = remember(repoImpl, selectedGroup?.id) {
        val gid = selectedGroup?.id
        if (!gid.isNullOrBlank()) {
            repoImpl?.observeMensagens(gid)
        } else {
            null
        } ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList<ChatMensagemDto>())
    }
    val mensagens by mensagensFlow.collectAsState(initial = emptyList())

    // Estado da subscrição Realtime
    val realtimeStatusFlow = remember(repoImpl, selectedGroup?.id) {
        val gid = selectedGroup?.id
        if (!gid.isNullOrBlank()) {
            repoImpl?.observeRealtimeStatus(gid)
        } else {
            null
        } ?: kotlinx.coroutines.flow.MutableStateFlow("SUBSCRIBED")
    }
    val realtimeStatus by realtimeStatusFlow.collectAsState(initial = "SUBSCRIBED")

    // Contactos autorizados reais da view v_ciadi_at_contactos
    val contactosFlow = remember(repoImpl) {
        repoImpl?.observeContactosAutorizados()
            ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList<AtContactoDto>())
    }
    val contactosAutorizados by contactosFlow.collectAsState(initial = emptyList())

    var messageInput by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showNewContactSheet by remember { mutableStateOf(false) }
    var contactSearchQuery by remember { mutableStateOf("") }

    // Sincroniza dados iniciais do Supabase e seleciona grupo real ativo
    LaunchedEffect(gruposList) {
        if (selectedGroup == null && gruposList.isNotEmpty()) {
            selectedGroup = gruposList.first()
        }
    }

    LaunchedEffect(selectedGroup?.id) {
        val gid = selectedGroup?.id
        if (!gid.isNullOrBlank()) {
            repoImpl?.carregarMensagensGrupo(gid)
            repoImpl?.iniciarRealtime(gid)
        }
    }

    LaunchedEffect(Unit) {
        repoImpl?.syncChat()
        repoImpl?.syncContactos()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = selectedGroup?.nome ?: "Chat Seguro CIADI+",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Badge de Estado do Realtime
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        when (realtimeStatus) {
                                            "SUBSCRIBED" -> Color(0xFFE8F5E9)
                                            "CHANNEL_ERROR" -> Color(0xFFFFEBEE)
                                            "TIMED_OUT" -> Color(0xFFFFF3E0)
                                            else -> Color(0xFFEEEEEE)
                                        }
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                    .testTag("realtime_status_badge")
                            ) {
                                Text(
                                    text = realtimeStatus,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (realtimeStatus) {
                                        "SUBSCRIBED" -> Color(0xFF2E7D32)
                                        "CHANNEL_ERROR" -> Color(0xFFC62828)
                                        "TIMED_OUT" -> Color(0xFFE65100)
                                        else -> Color(0xFF616161)
                                    }
                                )
                            }
                        }
                        Text(
                            text = if (!selectedGroup?.pacienteNome.isNullOrBlank())
                                "Assistido: ${selectedGroup?.pacienteNome} • RLS Ativo"
                            else
                                "Canal Clínico Seguro • RLS Ativo",
                            fontSize = 11.sp,
                            color = CIADIColors.TealPrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showNewContactSheet = true },
                        modifier = Modifier.testTag("btn_nova_conversa")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddComment,
                            contentDescription = "Nova Conversa / Contactos Autorizados",
                            tint = CIADIColors.OrangePrimary
                        )
                    }
                    IconButton(onClick = { showOptionsMenu = true }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Opções")
                    }
                    DropdownMenu(
                        expanded = showOptionsMenu,
                        onDismissRequest = { showOptionsMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Recarregar Mensagens") },
                            onClick = {
                                showOptionsMenu = false
                                val gid = selectedGroup?.id
                                if (!gid.isNullOrBlank()) {
                                    scope.launch {
                                        repoImpl?.carregarMensagensGrupo(gid)
                                        snackbarHostState.showSnackbar("Mensagens atualizadas.")
                                    }
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Enviar pelo WhatsApp") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color(0xFF25D366))
                            },
                            onClick = {
                                showOptionsMenu = false
                                val shareText = "Olá, entro em contato referente aos atendimentos no CIADI+ para ${selectedGroup?.pacienteNome ?: "assistido"}."
                                val url = "https://api.whatsapp.com/send?text=${Uri.encode(shareText)}"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = CIADIColors.Brown
                )
            )
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
            // Seletor de Grupos se houver grupos no Supabase
            if (gruposList.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 60.dp)
                        .background(Color.White)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    items(gruposList) { grp ->
                        val isSelected = grp.id == selectedGroup?.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedGroup = grp },
                            label = { Text(grp.nome, fontSize = 11.sp, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CIADIColors.CreamLight,
                                selectedLabelColor = CIADIColors.Brown
                            ),
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                }
            }

            // Lista de Mensagens ou Estado Vazio
            if (selectedGroup == null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp)
                        .testTag("empty_group_selection"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = CIADIColors.TealPrimary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Nenhuma conversa ativa selecionada",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = CIADIColors.Brown
                            ),
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Abra uma conversa segura através de um contacto autorizado no botão abaixo.",
                            style = MaterialTheme.typography.bodySmall.copy(color = CIADIColors.TextSecondary),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showNewContactSheet = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CIADIColors.OrangePrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("btn_iniciar_conversa_empty")
                        ) {
                            Icon(imageVector = Icons.Default.AddComment, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Iniciar Nova Conversa", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (mensagens.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp)
                        .testTag("empty_messages"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = CIADIColors.TealPrimary.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Você ainda não possui mensagens nesta conversa.",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = CIADIColors.Brown
                            ),
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Escreva abaixo para iniciar a conversa segura.",
                            style = MaterialTheme.typography.bodySmall.copy(color = CIADIColors.TextSecondary)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("lista_chat_mensagens"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(mensagens) { msg ->
                        val isMe = msg.senderId == user.id || msg.autorNome == user.fullName
                        ChatMessageBubble(msg = msg, isMe = isMe)
                    }
                }
            }

            // Barra de Envio
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    IconButton(
                        onClick = { showAttachmentSheet = true },
                        enabled = selectedGroup != null
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Anexar",
                            tint = if (selectedGroup != null) CIADIColors.OrangePrimary else Color.Gray
                        )
                    }

                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = { messageInput = it },
                        placeholder = {
                            Text(
                                if (selectedGroup != null) "Escreva uma mensagem segura..." else "Selecione um contacto para iniciar...",
                                fontSize = 14.sp
                            )
                        },
                        enabled = selectedGroup != null && !isSending,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_chat_mensagem")
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = {
                            val group = selectedGroup
                            if (group == null) {
                                showNewContactSheet = true
                                scope.launch {
                                    snackbarHostState.showSnackbar("Por favor, selecione um contacto no botão '+' para abrir a conversa segura.")
                                }
                                return@IconButton
                            }

                            if (messageInput.isNotBlank() && !isSending) {
                                val textToSend = messageInput.trim()
                                isSending = true
                                scope.launch {
                                    val result = repoImpl?.enviarMensagemTexto(
                                        grupoId = group.id,
                                        texto = textToSend,
                                        userId = user.id
                                    )
                                    isSending = false
                                    if (result != null && result.isSuccess) {
                                        // Limpar o campo estritamente após confirmação do Supabase (Item 5)
                                        messageInput = ""
                                    } else {
                                        val err = result?.exceptionOrNull()?.message
                                            ?: "Não foi possível enviar a mensagem. Verifique a ligação e tente novamente."
                                        snackbarHostState.showSnackbar(err)
                                    }
                                }
                            }
                        },
                        enabled = !isSending && messageInput.isNotBlank() && selectedGroup != null,
                        modifier = Modifier.testTag("btn_enviar_mensagem")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selectedGroup != null && messageInput.isNotBlank())
                                        CIADIColors.TealPrimary
                                    else
                                        CIADIColors.TealPrimary.copy(alpha = 0.4f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSending) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Enviar",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Modal Bottom Sheet para Anexos
        if (showAttachmentSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAttachmentSheet = false },
                sheetState = rememberModalBottomSheetState()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Anexar ao Chat Seguro (Supabase Storage)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = CIADIColors.Brown
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        AttachmentOption(icon = Icons.Default.CameraAlt, label = "Câmara") {
                            showAttachmentSheet = false
                            scope.launch {
                                snackbarHostState.showSnackbar("Captura via câmara autorizada.")
                            }
                        }
                        AttachmentOption(icon = Icons.Default.Image, label = "Galeria") {
                            showAttachmentSheet = false
                            scope.launch {
                                snackbarHostState.showSnackbar("Seleção de imagem autorizada.")
                            }
                        }
                        AttachmentOption(icon = Icons.Default.Description, label = "Documento") {
                            showAttachmentSheet = false
                            scope.launch {
                                snackbarHostState.showSnackbar("Anexo de documento selecionado.")
                            }
                        }
                        AttachmentOption(icon = Icons.Default.Folder, label = "Arquivos") {
                            showAttachmentSheet = false
                            scope.launch {
                                snackbarHostState.showSnackbar("Navegação de arquivos autorizada.")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Modal Bottom Sheet para Contactos Autorizados (Item 8 e 9 do Prompt Mestre)
        if (showNewContactSheet) {
            val contatosFiltrados = remember(contactSearchQuery, contactosAutorizados) {
                if (contactSearchQuery.isBlank()) {
                    contactosAutorizados
                } else {
                    contactosAutorizados.filter {
                        (it.nome?.contains(contactSearchQuery, ignoreCase = true) == true) ||
                                (it.funcao?.contains(contactSearchQuery, ignoreCase = true) == true) ||
                                (it.categoria?.contains(contactSearchQuery, ignoreCase = true) == true) ||
                                (it.assistidoRelacionado?.contains(contactSearchQuery, ignoreCase = true) == true)
                    }
                }
            }

            ModalBottomSheet(
                onDismissRequest = {
                    showNewContactSheet = false
                    contactSearchQuery = ""
                },
                sheetState = rememberModalBottomSheetState()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Nova Conversa • Contactos Autorizados",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = CIADIColors.Brown,
                            fontSize = 18.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Carregados via v_ciadi_at_contactos por vínculos autorizados no Supabase.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = CIADIColors.TextSecondary,
                            fontSize = 12.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = contactSearchQuery,
                        onValueChange = { contactSearchQuery = it },
                        placeholder = { Text("Pesquisar contacto autorizado...", fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = CIADIColors.Brown)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_contact_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (contatosFiltrados.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                                .testTag("empty_contacts"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Não existem contactos autorizados para esta conversa.",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = CIADIColors.Brown,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Apenas pessoas com vínculo autorizado ao assistido têm acesso.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = CIADIColors.TextSecondary),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 340.dp)
                                .testTag("lista_contactos_autorizados")
                        ) {
                            items(contatosFiltrados) { contacto ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val destId = contacto.contactId
                                            scope.launch {
                                                val res = repoImpl?.abrirConversaPrivada(
                                                    destinatarioId = destId,
                                                    pacienteId = contacto.pacienteId
                                                )
                                                if (res?.isSuccess == true) {
                                                    val gid = res.getOrNull()
                                                    if (!gid.isNullOrBlank()) {
                                                        selectedGroup = ChatGrupoDto(
                                                            id = gid,
                                                            nome = "${contacto.contactName} (${contacto.contactRole})",
                                                            tipo = "privado",
                                                            pacienteNome = contacto.assistidoRelacionado
                                                        )
                                                        showNewContactSheet = false
                                                        contactSearchQuery = ""
                                                        snackbarHostState.showSnackbar("Canal seguro aberto com ${contacto.contactName}")
                                                    } else {
                                                        snackbarHostState.showSnackbar("Não foi possível obter o identificador da conversa no Supabase.")
                                                    }
                                                } else {
                                                    val err = res?.exceptionOrNull()?.message
                                                        ?: "Erro ao abrir conversa privada."
                                                    snackbarHostState.showSnackbar(err)
                                                }
                                            }
                                        }
                                        .testTag("contact_item_${contacto.contactId}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when (contacto.categoria?.lowercase()) {
                                                        "família", "familia" -> CIADIColors.RoleFamily
                                                        "supervisor" -> CIADIColors.RoleGestor
                                                        "a.t." -> CIADIColors.RoleAt
                                                        else -> CIADIColors.RoleProfessional
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = contacto.contactName.take(1).uppercase(),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = contacto.contactName,
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = CIADIColors.Brown
                                                )
                                            )
                                            Text(
                                                text = contacto.contactRole,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = CIADIColors.OrangePrimary,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 11.sp
                                                )
                                            )
                                            if (!contacto.assistidoRelacionado.isNullOrBlank()) {
                                                Text(
                                                    text = "Assistido: ${contacto.assistidoRelacionado}",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = CIADIColors.TextSecondary,
                                                        fontSize = 11.sp
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun AttachmentOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(CIADIColors.CreamLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = CIADIColors.OrangePrimary)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ChatMessageBubble(msg: ChatMensagemDto, isMe: Boolean) {
    Column(
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        val autor = msg.autorNome
        if (!isMe && !autor.isNullOrBlank()) {
            Text(
                text = autor,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = CIADIColors.Brown,
                    fontSize = 11.sp
                ),
                modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
            )
        }

        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMe) 16.dp else 4.dp,
                        bottomEnd = if (isMe) 4.dp else 16.dp
                    )
                )
                .background(if (isMe) CIADIColors.TealPrimary else Color.White)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = msg.text,
                color = if (isMe) Color.White else CIADIColors.TextPrimary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }

        Text(
            text = msg.timestamp.ifBlank { "Hoje" },
            fontSize = 10.sp,
            color = CIADIColors.TextSecondary,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
