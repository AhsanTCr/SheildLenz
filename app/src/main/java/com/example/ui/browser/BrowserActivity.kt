package com.example.ui.browser

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.core.audio.AegisSoundEffects
import com.example.di.AegisContainer
import com.example.ui.theme.AegisShieldTheme
import com.example.ui.theme.AegisVoidBackground

/**
 * Standalone edge-to-edge Chrome-alternative browser window.
 * Launched from pinned Home Screen shortcuts, Home Screen Widget, and external http/https links.
 */
class BrowserActivity : ComponentActivity() {

    private val container by lazy { AegisContainer.getInstance(this) }
    private var pendingUrlState by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AegisSoundEffects.setEnabled(container.preferences.touchSoundsEnabled.value)
        AegisSoundEffects.setAutoSelectorEnabled(container.preferences.autoSoundSelectorEnabled.value)
        AegisSoundEffects.prewarm(applicationContext)
        pendingUrlState = extractTargetUrl(intent)

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
                    SafeBrowserScreen(
                        preferences = container.preferences,
                        protectionRepo = container.protectionRepository,
                        blockedDnsLogRepo = container.blockedDnsLogRepository,
                        overlayController = container.overlayController,
                        pendingExternalUrl = pendingUrlState,
                        onConsumePendingUrl = { pendingUrlState = null },
                        isStandaloneWindow = true,
                        onNavigateBack = { finish() }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractTargetUrl(intent)?.let { incoming ->
            pendingUrlState = incoming
        }
    }

    override fun onResume() {
        super.onResume()
        container.preferences.consumeAuthorizedExternalFlow()
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            val isScreenOff = powerManager?.isInteractive == false
            container.preferences.onAppBackgrounded(forceLock = isScreenOff)
        }
    }

    private fun extractTargetUrl(intent: Intent?): String? {
        if (intent == null) return null
        val fromExtra = intent.getStringExtra(EXTRA_TARGET_URL)?.trim()
        val fromData = intent.dataString?.trim()
        val candidate = fromExtra?.takeIf { it.isNotBlank() } ?: fromData?.takeIf { it.isNotBlank() }
        return candidate?.takeIf {
            it.startsWith("http://", true) ||
                it.startsWith("https://", true) ||
                it.equals(NEW_TAB_INTERNAL_URL, true)
        }
    }

    companion object {
        const val EXTRA_TARGET_URL = "extra_target_url"
        const val EXTRA_OPEN_NEW_TAB = "extra_open_new_tab"
    }
}
