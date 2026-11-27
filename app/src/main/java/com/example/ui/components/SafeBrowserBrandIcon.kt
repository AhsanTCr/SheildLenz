package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
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
 * Flagship vector browser emblem inspired by top standalone browsers:
 * Sapphire-Cyan orbital ring around a luminous crystal globe and glowing prism shield needle.
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
        initialValue = -3.0f,
        targetValue = 3.0f,
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

        // 1. Soft sapphire-cyan halo
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    (if (isProUnlocked) Color(0xFF06B6D4) else AegisPrimaryCyan).copy(alpha = 0.28f),
                    Color.Transparent
                ),
                center = c,
                radius = outerR * 1.18f
            ),
            radius = outerR * 1.18f,
            center = c
        )

        // 2. Luminous Sapphire-Cyan orbital ring
        val ringR = w * 0.42f
        val ringTopLeft = Offset(c.x - ringR, c.y - ringR)
        val ringSize = Size(ringR * 2f, ringR * 2f)
        val strokeW = w * 0.115f
        rotate(degrees = orbitSpin, pivot = c) {
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFF06B6D4),
                        AegisPrimaryCyan,
                        AegisElectricBlue,
                        AegisPurple,
                        AegisEmeraldTeal,
                        Color(0xFF06B6D4)
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

        // 3. White frosted separator bezel + sapphire crystal globe core
        val globeR = w * 0.305f
        drawCircle(
            color = Color.White,
            radius = globeR + w * 0.032f,
            center = c
        )
        drawCircle(
            brush = Brush.linearGradient(
                colors = if (isProUnlocked) {
                    listOf(Color(0xFF06B6D4), Color(0xFF0EA5E9), Color(0xFF2563EB), Color(0xFF4F46E5))
                } else {
                    listOf(Color(0xFF38BDF8), AegisPrimaryCyan, Color(0xFF1D4ED8))
                },
                start = Offset(c.x - globeR, c.y - globeR),
                end = Offset(c.x + globeR, c.y + globeR)
            ),
            radius = globeR,
            center = c
        )

        // Crystal lens specular gleam
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.65f), Color.Transparent),
                center = Offset(c.x - globeR * 0.35f, c.y - globeR * 0.35f),
                radius = globeR * 0.55f
            ),
            radius = globeR * 0.55f,
            center = Offset(c.x - globeR * 0.35f, c.y - globeR * 0.35f)
        )

        // 4. Glowing Prism Shield Needle (chamfered compass/shield diamond core)
        rotate(degrees = 34f + compassBreath, pivot = c) {
            val tipLen = globeR * 0.76f
            val waistW = globeR * 0.27f

            val northBlade = Path().apply {
                moveTo(c.x, c.y - tipLen)
                lineTo(c.x + waistW, c.y - globeR * 0.06f)
                lineTo(c.x, c.y + globeR * 0.06f)
                lineTo(c.x - waistW, c.y - globeR * 0.06f)
                close()
            }
            val southBlade = Path().apply {
                moveTo(c.x, c.y + tipLen)
                lineTo(c.x + waistW, c.y + globeR * 0.06f)
                lineTo(c.x, c.y - globeR * 0.06f)
                lineTo(c.x - waistW, c.y + globeR * 0.06f)
                close()
            }
            drawPath(northBlade, color = Color.White)
            drawPath(
                southBlade,
                color = if (isProUnlocked) Color(0xFF67E8F9) else AegisChampagneGold.copy(alpha = 0.94f)
            )
            drawCircle(
                color = Color(0xFF0F295C),
                radius = globeR * 0.11f,
                center = c
            )
        }
    }
}

/**
 * Generates an adaptive-ready high-DPI Sapphire-Cyan Orbital Prism Bitmap
 * for pinned Home Screen browser shortcuts, site shortcuts, and Android Home Screen Widgets.
 */
