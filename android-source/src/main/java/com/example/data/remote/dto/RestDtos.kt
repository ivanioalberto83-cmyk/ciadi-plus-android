package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Data Transfer Objects (DTOs) oficiais do ecossistema CIADI no Supabase PostgreSQL.
 * Todos os nomes de tabelas e colunas refletem a estrutura real existente.
 */

// 1. Perfil (tabela: perfis)
@JsonClass(generateAdapter = true)
data class PerfilDto(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "nome_completo") val nomeCompleto: String? = null,
    @param:Json(name = "nome_exibicao") val nomeExibicao: String? = null,
    @param:Json(name = "telefone") val telefone: String? = null,
    @param:Json(name = "email") val email: String? = null,
    @param:Json(name = "avatar_url") val avatarUrl: String? = null,
    @param:Json(name = "tipo") val tipo: String? = null, // admin, gestor, profissional, paciente, responsavel, operador
    @param:Json(name = "role") val role: String? = null,
    @param:Json(name = "perfil") val perfil: String? = null,
    @param:Json(name = "ativo") val ativo: Boolean = true,
    @param:Json(name = "status") val status: String? = "aprovado",
    @param:Json(name = "status_aprovacao") val statusAprovacao: String? = null,
    @param:Json(name = "ultimo_acesso") val ultimoAcesso: String? = null,
    @param:Json(name = "funcao_at") val funcaoAt: String? = null,
    @param:Json(name = "unidade_nome") val unidadeNome: String? = "CIADI — Centro Integrado",
    @param:Json(name = "criado_em") val criadoEm: String? = null
)

// 2. Paciente / Assistido (tabela: pacientes)
@JsonClass(generateAdapter = true)
data class PacienteDto(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "nome") val nome: String = "",
    @param:Json(name = "nome_completo") val nomeCompleto: String? = null,
    @param:Json(name = "codigo_paciente") val codigoPaciente: String? = null,
    @param:Json(name = "data_nascimento") val dataNascimento: String? = null,
    @param:Json(name = "diagnostico_resumo") val diagnosticoResumo: String? = null,
    @param:Json(name = "foto_url") val fotoUrl: String? = null,
    @param:Json(name = "ativo") val ativo: Boolean = true,
    @param:Json(name = "criado_em") val criadoEm: String? = null
) {
    val displayName: String
        get() = nomeCompleto?.takeIf { it.isNotBlank() }
            ?: nome.takeIf { it.isNotBlank() }
            ?: "Assistido"
}

// 3. Vínculo Familiar (tabela: ciadi_vinculos_familiares)
@JsonClass(generateAdapter = true)
data class VinculoFamiliarDto(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "perfil_id") val perfilId: String,
    @param:Json(name = "paciente_id") val pacienteId: String,
    @param:Json(name = "parentesco") val parentesco: String? = null,
    @param:Json(name = "ativo") val ativo: Boolean = true,
    @param:Json(name = "responsavel_legal") val responsavelLegal: Boolean = true,
    @param:Json(name = "nivel_acesso") val nivelAcesso: String? = "TOTAL",
    @param:Json(name = "pode_agendar") val podeAgendar: Boolean = true,
    @param:Json(name = "pode_ver_documentos") val podeVerDocumentos: Boolean = true,
    @param:Json(name = "pode_ver_acompanhamento") val podeVerAcompanhamento: Boolean = true,
    @param:Json(name = "pode_receber_notificacoes") val podeReceberNotificacoes: Boolean = true,
    @param:Json(name = "pode_gerir_dados") val podeGerirDados: Boolean = false,
    @param:Json(name = "validade_ate") val validadeAte: String? = null,
    @param:Json(name = "paciente") val paciente: PacienteDto? = null
)

// 4. Atribuição de A.T. (tabela: ciadi_atribuicoes_at)
@JsonClass(generateAdapter = true)
data class AtribuicaoAtDto(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "profissional_id") val profissionalId: String,
    @param:Json(name = "paciente_id") val pacienteId: String,
    @param:Json(name = "escola_ou_local") val escolaOuLocal: String? = null,
    @param:Json(name = "turno") val turno: String? = null,
    @param:Json(name = "ativo") val ativo: Boolean = true,
    @param:Json(name = "paciente") val paciente: PacienteDto? = null,
    @param:Json(name = "pacientes") val pacientes: PacienteDto? = null
)

