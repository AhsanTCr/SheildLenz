package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.Base64
import android.util.Log
import com.example.core.models.SecurityIncident
import com.example.core.models.SensitivityLevel
import com.example.core.models.ThreatCategory
import com.example.core.models.ThreatSeverity
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

data class PairedChildSummary(
    val pairingId: String = "",
    val childUid: String = "",
    val parentId: String = "",
    val pairingCode: String = "",
    val childDisplayName: String = "Child Device",
    val parentDisplayName: String = "",
    val active: Boolean = false
)

data class RemotePolicySettings(
    val sensitivity: SensitivityLevel = SensitivityLevel.BALANCED,
    val strictEverything: Boolean = false,
    val lockdownDurationMinutes: Int = 600,
    val antiTamperEnabled: Boolean = true,
    val childUninstallLocked: Boolean = true,
    val liveSeeEnabled: Boolean = true,
    val whitelistedPackages: Set<String> = emptySet(),
    val warningThresholdCount: Int = 0,
    val warningOnlyNoLockout: Boolean = false,
    val bedtimeEnabled: Boolean = false,
    val bedtimeStartMinutes: Int = 22 * 60,
    val bedtimeEndMinutes: Int = 6 * 60,
    val appTimeBudgets: Map<String, Int> = emptyMap(),
    val customBlockedDomains: Set<String> = emptySet(),
    val customBlockedKeywords: Set<String> = emptySet(),
    val safeDnsEnabled: Boolean = false,
    val blockExplicitAdult: Boolean = true,
    val blockSuggestiveContent: Boolean = true,
    val blockSensitiveGraphic: Boolean = true,
    val blockGamblingKeywords: Boolean = true,
    val studyModeEnabled: Boolean = false,
    val studyModeDurationMinutes: Int = 25,
    val studySoftWarningSeconds: Int = 30,
    val studyAllowedPackages: Set<String> = emptySet(),
    val smartSafeSearchEnabled: Boolean = false,
    val safeSearchPacks: Set<String> = setOf("adult", "gambling"),
    val discreetBlockingScreenMode: Boolean = true
)

data class RemoteCommandPayload(
    val action: String = ParentChildRepository.COMMAND_NONE,
    val timestamp: Long = 0L,
    val acknowledged: Boolean = true,
    val issuedByUid: String = ""
)

data class ChildLiveThumbnail(
    val thumbnailBase64: String = "",
    val activeAppName: String = "",
    val timestamp: Long = 0L,
    val protectionArmed: Boolean = false,
    val lockdownActive: Boolean = false
)

data class IntruderAlertPayload(
    val attemptCount: Int = 0,
    val surface: String = "",
    val timestamp: Long = 0L,
    val photoBase64: String = "",
    val acknowledged: Boolean = false
)