fun createSafeBrowserBrandBitmap(
    sizePx: Int = 256,
    isProUnlocked: Boolean = true
): Bitmap {
    val safeSize = sizePx.coerceIn(64, 512)
    val bitmap = Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val w = safeSize.toFloat()
    val cx = w * 0.5f
    val cy = w * 0.5f

    // 1. Soft sky-crystal backing circle (so Android launcher icon masks look clean & luminous)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            cx,
            cy,
            w * 0.50f,
            intArrayOf(
                android.graphics.Color.parseColor("#FFFFFF"),
                android.graphics.Color.parseColor("#E0F2FE"),
                android.graphics.Color.parseColor("#DCEBFF")
            ),
            floatArrayOf(0f, 0.72f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawCircle(cx, cy, w * 0.48f, bgPaint)

    // 2. Sapphire-Cyan Orbital Ring
    val ringR = w * 0.375f
    val strokeW = w * 0.095f
    val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeW
        strokeCap = Paint.Cap.ROUND
        shader = SweepGradient(
            cx,
            cy,
            intArrayOf(
                android.graphics.Color.parseColor("#06B6D4"),
                android.graphics.Color.parseColor("#0EA5E9"),
                android.graphics.Color.parseColor("#2563EB"),
                android.graphics.Color.parseColor("#7C5CFA"),
                android.graphics.Color.parseColor("#0D9488"),
                android.graphics.Color.parseColor("#06B6D4")
            ),
            null
        )
    }
    canvas.drawOval(RectF(cx - ringR, cy - ringR, cx + ringR, cy + ringR), ringPaint)

    // 3. Frosted white separator ring + sapphire core
    val globeR = w * 0.275f
    val bezelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
    }
    canvas.drawCircle(cx, cy, globeR + w * 0.032f, bezelPaint)

    val globePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            cx - globeR,
            cy - globeR,
            cx + globeR,
            cy + globeR,
            if (isProUnlocked) {
                intArrayOf(
                    android.graphics.Color.parseColor("#06B6D4"),
                    android.graphics.Color.parseColor("#0EA5E9"),
                    android.graphics.Color.parseColor("#2563EB"),
                    android.graphics.Color.parseColor("#4F46E5")
                )
            } else {
                intArrayOf(
                    android.graphics.Color.parseColor("#38BDF8"),
                    android.graphics.Color.parseColor("#0EA5E9"),
                    android.graphics.Color.parseColor("#1D4ED8")
                )
            },
            null,
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawCircle(cx, cy, globeR, globePaint)

    // Specular glint
    val glintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            cx - globeR * 0.34f,
            cy - globeR * 0.34f,
            globeR * 0.55f,
            intArrayOf(android.graphics.Color.argb(170, 255, 255, 255), android.graphics.Color.TRANSPARENT),
            null,
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawCircle(cx - globeR * 0.34f, cy - globeR * 0.34f, globeR * 0.55f, glintPaint)

    // 4. Prism Shield Needle rotated at 34 degrees
    canvas.save()
    canvas.rotate(34f, cx, cy)
    val tipLen = globeR * 0.76f
    val waistW = globeR * 0.27f

    val northPath = android.graphics.Path().apply {
        moveTo(cx, cy - tipLen)
        lineTo(cx + waistW, cy - globeR * 0.05f)
        lineTo(cx, cy + globeR * 0.06f)
        lineTo(cx - waistW, cy - globeR * 0.05f)
        close()
    }
    val northPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
    }
    canvas.drawPath(northPath, northPaint)

    val southPath = android.graphics.Path().apply {
        moveTo(cx, cy + tipLen)
        lineTo(cx + waistW, cy + globeR * 0.05f)
        lineTo(cx, cy - globeR * 0.06f)
        lineTo(cx - waistW, cy + globeR * 0.05f)
        close()
    }
    val southPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isProUnlocked) {
            android.graphics.Color.parseColor("#67E8F9")
        } else {
            android.graphics.Color.parseColor("#F5C451")
        }
    }
    canvas.drawPath(southPath, southPaint)

    val hubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#0F295C")
    }
    canvas.drawCircle(cx, cy, globeR * 0.11f, hubPaint)
    canvas.restore()

    return bitmap
}