// 5. Sessão de A.T. (tabela: ciadi_at_sessoes)
@JsonClass(generateAdapter = true)
data class AtSessaoDto(
    @param:Json(name = "id") val id: String = "",
    @param:Json(name = "atribuicao_id") val atribuicaoId: String? = null,
    @param:Json(name = "profissional_at_id") val profissionalAtId: String? = null,
    @param:Json(name = "paciente_id") val pacienteId: String? = null,
    @param:Json(name = "paciente_nome") val pacienteNome: String? = null,
    @param:Json(name = "data_sessao") val dataSessao: String,
    @param:Json(name = "hora_inicio") val horaInicio: String? = null,
    @param:Json(name = "hora_fim") val horaFim: String? = null,
    @param:Json(name = "resumo_observacoes") val resumoObservacoes: String? = null,
    @param:Json(name = "status") val status: String? = "FINALIZADA",
    @param:Json(name = "objetivo") val objetivo: String? = null,
    @param:Json(name = "atividades") val atividades: String? = null,
    @param:Json(name = "comportamento_observado") val comportamentoObservado: String? = null,
    @param:Json(name = "progresso") val progresso: String? = null,
    @param:Json(name = "comunicacao_familia") val comunicacaoFamilia: String? = null,
    @param:Json(name = "observacoes") val observacoes: String? = null,
    @param:Json(name = "estado") val estado: String? = "Realizada",
    @param:Json(name = "criado_em") val criadoEm: String? = null
)

// 6. Registro de Objetivo P.E.I. em Sessão (tabela: ciadi_at_registos_objetivos)
@JsonClass(generateAdapter = true)
data class AtRegistoObjetivoDto(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "sessao_id") val sessaoId: String,
    @param:Json(name = "objetivo_pei") val objetivoPei: String,
    @param:Json(name = "nivel_autonomia") val nivelAutonomia: String? = null, // INDEPENDENTE, AJUDA_LEVE, AJUDA_MODERADA, AJUDA_TOTAL
    @param:Json(name = "evolucao") val evolucao: String? = null,
    @param:Json(name = "pontuacao") val pontuacao: Int? = null
)

// 7. Incidente e Ocorrência de Campo (tabela: ciadi_at_incidentes)
@JsonClass(generateAdapter = true)
data class AtIncidenteDto(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "sessao_id") val sessaoId: String,
    @param:Json(name = "gravidade") val gravidade: String? = "LEVE", // LEVE, MODERADO, SEVERO
    @param:Json(name = "descricao") val descricao: String,
    @param:Json(name = "conduta_adotada") val condutaAdotada: String? = null,
    @param:Json(name = "criado_em") val criadoEm: String? = null
)

// 8. Supervisão de A.T. (tabela: ciadi_at_supervisoes)
@JsonClass(generateAdapter = true)
data class AtSupervisaoDto(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "profissional_at_id") val profissionalAtId: String,
    @param:Json(name = "supervisor_id") val supervisorId: String,
    @param:Json(name = "data_supervisao") val dataSupervisao: String,
    @param:Json(name = "orientacoes") val orientacoes: String? = null,
    @param:Json(name = "proximas_metas") val proximasMetas: String? = null
)

// 9. Profissional (tabela: profissionais)
@JsonClass(generateAdapter = true)
data class ProfissionalDto(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "perfil_id") val perfilId: String? = null,
    @param:Json(name = "nome_completo") val nomeCompleto: String,
    @param:Json(name = "titulo") val titulo: String? = null,
    @param:Json(name = "biografia") val biografia: String? = null,
    @param:Json(name = "registro_profissional") val registroProfissional: String? = null, // CRP, CRM, CREFITO
    @param:Json(name = "especialidade_id") val especialidadeId: String? = null,
    @param:Json(name = "foto_url") val fotoUrl: String? = null,
    @param:Json(name = "email_profissional") val emailProfissional: String? = null,
    @param:Json(name = "telefone_profissional") val telefoneProfissional: String? = null,
    @param:Json(name = "experiencia_anos") val experienciaAnos: Int? = null,
    @param:Json(name = "atendimento_presencial") val atendimentoPresencial: Boolean = true,
    @param:Json(name = "atendimento_online") val atendimentoOnline: Boolean = true,
    @param:Json(name = "ativo") val ativo: Boolean = true,
    @param:Json(name = "eh_at") val ehAt: Boolean = false
)

