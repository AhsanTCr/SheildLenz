package com.example.ui.browser

import android.Manifest
import android.app.DownloadManager
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddHome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.blocking.overlay.SystemOverlayController
import com.example.core.alerts.DiscreetAlertFormatter
import com.example.core.audio.AegisSoundEffects
import com.example.data.preferences.AegisPreferences
import com.example.data.repository.BlockedDnsLogRepository
import com.example.data.repository.ProtectionRepository
import com.example.services.protection.AegisLockdownService
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberCardTone
import com.example.ui.components.SafeBrowserBrandIcon
import com.example.ui.components.SheildlenzBackdrop
import com.example.ui.components.vipAnimatedCorners
import com.example.ui.theme.AegisCardBackground
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisChampagneSoft
import com.example.ui.theme.AegisElectricBlue
import com.example.ui.theme.AegisEmeraldTeal
import com.example.ui.theme.AegisGoldBorder
import com.example.ui.theme.AegisLavenderBorder
import com.example.ui.theme.AegisLavenderSoft
import com.example.ui.theme.AegisOnPrimaryWhite
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisPurple
import com.example.ui.theme.AegisSurfaceElevated
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.AegisWarningAmber
import com.example.ui.theme.ClassicalBodyFont
import com.example.ui.theme.ClassicalDisplayFont
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream

private data class PendingSitePermissionPrompt(
    val origin: String,
    val type: SitePermissionType,
    val webRequest: PermissionRequest? = null,
    val geoCallback: GeolocationPermissions.Callback? = null
)

