package com.example.blocking.overlay

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import com.example.MainActivity
import com.example.R
import com.example.core.models.ThreatCategory
import com.example.core.permissions.PermissionHelper
import com.example.data.preferences.AegisPreferences
import com.example.data.preferences.LockdownIncidentInfo
import com.example.data.preferences.RecoveryQrMode
import com.example.di.AegisContainer
import com.example.services.protection.AegisLockdownService
import com.example.ui.components.QrCodeGenerator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SystemOverlayController(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val displayTypeface: Typeface by lazy {
        try {
            ResourcesCompat.getFont(context, R.font.space_grotesk) ?: Typeface.SANS_SERIF
        } catch (_: Exception) {
            Typeface.SANS_SERIF
        }
    }
    private var overlayView: View? = null

    private var countdownTitleView: TextView? = null
    private var countdownClockView: TextView? = null
    private var unlockHintView: TextView? = null
    private var pinInputView: EditText? = null
    private var unlockButtonView: Button? = null
    private var doubleTimeButtonView: Button? = null
    private var shieldNoteView: TextView? = null

    private var activeOverlayMode: OverlayMode = OverlayMode.NONE
    private var softWarningClockView: TextView? = null
    private var softWarningRemainingSec: Int = 0
    private var softWarningAppLabel: String = ""
    private var softWarningPackageName: String = ""
    private var softWarningOnExpired: (() -> Unit)? = null
    private var appLockerPackageName: String = ""

    private val tickerRunnable = object : Runnable {
        override fun run() {
            if (!isOverlayVisible || activeOverlayMode != OverlayMode.LOCKDOWN) return
            updateLockdownTimerUi()
            mainHandler.postDelayed(this, 1000L)
        }
    }

    private val softWarningTickerRunnable = object : Runnable {
        override fun run() {
            if (!isOverlayVisible || activeOverlayMode != OverlayMode.STUDY_SOFT_WARNING) return
            softWarningRemainingSec -= 1
            if (softWarningRemainingSec <= 0) {
                val expiredCallback = softWarningOnExpired
                softWarningOnExpired = null
                dismissBlockOverlay()
                expiredCallback?.invoke()
                return
            }
            softWarningClockView?.text = "${softWarningRemainingSec}s"
            mainHandler.postDelayed(this, 1000L)
        }
    }

    @Volatile
    var isOverlayVisible: Boolean = false
        private set

    fun showLockdownOverlay(incidentInfo: LockdownIncidentInfo) {
        if (!AegisContainer.getInstance(context).preferences.masterPowerEnabled.value) {
            dismissBlockOverlay()
            return
        }
        if (!PermissionHelper.hasOverlayPermission(context)) {
            Log.w(TAG, "Cannot show lockdown overlay: SYSTEM_ALERT_WINDOW permission missing.")
            return
        }

        mainHandler.post {
            if (!AegisContainer.getInstance(context).preferences.masterPowerEnabled.value) {
                removeCurrentViewImmediate()
                activeOverlayMode = OverlayMode.NONE
                return@post
            }
            if (isOverlayVisible && overlayView != null && activeOverlayMode == OverlayMode.LOCKDOWN) {
                updateLockdownTimerUi()
                return@post
            }
            removeCurrentViewImmediate()
            activeOverlayMode = OverlayMode.LOCKDOWN

            try {
                val view = createLockdownLayout(incidentInfo)
                windowManager?.addView(view, buildFullscreenLayoutParams())
                overlayView = view
                isOverlayVisible = true
                updateLockdownTimerUi()
                mainHandler.removeCallbacks(tickerRunnable)
                mainHandler.postDelayed(tickerRunnable, 1000L)
                Log.i(TAG, "Aegis 10-Hour Full-Device Lockdown Overlay presented.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to present Lockdown Overlay", e)
            }
        }
    }

    fun showTamperLockOverlay(reason: String) {
        if (!AegisContainer.getInstance(context).preferences.masterPowerEnabled.value) {
            dismissBlockOverlay()
            return
        }
        if (!PermissionHelper.hasOverlayPermission(context)) {
            Log.w(TAG, "Cannot show tamper overlay: SYSTEM_ALERT_WINDOW permission missing.")
            return
        }

        mainHandler.post {
            val prefs = AegisContainer.getInstance(context).preferences
            if (!prefs.masterPowerEnabled.value) {
                removeCurrentViewImmediate()
                activeOverlayMode = OverlayMode.NONE
                return@post
            }
            if (prefs.isLockdownActive()) {
                showLockdownOverlay(prefs.lockdownIncidentInfo.value)
                return@post
            }

            if (isOverlayVisible && overlayView != null && activeOverlayMode == OverlayMode.TAMPER) {
                return@post
            }
            removeCurrentViewImmediate()
            activeOverlayMode = OverlayMode.TAMPER

            try {
                val view = createTamperLayout(reason)
                windowManager?.addView(view, buildFullscreenLayoutParams())
                overlayView = view
                isOverlayVisible = true
                Log.i(TAG, "Aegis Anti-Tamper Guardian Overlay presented.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to present Anti-Tamper Overlay", e)
            }
        }
    }

    fun showWarningOverlay(
        appName: String,
        packageName: String,
        categoryName: String,
        warningNumber: Int,
        maxWarnings: Int,
        warningOnly: Boolean = false
    ) {
        if (!AegisContainer.getInstance(context).preferences.masterPowerEnabled.value) {
            dismissBlockOverlay()
            return
        }
        if (!PermissionHelper.hasOverlayPermission(context)) {
            Log.w(TAG, "Cannot show warning overlay: SYSTEM_ALERT_WINDOW permission missing.")
            return
        }

        mainHandler.post {
            val prefs = AegisContainer.getInstance(context).preferences
            if (!prefs.masterPowerEnabled.value) {
                removeCurrentViewImmediate()
                activeOverlayMode = OverlayMode.NONE
                return@post
            }
            if (!warningOnly && prefs.isLockdownActive()) {
                showLockdownOverlay(prefs.lockdownIncidentInfo.value)
                return@post
            }
            removeCurrentViewImmediate()
            activeOverlayMode = OverlayMode.WARNING
            try {
                val view = createWarningLayout(appName, packageName, categoryName, warningNumber, maxWarnings, warningOnly)
                windowManager?.addView(view, buildFullscreenLayoutParams())
                overlayView = view
                isOverlayVisible = true
                Log.i(TAG, "Sheildlenz Warning Overlay #$warningNumber (warningOnly=$warningOnly) presented.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to present Warning Overlay", e)
            }
        }
    }

    fun showUsageOrBedtimeOverlay(
        title: String,
        subtitle: String,
        packageName: String,
        allowGuardianExtension: Boolean
    ) {
        if (!AegisContainer.getInstance(context).preferences.masterPowerEnabled.value) {
            dismissBlockOverlay()
            return
        }
        if (!PermissionHelper.hasOverlayPermission(context)) {
            Log.w(TAG, "Cannot show usage/bedtime overlay: SYSTEM_ALERT_WINDOW permission missing.")
            return
        }

        mainHandler.post {
            val prefs = AegisContainer.getInstance(context).preferences
            if (!prefs.masterPowerEnabled.value) {
                removeCurrentViewImmediate()
                activeOverlayMode = OverlayMode.NONE
                return@post
            }
            if (prefs.isLockdownActive()) {
                showLockdownOverlay(prefs.lockdownIncidentInfo.value)
                return@post
            }
            if (isOverlayVisible && overlayView != null && activeOverlayMode == OverlayMode.USAGE_OR_BEDTIME) {
                return@post
            }
            removeCurrentViewImmediate()
            activeOverlayMode = OverlayMode.USAGE_OR_BEDTIME
            try {
                val view = createUsageOrBedtimeLayout(title, subtitle, packageName, allowGuardianExtension)
                windowManager?.addView(view, buildFullscreenLayoutParams())
                overlayView = view
                isOverlayVisible = true
                Log.i(TAG, "Usage/Bedtime Overlay presented ($title).")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to present Usage/Bedtime Overlay", e)
            }
        }
    }

    fun showStudySoftWarningOverlay(
        appName: String,
        packageName: String,
        softWarningSeconds: Int,
        onCountdownExpired: () -> Unit
    ) {
        if (!AegisContainer.getInstance(context).preferences.masterPowerEnabled.value) {
            dismissBlockOverlay()
            return
        }
        if (!PermissionHelper.hasOverlayPermission(context)) {
            Log.w(TAG, "Cannot show study soft warning overlay: SYSTEM_ALERT_WINDOW permission missing.")
            return
        }

        mainHandler.post {
            val prefs = AegisContainer.getInstance(context).preferences
            if (!prefs.masterPowerEnabled.value) {
                removeCurrentViewImmediate()
                activeOverlayMode = OverlayMode.NONE
                return@post
            }
            if (prefs.isLockdownActive()) {
                showLockdownOverlay(prefs.lockdownIncidentInfo.value)
                return@post
            }
            if (isOverlayVisible &&
                activeOverlayMode == OverlayMode.STUDY_SOFT_WARNING &&
                softWarningPackageName == packageName
            ) {
                return@post
            }
            removeCurrentViewImmediate()
            activeOverlayMode = OverlayMode.STUDY_SOFT_WARNING
            softWarningRemainingSec = softWarningSeconds.coerceIn(5, 120)
            softWarningAppLabel = appName
            softWarningPackageName = packageName
            softWarningOnExpired = onCountdownExpired
            try {
                val view = createStudySoftWarningLayout(appName, softWarningRemainingSec)
                windowManager?.addView(view, buildFullscreenLayoutParams())
                overlayView = view
                isOverlayVisible = true
                mainHandler.removeCallbacks(softWarningTickerRunnable)
                mainHandler.postDelayed(softWarningTickerRunnable, 1000L)
                Log.i(TAG, "Study soft warning overlay presented for $packageName (${softWarningRemainingSec}s).")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to present Study Soft Warning Overlay", e)
            }
        }
    }

    fun showStudyLockOverlay(
        appName: String,
        packageName: String
    ) {
        if (!AegisContainer.getInstance(context).preferences.masterPowerEnabled.value) {
            dismissBlockOverlay()
            return
        }
        if (!PermissionHelper.hasOverlayPermission(context)) {
            Log.w(TAG, "Cannot show study lock overlay: SYSTEM_ALERT_WINDOW permission missing.")
            return
        }

        mainHandler.post {
            val prefs = AegisContainer.getInstance(context).preferences
            if (!prefs.masterPowerEnabled.value) {
                removeCurrentViewImmediate()
                activeOverlayMode = OverlayMode.NONE
                return@post
            }
            if (prefs.isLockdownActive()) {
                showLockdownOverlay(prefs.lockdownIncidentInfo.value)
                return@post
            }
            if (isOverlayVisible && activeOverlayMode == OverlayMode.STUDY_LOCK) {
                return@post
            }
            removeCurrentViewImmediate()
            activeOverlayMode = OverlayMode.STUDY_LOCK
            try {
                val view = createStudyLockLayout(appName, packageName)
                windowManager?.addView(view, buildFullscreenLayoutParams())
                overlayView = view
                isOverlayVisible = true
                Log.i(TAG, "Study lock overlay presented for $packageName.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to present Study Lock Overlay", e)
            }
        }
    }

    fun isShowingAppLockerFor(packageName: String): Boolean =
        isOverlayVisible && activeOverlayMode == OverlayMode.APP_LOCKER && appLockerPackageName == packageName

    fun showAppLockerPinOverlay(
        appName: String,
        packageName: String
    ) {
        if (!AegisContainer.getInstance(context).preferences.masterPowerEnabled.value) {
            dismissBlockOverlay()
            return
        }
        if (!PermissionHelper.hasOverlayPermission(context)) {
            Log.w(TAG, "Cannot show App Locker overlay: SYSTEM_ALERT_WINDOW permission missing.")
            return
        }

        mainHandler.post {
            val prefs = AegisContainer.getInstance(context).preferences
            if (!prefs.masterPowerEnabled.value) {
                removeCurrentViewImmediate()
                activeOverlayMode = OverlayMode.NONE
                return@post
            }
            if (prefs.isLockdownActive()) {
                showLockdownOverlay(prefs.lockdownIncidentInfo.value)
                return@post
            }
            if (isOverlayVisible && activeOverlayMode == OverlayMode.APP_LOCKER && appLockerPackageName == packageName) {
                return@post
            }
            removeCurrentViewImmediate()
            activeOverlayMode = OverlayMode.APP_LOCKER
            appLockerPackageName = packageName
            try {
                val view = createAppLockerLayout(appName, packageName)
                windowManager?.addView(view, buildFullscreenLayoutParams())
                overlayView = view
                isOverlayVisible = true
                Log.i(TAG, "App Locker overlay presented for $packageName.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to present App Locker overlay", e)
            }
        }
    }

    fun showBlockOverlay(
        category: ThreatCategory,
        appName: String,
        packageName: String,
        reason: String
    ) {
        val prefs = AegisContainer.getInstance(context).preferences
        if (!prefs.masterPowerEnabled.value) {
            dismissBlockOverlay()
            return
        }
        if (prefs.isLockdownActive()) {
            showLockdownOverlay(prefs.lockdownIncidentInfo.value)
        } else {
            showLockdownOverlay(
                LockdownIncidentInfo(
                    appName = appName,
                    packageName = packageName,
                    categoryName = category.displayName,
                    details = reason,
                    detectedAtMillis = System.currentTimeMillis()
                )
            )
        }
    }

    fun dismissBlockOverlay() {
        mainHandler.post {
            mainHandler.removeCallbacks(tickerRunnable)
            mainHandler.removeCallbacks(softWarningTickerRunnable)
            removeCurrentViewImmediate()
            activeOverlayMode = OverlayMode.NONE
        }
    }

    private fun removeCurrentViewImmediate() {
        mainHandler.removeCallbacks(softWarningTickerRunnable)
        if (overlayView != null) {
            try {
                windowManager?.removeView(overlayView)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing overlay view", e)
            } finally {
                overlayView = null
                isOverlayVisible = false
                countdownTitleView = null
                countdownClockView = null
                unlockHintView = null
                pinInputView = null
                unlockButtonView = null
                doubleTimeButtonView = null
                shieldNoteView = null
                softWarningClockView = null
                softWarningPackageName = ""
                softWarningAppLabel = ""
                softWarningOnExpired = null
                appLockerPackageName = ""
            }
        }
    }

    private fun buildFullscreenLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams().apply {
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            }
            format = PixelFormat.TRANSLUCENT
            flags = (
                WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                )
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            gravity = Gravity.CENTER
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    private fun createLockdownLayout(incident: LockdownIncidentInfo): View {
        val prefs = AegisContainer.getInstance(context).preferences
        val discreet = prefs.discreetBlockingScreenMode.value
        val customHeadline = prefs.blockCustomHeadline.value.trim()
        val customMessage = prefs.blockCustomMessage.value.trim()
        val showTimer = prefs.blockShowTimer.value
        val showAppBadge = prefs.blockShowAppBadge.value
        val showQrUnlock = prefs.blockShowQrUnlock.value
        val showQuickActions = prefs.blockShowQuickActions.value
        val showEmergencyDialer = prefs.showEmergencyDialerOnLockdown.value
        val showReturnHome = prefs.blockShowReturnHome.value ||
            (!showQuickActions && !showQrUnlock && !showEmergencyDialer)

        val scroll = ScrollView(context).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                if (discreet) {
                    intArrayOf(Color.parseColor("#F3F5F8"), Color.parseColor("#EAEFF5"))
                } else {
                    intArrayOf(Color.parseColor("#D2E5FF"), Color.parseColor("#E6E4FF"), Color.parseColor("#FFF0CC"))
                }
            )
            isFillViewport = true
            isClickable = true
            isFocusable = true
        }

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(48, 84, 48, 64)
        }

        if (showAppBadge) {
            val badge = TextView(context).apply {
                text = if (discreet) "SYSTEM DISPLAY POLICY" else "SHEILDLENZ • FULL DEVICE LOCKDOWN"
                setTextColor(Color.parseColor(if (discreet) "#334155" else "#D9820B"))
                textSize = 11f
                typeface = Typeface.create(displayTypeface, Typeface.BOLD)
                gravity = Gravity.CENTER
                background = if (discreet) roundedBox("#E2E8F0", "#CBD5E1", 50f) else roundedBox("#FFF1CE", "#F2C667", 50f)
                setPadding(36, 14, 36, 14)
            }
            root.addView(badge)
        }

        val title = TextView(context).apply {
            text = customHeadline.ifBlank {
                if (discreet) "Device Access Paused" else "Device Locked for Safety"
            }
            setTextColor(Color.parseColor(if (discreet) "#1E293B" else "#1A3673"))
            textSize = 26f
            typeface = Typeface.create(displayTypeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, if (showAppBadge) 28 else 8, 0, 6)
        }
        root.addView(title)

        val subTitle = TextView(context).apply {
            text = customMessage.ifBlank {
                if (discreet) {
                    "Screen policy timer is currently active on this device."
                } else {
                    "Inappropriate visual content was detected and blocked automatically."
                }
            }
            setTextColor(Color.parseColor(if (discreet) "#475569" else "#3E5894"))
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }
        root.addView(subTitle)

        val timerCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = if (discreet) roundedBox("#FFFFFF", "#D8E0EA", 36f) else roundedBox("#FFF1CE", "#F2C667", 36f)
            setPadding(36, 30, 36, 30)
            visibility = if (showTimer) View.VISIBLE else View.GONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        countdownTitleView = TextView(context).apply {
            text = if (discreet) "REMAINING POLICY TIMER" else "REMAINING LOCKDOWN PENALTY"
            setTextColor(Color.parseColor(if (discreet) "#475569" else "#2563EB"))
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        timerCard.addView(countdownTitleView)

        countdownClockView = TextView(context).apply {
            text = "10:00:00"
            setTextColor(Color.parseColor(if (discreet) "#1E293B" else "#E59712"))
            textSize = 38f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 8)
        }
        timerCard.addView(countdownClockView)

        unlockHintView = TextView(context).apply {
            text = if (discreet) {
                "Access resumes automatically when timer completes."
            } else {
                "Guardian password prompt unlocks after countdown completes."
            }
            setTextColor(Color.parseColor(if (discreet) "#64748B" else "#5D76B0"))
            textSize = 12f
            gravity = Gravity.CENTER
        }
        timerCard.addView(unlockHintView)
        root.addView(timerCard)

        if (showTimer) {
            root.addView(verticalSpacer(20))
        }

        if (showAppBadge) {
            val timestampText = if (incident.detectedAtMillis > 0L) {
                SimpleDateFormat("MMM dd, yyyy • hh:mm:ss a", Locale.getDefault())
                    .format(Date(incident.detectedAtMillis))
            } else {
                "Just now"
            }

            val forensicsCard = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                background = if (discreet) roundedBox("#FFFFFF", "#D8E0EA", 36f) else roundedBox("#EDE6FF", "#C0AEFD", 36f)
                setPadding(34, 28, 34, 28)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val forensicsHeading = TextView(context).apply {
                text = if (discreet) "SYSTEM NOTICE DETAILS" else "INCIDENT FORENSICS — WHAT WAS DETECTED"
                setTextColor(Color.parseColor(if (discreet) "#334155" else "#7C5CFA"))
                textSize = 11f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                setPadding(0, 0, 0, 12)
            }
            forensicsCard.addView(forensicsHeading)

            val forensicsBody = TextView(context).apply {
                val pkgDisplay = if (incident.packageName.isNotBlank()) " (${incident.packageName})" else ""
                text = if (discreet) {
                    buildString {
                        append("• Component: ${incident.appName}$pkgDisplay\n")
                        append("• Triggered At: $timestampText\n")
                        val stage1Label = AegisPreferences.formatMinutesLabel(prefs.lockdownDurationMinutes.value)
                        val doubleLabel = AegisPreferences.formatMinutesLabel(prefs.getDoubleTimeDurationMinutes())
                        append("• Policy Window: $stage1Label cooldown (or $doubleLabel extended wait without access code).")
                    }
                } else {
                    buildString {
                        append("• Application: ${incident.appName}$pkgDisplay\n")
                        append("• Threat Category: ${incident.categoryName}\n")
                        append("• Detected At: $timestampText\n")
                        append("• Evidence / AI Scores:\n${incident.details.ifBlank { "Threshold violated on screen frame" }}\n")
                        val stage1Label = AegisPreferences.formatMinutesLabel(prefs.lockdownDurationMinutes.value)
                        val doubleLabel = AegisPreferences.formatMinutesLabel(prefs.getDoubleTimeDurationMinutes())
                        append("• Actions Taken: Offending app closed & Recents guard armed, background process stopped, $stage1Label lock (or $doubleLabel Double-Time wait without Master Code). Sheildlenz Shield remains ON after unlock.")
                    }
                }
                setTextColor(Color.parseColor(if (discreet) "#475569" else "#3E5894"))
                textSize = 12f
                setLineSpacing(6f, 1.0f)
            }
            forensicsCard.addView(forensicsBody)
            root.addView(forensicsCard)
            root.addView(verticalSpacer(22))
        }

        val pinCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = if (discreet) roundedBox("#FFFFFF", "#D8E0EA", 36f) else roundedBox("#E8F2FF", "#9AC2FE", 36f)
            setPadding(34, 26, 34, 26)
            visibility = if (showQuickActions) View.VISIBLE else View.GONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val pinLabel = TextView(context).apply {
            text = if (discreet) "ADMINISTRATOR ACCESS CODE" else "MASTER CODE / DOUBLE-TIME UNLOCK"
            setTextColor(Color.parseColor(if (discreet) "#334155" else "#2563EB"))
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 12)
        }
        pinCard.addView(pinLabel)

        val pinField = EditText(context).apply {
            hint = if (discreet) "Enter Access Code" else "Enter Guardian Master Code / PIN"
            setHintTextColor(Color.parseColor(if (discreet) "#94A3B8" else "#5D76B0"))
            setTextColor(Color.parseColor(if (discreet) "#1E293B" else "#1A3673"))
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            imeOptions = EditorInfo.IME_ACTION_DONE
            isSingleLine = true
            typeface = Typeface.MONOSPACE
            textSize = 14f
            gravity = Gravity.CENTER
            background = if (discreet) roundedBox("#F8FAFC", "#CBD5E1", 22f) else roundedBox("#EDE6FF", "#C0AEFD", 22f)
            setPadding(24, 22, 24, 22)
        }
        pinInputView = pinField
        pinCard.addView(pinField)

        pinCard.addView(verticalSpacer(14))

        val unlockBtn = Button(context).apply {
            text = if (discreet) "VERIFY ACCESS CODE" else "UNLOCK WITH MASTER CODE"
            setTextColor(Color.WHITE)
            background = roundedBox(if (discreet) "#334155" else "#2563EB", if (discreet) "#334155" else "#2563EB", 24f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 12f
            setPadding(24, 24, 24, 24)
            setOnClickListener {
                if (!prefs.canAcceptPasswordForLockdown()) {
                    val rem = formatDuration(prefs.getRemainingLockdownMillis())
                    Toast.makeText(
                        context,
                        if (discreet) "Locked for $rem." else "Device is locked. Master Code entry opens in $rem.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@setOnClickListener
                }
                val entered = pinField.text.toString()
                if (prefs.verifyGuardianPin(entered)) {
                    prefs.clearLockdown()
                    prefs.setShieldDesiredActive(true)
                    prefs.unlockGuardianSession()
                    AegisLockdownService.stop(context)
                    dismissBlockOverlay()
                    Toast.makeText(
                        context,
                        if (discreet) "Access verified." else "Master Code verified. Device unlocked • Sheildlenz Shield remains ON.",
                        Toast.LENGTH_LONG
                    ).show()
                    ensureShieldRemainsOnAfterUnlock()
                } else {
                    pinField.setText("")
                    val dblLabel = AegisPreferences.formatMinutesLabel(prefs.getDoubleTimeDurationMinutes()).uppercase(Locale.US)
                    val fallbackHint = if (discreet) {
                        if (!prefs.isInDoubleTimeStage()) {
                            "Incorrect access code. Select 'Wait $dblLabel' if you do not have the code."
                        } else {
                            "Incorrect access code. Wait for the countdown to reach 00:00:00."
                        }
                    } else if (!prefs.isInDoubleTimeStage()) {
                        "Incorrect Master Code. Tap 'Wait Double Time ($dblLabel)' if you don't know the code."
                    } else {
                        "Incorrect Master Code. Wait for the Double-Time countdown to reach 00:00:00."
                    }
                    Toast.makeText(context, fallbackHint, Toast.LENGTH_LONG).show()
                }
            }
        }
        bindPinSubmitOnEnter(pinField, unlockBtn)
        unlockButtonView = unlockBtn
        pinCard.addView(unlockBtn)

        pinCard.addView(verticalSpacer(12))

        val initialDoubleLabel = AegisPreferences.formatMinutesLabel(prefs.getDoubleTimeDurationMinutes()).uppercase(Locale.US)
        val doubleTimeBtn = Button(context).apply {
            text = if (discreet) {
                "NO CODE? WAIT $initialDoubleLabel"
            } else {
                "DON'T KNOW CODE? WAIT DOUBLE TIME ($initialDoubleLabel)"
            }
            setTextColor(Color.parseColor(if (discreet) "#334155" else "#D9820B"))
            background = roundedBox(if (discreet) "#F1F5F9" else "#FFF1CE", if (discreet) "#CBD5E1" else "#F2C667", 24f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 11f
            setPadding(20, 22, 20, 22)
            visibility = View.GONE
            setOnClickListener {
                prefs.engageDoubleTimeLockdown()
                AegisLockdownService.start(context)
                updateLockdownTimerUi()
                val dblLabel = AegisPreferences.formatMinutesLabel(prefs.getDoubleTimeDurationMinutes())
                Toast.makeText(
                    context,
                    if (discreet) "Extended cooldown ($dblLabel) active." else "Double-Time Wait ($dblLabel) engaged. Shield stays ON after unlock.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        doubleTimeButtonView = doubleTimeBtn
        pinCard.addView(doubleTimeBtn)

        pinCard.addView(verticalSpacer(12))

        val shieldNote = TextView(context).apply {
            text = if (discreet) {
                "Device display policy stays active after unlock."
            } else {
                "SHIELD CONTINUITY: Sheildlenz visual shield stays ON after both Master Code unlock and Double-Time completion."
            }
            setTextColor(Color.parseColor(if (discreet) "#64748B" else "#7C5CFA"))
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        shieldNoteView = shieldNote
        pinCard.addView(shieldNote)

        root.addView(pinCard)
        if (showQuickActions) {
            root.addView(verticalSpacer(18))
        }

        if (showQrUnlock) {
            val challengeCode = prefs.getRecoveryChallengeCode()
            val registeredEmail = prefs.guardianEmail.value.ifBlank { "Not configured during setup" }
            val timerQrPayload = prefs.buildRecoveryQrUri(RecoveryQrMode.TIMER_END)

            val dominantChallengeCard = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                background = if (discreet) roundedBox("#FFFFFF", "#D8E0EA", 32f) else roundedBox("#FFF1CE", "#E59712", 32f)
                setPadding(32, 24, 32, 24)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val recoveryHeading = TextView(context).apply {
                text = if (discreet) "END COOLDOWN BY RECOVERY CODE" else "END TIMER BY QR • GUARDIAN RECOVERY"
                setTextColor(Color.parseColor(if (discreet) "#334155" else "#D9820B"))
                textSize = 11f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 10)
            }
            dominantChallengeCard.addView(recoveryHeading)

            val qrBitmap = runCatching {
                QrCodeGenerator.encodeBitmap(content = timerQrPayload, sizePx = 360)
            }.getOrNull()
            if (qrBitmap != null) {
                val qrContainer = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    background = roundedBox("#FFFFFF", if (discreet) "#CBD5E1" else "#E59712", 24f)
                    setPadding(18, 18, 18, 18)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = 14
                    }
                }
                val qrImageView = ImageView(context).apply {
                    setImageBitmap(qrBitmap)
                    contentDescription = "Timer Recovery QR Code for Challenge $challengeCode"
                    adjustViewBounds = true
                    layoutParams = LinearLayout.LayoutParams(360, 360)
                }
                qrContainer.addView(qrImageView)
                dominantChallengeCard.addView(qrContainer)
            }

            val challengeCodeBadge = TextView(context).apply {
                text = challengeCode
                setTextColor(Color.parseColor(if (discreet) "#1E293B" else "#1A3673"))
                textSize = 20f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                gravity = Gravity.CENTER
                background = roundedBox("#FFFFFF", if (discreet) "#CBD5E1" else "#E59712", 20f)
                setPadding(32, 12, 32, 12)
            }
            dominantChallengeCard.addView(challengeCodeBadge)

            val recoveryInfo = TextView(context).apply {
                text = if (discreet) {
                    "Recovery Email: $registeredEmail\nScan on registered admin phone to generate a 1-time timer end code."
                } else {
                    "Registered Guardian Email: $registeredEmail\n" +
                        "Scan this QR with Sheildlenz on a parent phone signed in with the same email to get a 1-time code that ends this timer only (Shield remains ON)."
                }
                setTextColor(Color.parseColor(if (discreet) "#475569" else "#3E5894"))
                textSize = 11.5f
                gravity = Gravity.CENTER
                setPadding(0, 12, 0, 14)
            }
            dominantChallengeCard.addView(recoveryInfo)

            val recoveryInput = EditText(context).apply {
                hint = "Enter 8-Char One-Time Timer End Code"
                setHintTextColor(Color.parseColor("#5D76B0"))
                setTextColor(Color.parseColor("#1A3673"))
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
                imeOptions = EditorInfo.IME_ACTION_DONE
                isSingleLine = true
                typeface = Typeface.MONOSPACE
                textSize = 13f
                gravity = Gravity.CENTER
                background = roundedBox("#FFFFFF", "#F2C667", 18f)
                setPadding(20, 18, 20, 18)
            }

            val recoveryPanel = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                visibility = View.GONE
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 12
                }
            }
            recoveryPanel.addView(recoveryInput)
            recoveryPanel.addView(verticalSpacer(10))

            val verifyRecoveryBtn = Button(context).apply {
                text = "END TIMER (SHIELD STAYS ON)"
                setTextColor(Color.WHITE)
                background = roundedBox("#D9820B", "#D9820B", 20f)
                textSize = 11.5f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(20, 20, 20, 20)
                setOnClickListener {
                    val entered = recoveryInput.text.toString()
                    if (prefs.endLockdownTimerWithToken(entered)) {
                        AegisLockdownService.stop(context)
                        dismissBlockOverlay()
                        Toast.makeText(
                            context,
                            if (discreet) {
                                "Timer ended via one-time authorization code."
                            } else {
                                "Timer ended via Guardian one-time code. Sheildlenz Shield remains ON."
                            },
                            Toast.LENGTH_LONG
                        ).show()
                        ensureShieldRemainsOnAfterUnlock()
                    } else {
                        recoveryInput.setText("")
                        Toast.makeText(
                            context,
                            "Invalid timer-end code for Challenge ID $challengeCode.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            bindPinSubmitOnEnter(recoveryInput, verifyRecoveryBtn)
            recoveryPanel.addView(verifyRecoveryBtn)

            val recoveryToggleBtn = Button(context).apply {
                text = "ENTER TIMER END CODE"
                setTextColor(Color.WHITE)
                background = roundedBox("#7C5CFA", "#7C5CFA", 18f)
                textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(24, 18, 24, 18)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setOnClickListener {
                    val nextVisible = recoveryPanel.visibility != View.VISIBLE
                    recoveryPanel.visibility = if (nextVisible) View.VISIBLE else View.GONE
                    text = if (nextVisible) "HIDE CODE FORM" else "ENTER TIMER END CODE"
                }
            }

            dominantChallengeCard.addView(recoveryToggleBtn)
            dominantChallengeCard.addView(recoveryPanel)
            root.addView(dominantChallengeCard)
            root.addView(verticalSpacer(16))
        }

        // Independent Emergency Dialer & Return Home controls (never tied to QR card visibility)
        if (showEmergencyDialer) {
            val emergencyBtn = Button(context).apply {
                text = "EMERGENCY DIALER"
                setTextColor(Color.parseColor(if (discreet) "#334155" else "#D9820B"))
                background = roundedBox("#FFFFFF", if (discreet) "#CBD5E1" else "#F2C667", 24f)
                textSize = 11.5f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                setPadding(24, 22, 24, 22)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setOnClickListener {
                    val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    try {
                        context.startActivity(dialIntent)
                    } catch (_: Exception) {
                    }
                }
            }
            root.addView(emergencyBtn)
            root.addView(verticalSpacer(12))
        }

        if (showReturnHome) {
            val homeBtn = Button(context).apply {
                text = "RETURN TO HOME SCREEN"
                setTextColor(Color.parseColor(if (discreet) "#334155" else "#2563EB"))
                background = if (discreet) roundedBox("#FFFFFF", "#CBD5E1", 24f) else roundedBox("#E8F2FF", "#9AC2FE", 24f)
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(24, 22, 24, 22)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setOnClickListener {
                    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(homeIntent)
                    dismissBlockOverlay()
                }
            }
            root.addView(homeBtn)
            root.addView(verticalSpacer(12))
        }

        if (!discreet && showAppBadge) {
            root.addView(verticalSpacer(10))
            root.addView(createOverlayBrandingView(allowOpenLink = false))
        }

        scroll.addView(root)
        return scroll
    }

    private fun createTamperLayout(reason: String): View {
        val prefs = AegisContainer.getInstance(context).preferences
        val discreet = prefs.discreetBlockingScreenMode.value
        val customHeadline = prefs.blockCustomHeadline.value.trim()
        val customMessage = prefs.blockCustomMessage.value.trim()
        val showAppBadge = prefs.blockShowAppBadge.value
        val showQuickActions = prefs.blockShowQuickActions.value
        val showReturnHome = prefs.blockShowReturnHome.value || !showQuickActions

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                if (discreet) {
                    intArrayOf(Color.parseColor("#F3F5F8"), Color.parseColor("#EAEFF5"))
                } else {
                    intArrayOf(Color.parseColor("#D2E5FF"), Color.parseColor("#E6E4FF"), Color.parseColor("#FFF0CC"))
                }
            )
            setPadding(56, 72, 56, 72)
            isClickable = true
            isFocusable = true
        }

        if (showAppBadge) {
            val badge = TextView(context).apply {
                text = if (discreet) "SYSTEM CONFIGURATION LOCKED" else "ANTI-TAMPER PROTECTION ACTIVE"
                setTextColor(Color.parseColor(if (discreet) "#334155" else "#D9820B"))
                textSize = 11f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                gravity = Gravity.CENTER
                background = if (discreet) roundedBox("#E2E8F0", "#CBD5E1", 50f) else roundedBox("#FFF1CE", "#F2C667", 50f)
                setPadding(32, 12, 32, 12)
            }
            root.addView(badge)
        }

        val title = TextView(context).apply {
            text = customHeadline.ifBlank {
                if (discreet) "Administrator Authorization" else "Guardian Authorization Required"
            }
            setTextColor(Color.parseColor(if (discreet) "#1E293B" else "#1A3673"))
            textSize = 23f
            typeface = Typeface.create(displayTypeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, if (showAppBadge) 26 else 8, 0, 12)
        }
        root.addView(title)

        val desc = TextView(context).apply {
            text = customMessage.ifBlank {
                if (discreet) {
                    "Protected system component settings cannot be changed without administrator authorization."
                } else {
                    reason
                }
            }
            setTextColor(Color.parseColor(if (discreet) "#475569" else "#3E5894"))
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(28, 20, 28, 24)
            background = if (discreet) roundedBox("#FFFFFF", "#D8E0EA", 32f) else roundedBox("#E8F2FF", "#9AC2FE", 32f)
        }
        root.addView(desc)

        root.addView(verticalSpacer(22))

        if (showQuickActions) {
            val pinField = EditText(context).apply {
                hint = if (discreet) "Enter Access Code" else "Enter Guardian Password / PIN"
                setHintTextColor(Color.parseColor(if (discreet) "#94A3B8" else "#5D76B0"))
                setTextColor(Color.parseColor(if (discreet) "#1E293B" else "#1A3673"))
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
                imeOptions = EditorInfo.IME_ACTION_DONE
                isSingleLine = true
                typeface = Typeface.MONOSPACE
                textSize = 14f
                gravity = Gravity.CENTER
                background = if (discreet) roundedBox("#FFFFFF", "#CBD5E1", 22f) else roundedBox("#EDE6FF", "#C0AEFD", 22f)
                setPadding(28, 22, 28, 22)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            root.addView(pinField)

            root.addView(verticalSpacer(16))

            val windowMins = prefs.guardianSettingsWindowMinutes.value
            val verifyBtn = Button(context).apply {
                text = if (discreet) "AUTHORIZE ($windowMins MIN WINDOW)" else "AUTHORIZE GUARDIAN ($windowMins MIN WINDOW)"
                setTextColor(Color.WHITE)
                background = roundedBox(if (discreet) "#334155" else "#7C5CFA", if (discreet) "#334155" else "#7C5CFA", 24f)
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(24, 24, 24, 24)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setOnClickListener {
                    val entered = pinField.text.toString()
                    if (prefs.verifyGuardianPin(entered)) {
                        val mins = prefs.guardianSettingsWindowMinutes.value
                        prefs.unlockGuardianSession(grantSettingsWindowMinutes = mins)
                        dismissBlockOverlay()
                        Toast.makeText(
                            context,
                            if (discreet) "Authorized for $mins minute(s)." else "Guardian verified. System settings authorized for $mins minute(s).",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        pinField.setText("")
                        Toast.makeText(context, if (discreet) "Incorrect access code." else "Incorrect Guardian Password.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            bindPinSubmitOnEnter(pinField, verifyBtn)
            root.addView(verifyBtn)

            root.addView(verticalSpacer(14))
        }

        if (showReturnHome) {
            val homeBtn = Button(context).apply {
                text = "RETURN TO HOME SCREEN"
                setTextColor(Color.parseColor(if (discreet) "#334155" else "#2563EB"))
                background = if (discreet) roundedBox("#FFFFFF", "#CBD5E1", 24f) else roundedBox("#E8F2FF", "#9AC2FE", 24f)
                textSize = 12f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setOnClickListener {
                    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(homeIntent)
                    dismissBlockOverlay()
                }
            }
            root.addView(homeBtn)
        }

        if (!discreet && showAppBadge) {
            root.addView(verticalSpacer(24))
            root.addView(createOverlayBrandingView(allowOpenLink = true))
        }

        return root
    }

    private fun createWarningLayout(
        appName: String,
        packageName: String,
        categoryName: String,
        warningNumber: Int,
        maxWarnings: Int,
        warningOnly: Boolean = false
    ): View {
        val prefs = AegisContainer.getInstance(context).preferences
        val discreet = prefs.discreetBlockingScreenMode.value
        val customHeadline = prefs.blockCustomHeadline.value.trim()
        val customMessage = prefs.blockCustomMessage.value.trim()
        val showAppBadge = prefs.blockShowAppBadge.value

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                if (discreet) {
                    intArrayOf(Color.parseColor("#F3F5F8"), Color.parseColor("#EAEFF5"))
                } else {
                    intArrayOf(Color.parseColor("#D2E5FF"), Color.parseColor("#FFF0CC"))
                }
            )
            setPadding(56, 72, 56, 72)
            isClickable = true
            isFocusable = true
        }

        if (showAppBadge) {
            val badge = TextView(context).apply {
                text = com.example.core.alerts.DiscreetAlertFormatter.warningNoticeBadge(
                    discreetMode = discreet,
                    warningNumber = warningNumber,
                    maxWarnings = maxWarnings,
                    warningOnly = warningOnly
                )
                setTextColor(Color.parseColor(if (discreet) "#334155" else "#D9820B"))
                textSize = 11f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                gravity = Gravity.CENTER
                background = if (discreet) roundedBox("#E2E8F0", "#CBD5E1", 50f) else roundedBox("#FFF1CE", "#F2C667", 50f)
                setPadding(32, 12, 32, 12)
            }
            root.addView(badge)
        }

        val title = TextView(context).apply {
            text = customHeadline.ifBlank {
                com.example.core.alerts.DiscreetAlertFormatter.warningNoticeHeadline(discreet)
            }
            setTextColor(Color.parseColor(if (discreet) "#1E293B" else "#1A3673"))
            textSize = 24f
            typeface = Typeface.create(displayTypeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, if (showAppBadge) 26 else 8, 0, 12)
        }
        root.addView(title)

        val desc = TextView(context).apply {
            text = customMessage.ifBlank {
                com.example.core.alerts.DiscreetAlertFormatter.warningNoticeBody(
                    discreetMode = discreet,
                    appName = appName,
                    packageName = packageName,
                    categoryName = categoryName,
                    warningNumber = warningNumber,
                    maxWarnings = maxWarnings,
                    warningOnly = warningOnly,
                    showAppBadge = showAppBadge
                )
            }
            setTextColor(Color.parseColor(if (discreet) "#475569" else "#3E5894"))
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(28, 22, 28, 24)
            background = if (discreet) roundedBox("#FFFFFF", "#D8E0EA", 32f) else roundedBox("#E8F2FF", "#9AC2FE", 32f)
        }
        root.addView(desc)

        root.addView(verticalSpacer(22))

        val homeBtn = Button(context).apply {
            text = if (discreet) "DISMISS & GO HOME" else "ACKNOWLEDGE & RETURN HOME"
            setTextColor(Color.WHITE)
            background = roundedBox(if (discreet) "#334155" else "#2563EB", if (discreet) "#334155" else "#2563EB", 24f)
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(24, 24, 24, 24)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(homeIntent)
                dismissBlockOverlay()
            }
        }
        root.addView(homeBtn)
        if (!discreet && showAppBadge) {
            root.addView(verticalSpacer(24))
            root.addView(createOverlayBrandingView(allowOpenLink = false))
        }
        return root
    }

    private fun createUsageOrBedtimeLayout(
        titleText: String,
        subtitleText: String,
        packageName: String,
        allowGuardianExtension: Boolean
    ): View {
        val prefs = AegisContainer.getInstance(context).preferences
        val discreet = prefs.discreetBlockingScreenMode.value
        val customHeadline = prefs.blockCustomHeadline.value.trim()
        val customMessage = prefs.blockCustomMessage.value.trim()
        val showAppBadge = prefs.blockShowAppBadge.value
        val showQuickActions = prefs.blockShowQuickActions.value
        val hasExtension = allowGuardianExtension && prefs.hasGuardianPin.value && showQuickActions
        val showReturnHome = prefs.blockShowReturnHome.value || !hasExtension

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                if (discreet) {
                    intArrayOf(Color.parseColor("#F3F5F8"), Color.parseColor("#EAEFF5"))
                } else {
                    intArrayOf(Color.parseColor("#D2E5FF"), Color.parseColor("#E6E4FF"))
                }
            )
            setPadding(56, 72, 56, 72)
            isClickable = true
            isFocusable = true
        }

        if (showAppBadge) {
            val badge = TextView(context).apply {
                text = if (discreet) "DIGITAL WELLBEING POLICY" else "SHEILDLENZ WELLBEING GUARD"
                setTextColor(Color.parseColor(if (discreet) "#334155" else "#2563EB"))
                textSize = 11f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                gravity = Gravity.CENTER
                background = if (discreet) roundedBox("#E2E8F0", "#CBD5E1", 50f) else roundedBox("#E8F2FF", "#9AC2FE", 50f)
                setPadding(32, 12, 32, 12)
            }
            root.addView(badge)
        }

        val title = TextView(context).apply {
            text = customHeadline.ifBlank { titleText }
            setTextColor(Color.parseColor(if (discreet) "#1E293B" else "#1A3673"))
            textSize = 23f
            typeface = Typeface.create(displayTypeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, if (showAppBadge) 26 else 8, 0, 12)
        }
        root.addView(title)

        val desc = TextView(context).apply {
            text = customMessage.ifBlank {
                if (discreet) {
                    subtitleText.replace("Guardian ", "").replace("Sheildlenz ", "")
                } else {
                    subtitleText
                }
            }
            setTextColor(Color.parseColor(if (discreet) "#475569" else "#3E5894"))
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(28, 22, 28, 24)
            background = if (discreet) roundedBox("#FFFFFF", "#D8E0EA", 32f) else roundedBox("#EDE6FF", "#C0AEFD", 32f)
        }
        root.addView(desc)

        root.addView(verticalSpacer(20))

        if (hasExtension) {
            val pinField = EditText(context).apply {
                hint = if (discreet) "Access Code for +15m Extension" else "Guardian PIN for +15m Extension"
                setHintTextColor(Color.parseColor(if (discreet) "#94A3B8" else "#5D76B0"))
                setTextColor(Color.parseColor(if (discreet) "#1E293B" else "#1A3673"))
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
                imeOptions = EditorInfo.IME_ACTION_DONE
                isSingleLine = true
                typeface = Typeface.MONOSPACE
                textSize = 14f
                gravity = Gravity.CENTER
                background = if (discreet) roundedBox("#FFFFFF", "#CBD5E1", 22f) else roundedBox("#E8F2FF", "#9AC2FE", 22f)
                setPadding(24, 20, 24, 20)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            root.addView(pinField)
            root.addView(verticalSpacer(12))

            val extendBtn = Button(context).apply {
                text = "EXTEND APP LIMIT +15 MIN"
                setTextColor(Color.WHITE)
                background = roundedBox(if (discreet) "#334155" else "#7C5CFA", if (discreet) "#334155" else "#7C5CFA", 24f)
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(24, 22, 24, 22)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setOnClickListener {
                    if (prefs.verifyGuardianPin(pinField.text.toString())) {
                        prefs.grantTemporaryBudgetExtension(packageName, 15)
                        dismissBlockOverlay()
                        Toast.makeText(context, "Granted +15 minutes.", Toast.LENGTH_SHORT).show()
                    } else {
                        pinField.setText("")
                        Toast.makeText(context, if (discreet) "Incorrect access code." else "Incorrect Guardian PIN.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            bindPinSubmitOnEnter(pinField, extendBtn)
            root.addView(extendBtn)
            root.addView(verticalSpacer(12))
        }

        if (showReturnHome) {
            val homeBtn = Button(context).apply {
                text = "RETURN TO HOME SCREEN"
                setTextColor(Color.parseColor(if (discreet) "#334155" else "#2563EB"))
                background = if (discreet) roundedBox("#FFFFFF", "#CBD5E1", 24f) else roundedBox("#E8F2FF", "#9AC2FE", 24f)
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(24, 22, 24, 22)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setOnClickListener {
                    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(homeIntent)
                    dismissBlockOverlay()
                }
            }
            root.addView(homeBtn)
        }
        if (!discreet && showAppBadge) {
            root.addView(verticalSpacer(24))
            root.addView(createOverlayBrandingView(allowOpenLink = false))
        }
        return root
    }

    private fun createStudySoftWarningLayout(
        appName: String,
        initialSeconds: Int
    ): View {
        val prefs = AegisContainer.getInstance(context).preferences
        val discreet = prefs.discreetBlockingScreenMode.value
        val customHeadline = prefs.blockCustomHeadline.value.trim()
        val customMessage = prefs.blockCustomMessage.value.trim()
        val showAppBadge = prefs.blockShowAppBadge.value
        val showTimer = prefs.blockShowTimer.value

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                if (discreet) {
                    intArrayOf(Color.parseColor("#F3F5F8"), Color.parseColor("#EAEFF5"))
                } else {
                    intArrayOf(Color.parseColor("#D2E5FF"), Color.parseColor("#FFF0CC"))
                }
            )
            setPadding(56, 72, 56, 72)
            isClickable = true
            isFocusable = true
        }

        if (showAppBadge) {
            val badge = TextView(context).apply {
                text = if (discreet) "FOCUS MODE • COUNTDOWN" else "STUDY MODE • SOFT WARNING"
                setTextColor(Color.parseColor(if (discreet) "#334155" else "#D9820B"))
                textSize = 11f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                gravity = Gravity.CENTER
                background = if (discreet) roundedBox("#E2E8F0", "#CBD5E1", 50f) else roundedBox("#FFF1CE", "#F2C667", 50f)
                setPadding(32, 12, 32, 12)
            }
            root.addView(badge)
        }

        val title = TextView(context).apply {
            text = customHeadline.ifBlank { "$appName Pauses Soon" }
            setTextColor(Color.parseColor(if (discreet) "#1E293B" else "#1A3673"))
            textSize = 24f
            typeface = Typeface.create(displayTypeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, if (showAppBadge) 26 else 8, 0, 8)
        }
        root.addView(title)

        val clockCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = if (discreet) roundedBox("#FFFFFF", "#D8E0EA", 32f) else roundedBox("#FFF1CE", "#F2C667", 32f)
            setPadding(36, 26, 36, 26)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val clockLabel = TextView(context).apply {
            text = "WRAPPING UP BEFORE FOCUS LOCK"
            setTextColor(Color.parseColor(if (discreet) "#64748B" else "#7C5CFA"))
            textSize = 10.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            gravity = Gravity.CENTER
            visibility = if (showTimer) View.VISIBLE else View.GONE
        }
        clockCard.addView(clockLabel)

        val countdownClock = TextView(context).apply {
            text = "${initialSeconds}s"
            setTextColor(Color.parseColor(if (discreet) "#1E293B" else "#D9820B"))
            textSize = 42f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 6, 0, 6)
            visibility = if (showTimer) View.VISIBLE else View.GONE
        }
        softWarningClockView = countdownClock
        clockCard.addView(countdownClock)

        val remainingStudyMins = (prefs.getRemainingStudyMillis() / 60_000L).coerceAtLeast(1L)
        val desc = TextView(context).apply {
            text = customMessage.ifBlank {
                "A focus session is running ($remainingStudyMins min remaining). Switch back to an allowed study app before the countdown finishes."
            }
            setTextColor(Color.parseColor(if (discreet) "#475569" else "#3E5894"))
            textSize = 12.5f
            gravity = Gravity.CENTER
        }
        clockCard.addView(desc)
        root.addView(clockCard)

        root.addView(verticalSpacer(22))

        val returnBtn = Button(context).apply {
            text = "RETURN TO FOCUS (GO HOME)"
            setTextColor(Color.WHITE)
            background = roundedBox(if (discreet) "#334155" else "#2563EB", if (discreet) "#334155" else "#2563EB", 24f)
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(24, 24, 24, 24)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(homeIntent)
                dismissBlockOverlay()
            }
        }
        root.addView(returnBtn)

        if (!discreet && showAppBadge) {
            root.addView(verticalSpacer(24))
            root.addView(createOverlayBrandingView(allowOpenLink = false))
        }
        return root
    }

    private fun createStudyLockLayout(
        appName: String,
        packageName: String
    ): View {
        val prefs = AegisContainer.getInstance(context).preferences
        val discreet = prefs.discreetBlockingScreenMode.value
        val customHeadline = prefs.blockCustomHeadline.value.trim()
        val customMessage = prefs.blockCustomMessage.value.trim()
        val showAppBadge = prefs.blockShowAppBadge.value
        val showTimer = prefs.blockShowTimer.value
        val showQuickActions = prefs.blockShowQuickActions.value
        val hasUnlockAction = prefs.hasGuardianPin.value && showQuickActions
        val showReturnHome = prefs.blockShowReturnHome.value || !hasUnlockAction
        val remainingMins = (prefs.getRemainingStudyMillis() / 60_000L).coerceAtLeast(1L)

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                if (discreet) {
                    intArrayOf(Color.parseColor("#F3F5F8"), Color.parseColor("#EAEFF5"))
                } else {
                    intArrayOf(Color.parseColor("#D2E5FF"), Color.parseColor("#E6E4FF"))
                }
            )
            setPadding(56, 72, 56, 72)
            isClickable = true
            isFocusable = true
        }

        if (showAppBadge) {
            val badge = TextView(context).apply {
                text = if (discreet) "FOCUS SESSION ACTIVE" else "SHEILDLENZ STUDY LOCK"
                setTextColor(Color.parseColor(if (discreet) "#334155" else "#2563EB"))
                textSize = 11f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                gravity = Gravity.CENTER
                background = if (discreet) roundedBox("#E2E8F0", "#CBD5E1", 50f) else roundedBox("#E8F2FF", "#9AC2FE", 50f)
                setPadding(32, 12, 32, 12)
            }
            root.addView(badge)
        }

        val title = TextView(context).apply {
            text = customHeadline.ifBlank {
                if (showAppBadge) "$appName Paused for Focus" else "App Paused for Focus"
            }
            setTextColor(Color.parseColor(if (discreet) "#1E293B" else "#1A3673"))
            textSize = 23f
            typeface = Typeface.create(displayTypeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, if (showAppBadge) 26 else 8, 0, 12)
        }
        root.addView(title)

        val desc = TextView(context).apply {
            text = customMessage.ifBlank {
                when {
                    showTimer && showAppBadge -> "Focus timer has ~$remainingMins minute(s) left. $appName will be available again when the session finishes."
                    showTimer -> "Focus timer has ~$remainingMins minute(s) left. Access resumes automatically when the session finishes."
                    showAppBadge -> "$appName is paused during active Study Mode."
                    else -> "This app is paused during your active Study Mode focus session."
                }
            }
            setTextColor(Color.parseColor(if (discreet) "#475569" else "#3E5894"))
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(28, 22, 28, 24)
            background = if (discreet) roundedBox("#FFFFFF", "#D8E0EA", 32f) else roundedBox("#EDE6FF", "#C0AEFD", 32f)
        }
        root.addView(desc)

        root.addView(verticalSpacer(20))

        if (hasUnlockAction) {
            val pinField = EditText(context).apply {
                hint = if (discreet) "Access Code (End Focus)" else "Guardian PIN (End Study Mode)"
                setHintTextColor(Color.parseColor(if (discreet) "#94A3B8" else "#5D76B0"))
                setTextColor(Color.parseColor(if (discreet) "#1E293B" else "#1A3673"))
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
                imeOptions = EditorInfo.IME_ACTION_DONE
                isSingleLine = true
                typeface = Typeface.MONOSPACE
                textSize = 14f
                gravity = Gravity.CENTER
                background = if (discreet) roundedBox("#FFFFFF", "#CBD5E1", 22f) else roundedBox("#E8F2FF", "#9AC2FE", 22f)
                setPadding(24, 20, 24, 20)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            root.addView(pinField)
            root.addView(verticalSpacer(12))

            val endFocusBtn = Button(context).apply {
                text = if (discreet) "UNLOCK WITH ACCESS CODE" else "END STUDY SESSION"
                setTextColor(Color.WHITE)
                background = roundedBox(if (discreet) "#334155" else "#7C5CFA", if (discreet) "#334155" else "#7C5CFA", 24f)
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(24, 22, 24, 22)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setOnClickListener {
                    if (prefs.verifyGuardianPin(pinField.text.toString())) {
                        prefs.stopStudySession()
                        dismissBlockOverlay()
                        Toast.makeText(context, "Focus session ended.", Toast.LENGTH_SHORT).show()
                    } else {
                        pinField.setText("")
                        Toast.makeText(context, if (discreet) "Incorrect access code." else "Incorrect Guardian PIN.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            bindPinSubmitOnEnter(pinField, endFocusBtn)
            root.addView(endFocusBtn)
            root.addView(verticalSpacer(12))
        }

        if (showReturnHome) {
            val homeBtn = Button(context).apply {
                text = "RETURN TO HOME SCREEN"
                setTextColor(Color.parseColor(if (discreet) "#334155" else "#2563EB"))
                background = if (discreet) roundedBox("#FFFFFF", "#CBD5E1", 24f) else roundedBox("#E8F2FF", "#9AC2FE", 24f)
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(24, 22, 24, 22)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setOnClickListener {
                    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(homeIntent)
                    dismissBlockOverlay()
                }
            }
            root.addView(homeBtn)
        }
        if (!discreet && showAppBadge) {
            root.addView(verticalSpacer(24))
            root.addView(createOverlayBrandingView(allowOpenLink = false))
        }
        return root
    }

    private fun createAppLockerLayout(
        appName: String,
        packageName: String
    ): View {
        val prefs = AegisContainer.getInstance(context).preferences
        val discreet = prefs.discreetBlockingScreenMode.value
        val showAppBadge = prefs.blockShowAppBadge.value
        val relockMins = prefs.appLockerRelockMinutes.value

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                if (discreet) {
                    intArrayOf(Color.parseColor("#F3F5F8"), Color.parseColor("#EAEFF5"))
                } else {
                    intArrayOf(Color.parseColor("#D2E5FF"), Color.parseColor("#E6E4FF"), Color.parseColor("#EDE6FF"))
                }
            )
            setPadding(56, 72, 56, 72)
            isClickable = true
            isFocusable = true
        }

        if (showAppBadge) {
            val badge = TextView(context).apply {
                text = if (discreet) "PROTECTED APPLICATION" else "GUARDIAN APP LOCKER"
                setTextColor(Color.parseColor(if (discreet) "#334155" else "#7C5CFA"))
                textSize = 11f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                gravity = Gravity.CENTER
                background = if (discreet) roundedBox("#E2E8F0", "#CBD5E1", 50f) else roundedBox("#EDE6FF", "#C0AEFD", 50f)
                setPadding(32, 12, 32, 12)
            }
            root.addView(badge)
        }

        val title = TextView(context).apply {
            text = "App Locked by Guardian"
            setTextColor(Color.parseColor(if (discreet) "#1E293B" else "#1A3673"))
            textSize = 23f
            typeface = Typeface.create(displayTypeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, if (showAppBadge) 24 else 8, 0, 10)
        }
        root.addView(title)

        val relockText = if (relockMins <= 0) {
            "Re-locks automatically when you leave this app."
        } else {
            "Stays unlocked for $relockMins minute(s) after PIN entry."
        }

        val desc = TextView(context).apply {
            text = "Enter Guardian PIN to unlock $appName.\n$relockText"
            setTextColor(Color.parseColor(if (discreet) "#475569" else "#3E5894"))
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(28, 22, 28, 24)
            background = if (discreet) roundedBox("#FFFFFF", "#D8E0EA", 32f) else roundedBox("#E8F2FF", "#9AC2FE", 32f)
        }
        root.addView(desc)

        root.addView(verticalSpacer(20))

        val pinField = EditText(context).apply {
            hint = if (discreet) "Enter Access Code" else "Enter Guardian PIN"
            setHintTextColor(Color.parseColor(if (discreet) "#94A3B8" else "#5D76B0"))
            setTextColor(Color.parseColor(if (discreet) "#1E293B" else "#1A3673"))
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            imeOptions = EditorInfo.IME_ACTION_DONE
            isSingleLine = true
            typeface = Typeface.MONOSPACE
            textSize = 14f
            gravity = Gravity.CENTER
            background = if (discreet) roundedBox("#FFFFFF", "#CBD5E1", 22f) else roundedBox("#EDE6FF", "#C0AEFD", 22f)
            setPadding(24, 20, 24, 20)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        root.addView(pinField)
        root.addView(verticalSpacer(14))

        val unlockBtn = Button(context).apply {
            text = "UNLOCK $appName".uppercase(Locale.US)
            setTextColor(Color.WHITE)
            background = roundedBox(if (discreet) "#334155" else "#7C5CFA", if (discreet) "#334155" else "#7C5CFA", 24f)
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(24, 22, 24, 22)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                if (prefs.verifyGuardianPin(pinField.text.toString())) {
                    prefs.grantTempAppUnlock(packageName)
                    dismissBlockOverlay()
                    runCatching {
                        context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                        }?.let { context.startActivity(it) }
                    }
                    Toast.makeText(context, "$appName unlocked", Toast.LENGTH_SHORT).show()
                } else {
                    pinField.setText("")
                    Toast.makeText(context, if (discreet) "Incorrect access code." else "Incorrect Guardian PIN.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        bindPinSubmitOnEnter(pinField, unlockBtn)
        root.addView(unlockBtn)
        root.addView(verticalSpacer(12))

        val homeBtn = Button(context).apply {
            text = "RETURN TO HOME SCREEN"
            setTextColor(Color.parseColor(if (discreet) "#334155" else "#2563EB"))
            background = if (discreet) roundedBox("#FFFFFF", "#CBD5E1", 24f) else roundedBox("#E8F2FF", "#9AC2FE", 24f)
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(24, 22, 24, 22)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(homeIntent)
                dismissBlockOverlay()
            }
        }
        root.addView(homeBtn)

        if (!discreet && showAppBadge) {
            root.addView(verticalSpacer(24))
            root.addView(createOverlayBrandingView(allowOpenLink = false))
        }
        return root
    }

    private fun createOverlayBrandingView(allowOpenLink: Boolean): View {
        return TextView(context).apply {
            text = "Built By AhsanTCr  •  ahsantcr.lovable.app"
            setTextColor(Color.parseColor("#1A3673"))
            textSize = 11f
            typeface = Typeface.create(displayTypeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            background = roundedBox("#FFF1CE", "#F2C667", 50f)
            setPadding(34, 16, 34, 16)
            if (allowOpenLink) {
                setOnClickListener {
                    dismissBlockOverlay()
                    com.example.ui.components.openAhsanTcrContact(context)
                }
            }
        }
    }

    private fun updateLockdownTimerUi() {
        val prefs = AegisContainer.getInstance(context).preferences
        val discreet = prefs.discreetBlockingScreenMode.value

        if (prefs.completeDoubleTimeIfExpired()) {
            onDoubleTimeUnlockCompleted()
            return
        }

        val remainingMs = prefs.getRemainingLockdownMillis()
        val canEnterPassword = prefs.canAcceptPasswordForLockdown()
        val isDoubleTime = prefs.isInDoubleTimeStage()
        val stage1Label = AegisPreferences.formatMinutesLabel(prefs.lockdownDurationMinutes.value)
        val stage1LabelUpper = stage1Label.uppercase(Locale.US)
        val doubleLabel = AegisPreferences.formatMinutesLabel(prefs.getDoubleTimeDurationMinutes())
        val doubleLabelUpper = doubleLabel.uppercase(Locale.US)

        countdownClockView?.text = formatDuration(remainingMs)

        when {
            isDoubleTime && remainingMs > 0L -> {
                countdownTitleView?.text = if (discreet) {
                    "EXTENDED WAIT PERIOD ($doubleLabelUpper)"
                } else {
                    "STAGE 2 • DOUBLE-TIME WAIT ($doubleLabelUpper)"
                }
                countdownClockView?.setTextColor(Color.parseColor(if (discreet) "#334155" else "#E59712"))
                unlockHintView?.text = if (discreet) {
                    "Extended wait active. Access restores automatically when timer reaches 00:00:00 (or earlier with an access code)."
                } else {
                    "Double-time wait active. Device opens automatically when timer hits 00:00:00 (or earlier with Guardian Master Code). Sheildlenz Shield remains ON."
                }
                pinInputView?.visibility = View.VISIBLE
                unlockButtonView?.isEnabled = true
                unlockButtonView?.text = if (discreet) "UNLOCK WITH ACCESS CODE" else "UNLOCK WITH MASTER CODE"
                unlockButtonView?.background = roundedBox(if (discreet) "#334155" else "#2563EB", if (discreet) "#334155" else "#2563EB", 24f)
                unlockButtonView?.setTextColor(Color.WHITE)
                doubleTimeButtonView?.visibility = View.GONE
            }

            !isDoubleTime && remainingMs > 0L -> {
                countdownTitleView?.text = if (discreet) {
                    "RESTRICTION TIMER ($stage1LabelUpper)"
                } else {
                    "STAGE 1 • LOCKDOWN TIMER ($stage1LabelUpper)"
                }
                countdownClockView?.setTextColor(Color.parseColor(if (discreet) "#334155" else "#E59712"))
                if (!canEnterPassword) {
                    unlockHintView?.text = if (discreet) {
                        "Device access is temporarily paused. When the timer finishes, enter the access code or complete the extended wait ($doubleLabel)."
                    } else {
                        "Whole device is locked. After countdown completes: enter Master Code, or wait Double Time ($doubleLabel). Shield stays ON."
                    }
                    pinInputView?.visibility = View.GONE
                    unlockButtonView?.isEnabled = false
                    unlockButtonView?.text = if (discreet) "ACCESS CODE LOCKED (${formatDuration(remainingMs)})" else "MASTER CODE LOCKED (${formatDuration(remainingMs)})"
                    unlockButtonView?.background = roundedBox(if (discreet) "#E2E8F0" else "#EDE6FF", if (discreet) "#CBD5E1" else "#C0AEFD", 24f)
                    unlockButtonView?.setTextColor(Color.parseColor(if (discreet) "#64748B" else "#7C5CFA"))
                    doubleTimeButtonView?.visibility = View.GONE
                } else {
                    unlockHintView?.text = if (discreet) {
                        "Enter the access code to unlock, or select Extended Wait ($doubleLabel)."
                    } else {
                        "Enter Master Code to unlock, or tap Wait Double Time ($doubleLabel) if you do not know the code. Shield remains ON."
                    }
                    pinInputView?.visibility = View.VISIBLE
                    unlockButtonView?.isEnabled = true
                    unlockButtonView?.text = if (discreet) "UNLOCK WITH ACCESS CODE" else "UNLOCK WITH MASTER CODE"
                    unlockButtonView?.background = roundedBox(if (discreet) "#334155" else "#2563EB", if (discreet) "#334155" else "#2563EB", 24f)
                    unlockButtonView?.setTextColor(Color.WHITE)
                    doubleTimeButtonView?.visibility = View.VISIBLE
                    doubleTimeButtonView?.text = if (discreet) {
                        "NO CODE? WAIT $doubleLabelUpper"
                    } else {
                        "DON'T KNOW CODE? WAIT DOUBLE TIME ($doubleLabelUpper)"
                    }
                }
            }

            else -> {
                countdownTitleView?.text = if (discreet) "TIMER COMPLETE • ACCESS CODE REQUIRED" else "TIME COMPLETE • MASTER CODE REQUIRED"
                countdownClockView?.text = "00:00:00"
                countdownClockView?.setTextColor(Color.parseColor(if (discreet) "#334155" else "#2563EB"))
                unlockHintView?.text = if (discreet) {
                    "Initial timer finished. Enter access code to continue, or select extended wait ($doubleLabel)."
                } else {
                    "Initial timer finished. Enter Master Code to open now, or wait Double Time ($doubleLabel) if you don't know the code. Shield remains ON after both."
                }
                pinInputView?.visibility = View.VISIBLE
                unlockButtonView?.isEnabled = true
                unlockButtonView?.text = if (discreet) "UNLOCK WITH ACCESS CODE" else "UNLOCK WITH MASTER CODE"
                unlockButtonView?.background = roundedBox(if (discreet) "#334155" else "#2563EB", if (discreet) "#334155" else "#2563EB", 24f)
                unlockButtonView?.setTextColor(Color.WHITE)
                doubleTimeButtonView?.visibility = View.VISIBLE
                doubleTimeButtonView?.text = if (discreet) {
                    "NO CODE? WAIT $doubleLabelUpper"
                } else {
                    "DON'T KNOW MASTER CODE? WAIT $doubleLabelUpper"
                }
            }
        }
    }

    private fun onDoubleTimeUnlockCompleted() {
        val container = AegisContainer.getInstance(context)
        val discreet = container.preferences.discreetBlockingScreenMode.value
        container.preferences.setShieldDesiredActive(true)
        AegisLockdownService.stop(context)
        dismissBlockOverlay()
        Toast.makeText(
            context,
            if (discreet) {
                "Extended wait complete. Device unlocked."
            } else {
                "Double-time wait complete. Device unlocked • Sheildlenz Shield remains ON."
            },
            Toast.LENGTH_LONG
        ).show()
        ensureShieldRemainsOnAfterUnlock()
    }

    private fun ensureShieldRemainsOnAfterUnlock() {
        val container = AegisContainer.getInstance(context)
        container.preferences.setShieldDesiredActive(true)
        val telemetry = container.protectionRepository.telemetry.value
        if (!telemetry.isMediaProjectionActive || !telemetry.isProtectionEnabled) {
            val rearmIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(MainActivity.EXTRA_REARM_SHIELD, true)
            }
            try {
                context.startActivity(rearmIntent)
            } catch (e: Exception) {
                Log.w(TAG, "Unable to launch MainActivity to re-arm Sheildlenz Shield", e)
            }
        }
    }

    private fun formatDuration(millis: Long): String {
        val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun bindPinSubmitOnEnter(pinField: EditText, submitButton: Button) {
        pinField.setOnEditorActionListener { _, actionId, event ->
            val isDoneAction = actionId == EditorInfo.IME_ACTION_DONE ||
                actionId == EditorInfo.IME_ACTION_GO ||
                actionId == EditorInfo.IME_ACTION_SEND
            val isEnterKey = event != null &&
                (event.keyCode == KeyEvent.KEYCODE_ENTER || event.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) &&
                event.action == KeyEvent.ACTION_DOWN
            if (isDoneAction || isEnterKey) {
                if (submitButton.isEnabled) {
                    submitButton.performClick()
                }
                true
            } else {
                false
            }
        }
        pinField.setOnKeyListener { _, keyCode, event ->
            if ((keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) &&
                event.action == KeyEvent.ACTION_DOWN
            ) {
                if (submitButton.isEnabled) {
                    submitButton.performClick()
                }
                true
            } else {
                false
            }
        }
    }

    private fun verticalSpacer(heightPx: Int): View {
        return View(context).apply {
            layoutParams = LinearLayout.LayoutParams(1, heightPx)
        }
    }

    private fun roundedBox(fillHex: String, strokeHex: String, radiusPx: Float): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(fillHex))
            setStroke(2, Color.parseColor(strokeHex))
            cornerRadius = radiusPx
        }
    }

    private enum class OverlayMode {
        NONE,
        LOCKDOWN,
        TAMPER,
        WARNING,
        USAGE_OR_BEDTIME,
        STUDY_SOFT_WARNING,
        STUDY_LOCK,
        APP_LOCKER
    }

    companion object {
        private const val TAG = "SystemOverlay"
    }
}
