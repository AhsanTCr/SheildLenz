package com.example.ui.about

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import android.widget.Toast
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.R
import com.example.core.audio.AegisSoundEffects
import com.example.core.update.ApkDownloadState
import com.example.core.update.UpdateChecker
import com.example.data.preferences.AegisPreferences
import com.example.services.protection.MasterPowerController
import com.example.ui.components.AegisShieldGraphic
import com.example.ui.components.AhsanTcrBrandingPill
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberCardTone
import com.example.ui.components.CyberTopBar
import com.example.ui.components.LuxurySectionHeader
import com.example.ui.components.SheildlenzBackdrop
import com.example.ui.components.rememberMasterKeyGate
import com.example.ui.settings.LanguageSelectorSection
import com.example.ui.settings.TouchSoundsCard
import com.example.ui.theme.AegisCardBackground
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisChampagneSoft
import com.example.ui.theme.AegisElectricBlue
import com.example.ui.theme.AegisEmeraldBorder
import com.example.ui.theme.AegisEmeraldSoft
import com.example.ui.theme.AegisEmeraldSurface
import com.example.ui.theme.AegisEmeraldTeal
import com.example.ui.theme.AegisGoldBorder
import com.example.ui.theme.AegisGoldBright
import com.example.ui.theme.AegisGoldDeepTint
import com.example.ui.theme.AegisIndigo
import com.example.ui.theme.AegisLavenderBorder
import com.example.ui.theme.AegisLavenderLight
import com.example.ui.theme.AegisLavenderSoft
import com.example.ui.theme.AegisLavenderSurface
import com.example.ui.theme.AegisOnPrimaryWhite
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisPurple
import com.example.ui.theme.AegisSkySoft
import com.example.ui.theme.AegisSuccessGreen
import com.example.ui.theme.AegisTextMuted
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.AegisWarningAmber
import com.example.ui.theme.ClassicalBodyFont
import com.example.ui.theme.ClassicalDisplayFont

private data class FaqEntry(
    val question: String,
    val answer: String,
    val icon: ImageVector
)

