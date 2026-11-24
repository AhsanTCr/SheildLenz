package com.example.security.deviceowner

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.UserManager
import android.util.Log
import com.example.di.AegisContainer
import com.example.monitoring.accessibility.AegisAccessibilityService

class AegisDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.i(TAG, "Sheildlenz Device Administrator privilege enabled.")
        applyDeviceOwnerHardeningIfEligible(context)
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        val prefs = AegisContainer.getInstance(context).preferences
        if (!prefs.masterPowerEnabled.value) {
            return "Sheildlenz Master Power is currently OFF. Device Administrator can be deactivated."
        }
        val childLocked = prefs.isChildUninstallBlocked()
        val authorized = prefs.isGuardianAuthorizedForSystemSettings() && !prefs.isLockdownActive() && !childLocked
        if (!authorized) {
            Log.w(TAG, "Unauthorized attempt to disable Aegis Device Administrator blocked (childLocked=$childLocked).")
            AegisAccessibilityService.performEmergencyHomeAction()
            AegisContainer.getInstance(context).overlayController.showTamperLockOverlay(
                reason = if (childLocked) {
                    "Child Protection Lock: Only your paired Parent can authorize deactivating Device Administrator or uninstalling Sheildlenz."
                } else {
                    "Unauthorized attempt to deactivate Device Administrator was intercepted. Guardian Password is required."
                }
            )
        }
        return if (childLocked) {
            "SHEILDLENZ CHILD GUARD: Only your paired Parent can authorize uninstalling or deactivating Sheildlenz."
        } else {
            "SHEILDLENZ ANTI-TAMPER PROTECTION: Guardian Password verification is required before Device Administrator can be deactivated."
        }
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.w(TAG, "Sheildlenz Device Administrator privilege disabled.")
    }

    companion object {
        private const val TAG = "AegisDeviceAdmin"

        fun getComponentName(context: Context): ComponentName {
            return ComponentName(context.applicationContext, AegisDeviceAdminReceiver::class.java)
        }

        fun createActivateAdminIntent(context: Context): Intent {
            return Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, getComponentName(context))
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Activates anti-tamper protection and requires Guardian Password before Sheildlenz administration can be disabled or uninstalled."
                )
            }
        }

        fun isDeviceOwner(context: Context): Boolean {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
                ?: return false
            return dpm.isDeviceOwnerApp(context.packageName)
        }

        fun isDeviceAdmin(context: Context): Boolean {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
                ?: return false
            return dpm.isAdminActive(getComponentName(context))
        }

        fun applyDeviceOwnerHardeningIfEligible(context: Context) {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager ?: return
            if (!dpm.isDeviceOwnerApp(context.packageName)) return
            val admin = getComponentName(context)
            try {
                val prefs = AegisContainer.getInstance(context).preferences
                val shouldBlockUninstall = !prefs.isParentApprovedUninstallActive()
                dpm.setUninstallBlocked(admin, context.packageName, shouldBlockUninstall)
                dpm.addUserRestriction(admin, UserManager.DISALLOW_SAFE_BOOT)
                Log.i(TAG, "Applied Device Owner uninstall lock ($shouldBlockUninstall) and Safe Boot restriction.")
            } catch (e: Exception) {
                Log.e(TAG, "Error applying Device Owner policies", e)
            }
        }

        fun setChildUninstallBlockedIfDeviceOwner(context: Context, blocked: Boolean) {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager ?: return
            if (!dpm.isDeviceOwnerApp(context.packageName)) return
            try {
                dpm.setUninstallBlocked(getComponentName(context), context.packageName, blocked)
                Log.i(TAG, "Updated Device Owner uninstallBlocked=$blocked")
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling Device Owner uninstall lock", e)
            }
        }

        /**
         * Strict Device Owner Mode:
         * Suspends an offending package using DevicePolicyManager.setPackagesSuspended().
         * Only executable if the application is genuinely provisioned as Device Owner via ADB/NFC.
         */
        fun suspendPackageIfDeviceOwner(context: Context, packageName: String): Boolean {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
                ?: return false

            if (!dpm.isDeviceOwnerApp(context.packageName)) {
                Log.d(TAG, "Cannot suspend package: Sheildlenz is not provisioned as Device Owner.")
                return false
            }

            if (packageName == context.packageName ||
                packageName == "android" ||
                packageName == "com.android.systemui" ||
                packageName == "com.android.settings"
            ) {
                Log.w(TAG, "Exempted critical package from suspension: $packageName")
                return false
            }

            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    val result = dpm.setPackagesSuspended(getComponentName(context), arrayOf(packageName), true)
                    val suspended = result != null && result.isEmpty()
                    Log.i(TAG, "Suspended package $packageName: $suspended")
                    suspended
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to suspend package $packageName", e)
                    false
                }
            } else {
                false
            }
        }
    }
}
