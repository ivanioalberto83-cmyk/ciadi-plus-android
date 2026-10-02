package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CIADIColors.OrangePrimary,
    onPrimary = Color.White,
    primaryContainer = CIADIColors.OrangeDark,
    onPrimaryContainer = Color.White,
    secondary = CIADIColors.Yellow,
    onSecondary = CIADIColors.BrownDark,
    secondaryContainer = CIADIColors.Brown,
    onSecondaryContainer = CIADIColors.Cream,
    tertiary = CIADIColors.Cream,
    onTertiary = CIADIColors.BrownDark,
    background = CIADIColors.BackgroundDark,
    onBackground = CIADIColors.TextPrimaryDark,
    surface = CIADIColors.SurfaceDark,
    onSurface = CIADIColors.TextPrimaryDark,
    surfaceVariant = CIADIColors.SurfaceVariantDark,
    onSurfaceVariant = CIADIColors.TextSecondaryDark,
    outline = CIADIColors.OutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = CIADIColors.OrangePrimary,
    onPrimary = Color.White,
    primaryContainer = CIADIColors.Cream,
    onPrimaryContainer = CIADIColors.Brown,
    secondary = CIADIColors.Yellow,
    onSecondary = CIADIColors.BrownDark,
    secondaryContainer = CIADIColors.CreamLight,
    onSecondaryContainer = CIADIColors.Brown,
    tertiary = CIADIColors.Brown,
    onTertiary = Color.White,
    background = CIADIColors.BackgroundLight,
    onBackground = CIADIColors.TextPrimary,
    surface = CIADIColors.SurfaceWhite,
    onSurface = CIADIColors.TextPrimary,
    surfaceVariant = CIADIColors.CreamLight,
    onSurfaceVariant = CIADIColors.TextSecondary,
    outline = CIADIColors.Outline
)

@Composable
fun CiadiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Preserva a identidade visual oficial do CIADI
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    CiadiTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
