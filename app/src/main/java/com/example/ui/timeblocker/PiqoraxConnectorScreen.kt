package com.example.ui.timeblocker

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.core.audio.AegisSoundEffects
import com.example.core.permissions.PermissionHelper
import com.example.data.preferences.AegisPreferences
import com.example.monitoring.accessibility.AegisAccessibilityService
import com.example.timeblocker.TimeBlockerEngine
import com.example.timeblocker.TimeBlockerRepository
import com.example.timeblocker.ui.TimeBlockLockActivity
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberCardTone
import com.example.ui.components.CyberTopBar
import com.example.ui.components.LuxurySectionHeader
import com.example.ui.components.SheildlenzBackdrop
import com.example.ui.components.StatusPill
import com.example.ui.components.rememberMasterKeyGate
import com.example.ui.settings.InstalledAppEntry
import com.example.ui.settings.loadInstalledLaunchableApps
import com.example.ui.theme.AegisCardBackground
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisChampagneSoft
import com.example.ui.theme.AegisEmeraldBorder
import com.example.ui.theme.AegisEmeraldSoft
import com.example.ui.theme.AegisEmeraldTeal
import com.example.ui.theme.AegisGoldBorder
import com.example.ui.theme.AegisLavenderBorder
import com.example.ui.theme.AegisOnPrimaryWhite
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisPurple
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.AegisWarningAmber
import com.example.ui.theme.ClassicalBodyFont
import com.example.ui.theme.ClassicalDisplayFont
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun PiqoraxConnectorScreen(
    preferences: AegisPreferences,
    timeBlockerRepository: TimeBlockerRepository,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val masterKeyGate = rememberMasterKeyGate(preferences)

    val isConnectorConnected by preferences.piqoraxConnectorConnected.collectAsState()
    val allowanceMinutes by preferences.timeBlockerAllowanceMinutes.collectAsState()
    val windowMinutes by preferences.timeBlockerWindowMinutes.collectAsState()
    val lastPassSummary by preferences.piqoraxLastPassSummary.collectAsState()
    val restrictedApps by timeBlockerRepository.restrictedApps.collectAsState()

    val isPiqoraxInstalled = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(TimeBlockLockActivity.PIQORAX_PACKAGE_NAME, 0)
            true
        }.getOrDefault(false)
    }

    var hasAccessibility by remember {
        mutableStateOf(PermissionHelper.isAccessibilityServiceEnabled(context, AegisAccessibilityService::class.java))
    }
    var installedApps by remember { mutableStateOf<List<InstalledAppEntry>>(emptyList()) }
    var showAppPicker by remember { mutableStateOf(false) }

    var allowanceInHours by remember { mutableStateOf(allowanceMinutes >= 60 && allowanceMinutes % 60 == 0) }
    var windowInHours by remember { mutableStateOf(windowMinutes >= 60 && windowMinutes % 60 == 0) }
    var elapsedTickMs by remember { mutableLongStateOf(timeBlockerRepository.getCurrentElapsedRealtime()) }

    LaunchedEffect(Unit) {
        installedApps = loadInstalledLaunchableApps(context)
        hasAccessibility = PermissionHelper.isAccessibilityServiceEnabled(context, AegisAccessibilityService::class.java)
    }

    LaunchedEffect(restrictedApps) {
        while (restrictedApps.isNotEmpty()) {
            elapsedTickMs = timeBlockerRepository.getCurrentElapsedRealtime()
            delay(1_000L)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasAccessibility = PermissionHelper.isAccessibilityServiceEnabled(
                    context,
                    AegisAccessibilityService::class.java
                )
                elapsedTickMs = timeBlockerRepository.getCurrentElapsedRealtime()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val appsByPkg = remember(installedApps) { installedApps.associateBy { it.packageName } }

    SheildlenzBackdrop(modifier = Modifier.testTag("piqorax_connector_screen_root")) {
        Scaffold(
            contentWindowInsets = WindowInsets.statusBars,
            topBar = {
                CyberTopBar(
                    title = "PiqoraX & App Blocker",
                    subtitle = "Connector Bridge & Minutes/Hours Limiter",
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
                // 1. Connect PiqoraX Connector Prompt Card
                item(key = "piqorax_connector_prompt") {
                    CyberCard(
                        borderColor = if (isConnectorConnected) AegisEmeraldBorder else AegisGoldBorder,
                        tone = if (isConnectorConnected) CyberCardTone.HERO else CyberCardTone.GOLD,
                        modifier = Modifier.testTag("piqorax_connector_prompt_card")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusPill(
                                label = if (isConnectorConnected) "CONNECTOR LINKED" else "NOT CONNECTED",
                                isActive = isConnectorConnected,
                                activeColor = AegisEmeraldTeal,
                                inactiveColor = AegisWarningAmber
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isPiqoraxInstalled) AegisEmeraldSoft else AegisChampagneSoft,
                                border = BorderStroke(1.dp, if (isPiqoraxInstalled) AegisEmeraldBorder else AegisGoldBorder)
                            ) {
                                Text(
                                    text = if (isPiqoraxInstalled) "PiqoraX Installed" else "PiqoraX Not Installed",
                                    color = if (isPiqoraxInstalled) AegisEmeraldTeal else AegisChampagneGold,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = ClassicalBodyFont,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isConnectorConnected) AegisEmeraldSoft else AegisChampagneSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = "PiqoraX Connector",
                                    tint = if (isConnectorConnected) AegisEmeraldTeal else AegisChampagneGold,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isConnectorConnected) {
                                        "PiqoraX Connector is Active"
                                    } else {
                                        "Connect PiqoraX Connector to Sheildlenz?"
                                    },
                                    color = AegisTextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = ClassicalDisplayFont
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = if (isConnectorConnected) {
                                        "Study passes earned in PiqoraX automatically unlock your timed-out apps."
                                    } else {
                                        "Connect the offline signature bridge so PiqoraX study sessions can unlock apps when time limits hit."
                                    },
                                    color = AegisTextSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    fontFamily = ClassicalBodyFont
                                )
                            }
                        }

                        if (lastPassSummary.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = AegisEmeraldSoft,
                                border = BorderStroke(1.dp, AegisEmeraldBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = AegisEmeraldTeal,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Last bridge activity: $lastPassSummary",
                                        color = AegisTextPrimary,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = ClassicalBodyFont
                                    )
                                }
                            }
                        }

                        if (!hasAccessibility) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = AegisChampagneSoft,
                                border = BorderStroke(1.dp, AegisGoldBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Enable Accessibility Guard so per-minute/hour locks activate automatically.",
                                        color = AegisTextPrimary,
                                        fontSize = 11.5.sp,
                                        fontFamily = ClassicalBodyFont,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 8.dp)
                                    )
                                    Button(
                                        onClick = {
                                            preferences.beginAuthorizedExternalFlow()
                                            runCatching {
                                                context.startActivity(PermissionHelper.getAccessibilitySettingsIntent())
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = AegisChampagneGold,
                                            contentColor = AegisOnPrimaryWhite
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp),
                                        modifier = Modifier
                                            .height(48.dp)
                                            .testTag("piqorax_allow_accessibility_button")
                                    ) {
                                        Text("ALLOW", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (isConnectorConnected) {
                                        masterKeyGate.request("disconnect PiqoraX connector") {
                                            preferences.setPiqoraxConnectorConnected(false)
                                            Toast.makeText(context, "PiqoraX Connector disconnected", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        AegisSoundEffects.playModeOn(context)
                                        preferences.setPiqoraxConnectorConnected(true)
                                        Toast.makeText(context, "PiqoraX Connector connected to Sheildlenz", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isConnectorConnected) AegisCardBackground else AegisEmeraldTeal,
                                    contentColor = if (isConnectorConnected) AegisWarningAmber else AegisOnPrimaryWhite
                                ),
                                border = if (isConnectorConnected) BorderStroke(1.dp, AegisWarningAmber.copy(alpha = 0.55f)) else null,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("piqorax_connect_button")
                            ) {
                                Icon(
                                    imageVector = if (isConnectorConnected) Icons.Default.LinkOff else Icons.Default.Link,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isConnectorConnected) "Disconnect" else "Connect Bridge",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    AegisSoundEffects.playTap(context)
                                    val launched = TimeBlockLockActivity.launchPiqoraxCompanion(
                                        context,
                                        context.packageName
                                    )
                                    if (!launched) {
                                        Toast.makeText(
                                            context,
                                            "PiqoraX (${TimeBlockLockActivity.PIQORAX_PACKAGE_NAME}) is not installed on this device.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                },
                                border = BorderStroke(1.2.dp, AegisChampagneGold),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AegisChampagneGold),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("piqorax_launch_app_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Open PiqoraX",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // 2. Start Blocking Apps Per Minutes or Hours
                item(key = "header_time_blocker_schedule") {
                    LuxurySectionHeader(
                        title = "App Blocking Timer (Minutes or Hours)",
                        accentColor = AegisPurple
                    )
                }

                item(key = "time_blocker_schedule_card") {
                    CyberCard(
                        borderColor = AegisLavenderBorder,
                        tone = CyberCardTone.LAVENDER,
                        modifier = Modifier.testTag("time_blocker_schedule_card")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = AegisPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Block Schedule Rule",
                                    color = AegisTextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = ClassicalDisplayFont
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = AegisEmeraldSoft,
                                border = BorderStroke(1.dp, AegisEmeraldBorder)
                            ) {
                                Text(
                                    text = "Allow ${TimeBlockerEngine.formatMinutesCompact(allowanceMinutes)} / ${TimeBlockerEngine.formatMinutesCompact(windowMinutes)}",
                                    color = AegisEmeraldTeal,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = ClassicalBodyFont,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Allowance Duration (Minutes or Hours)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Use Time Before Lock: ${TimeBlockerEngine.formatMinutesCompact(allowanceMinutes)}",
                                color = AegisTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = ClassicalBodyFont
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                UnitToggleChip(
                                    label = "Min",
                                    selected = !allowanceInHours,
                                    accentColor = AegisPrimaryCyan,
                                    tag = "allowance_unit_minutes",
                                    onClick = { allowanceInHours = false }
                                )
                                UnitToggleChip(
                                    label = "Hr",
                                    selected = allowanceInHours,
                                    accentColor = AegisPurple,
                                    tag = "allowance_unit_hours",
                                    onClick = {
                                        allowanceInHours = true
                                        if (allowanceMinutes < 60) {
                                            preferences.setTimeBlockerAllowanceMinutes(60)
                                        }
                                    }
                                )
                            }
                        }

                        if (allowanceInHours) {
                            val currentHours = (allowanceMinutes / 60).coerceIn(1, 3)
                            Slider(
                                value = currentHours.toFloat(),
                                onValueChange = { value ->
                                    val newMins = value.roundToInt().coerceIn(1, 3) * 60
                                    if (newMins > allowanceMinutes) {
                                        masterKeyGate.request("increase app time allowance to ${newMins / 60}h") {
                                            preferences.setTimeBlockerAllowanceMinutes(newMins)
                                        }
                                    } else {
                                        preferences.setTimeBlockerAllowanceMinutes(newMins)
                                    }
                                },
                                valueRange = 1f..3f,
                                steps = 1,
                                colors = SliderDefaults.colors(
                                    thumbColor = AegisPurple,
                                    activeTrackColor = AegisPurple
                                ),
                                modifier = Modifier.testTag("allowance_hours_slider")
                            )
                        } else {
                            Slider(
                                value = allowanceMinutes.coerceIn(1, 60).toFloat(),
                                onValueChange = { value ->
                                    val newMins = value.roundToInt().coerceIn(1, 60)
                                    if (newMins > allowanceMinutes) {
                                        masterKeyGate.request("increase app time allowance to ${newMins}m") {
                                            preferences.setTimeBlockerAllowanceMinutes(newMins)
                                        }
                                    } else {
                                        preferences.setTimeBlockerAllowanceMinutes(newMins)
                                    }
                                },
                                valueRange = 1f..60f,
                                colors = SliderDefaults.colors(
                                    thumbColor = AegisPrimaryCyan,
                                    activeTrackColor = AegisPrimaryCyan
                                ),
                                modifier = Modifier.testTag("allowance_minutes_slider")
                            )
                        }

                        PresetChipRow(
                            presets = listOf(5 to "5m", 10 to "10m", 15 to "15m", 30 to "30m", 60 to "1h", 120 to "2h"),
                            selectedMinutes = allowanceMinutes,
                            accentColor = AegisPrimaryCyan,
                            tagPrefix = "allowance_preset",
                            onSelect = { targetMins ->
                                allowanceInHours = targetMins >= 60
                                if (targetMins > allowanceMinutes) {
                                    masterKeyGate.request("raise allowance to ${TimeBlockerEngine.formatMinutesCompact(targetMins)}") {
                                        preferences.setTimeBlockerAllowanceMinutes(targetMins)
                                    }
                                } else {
                                    AegisSoundEffects.playTap(context)
                                    preferences.setTimeBlockerAllowanceMinutes(targetMins)
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Reset Window Duration (Minutes or Hours)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Reset Every: ${TimeBlockerEngine.formatMinutesCompact(windowMinutes)}",
                                color = AegisTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = ClassicalBodyFont
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                UnitToggleChip(
                                    label = "Min",
                                    selected = !windowInHours,
                                    accentColor = AegisPrimaryCyan,
                                    tag = "window_unit_minutes",
                                    onClick = { windowInHours = false }
                                )
                                UnitToggleChip(
                                    label = "Hr",
                                    selected = windowInHours,
                                    accentColor = AegisPurple,
                                    tag = "window_unit_hours",
                                    onClick = {
                                        windowInHours = true
                                        if (windowMinutes < 60) {
                                            preferences.setTimeBlockerWindowMinutes(60)
                                        }
                                    }
                                )
                            }
                        }

                        if (windowInHours) {
                            val currentWinHrs = (windowMinutes / 60).coerceIn(1, 24)
                            val minWinHrs = ((allowanceMinutes + 59) / 60).coerceIn(1, 24)
                            Slider(
                                value = currentWinHrs.coerceAtLeast(minWinHrs).toFloat(),
                                onValueChange = { value ->
                                    val newMins = value.roundToInt().coerceIn(minWinHrs, 24) * 60
                                    if (newMins < windowMinutes) {
                                        masterKeyGate.request("shorten reset window to ${newMins / 60}h") {
                                            preferences.setTimeBlockerWindowMinutes(newMins)
                                        }
                                    } else {
                                        preferences.setTimeBlockerWindowMinutes(newMins)
                                    }
                                },
                                valueRange = minWinHrs.toFloat().coerceAtMost(23f)..24f,
                                colors = SliderDefaults.colors(
                                    thumbColor = AegisPurple,
                                    activeTrackColor = AegisPurple
                                ),
                                modifier = Modifier.testTag("window_hours_slider")
                            )
                        } else {
                            val minWinMins = allowanceMinutes.coerceIn(5, 115)
                            Slider(
                                value = windowMinutes.coerceIn(minWinMins, 120).toFloat(),
                                onValueChange = { value ->
                                    val newMins = value.roundToInt().coerceIn(minWinMins, 120)
                                    if (newMins < windowMinutes) {
                                        masterKeyGate.request("shorten reset window to ${newMins}m") {
                                            preferences.setTimeBlockerWindowMinutes(newMins)
                                        }
                                    } else {
                                        preferences.setTimeBlockerWindowMinutes(newMins)
                                    }
                                },
                                valueRange = minWinMins.toFloat()..120f,
                                colors = SliderDefaults.colors(
                                    thumbColor = AegisPrimaryCyan,
                                    activeTrackColor = AegisPrimaryCyan
                                ),
                                modifier = Modifier.testTag("window_minutes_slider")
                            )
                        }

                        PresetChipRow(
                            presets = listOf(15 to "15m", 30 to "30m", 60 to "1h", 120 to "2h", 360 to "6h", 1440 to "24h"),
                            selectedMinutes = windowMinutes,
                            accentColor = AegisPurple,
                            tagPrefix = "window_preset",
                            onSelect = { targetMins ->
                                val adjusted = targetMins.coerceAtLeast(allowanceMinutes)
                                windowInHours = adjusted >= 60
                                if (adjusted < windowMinutes) {
                                    masterKeyGate.request("shorten reset window to ${TimeBlockerEngine.formatMinutesCompact(adjusted)}") {
                                        preferences.setTimeBlockerWindowMinutes(adjusted)
                                    }
                                } else {
                                    AegisSoundEffects.playTap(context)
                                    preferences.setTimeBlockerWindowMinutes(adjusted)
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    AegisSoundEffects.playTap(context)
                                    showAppPicker = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AegisPrimaryCyan,
                                    contentColor = AegisOnPrimaryWhite
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("time_blocker_start_blocking_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddCircleOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (restrictedApps.isEmpty()) "Start Blocking Apps" else "Add / Edit Apps",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (restrictedApps.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = {
                                        AegisSoundEffects.playModeOn(context)
                                        timeBlockerRepository.updateAllAppsSchedule(allowanceMinutes, windowMinutes)
                                        Toast.makeText(
                                            context,
                                            "Updated ${restrictedApps.size} timed app(s) to ${TimeBlockerEngine.formatMinutesCompact(allowanceMinutes)} / ${TimeBlockerEngine.formatMinutesCompact(windowMinutes)}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    border = BorderStroke(1.2.dp, AegisPurple),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AegisPurple),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("time_blocker_apply_all_button")
                                ) {
                                    Text(
                                        text = "Apply All",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Active Timed-Blocked Apps List
                item(key = "header_active_timed_apps") {
                    LuxurySectionHeader(
                        title = "Timed-Blocked Apps (${restrictedApps.size})",
                        accentColor = AegisEmeraldTeal
                    )
                }

                if (restrictedApps.isEmpty()) {
                    item(key = "empty_timed_apps") {
                        CyberCard(
                            borderColor = AegisCardBorder,
                            tone = CyberCardTone.SKY,
                            modifier = Modifier.testTag("time_blocker_empty_apps_card")
                        ) {
                            Text(
                                text = "No apps are scheduled for minute/hour time blocking yet.",
                                color = AegisTextPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = ClassicalDisplayFont
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Pick apps above to allow a set number of minutes or hours before Sheildlenz locks them—and optionally connect PiqoraX to earn study-unlocked passes.",
                                color = AegisTextSecondary,
                                fontSize = 12.sp,
                                fontFamily = ClassicalBodyFont
                            )
                        }
                    }
                } else {
                    items(restrictedApps, key = { it.packageName }) { entity ->
                        val appInfo = appsByPkg[entity.packageName]
                        val appLabel = appInfo?.label ?: entity.packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
                        val decision = remember(entity, elapsedTickMs) {
                            TimeBlockerEngine.evaluate(entity, elapsedTickMs)
                        }
                        TimeBlockedAppRowCard(
                            entity = entity,
                            appLabel = appLabel,
                            appIcon = appInfo?.icon,
                            decision = decision,
                            onResetWindow = {
                                masterKeyGate.request("reset timer for $appLabel") {
                                    timeBlockerRepository.resetAppWindowNow(entity.packageName)
                                    Toast.makeText(context, "Timer reset for $appLabel", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onRemoveApp = {
                                masterKeyGate.request("stop blocking $appLabel") {
                                    timeBlockerRepository.removeTimeBlockedApp(entity.packageName)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAppPicker) {
        val currentSet = remember(restrictedApps) { restrictedApps.map { it.packageName }.toSet() }
        TimeBlockerAppPickerDialog(
            installedApps = installedApps,
            initiallySelectedPackages = currentSet,
            allowanceMinutes = allowanceMinutes,
            windowMinutes = windowMinutes,
            onDismiss = { showAppPicker = false },
            onConfirmSelection = { chosenPackages ->
                val removed = currentSet - chosenPackages
                val added = chosenPackages - currentSet
                val executeChanges = {
                    removed.forEach { pkg -> timeBlockerRepository.removeTimeBlockedApp(pkg) }
                    chosenPackages.forEach { pkg ->
                        timeBlockerRepository.addOrUpdateTimeBlockedApp(
                            packageName = pkg,
                            allowanceMinutes = allowanceMinutes,
                            windowMinutes = windowMinutes
                        )
                    }
                    showAppPicker = false
                    Toast.makeText(
                        context,
                        "Time blocker active for ${chosenPackages.size} app(s)",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                if (removed.isNotEmpty() && added.isEmpty()) {
                    masterKeyGate.request("remove ${removed.size} app(s) from time blocker") {
                        executeChanges()
                    }
                } else if (removed.isNotEmpty()) {
                    masterKeyGate.request("update blocked app list") {
                        executeChanges()
                    }
                } else {
                    AegisSoundEffects.playModeOn(context)
                    executeChanges()
                }
            }
        )
    }
}
