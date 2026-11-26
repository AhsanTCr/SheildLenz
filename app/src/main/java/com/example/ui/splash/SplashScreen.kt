package com.example.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.update.UpdateChecker
import com.example.ui.components.AhsanTcrBrandingPill
import com.example.ui.components.SheildlenzBackdrop
import com.example.ui.components.vipAnimatedCorners
import com.example.ui.navigation.AegisDestinations
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisElectricBlue
import com.example.ui.theme.AegisGoldBright
import com.example.ui.theme.AegisLavenderLight
import com.example.ui.theme.AegisOnPrimaryWhite
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisPurple
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.ClassicalDisplayFont
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class SplashPhase {
    SHIELD_LOADING,
    STATEMENT_REVEAL,
    NAVIGATE_OUT
}

private data class KineticWord(
    val text: String,
    val isClimax: Boolean
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SplashScreen(
    isOnboardingCompleted: Boolean,
    onNavigateNext: (destination: String) -> Unit
) {
    val context = LocalContext.current
    var phase by remember { mutableStateOf(SplashPhase.SHIELD_LOADING) }

    // 3D emblem entrance & settle
    val shieldScale = remember { Animatable(0.68f) }
    val shieldAlpha = remember { Animatable(0.86f) }
    val shieldTiltY = remember { Animatable(-24f) }
    val progressAnim = remember { Animatable(0.08f) }

    // Smooth pearl-sky reveal & kinetic statement choreography
    val veilAlpha = remember { Animatable(0f) }
    val badgeScale = remember { Animatable(0.65f) }
    val ribbonProgress = remember { Animatable(0f) }

    val lineOneWords = remember {
        listOf(
            KineticWord("You", false),
            KineticWord("think", false),
            KineticWord("you", false),
            KineticWord("can", false),
            KineticWord("bypass,", false)
        )
    }
    val lineTwoWords = remember {
        listOf(
            KineticWord("but", true),
            KineticWord("you", true),
            KineticWord("can't.", true)
        )
    }
    val allWords = remember { lineOneWords + lineTwoWords }
    val wordProgress = remember { List(allWords.size) { Animatable(0f) } }

    // Continuous VIP animation directly on the start logo itself (breath, levitation, prismatic rim & sheen)
    val infiniteTransition = rememberInfiniteTransition(label = "SplashLogoHeroMotion")
    val logoBreath by infiniteTransition.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.038f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SplashLogoBreath"
    )
    val logoFloatY by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SplashLogoFloatY"
    )
    val bladeSpread = remember { Animatable(1f) } // 1f = disassembled floating energy shards, 0f = locked crystal monogram
    val bladeEnergyCharge = remember { Animatable(0f) }

    val logoSheen by infiniteTransition.animateFloat(
        initialValue = -0.6f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SplashLogoSheen"
    )
    val energyCycle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "InternalBladeEnergyCycle"
    )
    val rimRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SplashRimRotation"
    )
    val sonarPulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SplashSonarPulse"
    )
    val badgePulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "StatementBadgePulse"
    )

    LaunchedEffect(Unit) {
        val logoStartMs = android.os.SystemClock.elapsedRealtime()
        runCatching {
            UpdateChecker.startCheckForUpdates(context, showUpToDatePopup = false)
        }

        // Step 1: Kinetic 3-Blade Disassemble -> Energy Surge -> Magnetic Reassemble Lock
        launch {
            shieldAlpha.animateTo(1f, tween(180))
        }
        launch {
            shieldScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
        launch {
            shieldTiltY.animateTo(
                targetValue = 0f,
                animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)
            )
        }
        launch {
            // Start disassembled with energy flare, assemble crisply, pulse-explode briefly, then snap into lock!
            bladeEnergyCharge.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
            bladeSpread.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.62f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            delay(480)
            bladeSpread.animateTo(0.48f, tween(340, easing = FastOutSlowInEasing))
            delay(180)
            bladeSpread.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.58f,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
        progressAnim.animateTo(
            targetValue = 0.90f,
            animationSpec = tween(durationMillis = 3300, easing = FastOutSlowInEasing)
        )

        while (true) {
            val elapsedMs = android.os.SystemClock.elapsedRealtime() - logoStartMs
            val stillChecking = UpdateChecker.isChecking.value
            if ((elapsedMs >= 3500L && !stillChecking) || elapsedMs >= 4600L) {
                break
            }
            delay(100)
        }

        progressAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
        )

        // Step 2: Smooth luminous veil + kinetic word-by-word reveal
        phase = SplashPhase.STATEMENT_REVEAL
        launch {
            veilAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing)
            )
        }
        launch {
            delay(80)
            badgeScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }

        delay(160)
        lineOneWords.indices.forEach { idx ->
            launch {
                wordProgress[idx].animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = 0.74f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
            delay(62)
        }

        delay(110)
        for (idx in lineOneWords.size until allWords.size) {
            launch {
                wordProgress[idx].animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = 0.68f,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
            delay(70)
        }

        ribbonProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
        )

        delay(820)

        phase = SplashPhase.NAVIGATE_OUT
        if (isOnboardingCompleted) {
            onNavigateNext(AegisDestinations.HOME)
        } else {
            onNavigateNext(AegisDestinations.ONBOARDING)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("splash_screen_root")
    ) {
        // ─── 1. VIP Animated Start Logo Stage ───
        SheildlenzBackdrop(modifier = Modifier.fillMaxSize()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp)
            ) {
                Box(
                    modifier = Modifier.size(228.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Subtle soft halo pulse behind the animated logo
                    Canvas(modifier = Modifier.size(228.dp)) {
                        val c = Offset(size.width / 2f, size.height / 2f)
                        val maxR = size.minDimension * 0.47f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    AegisElectricBlue.copy(alpha = 0.16f + 0.08f * (1f - sonarPulse)),
                                    AegisPurple.copy(alpha = 0.09f),
                                    Color.Transparent
                                ),
                                center = c,
                                radius = maxR
                            ),
                            radius = maxR,
                            center = c
                        )
                        val pulseR = maxR * (0.68f + 0.30f * sonarPulse)
                        drawCircle(
                            color = AegisPrimaryCyan.copy(alpha = (1f - sonarPulse) * 0.36f),
                            radius = pulseR,
                            center = c,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }

                    // Rotating luminous prismatic bezel + animated 3D centered logo itself
                    Box(
                        modifier = Modifier
                            .size(164.dp)
                            .graphicsLayer {
                                val combinedScale = shieldScale.value * logoBreath
                                scaleX = combinedScale
                                scaleY = combinedScale
                                translationY = logoFloatY.dp.toPx()
                                rotationY = shieldTiltY.value
                                cameraDistance = 14f * density
                                alpha = shieldAlpha.value
                            }
                            .testTag("splash_animated_logo"),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val c = Offset(size.width / 2f, size.height / 2f)
                            val rimR = (size.minDimension / 2f) - 2.dp.toPx()
                            rotate(degrees = rimRotation, pivot = c) {
                                drawArc(
                                    brush = Brush.sweepGradient(
                                        colors = listOf(
                                            AegisElectricBlue,
                                            AegisPrimaryCyan,
                                            Color.White,
                                            AegisPurple,
                                            AegisGoldBright,
                                            AegisElectricBlue
                                        ),
                                        center = c
                                    ),
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    topLeft = Offset(c.x - rimR, c.y - rimR),
                                    size = Size(rimR * 2f, rimR * 2f),
                                    style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(152.dp)
                                .shadow(
                                    elevation = 16.dp,
                                    shape = RoundedCornerShape(36.dp),
                                    ambientColor = AegisElectricBlue.copy(alpha = 0.42f),
                                    spotColor = AegisPurple.copy(alpha = 0.36f)
                                )
                                .clip(RoundedCornerShape(36.dp))
                                .border(
                                    width = 1.5.dp,
                                    color = Color.White.copy(alpha = 0.72f),
                                    shape = RoundedCornerShape(36.dp)
                                )
                                .vipAnimatedCorners(
                                    cornerRadius = 36.dp,
                                    accentStart = AegisPrimaryCyan,
                                    accentEnd = AegisPurple,
                                    strokeWidth = 2.4.dp
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            AnimatedSheildlenzMonogram(
                                bladeSpread = bladeSpread.value,
                                energyCharge = bladeEnergyCharge.value,
                                energyPulse = energyCycle,
                                sheenProgress = logoSheen,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                Text(
                    text = "SHEILDLENZ",
                    color = AegisTextPrimary,
                    fontSize = 31.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.2.sp,
                    fontFamily = ClassicalDisplayFont
                )

                Spacer(modifier = Modifier.height(26.dp))

                // Sleek glowing energetic loading bar (zero noisy status clutter)
                Box(
                    modifier = Modifier
                        .width(204.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(AegisPrimaryCyan.copy(alpha = 0.14f))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val fillW = (size.width * progressAnim.value.coerceIn(0f, 1f))
                        if (fillW > 0f) {
                            drawRoundRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(AegisElectricBlue, AegisPrimaryCyan, AegisPurple)
                                ),
                                topLeft = Offset.Zero,
                                size = Size(fillW, size.height),
                                cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 18.dp)
            ) {
                AhsanTcrBrandingPill()
            }
        }

        // ─── 2. Radiant Luminous Veil + Kinetic Statement Reveal ───
        if (phase != SplashPhase.SHIELD_LOADING) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(veilAlpha.value)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFFFFFF),
                                Color(0xFFF1F7FF),
                                Color(0xFFF5F0FF)
                            )
                        )
                    )
                    .drawBehind {
                        // Soft Sky-Azure + Orchid ambient aura behind typography
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    AegisElectricBlue.copy(alpha = 0.14f + 0.06f * badgePulse),
                                    AegisPurple.copy(alpha = 0.08f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.5f, size.height * 0.46f),
                                radius = size.width * 0.72f
                            ),
                            radius = size.width * 0.72f,
                            center = Offset(size.width * 0.5f, size.height * 0.46f)
                        )
                    }
                    .padding(horizontal = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Luminous Royal Sky-Orchid emblem crest (zero dark colors)
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .graphicsLayer {
                                val pulseScale = badgeScale.value * (1f + 0.04f * badgePulse)
                                scaleX = pulseScale
                                scaleY = pulseScale
                            }
                            .shadow(
                                elevation = 12.dp,
                                shape = CircleShape,
                                ambientColor = AegisPrimaryCyan.copy(alpha = 0.30f),
                                spotColor = AegisPurple.copy(alpha = 0.30f)
                            )
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        AegisElectricBlue,
                                        AegisPrimaryCyan,
                                        AegisPurple
                                    )
                                )
                            )
                            .border(
                                width = 2.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(Color.White.copy(alpha = 0.9f), AegisGoldBright)
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Sheildlenz Protected",
                            tint = AegisOnPrimaryWhite,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Line 1: Kinetic rising spring words ("You think you can bypass,")
                    FlowRow(
                        horizontalArrangement = Arrangement.Center,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        lineOneWords.forEachIndexed { idx, word ->
                            val p = wordProgress[idx].value
                            Text(
                                text = if (idx < lineOneWords.lastIndex) "${word.text} " else word.text,
                                color = AegisTextPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = ClassicalDisplayFont,
                                textAlign = TextAlign.Center,
                                letterSpacing = 0.3.sp,
                                modifier = Modifier.graphicsLayer {
                                    alpha = p.coerceIn(0f, 1f)
                                    translationY = (1f - p) * 20.dp.toPx()
                                    val s = 0.88f + 0.12f * p
                                    scaleX = s
                                    scaleY = s
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Line 2: Climax kinetic phrase ("but you can't.")
                    FlowRow(
                        horizontalArrangement = Arrangement.Center,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        lineTwoWords.forEachIndexed { localIdx, word ->
                            val idx = lineOneWords.size + localIdx
                            val p = wordProgress[idx].value
                            Text(
                                text = if (localIdx < lineTwoWords.lastIndex) "${word.text} " else word.text,
                                color = if (localIdx == lineTwoWords.lastIndex) AegisPrimaryCyan else AegisPurple,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = ClassicalDisplayFont,
                                textAlign = TextAlign.Center,
                                letterSpacing = 0.4.sp,
                                modifier = Modifier.graphicsLayer {
                                    alpha = p.coerceIn(0f, 1f)
                                    translationY = (1f - p) * 24.dp.toPx()
                                    val s = 0.85f + 0.15f * p
                                    scaleX = s
                                    scaleY = s
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // Expanding luminous Sky-Gold ribbon sweep
                    Box(
                        modifier = Modifier
                            .width((124f * ribbonProgress.value.coerceIn(0f, 1f)).dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        AegisElectricBlue,
                                        AegisPurple,
                                        AegisChampagneGold
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}

/**
 * Vectorized 3-blade brushed chrome monogram & glowing cyan diamond core matching the
 * Sheildlenz emblem, featuring real independent disassemble/assemble translation,
 * energy arcs along blade seams, and magnetic plasma core lock-in.
 */
@Composable
private fun AnimatedSheildlenzMonogram(
    bladeSpread: Float,
    energyCharge: Float,
    energyPulse: Float,
    sheenProgress: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val c = Offset(w * 0.5f, h * 0.5f)

        // 1. Deep Midnight Navy metallic surface backdrop
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF14244B),
                    Color(0xFF0A142B)
                ),
                center = c,
                radius = w * 0.72f
            )
        )

        // Core plasma bloom pulsing from the central diamond gap
        val plasmaRadius = w * (0.28f + 0.14f * bladeSpread + 0.06f * kotlin.math.sin(energyPulse * Math.PI).toFloat())
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF00E5FF).copy(alpha = 0.52f * (0.65f + 0.35f * energyCharge)),
                    Color(0xFF38BDF8).copy(alpha = 0.20f),
                    Color.Transparent
                ),
                center = c,
                radius = plasmaRadius
            ),
            radius = plasmaRadius,
            center = c
        )

        val bladeMetallic = Brush.linearGradient(
            colors = listOf(
                Color(0xFFFFFFFF),
                Color(0xFFD6E0EF),
                Color(0xFF94A3B8),
                Color(0xFFE2E8F0)
            ),
            start = Offset(w * (0.15f + sheenProgress * 0.3f), 0f),
            end = Offset(w * 0.85f, h)
        )
        val bladeEdgeCyan = Color(0xFF38BDF8).copy(alpha = (0.35f + 0.65f * bladeSpread).coerceIn(0f, 1f))
        val spreadPx = w * 0.125f * bladeSpread

        // 2. Left Top-Slanted Metallic Blade (slides upper-left when disassembled)
        translate(left = -spreadPx * 1.1f, top = -spreadPx * 0.9f) {
            val leftBlade = Path().apply {
                moveTo(w * 0.49f, h * 0.15f)
                lineTo(w * 0.59f, h * 0.15f)
                lineTo(w * 0.34f, h * 0.41f)
                lineTo(w * 0.34f, h * 0.61f)
                lineTo(w * 0.25f, h * 0.67f)
                lineTo(w * 0.25f, h * 0.38f)
                close()
            }
            drawPath(leftBlade, brush = bladeMetallic)
            drawPath(
                leftBlade,
                color = bladeEdgeCyan,
                style = Stroke(width = 1.6.dp.toPx(), join = StrokeJoin.Round)
            )
        }

        // 3. Center Diagonal Blade Pair (splits vertically + horizontally around the Cyan Core)
        // Upper-center ribbon arm
        translate(left = spreadPx * 0.55f, top = -spreadPx * 1.15f) {
            val centerTopBlade = Path().apply {
                moveTo(w * 0.69f, h * 0.15f)
                lineTo(w * 0.82f, h * 0.15f)
                lineTo(w * 0.54f, h * 0.44f)
                lineTo(w * 0.40f, h * 0.44f)
                close()
            }
            drawPath(centerTopBlade, brush = bladeMetallic)
            drawPath(
                centerTopBlade,
                color = bladeEdgeCyan,
                style = Stroke(width = 1.6.dp.toPx(), join = StrokeJoin.Round)
            )
        }
        // Lower-center ribbon arm
        translate(left = -spreadPx * 0.55f, top = spreadPx * 1.15f) {
            val centerBottomBlade = Path().apply {
                moveTo(w * 0.46f, h * 0.56f)
                lineTo(w * 0.60f, h * 0.56f)
                lineTo(w * 0.31f, h * 0.85f)
                lineTo(w * 0.18f, h * 0.85f)
                close()
            }
            drawPath(centerBottomBlade, brush = bladeMetallic)
            drawPath(
                centerBottomBlade,
                color = bladeEdgeCyan,
                style = Stroke(width = 1.6.dp.toPx(), join = StrokeJoin.Round)
            )
        }

        // 4. Right Bottom-Slanted Metallic Blade (slides lower-right when disassembled)
        translate(left = spreadPx * 1.1f, top = spreadPx * 0.9f) {
            val rightBlade = Path().apply {
                moveTo(w * 0.75f, h * 0.33f)
                lineTo(w * 0.75f, h * 0.62f)
                lineTo(w * 0.51f, h * 0.85f)
                lineTo(w * 0.41f, h * 0.85f)
                lineTo(w * 0.66f, h * 0.59f)
                lineTo(w * 0.66f, h * 0.39f)
                close()
            }
            drawPath(rightBlade, brush = bladeMetallic)
            drawPath(
                rightBlade,
                color = bladeEdgeCyan,
                style = Stroke(width = 1.6.dp.toPx(), join = StrokeJoin.Round)
            )
        }

        // 5. Internal Electric Energy Diamond Core (rotates/pulses dynamically when blades reassemble)
        val coreExpand = 1f + 0.24f * bladeSpread + 0.08f * kotlin.math.sin(energyPulse * Math.PI * 2).toFloat()
        val diamondPath = Path().apply {
            moveTo(c.x - w * 0.12f * coreExpand, c.y - h * 0.055f * coreExpand)
            lineTo(c.x + w * 0.02f * coreExpand, c.y - h * 0.055f * coreExpand)
            lineTo(c.x + w * 0.12f * coreExpand, c.y + h * 0.055f * coreExpand)
            lineTo(c.x - w * 0.02f * coreExpand, c.y + h * 0.055f * coreExpand)
            close()
        }
        drawPath(
            path = diamondPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF7DF9FF),
                    Color(0xFF00E5FF),
                    Color(0xFF0284C7)
                ),
                start = Offset(c.x - w * 0.12f, c.y - h * 0.06f),
                end = Offset(c.x + w * 0.12f, c.y + h * 0.06f)
            )
        )
        drawPath(
            path = diamondPath,
            color = Color.White.copy(alpha = 0.85f),
            style = Stroke(width = 1.4.dp.toPx(), join = StrokeJoin.Round)
        )

        // Energy Conduit Discharge Arc when Disassembled / Locking
        if (bladeSpread > 0.04f) {
            drawCircle(
                color = Color(0xFF7DF9FF).copy(alpha = bladeSpread * 0.65f),
                radius = w * (0.20f + 0.18f * (1f - bladeSpread)),
                center = c,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}
