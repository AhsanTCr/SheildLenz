package com.example

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.core.models.DailyScrollSnapshot
import com.example.core.models.GameAppDetector
import com.example.core.models.GameLockReason
import com.example.core.models.MindfulScrollEvent
import com.example.core.models.ShortVideoPlatform
import com.example.core.models.ShortVideoScrollDetector
import com.example.data.preferences.AegisPreferences
import com.example.di.AegisContainer
import com.example.ui.mindful.UsageAndScrollGuardScreen
import com.example.ui.protection.LiveProtectionScreen
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
class MindfulScrollGuardTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `short video detector identifies Shorts, Reels, TikTok, and FB Reels accurately while excluding chats and long-form players`() {
        // 1. YouTube Shorts detected by Shorts view IDs
        val ytShorts = ShortVideoScrollDetector.detectActivePlatform(
            packageName = "com.google.android.youtube",
            viewIds = listOf("com.google.android.youtube:id/reel_player_page_container", "com.google.android.youtube:id/reel_recycler"),
            labels = listOf("Shorts", "Cooking hack in 30 seconds")
        )
        assertEquals(ShortVideoPlatform.YOUTUBE_SHORTS, ytShorts)

        // 2. YouTube regular long-form video excluded
        val ytRegular = ShortVideoScrollDetector.detectActivePlatform(
            packageName = "com.google.android.youtube",
            viewIds = listOf("com.google.android.youtube:id/watch_player", "com.google.android.youtube:id/player_fragment_container"),
            labels = listOf("Android Architecture Tutorial 45:10")
        )
        assertNull(ytRegular)

        // 3. Instagram Reels detected, Instagram DM inbox excluded
        val igReels = ShortVideoScrollDetector.detectActivePlatform(
            packageName = "com.instagram.android",
            viewIds = listOf("com.instagram.android:id/clips_viewer_view_pager"),
            labels = listOf("Reels", "Original audio")
        )
        assertEquals(ShortVideoPlatform.INSTAGRAM_REELS, igReels)

        val igDm = ShortVideoScrollDetector.detectActivePlatform(
            packageName = "com.instagram.android",
            viewIds = listOf("com.instagram.android:id/direct_inbox_recycler_view"),
            labels = listOf("Messages")
        )
        assertNull(igDm)

        // 4. TikTok detected unless inside inbox/settings
        val tikTokFeed = ShortVideoScrollDetector.detectActivePlatform(
            packageName = "com.zhiliaoapp.musically",
            viewIds = listOf("com.zhiliaoapp.musically:id/view_pager"),
            labels = listOf("For You", "@creator_demo")
        )
        assertEquals(ShortVideoPlatform.TIKTOK, tikTokFeed)

        val tikTokInbox = ShortVideoScrollDetector.detectActivePlatform(
            packageName = "com.zhiliaoapp.musically",
            viewIds = listOf("com.zhiliaoapp.musically:id/inbox_chat_input"),
            labels = listOf("Inbox")
        )
        assertNull(tikTokInbox)

        // 5. Facebook Reels detected and respects enabledPlatformIds filter
        val fbReels = ShortVideoScrollDetector.detectActivePlatform(
            packageName = "com.facebook.katana",
            viewIds = listOf("com.facebook.katana:id/reels_viewer_container"),
            labels = listOf("Reels")
        )
        assertEquals(ShortVideoPlatform.FACEBOOK_REELS, fbReels)

