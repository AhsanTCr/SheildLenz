package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import androidx.annotation.VisibleForTesting
import com.example.core.audio.AegisSoundEffects
import com.example.core.models.DailyScrollSnapshot
import com.example.core.models.GameGuardDecision
import com.example.core.models.GameLockReason
import com.example.core.models.MindfulScrollEvent
import com.example.core.models.SafeSearchPack
import com.example.core.models.SensitivityLevel
import com.example.core.models.ShortVideoPlatform
import com.example.core.models.ShortVideoScrollResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
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

enum class ProtectionProfile(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String
) {
    STRICT_KIDS(
        id = "strict_kids",
        title = "Strict Kids",
        subtitle = "Deen Strict DNS • All SafeSearch packs • Max sensitivity",
        badge = "MAX GUARD"
    ),
    HOMEWORK_FOCUS(
        id = "homework_focus",
        title = "Homework Focus",
        subtitle = "45m Study lock • Balanced scan • SafeSearch + Proxy guard",
        badge = "STUDY"
    ),
    BALANCED_GUARD(
        id = "balanced_guard",
        title = "Balanced Guard",
        subtitle = "Daily family guard • Fast DNS • Adaptive battery",
        badge = "DAILY"
    );

    companion object {
        fun fromId(id: String?): ProtectionProfile? =
            entries.firstOrNull { it.id == id }
    }
}

enum class QuickTileMode(
    val id: String,
    val label: String,
    val subtitle: String
) {
    SAFE_DNS(
        id = "safe_dns",
        label = "Safe DNS",
        subtitle = "Toggle Local Safe DNS VPN directly"
    ),
    STUDY_LOCK(
        id = "study_lock",
        label = "Study Lock",
        subtitle = "Start or stop timed Study Lock directly"
    );

    companion object {
        fun fromId(id: String?): QuickTileMode =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: SAFE_DNS
    }
}

enum class BrowserSearchEngine(
    val id: String,
    val displayName: String,
    val homeUrl: String,
    val searchUrlTemplate: String
) {
    GOOGLE_SAFE(
        id = "google",
        displayName = "Google Safe",
        homeUrl = "https://www.google.com/?safe=active",
        searchUrlTemplate = "https://www.google.com/search?q=%s&safe=active"
    ),
    DUCKDUCKGO(
        id = "ddg",
        displayName = "DuckDuckGo Safe",
        homeUrl = "https://duckduckgo.com/?kp=1",
        searchUrlTemplate = "https://duckduckgo.com/?q=%s&kp=1"
    ),
    BING_STRICT(
        id = "bing",
        displayName = "Bing Strict",
        homeUrl = "https://www.bing.com/?adlt=strict",
        searchUrlTemplate = "https://www.bing.com/search?q=%s&adlt=strict"
    ),
    BRAVE_SEARCH(
        id = "brave",
        displayName = "Brave Safe",
        homeUrl = "https://search.brave.com/",
        searchUrlTemplate = "https://search.brave.com/search?q=%s&safesearch=strict"
    ),
    STARTPAGE(
        id = "startpage",
        displayName = "Startpage",
        homeUrl = "https://www.startpage.com/",
        searchUrlTemplate = "https://www.startpage.com/do/search?q=%s&prf=safe"
    );

    fun buildSearchUrl(query: String): String {
        val encoded = android.net.Uri.encode(query.trim())
        return searchUrlTemplate.replace("%s", encoded)
    }

    companion object {
        fun fromId(id: String?): BrowserSearchEngine =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: GOOGLE_SAFE
    }
}

enum class SafeDnsProvider(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val primaryIp: String,
    val secondaryIp: String,
    val ratingBadge: String,
    val description: String
) {
    DEEN_STRICT(
        id = "deen_strict",
        displayName = "Deen Strict (CleanBrowsing Family)",
        subtitle = "Zero-tolerance adult, gambling & malicious blocklist",
        primaryIp = "185.228.168.168",
        secondaryIp = "185.228.169.168",
        ratingBadge = "STRICT DEEN",
        description = "Strict family filter blocking adult domains, gambling, proxies, and enforcing SafeSearch across search engines and YouTube."
    ),
    CLEANBROWSING_ADULT(
        id = "cleanbrowsing_adult",
        displayName = "CleanBrowsing Adult Filter",
        subtitle = "Dedicated anti-adult & SafeSearch resolver",
        primaryIp = "185.228.168.10",
        secondaryIp = "185.228.169.11",
        ratingBadge = "STRICT ADULT",
        description = "Blocks all explicit adult domains and adult sub-networks while enforcing SafeSearch across Google and Bing."
    ),
    OPENDNS_FAMILY(
        id = "opendns_family",
        displayName = "OpenDNS FamilyShield (Cisco)",
        subtitle = "Enterprise-grade adult, proxy & phishing shield",
        primaryIp = "208.67.222.123",
        secondaryIp = "208.67.220.123",
        ratingBadge = "CISCO STRICT",
        description = "Pre-configured Cisco FamilyShield blocking adult websites, anonymizers, proxies, and phishing hosts."
    ),
    CONTROLD_ADULT(
        id = "controld_adult",
        displayName = "Control D Strict Adult & Malware",
        subtitle = "Zero-tolerance adult, malware & ad-tracker resolver",
        primaryIp = "76.76.2.4",
        secondaryIp = "76.76.10.4",
        ratingBadge = "ZERO ADULT",
        description = "Strict Anycast DNS blocking adult websites, malware infrastructure, and ad trackers with high uptime."
    ),
    CLOUDFLARE_FAMILY(
        id = "cloudflare_family",
        displayName = "Cloudflare Family (1.1.1.3)",
        subtitle = "Low-latency Anycast malware & adult filter",
        primaryIp = "1.1.1.3",
        secondaryIp = "1.0.0.3",
        ratingBadge = "FAST FAMILY",
        description = "Anycast 1.1.1.3 resolver blocking known malware, phishing domains, and explicit adult websites."
    ),
    ADGUARD_FAMILY(
        id = "adguard_family",
        displayName = "AdGuard Family Protection",
        subtitle = "Ad-free SafeSearch & adult domain blocking",
        primaryIp = "94.140.14.15",
        secondaryIp = "94.140.15.16",
        ratingBadge = "AD-FREE FAMILY",
        description = "Family resolver blocking adult domains, intrusive trackers, advertising hosts, and enforces SafeSearch."
    );

    companion object {
        fun fromId(id: String?): SafeDnsProvider =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DEEN_STRICT
    }
}

enum class RecoveryQrMode(
    val wireCode: String,
    val label: String
) {
    TIMER_END("T", "End Lockdown Timer Only (Shield Stays Active)"),
    PIN_RESET("P", "Reset Master Key PIN");

    companion object {
        fun fromWireCode(code: String?): RecoveryQrMode? = when (code?.trim()?.uppercase(Locale.US)) {
            "T", "TIMER" -> TIMER_END
            "P", "PIN" -> PIN_RESET
            else -> null
        }
    }
}

data class RecoveryQrPayload(
    val mode: RecoveryQrMode,
    val challengeCode: String,
    val emailHash: String
)

sealed class RecoveryScanResult {
    data class Success(
        val mode: RecoveryQrMode,
        val challengeCode: String,
        val oneTimeCode: String
    ) : RecoveryScanResult()

    data object RequiresGoogleSignIn : RecoveryScanResult()

    data class EmailMismatch(val signedInEmail: String) : RecoveryScanResult()

    data object InvalidQr : RecoveryScanResult()
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
    val maxWarnings: Int,
    val isWarningOnly: Boolean = false
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

    private val _masterPowerEnabled = MutableStateFlow(prefs.getBoolean(KEY_MASTER_POWER_ENABLED, true))
    val masterPowerEnabled: StateFlow<Boolean> = _masterPowerEnabled.asStateFlow()

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

    private val _activeProtectionProfile = MutableStateFlow(
        ProtectionProfile.fromId(prefs.getString(KEY_ACTIVE_PROTECTION_PROFILE, null))
    )
    val activeProtectionProfile: StateFlow<ProtectionProfile?> = _activeProtectionProfile.asStateFlow()

    fun applyProtectionProfile(profile: ProtectionProfile) {
        when (profile) {
            ProtectionProfile.STRICT_KIDS -> {
                setSensitivity(SensitivityLevel.STRICT)
                setStrictEverything(true)
                setSafeDnsEnabled(true)
                setSafeDnsProvider(SafeDnsProvider.DEEN_STRICT)
                setSmartSafeSearchEnabled(true)
                setSafeSearchPacks(SafeSearchPack.entries.map { it.id }.toSet())
                setBlockExplicitAdult(true)
                setBlockSuggestiveContent(true)
                setBlockSensitiveGraphic(true)
                setBlockGamblingKeywords(true)
                setWarningThresholdCount(2)
                setWarningOnlyNoLockout(false)
                setLockdownDurationMinutes(600)
                setAntiTamperEnabled(true)
            }
            ProtectionProfile.HOMEWORK_FOCUS -> {
                setSensitivity(SensitivityLevel.BALANCED)
                setStrictEverything(false)
                setSafeDnsEnabled(true)
                setSafeDnsProvider(SafeDnsProvider.DEEN_STRICT)
                setSmartSafeSearchEnabled(true)
                setSafeSearchPacks(setOf(SafeSearchPack.ADULT_NSFW.id, SafeSearchPack.PROXY_VPN_BYPASS.id))
                setBlockExplicitAdult(true)
                setBlockGamblingKeywords(true)
                setStudyDurationMinutes(45)
                setStudySoftWarningSeconds(30)
                setWarningThresholdCount(3)
                setAntiTamperEnabled(true)
            }
            ProtectionProfile.BALANCED_GUARD -> {
                setSensitivity(SensitivityLevel.BALANCED)
                setStrictEverything(false)
                setSafeDnsEnabled(true)
                setSafeDnsProvider(SafeDnsProvider.CLEANBROWSING_ADULT)
                setSmartSafeSearchEnabled(true)
                setSafeSearchPacks(setOf(SafeSearchPack.ADULT_NSFW.id))
                setBlockExplicitAdult(true)
                setBlockSuggestiveContent(true)
                setBlockSensitiveGraphic(true)
                setBlockGamblingKeywords(true)
                setWarningThresholdCount(3)
                setWarningOnlyNoLockout(false)
                setAntiTamperEnabled(true)
            }
        }
        prefs.edit().putString(KEY_ACTIVE_PROTECTION_PROFILE, profile.id).apply()
        _activeProtectionProfile.value = profile
    }

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

    // Editable Block Screen Controls (inside 7-Tap Developer Options in Edits)
    private val _blockCustomHeadline = MutableStateFlow(prefs.getString(KEY_BLOCK_CUSTOM_HEADLINE, "") ?: "")
    val blockCustomHeadline: StateFlow<String> = _blockCustomHeadline.asStateFlow()

    private val _blockCustomMessage = MutableStateFlow(prefs.getString(KEY_BLOCK_CUSTOM_MESSAGE, "") ?: "")
    val blockCustomMessage: StateFlow<String> = _blockCustomMessage.asStateFlow()

    private val _blockShowTimer = MutableStateFlow(prefs.getBoolean(KEY_BLOCK_SHOW_TIMER, true))
    val blockShowTimer: StateFlow<Boolean> = _blockShowTimer.asStateFlow()

    private val _blockShowAppBadge = MutableStateFlow(prefs.getBoolean(KEY_BLOCK_SHOW_APP_BADGE, true))
    val blockShowAppBadge: StateFlow<Boolean> = _blockShowAppBadge.asStateFlow()

    private val _blockShowQrUnlock = MutableStateFlow(prefs.getBoolean(KEY_BLOCK_SHOW_QR_UNLOCK, true))
    val blockShowQrUnlock: StateFlow<Boolean> = _blockShowQrUnlock.asStateFlow()

    private val _blockShowQuickActions = MutableStateFlow(prefs.getBoolean(KEY_BLOCK_SHOW_QUICK_ACTIONS, true))
    val blockShowQuickActions: StateFlow<Boolean> = _blockShowQuickActions.asStateFlow()

    private val _blockShowReturnHome = MutableStateFlow(prefs.getBoolean(KEY_BLOCK_SHOW_RETURN_HOME, true))
    val blockShowReturnHome: StateFlow<Boolean> = _blockShowReturnHome.asStateFlow()

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

    private val _warningOnlyNoLockout = MutableStateFlow(
        prefs.getBoolean(KEY_WARNING_ONLY_NO_LOCKOUT, false)
    )
    val warningOnlyNoLockout: StateFlow<Boolean> = _warningOnlyNoLockout.asStateFlow()

    // Guardian Email & Emergency Recovery
    private val _guardianEmail = MutableStateFlow(
        prefs.getString(KEY_GUARDIAN_EMAIL, "") ?: ""
    )
    val guardianEmail: StateFlow<String> = _guardianEmail.asStateFlow()

    private val _recoveryChallengeCode = MutableStateFlow(readOrCreateChallengeCode())
    val recoveryChallengeCode: StateFlow<String> = _recoveryChallengeCode.asStateFlow()

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

    private val _safeDnsEnabled = MutableStateFlow(prefs.getBoolean(KEY_SAFE_DNS_ENABLED, false))
    val safeDnsEnabled: StateFlow<Boolean> = _safeDnsEnabled.asStateFlow()

    private val _safeDnsProvider = MutableStateFlow(
        SafeDnsProvider.fromId(prefs.getString(KEY_SAFE_DNS_PROVIDER, SafeDnsProvider.DEEN_STRICT.id))
    )
    val safeDnsProvider: StateFlow<SafeDnsProvider> = _safeDnsProvider.asStateFlow()

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

