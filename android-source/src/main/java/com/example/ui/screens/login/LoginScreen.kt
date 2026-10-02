package com.example.ui.screens.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.UserRole
import com.example.ui.components.CiadiDynamicBackground
import com.example.ui.theme.CIADIColors

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToBlocked: (String) -> Unit,
    onNavigateToSolicitarAcesso: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.dismissError()
        }
    }

    if (uiState.passwordResetSent) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissPasswordResetAlert() },
            title = { Text("Recuperação de Acesso") },
            text = {
                Text("Um link de redefinição foi enviado para o e-mail informado através do Supabase GoTrue.")
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissPasswordResetAlert() },
                    colors = ButtonDefaults.buttonColors(containerColor = CIADIColors.OrangePrimary)
                ) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        CiadiDynamicBackground(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            showJanethBackground = true
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // Cabeçalho Institucional Oficial CIADI
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(CIADIColors.Cream),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_ciadi_logo_1790332105105),
                        contentDescription = "Logo Oficial CIADI+",
                        modifier = Modifier.size(64.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "CIADI+",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = CIADIColors.Brown,
                        letterSpacing = 0.5.sp
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Centro Integrado de Apoio e Desenvolvimento Individual",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        color = CIADIColors.TextSecondary,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Cartão de Login Limpo e Profissional
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(22.dp)) {
                        Text(
                            text = "Entrar no CIADI+",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = CIADIColors.Brown
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Selecione o seu perfil de acesso:",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                color = CIADIColors.TextSecondary
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // As 5 portas de entrada por perfil solicitadas no item 39:
                        // [ A.T. ] [ FAMÍLIA ] [ ADMIN ] [ SECRETARIA ] [ PROFISSIONAL ]
                        val portalDoors = listOf(
                            Triple(UserRole.AT, "A.T.", "Acompanhante Terapêutico"),
                            Triple(UserRole.RESPONSAVEL, "FAMÍLIA", "Pais e Responsáveis Legais"),
                            Triple(UserRole.ADMIN, "ADMIN", "Administração Institucional"),
                            Triple(UserRole.OPERADOR, "SECRETARIA", "Secretaria & Atendimento"),
                            Triple(UserRole.PROFISSIONAL, "PROFISSIONAL", "Corpo Clínico Especializado")
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            portalDoors.forEach { (doorRole, title, subtitle) ->
                                val isSelected = uiState.selectedPortalDoor == doorRole
                                val borderColor = if (isSelected) CIADIColors.OrangePrimary else CIADIColors.Outline
                                val backgroundColor = if (isSelected) CIADIColors.CreamLight else CIADIColors.SurfaceCard

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(backgroundColor)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = borderColor,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { viewModel.onPortalDoorSelected(doorRole) }
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                        .testTag("portal_door_${doorRole.code}")
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = title,
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) CIADIColors.OrangeDark else CIADIColors.TextPrimary
                                                )
                                            )
                                            Text(
                                                text = subtitle,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 12.sp,
                                                    color = CIADIColors.TextSecondary
                                                )
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    if (isSelected) CIADIColors.OrangePrimary else Color.Transparent
                                                )
                                                .border(
                                                    width = 2.dp,
                                                    color = if (isSelected) CIADIColors.OrangePrimary else CIADIColors.Outline,
                                                    shape = RoundedCornerShape(10.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(Color.White)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Campo de E-mail Limpo (Sem credenciais de teste ou hardcode)
                        OutlinedTextField(
                            value = uiState.email,
                            onValueChange = { viewModel.onEmailChanged(it) },
                            label = { Text("E-mail cadastrado", fontSize = 14.sp) },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = CIADIColors.Brown)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = CIADIColors.TextPrimary,
                                unfocusedTextColor = CIADIColors.TextPrimary,
                                focusedLabelColor = CIADIColors.OrangePrimary,
                                unfocusedLabelColor = CIADIColors.TextSecondary,
                                focusedBorderColor = CIADIColors.OrangePrimary,
                                unfocusedBorderColor = CIADIColors.Outline
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("email_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Campo de Palavra-passe
                        OutlinedTextField(
                            value = uiState.password,
                            onValueChange = { viewModel.onPasswordChanged(it) },
                            label = { Text("Palavra-passe", fontSize = 14.sp) },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = CIADIColors.Brown)
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (passwordVisible) "Ocultar senha" else "Mostrar senha",
                                        tint = CIADIColors.Brown
                                    )
                                }
                            },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    viewModel.login(
                                        onSuccess = onLoginSuccess,
                                        onBlocked = onNavigateToBlocked
                                    )
                                }
                            ),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = CIADIColors.TextPrimary,
                                unfocusedTextColor = CIADIColors.TextPrimary,
                                focusedLabelColor = CIADIColors.OrangePrimary,
                                unfocusedLabelColor = CIADIColors.TextSecondary,
                                focusedBorderColor = CIADIColors.OrangePrimary,
                                unfocusedBorderColor = CIADIColors.Outline
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("password_input")
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Botão Entrar Oficial
                        Button(
                            onClick = {
                                viewModel.login(
                                    onSuccess = onLoginSuccess,
                                    onBlocked = onNavigateToBlocked
                                )
                            },
                            enabled = !uiState.isLoading,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CIADIColors.OrangePrimary,
                                disabledContainerColor = CIADIColors.OrangePrimary.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("login_button")
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Text(
                                    text = "ENTRAR NO CIADI+",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Opção de Recuperar Senha
                        TextButton(
                            onClick = { viewModel.requestPasswordReset() },
                            modifier = Modifier.fillMaxWidth().testTag("recover_password_button")
                        ) {
                            Text(
                                text = "Esqueceu a sua palavra-passe?",
                                color = CIADIColors.OrangePrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Opção de Solicitar Acesso para Famílias (Item 42)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ainda não tem acesso? ",
                                color = CIADIColors.TextSecondary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Solicitar acesso",
                                color = CIADIColors.TealPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { onNavigateToSolicitarAcesso() }
                                    .testTag("link_solicitar_acesso")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
