package com.example.ui.screens.at

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventNote
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
import com.example.data.remote.dto.AtAtividadeDto
import com.example.data.remote.dto.AtribuicaoAtDto
import com.example.data.remote.dto.PacienteDto
import com.example.domain.model.UserProfile
import com.example.ui.theme.CIADIColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Diálogo e Formulário Oficial de Criação de Atividade A.T. (Item 5 do Prompt Mestre).
 *
 * Salva na tabela 'ciadi_at_atividades' do Supabase com campos:
 * - atribuicao_id
 * - paciente_id
 * - profissional_at_id
 * - data_atividade
 * - hora_inicio
 * - hora_fim
 * - tipo
 * - titulo
 * - descricao
 * - objetivo
 * - resultado
 * - estado ("Planeada", "Realizada", "Cancelada")
 * - criado_por (auth.uid())
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtNovaAtividadeDialog(
    user: UserProfile,
    assistidos: List<PacienteDto>,
    atribuicoes: List<AtribuicaoAtDto>,
    profissionalId: String?,
    onDismiss: () -> Unit,
    onSalvar: suspend (AtAtividadeDto) -> Result<AtAtividadeDto>,
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

    var dataAtividade by remember { mutableStateOf(hojeFormatado) }
    var horaInicio by remember { mutableStateOf("09:00") }
    var horaFim by remember { mutableStateOf("10:00") }
    var tipo by remember { mutableStateOf("Escolar") }
    var tipoDropdownExpanded by remember { mutableStateOf(false) }
    val tiposOpcoes = listOf("Escolar", "Domiciliar", "Clínica", "Social", "Lazer Adaptado", "Rotina")

    var titulo by remember { mutableStateOf("") }
    var descricao by remember { mutableStateOf("") }
    var objetivo by remember { mutableStateOf("") }
    var resultado by remember { mutableStateOf("") }

    var estado by remember { mutableStateOf("Planeada") }
    var estadoDropdownExpanded by remember { mutableStateOf(false) }
    val estadosOpcoes = listOf("Planeada", "Realizada", "Cancelada")

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
                            .background(CIADIColors.RoleAt),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventNote,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "+ Atividade",
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

                // 1. Assistido (Dropdown das atribuições reais do A.T.)
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("dropdown_assistido_atividade")
                    )
                    ExposedDropdownMenu(
                        expanded = pacienteDropdownExpanded,
                        onDismissRequest = { pacienteDropdownExpanded = false }
                    ) {
                        if (assistidos.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Nenhum assistido atribuído") },
                                onClick = { pacienteDropdownExpanded = false }
                            )
                        } else {
                            assistidos.forEach { pac ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(pac.displayName, fontWeight = FontWeight.SemiBold)
                                            if (!pac.codigoPaciente.isNullOrBlank()) {
                                                Text("Cód: ${pac.codigoPaciente}", fontSize = 11.sp, color = CIADIColors.TextSecondary)
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedPaciente = pac
                                        pacienteDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // 2. Data
                OutlinedTextField(
                    value = dataAtividade,
                    onValueChange = { dataAtividade = it },
                    label = { Text("Data da Atividade (AAAA-MM-DD) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_data_atividade")
                )

                // 3. Horários
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = horaInicio,
                        onValueChange = { horaInicio = it },
                        label = { Text("Hora Inicial *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_hora_inicio")
                    )
                    OutlinedTextField(
                        value = horaFim,
                        onValueChange = { horaFim = it },
                        label = { Text("Hora Final *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_hora_fim")
                    )
                }

                // 4. Tipo
                ExposedDropdownMenuBox(
                    expanded = tipoDropdownExpanded,
                    onExpandedChange = { tipoDropdownExpanded = !tipoDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = tipo,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo de Atividade") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tipoDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = tipoDropdownExpanded,
                        onDismissRequest = { tipoDropdownExpanded = false }
                    ) {
                        tiposOpcoes.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = {
                                    tipo = opt
                                    tipoDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // 5. Título
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título da Atividade *") },
                    placeholder = { Text("Ex: Treino de Comunicação e Pareamento") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_titulo_atividade")
                )

                // 6. Descrição
                OutlinedTextField(
                    value = descricao,
                    onValueChange = { descricao = it },
                    label = { Text("Descrição detalhada") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                // 7. Objetivo
                OutlinedTextField(
                    value = objetivo,
                    onValueChange = { objetivo = it },
                    label = { Text("Objetivo Terapêutico") },
                    placeholder = { Text("Meta do P.E.I. trabalhada nesta sessão") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                // 8. Resultado
                OutlinedTextField(
                    value = resultado,
                    onValueChange = { resultado = it },
                    label = { Text("Resultado / Desempenho Observado") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                // 9. Estado
                ExposedDropdownMenuBox(
                    expanded = estadoDropdownExpanded,
                    onExpandedChange = { estadoDropdownExpanded = !estadoDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = estado,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Estado da Atividade") },
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
                    if (titulo.isBlank()) {
                        errorMessage = "Por favor, informe o título da atividade."
                        return@Button
                    }
                    if (dataAtividade.isBlank()) {
                        errorMessage = "Por favor, informe a data da atividade."
                        return@Button
                    }
                    val pac = selectedPaciente
                    if (pac == null) {
                        errorMessage = "Por favor, selecione um assistido atribuído."
                        return@Button
                    }

                    // Encontra a atribuição correspondente
                    val atribuicao = atribuicoes.firstOrNull { it.pacienteId == pac.id }
                    val novaAtividade = AtAtividadeDto(
                        id = null,
                        atribuicaoId = atribuicao?.id,
                        pacienteId = pac.id,
                        profissionalAtId = profissionalId,
                        dataAtividade = dataAtividade.trim(),
                        horaInicio = horaInicio.trim().ifBlank { null },
                        horaFim = horaFim.trim().ifBlank { null },
                        tipo = tipo,
                        titulo = titulo.trim(),
                        descricao = descricao.trim().ifBlank { null },
                        objetivo = objetivo.trim().ifBlank { null },
                        resultado = resultado.trim().ifBlank { null },
                        estado = estado,
                        criadoPor = user.id.ifBlank { null },
                        pacienteNome = pac.displayName
                    )

                    isSaving = true
                    errorMessage = null
                    scope.launch {
                        val result = onSalvar(novaAtividade)
                        isSaving = false
                        if (result.isSuccess) {
                            onSalvaComSucesso()
                            onDismiss()
                        } else {
                            errorMessage = result.exceptionOrNull()?.message
                                ?: "Não foi possível registrar a atividade. Verifique a conexão e tente novamente."
                        }
                    }
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = CIADIColors.RoleAt),
                modifier = Modifier.testTag("btn_salvar_atividade")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("A gravar...")
                } else {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Gravar Atividade", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text("Cancelar")
            }
        }
    )
}
