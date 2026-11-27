package com.example.timeblocker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TimeBlockedAppEntity::class,
        TimeBlockerMetaEntity::class,
        ProcessedPassNonceEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class TimeBlockerDatabase : RoomDatabase() {
    abstract fun timeBlockerDao(): TimeBlockerDao

    companion object {
        private const val DATABASE_NAME = "sheildlenz_time_blocker.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS processed_pass_nonces (
                        nonce TEXT NOT NULL PRIMARY KEY,
                        targetPackage TEXT NOT NULL,
                        recordedAtWallMs INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE time_blocked_apps ADD COLUMN allowanceMs INTEGER NOT NULL DEFAULT 300000")
                db.execSQL("ALTER TABLE time_blocked_apps ADD COLUMN windowMs INTEGER NOT NULL DEFAULT 3600000")
            }
        }

        @Volatile
        private var INSTANCE: TimeBlockerDatabase? = null

        fun getInstance(context: Context): TimeBlockerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TimeBlockerDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
