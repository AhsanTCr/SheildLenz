package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AegisCardBackground
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisChampagneSoft
import com.example.ui.theme.AegisElectricBlue
import com.example.ui.theme.AegisEmeraldBorder
import com.example.ui.theme.AegisEmeraldBright
import com.example.ui.theme.AegisEmeraldSoft
import com.example.ui.theme.AegisEmeraldSurface
import com.example.ui.theme.AegisEmeraldTeal
import com.example.ui.theme.AegisGoldBorder
import com.example.ui.theme.AegisGoldBright
import com.example.ui.theme.AegisGoldDeepTint
import com.example.ui.theme.AegisIndigo
import com.example.ui.theme.AegisIndigoBorder
import com.example.ui.theme.AegisIndigoBright
import com.example.ui.theme.AegisIndigoSoft
import com.example.ui.theme.AegisIndigoSurface
import com.example.ui.theme.AegisLavenderBorder
import com.example.ui.theme.AegisLavenderLight
import com.example.ui.theme.AegisLavenderSoft
import com.example.ui.theme.AegisLavenderSurface
import com.example.ui.theme.AegisOnPrimaryWhite
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisPurple
import com.example.ui.theme.AegisSecondaryBackground
import com.example.ui.theme.AegisSkySoft
import com.example.ui.theme.AegisTextMuted
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.AegisVoidBackground
import com.example.ui.theme.ClassicalBodyFont
import com.example.ui.theme.ClassicalDisplayFont

enum class CyberCardTone {
    SKY,
    LAVENDER,
    GOLD,
    EMERALD,
    INDIGO,
    HERO
}

/**
 * Luminous 3-color gradient atmosphere blending Sky Azure, Orchid Lavender, and Sunlit Gold.
 * Uses drawWithCache so gradient brushes are allocated only when canvas dimensions change.
 */
@Composable
fun SheildlenzBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AegisVoidBackground)
            .drawWithCache {
                val baseBgBrush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFD2E5FF), // Sky Azure Mist
                        Color(0xFFE1E8FF), // Periwinkle Blend
                        Color(0xFFECE2FF)  // Soft Orchid Lavender
                    )
                )
                val blueCenter = Offset(size.width * 0.88f, size.height * 0.08f)
                val blueRadius = (size.width * 0.72f).coerceAtLeast(1f)
                val blueAura = Brush.radialGradient(
                    colors = listOf(
                        AegisElectricBlue.copy(alpha = 0.22f),
                        Color.Transparent
                    ),
                    center = blueCenter,
                    radius = blueRadius
                )

                val purpleCenter = Offset(size.width * 0.10f, size.height * 0.42f)
                val purpleRadius = (size.width * 0.65f).coerceAtLeast(1f)
                val purpleAura = Brush.radialGradient(
                    colors = listOf(
                        AegisPurple.copy(alpha = 0.16f),
                        Color.Transparent
                    ),
                    center = purpleCenter,
                    radius = purpleRadius
                )

                val goldCenter = Offset(size.width * 0.78f, size.height * 0.88f)
                val goldRadius = (size.width * 0.58f).coerceAtLeast(1f)
                val goldAura = Brush.radialGradient(
                    colors = listOf(
                        AegisGoldBright.copy(alpha = 0.20f),
                        Color.Transparent
                    ),
                    center = goldCenter,
                    radius = goldRadius
                )

                onDrawBehind {
                    drawRect(brush = baseBgBrush)
                    drawCircle(brush = blueAura, radius = blueRadius, center = blueCenter)
                    drawCircle(brush = purpleAura, radius = purpleRadius, center = purpleCenter)
                    drawCircle(brush = goldAura, radius = goldRadius, center = goldCenter)
                }
            },
        content = content
    )
}

