package com.example.ui.settings

import android.app.Activity
import android.net.VpnService
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.AegisPreferences
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
import com.example.ui.theme.AegisOnPrimaryWhite
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisPurple
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.ClassicalBodyFont
import com.example.ui.theme.ClassicalDisplayFont

@Composable
fun WellbeingHubScreen(
    preferences: AegisPreferences,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val masterKeyGate = rememberMasterKeyGate(preferences)
    val safeDnsEnabled by preferences.safeDnsEnabled.collectAsState()
    val bedtimeEnabled by preferences.bedtimeEnabled.collectAsState()
    val warningThreshold by preferences.warningThresholdCount.collectAsState()
    var showDeviceOwnerWizard by remember { mutableStateOf(false) }

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
                    title = "Wellbeing & Guard",
                    subtitle = "Safe DNS, Bedtime, Limits & Recovery",
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
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Separate Dominant Quick-Action Feature Buttons Grid
                item {
                    LuxurySectionHeader(title = "Quick Controls", accentColor = AegisPrimaryCyan)
                    Spacer(modifier = Modifier.height(6.dp))
                    CyberCard(
                        borderColor = AegisCardBorder,
                        tone = CyberCardTone.HERO,
                        modifier = Modifier.testTag("wellbeing_quick_buttons_card")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HubQuickActionButton(
                                title = "Safe DNS",
                                status = if (safeDnsEnabled) "ACTIVE" else "TAP ON",
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
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HubQuickActionButton(
                                title = "Warnings",
                                status = if (warningThreshold == 0) "0 (NOW)" else "$warningThreshold WARN",
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
                                    .weight(1f)
                                    .testTag("hub_warnings_button")
                            )
                            HubQuickActionButton(
                                title = "Device Owner",
                                status = "WIZARD",
                                icon = Icons.Default.AdminPanelSettings,
                                accentColor = AegisPrimaryCyan,
                                active = false,
                                onClick = { showDeviceOwnerWizard = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("hub_device_owner_button")
                            )
                        }
                    }
                }

                // Separate Dedicated Cards for Each New Capability
                item {
                    GuardianRecoverySection(
                        preferences = preferences,
                        masterKeyGate = masterKeyGate
                    )
                }

                item {
                    SafeDnsAndBlocklistSection(
                        preferences = preferences,
                        masterKeyGate = masterKeyGate,
                        initiallyExpanded = true
                    )
                }

                item {
                    BedtimeAndBudgetsSection(
                        preferences = preferences,
                        masterKeyGate = masterKeyGate,
                        initiallyExpanded = true
                    )
                }

                item {
                    WarningThresholdsSection(
                        preferences = preferences,
                        masterKeyGate = masterKeyGate,
                        initiallyExpanded = true
                    )
                }

                item {
                    AiAcceleratorAndCategoriesSection(
                        preferences = preferences,
                        masterKeyGate = masterKeyGate,
                        initiallyExpanded = true
                    )
                }

                item {
                    DeviceOwnerSetupSection(
                        onOpenWizard = { showDeviceOwnerWizard = true }
                    )
                }

                item {
                    PolicyBackupRestoreSection(
                        preferences = preferences,
                        masterKeyGate = masterKeyGate,
                        initiallyExpanded = false
                    )
                }

                item {
                    LanguageSelectorSection(
                        preferences = preferences,
                        initiallyExpanded = false
                    )
                }
            }

            if (showDeviceOwnerWizard) {
                DeviceOwnerWizardDialog(
                    preferences = preferences,
                    onDismiss = { showDeviceOwnerWizard = false },
                    onStatusChanged = {}
                )
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
            .height(60.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (active) accentColor else AegisCardBackground)
            .border(
                width = if (active) 1.5.dp else 1.dp,
                color = if (active) accentColor else AegisCardBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (active) Color.White.copy(alpha = 0.22f) else accentColor.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (active) AegisOnPrimaryWhite else accentColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (active) AegisOnPrimaryWhite else AegisTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ClassicalDisplayFont,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = status,
                color = if (active) AegisOnPrimaryWhite.copy(alpha = 0.9f) else AegisTextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = ClassicalBodyFont,
                maxLines = 1
            )
        }
    }
}
