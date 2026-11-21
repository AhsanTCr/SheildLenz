package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.AegisPreferences
import com.example.data.preferences.RecoveryQrMode
import com.example.ui.theme.AegisCardBackground
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisChampagneSoft
import com.example.ui.theme.AegisGoldBorder
import com.example.ui.theme.AegisLavenderBorder
import com.example.ui.theme.AegisLavenderSoft
import com.example.ui.theme.AegisOnPrimaryWhite
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisPurple
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.AegisWarningAmber
import com.example.ui.theme.ClassicalBodyFont
import com.example.ui.theme.ClassicalDisplayFont

@Stable
class MasterKeyGateState(
    private val preferences: AegisPreferences
) {
    internal var pendingActionLabel by mutableStateOf<String?>(null)
    private var pendingAction: (() -> Unit)? = null
    private var pendingCancel: (() -> Unit)? = null

    fun request(
        actionLabel: String,
        onCancel: () -> Unit = {},
        onAuthorized: () -> Unit
    ) {
        val hasPin = preferences.hasGuardianPin.value
        val isUnlocked = preferences.isGuardianSessionUnlocked.value
        if (hasPin && isUnlocked) {
            onAuthorized()
            return
        }
        pendingActionLabel = actionLabel
        pendingCancel = onCancel
        pendingAction = onAuthorized
    }

    internal fun dismiss() {
        val cancelCallback = pendingCancel
        pendingActionLabel = null
        pendingAction = null
        pendingCancel = null
        cancelCallback?.invoke()
    }

    internal fun submitKey(keyInput: String, recoveryEmailInput: String = ""): String? {
        val hasPin = preferences.hasGuardianPin.value
        return if (hasPin) {
            if (keyInput.isEmpty() || !preferences.verifyGuardianPin(keyInput)) {
                "Incorrect Master Key"
            } else {
                preferences.unlockGuardianSession()
                val action = pendingAction
                pendingActionLabel = null
                pendingAction = null
                pendingCancel = null
                action?.invoke()
                null
            }
        } else {
            val trimmedEmail = recoveryEmailInput.trim()
            if (keyInput.length < 4) {
                "Master Key must be 4–8 digits"
            } else if (trimmedEmail.isNotEmpty() && (!trimmedEmail.contains("@") || !trimmedEmail.contains("."))) {
                "Enter a valid recovery email or leave blank"
            } else {
                if (trimmedEmail.isNotEmpty()) {
                    preferences.setGuardianEmail(trimmedEmail)
                }
                preferences.setGuardianPin(keyInput)
                preferences.unlockGuardianSession()
                val action = pendingAction
                pendingActionLabel = null
                pendingAction = null
                pendingCancel = null
                action?.invoke()
                null
            }
        }
    }

    internal fun submitQrPinReset(oneTimeCode: String, newPin: String): String? {
        val cleanPin = newPin.trim()
        if (cleanPin.length !in 4..8 || !cleanPin.all { it.isDigit() }) {
            return "Enter a new 4–8 digit Master Key"
        }
        if (!preferences.resetPinWithRecoveryToken(oneTimeCode, cleanPin)) {
            return "Invalid recovery code for Challenge ID ${preferences.getRecoveryChallengeCode()}"
        }
        val action = pendingAction
        pendingActionLabel = null
        pendingAction = null
        pendingCancel = null
        action?.invoke()
        return null
    }
}

