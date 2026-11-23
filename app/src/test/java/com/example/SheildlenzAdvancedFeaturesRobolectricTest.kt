package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.core.models.SensitivityLevel
import com.example.core.models.ThreatCategory
import com.example.data.preferences.AegisPreferences
import com.example.detection.classifier.ClassificationResult
import com.example.detection.decision.ThreatDecisionEngine
import com.example.di.AegisContainer
import com.example.monitoring.accessibility.AegisAccessibilityService
import com.example.services.dns.DnsPacketHelper
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import com.example.data.preferences.RecoveryQrMode
import com.example.data.preferences.RecoveryScanResult
import com.example.data.preferences.SafeDnsProvider
import com.example.ui.about.AboutScreen
import com.example.ui.auth.GuardianGateScreen
import com.example.ui.components.QrCodeGenerator
import com.example.ui.components.rememberMasterKeyGate
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.parents.ParentsScreen
import com.example.ui.protection.LiveProtectionScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.WellbeingHubScreen
import com.example.ui.theme.AegisShieldTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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
class SheildlenzAdvancedFeaturesRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `configurable warning thresholds issue warnings before triggering device lockdown`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = AegisPreferences(context)
        prefs.resetWarningCycle()

        prefs.setWarningThresholdCount(2)
        val w1 = prefs.recordThreatAndCheckLockdown()
        assertFalse(w1.shouldLockdown)
        assertEquals(1, w1.warningNumber)

        val w2 = prefs.recordThreatAndCheckLockdown()
        assertFalse(w2.shouldLockdown)
        assertEquals(2, w2.warningNumber)

        val w3 = prefs.recordThreatAndCheckLockdown()
        assertTrue(w3.shouldLockdown)

        prefs.setWarningThresholdCount(0)
        val immediate = prefs.recordThreatAndCheckLockdown()
        assertTrue(immediate.shouldLockdown)

        // Warning Only (Don't Lockout) never escalates to full device lockout
        prefs.resetWarningCycle()
        prefs.setWarningOnlyNoLockout(true)
        assertTrue(prefs.warningOnlyNoLockout.value)
        repeat(5) { idx ->
            val res = prefs.recordThreatAndCheckLockdown()
            assertFalse("Threat #${idx + 1} must not trigger lockout in Warn-Only mode", res.shouldLockdown)
            assertTrue(res.isWarningOnly)
            assertEquals(idx + 1, res.warningNumber)
        }
        val exportedJson = prefs.exportPolicyJson()
        prefs.setWarningOnlyNoLockout(false)
        assertTrue(prefs.restorePolicyJson(exportedJson))
        assertTrue(prefs.warningOnlyNoLockout.value)
    }

    @Test
    fun `guardian email and offline challenge recovery token unlock and reset pin`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = AegisPreferences(context)
        prefs.setGuardianEmail("guardian@family.example")
        prefs.setGuardianPin("9876")
        prefs.engageFullDeviceLockdown("Browser", "com.android.browser", "Explicit", "test")

        assertEquals("guardian@family.example", prefs.guardianEmail.value)
        assertTrue(prefs.isLockdownActive())

        val challenge1 = prefs.getRecoveryChallengeCode()
        assertTrue(challenge1.startsWith("SLZ-"))
        assertFalse(prefs.verifyRecoveryToken("WRONGCODE"))

        // Second lockdown / block creates a fresh Challenge ID automatically
        prefs.engageFullDeviceLockdown("Video", "org.videolan.vlc", "Explicit", "second block")
        val challenge2 = prefs.getRecoveryChallengeCode()
        assertTrue(challenge2.startsWith("SLZ-"))
        assertNotEquals(challenge1, challenge2)

        val validToken = AegisPreferences.computeRecoveryTokenForChallenge(challenge2)
        assertTrue(prefs.verifyRecoveryToken(validToken))
        assertTrue(prefs.resetPinWithRecoveryToken(validToken, "4321"))
        assertFalse(prefs.isLockdownActive())
        assertTrue(prefs.verifyGuardianPin("4321"))

        // Challenge ID rotates again after successful recovery so old code cannot be reused
        val challenge3 = prefs.getRecoveryChallengeCode()
        assertNotEquals(challenge2, challenge3)
        assertFalse(prefs.verifyRecoveryToken(validToken))
    }

    @Test
    fun `dns packet helper parses ipv4 udp query and builds sinkhole response`() {
        val rawQueryPacket = buildSampleIpv4UdpDnsQueryPacket("pornhub.com")
        val parsed = DnsPacketHelper.parseIpv4UdpDnsPacket(rawQueryPacket, rawQueryPacket.size)
        assertNotNull(parsed)
        assertEquals("pornhub.com", parsed!!.domain)
        assertEquals(DnsPacketHelper.DNS_TYPE_A, parsed.queryType)

        assertNotNull(
            DnsPacketHelper.matchesBlockedDomain(
                rawDomain = "sub.bet365.com",
                blockExplicitAdult = true,
                blockGambling = true
            )
        )
        assertNotNull(
            DnsPacketHelper.matchesBlockedDomain(
                rawDomain = "blocked-site.org",
                customDomains = setOf("blocked-site.org")
            )
        )
        assertNull(
            DnsPacketHelper.matchesBlockedDomain(
                rawDomain = "wikipedia.org",
                customDomains = setOf("blocked-site.org"),
                customKeywords = setOf("casino")
            )
        )

        val blockedDns = DnsPacketHelper.buildBlockedDnsResponsePayload(parsed.dnsPayload)
        assertNotNull(blockedDns)
        val responseIpPacket = DnsPacketHelper.buildIpv4UdpResponsePacket(parsed, blockedDns!!)
        assertTrue(responseIpPacket.size > 28)
        // QR response bit = 1 in DNS flags byte 2 of DNS payload (offset 20 + 8 + 2 = 30)
        assertTrue((responseIpPacket[30].toInt() and 0x80) != 0)
        // ANCOUNT = 1 (offset 20 + 8 + 7 = 35)
        assertEquals(1, responseIpPacket[35].toInt() and 0xFF)
    }

    @Test
    fun `accessibility url detector respects custom blocklists and gambling toggle`() {
        val customDomainHit = AegisAccessibilityService.detectExplicitBrowserUrl(
            rawText = "https://sub.custom-blocked.net/feed",
            customDomains = setOf("custom-blocked.net")
        )
        assertNotNull(customDomainHit)

        val customKeywordHit = AegisAccessibilityService.detectExplicitBrowserUrl(
            rawText = "searching for badtoken online",
            customKeywords = setOf("badtoken")
        )
        assertNotNull(customKeywordHit)

        val gamblingBlocked = AegisAccessibilityService.detectExplicitBrowserUrl(
            rawText = "https://www.bet365.com/en/",
            blockGamblingKeywords = true
        )
        assertNotNull(gamblingBlocked)

        val gamblingAllowed = AegisAccessibilityService.detectExplicitBrowserUrl(
            rawText = "https://www.bet365.com/en/",
            blockGamblingKeywords = false
        )
        assertNull(gamblingAllowed)
    }

    @Test
    fun `app time budgets track daily seconds grant extensions and check bedtime windows`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = AegisPreferences(context)
        val pkg = "com.video.sample"
        val dateKey = "2099-05-01"

        prefs.setAppTimeBudget(pkg, 2) // 2 minutes = 120 seconds
        prefs.recordAppUsageSeconds(pkg, deltaSeconds = 119, dateKey = dateKey)
        assertFalse(prefs.isAppTimeBudgetExceeded(pkg, dateKey = dateKey))

        prefs.recordAppUsageSeconds(pkg, deltaSeconds = 2, dateKey = dateKey)
        assertTrue(prefs.isAppTimeBudgetExceeded(pkg, dateKey = dateKey))

        prefs.grantTemporaryBudgetExtension(pkg, extraMinutes = 15, dateKey = dateKey)
        assertFalse(prefs.isAppTimeBudgetExceeded(pkg, dateKey = dateKey))

        // Overnight bedtime window 22:00 (1320) -> 06:00 (360)
        assertTrue(AegisPreferences.isMinuteWithinWindow(23 * 60 + 30, 22 * 60, 6 * 60))
        assertTrue(AegisPreferences.isMinuteWithinWindow(5 * 60 + 30, 22 * 60, 6 * 60))
        assertFalse(AegisPreferences.isMinuteWithinWindow(12 * 60, 22 * 60, 6 * 60))
    }

    @Test
    fun `threat decision engine honors independent category toggles`() {
        val engine = ThreatDecisionEngine()

        val explicitResult = ClassificationResult(
            safeScore = 0.03f,
            suggestiveScore = 0.05f,
            explicitScore = 0.92f,
            topCategory = ThreatCategory.EXPLICIT_ADULT,
            confidence = 0.92f,
            isModelLoaded = true,
            latencyMs = 15L,
            diagnosticInfo = "Test Explicit"
        )
        assertTrue(
            engine.evaluate(
                result = explicitResult,
                sensitivity = SensitivityLevel.BALANCED,
                isStrictEverything = false,
                blockExplicitAdult = true
            ).shouldBlock
        )
        assertFalse(
            engine.evaluate(
                result = explicitResult,
                sensitivity = SensitivityLevel.BALANCED,
                isStrictEverything = false,
                blockExplicitAdult = false
            ).shouldBlock
        )

        val suggestiveResult = ClassificationResult(
            safeScore = 0.05f,
            suggestiveScore = 0.90f,
            explicitScore = 0.05f,
            topCategory = ThreatCategory.SUGGESTIVE_CONTENT,
            confidence = 0.90f,
            isModelLoaded = true,
            latencyMs = 15L,
            diagnosticInfo = "Test Suggestive"
        )
        assertFalse(
            engine.evaluate(
                result = suggestiveResult,
                sensitivity = SensitivityLevel.BALANCED,
                isStrictEverything = false,
                blockSuggestiveContent = false
            ).shouldBlock
        )

        val graphicResult = ClassificationResult(
            safeScore = 0.05f,
            suggestiveScore = 0.05f,
            explicitScore = 0.02f,
            topCategory = ThreatCategory.SENSITIVE_GRAPHIC,
            confidence = 0.88f,
            isModelLoaded = true,
            latencyMs = 15L,
            diagnosticInfo = "Test Graphic"
        )
        assertFalse(
            engine.evaluate(
                result = graphicResult,
                sensitivity = SensitivityLevel.BALANCED,
                isStrictEverything = false,
                blockSensitiveGraphic = false
            ).shouldBlock
        )
    }

    @Test
    fun `guardian gate shows recovery email input and about screen shows gpl v3 card`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AegisContainer(context)
        container.preferences.removeGuardianPin()

        composeRule.setContent {
            AegisShieldTheme {
                GuardianGateScreen(
                    preferences = container.preferences,
                    protectionRepo = container.protectionRepository,
                    onUnlocked = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("guardian_gate_email_input").performScrollTo()
        composeRule.onNodeWithTag("guardian_gate_email_input").assertIsDisplayed()
    }

    @Test
    fun `guardian gate shows dominant challenge id and forgot pin recovery when pin is set`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AegisContainer(context)
        container.preferences.setGuardianPin("1234")

        composeRule.setContent {
            AegisShieldTheme {
                GuardianGateScreen(
                    preferences = container.preferences,
                    protectionRepo = container.protectionRepository,
                    onUnlocked = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("guardian_gate_challenge_badge").performScrollTo()
        composeRule.onNodeWithTag("guardian_gate_challenge_badge").assertIsDisplayed()
        composeRule.onNodeWithTag("guardian_gate_forgot_pin_button").performScrollTo()
        composeRule.onNodeWithTag("guardian_gate_forgot_pin_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("guardian_gate_recovery_code_input").performScrollTo()
        composeRule.onNodeWithTag("guardian_gate_recovery_code_input").assertIsDisplayed()
    }

    @Test
    fun `about screen displays open source gpl v3 card and new feature qas`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AegisContainer(context)

        composeRule.setContent {
            AegisShieldTheme {
                AboutScreen(preferences = container.preferences)
            }
        }
        composeRule.waitForIdle()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("about_gpl_license_card"))
        composeRule.onNodeWithTag("about_gpl_license_card").performScrollTo()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("about_gpl_license_card").assertIsDisplayed()

        composeRule.onNode(hasScrollAction()).performScrollToNode(
            androidx.compose.ui.test.hasText("How does Guardian Recovery work if I forget my PIN?")
        )
        composeRule.onNodeWithText("How does Guardian Recovery work if I forget my PIN?").assertIsDisplayed()
    }

    @Test
    fun `parents screen renders qr recovery scanner and scan child qr button`() {
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

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("guardian_qr_recovery_card"))
        composeRule.onNodeWithTag("guardian_qr_recovery_card").assertIsDisplayed()
        composeRule.onNodeWithTag("scan_recovery_qr_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Sign in with Google above using your registered Guardian email to authorize QR recovery.").assertIsDisplayed()

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("connect_as_parent_button"))
        composeRule.onNodeWithTag("connect_as_parent_button").performScrollTo()
        composeRule.onNodeWithTag("connect_as_parent_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("scan_child_qr_button"))
        composeRule.onNodeWithTag("scan_child_qr_button").performScrollTo()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("scan_child_qr_button").assertIsDisplayed()
    }

    @Test
    fun `settings screen renders separate cards for all new features`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AegisContainer(context)
        container.preferences.setGuardianPin("1234")
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

        listOf(
            "ai_model_pill",
            "thresholds_accordion",
            "ai_categories_card",
            "safe_dns_blocklist_card",
            "warning_thresholds_card",
            "power_mode_widget",
            "scheduled_protection_card",
            "bedtime_budgets_card",
            "whitelisted_apps_card",
            "guardian_pin_change_button",
            "guardian_recovery_card",
            "resistance_accordion",
            "device_owner_card",
            "touch_sounds_card",
            "policy_backup_restore_card",
            "language_selector_card"
        ).forEach { tag ->
            composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag(tag))
            composeRule.onNodeWithTag(tag).performScrollTo()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag(tag).assertIsDisplayed()
        }

        // Verify tapping Warnings pill opens popup dialog with Warn Only (Don't Lockout) switch
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("warning_thresholds_card"))
        composeRule.onNodeWithTag("warning_thresholds_card").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("warning_thresholds_card_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("warning_only_no_lockout_switch").performClick()
        composeRule.waitForIdle()
        assertTrue(container.preferences.warningOnlyNoLockout.value)
        composeRule.onNodeWithText("DONE").performClick()
    }

    @Test
    fun `status renders wellbeing gateway button and edits locks behind guardian password`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AegisContainer(context)
        var openedWellbeing = false

        composeRule.setContent {
            AegisShieldTheme {
                LiveProtectionScreen(
                    protectionRepo = container.protectionRepository,
                    preferences = container.preferences,
                    onNavigateToWellbeing = { openedWellbeing = true }
                )
            }
        }
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("status_parent_connection_card"))
        composeRule.onNodeWithTag("status_parent_connection_card").assertIsDisplayed()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("status_edits_overview_card"))
        composeRule.onNodeWithTag("status_edits_overview_card").assertIsDisplayed()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("status_open_wellbeing_button"))
        composeRule.onNodeWithTag("status_open_wellbeing_button").performClick()
        composeRule.waitForIdle()
        assertTrue(openedWellbeing)

        // Verify Edits password lock gate protects SettingsScreen in Child mode
        container.preferences.setGuardianPin("2580")
        container.preferences.lockGuardianSession()
        container.preferences.setParentalRole("child")
        var jumpedToStatus = false
        composeRule.setContent {
            AegisShieldTheme {
                SettingsScreen(
                    preferences = container.preferences,
                    modelManager = container.modelManager,
                    protectionRepo = container.protectionRepository,
                    onNavigateToStatus = { jumpedToStatus = true }
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("edits_lock_gate_card").assertIsDisplayed()
        composeRule.onNodeWithTag("edits_child_parent_notice").assertIsDisplayed()
        composeRule.onNodeWithTag("edits_view_status_button").performScrollTo()
        composeRule.onNodeWithTag("edits_view_status_button").performClick()
        composeRule.waitForIdle()
        assertTrue(jumpedToStatus)

        composeRule.onNodeWithTag("edits_unlock_pin_input").performScrollTo()
        composeRule.onNodeWithTag("edits_unlock_pin_input").performTextInput("2580")
        composeRule.onNodeWithTag("edits_unlock_button").performClick()
        composeRule.waitForIdle()
        assertTrue(container.preferences.isGuardianSessionUnlocked.value)
        composeRule.onNodeWithTag("edits_unlocked_banner").assertIsDisplayed()
    }

    @Test
    fun `wellbeing hub screen shows dedicated quick buttons`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AegisContainer(context)

        composeRule.setContent {
            AegisShieldTheme {
                WellbeingHubScreen(
                    preferences = container.preferences,
                    onNavigateBack = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("hub_safe_dns_button").assertIsDisplayed()
        composeRule.onNodeWithTag("hub_bedtime_button").assertIsDisplayed()
        composeRule.onNodeWithTag("hub_warnings_button").assertIsDisplayed()
        composeRule.onNodeWithTag("hub_device_owner_button").assertIsDisplayed()
    }

    @Test
    fun `qr recovery distinguishes timer-end vs pin-reset and enforces matching guardian email`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = AegisPreferences(context)
        prefs.setGuardianEmail("parent.guard@example.com")
        prefs.setGuardianPin("2468")
        prefs.lockGuardianSession()
        prefs.engageFullDeviceLockdown("Chrome", "com.android.chrome", "Explicit", "test")

        val timerQrUri = prefs.buildRecoveryQrUri(RecoveryQrMode.TIMER_END)
        val pinQrUri = prefs.buildRecoveryQrUri(RecoveryQrMode.PIN_RESET)

        // Requires Google sign-in on scanning parent phone
        assertTrue(
            AegisPreferences.resolveRecoveryTokenForGuardianScan(timerQrUri, null)
                is RecoveryScanResult.RequiresGoogleSignIn
        )

        // Wrong email is rejected
        val mismatch = AegisPreferences.resolveRecoveryTokenForGuardianScan(timerQrUri, "intruder@example.com")
        assertTrue(mismatch is RecoveryScanResult.EmailMismatch)

        // Matching email returns one-time timer code (case/whitespace normalized)
        val timerResult = AegisPreferences.resolveRecoveryTokenForGuardianScan(
            timerQrUri,
            " Parent.Guard@example.com "
        ) as RecoveryScanResult.Success
        assertEquals(RecoveryQrMode.TIMER_END, timerResult.mode)

        // Timer token cannot reset PIN, and PIN token cannot end timer cleanly via wrong verifier
        assertFalse(prefs.resetPinWithRecoveryToken(timerResult.oneTimeCode, "9999"))
        assertTrue(prefs.endLockdownTimerWithToken(timerResult.oneTimeCode))

        // Timer ended, Shield remains ON, original PIN unchanged, Guardian session still locked, Challenge rotated
        assertFalse(prefs.isLockdownActive())
        assertTrue(prefs.isShieldDesiredActive.value)
        assertTrue(prefs.verifyGuardianPin("2468"))
        assertFalse(prefs.isGuardianSessionUnlocked.value)
        assertFalse(prefs.endLockdownTimerWithToken(timerResult.oneTimeCode))

        // Older pinQrUri still checks out conceptually for matching email on its challenge
        val pinResult = AegisPreferences.resolveRecoveryTokenForGuardianScan(
            pinQrUri,
            "parent.guard@example.com"
        ) as RecoveryScanResult.Success
        assertEquals(RecoveryQrMode.PIN_RESET, pinResult.mode)
        assertNotEquals(timerResult.oneTimeCode, pinResult.oneTimeCode)

        // Bitmap QR encoder produces crisp valid square bitmap
        val qrBitmap = QrCodeGenerator.encodeBitmap(timerQrUri, sizePx = 280)
        assertEquals(qrBitmap.width, qrBitmap.height)
        assertTrue(qrBitmap.width >= 100)
    }

    @Test
    fun `master key gate supports inline forgot pin qr reset`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = AegisPreferences(context)
        prefs.setGuardianEmail("guardian@home.org")
        prefs.setGuardianPin("1122")
        prefs.lockGuardianSession()

        var callbackInvoked = false
        composeRule.setContent {
            AegisShieldTheme {
                val gate = rememberMasterKeyGate(prefs)
                androidx.compose.material3.Button(
                    onClick = {
                        gate.request("turn off shield") { callbackInvoked = true }
                    },
                    modifier = androidx.compose.ui.Modifier.testTag("test_trigger_gate_btn")
                ) {
                    androidx.compose.material3.Text("Trigger")
                }
            }
        }

        composeRule.onNodeWithTag("test_trigger_gate_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("master_key_forgot_pin_button").performClick()
        composeRule.waitForIdle()

        val qrUri = prefs.buildRecoveryQrUri(RecoveryQrMode.PIN_RESET)
        val scanResult = AegisPreferences.resolveRecoveryTokenForGuardianScan(
            qrUri,
            "guardian@home.org"
        ) as RecoveryScanResult.Success

        composeRule.onNodeWithTag("master_key_forgot_qr_matrix").assertIsDisplayed()
        composeRule.onNodeWithTag("master_key_recovery_code_input").performTextInput(scanResult.oneTimeCode)
        composeRule.onNodeWithTag("master_key_new_pin_input").performTextInput("7788")
        composeRule.onNodeWithTag("master_key_prompt_confirm").performClick()
        composeRule.waitForIdle()

        assertTrue(callbackInvoked)
        assertTrue(prefs.verifyGuardianPin("7788"))
        assertFalse(prefs.verifyGuardianPin("1122"))
    }

    @Test
    fun `onboarding step 1 requires master key and valid guardian email before proceeding`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = AegisPreferences(context)
        prefs.removeGuardianPin()
        prefs.setGuardianEmail("")

        composeRule.setContent {
            AegisShieldTheme {
                OnboardingScreen(preferences = prefs, onComplete = {})
            }
        }

        composeRule.onNodeWithTag("onboarding_get_started_button").performScrollTo()
        composeRule.onNodeWithTag("onboarding_get_started_button").performClick()
        composeRule.waitForIdle()

        // Trying to continue with empty fields stays on Step 1
        composeRule.onNodeWithTag("onboarding_guardian_continue_button").performScrollTo()
        composeRule.onNodeWithTag("onboarding_guardian_continue_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("onboarding_pin_input").performScrollTo()
        composeRule.onNodeWithTag("onboarding_pin_input").assertIsDisplayed()

        // Fill valid PIN + matching confirm + email
        composeRule.onNodeWithTag("onboarding_pin_input").performTextInput("5544")
        composeRule.onNodeWithTag("onboarding_confirm_pin_input").performScrollTo()
        composeRule.onNodeWithTag("onboarding_confirm_pin_input").performTextInput("5544")
        composeRule.onNodeWithTag("onboarding_email_input").performScrollTo()
        composeRule.onNodeWithTag("onboarding_email_input").performTextInput("mom.guard@example.com")
        composeRule.onNodeWithTag("onboarding_guardian_continue_button").performScrollTo()
        composeRule.onNodeWithTag("onboarding_guardian_continue_button").performClick()
        composeRule.waitForIdle()

        assertTrue(prefs.verifyGuardianPin("5544"))
        assertEquals("mom.guard@example.com", prefs.guardianEmail.value)
        composeRule.onNodeWithText("System Overlay").assertIsDisplayed()
    }

    @Test
    fun `strict anti adult dns catalog and packet filter block adult mirrors tlds and doh bypass`() {
        assertEquals("185.228.168.10", SafeDnsProvider.CLEANBROWSING_ADULT.primaryIp)
        assertEquals("208.67.222.123", SafeDnsProvider.OPENDNS_FAMILY.primaryIp)
        assertEquals("76.76.2.4", SafeDnsProvider.CONTROLD_ADULT.primaryIp)

        assertNotNull(DnsPacketHelper.matchesBlockedDomain("chaturbate.com", blockExplicitAdult = true))
        assertNotNull(DnsPacketHelper.matchesBlockedDomain("live.myfreecams.cam", blockExplicitAdult = true))
        assertNotNull(DnsPacketHelper.matchesBlockedDomain("model.sexy", blockExplicitAdult = true))
        assertNotNull(DnsPacketHelper.matchesBlockedDomain("free-pornhub-hd.net", blockExplicitAdult = true))
        assertNotNull(DnsPacketHelper.matchesBlockedDomain("dns.google", blockExplicitAdult = true))
        assertNotNull(DnsPacketHelper.matchesBlockedDomain("cloudflare-dns.com", blockExplicitAdult = true))
        assertNotNull(DnsPacketHelper.matchesBlockedDomain("mozilla.cloudflare-dns.com", blockExplicitAdult = true))

        assertNull(DnsPacketHelper.matchesBlockedDomain("wikipedia.org", blockExplicitAdult = true))
        assertNull(DnsPacketHelper.matchesBlockedDomain("scunthorpe-united.co.uk", blockExplicitAdult = true))
    }

    @Test
    fun `3d orbital projection computes depth z and perspective scaling cleanly`() {
        val center = androidx.compose.ui.geometry.Offset(100f, 100f)
        val front = com.example.ui.components.projectOrbitPoint3D(
            center = center,
            radiusX = 80f,
            radiusY = 48f,
            zAmplitude = 60f,
            angleDeg = 90f,
            tiltDeg = 16f,
            cameraDistance = 240f
        )
        val back = com.example.ui.components.projectOrbitPoint3D(
            center = center,
            radiusX = 80f,
            radiusY = 48f,
            zAmplitude = 60f,
            angleDeg = 270f,
            tiltDeg = 16f,
            cameraDistance = 240f
        )

        assertTrue(front.zNormalized > 0.9f)
        assertTrue(back.zNormalized < -0.9f)
        assertTrue(front.perspectiveScale > 1.0f)
        assertTrue(back.perspectiveScale < 1.0f)
        assertTrue(front.perspectiveScale > back.perspectiveScale)
    }

    @Test
    fun `about screen hero branding update card and expandable guide items render cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AegisContainer(context)

        composeRule.setContent {
            AegisShieldTheme {
                AboutScreen(preferences = container.preferences)
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("about_screen_root").assertIsDisplayed()
        composeRule.onNodeWithTag("built_by_ahsantcr_link").performScrollTo()
        composeRule.onNodeWithTag("built_by_ahsantcr_link").assertIsDisplayed()

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("app_update_checker_card"))
        composeRule.onNodeWithTag("app_update_checker_card").assertIsDisplayed()

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("about_faq_item_2"))
        composeRule.onNodeWithTag("about_faq_item_2").performScrollTo()
        composeRule.onNodeWithTag("about_faq_item_2").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("How does Strict Safe DNS block adult websites across apps?").assertIsDisplayed()
    }

    @Test
    fun `update checker extracts release apk urls reliably and never launches browser on update tap`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bodyOnlyRelease = org.json.JSONObject().apply {
            put("tag_name", "v99.0")
            put("html_url", "https://github.com/Ahsanpcin/Sheild-Lenz/releases/tag/v99.0")
            put("body", "Download directly: https://github.com/Ahsanpcin/Sheild-Lenz/releases/download/v99.0/Sheildlenz-release.apk")
            put("assets", org.json.JSONArray())
        }
        val parsedBody = com.example.core.update.UpdateChecker.parseReleaseJson(bodyOnlyRelease, "15.57")
        assertTrue(parsedBody.isUpdateAvailable)
        assertEquals(
            "https://github.com/Ahsanpcin/Sheild-Lenz/releases/download/v99.0/Sheildlenz-release.apk",
            parsedBody.apkDownloadUrl
        )

        val multiAssetRelease = org.json.JSONObject().apply {
            put("tag_name", "v99.1")
            put("assets", org.json.JSONArray().apply {
                put(org.json.JSONObject().apply {
                    put("name", "app-debug.apk")
                    put("browser_download_url", "https://github.com/Ahsanpcin/Sheild-Lenz/releases/download/v99.1/app-debug.apk")
                    put("size", 1000L)
                })
                put(org.json.JSONObject().apply {
                    put("name", "Sheildlenz-v99.1-release.apk")
                    put("browser_download_url", "https://github.com/Ahsanpcin/Sheild-Lenz/releases/download/v99.1/Sheildlenz-v99.1-release.apk")
                    put("size", 2000L)
                })
            })
        }
        val parsedMulti = com.example.core.update.UpdateChecker.parseReleaseJson(multiAssetRelease, "15.57")
        assertEquals(
            "https://github.com/Ahsanpcin/Sheild-Lenz/releases/download/v99.1/Sheildlenz-v99.1-release.apk",
            parsedMulti.apkDownloadUrl
        )

        // Ensure triggerUserUpdateAction does not fire a browser ACTION_VIEW intent to GitHub releases
        com.example.core.update.UpdateChecker.cancelApkDownload(context)
        com.example.core.update.UpdateChecker.triggerUserUpdateAction(context)
        val shadowApp = org.robolectric.Shadows.shadowOf(context as android.app.Application)
        val started = shadowApp.nextStartedActivity
        assertNull("Update action must stay inside app instead of opening browser", started)
    }

    private fun buildSampleIpv4UdpDnsQueryPacket(domain: String): ByteArray {
        val labels = domain.split(".")
        val qnameBytes = mutableListOf<Byte>()
        labels.forEach { label ->
            qnameBytes.add(label.length.toByte())
            label.forEach { ch -> qnameBytes.add(ch.code.toByte()) }
        }
        qnameBytes.add(0x00)

        val dnsLen = 12 + qnameBytes.size + 4
        val udpLen = 8 + dnsLen
        val totalLen = 20 + udpLen
        val pkt = ByteArray(totalLen)

        // IPv4 Header
        pkt[0] = 0x45
        pkt[2] = ((totalLen ushr 8) and 0xFF).toByte()
        pkt[3] = (totalLen and 0xFF).toByte()
        pkt[8] = 64
        pkt[9] = 17 // UDP
        // src 10.111.222.1 -> dst 10.111.222.2
        pkt[12] = 10; pkt[13] = 111; pkt[14] = 222.toByte(); pkt[15] = 1
        pkt[16] = 10; pkt[17] = 111; pkt[18] = 222.toByte(); pkt[19] = 2

        // UDP Header srcPort=54321 dstPort=53
        pkt[20] = 0xD4.toByte(); pkt[21] = 0x31
        pkt[22] = 0x00; pkt[23] = 53
        pkt[24] = ((udpLen ushr 8) and 0xFF).toByte()
        pkt[25] = (udpLen and 0xFF).toByte()

        // DNS Header tx=0x1234, QDCOUNT=1
        pkt[28] = 0x12; pkt[29] = 0x34
        pkt[32] = 0x00; pkt[33] = 0x01
        var pos = 40
        qnameBytes.forEach { b -> pkt[pos++] = b }
        // QTYPE=A (1), QCLASS=IN (1)
        pkt[pos++] = 0x00; pkt[pos++] = 0x01
        pkt[pos++] = 0x00; pkt[pos] = 0x01
        return pkt
    }
}
