package com.example.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.config.SupabaseConfig
import com.example.data.remote.dto.NotificacaoDto
import com.example.data.repository.SupabaseModulesRepositoryImpl
import com.example.domain.model.UserProfile
import com.example.domain.repository.ModulesRepository
import com.example.ui.components.CiadiTopBar
import com.example.ui.theme.CIADIColors
import kotlinx.coroutines.launch

@Composable
fun NotificationsModuleScreen(
    user: UserProfile,
    onNavigateBack: () -> Unit,
    modulesRepository: ModulesRepository? = null,
    modifier: Modifier = Modifier
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val repo = modulesRepository as? SupabaseModulesRepositoryImpl
    val notifications by (modulesRepository?.observeNotificacoes()?.collectAsState(initial = emptyList())
        ?: remember { mutableStateOf(emptyList<NotificacaoDto>()) })
    var loading by remember { mutableStateOf(false) }
    val unread = notifications.count { !it.lida }

    LaunchedEffect(modulesRepository) {
        if (modulesRepository != null) {
            loading = true
            val result = modulesRepository.sincronizarTabela(SupabaseConfig.Tables.NOTIFICACOES)
            loading = false
            if (result.isFailure) snackbar.showSnackbar(result.exceptionOrNull()?.message ?: "Não foi possível carregar as notificações.")
        }
    }

    Scaffold(
        topBar = {
            CiadiTopBar(
                title = if (unread > 0) "Notificações ($unread)" else "Notificações & Alertas",
                currentUser = user,
                onNavigateBack = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Alertas e comunicados CIADI", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CIADIColors.Brown)
                    Text("$unread por ler • ${notifications.size} no total", fontSize = 12.sp, color = CIADIColors.TextSecondary)
                }
                IconButton(enabled = !loading && modulesRepository != null, onClick = {
                    scope.launch {
                        loading = true
                        val result = modulesRepository?.sincronizarTabela(SupabaseConfig.Tables.NOTIFICACOES)
                        loading = false
                        snackbar.showSnackbar(if (result?.isSuccess == true) "Notificações atualizadas." else result?.exceptionOrNull()?.message ?: "Não foi possível atualizar.")
                    }
                }) { Icon(Icons.Default.Refresh, contentDescription = "Atualizar notificações") }
                IconButton(enabled = unread > 0 && repo != null && !loading, onClick = {
                    scope.launch {
                        loading = true
                        var failures = 0
                        notifications.filter { !it.lida }.forEach { item ->
                            if (repo?.marcarLida(item.id)?.isFailure == true) failures++
                        }
                        val refresh = modulesRepository?.sincronizarTabela(SupabaseConfig.Tables.NOTIFICACOES)
                        loading = false
                        snackbar.showSnackbar(if (failures == 0 && refresh?.isSuccess == true) "Notificações marcadas como lidas." else "Algumas notificações não foram atualizadas. Verifique a ligação e as permissões.")
                    }
                }) { Icon(Icons.Default.DoneAll, contentDescription = "Marcar todas como lidas") }
            }

            if (loading) Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CIADIColors.OrangePrimary)
            }

            if (notifications.isEmpty() && !loading) {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(44.dp), tint = CIADIColors.OrangePrimary)
                        Text("Sem notificações", fontWeight = FontWeight.Bold, color = CIADIColors.Brown)
                        Text("Quando receber alertas ou comunicados autorizados, aparecerão aqui.", color = CIADIColors.TextSecondary, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                    items(notifications, key = { it.id }) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                if (!item.lida && repo != null) scope.launch {
                                    val result = repo.marcarLida(item.id)
                                    if (result.isFailure) snackbar.showSnackbar(result.exceptionOrNull()?.message ?: "Não foi possível marcar como lida.")
                                }
                                if (!item.linkAcao.isNullOrBlank()) scope.launch { snackbar.showSnackbar("Ação associada: ${item.linkAcao}") }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = if (item.lida) Color.White else CIADIColors.CreamLight),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(Modifier.size(38.dp).background(CIADIColors.OrangePrimary.copy(alpha = 0.14f), CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Notifications, contentDescription = null, tint = CIADIColors.OrangePrimary)
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(item.titulo, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = CIADIColors.Brown)
                                        if (!item.lida) Text("NOVA", color = CIADIColors.OrangePrimary, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                    Text(item.mensagem, color = CIADIColors.TextPrimary, fontSize = 13.sp)
                                    Text(listOfNotNull(item.tipo?.takeIf { it.isNotBlank() }, item.criadoEm?.takeIf { it.isNotBlank() }).joinToString(" • "), color = CIADIColors.TextSecondary, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