// 10. Agendamento (tabela: agendamentos)
@JsonClass(generateAdapter = true)
data class AgendamentoDto(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "paciente_id") val pacienteId: String? = null,
    @param:Json(name = "profissional_id") val profissionalId: String? = null,
    @param:Json(name = "especialidade_id") val especialidadeId: String? = null,
    @param:Json(name = "data_hora") val dataHora: String,
    @param:Json(name = "duracao_minutos") val duracaoMinutos: Int? = 50,
    @param:Json(name = "tipo") val tipo: String? = "CONSULTA", // CONSULTA, SESSAO_AT, TERAPIA, AVALIACAO
    @param:Json(name = "estado") val estado: String? = "CONFIRMADO", // AGENDADO, CONFIRMADO, EM_ATENDIMENTO, FINALIZADO, CANCELADO
    @param:Json(name = "motivo") val motivo: String? = null,
    @param:Json(name = "observacoes") val observacoes: String? = null,
    @param:Json(name = "link_video") val linkVideo: String? = null,
    @param:Json(name = "modalidade") val modalidade: String? = "PRESENCIAL", // PRESENCIAL, ONLINE
    @param:Json(name = "sala_nome") val salaNome: String? = null,
    @param:Json(name = "recurso_nome") val recursoNome: String? = null,
    @param:Json(name = "origem") val origem: String? = "APP_CIADI",
    @param:Json(name = "triagem_id") val triagemId: String? = null,
    @param:Json(name = "criado_por") val criadoPor: String? = null,
    @param:Json(name = "atualizado_por") val atualizadoPor: String? = null,
    @param:Json(name = "paciente_nome") val pacienteNome: String? = null,
    @param:Json(name = "profissional_nome") val profissionalNome: String? = null,
    @param:Json(name = "especialidade_nome") val especialidadeNome: String? = null
)

// 11. Especialidade (tabela: especialidades)
@JsonClass(generateAdapter = true)
data class EspecialidadeDto(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "nome") val nome: String,
    @param:Json(name = "descricao") val descricao: String? = null,
    @param:Json(name = "ativo") val ativo: Boolean = true
)

// 12. Formulário Clínico (tabela: ciadi_formularios_clinicos)
@JsonClass(generateAdapter = true)
data class FormularioClinicoDto(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "codigo") val codigo: String? = null,
    @param:Json(name = "nome") val nome: String,
    @param:Json(name = "area_atuacao") val areaAtuacao: String? = null,
    @param:Json(name = "tipo_formulario") val tipoFormulario: String? = null,
    @param:Json(name = "descricao") val descricao: String? = null,
    @param:Json(name = "instrucoes") val instrucoes: String? = null,
    @param:Json(name = "versao") val versao: String? = "1.0",
    @param:Json(name = "schema_json") val schemaJson: String? = null,
    @param:Json(name = "regras_json") val regrasJson: String? = null,
    @param:Json(name = "modelo_documento_id") val modeloDocumentoId: String? = null,
    @param:Json(name = "ativo") val ativo: Boolean = true,
    @param:Json(name = "publicado") val publicado: Boolean = true,
    @param:Json(name = "atualizado_em") val atualizadoEm: String? = null
)

