package com.example.timeblocker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeBlockerDao {

    @Query("SELECT * FROM time_blocked_apps WHERE isRestricted = 1 ORDER BY packageName ASC")
    fun observeRestrictedApps(): Flow<List<TimeBlockedAppEntity>>

    @Query("SELECT * FROM time_blocked_apps ORDER BY packageName ASC")
    suspend fun getAllApps(): List<TimeBlockedAppEntity>

    @Query("SELECT * FROM time_blocked_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getApp(packageName: String): TimeBlockedAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertApp(entity: TimeBlockedAppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertApps(entities: List<TimeBlockedAppEntity>)

    @Query("DELETE FROM time_blocked_apps WHERE packageName = :packageName")
    suspend fun deleteApp(packageName: String)

    @Query("SELECT * FROM time_blocker_meta WHERE id = :id LIMIT 1")
    suspend fun getMeta(id: Int = TimeBlockerMetaEntity.META_ROW_ID): TimeBlockerMetaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMeta(meta: TimeBlockerMetaEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNonceIfAbsent(entity: ProcessedPassNonceEntity): Long

    @Query("DELETE FROM processed_pass_nonces WHERE recordedAtWallMs < :cutoffWallMs")
    suspend fun pruneExpiredNonces(cutoffWallMs: Long)

    @Query(
        """
        DELETE FROM processed_pass_nonces
        WHERE nonce NOT IN (
            SELECT nonce FROM processed_pass_nonces
            ORDER BY recordedAtWallMs DESC
            LIMIT :keepCount
        )
        """
    )
    suspend fun pruneExcessNonces(keepCount: Int = 500)

    @Query("SELECT COUNT(*) FROM processed_pass_nonces")
    suspend fun getNonceCount(): Int
}
