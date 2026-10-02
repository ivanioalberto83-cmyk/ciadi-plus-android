package com.example.domain.janeth

/**
 * Base de Conhecimento Oficial do CIADI para a Janeth
 * Centro Integrado de Apoio e Desenvolvimento Individual
 */
object JanethKnowledgeBase {

    val INSTITUTIONAL_SUMMARY = """
        O CIADI (Centro Integrado de Apoio e Desenvolvimento Individual) é uma instituição especializada no desenvolvimento neuropsicomotor, acolhimento familiar e intervenção multidisciplinar integrada.
        Nossa missão é promover o potencial máximo de cada indivíduo com ética, afeto, rigor técnico e integração contínua entre família, escola e terapeutas.
    """.trimIndent()

    val DISCIPLINES = listOf(
        "Psicologia e Análise do Comportamento Aplicada (ABA)",
        "Terapia da Fala / Fonoaudiologia",
        "Terapia Ocupacional e Integração Sensorial",
        "Acompanhamento Terapêutico (A.T.) Escolar e Domiciliar",
        "Psicomotricidade e Neurodesenvolvimento",
        "Pediatria e Avaliação do Desenvolvimento"
    )

    val FAQ_ITEMS = listOf(
        FaqItem(
            topic = "O que é o P.E.I. no CIADI?",
            keywords = listOf("pei", "plano", "metas", "individualizado"),
            answer = "O P.E.I. (Plano Educacional e de Intervenção Individualizado) é o mapa terapêutico do assistido no CIADI. Nele, a equipe multidisciplinar estabelece objetivos claros e mensuráveis para comunicação, autonomia, interação social e aprendizagem, revisados periodicamente junto à família."
        ),
        FaqItem(
            topic = "Qual o papel do Acompanhante Terapêutico (A.T.)?",
            keywords = listOf("at", "acompanhante", "escola", "campo", "rotina"),
            answer = "O A.T. atua como elo em campo (escola, casa e comunidade), facilitando a generalização das habilidades trabalhadas na clínica, promovendo autonomia nas atividades de vida diária e acolhendo momentos de regulação emocional sob constante supervisão clínica."
        ),
        FaqItem(
            topic = "Como a família acompanha a evolução?",
            keywords = listOf("familia", "acompanhamento", "laudo", "relatorio", "documentos"),
            answer = "Pelo CIADI+, a família tem acesso à agenda de consultas, registros de sessões autorizados, documentos e relatórios técnicos assinados pelos terapeutas, além do canal direto de mensagens com a equipe multidisciplinar."
        ),
        FaqItem(
            topic = "Como solicitar remanejamento de horários?",
            keywords = listOf("horario", "agenda", "remarcar", "remanejamento", "consulta"),
            answer = "Na aba 'Agenda', você pode verificar os atendimentos confirmados e utilizar o botão de solicitação para notificar a recepção e a coordenação sobre necessidades de reagendamento."
        ),
        FaqItem(
            topic = "O CIADI faz diagnóstico ou receita remédios?",
            keywords = listOf("diagnostico", "remedio", "medicamento", "receita", "laudo"),
            answer = "O CIADI realiza avaliações clínicas multidisciplinares e emite laudos e pareceres detalhados. No entanto, diagnósticos formais e prescrição de medicamentos são atos médicos exclusivos de especialistas em Neuropediatria ou Psiquiatria Infantil. Eu, Janeth, não diagnostico e não indico medicações."
        )
    )

    private fun normalize(text: String): String {
        return java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .trim()
            .lowercase()
    }

    fun answerQuery(query: String): JanethResponse {
        val q = normalize(query)

        // Regra de segurança clínica estrita
        if (q.contains("medicamento") || q.contains("remedio") || q.contains("medicacao") || q.contains("dosagem") || q.contains("prescricao") || q.contains("posologia")) {
            return JanethResponse(
                text = "Como assistente institucional do CIADI, sigo diretrizes éticas rígidas: não realizo prescrição medicamentosa nem ajustes de dosagem. Para orientações farmacológicas, consulte o médico neuropediatra ou pediatra responsável pelo acompanhamento.",
                category = "Orientação Médica Exclusiva",
                suggestedActions = listOf("Falar com a Equipe", "Ver Agenda")
            )
        }

        if (q.contains("diagnostico") || q.contains("tem autismo") || q.contains("tem tdah") || q.contains("eh autista")) {
            return JanethResponse(
                text = "A determinação de um diagnóstico requer avaliação clínica criteriosa, anamnese e exames especializados conduzidos por equipe multiprofissional e médico responsável. O CIADI oferece avaliação multidisciplinar completa para apoiar essa jornada.",
                category = "Avaliação Multidisciplinar",
                suggestedActions = listOf("Conhecer Especialidades", "Falar com Coordenação")
            )
        }

        // Busca na base de conhecimento
        for (item in FAQ_ITEMS) {
            if (item.keywords.any { q.contains(it) }) {
                return JanethResponse(
                    text = item.answer,
                    category = item.topic,
                    suggestedActions = listOf("Ver Módulos", "Falar com Atendimento")
                )
            }
        }

        if (q.contains("servico") || q.contains("especialidade") || q.contains("atendimento")) {
            val listText = DISCIPLINES.joinToString("\n• ", prefix = "• ")
            return JanethResponse(
                text = "O CIADI conta com uma equipe transdisciplinar com as seguintes áreas de atuação:\n\n$listText\n\nTodos os atendimentos são integrados e alinhados aos objetivos do P.E.I.",
                category = "Especialidades CIADI",
                suggestedActions = listOf("Ver Agenda", "Ver Documentos")
            )
        }

        // Resposta padrão acolhedora
        return JanethResponse(
            text = "Olá! Sou a Janeth, assistente oficial do CIADI+. Posso orientar sobre os serviços do centro, rotinas de A.T., acompanhamento pelo P.E.I., documentos e navegação no aplicativo. Como posso apoiar você hoje?",
            category = "Atendimento Institucional CIADI",
            suggestedActions = listOf("O que é o P.E.I.?", "Como funciona o A.T.?", "Ver Especialidades")
        )
    }
}

data class FaqItem(
    val topic: String,
    val keywords: List<String>,
    val answer: String
)

data class JanethResponse(
    val text: String,
    val category: String,
    val suggestedActions: List<String>
)
