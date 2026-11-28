package com.example

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.preferences.AegisPreferences
import com.example.di.AegisContainer
import com.example.timeblocker.BootCountProvider
import com.example.timeblocker.ElapsedClock
import com.example.timeblocker.PassBridgeResult
import com.example.timeblocker.TimeBlockerEngine
import com.example.timeblocker.TimeBlockerRepository
import com.example.timeblocker.connector.AppPassConnectorReceiver
import com.example.timeblocker.data.TimeBlockerDatabase
import com.example.timeblocker.ui.TimeBlockLockActivity
import com.example.timeblocker.ui.TimeBlockLockScreen
import com.example.ui.theme.AegisShieldTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TimeBlockerModuleTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `tier 1 five minute hourly base allowance blocks when exhausted resets after one hour and stays isolated from manual toggles`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = AegisPreferences(context)
        prefs.setMasterPowerEnabled(false)
        prefs.setLockedAppPackages(emptySet())

        var fakeElapsedMs = 1_000_000L
        val db = Room.inMemoryDatabaseBuilder(context, TimeBlockerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = TimeBlockerRepository(
                appContext = context,
                dao = db.timeBlockerDao(),
                clock = ElapsedClock { fakeElapsedMs },
                bootCountProvider = BootCountProvider { 1 },
                ioDispatcher = Dispatchers.Unconfined
            )

            val targetPkg = "com.instagram.android"
            repo.addTimeBlockedApp(targetPkg)
            assertTrue(repo.isTimeBlockedApp(targetPkg))
            assertFalse(prefs.lockedAppPackages.value.contains(targetPkg))

            val initial = repo.evaluatePackageAccess(targetPkg)
            assertTrue(initial.isRestricted)
            assertFalse(initial.shouldBlock)
            assertEquals(TimeBlockerEngine.BASE_ALLOWANCE_MS, initial.remainingAllowanceMs)

            // Consume 4 minutes -> still allowed (1 minute left)
            fakeElapsedMs += 240_000L
            val afterFourMin = repo.recordForegroundUsageDelta(targetPkg, 240_000L)
            assertFalse(afterFourMin.shouldBlock)
            assertEquals(60_000L, afterFourMin.remainingAllowanceMs)

            // Consume final 1 minute -> immediately blocked
            fakeElapsedMs += 60_000L
            val afterFiveMin = repo.recordForegroundUsageDelta(targetPkg, 60_000L)
            assertTrue(afterFiveMin.shouldBlock)
            assertEquals(0L, afterFiveMin.remainingAllowanceMs)

            // Advance 30 minutes -> still blocked with ~30m until hourly reset
            fakeElapsedMs += 1_800_000L
            val midHour = repo.evaluatePackageAccess(targetPkg)
            assertTrue(midHour.shouldBlock)
            assertTrue(midHour.remainingResetMs in 1_400_000L..1_600_000L)

            // Advance past 1-hour rolling window -> allowance automatically resets to 5 minutes
            fakeElapsedMs += 1_800_000L
            val afterOneHour = repo.evaluatePackageAccess(targetPkg)
            assertFalse(afterOneHour.shouldBlock)
            assertEquals(TimeBlockerEngine.BASE_ALLOWANCE_MS, afterOneHour.remainingAllowanceMs)
            repo.awaitPendingPersistence()
        } finally {
            db.close()
        }
    }

    @Test
    fun `tier 2 temporary pass hooks bypass hourly limit until monotonic expiry`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        var fakeElapsedMs = 5_000_000L
        val db = Room.inMemoryDatabaseBuilder(context, TimeBlockerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = TimeBlockerRepository(
                appContext = context,
                dao = db.timeBlockerDao(),
                clock = ElapsedClock { fakeElapsedMs },
                bootCountProvider = BootCountProvider { 2 },
                ioDispatcher = Dispatchers.Unconfined
            )

            val targetPkg = "com.zhiliaoapp.musically"
            repo.addTimeBlockedApp(targetPkg)

            // Exhaust 5-minute base allowance
            fakeElapsedMs += 300_000L
            val exhausted = repo.recordForegroundUsageDelta(targetPkg, 300_000L)
            assertTrue(exhausted.shouldBlock)
            assertFalse(repo.hasActivePass(targetPkg))

            // Grant 10-minute Tier 2 pass
            repo.grantTemporaryPass(targetPkg, durationMinutes = 10)
            assertTrue(repo.hasActivePass(targetPkg))
            val duringPass = repo.evaluatePackageAccess(targetPkg)
            assertFalse(duringPass.shouldBlock)
            assertTrue(duringPass.hasActivePass)
            assertEquals(600_000L, duringPass.passRemainingMs)

            // Advance 9 minutes -> pass remains active, does not get blocked
            fakeElapsedMs += 540_000L
            assertTrue(repo.hasActivePass(targetPkg))
            assertFalse(repo.recordForegroundUsageDelta(targetPkg, 60_000L).shouldBlock)

            // Advance past 10-minute expiry -> pass clears and block resumes
            fakeElapsedMs += 61_000L
            assertFalse(repo.hasActivePass(targetPkg))
            assertTrue(repo.evaluatePackageAccess(targetPkg).shouldBlock)
            repo.awaitPendingPersistence()
        } finally {
            db.close()
        }
    }

    @Test
    fun `device reboot resets monotonic elapsed counters cleanly without permanent lock and preserves valid pass remainder`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        var fakeElapsedMs = 80_000_000L
        var fakeBootCount = 5
        val db = Room.inMemoryDatabaseBuilder(context, TimeBlockerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = TimeBlockerRepository(
                appContext = context,
                dao = db.timeBlockerDao(),
                clock = ElapsedClock { fakeElapsedMs },
                bootCountProvider = BootCountProvider { fakeBootCount },
                ioDispatcher = Dispatchers.Unconfined
            )

            val blockedApp = "com.reddit.frontpage"
            val passApp = "com.discord"
            repo.addTimeBlockedApp(blockedApp)
            repo.addTimeBlockedApp(passApp)

            // Exhaust blockedApp at 80,000,000 ms uptime
            repo.recordForegroundUsageDelta(blockedApp, 300_000L)
            assertTrue(repo.evaluatePackageAccess(blockedApp).shouldBlock)

            // Grant 15-minute pass to passApp at 80,000,000 ms uptime -> expires at 80,900,000 ms
            repo.grantTemporaryPass(passApp, durationMinutes = 15)
            repo.awaitPendingPersistence()

            // Simulate device reboot: elapsedRealtime resets to 15,000 ms and bootCount increments
            fakeElapsedMs = 15_000L
            fakeBootCount = 6

            // Create a fresh repository reading persisted Room state after reboot
            val rebootedRepo = TimeBlockerRepository(
                appContext = context,
                dao = db.timeBlockerDao(),
                clock = ElapsedClock { fakeElapsedMs },
                bootCountProvider = BootCountProvider { fakeBootCount },
                ioDispatcher = Dispatchers.Unconfined
            )

            val afterRebootBlocked = rebootedRepo.evaluatePackageAccess(blockedApp)
            assertFalse(afterRebootBlocked.shouldBlock)
            assertEquals(TimeBlockerEngine.BASE_ALLOWANCE_MS, afterRebootBlocked.remainingAllowanceMs)
            assertEquals(TimeBlockerEngine.HOURLY_WINDOW_MS, afterRebootBlocked.remainingResetMs)

            // Valid temporary pass remainder is cleanly rebased to the new boot epoch
            assertTrue(rebootedRepo.hasActivePass(passApp))
            val passDecision = rebootedRepo.evaluatePackageAccess(passApp)
            assertEquals(900_000L, passDecision.passRemainingMs)

            // Advancing 16 minutes in new boot expires the rebased pass cleanly
            fakeElapsedMs += 960_000L
            assertFalse(rebootedRepo.hasActivePass(passApp))
            rebootedRepo.awaitPendingPersistence()
        } finally {
            db.close()
        }
    }

    @Test
    fun `lock screen renders blocked app countdown home exit and extended unlock hook`() {
        var exitedHome = false
        var requestedUnlockPkg: String? = null

        composeRule.setContent {
            AegisShieldTheme {
                TimeBlockLockScreen(
                    packageName = "com.google.android.youtube",
                    appLabel = "YouTube",
                    remainingResetMs = 1_530_000L,
                    onGoBackExit = { exitedHome = true },
                    onRequestExtendedUnlock = { pkg -> requestedUnlockPkg = pkg }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("time_blocker_lock_root").assertIsDisplayed()
        composeRule.onNodeWithTag("time_blocker_blocked_app_label").assertTextEquals("YouTube")
        composeRule.onNodeWithTag("time_blocker_blocked_package_name").assertTextEquals("com.google.android.youtube")
        composeRule.onNodeWithTag("time_blocker_countdown_text").assertTextEquals("25:30")

        composeRule.onNodeWithTag("time_blocker_extended_unlock_button")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
        assertEquals("com.google.android.youtube", requestedUnlockPkg)

        composeRule.onNodeWithTag("time_blocker_exit_button")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
        assertTrue(exitedHome)
    }

    @Test
    fun `launch lock screen fires dedicated activity with new task and clear top flags`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AegisContainer.getInstance(context)
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val shadowApp = Shadows.shadowOf(app)
        shadowApp.clearNextStartedActivities()

        val decision = com.example.timeblocker.TimeBlockDecision(
            packageName = "com.example.distraction",
            isRestricted = true,
            shouldBlock = true,
            hasActivePass = false,
            remainingAllowanceMs = 0L,
            remainingResetMs = 2_000_000L,
            passRemainingMs = 0L
        )
        container.timeBlockerRepository.launchLockScreen(context, decision)

        val startedIntent = shadowApp.nextStartedActivity
        assertNotNull(startedIntent)
        assertEquals(TimeBlockLockActivity::class.java.name, startedIntent.component?.className)
        assertTrue((startedIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0)
        assertTrue((startedIntent.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP) != 0)
        assertEquals("com.example.distraction", startedIntent.getStringExtra(TimeBlockLockActivity.EXTRA_PACKAGE_NAME))

        // Verify activity itself creates & resumes cleanly
        val controller = Robolectric.buildActivity(TimeBlockLockActivity::class.java, startedIntent)
            .create().start().resume()
        assertNotNull(controller.get())
        controller.pause().stop().destroy()
    }

    @Test
    fun `offline companion pass connector grants temporary pass prevents replay and prunes nonces`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        var fakeElapsedMs = 2_000_000L
        val db = Room.inMemoryDatabaseBuilder(context, TimeBlockerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = db.timeBlockerDao()
            val repo = TimeBlockerRepository(
                appContext = context,
                dao = dao,
                clock = ElapsedClock { fakeElapsedMs },
                bootCountProvider = BootCountProvider { 3 },
                ioDispatcher = Dispatchers.Unconfined
            )

            val targetPkg = "com.instagram.android"
            repo.addTimeBlockedApp(targetPkg)
            repo.recordForegroundUsageDelta(targetPkg, TimeBlockerEngine.BASE_ALLOWANCE_MS)
            assertTrue(repo.evaluatePackageAccess(targetPkg).shouldBlock)

            // Valid broadcast with omitted DURATION_MINUTES defaults to 30 minutes
            val validIntent = Intent(AppPassConnectorReceiver.ACTION_GRANT_APP_PASS).apply {
                putExtra(AppPassConnectorReceiver.EXTRA_TARGET_PACKAGE, targetPkg)
                putExtra(AppPassConnectorReceiver.EXTRA_NONCE, "nonce-unique-001")
            }
            val result = AppPassConnectorReceiver.processPassBroadcast(validIntent, repo)
            assertEquals(PassBridgeResult.GRANTED, result)
            assertTrue(repo.hasActivePass(targetPkg))
            val decision = repo.evaluatePackageAccess(targetPkg)
            assertFalse(decision.shouldBlock)
            assertEquals(30L * 60_000L, decision.passRemainingMs)

            // Replayed broadcast with same NONCE is rejected
            val replayResult = AppPassConnectorReceiver.processPassBroadcast(validIntent, repo)
            assertEquals(PassBridgeResult.DUPLICATE_NONCE, replayResult)

            // Blank nonce, blank package, wrong action, or negative duration are rejected
            val blankNonce = Intent(AppPassConnectorReceiver.ACTION_GRANT_APP_PASS).apply {
                putExtra(AppPassConnectorReceiver.EXTRA_TARGET_PACKAGE, targetPkg)
                putExtra(AppPassConnectorReceiver.EXTRA_NONCE, "   ")
            }
            assertEquals(PassBridgeResult.INVALID_INPUT, AppPassConnectorReceiver.processPassBroadcast(blankNonce, repo))

            val wrongAction = Intent("com.example.WRONG_ACTION").apply {
                putExtra(AppPassConnectorReceiver.EXTRA_TARGET_PACKAGE, targetPkg)
                putExtra(AppPassConnectorReceiver.EXTRA_NONCE, "nonce-unique-002")
            }
            assertEquals(PassBridgeResult.INVALID_INPUT, AppPassConnectorReceiver.processPassBroadcast(wrongAction, repo))

            // Verify nonce storage & pruning works
            assertEquals(1, dao.getNonceCount())
            repo.tryGrantTemporaryPassFromBridge(
                targetPackage = targetPkg,
                durationMinutes = 15,
                nonce = "nonce-unique-003",
                nowWallMs = 1_000_000L
            )
            repo.awaitPendingPersistence()
            assertTrue(dao.getNonceCount() >= 1)
            dao.pruneExcessNonces(keepCount = 1)
            assertEquals(1, dao.getNonceCount())
        } finally {
            db.close()
        }
    }

    @Test
    fun `manifest declares signature pass bridge receiver and omits unneeded battery optimization permission`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pm = context.packageManager

        val permInfo = pm.getPermissionInfo("com.ahsantcr.permission.APP_PASS_BRIDGE", 0)
        assertEquals(PermissionInfo.PROTECTION_SIGNATURE, permInfo.protection)

        val receiverComponent = ComponentName(context, AppPassConnectorReceiver::class.java)
        val receiverInfo = pm.getReceiverInfo(receiverComponent, 0)
        assertTrue(receiverInfo.exported)
        assertEquals("com.ahsantcr.permission.APP_PASS_BRIDGE", receiverInfo.permission)

        val resolved = pm.queryBroadcastReceivers(
            Intent(AppPassConnectorReceiver.ACTION_GRANT_APP_PASS),
            0
        )
        assertTrue(resolved.any { it.activityInfo.name == AppPassConnectorReceiver::class.java.name })

        val pkgInfo = pm.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        val requested = pkgInfo.requestedPermissions?.toList().orEmpty()
        assertFalse(requested.contains(Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS))
    }

    @Test
    fun `extended unlock launches piqorax when installed and shows clear notice when missing`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val shadowApp = Shadows.shadowOf(app)
        shadowApp.clearNextStartedActivities()

        // Not installed initially -> returns false
        assertFalse(TimeBlockLockActivity.launchPiqoraxCompanion(context, "com.instagram.android"))

        // Verify UI notice renders clearly when Piqorax is missing
        composeRule.setContent {
            AegisShieldTheme {
                TimeBlockLockScreen(
                    packageName = "com.instagram.android",
                    appLabel = "Instagram",
                    remainingResetMs = 900_000L,
                    companionNotice = "Piqorax companion study app (com.ahsantcr.piqorax) is not installed on this device.",
                    onGoBackExit = {},
                    onRequestExtendedUnlock = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("time_blocker_companion_missing_notice")
            .performScrollTo()
            .assertIsDisplayed()

        // Register Piqorax launcher activity via Robolectric ShadowPackageManager
        val shadowPm = Shadows.shadowOf(context.packageManager)
        val piqoraxComponent = ComponentName(
            TimeBlockLockActivity.PIQORAX_PACKAGE_NAME,
            "${TimeBlockLockActivity.PIQORAX_PACKAGE_NAME}.MainActivity"
        )
        shadowPm.addActivityIfNotPresent(piqoraxComponent)
        val launcherFilter = IntentFilter(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        shadowPm.addIntentFilterForActivity(piqoraxComponent, launcherFilter)

        assertTrue(TimeBlockLockActivity.launchPiqoraxCompanion(context, "com.instagram.android"))
        val launchedIntent = shadowApp.nextStartedActivity
        assertNotNull(launchedIntent)
        assertEquals(TimeBlockLockActivity.PIQORAX_PACKAGE_NAME, launchedIntent.component?.packageName)
        assertEquals(
            "com.instagram.android",
            launchedIntent.getStringExtra(TimeBlockLockActivity.EXTRA_REQUESTED_PACKAGE)
        )
    }

    @Test
    fun `piqorax connector screen shows connect and apply coin lock rule and clears on valid pass`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = com.example.di.AegisContainer(context)
        container.preferences.setPiqoraxConnectorConnected(false)
        container.preferences.setPiqoraxCoinDeviceLockActive(false)

        composeRule.setContent {
            AegisShieldTheme {
                com.example.ui.timeblocker.PiqoraxConnectorScreen(
                    preferences = container.preferences,
                    timeBlockerRepository = container.timeBlockerRepository,
                    onNavigateBack = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("piqorax_connector_prompt_card").assertIsDisplayed()
        composeRule.onNodeWithTag("piqorax_policy_info_toggle").assertIsDisplayed()
        composeRule.onNodeWithTag("piqorax_policy_info_toggle").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("piqorax_coin_lock_rule_card").assertIsDisplayed()
        composeRule.onNodeWithTag("piqorax_coin_device_lock_button").performClick()
        composeRule.waitForIdle()

        assertTrue(container.preferences.piqoraxConnectorConnected.value)
        assertTrue(container.preferences.piqoraxCoinDeviceLockActive.value)

        // Valid PiqoraX pass broadcast clears the coin device lock
        val intent = Intent(AppPassConnectorReceiver.ACTION_GRANT_APP_PASS).apply {
            putExtra(AppPassConnectorReceiver.EXTRA_TARGET_PACKAGE, "com.instagram.android")
            putExtra(AppPassConnectorReceiver.EXTRA_DURATION_MINUTES, 15)
            putExtra(AppPassConnectorReceiver.EXTRA_NONCE, "coin-unlock-nonce-${System.nanoTime()}")
        }
        val res = AppPassConnectorReceiver.processPassBroadcast(intent, container.timeBlockerRepository)
        assertEquals(com.example.timeblocker.PassBridgeResult.GRANTED, res)
    }
}
