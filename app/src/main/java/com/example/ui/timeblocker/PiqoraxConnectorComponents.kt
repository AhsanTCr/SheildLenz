package com.example.ui.timeblocker

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timeblocker.TimeBlockDecision
import com.example.timeblocker.TimeBlockerEngine
import com.example.timeblocker.data.TimeBlockedAppEntity
import com.example.ui.settings.InstalledAppEntry
import com.example.ui.theme.AegisCardBackground
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisChampagneSoft
import com.example.ui.theme.AegisEmeraldBorder
import com.example.ui.theme.AegisEmeraldSoft
import com.example.ui.theme.AegisEmeraldTeal
import com.example.ui.theme.AegisGoldBorder
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

@Composable
internal fun UnitToggleChip(
    label: String,
    selected: Boolean,
    accentColor: Color,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) accentColor else AegisCardBackground,
        border = BorderStroke(1.dp, if (selected) accentColor else AegisCardBorder),
        modifier = Modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                text = label,
                color = if (selected) AegisOnPrimaryWhite else AegisTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ClassicalBodyFont
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PresetChipRow(
    presets: List<Pair<Int, String>>,
    selectedMinutes: Int,
    accentColor: Color,
    tagPrefix: String,
    onSelect: (Int) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        presets.forEach { (mins, label) ->
            val isSelected = selectedMinutes == mins
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) accentColor.copy(alpha = 0.16f) else AegisCardBackground,
                border = BorderStroke(
                    width = if (isSelected) 1.4.dp else 1.dp,
                    color = if (isSelected) accentColor else AegisCardBorder
                ),
                modifier = Modifier
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelect(mins) }
                    .testTag("${tagPrefix}_$mins")
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) accentColor else AegisTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontFamily = ClassicalBodyFont
                    )
                }
            }
        }
    }
}

@Composable
internal fun TimeBlockedAppRowCard(
    entity: TimeBlockedAppEntity,
    appLabel: String,
    appIcon: ImageBitmap?,
    decision: TimeBlockDecision,
    onResetWindow: () -> Unit,
    onRemoveApp: () -> Unit
) {
    val allowanceMs = TimeBlockerEngine.effectiveAllowanceMs(entity)
    val windowMs = TimeBlockerEngine.effectiveWindowMs(entity)
    val allowanceMins = (allowanceMs / 60_000L).toInt().coerceAtLeast(1)
    val windowMins = (windowMs / 60_000L).toInt().coerceAtLeast(1)
    val progress = (decision.remainingAllowanceMs.toFloat() / allowanceMs.toFloat()).coerceIn(0f, 1f)

    val statusColor = when {
        decision.hasActivePass -> AegisEmeraldTeal
        decision.shouldBlock -> AegisWarningAmber
        else -> AegisPrimaryCyan
    }
    val statusText = when {
        decision.hasActivePass ->
            "PiqoraX Pass active • ${TimeBlockerEngine.formatCountdown(decision.passRemainingMs)} left"
        decision.shouldBlock ->
            "Locked • Resets in ${TimeBlockerEngine.formatCountdown(decision.remainingResetMs)}"
        else ->
            "${TimeBlockerEngine.formatCountdown(decision.remainingAllowanceMs)} left of ${TimeBlockerEngine.formatMinutesCompact(allowanceMins)} / ${TimeBlockerEngine.formatMinutesCompact(windowMins)}"
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = AegisCardBackground,
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("time_blocked_app_row_${entity.packageName}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (appIcon != null) {
                    Image(
                        bitmap = appIcon,
                        contentDescription = appLabel,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AegisLavenderSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = appLabel,
                            tint = AegisPurple,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appLabel,
                        color = AegisTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalDisplayFont,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = ClassicalBodyFont,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AegisChampagneSoft,
                    border = BorderStroke(1.dp, AegisGoldBorder)
                ) {
                    Text(
                        text = "${TimeBlockerEngine.formatMinutesCompact(allowanceMins)} / ${TimeBlockerEngine.formatMinutesCompact(windowMins)}",
                        color = AegisChampagneGold,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalBodyFont,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { if (decision.hasActivePass) 1f else progress },
                color = statusColor,
                trackColor = statusColor.copy(alpha = 0.16f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onResetWindow,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, AegisCardBorder),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier
                        .height(40.dp)
                        .testTag("reset_timer_${entity.packageName}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset timer for $appLabel",
                        tint = AegisPrimaryCyan,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Reset",
                        color = AegisPrimaryCyan,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onRemoveApp,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, AegisWarningAmber.copy(alpha = 0.45f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier
                        .height(40.dp)
                        .testTag("remove_time_block_${entity.packageName}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Stop blocking $appLabel",
                        tint = AegisWarningAmber,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Stop",
                        color = AegisWarningAmber,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
internal fun TimeBlockerAppPickerDialog(
    installedApps: List<InstalledAppEntry>,
    initiallySelectedPackages: Set<String>,
    allowanceMinutes: Int,
    windowMinutes: Int,
    onDismiss: () -> Unit,
    onConfirmSelection: (Set<String>) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedPackages by remember(initiallySelectedPackages) {
        mutableStateOf(initiallySelectedPackages)
    }

    val filtered = remember(installedApps, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isBlank()) installedApps
        else installedApps.filter {
            it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AegisCardBackground,
        title = {
            Column {
                Text(
                    text = "Select Apps to Block",
                    color = AegisTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalDisplayFont
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Rule: ${TimeBlockerEngine.formatMinutesCompact(allowanceMinutes)} allowance per ${TimeBlockerEngine.formatMinutesCompact(windowMinutes)}",
                    color = AegisPurple,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = ClassicalBodyFont
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search installed apps...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search apps",
                            tint = AegisPrimaryCyan
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AegisPrimaryCyan,
                        unfocusedBorderColor = AegisCardBorder,
                        focusedTextColor = AegisTextPrimary,
                        unfocusedTextColor = AegisTextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("time_blocker_picker_search")
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (installedApps.isEmpty()) "Loading installed apps..." else "No matching apps found",
                            color = AegisTextSecondary,
                            fontSize = 13.sp,
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
                        items(filtered, key = { it.packageName }) { app ->
                            val isChecked = app.packageName in selectedPackages
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isChecked) AegisEmeraldSoft else Color.Transparent)
                                    .border(
                                        1.dp,
                                        if (isChecked) AegisEmeraldBorder else AegisCardBorder.copy(alpha = 0.55f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        selectedPackages = if (isChecked) {
                                            selectedPackages - app.packageName
                                        } else {
                                            selectedPackages + app.packageName
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                    .testTag("time_blocker_picker_app_${app.packageName}")
                            ) {
                                if (app.icon != null) {
                                    Image(
                                        bitmap = app.icon,
                                        contentDescription = app.label,
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(AegisLavenderSoft),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.HourglassTop,
                                            contentDescription = null,
                                            tint = AegisPurple,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.label,
                                        color = AegisTextPrimary,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = ClassicalBodyFont,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = app.packageName,
                                        color = AegisTextMuted,
                                        fontSize = 10.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        selectedPackages = if (checked) {
                                            selectedPackages + app.packageName
                                        } else {
                                            selectedPackages - app.packageName
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = AegisEmeraldTeal,
                                        uncheckedColor = AegisTextMuted
                                    )
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmSelection(selectedPackages) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AegisPrimaryCyan,
                    contentColor = AegisOnPrimaryWhite
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("time_blocker_picker_confirm")
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Start Blocking (${selectedPackages.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Cancel", color = AegisTextSecondary, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}
