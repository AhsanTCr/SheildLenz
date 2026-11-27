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
                isLockdown -> "LOCKDOWN" to "#D9820B"
                !isMasterOn -> "POWER OFF" to "#5D76B0"
                isShieldActive -> "ARMED" to "#0D9488"
                else -> "STANDBY" to "#2563EB"
            }

            val blockedTotal = container.blockedDnsLogRepository.liveLogs.value.size +
                prefs.safeSearchBlockedCount.value
            val blockedSubtitle = if (blockedTotal == 1) {
                "1 threat blocked safely"
            } else {
                "$blockedTotal threats blocked safely"
            }

            val views = RemoteViews(appCtx.packageName, R.layout.widget_sheildlenz_home)
            views.setTextViewText(R.id.widget_shield_status_badge, statusLabel)
            views.setTextColor(R.id.widget_shield_status_badge, Color.parseColor(statusColorHex))
            views.setTextViewText(R.id.widget_blocked_count, blockedSubtitle)

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
