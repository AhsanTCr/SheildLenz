package com.example.core.models

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

enum class ShortVideoPlatform(
    val id: String,
    val displayName: String,
    val shortLabel: String,
    val packageNames: Set<String>
) {
    YOUTUBE_SHORTS(
        id = "youtube_shorts",
        displayName = "YouTube Shorts",
        shortLabel = "Shorts",
        packageNames = setOf(
            "com.google.android.youtube",
            "app.revanced.android.youtube",
            "com.google.android.apps.youtube.kids"
        )
    ),
    INSTAGRAM_REELS(
        id = "instagram_reels",
        displayName = "Instagram Reels",
        shortLabel = "IG Reels",
        packageNames = setOf(
            "com.instagram.android",
            "com.instagram.lite"
        )
    ),
    TIKTOK(
        id = "tiktok",
        displayName = "TikTok",
        shortLabel = "TikTok",
        packageNames = setOf(
            "com.zhiliaoapp.musically",
            "com.ss.android.ugc.trill",
            "com.ss.android.ugc.aweme",
            "com.zhiliaoapp.musically.lite",
            "com.ss.android.ugc.awemelite",
            "com.ss.android.ugc.trill.lite",
            "com.zhiliaoapp.musically.go"
        )
    ),
    FACEBOOK_REELS(
        id = "facebook_reels",
        displayName = "Facebook Reels",
        shortLabel = "FB Reels",
        packageNames = setOf(
            "com.facebook.katana",
            "com.facebook.lite"
        )
    );

    companion object {
        val ALL_IDS: Set<String> = entries.map { it.id }.toSet()
        val ALL_PACKAGES: Set<String> = entries.flatMap { it.packageNames }.toSet()

        fun fromId(id: String?): ShortVideoPlatform? =
            entries.firstOrNull { it.id.equals(id?.trim(), ignoreCase = true) }

        fun findByPackage(packageName: String?): List<ShortVideoPlatform> {
            val clean = packageName?.trim()?.lowercase().orEmpty()
            if (clean.isBlank()) return emptyList()
            return entries.filter { clean in it.packageNames }
        }
    }
}

data class DailyScrollSnapshot(
    val dateKey: String,
    val usageSeconds: Int,
    val reelsCount: Int,
    val mindfulExits: Int,
    val locksTriggered: Int
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("dateKey", dateKey)
        put("usageSeconds", usageSeconds)
        put("reelsCount", reelsCount)
        put("mindfulExits", mindfulExits)
        put("locksTriggered", locksTriggered)
    }

    companion object {
        fun fromJson(obj: JSONObject): DailyScrollSnapshot? {
            val key = obj.optString("dateKey", "").trim()
            if (key.isBlank()) return null
            return DailyScrollSnapshot(
                dateKey = key,
                usageSeconds = obj.optInt("usageSeconds", 0).coerceAtLeast(0),
                reelsCount = obj.optInt("reelsCount", 0).coerceAtLeast(0),
                mindfulExits = obj.optInt("mindfulExits", 0).coerceAtLeast(0),
                locksTriggered = obj.optInt("locksTriggered", 0).coerceAtLeast(0)
            )
        }

        fun listToJsonString(list: List<DailyScrollSnapshot>): String {
            val arr = JSONArray()
            list.forEach { arr.put(it.toJson()) }
            return arr.toString()
        }

        fun listFromJsonString(raw: String?): List<DailyScrollSnapshot> {
            if (raw.isNullOrBlank()) return emptyList()
            return runCatching {
                val arr = JSONArray(raw)
                buildList {
                    for (i in 0 until arr.length()) {
                        val item = arr.optJSONObject(i)?.let { fromJson(it) }
                        if (item != null) add(item)
                    }
                }
            }.getOrDefault(emptyList())
        }
    }
}

data class MindfulScrollEvent(
    val timestampMillis: Long,
    val type: String,
    val appLabel: String,
    val packageName: String,
    val detail: String
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("timestampMillis", timestampMillis)
        put("type", type)
        put("appLabel", appLabel)
        put("packageName", packageName)
        put("detail", detail)
    }

    companion object {
        const val TYPE_MINDFUL_PAUSE = "MINDFUL_PAUSE"
        const val TYPE_MINDFUL_EXIT = "MINDFUL_EXIT"
        const val TYPE_REEL_SCROLLED = "REEL_SCROLLED"
        const val TYPE_BINGE_NUDGE = "BINGE_NUDGE"
        const val TYPE_SCROLL_LOCK = "SCROLL_LOCK"
        const val TYPE_DETOX_LOCK = "DETOX_LOCK"
        const val TYPE_GAME_SESSION_LOCK = "GAME_SESSION_LOCK"
        const val TYPE_GAME_DAILY_CAP = "GAME_DAILY_CAP"
        const val TYPE_GAME_CURFEW = "GAME_CURFEW"

        fun fromJson(obj: JSONObject): MindfulScrollEvent? {
            val ts = obj.optLong("timestampMillis", 0L)
            val type = obj.optString("type", "").trim()
            if (ts <= 0L || type.isBlank()) return null
            return MindfulScrollEvent(
                timestampMillis = ts,
                type = type,
                appLabel = obj.optString("appLabel", "App"),
                packageName = obj.optString("packageName", ""),
                detail = obj.optString("detail", "")
            )
        }

        fun listToJsonString(list: List<MindfulScrollEvent>): String {
            val arr = JSONArray()
            list.forEach { arr.put(it.toJson()) }
            return arr.toString()
        }

        fun listFromJsonString(raw: String?): List<MindfulScrollEvent> {
            if (raw.isNullOrBlank()) return emptyList()
            return runCatching {
                val arr = JSONArray(raw)
                buildList {
                    for (i in 0 until arr.length()) {
                        val item = arr.optJSONObject(i)?.let { fromJson(it) }
                        if (item != null) add(item)
                    }
                }
            }.getOrDefault(emptyList())
        }
    }
}

