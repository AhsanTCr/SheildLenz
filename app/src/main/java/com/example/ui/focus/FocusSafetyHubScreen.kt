package com.example.ui.focus

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.audio.AegisSoundEffects
import com.example.core.models.SafeSearchPack
import com.example.core.permissions.PermissionHelper
import com.example.data.preferences.AegisPreferences
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberCardTone
import com.example.ui.components.CyberTopBar
import com.example.ui.components.LuxurySectionHeader
import com.example.ui.components.SheildlenzBackdrop
import com.example.ui.settings.InstalledAppEntry
import com.example.ui.settings.loadInstalledLaunchableApps
import com.example.ui.theme.AegisCardBackground
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisChampagneSoft
import com.example.ui.theme.AegisEmeraldBorder
import com.example.ui.theme.AegisEmeraldTeal
import com.example.ui.theme.AegisGoldBorder
import com.example.ui.theme.AegisIndigo
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
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FocusSafetyHubScreen(
    preferences: AegisPreferences,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    val studyModeEnabled by preferences.studyModeEnabled.collectAsState()
    val studySessionEndMillis by preferences.studySessionEndMillis.collectAsState()
    val studyDurationMinutes by preferences.studyDurationMinutes.collectAsState()
    val studySoftWarningSeconds by preferences.studySoftWarningSeconds.collectAsState()
    val studyAllowedPackages by preferences.studyAllowedPackages.collectAsState()

    val smartSafeSearchEnabled by preferences.smartSafeSearchEnabled.collectAsState()
    val activePacks by preferences.safeSearchPacks.collectAsState()
    val safeSearchBlockedCount by preferences.safeSearchBlockedCount.collectAsState()
    val customKeywords by preferences.customBlockedKeywords.collectAsState()

    val appTimeBudgets by preferences.appTimeBudgets.collectAsState()
    val dailyAppUsageSeconds by preferences.dailyAppUsageSeconds.collectAsState()

    var remainingStudyMillis by remember { mutableLongStateOf(preferences.getRemainingStudyMillis()) }
    LaunchedEffect(studyModeEnabled, studySessionEndMillis) {
        if (!studyModeEnabled) {
            remainingStudyMillis = 0L
        } else {
            while (true) {
                val rem = preferences.getRemainingStudyMillis()
                remainingStudyMillis = rem
                if (rem <= 0L) break
                delay(1000L)
            }
        }
    }
    val isStudyActive = studyModeEnabled && remainingStudyMillis > 0L

    var showStudyAppsDialog by remember { mutableStateOf(false) }
    var showBudgetPickerDialog by remember { mutableStateOf(false) }
    var showOverlayRequiredDialog by remember { mutableStateOf(false) }
    var customKeywordInput by remember { mutableStateOf("") }

    var installedApps by remember { mutableStateOf<List<InstalledAppEntry>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(false) }

    LaunchedEffect(showStudyAppsDialog, showBudgetPickerDialog) {
        if ((showStudyAppsDialog || showBudgetPickerDialog) && installedApps.isEmpty()) {
            isLoadingApps = true
            installedApps = loadInstalledLaunchableApps(context)
            isLoadingApps = false
        }
    }

    BackHandler { onNavigateBack() }

    SheildlenzBackdrop(modifier = Modifier.testTag("focus_safety_hub_root")) {
        Scaffold(
            contentWindowInsets = WindowInsets.statusBars,
            topBar = {
                CyberTopBar(
                    title = "Focus & Safety Hub",
                    subtitle = "Study Timer, Safe Search Packs & App Budgets",
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
                contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ─────────────────────────────────────────────────────────────────────────────
                // 1. Study Mode & Focus Lock Timer (Soft Warning Countdown)
                // ─────────────────────────────────────────────────────────────────────────────
                item {
                    LuxurySectionHeader(
                        title = "Study Mode & Focus Lock",
                        accentColor = AegisPurple,
                        modifier = Modifier.widthIn(max = 600.dp)
                    )
                }
                item {
                    CyberCard(
                        borderColor = if (isStudyActive) AegisPurple else AegisLavenderBorder,
                        tone = CyberCardTone.LAVENDER,
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .testTag("focus_study_card")
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
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(AegisPurple.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                        contentDescription = "Study Mode Timer",
                                        tint = AegisPurple,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (isStudyActive) "Study Lock Active" else "Focus Lock Timer",
                                        color = AegisTextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = ClassicalDisplayFont
                                    )
                                    Text(
                                        text = "Soft warning (${studySoftWarningSeconds}s countdown) before locking distracting apps",
                                        color = AegisTextSecondary,
                                        fontSize = 11.5.sp,
                                        fontFamily = ClassicalBodyFont
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isStudyActive) AegisPurple else AegisSurfaceElevated)
                                    .border(1.dp, AegisPurple.copy(alpha = 0.4f), RoundedCornerShape(50))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = if (isStudyActive) formatStudyClock(remainingStudyMillis) else "${studyDurationMinutes}M",
                                    color = if (isStudyActive) AegisOnPrimaryWhite else AegisPurple,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Session Duration",
                            color = AegisTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = ClassicalBodyFont
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(15, 25, 45, 60, 90).forEach { mins ->
                                val selected = studyDurationMinutes == mins
                                OutlinedButton(
                                    onClick = {
                                        AegisSoundEffects.playTap(context)
                                        if (isStudyActive && mins < studyDurationMinutes) {
                                            Toast.makeText(
                                                context,
                                                "Extend time here or stop in Edits",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            preferences.setStudyDurationMinutes(mins)
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (selected) AegisPurple else AegisCardBackground,
                                        contentColor = if (selected) AegisOnPrimaryWhite else AegisTextPrimary
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("study_duration_${mins}m")
                                ) {
                                    Text("${mins}m", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Soft Warning Countdown Before Lock",
                            color = AegisTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = ClassicalBodyFont
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(15, 30, 60).forEach { sec ->
                                val selected = studySoftWarningSeconds == sec
                                OutlinedButton(
                                    onClick = {
                                        AegisSoundEffects.playTap(context)
                                        preferences.setStudySoftWarningSeconds(sec)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (selected) AegisPrimaryCyan else AegisCardBackground,
                                        contentColor = if (selected) AegisOnPrimaryWhite else AegisTextPrimary
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("study_soft_warn_${sec}s")
                                ) {
                                    Text("${sec}s Warning", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    AegisSoundEffects.playTap(context)
                                    showStudyAppsDialog = true
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = AegisCardBackground,
                                    contentColor = AegisTextPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("focus_edit_study_apps_button")
                            ) {
                                Icon(Icons.Default.Apps, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "STUDY APPS (${studyAllowedPackages.size})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Button(
                                onClick = {
                                    if (isStudyActive) {
                                        AegisSoundEffects.playModeOn(context)
                                        preferences.extendActiveStudySession(15)
                                        Toast.makeText(context, "Study session extended +15m", Toast.LENGTH_SHORT).show()
                                    } else if (!PermissionHelper.hasOverlayPermission(context)) {
                                        showOverlayRequiredDialog = true
                                    } else {
                                        AegisSoundEffects.playModeOn(context)
                                        preferences.startStudySession(studyDurationMinutes)
                                        Toast.makeText(
                                            context,
                                            "Study Mode active for ${studyDurationMinutes}m (${studySoftWarningSeconds}s soft warning)",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isStudyActive) AegisEmeraldTeal else AegisPurple,
                                    contentColor = AegisOnPrimaryWhite
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag(if (isStudyActive) "focus_study_extend_button" else "focus_study_toggle_button")
                            ) {
                                Icon(
                                    imageVector = if (isStudyActive) Icons.Default.Add else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isStudyActive) "EXTEND +15M" else "START STUDY",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                // ─────────────────────────────────────────────────────────────────────────────
                // 2. Smart Safe Search & Keyword Guard
                // ─────────────────────────────────────────────────────────────────────────────
                item {
                    LuxurySectionHeader(
                        title = "Smart Safe Search & Keyword Guard",
                        accentColor = AegisPrimaryCyan,
                        modifier = Modifier.widthIn(max = 600.dp)
                    )
                }
                item {
                    CyberCard(
                        borderColor = if (smartSafeSearchEnabled) AegisPrimaryCyan else AegisCardBorder,
                        tone = CyberCardTone.SKY,
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .testTag("focus_safe_search_card")
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
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(13.dp))
                                        .background(AegisPrimaryCyan.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = "Smart Safe Search",
                                        tint = AegisPrimaryCyan,
                                        modifier = Modifier.size(21.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Smart Safe Search Guard",
                                        color = AegisTextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = ClassicalDisplayFont
                                    )
                                    Text(
                                        text = if (smartSafeSearchEnabled) {
                                            "${activePacks.size} packs active • $safeSearchBlockedCount queries stopped"
                                        } else {
                                            "Off • Tap to filter risky searches & proxies"
                                        },
                                        color = if (smartSafeSearchEnabled) AegisPrimaryCyan else AegisTextSecondary,
                                        fontSize = 11.5.sp,
                                        fontFamily = ClassicalBodyFont
                                    )
                                }
                            }
                            Switch(
                                checked = smartSafeSearchEnabled,
                                onCheckedChange = { next ->
                                    if (!next) {
                                        Toast.makeText(
                                            context,
                                            "Safe Search active • Turn off in Edits",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        AegisSoundEffects.playModeOn(context)
                                        preferences.setSmartSafeSearchEnabled(true)
                                    }
                                },
                                modifier = Modifier.testTag("focus_smart_safe_search_switch"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AegisOnPrimaryWhite,
                                    checkedTrackColor = AegisPrimaryCyan
                                )
                            )
                        }

                        HorizontalDivider(
                            color = AegisCardBorder.copy(alpha = 0.45f),
                            modifier = Modifier.padding(vertical = 10.dp)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SafeSearchPack.entries.forEach { pack ->
                                val packEnabled = pack.id in activePacks
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 54.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (packEnabled) AegisPrimaryCyan.copy(alpha = 0.12f) else AegisCardBackground
                                        )
                                        .border(
                                            width = if (packEnabled) 1.3.dp else 1.dp,
                                            color = if (packEnabled) AegisPrimaryCyan else AegisCardBorder,
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable {
                                            if (packEnabled) {
                                                Toast.makeText(
                                                    context,
                                                    "${pack.title} active • Disable in Edits",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                AegisSoundEffects.playModeOn(context)
                                                preferences.toggleSafeSearchPack(pack.id)
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                        .testTag("safe_search_pack_${pack.id}"),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                        Text(
                                            text = pack.title,
                                            color = AegisTextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = ClassicalDisplayFont
                                        )
                                        Text(
                                            text = pack.subtitle,
                                            color = AegisTextSecondary,
                                            fontSize = 11.sp,
                                            fontFamily = ClassicalBodyFont,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = if (packEnabled) "${pack.title} Enabled" else "${pack.title} Disabled",
                                        tint = if (packEnabled) AegisPrimaryCyan else AegisCardBorder,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customKeywordInput,
                                onValueChange = { customKeywordInput = it },
                                placeholder = { Text("Block custom search keyword...", fontSize = 12.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AegisPrimaryCyan,
                                    unfocusedBorderColor = AegisCardBorder,
                                    focusedContainerColor = AegisCardBackground,
                                    unfocusedContainerColor = AegisCardBackground,
                                    focusedTextColor = AegisTextPrimary,
                                    unfocusedTextColor = AegisTextPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("focus_custom_keyword_input")
                            )
                            Button(
                                onClick = {
                                    val clean = customKeywordInput.trim()
                                    if (preferences.addCustomBlockedKeyword(clean)) {
                                        AegisSoundEffects.playModeOn(context)
                                        customKeywordInput = ""
                                        Toast.makeText(context, "Added '$clean'", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Enter at least 2 characters", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AegisPrimaryCyan,
                                    contentColor = AegisOnPrimaryWhite
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("focus_add_keyword_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Keyword", modifier = Modifier.size(18.dp))
                            }
                        }

                        AnimatedVisibility(visible = customKeywords.isNotEmpty()) {
                            Column {
                                Spacer(modifier = Modifier.height(10.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    customKeywords.sorted().forEach { kw ->
                                        Row(
                                            modifier = Modifier
                                                .heightIn(min = 32.dp)
                                                .clip(RoundedCornerShape(50))
                                                .background(AegisSurfaceElevated)
                                                .border(1.dp, AegisCardBorder, RoundedCornerShape(50))
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = kw,
                                                color = AegisTextPrimary,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ─────────────────────────────────────────────────────────────────────────────
                // 3. Daily App Time Budgets
                // ─────────────────────────────────────────────────────────────────────────────
                item {
                    LuxurySectionHeader(
                        title = "Daily App Time Budgets",
                        accentColor = AegisEmeraldTeal,
                        modifier = Modifier.widthIn(max = 600.dp)
                    )
                }
                item {
                    CyberCard(
                        borderColor = AegisEmeraldBorder,
                        tone = CyberCardTone.EMERALD,
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .testTag("focus_app_budgets_card")
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
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(AegisEmeraldTeal.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = "Daily App Limits",
                                        tint = AegisEmeraldTeal,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Per-App Daily Caps",
                                        color = AegisTextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = ClassicalDisplayFont
                                    )
                                    Text(
                                        text = if (appTimeBudgets.isEmpty()) {
                                            "No daily limits set yet"
                                        } else {
                                            "${appTimeBudgets.size} app limit(s) actively enforced"
                                        },
                                        color = AegisTextSecondary,
                                        fontSize = 11.5.sp,
                                        fontFamily = ClassicalBodyFont
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    AegisSoundEffects.playTap(context)
                                    showBudgetPickerDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AegisEmeraldTeal,
                                    contentColor = AegisOnPrimaryWhite
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("focus_add_budget_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("SET LIMIT", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (appTimeBudgets.isEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(AegisCardBackground)
                                    .border(1.dp, AegisEmeraldBorder, RoundedCornerShape(14.dp))
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HourglassTop,
                                    contentDescription = null,
                                    tint = AegisEmeraldTeal,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = "Tap SET LIMIT to pick YouTube, TikTok, games, or any installed app and give it a healthy daily minute cap.",
                                    color = AegisTextSecondary,
                                    fontSize = 12.sp,
                                    fontFamily = ClassicalBodyFont
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                appTimeBudgets.entries.sortedBy { it.key }.forEach { (pkg, limitMins) ->
                                    val label = remember(pkg, installedApps) {
                                        installedApps.firstOrNull { it.packageName == pkg }?.label
                                            ?: SafeSearchPack.POPULAR_BUDGET_APP_PRESETS.firstOrNull { it.first == pkg }?.second
                                            ?: runCatching {
                                                val ai = context.packageManager.getApplicationInfo(pkg, 0)
                                                context.packageManager.getApplicationLabel(ai).toString()
                                            }.getOrDefault(pkg)
                                    }
                                    val usedSec = dailyAppUsageSeconds[pkg] ?: 0
                                    val usedMins = usedSec / 60
                                    val progress = (usedSec.toFloat() / (limitMins * 60f).coerceAtLeast(1f)).coerceIn(0f, 1f)

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(AegisCardBackground)
                                            .border(1.dp, AegisEmeraldBorder, RoundedCornerShape(14.dp))
                                            .padding(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.fillMaxWidth()) {
                                                Text(
                                                    text = label,
                                                    color = AegisTextPrimary,
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = ClassicalDisplayFont,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${usedMins}m used today / ${limitMins}m limit • Manage in Edits",
                                                    color = if (usedMins >= limitMins) AegisWarningAmber else AegisEmeraldTeal,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontFamily = ClassicalBodyFont
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        LinearProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = if (progress >= 0.95f) AegisWarningAmber else AegisEmeraldTeal,
                                            trackColor = AegisEmeraldTeal.copy(alpha = 0.16f),
                                            strokeCap = StrokeCap.Round
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showOverlayRequiredDialog) {
        AlertDialog(
            onDismissRequest = { showOverlayRequiredDialog = false },
            containerColor = AegisLavenderSoft,
            title = {
                Text("Display Over Apps Required", fontFamily = ClassicalDisplayFont, fontWeight = FontWeight.Bold, color = AegisTextPrimary)
            },
            text = {
                Text(
                    "Study Mode needs 'Display over other apps' permission to show the soft warning countdown and focus lock screen.",
                    color = AegisTextSecondary,
                    fontFamily = ClassicalBodyFont
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showOverlayRequiredDialog = false
                        preferences.grantTemporarySettingsAuthorization()
                        context.startActivity(PermissionHelper.getOverlaySettingsIntent(context))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AegisPurple, contentColor = AegisOnPrimaryWhite)
                ) {
                    Text("GRANT PERMISSION", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showOverlayRequiredDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }

    if (showStudyAppsDialog) {
        var studySearch by remember { mutableStateOf("") }
        val filteredStudyApps = remember(installedApps, studySearch, studyAllowedPackages) {
            val q = studySearch.trim().lowercase(Locale.US)
            val list = if (q.isEmpty()) installedApps else installedApps.filter {
                it.label.lowercase(Locale.US).contains(q) || it.packageName.lowercase(Locale.US).contains(q)
            }
            list.sortedByDescending { studyAllowedPackages.contains(it.packageName) }
        }

        AlertDialog(
            onDismissRequest = { showStudyAppsDialog = false },
            containerColor = AegisLavenderSoft,
            modifier = Modifier.testTag("study_apps_picker_dialog"),
            title = {
                Text(
                    text = "Allowed During Study Mode",
                    fontFamily = ClassicalDisplayFont,
                    fontWeight = FontWeight.Bold,
                    color = AegisTextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = studySearch,
                        onValueChange = { studySearch = it },
                        placeholder = { Text("Search apps to allow in Study Mode...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AegisPurple) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isLoadingApps) {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AegisPurple)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().height(290.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredStudyApps, key = { it.packageName }) { app ->
                                val allowed = app.packageName in studyAllowedPackages
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 52.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (allowed) AegisPurple.copy(alpha = 0.14f) else AegisCardBackground)
                                        .border(
                                            width = if (allowed) 1.3.dp else 1.dp,
                                            color = if (allowed) AegisPurple else AegisLavenderBorder,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            if (isStudyActive && !allowed) {
                                                Toast.makeText(
                                                    context,
                                                    "Add study apps in Edits during active session",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                preferences.toggleStudyAllowedPackage(app.packageName)
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
                                                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(AegisPurple.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(app.label.take(1).uppercase(), fontWeight = FontWeight.Bold, color = AegisPurple)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = app.label,
                                                color = AegisTextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
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
                                        contentDescription = if (allowed) "Allowed" else "Blocked during study",
                                        tint = if (allowed) AegisPurple else AegisCardBorder,
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
                    onClick = { showStudyAppsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = AegisPurple, contentColor = AegisOnPrimaryWhite)
                ) {
                    Text("DONE", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showBudgetPickerDialog) {
        var budgetSearch by remember { mutableStateOf("") }
        var selectedPkg by remember { mutableStateOf<String?>(null) }
        var selectedLabel by remember { mutableStateOf("") }
        var selectedMinutes by remember { mutableIntStateOf(30) }

        val mergedEntries = remember(installedApps, budgetSearch) {
            val existingPkgs = installedApps.map { it.packageName }.toSet()
            val presetExtras = SafeSearchPack.POPULAR_BUDGET_APP_PRESETS
                .filter { it.first !in existingPkgs }
                .map { InstalledAppEntry(packageName = it.first, label = it.second, icon = null) }
            val all = installedApps + presetExtras
            val q = budgetSearch.trim().lowercase(Locale.US)
            if (q.isEmpty()) all else all.filter {
                it.label.lowercase(Locale.US).contains(q) || it.packageName.lowercase(Locale.US).contains(q)
            }
        }

        AlertDialog(
            onDismissRequest = { showBudgetPickerDialog = false },
            containerColor = AegisChampagneSoft,
            modifier = Modifier.testTag("budget_app_picker_dialog"),
            title = {
                Text(
                    text = "Set Daily App Time Limit",
                    fontFamily = ClassicalDisplayFont,
                    fontWeight = FontWeight.Bold,
                    color = AegisTextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = budgetSearch,
                        onValueChange = { budgetSearch = it },
                        placeholder = { Text("Search app (e.g. YouTube, TikTok)...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AegisEmeraldTeal) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Daily Limit",
                        color = AegisTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(15, 30, 60, 120).forEach { mins ->
                            val isSelected = selectedMinutes == mins
                            OutlinedButton(
                                onClick = { selectedMinutes = mins },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) AegisEmeraldTeal else AegisCardBackground,
                                    contentColor = if (isSelected) AegisOnPrimaryWhite else AegisTextPrimary
                                ),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text("${mins}m", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (isLoadingApps) {
                        Box(modifier = Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AegisEmeraldTeal)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().height(230.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(mergedEntries, key = { it.packageName }) { app ->
                                val picked = selectedPkg == app.packageName
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 50.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (picked) AegisEmeraldTeal.copy(alpha = 0.16f) else AegisCardBackground)
                                        .border(
                                            width = if (picked) 1.5.dp else 1.dp,
                                            color = if (picked) AegisEmeraldTeal else AegisGoldBorder,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            selectedPkg = app.packageName
                                            selectedLabel = app.label
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
                                                modifier = Modifier.size(30.dp).clip(RoundedCornerShape(8.dp))
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .clip(CircleShape)
                                                    .background(AegisEmeraldTeal.copy(alpha = 0.16f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(app.label.take(1).uppercase(), fontWeight = FontWeight.Bold, color = AegisEmeraldTeal)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = app.label,
                                                color = AegisTextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
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
                                        contentDescription = if (picked) "Selected" else "Select",
                                        tint = if (picked) AegisEmeraldTeal else AegisCardBorder,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = selectedPkg != null,
                    onClick = {
                        val target = selectedPkg ?: return@Button
                        val currentLimit = appTimeBudgets[target]
                        if (currentLimit != null && selectedMinutes > currentLimit) {
                            Toast.makeText(
                                context,
                                "Limit active (${currentLimit}m) • Raise in Edits",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            preferences.setAppTimeBudget(target, selectedMinutes)
                            showBudgetPickerDialog = false
                            Toast.makeText(context, "$selectedLabel capped at ${selectedMinutes}m/day", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AegisEmeraldTeal, contentColor = AegisOnPrimaryWhite)
                ) {
                    Text("SAVE LIMIT", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showBudgetPickerDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }
}

private fun formatStudyClock(millis: Long): String {
    val totalSec = (millis / 1000L).coerceAtLeast(0L)
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}
