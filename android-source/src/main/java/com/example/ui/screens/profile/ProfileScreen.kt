package com.example.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Permission
import com.example.domain.model.UserProfile
import com.example.ui.components.CiadiTopBar
import com.example.ui.components.RoleBadge
import com.example.ui.designsystem.CIADICards
import com.example.ui.theme.CIADIColors

@Composable
fun ProfileScreen(
    user: UserProfile,
    onNavigateBack: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            CiadiTopBar(
                title = "Perfil & Permissões RLS",
                currentUser = user,
                onNavigateBack = onNavigateBack
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(CIADIColors.BackgroundLight)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar e Identificação
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(CIADIColors.Cream),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = CIADIColors.Brown,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = user.fullName,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = CIADIColors.TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = user.email,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = CIADIColors.TextSecondary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            RoleBadge(role = user.role)

            Spacer(modifier = Modifier.height(24.dp))

            // Detalhes da Instituição e Especialidade
            CIADICards.Warm(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Dados Institucionais",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = CIADIColors.Brown
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Unidade: ${user.unitName}",
                        style = MaterialTheme.typography.bodyMedium.copy(color = CIADIColors.TextPrimary)
                    )
                    user.specialty?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Especialidade: $it",
                            style = MaterialTheme.typography.bodySmall.copy(color = CIADIColors.TextSecondary)
                        )
                    }
                    user.activePatientName?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Assistido Vinculado: $it",
                            style = MaterialTheme.typography.bodySmall.copy(color = CIADIColors.TextSecondary)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Permissões ativas e RLS
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = CIADIColors.OrangePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Políticas RLS Aplicadas ao Perfil",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = CIADIColors.Brown
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Permission.entries.forEach { permission ->
                    val isGranted = user.hasPermission(permission)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isGranted) Color.White else CIADIColors.BackgroundLight
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isGranted) CIADIColors.Outline else CIADIColors.Outline.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = if (isGranted) "Ativo" else "Inativo",
                                tint = if (isGranted) CIADIColors.SuccessGreen else Color(0xFFB0BEC5),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = permission.title,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (isGranted) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isGranted) CIADIColors.TextPrimary else CIADIColors.TextSecondary
                                    )
                                )
                                Text(
                                    text = "RLS: ${permission.rlsPolicyName}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = CIADIColors.TextSecondary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Botão de Logout
            OutlinedButton(
                onClick = onSignOut,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = CIADIColors.ErrorRed
                ),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, CIADIColors.ErrorRed),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("logout_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    tint = CIADIColors.ErrorRed,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Encerrar Sessão",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = CIADIColors.ErrorRed
                    )
                )
            }
        }
    }
}
