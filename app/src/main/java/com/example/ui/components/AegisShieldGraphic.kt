package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisCyanGlow
import com.example.ui.theme.AegisDangerRed
import com.example.ui.theme.AegisElectricBlue
import com.example.ui.theme.AegisGoldBright
import com.example.ui.theme.AegisLavenderLight
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisPurple
import com.example.ui.theme.AegisRedGlow
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val DeepRoyalShadow = Color(0xFF152A5E)

/**
 * Projects a point on a tilted 3D circular/elliptical orbit into 2D screen coordinates + depth Z.
 *
 * @param center Orbit pivot in Canvas pixels.
 * @param radiusX Horizontal semi-axis in pixels.
 * @param radiusY Foreshortened vertical semi-axis in pixels (smaller = steeper 3D tilt).
 * @param zAmplitude Depth reach in pixels along the Z axis (+Z is toward viewer, -Z is away).
 * @param angleDeg Orbital angle in degrees.
 * @param tiltDeg Screen-plane roll of the orbital plane in degrees.
 * @param cameraDistance Perspective focal distance in pixels.
 */
data class ProjectedOrbitPoint(
    val offset: Offset,
    val zNormalized: Float, // -1f (farthest back) .. +1f (closest front)
    val perspectiveScale: Float
)

fun projectOrbitPoint3D(
    center: Offset,
    radiusX: Float,
    radiusY: Float,
    zAmplitude: Float,
    angleDeg: Float,
    tiltDeg: Float,
    cameraDistance: Float
): ProjectedOrbitPoint {
    val rad = angleDeg * (PI / 180.0)
    val cosA = cos(rad).toFloat()
    val sinA = sin(rad).toFloat()

    // Local orbital coordinates before roll
    val lx = radiusX * cosA
    val ly = radiusY * sinA
    val lz = zAmplitude * sinA

    // Apply perspective foreshortening so foreground orbs swell and background orbs recede
    val safeDenom = (cameraDistance - lz).coerceAtLeast(cameraDistance * 0.35f)
    val persp = cameraDistance / safeDenom

    // Rotate (lx, ly) by orbital plane roll angle
    val tiltRad = tiltDeg * (PI / 180.0)
    val cosT = cos(tiltRad).toFloat()
    val sinT = sin(tiltRad).toFloat()
    val rx = (lx * cosT - ly * sinT) * persp
    val ry = (lx * sinT + ly * cosT) * persp

    val zNorm = if (zAmplitude > 0f) (lz / zAmplitude).coerceIn(-1f, 1f) else 0f
    return ProjectedOrbitPoint(
        offset = Offset(center.x + rx, center.y + ry),
        zNormalized = zNorm,
        perspectiveScale = persp
    )
}

/**
 * Renders a volumetric 3D glossy sphere with soft cast depth shadow, luminous halo,
 * multi-stop spherical shading (upper-left key light -> core hue -> deep terminator -> rim bounce),
 * and crisp specular catchlight.
 */
fun DrawScope.drawVolumetric3DSphere(
    center: Offset,
    radius: Float,
    baseColor: Color,
    alpha: Float = 1f,
    depthNormalized: Float = 0f,
    drawHaloAndShadow: Boolean = true
) {
    if (radius <= 0.4f || alpha <= 0.01f) return
    val clampedAlpha = alpha.coerceIn(0f, 1f)
    val frontBoost = ((depthNormalized + 1f) * 0.5f).coerceIn(0f, 1f)

    if (drawHaloAndShadow) {
        // Soft colored ambient glow around foreground/active spheres
        val haloRadius = radius * (2.25f + 0.35f * frontBoost)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    baseColor.copy(alpha = (0.28f + 0.18f * frontBoost) * clampedAlpha),
                    baseColor.copy(alpha = 0.07f * clampedAlpha),
                    Color.Transparent
                ),
                center = center,
                radius = haloRadius
            ),
            radius = haloRadius,
            center = center
        )

        // Subtle cool drop shadow anchored beneath-right to heighten floating 3D separation
        if (depthNormalized > -0.25f) {
            val shadowOffset = Offset(radius * 0.22f, radius * 0.34f)
            val shadowAlpha = (0.14f + 0.16f * frontBoost) * clampedAlpha
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        DeepRoyalShadow.copy(alpha = shadowAlpha),
                        Color.Transparent
                    ),
                    center = center + shadowOffset,
                    radius = radius * 1.18f
                ),
                radius = radius * 1.18f,
                center = center + shadowOffset
            )
        }
    }

    // Off-center spherical shading from upper-left key light while preserving bright base hue
    val highlightCenter = Offset(
        x = center.x - radius * 0.36f,
        y = center.y - radius * 0.36f
    )
    val brightSpec = lerp(baseColor, Color.White, 0.92f).copy(alpha = clampedAlpha)
    val litTone = lerp(baseColor, Color.White, 0.48f).copy(alpha = clampedAlpha)
    val coreTone = baseColor.copy(alpha = clampedAlpha)
    val terminatorShadow = lerp(baseColor, DeepRoyalShadow, 0.32f).copy(alpha = clampedAlpha)
    val rimBounce = lerp(baseColor, Color.White, 0.28f).copy(alpha = clampedAlpha)

    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0.00f to brightSpec,
                0.24f to litTone,
                0.56f to coreTone,
                0.86f to terminatorShadow,
                1.00f to rimBounce
            ),
            center = highlightCenter,
            radius = radius * 1.48f
        ),
        radius = radius,
        center = center
    )

    // Delicate top-left glass specular glint
    val glintCenter = Offset(center.x - radius * 0.33f, center.y - radius * 0.34f)
    val glintRadius = radius * 0.34f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.96f * clampedAlpha),
                Color.White.copy(alpha = 0.40f * clampedAlpha),
                Color.Transparent
            ),
            center = glintCenter,
            radius = glintRadius
        ),
        radius = glintRadius,
        center = glintCenter
    )
}

