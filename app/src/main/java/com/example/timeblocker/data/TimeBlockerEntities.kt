package com.example.timeblocker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "time_blocked_apps")
data class TimeBlockedAppEntity(
    @PrimaryKey
    val packageName: String,
    val isRestricted: Boolean = true,
    val windowStartElapsedMs: Long = 0L,
    val usedMsInWindow: Long = 0L,
    val passExpiresAtElapsedMs: Long = 0L,
    val addedAtElapsedMs: Long = 0L
)

@Entity(tableName = "time_blocker_meta")
data class TimeBlockerMetaEntity(
    @PrimaryKey
    val id: Int = META_ROW_ID,
    val lastKnownElapsedMs: Long = 0L,
    val lastKnownBootCount: Int = -1
) {
    companion object {
        const val META_ROW_ID = 1
    }
}

@Entity(tableName = "processed_pass_nonces")
data class ProcessedPassNonceEntity(
    @PrimaryKey
    val nonce: String,
    val targetPackage: String,
    val recordedAtWallMs: Long
)
