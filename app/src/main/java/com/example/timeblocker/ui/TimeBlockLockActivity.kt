package com.example.timeblocker.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.di.AegisContainer
import com.example.timeblocker.TimeBlockerEngine
import com.example.ui.components.AnimatedCountdownRing
import com.example.ui.components.ExpandableDetailsToggle
import com.example.ui.components.SheildlenzBackdrop
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisChampagneSoft
import com.example.ui.theme.AegisGoldBorder
import com.example.ui.theme.AegisGoldBright
import com.example.ui.theme.AegisOnPrimaryWhite
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisPurple
import com.example.ui.theme.AegisShieldTheme
import com.example.ui.theme.AegisTextMuted
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.ClassicalBodyFont
import com.example.ui.theme.ClassicalDisplayFont
import kotlinx.coroutines.delay

/**
 * Dedicated, non-dismissible full-screen lock activity launched over restricted apps
 * when their 5-minute hourly base allowance and any temporary pass are exhausted.
 */
class TimeBlockLockActivity : ComponentActivity() {

    private var blockedPackageState by mutableStateOf("")
    private var blockedAppLabelState by mutableStateOf("")
    private var initialResetMsState by mutableLongStateOf(TimeBlockerEngine.HOURLY_WINDOW_MS)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setFinishOnTouchOutside(false)
        applyIntentExtras(intent)

        val container = AegisContainer.getInstance(this)
        val repository = container.timeBlockerRepository
        val preferences = container.preferences

