package com.example.ui.mindful

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.models.ShortVideoScrollDetector
import com.example.data.preferences.AegisPreferences
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberCardTone
import com.example.ui.components.StatusPill
import com.example.ui.settings.InstalledAppEntry
import com.example.ui.theme.AegisCardBackground
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisChampagneSoft
import com.example.ui.theme.AegisElectricBlue
import com.example.ui.theme.AegisGoldBorder
import com.example.ui.theme.AegisIndigo
import com.example.ui.theme.AegisLavenderBorder
import com.example.ui.theme.AegisLavenderSoft
import com.example.ui.theme.AegisOnPrimaryWhite
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisPurple
import com.example.ui.theme.AegisTextMuted
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.AegisWarningAmber
import com.example.ui.theme.ClassicalBodyFont
import com.example.ui.theme.ClassicalDisplayFont
import kotlin.math.roundToInt

private val CURFEW_WINDOW_PRESETS = listOf(
    Triple(21 * 60, 6 * 60, "9 PM – 6 AM"),
    Triple(22 * 60, 7 * 60, "10 PM – 7 AM"),
    Triple(23 * 60, 7 * 60, "11 PM – 7 AM")
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GamingAddictionControlCard(
    gameGuardEnabled: Boolean,
    gameAutoDetectEnabled: Boolean,
    trackedGamePackages: Set<String>,
    sessionLimitMinutes: Int,
    cooldownMinutes: Int,
    dailyCapMinutes: Int,
    dailyExtensionMinutes: Int,
    curfewEnabled: Boolean,
    curfewStartMinutes: Int,
    curfewEndMinutes: Int,
    isWithinCurfewNow: Boolean,
    cooldownRemainingMillis: Long,
    dailyGameTotalSeconds: Int,
    activeSessionSeconds: Int,
    dailyGameUsageByPackage: Map<String, Int>,
    installedApps: List<InstalledAppEntry>,
    onToggleGameGuard: (Boolean) -> Unit,
    onToggleAutoDetect: (Boolean) -> Unit,
    onChangeSessionLimit: (Int) -> Unit,
    onChangeCooldownMinutes: (Int) -> Unit,
    onChangeDailyCapMinutes: (Int) -> Unit,
    onUpdateCurfew: (Boolean, Int, Int) -> Unit,
    onUnlockGameLockEarly: () -> Unit,
    onOpenGamePicker: () -> Unit
) {
    var sessionDraft by remember(sessionLimitMinutes) { mutableStateOf(sessionLimitMinutes.toFloat()) }
    var cooldownDraft by remember(cooldownMinutes) { mutableStateOf(cooldownMinutes.toFloat()) }
    var dailyCapDraft by remember(dailyCapMinutes) { mutableStateOf(dailyCapMinutes.toFloat()) }

    val effectiveDailyCapMinutes = if (dailyCapMinutes > 0) dailyCapMinutes + dailyExtensionMinutes else 0
    val isDailyCapReached = effectiveDailyCapMinutes > 0 && dailyGameTotalSeconds >= effectiveDailyCapMinutes * 60
    val hasActiveGameLock = gameGuardEnabled && (cooldownRemainingMillis > 0L || isWithinCurfewNow || isDailyCapReached)

    val labelByPkg = remember(installedApps) {
        installedApps.associate { it.packageName to it.label }
    }
    val topPlayedGames = remember(dailyGameUsageByPackage, labelByPkg) {
        dailyGameUsageByPackage.entries
            .filter { it.value > 0 }
            .sortedByDescending { it.value }
            .take(4)
            .map { (pkg, secs) ->
                val label = labelByPkg[pkg] ?: pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() }
                label to secs
            }
    }

    CyberCard(
        borderColor = if (gameGuardEnabled) AegisElectricBlue.copy(alpha = 0.58f) else AegisCardBorder,
        tone = CyberCardTone.SKY,
        modifier = Modifier.testTag("gaming_addiction_control_card")
    ) {
        // Header row + master toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AegisElectricBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsEsports,
                        contentDescription = null,
                        tint = AegisOnPrimaryWhite,
                        modifier = Modifier.size(21.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Gaming Addiction Control",
                        color = AegisTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalDisplayFont
                    )
                    Text(
                        text = if (gameGuardEnabled) {
                            "${sessionLimitMinutes}m play / ${cooldownMinutes}m break • " +
                                if (dailyCapMinutes == 0) "No daily cap" else "${dailyCapMinutes}m daily cap"
                        } else {
                            "Game limits paused • playtime counter still active"
                        },
                        color = AegisTextSecondary,
                        fontSize = 11.5.sp,
                        fontFamily = ClassicalBodyFont
                    )
                }
            }
            Switch(
                checked = gameGuardEnabled,
                onCheckedChange = onToggleGameGuard,
                modifier = Modifier.testTag("gaming_guard_master_switch"),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AegisOnPrimaryWhite,
                    checkedTrackColor = AegisElectricBlue,
                    uncheckedThumbColor = AegisTextMuted,
                    uncheckedTrackColor = AegisCardBorder
                )
            )
        }

        // Active Lock Banner (Cooldown / Daily Cap / Curfew)
        if (hasActiveGameLock) {
            Spacer(modifier = Modifier.height(10.dp))
            val lockTitle = when {
                isWithinCurfewNow -> "Late-Night Gaming Curfew Active"
                isDailyCapReached -> "Daily Gaming Cap Reached (${effectiveDailyCapMinutes}m)"
                else -> {
                    val remMin = ((cooldownRemainingMillis + 59_999L) / 60_000L).coerceAtLeast(1L)
                    "Break Cooldown Active (${remMin}m left)"
                }
            }
            val lockDetail = when {
                isWithinCurfewNow -> "${AegisPreferences.formatClockTime(curfewStartMinutes)} – ${AegisPreferences.formatClockTime(curfewEndMinutes)}"
                isDailyCapReached -> "Tracked games paused until tomorrow"
                else -> "Games locked to prevent continuous play"
            }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = AegisChampagneSoft,
                border = BorderStroke(1.dp, AegisGoldBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gaming_active_lock_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = lockTitle,
                            color = AegisTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = ClassicalBodyFont
                        )
                        Text(
                            text = lockDetail,
                            color = AegisWarningAmber,
                            fontSize = 11.sp,
                            fontFamily = ClassicalBodyFont
                        )
                    }
                    Button(
                        onClick = onUnlockGameLockEarly,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AegisChampagneGold,
                            contentColor = AegisOnPrimaryWhite
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("unlock_game_lock_button")
                    ) {
                        Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("UNLOCK", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Live 2-column Gaming Telemetry (pure utility, zero gamification)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GamingStatTile(
                label = "Today's Gaming",
                value = ShortVideoScrollDetector.formatMinutesCompact(dailyGameTotalSeconds),
                subValue = if (dailyCapMinutes == 0) "Daily cap: Off" else "Cap: ${effectiveDailyCapMinutes}m",
                accent = AegisElectricBlue,
                modifier = Modifier.weight(1f)
            )
            GamingStatTile(
                label = "Current Session",
                value = ShortVideoScrollDetector.formatMinutesCompact(activeSessionSeconds),
                subValue = "Break after ${sessionLimitMinutes}m",
                accent = AegisIndigo,
                modifier = Modifier.weight(1f)
            )
        }

        HorizontalDivider(
            color = AegisCardBorder.copy(alpha = 0.55f),
            modifier = Modifier.padding(vertical = 12.dp)
        )

        // 1. Continuous Play Session Limit
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Continuous Session Limit",
                color = AegisTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ClassicalDisplayFont
            )
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = AegisElectricBlue.copy(alpha = 0.14f)
            ) {
                Text(
                    text = "${sessionDraft.roundToInt()} min",
                    color = AegisElectricBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        Slider(
            value = sessionDraft,
            onValueChange = { sessionDraft = it },
            onValueChangeFinished = {
                val chosen = sessionDraft.roundToInt().coerceIn(10, 90)
                if (chosen != sessionLimitMinutes) {
                    onChangeSessionLimit(chosen)
                }
            },
            valueRange = 10f..90f,
            steps = 7,
            colors = SliderDefaults.colors(
                thumbColor = AegisElectricBlue,
                activeTrackColor = AegisElectricBlue,
                inactiveTrackColor = AegisCardBorder.copy(alpha = 0.5f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("game_session_limit_slider")
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(15, 30, 45, 60).forEach { preset ->
                FilterChip(
                    selected = sessionLimitMinutes == preset,
                    onClick = {
                        sessionDraft = preset.toFloat()
                        onChangeSessionLimit(preset)
                    },
                    label = { Text("${preset}m", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AegisElectricBlue,
                        selectedLabelColor = AegisOnPrimaryWhite
                    ),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("game_session_preset_$preset")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Mandatory Break Cooldown
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Mandatory Break Cooldown",
                color = AegisTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ClassicalDisplayFont
            )
            Text(
                text = "${cooldownDraft.roundToInt()} min rest",
                color = AegisPurple,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        Slider(
            value = cooldownDraft,
            onValueChange = { cooldownDraft = it },
            onValueChangeFinished = {
                val chosen = cooldownDraft.roundToInt().coerceIn(5, 60)
                if (chosen != cooldownMinutes) {
                    onChangeCooldownMinutes(chosen)
                }
            },
            valueRange = 5f..60f,
            steps = 10,
            colors = SliderDefaults.colors(
                thumbColor = AegisPurple,
                activeTrackColor = AegisPurple,
                inactiveTrackColor = AegisLavenderBorder.copy(alpha = 0.45f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("game_cooldown_slider")
        )

        Spacer(modifier = Modifier.height(6.dp))

        // 3. Daily Gaming Cap
        val draftDailyCap = dailyCapDraft.roundToInt().let { if (it < 15) 0 else it.coerceIn(15, 240) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Daily Total Gaming Cap",
                color = AegisTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ClassicalDisplayFont
            )
            Text(
                text = if (draftDailyCap == 0) "Off" else AegisPreferences.formatMinutesLabel(draftDailyCap),
                color = AegisPrimaryCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        Slider(
            value = dailyCapDraft,
            onValueChange = { dailyCapDraft = it },
            onValueChangeFinished = {
                val raw = dailyCapDraft.roundToInt()
                val chosen = if (raw < 15) 0 else raw.coerceIn(15, 240)
                dailyCapDraft = chosen.toFloat()
                if (chosen != dailyCapMinutes) {
                    onChangeDailyCapMinutes(chosen)
                }
            },
            valueRange = 0f..240f,
            steps = 15,
            colors = SliderDefaults.colors(
                thumbColor = AegisPrimaryCyan,
                activeTrackColor = AegisPrimaryCyan,
                inactiveTrackColor = AegisCardBorder.copy(alpha = 0.5f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("game_daily_cap_slider")
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(0 to "Off", 60 to "1h", 90 to "1h 30m", 120 to "2h").forEach { (mins, label) ->
                FilterChip(
                    selected = dailyCapMinutes == mins,
                    onClick = {
                        dailyCapDraft = mins.toFloat()
                        onChangeDailyCapMinutes(mins)
                    },
                    label = { Text(label, fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AegisPrimaryCyan,
                        selectedLabelColor = AegisOnPrimaryWhite
                    ),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("game_daily_cap_preset_$mins")
                )
            }
        }

        HorizontalDivider(
            color = AegisCardBorder.copy(alpha = 0.55f),
            modifier = Modifier.padding(vertical = 12.dp)
        )

        // 4. Late-Night Gaming Curfew
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Default.Bedtime,
                    contentDescription = null,
                    tint = AegisIndigo,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Late-Night Gaming Curfew",
                        color = AegisTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalDisplayFont
                    )
                    Text(
                        text = if (curfewEnabled) {
                            "${AegisPreferences.formatClockTime(curfewStartMinutes)} – ${AegisPreferences.formatClockTime(curfewEndMinutes)}"
                        } else {
                            "Night block off"
                        },
                        color = AegisTextSecondary,
                        fontSize = 11.sp,
                        fontFamily = ClassicalBodyFont
                    )
                }
            }
            Switch(
                checked = curfewEnabled,
                onCheckedChange = { next ->
                    onUpdateCurfew(next, curfewStartMinutes, curfewEndMinutes)
                },
                modifier = Modifier.testTag("game_curfew_switch"),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AegisOnPrimaryWhite,
                    checkedTrackColor = AegisIndigo
                )
            )
        }
        if (curfewEnabled) {
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CURFEW_WINDOW_PRESETS.forEach { (startMin, endMin, label) ->
                    val selected = curfewStartMinutes == startMin && curfewEndMinutes == endMin
                    FilterChip(
                        selected = selected,
                        onClick = { onUpdateCurfew(true, startMin, endMin) },
                        label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AegisIndigo,
                            selectedLabelColor = AegisOnPrimaryWhite
                        ),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("game_curfew_preset_${startMin}_$endMin")
                    )
                }
            }
        }

        HorizontalDivider(
            color = AegisCardBorder.copy(alpha = 0.55f),
            modifier = Modifier.padding(vertical = 12.dp)
        )

        // 5. Auto-detect toggle & Tracked Games Manager
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = "Auto-Detect Installed Games",
                    color = AegisTextPrimary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalDisplayFont
                )
                Text(
                    text = "${trackedGamePackages.size} game(s) monitored automatically",
                    color = AegisTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = ClassicalBodyFont
                )
            }
            Switch(
                checked = gameAutoDetectEnabled,
                onCheckedChange = onToggleAutoDetect,
                modifier = Modifier.testTag("game_autodetect_switch"),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AegisOnPrimaryWhite,
                    checkedTrackColor = AegisElectricBlue
                )
            )
        }

        if (topPlayedGames.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                topPlayedGames.forEach { (label, secs) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.72f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            color = AegisTextPrimary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = ClassicalBodyFont,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                        Text(
                            text = ShortVideoScrollDetector.formatMinutesCompact(secs),
                            color = AegisElectricBlue,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        OutlinedButton(
            onClick = onOpenGamePicker,
            border = BorderStroke(1.2.dp, AegisElectricBlue),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AegisElectricBlue),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("configure_tracked_games_button")
        ) {
            Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "MANAGE TRACKED GAMES (${trackedGamePackages.size})",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun GamingStatTile(
    label: String,
    value: String,
    subValue: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.82f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = label,
                color = AegisTextSecondary,
                fontSize = 10.5.sp,
                fontFamily = ClassicalBodyFont
            )
            Text(
                text = value,
                color = accent,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ClassicalDisplayFont
            )
            Text(
                text = subValue,
                color = AegisTextMuted,
                fontSize = 10.sp,
                fontFamily = ClassicalBodyFont
            )
        }
    }
}

@Composable
internal fun TrackedGamesPickerDialog(
    installedApps: List<InstalledAppEntry>,
    detectedGamePackages: Set<String>,
    selectedGamePackages: Set<String>,
    onTogglePackage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    val filtered = remember(installedApps, search) {
        val q = search.trim().lowercase()
        if (q.isBlank()) installedApps
        else installedApps.filter { it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q) }
    }
    val detectedSection = remember(filtered, detectedGamePackages) {
        filtered.filter { it.packageName in detectedGamePackages }
    }
    val otherSection = remember(filtered, detectedGamePackages) {
        filtered.filter { it.packageName !in detectedGamePackages }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AegisCardBackground,
        titleContentColor = AegisTextPrimary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SportsEsports,
                    contentDescription = null,
                    tint = AegisElectricBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tracked Games & Apps",
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalDisplayFont
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tracked_games_picker_dialog")
            ) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("Search games or apps") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (search.isNotEmpty()) {
                            IconButton(onClick = { search = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AegisElectricBlue,
                        unfocusedBorderColor = AegisCardBorder,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tracked_games_search_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 330.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (detectedSection.isNotEmpty()) {
                        item(key = "header_detected_games") {
                            Text(
                                text = "DETECTED GAMES (${detectedSection.size})",
                                color = AegisElectricBlue,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = ClassicalBodyFont,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(detectedSection, key = { "game_${it.packageName}" }) { app ->
                            GamePickerRow(
                                app = app,
                                checked = app.packageName in selectedGamePackages,
                                isAutoDetected = true,
                                onToggle = { onTogglePackage(app.packageName) }
                            )
                        }
                    }

                    if (otherSection.isNotEmpty()) {
                        item(key = "header_other_apps") {
                            Text(
                                text = "OTHER INSTALLED APPS (${otherSection.size})",
                                color = AegisTextSecondary,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = ClassicalBodyFont,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                        items(otherSection, key = { "other_${it.packageName}" }) { app ->
                            GamePickerRow(
                                app = app,
                                checked = app.packageName in selectedGamePackages,
                                isAutoDetected = false,
                                onToggle = { onTogglePackage(app.packageName) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AegisElectricBlue,
                    contentColor = AegisOnPrimaryWhite
                ),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("DONE", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun GamePickerRow(
    app: InstalledAppEntry,
    checked: Boolean,
    isAutoDetected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (checked) AegisLavenderSoft else Color.White.copy(alpha = 0.78f))
            .clickable(onClick = onToggle)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("game_picker_row_${app.packageName}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (app.icon != null) {
            Image(
                bitmap = app.icon,
                contentDescription = app.label,
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(7.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(AegisElectricBlue.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = app.label.take(1).uppercase(),
                    color = AegisElectricBlue,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = app.label,
                    color = AegisTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isAutoDetected) {
                    Spacer(modifier = Modifier.width(6.dp))
                    StatusPill(
                        label = "GAME",
                        isActive = true,
                        activeColor = AegisElectricBlue,
                        inactiveColor = AegisTextMuted
                    )
                }
            }
            Text(
                text = app.packageName,
                color = AegisTextMuted,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(checkedColor = AegisElectricBlue)
        )
    }
}
