package com.example.core.update

import android.app.Activity
import android.app.PendingIntent
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.example.di.AegisContainer
import java.lang.ref.WeakReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

data class UpdateInfo(
    val isUpdateAvailable: Boolean,
    val latestVersion: String,
    val currentVersion: String,
    val releaseUrl: String,
    val releaseNotes: String?,
    val apkDownloadUrl: String? = null,
    val apkSizeBytes: Long = 0L,
    val expectedSha256: String? = null
)

sealed class ApkDownloadState {
    data object Idle : ApkDownloadState()
    data class Preloading(
        val progress: Float?,
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val speedBps: Long = UpdateChecker.THROTTLED_PRELOAD_BPS
    ) : ApkDownloadState()
    data class PausedLowBattery(
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : ApkDownloadState()
    data class Downloading(
        val progress: Float?,
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : ApkDownloadState()
    data object Verifying : ApkDownloadState()
    data object Installing : ApkDownloadState()
    data class ReadyToInstall(
        val apkFile: File,
        val needsInstallPermission: Boolean = false,
        val preloadedReady: Boolean = false,
        val verifiedSha256: String? = null
    ) : ApkDownloadState()
    data class SignatureConflict(
        val apkFile: File,
        val savedPublicPath: String? = null,
        val reason: String
    ) : ApkDownloadState()
    data object Installed : ApkDownloadState()
    data class Failed(val message: String) : ApkDownloadState()
}

data class IntegrityCheckResult(
    val isValid: Boolean,
    val computedSha256: String? = null,
    val failureReason: String? = null
)

object UpdateChecker {
    private const val TAG = "UpdateChecker"
    const val GITHUB_OWNER = "Ahsanpcin"
    const val GITHUB_REPO = "Sheild-Lenz"
    const val GITHUB_RELEASES_WEB_URL = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases"
    private const val GITHUB_LATEST_RELEASE_URL =
        "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
    private const val GITHUB_ALL_RELEASES_URL =
        "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases?per_page=20"

    /** Strict 100 KB/s ceiling during silent background pre-download. */
    const val THROTTLED_PRELOAD_BPS: Long = 100L * 1024L
    const val TURBO_UNLIMITED_BPS: Long = Long.MAX_VALUE
    const val MIN_BATTERY_FOR_PRELOAD = 21 // strictly > 20% when unplugged

    private const val PREFS_NAME = "sheildlenz_update_prefetch"
    private const val KEY_PREF_VERSION = "pref_version"
    private const val KEY_PREF_URL = "pref_url"
    private const val KEY_PREF_TOTAL_BYTES = "pref_total_bytes"
    private const val KEY_PREF_SHA256 = "pref_sha256"

    private val updateExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Unhandled update coroutine error caught safely", throwable)
    }

    private val updateScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + updateExceptionHandler)

    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    private val _checkError = MutableStateFlow<String?>(null)
    val checkError: StateFlow<String?> = _checkError.asStateFlow()

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()

    private val _showUpToDateDialog = MutableStateFlow(false)
    val showUpToDateDialog: StateFlow<Boolean> = _showUpToDateDialog.asStateFlow()

    private val _apkDownloadState = MutableStateFlow<ApkDownloadState>(ApkDownloadState.Idle)
    val apkDownloadState: StateFlow<ApkDownloadState> = _apkDownloadState.asStateFlow()

    private val _homeBannerHidden = MutableStateFlow(false)
    val homeBannerHidden: StateFlow<Boolean> = _homeBannerHidden.asStateFlow()

    @Volatile
    private var activeCheckJob: Job? = null

    @Volatile
    private var activeApkJob: Job? = null

    @Volatile
    private var pendingInstallApk: File? = null

    @Volatile
    private var lastActivityRef: WeakReference<Activity>? = null

    private val activeBandwidthLimitBps = AtomicLong(THROTTLED_PRELOAD_BPS)
    private val autoInstallOnComplete = AtomicBoolean(false)

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    private fun rememberUiActivity(context: Context?) {
        val activity = context?.findActivity() ?: return
        if (!activity.isFinishing && !activity.isDestroyed) {
            lastActivityRef = WeakReference(activity)
        }
    }

    private fun resolveLaunchContext(fallbackContext: Context): Context {
        fallbackContext.findActivity()?.takeIf { !it.isFinishing && !it.isDestroyed }?.let { return it }
        lastActivityRef?.get()?.takeIf { !it.isFinishing && !it.isDestroyed }?.let { return it }
        return fallbackContext.applicationContext
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private val downloadClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .pingInterval(20, TimeUnit.SECONDS)
            .build()
    }