private val SheildlenzFaqs = listOf(
    FaqEntry(
        question = "How does Guardian Recovery work if I forget my PIN?",
        answer = "Set your Master Key PIN and Guardian Email during initial setup. Tap \"Forgot PIN?\" on any PIN prompt or scan the QR on a timed-out lockdown screen from another phone running Sheildlenz signed in with that same Guardian email. Timed-out QR codes generate a one-time code that ends only the timer while Shield stays ON; Forgot-PIN QR codes authorize resetting the Master Key. Challenge IDs rotate automatically after every use.",
        icon = Icons.Default.QrCode2
    ),
    FaqEntry(
        question = "Does Sheildlenz store screenshots or send screen frames off-device?",
        answer = "No. Captured screen frames are resized into 256px tensors in volatile memory, evaluated locally by TensorFlow Lite, and discarded immediately with zero disk writes or remote frame uploads.",
        icon = Icons.Default.VisibilityOff
    ),
    FaqEntry(
        question = "How does Strict Safe DNS block adult websites across apps?",
        answer = "The local loopback VPN enforces strict adult-filtering upstream DNS providers (CleanBrowsing Family 185.228.168.168, CleanBrowsing Adult 185.228.168.10, OpenDNS FamilyShield 208.67.222.123, Control D Adult 76.76.2.4, Cloudflare Family 1.1.1.3) combined with on-device adult TLD/domain rules and DoH bypass blocking.",
        icon = Icons.Default.Public
    ),
    FaqEntry(
        question = "Why are Accessibility and System Overlay permissions required?",
        answer = "System Overlay presents intervention and lockdown screens above active apps. Accessibility monitors only the foreground package name and browser address bar hostnames to enforce excluded app pauses and anti-tamper rules.",
        icon = Icons.Default.Layers
    ),
    FaqEntry(
        question = "When is the Master Key PIN requested?",
        answer = "Increasing protection or turning features up never blocks on a PIN prompt. Actions that weaken or disable defenses—turning Shield OFF, switching to lower power cadence, opening Guardian Settings, or shortening lockdown rules—require Master Key verification once per app session.",
        icon = Icons.Default.Lock
    ),
    FaqEntry(
        question = "How do Bedtime schedules and app time budgets behave?",
        answer = "Configure overnight quiet hours or daily per-app minute caps under Edits or Wellbeing. Exceeded apps are blocked cleanly until the schedule resets or a Guardian authorizes a 15-minute extension.",
        icon = Icons.Default.Bedtime
    ),
    FaqEntry(
        question = "What does Device Administrator / Device Owner add?",
        answer = "Activating Device Admin blocks impulsive uninstallation and force-stop bypass attempts. Device Owner mode further locks package suspension and safe-mode circumvention.",
        icon = Icons.Default.AdminPanelSettings
    ),
    FaqEntry(
        question = "Which open-source license covers Sheildlenz?",
        answer = "Sheildlenz is licensed under the GNU General Public License v3.0 (GPL-3.0) with runtime localization across English, Urdu, Arabic, Spanish, Hindi, and French.",
        icon = Icons.Default.Gavel
    )
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AboutScreen(
    preferences: AegisPreferences
) {
    val context = LocalContext.current
    val masterKeyGate = rememberMasterKeyGate(preferences)
    val isMasterPowerOn by preferences.masterPowerEnabled.collectAsState()
    var expandedFaqIndex by remember { mutableIntStateOf(0) }

    SheildlenzBackdrop(modifier = Modifier.testTag("about_screen_root")) {
        Scaffold(
            contentWindowInsets = WindowInsets.statusBars,
            topBar = {
                CyberTopBar(title = stringResource(R.string.nav_about))
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Minimalist Shutdown Switch Pill/Card (0% Battery / Zero Background Process Switch)
                item {
                    val cardShape = RoundedCornerShape(24.dp)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 560.dp)
                            .shadow(
                                elevation = if (isMasterPowerOn) 8.dp else 4.dp,
                                shape = cardShape,
                                ambientColor = (if (isMasterPowerOn) AegisEmeraldTeal else AegisChampagneGold).copy(alpha = 0.18f),
                                spotColor = (if (isMasterPowerOn) AegisPrimaryCyan else AegisWarningAmber).copy(alpha = 0.16f)
                            )
                            .testTag("master_power_card"),
                        shape = cardShape,
                        color = Color.Transparent,
                        border = BorderStroke(
                            width = 1.4.dp,
                            color = if (isMasterPowerOn) AegisEmeraldBorder else AegisGoldBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = if (isMasterPowerOn) {
                                            listOf(AegisEmeraldSoft, AegisEmeraldSurface)
                                        } else {
                                            listOf(AegisChampagneSoft, AegisGoldDeepTint)
                                        }
                                    )
                                )
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            brush = Brush.linearGradient(
                                                colors = if (isMasterPowerOn) {
                                                    listOf(AegisEmeraldTeal, AegisPrimaryCyan)
                                                } else {
                                                    listOf(AegisChampagneGold, AegisWarningAmber)
                                                }
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PowerSettingsNew,
                                        contentDescription = if (isMasterPowerOn) "Shutdown Switch On" else "Shutdown Switch Off",
                                        tint = AegisOnPrimaryWhite,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Shutdown",
                                        color = AegisTextPrimary,
                                        fontSize = 16.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = ClassicalDisplayFont,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (isMasterPowerOn) {
                                            "On • App running normally"
                                        } else {
                                            "Off • 0% background battery"
                                        },
                                        color = if (isMasterPowerOn) AegisEmeraldTeal else AegisWarningAmber,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = ClassicalBodyFont,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Switch(
                                checked = isMasterPowerOn,
                                onCheckedChange = { targetEnabled ->
                                    if (!targetEnabled) {
                                        masterKeyGate.request("shutdown app to 0% battery") {
                                            MasterPowerController.setMasterPowerEnabled(context, false)
                                            Toast.makeText(
                                                context,
                                                "App shutdown complete (0% background battery).",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    } else {
                                        AegisSoundEffects.playModeOn(context)
                                        MasterPowerController.setMasterPowerEnabled(context, true)
                                        Toast.makeText(
                                            context,
                                            "Sheildlenz powered back ON.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AegisOnPrimaryWhite,
                                    checkedTrackColor = AegisEmeraldTeal,
                                    uncheckedThumbColor = AegisOnPrimaryWhite,
                                    uncheckedTrackColor = AegisWarningAmber.copy(alpha = 0.72f),
                                    uncheckedBorderColor = AegisGoldBorder
                                ),
                                modifier = Modifier.testTag("master_power_switch")
                            )
                        }
                    }
                }

                // 1. Charming Luminous Hero Showcase Card
                item {
                    val heroShape = RoundedCornerShape(30.dp)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 560.dp)
                            .shadow(
                                elevation = 12.dp,
                                shape = heroShape,
                                ambientColor = AegisPrimaryCyan.copy(alpha = 0.18f),
                                spotColor = AegisPurple.copy(alpha = 0.18f)
                            ),
                        shape = heroShape,
                        color = Color.Transparent,
                        border = BorderStroke(
                            width = 1.25.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.96f),
                                    AegisCardBorder.copy(alpha = 0.85f),
                                    AegisLavenderBorder.copy(alpha = 0.80f)
                                )
                            )
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFFFCFEFF),
                                            Color(0xFFF0F6FF),
                                            AegisLavenderSoft
                                        )
                                    )
                                )
                                .drawBehind {
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                AegisElectricBlue.copy(alpha = 0.14f),
                                                Color.Transparent
                                            ),
                                            center = Offset(size.width * 0.18f, size.height * 0.22f),
                                            radius = size.width * 0.55f
                                        ),
                                        radius = size.width * 0.55f,
                                        center = Offset(size.width * 0.18f, size.height * 0.22f)
                                    )
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                AegisGoldBright.copy(alpha = 0.16f),
                                                Color.Transparent
                                            ),
                                            center = Offset(size.width * 0.86f, size.height * 0.78f),
                                            radius = size.width * 0.48f
                                        ),
                                        radius = size.width * 0.48f,
                                        center = Offset(size.width * 0.86f, size.height * 0.78f)
                                    )
                                }
                                .padding(horizontal = 22.dp, vertical = 22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AegisShieldGraphic(
                                isProtected = true,
                                isThreatActive = false,
                                size = 104.dp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Sheildlenz",
                                    color = AegisTextPrimary,
                                    fontSize = 25.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = ClassicalDisplayFont,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = AegisPrimaryCyan.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, AegisPrimaryCyan.copy(alpha = 0.30f))
                                ) {
                                    Text(
                                        text = "v${BuildConfig.VERSION_NAME}",
                                        color = AegisPrimaryCyan,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(5.dp))

                            Text(
                                text = "Real-Time On-Device Visual Shield & Family Safety",
                                color = AegisTextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                textAlign = TextAlign.Center,
                                fontFamily = ClassicalBodyFont
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                AboutFeaturePill(
                                    icon = Icons.Default.Memory,
                                    label = "On-Device AI",
                                    accent = AegisPrimaryCyan,
                                    containerColor = AegisCardBackground
                                )
                                AboutFeaturePill(
                                    icon = Icons.Default.Dns,
                                    label = "Strict Safe DNS",
                                    accent = AegisPurple,
                                    containerColor = AegisLavenderSoft
                                )
                                AboutFeaturePill(
                                    icon = Icons.Default.QrCode2,
                                    label = "QR Guardian Lock",
                                    accent = AegisChampagneGold,
                                    containerColor = AegisChampagneSoft
                                )
                            }

                            HorizontalDivider(
                                color = AegisCardBorder.copy(alpha = 0.55f),
                                modifier = Modifier.padding(vertical = 14.dp)
                            )

                            AhsanTcrBrandingPill()
                        }
                    }
                }

                // 2. Live Release Channel (Update System untouched) & Open Preferences
                item {
                    Column(modifier = Modifier.widthIn(max = 560.dp)) {
                        LuxurySectionHeader(title = "Live Updates", accentColor = AegisPurple)
                        Spacer(modifier = Modifier.height(8.dp))
                        AppUpdateCheckerCard()
                    }
                }

                // 3. Open User Preferences (Sounds & Language — no PIN needed)
                item {
                    Column(
                        modifier = Modifier.widthIn(max = 560.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LuxurySectionHeader(title = "Preferences", accentColor = AegisIndigo)
                        TouchSoundsCard(
                            preferences = preferences,
                            compactPillMode = false
                        )
                        LanguageSelectorSection(
                            preferences = preferences,
                            compactPillMode = false
                        )
                    }
                }

                // 3. Open-Source License & Languages
                item {
                    Column(modifier = Modifier.widthIn(max = 560.dp)) {
                        LuxurySectionHeader(title = "Open Source & Languages", accentColor = AegisChampagneGold)
                        Spacer(modifier = Modifier.height(8.dp))
                        CyberCard(
                            borderColor = AegisLavenderBorder,
                            tone = CyberCardTone.LAVENDER,
                            modifier = Modifier.testTag("about_gpl_license_card")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                brush = Brush.linearGradient(
                                                    colors = listOf(AegisPurple, AegisLavenderLight)
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Gavel,
                                            contentDescription = null,
                                            tint = AegisOnPrimaryWhite,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = stringResource(R.string.about_gpl_title),
                                            color = AegisTextPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = ClassicalDisplayFont
                                        )
                                        Text(
                                            text = "Free, inspectable family protection",
                                            color = AegisTextSecondary,
                                            fontSize = 11.5.sp,
                                            fontFamily = ClassicalBodyFont
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = AegisPurple.copy(alpha = 0.14f),
                                    border = BorderStroke(1.dp, AegisLavenderBorder)
                                ) {
                                    Text(
                                        text = "GPL-3.0",
                                        color = AegisPurple,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Licensed under the GNU General Public License v3.0. All detection, DNS filtering, and QR recovery cryptography run transparently on device.",
                                color = AegisTextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                fontFamily = ClassicalBodyFont
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AboutFeaturePill(
                                    icon = Icons.Default.Code,
                                    label = "github.com/ahsantcr/SheildLenz",
                                    accent = AegisPrimaryCyan,
                                    containerColor = Color.White.copy(alpha = 0.72f)
                                )
                                AboutFeaturePill(
                                    icon = Icons.Default.Translate,
                                    label = "EN • UR • AR • ES • HI • FR",
                                    accent = AegisPurple,
                                    containerColor = Color.White.copy(alpha = 0.72f)
                                )
                            }
                        }
                    }
                }

                // 4. Interactive Guide & Common Questions
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 560.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            LuxurySectionHeader(
                                title = "Common Questions & Guide",
                                accentColor = AegisPrimaryCyan
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = AegisCardBackground,
                            border = BorderStroke(1.dp, AegisCardBorder)
                        ) {
                            Text(
                                text = "${SheildlenzFaqs.size} Topics",
                                color = AegisPrimaryCyan,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = ClassicalBodyFont,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                itemsIndexed(SheildlenzFaqs) { index, faq ->
                    val isExpanded = expandedFaqIndex == index
                    val (cardTone, borderTint, gradientColors, accentColor) = when (index % 3) {
                        0 -> QuadStyle(
                            CyberCardTone.SKY,
                            AegisCardBorder,
                            listOf(AegisPrimaryCyan, AegisElectricBlue),
                            AegisPrimaryCyan
                        )
                        1 -> QuadStyle(
                            CyberCardTone.LAVENDER,
                            AegisLavenderBorder,
                            listOf(AegisPurple, AegisLavenderLight),
                            AegisPurple
                        )
                        else -> QuadStyle(
                            CyberCardTone.GOLD,
                            AegisGoldBorder,
                            listOf(AegisChampagneGold, AegisGoldBright),
                            AegisChampagneGold
                        )
                    }
                    val chevronRotation by animateFloatAsState(
                        targetValue = if (isExpanded) 180f else 0f,
                        animationSpec = tween(240),
                        label = "FaqChevronRotation_$index"
                    )

                    CyberCard(
                        borderColor = borderTint,
                        tone = cardTone,
                        modifier = Modifier
                            .widthIn(max = 560.dp)
                            .testTag("about_faq_item_$index")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    expandedFaqIndex = if (isExpanded) -1 else index
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(brush = Brush.linearGradient(colors = gradientColors)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = faq.icon,
                                        contentDescription = null,
                                        tint = AegisOnPrimaryWhite,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = faq.question,
                                    color = AegisTextPrimary,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = ClassicalDisplayFont,
                                    lineHeight = 18.sp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp)
                                )

                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(accentColor.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ExpandMore,
                                        contentDescription = if (isExpanded) "Collapse section" else "Expand section",
                                        tint = accentColor,
                                        modifier = Modifier
                                            .size(19.dp)
                                            .rotate(chevronRotation)
                                    )
                                }
                            }

                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = fadeIn(tween(220)) + expandVertically(),
                                exit = fadeOut(tween(180)) + shrinkVertically()
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.White.copy(alpha = 0.62f),
                                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.22f))
                                ) {
                                    Text(
                                        text = faq.answer,
                                        color = AegisTextSecondary,
                                        fontSize = 12.5.sp,
                                        lineHeight = 18.5.sp,
                                        fontFamily = ClassicalBodyFont,
                                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp)
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

private data class QuadStyle(
    val tone: CyberCardTone,
    val border: Color,
    val gradient: List<Color>,
    val accent: Color
)

@Composable
private fun AboutFeaturePill(
    icon: ImageVector,
    label: String,
    accent: Color,
    containerColor: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(containerColor)
            .border(1.dp, accent.copy(alpha = 0.30f), RoundedCornerShape(50))
            .padding(horizontal = 11.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            color = AegisTextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = ClassicalBodyFont
        )
    }
}

