package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisGoldBright
import com.example.ui.theme.AegisLavenderSoft
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisPurple
import com.example.ui.theme.AegisTextMuted
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.ClassicalBodyFont

/**
 * Unified calm, animated circular countdown ring in Luminous Sky, Lavender & Gold
 * (or slate neutral when [discreetMode] is active).
 */
@Composable
fun AnimatedCountdownRing(
    progress: Float,
    centerText: String,
    modifier: Modifier = Modifier,
    subLabel: String = "",
    primaryColor: Color = AegisPrimaryCyan,
    secondaryColor: Color = AegisPurple,
    accentColor: Color = AegisGoldBright,
    ringSize: Dp = 148.dp,
    strokeWidth: Dp = 9.dp,
    centerFontSize: TextUnit = 24.sp,
    discreetMode: Boolean = false,
    centerIconContent: (@Composable () -> Unit)? = null
) {
    val clampedProgress = progress.coerceIn(0.03f, 1f)
    val animatedSweep by animateFloatAsState(
        targetValue = 360f * clampedProgress,
        animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
        label = "CountdownRingSweep"
    )
    val transition = rememberInfiniteTransition(label = "CountdownRingBreath")
    val haloBreath by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CountdownRingHaloBreath"
    )

    val resolvedPrimary = if (discreetMode) Color(0xFF334155) else primaryColor
    val resolvedSecondary = if (discreetMode) Color(0xFF64748B) else secondaryColor
    val resolvedAccent = if (discreetMode) Color(0xFF94A3B8) else accentColor
    val trackColor = if (discreetMode) Color(0xFFD8E0EA) else AegisCardBorder.copy(alpha = 0.36f)
    val centerInk = if (discreetMode) Color(0xFF1E293B) else AegisTextPrimary
    val subInk = if (discreetMode) Color(0xFF64748B) else AegisTextSecondary

    Box(
        modifier = modifier.size(ringSize),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(ringSize)) {
            val strokePx = strokeWidth.toPx()
            val inset = strokePx / 2f + 4.dp.toPx()
            val arcDiameter = (size.minDimension - inset * 2f).coerceAtLeast(10f)
            val topLeft = Offset((size.width - arcDiameter) / 2f, (size.height - arcDiameter) / 2f)
            val arcSize = Size(arcDiameter, arcDiameter)
            val center = Offset(size.width / 2f, size.height / 2f)

            if (!discreetMode) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            resolvedPrimary.copy(alpha = 0.14f * haloBreath),
                            resolvedSecondary.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension * 0.5f
                    ),
                    radius = size.minDimension * 0.5f,
                    center = center
                )
            }

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        resolvedPrimary,
                        resolvedSecondary,
                        resolvedAccent,
                        resolvedPrimary
                    ),
                    center = center
                ),
                startAngle = -90f,
                sweepAngle = animatedSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        Surface(
            shape = CircleShape,
            color = if (discreetMode) Color.White else Color.White.copy(alpha = 0.88f),
            border = BorderStroke(
                width = 1.dp,
                color = if (discreetMode) Color(0xFFE2E8F0) else resolvedPrimary.copy(alpha = 0.16f)
            ),
            modifier = Modifier.size(ringSize - strokeWidth * 2 - 18.dp)
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (centerIconContent != null) {
                    centerIconContent()
                    if (centerText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }
                if (centerText.isNotBlank()) {
                    Text(
                        text = centerText,
                        color = centerInk,
                        fontSize = centerFontSize,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (subLabel.isNotBlank()) {
                    Text(
                        text = subLabel,
                        color = subInk,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = ClassicalBodyFont,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Compact tap-to-expand details chip that keeps block screens and policy cards serene by
 * tucking verbose rule explanations, timestamps, and package details behind one tap.
 */
@Composable
fun ExpandableDetailsToggle(
    modifier: Modifier = Modifier,
    summaryLabel: String = "Details",
    accentColor: Color = AegisPurple,
    discreetMode: Boolean = false,
    initiallyExpanded: Boolean = false,
    testTagPrefix: String = "expandable_details",
    detailsContent: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val resolvedAccent = if (discreetMode) Color(0xFF475569) else accentColor
    val pillBg = if (discreetMode) Color.White.copy(alpha = 0.9f) else AegisLavenderSoft.copy(alpha = 0.78f)
    val pillBorder = if (discreetMode) Color(0xFFD8E0EA) else resolvedAccent.copy(alpha = 0.30f)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = pillBg,
            border = BorderStroke(1.dp, pillBorder),
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(50))
                .clickable(
                    role = Role.Button,
                    onClickLabel = if (expanded) "Hide details" else "Show details"
                ) { expanded = !expanded }
                .testTag("${testTagPrefix}_toggle")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = summaryLabel,
                    color = resolvedAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalBodyFont
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse $summaryLabel" else "Expand $summaryLabel",
                    tint = resolvedAccent,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(180)) + expandVertically(tween(220)),
            exit = fadeOut(tween(140)) + shrinkVertically(tween(180))
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (discreetMode) Color.White else Color.White.copy(alpha = 0.86f),
                border = BorderStroke(
                    1.dp,
                    if (discreetMode) Color(0xFFD8E0EA) else AegisChampagneGold.copy(alpha = 0.32f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .testTag("${testTagPrefix}_content")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    content = detailsContent
                )
            }
        }
    }
}
