package com.example

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.core.audio.AegisSoundEffects
import com.example.core.permissions.PermissionHelper
import com.example.core.update.UpdateChecker
import com.example.data.preferences.AegisPreferences
import com.example.data.repository.ParentChildRepository
import com.example.di.AegisContainer
import com.example.services.protection.AegisLockdownService
import com.example.services.protection.AegisProtectionService
import com.example.ui.navigation.AegisNavHost
import com.example.ui.theme.AegisShieldTheme
import com.example.ui.theme.AegisVoidBackground

class MainActivity : ComponentActivity() {
    private val container by lazy { AegisContainer.getInstance(this) }
    private var promptedShieldRearmInSession = false

    override fun attachBaseContext(newBase: Context) {
        val langCode = runCatching { AegisPreferences(newBase).appLanguageCode.value }
            .getOrDefault("system")
        if (langCode.isBlank() || langCode == "system") {
            super.attachBaseContext(newBase)
            return
        }
        val locale = Locale.forLanguageTag(langCode)
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    @SuppressLint("InvalidFragmentVersionForActivityResult")
    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            container.preferences.setShieldDesiredActive(true)
            AegisProtectionService.start(this, result.resultCode, result.data!!)
            container.protectionRepository.setProtectionEnabled(true)
            Toast.makeText(this, "Sheildlenz Armed • Screen Protection Active", Toast.LENGTH_SHORT).show()
        } else {
            container.protectionRepository.setProtectionEnabled(false)
            Toast.makeText(this, "Screen capture permission not granted.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handlePairingDeepLink(intent)
        container.parentChildSyncController.ensureStarted()
        AegisSoundEffects.setEnabled(container.preferences.touchSoundsEnabled.value)
        AegisSoundEffects.prewarm(this.applicationContext)
        // Silently check latest GitHub release vs current app version on startup & pre-download at 100 KB/s
        UpdateChecker.startCheckForUpdates(this, showUpToDatePopup = false)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        setContent {
            AegisShieldTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AegisVoidBackground
                ) {
                    AegisNavHost(
                        container = container,
                        onRequestStartProtection = { requestStartProtection() },
                        onRequestStopProtection = { requestStopProtection() }
                    )
                }
            }
        }
    }

    private fun requestStartProtection() {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
        if (projectionManager != null) {
            container.preferences.beginAuthorizedExternalFlow()
            val captureIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                projectionManager.createScreenCaptureIntent(
                    MediaProjectionConfig.createConfigForDefaultDisplay()
                )
            } else {
                projectionManager.createScreenCaptureIntent()
            }
            mediaProjectionLauncher.launch(captureIntent)
        } else {
            Toast.makeText(this, "MediaProjection service unavailable on this device.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestStopProtection() {
        container.preferences.setShieldDesiredActive(false)
        AegisProtectionService.stop(this)
        container.protectionRepository.setProtectionEnabled(false)
        Toast.makeText(this, "Sheildlenz Protection Disengaged.", Toast.LENGTH_SHORT).show()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePairingDeepLink(intent)
        if (intent.getBooleanExtra(EXTRA_REARM_SHIELD, false)) {
            promptedShieldRearmInSession = false
        }
    }

    private fun handlePairingDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "sheildlenz" && data.host == "pair") {
            val code = data.getQueryParameter("code")
                ?: ParentChildRepository.extractPairingCode(data.toString())
            if (!code.isNullOrBlank()) {
                container.setPendingPairingCode(code)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        container.protectionRepository.refreshPermissionHealth()
        container.parentChildSyncController.ensureStarted()
        UpdateChecker.retryPendingInstallIfAuthorized(this)
        if (container.preferences.isLockdownActive()) {
            AegisLockdownService.start(this)
        } else if (container.preferences.isShieldDesiredActive.value) {
            val telemetry = container.protectionRepository.telemetry.value
            val needsRearm = !telemetry.isMediaProjectionActive || !telemetry.isProtectionEnabled
            if (needsRearm && PermissionHelper.hasOverlayPermission(this) && !promptedShieldRearmInSession) {
                promptedShieldRearmInSession = true
                requestStartProtection()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Require Guardian re-authentication whenever the app leaves the foreground
        if (!isChangingConfigurations) {
            container.preferences.onAppBackgrounded()
        }
    }

    companion object {
        const val EXTRA_REARM_SHIELD = "extra_rearm_shield"
    }
}