@Composable
fun SafeBrowserScreen(
    preferences: AegisPreferences,
    protectionRepo: ProtectionRepository,
    blockedDnsLogRepo: BlockedDnsLogRepository,
    overlayController: SystemOverlayController,
    pendingExternalUrl: String? = null,
    onConsumePendingUrl: () -> Unit = {},
    isStandaloneWindow: Boolean = false,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    val telemetry by protectionRepo.telemetry.collectAsState()
    val customDomains by preferences.customBlockedDomains.collectAsState()
    val customKeywords by preferences.customBlockedKeywords.collectAsState()
    val blockAdult by preferences.blockExplicitAdult.collectAsState()
    val blockGambling by preferences.blockGamblingKeywords.collectAsState()
    val smartSafeActive by preferences.smartSafeSearchEnabled.collectAsState()
    val safeSearchPacks by preferences.safeSearchPacks.collectAsState()
    val discreetMode by preferences.discreetBlockingScreenMode.collectAsState()
    val fullBrowserInstalled by preferences.fullBrowserEngineInstalled.collectAsState()
    val desktopDefault by preferences.browserDesktopModeDefault.collectAsState()
    val bookmarksJson by preferences.browserBookmarksJson.collectAsState()
    val historyJson by preferences.browserHistoryJson.collectAsState()
    val downloadsJson by preferences.browserDownloadsJson.collectAsState()
    val chromeImportedCount by preferences.chromeImportedFileCount.collectAsState()
    val chromeComponentsCached by preferences.chromeComponentsAutoCached.collectAsState()
    val chromeCachedCount by preferences.chromeCachedComponentCount.collectAsState()
    val searchEngine by preferences.browserSearchEngine.collectAsState()
    val adBlockEnabled by preferences.browserAdBlockEnabled.collectAsState()
    val popupsBlocked by preferences.browserPopupsBlocked.collectAsState()
    val javaScriptEnabled by preferences.browserJavaScriptEnabled.collectAsState()
    val sitePermissionsJson by preferences.browserSitePermissionsJson.collectAsState()
    val masterPowerEnabled by preferences.masterPowerEnabled.collectAsState()
    val shieldDesiredActive by preferences.isShieldDesiredActive.collectAsState()
    val studyModeActive by preferences.studyModeEnabled.collectAsState()

    val isSheildlenzActive = masterPowerEnabled &&
        (telemetry.isProtectionEnabled || shieldDesiredActive || (studyModeActive && preferences.isStudySessionActive()))

    val latestShieldActive by rememberUpdatedState(isSheildlenzActive)
    val latestDiscreetMode by rememberUpdatedState(discreetMode)
    val latestCustomDomains by rememberUpdatedState(customDomains)
    val latestCustomKeywords by rememberUpdatedState(customKeywords)
    val latestBlockAdult by rememberUpdatedState(blockAdult)
    val latestBlockGambling by rememberUpdatedState(blockGambling)
    val latestSmartSafe by rememberUpdatedState(smartSafeActive)
    val latestPacks by rememberUpdatedState(safeSearchPacks)
    val latestAdBlock by rememberUpdatedState(adBlockEnabled)
    val latestSitePermissionsJson by rememberUpdatedState(sitePermissionsJson)

    val bookmarks = remember(bookmarksJson) { SafeBrowserFilterHelper.deserializeBookmarks(bookmarksJson) }
    val historyEntries = remember(historyJson) { SafeBrowserFilterHelper.deserializeHistory(historyJson) }
    val downloadItems = remember(downloadsJson) { SafeBrowserFilterHelper.deserializeDownloads(downloadsJson) }

    var isDefaultBrowserHeld by remember { mutableStateOf(HomeShortcutHelper.isDefaultBrowserRoleHeld(context)) }
    DisposableEffect(lifecycleOwner, context) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isDefaultBrowserHeld = HomeShortcutHelper.isDefaultBrowserRoleHeld(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    var tabs by remember {
        mutableStateOf(
            listOf(
                BrowserTabState(
                    title = "Safe Search",
                    url = searchEngine.homeUrl,
                    desktopMode = desktopDefault
                )
            )
        )
    }
    var activeTabId by remember { mutableStateOf(tabs.first().id) }
    val activeTab = tabs.firstOrNull { it.id == activeTabId } ?: tabs.first()

    var urlInput by remember(activeTab.id) {
        mutableStateOf(if (activeTab.isNewTabPage) "" else activeTab.url)
    }
    var blockedState by remember { mutableStateOf<BlockedState?>(null) }
    var quietNoticeBanner by remember { mutableStateOf<String?>(null) }
    var showPinHomeBanner by remember { mutableStateOf(false) }
    var activeSheet by remember { mutableStateOf(BrowserDrawerSheet.NONE) }
    var showFindBar by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    val webViewHolder = remember { arrayOfNulls<WebView>(1) }
    var defaultUserAgent by remember { mutableStateOf<String?>(null) }
    var webViewRecoveryEpoch by remember { mutableIntStateOf(0) }
    var lastRendererCrashElapsedMs by remember { mutableLongStateOf(0L) }

    var showUpgradeConfirmDialog by remember { mutableStateOf(false) }
    var isUpgradingEngine by remember { mutableStateOf(false) }
    var upgradeProgress by remember { mutableFloatStateOf(0f) }
    var isFullScreen by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showTabGridDialog by remember { mutableStateOf(false) }
    var showSitePermissionsDialog by remember { mutableStateOf(false) }
    var showBrowserSettingsDialog by remember { mutableStateOf(false) }

    var pendingFileChooserCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var pendingSitePrompt by remember { mutableStateOf<PendingSitePermissionPrompt?>(null) }
    var pendingRuntimePermissionCallback by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }
    var customFullscreenView by remember { mutableStateOf<View?>(null) }
    var customFullscreenCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    val latestActiveTabId by rememberUpdatedState(activeTab.id)
    val latestActiveTabIncognito by rememberUpdatedState(activeTab.isIncognito)
    val latestTabs by rememberUpdatedState(tabs)

    val recordHistory: (String, String) -> Unit = remember {
        { url, title ->
            if (!latestActiveTabIncognito &&
                (url.startsWith("http://", true) || url.startsWith("https://", true))
            ) {
                val cleanTitle = title.ifBlank { Uri.parse(url).host ?: "Web Page" }
                val existing = SafeBrowserFilterHelper.deserializeHistory(preferences.browserHistoryJson.value)
                val updated = listOf(BrowserHistoryEntry(url = url, title = cleanTitle)) +
                    existing.filterNot { it.url == url && System.currentTimeMillis() - it.visitedAtMillis < 10_000L }
                preferences.setBrowserHistoryJson(SafeBrowserFilterHelper.serializeHistory(updated))
            }
        }
    }

    val recordDownload: (BrowserDownloadItem) -> Unit = remember {
        { item ->
            val existing = SafeBrowserFilterHelper.deserializeDownloads(preferences.browserDownloadsJson.value)
            preferences.setBrowserDownloadsJson(
                SafeBrowserFilterHelper.serializeDownloads(listOf(item) + existing)
            )
        }
    }

    val chromeFileImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.onSuccess { raw ->
                if (!raw.isNullOrBlank()) {
                    val parsed = SafeBrowserFilterHelper.parseChromeBookmarksFile(raw)
                    if (parsed.isNotEmpty()) {
                        val existing = SafeBrowserFilterHelper.deserializeBookmarks(preferences.browserBookmarksJson.value)
                        val existingUrls = existing.map { it.url }.toSet()
                        val merged = parsed.filterNot { existingUrls.contains(it.url) } + existing
                        preferences.setBrowserBookmarksJson(SafeBrowserFilterHelper.serializeBookmarks(merged))
                        preferences.setChromeImportedFileCount(preferences.chromeImportedFileCount.value + parsed.size)
                        Toast.makeText(context, "Imported ${parsed.size} Chrome bookmarks", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "No valid Chrome bookmarks found in selected file", Toast.LENGTH_SHORT).show()
                    }
                }
            }.onFailure {
                Toast.makeText(context, "Unable to read selected Chrome file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val webFileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        val cb = pendingFileChooserCallback
        pendingFileChooserCallback = null
        cb?.onReceiveValue(uris.toTypedArray())
    }

    val runtimePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants.isNotEmpty() && grants.values.all { it }
        val cb = pendingRuntimePermissionCallback
        pendingRuntimePermissionCallback = null
        cb?.invoke(granted)
    }

    val roleRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        isDefaultBrowserHeld = HomeShortcutHelper.isDefaultBrowserRoleHeld(context)
        Toast.makeText(
            context,
            if (isDefaultBrowserHeld) "Sheildlenz is now your Default Browser" else "Default browser preference updated",
            Toast.LENGTH_SHORT
        ).show()
    }

    fun requestDefaultBrowserRole() {
        AegisSoundEffects.playTap(context)
        preferences.beginAuthorizedExternalFlow()
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val rm = context.getSystemService(RoleManager::class.java)
                if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_BROWSER) && !rm.isRoleHeld(RoleManager.ROLE_BROWSER)) {
                    roleRequestLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_BROWSER))
                } else {
                    context.startActivity(
                        Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                }
            } else {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
        }.onFailure {
            Toast.makeText(context, "Open system Default Apps settings to pick Sheildlenz", Toast.LENGTH_SHORT).show()
        }
    }

    fun triggerPinStandaloneBrowser() {
        val triggered = HomeShortcutHelper.pinStandaloneBrowserToHome(context)
        showPinHomeBanner = false
        Toast.makeText(
            context,
            if (triggered) "Add Sheildlenz Browser icon via your Home Screen prompt" else "Pinned shortcut created in launcher shortcuts",
            Toast.LENGTH_SHORT
        ).show()
    }

    fun requestAndroidPermissionsForSite(
        type: SitePermissionType,
        onResult: (Boolean) -> Unit
    ) {
        val needed = when (type) {
            SitePermissionType.CAMERA -> arrayOf(Manifest.permission.CAMERA)
            SitePermissionType.MICROPHONE -> arrayOf(Manifest.permission.RECORD_AUDIO)
            SitePermissionType.LOCATION -> arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
        val alreadyGranted = needed.any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        } && (type != SitePermissionType.CAMERA && type != SitePermissionType.MICROPHONE ||
            ContextCompat.checkSelfPermission(context, needed.first()) == PackageManager.PERMISSION_GRANTED)

        if (alreadyGranted) {
            onResult(true)
        } else {
            pendingRuntimePermissionCallback = onResult
            runtimePermissionLauncher.launch(needed)
        }
    }

    fun applyUserAgent(wv: WebView, desktop: Boolean) {
        val baseUa = defaultUserAgent ?: wv.settings.userAgentString.also { defaultUserAgent = it }
        val targetUa = if (desktop) {
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
        } else {
            baseUa
        }
        if (wv.settings.userAgentString != targetUa) {
            wv.settings.userAgentString = targetUa
        }
        if (wv.settings.useWideViewPort != desktop) {
            wv.settings.useWideViewPort = desktop
        }
        if (wv.settings.loadWithOverviewMode != desktop) {
            wv.settings.loadWithOverviewMode = desktop
        }
    }

    fun selectTab(tab: BrowserTabState) {
        activeTabId = tab.id
        urlInput = if (tab.isNewTabPage) "" else tab.url
        blockedState = null
        if (!tab.isNewTabPage) {
            webViewHolder[0]?.let { wv ->
                applyUserAgent(wv, tab.desktopMode)
                wv.loadUrl(tab.url)
            }
        }
    }

    fun openNewTab(incognito: Boolean) {
        val created = BrowserTabState(
            title = if (incognito) "Incognito Tab" else "New Tab ${tabs.size + 1}",
            url = NEW_TAB_INTERNAL_URL,
            desktopMode = desktopDefault,
            isIncognito = incognito,
            isNewTabPage = true
        )
        tabs = tabs + created
        activeTabId = created.id
        urlInput = ""
        blockedState = null
    }

    fun closeTab(tab: BrowserTabState) {
        val hadIncognitoBefore = tabs.any { it.isIncognito }
        val (nextTabs, nextId) = SafeBrowserFilterHelper.closeTabAndEnsureFallback(tabs, tab.id)
        tabs = nextTabs
        activeTabId = nextId
        val nextActive = nextTabs.first { it.id == nextId }
        urlInput = if (nextActive.isNewTabPage) "" else nextActive.url
        blockedState = null
        if (hadIncognitoBefore && nextTabs.none { it.isIncognito }) {
            runCatching { CookieManager.getInstance().flush() }
        }
        if (!nextActive.isNewTabPage) {
            webViewHolder[0]?.let { wv ->
                applyUserAgent(wv, nextActive.desktopMode)
                wv.loadUrl(nextActive.url)
            }
        }
    }

    fun navigateToUrl(rawInput: String, targetTabId: String = activeTab.id) {
        val resolved = SafeBrowserFilterHelper.resolveSafeNavigationUrl(rawInput, searchEngine)
        if (resolved == NEW_TAB_INTERNAL_URL) {
            blockedState = null
            urlInput = ""
            tabs = tabs.map { t ->
                if (t.id == targetTabId) t.copy(title = if (t.isIncognito) "Incognito" else "Speed Dial", url = NEW_TAB_INTERNAL_URL, isNewTabPage = true) else t
            }
            return
        }

        val decision = SafeBrowserFilterHelper.decideActionForNavigation(
            rawUrl = resolved,
            isSheildlenzActive = latestShieldActive,
            customDomains = latestCustomDomains,
            customKeywords = latestCustomKeywords,
            blockAdult = latestBlockAdult,
            blockGambling = latestBlockGambling,
            smartSafeActive = latestSmartSafe,
            safeSearchPacks = latestPacks
        )
        when (decision.action) {
            BrowserShieldAction.CLOSE_TAB_QUIETLY -> {
                val (nextTabs, nextActiveId) = SafeBrowserFilterHelper.closeTabAndEnsureFallback(tabs, targetTabId)
                tabs = nextTabs
                activeTabId = nextActiveId
                val nextActive = nextTabs.firstOrNull { it.id == nextActiveId }
                val fallbackUrl = nextActive?.url ?: searchEngine.homeUrl
                urlInput = if (nextActive?.isNewTabPage == true) "" else fallbackUrl
                blockedState = null
                quietNoticeBanner = DiscreetAlertFormatter.tabAutoClosedNotice(latestDiscreetMode)
                if (nextActive?.isNewTabPage != true) {
                    webViewHolder[0]?.loadUrl(fallbackUrl)
                }
            }
            BrowserShieldAction.FULL_SHIELD_ENFORCEMENT -> {
                val reason = decision.matchedReason ?: "Blocked by Sheildlenz"
                blockedState = BlockedState(url = resolved, reason = reason)
                runCatching {
                    val category = when {
                        reason.contains("Gambling", ignoreCase = true) -> BlockedDnsLogRepository.CATEGORY_GAMBLING
                        reason.contains("Keyword", ignoreCase = true) -> BlockedDnsLogRepository.CATEGORY_CUSTOM_KEYWORD
                        else -> BlockedDnsLogRepository.CATEGORY_ADULT
                    }
                    blockedDnsLogRepo.recordBlockedQuery(
                        domain = Uri.parse(resolved).host ?: resolved,
                        reasonCategory = category,
                        matchedRule = reason
                    )
                    preferences.incrementSafeSearchBlockedCount()
                    val esc = preferences.recordThreatAndCheckLockdown()
                    if (esc.shouldLockdown) {
                        val incident = preferences.engageFullDeviceLockdown(
                            appName = "Safe Browser",
                            packageName = context.packageName,
                            categoryName = category,
                            details = reason
                        )
                        AegisLockdownService.start(context)
                        overlayController.showLockdownOverlay(
                            preferences.lockdownIncidentInfo.value.copy(detectedAtMillis = incident)
                        )
                    } else {
                        overlayController.showWarningOverlay(
                            appName = "Safe Browser",
                            packageName = context.packageName,
                            categoryName = reason,
                            warningNumber = esc.warningNumber,
                            maxWarnings = esc.maxWarnings,
                            warningOnly = esc.isWarningOnly
                        )
                    }
                }
            }
            BrowserShieldAction.NONE -> {
                quietNoticeBanner = null
                blockedState = null
                urlInput = resolved
                tabs = tabs.map { t ->
                    if (t.id == targetTabId) t.copy(url = resolved, isNewTabPage = false) else t
                }
                webViewHolder[0]?.loadUrl(resolved)
            }
        }
    }

    LaunchedEffect(pendingExternalUrl) {
        val extUrl = pendingExternalUrl?.trim()
        if (!extUrl.isNullOrBlank()) {
            if (extUrl.equals(NEW_TAB_INTERNAL_URL, ignoreCase = true)) {
                openNewTab(incognito = false)
            } else {
                val targetId = if ((fullBrowserInstalled || isStandaloneWindow) && tabs.size < 16) {
                    val newTab = BrowserTabState(title = "Opened Link", url = extUrl, desktopMode = desktopDefault)
                    tabs = tabs + newTab
                    activeTabId = newTab.id
                    newTab.id
                } else {
                    activeTab.id
                }
                navigateToUrl(extUrl, targetId)
            }
            onConsumePendingUrl()
        }
    }

    LaunchedEffect(quietNoticeBanner) {
        if (quietNoticeBanner != null) {
            delay(3600L)
            quietNoticeBanner = null
        }
    }

    BackHandler {
        if (customFullscreenView != null) {
            customFullscreenCallback?.onCustomViewHidden()
            customFullscreenView = null
            customFullscreenCallback = null
            return@BackHandler
        }
        if (isFullScreen) {
            isFullScreen = false
            return@BackHandler
        }
        if (showBrowserSettingsDialog) {
            showBrowserSettingsDialog = false
            return@BackHandler
        }
        if (showSitePermissionsDialog) {
            showSitePermissionsDialog = false
            return@BackHandler
        }
        if (showTabGridDialog) {
            showTabGridDialog = false
            return@BackHandler
        }
        if (showUpgradeConfirmDialog) {
            showUpgradeConfirmDialog = false
            return@BackHandler
        }
        if (activeSheet != BrowserDrawerSheet.NONE) {
            activeSheet = BrowserDrawerSheet.NONE
            return@BackHandler
        }
        val wv = webViewHolder[0]
        if (!activeTab.isNewTabPage && wv != null && wv.canGoBack()) {
            wv.goBack()
        } else if (!activeTab.isNewTabPage && fullBrowserInstalled) {
            tabs = tabs.map {
                if (it.id == activeTab.id) it.copy(title = "Speed Dial", url = NEW_TAB_INTERNAL_URL, isNewTabPage = true) else it
            }
            urlInput = ""
        } else {
            onNavigateBack()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mainHandler.removeCallbacksAndMessages(null)
            runCatching {
                webViewHolder[0]?.apply {
                    stopLoading()
                    webChromeClient = null
                    (parent as? ViewGroup)?.removeView(this)
                    removeAllViews()
                    destroy()
                }
                webViewHolder[0] = null
            }
        }
    }

    // Auto-hide the Sheildlenz app header once Full Pro or Default Browser is active or inside Standalone BrowserActivity
    val showAppHeader = !isStandaloneWindow && !isFullScreen && !fullBrowserInstalled && !isDefaultBrowserHeld
    val isCurrentIncognito = activeTab.isIncognito

    SheildlenzBackdrop(modifier = Modifier.testTag("safe_browser_root")) {
        Scaffold(
            contentWindowInsets = WindowInsets.statusBars,
            topBar = {
                if (showAppHeader) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .vipAnimatedCorners(
                                cornerRadius = 20.dp,
                                accentStart = AegisPrimaryCyan,
                                accentEnd = AegisPurple,
                                strokeWidth = 2.dp
                            ),
                        shape = RoundedCornerShape(20.dp),
                        color = AegisCardBackground.copy(alpha = 0.94f),
                        border = BorderStroke(1.dp, AegisLavenderBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                IconButton(
                                    onClick = onNavigateBack,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .testTag("safe_browser_back_home")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back to Home",
                                        tint = AegisPurple
                                    )
                                }
                                SafeBrowserBrandIcon(size = 34.dp, isProUnlocked = fullBrowserInstalled)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Safe Browser",
                                            color = AegisTextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = ClassicalDisplayFont,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .background(AegisPrimaryCyan.copy(alpha = 0.14f))
                                                .padding(horizontal = 7.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "LIGHT",
                                                color = AegisPrimaryCyan,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Text(
                                        text = if (isSheildlenzActive) {
                                            "Shield ON • Full Protection Rules"
                                        } else {
                                            "Shield OFF • Quiet Auto-Close Tab Guard"
                                        },
                                        color = if (isSheildlenzActive) AegisEmeraldTeal else AegisTextSecondary,
                                        fontSize = 10.5.sp,
                                        fontFamily = ClassicalBodyFont,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = { requestDefaultBrowserRole() },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, AegisLavenderBorder),
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("safe_browser_set_default_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = "Set Default Browser",
                                    tint = AegisPurple,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Default",
                                    color = AegisPurple,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = if (isFullScreen || fullBrowserInstalled || isStandaloneWindow || isDefaultBrowserHeld) 4.dp else 10.dp,
                            vertical = if (isFullScreen) 0.dp else 4.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Light-to-Full Upgrade Card
                    AnimatedVisibility(
                        visible = !fullBrowserInstalled && !isFullScreen,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        CyberCard(
                            borderColor = AegisGoldBorder,
                            tone = CyberCardTone.GOLD,
                            modifier = Modifier.testTag("safe_browser_upgrade_card")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 10.dp)
                                ) {
                                    Text(
                                        text = "Upgrade to Full Browser Engine",
                                        color = AegisTextPrimary,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = ClassicalDisplayFont
                                    )
                                    Text(
                                        text = "1-Tap Chrome Engine, Speed Dial, Tabs, Incognito, PDF, Permissions & Home Icon",
                                        color = AegisTextSecondary,
                                        fontSize = 11.sp,
                                        fontFamily = ClassicalBodyFont
                                    )
                                }
                                Button(
                                    onClick = {
                                        if (!isUpgradingEngine) {
                                            AegisSoundEffects.playTap(context)
                                            showUpgradeConfirmDialog = true
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AegisPurple,
                                        contentColor = AegisOnPrimaryWhite
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("safe_browser_upgrade_button")
                                ) {
                                    Icon(Icons.Default.Upgrade, contentDescription = "Upgrade Browser", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isUpgradingEngine) "INSTALLING" else "DOWNLOAD PRO",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                            if (isUpgradingEngine) {
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { upgradeProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = AegisPurple,
                                    trackColor = AegisPurple.copy(alpha = 0.2f)
                                )
                            }
                        }
                    }

                    // Inline Pin Browser Prompt Banner after upgrade
                    AnimatedVisibility(
                        visible = showPinHomeBanner && !isFullScreen,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFEAF6FF),
                            border = BorderStroke(1.2.dp, AegisPrimaryCyan),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("safe_browser_pin_prompt_banner")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SafeBrowserBrandIcon(size = 28.dp, isProUnlocked = true)
                                Text(
                                    text = "Pin Sheildlenz Browser icon to Home Screen",
                                    color = AegisTextPrimary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = { triggerPinStandaloneBrowser() },
                                    colors = ButtonDefaults.buttonColors(containerColor = AegisPrimaryCyan, contentColor = AegisOnPrimaryWhite),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("Pin", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                IconButton(
                                    onClick = { showPinHomeBanner = false },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss pin banner", modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                    }

                    // Quiet Auto-Closed Notice Banner
                    AnimatedVisibility(
                        visible = quietNoticeBanner != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = AegisChampagneSoft,
                            border = BorderStroke(1.dp, AegisGoldBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .vipAnimatedCorners(
                                    cornerRadius = 14.dp,
                                    accentStart = AegisChampagneGold,
                                    accentEnd = AegisPurple,
                                    strokeWidth = 1.6.dp
                                )
                                .testTag("safe_browser_quiet_tab_closed_banner")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Shield, contentDescription = null, tint = AegisChampagneGold, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = quietNoticeBanner.orEmpty(),
                                        color = AegisTextPrimary,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = ClassicalBodyFont
                                    )
                                }
                                IconButton(onClick = { quietNoticeBanner = null }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = AegisTextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    if (!isFullScreen) {
                        // Chrome-Style Edge-to-Edge Omnibox
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(4.dp, RoundedCornerShape(18.dp))
                                .vipAnimatedCorners(
                                    cornerRadius = 18.dp,
                                    accentStart = if (isCurrentIncognito) IncognitoAccent else AegisElectricBlue,
                                    accentEnd = AegisPurple,
                                    strokeWidth = 1.8.dp
                                )
                                .testTag("safe_browser_toolbar"),
                            shape = RoundedCornerShape(18.dp),
                            color = if (isCurrentIncognito) Color(0xFFE0E7FF) else AegisLavenderSoft,
                            border = BorderStroke(1.dp, if (isCurrentIncognito) IncognitoAccent else AegisLavenderBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Site Lock / Brand Icon / Permissions Inspector
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White,
                                        border = BorderStroke(1.dp, AegisCardBorder),
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clickable {
                                                AegisSoundEffects.playTap(context)
                                                showSitePermissionsDialog = true
                                            }
                                            .testTag("safe_browser_site_lock_button")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            when {
                                                isCurrentIncognito -> Icon(
                                                    imageVector = Icons.Default.VisibilityOff,
                                                    contentDescription = "Incognito Site Info",
                                                    tint = IncognitoIndigo,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                activeTab.isNewTabPage -> SafeBrowserBrandIcon(
                                                    size = 26.dp,
                                                    isProUnlocked = fullBrowserInstalled
                                                )
                                                else -> Icon(
                                                    imageVector = Icons.Default.Lock,
                                                    contentDescription = "Site Permissions & HTTPS",
                                                    tint = if (activeTab.url.startsWith("https://", true)) AegisEmeraldTeal else AegisChampagneGold,
                                                    modifier = Modifier.size(17.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Edge-to-Edge Search & Address Pill
                                    OutlinedTextField(
                                        value = urlInput,
                                        onValueChange = { urlInput = it },
                                        placeholder = {
                                            Text(
                                                text = if (isCurrentIncognito) {
                                                    "Incognito • ${searchEngine.displayName}..."
                                                } else {
                                                    "Search ${searchEngine.displayName} or type URL"
                                                },
                                                fontSize = 11.5.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 44.dp)
                                            .testTag("safe_browser_url_input"),
                                        shape = RoundedCornerShape(14.dp),
                                        singleLine = true,
                                        trailingIcon = {
                                            IconButton(
                                                onClick = {
                                                    val query = urlInput.trim()
                                                    if (query.isNotEmpty() && query != activeTab.url) {
                                                        navigateToUrl(query)
                                                    } else if (!activeTab.isNewTabPage) {
                                                        webViewHolder[0]?.reload()
                                                    } else {
                                                        navigateToUrl(searchEngine.homeUrl)
                                                    }
                                                },
                                                modifier = Modifier.testTag("safe_browser_go_button")
                                            ) {
                                                Icon(
                                                    imageVector = if (urlInput.trim() == activeTab.url && !activeTab.isNewTabPage) {
                                                        Icons.Default.Refresh
                                                    } else {
                                                        Icons.Default.Search
                                                    },
                                                    contentDescription = "Search or Reload",
                                                    tint = if (isCurrentIncognito) IncognitoAccent else AegisPrimaryCyan,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        },
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                        keyboardActions = KeyboardActions(
                                            onSearch = {
                                                val query = urlInput.trim()
                                                if (query.isNotEmpty()) {
                                                    navigateToUrl(query)
                                                }
                                            }
                                        ),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = if (isCurrentIncognito) IncognitoAccent else AegisPrimaryCyan,
                                            unfocusedBorderColor = AegisCardBorder,
                                            focusedContainerColor = Color.White,
                                            unfocusedContainerColor = Color.White.copy(alpha = 0.90f)
                                        ),
                                        textStyle = TextStyle(
                                            fontSize = 12.sp,
                                            fontFamily = ClassicalBodyFont,
                                            color = AegisTextPrimary
                                        )
                                    )

                                    // Chrome-Style [N] Tabs Switcher Counter
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isCurrentIncognito) IncognitoIndigo else Color.White,
                                        border = BorderStroke(1.6.dp, if (isCurrentIncognito) IncognitoAccent else AegisPurple),
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clickable {
                                                AegisSoundEffects.playTap(context)
                                                showTabGridDialog = true
                                            }
                                            .testTag("safe_browser_tabs_counter_button")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${tabs.size}",
                                                color = if (isCurrentIncognito) Color.White else AegisPurple,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }

                                    // Overflow Menu (⋮)
                                    Box {
                                        IconButton(
                                            onClick = { showOverflowMenu = true },
                                            modifier = Modifier
                                                .size(38.dp)
                                                .testTag("safe_browser_overflow_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = "Browser Menu",
                                                tint = if (isCurrentIncognito) IncognitoIndigo else AegisPurple
                                            )
                                        }

                                        DropdownMenu(
                                            expanded = showOverflowMenu,
                                            onDismissRequest = { showOverflowMenu = false }
                                        ) {
                                            // Top Quick Nav Strip inside Chrome Menu
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceEvenly
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        showOverflowMenu = false
                                                        webViewHolder[0]?.goBack()
                                                    },
                                                    enabled = activeTab.canGoBack,
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AegisPurple)
                                                }
                                                IconButton(
                                                    onClick = {
                                                        showOverflowMenu = false
                                                        webViewHolder[0]?.goForward()
                                                    },
                                                    enabled = activeTab.canGoForward,
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward", tint = AegisPurple)
                                                }
                                                IconButton(
                                                    onClick = {
                                                        showOverflowMenu = false
                                                        webViewHolder[0]?.reload()
                                                    },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = AegisPrimaryCyan)
                                                }
                                                val isSaved = bookmarks.any { it.url == activeTab.url }
                                                IconButton(
                                                    onClick = {
                                                        showOverflowMenu = false
                                                        val next = if (isSaved) {
                                                            bookmarks.filterNot { it.url == activeTab.url }
                                                        } else {
                                                            listOf(SafeBookmark(title = activeTab.title.take(36), url = activeTab.url)) + bookmarks
                                                        }
                                                        preferences.setBrowserBookmarksJson(SafeBrowserFilterHelper.serializeBookmarks(next))
                                                        Toast.makeText(context, if (isSaved) "Removed bookmark" else "Saved bookmark", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                                        contentDescription = "Bookmark Page",
                                                        tint = AegisPurple
                                                    )
                                                }
                                            }
                                            HorizontalDivider(color = AegisCardBorder)
                                            DropdownMenuItem(
                                                text = { Text("New Tab (Speed Dial)") },
                                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                                                onClick = { showOverflowMenu = false; openNewTab(incognito = false) }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("New Incognito Tab") },
                                                leadingIcon = { Icon(Icons.Default.VisibilityOff, contentDescription = null) },
                                                onClick = { showOverflowMenu = false; openNewTab(incognito = true) }
                                            )
                                            DropdownMenuItem(
                                                text = { Text(if (activeTab.desktopMode) "Mobile Site" else "Desktop Site") },
                                                leadingIcon = { Icon(Icons.Default.DesktopWindows, contentDescription = null) },
                                                onClick = {
                                                    showOverflowMenu = false
                                                    val nextDesktop = !activeTab.desktopMode
                                                    preferences.setBrowserDesktopModeDefault(nextDesktop)
                                                    tabs = tabs.map { if (it.id == activeTab.id) it.copy(desktopMode = nextDesktop) else it }
                                                    webViewHolder[0]?.let { wv -> applyUserAgent(wv, nextDesktop); wv.reload() }
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Fullscreen View") },
                                                leadingIcon = { Icon(Icons.Default.Fullscreen, contentDescription = null) },
                                                onClick = { showOverflowMenu = false; isFullScreen = true }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Find in Page") },
                                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                                onClick = { showOverflowMenu = false; showFindBar = !showFindBar }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Site Permissions (Cam / Mic / Loc)") },
                                                leadingIcon = { Icon(Icons.Default.Security, contentDescription = null) },
                                                onClick = { showOverflowMenu = false; showSitePermissionsDialog = true }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Save Webpage as PDF") },
                                                leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                                                onClick = {
                                                    showOverflowMenu = false
                                                    val saved = BrowserPdfExportHelper.saveWebPageAsPdf(context, webViewHolder[0], activeTab.title, activeTab.url)
                                                    if (saved != null) {
                                                        recordDownload(saved)
                                                        Toast.makeText(context, "Saved ${saved.fileName} in Downloads", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(context, "Unable to export PDF from this page", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Print Webpage") },
                                                leadingIcon = { Icon(Icons.Default.Print, contentDescription = null) },
                                                onClick = {
                                                    showOverflowMenu = false
                                                    if (!BrowserPdfExportHelper.printWebPage(context, webViewHolder[0], activeTab.title)) {
                                                        Toast.makeText(context, "Open a webpage first to print", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Add Site to Home Screen") },
                                                leadingIcon = { Icon(Icons.Default.AddHome, contentDescription = null) },
                                                onClick = {
                                                    showOverflowMenu = false
                                                    val pinned = HomeShortcutHelper.pinWebsiteToHome(context, activeTab.title, activeTab.url)
                                                    val bm = SafeBookmark(title = activeTab.title.take(36), url = activeTab.url, folder = "Home Screen Shortcuts")
                                                    val existing = SafeBrowserFilterHelper.deserializeBookmarks(preferences.browserBookmarksJson.value)
                                                    if (existing.none { it.url == bm.url }) {
                                                        preferences.setBrowserBookmarksJson(SafeBrowserFilterHelper.serializeBookmarks(listOf(bm) + existing))
                                                    }
                                                    Toast.makeText(
                                                        context,
                                                        if (pinned) "Adding '${activeTab.title}' shortcut to Home" else "Saved '${activeTab.title}' shortcut",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Pin Browser Icon to Home") },
                                                leadingIcon = { Icon(Icons.Default.AddHome, contentDescription = null) },
                                                onClick = { showOverflowMenu = false; triggerPinStandaloneBrowser() }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Set as Default Browser") },
                                                leadingIcon = { Icon(Icons.Default.Public, contentDescription = null) },
                                                onClick = { showOverflowMenu = false; requestDefaultBrowserRole() }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Browser Settings") },
                                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                                onClick = { showOverflowMenu = false; showBrowserSettingsDialog = true }
                                            )
                                            if (!isStandaloneWindow) {
                                                HorizontalDivider(color = AegisCardBorder)
                                                DropdownMenuItem(
                                                    text = { Text("Back to Sheildlenz App") },
                                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) },
                                                    onClick = { showOverflowMenu = false; onNavigateBack() }
                                                )
                                            }
                                        }
                                    }
                                }

                                // Quick Find in Page Bar
                                if (showFindBar) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = findQuery,
                                            onValueChange = { q ->
                                                findQuery = q
                                                webViewHolder[0]?.findAllAsync(q)
                                            },
                                            placeholder = { Text("Find in page...", fontSize = 11.5.sp) },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        IconButton(onClick = { webViewHolder[0]?.findNext(true) }) {
                                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Match", tint = AegisPurple)
                                        }
                                        IconButton(onClick = {
                                            showFindBar = false
                                            findQuery = ""
                                            webViewHolder[0]?.clearMatches()
                                        }) {
                                            Icon(Icons.Default.Close, contentDescription = "Close Find", tint = AegisTextSecondary)
                                        }
                                    }
                                }
                            }
                        }

                        // Compact Tab Strip + Always-Visible Pro Action Grid (Full Pro unlocked)
                        if (fullBrowserInstalled) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("safe_browser_tab_strip"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                LazyRow(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(tabs, key = { it.id }) { tab ->
                                        val isSelected = tab.id == activeTab.id
                                        Surface(
                                            shape = RoundedCornerShape(11.dp),
                                            color = when {
                                                tab.isIncognito && isSelected -> IncognitoIndigo
                                                tab.isIncognito -> Color(0xFFE0E7FF)
                                                isSelected -> AegisPrimaryCyan.copy(alpha = 0.16f)
                                                else -> Color.White.copy(alpha = 0.88f)
                                            },
                                            border = BorderStroke(
                                                width = if (isSelected) 1.4.dp else 1.dp,
                                                color = when {
                                                    tab.isIncognito -> IncognitoAccent
                                                    isSelected -> AegisPrimaryCyan
                                                    else -> AegisCardBorder
                                                }
                                            ),
                                            modifier = Modifier
                                                .clickable { selectTab(tab) }
                                                .testTag("safe_browser_tab_${tab.id}")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 5.dp, bottom = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                if (tab.isIncognito) {
                                                    Icon(
                                                        imageVector = Icons.Default.VisibilityOff,
                                                        contentDescription = null,
                                                        tint = if (isSelected) Color.White else IncognitoAccent,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                                Text(
                                                    text = tab.title.take(15),
                                                    color = when {
                                                        tab.isIncognito && isSelected -> Color.White
                                                        tab.isIncognito -> IncognitoIndigo
                                                        isSelected -> AegisPrimaryCyan
                                                        else -> AegisTextPrimary
                                                    },
                                                    fontSize = 10.5.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                IconButton(
                                                    onClick = { closeTab(tab) },
                                                    modifier = Modifier.size(18.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Close tab",
                                                        tint = if (tab.isIncognito && isSelected) Color.White else AegisTextSecondary,
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = IncognitoIndigo,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clickable { openNewTab(incognito = true) }
                                        .testTag("safe_browser_new_incognito_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.VisibilityOff,
                                            contentDescription = "New Incognito Tab",
                                            tint = AegisOnPrimaryWhite,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = AegisPurple,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clickable { openNewTab(incognito = false) }
                                        .testTag("safe_browser_new_tab_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "New Tab",
                                            tint = AegisOnPrimaryWhite,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                            }

                            // 2-Row Compact Pro Action Strip (all 6 core Pro badges fit visibly at once with zero offscreen clipping)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    BrowserToolChip(
                                        label = if (chromeComponentsCached) {
                                            "Cache (${chromeCachedCount.coerceAtLeast(ChromeComponentCacheManager.CORE_CHROME_MODULES.size)})"
                                        } else {
                                            "Auto-Cache"
                                        },
                                        icon = Icons.Default.Shield,
                                        active = chromeComponentsCached,
                                        tag = "safe_browser_chrome_cache_badge",
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            val res = ChromeComponentCacheManager.autoCacheChromeComponents(
                                                context = context,
                                                preferences = preferences,
                                                webView = webViewHolder[0]
                                            )
                                            Toast.makeText(
                                                context,
                                                "Auto-cached ${res.cachedModulesCount} Chrome components",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    )
                                    BrowserToolChip(
                                        label = "Import",
                                        icon = Icons.Default.FileOpen,
                                        active = chromeImportedCount > 0,
                                        tag = "safe_browser_import_chrome_button",
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            preferences.beginAuthorizedExternalFlow()
                                            chromeFileImportLauncher.launch(arrayOf("text/html", "application/json", "*/*"))
                                        }
                                    )
                                    BrowserToolChip(
                                        label = "Bookmarks (${bookmarks.size})",
                                        icon = Icons.Default.Bookmark,
                                        active = activeSheet == BrowserDrawerSheet.BOOKMARKS,
                                        tag = "safe_browser_bookmarks_button",
                                        modifier = Modifier.weight(1f),
                                        onClick = { activeSheet = BrowserDrawerSheet.BOOKMARKS }
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    BrowserToolChip(
                                        label = "History",
                                        icon = Icons.Default.History,
                                        active = activeSheet == BrowserDrawerSheet.HISTORY,
                                        tag = "safe_browser_history_button",
                                        modifier = Modifier.weight(1f),
                                        onClick = { activeSheet = BrowserDrawerSheet.HISTORY }
                                    )
                                    BrowserToolChip(
                                        label = "Downloads",
                                        icon = Icons.Default.Download,
                                        active = activeSheet == BrowserDrawerSheet.DOWNLOADS,
                                        tag = "safe_browser_downloads_button",
                                        modifier = Modifier.weight(1f),
                                        onClick = { activeSheet = BrowserDrawerSheet.DOWNLOADS }
                                    )
                                    BrowserToolChip(
                                        label = "Settings",
                                        icon = Icons.Default.Settings,
                                        active = showBrowserSettingsDialog,
                                        tag = "safe_browser_settings_button",
                                        modifier = Modifier.weight(1f),
                                        onClick = { showBrowserSettingsDialog = true }
                                    )
                                }
                            }
                        }
                    }

                    if (activeTab.progress in 1..99 && !activeTab.isNewTabPage) {
                        LinearProgressIndicator(
                            progress = { activeTab.progress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = if (isCurrentIncognito) IncognitoAccent else AegisPrimaryCyan,
                            trackColor = AegisPrimaryCyan.copy(alpha = 0.2f)
                        )
                    }

                    // 2. Main Browser Viewport: Blocked Guard / Chrome Speed Dial NTP / WebView
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(if (isFullScreen) 0.dp else 18.dp))
                            .background(Color.White)
                            .border(
                                width = if (isFullScreen) 0.dp else 1.dp,
                                color = if (isCurrentIncognito) IncognitoAccent else AegisCardBorder,
                                shape = RoundedCornerShape(if (isFullScreen) 0.dp else 18.dp)
                            )
                            .vipAnimatedCorners(
                                cornerRadius = if (isFullScreen) 0.dp else 18.dp,
                                accentStart = if (isCurrentIncognito) IncognitoAccent else AegisPrimaryCyan,
                                accentEnd = AegisEmeraldTeal,
                                strokeWidth = if (isFullScreen) 0.dp else 1.8.dp
                            )
                    ) {
                        val currentBlock = blockedState
                        if (currentBlock != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(AegisWarningAmber.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = AegisWarningAmber,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = DiscreetAlertFormatter.browserBlockBadge(discreetMode),
                                    color = AegisPurple,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.0.sp
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = DiscreetAlertFormatter.browserBlockHeadline(discreetMode),
                                    color = AegisTextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = ClassicalDisplayFont,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = DiscreetAlertFormatter.browserBlockSubtitle(discreetMode, currentBlock.reason),
                                    color = AegisTextSecondary,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    fontFamily = ClassicalBodyFont,
                                    textAlign = TextAlign.Center
                                )

                                if (!discreetMode) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Target: ${currentBlock.url}",
                                        color = AegisPurple,
                                        fontSize = 11.5.sp,
                                        fontFamily = ClassicalBodyFont,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Button(
                                    onClick = {
                                        val home = searchEngine.homeUrl
                                        blockedState = null
                                        urlInput = home
                                        tabs = tabs.map {
                                            if (it.id == activeTab.id) it.copy(url = home, isNewTabPage = false) else it
                                        }
                                        webViewHolder[0]?.loadUrl(home)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AegisPrimaryCyan,
                                        contentColor = AegisOnPrimaryWhite
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Text("Return to Safe Search", fontWeight = FontWeight.Bold)
                                }
                            }
                        } else if (activeTab.isNewTabPage) {
                            SpeedDialNewTabCanvas(
                                isIncognito = isCurrentIncognito,
                                isProUnlocked = fullBrowserInstalled,
                                searchEngine = searchEngine,
                                bookmarks = bookmarks,
                                isShieldActive = isSheildlenzActive,
                                onSearchOrUrlSubmit = { input -> navigateToUrl(input) },
                                onOpenTabGrid = { showTabGridDialog = true },
                                onOpenNewIncognito = { openNewTab(incognito = true) },
                                onOpenBookmarks = { activeSheet = BrowserDrawerSheet.BOOKMARKS },
                                onOpenDownloads = { activeSheet = BrowserDrawerSheet.DOWNLOADS },
                                onOpenSettings = { showBrowserSettingsDialog = true },
                                onPinBrowserIcon = { triggerPinStandaloneBrowser() },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            key(webViewRecoveryEpoch) {
                                AndroidView(
                                    factory = { ctx ->
                                        WebView(ctx).apply {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, true)
                                            }
                                            settings.javaScriptEnabled = javaScriptEnabled
                                            settings.domStorageEnabled = true
                                            settings.databaseEnabled = true
                                            settings.cacheMode = WebSettings.LOAD_DEFAULT
                                            settings.loadsImagesAutomatically = true
                                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                                 settings.safeBrowsingEnabled = true
                                             }
                                            settings.setSupportZoom(true)
                                            settings.builtInZoomControls = true
                                            settings.displayZoomControls = false
                                            settings.javaScriptCanOpenWindowsAutomatically = !popupsBlocked
                                            // Keep target="_blank" navigation inside the single managed renderer
                                            settings.setSupportMultipleWindows(false)
                                            defaultUserAgent = settings.userAgentString
                                            applyUserAgent(this, activeTab.desktopMode)

                                        setDownloadListener { downloadUrl, userAgent, contentDisposition, mimetype, _ ->
                                            runCatching {
                                                val fileName = URLUtil.guessFileName(downloadUrl, contentDisposition, mimetype)
                                                val req = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                                                    setMimeType(mimetype)
                                                    addRequestHeader("User-Agent", userAgent)
                                                    setTitle(fileName)
                                                    setDescription("Safe Browser Download")
                                                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                                                }
                                                val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                                                dm?.enqueue(req)
                                                recordDownload(
                                                    BrowserDownloadItem(
                                                        fileName = fileName,
                                                        url = downloadUrl,
                                                        mimeType = mimetype ?: "application/octet-stream",
                                                        statusLabel = "Queued in Downloads"
                                                    )
                                                )
                                                Toast.makeText(ctx, "Downloading $fileName...", Toast.LENGTH_SHORT).show()
                                            }.onFailure {
                                                Toast.makeText(ctx, "Download could not be started", Toast.LENGTH_SHORT).show()
                                            }
                                        }

                                        webChromeClient = object : WebChromeClient() {
                                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                                val tabId = latestActiveTabId
                                                tabs = tabs.map {
                                                    if (it.id == tabId) {
                                                        it.copy(progress = newProgress, isLoading = newProgress in 1..99)
                                                    } else it
                                                }
                                            }

                                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                                val clean = title?.trim().orEmpty()
                                                if (clean.isNotBlank()) {
                                                    val tabId = latestActiveTabId
                                                    tabs = tabs.map {
                                                        if (it.id == tabId) it.copy(title = clean.take(36)) else it
                                                    }
                                                }
                                            }

                                            override fun onShowFileChooser(
                                                webView: WebView?,
                                                filePathCallback: ValueCallback<Array<Uri>>?,
                                                fileChooserParams: FileChooserParams?
                                            ): Boolean {
                                                pendingFileChooserCallback?.onReceiveValue(emptyArray())
                                                pendingFileChooserCallback = filePathCallback
                                                preferences.beginAuthorizedExternalFlow()
                                                val acceptTypes = fileChooserParams?.acceptTypes
                                                    ?.filter { !it.isNullOrBlank() }
                                                    ?.toTypedArray()
                                                    ?.takeIf { it.isNotEmpty() }
                                                    ?: arrayOf("*/*")
                                                webFileChooserLauncher.launch(acceptTypes)
                                                return true
                                            }

                                            override fun onPermissionRequest(request: PermissionRequest?) {
                                                if (request == null) return
                                                val resources = request.resources ?: emptyArray()
                                                val reqType = when {
                                                    resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE) -> SitePermissionType.CAMERA
                                                    resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE) -> SitePermissionType.MICROPHONE
                                                    else -> {
                                                        request.deny()
                                                        return
                                                    }
                                                }
                                                val originUrl = request.origin?.toString().orEmpty()
                                                mainHandler.post {
                                                    when (BrowserPermissionsStore.getPolicy(latestSitePermissionsJson, originUrl, reqType)) {
                                                        SitePermissionState.BLOCK -> request.deny()
                                                        SitePermissionState.ALLOW -> {
                                                            requestAndroidPermissionsForSite(reqType) { granted ->
                                                                if (granted) request.grant(resources) else request.deny()
                                                            }
                                                        }
                                                        SitePermissionState.ASK -> {
                                                            pendingSitePrompt = PendingSitePermissionPrompt(
                                                                origin = originUrl,
                                                                type = reqType,
                                                                webRequest = request
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            override fun onGeolocationPermissionsShowPrompt(
                                                origin: String?,
                                                callback: GeolocationPermissions.Callback?
                                            ) {
                                                if (callback == null || origin.isNullOrBlank()) return
                                                mainHandler.post {
                                                    when (BrowserPermissionsStore.getPolicy(latestSitePermissionsJson, origin, SitePermissionType.LOCATION)) {
                                                        SitePermissionState.BLOCK -> callback.invoke(origin, false, false)
                                                        SitePermissionState.ALLOW -> {
                                                            requestAndroidPermissionsForSite(SitePermissionType.LOCATION) { granted ->
                                                                callback.invoke(origin, granted, false)
                                                            }
                                                        }
                                                        SitePermissionState.ASK -> {
                                                            pendingSitePrompt = PendingSitePermissionPrompt(
                                                                origin = origin,
                                                                type = SitePermissionType.LOCATION,
                                                                geoCallback = callback
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                                if (view == null) {
                                                    callback?.onCustomViewHidden()
                                                    return
                                                }
                                                customFullscreenView = view
                                                customFullscreenCallback = callback
                                            }

                                            override fun onHideCustomView() {
                                                customFullscreenCallback?.onCustomViewHidden()
                                                customFullscreenView = null
                                                customFullscreenCallback = null
                                            }
                                        }

                                        webViewClient = object : WebViewClient() {
                                            private fun evaluateAndEnforce(targetUrl: String, isMainFrame: Boolean): Boolean {
                                                if (SafeBrowserFilterHelper.isExternalAppScheme(targetUrl)) {
                                                    mainHandler.post {
                                                        preferences.beginAuthorizedExternalFlow()
                                                        runCatching {
                                                            val extIntent = if (targetUrl.startsWith("intent:", ignoreCase = true)) {
                                                                Intent.parseUri(targetUrl, Intent.URI_INTENT_SCHEME)
                                                            } else {
                                                                Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                                                            }.apply {
                                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                            }
                                                            ctx.startActivity(extIntent)
                                                        }.onFailure {
                                                            runCatching {
                                                                val parsed = Intent.parseUri(targetUrl, Intent.URI_INTENT_SCHEME)
                                                                val fallback = parsed.getStringExtra("browser_fallback_url")
                                                                if (!fallback.isNullOrBlank()) {
                                                                    loadUrl(fallback)
                                                                }
                                                            }
                                                        }
                                                    }
                                                    return true
                                                }

                                                if (!isMainFrame) return false

                                                val decision = SafeBrowserFilterHelper.decideActionForNavigation(
                                                    rawUrl = targetUrl,
                                                    isSheildlenzActive = latestShieldActive,
                                                    customDomains = latestCustomDomains,
                                                    customKeywords = latestCustomKeywords,
                                                    blockAdult = latestBlockAdult,
                                                    blockGambling = latestBlockGambling,
                                                    smartSafeActive = latestSmartSafe,
                                                    safeSearchPacks = latestPacks
                                                )

                                                when (decision.action) {
                                                    BrowserShieldAction.NONE -> return false
                                                    BrowserShieldAction.CLOSE_TAB_QUIETLY -> {
                                                        mainHandler.post {
                                                            val currentList = latestTabs
                                                            val closingId = latestActiveTabId
                                                            val (nextTabs, nextId) = SafeBrowserFilterHelper.closeTabAndEnsureFallback(
                                                                currentList,
                                                                closingId
                                                            )
                                                            tabs = nextTabs
                                                            activeTabId = nextId
                                                            val nextActive = nextTabs.firstOrNull { it.id == nextId }
                                                            val safeFallbackUrl = nextActive?.url ?: searchEngine.homeUrl
                                                            urlInput = if (nextActive?.isNewTabPage == true) "" else safeFallbackUrl
                                                            blockedState = null
                                                            quietNoticeBanner = DiscreetAlertFormatter.tabAutoClosedNotice(latestDiscreetMode)
                                                            if (nextActive?.isNewTabPage != true) {
                                                                loadUrl(safeFallbackUrl)
                                                            }
                                                        }
                                                        return true
                                                    }
                                                    BrowserShieldAction.FULL_SHIELD_ENFORCEMENT -> {
                                                        val reason = decision.matchedReason ?: "Blocked by Sheildlenz"
                                                        mainHandler.post {
                                                            blockedState = BlockedState(url = targetUrl, reason = reason)
                                                            runCatching {
                                                                val category = when {
                                                                    reason.contains("Gambling", ignoreCase = true) -> BlockedDnsLogRepository.CATEGORY_GAMBLING
                                                                    reason.contains("Keyword", ignoreCase = true) -> BlockedDnsLogRepository.CATEGORY_CUSTOM_KEYWORD
                                                                    else -> BlockedDnsLogRepository.CATEGORY_ADULT
                                                                }
                                                                blockedDnsLogRepo.recordBlockedQuery(
                                                                    domain = Uri.parse(targetUrl).host ?: targetUrl,
                                                                    reasonCategory = category,
                                                                    matchedRule = reason
                                                                )
                                                                preferences.incrementSafeSearchBlockedCount()
                                                                val esc = preferences.recordThreatAndCheckLockdown()
                                                                if (esc.shouldLockdown) {
                                                                    val incident = preferences.engageFullDeviceLockdown(
                                                                        appName = "Safe Browser",
                                                                        packageName = ctx.packageName,
                                                                        categoryName = category,
                                                                        details = reason
                                                                    )
                                                                    AegisLockdownService.start(ctx)
                                                                    overlayController.showLockdownOverlay(
                                                                        preferences.lockdownIncidentInfo.value.copy(detectedAtMillis = incident)
                                                                    )
                                                                } else {
                                                                    overlayController.showWarningOverlay(
                                                                        appName = "Safe Browser",
                                                                        packageName = ctx.packageName,
                                                                        categoryName = reason,
                                                                        warningNumber = esc.warningNumber,
                                                                        maxWarnings = esc.maxWarnings,
                                                                        warningOnly = esc.isWarningOnly
                                                                    )
                                                                }
                                                            }
                                                        }
                                                        return true
                                                    }
                                                }
                                            }

                                            override fun shouldOverrideUrlLoading(
                                                view: WebView?,
                                                request: WebResourceRequest?
                                            ): Boolean {
                                                val targetUrl = request?.url?.toString() ?: return false
                                                val blocked = evaluateAndEnforce(targetUrl, request.isForMainFrame)
                                                if (blocked) return true
                                                urlInput = targetUrl
                                                tabs = tabs.map {
                                                    if (it.id == latestActiveTabId) it.copy(url = targetUrl, isNewTabPage = false) else it
                                                }
                                                return false
                                            }

                                            override fun shouldInterceptRequest(
                                                view: WebView?,
                                                request: WebResourceRequest?
                                            ): WebResourceResponse? {
                                                val url = request?.url?.toString() ?: return null
                                                if (!request.isForMainFrame && latestAdBlock && SafeBrowserFilterHelper.isTrackerOrAdDomain(url)) {
                                                    return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
                                                }
                                                if (request.isForMainFrame &&
                                                    (url.startsWith("http://", true) || url.startsWith("https://", true))
                                                ) {
                                                    val decision = SafeBrowserFilterHelper.decideActionForNavigation(
                                                        rawUrl = url,
                                                        isSheildlenzActive = latestShieldActive,
                                                        customDomains = latestCustomDomains,
                                                        customKeywords = latestCustomKeywords,
                                                        blockAdult = latestBlockAdult,
                                                        blockGambling = latestBlockGambling,
                                                        smartSafeActive = latestSmartSafe,
                                                        safeSearchPacks = latestPacks
                                                    )
                                                    if (decision.action != BrowserShieldAction.NONE) {
                                                        evaluateAndEnforce(url, isMainFrame = true)
                                                        return WebResourceResponse(
                                                            "text/plain",
                                                            "UTF-8",
                                                            ByteArrayInputStream(ByteArray(0))
                                                        )
                                                    }
                                                }
                                                return super.shouldInterceptRequest(view, request)
                                            }

                                            override fun onPageFinished(view: WebView?, url: String?) {
                                                super.onPageFinished(view, url)
                                                if (url != null) {
                                                    val title = view?.title?.takeIf { it.isNotBlank() } ?: "Safe Browser"
                                                    urlInput = url
                                                    val tabId = latestActiveTabId
                                                    tabs = tabs.map {
                                                        if (it.id == tabId) {
                                                            it.copy(
                                                                title = title.take(36),
                                                                url = url,
                                                                canGoBack = view?.canGoBack() == true,
                                                                canGoForward = view?.canGoForward() == true,
                                                                progress = 100,
                                                                isLoading = false,
                                                                isNewTabPage = false
                                                            )
                                                        } else it
                                                    }
                                                    recordHistory(url, title)
                                                }
                                            }

                                            override fun onRenderProcessGone(
                                                view: WebView?,
                                                detail: RenderProcessGoneDetail?
                                            ): Boolean {
                                                val didCrash = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                    detail?.didCrash() ?: true
                                                } else {
                                                    true
                                                }
                                                android.util.Log.w(
                                                    "SafeBrowserWebView",
                                                    "Renderer process exited (didCrash=$didCrash); recovering safely"
                                                )
                                                mainHandler.post {
                                                    if (webViewHolder[0] === view) {
                                                        webViewHolder[0] = null
                                                    }
                                                    runCatching {
                                                        view?.stopLoading()
                                                        view?.webChromeClient = null
                                                        (view?.parent as? ViewGroup)?.removeView(view)
                                                        view?.removeAllViews()
                                                        view?.destroy()
                                                    }
                                                    val nowElapsed = SystemClock.elapsedRealtime()
                                                    val rapidCrash = (nowElapsed - lastRendererCrashElapsedMs) < 4_500L
                                                    lastRendererCrashElapsedMs = nowElapsed
                                                    if (rapidCrash) {
                                                        val tabId = latestActiveTabId
                                                        tabs = tabs.map { t ->
                                                            if (t.id == tabId) {
                                                                t.copy(
                                                                    title = if (t.isIncognito) "Incognito" else "Speed Dial",
                                                                    url = NEW_TAB_INTERNAL_URL,
                                                                    isNewTabPage = true,
                                                                    isLoading = false,
                                                                    progress = 0
                                                                )
                                                            } else t
                                                        }
                                                        urlInput = ""
                                                        quietNoticeBanner = "Tab returned to Speed Dial after repeated page crash."
                                                    } else {
                                                        quietNoticeBanner = "Browser renderer recovered automatically."
                                                    }
                                                    webViewRecoveryEpoch++
                                                }
                                                return true
                                            }
                                        }

                                            loadUrl(activeTab.url)
                                            webViewHolder[0] = this
                                        }
                                    },
                                    update = { wv ->
                                        if (wv.settings.javaScriptEnabled != javaScriptEnabled) {
                                            wv.settings.javaScriptEnabled = javaScriptEnabled
                                        }
                                        val allowPopups = !popupsBlocked
                                        if (wv.settings.javaScriptCanOpenWindowsAutomatically != allowPopups) {
                                            wv.settings.javaScriptCanOpenWindowsAutomatically = allowPopups
                                        }
                                        applyUserAgent(wv, activeTab.desktopMode)
                                    },
                                    onRelease = { wv ->
                                        if (webViewHolder[0] === wv) {
                                            webViewHolder[0] = null
                                        }
                                        runCatching {
                                            wv.stopLoading()
                                            wv.webChromeClient = null
                                            (wv.parent as? ViewGroup)?.removeView(wv)
                                            wv.removeAllViews()
                                            wv.destroy()
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("safe_browser_webview")
                                )
                            }
                        }

                        // HTML5 Fullscreen Custom View Overlay (Videos)
                        val fsView = customFullscreenView
                        if (fsView != null) {
                            AndroidView(
                                factory = { ctx ->
                                    FrameLayout(ctx).apply {
                                        addView(fsView)
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                // Floating Exit Fullscreen Pill when in Fullscreen Browser Mode
                if (isFullScreen) {
                    Surface(
                        onClick = { isFullScreen = false },
                        shape = RoundedCornerShape(50),
                        color = AegisPurple.copy(alpha = 0.92f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .heightIn(min = 48.dp)
                            .testTag("safe_browser_exit_fullscreen_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.FullscreenExit, contentDescription = "Exit Fullscreen", tint = Color.White, modifier = Modifier.size(18.dp))
                            Text("Exit Fullscreen", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Bookmarks / History / Downloads Dialog
    BrowserLibraryDrawerDialog(
        activeSheet = activeSheet,
        bookmarks = bookmarks,
        historyEntries = historyEntries,
        downloadItems = downloadItems,
        onNavigateToUrl = { navigateToUrl(it) },
        onClearHistory = { preferences.setBrowserHistoryJson("") },
        onOpenDownload = { item ->
            if (!BrowserPdfExportHelper.openSavedDownload(context, item)) {
                Toast.makeText(context, "Unable to open downloaded file", Toast.LENGTH_SHORT).show()
            }
        },
        onDismiss = { activeSheet = BrowserDrawerSheet.NONE }
    )

    // 2-Column Tab Grid Switcher Dialog
    if (showTabGridDialog) {
        TabGridSwitcherDialog(
            tabs = tabs,
            activeTabId = activeTab.id,
            onSelectTab = { tab ->
                showTabGridDialog = false
                selectTab(tab)
            },
            onCloseTab = { tab -> closeTab(tab) },
            onNewRegularTab = {
                showTabGridDialog = false
                openNewTab(incognito = false)
            },
            onNewIncognitoTab = {
                showTabGridDialog = false
                openNewTab(incognito = true)
            },
            onCloseAllIncognito = {
                val filtered = tabs.filterNot { it.isIncognito }
                tabs = filtered.ifEmpty {
                    listOf(BrowserTabState(title = "Speed Dial", url = NEW_TAB_INTERNAL_URL, isNewTabPage = true, desktopMode = desktopDefault))
                }
                activeTabId = tabs.first().id
                urlInput = if (tabs.first().isNewTabPage) "" else tabs.first().url
                runCatching { CookieManager.getInstance().flush() }
            },
            onDismiss = { showTabGridDialog = false }
        )
    }

    // Per-Site Permissions Manager Dialog (Camera / Mic / Location)
    if (showSitePermissionsDialog) {
        SitePermissionsDialog(
            currentUrl = activeTab.url,
            sitePermissionsJson = sitePermissionsJson,
            isShieldActive = isSheildlenzActive,
            onUpdatePolicy = { type, nextState ->
                val updatedJson = BrowserPermissionsStore.updatePolicy(sitePermissionsJson, activeTab.url, type, nextState)
                preferences.setBrowserSitePermissionsJson(updatedJson)
            },
            onPinSiteToHome = {
                showSitePermissionsDialog = false
                val pinned = HomeShortcutHelper.pinWebsiteToHome(context, activeTab.title, activeTab.url)
                Toast.makeText(
                    context,
                    if (pinned) "Added '${activeTab.title}' shortcut" else "Open a live site first to pin",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onDismiss = { showSitePermissionsDialog = false }
        )
    }

    // One-Tap Full Browser Pro Confirmation & Auto-Caching Dialog
    if (showUpgradeConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showUpgradeConfirmDialog = false },
            containerColor = AegisLavenderSoft,
            modifier = Modifier
                .vipAnimatedCorners(
                    cornerRadius = 22.dp,
                    accentStart = AegisPrimaryCyan,
                    accentEnd = AegisPurple,
                    strokeWidth = 2.dp
                )
                .testTag("safe_browser_upgrade_confirm_dialog"),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SafeBrowserBrandIcon(size = 30.dp, isProUnlocked = true)
                    Text(
                        text = "Confirm Full Browser & Chrome Cache",
                        fontFamily = ClassicalDisplayFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = AegisTextPrimary
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "With one tap, Sheildlenz upgrades to the Full Browser Engine and automatically caches Chrome-compatible browser components:",
                        color = AegisTextSecondary,
                        fontSize = 12.5.sp,
                        fontFamily = ClassicalBodyFont
                    )
                    ChromeComponentCacheManager.CORE_CHROME_MODULES.forEach { moduleName ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(AegisEmeraldTeal)
                            )
                            Text(
                                text = moduleName,
                                color = AegisTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = ClassicalBodyFont
                            )
                        }
                    }
                    Text(
                        text = "Includes Chrome-style Speed Dial, Tab Grid, Incognito, Ad Shield, Print/PDF export, Per-Site Permissions, and Home Screen Icon pinning.",
                        color = AegisPurple,
                        fontSize = 11.sp,
                        fontFamily = ClassicalBodyFont
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUpgradeConfirmDialog = false
                        if (!isUpgradingEngine) {
                            AegisSoundEffects.playModeOn(context)
                            isUpgradingEngine = true
                            upgradeProgress = 0.12f
                            scope.launch {
                                for (step in 1..8) {
                                    delay(55L)
                                    upgradeProgress = step / 8f
                                }
                                val result = ChromeComponentCacheManager.autoCacheChromeComponents(
                                    context = context,
                                    preferences = preferences,
                                    webView = webViewHolder[0]
                                )
                                HomeShortcutHelper.pushDynamicBrowserShortcut(context)
                                if (!preferences.browserPinPromptShown.value) {
                                    preferences.setBrowserPinPromptShown(true)
                                    HomeShortcutHelper.pinStandaloneBrowserToHome(context)
                                    showPinHomeBanner = true
                                }
                                isUpgradingEngine = false
                                Toast.makeText(
                                    context,
                                    "Full Pro Ready • ${result.cachedModulesCount} Chrome components auto-cached",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AegisPurple,
                        contentColor = AegisOnPrimaryWhite
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("safe_browser_confirm_upgrade_button")
                ) {
                    Text("Confirm & Auto-Cache", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showUpgradeConfirmDialog = false },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("safe_browser_cancel_upgrade_button")
                ) {
                    Text("Stay Light", color = AegisTextSecondary)
                }
            }
        )
    }

    // Full Browser Settings Dialog
    if (showBrowserSettingsDialog) {
        BrowserSettingsFullDialog(
            preferences = preferences,
            searchEngine = searchEngine,
            adBlockEnabled = adBlockEnabled,
            popupsBlocked = popupsBlocked,
            javaScriptEnabled = javaScriptEnabled,
            desktopDefault = desktopDefault,
            smartSafeActive = smartSafeActive,
            blockAdult = blockAdult,
            chromeCachedCount = chromeCachedCount,
            sitePermissionsJson = sitePermissionsJson,
            onSearchEngineChanged = { engine ->
                preferences.setBrowserSearchEngine(engine)
                Toast.makeText(context, "Search engine set to ${engine.displayName}", Toast.LENGTH_SHORT).show()
            },
            onReCacheChrome = {
                val res = ChromeComponentCacheManager.autoCacheChromeComponents(context, preferences, webViewHolder[0])
                Toast.makeText(context, "Refreshed ${res.cachedModulesCount} Chrome modules", Toast.LENGTH_SHORT).show()
            },
            onImportChrome = {
                showBrowserSettingsDialog = false
                preferences.beginAuthorizedExternalFlow()
                chromeFileImportLauncher.launch(arrayOf("text/html", "application/json", "*/*"))
            },
            onRequestDefaultBrowser = {
                showBrowserSettingsDialog = false
                requestDefaultBrowserRole()
            },
            onPinBrowserIcon = {
                showBrowserSettingsDialog = false
                triggerPinStandaloneBrowser()
            },
            onPrintPage = {
                showBrowserSettingsDialog = false
                if (!BrowserPdfExportHelper.printWebPage(context, webViewHolder[0], activeTab.title)) {
                    Toast.makeText(context, "Open a webpage first to print", Toast.LENGTH_SHORT).show()
                }
            },
            onSavePdfPage = {
                showBrowserSettingsDialog = false
                val saved = BrowserPdfExportHelper.saveWebPageAsPdf(context, webViewHolder[0], activeTab.title, activeTab.url)
                if (saved != null) {
                    recordDownload(saved)
                    Toast.makeText(context, "Saved ${saved.fileName} in Downloads", Toast.LENGTH_SHORT).show()
                }
            },
            onClearBrowsingData = {
                clearAllBrowserSessionData(preferences, webViewHolder[0], context)
            },
            onDismiss = { showBrowserSettingsDialog = false }
        )
    }

    // In-App Site Runtime Permission Prompt (Camera / Mic / Location)
    val sitePrompt = pendingSitePrompt
    if (sitePrompt != null) {
        AlertDialog(
            onDismissRequest = {
                sitePrompt.webRequest?.deny()
                sitePrompt.geoCallback?.invoke(sitePrompt.origin, false, false)
                pendingSitePrompt = null
            },
            containerColor = AegisLavenderSoft,
            modifier = Modifier.testTag("site_runtime_prompt_dialog"),
            title = {
                Text(
                    text = "Allow ${sitePrompt.type.label}?",
                    fontFamily = ClassicalDisplayFont,
                    fontWeight = FontWeight.Bold,
                    color = AegisTextPrimary
                )
            },
            text = {
                Text(
                    text = "${sitePrompt.origin.ifBlank { "This website" }} wants to access your device ${sitePrompt.type.label.lowercase()}.",
                    color = AegisTextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingSitePrompt = null
                        val updated = BrowserPermissionsStore.updatePolicy(
                            sitePermissionsJson,
                            sitePrompt.origin,
                            sitePrompt.type,
                            SitePermissionState.ALLOW
                        )
                        preferences.setBrowserSitePermissionsJson(updated)
                        requestAndroidPermissionsForSite(sitePrompt.type) { granted ->
                            sitePrompt.webRequest?.let { req ->
                                if (granted) req.grant(req.resources) else req.deny()
                            }
                            sitePrompt.geoCallback?.invoke(sitePrompt.origin, granted, false)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AegisEmeraldTeal, contentColor = AegisOnPrimaryWhite)
                ) {
                    Text("Allow")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        pendingSitePrompt = null
                        val updated = BrowserPermissionsStore.updatePolicy(
                            sitePermissionsJson,
                            sitePrompt.origin,
                            sitePrompt.type,
                            SitePermissionState.BLOCK
                        )
                        preferences.setBrowserSitePermissionsJson(updated)
                        sitePrompt.webRequest?.deny()
                        sitePrompt.geoCallback?.invoke(sitePrompt.origin, false, false)
                    }
                ) {
                    Text("Block")
                }
            }
        )
    }
}

@Composable
private fun BrowserToolChip(
    label: String,
    icon: ImageVector,
    active: Boolean,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (active) AegisPurple else AegisSurfaceElevated,
        border = BorderStroke(1.dp, if (active) AegisPurple else AegisCardBorder),
        modifier = modifier
            .heightIn(min = 34.dp)
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) AegisOnPrimaryWhite else AegisPurple,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = if (active) AegisOnPrimaryWhite else AegisTextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