// 13. Documento Clínico / Laudo / P.E.I. (tabela: documentos_clinicos)
@JsonClass(generateAdapter = true)
data class DocumentoClinicoDto(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "paciente_id") val pacienteId: String? = null,
    @param:Json(name = "profissional_id") val profissionalId: String? = null,
    @param:Json(name = "paciente_nome") val pacienteNome: String? = null,
    @param:Json(name = "profissional_nome") val profissionalNome: String? = null,
    @param:Json(name = "especialidade_nome") val especialidadeNome: String? = null,
    @param:Json(name = "registro_profissional") val registroProfissional: String? = null,
    @param:Json(name = "titulo") val titulo: String,
    @param:Json(name = "tipo_documento") val tipoDocumento: String? = "RELATORIO", // PEI, LAUDO, RELATORIO, EVOLUCAO
    @param:Json(name = "conteudo_texto") val conteudoTexto: String? = null,
    @param:Json(name = "estado") val estado: String? = "PUBLICADO", // Rascunho, Revisão, Finalizado, Assinado, Publicado, Revogado
    @param:Json(name = "versao") val versao: String? = "1.0",
    @param:Json(name = "pdf_url") val pdfUrl: String? = null,
    @param:Json(name = "codigo_validacao") val codigoValidacao: String? = null,
    @param:Json(name = "criado_em") val criadoEm: String? = null
)

// 14. Histórico de Documento (tabela: ciadi_documentos_clinicos_historico)
@JsonClass(generateAdapter = true)
data class DocumentoHistoricoDto(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "documento_id") val documentoId: String,
    @param:Json(name = "autor_id") val autorId: String? = null,
    @param:Json(name = "autor_nome") val autorNome: String? = null,
    @param:Json(name = "estado_anterior") val estadoAnterior: String? = null,
    @param:Json(name = "estado_novo") val estadoNovo: String? = null,
    @param:Json(name = "alteracao_resumo") val alteracaoResumo: String? = null,
    @param:Json(name = "data_registro") val dataRegistro: String? = null
)

// 15. Chat: Grupo (tabela: ciadi_chat_grupos)
@JsonClass(generateAdapter = true)
data class ChatGrupoDto(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "nome") val nome: String,
    @param:Json(name = "tipo") val tipo: String = "familia_equipa", // equipa, familia_equipa, acompanhamento, individual
    @param:Json(name = "paciente_id") val pacienteId: String? = null,
    @param:Json(name = "paciente_nome") val pacienteNome: String? = null,
    @param:Json(name = "criado_em") val criadoEm: String? = null
)

// 16. Chat: Membro (tabela: ciadi_chat_membros)
@JsonClass(generateAdapter = true)
data class ChatMembroDto(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "grupo_id") val grupoId: String,
    @param:Json(name = "perfil_id") val perfilId: String,
    @param:Json(name = "papel_no_grupo") val papelNoGrupo: String? = "membro"
)

// 17. Chat: Mensagem (tabela: ciadi_chat_mensagens)
@JsonClass(generateAdapter = true)
data class ChatMensagemDto(
    @param:Json(name = "id") val id: String = "",
    @param:Json(name = "grupo_id") val grupoId: String,
    @param:Json(name = "remetente_id") val remetenteId: String? = null,
    @param:Json(name = "mensagem") val mensagem: String = "",
    @param:Json(name = "anexo_url") val anexoUrl: String? = null,
    @param:Json(name = "anexo_nome") val anexoNome: String? = null,
    @param:Json(name = "lida_em") val lidaEm: String? = null,
    @param:Json(name = "created_at") val createdAt: String? = null
) {
    val text: String
        get() = mensagem
    val senderId: String
        get() = remetenteId.orEmpty()
    val timestamp: String
        get() {
            val dt = createdAt ?: return "Agora"
            return try {
                if (dt.contains("T")) {
                    val timePart = dt.substringAfter("T").substringBefore("+").substringBefore("Z")
                    timePart.take(5)
                } else {
                    dt
                }
            } catch (_: Exception) {
                dt
            }
        }
    val autorNome: String?
        get() = null
}

// DTO estrito para INSERT na tabela ciadi_chat_mensagens (Item 4 do Prompt Mestre)
@JsonClass(generateAdapter = true)
data class ChatMensagemInsertDto(
    @param:Json(name = "grupo_id") val grupoId: String,
    @param:Json(name = "remetente_id") val remetenteId: String,
    @param:Json(name = "mensagem") val mensagem: String,
    @param:Json(name = "anexo_url") val anexoUrl: String? = null,
    @param:Json(name = "anexo_nome") val anexoNome: String? = null
)

