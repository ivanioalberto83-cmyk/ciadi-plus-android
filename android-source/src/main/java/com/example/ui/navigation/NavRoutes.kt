package com.example.ui.navigation

sealed class NavRoute(val route: String) {
    data object Splash : NavRoute("splash")
    data object Login : NavRoute("login")
    data object Dashboard : NavRoute("dashboard")
    data object Agenda : NavRoute("agenda")
    data object Tracking : NavRoute("tracking")
    data object Forms : NavRoute("forms")
    data object Documents : NavRoute("documents")
    data object Chat : NavRoute("chat")
    data object Notifications : NavRoute("notifications")
    data object Profile : NavRoute("profile")
    data object Janeth : NavRoute("janeth")
    data object Sos : NavRoute("sos")
    data object VirtualClinic : NavRoute("virtual_clinic")
    data object VideoRoom : NavRoute("video_room")
    data object Unauthorized : NavRoute("unauthorized")
    data object SolicitarAcesso : NavRoute("solicitar_acesso")
}