data class ShortVideoScrollResult(
    val totalReelsToday: Int,
    val sessionReelsSinceUnlock: Int,
    val scrollLimit: Int,
    val shouldTriggerScrollLock: Boolean,
    val shouldTriggerBingeNudge: Boolean
)

/**
 * Pure-Kotlin structural UI analyzer for detecting active short-form video feeds
 * (YouTube Shorts, Instagram Reels, TikTok, Facebook Reels) without false positives
 * on chats, search bars, settings, or standard long-form videos.
 */
object ShortVideoScrollDetector {

    private val YOUTUBE_SHORTS_IDS = listOf(
        "reel_player",
        "reel_watch",
        "reel_recycler",
        "shorts_player",
        "shorts_pivot",
        "shorts_shelf_video",
        "reel_progress_bar"
    )

    private val YOUTUBE_EXCLUSION_IDS = listOf(
        "watch_player",
        "player_fragment_container",
        "search_edit_text",
        "comment_composer"
    )

    private val INSTAGRAM_REELS_IDS = listOf(
        "clips_viewer",
        "clips_swipe_refresh",
        "clips_ufi",
        "clips_author",
        "reel_viewer",
        "clips_video_container"
    )

    private val INSTAGRAM_EXCLUSION_IDS = listOf(
        "direct_inbox",
        "thread_fragment",
        "row_thread_composer",
        "search_edit_text"
    )

    private val FACEBOOK_REELS_IDS = listOf(
        "reels_viewer",
        "fb_shorts",
        "short_form_video",
        "reels_player"
    )

    private val TIKTOK_EXCLUSION_TOKENS = listOf(
        "direct_message",
        "inbox",
        "chat_input",
        "login",
        "settings",
        "comment_list",
        "comment_container",
        "comment_panel"
    )

    val IGNORED_CHROME_LABELS = setOf(
        "shorts",
        "reels",
        "like",
        "dislike",
        "comment",
        "comments",
        "share",
        "remix",
        "home",
        "following",
        "for you",
        "friends",
        "live",
        "shop",
        "explore",
        "discover",
        "inbox",
        "profile",
        "search",
        "notifications",
        "subscriptions",
        "you",
        "add",
        "create",
        "follow",
        "mute",
        "unmute"
    )

    fun detectActivePlatform(
        packageName: String?,
        viewIds: List<String>,
        labels: List<String>,
        classNames: List<String> = emptyList(),
        enabledPlatformIds: Set<String> = ShortVideoPlatform.ALL_IDS
    ): ShortVideoPlatform? {
        val pkg = packageName?.trim()?.lowercase().orEmpty()
        if (pkg.isBlank()) return null

        val lowerIds = viewIds.map { it.lowercase() }
        val lowerLabels = labels.map { it.trim().lowercase() }.filter { it.isNotBlank() }
        val lowerClasses = classNames.map { it.lowercase() }

        if (pkg in ShortVideoPlatform.YOUTUBE_SHORTS.packageNames &&
            ShortVideoPlatform.YOUTUBE_SHORTS.id in enabledPlatformIds
        ) {
            val hasExcludedPlayer = lowerIds.any { id -> YOUTUBE_EXCLUSION_IDS.any { id.contains(it) } }
            val hasShortsId = lowerIds.any { id -> YOUTUBE_SHORTS_IDS.any { id.contains(it) } }
            val hasShortsUiCue = lowerLabels.any {
                it == "shorts" || it.contains("remix this short") || it.contains("subscribe") && hasShortsId
            }
            if (hasShortsId || (!hasExcludedPlayer && hasShortsUiCue && lowerClasses.any { it.contains("viewpager") || it.contains("recyclerview") })) {
                return ShortVideoPlatform.YOUTUBE_SHORTS
            }
        }

        if (pkg in ShortVideoPlatform.INSTAGRAM_REELS.packageNames &&
            ShortVideoPlatform.INSTAGRAM_REELS.id in enabledPlatformIds
        ) {
            val isDirectMessage = lowerIds.any { id -> INSTAGRAM_EXCLUSION_IDS.any { id.contains(it) } }
            if (isDirectMessage) return null
            val hasReelsId = lowerIds.any { id -> INSTAGRAM_REELS_IDS.any { id.contains(it) } }
            val hasReelsLabel = lowerLabels.any { it == "reels" || it.contains("original audio") || it.contains("reel by") }
            if (hasReelsId || (hasReelsLabel && lowerClasses.any { it.contains("viewpager") })) {
                return ShortVideoPlatform.INSTAGRAM_REELS
            }
        }

        if (pkg in ShortVideoPlatform.TIKTOK.packageNames &&
            ShortVideoPlatform.TIKTOK.id in enabledPlatformIds
        ) {
            val isExcluded = lowerIds.any { id -> TIKTOK_EXCLUSION_TOKENS.any { id.contains(it) } }
            if (!isExcluded) {
                return ShortVideoPlatform.TIKTOK
            }
        }

        if (pkg in ShortVideoPlatform.FACEBOOK_REELS.packageNames &&
            ShortVideoPlatform.FACEBOOK_REELS.id in enabledPlatformIds
        ) {
            val hasFbReelsId = lowerIds.any { id -> FACEBOOK_REELS_IDS.any { id.contains(it) } }
            val hasFbReelsLabel = lowerLabels.any { it == "reels" || it.contains("reels and short videos") }
            if (hasFbReelsId || (hasFbReelsLabel && lowerClasses.any { it.contains("viewpager") || it.contains("recyclerview") })) {
                return ShortVideoPlatform.FACEBOOK_REELS
            }
        }

        return null
    }