@Composable
fun CyberCard(
    modifier: Modifier = Modifier,
    borderColor: Color = AegisCardBorder,
    tone: CyberCardTone? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val resolvedTone = tone ?: when {
        borderColor == AegisChampagneGold || borderColor.red > 0.75f && borderColor.green > 0.45f && borderColor.blue < 0.35f -> CyberCardTone.GOLD
        borderColor == AegisPurple || borderColor == AegisLavenderBorder || (borderColor.blue > 0.80f && borderColor.red > 0.42f && borderColor.green < 0.48f) -> CyberCardTone.LAVENDER
        else -> CyberCardTone.SKY
    }

    val (bgBrush, borderBrush, stripeBrush, shadowTint) = remember(resolvedTone, borderColor) {
        val style = when (resolvedTone) {
            CyberCardTone.SKY -> Quad(
                listOf(AegisCardBackground, AegisSkySoft),
                if (borderColor == AegisCardBorder) AegisCardBorder else borderColor,
                listOf(AegisPrimaryCyan, AegisElectricBlue),
                AegisPrimaryCyan.copy(alpha = 0.16f)
            )
            CyberCardTone.LAVENDER -> Quad(
                listOf(AegisLavenderSoft, AegisLavenderSurface),
                AegisLavenderBorder,
                listOf(AegisPurple, AegisLavenderLight),
                AegisPurple.copy(alpha = 0.16f)
            )
            CyberCardTone.GOLD -> Quad(
                listOf(AegisChampagneSoft, AegisGoldDeepTint),
                AegisGoldBorder,
                listOf(AegisChampagneGold, AegisGoldBright),
                AegisChampagneGold.copy(alpha = 0.18f)
            )
            CyberCardTone.EMERALD -> Quad(
                listOf(AegisEmeraldSoft, AegisEmeraldSurface),
                AegisEmeraldBorder,
                listOf(AegisEmeraldTeal, AegisEmeraldBright),
                AegisEmeraldTeal.copy(alpha = 0.16f)
            )
            CyberCardTone.INDIGO -> Quad(
                listOf(AegisIndigoSoft, AegisIndigoSurface),
                AegisIndigoBorder,
                listOf(AegisIndigo, AegisIndigoBright),
                AegisIndigo.copy(alpha = 0.16f)
            )
            CyberCardTone.HERO -> Quad(
                listOf(
                    Color(0xFFF6FAFF),
                    Color(0xFFEDF4FF),
                    Color(0xFFF2EEFF)
                ),
                AegisCardBorder,
                listOf(AegisPrimaryCyan.copy(alpha = 0.55f), AegisPurple.copy(alpha = 0.55f)),
                AegisPrimaryCyan.copy(alpha = 0.14f)
            )
        }
        Quad(
            Brush.verticalGradient(colors = style.first),
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.94f),
                    style.second.copy(alpha = 0.65f)
                )
            ),
            Brush.horizontalGradient(colors = style.third),
            style.fourth
        )
    }

    val shape = remember { RoundedCornerShape(24.dp) }
    val cornerStart = when (resolvedTone) {
        CyberCardTone.LAVENDER -> AegisPurple
        CyberCardTone.GOLD -> AegisChampagneGold
        CyberCardTone.EMERALD -> AegisEmeraldTeal
        CyberCardTone.INDIGO -> AegisIndigo
        else -> AegisPrimaryCyan
    }
    val cornerEnd = when (resolvedTone) {
        CyberCardTone.LAVENDER -> AegisPrimaryCyan
        CyberCardTone.GOLD -> AegisGoldBright
        CyberCardTone.EMERALD -> AegisPrimaryCyan
        CyberCardTone.INDIGO -> AegisPurple
        else -> AegisPurple
    }
    val cardModifier = modifier
        .fillMaxWidth()
        .shadow(
            elevation = if (resolvedTone == CyberCardTone.HERO) 10.dp else 6.dp,
            shape = shape,
            ambientColor = shadowTint,
            spotColor = shadowTint
        )
        .clip(shape)
        .background(brush = bgBrush)
        .border(BorderStroke(1.1.dp, borderBrush), shape)
        .vipAnimatedCorners(
            cornerRadius = 24.dp,
            accentStart = cornerStart,
            accentEnd = cornerEnd,
            strokeWidth = 2.dp
        )
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)

    Column(modifier = cardModifier) {
        if (resolvedTone != CyberCardTone.HERO) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .background(brush = stripeBrush)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            content = content
        )
    }
}

private data class Quad<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

/**
 * Shared VIP Corner Animation Modifier enforced strictly across Home, Settings/Edits,
 * Live Status, and Browser surfaces. Draws smooth breathing dual-tone luminous arcs & corner glints
 * along all 4 rounded corners without obscuring content or tap targets.
 */
