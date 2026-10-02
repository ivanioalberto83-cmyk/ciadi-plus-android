package com.example.domain.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.CIADIColors

/**
 * Perfis de acesso oficiais do ecossistema digital CIADI+.
 *
 * Mapeados diretamente para as colunas 'tipo'/'role' da tabela 'perfis' e claims do Supabase/PostgreSQL com RLS.
 */
enum class UserRole(
    val label: String,
    val description: String,
    val badgeColor: Color,
    val code: String
) {
    RESPONSAVEL(
        label = "Família / Responsável",
        description = "Portal da Família: acompanhamento do assistido vinculado, rotinas, agenda, P.E.I. e SOS",
        badgeColor = CIADIColors.RoleFamily,
        code = "responsavel"
    ),
    AT(
        label = "A.T. — Acompanhante Terapêutico",
        description = "Acompanhamento Terapêutico: registros de campo/escola, sessões, metas P.E.I. e ocorrências",
        badgeColor = CIADIColors.RoleAt,
        code = "at"
    ),
    PROFISSIONAL(
        label = "Profissional Especializado",
        description = "Equipe Multidisciplinar (Psicologia, Fonoaudiologia, Terapia Ocupacional, Pediatria)",
        badgeColor = CIADIColors.RoleProfessional,
        code = "profissional"
    ),
    GESTOR(
        label = "Gestão & Coordenação",
        description = "Supervisão clínica, coordenação de equipe e fluxos institucionais",
        badgeColor = CIADIColors.RoleGestor,
        code = "gestor"
    ),
    ADMIN(
        label = "Administração",
        description = "Gestão global da unidade, auditoria, configurações e políticas de segurança RLS",
        badgeColor = CIADIColors.RoleAdmin,
        code = "admin"
    ),
    PACIENTE(
        label = "Assistido / Paciente",
        description = "Acesso adaptado para o assistido/aprendiz em desenvolvimento",
        badgeColor = CIADIColors.OrangePrimary,
        code = "paciente"
    ),
    OPERADOR(
        label = "Operador / Secretaria",
        description = "Recepção, triagem inicial e agendamentos de salas",
        badgeColor = CIADIColors.RoleOperador,
        code = "operador"
    );

    companion object {
        fun fromCode(code: String?): UserRole? {
            return resolve(code = code, email = null, funcao = null)
        }

        fun resolve(code: String?, email: String? = null, funcao: String? = null): UserRole? {
            // 1. Verifica função específica de A.T. se informada
            if (!funcao.isNullOrBlank()) {
                val f = funcao.trim().lowercase()
                if (f.contains("acompanhante") || f.contains("a.t.") || f.contains("at ") || f == "at") {
                    return AT
                }
            }

            // 2. Normaliza e analisa o código de role/tipo
            val raw = code?.trim()?.lowercase()
            if (!raw.isNullOrBlank() && raw != "authenticated" && raw != "user" && raw != "default") {
                // Checagem direta de entries
                val directMatch = entries.firstOrNull { it.code.equals(raw, ignoreCase = true) }
                if (directMatch != null) return directMatch

                // Variações de A.T.
                if (raw == "at" || raw == "a.t." || raw == "a.t" || raw.contains("acompanhante") || raw.contains("therapeutic")) {
                    return AT
                }
                // Variações de Admin
                if (raw == "admin" || raw.contains("administra") || raw == "coordenador_geral" || raw == "superadmin") {
                    return ADMIN
                }
                // Variações de Gestor
                if (raw == "gestor" || raw.contains("gestao") || raw.contains("gestão") || raw.contains("coordena")) {
                    return GESTOR
                }
                // Variações de Profissional
                if (raw == "profissional" || raw.contains("terapeuta") || raw.contains("especialista") ||
                    raw.contains("psico") || raw.contains("fono") || raw.contains("medico") || raw.contains("médico")) {
                    return PROFISSIONAL
                }
                // Variações de Responsável / Família
                if (raw == "responsavel" || raw.contains("respons") || raw.contains("famil") || raw == "pai" || raw == "mae" || raw == "mãe" || raw == "tutor") {
                    return RESPONSAVEL
                }
                // Variações de Paciente
                if (raw == "paciente" || raw.contains("assistid") || raw.contains("crianc") || raw.contains("crianç")) {
                    return PACIENTE
                }
                // Variações de Operador / Secretaria
                if (raw == "operador" || raw.contains("secretar") || raw.contains("recepc") || raw.contains("atendente")) {
                    return OPERADOR
                }
            }

            // 3. Resolução complementar baseada no identificador do e-mail
            if (!email.isNullOrBlank()) {
                val em = email.trim().lowercase()
                if (em.contains(".admin@") || em.contains("admin.") || em.startsWith("admin@") || em.contains("administrador")) {
                    return ADMIN
                }
                if (em.contains(".at@") || em.contains("at.") || em.contains("_at@") || em.contains("acompanhante")) {
                    return AT
                }
                if (em.contains(".gestor@") || em.contains("gestor.") || em.contains("coordenacao")) {
                    return GESTOR
                }
                if (em.contains(".profissional@") || em.contains("psicologia") || em.contains("terapeuta") || em.contains("medico")) {
                    return PROFISSIONAL
                }
                if (em.contains(".familia@") || em.contains("familia.") || em.contains("responsavel")) {
                    return RESPONSAVEL
                }
                if (em.contains(".paciente@") || em.contains("paciente.")) {
                    return PACIENTE
                }
                if (em.contains(".operador@") || em.contains("recepcao") || em.contains("secretaria")) {
                    return OPERADOR
                }
            }

            // REGRA ABSOLUTA (Item 40):
            // Um perfil não autorizado NUNCA deve ser convertido automaticamente em responsavel, familia ou outro.
            // Retorna null quando o perfil/role não for conhecido.
            return null
        }
    }
}
