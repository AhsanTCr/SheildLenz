package com.example.ui.parents

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.audio.AegisSoundEffects
import com.example.core.models.SafeSearchPack
import com.example.core.models.SecurityIncident
import com.example.core.models.SensitivityLevel
import com.example.data.preferences.AegisPreferences
import com.example.data.preferences.RecoveryQrMode
import com.example.data.preferences.RecoveryScanResult
import com.example.data.repository.ChildLiveThumbnail
import com.example.data.repository.ParentChildRepository
import com.example.data.repository.RemotePolicySettings
import com.example.services.protection.ParentChildSyncController
import com.example.ui.auth.GoogleSignInManager
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberCardTone
import com.example.ui.components.CyberTopBar
import com.example.ui.components.QrCodeImage
import com.example.ui.components.QrScannerDialog
import com.example.ui.components.SheildlenzBackdrop
import com.example.ui.settings.InstalledAppEntry
import com.example.ui.settings.loadInstalledLaunchableApps
import com.example.ui.theme.AegisCardBackground
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisChampagneSoft
import com.example.ui.theme.AegisDangerRed
import com.example.ui.theme.AegisElectricBlue
import com.example.ui.theme.AegisEmeraldBorder
import com.example.ui.theme.AegisEmeraldTeal
import com.example.ui.theme.AegisGoldBorder
import com.example.ui.theme.AegisGoldBright
import com.example.ui.theme.AegisGoldDeepTint
import com.example.ui.theme.AegisLavenderBorder
import com.example.ui.theme.AegisLavenderLight
import com.example.ui.theme.AegisLavenderSoft
import com.example.ui.theme.AegisLavenderSurface
import com.example.ui.theme.AegisOnPrimaryWhite
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisPurple
import com.example.ui.theme.AegisSkySoft
import com.example.ui.theme.AegisSuccessGreen
import com.example.ui.theme.AegisSurfaceElevated
import com.example.ui.theme.AegisTextMuted
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.AegisWarningAmber
import com.example.ui.theme.ClassicalBodyFont
import com.example.ui.theme.ClassicalDisplayFont
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class ParentPopupSheet {
    NONE,
    LIVE_SEE,
    LOCK_CONTROL,
    REMOTE_RULES,
    THREAT_FEED
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ParentsScreen(
    repository: ParentChildRepository,
    preferences: AegisPreferences,
    syncController: ParentChildSyncController? = null,
    pendingPairingCode: String? = null,
    onConsumePendingPairingCode: () -> Unit = {},
    onRequestGoogleSignIn: ((
        onSuccess: () -> Unit,
        onCancelled: () -> Unit,
        onError: (String) -> Unit
    ) -> Unit)? = null,
    onRequestGoogleSignOut: ((onComplete: () -> Unit) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentUser by remember {
        mutableStateOf(
            runCatching {
                GoogleSignInManager.ensureFirebaseInitialized(context)
                Firebase.auth.currentUser
            }.getOrNull()
        )
    }

    DisposableEffect(Unit) {
        val auth = runCatching {
            GoogleSignInManager.ensureFirebaseInitialized(context)
            Firebase.auth
        }.getOrNull()
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
    var showEnlargedChildQr by remember { mutableStateOf(false) }
    var activeParentPopup by remember { mutableStateOf(ParentPopupSheet.NONE) }

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
        AegisSoundEffects.playTap(context)
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
        AegisSoundEffects.playTap(context)
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

    val signInWithGoogle: () -> Unit = {
        AegisSoundEffects.playTap(context)
        isSigningIn = true
        statusMessage = null
        val onAuthSuccess: () -> Unit = {
            isSigningIn = false
            statusMessage = null
            syncController?.ensureStarted()
            Toast.makeText(context, "Signed in as Guardian!", Toast.LENGTH_SHORT).show()
        }
        val onAuthCancelled: () -> Unit = {
            isSigningIn = false
        }
        val onAuthError: (String) -> Unit = { msg ->
            isSigningIn = false
            statusMessage = msg
        }
        if (onRequestGoogleSignIn != null) {
            onRequestGoogleSignIn(onAuthSuccess, onAuthCancelled, onAuthError)
        } else {
            GoogleSignInManager.signInWithGoogle(
                context = context,
                scope = scope,
                onAuthSuccess = onAuthSuccess,
                onAuthCancelled = onAuthCancelled,
                onAuthError = onAuthError
            )
        }
    }

    SheildlenzBackdrop(modifier = Modifier.testTag("parents_screen_root")) {
        Scaffold(
            contentWindowInsets = WindowInsets.statusBars,
            topBar = {
                CyberTopBar(title = "Parents")
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // ─── 1. Compact Top Bento Row: Guardian Account (Left) + QR Recovery Scanner (Right) ───
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (currentUser == null) {
                        CompactBentoCard(
                            tone = CyberCardTone.SKY,
                            borderColor = AegisCardBorder,
                            modifier = Modifier
                                .weight(1.18f)
                                .fillMaxHeight()
                                .testTag("parents_signin_card")
                        ) {
                            Button(
                                onClick = signInWithGoogle,
                                enabled = !isSigningIn,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AegisPrimaryCyan,
                                    contentColor = AegisOnPrimaryWhite
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("parents_google_signin_button")
                            ) {
                                if (isSigningIn) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = AegisOnPrimaryWhite,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.SupervisorAccount,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Google Sign-In",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    } else {
                        CompactBentoCard(
                            tone = CyberCardTone.SKY,
                            borderColor = AegisCardBorder,
                            modifier = Modifier
                                .weight(1.22f)
                                .fillMaxHeight()
                                .testTag("parents_account_banner")
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
                                    Icon(
                                        imageVector = Icons.Default.CloudDone,
                                        contentDescription = "Connected account",
                                        tint = AegisSuccessGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Guardian",
                                            color = AegisTextPrimary,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = ClassicalDisplayFont,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = currentUser?.email?.takeIf { it.isNotBlank() } ?: "Connected",
                                            color = AegisTextSecondary,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        AegisSoundEffects.playTap(context)
                                        if (preferences.isChildUninstallBlocked() && !pairedParentUid.isNullOrBlank()) {
                                            Toast.makeText(
                                                context,
                                                "Child Lock active: Parent must unlock or unlink first.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } else {
                                            if (onRequestGoogleSignOut != null) {
                                                onRequestGoogleSignOut {}
                                            } else {
                                                GoogleSignInManager.signOut(
                                                    context = context,
                                                    scope = scope,
                                                    onSignOutComplete = { syncController?.stop() }
                                                )
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
                                        tint = AegisTextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Right top tile: Recovery QR Scanner
                    CompactBentoCard(
                        tone = CyberCardTone.GOLD,
                        borderColor = AegisGoldBorder,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag("guardian_qr_recovery_card")
                    ) {
                        Button(
                            onClick = {
                                AegisSoundEffects.playTap(context)
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
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("scan_recovery_qr_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "QR Recovery",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // Inline concise recovery status when triggered
                when (val outcome = recoveryScanOutcome) {
                    is RecoveryScanResult.Success -> {
                        CompactBentoCard(tone = CyberCardTone.GOLD, borderColor = AegisSuccessGreen.copy(alpha = 0.6f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (outcome.mode == RecoveryQrMode.TIMER_END) {
                                            "TIMER END • SHIELD STAYS ON"
                                        } else {
                                            "PIN RESET CODE"
                                        },
                                        color = AegisSuccessGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = outcome.oneTimeCode,
                                        color = AegisTextPrimary,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 3.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.testTag("recovery_generated_code_text")
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        AegisSoundEffects.playTap(context)
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        cm?.setPrimaryClip(ClipData.newPlainText("Sheildlenz One-Time Code", outcome.oneTimeCode))
                                        Toast.makeText(context, "Copied ${outcome.oneTimeCode}", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .height(44.dp)
                                        .testTag("copy_recovery_code_button")
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("COPY", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    RecoveryScanResult.RequiresGoogleSignIn -> {
                        Text(
                            text = "Sign in with Google above using your registered Guardian email to authorize QR recovery.",
                            color = AegisDangerRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = ClassicalBodyFont,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                    is RecoveryScanResult.EmailMismatch -> {
                        Text(
                            text = "Account mismatch: ${outcome.signedInEmail} is not the registered Guardian email.",
                            color = AegisDangerRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = ClassicalBodyFont,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                    RecoveryScanResult.InvalidQr -> {
                        Text(
                            text = "Unrecognized recovery QR code.",
                            color = AegisDangerRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = ClassicalBodyFont,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                    null -> Unit
                }

                // ─── 2. Compact Connection Role Switcher ───
                CompactBentoCard(
                    tone = CyberCardTone.LAVENDER,
                    borderColor = AegisLavenderBorder,
                    modifier = Modifier.testTag("parents_role_selector_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RoleConnectionOptionTile(
                            title = "As Parent",
                            subtitle = "Monitor & Lock",
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
                            subtitle = "Protected Mode",
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
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = statusMessage!!,
                            color = AegisPurple,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = ClassicalBodyFont
                        )
                    }
                }

                // ─── 3. Role-Specific Zero-Scroll Workspace ───
                if (savedRole == "child") {
                    val activeCode = savedPairingCode ?: childPairingState?.pairingCode ?: "------"
                    val isPaired = (childPairingState?.active == true && !childPairingState?.parentId.isNullOrBlank()) ||
                        !pairedParentUid.isNullOrBlank()
                    val parentLabel = childPairingState?.parentDisplayName?.takeIf { it.isNotBlank() }
                        ?: pairedParentName
                        ?: "Parent Guardian"
                    val uninstallBlocked = localUninstallLocked && !(parentApprovedUntil > System.currentTimeMillis())
                    val qrPayload = ParentChildRepository.buildPairingUri(
                        if (activeCode == "------") "000000" else activeCode
                    )

                    // Left: 6-digit Pairing Code & Action Buttons | Right: Small Tappable QR Code (Tap to Enlarge)
                    CompactBentoCard(
                        tone = CyberCardTone.GOLD,
                        borderColor = AegisGoldBorder,
                        modifier = Modifier.testTag("child_pairing_qr_card")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "CHILD PAIRING CODE",
                                    color = AegisChampagneGold,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp,
                                    fontFamily = ClassicalBodyFont,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = activeCode,
                                    color = AegisPrimaryCyan,
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 3.sp,
                                    fontFamily = ClassicalDisplayFont,
                                    maxLines = 1,
                                    modifier = Modifier.testTag("child_pairing_code_text")
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            AegisSoundEffects.playTap(context)
                                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                            cm?.setPrimaryClip(ClipData.newPlainText("Sheildlenz Pairing Code", activeCode))
                                            Toast.makeText(context, "Copied code $activeCode", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(9.dp),
                                        contentPadding = PaddingValues(horizontal = 3.dp, vertical = 0.dp),
                                        modifier = Modifier.weight(1f).height(48.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy pairing code", modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("COPY", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            AegisSoundEffects.playTap(context)
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
                                        shape = RoundedCornerShape(9.dp),
                                        contentPadding = PaddingValues(horizontal = 3.dp, vertical = 0.dp),
                                        modifier = Modifier.weight(1f).height(48.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "Share pairing code", modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("SHARE", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                    }

                                    Button(
                                        onClick = {
                                            AegisSoundEffects.playTap(context)
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
                                        shape = RoundedCornerShape(9.dp),
                                        contentPadding = PaddingValues(horizontal = 3.dp, vertical = 0.dp),
                                        modifier = Modifier
                                            .weight(0.65f)
                                            .height(48.dp)
                                            .testTag("child_regenerate_code_button")
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Generate new pairing code", modifier = Modifier.size(14.dp))
                                    }
                                }
                            }

                            // Right: Small tappable QR thumbnail -> tap opens full enlarged QR dialog
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(Color.White)
                                    .border(1.5.dp, AegisGoldBorder, RoundedCornerShape(11.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                QrCodeImage(
                                    payload = qrPayload,
                                    padding = 4.dp,
                                    testTag = "child_pairing_qr_matrix",
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable {
                                            AegisSoundEffects.playTap(context)
                                            showEnlargedChildQr = true
                                        }
                                        .testTag("child_qr_tap_enlarge")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(3.dp)
                                            .clip(CircleShape)
                                            .background(AegisPrimaryCyan.copy(alpha = 0.92f))
                                            .padding(2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ZoomIn,
                                            contentDescription = "Tap to enlarge QR",
                                            tint = AegisOnPrimaryWhite,
                                            modifier = Modifier.size(9.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Compact Child Guardian Status Card
                    CompactBentoCard(
                        tone = if (uninstallBlocked) CyberCardTone.LAVENDER else CyberCardTone.SKY,
                        borderColor = if (uninstallBlocked) AegisDangerRed.copy(alpha = 0.45f) else AegisCardBorder,
                        modifier = Modifier.testTag("child_connection_status_card")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Icon(
                                imageVector = if (isPaired) Icons.Default.AdminPanelSettings else Icons.Default.QrCode2,
                                contentDescription = null,
                                tint = if (isPaired) AegisSuccessGreen else AegisChampagneGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isPaired) "Connected to $parentLabel" else "Waiting for Parent Connection",
                                color = AegisTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = ClassicalDisplayFont,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (uninstallBlocked) AegisChampagneSoft else AegisSuccessGreen.copy(alpha = 0.12f))
                                .border(
                                    1.dp,
                                    if (uninstallBlocked) AegisDangerRed.copy(alpha = 0.35f) else AegisSuccessGreen.copy(alpha = 0.35f),
                                    RoundedCornerShape(9.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (uninstallBlocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = if (uninstallBlocked) AegisDangerRed else AegisSuccessGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (uninstallBlocked) {
                                    "Uninstall Blocked (Child Mode)"
                                } else {
                                    "Parent Authorized Uninstall Active"
                                },
                                color = AegisTextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = ClassicalDisplayFont,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (!uninstallBlocked) {
                                Button(
                                    onClick = {
                                        AegisSoundEffects.playTap(context)
                                        preferences.grantTemporarySettingsAuthorization(5)
                                        val intent = Intent(Intent.ACTION_DELETE).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                        context.startActivity(intent)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AegisDangerRed),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("child_parent_authorized_uninstall_button")
                                ) {
                                    Text("UNINSTALL", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            PolicyStatusChip(sensitivity.displayName, AegisPrimaryCyan)
                            PolicyStatusChip("Lock ${AegisPreferences.formatMinutesLabel(lockdownMinutes)}", AegisChampagneGold)
                            PolicyStatusChip(if (liveSeeEnabled) "Live ON" else "Live OFF", if (liveSeeEnabled) AegisSuccessGreen else AegisTextMuted)
                        }
                    }

                    if (showEnlargedChildQr) {
                        AlertDialog(
                            onDismissRequest = { showEnlargedChildQr = false },
                            containerColor = AegisChampagneSoft,
                            modifier = Modifier.testTag("child_qr_enlarged_dialog"),
                            title = {
                                Text(
                                    text = "Scan Child QR Code",
                                    color = AegisTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = ClassicalDisplayFont
                                )
                            },
                            text = {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    QrCodeImage(
                                        payload = qrPayload,
                                        testTag = "child_pairing_qr_enlarged",
                                        modifier = Modifier
                                            .size(230.dp)
                                            .border(2.dp, AegisGoldBorder, RoundedCornerShape(18.dp))
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = activeCode,
                                        color = AegisPrimaryCyan,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 6.sp,
                                        fontFamily = ClassicalDisplayFont
                                    )
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        AegisSoundEffects.playTap(context)
                                        showEnlargedChildQr = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AegisPrimaryCyan, contentColor = AegisOnPrimaryWhite)
                                ) {
                                    Text("DONE", fontWeight = FontWeight.Bold)
                                }
                            }
                        )
                    }
                } else if (savedRole == "parent") {
                    // Compact Parent Pairing Card (Fits cleanly on 1 viewport)
                    CompactBentoCard(
                        tone = CyberCardTone.SKY,
                        borderColor = AegisCardBorder,
                        modifier = Modifier.testTag("parent_pair_child_card")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = pairingInput,
                                onValueChange = { pairingInput = it },
                                label = { Text("Child 6-Digit Code", fontSize = 11.sp) },
                                placeholder = { Text("482910", fontSize = 12.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("parent_pairing_code_input")
                            )
                            Button(
                                onClick = {
                                    AegisSoundEffects.playTap(context)
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
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .height(52.dp)
                                    .testTag("pair_child_button")
                            ) {
                                Text("PAIR", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    AegisSoundEffects.playTap(context)
                                    qrScanMode = "pairing"
                                    showQrScanner = true
                                },
                                shape = RoundedCornerShape(11.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("scan_child_qr_button")
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Child QR", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("SCAN CHILD QR", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            }

                            OutlinedButton(
                                onClick = {
                                    AegisSoundEffects.playTap(context)
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val clipText = cm?.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                    val parsed = ParentChildRepository.extractPairingCode(clipText)
                                    if (parsed != null) {
                                        pairingInput = parsed
                                    } else {
                                        Toast.makeText(context, "No 6-digit code in clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(11.dp),
                                modifier = Modifier
                                    .weight(0.75f)
                                    .height(48.dp)
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = "Paste code", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("PASTE", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            }
                        }
                    }

                    // Linked Child Devices Strip + Compact 2x2 Action Grid (Opens Popup Dialogs!)
                    if (pairedChildren.isNotEmpty() && activeChild != null) {
                        CompactBentoCard(
                            tone = CyberCardTone.GOLD,
                            borderColor = AegisGoldBorder,
                            modifier = Modifier.testTag("parent_children_list_card")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    pairedChildren.forEach { child ->
                                        val isCurrent = child.childUid == activeChild.childUid
                                        FilterChip(
                                            selected = isCurrent,
                                            onClick = {
                                                AegisSoundEffects.playTap(context)
                                                selectedChildUid = child.childUid
                                            },
                                            label = {
                                                Text(
                                                    text = child.childDisplayName,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.ChildCare,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = AegisPrimaryCyan,
                                                selectedLabelColor = AegisOnPrimaryWhite,
                                                selectedLeadingIconColor = AegisOnPrimaryWhite
                                            ),
                                            modifier = Modifier.height(36.dp)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        AegisSoundEffects.playTap(context)
                                        scope.launch {
                                            repository.unlinkChild(activeChild.childUid)
                                            Toast.makeText(context, "Unlinked ${activeChild.childDisplayName}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Default.LinkOff, contentDescription = "Unlink child", tint = AegisDangerRed)
                                }
                            }
                        }

                        // Compact 2x2 Control Grid -> opens full Popup Dialogs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ParentBentoActionTile(
                                title = "Live See",
                                subtitle = if (remoteSettingsForChild?.liveSeeEnabled != false) "Stream ON" else "Stream OFF",
                                icon = Icons.Default.Visibility,
                                accentColor = AegisPrimaryCyan,
                                tone = CyberCardTone.SKY,
                                borderColor = AegisCardBorder,
                                testTag = "parent_live_see_card",
                                onClick = {
                                    AegisSoundEffects.playTap(context)
                                    activeParentPopup = ParentPopupSheet.LIVE_SEE
                                },
                                modifier = Modifier.weight(1f)
                            )
                            ParentBentoActionTile(
                                title = "Lock & Guard",
                                subtitle = if (remoteSettingsForChild?.childUninstallLocked != false) "Uninstall Locked" else "Unlocked",
                                icon = Icons.Default.Lock,
                                accentColor = AegisDangerRed,
                                tone = CyberCardTone.GOLD,
                                borderColor = AegisGoldBorder,
                                testTag = "parent_remote_actions_card",
                                onClick = {
                                    AegisSoundEffects.playTap(context)
                                    activeParentPopup = ParentPopupSheet.LOCK_CONTROL
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ParentBentoActionTile(
                                title = "Remote Rules",
                                subtitle = "${(remoteSettingsForChild?.sensitivity ?: SensitivityLevel.BALANCED).displayName} Policy",
                                icon = Icons.Default.Tune,
                                accentColor = AegisPurple,
                                tone = CyberCardTone.LAVENDER,
                                borderColor = AegisLavenderBorder,
                                testTag = "parent_remote_settings_card",
                                onClick = {
                                    AegisSoundEffects.playTap(context)
                                    activeParentPopup = ParentPopupSheet.REMOTE_RULES
                                },
                                modifier = Modifier.weight(1f)
                            )
                            ParentBentoActionTile(
                                title = "Threat Feed",
                                subtitle = if (remoteThreatLogs.isEmpty()) "0 incidents" else "${remoteThreatLogs.size} alerts",
                                icon = Icons.Default.Warning,
                                accentColor = AegisChampagneGold,
                                tone = CyberCardTone.GOLD,
                                borderColor = AegisGoldBorder,
                                testTag = "parent_threat_logs_card",
                                onClick = {
                                    AegisSoundEffects.playTap(context)
                                    activeParentPopup = ParentPopupSheet.THREAT_FEED
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Popup Sheets for Parent Controls
                        when (activeParentPopup) {
                            ParentPopupSheet.LIVE_SEE -> {
                                ParentPopupDialog(
                                    title = "Live See • ${activeChild.childDisplayName}",
                                    onDismiss = { activeParentPopup = ParentPopupSheet.NONE }
                                ) {
                                    ParentLiveSeeContent(
                                        thumbnail = liveThumbnail,
                                        liveSeeEnabled = remoteSettingsForChild?.liveSeeEnabled ?: true,
                                        onToggleLiveSee = { enabled ->
                                            val base = remoteSettingsForChild ?: RemotePolicySettings()
                                            scope.launch {
                                                repository.pushRemoteSettings(activeChild.childUid, base.copy(liveSeeEnabled = enabled))
                                            }
                                        },
                                        onRequestFreshFrame = {
                                            AegisSoundEffects.playTap(context)
                                            scope.launch {
                                                repository.sendRemoteCommand(
                                                    activeChild.childUid,
                                                    ParentChildRepository.COMMAND_REQUEST_LIVE_FRAME
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                            ParentPopupSheet.LOCK_CONTROL -> {
                                ParentPopupDialog(
                                    title = "Lock & Uninstall • ${activeChild.childDisplayName}",
                                    onDismiss = { activeParentPopup = ParentPopupSheet.NONE }
                                ) {
                                    ParentRemoteActionsContent(
                                        childName = activeChild.childDisplayName,
                                        childUninstallLocked = remoteSettingsForChild?.childUninstallLocked ?: true,
                                        onLockNow = {
                                            AegisSoundEffects.playTap(context)
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
                                            AegisSoundEffects.playTap(context)
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
                                            AegisSoundEffects.playTap(context)
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
                            }
                            ParentPopupSheet.REMOTE_RULES -> {
                                ParentPopupDialog(
                                    title = "Remote Rules • ${activeChild.childDisplayName}",
                                    onDismiss = { activeParentPopup = ParentPopupSheet.NONE }
                                ) {
                                    ParentRemoteSettingsContent(
                                        currentRemote = remoteSettingsForChild ?: RemotePolicySettings(),
                                        onPushSettings = { updated ->
                                            AegisSoundEffects.playModeOn(context)
                                            scope.launch {
                                                val ok = repository.pushRemoteSettings(activeChild.childUid, updated)
                                                Toast.makeText(
                                                    context,
                                                    if (ok) "Settings synced to ${activeChild.childDisplayName}" else "Sync failed",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                if (ok) activeParentPopup = ParentPopupSheet.NONE
                                            }
                                        }
                                    )
                                }
                            }
                            ParentPopupSheet.THREAT_FEED -> {
                                ParentPopupDialog(
                                    title = "Threat Feed • ${activeChild.childDisplayName}",
                                    onDismiss = { activeParentPopup = ParentPopupSheet.NONE }
                                ) {
                                    if (remoteThreatLogs.isEmpty()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AegisSuccessGreen)
                                            Text(
                                                text = "No incidents reported from ${activeChild.childDisplayName}. Real-time alerts appear here.",
                                                color = AegisTextSecondary,
                                                fontSize = 12.sp,
                                                fontFamily = ClassicalBodyFont
                                            )
                                        }
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            remoteThreatLogs.forEach { incident ->
                                                ParentThreatIncidentRow(incident)
                                            }
                                        }
                                    }
                                }
                            }
                            ParentPopupSheet.NONE -> Unit
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactBentoCard(
    tone: CyberCardTone,
    borderColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val (gradColors, stripeColors, shadowColor) = when (tone) {
        CyberCardTone.LAVENDER -> Triple(
            listOf(AegisLavenderSoft, AegisLavenderSurface),
            listOf(AegisPurple, AegisLavenderLight),
            AegisPurple.copy(alpha = 0.14f)
        )
        CyberCardTone.GOLD -> Triple(
            listOf(AegisChampagneSoft, AegisGoldDeepTint),
            listOf(AegisChampagneGold, AegisGoldBright),
            AegisChampagneGold.copy(alpha = 0.15f)
        )
        else -> Triple(
            listOf(AegisCardBackground, AegisSkySoft),
            listOf(AegisPrimaryCyan, AegisElectricBlue),
            AegisPrimaryCyan.copy(alpha = 0.14f)
        )
    }
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp, shape = shape, ambientColor = shadowColor, spotColor = shadowColor)
            .clip(shape)
            .background(Brush.verticalGradient(gradColors))
            .border(BorderStroke(1.dp, borderColor.copy(alpha = 0.65f)), shape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(Brush.horizontalGradient(stripeColors))
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 5.dp),
            content = content
        )
    }
}

@Composable
private fun ParentBentoActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    tone: CyberCardTone,
    borderColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CompactBentoCard(
        borderColor = borderColor,
        tone = tone,
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 42.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = AegisTextPrimary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalDisplayFont,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = accentColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = ClassicalBodyFont,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ParentPopupDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AegisLavenderSoft,
        titleContentColor = AegisTextPrimary,
        title = {
            Text(
                text = title,
                color = AegisTextPrimary,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ClassicalDisplayFont
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                content()
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    AegisSoundEffects.playTap(context)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = AegisPrimaryCyan, contentColor = AegisOnPrimaryWhite)
            ) {
                Text("CLOSE", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun RoleConnectionOptionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Surface(
        onClick = onClick,
        shape = shape,
        color = if (isSelected) accentColor.copy(alpha = 0.15f) else AegisCardBackground,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) accentColor else AegisCardBorder
        ),
        modifier = modifier.heightIn(min = 48.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(accentColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = title,
                    color = AegisTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalDisplayFont,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isSelected) {
                    Text(
                        text = "Active",
                        color = accentColor,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalBodyFont,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun ParentLiveSeeContent(
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

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val timeLabel = if (thumbnail != null && thumbnail.timestamp > 0L) {
            val fmt = SimpleDateFormat("hh:mm:ss a", Locale.US).format(Date(thumbnail.timestamp))
            "${thumbnail.activeAppName.ifBlank { "Screen" }} • $fmt"
        } else {
            "Streams periodic frames from Child"
        }
        Text(
            text = timeLabel,
            color = AegisTextSecondary,
            fontSize = 11.5.sp,
            fontFamily = ClassicalBodyFont,
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        )
        val switchContext = LocalContext.current
        Switch(
            checked = liveSeeEnabled,
            onCheckedChange = { next ->
                AegisSoundEffects.playTap(switchContext)
                onToggleLiveSee(next)
            },
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
                .height(140.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(AegisSkySoft)
                .border(1.dp, AegisCardBorder, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(14.dp)) {
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
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    OutlinedButton(
        onClick = onRequestFreshFrame,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().height(48.dp)
    ) {
        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("PING CHILD FOR FRESH SNAPSHOT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ParentRemoteActionsContent(
    childName: String,
    childUninstallLocked: Boolean,
    onLockNow: () -> Unit,
    onUnlockNow: () -> Unit,
    onToggleUninstallLock: (Boolean) -> Unit,
    onAuthorizeParentUninstall: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
            Text("LOCK CHILD", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
            Text("UNLOCK", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

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
                text = "Lock uninstall on $childName",
                color = AegisTextSecondary,
                fontSize = 11.sp,
                fontFamily = ClassicalBodyFont
            )
        }
        val uninstallSwitchContext = LocalContext.current
        Switch(
            checked = childUninstallLocked,
            onCheckedChange = { next ->
                AegisSoundEffects.playTap(uninstallSwitchContext)
                onToggleUninstallLock(next)
            },
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
        Text("ALLOW UNINSTALL (5 MIN)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ParentRemoteSettingsContent(
    currentRemote: RemotePolicySettings,
    onPushSettings: (RemotePolicySettings) -> Unit
) {
    val context = LocalContext.current
    var sensitivity by remember(currentRemote) { mutableStateOf(currentRemote.sensitivity) }
    var strictEverything by remember(currentRemote) { mutableStateOf(currentRemote.strictEverything) }
    var antiTamper by remember(currentRemote) { mutableStateOf(currentRemote.antiTamperEnabled) }
    var liveSee by remember(currentRemote) { mutableStateOf(currentRemote.liveSeeEnabled) }
    var durationMinutes by remember(currentRemote) { mutableIntStateOf(currentRemote.lockdownDurationMinutes) }
    var warningThreshold by remember(currentRemote) { mutableIntStateOf(currentRemote.warningThresholdCount) }
    var warningOnlyNoLockout by remember(currentRemote) { mutableStateOf(currentRemote.warningOnlyNoLockout) }
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
    var appBudgetsMap by remember(currentRemote) { mutableStateOf(currentRemote.appTimeBudgets) }
    var whitelistCsv by remember(currentRemote) {
        mutableStateOf(currentRemote.whitelistedPackages.joinToString(", "))
    }

    var studyModeEnabled by remember(currentRemote) { mutableStateOf(currentRemote.studyModeEnabled) }
    var studyModeDurationMinutes by remember(currentRemote) { mutableIntStateOf(currentRemote.studyModeDurationMinutes) }
    var studySoftWarningSeconds by remember(currentRemote) { mutableIntStateOf(currentRemote.studySoftWarningSeconds) }
    var studyAllowedPackages by remember(currentRemote) { mutableStateOf(currentRemote.studyAllowedPackages) }
    var smartSafeSearchEnabled by remember(currentRemote) { mutableStateOf(currentRemote.smartSafeSearchEnabled) }
    var safeSearchPacks by remember(currentRemote) { mutableStateOf(currentRemote.safeSearchPacks) }
    var discreetBlockingScreenMode by remember(currentRemote) { mutableStateOf(currentRemote.discreetBlockingScreenMode) }

    var showBudgetAppPicker by remember { mutableStateOf(false) }
    var showStudyAppsPicker by remember { mutableStateOf(false) }
    var installedApps by remember { mutableStateOf<List<InstalledAppEntry>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(false) }

    LaunchedEffect(showBudgetAppPicker, showStudyAppsPicker) {
        if ((showBudgetAppPicker || showStudyAppsPicker) && installedApps.isEmpty()) {
            isLoadingApps = true
            installedApps = loadInstalledLaunchableApps(context)
            isLoadingApps = false
        }
    }

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
                    .height(48.dp)
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
                    .height(48.dp)
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
                    .height(48.dp)
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

    RemoteSwitchRow("Non-Suspicious Blocking Screens (Hide App Name)", discreetBlockingScreenMode) {
        discreetBlockingScreenMode = it
    }
    RemoteSwitchRow("Remote Study Mode Lock", studyModeEnabled) { studyModeEnabled = it }
    AnimatedVisibility(visible = studyModeEnabled) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AegisLavenderSoft)
                .border(1.dp, AegisLavenderBorder, RoundedCornerShape(12.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Study Session Length & Soft Warning",
                color = AegisTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(15, 25, 45, 60).forEach { mins ->
                    val picked = studyModeDurationMinutes == mins
                    OutlinedButton(
                        onClick = { studyModeDurationMinutes = mins },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (picked) AegisPurple else AegisCardBackground,
                            contentColor = if (picked) AegisOnPrimaryWhite else AegisTextPrimary
                        ),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("${mins}m", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(15, 30, 60).forEach { sec ->
                    val picked = studySoftWarningSeconds == sec
                    OutlinedButton(
                        onClick = { studySoftWarningSeconds = sec },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (picked) AegisPrimaryCyan else AegisCardBackground,
                            contentColor = if (picked) AegisOnPrimaryWhite else AegisTextPrimary
                        ),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("${sec}s Warn", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            OutlinedButton(
                onClick = { showStudyAppsPicker = true },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = AegisCardBackground,
                    contentColor = AegisTextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("parent_study_allowed_apps_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp), tint = AegisPurple)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ALLOWED STUDY APPS (${studyAllowedPackages.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    RemoteSwitchRow("Smart Safe Search Guard", smartSafeSearchEnabled) { smartSafeSearchEnabled = it }
    AnimatedVisibility(visible = smartSafeSearchEnabled) {
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SafeSearchPack.entries.forEach { pack ->
                val active = pack.id in safeSearchPacks
                FilterChip(
                    selected = active,
                    onClick = {
                        val next = safeSearchPacks.toMutableSet()
                        if (!next.add(pack.id)) next.remove(pack.id)
                        safeSearchPacks = next
                    },
                    label = { Text(pack.title, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AegisPrimaryCyan,
                        selectedLabelColor = AegisOnPrimaryWhite
                    ),
                    modifier = Modifier.heightIn(min = 48.dp)
                )
            }
        }
    }

    RemoteSwitchRow("Warn Only (Don't Lockout)", warningOnlyNoLockout) { warningOnlyNoLockout = it }
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

    Spacer(modifier = Modifier.height(10.dp))

    // Visual Child App Time Budgets Sync Section
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AegisCardBackground)
            .border(1.dp, AegisEmeraldBorder, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = "Child App Time Budgets",
                    color = AegisTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalDisplayFont
                )
                Text(
                    text = if (appBudgetsMap.isEmpty()) "No child app limits configured" else "${appBudgetsMap.size} limit(s) synced with child",
                    color = AegisTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = ClassicalBodyFont
                )
            }
            Button(
                onClick = {
                    AegisSoundEffects.playTap(context)
                    showBudgetAppPicker = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = AegisEmeraldTeal, contentColor = AegisOnPrimaryWhite),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("parent_add_app_budget_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("ADD BUDGET", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (appBudgetsMap.isNotEmpty()) {
            appBudgetsMap.entries.sortedBy { it.key }.forEach { (pkg, mins) ->
                val friendlyLabel = remember(pkg, installedApps) {
                    installedApps.firstOrNull { it.packageName == pkg }?.label
                        ?: SafeSearchPack.POPULAR_BUDGET_APP_PRESETS.firstOrNull { it.first == pkg }?.second
                        ?: runCatching {
                            val ai = context.packageManager.getApplicationInfo(pkg, 0)
                            context.packageManager.getApplicationLabel(ai).toString()
                        }.getOrDefault(pkg)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AegisSurfaceElevated)
                        .border(1.dp, AegisEmeraldBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                        Text(
                            text = friendlyLabel,
                            color = AegisTextPrimary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${mins} min / day",
                            color = AegisEmeraldTeal,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val nextMins = (mins - 15).coerceAtLeast(15)
                                appBudgetsMap = appBudgetsMap.toMutableMap().apply { put(pkg, nextMins) }
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("-15m", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = {
                                val nextMins = (mins + 15).coerceAtMost(720)
                                appBudgetsMap = appBudgetsMap.toMutableMap().apply { put(pkg, nextMins) }
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("+15m", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(
                            onClick = {
                                appBudgetsMap = appBudgetsMap.toMutableMap().apply { remove(pkg) }
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove budget for $friendlyLabel",
                                tint = AegisWarningAmber,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

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
                    warningOnlyNoLockout = warningOnlyNoLockout,
                    bedtimeEnabled = bedtimeEnabled,
                    bedtimeStartMinutes = bedtimeStartMinutes,
                    bedtimeEndMinutes = bedtimeEndMinutes,
                    appTimeBudgets = appBudgetsMap,
                    customBlockedDomains = parsedDomains,
                    customBlockedKeywords = parsedKeywords,
                    safeDnsEnabled = safeDnsEnabled,
                    blockExplicitAdult = blockExplicit,
                    blockSuggestiveContent = blockSuggestive,
                    blockSensitiveGraphic = blockGraphic,
                    blockGamblingKeywords = blockGambling,
                    studyModeEnabled = studyModeEnabled,
                    studyModeDurationMinutes = studyModeDurationMinutes,
                    studySoftWarningSeconds = studySoftWarningSeconds,
                    studyAllowedPackages = studyAllowedPackages,
                    smartSafeSearchEnabled = smartSafeSearchEnabled,
                    safeSearchPacks = safeSearchPacks,
                    discreetBlockingScreenMode = discreetBlockingScreenMode
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

    if (showBudgetAppPicker) {
        var search by remember { mutableStateOf("") }
        var pickedPkg by remember { mutableStateOf<String?>(null) }
        var pickedMinutes by remember { mutableIntStateOf(30) }

        val entries = remember(installedApps, search) {
            val existing = installedApps.map { it.packageName }.toSet()
            val extras = SafeSearchPack.POPULAR_BUDGET_APP_PRESETS
                .filter { it.first !in existing }
                .map { InstalledAppEntry(packageName = it.first, label = it.second, icon = null) }
            val combined = installedApps + extras
            val q = search.trim().lowercase(Locale.US)
            if (q.isEmpty()) combined else combined.filter {
                it.label.lowercase(Locale.US).contains(q) || it.packageName.lowercase(Locale.US).contains(q)
            }
        }

        AlertDialog(
            onDismissRequest = { showBudgetAppPicker = false },
            containerColor = AegisChampagneSoft,
            title = {
                Text(
                    text = "Pick App & Daily Limit",
                    fontFamily = ClassicalDisplayFont,
                    fontWeight = FontWeight.Bold,
                    color = AegisTextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = { Text("Search apps or presets...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AegisEmeraldTeal) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(15, 30, 60, 120).forEach { mins ->
                            val isSelected = pickedMinutes == mins
                            OutlinedButton(
                                onClick = { pickedMinutes = mins },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) AegisEmeraldTeal else AegisCardBackground,
                                    contentColor = if (isSelected) AegisOnPrimaryWhite else AegisTextPrimary
                                ),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text("${mins}m", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (isLoadingApps) {
                        Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AegisEmeraldTeal)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().height(210.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(entries, key = { it.packageName }) { app ->
                                val isPicked = pickedPkg == app.packageName
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isPicked) AegisEmeraldTeal.copy(alpha = 0.16f) else AegisCardBackground)
                                        .border(
                                            width = if (isPicked) 1.4.dp else 1.dp,
                                            color = if (isPicked) AegisEmeraldTeal else AegisGoldBorder,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { pickedPkg = app.packageName }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f).padding(end = 6.dp)
                                    ) {
                                        if (app.icon != null) {
                                            Image(
                                                bitmap = app.icon,
                                                contentDescription = app.label,
                                                modifier = Modifier.size(28.dp).clip(RoundedCornerShape(7.dp))
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Column {
                                            Text(
                                                text = app.label,
                                                color = AegisTextPrimary,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = app.packageName,
                                                color = AegisTextMuted,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = if (isPicked) "Selected" else "Pick",
                                        tint = if (isPicked) AegisEmeraldTeal else AegisCardBorder,
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
                    enabled = pickedPkg != null,
                    onClick = {
                        val target = pickedPkg ?: return@Button
                        appBudgetsMap = appBudgetsMap.toMutableMap().apply { put(target, pickedMinutes) }
                        showBudgetAppPicker = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AegisEmeraldTeal, contentColor = AegisOnPrimaryWhite)
                ) {
                    Text("ADD LIMIT", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showBudgetAppPicker = false }) {
                    Text("CANCEL")
                }
            }
        )
    }

    if (showStudyAppsPicker) {
        var studySearch by remember { mutableStateOf("") }
        val entries = remember(installedApps, studySearch, studyAllowedPackages) {
            val existing = installedApps.map { it.packageName }.toSet()
            val defaults = SafeSearchPack.DEFAULT_STUDY_FRIENDLY_PACKAGES
                .filter { it !in existing }
                .map { pkg ->
                    val friendly = pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() }
                    InstalledAppEntry(packageName = pkg, label = friendly, icon = null)
                }
            val combined = installedApps + defaults
            val q = studySearch.trim().lowercase(Locale.US)
            val filtered = if (q.isEmpty()) combined else combined.filter {
                it.label.lowercase(Locale.US).contains(q) || it.packageName.lowercase(Locale.US).contains(q)
            }
            filtered.sortedByDescending { it.packageName in studyAllowedPackages }
        }

        AlertDialog(
            onDismissRequest = { showStudyAppsPicker = false },
            containerColor = AegisLavenderSoft,
            title = {
                Text(
                    text = "Child Allowed Study Apps",
                    fontFamily = ClassicalDisplayFont,
                    fontWeight = FontWeight.Bold,
                    color = AegisTextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = studySearch,
                        onValueChange = { studySearch = it },
                        placeholder = { Text("Search apps to allow during study...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AegisPurple) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isLoadingApps) {
                        Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AegisPurple)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().height(230.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(entries, key = { it.packageName }) { app ->
                                val isAllowed = app.packageName in studyAllowedPackages
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isAllowed) AegisPurple.copy(alpha = 0.16f) else AegisCardBackground)
                                        .border(
                                            width = if (isAllowed) 1.4.dp else 1.dp,
                                            color = if (isAllowed) AegisPurple else AegisLavenderBorder,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            val next = studyAllowedPackages.toMutableSet()
                                            if (!next.add(app.packageName)) next.remove(app.packageName)
                                            studyAllowedPackages = next
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f).padding(end = 6.dp)
                                    ) {
                                        if (app.icon != null) {
                                            Image(
                                                bitmap = app.icon,
                                                contentDescription = app.label,
                                                modifier = Modifier.size(28.dp).clip(RoundedCornerShape(7.dp))
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Column {
                                            Text(
                                                text = app.label,
                                                color = AegisTextPrimary,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = app.packageName,
                                                color = AegisTextMuted,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = if (isAllowed) "Allowed" else "Blocked",
                                        tint = if (isAllowed) AegisPurple else AegisCardBorder,
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
                    onClick = { showStudyAppsPicker = false },
                    colors = ButtonDefaults.buttonColors(containerColor = AegisPurple, contentColor = AegisOnPrimaryWhite)
                ) {
                    Text("DONE", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun RemoteSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = AegisTextPrimary,
            fontSize = 13.sp,
            fontFamily = ClassicalBodyFont,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(end = 10.dp)
        )
        Switch(
            checked = checked,
            onCheckedChange = { next ->
                AegisSoundEffects.playTap(context)
                onCheckedChange(next)
            },
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
        tone = CyberCardTone.GOLD
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
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

@Composable
private fun PolicyStatusChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = ClassicalBodyFont,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}
