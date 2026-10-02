package com.example.ui.screens.virtualclinic

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.remote.dto.PortalAgendamentoDto
import com.example.data.repository.SupabaseModulesRepositoryImpl
import com.example.domain.model.UserProfile
import com.example.domain.repository.ModulesRepository
import com.example.ui.designsystem.CIADIButtons
import com.example.ui.screens.chat.ChatModuleScreen
import com.example.ui.theme.CIADIColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class RoomState {
    PREPARING,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ERROR,
    FINISHED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalaVideoScreen(
    user: UserProfile,
    agendamento: PortalAgendamentoDto,
    onNavigateBack: () -> Unit,
    modulesRepository: ModulesRepository?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repoImpl = modulesRepository as? SupabaseModulesRepositoryImpl

    var showChatSheet by remember { mutableStateOf(false) }
    var roomState by remember { mutableStateOf(RoomState.PREPARING) }
    var secondsElapsed by remember { mutableIntStateOf(0) }

    var isMicMuted by remember { mutableStateOf(false) }
    var isVideoOff by remember { mutableStateOf(false) }
    var isFrontCam by remember { mutableStateOf(true) }
    var isSpeakerOn by remember { mutableStateOf(true) }

    var showLeaveConfirmation by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val canonicalWebRoomUrl = "https://egpkkttbcaukqnnxyhjv.supabase.co/storage/v1/object/public/ciadi-web/clinica-virtual.html?agendamento_id=${agendamento.id}"

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val cam = perms[Manifest.permission.CAMERA] ?: false
        val mic = perms[Manifest.permission.RECORD_AUDIO] ?: false
        if (!cam || !mic) {
            Log.w("CIADI_PERMISSIONS", "Câmara ou microfone não concedidos para teleconsulta.")
        }
    }

    LaunchedEffect(Unit) {
        val hasCam = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (!hasCam || !hasMic) {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    fun connectToRoom() {
        coroutineScope.launch {
            roomState = RoomState.PREPARING
            repoImpl?.prepararSalaVideo(agendamento.id)
            delay(400)
            roomState = RoomState.CONNECTING
            val enterResult = repoImpl?.registrarEntradaVideo(agendamento.id)
            delay(600)
            if (enterResult != null && enterResult.isFailure) {
                roomState = RoomState.ERROR
                errorMessage = enterResult.exceptionOrNull()?.message ?: "Falha ao sincronizar entrada."
            } else {
                roomState = RoomState.CONNECTED
            }
        }
    }

    LaunchedEffect(agendamento.id) {
        connectToRoom()
    }

    // Cronômetro da consulta quando em andamento
    LaunchedEffect(roomState) {
        if (roomState == RoomState.CONNECTED) {
            while (true) {
                delay(1000)
                secondsElapsed++
            }
        }
    }

    val formattedTime = remember(secondsElapsed) {
        val minutes = secondsElapsed / 60
        val seconds = secondsElapsed % 60
        String.format("%02d:%02d", minutes, seconds)
    }

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF14191F))
        ) {
            when (roomState) {
                RoomState.PREPARING -> {
                    StateNotice(
                        title = "A preparar a sua consulta...",
                        subtitle = "Conectando aos servidores seguros do CIADI",
                        isLoading = true
                    )
                }
                RoomState.CONNECTING -> {
                    StateNotice(
                        title = "A entrar na Clínica Virtual...",
                        subtitle = "Validando credenciais clínicas com o profissional",
                        isLoading = true
                    )
                }
                RoomState.RECONNECTING -> {
                    StateNotice(
                        title = "A ligação foi interrompida",
                        subtitle = "A tentar reconectar à sala com a equipe...",
                        isLoading = true
                    )
                }
                RoomState.ERROR -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = CIADIColors.ErrorRed, modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Não foi possível entrar na consulta.",
                            style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = errorMessage ?: "Verifique sua conexão ou tente novamente em instantes.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFB0BEC5)),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { connectToRoom() },
                            colors = ButtonDefaults.buttonColors(containerColor = CIADIColors.OrangePrimary)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tentar novamente", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                RoomState.FINISHED -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(CIADIColors.SuccessGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Consulta terminada",
                            style = MaterialTheme.typography.headlineSmall.copy(color = Color.White, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Duração: $formattedTime • O resumo e orientações serão disponibilizados no CIADI+.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFCFD8DC)),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        CIADIButtons.Primary(
                            text = "VOLTAR AO INÍCIO",
                            onClick = onNavigateBack,
                            modifier = Modifier.width(220.dp)
                        )
                    }
                }
                RoomState.CONNECTED -> {
                    // 1. Área de Vídeo Principal (Profissional)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(0.dp))
                            .background(Color(0xFF1E262E)),
                        contentAlignment = Alignment.Center
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.mediaPlaybackRequiresUserGesture = false
                                    settings.allowFileAccess = false
                                    settings.allowContentAccess = false
                                    settings.databaseEnabled = false

                                    webChromeClient = object : WebChromeClient() {
                                        override fun onPermissionRequest(request: PermissionRequest?) {
                                            if (request == null) return
                                            val origin = request.origin.toString().lowercase()
                                            val isAuthorizedOrigin = origin.startsWith("https://egpkkttbcaukqnnxyhjv.supabase.co") ||
                                                    origin.startsWith("https://www.ciadi.ao") ||
                                                    origin.startsWith("https://ciadi.ao")
                                            if (isAuthorizedOrigin) {
                                                val allowedResources = request.resources.filter {
                                                    it == PermissionRequest.RESOURCE_VIDEO_CAPTURE ||
                                                    it == PermissionRequest.RESOURCE_AUDIO_CAPTURE
                                                }.toTypedArray()
                                                if (allowedResources.isNotEmpty()) {
                                                    request.grant(allowedResources)
                                                } else {
                                                    request.deny()
                                                }
                                            } else {
                                                request.deny()
                                            }
                                        }
                                    }

                                    webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(view: WebView?, req: WebResourceRequest?): Boolean {
                                            val uri = req?.url ?: return true
                                            val scheme = uri.scheme?.lowercase() ?: return true
                                            val host = uri.host?.lowercase() ?: ""

                                            if (scheme != "https") {
                                                if (scheme == "mailto" || scheme == "tel") {
                                                    try {
                                                        ctx.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                                    } catch (_: Exception) {}
                                                }
                                                return true
                                            }

                                            val isAllowed = host == "egpkkttbcaukqnnxyhjv.supabase.co" ||
                                                    host == "www.ciadi.ao" ||
                                                    host == "ciadi.ao" ||
                                                    host.endsWith(".livekit.cloud")

                                            if (isAllowed) {
                                                return false
                                            }

                                            Log.w("CIADI_WEBVIEW", "Navegação externa bloqueada: $uri")
                                            return true
                                        }
                                    }

                                    loadUrl(canonicalWebRoomUrl)
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("webview_sala_video")
                        )

                        // Sombra de Gradiente Superior e Inferior para leitura de controles
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.75f),
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.85f)
                                        )
                                    )
                                )
                        )

                        // Identificação do Profissional no Vídeo
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 20.dp, bottom = 120.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = agendamento.profissional ?: "Profissional CIADI",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        // 2. Janela Pequena (PiP) da Criança / Família
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 90.dp, end = 20.dp)
                                .size(width = 110.dp, height = 150.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.5.dp, CIADIColors.OrangePrimary, RoundedCornerShape(14.dp))
                                .background(if (isVideoOff) Color(0xFF263238) else Color(0xFF37474F))
                                .shadow(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isVideoOff) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(imageVector = Icons.Default.VideocamOff, contentDescription = null, tint = Color(0xFFB0BEC5), modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Câmara desativada", fontSize = 9.sp, color = Color(0xFFB0BEC5), textAlign = TextAlign.Center)
                                }
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.janeth_avatar_1790333481531),
                                    contentDescription = "Feed da Criança",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            // Nome da Criança no PiP
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.7f))
                                    .padding(vertical = 3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = agendamento.paciente ?: "Você (Família)",
                                    fontSize = 10.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // 3. Cabeçalho Customizado CIADI+ da Sala de Vídeo
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_ciadi_logo_1790332105105),
                                contentDescription = "Logo CIADI",
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Clínica Virtual",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "${agendamento.especialidade ?: "Consulta"} • ${agendamento.paciente ?: "Assistido"}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = CIADIColors.Cream,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Chat Clínico da Sala de Vídeo (Item 14 do Prompt Mestre)
                            IconButton(
                                onClick = { showChatSheet = true },
                                modifier = Modifier.testTag("btn_video_chat")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Chat,
                                    contentDescription = "Chat Clínico",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Abrir na Web Oficial (Item 13 do Prompt Mestre: https://www.ciadi.ao/clinica-virtual/index.html?agendamento_id=ID)
                            IconButton(
                                onClick = {
                                    val url = "https://www.ciadi.ao/clinica-virtual/index.html?agendamento_id=${agendamento.id}"
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.testTag("btn_open_web_video")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInBrowser,
                                    contentDescription = "Abrir na Web Oficial",
                                    tint = CIADIColors.OrangePrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Indicador de Conexão com Cronômetro
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(CIADIColors.SuccessGreen)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "🟢 $formattedTime",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }

                    // 4. Barra Inferior de Controles (Grandes, Táteis, 54dp)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 20.dp, vertical = 20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Microfone
                            ControlButton(
                                icon = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                label = if (isMicMuted) "Sem som" else "Mudo",
                                active = !isMicMuted,
                                onClick = { isMicMuted = !isMicMuted }
                            )

                            // Câmera
                            ControlButton(
                                icon = if (isVideoOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                                label = if (isVideoOff) "Sem vídeo" else "Vídeo",
                                active = !isVideoOff,
                                onClick = { isVideoOff = !isVideoOff }
                            )

                            // Trocar Câmera
                            ControlButton(
                                icon = Icons.Default.Cameraswitch,
                                label = "Câmara",
                                active = true,
                                onClick = { isFrontCam = !isFrontCam }
                            )

                            // Áudio / Alto-falante
                            ControlButton(
                                icon = Icons.Default.VolumeUp,
                                label = "Áudio",
                                active = isSpeakerOn,
                                onClick = { isSpeakerOn = !isSpeakerOn }
                            )

                            // Terminar Consulta (Destacado em Vermelho)
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(CIADIColors.ErrorRed)
                                    .clickable { showLeaveConfirmation = true }
                                    .testTag("btn_end_call"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CallEnd,
                                    contentDescription = "Terminar consulta",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal de Confirmação Anti-Desligamento Acidental
    if (showLeaveConfirmation) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirmation = false },
            title = {
                Text(
                    text = "Terminar consulta?",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = CIADIColors.Brown
                    )
                )
            },
            text = {
                Text(
                    text = "Tem certeza de que deseja encerrar a sua participação nesta sessão de teleatendimento com a equipe CIADI?",
                    style = MaterialTheme.typography.bodyMedium.copy(color = CIADIColors.TextPrimary)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLeaveConfirmation = false
                        coroutineScope.launch {
                            repoImpl?.registrarSaidaVideo(agendamento.id)
                            roomState = RoomState.FINISHED
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CIADIColors.ErrorRed)
                ) {
                    Text("Terminar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLeaveConfirmation = false }) {
                    Text("Continuar na sala")
                }
            }
        )
    }

    // Chat Clínico Integrado da Sala de Vídeo (Item 14 do Prompt Mestre)
    if (showChatSheet) {
        ModalBottomSheet(
            onDismissRequest = { showChatSheet = false },
            containerColor = Color.White
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
            ) {
                ChatModuleScreen(
                    user = user,
                    onNavigateBack = { showChatSheet = false },
                    modulesRepository = modulesRepository
                )
            }
        }
    }
}

@Composable
private fun StateNotice(title: String, subtitle: String, isLoading: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = CIADIColors.OrangePrimary,
                modifier = Modifier.size(44.dp),
                strokeWidth = 3.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                color = Color.White,
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFB0BEC5)),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
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
                .background(if (active) Color(0xFF263238) else Color(0xFFFFCDD2).copy(alpha = 0.9f))
                .border(1.dp, if (active) Color(0xFF455A64) else CIADIColors.ErrorRed, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) Color.White else CIADIColors.ErrorRed,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color(0xFFCFD8DC),
            fontWeight = FontWeight.Medium
        )
    }
}
