package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import androidx.annotation.VisibleForTesting
import com.example.core.models.SensitivityLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LockdownIncidentInfo(
    val appName: String = "Detected App",
    val packageName: String = "",
    val categoryName: String = "Explicit / Inappropriate",
    val details: String = "",
    val detectedAtMillis: Long = 0L
)

enum class PowerMode(
    val displayName: String,
    val caption: String,
    val scanIntervalMs: Int,
    val captureWidthPx: Int,
    val skipDuplicates: Boolean,
    val pauseOnScreenOff: Boolean,
    val adaptiveBackoff: Boolean
) {
    ADAPTIVE_ECO(
        displayName = "Adaptive Eco",
        caption = "350ms–1600ms • Smart Battery",
        scanIntervalMs = 350,
        captureWidthPx = 256,
        skipDuplicates = true,
        pauseOnScreenOff = true,
        adaptiveBackoff = true
    ),
    POWER_SAVING(
        displayName = "Power Saving",
        caption = "750ms • Ultra Low CPU",
        scanIntervalMs = 750,
        captureWidthPx = 256,
        skipDuplicates = true,
        pauseOnScreenOff = true,
        adaptiveBackoff = true
    ),
    BALANCED(
        displayName = "Balanced",
        caption = "300ms • Daily Guard",
        scanIntervalMs = 300,
        captureWidthPx = 480,
        skipDuplicates = true,
        pauseOnScreenOff = true,
        adaptiveBackoff = false
    ),
    EXTREME(
        displayName = "Extreme",
        caption = "120ms • Max Speed",
        scanIntervalMs = 120,
        captureWidthPx = 640,
        skipDuplicates = false,
        pauseOnScreenOff = false,
        adaptiveBackoff = false
    )
}

enum class InferenceAccelerationMode(
    val displayName: String,
    val caption: String
) {
    AUTO_BEST(
        displayName = "Auto (NPU/GPU + CPU)",
        caption = "Uses hardware NNAPI when supported with XNNPACK fallback"
    ),
    CPU_XNNPACK(
        displayName = "CPU (XNNPACK)",
        caption = "Balanced SIMD CPU acceleration across all devices"
    ),
    NNAPI_NPU(
        displayName = "NPU / GPU (NNAPI)",
        caption = "Hardware AI delegate acceleration for lower CPU power"
    )
}

data class WarningEscalationResult(
    val shouldLockdown: Boolean,
    val warningNumber: Int,
    val maxWarnings: Int
)

class AegisPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("aegis_shield_prefs", Context.MODE_PRIVATE)

    private val _sensitivity = MutableStateFlow(getSavedSensitivity())
    val sensitivity: StateFlow<SensitivityLevel> = _sensitivity.asStateFlow()

    private val _strictEverything = MutableStateFlow(prefs.getBoolean(KEY_STRICT_EVERYTHING, false))
    val strictEverything: StateFlow<Boolean> = _strictEverything.asStateFlow()

    private val _autoStart = MutableStateFlow(prefs.getBoolean(KEY_AUTO_START, true))
    val autoStart: StateFlow<Boolean> = _autoStart.asStateFlow()

    private val _hasGuardianPin = MutableStateFlow(!prefs.getString(KEY_PIN_HASH, "").isNullOrEmpty())
    val hasGuardianPin: StateFlow<Boolean> = _hasGuardianPin.asStateFlow()

    private val _maintenanceMode = MutableStateFlow(prefs.getBoolean(KEY_MAINTENANCE_MODE, false))
    val maintenanceMode: StateFlow<Boolean> = _maintenanceMode.asStateFlow()

    private val _isOnboardingCompleted = MutableStateFlow(prefs.getBoolean(KEY_ONBOARDING_DONE, false))
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

    private val _importedModelName = MutableStateFlow(prefs.getString(KEY_IMPORTED_MODEL_NAME, null))
    val importedModelName: StateFlow<String?> = _importedModelName.asStateFlow()

    // 10-Hour Whole-Device Lockdown & Anti-Tamper Preferences
    private val _lockdownHours = MutableStateFlow(prefs.getInt(KEY_LOCKDOWN_HOURS, DEFAULT_LOCKDOWN_HOURS))
    val lockdownHours: StateFlow<Int> = _lockdownHours.asStateFlow()

    private val _waitCooldownBeforePassword = MutableStateFlow(
        prefs.getBoolean(KEY_WAIT_COOLDOWN_BEFORE_PIN, true)
    )
    val waitCooldownBeforePassword: StateFlow<Boolean> = _waitCooldownBeforePassword.asStateFlow()

    private val _lockdownUntilMillis = MutableStateFlow(prefs.getLong(KEY_LOCKDOWN_UNTIL_MILLIS, 0L))
    val lockdownUntilMillis: StateFlow<Long> = _lockdownUntilMillis.asStateFlow()

    private val _lockdownPendingUnlock = MutableStateFlow(prefs.getBoolean(KEY_LOCKDOWN_PENDING_UNLOCK, false))
    val lockdownPendingUnlock: StateFlow<Boolean> = _lockdownPendingUnlock.asStateFlow()

    private val _lockdownStage = MutableStateFlow(prefs.getInt(KEY_LOCKDOWN_STAGE, inferInitialStage()))
    val lockdownStage: StateFlow<Int> = _lockdownStage.asStateFlow()

    private val _isShieldDesiredActive = MutableStateFlow(prefs.getBoolean(KEY_SHIELD_DESIRED_ACTIVE, false))
    val isShieldDesiredActive: StateFlow<Boolean> = _isShieldDesiredActive.asStateFlow()

    private val _lockdownIncidentInfo = MutableStateFlow(readLockdownIncident())
    val lockdownIncidentInfo: StateFlow<LockdownIncidentInfo> = _lockdownIncidentInfo.asStateFlow()

    private val _blockedOffendingPackage = MutableStateFlow(prefs.getString(KEY_BLOCKED_PACKAGE, null))
    val blockedOffendingPackage: StateFlow<String?> = _blockedOffendingPackage.asStateFlow()

    private val _antiTamperEnabled = MutableStateFlow(prefs.getBoolean(KEY_ANTI_TAMPER_ENABLED, true))
    val antiTamperEnabled: StateFlow<Boolean> = _antiTamperEnabled.asStateFlow()

    // 7-Tap Hidden Advanced Precision Lab Preferences
    private val _advancedOptionsUnlocked = MutableStateFlow(prefs.getBoolean(KEY_ADVANCED_UNLOCKED, false))
    val advancedOptionsUnlocked: StateFlow<Boolean> = _advancedOptionsUnlocked.asStateFlow()

    private val _lockdownDurationMinutes = MutableStateFlow(
        prefs.getInt(
            KEY_LOCKDOWN_MINUTES,
            prefs.getInt(KEY_LOCKDOWN_HOURS, DEFAULT_LOCKDOWN_HOURS) * 60
        ).coerceIn(1, MAX_LOCKDOWN_MINUTES)
    )
    val lockdownDurationMinutes: StateFlow<Int> = _lockdownDurationMinutes.asStateFlow()

    private val _doubleTimeMultiplier = MutableStateFlow(prefs.getFloat(KEY_DOUBLE_TIME_MULTIPLIER, 2.0f))
    val doubleTimeMultiplier: StateFlow<Float> = _doubleTimeMultiplier.asStateFlow()

    private val _powerMode = MutableStateFlow(getSavedPowerMode())
    val powerMode: StateFlow<PowerMode> = _powerMode.asStateFlow()

    private val _scanIntervalMs = MutableStateFlow(prefs.getInt(KEY_SCAN_INTERVAL_MS, 350).coerceIn(100, 2000))
    val scanIntervalMs: StateFlow<Int> = _scanIntervalMs.asStateFlow()

    private val _captureWidthPx = MutableStateFlow(prefs.getInt(KEY_CAPTURE_WIDTH_PX, 256).coerceIn(224, 1080))
    val captureWidthPx: StateFlow<Int> = _captureWidthPx.asStateFlow()

    private val _skipDuplicateFrames = MutableStateFlow(prefs.getBoolean(KEY_SKIP_DUPLICATE_FRAMES, true))
    val skipDuplicateFrames: StateFlow<Boolean> = _skipDuplicateFrames.asStateFlow()

    private val _pauseWhenScreenOff = MutableStateFlow(prefs.getBoolean(KEY_PAUSE_WHEN_SCREEN_OFF, true))
    val pauseWhenScreenOff: StateFlow<Boolean> = _pauseWhenScreenOff.asStateFlow()

    private val _adaptiveCadenceEnabled = MutableStateFlow(prefs.getBoolean(KEY_ADAPTIVE_CADENCE, true))
    val adaptiveCadenceEnabled: StateFlow<Boolean> = _adaptiveCadenceEnabled.asStateFlow()

    private val _autoThrottleLowBattery = MutableStateFlow(prefs.getBoolean(KEY_AUTO_THROTTLE_LOW_BATTERY, true))
    val autoThrottleLowBattery: StateFlow<Boolean> = _autoThrottleLowBattery.asStateFlow()

    private val _whitelistedPackages = MutableStateFlow(
        prefs.getStringSet(KEY_WHITELISTED_PACKAGES, emptySet()) ?: emptySet()
    )
    val whitelistedPackages: StateFlow<Set<String>> = _whitelistedPackages.asStateFlow()

    fun setWhitelistedPackages(packages: Set<String>) {
        prefs.edit().putStringSet(KEY_WHITELISTED_PACKAGES, packages).apply()
        _whitelistedPackages.value = packages
    }

    fun addWhitelistedPackage(packageName: String) {
        if (packageName.isBlank()) return
        val current = _whitelistedPackages.value.toMutableSet()
        if (current.add(packageName.trim())) {
            setWhitelistedPackages(current)
        }
    }

    fun removeWhitelistedPackage(packageName: String) {
        val current = _whitelistedPackages.value.toMutableSet()
        if (current.remove(packageName)) {
            setWhitelistedPackages(current)
        }
    }

    fun isPackageWhitelisted(packageName: String): Boolean {
        if (packageName.isBlank()) return false
        return _whitelistedPackages.value.contains(packageName)
    }

    // Scheduled Active Protection Hours ("Study / Bedtime Guard")
    private val _scheduleEnabled = MutableStateFlow(prefs.getBoolean(KEY_SCHEDULE_ENABLED, false))
    val scheduleEnabled: StateFlow<Boolean> = _scheduleEnabled.asStateFlow()

    private val _scheduleStartMinutes = MutableStateFlow(
        prefs.getInt(KEY_SCHEDULE_START_MINUTES, 21 * 60).coerceIn(0, 1439)
    )
    val scheduleStartMinutes: StateFlow<Int> = _scheduleStartMinutes.asStateFlow()

    private val _scheduleEndMinutes = MutableStateFlow(
        prefs.getInt(KEY_SCHEDULE_END_MINUTES, 7 * 60).coerceIn(0, 1439)
    )
    val scheduleEndMinutes: StateFlow<Int> = _scheduleEndMinutes.asStateFlow()

    fun setScheduleEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SCHEDULE_ENABLED, enabled).apply()
        _scheduleEnabled.value = enabled
    }

    fun setScheduleWindow(startMinutes: Int, endMinutes: Int) {
        val safeStart = startMinutes.coerceIn(0, 1439)
        val safeEnd = endMinutes.coerceIn(0, 1439)
        prefs.edit()
            .putInt(KEY_SCHEDULE_START_MINUTES, safeStart)
            .putInt(KEY_SCHEDULE_END_MINUTES, safeEnd)
            .apply()
        _scheduleStartMinutes.value = safeStart
        _scheduleEndMinutes.value = safeEnd
    }

    fun isWithinScheduledWindow(
        currentMinuteOfDay: Int = currentClockMinuteOfDay()
    ): Boolean {
        if (!_scheduleEnabled.value) return false
        return isMinuteWithinWindow(
            minuteOfDay = currentMinuteOfDay,
            startMinute = _scheduleStartMinutes.value,
            endMinute = _scheduleEndMinutes.value
        )
    }

    private fun currentClockMinuteOfDay(): Int {
        val cal = java.util.Calendar.getInstance()
        return cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
    }

    private val _customThresholdsEnabled = MutableStateFlow(prefs.getBoolean(KEY_CUSTOM_THRESHOLDS_ENABLED, false))
    val customThresholdsEnabled: StateFlow<Boolean> = _customThresholdsEnabled.asStateFlow()

    private val _customExplicitThreshold = MutableStateFlow(prefs.getFloat(KEY_CUSTOM_EXPLICIT_THRESHOLD, 0.75f).coerceIn(0.35f, 0.95f))
    val customExplicitThreshold: StateFlow<Float> = _customExplicitThreshold.asStateFlow()

    private val _customSuggestiveThreshold = MutableStateFlow(prefs.getFloat(KEY_CUSTOM_SUGGESTIVE_THRESHOLD, 0.80f).coerceIn(0.35f, 0.95f))
    val customSuggestiveThreshold: StateFlow<Float> = _customSuggestiveThreshold.asStateFlow()

    private val _consecutiveFramesRequired = MutableStateFlow(prefs.getInt(KEY_CONSECUTIVE_FRAMES_REQUIRED, 2).coerceIn(1, 5))
    val consecutiveFramesRequired: StateFlow<Int> = _consecutiveFramesRequired.asStateFlow()

    private val _showEmergencyDialerOnLockdown = MutableStateFlow(prefs.getBoolean(KEY_SHOW_EMERGENCY_DIALER, true))
    val showEmergencyDialerOnLockdown: StateFlow<Boolean> = _showEmergencyDialerOnLockdown.asStateFlow()

    private val _evictAppOnThreat = MutableStateFlow(prefs.getBoolean(KEY_EVICT_APP_ON_THREAT, true))
    val evictAppOnThreat: StateFlow<Boolean> = _evictAppOnThreat.asStateFlow()

    private val _guardianSettingsWindowMinutes = MutableStateFlow(prefs.getInt(KEY_GUARDIAN_WINDOW_MINUTES, 2).coerceIn(1, 15))
    val guardianSettingsWindowMinutes: StateFlow<Int> = _guardianSettingsWindowMinutes.asStateFlow()

    // Session-scoped Guardian authorization (automatically resets when app leaves foreground)
    private val _isGuardianSessionUnlocked = MutableStateFlow(false)
    val isGuardianSessionUnlocked: StateFlow<Boolean> = _isGuardianSessionUnlocked.asStateFlow()

    // Parent-Child Cloud Sync & Guardian Control State
    private val _parentalRole = MutableStateFlow(prefs.getString(KEY_PARENTAL_ROLE, null))
    val parentalRole: StateFlow<String?> = _parentalRole.asStateFlow()

    private val _pairedParentUid = MutableStateFlow(prefs.getString(KEY_PAIRED_PARENT_UID, null))
    val pairedParentUid: StateFlow<String?> = _pairedParentUid.asStateFlow()

    private val _pairedParentName = MutableStateFlow(prefs.getString(KEY_PAIRED_PARENT_NAME, null))
    val pairedParentName: StateFlow<String?> = _pairedParentName.asStateFlow()

    private val _pairedChildUid = MutableStateFlow(prefs.getString(KEY_PAIRED_CHILD_UID, null))
    val pairedChildUid: StateFlow<String?> = _pairedChildUid.asStateFlow()

    private val _childPairingCode = MutableStateFlow(prefs.getString(KEY_CHILD_PAIRING_CODE, null))
    val childPairingCode: StateFlow<String?> = _childPairingCode.asStateFlow()

    private val _childUninstallLocked = MutableStateFlow(prefs.getBoolean(KEY_CHILD_UNINSTALL_LOCKED, true))
    val childUninstallLocked: StateFlow<Boolean> = _childUninstallLocked.asStateFlow()

    private val _parentApprovedUninstallUntilMillis = MutableStateFlow(
        prefs.getLong(KEY_PARENT_APPROVED_UNINSTALL_UNTIL, 0L)
    )
    val parentApprovedUninstallUntilMillis: StateFlow<Long> = _parentApprovedUninstallUntilMillis.asStateFlow()

    private val _liveSeeEnabled = MutableStateFlow(prefs.getBoolean(KEY_LIVE_SEE_ENABLED, true))
    val liveSeeEnabled: StateFlow<Boolean> = _liveSeeEnabled.asStateFlow()

    private val _lastHandledRemoteCommandTs = MutableStateFlow(
        prefs.getLong(KEY_LAST_REMOTE_CMD_TS, 0L)
    )
    val lastHandledRemoteCommandTs: StateFlow<Long> = _lastHandledRemoteCommandTs.asStateFlow()

    // Configurable Warning Thresholds (0 = immediate lockdown; 1..5 = warnings before device lock)
    private val _warningThresholdCount = MutableStateFlow(
        prefs.getInt(KEY_WARNING_THRESHOLD_COUNT, 0).coerceIn(0, 5)
    )
    val warningThresholdCount: StateFlow<Int> = _warningThresholdCount.asStateFlow()

    private val _warningsIssuedInCycle = MutableStateFlow(
        prefs.getInt(KEY_WARNINGS_IN_CYCLE, 0).coerceAtLeast(0)
    )
    val warningsIssuedInCycle: StateFlow<Int> = _warningsIssuedInCycle.asStateFlow()

    // Guardian Email & Emergency Recovery
    private val _guardianEmail = MutableStateFlow(
        prefs.getString(KEY_GUARDIAN_EMAIL, "") ?: ""
    )
    val guardianEmail: StateFlow<String> = _guardianEmail.asStateFlow()

    // AI Hardware Acceleration & Content Category Toggles
    private val _inferenceAccelerationMode = MutableStateFlow(getSavedAccelerationMode())
    val inferenceAccelerationMode: StateFlow<InferenceAccelerationMode> = _inferenceAccelerationMode.asStateFlow()

    private val _blockExplicitAdult = MutableStateFlow(prefs.getBoolean(KEY_BLOCK_EXPLICIT_ADULT, true))
    val blockExplicitAdult: StateFlow<Boolean> = _blockExplicitAdult.asStateFlow()

    private val _blockSuggestiveContent = MutableStateFlow(prefs.getBoolean(KEY_BLOCK_SUGGESTIVE_CONTENT, true))
    val blockSuggestiveContent: StateFlow<Boolean> = _blockSuggestiveContent.asStateFlow()

    private val _blockSensitiveGraphic = MutableStateFlow(prefs.getBoolean(KEY_BLOCK_SENSITIVE_GRAPHIC, true))
    val blockSensitiveGraphic: StateFlow<Boolean> = _blockSensitiveGraphic.asStateFlow()

    private val _blockGamblingKeywords = MutableStateFlow(prefs.getBoolean(KEY_BLOCK_GAMBLING_KEYWORDS, true))
    val blockGamblingKeywords: StateFlow<Boolean> = _blockGamblingKeywords.asStateFlow()

    // Bedtime Lock & Daily Per-App Time Budgets
    private val _bedtimeEnabled = MutableStateFlow(prefs.getBoolean(KEY_BEDTIME_ENABLED, false))
    val bedtimeEnabled: StateFlow<Boolean> = _bedtimeEnabled.asStateFlow()

    private val _bedtimeStartMinutes = MutableStateFlow(
        prefs.getInt(KEY_BEDTIME_START_MINUTES, 22 * 60).coerceIn(0, 1439)
    )
    val bedtimeStartMinutes: StateFlow<Int> = _bedtimeStartMinutes.asStateFlow()

    private val _bedtimeEndMinutes = MutableStateFlow(
        prefs.getInt(KEY_BEDTIME_END_MINUTES, 6 * 60).coerceIn(0, 1439)
    )
    val bedtimeEndMinutes: StateFlow<Int> = _bedtimeEndMinutes.asStateFlow()

    private val _appTimeBudgets = MutableStateFlow(readIntMap(KEY_APP_TIME_BUDGETS))
    val appTimeBudgets: StateFlow<Map<String, Int>> = _appTimeBudgets.asStateFlow()

    private val _dailyAppUsageSeconds = MutableStateFlow(readDailyUsageMap(todayDateKey()))
    val dailyAppUsageSeconds: StateFlow<Map<String, Int>> = _dailyAppUsageSeconds.asStateFlow()

    // System-Wide Safe DNS & Custom Blocklists
    private val _safeDnsEnabled = MutableStateFlow(prefs.getBoolean(KEY_SAFE_DNS_ENABLED, false))
    val safeDnsEnabled: StateFlow<Boolean> = _safeDnsEnabled.asStateFlow()

    private val _customBlockedDomains = MutableStateFlow(
        prefs.getStringSet(KEY_CUSTOM_BLOCKED_DOMAINS, emptySet())?.toSet() ?: emptySet()
    )
    val customBlockedDomains: StateFlow<Set<String>> = _customBlockedDomains.asStateFlow()

    private val _customBlockedKeywords = MutableStateFlow(
        prefs.getStringSet(KEY_CUSTOM_BLOCKED_KEYWORDS, emptySet())?.toSet() ?: emptySet()
    )
    val customBlockedKeywords: StateFlow<Set<String>> = _customBlockedKeywords.asStateFlow()

    // Multi-Language Localization Preference ("system", "en", "ur", "ar", "es", "hi", "fr")
    private val _appLanguageCode = MutableStateFlow(
        prefs.getString(KEY_APP_LANGUAGE_CODE, "system") ?: "system"
    )
    val appLanguageCode: StateFlow<String> = _appLanguageCode.asStateFlow()

    @Volatile
    private var guardianSettingsBypassUntilRealtime: Long = 0L

    @Volatile
    private var explicitSettingsTripPending: Boolean = false

    @Volatile
    private var authorizedExternalFlowUntilRealtime: Long = 0L

    private fun getSavedSensitivity(): SensitivityLevel {
        val name = prefs.getString(KEY_SENSITIVITY, SensitivityLevel.BALANCED.name)
        return try {
            SensitivityLevel.valueOf(name ?: SensitivityLevel.BALANCED.name)
        } catch (_: Exception) {
            SensitivityLevel.BALANCED
        }
    }

    private fun getSavedPowerMode(): PowerMode {
        val name = prefs.getString(KEY_POWER_MODE, PowerMode.ADAPTIVE_ECO.name)
        return try {
            PowerMode.valueOf(name ?: PowerMode.ADAPTIVE_ECO.name)
        } catch (_: Exception) {
            PowerMode.ADAPTIVE_ECO
        }
    }

    private fun getSavedAccelerationMode(): InferenceAccelerationMode {
        val name = prefs.getString(KEY_INFERENCE_ACCELERATION_MODE, InferenceAccelerationMode.AUTO_BEST.name)
        return try {
            InferenceAccelerationMode.valueOf(name ?: InferenceAccelerationMode.AUTO_BEST.name)
        } catch (_: Exception) {
            InferenceAccelerationMode.AUTO_BEST
        }
    }

    private fun inferInitialStage(): Int {
        val pending = prefs.getBoolean(KEY_LOCKDOWN_PENDING_UNLOCK, false)
        val until = prefs.getLong(KEY_LOCKDOWN_UNTIL_MILLIS, 0L)
        return if (pending || until > System.currentTimeMillis()) {
            LOCKDOWN_STAGE_INITIAL
        } else {
            LOCKDOWN_STAGE_NONE
        }
    }

    private fun readLockdownIncident(): LockdownIncidentInfo {
        return LockdownIncidentInfo(
            appName = prefs.getString(KEY_LOCKDOWN_APP_NAME, "Detected App") ?: "Detected App",
            packageName = prefs.getString(KEY_LOCKDOWN_PACKAGE, "") ?: "",
            categoryName = prefs.getString(KEY_LOCKDOWN_CATEGORY, "Explicit / Inappropriate") ?: "Explicit / Inappropriate",
            details = prefs.getString(KEY_LOCKDOWN_DETAILS, "") ?: "",
            detectedAtMillis = prefs.getLong(KEY_LOCKDOWN_DETECTED_AT, 0L)
        )
    }

    fun setSensitivity(level: SensitivityLevel) {
        prefs.edit().putString(KEY_SENSITIVITY, level.name).apply()
        _sensitivity.value = level
    }

    fun setStrictEverything(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_STRICT_EVERYTHING, enabled).apply()
        _strictEverything.value = enabled
    }

    fun setAutoStart(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_START, enabled).apply()
        _autoStart.value = enabled
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, completed).apply()
        _isOnboardingCompleted.value = completed
    }

    fun setMaintenanceMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MAINTENANCE_MODE, enabled).apply()
        _maintenanceMode.value = enabled
    }

    fun setImportedModelName(name: String?) {
        if (name == null) {
            prefs.edit().remove(KEY_IMPORTED_MODEL_NAME).apply()
        } else {
            prefs.edit().putString(KEY_IMPORTED_MODEL_NAME, name).apply()
        }
        _importedModelName.value = name
    }

    fun setLockdownHours(hours: Int) {
        val clampedHours = hours.coerceIn(1, 24)
        val totalMinutes = clampedHours * 60
        prefs.edit()
            .putInt(KEY_LOCKDOWN_HOURS, clampedHours)
            .putInt(KEY_LOCKDOWN_MINUTES, totalMinutes)
            .apply()
        _lockdownHours.value = clampedHours
        _lockdownDurationMinutes.value = totalMinutes
    }

    fun setLockdownDurationMinutes(totalMinutes: Int) {
        val clamped = totalMinutes.coerceIn(1, MAX_LOCKDOWN_MINUTES)
        val wholeHours = (clamped / 60).coerceAtLeast(1)
        prefs.edit()
            .putInt(KEY_LOCKDOWN_MINUTES, clamped)
            .putInt(KEY_LOCKDOWN_HOURS, wholeHours)
            .apply()
        _lockdownDurationMinutes.value = clamped
        _lockdownHours.value = wholeHours
    }

    fun setAdvancedOptionsUnlocked(unlocked: Boolean) {
        prefs.edit().putBoolean(KEY_ADVANCED_UNLOCKED, unlocked).apply()
        _advancedOptionsUnlocked.value = unlocked
    }

    fun setDoubleTimeMultiplier(multiplier: Float) {
        val clamped = multiplier.coerceIn(1.0f, 5.0f)
        prefs.edit().putFloat(KEY_DOUBLE_TIME_MULTIPLIER, clamped).apply()
        _doubleTimeMultiplier.value = clamped
    }

    fun setPowerMode(mode: PowerMode) {
        prefs.edit()
            .putString(KEY_POWER_MODE, mode.name)
            .putInt(KEY_SCAN_INTERVAL_MS, mode.scanIntervalMs)
            .putInt(KEY_CAPTURE_WIDTH_PX, mode.captureWidthPx)
            .putBoolean(KEY_SKIP_DUPLICATE_FRAMES, mode.skipDuplicates)
            .putBoolean(KEY_PAUSE_WHEN_SCREEN_OFF, mode.pauseOnScreenOff)
            .putBoolean(KEY_ADAPTIVE_CADENCE, mode.adaptiveBackoff)
            .apply()
        _powerMode.value = mode
        _scanIntervalMs.value = mode.scanIntervalMs
        _captureWidthPx.value = mode.captureWidthPx
        _skipDuplicateFrames.value = mode.skipDuplicates
        _pauseWhenScreenOff.value = mode.pauseOnScreenOff
        _adaptiveCadenceEnabled.value = mode.adaptiveBackoff
    }

    fun setScanIntervalMs(intervalMs: Int) {
        val clamped = intervalMs.coerceIn(100, 2000)
        prefs.edit().putInt(KEY_SCAN_INTERVAL_MS, clamped).apply()
        _scanIntervalMs.value = clamped
    }

    fun setCaptureWidthPx(widthPx: Int) {
        val clamped = widthPx.coerceIn(224, 1080)
        prefs.edit().putInt(KEY_CAPTURE_WIDTH_PX, clamped).apply()
        _captureWidthPx.value = clamped
    }

    fun setSkipDuplicateFrames(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SKIP_DUPLICATE_FRAMES, enabled).apply()
        _skipDuplicateFrames.value = enabled
    }

    fun setPauseWhenScreenOff(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PAUSE_WHEN_SCREEN_OFF, enabled).apply()
        _pauseWhenScreenOff.value = enabled
    }

    fun setCompactPowerResolution(enabled: Boolean) {
        val targetWidth = if (enabled) 256 else PowerMode.BALANCED.captureWidthPx
        setCaptureWidthPx(targetWidth)
    }

    fun setAdaptiveCadenceEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ADAPTIVE_CADENCE, enabled).apply()
        _adaptiveCadenceEnabled.value = enabled
    }

    fun setAutoThrottleLowBattery(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_THROTTLE_LOW_BATTERY, enabled).apply()
        _autoThrottleLowBattery.value = enabled
    }

    fun setCustomThresholdsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CUSTOM_THRESHOLDS_ENABLED, enabled).apply()
        _customThresholdsEnabled.value = enabled
    }

    fun setCustomExplicitThreshold(threshold: Float) {
        val clamped = threshold.coerceIn(0.35f, 0.95f)
        prefs.edit().putFloat(KEY_CUSTOM_EXPLICIT_THRESHOLD, clamped).apply()
        _customExplicitThreshold.value = clamped
    }

    fun setCustomSuggestiveThreshold(threshold: Float) {
        val clamped = threshold.coerceIn(0.35f, 0.95f)
        prefs.edit().putFloat(KEY_CUSTOM_SUGGESTIVE_THRESHOLD, clamped).apply()
        _customSuggestiveThreshold.value = clamped
    }

    fun setConsecutiveFramesRequired(count: Int) {
        val clamped = count.coerceIn(1, 5)
        prefs.edit().putInt(KEY_CONSECUTIVE_FRAMES_REQUIRED, clamped).apply()
        _consecutiveFramesRequired.value = clamped
    }

    fun setShowEmergencyDialerOnLockdown(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_EMERGENCY_DIALER, enabled).apply()
        _showEmergencyDialerOnLockdown.value = enabled
    }

    fun setEvictAppOnThreat(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_EVICT_APP_ON_THREAT, enabled).apply()
        _evictAppOnThreat.value = enabled
    }

    fun setGuardianSettingsWindowMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(1, 15)
        prefs.edit().putInt(KEY_GUARDIAN_WINDOW_MINUTES, clamped).apply()
        _guardianSettingsWindowMinutes.value = clamped
    }

    fun resetAdvancedCustomizations() {
        setLockdownHours(DEFAULT_LOCKDOWN_HOURS)
        setDoubleTimeMultiplier(2.0f)
        setPowerMode(PowerMode.ADAPTIVE_ECO)
        setAutoThrottleLowBattery(true)
        setCustomThresholdsEnabled(false)
        setCustomExplicitThreshold(0.75f)
        setCustomSuggestiveThreshold(0.80f)
        setConsecutiveFramesRequired(2)
        setShowEmergencyDialerOnLockdown(true)
        setEvictAppOnThreat(true)
        setGuardianSettingsWindowMinutes(2)
    }

    fun setWaitCooldownBeforePassword(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WAIT_COOLDOWN_BEFORE_PIN, enabled).apply()
        _waitCooldownBeforePassword.value = enabled
    }

    fun setAntiTamperEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ANTI_TAMPER_ENABLED, enabled).apply()
        _antiTamperEnabled.value = enabled
    }

    fun setShieldDesiredActive(active: Boolean) {
        prefs.edit().putBoolean(KEY_SHIELD_DESIRED_ACTIVE, active).apply()
        _isShieldDesiredActive.value = active
    }

    fun setParentalRole(role: String?) {
        if (role.isNullOrBlank()) {
            prefs.edit().remove(KEY_PARENTAL_ROLE).apply()
            _parentalRole.value = null
        } else {
            prefs.edit().putString(KEY_PARENTAL_ROLE, role).apply()
            _parentalRole.value = role
        }
    }

    fun setPairedParentInfo(parentUid: String?, parentName: String?) {
        prefs.edit().apply {
            if (parentUid.isNullOrBlank()) remove(KEY_PAIRED_PARENT_UID) else putString(KEY_PAIRED_PARENT_UID, parentUid)
            if (parentName.isNullOrBlank()) remove(KEY_PAIRED_PARENT_NAME) else putString(KEY_PAIRED_PARENT_NAME, parentName)
        }.apply()
        _pairedParentUid.value = parentUid?.takeIf { it.isNotBlank() }
        _pairedParentName.value = parentName?.takeIf { it.isNotBlank() }
    }

    fun setPairedChildUid(childUid: String?) {
        if (childUid.isNullOrBlank()) {
            prefs.edit().remove(KEY_PAIRED_CHILD_UID).apply()
            _pairedChildUid.value = null
        } else {
            prefs.edit().putString(KEY_PAIRED_CHILD_UID, childUid).apply()
            _pairedChildUid.value = childUid
        }
    }

    fun setChildPairingCode(code: String?) {
        if (code.isNullOrBlank()) {
            prefs.edit().remove(KEY_CHILD_PAIRING_CODE).apply()
            _childPairingCode.value = null
        } else {
            prefs.edit().putString(KEY_CHILD_PAIRING_CODE, code).apply()
            _childPairingCode.value = code
        }
    }

    fun setChildUninstallLocked(locked: Boolean) {
        prefs.edit().putBoolean(KEY_CHILD_UNINSTALL_LOCKED, locked).apply()
        _childUninstallLocked.value = locked
    }

    fun setParentApprovedUninstallUntil(untilMillis: Long) {
        prefs.edit().putLong(KEY_PARENT_APPROVED_UNINSTALL_UNTIL, untilMillis).apply()
        _parentApprovedUninstallUntilMillis.value = untilMillis
    }

    fun isParentApprovedUninstallActive(): Boolean =
        _parentApprovedUninstallUntilMillis.value > System.currentTimeMillis()

    fun isChildUninstallBlocked(): Boolean =
        _parentalRole.value == "child" && _childUninstallLocked.value && !isParentApprovedUninstallActive()

    fun setLiveSeeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LIVE_SEE_ENABLED, enabled).apply()
        _liveSeeEnabled.value = enabled
    }

    fun setLastHandledRemoteCommandTs(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_REMOTE_CMD_TS, timestamp).apply()
        _lastHandledRemoteCommandTs.value = timestamp
    }

    fun clearParentalConnectionState() {
        prefs.edit()
            .remove(KEY_PARENTAL_ROLE)
            .remove(KEY_PAIRED_PARENT_UID)
            .remove(KEY_PAIRED_PARENT_NAME)
            .remove(KEY_PAIRED_CHILD_UID)
            .remove(KEY_CHILD_PAIRING_CODE)
            .putLong(KEY_PARENT_APPROVED_UNINSTALL_UNTIL, 0L)
            .apply()
        _parentalRole.value = null
        _pairedParentUid.value = null
        _pairedParentName.value = null
        _pairedChildUid.value = null
        _childPairingCode.value = null
        _parentApprovedUninstallUntilMillis.value = 0L
    }

    fun getDoubleTimeDurationMinutes(): Int =
        (_lockdownDurationMinutes.value * _doubleTimeMultiplier.value).toInt().coerceAtLeast(1)

    fun getDoubleTimeHours(): Int = (getDoubleTimeDurationMinutes() / 60).coerceAtLeast(1)

    fun isInDoubleTimeStage(): Boolean = _lockdownStage.value == LOCKDOWN_STAGE_DOUBLE_TIME

    fun engageFullDeviceLockdown(
        appName: String,
        packageName: String,
        categoryName: String,
        details: String
    ): Long {
        val now = System.currentTimeMillis()
        val durationMs = _lockdownDurationMinutes.value * 60L * 1000L
        val until = now + durationMs

        prefs.edit()
            .putLong(KEY_LOCKDOWN_UNTIL_MILLIS, until)
            .putBoolean(KEY_LOCKDOWN_PENDING_UNLOCK, true)
            .putInt(KEY_LOCKDOWN_STAGE, LOCKDOWN_STAGE_INITIAL)
            .putBoolean(KEY_SHIELD_DESIRED_ACTIVE, true)
            .putString(KEY_LOCKDOWN_APP_NAME, appName)
            .putString(KEY_LOCKDOWN_PACKAGE, packageName)
            .putString(KEY_LOCKDOWN_CATEGORY, categoryName)
            .putString(KEY_LOCKDOWN_DETAILS, details)
            .putLong(KEY_LOCKDOWN_DETECTED_AT, now)
            .putString(KEY_BLOCKED_PACKAGE, packageName)
            .putInt(KEY_WARNINGS_IN_CYCLE, 0)
            .apply()

        _lockdownUntilMillis.value = until
        _lockdownPendingUnlock.value = true
        _lockdownStage.value = LOCKDOWN_STAGE_INITIAL
        _isShieldDesiredActive.value = true
        _lockdownIncidentInfo.value = LockdownIncidentInfo(appName, packageName, categoryName, details, now)
        _blockedOffendingPackage.value = packageName
        _warningsIssuedInCycle.value = 0
        return until
    }

    /**
     * Stage 2 Double-Time Fallback:
     * If the user does not know the Master Code after Stage 1 finishes, they can wait
     * (lockdownDurationMinutes * doubleTimeMultiplier). When that double-time countdown finishes,
     * the device automatically unlocks while keeping the Sheildlenz Shield ON.
     */
    fun engageDoubleTimeLockdown(): Long {
        val now = System.currentTimeMillis()
        val durationMs = getDoubleTimeDurationMinutes() * 60L * 1000L
        val until = now + durationMs

        prefs.edit()
            .putLong(KEY_LOCKDOWN_UNTIL_MILLIS, until)
            .putBoolean(KEY_LOCKDOWN_PENDING_UNLOCK, true)
            .putInt(KEY_LOCKDOWN_STAGE, LOCKDOWN_STAGE_DOUBLE_TIME)
            .putBoolean(KEY_SHIELD_DESIRED_ACTIVE, true)
            .apply()

        _lockdownUntilMillis.value = until
        _lockdownPendingUnlock.value = true
        _lockdownStage.value = LOCKDOWN_STAGE_DOUBLE_TIME
        _isShieldDesiredActive.value = true
        return until
    }

    fun getRemainingLockdownMillis(): Long {
        val until = _lockdownUntilMillis.value
        if (until <= 0L) return 0L
        return (until - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    fun completeDoubleTimeIfExpired(): Boolean {
        if (_lockdownStage.value == LOCKDOWN_STAGE_DOUBLE_TIME && getRemainingLockdownMillis() <= 0L) {
            clearLockdown()
            return true
        }
        return false
    }

    fun isLockdownActive(): Boolean {
        if (_lockdownStage.value == LOCKDOWN_STAGE_DOUBLE_TIME) {
            return if (getRemainingLockdownMillis() > 0L) {
                true
            } else {
                clearLockdown()
                false
            }
        }
        return _lockdownPendingUnlock.value || getRemainingLockdownMillis() > 0L
    }

    fun canAcceptPasswordForLockdown(): Boolean {
        if (_lockdownStage.value == LOCKDOWN_STAGE_DOUBLE_TIME) return true
        if (!_waitCooldownBeforePassword.value) return true
        return getRemainingLockdownMillis() <= 0L
    }

    fun clearLockdown() {
        prefs.edit()
            .putLong(KEY_LOCKDOWN_UNTIL_MILLIS, 0L)
            .putBoolean(KEY_LOCKDOWN_PENDING_UNLOCK, false)
            .putInt(KEY_LOCKDOWN_STAGE, LOCKDOWN_STAGE_NONE)
            .putInt(KEY_WARNINGS_IN_CYCLE, 0)
            .remove(KEY_BLOCKED_PACKAGE)
            .apply()
        _lockdownUntilMillis.value = 0L
        _lockdownPendingUnlock.value = false
        _lockdownStage.value = LOCKDOWN_STAGE_NONE
        _blockedOffendingPackage.value = null
        _warningsIssuedInCycle.value = 0
    }

    @VisibleForTesting
    internal fun setLockdownUntilForTesting(untilMillis: Long) {
        prefs.edit().putLong(KEY_LOCKDOWN_UNTIL_MILLIS, untilMillis).apply()
        _lockdownUntilMillis.value = untilMillis
    }

    fun setBlockedOffendingPackage(packageName: String?) {
        if (packageName.isNullOrBlank()) {
            prefs.edit().remove(KEY_BLOCKED_PACKAGE).apply()
        } else {
            prefs.edit().putString(KEY_BLOCKED_PACKAGE, packageName).apply()
        }
        _blockedOffendingPackage.value = packageName
    }

    fun unlockGuardianSession(grantSettingsWindowMinutes: Int = _guardianSettingsWindowMinutes.value) {
        _isGuardianSessionUnlocked.value = true
        guardianSettingsBypassUntilRealtime =
            SystemClock.elapsedRealtime() + (grantSettingsWindowMinutes * 60_000L)
    }

    fun lockGuardianSession() {
        _isGuardianSessionUnlocked.value = false
        guardianSettingsBypassUntilRealtime = 0L
        explicitSettingsTripPending = false
        authorizedExternalFlowUntilRealtime = 0L
    }

    fun beginAuthorizedExternalFlow(windowMillis: Long = 180_000L) {
        authorizedExternalFlowUntilRealtime = SystemClock.elapsedRealtime() + windowMillis
    }

    fun onAppBackgrounded() {
        val now = SystemClock.elapsedRealtime()
        val withinAuthorizedExternalFlow = now <= authorizedExternalFlowUntilRealtime
        authorizedExternalFlowUntilRealtime = 0L
        if (withinAuthorizedExternalFlow) {
            return
        }
        _isGuardianSessionUnlocked.value = false
        if (explicitSettingsTripPending) {
            explicitSettingsTripPending = false
        } else {
            guardianSettingsBypassUntilRealtime = 0L
        }
    }

    fun grantTemporarySettingsAuthorization(minutes: Int = _guardianSettingsWindowMinutes.value) {
        explicitSettingsTripPending = true
        beginAuthorizedExternalFlow(minutes * 60_000L)
        guardianSettingsBypassUntilRealtime =
            SystemClock.elapsedRealtime() + (minutes * 60_000L)
    }

    fun isGuardianAuthorizedForSystemSettings(): Boolean {
        if (!_hasGuardianPin.value) return true
        if (_isGuardianSessionUnlocked.value) return true
        return SystemClock.elapsedRealtime() < guardianSettingsBypassUntilRealtime
    }

    fun verifyGuardianPin(pin: String): Boolean {
        val storedHash = prefs.getString(KEY_PIN_HASH, null)
        if (storedHash.isNullOrEmpty() || pin.isEmpty()) return false
        val inputHash = hashPin(pin)
        return storedHash == inputHash
    }

    fun setGuardianPin(pin: String) {
        val hash = hashPin(pin)
        prefs.edit().putString(KEY_PIN_HASH, hash).apply()
        _hasGuardianPin.value = true
        unlockGuardianSession()
    }

    fun removeGuardianPin() {
        prefs.edit().remove(KEY_PIN_HASH).apply()
        _hasGuardianPin.value = false
        _isGuardianSessionUnlocked.value = true
    }

    private fun hashPin(pin: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(("aegis_salt_" + pin).toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    // --- 1. Configurable Warning Thresholds ---

    fun setWarningThresholdCount(count: Int) {
        val clamped = count.coerceIn(0, 5)
        prefs.edit().putInt(KEY_WARNING_THRESHOLD_COUNT, clamped).apply()
        _warningThresholdCount.value = clamped
    }

    fun recordThreatAndCheckLockdown(): WarningEscalationResult {
        val maxWarnings = _warningThresholdCount.value.coerceIn(0, 5)
        if (maxWarnings == 0) {
            return WarningEscalationResult(shouldLockdown = true, warningNumber = 0, maxWarnings = 0)
        }
        val nextCount = _warningsIssuedInCycle.value + 1
        return if (nextCount > maxWarnings) {
            resetWarningCycle()
            WarningEscalationResult(shouldLockdown = true, warningNumber = nextCount, maxWarnings = maxWarnings)
        } else {
            prefs.edit().putInt(KEY_WARNINGS_IN_CYCLE, nextCount).apply()
            _warningsIssuedInCycle.value = nextCount
            WarningEscalationResult(shouldLockdown = false, warningNumber = nextCount, maxWarnings = maxWarnings)
        }
    }

    fun resetWarningCycle() {
        prefs.edit().putInt(KEY_WARNINGS_IN_CYCLE, 0).apply()
        _warningsIssuedInCycle.value = 0
    }

    // --- 2. Guardian Email & Emergency Dialer Recovery ---

    fun setGuardianEmail(email: String) {
        val clean = email.trim().take(160)
        prefs.edit().putString(KEY_GUARDIAN_EMAIL, clean).apply()
        _guardianEmail.value = clean
    }

    fun getRecoveryChallengeCode(): String {
        val existing = prefs.getString(KEY_RECOVERY_CHALLENGE_ID, null)
        if (!existing.isNullOrBlank()) return existing
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val suffix = (1..6).map { chars.random() }.joinToString("")
        val generated = "SLZ-$suffix"
        prefs.edit().putString(KEY_RECOVERY_CHALLENGE_ID, generated).apply()
        return generated
    }

    fun verifyRecoveryToken(input: String): Boolean {
        val normalized = input.trim().uppercase(Locale.US).replace("-", "").replace(" ", "")
        if (normalized.length < 6) return false
        val expected = computeRecoveryTokenForChallenge(getRecoveryChallengeCode())
            .uppercase(Locale.US)
            .replace("-", "")
        return normalized == expected
    }

    fun resetPinWithRecoveryToken(token: String, newPin: String? = null): Boolean {
        if (!verifyRecoveryToken(token)) return false
        clearLockdown()
        if (!newPin.isNullOrBlank() && newPin.length in 4..8 && newPin.all { it.isDigit() }) {
            setGuardianPin(newPin)
        } else {
            unlockGuardianSession()
        }
        return true
    }

    // --- 3. AI Hardware Acceleration & Content Category Toggles ---

    fun setInferenceAccelerationMode(mode: InferenceAccelerationMode) {
        prefs.edit().putString(KEY_INFERENCE_ACCELERATION_MODE, mode.name).apply()
        _inferenceAccelerationMode.value = mode
    }

    fun setBlockExplicitAdult(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BLOCK_EXPLICIT_ADULT, enabled).apply()
        _blockExplicitAdult.value = enabled
    }

    fun setBlockSuggestiveContent(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BLOCK_SUGGESTIVE_CONTENT, enabled).apply()
        _blockSuggestiveContent.value = enabled
    }

    fun setBlockSensitiveGraphic(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BLOCK_SENSITIVE_GRAPHIC, enabled).apply()
        _blockSensitiveGraphic.value = enabled
    }

    fun setBlockGamblingKeywords(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BLOCK_GAMBLING_KEYWORDS, enabled).apply()
        _blockGamblingKeywords.value = enabled
    }

    // --- 4. Bedtime Lock & Daily Per-App Time Budgets ---

    fun setBedtimeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BEDTIME_ENABLED, enabled).apply()
        _bedtimeEnabled.value = enabled
    }

    fun setBedtimeWindow(startMinutes: Int, endMinutes: Int) {
        val safeStart = startMinutes.coerceIn(0, 1439)
        val safeEnd = endMinutes.coerceIn(0, 1439)
        prefs.edit()
            .putInt(KEY_BEDTIME_START_MINUTES, safeStart)
            .putInt(KEY_BEDTIME_END_MINUTES, safeEnd)
            .apply()
        _bedtimeStartMinutes.value = safeStart
        _bedtimeEndMinutes.value = safeEnd
    }

    fun isWithinBedtimeLockWindow(currentMinuteOfDay: Int = currentClockMinuteOfDay()): Boolean {
        if (!_bedtimeEnabled.value) return false
        return isMinuteWithinWindow(
            minuteOfDay = currentMinuteOfDay,
            startMinute = _bedtimeStartMinutes.value,
            endMinute = _bedtimeEndMinutes.value
        )
    }

    fun setAppTimeBudgets(budgets: Map<String, Int>) {
        val sanitized = budgets.entries
            .filter { it.key.isNotBlank() && it.value > 0 }
            .take(50)
            .associate { it.key.trim() to it.value.coerceIn(1, 720) }
        writeIntMap(KEY_APP_TIME_BUDGETS, sanitized)
        _appTimeBudgets.value = sanitized
    }

    fun setAppTimeBudget(packageName: String, limitMinutes: Int) {
        val cleanPkg = packageName.trim()
        if (cleanPkg.isBlank()) return
        val next = _appTimeBudgets.value.toMutableMap()
        if (limitMinutes <= 0) {
            next.remove(cleanPkg)
        } else {
            next[cleanPkg] = limitMinutes.coerceIn(1, 720)
        }
        setAppTimeBudgets(next)
    }

    fun removeAppTimeBudget(packageName: String) {
        val next = _appTimeBudgets.value.toMutableMap()
        if (next.remove(packageName) != null) {
            setAppTimeBudgets(next)
        }
    }

    fun recordAppUsageSeconds(
        packageName: String,
        deltaSeconds: Int = 1,
        dateKey: String = todayDateKey()
    ): Int {
        if (packageName.isBlank() || deltaSeconds <= 0) return 0
        val currentMap = readDailyUsageMap(dateKey).toMutableMap()
        val updated = (currentMap[packageName] ?: 0) + deltaSeconds
        currentMap[packageName] = updated
        prefs.edit()
            .putString(KEY_DAILY_USAGE_DATE, dateKey)
            .putString(KEY_DAILY_USAGE_MAP, JSONObject(currentMap.mapValues { it.value }).toString())
            .apply()
        _dailyAppUsageSeconds.value = currentMap
        return updated
    }

    fun getAppUsedSecondsToday(packageName: String, dateKey: String = todayDateKey()): Int {
        if (packageName.isBlank()) return 0
        val currentMap = readDailyUsageMap(dateKey)
        if (_dailyAppUsageSeconds.value != currentMap) {
            _dailyAppUsageSeconds.value = currentMap
        }
        return currentMap[packageName] ?: 0
    }

    fun isAppTimeBudgetExceeded(packageName: String, dateKey: String = todayDateKey()): Boolean {
        val limitMinutes = _appTimeBudgets.value[packageName] ?: return false
        val extMap = readDailyExtensionMap(dateKey)
        val extraMinutes = extMap[packageName] ?: 0
        val allowedSeconds = (limitMinutes + extraMinutes) * 60
        return getAppUsedSecondsToday(packageName, dateKey) >= allowedSeconds
    }

    fun getRemainingBudgetSeconds(packageName: String, dateKey: String = todayDateKey()): Int? {
        val limitMinutes = _appTimeBudgets.value[packageName] ?: return null
        val extMap = readDailyExtensionMap(dateKey)
        val extraMinutes = extMap[packageName] ?: 0
        val allowedSeconds = (limitMinutes + extraMinutes) * 60
        return (allowedSeconds - getAppUsedSecondsToday(packageName, dateKey)).coerceAtLeast(0)
    }

    fun grantTemporaryBudgetExtension(
        packageName: String,
        extraMinutes: Int = 15,
        dateKey: String = todayDateKey()
    ) {
        if (packageName.isBlank() || extraMinutes <= 0) return
        val extMap = readDailyExtensionMap(dateKey).toMutableMap()
        extMap[packageName] = (extMap[packageName] ?: 0) + extraMinutes
        prefs.edit()
            .putString(KEY_DAILY_EXT_DATE, dateKey)
            .putString(KEY_DAILY_EXT_MAP, JSONObject(extMap.mapValues { it.value }).toString())
            .apply()
    }

    private fun readIntMap(key: String): Map<String, Int> {
        val raw = prefs.getString(key, null) ?: return emptyMap()
        return runCatching {
            val json = JSONObject(raw)
            buildMap {
                val iter = json.keys()
                while (iter.hasNext()) {
                    val k = iter.next()
                    put(k, json.optInt(k, 0))
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun writeIntMap(key: String, map: Map<String, Int>) {
        prefs.edit().putString(key, JSONObject(map.mapValues { it.value }).toString()).apply()
    }

    private fun readDailyUsageMap(dateKey: String): Map<String, Int> {
        val storedDate = prefs.getString(KEY_DAILY_USAGE_DATE, "")
        if (storedDate != dateKey) {
            return emptyMap()
        }
        return readIntMap(KEY_DAILY_USAGE_MAP)
    }

    private fun readDailyExtensionMap(dateKey: String): Map<String, Int> {
        val storedDate = prefs.getString(KEY_DAILY_EXT_DATE, "")
        if (storedDate != dateKey) {
            return emptyMap()
        }
        return readIntMap(KEY_DAILY_EXT_MAP)
    }

    private fun todayDateKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    // --- 5. System-Wide Safe DNS & Custom Blocklists ---

    fun setSafeDnsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SAFE_DNS_ENABLED, enabled).apply()
        _safeDnsEnabled.value = enabled
    }

    fun setCustomBlockedDomains(domains: Set<String>) {
        val cleaned = domains.mapNotNull { normalizeDomainInput(it) }.take(200).toSet()
        prefs.edit().putStringSet(KEY_CUSTOM_BLOCKED_DOMAINS, cleaned).apply()
        _customBlockedDomains.value = cleaned
    }

    fun addCustomBlockedDomain(raw: String): Boolean {
        val domain = normalizeDomainInput(raw) ?: return false
        val next = _customBlockedDomains.value.toMutableSet()
        if (next.add(domain)) {
            setCustomBlockedDomains(next)
            return true
        }
        return false
    }

    fun removeCustomBlockedDomain(domain: String) {
        val next = _customBlockedDomains.value.toMutableSet()
        if (next.remove(domain)) {
            setCustomBlockedDomains(next)
        }
    }

    fun setCustomBlockedKeywords(keywords: Set<String>) {
        val cleaned = keywords.map { it.trim().lowercase(Locale.US) }
            .filter { it.length >= 2 }
            .take(200)
            .toSet()
        prefs.edit().putStringSet(KEY_CUSTOM_BLOCKED_KEYWORDS, cleaned).apply()
        _customBlockedKeywords.value = cleaned
    }

    fun addCustomBlockedKeyword(raw: String): Boolean {
        val kw = raw.trim().lowercase(Locale.US)
        if (kw.length < 2) return false
        val next = _customBlockedKeywords.value.toMutableSet()
        if (next.add(kw)) {
            setCustomBlockedKeywords(next)
            return true
        }
        return false
    }

    fun removeCustomBlockedKeyword(keyword: String) {
        val next = _customBlockedKeywords.value.toMutableSet()
        if (next.remove(keyword)) {
            setCustomBlockedKeywords(next)
        }
    }

    // --- 6. Multi-Language Support ---

    fun setAppLanguageCode(code: String) {
        val safe = code.trim().lowercase(Locale.US).ifBlank { "system" }
        prefs.edit().putString(KEY_APP_LANGUAGE_CODE, safe).apply()
        _appLanguageCode.value = safe
    }

    companion object {
        const val DEFAULT_LOCKDOWN_HOURS = 10
        const val MAX_LOCKDOWN_MINUTES = 24 * 60
        const val LOCKDOWN_STAGE_NONE = 0
        const val LOCKDOWN_STAGE_INITIAL = 1
        const val LOCKDOWN_STAGE_DOUBLE_TIME = 2

        fun computeRecoveryTokenForChallenge(challengeCode: String): String {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(("ahsantcr_sheildlenz_recovery_v1_" + challengeCode.trim().uppercase(Locale.US)).toByteArray(Charsets.UTF_8))
            return digest.take(4).joinToString("") { "%02X".format(it) }
        }

        fun normalizeDomainInput(raw: String): String? {
            val cleaned = raw.trim()
                .lowercase(Locale.US)
                .removePrefix("https://")
                .removePrefix("http://")
                .removePrefix("www.")
                .substringBefore("/")
                .substringBefore("?")
                .substringBefore(":")
                .trim('.')
            return if (cleaned.length >= 3 && cleaned.contains('.')) cleaned else null
        }

        fun formatMinutesLabel(totalMinutes: Int): String {
            val safe = totalMinutes.coerceAtLeast(1)
            val hrs = safe / 60
            val mins = safe % 60
            return when {
                hrs == 0 -> "${mins}m"
                mins == 0 -> "${hrs}h"
                else -> "${hrs}h ${mins}m"
            }
        }

        fun isMinuteWithinWindow(minuteOfDay: Int, startMinute: Int, endMinute: Int): Boolean {
            val m = minuteOfDay.coerceIn(0, 1439)
            val s = startMinute.coerceIn(0, 1439)
            val e = endMinute.coerceIn(0, 1439)
            return if (s <= e) {
                m in s..e
            } else {
                m >= s || m <= e
            }
        }

        fun formatClockTime(minuteOfDay: Int): String {
            val safe = ((minuteOfDay % 1440) + 1440) % 1440
            val hour24 = safe / 60
            val min = safe % 60
            val amPm = if (hour24 < 12) "AM" else "PM"
            val hour12 = when {
                hour24 == 0 -> 12
                hour24 > 12 -> hour24 - 12
                else -> hour24
            }
            return String.format(java.util.Locale.US, "%d:%02d %s", hour12, min, amPm)
        }

        const val LOW_BATTERY_ENGAGE_PERCENT = 19
        const val LOW_BATTERY_RELEASE_PERCENT = 22

        /**
         * Computes the effective scan interval (in ms) combining Smart Adaptive Eco backoff
         * and <20% Low-Battery / Android Battery Saver auto-throttling.
         * Returns Pair(effectiveIntervalMs, isLowBatteryThrottled).
         */
        fun computeEffectiveScanIntervalMs(
            baseIntervalMs: Int,
            adaptiveEnabled: Boolean,
            consecutiveIdleOrSafeCount: Int,
            autoThrottleLowBattery: Boolean,
            batteryPercent: Int,
            isCharging: Boolean,
            isPowerSaveMode: Boolean,
            wasLowBatteryThrottled: Boolean
        ): Pair<Int, Boolean> {
            val lowBattActive = when {
                !autoThrottleLowBattery || isCharging -> false
                batteryPercent <= LOW_BATTERY_ENGAGE_PERCENT -> true
                batteryPercent >= LOW_BATTERY_RELEASE_PERCENT -> false
                else -> wasLowBatteryThrottled
            }

            val throttleActive = lowBattActive || (autoThrottleLowBattery && !isCharging && isPowerSaveMode)

            val adaptiveMs = if (!adaptiveEnabled) {
                baseIntervalMs
            } else {
                val multiplier = when {
                    consecutiveIdleOrSafeCount >= 14 -> 4.5f
                    consecutiveIdleOrSafeCount >= 8 -> 3.0f
                    consecutiveIdleOrSafeCount >= 4 -> 1.75f
                    else -> 1.0f
                }
                (baseIntervalMs * multiplier).toInt().coerceAtMost(1800)
            }

            val finalMs = if (throttleActive) {
                maxOf((adaptiveMs * 1.35f).toInt(), 1000).coerceIn(100, 2000)
            } else {
                adaptiveMs.coerceIn(100, 2000)
            }

            return finalMs to lowBattActive
        }

        private const val KEY_SENSITIVITY = "key_sensitivity"
        private const val KEY_STRICT_EVERYTHING = "key_strict_everything"
        private const val KEY_AUTO_START = "key_auto_start"
        private const val KEY_PIN_HASH = "key_guardian_pin_hash"
        private const val KEY_MAINTENANCE_MODE = "key_maintenance_mode"
        private const val KEY_ONBOARDING_DONE = "key_onboarding_done"
        private const val KEY_IMPORTED_MODEL_NAME = "key_imported_model_name"

        private const val KEY_LOCKDOWN_HOURS = "key_lockdown_hours"
        private const val KEY_LOCKDOWN_MINUTES = "key_lockdown_minutes"
        private const val KEY_WAIT_COOLDOWN_BEFORE_PIN = "key_wait_cooldown_before_pin"
        private const val KEY_LOCKDOWN_UNTIL_MILLIS = "key_lockdown_until_millis"
        private const val KEY_LOCKDOWN_PENDING_UNLOCK = "key_lockdown_pending_unlock"
        private const val KEY_LOCKDOWN_STAGE = "key_lockdown_stage"
        private const val KEY_SHIELD_DESIRED_ACTIVE = "key_shield_desired_active"
        private const val KEY_LOCKDOWN_APP_NAME = "key_lockdown_app_name"
        private const val KEY_LOCKDOWN_PACKAGE = "key_lockdown_package"
        private const val KEY_LOCKDOWN_CATEGORY = "key_lockdown_category"
        private const val KEY_LOCKDOWN_DETAILS = "key_lockdown_details"
        private const val KEY_LOCKDOWN_DETECTED_AT = "key_lockdown_detected_at"
        private const val KEY_BLOCKED_PACKAGE = "key_blocked_package"
        private const val KEY_ANTI_TAMPER_ENABLED = "key_anti_tamper_enabled"

        private const val KEY_ADVANCED_UNLOCKED = "key_advanced_lab_unlocked"
        private const val KEY_DOUBLE_TIME_MULTIPLIER = "key_double_time_multiplier"
        private const val KEY_POWER_MODE = "key_power_mode"
        private const val KEY_SCAN_INTERVAL_MS = "key_scan_interval_ms"
        private const val KEY_CAPTURE_WIDTH_PX = "key_capture_width_px"
        private const val KEY_SKIP_DUPLICATE_FRAMES = "key_skip_duplicate_frames"
        private const val KEY_PAUSE_WHEN_SCREEN_OFF = "key_pause_when_screen_off"
        private const val KEY_ADAPTIVE_CADENCE = "key_adaptive_cadence"
        private const val KEY_AUTO_THROTTLE_LOW_BATTERY = "key_auto_throttle_low_battery"
        private const val KEY_CUSTOM_THRESHOLDS_ENABLED = "key_custom_thresholds_enabled"
        private const val KEY_CUSTOM_EXPLICIT_THRESHOLD = "key_custom_explicit_threshold"
        private const val KEY_CUSTOM_SUGGESTIVE_THRESHOLD = "key_custom_suggestive_threshold"
        private const val KEY_CONSECUTIVE_FRAMES_REQUIRED = "key_consecutive_frames_required"
        private const val KEY_SHOW_EMERGENCY_DIALER = "key_show_emergency_dialer"
        private const val KEY_EVICT_APP_ON_THREAT = "key_evict_app_on_threat"
        private const val KEY_GUARDIAN_WINDOW_MINUTES = "key_guardian_window_minutes"
        private const val KEY_WHITELISTED_PACKAGES = "key_whitelisted_packages"
        private const val KEY_SCHEDULE_ENABLED = "key_schedule_enabled"
        private const val KEY_SCHEDULE_START_MINUTES = "key_schedule_start_minutes"
        private const val KEY_SCHEDULE_END_MINUTES = "key_schedule_end_minutes"
        private const val KEY_PARENTAL_ROLE = "key_parental_role"
        private const val KEY_PAIRED_PARENT_UID = "key_paired_parent_uid"
        private const val KEY_PAIRED_PARENT_NAME = "key_paired_parent_name"
        private const val KEY_PAIRED_CHILD_UID = "key_paired_child_uid"
        private const val KEY_CHILD_PAIRING_CODE = "key_child_pairing_code"
        private const val KEY_CHILD_UNINSTALL_LOCKED = "key_child_uninstall_locked"
        private const val KEY_PARENT_APPROVED_UNINSTALL_UNTIL = "key_parent_approved_uninstall_until"
        private const val KEY_LIVE_SEE_ENABLED = "key_live_see_enabled"
        private const val KEY_LAST_REMOTE_CMD_TS = "key_last_remote_cmd_ts"

        private const val KEY_WARNING_THRESHOLD_COUNT = "key_warning_threshold_count"
        private const val KEY_WARNINGS_IN_CYCLE = "key_warnings_in_cycle"
        private const val KEY_GUARDIAN_EMAIL = "key_guardian_email"
        private const val KEY_RECOVERY_CHALLENGE_ID = "key_recovery_challenge_id"
        private const val KEY_INFERENCE_ACCELERATION_MODE = "key_inference_acceleration_mode"
        private const val KEY_BLOCK_EXPLICIT_ADULT = "key_block_explicit_adult"
        private const val KEY_BLOCK_SUGGESTIVE_CONTENT = "key_block_suggestive_content"
        private const val KEY_BLOCK_SENSITIVE_GRAPHIC = "key_block_sensitive_graphic"
        private const val KEY_BLOCK_GAMBLING_KEYWORDS = "key_block_gambling_keywords"
        private const val KEY_BEDTIME_ENABLED = "key_bedtime_enabled"
        private const val KEY_BEDTIME_START_MINUTES = "key_bedtime_start_minutes"
        private const val KEY_BEDTIME_END_MINUTES = "key_bedtime_end_minutes"
        private const val KEY_APP_TIME_BUDGETS = "key_app_time_budgets"
        private const val KEY_DAILY_USAGE_DATE = "key_daily_usage_date"
        private const val KEY_DAILY_USAGE_MAP = "key_daily_usage_map"
        private const val KEY_DAILY_EXT_DATE = "key_daily_ext_date"
        private const val KEY_DAILY_EXT_MAP = "key_daily_ext_map"
        private const val KEY_SAFE_DNS_ENABLED = "key_safe_dns_enabled"
        private const val KEY_CUSTOM_BLOCKED_DOMAINS = "key_custom_blocked_domains"
        private const val KEY_CUSTOM_BLOCKED_KEYWORDS = "key_custom_blocked_keywords"
        private const val KEY_APP_LANGUAGE_CODE = "key_app_language_code"
    }
}
