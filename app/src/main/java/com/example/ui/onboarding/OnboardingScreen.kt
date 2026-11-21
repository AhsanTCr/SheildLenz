package com.example.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.core.permissions.PermissionHelper
import com.example.data.preferences.AegisPreferences
import com.example.security.deviceowner.AegisDeviceAdminReceiver
import com.example.ui.components.AegisShieldGraphic
import com.example.ui.components.CyberTopBar
import com.example.ui.components.SheildlenzBackdrop
import com.example.ui.theme.AegisCardBackground
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisDangerRed
import com.example.ui.theme.AegisLavenderBorder
import com.example.ui.theme.AegisOnPrimaryWhite
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisPurple
import com.example.ui.theme.AegisSuccessGreen
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.AegisWarningAmber
import com.example.ui.theme.ClassicalBodyFont
import com.example.ui.theme.ClassicalDisplayFont

enum class StepIndex(val title: String) {
    WELCOME("Welcome"),
    GUARDIAN_KEY("Master Key"),
    OVERLAY("Overlay"),
    ACCESSIBILITY("Detection"),
    BATTERY("Reliability"),
    ADMIN("Anti-Tamper"),
    COMPLETE("Ready")
}

@Composable
fun OnboardingScreen(
    preferences: AegisPreferences,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var currentStep by remember { mutableIntStateOf(0) }

    var hasOverlay by remember { mutableStateOf(PermissionHelper.hasOverlayPermission(context)) }
    var hasBatteryOpt by remember { mutableStateOf(PermissionHelper.isIgnoringBatteryOptimizations(context)) }
    var isDeviceAdmin by remember { mutableStateOf(AegisDeviceAdminReceiver.isDeviceAdmin(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlay = PermissionHelper.hasOverlayPermission(context)
                hasBatteryOpt = PermissionHelper.isIgnoringBatteryOptimizations(context)
                isDeviceAdmin = AegisDeviceAdminReceiver.isDeviceAdmin(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    SheildlenzBackdrop(modifier = Modifier.testTag("onboarding_screen_root")) {
        Scaffold(
            contentWindowInsets = WindowInsets.statusBars,
            topBar = {
                CyberTopBar(
                    title = when (currentStep) {
                        0 -> "Welcome"
                        6 -> "Setup Complete"
                        else -> "Setup • Step $currentStep of 5"
                    }
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (currentStep in 1..5) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 500.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 1..5) {
                            val isPassed = i <= currentStep
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        if (isPassed) AegisPrimaryCyan else AegisCardBorder
                                    )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                } else {
                    Spacer(modifier = Modifier.height(10.dp))
                }

                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { it / 2 } + fadeIn()).togetherWith(
                                slideOutHorizontally { -it / 2 } + fadeOut()
                            )
                        } else {
                            (slideInHorizontally { -it / 2 } + fadeIn()).togetherWith(
                                slideOutHorizontally { it / 2 } + fadeOut()
                            )
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    label = "OnboardingStepTransition"
                ) { step ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        when (step) {
                            0 -> WelcomeStepContent(
                                onGetStarted = { currentStep = 1 }
                            )
                            1 -> GuardianKeySetupStepCard(
                                initialEmail = preferences.guardianEmail.value,
                                onBack = { currentStep = 0 },
                                onSaveAndContinue = { pin, email ->
                                    preferences.setGuardianEmail(email)
                                    preferences.setGuardianPin(pin)
                                    currentStep = 2
                                }
                            )
                            2 -> PermissionSingleStepCard(
                                stepTitle = "System Overlay",
                                badgeText = "ESSENTIAL • STEP 2",
                                icon = Icons.Default.Layers,
                                accentColor = AegisPrimaryCyan,
                                isGranted = hasOverlay,
                                explanation = "Required to display the intervention overlay above browsers and apps immediately when blocked visual content is detected.",
                                actionLabel = if (hasOverlay) "Granted ✓" else "Grant Overlay Permission",
                                onAction = {
                                    try {
                                        context.startActivity(PermissionHelper.getOverlaySettingsIntent(context))
                                    } catch (_: Exception) {
                                        context.startActivity(PermissionHelper.getAppDetailsSettingsIntent(context))
                                    }
                                },
                                onNext = { currentStep = 3 },
                                onBack = { currentStep = 1 }
                            )
                            3 -> PermissionSingleStepCard(
                                stepTitle = "Accessibility Sensor",
                                badgeText = "DETECTION • STEP 3",
                                icon = Icons.Default.Visibility,
                                accentColor = AegisPurple,
                                isGranted = false,
                                explanation = "Reads foreground package changes to enforce excluded app rules, SafeSearch/URL checks, and anti-tamper protection.",
                                actionLabel = "Open Accessibility Settings",
                                onAction = {
                                    try {
                                        context.startActivity(PermissionHelper.getAccessibilitySettingsIntent())
                                    } catch (_: Exception) {
                                    }
                                },
                                onNext = { currentStep = 4 },
                                onBack = { currentStep = 2 }
                            )
                            4 -> PermissionSingleStepCard(
                                stepTitle = "Battery Optimization",
                                badgeText = "RELIABILITY • STEP 4",
                                icon = Icons.Default.BatteryChargingFull,
                                accentColor = AegisSuccessGreen,
                                isGranted = hasBatteryOpt,
                                explanation = "Prevents Android background standby policies from terminating protection services during active sessions.",
                                actionLabel = if (hasBatteryOpt) "Excluded ✓" else "Allow Unrestricted",
                                onAction = {
                                    try {
                                        context.startActivity(PermissionHelper.getBatteryOptimizationSettingsIntent(context))
                                    } catch (_: Exception) {
                                        context.startActivity(PermissionHelper.getAppDetailsSettingsIntent(context))
                                    }
                                },
                                onNext = { currentStep = 5 },
                                onBack = { currentStep = 3 }
                            )
                            5 -> PermissionSingleStepCard(
                                stepTitle = "Device Administrator",
                                badgeText = "ANTI-TAMPER • STEP 5",
                                icon = Icons.Default.AdminPanelSettings,
                                accentColor = AegisDangerRed,
                                isGranted = isDeviceAdmin,
                                explanation = "Optional hardening layer that prevents unauthorized deactivation or uninstallation without Guardian authorization.",
                                actionLabel = if (isDeviceAdmin) "Admin Active ✓" else "Activate Device Admin",
                                onAction = {
                                    try {
                                        context.startActivity(AegisDeviceAdminReceiver.createActivateAdminIntent(context))
                                    } catch (_: Exception) {
                                        context.startActivity(PermissionHelper.getAppDetailsSettingsIntent(context))
                                    }
                                },
                                onNext = { currentStep = 6 },
                                onBack = { currentStep = 4 },
                                nextButtonLabel = "FINISH SETUP"
                            )
                            6 -> SetupCompleteContent(
                                onEnterHome = onComplete
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun GuardianKeySetupStepCard(
    initialEmail: String,
    onBack: () -> Unit,
    onSaveAndContinue: (pin: String, email: String) -> Unit
) {
    var pinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var emailInput by remember(initialEmail) { mutableStateOf(initialEmail) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val validateAndProceed: () -> Unit = {
        val cleanPin = pinInput.trim()
        val cleanConfirm = confirmPinInput.trim()
        val cleanEmail = emailInput.trim()
        val validEmail = cleanEmail.contains("@") && cleanEmail.contains(".") && !cleanEmail.contains(" ")
        when {
            cleanPin.length !in 4..8 || !cleanPin.all { it.isDigit() } -> {
                errorMessage = "Enter a 4–8 digit Master Key PIN."
            }
            cleanPin != cleanConfirm -> {
                errorMessage = "Master Key entries do not match."
                confirmPinInput = ""
            }
            !validEmail -> {
                errorMessage = "Enter a valid Guardian Email for QR recovery."
            }
            else -> {
                errorMessage = null
                onSaveAndContinue(cleanPin, cleanEmail)
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 500.dp)
            .shadow(10.dp, RoundedCornerShape(26.dp), ambientColor = AegisChampagneGold.copy(alpha = 0.16f)),
        shape = RoundedCornerShape(26.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.2.dp, AegisChampagneGold.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(AegisChampagneGold.copy(alpha = 0.14f))
                    .border(1.dp, AegisChampagneGold.copy(alpha = 0.38f), RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "REQUIRED • STEP 1",
                    color = AegisChampagneGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    fontFamily = ClassicalBodyFont
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(AegisChampagneGold, AegisPurple)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Set Master Key & Email",
                color = AegisTextPrimary,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ClassicalDisplayFont,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Required before activation. Your Guardian Email authorizes self-service QR recovery if a PIN is forgotten or a lockdown timer needs ending from a parent device.",
                color = AegisTextSecondary,
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                fontFamily = ClassicalBodyFont,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = pinInput,
                onValueChange = {
                    if (it.length <= 8 && it.all { ch -> ch.isDigit() }) {
                        pinInput = it
                        errorMessage = null
                    }
                },
                label = { Text("Master Key PIN (4–8 digits)", fontFamily = ClassicalBodyFont) },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = AegisPurple) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AegisPurple,
                    unfocusedBorderColor = AegisCardBorder,
                    focusedContainerColor = AegisCardBackground,
                    unfocusedContainerColor = AegisCardBackground
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("onboarding_pin_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = confirmPinInput,
                onValueChange = {
                    if (it.length <= 8 && it.all { ch -> ch.isDigit() }) {
                        confirmPinInput = it
                        errorMessage = null
                    }
                },
                label = { Text("Confirm Master Key PIN", fontFamily = ClassicalBodyFont) },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = AegisPurple) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AegisPurple,
                    unfocusedBorderColor = AegisCardBorder,
                    focusedContainerColor = AegisCardBackground,
                    unfocusedContainerColor = AegisCardBackground
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("onboarding_confirm_pin_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = emailInput,
                onValueChange = {
                    emailInput = it
                    errorMessage = null
                },
                label = { Text("Guardian Recovery Email", fontFamily = ClassicalBodyFont) },
                placeholder = { Text("parent@example.com") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = AegisPrimaryCyan) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { validateAndProceed() }),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AegisPrimaryCyan,
                    unfocusedBorderColor = AegisCardBorder,
                    focusedContainerColor = AegisCardBackground,
                    unfocusedContainerColor = AegisCardBackground
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("onboarding_email_input")
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = AegisWarningAmber,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = ClassicalBodyFont,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AegisTextSecondary),
                    modifier = Modifier.height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "BACK",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalBodyFont
                    )
                }

                Button(
                    onClick = validateAndProceed,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AegisPrimaryCyan,
                        contentColor = AegisOnPrimaryWhite
                    ),
                    modifier = Modifier
                        .height(44.dp)
                        .testTag("onboarding_guardian_continue_button")
                ) {
                    Text(
                        text = "SAVE & CONTINUE",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalBodyFont
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeStepContent(
    onGetStarted: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 500.dp)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AegisShieldGraphic(
            isProtected = true,
            isThreatActive = false,
            size = 140.dp
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Welcome to Sheildlenz",
            color = AegisTextPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = ClassicalDisplayFont,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "On-device visual screening, strict Safe DNS filtering,\nand Guardian lock protection.",
            color = AegisTextSecondary,
            fontSize = 13.5.sp,
            lineHeight = 20.sp,
            fontFamily = ClassicalBodyFont,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        HighlightPill(
            icon = Icons.Default.Shield,
            title = "Real-Time Visual Shield",
            subtitle = "Inspects active frames locally across browsers and applications.",
            accentColor = AegisPrimaryCyan
        )

        Spacer(modifier = Modifier.height(10.dp))

        HighlightPill(
            icon = Icons.Default.WifiOff,
            title = "Offline Inference Engine",
            subtitle = "Zero screen frame uploads. All neural classification stays in device RAM.",
            accentColor = AegisPurple
        )

        Spacer(modifier = Modifier.height(10.dp))

        HighlightPill(
            icon = Icons.Default.Lock,
            title = "Guardian Key & QR Recovery",
            subtitle = "Locks critical controls with a PIN backed by parent-authenticated QR recovery.",
            accentColor = AegisChampagneGold
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onGetStarted,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .shadow(8.dp, RoundedCornerShape(16.dp), ambientColor = AegisPrimaryCyan.copy(alpha = 0.25f))
                .testTag("onboarding_get_started_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = AegisPrimaryCyan,
                contentColor = AegisOnPrimaryWhite
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "GET STARTED",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    letterSpacing = 1.sp,
                    fontFamily = ClassicalBodyFont
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun HighlightPill(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFF9FBFF),
        border = androidx.compose.foundation.BorderStroke(1.dp, AegisLavenderBorder.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = AegisTextPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalDisplayFont
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = AegisTextSecondary,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    fontFamily = ClassicalBodyFont
                )
            }
        }
    }
}

@Composable
private fun PermissionSingleStepCard(
    stepTitle: String,
    badgeText: String,
    icon: ImageVector,
    accentColor: Color,
    isGranted: Boolean,
    explanation: String,
    actionLabel: String,
    onAction: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    nextButtonLabel: String = "CONTINUE"
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 500.dp)
            .shadow(10.dp, RoundedCornerShape(26.dp), ambientColor = accentColor.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(26.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.2.dp, accentColor.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(accentColor.copy(alpha = 0.12f))
                    .border(1.dp, accentColor.copy(alpha = 0.32f), RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 5.dp)
            ) {
                Text(
                    text = badgeText,
                    color = accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    fontFamily = ClassicalBodyFont
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(accentColor, accentColor.copy(alpha = 0.75f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stepTitle,
                color = AegisTextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ClassicalDisplayFont,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = explanation,
                color = AegisTextSecondary,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                fontFamily = ClassicalBodyFont,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onAction,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isGranted) AegisSuccessGreen else accentColor,
                    contentColor = AegisOnPrimaryWhite
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (isGranted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = actionLabel.uppercase(),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    fontFamily = ClassicalBodyFont
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = AegisTextSecondary
                    ),
                    modifier = Modifier.height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "BACK",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalBodyFont
                    )
                }

                Button(
                    onClick = onNext,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AegisPrimaryCyan,
                        contentColor = AegisOnPrimaryWhite
                    ),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text(
                        text = nextButtonLabel,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalBodyFont
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SetupCompleteContent(
    onEnterHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 500.dp)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(AegisSuccessGreen.copy(alpha = 0.15f))
                .border(2.dp, AegisSuccessGreen.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = AegisSuccessGreen,
                modifier = Modifier.size(52.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Setup Complete",
            color = AegisTextPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = ClassicalDisplayFont,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Master Key, recovery email, and system permissions are configured. Activate the shield from the home screen.",
            color = AegisTextSecondary,
            fontSize = 13.5.sp,
            lineHeight = 20.sp,
            fontFamily = ClassicalBodyFont,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onEnterHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .shadow(10.dp, RoundedCornerShape(16.dp), ambientColor = AegisPrimaryCyan.copy(alpha = 0.3f))
                .testTag("onboarding_continue_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = AegisPrimaryCyan,
                contentColor = AegisOnPrimaryWhite
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "ENTER SHEILDLENZ HOME",
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                letterSpacing = 1.sp,
                fontFamily = ClassicalBodyFont
            )
        }
    }
}
