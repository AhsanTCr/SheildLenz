package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Centralized Jetpack Credential Manager + Firebase Auth Google Sign-In helper.
 *
 * Uses compile-time `R.string.default_web_client_id` generated from `app/google-services.json`
 * and enforces one credential option per `GetCredentialRequest` for API 36 compatibility.
 */
object GoogleSignInManager {
    private const val TAG = "Auth"

    fun ensureFirebaseInitialized(context: Context): Boolean {
        val appContext = context.applicationContext
        if (FirebaseApp.getApps(appContext).isNotEmpty()) return true
        return runCatching { FirebaseApp.initializeApp(appContext) != null }.getOrDefault(false)
    }

    tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    fun resolveWebClientId(context: Context): String {
        return runCatching {
            context.getString(R.string.default_web_client_id).trim()
        }.getOrElse {
            runCatching {
                context.applicationContext.getString(R.string.default_web_client_id).trim()
            }.getOrDefault("")
        }
    }

    suspend fun attemptAutoSignIn(
        context: Context,
        credentialManager: CredentialManager,
        onAuthSuccess: () -> Unit = {}
    ) {
        if (!ensureFirebaseInitialized(context)) return
        if (runCatching { Firebase.auth.currentUser != null }.getOrDefault(false)) {
            onAuthSuccess()
            return
        }
        val clientId = resolveWebClientId(context)
        if (clientId.isBlank()) return

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        try {
            val activity = context.findActivity() ?: return
            val response = credentialManager.getCredential(activity, request)
            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                Firebase.auth.signInWithCredential(
                    GoogleAuthProvider.getCredential(idToken, null)
                ).await()
                onAuthSuccess()
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Log.d(TAG, "Silent Google auto sign-in skipped: ${e.message}")
        }
    }

    fun signInWithGoogle(
        context: Context,
        credentialManager: CredentialManager? = null,
        scope: CoroutineScope,
        onAuthSuccess: () -> Unit,
        onAuthCancelled: () -> Unit,
        onAuthError: (String) -> Unit
    ) {
        val activity = context.findActivity()
        if (activity == null) {
            onAuthError("Open Sheildlenz inside the main app window to sign in.")
            return
        }

        if (!ensureFirebaseInitialized(activity)) {
            onAuthError("Firebase configuration unavailable on this build.")
            return
        }

        val clientId = resolveWebClientId(activity)
        if (clientId.isBlank()) {
            onAuthError("Google Sign-In configuration missing: default_web_client_id not found.")
            return
        }

        val activeCredentialManager = credentialManager ?: CredentialManager.create(activity)
        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        scope.launch {
            try {
                val result = activeCredentialManager.getCredential(activity, request)
                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    Firebase.auth.signInWithCredential(authCredential).await()
                    onAuthSuccess()
                } else {
                    onAuthError("Unexpected credential type returned by Google Sign-In.")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
                onAuthCancelled()
            } catch (e: NoCredentialException) {
                Log.w(TAG, "No Google credentials available on device: ${e.message}", e)
                onAuthError("No Google account found on this device. Add a Google account in device Settings and retry.")
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In failed", e)
                onAuthError(e.localizedMessage ?: "Google Sign-In failed.")
            }
        }
    }

    fun signOut(
        context: Context,
        credentialManager: CredentialManager? = null,
        scope: CoroutineScope,
        onSignOutComplete: () -> Unit = {}
    ) {
        val activeManager = credentialManager ?: CredentialManager.create(context.applicationContext)
        scope.launch {
            withContext(NonCancellable) {
                try {
                    activeManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to clear credential state: ${e.message}")
                } finally {
                    runCatching { Firebase.auth.signOut() }
                    onSignOutComplete()
                }
            }
        }
    }
}
