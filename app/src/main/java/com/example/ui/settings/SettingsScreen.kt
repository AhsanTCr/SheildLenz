package com.example.ui.settings

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.PowerManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.core.alerts.DiscreetAlertFormatter
import com.example.core.audio.AegisSoundEffects
import com.example.core.models.SensitivityLevel
import com.example.core.notifications.AegisNotificationHelper
import com.example.data.preferences.AegisPreferences
import com.example.data.preferences.PowerMode
import com.example.data.preferences.ProtectionProfile
import com.example.data.preferences.QuickTileMode
import com.example.data.preferences.RecoveryQrMode
import com.example.data.repository.ProtectionRepository
import com.example.detection.classifier.ModelManager
import com.example.security.deviceowner.AegisDeviceAdminReceiver
import com.example.services.protection.ProfileAutoScheduler
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
import com.example.ui.theme.AegisElectricBlue
import com.example.ui.theme.AegisEmeraldBorder
import com.example.ui.theme.AegisEmeraldSoft
import com.example.ui.theme.AegisEmeraldTeal
import com.example.ui.theme.AegisGoldBorder
import com.example.ui.theme.AegisGoldBright
import com.example.ui.theme.AegisIndigo
import com.example.ui.theme.AegisIndigoBorder
import com.example.ui.theme.AegisLavenderBorder
import com.example.ui.theme.AegisLavenderLight
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
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

