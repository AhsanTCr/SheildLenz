package com.example.ui.browser

import android.net.Uri
import com.example.monitoring.accessibility.AegisAccessibilityService
import com.example.services.dns.DnsPacketHelper
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class BrowserTabState(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New Safe Tab",
    val url: String = DEFAULT_SAFE_HOME_URL,
    val progress: Int = 0,
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val desktopMode: Boolean = false
)

data class SafeBookmark(
    val title: String,
    val url: String,
    val folder: String = "Quick Links",
    val importedFromChrome: Boolean = false
)

data class BrowserHistoryEntry(
    val url: String,
    val title: String,
    val visitedAtMillis: Long = System.currentTimeMillis()
)

data class BrowserDownloadItem(
    val id: String = UUID.randomUUID().toString(),
    val fileName: String,
    val url: String,
    val mimeType: String = "application/octet-stream",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val statusLabel: String = "Saved to Downloads"
)

data class BlockedState(
    val url: String,
    val reason: String
)

enum class BrowserShieldAction {
    NONE,
    CLOSE_TAB_QUIETLY,
    FULL_SHIELD_ENFORCEMENT
}

data class BrowserRuleDecision(
    val action: BrowserShieldAction,
    val matchedReason: String? = null
)

const val DEFAULT_SAFE_HOME_URL = "https://www.google.com/?safe=active"

object SafeBrowserFilterHelper {

    fun resolveSafeNavigationUrl(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return DEFAULT_SAFE_HOME_URL
        if (trimmed.startsWith("http://", true) || trimmed.startsWith("https://", true)) {
            return enforceSafeSearchQuery(trimmed)
        }
        if (trimmed.contains('.') && !trimmed.contains(' ')) {
            return enforceSafeSearchQuery("https://$trimmed")
        }
        val encoded = Uri.encode(trimmed)
        return "https://www.google.com/search?q=$encoded&safe=active"
    }

    fun enforceSafeSearchQuery(rawUrl: String): String {
        try {
            val uri = Uri.parse(rawUrl)
            val host = uri.host?.lowercase() ?: return rawUrl
            if (host.contains("google.") && !rawUrl.contains("safe=")) {
                return uri.buildUpon().appendQueryParameter("safe", "active").build().toString()
            }
            if (host.contains("bing.") && !rawUrl.contains("adlt=")) {
                return uri.buildUpon().appendQueryParameter("adlt", "strict").build().toString()
            }
            if (host.contains("youtube.") && !rawUrl.contains("safe=")) {
                return uri.buildUpon().appendQueryParameter("safe", "active").build().toString()
            }
            if (host.contains("duckduckgo.") && !rawUrl.contains("kp=")) {
                return uri.buildUpon().appendQueryParameter("kp", "1").build().toString()
            }
        } catch (_: Exception) {
        }
        return rawUrl
    }

    fun evaluateSafeBrowserUrl(
        rawUrl: String,
        customDomains: Set<String>,
        customKeywords: Set<String>,
        blockAdult: Boolean,
        blockGambling: Boolean,
        smartSafeActive: Boolean,
        safeSearchPacks: Set<String>
    ): String? {
        val uri = Uri.parse(rawUrl)
        val host = uri.host?.lowercase() ?: rawUrl
        val decodedQuery = runCatching { Uri.decode(uri.query.orEmpty()) }.getOrDefault(uri.query.orEmpty())
        val fullText = "$host ${uri.path} $decodedQuery".lowercase()

        val match = DnsPacketHelper.matchesBlockedDomainDetailed(
            rawDomain = host,
            customDomains = customDomains,
            customKeywords = customKeywords,
            blockExplicitAdult = blockAdult,
            blockGambling = blockGambling,
            whitelistedDomains = emptySet(),
            blockDohBypass = true
        )
        if (match != null) {
            return match.reason
        }

        return AegisAccessibilityService.detectExplicitBrowserUrl(
            rawText = fullText,
            customDomains = customDomains,
            customKeywords = customKeywords,
            blockGamblingKeywords = blockGambling,
            smartSafeSearchEnabled = smartSafeActive,
            safeSearchPacks = safeSearchPacks
        )
    }

