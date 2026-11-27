package com.example.ui.browser

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import androidx.core.content.FileProvider
import com.example.data.preferences.BrowserSearchEngine
import com.example.monitoring.accessibility.AegisAccessibilityService
import com.example.services.dns.DnsPacketHelper
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class BrowserTabState(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New Safe Tab",
    val url: String = DEFAULT_SAFE_HOME_URL,
    val progress: Int = 0,
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val desktopMode: Boolean = false,
    val isIncognito: Boolean = false,
    val isNewTabPage: Boolean = false,
    val pageZoomPercent: Int = 100
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

enum class SitePermissionType(val label: String) {
    CAMERA("Camera"),
    MICROPHONE("Microphone"),
    LOCATION("Location")
}

enum class SitePermissionState(val id: String, val label: String) {
    ASK("ask", "Ask"),
    ALLOW("allow", "Allow"),
    BLOCK("block", "Block");

    companion object {
        fun fromId(id: String?): SitePermissionState =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: ASK
    }
}

data class SitePermissionsEntry(
    val origin: String,
    val camera: SitePermissionState = SitePermissionState.ASK,
    val microphone: SitePermissionState = SitePermissionState.ASK,
    val location: SitePermissionState = SitePermissionState.ASK
)

const val DEFAULT_SAFE_HOME_URL = "https://www.google.com/?safe=active"
const val NEW_TAB_INTERNAL_URL = "sheildlenz://newtab"

object BrowserPermissionsStore {
    fun extractOrigin(rawUrl: String): String {
        val uri = runCatching { Uri.parse(rawUrl.trim()) }.getOrNull() ?: return ""
        val host = uri.host?.lowercase(Locale.US)?.takeIf { it.isNotBlank() } ?: return ""
        val scheme = uri.scheme?.lowercase(Locale.US)?.takeIf { it == "http" || it == "https" } ?: "https"
        return "$scheme://$host"
    }

    fun parseSitePermissions(rawJson: String): Map<String, SitePermissionsEntry> {
        if (rawJson.isBlank()) return emptyMap()
        return runCatching {
            val root = JSONObject(rawJson)
            buildMap {
                val keys = root.keys()
                while (keys.hasNext()) {
                    val origin = keys.next()
                    val obj = root.optJSONObject(origin) ?: continue
                    put(
                        origin,
                        SitePermissionsEntry(
                            origin = origin,
                            camera = SitePermissionState.fromId(obj.optString("cam")),
                            microphone = SitePermissionState.fromId(obj.optString("mic")),
                            location = SitePermissionState.fromId(obj.optString("loc"))
                        )
                    )
                }
            }
        }.getOrDefault(emptyMap())
    }

    fun serializeSitePermissions(map: Map<String, SitePermissionsEntry>): String {
        val root = JSONObject()
        map.entries.take(120).forEach { (origin, entry) ->
            root.put(
                origin,
                JSONObject().apply {
                    put("cam", entry.camera.id)
                    put("mic", entry.microphone.id)
                    put("loc", entry.location.id)
                }
            )
        }
        return root.toString()
    }

    fun getEntry(rawJson: String, url: String): SitePermissionsEntry {
        val origin = extractOrigin(url)
        if (origin.isBlank()) return SitePermissionsEntry("")
        return parseSitePermissions(rawJson)[origin] ?: SitePermissionsEntry(origin)
    }

    fun getPolicy(rawJson: String, url: String, type: SitePermissionType): SitePermissionState {
        val entry = getEntry(rawJson, url)
        return when (type) {
            SitePermissionType.CAMERA -> entry.camera
            SitePermissionType.MICROPHONE -> entry.microphone
            SitePermissionType.LOCATION -> entry.location
        }
    }

    fun updatePolicy(
        rawJson: String,
        url: String,
        type: SitePermissionType,
        state: SitePermissionState
    ): String {
        val origin = extractOrigin(url)
        if (origin.isBlank()) return rawJson
        val map = parseSitePermissions(rawJson).toMutableMap()
        val existing = map[origin] ?: SitePermissionsEntry(origin)
        val updated = when (type) {
            SitePermissionType.CAMERA -> existing.copy(camera = state)
            SitePermissionType.MICROPHONE -> existing.copy(microphone = state)
            SitePermissionType.LOCATION -> existing.copy(location = state)
        }
        if (updated.camera == SitePermissionState.ASK &&
            updated.microphone == SitePermissionState.ASK &&
            updated.location == SitePermissionState.ASK
        ) {
            map.remove(origin)
        } else {
            map[origin] = updated
        }
        return serializeSitePermissions(map)
    }
}

object BrowserPdfExportHelper {

    fun pdfOutputDirectory(context: Context): File {
        return File(context.filesDir, "browser_pdfs").apply {
            if (!exists()) mkdirs()
        }
    }

    fun saveWebPageAsPdf(
        context: Context,
        webView: WebView?,
        pageTitle: String,
        pageUrl: String
    ): BrowserDownloadItem? {
        return runCatching {
            val dir = pdfOutputDirectory(context)
            val cleanStem = pageTitle
                .replace(Regex("[^A-Za-z0-9_-]+"), "_")
                .trim('_')
                .ifBlank { "WebPage" }
                .take(32)
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "${cleanStem}_$stamp.pdf"
            val outFile = File(dir, fileName)

            val renderedWithNativePdf = runCatching {
                val doc = PdfDocument()
                try {
                    val wvWidth = webView?.width?.takeIf { it > 0 } ?: 595
                    val wvHeight = webView?.height?.takeIf { it > 0 } ?: 842
                    val pageWidth = 595
                    val pageHeight = ((wvHeight.toFloat() / wvWidth.toFloat()) * pageWidth)
                        .toInt()
                        .coerceIn(842, 1684)

                    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
                    val page = doc.startPage(pageInfo)
                    val canvas: Canvas = page.canvas

                    if (webView != null && webView.width > 0 && webView.height > 0) {
                        val scale = pageWidth.toFloat() / webView.width.toFloat()
                        canvas.save()
                        canvas.scale(scale, scale)
                        webView.draw(canvas)
                        canvas.restore()
                    } else {
                        canvas.drawColor(Color.WHITE)
                        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.parseColor("#1A3673")
                            textSize = 20f
                            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                        }
                        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.parseColor("#3E5894")
                            textSize = 12f
                        }
                        canvas.drawText("Sheildlenz Safe Browser — Saved Webpage", 36f, 64f, titlePaint)
                        canvas.drawText("Title: ${pageTitle.ifBlank { "Web Page" }}", 36f, 108f, bodyPaint)
                        canvas.drawText("URL: ${pageUrl.take(90)}", 36f, 134f, bodyPaint)
                        canvas.drawText("Saved: ${SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US).format(Date())}", 36f, 160f, bodyPaint)
                    }

                    doc.finishPage(page)
                    FileOutputStream(outFile).use { fos ->
                        doc.writeTo(fos)
                    }
                    outFile.exists() && outFile.length() > 0L
                } finally {
                    runCatching { doc.close() }
                }
            }.getOrDefault(false)

            if (!renderedWithNativePdf) {
                writeFallbackPdfDocument(
                    outFile = outFile,
                    pageTitle = pageTitle.ifBlank { "Web Page" },
                    pageUrl = pageUrl
                )
            }

            if (!outFile.exists() || outFile.length() == 0L) {
                return@runCatching null
            }

            BrowserDownloadItem(
                fileName = fileName,
                url = outFile.absolutePath,
                mimeType = "application/pdf",
                statusLabel = "Saved PDF in Downloads"
            )
        }.getOrNull()
    }

    private fun escapePdfLiteral(value: String): String {
        return buildString(value.length) {
            value.forEach { ch ->
                when (ch) {
                    '\\', '(', ')' -> append('\\').append(ch)
                    in ' '..'~' -> append(ch)
                    else -> append(' ')
                }
            }
        }
    }

    private fun writeFallbackPdfDocument(
        outFile: File,
        pageTitle: String,
        pageUrl: String
    ) {
        val savedAt = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US).format(Date())
        val contentStream = buildString {
            append("BT\n")
            append("/F1 18 Tf 48 770 Td (")
            append(escapePdfLiteral("Sheildlenz Safe Browser - Saved Webpage"))
            append(") Tj\n")
            append("/F1 12 Tf 0 -32 Td (Title: ")
            append(escapePdfLiteral(pageTitle.take(80)))
            append(") Tj\n")
            append("0 -22 Td (URL: ")
            append(escapePdfLiteral(pageUrl.take(90)))
            append(") Tj\n")
            append("0 -22 Td (Saved: ")
            append(escapePdfLiteral(savedAt))
            append(") Tj\n")
            append("ET\n")
        }

        val objects = listOf(
            "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n",
            "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n",
            "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>\nendobj\n",
            "4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n",
            "5 0 obj\n<< /Length ${contentStream.toByteArray(Charsets.US_ASCII).size} >>\nstream\n$contentStream\nendstream\nendobj\n"
        )

        val sb = StringBuilder("%PDF-1.4\n")
        val offsets = mutableListOf<Int>()
        objects.forEach { obj ->
            offsets += sb.toString().toByteArray(Charsets.US_ASCII).size
            sb.append(obj)
        }
        val xrefOffset = sb.toString().toByteArray(Charsets.US_ASCII).size
        sb.append("xref\n0 ${objects.size + 1}\n")
        sb.append("0000000000 65535 f \n")
        offsets.forEach { offset ->
            sb.append(String.format(Locale.US, "%010d 00000 n \n", offset))
        }
        sb.append("trailer\n<< /Size ${objects.size + 1} /Root 1 0 R >>\n")
        sb.append("startxref\n$xrefOffset\n%%EOF\n")

        FileOutputStream(outFile).use { fos ->
            fos.write(sb.toString().toByteArray(Charsets.US_ASCII))
        }
    }

    fun printWebPage(
        context: Context,
        webView: WebView?,
        jobName: String
    ): Boolean {
        if (webView == null) return false
        return runCatching {
            val pm = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return false
            val cleanJob = jobName.ifBlank { "Sheildlenz Webpage" }.take(40)
            val adapter = webView.createPrintDocumentAdapter(cleanJob)
            pm.print(cleanJob, adapter, PrintAttributes.Builder().build())
            true
        }.getOrDefault(false)
    }

    fun openSavedDownload(context: Context, item: BrowserDownloadItem): Boolean {
        return runCatching {
            val localFile = File(item.url)
            if (localFile.exists()) {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    localFile
                )
                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, item.mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(viewIntent)
                true
            } else if (item.url.startsWith("http://", true) || item.url.startsWith("https://", true)) {
                val extIntent = Intent(Intent.ACTION_VIEW, Uri.parse(item.url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(extIntent)
                true
            } else {
                false
            }
        }.getOrDefault(false)
    }
}

