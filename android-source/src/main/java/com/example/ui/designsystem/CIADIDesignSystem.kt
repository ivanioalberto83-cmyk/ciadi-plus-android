package com.example.ui.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CIADIColors

/**
 * CIADI+ Design System Oficial
 * Centro Integrado de Apoio e Desenvolvimento Individual
 *
 * Padrão clínico humanizado, com alto contraste WCAG 2.1 AA.
 */
object CIADIShapes {
    val Small = RoundedCornerShape(10.dp)
    val Medium = RoundedCornerShape(16.dp)
    val Large = RoundedCornerShape(20.dp)
    val ExtraLarge = RoundedCornerShape(28.dp)
    val Pill = CircleShape
}

object CIADITypography {
    val HeadlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.5).sp,
        color = CIADIColors.Brown
    )
    val HeadlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        color = CIADIColors.Brown
    )
    val TitleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = CIADIColors.Brown
    )
    val TitleMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = CIADIColors.TextPrimary
    )
    val BodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = CIADIColors.TextPrimary
    )
    val BodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = CIADIColors.TextPrimary
    )
    val BodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = CIADIColors.TextSecondary
    )
    val LabelSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        color = CIADIColors.TextSecondary
    )
}

object CIADIButtons {
    @Composable
    fun Primary(
        text: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        isLoading: Boolean = false,
        icon: ImageVector? = null,
        shape: Shape = CIADIShapes.Medium
    ) {
        Button(
            onClick = onClick,
            enabled = enabled && !isLoading,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = CIADIColors.OrangePrimary,
                contentColor = Color.White,
                disabledContainerColor = CIADIColors.OrangePrimary.copy(alpha = 0.4f),
                disabledContentColor = Color.White.copy(alpha = 0.7f)
            ),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
            modifier = modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
            } else if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            )
        }
    }

    @Composable
    fun Secondary(
        text: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        icon: ImageVector? = null,
        shape: Shape = CIADIShapes.Medium
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = CIADIColors.Cream,
                contentColor = CIADIColors.Brown,
                disabledContainerColor = CIADIColors.Cream.copy(alpha = 0.4f)
            ),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            modifier = modifier.height(50.dp)
        ) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = CIADIColors.Brown)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = CIADIColors.Brown
                )
            )
        }
    }

    @Composable
    fun Outlined(
        text: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        icon: ImageVector? = null,
        shape: Shape = CIADIShapes.Medium
    ) {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            shape = shape,
            border = BorderStroke(1.5.dp, CIADIColors.Outline),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = CIADIColors.TextPrimary
            ),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            modifier = modifier.height(50.dp)
        ) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = CIADIColors.TextPrimary)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = CIADIColors.TextPrimary
                )
            )
        }
    }

    @Composable
    fun Emergency(
        text: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        isLoading: Boolean = false
    ) {
        Button(
            onClick = onClick,
            enabled = enabled && !isLoading,
            shape = CIADIShapes.Large,
            colors = ButtonDefaults.buttonColors(
                containerColor = CIADIColors.ErrorRed,
                contentColor = Color.White
            ),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
            modifier = modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("emergency_button")
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(10.dp))
            } else {
                Icon(imageVector = Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            )
        }
    }
}

object CIADICards {
    @Composable
    fun Base(
        modifier: Modifier = Modifier,
        backgroundColor: Color = CIADIColors.SurfaceWhite,
        borderColor: Color = CIADIColors.Outline,
        elevation: Dp = 1.5.dp,
        shape: Shape = CIADIShapes.Large,
        content: @Composable () -> Unit
    ) {
        Card(
            modifier = modifier,
            shape = shape,
            border = BorderStroke(1.dp, borderColor),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            elevation = CardDefaults.cardElevation(defaultElevation = elevation)
        ) {
            content()
        }
    }

    @Composable
    fun Warm(
        modifier: Modifier = Modifier,
        shape: Shape = CIADIShapes.Large,
        content: @Composable () -> Unit
    ) {
        Card(
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = CIADIColors.CreamLight),
            border = BorderStroke(1.dp, CIADIColors.Cream),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            content()
        }
    }
}

object CIADIBadge {
    @Composable
    fun Modalidade(isOnline: Boolean, modifier: Modifier = Modifier) {
        val bg = if (isOnline) Color(0xFFE0F2F1) else CIADIColors.Cream
        val textColor = if (isOnline) CIADIColors.TealPrimary else CIADIColors.Brown
        val icon = if (isOnline) Icons.Default.Videocam else Icons.Default.Business
        val label = if (isOnline) "Consulta Online" else "Presencial"

        Row(
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .background(bg)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isOnline) "🟠 " else "🟡 ",
                fontSize = 11.sp
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            )
        }
    }
}

@Composable
fun CIADIBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        Triple("dashboard", "Início", Icons.Default.Home),
        Triple("agenda", "Calendário", Icons.Default.CalendarMonth),
        Triple("tracking", "Acompanhamento", Icons.Default.Timeline),
        Triple("janeth", "Janeth", Icons.AutoMirrored.Filled.Chat),
        Triple("profile", "Perfil", Icons.Default.Person)
    )

    NavigationBar(
        containerColor = CIADIColors.SurfaceWhite,
        contentColor = CIADIColors.Brown,
        tonalElevation = 8.dp,
        modifier = modifier
            .border(BorderStroke(0.5.dp, CIADIColors.Outline))
    ) {
        items.forEach { (route, label, icon) ->
            val isSelected = currentRoute == route
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(route) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) CIADIColors.OrangePrimary else CIADIColors.TextSecondary,
                            fontSize = 11.sp
                        )
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = CIADIColors.OrangePrimary,
                    selectedTextColor = CIADIColors.OrangePrimary,
                    unselectedIconColor = CIADIColors.TextSecondary,
                    unselectedTextColor = CIADIColors.TextSecondary,
                    indicatorColor = CIADIColors.CreamLight
                ),
                modifier = Modifier.testTag("nav_item_$route")
            )
        }
    }
}
