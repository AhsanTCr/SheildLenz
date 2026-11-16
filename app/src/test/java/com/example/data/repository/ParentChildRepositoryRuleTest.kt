package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.example.base.FirestoreEmulatorTestBase
import com.example.core.models.SecurityIncident
import com.example.core.models.SensitivityLevel
import com.example.core.models.ThreatCategory
import com.example.core.models.ThreatSeverity
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ParentChildRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private lateinit var repo: ParentChildRepository

    @Before
    fun initRepo() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        repo = ParentChildRepository(context = context, db = firestore, auth = auth)
    }

    @Test
    fun unauthenticated_caller_is_rejected_from_pairing_and_profile_writes() = runBlocking {
        auth.signOut()
        assertFalse(repo.saveUserProfile(role = "parent", displayName = "Anon"))
        val pairResult = repo.pairWithChildByCode("123456")
        assertTrue(pairResult.isFailure)
    }

    @Test
    fun child_registers_parent_pairs_syncs_settings_commands_thumbnails_and_logs() = runBlocking {
        val ts = System.currentTimeMillis()
        val childEmail = "child_test_$ts@example.com"
        val parentEmail = "parent_test_$ts@example.com"

        val childUid = signInTestUser(childEmail)
        assertTrue(repo.saveUserProfile(role = "child", displayName = "Child Phone"))

        val code = withTimeout(5000L) { repo.registerAsChild("Child Phone") }
        assertEquals(6, code.length)

        // Switch to Parent account and claim child pairing code
        val parentUid = signInTestUser(parentEmail)
        assertTrue(repo.saveUserProfile(role = "parent", displayName = "Parent Phone"))

        val pairResult = withTimeout(5000L) { repo.pairWithChildByCode("sheildlenz://pair?code=$code") }
        assertTrue("Pairing should succeed: ${pairResult.exceptionOrNull()}", pairResult.isSuccess)
        assertEquals(childUid, pairResult.getOrNull()?.childUid)

        // Parent sees paired child in observePairedChildren
        val children = withTimeout(5000L) { repo.observePairedChildren().first { it.isNotEmpty() } }
        assertEquals(childUid, children.first().childUid)

        // Parent pushes remote settings + ALLOW_UNINSTALL command
        assertTrue(
            repo.pushRemoteSettings(
                childUid = childUid,
                settings = RemotePolicySettings(
                    sensitivity = SensitivityLevel.STRICT,
                    strictEverything = true,
                    lockdownDurationMinutes = 90,
                    antiTamperEnabled = true,
                    childUninstallLocked = true,
                    liveSeeEnabled = true,
                    whitelistedPackages = setOf("com.android.dialer"),
                    warningThresholdCount = 2,
                    bedtimeEnabled = true,
                    bedtimeStartMinutes = 22 * 60,
                    bedtimeEndMinutes = 6 * 60,
                    appTimeBudgets = mapOf("com.google.android.youtube" to 45),
                    customBlockedDomains = setOf("blocked-test.com"),
                    customBlockedKeywords = setOf("gambletest"),
                    safeDnsEnabled = true,
                    blockExplicitAdult = true,
                    blockSuggestiveContent = false,
                    blockSensitiveGraphic = true,
                    blockGamblingKeywords = true
                )
            )
        )
        assertTrue(repo.sendRemoteCommand(childUid, ParentChildRepository.COMMAND_ALLOW_UNINSTALL))

        // Switch back to Child: verifies child receives parent settings and remote command
        signInTestUser(childEmail)
        val syncedSettings = withTimeout(5000L) { repo.observeRemoteSettings(childUid).first { it != null } }
        assertEquals(SensitivityLevel.STRICT, syncedSettings?.sensitivity)
        assertEquals(90, syncedSettings?.lockdownDurationMinutes)
        assertEquals(2, syncedSettings?.warningThresholdCount)
        assertEquals(true, syncedSettings?.bedtimeEnabled)
        assertEquals(45, syncedSettings?.appTimeBudgets?.get("com.google.android.youtube"))
        assertTrue(syncedSettings?.customBlockedDomains?.contains("blocked-test.com") == true)
        assertTrue(syncedSettings?.customBlockedKeywords?.contains("gambletest") == true)
        assertEquals(true, syncedSettings?.safeDnsEnabled)
        assertEquals(false, syncedSettings?.blockSuggestiveContent)

        val syncedCmd = withTimeout(5000L) { repo.observeRemoteCommands(childUid).first { it != null } }
        assertEquals(ParentChildRepository.COMMAND_ALLOW_UNINSTALL, syncedCmd?.action)
        assertTrue(parentUid.isNotBlank())
    }

    @Test
    fun stranger_cannot_read_or_command_paired_child_remote_control() = runBlocking {
        val ts = System.currentTimeMillis()
        val childEmail = "c_sec_$ts@example.com"
        val parentEmail = "p_sec_$ts@example.com"
        val strangerEmail = "s_sec_$ts@example.com"

        val childUid = signInTestUser(childEmail)
        val code = withTimeout(5000L) { repo.registerAsChild("Child A") }

        signInTestUser(parentEmail)
        val paired = withTimeout(5000L) { repo.pairWithChildByCode(code) }
        assertTrue(paired.isSuccess)

        // Child uploads Live See thumbnail and threat log
        signInTestUser(childEmail)
        val bmp = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        assertTrue(repo.uploadLiveThumbnail(bmp, "Chrome", protectionArmed = true, lockdownActive = false))
        bmp.recycle()
        assertTrue(
            repo.logThreatToCloud(
                SecurityIncident(
                    timestampMillis = System.currentTimeMillis(),
                    packageName = "com.android.chrome",
                    appDisplayName = "Chrome",
                    category = ThreatCategory.EXPLICIT_ADULT,
                    severity = ThreatSeverity.CRITICAL,
                    confidenceScore = 0.94f,
                    actionTaken = "Evicted & Locked"
                )
            )
        )

        // Paired parent reads Live See thumbnail and threat log
        signInTestUser(parentEmail)
        val thumb = withTimeout(5000L) { repo.observeLiveThumbnail(childUid).first { it != null } }
        assertNotNull(thumb)
        assertEquals("Chrome", thumb?.activeAppName)

        val logs = withTimeout(5000L) { repo.observeThreatLogsForChild(childUid).first { it.isNotEmpty() } }
        assertEquals(1, logs.size)

        // Stranger is rejected from issuing remote commands or pushing settings to child
        signInTestUser(strangerEmail)
        assertFalse(repo.sendRemoteCommand(childUid, ParentChildRepository.COMMAND_UNLOCK))
        assertFalse(repo.pushRemoteSettings(childUid, RemotePolicySettings()))
    }
}
