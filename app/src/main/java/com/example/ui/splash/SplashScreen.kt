package com.example.ui.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AegisShieldGraphic
import com.example.ui.components.AhsanTcrBrandingPill
import com.example.ui.components.SheildlenzBackdrop
import com.example.ui.navigation.AegisDestinations
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisElectricBlue
import com.example.ui.theme.AegisLavenderBorder
import com.example.ui.theme.AegisLavenderSoft
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisPurple
import com.example.ui.theme.AegisTextMuted
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.ClassicalBodyFont
import com.example.ui.theme.ClassicalDisplayFont
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

private enum class SplashPhase {
    SHIELD_LOADING,
    WHITE_FLASH,
    TYPEWRITER_MESSAGE,
    NAVIGATE_OUT
}

@Composable
fun SplashScreen(
    isOnboardingCompleted: Boolean,
    onNavigateNext: (destination: String) -> Unit
) {
    var phase by remember { mutableStateOf(SplashPhase.SHIELD_LOADING) }

    // Big shield animation controls
    val shieldScale = remember { Animatable(0.72f) }
    val shieldAlpha = remember { Animatable(0f) }
    var initStatus by remember { mutableStateOf("Initializing Sheildlenz Security Core...") }
    var progress by remember { mutableFloatStateOf(0.15f) }

    // White flash transition opacity
    val whiteFlashAlpha = remember { Animatable(0f) }

    // Typewriter state
    val fullSentence = "You can think you can bypass, but you can't."
    var typedText by remember { mutableStateOf("") }
    var showTypewriterCursor by remember { mutableStateOf(true) }
    var showSubtitle by remember { mutableStateOf(false) }

    // Continuous orbital halo animations for the big shield stage
    val infiniteTransition = rememberInfiniteTransition(label = "SplashInfinite")
    val sonarWave1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SonarWave1"
    )
    val sonarWave2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, delayMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SonarWave2"
    )
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RingRotation"
    )
    val cursorBlink by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 480, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CursorBlink"
    )

    LaunchedEffect(Unit) {
        // Step 1: Big shield entrance
        shieldScale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing)
        )
        shieldAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 450)
        )

        delay(320)
        initStatus = "Verifying On-Device Vision Classifier..."
        progress = 0.50f

        delay(340)
        initStatus = "Arming Zero-Bypass Guardian Shield..."
        progress = 0.85f

        delay(340)
        initStatus = "Sheildlenz Security Active • Ready"
        progress = 1.0f

        delay(350)

        // Step 2: Smooth transition into bright luminous white screen
        phase = SplashPhase.WHITE_FLASH
        whiteFlashAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
        )

        // Step 3: Typewriter animation on the luminous white canvas
        phase = SplashPhase.TYPEWRITER_MESSAGE
        for (i in 1..fullSentence.length) {
            typedText = fullSentence.substring(0, i)
            delay(36)
        }
        delay(120)
        showSubtitle = true

        // Keep reading pause
        delay(950)

        // Step 4: Direct navigation into Home (NO password prompt on launch!)
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
        // ─── 1. Underlying Dark Luxury Cosmic Shield Layer ───
        SheildlenzBackdrop(modifier = Modifier.fillMaxSize()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
            ) {
                // Large living animated shield stage with celestial sweep rings
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .scale(shieldScale.value)
                        .alpha(shieldAlpha.value),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(240.dp)) {
                        val c = Offset(size.width / 2f, size.height / 2f)
                        val maxR = size.minDimension * 0.48f

                        // Pulsing outer sonar waves
                        listOf(sonarWave1 to AegisElectricBlue, sonarWave2 to AegisPurple).forEach { (wave, color) ->
                            val r = maxR * (0.50f + 0.48f * wave)
                            val a = (1f - wave) * 0.42f
                            drawCircle(
                                color = color.copy(alpha = a),
                                radius = r,
                                center = c,
                                style = Stroke(width = 2.8.dp.toPx() * (1f - wave * 0.45f))
                            )
                        }

                        // Subtle dashed orbital track
                        val trackR = maxR * 0.90f
                        drawCircle(
                            color = AegisLavenderBorder.copy(alpha = 0.35f),
                            radius = trackR,
                            center = c,
                            style = Stroke(
                                width = 1.4.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 12f), 0f)
                            )
                        )

                        // Rotating orbital sweep glow ring
                        val arcR = maxR * 0.86f
                        val arcTopLeft = Offset(c.x - arcR, c.y - arcR)
                        val arcSize = Size(arcR * 2f, arcR * 2f)
                        rotate(degrees = ringRotation, pivot = c) {
                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        AegisElectricBlue.copy(alpha = 0.40f),
                                        AegisPrimaryCyan.copy(alpha = 0.90f),
                                        Color.Transparent
                                    ),
                                    center = c
                                ),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = arcTopLeft,
                                size = arcSize,
                                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        // Floating orbital light spark
                        for (i in 0..2) {
                            val angleRad = ((ringRotation + i * 120f) * Math.PI / 180.0)
                            val px = c.x + (trackR * cos(angleRad)).toFloat()
                            val py = c.y + (trackR * sin(angleRad)).toFloat()
                            drawCircle(
                                color = AegisElectricBlue.copy(alpha = 0.90f),
                                radius = 3.2.dp.toPx(),
                                center = Offset(px, py)
                            )
                        }
                    }

                    AegisShieldGraphic(
                        isProtected = true,
                        isThreatActive = false,
                        size = 190.dp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "SHEILDLENZ",
                    color = AegisTextPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                    fontFamily = ClassicalDisplayFont
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .width(76.dp)
                        .height(2.5.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, AegisChampagneGold, Color.Transparent)
                            )
                        )
                )

                Text(
                    text = "ON-DEVICE SCREEN & NETWORK GUARDIAN",
                    color = AegisPrimaryCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.0.sp,
                    fontFamily = ClassicalBodyFont,
                    modifier = Modifier.padding(top = 10.dp)
                )

                Spacer(modifier = Modifier.height(38.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .width(220.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = AegisElectricBlue,
                    trackColor = AegisPrimaryCyan.copy(alpha = 0.16f),
                    strokeCap = StrokeCap.Round
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = initStatus,
                    color = AegisTextMuted,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            ) {
                AhsanTcrBrandingPill()
            }
        }

        // ─── 2. Luminous White Canvas with Kinetic Typewriter Reveal ───
        if (phase != SplashPhase.SHIELD_LOADING) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(whiteFlashAlpha.value)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFFFFFF),
                                Color(0xFFF9FBFF),
                                Color(0xFFF2F6FE)
                            )
                        )
                    )
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Small elegant shield emblem above the typewriter statement
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF0F172A),
                                        Color(0xFF1E293B)
                                    )
                                )
                            )
                            .border(
                                width = 1.5.dp,
                                color = AegisChampagneGold.copy(alpha = 0.85f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Sheildlenz Armed",
                            tint = AegisChampagneGold,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Typewriter Main Statement
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = typedText,
                                color = Color(0xFF0B132B),
                                fontSize = 23.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = ClassicalDisplayFont,
                                letterSpacing = 0.5.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 32.sp
                            )
                            // Animated blinking typewriter cursor
                            if (phase == SplashPhase.TYPEWRITER_MESSAGE) {
                                Box(
                                    modifier = Modifier
                                        .padding(start = 2.dp)
                                        .width(3.dp)
                                        .height(24.dp)
                                        .alpha(if (cursorBlink > 0.5f) 1f else 0f)
                                        .background(Color(0xFF0B132B))
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // Golden divider accent line
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        AegisChampagneGold,
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Subtitle & reassurance tags
                    AnimatedVisibility(
                        visible = showSubtitle,
                        enter = fadeIn(tween(350))
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "SHEILDLENZ ON-DEVICE GUARDIAN",
                                color = Color(0xFF1E3A8A),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.2.sp,
                                fontFamily = ClassicalBodyFont
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "ZERO-BYPASS • FULL ON-DEVICE PRIVACY",
                                color = Color(0xFF64748B),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.4.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Bottom Branding Pill on White Canvas
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp)
                ) {
                    AhsanTcrBrandingPill()
                }
            }
        }
    }
}
