package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [IncidentEntity::class, BlockedDnsLogEntity::class], version = 3, exportSchema = false)
abstract class AegisDatabase : RoomDatabase() {
    abstract fun incidentDao(): IncidentDao
    abstract fun blockedDnsLogDao(): BlockedDnsLogDao

    companion object {
        @Volatile
        private var INSTANCE: AegisDatabase? = null

        fun getInstance(context: Context): AegisDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AegisDatabase::class.java,
                    "aegis_shield.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
