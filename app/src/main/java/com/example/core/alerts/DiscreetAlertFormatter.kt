package com.example.core.alerts

/**
 * Centralized Discreet & Calm Alert copy formatter shared across Safe Browser,
 * Quick Settings Tile feedback, Scheduled Auto-Switcher, Notifications, and Overlays.
 */
object DiscreetAlertFormatter {

    fun tabAutoClosedNotice(discreetMode: Boolean): String = if (discreetMode) {
        "Page paused — tab closed quietly"
    } else {
        "Filtered content detected — tab closed automatically"
    }

    fun browserBlockBadge(discreetMode: Boolean): String = if (discreetMode) {
        "BROWSING POLICY ACTIVE"
    } else {
        "SHEILDLENZ SAFE BROWSER SHIELD"
    }

    fun browserBlockHeadline(discreetMode: Boolean): String = if (discreetMode) {
        "Quiet Pause Applied"
    } else {
        "Safe Browser Blocked Content"
    }

    fun browserBlockSubtitle(discreetMode: Boolean, rawReason: String): String = if (discreetMode) {
        "This destination is paused under your active browsing preferences."
    } else {
        rawReason
    }

    fun warningNoticeBadge(
        discreetMode: Boolean,
        warningNumber: Int,
        maxWarnings: Int,
        warningOnly: Boolean
    ): String = when {
        discreetMode -> "QUIET FILTER NOTICE (#$warningNumber)"
        warningOnly -> "WARNING ONLY • NO LOCKOUT (#$warningNumber)"
        else -> "WARNING $warningNumber OF $maxWarnings BEFORE LOCKDOWN"
    }

    fun warningNoticeHeadline(discreetMode: Boolean): String = if (discreetMode) {
        "Quiet Pause Applied"
    } else {
        "Unsafe Content Blocked"
    }

    fun warningNoticeBody(
        discreetMode: Boolean,
        appName: String,
        packageName: String,
        categoryName: String,
        warningNumber: Int,
        maxWarnings: Int,
        warningOnly: Boolean,
        showAppBadge: Boolean
    ): String {
        val remaining = (maxWarnings - warningNumber).coerceAtLeast(0)
        return when {
            discreetMode && showAppBadge ->
                "$appName is taking a quiet pause under your active display policy."
            discreetMode ->
                "This view is taking a quiet pause under your active display policy."
            warningOnly -> if (showAppBadge) {
                "Detected $categoryName in $appName ($packageName).\n" +
                    "The app was closed automatically. Warning-Only mode is active so no device lockdown was engaged."
            } else {
                "The app was closed automatically. Warning-Only mode is active so no device lockdown was engaged."
            }
            else -> {
                val prefix = if (showAppBadge) "Detected $categoryName in $appName ($packageName).\n" else ""
                prefix + "The app was closed automatically. " +
                    if (remaining == 0) {
                        "This is your final warning—the next violation will trigger a full device lockdown."
                    } else {
                        "$remaining warning(s) remaining before a full device lockdown engages."
                    }
            }
        }
    }

    fun quickTileFeedback(
        discreetMode: Boolean,
        modeLabel: String,
        enabled: Boolean
    ): String = if (discreetMode) {
        if (enabled) "$modeLabel enabled quietly" else "$modeLabel paused quietly"
    } else {
        if (enabled) "$modeLabel Activated" else "$modeLabel Disengaged"
    }

    fun autoSwitchProfileToast(discreetMode: Boolean, profileTitle: String): String =
        if (discreetMode) {
            "Schedule switched gently to $profileTitle"
        } else {
            "Auto-Switcher activated $profileTitle profile"
        }
}
