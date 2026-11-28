package com.example.ui.blocking

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.models.ThreatCategory
import com.example.data.preferences.AegisPreferences
import com.example.data.preferences.RecoveryQrMode
import com.example.services.protection.AegisLockdownService
import com.example.ui.components.AnimatedCountdownRing
import com.example.ui.components.ExpandableDetailsToggle
import com.example.ui.components.QrCodeImage
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisChampagneSoft
import com.example.ui.theme.AegisDangerRed
import com.example.ui.theme.AegisGoldBorder
import com.example.ui.theme.AegisGoldBright
import com.example.ui.theme.AegisLavenderSoft
import com.example.ui.theme.AegisOnPrimaryWhite
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisPurple
import com.example.ui.theme.AegisTextMuted
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.AegisVoidBackground
import com.example.ui.theme.AegisWarningAmber
import com.example.ui.theme.ClassicalBodyFont
import com.example.ui.theme.ClassicalDisplayFont

@Composable
fun BlockOverlayScreen(
    category: ThreatCategory = ThreatCategory.EXPLICIT_ADULT,
    targetAppName: String = "Active Screen",
    preferences: AegisPreferences,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lockdownMinutes by preferences.lockdownDurationMinutes.collectAsState()
    val doubleTimeMultiplier by preferences.doubleTimeMultiplier.collectAsState()
    val doubleTimeMinutes = remember(lockdownMinutes, doubleTimeMultiplier) {
        (lockdownMinutes * doubleTimeMultiplier).toInt().coerceAtLeast(1)
    }
    val stage1Label = remember(lockdownMinutes) { AegisPreferences.formatMinutesLabel(lockdownMinutes) }
    val doubleTimeLabel = remember(doubleTimeMinutes) { AegisPreferences.formatMinutesLabel(doubleTimeMinutes) }
    val challengeCode by preferences.recoveryChallengeCode.collectAsState()
    val savedEmail by preferences.guardianEmail.collectAsState()
    val discreetMode by preferences.discreetBlockingScreenMode.collectAsState()
    val customHeadline by preferences.blockCustomHeadline.collectAsState()
    val customMessage by preferences.blockCustomMessage.collectAsState()
    val showTimer by preferences.blockShowTimer.collectAsState()
    val showAppBadge by preferences.blockShowAppBadge.collectAsState()
    val showQrUnlock by preferences.blockShowQrUnlock.collectAsState()
    val showQuickActions by preferences.blockShowQuickActions.collectAsState()
    val showReturnHome by preferences.blockShowReturnHome.collectAsState()
    val showEmergencyDialer by preferences.showEmergencyDialerOnLockdown.collectAsState()

    val effectiveReturnHome = showReturnHome || (!showQuickActions && !showEmergencyDialer)

    var showPinDialog by remember { mutableStateOf(false) }
    var showForgotPinQr by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var recoveryCodeInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    BackHandler {
        if (showQuickActions || showQrUnlock) {
            showPinDialog = true
        } else {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(homeIntent)
            onDismiss()
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "WarningPulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    val rootBgBrush = if (discreetMode) {
        Brush.verticalGradient(listOf(Color(0xFFF3F5F8), Color(0xFFE9EEF5)))
    } else {
        Brush.verticalGradient(listOf(AegisVoidBackground, AegisLavenderSoft, AegisChampagneSoft))
    }
    val headingColor = if (discreetMode) Color(0xFF1E293B) else AegisTextPrimary
    val bodyColor = if (discreetMode) Color(0xFF475569) else AegisTextSecondary
    val primaryAccent = if (discreetMode) Color(0xFF334155) else AegisPrimaryCyan
    val alertAccent = if (discreetMode) Color(0xFF475569) else AegisPurple

    val resolvedHeadline = customHeadline.trim().ifBlank {
        if (discreetMode) "Access Paused" else "Content Blocked"
    }
    val resolvedMessage = customMessage.trim().ifBlank {
        if (discreetMode) {
            "This page or application was paused automatically by system display policy rules."
        } else {
            "Visual content matching ${category.displayName} was identified on the active display. The screen has been obscured to uphold protection policies."
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = rootBgBrush)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("block_overlay_screen_root"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (showAppBadge) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (discreetMode) Color(0xFFE2E8F0)
                            else AegisLavenderSoft.copy(alpha = 0.92f * pulseAlpha)
                        )
                        .border(
                            1.dp,
                            if (discreetMode) Color(0xFFCBD5E1)
                            else AegisPurple.copy(alpha = 0.45f * pulseAlpha),
                            RoundedCornerShape(50)
                        )
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                        .testTag("block_screen_app_badge")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (discreetMode) Icons.Default.Lock else Icons.Default.Warning,
                            contentDescription = null,
                            tint = alertAccent,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (discreetMode) "SYSTEM DISPLAY NOTICE" else "SHEILDLENZ INTERVENTION",
                            color = alertAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = ClassicalBodyFont,
                            letterSpacing = 0.9.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }

            // Calm animated countdown / policy ring on the resting surface
            AnimatedCountdownRing(
                progress = if (showTimer) 0.85f else 1f,
                centerText = if (showTimer) stage1Label.uppercase() else "",
                subLabel = if (showTimer) (if (discreetMode) "COOLDOWN" else "SHIELDED") else "",
                primaryColor = AegisPrimaryCyan,
                secondaryColor = AegisPurple,
                accentColor = AegisGoldBright,
                ringSize = 154.dp,
                strokeWidth = 9.dp,
                centerFontSize = 20.sp,
                discreetMode = discreetMode,
                modifier = if (showTimer) Modifier.testTag("block_screen_timer_badge") else Modifier,
                centerIconContent = {
                    Icon(
                        imageVector = if (discreetMode) Icons.Default.Lock else Icons.Default.Shield,
                        contentDescription = if (discreetMode) "Access Paused" else "Sheildlenz Shield Active",
                        tint = if (discreetMode) Color(0xFF334155) else AegisPrimaryCyan,
                        modifier = Modifier.size(if (showTimer) 24.dp else 40.dp)
                    )
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = resolvedHeadline,
                color = headingColor,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ClassicalDisplayFont,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("block_screen_headline")
            )

            if (showAppBadge) {
                Text(
                    text = if (discreetMode) "System display filter active" else category.displayName,
                    color = if (discreetMode) bodyColor else AegisChampagneGold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalBodyFont,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (customMessage.isNotBlank()) {
                Text(
                    text = customMessage.trim(),
                    color = bodyColor,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp,
                    fontFamily = ClassicalBodyFont,
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .testTag("block_screen_message")
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            if (effectiveReturnHome) {
                Button(
                    onClick = {
                        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_HOME)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(homeIntent)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("block_screen_return_home_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryAccent,
                        contentColor = AegisOnPrimaryWhite
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RETURN TO HOME SCREEN",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalBodyFont,
                        letterSpacing = 0.8.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            if (showQuickActions) {
                Button(
                    onClick = {
                        pinInput = ""
                        pinError = null
                        showForgotPinQr = false
                        showPinDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("block_screen_guardian_override_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (discreetMode) Color(0xFFE2E8F0) else AegisLavenderSoft,
                        contentColor = if (discreetMode) Color(0xFF334155) else AegisPurple
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (discreetMode) "ENTER ACCESS CODE" else "MASTER CODE OVERRIDE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalBodyFont,
                        letterSpacing = 0.6.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            if (showEmergencyDialer) {
                OutlinedButton(
                    onClick = {
                        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        runCatching { context.startActivity(dialIntent) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("block_screen_emergency_dialer_button"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White.copy(alpha = 0.78f),
                        contentColor = if (discreetMode) Color(0xFF334155) else AegisWarningAmber
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "EMERGENCY DIALER",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalBodyFont,
                        letterSpacing = 0.6.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // Secondary rule explanations, app target details, and extended timer info tucked behind tap toggle
            ExpandableDetailsToggle(
                summaryLabel = if (discreetMode) "Policy details" else "Why paused & details",
                accentColor = if (discreetMode) Color(0xFF334155) else AegisPurple,
                discreetMode = discreetMode,
                testTagPrefix = "block_screen_details",
                modifier = Modifier.padding(top = 4.dp)
            ) {
                if (customMessage.isBlank()) {
                    Text(
                        text = resolvedMessage,
                        color = bodyColor,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        fontFamily = ClassicalBodyFont,
                        modifier = Modifier.testTag("block_screen_message")
                    )
                }
                if (showAppBadge) {
                    Text(
                        text = if (discreetMode) "STATUS: PAUSED ($targetAppName)" else "TARGET: $targetAppName • DISPLAY SHIELDED",
                        color = if (discreetMode) Color(0xFF64748B) else AegisTextMuted,
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("block_screen_target_label")
                    )
                }
                if (showTimer) {
                    Text(
                        text = if (discreetMode) {
                            "Cooldown: ${stage1Label.uppercase()} • Extended wait: ${doubleTimeLabel.uppercase()}"
                        } else {
                            "Timer: ${stage1Label.uppercase()} Lock or ${doubleTimeLabel.uppercase()} Double-Time Wait • Shield stays ON"
                        },
                        color = primaryAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalBodyFont
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (showPinDialog) {
            val verifyMasterCode: () -> Unit = {
                if (showForgotPinQr && showQrUnlock) {
                    val cleanNewPin = newPinInput.trim()
                    if (cleanNewPin.length !in 4..8 || !cleanNewPin.all { it.isDigit() }) {
                        pinError = "Enter a new 4–8 digit PIN."
                    } else if (preferences.resetPinWithRecoveryToken(recoveryCodeInput, cleanNewPin)) {
                        preferences.setShieldDesiredActive(true)
                        showPinDialog = false
                        showForgotPinQr = false
                        onDismiss()
                    } else {
                        pinError = "Invalid code for Challenge ID $challengeCode."
                    }
                } else {
                    if (preferences.verifyGuardianPin(pinInput)) {
                        preferences.clearLockdown()
                        preferences.setShieldDesiredActive(true)
                        showPinDialog = false
                        onDismiss()
                    } else {
                        pinError = if (discreetMode) {
                            "Incorrect access code."
                        } else {
                            "Incorrect Master Code. Use Double-Time Wait if unknown."
                        }
                        pinInput = ""
                    }
                }
            }
            AlertDialog(
                onDismissRequest = {
                    showPinDialog = false
                    showForgotPinQr = false
                },
                containerColor = if (discreetMode) Color.White else AegisLavenderSoft,
                titleContentColor = headingColor,
                title = {
                    Text(
                        text = when {
                            showForgotPinQr && showQrUnlock -> "Recovery Reset"
                            discreetMode -> "Access Code Required"
                            else -> "Master Code Verification"
                        },
                        fontFamily = ClassicalDisplayFont,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (showForgotPinQr && showQrUnlock) {
                            val pinQrPayload = remember(challengeCode, savedEmail) {
                                preferences.buildRecoveryQrUri(RecoveryQrMode.PIN_RESET)
                            }
                            Text(
                                text = if (discreetMode) {
                                    "Scan with registered recovery device (${savedEmail.ifBlank { "admin email" }})."
                                } else {
                                    "Scan with Sheildlenz on a parent device signed in with ${savedEmail.ifBlank { "your Guardian email" }}."
                                },
                                color = bodyColor,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                textAlign = TextAlign.Center,
                                fontFamily = ClassicalBodyFont
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            QrCodeImage(
                                payload = pinQrPayload,
                                padding = 10.dp,
                                modifier = Modifier
                                    .size(152.dp)
                                    .border(1.2.dp, if (discreetMode) Color(0xFFCBD5E1) else AegisGoldBorder, RoundedCornerShape(14.dp))
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Challenge ID: $challengeCode",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                color = headingColor
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = recoveryCodeInput,
                                onValueChange = {
                                    recoveryCodeInput = it.uppercase()
                                    pinError = null
                                },
                                label = { Text("8-Char One-Time Code") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = newPinInput,
                                onValueChange = {
                                    if (it.length <= 8 && it.all { c -> c.isDigit() }) {
                                        newPinInput = it
                                        pinError = null
                                    }
                                },
                                label = { Text("New PIN (4–8 digits)") },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.NumberPassword,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { verifyMasterCode() }),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            TextButton(
                                onClick = {
                                    showForgotPinQr = false
                                    pinError = null
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Back to PIN entry", fontSize = 11.5.sp)
                            }
                        } else {
                            Text(
                                text = if (discreetMode) {
                                    "Enter the administrator access code to continue, or start the $doubleTimeLabel cooldown timer."
                                } else {
                                    "Enter the Guardian Master Code to continue. If you do not know the Master Code, start a $doubleTimeLabel Double-Time Wait. Sheildlenz Shield remains ON after both."
                                },
                                color = bodyColor,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                fontFamily = ClassicalBodyFont
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = pinInput,
                                onValueChange = {
                                    if (it.length <= 8 && it.all { c -> c.isDigit() }) {
                                        pinInput = it
                                        pinError = null
                                    }
                                },
                                label = { Text(if (discreetMode) "Access Code" else "Master Code / PIN") },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.NumberPassword,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { verifyMasterCode() }),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                isError = pinError != null,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AegisPrimaryCyan,
                                    unfocusedBorderColor = AegisCardBorder,
                                    focusedTextColor = AegisTextPrimary,
                                    unfocusedTextColor = AegisTextPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onPreviewKeyEvent { event ->
                                        if (event.key == Key.Enter || event.key == Key.NumPadEnter) {
                                            if (event.type == KeyEventType.KeyDown) {
                                                verifyMasterCode()
                                            }
                                            true
                                        } else {
                                            false
                                        }
                                    }
                            )
                            if (showQrUnlock) {
                                TextButton(
                                    onClick = {
                                        showForgotPinQr = true
                                        pinError = null
                                    },
                                    modifier = Modifier
                                        .align(Alignment.End)
                                        .testTag("block_screen_forgot_pin_button")
                                ) {
                                    Text("Forgot PIN?", color = AegisPurple, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                            if (showQuickActions) {
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedButton(
                                    onClick = {
                                        preferences.engageFullDeviceLockdown(
                                            appName = targetAppName,
                                            packageName = "",
                                            categoryName = category.displayName,
                                            details = "Double-Time Wait ($doubleTimeLabel) selected because Master Code was unknown."
                                        )
                                        preferences.engageDoubleTimeLockdown()
                                        AegisLockdownService.start(context)
                                        showPinDialog = false
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = AegisChampagneGold.copy(alpha = 0.12f),
                                        contentColor = AegisWarningAmber
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("block_screen_double_time_button")
                                ) {
                                    Text(
                                        text = "DON'T KNOW CODE? WAIT DOUBLE TIME (${doubleTimeLabel.uppercase()})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                        if (pinError != null) {
                            Text(
                                text = pinError!!,
                                color = AegisDangerRed,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = verifyMasterCode,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AegisPrimaryCyan,
                            contentColor = AegisOnPrimaryWhite
                        )
                    ) {
                        Text(
                            text = if (showForgotPinQr && showQrUnlock) "RESET & CONTINUE" else "VERIFY (SHIELD ON)",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            showPinDialog = false
                            showForgotPinQr = false
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AegisTextSecondary)
                    ) {
                        Text("CANCEL")
                    }
                }
            )
        }
    }
}
