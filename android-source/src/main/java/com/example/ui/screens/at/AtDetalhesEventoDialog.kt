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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.dto.AtCalendarioEventoDto
import com.example.ui.theme.CIADIColors

@Composable
fun AtDetalhesEventoDialog(
    evento: AtCalendarioEventoDto,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
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
                            .background(CIADIColors.OrangePrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = evento.tipoEvento ?: "Evento do Calendário",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = CIADIColors.Brown
                        )
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Título
                Text(
                    text = evento.titulo ?: "Atividade / Agendamento",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CIADIColors.Brown, fontSize = 16.sp)
                )

                // Assistido
                if (!evento.assistido.isNullOrBlank()) {
                    Row {
                        Text("Assistido: ", fontWeight = FontWeight.Bold, color = CIADIColors.TextSecondary)
                        Text(evento.assistido, color = CIADIColors.TextPrimary)
                    }
                }

                // Data e Hora
                Row {
                    Text("Data: ", fontWeight = FontWeight.Bold, color = CIADIColors.TextSecondary)
                    Text(evento.data ?: evento.dataHora ?: "Não informada", color = CIADIColors.TextPrimary)
                }

                if (!evento.hora.isNullOrBlank()) {
                    Row {
                        Text("Horário: ", fontWeight = FontWeight.Bold, color = CIADIColors.TextSecondary)
                        Text(evento.hora, color = CIADIColors.TextPrimary)
                    }
                }

                // Estado
                if (!evento.estado.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Estado: ", fontWeight = FontWeight.Bold, color = CIADIColors.TextSecondary)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    when (evento.estado.lowercase()) {
                                        "realizada", "concluida", "confirmado" -> Color(0xFFE8F5E9)
                                        "cancelada", "cancelado" -> Color(0xFFFFEBEE)
                                        else -> Color(0xFFFFF3E0)
                                    }
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = evento.estado,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (evento.estado.lowercase()) {
                                    "realizada", "concluida", "confirmado" -> Color(0xFF2E7D32)
                                    "cancelada", "cancelado" -> Color(0xFFC62828)
                                    else -> Color(0xFFE65100)
                                }
                            )
                        }
                    }
                }

                // Modalidade
                if (!evento.modalidade.isNullOrBlank()) {
                    Row {
                        Text("Modalidade: ", fontWeight = FontWeight.Bold, color = CIADIColors.TextSecondary)
                        Text(evento.modalidade, color = CIADIColors.TextPrimary)
                    }
                }

                // Descrição
                if (!evento.descricao.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Detalhes:", fontWeight = FontWeight.Bold, color = CIADIColors.TextSecondary)
                    Text(evento.descricao, color = CIADIColors.TextPrimary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CIADIColors.OrangePrimary)
            ) {
                Text("Fechar")
            }
        }
    )
}
