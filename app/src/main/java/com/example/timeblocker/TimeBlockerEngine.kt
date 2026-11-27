package com.example.timeblocker

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import com.example.timeblocker.data.TimeBlockedAppEntity
import com.example.timeblocker.data.TimeBlockerMetaEntity
import java.util.Locale

fun interface ElapsedClock {
    fun elapsedRealtime(): Long

    companion object {
        val SYSTEM: ElapsedClock = ElapsedClock { SystemClock.elapsedRealtime() }
    }
}

fun interface BootCountProvider {
    fun getBootCount(): Int

    companion object {
        fun fromContext(context: Context): BootCountProvider = BootCountProvider {
            runCatching {
                Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)
            }.getOrDefault(-1)
        }
    }
}

data class TimeBlockDecision(
    val packageName: String,
    val isRestricted: Boolean,
    val shouldBlock: Boolean,
    val hasActivePass: Boolean,
    val remainingAllowanceMs: Long,
    val remainingResetMs: Long,
    val passRemainingMs: Long
)

object TimeBlockerEngine {
    const val BASE_ALLOWANCE_MS: Long = 300_000L
    const val HOURLY_WINDOW_MS: Long = 3_600_000L

    fun effectiveAllowanceMs(record: TimeBlockedAppEntity): Long =
        if (record.allowanceMs > 0L) record.allowanceMs else BASE_ALLOWANCE_MS

    fun effectiveWindowMs(record: TimeBlockedAppEntity): Long {
        val allowance = effectiveAllowanceMs(record)
        val raw = if (record.windowMs > 0L) record.windowMs else HOURLY_WINDOW_MS
        return raw.coerceAtLeast(allowance)
    }

    fun hasActivePass(record: TimeBlockedAppEntity, nowElapsedMs: Long): Boolean {
        return record.passExpiresAtElapsedMs > 0L && nowElapsedMs < record.passExpiresAtElapsedMs
    }

    fun normalizeWindow(
        record: TimeBlockedAppEntity,
        nowElapsedMs: Long
    ): TimeBlockedAppEntity {
        val clearedExpiredPass = if (record.passExpiresAtElapsedMs > 0L && nowElapsedMs >= record.passExpiresAtElapsedMs) {
            record.copy(passExpiresAtElapsedMs = 0L)
        } else {
            record
        }

        if (clearedExpiredPass.windowStartElapsedMs <= 0L) {
            return clearedExpiredPass.copy(
                windowStartElapsedMs = nowElapsedMs.coerceAtLeast(1L),
                usedMsInWindow = 0L
            )
        }

        // Guard against any stale future timestamp (e.g., missed reboot detection)
        if (nowElapsedMs < clearedExpiredPass.windowStartElapsedMs) {
            return clearedExpiredPass.copy(
                windowStartElapsedMs = nowElapsedMs.coerceAtLeast(1L),
                usedMsInWindow = 0L
            )
        }

        val windowDuration = effectiveWindowMs(clearedExpiredPass)
        val elapsedSinceWindowStart = nowElapsedMs - clearedExpiredPass.windowStartElapsedMs
        return if (elapsedSinceWindowStart >= windowDuration) {
            clearedExpiredPass.copy(
                windowStartElapsedMs = nowElapsedMs.coerceAtLeast(1L),
                usedMsInWindow = 0L
            )
        } else {
            clearedExpiredPass
        }
    }

    fun consumeForegroundUsage(
        record: TimeBlockedAppEntity,
        deltaMs: Long,
        nowElapsedMs: Long
    ): TimeBlockedAppEntity {
        val normalized = normalizeWindow(record, nowElapsedMs)
        if (deltaMs <= 0L) return normalized
        if (hasActivePass(normalized, nowElapsedMs)) {
            // Tier 2 temporary pass bypasses Tier 1 allowance consumption
            return normalized
        }
        val allowanceLimit = effectiveAllowanceMs(normalized)
        val nextUsed = (normalized.usedMsInWindow + deltaMs).coerceAtMost(allowanceLimit)
        return normalized.copy(usedMsInWindow = nextUsed)
    }

