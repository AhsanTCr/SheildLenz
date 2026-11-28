package com.example.ui.mindful

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.core.audio.AegisSoundEffects
import com.example.core.models.GameAppDetector
import com.example.core.models.MindfulScrollEvent
import com.example.core.models.ShortVideoScrollDetector
import com.example.core.permissions.PermissionHelper
import com.example.data.preferences.AegisPreferences
import com.example.data.repository.ProtectionRepository
import com.example.monitoring.accessibility.AegisAccessibilityService
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberCardTone
import com.example.ui.components.CyberTopBar
import com.example.ui.components.LuxurySectionHeader
import com.example.ui.components.SheildlenzBackdrop
import com.example.ui.components.rememberMasterKeyGate
import com.example.ui.settings.InstalledAppEntry
import com.example.ui.settings.loadInstalledLaunchableApps
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisElectricBlue
import com.example.ui.theme.AegisEmeraldTeal
import com.example.ui.theme.AegisGoldBorder
import com.example.ui.theme.AegisIndigo
import com.example.ui.theme.AegisLavenderBorder
import com.example.ui.theme.AegisOnPrimaryWhite
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisPurple
import com.example.ui.theme.AegisTextMuted
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.AegisWarningAmber
import com.example.ui.theme.ClassicalBodyFont
import com.example.ui.theme.ClassicalDisplayFont
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class AppUsageRowUi(
    val packageName: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.ImageBitmap?,
    val usedSecondsToday: Int,
    val opensToday: Int,
    val budgetMinutes: Int,
    val isMindfulPaused: Boolean,
    val isAppLocked: Boolean
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UsageAndScrollGuardScreen(
    preferences: AegisPreferences,
    protectionRepo: ProtectionRepository,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val telemetry by protectionRepo.telemetry.collectAsState()

    val scrollLockEnabled by preferences.scrollLockEnabled.collectAsState()
    val scrollLimitCount by preferences.scrollLimitCount.collectAsState()
    val scrollLockCooldownMinutes by preferences.scrollLockCooldownMinutes.collectAsState()
    val scrollBingeNudgeCount by preferences.scrollBingeNudgeCount.collectAsState()
    val reelsGuardPlatforms by preferences.reelsGuardPlatforms.collectAsState()

    val mindfulPauseEnabled by preferences.mindfulPauseEnabled.collectAsState()
    val mindfulPausePackages by preferences.mindfulPausePackages.collectAsState()
    val mindfulGraceMinutes by preferences.mindfulGraceMinutes.collectAsState()

    val dailyReelsTotalCount by preferences.dailyReelsTotalCount.collectAsState()
    val dailyReelsByPlatform by preferences.dailyReelsByPlatform.collectAsState()
    val dailyAppUsageSeconds by preferences.dailyAppUsageSeconds.collectAsState()
    val dailyAppOpensCount by preferences.dailyAppOpensCount.collectAsState()
    val dailyMindfulExitsCount by preferences.dailyMindfulExitsCount.collectAsState()
    val dailyScrollLocksTriggered by preferences.dailyScrollLocksTriggered.collectAsState()
    val hourlyScrollBuckets by preferences.hourlyScrollBuckets.collectAsState()
    val weeklyScrollHistory by preferences.weeklyScrollHistory.collectAsState()
    val cleanStreakDays by preferences.cleanStreakDays.collectAsState()
    val recentEvents by preferences.recentMindfulScrollEvents.collectAsState()
    val scrollLockedUntilByPackage by preferences.scrollLockedUntilByPackage.collectAsState()
    val instantDetoxUntilMillis by preferences.instantDetoxUntilMillis.collectAsState()

    val appTimeBudgets by preferences.appTimeBudgets.collectAsState()
    val lockedAppPackages by preferences.lockedAppPackages.collectAsState()
    val appLockerEnabled by preferences.appLockerEnabled.collectAsState()

    val gameGuardEnabled by preferences.gameGuardEnabled.collectAsState()
    val gameAutoDetectEnabled by preferences.gameAutoDetectEnabled.collectAsState()
    val trackedGamePackages by preferences.trackedGamePackages.collectAsState()
    val gameSessionLimitMinutes by preferences.gameSessionLimitMinutes.collectAsState()
    val gameCooldownMinutes by preferences.gameCooldownMinutes.collectAsState()
    val gameDailyCapMinutes by preferences.gameDailyCapMinutes.collectAsState()
    val gameCurfewEnabled by preferences.gameCurfewEnabled.collectAsState()
    val gameCurfewStartMinutes by preferences.gameCurfewStartMinutes.collectAsState()
    val gameCurfewEndMinutes by preferences.gameCurfewEndMinutes.collectAsState()
    val gameCooldownUntilMillis by preferences.gameCooldownUntilMillis.collectAsState()
    val gameCurfewOverrideUntilMillis by preferences.gameCurfewOverrideUntilMillis.collectAsState()
    val dailyGameUsageByPackage by preferences.dailyGameUsageByPackage.collectAsState()
    val dailyGameTotalSeconds by preferences.dailyGameTotalSeconds.collectAsState()
    val dailyGameExtensionMinutes by preferences.dailyGameExtensionMinutes.collectAsState()
    val activeGameSessionSeconds by preferences.activeGameSessionSeconds.collectAsState()

    val masterKeyGate = rememberMasterKeyGate(preferences)

    var hasAccessibility by remember { mutableStateOf(telemetry.isAccessibilityActive) }
    var installedApps by remember { mutableStateOf<List<InstalledAppEntry>>(emptyList()) }
    var detectedGamePackages by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showAppPickerDialog by remember { mutableStateOf(false) }
    var showGamePickerDialog by remember { mutableStateOf(false) }
    var showPreviewBreathing by remember { mutableStateOf(false) }
    var selectedAppForControl by remember { mutableStateOf<AppUsageRowUi?>(null) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        preferences.ensureMindfulDailyRollover()
        hasAccessibility = PermissionHelper.isAccessibilityServiceEnabled(context, AegisAccessibilityService::class.java)
        installedApps = loadInstalledLaunchableApps(context)
        val detected = withContext(Dispatchers.IO) {
            GameAppDetector.discoverInstalledGamePackages(context)
        }
        detectedGamePackages = detected
        if (gameAutoDetectEnabled) {
            preferences.syncAutoDetectedGames(detected)
        }
    }

    LaunchedEffect(instantDetoxUntilMillis, scrollLockedUntilByPackage, gameCooldownUntilMillis) {
        while (instantDetoxUntilMillis > System.currentTimeMillis() ||
            scrollLockedUntilByPackage.values.any { it > System.currentTimeMillis() } ||
            gameCooldownUntilMillis > System.currentTimeMillis()
        ) {
            nowMillis = System.currentTimeMillis()
            delay(1000L)
        }
        nowMillis = System.currentTimeMillis()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasAccessibility = PermissionHelper.isAccessibilityServiceEnabled(
                    context,
                    AegisAccessibilityService::class.java
                )
                nowMillis = System.currentTimeMillis()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val totalUsageSecondsToday = remember(dailyAppUsageSeconds) {
        dailyAppUsageSeconds.values.sum()
    }

    val peakWindow = remember(hourlyScrollBuckets) {
        ShortVideoScrollDetector.computePeakScrollWindow(hourlyScrollBuckets)
    }

    val usageRows = remember(
        dailyAppUsageSeconds,
        dailyAppOpensCount,
        installedApps,
        appTimeBudgets,
        mindfulPausePackages,
        lockedAppPackages,
        appLockerEnabled
    ) {
        val byPkg = installedApps.associateBy { it.packageName }
        val allPkgs = (dailyAppUsageSeconds.keys + dailyAppOpensCount.keys + appTimeBudgets.keys + mindfulPausePackages)
            .distinct()
        allPkgs.map { pkg ->
            val installed = byPkg[pkg]
            val fallbackName = pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() }
            AppUsageRowUi(
                packageName = pkg,
                label = installed?.label ?: fallbackName,
                icon = installed?.icon,
                usedSecondsToday = dailyAppUsageSeconds[pkg] ?: 0,
                opensToday = dailyAppOpensCount[pkg] ?: 0,
                budgetMinutes = appTimeBudgets[pkg] ?: 0,
                isMindfulPaused = pkg in mindfulPausePackages,
                isAppLocked = appLockerEnabled && pkg in lockedAppPackages
            )
        }.sortedWith(
            compareByDescending<AppUsageRowUi> { it.usedSecondsToday }
                .thenByDescending { it.opensToday }
                .thenBy { it.label.lowercase() }
        )
    }

    val detoxRemainingMillis = remember(instantDetoxUntilMillis, nowMillis) {
        (instantDetoxUntilMillis - nowMillis).coerceAtLeast(0L)
    }

    SheildlenzBackdrop(modifier = Modifier.testTag("usage_scroll_guard_screen_root")) {
        Scaffold(
            contentWindowInsets = WindowInsets.statusBars,
            topBar = {
                CyberTopBar(
                    title = "Usage & Scroll Guard",
                    subtitle = "Mindful Pause & Reels/Shorts Auto-Lock",
                    onNavigateBack = onNavigateBack
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item(key = "hero_reels_usage") {
                    HeroUsageAndReelsCard(
                        dailyReelsCount = dailyReelsTotalCount,
                        scrollLimit = scrollLimitCount,
                        dailyReelsByPlatform = dailyReelsByPlatform,
                        totalUsageSecondsToday = totalUsageSecondsToday,
                        dailyGameSecondsToday = dailyGameTotalSeconds,
                        cleanStreakDays = cleanStreakDays,
                        mindfulExitsToday = dailyMindfulExitsCount,
                        isShieldActive = telemetry.isProtectionEnabled,
                        hasAccessibility = hasAccessibility,
                        onEnableAccessibility = {
                            preferences.beginAuthorizedExternalFlow()
                            runCatching { context.startActivity(PermissionHelper.getAccessibilitySettingsIntent()) }
                        },
                        onResetTodayReels = {
                            masterKeyGate.request("reset today's Reels/Shorts counters") {
                                preferences.resetTodayReelCounters()
                                Toast.makeText(context, "Today's reel counters reset", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                item(key = "header_scroll_lock") {
                    LuxurySectionHeader(title = "Scroll-Limit Lock Editor", accentColor = AegisPurple)
                }

                item(key = "scroll_limit_editor") {
                    ScrollLimitEditorCard(
                        scrollLockEnabled = scrollLockEnabled,
                        scrollLimitCount = scrollLimitCount,
                        cooldownMinutes = scrollLockCooldownMinutes,
                        bingeNudgeCount = scrollBingeNudgeCount,
                        selectedPlatforms = reelsGuardPlatforms,
                        activeScrollLockedPackages = scrollLockedUntilByPackage,
                        nowMillis = nowMillis,
                        onToggleScrollLock = { next ->
                            if (!next) {
                                masterKeyGate.request("pause auto-stop scroll lock") {
                                    preferences.setScrollLockEnabled(false)
                                }
                            } else {
                                AegisSoundEffects.playModeOn(context)
                                preferences.setScrollLockEnabled(true)
                            }
                        },
                        onChangeScrollLimit = { newLimit ->
                            if (newLimit > scrollLimitCount) {
                                masterKeyGate.request("raise Reels/Shorts limit to $newLimit") {
                                    preferences.setScrollLimitCount(newLimit)
                                }
                            } else {
                                AegisSoundEffects.playTap(context)
                                preferences.setScrollLimitCount(newLimit)
                            }
                        },
                        onChangeCooldownMinutes = { newCooldown ->
                            if (newCooldown < scrollLockCooldownMinutes) {
                                masterKeyGate.request("shorten lock cooldown to ${newCooldown}m") {
                                    preferences.setScrollLockCooldownMinutes(newCooldown)
                                }
                            } else {
                                AegisSoundEffects.playTap(context)
                                preferences.setScrollLockCooldownMinutes(newCooldown)
                            }
                        },
                        onChangeBingeNudgeCount = { newNudge ->
                            if (newNudge == 0 || (scrollBingeNudgeCount in 1 until newNudge)) {
                                masterKeyGate.request("relax binge scroll reminder") {
                                    preferences.setScrollBingeNudgeCount(newNudge)
                                }
                            } else {
                                AegisSoundEffects.playTap(context)
                                preferences.setScrollBingeNudgeCount(newNudge)
                            }
                        },
                        onTogglePlatform = { platformId ->
                            val isRemoving = platformId in reelsGuardPlatforms
                            if (isRemoving) {
                                masterKeyGate.request("unprotect platform scroll feed") {
                                    preferences.toggleReelsGuardPlatform(platformId)
                                }
                            } else {
                                AegisSoundEffects.playModeOn(context)
                                preferences.toggleReelsGuardPlatform(platformId)
                            }
                        },
                        onUnlockLockedPackage = { pkg ->
                            masterKeyGate.request("unlock scroll-limited app early") {
                                preferences.unlockScrollLockWithPin(pkg)
                                Toast.makeText(context, "App unlocked for fresh session", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                item(key = "header_gaming_control") {
                    LuxurySectionHeader(title = "Gaming Addiction Control", accentColor = AegisElectricBlue)
                }

                item(key = "gaming_addiction_control") {
                    val cooldownRemaining = (gameCooldownUntilMillis - nowMillis).coerceAtLeast(0L)
                    val isWithinCurfewNow = remember(
                        gameGuardEnabled,
                        gameCurfewEnabled,
                        gameCurfewStartMinutes,
                        gameCurfewEndMinutes,
                        gameCurfewOverrideUntilMillis,
                        nowMillis
                    ) {
                        preferences.isWithinGameCurfewWindow(nowMillis = nowMillis)
                    }
                    GamingAddictionControlCard(
                        gameGuardEnabled = gameGuardEnabled,
                        gameAutoDetectEnabled = gameAutoDetectEnabled,
                        trackedGamePackages = trackedGamePackages,
                        sessionLimitMinutes = gameSessionLimitMinutes,
                        cooldownMinutes = gameCooldownMinutes,
                        dailyCapMinutes = gameDailyCapMinutes,
                        dailyExtensionMinutes = dailyGameExtensionMinutes,
                        curfewEnabled = gameCurfewEnabled,
                        curfewStartMinutes = gameCurfewStartMinutes,
                        curfewEndMinutes = gameCurfewEndMinutes,
                        isWithinCurfewNow = isWithinCurfewNow,
                        cooldownRemainingMillis = cooldownRemaining,
                        dailyGameTotalSeconds = dailyGameTotalSeconds,
                        activeSessionSeconds = activeGameSessionSeconds,
                        dailyGameUsageByPackage = dailyGameUsageByPackage,
                        installedApps = installedApps,
                        onToggleGameGuard = { next ->
                            if (!next) {
                                masterKeyGate.request("pause Gaming Addiction Control") {
                                    preferences.setGameGuardEnabled(false)
                                }
                            } else {
                                AegisSoundEffects.playModeOn(context)
                                preferences.setGameGuardEnabled(true)
                            }
                        },
                        onToggleAutoDetect = { next ->
                            if (!next) {
                                masterKeyGate.request("disable game auto-detection") {
                                    preferences.setGameAutoDetectEnabled(false)
                                }
                            } else {
                                AegisSoundEffects.playModeOn(context)
                                preferences.setGameAutoDetectEnabled(true)
                                if (detectedGamePackages.isNotEmpty()) {
                                    preferences.syncAutoDetectedGames(detectedGamePackages)
                                }
                            }
                        },
                        onChangeSessionLimit = { newMins ->
                            if (newMins > gameSessionLimitMinutes) {
                                masterKeyGate.request("extend game session limit to ${newMins}m") {
                                    preferences.setGameSessionLimitMinutes(newMins)
                                }
                            } else {
                                AegisSoundEffects.playTap(context)
                                preferences.setGameSessionLimitMinutes(newMins)
                            }
                        },
                        onChangeCooldownMinutes = { newCooldown ->
                            if (newCooldown < gameCooldownMinutes) {
                                masterKeyGate.request("shorten gaming break cooldown to ${newCooldown}m") {
                                    preferences.setGameCooldownMinutes(newCooldown)
                                }
                            } else {
                                AegisSoundEffects.playTap(context)
                                preferences.setGameCooldownMinutes(newCooldown)
                            }
                        },
                        onChangeDailyCapMinutes = { newCap ->
                            val isLoosening = (newCap == 0 && gameDailyCapMinutes > 0) ||
                                (gameDailyCapMinutes > 0 && newCap > gameDailyCapMinutes)
                            if (isLoosening) {
                                masterKeyGate.request("relax daily gaming cap") {
                                    preferences.setGameDailyCapMinutes(newCap)
                                }
                            } else {
                                AegisSoundEffects.playTap(context)
                                preferences.setGameDailyCapMinutes(newCap)
                            }
                        },
                        onUpdateCurfew = { enabled, startMin, endMin ->
                            val isDisabling = !enabled && gameCurfewEnabled
                            val isShorteningNight = enabled && gameCurfewEnabled &&
                                (startMin > gameCurfewStartMinutes || endMin < gameCurfewEndMinutes)
                            if (isDisabling || isShorteningNight) {
                                masterKeyGate.request("relax late-night gaming curfew") {
                                    preferences.setGameCurfew(enabled, startMin, endMin)
                                }
                            } else {
                                AegisSoundEffects.playTap(context)
                                preferences.setGameCurfew(enabled, startMin, endMin)
                            }
                        },
                        onUnlockGameLockEarly = {
                            masterKeyGate.request("unlock gaming break or curfew early") {
                                preferences.unlockGameLockWithPin()
                                Toast.makeText(context, "Gaming access unlocked", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onOpenGamePicker = {
                            AegisSoundEffects.playTap(context)
                            showGamePickerDialog = true
                        }
                    )
                }

                item(key = "header_mindful_pause") {
                    LuxurySectionHeader(title = "Mindful Breathing Pause", accentColor = AegisEmeraldTeal)
                }

                item(key = "mindful_breathing_config") {
                    MindfulBreathingConfigCard(
                        mindfulPauseEnabled = mindfulPauseEnabled,
                        selectedPackagesCount = mindfulPausePackages.size,
                        graceMinutes = mindfulGraceMinutes,
                        onToggleMindfulPause = { next ->
                            if (!next) {
                                masterKeyGate.request("turn off 5s Mindful Breathing Pause") {
                                    preferences.setMindfulPauseEnabled(false)
                                }
                            } else {
                                AegisSoundEffects.playModeOn(context)
                                preferences.setMindfulPauseEnabled(true)
                            }
                        },
                        onSelectGraceMinutes = { mins ->
                            if (mins > mindfulGraceMinutes) {
                                masterKeyGate.request("extend mindful grace window to ${mins}m") {
                                    preferences.setMindfulGraceMinutes(mins)
                                }
                            } else {
                                AegisSoundEffects.playTap(context)
                                preferences.setMindfulGraceMinutes(mins)
                            }
                        },
                        onOpenAppPicker = {
                            AegisSoundEffects.playTap(context)
                            showAppPickerDialog = true
                        },
                        onPreviewBreathingPause = {
                            AegisSoundEffects.playTap(context)
                            showPreviewBreathing = true
                        }
                    )
                }

                item(key = "instant_detox_share") {
                    InstantDetoxAndShareCard(
                        detoxRemainingMillis = detoxRemainingMillis,
                        onStartDetox = { mins ->
                            AegisSoundEffects.playModeOn(context)
                            preferences.startInstantDetox(mins)
                            Toast.makeText(context, "${mins}m Instant Detox started", Toast.LENGTH_SHORT).show()
                        },
                        onStopDetox = {
                            masterKeyGate.request("stop Instant Detox early") {
                                preferences.stopInstantDetox()
                                Toast.makeText(context, "Instant Detox ended", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onShareWeeklyReport = {
                            val report = ShortVideoScrollDetector.buildWeeklySummaryReport(
                                todayUsageSeconds = totalUsageSecondsToday,
                                todayReels = dailyReelsTotalCount,
                                scrollLimit = scrollLimitCount,
                                todayExits = dailyMindfulExitsCount,
                                streakDays = cleanStreakDays,
                                peakWindow = peakWindow,
                                history = weeklyScrollHistory
                            )
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Sheildlenz Usage & Scroll Guard Report")
                                putExtra(Intent.EXTRA_TEXT, report)
                            }
                            runCatching {
                                context.startActivity(Intent.createChooser(sendIntent, "Share Weekly Summary"))
                            }
                        }
                    )
                }

                item(key = "header_trends") {
                    LuxurySectionHeader(title = "Weekly Insights & Streaks", accentColor = AegisPrimaryCyan)
                }

                item(key = "seven_day_trends") {
                    SevenDayScrollTrendsCard(
                        history = weeklyScrollHistory,
                        todayReels = dailyReelsTotalCount,
                        todayUsageSeconds = totalUsageSecondsToday,
                        todayExits = dailyMindfulExitsCount,
                        todayLocks = dailyScrollLocksTriggered,
                        scrollLimit = scrollLimitCount,
                        cleanStreakDays = cleanStreakDays,
                        peakWindow = peakWindow
                    )
                }

                item(key = "header_app_usage") {
                    LuxurySectionHeader(title = "Today's App Usage Breakdown", accentColor = AegisPrimaryCyan)
                }

                if (usageRows.isEmpty()) {
                    item(key = "empty_app_usage") {
                        CyberCard(
                            borderColor = AegisCardBorder,
                            tone = CyberCardTone.SKY,
                            modifier = Modifier.testTag("empty_usage_card")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Apps,
                                    contentDescription = null,
                                    tint = AegisPrimaryCyan,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "No App Usage Recorded Today Yet",
                                        color = AegisTextPrimary,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = ClassicalDisplayFont
                                    )
                                    Text(
                                        text = "Use your phone normally — screen time and opens appear here whether Shield is ON or OFF.",
                                        color = AegisTextSecondary,
                                        fontSize = 11.5.sp,
                                        fontFamily = ClassicalBodyFont
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { showAppPickerDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AegisPrimaryCyan,
                                    contentColor = AegisOnPrimaryWhite
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("empty_usage_configure_apps_btn")
                            ) {
                                Text("CONFIGURE DISTRACTING APPS", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    items(usageRows, key = { "usage_${it.packageName}" }) { row ->
                        CyberCard(
                            borderColor = if (row.budgetMinutes > 0 || row.isMindfulPaused) AegisLavenderBorder else AegisCardBorder,
                            tone = CyberCardTone.SKY,
                            onClick = {
                                AegisSoundEffects.playTap(context)
                                selectedAppForControl = row
                            },
                            modifier = Modifier.testTag("usage_app_row_${row.packageName}")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp)
                                ) {
                                    if (row.icon != null) {
                                        Image(
                                            bitmap = row.icon,
                                            contentDescription = row.label,
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(AegisPrimaryCyan.copy(alpha = 0.14f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = row.label.take(1).uppercase(),
                                                color = AegisPrimaryCyan,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = row.label,
                                            color = AegisTextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = ClassicalDisplayFont,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${ShortVideoScrollDetector.formatMinutesCompact(row.usedSecondsToday)} • ${row.opensToday} open(s)",
                                            color = AegisTextSecondary,
                                            fontSize = 11.5.sp,
                                            fontFamily = ClassicalBodyFont
                                        )
                                        if (row.budgetMinutes > 0 || row.isMindfulPaused || row.isAppLocked) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                if (row.budgetMinutes > 0) {
                                                    MiniControlBadge("Limit ${row.budgetMinutes}m", AegisPrimaryCyan)
                                                }
                                                if (row.isMindfulPaused) {
                                                    MiniControlBadge("5s Pause", AegisEmeraldTeal)
                                                }
                                                if (row.isAppLocked) {
                                                    MiniControlBadge("PIN Lock", AegisChampagneGold)
                                                }
                                            }
                                        }
                                    }
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Edit controls for ${row.label}",
                                    tint = AegisTextMuted
                                )
                            }
                        }
                    }
                }

                item(key = "header_recent_events") {
                    LuxurySectionHeader(title = "Recent Pause & Scroll Events", accentColor = AegisChampagneGold)
                }

                item(key = "recent_events_card") {
                    RecentMindfulEventsCard(events = recentEvents)
                }
            }
        }
    }

    if (showAppPickerDialog) {
        MindfulAppPickerDialog(
            installedApps = installedApps,
            selectedPackages = mindfulPausePackages,
            onTogglePackage = { pkg ->
                val isRemoving = pkg in mindfulPausePackages
                if (isRemoving) {
                    masterKeyGate.request("remove 5s pause from app") {
                        preferences.toggleMindfulPausePackage(pkg)
                    }
                } else {
                    AegisSoundEffects.playTap(context)
                    preferences.toggleMindfulPausePackage(pkg)
                }
            },
            onDismiss = { showAppPickerDialog = false }
        )
    }

    if (showPreviewBreathing) {
        InAppBreathingPreviewDialog(onDismiss = { showPreviewBreathing = false })
    }

    if (showGamePickerDialog) {
        TrackedGamesPickerDialog(
            installedApps = installedApps,
            detectedGamePackages = detectedGamePackages,
            selectedGamePackages = trackedGamePackages,
            onTogglePackage = { pkg ->
                val isRemoving = pkg in trackedGamePackages
                if (isRemoving) {
                    masterKeyGate.request("untrack game from addiction control") {
                        preferences.toggleTrackedGamePackage(pkg)
                    }
                } else {
                    AegisSoundEffects.playTap(context)
                    preferences.toggleTrackedGamePackage(pkg)
                }
            },
            onDismiss = { showGamePickerDialog = false }
        )
    }

    val controlRow = selectedAppForControl
    if (controlRow != null) {
        PerAppControlDialog(
            appLabel = controlRow.label,
            packageName = controlRow.packageName,
            usedSecondsToday = controlRow.usedSecondsToday,
            opensToday = controlRow.opensToday,
            currentBudgetMinutes = appTimeBudgets[controlRow.packageName] ?: 0,
            isMindfulPaused = controlRow.packageName in mindfulPausePackages,
            isAppLocked = appLockerEnabled && controlRow.packageName in lockedAppPackages,
            onSetBudgetMinutes = { mins ->
                val oldMins = appTimeBudgets[controlRow.packageName] ?: 0
                val isLoosening = (mins == 0 && oldMins > 0) || (oldMins > 0 && mins > oldMins)
                if (isLoosening) {
                    masterKeyGate.request("loosen daily limit for ${controlRow.label}") {
                        preferences.setAppTimeBudget(controlRow.packageName, mins)
                    }
                } else {
                    AegisSoundEffects.playTap(context)
                    preferences.setAppTimeBudget(controlRow.packageName, mins)
                }
            },
            onToggleMindfulPause = { enable ->
                if (!enable) {
                    masterKeyGate.request("remove 5s pause from ${controlRow.label}") {
                        if (controlRow.packageName in mindfulPausePackages) {
                            preferences.toggleMindfulPausePackage(controlRow.packageName)
                        }
                    }
                } else {
                    AegisSoundEffects.playModeOn(context)
                    if (controlRow.packageName !in mindfulPausePackages) {
                        preferences.toggleMindfulPausePackage(controlRow.packageName)
                    }
                    if (!mindfulPauseEnabled) {
                        preferences.setMindfulPauseEnabled(true)
                    }
                }
            },
            onToggleAppLock = { enable ->
                if (!enable) {
                    masterKeyGate.request("unlock ${controlRow.label} from App Locker") {
                        val next = lockedAppPackages.toMutableSet().apply { remove(controlRow.packageName) }
                        preferences.setLockedAppPackages(next)
                    }
                } else {
                    AegisSoundEffects.playModeOn(context)
                    if (!appLockerEnabled) {
                        preferences.setAppLockerEnabled(true)
                    }
                    val next = lockedAppPackages.toMutableSet().apply { add(controlRow.packageName) }
                    preferences.setLockedAppPackages(next)
                }
            },
            onDismiss = { selectedAppForControl = null }
        )
    }
}

@Composable
private fun MiniControlBadge(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(0.8.dp, color.copy(alpha = 0.28f))
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = ClassicalBodyFont,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun RecentMindfulEventsCard(events: List<MindfulScrollEvent>) {
    CyberCard(
        borderColor = AegisGoldBorder,
        tone = CyberCardTone.GOLD,
        modifier = Modifier.testTag("recent_mindful_events_card")
    ) {
        if (events.isEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = AegisEmeraldTeal,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "No mindful pauses or scroll locks triggered yet today.",
                    color = AegisTextSecondary,
                    fontSize = 12.sp,
                    fontFamily = ClassicalBodyFont
                )
            }
        } else {
            val fmt = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                events.take(8).forEach { ev ->
                    val (evIcon, evColor) = when (ev.type) {
                        MindfulScrollEvent.TYPE_MINDFUL_EXIT -> Icons.Default.CheckCircle to AegisEmeraldTeal
                        MindfulScrollEvent.TYPE_SCROLL_LOCK -> Icons.Default.HourglassBottom to AegisWarningAmber
                        MindfulScrollEvent.TYPE_DETOX_LOCK -> Icons.Default.Timer to AegisChampagneGold
                        MindfulScrollEvent.TYPE_BINGE_NUDGE -> Icons.Default.Lock to AegisPurple
                        MindfulScrollEvent.TYPE_GAME_SESSION_LOCK -> Icons.Default.SportsEsports to AegisElectricBlue
                        MindfulScrollEvent.TYPE_GAME_DAILY_CAP -> Icons.Default.Timer to AegisWarningAmber
                        MindfulScrollEvent.TYPE_GAME_CURFEW -> Icons.Default.Bedtime to AegisIndigo
                        else -> Icons.Default.SelfImprovement to AegisPrimaryCyan
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.72f))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(evIcon, contentDescription = null, tint = evColor, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ev.appLabel,
                                color = AegisTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = ClassicalBodyFont
                            )
                            Text(
                                text = ev.detail,
                                color = AegisTextSecondary,
                                fontSize = 11.sp,
                                fontFamily = ClassicalBodyFont,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = fmt.format(Date(ev.timestampMillis)),
                            color = AegisTextMuted,
                            fontSize = 10.sp,
                            fontFamily = ClassicalBodyFont
                        )
                    }
                }
            }
        }
    }
}
