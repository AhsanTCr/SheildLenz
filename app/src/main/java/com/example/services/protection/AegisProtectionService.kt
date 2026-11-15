package com.example.services.protection

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.core.models.SecurityIncident
import com.example.core.update.UpdateChecker
import com.example.data.preferences.AegisPreferences
import com.example.data.repository.ProtectionRepository
import com.example.detection.classifier.ClassificationResult
import com.example.detection.classifier.TFLiteContentClassifier
import com.example.detection.decision.DecisionOutcome
import com.example.detection.decision.ThreatDecisionEngine
import com.example.detection.preprocessing.FramePreprocessor
import com.example.di.AegisContainer
import com.example.monitoring.accessibility.AegisAccessibilityService
import com.example.security.deviceowner.AegisDeviceAdminReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class AegisProtectionService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val container by lazy { AegisContainer.getInstance(this) }
    private val overlayController get() = container.overlayController

    private var mediaProjection: MediaProjection? = null
    private var activeProjectionCallback: MediaProjection.Callback? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private lateinit var framePreprocessor: FramePreprocessor
    private lateinit var contentClassifier: TFLiteContentClassifier
    private lateinit var decisionEngine: ThreatDecisionEngine

    private var lastInspectionTimeMs: Long = 0L
    private var lastTelemetryEmitMs: Long = 0L

    private var frameCounter: Long = 0L
    private var skippedCounter: Long = 0L
    private var fpsCounter: Int = 0
    private var lastFpsSampleTime: Long = 0L
    private var consecutiveIdleOrSafeTicks: Int = 0
    private var seenContextChangeUptimeMs: Long = 0L
    private var lastPublishedEffectiveIntervalMs: Int = 350

    @Volatile
    private var deferredBitmap: Bitmap? = null

    @Volatile
    private var isStopped = false

    @Volatile
    private var userRequestedStop = false

    @Volatile
    private var isScreenInteractive: Boolean = true

    @Volatile
    private var batteryPercent: Int = 100

    @Volatile
    private var isCharging: Boolean = false

    @Volatile
    private var isPowerSaveMode: Boolean = false

    @Volatile
    private var isLowBatteryThrottled: Boolean = false

    private var isPowerReceiverRegistered = false
    private val powerManager by lazy { getSystemService(Context.POWER_SERVICE) as? PowerManager }

    private val trailingScanRunnable = Runnable {
        if (isStopped) return@Runnable
        val bitmap = deferredBitmap ?: return@Runnable
        deferredBitmap = null
        if (bitmap.isRecycled) return@Runnable

        val prefs = container.preferences
        if (prefs.pauseWhenScreenOff.value && (!isScreenInteractive || powerManager?.isInteractive == false)) {
            return@Runnable
        }
        if (prefs.isLockdownActive() && overlayController.isOverlayVisible) {
            return@Runnable
        }
        if (!contentClassifier.isModelReady) {
            return@Runnable
        }

        val activePkg = resolveTrackedForegroundPackage()
        if ((!activePkg.isNullOrBlank() && prefs.isPackageWhitelisted(activePkg)) ||
            isZeroCostSkipPackage(activePkg, packageName)
        ) {
            return@Runnable
        }

        val now = SystemClock.uptimeMillis()
        lastInspectionTimeMs = now
        inspectBitmap(
            bitmap = bitmap,
            nowUptime = now,
            forceEvaluate = decisionEngine.isAwaitingConfirmation()
        )
    }

    private val powerAndScreenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenInteractive = false
                    cancelTrailingScan()
                    if (container.preferences.pauseWhenScreenOff.value) {
                        container.protectionRepository.recordInspectionMetrics(
                            frameCount = frameCounter,
                            fps = 0.0f,
                            skippedCount = skippedCounter,
                            effectiveIntervalMs = lastPublishedEffectiveIntervalMs
                        )
                    }
                }
                Intent.ACTION_SCREEN_ON -> {
                    isScreenInteractive = true
                    consecutiveIdleOrSafeTicks = 0
                    lastInspectionTimeMs = 0L
                }
                Intent.ACTION_BATTERY_CHANGED -> {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
                    if (level >= 0) {
                        batteryPercent = ((level * 100) / scale).coerceIn(0, 100)
                    }
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
                    syncPowerStateToRepo()
                    UpdateChecker.onBatteryStateChanged(
                        context = this@AegisProtectionService,
                        batteryPercent = batteryPercent,
                        isCharging = isCharging
                    )
                }
                PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                    isPowerSaveMode = powerManager?.isPowerSaveMode == true
                    syncPowerStateToRepo()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "AegisProtectionService created (Eco Engine + Multi-Region Chrome Protection).")
        createNotificationChannel()

        contentClassifier = TFLiteContentClassifier(this, container.modelManager)
        framePreprocessor = FramePreprocessor(
            inputWidth = contentClassifier.inputWidth,
            inputHeight = contentClassifier.inputHeight,
            isQuantizedUInt8 = contentClassifier.isQuantizedInput
        )
        decisionEngine = ThreatDecisionEngine()

        val thread = HandlerThread("AegisFrameProcessor", Process.THREAD_PRIORITY_BACKGROUND).apply {
            start()
        }
        backgroundThread = thread
        backgroundHandler = Handler(thread.looper)

        isScreenInteractive = powerManager?.isInteractive != false
        isPowerSaveMode = powerManager?.isPowerSaveMode == true
        val (pct, charging) = ProtectionRepository.readBatterySnapshot(this)
        batteryPercent = pct
        isCharging = charging
        registerPowerReceiver()
        syncPowerStateToRepo()
        UpdateChecker.onBatteryStateChanged(
            context = this,
            batteryPercent = batteryPercent,
            isCharging = isCharging
        )

        container.refreshModelTelemetry()
        container.protectionRepository.setServiceRunning(true)

        serviceScope.launch {
            container.modelManager.modelVersion.drop(1).collect {
                backgroundHandler?.post {
                    val reloaded = contentClassifier.reloadModel()
                    if (reloaded) {
                        framePreprocessor.configure(
                            width = contentClassifier.inputWidth,
                            height = contentClassifier.inputHeight,
                            quantizedUInt8 = contentClassifier.isQuantizedInput
                        )
                    }
                    decisionEngine.reset()
                    consecutiveIdleOrSafeTicks = 0
                    cancelTrailingScan()
                    container.refreshModelTelemetry()
                    val statusLabel = if (reloaded) {
                        "Sheildlenz Active • Chrome & All Apps Armed"
                    } else {
                        "Chrome Web Guard Active • Load .tflite for Vision"
                    }
                    val nm = getSystemService(NotificationManager::class.java)
                    nm?.notify(NOTIFICATION_ID, buildForegroundNotification(statusLabel))
                    Log.i(TAG, "Service reloaded TFLite model cleanly. Ready=$reloaded")
                }
            }
        }

        serviceScope.launch {
            container.preferences.captureWidthPx.drop(1).collect { newWidth ->
                backgroundHandler?.post {
                    if (!isStopped && mediaProjection != null) {
                        resizeVirtualDisplayCapture(newWidth.coerceIn(224, 1080))
                    }
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        backgroundHandler?.post {
            if (!isStopped && mediaProjection != null) {
                cancelTrailingScan()
                resizeVirtualDisplayCapture(container.preferences.captureWidthPx.value.coerceIn(224, 1080))
            }
        }
    }

    private fun registerPowerReceiver() {
        if (isPowerReceiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        }
        registerReceiver(powerAndScreenReceiver, filter)
        isPowerReceiverRegistered = true
    }

    private fun syncPowerStateToRepo() {
        val prefs = container.preferences
        val (effectiveMs, lowBatt) = AegisPreferences.computeEffectiveScanIntervalMs(
            baseIntervalMs = prefs.scanIntervalMs.value,
            adaptiveEnabled = prefs.adaptiveCadenceEnabled.value,
            consecutiveIdleOrSafeCount = consecutiveIdleOrSafeTicks,
            autoThrottleLowBattery = prefs.autoThrottleLowBattery.value,
            batteryPercent = batteryPercent,
            isCharging = isCharging,
            isPowerSaveMode = isPowerSaveMode,
            wasLowBatteryThrottled = isLowBatteryThrottled
        )
        isLowBatteryThrottled = lowBatt
        lastPublishedEffectiveIntervalMs = effectiveMs
        container.protectionRepository.updateBatteryPowerState(
            batteryPercent = batteryPercent,
            isCharging = isCharging,
            isPowerSaveMode = isPowerSaveMode,
            isLowBatteryThrottled = lowBatt,
            effectiveIntervalMs = effectiveMs
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY

        when (intent.action) {
            ACTION_START_PROTECTION -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                @Suppress("DEPRECATION")
                val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA) as? Intent
                }

                if (resultCode != 0 && resultData != null) {
                    userRequestedStop = false
                    isStopped = false
                    container.preferences.setShieldDesiredActive(true)
                    getSystemService(NotificationManager::class.java)?.cancel(PAUSED_NOTIFICATION_ID)

                    val statusLabel = if (contentClassifier.isModelReady) {
                        "Sheildlenz Active • Chrome & All Apps Armed"
                    } else {
                        "Chrome Web Guard Active • Load .tflite for Vision"
                    }
                    val notification = buildForegroundNotification(statusLabel)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
                    } else {
                        startForeground(NOTIFICATION_ID, notification)
                    }
                    startScreenCapture(resultCode, resultData)
                } else {
                    Log.e(TAG, "Missing MediaProjection consent token data.")
                    stopSelf()
                }
            }

            ACTION_STOP_PROTECTION -> {
                userRequestedStop = true
                stopProtection()
            }
        }

        return START_STICKY
    }

    private fun computeCaptureDimensions(targetShortSidePx: Int): Triple<Int, Int, Int> {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val screenWidth: Int
        val screenHeight: Int
        val densityDpi: Int

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.maximumWindowMetrics.bounds
            screenWidth = bounds.width().coerceAtLeast(1)
            screenHeight = bounds.height().coerceAtLeast(1)
            densityDpi = resources.configuration.densityDpi.takeIf { it > 0 } ?: DisplayMetrics.DENSITY_DEFAULT
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
            screenWidth = metrics.widthPixels.coerceAtLeast(1)
            screenHeight = metrics.heightPixels.coerceAtLeast(1)
            densityDpi = metrics.densityDpi.takeIf { it > 0 } ?: DisplayMetrics.DENSITY_DEFAULT
        }

        return computeCaptureDimensionsForBounds(
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            densityDpi = densityDpi,
            targetShortSidePx = targetShortSidePx
        )
    }

    private fun attachImageReaderListener(reader: ImageReader) {
        reader.setOnImageAvailableListener({ ir ->
            val image = try {
                ir.acquireLatestImage()
            } catch (_: Exception) {
                null
            } ?: return@setOnImageAvailableListener

            handleIncomingImage(image)
        }, backgroundHandler)
    }

    private fun startScreenCapture(resultCode: Int, resultData: Intent) {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
            ?: return

        releaseCaptureHardware()

        val projection = projectionManager.getMediaProjection(resultCode, resultData)
        if (projection == null) {
            Log.e(TAG, "MediaProjection token retrieval failed.")
            stopSelf()
            return
        }
        mediaProjection = projection

        val callback = object : MediaProjection.Callback() {
            override fun onCapturedContentResize(width: Int, height: Int) {
                if (width > 0 && height > 0 && !isStopped) {
                    resizeVirtualDisplayCaptureForAspect(width, height)
                }
            }

            override fun onStop() {
                Log.w(TAG, "MediaProjection stopped by system or user.")
                if (!userRequestedStop &&
                    container.preferences.isShieldDesiredActive.value &&
                    !container.preferences.isLockdownActive()
                ) {
                    postPausedRearmNotification()
                }
                stopProtection()
            }
        }
        activeProjectionCallback = callback
        projection.registerCallback(callback, backgroundHandler)

        val (captureWidth, captureHeight, densityDpi) =
            computeCaptureDimensions(container.preferences.captureWidthPx.value)

        val reader = ImageReader.newInstance(captureWidth, captureHeight, PixelFormat.RGBA_8888, 2)
        imageReader = reader
        attachImageReaderListener(reader)

        virtualDisplay = projection.createVirtualDisplay(
            "AegisProtectionStream",
            captureWidth,
            captureHeight,
            densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,
            null,
            backgroundHandler
        )

        lastInspectionTimeMs = 0L
        consecutiveIdleOrSafeTicks = 0
        Log.i(TAG, "VirtualDisplay engaged: ${captureWidth}x${captureHeight} @ ${densityDpi}dpi")
        container.protectionRepository.setServiceRunning(true)
        container.protectionRepository.updateMediaProjectionState(true)
    }

    private fun resizeVirtualDisplayCapture(newShortSidePx: Int) {
        val (captureWidth, captureHeight, densityDpi) = computeCaptureDimensions(newShortSidePx)
        applyVirtualDisplayResize(captureWidth, captureHeight, densityDpi)
    }

    private fun resizeVirtualDisplayCaptureForAspect(contentWidth: Int, contentHeight: Int) {
        val densityDpi = resources.configuration.densityDpi.takeIf { it > 0 } ?: DisplayMetrics.DENSITY_DEFAULT
        val (captureWidth, captureHeight, dpi) = computeCaptureDimensionsForBounds(
            screenWidth = contentWidth,
            screenHeight = contentHeight,
            densityDpi = densityDpi,
            targetShortSidePx = container.preferences.captureWidthPx.value
        )
        applyVirtualDisplayResize(captureWidth, captureHeight, dpi)
    }

    private fun applyVirtualDisplayResize(captureWidth: Int, captureHeight: Int, densityDpi: Int) {
        val vd = virtualDisplay ?: return
        val oldReader = imageReader
        try {
            if (oldReader != null && oldReader.width == captureWidth && oldReader.height == captureHeight) {
                return
            }
            val newReader = ImageReader.newInstance(captureWidth, captureHeight, PixelFormat.RGBA_8888, 2)
            attachImageReaderListener(newReader)
            vd.resize(captureWidth, captureHeight, densityDpi)
            vd.surface = newReader.surface
            imageReader = newReader
            try {
                oldReader?.close()
            } catch (_: Exception) {
            }
            framePreprocessor.reset()
            Log.i(TAG, "Resized VirtualDisplay capture cleanly to ${captureWidth}x${captureHeight}")
        } catch (e: Exception) {
            Log.w(TAG, "VirtualDisplay resize skipped: ${e.message}")
        }
    }

    private fun resolveTrackedForegroundPackage(): String? {
        if (!AegisAccessibilityService.isServiceRunning()) {
            return null
        }
        return container.protectionRepository.telemetry.value.activeMonitoredPackage
    }

    private fun handleIncomingImage(image: Image) {
        val prefs = container.preferences

        // Gate 1: Screen-Off true sleep
        if (prefs.pauseWhenScreenOff.value && (!isScreenInteractive || powerManager?.isInteractive == false)) {
            image.close()
            cancelTrailingScan()
            return
        }

        // Gate 2: Lockdown overlay active
        if (prefs.isLockdownActive() && overlayController.isOverlayVisible) {
            image.close()
            cancelTrailingScan()
            return
        }

        // Gate 3: Wake cadence immediately when Accessibility sees app/window switch
        val latestContextChange = container.protectionRepository.lastContextChangeUptimeMs
        if (latestContextChange != seenContextChangeUptimeMs) {
            seenContextChangeUptimeMs = latestContextChange
            consecutiveIdleOrSafeTicks = 0
            deferredBitmap = null
        }

        val (effectiveIntervalMs, lowBatt) = AegisPreferences.computeEffectiveScanIntervalMs(
            baseIntervalMs = prefs.scanIntervalMs.value,
            adaptiveEnabled = prefs.adaptiveCadenceEnabled.value,
            consecutiveIdleOrSafeCount = consecutiveIdleOrSafeTicks,
            autoThrottleLowBattery = prefs.autoThrottleLowBattery.value,
            batteryPercent = batteryPercent,
            isCharging = isCharging,
            isPowerSaveMode = isPowerSaveMode,
            wasLowBatteryThrottled = isLowBatteryThrottled
        )
        if (lowBatt != isLowBatteryThrottled) {
            isLowBatteryThrottled = lowBatt
            container.protectionRepository.updateBatteryPowerState(
                batteryPercent = batteryPercent,
                isCharging = isCharging,
                isPowerSaveMode = isPowerSaveMode,
                isLowBatteryThrottled = lowBatt,
                effectiveIntervalMs = effectiveIntervalMs
            )
        }
        lastPublishedEffectiveIntervalMs = effectiveIntervalMs

        // Gate 4: Skip pixel copy & AI inference if .tflite model isn't loaded
        if (!contentClassifier.isModelReady) {
            image.close()
            cancelTrailingScan()
            return
        }

        // Gate 5: Whitelisted apps or launcher/self skip BEFORE pixel copy
        val activePkg = resolveTrackedForegroundPackage()
        if ((!activePkg.isNullOrBlank() && prefs.isPackageWhitelisted(activePkg)) ||
            isZeroCostSkipPackage(activePkg, packageName)
        ) {
            image.close()
            cancelTrailingScan()
            skippedCounter++
            consecutiveIdleOrSafeTicks = (consecutiveIdleOrSafeTicks + 1).coerceAtMost(30)
            emitThrottledTelemetry(SystemClock.uptimeMillis(), currentFpsOverride = 0.0f)
            return
        }

        val currentTime = SystemClock.uptimeMillis()
        val elapsed = currentTime - lastInspectionTimeMs
        if (elapsed < effectiveIntervalMs.toLong()) {
            // Retain latest settled frame (e.g. Chrome finishing page render during throttle window)
            // so a trailing-edge scan catches static browser pages reliably.
            val extracted = try {
                framePreprocessor.extractBitmapFromImage(image)
            } catch (_: Exception) {
                null
            } finally {
                image.close()
            }
            if (extracted != null) {
                val delayMs = (effectiveIntervalMs.toLong() - elapsed).coerceIn(35L, 2000L)
                scheduleTrailingScan(extracted, delayMs)
            }
            return
        }

        cancelTrailingScan()
        lastInspectionTimeMs = currentTime

        val bitmap = try {
            framePreprocessor.extractBitmapFromImage(image)
        } catch (e: Exception) {
            Log.e(TAG, "Frame extraction error", e)
            null
        } finally {
            image.close()
        }

        if (bitmap != null) {
            inspectBitmap(bitmap = bitmap, nowUptime = currentTime, forceEvaluate = false)
        }
    }

    private fun scheduleTrailingScan(sourceBitmap: Bitmap, delayMs: Long) {
        val handler = backgroundHandler ?: return
        try {
            val retained = framePreprocessor.retainDeferredBitmap(sourceBitmap)
            deferredBitmap = retained
            handler.removeCallbacks(trailingScanRunnable)
            handler.postDelayed(trailingScanRunnable, delayMs.coerceAtLeast(35L))
        } catch (e: Exception) {
            Log.w(TAG, "Unable to retain deferred frame: ${e.message}")
        }
    }

    private fun cancelTrailingScan() {
        deferredBitmap = null
        backgroundHandler?.removeCallbacks(trailingScanRunnable)
    }

    private fun inspectBitmap(bitmap: Bitmap, nowUptime: Long, forceEvaluate: Boolean) {
        try {
            val prefs = container.preferences
            val awaitingConfirmation = decisionEngine.isAwaitingConfirmation()

            // Duplicate frame skip — bypassed when awaiting confirmation or running trailing confirmation check
            if (!forceEvaluate && !awaitingConfirmation && prefs.skipDuplicateFrames.value &&
                framePreprocessor.isFrameDuplicate(bitmap)
            ) {
                skippedCounter++
                consecutiveIdleOrSafeTicks = (consecutiveIdleOrSafeTicks + 1).coerceAtMost(30)
                emitThrottledTelemetry(nowUptime)
                return
            }

            consecutiveIdleOrSafeTicks = 0
            frameCounter++
            fpsCounter++
            emitThrottledTelemetry(nowUptime)

            framePreprocessor.configure(
                width = contentClassifier.inputWidth,
                height = contentClassifier.inputHeight,
                quantizedUInt8 = contentClassifier.isQuantizedInput
            )

            val sensitivity = prefs.sensitivity.value
            val isStrictEverything = prefs.strictEverything.value
            val isMaintenance = prefs.maintenanceMode.value
            val useCustomThresholds = prefs.customThresholdsEnabled.value
            val explicitOverride = if (useCustomThresholds) prefs.customExplicitThreshold.value else null
            val suggestiveOverride = if (useCustomThresholds) prefs.customSuggestiveThreshold.value else null
            val consecutiveRequired = prefs.consecutiveFramesRequired.value

            val explicitThreshold = explicitOverride ?: sensitivity.threshold
            val suggestiveThreshold = suggestiveOverride ?: sensitivity.threshold

            val regions = FramePreprocessor.computeInspectionRegions(bitmap.width, bitmap.height)
            var worstClassification: ClassificationResult? = null

            if (regions.isEmpty()) {
                val tb = framePreprocessor.preprocessBitmapToTensorBuffer(bitmap, null)
                val res = contentClassifier.classifyTensorBuffer(tb)
                if (res.isModelLoaded) worstClassification = res
            } else {
                for (region in regions) {
                    val tb = framePreprocessor.preprocessBitmapToTensorBuffer(bitmap, region)
                    val res = contentClassifier.classifyTensorBuffer(tb)
                    if (!res.isModelLoaded) return

                    worstClassification = selectHigherRiskClassification(worstClassification, res)

                    // Short-circuit immediately if this crop already triggers an immediate block
                    if (!isMaintenance) {
                        val immediateExplicit = res.explicitScore >= explicitThreshold ||
                            (isStrictEverything && res.explicitScore >= 0.50f)
                        val suggestiveHit = res.suggestiveScore >= suggestiveThreshold ||
                            (isStrictEverything && res.suggestiveScore >= 0.55f)
                        val immediateSuggestive = suggestiveHit &&
                            (isStrictEverything || res.suggestiveScore >= 0.85f || consecutiveRequired <= 1)
                        if (immediateExplicit || immediateSuggestive) {
                            break
                        }
                    }
                }
            }

            val finalClassification = worstClassification ?: return
            if (!finalClassification.isModelLoaded) return

            if (!isMaintenance) {
                val decision = decisionEngine.evaluate(
                    result = finalClassification,
                    sensitivity = sensitivity,
                    isStrictEverything = isStrictEverything,
                    explicitThresholdOverride = explicitOverride,
                    suggestiveThresholdOverride = suggestiveOverride,
                    consecutiveFramesRequired = consecutiveRequired
                )
                if (decision.shouldBlock) {
                    cancelTrailingScan()
                    handleThreatDetected(finalClassification, decision)
                } else if (decisionEngine.isAwaitingConfirmation()) {
                    // Schedule confirmation check so static suggestive webpages confirm cleanly without stalling
                    val confirmDelay = prefs.scanIntervalMs.value.toLong().coerceIn(150L, 1200L)
                    scheduleTrailingScan(bitmap, confirmDelay)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Frame processing error", e)
        }
    }

    private fun selectHigherRiskClassification(
        current: ClassificationResult?,
        candidate: ClassificationResult
    ): ClassificationResult {
        if (current == null) return candidate
        val currentRisk = maxOf(current.explicitScore * 1.25f, current.suggestiveScore)
        val candidateRisk = maxOf(candidate.explicitScore * 1.25f, candidate.suggestiveScore)
        return if (candidateRisk > currentRisk) candidate else current
    }

    private fun emitThrottledTelemetry(nowUptime: Long, currentFpsOverride: Float? = null) {
        if (lastFpsSampleTime == 0L) {
            lastFpsSampleTime = nowUptime
        }
        if (nowUptime - lastTelemetryEmitMs < 1000L) return

        val elapsedMs = (nowUptime - lastFpsSampleTime).coerceAtLeast(1L)
        val computedFps = currentFpsOverride ?: ((fpsCounter * 1000f) / elapsedMs)
        fpsCounter = 0
        lastFpsSampleTime = nowUptime
        lastTelemetryEmitMs = nowUptime

        container.protectionRepository.recordInspectionMetrics(
            frameCount = frameCounter,
            fps = computedFps,
            skippedCount = skippedCounter,
            effectiveIntervalMs = lastPublishedEffectiveIntervalMs
        )
    }

    private fun handleThreatDetected(classification: ClassificationResult, decision: DecisionOutcome) {
        val rawPkg = resolveTrackedForegroundPackage()
            ?.takeIf { it.isNotBlank() && !isZeroCostSkipPackage(it, packageName) }
        val targetPkg = rawPkg ?: ""
        val targetAppName = if (rawPkg != null) {
            container.protectionRepository.activeAppLabel.value
        } else {
            "Foreground App"
        }
        val rawScores = if (classification.rawClassScores.isNotEmpty()) {
            classification.rawClassScores.entries.joinToString(", ") { (k, v) -> "$k: ${(v * 100).toInt()}%" }
        } else {
            "Safe: ${(classification.safeScore * 100).toInt()}%, Suggestive: ${(classification.suggestiveScore * 100).toInt()}%, Explicit: ${(classification.explicitScore * 100).toInt()}%"
        }
        val fullDetails = "${decision.reason}\nModel Breakdown: $rawScores (Inference ${classification.latencyMs}ms)"

        Log.w(TAG, "THREAT CONFIRMED: ${decision.category.name} in $targetAppName ($targetPkg). Executing eviction & full-device lockdown.")

        val wentHome = if (container.preferences.evictAppOnThreat.value) {
            AegisAccessibilityService.closeAndEvictPackage(this, targetPkg)
        } else {
            AegisAccessibilityService.performEmergencyHomeAction()
        }
        if (!wentHome) {
            launchHomeFallback()
        }
        val suspended = if (targetPkg.isNotBlank()) {
            AegisDeviceAdminReceiver.suspendPackageIfDeviceOwner(this, targetPkg)
        } else {
            false
        }

        container.preferences.engageFullDeviceLockdown(
            appName = targetAppName,
            packageName = targetPkg,
            categoryName = decision.category.displayName,
            details = fullDetails
        )
        overlayController.showLockdownOverlay(container.preferences.lockdownIncidentInfo.value)
        AegisLockdownService.start(this)

        val totalMinutes = container.preferences.lockdownDurationMinutes.value
        val durationLabel = AegisPreferences.formatMinutesLabel(totalMinutes)
        val actionSummary = buildString {
            append(if (container.preferences.evictAppOnThreat.value) "App Evicted & Background Killed" else "Screen Shielded")
            if (suspended) append(" + Package Suspended")
            append(" • $durationLabel Device Lock")
        }

        serviceScope.launch {
            val incident = SecurityIncident(
                timestampMillis = System.currentTimeMillis(),
                packageName = targetPkg.ifBlank { "foreground.screen" },
                appDisplayName = targetAppName,
                category = decision.category,
                severity = decision.severity,
                confidenceScore = decision.confidence,
                actionTaken = actionSummary,
                diagnosticSummary = fullDetails,
                lockdownDurationMinutes = totalMinutes
            )
            container.securityHistoryRepository.logIncident(incident)
            container.protectionRepository.recordDetection(decision.category)
        }
    }

    private fun launchHomeFallback() {
        try {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            }
            startActivity(homeIntent)
        } catch (e: Exception) {
            Log.w(TAG, "Fallback HOME intent failed", e)
        }
    }

    private fun releaseCaptureHardware() {
        cancelTrailingScan()
        try {
            virtualDisplay?.release()
        } catch (_: Exception) {
        }
        virtualDisplay = null

        try {
            imageReader?.close()
        } catch (_: Exception) {
        }
        imageReader = null

        val projection = mediaProjection
        val cb = activeProjectionCallback
        activeProjectionCallback = null
        mediaProjection = null
        if (projection != null) {
            if (cb != null) {
                try {
                    projection.unregisterCallback(cb)
                } catch (_: Exception) {
                }
            }
            try {
                projection.stop()
            } catch (_: Exception) {
            }
        }
    }

    private fun cleanupResources() {
        if (isStopped) return
        isStopped = true
        Log.i(TAG, "Cleaning up AegisProtectionService resources.")

        if (isPowerReceiverRegistered) {
            try {
                unregisterReceiver(powerAndScreenReceiver)
            } catch (_: Exception) {
            }
            isPowerReceiverRegistered = false
        }

        releaseCaptureHardware()

        if (!container.preferences.isLockdownActive()) {
            overlayController.dismissBlockOverlay()
        }
        contentClassifier.close()
        decisionEngine.reset()
        framePreprocessor.recyclePools()

        container.protectionRepository.updateMediaProjectionState(false)
        container.protectionRepository.setServiceRunning(false)
    }

    private fun stopProtection() {
        cleanupResources()
        @Suppress("DEPRECATION")
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanupResources()
        backgroundThread?.quitSafely()
        backgroundThread = null
        serviceScope.cancel()
        Log.i(TAG, "AegisProtectionService destroyed.")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sheildlenz Protection Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors screen frames for inappropriate content and provides protection status"
                setShowBadge(false)
            }
            nm?.createNotificationChannel(channel)

            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Sheildlenz Resume Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts if Android pauses screen capture so visual protection can be resumed in one tap"
            }
            nm?.createNotificationChannel(alertChannel)
        }
    }

    private fun postPausedRearmNotification() {
        try {
            val rearmIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(MainActivity.EXTRA_REARM_SHIELD, true)
            }
            val pendingIntent = PendingIntent.getActivity(
                this,
                PAUSED_NOTIFICATION_ID,
                rearmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )
            val note = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Visual Screen Capture Paused")
                .setContentText("Tap to resume AI screen scanning across Chrome & apps. Web URL Guard stays active.")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
            getSystemService(NotificationManager::class.java)?.notify(PAUSED_NOTIFICATION_ID, note)
        } catch (e: Exception) {
            Log.w(TAG, "Failed posting paused notification", e)
        }
    }

    private fun buildForegroundNotification(contentText: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Sheildlenz")
            .setContentText(contentText)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val TAG = "AegisProtectionService"
        const val CHANNEL_ID = "aegis_protection_channel"
        const val ALERT_CHANNEL_ID = "aegis_protection_alerts"
        const val NOTIFICATION_ID = 1001
        const val PAUSED_NOTIFICATION_ID = 1004

        const val ACTION_START_PROTECTION = "com.example.action.START_PROTECTION"
        const val ACTION_STOP_PROTECTION = "com.example.action.STOP_PROTECTION"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"

        private val SAFE_SYSTEM_PACKAGES = setOf(
            "com.android.systemui",
            "com.android.dialer",
            "com.google.android.dialer",
            "com.android.phone",
            "com.android.incallui",
            "com.android.emergency",
            "com.android.launcher",
            "com.android.launcher3",
            "com.google.android.apps.nexuslauncher",
            "com.sec.android.app.launcher",
            "com.miui.home",
            "com.oneplus.launcher",
            "com.oppo.launcher",
            "com.android.calculator2",
            "com.google.android.calculator",
            "com.android.deskclock",
            "com.google.android.deskclock"
        )

        internal fun isZeroCostSkipPackage(activePackage: String?, ownPackage: String): Boolean {
            if (activePackage.isNullOrBlank()) return false
            val pkg = activePackage.lowercase()
            return pkg == ownPackage.lowercase() || pkg in SAFE_SYSTEM_PACKAGES
        }

        internal fun computeCaptureDimensionsForBounds(
            screenWidth: Int,
            screenHeight: Int,
            densityDpi: Int,
            targetShortSidePx: Int
        ): Triple<Int, Int, Int> {
            val w = screenWidth.coerceAtLeast(1)
            val h = screenHeight.coerceAtLeast(1)
            val shortTarget = targetShortSidePx.coerceIn(224, 1080)
            val (captureWidth, captureHeight) = if (w <= h) {
                val scaledH = (h * (shortTarget.toFloat() / w)).roundToInt().coerceAtLeast(224)
                shortTarget to scaledH
            } else {
                val scaledW = (w * (shortTarget.toFloat() / h)).roundToInt().coerceAtLeast(224)
                scaledW to shortTarget
            }
            return Triple(captureWidth, captureHeight, densityDpi.coerceAtLeast(1))
        }

        fun start(context: Context, resultCode: Int, resultData: Intent) {
            val intent = Intent(context, AegisProtectionService::class.java).apply {
                action = ACTION_START_PROTECTION
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, resultData)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AegisProtectionService::class.java).apply {
                action = ACTION_STOP_PROTECTION
            }
            context.startService(intent)
        }
    }
}