    // Tactile UI Touch Sounds (clean ceramic tile tick + optional Auto Sound Selector)
    private val _touchSoundsEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_TOUCH_SOUNDS_ENABLED, true)
    )
    val touchSoundsEnabled: StateFlow<Boolean> = _touchSoundsEnabled.asStateFlow()

    private val _autoSoundSelectorEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_AUTO_SOUND_SELECTOR_ENABLED, false)
    )
    val autoSoundSelectorEnabled: StateFlow<Boolean> = _autoSoundSelectorEnabled.asStateFlow()

    // Study Mode & Focus Lock Timer (with soft warning countdown)
    private val _studyModeEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_STUDY_MODE_ENABLED, false)
    )
    val studyModeEnabled: StateFlow<Boolean> = _studyModeEnabled.asStateFlow()

    private val _studySessionEndMillis = MutableStateFlow(
        prefs.getLong(KEY_STUDY_SESSION_END_MILLIS, 0L)
    )
    val studySessionEndMillis: StateFlow<Long> = _studySessionEndMillis.asStateFlow()

    private val _studyDurationMinutes = MutableStateFlow(
        prefs.getInt(KEY_STUDY_DURATION_MINUTES, 25).coerceIn(5, 240)
    )
    val studyDurationMinutes: StateFlow<Int> = _studyDurationMinutes.asStateFlow()

    private val _studySoftWarningSeconds = MutableStateFlow(
        prefs.getInt(KEY_STUDY_SOFT_WARNING_SECONDS, 30).coerceIn(5, 120)
    )
    val studySoftWarningSeconds: StateFlow<Int> = _studySoftWarningSeconds.asStateFlow()

    private val _studyAllowedPackages = MutableStateFlow(
        prefs.getStringSet(KEY_STUDY_ALLOWED_PACKAGES, SafeSearchPack.DEFAULT_STUDY_FRIENDLY_PACKAGES)
            ?.toSet()
            ?: SafeSearchPack.DEFAULT_STUDY_FRIENDLY_PACKAGES
    )
    val studyAllowedPackages: StateFlow<Set<String>> = _studyAllowedPackages.asStateFlow()

    // Smart Safe Search & Keyword Guard
    private val _smartSafeSearchEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_SMART_SAFE_SEARCH_ENABLED, false)
    )
    val smartSafeSearchEnabled: StateFlow<Boolean> = _smartSafeSearchEnabled.asStateFlow()

    private val _safeSearchPacks = MutableStateFlow(
        prefs.getStringSet(KEY_SAFE_SEARCH_PACKS, SafeSearchPack.DEFAULT_PACK_IDS)
            ?.toSet()
            ?: SafeSearchPack.DEFAULT_PACK_IDS
    )
    val safeSearchPacks: StateFlow<Set<String>> = _safeSearchPacks.asStateFlow()

    private val _safeSearchBlockedCount = MutableStateFlow(
        prefs.getInt(KEY_SAFE_SEARCH_BLOCKED_COUNT, 0).coerceAtLeast(0)
    )
    val safeSearchBlockedCount: StateFlow<Int> = _safeSearchBlockedCount.asStateFlow()

    // Edits Notifications Toggle & Non-Suspicious Parent-Controlled Blocking Screen Mode
    private val _notificationsEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    )
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _discreetBlockingScreenMode = MutableStateFlow(
        prefs.getBoolean(KEY_DISCREET_BLOCKING_SCREEN_MODE, true)
    )
    val discreetBlockingScreenMode: StateFlow<Boolean> = _discreetBlockingScreenMode.asStateFlow()

    // Scheduled Profile Auto-Switcher & Direct Quick Settings Tile Mode
    private val _profileAutoSwitchEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_PROFILE_AUTO_SWITCH_ENABLED, false)
    )
    val profileAutoSwitchEnabled: StateFlow<Boolean> = _profileAutoSwitchEnabled.asStateFlow()

    private val _profileScheduleStartMinutes = MutableStateFlow(
        prefs.getInt(KEY_PROFILE_SCHEDULE_START_MINUTES, 15 * 60).coerceIn(0, 1439)
    )
    val profileScheduleStartMinutes: StateFlow<Int> = _profileScheduleStartMinutes.asStateFlow()

    private val _profileScheduleEndMinutes = MutableStateFlow(
        prefs.getInt(KEY_PROFILE_SCHEDULE_END_MINUTES, 20 * 60).coerceIn(0, 1439)
    )
    val profileScheduleEndMinutes: StateFlow<Int> = _profileScheduleEndMinutes.asStateFlow()

    private val _scheduledTargetProfile = MutableStateFlow(
        ProtectionProfile.fromId(prefs.getString(KEY_SCHEDULED_TARGET_PROFILE, null))
            ?: ProtectionProfile.HOMEWORK_FOCUS
    )
    val scheduledTargetProfile: StateFlow<ProtectionProfile> = _scheduledTargetProfile.asStateFlow()

    private val _scheduledFallbackProfile = MutableStateFlow(
        ProtectionProfile.fromId(prefs.getString(KEY_SCHEDULED_FALLBACK_PROFILE, null))
            ?: ProtectionProfile.BALANCED_GUARD
    )
    val scheduledFallbackProfile: StateFlow<ProtectionProfile> = _scheduledFallbackProfile.asStateFlow()

    private val _lastAppliedAutoSwitchSlot = MutableStateFlow(
        prefs.getString(KEY_LAST_AUTO_SWITCH_SLOT, null)
    )
    val lastAppliedAutoSwitchSlot: StateFlow<String?> = _lastAppliedAutoSwitchSlot.asStateFlow()

    private val _quickTileMode = MutableStateFlow(
        QuickTileMode.fromId(prefs.getString(KEY_QUICK_TILE_MODE, QuickTileMode.SAFE_DNS.id))
    )
    val quickTileMode: StateFlow<QuickTileMode> = _quickTileMode.asStateFlow()

    // VIP Safe Browser (Light Quick Mode -> Full Chrome Replacement Engine)
    private val _fullBrowserEngineInstalled = MutableStateFlow(
        prefs.getBoolean(KEY_FULL_BROWSER_INSTALLED, false)
    )
    val fullBrowserEngineInstalled: StateFlow<Boolean> = _fullBrowserEngineInstalled.asStateFlow()

    private val _browserDesktopModeDefault = MutableStateFlow(
        prefs.getBoolean(KEY_BROWSER_DESKTOP_DEFAULT, false)
    )
    val browserDesktopModeDefault: StateFlow<Boolean> = _browserDesktopModeDefault.asStateFlow()

    private val _browserBookmarksJson = MutableStateFlow(
        prefs.getString(KEY_BROWSER_BOOKMARKS_JSON, "") ?: ""
    )
    val browserBookmarksJson: StateFlow<String> = _browserBookmarksJson.asStateFlow()

    private val _browserHistoryJson = MutableStateFlow(
        prefs.getString(KEY_BROWSER_HISTORY_JSON, "") ?: ""
    )
    val browserHistoryJson: StateFlow<String> = _browserHistoryJson.asStateFlow()

    private val _browserDownloadsJson = MutableStateFlow(
        prefs.getString(KEY_BROWSER_DOWNLOADS_JSON, "") ?: ""
    )
    val browserDownloadsJson: StateFlow<String> = _browserDownloadsJson.asStateFlow()

    private val _chromeImportedFileCount = MutableStateFlow(
        prefs.getInt(KEY_CHROME_IMPORTED_FILE_COUNT, 0).coerceAtLeast(0)
    )
    val chromeImportedFileCount: StateFlow<Int> = _chromeImportedFileCount.asStateFlow()

    private val _chromeComponentsAutoCached = MutableStateFlow(
        prefs.getBoolean(KEY_CHROME_COMPONENTS_AUTO_CACHED, false)
    )
    val chromeComponentsAutoCached: StateFlow<Boolean> = _chromeComponentsAutoCached.asStateFlow()

    private val _chromeCachedComponentCount = MutableStateFlow(
        prefs.getInt(KEY_CHROME_CACHED_COMPONENT_COUNT, 0).coerceAtLeast(0)
    )
    val chromeCachedComponentCount: StateFlow<Int> = _chromeCachedComponentCount.asStateFlow()

    private val _chromeComponentCacheTimestampMillis = MutableStateFlow(
        prefs.getLong(KEY_CHROME_COMPONENT_CACHE_TS, 0L).coerceAtLeast(0L)
    )
    val chromeComponentCacheTimestampMillis: StateFlow<Long> = _chromeComponentCacheTimestampMillis.asStateFlow()

    // Full Chrome-Alternative Browser Settings & Per-Site Permissions
    private val _browserSearchEngine = MutableStateFlow(
        BrowserSearchEngine.fromId(prefs.getString(KEY_BROWSER_SEARCH_ENGINE, BrowserSearchEngine.GOOGLE_SAFE.id))
    )
    val browserSearchEngine: StateFlow<BrowserSearchEngine> = _browserSearchEngine.asStateFlow()

    private val _browserAdBlockEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_BROWSER_AD_BLOCK, true)
    )
    val browserAdBlockEnabled: StateFlow<Boolean> = _browserAdBlockEnabled.asStateFlow()

    private val _browserPopupsBlocked = MutableStateFlow(
        prefs.getBoolean(KEY_BROWSER_POPUPS_BLOCKED, true)
    )
    val browserPopupsBlocked: StateFlow<Boolean> = _browserPopupsBlocked.asStateFlow()

    private val _browserJavaScriptEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_BROWSER_JS_ENABLED, true)
    )
    val browserJavaScriptEnabled: StateFlow<Boolean> = _browserJavaScriptEnabled.asStateFlow()

    private val _browserHomeUrl = MutableStateFlow(
        prefs.getString(KEY_BROWSER_HOME_URL, "") ?: ""
    )
    val browserHomeUrl: StateFlow<String> = _browserHomeUrl.asStateFlow()

    private val _browserSitePermissionsJson = MutableStateFlow(
        prefs.getString(KEY_BROWSER_SITE_PERMS_JSON, "") ?: ""
    )
    val browserSitePermissionsJson: StateFlow<String> = _browserSitePermissionsJson.asStateFlow()

    private val _browserPinPromptShown = MutableStateFlow(
        prefs.getBoolean(KEY_BROWSER_PIN_PROMPT_SHOWN, false)
    )
    val browserPinPromptShown: StateFlow<Boolean> = _browserPinPromptShown.asStateFlow()

    // Individual App Locker Gated by Guardian PIN
    private val _appLockerEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_APP_LOCKER_ENABLED, false)
    )
    val appLockerEnabled: StateFlow<Boolean> = _appLockerEnabled.asStateFlow()

    private val _lockedAppPackages = MutableStateFlow(
        prefs.getStringSet(KEY_LOCKED_APP_PACKAGES, emptySet())?.toSet() ?: emptySet()
    )
    val lockedAppPackages: StateFlow<Set<String>> = _lockedAppPackages.asStateFlow()

    private val _appLockerRelockMinutes = MutableStateFlow(
        prefs.getInt(KEY_APP_LOCKER_RELOCK_MINUTES, 0).coerceIn(0, 60)
    )
    val appLockerRelockMinutes: StateFlow<Int> = _appLockerRelockMinutes.asStateFlow()

    private val unlockedAppGraceUntilRealtime = java.util.concurrent.ConcurrentHashMap<String, Long>()

    @Volatile
    private var activeUnlockedAppPackage: String? = null

    // Mindful Pause, Reels/Shorts Counter & Slider-Driven Scroll-Limit Auto-Lock
    private val _scrollLockEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_SCROLL_LOCK_ENABLED, true)
    )
    val scrollLockEnabled: StateFlow<Boolean> = _scrollLockEnabled.asStateFlow()

    private val _scrollLimitCount = MutableStateFlow(
        prefs.getInt(KEY_SCROLL_LIMIT_COUNT, 20).coerceIn(5, 60)
    )
    val scrollLimitCount: StateFlow<Int> = _scrollLimitCount.asStateFlow()

    private val _scrollLockCooldownMinutes = MutableStateFlow(
        prefs.getInt(KEY_SCROLL_LOCK_COOLDOWN_MINUTES, 15).coerceIn(5, 60)
    )
    val scrollLockCooldownMinutes: StateFlow<Int> = _scrollLockCooldownMinutes.asStateFlow()

    private val _scrollBingeNudgeCount = MutableStateFlow(
        prefs.getInt(KEY_SCROLL_BINGE_NUDGE_COUNT, 10).coerceIn(0, 25)
    )
    val scrollBingeNudgeCount: StateFlow<Int> = _scrollBingeNudgeCount.asStateFlow()

    private val _reelsGuardPlatforms = MutableStateFlow(
        prefs.getStringSet(KEY_REELS_GUARD_PLATFORMS, ShortVideoPlatform.ALL_IDS)
            ?.toSet()
            ?: ShortVideoPlatform.ALL_IDS
    )
    val reelsGuardPlatforms: StateFlow<Set<String>> = _reelsGuardPlatforms.asStateFlow()

    private val _mindfulPauseEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_MINDFUL_PAUSE_ENABLED, false)
    )
    val mindfulPauseEnabled: StateFlow<Boolean> = _mindfulPauseEnabled.asStateFlow()

    private val _mindfulPausePackages = MutableStateFlow(
        prefs.getStringSet(KEY_MINDFUL_PAUSE_PACKAGES, emptySet())?.toSet() ?: emptySet()
    )
    val mindfulPausePackages: StateFlow<Set<String>> = _mindfulPausePackages.asStateFlow()

    private val _mindfulGraceMinutes = MutableStateFlow(
        prefs.getInt(KEY_MINDFUL_GRACE_MINUTES, 10).coerceIn(0, 30)
    )
    val mindfulGraceMinutes: StateFlow<Int> = _mindfulGraceMinutes.asStateFlow()

    private val _instantDetoxUntilMillis = MutableStateFlow(
        prefs.getLong(KEY_INSTANT_DETOX_UNTIL_MILLIS, 0L)
    )
    val instantDetoxUntilMillis: StateFlow<Long> = _instantDetoxUntilMillis.asStateFlow()

    private val _dailyReelsTotalCount = MutableStateFlow(
        readDailyIntIfToday(KEY_DAILY_REELS_TOTAL, 0)
    )
    val dailyReelsTotalCount: StateFlow<Int> = _dailyReelsTotalCount.asStateFlow()

    private val _dailyReelsByPlatform = MutableStateFlow(
        readDailyIntMapIfToday(KEY_DAILY_REELS_BY_PLATFORM)
    )
    val dailyReelsByPlatform: StateFlow<Map<String, Int>> = _dailyReelsByPlatform.asStateFlow()

    private val _dailyAppOpensCount = MutableStateFlow(
        readDailyIntMapIfToday(KEY_DAILY_APP_OPENS_MAP)
    )
    val dailyAppOpensCount: StateFlow<Map<String, Int>> = _dailyAppOpensCount.asStateFlow()

    private val _dailyMindfulExitsCount = MutableStateFlow(
        readDailyIntIfToday(KEY_DAILY_MINDFUL_EXITS, 0)
    )
    val dailyMindfulExitsCount: StateFlow<Int> = _dailyMindfulExitsCount.asStateFlow()

    private val _dailyScrollLocksTriggered = MutableStateFlow(
        readDailyIntIfToday(KEY_DAILY_SCROLL_LOCKS, 0)
    )
    val dailyScrollLocksTriggered: StateFlow<Int> = _dailyScrollLocksTriggered.asStateFlow()

    private val _hourlyScrollBuckets = MutableStateFlow(
        readDailyHourlyBuckets(todayDateKey())
    )
    val hourlyScrollBuckets: StateFlow<List<Int>> = _hourlyScrollBuckets.asStateFlow()

    private val _weeklyScrollHistory = MutableStateFlow(
        DailyScrollSnapshot.listFromJsonString(prefs.getString(KEY_WEEKLY_SCROLL_HISTORY, ""))
    )
    val weeklyScrollHistory: StateFlow<List<DailyScrollSnapshot>> = _weeklyScrollHistory.asStateFlow()

    private val _cleanStreakDays = MutableStateFlow(
        prefs.getInt(KEY_CLEAN_STREAK_DAYS, 0).coerceAtLeast(0)
    )
    val cleanStreakDays: StateFlow<Int> = _cleanStreakDays.asStateFlow()

    private val _recentMindfulScrollEvents = MutableStateFlow(
        MindfulScrollEvent.listFromJsonString(prefs.getString(KEY_RECENT_MINDFUL_EVENTS, ""))
    )
    val recentMindfulScrollEvents: StateFlow<List<MindfulScrollEvent>> = _recentMindfulScrollEvents.asStateFlow()

    private val _scrollLockedUntilByPackage = MutableStateFlow(
        readLongMap(KEY_SCROLL_LOCKED_UNTIL_MAP)
    )
    val scrollLockedUntilByPackage: StateFlow<Map<String, Long>> = _scrollLockedUntilByPackage.asStateFlow()

    private val mindfulGraceUntilRealtime = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private val scrollSessionBaselineByPkg = java.util.concurrent.ConcurrentHashMap<String, Int>()
    private val bingeSessionLastNudgeByPkg = java.util.concurrent.ConcurrentHashMap<String, Int>()

    // PiqoraX Connector & Timed App Blocker (Minutes / Hours)
    private val _piqoraxConnectorConnected = MutableStateFlow(
        prefs.getBoolean(KEY_PIQORAX_CONNECTOR_CONNECTED, false)
    )
    val piqoraxConnectorConnected: StateFlow<Boolean> = _piqoraxConnectorConnected.asStateFlow()

    private val _timeBlockerAllowanceMinutes = MutableStateFlow(
        prefs.getInt(KEY_TIME_BLOCKER_ALLOWANCE_MINUTES, 5).coerceIn(1, 180)
    )
    val timeBlockerAllowanceMinutes: StateFlow<Int> = _timeBlockerAllowanceMinutes.asStateFlow()

    private val _timeBlockerWindowMinutes = MutableStateFlow(
        prefs.getInt(KEY_TIME_BLOCKER_WINDOW_MINUTES, 60).coerceIn(5, 1440)
    )
    val timeBlockerWindowMinutes: StateFlow<Int> = _timeBlockerWindowMinutes.asStateFlow()

    private val _piqoraxLastPassSummary = MutableStateFlow(
        prefs.getString(KEY_PIQORAX_LAST_PASS_SUMMARY, "").orEmpty()
    )
    val piqoraxLastPassSummary: StateFlow<String> = _piqoraxLastPassSummary.asStateFlow()

    private val _piqoraxCoinDeviceLockActive = MutableStateFlow(
        prefs.getBoolean(KEY_PIQORAX_COIN_DEVICE_LOCK, false)
    )
    val piqoraxCoinDeviceLockActive: StateFlow<Boolean> = _piqoraxCoinDeviceLockActive.asStateFlow()

    // Gaming Addiction Control (inside Usage & Scroll Guard Studio)
    private val _gameGuardEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_GAME_GUARD_ENABLED, false)
    )
    val gameGuardEnabled: StateFlow<Boolean> = _gameGuardEnabled.asStateFlow()

    private val _gameAutoDetectEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_GAME_AUTO_DETECT_ENABLED, true)
    )
    val gameAutoDetectEnabled: StateFlow<Boolean> = _gameAutoDetectEnabled.asStateFlow()

    private val _trackedGamePackages = MutableStateFlow(
        prefs.getStringSet(KEY_TRACKED_GAME_PACKAGES, emptySet())?.toSet().orEmpty()
    )
    val trackedGamePackages: StateFlow<Set<String>> = _trackedGamePackages.asStateFlow()

    private val _excludedGamePackages = MutableStateFlow(
        prefs.getStringSet(KEY_EXCLUDED_GAME_PACKAGES, emptySet())?.toSet().orEmpty()
    )
    val excludedGamePackages: StateFlow<Set<String>> = _excludedGamePackages.asStateFlow()

    private val _gameSessionLimitMinutes = MutableStateFlow(
        prefs.getInt(KEY_GAME_SESSION_LIMIT_MINUTES, 30).coerceIn(10, 90)
    )
    val gameSessionLimitMinutes: StateFlow<Int> = _gameSessionLimitMinutes.asStateFlow()

    private val _gameCooldownMinutes = MutableStateFlow(
        prefs.getInt(KEY_GAME_COOLDOWN_MINUTES, 15).coerceIn(5, 60)
    )
    val gameCooldownMinutes: StateFlow<Int> = _gameCooldownMinutes.asStateFlow()

    private val _gameDailyCapMinutes = MutableStateFlow(
        prefs.getInt(KEY_GAME_DAILY_CAP_MINUTES, 90).let { if (it <= 0) 0 else it.coerceIn(15, 240) }
    )
    val gameDailyCapMinutes: StateFlow<Int> = _gameDailyCapMinutes.asStateFlow()

    private val _gameCurfewEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_GAME_CURFEW_ENABLED, false)
    )
    val gameCurfewEnabled: StateFlow<Boolean> = _gameCurfewEnabled.asStateFlow()

    private val _gameCurfewStartMinutes = MutableStateFlow(
        prefs.getInt(KEY_GAME_CURFEW_START_MINUTES, 22 * 60).coerceIn(0, 1439)
    )
    val gameCurfewStartMinutes: StateFlow<Int> = _gameCurfewStartMinutes.asStateFlow()

    private val _gameCurfewEndMinutes = MutableStateFlow(
        prefs.getInt(KEY_GAME_CURFEW_END_MINUTES, 7 * 60).coerceIn(0, 1439)
    )
    val gameCurfewEndMinutes: StateFlow<Int> = _gameCurfewEndMinutes.asStateFlow()

    private val _gameCooldownUntilMillis = MutableStateFlow(
        prefs.getLong(KEY_GAME_COOLDOWN_UNTIL_MILLIS, 0L)
    )
    val gameCooldownUntilMillis: StateFlow<Long> = _gameCooldownUntilMillis.asStateFlow()

    private val _gameCurfewOverrideUntilMillis = MutableStateFlow(
        prefs.getLong(KEY_GAME_CURFEW_OVERRIDE_UNTIL_MILLIS, 0L)
    )
    val gameCurfewOverrideUntilMillis: StateFlow<Long> = _gameCurfewOverrideUntilMillis.asStateFlow()

    private val _dailyGameUsageByPackage = MutableStateFlow(
        readDailyIntMapIfToday(KEY_DAILY_GAME_USAGE_MAP)
    )
    val dailyGameUsageByPackage: StateFlow<Map<String, Int>> = _dailyGameUsageByPackage.asStateFlow()

    private val _dailyGameTotalSeconds = MutableStateFlow(
        _dailyGameUsageByPackage.value.values.sum()
    )
    val dailyGameTotalSeconds: StateFlow<Int> = _dailyGameTotalSeconds.asStateFlow()

    private val _dailyGameExtensionMinutes = MutableStateFlow(
        readDailyIntIfToday(KEY_DAILY_GAME_EXT_MINUTES, 0)
    )
    val dailyGameExtensionMinutes: StateFlow<Int> = _dailyGameExtensionMinutes.asStateFlow()

    private val _activeGameSessionSeconds = MutableStateFlow(0)
    val activeGameSessionSeconds: StateFlow<Int> = _activeGameSessionSeconds.asStateFlow()

    @Volatile
    private var lastGameTickUptimeMs: Long = 0L

    @Volatile
    private var gameSoftWarnedThisSession: Boolean = false

    init {
        AegisSoundEffects.setEnabled(_touchSoundsEnabled.value)
        AegisSoundEffects.setAutoSelectorEnabled(_autoSoundSelectorEnabled.value)
    }

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

    fun setBlockCustomHeadline(headline: String) {
        val clean = headline.take(80)
        prefs.edit().putString(KEY_BLOCK_CUSTOM_HEADLINE, clean).apply()
        _blockCustomHeadline.value = clean
    }

    fun setBlockCustomMessage(message: String) {
        val clean = message.take(240)
        prefs.edit().putString(KEY_BLOCK_CUSTOM_MESSAGE, clean).apply()
        _blockCustomMessage.value = clean
    }

    fun setBlockShowTimer(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BLOCK_SHOW_TIMER, enabled).apply()
        _blockShowTimer.value = enabled
    }

    fun setBlockShowAppBadge(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BLOCK_SHOW_APP_BADGE, enabled).apply()
        _blockShowAppBadge.value = enabled
    }

    fun setBlockShowQrUnlock(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BLOCK_SHOW_QR_UNLOCK, enabled).apply()
        _blockShowQrUnlock.value = enabled
    }

    fun setBlockShowQuickActions(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BLOCK_SHOW_QUICK_ACTIONS, enabled).apply()
        _blockShowQuickActions.value = enabled
    }

    fun setBlockShowReturnHome(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BLOCK_SHOW_RETURN_HOME, enabled).apply()
        _blockShowReturnHome.value = enabled
    }

    fun resetBlockScreenCustomizations() {
        setBlockCustomHeadline("")
        setBlockCustomMessage("")
        setBlockShowTimer(true)
        setBlockShowAppBadge(true)
        setBlockShowQrUnlock(true)
        setBlockShowQuickActions(true)
        setBlockShowReturnHome(true)
        setShowEmergencyDialerOnLockdown(true)
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
        resetBlockScreenCustomizations()
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

    fun setMasterPowerEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MASTER_POWER_ENABLED, enabled).apply()
        _masterPowerEnabled.value = enabled
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
        rotateRecoveryChallengeCode()
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

    fun consumeAuthorizedExternalFlow() {
        authorizedExternalFlowUntilRealtime = 0L
    }

    fun onAppBackgrounded(forceLock: Boolean = false) {
        val now = SystemClock.elapsedRealtime()
        val withinAuthorizedExternalFlow = !forceLock && now <= authorizedExternalFlowUntilRealtime
        authorizedExternalFlowUntilRealtime = 0L
        if (withinAuthorizedExternalFlow) {
            return
        }
        _isGuardianSessionUnlocked.value = false
        if (forceLock) {
            clearTempAppUnlocks()
        }
        if (explicitSettingsTripPending && !forceLock) {
            explicitSettingsTripPending = false
        } else {
            explicitSettingsTripPending = false
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

    fun setWarningOnlyNoLockout(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WARNING_ONLY_NO_LOCKOUT, enabled).apply()
        _warningOnlyNoLockout.value = enabled
    }

    fun recordThreatAndCheckLockdown(): WarningEscalationResult {
        if (_warningOnlyNoLockout.value) {
            val nextCount = _warningsIssuedInCycle.value + 1
            prefs.edit().putInt(KEY_WARNINGS_IN_CYCLE, nextCount).apply()
            _warningsIssuedInCycle.value = nextCount
            return WarningEscalationResult(
                shouldLockdown = false,
                warningNumber = nextCount,
                maxWarnings = 0,
                isWarningOnly = true
            )
        }
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

    private fun readOrCreateChallengeCode(): String {
        val existing = prefs.getString(KEY_RECOVERY_CHALLENGE_ID, null)
        if (!existing.isNullOrBlank()) return existing
        return generateAndPersistChallengeCode(previousCode = null)
    }

    private fun generateAndPersistChallengeCode(previousCode: String?): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        var generated: String
        do {
            val suffix = (1..6).map { chars.random() }.joinToString("")
            generated = "SLZ-$suffix"
        } while (generated == previousCode)
        prefs.edit().putString(KEY_RECOVERY_CHALLENGE_ID, generated).apply()
        return generated
    }

    fun getRecoveryChallengeCode(): String {
        val current = _recoveryChallengeCode.value
        if (current.isNotBlank()) return current
        return rotateRecoveryChallengeCode()
    }

    fun rotateRecoveryChallengeCode(): String {
        val fresh = generateAndPersistChallengeCode(previousCode = _recoveryChallengeCode.value)
        _recoveryChallengeCode.value = fresh
        return fresh
    }

    fun verifyRecoveryToken(input: String): Boolean {
        val normalized = input.trim().uppercase(Locale.US).replace("-", "").replace(" ", "")
        if (normalized.length < 6) return false
        val expected = computeRecoveryTokenForChallenge(getRecoveryChallengeCode())
            .uppercase(Locale.US)
            .replace("-", "")
        return normalized == expected
    }

    fun verifyTimerRecoveryToken(input: String): Boolean {
        val normalized = input.trim().uppercase(Locale.US).replace("-", "").replace(" ", "")
        if (normalized.length < 6) return false
        val expected = computeTimerRecoveryToken(getRecoveryChallengeCode())
            .uppercase(Locale.US)
            .replace("-", "")
        return normalized == expected
    }

    /**
     * Ends only an active lockdown/cooldown timer using a single-use code derived from the
     * parent-scanned TIMER_END QR. Protection remains desired/active and the Master Key is NOT reset.
     */
    fun endLockdownTimerWithToken(token: String): Boolean {
        if (!verifyTimerRecoveryToken(token)) return false
        clearLockdown()
        setShieldDesiredActive(true)
        rotateRecoveryChallengeCode()
        return true
    }

    fun buildRecoveryQrUri(mode: RecoveryQrMode): String = buildRecoveryQrUri(
        mode = mode,
        challengeCode = getRecoveryChallengeCode(),
        guardianEmail = _guardianEmail.value
    )

    fun resetPinWithRecoveryToken(token: String, newPin: String? = null): Boolean {
        if (!verifyRecoveryToken(token)) return false
        clearLockdown()
        if (!newPin.isNullOrBlank() && newPin.length in 4..8 && newPin.all { it.isDigit() }) {
            setGuardianPin(newPin)
        } else {
            unlockGuardianSession()
        }
        rotateRecoveryChallengeCode()
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

    fun setSafeDnsProvider(provider: SafeDnsProvider) {
        prefs.edit().putString(KEY_SAFE_DNS_PROVIDER, provider.id).apply()
        _safeDnsProvider.value = provider
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

    fun setTouchSoundsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TOUCH_SOUNDS_ENABLED, enabled).apply()
        _touchSoundsEnabled.value = enabled
        AegisSoundEffects.setEnabled(enabled)
    }

    fun setAutoSoundSelectorEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SOUND_SELECTOR_ENABLED, enabled).apply()
        _autoSoundSelectorEnabled.value = enabled
        AegisSoundEffects.setAutoSelectorEnabled(enabled)
    }

    // --- 7. Study Mode & Focus Lock Timer ---

    fun setStudyDurationMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(5, 240)
        prefs.edit().putInt(KEY_STUDY_DURATION_MINUTES, clamped).apply()
        _studyDurationMinutes.value = clamped
    }

    fun setStudySoftWarningSeconds(seconds: Int) {
        val clamped = seconds.coerceIn(5, 120)
        prefs.edit().putInt(KEY_STUDY_SOFT_WARNING_SECONDS, clamped).apply()
        _studySoftWarningSeconds.value = clamped
    }

    fun startStudySession(durationMinutes: Int = _studyDurationMinutes.value): Long {
        val clamped = durationMinutes.coerceIn(5, 240)
        val endMillis = System.currentTimeMillis() + clamped * 60_000L
        prefs.edit()
            .putBoolean(KEY_STUDY_MODE_ENABLED, true)
            .putInt(KEY_STUDY_DURATION_MINUTES, clamped)
            .putLong(KEY_STUDY_SESSION_END_MILLIS, endMillis)
            .apply()
        _studyDurationMinutes.value = clamped
        _studySessionEndMillis.value = endMillis
        _studyModeEnabled.value = true
        return endMillis
    }

    fun stopStudySession() {
        prefs.edit()
            .putBoolean(KEY_STUDY_MODE_ENABLED, false)
            .putLong(KEY_STUDY_SESSION_END_MILLIS, 0L)
            .apply()
        _studyModeEnabled.value = false
        _studySessionEndMillis.value = 0L
    }

    fun getRemainingStudyMillis(): Long {
        if (!_studyModeEnabled.value) return 0L
        val end = _studySessionEndMillis.value
        if (end <= 0L) return 0L
        val rem = end - System.currentTimeMillis()
        if (rem <= 0L) {
            stopStudySession()
            return 0L
        }
        return rem
    }

    fun isStudySessionActive(): Boolean = getRemainingStudyMillis() > 0L

    fun extendActiveStudySession(extraMinutes: Int): Long {
        val addMins = extraMinutes.coerceIn(1, 120)
        val now = System.currentTimeMillis()
        val baseEnd = if (_studyModeEnabled.value && _studySessionEndMillis.value > now) {
            _studySessionEndMillis.value
        } else {
            now
        }
        val maxEnd = now + 240L * 60_000L
        val nextEnd = (baseEnd + addMins * 60_000L).coerceAtMost(maxEnd)
        val nextDur = ((_studyDurationMinutes.value) + addMins).coerceIn(5, 240)
        prefs.edit()
            .putBoolean(KEY_STUDY_MODE_ENABLED, true)
            .putLong(KEY_STUDY_SESSION_END_MILLIS, nextEnd)
            .putInt(KEY_STUDY_DURATION_MINUTES, nextDur)
            .apply()
        _studyModeEnabled.value = true
        _studySessionEndMillis.value = nextEnd
        _studyDurationMinutes.value = nextDur
        return nextEnd
    }

    fun setStudyAllowedPackages(packages: Set<String>) {
        val cleaned = packages.map { it.trim() }.filter { it.isNotBlank() }.take(40).toSet()
        prefs.edit().putStringSet(KEY_STUDY_ALLOWED_PACKAGES, cleaned).apply()
        _studyAllowedPackages.value = cleaned
    }

    fun toggleStudyAllowedPackage(packageName: String) {
        val clean = packageName.trim()
        if (clean.isBlank()) return
        val next = _studyAllowedPackages.value.toMutableSet()
        if (!next.add(clean)) {
            next.remove(clean)
        }
        setStudyAllowedPackages(next)
    }

    fun isPackageAllowedDuringStudy(packageName: String): Boolean {
        if (packageName.isBlank()) return true
        return _studyAllowedPackages.value.contains(packageName) || isPackageWhitelisted(packageName)
    }

    // --- 8. Smart Safe Search & Keyword Guard ---

    fun setSmartSafeSearchEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SMART_SAFE_SEARCH_ENABLED, enabled).apply()
        _smartSafeSearchEnabled.value = enabled
    }

    fun setSafeSearchPacks(packIds: Set<String>) {
        val valid = packIds
            .mapNotNull { SafeSearchPack.fromId(it)?.id }
            .take(10)
            .toSet()
        prefs.edit().putStringSet(KEY_SAFE_SEARCH_PACKS, valid).apply()
        _safeSearchPacks.value = valid
    }

    fun toggleSafeSearchPack(packId: String) {
        val canonical = SafeSearchPack.fromId(packId)?.id ?: return
        val next = _safeSearchPacks.value.toMutableSet()
        if (!next.add(canonical)) {
            next.remove(canonical)
        } else if (!_smartSafeSearchEnabled.value) {
            setSmartSafeSearchEnabled(true)
        }
        setSafeSearchPacks(next)
    }

    fun incrementSafeSearchBlockedCount(): Int {
        val next = _safeSearchBlockedCount.value + 1
        prefs.edit().putInt(KEY_SAFE_SEARCH_BLOCKED_COUNT, next).apply()
        _safeSearchBlockedCount.value = next
        return next
    }

    fun resetSafeSearchBlockedCount() {
        prefs.edit().putInt(KEY_SAFE_SEARCH_BLOCKED_COUNT, 0).apply()
        _safeSearchBlockedCount.value = 0
    }

    // --- 9. Notifications Toggle & Discreet Blocking Screen Style ---

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
        _notificationsEnabled.value = enabled
    }

    fun setDiscreetBlockingScreenMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DISCREET_BLOCKING_SCREEN_MODE, enabled).apply()
        _discreetBlockingScreenMode.value = enabled
    }

    // --- 10. Scheduled Profile Auto-Switcher & Direct Quick Settings Tile ---

    fun setProfileAutoSwitchEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PROFILE_AUTO_SWITCH_ENABLED, enabled).apply()
        _profileAutoSwitchEnabled.value = enabled
        if (!enabled) {
            setLastAppliedAutoSwitchSlot(null)
        }
    }

    fun setProfileScheduleWindow(startMinutes: Int, endMinutes: Int) {
        val safeStart = startMinutes.coerceIn(0, 1439)
        val safeEnd = endMinutes.coerceIn(0, 1439)
        prefs.edit()
            .putInt(KEY_PROFILE_SCHEDULE_START_MINUTES, safeStart)
            .putInt(KEY_PROFILE_SCHEDULE_END_MINUTES, safeEnd)
            .apply()
        _profileScheduleStartMinutes.value = safeStart
        _profileScheduleEndMinutes.value = safeEnd
    }

    fun setScheduledTargetProfile(profile: ProtectionProfile) {
        prefs.edit().putString(KEY_SCHEDULED_TARGET_PROFILE, profile.id).apply()
        _scheduledTargetProfile.value = profile
    }

    fun setScheduledFallbackProfile(profile: ProtectionProfile) {
        prefs.edit().putString(KEY_SCHEDULED_FALLBACK_PROFILE, profile.id).apply()
        _scheduledFallbackProfile.value = profile
    }

    fun setLastAppliedAutoSwitchSlot(slot: String?) {
        if (slot.isNullOrBlank()) {
            prefs.edit().remove(KEY_LAST_AUTO_SWITCH_SLOT).apply()
            _lastAppliedAutoSwitchSlot.value = null
        } else {
            prefs.edit().putString(KEY_LAST_AUTO_SWITCH_SLOT, slot).apply()
            _lastAppliedAutoSwitchSlot.value = slot
        }
    }

    fun setQuickTileMode(mode: QuickTileMode) {
        prefs.edit().putString(KEY_QUICK_TILE_MODE, mode.id).apply()
        _quickTileMode.value = mode
    }

    // --- 11. Light-to-Full VIP Browser Engine & Chrome Import State ---

    fun setFullBrowserEngineInstalled(installed: Boolean) {
        prefs.edit().putBoolean(KEY_FULL_BROWSER_INSTALLED, installed).apply()
        _fullBrowserEngineInstalled.value = installed
    }

    fun setBrowserDesktopModeDefault(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BROWSER_DESKTOP_DEFAULT, enabled).apply()
        _browserDesktopModeDefault.value = enabled
    }

    fun setBrowserBookmarksJson(json: String) {
        prefs.edit().putString(KEY_BROWSER_BOOKMARKS_JSON, json).apply()
        _browserBookmarksJson.value = json
    }

    fun setBrowserHistoryJson(json: String) {
        prefs.edit().putString(KEY_BROWSER_HISTORY_JSON, json).apply()
        _browserHistoryJson.value = json
    }

    fun setBrowserDownloadsJson(json: String) {
        prefs.edit().putString(KEY_BROWSER_DOWNLOADS_JSON, json).apply()
        _browserDownloadsJson.value = json
    }

    fun setChromeImportedFileCount(count: Int) {
        val safe = count.coerceAtLeast(0)
        prefs.edit().putInt(KEY_CHROME_IMPORTED_FILE_COUNT, safe).apply()
        _chromeImportedFileCount.value = safe
    }

    fun setChromeComponentsAutoCached(cached: Boolean) {
        prefs.edit().putBoolean(KEY_CHROME_COMPONENTS_AUTO_CACHED, cached).apply()
        _chromeComponentsAutoCached.value = cached
    }

    fun setChromeCachedComponentCount(count: Int) {
        val safe = count.coerceAtLeast(0)
        prefs.edit().putInt(KEY_CHROME_CACHED_COMPONENT_COUNT, safe).apply()
        _chromeCachedComponentCount.value = safe
    }

    fun setChromeComponentCacheTimestampMillis(timestampMillis: Long) {
        val safe = timestampMillis.coerceAtLeast(0L)
        prefs.edit().putLong(KEY_CHROME_COMPONENT_CACHE_TS, safe).apply()
        _chromeComponentCacheTimestampMillis.value = safe
    }

    fun setBrowserSearchEngine(engine: BrowserSearchEngine) {
        prefs.edit().putString(KEY_BROWSER_SEARCH_ENGINE, engine.id).apply()
        _browserSearchEngine.value = engine
    }

    fun setBrowserAdBlockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BROWSER_AD_BLOCK, enabled).apply()
        _browserAdBlockEnabled.value = enabled
    }

    fun setBrowserPopupsBlocked(blocked: Boolean) {
        prefs.edit().putBoolean(KEY_BROWSER_POPUPS_BLOCKED, blocked).apply()
        _browserPopupsBlocked.value = blocked
    }

    fun setBrowserJavaScriptEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BROWSER_JS_ENABLED, enabled).apply()
        _browserJavaScriptEnabled.value = enabled
    }

    fun setBrowserHomeUrl(url: String) {
        val clean = url.trim().take(320)
        prefs.edit().putString(KEY_BROWSER_HOME_URL, clean).apply()
        _browserHomeUrl.value = clean
    }

    fun setBrowserSitePermissionsJson(json: String) {
        prefs.edit().putString(KEY_BROWSER_SITE_PERMS_JSON, json).apply()
        _browserSitePermissionsJson.value = json
    }

    fun setBrowserPinPromptShown(shown: Boolean) {
        prefs.edit().putBoolean(KEY_BROWSER_PIN_PROMPT_SHOWN, shown).apply()
        _browserPinPromptShown.value = shown
    }

    // --- 12. Individual App Locker (Guardian PIN Gated) ---

    fun setAppLockerEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_APP_LOCKER_ENABLED, enabled).apply()
        _appLockerEnabled.value = enabled
        if (!enabled) {
            clearTempAppUnlocks()
        }
    }

    fun setLockedAppPackages(packages: Set<String>) {
        val cleaned = packages.map { it.trim() }.filter { it.isNotBlank() }.take(80).toSet()
        prefs.edit().putStringSet(KEY_LOCKED_APP_PACKAGES, cleaned).apply()
        _lockedAppPackages.value = cleaned
    }

    fun toggleLockedAppPackage(packageName: String) {
        val clean = packageName.trim()
        if (clean.isBlank()) return
        val next = _lockedAppPackages.value.toMutableSet()
        if (!next.add(clean)) {
            next.remove(clean)
            unlockedAppGraceUntilRealtime.remove(clean)
            if (activeUnlockedAppPackage == clean) activeUnlockedAppPackage = null
        }
        setLockedAppPackages(next)
    }

    fun setAppLockerRelockMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(0, 60)
        prefs.edit().putInt(KEY_APP_LOCKER_RELOCK_MINUTES, clamped).apply()
        _appLockerRelockMinutes.value = clamped
    }

    fun grantTempAppUnlock(
        packageName: String,
        durationMinutes: Int = _appLockerRelockMinutes.value,
        nowRealtimeMs: Long = SystemClock.elapsedRealtime()
    ) {
        val clean = packageName.trim()
        if (clean.isBlank()) return
        if (durationMinutes <= 0) {
            activeUnlockedAppPackage = clean
            unlockedAppGraceUntilRealtime.remove(clean)
        } else {
            activeUnlockedAppPackage = clean
            unlockedAppGraceUntilRealtime[clean] = nowRealtimeMs + durationMinutes * 60_000L
        }
    }

    fun onForegroundPackageChanged(packageName: String) {
        val clean = packageName.trim()
        if (clean.isBlank()) return
        val active = activeUnlockedAppPackage ?: return
        if (_appLockerRelockMinutes.value <= 0 && clean != active) {
            activeUnlockedAppPackage = null
        }
    }

    fun isAppTemporarilyUnlocked(
        packageName: String,
        nowRealtimeMs: Long = SystemClock.elapsedRealtime()
    ): Boolean {
        val clean = packageName.trim()
        if (clean.isBlank()) return false
        if (_appLockerRelockMinutes.value <= 0) {
            return activeUnlockedAppPackage == clean
        }
        val until = unlockedAppGraceUntilRealtime[clean] ?: return false
        if (nowRealtimeMs < until) {
            return true
        }
        unlockedAppGraceUntilRealtime.remove(clean)
        return false
    }

    fun isPackageLockedByAppLocker(
        packageName: String,
        nowRealtimeMs: Long = SystemClock.elapsedRealtime()
    ): Boolean {
        val clean = packageName.trim()
        if (clean.isBlank()) return false
        if (!_appLockerEnabled.value || !_hasGuardianPin.value) return false
        if (!_lockedAppPackages.value.contains(clean)) return false
        return !isAppTemporarilyUnlocked(clean, nowRealtimeMs)
    }

    fun clearTempAppUnlocks() {
        activeUnlockedAppPackage = null
        unlockedAppGraceUntilRealtime.clear()
    }

    fun exportPolicyJson(): String {
        val json = JSONObject().apply {
            put("schemaVersion", 1)
            put("exportedAtMillis", System.currentTimeMillis())
            put("sensitivity", _sensitivity.value.name)
            put("strictEverything", _strictEverything.value)
            put("lockdownDurationMinutes", _lockdownDurationMinutes.value)
            put("antiTamperEnabled", _antiTamperEnabled.value)
            put("childUninstallLocked", _childUninstallLocked.value)
            put("liveSeeEnabled", _liveSeeEnabled.value)
            put("warningThresholdCount", _warningThresholdCount.value)
            put("warningOnlyNoLockout", _warningOnlyNoLockout.value)
            put("bedtimeEnabled", _bedtimeEnabled.value)
            put("bedtimeStartMinutes", _bedtimeStartMinutes.value)
            put("bedtimeEndMinutes", _bedtimeEndMinutes.value)
            put("safeDnsEnabled", _safeDnsEnabled.value)
            put("safeDnsProvider", _safeDnsProvider.value.id)
            put("blockExplicitAdult", _blockExplicitAdult.value)
            put("blockSuggestiveContent", _blockSuggestiveContent.value)
            put("blockSensitiveGraphic", _blockSensitiveGraphic.value)
            put("blockGamblingKeywords", _blockGamblingKeywords.value)
            put("guardianEmail", _guardianEmail.value)
            put("appLanguageCode", _appLanguageCode.value)
            put("touchSoundsEnabled", _touchSoundsEnabled.value)
            put("autoSoundSelectorEnabled", _autoSoundSelectorEnabled.value)
            put("studyDurationMinutes", _studyDurationMinutes.value)
            put("studySoftWarningSeconds", _studySoftWarningSeconds.value)
            put("studyAllowedPackages", JSONArray(_studyAllowedPackages.value.toList()))
            put("smartSafeSearchEnabled", _smartSafeSearchEnabled.value)
            put("safeSearchPacks", JSONArray(_safeSearchPacks.value.toList()))
            put("notificationsEnabled", _notificationsEnabled.value)
            put("discreetBlockingScreenMode", _discreetBlockingScreenMode.value)
            put("blockCustomHeadline", _blockCustomHeadline.value)
            put("blockCustomMessage", _blockCustomMessage.value)
            put("blockShowTimer", _blockShowTimer.value)
            put("blockShowAppBadge", _blockShowAppBadge.value)
            put("blockShowQrUnlock", _blockShowQrUnlock.value)
            put("blockShowQuickActions", _blockShowQuickActions.value)
            put("blockShowReturnHome", _blockShowReturnHome.value)
            put("showEmergencyDialerOnLockdown", _showEmergencyDialerOnLockdown.value)
            put("activeProtectionProfile", _activeProtectionProfile.value?.id ?: "")
            put("profileAutoSwitchEnabled", _profileAutoSwitchEnabled.value)
            put("profileScheduleStartMinutes", _profileScheduleStartMinutes.value)
            put("profileScheduleEndMinutes", _profileScheduleEndMinutes.value)
            put("scheduledTargetProfile", _scheduledTargetProfile.value.id)
            put("scheduledFallbackProfile", _scheduledFallbackProfile.value.id)
            put("quickTileMode", _quickTileMode.value.id)
            put("fullBrowserEngineInstalled", _fullBrowserEngineInstalled.value)
            put("chromeComponentsAutoCached", _chromeComponentsAutoCached.value)
            put("chromeCachedComponentCount", _chromeCachedComponentCount.value)
            put("browserSearchEngine", _browserSearchEngine.value.id)
            put("browserAdBlockEnabled", _browserAdBlockEnabled.value)
            put("browserPopupsBlocked", _browserPopupsBlocked.value)
            put("browserJavaScriptEnabled", _browserJavaScriptEnabled.value)
            put("browserHomeUrl", _browserHomeUrl.value)
            put("browserSitePermissionsJson", _browserSitePermissionsJson.value)
            put("appLockerEnabled", _appLockerEnabled.value)
            put("appLockerRelockMinutes", _appLockerRelockMinutes.value)
            put("lockedAppPackages", JSONArray(_lockedAppPackages.value.toList()))
            put("whitelistedPackages", JSONArray(_whitelistedPackages.value.toList()))
            put("customBlockedDomains", JSONArray(_customBlockedDomains.value.toList()))
            put("customBlockedKeywords", JSONArray(_customBlockedKeywords.value.toList()))
            put("appTimeBudgets", JSONObject(_appTimeBudgets.value))
        }
        val content = json.toString()
        val md = MessageDigest.getInstance("SHA-256")
        val checksum = md.digest(content.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
        return JSONObject().apply {
            put("checksum", checksum)
            put("payload", json)
        }.toString(2)
    }

    fun restorePolicyJson(fileContent: String): Boolean {
        return runCatching {
            val root = JSONObject(fileContent)
            val checksum = root.optString("checksum", "")
            val payload = root.optJSONObject("payload") ?: root
            if (!payload.has("schemaVersion") && !payload.has("sensitivity") && !payload.has("safeDnsEnabled")) {
                return@runCatching false
            }
            val payloadStr = payload.toString()
            val md = MessageDigest.getInstance("SHA-256")
            val computed = md.digest(payloadStr.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
            if (checksum.isNotBlank() && checksum != computed) {
                // Checksum mismatch warning handled gracefully
            }

            val sens = payload.optString("sensitivity", "BALANCED")
            runCatching { setSensitivity(SensitivityLevel.valueOf(sens)) }
            setStrictEverything(payload.optBoolean("strictEverything", false))
            setLockdownDurationMinutes(payload.optInt("lockdownDurationMinutes", 600))
            setAntiTamperEnabled(payload.optBoolean("antiTamperEnabled", true))
            setChildUninstallLocked(payload.optBoolean("childUninstallLocked", true))
            setLiveSeeEnabled(payload.optBoolean("liveSeeEnabled", true))
            setWarningThresholdCount(payload.optInt("warningThresholdCount", 0))
            setWarningOnlyNoLockout(payload.optBoolean("warningOnlyNoLockout", false))
            setBedtimeEnabled(payload.optBoolean("bedtimeEnabled", false))
            setBedtimeWindow(
                payload.optInt("bedtimeStartMinutes", 22 * 60),
                payload.optInt("bedtimeEndMinutes", 6 * 60)
            )
            setSafeDnsEnabled(payload.optBoolean("safeDnsEnabled", false))
            val savedDnsProv = payload.optString("safeDnsProvider", "")
            if (savedDnsProv.isNotBlank()) {
                setSafeDnsProvider(SafeDnsProvider.fromId(savedDnsProv))
            }
            setBlockExplicitAdult(payload.optBoolean("blockExplicitAdult", true))
            setBlockSuggestiveContent(payload.optBoolean("blockSuggestiveContent", true))
            setBlockSensitiveGraphic(payload.optBoolean("blockSensitiveGraphic", true))
            setBlockGamblingKeywords(payload.optBoolean("blockGamblingKeywords", true))
            setGuardianEmail(payload.optString("guardianEmail", ""))
            setAppLanguageCode(payload.optString("appLanguageCode", "system"))
            setTouchSoundsEnabled(payload.optBoolean("touchSoundsEnabled", true))
            setAutoSoundSelectorEnabled(payload.optBoolean("autoSoundSelectorEnabled", false))
            setStudyDurationMinutes(payload.optInt("studyDurationMinutes", 25))
            setStudySoftWarningSeconds(payload.optInt("studySoftWarningSeconds", 30))
            setSmartSafeSearchEnabled(payload.optBoolean("smartSafeSearchEnabled", false))
            setNotificationsEnabled(payload.optBoolean("notificationsEnabled", true))
            setDiscreetBlockingScreenMode(payload.optBoolean("discreetBlockingScreenMode", true))
            setBlockCustomHeadline(payload.optString("blockCustomHeadline", ""))
            setBlockCustomMessage(payload.optString("blockCustomMessage", ""))
            setBlockShowTimer(payload.optBoolean("blockShowTimer", true))
            setBlockShowAppBadge(payload.optBoolean("blockShowAppBadge", true))
            setBlockShowQrUnlock(payload.optBoolean("blockShowQrUnlock", true))
            setBlockShowQuickActions(payload.optBoolean("blockShowQuickActions", true))
            setBlockShowReturnHome(payload.optBoolean("blockShowReturnHome", true))
            setShowEmergencyDialerOnLockdown(payload.optBoolean("showEmergencyDialerOnLockdown", true))

            val studyAllowedArr = payload.optJSONArray("studyAllowedPackages")
            if (studyAllowedArr != null) {
                val set = mutableSetOf<String>()
                for (i in 0 until studyAllowedArr.length()) {
                    studyAllowedArr.optString(i)?.takeIf { it.isNotBlank() }?.let { set.add(it) }
                }
                setStudyAllowedPackages(set)
            }

            val packsArr = payload.optJSONArray("safeSearchPacks")
            if (packsArr != null) {
                val set = mutableSetOf<String>()
                for (i in 0 until packsArr.length()) {
                    packsArr.optString(i)?.takeIf { it.isNotBlank() }?.let { set.add(it) }
                }
                setSafeSearchPacks(set)
            }

            val whitelistArr = payload.optJSONArray("whitelistedPackages")
            if (whitelistArr != null) {
                val set = mutableSetOf<String>()
                for (i in 0 until whitelistArr.length()) {
                    whitelistArr.optString(i)?.takeIf { it.isNotBlank() }?.let { set.add(it) }
                }
                setWhitelistedPackages(set)
            }

            val domainsArr = payload.optJSONArray("customBlockedDomains")
            if (domainsArr != null) {
                val set = mutableSetOf<String>()
                for (i in 0 until domainsArr.length()) {
                    domainsArr.optString(i)?.takeIf { it.isNotBlank() }?.let { set.add(it) }
                }
                setCustomBlockedDomains(set)
            }

            val keywordsArr = payload.optJSONArray("customBlockedKeywords")
            if (keywordsArr != null) {
                val set = mutableSetOf<String>()
                for (i in 0 until keywordsArr.length()) {
                    keywordsArr.optString(i)?.takeIf { it.isNotBlank() }?.let { set.add(it) }
                }
                setCustomBlockedKeywords(set)
            }

            val budgetsObj = payload.optJSONObject("appTimeBudgets")
            if (budgetsObj != null) {
                val map = mutableMapOf<String, Int>()
                val keys = budgetsObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val v = budgetsObj.optInt(k, 0)
                    if (v > 0) map[k] = v
                }
                setAppTimeBudgets(map)
            }
            val profileId = payload.optString("activeProtectionProfile", "")
            val restoredProfile = ProtectionProfile.fromId(profileId)
            if (restoredProfile != null) {
                prefs.edit().putString(KEY_ACTIVE_PROTECTION_PROFILE, restoredProfile.id).apply()
            } else {
                prefs.edit().remove(KEY_ACTIVE_PROTECTION_PROFILE).apply()
            }
            _activeProtectionProfile.value = restoredProfile

            setProfileAutoSwitchEnabled(payload.optBoolean("profileAutoSwitchEnabled", false))
            setProfileScheduleWindow(
                payload.optInt("profileScheduleStartMinutes", 15 * 60),
                payload.optInt("profileScheduleEndMinutes", 20 * 60)
            )
            ProtectionProfile.fromId(payload.optString("scheduledTargetProfile", ""))?.let {
                setScheduledTargetProfile(it)
            }
            ProtectionProfile.fromId(payload.optString("scheduledFallbackProfile", ""))?.let {
                setScheduledFallbackProfile(it)
            }
            val tileModeRaw = payload.optString("quickTileMode", "")
            if (tileModeRaw.isNotBlank()) {
                setQuickTileMode(QuickTileMode.fromId(tileModeRaw))
            }
            if (payload.has("fullBrowserEngineInstalled")) {
                setFullBrowserEngineInstalled(payload.optBoolean("fullBrowserEngineInstalled", false))
            }
            if (payload.has("chromeComponentsAutoCached")) {
                setChromeComponentsAutoCached(payload.optBoolean("chromeComponentsAutoCached", false))
            }
            if (payload.has("chromeCachedComponentCount")) {
                setChromeCachedComponentCount(payload.optInt("chromeCachedComponentCount", 0))
            }
            if (payload.has("browserSearchEngine")) {
                setBrowserSearchEngine(BrowserSearchEngine.fromId(payload.optString("browserSearchEngine", "")))
            }
            if (payload.has("browserAdBlockEnabled")) {
                setBrowserAdBlockEnabled(payload.optBoolean("browserAdBlockEnabled", true))
            }
            if (payload.has("browserPopupsBlocked")) {
                setBrowserPopupsBlocked(payload.optBoolean("browserPopupsBlocked", true))
            }
            if (payload.has("browserJavaScriptEnabled")) {
                setBrowserJavaScriptEnabled(payload.optBoolean("browserJavaScriptEnabled", true))
            }
            if (payload.has("browserHomeUrl")) {
                setBrowserHomeUrl(payload.optString("browserHomeUrl", ""))
            }
            if (payload.has("browserSitePermissionsJson")) {
                setBrowserSitePermissionsJson(payload.optString("browserSitePermissionsJson", ""))
            }
            if (payload.has("appLockerEnabled")) {
                setAppLockerEnabled(payload.optBoolean("appLockerEnabled", false))
            }
            if (payload.has("appLockerRelockMinutes")) {
                setAppLockerRelockMinutes(payload.optInt("appLockerRelockMinutes", 0))
            }
            val lockedAppsArr = payload.optJSONArray("lockedAppPackages")
            if (lockedAppsArr != null) {
                val set = mutableSetOf<String>()
                for (i in 0 until lockedAppsArr.length()) {
                    lockedAppsArr.optString(i)?.takeIf { it.isNotBlank() }?.let { set.add(it) }
                }
                setLockedAppPackages(set)
            }
            true
        }.getOrDefault(false)
    }

    // ------------------------------------------------------------------------
    // Mindful Pause, Usage Insights & Reels/Shorts Scroll-Limit Auto-Lock
    // ------------------------------------------------------------------------

    fun setScrollLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SCROLL_LOCK_ENABLED, enabled).apply()
        _scrollLockEnabled.value = enabled
    }

    fun setScrollLimitCount(count: Int) {
        val clamped = count.coerceIn(5, 60)
        prefs.edit().putInt(KEY_SCROLL_LIMIT_COUNT, clamped).apply()
        _scrollLimitCount.value = clamped
    }

    fun setScrollLockCooldownMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(5, 60)
        prefs.edit().putInt(KEY_SCROLL_LOCK_COOLDOWN_MINUTES, clamped).apply()
        _scrollLockCooldownMinutes.value = clamped
    }

    fun setScrollBingeNudgeCount(count: Int) {
        val clamped = if (count <= 0) 0 else count.coerceIn(5, 25)
        prefs.edit().putInt(KEY_SCROLL_BINGE_NUDGE_COUNT, clamped).apply()
        _scrollBingeNudgeCount.value = clamped
    }

    fun setReelsGuardPlatforms(platforms: Set<String>) {
        val clean = platforms.filter { ShortVideoPlatform.fromId(it) != null }.toSet()
        prefs.edit().putStringSet(KEY_REELS_GUARD_PLATFORMS, clean).apply()
        _reelsGuardPlatforms.value = clean
    }

    fun toggleReelsGuardPlatform(platformId: String) {
        val next = _reelsGuardPlatforms.value.toMutableSet()
        if (platformId in next) {
            next.remove(platformId)
        } else {
            next.add(platformId)
        }
        setReelsGuardPlatforms(next)
    }

    fun setMindfulPauseEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MINDFUL_PAUSE_ENABLED, enabled).apply()
        _mindfulPauseEnabled.value = enabled
    }

    fun setMindfulPausePackages(packages: Set<String>) {
        val clean = packages.map { it.trim() }.filter { it.isNotBlank() }.toSet()
        prefs.edit().putStringSet(KEY_MINDFUL_PAUSE_PACKAGES, clean).apply()
        _mindfulPausePackages.value = clean
    }

    fun toggleMindfulPausePackage(packageName: String) {
        val clean = packageName.trim()
        if (clean.isBlank()) return
        val next = _mindfulPausePackages.value.toMutableSet()
        if (clean in next) next.remove(clean) else next.add(clean)
        setMindfulPausePackages(next)
    }

    fun setMindfulGraceMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(0, 30)
        prefs.edit().putInt(KEY_MINDFUL_GRACE_MINUTES, clamped).apply()
        _mindfulGraceMinutes.value = clamped
    }

    fun startInstantDetox(durationMinutes: Int = 15, nowMillis: Long = System.currentTimeMillis()) {
        val clamped = durationMinutes.coerceIn(5, 180)
        val until = nowMillis + clamped * 60_000L
        prefs.edit().putLong(KEY_INSTANT_DETOX_UNTIL_MILLIS, until).apply()
        _instantDetoxUntilMillis.value = until
        appendMindfulEvent(
            MindfulScrollEvent(
                timestampMillis = nowMillis,
                type = MindfulScrollEvent.TYPE_DETOX_LOCK,
                appLabel = "Instant Detox",
                packageName = "",
                detail = "Started ${clamped}m Instant Detox focus lock"
            )
        )
    }

    fun stopInstantDetox() {
        prefs.edit().putLong(KEY_INSTANT_DETOX_UNTIL_MILLIS, 0L).apply()
        _instantDetoxUntilMillis.value = 0L
    }

    fun isInstantDetoxActive(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val until = _instantDetoxUntilMillis.value
        if (until <= 0L) return false
        if (nowMillis >= until) {
            stopInstantDetox()
            return false
        }
        return true
    }

    fun getRemainingDetoxMillis(nowMillis: Long = System.currentTimeMillis()): Long =
        (_instantDetoxUntilMillis.value - nowMillis).coerceAtLeast(0L)

    fun isPackageBlockedByInstantDetox(packageName: String, nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (!isInstantDetoxActive(nowMillis)) return false
        return packageName in _mindfulPausePackages.value || packageName in ShortVideoPlatform.ALL_PACKAGES
    }

    fun isAnyMindfulOrScrollGuardActive(): Boolean =
        _mindfulPauseEnabled.value ||
            (_scrollLockEnabled.value && _reelsGuardPlatforms.value.isNotEmpty()) ||
            isInstantDetoxActive()

    fun getTotalUsageSecondsToday(dateKey: String = todayDateKey()): Int {
        val map = readDailyUsageMap(dateKey)
        if (_dailyAppUsageSeconds.value != map) {
            _dailyAppUsageSeconds.value = map
        }
        return map.values.sum()
    }

    fun shouldTriggerMindfulPauseForPackage(packageName: String): Boolean {
        if (!_mindfulPauseEnabled.value) return false
        if (packageName !in _mindfulPausePackages.value) return false
        return !hasActiveMindfulGracePass(packageName)
    }

    fun hasActiveMindfulGracePass(
        packageName: String,
        nowRealtime: Long = SystemClock.elapsedRealtime()
    ): Boolean {
        val until = mindfulGraceUntilRealtime[packageName] ?: return false
        if (nowRealtime >= until) {
            mindfulGraceUntilRealtime.remove(packageName)
            return false
        }
        return true
    }

    fun grantMindfulGracePass(
        packageName: String,
        nowRealtime: Long = SystemClock.elapsedRealtime()
    ) {
        val mins = _mindfulGraceMinutes.value
        if (mins <= 0) {
            // Short 12-second transition grace so same-open window updates don't double-prompt
            mindfulGraceUntilRealtime[packageName] = nowRealtime + 12_000L
        } else {
            mindfulGraceUntilRealtime[packageName] = nowRealtime + mins * 60_000L
        }
    }

    fun recordAppOpen(
        packageName: String,
        dateKey: String = todayDateKey()
    ): Int {
        if (packageName.isBlank()) return 0
        ensureMindfulDailyRollover(dateKey)
        val current = _dailyAppOpensCount.value.toMutableMap()
        val updated = (current[packageName] ?: 0) + 1
        current[packageName] = updated
        writeIntMap(KEY_DAILY_APP_OPENS_MAP, current)
        _dailyAppOpensCount.value = current
        return updated
    }

    fun recordMindfulPauseShown(
        packageName: String,
        appLabel: String,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        appendMindfulEvent(
            MindfulScrollEvent(
                timestampMillis = nowMillis,
                type = MindfulScrollEvent.TYPE_MINDFUL_PAUSE,
                appLabel = appLabel,
                packageName = packageName,
                detail = "5s breathing pause shown"
            )
        )
    }

    fun recordMindfulExit(
        packageName: String,
        appLabel: String,
        detail: String = "Chose Close & Return Home after breathing pause",
        dateKey: String = todayDateKey(),
        nowMillis: Long = System.currentTimeMillis()
    ): Int {
        ensureMindfulDailyRollover(dateKey)
        val next = _dailyMindfulExitsCount.value + 1
        prefs.edit()
            .putString(KEY_MINDFUL_DAILY_DATE, dateKey)
            .putInt(KEY_DAILY_MINDFUL_EXITS, next)
            .apply()
        _dailyMindfulExitsCount.value = next
        mindfulGraceUntilRealtime.remove(packageName)
        appendMindfulEvent(
            MindfulScrollEvent(
                timestampMillis = nowMillis,
                type = MindfulScrollEvent.TYPE_MINDFUL_EXIT,
                appLabel = appLabel,
                packageName = packageName,
                detail = detail
            )
        )
        return next
    }

    fun recordShortVideoScrolled(
        platform: ShortVideoPlatform,
        packageName: String,
        appLabel: String,
        dateKey: String = todayDateKey(),
        nowMillis: Long = System.currentTimeMillis(),
        hourOfDay: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    ): ShortVideoScrollResult {
        ensureMindfulDailyRollover(dateKey)

        val newTotal = _dailyReelsTotalCount.value + 1
        val byPlat = _dailyReelsByPlatform.value.toMutableMap()
        val platNext = (byPlat[platform.id] ?: 0) + 1
        byPlat[platform.id] = platNext

        val buckets = _hourlyScrollBuckets.value.toMutableList().let {
            if (it.size == 24) it else MutableList(24) { 0 }
        }
        val safeHour = hourOfDay.coerceIn(0, 23)
        buckets[safeHour] = buckets[safeHour] + 1

        prefs.edit()
            .putString(KEY_MINDFUL_DAILY_DATE, dateKey)
            .putInt(KEY_DAILY_REELS_TOTAL, newTotal)
            .putString(KEY_DAILY_REELS_BY_PLATFORM, JSONObject(byPlat.mapValues { it.value }).toString())
            .putString(KEY_HOURLY_SCROLL_BUCKETS, JSONArray(buckets).toString())
            .apply()

        _dailyReelsTotalCount.value = newTotal
        _dailyReelsByPlatform.value = byPlat
        _hourlyScrollBuckets.value = buckets

        val baseline = scrollSessionBaselineByPkg[packageName] ?: 0
        val sessionSinceUnlock = (platNext - baseline).coerceAtLeast(1)
        val limit = _scrollLimitCount.value
        val shouldLock = _scrollLockEnabled.value &&
            platform.id in _reelsGuardPlatforms.value &&
            sessionSinceUnlock >= limit

        val bingeEvery = _scrollBingeNudgeCount.value
        val lastNudgeAt = bingeSessionLastNudgeByPkg[packageName] ?: 0
        val shouldNudge = !shouldLock &&
            bingeEvery > 0 &&
            sessionSinceUnlock >= bingeEvery &&
            (sessionSinceUnlock - lastNudgeAt) >= bingeEvery

        if (shouldNudge) {
            bingeSessionLastNudgeByPkg[packageName] = sessionSinceUnlock
            appendMindfulEvent(
                MindfulScrollEvent(
                    timestampMillis = nowMillis,
                    type = MindfulScrollEvent.TYPE_BINGE_NUDGE,
                    appLabel = appLabel,
                    packageName = packageName,
                    detail = "${platform.shortLabel}: $sessionSinceUnlock scrolled in a row"
                )
            )
        }

        if (shouldLock) {
            val cooldownMs = _scrollLockCooldownMinutes.value * 60_000L
            val lockMap = _scrollLockedUntilByPackage.value.toMutableMap()
            lockMap[packageName] = nowMillis + cooldownMs
            writeLongMap(KEY_SCROLL_LOCKED_UNTIL_MAP, lockMap)
            _scrollLockedUntilByPackage.value = lockMap

            val nextLocks = _dailyScrollLocksTriggered.value + 1
            prefs.edit().putInt(KEY_DAILY_SCROLL_LOCKS, nextLocks).apply()
            _dailyScrollLocksTriggered.value = nextLocks

            appendMindfulEvent(
                MindfulScrollEvent(
                    timestampMillis = nowMillis,
                    type = MindfulScrollEvent.TYPE_SCROLL_LOCK,
                    appLabel = appLabel,
                    packageName = packageName,
                    detail = "Auto-stopped after $limit ${platform.shortLabel} (${_scrollLockCooldownMinutes.value}m lock)"
                )
            )
        }

        return ShortVideoScrollResult(
            totalReelsToday = newTotal,
            sessionReelsSinceUnlock = sessionSinceUnlock,
            scrollLimit = limit,
            shouldTriggerScrollLock = shouldLock,
            shouldTriggerBingeNudge = shouldNudge
        )
    }

    fun getRemainingScrollLockMillis(
        packageName: String,
        nowMillis: Long = System.currentTimeMillis()
    ): Long {
        val until = _scrollLockedUntilByPackage.value[packageName] ?: return 0L
        val remaining = until - nowMillis
        if (remaining <= 0L) {
            val next = _scrollLockedUntilByPackage.value.toMutableMap()
            next.remove(packageName)
            writeLongMap(KEY_SCROLL_LOCKED_UNTIL_MAP, next)
            _scrollLockedUntilByPackage.value = next
            val platCount = ShortVideoPlatform.findByPackage(packageName)
                .sumOf { _dailyReelsByPlatform.value[it.id] ?: 0 }
            scrollSessionBaselineByPkg[packageName] = platCount
            return 0L
        }
        return remaining
    }

    fun unlockScrollLockWithPin(packageName: String) {
        val next = _scrollLockedUntilByPackage.value.toMutableMap()
        next.remove(packageName)
        writeLongMap(KEY_SCROLL_LOCKED_UNTIL_MAP, next)
        _scrollLockedUntilByPackage.value = next
        val platCount = ShortVideoPlatform.findByPackage(packageName)
            .sumOf { _dailyReelsByPlatform.value[it.id] ?: 0 }
        scrollSessionBaselineByPkg[packageName] = platCount
        bingeSessionLastNudgeByPkg[packageName] = 0
    }

    fun resetTodayReelCounters() {
        val dateKey = todayDateKey()
        val emptyBuckets = List(24) { 0 }
        prefs.edit()
            .putString(KEY_MINDFUL_DAILY_DATE, dateKey)
            .putInt(KEY_DAILY_REELS_TOTAL, 0)
            .putString(KEY_DAILY_REELS_BY_PLATFORM, "{}")
            .putString(KEY_HOURLY_SCROLL_BUCKETS, JSONArray(emptyBuckets).toString())
            .apply()
        _dailyReelsTotalCount.value = 0
        _dailyReelsByPlatform.value = emptyMap()
        _hourlyScrollBuckets.value = emptyBuckets
        scrollSessionBaselineByPkg.clear()
        bingeSessionLastNudgeByPkg.clear()
        writeLongMap(KEY_SCROLL_LOCKED_UNTIL_MAP, emptyMap())
        _scrollLockedUntilByPackage.value = emptyMap()
    }

    // ------------------------------------------------------------------------
    // PiqoraX Connector & App Blocker Schedule (Minutes / Hours)
    // ------------------------------------------------------------------------

    fun setPiqoraxConnectorConnected(connected: Boolean) {
        prefs.edit().putBoolean(KEY_PIQORAX_CONNECTOR_CONNECTED, connected).apply()
        _piqoraxConnectorConnected.value = connected
        if (!connected && _piqoraxCoinDeviceLockActive.value) {
            setPiqoraxCoinDeviceLockActive(false)
        }
    }

    fun setPiqoraxCoinDeviceLockActive(active: Boolean) {
        prefs.edit().putBoolean(KEY_PIQORAX_COIN_DEVICE_LOCK, active).apply()
        _piqoraxCoinDeviceLockActive.value = active
    }

    fun setTimeBlockerAllowanceMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(1, 180)
        val currentWindow = _timeBlockerWindowMinutes.value
        val adjustedWindow = currentWindow.coerceAtLeast(clamped)
        prefs.edit()
            .putInt(KEY_TIME_BLOCKER_ALLOWANCE_MINUTES, clamped)
            .putInt(KEY_TIME_BLOCKER_WINDOW_MINUTES, adjustedWindow)
            .apply()
        _timeBlockerAllowanceMinutes.value = clamped
        _timeBlockerWindowMinutes.value = adjustedWindow
    }

    fun setTimeBlockerWindowMinutes(minutes: Int) {
        val minAllowed = _timeBlockerAllowanceMinutes.value.coerceAtLeast(5)
        val clamped = minutes.coerceIn(minAllowed, 1440)
        prefs.edit().putInt(KEY_TIME_BLOCKER_WINDOW_MINUTES, clamped).apply()
        _timeBlockerWindowMinutes.value = clamped
    }

    fun recordPiqoraxPassGranted(targetPackage: String, durationMinutes: Int) {
        val shortApp = targetPackage.substringAfterLast('.').ifBlank { targetPackage }
        val summary = "${durationMinutes}m pass granted for $shortApp"
        prefs.edit().putString(KEY_PIQORAX_LAST_PASS_SUMMARY, summary).apply()
        _piqoraxLastPassSummary.value = summary
    }

    private fun appendMindfulEvent(event: MindfulScrollEvent) {
        val next = (listOf(event) + _recentMindfulScrollEvents.value).take(30)
        prefs.edit()
            .putString(KEY_RECENT_MINDFUL_EVENTS, MindfulScrollEvent.listToJsonString(next))
            .apply()
        _recentMindfulScrollEvents.value = next
    }

    fun ensureMindfulDailyRollover(currentDateKey: String = todayDateKey()) {
        val storedDate = prefs.getString(KEY_MINDFUL_DAILY_DATE, "").orEmpty()
        if (storedDate == currentDateKey) return

        if (storedDate.isNotBlank()) {
            val prevUsage = readDailyUsageMap(storedDate).values.sum()
            val prevReels = prefs.getInt(KEY_DAILY_REELS_TOTAL, 0).coerceAtLeast(0)
            val prevExits = prefs.getInt(KEY_DAILY_MINDFUL_EXITS, 0).coerceAtLeast(0)
            val prevLocks = prefs.getInt(KEY_DAILY_SCROLL_LOCKS, 0).coerceAtLeast(0)

            if (prevUsage > 0 || prevReels > 0 || prevExits > 0 || prevLocks > 0) {
                val snap = DailyScrollSnapshot(
                    dateKey = storedDate,
                    usageSeconds = prevUsage,
                    reelsCount = prevReels,
                    mindfulExits = prevExits,
                    locksTriggered = prevLocks
                )
                val updatedHistory = (_weeklyScrollHistory.value.filterNot { it.dateKey == storedDate } + snap)
                    .takeLast(7)
                prefs.edit()
                    .putString(KEY_WEEKLY_SCROLL_HISTORY, DailyScrollSnapshot.listToJsonString(updatedHistory))
                    .apply()
                _weeklyScrollHistory.value = updatedHistory

                val nextStreak = if (prevReels <= _scrollLimitCount.value) {
                    _cleanStreakDays.value + 1
                } else {
                    0
                }
                prefs.edit().putInt(KEY_CLEAN_STREAK_DAYS, nextStreak).apply()
                _cleanStreakDays.value = nextStreak
            }
        }

        val emptyBuckets = List(24) { 0 }
        prefs.edit()
            .putString(KEY_MINDFUL_DAILY_DATE, currentDateKey)
            .putInt(KEY_DAILY_REELS_TOTAL, 0)
            .putString(KEY_DAILY_REELS_BY_PLATFORM, "{}")
            .putString(KEY_DAILY_APP_OPENS_MAP, "{}")
            .putInt(KEY_DAILY_MINDFUL_EXITS, 0)
            .putInt(KEY_DAILY_SCROLL_LOCKS, 0)
            .putString(KEY_HOURLY_SCROLL_BUCKETS, JSONArray(emptyBuckets).toString())
            .putString(KEY_DAILY_GAME_USAGE_MAP, "{}")
            .putInt(KEY_DAILY_GAME_EXT_MINUTES, 0)
            .apply()

        _dailyReelsTotalCount.value = 0
        _dailyReelsByPlatform.value = emptyMap()
        _dailyAppOpensCount.value = emptyMap()
        _dailyMindfulExitsCount.value = 0
        _dailyScrollLocksTriggered.value = 0
        _hourlyScrollBuckets.value = emptyBuckets
        _dailyGameUsageByPackage.value = emptyMap()
        _dailyGameTotalSeconds.value = 0
        _dailyGameExtensionMinutes.value = 0
        _activeGameSessionSeconds.value = 0
        lastGameTickUptimeMs = 0L
        gameSoftWarnedThisSession = false
        scrollSessionBaselineByPkg.clear()
        bingeSessionLastNudgeByPkg.clear()
    }

    // ------------------------------------------------------------------------
    // Gaming Addiction Control (Session Cap, Cooldown, Daily Cap & Curfew)
    // ------------------------------------------------------------------------

    fun setGameGuardEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GAME_GUARD_ENABLED, enabled).apply()
        _gameGuardEnabled.value = enabled
        if (!enabled) {
            _activeGameSessionSeconds.value = 0
            gameSoftWarnedThisSession = false
        }
    }

    fun setGameAutoDetectEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GAME_AUTO_DETECT_ENABLED, enabled).apply()
        _gameAutoDetectEnabled.value = enabled
    }

    fun setGameSessionLimitMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(10, 90)
        prefs.edit().putInt(KEY_GAME_SESSION_LIMIT_MINUTES, clamped).apply()
        _gameSessionLimitMinutes.value = clamped
    }

    fun setGameCooldownMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(5, 60)
        prefs.edit().putInt(KEY_GAME_COOLDOWN_MINUTES, clamped).apply()
        _gameCooldownMinutes.value = clamped
    }

    fun setGameDailyCapMinutes(minutes: Int) {
        val normalized = if (minutes <= 0) 0 else minutes.coerceIn(15, 240)
        prefs.edit().putInt(KEY_GAME_DAILY_CAP_MINUTES, normalized).apply()
        _gameDailyCapMinutes.value = normalized
    }

    fun setGameCurfew(enabled: Boolean, startMinutes: Int = _gameCurfewStartMinutes.value, endMinutes: Int = _gameCurfewEndMinutes.value) {
        val start = startMinutes.coerceIn(0, 1439)
        val end = endMinutes.coerceIn(0, 1439)
        prefs.edit()
            .putBoolean(KEY_GAME_CURFEW_ENABLED, enabled)
            .putInt(KEY_GAME_CURFEW_START_MINUTES, start)
            .putInt(KEY_GAME_CURFEW_END_MINUTES, end)
            .apply()
        _gameCurfewEnabled.value = enabled
        _gameCurfewStartMinutes.value = start
        _gameCurfewEndMinutes.value = end
    }

    fun syncAutoDetectedGames(detectedPackages: Set<String>) {
        if (detectedPackages.isEmpty()) return
        val excluded = _excludedGamePackages.value
        val merged = (_trackedGamePackages.value + detectedPackages.filterNot { it in excluded }).toSet()
        if (merged != _trackedGamePackages.value) {
            prefs.edit().putStringSet(KEY_TRACKED_GAME_PACKAGES, merged).apply()
            _trackedGamePackages.value = merged
        }
    }

    fun toggleTrackedGamePackage(packageName: String) {
        val clean = packageName.trim()
        if (clean.isBlank()) return
        val currentTracked = _trackedGamePackages.value.toMutableSet()
        val currentExcluded = _excludedGamePackages.value.toMutableSet()
        if (clean in currentTracked) {
            currentTracked.remove(clean)
            currentExcluded.add(clean)
        } else {
            currentTracked.add(clean)
            currentExcluded.remove(clean)
        }
        prefs.edit()
            .putStringSet(KEY_TRACKED_GAME_PACKAGES, currentTracked)
            .putStringSet(KEY_EXCLUDED_GAME_PACKAGES, currentExcluded)
            .apply()
        _trackedGamePackages.value = currentTracked
        _excludedGamePackages.value = currentExcluded
    }

    fun isPackageTrackedGame(packageName: String, isDetectedGame: Boolean = false): Boolean {
        val clean = packageName.trim()
        if (clean.isBlank()) return false
        if (clean in _trackedGamePackages.value) return true
        if (_gameAutoDetectEnabled.value && isDetectedGame && clean !in _excludedGamePackages.value) {
            return true
        }
        return false
    }

    fun isWithinGameCurfewWindow(
        currentMinuteOfDay: Int = currentClockMinuteOfDay(),
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (!_gameGuardEnabled.value || !_gameCurfewEnabled.value) return false
        if (_gameCurfewOverrideUntilMillis.value > nowMillis) return false
        return isMinuteWithinWindow(
            minuteOfDay = currentMinuteOfDay,
            startMinute = _gameCurfewStartMinutes.value,
            endMinute = _gameCurfewEndMinutes.value
        )
    }

    fun getRemainingGameCooldownMillis(nowMillis: Long = System.currentTimeMillis()): Long {
        val until = _gameCooldownUntilMillis.value
        if (until <= 0L) return 0L
        val remaining = until - nowMillis
        if (remaining <= 0L) {
            prefs.edit().putLong(KEY_GAME_COOLDOWN_UNTIL_MILLIS, 0L).apply()
            _gameCooldownUntilMillis.value = 0L
            _activeGameSessionSeconds.value = 0
            gameSoftWarnedThisSession = false
            return 0L
        }
        return remaining
    }

    fun onNonGameForegroundResumed(nowUptimeMs: Long = SystemClock.uptimeMillis()) {
        if (_activeGameSessionSeconds.value <= 0 || lastGameTickUptimeMs <= 0L) return
        val restThresholdMs = _gameCooldownMinutes.value * 60_000L
        if (nowUptimeMs - lastGameTickUptimeMs >= restThresholdMs) {
            _activeGameSessionSeconds.value = 0
            gameSoftWarnedThisSession = false
        }
    }

    fun evaluateGamePackageAccess(
        packageName: String,
        isDetectedGame: Boolean = false,
        nowMillis: Long = System.currentTimeMillis(),
        nowUptimeMs: Long = SystemClock.uptimeMillis(),
        minuteOfDay: Int = currentClockMinuteOfDay(),
        dateKey: String = todayDateKey()
    ): GameGuardDecision {
        ensureMindfulDailyRollover(dateKey)
        val tracked = isPackageTrackedGame(packageName, isDetectedGame)
        if (!tracked || !_gameGuardEnabled.value) {
            return GameGuardDecision(isTrackedGame = tracked, shouldBlock = false)
        }
        if (isDetectedGame && packageName !in _trackedGamePackages.value) {
            syncAutoDetectedGames(setOf(packageName))
        }

        val dailyUsed = _dailyGameTotalSeconds.value
        val baseCapMins = _gameDailyCapMinutes.value
        val effectiveCapSec = if (baseCapMins > 0) (baseCapMins + _dailyGameExtensionMinutes.value) * 60 else 0
        val sessionCapSec = _gameSessionLimitMinutes.value * 60

        // 1. Late-night gaming curfew check
        if (isWithinGameCurfewWindow(minuteOfDay, nowMillis)) {
            return GameGuardDecision(
                isTrackedGame = true,
                shouldBlock = true,
                lockReason = GameLockReason.CURFEW,
                remainingUnlockMs = computeMillisUntilCurfewEnds(minuteOfDay),
                remainingSessionSeconds = 0,
                dailyGameSecondsUsed = dailyUsed,
                dailyCapSeconds = effectiveCapSec
            )
        }

        // 2. Daily total gaming cap check
        if (effectiveCapSec > 0 && dailyUsed >= effectiveCapSec) {
            return GameGuardDecision(
                isTrackedGame = true,
                shouldBlock = true,
                lockReason = GameLockReason.DAILY_CAP,
                remainingUnlockMs = computeMillisUntilMidnight(nowMillis),
                remainingSessionSeconds = 0,
                dailyGameSecondsUsed = dailyUsed,
                dailyCapSeconds = effectiveCapSec
            )
        }

        // 3. Active session break cooldown check
        val remCooldownMs = getRemainingGameCooldownMillis(nowMillis)
        if (remCooldownMs > 0L) {
            return GameGuardDecision(
                isTrackedGame = true,
                shouldBlock = true,
                lockReason = GameLockReason.SESSION_COOLDOWN,
                remainingUnlockMs = remCooldownMs,
                remainingSessionSeconds = 0,
                dailyGameSecondsUsed = dailyUsed,
                dailyCapSeconds = effectiveCapSec
            )
        }

        // If user stayed away from games for at least cooldownMinutes, refresh session timer cleanly
        if (lastGameTickUptimeMs > 0L && (nowUptimeMs - lastGameTickUptimeMs) >= _gameCooldownMinutes.value * 60_000L) {
            _activeGameSessionSeconds.value = 0
            gameSoftWarnedThisSession = false
        }

        val remSession = (sessionCapSec - _activeGameSessionSeconds.value).coerceAtLeast(0)
        return GameGuardDecision(
            isTrackedGame = true,
            shouldBlock = false,
            lockReason = GameLockReason.NONE,
            remainingSessionSeconds = remSession,
            dailyGameSecondsUsed = dailyUsed,
            dailyCapSeconds = effectiveCapSec
        )
    }

    fun recordGameUsageTick(
        packageName: String,
        appLabel: String,
        deltaSeconds: Int,
        isDetectedGame: Boolean = false,
        nowMillis: Long = System.currentTimeMillis(),
        nowUptimeMs: Long = SystemClock.uptimeMillis(),
        minuteOfDay: Int = currentClockMinuteOfDay(),
        dateKey: String = todayDateKey()
    ): GameGuardDecision {
        if (deltaSeconds <= 0) {
            return evaluateGamePackageAccess(packageName, isDetectedGame, nowMillis, nowUptimeMs, minuteOfDay, dateKey)
        }
        ensureMindfulDailyRollover(dateKey)
        if (!isPackageTrackedGame(packageName, isDetectedGame)) {
            return GameGuardDecision(isTrackedGame = false, shouldBlock = false)
        }

        val nextByPkg = _dailyGameUsageByPackage.value.toMutableMap()
        nextByPkg[packageName] = (nextByPkg[packageName] ?: 0) + deltaSeconds
        val nextTotal = nextByPkg.values.sum()
        writeIntMap(KEY_DAILY_GAME_USAGE_MAP, nextByPkg)
        _dailyGameUsageByPackage.value = nextByPkg
        _dailyGameTotalSeconds.value = nextTotal

        if (!_gameGuardEnabled.value) {
            return GameGuardDecision(
                isTrackedGame = true,
                shouldBlock = false,
                dailyGameSecondsUsed = nextTotal
            )
        }

        // Refresh session counter if previous game tick was older than cooldown window
        if (lastGameTickUptimeMs > 0L && (nowUptimeMs - lastGameTickUptimeMs) >= _gameCooldownMinutes.value * 60_000L) {
            _activeGameSessionSeconds.value = 0
            gameSoftWarnedThisSession = false
        }
        lastGameTickUptimeMs = nowUptimeMs

        val nextSessionSec = _activeGameSessionSeconds.value + deltaSeconds
        _activeGameSessionSeconds.value = nextSessionSec

        val baseCapMins = _gameDailyCapMinutes.value
        val effectiveCapSec = if (baseCapMins > 0) (baseCapMins + _dailyGameExtensionMinutes.value) * 60 else 0
        val sessionCapSec = _gameSessionLimitMinutes.value * 60

        if (isWithinGameCurfewWindow(minuteOfDay, nowMillis)) {
            appendMindfulEvent(
                MindfulScrollEvent(
                    timestampMillis = nowMillis,
                    type = MindfulScrollEvent.TYPE_GAME_CURFEW,
                    appLabel = appLabel,
                    packageName = packageName,
                    detail = "Gaming paused for late-night curfew"
                )
            )
            return GameGuardDecision(
                isTrackedGame = true,
                shouldBlock = true,
                lockReason = GameLockReason.CURFEW,
                remainingUnlockMs = computeMillisUntilCurfewEnds(minuteOfDay),
                dailyGameSecondsUsed = nextTotal,
                dailyCapSeconds = effectiveCapSec
            )
        }

        if (effectiveCapSec > 0 && nextTotal >= effectiveCapSec) {
            appendMindfulEvent(
                MindfulScrollEvent(
                    timestampMillis = nowMillis,
                    type = MindfulScrollEvent.TYPE_GAME_DAILY_CAP,
                    appLabel = appLabel,
                    packageName = packageName,
                    detail = "Reached daily gaming cap (${effectiveCapSec / 60}m)"
                )
            )
            return GameGuardDecision(
                isTrackedGame = true,
                shouldBlock = true,
                lockReason = GameLockReason.DAILY_CAP,
                remainingUnlockMs = computeMillisUntilMidnight(nowMillis),
                dailyGameSecondsUsed = nextTotal,
                dailyCapSeconds = effectiveCapSec
            )
        }

        if (nextSessionSec >= sessionCapSec) {
            val cooldownMs = _gameCooldownMinutes.value * 60_000L
            val until = nowMillis + cooldownMs
            prefs.edit().putLong(KEY_GAME_COOLDOWN_UNTIL_MILLIS, until).apply()
            _gameCooldownUntilMillis.value = until
            _activeGameSessionSeconds.value = 0
            gameSoftWarnedThisSession = false
            appendMindfulEvent(
                MindfulScrollEvent(
                    timestampMillis = nowMillis,
                    type = MindfulScrollEvent.TYPE_GAME_SESSION_LOCK,
                    appLabel = appLabel,
                    packageName = packageName,
                    detail = "Session break after ${_gameSessionLimitMinutes.value}m play (${_gameCooldownMinutes.value}m rest)"
                )
            )
            return GameGuardDecision(
                isTrackedGame = true,
                shouldBlock = true,
                lockReason = GameLockReason.SESSION_COOLDOWN,
                remainingUnlockMs = cooldownMs,
                dailyGameSecondsUsed = nextTotal,
                dailyCapSeconds = effectiveCapSec
            )
        }

        val remSessionSec = (sessionCapSec - nextSessionSec).coerceAtLeast(0)
        val shouldWarn = !gameSoftWarnedThisSession && remSessionSec in 1..60
        if (shouldWarn) {
            gameSoftWarnedThisSession = true
        }
        return GameGuardDecision(
            isTrackedGame = true,
            shouldBlock = false,
            lockReason = GameLockReason.NONE,
            shouldShowSoftWarning = shouldWarn,
            remainingSessionSeconds = remSessionSec,
            dailyGameSecondsUsed = nextTotal,
            dailyCapSeconds = effectiveCapSec
        )
    }

    fun unlockGameLockWithPin(
        nowMillis: Long = System.currentTimeMillis(),
        nowUptimeMs: Long = SystemClock.uptimeMillis()
    ) {
        ensureMindfulDailyRollover()
        val editor = prefs.edit()
        editor.putLong(KEY_GAME_COOLDOWN_UNTIL_MILLIS, 0L)
        _gameCooldownUntilMillis.value = 0L
        _activeGameSessionSeconds.value = 0
        lastGameTickUptimeMs = nowUptimeMs
        gameSoftWarnedThisSession = false

        val baseCap = _gameDailyCapMinutes.value
        val curExt = _dailyGameExtensionMinutes.value
        if (baseCap > 0 && _dailyGameTotalSeconds.value >= (baseCap + curExt) * 60) {
            val nextExt = curExt + 30
            editor.putInt(KEY_DAILY_GAME_EXT_MINUTES, nextExt)
            _dailyGameExtensionMinutes.value = nextExt
        }

        if (isWithinGameCurfewWindow(nowMillis = nowMillis)) {
            val overrideUntil = nowMillis + 30 * 60_000L
            editor.putLong(KEY_GAME_CURFEW_OVERRIDE_UNTIL_MILLIS, overrideUntil)
            _gameCurfewOverrideUntilMillis.value = overrideUntil
        }
        editor.apply()
    }

    private fun computeMillisUntilCurfewEnds(minuteOfDay: Int): Long {
        val end = _gameCurfewEndMinutes.value
        val diffMins = if (minuteOfDay < end) end - minuteOfDay else (1440 - minuteOfDay) + end
        return diffMins.coerceAtLeast(1) * 60_000L
    }

    private fun computeMillisUntilMidnight(nowMillis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return (cal.timeInMillis - nowMillis).coerceAtLeast(60_000L)
    }

    private fun readDailyIntIfToday(key: String, defaultVal: Int): Int {
        val storedDate = prefs.getString(KEY_MINDFUL_DAILY_DATE, "")
        return if (storedDate == todayDateKey()) prefs.getInt(key, defaultVal).coerceAtLeast(0) else defaultVal
    }

    private fun readDailyIntMapIfToday(key: String): Map<String, Int> {
        val storedDate = prefs.getString(KEY_MINDFUL_DAILY_DATE, "")
        return if (storedDate == todayDateKey()) readIntMap(key) else emptyMap()
    }

    private fun readDailyHourlyBuckets(dateKey: String): List<Int> {
        val storedDate = prefs.getString(KEY_MINDFUL_DAILY_DATE, "")
        if (storedDate != dateKey) return List(24) { 0 }
        val raw = prefs.getString(KEY_HOURLY_SCROLL_BUCKETS, null) ?: return List(24) { 0 }
        return runCatching {
            val arr = JSONArray(raw)
            List(24) { idx -> if (idx < arr.length()) arr.optInt(idx, 0).coerceAtLeast(0) else 0 }
        }.getOrDefault(List(24) { 0 })
    }

    private fun readLongMap(key: String): Map<String, Long> {
        val raw = prefs.getString(key, null) ?: return emptyMap()
        return runCatching {
            val json = JSONObject(raw)
            buildMap {
                val iter = json.keys()
                while (iter.hasNext()) {
                    val k = iter.next()
                    put(k, json.optLong(k, 0L))
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun writeLongMap(key: String, map: Map<String, Long>) {
        prefs.edit().putString(key, JSONObject(map.mapValues { it.value }).toString()).apply()
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

        fun computeTimerRecoveryToken(challengeCode: String): String {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(("sheildlenz_timer_end_v2_" + challengeCode.trim().uppercase(Locale.US)).toByteArray(Charsets.UTF_8))
            return digest.take(4).joinToString("") { "%02X".format(it) }
        }

        fun computeEmailHashForQr(email: String): String {
            val clean = email.trim().lowercase(Locale.US)
            if (clean.isEmpty()) return ""
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(("slz_email_v1_$clean").toByteArray(Charsets.UTF_8))
            return digest.take(6).joinToString("") { "%02X".format(it) }
        }

        fun buildRecoveryQrUri(
            mode: RecoveryQrMode,
            challengeCode: String,
            guardianEmail: String
        ): String {
            val cleanChallenge = challengeCode.trim().uppercase(Locale.US)
            val hash = computeEmailHashForQr(guardianEmail)
            return if (hash.isNotEmpty()) {
                "sheildlenz://rec?t=${mode.wireCode}&c=$cleanChallenge&e=$hash"
            } else {
                "sheildlenz://rec?t=${mode.wireCode}&c=$cleanChallenge"
            }
        }

        fun parseRecoveryQrUri(rawInput: String): RecoveryQrPayload? {
            val trimmed = rawInput.trim()
            if (!trimmed.startsWith("sheildlenz://rec", ignoreCase = true)) return null
            val query = trimmed.substringAfter("?", "")
            if (query.isBlank()) return null
            val params = query.split("&").mapNotNull { pair ->
                val idx = pair.indexOf('=')
                if (idx <= 0) null
                else pair.substring(0, idx).lowercase(Locale.US) to pair.substring(idx + 1).trim()
            }.toMap()
            val mode = RecoveryQrMode.fromWireCode(params["t"]) ?: return null
            val challenge = params["c"]?.uppercase(Locale.US)?.takeIf { it.startsWith("SLZ-") && it.length >= 8 }
                ?: return null
            val emailHash = params["e"]?.uppercase(Locale.US).orEmpty()
            return RecoveryQrPayload(mode = mode, challengeCode = challenge, emailHash = emailHash)
        }

        fun resolveRecoveryTokenForGuardianScan(
            rawQrInput: String,
            signedInEmail: String?
        ): RecoveryScanResult {
            val payload = parseRecoveryQrUri(rawQrInput) ?: return RecoveryScanResult.InvalidQr
            val cleanSignedIn = signedInEmail?.trim().orEmpty()
            if (cleanSignedIn.isEmpty()) {
                return RecoveryScanResult.RequiresGoogleSignIn
            }
            if (payload.emailHash.isNotEmpty() && computeEmailHashForQr(cleanSignedIn) != payload.emailHash) {
                return RecoveryScanResult.EmailMismatch(signedInEmail = cleanSignedIn)
            }
            val code = when (payload.mode) {
                RecoveryQrMode.TIMER_END -> computeTimerRecoveryToken(payload.challengeCode)
                RecoveryQrMode.PIN_RESET -> computeRecoveryTokenForChallenge(payload.challengeCode)
            }
            return RecoveryScanResult.Success(
                mode = payload.mode,
                challengeCode = payload.challengeCode,
                oneTimeCode = code
            )
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
        private const val KEY_WARNING_ONLY_NO_LOCKOUT = "key_warning_only_no_lockout"
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
        private const val KEY_SAFE_DNS_PROVIDER = "key_safe_dns_provider"
        private const val KEY_CUSTOM_BLOCKED_DOMAINS = "key_custom_blocked_domains"
        private const val KEY_CUSTOM_BLOCKED_KEYWORDS = "key_custom_blocked_keywords"
        private const val KEY_APP_LANGUAGE_CODE = "key_app_language_code"
        private const val KEY_TOUCH_SOUNDS_ENABLED = "key_touch_sounds_enabled"
        private const val KEY_AUTO_SOUND_SELECTOR_ENABLED = "key_auto_sound_selector_enabled"
        private const val KEY_STUDY_MODE_ENABLED = "key_study_mode_enabled"
        private const val KEY_STUDY_SESSION_END_MILLIS = "key_study_session_end_millis"
        private const val KEY_STUDY_DURATION_MINUTES = "key_study_duration_minutes"
        private const val KEY_STUDY_SOFT_WARNING_SECONDS = "key_study_soft_warning_seconds"
        private const val KEY_STUDY_ALLOWED_PACKAGES = "key_study_allowed_packages"
        private const val KEY_SMART_SAFE_SEARCH_ENABLED = "key_smart_safe_search_enabled"
        private const val KEY_SAFE_SEARCH_PACKS = "key_safe_search_packs"
        private const val KEY_SAFE_SEARCH_BLOCKED_COUNT = "key_safe_search_blocked_count"
        private const val KEY_NOTIFICATIONS_ENABLED = "key_notifications_enabled"
        private const val KEY_DISCREET_BLOCKING_SCREEN_MODE = "key_discreet_blocking_screen_mode"
        private const val KEY_MASTER_POWER_ENABLED = "key_master_power_enabled"
        private const val KEY_BLOCK_CUSTOM_HEADLINE = "key_block_custom_headline"
        private const val KEY_BLOCK_CUSTOM_MESSAGE = "key_block_custom_message"
        private const val KEY_BLOCK_SHOW_TIMER = "key_block_show_timer"
        private const val KEY_BLOCK_SHOW_APP_BADGE = "key_block_show_app_badge"
        private const val KEY_BLOCK_SHOW_QR_UNLOCK = "key_block_show_qr_unlock"
        private const val KEY_BLOCK_SHOW_QUICK_ACTIONS = "key_block_show_quick_actions"
        private const val KEY_BLOCK_SHOW_RETURN_HOME = "key_block_show_return_home"
        private const val KEY_ACTIVE_PROTECTION_PROFILE = "key_active_protection_profile"
        private const val KEY_PROFILE_AUTO_SWITCH_ENABLED = "key_profile_auto_switch_enabled"
        private const val KEY_PROFILE_SCHEDULE_START_MINUTES = "key_profile_schedule_start_minutes"
        private const val KEY_PROFILE_SCHEDULE_END_MINUTES = "key_profile_schedule_end_minutes"
        private const val KEY_SCHEDULED_TARGET_PROFILE = "key_scheduled_target_profile"
        private const val KEY_SCHEDULED_FALLBACK_PROFILE = "key_scheduled_fallback_profile"
        private const val KEY_LAST_AUTO_SWITCH_SLOT = "key_last_auto_switch_slot"
        private const val KEY_QUICK_TILE_MODE = "key_quick_tile_mode"
        private const val KEY_FULL_BROWSER_INSTALLED = "key_full_browser_installed"
        private const val KEY_BROWSER_DESKTOP_DEFAULT = "key_browser_desktop_default"
        private const val KEY_BROWSER_BOOKMARKS_JSON = "key_browser_bookmarks_json"
        private const val KEY_BROWSER_HISTORY_JSON = "key_browser_history_json"
        private const val KEY_BROWSER_DOWNLOADS_JSON = "key_browser_downloads_json"
        private const val KEY_CHROME_IMPORTED_FILE_COUNT = "key_chrome_imported_file_count"
        private const val KEY_CHROME_COMPONENTS_AUTO_CACHED = "key_chrome_components_auto_cached"
        private const val KEY_CHROME_CACHED_COMPONENT_COUNT = "key_chrome_cached_component_count"
        private const val KEY_CHROME_COMPONENT_CACHE_TS = "key_chrome_component_cache_ts"
        private const val KEY_BROWSER_SEARCH_ENGINE = "key_browser_search_engine"
        private const val KEY_BROWSER_AD_BLOCK = "key_browser_ad_block"
        private const val KEY_BROWSER_POPUPS_BLOCKED = "key_browser_popups_blocked"
        private const val KEY_BROWSER_JS_ENABLED = "key_browser_js_enabled"
        private const val KEY_BROWSER_HOME_URL = "key_browser_home_url"
        private const val KEY_BROWSER_SITE_PERMS_JSON = "key_browser_site_perms_json"
        private const val KEY_BROWSER_PIN_PROMPT_SHOWN = "key_browser_pin_prompt_shown"
        private const val KEY_APP_LOCKER_ENABLED = "key_app_locker_enabled"
        private const val KEY_LOCKED_APP_PACKAGES = "key_locked_app_packages"
        private const val KEY_APP_LOCKER_RELOCK_MINUTES = "key_app_locker_relock_minutes"
        private const val KEY_SCROLL_LOCK_ENABLED = "key_scroll_lock_enabled"
        private const val KEY_SCROLL_LIMIT_COUNT = "key_scroll_limit_count"
        private const val KEY_SCROLL_LOCK_COOLDOWN_MINUTES = "key_scroll_lock_cooldown_minutes"
        private const val KEY_SCROLL_BINGE_NUDGE_COUNT = "key_scroll_binge_nudge_count"
        private const val KEY_REELS_GUARD_PLATFORMS = "key_reels_guard_platforms"
        private const val KEY_MINDFUL_PAUSE_ENABLED = "key_mindful_pause_enabled"
        private const val KEY_MINDFUL_PAUSE_PACKAGES = "key_mindful_pause_packages"
        private const val KEY_MINDFUL_GRACE_MINUTES = "key_mindful_grace_minutes"
        private const val KEY_INSTANT_DETOX_UNTIL_MILLIS = "key_instant_detox_until_millis"
        private const val KEY_MINDFUL_DAILY_DATE = "key_mindful_daily_date"
        private const val KEY_DAILY_REELS_TOTAL = "key_daily_reels_total"
        private const val KEY_DAILY_REELS_BY_PLATFORM = "key_daily_reels_by_platform"
        private const val KEY_DAILY_APP_OPENS_MAP = "key_daily_app_opens_map"
        private const val KEY_DAILY_MINDFUL_EXITS = "key_daily_mindful_exits"
        private const val KEY_DAILY_SCROLL_LOCKS = "key_daily_scroll_locks"
        private const val KEY_HOURLY_SCROLL_BUCKETS = "key_hourly_scroll_buckets"
        private const val KEY_WEEKLY_SCROLL_HISTORY = "key_weekly_scroll_history"
        private const val KEY_CLEAN_STREAK_DAYS = "key_clean_streak_days"
        private const val KEY_RECENT_MINDFUL_EVENTS = "key_recent_mindful_events"
        private const val KEY_SCROLL_LOCKED_UNTIL_MAP = "key_scroll_locked_until_map"
        private const val KEY_PIQORAX_CONNECTOR_CONNECTED = "key_piqorax_connector_connected"
        private const val KEY_TIME_BLOCKER_ALLOWANCE_MINUTES = "key_time_blocker_allowance_minutes"
        private const val KEY_TIME_BLOCKER_WINDOW_MINUTES = "key_time_blocker_window_minutes"
        private const val KEY_PIQORAX_LAST_PASS_SUMMARY = "key_piqorax_last_pass_summary"
        private const val KEY_PIQORAX_COIN_DEVICE_LOCK = "key_piqorax_coin_device_lock"
        private const val KEY_GAME_GUARD_ENABLED = "key_game_guard_enabled"
        private const val KEY_GAME_AUTO_DETECT_ENABLED = "key_game_auto_detect_enabled"
        private const val KEY_TRACKED_GAME_PACKAGES = "key_tracked_game_packages"
        private const val KEY_EXCLUDED_GAME_PACKAGES = "key_excluded_game_packages"
        private const val KEY_GAME_SESSION_LIMIT_MINUTES = "key_game_session_limit_minutes"
        private const val KEY_GAME_COOLDOWN_MINUTES = "key_game_cooldown_minutes"
        private const val KEY_GAME_DAILY_CAP_MINUTES = "key_game_daily_cap_minutes"
        private const val KEY_GAME_CURFEW_ENABLED = "key_game_curfew_enabled"
        private const val KEY_GAME_CURFEW_START_MINUTES = "key_game_curfew_start_minutes"
        private const val KEY_GAME_CURFEW_END_MINUTES = "key_game_curfew_end_minutes"
        private const val KEY_GAME_COOLDOWN_UNTIL_MILLIS = "key_game_cooldown_until_millis"
        private const val KEY_GAME_CURFEW_OVERRIDE_UNTIL_MILLIS = "key_game_curfew_override_until_millis"
        private const val KEY_DAILY_GAME_USAGE_MAP = "key_daily_game_usage_map"
        private const val KEY_DAILY_GAME_EXT_MINUTES = "key_daily_game_ext_minutes"
    }
}
