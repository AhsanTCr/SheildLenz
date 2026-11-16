package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_dns_logs")
data class BlockedDnsLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val domain: String,
    val reasonCategory: String,
    val matchedRule: String,
    val timestampMillis: Long = System.currentTimeMillis()
)