// 18. Notificação (tabela: notificacoes)
@JsonClass(generateAdapter = true)
data class NotificacaoDto(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "perfil_id") val perfilId: String? = null,
    @param:Json(name = "titulo") val titulo: String,
    @param:Json(name = "mensagem") val mensagem: String,
    @param:Json(name = "tipo") val tipo: String? = "INFO", // SESSAO, DOCUMENTO, MENSAGEM, AVISO
    @param:Json(name = "lida") val lida: Boolean = false,
    @param:Json(name = "link_acao") val linkAcao: String? = null,
    @param:Json(name = "criado_em") val criadoEm: String? = null
)

// 19. SOS CIADI+ (RPC: ciadi_criar_alerta_sos)
@JsonClass(generateAdapter = true)
data class CriarAlertaSosRequest(
    @param:Json(name = "p_paciente_id") val pacienteId: String,
    @param:Json(name = "p_mensagem") val mensagem: String,
    @param:Json(name = "p_latitude") val latitude: Double? = null,
    @param:Json(name = "p_longitude") val longitude: Double? = null,
    @param:Json(name = "p_localizacao_autorizada") val localizacaoAutorizada: Boolean = false
)

@JsonClass(generateAdapter = true)
data class SosAlertaResultDto(
    @param:Json(name = "id") val id: String? = null,
    @param:Json(name = "sucesso") val sucesso: Boolean = true,
    @param:Json(name = "mensagem") val mensagem: String? = null,
    @param:Json(name = "protocolo") val protocolo: String? = null,
    @param:Json(name = "criado_em") val criadoEm: String? = null
)

// 20. Portal Agendamento Autorizado (RPC: ciadi_portal_agendamentos_autorizados)
@JsonClass(generateAdapter = true)
data class PortalAgendamentoDto(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "paciente") val paciente: String? = null,
    @param:Json(name = "paciente_id") val pacienteId: String? = null,
    @param:Json(name = "profissional") val profissional: String? = null,
    @param:Json(name = "profissional_id") val profissionalId: String? = null,
    @param:Json(name = "titulo_profissional") val tituloProfissional: String? = null,
    @param:Json(name = "registro_profissional") val registroProfissional: String? = null,
    @param:Json(name = "especialidade") val especialidade: String? = null,
    @param:Json(name = "data_hora") val dataHora: String,
    @param:Json(name = "duracao") val duracao: Int? = 50,
    @param:Json(name = "duracao_minutos") val duracaoMinutos: Int? = 50,
    @param:Json(name = "tipo") val tipo: String? = "Consulta",
    @param:Json(name = "modalidade") val modalidade: String? = "Presencial",
    @param:Json(name = "estado") val estado: String? = "agendada",
    @param:Json(name = "motivo") val motivo: String? = null,
    @param:Json(name = "observacoes") val observacoes: String? = null,
    @param:Json(name = "link_video") val linkVideo: String? = null,
    @param:Json(name = "sala_virtual_id") val salaVirtualId: String? = null,
    @param:Json(name = "sala_nome") val salaNome: String? = null
)

// 21. Clínica Virtual (RPCs de Vídeo)
@JsonClass(generateAdapter = true)
data class VideoSalaRequest(
    @param:Json(name = "p_agendamento_id") val agendamentoId: String
)

@JsonClass(generateAdapter = true)
data class VideoSalaResponseDto(
    @param:Json(name = "sala_id") val salaId: String? = null,
    @param:Json(name = "sucesso") val sucesso: Boolean = true,
    @param:Json(name = "token") val token: String? = null,
    @param:Json(name = "url") val url: String? = null,
    @param:Json(name = "mensagem") val mensagem: String? = null,
    @param:Json(name = "estado") val estado: String? = null
)

