package com.example.ui.mindful

import android.content.Intent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.models.DailyScrollSnapshot
import com.example.core.models.ShortVideoPlatform
import com.example.core.models.ShortVideoScrollDetector
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberCardTone
import com.example.ui.components.StatusPill
import com.example.ui.settings.InstalledAppEntry
import com.example.ui.theme.AegisCardBackground
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisChampagneSoft
import com.example.ui.theme.AegisElectricBlue
import com.example.ui.theme.AegisEmeraldBorder
import com.example.ui.theme.AegisEmeraldSoft
import com.example.ui.theme.AegisEmeraldTeal
import com.example.ui.theme.AegisGoldBorder
import com.example.ui.theme.AegisGoldBright
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HeroUsageAndReelsCard(
    dailyReelsCount: Int,
    scrollLimit: Int,
    dailyReelsByPlatform: Map<String, Int>,
    totalUsageSecondsToday: Int,
    dailyGameSecondsToday: Int = 0,
    cleanStreakDays: Int,
    mindfulExitsToday: Int,
    isShieldActive: Boolean,
    hasAccessibility: Boolean,
    onEnableAccessibility: () -> Unit,
    onResetTodayReels: () -> Unit
) {
    val progress = (dailyReelsCount.toFloat() / scrollLimit.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 550),
        label = "HeroReelsRing"
    )
    val ringAccent = when {
        progress >= 1f -> AegisWarningAmber
        progress >= 0.7f -> AegisPurple
        else -> AegisEmeraldTeal
    }

    CyberCard(
        borderColor = AegisLavenderBorder,
        tone = CyberCardTone.HERO,
        modifier = Modifier.testTag("usage_scroll_hero_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusPill(
                label = when {
                    !hasAccessibility -> "ENABLE ACCESSIBILITY"
                    isShieldActive -> "SHIELD ON • TRACKING"
                    else -> "SHIELD OFF • STILL ACTIVE"
                },
                isActive = hasAccessibility,
                activeColor = AegisEmeraldTeal,
                inactiveColor = AegisWarningAmber
            )
            if (dailyReelsCount > 0) {
                TextButton(
                    onClick = onResetTodayReels,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("reset_today_reels_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset today's reel counter",
                        tint = AegisPurple,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Reset",
                        color = AegisPurple,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalBodyFont
                    )
                }
            }
        }

        if (!hasAccessibility) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = AegisChampagneSoft,
                border = BorderStroke(1.dp, AegisGoldBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Enable Accessibility Guard so usage, 5s pause & reel locks run even when Shield is OFF.",
                        color = AegisTextPrimary,
                        fontSize = 11.5.sp,
                        fontFamily = ClassicalBodyFont,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )
                    Button(
                        onClick = onEnableAccessibility,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AegisChampagneGold,
                            contentColor = AegisOnPrimaryWhite
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("hero_enable_accessibility_button")
                    ) {
                        Text("ALLOW", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(118.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokePx = 11.dp.toPx()
                    val pad = strokePx / 2f
                    val arcSize = Size(size.width - strokePx, size.height - strokePx)
                    drawArc(
                        color = AegisCardBorder.copy(alpha = 0.38f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(pad, pad),
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(AegisEmeraldTeal, AegisPrimaryCyan, AegisPurple, ringAccent)
                        ),
                        startAngle = -90f,
                        sweepAngle = (360f * animatedProgress).coerceAtLeast(8f),
                        useCenter = false,
                        topLeft = Offset(pad, pad),
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$dailyReelsCount",
                        color = AegisTextPrimary,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalDisplayFont
                    )
                    Text(
                        text = "of $scrollLimit reels",
                        color = AegisTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = ClassicalBodyFont
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HeroMiniStatRow(
                    label = "Screen Time Today",
                    value = ShortVideoScrollDetector.formatMinutesCompact(totalUsageSecondsToday),
                    accent = AegisPrimaryCyan
                )
                HeroMiniStatRow(
                    label = "Gaming Today",
                    value = ShortVideoScrollDetector.formatMinutesCompact(dailyGameSecondsToday),
                    accent = AegisElectricBlue
                )
                HeroMiniStatRow(
                    label = "Mindful Exits",
                    value = "$mindfulExitsToday (${mindfulExitsToday * 4}m saved)",
                    accent = AegisEmeraldTeal
                )
                HeroMiniStatRow(
                    label = "Under-Limit Streak",
                    value = "$cleanStreakDays day${if (cleanStreakDays == 1) "" else "s"}",
                    accent = AegisChampagneGold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = AegisCardBorder.copy(alpha = 0.45f))
        Spacer(modifier = Modifier.height(10.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ShortVideoPlatform.entries.forEach { plat ->
                val count = dailyReelsByPlatform[plat.id] ?: 0
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.78f),
                    border = BorderStroke(1.dp, AegisLavenderBorder.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircleOutline,
                            contentDescription = null,
                            tint = AegisPurple,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "${plat.shortLabel}: $count",
                            color = AegisTextPrimary,
                            fontSize = 11.5.sp,
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
private fun HeroMiniStatRow(
    label: String,
    value: String,
    accent: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.74f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = AegisTextSecondary,
            fontSize = 11.sp,
            fontFamily = ClassicalBodyFont
        )
        Text(
            text = value,
            color = accent,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = ClassicalBodyFont
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ScrollLimitEditorCard(
    scrollLockEnabled: Boolean,
    scrollLimitCount: Int,
    cooldownMinutes: Int,
    bingeNudgeCount: Int,
    selectedPlatforms: Set<String>,
    activeScrollLockedPackages: Map<String, Long>,
    nowMillis: Long,
    onToggleScrollLock: (Boolean) -> Unit,
    onChangeScrollLimit: (Int) -> Unit,
    onChangeCooldownMinutes: (Int) -> Unit,
    onChangeBingeNudgeCount: (Int) -> Unit,
    onTogglePlatform: (String) -> Unit,
    onUnlockLockedPackage: (String) -> Unit
) {
    var sliderLimitDraft by remember(scrollLimitCount) { mutableStateOf(scrollLimitCount.toFloat()) }
    var sliderCooldownDraft by remember(cooldownMinutes) { mutableStateOf(cooldownMinutes.toFloat()) }
    var sliderNudgeDraft by remember(bingeNudgeCount) { mutableStateOf(bingeNudgeCount.toFloat()) }

    CyberCard(
        borderColor = if (scrollLockEnabled) AegisPurple.copy(alpha = 0.55f) else AegisLavenderBorder,
        tone = CyberCardTone.LAVENDER,
        modifier = Modifier.testTag("scroll_limit_editor_card")
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
                    .padding(end = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AegisPurple),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.HourglassBottom,
                        contentDescription = null,
                        tint = AegisOnPrimaryWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto-Stop After Scrolling",
                        color = AegisTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalDisplayFont
                    )
                    Text(
                        text = if (scrollLockEnabled) {
                            "Stops app after $scrollLimitCount Reels/Shorts for ${cooldownMinutes}m"
                        } else {
                            "Counter still tracks • auto-lock is paused"
                        },
                        color = AegisTextSecondary,
                        fontSize = 11.5.sp,
                        fontFamily = ClassicalBodyFont
                    )
                }
            }
            Switch(
                checked = scrollLockEnabled,
                onCheckedChange = onToggleScrollLock,
                modifier = Modifier.testTag("scroll_lock_master_switch"),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AegisOnPrimaryWhite,
                    checkedTrackColor = AegisPurple,
                    uncheckedThumbColor = AegisTextMuted,
                    uncheckedTrackColor = AegisCardBorder
                )
            )
        }

        val activeLocks = activeScrollLockedPackages.filterValues { it > nowMillis }
        if (activeLocks.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            activeLocks.forEach { (pkg, untilMs) ->
                val remMin = ((untilMs - nowMillis) / 60_000L).coerceAtLeast(1L)
                val platLabel = ShortVideoPlatform.findByPackage(pkg).firstOrNull()?.displayName ?: pkg
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = AegisChampagneSoft,
                    border = BorderStroke(1.dp, AegisGoldBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "$platLabel Locked (${remMin}m left)",
                                color = AegisTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = ClassicalBodyFont
                            )
                            Text(
                                text = "Scroll limit cooldown active",
                                color = AegisWarningAmber,
                                fontSize = 11.sp,
                                fontFamily = ClassicalBodyFont
                            )
                        }
                        Button(
                            onClick = { onUnlockLockedPackage(pkg) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AegisChampagneGold,
                                contentColor = AegisOnPrimaryWhite
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("unlock_scroll_lock_$pkg")
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("UNLOCK", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        HorizontalDivider(
            color = AegisLavenderBorder.copy(alpha = 0.5f),
            modifier = Modifier.padding(vertical = 12.dp)
        )

        // 1. Max Reels/Shorts Limit Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Lock App After Scrolling",
                color = AegisTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ClassicalDisplayFont
            )
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = AegisPurple.copy(alpha = 0.14f)
            ) {
                Text(
                    text = "${sliderLimitDraft.roundToInt()} Reels / Shorts",
                    color = AegisPurple,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        Slider(
            value = sliderLimitDraft,
            onValueChange = { sliderLimitDraft = it },
            onValueChangeFinished = {
                val chosen = sliderLimitDraft.roundToInt().coerceIn(5, 60)
                if (chosen != scrollLimitCount) {
                    onChangeScrollLimit(chosen)
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
                .testTag("scroll_limit_slider")
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(10, 20, 30, 45).forEach { preset ->
                val selected = scrollLimitCount == preset
                FilterChip(
                    selected = selected,
                    onClick = {
                        sliderLimitDraft = preset.toFloat()
                        onChangeScrollLimit(preset)
                    },
                    label = {
                        Text(
                            text = "$preset reels",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AegisPurple,
                        selectedLabelColor = AegisOnPrimaryWhite
                    ),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("scroll_preset_$preset")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Auto-Lock Cooldown Duration Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Lock Cooldown Duration",
                color = AegisTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ClassicalDisplayFont
            )
            Text(
                text = "${sliderCooldownDraft.roundToInt()} min lock",
                color = AegisPrimaryCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        Slider(
            value = sliderCooldownDraft,
            onValueChange = { sliderCooldownDraft = it },
            onValueChangeFinished = {
                val chosen = sliderCooldownDraft.roundToInt().coerceIn(5, 60)
                if (chosen != cooldownMinutes) {
                    onChangeCooldownMinutes(chosen)
                }
            },
            valueRange = 5f..60f,
            steps = 10,
            colors = SliderDefaults.colors(
                thumbColor = AegisPrimaryCyan,
                activeTrackColor = AegisPrimaryCyan,
                inactiveTrackColor = AegisCardBorder.copy(alpha = 0.55f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("scroll_cooldown_slider")
        )

        Spacer(modifier = Modifier.height(6.dp))

        // 3. Halfway Doomscroll Binge Nudge Slider
        val draftNudgeVal = sliderNudgeDraft.roundToInt().let { if (it < 5) 0 else it.coerceIn(5, 25) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Halfway Binge Reminder",
                color = AegisTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ClassicalDisplayFont
            )
            Text(
                text = if (draftNudgeVal == 0) "Off" else "Every $draftNudgeVal reels",
                color = AegisEmeraldTeal,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        Slider(
            value = sliderNudgeDraft,
            onValueChange = { sliderNudgeDraft = it },
            onValueChangeFinished = {
                val raw = sliderNudgeDraft.roundToInt()
                val chosen = if (raw < 5) 0 else raw.coerceIn(5, 25)
                sliderNudgeDraft = chosen.toFloat()
                if (chosen != bingeNudgeCount) {
                    onChangeBingeNudgeCount(chosen)
                }
            },
            valueRange = 0f..25f,
            steps = 4,
            colors = SliderDefaults.colors(
                thumbColor = AegisEmeraldTeal,
                activeTrackColor = AegisEmeraldTeal,
                inactiveTrackColor = AegisEmeraldBorder.copy(alpha = 0.45f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("scroll_binge_nudge_slider")
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Protected Short-Video Feeds",
            color = AegisTextPrimary,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = ClassicalDisplayFont
        )
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ShortVideoPlatform.entries.forEach { platform ->
                val enabled = platform.id in selectedPlatforms
                FilterChip(
                    selected = enabled,
                    onClick = { onTogglePlatform(platform.id) },
                    leadingIcon = if (enabled) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    } else null,
                    label = {
                        Text(
                            text = platform.displayName,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AegisPrimaryCyan,
                        selectedLabelColor = AegisOnPrimaryWhite,
                        selectedLeadingIconColor = AegisOnPrimaryWhite
                    ),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("platform_chip_${platform.id}")
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MindfulBreathingConfigCard(
    mindfulPauseEnabled: Boolean,
    selectedPackagesCount: Int,
    graceMinutes: Int,
    onToggleMindfulPause: (Boolean) -> Unit,
    onSelectGraceMinutes: (Int) -> Unit,
    onOpenAppPicker: () -> Unit,
    onPreviewBreathingPause: () -> Unit
) {
    CyberCard(
        borderColor = if (mindfulPauseEnabled) AegisEmeraldTeal.copy(alpha = 0.6f) else AegisEmeraldBorder,
        tone = CyberCardTone.EMERALD,
        modifier = Modifier.testTag("mindful_breathing_config_card")
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
                    .padding(end = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AegisEmeraldTeal),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SelfImprovement,
                        contentDescription = null,
                        tint = AegisOnPrimaryWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "5-Second Mindful Pause",
                        color = AegisTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalDisplayFont
                    )
                    Text(
                        text = "$selectedPackagesCount app(s) paused before opening",
                        color = AegisTextSecondary,
                        fontSize = 11.5.sp,
                        fontFamily = ClassicalBodyFont
                    )
                }
            }
            Switch(
                checked = mindfulPauseEnabled,
                onCheckedChange = onToggleMindfulPause,
                modifier = Modifier.testTag("mindful_pause_master_switch"),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AegisOnPrimaryWhite,
                    checkedTrackColor = AegisEmeraldTeal,
                    uncheckedThumbColor = AegisTextMuted,
                    uncheckedTrackColor = AegisEmeraldBorder
                )
            )
        }

        HorizontalDivider(
            color = AegisEmeraldBorder.copy(alpha = 0.55f),
            modifier = Modifier.padding(vertical = 10.dp)
        )

        Text(
            text = "Re-Prompt Cooldown Window",
            color = AegisTextPrimary,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = ClassicalDisplayFont
        )
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(0 to "Every Open", 5 to "5 min", 10 to "10 min", 15 to "15 min").forEach { (mins, label) ->
                val selected = graceMinutes == mins
                FilterChip(
                    selected = selected,
                    onClick = { onSelectGraceMinutes(mins) },
                    label = { Text(label, fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AegisEmeraldTeal,
                        selectedLabelColor = AegisOnPrimaryWhite
                    ),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("mindful_grace_$mins")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onOpenAppPicker,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AegisEmeraldTeal,
                    contentColor = AegisOnPrimaryWhite
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("mindful_choose_apps_button")
            ) {
                Text("CHOOSE APPS ($selectedPackagesCount)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onPreviewBreathingPause,
                border = BorderStroke(1.2.dp, AegisEmeraldTeal),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AegisEmeraldTeal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("mindful_preview_button")
            ) {
                Text("PREVIEW 5s", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun InstantDetoxAndShareCard(
    detoxRemainingMillis: Long,
    onStartDetox: (Int) -> Unit,
    onStopDetox: () -> Unit,
    onShareWeeklyReport: () -> Unit
) {
    val isDetoxActive = detoxRemainingMillis > 0L
    CyberCard(
        borderColor = AegisGoldBorder,
        tone = CyberCardTone.GOLD,
        modifier = Modifier.testTag("instant_detox_share_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AegisChampagneGold),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = AegisOnPrimaryWhite,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Instant Detox & Report",
                        color = AegisTextPrimary,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalDisplayFont
                    )
                    Text(
                        text = if (isDetoxActive) {
                            val mins = ((detoxRemainingMillis + 59_999L) / 60_000L).coerceAtLeast(1L)
                            "Detox Active • ${mins}m remaining"
                        } else {
                            "1-tap pause for all Reel & distracting apps"
                        },
                        color = if (isDetoxActive) AegisWarningAmber else AegisTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isDetoxActive) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = ClassicalBodyFont
                    )
                }
            }
            StatusPill(
                label = if (isDetoxActive) "DETOX ON" else "READY",
                isActive = isDetoxActive,
                activeColor = AegisChampagneGold,
                inactiveColor = AegisTextMuted
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isDetoxActive) {
                Button(
                    onClick = onStopDetox,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AegisChampagneGold,
                        contentColor = AegisOnPrimaryWhite
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("stop_detox_button")
                ) {
                    Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("END DETOX", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = { onStartDetox(15) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AegisChampagneGold,
                        contentColor = AegisOnPrimaryWhite
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("start_detox_15_button")
                ) {
                    Text("15m DETOX", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { onStartDetox(30) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AegisWarningAmber,
                        contentColor = AegisOnPrimaryWhite
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("start_detox_30_button")
                ) {
                    Text("30m DETOX", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }

            OutlinedButton(
                onClick = onShareWeeklyReport,
                border = BorderStroke(1.2.dp, AegisPrimaryCyan),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AegisPrimaryCyan),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("share_weekly_report_button")
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share Weekly Report", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(5.dp))
                Text("SHARE", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SevenDayScrollTrendsCard(
    history: List<DailyScrollSnapshot>,
    todayReels: Int,
    todayUsageSeconds: Int,
    todayExits: Int,
    todayLocks: Int,
    scrollLimit: Int,
    cleanStreakDays: Int,
    peakWindow: String
) {
    var showScreenTime by remember { mutableStateOf(false) }

    val chartEntries = remember(history, todayReels, todayUsageSeconds, todayExits, todayLocks) {
        val past = history.takeLast(6)
        past + DailyScrollSnapshot(
            dateKey = "Today",
            usageSeconds = todayUsageSeconds,
            reelsCount = todayReels,
            mindfulExits = todayExits,
            locksTriggered = todayLocks
        )
    }

    val maxBarValue = remember(chartEntries, showScreenTime, scrollLimit) {
        val maxData = if (showScreenTime) {
            chartEntries.maxOfOrNull { it.usageSeconds / 60 } ?: 30
        } else {
            maxOf(scrollLimit, chartEntries.maxOfOrNull { it.reelsCount } ?: 10)
        }
        maxData.coerceAtLeast(10)
    }

    CyberCard(
        borderColor = AegisCardBorder,
        tone = CyberCardTone.SKY,
        modifier = Modifier.testTag("seven_day_trends_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "7-Day Focus Trends",
                    color = AegisTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalDisplayFont
                )
                Text(
                    text = "Peak scroll time: $peakWindow",
                    color = AegisTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = ClassicalBodyFont
                )
            }
            FilterChip(
                selected = showScreenTime,
                onClick = { showScreenTime = !showScreenTime },
                label = {
                    Text(
                        text = if (showScreenTime) "Minutes" else "Reels",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AegisPrimaryCyan,
                    selectedLabelColor = AegisOnPrimaryWhite
                ),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("trend_metric_toggle_chip")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(126.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.72f))
                .border(1.dp, AegisCardBorder.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val count = chartEntries.size.coerceAtLeast(1)
                val slotW = size.width / count
                val barW = (slotW * 0.46f).coerceIn(14.dp.toPx(), 34.dp.toPx())
                val chartH = size.height - 8.dp.toPx()

                if (!showScreenTime) {
                    val limitRatio = (scrollLimit.toFloat() / maxBarValue.toFloat()).coerceIn(0f, 1f)
                    val limitY = chartH * (1f - limitRatio)
                    drawLine(
                        color = AegisWarningAmber.copy(alpha = 0.55f),
                        start = Offset(0f, limitY),
                        end = Offset(size.width, limitY),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }

                chartEntries.forEachIndexed { idx, snap ->
                    val rawVal = if (showScreenTime) (snap.usageSeconds / 60) else snap.reelsCount
                    val frac = (rawVal.toFloat() / maxBarValue.toFloat()).coerceIn(0.05f, 1f)
                    val barH = (chartH * frac).coerceAtLeast(6.dp.toPx())
                    val leftX = idx * slotW + (slotW - barW) / 2f
                    val topY = chartH - barH
                    val isOverLimit = !showScreenTime && snap.reelsCount > scrollLimit
                    val barColor = when {
                        isOverLimit -> AegisWarningAmber
                        showScreenTime -> AegisPrimaryCyan
                        else -> AegisPurple
                    }
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(leftX, topY),
                        size = Size(barW, barH),
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            chartEntries.forEach { snap ->
                val shortDay = if (snap.dateKey == "Today") "Today" else snap.dateKey.takeLast(5)
                val num = if (showScreenTime) "${snap.usageSeconds / 60}m" else "${snap.reelsCount}"
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = num,
                        color = AegisTextPrimary,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalBodyFont
                    )
                    Text(
                        text = shortDay,
                        color = AegisTextMuted,
                        fontSize = 9.5.sp,
                        fontFamily = ClassicalBodyFont
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MilestoneBadgeChip(
                title = "Limit Keeper",
                earned = todayReels <= scrollLimit,
                accent = AegisEmeraldTeal
            )
            MilestoneBadgeChip(
                title = "Mindful Starter",
                earned = todayExits >= 3,
                accent = AegisPrimaryCyan
            )
            MilestoneBadgeChip(
                title = "7-Day Streak",
                earned = cleanStreakDays >= 7,
                accent = AegisChampagneGold
            )
        }
    }
}

@Composable
private fun MilestoneBadgeChip(
    title: String,
    earned: Boolean,
    accent: Color
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (earned) accent.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, if (earned) accent.copy(alpha = 0.55f) else AegisCardBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = if (earned) accent else AegisTextMuted,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = title,
                color = if (earned) accent else AegisTextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ClassicalBodyFont
            )
        }
    }
}

@Composable
internal fun MindfulAppPickerDialog(
    installedApps: List<InstalledAppEntry>,
    selectedPackages: Set<String>,
    onTogglePackage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    val filtered = remember(installedApps, search) {
        val q = search.trim().lowercase()
        if (q.isBlank()) installedApps
        else installedApps.filter { it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AegisLavenderSoft,
        titleContentColor = AegisTextPrimary,
        title = {
            Text(
                text = "Pause Apps Before Opening",
                fontWeight = FontWeight.Bold,
                fontFamily = ClassicalDisplayFont
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("Search installed apps") },
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
                        focusedBorderColor = AegisEmeraldTeal,
                        unfocusedBorderColor = AegisLavenderBorder,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filtered, key = { it.packageName }) { app ->
                        val checked = app.packageName in selectedPackages
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (checked) AegisEmeraldSoft else Color.White.copy(alpha = 0.72f))
                                .clickable { onTogglePackage(app.packageName) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
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
                                        .background(AegisEmeraldTeal.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = app.label.take(1).uppercase(),
                                        color = AegisEmeraldTeal,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.label,
                                    color = AegisTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
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
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { onTogglePackage(app.packageName) },
                                colors = CheckboxDefaults.colors(checkedColor = AegisEmeraldTeal)
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
                    containerColor = AegisEmeraldTeal,
                    contentColor = AegisOnPrimaryWhite
                )
            ) {
                Text("DONE", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
internal fun PerAppControlDialog(
    appLabel: String,
    packageName: String,
    usedSecondsToday: Int,
    opensToday: Int,
    currentBudgetMinutes: Int,
    isMindfulPaused: Boolean,
    isAppLocked: Boolean,
    onSetBudgetMinutes: (Int) -> Unit,
    onToggleMindfulPause: (Boolean) -> Unit,
    onToggleAppLock: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var budgetDraft by remember(packageName, currentBudgetMinutes) {
        mutableStateOf(currentBudgetMinutes.toFloat())
    }
    val roundedBudget = budgetDraft.roundToInt().let { if (it < 10) 0 else it.coerceIn(10, 180) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AegisCardBackground,
        titleContentColor = AegisTextPrimary,
        title = {
            Column {
                Text(
                    text = appLabel,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalDisplayFont,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${ShortVideoScrollDetector.formatMinutesCompact(usedSecondsToday)} used • $opensToday opens today",
                    color = AegisTextSecondary,
                    fontSize = 11.5.sp,
                    fontFamily = ClassicalBodyFont
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daily Time Limit",
                        color = AegisTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (roundedBudget == 0) "Unlimited" else "${roundedBudget}m / day",
                        color = AegisPrimaryCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Slider(
                    value = budgetDraft,
                    onValueChange = { budgetDraft = it },
                    onValueChangeFinished = {
                        onSetBudgetMinutes(roundedBudget)
                    },
                    valueRange = 0f..180f,
                    steps = 17,
                    colors = SliderDefaults.colors(
                        thumbColor = AegisPrimaryCyan,
                        activeTrackColor = AegisPrimaryCyan
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("per_app_budget_slider")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.78f))
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("5s Mindful Breathing Pause", color = AegisTextPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                    Switch(
                        checked = isMindfulPaused,
                        onCheckedChange = onToggleMindfulPause,
                        colors = SwitchDefaults.colors(checkedTrackColor = AegisEmeraldTeal)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.78f))
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = AegisChampagneGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Guardian PIN App Lock", color = AegisTextPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Switch(
                        checked = isAppLocked,
                        onCheckedChange = onToggleAppLock,
                        colors = SwitchDefaults.colors(checkedTrackColor = AegisChampagneGold)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AegisPrimaryCyan,
                    contentColor = AegisOnPrimaryWhite
                )
            ) {
                Text("DONE", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
internal fun InAppBreathingPreviewDialog(
    onDismiss: () -> Unit
) {
    val inf = rememberInfiniteTransition(label = "PreviewBreathHalo")
    val scale by inf.animateFloat(
        initialValue = 0.76f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PreviewHaloScale"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AegisEmeraldSoft,
        title = {
            Text(
                text = "5s Mindful Breathing Preview",
                color = AegisTextPrimary,
                fontWeight = FontWeight.Bold,
                fontFamily = ClassicalDisplayFont
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "When a selected app opens, Sheildlenz shows this calm 5-second breathing pause with instant Home exit.",
                    color = AegisTextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    fontFamily = ClassicalBodyFont
                )
                Spacer(modifier = Modifier.height(18.dp))
                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size((105 * scale).dp)
                            .clip(CircleShape)
                            .background(AegisEmeraldTeal.copy(alpha = 0.18f))
                            .border(2.5.dp, AegisEmeraldTeal, CircleShape)
                    )
                    Text(
                        text = "Breathe",
                        color = AegisEmeraldTeal,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalDisplayFont
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AegisEmeraldTeal,
                    contentColor = AegisOnPrimaryWhite
                )
            ) {
                Text("CLOSE PREVIEW", fontWeight = FontWeight.Bold)
            }
        }
    )
}