        val filteredOut = ShortVideoScrollDetector.detectActivePlatform(
            packageName = "com.facebook.katana",
            viewIds = listOf("com.facebook.katana:id/reels_viewer_container"),
            labels = listOf("Reels"),
            enabledPlatformIds = setOf(ShortVideoPlatform.YOUTUBE_SHORTS.id)
        )
        assertNull(filteredOut)
    }

    @Test
    fun `detector signature, peak scroll window, compact formatting, and weekly report work cleanly`() {
        val sig = ShortVideoScrollDetector.buildContentSignature(
            labels = listOf("Shorts", "Like", "How to bake sourdough bread", "@chef_mike"),
            viewIds = listOf("reel_player")
        )
        assertEquals("How to bake sourdough bread|@chef_mike", sig)

        val buckets = MutableList(24) { 0 }.apply { this[22] = 18; this[14] = 4 }
        assertEquals("10 PM – 11 PM", ShortVideoScrollDetector.computePeakScrollWindow(buckets))
        assertEquals("No peak scroll data yet", ShortVideoScrollDetector.computePeakScrollWindow(List(24) { 0 }))

        assertEquals("0m", ShortVideoScrollDetector.formatMinutesCompact(0))
        assertEquals("45s", ShortVideoScrollDetector.formatMinutesCompact(45))
        assertEquals("12m", ShortVideoScrollDetector.formatMinutesCompact(12 * 60))
        assertEquals("1h 30m", ShortVideoScrollDetector.formatMinutesCompact(90 * 60))

        val report = ShortVideoScrollDetector.buildWeeklySummaryReport(
            todayUsageSeconds = 3900,
            todayReels = 14,
            scrollLimit = 20,
            todayExits = 3,
            streakDays = 5,
            peakWindow = "10 PM – 11 PM",
            history = listOf(
                DailyScrollSnapshot(
                    dateKey = "2026-11-26",
                    usageSeconds = 2400,
                    reelsCount = 12,
                    mindfulExits = 2,
                    locksTriggered = 0
                )
            )
        )
        assertTrue(report.contains("Sheildlenz Usage & Scroll Guard Weekly Report"))
        assertTrue(report.contains("Today Screen Time: 1h 5m"))
        assertTrue(report.contains("Today Reels/Shorts: 14 / 20"))
        assertTrue(report.contains("Under-Limit Streak: 5 day(s)"))
        assertTrue(report.contains("2026-11-26 | 40m | 12 reels | 2 exits | 0 locks"))
    }

    @Test
    fun `slider preferences clamp bounds and scroll-limit triggers binge nudge, lock, and PIN unlock reset`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = AegisPreferences(context)
        prefs.resetTodayReelCounters()

        // Check clamping on slider setters
        prefs.setScrollLimitCount(2)
        assertEquals(5, prefs.scrollLimitCount.value)
        prefs.setScrollLimitCount(99)
        assertEquals(60, prefs.scrollLimitCount.value)
        prefs.setScrollLimitCount(6)
        assertEquals(6, prefs.scrollLimitCount.value)

        prefs.setScrollLockCooldownMinutes(1)
        assertEquals(5, prefs.scrollLockCooldownMinutes.value)
        prefs.setScrollLockCooldownMinutes(90)
        assertEquals(60, prefs.scrollLockCooldownMinutes.value)
        prefs.setScrollLockCooldownMinutes(15)
        assertEquals(15, prefs.scrollLockCooldownMinutes.value)

        prefs.setScrollBingeNudgeCount(0)
        assertEquals(0, prefs.scrollBingeNudgeCount.value)
        prefs.setScrollBingeNudgeCount(3)
        assertEquals(5, prefs.scrollBingeNudgeCount.value)
        prefs.setScrollBingeNudgeCount(5)
        assertEquals(5, prefs.scrollBingeNudgeCount.value)

        prefs.setScrollLockEnabled(true)
        prefs.setReelsGuardPlatforms(setOf(ShortVideoPlatform.YOUTUBE_SHORTS.id))

        val baseTime = 1_795_000_000_000L
        // Scroll 1..4 -> no nudge, no lock
        for (i in 1..4) {
            val res = prefs.recordShortVideoScrolled(
                platform = ShortVideoPlatform.YOUTUBE_SHORTS,
                packageName = "com.google.android.youtube",
                appLabel = "YouTube",
                nowMillis = baseTime + i * 2000L,
                hourOfDay = 21
            )
            assertFalse(res.shouldTriggerBingeNudge)
            assertFalse(res.shouldTriggerScrollLock)
        }

        // Scroll 5 -> hits halfway binge nudge (5)
        val nudgeRes = prefs.recordShortVideoScrolled(
            platform = ShortVideoPlatform.YOUTUBE_SHORTS,
            packageName = "com.google.android.youtube",
            appLabel = "YouTube",
            nowMillis = baseTime + 10_000L,
            hourOfDay = 21
        )
        assertTrue(nudgeRes.shouldTriggerBingeNudge)
        assertFalse(nudgeRes.shouldTriggerScrollLock)

        // Scroll 6 -> hits scrollLimitCount (6) and locks YouTube for 15 minutes
        val lockRes = prefs.recordShortVideoScrolled(
            platform = ShortVideoPlatform.YOUTUBE_SHORTS,
            packageName = "com.google.android.youtube",
            appLabel = "YouTube",
            nowMillis = baseTime + 12_000L,
            hourOfDay = 21
        )
        assertTrue(lockRes.shouldTriggerScrollLock)
        assertEquals(6, prefs.dailyReelsTotalCount.value)
        assertEquals(6, prefs.hourlyScrollBuckets.value[21])
        assertTrue(prefs.dailyScrollLocksTriggered.value >= 1)

        val remainingLock = prefs.getRemainingScrollLockMillis("com.google.android.youtube", nowMillis = baseTime + 13_000L)
        assertTrue(remainingLock in (14 * 60_000L)..(15 * 60_000L))

        // Guardian PIN unlock clears active lock AND sets baseline to 6 so next scroll starts at session #1
        prefs.unlockScrollLockWithPin("com.google.android.youtube")
        assertEquals(0L, prefs.getRemainingScrollLockMillis("com.google.android.youtube", nowMillis = baseTime + 14_000L))

        val postUnlockRes = prefs.recordShortVideoScrolled(
            platform = ShortVideoPlatform.YOUTUBE_SHORTS,
            packageName = "com.google.android.youtube",
            appLabel = "YouTube",
            nowMillis = baseTime + 15_000L,
            hourOfDay = 21
        )
        assertEquals(1, postUnlockRes.sessionReelsSinceUnlock)
        assertFalse(postUnlockRes.shouldTriggerScrollLock)
        assertNotNull(prefs.recentMindfulScrollEvents.value.firstOrNull { it.type == MindfulScrollEvent.TYPE_SCROLL_LOCK })
    }

    @Test
    fun `mindful breathing pause, grace window, app opens, mindful exits, and instant detox work as designed`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = AegisPreferences(context)
        prefs.stopInstantDetox()
        prefs.setMindfulPauseEnabled(true)
        prefs.setMindfulPausePackages(setOf("com.instagram.android"))
        prefs.setMindfulGraceMinutes(10)

        assertTrue(prefs.shouldTriggerMindfulPauseForPackage("com.instagram.android"))
        assertFalse(prefs.shouldTriggerMindfulPauseForPackage("org.wikipedia"))

        // Granting grace suppresses repeat prompt until grace expires
        prefs.grantMindfulGracePass("com.instagram.android", nowRealtime = 1_000L)
        assertTrue(prefs.hasActiveMindfulGracePass("com.instagram.android", nowRealtime = 2_000L))
        assertFalse(prefs.hasActiveMindfulGracePass("com.instagram.android", nowRealtime = 1_000L + 11 * 60_000L))

        // App open & mindful exit counters increment and exit revokes grace
        val opens = prefs.recordAppOpen("com.instagram.android")
        assertTrue(opens >= 1)
        prefs.grantMindfulGracePass("com.instagram.android", nowRealtime = 5_000L)
        val exitsBefore = prefs.dailyMindfulExitsCount.value
        val exitsAfter = prefs.recordMindfulExit("com.instagram.android", "Instagram")
        assertEquals(exitsBefore + 1, exitsAfter)
        assertFalse(prefs.hasActiveMindfulGracePass("com.instagram.android", nowRealtime = 6_000L))

        // Instant Detox blocks mindful apps & short-video platforms immediately
        val detoxStart = 1_795_100_000_000L
        prefs.startInstantDetox(durationMinutes = 15, nowMillis = detoxStart)
        assertTrue(prefs.isInstantDetoxActive(nowMillis = detoxStart + 5_000L))
        assertTrue(prefs.isPackageBlockedByInstantDetox("com.instagram.android", nowMillis = detoxStart + 5_000L))
        assertTrue(prefs.isPackageBlockedByInstantDetox("com.zhiliaoapp.musically", nowMillis = detoxStart + 5_000L))
        assertFalse(prefs.isPackageBlockedByInstantDetox("org.wikipedia", nowMillis = detoxStart + 5_000L))

        // Auto-expires cleanly after duration
        assertFalse(prefs.isInstantDetoxActive(nowMillis = detoxStart + 16 * 60_000L))
        prefs.stopInstantDetox()
    }

    @Test
    fun `daily rollover archives previous snapshot into 7-day history and updates clean streak`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = AegisPreferences(context)
        prefs.resetTodayReelCounters()
        prefs.setScrollLimitCount(20)

        val day1 = "2026-11-20"
        val day2 = "2026-11-21"
        prefs.ensureMindfulDailyRollover(day1)
        prefs.recordAppUsageSeconds("com.google.android.youtube", 600, dateKey = day1)
        prefs.recordShortVideoScrolled(
            platform = ShortVideoPlatform.YOUTUBE_SHORTS,
            packageName = "com.google.android.youtube",
            appLabel = "YouTube",
            dateKey = day1,
            hourOfDay = 19
        )

        val streakBefore = prefs.cleanStreakDays.value
        // Rollover into day2 -> archives day1 and increments streak (1 <= 20)
        prefs.ensureMindfulDailyRollover(day2)
        assertEquals(0, prefs.dailyReelsTotalCount.value)
        assertEquals(streakBefore + 1, prefs.cleanStreakDays.value)
        val archived = prefs.weeklyScrollHistory.value.firstOrNull { it.dateKey == day1 }
        assertNotNull(archived)
        assertEquals(600, archived?.usageSeconds)
        assertEquals(1, archived?.reelsCount)
    }

    @Test
    fun `status screen shows usage and scroll guard gateway card and opens studio on tap`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AegisContainer(context)
        var openedStudio = false

        composeRule.setContent {
            AegisShieldTheme {
                LiveProtectionScreen(
                    protectionRepo = container.protectionRepository,
                    preferences = container.preferences,
                    blockedDnsLogRepo = container.blockedDnsLogRepository,
                    onNavigateToUsageScrollGuard = { openedStudio = true }
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("status_open_usage_scroll_guard_card"))
        composeRule.onNodeWithTag("status_open_usage_scroll_guard_card").assertIsDisplayed()
        composeRule.onNodeWithTag("status_open_usage_scroll_guard_card").performClick()
        composeRule.waitForIdle()
        assertTrue(openedStudio)
    }

    @Test
    fun `usage and scroll guard studio screen renders hero ring, sliders, breathing setup, detox, and trends`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AegisContainer(context)
        container.preferences.stopInstantDetox()
        container.preferences.resetTodayReelCounters()

        composeRule.setContent {
            AegisShieldTheme {
                UsageAndScrollGuardScreen(
                    preferences = container.preferences,
                    protectionRepo = container.protectionRepository,
                    onNavigateBack = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("usage_scroll_guard_screen_root").assertIsDisplayed()
        composeRule.onNodeWithTag("usage_scroll_hero_card").assertIsDisplayed()

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("scroll_limit_editor_card"))
        composeRule.onNodeWithTag("scroll_limit_editor_card").assertIsDisplayed()

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("mindful_breathing_config_card"))
        composeRule.onNodeWithTag("mindful_breathing_config_card").assertIsDisplayed()

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("instant_detox_share_card"))
        composeRule.onNodeWithTag("instant_detox_share_card").assertIsDisplayed()

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("seven_day_trends_card"))
        composeRule.onNodeWithTag("seven_day_trends_card").assertIsDisplayed()
    }

    @Test
    fun `tiktok lite packages and static tab chrome filtering work accurately`() {
        val liteFeed = ShortVideoScrollDetector.detectActivePlatform(
            packageName = "com.zhiliaoapp.musically.lite",
            viewIds = listOf("com.zhiliaoapp.musically.lite:id/pager"),
            labels = listOf("Following", "For You", "Friends", "LIVE", "@creator_lite", "Street food tour")
        )
        assertEquals(ShortVideoPlatform.TIKTOK, liteFeed)
        assertTrue("com.zhiliaoapp.musically.lite" in ShortVideoPlatform.ALL_PACKAGES)
        assertTrue("com.ss.android.ugc.awemelite" in ShortVideoPlatform.ALL_PACKAGES)
        assertTrue("com.zhiliaoapp.musically.go" in ShortVideoPlatform.ALL_PACKAGES)

        val sig = ShortVideoScrollDetector.buildContentSignature(
            labels = listOf("Following", "For You", "Friends", "LIVE", "@creator_lite", "Street food tour"),
            viewIds = listOf("com.zhiliaoapp.musically.lite:id/pager")
        )
        assertEquals("@creator_lite|Street food tour", sig)
    }

    @Test
    fun `gaming addiction control detects games, warns before session limit, enforces cooldown, daily cap, curfew, and PIN unlock`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = AegisPreferences(context)
        prefs.resetTodayReelCounters()

        // 1. GameAppDetector checks
        assertTrue(GameAppDetector.isKnownGamePackageHeuristic("com.supercell.brawlstars"))
        assertTrue(GameAppDetector.isKnownGamePackageHeuristic("com.dts.freefireth"))
        assertFalse(GameAppDetector.isKnownGamePackageHeuristic("com.google.android.apps.docs"))

        val categoryGameInfo = ApplicationInfo().apply {
            packageName = "org.custom.indie"
            category = ApplicationInfo.CATEGORY_GAME
        }
        assertTrue(GameAppDetector.isApplicationInfoGame(categoryGameInfo))

        // 2. Setter clamping
        prefs.setGameSessionLimitMinutes(5)
        assertEquals(10, prefs.gameSessionLimitMinutes.value)
        prefs.setGameCooldownMinutes(2)
        assertEquals(5, prefs.gameCooldownMinutes.value)
        prefs.setGameDailyCapMinutes(15)
        assertEquals(15, prefs.gameDailyCapMinutes.value)
        prefs.setGameGuardEnabled(true)
        prefs.setGameCurfew(enabled = true, startMinutes = 22 * 60, endMinutes = 7 * 60)

        val gamePkg = "com.supercell.clashroyale"
        prefs.syncAutoDetectedGames(setOf(gamePkg))
        assertTrue(prefs.isPackageTrackedGame(gamePkg))

        val dayKey = "2026-11-28"
        val nowMs = 1_795_800_000_000L
        prefs.ensureMindfulDailyRollover(dayKey)

        // 3. Curfew lock during 23:30 (1410m)
        val curfewDecision = prefs.evaluateGamePackageAccess(
            packageName = gamePkg,
            isDetectedGame = true,
            nowMillis = nowMs,
            nowUptimeMs = 10_000L,
            minuteOfDay = 23 * 60 + 30,
            dateKey = dayKey
        )
        assertTrue(curfewDecision.shouldBlock)
        assertEquals(GameLockReason.CURFEW, curfewDecision.lockReason)

        // 4. Daytime play: 9m10s tick -> hits 10m session 1-minute wrap-up soft warning (50s left)
        val warnTick = prefs.recordGameUsageTick(
            packageName = gamePkg,
            appLabel = "Clash Royale",
            deltaSeconds = 9 * 60 + 10,
            isDetectedGame = true,
            nowMillis = nowMs,
            nowUptimeMs = 20_000L,
            minuteOfDay = 14 * 60,
            dateKey = dayKey
        )
        assertFalse(warnTick.shouldBlock)
        assertTrue(warnTick.shouldShowSoftWarning)
        assertEquals(50, warnTick.remainingSessionSeconds)

        // 5. Remaining 50s -> triggers 5m SESSION_COOLDOWN lock
        val lockTick = prefs.recordGameUsageTick(
            packageName = gamePkg,
            appLabel = "Clash Royale",
            deltaSeconds = 50,
            isDetectedGame = true,
            nowMillis = nowMs + 50_000L,
            nowUptimeMs = 70_000L,
            minuteOfDay = 14 * 60 + 1,
            dateKey = dayKey
        )
        assertTrue(lockTick.shouldBlock)
        assertEquals(GameLockReason.SESSION_COOLDOWN, lockTick.lockReason)
        assertTrue(prefs.getRemainingGameCooldownMillis(nowMs + 51_000L) in (4 * 60_000L)..(5 * 60_000L))
        assertNotNull(prefs.recentMindfulScrollEvents.value.firstOrNull { it.type == MindfulScrollEvent.TYPE_GAME_SESSION_LOCK })

        // 6. Guardian PIN unlock clears cooldown immediately
        prefs.unlockGameLockWithPin(nowMillis = nowMs + 52_000L, nowUptimeMs = 72_000L)
        assertEquals(0L, prefs.getRemainingGameCooldownMillis(nowMs + 52_000L))

        // 7. Reaches 15m daily cap (already played 10m, add 5m) -> DAILY_CAP lock
        val dailyCapTick = prefs.recordGameUsageTick(
            packageName = gamePkg,
            appLabel = "Clash Royale",
            deltaSeconds = 5 * 60,
            isDetectedGame = true,
            nowMillis = nowMs + 400_000L,
            nowUptimeMs = 420_000L,
            minuteOfDay = 14 * 60 + 10,
            dateKey = dayKey
        )
        assertTrue(dailyCapTick.shouldBlock)
        assertEquals(GameLockReason.DAILY_CAP, dailyCapTick.lockReason)

        // Unlocking daily cap with PIN extends cap by 30m
        prefs.unlockGameLockWithPin(nowMillis = nowMs + 401_000L, nowUptimeMs = 421_000L)
        val afterPinDecision = prefs.evaluateGamePackageAccess(
            packageName = gamePkg,
            isDetectedGame = true,
            nowMillis = nowMs + 402_000L,
            nowUptimeMs = 422_000L,
            minuteOfDay = 14 * 60 + 10,
            dateKey = dayKey
        )
        assertFalse(afterPinDecision.shouldBlock)
    }

    @Test
    fun `usage and scroll guard studio displays gaming addiction control card and opens tracked games picker`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AegisContainer(context)
        container.preferences.stopInstantDetox()
        container.preferences.resetTodayReelCounters()

        composeRule.setContent {
            AegisShieldTheme {
                UsageAndScrollGuardScreen(
                    preferences = container.preferences,
                    protectionRepo = container.protectionRepository,
                    onNavigateBack = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("gaming_addiction_control_card"))
        composeRule.onNodeWithTag("gaming_addiction_control_card").assertIsDisplayed()
        composeRule.onNodeWithTag("gaming_guard_master_switch").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("game_curfew_switch").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("configure_tracked_games_button").performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tracked_games_picker_dialog").assertIsDisplayed()
    }
}
