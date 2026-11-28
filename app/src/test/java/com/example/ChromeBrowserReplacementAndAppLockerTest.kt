package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.data.preferences.AegisPreferences
import com.example.data.preferences.BrowserSearchEngine
import com.example.di.AegisContainer
import com.example.ui.browser.BrowserActivity
import com.example.ui.browser.BrowserPdfExportHelper
import com.example.ui.browser.BrowserPermissionsStore
import com.example.ui.browser.HomeShortcutHelper
import com.example.ui.browser.SafeBrowserFilterHelper
import com.example.ui.browser.SafeBrowserScreen
import com.example.ui.browser.SitePermissionState
import com.example.ui.browser.SitePermissionType
import com.example.ui.components.createSafeBrowserBrandBitmap
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.AegisShieldTheme
import com.example.widget.SheildlenzAppWidgetProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ChromeBrowserReplacementAndAppLockerTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `full pro and standalone mode auto-hide app header and show chrome omnibox speed dial tab grid and permissions`() {
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
                    isStandaloneWindow = false,
                    onNavigateBack = {}
                )
            }
        }
        composeRule.waitForIdle()

        // App header hidden automatically once Full Pro is active
        composeRule.onNodeWithTag("safe_browser_back_home").assertDoesNotExist()
        composeRule.onNodeWithTag("safe_browser_toolbar").assertIsDisplayed()
        composeRule.onNodeWithTag("safe_browser_tabs_counter_button").assertIsDisplayed()
        composeRule.onNodeWithTag("safe_browser_overflow_button").assertIsDisplayed()
        composeRule.onNodeWithTag("safe_browser_site_lock_button").assertIsDisplayed()

        // Open new tab via Chrome overflow menu -> Speed Dial NTP renders
        composeRule.onNodeWithTag("safe_browser_overflow_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("safe_browser_new_tab_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("safe_browser_speed_dial").assertIsDisplayed()

        // Open 2-column Tab Grid Switcher & create Incognito tab
        composeRule.onNodeWithTag("safe_browser_tabs_counter_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("safe_browser_tab_grid_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_grid_new_incognito_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("safe_browser_speed_dial").assertIsDisplayed()

        // Open Site Permissions manager & toggle Camera to Allow
        composeRule.onNodeWithTag("safe_browser_site_lock_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("site_permissions_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("site_permissions_close_button").performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun `search engines ad shield and per-site permission store resolve safely`() {
        val ddgUrl = SafeBrowserFilterHelper.resolveSafeNavigationUrl(
            input = "solar system planets",
            searchEngine = BrowserSearchEngine.DUCKDUCKGO
        )
        assertTrue(ddgUrl.contains("duckduckgo.com"))
        assertTrue(ddgUrl.contains("kp=1"))

        val braveUrl = SafeBrowserFilterHelper.resolveSafeNavigationUrl(
            input = "kotlin compose",
            searchEngine = BrowserSearchEngine.BRAVE_SEARCH
        )
        assertTrue(braveUrl.contains("search.brave.com"))
        assertTrue(braveUrl.contains("safesearch=strict"))

        assertTrue(SafeBrowserFilterHelper.isTrackerOrAdDomain("https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js"))
        assertTrue(SafeBrowserFilterHelper.isTrackerOrAdDomain("https://www.google-analytics.com/analytics.js"))
        assertFalse(SafeBrowserFilterHelper.isTrackerOrAdDomain("https://en.wikipedia.org/wiki/Compass"))

        var json = ""
        json = BrowserPermissionsStore.updatePolicy(
            rawJson = json,
            url = "https://meet.example.org/room/123",
            type = SitePermissionType.CAMERA,
            state = SitePermissionState.ALLOW
        )
        json = BrowserPermissionsStore.updatePolicy(
            rawJson = json,
            url = "https://meet.example.org/room/123",
            type = SitePermissionType.LOCATION,
            state = SitePermissionState.BLOCK
        )
        assertEquals(
            SitePermissionState.ALLOW,
            BrowserPermissionsStore.getPolicy(json, "https://meet.example.org/other", SitePermissionType.CAMERA)
        )
        assertEquals(
            SitePermissionState.BLOCK,
            BrowserPermissionsStore.getPolicy(json, "https://meet.example.org/other", SitePermissionType.LOCATION)
        )
        assertEquals(
            SitePermissionState.ASK,
            BrowserPermissionsStore.getPolicy(json, "https://meet.example.org/other", SitePermissionType.MICROPHONE)
        )
    }

    @Test
    fun `pdf export creates readable pdf brand bitmap renders and browser activity launches cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val exported = BrowserPdfExportHelper.saveWebPageAsPdf(
            context = context,
            webView = null,
            pageTitle = "Solar Science Article",
            pageUrl = "https://science.nasa.gov/solar-system"
        )
        assertNotNull(exported)
        val outFile = File(exported!!.url)
        assertTrue(outFile.exists())
        assertTrue(outFile.length() > 100L)
        assertEquals("application/pdf", exported.mimeType)
        outFile.delete()

        val brandBitmap = createSafeBrowserBrandBitmap(sizePx = 256, isProUnlocked = true)
        assertEquals(256, brandBitmap.width)
        assertEquals(256, brandBitmap.height)

        val shortcutIntent = HomeShortcutHelper.buildStandaloneBrowserIntent(
            context = context,
            targetUrl = "https://en.wikipedia.org"
        )
        assertEquals(BrowserActivity::class.java.name, shortcutIntent.component?.className)

        val controller = Robolectric.buildActivity(
            BrowserActivity::class.java,
            Intent(Intent.ACTION_VIEW, Uri.parse("https://en.wikipedia.org"))
        ).create().start().resume()
        assertNotNull(controller.get())
        controller.pause().stop().destroy()

        val widgetViews = SheildlenzAppWidgetProvider.buildRemoteViews(context)
        assertNotNull(widgetViews)
    }

    @Test
    fun `app locker gates locked packages requires guardian pin temp unlock relock on exit and policy backup`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = AegisPreferences(context)
        val targetPkg = "com.example.socialapp"

        // Without Guardian PIN set, App Locker does not enforce yet
        prefs.setAppLockerEnabled(true)
        prefs.setLockedAppPackages(setOf(targetPkg))
        assertFalse(prefs.isPackageLockedByAppLocker(targetPkg))

        // Set Guardian PIN -> App Locker actively enforces locked package
        prefs.setGuardianPin("2580")
        assertTrue(prefs.isPackageLockedByAppLocker(targetPkg))

        // Relock Every Exit (0 minutes) unlocks while targetPkg is active, re-locks on switch
        prefs.setAppLockerRelockMinutes(0)
        prefs.grantTempAppUnlock(targetPkg, durationMinutes = 0)
        assertFalse(prefs.isPackageLockedByAppLocker(targetPkg))
        prefs.onForegroundPackageChanged("com.other.calculator")
        assertTrue(prefs.isPackageLockedByAppLocker(targetPkg))

        // Timed grace unlock stays unlocked until grace expires or screen locks
        prefs.setAppLockerRelockMinutes(15)
        prefs.grantTempAppUnlock(targetPkg, durationMinutes = 15, nowRealtimeMs = 10_000L)
        assertFalse(prefs.isPackageLockedByAppLocker(targetPkg, nowRealtimeMs = 5 * 60_000L))
        assertTrue(prefs.isPackageLockedByAppLocker(targetPkg, nowRealtimeMs = 20 * 60_000L))

        // Policy JSON export/restore preserves App Locker and Browser search engine settings
        prefs.setBrowserSearchEngine(BrowserSearchEngine.DUCKDUCKGO)
        prefs.setBrowserAdBlockEnabled(false)
        val backupJson = prefs.exportPolicyJson()

        prefs.setAppLockerEnabled(false)
        prefs.setLockedAppPackages(emptySet())
        prefs.setAppLockerRelockMinutes(0)
        prefs.setBrowserSearchEngine(BrowserSearchEngine.GOOGLE_SAFE)
        prefs.setBrowserAdBlockEnabled(true)

        assertTrue(prefs.restorePolicyJson(backupJson))
        assertTrue(prefs.appLockerEnabled.value)
        assertTrue(prefs.lockedAppPackages.value.contains(targetPkg))
        assertEquals(15, prefs.appLockerRelockMinutes.value)
        assertEquals(BrowserSearchEngine.DUCKDUCKGO, prefs.browserSearchEngine.value)
        assertFalse(prefs.browserAdBlockEnabled.value)
    }

    @Test
    fun `settings screen shows individual app locker card and relock options`() {
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
            .performScrollToNode(hasTestTag("edits_app_locker_card"))
        composeRule.onNodeWithTag("edits_app_locker_card").assertIsDisplayed()
        composeRule.onNodeWithTag("app_locker_relock_15m").performClick()
        composeRule.waitForIdle()
        assertEquals(15, container.preferences.appLockerRelockMinutes.value)
    }
}