    private val http1DownloadClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .protocols(listOf(Protocol.HTTP_1_1))
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .build()
    }

    fun hideHomeUpdateBanner() {
        _homeBannerHidden.value = true
    }

    internal fun isNetworkAvailable(context: Context?): Boolean {
        if (context == null) return true
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
            val activeNet = cm.activeNetwork
            if (activeNet != null) {
                val activeCaps = cm.getNetworkCapabilities(activeNet)
                if (activeCaps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true) {
                    return true
                }
            }
            // Local VPNs (Android 7/8) or secondary links may report NET_CAPABILITY_INTERNET on underlying networks
            @Suppress("DEPRECATION")
            val anyCapInternet = cm.allNetworks.any { net ->
                cm.getNetworkCapabilities(net)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            }
            if (anyCapInternet) return true

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                @Suppress("DEPRECATION")
                return cm.activeNetworkInfo?.isConnectedOrConnecting == true
            }
            false
        } catch (_: Throwable) {
            false
        }
    }

    internal fun formatFriendlyUpdateError(
        context: Context?,
        err: Throwable?,
        fallback: String = "Unable to check for updates right now. Please try again."
    ): String {
        val offlineMsg = "You are offline. Please check your internet connection."
        if (context != null && !isNetworkAvailable(context)) {
            return offlineMsg
        }
        val causes = err?.let { root ->
            generateSequence(root) { it.cause }.take(8).toList()
        }.orEmpty()

        val looksOffline = causes.any { t ->
            val msg = t.message.orEmpty()
            t is java.net.UnknownHostException ||
                t is java.net.ConnectException ||
                t is java.net.SocketException ||
                t is java.net.SocketTimeoutException ||
                t is java.net.UnknownServiceException ||
                t is javax.net.ssl.SSLException ||
                msg.contains("unable to resolve host", ignoreCase = true) ||
                msg.contains("no address associated", ignoreCase = true) ||
                msg.contains("api.github.com", ignoreCase = true) ||
                msg.contains("github.com", ignoreCase = true) ||
                msg.contains("githubusercontent", ignoreCase = true) ||
                msg.contains("failed to connect", ignoreCase = true) ||
                msg.contains("network is unreachable", ignoreCase = true) ||
                msg.contains("enetunreach", ignoreCase = true) ||
                msg.contains("econnrefused", ignoreCase = true) ||
                msg.contains("eai_nodata", ignoreCase = true) ||
                msg.contains("timeout", ignoreCase = true) ||
                msg.contains("timed out", ignoreCase = true) ||
                msg.contains("connection reset", ignoreCase = true) ||
                msg.contains("software caused connection abort", ignoreCase = true) ||
                msg.contains("broken pipe", ignoreCase = true) ||
                msg.contains("offline", ignoreCase = true)
        }
        if (looksOffline) return offlineMsg

        val rawMsg = err?.message.orEmpty().trim()
        val leaksTechnicalDetails = rawMsg.isBlank() ||
            causes.any { t ->
                t is Error ||
                    t is SecurityException ||
                    t is IllegalStateException ||
                    t is IllegalArgumentException ||
                    t is NullPointerException ||
                    t is ClassCastException ||
                    t is java.io.IOException
            } ||
            rawMsg.contains("github", ignoreCase = true) ||
            rawMsg.contains("http://", ignoreCase = true) ||
            rawMsg.contains("https://", ignoreCase = true) ||
            rawMsg.contains("api.", ignoreCase = true) ||
            rawMsg.contains("Landroid/", ignoreCase = false) ||
            rawMsg.contains("Ljava/", ignoreCase = false) ||
            rawMsg.contains("framework.jar", ignoreCase = true) ||
            rawMsg.contains("No virtual method", ignoreCase = true) ||
            rawMsg.contains("NoSuchMethod", ignoreCase = true) ||
            rawMsg.contains("ClassNotFound", ignoreCase = true) ||
            rawMsg.contains("JobInfo", ignoreCase = true) ||
            rawMsg.contains("PackageInstaller", ignoreCase = true) ||
            rawMsg.contains("FileProvider", ignoreCase = true) ||
            rawMsg.contains(".kt:", ignoreCase = true) ||
            rawMsg.contains(".java:", ignoreCase = true) ||
            rawMsg.length > 160

        return if (leaksTechnicalDetails) fallback else rawMsg
    }

    fun startCheckForUpdates(
        context: Context? = null,
        showUpToDatePopup: Boolean = true
    ) {
        if (_isChecking.value) return
        val currentState = _apkDownloadState.value
        if (currentState is ApkDownloadState.Downloading ||
            currentState is ApkDownloadState.Verifying ||
            currentState is ApkDownloadState.Installing
        ) {
            return
        }

        rememberUiActivity(context)
        val appContext = context?.applicationContext
        if (appContext != null && !isNetworkAvailable(appContext)) {
            _isChecking.value = false
            if (showUpToDatePopup) {
                _showUpToDateDialog.value = false
                _checkError.value = "You are offline. Please check your internet connection."
            }
            return
        }

        if (showUpToDatePopup) {
            _homeBannerHidden.value = false
        }

        val previousTag = _updateInfo.value?.latestVersion
        activeCheckJob?.cancel()
        _isChecking.value = true
        _checkError.value = null
        if (!showUpToDatePopup) {
            _showUpToDateDialog.value = false
        }
        activeCheckJob = updateScope.launch {
            try {
                val result = checkForUpdates(appContext)
                _isChecking.value = false
                result.onSuccess { info ->
                    _updateInfo.value = info
                    if (previousTag != null && previousTag != info.latestVersion) {
                        _homeBannerHidden.value = false
                    }
                    if (!info.isUpdateAvailable) {
                        appContext?.let { clearPersistedTransferState(it) }
                        if (showUpToDatePopup) _showUpToDateDialog.value = true
                    } else if (appContext != null) {
                        maybeRestoreOrStartSilentPreload(appContext, info)
                    }
                }.onFailure { err ->
                    if (showUpToDatePopup) {
                        _checkError.value = formatFriendlyUpdateError(
                            context = appContext,
                            err = err,
                            fallback = "Unable to check for updates right now. Please try again."
                        )
                    }
                }
            } catch (ce: CancellationException) {
                _isChecking.value = false
                throw ce
            } catch (t: Throwable) {
                _isChecking.value = false
                Log.w(TAG, "Update check aborted safely: ${t.message}")
                if (showUpToDatePopup) {
                    _checkError.value = formatFriendlyUpdateError(
                        context = appContext,
                        err = t,
                        fallback = "Unable to check for updates right now. Please try again."
                    )
                }
            }
        }
    }

    private fun maybeRestoreOrStartSilentPreload(appContext: Context, info: UpdateInfo) {
        try {
            val apkUrl = info.apkDownloadUrl ?: return
            val cleanVer = cleanVersionTag(info.latestVersion)
            val finalApk = getFinalApkFile(appContext, cleanVer)

            // Already pre-downloaded & verified earlier -> hold ready for 0-second 1-tap install!
            if (finalApk.exists()) {
                val check = verifyApkArchiveAndHash(
                    context = appContext,
                    apkFile = finalApk,
                    expectedTotalBytes = info.apkSizeBytes,
                    expectedSha256 = info.expectedSha256
                )
                if (check.isValid) {
                    pendingInstallApk = finalApk
                    AegisUpdateResumeJobService.cancel(appContext)
                    _apkDownloadState.value = ApkDownloadState.ReadyToInstall(
                        apkFile = finalApk,
                        needsInstallPermission = false,
                        preloadedReady = true,
                        verifiedSha256 = check.computedSha256
                    )
                    return
                } else {
                    finalApk.delete()
                }
            }

            // Persist release target so app close / JobScheduler can resume `.part` seamlessly
            persistTransferMetadata(appContext, cleanVer, apkUrl, info.apkSizeBytes, info.expectedSha256)

        val currentState = _apkDownloadState.value
        if (currentState is ApkDownloadState.Preloading || currentState is ApkDownloadState.Downloading) {
            return
        }

        val (pct, charging) = readBatteryStatus(appContext)
        if (!isBatteryEligibleForSilentPreload(pct, charging)) {
            val partFile = getPartApkFile(appContext, cleanVer)
            _apkDownloadState.value = ApkDownloadState.PausedLowBattery(
                bytesDownloaded = if (partFile.exists()) partFile.length() else 0L,
                totalBytes = info.apkSizeBytes
            )
            AegisUpdateResumeJobService.schedule(appContext)
            return
        }

            startApkTransfer(
                context = appContext,
                apkUrl = apkUrl,
                versionTag = cleanVer,
                expectedTotalBytes = info.apkSizeBytes,
                expectedSha256 = info.expectedSha256,
                silentPreload = true
            )
        } catch (t: Throwable) {
            Log.w(TAG, "Silent preload skipped safely: ${t.message}")
        }
    }

    /**
     * Primary user CTA handler — strictly performs in-app download & install (never opens browser):
     * - If already verified/preloaded (100%) -> instant 1-tap in-app install
     * - If silently preloading at 100 KB/s -> lifts throttle live to Unlimited (Turbo finish)
     * - If release APK URL is present -> downloads directly inside the app and installs
     * - If APK URL was missing/stale -> live re-queries release & expanded assets, then downloads in-app
     */
    fun triggerUserUpdateAction(context: Context) {
        rememberUiActivity(context)
        val appContext = context.applicationContext
        try {
            val info = _updateInfo.value
            when (val state = _apkDownloadState.value) {
                is ApkDownloadState.ReadyToInstall -> {
                    autoInstallApk(resolveLaunchContext(context), state.apkFile)
                }
                is ApkDownloadState.SignatureConflict -> {
                    triggerCleanUninstallForUpgrade(resolveLaunchContext(context))
                }
                is ApkDownloadState.Preloading -> {
                    boostActivePreloadToTurbo(appContext, state)
                }
                is ApkDownloadState.Downloading,
                ApkDownloadState.Verifying,
                ApkDownloadState.Installing -> Unit
                else -> {
                    val apkUrl = info?.apkDownloadUrl
                    if (!apkUrl.isNullOrBlank()) {
                        startApkDownload(
                            context = appContext,
                            apkUrl = apkUrl,
                            versionTag = cleanVersionTag(info.latestVersion),
                            expectedTotalBytes = info.apkSizeBytes,
                            expectedSha256 = info.expectedSha256
                        )
                    } else {
                        refreshReleaseAndStartInAppDownload(appContext)
                    }
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Safe recovery from update action failure", t)
            _apkDownloadState.value = ApkDownloadState.Failed(
                formatFriendlyUpdateError(
                    context = appContext,
                    err = t,
                    fallback = "Unable to start update download right now. Please tap to retry."
                )
            )
        }
    }

    private fun refreshReleaseAndStartInAppDownload(appContext: Context) {
        if (!isNetworkAvailable(appContext)) {
            _apkDownloadState.value = ApkDownloadState.Failed(
                "You are offline. Please check your internet connection."
            )
            return
        }
        activeCheckJob?.cancel()
        _apkDownloadState.value = ApkDownloadState.Downloading(
            progress = null,
            bytesDownloaded = 0L,
            totalBytes = _updateInfo.value?.apkSizeBytes ?: 0L
        )
        AegisBackgroundDownloadService.notifyProgress(
            context = appContext,
            title = "Checking Sheildlenz Update",
            subtitle = "Locating release package...",
            progressPct = -1,
            force = true,
            useForegroundService = true
        )
        activeCheckJob = updateScope.launch {
            val result = checkForUpdates(appContext)
            result.onSuccess { latest ->
                _updateInfo.value = latest
                val resolvedApkUrl = latest.apkDownloadUrl
                if (!latest.isUpdateAvailable) {
                    AegisBackgroundDownloadService.stop(appContext)
                    _apkDownloadState.value = ApkDownloadState.Installed
                } else if (!resolvedApkUrl.isNullOrBlank()) {
                    startApkTransfer(
                        context = appContext,
                        apkUrl = resolvedApkUrl,
                        versionTag = cleanVersionTag(latest.latestVersion),
                        expectedTotalBytes = latest.apkSizeBytes,
                        expectedSha256 = latest.expectedSha256,
                        silentPreload = false
                    )
                } else {
                    AegisBackgroundDownloadService.stop(appContext)
                    _apkDownloadState.value = ApkDownloadState.Failed(
                        "Direct APK package was not found on this release. Tap Update to retry."
                    )
                }
            }.onFailure { err ->
                AegisBackgroundDownloadService.stop(appContext)
                _apkDownloadState.value = ApkDownloadState.Failed(
                    formatFriendlyUpdateError(
                        context = appContext,
                        err = err,
                        fallback = "Unable to fetch release package right now. Tap to retry."
                    )
                )
            }
        }
    }

    fun boostActivePreloadToTurbo(context: Context, snapshot: ApkDownloadState.Preloading? = null) {
        val appContext = context.applicationContext
        activeBandwidthLimitBps.set(TURBO_UNLIMITED_BPS)
        autoInstallOnComplete.set(true)
        val pre = snapshot ?: (_apkDownloadState.value as? ApkDownloadState.Preloading)
        if (pre != null) {
            _apkDownloadState.value = ApkDownloadState.Downloading(
                progress = pre.progress,
                bytesDownloaded = pre.bytesDownloaded,
                totalBytes = pre.totalBytes
            )
        }
        AegisBackgroundDownloadService.notifyProgress(
            context = appContext,
            title = "Finishing Sheildlenz Update",
            subtitle = "Accelerating download...",
            progressPct = pre?.progress?.let { (it * 100).toInt() } ?: -1,
            force = true
        )
    }

    fun startApkDownload(
        context: Context,
        apkUrl: String,
        versionTag: String = cleanVersionTag(_updateInfo.value?.latestVersion ?: BuildConfig.VERSION_NAME),
        expectedTotalBytes: Long = _updateInfo.value?.apkSizeBytes ?: 0L,
        expectedSha256: String? = _updateInfo.value?.expectedSha256
    ) {
        val current = _apkDownloadState.value
        if (current is ApkDownloadState.Preloading) {
            boostActivePreloadToTurbo(context, current)
            return
        }
        if (current is ApkDownloadState.Downloading ||
            current is ApkDownloadState.Verifying ||
            current is ApkDownloadState.Installing
        ) {
            return
        }
        startApkTransfer(
            context = context.applicationContext,
            apkUrl = apkUrl,
            versionTag = versionTag,
            expectedTotalBytes = expectedTotalBytes,
            expectedSha256 = expectedSha256,
            silentPreload = false
        )
    }

    private fun startApkTransfer(
        context: Context,
        apkUrl: String,
        versionTag: String,
        expectedTotalBytes: Long,
        expectedSha256: String?,
        silentPreload: Boolean
    ) {
        val appContext = context.applicationContext
        activeBandwidthLimitBps.set(if (silentPreload) THROTTLED_PRELOAD_BPS else TURBO_UNLIMITED_BPS)
        autoInstallOnComplete.set(!silentPreload)
        persistTransferMetadata(appContext, versionTag, apkUrl, expectedTotalBytes, expectedSha256)
        AegisUpdateResumeJobService.schedule(appContext)

        activeApkJob?.cancel()
        activeApkJob = updateScope.launch {
            try {
                if (!silentPreload) {
                    AegisBackgroundDownloadService.notifyProgress(
                        context = appContext,
                        title = "Downloading Sheildlenz Update",
                        subtitle = "Connecting...",
                        progressPct = -1,
                        force = true,
                        useForegroundService = true
                    )
                }

                val result = downloadApkResumable(
                    context = appContext,
                    apkUrl = apkUrl,
                    versionTag = versionTag,
                    expectedTotalBytes = expectedTotalBytes,
                    expectedSha256 = expectedSha256,
                    silentPreload = silentPreload
                ) { state ->
                    if (silentPreload && state is ApkDownloadState.Failed) {
                        _apkDownloadState.value = ApkDownloadState.Idle
                    } else {
                        _apkDownloadState.value = state
                    }
                    when (state) {
                        is ApkDownloadState.Preloading -> Unit
                        is ApkDownloadState.Downloading -> {
                            val pct = state.progress?.let { (it * 100).toInt().coerceIn(0, 100) } ?: -1
                            val label = formatProgressLabel(state.bytesDownloaded, state.totalBytes)
                            AegisBackgroundDownloadService.notifyProgress(
                                context = appContext,
                                title = "Downloading Sheildlenz Update",
                                subtitle = if (pct >= 0) "$pct% • $label" else label,
                                progressPct = pct,
                                useForegroundService = true
                            )
                        }
                        ApkDownloadState.Verifying -> {
                            if (!silentPreload || autoInstallOnComplete.get()) {
                                AegisBackgroundDownloadService.notifyProgress(
                                    context = appContext,
                                    title = "Verifying Sheildlenz Update",
                                    subtitle = "Checking SHA-256 & APK integrity...",
                                    progressPct = -1,
                                    force = true,
                                    useForegroundService = true
                                )
                            }
                        }
                        else -> Unit
                    }
                }

                result.onSuccess { verifiedApk ->
                    AegisUpdateResumeJobService.cancel(appContext)
                    if (autoInstallOnComplete.get()) {
                        AegisBackgroundDownloadService.notifyProgress(
                            context = appContext,
                            title = "Installing Sheildlenz Update",
                            subtitle = "Launching package installer...",
                            progressPct = -1,
                            force = true,
                            useForegroundService = true
                        )
                        withContext(Dispatchers.Main) {
                            autoInstallApk(resolveLaunchContext(appContext), verifiedApk)
                        }
                    } else {
                        // Hold ready quietly with badge for instant 1-tap install
                        AegisBackgroundDownloadService.stop(appContext)
                    }
                }.onFailure {
                    AegisBackgroundDownloadService.stop(appContext)
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                Log.w(TAG, "APK transfer coroutine stopped safely: ${t.message}")
                AegisBackgroundDownloadService.stop(appContext)
                if (!silentPreload) {
                    _apkDownloadState.value = ApkDownloadState.Failed(
                        formatFriendlyUpdateError(
                            context = appContext,
                            err = t,
                            fallback = "Update download interrupted. Tap to retry."
                        )
                    )
                }
            }
        }
    }

    fun onBatteryStateChanged(context: Context, batteryPercent: Int, isCharging: Boolean) {
        val appContext = context.applicationContext
        val current = _apkDownloadState.value
        val eligible = isBatteryEligibleForSilentPreload(batteryPercent, isCharging)
        if (!eligible && current is ApkDownloadState.Preloading) {
            // Pause polite background preload below 20% while keeping `.part` bytes intact!
            activeApkJob?.cancel()
            activeApkJob = null
            AegisBackgroundDownloadService.stop(appContext)
            _apkDownloadState.value = ApkDownloadState.PausedLowBattery(
                bytesDownloaded = current.bytesDownloaded,
                totalBytes = current.totalBytes
            )
            AegisUpdateResumeJobService.schedule(appContext)
        } else if (eligible && current is ApkDownloadState.PausedLowBattery) {
            resumePersistedPreloadFromJob(appContext)
        }
    }

    fun hasIncompletePersistedDownload(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val ver = prefs.getString(KEY_PREF_VERSION, null) ?: return false
        val url = prefs.getString(KEY_PREF_URL, null) ?: return false
        if (ver.isBlank() || url.isBlank()) return false
        if (compareVersions(ver, cleanVersionTag(BuildConfig.VERSION_NAME)) <= 0) return false
        return !getFinalApkFile(context, ver).exists()
    }

    fun resumePersistedPreloadFromJob(context: Context, onFinished: (() -> Unit)? = null) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val ver = prefs.getString(KEY_PREF_VERSION, null)
        val url = prefs.getString(KEY_PREF_URL, null)
        val total = prefs.getLong(KEY_PREF_TOTAL_BYTES, 0L)
        val sha256 = prefs.getString(KEY_PREF_SHA256, null)

        if (ver.isNullOrBlank() || url.isNullOrBlank() ||
            compareVersions(ver, cleanVersionTag(BuildConfig.VERSION_NAME)) <= 0
        ) {
            clearPersistedTransferState(appContext)
            onFinished?.invoke()
            return
        }

        val (pct, charging) = readBatteryStatus(appContext)
        if (!isBatteryEligibleForSilentPreload(pct, charging)) {
            onFinished?.invoke()
            return
        }

        val current = _apkDownloadState.value
        if (current is ApkDownloadState.Preloading ||
            current is ApkDownloadState.Downloading ||
            current is ApkDownloadState.Verifying ||
            current is ApkDownloadState.Installing
        ) {
            onFinished?.invoke()
            return
        }

        startApkTransfer(
            context = appContext,
            apkUrl = url,
            versionTag = ver,
            expectedTotalBytes = total,
            expectedSha256 = sha256,
            silentPreload = true
        )
        updateScope.launch {
            activeApkJob?.join()
            onFinished?.invoke()
        }
    }

    fun cancelApkDownload(context: Context? = null) {
        activeApkJob?.cancel()
        activeApkJob = null
        autoInstallOnComplete.set(false)
        context?.applicationContext?.let {
            AegisBackgroundDownloadService.stop(it)
            AegisUpdateResumeJobService.cancel(it)
        }
        _apkDownloadState.value = ApkDownloadState.Idle
    }

    fun dismissUpToDateDialog() {
        _showUpToDateDialog.value = false
    }

    fun dismissUpdatePrompt() {
        val state = _apkDownloadState.value
        if (state !is ApkDownloadState.Preloading &&
            state !is ApkDownloadState.Downloading &&
            state !is ApkDownloadState.Verifying &&
            state !is ApkDownloadState.Installing
        ) {
            _apkDownloadState.value = ApkDownloadState.Idle
        }
    }

    suspend fun checkForUpdates(context: Context? = null): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        try {
            if (context != null && !isNetworkAvailable(context)) {
                return@withContext Result.failure(
                    Exception("You are offline. Please check your internet connection.")
                )
            }
            val candidates = mutableListOf<JSONObject>()

            val allResult = runCatching { fetchReleaseJson(GITHUB_ALL_RELEASES_URL) }.getOrNull()
            if (allResult != null && allResult.code == 200 && allResult.body != null) {
                val releasesArray = JSONArray(allResult.body)
                for (i in 0 until releasesArray.length()) {
                    val item = releasesArray.optJSONObject(i) ?: continue
                    if (!item.optBoolean("draft", false)) {
                        candidates.add(item)
                    }
                }
            }

            if (candidates.isEmpty()) {
                val latestResult = runCatching { fetchReleaseJson(GITHUB_LATEST_RELEASE_URL) }.getOrNull()
                if (latestResult != null && latestResult.code == 200 && latestResult.body != null) {
                    val obj = JSONObject(latestResult.body)
                    if (!obj.optBoolean("draft", false)) {
                        candidates.add(obj)
                    }
                } else {
                    // Web redirect + expanded_assets fallback avoids REST API rate limits strictly in-app
                    val webFallback = fetchLatestReleaseViaWebRedirect()
                    if (webFallback != null) {
                        return@withContext Result.success(webFallback)
                    }
                    val msg = if (context != null && !isNetworkAvailable(context)) {
                        "You are offline. Please check your internet connection."
                    } else {
                        "Unable to check for updates right now. Please try again later."
                    }
                    return@withContext Result.failure(Exception(msg))
                }
            }

            if (candidates.isEmpty()) {
                return@withContext Result.success(
                    UpdateInfo(
                        isUpdateAvailable = false,
                        latestVersion = "v${BuildConfig.VERSION_NAME}",
                        currentVersion = BuildConfig.VERSION_NAME,
                        releaseUrl = GITHUB_RELEASES_WEB_URL,
                        releaseNotes = null
                    )
                )
            }

            val apkCandidates = candidates.filter { hasApkAsset(it) }
            val pool = if (apkCandidates.isNotEmpty()) apkCandidates else candidates
            val bestRelease = pool.maxWithOrNull { a, b ->
                compareVersions(
                    cleanVersionTag(a.optString("tag_name", "")),
                    cleanVersionTag(b.optString("tag_name", ""))
                )
            } ?: candidates.first()

            var parsed = parseReleaseJson(bestRelease, BuildConfig.VERSION_NAME)
            if (parsed.apkDownloadUrl.isNullOrBlank()) {
                val expandedApkUrl = fetchExpandedAssetsApkUrl(parsed.latestVersion)
                if (!expandedApkUrl.isNullOrBlank()) {
                    parsed = parsed.copy(apkDownloadUrl = expandedApkUrl)
                }
            }
            Result.success(parsed)
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to check for updates", t)
            val friendly = formatFriendlyUpdateError(
                context = context,
                err = t,
                fallback = "Unable to check for updates right now. Please try again."
            )
            Result.failure(Exception(friendly, t))
        }
    }

    private fun fetchLatestReleaseViaWebRedirect(): UpdateInfo? {
        return try {
            val req = Request.Builder()
                .url("https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases/latest")
                .header("User-Agent", "Sheildlenz-Android-App")
                .header("Cache-Control", "no-cache")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val finalUrl = resp.request.url.toString()
                val tagName = finalUrl.substringAfter("/releases/tag/", "").substringBefore('?').substringBefore('#').trim()
                if (tagName.isBlank()) return null
                val expandedApkUrl = fetchExpandedAssetsApkUrl(tagName)
                val latestClean = cleanVersionTag(tagName)
                val currentClean = cleanVersionTag(BuildConfig.VERSION_NAME)
                val isNewer = compareVersions(latestClean, currentClean) > 0
                UpdateInfo(
                    isUpdateAvailable = isNewer,
                    latestVersion = tagName,
                    currentVersion = BuildConfig.VERSION_NAME,
                    releaseUrl = finalUrl,
                    releaseNotes = "Bug fixes and performance improvements.",
                    apkDownloadUrl = expandedApkUrl,
                    apkSizeBytes = 0L,
                    expectedSha256 = null
                )
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun isApkAssetJson(asset: JSONObject): Boolean {
        val assetName = asset.optString("name", "")
        val downloadUrl = asset.optString("browser_download_url", "")
        val contentType = asset.optString("content_type", "")
        if (downloadUrl.isBlank()) return false
        return assetName.endsWith(".apk", ignoreCase = true) ||
            downloadUrl.substringBefore('?').endsWith(".apk", ignoreCase = true) ||
            contentType.equals("application/vnd.android.package-archive", ignoreCase = true)
    }

    private fun hasApkAsset(json: JSONObject): Boolean {
        val assets = json.optJSONArray("assets")
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                if (isApkAssetJson(asset)) return true
            }
        }
        return extractApkUrlFromReleaseBody(json.optString("body", "")) != null
    }

    private fun extractApkUrlFromReleaseBody(body: String?): String? {
        if (body.isNullOrBlank()) return null
        return Regex("https://[^\\s)\"'<>]+\\.apk(?:\\?[^\\s)\"'<>]*)?", RegexOption.IGNORE_CASE)
            .find(body)
            ?.value
    }

    private fun fetchExpandedAssetsApkUrl(tagName: String): String? {
        val cleanTag = tagName.trim()
        if (cleanTag.isBlank()) return null
        return try {
            val req = Request.Builder()
                .url("https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases/expanded_assets/$cleanTag")
                .header("User-Agent", "Sheildlenz-Android-App")
                .header("Cache-Control", "no-cache")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val html = resp.body?.string().orEmpty()
                val hrefMatch = Regex(
                    "href=[\"'](/[^\"']*releases/download/[^\"']*\\.apk(?:\\?[^\"']*)?)[\"']",
                    RegexOption.IGNORE_CASE
                ).find(html)
                hrefMatch?.groupValues?.getOrNull(1)?.let { rel ->
                    "https://github.com$rel"
                }
            }
        } catch (_: Throwable) {
            null
        }
    }

    internal fun cleanVersionTag(raw: String): String {
        val trimmed = raw.trim()
        val match = Regex("(\\d+(?:\\.\\d+)*)").find(trimmed)
        return match?.value ?: trimmed.removePrefix("v").removePrefix("V").trim()
    }

    internal fun extractSha256FromText(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val labeledMatch = Regex("APK_SHA256\\s*[:=]\\s*([a-fA-F0-9]{64})").find(text)
        if (labeledMatch != null) return labeledMatch.groupValues[1].lowercase(Locale.US)
        val digestPrefixMatch = Regex("sha256:([a-fA-F0-9]{64})").find(text)
        return digestPrefixMatch?.groupValues?.get(1)?.lowercase(Locale.US)
    }

    internal fun parseReleaseJson(json: JSONObject, currentVersionName: String): UpdateInfo {
        val tagName = json.optString("tag_name", "v$currentVersionName")
        val htmlUrl = json.optString("html_url", GITHUB_RELEASES_WEB_URL)
        val body = json.optString("body", "").ifBlank {
            "Bug fixes and performance improvements."
        }

        var apkUrl: String? = null
        var apkSize = 0L
        var assetDigest: String? = null
        val assets = json.optJSONArray("assets")
        if (assets != null) {
            var bestScore = Int.MIN_VALUE
            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                if (!isApkAssetJson(asset)) continue
                val assetName = asset.optString("name", "")
                val downloadUrl = asset.optString("browser_download_url", "")
                var score = 10
                if (assetName.contains("release", ignoreCase = true)) score += 20
                if (assetName.contains("sheild", ignoreCase = true) || assetName.contains("shield", ignoreCase = true)) score += 5
                if (assetName.contains("debug", ignoreCase = true)) score -= 15
                if (score > bestScore) {
                    bestScore = score
                    apkUrl = downloadUrl
                    apkSize = asset.optLong("size", 0L)
                    assetDigest = extractSha256FromText(asset.optString("digest", ""))
                }
            }
        }
        if (apkUrl.isNullOrBlank()) {
            apkUrl = extractApkUrlFromReleaseBody(body)
        }

        val resolvedSha256 = assetDigest ?: extractSha256FromText(body)
        val latestClean = cleanVersionTag(tagName)
        val currentClean = cleanVersionTag(currentVersionName)
        val isNewer = compareVersions(latestClean, currentClean) > 0

        return UpdateInfo(
            isUpdateAvailable = isNewer,
            latestVersion = tagName,
            currentVersion = currentVersionName,
            releaseUrl = htmlUrl,
            releaseNotes = body,
            apkDownloadUrl = apkUrl,
            apkSizeBytes = apkSize,
            expectedSha256 = resolvedSha256
        )
    }

    suspend fun downloadApkResumable(
        context: Context,
        apkUrl: String,
        versionTag: String = "latest",
        expectedTotalBytes: Long = 0L,
        expectedSha256: String? = null,
        silentPreload: Boolean = false,
        onStateChanged: (ApkDownloadState) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val cleanVer = cleanVersionTag(versionTag).ifBlank { "latest" }
        val partFile = getPartApkFile(context, cleanVer)
        val finalApk = getFinalApkFile(context, cleanVer)
        cleanupOldVersionArtifacts(context, cleanVer)

        try {
            if (!isNetworkAvailable(context) && !finalApk.exists()) {
                val msg = "You are offline. Please check your internet connection."
                onStateChanged(ApkDownloadState.Failed(msg))
                return@withContext Result.failure(Exception(msg))
            }
            if (finalApk.exists()) {
                onStateChanged(ApkDownloadState.Verifying)
                val existingVerify = verifyApkArchiveAndHash(context, finalApk, expectedTotalBytes, expectedSha256)
                if (existingVerify.isValid) {
                    pendingInstallApk = finalApk
                    onStateChanged(
                        ApkDownloadState.ReadyToInstall(
                            apkFile = finalApk,
                            needsInstallPermission = false,
                            preloadedReady = !autoInstallOnComplete.get(),
                            verifiedSha256 = existingVerify.computedSha256
                        )
                    )
                    return@withContext Result.success(finalApk)
                } else {
                    finalApk.delete()
                }
            }

            if (expectedTotalBytes > 0L && partFile.exists() && partFile.length() > expectedTotalBytes) {
                partFile.delete()
            }

            var cleanVerificationRetriesRemaining = 1
            var activeClient = downloadClient
            while (true) {
                var existingBytes = if (partFile.exists()) partFile.length() else 0L
                val startedWithPartial = existingBytes > 0L
                val initialProgress = if (expectedTotalBytes > 0L && existingBytes > 0L) {
                    (existingBytes.toFloat() / expectedTotalBytes.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }
                emitTransferState(onStateChanged, initialProgress, existingBytes, expectedTotalBytes)

                // Only stream over HTTP if not already completely present in `.part`
                if (expectedTotalBytes <= 0L || existingBytes < expectedTotalBytes) {
                    var attemptResume = existingBytes > 0L
                    var transientIoRetriesRemaining = 5
                    var rangeResetAttemptsRemaining = 3
                    var retryAttemptCount = 0
                    var shouldContinueTransferLoop: Boolean

                    do {
                        shouldContinueTransferLoop = false
                        existingBytes = if (partFile.exists()) partFile.length() else 0L
                        val reqBuilder = Request.Builder()
                            .url(apkUrl)
                            .header("User-Agent", "Sheildlenz-Android-App")
                            .header("Accept-Encoding", "identity")
                        if (attemptResume && existingBytes > 0L) {
                            reqBuilder.header("Range", "bytes=$existingBytes-")
                        }

                        try {
                            activeClient.newCall(reqBuilder.build()).execute().use { response ->
                                if (response.code == 416) {
                                    // Server indicates offset >= total; verify exact length or wipe & restart fresh
                                    if (expectedTotalBytes > 0L && existingBytes == expectedTotalBytes) {
                                        return@use
                                    }
                                    partFile.delete()
                                    existingBytes = 0L
                                    attemptResume = false
                                    if (rangeResetAttemptsRemaining > 0) {
                                        rangeResetAttemptsRemaining--
                                        shouldContinueTransferLoop = true
                                    }
                                    return@use
                                }

                                // Transient CDN 5xx or stale Range 403/410 -> cleanly retry / reset Range once
                                if ((response.code in 500..599 || ((response.code == 403 || response.code == 410) && attemptResume)) &&
                                    transientIoRetriesRemaining > 0
                                ) {
                                    if (response.code == 403 || response.code == 410) {
                                        partFile.delete()
                                        attemptResume = false
                                    }
                                    transientIoRetriesRemaining--
                                    retryAttemptCount++
                                    shouldContinueTransferLoop = true
                                    delay((retryAttemptCount * 1_000L).coerceAtMost(4_000L))
                                    return@use
                                }

                                if (!response.isSuccessful && response.code != 206) {
                                    val msg = "Update package could not be downloaded right now. Tap to retry."
                                    onStateChanged(ApkDownloadState.Failed(msg))
                                    return@withContext Result.failure(Exception(msg))
                                }

                                val contentType = response.header("Content-Type").orEmpty()
                                if (contentType.contains("text/html", ignoreCase = true)) {
                                    partFile.delete()
                                    val msg = "Network blocked or altered the update file. Check Wi-Fi/DNS and retry."
                                    onStateChanged(ApkDownloadState.Failed(msg))
                                    return@withContext Result.failure(Exception(msg))
                                }

                                val contentRangeHeader = response.header("Content-Range")
                                val rangeOffsetValid = if (response.code == 206 && !contentRangeHeader.isNullOrBlank()) {
                                    val startOffset = contentRangeHeader
                                        .substringAfter("bytes ", "")
                                        .substringBefore('-', "")
                                        .trim()
                                        .toLongOrNull()
                                    startOffset == null || startOffset == existingBytes
                                } else {
                                    true
                                }
                                if (response.code == 206 && !rangeOffsetValid) {
                                    partFile.delete()
                                    existingBytes = 0L
                                    attemptResume = false
                                    if (rangeResetAttemptsRemaining > 0) {
                                        rangeResetAttemptsRemaining--
                                        shouldContinueTransferLoop = true
                                    }
                                    return@use
                                }

                                val isAppend = response.code == 206 && existingBytes > 0L
                                val body = response.body ?: run {
                                    val msg = "Empty update package response. Tap to retry."
                                    onStateChanged(ApkDownloadState.Failed(msg))
                                    return@withContext Result.failure(Exception(msg))
                                }

                                val totalBytes = resolveTotalBytes(
                                    contentRangeHeader = contentRangeHeader,
                                    bodyContentLength = body.contentLength(),
                                    existingOffset = if (isAppend) existingBytes else 0L,
                                    fallbackExpected = expectedTotalBytes
                                )
                                var downloadedBytes = if (isAppend) existingBytes else 0L
                                if (totalBytes > 0L) {
                                    persistTotalBytes(context, totalBytes)
                                }

                                val buffer = ByteArray(16 * 1024)
                                var windowBytes = 0L
                                var unflushedBytes = 0L
                                var windowStart = SystemClock.elapsedRealtime()
                                var lastEmit = 0L

                                body.byteStream().use { input ->
                                    FileOutputStream(partFile, isAppend).use { output ->
                                        while (true) {
                                            currentCoroutineContext().ensureActive()
                                            val read = input.read(buffer)
                                            if (read == -1) break
                                            output.write(buffer, 0, read)
                                            downloadedBytes += read
                                            windowBytes += read
                                            unflushedBytes += read

                                            if (unflushedBytes >= 256L * 1024L) {
                                                output.flush()
                                                unflushedBytes = 0L
                                            }

                                            val limit = activeBandwidthLimitBps.get()
                                            if (limit != TURBO_UNLIMITED_BPS) {
                                                var sleepRemaining = computeThrottleDelayMs(
                                                    bytesInWindow = windowBytes,
                                                    elapsedMs = SystemClock.elapsedRealtime() - windowStart,
                                                    limitBps = limit
                                                ).coerceAtMost(150L)
                                                while (sleepRemaining > 0L && activeBandwidthLimitBps.get() != TURBO_UNLIMITED_BPS) {
                                                    val slice = minOf(sleepRemaining, 30L)
                                                    delay(slice)
                                                    sleepRemaining -= slice
                                                }
                                                val elapsedNow = SystemClock.elapsedRealtime() - windowStart
                                                if (elapsedNow >= 1000L || activeBandwidthLimitBps.get() == TURBO_UNLIMITED_BPS) {
                                                    windowBytes = 0L
                                                    windowStart = SystemClock.elapsedRealtime()
                                                }
                                            }

                                            val now = System.currentTimeMillis()
                                            if (now - lastEmit >= 120L || (totalBytes > 0L && downloadedBytes >= totalBytes)) {
                                                lastEmit = now
                                                val fraction = if (totalBytes > 0L) {
                                                    (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                                                } else {
                                                    null
                                                }
                                                emitTransferState(onStateChanged, fraction, downloadedBytes, totalBytes)
                                            }
                                        }
                                        output.flush()
                                    }
                                }
                            }
                        } catch (ce: CancellationException) {
                            throw ce
                        } catch (ioe: java.io.IOException) {
                            val errName = ioe.javaClass.simpleName
                            val errMsg = ioe.message.orEmpty()
                            if (activeClient !== http1DownloadClient &&
                                (errName.contains("StreamReset", ignoreCase = true) ||
                                    errName.contains("Protocol", ignoreCase = true) ||
                                    errMsg.contains("stream", ignoreCase = true) ||
                                    errMsg.contains("PROTOCOL_ERROR", ignoreCase = true) ||
                                    errMsg.contains("HTTP_1_1_REQUIRED", ignoreCase = true))
                            ) {
                                activeClient = http1DownloadClient
                            }

                            var networkUp = isNetworkAvailable(context)
                            if (!networkUp && transientIoRetriesRemaining > 0) {
                                delay(1_200L)
                                currentCoroutineContext().ensureActive()
                                networkUp = isNetworkAvailable(context)
                            }
                            if (transientIoRetriesRemaining > 0 && networkUp) {
                                transientIoRetriesRemaining--
                                retryAttemptCount++
                                attemptResume = partFile.exists() && partFile.length() > 0L
                                shouldContinueTransferLoop = true
                                delay((retryAttemptCount * 1_000L).coerceAtMost(5_000L))
                            } else {
                                throw ioe
                            }
                        }
                    } while (shouldContinueTransferLoop)
                }

                onStateChanged(ApkDownloadState.Verifying)
                val effectiveExpectedSize = expectedTotalBytes.takeIf { it > 0L }
                    ?: context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getLong(KEY_PREF_TOTAL_BYTES, 0L)

                val check = verifyApkArchiveAndHash(
                    context = context,
                    apkFile = partFile,
                    expectedTotalBytes = effectiveExpectedSize,
                    expectedSha256 = expectedSha256
                )
                if (check.isValid) {
                    if (finalApk.exists()) finalApk.delete()
                    if (!partFile.renameTo(finalApk)) {
                        partFile.copyTo(finalApk, overwrite = true)
                        partFile.delete()
                    }

                    pendingInstallApk = finalApk
                    onStateChanged(
                        ApkDownloadState.ReadyToInstall(
                            apkFile = finalApk,
                            needsInstallPermission = false,
                            preloadedReady = !autoInstallOnComplete.get(),
                            verifiedSha256 = check.computedSha256
                        )
                    )
                    return@withContext Result.success(finalApk)
                }

                partFile.delete()
                if (startedWithPartial && cleanVerificationRetriesRemaining > 0 && isNetworkAvailable(context)) {
                    // Self-heal if a resumed partial chunk came from a stale build/CDN mismatch
                    cleanVerificationRetriesRemaining--
                    continue
                }
                val msg = check.failureReason ?: "Downloaded APK failed integrity check."
                onStateChanged(ApkDownloadState.Failed(msg))
                return@withContext Result.failure<File>(Exception(msg))
            }
            @Suppress("UNREACHABLE_CODE")
            Result.failure<File>(Exception("Update download interrupted — tap to resume."))
        } catch (ce: CancellationException) {
            // Preserve `.part` file across pause/cancellation so HTTP Range resumes cleanly!
            throw ce
        } catch (t: Throwable) {
            if (silentPreload) {
                Log.w(TAG, "Silent APK preload paused (`.part` kept for resume): ${t.message}")
                AegisUpdateResumeJobService.schedule(context)
            } else {
                Log.e(TAG, "Failed to download APK (`.part` kept for resume)", t)
            }
            val msg = formatFriendlyUpdateError(
                context = context,
                err = t,
                fallback = "Update download interrupted — tap to resume."
            )
            onStateChanged(ApkDownloadState.Failed(msg))
            return@withContext Result.failure<File>(Exception(msg, t))
        }
    }

    private fun emitTransferState(
        onStateChanged: (ApkDownloadState) -> Unit,
        progress: Float?,
        bytesDownloaded: Long,
        totalBytes: Long
    ) {
        val limit = activeBandwidthLimitBps.get()
        if (limit == TURBO_UNLIMITED_BPS) {
            onStateChanged(ApkDownloadState.Downloading(progress, bytesDownloaded, totalBytes))
        } else {
            onStateChanged(ApkDownloadState.Preloading(progress, bytesDownloaded, totalBytes, limit))
        }
    }

    internal fun computeThrottleDelayMs(
        bytesInWindow: Long,
        elapsedMs: Long,
        limitBps: Long
    ): Long {
        if (limitBps <= 0L || limitBps == TURBO_UNLIMITED_BPS || bytesInWindow <= 0L) return 0L
        val targetMs = (bytesInWindow * 1000L) / limitBps
        return (targetMs - elapsedMs.coerceAtLeast(0L)).coerceIn(0L, 450L)
    }

    internal fun isBatteryEligibleForSilentPreload(batteryPercent: Int, isCharging: Boolean): Boolean {
        if (isCharging) return true
        return batteryPercent >= MIN_BATTERY_FOR_PRELOAD
    }

    internal fun resolveTotalBytes(
        contentRangeHeader: String?,
        bodyContentLength: Long,
        existingOffset: Long,
        fallbackExpected: Long
    ): Long {
        if (!contentRangeHeader.isNullOrBlank()) {
            val slashTotal = contentRangeHeader.substringAfterLast('/', "").trim().toLongOrNull()
            if (slashTotal != null && slashTotal > 0L) return slashTotal
        }
        if (bodyContentLength > 0L) return existingOffset + bodyContentLength
        return fallbackExpected.coerceAtLeast(0L)
    }

    internal fun verifyApkFileAndHash(
        apkFile: File,
        expectedTotalBytes: Long = 0L,
        expectedSha256: String? = null
    ): IntegrityCheckResult {
        if (!apkFile.exists() || apkFile.length() < 10_000L) {
            return IntegrityCheckResult(false, failureReason = "APK file is incomplete or too small.")
        }
        if (expectedTotalBytes > 0L && apkFile.length() != expectedTotalBytes) {
            return IntegrityCheckResult(
                false,
                failureReason = "APK size mismatch (${apkFile.length()} vs $expectedTotalBytes bytes)."
            )
        }
        val sha = computeSha256Hex(apkFile)
        if (!expectedSha256.isNullOrBlank() && !sha.equals(expectedSha256.trim(), ignoreCase = true)) {
            return IntegrityCheckResult(
                false,
                computedSha256 = sha,
                failureReason = "APK SHA-256 verification failed. Re-download required."
            )
        }
        return IntegrityCheckResult(true, computedSha256 = sha)
    }

    fun verifyApkArchiveAndHash(
        context: Context,
        apkFile: File,
        expectedTotalBytes: Long = 0L,
        expectedSha256: String? = null
    ): IntegrityCheckResult {
        val baseCheck = verifyApkFileAndHash(apkFile, expectedTotalBytes, expectedSha256)
        if (!baseCheck.isValid) return baseCheck

        return try {
            val pm = context.packageManager
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_SIGNATURES
            }
            @Suppress("DEPRECATION")
            val archiveInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, flags)
            if (archiveInfo == null) {
                return IntegrityCheckResult(
                    false,
                    computedSha256 = baseCheck.computedSha256,
                    failureReason = "Corrupted APK archive header."
                )
            }
            if (archiveInfo.packageName != context.packageName) {
                return IntegrityCheckResult(
                    false,
                    computedSha256 = baseCheck.computedSha256,
                    failureReason = "APK package ID mismatch (${archiveInfo.packageName})."
                )
            }
            val installedInfo = try {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, flags)
            } catch (_: Exception) {
                null
            }
            if (installedInfo != null) {
                val archiveVerCode = extractVersionCodeCompat(archiveInfo)
                val installedVerCode = extractVersionCodeCompat(installedInfo)
                if (archiveVerCode > 0L && installedVerCode > 0L && archiveVerCode < installedVerCode) {
                    return IntegrityCheckResult(
                        false,
                        computedSha256 = baseCheck.computedSha256,
                        failureReason = "APK versionCode ($archiveVerCode) is older than installed ($installedVerCode)."
                    )
                }
            }
            baseCheck
        } catch (t: Throwable) {
            Log.w(TAG, "Package archive validation warning: ${t.message}")
            baseCheck
        }
    }

    internal fun haveCompatibleSigningCerts(
        installedCertSha256s: Set<String>,
        archiveCertSha256s: Set<String>
    ): Boolean {
        if (installedCertSha256s.isEmpty() || archiveCertSha256s.isEmpty()) return true
        val cleanInstalled = installedCertSha256s.map { it.replace(":", "").lowercase(java.util.Locale.ROOT).trim() }
        val cleanArchive = archiveCertSha256s.map { it.replace(":", "").lowercase(java.util.Locale.ROOT).trim() }
        return cleanInstalled.any { it in cleanArchive }
    }

    private fun extractPackageSigningSha256s(pkgInfo: PackageInfo): Set<String> {
        val collector = mutableListOf<Signature>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = pkgInfo.signingInfo
            if (signingInfo != null) {
                signingInfo.apkContentsSigners?.let { collector.addAll(it) }
                signingInfo.signingCertificateHistory?.let { collector.addAll(it) }
            }
        }
        @Suppress("DEPRECATION")
        pkgInfo.signatures?.let { collector.addAll(it) }
        if (collector.isEmpty()) return emptySet()
        return collector.mapNotNull { sig ->
            try {
                val digest = MessageDigest.getInstance("SHA-256").digest(sig.toByteArray())
                digest.joinToString("") { "%02x".format(it) }
            } catch (_: Throwable) {
                null
            }
        }.toSet()
    }

    private fun extractVersionCodeCompat(pkgInfo: PackageInfo): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pkgInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            pkgInfo.versionCode.toLong()
        }
    }

    private fun checkSigningConflictBeforeInstall(context: Context, apkFile: File): String? {
        return try {
            val pm = context.packageManager
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_SIGNATURES
            }
            @Suppress("DEPRECATION")
            val archiveInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, flags) ?: return null
            archiveInfo.applicationInfo?.sourceDir = apkFile.absolutePath
            archiveInfo.applicationInfo?.publicSourceDir = apkFile.absolutePath

            @Suppress("DEPRECATION")
            val installedInfo = pm.getPackageInfo(context.packageName, flags)
            val installedSigs = extractPackageSigningSha256s(installedInfo)
            val archiveSigs = extractPackageSigningSha256s(archiveInfo)
            if (!haveCompatibleSigningCerts(installedSigs, archiveSigs)) {
                return "Signing certificate mismatch with installed build. Saved APK to Downloads — tap Clean Upgrade below."
            }
            val archiveCode = extractVersionCodeCompat(archiveInfo)
            val installedCode = extractVersionCodeCompat(installedInfo)
            if (archiveCode > 0L && installedCode > 0L && archiveCode < installedCode) {
                return "Newer build ($installedCode) is already installed compared to release APK ($archiveCode)."
            }
            null
        } catch (t: Throwable) {
            Log.w(TAG, "Pre-install signature check skipped: ${t.message}")
            null
        }
    }

    private fun exportApkToPublicDownloads(context: Context, apkFile: File): String? {
        return try {
            val fileName = apkFile.name.ifBlank { "Sheildlenz-Update.apk" }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/vnd.android.package-archive")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
                resolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(apkFile).use { input ->
                        input.copyTo(out)
                    }
                }
                "Downloads/$fileName"
            } else {
                val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (publicDir != null) {
                    if (!publicDir.exists()) publicDir.mkdirs()
                    val dest = File(publicDir, fileName)
                    apkFile.copyTo(dest, overwrite = true)
                    "Downloads/$fileName"
                } else {
                    null
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Could not export APK to public Downloads: ${t.message}")
            null
        }
    }

    internal fun computeSha256Hex(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(32 * 1024)
        FileInputStream(file).use { input ->
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun readBatteryStatus(context: Context): Pair<Int, Boolean> {
        val sticky = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = sticky?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = sticky?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val status = sticky?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val plugged = sticky?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL ||
            plugged != 0
        val pct = if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt().coerceIn(0, 100) else 100
        return pct to isCharging
    }

    private fun getUpdatesDir(context: Context): File {
        // Use app-internal filesDir ("updates") mapped by <files-path> in file_paths.xml
        // so Scoped Storage or external volumes never block FileProvider APK installs.
        return File(context.filesDir, "updates").apply { if (!exists()) mkdirs() }
    }

    internal fun getPartApkFile(context: Context, cleanVersion: String): File =
        File(getUpdatesDir(context), "Sheildlenz-v$cleanVersion.apk.part")

    internal fun getFinalApkFile(context: Context, cleanVersion: String): File =
        File(getUpdatesDir(context), "Sheildlenz-v$cleanVersion.apk")

    private fun cleanupOldVersionArtifacts(context: Context, activeVersion: String) {
        val dir = getUpdatesDir(context)
        val keepPart = "Sheildlenz-v$activeVersion.apk.part"
        val keepApk = "Sheildlenz-v$activeVersion.apk"
        dir.listFiles()?.forEach { f ->
            if (f.name.startsWith("Sheildlenz-") && f.name != keepPart && f.name != keepApk) {
                f.delete()
            }
        }
        // Clean up any older APKs from legacy external downloads directory
        try {
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.listFiles()?.forEach { f ->
                if (f.name.startsWith("Sheildlenz-")) f.delete()
            }
        } catch (_: Exception) {
        }
    }

    private fun persistTransferMetadata(
        context: Context,
        version: String,
        url: String,
        totalBytes: Long,
        sha256: String?
    ) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_PREF_VERSION, version)
            .putString(KEY_PREF_URL, url)
            .apply {
                if (totalBytes > 0L) putLong(KEY_PREF_TOTAL_BYTES, totalBytes)
                if (!sha256.isNullOrBlank()) putString(KEY_PREF_SHA256, sha256) else remove(KEY_PREF_SHA256)
            }
            .apply()
    }

    private fun persistTotalBytes(context: Context, totalBytes: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putLong(KEY_PREF_TOTAL_BYTES, totalBytes)
            .apply()
    }

    private fun clearPersistedTransferState(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
        AegisUpdateResumeJobService.cancel(context)
    }

    fun autoInstallApk(context: Context, apkFile: File): Boolean {
        rememberUiActivity(context)
        val appContext = context.applicationContext
        pendingInstallApk = apkFile

        val conflictReason = checkSigningConflictBeforeInstall(appContext, apkFile)
        if (conflictReason != null) {
            onPackageInstallConflict(appContext, conflictReason)
            return false
        }

        // Standard Intent.ACTION_VIEW via FileProvider guarantees 100% crash-free install
        // without process termination from PackageInstaller session.commit()
        return installApk(resolveLaunchContext(context), apkFile)
    }

    fun onPackageInstallConflict(context: Context, customReason: String? = null) {
        val appContext = context.applicationContext
        val file = pendingInstallApk
            ?: (_apkDownloadState.value as? ApkDownloadState.ReadyToInstall)?.apkFile
        if (file == null || !file.exists()) {
            _apkDownloadState.value = ApkDownloadState.Failed(
                customReason ?: "Existing app uses a different signing key. Uninstall the old build once to finish upgrading."
            )
            return
        }
        val exported = exportApkToPublicDownloads(appContext, file)
        AegisBackgroundDownloadService.stop(appContext)
        _apkDownloadState.value = ApkDownloadState.SignatureConflict(
            apkFile = file,
            savedPublicPath = exported,
            reason = customReason
                ?: "Existing Sheildlenz build uses an older signing key. APK saved to ${exported ?: "Downloads"} — tap Clean Upgrade once."
        )
    }

    fun triggerCleanUninstallForUpgrade(context: Context) {
        rememberUiActivity(context)
        val appContext = context.applicationContext
        val launchCtx = resolveLaunchContext(context)
        val conflict = _apkDownloadState.value as? ApkDownloadState.SignatureConflict
        val file = conflict?.apkFile ?: pendingInstallApk
        if (file != null && file.exists() && conflict?.savedPublicPath == null) {
            exportApkToPublicDownloads(appContext, file)
        }
        try {
            AegisContainer.getInstance(appContext).preferences.grantTemporarySettingsAuthorization()
            val uninstallIntent = Intent(Intent.ACTION_DELETE, Uri.parse("package:${appContext.packageName}")).apply {
                if (launchCtx !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            launchCtx.startActivity(uninstallIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch clean uninstall", e)
        }
    }

    fun retryPendingInstallIfAuthorized(context: Context) {
        rememberUiActivity(context)
        val state = _apkDownloadState.value
        val file = pendingInstallApk ?: (state as? ApkDownloadState.ReadyToInstall)?.apkFile ?: return
        if (!file.exists()) return
        if (state is ApkDownloadState.ReadyToInstall && state.needsInstallPermission) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()) {
                autoInstallApk(context, file)
            }
        }
    }

    fun fallbackViewInstallIfPending(context: Context): Boolean {
        val file = pendingInstallApk ?: return false
        if (!file.exists()) return false
        return installApk(context, file)
    }

    fun onPackageInstallSucceeded(context: Context) {
        pendingInstallApk = null
        clearPersistedTransferState(context)
        AegisBackgroundDownloadService.stop(context)
        _apkDownloadState.value = ApkDownloadState.Installed
    }

    fun onPackageInstallFailed(message: String) {
        _apkDownloadState.value = ApkDownloadState.Failed(message)
    }

    internal fun resolveNonBrowserInstallerPackage(pm: PackageManager, intent: Intent): String? {
        return try {
            @Suppress("DEPRECATION")
            val candidates = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            val browserHints = listOf(
                "chrome", "browser", "firefox", "mozilla", "opera", "brave", "edge", "emmx", "duckduckgo", "sbrowser", "webview"
            )
            val nonBrowserCandidates = candidates
                .mapNotNull { it.activityInfo?.packageName }
                .filter { pkg -> browserHints.none { hint -> pkg.contains(hint, ignoreCase = true) } }
            nonBrowserCandidates.firstOrNull { pkg ->
                pkg.contains("packageinstaller", ignoreCase = true)
            } ?: nonBrowserCandidates.firstOrNull()
        } catch (_: Throwable) {
            null
        }
    }

    fun installApk(context: Context, apkFile: File): Boolean {
        rememberUiActivity(context)
        val appContext = context.applicationContext
        val launchCtx = resolveLaunchContext(context)
        pendingInstallApk = apkFile
        return try {
            try {
                AegisContainer.getInstance(appContext).preferences.grantTemporarySettingsAuthorization()
            } catch (_: Throwable) {
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !appContext.packageManager.canRequestPackageInstalls()) {
                val prev = _apkDownloadState.value as? ApkDownloadState.ReadyToInstall
                _apkDownloadState.value = ApkDownloadState.ReadyToInstall(
                    apkFile = apkFile,
                    needsInstallPermission = true,
                    preloadedReady = prev?.preloadedReady ?: false,
                    verifiedSha256 = prev?.verifiedSha256
                )
                AegisBackgroundDownloadService.stop(appContext)
                val permIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${appContext.packageName}")
                    if (launchCtx !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                launchCtx.startActivity(permIntent)
                return false
            }

            val apkUri = FileProvider.getUriForFile(
                appContext,
                "${appContext.packageName}.fileprovider",
                apkFile
            )
            val pm = appContext.packageManager

            @Suppress("DEPRECATION")
            val installPkgIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                data = apkUri
                clipData = ClipData.newRawUri("Sheildlenz Update", apkUri)
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, appContext.packageName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (launchCtx !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }.also { intent ->
                resolveNonBrowserInstallerPackage(pm, intent)?.let { pkg ->
                    intent.setPackage(pkg)
                }
            }

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                clipData = ClipData.newRawUri("Sheildlenz Update", apkUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (launchCtx !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }.also { intent ->
                resolveNonBrowserInstallerPackage(pm, intent)?.let { pkg ->
                    intent.setPackage(pkg)
                }
            }

            // On Android 7-9 (API 24-28), ACTION_INSTALL_PACKAGE targets the native PackageInstaller directly
            // without browser/file-viewer disambiguation; on Android 10+ (API 29+), prefer non-browser ACTION_VIEW first.
            val primaryIntent = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) installPkgIntent else viewIntent
            val secondaryIntent = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) viewIntent else installPkgIntent

            val launched = try {
                launchCtx.startActivity(primaryIntent)
                true
            } catch (primaryErr: Throwable) {
                Log.w(TAG, "Primary installer intent fallback: ${primaryErr.message}")
                launchCtx.startActivity(secondaryIntent)
                true
            }
            AegisBackgroundDownloadService.stop(appContext)
            launched
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to launch APK installer", e)
            val exported = exportApkToPublicDownloads(appContext, apkFile)
            AegisBackgroundDownloadService.stop(appContext)
            _apkDownloadState.value = ApkDownloadState.Failed(
                if (exported != null) {
                    "APK ready in $exported. Tap Update to retry install."
                } else {
                    "Unable to open package installer. Tap Update to retry."
                }
            )
            false
        }
    }

    private data class HttpResponseSnapshot(val code: Int, val body: String?)

    private fun fetchReleaseJson(urlString: String): HttpResponseSnapshot {
        val request = Request.Builder()
            .url(urlString)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "Sheildlenz-Android-App")
            .header("Cache-Control", "no-cache")
            .header("Pragma", "no-cache")
            .build()

        client.newCall(request).execute().use { response ->
            return HttpResponseSnapshot(code = response.code, body = response.body?.string())
        }
    }

    internal fun compareVersions(latest: String, current: String): Int {
        return try {
            val cleanLatest = latest.substringBefore("-").trim()
            val cleanCurrent = current.substringBefore("-").trim()
            val latestParts = cleanLatest.split(".")
            val currentParts = cleanCurrent.split(".")
            val maxLength = maxOf(latestParts.size, currentParts.size)
            for (i in 0 until maxLength) {
                val lVal = latestParts.getOrNull(i)?.toIntOrNull() ?: 0
                val cVal = currentParts.getOrNull(i)?.toIntOrNull() ?: 0
                if (lVal != cVal) {
                    return lVal.compareTo(cVal)
                }
            }
            0
        } catch (_: Exception) {
            latest.compareTo(current)
        }
    }

    private fun formatProgressLabel(downloaded: Long, total: Long): String {
        return if (total > 0L) {
            "${formatBytesMb(downloaded)} / ${formatBytesMb(total)}"
        } else {
            formatBytesMb(downloaded)
        }
    }

    fun formatBytesMb(bytes: Long): String {
        if (bytes <= 0L) return "0.0 MB"
        return String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    }
}
