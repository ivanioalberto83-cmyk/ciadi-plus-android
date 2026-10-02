package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.CIADIColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * CiadiDynamicBackground
 *
 * Sistema visual de fundo institucional do CIADI+.
 * Substitui o fundo branco monótono por formas orgânicas acolhedoras, manchas circulares,
 * gradientes suaves e movimentos lentos, garantindo 100% de legibilidade e alto contraste.
 *
 * Cores utilizadas:
 * - Fundo claro: #FBFDFF / #FFF8EC
 * - Creme: #FFE7BF
 * - Amarelo: #FFDF19
 * - Laranja CIADI: #F58200 (suave)
 * - Castanho: #6F351F (muito sutil)
 *
 * Respeita reducedMotion para utilizadores com preferência de movimento reduzido.
 */
@Composable
fun CiadiDynamicBackground(
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = false,
    showJanethBackground: Boolean = false,
    content: @Composable () -> Unit
) {
    // Animação contínua lenta e institucional (ciclos de 18 a 26 segundos)
    val infiniteTransition = rememberInfiniteTransition(label = "ciadi_background_motion")

    val phase1 by if (!reducedMotion) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 2f * PI.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 22000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "phase1"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }

    val phase2 by if (!reducedMotion) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 2f * PI.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 18000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "phase2"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // 1. Gradiente Base Acolhedor: do #FBFDFF para #FFF8EC com toque de creme suave
            val baseGradient = Brush.verticalGradient(
                colors = listOf(
                    CIADIColors.BackgroundLight,
                    Color(0xFFFFF9EE),
                    Color(0xFFFFF4E4)
                ),
                startY = 0f,
                endY = height
            )
            drawRect(brush = baseGradient, size = size)

            // Deslocamentos senoidais lentos e quase imperceptíveis
            val shiftX1 = if (!reducedMotion) sin(phase1.toDouble()).toFloat() * 24f else 0f
            val shiftY1 = if (!reducedMotion) cos(phase1.toDouble()).toFloat() * 18f else 0f

            val shiftX2 = if (!reducedMotion) cos(phase2.toDouble()).toFloat() * 20f else 0f
            val shiftY2 = if (!reducedMotion) sin(phase2.toDouble()).toFloat() * 24f else 0f

            // 2. Grande Círculo Superior Direito (Laranja CIADI suave / Creme)
            // Representa sol, acolhimento e calor humano no desenvolvimento infantil
            val centerTopRight = Offset(width * 0.92f + shiftX1, height * 0.08f + shiftY1)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        CIADIColors.OrangePrimary.copy(alpha = 0.10f),
                        CIADIColors.Cream.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = centerTopRight,
                    radius = width * 0.65f
                ),
                center = centerTopRight,
                radius = width * 0.65f
            )

            // 3. Mancha Orgânica Esquerda Média (Amarelo e Creme Destaque)
            val centerLeft = Offset(width * 0.05f + shiftX2, height * 0.38f + shiftY2)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        CIADIColors.Yellow.copy(alpha = 0.12f),
                        CIADIColors.Cream.copy(alpha = 0.06f),
                        Color.Transparent
                    ),
                    center = centerLeft,
                    radius = width * 0.55f
                ),
                center = centerLeft,
                radius = width * 0.55f
            )

            // 4. Círculo Inferior Direito (Castanho acolhedor + Laranja)
            val centerBottom = Offset(width * 0.85f - shiftX1, height * 0.82f - shiftY2)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        CIADIColors.OrangeLight.copy(alpha = 0.09f),
                        CIADIColors.Cream.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = centerBottom,
                    radius = width * 0.60f
                ),
                center = centerBottom,
                radius = width * 0.60f
            )

            // 5. Onda fluida orgânica sutil na parte superior para quebrar a monotonia
            val wavePath = Path().apply {
                moveTo(0f, 0f)
                lineTo(width, 0f)
                lineTo(width, height * 0.14f + shiftY1 * 0.5f)
                cubicTo(
                    width * 0.70f, height * 0.18f + shiftY2 * 0.4f,
                    width * 0.30f, height * 0.08f - shiftY1 * 0.4f,
                    0f, height * 0.13f + shiftY2 * 0.5f
                )
                close()
            }
            drawPath(
                path = wavePath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        CIADIColors.Cream.copy(alpha = 0.22f),
                        CIADIColors.CreamLight.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(width, height * 0.16f)
                ),
                style = Fill
            )

            // 6. Pequenos elementos circulares lúdicos / clínicos (remetem a conexões neurais e crescimento)
            val dot1 = Offset(width * 0.18f + shiftX1 * 0.5f, height * 0.18f + shiftY1 * 0.5f)
            drawCircle(color = CIADIColors.OrangePrimary.copy(alpha = 0.15f), radius = 6f, center = dot1)

            val dot2 = Offset(width * 0.88f - shiftX2 * 0.4f, height * 0.26f + shiftY2 * 0.4f)
            drawCircle(color = CIADIColors.Yellow.copy(alpha = 0.20f), radius = 8f, center = dot2)

            val dot3 = Offset(width * 0.12f + shiftX2 * 0.3f, height * 0.65f - shiftY1 * 0.3f)
            drawCircle(color = CIADIColors.Cream.copy(alpha = 0.35f), radius = 10f, center = dot3)

            val dot4 = Offset(width * 0.78f + shiftX1 * 0.3f, height * 0.55f + shiftY1 * 0.4f)
            drawCircle(color = CIADIColors.OrangePrimary.copy(alpha = 0.12f), radius = 5f, center = dot4)
        }

        // Janeth integrada ao background oficial CIADI+ (alta elegância, lado direito/centro-direito, baixa opacidade)
        if (showJanethBackground) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Image(
                    painter = painterResource(id = R.drawable.janeth_avatar_1790333481531),
                    contentDescription = null, // Estritamente decorativo de fundo
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxHeight(0.75f)
                        .width(370.dp)
                        .offset(x = 40.dp, y = (-15).dp)
                        .alpha(0.095f)
                )
            }
        }

        // Conteúdo da Tela renderizado em cima do background dinâmico
        content()
    }
}
