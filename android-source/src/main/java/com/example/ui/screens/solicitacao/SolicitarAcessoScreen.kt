package com.example.ui.screens.solicitacao

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CiadiDynamicBackground
import com.example.ui.designsystem.CIADIButtons
import com.example.ui.theme.CIADIColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolicitarAcessoScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var nomeResponsavel by remember { mutableStateOf("") }
    var parentesco by remember { mutableStateOf("Mãe") }
    var telefone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var nomeCrianca by remember { mutableStateOf("") }
    var dataNascimentoCrianca by remember { mutableStateOf("") }
    var observacoes by remember { mutableStateOf("") }
    var enviadoSucesso by remember { mutableStateOf(false) }

    val parentescoOptions = listOf("Mãe", "Pai", "Tutor Legal", "Avô(ó)", "Outro")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Solicitar Acesso — Família",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = CIADIColors.Brown
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("solicitar_acesso_back_btn")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = CIADIColors.Brown)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        CiadiDynamicBackground(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (enviadoSucesso) {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("solicitacao_sucesso_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(CIADIColors.SuccessGreen.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = CIADIColors.SuccessGreen,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                text = "Solicitação Enviada com Sucesso!",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = CIADIColors.Brown
                                ),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "O seu pedido de acesso foi registrado com status PENDENTE. A Secretaria e Coordenação do CIADI analisarão os dados para confirmação do vínculo familiar com $nomeCrianca.\n\nAssim que aprovado, você receberá a confirmação de ativação do seu acesso.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = CIADIColors.TextPrimary,
                                    lineHeight = 22.sp
                                ),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            CIADIButtons.Primary(
                                text = "VOLTAR AO LOGIN",
                                onClick = onNavigateBack,
                                modifier = Modifier.fillMaxWidth().testTag("solicitacao_voltar_login_btn")
                            )
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "Identificação do Responsável",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = CIADIColors.Brown
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Informe os dados para análise da Secretaria do CIADI.",
                                style = MaterialTheme.typography.bodySmall.copy(color = CIADIColors.TextSecondary)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = nomeResponsavel,
                                onValueChange = { nomeResponsavel = it },
                                label = { Text("Nome completo do responsável") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = CIADIColors.Brown) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("input_solicitar_nome_resp"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Grau de Parentesco:", style = MaterialTheme.typography.labelMedium.copy(color = CIADIColors.TextSecondary, fontWeight = FontWeight.SemiBold))
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                parentescoOptions.take(3).forEach { option ->
                                    val isSelected = parentesco == option
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { parentesco = option },
                                        label = { Text(option, fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = CIADIColors.CreamLight,
                                            selectedLabelColor = CIADIColors.OrangePrimary
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = telefone,
                                onValueChange = { telefone = it },
                                label = { Text("Telefone / WhatsApp") },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = CIADIColors.Brown) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("input_solicitar_telefone"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("E-mail para acesso") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = CIADIColors.Brown) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("input_solicitar_email"),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "Identificação da Criança / Assistido",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = CIADIColors.Brown
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Dados para conferência do vínculo no prontuário CIADI.",
                                style = MaterialTheme.typography.bodySmall.copy(color = CIADIColors.TextSecondary)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = nomeCrianca,
                                onValueChange = { nomeCrianca = it },
                                label = { Text("Nome completo da criança") },
                                leadingIcon = { Icon(Icons.Default.ChildCare, contentDescription = null, tint = CIADIColors.Brown) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("input_solicitar_nome_crianca"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = dataNascimentoCrianca,
                                onValueChange = { dataNascimentoCrianca = it },
                                label = { Text("Data de nascimento (DD/MM/AAAA)") },
                                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = CIADIColors.Brown) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("input_solicitar_data_nasc"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = observacoes,
                                onValueChange = { observacoes = it },
                                label = { Text("Observações adicionais (opcional)") },
                                maxLines = 3,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    CIADIButtons.Primary(
                        text = "ENVIAR SOLICITAÇÃO DE ACESSO",
                        onClick = {
                            if (nomeResponsavel.isNotBlank() && email.isNotBlank() && nomeCrianca.isNotBlank()) {
                                enviadoSucesso = true
                            }
                        },
                        icon = Icons.AutoMirrored.Filled.Send,
                        modifier = Modifier.fillMaxWidth().testTag("btn_enviar_solicitacao")
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
