package com.example.ui.settings

import android.app.Activity
import android.net.VpnService
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.AegisPreferences
import com.example.data.preferences.SafeDnsProvider
import com.example.data.repository.BlockedDnsLogRepository
import com.example.services.dns.AegisSafeDnsVpnService
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberCardTone
import com.example.ui.components.CyberTopBar
import com.example.ui.components.LuxurySectionHeader
import com.example.ui.components.SheildlenzBackdrop
import com.example.ui.components.rememberMasterKeyGate
import com.example.ui.theme.AegisCardBackground
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisChampagneSoft
import com.example.ui.theme.AegisDangerRed
import com.example.ui.theme.AegisGoldBorder
import com.example.ui.theme.AegisLavenderBorder
import com.example.ui.theme.AegisLavenderSoft
import com.example.ui.theme.AegisOnPrimaryWhite
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisPurple
import com.example.ui.theme.AegisSuccessGreen
import com.example.ui.theme.AegisTextMuted
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.ClassicalBodyFont
import com.example.ui.theme.ClassicalDisplayFont
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WellbeingHubScreen(
    preferences: AegisPreferences,
    blockedDnsLogRepo: BlockedDnsLogRepository? = null,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val masterKeyGate = rememberMasterKeyGate(preferences)
    val safeDnsEnabled by preferences.safeDnsEnabled.collectAsState()
    val safeDnsProvider by preferences.safeDnsProvider.collectAsState()
    val bedtimeEnabled by preferences.bedtimeEnabled.collectAsState()
    val warningThreshold by preferences.warningThresholdCount.collectAsState()
    val blockedQueryCount by AegisSafeDnsVpnService.blockedQueryCount.collectAsState()
    val liveDnsLogs by (blockedDnsLogRepo?.liveLogs?.collectAsState() ?: remember { mutableStateOf(emptyList()) })

    val vpnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            preferences.setSafeDnsEnabled(true)
            AegisSafeDnsVpnService.start(context)
            Toast.makeText(context, "Safe DNS VPN active", Toast.LENGTH_SHORT).show()
        } else {
            preferences.setSafeDnsEnabled(false)
            Toast.makeText(context, "VPN permission declined", Toast.LENGTH_SHORT).show()
        }
    }

    BackHandler { onNavigateBack() }

    SheildlenzBackdrop(modifier = Modifier.testTag("wellbeing_hub_screen_root")) {
        Scaffold(
            contentWindowInsets = WindowInsets.statusBars,
            topBar = {
                CyberTopBar(
                    title = "Wellbeing & Safe DNS",
                    subtitle = "Strict Halal & Family DNS, Bedtime & Budgets",
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
                // 1. Quick Wellbeing Controls (Spacious, prominent buttons)
                item {
                    LuxurySectionHeader(title = "Wellbeing Toggles", accentColor = AegisPrimaryCyan)
                    Spacer(modifier = Modifier.height(6.dp))
                    CyberCard(
                        borderColor = AegisCardBorder,
                        tone = CyberCardTone.HERO,
                        modifier = Modifier.testTag("wellbeing_quick_buttons_card")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HubQuickActionButton(
                                title = "Safe DNS",
                                status = if (safeDnsEnabled) "ACTIVE (${safeDnsProvider.ratingBadge})" else "TAP ON",
                                icon = Icons.Default.Dns,
                                accentColor = AegisPrimaryCyan,
                                active = safeDnsEnabled,
                                onClick = {
                                    if (!safeDnsEnabled) {
                                        val prep = runCatching { VpnService.prepare(context) }.getOrNull()
                                        if (prep == null) {
                                            preferences.setSafeDnsEnabled(true)
                                            AegisSafeDnsVpnService.start(context)
                                        } else {
                                            preferences.beginAuthorizedExternalFlow()
                                            vpnLauncher.launch(prep)
                                        }
                                    } else {
                                        masterKeyGate.request("disable Safe DNS") {
                                            preferences.setSafeDnsEnabled(false)
                                            AegisSafeDnsVpnService.stop(context)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("hub_safe_dns_button")
                            )
                            HubQuickActionButton(
                                title = "Bedtime Lock",
                                status = if (bedtimeEnabled) "ACTIVE" else "TAP ON",
                                icon = Icons.Default.Bedtime,
                                accentColor = AegisChampagneGold,
                                active = bedtimeEnabled,
                                onClick = {
                                    if (bedtimeEnabled) {
                                        masterKeyGate.request("disable Bedtime Lock") {
                                            preferences.setBedtimeEnabled(false)
                                        }
                                    } else {
                                        preferences.setBedtimeEnabled(true)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("hub_bedtime_button")
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HubQuickActionButton(
                                title = "Warnings Before Lock",
                                status = if (warningThreshold == 0) "0 (INSTANT LOCK)" else "$warningThreshold SCREEN WARNINGS",
                                icon = Icons.Default.NotificationsActive,
                                accentColor = AegisPurple,
                                active = warningThreshold > 0,
                                onClick = {
                                    masterKeyGate.request("cycle warning threshold") {
                                        val next = (warningThreshold + 1) % 4
                                        preferences.setWarningThresholdCount(next)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("hub_warnings_button")
                            )
                        }
                    }
                }

                // 2. Safe DNS Provider & Blocklist (Strict Deen, Cloudflare Family, AdGuard Family)
                item {
                    SafeDnsAndBlocklistSection(
                        preferences = preferences,
                        masterKeyGate = masterKeyGate,
                        initiallyExpanded = true
                    )
                }

                // 3. Live Blocked DNS Query Stream (Real-time inspection of blocked domain queries)
                if (liveDnsLogs.isNotEmpty() || blockedQueryCount > 0L) {
                    item {
                        CyberCard(
                            borderColor = AegisLavenderBorder,
                            tone = CyberCardTone.LAVENDER,
                            modifier = Modifier.testTag("live_dns_log_card")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(AegisPurple.copy(alpha = 0.16f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Block,
                                            contentDescription = null,
                                            tint = AegisPurple,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Recent Blocked DNS Queries",
                                            color = AegisTextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = ClassicalDisplayFont
                                        )
                                        Text(
                                            text = "$blockedQueryCount queries blocked system-wide",
                                            color = AegisTextSecondary,
                                            fontSize = 11.sp,
                                            fontFamily = ClassicalBodyFont
                                        )
                                    }
                                }

                                if (liveDnsLogs.isNotEmpty()) {
                                    Button(
                                        onClick = { blockedDnsLogRepo?.clearLogs() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = AegisChampagneSoft,
                                            contentColor = AegisTextPrimary
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text("CLEAR", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (liveDnsLogs.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = AegisCardBorder.copy(alpha = 0.45f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    liveDnsLogs.take(8).forEach { log ->
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp),
                                            color = AegisCardBackground,
                                            border = BorderStroke(1.dp, AegisCardBorder.copy(alpha = 0.6f))
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
                                                        text = log.domain,
                                                        color = AegisTextPrimary,
                                                        fontSize = 12.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "Rule: ${log.matchedRule}",
                                                        color = AegisTextMuted,
                                                        fontSize = 10.sp,
                                                        fontFamily = ClassicalBodyFont
                                                    )
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(AegisDangerRed.copy(alpha = 0.12f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = log.reasonCategory,
                                                        color = AegisDangerRed,
                                                        fontSize = 9.sp,
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
                    }
                }

                // 4. Bedtime Lock & Daily App Usage Budgets
                item {
                    BedtimeAndBudgetsSection(
                        preferences = preferences,
                        masterKeyGate = masterKeyGate,
                        initiallyExpanded = true
                    )
                }

                // 5. Warning Thresholds Before Device Lockdown
                item {
                    WarningThresholdsSection(
                        preferences = preferences,
                        masterKeyGate = masterKeyGate,
                        initiallyExpanded = true
                    )
                }
            }
        }
    }
}

@Composable
private fun HubQuickActionButton(
    title: String,
    status: String,
    icon: ImageVector,
    accentColor: Color,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (active) accentColor else AegisCardBackground)
            .border(
                width = if (active) 1.5.dp else 1.dp,
                color = if (active) accentColor else AegisCardBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (active) Color.White.copy(alpha = 0.22f) else accentColor.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (active) AegisOnPrimaryWhite else accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (active) AegisOnPrimaryWhite else AegisTextPrimary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ClassicalDisplayFont,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = status,
                color = if (active) AegisOnPrimaryWhite.copy(alpha = 0.9f) else AegisTextSecondary,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = ClassicalBodyFont,
                maxLines = 1
            )
        }
    }
}
