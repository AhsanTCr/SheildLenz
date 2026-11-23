package com.example

import com.example.core.models.SafeSearchPack
import com.example.data.repository.RemotePolicySettings
import com.example.monitoring.accessibility.AegisAccessibilityService
import com.example.services.dns.DnsPacketHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeSearchAndFocusPolicyUnitTest {

    @Test
    fun safeSearchPackDetectMatchInTextRespectsActivePacks() {
        val activePacks = setOf(SafeSearchPack.PROXY_VPN_BYPASS.id, SafeSearchPack.SHORTS_DISTRACTION.id)

        val proxyHit = SafeSearchPack.detectMatchInText(
            rawText = "how to use croxyproxy on school wifi",
            activePackIds = activePacks
        )
        assertNotNull(proxyHit)
        assertEquals(SafeSearchPack.PROXY_VPN_BYPASS, proxyHit!!.first)
        assertTrue(proxyHit.second.startsWith("Smart Safe Search"))

        val shortsHit = SafeSearchPack.detectMatchInText(
            rawText = "https://www.tiktok.com/@trending",
            activePackIds = activePacks
        )
        assertNotNull(shortsHit)
        assertEquals(SafeSearchPack.SHORTS_DISTRACTION, shortsHit!!.first)

        // Violence pack is NOT active in activePacks, so it should not match here
        val inactiveViolence = SafeSearchPack.detectMatchInText(
            rawText = "searching liveleak",
            activePackIds = activePacks
        )
        assertNull(inactiveViolence)

        // Enabling violence pack catches it cleanly
        val activeViolence = SafeSearchPack.detectMatchInText(
            rawText = "searching liveleak",
            activePackIds = activePacks + SafeSearchPack.VIOLENCE_SELFHARM.id
        )
        assertNotNull(activeViolence)
        assertEquals(SafeSearchPack.VIOLENCE_SELFHARM, activeViolence!!.first)
    }

    @Test
    fun safeSearchPackMatchesDomainAndSubdomainInActivePacks() {
        val packs = setOf(SafeSearchPack.PROXY_VPN_BYPASS.id, SafeSearchPack.GAMBLING_BETTING.id)

        assertEquals(
            SafeSearchPack.PROXY_VPN_BYPASS,
            SafeSearchPack.matchesDomainInActivePacks("www.croxyproxy.com", packs)
        )
        assertEquals(
            SafeSearchPack.PROXY_VPN_BYPASS,
            SafeSearchPack.matchesDomainInActivePacks("mirror1.blockaway.net", packs)
        )
        assertEquals(
            SafeSearchPack.GAMBLING_BETTING,
            SafeSearchPack.matchesDomainInActivePacks("sports.bet365.com", packs)
        )
        assertNull(SafeSearchPack.matchesDomainInActivePacks("khanacademy.org", packs))
        assertNull(SafeSearchPack.matchesDomainInActivePacks("tiktok.com", packs))
    }

    @Test
    fun browserUrlDetectorHonorsSmartSafeSearchPacksOnlyWhenEnabled() {
        val packs = setOf(SafeSearchPack.PROXY_VPN_BYPASS.id, SafeSearchPack.SHORTS_DISTRACTION.id)

        // When Smart Safe Search is OFF, non-explicit proxy/shorts terms are allowed
        val offResult = AegisAccessibilityService.detectExplicitBrowserUrl(
            rawText = "https://croxyproxy.com/",
            blockGamblingKeywords = false,
            smartSafeSearchEnabled = false,
            safeSearchPacks = packs
        )
        assertNull(offResult)

        // When Smart Safe Search is ON, active pack domains and queries are blocked
        val onProxy = AegisAccessibilityService.detectExplicitBrowserUrl(
            rawText = "https://croxyproxy.com/",
            blockGamblingKeywords = false,
            smartSafeSearchEnabled = true,
            safeSearchPacks = packs
        )
        assertNotNull(onProxy)
        assertTrue(onProxy!!.startsWith("Smart Safe Search"))

        val onQuery = AegisAccessibilityService.detectExplicitBrowserUrl(
            rawText = "youtube shorts compilation",
            blockGamblingKeywords = false,
            smartSafeSearchEnabled = true,
            safeSearchPacks = packs
        )
        assertNotNull(onQuery)
        assertTrue(onQuery!!.contains("Shorts"))

        // Safe educational searches remain unaffected
        val safeEdu = AegisAccessibilityService.detectExplicitBrowserUrl(
            rawText = "https://www.khanacademy.org/math/algebra",
            blockGamblingKeywords = true,
            smartSafeSearchEnabled = true,
            safeSearchPacks = packs
        )
        assertNull(safeEdu)
    }

    @Test
    fun dnsPacketHelperAndRemotePolicyDefaultsAreValid() {
        assertNull(
            DnsPacketHelper.matchesBlockedDomain(
                rawDomain = "classroom.google.com",
                blockExplicitAdult = true,
                blockGambling = true
            )
        )
        assertNotNull(
            DnsPacketHelper.matchesBlockedDomain(
                rawDomain = "sub.bet365.com",
                blockExplicitAdult = true,
                blockGambling = true
            )
        )

        val remote = RemotePolicySettings()
        assertFalse(remote.studyModeEnabled)
        assertEquals(25, remote.studyModeDurationMinutes)
        assertEquals(30, remote.studySoftWarningSeconds)
        assertFalse(remote.smartSafeSearchEnabled)
        assertTrue(remote.discreetBlockingScreenMode)
        assertTrue(remote.safeSearchPacks.contains(SafeSearchPack.ADULT_NSFW.id))
    }

    @Test
    fun editsScreenLocksWheneverGuardianPinExistsAndSessionIsLocked() {
        assertFalse(com.example.ui.settings.shouldLockEditsScreen(hasGuardianPin = false, isSessionUnlocked = false))
        assertFalse(com.example.ui.settings.shouldLockEditsScreen(hasGuardianPin = false, isSessionUnlocked = true))
        assertTrue(com.example.ui.settings.shouldLockEditsScreen(hasGuardianPin = true, isSessionUnlocked = false))
        assertFalse(com.example.ui.settings.shouldLockEditsScreen(hasGuardianPin = true, isSessionUnlocked = true))
    }
}