        setContent {
            AegisShieldTheme {
                // Non-dismissible: consume system back button while lock overlay is shown
                BackHandler(enabled = true) { }

                val pkg = blockedPackageState
                val isCoinDeviceLock = pkg == PIQORAX_DEVICE_LOCK_SENTINEL ||
                    (preferences.piqoraxConnectorConnected.value && preferences.piqoraxCoinDeviceLockActive.value)
                val label = if (isCoinDeviceLock && blockedAppLabelState.isBlank()) {
                    "Device Locked by PiqoraX"
                } else {
                    blockedAppLabelState.ifBlank { resolveAppLabel(pkg) }
                }
                var remainingResetMs by remember(pkg, initialResetMsState) {
                    mutableLongStateOf(
                        repository.getRemainingResetMs(pkg).takeIf { it > 0L } ?: initialResetMsState
                    )
                }

                LaunchedEffect(pkg, isCoinDeviceLock) {
                    while (true) {
                        if (isCoinDeviceLock) {
                            if (!preferences.piqoraxCoinDeviceLockActive.value) {
                                finishAndRemoveTaskSafe()
                                break
                            }
                        } else {
                            val decision = repository.evaluatePackageAccess(pkg)
                            remainingResetMs = decision.remainingResetMs
                            if (pkg.isNotBlank() && decision.isRestricted && !decision.shouldBlock) {
                                finishAndRemoveTaskSafe()
                                break
                            }
                        }
                        delay(1_000L)
                    }
                }

                var companionNotice by remember(pkg) { mutableStateOf<String?>(null) }

                TimeBlockLockScreen(
                    packageName = pkg,
                    appLabel = label,
                    remainingResetMs = remainingResetMs,
                    isCoinDeviceLock = isCoinDeviceLock,
                    companionNotice = companionNotice,
                    onGoBackExit = { navigateSafelyToHome() },
                    onRequestExtendedUnlock = { targetPkg ->
                        val handledByHook = if (!isCoinDeviceLock) {
                            repository.invokeExtendedUnlock(this@TimeBlockLockActivity, targetPkg)
                        } else {
                            false
                        }
                        if (!handledByHook) {
                            val passPkg = if (isCoinDeviceLock) packageName else targetPkg
                            val launched = launchPiqoraxCompanion(this@TimeBlockLockActivity, passPkg)
                            if (launched) {
                                companionNotice = null
                            } else {
                                val msg = "Piqorax companion study app ($PIQORAX_PACKAGE_NAME) is not installed on this device."
                                companionNotice = msg
                                Toast.makeText(this@TimeBlockLockActivity, msg, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyIntentExtras(intent)
    }

    private fun applyIntentExtras(intent: Intent?) {
        val pkg = intent?.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty()
        val label = intent?.getStringExtra(EXTRA_APP_LABEL).orEmpty()
        val resetMs = intent?.getLongExtra(EXTRA_REMAINING_RESET_MS, TimeBlockerEngine.HOURLY_WINDOW_MS)
            ?: TimeBlockerEngine.HOURLY_WINDOW_MS
        blockedPackageState = pkg
        blockedAppLabelState = label.ifBlank {
            if (pkg == PIQORAX_DEVICE_LOCK_SENTINEL) "Device Locked Until Unlock by Coin in PiqoraX" else resolveAppLabel(pkg)
        }
        initialResetMsState = resetMs.coerceIn(0L, 24L * TimeBlockerEngine.HOURLY_WINDOW_MS)
    }

    private fun resolveAppLabel(pkg: String): String {
        if (pkg.isBlank()) return "Restricted App"
        return try {
            val info = packageManager.getApplicationInfo(pkg, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            pkg
        }
    }

    private fun navigateSafelyToHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        runCatching { startActivity(homeIntent) }
        finishAndRemoveTaskSafe()
    }

    private fun finishAndRemoveTaskSafe() {
        runCatching { finishAndRemoveTask() }.onFailure { finish() }
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_time_block_package_name"
        const val EXTRA_APP_LABEL = "extra_time_block_app_label"
        const val EXTRA_REMAINING_RESET_MS = "extra_time_block_remaining_reset_ms"
        const val PIQORAX_PACKAGE_NAME = "com.ahsantcr.piqorax"
        const val EXTRA_REQUESTED_PACKAGE = "EXTRA_REQUESTED_PACKAGE"
        const val PIQORAX_DEVICE_LOCK_SENTINEL = "__piqorax_coin_device_lock__"

        fun launchPiqoraxCoinDeviceLock(context: Context, attemptedPackage: String) {
            val label = runCatching {
                val info = context.packageManager.getApplicationInfo(attemptedPackage, 0)
                context.packageManager.getApplicationLabel(info).toString()
            }.getOrDefault(attemptedPackage)
            val intent = Intent(context, TimeBlockLockActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                )
                putExtra(EXTRA_PACKAGE_NAME, PIQORAX_DEVICE_LOCK_SENTINEL)
                putExtra(EXTRA_APP_LABEL, label)
                putExtra(EXTRA_REMAINING_RESET_MS, 0L)
            }
            runCatching { context.startActivity(intent) }
        }

        fun launchPiqoraxCompanion(context: Context, targetPackage: String): Boolean {
            val launchIntent = context.packageManager
                .getLaunchIntentForPackage(PIQORAX_PACKAGE_NAME)
                ?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra(EXTRA_REQUESTED_PACKAGE, targetPackage)
                }
                ?: return false

            return runCatching {
                context.startActivity(launchIntent)
                true
            }.getOrDefault(false)
        }
    }
}

@Composable
fun TimeBlockLockScreen(
    packageName: String,
    appLabel: String,
    remainingResetMs: Long,
    isCoinDeviceLock: Boolean = false,
    companionNotice: String? = null,
    onGoBackExit: () -> Unit,
    onRequestExtendedUnlock: (String) -> Unit
) {
    val countdownText = remember(remainingResetMs) {
        TimeBlockerEngine.formatCountdown(remainingResetMs)
    }
    val ringProgress = remember(remainingResetMs, isCoinDeviceLock) {
        if (isCoinDeviceLock) 1f
        else (remainingResetMs.toFloat() / TimeBlockerEngine.HOURLY_WINDOW_MS.toFloat()).coerceIn(0.05f, 1f)
    }

    SheildlenzBackdrop(
        modifier = Modifier
            .fillMaxSize()
            .testTag("time_blocker_lock_root")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (isCoinDeviceLock) "Device Locked by PiqoraX" else "Hourly Allowance Paused",
                    color = AegisTextPrimary,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = ClassicalDisplayFont,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = appLabel.ifBlank { packageName },
                    color = AegisPurple,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalDisplayFont,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("time_blocker_blocked_app_label")
                )

                // Hero Animated Countdown Ring
                AnimatedCountdownRing(
                    progress = ringProgress,
                    centerText = "",
                    primaryColor = if (isCoinDeviceLock) AegisChampagneGold else AegisPrimaryCyan,
                    secondaryColor = AegisPurple,
                    accentColor = AegisGoldBright,
                    ringSize = 176.dp,
                    strokeWidth = 10.dp,
                    centerIconContent = {
                        Icon(
                            imageVector = Icons.Default.LockClock,
                            contentDescription = "App Time Limit Reached",
                            tint = if (isCoinDeviceLock) AegisChampagneGold else AegisPrimaryCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isCoinDeviceLock) "COIN" else countdownText,
                            color = AegisTextPrimary,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.testTag("time_blocker_countdown_text")
                        )
                        Text(
                            text = if (isCoinDeviceLock) "COIN LOCK" else "RESET TIMER",
                            color = AegisTextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            fontFamily = ClassicalBodyFont
                        )
                    }
                )

                Button(
                    onClick = onGoBackExit,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AegisPrimaryCyan,
                        contentColor = AegisOnPrimaryWhite
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("time_blocker_exit_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Go Back / Exit",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalDisplayFont
                    )
                }

                OutlinedButton(
                    onClick = { onRequestExtendedUnlock(packageName) },
                    border = BorderStroke(1.2.dp, AegisPurple.copy(alpha = 0.65f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White.copy(alpha = 0.72f),
                        contentColor = AegisPurple
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("time_blocker_extended_unlock_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isCoinDeviceLock) "Unlock by Coin in PiqoraX" else "Request Extended Unlock",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalDisplayFont
                    )
                }

                if (packageName.isNotBlank()) {
                    Text(
                        text = packageName,
                        color = AegisTextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("time_blocker_blocked_package_name")
                    )
                }

                ExpandableDetailsToggle(
                    summaryLabel = "Allowance & unlock details",
                    accentColor = AegisPurple,
                    testTagPrefix = "time_blocker_details"
                ) {
                    Text(
                        text = if (isCoinDeviceLock) {
                            "Lock the device until unlock by coin in PiqoraX. Open PiqoraX and spend study coins to unlock apps."
                        } else {
                            "Your 5-minute hourly base allowance for this app has finished. Monotonic uptime timer protected."
                        },
                        color = AegisTextSecondary,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        fontFamily = ClassicalBodyFont
                    )
                }

                if (!companionNotice.isNullOrBlank()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("time_blocker_companion_missing_notice"),
                        shape = RoundedCornerShape(14.dp),
                        color = AegisChampagneSoft,
                        border = BorderStroke(1.dp, AegisGoldBorder)
                    ) {
                        Text(
                            text = companionNotice,
                            color = AegisTextPrimary,
                            fontSize = 12.5.sp,
                            fontFamily = ClassicalBodyFont,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}
