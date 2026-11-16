package com.example.di

import android.content.Context
import com.example.blocking.overlay.SystemOverlayController
import com.example.data.local.AegisDatabase
import com.example.data.preferences.AegisPreferences
import com.example.data.repository.ParentChildRepository
import com.example.data.repository.ProtectionRepository
import com.example.data.repository.SecurityHistoryRepository
import com.example.detection.classifier.ModelManager
import com.example.services.protection.ParentChildSyncController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AegisContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: AegisDatabase by lazy {
        AegisDatabase.getInstance(appContext)
    }

    val preferences: AegisPreferences by lazy {
        AegisPreferences(appContext)
    }

    val modelManager: ModelManager by lazy {
        ModelManager(appContext, preferences)
    }

    val protectionRepository: ProtectionRepository by lazy {
        ProtectionRepository(appContext).apply {
            updateModelInfo(modelManager.activeModelInfo.value)
        }
    }

    val securityHistoryRepository: SecurityHistoryRepository by lazy {
        SecurityHistoryRepository(database.incidentDao())
    }

    val blockedDnsLogRepository: com.example.data.repository.BlockedDnsLogRepository by lazy {
        com.example.data.repository.BlockedDnsLogRepository(database.blockedDnsLogDao())
    }

    val overlayController: SystemOverlayController by lazy {
        SystemOverlayController(appContext)
    }

    val parentChildRepository: ParentChildRepository by lazy {
        ParentChildRepository(appContext)
    }

    val parentChildSyncController: ParentChildSyncController by lazy {
        ParentChildSyncController(
            context = appContext,
            repository = parentChildRepository,
            preferences = preferences,
            overlayController = overlayController
        )
    }

    private val _pendingPairingCode = MutableStateFlow<String?>(null)
    val pendingPairingCode: StateFlow<String?> = _pendingPairingCode.asStateFlow()

    fun setPendingPairingCode(code: String?) {
        _pendingPairingCode.value = code?.takeIf { it.isNotBlank() }
    }

    fun refreshModelTelemetry() {
        val info = modelManager.refreshStatus()
        protectionRepository.updateModelInfo(info)
    }

    companion object {
        @Volatile
        private var INSTANCE: AegisContainer? = null

        fun getInstance(context: Context): AegisContainer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AegisContainer(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
