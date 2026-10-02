package com.example.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Módulos funcionais preparados para integração com o Supabase CIADI.
 */
enum class ModuleType(
    val route: String,
    val title: String,
    val shortDescription: String,
    val icon: ImageVector,
    val requiredPermission: Permission,
    val targetTable: String
) {
    AGENDA(
        route = "module_agenda",
        title = "Agenda & Horários",
        shortDescription = "Consultas, sessões multidisciplinares e remanejamentos",
        icon = Icons.Default.CalendarMonth,
        requiredPermission = Permission.VIEW_AGENDA,
        targetTable = "agendamentos"
    ),
    TRACKING(
        route = "module_tracking",
        title = "Acompanhamento Terapêutico",
        shortDescription = "Metas do P.E.I., registros diários do A.T., sessões e evolução",
        icon = Icons.Default.Timeline,
        requiredPermission = Permission.VIEW_ATRIBUICOES_AT,
        targetTable = "ciadi_at_sessoes"
    ),
    FORMS(
        route = "module_forms",
        title = "Formulários Clínicos",
        shortDescription = "Anamneses, protocolos de avaliação e triagem",
        icon = Icons.AutoMirrored.Filled.Assignment,
        requiredPermission = Permission.VIEW_CLINICAL_FORMS,
        targetTable = "ciadi_formularios_clinicos"
    ),
    DOCUMENTS(
        route = "module_documents",
        title = "Documentos & Relatórios",
        shortDescription = "Laudos, relatórios circunstanciados, P.E.I. e autorizações",
        icon = Icons.Default.Description,
        requiredPermission = Permission.VIEW_DOCUMENTS_PUBLISHED,
        targetTable = "documentos_clinicos"
    ),
    CHAT(
        route = "module_chat",
        title = "Equipe Multidisciplinar",
        shortDescription = "Canal seguro entre terapeutas, A.T.s e família",
        icon = Icons.AutoMirrored.Filled.Chat,
        requiredPermission = Permission.ACCESS_CHAT,
        targetTable = "ciadi_chat_mensagens"
    ),
    NOTIFICATIONS(
        route = "module_notifications",
        title = "Notificações & Alertas",
        shortDescription = "Lembretes de atendimento e comunicados oficiais",
        icon = Icons.Default.Notifications,
        requiredPermission = Permission.RECEIVE_NOTIFICATIONS,
        targetTable = "notificacoes"
    );

    companion object {
        fun modulesForRole(role: UserRole): List<ModuleType> {
            val userPerms = Permission.defaultPermissionsFor(role)
            return entries.filter { userPerms.contains(it.requiredPermission) || role == UserRole.ADMIN }
        }
    }
}
