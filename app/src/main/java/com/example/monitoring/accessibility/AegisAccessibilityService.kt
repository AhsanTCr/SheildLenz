package com.example.monitoring.accessibility

import android.accessibilityservice.AccessibilityService
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.core.models.SecurityIncident
import com.example.core.models.ThreatCategory
import com.example.core.models.ThreatSeverity
import com.example.data.preferences.AegisPreferences
import com.example.di.AegisContainer
import com.example.security.deviceowner.AegisDeviceAdminReceiver
import com.example.services.protection.AegisLockdownService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AegisAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val container by lazy { AegisContainer.getInstance(this) }
    private val overlayController get() = container.overlayController
    private var lastTamperInterceptTimeMs: Long = 0L
    private var lastLockdownInterceptTimeMs: Long = 0L
    private var lastRecentsInterceptTimeMs: Long = 0L
    private var lastBrowserScanTimeMs: Long = 0L
    private var lastBrowserBlockTimeMs: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "AegisAccessibilityService connected with Chrome Web Guard, anti-tamper & app eviction.")
        container.protectionRepository.updateAccessibilityState(true)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val eventType = event.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) {
            return
        }

        val resolvedPkg = event.packageName?.toString()?.takeIf { it.isNotBlank() }
            ?: safeGetRootPackageName()
            ?: return
        val prefs = container.preferences
        val now = SystemClock.uptimeMillis()

        // 1. Whole-Device Lockdown Enforcement
        if (prefs.isLockdownActive() && !isAllowedDuringLockdown(resolvedPkg)) {
            if (eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
                now - lastLockdownInterceptTimeMs > 600L
            ) {
                lastLockdownInterceptTimeMs = now
                Log.w(TAG, "Lockdown active — blocking access to $resolvedPkg and restoring full-screen lock overlay.")
                performGlobalAction(GLOBAL_ACTION_HOME)
                overlayController.showLockdownOverlay(prefs.lockdownIncidentInfo.value)
            }
            return
        }

        // 2. Anti-Tamper Settings Guard — ONLY when tampering with Sheildlenz itself
        val childUninstallBlocked = prefs.isChildUninstallBlocked()
        val guardLocalTamper = prefs.antiTamperEnabled.value &&
            prefs.hasGuardianPin.value &&
            !prefs.isGuardianAuthorizedForSystemSettings()
        if ((childUninstallBlocked || guardLocalTamper) &&
            isSettingsOrInstallerPackage(resolvedPkg) &&
            (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)
        ) {
            if (isSheildlenzTamperScreen(resolvedPkg, event)) {
                if (now - lastTamperInterceptTimeMs > 800L) {
                    lastTamperInterceptTimeMs = now
                    Log.w(TAG, "Blocked unauthorized tamper/uninstall access targeting Sheildlenz inside $resolvedPkg (childLocked=$childUninstallBlocked)")
                    performGlobalAction(GLOBAL_ACTION_HOME)
                    val reason = if (childUninstallBlocked) {
                        "Uninstall & Tamper Locked by Parent Guardian. Only your paired Parent can authorize uninstalling or disabling Sheildlenz."
                    } else {
                        "Modifying or uninstalling Sheildlenz (App Info, Device Admin, Accessibility, or Overlay) requires Guardian Password verification."
                    }
                    overlayController.showTamperLockOverlay(reason = reason)
                }
                return
            }
        }

        // 3. Re-entry / Recents Guard for recently blocked offending package
        val blockedPkg = prefs.blockedOffendingPackage.value
        if (!blockedPkg.isNullOrBlank() && resolvedPkg == blockedPkg) {
            if (eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
                now - lastRecentsInterceptTimeMs > 600L
            ) {
                lastRecentsInterceptTimeMs = now
                Log.w(TAG, "Prevented reopening blocked package $blockedPkg from launcher/Recents.")
                performGlobalAction(GLOBAL_ACTION_HOME)
                killBackgroundApp(this, blockedPkg)
                overlayController.showLockdownOverlay(prefs.lockdownIncidentInfo.value)
            }
            return
        }

        // 4. Track active monitored foreground application accurately across apps (including Chrome & returning to Sheildlenz)
        updateTrackedForegroundPackage(eventType, resolvedPkg)

        // 4b. Bedtime Lock & Daily Per-App Time Budget Enforcement (zero special usage permissions)
        if (!prefs.maintenanceMode.value &&
            !isExemptSystemOrLauncherPackage(resolvedPkg) &&
            !prefs.isPackageWhitelisted(resolvedPkg)
        ) {
            if (prefs.isWithinBedtimeLockWindow()) {
                if (now - lastLockdownInterceptTimeMs > 900L) {
                    lastLockdownInterceptTimeMs = now
                    val appLabel = getAppLabel(resolvedPkg)
                    performGlobalAction(GLOBAL_ACTION_HOME)
                    overlayController.showUsageOrBedtimeOverlay(
                        title = "Bedtime Lock Active",
                        subtitle = "$appLabel is paused during Guardian Bedtime Hours (${AegisPreferences.formatClockTime(prefs.bedtimeStartMinutes.value)} – ${AegisPreferences.formatClockTime(prefs.bedtimeEndMinutes.value)}).",
                        packageName = resolvedPkg,
                        allowGuardianExtension = false
                    )
                }
                return
            }

            if (prefs.isAppTimeBudgetExceeded(resolvedPkg)) {
                if (now - lastLockdownInterceptTimeMs > 900L) {
                    lastLockdownInterceptTimeMs = now
                    val appLabel = getAppLabel(resolvedPkg)
                    val limitMins = prefs.appTimeBudgets.value[resolvedPkg] ?: 0
                    performGlobalAction(GLOBAL_ACTION_HOME)
                    if (prefs.evictAppOnThreat.value) {
                        killBackgroundApp(this, resolvedPkg)
                    }
                    overlayController.showUsageOrBedtimeOverlay(
                        title = "Daily Time Limit Reached",
                        subtitle = "$appLabel has reached its $limitMins minute daily time budget.",
                        packageName = resolvedPkg,
                        allowGuardianExtension = true
                    )
                }
                return
            }
        }

        // 5. Real-time Chrome & Third-Party Browser URL / Search Safety Guard
        val isShieldArmed = prefs.isShieldDesiredActive.value ||
            container.protectionRepository.telemetry.value.isProtectionEnabled
        if (isShieldArmed &&
            !prefs.maintenanceMode.value &&
            !prefs.isPackageWhitelisted(resolvedPkg) &&
            isBrowserPackage(resolvedPkg)
        ) {
            inspectBrowserForUnsafeUrl(resolvedPkg, event, eventType, now)
        }
    }

    private fun isExemptSystemOrLauncherPackage(pkgName: String): Boolean {
        if (pkgName == packageName) return true
        if (isAllowedDuringLockdown(pkgName) || isTransientOverlayPackage(pkgName)) return true
        val lower = pkgName.lowercase()
        return lower.contains("launcher") ||
            lower == "com.android.settings" ||
            lower == "com.miui.home"
    }

    private fun updateTrackedForegroundPackage(eventType: Int, pkgName: String) {
        if (isTransientOverlayPackage(pkgName)) return

        val currentTracked = container.protectionRepository.telemetry.value.activeMonitoredPackage
        val shouldVerifyRoot = eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED ||
            (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED && pkgName != currentTracked)

        val verifiedPkg = if (shouldVerifyRoot) {
            val rootPkg = safeGetRootPackageName()
            if (rootPkg != null && !isTransientOverlayPackage(rootPkg)) rootPkg else null
        } else {
            pkgName
        } ?: return

        if (verifiedPkg != currentTracked) {
            val appLabel = if (verifiedPkg == packageName) "Sheildlenz" else getAppLabel(verifiedPkg)
            container.protectionRepository.updateActivePackage(verifiedPkg, appLabel)
        } else if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            container.protectionRepository.notifyScreenContextChanged()
        }
    }

    private fun isTransientOverlayPackage(pkgName: String): Boolean {
        val lower = pkgName.lowercase()
        return lower == "com.android.systemui" ||
            lower.contains("inputmethod") ||
            lower == "android"
    }

    private fun safeGetRootPackageName(): String? {
        val root = try {
            rootInActiveWindow
        } catch (_: Exception) {
            null
        } ?: return null
        return try {
            root.packageName?.toString()?.takeIf { it.isNotBlank() }
        } finally {
            @Suppress("DEPRECATION")
            try {
                root.recycle()
            } catch (_: Exception) {
            }
        }
    }

    private fun inspectBrowserForUnsafeUrl(
        browserPkg: String,
        event: AccessibilityEvent,
        eventType: Int,
        nowUptime: Long
    ) {
        if (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            nowUptime - lastBrowserScanTimeMs < 220L
        ) {
            return
        }
        lastBrowserScanTimeMs = nowUptime

        val builder = StringBuilder()
        for (item in event.text) {
            val s = item?.toString()?.trim()
            if (!s.isNullOrBlank() && s.length <= 320) {
                builder.append(' ').append(s)
            }
        }

        val root = try {
            rootInActiveWindow
        } catch (_: Exception) {
            null
        }
        if (root != null) {
            try {
                val rootPkg = root.packageName?.toString()
                if (rootPkg == null || rootPkg == browserPkg) {
                    collectBrowserAddressAndTitle(root, depth = 0, out = builder)
                }
            } finally {
                @Suppress("DEPRECATION")
                try {
                    root.recycle()
                } catch (_: Exception) {
                }
            }
        }

        val candidate = builder.toString()
        if (candidate.isBlank()) return

        val prefs = container.preferences
        val matchedReason = detectExplicitBrowserUrl(
            rawText = candidate,
            customDomains = prefs.customBlockedDomains.value,
            customKeywords = prefs.customBlockedKeywords.value,
            blockGamblingKeywords = prefs.blockGamblingKeywords.value
        ) ?: return
        if (nowUptime - lastBrowserBlockTimeMs < 1500L) return
        lastBrowserBlockTimeMs = nowUptime

        triggerBrowserLockdown(browserPkg, matchedReason)
    }

    private fun collectBrowserAddressAndTitle(
        node: AccessibilityNodeInfo,
        depth: Int,
        out: StringBuilder
    ) {
        if (depth > 6 || out.length >= MAX_BROWSER_TEXT_CHARS) return

        val viewId = node.viewIdResourceName?.lowercase() ?: ""
        val clsName = node.className?.toString()?.lowercase() ?: ""
        val isAddressOrTitleNode = viewId.contains("url") ||
            viewId.contains("omnibox") ||
            viewId.contains("location") ||
            viewId.contains("address") ||
            viewId.contains("search_box") ||
            viewId.contains("toolbar_title") ||
            clsName.endsWith("edittext") ||
            clsName.endsWith("webview")

        if (isAddressOrTitleNode) {
            val nodeText = node.text?.toString()?.trim()
            if (!nodeText.isNullOrBlank() && nodeText.length <= 320) {
                out.append(' ').append(nodeText)
            }
            if (!clsName.endsWith("webview")) {
                val desc = node.contentDescription?.toString()?.trim()
                if (!desc.isNullOrBlank() && desc.length <= 320) {
                    out.append(' ').append(desc)
                }
            }
        }

        // Do not descend inside rendered webpage DOM children to avoid reading article prose
        if (clsName.endsWith("webview")) return

        val count = node.childCount.coerceAtMost(20)
        for (i in 0 until count) {
            if (out.length >= MAX_BROWSER_TEXT_CHARS) break
            val child = node.getChild(i) ?: continue
            try {
                collectBrowserAddressAndTitle(child, depth + 1, out)
            } finally {
                @Suppress("DEPRECATION")
                try {
                    child.recycle()
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun triggerBrowserLockdown(browserPkg: String, matchedReason: String) {
        val prefs = container.preferences
        val appLabel = getAppLabel(browserPkg)
        val fullDetails = "$matchedReason\nDetected in Browser: $appLabel ($browserPkg)"

        Log.w(TAG, "BROWSER SAFETY GUARD TRIGGERED in $appLabel ($browserPkg): $matchedReason")

        val wentHome = performGlobalAction(GLOBAL_ACTION_HOME)
        if (!wentHome) {
            try {
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                }
                startActivity(homeIntent)
            } catch (_: Exception) {
            }
        }

        if (prefs.evictAppOnThreat.value) {
            killBackgroundApp(this, browserPkg)
        }

        val escalation = prefs.recordThreatAndCheckLockdown()
        if (!escalation.shouldLockdown) {
            overlayController.showWarningOverlay(
                appName = appLabel,
                packageName = browserPkg,
                categoryName = ThreatCategory.EXPLICIT_ADULT.displayName,
                warningNumber = escalation.warningNumber,
                maxWarnings = escalation.maxWarnings
            )
            serviceScope.launch {
                val incident = SecurityIncident(
                    timestampMillis = System.currentTimeMillis(),
                    packageName = browserPkg,
                    appDisplayName = appLabel,
                    category = ThreatCategory.EXPLICIT_ADULT,
                    severity = ThreatSeverity.MEDIUM,
                    confidenceScore = 1.0f,
                    actionTaken = "Warning ${escalation.warningNumber}/${escalation.maxWarnings} • Browser Evicted",
                    diagnosticSummary = fullDetails,
                    lockdownDurationMinutes = 0
                )
                container.securityHistoryRepository.logIncident(incident)
                container.protectionRepository.recordDetection(ThreatCategory.EXPLICIT_ADULT)
            }
            return
        }

        val suspended = AegisDeviceAdminReceiver.suspendPackageIfDeviceOwner(this, browserPkg)

        prefs.engageFullDeviceLockdown(
            appName = appLabel,
            packageName = browserPkg,
            categoryName = ThreatCategory.EXPLICIT_ADULT.displayName,
            details = fullDetails
        )
        overlayController.showLockdownOverlay(prefs.lockdownIncidentInfo.value)
        AegisLockdownService.start(this)

        val totalMinutes = prefs.lockdownDurationMinutes.value
        val durationLabel = AegisPreferences.formatMinutesLabel(totalMinutes)
        val actionSummary = buildString {
            append(if (prefs.evictAppOnThreat.value) "Browser Evicted & Killed" else "Browser Blocked")
            if (suspended) append(" + Package Suspended")
            append(" • Web Guard $durationLabel Lock")
        }

        serviceScope.launch {
            val incident = SecurityIncident(
                timestampMillis = System.currentTimeMillis(),
                packageName = browserPkg,
                appDisplayName = appLabel,
                category = ThreatCategory.EXPLICIT_ADULT,
                severity = ThreatSeverity.CRITICAL,
                confidenceScore = 1.0f,
                actionTaken = actionSummary,
                diagnosticSummary = fullDetails,
                lockdownDurationMinutes = totalMinutes
            )
            container.securityHistoryRepository.logIncident(incident)
            container.protectionRepository.recordDetection(ThreatCategory.EXPLICIT_ADULT)
        }
    }

    private fun isAllowedDuringLockdown(pkgName: String): Boolean {
        return pkgName == packageName ||
            pkgName == "com.android.dialer" ||
            pkgName == "com.google.android.dialer" ||
            pkgName == "com.android.phone" ||
            pkgName == "com.android.incallui" ||
            pkgName == "com.android.emergency"
    }

    private fun isSheildlenzTamperScreen(pkgName: String, event: AccessibilityEvent): Boolean {
        val className = event.className?.toString() ?: ""
        val builder = StringBuilder(event.text.joinToString(" "))

        val root = try {
            rootInActiveWindow
        } catch (_: Exception) {
            null
        }

        if (root != null) {
            try {
                collectWindowText(root, depth = 0, out = builder)
            } finally {
                @Suppress("DEPRECATION")
                try {
                    root.recycle()
                } catch (_: Exception) {
                }
            }
        }

        return shouldTriggerAntiTamper(
            packageName = pkgName,
            className = className,
            screenText = builder.toString()
        )
    }

    private fun collectWindowText(node: AccessibilityNodeInfo, depth: Int, out: StringBuilder) {
        if (depth > 6 || out.length >= MAX_INSPECTED_TEXT_CHARS) return
        val nodeText = node.text?.toString()
        if (!nodeText.isNullOrBlank()) {
            out.append(' ').append(nodeText)
        }
        val desc = node.contentDescription?.toString()
        if (!desc.isNullOrBlank()) {
            out.append(' ').append(desc)
        }
        val count = node.childCount.coerceAtMost(24)
        for (i in 0 until count) {
            if (out.length >= MAX_INSPECTED_TEXT_CHARS) break
            val child = node.getChild(i) ?: continue
            try {
                collectWindowText(child, depth + 1, out)
            } finally {
                @Suppress("DEPRECATION")
                try {
                    child.recycle()
                } catch (_: Exception) {
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "AegisAccessibilityService interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
        container.protectionRepository.updateActivePackage(null, "Standby")
        container.protectionRepository.updateAccessibilityState(false)
        serviceScope.cancel()
        Log.i(TAG, "AegisAccessibilityService destroyed.")
    }

    private fun getAppLabel(pkgName: String): String {
        return try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(pkgName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            pkgName
        }
    }

    companion object {
        private const val TAG = "AegisAccessibility"
        private const val MAX_INSPECTED_TEXT_CHARS = 4096
        private const val MAX_BROWSER_TEXT_CHARS = 1024

        val SUPPORTED_BROWSER_PACKAGES = setOf(
            "com.android.chrome",
            "com.chrome.beta",
            "com.chrome.dev",
            "com.chrome.canary",
            "com.microsoft.emmx",
            "org.mozilla.firefox",
            "org.mozilla.firefox_beta",
            "org.mozilla.focus",
            "com.brave.browser",
            "com.opera.browser",
            "com.opera.mini.native",
            "com.opera.gx",
            "com.sec.android.app.sbrowser",
            "com.duckduckgo.mobile.android",
            "com.vivaldi.browser",
            "com.kiwibrowser.browser",
            "com.yandex.browser",
            "com.ucmobile.intl",
            "com.uc.browser.en",
            "com.android.browser",
            "com.mi.globalbrowser"
        )

        private val EXPLICIT_TLD_REGEX = Regex(
            """(?:^|[^a-z0-9])[a-z0-9-]+(?:\.[a-z0-9-]+)*\.(?:xxx|porn|sex|adult)(?=$|[^a-z0-9])"""
        )

        private val KNOWN_ADULT_HOST_REGEX = Regex(
            """(?:^|[^a-z0-9])(?:[a-z0-9-]+\.)*(?:onlyfans|fansly|brazzers|livejasmin|cam4|myfreecams|bongacams|beeg|eporner|txxx|hqporner|motherless|coomer|kemono|danbooru|gelbooru|hitomi)\.(?:com|net|org|tv|xxx|sex|porn|io|su|party|info|co|me|club|live|cam|cc|gg|to|ws)(?=$|[^a-z0-9])"""
        )

        private val EXPLICIT_TOKEN_REGEX = Regex(
            """(?:^|[^a-z0-9])(?:pornhub|xvideos|xnxx|xhamster|redtube|youporn|tube8|spankbang|chaturbate|stripchat|rule34|nhentai|hanime|erome|youjizz|tnaflix|fapello|porn|porno|pornography|hentai|nsfw|xxx|nude|nudes|blowjob|handjob|milf|creampie|gangbang|threesome|bdsm)(?=$|[^a-z0-9])"""
        )

        private val GAMBLING_TOKEN_REGEX = Regex(
            """(?:^|[^a-z0-9])(?:bet365|draftkings|fanduel|1xbet|stake\.com|roobet|pokerstars|888casino|betway|williamhill|sportsbook|online\s+casino|slot\s+machine)(?=$|[^a-z0-9])"""
        )

        fun isBrowserPackage(packageName: String?): Boolean {
            if (packageName.isNullOrBlank()) return false
            val lower = packageName.lowercase()
            return lower in SUPPORTED_BROWSER_PACKAGES || lower.contains(".browser")
        }

        /**
         * Inspects browser omnibox URLs, search terms, and page titles using boundary-safe rules
         * to prevent substring false positives (e.g., "portrait", "portal", "scunthorpe").
         */
        @JvmOverloads
        fun detectExplicitBrowserUrl(
            rawText: String?,
            customDomains: Set<String> = emptySet(),
            customKeywords: Set<String> = emptySet(),
            blockGamblingKeywords: Boolean = true
        ): String? {
            if (rawText.isNullOrBlank()) return null
            val normalized = rawText.lowercase()
                .replace("%20", " ")
                .replace("+", " ")

            for (customDomain in customDomains) {
                val clean = customDomain.trim().lowercase().removePrefix("www.")
                if (clean.length >= 3) {
                    val escaped = Regex.escape(clean)
                    val domainRegex = Regex("""(?:^|[^a-z0-9-])$escaped(?=$|[^a-z0-9])""")
                    if (domainRegex.containsMatchIn(normalized)) {
                        return "Custom Blocklist Guard blocked domain '$clean'."
                    }
                }
            }

            for (customKeyword in customKeywords) {
                val kw = customKeyword.trim().lowercase()
                if (kw.length >= 2) {
                    val escaped = Regex.escape(kw)
                    val kwRegex = Regex("""(?:^|[^a-z0-9])$escaped(?=$|[^a-z0-9])""")
                    if (kwRegex.containsMatchIn(normalized)) {
                        return "Custom Blocklist Guard blocked keyword '$kw'."
                    }
                }
            }

            if (blockGamblingKeywords && GAMBLING_TOKEN_REGEX.containsMatchIn(normalized)) {
                return "Gambling Filter Guard blocked betting/casino site or search."
            }

            val tldMatch = EXPLICIT_TLD_REGEX.find(normalized)
            if (tldMatch != null) {
                return "Browser Safety Guard blocked explicit adult domain (.xxx/.porn/.sex)."
            }
            val hostMatch = KNOWN_ADULT_HOST_REGEX.find(normalized)
            if (hostMatch != null) {
                return "Browser Safety Guard blocked explicit adult website."
            }
            val tokenMatch = EXPLICIT_TOKEN_REGEX.find(normalized)
            if (tokenMatch != null) {
                return "Browser Safety Guard blocked explicit search/site in address bar."
            }
            return null
        }

        private val SHEILDLENZ_IDENTIFIERS = listOf(
            "sheildlenz",
            "shieldlenz",
            "com.ahsan.tcr.sheildlenz"
        )

        private val TAMPER_ACTION_KEYWORDS = listOf(
            "uninstall",
            "force stop",
            "deactivate",
            "disable",
            "device admin",
            "device administrator",
            "use sheildlenz",
            "sheildlenz shortcut",
            "allow display over other apps",
            "allow draw over other apps",
            "permit drawing over other apps",
            "clear storage",
            "clear data"
        )

        fun isSettingsOrInstallerPackage(pkgName: String): Boolean {
            val lower = pkgName.lowercase()
            return lower == "com.android.settings" ||
                lower.contains("packageinstaller") ||
                lower.contains("permissioncontroller")
        }

        fun shouldTriggerAntiTamper(
            packageName: String,
            className: String,
            screenText: String
        ): Boolean {
            val pkg = packageName.lowercase()
            val cls = className.lowercase()
            val text = screenText.lowercase()

            if (!isSettingsOrInstallerPackage(pkg) && !cls.contains("uninstaller") && !cls.contains("deviceadminadd")) {
                return false
            }

            val targetsSheildlenz = SHEILDLENZ_IDENTIFIERS.any { id -> text.contains(id) }
            if (!targetsSheildlenz) {
                return false
            }

            if (cls.contains("uninstaller") || cls.contains("deviceadminadd")) {
                return true
            }

            if (pkg.contains("packageinstaller")) {
                val isUninstallAction = text.contains("uninstall") ||
                    text.contains("remove this app") ||
                    text.contains("delete this app")
                return isUninstallAction
            }

            return TAMPER_ACTION_KEYWORDS.any { keyword -> text.contains(keyword) }
        }

        @Volatile
        private var instance: AegisAccessibilityService? = null

        fun isServiceRunning(): Boolean = instance != null

        fun performEmergencyHomeAction(): Boolean {
            val service = instance
            if (service != null) {
                val success = service.performGlobalAction(GLOBAL_ACTION_HOME)
                Log.i(TAG, "Executed emergency GLOBAL_ACTION_HOME: $success")
                return success
            }
            Log.w(TAG, "Cannot execute emergency home: AccessibilityService not connected.")
            return false
        }

        fun closeAndEvictPackage(context: Context, packageName: String): Boolean {
            val wentHome = performEmergencyHomeAction()
            killBackgroundApp(context, packageName)
            return wentHome
        }

        fun killBackgroundApp(context: Context, packageName: String) {
            if (packageName.isBlank() || packageName == context.packageName) return
            try {
                val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                am?.killBackgroundProcesses(packageName)
                Log.i(TAG, "Terminated background processes for $packageName")
            } catch (e: Exception) {
                Log.e(TAG, "Failed killing background processes for $packageName", e)
            }
        }
    }
}