// 22. Efemérides Oficiais (tabela: ciadi_efemerides)
@JsonClass(generateAdapter = true)
data class EfemerideDto(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "titulo") val titulo: String,
    @param:Json(name = "descricao") val descricao: String? = null,
    @param:Json(name = "data_evento") val dataEvento: String,
    @param:Json(name = "categoria") val categoria: String? = "saude", // feriado, saude, psicologia, neurodesenvolvimento, familia, infancia, ciadi, nacional, internacional
    @param:Json(name = "publico") val publico: Boolean = true,
    @param:Json(name = "ativo") val ativo: Boolean = true,
    @param:Json(name = "notificar") val notificar: Boolean = false
)

// 23. Aniversários & Lembretes (tabela: ciadi_aniversarios)
@JsonClass(generateAdapter = true)
data class AniversarioDto(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "nome") val nome: String,
    @param:Json(name = "data_aniversario") val dataAniversario: String,
    @param:Json(name = "tipo") val tipo: String? = "paciente", // paciente, profissional, equipe
    @param:Json(name = "mostrar_no_calendario") val mostrarNoCalendario: Boolean = true,
    @param:Json(name = "receber_lembretes") val receberLembretes: Boolean = true,
    @param:Json(name = "permitir_mensagens") val permitirMensagens: Boolean = true
)

// 24. Sistema de Popups & Comunicados (tabelas: ciadi_popups, ciadi_anuncios)
@JsonClass(generateAdapter = true)
data class PopupComunicadoDto(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "titulo") val titulo: String,
    @param:Json(name = "mensagem") val mensagem: String,
    @param:Json(name = "categoria") val categoria: String = "comunicado", // nova_mensagem, consulta, documento, aniversario, efemeride, comunicado, anuncio, seguranca, janeth, sos, atualizacao
    @param:Json(name = "prioridade") val prioridade: String = "NORMAL", // NORMAL, ALTA, URGENTE
    @param:Json(name = "ativo") val ativo: Boolean = true,
    @param:Json(name = "publico") val publico: Boolean = true,
    @param:Json(name = "perfil_autorizado") val perfilAutorizado: String? = null,
    @param:Json(name = "data_inicio") val dataInicio: String? = null,
    @param:Json(name = "data_fim") val dataFim: String? = null
)

// 25. Coleção de 11 Modelos de Documentos Oficiais (tabela: ciadi_modelos_documentos)
@JsonClass(generateAdapter = true)
data class ModeloDocumentoDto(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "codigo") val codigo: String,
    @param:Json(name = "nome") val nome: String,
    @param:Json(name = "categoria") val categoria: String,
    @param:Json(name = "versao") val versao: String = "1.0",
    @param:Json(name = "estado") val estado: String = "ATIVO",
    @param:Json(name = "descricao") val descricao: String? = null,
    @param:Json(name = "requer_assinatura") val requerAssinatura: Boolean = true
)

// 26. Registro de Auditoria do Ecossistema (tabela: ciadi_auditoria)
@JsonClass(generateAdapter = true)
data class AuditoriaRegistroDto(
    @param:Json(name = "id") val id: String? = null,
    @param:Json(name = "user_id") val userId: String,
    @param:Json(name = "acao") val acao: String,
    @param:Json(name = "detalhes") val detalhes: String? = null,
    @param:Json(name = "criado_em") val criadoEm: String? = null
)

// 27. Atividade A.T. (tabela: ciadi_at_atividades)
@JsonClass(generateAdapter = true)
data class AtAtividadeDto(
    @param:Json(name = "id") val id: String? = null,
    @param:Json(name = "atribuicao_id") val atribuicaoId: String? = null,
    @param:Json(name = "paciente_id") val pacienteId: String? = null,
    @param:Json(name = "profissional_at_id") val profissionalAtId: String? = null,
    @param:Json(name = "data_atividade") val dataAtividade: String,
    @param:Json(name = "hora_inicio") val horaInicio: String? = null,
    @param:Json(name = "hora_fim") val horaFim: String? = null,
    @param:Json(name = "tipo") val tipo: String? = null,
    @param:Json(name = "titulo") val titulo: String,
    @param:Json(name = "descricao") val descricao: String? = null,
    @param:Json(name = "objetivo") val objetivo: String? = null,
    @param:Json(name = "resultado") val resultado: String? = null,
    @param:Json(name = "estado") val estado: String = "Planeada", // Planeada, Realizada, Cancelada
    @param:Json(name = "criado_por") val criadoPor: String? = null,
    @param:Json(name = "criado_em") val criadoEm: String? = null,
    @param:Json(name = "paciente_nome") val pacienteNome: String? = null
)