@Composable
private fun AppUpdateCheckerCard() {
    val context = LocalContext.current
    val isChecking by UpdateChecker.isChecking.collectAsState()
    val updateInfo by UpdateChecker.updateInfo.collectAsState()
    val showUpToDateDialog by UpdateChecker.showUpToDateDialog.collectAsState()
    val checkError by UpdateChecker.checkError.collectAsState()
    val apkDownloadState by UpdateChecker.apkDownloadState.collectAsState()

    val currentLabel = "v${UpdateChecker.cleanVersionTag(BuildConfig.VERSION_NAME)}"
    val latestLabel = updateInfo?.let { info ->
        "v${UpdateChecker.cleanVersionTag(info.latestVersion)}"
    } ?: "Not checked"
    val hasUpdate = updateInfo?.isUpdateAvailable == true
    val isBusyDownloading = apkDownloadState is ApkDownloadState.Downloading ||
        apkDownloadState is ApkDownloadState.Preloading ||
        apkDownloadState is ApkDownloadState.Verifying ||
        apkDownloadState is ApkDownloadState.Installing

    CyberCard(
        borderColor = if (hasUpdate) AegisGoldBorder else AegisLavenderBorder,
        tone = if (hasUpdate) CyberCardTone.GOLD else CyberCardTone.SKY,
        modifier = Modifier.testTag("app_update_checker_card")
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
                        .background(
                            brush = Brush.linearGradient(
                                colors = if (hasUpdate) {
                                    listOf(AegisChampagneGold, AegisGoldBright)
                                } else {
                                    listOf(AegisPrimaryCyan, AegisElectricBlue)
                                }
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (hasUpdate) Icons.Default.Download else Icons.Default.Update,
                        contentDescription = null,
                        tint = AegisOnPrimaryWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (hasUpdate) "Update Available ($latestLabel)" else "Release Channel",
                        color = AegisTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalDisplayFont,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (hasUpdate) "Signed release ready to download" else "Verified over HTTPS with SHA-256",
                        color = if (hasUpdate) AegisChampagneGold else AegisTextSecondary,
                        fontSize = 11.5.sp,
                        fontFamily = ClassicalBodyFont,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (hasUpdate && !isBusyDownloading) {
                IconButton(
                    onClick = { UpdateChecker.startCheckForUpdates(context, showUpToDatePopup = true) },
                    enabled = !isChecking
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Re-check for updates",
                        tint = AegisPurple
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Clean side-by-side version comparison capsules
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, AegisCardBorder.copy(alpha = 0.7f))
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                    Text(
                        text = "INSTALLED",
                        color = AegisTextSecondary,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        fontFamily = ClassicalBodyFont,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = currentLabel,
                        color = AegisTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = if (hasUpdate) AegisChampagneSoft else Color.White.copy(alpha = 0.65f),
                border = BorderStroke(
                    1.dp,
                    if (hasUpdate) AegisGoldBorder else AegisLavenderBorder.copy(alpha = 0.7f)
                )
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                    Text(
                        text = "LATEST RELEASE",
                        color = if (hasUpdate) AegisChampagneGold else AegisTextSecondary,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        fontFamily = ClassicalBodyFont,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = latestLabel,
                        color = if (hasUpdate) AegisChampagneGold else AegisPurple,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        when (val state = apkDownloadState) {
            is ApkDownloadState.Preloading -> {
                Spacer(modifier = Modifier.height(10.dp))
                val pct = state.progress?.let { "${(it * 100).toInt()}%" } ?: "..."
                Text(
                    text = "Background preload • $pct",
                    color = AegisChampagneGold,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalBodyFont
                )
                Spacer(modifier = Modifier.height(5.dp))
                if (state.progress != null) {
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = AegisChampagneGold,
                        trackColor = AegisChampagneGold.copy(alpha = 0.2f),
                        strokeCap = StrokeCap.Round
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = AegisChampagneGold,
                        trackColor = AegisChampagneGold.copy(alpha = 0.2f)
                    )
                }
            }
            is ApkDownloadState.PausedLowBattery -> {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Paused (low battery) • ${UpdateChecker.formatBytesMb(state.bytesDownloaded)} saved",
                    color = AegisChampagneGold,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = ClassicalBodyFont
                )
            }
            is ApkDownloadState.Downloading -> {
                Spacer(modifier = Modifier.height(10.dp))
                val pct = state.progress?.let { "${(it * 100).toInt()}%" } ?: "..."
                Text(
                    text = "Downloading package • $pct",
                    color = AegisPrimaryCyan,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalBodyFont
                )
                Spacer(modifier = Modifier.height(5.dp))
                if (state.progress != null) {
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = AegisPrimaryCyan,
                        trackColor = AegisPrimaryCyan.copy(alpha = 0.2f),
                        strokeCap = StrokeCap.Round
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = AegisPrimaryCyan,
                        trackColor = AegisPrimaryCyan.copy(alpha = 0.2f)
                    )
                }
            }
            ApkDownloadState.Verifying -> {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Verifying SHA-256 signature...",
                    color = AegisPurple,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalBodyFont
                )
                Spacer(modifier = Modifier.height(5.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = AegisPurple,
                    trackColor = AegisPurple.copy(alpha = 0.2f)
                )
            }
            ApkDownloadState.Installing -> {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Installing release package...",
                    color = AegisPurple,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalBodyFont
                )
                Spacer(modifier = Modifier.height(5.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = AegisPurple,
                    trackColor = AegisPurple.copy(alpha = 0.2f)
                )
            }
            is ApkDownloadState.ReadyToInstall -> {
                if (state.needsInstallPermission) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Allow 'Install unknown apps' to apply update.",
                        color = AegisChampagneGold,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = ClassicalBodyFont
                    )
                }
            }
            is ApkDownloadState.SignatureConflict -> {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.reason,
                    color = AegisChampagneGold,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = ClassicalBodyFont
                )
            }
            ApkDownloadState.Installed -> {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = AegisSuccessGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Version current",
                        color = AegisSuccessGreen,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalBodyFont
                    )
                }
            }
            is ApkDownloadState.Failed -> {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = AegisWarningAmber.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, AegisWarningAmber.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = state.message,
                        color = AegisWarningAmber,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = ClassicalBodyFont,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    )
                }
            }
            ApkDownloadState.Idle -> {}
        }

        Spacer(modifier = Modifier.height(12.dp))

        when {
            apkDownloadState is ApkDownloadState.Downloading -> {
                OutlinedButton(
                    onClick = { UpdateChecker.cancelApkDownload(context) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("cancel_update_download_button")
                ) {
                    Text("CANCEL DOWNLOAD", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
            apkDownloadState is ApkDownloadState.Preloading -> {
                Button(
                    onClick = { UpdateChecker.triggerUserUpdateAction(context) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AegisPrimaryCyan,
                        contentColor = AegisOnPrimaryWhite
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("boost_preload_turbo_button")
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(7.dp))
                    Text("DOWNLOAD NOW", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
            apkDownloadState is ApkDownloadState.Verifying -> {
                Button(
                    onClick = {},
                    enabled = false,
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = AegisPurple.copy(alpha = 0.65f),
                        disabledContentColor = AegisOnPrimaryWhite
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(17.dp), color = AegisOnPrimaryWhite, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(7.dp))
                    Text("VERIFYING...", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
            apkDownloadState is ApkDownloadState.Installing -> {
                Button(
                    onClick = {},
                    enabled = false,
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = AegisPurple.copy(alpha = 0.65f),
                        disabledContentColor = AegisOnPrimaryWhite
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(17.dp), color = AegisOnPrimaryWhite, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(7.dp))
                    Text("INSTALLING...", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
            (apkDownloadState as? ApkDownloadState.ReadyToInstall)?.needsInstallPermission == true -> {
                Button(
                    onClick = { UpdateChecker.triggerUserUpdateAction(context) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AegisPurple,
                        contentColor = AegisOnPrimaryWhite
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("grant_install_permission_button")
                ) {
                    Icon(imageVector = Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(7.dp))
                    Text("ALLOW & INSTALL", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
            (apkDownloadState as? ApkDownloadState.ReadyToInstall)?.preloadedReady == true -> {
                Button(
                    onClick = { UpdateChecker.triggerUserUpdateAction(context) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AegisPurple,
                        contentColor = AegisOnPrimaryWhite
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("install_ready_update_button")
                ) {
                    Icon(imageVector = Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(7.dp))
                    Text("INSTALL READY UPDATE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
            apkDownloadState is ApkDownloadState.SignatureConflict -> {
                Button(
                    onClick = { UpdateChecker.triggerCleanUninstallForUpgrade(context) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AegisChampagneGold,
                        contentColor = AegisOnPrimaryWhite
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("clean_upgrade_uninstall_button")
                ) {
                    Icon(imageVector = Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(7.dp))
                    Text("CLEAN UPGRADE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
            hasUpdate -> {
                val info = updateInfo!!
                Button(
                    onClick = { UpdateChecker.triggerUserUpdateAction(context) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AegisChampagneGold,
                        contentColor = AegisOnPrimaryWhite
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("download_install_update_button")
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(7.dp))
                    val sizeHint = if (info.apkSizeBytes > 0L) " (${UpdateChecker.formatBytesMb(info.apkSizeBytes)})" else ""
                    Text("UPDATE TO $latestLabel$sizeHint", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
            else -> {
                Button(
                    onClick = { UpdateChecker.startCheckForUpdates(context, showUpToDatePopup = true) },
                    enabled = !isChecking,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AegisPrimaryCyan,
                        contentColor = AegisOnPrimaryWhite
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("check_updates_button")
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(modifier = Modifier.size(17.dp), color = AegisOnPrimaryWhite, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(7.dp))
                        Text("CHECKING...", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    } else {
                        Icon(imageVector = Icons.Default.Update, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(7.dp))
                        Text("CHECK FOR UPDATES", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        if (checkError != null) {
            Text(
                text = checkError!!,
                color = AegisWarningAmber,
                fontSize = 11.5.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }

    if (showUpToDateDialog) {
        AlertDialog(
            onDismissRequest = { UpdateChecker.dismissUpToDateDialog() },
            containerColor = AegisLavenderSoft,
            title = { Text("Version Current", fontFamily = ClassicalDisplayFont, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Installed: $currentLabel • Remote: $latestLabel\nCurrent build matches the latest release.",
                    fontFamily = ClassicalBodyFont
                )
            },
            confirmButton = {
                Button(onClick = { UpdateChecker.dismissUpToDateDialog() }) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
