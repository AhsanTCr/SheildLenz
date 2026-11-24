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
import com.example.core.models.SafeSearchPack
import com.example.data.preferences.AegisPreferences
import com.example.di.AegisContainer
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.focus.FocusSafetyHubScreen
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
class FocusFeaturesRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `study mode lifecycle notifications and discreet mode persist and restore accurately`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = AegisPreferences(context)
        prefs.stopStudySession()
        prefs.resetSafeSearchBlockedCount()

        prefs.setStudyDurationMinutes(45)
        prefs.setStudySoftWarningSeconds(60)
        val endMillis = prefs.startStudySession(45)
        assertTrue(endMillis > System.currentTimeMillis())
        assertTrue(prefs.isStudySessionActive())
        assertTrue(prefs.getRemainingStudyMillis() > 44 * 60_000L)

        prefs.stopStudySession()
        assertFalse(prefs.isStudySessionActive())
        assertEquals(0L, prefs.getRemainingStudyMillis())

        assertEquals(1, prefs.incrementSafeSearchBlockedCount())
        assertEquals(2, prefs.incrementSafeSearchBlockedCount())

        prefs.setNotificationsEnabled(false)
        prefs.setDiscreetBlockingScreenMode(true)
        prefs.setSmartSafeSearchEnabled(true)
        prefs.setSafeSearchPacks(setOf(SafeSearchPack.ADULT_NSFW.id, SafeSearchPack.PROXY_VPN_BYPASS.id))

        val exported = prefs.exportPolicyJson()
        prefs.setNotificationsEnabled(true)
        prefs.setDiscreetBlockingScreenMode(false)
        prefs.setSmartSafeSearchEnabled(false)

        assertTrue(prefs.restorePolicyJson(exported))
        assertFalse(prefs.notificationsEnabled.value)
        assertTrue(prefs.discreetBlockingScreenMode.value)
        assertTrue(prefs.smartSafeSearchEnabled.value)
        assertEquals(45, prefs.studyDurationMinutes.value)
        assertEquals(60, prefs.studySoftWarningSeconds.value)
        assertTrue(prefs.safeSearchPacks.value.contains(SafeSearchPack.PROXY_VPN_BYPASS.id))
    }

    @Test
    fun `home renders one-tap focus strip and navigates to focus safety hub`() {
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
        composeRule.waitForIdle()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("home_one_tap_features_strip"))
        composeRule.onNodeWithTag("home_one_tap_study").assertIsDisplayed()
        composeRule.onNodeWithTag("home_one_tap_safe_search").assertIsDisplayed()
        composeRule.onNodeWithTag("home_one_tap_budgets").assertIsDisplayed()

        composeRule.onNodeWithTag("home_one_tap_safe_search").performClick()
        composeRule.waitForIdle()
        assertTrue(container.preferences.smartSafeSearchEnabled.value)

        composeRule.onNodeWithTag("home_one_tap_budgets").performClick()
        composeRule.waitForIdle()
        assertTrue(openedFocusHub)
    }

    @Test
    fun `focus safety hub renders study timer safe search packs and daily budgets cards`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AegisContainer(context)

        composeRule.setContent {
            AegisShieldTheme {
                FocusSafetyHubScreen(
                    preferences = container.preferences,
                    onNavigateBack = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("focus_safety_hub_root").assertIsDisplayed()
        composeRule.onNodeWithTag("focus_study_card").assertIsDisplayed()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("focus_safe_search_card"))
        composeRule.onNodeWithTag("focus_safe_search_card").assertIsDisplayed()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("focus_app_budgets_card"))
        composeRule.onNodeWithTag("focus_app_budgets_card").assertIsDisplayed()
    }
}
