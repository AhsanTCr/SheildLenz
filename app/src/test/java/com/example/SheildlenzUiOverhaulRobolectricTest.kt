package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.example.core.audio.AegisSoundEffects
import com.example.core.models.SensitivityLevel
import com.example.core.models.ThreatCategory
import com.example.core.models.ThreatSeverity
import com.example.data.repository.ParentChildRepository
import com.example.di.AegisContainer
import com.example.ui.about.AboutScreen
import com.example.ui.components.CyberTopBar
import com.example.ui.components.QrCodeGenerator
import com.example.ui.components.SeverityTag
import com.example.ui.components.StatusPill
import com.example.ui.components.ThreatCategoryBadge
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.history.SecurityHistoryScreen
import com.example.ui.navigation.AegisDestinations
import com.example.ui.navigation.AegisNavHost
import com.example.ui.parents.ParentsScreen
import com.example.ui.protection.LiveProtectionScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.splash.SplashScreen
import com.example.ui.theme.AegisShieldTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SheildlenzUiOverhaulRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `top bar renders clean band with separate back button callback`() {
        var backTapped = false
        composeRule.setContent {
            AegisShieldTheme {
                CyberTopBar(
                    title = "Progress",
                    onNavigateBack = { backTapped = true }
                )
            }
        }

        composeRule.onNodeWithTag("sheildlenz_compact_top_bar").assertIsDisplayed()
        composeRule.onNodeWithText("SHEILDLENZ").assertIsDisplayed()
        composeRule.onNodeWithText("Progress").assertIsDisplayed()
        composeRule.onNodeWithTag("topbar_back_button").performClick()
        assertTrue(backTapped)
    }

    @Test
    fun `home renders priority tiles and triggers focus hub navigation`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AegisContainer(context)
        var openedFocusHub = false

        composeRule.setContent {
            AegisShieldTheme {
                DashboardScreen(
                    protectionRepo = container.protectionRepository,
                    modelManager = container.modelManager,
                    preferences = container.preferences,
                    onNavigateToFocusHub = { openedFocusHub = true }
                )
            }
        }

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("home_one_tap_priority_strip"))
        composeRule.onNodeWithTag("home_one_tap_safe_dns").assertIsDisplayed()
        composeRule.onNodeWithTag("home_one_tap_bedtime").assertIsDisplayed()
        composeRule.onNodeWithTag("home_one_tap_strict").assertIsDisplayed()
        composeRule.onNodeWithTag("home_one_tap_budgets").performClick()
        composeRule.waitForIdle()
        assertTrue(openedFocusHub)
    }

    @Test
    fun `status screen triggers progress and wellbeing gateway callbacks`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AegisContainer(context)
        var openedProgress = false
        var openedWellbeing = false

        composeRule.setContent {
            AegisShieldTheme {
                LiveProtectionScreen(
                    protectionRepo = container.protectionRepository,
                    preferences = container.preferences,
                    onNavigateToProgress = { openedProgress = true },
                    onNavigateToWellbeing = { openedWellbeing = true }
                )
            }
        }
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("status_open_progress_button"))
        composeRule.onNodeWithTag("status_open_progress_button").performClick()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("status_open_wellbeing_button"))
        composeRule.onNodeWithTag("status_open_wellbeing_button").performClick()
        composeRule.waitForIdle()
        assertTrue(openedProgress)
        assertTrue(openedWellbeing)
    }

    @Test
    fun `progress screen omits top widgets and toggles security history with one tap`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AegisContainer(context)

        composeRule.setContent {
            AegisShieldTheme {
                SecurityHistoryScreen(
                    historyRepo = container.securityHistoryRepository,
                    preferences = container.preferences,
                    onNavigateBack = {}
                )
            }
        }

        assertEquals(0, composeRule.onAllNodesWithText("THREAT OVERVIEW").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("PROTECTION LEDGER").fetchSemanticsNodes().size)
        composeRule.onNodeWithText("Blocked Applications").assertIsDisplayed()

        // Closed by default
        assertEquals(0, composeRule.onAllNodesWithText("No Threats Detected").fetchSemanticsNodes().size)

        // One-tap horizontal toggle opens it cleanly
        composeRule.onNodeWithTag("toggle_security_history_button").performClick()
        composeRule.onNodeWithText("No Threats Detected").assertIsDisplayed()
    }

    @Test
    fun `status screen removes AI Classification and duplicate Permission Health Audit`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AegisContainer(context)

        composeRule.setContent {
            AegisShieldTheme {
                LiveProtectionScreen(
                    protectionRepo = container.protectionRepository,
                    preferences = container.preferences,
                    onNavigateBack = {}
                )
            }
        }

        composeRule.onNodeWithTag("status_heartbeat_card").assertIsDisplayed()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("security_health_gauge_card"))
        composeRule.onNodeWithTag("security_health_gauge_card").assertIsDisplayed()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("PERMISSIONS"))
        assertTrue(composeRule.onAllNodesWithText("PERMISSIONS").fetchSemanticsNodes().isNotEmpty())
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Overlay"))
        composeRule.onNodeWithText("Overlay").assertIsDisplayed()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Device Admin"))
        composeRule.onNodeWithText("Device Admin").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithText("AI CLASSIFICATION").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("Permission Health Audit").fetchSemanticsNodes().size)
    }

    @Test
    fun `edits screen shows compact pills and tapping Quiet Hours opens popup edits dialog`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AegisContainer(context)
        container.preferences.setScheduleEnabled(false)

        composeRule.setContent {
            AegisShieldTheme {
                SettingsScreen(
                    preferences = container.preferences,
                    modelManager = container.modelManager,
                    protectionRepo = container.protectionRepository
                )
            }
        }

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("AI CLASSIFICATION"))
        composeRule.onNodeWithText("AI CLASSIFICATION").assertIsDisplayed()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("ai_model_pill"))
        composeRule.onNodeWithTag("ai_model_pill").assertIsDisplayed()
        // Quiet Hours starts as a compact pill without inline presets clutter
        assertEquals(0, composeRule.onAllNodesWithText("10P–6A").fetchSemanticsNodes().size)

        // Single tap on Quiet Hours pill opens its popup edits dialog cleanly
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("scheduled_protection_card"))
        composeRule.onNodeWithTag("scheduled_protection_card").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("scheduled_protection_card_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("schedule_enabled_switch").performClick()
        composeRule.waitForIdle()
        assertTrue(container.preferences.scheduleEnabled.value)
        composeRule.onNodeWithText("10P–6A").assertIsDisplayed()
        composeRule.onNodeWithText("DONE").performClick()
    }

    @Test
    fun `parents screen shows both As Parent and As Child options and switches views cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AegisContainer(context)
        container.preferences.clearParentalConnectionState()

        composeRule.setContent {
            AegisShieldTheme {
                ParentsScreen(
                    repository = container.parentChildRepository,
                    preferences = container.preferences,
                    syncController = container.parentChildSyncController
                )
            }
        }

        composeRule.onNodeWithTag("parents_screen_root").assertIsDisplayed()
        composeRule.onNodeWithTag("parents_google_signin_button").assertIsDisplayed()

        // Scroll to role selector card and verify both As Parent and As Child are visible
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("connect_as_parent_button"))
        composeRule.onNodeWithTag("connect_as_parent_button").assertIsDisplayed()
        composeRule.onNodeWithTag("connect_as_child_button").assertIsDisplayed()

        // Tap As Parent -> opens Parent pairing card
        composeRule.onNodeWithTag("connect_as_parent_button").performClick()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("parent_pair_child_card"))
        composeRule.onNodeWithTag("parent_pairing_code_input").assertIsDisplayed()
        composeRule.onNodeWithTag("pair_child_button").assertIsDisplayed()

        // Tap As Child -> shows Child QR card + Uninstall Lock card together without scrolling, and tapping QR enlarges it
        composeRule.onNodeWithTag("connect_as_child_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("child_pairing_qr_card").assertIsDisplayed()
        composeRule.onNodeWithTag("child_pairing_qr_matrix").assertIsDisplayed()
        composeRule.onNodeWithTag("child_connection_status_card").assertIsDisplayed()
        composeRule.onNodeWithText("Uninstall Blocked (Child Mode)").assertIsDisplayed()

        composeRule.onNodeWithTag("child_qr_tap_enlarge").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("child_qr_enlarged_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("child_pairing_qr_enlarged").assertIsDisplayed()
        composeRule.onNodeWithText("DONE").performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun `child uninstall lock blocks uninstall until parent grants temporary approval`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AegisContainer(context)
        val prefs = container.preferences
        prefs.clearParentalConnectionState()

        // Standalone / Parent role never blocks uninstall via child lock
        prefs.setParentalRole("parent")
        assertFalse(prefs.isChildUninstallBlocked())

        // Child role locks uninstall by default
        prefs.setParentalRole("child")
        prefs.setChildUninstallLocked(true)
        assertTrue(prefs.isChildUninstallBlocked())

        // Parent approves uninstall window -> unblocks uninstall for 5 minutes
        prefs.setParentApprovedUninstallUntil(System.currentTimeMillis() + 300_000L)
        assertFalse(prefs.isChildUninstallBlocked())
    }

    @Test
    fun `qr code encoder and pairing URI parser work accurately`() {
        val uri = ParentChildRepository.buildPairingUri("482910")
        assertEquals("sheildlenz://pair?code=482910", uri)
        assertEquals("482910", ParentChildRepository.extractPairingCode(uri))
        assertEquals("482910", ParentChildRepository.extractPairingCode("482910"))

        val matrix = QrCodeGenerator.encode(uri)
        assertTrue(matrix.size >= 21)
        assertEquals(matrix.size, matrix[0].size)
    }

    @Test
    fun `bottom navigation shows Home Status Edits Parents About and excludes Progress tab`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AegisContainer(context)
        container.preferences.setOnboardingCompleted(true)
        container.preferences.setGuardianPin("1234")
        container.preferences.unlockGuardianSession()

        lateinit var navController: NavHostController
        composeRule.setContent {
            navController = rememberNavController()
            AegisShieldTheme {
                AegisNavHost(
                    container = container,
                    navController = navController
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.runOnUiThread {
            navController.navigate(AegisDestinations.HOME) {
                popUpTo(AegisDestinations.SPLASH) { inclusive = true }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("nav_tab_${AegisDestinations.HOME}").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_tab_${AegisDestinations.STATUS}").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_tab_${AegisDestinations.EDITS}").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_tab_${AegisDestinations.PARENTS}").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_tab_${AegisDestinations.ABOUT}").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_tab_${AegisDestinations.PROGRESS}").assertDoesNotExist()

        composeRule.onNodeWithTag("nav_tab_${AegisDestinations.STATUS}").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("live_protection_screen_root").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_tab_${AegisDestinations.PARENTS}").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("parents_screen_root").assertIsDisplayed()
        assertTrue(SensitivityLevel.BALANCED.displayName.isNotBlank())
    }

    @Test
    fun `splash screen renders crisp immediate logo without cluttered status text`() {
        var navigatedTo: String? = null
        composeRule.setContent {
            AegisShieldTheme {
                SplashScreen(
                    isOnboardingCompleted = true,
                    onNavigateNext = { navigatedTo = it }
                )
            }
        }

        composeRule.onNodeWithTag("splash_screen_root").assertIsDisplayed()
        composeRule.onNodeWithText("SHEILDLENZ").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithText("ON-DEVICE SCREEN & NETWORK GUARDIAN").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("Initializing Sheildlenz Security Core...").fetchSemanticsNodes().size)
    }

    @Test
    fun `safe dns pill opens popup with single switch and reveals options once turned on`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AegisContainer(context)
        container.preferences.setSafeDnsEnabled(false)

        composeRule.setContent {
            AegisShieldTheme {
                SettingsScreen(
                    preferences = container.preferences,
                    modelManager = container.modelManager,
                    protectionRepo = container.protectionRepository
                )
            }
        }

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("safe_dns_blocklist_card"))
        composeRule.onNodeWithTag("safe_dns_blocklist_card").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("safe_dns_blocklist_card_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("safe_dns_enabled_switch").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithText("DNS Filter Provider").fetchSemanticsNodes().size)

        // Enabling Safe DNS reveals all options inside the popup dialog
        composeRule.runOnUiThread {
            container.preferences.setSafeDnsEnabled(true)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("DNS Filter Provider").assertIsDisplayed()
        composeRule.onNodeWithText("Blocked Domains").performScrollTo()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Blocked Domains").assertIsDisplayed()
        composeRule.onNodeWithText("DONE").performClick()
    }

    @Test
    fun `about screen shows inline Touch Sounds card with Ceramic Tile Tick and Auto Sound Selector`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AegisContainer(context)
        container.preferences.setTouchSoundsEnabled(true)

        composeRule.setContent {
            AegisShieldTheme {
                AboutScreen(
                    preferences = container.preferences
                )
            }
        }

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("touch_sounds_card"))
        composeRule.onNodeWithTag("touch_sounds_card").performScrollTo()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("touch_sounds_card").assertIsDisplayed()
        composeRule.onNodeWithTag("touch_sounds_switch").assertIsDisplayed()

        // Toggle OFF
        composeRule.onNodeWithTag("touch_sounds_switch").performClick()
        composeRule.waitForIdle()
        assertFalse(container.preferences.touchSoundsEnabled.value)
        assertFalse(AegisSoundEffects.isEnabled())
        composeRule.onNodeWithText("Off • Touch feedback muted").assertIsDisplayed()

        // Toggle back ON via switch & verify Auto Sound Selector and Test Sound controls
        composeRule.onNodeWithTag("touch_sounds_switch").performClick()
        composeRule.waitForIdle()
        assertTrue(container.preferences.touchSoundsEnabled.value)
        assertTrue(AegisSoundEffects.isEnabled())
        composeRule.onNodeWithText("On • Ceramic Tile Tick").assertIsDisplayed()

        composeRule.onNodeWithTag("auto_sound_selector_switch").performScrollTo()
        composeRule.onNodeWithTag("auto_sound_selector_switch").assertIsDisplayed()
        composeRule.onNodeWithTag("auto_sound_selector_switch").performClick()
        composeRule.waitForIdle()
        assertTrue(container.preferences.autoSoundSelectorEnabled.value)
        assertTrue(AegisSoundEffects.isAutoSelectorEnabled())

        composeRule.onNodeWithTag("test_sound_button").performScrollTo()
        composeRule.onNodeWithTag("test_sound_button").assertIsDisplayed()
        composeRule.onNodeWithTag("test_sound_button").performClick()
    }

    @Test
    fun `edits screen opens Excluded Apps picker popup directly on one tap`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AegisContainer(context)
        container.preferences.whitelistedPackages.value.forEach { container.preferences.removeWhitelistedPackage(it) }

        composeRule.setContent {
            AegisShieldTheme {
                SettingsScreen(
                    preferences = container.preferences,
                    modelManager = container.modelManager,
                    protectionRepo = container.protectionRepository
                )
            }
        }

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("whitelisted_apps_card"))
        composeRule.onNodeWithTag("whitelisted_apps_card").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("installed_apps_picker_dialog").assertIsDisplayed()
        composeRule.onNodeWithText("DONE").performClick()
    }

    @Test
    fun `status pill and threat badges keep unconstrained width inside tight horizontal row`() {
        composeRule.setContent {
            AegisShieldTheme {
                androidx.compose.foundation.layout.Column {
                    Row(modifier = Modifier.width(240.dp)) {
                        Text(
                            text = "Update Ready: v16.27 Verified Ready",
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                        StatusPill(label = "READY 0s", isActive = true)
                    }
                    Row(modifier = Modifier.width(800.dp)) {
                        Text(
                            text = "Update Ready: v16.27 Verified Ready",
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                        StatusPill(label = "READY 0s", isActive = true)
                    }
                    Row(modifier = Modifier.width(240.dp)) {
                        Text(
                            text = "Detected Application Package Name",
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                        ThreatCategoryBadge(category = ThreatCategory.EXPLICIT_ADULT)
                    }
                    Row(modifier = Modifier.width(800.dp)) {
                        Text(
                            text = "Detected Application Package Name",
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                        ThreatCategoryBadge(category = ThreatCategory.EXPLICIT_ADULT)
                    }
                    Row(modifier = Modifier.width(240.dp)) {
                        Text(
                            text = "Blocked Incident Header Title",
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                        SeverityTag(severity = ThreatSeverity.CRITICAL)
                    }
                    Row(modifier = Modifier.width(800.dp)) {
                        Text(
                            text = "Blocked Incident Header Title",
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                        SeverityTag(severity = ThreatSeverity.CRITICAL)
                    }
                }
            }
        }

        val readyNodes = composeRule.onAllNodesWithText("READY 0s", ignoreCase = true)
        val readyTight = readyNodes[0].getUnclippedBoundsInRoot()
        val readyWide = readyNodes[1].getUnclippedBoundsInRoot()
        assertEquals(readyWide.right - readyWide.left, readyTight.right - readyTight.left)
        assertEquals(readyWide.bottom - readyWide.top, readyTight.bottom - readyTight.top)

        val catNodes = composeRule.onAllNodesWithText("EXPLICIT ADULT")
        val catTight = catNodes[0].getUnclippedBoundsInRoot()
        val catWide = catNodes[1].getUnclippedBoundsInRoot()
        assertEquals(catWide.right - catWide.left, catTight.right - catTight.left)
        assertEquals(catWide.bottom - catWide.top, catTight.bottom - catTight.top)

        val sevNodes = composeRule.onAllNodesWithText("CRITICAL")
        val sevTight = sevNodes[0].getUnclippedBoundsInRoot()
        val sevWide = sevNodes[1].getUnclippedBoundsInRoot()
        assertEquals(sevWide.right - sevWide.left, sevTight.right - sevTight.left)
        assertEquals(sevWide.bottom - sevWide.top, sevTight.bottom - sevTight.top)
    }

    @Test
    fun `leaving edits tab in nav host automatically locks guardian edits session`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AegisContainer(context)
        container.preferences.setOnboardingCompleted(true)
        container.preferences.setGuardianPin("2580")
        container.preferences.unlockGuardianSession()
        container.preferences.setGuardianPin("2580")

        lateinit var navController: androidx.navigation.NavHostController
        composeRule.setContent {
            AegisShieldTheme {
                navController = androidx.navigation.compose.rememberNavController()
                com.example.ui.navigation.AegisNavHost(
                    container = container,
                    navController = navController
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.runOnUiThread {
            navController.navigate(com.example.ui.navigation.AegisDestinations.EDITS)
        }
        composeRule.waitForIdle()
        assertTrue(container.preferences.isGuardianSessionUnlocked.value)

        // Switching away from Edits to Home must lock Edits immediately
        composeRule.runOnUiThread {
            navController.navigate(com.example.ui.navigation.AegisDestinations.HOME)
        }
        composeRule.waitForIdle()
        assertFalse(container.preferences.isGuardianSessionUnlocked.value)
    }

    @Test
    fun `vip animated bottom bar renders active indicator line and switches tabs`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AegisContainer(context)
        container.preferences.setOnboardingCompleted(true)
        container.preferences.setGuardianPin("1234")
        container.preferences.lockGuardianSession()

        lateinit var navController: NavHostController
        composeRule.setContent {
            navController = rememberNavController()
            AegisShieldTheme {
                AegisNavHost(
                    container = container,
                    navController = navController
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.runOnUiThread {
            navController.navigate(AegisDestinations.HOME) {
                popUpTo(AegisDestinations.SPLASH) { inclusive = true }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("vip_animated_bottom_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("vip_nav_active_line").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_tab_${AegisDestinations.EDITS}").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("edits_lock_gate_card").assertIsDisplayed()
    }

    @Test
    fun `splash screen animates logo emblem with dedicated tag`() {
        composeRule.setContent {
            AegisShieldTheme {
                SplashScreen(
                    isOnboardingCompleted = true,
                    onNavigateNext = {}
                )
            }
        }

        composeRule.onNodeWithTag("splash_animated_logo").assertIsDisplayed()
        composeRule.onNodeWithText("SHEILDLENZ").assertIsDisplayed()
    }

    @Test
    fun `fresh boot dashboard shows Download AI Here alongside From File Recommended`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AegisContainer(context)
        container.preferences.setMasterPowerEnabled(true)

        composeRule.setContent {
            AegisShieldTheme {
                DashboardScreen(
                    protectionRepo = container.protectionRepository,
                    modelManager = container.modelManager,
                    preferences = container.preferences
                )
            }
        }
        composeRule.waitForIdle()

        if (!container.protectionRepository.telemetry.value.isModelLoaded) {
            composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("home_single_download_button"))
            composeRule.onNodeWithText("DOWNLOAD AI HERE").assertIsDisplayed()
            composeRule.onNodeWithTag("home_pick_file_recommended_button").performScrollTo()
            composeRule.onNodeWithText("FROM FILE (RECOMMENDED)").assertIsDisplayed()
        }
    }

    @Test
    fun `redesigned edits lock gate validates wrong pin and unlocks without status shortcut`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AegisContainer(context)
        container.preferences.setGuardianPin("2580")
        container.preferences.lockGuardianSession()

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

        composeRule.onNodeWithTag("edits_lock_gate_card").assertIsDisplayed()
        composeRule.onNodeWithTag("edits_view_status_button").assertDoesNotExist()
        composeRule.onNodeWithText("GUARDIAN VAULT").assertIsDisplayed()
        composeRule.onNodeWithText("Edits Protected").assertIsDisplayed()

        composeRule.onNodeWithTag("edits_unlock_pin_input").performTextInput("0000")
        composeRule.onNodeWithTag("edits_unlock_button").performClick()
        composeRule.waitForIdle()
        assertFalse(container.preferences.isGuardianSessionUnlocked.value)
        composeRule.onNodeWithText("Incorrect Guardian PIN").assertIsDisplayed()

        composeRule.onNodeWithTag("edits_unlock_pin_input").performTextInput("2580")
        composeRule.onNodeWithTag("edits_unlock_button").performClick()
        composeRule.waitForIdle()
        assertTrue(container.preferences.isGuardianSessionUnlocked.value)
    }

    @Test
    fun `status screen renders top protection heartbeat card and health gauge`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AegisContainer(context)
        container.preferences.setMasterPowerEnabled(true)

        composeRule.setContent {
            AegisShieldTheme {
                LiveProtectionScreen(
                    protectionRepo = container.protectionRepository,
                    preferences = container.preferences
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("status_heartbeat_card").assertIsDisplayed()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("security_health_gauge_card"))
        composeRule.onNodeWithTag("security_health_gauge_card").assertIsDisplayed()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("status_edits_overview_card"))
        composeRule.onNodeWithTag("status_edits_overview_card").assertIsDisplayed()
    }

    @Test
    fun `launch setup option 2 routes to device link and option 3 reveals backup restore picker`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = com.example.data.preferences.AegisPreferences(context)
        var linkedDeviceClicked = false

        composeRule.setContent {
            AegisShieldTheme {
                com.example.ui.onboarding.OnboardingScreen(
                    preferences = prefs,
                    onComplete = {},
                    onNavigateToLinkDevice = { linkedDeviceClicked = true }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("launch_option_fresh").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("launch_option_link").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("launch_option_restore").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("launch_option_restore").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("onboarding_choose_backup_button").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("launch_option_link").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("onboarding_get_started_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertTrue(linkedDeviceClicked)
    }

    @Test
    fun `unlocked edits shows quick protection profiles and applies strict kids on tap`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AegisContainer(context)
        container.preferences.setStrictEverything(false)
        container.preferences.setSensitivity(SensitivityLevel.BALANCED)

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

        composeRule.onNodeWithTag("edits_quick_profiles_card").assertIsDisplayed()
        composeRule.onNodeWithTag("profile_card_strict_kids").performClick()
        composeRule.waitForIdle()

        assertEquals(
            com.example.data.preferences.ProtectionProfile.STRICT_KIDS,
            container.preferences.activeProtectionProfile.value
        )
        assertTrue(container.preferences.strictEverything.value)
        assertEquals(SensitivityLevel.STRICT, container.preferences.sensitivity.value)
    }

    @Test
    fun `status screen renders bypass shield diagnostics and live dns activity preview`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AegisContainer(context)

        composeRule.setContent {
            AegisShieldTheme {
                LiveProtectionScreen(
                    protectionRepo = container.protectionRepository,
                    preferences = container.preferences,
                    blockedDnsLogRepo = container.blockedDnsLogRepository
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("status_bypass_audit_card"))
        composeRule.onNodeWithTag("status_bypass_audit_card").assertIsDisplayed()
        composeRule.onNodeWithText("Bypass Shield Diagnostics").assertIsDisplayed()

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("status_live_activity_card"))
        composeRule.onNodeWithTag("status_live_activity_card").assertIsDisplayed()
        composeRule.onNodeWithTag("status_open_activity_feed_button").assertIsDisplayed()
    }
}

