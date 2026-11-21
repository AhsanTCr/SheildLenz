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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AegisShieldGraphic
import com.example.ui.components.AhsanTcrBrandingPill
import com.example.ui.components.SheildlenzBackdrop
import com.example.ui.components.drawVolumetric3DSphere
import com.example.ui.components.projectOrbitPoint3D
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
    var phase by remember { mutableStateOf(SplashPhase.SHIELD_LOADING) }

    // Instant energetic shield entrance (no dead fade wait)
    val shieldScale = remember { Animatable(0.84f) }
    val shieldAlpha = remember { Animatable(0.82f) }
    val progressAnim = remember { Animatable(0.06f) }

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

    // Continuous lively orbital & pulse motion from frame 1
    val infiniteTransition = rememberInfiniteTransition(label = "SplashEnergeticMotion")
    val sonarWave1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SplashSonar1"
    )
    val sonarWave2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1550, delayMillis = 520, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SplashSonar2"
    )
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SplashRingRotation"
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
        // Step 1: Parallel spring-pop emblem + smooth energetic progress fill
        launch {
            shieldAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing)
            )
        }
        launch {
            shieldScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
        progressAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1050, easing = FastOutSlowInEasing)
        )

        delay(120)

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
        // Staggered Phrase 1 ("You think you can bypass,")
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
        // Staggered Climax Phrase 2 ("but you can't.")
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

        // Sweep luminous light ribbon beneath climax
        ribbonProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
        )

        // Clean comfortable hold
        delay(820)

        // Step 3: Navigate directly (no PIN gate on launch)
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
        // ─── 1. Energetic Luminous Shield Boot Stage ───
        SheildlenzBackdrop(modifier = Modifier.fillMaxSize()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(236.dp)
                        .graphicsLayer {
                            scaleX = shieldScale.value
                            scaleY = shieldScale.value
                            alpha = shieldAlpha.value
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(236.dp)) {
                        val c = Offset(size.width / 2f, size.height / 2f)
                        val maxR = size.minDimension * 0.48f

                        // Rapid concentric sonar pulses
                        listOf(
                            sonarWave1 to AegisElectricBlue,
                            sonarWave2 to AegisPurple
                        ).forEach { (wave, color) ->
                            val r = maxR * (0.54f + 0.45f * wave)
                            val a = (1f - wave) * 0.48f
                            drawCircle(
                                color = color.copy(alpha = a),
                                radius = r,
                                center = c,
                                style = Stroke(width = 2.6.dp.toPx() * (1f - wave * 0.4f))
                            )
                        }

                        // Energetic sweep ring
                        val arcR = maxR * 0.87f
                        rotate(degrees = ringRotation, pivot = c) {
                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        AegisElectricBlue.copy(alpha = 0.45f),
                                        AegisPrimaryCyan.copy(alpha = 0.92f),
                                        Color.White.copy(alpha = 0.85f),
                                        Color.Transparent
                                    ),
                                    center = c
                                ),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = Offset(c.x - arcR, c.y - arcR),
                                size = Size(arcR * 2f, arcR * 2f),
                                style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        // Tilted 3D orbiting spheres around emblem
                        val rx = maxR * 0.96f
                        val ry = maxR * 0.56f
                        val zAmp = maxR * 0.72f
                        val camDist = size.width * 2.1f
                        for (i in 0..2) {
                            val pt = projectOrbitPoint3D(
                                center = c,
                                radiusX = rx,
                                radiusY = ry,
                                zAmplitude = zAmp,
                                angleDeg = ringRotation + i * 120f,
                                tiltDeg = -18f,
                                cameraDistance = camDist
                            )
                            val orbHue = lerp(
                                if (i % 2 == 0) AegisElectricBlue else AegisPurple,
                                Color.White,
                                0.22f
                            )
                            drawVolumetric3DSphere(
                                center = pt.offset,
                                radius = 4.5.dp.toPx() * pt.perspectiveScale,
                                baseColor = orbHue,
                                alpha = (0.68f + 0.32f * ((pt.zNormalized + 1f) * 0.5f)).coerceIn(0.6f, 1f),
                                depthNormalized = pt.zNormalized,
                                drawHaloAndShadow = true
                            )
                        }
                    }

                    AegisShieldGraphic(
                        isProtected = true,
                        isThreatActive = false,
                        size = 184.dp
                    )
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
