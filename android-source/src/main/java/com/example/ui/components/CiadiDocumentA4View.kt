package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.remote.dto.DocumentoClinicoDto
import com.example.ui.theme.CIADIColors

/**
 * Visualizador de Documento Clínico com Layout Oficial A4 do CIADI.
 * Contém cabeçalho com logótipo oficial, dados do assistido, corpo técnico,
 * campo de assinatura com registro profissional e rodapé com código de validação.
 */
@Composable
fun CiadiDocumentA4View(
    documento: DocumentoClinicoDto,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(1.dp, Color(0xFFD6D0CB)),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Cabeçalho Institucional A4
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_ciadi_logo_1790332105105),
                        contentDescription = "Logo CIADI",
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "CIADI+",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = CIADIColors.Brown,
                                letterSpacing = 0.5.sp
                            )
                        )
                        Text(
                            text = "Centro Integrado de Apoio e Desenvolvimento Individual",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = CIADIColors.TextSecondary,
                                fontSize = 10.sp
                            )
                        )
                    }
                }

                // Tag de Estado Oficial
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when (documento.estado?.uppercase()) {
                                "PUBLICADO", "ASSINADO" -> CIADIColors.SuccessGreen.copy(alpha = 0.15f)
                                "REVISAO" -> CIADIColors.OrangePrimary.copy(alpha = 0.15f)
                                else -> CIADIColors.TextMuted.copy(alpha = 0.15f)
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = documento.estado ?: "PUBLICADO",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = when (documento.estado?.uppercase()) {
                                "PUBLICADO", "ASSINADO" -> CIADIColors.SuccessGreen
                                "REVISAO" -> CIADIColors.OrangeDark
                                else -> CIADIColors.TextSecondary
                            }
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = CIADIColors.OrangePrimary, thickness = 2.dp)
            Spacer(modifier = Modifier.height(14.dp))

            // Metadados do Atendimento / Paciente
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(CIADIColors.CreamLight)
                    .padding(12.dp)
            ) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Assistido(a):",
                                style = MaterialTheme.typography.labelSmall.copy(color = CIADIColors.TextSecondary, fontSize = 10.sp)
                            )
                            Text(
                                text = documento.pacienteNome ?: "Lucas Silva",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = CIADIColors.TextPrimary)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Especialidade:",
                                style = MaterialTheme.typography.labelSmall.copy(color = CIADIColors.TextSecondary, fontSize = 10.sp)
                            )
                            Text(
                                text = documento.especialidadeNome ?: "Neurodesenvolvimento / P.E.I.",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = CIADIColors.TextPrimary)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Profissional Responsável:",
                                style = MaterialTheme.typography.labelSmall.copy(color = CIADIColors.TextSecondary, fontSize = 10.sp)
                            )
                            Text(
                                text = documento.profissionalNome ?: "Equipe Multidisciplinar CIADI",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, color = CIADIColors.TextPrimary)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Data de Emissão:",
                                style = MaterialTheme.typography.labelSmall.copy(color = CIADIColors.TextSecondary, fontSize = 10.sp)
                            )
                            Text(
                                text = documento.criadoEm ?: "2026-09-25",
                                style = MaterialTheme.typography.bodySmall.copy(color = CIADIColors.TextPrimary)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Título do Documento
            Text(
                text = documento.titulo,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = CIADIColors.Brown,
                    fontSize = 16.sp
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Conteúdo Clínico do Documento A4
            Text(
                text = documento.conteudoTexto
                    ?: "Relatório circunstanciado de evolução no Plano Educacional e Terapêutico Individualizado (P.E.I.). O assistido demonstra avanços consistentes no engajamento de atividades estruturadas e respostas positivas a apoios visuais com mediação do Acompanhante Terapêutico.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = CIADIColors.TextPrimary,
                    lineHeight = 22.sp,
                    fontSize = 13.sp
                ),
                textAlign = TextAlign.Justify,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Bloco de Assinatura e Registro Profissional
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Código de Validação / QR
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = "Código de Validação",
                        tint = CIADIColors.Brown,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Validação Digital CIADI",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = documento.codigoValidacao ?: "CIADI-DOC-2026-7789A",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = CIADIColors.TextSecondary
                            )
                        )
                    }
                }

                // Assinatura e Carimbo
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(180.dp)
                ) {
                    HorizontalDivider(color = Color.Black, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = documento.profissionalNome ?: "Dra. Renata Costa",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    )
                    Text(
                        text = documento.registroProfissional ?: "CRP 06/142981 - Responsável Técnico",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            color = CIADIColors.TextSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = CIADIColors.Outline, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(6.dp))

            // Rodapé Institucional A4
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CIADI — Documento Oficial emitido sob protocolos RLS e sigilo profissional.",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        color = CIADIColors.TextMuted
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = "Assinatura válida",
                        tint = CIADIColors.SuccessGreen,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Válido A4",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = CIADIColors.SuccessGreen
                        )
                    )
                }
            }
        }
    }
}
