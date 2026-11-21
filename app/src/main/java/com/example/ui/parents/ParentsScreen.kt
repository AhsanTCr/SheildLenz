package com.example.ui.parents

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.core.models.SecurityIncident
import com.example.core.models.SensitivityLevel
import com.example.data.preferences.AegisPreferences
import com.example.data.preferences.RecoveryQrMode
import com.example.data.preferences.RecoveryScanResult
import com.example.data.repository.ChildLiveThumbnail
import com.example.data.repository.PairedChildSummary
import com.example.data.repository.ParentChildRepository
import com.example.data.repository.RemotePolicySettings
import com.example.services.protection.ParentChildSyncController
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberCardTone
import com.example.ui.components.CyberTopBar
import com.example.ui.components.LuxurySectionHeader
import com.example.ui.components.QrCodeImage
import com.example.ui.components.QrScannerDialog
import com.example.ui.components.SheildlenzBackdrop
import com.example.ui.theme.AegisCardBackground
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisChampagneSoft
import com.example.ui.theme.AegisDangerRed
import com.example.ui.theme.AegisElectricBlue
import com.example.ui.theme.AegisGoldBorder
import com.example.ui.theme.AegisLavenderBorder
import com.example.ui.theme.AegisLavenderSoft
import com.example.ui.theme.AegisMintSoft
import com.example.ui.theme.AegisOnPrimaryWhite
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisPurple
import com.example.ui.theme.AegisSkySoft
import com.example.ui.theme.AegisSuccessGreen
import com.example.ui.theme.AegisTextMuted
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.ClassicalBodyFont
import com.example.ui.theme.ClassicalDisplayFont
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ParentsScreen(
    repository: ParentChildRepository,
    preferences: AegisPreferences,
    syncController: ParentChildSyncController? = null,
    pendingPairingCode: String? = null,
    onConsumePendingPairingCode: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentUser by remember {
        mutableStateOf(
            runCatching { Firebase.auth.currentUser }.getOrNull()
        )
    }

    DisposableEffect(Unit) {
        val auth = runCatching { Firebase.auth }.getOrNull()
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        auth?.addAuthStateListener(listener)
        onDispose {
            auth?.removeAuthStateListener(listener)
        }
    }

    val savedRole by preferences.parentalRole.collectAsState()
    val pairedParentUid by preferences.pairedParentUid.collectAsState()
    val pairedParentName by preferences.pairedParentName.collectAsState()
    val savedPairingCode by preferences.childPairingCode.collectAsState()
    val localUninstallLocked by preferences.childUninstallLocked.collectAsState()
    val parentApprovedUntil by preferences.parentApprovedUninstallUntilMillis.collectAsState()
    val liveSeeEnabled by preferences.liveSeeEnabled.collectAsState()
    val sensitivity by preferences.sensitivity.collectAsState()
    val strictEverything by preferences.strictEverything.collectAsState()
    val lockdownMinutes by preferences.lockdownDurationMinutes.collectAsState()
    val antiTamperEnabled by preferences.antiTamperEnabled.collectAsState()

    var isSigningIn by remember { mutableStateOf(false) }
    var isWorking by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var pairingInput by remember { mutableStateOf("") }
    var selectedChildUid by remember { mutableStateOf<String?>(null) }
    var showQrScanner by remember { mutableStateOf(false) }
    var qrScanMode by remember { mutableStateOf("pairing") }
    var recoveryScanOutcome by remember { mutableStateOf<RecoveryScanResult?>(null) }

    LaunchedEffect(pendingPairingCode) {
        if (!pendingPairingCode.isNullOrBlank()) {
            pairingInput = pendingPairingCode
            preferences.setParentalRole("parent")
            onConsumePendingPairingCode()
        }
    }

    if (showQrScanner) {
        val isRecoveryScanner = qrScanMode == "recovery"
        QrScannerDialog(
            onDismiss = { showQrScanner = false },
            onBeforePermissionRequest = { preferences.beginAuthorizedExternalFlow() },
            title = if (isRecoveryScanner) "Scan Recovery QR Code" else "Scan Child QR Code",
            subtitle = if (isRecoveryScanner) {
                "Point camera at the timed-out or Forgot-PIN screen"
            } else {
                "Align pairing QR inside the frame"
            },
            onCodeScanned = { scanned ->
                showQrScanner = false
                if (isRecoveryScanner || AegisPreferences.parseRecoveryQrUri(scanned) != null) {
                    val outcome = AegisPreferences.resolveRecoveryTokenForGuardianScan(
                        rawQrInput = scanned,
                        signedInEmail = currentUser?.email
                    )
                    recoveryScanOutcome = outcome
                    if (outcome is RecoveryScanResult.Success) {
                        Toast.makeText(
                            context,
                            "One-time code ready: ${outcome.oneTimeCode}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    val parsed = ParentChildRepository.extractPairingCode(scanned) ?: scanned
                    pairingInput = parsed
                    Toast.makeText(context, "QR Scanned: $parsed", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    val currentUid = currentUser?.uid
    val pairedChildren by if (currentUid != null && savedRole == "parent") {
        repository.observePairedChildren().collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList()) }
    }

    val childPairingState by if (currentUid != null && savedRole == "child") {
        repository.observeChildOwnPairing().collectAsState(initial = null)
    } else {
        remember { mutableStateOf(null) }
    }

    LaunchedEffect(pairedChildren) {
        if (pairedChildren.isNotEmpty() && pairedChildren.none { it.childUid == selectedChildUid }) {
            selectedChildUid = pairedChildren.first().childUid
        } else if (pairedChildren.isEmpty()) {
            selectedChildUid = null
        }
    }

    val activeChild = pairedChildren.firstOrNull { it.childUid == selectedChildUid } ?: pairedChildren.firstOrNull()
    val childUidForParent = activeChild?.childUid

    val liveThumbnail by if (currentUid != null && savedRole == "parent" && !childUidForParent.isNullOrBlank()) {
        repository.observeLiveThumbnail(childUidForParent).collectAsState(initial = null)
    } else {
        remember { mutableStateOf<ChildLiveThumbnail?>(null) }
    }

    val remoteSettingsForChild by if (currentUid != null && savedRole == "parent" && !childUidForParent.isNullOrBlank()) {
        repository.observeRemoteSettings(childUidForParent).collectAsState(initial = null)
    } else {
        remember { mutableStateOf<RemotePolicySettings?>(null) }
    }

    val remoteThreatLogs by if (currentUid != null && savedRole == "parent" && !childUidForParent.isNullOrBlank()) {
        repository.observeThreatLogsForChild(childUidForParent).collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf<List<SecurityIncident>>(emptyList()) }
    }

    val selectParentRole: () -> Unit = {
        if (preferences.isChildUninstallBlocked() && !pairedParentUid.isNullOrBlank()) {
            Toast.makeText(
                context,
                "Device is locked under Parent Control. Ask your Parent to unlink or allow uninstall.",
                Toast.LENGTH_LONG
            ).show()
        } else {
            preferences.setParentalRole("parent")
            syncController?.stop()
            if (currentUid != null) {
                scope.launch { repository.saveUserProfile(role = "parent") }
            }
        }
    }

    val selectChildRole: () -> Unit = {
        preferences.setParentalRole("child")
        if (currentUid != null) {
            scope.launch {
                isWorking = true
                runCatching {
                    repository.saveUserProfile(role = "child")
                    val existing = savedPairingCode
                    if (existing.isNullOrBlank()) {
                        val code = repository.registerAsChild()
                        preferences.setChildPairingCode(code)
                    }
                    syncController?.ensureStarted()
                }.onFailure { err ->
                    statusMessage = err.localizedMessage ?: "Failed to initialize Child mode."
                }
                isWorking = false
            }
        }
    }

    SheildlenzBackdrop(modifier = Modifier.testTag("parents_screen_root")) {
        Scaffold(
            contentWindowInsets = WindowInsets.statusBars,
            topBar = {
                CyberTopBar(
                    title = "Parents",
                    subtitle = "Parent & Child Guardian Control"
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Account & Sign-In Section (Google Sign-In via Credential Manager)
                item {
                    LuxurySectionHeader(title = "Cloud Guardian Account", accentColor = AegisPrimaryCyan)
                    Spacer(modifier = Modifier.height(6.dp))
                    if (currentUser == null) {
                        CyberCard(
                            borderColor = AegisCardBorder,
                            tone = CyberCardTone.SKY,
                            modifier = Modifier.testTag("parents_signin_card")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(13.dp))
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(AegisPrimaryCyan, AegisElectricBlue)
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SupervisorAccount,
                                        contentDescription = null,
                                        tint = AegisOnPrimaryWhite,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Connect Parent & Child",
                                        color = AegisTextPrimary,
                                        fontSize = 15.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = ClassicalDisplayFont
                                    )
                                    Text(
                                        text = "Sign in with Google to enable live monitoring, Live See screen preview, remote lockdown, and child uninstall protection.",
                                        color = AegisTextSecondary,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp,
                                        fontFamily = ClassicalBodyFont
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    isSigningIn = true
                                    scope.launch {
                                        try {
                                            val webClientResId = context.resources.getIdentifier(
                                                "default_web_client_id",
                                                "string",
                                                context.packageName
                                            )
                                            val clientId = if (webClientResId != 0) {
                                                context.getString(webClientResId)
                                            } else {
                                                ""
                                            }
                                            if (clientId.isBlank()) {
                                                statusMessage = "Google Sign-In client ID not bundled. Add google-services.json before building."
                                            } else {
                                                val signInOption = GetSignInWithGoogleOption.Builder(
                                                    serverClientId = clientId
                                                ).build()
                                                val request = GetCredentialRequest.Builder()
                                                    .addCredentialOption(signInOption)
                                                    .build()
                                                val credentialManager = CredentialManager.create(context)
                                                val activity = context as? Activity
                                                if (activity == null) {
                                                    statusMessage = "Open Parents inside the app window to sign in."
                                                } else {
                                                    val result = credentialManager.getCredential(activity, request)
                                                    val credential = result.credential
                                                    if (credential is CustomCredential &&
                                                        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                                                    ) {
                                                        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                                                        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                                                        Firebase.auth.signInWithCredential(authCredential).await()
                                                        syncController?.ensureStarted()
                                                        Toast.makeText(context, "Signed in as Guardian!", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        } catch (e: GetCredentialCancellationException) {
                                            Log.w("ParentsAuth", "Google Sign-In cancelled: ${e.message}", e)
                                        } catch (e: Exception) {
                                            Log.e("ParentsAuth", "Google Sign-In failed", e)
                                            statusMessage = e.localizedMessage ?: "Google Sign-In failed."
                                        } finally {
                                            isSigningIn = false
                                        }
                                    }
                                },
                                enabled = !isSigningIn,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AegisPrimaryCyan,
                                    contentColor = AegisOnPrimaryWhite
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("parents_google_signin_button")
                            ) {
                                if (isSigningIn) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = AegisOnPrimaryWhite,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = "Sign in with Google",
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = ClassicalBodyFont
                                    )
                                }
                            }
                        }
                    } else {
                        CyberCard(
                            borderColor = AegisCardBorder,
                            tone = CyberCardTone.SKY,
                            modifier = Modifier.testTag("parents_account_banner")
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
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(AegisSuccessGreen.copy(alpha = 0.14f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudDone,
                                            contentDescription = "Connected account",
                                            tint = AegisSuccessGreen,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = currentUser?.displayName?.takeIf { it.isNotBlank() }
                                                ?: "Signed-In Guardian",
                                            color = AegisTextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = ClassicalDisplayFont,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = currentUser?.email?.takeIf { it.isNotBlank() }
                                                ?: "UID: ${currentUser?.uid?.take(10)}…",
                                            color = AegisTextSecondary,
                                            fontSize = 11.sp,
                                            fontFamily = ClassicalBodyFont,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        if (preferences.isChildUninstallBlocked() && !pairedParentUid.isNullOrBlank()) {
                                            Toast.makeText(
                                                context,
                                                "Child Lock active: Parent must unlock or unlink before signing out.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } else {
                                            scope.launch {
                                                withContext(NonCancellable) {
                                                    runCatching {
                                                        CredentialManager.create(context)
                                                            .clearCredentialState(ClearCredentialStateRequest())
                                                    }
                                                    runCatching { Firebase.auth.signOut() }
                                                    syncController?.stop()
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("parents_signout_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = "Sign out Google account",
                                        tint = AegisTextMuted
                                    )
                                }
                            }
                        }
                    }
                }

                // 1b. Self-Service Guardian QR Recovery Scanner (Timer End & Forgot PIN)
                item {
                    LuxurySectionHeader(title = "QR Recovery Scanner", accentColor = AegisChampagneGold)
                    Spacer(modifier = Modifier.height(6.dp))
                    CyberCard(
                        borderColor = AegisGoldBorder,
                        tone = CyberCardTone.GOLD,
                        modifier = Modifier.testTag("guardian_qr_recovery_card")
                    ) {
                        Text(
                            text = "Scan Recovery QR from Another Device",
                            color = AegisTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = ClassicalDisplayFont
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Sign in with the same Guardian email registered at first boot, then scan the QR shown on a timed-out screen (ends timer only) or a Forgot-PIN prompt.",
                            color = AegisTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.5.sp,
                            fontFamily = ClassicalBodyFont
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                if (currentUser == null) {
                                    recoveryScanOutcome = RecoveryScanResult.RequiresGoogleSignIn
                                    Toast.makeText(
                                        context,
                                        "Sign in with your Guardian Google account above first.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    qrScanMode = "recovery"
                                    showQrScanner = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AegisChampagneGold,
                                contentColor = AegisOnPrimaryWhite
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("scan_recovery_qr_button")
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SCAN TIMEOUT / FORGOT-PIN QR", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }

                        when (val outcome = recoveryScanOutcome) {
                            is RecoveryScanResult.Success -> {
                                Spacer(modifier = Modifier.height(12.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(AegisCardBackground)
                                        .border(1.2.dp, AegisSuccessGreen.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                        .padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = if (outcome.mode == RecoveryQrMode.TIMER_END) {
                                            "TIMER END ONLY • SHIELD STAYS ON"
                                        } else {
                                            "MASTER KEY PIN RESET"
                                        },
                                        color = AegisSuccessGreen,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp,
                                        fontFamily = ClassicalBodyFont
                                    )
                                    Text(
                                        text = outcome.oneTimeCode,
                                        color = AegisTextPrimary,
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 4.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.testTag("recovery_generated_code_text")
                                    )
                                    Text(
                                        text = "Challenge: ${outcome.challengeCode} • Single-use code",
                                        color = AegisTextSecondary,
                                        fontSize = 11.sp,
                                        fontFamily = ClassicalBodyFont
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                            cm?.setPrimaryClip(ClipData.newPlainText("Sheildlenz One-Time Code", outcome.oneTimeCode))
                                            Toast.makeText(context, "Copied ${outcome.oneTimeCode}", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(42.dp)
                                            .testTag("copy_recovery_code_button")
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("COPY ONE-TIME CODE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            RecoveryScanResult.RequiresGoogleSignIn -> {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Sign in with Google above using your registered Guardian email to authorize QR recovery.",
                                    color = AegisDangerRed,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = ClassicalBodyFont
                                )
                            }
                            is RecoveryScanResult.EmailMismatch -> {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Account mismatch: signed in as ${outcome.signedInEmail}, which does not match the Guardian email configured at first boot on the target device.",
                                    color = AegisDangerRed,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = ClassicalBodyFont
                                )
                            }
                            RecoveryScanResult.InvalidQr -> {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Unrecognized recovery QR code.",
                                    color = AegisDangerRed,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = ClassicalBodyFont
                                )
                            }
                            null -> Unit
                        }
                    }
                }

                // 2. Dual Connection Options: Connect as Parent AND Connect as Child
                item {
                    LuxurySectionHeader(title = "Connection Role", accentColor = AegisPurple)
                    Spacer(modifier = Modifier.height(6.dp))
                    CyberCard(
                        borderColor = AegisLavenderBorder,
                        tone = CyberCardTone.LAVENDER,
                        modifier = Modifier.testTag("parents_role_selector_card")
                    ) {
                        Text(
                            text = "Choose how this phone connects",
                            color = AegisTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = ClassicalDisplayFont
                        )
                        Text(
                            text = "Connect one device as Parent to monitor, view Live See, configure rules, and lock/uninstall; connect the other device as Child.",
                            color = AegisTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            fontFamily = ClassicalBodyFont
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RoleConnectionOptionTile(
                                title = "As Parent",
                                subtitle = "Monitor & Live See\nControl & Lock Child",
                                icon = Icons.Default.Person,
                                isSelected = savedRole == "parent",
                                accentColor = AegisPrimaryCyan,
                                onClick = selectParentRole,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("connect_as_parent_button")
                            )

                            RoleConnectionOptionTile(
                                title = "As Child",
                                subtitle = "Protected Device\nUninstall Locked",
                                icon = Icons.Default.ChildCare,
                                isSelected = savedRole == "child",
                                accentColor = AegisChampagneGold,
                                onClick = selectChildRole,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("connect_as_child_button")
                            )
                        }

                        if (!statusMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = statusMessage!!,
                                color = AegisPurple,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = ClassicalBodyFont
                            )
                        }
                    }
                }

                // 3. Mode-Specific Workflows
                if (savedRole == "parent") {
                    // Pair Child Card (6-digit code OR scanned QR URL)
                    item {
                        LuxurySectionHeader(title = "Pair Child Device", accentColor = AegisPrimaryCyan)
                        Spacer(modifier = Modifier.height(6.dp))
                        CyberCard(
                            borderColor = AegisCardBorder,
                            tone = CyberCardTone.SKY,
                            modifier = Modifier.testTag("parent_pair_child_card")
                        ) {
                            Text(
                                text = "Scan Child QR Code with your Camera or enter the 6-digit Pairing Code shown on the Child phone.",
                                color = AegisTextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                fontFamily = ClassicalBodyFont
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = pairingInput,
                                onValueChange = { pairingInput = it },
                                label = { Text("6-Digit Code or QR Link") },
                                placeholder = { Text("e.g. 482910") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("parent_pairing_code_input")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        qrScanMode = "pairing"
                                        showQrScanner = true
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("scan_child_qr_button")
                                ) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Child QR", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("SCAN QR", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        val clipText = cm?.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                        val parsed = ParentChildRepository.extractPairingCode(clipText)
                                        if (parsed != null) {
                                            pairingInput = parsed
                                        } else {
                                            Toast.makeText(context, "No 6-digit code in clipboard", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = "Paste code", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("PASTE", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    if (currentUser == null) {
                                        Toast.makeText(context, "Please sign in with Google above first.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    isWorking = true
                                    scope.launch {
                                        val result = repository.pairWithChildByCode(pairingInput)
                                        result.onSuccess { paired ->
                                            selectedChildUid = paired.childUid
                                            pairingInput = ""
                                            statusMessage = "Paired with ${paired.childDisplayName}!"
                                            Toast.makeText(context, statusMessage, Toast.LENGTH_SHORT).show()
                                        }.onFailure { err ->
                                            statusMessage = err.localizedMessage ?: "Pairing failed."
                                            Toast.makeText(context, statusMessage, Toast.LENGTH_LONG).show()
                                        }
                                        isWorking = false
                                    }
                                },
                                enabled = !isWorking && pairingInput.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AegisPrimaryCyan,
                                    contentColor = AegisOnPrimaryWhite
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("pair_child_button")
                            ) {
                                Text("PAIR CHILD DEVICE", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Paired Children Strip + Remote Controls
                    if (pairedChildren.isNotEmpty() && activeChild != null) {
                        item {
                            LuxurySectionHeader(title = "Family Dashboard", accentColor = AegisPrimaryCyan)
                            Spacer(modifier = Modifier.height(6.dp))
                            CyberCard(
                                borderColor = AegisCardBorder,
                                tone = CyberCardTone.SKY,
                                modifier = Modifier.testTag("parent_family_overview_card")
                            ) {
                                Text(
                                    text = "${pairedChildren.size} Paired Child ${if (pairedChildren.size == 1) "Device" else "Devices"}",
                                    color = AegisTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = ClassicalDisplayFont
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Active child: ${activeChild.childDisplayName} • Tap a child chip below to manage warnings, bedtime lock, Safe DNS, and budgets.",
                                    color = AegisTextSecondary,
                                    fontSize = 11.5.sp,
                                    fontFamily = ClassicalBodyFont
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            LuxurySectionHeader(title = "Linked Child Devices (${pairedChildren.size})", accentColor = AegisChampagneGold)
                            Spacer(modifier = Modifier.height(6.dp))
                            CyberCard(
                                borderColor = AegisGoldBorder,
                                tone = CyberCardTone.GOLD,
                                modifier = Modifier.testTag("parent_children_list_card")
                            ) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    pairedChildren.forEach { child ->
                                        val isCurrent = child.childUid == activeChild.childUid
                                        FilterChip(
                                            selected = isCurrent,
                                            onClick = { selectedChildUid = child.childUid },
                                            label = {
                                                Text(
                                                    text = child.childDisplayName,
                                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.ChildCare,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = AegisPrimaryCyan,
                                                selectedLabelColor = AegisOnPrimaryWhite,
                                                selectedLeadingIconColor = AegisOnPrimaryWhite
                                            ),
                                            modifier = Modifier.height(40.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Monitoring: ${activeChild.childDisplayName} • Code ${activeChild.pairingCode}",
                                        color = AegisTextSecondary,
                                        fontSize = 11.5.sp,
                                        fontFamily = ClassicalBodyFont
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            scope.launch {
                                                repository.unlinkChild(activeChild.childUid)
                                                Toast.makeText(context, "Unlinked ${activeChild.childDisplayName}", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Icon(Icons.Default.LinkOff, contentDescription = "Unlink child", modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("UNLINK", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Live See Screen Preview Card
                        item {
                            LuxurySectionHeader(title = "Live See • Child Screen", accentColor = AegisPrimaryCyan)
                            Spacer(modifier = Modifier.height(6.dp))
                            ParentLiveSeeCard(
                                thumbnail = liveThumbnail,
                                liveSeeEnabled = remoteSettingsForChild?.liveSeeEnabled ?: true,
                                onToggleLiveSee = { enabled ->
                                    val base = remoteSettingsForChild ?: RemotePolicySettings()
                                    scope.launch {
                                        repository.pushRemoteSettings(activeChild.childUid, base.copy(liveSeeEnabled = enabled))
                                    }
                                },
                                onRequestFreshFrame = {
                                    scope.launch {
                                        repository.sendRemoteCommand(
                                            activeChild.childUid,
                                            ParentChildRepository.COMMAND_REQUEST_LIVE_FRAME
                                        )
                                    }
                                }
                            )
                        }

                        // Remote Lock & Child Uninstall Control Card
                        item {
                            LuxurySectionHeader(title = "Remote Lock & Uninstall Control", accentColor = AegisDangerRed)
                            Spacer(modifier = Modifier.height(6.dp))
                            ParentRemoteActionsCard(
                                childName = activeChild.childDisplayName,
                                childUninstallLocked = remoteSettingsForChild?.childUninstallLocked ?: true,
                                onLockNow = {
                                    scope.launch {
                                        val ok = repository.sendRemoteCommand(
                                            activeChild.childUid,
                                            ParentChildRepository.COMMAND_LOCKDOWN
                                        )
                                        Toast.makeText(
                                            context,
                                            if (ok) "Remote Lockdown sent to ${activeChild.childDisplayName}" else "Failed to send command",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                onUnlockNow = {
                                    scope.launch {
                                        val ok = repository.sendRemoteCommand(
                                            activeChild.childUid,
                                            ParentChildRepository.COMMAND_UNLOCK
                                        )
                                        Toast.makeText(
                                            context,
                                            if (ok) "Remote Unlock sent to ${activeChild.childDisplayName}" else "Failed to send command",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                onToggleUninstallLock = { lock ->
                                    val base = remoteSettingsForChild ?: RemotePolicySettings()
                                    scope.launch {
                                        repository.pushRemoteSettings(
                                            activeChild.childUid,
                                            base.copy(childUninstallLocked = lock)
                                        )
                                        repository.sendRemoteCommand(
                                            activeChild.childUid,
                                            if (lock) ParentChildRepository.COMMAND_LOCK_UNINSTALL else ParentChildRepository.COMMAND_ALLOW_UNINSTALL
                                        )
                                    }
                                },
                                onAuthorizeParentUninstall = {
                                    scope.launch {
                                        val ok = repository.sendRemoteCommand(
                                            activeChild.childUid,
                                            ParentChildRepository.COMMAND_ALLOW_UNINSTALL
                                        )
                                        Toast.makeText(
                                            context,
                                            if (ok) "Parent authorized uninstall on ${activeChild.childDisplayName} for 5 min" else "Command failed",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            )
                        }

                        // Remote Policy & Settings Control Card
                        item {
                            LuxurySectionHeader(title = "Remote Policy Control", accentColor = AegisPurple)
                            Spacer(modifier = Modifier.height(6.dp))
                            ParentRemoteSettingsCard(
                                currentRemote = remoteSettingsForChild ?: RemotePolicySettings(),
                                onPushSettings = { updated ->
                                    scope.launch {
                                        val ok = repository.pushRemoteSettings(activeChild.childUid, updated)
                                        Toast.makeText(
                                            context,
                                            if (ok) "Settings synced to ${activeChild.childDisplayName}" else "Sync failed",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            )
                        }

                        // Real-Time Threat Logs
                        item {
                            LuxurySectionHeader(title = "Child Real-Time Threat Feed", accentColor = AegisChampagneGold)
                        }

                        if (remoteThreatLogs.isEmpty()) {
                            item {
                                CyberCard(
                                    borderColor = AegisCardBorder,
                                    tone = CyberCardTone.SKY,
                                    modifier = Modifier.testTag("parent_threat_logs_card")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AegisSuccessGreen)
                                        Text(
                                            text = "No incidents reported from ${activeChild.childDisplayName}. Real-time threat alerts will appear here immediately.",
                                            color = AegisTextSecondary,
                                            fontSize = 12.sp,
                                            fontFamily = ClassicalBodyFont
                                        )
                                    }
                                }
                            }
                        } else {
                            items(remoteThreatLogs) { incident ->
                                ParentThreatIncidentRow(incident)
                            }
                        }
                    }
                } else if (savedRole == "child") {
                    val activeCode = savedPairingCode ?: childPairingState?.pairingCode ?: "------"
                    val isPaired = (childPairingState?.active == true && !childPairingState?.parentId.isNullOrBlank()) ||
                        !pairedParentUid.isNullOrBlank()
                    val parentLabel = childPairingState?.parentDisplayName?.takeIf { it.isNotBlank() }
                        ?: pairedParentName
                        ?: "Parent Guardian"
                    val uninstallBlocked = localUninstallLocked && !(parentApprovedUntil > System.currentTimeMillis())

                    // Child QR Code & 6-Digit Pairing Card
                    item {
                        LuxurySectionHeader(title = "Child Pairing QR & Code", accentColor = AegisChampagneGold)
                        Spacer(modifier = Modifier.height(6.dp))
                        CyberCard(
                            borderColor = AegisGoldBorder,
                            tone = CyberCardTone.GOLD,
                            modifier = Modifier.testTag("child_pairing_qr_card")
                        ) {
                            Text(
                                text = "Ask Parent to scan this QR code or enter the 6-digit code in their Parents tab:",
                                color = AegisTextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                fontFamily = ClassicalBodyFont
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                val qrPayload = ParentChildRepository.buildPairingUri(
                                    if (activeCode == "------") "000000" else activeCode
                                )
                                QrCodeImage(
                                    payload = qrPayload,
                                    modifier = Modifier
                                        .size(180.dp)
                                        .border(1.5.dp, AegisGoldBorder, RoundedCornerShape(16.dp))
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = activeCode,
                                color = AegisPrimaryCyan,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 6.sp,
                                fontFamily = ClassicalDisplayFont,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("child_pairing_code_text")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        cm?.setPrimaryClip(ClipData.newPlainText("Sheildlenz Pairing Code", activeCode))
                                        Toast.makeText(context, "Copied code $activeCode", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text("COPY", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "Pair with my Child device on Sheildlenz: ${ParentChildRepository.buildPairingUri(activeCode)} (Code: $activeCode)"
                                            )
                                        }
                                        preferences.beginAuthorizedExternalFlow()
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Pairing Code"))
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text("SHARE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        if (currentUser == null) {
                                            Toast.makeText(context, "Sign in with Google above to generate a cloud code.", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        scope.launch {
                                            isWorking = true
                                            runCatching {
                                                val newCode = repository.registerAsChild()
                                                preferences.setChildPairingCode(newCode)
                                                syncController?.ensureStarted()
                                            }.onFailure { err ->
                                                Toast.makeText(context, err.localizedMessage ?: "Code refresh failed", Toast.LENGTH_SHORT).show()
                                            }
                                            isWorking = false
                                        }
                                    },
                                    enabled = !isWorking,
                                    colors = ButtonDefaults.buttonColors(containerColor = AegisPrimaryCyan),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("child_regenerate_code_button")
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("NEW", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Child Guardian Status & Uninstall Lock Card
                    item {
                        LuxurySectionHeader(title = "Child Guardian Protection", accentColor = AegisPurple)
                        Spacer(modifier = Modifier.height(6.dp))
                        CyberCard(
                            borderColor = if (uninstallBlocked) AegisDangerRed.copy(alpha = 0.45f) else AegisCardBorder,
                            tone = if (uninstallBlocked) CyberCardTone.LAVENDER else CyberCardTone.SKY,
                            modifier = Modifier.testTag("child_connection_status_card")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isPaired) Icons.Default.AdminPanelSettings else Icons.Default.QrCode2,
                                        contentDescription = null,
                                        tint = if (isPaired) AegisSuccessGreen else AegisChampagneGold,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (isPaired) "Connected to $parentLabel" else "Waiting for Parent Connection",
                                            color = AegisTextPrimary,
                                            fontSize = 14.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = ClassicalDisplayFont
                                        )
                                        Text(
                                            text = if (isPaired) "Remote policy & Live See synced" else "Share QR or 6-digit code above",
                                            color = AegisTextSecondary,
                                            fontSize = 11.5.sp,
                                            fontFamily = ClassicalBodyFont
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Uninstall Protection Status Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (uninstallBlocked) AegisChampagneSoft else AegisSuccessGreen.copy(alpha = 0.12f))
                                    .border(
                                        1.dp,
                                        if (uninstallBlocked) AegisDangerRed.copy(alpha = 0.35f) else AegisSuccessGreen.copy(alpha = 0.35f),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (uninstallBlocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = null,
                                    tint = if (uninstallBlocked) AegisDangerRed else AegisSuccessGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (uninstallBlocked) {
                                            "Uninstall Blocked (Child Mode)"
                                        } else {
                                            "Parent Authorized Uninstall Active"
                                        },
                                        color = AegisTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = ClassicalDisplayFont
                                    )
                                    Text(
                                        text = if (uninstallBlocked) {
                                            "Only your Parent can allow uninstalling or deactivating Sheildlenz from their Parent device."
                                        } else {
                                            "Parent approved temporary uninstall permission on this device."
                                        },
                                        color = AegisTextSecondary,
                                        fontSize = 11.5.sp,
                                        fontFamily = ClassicalBodyFont
                                    )
                                }
                            }

                            AnimatedVisibility(visible = !uninstallBlocked) {
                                Column {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            preferences.grantTemporarySettingsAuthorization(5)
                                            val intent = Intent(Intent.ACTION_DELETE).apply {
                                                data = Uri.parse("package:${context.packageName}")
                                            }
                                            context.startActivity(intent)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = AegisDangerRed),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("child_parent_authorized_uninstall_button")
                                    ) {
                                        Icon(Icons.Default.DeleteForever, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("UNINSTALL SHEILDLENZ (PARENT APPROVED)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PolicyStatusChip(
                                    label = "Sensitivity: ${sensitivity.displayName}",
                                    color = AegisPrimaryCyan
                                )
                                PolicyStatusChip(
                                    label = "Strict: ${if (strictEverything) "ON" else "OFF"}",
                                    color = AegisPurple
                                )
                                PolicyStatusChip(
                                    label = "Lockdown: ${AegisPreferences.formatMinutesLabel(lockdownMinutes)}",
                                    color = AegisChampagneGold
                                )
                                PolicyStatusChip(
                                    label = "Live See: ${if (liveSeeEnabled) "ON" else "OFF"}",
                                    color = if (liveSeeEnabled) AegisSuccessGreen else AegisTextMuted
                                )
                                PolicyStatusChip(
                                    label = "Anti-Tamper: ${if (antiTamperEnabled) "ON" else "OFF"}",
                                    color = AegisElectricBlue
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleConnectionOptionTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        onClick = onClick,
        shape = shape,
        color = if (isSelected) accentColor.copy(alpha = 0.12f) else AegisCardBackground,
        border = BorderStroke(
            width = if (isSelected) 1.6.dp else 1.dp,
            color = if (isSelected) accentColor else AegisCardBorder
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(accentColor)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "ACTIVE",
                            color = AegisOnPrimaryWhite,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                color = AegisTextPrimary,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ClassicalDisplayFont
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = AegisTextSecondary,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontFamily = ClassicalBodyFont
            )
        }
    }
}

@Composable
private fun ParentLiveSeeCard(
    thumbnail: ChildLiveThumbnail?,
    liveSeeEnabled: Boolean,
    onToggleLiveSee: (Boolean) -> Unit,
    onRequestFreshFrame: () -> Unit
) {
    val decodedBitmap = remember(thumbnail?.thumbnailBase64) {
        val raw = thumbnail?.thumbnailBase64
        if (raw.isNullOrBlank()) null else {
            runCatching {
                val bytes = Base64.decode(raw, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }.getOrNull()
        }
    }

    CyberCard(
        borderColor = AegisCardBorder,
        tone = CyberCardTone.SKY,
        modifier = Modifier.testTag("parent_live_see_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Visibility, contentDescription = null, tint = AegisPrimaryCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Live Screen View",
                        color = AegisTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalDisplayFont
                    )
                    val timeLabel = if (thumbnail != null && thumbnail.timestamp > 0L) {
                        val fmt = SimpleDateFormat("hh:mm:ss a", Locale.US).format(Date(thumbnail.timestamp))
                        "${thumbnail.activeAppName.ifBlank { "Screen" }} • $fmt"
                    } else {
                        "Streams periodic screen frames from Child"
                    }
                    Text(
                        text = timeLabel,
                        color = AegisTextSecondary,
                        fontSize = 11.sp,
                        fontFamily = ClassicalBodyFont
                    )
                }
            }

            Switch(
                checked = liveSeeEnabled,
                onCheckedChange = onToggleLiveSee,
                colors = SwitchDefaults.colors(checkedThumbColor = AegisOnPrimaryWhite, checkedTrackColor = AegisPrimaryCyan)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (decodedBitmap != null) {
            Image(
                bitmap = decodedBitmap,
                contentDescription = "Child device live screen thumbnail",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 14f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0E1420))
                    .border(1.dp, AegisCardBorder, RoundedCornerShape(14.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AegisSkySoft)
                    .border(1.dp, AegisCardBorder, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Default.Visibility, contentDescription = null, tint = AegisPrimaryCyan, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Waiting for Child Screen Shield frame…",
                        color = AegisTextPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = ClassicalDisplayFont,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Thumbnails update every few seconds when Child protection is armed and Live See is ON.",
                        color = AegisTextSecondary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        fontFamily = ClassicalBodyFont
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onRequestFreshFrame,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(44.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("PING CHILD FOR FRESH SNAPSHOT", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ParentRemoteActionsCard(
    childName: String,
    childUninstallLocked: Boolean,
    onLockNow: () -> Unit,
    onUnlockNow: () -> Unit,
    onToggleUninstallLock: (Boolean) -> Unit,
    onAuthorizeParentUninstall: () -> Unit
) {
    CyberCard(
        borderColor = AegisDangerRed.copy(alpha = 0.35f),
        tone = CyberCardTone.LAVENDER,
        modifier = Modifier.testTag("parent_remote_actions_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onLockNow,
                colors = ButtonDefaults.buttonColors(containerColor = AegisDangerRed, contentColor = AegisOnPrimaryWhite),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("remote_lock_child_button")
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("LOCK CHILD", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onUnlockNow,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("remote_unlock_child_button")
            ) {
                Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("UNLOCK CHILD", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.7f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = "Prevent Child Uninstall",
                    color = AegisTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalDisplayFont
                )
                Text(
                    text = "Child cannot uninstall or disable admin on $childName",
                    color = AegisTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = ClassicalBodyFont
                )
            }
            Switch(
                checked = childUninstallLocked,
                onCheckedChange = onToggleUninstallLock,
                colors = SwitchDefaults.colors(checkedThumbColor = AegisOnPrimaryWhite, checkedTrackColor = AegisPurple)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = onAuthorizeParentUninstall,
            colors = ButtonDefaults.buttonColors(containerColor = AegisPurple, contentColor = AegisOnPrimaryWhite),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("remote_allow_uninstall_button")
        ) {
            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("ALLOW PARENT UNINSTALL ON CHILD (5 MIN)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ParentRemoteSettingsCard(
    currentRemote: RemotePolicySettings,
    onPushSettings: (RemotePolicySettings) -> Unit
) {
    var sensitivity by remember(currentRemote) { mutableStateOf(currentRemote.sensitivity) }
    var strictEverything by remember(currentRemote) { mutableStateOf(currentRemote.strictEverything) }
    var antiTamper by remember(currentRemote) { mutableStateOf(currentRemote.antiTamperEnabled) }
    var liveSee by remember(currentRemote) { mutableStateOf(currentRemote.liveSeeEnabled) }
    var durationMinutes by remember(currentRemote) { mutableIntStateOf(currentRemote.lockdownDurationMinutes) }
    var warningThreshold by remember(currentRemote) { mutableIntStateOf(currentRemote.warningThresholdCount) }
    var blockExplicit by remember(currentRemote) { mutableStateOf(currentRemote.blockExplicitAdult) }
    var blockSuggestive by remember(currentRemote) { mutableStateOf(currentRemote.blockSuggestiveContent) }
    var blockGraphic by remember(currentRemote) { mutableStateOf(currentRemote.blockSensitiveGraphic) }
    var blockGambling by remember(currentRemote) { mutableStateOf(currentRemote.blockGamblingKeywords) }
    var bedtimeEnabled by remember(currentRemote) { mutableStateOf(currentRemote.bedtimeEnabled) }
    var bedtimeStartMinutes by remember(currentRemote) { mutableIntStateOf(currentRemote.bedtimeStartMinutes) }
    var bedtimeEndMinutes by remember(currentRemote) { mutableIntStateOf(currentRemote.bedtimeEndMinutes) }
    var safeDnsEnabled by remember(currentRemote) { mutableStateOf(currentRemote.safeDnsEnabled) }
    var domainsCsv by remember(currentRemote) { mutableStateOf(currentRemote.customBlockedDomains.joinToString(", ")) }
    var keywordsCsv by remember(currentRemote) { mutableStateOf(currentRemote.customBlockedKeywords.joinToString(", ")) }
    var budgetsCsv by remember(currentRemote) {
        mutableStateOf(currentRemote.appTimeBudgets.entries.joinToString(", ") { "${it.key}=${it.value}" })
    }
    var whitelistCsv by remember(currentRemote) {
        mutableStateOf(currentRemote.whitelistedPackages.joinToString(", "))
    }

    CyberCard(
        borderColor = AegisLavenderBorder,
        tone = CyberCardTone.LAVENDER,
        modifier = Modifier.testTag("parent_remote_settings_card")
    ) {
        Text(
            text = "AI Sensitivity Level",
            color = AegisTextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = ClassicalDisplayFont
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SensitivityLevel.entries.forEach { level ->
                val selected = sensitivity == level
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) AegisPrimaryCyan else AegisCardBackground)
                        .border(1.dp, if (selected) AegisPrimaryCyan else AegisCardBorder, RoundedCornerShape(10.dp))
                        .clickable { sensitivity = level },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = level.displayName,
                        color = if (selected) AegisOnPrimaryWhite else AegisTextPrimary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Warnings Before Lock",
            color = AegisTextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = ClassicalDisplayFont
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(0 to "Immediate", 1 to "1 Warn", 2 to "2 Warns", 3 to "3 Warns").forEach { (count, label) ->
                val selected = warningThreshold == count
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) AegisChampagneGold else AegisCardBackground)
                        .border(1.dp, if (selected) AegisChampagneGold else AegisCardBorder, RoundedCornerShape(10.dp))
                        .clickable { warningThreshold = count },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (selected) AegisOnPrimaryWhite else AegisTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Lockdown Timer",
            color = AegisTextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = ClassicalDisplayFont
        )
        Spacer(modifier = Modifier.height(6.dp))
        val presets = listOf(10 to "10m", 60 to "1h", 360 to "6h", 600 to "10h")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { (mins, label) ->
                val selected = durationMinutes == mins
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) AegisPurple else AegisCardBackground)
                        .border(1.dp, if (selected) AegisPurple else AegisCardBorder, RoundedCornerShape(10.dp))
                        .clickable { durationMinutes = mins },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (selected) AegisOnPrimaryWhite else AegisTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        RemoteSwitchRow("Strict Everything Filter", strictEverything) { strictEverything = it }
        RemoteSwitchRow("Anti-Tamper Guard", antiTamper) { antiTamper = it }
        RemoteSwitchRow("Live See Thumbnails", liveSee) { liveSee = it }
        RemoteSwitchRow("Explicit Adult Category", blockExplicit) { blockExplicit = it }
        RemoteSwitchRow("Suggestive Content Category", blockSuggestive) { blockSuggestive = it }
        RemoteSwitchRow("Sensitive Graphic Category", blockGraphic) { blockGraphic = it }
        RemoteSwitchRow("Gambling Filter", blockGambling) { blockGambling = it }
        RemoteSwitchRow("Safe DNS Filtering", safeDnsEnabled) { safeDnsEnabled = it }
        RemoteSwitchRow(
            "Bedtime Lock (${AegisPreferences.formatClockTime(bedtimeStartMinutes)}-${AegisPreferences.formatClockTime(bedtimeEndMinutes)})",
            bedtimeEnabled
        ) { bedtimeEnabled = it }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = domainsCsv,
            onValueChange = { domainsCsv = it },
            label = { Text("Blocked Domains (comma-separated)") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = keywordsCsv,
            onValueChange = { keywordsCsv = it },
            label = { Text("Blocked Keywords (comma-separated)") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = budgetsCsv,
            onValueChange = { budgetsCsv = it },
            label = { Text("App Time Budgets (pkg=mins, comma-separated)") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = whitelistCsv,
            onValueChange = { whitelistCsv = it },
            label = { Text("Child Excluded Packages (comma-separated)") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                val parsedWhitelist = whitelistCsv.split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .toSet()
                val parsedDomains = domainsCsv.split(",")
                    .mapNotNull { AegisPreferences.normalizeDomainInput(it) }
                    .toSet()
                val parsedKeywords = keywordsCsv.split(",")
                    .map { it.trim().lowercase(Locale.US) }
                    .filter { it.length >= 2 }
                    .toSet()
                val parsedBudgets = budgetsCsv.split(",")
                    .mapNotNull { entry ->
                        val parts = entry.split("=")
                        if (parts.size == 2) {
                            val pkg = parts[0].trim()
                            val mins = parts[1].trim().toIntOrNull()
                            if (pkg.isNotBlank() && mins != null && mins > 0) pkg to mins.coerceIn(1, 720) else null
                        } else null
                    }
                    .toMap()
                onPushSettings(
                    RemotePolicySettings(
                        sensitivity = sensitivity,
                        strictEverything = strictEverything,
                        lockdownDurationMinutes = durationMinutes,
                        antiTamperEnabled = antiTamper,
                        childUninstallLocked = currentRemote.childUninstallLocked,
                        liveSeeEnabled = liveSee,
                        whitelistedPackages = parsedWhitelist,
                        warningThresholdCount = warningThreshold,
                        bedtimeEnabled = bedtimeEnabled,
                        bedtimeStartMinutes = bedtimeStartMinutes,
                        bedtimeEndMinutes = bedtimeEndMinutes,
                        appTimeBudgets = parsedBudgets,
                        customBlockedDomains = parsedDomains,
                        customBlockedKeywords = parsedKeywords,
                        safeDnsEnabled = safeDnsEnabled,
                        blockExplicitAdult = blockExplicit,
                        blockSuggestiveContent = blockSuggestive,
                        blockSensitiveGraphic = blockGraphic,
                        blockGamblingKeywords = blockGambling
                    )
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = AegisPrimaryCyan, contentColor = AegisOnPrimaryWhite),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("push_remote_settings_button")
        ) {
            Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("PUSH SETTINGS TO CHILD", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RemoteSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = AegisTextPrimary,
            fontSize = 13.sp,
            fontFamily = ClassicalBodyFont
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = AegisOnPrimaryWhite, checkedTrackColor = AegisPrimaryCyan)
        )
    }
}

@Composable
private fun ParentThreatIncidentRow(incident: SecurityIncident) {
    val timeText = remember(incident.timestampMillis) {
        SimpleDateFormat("MMM d, hh:mm a", Locale.US).format(Date(incident.timestampMillis))
    }
    CyberCard(
        borderColor = AegisDangerRed.copy(alpha = 0.35f),
        tone = CyberCardTone.GOLD,
        modifier = Modifier.testTag("parent_threat_logs_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = AegisDangerRed, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "${incident.appDisplayName} • ${incident.category.displayName}",
                        color = AegisTextPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalDisplayFont
                    )
                    Text(
                        text = "$timeText • ${(incident.confidenceScore * 100).toInt()}% • ${incident.actionTaken}",
                        color = AegisTextSecondary,
                        fontSize = 11.sp,
                        fontFamily = ClassicalBodyFont
                    )
                }
            }
        }
    }
}

@Composable
private fun PolicyStatusChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = ClassicalBodyFont
        )
    }
}
