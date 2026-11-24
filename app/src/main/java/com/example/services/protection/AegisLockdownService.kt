package com.example.services.protection

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.core.notifications.AegisNotificationHelper
import com.example.di.AegisContainer

class AegisLockdownService : Service() {

    private val container by lazy { AegisContainer.getInstance(this) }
    private val overlayController get() = container.overlayController
    private val watchdogHandler = Handler(Looper.getMainLooper())

    private val watchdogRunnable = object : Runnable {
        override fun run() {
            if (!container.preferences.masterPowerEnabled.value) {
                overlayController.dismissBlockOverlay()
                @Suppress("DEPRECATION")
                stopForeground(true)
                stopSelf()
                return
            }
            if (container.preferences.isLockdownActive()) {
                overlayController.showLockdownOverlay(container.preferences.lockdownIncidentInfo.value)
                watchdogHandler.postDelayed(this, 2000L)
            } else {
                overlayController.dismissBlockOverlay()
                @Suppress("DEPRECATION")
                stopForeground(true)
                stopSelf()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!container.preferences.masterPowerEnabled.value) {
            watchdogHandler.removeCallbacks(watchdogRunnable)
            overlayController.dismissBlockOverlay()
            @Suppress("DEPRECATION")
            stopForeground(true)
            stopSelf()
            return START_NOT_STICKY
        }
        when (intent?.action) {
            ACTION_STOP_LOCKDOWN -> {
                watchdogHandler.removeCallbacks(watchdogRunnable)
                overlayController.dismissBlockOverlay()
                @Suppress("DEPRECATION")
                stopForeground(true)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                if (!container.preferences.isLockdownActive()) {
                    try {
                        startForegroundCompat(buildNotification())
                        @Suppress("DEPRECATION")
                        stopForeground(true)
                    } catch (_: Throwable) {
                    }
                    stopSelf()
                    return START_NOT_STICKY
                }
                val fgsStarted = try {
                    startForegroundCompat(buildNotification())
                    true
                } catch (t: Throwable) {
                    Log.w(TAG, "Lockdown startForeground degraded cleanly: ${t.message}")
                    false
                }
                overlayController.showLockdownOverlay(container.preferences.lockdownIncidentInfo.value)
                watchdogHandler.removeCallbacks(watchdogRunnable)
                watchdogHandler.postDelayed(watchdogRunnable, 2000L)
                Log.i(TAG, "AegisLockdownService watchdog engaged (foreground=$fgsStarted).")
                return START_STICKY
            }
        }
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        watchdogHandler.removeCallbacks(watchdogRunnable)
        if (!container.preferences.isLockdownActive()) {
            overlayController.dismissBlockOverlay()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sheildlenz Device Lockdown",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Enforces active Sheildlenz device lockdown timer across restarts"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pi = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val notificationsOn = container.preferences.notificationsEnabled.value
        val discreet = container.preferences.discreetBlockingScreenMode.value
        val channelId = if (notificationsOn) {
            CHANNEL_ID
        } else {
            AegisNotificationHelper.ensureSilentChannel(this)
            AegisNotificationHelper.SILENT_MONITOR_CHANNEL_ID
        }

        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(if (discreet || !notificationsOn) "Display Policy Active" else "Sheildlenz Lockdown Active")
            .setContentText(if (discreet || !notificationsOn) "Device access policy running" else "Device locked due to detected content violation")
            .setContentIntent(pi)
            .setOngoing(true)
            .setSilent(!notificationsOn)
            .setPriority(if (notificationsOn) NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_MIN)
            .build()
    }

    companion object {
        private const val TAG = "AegisLockdownService"
        const val CHANNEL_ID = "aegis_lockdown_channel"
        const val NOTIFICATION_ID = 1002
        const val ACTION_START_LOCKDOWN = "com.example.action.START_LOCKDOWN"
        const val ACTION_STOP_LOCKDOWN = "com.example.action.STOP_LOCKDOWN"

        fun start(context: Context) {
            if (!AegisContainer.getInstance(context).preferences.masterPowerEnabled.value) return
            val intent = Intent(context, AegisLockdownService::class.java).apply {
                action = ACTION_START_LOCKDOWN
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Unable to start foreground lockdown service directly; trying overlay fallback: ${t.message}")
                try {
                    val container = AegisContainer.getInstance(context)
                    if (container.preferences.masterPowerEnabled.value && container.preferences.isLockdownActive()) {
                        container.overlayController.showLockdownOverlay(container.preferences.lockdownIncidentInfo.value)
                    }
                } catch (_: Throwable) {
                }
            }
        }

        fun stop(context: Context) {
            val appContext = context.applicationContext
            runCatching {
                appContext.stopService(Intent(appContext, AegisLockdownService::class.java))
            }
        }
    }
}
