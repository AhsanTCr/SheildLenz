package com.example.ui.browser

import android.app.DownloadManager
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.ui.theme.AegisEmeraldBorder
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class BrowserDrawerSheet {
    NONE,
    BOOKMARKS,
    HISTORY,
    DOWNLOADS
}

@Composable
fun SafeBrowserScreen(
    preferences: AegisPreferences,
    protectionRepo: ProtectionRepository,
    blockedDnsLogRepo: BlockedDnsLogRepository,
    overlayController: SystemOverlayController,
    pendingExternalUrl: String? = null,
    onConsumePendingUrl: () -> Unit = {},
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
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
    val masterPowerEnabled by preferences.masterPowerEnabled.collectAsState()
    val shieldDesiredActive by preferences.isShieldDesiredActive.collectAsState()
    val studyModeActive by preferences.studyModeEnabled.collectAsState()

    val isSheildlenzActive = masterPowerEnabled &&
        (telemetry.isProtectionEnabled || shieldDesiredActive || studyModeActive && preferences.isStudySessionActive())
    val latestShieldActive by rememberUpdatedState(isSheildlenzActive)
    val latestDiscreetMode by rememberUpdatedState(discreetMode)
    val latestCustomDomains by rememberUpdatedState(customDomains)
    val latestCustomKeywords by rememberUpdatedState(customKeywords)
    val latestBlockAdult by rememberUpdatedState(blockAdult)
    val latestBlockGambling by rememberUpdatedState(blockGambling)
    val latestSmartSafe by rememberUpdatedState(smartSafeActive)
    val latestPacks by rememberUpdatedState(safeSearchPacks)

    val bookmarks = remember(bookmarksJson) { SafeBrowserFilterHelper.deserializeBookmarks(bookmarksJson) }
    val historyEntries = remember(historyJson) { SafeBrowserFilterHelper.deserializeHistory(historyJson) }
    val downloadItems = remember(downloadsJson) { SafeBrowserFilterHelper.deserializeDownloads(downloadsJson) }

    var tabs by remember {
        mutableStateOf(
            listOf(
                BrowserTabState(
                    title = "Safe Search",
                    url = DEFAULT_SAFE_HOME_URL,
                    desktopMode = desktopDefault
                )
            )
        )
    }
    var activeTabId by remember { mutableStateOf(tabs.first().id) }
    val activeTab = tabs.firstOrNull { it.id == activeTabId } ?: tabs.first()

    var urlInput by remember(activeTab.id) { mutableStateOf(activeTab.url) }
    var blockedState by remember { mutableStateOf<BlockedState?>(null) }
    var quietNoticeBanner by remember { mutableStateOf<String?>(null) }
    var activeSheet by remember { mutableStateOf(BrowserDrawerSheet.NONE) }
    var showFindBar by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    val webViewHolder = remember { arrayOfNulls<WebView>(1) }
    var defaultUserAgent by remember { mutableStateOf<String?>(null) }

    var showUpgradeConfirmDialog by remember { mutableStateOf(false) }
    var isUpgradingEngine by remember { mutableStateOf(false) }
    var upgradeProgress by remember { mutableFloatStateOf(0f) }
    var isFullScreen by remember { mutableStateOf(false) }
    var showBrowserSettingsDialog by remember { mutableStateOf(false) }

    val latestActiveTabId by rememberUpdatedState(activeTab.id)
    val latestTabs by rememberUpdatedState(tabs)

    val recordHistory: (String, String) -> Unit = remember {
        { url, title ->
            if (url.startsWith("http://", true) || url.startsWith("https://", true)) {
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
                        Toast.makeText(
                            context,
                            "Imported ${parsed.size} Chrome bookmarks",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            "No valid Chrome bookmarks found in selected file",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }.onFailure {
                Toast.makeText(context, "Unable to read selected Chrome file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val roleRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        Toast.makeText(context, "Default browser preference updated", Toast.LENGTH_SHORT).show()
    }

    fun applyUserAgent(wv: WebView, desktop: Boolean) {
        val baseUa = defaultUserAgent ?: wv.settings.userAgentString.also { defaultUserAgent = it }
        wv.settings.userAgentString = if (desktop) {
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
        } else {
            baseUa
        }
        wv.settings.useWideViewPort = desktop
        wv.settings.loadWithOverviewMode = desktop
    }

    fun navigateToUrl(rawInput: String, targetTabId: String = activeTab.id) {
        val resolved = SafeBrowserFilterHelper.resolveSafeNavigationUrl(rawInput)
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
                val fallbackUrl = nextTabs.firstOrNull { it.id == nextActiveId }?.url ?: DEFAULT_SAFE_HOME_URL
                urlInput = fallbackUrl
                blockedState = null
                quietNoticeBanner = DiscreetAlertFormatter.tabAutoClosedNotice(latestDiscreetMode)
                webViewHolder[0]?.loadUrl(fallbackUrl)
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
                    if (t.id == targetTabId) t.copy(url = resolved) else t
                }
                webViewHolder[0]?.loadUrl(resolved)
            }
        }
    }

    LaunchedEffect(pendingExternalUrl) {
        val extUrl = pendingExternalUrl?.trim()
        if (!extUrl.isNullOrBlank()) {
            val targetId = if (fullBrowserInstalled && tabs.size < 12) {
                val newTab = BrowserTabState(title = "Opened Link", url = extUrl, desktopMode = desktopDefault)
                tabs = tabs + newTab
                activeTabId = newTab.id
                newTab.id
            } else {
                activeTab.id
            }
            navigateToUrl(extUrl, targetId)
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
        if (showUpgradeConfirmDialog) {
            showUpgradeConfirmDialog = false
            return@BackHandler
        }
        if (activeSheet != BrowserDrawerSheet.NONE) {
            activeSheet = BrowserDrawerSheet.NONE
            return@BackHandler
        }
        val wv = webViewHolder[0]
        if (wv != null && wv.canGoBack()) {
            wv.goBack()
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
                    destroy()
                }
                webViewHolder[0] = null
            }
        }
    }

    SheildlenzBackdrop(modifier = Modifier.testTag("safe_browser_root")) {
        Scaffold(
            contentWindowInsets = WindowInsets.statusBars,
            topBar = {
                if (!isFullScreen) {
                    // Sleek VIP Browser Header with custom orbital prism icon & tier badge
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
                                SafeBrowserBrandIcon(
                                    size = 34.dp,
                                    isProUnlocked = fullBrowserInstalled
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = if (fullBrowserInstalled) "Sheildlenz Browser Pro" else "Safe Browser",
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
                                                .background(
                                                    if (fullBrowserInstalled) AegisPurple.copy(alpha = 0.14f)
                                                    else AegisPrimaryCyan.copy(alpha = 0.14f)
                                                )
                                                .padding(horizontal = 7.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (fullBrowserInstalled) "FULL" else "LIGHT",
                                                color = if (fullBrowserInstalled) AegisPurple else AegisPrimaryCyan,
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

                            // Compact Set Default Browser Pill
                            OutlinedButton(
                                onClick = {
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
                                },
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Light-to-Full Upgrade Card (shown cleanly until Pro Engine is downloaded)
                AnimatedVisibility(
                    visible = !fullBrowserInstalled,
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
                                    text = "1-Tap Full Browser + Auto-Cache Chrome Engine, Cookies, Bookmarks, History & Downloads",
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
                                Icon(
                                    imageVector = Icons.Default.Upgrade,
                                    contentDescription = "Upgrade Browser",
                                    modifier = Modifier.size(16.dp)
                                )
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

                // Quiet Auto-Closed Notice Banner (when Shield OFF closed an offending tab quietly)
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
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = AegisChampagneGold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = quietNoticeBanner.orEmpty(),
                                    color = AegisTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = ClassicalBodyFont
                                )
                            }
                            IconButton(
                                onClick = { quietNoticeBanner = null },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = AegisTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Multi-Tab Strip (Full Pro unlocked)
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
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) AegisPrimaryCyan.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.86f),
                                    border = BorderStroke(
                                        width = if (isSelected) 1.4.dp else 1.dp,
                                        color = if (isSelected) AegisPrimaryCyan else AegisCardBorder
                                    ),
                                    modifier = Modifier
                                        .clickable {
                                            activeTabId = tab.id
                                            urlInput = tab.url
                                            blockedState = null
                                            webViewHolder[0]?.let { wv ->
                                                applyUserAgent(wv, tab.desktopMode)
                                                wv.loadUrl(tab.url)
                                            }
                                        }
                                        .testTag("safe_browser_tab_${tab.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = tab.title.take(18),
                                            color = if (isSelected) AegisPrimaryCyan else AegisTextPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        IconButton(
                                            onClick = {
                                                val (nextTabs, nextId) = SafeBrowserFilterHelper.closeTabAndEnsureFallback(tabs, tab.id)
                                                tabs = nextTabs
                                                activeTabId = nextId
                                                val targetUrl = nextTabs.first { it.id == nextId }.url
                                                urlInput = targetUrl
                                                blockedState = null
                                                webViewHolder[0]?.loadUrl(targetUrl)
                                            },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Close tab",
                                                tint = AegisTextSecondary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AegisPurple,
                            modifier = Modifier
                                .size(34.dp)
                                .clickable {
                                    val created = BrowserTabState(
                                        title = "New Tab ${tabs.size + 1}",
                                        url = DEFAULT_SAFE_HOME_URL,
                                        desktopMode = desktopDefault
                                    )
                                    tabs = tabs + created
                                    activeTabId = created.id
                                    urlInput = created.url
                                    blockedState = null
                                    webViewHolder[0]?.let { wv ->
                                        applyUserAgent(wv, created.desktopMode)
                                        wv.loadUrl(created.url)
                                    }
                                }
                                .testTag("safe_browser_new_tab_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "New Tab",
                                    tint = AegisOnPrimaryWhite,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // 1. VIP Omnibox Toolbar with Animated Corners
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(5.dp, RoundedCornerShape(18.dp))
                        .vipAnimatedCorners(
                            cornerRadius = 18.dp,
                            accentStart = AegisElectricBlue,
                            accentEnd = AegisPurple,
                            strokeWidth = 1.8.dp
                        )
                        .testTag("safe_browser_toolbar"),
                    shape = RoundedCornerShape(18.dp),
                    color = AegisLavenderSoft,
                    border = BorderStroke(1.dp, AegisLavenderBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = { webViewHolder[0]?.goBack() },
                                enabled = activeTab.canGoBack,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = if (activeTab.canGoBack) AegisPurple else AegisTextSecondary.copy(alpha = 0.4f)
                                )
                            }
                            IconButton(
                                onClick = { webViewHolder[0]?.goForward() },
                                enabled = activeTab.canGoForward,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Forward",
                                    tint = if (activeTab.canGoForward) AegisPurple else AegisTextSecondary.copy(alpha = 0.4f)
                                )
                            }
                            IconButton(
                                onClick = { webViewHolder[0]?.reload() },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reload",
                                    tint = AegisPrimaryCyan
                                )
                            }

                            OutlinedTextField(
                                value = urlInput,
                                onValueChange = { urlInput = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 44.dp)
                                    .testTag("safe_browser_url_input"),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Secure HTTPS",
                                        tint = AegisEmeraldTeal,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            val query = urlInput.trim()
                                            if (query.isNotEmpty()) {
                                                navigateToUrl(query)
                                            }
                                        },
                                        modifier = Modifier.testTag("safe_browser_go_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "Navigate",
                                            tint = AegisPrimaryCyan,
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
                                    focusedBorderColor = AegisPrimaryCyan,
                                    unfocusedBorderColor = AegisCardBorder,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.85f)
                                ),
                                textStyle = TextStyle(
                                    fontSize = 12.5.sp,
                                    fontFamily = ClassicalBodyFont,
                                    color = AegisTextPrimary
                                )
                            )
                        }

                        // Quick Dial Bookmarks Row
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            items(bookmarks.take(12)) { bm ->
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable {
                                            AegisSoundEffects.playTap(context)
                                            navigateToUrl(bm.url)
                                        },
                                    shape = RoundedCornerShape(50),
                                    color = Color.White.copy(alpha = 0.9f),
                                    border = BorderStroke(1.dp, AegisLavenderBorder)
                                ) {
                                    Text(
                                        text = bm.title,
                                        color = AegisPurple,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = ClassicalBodyFont,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        // Pro Chrome Action Bar (Full Pro Engine features + Bookmark & Import access)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            val isCurrentBookmarked = bookmarks.any { it.url == activeTab.url }
                            item {
                                BrowserToolChip(
                                    label = if (isCurrentBookmarked) "Saved" else "Bookmark",
                                    icon = if (isCurrentBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    active = isCurrentBookmarked,
                                    tag = "safe_browser_bookmark_toggle",
                                    onClick = {
                                        val next = if (isCurrentBookmarked) {
                                            bookmarks.filterNot { it.url == activeTab.url }
                                        } else {
                                            listOf(SafeBookmark(title = activeTab.title.take(36), url = activeTab.url)) + bookmarks
                                        }
                                        preferences.setBrowserBookmarksJson(SafeBrowserFilterHelper.serializeBookmarks(next))
                                        Toast.makeText(
                                            context,
                                            if (isCurrentBookmarked) "Removed bookmark" else "Saved bookmark",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                            }
                            if (fullBrowserInstalled) {
                                item {
                                    BrowserToolChip(
                                        label = if (chromeComponentsCached) {
                                            "Chrome Cached (${chromeCachedCount.coerceAtLeast(ChromeComponentCacheManager.CORE_CHROME_MODULES.size)})"
                                        } else {
                                            "Auto-Cache Chrome"
                                        },
                                        icon = Icons.Default.Shield,
                                        active = chromeComponentsCached,
                                        tag = "safe_browser_chrome_cache_badge",
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
                                }
                                item {
                                    BrowserToolChip(
                                        label = "Import Chrome",
                                        icon = Icons.Default.FileOpen,
                                        active = chromeImportedCount > 0,
                                        tag = "safe_browser_import_chrome_button",
                                        onClick = {
                                            preferences.beginAuthorizedExternalFlow()
                                            chromeFileImportLauncher.launch(arrayOf("text/html", "application/json", "*/*"))
                                        }
                                    )
                                }
                                item {
                                    BrowserToolChip(
                                        label = "Bookmarks (${bookmarks.size})",
                                        icon = Icons.Default.Bookmark,
                                        active = activeSheet == BrowserDrawerSheet.BOOKMARKS,
                                        tag = "safe_browser_bookmarks_button",
                                        onClick = { activeSheet = BrowserDrawerSheet.BOOKMARKS }
                                    )
                                }
                                item {
                                    BrowserToolChip(
                                        label = "History",
                                        icon = Icons.Default.History,
                                        active = activeSheet == BrowserDrawerSheet.HISTORY,
                                        tag = "safe_browser_history_button",
                                        onClick = { activeSheet = BrowserDrawerSheet.HISTORY }
                                    )
                                }
                                item {
                                    BrowserToolChip(
                                        label = "Downloads",
                                        icon = Icons.Default.Download,
                                        active = activeSheet == BrowserDrawerSheet.DOWNLOADS,
                                        tag = "safe_browser_downloads_button",
                                        onClick = { activeSheet = BrowserDrawerSheet.DOWNLOADS }
                                    )
                                }
                                item {
                                    BrowserToolChip(
                                        label = if (activeTab.desktopMode) "Desktop ON" else "Desktop",
                                        icon = Icons.Default.DesktopWindows,
                                        active = activeTab.desktopMode,
                                        tag = "safe_browser_desktop_toggle",
                                        onClick = {
                                            val nextDesktop = !activeTab.desktopMode
                                            preferences.setBrowserDesktopModeDefault(nextDesktop)
                                            tabs = tabs.map {
                                                if (it.id == activeTab.id) it.copy(desktopMode = nextDesktop) else it
                                            }
                                            webViewHolder[0]?.let { wv ->
                                                applyUserAgent(wv, nextDesktop)
                                                wv.reload()
                                            }
                                        }
                                    )
                                }
                                item {
                                    BrowserToolChip(
                                        label = "Find",
                                        icon = Icons.Default.Search,
                                        active = showFindBar,
                                        tag = "safe_browser_find_toggle",
                                        onClick = {
                                             showFindBar = !showFindBar
                                             if (!showFindBar) {
                                                 findQuery = ""
                                                 webViewHolder[0]?.clearMatches()
                                             }
                                        }
                                     )
                                 }
                                 item {
                                     BrowserToolChip(
                                         label = if (isFullScreen) "Exit Full" else "Fullscreen",
                                         icon = Icons.Default.Fullscreen,
                                         active = isFullScreen,
                                         tag = "safe_browser_fullscreen_toggle",
                                         onClick = { isFullScreen = !isFullScreen }
                                     )
                                 }
                                 item {
                                     BrowserToolChip(
                                         label = "Add to Home",
                                         icon = Icons.Default.Add,
                                         active = false,
                                         tag = "safe_browser_add_to_home",
                                         onClick = {
                                             val bm = SafeBookmark(title = activeTab.title.take(36), url = activeTab.url, folder = "Home Screen Shortcuts")
                                             val existing = SafeBrowserFilterHelper.deserializeBookmarks(preferences.browserBookmarksJson.value)
                                             preferences.setBrowserBookmarksJson(SafeBrowserFilterHelper.serializeBookmarks(listOf(bm) + existing))
                                             Toast.makeText(context, "Added '${activeTab.title}' shortcut", Toast.LENGTH_SHORT).show()
                                         }
                                     )
                                 }
                                 item {
                                     BrowserToolChip(
                                         label = "Settings",
                                         icon = Icons.Default.Settings,
                                         active = showBrowserSettingsDialog,
                                         tag = "safe_browser_settings_button",
                                         onClick = { showBrowserSettingsDialog = true }
                                     )
                                 }
                            }
                        }

                        if (showFindBar && fullBrowserInstalled) {
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

                if (activeTab.progress in 1..99) {
                    LinearProgressIndicator(
                        progress = { activeTab.progress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = AegisPrimaryCyan,
                        trackColor = AegisPrimaryCyan.copy(alpha = 0.2f)
                    )
                }

                // 2. WebView Sandbox or Calm Blocked Card (with VIP Animated Corners)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White)
                        .border(1.dp, AegisCardBorder, RoundedCornerShape(18.dp))
                        .vipAnimatedCorners(
                            cornerRadius = 18.dp,
                            accentStart = AegisPrimaryCyan,
                            accentEnd = AegisEmeraldTeal,
                            strokeWidth = 1.8.dp
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

                            Spacer(modifier = Modifier.height(6.dp))

                            if (!discreetMode) {
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
                                    blockedState = null
                                    urlInput = DEFAULT_SAFE_HOME_URL
                                    tabs = tabs.map {
                                        if (it.id == activeTab.id) it.copy(url = DEFAULT_SAFE_HOME_URL) else it
                                    }
                                    webViewHolder[0]?.loadUrl(DEFAULT_SAFE_HOME_URL)
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
                    } else {
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.databaseEnabled = true
                                    settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                                    settings.loadsImagesAutomatically = true
                                    settings.safeBrowsingEnabled = true
                                    settings.setSupportZoom(true)
                                    settings.builtInZoomControls = true
                                    settings.displayZoomControls = false
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
                                                        val safeFallbackUrl = nextTabs.firstOrNull { it.id == nextId }?.url
                                                            ?: DEFAULT_SAFE_HOME_URL
                                                        urlInput = safeFallbackUrl
                                                        blockedState = null
                                                        quietNoticeBanner = DiscreetAlertFormatter.tabAutoClosedNotice(latestDiscreetMode)
                                                        loadUrl(safeFallbackUrl)
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
                                                if (it.id == latestActiveTabId) it.copy(url = targetUrl) else it
                                            }
                                            return false
                                        }

                                        override fun shouldInterceptRequest(
                                            view: WebView?,
                                            request: WebResourceRequest?
                                        ): WebResourceResponse? {
                                            val url = request?.url?.toString() ?: return null
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
                                                            isLoading = false
                                                        )
                                                    } else it
                                                }
                                                recordHistory(url, title)
                                            }
                                        }
                                    }

                                    loadUrl(activeTab.url)
                                    webViewHolder[0] = this
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("safe_browser_webview")
                        )
                    }
                }
            }
        }
    }

    if (activeSheet != BrowserDrawerSheet.NONE) {
        AlertDialog(
            onDismissRequest = { activeSheet = BrowserDrawerSheet.NONE },
            containerColor = AegisLavenderSoft,
            title = {
                Text(
                    text = when (activeSheet) {
                        BrowserDrawerSheet.BOOKMARKS -> "Bookmarks (${bookmarks.size})"
                        BrowserDrawerSheet.HISTORY -> "Browsing History"
                        BrowserDrawerSheet.DOWNLOADS -> "Downloads"
                        BrowserDrawerSheet.NONE -> ""
                    },
                    fontFamily = ClassicalDisplayFont,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                ) {
                    when (activeSheet) {
                        BrowserDrawerSheet.BOOKMARKS -> {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(bookmarks) { bm ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color.White)
                                            .clickable {
                                                activeSheet = BrowserDrawerSheet.NONE
                                                navigateToUrl(bm.url)
                                            }
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = bm.title,
                                                color = AegisTextPrimary,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${bm.folder} • ${bm.url}",
                                                color = AegisTextSecondary,
                                                fontSize = 10.5.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        BrowserDrawerSheet.HISTORY -> {
                            if (historyEntries.isEmpty()) {
                                Text("No pages visited yet.", color = AegisTextSecondary, fontSize = 12.sp)
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    OutlinedButton(onClick = { preferences.setBrowserHistoryJson("") }) {
                                        Text("Clear History", fontSize = 11.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(historyEntries) { entry ->
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color.White)
                                                .clickable {
                                                    activeSheet = BrowserDrawerSheet.NONE
                                                    navigateToUrl(entry.url)
                                                }
                                                .padding(10.dp)
                                        ) {
                                            Text(
                                                text = entry.title,
                                                color = AegisTextPrimary,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = entry.url,
                                                color = AegisTextSecondary,
                                                fontSize = 10.5.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        BrowserDrawerSheet.DOWNLOADS -> {
                            if (downloadItems.isEmpty()) {
                                Text(
                                    text = "Downloads are enabled normally. Files you download in Safe Browser will appear here.",
                                    color = AegisTextSecondary,
                                    fontSize = 12.sp
                                )
                            } else {
                                val fmt = remember { SimpleDateFormat("MMM d, HH:mm", Locale.US) }
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(downloadItems, key = { it.id }) { item ->
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color.White)
                                                .padding(10.dp)
                                        ) {
                                            Text(
                                                text = item.fileName,
                                                color = AegisTextPrimary,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${item.statusLabel} • ${fmt.format(Date(item.createdAtMillis))}",
                                                color = AegisEmeraldTeal,
                                                fontSize = 10.5.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        BrowserDrawerSheet.NONE -> Unit
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { activeSheet = BrowserDrawerSheet.NONE },
                    colors = ButtonDefaults.buttonColors(containerColor = AegisPurple, contentColor = AegisOnPrimaryWhite)
                ) {
                    Text("Close")
                }
            }
        )
    }

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
                        text = "Any accessible Chrome bookmarks export is also auto-cached, and you can import extra Chrome files anytime.",
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

    if (showBrowserSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showBrowserSettingsDialog = false },
            containerColor = AegisLavenderSoft,
            title = {
                Text(
                    text = "Safe Browser Settings",
                    fontFamily = ClassicalDisplayFont,
                    fontWeight = FontWeight.Bold,
                    color = AegisTextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Customize browsing security, search preferences, and default browser behavior.",
                        color = AegisTextSecondary,
                        fontSize = 12.sp,
                        fontFamily = ClassicalBodyFont
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Smart Safe Search", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = AegisTextPrimary)
                        androidx.compose.material3.Switch(
                            checked = smartSafeActive,
                            onCheckedChange = { preferences.setSmartSafeSearchEnabled(it) }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Block Adult Content", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = AegisTextPrimary)
                        androidx.compose.material3.Switch(
                            checked = blockAdult,
                            onCheckedChange = { preferences.setBlockExplicitAdult(it) }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Default Desktop Mode", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = AegisTextPrimary)
                        androidx.compose.material3.Switch(
                            checked = desktopDefault,
                            onCheckedChange = { preferences.setBrowserDesktopModeDefault(it) }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showBrowserSettingsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = AegisPurple, contentColor = AegisOnPrimaryWhite),
                    modifier = Modifier.testTag("browser_settings_close_button")
                ) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
private fun BrowserToolChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (active) AegisPurple else AegisSurfaceElevated,
        border = BorderStroke(1.dp, if (active) AegisPurple else AegisCardBorder),
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) AegisOnPrimaryWhite else AegisPurple,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                color = if (active) AegisOnPrimaryWhite else AegisTextPrimary,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}
