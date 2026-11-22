package com.example

import android.app.Application
import android.app.job.JobScheduler
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.example.core.update.AegisUpdateResumeJobService
import com.example.core.update.ApkDownloadState
import com.example.core.update.UpdateChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowApplicationPackageManager
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 34])
class UpdateCompatRobolectricTest {

    @Test
    fun `resume job scheduling succeeds without NoSuchMethodError across API 24 and API 34`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        scheduler.cancelAll()

        AegisUpdateResumeJobService.schedule(context)

        val scheduledJobs = scheduler.allPendingJobs
        assertTrue("Expected persisted resume job to schedule cleanly", scheduledJobs.any { it.id == 7741 })

        AegisUpdateResumeJobService.cancel(context)
        assertFalse(scheduler.allPendingJobs.any { it.id == 7741 })
    }

    @Test
    fun `formatFriendlyUpdateError never leaks jvm linkage or framework class errors`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val linkageErr = NoSuchMethodError(
            "No virtual method setRequiresBatteryNotLow(Z)Landroid/app/job/JobInfo\$Builder; in class Landroid/app/job/JobInfo\$Builder;"
        )
        val formatted = UpdateChecker.formatFriendlyUpdateError(context, linkageErr)

        assertFalse(formatted.contains("No virtual method", ignoreCase = true))
        assertFalse(formatted.contains("JobInfo", ignoreCase = true))
        assertFalse(formatted.contains("Landroid/", ignoreCase = false))
        assertFalse(formatted.contains("framework.jar", ignoreCase = true))
        assertTrue(formatted.isNotBlank())
    }

    @Test
    fun `resolveNonBrowserInstallerPackage skips chrome and prefers system package installer`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pm = context.packageManager
        val shadowPm = Shadows.shadowOf(pm)

        val apkUri = Uri.parse("content://${context.packageName}.fileprovider/apk_files/updates/Sheildlenz-v17.56.apk")
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
        }

        val chromeResolve = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = "com.android.chrome"
                name = "com.google.android.apps.chrome.IntentDispatcher"
            }
        }
        val sysInstallerResolve = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = "com.google.android.packageinstaller"
                name = "com.android.packageinstaller.InstallStart"
            }
        }
        @Suppress("DEPRECATION")
        shadowPm.addResolveInfoForIntent(viewIntent, listOf(chromeResolve, sysInstallerResolve))

        val selected = UpdateChecker.resolveNonBrowserInstallerPackage(pm, viewIntent)
        assertEquals("com.google.android.packageinstaller", selected)
    }

    @Test
    fun `installApk launches package installer directly inside app and never opens browser`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pm = context.packageManager
        val shadowPm = Shadows.shadowOf(pm) as ShadowApplicationPackageManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            shadowPm.setCanRequestPackageInstalls(true)
        }
        val shadowApp = Shadows.shadowOf(context as Application)
        shadowApp.clearNextStartedActivities()

        val updatesDir = File(context.filesDir, "updates").apply { mkdirs() }
        val dummyApk = File(updatesDir, "Sheildlenz-v99.99.apk").apply {
            writeBytes(ByteArray(12 * 1024) { 0x5A })
        }

        val apkContentUri = Uri.parse("content://${context.packageName}.fileprovider/apk_files/updates/Sheildlenz-v99.99.apk")
        val viewFilterIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkContentUri, "application/vnd.android.package-archive")
        }
        @Suppress("DEPRECATION")
        val installPkgFilterIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            data = apkContentUri
        }

        val sysInstaller = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = "com.android.packageinstaller"
                name = "com.android.packageinstaller.InstallStart"
            }
        }
        @Suppress("DEPRECATION")
        shadowPm.addResolveInfoForIntent(viewFilterIntent, listOf(sysInstaller))
        @Suppress("DEPRECATION")
        shadowPm.addResolveInfoForIntent(installPkgFilterIntent, listOf(sysInstaller))

        UpdateChecker.cancelApkDownload(context)
        val launched = UpdateChecker.installApk(context, dummyApk)
        assertTrue(launched)

        val startedIntent = shadowApp.nextStartedActivity
        assertNotNull(startedIntent)
        assertFalse(
            "Installer intent must never target a browser",
            startedIntent.`package`?.contains("chrome", ignoreCase = true) == true
        )
        assertFalse(
            "Download state must not fail with NoSuchMethodError",
            UpdateChecker.apkDownloadState.value is ApkDownloadState.Failed
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            shadowApp.clearNextStartedActivities()
            shadowPm.setCanRequestPackageInstalls(false)
            val pausedForPermission = UpdateChecker.installApk(context, dummyApk)
            assertFalse(pausedForPermission)
            val permIntent = shadowApp.nextStartedActivity
            assertEquals(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, permIntent?.action)
            assertTrue((UpdateChecker.apkDownloadState.value as? ApkDownloadState.ReadyToInstall)?.needsInstallPermission == true)
        }

        dummyApk.delete()
    }
}
