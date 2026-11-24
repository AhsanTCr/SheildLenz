package com.example.services.protection

import android.content.ComponentName
import android.content.Context
import android.net.VpnService
import android.os.Build
import android.service.quicksettings.TileService
import com.example.core.notifications.AegisNotificationHelper
import com.example.di.AegisContainer
import com.example.monitoring.accessibility.AegisAccessibilityService
import com.example.services.dns.AegisSafeDnsVpnService

/**
 * Coordinates the global Main Power Switch from AboutScreen.
 *
 * When switched OFF, terminates all background services, foreground notifications,
 * Firestore sync listeners, overlay windows, anti-tamper guards, and Accessibility
 * processing so Sheildlenz consumes 0% battery with zero running background processes.
 */
object MasterPowerController {

    fun setMasterPowerEnabled(context: Context, enabled: Boolean) {
        val appContext = context.applicationContext
        val container = AegisContainer.getInstance(appContext)
        container.preferences.setMasterPowerEnabled(enabled)
        if (enabled) {
            restoreOnPowerUp(appContext, container)
        } else {
            engageZeroBatteryShutdown(appContext, container)
        }
    }

    fun engageZeroBatteryShutdown(context: Context, existingContainer: AegisContainer? = null) {
        val appContext = context.applicationContext
        val container = existingContainer ?: AegisContainer.getInstance(appContext)
        val prefs = container.preferences

        // 1. Disarm shield & study timers so nothing auto-arms or runs in background
        prefs.setShieldDesiredActive(false)
        prefs.stopStudySession()

        // 2. Stop all protection, lockdown, and Safe DNS VPN services cleanly
        AegisProtectionService.stop(appContext)
        AegisLockdownService.stop(appContext)
        AegisSafeDnsVpnService.stop(appContext)

        // 3. Cancel real-time Firestore parent-child background sync listeners
        container.parentChildSyncController.stop()

        // 4. Dismiss any active overdraw / system overlay windows immediately
        container.overlayController.dismissBlockOverlay()

        // 5. Disconnect AccessibilityService cleanly (0% background processing & no anti-tamper)
        AegisAccessibilityService.disableServiceIfRunning()

        // 6. Clear protection/lockdown/DNS notifications without touching update system
        AegisNotificationHelper.cancelAllProtectionNotifications(appContext)

        // 7. Update telemetry repository to reflect zero active background subsystems
        container.protectionRepository.setProtectionEnabled(false)
        container.protectionRepository.updateMediaProjectionState(false)
        container.protectionRepository.updateAccessibilityState(false)
        container.protectionRepository.updateActivePackage(null, "Master Power Off")

        refreshQuickTile(appContext)
    }

    private fun restoreOnPowerUp(appContext: Context, container: AegisContainer) {
        val prefs = container.preferences
        container.parentChildSyncController.ensureStarted()
        if (prefs.safeDnsEnabled.value && runCatching { VpnService.prepare(appContext) == null }.getOrDefault(false)) {
            AegisSafeDnsVpnService.start(appContext)
        }
        if (prefs.isLockdownActive()) {
            AegisLockdownService.start(appContext)
        }
        container.protectionRepository.refreshPermissionHealth()
        refreshQuickTile(appContext)
    }

    private fun refreshQuickTile(appContext: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            runCatching {
                TileService.requestListeningState(
                    appContext,
                    ComponentName(appContext, AegisQuickTileService::class.java)
                )
            }
        }
    }
}