    /**
     * Autonomous Dual-Mode Decision:
     * - When Sheildlenz Shield is OFF: prohibited content quietly closes only the active tab.
     * - When Sheildlenz Shield is ON: full Sheildlenz rule engine (warnings, overlays, timers) applies.
     */
    fun decideActionForNavigation(
        rawUrl: String,
        isSheildlenzActive: Boolean,
        customDomains: Set<String>,
        customKeywords: Set<String>,
        blockAdult: Boolean,
        blockGambling: Boolean,
        smartSafeActive: Boolean,
        safeSearchPacks: Set<String>
    ): BrowserRuleDecision {
        val hitReason = evaluateSafeBrowserUrl(
            rawUrl = rawUrl,
            customDomains = customDomains,
            customKeywords = customKeywords,
            blockAdult = blockAdult,
            blockGambling = blockGambling,
            smartSafeActive = smartSafeActive,
            safeSearchPacks = safeSearchPacks
        ) ?: return BrowserRuleDecision(BrowserShieldAction.NONE, null)

        val action = if (isSheildlenzActive) {
            BrowserShieldAction.FULL_SHIELD_ENFORCEMENT
        } else {
            BrowserShieldAction.CLOSE_TAB_QUIETLY
        }
        return BrowserRuleDecision(action = action, matchedReason = hitReason)
    }

    /**
     * Closes the offending tab by [tabId] cleanly. Guarantees at least 1 fresh safe tab remains.
     */
    fun closeTabAndEnsureFallback(
        tabs: List<BrowserTabState>,
        closingTabId: String
    ): Pair<List<BrowserTabState>, String> {
        val closingIdx = tabs.indexOfFirst { it.id == closingTabId }
        val remaining = tabs.filterNot { it.id == closingTabId }
        if (remaining.isEmpty()) {
            val fresh = BrowserTabState(title = "Safe Search", url = DEFAULT_SAFE_HOME_URL)
            return listOf(fresh) to fresh.id
        }
        val nextIndex = if (closingIdx >= 0) {
            closingIdx.coerceAtMost(remaining.lastIndex)
        } else {
            0
        }
        return remaining to remaining[nextIndex].id
    }

    fun isExternalAppScheme(rawUrl: String): Boolean {
        val lower = rawUrl.trim().lowercase()
        if (lower.isBlank()) return false
        if (lower.startsWith("http://") ||
            lower.startsWith("https://") ||
            lower.startsWith("about:") ||
            lower.startsWith("data:") ||
            lower.startsWith("blob:") ||
            lower.startsWith("javascript:")
        ) {
            return false
        }
        return lower.contains(":")
    }

    /**
     * Parses exported Chrome Bookmarks (Netscape HTML `<A HREF="...">Title</A>` or Chrome Bookmarks JSON).
     */
    fun parseChromeBookmarksFile(rawContent: String): List<SafeBookmark> {
        val trimmed = rawContent.trim()
        if (trimmed.isEmpty()) return emptyList()
        val results = mutableListOf<SafeBookmark>()
        val seenUrls = mutableSetOf<String>()

        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            runCatching {
                if (trimmed.startsWith("{")) {
                    val root = JSONObject(trimmed)
                    extractJsonBookmarksRecursive(root, "Chrome Import", results, seenUrls)
                } else {
                    val arr = JSONArray(trimmed)
                    for (i in 0 until arr.length()) {
                        arr.optJSONObject(i)?.let {
                            extractJsonBookmarksRecursive(it, "Chrome Import", results, seenUrls)
                        }
                    }
                }
            }
            if (results.isNotEmpty()) return results.take(120)
        }

