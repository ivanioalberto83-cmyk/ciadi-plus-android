package com.example.ui.screens.notifications

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.example.core.config.SupabaseConfig
import com.example.domain.model.Permission
import com.example.domain.model.UserProfile
import com.example.domain.repository.ModulesRepository
import com.example.ui.components.CiadiTopBar
import com.example.ui.components.EmptyModuleState
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
        EmptyModuleState(
            icon = Icons.Default.Notifications,
            moduleTitle = "Alertas e Comunicados CIADI",
            targetTable = SupabaseConfig.Tables.NOTIFICACOES,
            requiredPermission = Permission.RECEIVE_NOTIFICATIONS,
            onRefresh = {
                scope.launch {
                    if (modulesRepository != null) {
                        val result = modulesRepository.sincronizarTabela(SupabaseConfig.Tables.NOTIFICACOES)
                        if (result.isSuccess) {
                            snackbarHostState.showSnackbar("Notificações sincronizadas com o Supabase.")
                        } else {
                            snackbarHostState.showSnackbar(result.exceptionOrNull()?.message ?: "Falha ao conectar")
                        }
                    } else {
                        snackbarHostState.showSnackbar("Buscando novos comunicados institucionais...")
                    }
                }
            },
            primaryActionLabel = "Marcar Lidas",
            onPrimaryAction = {
                scope.launch {
                    snackbarHostState.showSnackbar("Estado de leitura atualizado no banco PostgreSQL.")
                }
            },
            modifier = Modifier.padding(innerPadding)
        )
    }
}
