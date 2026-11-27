package com.example.timeblocker.connector

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.di.AegisContainer
import com.example.timeblocker.PassBridgeResult
import com.example.timeblocker.TimeBlockerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Signature-protected offline BroadcastReceiver accepting temporary unlock passes
 * from trusted companion apps signed with the same developer certificate
 * (permission: com.ahsantcr.permission.APP_PASS_BRIDGE).
 */
class AppPassConnectorReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_GRANT_APP_PASS) return
        val appContext = context.applicationContext
        val pendingResult = runCatching { goAsync() }.getOrNull()

        if (pendingResult == null) {
            val repo = AegisContainer.getInstance(appContext).timeBlockerRepository
            processPassBroadcast(intent, repo)
            return
        }

        receiverScope.launch {
            try {
                val repo = AegisContainer.getInstance(appContext).timeBlockerRepository
                processPassBroadcast(intent, repo)
            } catch (t: Throwable) {
                Log.e(TAG, "Failed handling companion pass broadcast", t)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "AppPassConnector"
        const val ACTION_GRANT_APP_PASS = "com.ahsan.tcr.sheildlenz.ACTION_GRANT_APP_PASS"
        const val EXTRA_TARGET_PACKAGE = "TARGET_PACKAGE"
        const val EXTRA_DURATION_MINUTES = "DURATION_MINUTES"
        const val EXTRA_NONCE = "NONCE"
        const val DEFAULT_DURATION_MINUTES = 30

        private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        internal fun processPassBroadcast(
            intent: Intent?,
            repository: TimeBlockerRepository
        ): PassBridgeResult {
            if (intent == null || intent.action != ACTION_GRANT_APP_PASS) {
                return PassBridgeResult.INVALID_INPUT
            }

            val targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE)?.trim().orEmpty()
            val nonce = intent.getStringExtra(EXTRA_NONCE)?.trim().orEmpty()
            val durationMinutes = intent.getIntExtra(EXTRA_DURATION_MINUTES, DEFAULT_DURATION_MINUTES)

            if (targetPackage.isBlank() || nonce.isBlank() || durationMinutes <= 0) {
                Log.w(TAG, "Rejected malformed pass broadcast (pkgBlank=${targetPackage.isBlank()}, nonceBlank=${nonce.isBlank()}, duration=$durationMinutes)")
                return PassBridgeResult.INVALID_INPUT
            }

            val result = repository.tryGrantTemporaryPassFromBridge(
                targetPackage = targetPackage,
                durationMinutes = durationMinutes,
                nonce = nonce
            )
            when (result) {
                PassBridgeResult.GRANTED ->
                    Log.i(TAG, "Granted ${durationMinutes}m offline pass to $targetPackage (nonce accepted).")
                PassBridgeResult.DUPLICATE_NONCE ->
                    Log.w(TAG, "Ignored replayed pass broadcast for $targetPackage (duplicate nonce).")
                PassBridgeResult.INVALID_INPUT ->
                    Log.w(TAG, "Ignored invalid pass broadcast for $targetPackage.")
            }
            return result
        }
    }
}
