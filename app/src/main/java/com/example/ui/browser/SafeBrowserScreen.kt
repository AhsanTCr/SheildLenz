package com.example.ui.browser

import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.core.audio.AegisSoundEffects
import com.example.data.preferences.AegisPreferences
import com.example.data.repository.BlockedDnsLogRepository
import com.example.monitoring.accessibility.AegisAccessibilityService
import com.example.services.dns.DnsPacketHelper
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberCardTone
import com.example.ui.components.CyberTopBar
import com.example.ui.components.SheildlenzBackdrop
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisElectricBlue
import com.example.ui.theme.AegisEmeraldTeal
import com.example.ui.theme.AegisLavenderBorder
import com.example.ui.theme.AegisLavenderSoft
import com.example.ui.theme.AegisOnPrimaryWhite
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisPurple
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.AegisWarningAmber
import com.example.ui.theme.ClassicalBodyFont
import com.example.ui.theme.ClassicalDisplayFont

data class SafeBookmark(
    val title: String,
    val url: String
)

@Composable
fun SafeBrowserScreen(
    preferences: AegisPreferences,
    blockedDnsLogRepo: BlockedDnsLogRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val customDomains by preferences.customBlockedDomains.collectAsState()
    val customKeywords by preferences.customBlockedKeywords.collectAsState()
    val blockAdult by preferences.blockExplicitAdult.collectAsState()
    val blockGambling by preferences.blockGamblingKeywords.collectAsState()
    val smartSafeActive by preferences.smartSafeSearchEnabled.collectAsState()
    val safeSearchPacks by preferences.safeSearchPacks.collectAsState()
    val discreetMode by preferences.discreetBlockingScreenMode.collectAsState()

    var urlInput by remember { mutableStateOf("https://www.google.com/?safe=active") }
    var currentLoadedUrl by remember { mutableStateOf("https://www.google.com/?safe=active") }
    var pageTitle by remember { mutableStateOf("Safe Browser") }
    var pageProgress by remember { mutableStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    var blockedState by remember { mutableStateOf<BlockedState?>(null) }

    val bookmarks = remember {
        listOf(
            SafeBookmark("Google Search", "https://www.google.com/?safe=active"),
            SafeBookmark("Wikipedia", "https://en.wikipedia.org"),
            SafeBookmark("Khan Academy", "https://www.khanacademy.org"),
            SafeBookmark("BBC Learning", "https://www.bbc.co.uk/learning")
        )
    }

    BackHandler {
        val wv = webViewRef
        if (wv != null && wv.canGoBack()) {
            wv.goBack()
        } else {
            onNavigateBack()
        }
    }

    SheildlenzBackdrop(modifier = Modifier.testTag("safe_browser_root")) {
        Scaffold(
            contentWindowInsets = WindowInsets.statusBars,
            topBar = {
                CyberTopBar(
                    title = "Safe Browser",
                    subtitle = "Local SafeSearch & Blocklist Sandbox",
                    onNavigateBack = onNavigateBack
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Omnibox Toolbar
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(16.dp))
                        .testTag("safe_browser_toolbar"),
                    shape = RoundedCornerShape(16.dp),
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
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IconButton(
                                onClick = { webViewRef?.goBack() },
                                enabled = canGoBack,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = if (canGoBack) AegisPurple else AegisTextSecondary.copy(alpha = 0.4f)
                                )
                            }
                            IconButton(
                                onClick = { webViewRef?.goForward() },
                                enabled = canGoForward,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Forward",
                                    tint = if (canGoForward) AegisPurple else AegisTextSecondary.copy(alpha = 0.4f)
                                )
                            }
                            IconButton(
                                onClick = { webViewRef?.reload() },
                                modifier = Modifier.size(36.dp)
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
                                                val resolved = resolveSafeNavigationUrl(query)
                                                urlInput = resolved
                                                blockedState = null
                                                webViewRef?.loadUrl(resolved)
                                            }
                                        }
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
                                            val resolved = resolveSafeNavigationUrl(query)
                                            urlInput = resolved
                                            blockedState = null
                                            webViewRef?.loadUrl(resolved)
                                        }
                                    }
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AegisPrimaryCyan,
                                    unfocusedBorderColor = AegisCardBorder,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.8f)
                                ),
                                textStyle = TextStyle(
                                    fontSize = 12.5.sp,
                                    fontFamily = ClassicalBodyFont,
                                    color = AegisTextPrimary
                                )
                            )
                        }

                        // Bookmarks Row
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            items(bookmarks) { bm ->
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable {
                                            AegisSoundEffects.playTap(context)
                                            val resolved = resolveSafeNavigationUrl(bm.url)
                                            urlInput = resolved
                                            blockedState = null
                                            webViewRef?.loadUrl(resolved)
                                        },
                                    shape = RoundedCornerShape(50),
                                    color = Color.White.copy(alpha = 0.85f),
                                    border = BorderStroke(1.dp, AegisLavenderBorder)
                                ) {
                                    Text(
                                        text = bm.title,
                                        color = AegisPurple,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = ClassicalBodyFont,
                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (pageProgress in 1..99) {
                    LinearProgressIndicator(
                        progress = { pageProgress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = AegisPrimaryCyan,
                        trackColor = AegisPrimaryCyan.copy(alpha = 0.2f)
                    )
                }

                // 2. WebView Sandbox or Calm Blocked Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.dp, AegisCardBorder, RoundedCornerShape(16.dp))
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
                                text = if (discreetMode) "Quiet Pause Applied" else "Safe Browser Blocked Content",
                                color = AegisTextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = ClassicalDisplayFont,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = currentBlock.reason,
                                color = AegisTextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                fontFamily = ClassicalBodyFont,
                                textAlign = TextAlign.Center
                            )

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

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    blockedState = null
                                    urlInput = "https://www.google.com/?safe=active"
                                    webViewRef?.loadUrl("https://www.google.com/?safe=active")
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AegisPrimaryCyan,
                                    contentColor = AegisOnPrimaryWhite
                                ),
                                shape = RoundedCornerShape(14.dp)
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
                                    settings.loadsImagesAutomatically = true
                                    settings.safeBrowsingEnabled = true
                                    settings.setSupportMultipleWindows(false)

                                    webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(
                                            view: WebView?,
                                            request: WebResourceRequest?
                                        ): Boolean {
                                            val targetUrl = request?.url?.toString() ?: return false
                                            val inspectResult = evaluateSafeBrowserUrl(
                                                rawUrl = targetUrl,
                                                customDomains = customDomains,
                                                customKeywords = customKeywords,
                                                blockAdult = blockAdult,
                                                blockGambling = blockGambling,
                                                smartSafeActive = smartSafeActive,
                                                safeSearchPacks = safeSearchPacks
                                            )
                                            if (inspectResult != null) {
                                                blockedState = BlockedState(
                                                    url = targetUrl,
                                                    reason = inspectResult
                                                )
                                                runCatching {
                                                    val category = when {
                                                        inspectResult.contains("Gambling", ignoreCase = true) ->
                                                            BlockedDnsLogRepository.CATEGORY_GAMBLING
                                                        inspectResult.contains("Keyword", ignoreCase = true) ->
                                                            BlockedDnsLogRepository.CATEGORY_CUSTOM_KEYWORD
                                                        else -> BlockedDnsLogRepository.CATEGORY_ADULT
                                                    }
                                                    blockedDnsLogRepo.recordBlockedQuery(
                                                        domain = Uri.parse(targetUrl).host ?: targetUrl,
                                                        reasonCategory = category,
                                                        matchedRule = inspectResult
                                                    )
                                                }
                                                return true
                                            }
                                            urlInput = targetUrl
                                            currentLoadedUrl = targetUrl
                                            return false
                                        }

                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            super.onPageFinished(view, url)
                                            if (url != null) {
                                                urlInput = url
                                                currentLoadedUrl = url
                                                pageTitle = view?.title ?: "Safe Browser"
                                                canGoBack = view?.canGoBack() == true
                                                canGoForward = view?.canGoForward() == true
                                            }
                                        }
                                    }

                                    loadUrl(urlInput)
                                    webViewRef = this
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
}

