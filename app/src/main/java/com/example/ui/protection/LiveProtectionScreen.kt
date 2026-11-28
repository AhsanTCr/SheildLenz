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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import com.example.core.models.ShortVideoScrollDetector
import com.example.core.permissions.PermissionHelper
import com.example.data.preferences.AegisPreferences
import com.example.data.repository.BlockedDnsLogRepository
import com.example.data.repository.ProtectionRepository
import com.example.services.dns.AegisSafeDnsVpnService
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
    blockedDnsLogRepo: BlockedDnsLogRepository? = null,
    onRequestArmShield: () -> Unit = {},
    onNavigateToFocusHub: () -> Unit = {},
    onNavigateToProgress: () -> Unit = {},
    onNavigateToWellbeing: () -> Unit = {},
    onNavigateToParents: () -> Unit = {},
    onNavigateToEdits: () -> Unit = {},
    onNavigateToUsageScrollGuard: () -> Unit = {},
    onNavigateBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val telemetry by protectionRepo.telemetry.collectAsState()
    val hasGuardianPin by preferences.hasGuardianPin.collectAsState()
    val isGuardianUnlocked by preferences.isGuardianSessionUnlocked.collectAsState()
    val blockedQueryCount by AegisSafeDnsVpnService.blockedQueryCount.collectAsState()
    val liveDnsLogs by (blockedDnsLogRepo?.liveLogs?.collectAsState() ?: remember { mutableStateOf(emptyList()) })

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
    val isMasterPowerOn by preferences.masterPowerEnabled.collectAsState()
    val dailyReelsTotalCount by preferences.dailyReelsTotalCount.collectAsState()
    val scrollLimitCount by preferences.scrollLimitCount.collectAsState()
    val scrollLockEnabled by preferences.scrollLockEnabled.collectAsState()
    val mindfulPauseEnabled by preferences.mindfulPauseEnabled.collectAsState()
    val mindfulPausePackages by preferences.mindfulPausePackages.collectAsState()
    val dailyAppUsageSeconds by preferences.dailyAppUsageSeconds.collectAsState()
    val gameGuardEnabled by preferences.gameGuardEnabled.collectAsState()
    val dailyGameTotalSeconds by preferences.dailyGameTotalSeconds.collectAsState()

    val activeAppName by protectionRepo.activeAppLabel.collectAsState()

    var hasOverlay by remember { mutableStateOf(telemetry.isOverlayPermitted) }
    var hasAccessibility by remember { mutableStateOf(telemetry.isAccessibilityActive) }
    var hasBattery by remember { mutableStateOf(false) }
    var isDeviceAdmin by remember { mutableStateOf(false) }

    val refreshPermissions = remember(context, protectionRepo) {
        {
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

    LaunchedEffect(Unit) {
        refreshPermissions()
    }

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

    DisposableEffect(lifecycleOwner, refreshPermissions) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissions()
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
                // 0. Sleek Live Protection Heartbeat Strip
                item(key = "status_heartbeat") {
                    val heartbeatColor = when {
                        !isMasterPowerOn -> AegisWarningAmber
                        telemetry.isProtectionEnabled -> AegisEmeraldTeal
                        else -> AegisPurple
                    }
                    CyberCard(
                        borderColor = when {
                            !isMasterPowerOn -> AegisGoldBorder
                            telemetry.isProtectionEnabled -> AegisEmeraldBorder
                            else -> AegisLavenderBorder
                        },
                        tone = when {
                            !isMasterPowerOn -> CyberCardTone.GOLD
                            telemetry.isProtectionEnabled -> CyberCardTone.EMERALD
                            else -> CyberCardTone.LAVENDER
                        },
                        modifier = Modifier.testTag("status_heartbeat_card")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(13.dp))
                                        .background(
                                            brush = Brush.linearGradient(
                                                colors = listOf(heartbeatColor, AegisPrimaryCyan)
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (!isMasterPowerOn) Icons.Default.PowerSettingsNew else Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = AegisOnPrimaryWhite,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = when {
                                            !isMasterPowerOn -> "Shutdown Mode"
                                            telemetry.isProtectionEnabled -> "Visual Shield Armed"
                                            else -> "Shield on Standby"
                                        },
                                        color = AegisTextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = ClassicalDisplayFont,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Active App: $activeAppName",
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
                                    !isMasterPowerOn -> "POWER OFF"
                                    telemetry.isProtectionEnabled -> "LIVE"
                                    else -> "STANDBY"
                                },
                                isActive = telemetry.isProtectionEnabled && isMasterPowerOn,
                                activeColor = AegisEmeraldTeal,
                                inactiveColor = heartbeatColor
                            )
                        }
                    }
                }

                // 0B. Usage, Mindful Pause & Reels/Shorts Scroll Guard Gateway (directly on Status screen; acts as both monitor & editor)
                item(key = "status_usage_scroll_guard") {
                    val totalUsageToday = remember(dailyAppUsageSeconds) {
                        ShortVideoScrollDetector.formatMinutesCompact(dailyAppUsageSeconds.values.sum())
                    }
                    val guardSubtitle = buildString {
                        append("$dailyReelsTotalCount / $scrollLimitCount Reels • $totalUsageToday Today")
                        append(if (scrollLockEnabled) " • Auto-Stop ON" else " • Counter ON")
                        if (gameGuardEnabled) {
                            val gameToday = ShortVideoScrollDetector.formatMinutesCompact(dailyGameTotalSeconds)
                            append(" • Game Guard ON ($gameToday)")
                        }
                        if (mindfulPauseEnabled) {
                            append(" • ${mindfulPausePackages.size} Pause App(s)")
                        }
                    }
                    LuxurySectionHeader(title = "Usage & Scroll Guard", accentColor = AegisEmeraldTeal)
                    Spacer(modifier = Modifier.height(6.dp))
                    StatusDestinationGatewayCard(
                        title = "Mindful Pause & Reels Guard",
                        subtitle = guardSubtitle,
                        icon = Icons.Default.HourglassBottom,
                        gradientColors = listOf(AegisEmeraldTeal, AegisPurple),
                        surfaceTint = listOf(Color(0xFFE8FAF5), Color(0xFFF1EDFF)),
                        borderColor = AegisEmeraldBorder,
                        accentColor = AegisEmeraldTeal,
                        onClick = {
                            AegisSoundEffects.playTap(context)
                            onNavigateToUsageScrollGuard()
                        },
                        testTag = "status_open_usage_scroll_guard_card"
                    )
                }

                // 1. Unified Compact Permissions & Shield Health
                item(key = "status_permissions") {
                    LuxurySectionHeader(title = "Permissions", accentColor = AegisPurple)
                    Spacer(modifier = Modifier.height(6.dp))
                    SecurityHealthGaugeCard(
                        report = healthReport,
                        preferences = preferences,
                        onRequestArmShield = {
                            if (!isMasterPowerOn) {
                                android.widget.Toast.makeText(
                                    context,
                                    "Shutdown is OFF in About. Turn Shutdown ON first.",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                if (!telemetry.isProtectionEnabled) {
                                    AegisSoundEffects.playModeOn(context)
                                }
                                onRequestArmShield()
                            }
                        }
                    )
                }

                // 1B. Built-In Bypass Shield Diagnostics with 1-Tap Fixes
                item(key = "status_bypass_audit") {
                    LuxurySectionHeader(title = "Bypass Diagnostics", accentColor = AegisElectricBlue)
                    Spacer(modifier = Modifier.height(6.dp))
                    BypassShieldDiagnosticsCard(
                        hasOverlay = hasOverlay,
                        hasAccessibility = hasAccessibility,
                        hasBattery = hasBattery,
                        safeDnsEnabled = safeDnsEnabled,
                        isDeviceAdmin = isDeviceAdmin,
                        isModelLoaded = telemetry.isModelLoaded,
                        onFixOverlay = {
                            preferences.beginAuthorizedExternalFlow()
                            runCatching { context.startActivity(PermissionHelper.getOverlaySettingsIntent(context)) }
                                .onFailure { runCatching { context.startActivity(PermissionHelper.getAppDetailsSettingsIntent(context)) } }
                        },
                        onFixAccessibility = {
                            preferences.beginAuthorizedExternalFlow()
                            runCatching { context.startActivity(PermissionHelper.getAccessibilitySettingsIntent()) }
                        },
                        onFixBattery = {
                            preferences.beginAuthorizedExternalFlow()
                            runCatching { context.startActivity(PermissionHelper.getBatteryOptimizationSettingsIntent(context)) }
                                .onFailure { runCatching { context.startActivity(PermissionHelper.getAppDetailsSettingsIntent(context)) } }
                        },
                        onFixSafeDns = onNavigateToWellbeing,
                        onFixDeviceAdmin = {
                            preferences.grantTemporarySettingsAuthorization()
                            runCatching { context.startActivity(AegisDeviceAdminReceiver.createActivateAdminIntent(context)) }
                        },
                        onFixAiModel = onNavigateToEdits
                    )
                }

                // 1C. Live DNS & SafeSearch Activity Preview
                item(key = "status_live_activity") {
                    LuxurySectionHeader(title = "Live Blocking Feed", accentColor = AegisPurple)
                    Spacer(modifier = Modifier.height(6.dp))
                    LiveActivityFeedPreviewCard(
                        liveLogs = liveDnsLogs,
                        blockedDnsCount = blockedQueryCount,
                        safeSearchBlockedCount = safeSearchBlockedCount,
                        onOpenFullFeed = {
                            AegisSoundEffects.playTap(context)
                            onNavigateToWellbeing()
                        }
                    )
                }

                // 2. Sculpted Organic Leaf-Orb Pods for Live Rates & Frames
                item(key = "status_rates") {
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
                item(key = "status_parents") {
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
                item(key = "status_edits_overview") {
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

                        val overviewRows = listOf(
                            Triple("Master App Power", if (isMasterPowerOn) "ON" else "OFF (0% Battery • No Background)", if (isMasterPowerOn) AegisSuccessGreen else AegisWarningAmber),
                            Triple("AI Model", if (telemetry.isModelLoaded) telemetry.loadedModelName ?: "Loaded" else "Not Loaded", if (telemetry.isModelLoaded) AegisSuccessGreen else AegisWarningAmber),
                            Triple("Filter Sensitivity", "${sensitivity.displayName}${if (strictEverything) " • Strict All" else ""}", if (strictEverything) AegisPurple else AegisTextPrimary),
                            Triple("Safe DNS", if (safeDnsEnabled) "ON • ${safeDnsProvider.ratingBadge}" else "OFF", if (safeDnsEnabled) AegisPrimaryCyan else AegisTextMuted),
                            Triple("Smart Safe Search", if (smartSafeSearchEnabled) "ON • ${safeSearchPacks.size} Packs ($safeSearchBlockedCount hits)" else "OFF", if (smartSafeSearchEnabled) AegisPrimaryCyan else AegisTextMuted),
                            Triple("Study Lock Timer", if (isStudyActive) {
                                val minsLeft = ((remainingStudyMillis + 59_999L) / 60_000L).coerceAtLeast(1L)
                                "ACTIVE (${minsLeft}m left)"
                            } else {
                                "Ready (${studyDurationMinutes}m • ${studySoftWarningSeconds}s warn)"
                            }, if (isStudyActive) AegisPurple else AegisTextPrimary),
                            Triple("Warnings & Lockout", "$warningThresholdCount warn • ${if (warningOnlyNoLockout) "Warn Only" else AegisPreferences.formatMinutesLabel(lockdownDurationMinutes)}", AegisTextPrimary),
                            Triple("Bedtime Lock", if (bedtimeEnabled) "ON • ${AegisPreferences.formatClockTime(bedtimeStartMinutes)}–${AegisPreferences.formatClockTime(bedtimeEndMinutes)}" else "OFF", if (bedtimeEnabled) AegisChampagneGold else AegisTextMuted),
                            Triple("Quiet Hours", if (scheduleEnabled) "ON • ${AegisPreferences.formatClockTime(scheduleStartMinutes)}–${AegisPreferences.formatClockTime(scheduleEndMinutes)}" else "OFF", if (scheduleEnabled) AegisPurple else AegisTextMuted),
                            Triple("App Time Limits", if (appTimeBudgets.isNotEmpty()) "${appTimeBudgets.size} Apps Capped" else "None Set", if (appTimeBudgets.isNotEmpty()) AegisEmeraldTeal else AegisTextMuted),
                            Triple("Reels/Shorts Scroll Guard", if (scrollLockEnabled) "$dailyReelsTotalCount/$scrollLimitCount • Auto-Stop" else "$dailyReelsTotalCount watched • Counter", if (scrollLockEnabled) AegisEmeraldTeal else AegisPrimaryCyan),
                            Triple("Anti-Tamper & Power", "${if (antiTamperEnabled) "Locked" else "Off"} • ${powerMode.displayName}", AegisTextPrimary),
                            Triple("Boot Auto-Start", if (autoStart) "ON" else "OFF", if (autoStart) AegisSuccessGreen else AegisTextMuted),
                            Triple("Notifications", if (notificationsEnabled) "ON" else "OFF (Silent)", if (notificationsEnabled) AegisPrimaryCyan else AegisTextMuted),
                            Triple("Block Screen Style", if (discreetBlockingScreenMode) "Parent System Block" else "Standard Sheildlenz", AegisTextPrimary),
                            Triple("Language", when (appLanguageCode) {
                                "ur" -> "اردو (Urdu)"
                                "ar" -> "العربية (Arabic)"
                                else -> "English"
                            }, AegisTextPrimary)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            overviewRows.forEachIndexed { idx, (label, value, color) ->
                                StatusKeyValueRow(
                                    label = label,
                                    value = value,
                                    valueColor = color,
                                    tintedBackground = idx % 2 == 0
                                )
                            }
                        }
                    }
                }

                // 5. Shifted Monitoring & Control Hub Gateways (previously bottom of Home)
                item(key = "status_gateways") {
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
    valueColor: Color,
    tintedBackground: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (tintedBackground) Color.White.copy(alpha = 0.62f) else Color.White.copy(alpha = 0.28f))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(end = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(valueColor.copy(alpha = 0.78f))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = AegisTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = ClassicalBodyFont
            )
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = valueColor.copy(alpha = 0.10f),
            border = BorderStroke(0.8.dp, valueColor.copy(alpha = 0.22f))
        ) {
            Text(
                text = value,
                color = valueColor,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ClassicalBodyFont,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
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

private data class BypassDiagnosticItem(
    val title: String,
    val detail: String,
    val fixTag: String,
    val actionLabel: String,
    val onFix: () -> Unit
)

@Composable
private fun BypassShieldDiagnosticsCard(
    hasOverlay: Boolean,
    hasAccessibility: Boolean,
    hasBattery: Boolean,
    safeDnsEnabled: Boolean,
    isDeviceAdmin: Boolean,
    isModelLoaded: Boolean,
    onFixOverlay: () -> Unit,
    onFixAccessibility: () -> Unit,
    onFixBattery: () -> Unit,
    onFixSafeDns: () -> Unit,
    onFixDeviceAdmin: () -> Unit,
    onFixAiModel: () -> Unit
) {
    val gaps = remember(
        hasOverlay,
        hasAccessibility,
        hasBattery,
        safeDnsEnabled,
        isDeviceAdmin,
        isModelLoaded
    ) {
        buildList {
            if (!hasOverlay) {
                add(
                    BypassDiagnosticItem(
                        title = "Overlay Lockout Disabled",
                        detail = "Blocked screens cannot pop above apps.",
                        fixTag = "audit_fix_overlay",
                        actionLabel = "FIX",
                        onFix = onFixOverlay
                    )
                )
            }
            if (!hasAccessibility) {
                add(
                    BypassDiagnosticItem(
                        title = "Accessibility Guard Off",
                        detail = "Browser URL & SafeSearch inspection paused.",
                        fixTag = "audit_fix_accessibility",
                        actionLabel = "FIX",
                        onFix = onFixAccessibility
                    )
                )
            }
            if (!hasBattery) {
                add(
                    BypassDiagnosticItem(
                        title = "Battery Saver May Kill Shield",
                        detail = "Allow unrestricted background power.",
                        fixTag = "audit_fix_battery",
                        actionLabel = "FIX",
                        onFix = onFixBattery
                    )
                )
            }
            if (!safeDnsEnabled) {
                add(
                    BypassDiagnosticItem(
                        title = "Safe DNS Filter Off",
                        detail = "Enable Strict Family DNS for network-wide protection.",
                        fixTag = "audit_fix_safedns",
                        actionLabel = "ENABLE",
                        onFix = onFixSafeDns
                    )
                )
            }
            if (!isDeviceAdmin) {
                add(
                    BypassDiagnosticItem(
                        title = "Device Admin Anti-Tamper Off",
                        detail = "Activate admin guard to prevent uninstall bypass.",
                        fixTag = "audit_fix_admin",
                        actionLabel = "FIX",
                        onFix = onFixDeviceAdmin
                    )
                )
            }
            if (!isModelLoaded) {
                add(
                    BypassDiagnosticItem(
                        title = "On-Device AI Model Missing",
                        detail = "Load or download a .tflite classifier in Edits.",
                        fixTag = "audit_fix_model",
                        actionLabel = "LOAD",
                        onFix = onFixAiModel
                    )
                )
            }
        }
    }

    CyberCard(
        borderColor = if (gaps.isEmpty()) AegisEmeraldBorder else AegisGoldBorder,
        tone = if (gaps.isEmpty()) CyberCardTone.EMERALD else CyberCardTone.GOLD,
        modifier = Modifier.testTag("status_bypass_audit_card")
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
                        .background(if (gaps.isEmpty()) AegisEmeraldTeal else AegisWarningAmber),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (gaps.isEmpty()) Icons.Default.Security else Icons.Default.Warning,
                        contentDescription = "Bypass Shield Diagnostics",
                        tint = AegisOnPrimaryWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Bypass Shield Diagnostics",
                        color = AegisTextPrimary,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalDisplayFont,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (gaps.isEmpty()) {
                            "All 6 bypass defenses active & verified"
                        } else {
                            "${gaps.size} vulnerability gap${if (gaps.size > 1) "s" else ""} found • 1-tap fix ready"
                        },
                        color = AegisTextSecondary,
                        fontSize = 11.sp,
                        fontFamily = ClassicalBodyFont
                    )
                }
            }
            StatusPill(
                label = if (gaps.isEmpty()) "ARMED" else "${gaps.size} GAPS",
                isActive = gaps.isEmpty(),
                activeColor = AegisEmeraldTeal,
                inactiveColor = AegisWarningAmber
            )
        }

        if (gaps.isNotEmpty()) {
            HorizontalDivider(
                color = AegisGoldBorder.copy(alpha = 0.5f),
                modifier = Modifier.padding(vertical = 10.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                gaps.take(4).forEach { gap ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.72f))
                            .border(1.dp, AegisCardBorder.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = gap.title,
                                color = AegisTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = ClassicalBodyFont,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = gap.detail,
                                color = AegisTextSecondary,
                                fontSize = 10.5.sp,
                                fontFamily = ClassicalBodyFont,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Button(
                            onClick = gap.onFix,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AegisPrimaryCyan,
                                contentColor = AegisOnPrimaryWhite
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag(gap.fixTag)
                        ) {
                            Text(
                                text = gap.actionLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = ClassicalBodyFont
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveActivityFeedPreviewCard(
    liveLogs: List<com.example.data.repository.BlockedDnsLogItem>,
    blockedDnsCount: Long,
    safeSearchBlockedCount: Int,
    onOpenFullFeed: () -> Unit
) {
    CyberCard(
        borderColor = AegisLavenderBorder,
        tone = CyberCardTone.LAVENDER,
        modifier = Modifier.testTag("status_live_activity_card")
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
                        .background(AegisPurple),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Dns,
                        contentDescription = "Live DNS & Search Activity",
                        tint = AegisOnPrimaryWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Live DNS & Search Activity",
                        color = AegisTextPrimary,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalDisplayFont,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$blockedDnsCount DNS blocked • $safeSearchBlockedCount SafeSearch hits",
                        color = AegisTextSecondary,
                        fontSize = 11.sp,
                        fontFamily = ClassicalBodyFont
                    )
                }
            }
            StatusPill(
                label = if (liveLogs.isEmpty()) "CLEAR" else "${liveLogs.size} LOGS",
                isActive = liveLogs.isNotEmpty(),
                activeColor = AegisPurple,
                inactiveColor = AegisEmeraldTeal
            )
        }

        HorizontalDivider(
            color = AegisLavenderBorder.copy(alpha = 0.5f),
            modifier = Modifier.padding(vertical = 10.dp)
        )

        if (liveLogs.isEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.65f))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = AegisEmeraldTeal,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Clean session — 0 blocked domains or search hits recorded yet.",
                    color = AegisTextSecondary,
                    fontSize = 11.5.sp,
                    fontFamily = ClassicalBodyFont
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                liveLogs.take(4).forEach { entry ->
                    StatusKeyValueRow(
                        label = entry.domain,
                        value = entry.reasonCategory,
                        valueColor = when (entry.reasonCategory) {
                            BlockedDnsLogRepository.CATEGORY_SEARCH -> AegisPurple
                            BlockedDnsLogRepository.CATEGORY_GAMBLING -> AegisWarningAmber
                            else -> AegisChampagneGold
                        },
                        tintedBackground = true
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = onOpenFullFeed,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AegisPurple,
                contentColor = AegisOnPrimaryWhite
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("status_open_activity_feed_button")
        ) {
            Text(
                text = "VIEW FULL ACTIVITY FEED",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ClassicalBodyFont
            )
        }
    }
}
