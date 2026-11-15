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
import androidx.compose.ui.test.performScrollToNode
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.example.core.models.SensitivityLevel
import com.example.data.repository.ParentChildRepository
import com.example.di.AegisContainer
import com.example.ui.components.CyberTopBar
import com.example.ui.components.QrCodeGenerator
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.history.SecurityHistoryScreen
import com.example.ui.navigation.AegisDestinations
import com.example.ui.navigation.AegisNavHost
import com.example.ui.parents.ParentsScreen
import com.example.ui.protection.LiveProtectionScreen
import com.example.ui.settings.SettingsScreen
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
    fun `home screen renders Status and Progress gateway buttons and triggers callbacks`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AegisContainer(context)
        var openedStatus = false
        var openedProgress = false

        composeRule.setContent {
            AegisShieldTheme {
                DashboardScreen(
                    protectionRepo = container.protectionRepository,
                    modelManager = container.modelManager,
                    preferences = container.preferences,
                    onNavigateToStatus = { openedStatus = true },
                    onNavigateToProgress = { openedProgress = true }
                )
            }
        }

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("home_open_status_button"))
        composeRule.onNodeWithTag("home_open_status_button").performClick()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("home_open_progress_button"))
        composeRule.onNodeWithTag("home_open_progress_button").performClick()
        composeRule.waitForIdle()
        assertTrue(openedStatus)
        assertTrue(openedProgress)
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

        composeRule.onNodeWithTag("security_health_gauge_card").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("PERMISSIONS").fetchSemanticsNodes().isNotEmpty())
        composeRule.onNodeWithText("Overlay").assertIsDisplayed()
        composeRule.onNodeWithText("Device Admin").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithText("AI CLASSIFICATION").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("Permission Health Audit").fetchSemanticsNodes().size)
    }

    @Test
    fun `edits screen shows AI Classification at top and expands Quiet Hours on one tap`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AegisContainer(context)

        composeRule.setContent {
            AegisShieldTheme {
                SettingsScreen(
                    preferences = container.preferences,
                    modelManager = container.modelManager,
                    protectionRepo = container.protectionRepository
                )
            }
        }

        composeRule.onNodeWithText("AI Classification").assertIsDisplayed()
        // Quiet Hours starts collapsed
        assertEquals(0, composeRule.onAllNodesWithText("Enable Scheduled Guard").fetchSemanticsNodes().size)

        // Scroll to Quiet Hours card and expand with one tap
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("scheduled_protection_card_toggle"))
        composeRule.onNodeWithTag("scheduled_protection_card_toggle").performClick()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Enable Scheduled Guard"))
        composeRule.onNodeWithText("Enable Scheduled Guard").assertIsDisplayed()
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

        // Tap As Child -> opens Child QR card + Uninstall Lock card
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("connect_as_child_button"))
        composeRule.onNodeWithTag("connect_as_child_button").performClick()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("child_pairing_qr_card"))
        composeRule.onNodeWithTag("child_pairing_qr_matrix").assertIsDisplayed()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("child_connection_status_card"))
        composeRule.onNodeWithText("Uninstall Blocked (Child Mode)").assertIsDisplayed()
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
    fun `bottom navigation shows Home Edits Parents About and excludes Status and Progress tabs`() {
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
        composeRule.onNodeWithTag("nav_tab_${AegisDestinations.EDITS}").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_tab_${AegisDestinations.PARENTS}").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_tab_${AegisDestinations.ABOUT}").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_tab_${AegisDestinations.STATUS}").assertDoesNotExist()
        composeRule.onNodeWithTag("nav_tab_${AegisDestinations.PROGRESS}").assertDoesNotExist()

        composeRule.onNodeWithTag("nav_tab_${AegisDestinations.PARENTS}").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("parents_screen_root").assertIsDisplayed()
        assertTrue(SensitivityLevel.BALANCED.displayName.isNotBlank())
    }
}
