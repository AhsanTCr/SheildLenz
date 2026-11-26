package com.example.services.protection

import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.example.MainActivity
import com.example.core.alerts.DiscreetAlertFormatter
import com.example.data.preferences.QuickTileMode
import com.example.di.AegisContainer
import com.example.services.dns.AegisSafeDnsVpnService

class AegisQuickTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        refreshTileState()
    }

    override fun onClick() {
        super.onClick()
        val container = AegisContainer.getInstance(applicationContext)
        val prefs = container.preferences
        val discreet = prefs.discreetBlockingScreenMode.value

        if (!prefs.masterPowerEnabled.value) {
            openMainActivity()
            refreshTileState()
            return
        }

        when (prefs.quickTileMode.value) {
            QuickTileMode.SAFE_DNS -> {
                val isDnsActive = AegisSafeDnsVpnService.isTunnelActive.value || prefs.safeDnsEnabled.value
                if (!isDnsActive) {
                    val consentIntent = runCatching { VpnService.prepare(this) }.getOrNull()
                    if (consentIntent == null) {
                        prefs.setSafeDnsEnabled(true)
                        AegisSafeDnsVpnService.start(this)
                        Toast.makeText(
                            applicationContext,
                            DiscreetAlertFormatter.quickTileFeedback(discreet, "Safe DNS", true),
                            Toast.LENGTH_SHORT
                        ).show()
                        refreshTileState()
                    } else {
                        openMainActivity()
                    }
                } else {
                    val guardianAuthorized = !prefs.hasGuardianPin.value || prefs.isGuardianSessionUnlocked.value
                    if (guardianAuthorized) {
                        prefs.setSafeDnsEnabled(false)
                        AegisSafeDnsVpnService.stop(this)
                        Toast.makeText(
                            applicationContext,
                            DiscreetAlertFormatter.quickTileFeedback(discreet, "Safe DNS", false),
                            Toast.LENGTH_SHORT
                        ).show()
                        refreshTileState()
                    } else {
                        openMainActivity()
                    }
                }
            }

            QuickTileMode.STUDY_LOCK -> {
                val isStudyActive = prefs.isStudySessionActive()
                if (!isStudyActive) {
                    prefs.startStudySession(prefs.studyDurationMinutes.value)
                    Toast.makeText(
                        applicationContext,
                        DiscreetAlertFormatter.quickTileFeedback(discreet, "Study Lock", true),
                        Toast.LENGTH_SHORT
                    ).show()
                    refreshTileState()
                } else {
                    val guardianAuthorized = !prefs.hasGuardianPin.value || prefs.isGuardianSessionUnlocked.value
                    if (guardianAuthorized) {
                        prefs.stopStudySession()
                        Toast.makeText(
                            applicationContext,
                            DiscreetAlertFormatter.quickTileFeedback(discreet, "Study Lock", false),
                            Toast.LENGTH_SHORT
                        ).show()
                        refreshTileState()
                    } else {
                        openMainActivity()
                    }
                }
            }
        }
    }

    private fun openMainActivity() {
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
        val prefs = container.preferences
        val isMasterOn = prefs.masterPowerEnabled.value
        val isShieldActive = isMasterOn && container.protectionRepository.telemetry.value.isProtectionEnabled
        val isDnsActive = isMasterOn && AegisSafeDnsVpnService.isTunnelActive.value
        val isStudyActive = isMasterOn && prefs.isStudySessionActive()
        val anyGuardActive = isShieldActive || isDnsActive || isStudyActive

        tile.state = if (anyGuardActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "Sheildlenz"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = when {
                !isMasterOn -> "Power Off"
                isShieldActive && isDnsActive -> "Shield + DNS"
                isStudyActive -> "Study Lock"
                isDnsActive -> "Safe DNS"
                isShieldActive -> "Shield Armed"
                prefs.quickTileMode.value == QuickTileMode.STUDY_LOCK -> "Tap: Study"
                else -> "Tap: Safe DNS"
            }
        }
        tile.updateTile()
    }
}
