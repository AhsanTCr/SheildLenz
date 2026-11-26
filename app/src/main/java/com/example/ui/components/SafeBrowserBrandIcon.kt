package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisElectricBlue
import com.example.ui.theme.AegisEmeraldTeal
import com.example.ui.theme.AegisGoldBright
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisPurple

/**
 * Sleek, professional vector browser icon inspired by top flagship browsers:
 * Multi-band luminous orbital rings around a frosted sapphire-glass core and crisp 38-degree compass needle.
 */
@Composable
fun SafeBrowserBrandIcon(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    isProUnlocked: Boolean = false
) {
    val transition = rememberInfiniteTransition(label = "SafeBrowserIconMotion")
    val orbitSpin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SafeBrowserOrbitSpin"
    )
    val compassBreath by transition.animateFloat(
        initialValue = -3.5f,
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SafeBrowserCompassBreath"
    )

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val c = Offset(w * 0.5f, h * 0.5f)
        val outerR = w * 0.46f

        // 1. Soft sky-royal ambient glow behind browser globe
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    (if (isProUnlocked) AegisPurple else AegisPrimaryCyan).copy(alpha = 0.24f),
                    Color.Transparent
                ),
                center = c,
                radius = outerR * 1.18f
            ),
            radius = outerR * 1.18f,
            center = c
        )

        // 2. Chrome/Safari-inspired sleek tri-tone segmented orbital frame (rotating very smoothly)
        val ringR = w * 0.42f
        val ringTopLeft = Offset(c.x - ringR, c.y - ringR)
        val ringSize = Size(ringR * 2f, ringR * 2f)
        val strokeW = w * 0.115f
        rotate(degrees = orbitSpin, pivot = c) {
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        AegisElectricBlue,
                        AegisPrimaryCyan,
                        AegisEmeraldTeal,
                        AegisGoldBright,
                        AegisPurple,
                        AegisElectricBlue
                    ),
                    center = c
                ),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = ringTopLeft,
                size = ringSize,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )
        }

        // 3. White frosted separator ring + sapphire-azure crystal globe lens
        val globeR = w * 0.305f
        drawCircle(
            color = Color.White,
            radius = globeR + w * 0.03f,
            center = c
        )
        drawCircle(
            brush = Brush.linearGradient(
                colors = if (isProUnlocked) {
                    listOf(Color(0xFF0EA5E9), AegisElectricBlue, AegisPurple)
                } else {
                    listOf(Color(0xFF0EA5E9), AegisPrimaryCyan, Color(0xFF1D4ED8))
                },
                start = Offset(c.x - globeR, c.y - globeR),
                end = Offset(c.x + globeR, c.y + globeR)
            ),
            radius = globeR,
            center = c
        )

        // Top-left crystal lens specular glint
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.62f), Color.Transparent),
                center = Offset(c.x - globeR * 0.35f, c.y - globeR * 0.35f),
                radius = globeR * 0.55f
            ),
            radius = globeR * 0.55f,
            center = Offset(c.x - globeR * 0.35f, c.y - globeR * 0.35f)
        )

        // 4. Precision compass prism needle (angled at ~38 deg with gentle magnetic sway)
        rotate(degrees = 38f + compassBreath, pivot = c) {
            val tipLen = globeR * 0.72f
            val waistW = globeR * 0.24f

            val northBlade = Path().apply {
                moveTo(c.x, c.y - tipLen)
                lineTo(c.x + waistW, c.y)
                lineTo(c.x - waistW, c.y)
                close()
            }
            val southBlade = Path().apply {
                moveTo(c.x, c.y + tipLen)
                lineTo(c.x + waistW, c.y)
                lineTo(c.x - waistW, c.y)
                close()
            }
            drawPath(northBlade, color = Color.White)
            drawPath(
                southBlade,
                color = if (isProUnlocked) AegisGoldBright else AegisChampagneGold.copy(alpha = 0.92f)
            )
            drawCircle(
                color = Color(0xFF0F295C),
                radius = globeR * 0.11f,
                center = c
            )
        }
    }
}
