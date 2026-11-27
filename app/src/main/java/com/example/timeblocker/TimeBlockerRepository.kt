package com.example.timeblocker

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import com.example.timeblocker.data.ProcessedPassNonceEntity
import com.example.timeblocker.data.TimeBlockedAppEntity
import com.example.timeblocker.data.TimeBlockerDao
import com.example.timeblocker.data.TimeBlockerDatabase
import com.example.timeblocker.data.TimeBlockerMetaEntity
import com.example.timeblocker.ui.TimeBlockLockActivity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Standalone, background-ready time-based app blocker repository.
 *
 * Completely isolated from manual/parental toggles in AegisPreferences and AegisDatabase.
 * Persists restricted package names, 1-hour rolling usage windows (5-min base allowance),
 * temporary unlock pass expiries, and monotonic boot-epoch metadata in [TimeBlockerDatabase].
 */
enum class PassBridgeResult {
    GRANTED,
    DUPLICATE_NONCE,
    INVALID_INPUT
}

class TimeBlockerRepository(
    private val appContext: Context,
    private val dao: TimeBlockerDao,
    private val clock: ElapsedClock = ElapsedClock.SYSTEM,
    private val bootCountProvider: BootCountProvider = BootCountProvider.fromContext(appContext),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val tickerIntervalMs: Long = DEFAULT_TICK_INTERVAL_MS
) {
    private val stateLock = Any()
    private val persistenceMutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    private var initialized = false
    private val recordsByPackage = linkedMapOf<String, TimeBlockedAppEntity>()
    private var meta: TimeBlockerMetaEntity = TimeBlockerMetaEntity()

    private var activeForegroundPkg: String? = null
    private var lastForegroundTickElapsedMs: Long = 0L
    private var lastLaunchTriggeredPkg: String? = null
    private var lastLaunchTriggeredElapsedMs: Long = 0L
    private var tickerJob: Job? = null

    @Volatile
    private var extendedUnlockCallback: ((Context, String) -> Unit)? = null

    private val _restrictedApps = MutableStateFlow<List<TimeBlockedAppEntity>>(emptyList())
    val restrictedApps: StateFlow<List<TimeBlockedAppEntity>> = _restrictedApps.asStateFlow()

    private fun ensureInitializedLocked() {
        if (initialized) return
        val loadedApps = runBlocking(ioDispatcher) { dao.getAllApps() }
        val loadedMeta = runBlocking(ioDispatcher) { dao.getMeta() } ?: TimeBlockerMetaEntity()
        recordsByPackage.clear()
        loadedApps.forEach { entity ->
            recordsByPackage[entity.packageName] = entity
        }
        meta = loadedMeta
        initialized = true

        val nowElapsed = clock.elapsedRealtime().coerceAtLeast(1L)
        val bootCount = bootCountProvider.getBootCount()
        if (TimeBlockerEngine.isRebootDetected(meta, recordsByPackage.values, nowElapsed, bootCount)) {
            rebaseAllAfterRebootLocked(nowElapsed, bootCount)
        } else {
            meta = meta.copy(
                lastKnownElapsedMs = maxOf(meta.lastKnownElapsedMs, nowElapsed),
                lastKnownBootCount = if (bootCount >= 0) bootCount else meta.lastKnownBootCount
            )
            scheduleMetaPersist(meta)
        }
        publishRestrictedAppsLocked()
    }

    private fun checkAndHandleRebootLocked(nowElapsed: Long) {
        val bootCount = bootCountProvider.getBootCount()
        if (TimeBlockerEngine.isRebootDetected(meta, recordsByPackage.values, nowElapsed, bootCount)) {
            rebaseAllAfterRebootLocked(nowElapsed, bootCount)
        } else if (nowElapsed > meta.lastKnownElapsedMs) {
            meta = meta.copy(
                lastKnownElapsedMs = nowElapsed,
                lastKnownBootCount = if (bootCount >= 0) bootCount else meta.lastKnownBootCount
            )
        }
    }

    private fun rebaseAllAfterRebootLocked(nowElapsed: Long, currentBootCount: Int) {
        val previousElapsed = meta.lastKnownElapsedMs
        val updatedList = mutableListOf<TimeBlockedAppEntity>()
        recordsByPackage.entries.toList().forEach { (pkg, record) ->
            val rebased = TimeBlockerEngine.rebaseRecordAfterReboot(
                record = record,
                nowElapsedMs = nowElapsed,
                previousBootLastElapsedMs = previousElapsed
            )
            recordsByPackage[pkg] = rebased
            updatedList.add(rebased)
        }
        val nextMeta = TimeBlockerMetaEntity(
            lastKnownElapsedMs = nowElapsed,
            lastKnownBootCount = currentBootCount
        )
        meta = nextMeta
        activeForegroundPkg = null
        lastForegroundTickElapsedMs = 0L
        stopTickerLocked()
        publishRestrictedAppsLocked()

        scope.launch {
            persistenceMutex.withLock {
                if (updatedList.isNotEmpty()) {
                    dao.upsertApps(updatedList)
                }
                dao.upsertMeta(nextMeta)
            }
        }
    }

    private fun publishRestrictedAppsLocked() {
        _restrictedApps.value = recordsByPackage.values
            .filter { it.isRestricted }
            .sortedBy { it.packageName }
    }

    private fun scheduleEntityPersist(entity: TimeBlockedAppEntity, currentMeta: TimeBlockerMetaEntity) {
        scope.launch {
            persistenceMutex.withLock {
                dao.upsertApp(entity)
                dao.upsertMeta(currentMeta)
            }
        }
    }

    private fun scheduleMetaPersist(currentMeta: TimeBlockerMetaEntity) {
        scope.launch {
            persistenceMutex.withLock {
                dao.upsertMeta(currentMeta)
            }
        }
    }

    private fun normalizeAndPersistIfChangedLocked(pkg: String, nowElapsed: Long): TimeBlockedAppEntity? {
        val existing = recordsByPackage[pkg] ?: return null
        val normalized = TimeBlockerEngine.normalizeWindow(existing, nowElapsed)
        if (normalized != existing) {
            recordsByPackage[pkg] = normalized
            publishRestrictedAppsLocked()
            scheduleEntityPersist(normalized, meta)
        }
        return normalized
    }

    /**
     * Adds an app package to the isolated time-blocker list with an automatic
     * 5-minute (300,000 ms) base allowance per 1-hour rolling window.
     */
    fun addTimeBlockedApp(packageName: String) {
        addOrUpdateTimeBlockedApp(
            packageName = packageName,
            allowanceMinutes = (TimeBlockerEngine.BASE_ALLOWANCE_MS / 60_000L).toInt(),
            windowMinutes = (TimeBlockerEngine.HOURLY_WINDOW_MS / 60_000L).toInt()
        )
    }

    /**
     * Adds or updates an app package with configurable allowance (minutes) and
     * rolling reset window (minutes or hours converted to minutes).
     */
    fun addOrUpdateTimeBlockedApp(
        packageName: String,
        allowanceMinutes: Int,
        windowMinutes: Int
    ) {
        val cleanPkg = packageName.trim()
        if (cleanPkg.isBlank() || cleanPkg == appContext.packageName) return

        val safeAllowanceMins = allowanceMinutes.coerceIn(1, 180)
        val safeWindowMins = windowMinutes.coerceIn(safeAllowanceMins, 24 * 60)
        val allowanceMs = safeAllowanceMins * 60_000L
        val windowMs = safeWindowMins * 60_000L

        synchronized(stateLock) {
            ensureInitializedLocked()
            val nowElapsed = clock.elapsedRealtime().coerceAtLeast(1L)
            checkAndHandleRebootLocked(nowElapsed)

            val existing = recordsByPackage[cleanPkg]
            val entity = if (existing != null) {
                val normalized = TimeBlockerEngine.normalizeWindow(existing, nowElapsed)
                normalized.copy(
                    isRestricted = true,
                    allowanceMs = allowanceMs,
                    windowMs = windowMs
                )
            } else {
                TimeBlockedAppEntity(
                    packageName = cleanPkg,
                    isRestricted = true,
                    windowStartElapsedMs = nowElapsed,
                    usedMsInWindow = 0L,
                    passExpiresAtElapsedMs = 0L,
                    addedAtElapsedMs = nowElapsed,
                    allowanceMs = allowanceMs,
                    windowMs = windowMs
                )
            }
            recordsByPackage[cleanPkg] = entity
            meta = meta.copy(lastKnownElapsedMs = nowElapsed)
            publishRestrictedAppsLocked()
            scheduleEntityPersist(entity, meta)
        }
    }

    fun updateAllAppsSchedule(allowanceMinutes: Int, windowMinutes: Int) {
        val safeAllowanceMins = allowanceMinutes.coerceIn(1, 180)
        val safeWindowMins = windowMinutes.coerceIn(safeAllowanceMins, 24 * 60)
        val allowanceMs = safeAllowanceMins * 60_000L
        val windowMs = safeWindowMins * 60_000L

        synchronized(stateLock) {
            ensureInitializedLocked()
            val nowElapsed = clock.elapsedRealtime().coerceAtLeast(1L)
            checkAndHandleRebootLocked(nowElapsed)
            val updatedList = mutableListOf<TimeBlockedAppEntity>()
            recordsByPackage.entries.toList().forEach { (pkg, record) ->
                if (record.isRestricted) {
                    val updated = TimeBlockerEngine.normalizeWindow(record, nowElapsed).copy(
                        allowanceMs = allowanceMs,
                        windowMs = windowMs
                    )
                    recordsByPackage[pkg] = updated
                    updatedList.add(updated)
                }
            }
            meta = meta.copy(lastKnownElapsedMs = nowElapsed)
            publishRestrictedAppsLocked()
            val currentMeta = meta
            scope.launch {
                persistenceMutex.withLock {
                    if (updatedList.isNotEmpty()) {
                        dao.upsertApps(updatedList)
                    }
                    dao.upsertMeta(currentMeta)
                }
            }
        }
    }

    fun resetAppWindowNow(packageName: String) {
        val cleanPkg = packageName.trim()
        if (cleanPkg.isBlank()) return
        synchronized(stateLock) {
            ensureInitializedLocked()
            val nowElapsed = clock.elapsedRealtime().coerceAtLeast(1L)
            val existing = recordsByPackage[cleanPkg] ?: return
            val updated = existing.copy(
                windowStartElapsedMs = nowElapsed,
                usedMsInWindow = 0L,
                passExpiresAtElapsedMs = 0L
            )
            recordsByPackage[cleanPkg] = updated
            meta = meta.copy(lastKnownElapsedMs = nowElapsed)
            publishRestrictedAppsLocked()
            scheduleEntityPersist(updated, meta)
        }
    }

    fun getCurrentElapsedRealtime(): Long = clock.elapsedRealtime().coerceAtLeast(1L)

    /**
     * Removes an app package from the time-blocker list.
     */
    fun removeTimeBlockedApp(packageName: String) {
        val cleanPkg = packageName.trim()
        if (cleanPkg.isBlank()) return

        synchronized(stateLock) {
            ensureInitializedLocked()
            val removed = recordsByPackage.remove(cleanPkg) ?: return
            if (activeForegroundPkg == cleanPkg) {
                activeForegroundPkg = null
                lastForegroundTickElapsedMs = 0L
                stopTickerLocked()
            }
            publishRestrictedAppsLocked()
            val currentMeta = meta
            scope.launch {
                persistenceMutex.withLock {
                    dao.deleteApp(removed.packageName)
                    dao.upsertMeta(currentMeta)
                }
            }
        }
    }

    fun isTimeBlockedApp(packageName: String): Boolean {
        val cleanPkg = packageName.trim()
        if (cleanPkg.isBlank()) return false
        synchronized(stateLock) {
            ensureInitializedLocked()
            return recordsByPackage[cleanPkg]?.isRestricted == true
        }
    }

    fun hasAnyRestrictedApps(): Boolean {
        synchronized(stateLock) {
            ensureInitializedLocked()
            return recordsByPackage.values.any { it.isRestricted }
        }
    }

    /**
     * Tier 2 Future Pass Hook (Backend API):
     * Grants a monotonic temporary pass expiring at `elapsedRealtime() + durationMinutes * 60_000L`.
     * While active, bypasses the 5-minute hourly limit without modifying existing manual toggles.
     */
    fun grantTemporaryPass(packageName: String, durationMinutes: Int) {
        val cleanPkg = packageName.trim()
        if (cleanPkg.isBlank()) return

        synchronized(stateLock) {
            ensureInitializedLocked()
            val nowElapsed = clock.elapsedRealtime().coerceAtLeast(1L)
            checkAndHandleRebootLocked(nowElapsed)

            val expiryElapsedMs = if (durationMinutes > 0) {
                nowElapsed + durationMinutes.toLong() * 60_000L
            } else {
                0L
            }
            val existing = recordsByPackage[cleanPkg]
            val updated = if (existing != null) {
                TimeBlockerEngine.normalizeWindow(existing, nowElapsed).copy(
                    passExpiresAtElapsedMs = expiryElapsedMs
                )
            } else {
                TimeBlockedAppEntity(
                    packageName = cleanPkg,
                    isRestricted = false,
                    windowStartElapsedMs = nowElapsed,
                    usedMsInWindow = 0L,
                    passExpiresAtElapsedMs = expiryElapsedMs,
                    addedAtElapsedMs = nowElapsed
                )
            }
            recordsByPackage[cleanPkg] = updated
            meta = meta.copy(lastKnownElapsedMs = nowElapsed)
            publishRestrictedAppsLocked()
            scheduleEntityPersist(updated, meta)
        }
    }

    /**
     * Tier 2 Future Pass Hook (Backend API):
     * Returns true when a valid monotonic temporary pass is active for [packageName].
     */
    fun hasActivePass(packageName: String): Boolean {
        val cleanPkg = packageName.trim()
        if (cleanPkg.isBlank()) return false
        synchronized(stateLock) {
            ensureInitializedLocked()
            val nowElapsed = clock.elapsedRealtime().coerceAtLeast(1L)
            checkAndHandleRebootLocked(nowElapsed)
            val record = normalizeAndPersistIfChangedLocked(cleanPkg, nowElapsed) ?: return false
            return TimeBlockerEngine.hasActivePass(record, nowElapsed)
        }
    }

    /**
     * Replay-protected entry point for [com.example.timeblocker.connector.AppPassConnectorReceiver].
     * Atomically records [nonce] in [TimeBlockerDatabase]; ignores duplicates and grants the
     * monotonic temporary pass when valid.
     */
    fun tryGrantTemporaryPassFromBridge(
        targetPackage: String,
        durationMinutes: Int,
        nonce: String,
        nowWallMs: Long = System.currentTimeMillis()
    ): PassBridgeResult {
        val cleanPkg = targetPackage.trim()
        val cleanNonce = nonce.trim()
        if (cleanPkg.isBlank() ||
            cleanPkg == appContext.packageName ||
            cleanNonce.isBlank() ||
            durationMinutes <= 0
        ) {
            return PassBridgeResult.INVALID_INPUT
        }

        val insertedRowId = runBlocking(ioDispatcher) {
            persistenceMutex.withLock {
                dao.insertNonceIfAbsent(
                    ProcessedPassNonceEntity(
                        nonce = cleanNonce,
                        targetPackage = cleanPkg,
                        recordedAtWallMs = nowWallMs
                    )
                )
            }
        }
        if (insertedRowId == -1L) {
            return PassBridgeResult.DUPLICATE_NONCE
        }

        grantTemporaryPass(cleanPkg, durationMinutes)

        scope.launch {
            persistenceMutex.withLock {
                dao.pruneExpiredNonces(cutoffWallMs = nowWallMs - NONCE_RETENTION_MS)
                dao.pruneExcessNonces(keepCount = MAX_STORED_NONCES)
            }
        }
        return PassBridgeResult.GRANTED
    }

    fun evaluatePackageAccess(packageName: String): TimeBlockDecision {
        val cleanPkg = packageName.trim()
        if (cleanPkg.isBlank()) {
            return TimeBlockerEngine.evaluate(null, clock.elapsedRealtime())
        }
        synchronized(stateLock) {
            ensureInitializedLocked()
            val nowElapsed = clock.elapsedRealtime().coerceAtLeast(1L)
            checkAndHandleRebootLocked(nowElapsed)
            val record = normalizeAndPersistIfChangedLocked(cleanPkg, nowElapsed)
            return TimeBlockerEngine.evaluate(record, nowElapsed)
        }
    }

    fun getRemainingAllowanceMs(packageName: String): Long =
        evaluatePackageAccess(packageName).remainingAllowanceMs

    fun getRemainingResetMs(packageName: String): Long =
        evaluatePackageAccess(packageName).remainingResetMs

    /**
     * Records active foreground usage delta (in milliseconds) for [packageName] using monotonic time
     * and returns the resulting access decision.
     */
    fun recordForegroundUsageDelta(packageName: String, deltaMs: Long): TimeBlockDecision {
        val cleanPkg = packageName.trim()
        if (cleanPkg.isBlank()) {
            return TimeBlockerEngine.evaluate(null, clock.elapsedRealtime())
        }
        synchronized(stateLock) {
            ensureInitializedLocked()
            val nowElapsed = clock.elapsedRealtime().coerceAtLeast(1L)
            checkAndHandleRebootLocked(nowElapsed)
            val existing = recordsByPackage[cleanPkg]
                ?: return TimeBlockerEngine.evaluate(null, nowElapsed)

            if (!existing.isRestricted) {
                return TimeBlockerEngine.evaluate(existing, nowElapsed)
            }

            val updated = TimeBlockerEngine.consumeForegroundUsage(
                record = existing,
                deltaMs = deltaMs.coerceAtLeast(0L),
                nowElapsedMs = nowElapsed
            )
            recordsByPackage[cleanPkg] = updated
            meta = meta.copy(lastKnownElapsedMs = nowElapsed)
            if (updated != existing) {
                publishRestrictedAppsLocked()
                scheduleEntityPersist(updated, meta)
            }
            return TimeBlockerEngine.evaluate(updated, nowElapsed)
        }
    }

    /**
     * Silently called by background foreground detection (e.g., AccessibilityService).
     * Accumulates elapsed foreground time for the prior active restricted package, evaluates access
     * for [packageName], and runs an active 1-second monitor ticker while a restricted app is active.
     */
    fun onForegroundPackageObserved(
        packageName: String,
        onLimitExhausted: ((TimeBlockDecision) -> Unit)? = null
    ): TimeBlockDecision {
        val cleanPkg = packageName.trim()
        if (cleanPkg.isBlank()) {
            return TimeBlockerEngine.evaluate(null, clock.elapsedRealtime())
        }

        val exhaustedDecision: TimeBlockDecision?
        val currentDecision: TimeBlockDecision

        synchronized(stateLock) {
            ensureInitializedLocked()
            val nowElapsed = clock.elapsedRealtime().coerceAtLeast(1L)
            checkAndHandleRebootLocked(nowElapsed)

            val prevPkg = activeForegroundPkg
            val prevTick = lastForegroundTickElapsedMs
            if (!prevPkg.isNullOrBlank() && prevTick > 0L) {
                val delta = (nowElapsed - prevTick).coerceIn(0L, MAX_SINGLE_TICK_DELTA_MS)
                accrueLocked(prevPkg, delta, nowElapsed)
            }

            val record = normalizeAndPersistIfChangedLocked(cleanPkg, nowElapsed)
            if (record != null && record.isRestricted) {
                activeForegroundPkg = cleanPkg
                lastForegroundTickElapsedMs = nowElapsed
                val evaluated = TimeBlockerEngine.evaluate(record, nowElapsed)
                currentDecision = evaluated
                if (evaluated.shouldBlock) {
                    activeForegroundPkg = null
                    lastForegroundTickElapsedMs = 0L
                    stopTickerLocked()
                    exhaustedDecision = evaluated
                } else {
                    startTickerLocked(onLimitExhausted)
                    exhaustedDecision = null
                }
            } else {
                activeForegroundPkg = null
                lastForegroundTickElapsedMs = 0L
                stopTickerLocked()
                currentDecision = TimeBlockerEngine.evaluate(record, nowElapsed)
                exhaustedDecision = null
            }
        }

        if (exhaustedDecision != null) {
            onLimitExhausted?.invoke(exhaustedDecision) ?: launchLockScreen(appContext, exhaustedDecision)
        }
        return currentDecision
    }

    /**
     * Pauses active foreground accumulation when switching to Home, SystemUI, or Sheildlenz itself.
     */
    fun onForegroundLeft() {
        synchronized(stateLock) {
            if (!initialized && recordsByPackage.isEmpty()) return
            val nowElapsed = clock.elapsedRealtime().coerceAtLeast(1L)
            checkAndHandleRebootLocked(nowElapsed)
            val prevPkg = activeForegroundPkg
            val prevTick = lastForegroundTickElapsedMs
            if (!prevPkg.isNullOrBlank() && prevTick > 0L) {
                val delta = (nowElapsed - prevTick).coerceIn(0L, MAX_SINGLE_TICK_DELTA_MS)
                accrueLocked(prevPkg, delta, nowElapsed)
            }
            activeForegroundPkg = null
            lastForegroundTickElapsedMs = 0L
            stopTickerLocked()
        }
    }

    /**
     * Explicit reboot hook invoked by [com.example.services.protection.AegisBootReceiver].
     */
    fun handleDeviceReboot() {
        synchronized(stateLock) {
            ensureInitializedLocked()
            val nowElapsed = clock.elapsedRealtime().coerceAtLeast(1L)
            val bootCount = bootCountProvider.getBootCount()
            rebaseAllAfterRebootLocked(nowElapsed, bootCount)
        }
    }

    /**
     * Clean public hook for wiring the external unlock pass flow in a future update.
     */
    fun setExtendedUnlockHandler(handler: ((Context, String) -> Unit)?) {
        extendedUnlockCallback = handler
    }

    /**
     * Invokes the registered external unlock pass handler (if any) for [packageName].
     * Returns true if an external handler was registered and invoked.
     */
    fun invokeExtendedUnlock(context: Context, packageName: String): Boolean {
        val handler = extendedUnlockCallback ?: return false
        handler.invoke(context, packageName)
        return true
    }

    fun launchLockScreen(context: Context, decision: TimeBlockDecision) {
        if (!decision.shouldBlock || decision.packageName.isBlank()) return
        val nowElapsed = clock.elapsedRealtime().coerceAtLeast(1L)
        synchronized(stateLock) {
            if (lastLaunchTriggeredPkg == decision.packageName &&
                (nowElapsed - lastLaunchTriggeredElapsedMs) < LOCK_RELAUNCH_DEBOUNCE_MS
            ) {
                return
            }
            lastLaunchTriggeredPkg = decision.packageName
            lastLaunchTriggeredElapsedMs = nowElapsed
        }

        runCatching {
            val appLabel = resolveAppLabel(context, decision.packageName)
            val intent = Intent(context, TimeBlockLockActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(TimeBlockLockActivity.EXTRA_PACKAGE_NAME, decision.packageName)
                putExtra(TimeBlockLockActivity.EXTRA_APP_LABEL, appLabel)
                putExtra(TimeBlockLockActivity.EXTRA_REMAINING_RESET_MS, decision.remainingResetMs)
            }
            context.startActivity(intent)
        }.onFailure { err ->
            Log.e(TAG, "Failed launching TimeBlockLockActivity for ${decision.packageName}", err)
        }
    }

    suspend fun awaitPendingPersistence() {
        persistenceMutex.withLock { /* barrier */ }
    }

    private fun accrueLocked(pkg: String, deltaMs: Long, nowElapsed: Long): TimeBlockDecision? {
        val existing = recordsByPackage[pkg] ?: return null
        if (!existing.isRestricted) return null
        val updated = TimeBlockerEngine.consumeForegroundUsage(existing, deltaMs, nowElapsed)
        recordsByPackage[pkg] = updated
        meta = meta.copy(lastKnownElapsedMs = nowElapsed)
        if (updated != existing) {
            publishRestrictedAppsLocked()
            scheduleEntityPersist(updated, meta)
        }
        return TimeBlockerEngine.evaluate(updated, nowElapsed)
    }

    private fun startTickerLocked(onLimitExhausted: ((TimeBlockDecision) -> Unit)?) {
        if (tickerJob?.isActive == true) return
        tickerJob = scope.launch {
            while (isActive) {
                delay(tickerIntervalMs)
                var exhaustedDecision: TimeBlockDecision? = null
                synchronized(stateLock) {
                    val pkg = activeForegroundPkg ?: return@synchronized
                    val nowElapsed = clock.elapsedRealtime().coerceAtLeast(1L)
                    val prevTick = lastForegroundTickElapsedMs
                    val delta = if (prevTick > 0L) (nowElapsed - prevTick).coerceIn(0L, MAX_SINGLE_TICK_DELTA_MS) else 0L
                    lastForegroundTickElapsedMs = nowElapsed
                    val decision = accrueLocked(pkg, delta, nowElapsed)
                    if (decision != null && decision.shouldBlock) {
                        activeForegroundPkg = null
                        lastForegroundTickElapsedMs = 0L
                        exhaustedDecision = decision
                    }
                }
                if (exhaustedDecision != null) {
                    val hit = exhaustedDecision!!
                    onLimitExhausted?.invoke(hit) ?: launchLockScreen(appContext, hit)
                    break
                }
            }
        }
    }

    private fun stopTickerLocked() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun resolveAppLabel(context: Context, packageName: String): String {
        return try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    companion object {
        private const val TAG = "TimeBlockerRepository"
        private const val DEFAULT_TICK_INTERVAL_MS = 1_000L
        private const val MAX_SINGLE_TICK_DELTA_MS = 60_000L
        private const val LOCK_RELAUNCH_DEBOUNCE_MS = 700L
        private const val NONCE_RETENTION_MS = 7L * 24L * 60L * 60L * 1000L
        private const val MAX_STORED_NONCES = 500

        fun create(context: Context): TimeBlockerRepository {
            val appCtx = context.applicationContext
            val db = TimeBlockerDatabase.getInstance(appCtx)
            return TimeBlockerRepository(
                appContext = appCtx,
                dao = db.timeBlockerDao()
            )
        }
    }
}
