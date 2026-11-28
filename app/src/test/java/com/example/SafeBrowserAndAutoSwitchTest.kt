package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.core.alerts.DiscreetAlertFormatter
import com.example.core.models.SafeSearchPack
import com.example.data.preferences.AegisPreferences
import com.example.data.preferences.ProtectionProfile
import com.example.data.preferences.QuickTileMode
import com.example.di.AegisContainer
import com.example.services.protection.ProfileAutoScheduler
import com.example.ui.browser.BrowserShieldAction
import com.example.ui.browser.BrowserTabState
import com.example.ui.browser.ChromeComponentCacheManager
import com.example.ui.browser.DEFAULT_SAFE_HOME_URL
import com.example.ui.browser.SafeBrowserFilterHelper
import com.example.ui.browser.SafeBrowserScreen
import java.io.File
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.AegisShieldTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SafeBrowserAndAutoSwitchTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `profile auto scheduler evaluates window midnight wrap masterPower guard and avoids duplicates`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = AegisPreferences(context)

        prefs.setMasterPowerEnabled(true)
        prefs.setScheduledTargetProfile(ProtectionProfile.HOMEWORK_FOCUS)
        prefs.setScheduledFallbackProfile(ProtectionProfile.BALANCED_GUARD)
        prefs.setProfileScheduleWindow(startMinutes = 22 * 60, endMinutes = 6 * 60)
        prefs.setProfileAutoSwitchEnabled(true)

        // Inside midnight-wrapping window (23:30)
        val firstApply = ProfileAutoScheduler.evaluateAndApply(prefs, currentMinuteOfDay = 23 * 60 + 30)
        assertEquals(ProtectionProfile.HOMEWORK_FOCUS, firstApply)
        assertEquals(ProfileAutoScheduler.SLOT_TARGET, prefs.lastAppliedAutoSwitchSlot.value)

        // Re-evaluating within same slot should NOT duplicate apply
        val duplicate = ProfileAutoScheduler.evaluateAndApply(prefs, currentMinuteOfDay = 1 * 60)
        assertNull(duplicate)

        // Outside window (12:00) switches cleanly to fallback profile
        val fallbackApply = ProfileAutoScheduler.evaluateAndApply(prefs, currentMinuteOfDay = 12 * 60)
        assertEquals(ProtectionProfile.BALANCED_GUARD, fallbackApply)
        assertEquals(ProfileAutoScheduler.SLOT_FALLBACK, prefs.lastAppliedAutoSwitchSlot.value)

        // Master power OFF suppresses auto-switching
        prefs.setMasterPowerEnabled(false)
        assertNull(
            ProfileAutoScheduler.evaluateAndApply(
                prefs,
                currentMinuteOfDay = 23 * 60,
                forceApply = true
            )
        )

        // Policy JSON export/restore preserves auto-switch, quick tile mode & full browser flag
        prefs.setMasterPowerEnabled(true)
        prefs.setQuickTileMode(QuickTileMode.STUDY_LOCK)
        prefs.setFullBrowserEngineInstalled(true)
        prefs.setChromeComponentsAutoCached(true)
        prefs.setChromeCachedComponentCount(6)
        val exported = prefs.exportPolicyJson()

        prefs.setProfileAutoSwitchEnabled(false)
        prefs.setQuickTileMode(QuickTileMode.SAFE_DNS)
        prefs.setFullBrowserEngineInstalled(false)
        prefs.setChromeComponentsAutoCached(false)
        prefs.setChromeCachedComponentCount(0)

        assertTrue(prefs.restorePolicyJson(exported))
        assertTrue(prefs.profileAutoSwitchEnabled.value)
        assertEquals(QuickTileMode.STUDY_LOCK, prefs.quickTileMode.value)
        assertTrue(prefs.fullBrowserEngineInstalled.value)
        assertTrue(prefs.chromeComponentsAutoCached.value)
        assertEquals(6, prefs.chromeCachedComponentCount.value)
    }

    @Test
    fun `chrome component auto cache manager writes manifest and auto-discovers readable bookmarks`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = AegisPreferences(context)
        prefs.setFullBrowserEngineInstalled(false)
        prefs.setChromeComponentsAutoCached(false)

        val localBookmarks = File(context.filesDir, "Bookmarks.html")
        localBookmarks.writeText(
            """<DL><p><DT><A HREF="https://mit.edu">MIT OpenCourseWare</A></DL><p>"""
        )

        try {
            val result = ChromeComponentCacheManager.autoCacheChromeComponents(context, prefs)
            assertEquals(ChromeComponentCacheManager.CORE_CHROME_MODULES.size, result.cachedModulesCount)
            assertTrue(result.autoDiscoveredBookmarkCount >= 1)
            assertTrue(prefs.fullBrowserEngineInstalled.value)
            assertTrue(prefs.chromeComponentsAutoCached.value)
            assertTrue(prefs.chromeCachedComponentCount.value >= result.cachedModulesCount)
            assertTrue(ChromeComponentCacheManager.isManifestCachedOnDisk(context))
            val savedBookmarks = SafeBrowserFilterHelper.deserializeBookmarks(prefs.browserBookmarksJson.value)
            assertTrue(savedBookmarks.any { it.url == "https://mit.edu" })
        } finally {
            localBookmarks.delete()
        }
    }

    @Test
    fun `safe browser filter closes tab quietly when shield is off and enforces full shield when on`() {
        val offDecision = SafeBrowserFilterHelper.decideActionForNavigation(
            rawUrl = "https://www.google.com/search?q=porn+video",
            isSheildlenzActive = false,
            customDomains = emptySet(),
            customKeywords = setOf("porn"),
            blockAdult = true,
            blockGambling = true,
            smartSafeActive = true,
            safeSearchPacks = SafeSearchPack.DEFAULT_PACK_IDS
        )
        assertEquals(BrowserShieldAction.CLOSE_TAB_QUIETLY, offDecision.action)

        val onDecision = SafeBrowserFilterHelper.decideActionForNavigation(
            rawUrl = "https://www.google.com/search?q=porn+video",
            isSheildlenzActive = true,
            customDomains = emptySet(),
            customKeywords = setOf("porn"),
            blockAdult = true,
            blockGambling = true,
            smartSafeActive = true,
            safeSearchPacks = SafeSearchPack.DEFAULT_PACK_IDS
        )
        assertEquals(BrowserShieldAction.FULL_SHIELD_ENFORCEMENT, onDecision.action)

        val cleanDecision = SafeBrowserFilterHelper.decideActionForNavigation(
            rawUrl = "https://en.wikipedia.org/wiki/Solar_System",
            isSheildlenzActive = true,
            customDomains = emptySet(),
            customKeywords = emptySet(),
            blockAdult = true,
            blockGambling = true,
            smartSafeActive = true,
            safeSearchPacks = SafeSearchPack.DEFAULT_PACK_IDS
        )
        assertEquals(BrowserShieldAction.NONE, cleanDecision.action)

        // Closing the only tab spawns a clean fallback Safe Search tab
        val t1 = BrowserTabState(id = "tab-1", title = "Blocked", url = "https://bad.example")
        val (remainingSingle, activeSingle) = SafeBrowserFilterHelper.closeTabAndEnsureFallback(listOf(t1), "tab-1")
        assertEquals(1, remainingSingle.size)
        assertEquals(DEFAULT_SAFE_HOME_URL, remainingSingle.first().url)
        assertEquals(remainingSingle.first().id, activeSingle)

        // External app scheme detection allows app links while keeping web URLs inside browser
        assertTrue(SafeBrowserFilterHelper.isExternalAppScheme("intent://maps.google.com/#Intent;scheme=https;end"))
        assertTrue(SafeBrowserFilterHelper.isExternalAppScheme("tel:+15551234567"))
        assertFalse(SafeBrowserFilterHelper.isExternalAppScheme("https://khanacademy.org"))
    }

    @Test
    fun `chrome html and json bookmarks importer extracts bookmarks cleanly`() {
        val chromeHtml = """
            <!DOCTYPE NETSCAPE-Bookmark-file-1>
            <DL><p>
              <DT><A HREF="https://developer.android.com/compose">Jetpack Compose Docs</A>
              <DT><A HREF="https://www.khanacademy.org/">Khan Academy</A>
            </DL><p>
        """.trimIndent()
        val parsedHtml = SafeBrowserFilterHelper.parseChromeBookmarksFile(chromeHtml)
        assertEquals(2, parsedHtml.size)
        assertEquals("Jetpack Compose Docs", parsedHtml[0].title)
        assertTrue(parsedHtml[0].importedFromChrome)

        val chromeJson = """
            {
              "roots": {
                "bookmark_bar": {
                  "type": "folder",
                  "name": "Bookmarks Bar",
                  "children": [
                    { "type": "url", "name": "NASA Science", "url": "https://science.nasa.gov/" }
                  ]
                }
              }
            }
        """.trimIndent()
        val parsedJson = SafeBrowserFilterHelper.parseChromeBookmarksFile(chromeJson)
        assertEquals(1, parsedJson.size)
        assertEquals("https://science.nasa.gov/", parsedJson[0].url)
        assertEquals("NASA Science", parsedJson[0].title)
    }

    @Test
    fun `discreet alert formatter produces calm copy in discreet mode`() {
        val calmNotice = DiscreetAlertFormatter.tabAutoClosedNotice(discreetMode = true)
        assertTrue(calmNotice.contains("quietly", ignoreCase = true))
        val calmHead = DiscreetAlertFormatter.browserBlockHeadline(discreetMode = true)
        assertEquals("Quiet Pause Applied", calmHead)
    }

    @Test
    fun `home screen shows safe browser card and navigates when clicked`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AegisContainer(context)
        container.preferences.setMasterPowerEnabled(true)
        var navigatedToBrowser = false

        composeRule.setContent {
            AegisShieldTheme {
                DashboardScreen(
                    protectionRepo = container.protectionRepository,
                    modelManager = container.modelManager,
                    preferences = container.preferences,
                    onNavigateToSafeBrowser = { navigatedToBrowser = true }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNode(hasScrollAction())
            .performScrollToNode(hasTestTag("home_safe_browser_card"))
        composeRule.onNodeWithTag("home_safe_browser_card").assertIsDisplayed()
        composeRule.onNodeWithTag("home_safe_browser_open_button").performClick()
        assertTrue(navigatedToBrowser)
    }

    @Test
    fun `settings screen shows profile auto switcher and quick tile mode selector`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AegisContainer(context)
        container.preferences.setMasterPowerEnabled(true)
        container.preferences.unlockGuardianSession()

        composeRule.setContent {
            AegisShieldTheme {
                SettingsScreen(
                    preferences = container.preferences,
                    modelManager = container.modelManager,
                    protectionRepo = container.protectionRepository
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNode(hasScrollAction())
            .performScrollToNode(hasTestTag("edits_profile_auto_switch_card"))
        composeRule.onNodeWithTag("edits_profile_auto_switch_card").assertIsDisplayed()

        composeRule.onNode(hasScrollAction())
            .performScrollToNode(hasTestTag("edits_quick_tile_mode_selector"))
        composeRule.onNodeWithTag("edits_quick_tile_mode_selector").assertIsDisplayed()

        composeRule.onNodeWithTag("quick_tile_mode_${QuickTileMode.STUDY_LOCK.id}").performClick()
        composeRule.waitForIdle()
        assertEquals(QuickTileMode.STUDY_LOCK, container.preferences.quickTileMode.value)
    }

    @Test
    fun `safe browser light mode confirms full upgrade and auto-caches chrome components in one tap`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AegisContainer(context)
        container.preferences.setFullBrowserEngineInstalled(false)
        container.preferences.setChromeComponentsAutoCached(false)

        composeRule.setContent {
            AegisShieldTheme {
                SafeBrowserScreen(
                    preferences = container.preferences,
                    protectionRepo = container.protectionRepository,
                    blockedDnsLogRepo = container.blockedDnsLogRepository,
                    overlayController = container.overlayController,
                    onNavigateBack = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("safe_browser_root").assertIsDisplayed()
        composeRule.onNodeWithTag("safe_browser_upgrade_card").assertDoesNotExist()
        composeRule.onNodeWithTag("safe_browser_tab_strip").assertDoesNotExist()

        // Open Chrome overflow menu -> Upgrade Pro -> Confirmation Dialog opens
        composeRule.onNodeWithTag("safe_browser_overflow_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("safe_browser_upgrade_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("safe_browser_upgrade_confirm_dialog").assertIsDisplayed()

        // Single confirmation tap unlocks Full Browser AND auto-caches Chrome components
        composeRule.onNodeWithTag("safe_browser_confirm_upgrade_button").performClick()
        composeRule.mainClock.advanceTimeBy(1200L)
        composeRule.waitForIdle()

        assertTrue(container.preferences.fullBrowserEngineInstalled.value)
        assertTrue(container.preferences.chromeComponentsAutoCached.value)
        composeRule.onNodeWithTag("safe_browser_tab_strip").assertDoesNotExist()
        composeRule.onNodeWithTag("safe_browser_overflow_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("safe_browser_chrome_cache_badge").assertIsDisplayed()
        composeRule.onNodeWithTag("safe_browser_import_chrome_button").assertIsDisplayed()
        composeRule.onNodeWithTag("safe_browser_bookmarks_button").assertIsDisplayed()
        composeRule.onNodeWithTag("safe_browser_history_button").assertIsDisplayed()
        composeRule.onNodeWithTag("safe_browser_downloads_button").assertIsDisplayed()
        assertNotNull(container.preferences.fullBrowserEngineInstalled.value)
    }

    @Test
    fun `safe browser full pro features including settings and bookmarks shortcuts`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AegisContainer(context)
        container.preferences.setFullBrowserEngineInstalled(true)
        container.preferences.setChromeComponentsAutoCached(true)

        composeRule.setContent {
            AegisShieldTheme {
                SafeBrowserScreen(
                    preferences = container.preferences,
                    protectionRepo = container.protectionRepository,
                    blockedDnsLogRepo = container.blockedDnsLogRepository,
                    overlayController = container.overlayController,
                    onNavigateBack = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("safe_browser_root").assertIsDisplayed()
        composeRule.onNodeWithTag("safe_browser_overflow_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("safe_browser_settings_button").performScrollTo()
        composeRule.onNodeWithTag("safe_browser_settings_button").assertIsDisplayed()
        composeRule.onNodeWithTag("safe_browser_settings_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("browser_setting_safesearch").assertDoesNotExist()
        composeRule.onNodeWithTag("browser_settings_close_button").assertIsDisplayed()
        composeRule.onNodeWithTag("browser_settings_close_button").performClick()
        composeRule.waitForIdle()
    }
}
