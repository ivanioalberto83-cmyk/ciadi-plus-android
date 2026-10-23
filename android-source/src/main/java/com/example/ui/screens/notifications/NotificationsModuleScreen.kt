package com.example.ui.screens.notifications

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.remote.dto.NotificacaoDto
import com.example.domain.model.Permission
import com.example.domain.model.UserProfile
import com.example.domain.repository.ModulesRepository
import com.example.domain.repository.NotificationsRepository
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
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val notificationsRepository = modulesRepository as? NotificationsRepository
    val notifications by (notificationsRepository?.observeNotificacoes()?.collectAsState(initial = emptyList())
        ?: remember { androidx.compose.runtime.mutableStateOf(emptyList<NotificacaoDto>()) })

    LaunchedEffect(notificationsRepository) {
        if (notificationsRepository != null) {
            val result = notificationsRepository.syncNotificacoes()
            if (result.isFailure) {
                snackbarHostState.showSnackbar(
                    result.exceptionOrNull()?.message ?: "Não foi possível sincronizar as notificações."
                )
            }
        }
    }

    Scaffold(
        topBar = {
            CiadiTopBar(
                title = "Notificações & Alertas",
                currentUser = user,
                onNavigateBack = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (!user.hasPermission(Permission.RECEIVE_NOTIFICATIONS)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null)
                Spacer(Modifier.height(12.dp))
                Text("O seu perfil não tem autorização para receber notificações.", style = MaterialTheme.typography.bodyLarge)
            }
        } else if (notificationsRepository == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Módulo de notificações sem ligação ao repositório Supabase.")
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = {
                    scope.launch { snackbarHostState.showSnackbar("Não foi possível atualizar: repositório não ligado.") }
                }) { Text("Ver estado da ligação") }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val result = notificationsRepository.syncNotificacoes()
                                snackbarHostState.showSnackbar(
                                    if (result.isSuccess) "Notificações atualizadas."
                                    else result.exceptionOrNull()?.message ?: "Falha ao atualizar notificações."
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Text(" Atualizar")
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                val unread = notifications.filterNot { it.lida }
                                var failures = 0
                                unread.forEach { item ->
                                    if (notificationsRepository.marcarLida(item.id).isFailure) failures++
                                }
                                snackbarHostState.showSnackbar(
                                    if (failures == 0) {
                                        if (unread.isEmpty()) "Não existem notificações por ler."
                                        else "${unread.size} notificação(ões) marcada(s) como lida(s)."
                                    } else {
                                        "Falha ao marcar $failures notificação(ões). Verifique a ligação e as permissões."
                                    }
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.DoneAll, contentDescription = null)
                        Text(" Lidas")
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (notifications.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null)
                        Spacer(Modifier.height(8.dp))
                        Text("Ainda não existem notificações disponíveis.")
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(notifications, key = { it.id }) { item ->
                            NotificationCard(
                                item = item,
                                onMarkRead = {
                                    scope.launch {
                                        val result = notificationsRepository.marcarLida(item.id)
                                        snackbarHostState.showSnackbar(
                                            if (result.isSuccess) "Notificação marcada como lida."
                                            else result.exceptionOrNull()?.message ?: "Não foi possível atualizar a notificação."
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(item: NotificacaoDto, onMarkRead: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CIADIColors.SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.lida) 1.dp else 3.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.titulo,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = CIADIColors.Brown
                )
                Text(
                    text = if (item.lida) "Lida" else "Nova",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (item.lida) CIADIColors.TextSecondary else CIADIColors.OrangePrimary
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(item.mensagem, style = MaterialTheme.typography.bodyMedium)
            val metadata = listOfNotNull(item.tipo?.takeIf { it.isNotBlank() }, item.criadoEm?.takeIf { it.isNotBlank() })
            if (metadata.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(metadata.joinToString(" • "), style = MaterialTheme.typography.bodySmall, color = CIADIColors.TextSecondary)
            }
            if (!item.lida) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onMarkRead, modifier = Modifier.align(Alignment.End)) {
                    Text("Marcar como lida")
                }
            }
        }
    }
}
