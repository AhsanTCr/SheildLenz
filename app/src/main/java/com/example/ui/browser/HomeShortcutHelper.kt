package com.example.ui.browser

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.ui.components.createSafeBrowserBrandBitmap
import java.util.Locale

object HomeShortcutHelper {

    private const val SHORTCUT_BROWSER_ID = "sheildlenz_standalone_browser_v1"

    fun buildStandaloneBrowserIntent(
        context: Context,
        targetUrl: String? = null,
        forceNewTab: Boolean = false
    ): Intent {
        return Intent(context, BrowserActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            val cleanUrl = targetUrl?.trim()?.takeIf { it.isNotBlank() } ?: NEW_TAB_INTERNAL_URL
            data = Uri.parse(cleanUrl)
            putExtra(BrowserActivity.EXTRA_TARGET_URL, cleanUrl)
            putExtra(BrowserActivity.EXTRA_OPEN_NEW_TAB, forceNewTab)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
    }

    fun canPinHomeShortcuts(context: Context): Boolean {
        return runCatching {
            ShortcutManagerCompat.isRequestPinShortcutSupported(context)
        }.getOrDefault(false)
    }

    fun pushDynamicBrowserShortcut(context: Context): Boolean {
        return runCatching {
            val icon = IconCompat.createWithBitmap(createSafeBrowserBrandBitmap(sizePx = 256, isProUnlocked = true))
            val shortcut = ShortcutInfoCompat.Builder(context, SHORTCUT_BROWSER_ID)
                .setShortLabel("Safe Browser")
                .setLongLabel("Sheildlenz Browser")
                .setIcon(icon)
                .setIntent(buildStandaloneBrowserIntent(context, NEW_TAB_INTERNAL_URL, forceNewTab = false))
                .build()
            ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
        }.getOrDefault(false)
    }

    fun pinStandaloneBrowserToHome(context: Context): Boolean {
        return runCatching {
            val icon = IconCompat.createWithBitmap(createSafeBrowserBrandBitmap(sizePx = 256, isProUnlocked = true))
            val shortcut = ShortcutInfoCompat.Builder(context, SHORTCUT_BROWSER_ID)
                .setShortLabel("Safe Browser")
                .setLongLabel("Sheildlenz Browser")
                .setIcon(icon)
                .setIntent(buildStandaloneBrowserIntent(context, NEW_TAB_INTERNAL_URL, forceNewTab = false))
                .build()
            runCatching { ShortcutManagerCompat.pushDynamicShortcut(context, shortcut) }
            ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
        }.getOrDefault(false)
    }

    fun pinWebsiteToHome(
        context: Context,
        title: String,
        url: String
    ): Boolean {
        val cleanUrl = url.trim().takeIf {
            it.startsWith("http://", true) || it.startsWith("https://", true)
        } ?: return false
        return runCatching {
            val host = Uri.parse(cleanUrl).host?.removePrefix("www.") ?: "Website"
            val shortLabel = title.trim().ifBlank { host }.take(18)
            val longLabel = title.trim().ifBlank { host }.take(36)
            val shortcutId = "slz_site_${cleanUrl.hashCode().toUInt()}"
            val icon = IconCompat.createWithBitmap(createSafeBrowserBrandBitmap(sizePx = 256, isProUnlocked = true))
            val shortcut = ShortcutInfoCompat.Builder(context, shortcutId)
                .setShortLabel(shortLabel)
                .setLongLabel(longLabel)
                .setIcon(icon)
                .setIntent(buildStandaloneBrowserIntent(context, cleanUrl, forceNewTab = true))
                .build()
            ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
        }.getOrDefault(false)
    }

    fun isDefaultBrowserRoleHeld(context: Context): Boolean {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val rm = context.getSystemService(RoleManager::class.java)
                if (rm != null && rm.isRoleHeld(RoleManager.ROLE_BROWSER)) {
                    return@runCatching true
                }
            }
            val browseIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
            }
            val resolved = context.packageManager.resolveActivity(browseIntent, PackageManager.MATCH_DEFAULT_ONLY)
            resolved?.activityInfo?.packageName?.lowercase(Locale.US) == context.packageName.lowercase(Locale.US)
        }.getOrDefault(false)
    }
}
