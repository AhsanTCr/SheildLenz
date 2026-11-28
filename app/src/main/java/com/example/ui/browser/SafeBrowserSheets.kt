package com.example.ui.browser

import android.webkit.CookieManager
import android.webkit.WebView
import android.widget.Toast
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddHome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.audio.AegisSoundEffects
import com.example.data.preferences.AegisPreferences
import com.example.data.preferences.BrowserSearchEngine
import com.example.ui.components.SafeBrowserBrandIcon
import com.example.ui.components.vipAnimatedCorners
import com.example.ui.theme.AegisCardBackground
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
import com.example.ui.theme.ClassicalBodyFont
import com.example.ui.theme.ClassicalDisplayFont
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal enum class BrowserDrawerSheet {
    NONE,
    BOOKMARKS,
    HISTORY,
    DOWNLOADS
}

internal val IncognitoIndigo = Color(0xFF312E81)
internal val IncognitoAccent = Color(0xFF4F46E5)

@Composable
internal fun SpeedDialNewTabCanvas(
    isIncognito: Boolean,
    isProUnlocked: Boolean,
    searchEngine: BrowserSearchEngine,
    bookmarks: List<SafeBookmark>,
    isShieldActive: Boolean,
    onSearchOrUrlSubmit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var dialQuery by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isIncognito) {
                        listOf(Color(0xFFEEF2FF), Color(0xFFE0E7FF), Color(0xFFF5F3FF))
                    } else {
                        listOf(Color(0xFFF5FAFF), Color(0xFFEDE8FF), Color(0xFFFFFFFF))
                    }
                )
            )
            .padding(horizontal = 16.dp, vertical = 18.dp)
            .testTag("safe_browser_speed_dial"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(modifier = Modifier.height(6.dp))

        SafeBrowserBrandIcon(
            size = 74.dp,
            isProUnlocked = isProUnlocked || isIncognito
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (isIncognito) "Incognito Private Tab" else "Sheildlenz Browser",
                color = if (isIncognito) IncognitoIndigo else AegisTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ClassicalDisplayFont
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = if (isIncognito) {
                    if (isShieldActive) {
                        "No local history saved • Sheildlenz safety rules active"
                    } else {
                        "No local history saved • Quiet auto-close guard active"
                    }
                } else {
                    "Search privately with ${searchEngine.displayName} • Speed Dial"
                },
                color = if (isIncognito) IncognitoAccent else AegisTextSecondary,
                fontSize = 11.5.sp,
                fontFamily = ClassicalBodyFont,
                textAlign = TextAlign.Center
            )
        }

        // Centered Speed Dial Omnibox Pill
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            border = BorderStroke(1.2.dp, if (isIncognito) IncognitoAccent else AegisLavenderBorder),
            modifier = Modifier
                .fillMaxWidth()
                .vipAnimatedCorners(
                    cornerRadius = 24.dp,
                    accentStart = if (isIncognito) IncognitoAccent else AegisPrimaryCyan,
                    accentEnd = AegisPurple,
                    strokeWidth = 1.6.dp
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isIncognito) Icons.Default.VisibilityOff else Icons.Default.Search,
                    contentDescription = null,
                    tint = if (isIncognito) IncognitoAccent else AegisPrimaryCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                OutlinedTextField(
                    value = dialQuery,
                    onValueChange = { dialQuery = it },
                    placeholder = {
                        Text(
                            text = "Search ${searchEngine.displayName} or type URL...",
                            fontSize = 12.5.sp,
                            color = AegisTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            if (dialQuery.isNotBlank()) {
                                onSearchOrUrlSubmit(dialQuery)
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("speed_dial_search_input")
                )
                Button(
                    onClick = {
                        AegisSoundEffects.playTap(context)
                        val q = dialQuery.trim()
                        if (q.isNotBlank()) {
                            onSearchOrUrlSubmit(q)
                        } else {
                            onSearchOrUrlSubmit(searchEngine.homeUrl)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isIncognito) IncognitoAccent else AegisPrimaryCyan,
                        contentColor = AegisOnPrimaryWhite
                    ),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier
                        .height(40.dp)
                        .testTag("speed_dial_go_button")
                ) {
                    Text("Go", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 4-Column Chrome-style Speed Dial Grid
        val tiles = bookmarks.take(8)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "SPEED DIAL SHORTCUTS",
                color = if (isIncognito) IncognitoAccent else AegisPurple,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            tiles.chunked(4).forEach { rowTiles ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowTiles.forEach { bm ->
                        val initial = bm.title.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "W"
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.94f),
                            border = BorderStroke(1.dp, AegisCardBorder),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 82.dp)
                                .clickable {
                                    AegisSoundEffects.playTap(context)
                                    onSearchOrUrlSubmit(bm.url)
                                }
                                .testTag("speed_dial_tile_${bm.title}")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(AegisPrimaryCyan, AegisPurple)
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = initial,
                                        color = AegisOnPrimaryWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = bm.title,
                                    color = AegisTextPrimary,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    repeat(4 - rowTiles.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
internal fun TabGridSwitcherDialog(
    tabs: List<BrowserTabState>,
    activeTabId: String,
    onSelectTab: (BrowserTabState) -> Unit,
    onCloseTab: (BrowserTabState) -> Unit,
    onNewRegularTab: () -> Unit,
    onNewIncognitoTab: () -> Unit,
    onCloseAllIncognito: () -> Unit,
    onDismiss: () -> Unit
) {
    val incognitoCount = tabs.count { it.isIncognito }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AegisLavenderSoft,
        modifier = Modifier
            .vipAnimatedCorners(
                cornerRadius = 22.dp,
                accentStart = AegisPrimaryCyan,
                accentEnd = AegisPurple,
                strokeWidth = 2.dp
            )
            .testTag("safe_browser_tab_grid_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Tabs (${tabs.size})",
                    fontFamily = ClassicalDisplayFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = AegisTextPrimary
                )
                if (incognitoCount > 0) {
                    OutlinedButton(
                        onClick = onCloseAllIncognito,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Close Incognito ($incognitoCount)", fontSize = 10.sp)
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onNewRegularTab,
                        colors = ButtonDefaults.buttonColors(containerColor = AegisPrimaryCyan, contentColor = AegisOnPrimaryWhite),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("tab_grid_new_tab_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Tab", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onNewIncognitoTab,
                        colors = ButtonDefaults.buttonColors(containerColor = IncognitoIndigo, contentColor = AegisOnPrimaryWhite),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("tab_grid_new_incognito_button")
                    ) {
                        Icon(Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Incognito", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 310.dp)
                ) {
                    items(tabs, key = { it.id }) { tab ->
                        val selected = tab.id == activeTabId
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = when {
                                tab.isIncognito -> Color(0xFFE0E7FF)
                                selected -> Color.White
                                else -> Color.White.copy(alpha = 0.85f)
                            },
                            border = BorderStroke(
                                width = if (selected) 2.dp else 1.dp,
                                color = when {
                                    tab.isIncognito && selected -> IncognitoAccent
                                    tab.isIncognito -> IncognitoAccent.copy(alpha = 0.45f)
                                    selected -> AegisPrimaryCyan
                                    else -> AegisCardBorder
                                }
                            ),
                            modifier = Modifier
                                .clickable { onSelectTab(tab) }
                                .testTag("tab_grid_item_${tab.id}")
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = if (tab.isIncognito) Icons.Default.VisibilityOff else Icons.Default.Public,
                                            contentDescription = null,
                                            tint = if (tab.isIncognito) IncognitoAccent else AegisPrimaryCyan,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = if (tab.isIncognito) "Incognito" else "Safe Tab",
                                            color = if (tab.isIncognito) IncognitoIndigo else AegisPurple,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    IconButton(
                                        onClick = { onCloseTab(tab) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close ${tab.title}",
                                            tint = AegisTextSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = tab.title.ifBlank { "New Safe Tab" },
                                    color = AegisTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (tab.isNewTabPage) {
                                        "Speed Dial"
                                    } else {
                                        android.net.Uri.parse(tab.url).host ?: tab.url
                                    },
                                    color = AegisTextSecondary,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = AegisPurple, contentColor = AegisOnPrimaryWhite),
                modifier = Modifier.testTag("tab_grid_close_button")
            ) {
                Text("Done")
            }
        }
    )
}

@Composable
internal fun SitePermissionsDialog(
    currentUrl: String,
    sitePermissionsJson: String,
    isShieldActive: Boolean,
    onUpdatePolicy: (SitePermissionType, SitePermissionState) -> Unit,
    onPinSiteToHome: () -> Unit,
    onDismiss: () -> Unit
) {
    val origin = remember(currentUrl) {
        BrowserPermissionsStore.extractOrigin(currentUrl).ifBlank { "https://sheildlenz.local" }
    }
    val entry = remember(sitePermissionsJson, currentUrl) {
        BrowserPermissionsStore.getEntry(sitePermissionsJson, currentUrl)
    }
    val isHttps = currentUrl.startsWith("https://", ignoreCase = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AegisLavenderSoft,
        modifier = Modifier
            .vipAnimatedCorners(
                cornerRadius = 20.dp,
                accentStart = AegisPrimaryCyan,
                accentEnd = AegisEmeraldTeal,
                strokeWidth = 1.8.dp
            )
            .testTag("site_permissions_dialog"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (isHttps) AegisEmeraldTeal else AegisChampagneGold
                )
                Column {
                    Text(
                        text = if (isHttps) "Connection Secure (HTTPS)" else "Site Information",
                        fontFamily = ClassicalDisplayFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.5.sp,
                        color = AegisTextPrimary
                    )
                    Text(
                        text = origin,
                        fontSize = 11.sp,
                        color = AegisPurple,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isShieldActive) {
                        "Sheildlenz Shield is actively guarding this tab."
                    } else {
                        "Sheildlenz Quiet Auto-Close Guard is active on this tab."
                    },
                    color = AegisEmeraldTeal,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                HorizontalDivider(color = AegisCardBorder)
                Text(
                    text = "Per-Site Device Permissions",
                    color = AegisTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    fontFamily = ClassicalDisplayFont
                )
                SitePermissionSelectorRow(
                    label = "Camera",
                    currentState = entry.camera,
                    tagPrefix = "site_perm_camera",
                    onSelect = { onUpdatePolicy(SitePermissionType.CAMERA, it) }
                )
                SitePermissionSelectorRow(
                    label = "Microphone",
                    currentState = entry.microphone,
                    tagPrefix = "site_perm_mic",
                    onSelect = { onUpdatePolicy(SitePermissionType.MICROPHONE, it) }
                )
                SitePermissionSelectorRow(
                    label = "Location",
                    currentState = entry.location,
                    tagPrefix = "site_perm_location",
                    onSelect = { onUpdatePolicy(SitePermissionType.LOCATION, it) }
                )
                if (currentUrl.startsWith("http://", true) || currentUrl.startsWith("https://", true)) {
                    OutlinedButton(
                        onClick = onPinSiteToHome,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.AddHome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Site to Home Screen", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = AegisPurple, contentColor = AegisOnPrimaryWhite),
                modifier = Modifier.testTag("site_permissions_close_button")
            ) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun SitePermissionSelectorRow(
    label: String,
    currentState: SitePermissionState,
    tagPrefix: String,
    onSelect: (SitePermissionState) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, color = AegisTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SitePermissionState.entries.forEach { st ->
                val selected = st == currentState
                val activeBg = when (st) {
                    SitePermissionState.ALLOW -> AegisEmeraldTeal
                    SitePermissionState.BLOCK -> AegisChampagneGold
                    SitePermissionState.ASK -> AegisPurple
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (selected) activeBg else Color.White,
                    border = BorderStroke(1.dp, if (selected) activeBg else AegisCardBorder),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 38.dp)
                        .clickable { onSelect(st) }
                        .testTag("${tagPrefix}_${st.id}")
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 7.dp)) {
                        Text(
                            text = st.label,
                            color = if (selected) AegisOnPrimaryWhite else AegisTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun BrowserSettingsFullDialog(
    preferences: AegisPreferences,
    searchEngine: BrowserSearchEngine,
    adBlockEnabled: Boolean,
    popupsBlocked: Boolean,
    javaScriptEnabled: Boolean,
    desktopDefault: Boolean,
    blockAdult: Boolean,
    chromeCachedCount: Int,
    sitePermissionsJson: String,
    onSearchEngineChanged: (BrowserSearchEngine) -> Unit,
    onReCacheChrome: () -> Unit,
    onImportChrome: () -> Unit,
    onRequestDefaultBrowser: () -> Unit,
    onPinBrowserIcon: () -> Unit,
    onPrintPage: () -> Unit,
    onSavePdfPage: () -> Unit,
    onClearBrowsingData: () -> Unit,
    onDismiss: () -> Unit
) {
    val parsedPermissions = remember(sitePermissionsJson) {
        BrowserPermissionsStore.parseSitePermissions(sitePermissionsJson).values.toList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AegisLavenderSoft,
        modifier = Modifier
            .vipAnimatedCorners(
                cornerRadius = 22.dp,
                accentStart = AegisPrimaryCyan,
                accentEnd = AegisPurple,
                strokeWidth = 2.dp
            )
            .testTag("browser_settings_dialog"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SafeBrowserBrandIcon(size = 28.dp, isProUnlocked = true)
                Text(
                    text = "Safe Browser Settings",
                    fontFamily = ClassicalDisplayFont,
                    fontWeight = FontWeight.Bold,
                    color = AegisTextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Search Engine",
                    color = AegisPurple,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    BrowserSearchEngine.entries.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            pair.forEach { engine ->
                                val selected = engine == searchEngine
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) AegisPrimaryCyan else Color.White,
                                    border = BorderStroke(1.dp, if (selected) AegisPrimaryCyan else AegisCardBorder),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 42.dp)
                                        .clickable { onSearchEngineChanged(engine) }
                                        .testTag("browser_search_engine_${engine.id}")
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = engine.displayName,
                                            color = if (selected) AegisOnPrimaryWhite else AegisTextPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                HorizontalDivider(color = AegisCardBorder)
                Text(
                    text = "Privacy, Ad Shield & Engine",
                    color = AegisPurple,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                SettingsToggleRow(
                    title = "Ad & Tracker Shield",
                    checked = adBlockEnabled,
                    tag = "browser_setting_adblock",
                    onCheckedChange = { preferences.setBrowserAdBlockEnabled(it) }
                )
                SettingsToggleRow(
                    title = "Block Intrusive Pop-ups",
                    checked = popupsBlocked,
                    tag = "browser_setting_popups",
                    onCheckedChange = { preferences.setBrowserPopupsBlocked(it) }
                )
                SettingsToggleRow(
                    title = "JavaScript Engine",
                    checked = javaScriptEnabled,
                    tag = "browser_setting_js",
                    onCheckedChange = { preferences.setBrowserJavaScriptEnabled(it) }
                )
                SettingsToggleRow(
                    title = "Default Desktop Mode",
                    checked = desktopDefault,
                    tag = "browser_setting_desktop",
                    onCheckedChange = { preferences.setBrowserDesktopModeDefault(it) }
                )
                SettingsToggleRow(
                    title = "Block Adult Content",
                    checked = blockAdult,
                    tag = "browser_setting_adult",
                    onCheckedChange = { preferences.setBlockExplicitAdult(it) }
                )

                HorizontalDivider(color = AegisCardBorder)
                Text(
                    text = "Page Export & System Integration",
                    color = AegisPurple,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onSavePdfPage,
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onPrintPage,
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Print", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = onPinBrowserIcon,
                        colors = ButtonDefaults.buttonColors(containerColor = AegisPrimaryCyan, contentColor = AegisOnPrimaryWhite),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("browser_settings_pin_home_button")
                    ) {
                        Icon(Icons.Default.AddHome, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pin to Home", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onRequestDefaultBrowser,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("browser_settings_default_button")
                    ) {
                        Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Default App", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onReCacheChrome,
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cache ($chromeCachedCount)", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onImportChrome,
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import Chrome", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (parsedPermissions.isNotEmpty()) {
                    HorizontalDivider(color = AegisCardBorder)
                    Text(
                        text = "Saved Site Permissions (${parsedPermissions.size})",
                        color = AegisPurple,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    parsedPermissions.take(5).forEach { entry ->
                        Text(
                            text = "${entry.origin} • Cam:${entry.camera.label} Mic:${entry.microphone.label} Loc:${entry.location.label}",
                            fontSize = 10.5.sp,
                            color = AegisTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                OutlinedButton(
                    onClick = onClearBrowsingData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("browser_settings_clear_data_button")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(15.dp), tint = AegisChampagneGold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear History, Cookies & Site Permissions", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AegisTextPrimary)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = AegisPurple, contentColor = AegisOnPrimaryWhite),
                modifier = Modifier.testTag("browser_settings_close_button")
            ) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun SettingsToggleRow(
    title: String,
    checked: Boolean,
    tag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.5.sp,
            color = AegisTextPrimary,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AegisOnPrimaryWhite,
                checkedTrackColor = AegisPurple
            ),
            modifier = Modifier.testTag(tag)
        )
    }
}

@Composable
internal fun BrowserLibraryDrawerDialog(
    activeSheet: BrowserDrawerSheet,
    bookmarks: List<SafeBookmark>,
    historyEntries: List<BrowserHistoryEntry>,
    downloadItems: List<BrowserDownloadItem>,
    onNavigateToUrl: (String) -> Unit,
    onClearHistory: () -> Unit,
    onOpenDownload: (BrowserDownloadItem) -> Unit,
    onDismiss: () -> Unit
) {
    if (activeSheet == BrowserDrawerSheet.NONE) return
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AegisLavenderSoft,
        title = {
            Text(
                text = when (activeSheet) {
                    BrowserDrawerSheet.BOOKMARKS -> "Bookmarks (${bookmarks.size})"
                    BrowserDrawerSheet.HISTORY -> "Browsing History"
                    BrowserDrawerSheet.DOWNLOADS -> "Downloads & Saved PDFs"
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
                                            onDismiss()
                                            onNavigateToUrl(bm.url)
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
                                    Icon(
                                        imageVector = Icons.Default.OpenInNew,
                                        contentDescription = "Open Bookmark",
                                        tint = AegisPurple,
                                        modifier = Modifier.size(16.dp)
                                    )
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
                                OutlinedButton(onClick = onClearHistory) {
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
                                                onDismiss()
                                                onNavigateToUrl(entry.url)
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
                                text = "Downloads and Saved Webpage PDFs will appear here.",
                                color = AegisTextSecondary,
                                fontSize = 12.sp
                            )
                        } else {
                            val fmt = remember { SimpleDateFormat("MMM d, HH:mm", Locale.US) }
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(downloadItems, key = { it.id }) { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color.White)
                                            .clickable { onOpenDownload(item) }
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
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
                                        Icon(
                                            imageVector = Icons.Default.OpenInNew,
                                            contentDescription = "Open File",
                                            tint = AegisPrimaryCyan,
                                            modifier = Modifier.size(16.dp)
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
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = AegisPurple, contentColor = AegisOnPrimaryWhite)
            ) {
                Text("Close")
            }
        }
    )
}

internal fun clearAllBrowserSessionData(
    preferences: AegisPreferences,
    webView: WebView?,
    context: android.content.Context
) {
    preferences.setBrowserHistoryJson("")
    preferences.setBrowserSitePermissionsJson("")
    runCatching {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        webView?.clearCache(true)
        webView?.clearHistory()
        webView?.clearFormData()
    }
    Toast.makeText(context, "Cleared browsing history, cookies & site permissions", Toast.LENGTH_SHORT).show()
}
