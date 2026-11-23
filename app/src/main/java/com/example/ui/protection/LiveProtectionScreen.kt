package com.example.ui.protection

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberCardTone
import com.example.ui.components.CyberTopBar
import com.example.ui.components.LuxurySectionHeader
import com.example.ui.components.SecurityHealthGaugeCard
import com.example.ui.components.SecurityHealthReport
import com.example.ui.components.SheildlenzBackdrop
import com.example.ui.components.StatusPill
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisChampagneSoft
import com.example.ui.theme.AegisElectricBlue
import com.example.ui.theme.AegisEmeraldBorder
import com.example.ui.theme.AegisEmeraldTeal
import com.example.ui.theme.AegisGoldBorder
import com.example.ui.theme.AegisGoldDeepTint
import com.example.ui.theme.AegisLavenderBorder
import com.example.ui.theme.AegisLavenderSoft
import com.example.ui.theme.AegisOnPrimaryWhite
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisPurple
import com.example.ui.theme.AegisSkySoft
import com.example.ui.theme.AegisSuccessGreen
import com.example.ui.theme.AegisTextMuted
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.AegisWarningAmber
import com.example.ui.theme.ClassicalBodyFont
import com.example.ui.theme.ClassicalDisplayFont

@Composable
fun LiveProtectionScreen(
    protectionRepo: ProtectionRepository,
    preferences: AegisPreferences,
    onRequestArmShield: () -> Unit = {},
    onNavigateToFocusHub: () -> Unit = {},
    onNavigateToProgress: () -> Unit = {},
    onNavigateToWellbeing: () -> Unit = {},
    onNavigateToParents: () -> Unit = {},
    onNavigateBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val telemetry by protectionRepo.telemetry.collectAsState()
    val hasGuardianPin by preferences.hasGuardianPin.collectAsState()
    val isGuardianUnlocked by preferences.isGuardianSessionUnlocked.collectAsState()

    // Parent connection & child sync statuses
    val parentalRole by preferences.parentalRole.collectAsState()
    val pairedParentUid by preferences.pairedParentUid.collectAsState()
    val pairedParentName by preferences.pairedParentName.collectAsState()
    val pairedChildUid by preferences.pairedChildUid.collectAsState()
    val childPairingCode by preferences.childPairingCode.collectAsState()
    val childUninstallLocked by preferences.childUninstallLocked.collectAsState()
    val liveSeeEnabled by preferences.liveSeeEnabled.collectAsState()

    // All Edits / Policy statuses (read-only view for child & parent)
    val sensitivity by preferences.sensitivity.collectAsState()
    val strictEverything by preferences.strictEverything.collectAsState()
    val safeDnsEnabled by preferences.safeDnsEnabled.collectAsState()
    val safeDnsProvider by preferences.safeDnsProvider.collectAsState()
    val smartSafeSearchEnabled by preferences.smartSafeSearchEnabled.collectAsState()
    val safeSearchPacks by preferences.safeSearchPacks.collectAsState()
    val safeSearchBlockedCount by preferences.safeSearchBlockedCount.collectAsState()
    val studyModeEnabled by preferences.studyModeEnabled.collectAsState()
    val studySessionEndMillis by preferences.studySessionEndMillis.collectAsState()
    val studyDurationMinutes by preferences.studyDurationMinutes.collectAsState()
    val studySoftWarningSeconds by preferences.studySoftWarningSeconds.collectAsState()
    val warningThresholdCount by preferences.warningThresholdCount.collectAsState()
    val warningOnlyNoLockout by preferences.warningOnlyNoLockout.collectAsState()
    val lockdownDurationMinutes by preferences.lockdownDurationMinutes.collectAsState()
    val bedtimeEnabled by preferences.bedtimeEnabled.collectAsState()
    val bedtimeStartMinutes by preferences.bedtimeStartMinutes.collectAsState()
    val bedtimeEndMinutes by preferences.bedtimeEndMinutes.collectAsState()
    val scheduleEnabled by preferences.scheduleEnabled.collectAsState()
    val scheduleStartMinutes by preferences.scheduleStartMinutes.collectAsState()
    val scheduleEndMinutes by preferences.scheduleEndMinutes.collectAsState()
    val appTimeBudgets by preferences.appTimeBudgets.collectAsState()
    val antiTamperEnabled by preferences.antiTamperEnabled.collectAsState()
    val autoStart by preferences.autoStart.collectAsState()
    val powerMode by preferences.powerMode.collectAsState()
    val notificationsEnabled by preferences.notificationsEnabled.collectAsState()
    val discreetBlockingScreenMode by preferences.discreetBlockingScreenMode.collectAsState()
    val appLanguageCode by preferences.appLanguageCode.collectAsState()

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

    val remainingStudyMillis = remember(studyModeEnabled, studySessionEndMillis) {
        if (studyModeEnabled) preferences.getRemainingStudyMillis() else 0L
    }
    val isStudyActive = studyModeEnabled && remainingStudyMillis > 0L

    val roleLabel = when (parentalRole) {
        "child" -> "Child Device"
        "parent" -> "Parent Controller"
        else -> "Standalone Device"
    }
    val isChildPaired = parentalRole == "child" && !pairedParentUid.isNullOrBlank()
    val isParentPaired = parentalRole == "parent" && !pairedChildUid.isNullOrBlank()
    val pairSummary = when {
        isChildPaired -> "Linked to ${pairedParentName?.takeIf { it.isNotBlank() } ?: "Parent"}"
        parentalRole == "child" && !childPairingCode.isNullOrBlank() -> "Waiting scan (Code $childPairingCode)"
        isParentPaired -> "Managing child (${pairedChildUid?.take(8)})"
        parentalRole == "parent" -> "Ready to pair child"
        else -> "Not linked yet"
    }
    val editsLocked = hasGuardianPin && !isGuardianUnlocked

    SheildlenzBackdrop(modifier = Modifier.testTag("live_protection_screen_root")) {
        Scaffold(
            contentWindowInsets = WindowInsets.statusBars,
            topBar = {
                CyberTopBar(
                    title = "Status",
                    subtitle = "All Shield, Parent & Policy Statuses",
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
                contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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

                // 3. Parent Connection & Sync Status
                item {
                    LuxurySectionHeader(title = "Parent Connection", accentColor = AegisEmeraldTeal)
                    Spacer(modifier = Modifier.height(6.dp))
                    CyberCard(
                        borderColor = if (isChildPaired || isParentPaired) AegisEmeraldBorder else AegisLavenderBorder,
                        tone = CyberCardTone.SKY,
                        modifier = Modifier.testTag("status_parent_connection_card")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isChildPaired || isParentPaired) AegisEmeraldTeal else AegisPurple),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SupervisorAccount,
                                        contentDescription = "Parent Connection",
                                        tint = AegisOnPrimaryWhite,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = roleLabel,
                                        color = AegisTextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = ClassicalDisplayFont,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = pairSummary,
                                        color = AegisTextSecondary,
                                        fontSize = 11.5.sp,
                                        fontFamily = ClassicalBodyFont,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            StatusPill(
                                label = when {
                                    isChildPaired || isParentPaired -> "CONNECTED"
                                    parentalRole == "child" -> "WAITING"
                                    else -> "LOCAL"
                                },
                                isActive = isChildPaired || isParentPaired
                            )
                        }

                        HorizontalDivider(
                            color = AegisCardBorder.copy(alpha = 0.45f),
                            modifier = Modifier.padding(vertical = 10.dp)
                        )

                        StatusKeyValueRow(
                            label = "Remote Edits",
                            value = if (isChildPaired) "Parent Mobile Phone" else "Local Guardian PIN",
                            valueColor = if (isChildPaired) AegisEmeraldTeal else AegisTextPrimary
                        )
                        StatusKeyValueRow(
                            label = "Uninstall Lock",
                            value = when {
                                preferences.isParentApprovedUninstallActive() -> "Parent Approved (Temp)"
                                parentalRole == "child" && childUninstallLocked -> "Locked (Child Mode)"
                                else -> "Standard"
                            },
                            valueColor = if (parentalRole == "child" && childUninstallLocked) AegisWarningAmber else AegisTextPrimary
                        )
                        StatusKeyValueRow(
                            label = "Live-See Stream",
                            value = if (liveSeeEnabled) "Enabled" else "Off",
                            valueColor = if (liveSeeEnabled) AegisPrimaryCyan else AegisTextMuted
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                AegisSoundEffects.playTap(context)
                                onNavigateToParents()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AegisEmeraldTeal,
                                contentColor = AegisOnPrimaryWhite
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("status_open_parents_button")
                        ) {
                            Text(
                                text = "OPEN PARENTS & LINKING",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = ClassicalBodyFont
                            )
                        }
                    }
                }

                // 4. All Edits & Policy Overview (Read-Only View)
                item {
                    LuxurySectionHeader(title = "Edits & Policy Status", accentColor = AegisChampagneGold)
                    Spacer(modifier = Modifier.height(6.dp))
                    CyberCard(
                        borderColor = if (editsLocked) AegisGoldBorder else AegisLavenderBorder,
                        tone = CyberCardTone.GOLD,
                        modifier = Modifier.testTag("status_edits_overview_card")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(if (editsLocked) AegisChampagneGold else AegisSuccessGreen),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (editsLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                        contentDescription = "Edits Lock Status",
                                        tint = AegisOnPrimaryWhite,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Read-Only Settings Status",
                                        color = AegisTextPrimary,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = ClassicalDisplayFont,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (editsLocked) {
                                            "Edits locked by Password / Parent Mobile"
                                        } else if (hasGuardianPin) {
                                            "Edits unlocked for Guardian session"
                                        } else {
                                            "No Guardian Password configured yet"
                                        },
                                        color = AegisTextSecondary,
                                        fontSize = 11.sp,
                                        fontFamily = ClassicalBodyFont
                                    )
                                }
                            }

                            StatusPill(
                                label = when {
                                    editsLocked -> "LOCKED"
                                    hasGuardianPin -> "UNLOCKED"
                                    else -> "OPEN"
                                },
                                isActive = !editsLocked
                            )
                        }

                        HorizontalDivider(
                            color = AegisGoldBorder.copy(alpha = 0.55f),
                            modifier = Modifier.padding(vertical = 10.dp)
                        )

                        StatusKeyValueRow(
                            label = "AI Model",
                            value = if (telemetry.isModelLoaded) telemetry.loadedModelName ?: "Loaded" else "Not Loaded",
                            valueColor = if (telemetry.isModelLoaded) AegisSuccessGreen else AegisWarningAmber
                        )
                        StatusKeyValueRow(
                            label = "Filter Sensitivity",
                            value = "${sensitivity.displayName}${if (strictEverything) " • Strict All" else ""}",
                            valueColor = if (strictEverything) AegisPurple else AegisTextPrimary
                        )
                        StatusKeyValueRow(
                            label = "Safe DNS",
                            value = if (safeDnsEnabled) "ON • ${safeDnsProvider.ratingBadge}" else "OFF",
                            valueColor = if (safeDnsEnabled) AegisPrimaryCyan else AegisTextMuted
                        )
                        StatusKeyValueRow(
                            label = "Smart Safe Search",
                            value = if (smartSafeSearchEnabled) {
                                "ON • ${safeSearchPacks.size} Packs ($safeSearchBlockedCount hits)"
                            } else {
                                "OFF"
                            },
                            valueColor = if (smartSafeSearchEnabled) AegisPrimaryCyan else AegisTextMuted
                        )
                        StatusKeyValueRow(
                            label = "Study Lock Timer",
                            value = if (isStudyActive) {
                                val minsLeft = ((remainingStudyMillis + 59_999L) / 60_000L).coerceAtLeast(1L)
                                "ACTIVE (${minsLeft}m left)"
                            } else {
                                "Ready (${studyDurationMinutes}m • ${studySoftWarningSeconds}s warn)"
                            },
                            valueColor = if (isStudyActive) AegisPurple else AegisTextPrimary
                        )
                        StatusKeyValueRow(
                            label = "Warnings & Lockout",
                            value = "$warningThresholdCount warn • ${if (warningOnlyNoLockout) "Warn Only" else AegisPreferences.formatMinutesLabel(lockdownDurationMinutes)}",
                            valueColor = AegisTextPrimary
                        )
                        StatusKeyValueRow(
                            label = "Bedtime Lock",
                            value = if (bedtimeEnabled) {
                                "ON • ${AegisPreferences.formatClockTime(bedtimeStartMinutes)}–${AegisPreferences.formatClockTime(bedtimeEndMinutes)}"
                            } else {
                                "OFF"
                            },
                            valueColor = if (bedtimeEnabled) AegisChampagneGold else AegisTextMuted
                        )
                        StatusKeyValueRow(
                            label = "Quiet Hours",
                            value = if (scheduleEnabled) {
                                "ON • ${AegisPreferences.formatClockTime(scheduleStartMinutes)}–${AegisPreferences.formatClockTime(scheduleEndMinutes)}"
                            } else {
                                "OFF"
                            },
                            valueColor = if (scheduleEnabled) AegisPurple else AegisTextMuted
                        )
                        StatusKeyValueRow(
                            label = "App Time Limits",
                            value = if (appTimeBudgets.isNotEmpty()) "${appTimeBudgets.size} Apps Capped" else "None Set",
                            valueColor = if (appTimeBudgets.isNotEmpty()) AegisEmeraldTeal else AegisTextMuted
                        )
                        StatusKeyValueRow(
                            label = "Anti-Tamper & Power",
                            value = "${if (antiTamperEnabled) "Locked" else "Off"} • ${powerMode.displayName}",
                            valueColor = AegisTextPrimary
                        )
                        StatusKeyValueRow(
                            label = "Boot Auto-Start",
                            value = if (autoStart) "ON" else "OFF",
                            valueColor = if (autoStart) AegisSuccessGreen else AegisTextMuted
                        )
                        StatusKeyValueRow(
                            label = "Notifications",
                            value = if (notificationsEnabled) "ON" else "OFF (Silent)",
                            valueColor = if (notificationsEnabled) AegisPrimaryCyan else AegisTextMuted
                        )
                        StatusKeyValueRow(
                            label = "Block Screen Style",
                            value = if (discreetBlockingScreenMode) "Parent System Block" else "Standard Sheildlenz",
                            valueColor = AegisTextPrimary
                        )
                        StatusKeyValueRow(
                            label = "Language",
                            value = when (appLanguageCode) {
                                "ur" -> "اردو (Urdu)"
                                "ar" -> "العربية (Arabic)"
                                else -> "English"
                            },
                            valueColor = AegisTextPrimary
                        )
                    }
                }

                // 5. Shifted Monitoring & Control Hub Gateways (previously bottom of Home)
                item {
                    LuxurySectionHeader(title = "Monitoring & Tools", accentColor = AegisPurple)
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("status_navigation_section"),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatusDestinationGatewayCard(
                            title = "Focus & Safety Hub",
                            subtitle = "Study timer, Smart Safe Search & app budgets",
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            gradientColors = listOf(AegisPurple, AegisPrimaryCyan),
                            surfaceTint = listOf(Color(0xFFFAF7FF), Color(0xFFEEF6FF)),
                            borderColor = AegisLavenderBorder,
                            accentColor = AegisPurple,
                            onClick = {
                                AegisSoundEffects.playTap(context)
                                onNavigateToFocusHub()
                            },
                            testTag = "status_open_focus_hub_button"
                        )

                        StatusDestinationGatewayCard(
                            title = "Security Progress",
                            subtitle = "Blocked applications & security history",
                            icon = Icons.Default.Assessment,
                            gradientColors = listOf(AegisPurple, AegisChampagneGold),
                            surfaceTint = listOf(Color(0xFFFAF8FF), AegisLavenderSoft),
                            borderColor = AegisLavenderBorder,
                            accentColor = AegisPurple,
                            onClick = {
                                AegisSoundEffects.playTap(context)
                                onNavigateToProgress()
                            },
                            testTag = "status_open_progress_button"
                        )

                        StatusDestinationGatewayCard(
                            title = "Wellbeing & Safe DNS",
                            subtitle = "Strict Deen Safe DNS, Bedtime & App Budgets",
                            icon = Icons.Default.Dns,
                            gradientColors = listOf(AegisChampagneGold, AegisPrimaryCyan),
                            surfaceTint = listOf(Color(0xFFFFFBF2), Color(0xFFF0F9FF)),
                            borderColor = AegisGoldBorder,
                            accentColor = AegisChampagneGold,
                            onClick = {
                                AegisSoundEffects.playTap(context)
                                onNavigateToWellbeing()
                            },
                            testTag = "status_open_wellbeing_button"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusKeyValueRow(
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = AegisTextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = ClassicalBodyFont,
            modifier = Modifier.padding(end = 10.dp)
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = ClassicalBodyFont,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

@Composable
private fun StatusDestinationGatewayCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    surfaceTint: List<Color>,
    borderColor: Color,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    val cardShape = RoundedCornerShape(22.dp)
    Surface(
        onClick = onClick,
        shape = cardShape,
        color = Color.Transparent,
        border = BorderStroke(1.2.dp, borderColor.copy(alpha = 0.85f)),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .shadow(
                elevation = 8.dp,
                shape = cardShape,
                ambientColor = accentColor.copy(alpha = 0.16f),
                spotColor = accentColor.copy(alpha = 0.16f)
            )
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = Brush.horizontalGradient(colors = surfaceTint))
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(brush = Brush.linearGradient(colors = gradientColors)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = AegisOnPrimaryWhite,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        color = AegisTextPrimary,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalDisplayFont
                    )
                    Text(
                        text = subtitle,
                        color = AegisTextSecondary,
                        fontSize = 12.sp,
                        fontFamily = ClassicalBodyFont
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f))
                    .border(1.dp, accentColor.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Open $title",
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
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
