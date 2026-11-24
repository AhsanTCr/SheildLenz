package com.example.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object AegisNotificationHelper {
    const val SILENT_MONITOR_CHANNEL_ID = "system_display_sync_silent"
    const val PAUSED_ALERT_NOTIFICATION_ID = 1004
    const val REBOOT_SHIELD_NOTIFICATION_ID = 1003

    fun cancelOptionalAlerts(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        runCatching {
            nm.cancel(PAUSED_ALERT_NOTIFICATION_ID)
            nm.cancel(REBOOT_SHIELD_NOTIFICATION_ID)
        }
    }

    fun cancelAllProtectionNotifications(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        runCatching {
            nm.cancel(1001) // AegisProtectionService
            nm.cancel(1002) // AegisLockdownService
            nm.cancel(REBOOT_SHIELD_NOTIFICATION_ID)
            nm.cancel(PAUSED_ALERT_NOTIFICATION_ID)
            nm.cancel(1009) // AegisSafeDnsVpnService
        }
    }

    fun ensureSilentChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java) ?: return
            if (nm.getNotificationChannel(SILENT_MONITOR_CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    SILENT_MONITOR_CHANNEL_ID,
                    "Background Display Service",
                    NotificationManager.IMPORTANCE_MIN
                ).apply {
                    description = "Quiet background policy synchronization"
                    setShowBadge(false)
                    setSound(null, null)
                    enableVibration(false)
                }
                nm.createNotificationChannel(channel)
            }
        }
    }
}