class ParentChildRepository(
    context: Context,
    db: FirebaseFirestore? = null,
    auth: FirebaseAuth? = null
) {
    private val dbOverride: FirebaseFirestore? = db
    private val authOverride: FirebaseAuth? = auth
    private val appContext: Context = context.applicationContext

    private fun ensureFirebaseInitialized(): Boolean {
        if (dbOverride != null && authOverride != null) return true
        if (FirebaseApp.getApps(appContext).isNotEmpty()) return true
        return runCatching { FirebaseApp.initializeApp(appContext) != null }.getOrDefault(false)
    }

    private val firestoreOrNull: FirebaseFirestore? by lazy {
        dbOverride ?: runCatching {
            if (ensureFirebaseInitialized()) {
                val dbId = runCatching {
                    val resId = appContext.resources.getIdentifier("firestore_database_id", "string", appContext.packageName)
                    if (resId != 0) appContext.getString(resId).trim() else null
                }.getOrNull()?.takeIf { it.isNotBlank() } ?: "ai-studio-android-sheildle-445b68c5-4463-4bd1-939c-33931bb3fc12"
                FirebaseFirestore.getInstance(dbId)
            } else {
                null
            }
        }.getOrNull()
    }

    private val auth: FirebaseAuth? by lazy {
        authOverride ?: runCatching {
            if (ensureFirebaseInitialized()) {
                Firebase.auth
            } else {
                null
            }
        }.getOrNull()
    }

    private fun requireDb(): FirebaseFirestore =
        firestoreOrNull ?: throw IllegalStateException("Firebase Firestore is not initialized.")

    private val defaultDeviceName: String =
        listOfNotNull(Build.MANUFACTURER, Build.MODEL)
            .joinToString(" ")
            .trim()
            .ifBlank { "Android Device" }
            .take(60)

    fun getCurrentUserId(): String? = auth?.currentUser?.uid

    fun getCurrentUserEmail(): String = auth?.currentUser?.email ?: ""

    fun getCurrentUserDisplayName(): String =
        auth?.currentUser?.displayName?.takeIf { it.isNotBlank() }
            ?: auth?.currentUser?.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
            ?: defaultDeviceName

    private fun requireUserId(): String =
        auth?.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    fun handleFirestoreError(exception: Throwable, operationType: OperationType, path: String?): String {
        val causes = generateSequence(exception) { it.cause }
        val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
            ?: causes.firstOrNull { it !is CancellationException }
            ?: exception

        val currentUser = auth?.currentUser
        val providerInfoList = currentUser?.providerData?.map { provider ->
            JSONObject().apply {
                put("providerId", provider.providerId)
                put("email", provider.email)
            }
        } ?: emptyList()

        val authInfoJson = JSONObject().apply {
            put("userId", currentUser?.uid)
            put("email", currentUser?.email)
            put("emailVerified", currentUser?.isEmailVerified)
            put("tenantId", currentUser?.tenantId)
            put("providerInfo", JSONArray(providerInfoList))
        }

        val errorInfoJson = JSONObject().apply {
            put("error", rootError.message ?: rootError.toString())
            put("operationType", operationType.value)
            put("path", path)
            put("authInfo", authInfoJson)
        }

        val jsonString = errorInfoJson.toString()
        Log.e("FirestoreError", "Firestore Error: $jsonString")
        return jsonString
    }

    suspend fun saveUserProfile(
        role: String,
        displayName: String = getCurrentUserDisplayName(),
        protectionArmed: Boolean = false,
        lockdownActive: Boolean = false
    ): Boolean {
        val uid = auth?.currentUser?.uid ?: return false
        val db = firestoreOrNull ?: return false
        val path = "users/$uid"
        val data = mapOf(
            "userId" to uid,
            "role" to role,
            "email" to getCurrentUserEmail(),
            "displayName" to displayName.take(80).ifBlank { defaultDeviceName },
            "deviceName" to defaultDeviceName,
            "protectionArmed" to protectionArmed,
            "lockdownActive" to lockdownActive,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            db.document(path).set(data).await()
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            false
        }
    }

    suspend fun registerAsChild(deviceDisplayName: String = getCurrentUserDisplayName()): String {
        val uid = requireUserId()
        val db = requireDb()
        val code = (100000..999999).random().toString()
        val cleanName = deviceDisplayName.take(80).ifBlank { defaultDeviceName }

        val pairingPath = "pairings/$uid"
        val pairingData = mapOf(
            "pairingId" to uid,
            "parentId" to "",
            "childUid" to uid,
            "pairingCode" to code,
            "childDisplayName" to cleanName,
            "parentDisplayName" to "",
            "active" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        val lookupPath = "pairing_codes/$code"
        val lookupData = mapOf(
            "pairingCode" to code,
            "childUid" to uid,
            "childDisplayName" to cleanName,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            db.document(pairingPath).set(pairingData).await()
            db.document(lookupPath).set(lookupData).await()
            return code
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, pairingPath)
            throw e
        }
    }

    suspend fun pairWithChildByCode(rawInput: String): Result<PairedChildSummary> {
        val parentUid = auth?.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Please sign in with Google first."))
        val db = firestoreOrNull
            ?: return Result.failure(IllegalStateException("Cloud service unavailable."))
        val code = extractPairingCode(rawInput)
            ?: return Result.failure(IllegalArgumentException("Enter a valid 6-digit code or scan QR link."))

        val lookupPath = "pairing_codes/$code"
        return try {
            val lookupSnap = db.document(lookupPath).get().await()
            if (!lookupSnap.exists()) {
                return Result.failure(IllegalStateException("Code not found or expired. Generate a fresh code on Child device."))
            }
            val childUid = lookupSnap.getString("childUid")?.takeIf { it.isNotBlank() }
                ?: return Result.failure(IllegalStateException("Invalid child reference."))
            if (childUid == parentUid) {
                return Result.failure(IllegalStateException("Cannot pair a device to the same UID as both parent and child."))
            }
            val childDisplayName = lookupSnap.getString("childDisplayName") ?: "Child Device"
            val parentDisplayName = getCurrentUserDisplayName()

            val pairingPath = "pairings/$childUid"
            db.document(pairingPath).update(
                mapOf(
                    "parentId" to parentUid,
                    "parentDisplayName" to parentDisplayName.take(80),
                    "active" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Remove consumed code best-effort
            runCatching { db.document(lookupPath).delete().await() }

            // Initialize default remote settings document if missing so child & parent start in sync
            val settingsPath = "users/$childUid/remote_control/settings"
            val existingSettings = runCatching { db.document(settingsPath).get().await() }.getOrNull()
            if (existingSettings == null || !existingSettings.exists()) {
                pushRemoteSettings(childUid, RemotePolicySettings())
            }

            Result.success(
                PairedChildSummary(
                    pairingId = childUid,
                    childUid = childUid,
                    parentId = parentUid,
                    pairingCode = code,
                    childDisplayName = childDisplayName,
                    parentDisplayName = parentDisplayName,
                    active = true
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, lookupPath)
            Result.failure(e)
        }
    }

    suspend fun unlinkChild(childUid: String): Boolean {
        if (childUid.isBlank()) return false
        val path = "pairings/$childUid"
        return try {
            requireUserId()
            requireDb().document(path).update(
                mapOf(
                    "active" to false,
                    "parentId" to "",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            false
        }
    }

    fun observePairedChildren(): Flow<List<PairedChildSummary>> = flow {
        val parentUid = auth?.currentUser?.uid
        val db = firestoreOrNull
        if (parentUid == null || db == null) {
            emit(emptyList())
            return@flow
        }
        val path = "pairings"
        emitAll(
            db.collection(path)
                .whereEqualTo("parentId", parentUid)
                .whereEqualTo("active", true)
                .snapshots()
                .map { snap ->
                    snap.documents.map { doc ->
                        PairedChildSummary(
                            pairingId = doc.id,
                            childUid = doc.getString("childUid") ?: doc.id,
                            parentId = doc.getString("parentId") ?: "",
                            pairingCode = doc.getString("pairingCode") ?: "",
                            childDisplayName = doc.getString("childDisplayName") ?: "Child Device",
                            parentDisplayName = doc.getString("parentDisplayName") ?: "",
                            active = doc.getBoolean("active") ?: false
                        )
                    }
                }
                .catch { error ->
                    val fe = unwrapFirestoreError(error)
                    if (fe == null && error is CancellationException) throw error
                    if (auth?.currentUser?.uid == parentUid) {
                        handleFirestoreError(fe ?: error, OperationType.LIST, path)
                    }
                    emit(emptyList())
                }
        )
    }

    fun observeChildOwnPairing(): Flow<PairedChildSummary?> = flow {
        val childUid = auth?.currentUser?.uid
        val db = firestoreOrNull
        if (childUid == null || db == null) {
            emit(null)
            return@flow
        }
        val path = "pairings/$childUid"
        emitAll(
            db.document(path)
                .snapshots()
                .map { doc ->
                    if (!doc.exists()) null else PairedChildSummary(
                        pairingId = doc.id,
                        childUid = doc.getString("childUid") ?: doc.id,
                        parentId = doc.getString("parentId") ?: "",
                        pairingCode = doc.getString("pairingCode") ?: "",
                        childDisplayName = doc.getString("childDisplayName") ?: "Child Device",
                        parentDisplayName = doc.getString("parentDisplayName") ?: "",
                        active = doc.getBoolean("active") ?: false
                    )
                }
                .catch { error ->
                    val fe = unwrapFirestoreError(error)
                    if (fe == null && error is CancellationException) throw error
                    if (auth?.currentUser?.uid == childUid) {
                        handleFirestoreError(fe ?: error, OperationType.GET, path)
                    }
                    emit(null)
                }
        )
    }

    suspend fun pushRemoteSettings(childUid: String, settings: RemotePolicySettings): Boolean {
        val path = "users/$childUid/remote_control/settings"
        return try {
            requireUserId()
            val db = requireDb()
            val payload = mapOf(
                "sensitivity" to settings.sensitivity.name,
                "strictEverything" to settings.strictEverything,
                "lockdownDurationMinutes" to settings.lockdownDurationMinutes.coerceIn(1, 1440),
                "antiTamperEnabled" to settings.antiTamperEnabled,
                "childUninstallLocked" to settings.childUninstallLocked,
                "liveSeeEnabled" to settings.liveSeeEnabled,
                "whitelistedPackages" to settings.whitelistedPackages.take(40),
                "warningThresholdCount" to settings.warningThresholdCount.coerceIn(0, 5),
                "warningOnlyNoLockout" to settings.warningOnlyNoLockout,
                "bedtimeEnabled" to settings.bedtimeEnabled,
                "bedtimeStartMinutes" to settings.bedtimeStartMinutes.coerceIn(0, 1439),
                "bedtimeEndMinutes" to settings.bedtimeEndMinutes.coerceIn(0, 1439),
                "appTimeBudgets" to settings.appTimeBudgets.entries.take(50).associate { it.key to it.value.coerceIn(1, 720) },
                "customBlockedDomains" to settings.customBlockedDomains.take(200),
                "customBlockedKeywords" to settings.customBlockedKeywords.take(200),
                "safeDnsEnabled" to settings.safeDnsEnabled,
                "blockExplicitAdult" to settings.blockExplicitAdult,
                "blockSuggestiveContent" to settings.blockSuggestiveContent,
                "blockSensitiveGraphic" to settings.blockSensitiveGraphic,
                "blockGamblingKeywords" to settings.blockGamblingKeywords,
                "studyModeEnabled" to settings.studyModeEnabled,
                "studyModeDurationMinutes" to settings.studyModeDurationMinutes.coerceIn(1, 240),
                "studySoftWarningSeconds" to settings.studySoftWarningSeconds.coerceIn(5, 120),
                "studyAllowedPackages" to settings.studyAllowedPackages.take(40),
                "smartSafeSearchEnabled" to settings.smartSafeSearchEnabled,
                "safeSearchPacks" to settings.safeSearchPacks.take(10),
                "discreetBlockingScreenMode" to settings.discreetBlockingScreenMode,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.document(path).set(payload).await()
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            false
        }
    }

    fun observeRemoteSettings(childUid: String): Flow<RemotePolicySettings?> = flow {
        val uid = auth?.currentUser?.uid
        val db = firestoreOrNull
        if (uid == null || db == null) {
            emit(null)
            return@flow
        }
        val path = "users/$childUid/remote_control/settings"
        emitAll(
            db.document(path)
                .snapshots()
                .map { doc ->
                    if (!doc.exists()) null else {
                        val sensName = doc.getString("sensitivity") ?: SensitivityLevel.BALANCED.name
                        val sensitivity = runCatching { SensitivityLevel.valueOf(sensName) }
                            .getOrDefault(SensitivityLevel.BALANCED)
                        val whitelistRaw = (doc.get("whitelistedPackages") as? List<*>)
                            ?.mapNotNull { it as? String }
                            ?.toSet()
                            ?: emptySet()
                        val domainsRaw = (doc.get("customBlockedDomains") as? List<*>)
                            ?.mapNotNull { it as? String }
                            ?.toSet()
                            ?: emptySet()
                        val keywordsRaw = (doc.get("customBlockedKeywords") as? List<*>)
                            ?.mapNotNull { it as? String }
                            ?.toSet()
                            ?: emptySet()
                        val budgetsRaw = (doc.get("appTimeBudgets") as? Map<*, *>)
                            ?.entries
                            ?.mapNotNull { (k, v) ->
                                val pkg = k as? String
                                val mins = (v as? Number)?.toInt()
                                if (!pkg.isNullOrBlank() && mins != null && mins > 0) pkg to mins.coerceIn(1, 720) else null
                            }
                            ?.toMap()
                            ?: emptyMap()
                        val studyAllowedRaw = (doc.get("studyAllowedPackages") as? List<*>)
                            ?.mapNotNull { it as? String }
                            ?.toSet()
                            ?: emptySet()
                        val safeSearchPacksRaw = (doc.get("safeSearchPacks") as? List<*>)
                            ?.mapNotNull { it as? String }
                            ?.toSet()
                            ?: setOf("adult", "gambling")
                        RemotePolicySettings(
                            sensitivity = sensitivity,
                            strictEverything = doc.getBoolean("strictEverything") ?: false,
                            lockdownDurationMinutes = (doc.getLong("lockdownDurationMinutes") ?: 600L)
                                .toInt().coerceIn(1, 1440),
                            antiTamperEnabled = doc.getBoolean("antiTamperEnabled") ?: true,
                            childUninstallLocked = doc.getBoolean("childUninstallLocked") ?: true,
                            liveSeeEnabled = doc.getBoolean("liveSeeEnabled") ?: true,
                            whitelistedPackages = whitelistRaw,
                            warningThresholdCount = (doc.getLong("warningThresholdCount") ?: 0L).toInt().coerceIn(0, 5),
                            warningOnlyNoLockout = doc.getBoolean("warningOnlyNoLockout") ?: false,
                            bedtimeEnabled = doc.getBoolean("bedtimeEnabled") ?: false,
                            bedtimeStartMinutes = (doc.getLong("bedtimeStartMinutes") ?: (22L * 60L)).toInt().coerceIn(0, 1439),
                            bedtimeEndMinutes = (doc.getLong("bedtimeEndMinutes") ?: (6L * 60L)).toInt().coerceIn(0, 1439),
                            appTimeBudgets = budgetsRaw,
                            customBlockedDomains = domainsRaw,
                            customBlockedKeywords = keywordsRaw,
                            safeDnsEnabled = doc.getBoolean("safeDnsEnabled") ?: false,
                            blockExplicitAdult = doc.getBoolean("blockExplicitAdult") ?: true,
                            blockSuggestiveContent = doc.getBoolean("blockSuggestiveContent") ?: true,
                            blockSensitiveGraphic = doc.getBoolean("blockSensitiveGraphic") ?: true,
                            blockGamblingKeywords = doc.getBoolean("blockGamblingKeywords") ?: true,
                            studyModeEnabled = doc.getBoolean("studyModeEnabled") ?: false,
                            studyModeDurationMinutes = (doc.getLong("studyModeDurationMinutes") ?: 25L).toInt().coerceIn(1, 240),
                            studySoftWarningSeconds = (doc.getLong("studySoftWarningSeconds") ?: 30L).toInt().coerceIn(5, 120),
                            studyAllowedPackages = studyAllowedRaw,
                            smartSafeSearchEnabled = doc.getBoolean("smartSafeSearchEnabled") ?: false,
                            safeSearchPacks = safeSearchPacksRaw,
                            discreetBlockingScreenMode = doc.getBoolean("discreetBlockingScreenMode") ?: true
                        )
                    }
                }
                .catch { error ->
                    val fe = unwrapFirestoreError(error)
                    if (fe == null && error is CancellationException) throw error
                    if (auth?.currentUser?.uid == uid) {
                        handleFirestoreError(fe ?: error, OperationType.GET, path)
                    }
                    emit(null)
                }
        )
    }

    suspend fun sendRemoteCommand(childUid: String, action: String): Boolean {
        val path = "users/$childUid/remote_control/commands"
        return try {
            val uid = requireUserId()
            requireDb().document(path).set(
                mapOf(
                    "action" to action,
                    "timestamp" to System.currentTimeMillis(),
                    "acknowledged" to false,
                    "issuedByUid" to uid
                )
            ).await()
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            false
        }
    }

    suspend fun acknowledgeRemoteCommand(childUid: String): Boolean {
        val path = "users/$childUid/remote_control/commands"
        return try {
            requireUserId()
            requireDb().document(path).update("acknowledged", true).await()
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            false
        }
    }

    fun observeRemoteCommands(childUid: String): Flow<RemoteCommandPayload?> = flow {
        val uid = auth?.currentUser?.uid
        val db = firestoreOrNull
        if (uid == null || db == null) {
            emit(null)
            return@flow
        }
        val path = "users/$childUid/remote_control/commands"
        emitAll(
            db.document(path)
                .snapshots()
                .map { doc ->
                    if (!doc.exists()) null else RemoteCommandPayload(
                        action = doc.getString("action") ?: COMMAND_NONE,
                        timestamp = doc.getLong("timestamp") ?: 0L,
                        acknowledged = doc.getBoolean("acknowledged") ?: true,
                        issuedByUid = doc.getString("issuedByUid") ?: ""
                    )
                }
                .catch { error ->
                    val fe = unwrapFirestoreError(error)
                    if (fe == null && error is CancellationException) throw error
                    if (auth?.currentUser?.uid == uid) {
                        handleFirestoreError(fe ?: error, OperationType.GET, path)
                    }
                    emit(null)
                }
        )
    }

    suspend fun uploadLiveThumbnail(
        bitmap: Bitmap,
        activeAppName: String,
        protectionArmed: Boolean,
        lockdownActive: Boolean
    ): Boolean {
        val childUid = auth?.currentUser?.uid ?: return false
        val db = firestoreOrNull ?: return false
        val path = "users/$childUid/remote_control/thumbnails"
        return try {
            val base64 = encodeThumbnailToBase64(bitmap)
            if (base64.isBlank()) return false
            db.document(path).set(
                mapOf(
                    "thumbnailBase64" to base64,
                    "activeAppName" to activeAppName.take(80).ifBlank { "Screen" },
                    "timestamp" to System.currentTimeMillis(),
                    "protectionArmed" to protectionArmed,
                    "lockdownActive" to lockdownActive
                )
            ).await()
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            false
        }
    }

    fun observeLiveThumbnail(childUid: String): Flow<ChildLiveThumbnail?> = flow {
        val uid = auth?.currentUser?.uid
        val db = firestoreOrNull
        if (uid == null || db == null) {
            emit(null)
            return@flow
        }
        val path = "users/$childUid/remote_control/thumbnails"
        emitAll(
            db.document(path)
                .snapshots()
                .map { doc ->
                    if (!doc.exists()) null else ChildLiveThumbnail(
                        thumbnailBase64 = doc.getString("thumbnailBase64") ?: "",
                        activeAppName = doc.getString("activeAppName") ?: "Screen",
                        timestamp = doc.getLong("timestamp") ?: 0L,
                        protectionArmed = doc.getBoolean("protectionArmed") ?: false,
                        lockdownActive = doc.getBoolean("lockdownActive") ?: false
                    )
                }
                .catch { error ->
                    val fe = unwrapFirestoreError(error)
                    if (fe == null && error is CancellationException) throw error
                    if (auth?.currentUser?.uid == uid) {
                        handleFirestoreError(fe ?: error, OperationType.GET, path)
                    }
                    emit(null)
                }
        )
    }

    suspend fun uploadIntruderAlert(
        attemptCount: Int,
        surface: String,
        photoBase64: String = ""
    ): Boolean {
        val childUid = auth?.currentUser?.uid ?: return false
        val db = firestoreOrNull ?: return false
        val path = "users/$childUid/remote_control/intruder_alert"
        return try {
            db.document(path).set(
                mapOf(
                    "attemptCount" to attemptCount,
                    "surface" to surface.take(60),
                    "timestamp" to System.currentTimeMillis(),
                    "photoBase64" to photoBase64.take(350000),
                    "acknowledged" to false
                )
            ).await()
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            false
        }
    }

    fun observeIntruderAlert(childUid: String): Flow<IntruderAlertPayload?> = flow {
        val uid = auth?.currentUser?.uid
        val db = firestoreOrNull
        if (uid == null || db == null) {
            emit(null)
            return@flow
        }
        val path = "users/$childUid/remote_control/intruder_alert"
        emitAll(
            db.document(path)
                .snapshots()
                .map { doc ->
                    if (!doc.exists()) null else IntruderAlertPayload(
                        attemptCount = (doc.getLong("attemptCount") ?: 1L).toInt(),
                        surface = doc.getString("surface") ?: "PIN Entry",
                        timestamp = doc.getLong("timestamp") ?: 0L,
                        photoBase64 = doc.getString("photoBase64") ?: "",
                        acknowledged = doc.getBoolean("acknowledged") ?: false
                    )
                }
                .catch { error ->
                    val fe = unwrapFirestoreError(error)
                    if (fe == null && error is CancellationException) throw error
                    if (auth?.currentUser?.uid == uid) {
                        handleFirestoreError(fe ?: error, OperationType.GET, path)
                    }
                    emit(null)
                }
        )
    }

    suspend fun acknowledgeIntruderAlert(childUid: String): Boolean {
        val path = "users/$childUid/remote_control/intruder_alert"
        return try {
            requireUserId()
            requireDb().document(path).update("acknowledged", true).await()
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            false
        }
    }

    suspend fun logThreatToCloud(incident: SecurityIncident): Boolean {
        val childUid = auth?.currentUser?.uid ?: return false
        val db = firestoreOrNull ?: return false
        val logRef = db.collection("users").document(childUid).collection("threat_logs").document()
        val path = "users/$childUid/threat_logs/${logRef.id}"
        return try {
            val data = mapOf(
                "logId" to logRef.id,
                "childUid" to childUid,
                "timestampMillis" to incident.timestampMillis,
                "packageName" to incident.packageName.take(160),
                "appDisplayName" to incident.appDisplayName.take(80),
                "category" to incident.category.name,
                "severity" to incident.severity.name,
                "confidenceScore" to incident.confidenceScore.coerceIn(0f, 1f),
                "actionTaken" to incident.actionTaken.take(160)
            )
            logRef.set(data).await()
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            false
        }
    }

    fun observeThreatLogsForChild(childUid: String): Flow<List<SecurityIncident>> = flow {
        val uid = auth?.currentUser?.uid
        val db = firestoreOrNull
        if (uid == null || db == null) {
            emit(emptyList())
            return@flow
        }
        val path = "users/$childUid/threat_logs"
        emitAll(
            db.collection("users").document(childUid).collection("threat_logs")
                .orderBy("timestampMillis", Query.Direction.DESCENDING)
                .limit(30)
                .snapshots()
                .map { snap ->
                    snap.documents.mapNotNull { doc ->
                        try {
                            SecurityIncident(
                                timestampMillis = doc.getLong("timestampMillis") ?: 0L,
                                packageName = doc.getString("packageName") ?: "",
                                appDisplayName = doc.getString("appDisplayName") ?: "",
                                category = ThreatCategory.valueOf(doc.getString("category") ?: ThreatCategory.EXPLICIT_ADULT.name),
                                severity = ThreatSeverity.valueOf(doc.getString("severity") ?: ThreatSeverity.CRITICAL.name),
                                confidenceScore = (doc.getDouble("confidenceScore") ?: 0.9).toFloat(),
                                actionTaken = doc.getString("actionTaken") ?: ""
                            )
                        } catch (_: Exception) {
                            null
                        }
                    }
                }
                .catch { error ->
                    val fe = unwrapFirestoreError(error)
                    if (fe == null && error is CancellationException) throw error
                    if (auth?.currentUser?.uid == uid) {
                        handleFirestoreError(fe ?: error, OperationType.LIST, path)
                    }
                    emit(emptyList())
                }
        )
    }

    private fun unwrapFirestoreError(error: Throwable): FirebaseFirestoreException? {
        return generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
    }

    private fun encodeThumbnailToBase64(source: Bitmap): String {
        if (source.isRecycled || source.width <= 0 || source.height <= 0) return ""
        val targetWidth = 220
        val scale = targetWidth.toFloat() / source.width.toFloat()
        val targetHeight = (source.height * scale).roundToInt().coerceAtLeast(120)
        val scaled = if (source.width > targetWidth) {
            Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
        } else {
            source
        }
        return try {
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 45, out)
            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        } finally {
            if (scaled !== source && !scaled.isRecycled) {
                scaled.recycle()
            }
        }
    }

    companion object {
        const val COMMAND_NONE = "NONE"
        const val COMMAND_LOCKDOWN = "LOCKDOWN"
        const val COMMAND_UNLOCK = "UNLOCK"
        const val COMMAND_ALLOW_UNINSTALL = "ALLOW_UNINSTALL"
        const val COMMAND_LOCK_UNINSTALL = "LOCK_UNINSTALL"
        const val COMMAND_REQUEST_LIVE_FRAME = "REQUEST_LIVE_FRAME"

        private val PAIRING_CODE_REGEX = Regex("\\b([0-9]{6})\\b")

        fun buildPairingUri(code: String): String = "sheildlenz://pair?code=${code.trim()}"

        fun extractPairingCode(rawInput: String): String? {
            val trimmed = rawInput.trim()
            if (trimmed.isEmpty()) return null
            if (trimmed.length == 6 && trimmed.all { it.isDigit() }) return trimmed
            val queryIdx = trimmed.indexOf("code=")
            if (queryIdx >= 0) {
                val sub = trimmed.substring(queryIdx + 5).takeWhile { it.isDigit() }
                if (sub.length == 6) return sub
            }
            return PAIRING_CODE_REGEX.find(trimmed)?.groupValues?.getOrNull(1)
        }
    }
}
