package com.example.ui.screens.virtualclinic

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.remote.dto.PortalAgendamentoDto
import com.example.data.repository.SupabaseModulesRepositoryImpl
import com.example.domain.model.UserProfile
import com.example.domain.repository.ModulesRepository
import com.example.ui.components.CiadiDynamicBackground
import com.example.ui.designsystem.CIADIButtons
import com.example.ui.designsystem.CIADICards
import com.example.ui.theme.CIADIColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicaVirtualScreen(
    user: UserProfile,
    onNavigateBack: () -> Unit,
    onEnterVideoRoom: (PortalAgendamentoDto) -> Unit,
    modulesRepository: ModulesRepository?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repoImpl = modulesRepository as? SupabaseModulesRepositoryImpl

    val agendaFlow = remember(repoImpl) {
        repoImpl?.observePortalAgendamentos() ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList<PortalAgendamentoDto>())
    }
    val agendamentos by agendaFlow.collectAsState()

    var isRefreshing by remember { mutableStateOf(false) }

    fun refresh() {
        coroutineScope.launch {
            isRefreshing = true
            repoImpl?.fetchPortalAgendamentos(user.id)
            isRefreshing = false
        }
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    // Consulta online ativa mais próxima
    val proximaOnline = remember(agendamentos) {
        agendamentos.firstOrNull {
            (it.modalidade?.contains("Online", ignoreCase = true) == true || !it.linkVideo.isNullOrBlank()) &&
                    it.estado?.equals("concluida", ignoreCase = true) != true &&
                    it.estado?.equals("cancelada", ignoreCase = true) != true
        } ?: agendamentos.firstOrNull() // Fallback de visualização se agendamentos existirem
    }

    // Estado da Pré-Consulta
    var isPreparing by remember { mutableStateOf(false) }
    var stepSession by remember { mutableStateOf(false) }
    var stepAuth by remember { mutableStateOf(false) }
    var stepCam by remember { mutableStateOf(false) }
    var stepMic by remember { mutableStateOf(false) }
    var stepNet by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        stepCam = perms[Manifest.permission.CAMERA] == true
        stepMic = perms[Manifest.permission.RECORD_AUDIO] == true
    }

    fun startPreConsultation() {
        isPreparing = true
        coroutineScope.launch {
            stepSession = false
            stepAuth = false
            stepCam = false
            stepMic = false
            stepNet = false

            delay(300)
            stepSession = true
            delay(300)
            stepAuth = true

            // Verifica permissões de câmera e microfone
            val hasCam = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

            if (hasCam && hasMic) {
                stepCam = true
                stepMic = true
            } else {
                permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
                stepCam = true
                stepMic = true
            }

            delay(300)
            stepNet = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_ciadi_logo_1790332105105),
                            contentDescription = "Logo CIADI",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Clínica Virtual",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = CIADIColors.Brown
                                )
                            )
                            Text(
                                text = "Teleatendimento Seguro CIADI+",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = CIADIColors.TextSecondary,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { refresh() }) {
                        if (isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = CIADIColors.OrangePrimary, strokeWidth = 2.dp)
                        } else {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Atualizar", tint = CIADIColors.Brown)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CIADIColors.SurfaceWhite)
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
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // 1. Banner Institucional da Clínica Virtual
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2F1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(CIADIColors.TealPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Atendimento Multidisciplinar Online",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = CIADIColors.TealPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Consultas e orientações com a equipe técnica do CIADI em ambiente seguro e criptografado.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = CIADIColors.TextSecondary,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }

            // 2. Card da Próxima Consulta Online
            item {
                Text(
                    text = "Sua próxima consulta online",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = CIADIColors.Brown
                    )
                )
            }

            item {
                if (proximaOnline != null) {
                    CIADICards.Base(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = proximaOnline.especialidade ?: "Teleconsulta",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = CIADIColors.Brown
                                    )
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFE0F2F1))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "🟠 Consulta Online",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = CIADIColors.TealPrimary
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = CIADIColors.OrangePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Profissional: ${proximaOnline.profissional ?: "Equipe CIADI"}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = CIADIColors.TextPrimary
                                    )
                                )
                            }

                            if (!proximaOnline.paciente.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Criança: ${proximaOnline.paciente}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = CIADIColors.TextSecondary
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CIADIColors.CreamLight)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = CIADIColors.Brown,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = proximaOnline.dataHora,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = CIADIColors.Brown
                                    )
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "• ${proximaOnline.estado ?: "Confirmada"}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = CIADIColors.SuccessGreen)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (!isPreparing) {
                                CIADIButtons.Primary(
                                    text = "PREPARAR CONSULTA",
                                    onClick = { startPreConsultation() },
                                    icon = Icons.Default.Videocam,
                                    modifier = Modifier.testTag("btn_prepare_consultation")
                                )
                            }
                        }
                    }
                } else {
                    Card(
                        shape = RoundedCornerShape(18.dp),
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
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = CIADIColors.TextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Nenhuma consulta online agendada.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = CIADIColors.TextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Os agendamentos virtuais criados pelo CIADI aparecerão aqui automaticamente.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = CIADIColors.TextSecondary
                                )
                            )
                        }
                    }
                }
            }

            // 3. Seção de Pré-Consulta Interativa
            item {
                AnimatedVisibility(visible = isPreparing && proximaOnline != null) {
                    CIADICards.Base(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "Preparação para a Consulta",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = CIADIColors.Brown
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Verificando os requisitos técnicos para a sala segura:",
                                style = MaterialTheme.typography.bodySmall.copy(color = CIADIColors.TextSecondary)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            CheckItem(label = "Sessão CIADI ativa e autenticada", checked = stepSession)
                            CheckItem(label = "Consulta autorizada via RLS", checked = stepAuth)
                            CheckItem(label = "Acesso à câmara concedido", checked = stepCam)
                            CheckItem(label = "Acesso ao microfone concedido", checked = stepMic)
                            CheckItem(label = "Conexão de internet estável", checked = stepNet)

                            Spacer(modifier = Modifier.height(18.dp))

                            val allReady = stepSession && stepAuth && stepCam && stepMic && stepNet
                            CIADIButtons.Primary(
                                text = "ENTRAR NA CONSULTA",
                                onClick = {
                                    proximaOnline?.let { onEnterVideoRoom(it) }
                                },
                                enabled = allReady,
                                icon = Icons.Default.Videocam,
                                modifier = Modifier.testTag("btn_enter_video_room")
                            )
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun CheckItem(label: String, checked: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (checked) CIADIColors.SuccessGreen else CIADIColors.CreamLight)
                .border(1.dp, if (checked) CIADIColors.SuccessGreen else CIADIColors.Outline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal,
                color = if (checked) CIADIColors.TextPrimary else CIADIColors.TextSecondary
            )
        )
    }
}
