package com.example.ui.history

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.models.SecurityIncident
import com.example.data.preferences.AegisPreferences
import com.example.data.repository.SecurityHistoryRepository
import com.example.data.repository.ThreatAnalyticsSummary
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberCardTone
import com.example.ui.components.CyberTopBar
import com.example.ui.components.SeverityTag
import com.example.ui.components.SheildlenzBackdrop
import com.example.ui.components.ThreatCategoryBadge
import com.example.ui.components.rememberMasterKeyGate
import com.example.ui.theme.AegisCardBackground
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisChampagneSoft
import com.example.ui.theme.AegisDangerRed
import com.example.ui.theme.AegisGoldBorder
import com.example.ui.theme.AegisGoldDeepTint
import com.example.ui.theme.AegisLavenderBorder
import com.example.ui.theme.AegisLavenderLight
import com.example.ui.theme.AegisLavenderSoft
import com.example.ui.theme.AegisLavenderSurface
import com.example.ui.theme.AegisOnPrimaryWhite
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisPurple
import com.example.ui.theme.AegisSuccessGreen
import com.example.ui.theme.AegisSurfaceElevated
import com.example.ui.theme.AegisTextMuted
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.AegisWarningAmber
import com.example.ui.theme.ClassicalBodyFont
import com.example.ui.theme.ClassicalDisplayFont
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class BlockedAppSummary(
    val packageName: String,
    val appDisplayName: String,
    val count: Int,
    val latestIncident: SecurityIncident
)

