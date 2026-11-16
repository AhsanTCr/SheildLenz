package com.example.core.permissions

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.VpnService
import android.os.Build
import android.provider.Settings
import com.example.security.deviceowner.AegisDeviceAdminReceiver
import com.example.services.dns.AegisSafeDnsVpnService

enum class DeviceOwnerState {
    OWNER_ACTIVE,
    ADMIN_ACTIVE,
    INACTIVE
}

data class DeviceOwnerDiagnostics(
    val state: DeviceOwnerState,
    val isDeviceOwner: Boolean,
    val isDeviceAdmin: Boolean
)

data class SafeDnsDiagnostics(
    val vpnPermissionGranted: Boolean,
    val isTunnelActive: Boolean,
    val isPrivateDnsOverriding: Boolean,
    val privateDnsServerName: String? = null
)

object SystemDiagnosticsHelper {

    fun inspectDeviceOwnerStatus(context: Context): DeviceOwnerDiagnostics {
        val isOwner = runCatching { AegisDeviceAdminReceiver.isDeviceOwner(context) }.getOrDefault(false)
        val isAdmin = runCatching { AegisDeviceAdminReceiver.isDeviceAdmin(context) }.getOrDefault(false)
        val state = when {
            isOwner -> DeviceOwnerState.OWNER_ACTIVE
            isAdmin -> DeviceOwnerState.ADMIN_ACTIVE
            else -> DeviceOwnerState.INACTIVE
        }
        return DeviceOwnerDiagnostics(
            state = state,
            isDeviceOwner = isOwner,
            isDeviceAdmin = isAdmin
        )
    }

    fun inspectSafeDnsDiagnostics(context: Context): SafeDnsDiagnostics {
        val vpnPermGranted = runCatching { VpnService.prepare(context) == null }.getOrDefault(false)
        val tunnelActive = AegisSafeDnsVpnService.isTunnelActive.value

        var privateDnsActive = false
        var privateDnsHost: String? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                val activeNet = cm?.activeNetwork
                val lp = if (activeNet != null) cm.getLinkProperties(activeNet) else null
                if (lp != null) {
                    privateDnsActive = lp.isPrivateDnsActive
                    privateDnsHost = lp.privateDnsServerName
                }
            }
        }

        val isOverriding = privateDnsActive && !privateDnsHost.isNullOrBlank()
        return SafeDnsDiagnostics(
            vpnPermissionGranted = vpnPermGranted,
            isTunnelActive = tunnelActive,
            isPrivateDnsOverriding = isOverriding,
            privateDnsServerName = privateDnsHost
        )
    }

    fun getNetworkSettingsIntent(): Intent {
        return Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
