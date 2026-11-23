package com.example.ui.settings

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.audio.AegisSoundEffects
import com.example.core.models.SensitivityLevel
import com.example.core.notifications.AegisNotificationHelper
import com.example.data.preferences.AegisPreferences
import com.example.data.preferences.PowerMode
import com.example.data.preferences.RecoveryQrMode
import com.example.data.repository.ProtectionRepository
import com.example.detection.classifier.ModelManager
import com.example.security.deviceowner.AegisDeviceAdminReceiver
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberCardTone
import com.example.ui.components.CyberTopBar
import com.example.ui.components.LuxurySectionHeader
import com.example.ui.components.MasterKeyGateState
import com.example.ui.components.ModelManagerCard
import com.example.ui.components.QrCodeImage
import com.example.ui.components.SheildlenzBackdrop
import com.example.ui.components.StatusPill
import com.example.ui.components.rememberMasterKeyGate
import com.example.ui.theme.AegisCardBackground
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisChampagneSoft
import com.example.ui.theme.AegisDangerRed
import com.example.ui.theme.AegisEmeraldBorder
import com.example.ui.theme.AegisEmeraldTeal
import com.example.ui.theme.AegisGoldBorder
import com.example.ui.theme.AegisGoldBright
import com.example.ui.theme.AegisIndigo
import com.example.ui.theme.AegisIndigoBorder
import com.example.ui.theme.AegisLavenderBorder
import com.example.ui.theme.AegisLavenderSoft
import com.example.ui.theme.AegisOnPrimaryWhite
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisPurple
import com.example.ui.theme.AegisSurfaceElevated
import com.example.ui.theme.AegisTextMuted
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.AegisWarningAmber
import com.example.ui.theme.ClassicalBodyFont
import com.example.ui.theme.ClassicalDisplayFont
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class InstalledAppEntry(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?
)

internal fun drawableToScaledBitmap(drawable: Drawable, sizePx: Int = 72): ImageBitmap? {
    return try {
        val raw = if (drawable is BitmapDrawable && drawable.bitmap != null) {
            drawable.bitmap
        } else {
            val bmp = Bitmap.createBitmap(
                drawable.intrinsicWidth.coerceAtLeast(1),
                drawable.intrinsicHeight.coerceAtLeast(1),
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bmp)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bmp
        }
        Bitmap.createScaledBitmap(raw, sizePx, sizePx, true).asImageBitmap()
    } catch (_: Exception) {
        null
    }
}