@Composable
fun rememberMasterKeyGate(preferences: AegisPreferences): MasterKeyGateState {
    val state = remember(preferences) { MasterKeyGateState(preferences) }
    val hasGuardianPin by preferences.hasGuardianPin.collectAsState()
    val savedEmail by preferences.guardianEmail.collectAsState()
    val challengeCode by preferences.recoveryChallengeCode.collectAsState()
    val actionLabel = state.pendingActionLabel

    if (actionLabel != null) {
        var keyInput by remember(actionLabel) { mutableStateOf("") }
        var emailInput by remember(actionLabel) { mutableStateOf(preferences.guardianEmail.value) }
        var keyError by remember(actionLabel) { mutableStateOf<String?>(null) }
        var showForgotPinQr by remember(actionLabel) { mutableStateOf(false) }
        var recoveryCodeInput by remember(actionLabel) { mutableStateOf("") }
        var newPinInput by remember(actionLabel) { mutableStateOf("") }

        val submitAction: () -> Unit = {
            if (hasGuardianPin && showForgotPinQr) {
                val err = state.submitQrPinReset(recoveryCodeInput, newPinInput)
                if (err != null) {
                    keyError = err
                }
            } else {
                val err = state.submitKey(keyInput, emailInput)
                if (err != null) {
                    keyError = err
                    keyInput = ""
                }
            }
        }

        AlertDialog(
            onDismissRequest = { state.dismiss() },
            containerColor = AegisLavenderSoft,
            titleContentColor = AegisTextPrimary,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(AegisChampagneGold, AegisPurple)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = AegisOnPrimaryWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = when {
                            !hasGuardianPin -> "Create Master Key"
                            showForgotPinQr -> "Forgot PIN • QR Reset"
                            else -> "Master Key Required"
                        },
                        fontFamily = ClassicalDisplayFont,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (hasGuardianPin && showForgotPinQr) {
                        val qrPayload = remember(challengeCode, savedEmail) {
                            preferences.buildRecoveryQrUri(RecoveryQrMode.PIN_RESET)
                        }
                        Text(
                            text = "Scan this QR with Sheildlenz on a parent phone signed in with ${savedEmail.ifBlank { "your registered Guardian email" }}.",
                            color = AegisTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            textAlign = TextAlign.Center,
                            fontFamily = ClassicalBodyFont
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        QrCodeImage(
                            payload = qrPayload,
                            padding = 10.dp,
                            modifier = Modifier
                                .size(156.dp)
                                .border(1.2.dp, AegisGoldBorder, RoundedCornerShape(16.dp))
                                .testTag("master_key_forgot_qr_matrix")
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Challenge ID: $challengeCode",
                            color = AegisTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = recoveryCodeInput,
                            onValueChange = {
                                recoveryCodeInput = it.uppercase()
                                keyError = null
                            },
                            label = { Text("8-Char One-Time Reset Code", fontFamily = ClassicalBodyFont) },
                            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = AegisChampagneGold) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AegisChampagneGold,
                                unfocusedBorderColor = AegisLavenderBorder,
                                focusedContainerColor = AegisCardBackground,
                                unfocusedContainerColor = AegisCardBackground
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("master_key_recovery_code_input")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newPinInput,
                            onValueChange = {
                                if (it.length <= 8 && it.all { ch -> ch.isDigit() }) {
                                    newPinInput = it
                                    keyError = null
                                }
                            },
                            label = { Text("New Master Key (4–8 digits)", fontFamily = ClassicalBodyFont) },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = AegisPrimaryCyan) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { submitAction() }),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AegisPurple,
                                unfocusedBorderColor = AegisLavenderBorder,
                                focusedContainerColor = AegisCardBackground,
                                unfocusedContainerColor = AegisCardBackground
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("master_key_new_pin_input")
                        )
                        TextButton(
                            onClick = {
                                showForgotPinQr = false
                                keyError = null
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Back to PIN entry", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        Text(
                            text = if (hasGuardianPin) {
                                "Enter Master Key to $actionLabel."
                            } else {
                                "Set a 4–8 digit Master Key to $actionLabel."
                            },
                            color = AegisTextSecondary,
                            fontSize = 13.sp,
                            fontFamily = ClassicalBodyFont,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = keyInput,
                            onValueChange = {
                                if (it.length <= 8 && it.all { ch -> ch.isDigit() }) {
                                    keyInput = it
                                    keyError = null
                                }
                            },
                            label = { Text("Master Key (4–8 digits)", fontFamily = ClassicalBodyFont) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { submitAction() }),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            isError = keyError != null,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AegisPurple,
                                unfocusedBorderColor = AegisLavenderBorder,
                                focusedContainerColor = AegisCardBackground,
                                unfocusedContainerColor = AegisCardBackground,
                                focusedTextColor = AegisTextPrimary,
                                unfocusedTextColor = AegisTextPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onPreviewKeyEvent { event ->
                                    if (event.key == Key.Enter || event.key == Key.NumPadEnter) {
                                        if (event.type == KeyEventType.KeyDown) {
                                            submitAction()
                                        }
                                        true
                                    } else {
                                        false
                                    }
                                }
                                .testTag("master_key_prompt_input")
                        )
                        if (hasGuardianPin) {
                            TextButton(
                                onClick = {
                                    showForgotPinQr = true
                                    keyError = null
                                },
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .testTag("master_key_forgot_pin_button")
                            ) {
                                Text(
                                    text = "Forgot PIN?",
                                    color = AegisPurple,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = ClassicalBodyFont
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = {
                                    emailInput = it
                                    keyError = null
                                },
                                label = { Text("Guardian Recovery Email", fontFamily = ClassicalBodyFont) },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { submitAction() }),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AegisPurple,
                                    unfocusedBorderColor = AegisLavenderBorder,
                                    focusedContainerColor = AegisCardBackground,
                                    unfocusedContainerColor = AegisCardBackground,
                                    focusedTextColor = AegisTextPrimary,
                                    unfocusedTextColor = AegisTextPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("master_key_prompt_email_input")
                            )
                        }
                    }

                    if (keyError != null) {
                        Text(
                            text = keyError!!,
                            color = AegisWarningAmber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = ClassicalBodyFont,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = submitAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasGuardianPin && showForgotPinQr) AegisChampagneGold else AegisPurple,
                        contentColor = AegisOnPrimaryWhite
                    ),
                    modifier = Modifier.testTag("master_key_prompt_confirm")
                ) {
                    Text(
                        text = when {
                            !hasGuardianPin -> "SET & APPLY"
                            showForgotPinQr -> "RESET PIN & UNLOCK"
                            else -> "VERIFY"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { state.dismiss() },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = AegisChampagneSoft,
                        contentColor = AegisTextPrimary
                    ),
                    modifier = Modifier.testTag("master_key_prompt_cancel")
                ) {
                    Text("CANCEL", fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    return state
}