@Composable
fun SettingsScreen(
    preferences: AegisPreferences,
    modelManager: ModelManager,
    protectionRepo: ProtectionRepository,
    onModelUpdated: () -> Unit = {},
    onNavigateToStatus: () -> Unit = {},
    onNavigateToParents: () -> Unit = {},
    onNavigateToUsageScrollGuard: () -> Unit = {}
) {
    val context = LocalContext.current
    val hasPin by preferences.hasGuardianPin.collectAsState()
    val isGuardianUnlocked by preferences.isGuardianSessionUnlocked.collectAsState()
    val parentalRole by preferences.parentalRole.collectAsState()
    val pairedParentName by preferences.pairedParentName.collectAsState()
    val guardianEmail by preferences.guardianEmail.collectAsState()
    val challengeCode by preferences.recoveryChallengeCode.collectAsState()
    val isEditsLocked = shouldLockEditsScreen(hasPin, isGuardianUnlocked)
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, preferences) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                val isScreenOff = powerManager?.isInteractive == false
                preferences.onAppBackgrounded(forceLock = isScreenOff)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp)
                )
            } else {
                UnlockedEditsContent(
                    preferences = preferences,
                    modelManager = modelManager,
                    protectionRepo = protectionRepo,
                    hasPin = hasPin,
                    parentalRole = parentalRole,
                    onModelUpdated = onModelUpdated,
                    onNavigateToUsageScrollGuard = onNavigateToUsageScrollGuard,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UnlockedEditsContent(
    preferences: AegisPreferences,
    modelManager: ModelManager,
    protectionRepo: ProtectionRepository,
    hasPin: Boolean,
    parentalRole: String?,
    onModelUpdated: () -> Unit,
    onNavigateToUsageScrollGuard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val telemetry by protectionRepo.telemetry.collectAsState()
    val activeAppName by protectionRepo.activeAppLabel.collectAsState()
    val sensitivity by preferences.sensitivity.collectAsState()
    val isStrictEverything by preferences.strictEverything.collectAsState()
    val isAutoStart by preferences.autoStart.collectAsState()
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
    val activeProtectionProfile by preferences.activeProtectionProfile.collectAsState()
    val scrollLockEnabled by preferences.scrollLockEnabled.collectAsState()
    val scrollLimitCount by preferences.scrollLimitCount.collectAsState()
    val dailyReelsTotalCount by preferences.dailyReelsTotalCount.collectAsState()

    val stage1DurationLabel = AegisPreferences.formatMinutesLabel(lockdownDurationMinutes)

    var showModelDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var showDeviceOwnerWizard by remember { mutableStateOf(false) }

    var isDeviceAdmin by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isDeviceAdmin = AegisDeviceAdminReceiver.isDeviceAdmin(context)
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isDeviceAdmin = AegisDeviceAdminReceiver.isDeviceAdmin(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val masterKeyGate = rememberMasterKeyGate(preferences)

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(top = 10.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (hasPin) {
            item(key = "edits_unlocked_banner") {
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
        item(key = "edits_quick_profiles") {
            QuickProtectionProfilesCard(
                activeProfile = activeProtectionProfile,
                onApplyProfile = { profile ->
                    AegisSoundEffects.playModeOn(context)
                    preferences.applyProtectionProfile(profile)
                    Toast.makeText(
                        context,
                        "${profile.title} profile applied",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
        item(key = "edits_profile_auto_switch") {
            ProfileAutoSwitcherCard(preferences = preferences)
        }
        item(key = "edits_quick_tile_mode") {
            QuickTileModeCard(preferences = preferences)
        }
        // 1. AI Classification
        item(key = "edits_header_ai") {
            LuxurySectionHeader(title = "AI Classification", accentColor = AegisPurple)
        }
        item(key = "edits_ai_model") {
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
                item {
                    AppLockerSection(
                        preferences = preferences
                    )
                }
                item(key = "edits_usage_scroll_guard") {
                    EditsActionCard(
                        title = "Usage & Scroll Guard",
                        summary = "$dailyReelsTotalCount/$scrollLimitCount Reels • ${if (scrollLockEnabled) "Auto-Stop ON" else "Counter ON"}",
                        icon = Icons.Default.HourglassBottom,
                        accentColor = AegisEmeraldTeal,
                        tone = CyberCardTone.EMERALD,
                        borderColor = AegisEmeraldBorder,
                        actionLabel = "OPEN",
                        testTag = "edits_usage_scroll_guard_card",
                        buttonTestTag = "edits_usage_scroll_guard_button",
                        onClick = onNavigateToUsageScrollGuard
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

                // 5. System & Developer Options (7-tap)
                item {
                    LuxurySectionHeader(title = "System", accentColor = AegisIndigo)
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
                    PolicyBackupRestoreSection(
                        preferences = preferences,
                        masterKeyGate = masterKeyGate,
                        compactPillMode = true
                    )
                }
                item {
                    AppVersionSecretCard(preferences = preferences)
                }
                item {
                    AdvancedPrecisionLabSection(preferences = preferences)
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

@Composable
private fun EditsPasswordLockGate(
    preferences: AegisPreferences,
    parentalRole: String?,
    pairedParentName: String?,
    guardianEmail: String,
    challengeCode: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var passwordInput by remember { mutableStateOf("") }
    var showPinCharacters by remember { mutableStateOf(false) }
    var recoveryCodeInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var showForgotQr by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val shakeOffsetX = remember { Animatable(0f) }
    val triggerErrorShake: () -> Unit = {
        scope.launch {
            shakeOffsetX.snapTo(0f)
            val offsets = listOf(-14f, 14f, -10f, 10f, -5f, 5f, 0f)
            offsets.forEach { target ->
                shakeOffsetX.animateTo(target, tween(durationMillis = 36))
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "EditsLockCrestMotion")
    val crestPulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "EditsLockCrestPulse"
    )

    val submitUnlock: () -> Unit = {
        if (showForgotQr) {
            val cleanPin = newPinInput.trim()
            if (cleanPin.length !in 4..8 || !cleanPin.all { it.isDigit() }) {
                errorMessage = "Enter a new 4–8 digit Guardian PIN"
                triggerErrorShake()
            } else if (!preferences.resetPinWithRecoveryToken(recoveryCodeInput, cleanPin)) {
                errorMessage = "Invalid code for Challenge ID $challengeCode"
                triggerErrorShake()
            } else {
                AegisSoundEffects.playModeOn(context)
                preferences.unlockGuardianSession()
                errorMessage = null
            }
        } else {
            if (passwordInput.isEmpty() || !preferences.verifyGuardianPin(passwordInput)) {
                errorMessage = "Incorrect Guardian PIN"
                passwordInput = ""
                triggerErrorShake()
            } else {
                AegisSoundEffects.playModeOn(context)
                preferences.unlockGuardianSession()
                errorMessage = null
            }
        }
    }

    val accentPrimary = if (showForgotQr) AegisChampagneGold else AegisPurple
    val accentSecondary = if (showForgotQr) AegisWarningAmber else AegisElectricBlue
    val cardShape = RoundedCornerShape(28.dp)

    Box(
        modifier = modifier.verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 436.dp)
                .padding(vertical = 8.dp)
                .offset { IntOffset(shakeOffsetX.value.roundToInt(), 0) }
                .shadow(
                    elevation = 16.dp,
                    shape = cardShape,
                    ambientColor = accentPrimary.copy(alpha = 0.24f),
                    spotColor = accentSecondary.copy(alpha = 0.22f)
                )
                .testTag("edits_lock_gate_card"),
            shape = cardShape,
            color = Color.Transparent,
            border = BorderStroke(
                width = 1.4.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.96f),
                        if (showForgotQr) AegisGoldBorder else AegisLavenderBorder,
                        if (showForgotQr) AegisChampagneGold.copy(alpha = 0.65f) else AegisCardBorder
                    )
                )
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFCFCFF),
                                if (showForgotQr) AegisChampagneSoft else Color(0xFFF3EEFF),
                                Color(0xFFEAF4FF)
                            )
                        )
                    )
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    accentSecondary.copy(alpha = 0.14f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.18f, size.height * 0.15f),
                                radius = size.width * 0.56f
                            ),
                            radius = size.width * 0.56f,
                            center = Offset(size.width * 0.18f, size.height * 0.15f)
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    AegisGoldBright.copy(alpha = 0.15f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.85f, size.height * 0.85f),
                                radius = size.width * 0.48f
                            ),
                            radius = size.width * 0.48f,
                            center = Offset(size.width * 0.85f, size.height * 0.85f)
                        )
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Iridescent top crown stripe
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(AegisElectricBlue, AegisPurple, AegisChampagneGold)
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Breathing halo + VIP gradient crest badge
                    Box(
                        modifier = Modifier.size(68.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size((60f + 6f * crestPulse).dp)
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            accentPrimary.copy(alpha = 0.22f + 0.10f * crestPulse),
                                            accentSecondary.copy(alpha = 0.08f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(16.dp),
                                    ambientColor = accentPrimary.copy(alpha = 0.32f),
                                    spotColor = accentSecondary.copy(alpha = 0.30f)
                                )
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = if (showForgotQr) {
                                            listOf(AegisChampagneGold, AegisGoldBright, AegisWarningAmber)
                                        } else {
                                            listOf(AegisElectricBlue, AegisPurple, AegisLavenderLight)
                                        }
                                    )
                                )
                                .border(1.3.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (showForgotQr) Icons.Default.Shield else Icons.Default.Lock,
                                contentDescription = "Edits Locked",
                                tint = AegisOnPrimaryWhite,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (parentalRole == "child") {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = AegisChampagneSoft,
                            border = BorderStroke(1.dp, AegisGoldBorder),
                            modifier = Modifier.testTag("edits_child_parent_notice")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SupervisorAccount,
                                    contentDescription = "Parent Linked",
                                    tint = AegisChampagneGold,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Parent-linked • ${pairedParentName?.takeIf { it.isNotBlank() } ?: "Guardian"}",
                                    color = AegisTextPrimary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = ClassicalBodyFont
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    } else {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = accentPrimary.copy(alpha = 0.11f),
                            border = BorderStroke(1.dp, accentPrimary.copy(alpha = 0.28f))
                        ) {
                            Text(
                                text = if (showForgotQr) "RECOVERY VERIFICATION" else "GUARDIAN VAULT",
                                color = accentPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.2.sp,
                                fontFamily = ClassicalBodyFont,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Text(
                        text = if (showForgotQr) "Parent QR Reset" else "Edits Protected",
                        color = AegisTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalDisplayFont,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = if (showForgotQr) {
                            "Enter recovery code from Parent phone to set a new PIN"
                        } else {
                            "Enter your Guardian PIN to customize protection settings"
                        },
                        color = AegisTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        fontFamily = ClassicalBodyFont,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (showForgotQr) {
                        val qrPayload = remember(challengeCode, guardianEmail) {
                            preferences.buildRecoveryQrUri(RecoveryQrMode.PIN_RESET)
                        }
                        Text(
                            text = "Scan on parent's phone (${guardianEmail.ifBlank { "Guardian Google account" }}) to generate reset code.",
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
                                .size(156.dp)
                                .border(1.4.dp, AegisGoldBorder, RoundedCornerShape(18.dp)),
                            testTag = "edits_forgot_qr_matrix"
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.78f),
                            border = BorderStroke(1.dp, AegisGoldBorder)
                        ) {
                            Text(
                                text = "Challenge ID: $challengeCode",
                                color = AegisTextPrimary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = recoveryCodeInput,
                            onValueChange = {
                                recoveryCodeInput = it.uppercase()
                                errorMessage = null
                            },
                            label = { Text("8-Char One-Time Code", fontFamily = ClassicalBodyFont) },
                            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = AegisChampagneGold) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AegisChampagneGold,
                                unfocusedBorderColor = AegisGoldBorder,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White.copy(alpha = 0.88f)
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
                            label = { Text("New Guardian PIN (4–8 digits)", fontFamily = ClassicalBodyFont) },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = AegisPurple) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { submitUnlock() }),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AegisPurple,
                                unfocusedBorderColor = AegisLavenderBorder,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White.copy(alpha = 0.88f)
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
                            label = { Text("Guardian PIN", fontFamily = ClassicalBodyFont) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = AegisPurple
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { showPinCharacters = !showPinCharacters }
                                ) {
                                    Icon(
                                        imageVector = if (showPinCharacters) {
                                            Icons.Default.VisibilityOff
                                        } else {
                                            Icons.Default.Visibility
                                        },
                                        contentDescription = if (showPinCharacters) "Hide PIN" else "Show PIN",
                                        tint = AegisTextSecondary
                                    )
                                }
                            },
                            visualTransformation = if (showPinCharacters) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { submitUnlock() }),
                            singleLine = true,
                            isError = errorMessage != null,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AegisPurple,
                                unfocusedBorderColor = AegisLavenderBorder,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White.copy(alpha = 0.90f),
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
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = AegisDangerRed.copy(alpha = 0.10f),
                            border = BorderStroke(1.dp, AegisDangerRed.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = AegisDangerRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errorMessage!!,
                                    color = AegisDangerRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = ClassicalBodyFont
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = submitUnlock,
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = AegisOnPrimaryWhite
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 1.dp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("edits_unlock_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = if (showForgotQr) {
                                            listOf(AegisChampagneGold, AegisWarningAmber)
                                        } else {
                                            listOf(AegisElectricBlue, AegisPurple)
                                        }
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LockOpen,
                                    contentDescription = null,
                                    tint = AegisOnPrimaryWhite,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (showForgotQr) "RESET & UNLOCK" else "UNLOCK EDITS",
                                    color = AegisOnPrimaryWhite,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp,
                                    fontFamily = ClassicalBodyFont
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

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
                            text = if (showForgotQr) "Back to PIN Entry" else "Forgot Password?",
                            color = accentPrimary,
                            fontSize = 12.5.sp,
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
        animationSpec = tween(durationMillis = 120),
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
            enter = fadeIn(tween(120)) + expandVertically(tween(120)),
            exit = fadeOut(tween(100)) + shrinkVertically(tween(120))
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
                    enter = fadeIn(tween(120)) + expandVertically(tween(120)),
                    exit = fadeOut(tween(100)) + shrinkVertically(tween(120))
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
internal fun TouchSoundsCard(
    preferences: AegisPreferences,
    compactPillMode: Boolean = false
) {
    val context = LocalContext.current
    val touchSoundsEnabled by preferences.touchSoundsEnabled.collectAsState()
    val autoSoundSelectorEnabled by preferences.autoSoundSelectorEnabled.collectAsState()
    var showPopup by remember { mutableStateOf(false) }

    val activeSummary = when {
        !touchSoundsEnabled -> "Off • Touch feedback muted"
        autoSoundSelectorEnabled -> "On • Auto Sound Selector"
        else -> "On • Ceramic Tile Tick"
    }

    if (compactPillMode) {
        ToggleEditsActionCard(
            title = "Touch Sounds",
            summary = activeSummary,
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
                        Toast.makeText(
                            context,
                            if (autoSoundSelectorEnabled) "Playing auto tile variation..." else "Playing ceramic tile tick...",
                            Toast.LENGTH_SHORT
                        ).show()
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
                subtitle = when {
                    !touchSoundsEnabled -> "Touch Feedback Muted"
                    autoSoundSelectorEnabled -> "Auto Sound Selector Enabled (8 Tones)"
                    else -> "Ceramic Tile Tick Enabled"
                },
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
                            text = "Ceramic Tile Tick",
                            color = AegisTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = ClassicalDisplayFont
                        )
                        Text(
                            text = if (touchSoundsEnabled) "On • Crisp cool tile tick" else "Muted",
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

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AegisCardBackground)
                        .border(
                            1.dp,
                            if (autoSoundSelectorEnabled) AegisPurple.copy(alpha = 0.45f) else AegisLavenderBorder,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            val next = !autoSoundSelectorEnabled
                            preferences.setAutoSoundSelectorEnabled(next)
                            if (next && !touchSoundsEnabled) {
                                preferences.setTouchSoundsEnabled(true)
                            }
                            if (next) {
                                AegisSoundEffects.playPreviewTap(context)
                            } else {
                                AegisSoundEffects.playTap(context)
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("auto_sound_selector_row"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "Auto Sound Selector",
                            color = AegisTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = ClassicalDisplayFont
                        )
                        Text(
                            text = if (autoSoundSelectorEnabled) {
                                "On • Different friendly short tick every touch"
                            } else {
                                "Off • Fixed ceramic tile tick"
                            },
                            color = if (autoSoundSelectorEnabled) AegisPurple else AegisTextSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = ClassicalBodyFont
                        )
                    }
                    Switch(
                        checked = autoSoundSelectorEnabled,
                        onCheckedChange = { next ->
                            preferences.setAutoSoundSelectorEnabled(next)
                            if (next && !touchSoundsEnabled) {
                                preferences.setTouchSoundsEnabled(true)
                            }
                            if (next) {
                                AegisSoundEffects.playPreviewTap(context)
                            } else {
                                AegisSoundEffects.playTap(context)
                            }
                        },
                        modifier = Modifier.testTag("auto_sound_selector_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AegisOnPrimaryWhite,
                            checkedTrackColor = AegisPurple,
                            uncheckedThumbColor = AegisTextMuted,
                            uncheckedTrackColor = AegisCardBorder
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        AegisSoundEffects.playPreviewTap(context)
                        Toast.makeText(
                            context,
                            if (autoSoundSelectorEnabled) "Playing next friendly tile sound..." else "Playing ceramic tile tick...",
                            Toast.LENGTH_SHORT
                        ).show()
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
                        text = activeSummary,
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

        HorizontalDivider(
            color = AegisCardBorder.copy(alpha = 0.45f),
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    val next = !autoSoundSelectorEnabled
                    preferences.setAutoSoundSelectorEnabled(next)
                    if (next && !touchSoundsEnabled) {
                        preferences.setTouchSoundsEnabled(true)
                    }
                    if (next) {
                        AegisSoundEffects.playPreviewTap(context)
                    } else {
                        AegisSoundEffects.playTap(context)
                    }
                }
                .testTag("auto_sound_selector_row"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = "Auto Sound Selector",
                    color = AegisTextPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalDisplayFont
                )
                Text(
                    text = if (autoSoundSelectorEnabled) {
                        "Every touch uses a different cool short tick"
                    } else {
                        "Standard ceramic tile tick"
                    },
                    color = if (autoSoundSelectorEnabled) AegisPurple else AegisTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = ClassicalBodyFont
                )
            }
            Switch(
                checked = autoSoundSelectorEnabled,
                onCheckedChange = { next ->
                    preferences.setAutoSoundSelectorEnabled(next)
                    if (next && !touchSoundsEnabled) {
                        preferences.setTouchSoundsEnabled(true)
                    }
                    if (next) {
                        AegisSoundEffects.playPreviewTap(context)
                    } else {
                        AegisSoundEffects.playTap(context)
                    }
                },
                modifier = Modifier.testTag("auto_sound_selector_switch"),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AegisOnPrimaryWhite,
                    checkedTrackColor = AegisPurple,
                    uncheckedThumbColor = AegisTextMuted,
                    uncheckedTrackColor = AegisCardBorder
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = {
                AegisSoundEffects.playPreviewTap(context)
                Toast.makeText(
                    context,
                    if (autoSoundSelectorEnabled) "Playing next friendly tile sound..." else "Playing ceramic tile tick...",
                    Toast.LENGTH_SHORT
                ).show()
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("test_sound_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = null,
                tint = AegisIndigo,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "TEST SOUND EFFECT",
                color = AegisIndigo,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ClassicalBodyFont
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

@Composable
private fun QuickProtectionProfilesCard(
    activeProfile: ProtectionProfile?,
    onApplyProfile: (ProtectionProfile) -> Unit
) {
    CyberCard(
        borderColor = AegisLavenderBorder,
        tone = CyberCardTone.LAVENDER,
        modifier = Modifier.testTag("edits_quick_profiles_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = "Quick Protection Profiles",
                    color = AegisTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalDisplayFont
                )
                Text(
                    text = "Apply tested rule bundles with one tap",
                    color = AegisTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = ClassicalBodyFont
                )
            }
            StatusPill(
                label = activeProfile?.badge ?: "CUSTOM",
                isActive = activeProfile != null,
                activeColor = AegisPurple,
                inactiveColor = AegisPrimaryCyan
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ProtectionProfile.entries.forEach { profile ->
                val isSelected = activeProfile == profile
                val accent = when (profile) {
                    ProtectionProfile.STRICT_KIDS -> AegisPurple
                    ProtectionProfile.HOMEWORK_FOCUS -> AegisPrimaryCyan
                    ProtectionProfile.BALANCED_GUARD -> AegisEmeraldTeal
                }
                val icon = when (profile) {
                    ProtectionProfile.STRICT_KIDS -> Icons.Default.Shield
                    ProtectionProfile.HOMEWORK_FOCUS -> Icons.AutoMirrored.Filled.MenuBook
                    ProtectionProfile.BALANCED_GUARD -> Icons.Default.Security
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 58.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) accent.copy(alpha = 0.14f) else AegisCardBackground)
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) accent else AegisCardBorder.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { onApplyProfile(profile) }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("profile_card_${profile.id}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) accent else accent.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = profile.title,
                                tint = if (isSelected) AegisOnPrimaryWhite else accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile.title,
                                color = AegisTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = ClassicalDisplayFont,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = profile.subtitle,
                                color = AegisTextSecondary,
                                fontSize = 10.5.sp,
                                fontFamily = ClassicalBodyFont,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (isSelected) accent else AegisSurfaceElevated)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) accent else AegisCardBorder,
                                shape = RoundedCornerShape(50)
                            )
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isSelected) "ACTIVE" else "APPLY",
                            color = if (isSelected) AegisOnPrimaryWhite else AegisTextSecondary,
                            fontSize = 10.sp,
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
private fun ProfileAutoSwitcherCard(
    preferences: AegisPreferences
) {
    val context = LocalContext.current
    val autoSwitchEnabled by preferences.profileAutoSwitchEnabled.collectAsState()
    val startMinutes by preferences.profileScheduleStartMinutes.collectAsState()
    val endMinutes by preferences.profileScheduleEndMinutes.collectAsState()
    val targetProfile by preferences.scheduledTargetProfile.collectAsState()
    val fallbackProfile by preferences.scheduledFallbackProfile.collectAsState()
    val lastSlot by preferences.lastAppliedAutoSwitchSlot.collectAsState()
    val discreetMode by preferences.discreetBlockingScreenMode.collectAsState()

    CyberCard(
        borderColor = AegisCardBorder,
        tone = CyberCardTone.SKY,
        modifier = Modifier.testTag("edits_profile_auto_switch_card")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Scheduled Profile Auto-Switcher",
                        color = AegisTextPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalDisplayFont
                    )
                    Text(
                        text = if (autoSwitchEnabled) {
                            "Active slot: ${lastSlot ?: "PENDING"} • ${targetProfile.title} inside window"
                        } else {
                            "Automatically switch between Homework & Family Guard by time"
                        },
                        color = AegisTextSecondary,
                        fontSize = 11.sp,
                        fontFamily = ClassicalBodyFont
                    )
                }
                Switch(
                    checked = autoSwitchEnabled,
                    onCheckedChange = { checked ->
                        AegisSoundEffects.playTap(context)
                        preferences.setProfileAutoSwitchEnabled(checked)
                        if (checked) {
                            val applied = ProfileAutoScheduler.evaluateAndApply(preferences, forceApply = true)
                            if (applied != null) {
                                Toast.makeText(
                                    context,
                                    DiscreetAlertFormatter.autoSwitchProfileToast(discreetMode, applied.title),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AegisOnPrimaryWhite,
                        checkedTrackColor = AegisPurple
                    ),
                    modifier = Modifier.testTag("edits_profile_auto_switch_toggle")
                )
            }

            if (autoSwitchEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ScheduleHourStepper(
                        label = "Start",
                        minuteOfDay = startMinutes,
                        onMinusHour = {
                            val next = (startMinutes - 60 + 1440) % 1440
                            preferences.setProfileScheduleWindow(next, endMinutes)
                            ProfileAutoScheduler.evaluateAndApply(preferences, forceApply = true)
                        },
                        onPlusHour = {
                            val next = (startMinutes + 60) % 1440
                            preferences.setProfileScheduleWindow(next, endMinutes)
                            ProfileAutoScheduler.evaluateAndApply(preferences, forceApply = true)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ScheduleHourStepper(
                        label = "End",
                        minuteOfDay = endMinutes,
                        onMinusHour = {
                            val next = (endMinutes - 60 + 1440) % 1440
                            preferences.setProfileScheduleWindow(startMinutes, next)
                            ProfileAutoScheduler.evaluateAndApply(preferences, forceApply = true)
                        },
                        onPlusHour = {
                            val next = (endMinutes + 60) % 1440
                            preferences.setProfileScheduleWindow(startMinutes, next)
                            ProfileAutoScheduler.evaluateAndApply(preferences, forceApply = true)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "Window Profile: ${targetProfile.title}  •  Rest-of-Day: ${fallbackProfile.title}",
                    color = AegisPurple,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = ClassicalBodyFont
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ProtectionProfile.entries.forEach { prof ->
                        val selected = targetProfile == prof
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) AegisPrimaryCyan else AegisCardBackground,
                            border = BorderStroke(1.dp, if (selected) AegisPrimaryCyan else AegisCardBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    preferences.setScheduledTargetProfile(prof)
                                    ProfileAutoScheduler.evaluateAndApply(preferences, forceApply = true)
                                }
                                .testTag("auto_target_${prof.id}")
                        ) {
                            Text(
                                text = prof.badge,
                                color = if (selected) AegisOnPrimaryWhite else AegisTextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickTileModeCard(
    preferences: AegisPreferences
) {
    val context = LocalContext.current
    val quickTileMode by preferences.quickTileMode.collectAsState()

    CyberCard(
        borderColor = AegisLavenderBorder,
        tone = CyberCardTone.LAVENDER,
        modifier = Modifier.testTag("edits_quick_tile_mode_selector")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Quick Settings Tile Tap Action",
                color = AegisTextPrimary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ClassicalDisplayFont
            )
            Text(
                text = "Tap Sheildlenz shade tile to toggle ${quickTileMode.label} directly",
                color = AegisTextSecondary,
                fontSize = 11.sp,
                fontFamily = ClassicalBodyFont
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickTileMode.entries.forEach { mode ->
                    val selected = quickTileMode == mode
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) AegisPurple else AegisCardBackground,
                        border = BorderStroke(1.dp, if (selected) AegisPurple else AegisCardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clickable {
                                AegisSoundEffects.playTap(context)
                                preferences.setQuickTileMode(mode)
                            }
                            .testTag("quick_tile_mode_${mode.id}")
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode.label,
                                color = if (selected) AegisOnPrimaryWhite else AegisTextPrimary,
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
}

@Composable
private fun AppLockerSection(
    preferences: AegisPreferences
) {
    val context = LocalContext.current
    val enabled by preferences.appLockerEnabled.collectAsState()
    val lockedPackages by preferences.lockedAppPackages.collectAsState()
    val relockMinutes by preferences.appLockerRelockMinutes.collectAsState()
    val hasGuardianPin by preferences.hasGuardianPin.collectAsState()

    var showAppPicker by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var installedApps by remember { mutableStateOf<List<InstalledAppEntry>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(false) }

    LaunchedEffect(showAppPicker) {
        if (showAppPicker && installedApps.isEmpty()) {
            isLoadingApps = true
            installedApps = loadInstalledLaunchableApps(context)
            isLoadingApps = false
        }
    }

    CyberCard(
        borderColor = AegisEmeraldBorder,
        tone = CyberCardTone.EMERALD,
        modifier = Modifier.testTag("edits_app_locker_card")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            .background(AegisEmeraldTeal.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = AegisEmeraldTeal,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Individual App Locker",
                            color = AegisTextPrimary,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = ClassicalDisplayFont,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = when {
                                !hasGuardianPin -> "Set a Guardian PIN below to gate locked apps"
                                enabled -> "Armed • ${lockedPackages.size} app(s) PIN-locked"
                                else -> "Off • Tap to gate selected apps with PIN"
                            },
                            color = if (enabled && hasGuardianPin) AegisEmeraldTeal else AegisTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = ClassicalBodyFont,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { next ->
                        AegisSoundEffects.playTap(context)
                        if (next && !hasGuardianPin) {
                            Toast.makeText(context, "Set a Guardian PIN first to lock apps", Toast.LENGTH_SHORT).show()
                        }
                        preferences.setAppLockerEnabled(next)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AegisOnPrimaryWhite,
                        checkedTrackColor = AegisEmeraldTeal
                    ),
                    modifier = Modifier.testTag("edits_app_locker_toggle")
                )
            }

            // Relock Window Selector
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = "Re-lock Window After PIN Unlock",
                    color = AegisTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalBodyFont
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0 to "Every Exit", 5 to "5m", 15 to "15m", 30 to "30m").forEach { (mins, label) ->
                        val selected = relockMinutes == mins
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) AegisEmeraldTeal else Color.White,
                            border = BorderStroke(1.dp, if (selected) AegisEmeraldTeal else AegisCardBorder),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 40.dp)
                                .clickable {
                                    AegisSoundEffects.playTap(context)
                                    preferences.setAppLockerRelockMinutes(mins)
                                }
                                .testTag("app_locker_relock_${mins}m")
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)) {
                                Text(
                                    text = label,
                                    color = if (selected) AegisOnPrimaryWhite else AegisTextPrimary,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    AegisSoundEffects.playTap(context)
                    searchQuery = ""
                    showAppPicker = true
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AegisEmeraldTeal,
                    contentColor = AegisOnPrimaryWhite
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("app_locker_select_apps_button")
            ) {
                Icon(Icons.Default.Apps, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Select Locked Apps (${lockedPackages.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showAppPicker) {
        val filtered = remember(installedApps, searchQuery, lockedPackages) {
            val q = searchQuery.trim().lowercase()
            val list = if (q.isEmpty()) installedApps else installedApps.filter {
                it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
            }
            list.sortedByDescending { lockedPackages.contains(it.packageName) }
        }
        AlertDialog(
            onDismissRequest = { showAppPicker = false },
            containerColor = AegisLavenderSoft,
            modifier = Modifier.testTag("app_locker_picker_dialog"),
            title = {
                Text(
                    text = "Lock Apps with Guardian PIN",
                    fontFamily = ClassicalDisplayFont,
                    fontWeight = FontWeight.Bold,
                    color = AegisTextPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search apps...", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (isLoadingApps) {
                        Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AegisEmeraldTeal, modifier = Modifier.size(28.dp))
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(filtered, key = { it.packageName }) { app ->
                                val isLocked = lockedPackages.contains(app.packageName)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isLocked) AegisEmeraldSoft else Color.White)
                                        .border(
                                            1.dp,
                                            if (isLocked) AegisEmeraldBorder else AegisCardBorder,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { preferences.toggleLockedAppPackage(app.packageName) }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (app.icon != null) {
                                            androidx.compose.foundation.Image(
                                                bitmap = app.icon,
                                                contentDescription = app.label,
                                                modifier = Modifier.size(28.dp).clip(RoundedCornerShape(7.dp))
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = app.label,
                                                color = AegisTextPrimary,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = app.packageName,
                                                color = AegisTextMuted,
                                                fontSize = 10.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Checkbox(
                                        checked = isLocked,
                                        onCheckedChange = { preferences.toggleLockedAppPackage(app.packageName) }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAppPicker = false },
                    colors = ButtonDefaults.buttonColors(containerColor = AegisEmeraldTeal, contentColor = AegisOnPrimaryWhite),
                    modifier = Modifier.testTag("app_locker_picker_done_button")
                ) {
                    Text("Done")
                }
            }
        )
    }
}




