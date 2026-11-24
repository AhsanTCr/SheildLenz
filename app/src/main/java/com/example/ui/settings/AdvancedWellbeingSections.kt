package com.example.ui.settings

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import com.example.data.preferences.SafeDnsProvider
import com.example.ui.theme.AegisSuccessGreen
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.audio.AegisSoundEffects
import com.example.data.preferences.AegisPreferences
import com.example.data.preferences.InferenceAccelerationMode
import com.example.security.deviceowner.AegisDeviceAdminReceiver
import com.example.services.dns.AegisSafeDnsVpnService
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberCardTone
import com.example.ui.components.MasterKeyGateState
import com.example.ui.theme.AegisCardBackground
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisEmeraldBorder
import com.example.ui.theme.AegisEmeraldTeal
import com.example.ui.theme.AegisGoldBorder
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SafeDnsAndBlocklistSection(
    preferences: AegisPreferences,
    masterKeyGate: MasterKeyGateState,
    initiallyExpanded: Boolean = false,
    compactPillMode: Boolean = true,
    allowDisableOrWeaken: Boolean = true
) {
    val context = LocalContext.current
    val safeDnsEnabled by preferences.safeDnsEnabled.collectAsState()
    val currentProvider by preferences.safeDnsProvider.collectAsState()
    val blockedDomains by preferences.customBlockedDomains.collectAsState()
    val blockedKeywords by preferences.customBlockedKeywords.collectAsState()
    val blockedQueryCount by AegisSafeDnsVpnService.blockedQueryCount.collectAsState()
    val lastBlockedDomain by AegisSafeDnsVpnService.lastBlockedDomain.collectAsState()

    var domainInput by remember { mutableStateOf("") }
    var keywordInput by remember { mutableStateOf("") }

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
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

    val summaryText = when {
        safeDnsEnabled -> "${currentProvider.ratingBadge} • ${blockedDomains.size + blockedKeywords.size} custom rules"
        else -> "Off • DNS filter inactive"
    }

    val toggleSafeDns: (Boolean) -> Unit = { enable ->
        if (enable) {
            val prepIntent = runCatching { VpnService.prepare(context) }.getOrNull()
            if (prepIntent == null) {
                AegisSoundEffects.playModeOn(context)
                preferences.setSafeDnsEnabled(true)
                AegisSafeDnsVpnService.start(context)
            } else {
                preferences.beginAuthorizedExternalFlow()
                vpnPermissionLauncher.launch(prepIntent)
            }
        } else if (!allowDisableOrWeaken) {
            Toast.makeText(context, "Safe DNS active • Disable in Edits", Toast.LENGTH_SHORT).show()
        } else {
            masterKeyGate.request("disable Safe DNS filter") {
                preferences.setSafeDnsEnabled(false)
                AegisSafeDnsVpnService.stop(context)
            }
        }
    }

    ToggleRevealEditsCard(
        title = "Safe DNS",
        summary = summaryText,
        icon = Icons.Default.Dns,
        accentColor = AegisPrimaryCyan,
        tone = CyberCardTone.SKY,
        borderColor = if (safeDnsEnabled) AegisPrimaryCyan.copy(alpha = 0.6f) else AegisCardBorder,
        checked = safeDnsEnabled,
        onCheckedChange = toggleSafeDns,
        cardTestTag = "safe_dns_blocklist_card",
        switchTestTag = "safe_dns_enabled_switch",
        initiallyExpanded = initiallyExpanded || safeDnsEnabled,
        compactPillMode = compactPillMode,
        dialogTitle = stringResource(R.string.section_safe_dns)
    ) {
        if (blockedQueryCount > 0L) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(AegisPrimaryCyan.copy(alpha = 0.10f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Blocked: $blockedQueryCount" + (lastBlockedDomain?.let { " • $it" } ?: ""),
                    color = AegisPrimaryCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalBodyFont,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        Text(
            text = "DNS Filter Provider",
            color = AegisTextPrimary,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = ClassicalDisplayFont
        )
        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SafeDnsProvider.entries.forEach { prov ->
                val selected = currentProvider == prov
                val borderColor = if (selected) AegisPrimaryCyan else AegisCardBorder
                val containerColor = if (selected) AegisPrimaryCyan.copy(alpha = 0.10f) else AegisCardBackground

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            if (prov != currentProvider) {
                                AegisSoundEffects.playModeOn(context)
                                preferences.setSafeDnsProvider(prov)
                                Toast.makeText(context, "${prov.displayName} activated", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .testTag("dns_provider_${prov.id}"),
                    shape = RoundedCornerShape(12.dp),
                    color = containerColor,
                    border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, if (selected) AegisPrimaryCyan else AegisCardBorder, CircleShape)
                                .background(if (selected) AegisPrimaryCyan else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = prov.displayName,
                                    color = AegisTextPrimary,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = ClassicalDisplayFont,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f).padding(end = 6.dp)
                                )
                                val isStrictProvider = prov == SafeDnsProvider.DEEN_STRICT ||
                                    prov == SafeDnsProvider.CLEANBROWSING_ADULT ||
                                    prov == SafeDnsProvider.OPENDNS_FAMILY ||
                                    prov == SafeDnsProvider.CONTROLD_ADULT
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(
                                            if (isStrictProvider) AegisSuccessGreen.copy(alpha = 0.14f)
                                            else AegisPurple.copy(alpha = 0.14f)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = prov.ratingBadge,
                                        color = if (isStrictProvider) AegisSuccessGreen else AegisPurple,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = ClassicalBodyFont,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                            if (selected) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${prov.subtitle} • ${prov.primaryIp}",
                                    color = AegisTextSecondary,
                                    fontSize = 10.5.sp,
                                    fontFamily = ClassicalBodyFont,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = AegisCardBorder.copy(alpha = 0.45f), modifier = Modifier.padding(vertical = 10.dp))

        Text(
            text = "Blocked Domains",
            color = AegisTextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = ClassicalDisplayFont
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = domainInput,
                onValueChange = { domainInput = it },
                placeholder = { Text("example.com", fontSize = 12.sp) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("add_blocked_domain_input")
            )
            Button(
                onClick = {
                    if (preferences.addCustomBlockedDomain(domainInput)) {
                        domainInput = ""
                    } else {
                        Toast.makeText(context, "Enter a valid domain", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AegisPrimaryCyan, contentColor = AegisOnPrimaryWhite),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("add_blocked_domain_button")
            ) {
                Text("ADD", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (blockedDomains.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                blockedDomains.forEach { domain ->
                    BlocklistTokenChip(
                        text = domain,
                        onRemove = if (allowDisableOrWeaken) {
                            {
                                masterKeyGate.request("remove blocked domain $domain") {
                                    preferences.removeCustomBlockedDomain(domain)
                                }
                            }
                        } else {
                            null
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Blocked Keywords",
            color = AegisTextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = ClassicalDisplayFont
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = keywordInput,
                onValueChange = { keywordInput = it },
                placeholder = { Text("casino", fontSize = 12.sp) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("add_blocked_keyword_input")
            )
            Button(
                onClick = {
                    if (preferences.addCustomBlockedKeyword(keywordInput)) {
                        keywordInput = ""
                    } else {
                        Toast.makeText(context, "Minimum 2 characters", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AegisPurple, contentColor = AegisOnPrimaryWhite),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("add_blocked_keyword_button")
            ) {
                Text("ADD", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (blockedKeywords.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                blockedKeywords.forEach { kw ->
                    BlocklistTokenChip(
                        text = kw,
                        onRemove = if (allowDisableOrWeaken) {
                            {
                                masterKeyGate.request("remove blocked keyword $kw") {
                                    preferences.removeCustomBlockedKeyword(kw)
                                }
                            }
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BlocklistTokenChip(
    text: String,
    onRemove: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(AegisSurfaceElevated)
            .border(1.dp, AegisLavenderBorder, RoundedCornerShape(50))
            .padding(start = 10.dp, end = if (onRemove != null) 4.dp else 10.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = AegisTextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
        if (onRemove != null) {
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove $text",
                    tint = AegisWarningAmber,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

private data class SimpleAppEntry(
    val packageName: String,
    val label: String
)

@Composable
internal fun BedtimeAndBudgetsSection(
    preferences: AegisPreferences,
    masterKeyGate: MasterKeyGateState,
    initiallyExpanded: Boolean = false,
    compactPillMode: Boolean = true,
    allowDisableOrWeaken: Boolean = true
) {
    val context = LocalContext.current
    val bedtimeEnabled by preferences.bedtimeEnabled.collectAsState()
    val bedtimeStart by preferences.bedtimeStartMinutes.collectAsState()
    val bedtimeEnd by preferences.bedtimeEndMinutes.collectAsState()
    val appBudgets by preferences.appTimeBudgets.collectAsState()
    val dailyUsageSeconds by preferences.dailyAppUsageSeconds.collectAsState()

    var showBudgetPicker by remember { mutableStateOf(false) }
    var selectedMinutes by remember { mutableIntStateOf(30) }
    var installedApps by remember { mutableStateOf<List<SimpleAppEntry>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(false) }

    LaunchedEffect(showBudgetPicker) {
        if (showBudgetPicker && installedApps.isEmpty()) {
            isLoadingApps = true
            installedApps = withContext(Dispatchers.IO) {
                val pm = context.packageManager
                val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                pm.queryIntentActivities(intent, 0)
                    .mapNotNull { info ->
                        val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                        if (pkg == context.packageName) return@mapNotNull null
                        val label = info.loadLabel(pm)?.toString()?.trim().orEmpty().ifBlank { pkg }
                        SimpleAppEntry(pkg, label)
                    }
                    .distinctBy { it.packageName }
                    .sortedBy { it.label.lowercase() }
            }
            isLoadingApps = false
        }
    }

    val bedtimeWindowLabel =
        "${AegisPreferences.formatClockTime(bedtimeStart)} → ${AegisPreferences.formatClockTime(bedtimeEnd)}"

    ToggleRevealEditsCard(
        title = "Bedtime & Limits",
        summary = "${if (bedtimeEnabled) bedtimeWindowLabel else "Off"} • ${appBudgets.size} app limits",
        icon = Icons.Default.Bedtime,
        accentColor = AegisEmeraldTeal,
        tone = CyberCardTone.EMERALD,
        borderColor = AegisEmeraldBorder,
        checked = bedtimeEnabled,
        onCheckedChange = { enable ->
            if (!enable) {
                if (!allowDisableOrWeaken) {
                    Toast.makeText(context, "Bedtime active • Disable in Edits", Toast.LENGTH_SHORT).show()
                } else {
                    masterKeyGate.request("disable Bedtime Lock") {
                        preferences.setBedtimeEnabled(false)
                    }
                }
            } else {
                AegisSoundEffects.playModeOn(context)
                preferences.setBedtimeEnabled(true)
            }
        },
        cardTestTag = "bedtime_budgets_card",
        switchTestTag = "bedtime_enabled_switch",
        initiallyExpanded = initiallyExpanded || bedtimeEnabled,
        allowExpandWhenOff = true,
        compactPillMode = compactPillMode,
        isPillActive = bedtimeEnabled || appBudgets.isNotEmpty(),
        dialogTitle = stringResource(R.string.section_bedtime_budgets)
    ) {
        if (bedtimeEnabled) {
            if (allowDisableOrWeaken) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        Triple("9P–7A", 21 * 60, 7 * 60),
                        Triple("10P–6A", 22 * 60, 6 * 60),
                        Triple("11P–7A", 23 * 60, 7 * 60)
                    ).forEach { (label, s, e) ->
                        val isSelected = bedtimeStart == s && bedtimeEnd == e
                        OutlinedButton(
                            onClick = {
                                preferences.setBedtimeWindow(s, e)
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) AegisEmeraldTeal else AegisCardBackground,
                                contentColor = if (isSelected) AegisOnPrimaryWhite else AegisTextPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ScheduleHourStepper(
                        label = "Bedtime",
                        minuteOfDay = bedtimeStart,
                        onMinusHour = { preferences.setBedtimeWindow((bedtimeStart - 60 + 1440) % 1440, bedtimeEnd) },
                        onPlusHour = { preferences.setBedtimeWindow((bedtimeStart + 60) % 1440, bedtimeEnd) },
                        modifier = Modifier.weight(1f)
                    )
                    ScheduleHourStepper(
                        label = "Wake",
                        minuteOfDay = bedtimeEnd,
                        onMinusHour = { preferences.setBedtimeWindow(bedtimeStart, (bedtimeEnd - 60 + 1440) % 1440) },
                        onPlusHour = { preferences.setBedtimeWindow(bedtimeStart, (bedtimeEnd + 60) % 1440) },
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Text(
                    text = "Active Window: $bedtimeWindowLabel • Modify or turn off in Edits",
                    color = AegisEmeraldTeal,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = ClassicalBodyFont
                )
            }

            HorizontalDivider(color = AegisCardBorder.copy(alpha = 0.45f), modifier = Modifier.padding(vertical = 10.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = "App Time Limits",
                    color = AegisTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalDisplayFont
                )
                Text(
                    text = if (appBudgets.isEmpty()) "No limits active" else "${appBudgets.size} apps limited",
                    color = AegisTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = ClassicalBodyFont
                )
            }
            Button(
                onClick = { showBudgetPicker = true },
                colors = ButtonDefaults.buttonColors(containerColor = AegisPrimaryCyan, contentColor = AegisOnPrimaryWhite),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("add_app_budget_button")
            ) {
                Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("SET LIMIT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (appBudgets.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                appBudgets.forEach { (pkg, limitMins) ->
                    val usedMins = (dailyUsageSeconds[pkg] ?: 0) / 60
                    val appLabel = remember(pkg) {
                        runCatching {
                            val ai = context.packageManager.getApplicationInfo(pkg, 0)
                            context.packageManager.getApplicationLabel(ai).toString()
                        }.getOrDefault(pkg)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AegisSurfaceElevated)
                            .border(1.dp, AegisGoldBorder.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                            Text(
                                text = appLabel,
                                color = AegisTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Used ${usedMins}m / ${limitMins}m daily",
                                color = AegisTextSecondary,
                                fontSize = 10.5.sp,
                                fontFamily = ClassicalBodyFont
                            )
                        }
                        if (allowDisableOrWeaken) {
                            IconButton(
                                onClick = {
                                    masterKeyGate.request("remove time limit for $appLabel") {
                                        preferences.removeAppTimeBudget(pkg)
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove budget for $appLabel",
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

    if (showBudgetPicker) {
        AlertDialog(
            onDismissRequest = { showBudgetPicker = false },
            containerColor = AegisLavenderSoft,
            title = {
                Text(
                    text = "Set Daily App Limit",
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
                    Text(
                        text = "Choose daily limit, then tap an app:",
                        color = AegisTextSecondary,
                        fontSize = 12.sp,
                        fontFamily = ClassicalBodyFont
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(15, 30, 60, 120).forEach { mins ->
                            val selected = selectedMinutes == mins
                            OutlinedButton(
                                onClick = { selectedMinutes = mins },
                                contentPadding = PaddingValues(0.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selected) AegisChampagneGold else AegisCardBackground,
                                    contentColor = if (selected) AegisOnPrimaryWhite else AegisTextPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Text("${mins}m", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (isLoadingApps) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = AegisPurple)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().height(260.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(installedApps, key = { it.packageName }) { app ->
                                val existing = appBudgets[app.packageName]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (existing != null) AegisPrimaryCyan.copy(alpha = 0.14f) else AegisCardBackground)
                                        .border(1.dp, AegisLavenderBorder, RoundedCornerShape(12.dp))
                                        .clickable {
                                            if (!allowDisableOrWeaken && existing != null && selectedMinutes > existing) {
                                                Toast.makeText(
                                                    context,
                                                    "Limit active (${existing}m) • Raise or remove in Edits",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                preferences.setAppTimeBudget(app.packageName, selectedMinutes)
                                                Toast.makeText(
                                                    context,
                                                    "${app.label}: ${selectedMinutes}m/day limit set",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                                        Text(
                                            text = app.label,
                                            color = AegisTextPrimary,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = existing?.let { "Limit: ${it}m/day" } ?: app.packageName,
                                            color = if (existing != null) AegisPrimaryCyan else AegisTextMuted,
                                            fontSize = 10.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (existing != null) AegisPrimaryCyan else AegisCardBorder,
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
                    onClick = { showBudgetPicker = false },
                    colors = ButtonDefaults.buttonColors(containerColor = AegisPrimaryCyan, contentColor = AegisOnPrimaryWhite)
                ) {
                    Text("DONE", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

private val SupportedLanguages = listOf(
    "system" to "System Default",
    "en" to "English",
    "ur" to "اردو (Urdu)",
    "ar" to "العربية (Arabic)",
    "es" to "Español",
    "hi" to "हिन्दी (Hindi)",
    "fr" to "Français"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LanguageSelectorSection(
    preferences: AegisPreferences,
    initiallyExpanded: Boolean = false,
    compactPillMode: Boolean = true
) {
    val context = LocalContext.current
    val currentCode by preferences.appLanguageCode.collectAsState()
    val currentLabel = SupportedLanguages.firstOrNull { it.first == currentCode }?.second ?: "System Default"

    CollapsibleEditsCard(
        title = "Language",
        summary = currentLabel,
        icon = Icons.Default.Language,
        accentColor = AegisIndigo,
        tone = CyberCardTone.INDIGO,
        borderColor = AegisIndigoBorder,
        testTag = "language_selector_card",
        initiallyExpanded = initiallyExpanded,
        compactPillMode = compactPillMode,
        isPillActive = currentCode != "system",
        dialogTitle = stringResource(R.string.section_app_language)
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SupportedLanguages.forEach { (code, label) ->
                val selected = currentCode == code
                FilterChip(
                    selected = selected,
                    onClick = {
                        if (currentCode != code) {
                            preferences.setAppLanguageCode(code)
                            (context as? Activity)?.recreate()
                        }
                    },
                    label = {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AegisIndigo,
                        selectedLabelColor = AegisOnPrimaryWhite
                    ),
                    modifier = Modifier
                        .height(42.dp)
                        .testTag("lang_chip_$code")
                )
            }
        }
    }
}

@Composable
internal fun WarningThresholdsSection(
    preferences: AegisPreferences,
    masterKeyGate: MasterKeyGateState,
    initiallyExpanded: Boolean = false,
    compactPillMode: Boolean = true,
    allowDisableOrWeaken: Boolean = true
) {
    val context = LocalContext.current
    val warningThreshold by preferences.warningThresholdCount.collectAsState()
    val warningOnlyNoLockout by preferences.warningOnlyNoLockout.collectAsState()
    val summary = when {
        warningOnlyNoLockout -> "Warn Only • No Lockout"
        warningThreshold == 0 -> "0 Warnings • Instant Lock"
        else -> "$warningThreshold ${if (warningThreshold == 1) "Warning" else "Warnings"}"
    }

    CollapsibleEditsCard(
        title = "Warnings",
        summary = summary,
        icon = Icons.Default.NotificationsActive,
        accentColor = AegisPrimaryCyan,
        tone = CyberCardTone.SKY,
        borderColor = AegisCardBorder,
        testTag = "warning_thresholds_card",
        initiallyExpanded = initiallyExpanded,
        compactPillMode = compactPillMode,
        isPillActive = warningOnlyNoLockout || warningThreshold > 0,
        dialogTitle = "Warning Thresholds"
    ) {
        if (allowDisableOrWeaken || warningOnlyNoLockout) {
            CompactSwitchRow(
                title = "Warn Only (Don't Lockout)",
                checked = warningOnlyNoLockout,
                activeColor = AegisPrimaryCyan,
                onCheckedChange = { enabled ->
                    if (enabled) {
                        if (!allowDisableOrWeaken) {
                            Toast.makeText(context, "Manage Warn-Only in Edits", Toast.LENGTH_SHORT).show()
                        } else {
                            masterKeyGate.request("enable Warn-Only (no lockout)") {
                                preferences.setWarningOnlyNoLockout(true)
                            }
                        }
                    } else {
                        AegisSoundEffects.playModeOn(context)
                        preferences.setWarningOnlyNoLockout(false)
                    }
                },
                testTag = "warning_only_no_lockout_switch"
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        Text(
            text = if (warningOnlyNoLockout) {
                "Apps are closed with a warning alert. Full device lockout never triggers."
            } else {
                "Warnings before full device lockout engages:"
            },
            color = AegisTextSecondary,
            fontSize = 11.5.sp,
            fontFamily = ClassicalBodyFont
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(0 to "0 (Now)", 1 to "1 Warn", 2 to "2 Warn", 3 to "3 Warn").forEach { (count, label) ->
                val selected = !warningOnlyNoLockout && warningThreshold == count
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) AegisPrimaryCyan else AegisCardBackground.copy(alpha = 0.85f))
                        .border(1.dp, if (selected) AegisPrimaryCyan else AegisCardBorder, RoundedCornerShape(10.dp))
                        .clickable {
                            if (warningOnlyNoLockout) {
                                AegisSoundEffects.playModeOn(context)
                                preferences.setWarningOnlyNoLockout(false)
                                preferences.setWarningThresholdCount(count)
                            } else if (count > warningThreshold) {
                                if (!allowDisableOrWeaken) {
                                    Toast.makeText(context, "Raise warnings in Edits", Toast.LENGTH_SHORT).show()
                                } else {
                                    masterKeyGate.request("increase warnings before lock to $count") {
                                        preferences.setWarningThresholdCount(count)
                                    }
                                }
                            } else if (count < warningThreshold) {
                                AegisSoundEffects.playModeOn(context)
                                preferences.setWarningThresholdCount(count)
                            }
                        }
                        .testTag("warning_threshold_$count"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (selected) AegisOnPrimaryWhite else AegisTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
internal fun AiAcceleratorAndCategoriesSection(
    preferences: AegisPreferences,
    masterKeyGate: MasterKeyGateState,
    initiallyExpanded: Boolean = false,
    compactPillMode: Boolean = true
) {
    val context = LocalContext.current
    val accelerationMode by preferences.inferenceAccelerationMode.collectAsState()
    val blockExplicitAdult by preferences.blockExplicitAdult.collectAsState()
    val blockSuggestiveContent by preferences.blockSuggestiveContent.collectAsState()
    val blockSensitiveGraphic by preferences.blockSensitiveGraphic.collectAsState()
    val blockGamblingKeywords by preferences.blockGamblingKeywords.collectAsState()

    val enabledCount = listOf(
        blockExplicitAdult,
        blockSuggestiveContent,
        blockSensitiveGraphic,
        blockGamblingKeywords
    ).count { it }

    CollapsibleEditsCard(
        title = "AI Filters",
        summary = "${accelerationMode.displayName} • $enabledCount/4 Filters Active",
        icon = Icons.Default.Memory,
        accentColor = AegisPurple,
        tone = CyberCardTone.LAVENDER,
        borderColor = AegisLavenderBorder,
        testTag = "ai_categories_card",
        initiallyExpanded = initiallyExpanded,
        compactPillMode = compactPillMode,
        isPillActive = enabledCount > 0,
        dialogTitle = "AI Hardware & Categories"
    ) {
        Text(
            text = "AI Hardware Accelerator",
            color = AegisTextPrimary,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = ClassicalDisplayFont
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                InferenceAccelerationMode.AUTO_BEST to "Auto",
                InferenceAccelerationMode.CPU_XNNPACK to "CPU XNN",
                InferenceAccelerationMode.NNAPI_NPU to "NPU/GPU"
            ).forEach { (mode, label) ->
                val selected = accelerationMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) AegisPurple else AegisCardBackground.copy(alpha = 0.85f))
                        .border(1.dp, if (selected) AegisPurple else AegisCardBorder, RoundedCornerShape(10.dp))
                        .clickable { preferences.setInferenceAccelerationMode(mode) }
                        .testTag("accel_mode_${mode.name}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (selected) AegisOnPrimaryWhite else AegisTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }

        HorizontalDivider(color = AegisCardBorder.copy(alpha = 0.55f), modifier = Modifier.padding(vertical = 8.dp))

        Text(
            text = "Content Category Toggles",
            color = AegisTextPrimary,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = ClassicalDisplayFont
        )
        CompactSwitchRow(
            title = "Explicit Adult Content",
            checked = blockExplicitAdult,
            activeColor = AegisPurple,
            onCheckedChange = { enabled ->
                if (enabled) {
                    AegisSoundEffects.playModeOn(context)
                    preferences.setBlockExplicitAdult(true)
                } else {
                    masterKeyGate.request("disable explicit adult filter") {
                        preferences.setBlockExplicitAdult(false)
                    }
                }
            },
            testTag = "category_explicit_switch"
        )
        CompactSwitchRow(
            title = "Suggestive Visuals",
            checked = blockSuggestiveContent,
            activeColor = AegisPurple,
            onCheckedChange = { enabled ->
                if (enabled) {
                    AegisSoundEffects.playModeOn(context)
                    preferences.setBlockSuggestiveContent(true)
                } else {
                    masterKeyGate.request("disable suggestive visuals filter") {
                        preferences.setBlockSuggestiveContent(false)
                    }
                }
            },
            testTag = "category_suggestive_switch"
        )
        CompactSwitchRow(
            title = "Sensitive / Graphic Media",
            checked = blockSensitiveGraphic,
            activeColor = AegisPurple,
            onCheckedChange = { enabled ->
                if (enabled) {
                    AegisSoundEffects.playModeOn(context)
                    preferences.setBlockSensitiveGraphic(true)
                } else {
                    masterKeyGate.request("disable sensitive graphic filter") {
                        preferences.setBlockSensitiveGraphic(false)
                    }
                }
            },
            testTag = "category_graphic_switch"
        )
        CompactSwitchRow(
            title = "Gambling Websites & Search",
            checked = blockGamblingKeywords,
            activeColor = AegisPurple,
            onCheckedChange = { enabled ->
                if (enabled) {
                    AegisSoundEffects.playModeOn(context)
                    preferences.setBlockGamblingKeywords(true)
                } else {
                    masterKeyGate.request("disable gambling filter") {
                        preferences.setBlockGamblingKeywords(false)
                    }
                }
            },
            testTag = "category_gambling_switch"
        )
    }
}

@Composable
internal fun GuardianRecoverySection(
    preferences: AegisPreferences,
    masterKeyGate: MasterKeyGateState,
    compactPillMode: Boolean = true
) {
    val context = LocalContext.current
    val guardianEmail by preferences.guardianEmail.collectAsState()
    val challengeId by preferences.recoveryChallengeCode.collectAsState()
    var emailDraft by remember(guardianEmail) { mutableStateOf(guardianEmail) }

    CollapsibleEditsCard(
        title = "Recovery ID",
        summary = if (guardianEmail.isBlank()) "ID $challengeId • Tap to set email" else "$guardianEmail • ID $challengeId",
        icon = Icons.Default.Key,
        accentColor = AegisChampagneGold,
        tone = CyberCardTone.GOLD,
        borderColor = AegisGoldBorder,
        testTag = "guardian_recovery_card",
        compactPillMode = compactPillMode,
        isPillActive = guardianEmail.isNotBlank(),
        dialogTitle = "Guardian Recovery & ID"
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .testTag("recovery_challenge_badge"),
            color = AegisCardBackground,
            border = BorderStroke(1.2.dp, AegisChampagneGold.copy(alpha = 0.7f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "CHALLENGE ID",
                        color = AegisChampagneGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontFamily = ClassicalDisplayFont
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = challengeId,
                        color = AegisTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.5.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            cm?.setPrimaryClip(ClipData.newPlainText("Sheildlenz Challenge ID", challengeId))
                            Toast.makeText(context, "Copied ID $challengeId", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.height(42.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy ID", modifier = Modifier.size(14.dp))
                    }
                    OutlinedButton(
                        onClick = {
                            masterKeyGate.request("generate new Challenge ID") {
                                preferences.rotateRecoveryChallengeCode()
                                Toast.makeText(context, "New Challenge ID generated", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier
                            .height(42.dp)
                            .testTag("rotate_challenge_id_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("NEW", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = emailDraft,
                onValueChange = { emailDraft = it },
                placeholder = { Text("guardian@email.com", fontSize = 12.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("settings_guardian_email_input")
            )
            Button(
                onClick = {
                    masterKeyGate.request("update Guardian Recovery Email") {
                        preferences.setGuardianEmail(emailDraft)
                        Toast.makeText(context, "Recovery email saved", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AegisPrimaryCyan, contentColor = AegisOnPrimaryWhite),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("settings_save_email_button")
            ) {
                Text("SAVE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun DeviceOwnerSetupSection(
    onOpenWizard: () -> Unit,
    compactPillMode: Boolean = true
) {
    val context = LocalContext.current
    val isDeviceOwner = remember { AegisDeviceAdminReceiver.isDeviceOwner(context) }
    val isDeviceAdmin = remember { AegisDeviceAdminReceiver.isDeviceAdmin(context) }

    if (compactPillMode) {
        EditsActionCard(
            title = "Device Owner",
            summary = when {
                isDeviceOwner -> "Active • Maximum tamper resistance"
                isDeviceAdmin -> "Device Admin active • Upgrade to Owner"
                else -> "Step-by-step ADB tamper-proof wizard"
            },
            icon = Icons.Default.AdminPanelSettings,
            accentColor = AegisChampagneGold,
            tone = CyberCardTone.GOLD,
            borderColor = AegisGoldBorder,
            actionLabel = if (isDeviceOwner) "STATUS" else "SETUP",
            testTag = "device_owner_card",
            buttonTestTag = "device_owner_wizard_button",
            onClick = {
                AegisSoundEffects.playTap(context)
                onOpenWizard()
            }
        )
        return
    }

    CyberCard(
        borderColor = if (isDeviceOwner) AegisPrimaryCyan.copy(alpha = 0.6f) else AegisLavenderBorder,
        tone = CyberCardTone.SKY,
        modifier = Modifier.testTag("device_owner_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onOpenWizard),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AegisPrimaryCyan.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = AegisPrimaryCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Guided Device Owner Setup",
                        color = AegisTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalDisplayFont,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when {
                            isDeviceOwner -> "Active • Maximum tamper resistance"
                            isDeviceAdmin -> "Device Admin active • Upgrade to Owner"
                            else -> "Step-by-step ADB tamper-proof wizard"
                        },
                        color = if (isDeviceOwner) AegisPrimaryCyan else AegisTextSecondary,
                        fontSize = 11.sp,
                        fontFamily = ClassicalBodyFont,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Button(
                onClick = onOpenWizard,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AegisPrimaryCyan,
                    contentColor = AegisOnPrimaryWhite
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("device_owner_wizard_button")
            ) {
                Text(
                    text = if (isDeviceOwner) "STATUS" else "SETUP",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
internal fun PolicyBackupRestoreSection(
    preferences: AegisPreferences,
    masterKeyGate: MasterKeyGateState,
    initiallyExpanded: Boolean = false,
    compactPillMode: Boolean = true
) {
    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            runCatching {
                val json = preferences.exportPolicyJson()
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(json.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "Policy backup saved successfully", Toast.LENGTH_SHORT).show()
            }.onFailure { err ->
                Toast.makeText(context, "Export failed: ${err.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (!content.isNullOrBlank()) {
                    if (preferences.restorePolicyJson(content)) {
                        Toast.makeText(context, "Policy restored successfully", Toast.LENGTH_SHORT).show()
                        (context as? Activity)?.recreate()
                    } else {
                        Toast.makeText(context, "Invalid backup format or checksum mismatch", Toast.LENGTH_LONG).show()
                    }
                }
            }.onFailure { err ->
                Toast.makeText(context, "Restore failed: ${err.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    CollapsibleEditsCard(
        title = "Backup & Restore",
        summary = "Export or restore settings",
        icon = Icons.Default.Backup,
        accentColor = AegisIndigo,
        tone = CyberCardTone.INDIGO,
        borderColor = AegisIndigoBorder,
        testTag = "policy_backup_restore_card",
        initiallyExpanded = initiallyExpanded,
        compactPillMode = compactPillMode,
        dialogTitle = "Backup & Restore Policy"
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    masterKeyGate.request("export policy backup") {
                        val defaultName = "sheildlenz_policy_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.slzbackup"
                        exportLauncher.launch(defaultName)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(48.dp).testTag("export_policy_button")
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("EXPORT", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            }

            Button(
                onClick = {
                    masterKeyGate.request("restore policy backup") {
                        importLauncher.launch(arrayOf("application/json", "*/*"))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AegisIndigo, contentColor = AegisOnPrimaryWhite),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(48.dp).testTag("restore_policy_button")
            ) {
                Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("RESTORE", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            }
        }
    }
}