// 28. Evento do Calendário A.T. (view: v_ciadi_at_calendario)
@JsonClass(generateAdapter = true)
data class AtCalendarioEventoDto(
    @param:Json(name = "id") val id: String = "",
    @param:Json(name = "titulo") val titulo: String? = null,
    @param:Json(name = "assistido") val assistido: String? = null,
    @param:Json(name = "paciente_id") val pacienteId: String? = null,
    @param:Json(name = "data") val data: String? = null,
    @param:Json(name = "hora") val hora: String? = null,
    @param:Json(name = "data_hora") val dataHora: String? = null,
    @param:Json(name = "estado") val estado: String? = null,
    @param:Json(name = "modalidade") val modalidade: String? = null,
    @param:Json(name = "tipo_evento") val tipoEvento: String? = null,
    @param:Json(name = "descricao") val descricao: String? = null
)

// 29. Contacto Autorizado A.T. (view: v_ciadi_at_contactos)
@JsonClass(generateAdapter = true)
data class AtContactoDto(
    @param:Json(name = "id") val id: String? = null,
    @param:Json(name = "perfil_id") val perfilId: String? = null,
    @param:Json(name = "destinatario_id") val destinatarioId: String? = null,
    @param:Json(name = "nome") val nome: String? = null,
    @param:Json(name = "funcao") val funcao: String? = null,
    @param:Json(name = "categoria") val categoria: String? = null, // Família, Profissional, A.T., Supervisor, Coordenação/Administração
    @param:Json(name = "assistido_relacionado") val assistidoRelacionado: String? = null,
    @param:Json(name = "paciente_id") val pacienteId: String? = null,
    @param:Json(name = "avatar_url") val avatarUrl: String? = null,
    @param:Json(name = "contacto") val contacto: String? = null,
    @param:Json(name = "telefone") val telefone: String? = null,
    @param:Json(name = "email") val email: String? = null
) {
    val contactId: String
        get() = destinatarioId?.takeIf { it.isNotBlank() } ?: perfilId ?: id ?: ""
    val contactName: String
        get() = nome?.takeIf { it.isNotBlank() } ?: "Contacto"
    val contactRole: String
        get() = funcao?.takeIf { it.isNotBlank() } ?: categoria ?: "Equipe"
}

// 30. Organização A.T. (view: v_ciadi_at_organizacao)
@JsonClass(generateAdapter = true)
data class AtOrganizacaoDto(
    @param:Json(name = "id") val id: String? = null,
    @param:Json(name = "nome") val nome: String? = null,
    @param:Json(name = "funcao") val funcao: String? = null, // Supervisor, Outros A.T., Profissionais, Família autorizada
    @param:Json(name = "categoria") val categoria: String? = null,
    @param:Json(name = "assistido_relacionado") val assistidoRelacionado: String? = null,
    @param:Json(name = "telefone") val telefone: String? = null,
    @param:Json(name = "email") val email: String? = null
)

// 31. Acompanhamento Diário ABA (tabela: ciadi_acompanhamentos_diarios_aba)
@JsonClass(generateAdapter = true)
data class AcompanhamentoDiarioAbaDto(
    @param:Json(name = "id") val id: String? = null,
    @param:Json(name = "paciente_id") val pacienteId: String? = null,
    @param:Json(name = "profissional_id") val profissionalId: String? = null,
    @param:Json(name = "data_registro") val dataRegistro: String? = null,
    @param:Json(name = "comportamento_observado") val comportamentoObservado: String? = null,
    @param:Json(name = "progresso") val progresso: String? = null,
    @param:Json(name = "comunicacao_familia") val comunicacaoFamilia: String? = null,
    @param:Json(name = "observacoes") val observacoes: String? = null,
    @param:Json(name = "estado") val estado: String? = "Concluído"
)


