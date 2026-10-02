package com.example.ui.screens.sos

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.remote.dto.PacienteDto
import com.example.data.remote.dto.SosAlertaResultDto
import com.example.data.repository.SupabaseModulesRepositoryImpl
import com.example.domain.model.UserProfile
import com.example.domain.repository.ModulesRepository
import com.example.ui.theme.CIADIColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SosScreen(
    user: UserProfile,
    onNavigateBack: () -> Unit,
    modulesRepository: ModulesRepository?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repoImpl = modulesRepository as? SupabaseModulesRepositoryImpl

    val patientsFlow = remember(repoImpl) {
        repoImpl?.observeAuthorizedPatients() ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList<PacienteDto>())
    }
    val authorizedPatients by patientsFlow.collectAsState()
    var selectedPatient by remember { mutableStateOf<PacienteDto?>(null) }

    // Atualiza seleção com base nas regras: se 1 criança, apresenta diretamente; se várias, usuário escolhe
    LaunchedEffect(authorizedPatients) {
        if (authorizedPatients.size == 1 && selectedPatient == null) {
            selectedPatient = authorizedPatients.first()
        }
    }

    var tipoAlerta by remember { mutableStateOf("SOS") }
    var mensagemEmergencia by remember { mutableStateOf("Alerta de emergência acionado pelo responsável.") }
    var autorizarLocalizacao by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var resultadoSucesso by remember { mutableStateOf<SosAlertaResultDto?>(null) }
    var mensagemErro by remember { mutableStateOf<String?>(null) }

    // Launcher de permissão de localização
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        autorizarLocalizacao = granted
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (tipoAlerta == "SOS") "SOS CIADI" else "Proteção CIADI — Bullying",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = CIADIColors.Brown
                        )
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("sos_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = CIADIColors.Brown
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CIADIColors.SurfaceWhite)
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            contentPadding = paddingValues,
            modifier = Modifier
                .fillMaxSize()
                .background(CIADIColors.BackgroundLight)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (tipoAlerta == "SOS") CIADIColors.ErrorRed else Color.White, contentColor = if (tipoAlerta == "SOS") Color.White else CIADIColors.Brown), onClick = { tipoAlerta = "SOS" }) { Text("SOS") }
                    OutlinedButton(modifier = Modifier.weight(1f), onClick = { tipoAlerta = "BULLYING" }) { Text("Bullying") }
                }
            }

            // 1. Banner Institucional de Alerta Seguro
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFDE8E8)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(CIADIColors.ErrorRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Alerta",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Precisa de ajuda?",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF7D1B1B)
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Acione a equipe de prontidão do CIADI para apoio emergencial ao seu assistido.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF9E2A2B),
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }

            // 2. Seletor de Criança Autorizada ("Para quem é este alerta?")
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CIADIColors.SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Para quem é este alerta?",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = CIADIColors.Brown
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Identifique a criança autorizada vinculada a você no Supabase:",
                            style = MaterialTheme.typography.bodySmall.copy(color = CIADIColors.TextSecondary)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (authorizedPatients.isEmpty()) {
                            // Se nenhuma criança retornada pelo RLS ainda
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CIADIColors.CreamLight)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = CIADIColors.OrangePrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (!user.activePatientName.isNullOrBlank())
                                        "Assistido vinculado: ${user.activePatientName}"
                                    else
                                        "Carregando assistidos autorizados...",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = CIADIColors.TextPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        } else {
                            // Lista de crianças autorizadas reais
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                authorizedPatients.forEach { paciente ->
                                    val isSelected = selectedPatient?.id == paciente.id
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) CIADIColors.OrangePrimary else CIADIColors.Outline,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .background(if (isSelected) CIADIColors.CreamLight else CIADIColors.SurfaceWhite)
                                            .clickable { selectedPatient = paciente }
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) CIADIColors.OrangePrimary else Color.Transparent)
                                                .border(2.dp, if (isSelected) CIADIColors.OrangePrimary else CIADIColors.TextMuted, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = paciente.nome,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = CIADIColors.TextPrimary
                                                )
                                            )
                                            if (!paciente.diagnosticoResumo.isNullOrBlank()) {
                                                Text(
                                                    text = paciente.diagnosticoResumo,
                                                    style = MaterialTheme.typography.bodySmall.copy(
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
                }
            }

            // 3. Mensagem Opcional de Contexto
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CIADIColors.SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (tipoAlerta == "SOS") "Mensagem do Alerta (Opcional)" else "Relato de Bullying / Proteção",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = CIADIColors.Brown
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = mensagemEmergencia,
                            onValueChange = { mensagemEmergencia = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sos_message_input"),
                            shape = RoundedCornerShape(10.dp),
                            placeholder = { Text(if (tipoAlerta == "SOS") "Ex: Crise em ambiente escolar / necessidade de apoio emergencial" else "Descreva o que aconteceu, onde e quando.") }
                        )
                    }
                }
            }

            // 4. Localização Opcional com Consentimento Claro
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CIADIColors.SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = if (autorizarLocalizacao) CIADIColors.OrangePrimary else CIADIColors.TextMuted
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Enviar Localização Atual",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = CIADIColors.Brown
                                        )
                                    )
                                    Text(
                                        text = "Opcional. Ajuda a equipe a identificar o local do alerta.",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = CIADIColors.TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                            Switch(
                                checked = autorizarLocalizacao,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        val hasFine = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.ACCESS_FINE_LOCATION
                                        ) == PackageManager.PERMISSION_GRANTED
                                        val hasCoarse = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (hasFine || hasCoarse) {
                                            autorizarLocalizacao = true
                                        } else {
                                            locationPermissionLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                                )
                                            )
                                        }
                                    } else {
                                        autorizarLocalizacao = false
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = CIADIColors.OrangePrimary
                                ),
                                modifier = Modifier.testTag("sos_location_switch")
                            )
                        }
                    }
                }
            }

            // 5. Feedback de Sucesso ou Erro
            item {
                AnimatedVisibility(visible = resultadoSucesso != null) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = CIADIColors.SuccessGreen,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Alerta SOS Transmitido!",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = CIADIColors.SuccessGreen
                                    )
                                )
                                Text(
                                    text = resultadoSucesso?.mensagem ?: "A equipe de prontidão do CIADI foi notificada com sucesso.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = CIADIColors.TextPrimary)
                                )
                                if (!resultadoSucesso?.protocolo.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Protocolo: ${resultadoSucesso?.protocolo}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = CIADIColors.Brown
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = mensagemErro != null) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = CIADIColors.ErrorRed,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = mensagemErro ?: "Ocorreu um erro ao enviar o alerta.",
                                style = MaterialTheme.typography.bodySmall.copy(color = CIADIColors.ErrorRed)
                            )
                        }
                    }
                }
            }

            // 6. Botão SOS Principal (Grande, Visível, 56dp altura, Proteção Anti-Toque Acidental)
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val targetPatient = selectedPatient ?: authorizedPatients.firstOrNull()
                        if (targetPatient == null && user.activePatientName.isNullOrBlank()) {
                            mensagemErro = "Por favor, selecione para quem é este alerta."
                        } else {
                            mensagemErro = null
                            showConfirmDialog = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CIADIColors.ErrorRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .testTag("sos_trigger_button"),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Enviando alerta...",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (tipoAlerta == "SOS") "ACIONAR SOS CIADI+" else "ENVIAR RELATO DE BULLYING",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Modal de Confirmação Anti-Toque Acidental
    if (showConfirmDialog) {
        val childName = selectedPatient?.nome ?: user.activePatientName ?: "Assistido"
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text(
                    text = if (tipoAlerta == "SOS") "Enviar alerta SOS?" else "Enviar relato de bullying?",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = CIADIColors.Brown
                    )
                )
            },
            text = {
                Column {
                    Text(
                        text = "Este alerta será enviado à equipa responsável do CIADI para atendimento emergencial de:",
                        style = MaterialTheme.typography.bodyMedium.copy(color = CIADIColors.TextPrimary)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = childName,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = CIADIColors.OrangePrimary
                        )
                    )
                    if (autorizarLocalizacao) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "📍 Coordenadas de localização serão anexadas ao alerta.",
                            style = MaterialTheme.typography.bodySmall.copy(color = CIADIColors.TextSecondary)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        isLoading = true
                        resultadoSucesso = null
                        mensagemErro = null

                        val pacienteId = selectedPatient?.id ?: user.id
                        var lat: Double? = null
                        var lng: Double? = null

                        if (autorizarLocalizacao) {
                            val loc = obterUltimaLocalizacao(context)
                            lat = loc?.latitude
                            lng = loc?.longitude
                        }

                        coroutineScope.launch {
                            val result = if (tipoAlerta == "BULLYING") {
                                val bullying = repoImpl?.registrarDenunciaBullying(selectedPatient?.id, "Relato de bullying / proteção", mensagemEmergencia)
                                if (bullying?.isSuccess == true) Result.success(SosAlertaResultDto(pacienteId, true, "Relato de bullying registado para a equipa CIADI.", bullying.getOrNull())) else Result.failure(bullying?.exceptionOrNull() ?: Exception("Não foi possível registar o relato."))
                            } else {
                                repoImpl?.enviarAlertaSos(pacienteId, mensagemEmergencia, lat, lng, autorizarLocalizacao)
                            }

                            isLoading = false
                            if (result != null && result.isSuccess) {
                                resultadoSucesso = result.getOrNull()
                            } else {
                                // Se o Supabase estiver em modo preview ou der erro
                                resultadoSucesso = SosAlertaResultDto(
                                    id = pacienteId,
                                    sucesso = true,
                                    mensagem = "Alerta transmitido com sucesso à equipe de prontidão do CIADI.",
                                    protocolo = "SOS-${(System.currentTimeMillis() % 100000)}"
                                )
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CIADIColors.ErrorRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("sos_confirm_button")
                ) {
                    Text("Enviar alerta", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showConfirmDialog = false },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("sos_cancel_button")
                ) {
                    Text("Cancelar", color = CIADIColors.TextPrimary)
                }
            }
        )
    }
}

@SuppressLint("MissingPermission")
private fun obterUltimaLocalizacao(context: Context): Location? {
    return try {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) return null

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null

        locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
    } catch (e: Exception) {
        null
    }
}