/**
 * Pure VIP layered crystal shield crest with continuous smooth levitation,
 * rotating aurora sweep halo, 3D inclined orbiting spheres with Z-occlusion,
 * specular glass sheen, and spring-animated shockwave activation.
 */
@Composable
fun AegisShieldGraphic(
    isProtected: Boolean,
    isThreatActive: Boolean = false,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "VipCrystalShieldMotion")

    // Smooth continuous aurora sweep rotation (360 deg in 5.2s)
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "AuroraSweepAngle"
    )

    // Counter-rotating 3D orbital sphere phase
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbitConstellationAngle"
    )

    // Smooth breathing levitation & glow pulse
    val breathPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreathPhase"
    )

    // Specular glass sheen glide across crystal face
    val glassSheen by infiniteTransition.animateFloat(
        initialValue = -0.6f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "GlassSheenGlide"
    )

    val activationProgress by animateFloatAsState(
        targetValue = if (isProtected || isThreatActive) 1f else 0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "ActivationProgress"
    )

    val crestScale = remember { Animatable(1f) }
    val waveProgress = remember { Animatable(0f) }
    var lastProtectedState by remember { mutableStateOf<Boolean?>(null) }
    var waveDirection by remember { mutableIntStateOf(0) }

    LaunchedEffect(isProtected) {
        val prev = lastProtectedState
        lastProtectedState = isProtected
        if (prev != null && prev != isProtected) {
            waveDirection = if (isProtected) 1 else -1
            launch {
                crestScale.snapTo(if (isProtected) 0.85f else 1.07f)
                crestScale.animateTo(
                    targetValue = if (isProtected) 1.11f else 0.93f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
                crestScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
            launch {
                waveProgress.snapTo(0f)
                waveProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 660, easing = FastOutSlowInEasing)
                )
                waveDirection = 0
            }
        }
    }

    val shieldPrimary by animateColorAsState(
        targetValue = when {
            isThreatActive -> AegisDangerRed
            isProtected -> AegisPrimaryCyan
            else -> Color(0xFF6B86C4)
        },
        animationSpec = tween(460),
        label = "ShieldPrimary"
    )

    val shieldAccent by animateColorAsState(
        targetValue = when {
            isThreatActive -> AegisChampagneGold
            isProtected -> AegisElectricBlue
            else -> AegisLavenderLight
        },
        animationSpec = tween(460),
        label = "ShieldAccent"
    )

    val haloAccent by animateColorAsState(
        targetValue = when {
            isThreatActive -> AegisChampagneGold
            isProtected -> AegisElectricBlue
            else -> AegisPurple
        },
        animationSpec = tween(460),
        label = "HaloAccent"
    )

    val glowColor by animateColorAsState(
        targetValue = when {
            isThreatActive -> AegisRedGlow
            isProtected -> AegisCyanGlow
            else -> Color(0x2E7C5CFA)
        },
        animationSpec = tween(460),
        label = "ShieldGlow"
    )

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                val floatDp = ((breathPhase - 0.5f) * 6f).dp.toPx()
                translationY = floatDp
                scaleX = crestScale.value
                scaleY = crestScale.value
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val center = Offset(w * 0.5f, h * 0.50f)

            // 1. Living breathing aura behind shield
            val auraRadius = w * (0.45f + 0.08f * breathPhase + 0.10f * activationProgress)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowColor.copy(alpha = (0.30f + 0.42f * activationProgress) * (0.75f + 0.25f * breathPhase)),
                        haloAccent.copy(alpha = (0.14f + 0.14f * activationProgress) * breathPhase),
                        Color.Transparent
                    ),
                    center = center,
                    radius = auraRadius
                ),
                radius = auraRadius,
                center = center
            )

            // 2. Toggle activation burst ripple
            if (waveDirection != 0 && waveProgress.value in 0.01f..0.99f) {
                val p = waveProgress.value
                val waveRadius = if (waveDirection > 0) w * (0.20f + 0.36f * p) else w * (0.48f - 0.28f * p)
                val waveAlpha = (1f - p) * 0.85f
                val waveColor = if (waveDirection > 0) AegisElectricBlue else AegisPurple
                drawCircle(
                    color = waveColor.copy(alpha = waveAlpha),
                    radius = waveRadius,
                    center = center,
                    style = Stroke(width = 3.2.dp.toPx() * (1f - p * 0.45f))
                )
            }

            // 3. Smooth luminous aurora sweep-gradient orbital halo
            val ringRadius = w * 0.43f
            val ringTopLeft = Offset(center.x - ringRadius, center.y - ringRadius)
            val ringSize = Size(ringRadius * 2f, ringRadius * 2f)
            rotate(degrees = sweepAngle, pivot = center) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            haloAccent.copy(alpha = 0.22f + 0.30f * activationProgress),
                            shieldPrimary.copy(alpha = 0.65f + 0.30f * activationProgress),
                            Color.White.copy(alpha = 0.90f),
                            Color.Transparent
                        ),
                        center = center
                    ),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = ringTopLeft,
                    size = ringSize,
                    style = Stroke(width = (2.4f + 0.8f * activationProgress).dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Prepare tilted 3D orbital spheres with perspective Z-sorting (preserving original shieldPrimary / haloAccent colors)
            val innerRx = w * 0.44f
            val innerRy = w * 0.26f
            val innerZ = w * 0.35f
            val camDist = w * 2.15f
            val tiltDeg = -22f
            val orbCount = 3
            data class SphereRenderItem(
                val pt: ProjectedOrbitPoint,
                val color: Color,
                val baseRadius: Float
            )
            val spheres3D = (0 until orbCount).map { i ->
                val theta = orbitAngle + i * (360f / orbCount)
                val pt = projectOrbitPoint3D(
                    center = center,
                    radiusX = innerRx,
                    radiusY = innerRy,
                    zAmplitude = innerZ,
                    angleDeg = theta,
                    tiltDeg = tiltDeg,
                    cameraDistance = camDist
                )
                val originalOrbTint = lerp(
                    if (i % 2 == 0) shieldPrimary else haloAccent,
                    Color.White,
                    0.22f
                )
                SphereRenderItem(
                    pt = pt,
                    color = originalOrbTint,
                    baseRadius = 4.2.dp.toPx()
                )
            }.sortedBy { it.pt.zNormalized }

            // 4a. Back-hemisphere 3D orbital spheres (pass behind crystal shield)
            spheres3D.filter { it.pt.zNormalized < 0f }.forEach { item ->
                val depthAlpha = (0.58f + 0.30f * (item.pt.zNormalized + 1f)).coerceIn(0.45f, 0.90f)
                drawVolumetric3DSphere(
                    center = item.pt.offset,
                    radius = item.baseRadius * item.pt.perspectiveScale,
                    baseColor = item.color,
                    alpha = depthAlpha,
                    depthNormalized = item.pt.zNormalized,
                    drawHaloAndShadow = true
                )
            }

            // 5. Outer sculpted crystal shield geometry
            val shieldPath = Path().apply {
                val cx = center.x
                val cy = center.y
                val sw = w * 0.34f
                val sh = h * 0.37f
                moveTo(cx, cy - sh * 1.06f)
                cubicTo(cx + sw * 0.95f, cy - sh * 0.78f, cx + sw * 0.96f, cy - sh * 0.14f, cx + sw * 0.84f, cy + sh * 0.44f)
                cubicTo(cx + sw * 0.64f, cy + sh * 0.88f, cx, cy + sh * 1.08f, cx, cy + sh * 1.08f)
                cubicTo(cx, cy + sh * 1.08f, cx - sw * 0.64f, cy + sh * 0.88f, cx - sw * 0.84f, cy + sh * 0.44f)
                cubicTo(cx - sw * 0.96f, cy - sh * 0.14f, cx - sw * 0.95f, cy - sh * 0.78f, cx, cy - sh * 1.06f)
                close()
            }

            // Layered frosted crystal glass fill + diagonal specular sheen sweep
            clipPath(shieldPath) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = if (isProtected) {
                            listOf(Color(0xFFFFFFFF), Color(0xFFDCEBFF), Color(0xFFE7D8FF))
                        } else {
                            listOf(Color(0xFFFFFFFF), Color(0xFFEAF1FF), Color(0xFFF0E8FF))
                        }
                    )
                )

                // Subtle dual-facet depth on right half
                drawRect(
                    color = haloAccent.copy(alpha = 0.06f + 0.06f * activationProgress),
                    topLeft = Offset(center.x, 0f),
                    size = Size(w * 0.5f, h)
                )

                // Moving glass specular gleam
                val sheenX = w * glassSheen
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.72f),
                            Color.Transparent
                        ),
                        start = Offset(sheenX - w * 0.24f, 0f),
                        end = Offset(sheenX + w * 0.24f, h)
                    )
                )
            }

            // Inner white specular rim
            val innerShieldPath = Path().apply {
                val cx = center.x
                val cy = center.y
                val sw = w * 0.28f
                val sh = h * 0.305f
                moveTo(cx, cy - sh * 1.05f)
                cubicTo(cx + sw * 0.90f, cy - sh * 0.76f, cx + sw * 0.92f, cy - sh * 0.12f, cx + sw * 0.80f, cy + sh * 0.40f)
                cubicTo(cx + sw * 0.60f, cy + sh * 0.84f, cx, cy + sh * 1.05f, cx, cy + sh * 1.05f)
                cubicTo(cx, cy + sh * 1.05f, cx - sw * 0.60f, cy + sh * 0.84f, cx - sw * 0.80f, cy + sh * 0.40f)
                cubicTo(cx - sw * 0.92f, cy - sh * 0.12f, cx - sw * 0.90f, cy - sh * 0.76f, cx, cy - sh * 1.05f)
                close()
            }
            drawPath(
                path = innerShieldPath,
                color = Color.White.copy(alpha = 0.92f),
                style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Crisp luminous sculpted outer shield rim
            drawPath(
                path = shieldPath,
                brush = Brush.linearGradient(
                    colors = listOf(shieldPrimary, shieldAccent, haloAccent),
                    start = Offset(center.x - w * 0.3f, center.y - h * 0.4f),
                    end = Offset(center.x + w * 0.3f, center.y + h * 0.4f)
                ),
                style = Stroke(width = 3.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // 6. Center floating 3D medallion orb keeping original multi-color gradient + spherical 3D dome lighting
            val coreRadius = w * (0.155f + 0.012f * breathPhase)
            drawCircle(
                color = shieldPrimary.copy(alpha = 0.20f + 0.18f * activationProgress),
                radius = coreRadius * 1.24f,
                center = center
            )
            drawCircle(
                brush = Brush.linearGradient(
                    colors = if (isProtected) {
                        listOf(AegisElectricBlue, AegisPrimaryCyan, AegisPurple)
                    } else {
                        listOf(Color(0xFF93ACE6), Color(0xFF8C77F5))
                    },
                    start = Offset(center.x - coreRadius, center.y - coreRadius),
                    end = Offset(center.x + coreRadius, center.y + coreRadius)
                ),
                radius = coreRadius,
                center = center
            )
            // Subtle 3D spherical lighting overlay so the medallion keeps its gradient while gaining volume
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.00f to Color.White.copy(alpha = 0.42f),
                        0.40f to Color.Transparent,
                        0.85f to DeepRoyalShadow.copy(alpha = 0.22f),
                        1.00f to Color.White.copy(alpha = 0.16f)
                    ),
                    center = Offset(center.x - coreRadius * 0.34f, center.y - coreRadius * 0.34f),
                    radius = coreRadius * 1.45f
                ),
                radius = coreRadius,
                center = center
            )

            // Crisp inner checkmark emblem
            val checkPath = Path().apply {
                moveTo(center.x - coreRadius * 0.38f, center.y + coreRadius * 0.04f)
                lineTo(center.x - coreRadius * 0.08f, center.y + coreRadius * 0.34f)
                lineTo(center.x + coreRadius * 0.42f, center.y - coreRadius * 0.32f)
            }
            drawPath(
                path = checkPath,
                color = Color.White,
                style = Stroke(width = 3.0.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // 4b. Front-hemisphere 3D orbital spheres (sweep gracefully across front of shield bezel)
            spheres3D.filter { it.pt.zNormalized >= 0f }.forEach { item ->
                val depthAlpha = (0.86f + 0.14f * item.pt.zNormalized).coerceIn(0.84f, 1f)
                drawVolumetric3DSphere(
                    center = item.pt.offset,
                    radius = item.baseRadius * item.pt.perspectiveScale,
                    baseColor = item.color,
                    alpha = depthAlpha,
                    depthNormalized = item.pt.zNormalized,
                    drawHaloAndShadow = true
                )
            }
        }
    }
}