@Composable
fun SecurityHistoryScreen(
    historyRepo: SecurityHistoryRepository,
    preferences: AegisPreferences,
    onNavigateBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val incidents by historyRepo.getIncidents().collectAsState(initial = emptyList())
    val totalDetections by historyRepo.getTotalDetectionsCount().collectAsState(initial = 0)
    val blockedAppCount by historyRepo.getBlockedAppCount().collectAsState(initial = 0)
    val scope = rememberCoroutineScope()
    var showClearDialog by remember { mutableStateOf(false) }
    var isHistoryExpanded by remember { mutableStateOf(false) }
    val masterKeyGate = rememberMasterKeyGate(preferences)

    val blockedAppsBreakdown = remember(incidents) {
        incidents
            .groupBy { it.packageName.ifBlank { it.appDisplayName } }
            .map { (pkg, group) ->
                val latest = group.maxByOrNull { it.timestampMillis } ?: group.first()
                BlockedAppSummary(
                    packageName = pkg,
                    appDisplayName = latest.appDisplayName.ifBlank { pkg },
                    count = group.size,
                    latestIncident = latest
                )
            }
            .sortedByDescending { it.count }
    }

    val analytics = remember(incidents) {
        SecurityHistoryRepository.computeAnalytics(incidents)
    }

    SheildlenzBackdrop(modifier = Modifier.testTag("security_history_screen_root")) {
        Scaffold(
            contentWindowInsets = WindowInsets.statusBars,
            topBar = {
                CyberTopBar(
                    title = "Progress",
                    onNavigateBack = onNavigateBack,
                    actions = {
                        IconButton(
                            onClick = {
                                val reportText = buildAuditReportText(
                                    analytics = analytics,
                                    totalDetections = totalDetections,
                                    blockedAppCount = blockedAppCount
                                )
                                preferences.beginAuthorizedExternalFlow()
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Sheildlenz Security Audit Report")
                                    putExtra(Intent.EXTRA_TEXT, reportText)
                                }
                                context.startActivity(
                                    Intent.createChooser(sendIntent, "Export Sheildlenz Audit Report")
                                )
                            },
                            modifier = Modifier.testTag("export_audit_report_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Export Audit Report",
                                tint = AegisPrimaryCyan
                            )
                        }
                        if (incidents.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    masterKeyGate.request("clear security history") {
                                        showClearDialog = true
                                    }
                                },
                                modifier = Modifier.testTag("clear_history_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Clear History",
                                    tint = AegisDangerRed
                                )
                            }
                        }
                    }
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Sculpted Arch-Capsule Blocked Applications Card
                item {
                    val ledgerCardShape = RoundedCornerShape(
                        topStart = 28.dp,
                        topEnd = 14.dp,
                        bottomStart = 14.dp,
                        bottomEnd = 28.dp
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 7.dp,
                                shape = ledgerCardShape,
                                ambientColor = AegisPurple.copy(alpha = 0.16f),
                                spotColor = AegisPurple.copy(alpha = 0.16f)
                            )
                            .clip(ledgerCardShape)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(AegisLavenderSoft, AegisLavenderSurface)
                                )
                            )
                            .border(1.2.dp, AegisLavenderBorder, ledgerCardShape)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Blocked Applications",
                            color = AegisTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = ClassicalDisplayFont
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        if (blockedAppsBreakdown.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.65f))
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No apps blocked yet.",
                                    color = AegisTextMuted,
                                    fontSize = 12.sp,
                                    fontFamily = ClassicalBodyFont
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                blockedAppsBreakdown.forEach { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(50))
                                            .background(Color.White.copy(alpha = 0.72f))
                                            .border(1.dp, AegisLavenderBorder.copy(alpha = 0.7f), RoundedCornerShape(50))
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        brush = Brush.linearGradient(
                                                            colors = listOf(AegisPurple, AegisPrimaryCyan)
                                                        )
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = item.appDisplayName.take(1).uppercase(),
                                                    color = AegisOnPrimaryWhite,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = item.appDisplayName,
                                                    color = AegisTextPrimary,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = ClassicalDisplayFont,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = item.packageName,
                                                    color = AegisTextMuted,
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            ThreatCategoryBadge(category = item.latestIncident.category)
                                            Box(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(AegisDangerRed.copy(alpha = 0.14f))
                                                    .border(1.dp, AegisDangerRed.copy(alpha = 0.40f), CircleShape)
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "${item.count}×",
                                                    color = AegisDangerRed,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Security History Horizontal 1-Tap Open/Close Control
                item {
                    val toggleShape = RoundedCornerShape(22.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 60.dp)
                            .shadow(
                                elevation = 6.dp,
                                shape = toggleShape,
                                ambientColor = AegisPrimaryCyan.copy(alpha = 0.16f),
                                spotColor = AegisPurple.copy(alpha = 0.16f)
                            )
                            .clip(toggleShape)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFFF9FCFF),
                                        AegisLavenderSoft,
                                        AegisChampagneSoft.copy(alpha = 0.72f)
                                    )
                                )
                            )
                            .border(
                                width = 1.2.dp,
                                color = if (isHistoryExpanded) AegisPrimaryCyan.copy(alpha = 0.55f) else AegisLavenderBorder,
                                shape = toggleShape
                            )
                            .clickable { isHistoryExpanded = !isHistoryExpanded }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .testTag("toggle_security_history_button"),
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
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(AegisPrimaryCyan, AegisPurple)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = AegisOnPrimaryWhite,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Security History",
                                    color = AegisTextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = ClassicalDisplayFont
                                )
                                Text(
                                    text = if (incidents.isEmpty()) {
                                        "No incidents logged"
                                    } else {
                                        "${incidents.size} recorded ${if (incidents.size == 1) "event" else "events"}"
                                    },
                                    color = AegisTextSecondary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = ClassicalBodyFont
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (isHistoryExpanded) AegisPurple else AegisPrimaryCyan
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (isHistoryExpanded) "HIDE" else "OPEN",
                                color = AegisOnPrimaryWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp,
                                fontFamily = ClassicalBodyFont
                            )
                            Icon(
                                imageVector = if (isHistoryExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isHistoryExpanded) "Hide Security History" else "Open Security History",
                                tint = AegisOnPrimaryWhite,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                if (isHistoryExpanded) {
                    if (incidents.isEmpty()) {
                        item {
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                CyberCard(
                                    borderColor = AegisGoldBorder,
                                    tone = CyberCardTone.HERO
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 14.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    brush = Brush.linearGradient(
                                                        colors = listOf(AegisPrimaryCyan, AegisPurple, AegisChampagneGold)
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Shield,
                                                contentDescription = null,
                                                tint = AegisOnPrimaryWhite,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "No Threats Detected",
                                            color = AegisTextPrimary,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = ClassicalDisplayFont
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "No incidents recorded yet.",
                                            color = AegisTextMuted,
                                            fontSize = 12.sp,
                                            fontFamily = ClassicalBodyFont,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        items(incidents, key = { it.id }) { incident ->
                            IncidentItemCard(
                                incident = incident,
                                onToggleFalsePositive = {
                                    scope.launch {
                                        historyRepo.markFalsePositive(incident.id, !incident.isReviewedFalsePositive)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            if (showClearDialog) {
                AlertDialog(
                    onDismissRequest = { showClearDialog = false },
                    containerColor = AegisChampagneSoft,
                    titleContentColor = AegisTextPrimary,
                    textContentColor = AegisTextSecondary,
                    title = { Text(text = "Clear Security Audit Log?", fontFamily = ClassicalDisplayFont, fontWeight = FontWeight.Bold) },
                    text = {
                        Text(
                            text = "This will permanently purge all recorded detection history and threat timestamps from local SQLite storage.",
                            fontFamily = ClassicalBodyFont
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                scope.launch {
                                    historyRepo.clearHistory()
                                    showClearDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AegisChampagneGold,
                                contentColor = AegisOnPrimaryWhite
                            )
                        ) {
                            Text(text = "PURGE LOG", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { showClearDialog = false },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = AegisCardBackground,
                                contentColor = AegisTextPrimary
                            )
                        ) {
                            Text(text = "CANCEL", fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun IncidentItemCard(
    incident: SecurityIncident,
    onToggleFalsePositive: () -> Unit
) {
    val formattedDate = SimpleDateFormat("MMM dd, yyyy • HH:mm:ss", Locale.getDefault())
        .format(Date(incident.timestampMillis))

    CyberCard(
        borderColor = if (incident.isReviewedFalsePositive) AegisCardBorder else AegisGoldBorder,
        tone = if (incident.isReviewedFalsePositive) CyberCardTone.SKY else CyberCardTone.GOLD
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = incident.appDisplayName,
                    color = AegisTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalDisplayFont
                )
                Text(
                    text = incident.packageName,
                    color = AegisTextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            SeverityTag(severity = incident.severity)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ThreatCategoryBadge(category = incident.category)
            Text(
                text = "Confidence ${(incident.confidenceScore * 100).toInt()}%",
                color = AegisPrimaryCyan,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Action: ${incident.actionTaken}",
            color = AegisTextSecondary,
            fontSize = 12.sp,
            fontFamily = ClassicalBodyFont,
            fontWeight = FontWeight.Medium
        )

        if (incident.diagnosticSummary.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AegisSurfaceElevated)
                    .border(1.dp, AegisCardBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = incident.diagnosticSummary,
                    color = AegisTextPrimary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    fontFamily = ClassicalBodyFont
                )
            }
        }

        if (incident.lockdownDurationMinutes > 0) {
            Text(
                text = "Lockdown Penalty Enforced: ${AegisPreferences.formatMinutesLabel(incident.lockdownDurationMinutes)}",
                color = AegisDangerRed,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ClassicalBodyFont,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formattedDate,
                color = AegisTextMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )

            OutlinedButton(
                onClick = onToggleFalsePositive,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (incident.isReviewedFalsePositive) AegisSuccessGreen else AegisWarningAmber
                ),
                modifier = Modifier.height(34.dp)
            ) {
                Icon(
                    imageVector = if (incident.isReviewedFalsePositive) Icons.Default.Check else Icons.Default.Flag,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (incident.isReviewedFalsePositive) "MARKED SAFE" else "FALSE POSITIVE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalBodyFont
                )
            }
        }
    }
}

internal fun buildAuditReportText(
    analytics: ThreatAnalyticsSummary,
    totalDetections: Int,
    blockedAppCount: Int,
    generatedAtMillis: Long = System.currentTimeMillis()
): String {
    val generatedDate = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.US)
        .format(Date(generatedAtMillis))
    val weeklyTotal = analytics.weeklyTrend.sumOf { it.count }
    val topApp = analytics.peakAppDisplayName ?: "None"
    return buildString {
        appendLine("Sheildlenz Offline Security Audit Report")
        appendLine("Generated: $generatedDate")
        appendLine("----------------------------------------")
        appendLine("Total Blocked Detections: $totalDetections")
        appendLine("Unique Blocked Apps: $blockedAppCount")
        appendLine("Last 7 Days Detections: $weeklyTotal")
        appendLine("Peak Threat Window: ${analytics.peakHourWindow}")
        appendLine("Most Flagged App: $topApp")
        appendLine("")
        appendLine("Category Breakdown:")
        appendLine("- Explicit Adult: ${analytics.explicitAdultCount}")
        appendLine("- Suggestive Content: ${analytics.suggestiveCount}")
        appendLine("- Sensitive Graphic: ${analytics.sensitiveGraphicCount}")
    }.trim()
}
