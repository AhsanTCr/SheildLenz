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
import com.example.core.models.SafeSearchPack
import com.example.core.models.SecurityIncident
import com.example.core.models.ShortVideoPlatform
import com.example.core.models.ShortVideoScrollDetector
import com.example.core.models.ThreatCategory
import com.example.core.models.ThreatSeverity
import com.example.data.preferences.AegisPreferences
import com.example.data.repository.BlockedDnsLogRepository
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
    private var lastAppLockerInterceptTimeMs: Long = 0L
    private var lastBrowserScanTimeMs: Long = 0L
    private var lastBrowserBlockTimeMs: Long = 0L
    private var lastUsageTrackedPkg: String? = null
    private var lastUsageTickUptimeMs: Long = 0L
    private var lastOpenedTrackedPkg: String? = null
    private var lastScrollSignature: String = ""
    private var lastScrollCountTimeMs: Long = 0L
    private var lastReelsScanTimeMs: Long = 0L
    private var lastDetoxOrScrollLockTimeMs: Long = 0L
    private val studyLockedPackagesThisSession = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    @Volatile
    private var activeSoftWarningPkg: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        val hasTimeBlocked = runCatching { container.timeBlockerRepository.hasAnyRestrictedApps() }.getOrDefault(false)
        if (!container.preferences.masterPowerEnabled.value && !hasTimeBlocked) {
            Log.i(TAG, "Master Power is OFF — disconnecting AegisAccessibilityService immediately.")
            disableSelf()
            return
        }
        instance = this
        Log.i(TAG, "AegisAccessibilityService connected with Chrome Web Guard, anti-tamper & app eviction.")
        container.protectionRepository.updateAccessibilityState(true)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val eventType = event.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED &&
            eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED
        ) {
            return
        }

        val resolvedPkg = event.packageName?.toString()?.takeIf { it.isNotBlank() }
            ?: safeGetRootPackageName()
            ?: return
        val prefs = container.preferences
        val timeBlockerRepo = container.timeBlockerRepository
        val hasPiqoraxCoinLock = prefs.piqoraxConnectorConnected.value && prefs.piqoraxCoinDeviceLockActive.value
        val hasTimeBlocked = runCatching { timeBlockerRepo.hasAnyRestrictedApps() }.getOrDefault(false)
        if (!prefs.masterPowerEnabled.value && !hasTimeBlocked && !hasPiqoraxCoinLock) {
            disableSelf()
            return
        }

        // Silent isolated Time-Based App Blocker monitor (independent of manual/parental toggles)
        if (hasTimeBlocked) {
            if (resolvedPkg == packageName ||
                isTransientOverlayPackage(resolvedPkg) ||
                isExemptSystemOrLauncherPackage(resolvedPkg)
            ) {
                timeBlockerRepo.onForegroundLeft()
            } else {
                val decision = timeBlockerRepo.onForegroundPackageObserved(resolvedPkg) { exhausted ->
                    timeBlockerRepo.launchLockScreen(this, exhausted)
                }
                if (decision.shouldBlock) {
                    timeBlockerRepo.launchLockScreen(this, decision)
                    return
                }
            }
        }

        // PiqoraX Coin-Unlock Device Lock (keeps PiqoraX, Sheildlenz, dialer & launcher accessible)
        if (hasPiqoraxCoinLock &&
            resolvedPkg != com.example.timeblocker.ui.TimeBlockLockActivity.PIQORAX_PACKAGE_NAME &&
            !isExemptSystemOrLauncherPackage(resolvedPkg) &&
            !isTransientOverlayPackage(resolvedPkg)
        ) {
            val nowMs = SystemClock.uptimeMillis()
            if (nowMs - lastLockdownInterceptTimeMs > 700L) {
                lastLockdownInterceptTimeMs = nowMs
                com.example.timeblocker.ui.TimeBlockLockActivity.launchPiqoraxCoinDeviceLock(this, resolvedPkg)
            }
            return
        }

        if (!prefs.masterPowerEnabled.value) {
            return
        }
        com.example.services.protection.ProfileAutoScheduler.evaluateAndApply(prefs)
        val now = SystemClock.uptimeMillis()

        // Fast-path for vertical scroll swipes inside Short-Video apps (works whether Shield is ON or OFF)
        if (eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            if (!prefs.maintenanceMode.value &&
                !prefs.isLockdownActive() &&
                !overlayController.isOverlayVisible &&
                resolvedPkg in ShortVideoPlatform.ALL_PACKAGES
            ) {
                inspectShortVideoScroll(resolvedPkg, event, eventType, now)
            }
            return
        }

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

        // 4. Track active monitored foreground application accurately across apps (works whether Shield is ON or OFF)
        updateTrackedForegroundPackage(eventType, resolvedPkg)
        if (!isTransientOverlayPackage(resolvedPkg) && resolvedPkg != packageName) {
            prefs.onForegroundPackageChanged(resolvedPkg)
        }
        if (!isTransientOverlayPackage(resolvedPkg) && !isExemptSystemOrLauncherPackage(resolvedPkg)) {
            if (resolvedPkg != lastOpenedTrackedPkg) {
                lastOpenedTrackedPkg = resolvedPkg
                lastScrollSignature = ""
                prefs.recordAppOpen(resolvedPkg)
            }
        } else if (resolvedPkg == packageName || isExemptSystemOrLauncherPackage(resolvedPkg)) {
            if (resolvedPkg != packageName) {
                lastOpenedTrackedPkg = null
            }
        }

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

        // 4c. Study Mode & Focus Lock Timer with Soft Warning Countdown
        val studyActive = prefs.isStudySessionActive()
        if (!studyActive) {
            if (studyLockedPackagesThisSession.isNotEmpty()) {
                studyLockedPackagesThisSession.clear()
            }
            if (activeSoftWarningPkg != null) {
                activeSoftWarningPkg = null
            }
        } else if (!prefs.maintenanceMode.value && !isTransientOverlayPackage(resolvedPkg)) {
            val isAllowedForStudy = isExemptSystemOrLauncherPackage(resolvedPkg) ||
                prefs.isPackageAllowedDuringStudy(resolvedPkg)
            if (isAllowedForStudy) {
                if (activeSoftWarningPkg != null) {
                    activeSoftWarningPkg = null
                    overlayController.dismissBlockOverlay()
                }
            } else {
                val appLabel = getAppLabel(resolvedPkg)
                if (resolvedPkg in studyLockedPackagesThisSession) {
                    if (now - lastLockdownInterceptTimeMs > 800L) {
                        lastLockdownInterceptTimeMs = now
                        performGlobalAction(GLOBAL_ACTION_HOME)
                        killBackgroundApp(this, resolvedPkg)
                        overlayController.showStudyLockOverlay(appLabel, resolvedPkg)
                    }
                    return
                } else if (activeSoftWarningPkg != resolvedPkg) {
                    activeSoftWarningPkg = resolvedPkg
                    val softWarnSec = prefs.studySoftWarningSeconds.value
                    overlayController.showStudySoftWarningOverlay(
                        appName = appLabel,
                        packageName = resolvedPkg,
                        softWarningSeconds = softWarnSec
                    ) {
                        studyLockedPackagesThisSession.add(resolvedPkg)
                        if (activeSoftWarningPkg == resolvedPkg) {
                            activeSoftWarningPkg = null
                        }
                        if (prefs.isStudySessionActive()) {
                            val currentForeground = safeGetRootPackageName() ?: resolvedPkg
                            val stillDistracting = !isExemptSystemOrLauncherPackage(currentForeground) &&
                                !prefs.isPackageAllowedDuringStudy(currentForeground)
                            if (stillDistracting) {
                                performGlobalAction(GLOBAL_ACTION_HOME)
                                killBackgroundApp(this@AegisAccessibilityService, resolvedPkg)
                                overlayController.showStudyLockOverlay(appLabel, resolvedPkg)
                            }
                        }
                    }
                }
            }
        }

        // 4d. Individual App Locker (Guardian PIN Gated)
        if (!prefs.maintenanceMode.value &&
            !isExemptSystemOrLauncherPackage(resolvedPkg) &&
            prefs.isPackageLockedByAppLocker(resolvedPkg)
        ) {
            if (!overlayController.isShowingAppLockerFor(resolvedPkg) &&
                (eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || now - lastAppLockerInterceptTimeMs > 700L)
            ) {
                lastAppLockerInterceptTimeMs = now
                val appLabel = getAppLabel(resolvedPkg)
                performGlobalAction(GLOBAL_ACTION_HOME)
                overlayController.showAppLockerPinOverlay(appLabel, resolvedPkg)
            }
            return
        }

        // 4e. Instant Detox & Reels/Shorts Scroll-Limit Cooldown Lock (Shield-independent)
        if (!prefs.maintenanceMode.value && !isExemptSystemOrLauncherPackage(resolvedPkg)) {
            if (prefs.isPackageBlockedByInstantDetox(resolvedPkg)) {
                if (now - lastDetoxOrScrollLockTimeMs > 800L) {
                    lastDetoxOrScrollLockTimeMs = now
                    val appLabel = getAppLabel(resolvedPkg)
                    performGlobalAction(GLOBAL_ACTION_HOME)
                    killBackgroundApp(this, resolvedPkg)
                    overlayController.showScrollLimitLockOverlay(
                        appName = appLabel,
                        packageName = resolvedPkg,
                        platformName = appLabel,
                        watchedCount = prefs.dailyReelsTotalCount.value,
                        limitCount = prefs.scrollLimitCount.value,
                        isDetoxMode = true
                    )
                }
                return
            }

            if (prefs.getRemainingScrollLockMillis(resolvedPkg) > 0L) {
                if (now - lastDetoxOrScrollLockTimeMs > 800L) {
                    lastDetoxOrScrollLockTimeMs = now
                    val appLabel = getAppLabel(resolvedPkg)
                    val plat = ShortVideoPlatform.findByPackage(resolvedPkg).firstOrNull()
                    val watched = plat?.let { prefs.dailyReelsByPlatform.value[it.id] } ?: prefs.dailyReelsTotalCount.value
                    performGlobalAction(GLOBAL_ACTION_HOME)
                    killBackgroundApp(this, resolvedPkg)
                    overlayController.showScrollLimitLockOverlay(
                        appName = appLabel,
                        packageName = resolvedPkg,
                        platformName = plat?.shortLabel ?: appLabel,
                        watchedCount = watched,
                        limitCount = prefs.scrollLimitCount.value,
                        isDetoxMode = false
                    )
                }
                return
            }
        }

        // 4f. 5-Second Mindful Breathing Pause (Shield-independent)
        if (!prefs.maintenanceMode.value &&
            !isExemptSystemOrLauncherPackage(resolvedPkg) &&
            (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED) &&
            prefs.shouldTriggerMindfulPauseForPackage(resolvedPkg) &&
            !overlayController.isOverlayVisible
        ) {
            val appLabel = getAppLabel(resolvedPkg)
            val opensToday = prefs.dailyAppOpensCount.value[resolvedPkg] ?: 1
            val usedMinutesToday = prefs.getAppUsedSecondsToday(resolvedPkg) / 60
            prefs.recordMindfulPauseShown(resolvedPkg, appLabel)
            overlayController.showMindfulBreathingOverlay(
                appName = appLabel,
                packageName = resolvedPkg,
                opensToday = opensToday,
                usedMinutesToday = usedMinutesToday,
                graceMinutes = prefs.mindfulGraceMinutes.value,
                onExitHome = {
                    prefs.recordMindfulExit(resolvedPkg, appLabel)
                    performGlobalAction(GLOBAL_ACTION_HOME)
                },
                onContinue = {
                    prefs.grantMindfulGracePass(resolvedPkg)
                }
            )
            return
        }

        // 4g. Reels/Shorts Counter, Binge Nudge & Auto-Stop Scroll Guard (Shield-independent)
        if (!prefs.maintenanceMode.value &&
            !overlayController.isOverlayVisible &&
            resolvedPkg in ShortVideoPlatform.ALL_PACKAGES
        ) {
            inspectShortVideoScroll(resolvedPkg, event, eventType, now)
        }

        // 5. Real-time Chrome & Third-Party Browser URL / Search Safety Guard
        val isShieldArmed = prefs.isShieldDesiredActive.value ||
            container.protectionRepository.telemetry.value.isProtectionEnabled
        val smartSafeActive = prefs.smartSafeSearchEnabled.value
        if ((isShieldArmed || smartSafeActive) &&
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

        val nowUptime = SystemClock.uptimeMillis()
        val prevPkg = lastUsageTrackedPkg
        val prevTick = lastUsageTickUptimeMs
        val shieldIsTrackingPrevPkg = container.protectionRepository.telemetry.value.isProtectionEnabled &&
            !prevPkg.isNullOrBlank() &&
            !container.preferences.isPackageWhitelisted(prevPkg)
        if (prevTick > 0L && !prevPkg.isNullOrBlank() &&
            !shieldIsTrackingPrevPkg &&
            !container.preferences.maintenanceMode.value &&
            !container.preferences.isLockdownActive() &&
            !isExemptSystemOrLauncherPackage(prevPkg)
        ) {
            val deltaSec = ((nowUptime - prevTick) / 1000L).toInt()
            if (deltaSec in 1..300) {
                container.preferences.recordAppUsageSeconds(prevPkg, deltaSec)
                lastUsageTickUptimeMs = nowUptime
            }
        } else {
            lastUsageTickUptimeMs = nowUptime
        }
        lastUsageTrackedPkg = verifiedPkg

        if (verifiedPkg != currentTracked) {
            lastUsageTickUptimeMs = nowUptime
            val appLabel = if (verifiedPkg == packageName) "Sheildlenz" else getAppLabel(verifiedPkg)
            container.protectionRepository.updateActivePackage(verifiedPkg, appLabel)
        } else if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            container.protectionRepository.notifyScreenContextChanged()
        }
    }

    private fun inspectShortVideoScroll(
        pkgName: String,
        event: AccessibilityEvent,
        eventType: Int,
        nowUptime: Long
    ) {
        if (eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED &&
            nowUptime - lastReelsScanTimeMs < 450L
        ) {
            return
        }
        lastReelsScanTimeMs = nowUptime

        val viewIds = mutableListOf<String>()
        val labels = mutableListOf<String>()
        val classNames = mutableListOf<String>()

        for (item in event.text) {
            val s = item?.toString()?.trim()
            if (!s.isNullOrBlank() && s.length <= 160) {
                labels.add(s)
            }
        }
        event.className?.toString()?.let { classNames.add(it) }

        val sourceNode = try {
            event.source
        } catch (_: Exception) {
            null
        }
        if (sourceNode != null) {
            try {
                collectShortVideoSignals(sourceNode, depth = 0, viewIds = viewIds, labels = labels, classNames = classNames)
            } finally {
                @Suppress("DEPRECATION")
                try {
                    sourceNode.recycle()
                } catch (_: Exception) {
                }
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
                if (rootPkg == null || rootPkg == pkgName) {
                    collectShortVideoSignals(root, depth = 0, viewIds = viewIds, labels = labels, classNames = classNames)
                }
            } finally {
                @Suppress("DEPRECATION")
                try {
                    root.recycle()
                } catch (_: Exception) {
                }
            }
        }

        val platform = ShortVideoScrollDetector.detectActivePlatform(
            packageName = pkgName,
            viewIds = viewIds,
            labels = labels,
            classNames = classNames,
            enabledPlatformIds = ShortVideoPlatform.ALL_IDS
        ) ?: return

        val hasDynamicText = ShortVideoScrollDetector.hasDynamicTextLabel(labels)
        val contentSig = ShortVideoScrollDetector.buildContentSignature(labels, viewIds)
        val fullSignature = when {
            hasDynamicText && contentSig.isNotBlank() -> "${platform.id}|$contentSig"
            eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                val scrollY = event.scrollY
                val fromIdx = event.fromIndex
                val deltaY = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) event.scrollDeltaY else 0
                "${platform.id}|viewscroll|$fromIdx|$scrollY|$deltaY|$nowUptime"
            }
            contentSig.isNotBlank() -> "${platform.id}|$contentSig"
            else -> return
        }
        if (fullSignature == lastScrollSignature) return
        if (nowUptime - lastScrollCountTimeMs < 1500L) return

        lastScrollSignature = fullSignature
        lastScrollCountTimeMs = nowUptime

        val prefs = container.preferences
        val appLabel = getAppLabel(pkgName)
        val result = prefs.recordShortVideoScrolled(
            platform = platform,
            packageName = pkgName,
            appLabel = appLabel
        )

        if (result.shouldTriggerScrollLock) {
            Log.w(TAG, "Scroll limit reached in $appLabel (${result.sessionReelsSinceUnlock}/${result.scrollLimit}) — auto-stopping app.")
            performGlobalAction(GLOBAL_ACTION_HOME)
            killBackgroundApp(this, pkgName)
            overlayController.showScrollLimitLockOverlay(
                appName = appLabel,
                packageName = pkgName,
                platformName = platform.shortLabel,
                watchedCount = result.sessionReelsSinceUnlock,
                limitCount = result.scrollLimit,
                isDetoxMode = false
            )
        } else if (result.shouldTriggerBingeNudge) {
            overlayController.showScrollBingeNudgeOverlay(
                platformName = platform.shortLabel,
                watchedCount = result.sessionReelsSinceUnlock,
                limitCount = result.scrollLimit,
                onStepAway = {
                    prefs.recordMindfulExit(
                        packageName = pkgName,
                        appLabel = appLabel,
                        detail = "Stepped away after ${result.sessionReelsSinceUnlock} ${platform.shortLabel} nudge"
                    )
                    performGlobalAction(GLOBAL_ACTION_HOME)
                },
                onKeepWatching = {}
            )
        }
    }

    private fun collectShortVideoSignals(
        node: AccessibilityNodeInfo,
        depth: Int,
        viewIds: MutableList<String>,
        labels: MutableList<String>,
        classNames: MutableList<String>
    ) {
        if (depth > 7 || (viewIds.size >= 60 && labels.size >= 40)) return
        node.viewIdResourceName?.takeIf { it.isNotBlank() }?.let {
            if (viewIds.size < 60) viewIds.add(it)
        }
        node.className?.toString()?.takeIf { it.isNotBlank() }?.let {
            if (classNames.size < 40) classNames.add(it)
        }
        node.text?.toString()?.trim()?.takeIf { it.isNotBlank() && it.length <= 160 }?.let {
            if (labels.size < 40) labels.add(it)
        }
        node.contentDescription?.toString()?.trim()?.takeIf { it.isNotBlank() && it.length <= 160 }?.let {
            if (labels.size < 40) labels.add(it)
        }

        val count = node.childCount.coerceAtMost(18)
        for (i in 0 until count) {
            if (viewIds.size >= 60 && labels.size >= 40) break
            val child = node.getChild(i) ?: continue
            try {
                collectShortVideoSignals(child, depth + 1, viewIds, labels, classNames)
            } finally {
                @Suppress("DEPRECATION")
                try {
                    child.recycle()
                } catch (_: Exception) {
                }
            }
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
            blockGamblingKeywords = prefs.blockGamblingKeywords.value,
            smartSafeSearchEnabled = prefs.smartSafeSearchEnabled.value,
            safeSearchPacks = prefs.safeSearchPacks.value
        ) ?: return
        if (nowUptime - lastBrowserBlockTimeMs < 1500L) return
        lastBrowserBlockTimeMs = nowUptime

        if (matchedReason.startsWith("Smart Safe Search")) {
            prefs.incrementSafeSearchBlockedCount()
        }
        runCatching {
            val category = when {
                matchedReason.startsWith("Smart Safe Search") || matchedReason.contains("search", ignoreCase = true) ->
                    BlockedDnsLogRepository.CATEGORY_SEARCH
                matchedReason.contains("Gambling", ignoreCase = true) ->
                    BlockedDnsLogRepository.CATEGORY_GAMBLING
                matchedReason.contains("keyword", ignoreCase = true) ->
                    BlockedDnsLogRepository.CATEGORY_CUSTOM_KEYWORD
                matchedReason.contains("domain", ignoreCase = true) ->
                    BlockedDnsLogRepository.CATEGORY_CUSTOM_DOMAIN
                else -> BlockedDnsLogRepository.CATEGORY_ADULT
            }
            val displayEntry = extractCleanBrowserLogEntry(candidate, browserPkg)
            AegisContainer.getInstance(this).blockedDnsLogRepository.recordBlockedQuery(
                domain = displayEntry,
                reasonCategory = category,
                matchedRule = matchedReason
            )
        }
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
                maxWarnings = escalation.maxWarnings,
                warningOnly = escalation.isWarningOnly
            )
            serviceScope.launch {
                val actionSummary = if (escalation.isWarningOnly) {
                    "Warning Only #${escalation.warningNumber} • Browser Evicted (No Lockout)"
                } else {
                    "Warning ${escalation.warningNumber}/${escalation.maxWarnings} • Browser Evicted"
                }
                val incident = SecurityIncident(
                    timestampMillis = System.currentTimeMillis(),
                    packageName = browserPkg,
                    appDisplayName = appLabel,
                    category = ThreatCategory.EXPLICIT_ADULT,
                    severity = ThreatSeverity.MEDIUM,
                    confidenceScore = 1.0f,
                    actionTaken = actionSummary,
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
        runCatching { container.timeBlockerRepository.onForegroundLeft() }
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

        private val BROWSER_HOST_URL_REGEX = Regex(
            """(?:https?://)?(?:www\.)?([a-z0-9-]+(?:\.[a-z0-9-]+)+)(?:[/?#][^\s]*)?""",
            RegexOption.IGNORE_CASE
        )

        /**
         * Extracts a crisp domain/path or search phrase from concatenated accessibility text
         * for the Live Blocked DNS & Search Activity feed.
         */
        fun extractCleanBrowserLogEntry(rawCandidate: String?, fallbackPackage: String): String {
            val cleaned = rawCandidate
                ?.replace(Regex("""\s+"""), " ")
                ?.trim()
                .orEmpty()
            if (cleaned.isBlank()) return fallbackPackage

            val urlMatch = BROWSER_HOST_URL_REGEX.find(cleaned)
            if (urlMatch != null) {
                val host = urlMatch.groupValues.getOrNull(1)?.lowercase().orEmpty()
                if (host.contains('.') && !host.startsWith("com.android.")) {
                    return host.take(64)
                }
            }
            return cleaned.take(64)
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
            blockGamblingKeywords: Boolean = true,
            smartSafeSearchEnabled: Boolean = false,
            safeSearchPacks: Set<String> = emptySet()
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

            if (smartSafeSearchEnabled && safeSearchPacks.isNotEmpty()) {
                val packHit = SafeSearchPack.detectMatchInText(normalized, safeSearchPacks)
                if (packHit != null) {
                    return packHit.second
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

        fun disableServiceIfRunning() {
            val service = instance ?: return
            instance = null
            try {
                service.disableSelf()
                runCatching {
                    AegisContainer.getInstance(service.applicationContext)
                        .protectionRepository
                        .updateAccessibilityState(false)
                }
                Log.i(TAG, "Disabled AegisAccessibilityService via Master Power OFF.")
            } catch (e: Exception) {
                Log.w(TAG, "Failed disabling AegisAccessibilityService: ${e.message}")
            }
        }

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
