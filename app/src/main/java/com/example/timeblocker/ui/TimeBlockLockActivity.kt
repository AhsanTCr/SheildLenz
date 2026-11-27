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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.HourglassTop
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.di.AegisContainer
import com.example.timeblocker.TimeBlockerEngine
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberCardTone
import com.example.ui.components.SheildlenzBackdrop
import com.example.ui.theme.AegisCardBackground
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisChampagneSoft
import com.example.ui.theme.AegisGoldBorder
import com.example.ui.theme.AegisOnPrimaryWhite
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisPurple
import com.example.ui.theme.AegisShieldTheme
import com.example.ui.theme.AegisTextMuted
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.AegisWarningAmber
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

        val repository = AegisContainer.getInstance(this).timeBlockerRepository

        setContent {
            AegisShieldTheme {
                // Non-dismissible: consume system back button while lock overlay is shown
                BackHandler(enabled = true) { }

                val pkg = blockedPackageState
                val label = blockedAppLabelState.ifBlank { resolveAppLabel(pkg) }
                var remainingResetMs by remember(pkg, initialResetMsState) {
                    mutableLongStateOf(
                        repository.getRemainingResetMs(pkg).takeIf { it > 0L } ?: initialResetMsState
                    )
                }

                LaunchedEffect(pkg) {
                    while (true) {
                        val decision = repository.evaluatePackageAccess(pkg)
                        remainingResetMs = decision.remainingResetMs
                        if (pkg.isNotBlank() && decision.isRestricted && !decision.shouldBlock) {
                            finishAndRemoveTaskSafe()
                            break
                        }
                        delay(1_000L)
                    }
                }

                var companionNotice by remember(pkg) { mutableStateOf<String?>(null) }

                TimeBlockLockScreen(
                    packageName = pkg,
                    appLabel = label,
                    remainingResetMs = remainingResetMs,
                    companionNotice = companionNotice,
                    onGoBackExit = { navigateSafelyToHome() },
                    onRequestExtendedUnlock = { targetPkg ->
                        val handledByHook = repository.invokeExtendedUnlock(this@TimeBlockLockActivity, targetPkg)
                        if (!handledByHook) {
                            val launched = launchPiqoraxCompanion(this@TimeBlockLockActivity, targetPkg)
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
        blockedAppLabelState = label.ifBlank { resolveAppLabel(pkg) }
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
    companionNotice: String? = null,
    onGoBackExit: () -> Unit,
    onRequestExtendedUnlock: (String) -> Unit
) {
    val countdownText = remember(remainingResetMs) {
        TimeBlockerEngine.formatCountdown(remainingResetMs)
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
                .padding(horizontal = 20.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(AegisPrimaryCyan, AegisPurple)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LockClock,
                        contentDescription = "App Time Limit Reached",
                        tint = AegisOnPrimaryWhite,
                        modifier = Modifier.size(42.dp)
                    )
                }

                Text(
                    text = "Hourly Allowance Paused",
                    color = AegisTextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = ClassicalDisplayFont,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Your 5-minute hourly base allowance for this app has finished.",
                    color = AegisTextSecondary,
                    fontSize = 14.sp,
                    fontFamily = ClassicalBodyFont,
                    textAlign = TextAlign.Center
                )

                CyberCard(
                    tone = CyberCardTone.HERO,
                    borderColor = AegisCardBorder,
                    modifier = Modifier.testTag("time_blocker_app_info_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AegisChampagneSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.HourglassTop,
                                contentDescription = null,
                                tint = AegisChampagneGold,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = appLabel.ifBlank { packageName },
                                color = AegisTextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = ClassicalDisplayFont,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("time_blocker_blocked_app_label")
                            )
                            if (packageName.isNotBlank()) {
                                Text(
                                    text = packageName,
                                    color = AegisTextMuted,
                                    fontSize = 11.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("time_blocker_blocked_package_name")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = AegisCardBackground,
                        border = BorderStroke(1.dp, AegisCardBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp, horizontal = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "NEXT 5-MIN RESET IN",
                                color = AegisTextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp,
                                fontFamily = ClassicalBodyFont
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = countdownText,
                                color = AegisPrimaryCyan,
                                fontSize = 38.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.testTag("time_blocker_countdown_text")
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Monotonic uptime timer protected",
                                color = AegisWarningAmber,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = ClassicalBodyFont
                            )
                        }
                    }
                }

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
                        text = "Request Extended Unlock",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalDisplayFont
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
