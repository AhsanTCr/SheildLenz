package com.example.services.protection

import com.example.data.preferences.AegisPreferences
import com.example.data.preferences.ProtectionProfile
import java.util.Calendar

/**
 * Evaluates daily time-window profile auto-switching rules for Sheildlenz.
 * Applies the scheduled target profile when inside [startMinute, endMinute] and falls back
 * to the configured rest-of-day profile outside the window without duplicate re-applications.
 */
object ProfileAutoScheduler {

    const val SLOT_TARGET = "TARGET"
    const val SLOT_FALLBACK = "FALLBACK"

    fun determineSlotAndTargetProfile(
        enabled: Boolean,
        masterPowerEnabled: Boolean,
        minuteOfDay: Int,
        startMinute: Int,
        endMinute: Int,
        targetProfile: ProtectionProfile,
        fallbackProfile: ProtectionProfile
    ): Pair<String, ProtectionProfile>? {
        if (!enabled || !masterPowerEnabled) return null
        val inTargetWindow = AegisPreferences.isMinuteWithinWindow(
            minuteOfDay = minuteOfDay,
            startMinute = startMinute,
            endMinute = endMinute
        )
        return if (inTargetWindow) {
            SLOT_TARGET to targetProfile
        } else {
            SLOT_FALLBACK to fallbackProfile
        }
    }

    fun currentClockMinuteOfDay(): Int {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    /**
     * Evaluates schedule state against [currentMinuteOfDay] and applies profile transitions
     * at most once per slot change unless [forceApply] is true (such as when user toggles switch ON).
     * Returns the newly applied [ProtectionProfile] if a transition occurred, or null otherwise.
     */
    fun evaluateAndApply(
        preferences: AegisPreferences,
        currentMinuteOfDay: Int = currentClockMinuteOfDay(),
        forceApply: Boolean = false
    ): ProtectionProfile? {
        val decision = determineSlotAndTargetProfile(
            enabled = preferences.profileAutoSwitchEnabled.value,
            masterPowerEnabled = preferences.masterPowerEnabled.value,
            minuteOfDay = currentMinuteOfDay,
            startMinute = preferences.profileScheduleStartMinutes.value,
            endMinute = preferences.profileScheduleEndMinutes.value,
            targetProfile = preferences.scheduledTargetProfile.value,
            fallbackProfile = preferences.scheduledFallbackProfile.value
        ) ?: return null

        val (slot, desiredProfile) = decision
        val lastSlot = preferences.lastAppliedAutoSwitchSlot.value
        val activeProfile = preferences.activeProtectionProfile.value

        val shouldApply = forceApply || lastSlot != slot || activeProfile == null
        if (!shouldApply) return null

        if (activeProfile != desiredProfile) {
            preferences.applyProtectionProfile(desiredProfile)
        }
        preferences.setLastAppliedAutoSwitchSlot(slot)
        return desiredProfile
    }
}
