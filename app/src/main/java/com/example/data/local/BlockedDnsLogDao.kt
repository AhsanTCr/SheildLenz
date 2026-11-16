package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedDnsLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: BlockedDnsLogEntity): Long

    @Query("SELECT * FROM blocked_dns_logs ORDER BY timestampMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int = 250): Flow<List<BlockedDnsLogEntity>>

    @Query("SELECT * FROM blocked_dns_logs ORDER BY timestampMillis DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 250): List<BlockedDnsLogEntity>

    @Query("DELETE FROM blocked_dns_logs WHERE id NOT IN (SELECT id FROM blocked_dns_logs ORDER BY timestampMillis DESC LIMIT :keepCount)")
    suspend fun trimOldEntries(keepCount: Int = 250)

    @Query("DELETE FROM blocked_dns_logs WHERE LOWER(domain) = LOWER(:domain)")
    suspend fun deleteByDomain(domain: String)

    @Query("DELETE FROM blocked_dns_logs")
    suspend fun clearAll()
}