@Composable
fun Modifier.vipAnimatedCorners(
    cornerRadius: Dp = 22.dp,
    accentStart: Color = AegisPrimaryCyan,
    accentEnd: Color = AegisPurple,
    strokeWidth: Dp = 2.dp
): Modifier {
    val transition = rememberInfiniteTransition(label = "VipCornerMotion")
    val pulse by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "VipCornerPulse"
    )
    val sweepShift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "VipCornerSweep"
    )

    return this.drawWithContent {
        drawContent()
        val w = size.width
        val h = size.height
        val r = cornerRadius.toPx().coerceAtMost(minOf(w, h) * 0.42f)
        val arcBox = Size(r * 2f, r * 2f)
        val inset = strokeWidth.toPx() * 0.55f
        val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)

        val sweepX = w * sweepShift
        val primaryBrush = Brush.linearGradient(
            colors = listOf(
                accentStart.copy(alpha = 0.85f * pulse),
                AegisGoldBright.copy(alpha = 0.65f * pulse),
                accentEnd.copy(alpha = 0.85f * pulse)
            ),
            start = Offset(sweepX - w * 0.5f, 0f),
            end = Offset(sweepX + w * 0.5f, h)
        )

        // Top-Left corner luminous arc
        drawArc(
            brush = primaryBrush,
            startAngle = 180f,
            sweepAngle = 72f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcBox,
            style = stroke
        )
        // Top-Right corner luminous arc
        drawArc(
            brush = primaryBrush,
            startAngle = 288f,
            sweepAngle = 72f,
            useCenter = false,
            topLeft = Offset(w - r * 2f - inset, inset),
            size = arcBox,
            style = stroke
        )
        // Bottom-Right corner luminous arc
        drawArc(
            brush = primaryBrush,
            startAngle = 18f,
            sweepAngle = 72f,
            useCenter = false,
            topLeft = Offset(w - r * 2f - inset, h - r * 2f - inset),
            size = arcBox,
            style = stroke
        )
        // Bottom-Left corner luminous arc
        drawArc(
            brush = primaryBrush,
            startAngle = 108f,
            sweepAngle = 72f,
            useCenter = false,
            topLeft = Offset(inset, h - r * 2f - inset),
            size = arcBox,
            style = stroke
        )

        // Delicate sparkling pinpoints on diagonal corners
        val dotRadius = 2.1.dp.toPx() * (0.8f + 0.35f * pulse)
        drawCircle(
            color = accentStart.copy(alpha = 0.65f * pulse),
            radius = dotRadius,
            center = Offset(r * 0.36f, r * 0.36f)
        )
        drawCircle(
            color = accentEnd.copy(alpha = 0.65f * pulse),
            radius = dotRadius,
            center = Offset(w - r * 0.36f, h - r * 0.36f)
        )
    }
}

@Composable
fun LuxurySectionHeader(
    title: String,
    accentColor: Color = AegisChampagneGold,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(22.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(accentColor, accentColor.copy(alpha = 0.55f))
                    )
                )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            color = accentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = ClassicalBodyFont,
            letterSpacing = 1.2.sp
        )
    }
}

@Composable
fun MetricTile(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color = AegisPrimaryCyan,
    modifier: Modifier = Modifier,
    caption: String? = null
) {
    val tileTone = when (accentColor) {
        AegisPurple, AegisLavenderLight -> CyberCardTone.LAVENDER
        AegisChampagneGold, AegisGoldBright -> CyberCardTone.GOLD
        AegisEmeraldTeal, AegisEmeraldBright -> CyberCardTone.EMERALD
        AegisIndigo, AegisIndigoBright -> CyberCardTone.INDIGO
        else -> CyberCardTone.SKY
    }
    CyberCard(
        modifier = modifier,
        borderColor = accentColor.copy(alpha = 0.45f),
        tone = tileTone
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title.uppercase(),
                color = AegisTextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ClassicalBodyFont,
                letterSpacing = 0.9.sp
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(accentColor, accentColor.copy(alpha = 0.72f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AegisOnPrimaryWhite,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Text(
            text = value,
            color = AegisTextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = ClassicalDisplayFont,
            modifier = Modifier.padding(top = 10.dp)
        )
        if (caption != null) {
            Text(
                text = caption,
                color = AegisTextMuted,
                fontSize = 12.sp,
                fontFamily = ClassicalBodyFont,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .width(42.dp)
                .height(3.5.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(accentColor, AegisChampagneGold)
                    )
                )
        )
    }
}
