package com.example.ui.screens.forms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.dto.FormularioClinicoDto
import com.example.ui.theme.CIADIColors
import org.json.JSONArray
import org.json.JSONObject

/**
 * Representa os dados submetidos de um formulário clínico preenchido.
 */
data class ClinicalFormSubmission(
    val formularioId: String,
    val pacienteId: String,
    val profissionalId: String?,
    val respostasJson: String,
    val dataSubmissao: String = System.currentTimeMillis().toString()
)

/**
 * Renderizador dinâmico de formulários clínicos do CIADI a partir de schema_json.
 */
@Composable
fun ClinicalFormRenderer(
    formulario: FormularioClinicoDto,
    pacienteId: String,
    pacienteNome: String,
    profissionalId: String? = null,
    onSubmit: (ClinicalFormSubmission) -> Unit,
    modifier: Modifier = Modifier
) {
    val respostas = remember { mutableStateMapOf<String, String>() }
    var submitted by remember { mutableStateOf(false) }
    var validationMessage by remember { mutableStateOf<String?>(null) }

    val parsedFields = remember(formulario.schemaJson) {
        parseSchemaFields(formulario.schemaJson)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = formulario.nome,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = CIADIColors.Brown
                )
            )

            formulario.areaAtuacao?.let { area ->
                Text(
                    text = "Especialidade: $area (v${formulario.versao ?: "1.0"})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = CIADIColors.OrangePrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            formulario.descricao?.let { desc ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = CIADIColors.TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Assistido: $pacienteNome",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = CIADIColors.TealPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            parsedFields.forEach { field ->
                FormFieldItem(
                    field = field,
                    currentValue = respostas[field.key].orEmpty(),
                    onValueChange = { novoValor ->
                        respostas[field.key] = novoValor
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val missingRequired = parsedFields
                        .filter { it.required && respostas[it.key].orEmpty().isBlank() }
                        .map { it.label }
                    if (missingRequired.isNotEmpty()) {
                        validationMessage = "Preencha: ${missingRequired.joinToString(", ")}"
                        return@Button
                    }
                    if (pacienteId.isBlank()) {
                        validationMessage = "Selecione o assistido antes de submeter."
                        return@Button
                    }
                    val jsonOutput = JSONObject()
                    respostas.forEach { (k, v) -> jsonOutput.put(k, v) }
                    val submission = ClinicalFormSubmission(
                        formularioId = formulario.id,
                        pacienteId = pacienteId,
                        profissionalId = profissionalId,
                        respostasJson = jsonOutput.toString()
                    )
                    validationMessage = null
                    submitted = true
                    onSubmit(submission)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CIADIColors.TealPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Submeter Formulário Clínico", fontWeight = FontWeight.Bold)
            }
            validationMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = Color(0xFFB3261E), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

data class DynamicFormField(
    val key: String,
    val label: String,
    val type: String, // "text", "textarea", "select", "radio", "checkbox", "number"
    val options: List<String> = emptyList(),
    val required: Boolean = false
)

@Composable
private fun FormFieldItem(
    field: DynamicFormField,
    currentValue: String,
    onValueChange: (String) -> Unit
) {
    Column {
        Text(
            text = field.label + if (field.required) " *" else "",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = CIADIColors.Brown
        )
        Spacer(modifier = Modifier.height(4.dp))

        when (field.type) {
            "radio" -> {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    field.options.forEach { option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = currentValue == option,
                                onClick = { onValueChange(option) },
                                colors = RadioButtonDefaults.colors(selectedColor = CIADIColors.OrangePrimary)
                            )
                            Text(text = option, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            "checkbox" -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = currentValue == "true",
                        onCheckedChange = { onValueChange(if (it) "true" else "false") },
                        colors = CheckboxDefaults.colors(checkedColor = CIADIColors.OrangePrimary)
                    )
                    Text(text = "Sim / Confirmado", style = MaterialTheme.typography.bodySmall)
                }
            }
            "textarea" -> {
                OutlinedTextField(
                    value = currentValue,
                    onValueChange = onValueChange,
                    minLines = 3,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            else -> {
                OutlinedTextField(
                    value = currentValue,
                    onValueChange = onValueChange,
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun parseSchemaFields(schemaJson: String?): List<DynamicFormField> {
    if (schemaJson.isNullOrBlank()) {
        // Fallback para formulário padrão de evolução/anamnese clínica CIADI
        return listOf(
            DynamicFormField("observacoes_gerais", "Observações Clínicas Gerais", "textarea", required = true),
            DynamicFormField("nivel_engajamento", "Nível de Engajamento e Atenção Compartilhada", "radio", options = listOf("Excelente", "Bom", "Oscilante", "Resistente")),
            DynamicFormField("intervencao_realizada", "Intervenções e Estratégias Utilizadas", "textarea"),
            DynamicFormField("meta_atingida", "Objetivo da Sessão Atingido", "checkbox")
        )
    }

    return try {
        val root = JSONObject(schemaJson)
        val fieldsArray = root.optJSONArray("fields") ?: JSONArray()
        val list = mutableListOf<DynamicFormField>()
        for (i in 0 until fieldsArray.length()) {
            val obj = fieldsArray.getJSONObject(i)
            val key = obj.optString("key", "campo_$i")
            val label = obj.optString("label", key)
            val type = obj.optString("type", "text")
            val required = obj.optBoolean("required", false)
            val optionsJson = obj.optJSONArray("options")
            val options = mutableListOf<String>()
            if (optionsJson != null) {
                for (j in 0 until optionsJson.length()) {
                    options.add(optionsJson.getString(j))
                }
            }
            list.add(DynamicFormField(key, label, type, options, required))
        }
        if (list.isEmpty()) {
            listOf(
                DynamicFormField("evolucao", "Registro de Evolução", "textarea", required = true),
                DynamicFormField("conduta", "Conduta Terapêutica", "textarea")
            )
        } else list
    } catch (e: Exception) {
        listOf(
            DynamicFormField("resumo_clinico", "Resumo do Atendimento", "textarea", required = true)
        )
    }
}
