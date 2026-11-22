package com.example.core.update

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

/**
 * Keeps active APK update and AI model (.zip/.tflite) downloads alive at foreground
 * priority even if the user minimizes Sheildlenz or switches apps.
 */
class AegisBackgroundDownloadService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        ensureNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "Sheildlenz Update"
        val subtitle = intent?.getStringExtra(EXTRA_SUBTITLE) ?: "Downloading update..."
        val progressPct = intent?.getIntExtra(EXTRA_PROGRESS, -1) ?: -1

        val promoted = try {
            promoteToForeground(buildNotification(title, subtitle, progressPct))
        } catch (_: Throwable) {
            false
        }
        if (intent?.action == ACTION_STOP || !promoted) {
            stopForegroundCompat()
            stopSelf()
            return START_NOT_STICKY
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        lastNotifyTimeMs = 0L
        stopForegroundCompat()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        try {
            if (UpdateChecker.hasIncompletePersistedDownload(applicationContext)) {
                AegisUpdateResumeJobService.schedule(applicationContext)
            }
        } catch (_: Throwable) {
        }
    }

    private fun promoteToForeground(notification: Notification): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            true
        } catch (t: Throwable) {
            android.util.Log.w("AegisDownloadSvc", "Unable to promote to foreground: ${t.message}")
            false
        }
    }

    private fun stopForegroundCompat() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (_: Throwable) {
        }
    }

    private fun ensureNotificationChannel() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val nm = getSystemService(NotificationManager::class.java) ?: return
                if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                    val channel = NotificationChannel(
                        CHANNEL_ID,
                        "Downloads & Updates",
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        description = "Shows progress for Sheildlenz app updates and AI model downloads."
                        setShowBadge(false)
                    }
                    nm.createNotificationChannel(channel)
                }
            }
        } catch (_: Throwable) {
        }
    }

    private fun buildNotification(title: String, subtitle: String, progressPct: Int): Notification {
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingTap = PendingIntent.getActivity(
            this,
            202,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_sheildlenz_update)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(pendingTap)

        if (progressPct in 0..100) {
            builder.setProgress(100, progressPct, false)
        } else {
            builder.setProgress(100, 0, true)
        }
        return builder.build()
    }

    companion object {
        private const val CHANNEL_ID = "sheildlenz_downloads"
        private const val NOTIFICATION_ID = 4401
        private const val ACTION_UPDATE_PROGRESS = "com.example.action.UPDATE_DOWNLOAD_PROGRESS"
        private const val ACTION_STOP = "com.example.action.STOP_DOWNLOAD_SERVICE"
        private const val EXTRA_TITLE = "extra_title"
        private const val EXTRA_SUBTITLE = "extra_subtitle"
        private const val EXTRA_PROGRESS = "extra_progress"

        @Volatile
        private var lastNotifyTimeMs: Long = 0L

        @Volatile
        private var isRunning: Boolean = false

        fun notifyProgress(
            context: Context,
            title: String,
            subtitle: String,
            progressPct: Int,
            force: Boolean = false,
            useForegroundService: Boolean = true
        ) {
            if (!useForegroundService) return
            val now = System.currentTimeMillis()
            if (!force && now - lastNotifyTimeMs < 450L) return
            lastNotifyTimeMs = now
            try {
                val appCtx = context.applicationContext
                val intent = Intent(appCtx, AegisBackgroundDownloadService::class.java).apply {
                    action = ACTION_UPDATE_PROGRESS
                    putExtra(EXTRA_TITLE, title)
                    putExtra(EXTRA_SUBTITLE, subtitle)
                    putExtra(EXTRA_PROGRESS, progressPct)
                }
                ContextCompat.startForegroundService(appCtx, intent)
            } catch (_: Throwable) {
                // Safe no-op in unit tests or Android 14+ background-start restriction cases
            }
        }

        fun stop(context: Context) {
            lastNotifyTimeMs = 0L
            if (!isRunning) return
            try {
                val appCtx = context.applicationContext
                appCtx.stopService(Intent(appCtx, AegisBackgroundDownloadService::class.java))
            } catch (_: Throwable) {
            }
        }
    }
}
