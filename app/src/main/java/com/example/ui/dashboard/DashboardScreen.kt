package com.example.ui.dashboard

import android.app.Activity
import android.net.VpnService
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.example.ui.theme.AegisDangerRed
import com.example.ui.theme.AegisWarningAmber
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.example.ui.components.AnimatedCountdownRing
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.core.audio.AegisSoundEffects
import com.example.core.models.SensitivityLevel
import com.example.core.permissions.PermissionHelper
import com.example.core.update.ApkDownloadState
import com.example.core.update.UpdateChecker
import com.example.data.preferences.AegisPreferences
import com.example.data.repository.ProtectionRepository
import com.example.detection.classifier.ModelDownloader
import com.example.detection.classifier.ModelImportResult
import com.example.detection.classifier.ModelManager
import com.example.services.dns.AegisSafeDnsVpnService
import com.example.services.protection.MasterPowerController
import com.example.timeblocker.ui.TimeBlockLockActivity
import com.example.ui.components.AegisShieldGraphic
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberCardTone
import com.example.ui.components.CyberTopBar
import com.example.ui.components.HomeModelDownloadCard
import com.example.ui.components.ModelManagerCard
import com.example.ui.components.ProjectedOrbitPoint
import com.example.ui.components.SafeBrowserBrandIcon
import com.example.ui.components.SheildlenzBackdrop
import com.example.ui.components.drawVolumetric3DSphere
import com.example.ui.components.projectOrbitPoint3D
import com.example.ui.components.rememberMasterKeyGate
import com.example.ui.components.vipAnimatedCorners
import com.example.ui.theme.AegisCardBackground
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisChampagneSoft
import com.example.ui.theme.AegisElectricBlue
import com.example.ui.theme.AegisEmeraldBorder
import com.example.ui.theme.AegisEmeraldTeal
import com.example.ui.theme.AegisGoldBorder
import com.example.ui.theme.AegisGoldBright
import com.example.ui.theme.AegisLavenderBorder
import com.example.ui.theme.AegisLavenderLight
import com.example.ui.theme.AegisLavenderSoft
import com.example.ui.theme.AegisOnPrimaryWhite
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisPurple
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.ClassicalBodyFont
import com.example.ui.theme.ClassicalDisplayFont
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DashboardScreen(
    protectionRepo: ProtectionRepository,
    modelManager: ModelManager,
    preferences: AegisPreferences,
    onModelUpdated: () -> Unit = {},
    onRequestStartProtection: () -> Unit = {},
    onRequestStopProtection: () -> Unit = {},
    onNavigateToStatus: () -> Unit = {},
    onNavigateToProgress: () -> Unit = {},
    onNavigateToWellbeing: () -> Unit = {},
    onNavigateToFocusHub: () -> Unit = {},
    onNavigateToSafeBrowser: () -> Unit = {},
    onNavigateToPiqorax: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val telemetry by protectionRepo.telemetry.collectAsState()
    val sensitivity by preferences.sensitivity.collectAsState()
    val scheduleEnabled by preferences.scheduleEnabled.collectAsState()
    val scheduleStartMinutes by preferences.scheduleStartMinutes.collectAsState()
    val scheduleEndMinutes by preferences.scheduleEndMinutes.collectAsState()
    val isWithinScheduledWindow = remember(scheduleEnabled, scheduleStartMinutes, scheduleEndMinutes) {
        scheduleEnabled && preferences.isWithinScheduledWindow()
    }
    val studyModeEnabled by preferences.studyModeEnabled.collectAsState()
    val studySessionEndMillis by preferences.studySessionEndMillis.collectAsState()
    val studyDurationMinutes by preferences.studyDurationMinutes.collectAsState()
    val appTimeBudgets by preferences.appTimeBudgets.collectAsState()
    val safeDnsEnabled by preferences.safeDnsEnabled.collectAsState()
    val bedtimeEnabled by preferences.bedtimeEnabled.collectAsState()
    val bedtimeStartMinutes by preferences.bedtimeStartMinutes.collectAsState()
    val bedtimeEndMinutes by preferences.bedtimeEndMinutes.collectAsState()
    val strictEverything by preferences.strictEverything.collectAsState()
    val antiTamperEnabled by preferences.antiTamperEnabled.collectAsState()
    val isAutoStart by preferences.autoStart.collectAsState()
    val waitCooldownBeforePin by preferences.waitCooldownBeforePassword.collectAsState()
    val pauseWhenScreenOff by preferences.pauseWhenScreenOff.collectAsState()
    val skipDuplicateFrames by preferences.skipDuplicateFrames.collectAsState()
    val isMasterPowerOn by preferences.masterPowerEnabled.collectAsState()
    val fullBrowserInstalled by preferences.fullBrowserEngineInstalled.collectAsState()

    val isPiqoraxInstalled = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(TimeBlockLockActivity.PIQORAX_PACKAGE_NAME, 0)
            true
        }.getOrDefault(false)
    }

    val isStudyActive = remember(studyModeEnabled, studySessionEndMillis) {
        studyModeEnabled && preferences.getRemainingStudyMillis() > 0L
    }

    var showOverlayWarningDialog by remember { mutableStateOf(false) }
    var showMissingModelDialog by remember { mutableStateOf(false) }

    val vpnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            AegisSoundEffects.playModeOn(context)
            preferences.setSafeDnsEnabled(true)
            AegisSafeDnsVpnService.start(context)
            Toast.makeText(context, "Safe DNS VPN active", Toast.LENGTH_SHORT).show()
        } else {
            preferences.setSafeDnsEnabled(false)
            Toast.makeText(context, "VPN permission declined", Toast.LENGTH_SHORT).show()
        }
    }

    val quickModelPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val res = withContext(Dispatchers.IO) { modelManager.importModelFromUri(uri) }
                onModelUpdated()
                if (res is ModelImportResult.Success && telemetry.isOverlayPermitted && !telemetry.isProtectionEnabled) {
                    onRequestStartProtection()
                }
            }
        }
    }

    val attemptStartProtection = {
        when {
            !isMasterPowerOn -> Toast.makeText(
                context,
                "Master Power is OFF in About. Turn Master Power ON first.",
                Toast.LENGTH_SHORT
            ).show()
            !telemetry.isOverlayPermitted -> showOverlayWarningDialog = true
            !telemetry.isModelLoaded -> showMissingModelDialog = true
            else -> onRequestStartProtection()
        }
    }

    val masterKeyGate = rememberMasterKeyGate(preferences)
    val isOn = telemetry.isProtectionEnabled

    val cardPulseScale = remember { Animatable(1f) }
    val buttonBounceScale = remember { Animatable(1f) }
    var previousProtectionState by remember { mutableStateOf<Boolean?>(null) }

    val requestShieldToggle: (Boolean) -> Unit = { targetActive ->
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            buttonBounceScale.snapTo(0.94f)
            buttonBounceScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
        if (targetActive) {
            AegisSoundEffects.playModeOn(context)
            attemptStartProtection()
        } else {
            masterKeyGate.request("turn Shield OFF") {
                onRequestStopProtection()
            }
        }
    }

    LaunchedEffect(isOn) {
        val prev = previousProtectionState
        previousProtectionState = isOn
        if (prev != null && prev != isOn) {
            cardPulseScale.snapTo(if (isOn) 0.968f else 1.018f)
            cardPulseScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    // Continuous VIP living animations for stage halo, status indicator, and button sheen
    val infiniteTransition = rememberInfiniteTransition(label = "VipHomeMotion")
    val sonarWave1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SonarWave1"
    )
    val sonarWave2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, delayMillis = 1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SonarWave2"
    )
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RingRotation"
    )
    val statusDotPulse by infiniteTransition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.38f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "StatusDotPulse"
    )
    val powerPulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PowerPulseScale"
    )
    val buttonSheenSlide by infiniteTransition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ButtonSheenSlide"
    )
    val eqWavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "EqWavePhase"
    )

    val heroElevation by animateDpAsState(
        targetValue = if (isOn) 18.dp else 10.dp,
        animationSpec = tween(durationMillis = 480),
        label = "HeroElevation"
    )
    val heroShadowColor by animateColorAsState(
        targetValue = if (isOn) AegisPrimaryCyan.copy(alpha = 0.26f) else AegisPurple.copy(alpha = 0.16f),
        animationSpec = tween(durationMillis = 480),
        label = "HeroShadow"
    )
    val heroBorderEnd by animateColorAsState(
        targetValue = if (isOn) AegisPrimaryCyan.copy(alpha = 0.58f) else AegisLavenderBorder.copy(alpha = 0.62f),
        animationSpec = tween(durationMillis = 480),
        label = "HeroBorderEnd"
    )
    val buttonGradStart by animateColorAsState(
        targetValue = if (isOn) AegisPrimaryCyan else AegisPurple,
        animationSpec = tween(durationMillis = 420),
        label = "BtnGradStart"
    )
    val buttonGradEnd by animateColorAsState(
        targetValue = if (isOn) AegisElectricBlue else AegisPrimaryCyan,
        animationSpec = tween(durationMillis = 420),
        label = "BtnGradEnd"
    )
    val powerIconRotation by animateFloatAsState(
        targetValue = if (isOn) 360f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "PowerIconRotation"
    )

    val updateInfo by UpdateChecker.updateInfo.collectAsState()
    val apkDownloadState by UpdateChecker.apkDownloadState.collectAsState()
    val homeBannerHidden by UpdateChecker.homeBannerHidden.collectAsState()

    val hasPin by preferences.hasGuardianPin.collectAsState()
    var showMissingPinDialog by remember { mutableStateOf(!hasPin) }

    if (showMissingPinDialog && !hasPin) {
        var setupPinInput by remember { mutableStateOf("") }
        var setupEmailInput by remember { mutableStateOf(preferences.guardianEmail.value) }
        var pinSetupError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = {},
            containerColor = AegisCardBackground,
            title = {
                Text(
                    text = "Master Key Protection Required",
                    fontFamily = ClassicalDisplayFont,
                    fontWeight = FontWeight.Bold,
                    color = AegisDangerRed
                )
            },
            text = {
                Column {
                    Text(
                        text = "Without a Master Key PIN and recovery email, shielding controls are not enough to prevent unauthorized removal or bypass. Please set up your Master Key now.",
                        fontFamily = ClassicalBodyFont,
                        fontSize = 13.sp,
                        color = AegisTextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = setupPinInput,
                        onValueChange = { if (it.length <= 8 && it.all { ch -> ch.isDigit() }) setupPinInput = it },
                        label = { Text("Master Key PIN (4-8 digits)") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = setupEmailInput,
                        onValueChange = { setupEmailInput = it },
                        label = { Text("Guardian Recovery Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pinSetupError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = pinSetupError!!, color = AegisWarningAmber, fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanPin = setupPinInput.trim()
                        val cleanEmail = setupEmailInput.trim()
                        if (cleanPin.length !in 4..8) {
                            pinSetupError = "Enter a 4-8 digit PIN."
                        } else if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
                            pinSetupError = "Enter a valid email."
                        } else {
                            preferences.setGuardianPin(cleanPin)
                            preferences.setGuardianEmail(cleanEmail)
                            showMissingPinDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AegisPrimaryCyan)
                ) {
                    Text("SAVE MASTER KEY", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    val showUpdateBanner = updateInfo?.isUpdateAvailable == true &&
        !homeBannerHidden &&
        apkDownloadState !is ApkDownloadState.Installed

    val hasAnyExtraCard = showUpdateBanner || (isWithinScheduledWindow && !isOn) || !telemetry.isModelLoaded

    SheildlenzBackdrop(modifier = Modifier.testTag("dashboard_screen_root")) {
        Scaffold(
            contentWindowInsets = WindowInsets.statusBars,
            topBar = {
                CyberTopBar(title = "Home")
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 18.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.Top),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 0. Update Banner
                item(key = "home_update") {
                    AnimatedVisibility(
                        visible = showUpdateBanner,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        val info = updateInfo
                        if (info != null) {
                            val currentLabel = "v${UpdateChecker.cleanVersionTag(BuildConfig.VERSION_NAME)}"
                            val latestLabel = "v${UpdateChecker.cleanVersionTag(info.latestVersion)}"
                            val isReadyPreloaded = (apkDownloadState as? ApkDownloadState.ReadyToInstall)?.preloadedReady == true
                            val isConflict = apkDownloadState is ApkDownloadState.SignatureConflict
                            CyberCard(
                                borderColor = if (isReadyPreloaded) AegisLavenderBorder else AegisGoldBorder,
                                tone = if (isReadyPreloaded) CyberCardTone.LAVENDER else CyberCardTone.GOLD,
                                modifier = Modifier
                                    .widthIn(max = 580.dp)
                                    .testTag("home_update_banner")
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
                                            .padding(end = if (isReadyPreloaded) 10.dp else 0.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isReadyPreloaded) AegisPurple else AegisChampagneGold),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Download,
                                                contentDescription = "Update available",
                                                tint = AegisOnPrimaryWhite,
                                                modifier = Modifier.size(19.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isReadyPreloaded) {
                                                    "Update Ready: $latestLabel"
                                                } else {
                                                    "Update Available: $latestLabel"
                                                },
                                                color = AegisTextPrimary,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = ClassicalDisplayFont,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = when (val st = apkDownloadState) {
                                                    is ApkDownloadState.Preloading -> {
                                                        val pct = st.progress?.let { "${(it * 100).toInt()}%" } ?: "..."
                                                        "Preloading silently (100 KB/s) • $pct"
                                                    }
                                                    is ApkDownloadState.Downloading -> {
                                                        val pct = st.progress?.let { "${(it * 100).toInt()}%" } ?: "..."
                                                        "Downloading inside app • $pct"
                                                    }
                                                    is ApkDownloadState.PausedLowBattery -> {
                                                        val saved = UpdateChecker.formatBytesMb(st.bytesDownloaded)
                                                        "Preload paused (Battery <= 20%) • $saved"
                                                    }
                                                    ApkDownloadState.Verifying -> "Verifying SHA-256 & APK integrity..."
                                                    ApkDownloadState.Installing -> "Launching in-app installer..."
                                                    is ApkDownloadState.ReadyToInstall -> when {
                                                        st.needsInstallPermission -> "Allow 'Install unknown apps' to finish"
                                                        st.preloadedReady -> "Verified Ready • Instant 1-Tap Install"
                                                        else -> "App $currentLabel → $latestLabel"
                                                    }
                                                    is ApkDownloadState.SignatureConflict -> st.reason
                                                    is ApkDownloadState.Failed -> st.message
                                                    else -> "App $currentLabel → Latest $latestLabel"
                                                },
                                                color = if (isReadyPreloaded) AegisPurple else AegisChampagneGold,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                fontFamily = ClassicalBodyFont,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    if (isReadyPreloaded) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(AegisPurple.copy(alpha = 0.14f))
                                                .border(1.dp, AegisLavenderBorder, RoundedCornerShape(8.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "READY 0s",
                                                color = AegisPurple,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = ClassicalBodyFont,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }

                                val showActionButton = apkDownloadState !is ApkDownloadState.Verifying &&
                                    apkDownloadState !is ApkDownloadState.Installing
                                if (showActionButton) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    when (val st = apkDownloadState) {
                                        is ApkDownloadState.Preloading -> {
                                            Button(
                                                onClick = {
                                                    UpdateChecker.triggerUserUpdateAction(context)
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = AegisPrimaryCyan,
                                                    contentColor = AegisOnPrimaryWhite
                                                ),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(48.dp)
                                                    .testTag("home_finish_preload_button")
                                            ) {
                                                Text(
                                                    text = "FINISH NOW",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                        is ApkDownloadState.Downloading -> {
                                            OutlinedButton(
                                                onClick = { UpdateChecker.cancelApkDownload(context) },
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(48.dp)
                                                    .testTag("home_cancel_download_button")
                                            ) {
                                                Text(
                                                    text = "CANCEL DOWNLOAD",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                        is ApkDownloadState.ReadyToInstall -> {
                                            Button(
                                                onClick = {
                                                    UpdateChecker.triggerUserUpdateAction(context)
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = AegisPurple,
                                                    contentColor = AegisOnPrimaryWhite
                                                ),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(48.dp)
                                                    .testTag("home_install_ready_button")
                                            ) {
                                                Text(
                                                    text = when {
                                                        st.needsInstallPermission -> "ALLOW & INSTALL"
                                                        st.preloadedReady -> "INSTALL (0s)"
                                                        else -> "INSTALL UPDATE"
                                                    },
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                        else -> {
                                            Button(
                                                onClick = {
                                                    UpdateChecker.triggerUserUpdateAction(context)
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isConflict) AegisChampagneGold else AegisPrimaryCyan,
                                                    contentColor = AegisOnPrimaryWhite
                                                ),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(48.dp)
                                                    .testTag("home_download_update_button")
                                            ) {
                                                Text(
                                                    text = when {
                                                        isConflict -> "UPGRADE"
                                                        st is ApkDownloadState.Failed -> "RETRY DOWNLOAD"
                                                        else -> "DOWNLOAD & UPDATE"
                                                    },
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                    }
                                }

                                if (apkDownloadState is ApkDownloadState.Preloading ||
                                    apkDownloadState is ApkDownloadState.Downloading ||
                                    apkDownloadState is ApkDownloadState.Verifying ||
                                    apkDownloadState is ApkDownloadState.Installing
                                ) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val frac = when (val st = apkDownloadState) {
                                        is ApkDownloadState.Preloading -> st.progress
                                        is ApkDownloadState.Downloading -> st.progress
                                        else -> null
                                    }
                                    val barColor = if (apkDownloadState is ApkDownloadState.Downloading) AegisPrimaryCyan else AegisChampagneGold
                                    if (frac != null) {
                                        LinearProgressIndicator(
                                            progress = { frac },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = barColor,
                                            trackColor = barColor.copy(alpha = 0.2f),
                                            strokeCap = StrokeCap.Round
                                        )
                                    } else {
                                        LinearProgressIndicator(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = barColor,
                                            trackColor = barColor.copy(alpha = 0.2f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 0b. Master Power OFF Banner (0% Background Battery Mode)
                item(key = "home_master_power_off") {
                    AnimatedVisibility(
                        visible = !isMasterPowerOn,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        CyberCard(
                            borderColor = AegisGoldBorder,
                            tone = CyberCardTone.GOLD,
                            modifier = Modifier
                                .widthIn(max = 580.dp)
                                .testTag("home_master_power_off_banner")
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
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(AegisWarningAmber),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PowerSettingsNew,
                                            contentDescription = "Master Power Off",
                                            tint = AegisOnPrimaryWhite,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Master Power is OFF",
                                            color = AegisTextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = ClassicalDisplayFont,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "0% background battery • All services, accessibility & overlays stopped",
                                            color = AegisWarningAmber,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = ClassicalBodyFont,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        AegisSoundEffects.playModeOn(context)
                                        MasterPowerController.setMasterPowerEnabled(context, true)
                                        Toast.makeText(
                                            context,
                                            "Sheildlenz Master Power ON",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AegisEmeraldTeal,
                                        contentColor = AegisOnPrimaryWhite
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("home_master_power_on_button")
                                ) {
                                    Text(
                                        text = "POWER ON",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }

                // 1. Pure VIP Shield-Only Sanctuary Hero Widget
                item(key = "home_hero_shield") {
                    val heroShape = RoundedCornerShape(34.dp)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 540.dp)
                            .graphicsLayer {
                                scaleX = cardPulseScale.value
                                scaleY = cardPulseScale.value
                            }
                            .shadow(
                                elevation = heroElevation,
                                shape = heroShape,
                                ambientColor = heroShadowColor,
                                spotColor = heroShadowColor
                            )
                            .vipAnimatedCorners(
                                cornerRadius = 34.dp,
                                accentStart = if (isOn) AegisElectricBlue else AegisPrimaryCyan,
                                accentEnd = AegisPurple,
                                strokeWidth = 2.2.dp
                            )
                            .testTag("vip_quantum_shield_card"),
                        shape = heroShape,
                        color = Color.Transparent,
                        border = BorderStroke(
                            width = 1.4.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.96f),
                                    if (isOn) AegisElectricBlue.copy(alpha = 0.62f) else heroBorderEnd
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
                                            Color(0xFFAEE2FF), // Charming Luminous Sky Blue Top
                                            Color(0xFFCDEEFF), // Radiant Soft Sky Mid
                                            Color(0xFFEAF6FF)  // Airy Pearl Sky Base
                                        )
                                    )
                                )
                                .padding(horizontal = 24.dp, vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Top Status & Sensitivity Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(Color.White.copy(alpha = 0.72f))
                                        .border(
                                            width = 1.dp,
                                            color = if (isOn) AegisElectricBlue.copy(alpha = 0.45f)
                                            else AegisLavenderBorder.copy(alpha = 0.68f),
                                            shape = RoundedCornerShape(50)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .graphicsLayer {
                                                scaleX = statusDotPulse
                                                scaleY = statusDotPulse
                                            }
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    !isMasterPowerOn -> AegisWarningAmber
                                                    isOn -> AegisPrimaryCyan
                                                    else -> AegisPurple.copy(alpha = 0.80f)
                                                }
                                            )
                                    )
                                    Text(
                                        text = when {
                                            !isMasterPowerOn -> "POWER OFF"
                                            isOn -> "ACTIVE"
                                            else -> "STANDBY"
                                        },
                                        color = when {
                                            !isMasterPowerOn -> AegisWarningAmber
                                            isOn -> AegisPrimaryCyan
                                            else -> AegisPurple
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.2.sp,
                                        fontFamily = ClassicalBodyFont
                                    )
                                }

                                Text(
                                    text = sensitivity.name.lowercase().replaceFirstChar { it.uppercase() },
                                    color = AegisTextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = ClassicalBodyFont
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Pure VIP Orbital Shield Stage with continuous fluid ripples & 3D orbiting spheres
                            Box(
                                modifier = Modifier
                                    .size(216.dp)
                                    .clip(CircleShape)
                                    .clickable { requestShieldToggle(!isOn) }
                                    .testTag("shield_graphic_toggle"),
                                contentAlignment = Alignment.Center
                            ) {
                                val outerOrbColors = if (isOn) {
                                    listOf(AegisElectricBlue)
                                } else {
                                    listOf(AegisPurple)
                                }

                                // Back hemisphere canvas (ripples, halo track, and z < 0 distant 3D spheres)
                                Canvas(modifier = Modifier.size(216.dp)) {
                                    val c = Offset(this.size.width / 2f, this.size.height / 2f)
                                    val maxR = this.size.minDimension * 0.48f

                                    // Expanding soothing radar ripples
                                    listOf(sonarWave1 to AegisElectricBlue, sonarWave2 to AegisPurple).forEach { (wave, waveColor) ->
                                        val r = maxR * (0.54f + 0.44f * wave)
                                        val alpha = (1f - wave) * (if (isOn) 0.48f else 0.24f)
                                        drawCircle(
                                            color = waveColor.copy(alpha = alpha),
                                            radius = r,
                                            center = c,
                                            style = Stroke(width = 2.6.dp.toPx() * (1f - wave * 0.45f))
                                        )
                                    }

                                    // Subtle dotted celestial track + smooth sweep glow ring
                                    val trackR = maxR * 0.90f
                                    drawCircle(
                                        color = (if (isOn) AegisPrimaryCyan else AegisLavenderBorder).copy(alpha = 0.26f),
                                        radius = trackR,
                                        center = c,
                                        style = Stroke(
                                            width = 1.5.dp.toPx(),
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 12f), 0f)
                                        )
                                    )

                                    val arcR = maxR * 0.85f
                                    val arcTopLeft = Offset(c.x - arcR, c.y - arcR)
                                    val arcSize = Size(arcR * 2f, arcR * 2f)
                                    rotate(degrees = ringRotation, pivot = c) {
                                        drawArc(
                                            brush = Brush.sweepGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    (if (isOn) AegisElectricBlue else AegisPurple).copy(alpha = 0.45f),
                                                    (if (isOn) AegisPrimaryCyan else AegisLavenderLight).copy(alpha = 0.85f),
                                                    Color.Transparent
                                                ),
                                                center = c
                                            ),
                                            startAngle = 0f,
                                            sweepAngle = 360f,
                                            useCenter = false,
                                            topLeft = arcTopLeft,
                                            size = arcSize,
                                            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
                                        )
                                    }

                                    // Subtle tilted 3D orbital ellipse back arc
                                    rotate(degrees = 16f, pivot = c) {
                                        val orbRx = maxR * 0.90f
                                        val orbRy = maxR * 0.54f
                                        drawOval(
                                            color = (if (isOn) AegisPrimaryCyan else AegisPurple).copy(alpha = 0.14f),
                                            topLeft = Offset(c.x - orbRx, c.y - orbRy),
                                            size = Size(orbRx * 2f, orbRy * 2f),
                                            style = Stroke(width = 1.1.dp.toPx())
                                        )
                                    }

                                    // Distant back-hemisphere 3D spheres (z < 0, pass behind shield)
                                    val orbRx = maxR * 0.90f
                                    val orbRy = maxR * 0.54f
                                    val orbZ = maxR * 0.66f
                                    val camDist = this.size.minDimension * 2.15f
                                    val baseRadiusPx = 5.0.dp.toPx()

                                    (0 until 4)
                                        .map { i ->
                                            val deg = ringRotation + i * 90f
                                            val pt = projectOrbitPoint3D(
                                                center = c,
                                                radiusX = orbRx,
                                                radiusY = orbRy,
                                                zAmplitude = orbZ,
                                                angleDeg = deg,
                                                tiltDeg = 16f,
                                                cameraDistance = camDist
                                            )
                                            Triple(i, deg, pt)
                                        }
                                        .filter { (_, _, pt) -> pt.zNormalized < 0f }
                                        .sortedBy { (_, _, pt) -> pt.zNormalized }
                                        .forEach { (i, _, pt) ->
                                            val orbColor = outerOrbColors[i % outerOrbColors.size]
                                            val depthAlpha = (0.56f + 0.34f * (pt.zNormalized + 1f)).coerceIn(0.45f, 0.90f)
                                            drawVolumetric3DSphere(
                                                center = pt.offset,
                                                radius = baseRadiusPx * pt.perspectiveScale,
                                                baseColor = orbColor,
                                                alpha = depthAlpha,
                                                depthNormalized = pt.zNormalized,
                                                drawHaloAndShadow = true
                                            )
                                        }
                                }

                                AegisShieldGraphic(
                                    isProtected = isOn,
                                    isThreatActive = false,
                                    size = 160.dp
                                )

                                // Front hemisphere canvas (z >= 0 foreground 3D spheres passing in front of outer stage)
                                Canvas(modifier = Modifier.size(216.dp)) {
                                    val c = Offset(this.size.width / 2f, this.size.height / 2f)
                                    val maxR = this.size.minDimension * 0.48f
                                    val orbRx = maxR * 0.90f
                                    val orbRy = maxR * 0.54f
                                    val orbZ = maxR * 0.66f
                                    val camDist = this.size.minDimension * 2.15f
                                    val baseRadiusPx = 5.0.dp.toPx()

                                    (0 until 4)
                                        .map { i ->
                                            val deg = ringRotation + i * 90f
                                            val pt: ProjectedOrbitPoint = projectOrbitPoint3D(
                                                center = c,
                                                radiusX = orbRx,
                                                radiusY = orbRy,
                                                zAmplitude = orbZ,
                                                angleDeg = deg,
                                                tiltDeg = 16f,
                                                cameraDistance = camDist
                                            )
                                            Triple(i, deg, pt)
                                        }
                                        .filter { (_, _, pt) -> pt.zNormalized >= 0f }
                                        .sortedBy { (_, _, pt) -> pt.zNormalized }
                                        .forEach { (i, _, pt) ->
                                            val orbColor = outerOrbColors[i % outerOrbColors.size]
                                            val depthAlpha = (0.88f + 0.12f * pt.zNormalized).coerceIn(0.86f, 1f)
                                            drawVolumetric3DSphere(
                                                center = pt.offset,
                                                radius = baseRadiusPx * pt.perspectiveScale,
                                                baseColor = orbColor,
                                                alpha = depthAlpha,
                                                depthNormalized = pt.zNormalized,
                                                drawHaloAndShadow = true
                                            )
                                        }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            AnimatedContent(
                                targetState = isOn,
                                transitionSpec = {
                                    (fadeIn(tween(280)) + scaleIn(tween(280), initialScale = 0.94f))
                                        .togetherWith(fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.96f))
                                },
                                label = "VipQuantumHeadline"
                            ) { armed ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = when {
                                            !isMasterPowerOn -> "Master Power Off"
                                            armed -> "Shield Active"
                                            else -> "Shield Off"
                                        },
                                        color = AegisTextPrimary,
                                        fontSize = 25.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = ClassicalDisplayFont,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = when {
                                            !isMasterPowerOn -> "0% background battery & zero processes"
                                            armed -> "On-device visual guard running"
                                            else -> "Ready to protect screen"
                                        },
                                        color = when {
                                            !isMasterPowerOn -> AegisWarningAmber
                                            armed -> AegisPrimaryCyan
                                            else -> AegisTextSecondary
                                        },
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = ClassicalBodyFont,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(22.dp))

                            // ─── PURE ANIMATED VIP CAPSULE ACTION BUTTON (No Switch UI) ───
                            val capsuleShape = RoundedCornerShape(50)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp)
                                    .graphicsLayer {
                                        scaleX = buttonBounceScale.value
                                        scaleY = buttonBounceScale.value
                                    }
                                    .shadow(
                                        elevation = if (isOn) 14.dp else 9.dp,
                                        shape = capsuleShape,
                                        ambientColor = buttonGradStart.copy(alpha = 0.36f),
                                        spotColor = buttonGradEnd.copy(alpha = 0.36f)
                                    )
                                    .clip(capsuleShape)
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(buttonGradStart, buttonGradEnd)
                                        )
                                    )
                                    .border(
                                        width = 1.2.dp,
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.80f),
                                                Color.White.copy(alpha = 0.25f)
                                            )
                                        ),
                                        shape = capsuleShape
                                    )
                                    .clickable { requestShieldToggle(!isOn) }
                                    .testTag("master_protection_switch"),
                                contentAlignment = Alignment.Center
                            ) {
                                // Smooth glass shimmer wave gliding across the animated VIP button
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val sheenCenterX = size.width * buttonSheenSlide
                                    drawRect(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.White.copy(alpha = 0.24f),
                                                Color.Transparent
                                            ),
                                            start = Offset(sheenCenterX - size.width * 0.22f, 0f),
                                            end = Offset(sheenCenterX + size.width * 0.22f, size.height)
                                        )
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Glowing orb power emblem
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .graphicsLayer {
                                                scaleX = powerPulseScale
                                                scaleY = powerPulseScale
                                            }
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.22f))
                                            .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PowerSettingsNew,
                                            contentDescription = if (isOn) "Pause screen shield" else "Activate screen shield",
                                            tint = AegisOnPrimaryWhite,
                                            modifier = Modifier
                                                .size(21.dp)
                                                .graphicsLayer {
                                                    rotationZ = powerIconRotation
                                                }
                                        )
                                    }

                                    AnimatedContent(
                                        targetState = isOn,
                                        transitionSpec = {
                                            (fadeIn(tween(240)) + scaleIn(tween(240), initialScale = 0.94f))
                                                .togetherWith(fadeOut(tween(160)))
                                        },
                                        label = "PureVipButtonText"
                                    ) { armed ->
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = if (armed) "SHIELD ACTIVE" else "PROTECT SCREEN",
                                                color = AegisOnPrimaryWhite,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.0.sp,
                                                fontFamily = ClassicalDisplayFont
                                            )
                                            Text(
                                                text = if (armed) "Tap to pause guard" else "Tap to start protection",
                                                color = AegisOnPrimaryWhite.copy(alpha = 0.86f),
                                                fontSize = 10.5.sp,
                                                fontFamily = ClassicalBodyFont
                                            )
                                        }
                                    }

                                    // Animated live pulse wave bars (no switch look!)
                                    Row(
                                        modifier = Modifier
                                            .padding(end = 6.dp)
                                            .width(26.dp),
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val h1 = if (isOn) 10.dp + (10.dp * eqWavePhase) else 8.dp + (4.dp * eqWavePhase)
                                        val h2 = if (isOn) 20.dp - (9.dp * eqWavePhase) else 13.dp - (4.dp * eqWavePhase)
                                        val h3 = if (isOn) 12.dp + (8.dp * eqWavePhase) else 9.dp + (3.dp * eqWavePhase)
                                        listOf(h1, h2, h3).forEach { barH ->
                                            Box(
                                                modifier = Modifier
                                                    .width(4.dp)
                                                    .height(barH)
                                                    .clip(RoundedCornerShape(50))
                                                    .background(Color.White.copy(alpha = if (isOn) 0.95f else 0.78f))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 1aa. VIP Safe Browser Card on Home (Clean, uncrowded launch row)
                item(key = "home_safe_browser") {
                    CyberCard(
                        borderColor = AegisLavenderBorder,
                        tone = CyberCardTone.SKY,
                        onClick = {
                            AegisSoundEffects.playTap(context)
                            onNavigateToSafeBrowser()
                        },
                        modifier = Modifier
                            .widthIn(max = 540.dp)
                            .testTag("home_safe_browser_card")
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
                                    .padding(end = 12.dp)
                            ) {
                                SafeBrowserBrandIcon(
                                    size = 44.dp,
                                    isProUnlocked = fullBrowserInstalled
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Safe Browser",
                                            color = AegisTextPrimary,
                                            fontSize = 15.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = ClassicalDisplayFont,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .background(
                                                    if (fullBrowserInstalled) AegisPurple.copy(alpha = 0.14f)
                                                    else AegisPrimaryCyan.copy(alpha = 0.14f)
                                                )
                                                .padding(horizontal = 7.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (fullBrowserInstalled) "PRO" else "SAFE",
                                                color = if (fullBrowserInstalled) AegisPurple else AegisPrimaryCyan,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                    Text(
                                        text = if (fullBrowserInstalled) {
                                            "Chrome-style engine • Tabs & dual Shield"
                                        } else {
                                            "Clean Chrome-style private browser"
                                        },
                                        color = AegisTextSecondary,
                                        fontSize = 11.5.sp,
                                        fontFamily = ClassicalBodyFont,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    AegisSoundEffects.playTap(context)
                                    onNavigateToSafeBrowser()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (fullBrowserInstalled) AegisPurple else AegisPrimaryCyan,
                                    contentColor = AegisOnPrimaryWhite
                                ),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .widthIn(min = 84.dp)
                                    .testTag("home_safe_browser_open_button")
                            ) {
                                Text(
                                    text = "OPEN",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                if (isPiqoraxInstalled) {
                    item(key = "home_piqorax_companion") {
                        CyberCard(
                            borderColor = AegisGoldBorder,
                            tone = CyberCardTone.HERO,
                            modifier = Modifier
                                .widthIn(max = 540.dp)
                                .testTag("home_piqorax_companion_card")
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
                                        .padding(end = 12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(AegisChampagneSoft),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VerifiedUser,
                                            contentDescription = "Piqorax Study Companion",
                                            tint = AegisChampagneGold,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Piqorax Study Companion",
                                            color = AegisTextPrimary,
                                            fontSize = 14.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = ClassicalDisplayFont,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Connect PiqoraX • Coin lock & app blocker",
                                            color = AegisTextSecondary,
                                            fontSize = 11.5.sp,
                                            fontFamily = ClassicalBodyFont,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Button(
                                    onClick = {
                                        AegisSoundEffects.playTap(context)
                                        onNavigateToPiqorax()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AegisChampagneGold,
                                        contentColor = AegisOnPrimaryWhite
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .widthIn(min = 96.dp)
                                        .testTag("home_open_piqorax_button")
                                ) {
                                    Text(
                                        text = "CONNECT",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }

                // 1a. Increase-Only Live Timer Banners (isolated ticking keeps rest of Home crisp)
                if (isStudyActive) {
                    item(key = "home_active_study") {
                        ActiveStudyExtendBanner(
                            preferences = preferences,
                            studySessionEndMillis = studySessionEndMillis,
                            studyDurationMinutes = studyDurationMinutes,
                            modifier = Modifier.widthIn(max = 540.dp)
                        )
                    }
                }

                if (bedtimeEnabled) {
                    item(key = "home_active_bedtime") {
                        CyberCard(
                            borderColor = AegisGoldBorder,
                            tone = CyberCardTone.GOLD,
                            modifier = Modifier
                                .widthIn(max = 540.dp)
                                .testTag("home_active_bedtime_banner")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AnimatedCountdownRing(
                                    progress = 0.72f,
                                    centerText = "",
                                    primaryColor = AegisChampagneGold,
                                    secondaryColor = AegisPurple,
                                    ringSize = 44.dp,
                                    strokeWidth = 4.dp,
                                    centerIconContent = {
                                        Icon(
                                            imageVector = Icons.Default.Bedtime,
                                            contentDescription = "Bedtime Schedule Active",
                                            tint = AegisChampagneGold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Bedtime Lock Active",
                                        color = AegisTextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = ClassicalDisplayFont
                                    )
                                    Text(
                                        text = "${formatCompactHour(bedtimeStartMinutes)}–${formatCompactHour(bedtimeEndMinutes)} • Manage in Edits",
                                        color = AegisChampagneGold,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = ClassicalBodyFont
                                    )
                                }
                            }
                        }
                    }
                }

                // 1b. Dynamic 6-Tile Switch-ON Strip (Auto-rotates active controls out, zero PIN prompts)
                item(key = "home_quick_tiles") {
                    val quickTilePool = listOf(
                        HomeQuickTileItem(
                            title = "Study Lock",
                            status = "Tap ${studyDurationMinutes}m",
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            accentColor = AegisPurple,
                            borderColor = AegisLavenderBorder,
                            testTag = "home_one_tap_study",
                            isEnabled = isStudyActive,
                            onEnable = {
                                if (!isMasterPowerOn) {
                                    Toast.makeText(
                                        context,
                                        "Shutdown is OFF in About. Turn Shutdown ON first.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else if (!telemetry.isOverlayPermitted) {
                                    showOverlayWarningDialog = true
                                } else {
                                    AegisSoundEffects.playModeOn(context)
                                    preferences.startStudySession(studyDurationMinutes)
                                }
                            }
                        ),
                        HomeQuickTileItem(
                            title = "App Limits",
                            status = "Tap Set",
                            icon = Icons.Default.Timer,
                            accentColor = AegisEmeraldTeal,
                            borderColor = AegisEmeraldBorder,
                            testTag = "home_one_tap_budgets",
                            isEnabled = appTimeBudgets.isNotEmpty(),
                            onEnable = {
                                AegisSoundEffects.playTap(context)
                                onNavigateToFocusHub()
                            }
                        ),
                        HomeQuickTileItem(
                            title = "Safe DNS",
                            status = "Tap ON",
                            icon = Icons.Default.Dns,
                            accentColor = AegisPrimaryCyan,
                            borderColor = AegisCardBorder,
                            testTag = "home_one_tap_safe_dns",
                            isEnabled = safeDnsEnabled,
                            onEnable = {
                                if (!isMasterPowerOn) {
                                    Toast.makeText(
                                        context,
                                        "Shutdown is OFF in About. Turn Shutdown ON first.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    val prep = runCatching { VpnService.prepare(context) }.getOrNull()
                                    if (prep == null) {
                                        AegisSoundEffects.playModeOn(context)
                                        preferences.setSafeDnsEnabled(true)
                                        AegisSafeDnsVpnService.start(context)
                                    } else {
                                        preferences.beginAuthorizedExternalFlow()
                                        vpnLauncher.launch(prep)
                                    }
                                }
                            }
                        ),
                        HomeQuickTileItem(
                            title = "Bedtime",
                            status = "Tap ON",
                            icon = Icons.Default.Bedtime,
                            accentColor = AegisChampagneGold,
                            borderColor = AegisGoldBorder,
                            testTag = "home_one_tap_bedtime",
                            isEnabled = bedtimeEnabled,
                            onEnable = {
                                AegisSoundEffects.playModeOn(context)
                                preferences.setBedtimeEnabled(true)
                            }
                        ),
                        HomeQuickTileItem(
                            title = "Strict Filter",
                            status = "Tap ON",
                            icon = Icons.Default.Shield,
                            accentColor = AegisPurple,
                            borderColor = AegisLavenderBorder,
                            testTag = "home_one_tap_strict",
                            isEnabled = strictEverything || sensitivity == SensitivityLevel.STRICT,
                            onEnable = {
                                AegisSoundEffects.playModeOn(context)
                                preferences.setStrictEverything(true)
                            }
                        ),
                        HomeQuickTileItem(
                            title = "Quiet Hours",
                            status = "Tap ON",
                            icon = Icons.Default.Schedule,
                            accentColor = AegisPurple,
                            borderColor = AegisLavenderBorder,
                            testTag = "home_one_tap_schedule",
                            isEnabled = scheduleEnabled,
                            onEnable = {
                                AegisSoundEffects.playModeOn(context)
                                preferences.setScheduleEnabled(true)
                            }
                        ),
                        HomeQuickTileItem(
                            title = "Anti-Tamper",
                            status = "Tap ON",
                            icon = Icons.Default.Lock,
                            accentColor = AegisChampagneGold,
                            borderColor = AegisGoldBorder,
                            testTag = "home_one_tap_anti_tamper",
                            isEnabled = antiTamperEnabled,
                            onEnable = {
                                AegisSoundEffects.playModeOn(context)
                                preferences.setAntiTamperEnabled(true)
                            }
                        ),
                        HomeQuickTileItem(
                            title = "Boot Start",
                            status = "Tap ON",
                            icon = Icons.Default.PlayArrow,
                            accentColor = AegisEmeraldTeal,
                            borderColor = AegisEmeraldBorder,
                            testTag = "home_one_tap_autostart",
                            isEnabled = isAutoStart,
                            onEnable = {
                                AegisSoundEffects.playModeOn(context)
                                preferences.setAutoStart(true)
                            }
                        ),
                        HomeQuickTileItem(
                            title = "Cooldown PIN",
                            status = "Tap ON",
                            icon = Icons.Default.Bolt,
                            accentColor = AegisChampagneGold,
                            borderColor = AegisGoldBorder,
                            testTag = "home_one_tap_cooldown",
                            isEnabled = waitCooldownBeforePin,
                            onEnable = {
                                AegisSoundEffects.playModeOn(context)
                                preferences.setWaitCooldownBeforePassword(true)
                            }
                        ),
                        HomeQuickTileItem(
                            title = "Idle Pause",
                            status = "Tap ON",
                            icon = Icons.Default.VisibilityOff,
                            accentColor = AegisPrimaryCyan,
                            borderColor = AegisCardBorder,
                            testTag = "home_one_tap_idle_pause",
                            isEnabled = pauseWhenScreenOff,
                            onEnable = {
                                AegisSoundEffects.playModeOn(context)
                                preferences.setPauseWhenScreenOff(true)
                            }
                        ),
                        HomeQuickTileItem(
                            title = "Static Skip",
                            status = "Tap ON",
                            icon = Icons.Default.CheckCircle,
                            accentColor = AegisEmeraldTeal,
                            borderColor = AegisEmeraldBorder,
                            testTag = "home_one_tap_static_skip",
                            isEnabled = skipDuplicateFrames,
                            onEnable = {
                                AegisSoundEffects.playModeOn(context)
                                preferences.setSkipDuplicateFrames(true)
                            }
                        )
                    )
                    val visibleTiles = quickTilePool.filter { !it.isEnabled }.take(6)

                    if (visibleTiles.isEmpty()) {
                        CyberCard(
                            borderColor = AegisEmeraldBorder,
                            tone = CyberCardTone.EMERALD,
                            modifier = Modifier
                                .widthIn(max = 540.dp)
                                .testTag("home_all_guards_active_banner")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "All Quick Guards Active",
                                    tint = AegisEmeraldTeal,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "All Quick Guards Active • Manage in Edits",
                                    color = AegisTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = ClassicalBodyFont
                                )
                            }
                        }
                    } else {
                        val row1 = visibleTiles.take(3)
                        val row2 = visibleTiles.drop(3).take(3)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 540.dp)
                                .testTag("home_one_tap_features_strip"),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            row1.forEach { tile ->
                                HomeOneTapFeatureTile(
                                    title = tile.title,
                                    status = tile.status,
                                    icon = tile.icon,
                                    active = false,
                                    accentColor = tile.accentColor,
                                    borderColor = tile.borderColor,
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        tile.onEnable()
                                    },
                                    testTag = tile.testTag,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(3 - row1.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }

                        if (row2.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 540.dp)
                                    .testTag("home_one_tap_priority_strip"),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                row2.forEach { tile ->
                                    HomeOneTapFeatureTile(
                                        title = tile.title,
                                        status = tile.status,
                                        icon = tile.icon,
                                        active = false,
                                        accentColor = tile.accentColor,
                                        borderColor = tile.borderColor,
                                        onClick = {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            tile.onEnable()
                                        },
                                        testTag = tile.testTag,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                repeat(3 - row2.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                // 2. Scheduled Guard Window Reminder
                item(key = "home_scheduled_guard") {
                    AnimatedVisibility(
                        visible = isWithinScheduledWindow && !isOn,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        CyberCard(
                            borderColor = AegisLavenderBorder,
                            tone = CyberCardTone.LAVENDER,
                            modifier = Modifier
                                .widthIn(max = 560.dp)
                                .testTag("scheduled_guard_banner")
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
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(AegisPurple),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = "Scheduled Guard Active",
                                            tint = AegisOnPrimaryWhite,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Scheduled Guard Window",
                                            color = AegisTextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = ClassicalDisplayFont,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${AegisPreferences.formatClockTime(scheduleStartMinutes)} – ${AegisPreferences.formatClockTime(scheduleEndMinutes)} active now",
                                            color = AegisPurple,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = ClassicalBodyFont,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Button(
                                    onClick = { attemptStartProtection() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AegisPrimaryCyan,
                                        contentColor = AegisOnPrimaryWhite
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("schedule_arm_now_button")
                                ) {
                                    Text(
                                        text = "ARM",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = ClassicalBodyFont,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. First-Time Model Setup Card
                item(key = "home_model_setup") {
                    AnimatedVisibility(
                        visible = !telemetry.isModelLoaded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        HomeModelDownloadCard(
                            telemetry = telemetry,
                            modelManager = modelManager,
                            onModelUpdated = onModelUpdated,
                            onBeforeOpenPicker = { preferences.beginAuthorizedExternalFlow() },
                            modifier = Modifier.widthIn(max = 540.dp)
                        )
                    }
                }

            }

            if (showOverlayWarningDialog) {
                AlertDialog(
                    onDismissRequest = { showOverlayWarningDialog = false },
                    containerColor = AegisLavenderSoft,
                    titleContentColor = AegisTextPrimary,
                    textContentColor = AegisTextSecondary,
                    title = { Text("Overlay Permission Required", fontFamily = ClassicalDisplayFont, fontWeight = FontWeight.Bold) },
                    text = {
                        Text(
                            "Sheildlenz requires 'Display over other apps' permission to shield the screen when unsafe visual content is detected.",
                            fontFamily = ClassicalBodyFont
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showOverlayWarningDialog = false
                                try {
                                    preferences.grantTemporarySettingsAuthorization()
                                    context.startActivity(PermissionHelper.getOverlaySettingsIntent(context))
                                } catch (_: Exception) {
                                    context.startActivity(PermissionHelper.getAppDetailsSettingsIntent(context))
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AegisPurple, contentColor = AegisOnPrimaryWhite)
                        ) {
                            Text("GRANT PERMISSION", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { showOverlayWarningDialog = false },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = AegisCardBackground,
                                contentColor = AegisTextPrimary
                            )
                        ) {
                            Text("CANCEL", fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
            }

            if (showMissingModelDialog) {
                AlertDialog(
                    onDismissRequest = { showMissingModelDialog = false },
                    containerColor = AegisChampagneSoft,
                    titleContentColor = AegisTextPrimary,
                    textContentColor = AegisTextSecondary,
                    title = { Text("Download AI Vision Model", fontFamily = ClassicalDisplayFont, fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Download the lightweight AI Vision Model (~4 MB) for full visual detection inside Chrome and all apps. You can also load a local .tflite file or start Browser URL Guard now.",
                                fontFamily = ClassicalBodyFont
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        showMissingModelDialog = false
                                        preferences.beginAuthorizedExternalFlow()
                                        quickModelPickerLauncher.launch(arrayOf("*/*"))
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = AegisCardBackground,
                                        contentColor = AegisTextPrimary
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("PICK .TFLITE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = {
                                        showMissingModelDialog = false
                                        onRequestStartProtection()
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = AegisLavenderSoft,
                                        contentColor = AegisPurple
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("WEB GUARD", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showMissingModelDialog = false
                                ModelDownloader.startDownload(
                                    context = context,
                                    modelManager = modelManager,
                                    linkIndex = 1,
                                    onCompleted = { onModelUpdated() }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AegisPrimaryCyan, contentColor = AegisOnPrimaryWhite)
                        ) {
                            Text("DOWNLOAD AI MODEL", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { showMissingModelDialog = false },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = AegisCardBackground,
                                contentColor = AegisTextSecondary
                            )
                        ) {
                            Text("CANCEL", fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ActiveStudyExtendBanner(
    preferences: AegisPreferences,
    studySessionEndMillis: Long,
    studyDurationMinutes: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var remainingMillis by remember(studySessionEndMillis) {
        mutableLongStateOf(preferences.getRemainingStudyMillis())
    }
    LaunchedEffect(studySessionEndMillis) {
        while (true) {
            val rem = preferences.getRemainingStudyMillis()
            remainingMillis = rem
            if (rem <= 0L) break
            delay(1000L)
        }
    }
    if (remainingMillis <= 0L) return

    val totalStudyMs = (studyDurationMinutes.coerceAtLeast(1) * 60_000L).toFloat()
    val ringProgress = (remainingMillis / totalStudyMs).coerceIn(0.05f, 1f)

    CyberCard(
        borderColor = AegisLavenderBorder,
        tone = CyberCardTone.LAVENDER,
        modifier = modifier.testTag("home_active_study_extend_banner")
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
                AnimatedCountdownRing(
                    progress = ringProgress,
                    centerText = "",
                    primaryColor = AegisPurple,
                    secondaryColor = AegisPrimaryCyan,
                    ringSize = 46.dp,
                    strokeWidth = 4.dp,
                    centerIconContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = "Study Lock Running",
                            tint = AegisPurple,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Study Lock • ${formatHomeClock(remainingMillis)}",
                        color = AegisTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalDisplayFont
                    )
                    Text(
                        text = "Active • Extend focus time anytime",
                        color = AegisPurple,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = ClassicalBodyFont
                    )
                }
            }
            Button(
                onClick = {
                    AegisSoundEffects.playModeOn(context)
                    preferences.extendActiveStudySession(15)
                    Toast.makeText(context, "Study Lock extended +15m", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AegisPurple,
                    contentColor = AegisOnPrimaryWhite
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("home_extend_study_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("+15M", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private data class HomeQuickTileItem(
    val title: String,
    val status: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val accentColor: Color,
    val borderColor: Color,
    val testTag: String,
    val isEnabled: Boolean,
    val onEnable: () -> Unit
)

@Composable
private fun HomeOneTapFeatureTile(
    title: String,
    status: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    accentColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val tileShape = RoundedCornerShape(18.dp)
    Surface(
        onClick = onClick,
        shape = tileShape,
        color = if (active) accentColor.copy(alpha = 0.14f) else AegisCardBackground.copy(alpha = 0.92f),
        border = BorderStroke(
            width = if (active) 1.5.dp else 1.dp,
            color = if (active) accentColor else borderColor.copy(alpha = 0.85f)
        ),
        modifier = modifier
            .heightIn(min = 76.dp)
            .shadow(
                elevation = if (active) 6.dp else 3.dp,
                shape = tileShape,
                ambientColor = accentColor.copy(alpha = 0.14f),
                spotColor = accentColor.copy(alpha = 0.14f)
            )
            .vipAnimatedCorners(
                cornerRadius = 18.dp,
                accentStart = accentColor,
                accentEnd = borderColor,
                strokeWidth = 1.6.dp
            )
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(if (active) accentColor else accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (active) AegisOnPrimaryWhite else accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                color = AegisTextPrimary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ClassicalDisplayFont,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = status,
                color = if (active) accentColor else AegisTextSecondary,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ClassicalBodyFont,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatHomeClock(millis: Long): String {
    val totalSec = (millis / 1000L).coerceAtLeast(0L)
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}

private fun formatCompactHour(minuteOfDay: Int): String {
    val safe = ((minuteOfDay % 1440) + 1440) % 1440
    val hour24 = safe / 60
    val min = safe % 60
    val suffix = if (hour24 < 12) "A" else "P"
    val hour12 = when {
        hour24 == 0 -> 12
        hour24 > 12 -> hour24 - 12
        else -> hour24
    }
    return if (min == 0) "$hour12$suffix" else String.format(Locale.US, "%d:%02d%s", hour12, min, suffix)
}