internal suspend fun loadInstalledLaunchableApps(context: Context): List<InstalledAppEntry> =
    withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val activities = pm.queryIntentActivities(intent, 0)
        activities
            .mapNotNull { resolveInfo ->
                val pkg = resolveInfo.activityInfo?.packageName ?: return@mapNotNull null
                if (pkg == context.packageName) return@mapNotNull null
                val label = resolveInfo.loadLabel(pm)?.toString()?.trim().orEmpty().ifBlank { pkg }
                val icon = try {
                    drawableToScaledBitmap(resolveInfo.loadIcon(pm))
                } catch (_: Exception) {
                    null
                }
                InstalledAppEntry(packageName = pkg, label = label, icon = icon)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

internal fun shouldLockEditsScreen(
    hasGuardianPin: Boolean,
    isSessionUnlocked: Boolean
): Boolean = hasGuardianPin && !isSessionUnlocked

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    preferences: AegisPreferences,
    modelManager: ModelManager,
    protectionRepo: ProtectionRepository,
    onModelUpdated: () -> Unit = {},
    onNavigateToStatus: () -> Unit = {},
    onNavigateToParents: () -> Unit = {}
) {
    val context = LocalContext.current
    val telemetry by protectionRepo.telemetry.collectAsState()
    val activeAppName by protectionRepo.activeAppLabel.collectAsState()
    val sensitivity by preferences.sensitivity.collectAsState()
    val isStrictEverything by preferences.strictEverything.collectAsState()
    val isAutoStart by preferences.autoStart.collectAsState()
    val hasPin by preferences.hasGuardianPin.collectAsState()
    val isGuardianUnlocked by preferences.isGuardianSessionUnlocked.collectAsState()
    val parentalRole by preferences.parentalRole.collectAsState()
    val pairedParentName by preferences.pairedParentName.collectAsState()
    val guardianEmail by preferences.guardianEmail.collectAsState()
    val challengeCode by preferences.recoveryChallengeCode.collectAsState()
    val isMaintenanceMode by preferences.maintenanceMode.collectAsState()
    val lockdownDurationMinutes by preferences.lockdownDurationMinutes.collectAsState()
    val waitCooldownBeforePin by preferences.waitCooldownBeforePassword.collectAsState()
    val antiTamperEnabled by preferences.antiTamperEnabled.collectAsState()
    val powerMode by preferences.powerMode.collectAsState()
    val skipDuplicateFrames by preferences.skipDuplicateFrames.collectAsState()
    val captureWidthPx by preferences.captureWidthPx.collectAsState()
    val pauseWhenScreenOff by preferences.pauseWhenScreenOff.collectAsState()
    val adaptiveCadenceEnabled by preferences.adaptiveCadenceEnabled.collectAsState()
    val autoThrottleLowBattery by preferences.autoThrottleLowBattery.collectAsState()
    val isAdvancedUnlocked by preferences.advancedOptionsUnlocked.collectAsState()

    val isEditsLocked = shouldLockEditsScreen(hasPin, isGuardianUnlocked)
    val stage1DurationLabel = AegisPreferences.formatMinutesLabel(lockdownDurationMinutes)

    var showModelDialog by remember { mutableStateOf(false) }
    var showPrecisionLabDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var showDeviceOwnerWizard by remember { mutableStateOf(false) }

    var isDeviceAdmin by remember { mutableStateOf(AegisDeviceAdminReceiver.isDeviceAdmin(context)) }
    val masterKeyGate = rememberMasterKeyGate(preferences)

    SheildlenzBackdrop(modifier = Modifier.testTag("settings_screen_root")) {
        Scaffold(
            contentWindowInsets = WindowInsets.statusBars,
            topBar = {
                CyberTopBar(
                    title = stringResource(R.string.nav_edits),
                    subtitle = if (isEditsLocked) "Password & Parent Protected" else "Protection & Policy Controls"
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            if (isEditsLocked) {
                EditsPasswordLockGate(
                    preferences = preferences,
                    parentalRole = parentalRole,
                    pairedParentName = pairedParentName,
                    guardianEmail = guardianEmail,
                    challengeCode = challengeCode,
                    onNavigateToStatus = onNavigateToStatus,
                    onNavigateToParents = onNavigateToParents,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp)
                )
            } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (hasPin) {
                    item {
                        CyberCard(
                            borderColor = AegisEmeraldBorder,
                            tone = CyberCardTone.SKY,
                            modifier = Modifier.testTag("edits_unlocked_banner")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f).padding(end = 10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(AegisEmeraldTeal),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LockOpen,
                                            contentDescription = "Edits Unlocked",
                                            tint = AegisOnPrimaryWhite,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Guardian Edits Unlocked",
                                            color = AegisTextPrimary,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = ClassicalDisplayFont
                                        )
                                        Text(
                                            text = if (parentalRole == "child") {
                                                "Also syncs remotely from Parent phone"
                                            } else {
                                                "Lock again before handing phone back"
                                            },
                                            color = AegisTextSecondary,
                                            fontSize = 11.sp,
                                            fontFamily = ClassicalBodyFont
                                        )
                                    }
                                }
                                Button(
                                    onClick = {
                                        AegisSoundEffects.playTap(context)
                                        preferences.lockGuardianSession()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AegisPurple,
                                        contentColor = AegisOnPrimaryWhite
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("edits_lock_now_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("LOCK EDITS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                // 1. AI Classification
                item {
                    LuxurySectionHeader(title = "AI Classification", accentColor = AegisPurple)
                }
                item {
                    EditsActionCard(
                        title = "AI Model",
                        summary = if (telemetry.isModelLoaded) "Active • ${telemetry.loadedModelName ?: "On-Device NPU"}" else "No Model Selected • Tap to load",
                        icon = Icons.Default.Memory,
                        accentColor = AegisPurple,
                        tone = CyberCardTone.LAVENDER,
                        borderColor = AegisLavenderBorder,
                        testTag = "ai_model_pill",
                        buttonTestTag = "ai_model_pill_button",
                        onClick = {
                            AegisSoundEffects.playTap(context)
                            showModelDialog = true
                        }
                    )
                }
                item {
                    CollapsibleEditsCard(
                        title = "AI Sensitivity",
                        summary = "${sensitivity.displayName}${if (isStrictEverything) " • Strict All" else ""}",
                        icon = Icons.Default.Tune,
                        accentColor = AegisPurple,
                        tone = CyberCardTone.LAVENDER,
                        borderColor = AegisLavenderBorder,
                        testTag = "thresholds_accordion",
                        compactPillMode = true,
                        isPillActive = isStrictEverything || sensitivity == SensitivityLevel.STRICT,
                        dialogTitle = "AI Sensitivity & Strictness"
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SensitivityLevel.entries.forEach { level ->
                                val isSelected = (sensitivity == level)
                                val shortTitle = when (level) {
                                    SensitivityLevel.STRICT -> "Strict"
                                    SensitivityLevel.BALANCED -> "Balanced"
                                    SensitivityLevel.PERMISSIVE -> "Permissive"
                                }
                                val levelAccent = AegisPurple
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) levelAccent else AegisCardBackground.copy(alpha = 0.8f))
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) levelAccent else AegisLavenderBorder,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            if (level != sensitivity) {
                                                if (level.ordinal > sensitivity.ordinal) {
                                                    masterKeyGate.request("relax sensitivity to ${level.displayName}") {
                                                        preferences.setSensitivity(level)
                                                    }
                                                } else {
                                                    AegisSoundEffects.playModeOn(context)
                                                    preferences.setSensitivity(level)
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = shortTitle,
                                        color = if (isSelected) AegisOnPrimaryWhite else AegisTextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = ClassicalBodyFont,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = AegisLavenderBorder.copy(alpha = 0.45f), modifier = Modifier.padding(vertical = 8.dp))

                        CompactSwitchRow(
                            title = "Strict Everything Mode",
                            checked = isStrictEverything,
                            activeColor = AegisPurple,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    AegisSoundEffects.playModeOn(context)
                                    preferences.setStrictEverything(true)
                                } else {
                                    masterKeyGate.request("disable Strict Everything") {
                                        preferences.setStrictEverything(false)
                                    }
                                }
                            },
                            testTag = "strict_everything_switch"
                        )
                    }
                }
                item {
                    AiAcceleratorAndCategoriesSection(
                        preferences = preferences,
                        masterKeyGate = masterKeyGate,
                        compactPillMode = true
                    )
                }

                // 2. Protection Controls
                item {
                    LuxurySectionHeader(title = "Protection", accentColor = AegisPrimaryCyan)
                }
                item {
                    SafeDnsAndBlocklistSection(
                        preferences = preferences,
                        masterKeyGate = masterKeyGate,
                        compactPillMode = true
                    )
                }
                item {
                    WarningThresholdsSection(
                        preferences = preferences,
                        masterKeyGate = masterKeyGate,
                        compactPillMode = true
                    )
                }
                item {
                    CollapsibleEditsCard(
                        title = "Power Mode",
                        summary = if (telemetry.isLowBatteryThrottled) {
                            "${powerMode.displayName} • Throttled (<20% battery)"
                        } else {
                            "${powerMode.displayName} • ${powerMode.scanIntervalMs}ms"
                        },
                        icon = Icons.Default.Bolt,
                        accentColor = AegisPrimaryCyan,
                        tone = CyberCardTone.SKY,
                        borderColor = AegisCardBorder,
                        testTag = "power_mode_widget",
                        compactPillMode = true,
                        isPillActive = telemetry.isLowBatteryThrottled || powerMode != PowerMode.BALANCED,
                        dialogTitle = "Power Mode & Scan Cadence"
                    ) {
                        if (telemetry.isLowBatteryThrottled) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                StatusPill(
                                    label = "THROTTLED",
                                    isActive = false,
                                    activeColor = AegisPrimaryCyan,
                                    inactiveColor = AegisChampagneGold
                                )
                            }
                        }

                        val selectPowerMode: (PowerMode) -> Unit = { targetMode ->
                            if (targetMode != powerMode) {
                                if (targetMode.scanIntervalMs > powerMode.scanIntervalMs) {
                                    masterKeyGate.request("switch to ${targetMode.displayName}") {
                                        preferences.setPowerMode(targetMode)
                                    }
                                } else {
                                    AegisSoundEffects.playModeOn(context)
                                    preferences.setPowerMode(targetMode)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PowerModeTile(
                                mode = PowerMode.ADAPTIVE_ECO,
                                icon = Icons.Default.AutoAwesome,
                                accentColor = AegisPrimaryCyan,
                                selected = powerMode == PowerMode.ADAPTIVE_ECO,
                                onClick = { selectPowerMode(PowerMode.ADAPTIVE_ECO) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("power_mode_adaptive_eco")
                            )
                            PowerModeTile(
                                mode = PowerMode.POWER_SAVING,
                                icon = Icons.Default.BatterySaver,
                                accentColor = AegisPurple,
                                selected = powerMode == PowerMode.POWER_SAVING,
                                onClick = { selectPowerMode(PowerMode.POWER_SAVING) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("power_mode_power_saving")
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PowerModeTile(
                                mode = PowerMode.BALANCED,
                                icon = Icons.Default.Tune,
                                accentColor = AegisPurple,
                                selected = powerMode == PowerMode.BALANCED,
                                onClick = { selectPowerMode(PowerMode.BALANCED) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("power_mode_balanced")
                            )
                            PowerModeTile(
                                mode = PowerMode.EXTREME,
                                icon = Icons.Default.Bolt,
                                accentColor = AegisChampagneGold,
                                selected = powerMode == PowerMode.EXTREME,
                                onClick = { selectPowerMode(PowerMode.EXTREME) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("power_mode_extreme")
                            )
                        }

                        HorizontalDivider(
                            color = AegisCardBorder.copy(alpha = 0.45f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        CompactSwitchRow(
                            title = "Auto-Throttle (<20% Battery)",
                            checked = autoThrottleLowBattery,
                            onCheckedChange = { throttle ->
                                if (throttle) {
                                    masterKeyGate.request("enable low-battery throttling") {
                                        preferences.setAutoThrottleLowBattery(true)
                                    }
                                } else {
                                    AegisSoundEffects.playModeOn(context)
                                    preferences.setAutoThrottleLowBattery(false)
                                }
                            },
                            testTag = "power_opt_low_battery_throttle"
                        )
                        CompactSwitchRow(
                            title = "Adaptive Idle Backoff",
                            checked = adaptiveCadenceEnabled,
                            onCheckedChange = { backoff ->
                                if (backoff) {
                                    masterKeyGate.request("enable idle scan backoff") {
                                        preferences.setAdaptiveCadenceEnabled(true)
                                    }
                                } else {
                                    AegisSoundEffects.playModeOn(context)
                                    preferences.setAdaptiveCadenceEnabled(false)
                                }
                            },
                            testTag = "power_opt_adaptive_cadence"
                        )
                        CompactSwitchRow(
                            title = "Skip Static Frames",
                            checked = skipDuplicateFrames,
                            onCheckedChange = { skip ->
                                if (skip) {
                                    masterKeyGate.request("skip static frames") {
                                        preferences.setSkipDuplicateFrames(true)
                                    }
                                } else {
                                    AegisSoundEffects.playModeOn(context)
                                    preferences.setSkipDuplicateFrames(false)
                                }
                            },
                            testTag = "power_opt_skip_duplicates"
                        )
                        CompactSwitchRow(
                            title = "Compact Capture (256px)",
                            checked = captureWidthPx <= 320,
                            onCheckedChange = { compact ->
                                if (compact) {
                                    masterKeyGate.request("lower capture resolution to 256px") {
                                        preferences.setCompactPowerResolution(true)
                                    }
                                } else {
                                    AegisSoundEffects.playModeOn(context)
                                    preferences.setCompactPowerResolution(false)
                                }
                            },
                            testTag = "power_opt_compact_capture"
                        )
                        CompactSwitchRow(
                            title = "Pause on Screen Off",
                            checked = pauseWhenScreenOff,
                            onCheckedChange = { pauseOff ->
                                if (pauseOff) {
                                    masterKeyGate.request("pause scanning on screen off") {
                                        preferences.setPauseWhenScreenOff(true)
                                    }
                                } else {
                                    AegisSoundEffects.playModeOn(context)
                                    preferences.setPauseWhenScreenOff(false)
                                }
                            },
                            testTag = "power_opt_pause_screen_off"
                        )
                    }
                }

                // 3. Schedules & Apps
                item {
                    LuxurySectionHeader(title = "Schedules & Apps", accentColor = AegisEmeraldTeal)
                }
                item {
                    ScheduledProtectionSection(
                        preferences = preferences,
                        masterKeyGate = masterKeyGate,
                        compactPillMode = true
                    )
                }
                item {
                    BedtimeAndBudgetsSection(
                        preferences = preferences,
                        masterKeyGate = masterKeyGate,
                        compactPillMode = true
                    )
                }
                item {
                    WhitelistedAppsSection(
                        preferences = preferences,
                        compactPillMode = true
                    )
                }

                // 4. Security & Recovery
                item {
                    LuxurySectionHeader(title = "Security & Recovery", accentColor = AegisChampagneGold)
                }
                item {
                    EditsActionCard(
                        title = if (hasPin) "Guardian PIN" else "Set PIN",
                        summary = if (hasPin) "Passcode Configured • Protected" else "Not Set • Tap to set security PIN",
                        icon = Icons.Default.Lock,
                        accentColor = AegisChampagneGold,
                        tone = CyberCardTone.GOLD,
                        borderColor = AegisGoldBorder,
                        testTag = "guardian_pin_change_button",
                        buttonTestTag = "guardian_pin_change_button_action",
                        onClick = {
                            AegisSoundEffects.playTap(context)
                            masterKeyGate.request(if (hasPin) "change Guardian PIN" else "set Guardian PIN") {
                                pinInput = ""
                                confirmPinInput = ""
                                pinError = null
                                showPinDialog = true
                            }
                        }
                    )
                }
                item {
                    GuardianRecoverySection(
                        preferences = preferences,
                        masterKeyGate = masterKeyGate,
                        compactPillMode = true
                    )
                }
                item {
                    CollapsibleEditsCard(
                        title = "Resistance",
                        summary = "$stage1DurationLabel Penalty • ${if (antiTamperEnabled) "Tamper Lock ON" else "Standard"}",
                        icon = Icons.Default.Security,
                        accentColor = AegisChampagneGold,
                        tone = CyberCardTone.GOLD,
                        borderColor = AegisGoldBorder,
                        testTag = "resistance_accordion",
                        compactPillMode = true,
                        isPillActive = antiTamperEnabled,
                        dialogTitle = "Tamper Resistance & Penalty"
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Lockdown Penalty",
                                color = AegisTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = ClassicalDisplayFont
                            )
                            Text(
                                text = stage1DurationLabel,
                                color = AegisWarningAmber,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(1, 4, 10, 24).forEach { hrs ->
                                val selected = (lockdownDurationMinutes == hrs * 60)
                                OutlinedButton(
                                    onClick = {
                                        val targetMinutes = hrs * 60
                                        if (targetMinutes < lockdownDurationMinutes) {
                                            masterKeyGate.request("lower lockdown penalty to ${hrs}H") {
                                                preferences.setLockdownHours(hrs)
                                            }
                                        } else if (targetMinutes > lockdownDurationMinutes) {
                                            AegisSoundEffects.playModeOn(context)
                                            preferences.setLockdownHours(hrs)
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (selected) AegisChampagneGold else AegisCardBackground,
                                        contentColor = if (selected) AegisOnPrimaryWhite else AegisTextPrimary
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Text("${hrs}H", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = ClassicalBodyFont)
                                }
                            }
                        }

                        HorizontalDivider(color = AegisCardBorder.copy(alpha = 0.45f), modifier = Modifier.padding(vertical = 8.dp))

                        CompactSwitchRow(
                            title = "Cooldown Before PIN",
                            checked = waitCooldownBeforePin,
                            activeColor = AegisChampagneGold,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    AegisSoundEffects.playModeOn(context)
                                    preferences.setWaitCooldownBeforePassword(true)
                                } else {
                                    masterKeyGate.request("disable cooldown before PIN") {
                                        preferences.setWaitCooldownBeforePassword(false)
                                    }
                                }
                            },
                            testTag = "cooldown_before_pin_switch"
                        )
                        CompactSwitchRow(
                            title = "Anti-Tamper Lock",
                            checked = antiTamperEnabled,
                            activeColor = AegisChampagneGold,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    AegisSoundEffects.playModeOn(context)
                                    preferences.setAntiTamperEnabled(true)
                                } else {
                                    masterKeyGate.request("disable anti-tamper lock") {
                                        preferences.setAntiTamperEnabled(false)
                                    }
                                }
                            },
                            testTag = "anti_tamper_switch"
                        )
                        CompactSwitchRow(
                            title = "Auto-Start on Boot",
                            checked = isAutoStart,
                            activeColor = AegisChampagneGold,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    AegisSoundEffects.playModeOn(context)
                                    preferences.setAutoStart(true)
                                } else {
                                    masterKeyGate.request("disable boot auto-start") {
                                        preferences.setAutoStart(false)
                                    }
                                }
                            },
                            testTag = "auto_start_boot_switch"
                        )
                        CompactSwitchRow(
                            title = "Maintenance Mode",
                            checked = isMaintenanceMode,
                            titleColor = AegisDangerRed,
                            activeColor = AegisChampagneGold,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    masterKeyGate.request("enable Maintenance Mode") {
                                        preferences.setMaintenanceMode(true)
                                    }
                                } else {
                                    AegisSoundEffects.playModeOn(context)
                                    preferences.setMaintenanceMode(false)
                                }
                            },
                            testTag = "maintenance_mode_switch"
                        )

                        if (!isDeviceAdmin) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    preferences.grantTemporarySettingsAuthorization()
                                    context.startActivity(AegisDeviceAdminReceiver.createActivateAdminIntent(context))
                                    isDeviceAdmin = AegisDeviceAdminReceiver.isDeviceAdmin(context)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AegisPrimaryCyan,
                                    contentColor = AegisOnPrimaryWhite
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("ACTIVATE DEVICE ADMIN", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = ClassicalBodyFont)
                            }
                        }
                    }
                }
                item {
                    DeviceOwnerSetupSection(
                        onOpenWizard = { showDeviceOwnerWizard = true },
                        compactPillMode = true
                    )
                }

                // 5. System & Language
                item {
                    LuxurySectionHeader(title = "System & Language", accentColor = AegisIndigo)
                }
                item {
                    NotificationsControlCard(
                        preferences = preferences,
                        masterKeyGate = masterKeyGate
                    )
                }
                item {
                    BlockingScreenStyleCard(
                        preferences = preferences,
                        masterKeyGate = masterKeyGate
                    )
                }
                item {
                    TouchSoundsCard(preferences = preferences, compactPillMode = true)
                }
                item {
                    PolicyBackupRestoreSection(
                        preferences = preferences,
                        masterKeyGate = masterKeyGate,
                        compactPillMode = true
                    )
                }
                item {
                    LanguageSelectorSection(
                        preferences = preferences,
                        compactPillMode = true
                    )
                }
                if (isAdvancedUnlocked) {
                    item {
                        EditsActionCard(
                            title = "Precision Lab",
                            summary = "Fine-grained model thresholds & screen frame pacing",
                            icon = Icons.Default.AutoAwesome,
                            accentColor = AegisIndigo,
                            tone = CyberCardTone.INDIGO,
                            borderColor = AegisIndigoBorder,
                            testTag = "precision_lab_pill",
                            buttonTestTag = "precision_lab_button",
                            onClick = {
                                AegisSoundEffects.playTap(context)
                                showPrecisionLabDialog = true
                            }
                        )
                    }
                }
            }

            if (showModelDialog) {
                EditsPopupDialog(
                    title = "AI Classification",
                    subtitle = telemetry.loadedModelName ?: "Attach or download a .tflite model",
                    icon = Icons.Default.Memory,
                    accentColor = AegisPurple,
                    onDismiss = { showModelDialog = false },
                    testTag = "ai_model_pill_dialog"
                ) {
                    ModelManagerCard(
                        telemetry = telemetry,
                        modelManager = modelManager,
                        onModelUpdated = onModelUpdated,
                        cardTone = CyberCardTone.LAVENDER,
                        showCloudDownloader = true,
                        activeAppName = activeAppName,
                        onBeforeOpenPicker = { preferences.beginAuthorizedExternalFlow() },
                        onRequireAuth = { label, action ->
                            masterKeyGate.request(label) { action() }
                        }
                    )
                }
            }

            if (showPrecisionLabDialog && isAdvancedUnlocked) {
                EditsPopupDialog(
                    title = "Advanced Precision Lab",
                    subtitle = "Custom sensitivity & frame tuning",
                    icon = Icons.Default.AutoAwesome,
                    accentColor = AegisChampagneGold,
                    onDismiss = { showPrecisionLabDialog = false },
                    testTag = "precision_lab_dialog"
                ) {
                    AdvancedPrecisionLabSection(preferences = preferences)
                }
            }



            if (showDeviceOwnerWizard) {
                DeviceOwnerWizardDialog(
                    preferences = preferences,
                    onDismiss = { showDeviceOwnerWizard = false },
                    onStatusChanged = { isDeviceAdmin = AegisDeviceAdminReceiver.isDeviceAdmin(context) }
                )
            }

            if (showPinDialog) {
                val confirmFocusRequester = remember { FocusRequester() }
                val saveNewPin: () -> Unit = {
                    when {
                        pinInput.length < 4 -> {
                            pinError = "PIN must be at least 4 digits"
                        }
                        pinInput != confirmPinInput -> {
                            pinError = "PINs do not match"
                            confirmPinInput = ""
                        }
                        else -> {
                            preferences.setGuardianPin(pinInput)
                            showPinDialog = false
                        }
                    }
                }
                AlertDialog(
                    onDismissRequest = { showPinDialog = false },
                    containerColor = AegisLavenderSoft,
                    titleContentColor = AegisTextPrimary,
                    title = {
                        Text(
                            text = if (hasPin) "Change Guardian PIN" else "Set Guardian PIN",
                            fontFamily = ClassicalDisplayFont,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = "4 to 8 digit passcode for console & unlock control.",
                                color = AegisTextSecondary,
                                fontSize = 13.sp,
                                fontFamily = ClassicalBodyFont
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedTextField(
                                value = pinInput,
                                onValueChange = {
                                    if (it.length <= 8 && it.all { char -> char.isDigit() }) {
                                        pinInput = it
                                        pinError = null
                                    }
                                },
                                label = { Text("New Guardian PIN") },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.NumberPassword,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { confirmFocusRequester.requestFocus() }
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                isError = pinError != null,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AegisPurple,
                                    unfocusedBorderColor = AegisLavenderBorder,
                                    focusedContainerColor = AegisCardBackground,
                                    unfocusedContainerColor = AegisCardBackground,
                                    focusedTextColor = AegisTextPrimary,
                                    unfocusedTextColor = AegisTextPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onPreviewKeyEvent { event ->
                                        if (event.key == Key.Enter || event.key == Key.NumPadEnter) {
                                            if (event.type == KeyEventType.KeyDown) {
                                                confirmFocusRequester.requestFocus()
                                            }
                                            true
                                        } else {
                                            false
                                        }
                                    }
                                    .testTag("change_pin_input")
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = confirmPinInput,
                                onValueChange = {
                                    if (it.length <= 8 && it.all { char -> char.isDigit() }) {
                                        confirmPinInput = it
                                        pinError = null
                                    }
                                },
                                label = { Text("Confirm New PIN") },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.NumberPassword,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { saveNewPin() }
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                isError = pinError != null,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AegisPurple,
                                    unfocusedBorderColor = AegisLavenderBorder,
                                    focusedContainerColor = AegisCardBackground,
                                    unfocusedContainerColor = AegisCardBackground,
                                    focusedTextColor = AegisTextPrimary,
                                    unfocusedTextColor = AegisTextPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(confirmFocusRequester)
                                    .onPreviewKeyEvent { event ->
                                        if (event.key == Key.Enter || event.key == Key.NumPadEnter) {
                                            if (event.type == KeyEventType.KeyDown) {
                                                saveNewPin()
                                            }
                                            true
                                        } else {
                                            false
                                        }
                                    }
                                    .testTag("change_pin_confirm_input")
                            )
                            if (pinError != null) {
                                Text(
                                    text = pinError!!,
                                    color = AegisWarningAmber,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = saveNewPin,
                            colors = ButtonDefaults.buttonColors(containerColor = AegisPurple, contentColor = AegisOnPrimaryWhite),
                            modifier = Modifier.testTag("change_pin_save_button")
                        ) {
                            Text(text = "SAVE PIN", fontWeight = FontWeight.Bold)
                        }
                    },
                        dismissButton = {
                            OutlinedButton(
                                onClick = { showPinDialog = false },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = AegisChampagneSoft,
                                    contentColor = AegisTextPrimary
                                )
                            ) {
                                Text(text = "CANCEL", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EditsPasswordLockGate(
    preferences: AegisPreferences,
    parentalRole: String?,
    pairedParentName: String?,
    guardianEmail: String,
    challengeCode: String,
    onNavigateToStatus: () -> Unit,
    onNavigateToParents: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var passwordInput by remember { mutableStateOf("") }
    var recoveryCodeInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var showForgotQr by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val submitUnlock: () -> Unit = {
        if (showForgotQr) {
            val cleanPin = newPinInput.trim()
            if (cleanPin.length !in 4..8 || !cleanPin.all { it.isDigit() }) {
                errorMessage = "Enter a new 4–8 digit Guardian Password"
            } else if (!preferences.resetPinWithRecoveryToken(recoveryCodeInput, cleanPin)) {
                errorMessage = "Invalid recovery code for Challenge ID $challengeCode"
            } else {
                AegisSoundEffects.playModeOn(context)
                preferences.unlockGuardianSession()
                errorMessage = null
            }
        } else {
            if (passwordInput.isEmpty() || !preferences.verifyGuardianPin(passwordInput)) {
                errorMessage = "Incorrect Guardian Password"
                passwordInput = ""
            } else {
                AegisSoundEffects.playModeOn(context)
                preferences.unlockGuardianSession()
                errorMessage = null
            }
        }
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        CyberCard(
            borderColor = AegisGoldBorder,
            tone = CyberCardTone.LAVENDER,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("edits_lock_gate_card")
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(AegisChampagneGold, AegisPurple)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Edits Password Protected",
                        tint = AegisOnPrimaryWhite,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (showForgotQr) "Parent QR Password Reset" else "Edits Screen Locked",
                    color = AegisTextPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalDisplayFont,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (parentalRole == "child") {
                        "Child View Only • Edit permissions require the Guardian Password or remote update from ${pairedParentName?.takeIf { it.isNotBlank() } ?: "Parent"}'s mobile phone."
                    } else {
                        "Enter Guardian Password to view & edit protection policies."
                    },
                    color = AegisTextSecondary,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                    fontFamily = ClassicalBodyFont,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("edits_child_parent_notice")
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (parentalRole == "child") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(AegisChampagneSoft)
                            .border(1.dp, AegisGoldBorder, RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SupervisorAccount,
                            contentDescription = "Controlled by Parent Phone",
                            tint = AegisChampagneGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Managed remotely from Parent Mobile Phone (${pairedParentName?.takeIf { it.isNotBlank() } ?: "Family Link"})",
                            color = AegisTextPrimary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = ClassicalBodyFont
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (showForgotQr) {
                    val qrPayload = remember(challengeCode, guardianEmail) {
                        preferences.buildRecoveryQrUri(RecoveryQrMode.PIN_RESET)
                    }
                    Text(
                        text = "Scan on parent's phone (${guardianEmail.ifBlank { "Guardian Google account" }}) to generate an 8-char reset code.",
                        color = AegisTextSecondary,
                        fontSize = 11.5.sp,
                        fontFamily = ClassicalBodyFont,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    QrCodeImage(
                        payload = qrPayload,
                        padding = 10.dp,
                        modifier = Modifier
                            .size(164.dp)
                            .border(1.2.dp, AegisGoldBorder, RoundedCornerShape(16.dp)),
                        testTag = "edits_forgot_qr_matrix"
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Challenge ID: $challengeCode",
                        color = AegisTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = recoveryCodeInput,
                        onValueChange = {
                            recoveryCodeInput = it.uppercase()
                            errorMessage = null
                        },
                        label = { Text("8-Char One-Time Code", fontFamily = ClassicalBodyFont) },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = AegisChampagneGold) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AegisChampagneGold,
                            unfocusedBorderColor = AegisLavenderBorder,
                            focusedContainerColor = AegisCardBackground,
                            unfocusedContainerColor = AegisCardBackground
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edits_recovery_code_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = {
                            if (it.length <= 8 && it.all { ch -> ch.isDigit() }) {
                                newPinInput = it
                                errorMessage = null
                            }
                        },
                        label = { Text("New Guardian Password (4–8 digits)", fontFamily = ClassicalBodyFont) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = AegisPurple) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { submitUnlock() }),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AegisPurple,
                            unfocusedBorderColor = AegisLavenderBorder,
                            focusedContainerColor = AegisCardBackground,
                            unfocusedContainerColor = AegisCardBackground
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edits_new_pin_input")
                    )
                } else {
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = {
                            if (it.length <= 8 && it.all { ch -> ch.isDigit() }) {
                                passwordInput = it
                                errorMessage = null
                            }
                        },
                        label = { Text("Guardian Password (4–8 digits)", fontFamily = ClassicalBodyFont) },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = AegisPurple) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { submitUnlock() }),
                        singleLine = true,
                        isError = errorMessage != null,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AegisPurple,
                            unfocusedBorderColor = AegisLavenderBorder,
                            focusedContainerColor = AegisCardBackground,
                            unfocusedContainerColor = AegisCardBackground,
                            focusedTextColor = AegisTextPrimary,
                            unfocusedTextColor = AegisTextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onPreviewKeyEvent { event ->
                                if (event.key == Key.Enter || event.key == Key.NumPadEnter) {
                                    if (event.type == KeyEventType.KeyDown) {
                                        submitUnlock()
                                    }
                                    true
                                } else {
                                    false
                                }
                            }
                            .testTag("edits_unlock_pin_input")
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage!!,
                        color = AegisDangerRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = ClassicalBodyFont,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = submitUnlock,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showForgotQr) AegisChampagneGold else AegisPurple,
                        contentColor = AegisOnPrimaryWhite
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("edits_unlock_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (showForgotQr) "RESET PASSWORD & UNLOCK" else "UNLOCK EDITS",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalBodyFont
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                TextButton(
                    onClick = {
                        showForgotQr = !showForgotQr
                        errorMessage = null
                    },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("edits_forgot_pin_button")
                ) {
                    Text(
                        text = if (showForgotQr) "Back to Password Entry" else "Forgot Password? Parent QR Unlock",
                        color = AegisPurple,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalBodyFont
                    )
                }
            }
        }

        CyberCard(
            borderColor = AegisCardBorder,
            tone = CyberCardTone.SKY,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Only want to check current settings?",
                    color = AegisTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalDisplayFont
                )
                Text(
                    text = "The Status tab displays every active protection policy, DNS state, study timer, and parent link in read-only mode without needing the password.",
                    color = AegisTextSecondary,
                    fontSize = 12.sp,
                    fontFamily = ClassicalBodyFont
                )
                OutlinedButton(
                    onClick = {
                        AegisSoundEffects.playTap(context)
                        onNavigateToStatus()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = AegisCardBackground,
                        contentColor = AegisPrimaryCyan
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("edits_view_status_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MonitorHeart,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "VIEW STATUS (READ-ONLY)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalBodyFont
                    )
                }

                if (parentalRole == "child") {
                    TextButton(
                        onClick = {
                            AegisSoundEffects.playTap(context)
                            onNavigateToParents()
                        },
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .heightIn(min = 48.dp)
                            .testTag("edits_open_parents_button")
                    ) {
                        Text(
                            text = "Open Parents Link Screen",
                            color = AegisEmeraldTeal,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = ClassicalBodyFont
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun EditsActionCard(
    title: String,
    summary: String,
    icon: ImageVector,
    accentColor: Color,
    actionLabel: String = "CHANGE",
    actionIcon: ImageVector = Icons.Default.Tune,
    testTag: String,
    buttonTestTag: String = if (testTag.isNotBlank()) "${testTag}_button" else "",
    tone: CyberCardTone = CyberCardTone.SKY,
    borderColor: Color = AegisCardBorder,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    CyberCard(
        borderColor = borderColor,
        tone = tone,
        onClick = {
            AegisSoundEffects.playTap(context)
            onClick()
        },
        modifier = modifier
            .fillMaxWidth()
            .then(if (testTag.isNotBlank()) Modifier.testTag(testTag) else Modifier)
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
                    .padding(end = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.20f),
                                    accentColor.copy(alpha = 0.08f)
                                )
                            )
                        )
                        .border(1.dp, accentColor.copy(alpha = 0.34f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = AegisTextPrimary,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalDisplayFont,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = summary,
                        color = AegisTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = ClassicalBodyFont,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Button(
                onClick = {
                    AegisSoundEffects.playTap(context)
                    onClick()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = AegisOnPrimaryWhite
                ),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                modifier = Modifier
                    .height(48.dp)
                    .then(if (buttonTestTag.isNotBlank()) Modifier.testTag(buttonTestTag) else Modifier)
            ) {
                Icon(
                    imageVector = actionIcon,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = actionLabel,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalBodyFont,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
internal fun ToggleEditsActionCard(
    title: String,
    summary: String,
    icon: ImageVector,
    accentColor: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onOpenEdits: () -> Unit,
    cardTestTag: String,
    switchTestTag: String,
    buttonTestTag: String = if (cardTestTag.isNotBlank()) "${cardTestTag}_button" else "",
    tone: CyberCardTone = CyberCardTone.SKY,
    borderColor: Color = AegisCardBorder,
    extraAction: (@Composable androidx.compose.foundation.layout.RowScope.() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    CyberCard(
        borderColor = if (checked) accentColor.copy(alpha = 0.55f) else borderColor,
        tone = tone,
        onClick = {
            AegisSoundEffects.playTap(context)
            onOpenEdits()
        },
        modifier = modifier
            .fillMaxWidth()
            .then(if (cardTestTag.isNotBlank()) Modifier.testTag(cardTestTag) else Modifier)
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
                    .padding(end = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = if (checked) 0.24f else 0.16f),
                                    accentColor.copy(alpha = if (checked) 0.10f else 0.06f)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            accentColor.copy(alpha = if (checked) 0.42f else 0.28f),
                            RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = AegisTextPrimary,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalDisplayFont,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = summary,
                        color = if (checked) accentColor else AegisTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Medium,
                        fontFamily = ClassicalBodyFont,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Switch(
                checked = checked,
                onCheckedChange = { next ->
                    onCheckedChange(next)
                },
                modifier = Modifier.testTag(switchTestTag),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AegisOnPrimaryWhite,
                    checkedTrackColor = accentColor,
                    uncheckedThumbColor = AegisTextMuted,
                    uncheckedTrackColor = AegisCardBorder
                )
            )
        }

        HorizontalDivider(
            color = AegisCardBorder.copy(alpha = 0.42f),
            modifier = Modifier.padding(vertical = 10.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (extraAction != null) {
                extraAction()
            }

            Button(
                onClick = {
                    AegisSoundEffects.playTap(context)
                    onOpenEdits()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (checked) accentColor else accentColor.copy(alpha = 0.14f),
                    contentColor = if (checked) AegisOnPrimaryWhite else accentColor
                ),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .then(if (buttonTestTag.isNotBlank()) Modifier.testTag(buttonTestTag) else Modifier)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (extraAction != null) "CHANGE" else "CHANGE OPTIONS",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalBodyFont,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
internal fun EditsPillWidget(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    isActive: Boolean = false,
    containerColor: Color = AegisCardBackground,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    EditsActionCard(
        title = title,
        summary = if (isActive) "Active • Configured" else "Tap to change",
        icon = icon,
        accentColor = accentColor,
        testTag = "",
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
internal fun EditsPopupDialog(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    accentColor: Color,
    onDismiss: () -> Unit,
    testTag: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AegisLavenderSoft,
        titleContentColor = AegisTextPrimary,
        modifier = Modifier.testTag(testTag),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = AegisTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalDisplayFont,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            color = AegisTextSecondary,
                            fontSize = 11.5.sp,
                            fontFamily = ClassicalBodyFont,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                content = content
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    AegisSoundEffects.playTap(context)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = AegisOnPrimaryWhite
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("DONE", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
            }
        }
    )
}

@Composable
internal fun CollapsibleEditsCard(
    title: String,
    summary: String,
    icon: ImageVector,
    accentColor: Color,
    tone: CyberCardTone,
    borderColor: Color,
    testTag: String,
    initiallyExpanded: Boolean = false,
    compactPillMode: Boolean = false,
    isPillActive: Boolean = false,
    dialogTitle: String = title,
    content: @Composable ColumnScope.() -> Unit
) {
    val context = LocalContext.current
    if (compactPillMode) {
        var showPopup by remember { mutableStateOf(false) }
        EditsActionCard(
            title = title,
            summary = summary,
            icon = icon,
            accentColor = accentColor,
            tone = tone,
            borderColor = borderColor,
            testTag = testTag,
            buttonTestTag = "${testTag}_button",
            onClick = {
                AegisSoundEffects.playTap(context)
                showPopup = true
            }
        )
        if (showPopup) {
            EditsPopupDialog(
                title = dialogTitle,
                subtitle = summary,
                icon = icon,
                accentColor = accentColor,
                onDismiss = { showPopup = false },
                testTag = "${testTag}_dialog",
                content = content
            )
        }
        return
    }

    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "AccordionChevronRotation"
    )

    CyberCard(
        borderColor = if (expanded) accentColor.copy(alpha = 0.6f) else borderColor,
        tone = tone,
        modifier = Modifier.testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { expanded = !expanded }
                .testTag("${testTag}_toggle"),
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
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = AegisTextPrimary,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalDisplayFont,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = summary,
                        color = if (expanded) accentColor else AegisTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = ClassicalBodyFont,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse $title" else "Expand $title",
                    tint = accentColor,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(chevronRotation)
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(200)) + expandVertically(tween(220)),
            exit = fadeOut(tween(150)) + shrinkVertically(tween(200))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(
                    color = AegisCardBorder.copy(alpha = 0.45f),
                    modifier = Modifier.padding(vertical = 10.dp)
                )
                content()
            }
        }
    }
}

/**
 * Single-toggle Edits control: in compact pill mode renders a clean small pill widget that opens a popup dialog
 * containing one master switch + dynamically revealed options once enabled.
 */
@Composable
internal fun ToggleRevealEditsCard(
    title: String,
    summary: String,
    icon: ImageVector,
    accentColor: Color,
    tone: CyberCardTone,
    borderColor: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    cardTestTag: String,
    switchTestTag: String,
    initiallyExpanded: Boolean = false,
    allowExpandWhenOff: Boolean = false,
    compactPillMode: Boolean = false,
    isPillActive: Boolean = checked,
    dialogTitle: String = title,
    content: @Composable ColumnScope.() -> Unit
) {
    val context = LocalContext.current
    if (compactPillMode) {
        var showPopup by remember { mutableStateOf(false) }
        ToggleEditsActionCard(
            title = title,
            summary = summary,
            icon = icon,
            accentColor = accentColor,
            checked = checked,
            onCheckedChange = { next ->
                val preModeTs = AegisSoundEffects.currentModeOnElapsedMs()
                onCheckedChange(next)
                if (AegisSoundEffects.currentModeOnElapsedMs() == preModeTs) {
                    AegisSoundEffects.playTap(context)
                }
            },
            onOpenEdits = {
                AegisSoundEffects.playTap(context)
                showPopup = true
            },
            cardTestTag = cardTestTag,
            switchTestTag = "${cardTestTag}_card_switch",
            buttonTestTag = "${cardTestTag}_button",
            tone = tone,
            borderColor = borderColor
        )
        if (showPopup) {
            EditsPopupDialog(
                title = dialogTitle,
                subtitle = summary,
                icon = icon,
                accentColor = accentColor,
                onDismiss = { showPopup = false },
                testTag = "${cardTestTag}_dialog"
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AegisCardBackground)
                        .border(1.dp, accentColor.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = if (checked) "$title Enabled" else "Enable $title",
                            color = AegisTextPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = ClassicalDisplayFont
                        )
                        Text(
                            text = summary,
                            color = if (checked) accentColor else AegisTextSecondary,
                            fontSize = 11.sp,
                            fontFamily = ClassicalBodyFont,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Switch(
                        checked = checked,
                        onCheckedChange = { next ->
                            val preModeTs = AegisSoundEffects.currentModeOnElapsedMs()
                            onCheckedChange(next)
                            if (AegisSoundEffects.currentModeOnElapsedMs() == preModeTs) {
                                AegisSoundEffects.playTap(context)
                            }
                        },
                        modifier = Modifier.testTag(switchTestTag),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AegisOnPrimaryWhite,
                            checkedTrackColor = accentColor,
                            uncheckedThumbColor = AegisTextMuted,
                            uncheckedTrackColor = AegisCardBorder
                        )
                    )
                }

                AnimatedVisibility(
                    visible = checked || allowExpandWhenOff,
                    enter = fadeIn(tween(200)) + expandVertically(tween(220)),
                    exit = fadeOut(tween(150)) + shrinkVertically(tween(200))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        HorizontalDivider(
                            color = AegisCardBorder.copy(alpha = 0.45f),
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                        content()
                    }
                }
            }
        }
        return
    }

    var expanded by remember { mutableStateOf(initiallyExpanded) }
    var prevChecked by remember { mutableStateOf(checked) }

    LaunchedEffect(checked) {
        if (checked && !prevChecked) {
            expanded = true
        } else if (!checked && prevChecked) {
            expanded = false
        }
        prevChecked = checked
    }

    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "ToggleRevealChevron"
    )

    CyberCard(
        borderColor = if (checked || expanded) accentColor.copy(alpha = 0.6f) else borderColor,
        tone = tone,
        modifier = Modifier.testTag(cardTestTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    if (checked || allowExpandWhenOff) {
                        expanded = !expanded
                    } else {
                        onCheckedChange(true)
                    }
                }
                .testTag("${cardTestTag}_toggle"),
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
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = AegisTextPrimary,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalDisplayFont,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = summary,
                        color = if (checked) accentColor else AegisTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = ClassicalBodyFont,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (checked || allowExpandWhenOff) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.10f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Collapse $title" else "Expand $title",
                            tint = accentColor,
                            modifier = Modifier
                                .size(17.dp)
                                .rotate(chevronRotation)
                        )
                    }
                }

                val switchContext = LocalContext.current
                Switch(
                    checked = checked,
                    onCheckedChange = { next ->
                        val preModeTs = AegisSoundEffects.currentModeOnElapsedMs()
                        onCheckedChange(next)
                        if (AegisSoundEffects.currentModeOnElapsedMs() == preModeTs) {
                            AegisSoundEffects.playTap(switchContext)
                        }
                    },
                    modifier = Modifier.testTag(switchTestTag),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AegisOnPrimaryWhite,
                        checkedTrackColor = accentColor,
                        uncheckedThumbColor = AegisTextMuted,
                        uncheckedTrackColor = AegisCardBorder
                    )
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(220)) + expandVertically(tween(240)),
            exit = fadeOut(tween(150)) + shrinkVertically(tween(200))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(
                    color = AegisCardBorder.copy(alpha = 0.45f),
                    modifier = Modifier.padding(vertical = 10.dp)
                )
                content()
            }
        }
    }
}

@Composable
private fun PowerModeTile(
    mode: PowerMode,
    icon: ImageVector,
    accentColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerFill by animateColorAsState(
        targetValue = if (selected) accentColor else AegisCardBackground.copy(alpha = 0.85f),
        animationSpec = tween(durationMillis = 260),
        label = "PowerModeFill"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) accentColor else AegisCardBorder,
        animationSpec = tween(durationMillis = 260),
        label = "PowerModeBorder"
    )
    val contentTint = if (selected) AegisOnPrimaryWhite else accentColor
    val titleColor = if (selected) AegisOnPrimaryWhite else AegisTextPrimary
    val subColor = if (selected) AegisOnPrimaryWhite.copy(alpha = 0.88f) else AegisTextMuted

    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(containerFill)
            .border(if (selected) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = mode.displayName,
            tint = contentTint,
            modifier = Modifier.size(18.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mode.displayName,
                color = titleColor,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ClassicalDisplayFont,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${mode.scanIntervalMs}ms",
                color = subColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
internal fun CompactSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
    titleColor: Color = AegisTextPrimary,
    activeColor: Color = AegisPrimaryCyan
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = titleColor,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = ClassicalBodyFont,
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        )
        Switch(
            checked = checked,
            onCheckedChange = { next ->
                val preModeTs = AegisSoundEffects.currentModeOnElapsedMs()
                onCheckedChange(next)
                if (AegisSoundEffects.currentModeOnElapsedMs() == preModeTs) {
                    AegisSoundEffects.playTap(context)
                }
            },
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = AegisOnPrimaryWhite,
                checkedTrackColor = activeColor,
                uncheckedThumbColor = AegisTextMuted,
                uncheckedTrackColor = AegisCardBorder
            )
        )
    }
}

@Composable
private fun NotificationsControlCard(
    preferences: AegisPreferences,
    masterKeyGate: MasterKeyGateState
) {
    val context = LocalContext.current
    val notificationsEnabled by preferences.notificationsEnabled.collectAsState()
    var showPopup by remember { mutableStateOf(false) }

    val handleToggle: (Boolean) -> Unit = { nextEnabled ->
        if (!nextEnabled) {
            masterKeyGate.request("silence notifications") {
                preferences.setNotificationsEnabled(false)
                AegisNotificationHelper.cancelOptionalAlerts(context)
                Toast.makeText(context, "Optional alerts silenced", Toast.LENGTH_SHORT).show()
            }
        } else {
            AegisSoundEffects.playModeOn(context)
            preferences.setNotificationsEnabled(true)
            Toast.makeText(context, "Notifications enabled", Toast.LENGTH_SHORT).show()
        }
    }

    ToggleEditsActionCard(
        title = "Notifications",
        summary = if (notificationsEnabled) {
            "ON • Alerts & resume reminders active"
        } else {
            "OFF • Quiet background service only"
        },
        icon = if (notificationsEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
        accentColor = AegisIndigo,
        tone = CyberCardTone.INDIGO,
        borderColor = AegisIndigoBorder,
        checked = notificationsEnabled,
        onCheckedChange = handleToggle,
        onOpenEdits = {
            AegisSoundEffects.playTap(context)
            showPopup = true
        },
        cardTestTag = "edits_notifications_card",
        switchTestTag = "edits_notifications_switch",
        buttonTestTag = "edits_notifications_button"
    )

    if (showPopup) {
        EditsPopupDialog(
            title = "App Notifications",
            subtitle = if (notificationsEnabled) "Alerts & Shield Reminders ON" else "Quiet Mode Active (Silenced)",
            icon = if (notificationsEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
            accentColor = AegisIndigo,
            onDismiss = { showPopup = false },
            testTag = "edits_notifications_dialog"
        ) {
            Text(
                text = "Turn OFF to dismiss pause/resume alert banners and switch required foreground monitoring into silent minimal-priority mode.",
                color = AegisTextSecondary,
                fontSize = 12.5.sp,
                fontFamily = ClassicalBodyFont
            )
            Spacer(modifier = Modifier.height(10.dp))
            CompactSwitchRow(
                title = "Show Alert Notifications",
                checked = notificationsEnabled,
                activeColor = AegisIndigo,
                onCheckedChange = handleToggle,
                testTag = "edits_notifications_dialog_switch"
            )
        }
    }
}

@Composable
private fun BlockingScreenStyleCard(
    preferences: AegisPreferences,
    masterKeyGate: MasterKeyGateState
) {
    val context = LocalContext.current
    val discreetMode by preferences.discreetBlockingScreenMode.collectAsState()
    var showPopup by remember { mutableStateOf(false) }

    val handleStyleChange: (Boolean) -> Unit = { nextDiscreet ->
        masterKeyGate.request("change blocking screen style") {
            preferences.setDiscreetBlockingScreenMode(nextDiscreet)
            Toast.makeText(
                context,
                if (nextDiscreet) "System Notice style enabled" else "Branded Shield style enabled",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    ToggleEditsActionCard(
        title = "Discreet Lock Screens",
        summary = if (discreetMode) {
            "System Notice • Non-suspicious (hides app name)"
        } else {
            "Branded • Shows Sheildlenz Shield badge"
        },
        icon = if (discreetMode) Icons.Default.VisibilityOff else Icons.Default.Shield,
        accentColor = AegisIndigo,
        tone = CyberCardTone.INDIGO,
        borderColor = AegisIndigoBorder,
        checked = discreetMode,
        onCheckedChange = handleStyleChange,
        onOpenEdits = {
            AegisSoundEffects.playTap(context)
            showPopup = true
        },
        cardTestTag = "edits_discreet_blocking_card",
        switchTestTag = "edits_discreet_blocking_switch",
        buttonTestTag = "edits_discreet_blocking_button"
    )

    if (showPopup) {
        EditsPopupDialog(
            title = "Parent Blocking Screen Style",
            subtitle = if (discreetMode) "Non-Suspicious System Notice" else "Sheildlenz Branded Shield",
            icon = if (discreetMode) Icons.Default.VisibilityOff else Icons.Default.Shield,
            accentColor = AegisIndigo,
            onDismiss = { showPopup = false },
            testTag = "edits_discreet_blocking_dialog"
        ) {
            Text(
                text = "When System Notice is ON, every block, warning, bedtime, app limit, and lockdown screen uses neutral system-style wording with zero app name or parental branding.",
                color = AegisTextSecondary,
                fontSize = 12.5.sp,
                fontFamily = ClassicalBodyFont
            )
            Spacer(modifier = Modifier.height(10.dp))
            CompactSwitchRow(
                title = "System Notice (Non-Suspicious)",
                checked = discreetMode,
                activeColor = AegisIndigo,
                onCheckedChange = handleStyleChange,
                testTag = "edits_discreet_blocking_dialog_switch"
            )
        }
    }
}

@Composable
private fun TouchSoundsCard(
    preferences: AegisPreferences,
    compactPillMode: Boolean = false
) {
    val context = LocalContext.current
    val touchSoundsEnabled by preferences.touchSoundsEnabled.collectAsState()
    var showPopup by remember { mutableStateOf(false) }

    if (compactPillMode) {
        ToggleEditsActionCard(
            title = "Touch Sounds",
            summary = if (touchSoundsEnabled) "On • Bubbly glassy pop" else "Off • Touch feedback muted",
            icon = if (touchSoundsEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
            accentColor = AegisIndigo,
            tone = CyberCardTone.INDIGO,
            borderColor = AegisIndigoBorder,
            checked = touchSoundsEnabled,
            onCheckedChange = { enabled ->
                preferences.setTouchSoundsEnabled(enabled)
                if (enabled) {
                    AegisSoundEffects.playTap(context)
                }
            },
            onOpenEdits = {
                AegisSoundEffects.playTap(context)
                showPopup = true
            },
            cardTestTag = "touch_sounds_card",
            switchTestTag = "touch_sounds_card_switch",
            buttonTestTag = "touch_sounds_card_button",
            extraAction = {
                OutlinedButton(
                    onClick = {
                        AegisSoundEffects.playPreviewTap(context)
                        Toast.makeText(context, "Playing bubbly pop...", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("test_sound_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = AegisIndigo,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TEST SOUND",
                        color = AegisIndigo,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalBodyFont,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        )

        if (showPopup) {
            EditsPopupDialog(
                title = "Touch Feedback Sounds",
                subtitle = if (touchSoundsEnabled) "Bubbly Glassy Pop Enabled" else "Touch Feedback Muted",
                icon = if (touchSoundsEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                accentColor = AegisIndigo,
                onDismiss = { showPopup = false },
                testTag = "touch_sounds_card_dialog"
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AegisCardBackground)
                        .border(1.dp, AegisPrimaryCyan.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .clickable {
                            val enabled = !touchSoundsEnabled
                            preferences.setTouchSoundsEnabled(enabled)
                            if (enabled) {
                                AegisSoundEffects.playTap(context)
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "Bubbly Glassy Tap",
                            color = AegisTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = ClassicalDisplayFont
                        )
                        Text(
                            text = if (touchSoundsEnabled) "On • Bubbly glassy pop" else "Muted",
                            color = if (touchSoundsEnabled) AegisPrimaryCyan else AegisTextSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = ClassicalBodyFont
                        )
                    }
                    Switch(
                        checked = touchSoundsEnabled,
                        onCheckedChange = { enabled ->
                            preferences.setTouchSoundsEnabled(enabled)
                            if (enabled) {
                                AegisSoundEffects.playTap(context)
                            }
                        },
                        modifier = Modifier.testTag("touch_sounds_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AegisOnPrimaryWhite,
                            checkedTrackColor = AegisPrimaryCyan,
                            uncheckedThumbColor = AegisTextMuted,
                            uncheckedTrackColor = AegisCardBorder
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        AegisSoundEffects.playPreviewTap(context)
                        Toast.makeText(context, "Playing bubbly pop...", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AegisPrimaryCyan,
                        contentColor = AegisOnPrimaryWhite
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("dialog_test_sound_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TEST SOUND EFFECT",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalBodyFont
                    )
                }
            }
        }
        return
    }

    CyberCard(
        borderColor = AegisLavenderBorder,
        tone = CyberCardTone.SKY,
        modifier = Modifier.testTag("touch_sounds_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    val enabled = !touchSoundsEnabled
                    preferences.setTouchSoundsEnabled(enabled)
                    if (enabled) {
                        AegisSoundEffects.playTap(context)
                    }
                },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(AegisPrimaryCyan.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (touchSoundsEnabled) {
                            Icons.AutoMirrored.Filled.VolumeUp
                        } else {
                            Icons.AutoMirrored.Filled.VolumeOff
                        },
                        contentDescription = null,
                        tint = AegisPrimaryCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Touch Sounds",
                        color = AegisTextPrimary,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalDisplayFont,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (touchSoundsEnabled) "On • Bubbly glassy pop" else "Muted",
                        color = if (touchSoundsEnabled) AegisPrimaryCyan else AegisTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = ClassicalBodyFont,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Switch(
                checked = touchSoundsEnabled,
                onCheckedChange = { enabled ->
                    preferences.setTouchSoundsEnabled(enabled)
                    if (enabled) {
                        AegisSoundEffects.playTap(context)
                    }
                },
                modifier = Modifier.testTag("touch_sounds_switch"),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AegisOnPrimaryWhite,
                    checkedTrackColor = AegisPrimaryCyan,
                    uncheckedThumbColor = AegisTextMuted,
                    uncheckedTrackColor = AegisCardBorder
                )
            )
        }
    }
}

@Composable
fun WhitelistedAppsSection(
    preferences: AegisPreferences,
    compactPillMode: Boolean = false
) {
    val whitelisted by preferences.whitelistedPackages.collectAsState()
    var showAppsPicker by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var installedApps by remember { mutableStateOf<List<InstalledAppEntry>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Resolve installed launchable apps asynchronously when picker opens
    LaunchedEffect(showAppsPicker) {
        if (showAppsPicker && installedApps.isEmpty()) {
            isLoadingApps = true
            installedApps = withContext(Dispatchers.IO) {
                val pm = context.packageManager
                val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                val activities = pm.queryIntentActivities(intent, 0)
                activities
                    .mapNotNull { resolveInfo ->
                        val pkg = resolveInfo.activityInfo?.packageName ?: return@mapNotNull null
                        if (pkg == context.packageName) return@mapNotNull null
                        val label = resolveInfo.loadLabel(pm)?.toString()?.trim().orEmpty().ifBlank { pkg }
                        val icon = try {
                            drawableToScaledBitmap(resolveInfo.loadIcon(pm))
                        } catch (_: Exception) {
                            null
                        }
                        InstalledAppEntry(packageName = pkg, label = label, icon = icon)
                    }
                    .distinctBy { it.packageName }
                    .sortedBy { it.label.lowercase() }
            }
            isLoadingApps = false
        }
    }

    val openAppPicker: () -> Unit = {
        AegisSoundEffects.playTap(context)
        searchQuery = ""
        showAppsPicker = true
    }

    if (compactPillMode) {
        EditsActionCard(
            title = "Excluded Apps",
            summary = if (whitelisted.isEmpty()) "0 apps excluded from protection" else "${whitelisted.size} apps excluded",
            icon = Icons.Default.Apps,
            accentColor = AegisEmeraldTeal,
            tone = CyberCardTone.EMERALD,
            borderColor = AegisEmeraldBorder,
            testTag = "whitelisted_apps_card",
            buttonTestTag = "whitelisted_apps_card_button",
            onClick = openAppPicker
        )
    } else if (whitelisted.isEmpty()) {
        CyberCard(
            borderColor = AegisLavenderBorder,
            tone = CyberCardTone.SKY,
            modifier = Modifier.testTag("whitelisted_apps_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = openAppPicker)
                    .testTag("whitelisted_apps_card_toggle"),
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
                            .background(AegisPrimaryCyan.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = null,
                            tint = AegisPrimaryCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Excluded Apps",
                            color = AegisTextPrimary,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = ClassicalDisplayFont,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "None excluded • Tap to choose",
                            color = AegisTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = ClassicalBodyFont,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Button(
                    onClick = openAppPicker,
                    colors = ButtonDefaults.buttonColors(containerColor = AegisPrimaryCyan, contentColor = AegisOnPrimaryWhite),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("add_whitelist_button")
                ) {
                    Text("SELECT", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
            }
        }
    } else {
        CollapsibleEditsCard(
            title = "Excluded Apps",
            summary = "${whitelisted.size} excluded ${if (whitelisted.size == 1) "app" else "apps"}",
            icon = Icons.Default.Apps,
            accentColor = AegisPrimaryCyan,
            tone = CyberCardTone.SKY,
            borderColor = AegisLavenderBorder,
            testTag = "whitelisted_apps_card"
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tap to choose installed apps",
                    color = AegisTextSecondary,
                    fontSize = 12.sp,
                    fontFamily = ClassicalBodyFont,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                Button(
                    onClick = openAppPicker,
                    colors = ButtonDefaults.buttonColors(containerColor = AegisPrimaryCyan, contentColor = AegisOnPrimaryWhite),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("add_whitelist_button")
                ) {
                    Text("SELECT", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
            }

        if (whitelisted.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = AegisCardBorder.copy(alpha = 0.45f))
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                whitelisted.forEach { pkg ->
                    val resolvedLabel = remember(pkg, installedApps) {
                        installedApps.firstOrNull { it.packageName == pkg }?.label ?: try {
                            val ai = context.packageManager.getApplicationInfo(pkg, 0)
                            context.packageManager.getApplicationLabel(ai).toString()
                        } catch (_: Exception) {
                            pkg
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(50))
                            .background(AegisSurfaceElevated)
                            .border(1.dp, AegisLavenderBorder, RoundedCornerShape(50))
                            .padding(start = 12.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                            Text(
                                text = resolvedLabel,
                                color = AegisTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = ClassicalBodyFont,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (resolvedLabel != pkg) {
                                Text(
                                    text = pkg,
                                    color = AegisTextMuted,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                preferences.removeWhitelistedPackage(pkg)
                                Toast.makeText(context, "Removed $resolvedLabel", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove $resolvedLabel",
                                tint = AegisWarningAmber,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
        }
    }

    if (showAppsPicker) {
        val filteredApps = remember(installedApps, searchQuery, whitelisted) {
            val q = searchQuery.trim().lowercase()
            val list = if (q.isEmpty()) installedApps else installedApps.filter {
                it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
            }
            list.sortedByDescending { whitelisted.contains(it.packageName) }
        }

        AlertDialog(
            onDismissRequest = { showAppsPicker = false },
            containerColor = AegisLavenderSoft,
            modifier = Modifier.testTag("installed_apps_picker_dialog"),
            title = {
                Text(
                    text = "Tap Apps to Exclude",
                    fontFamily = ClassicalDisplayFont,
                    fontWeight = FontWeight.Bold,
                    color = AegisTextPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search installed apps...", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = AegisPurple,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AegisPrimaryCyan,
                            unfocusedBorderColor = AegisLavenderBorder,
                            focusedContainerColor = AegisCardBackground,
                            unfocusedContainerColor = AegisCardBackground,
                            focusedTextColor = AegisTextPrimary,
                            unfocusedTextColor = AegisTextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("installed_apps_search_input")
                    )

                    if (isLoadingApps) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = AegisPurple)
                        }
                    } else if (filteredApps.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No matching installed apps found",
                                color = AegisTextMuted,
                                fontSize = 12.sp,
                                fontFamily = ClassicalBodyFont
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredApps, key = { it.packageName }) { app ->
                                val isExcluded = whitelisted.contains(app.packageName)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isExcluded) AegisPrimaryCyan.copy(alpha = 0.14f) else AegisCardBackground)
                                        .border(
                                            width = if (isExcluded) 1.4.dp else 1.dp,
                                            color = if (isExcluded) AegisPrimaryCyan else AegisLavenderBorder.copy(alpha = 0.65f),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable {
                                            if (isExcluded) {
                                                preferences.removeWhitelistedPackage(app.packageName)
                                            } else {
                                                preferences.addWhitelistedPackage(app.packageName)
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                                    ) {
                                        if (app.icon != null) {
                                            Image(
                                                bitmap = app.icon,
                                                contentDescription = app.label,
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(RoundedCornerShape(9.dp))
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(AegisPurple.copy(alpha = 0.16f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = app.label.take(1).uppercase(),
                                                    color = AegisPurple,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = app.label,
                                                color = AegisTextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = ClassicalBodyFont,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = app.packageName,
                                                color = AegisTextMuted,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = if (isExcluded) "Excluded" else "Not excluded",
                                        tint = if (isExcluded) AegisPrimaryCyan else AegisCardBorder,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAppsPicker = false },
                    colors = ButtonDefaults.buttonColors(containerColor = AegisPrimaryCyan, contentColor = AegisOnPrimaryWhite)
                ) {
                    Text("DONE", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun ScheduledProtectionSection(
    preferences: AegisPreferences,
    masterKeyGate: MasterKeyGateState,
    compactPillMode: Boolean = false
) {
    val context = LocalContext.current
    val scheduleEnabled by preferences.scheduleEnabled.collectAsState()
    val startMinutes by preferences.scheduleStartMinutes.collectAsState()
    val endMinutes by preferences.scheduleEndMinutes.collectAsState()
    val isActiveNow = scheduleEnabled && preferences.isWithinScheduledWindow()

    val windowLabel = "${AegisPreferences.formatClockTime(startMinutes)} → ${AegisPreferences.formatClockTime(endMinutes)}"
    ToggleRevealEditsCard(
        title = "Quiet Hours",
        summary = if (scheduleEnabled) "Active • $windowLabel" else "Off • Scheduled window inactive",
        icon = Icons.Default.Schedule,
        accentColor = AegisEmeraldTeal,
        tone = CyberCardTone.EMERALD,
        borderColor = AegisEmeraldBorder,
        checked = scheduleEnabled,
        onCheckedChange = { enabled ->
            if (!enabled) {
                AegisSoundEffects.playTap(context)
                masterKeyGate.request("disable Scheduled Guard") {
                    preferences.setScheduleEnabled(false)
                }
            } else {
                AegisSoundEffects.playModeOn(context)
                preferences.setScheduleEnabled(true)
            }
        },
        cardTestTag = "scheduled_protection_card",
        switchTestTag = "schedule_enabled_switch",
        initiallyExpanded = scheduleEnabled,
        compactPillMode = compactPillMode
    ) {
        val presets = listOf(
            Triple("9P–7A", 21 * 60, 7 * 60),
            Triple("10P–6A", 22 * 60, 6 * 60),
            Triple("2P–6P", 14 * 60, 18 * 60)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            presets.forEach { (label, pStart, pEnd) ->
                val isSelected = startMinutes == pStart && endMinutes == pEnd
                OutlinedButton(
                    onClick = {
                        preferences.setScheduleWindow(pStart, pEnd)
                    },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected) AegisPurple else AegisCardBackground,
                        contentColor = if (isSelected) AegisOnPrimaryWhite else AegisTextPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalBodyFont,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ScheduleHourStepper(
                label = "Start",
                minuteOfDay = startMinutes,
                onMinusHour = {
                    val next = (startMinutes - 60 + 1440) % 1440
                    preferences.setScheduleWindow(next, endMinutes)
                },
                onPlusHour = {
                    val next = (startMinutes + 60) % 1440
                    preferences.setScheduleWindow(next, endMinutes)
                },
                modifier = Modifier.weight(1f)
            )
            ScheduleHourStepper(
                label = "End",
                minuteOfDay = endMinutes,
                onMinusHour = {
                    val next = (endMinutes - 60 + 1440) % 1440
                    preferences.setScheduleWindow(startMinutes, next)
                },
                onPlusHour = {
                    val next = (endMinutes + 60) % 1440
                    preferences.setScheduleWindow(startMinutes, next)
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
internal fun ScheduleHourStepper(
    label: String,
    minuteOfDay: Int,
    onMinusHour: () -> Unit,
    onPlusHour: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(AegisSurfaceElevated)
            .border(1.dp, AegisLavenderBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$label: ${AegisPreferences.formatClockTime(minuteOfDay)}",
            color = AegisTextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = ClassicalBodyFont,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedButton(
                onClick = onMinusHour,
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("-1h", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            }
            OutlinedButton(
                onClick = onPlusHour,
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("+1h", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            }
        }
    }
}
