package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.remote.dto.PortalAgendamentoDto
import com.example.domain.model.UserProfile
import com.example.domain.model.UserRole
import com.example.domain.model.UserSession
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.ModulesRepository
import com.example.ui.screens.agenda.AgendaModuleScreen
import com.example.ui.screens.at.AtEnvironmentScreen
import com.example.ui.screens.chat.ChatModuleScreen
import com.example.ui.screens.dashboard.MainDashboardScreen
import com.example.ui.screens.documents.DocumentsModuleScreen
import com.example.ui.screens.forms.FormsModuleScreen
import com.example.ui.screens.janeth.JanethScreen
import com.example.ui.screens.login.LoginScreen
import com.example.ui.screens.login.LoginViewModel
import com.example.ui.screens.notifications.NotificationsModuleScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.solicitacao.SolicitarAcessoScreen
import com.example.ui.screens.sos.SosScreen
import com.example.ui.screens.splash.SplashScreen
import com.example.ui.screens.tracking.TrackingModuleScreen
import com.example.ui.screens.unauthorized.UnauthorizedScreen
import com.example.ui.screens.virtualclinic.ClinicaVirtualScreen
import com.example.ui.screens.virtualclinic.SalaVideoScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(
    authRepository: AuthRepository,
    modulesRepository: ModulesRepository? = null,
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    val session by authRepository.sessionFlow.collectAsState()
    val supabaseStatus = remember { authRepository.getSupabaseStatus() }

    val fallbackUser = remember {
        UserProfile(
            id = "",
            email = "",
            fullName = "Utilizador CIADI+",
            role = UserRole.RESPONSAVEL,
            activePatientName = null
        )
    }

    val activeUser = (session as? UserSession.Authenticated)?.user ?: fallbackUser
    val activeMeetingForVideo = remember { mutableStateOf<PortalAgendamentoDto?>(null) }
    val blockedMessageState = remember { mutableStateOf("") }

    NavHost(
        navController = navController,
        startDestination = NavRoute.Splash.route,
        modifier = modifier
    ) {
        composable(NavRoute.Splash.route) {
            SplashScreen(
                session = session,
                onNavigateToDashboard = {
                    val authUser = (session as? UserSession.Authenticated)?.user
                    if (authUser != null && !authUser.isAuthorized) {
                        blockedMessageState.value = if (!authUser.ativo) {
                            "O seu acesso ao CIADI+ está desativado. Contacte a administração do CIADI."
                        } else if (authUser.statusAprovacao.equals("pendente", ignoreCase = true)) {
                            "O seu pedido de acesso está pendente de aprovação pelo CIADI."
                        } else {
                            "O seu perfil ainda não está autorizado a utilizar esta área do CIADI+. Contacte a administração do CIADI."
                        }
                        navController.navigate(NavRoute.Unauthorized.route) {
                            popUpTo(NavRoute.Splash.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(NavRoute.Dashboard.route) {
                            popUpTo(NavRoute.Splash.route) { inclusive = true }
                        }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(NavRoute.Login.route) {
                        popUpTo(NavRoute.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoute.Login.route) {
            val loginViewModel = remember { LoginViewModel(authRepository) }
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = {
                    navController.navigate(NavRoute.Dashboard.route) {
                        popUpTo(NavRoute.Login.route) { inclusive = true }
                    }
                },
                onNavigateToBlocked = { reason ->
                    blockedMessageState.value = reason
                    navController.navigate(NavRoute.Unauthorized.route)
                },
                onNavigateToSolicitarAcesso = {
                    navController.navigate(NavRoute.SolicitarAcesso.route)
                }
            )
        }

        composable(NavRoute.Unauthorized.route) {
            UnauthorizedScreen(
                message = blockedMessageState.value,
                onSignOut = {
                    CoroutineScope(Dispatchers.Main).launch {
                        modulesRepository?.clearCache()
                        authRepository.signOut()
                        navController.navigate(NavRoute.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                onNavigateToHome = {
                    CoroutineScope(Dispatchers.Main).launch {
                        modulesRepository?.clearCache()
                        authRepository.signOut()
                        navController.navigate(NavRoute.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(NavRoute.SolicitarAcesso.route) {
            SolicitarAcessoScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NavRoute.Dashboard.route) {
            if (activeUser.role == UserRole.AT) {
                AtEnvironmentScreen(
                    user = activeUser,
                    modulesRepository = modulesRepository,
                    onNavigateToChat = { navController.navigate(NavRoute.Chat.route) },
                    onNavigateToProfile = { navController.navigate(NavRoute.Profile.route) },
                    onNavigateToNotifications = { navController.navigate(NavRoute.Notifications.route) },
                    onNavigateToSos = { navController.navigate(NavRoute.Sos.route) },
                    onNavigateToJaneth = { navController.navigate(NavRoute.Janeth.route) },
                    onNavigateToForms = { navController.navigate(NavRoute.Forms.route) },
                    onSignOut = {
                        CoroutineScope(Dispatchers.Main).launch {
                            modulesRepository?.clearCache()
                            authRepository.signOut()
                            navController.navigate(NavRoute.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            } else {
                MainDashboardScreen(
                    user = activeUser,
                    supabaseStatus = supabaseStatus,
                    onRoleSwitch = { role ->
                        authRepository.switchRoleForPreview(role)
                    },
                    onNavigateToModule = { route ->
                        when (route) {
                            "module_agenda", "agenda" -> navController.navigate(NavRoute.Agenda.route)
                            "module_tracking", "tracking" -> navController.navigate(NavRoute.Tracking.route)
                            "module_forms", "forms" -> navController.navigate(NavRoute.Forms.route)
                            "module_documents", "documents" -> navController.navigate(NavRoute.Documents.route)
                            "module_chat", "chat" -> navController.navigate(NavRoute.Chat.route)
                            "module_notifications", "notifications" -> navController.navigate(NavRoute.Notifications.route)
                        }
                    },
                    onNavigateToJaneth = {
                        navController.navigate(NavRoute.Janeth.route)
                    },
                    onNavigateToProfile = {
                        navController.navigate(NavRoute.Profile.route)
                    },
                    onNavigateToSos = {
                        navController.navigate(NavRoute.Sos.route)
                    },
                    onNavigateToVirtualClinic = { agendamento ->
                        activeMeetingForVideo.value = agendamento
                        navController.navigate(NavRoute.VirtualClinic.route)
                    },
                    modulesRepository = modulesRepository
                )
            }
        }

        composable(NavRoute.Agenda.route) {
            AgendaModuleScreen(
                user = activeUser,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToVirtualClinic = { agendamento ->
                    activeMeetingForVideo.value = agendamento
                    navController.navigate(NavRoute.VirtualClinic.route)
                },
                onNavigateToRoute = { route ->
                    when (route) {
                        "janeth" -> navController.navigate(NavRoute.Janeth.route)
                        "profile" -> navController.navigate(NavRoute.Profile.route)
                        "dashboard" -> navController.navigate(NavRoute.Dashboard.route)
                        "tracking" -> navController.navigate(NavRoute.Tracking.route)
                    }
                },
                modulesRepository = modulesRepository
            )
        }

        composable(NavRoute.Tracking.route) {
            TrackingModuleScreen(
                user = activeUser,
                onNavigateBack = { navController.popBackStack() },
                modulesRepository = modulesRepository
            )
        }

        composable(NavRoute.Forms.route) {
            FormsModuleScreen(
                user = activeUser,
                onNavigateBack = { navController.popBackStack() },
                modulesRepository = modulesRepository
            )
        }

        composable(NavRoute.Documents.route) {
            DocumentsModuleScreen(
                user = activeUser,
                onNavigateBack = { navController.popBackStack() },
                modulesRepository = modulesRepository
            )
        }

        composable(NavRoute.Janeth.route) {
            JanethScreen(
                user = activeUser,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NavRoute.Sos.route) {
            SosScreen(
                user = activeUser,
                onNavigateBack = { navController.popBackStack() },
                modulesRepository = modulesRepository
            )
        }

        composable(NavRoute.VirtualClinic.route) {
            ClinicaVirtualScreen(
                user = activeUser,
                onNavigateBack = { navController.popBackStack() },
                onEnterVideoRoom = { agendamento ->
                    activeMeetingForVideo.value = agendamento
                    navController.navigate("${NavRoute.VideoRoom.route}/${agendamento.id}/sala_01")
                },
                modulesRepository = modulesRepository
            )
        }

        composable("${NavRoute.VideoRoom.route}/{agendamentoId}/{salaId}") { backStackEntry ->
            val agendamentoId = backStackEntry.arguments?.getString("agendamentoId") ?: ""
            val currentAgendamento = activeMeetingForVideo.value ?: PortalAgendamentoDto(
                id = agendamentoId,
                paciente = "Assistido",
                profissional = "Equipe CIADI",
                especialidade = "Consulta Online",
                dataHora = "Hoje",
                modalidade = "Online"
            )
            SalaVideoScreen(
                user = activeUser,
                agendamento = currentAgendamento,
                onNavigateBack = { navController.popBackStack() },
                modulesRepository = modulesRepository
            )
        }

        composable(NavRoute.Chat.route) {
            ChatModuleScreen(
                user = activeUser,
                onNavigateBack = { navController.popBackStack() },
                modulesRepository = modulesRepository
            )
        }

        composable(NavRoute.Notifications.route) {
            NotificationsModuleScreen(
                user = activeUser,
                onNavigateBack = { navController.popBackStack() },
                modulesRepository = modulesRepository
            )
        }

        composable(NavRoute.Profile.route) {
            ProfileScreen(
                user = activeUser,
                onNavigateBack = { navController.popBackStack() },
                onSignOut = {
                    CoroutineScope(Dispatchers.Main).launch {
                        modulesRepository?.clearCache()
                        authRepository.signOut()
                        navController.navigate(NavRoute.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }
    }
}
