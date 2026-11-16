package com.example.ui.settings

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.preferences.AegisPreferences
import com.example.services.dns.AegisSafeDnsVpnService
import com.example.ui.components.CyberCardTone
import com.example.ui.components.MasterKeyGateState
import com.example.ui.theme.AegisCardBackground
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisGoldBorder
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
    masterKeyGate: MasterKeyGateState
) {
    val context = LocalContext.current
    val safeDnsEnabled by preferences.safeDnsEnabled.collectAsState()
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
            preferences.setSafeDnsEnabled(true)
            AegisSafeDnsVpnService.start(context)
            Toast.makeText(context, "Safe DNS VPN active", Toast.LENGTH_SHORT).show()
        } else {
            preferences.setSafeDnsEnabled(false)
            Toast.makeText(context, "VPN permission declined", Toast.LENGTH_SHORT).show()
        }
    }

    val summaryText = buildString {
        append(if (safeDnsEnabled) "Safe DNS ON" else "Safe DNS OFF")
        append(" • ${blockedDomains.size} domains, ${blockedKeywords.size} keywords")
    }

    CollapsibleEditsCard(
        title = stringResource(R.string.section_safe_dns),
        summary = summaryText,
        icon = Icons.Default.Dns,
        accentColor = AegisPrimaryCyan,
        tone = CyberCardTone.SKY,
        borderColor = if (safeDnsEnabled) AegisPrimaryCyan.copy(alpha = 0.6f) else AegisCardBorder,
        testTag = "safe_dns_blocklist_card"
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = "System-Wide Safe DNS",
                    color = AegisTextPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalDisplayFont
                )
                Text(
                    text = "Zero-battery local DNS filter (Cloudflare Family 1.1.1.3) + SafeSearch enforcement.",
                    color = AegisTextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    fontFamily = ClassicalBodyFont
                )
            }
            Switch(
                checked = safeDnsEnabled,
                onCheckedChange = { enable ->
                    if (enable) {
                        val prepIntent = runCatching { VpnService.prepare(context) }.getOrNull()
                        if (prepIntent == null) {
                            preferences.setSafeDnsEnabled(true)
                            AegisSafeDnsVpnService.start(context)
                        } else {
                            preferences.beginAuthorizedExternalFlow()
                            vpnPermissionLauncher.launch(prepIntent)
                        }
                    } else {
                        masterKeyGate.request("disable Safe DNS filter") {
                            preferences.setSafeDnsEnabled(false)
                            AegisSafeDnsVpnService.stop(context)
                        }
                    }
                },
                modifier = Modifier.testTag("safe_dns_enabled_switch"),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AegisOnPrimaryWhite,
                    checkedTrackColor = AegisPrimaryCyan,
                    uncheckedThumbColor = AegisTextMuted,
                    uncheckedTrackColor = AegisCardBorder
                )
            )
        }

        if (safeDnsEnabled || blockedQueryCount > 0L) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Blocked DNS queries: $blockedQueryCount" +
                    (lastBlockedDomain?.let { " • Last: $it" } ?: ""),
                color = AegisPrimaryCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = ClassicalBodyFont
            )
        }

        HorizontalDivider(color = AegisCardBorder.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 10.dp))

        Text(
            text = "Custom Blocked Websites",
            color = AegisTextPrimary,
            fontSize = 12.5.sp,
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
                placeholder = { Text("e.g. example.com", fontSize = 12.sp) },
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
                        Toast.makeText(context, "Enter a valid domain (e.g. site.com)", Toast.LENGTH_SHORT).show()
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
                        onRemove = {
                            masterKeyGate.request("remove blocked domain $domain") {
                                preferences.removeCustomBlockedDomain(domain)
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Custom Blocked Keywords",
            color = AegisTextPrimary,
            fontSize = 12.5.sp,
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
                placeholder = { Text("e.g. casino", fontSize = 12.sp) },
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
                        Toast.makeText(context, "Keyword must be at least 2 characters", Toast.LENGTH_SHORT).show()
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
                        onRemove = {
                            masterKeyGate.request("remove blocked keyword $kw") {
                                preferences.removeCustomBlockedKeyword(kw)
                            }
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
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(AegisSurfaceElevated)
            .border(1.dp, AegisLavenderBorder, RoundedCornerShape(50))
            .padding(start = 10.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = AegisTextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
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

private data class SimpleAppEntry(
    val packageName: String,
    val label: String
)

@Composable
internal fun BedtimeAndBudgetsSection(
    preferences: AegisPreferences,
    masterKeyGate: MasterKeyGateState
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

    CollapsibleEditsCard(
        title = stringResource(R.string.section_bedtime_budgets),
        summary = "${if (bedtimeEnabled) "Bedtime $bedtimeWindowLabel" else "Bedtime Off"} • ${appBudgets.size} app limits",
        icon = Icons.Default.Bedtime,
        accentColor = AegisChampagneGold,
        tone = CyberCardTone.GOLD,
        borderColor = AegisGoldBorder,
        testTag = "bedtime_budgets_card"
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = "Bedtime Device Lock",
                    color = AegisTextPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalDisplayFont
                )
                Text(
                    text = bedtimeWindowLabel,
                    color = if (bedtimeEnabled) AegisChampagneGold else AegisTextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = ClassicalBodyFont
                )
            }
            Switch(
                checked = bedtimeEnabled,
                onCheckedChange = { enable ->
                    if (!enable) {
                        masterKeyGate.request("disable Bedtime Lock") {
                            preferences.setBedtimeEnabled(false)
                        }
                    } else {
                        preferences.setBedtimeEnabled(true)
                    }
                },
                modifier = Modifier.testTag("bedtime_enabled_switch"),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AegisOnPrimaryWhite,
                    checkedTrackColor = AegisChampagneGold,
                    uncheckedThumbColor = AegisTextMuted,
                    uncheckedTrackColor = AegisCardBorder
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

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
                        if (!bedtimeEnabled) preferences.setBedtimeEnabled(true)
                    },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected) AegisChampagneGold else AegisCardBackground,
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

        HorizontalDivider(color = AegisCardBorder.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = "Daily App Time Budgets",
                    color = AegisTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalDisplayFont
                )
                Text(
                    text = "Set per-app daily minute limits with PIN override.",
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
                                            preferences.setAppTimeBudget(app.packageName, selectedMinutes)
                                            Toast.makeText(
                                                context,
                                                "${app.label}: ${selectedMinutes}m/day limit set",
                                                Toast.LENGTH_SHORT
                                            ).show()
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
    preferences: AegisPreferences
) {
    val context = LocalContext.current
    val currentCode by preferences.appLanguageCode.collectAsState()
    val currentLabel = SupportedLanguages.firstOrNull { it.first == currentCode }?.second ?: "System Default"

    CollapsibleEditsCard(
        title = stringResource(R.string.section_app_language),
        summary = currentLabel,
        icon = Icons.Default.Language,
        accentColor = AegisPurple,
        tone = CyberCardTone.LAVENDER,
        borderColor = AegisLavenderBorder,
        testTag = "language_selector_card"
    ) {
        Text(
            text = "Choose interface language (applies immediately):",
            color = AegisTextSecondary,
            fontSize = 11.5.sp,
            fontFamily = ClassicalBodyFont
        )
        Spacer(modifier = Modifier.height(8.dp))
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
                        selectedContainerColor = AegisPurple,
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
