package com.example.ui.screens.at

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.dto.AtSessaoDto
import com.example.data.remote.dto.AtribuicaoAtDto
import com.example.data.remote.dto.PacienteDto
import com.example.domain.model.UserProfile
import com.example.ui.theme.CIADIColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Diálogo de Registro de Novo Acompanhamento (Item 4 do Prompt Mestre).
 *
 * Salva na tabela 'ciadi_at_sessoes' com relação ao profissional A.T. e/ou atribuição.
 * Campos: data, horário, assistido, objetivo, atividades, comportamento observado,
 * progresso, comunicação com família, observações, estado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtNovoAcompanhamentoDialog(
    user: UserProfile,
    assistidos: List<PacienteDto>,
    atribuicoes: List<AtribuicaoAtDto>,
    profissionalId: String?,
    onDismiss: () -> Unit,
    onSalvar: suspend (AtSessaoDto) -> Result<AtSessaoDto>,
    onSalvaComSucesso: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val hojeFormatado = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    var selectedPaciente by remember {
        mutableStateOf(assistidos.firstOrNull())
    }
    var pacienteDropdownExpanded by remember { mutableStateOf(false) }

    var dataSessao by remember { mutableStateOf(hojeFormatado) }
    var horaInicio by remember { mutableStateOf("08:30") }
    var horaFim by remember { mutableStateOf("11:30") }
    var objetivo by remember { mutableStateOf("") }
    var atividades by remember { mutableStateOf("") }
    var comportamentoObservado by remember { mutableStateOf("") }
    var progresso by remember { mutableStateOf("") }
    var comunicacaoFamilia by remember { mutableStateOf("") }
    var observacoes by remember { mutableStateOf("") }

    var estado by remember { mutableStateOf("Realizada") }
    var estadoDropdownExpanded by remember { mutableStateOf(false) }
    val estadosOpcoes = listOf("Realizada", "Em Andamento", "Cancelada")

    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CIADIColors.TealPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AssignmentTurnedIn,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "+ Novo acompanhamento",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = CIADIColors.Brown
                        )
                    )
                }
                IconButton(onClick = onDismiss, enabled = !isSaving) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFFEBEE))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFC62828))
                        )
                    }
                }

                // Assistido
                ExposedDropdownMenuBox(
                    expanded = pacienteDropdownExpanded,
                    onExpandedChange = { pacienteDropdownExpanded = !pacienteDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedPaciente?.displayName ?: "Selecione o assistido",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Assistido *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = pacienteDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor().testTag("dropdown_assistido_acompanhamento")
                    )
                    ExposedDropdownMenu(
                        expanded = pacienteDropdownExpanded,
                        onDismissRequest = { pacienteDropdownExpanded = false }
                    ) {
                        assistidos.forEach { pac ->
                            DropdownMenuItem(
                                text = { Text(pac.displayName) },
                                onClick = {
                                    selectedPaciente = pac
                                    pacienteDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Data e Horário
                OutlinedTextField(
                    value = dataSessao,
                    onValueChange = { dataSessao = it },
                    label = { Text("Data da Sessão (AAAA-MM-DD) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_data_acompanhamento")
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = horaInicio,
                        onValueChange = { horaInicio = it },
                        label = { Text("Hora Início *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = horaFim,
                        onValueChange = { horaFim = it },
                        label = { Text("Hora Fim *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Objetivo
                OutlinedTextField(
                    value = objetivo,
                    onValueChange = { objetivo = it },
                    label = { Text("Objetivo da Sessão *") },
                    placeholder = { Text("Meta do P.E.I. ou rotina trabalhada") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("input_objetivo_acompanhamento")
                )

                // Atividades
                OutlinedTextField(
                    value = atividades,
                    onValueChange = { atividades = it },
                    label = { Text("Atividades Realizadas *") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("input_atividades_acompanhamento")
                )

                // Comportamento Observado
                OutlinedTextField(
                    value = comportamentoObservado,
                    onValueChange = { comportamentoObservado = it },
                    label = { Text("Comportamento Observado") },
                    placeholder = { Text("Engajamento, regulação sensorial, transições...") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                // Progresso
                OutlinedTextField(
                    value = progresso,
                    onValueChange = { progresso = it },
                    label = { Text("Progresso / Evolução") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                // Comunicação com Família
                OutlinedTextField(
                    value = comunicacaoFamilia,
                    onValueChange = { comunicacaoFamilia = it },
                    label = { Text("Comunicação com a Família") },
                    placeholder = { Text("Recados transmitidos aos responsáveis na entrega/saída") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                // Observações
                OutlinedTextField(
                    value = observacoes,
                    onValueChange = { observacoes = it },
                    label = { Text("Observações Técnicas") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                // Estado
                ExposedDropdownMenuBox(
                    expanded = estadoDropdownExpanded,
                    onExpandedChange = { estadoDropdownExpanded = !estadoDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = estado,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Estado da Sessão") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = estadoDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = estadoDropdownExpanded,
                        onDismissRequest = { estadoDropdownExpanded = false }
                    ) {
                        estadosOpcoes.forEach { est ->
                            DropdownMenuItem(
                                text = { Text(est) },
                                onClick = {
                                    estado = est
                                    estadoDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val pac = selectedPaciente
                    if (pac == null) {
                        errorMessage = "Por favor, selecione o assistido."
                        return@Button
                    }
                    if (dataSessao.isBlank()) {
                        errorMessage = "Por favor, informe a data."
                        return@Button
                    }
                    if (objetivo.isBlank() && atividades.isBlank()) {
                        errorMessage = "Por favor, informe o objetivo ou as atividades da sessão."
                        return@Button
                    }

                    val atribuicao = atribuicoes.firstOrNull { it.pacienteId == pac.id }
                    val novaSessao = AtSessaoDto(
                        id = "",
                        atribuicaoId = atribuicao?.id,
                        profissionalAtId = profissionalId,
                        pacienteId = pac.id,
                        pacienteNome = pac.displayName,
                        dataSessao = dataSessao.trim(),
                        horaInicio = horaInicio.trim().ifBlank { null },
                        horaFim = horaFim.trim().ifBlank { null },
                        resumoObservacoes = observacoes.trim().ifBlank { null },
                        status = estado.uppercase(),
                        objetivo = objetivo.trim().ifBlank { null },
                        atividades = atividades.trim().ifBlank { null },
                        comportamentoObservado = comportamentoObservado.trim().ifBlank { null },
                        progresso = progresso.trim().ifBlank { null },
                        comunicacaoFamilia = comunicacaoFamilia.trim().ifBlank { null },
                        observacoes = observacoes.trim().ifBlank { null },
                        estado = estado,
                        criadoEm = null
                    )

                    isSaving = true
                    errorMessage = null
                    scope.launch {
                        val result = onSalvar(novaSessao)
                        isSaving = false
                        if (result.isSuccess) {
                            onSalvaComSucesso()
                            onDismiss()
                        } else {
                            errorMessage = result.exceptionOrNull()?.message
                                ?: "Não foi possível registrar o acompanhamento. Tente novamente."
                        }
                    }
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = CIADIColors.TealPrimary),
                modifier = Modifier.testTag("btn_salvar_acompanhamento")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("A gravar...")
                } else {
                    Text("Gravar Acompanhamento", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancelar")
            }
        }
    )
}
