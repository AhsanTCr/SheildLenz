package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.di.AegisContainer
import com.example.ui.browser.HomeShortcutHelper
import com.example.ui.components.createSafeBrowserBrandBitmap

class SheildlenzAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateSingleWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_WIDGET) {
            updateAllWidgets(context)
        }
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.ahsan.tcr.sheildlenz.ACTION_REFRESH_WIDGET"

        fun requestWidgetRefresh(context: Context) {
            runCatching {
                updateAllWidgets(context.applicationContext ?: context)
            }
        }

        fun updateAllWidgets(context: Context) {
            val appCtx = context.applicationContext ?: context
            val manager = AppWidgetManager.getInstance(appCtx) ?: return
            val component = ComponentName(appCtx, SheildlenzAppWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isEmpty()) return
            for (widgetId in ids) {
                updateSingleWidget(appCtx, manager, widgetId)
            }
        }

        internal fun buildRemoteViews(context: Context): RemoteViews {
            val appCtx = context.applicationContext ?: context
            val container = AegisContainer.getInstance(appCtx)
            val prefs = container.preferences
            val telemetry = container.protectionRepository.telemetry.value

            val isLockdown = prefs.isLockdownActive()
            val isMasterOn = prefs.masterPowerEnabled.value
            val isShieldActive = isMasterOn && (telemetry.isProtectionEnabled || prefs.isShieldDesiredActive.value)

            val (statusLabel, statusColorHex) = when {
                isLockdown -> "LOCKDOWN" to "#E59712"
                !isMasterOn -> "POWER OFF" to "#64748B"
                isShieldActive -> "ARMED" to "#2563EB"
                else -> "STANDBY" to "#7C5CFA"
            }

            val blockedTotal = container.blockedDnsLogRepository.liveLogs.value.size +
                prefs.safeSearchBlockedCount.value
            val dnsPrefix = if (prefs.safeDnsEnabled.value) "Safe DNS ON" else "Safe DNS Standby"
            val blockedSubtitle = if (blockedTotal == 1) {
                "$dnsPrefix • 1 block today"
            } else {
                "$dnsPrefix • $blockedTotal blocks today"
            }

            val remainingStudyMillis = if (prefs.studyModeEnabled.value) prefs.getRemainingStudyMillis() else 0L
            val remainingLockdownMillis = if (isLockdown) prefs.getRemainingLockdownMillis() else 0L
            val (timerLabel, timerValue, timerColorHex) = when {
                isLockdown -> {
                    val mins = ((remainingLockdownMillis + 59_999L) / 60_000L).coerceAtLeast(1L)
                    Triple("Lockdown Timer Active", "${mins}m left", "#E59712")
                }
                prefs.studyModeEnabled.value && remainingStudyMillis > 0L -> {
                    val mins = ((remainingStudyMillis + 59_999L) / 60_000L).coerceAtLeast(1L)
                    Triple("Study Focus Timer", "${mins}m left", "#7C5CFA")
                }
                prefs.isWithinBedtimeLockWindow() -> {
                    Triple("Bedtime Quiet Lock", "ACTIVE", "#E59712")
                }
                else -> {
                    val reelsCount = prefs.dailyReelsTotalCount.value
                    val reelsLimit = prefs.scrollLimitCount.value
                    Triple("Reels & App Guard", "$reelsCount / $reelsLimit", "#2563EB")
                }
            }

            val views = RemoteViews(appCtx.packageName, R.layout.widget_sheildlenz_home)
            views.setTextViewText(R.id.widget_shield_status_badge, statusLabel)
            views.setTextColor(R.id.widget_shield_status_badge, Color.parseColor(statusColorHex))
            views.setTextViewText(R.id.widget_blocked_count, blockedSubtitle)
            views.setTextViewText(R.id.widget_timer_label, timerLabel)
            views.setTextViewText(R.id.widget_timer_value, timerValue)
            views.setTextColor(R.id.widget_timer_value, Color.parseColor(timerColorHex))

            val browserBitmap = createSafeBrowserBrandBitmap(
                sizePx = 128,
                isProUnlocked = prefs.fullBrowserEngineInstalled.value
            )
            views.setImageViewBitmap(R.id.widget_browser_icon, browserBitmap)

            val shieldIntent = Intent(appCtx, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            val shieldPending = PendingIntent.getActivity(
                appCtx,
                9101,
                shieldIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_shield, shieldPending)

            val browserIntent = HomeShortcutHelper.buildStandaloneBrowserIntent(appCtx)
            val browserPending = PendingIntent.getActivity(
                appCtx,
                9102,
                browserIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_browser, browserPending)
            views.setOnClickPendingIntent(R.id.widget_browser_icon, browserPending)

            return views
        }

        private fun updateSingleWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            runCatching {
                val views = buildRemoteViews(context)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}