        // Netscape HTML Bookmarks format exported natively by Chrome
        val linkRegex = Regex(
            """<a\s+[^>]*href\s*=\s*["'](https?://[^"']+)["'][^>]*>(.*?)</a>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        linkRegex.findAll(trimmed).forEach { match ->
            val url = match.groupValues[1].trim()
            val rawTitle = match.groupValues[2]
                .replace(Regex("<[^>]+>"), "")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .trim()
            if (url.isNotBlank() && seenUrls.add(url)) {
                val hostLabel = runCatching { Uri.parse(url).host }.getOrNull() ?: "Bookmark"
                results.add(
                    SafeBookmark(
                        title = rawTitle.ifBlank { hostLabel }.take(48),
                        url = url,
                        folder = "Chrome Imported",
                        importedFromChrome = true
                    )
                )
            }
        }
        return results.take(120)
    }

    private fun extractJsonBookmarksRecursive(
        obj: JSONObject,
        folder: String,
        out: MutableList<SafeBookmark>,
        seenUrls: MutableSet<String>
    ) {
        val type = obj.optString("type", "")
        val url = obj.optString("url", "")
        val name = obj.optString("name", obj.optString("title", "")).trim()
        if (url.startsWith("http://", true) || url.startsWith("https://", true)) {
            if (seenUrls.add(url)) {
                out.add(
                    SafeBookmark(
                        title = name.ifBlank { url }.take(48),
                        url = url,
                        folder = folder,
                        importedFromChrome = true
                    )
                )
            }
        }
        val nextFolder = if (type == "folder" && name.isNotBlank()) name else folder
        val children = obj.optJSONArray("children")
        if (children != null) {
            for (i in 0 until children.length()) {
                children.optJSONObject(i)?.let {
                    extractJsonBookmarksRecursive(it, nextFolder, out, seenUrls)
                }
            }
        }
        val roots = obj.optJSONObject("roots")
        if (roots != null) {
            val keys = roots.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                roots.optJSONObject(k)?.let {
                    extractJsonBookmarksRecursive(it, "Chrome $k", out, seenUrls)
                }
            }
        }
    }

    fun serializeBookmarks(bookmarks: List<SafeBookmark>): String {
        val arr = JSONArray()
        bookmarks.take(120).forEach { b ->
            arr.put(
                JSONObject().apply {
                    put("title", b.title)
                    put("url", b.url)
                    put("folder", b.folder)
                    put("importedFromChrome", b.importedFromChrome)
                }
            )
        }
        return arr.toString()
    }

    fun deserializeBookmarks(rawJson: String): List<SafeBookmark> {
        if (rawJson.isBlank()) return defaultBookmarks()
        return runCatching {
            val arr = JSONArray(rawJson)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val u = o.optString("url", "")
                    if (u.isNotBlank()) {
                        add(
                            SafeBookmark(
                                title = o.optString("title", u),
                                url = u,
                                folder = o.optString("folder", "Quick Links"),
                                importedFromChrome = o.optBoolean("importedFromChrome", false)
                            )
                        )
                    }
                }
            }.ifEmpty { defaultBookmarks() }
        }.getOrDefault(defaultBookmarks())
    }

    fun serializeHistory(entries: List<BrowserHistoryEntry>): String {
        val arr = JSONArray()
        entries.take(100).forEach { h ->
            arr.put(
                JSONObject().apply {
                    put("url", h.url)
                    put("title", h.title)
                    put("ts", h.visitedAtMillis)
                }
            )
        }
        return arr.toString()
    }

    fun deserializeHistory(rawJson: String): List<BrowserHistoryEntry> {
        if (rawJson.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(rawJson)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val u = o.optString("url", "")
                    if (u.isNotBlank()) {
                        add(
                            BrowserHistoryEntry(
                                url = u,
                                title = o.optString("title", u),
                                visitedAtMillis = o.optLong("ts", 0L)
                            )
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    fun serializeDownloads(items: List<BrowserDownloadItem>): String {
        val arr = JSONArray()
        items.take(60).forEach { d ->
            arr.put(
                JSONObject().apply {
                    put("id", d.id)
                    put("fileName", d.fileName)
                    put("url", d.url)
                    put("mimeType", d.mimeType)
                    put("ts", d.createdAtMillis)
                    put("status", d.statusLabel)
                }
            )
        }
        return arr.toString()
    }

    fun deserializeDownloads(rawJson: String): List<BrowserDownloadItem> {
        if (rawJson.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(rawJson)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val u = o.optString("url", "")
                    if (u.isNotBlank()) {
                        add(
                            BrowserDownloadItem(
                                id = o.optString("id", UUID.randomUUID().toString()),
                                fileName = o.optString("fileName", "download"),
                                url = u,
                                mimeType = o.optString("mimeType", "application/octet-stream"),
                                createdAtMillis = o.optLong("ts", 0L),
                                statusLabel = o.optString("status", "Saved to Downloads")
                            )
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    fun defaultBookmarks(): List<SafeBookmark> = listOf(
        SafeBookmark("Google Safe", "https://www.google.com/?safe=active"),
        SafeBookmark("Wikipedia", "https://en.wikipedia.org"),
        SafeBookmark("Khan Academy", "https://www.khanacademy.org"),
        SafeBookmark("BBC Learning", "https://www.bbc.co.uk/learning"),
        SafeBookmark("YouTube Kids", "https://www.youtube.com/?safe=active")
    )
}