data class BlockedState(
    val url: String,
    val reason: String
)

private fun resolveSafeNavigationUrl(input: String): String {
    val trimmed = input.trim()
    if (trimmed.startsWith("http://", true) || trimmed.startsWith("https://", true)) {
        return enforceSafeSearchQuery(trimmed)
    }
    if (trimmed.contains('.') && !trimmed.contains(' ')) {
        return enforceSafeSearchQuery("https://$trimmed")
    }
    val encoded = Uri.encode(trimmed)
    return "https://www.google.com/search?q=$encoded&safe=active"
}

private fun enforceSafeSearchQuery(rawUrl: String): String {
    try {
        val uri = Uri.parse(rawUrl)
        val host = uri.host?.lowercase() ?: return rawUrl
        if (host.contains("google.") && !rawUrl.contains("safe=")) {
            val builder = uri.buildUpon().appendQueryParameter("safe", "active")
            return builder.build().toString()
        }
        if (host.contains("bing.") && !rawUrl.contains("adlt=")) {
            val builder = uri.buildUpon().appendQueryParameter("adlt", "strict")
            return builder.build().toString()
        }
        if (host.contains("youtube.") && !rawUrl.contains("safe=")) {
            val builder = uri.buildUpon().appendQueryParameter("safe", "active")
            return builder.build().toString()
        }
    } catch (_: Exception) {
    }
    return rawUrl
}

private fun evaluateSafeBrowserUrl(
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
    val fullText = "$host ${uri.path} ${uri.query}".lowercase()

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

    val urlScanHit = AegisAccessibilityService.detectExplicitBrowserUrl(
        rawText = fullText,
        customDomains = customDomains,
        customKeywords = customKeywords,
        blockGamblingKeywords = blockGambling,
        smartSafeSearchEnabled = smartSafeActive,
        safeSearchPacks = safeSearchPacks
    )
    return urlScanHit
}
