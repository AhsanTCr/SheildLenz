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
import com.example.ui.about.AboutScreen
import com.example.ui.auth.GuardianGateScreen
import com.example.ui.parents.ParentsScreen
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

        val challenge = prefs.getRecoveryChallengeCode()
        assertTrue(challenge.startsWith("SLZ-"))
        assertFalse(prefs.verifyRecoveryToken("WRONGCODE"))

        val validToken = AegisPreferences.computeRecoveryTokenForChallenge(challenge)
        assertTrue(prefs.verifyRecoveryToken(validToken))
        assertTrue(prefs.resetPinWithRecoveryToken(validToken, "4321"))
        assertFalse(prefs.isLockdownActive())
        assertTrue(prefs.verifyGuardianPin("4321"))
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
    fun `about screen displays open source gpl v3 card`() {
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
    }

    @Test
    fun `parents screen renders scan qr button in parent role`() {
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
    fun `settings screen renders safe dns bedtime language and device owner wizard trigger`() {
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

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("safe_dns_blocklist_card"))
        composeRule.onNodeWithTag("safe_dns_blocklist_card").assertIsDisplayed()

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("bedtime_budgets_card"))
        composeRule.onNodeWithTag("bedtime_budgets_card").assertIsDisplayed()

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("language_selector_card"))
        composeRule.onNodeWithTag("language_selector_card").assertIsDisplayed()

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("resistance_accordion_toggle"))
        composeRule.onNodeWithTag("resistance_accordion_toggle").performClick()
        composeRule.waitForIdle()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("device_owner_wizard_button"))
        composeRule.onNodeWithTag("device_owner_wizard_button").performScrollTo()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("device_owner_wizard_button").assertIsDisplayed()
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