    fun hasDynamicTextLabel(labels: List<String>): Boolean {
        return labels.any { raw ->
            val trimmed = raw.trim()
            trimmed.length in 2..120 && trimmed.lowercase() !in IGNORED_CHROME_LABELS
        }
    }

    fun buildContentSignature(labels: List<String>, viewIds: List<String>): String {
        val meaningful = labels
            .map { it.trim() }
            .filter { it.length in 2..120 }
            .filterNot { it.lowercase() in IGNORED_CHROME_LABELS }
            .distinct()
            .take(4)
        if (meaningful.isNotEmpty()) {
            return meaningful.joinToString("|")
        }
        return viewIds.take(4).joinToString("|")
    }

    fun computePeakScrollWindow(hourlyBuckets: List<Int>): String {
        if (hourlyBuckets.size != 24 || hourlyBuckets.all { it <= 0 }) {
            return "No peak scroll data yet"
        }
        val peakHour = hourlyBuckets.indices.maxByOrNull { hourlyBuckets[it] } ?: return "No peak scroll data yet"
        if (hourlyBuckets[peakHour] <= 0) return "No peak scroll data yet"
        val nextHour = (peakHour + 1) % 24
        return "${formatHourLabel(peakHour)} – ${formatHourLabel(nextHour)}"
    }

    fun formatHourLabel(hour24: Int): String {
        val h = ((hour24 % 24) + 24) % 24
        return when {
            h == 0 -> "12 AM"
            h < 12 -> "$h AM"
            h == 12 -> "12 PM"
            else -> "${h - 12} PM"
        }
    }

    fun formatMinutesCompact(totalSeconds: Int): String {
        val safeSec = totalSeconds.coerceAtLeast(0)
        val hours = safeSec / 3600
        val mins = (safeSec % 3600) / 60
        return when {
            hours > 0 -> "${hours}h ${mins}m"
            mins > 0 -> "${mins}m"
            safeSec > 0 -> "${safeSec}s"
            else -> "0m"
        }
    }

    fun buildWeeklySummaryReport(
        todayUsageSeconds: Int,
        todayReels: Int,
        scrollLimit: Int,
        todayExits: Int,
        streakDays: Int,
        peakWindow: String,
        history: List<DailyScrollSnapshot>
    ): String {
        val sb = StringBuilder()
        sb.appendLine("Sheildlenz Usage & Scroll Guard Weekly Report")
        sb.appendLine("--------------------------------------------")
        sb.appendLine("Today Screen Time: ${formatMinutesCompact(todayUsageSeconds)}")
        sb.appendLine("Today Reels/Shorts: $todayReels / $scrollLimit")
        sb.appendLine("Mindful Exits Today: $todayExits")
        sb.appendLine("Under-Limit Streak: $streakDays day(s)")
        sb.appendLine("Peak Scroll Window: $peakWindow")
        if (history.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("7-Day Log (Date | Usage | Reels | Exits | Locks):")
            history.forEach { snap ->
                sb.appendLine(
                    String.format(
                        Locale.US,
                        "%s | %s | %d reels | %d exits | %d locks",
                        snap.dateKey,
                        formatMinutesCompact(snap.usageSeconds),
                        snap.reelsCount,
                        snap.mindfulExits,
                        snap.locksTriggered
                    )
                )
            }
        }
        return sb.toString().trim()
    }
}
