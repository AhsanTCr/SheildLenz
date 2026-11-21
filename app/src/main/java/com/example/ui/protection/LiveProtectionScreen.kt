package com.example.ui.protection

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.core.audio.AegisSoundEffects
import com.example.core.permissions.PermissionHelper
import com.example.data.preferences.AegisPreferences
import com.example.data.repository.ProtectionRepository
import com.example.monitoring.accessibility.AegisAccessibilityService
import com.example.security.deviceowner.AegisDeviceAdminReceiver
import com.example.ui.components.CyberTopBar
import com.example.ui.components.LuxurySectionHeader
import com.example.ui.components.SecurityHealthGaugeCard
import com.example.ui.components.SecurityHealthReport
import com.example.ui.components.SheildlenzBackdrop
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisChampagneSoft
import com.example.ui.theme.AegisGoldBorder
import com.example.ui.theme.AegisGoldDeepTint
import com.example.ui.theme.AegisOnPrimaryWhite
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisPurple
import com.example.ui.theme.AegisSkySoft
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.ClassicalBodyFont
import com.example.ui.theme.ClassicalDisplayFont

@Composable
fun LiveProtectionScreen(
    protectionRepo: ProtectionRepository,
    preferences: AegisPreferences,
    onRequestArmShield: () -> Unit = {},
    onNavigateBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val telemetry by protectionRepo.telemetry.collectAsState()
    val hasGuardianPin by preferences.hasGuardianPin.collectAsState()

    var hasOverlay by remember { mutableStateOf(PermissionHelper.hasOverlayPermission(context)) }
    var hasAccessibility by remember {
        mutableStateOf(PermissionHelper.isAccessibilityServiceEnabled(context, AegisAccessibilityService::class.java))
    }
    var hasBattery by remember { mutableStateOf(PermissionHelper.isIgnoringBatteryOptimizations(context)) }
    var isDeviceAdmin by remember { mutableStateOf(AegisDeviceAdminReceiver.isDeviceAdmin(context)) }

    val healthReport = remember(
        telemetry.isProtectionEnabled,
        hasOverlay,
        hasAccessibility,
        isDeviceAdmin,
        hasGuardianPin,
        hasBattery,
        telemetry.isModelLoaded
    ) {
        SecurityHealthReport.compute(
            isShieldRunning = telemetry.isProtectionEnabled,
            hasOverlay = hasOverlay,
            hasAccessibility = hasAccessibility,
            isDeviceAdmin = isDeviceAdmin,
            hasGuardianPin = hasGuardianPin,
            hasBatteryExemption = hasBattery,
            isModelLoaded = telemetry.isModelLoaded
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlay = PermissionHelper.hasOverlayPermission(context)
                hasAccessibility = PermissionHelper.isAccessibilityServiceEnabled(
                    context,
                    AegisAccessibilityService::class.java
                )
                hasBattery = PermissionHelper.isIgnoringBatteryOptimizations(context)
                isDeviceAdmin = AegisDeviceAdminReceiver.isDeviceAdmin(context)
                protectionRepo.refreshPermissionHealth()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    SheildlenzBackdrop(modifier = Modifier.testTag("live_protection_screen_root")) {
        Scaffold(
            contentWindowInsets = WindowInsets.statusBars,
            topBar = {
                CyberTopBar(
                    title = "Status",
                    onNavigateBack = onNavigateBack
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 22.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                // 1. Unified Compact Permissions & Shield Health
                item {
                    LuxurySectionHeader(title = "Permissions", accentColor = AegisPurple)
                    Spacer(modifier = Modifier.height(6.dp))
                    SecurityHealthGaugeCard(
                        report = healthReport,
                        preferences = preferences,
                        onRequestArmShield = {
                            if (!telemetry.isProtectionEnabled) {
                                AegisSoundEffects.playModeOn(context)
                            }
                            onRequestArmShield()
                        }
                    )
                }

                // 2. Sculpted Organic Leaf-Orb Pods for Live Rates & Frames
                item {
                    LuxurySectionHeader(title = "Live Rates & Frames", accentColor = AegisPrimaryCyan)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SculptedLiveTelemetryPod(
                            title = "Inspection Rate",
                            value = if (telemetry.isProtectionEnabled) "%.1f FPS".format(telemetry.currentInspectionFps) else "0.0 FPS",
                            caption = "Live speed",
                            icon = Icons.Default.Speed,
                            accentColor = AegisPrimaryCyan,
                            gradientColors = listOf(Color(0xFFEAF4FF), AegisSkySoft),
                            borderColor = AegisCardBorder,
                            isLeftWing = true,
                            progressFraction = if (telemetry.isProtectionEnabled) (telemetry.currentInspectionFps / 5f).coerceIn(0.15f, 1f) else 0.12f,
                            modifier = Modifier.weight(1f)
                        )

                        SculptedLiveTelemetryPod(
                            title = "Frames Scanned",
                            value = telemetry.framesProcessedCount.toString(),
                            caption = "Volatile RAM",
                            icon = Icons.Default.Camera,
                            accentColor = AegisChampagneGold,
                            gradientColors = listOf(AegisChampagneSoft, AegisGoldDeepTint),
                            borderColor = AegisGoldBorder,
                            isLeftWing = false,
                            progressFraction = ((telemetry.framesProcessedCount % 100) / 100f).coerceAtLeast(0.18f),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

            }
        }
    }
}

@Composable
private fun SculptedLiveTelemetryPod(
    title: String,
    value: String,
    caption: String,
    icon: ImageVector,
    accentColor: Color,
    gradientColors: List<Color>,
    borderColor: Color,
    isLeftWing: Boolean,
    progressFraction: Float,
    modifier: Modifier = Modifier
) {
    // Mirrored organic teardrop/leaf capsule shape for modern visual contrast
    val podShape = if (isLeftWing) {
        RoundedCornerShape(topStart = 34.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 34.dp)
    } else {
        RoundedCornerShape(topStart = 12.dp, topEnd = 34.dp, bottomStart = 34.dp, bottomEnd = 12.dp)
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = podShape,
                ambientColor = accentColor.copy(alpha = 0.22f),
                spotColor = accentColor.copy(alpha = 0.22f)
            )
            .clip(podShape)
            .background(brush = Brush.verticalGradient(colors = gradientColors))
            .border(1.3.dp, borderColor.copy(alpha = 0.85f), podShape)
    ) {
        // Subtle radial arc ornament in background
        Canvas(modifier = Modifier.matchParentSize()) {
            val arcCenter = if (isLeftWing) Offset(size.width * 0.85f, size.height * 0.25f) else Offset(size.width * 0.15f, size.height * 0.25f)
            val r = size.minDimension * 0.42f
            drawCircle(
                color = accentColor.copy(alpha = 0.08f),
                radius = r,
                center = arcCenter
            )
            drawArc(
                color = accentColor.copy(alpha = 0.28f),
                startAngle = -110f,
                sweepAngle = 220f * progressFraction,
                useCenter = false,
                topLeft = Offset(arcCenter.x - r * 0.72f, arcCenter.y - r * 0.72f),
                size = Size(r * 1.44f, r * 1.44f),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
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
                    letterSpacing = 0.8.sp,
                    fontFamily = ClassicalBodyFont,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(accentColor, AegisPurple)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = AegisOnPrimaryWhite,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                color = AegisTextPrimary,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ClassicalDisplayFont,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.65f))
                    .padding(horizontal = 9.dp, vertical = 3.dp)
            ) {
                Text(
                    text = caption,
                    color = accentColor,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalBodyFont
                )
            }
        }
    }
}
