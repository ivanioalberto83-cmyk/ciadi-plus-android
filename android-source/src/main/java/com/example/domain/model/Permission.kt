package com.example.domain.model

/**
 * Permissões do ecossistema CIADI+.
 *
 * Mapeadas diretamente para as políticas de segurança RLS (Row Level Security) do Supabase PostgreSQL
 * sobre as tabelas reais do CIADI.
 */
enum class Permission(
    val code: String,
    val title: String,
    val targetTable: String,
    val rlsPolicyName: String
) {
    VIEW_AGENDA(
        code = "agenda:view",
        title = "Visualizar horários autorizados",
        targetTable = "agendamentos",
        rlsPolicyName = "agendamentos_select_autorizados"
    ),
    MANAGE_AGENDA(
        code = "agenda:manage",
        title = "Cadastrar e remanejar agendamentos",
        targetTable = "agendamentos",
        rlsPolicyName = "agendamentos_profissional_gestor_manage"
    ),
    VIEW_VINCULO_CRIANCAS(
        code = "criancas:view_vinculadas",
        title = "Visualizar crianças/assistidos autorizados",
        targetTable = "pacientes",
        rlsPolicyName = "pacientes_select_por_vinculo"
    ),
    VIEW_ATRIBUICOES_AT(
        code = "at:atribuicoes",
        title = "Visualizar atribuições ativas de A.T.",
        targetTable = "ciadi_atribuicoes_at",
        rlsPolicyName = "atribuicoes_at_select_proprias"
    ),
    WRITE_AT_SESSAO(
        code = "at:sessoes_write",
        title = "Registrar sessões e objetivos de A.T.",
        targetTable = "ciadi_at_sessoes",
        rlsPolicyName = "at_sessoes_insert_autorizado"
    ),
    WRITE_AT_INCIDENTE(
        code = "at:incidentes_write",
        title = "Registrar ocorrências e incidentes de campo",
        targetTable = "ciadi_at_incidentes",
        rlsPolicyName = "at_incidentes_insert_autorizado"
    ),
    VIEW_AT_SUPERVISAO(
        code = "at:supervisoes_view",
        title = "Acessar orientações de supervisão",
        targetTable = "ciadi_at_supervisoes",
        rlsPolicyName = "at_supervisoes_select_autorizado"
    ),
    VIEW_CLINICAL_FORMS(
        code = "forms:view",
        title = "Acessar formulários clínicos autorizados",
        targetTable = "ciadi_formularios_clinicos",
        rlsPolicyName = "formularios_clinicos_select_autorizado"
    ),
    SUBMIT_CLINICAL_FORM(
        code = "forms:submit",
        title = "Preencher e submeter formulários clínicos",
        targetTable = "ciadi_formularios_clinicos",
        rlsPolicyName = "formularios_clinicos_submit_autorizado"
    ),
    VIEW_DOCUMENTS_PUBLISHED(
        code = "documents:view_published",
        title = "Acessar laudos e P.E.I. publicados à família",
        targetTable = "documentos_clinicos",
        rlsPolicyName = "documentos_select_publicados_familia"
    ),
    MANAGE_DOCUMENTS(
        code = "documents:manage",
        title = "Criar, revisar e assinar documentos clínicos",
        targetTable = "documentos_clinicos",
        rlsPolicyName = "documentos_clinicos_terapeuta_gestao"
    ),
    ACCESS_CHAT(
        code = "chat:access",
        title = "Comunicação em grupos autorizados",
        targetTable = "ciadi_chat_mensagens",
        rlsPolicyName = "chat_mensagens_membros_autorizados"
    ),
    RECEIVE_NOTIFICATIONS(
        code = "notifications:view",
        title = "Receber alertas institucionais e de atendimento",
        targetTable = "notificacoes",
        rlsPolicyName = "notificacoes_select_usuario"
    ),
    ADMIN_AUDIT(
        code = "admin:audit",
        title = "Acesso de auditoria e conformidade",
        targetTable = "perfis",
        rlsPolicyName = "perfis_admin_gestor_select"
    );

    companion object {
        fun defaultPermissionsFor(role: UserRole): Set<Permission> {
            return when (role) {
                UserRole.RESPONSAVEL -> setOf(
                    VIEW_AGENDA,
                    VIEW_VINCULO_CRIANCAS,
                    VIEW_DOCUMENTS_PUBLISHED,
                    VIEW_CLINICAL_FORMS,
                    ACCESS_CHAT,
                    RECEIVE_NOTIFICATIONS
                )
                UserRole.AT -> setOf(
                    VIEW_AGENDA,
                    VIEW_ATRIBUICOES_AT,
                    WRITE_AT_SESSAO,
                    WRITE_AT_INCIDENTE,
                    VIEW_AT_SUPERVISAO,
                    VIEW_CLINICAL_FORMS,
                    VIEW_DOCUMENTS_PUBLISHED,
                    ACCESS_CHAT,
                    RECEIVE_NOTIFICATIONS
                )
                UserRole.PROFISSIONAL -> setOf(
                    VIEW_AGENDA,
                    MANAGE_AGENDA,
                    VIEW_VINCULO_CRIANCAS,
                    VIEW_CLINICAL_FORMS,
                    SUBMIT_CLINICAL_FORM,
                    VIEW_DOCUMENTS_PUBLISHED,
                    MANAGE_DOCUMENTS,
                    ACCESS_CHAT,
                    RECEIVE_NOTIFICATIONS
                )
                UserRole.GESTOR -> setOf(
                    VIEW_AGENDA,
                    MANAGE_AGENDA,
                    VIEW_VINCULO_CRIANCAS,
                    VIEW_ATRIBUICOES_AT,
                    VIEW_AT_SUPERVISAO,
                    VIEW_CLINICAL_FORMS,
                    SUBMIT_CLINICAL_FORM,
                    VIEW_DOCUMENTS_PUBLISHED,
                    MANAGE_DOCUMENTS,
                    ACCESS_CHAT,
                    RECEIVE_NOTIFICATIONS,
                    ADMIN_AUDIT
                )
                UserRole.ADMIN -> Permission.entries.toSet()
                UserRole.PACIENTE -> setOf(
                    VIEW_AGENDA,
                    VIEW_DOCUMENTS_PUBLISHED,
                    ACCESS_CHAT,
                    RECEIVE_NOTIFICATIONS
                )
                UserRole.OPERADOR -> setOf(
                    VIEW_AGENDA,
                    MANAGE_AGENDA,
                    RECEIVE_NOTIFICATIONS
                )
            }
        }
    }
}