object SafeBrowserFilterHelper {

    private val AD_AND_TRACKER_HOSTS = setOf(
        "doubleclick.net",
        "googlesyndication.com",
        "googleadservices.com",
        "adservice.google.com",
        "google-analytics.com",
        "googletagmanager.com",
        "taboola.com",
        "outbrain.com",
        "criteo.com",
        "criteo.net",
        "pubmatic.com",
        "rubiconproject.com",
        "openx.net",
        "adnxs.com",
        "scorecardresearch.com",
        "quantserve.com",
        "mixpanel.com",
        "hotjar.com",
        "appsflyer.com",
        "branch.io",
        "adjust.com",
        "ads.yahoo.com",
        "admob.com",
        "amazon-adsystem.com",
        "moatads.com"
    )

    fun isTrackerOrAdDomain(rawUrl: String): Boolean {
        val host = runCatching { Uri.parse(rawUrl).host?.lowercase(Locale.US) }.getOrNull() ?: return false
        return AD_AND_TRACKER_HOSTS.any { tracker ->
            host == tracker || host.endsWith(".$tracker")
        }
    }

    fun resolveSafeNavigationUrl(
        input: String,
        searchEngine: BrowserSearchEngine = BrowserSearchEngine.GOOGLE_SAFE
    ): String {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return searchEngine.homeUrl
        if (trimmed.equals(NEW_TAB_INTERNAL_URL, ignoreCase = true) ||
            trimmed.equals("about:newtab", ignoreCase = true)
        ) {
            return NEW_TAB_INTERNAL_URL
        }
        if (trimmed.startsWith("http://", true) || trimmed.startsWith("https://", true)) {
            return enforceSafeSearchQuery(trimmed)
        }
        if (trimmed.contains('.') && !trimmed.contains(' ')) {
            return enforceSafeSearchQuery("https://$trimmed")
        }
        return searchEngine.buildSearchUrl(trimmed)
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
            if (host.contains("search.brave.com") && !rawUrl.contains("safesearch=")) {
                return uri.buildUpon().appendQueryParameter("safesearch", "strict").build().toString()
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
        if (rawUrl.equals(NEW_TAB_INTERNAL_URL, ignoreCase = true) ||
            rawUrl.startsWith("about:", ignoreCase = true)
        ) {
            return null
        }
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
     * Autonomous Dual-Mode Decision (applies equally in Regular and Incognito tabs):
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
            lower.startsWith("javascript:") ||
            lower.startsWith("sheildlenz://")
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