    fun evaluate(
        record: TimeBlockedAppEntity?,
        nowElapsedMs: Long
    ): TimeBlockDecision {
        if (record == null) {
            return TimeBlockDecision(
                packageName = "",
                isRestricted = false,
                shouldBlock = false,
                hasActivePass = false,
                remainingAllowanceMs = 0L,
                remainingResetMs = 0L,
                passRemainingMs = 0L
            )
        }

        val normalized = normalizeWindow(record, nowElapsedMs)
        val allowanceLimit = effectiveAllowanceMs(normalized)
        val windowDuration = effectiveWindowMs(normalized)
        val passRemaining = (normalized.passExpiresAtElapsedMs - nowElapsedMs).coerceAtLeast(0L)
        val passActive = passRemaining > 0L
        if (!normalized.isRestricted) {
            return TimeBlockDecision(
                packageName = normalized.packageName,
                isRestricted = false,
                shouldBlock = false,
                hasActivePass = passActive,
                remainingAllowanceMs = allowanceLimit,
                remainingResetMs = windowDuration,
                passRemainingMs = passRemaining
            )
        }
        val remainingAllowance = (allowanceLimit - normalized.usedMsInWindow).coerceAtLeast(0L)
        val remainingReset = if (normalized.windowStartElapsedMs > 0L) {
            (windowDuration - (nowElapsedMs - normalized.windowStartElapsedMs)).coerceIn(0L, windowDuration)
        } else {
            windowDuration
        }

        val shouldBlock = !passActive && remainingAllowance <= 0L
        return TimeBlockDecision(
            packageName = normalized.packageName,
            isRestricted = true,
            shouldBlock = shouldBlock,
            hasActivePass = passActive,
            remainingAllowanceMs = remainingAllowance,
            remainingResetMs = remainingReset,
            passRemainingMs = passRemaining
        )
    }

    fun isRebootDetected(
        meta: TimeBlockerMetaEntity?,
        records: Collection<TimeBlockedAppEntity>,
        nowElapsedMs: Long,
        currentBootCount: Int
    ): Boolean {
        if (meta != null) {
            if (meta.lastKnownBootCount >= 0 && currentBootCount >= 0 && currentBootCount != meta.lastKnownBootCount) {
                return true
            }
            if (meta.lastKnownElapsedMs > 0L && nowElapsedMs < meta.lastKnownElapsedMs) {
                return true
            }
        }
        return records.any { record ->
            (record.windowStartElapsedMs > 0L && nowElapsedMs < record.windowStartElapsedMs) ||
                (record.addedAtElapsedMs > 0L && nowElapsedMs < record.addedAtElapsedMs)
        }
    }

    fun rebaseRecordAfterReboot(
        record: TimeBlockedAppEntity,
        nowElapsedMs: Long,
        previousBootLastElapsedMs: Long
    ): TimeBlockedAppEntity {
        val safeNow = nowElapsedMs.coerceAtLeast(1L)
        val remainingPassMs = if (record.passExpiresAtElapsedMs > 0L && previousBootLastElapsedMs > 0L) {
            (record.passExpiresAtElapsedMs - previousBootLastElapsedMs).coerceAtLeast(0L)
        } else {
            0L
        }
        val rebasedPassExpiry = if (remainingPassMs > 0L) safeNow + remainingPassMs else 0L

        // Re-anchor rolling window cleanly in the new boot epoch so counters never get stuck
        // in a permanent negative-offset lock across reboots.
        return record.copy(
            windowStartElapsedMs = safeNow,
            usedMsInWindow = 0L,
            passExpiresAtElapsedMs = rebasedPassExpiry,
            addedAtElapsedMs = safeNow
        )
    }

    fun formatCountdown(ms: Long): String {
        val totalSeconds = (ms.coerceAtLeast(0L) + 999L) / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0L) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    fun formatMinutesCompact(totalMinutes: Int): String {
        val safe = totalMinutes.coerceAtLeast(1)
        val hours = safe / 60
        val mins = safe % 60
        return when {
            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
            hours > 0 -> "${hours}h"
            else -> "${mins}m"
        }
    }
}
