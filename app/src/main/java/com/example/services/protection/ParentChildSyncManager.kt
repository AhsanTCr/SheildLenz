package com.example.services.protection

import android.content.Context
import android.util.Log
import com.example.blocking.overlay.SystemOverlayController
import com.example.data.preferences.AegisPreferences
import com.example.data.repository.ParentChildRepository
import com.example.security.deviceowner.AegisDeviceAdminReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Coordinates real-time Firestore synchronization when the device operates in Child mode:
 * - Observes Parent pairing status & keeps `pairedParentUid` synced
 * - Applies Parent-pushed RemoteSettings onto local `AegisPreferences`
 * - Executes Parent-issued RemoteCommands (`LOCKDOWN`, `UNLOCK`, `ALLOW_UNINSTALL`, `LOCK_UNINSTALL`)
 */
class ParentChildSyncController(
    private val context: Context,
    private val repository: ParentChildRepository,
    private val preferences: AegisPreferences,
    private val overlayController: SystemOverlayController
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var activeChildUid: String? = null
    private var syncJob: Job? = null

    fun ensureStarted() {
        val uid = repository.getCurrentUserId()
        if (uid.isNullOrBlank() || preferences.parentalRole.value != "child") {
            stop()
            return
        }
        if (activeChildUid == uid && syncJob?.isActive == true) {
            return
        }

        stop()
        activeChildUid = uid
        syncJob = scope.launch {
            launch {
                repository.observeChildOwnPairing()
                    .catch { e -> Log.w(TAG, "Pairing observer error: ${e.message}") }
                    .collect { pairing ->
                        if (pairing != null && pairing.active && pairing.parentId.isNotBlank()) {
                            preferences.setPairedParentInfo(
                                parentUid = pairing.parentId,
                                parentName = pairing.parentDisplayName.ifBlank { "Parent Guardian" }
                            )
                            if (pairing.pairingCode.isNotBlank()) {
                                preferences.setChildPairingCode(pairing.pairingCode)
                            }
                        } else if (pairing != null && !pairing.active) {
                            preferences.setPairedParentInfo(null, null)
                        }
                    }
            }

            launch {
                repository.observeRemoteSettings(uid)
                    .catch { e -> Log.w(TAG, "Remote settings observer error: ${e.message}") }
                    .collect { remote ->
                        if (remote != null) {
                            preferences.setSensitivity(remote.sensitivity)
                            preferences.setStrictEverything(remote.strictEverything)
                            preferences.setLockdownDurationMinutes(remote.lockdownDurationMinutes)
                            preferences.setAntiTamperEnabled(remote.antiTamperEnabled)
                            preferences.setChildUninstallLocked(remote.childUninstallLocked)
                            preferences.setLiveSeeEnabled(remote.liveSeeEnabled)
                            if (remote.whitelistedPackages.isNotEmpty()) {
                                preferences.setWhitelistedPackages(remote.whitelistedPackages)
                            }
                            AegisDeviceAdminReceiver.setChildUninstallBlockedIfDeviceOwner(
                                context,
                                preferences.isChildUninstallBlocked()
                            )
                        }
                    }
            }

            launch {
                repository.observeRemoteCommands(uid)
                    .catch { e -> Log.w(TAG, "Remote command observer error: ${e.message}") }
                    .collect { cmd ->
                        if (cmd == null || cmd.timestamp <= 0L) return@collect
                        if (cmd.timestamp <= preferences.lastHandledRemoteCommandTs.value) return@collect
                        preferences.setLastHandledRemoteCommandTs(cmd.timestamp)

                        when (cmd.action) {
                            ParentChildRepository.COMMAND_LOCKDOWN -> {
                                preferences.engageFullDeviceLockdown(
                                    appName = "Parent Guardian",
                                    packageName = "parent.remote.lock",
                                    categoryName = "Parent Remote Lockdown",
                                    details = "Your parent triggered an immediate remote lockdown."
                                )
                                overlayController.showLockdownOverlay(preferences.lockdownIncidentInfo.value)
                                AegisLockdownService.start(context)
                                repository.acknowledgeRemoteCommand(uid)
                            }
                            ParentChildRepository.COMMAND_UNLOCK -> {
                                preferences.clearLockdown()
                                overlayController.dismissBlockOverlay()
                                AegisLockdownService.stop(context)
                                repository.acknowledgeRemoteCommand(uid)
                            }
                            ParentChildRepository.COMMAND_ALLOW_UNINSTALL -> {
                                preferences.setParentApprovedUninstallUntil(
                                    System.currentTimeMillis() + 5 * 60_000L
                                )
                                preferences.setChildUninstallLocked(false)
                                preferences.grantTemporarySettingsAuthorization(5)
                                AegisDeviceAdminReceiver.setChildUninstallBlockedIfDeviceOwner(context, false)
                                repository.acknowledgeRemoteCommand(uid)
                            }
                            ParentChildRepository.COMMAND_LOCK_UNINSTALL -> {
                                preferences.setParentApprovedUninstallUntil(0L)
                                preferences.setChildUninstallLocked(true)
                                AegisDeviceAdminReceiver.setChildUninstallBlockedIfDeviceOwner(context, true)
                                repository.acknowledgeRemoteCommand(uid)
                            }
                        }
                    }
            }
        }
    }

    fun stop() {
        syncJob?.cancel()
        syncJob = null
        activeChildUid = null
    }

    companion object {
        private const val TAG = "ParentChildSync"
    }
}
