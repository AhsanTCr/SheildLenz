package com.example.services.protection

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.MainActivity
import com.example.di.AegisContainer

class AegisQuickTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        refreshTileState()
    }

    override fun onClick() {
        super.onClick()
        refreshTileState()
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private fun refreshTileState() {
        val tile = qsTile ?: return
        val container = AegisContainer.getInstance(applicationContext)
        val isMasterOn = container.preferences.masterPowerEnabled.value
        val isShieldActive = isMasterOn && container.protectionRepository.telemetry.value.isProtectionEnabled
        val isDnsActive = isMasterOn && com.example.services.dns.AegisSafeDnsVpnService.isTunnelActive.value
        val isStudyActive = isMasterOn && container.preferences.isStudySessionActive()
        val anyGuardActive = isShieldActive || isDnsActive || isStudyActive

        tile.state = if (anyGuardActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "Sheildlenz"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = when {
                !isMasterOn -> "Power Off"
                isShieldActive && isDnsActive -> "Shield + DNS"
                isShieldActive -> "Shield Armed"
                isStudyActive -> "Study Lock"
                isDnsActive -> "Safe DNS"
                else -> "Standby"
            }
        }
        tile.updateTile()
    }
}
