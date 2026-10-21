package com.shohan.omnicalc.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Local history/favorites store (offline-first, spec §19).
 *
 * Schema versioning: v1 — future schema changes MUST add a migration here
 * (exportSchema will be enabled when the first migration is needed).
 */
@Database(
    entities = [HistoryEntity::class, FavoriteEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class OmniCalcDatabase : RoomDatabase() {

    abstract fun historyDao(): HistoryDao
    abstract fun favoritesDao(): FavoritesDao

    companion object {
        const val DB_NAME = "omnicalc.db"

        @Volatile
        private var instance: OmniCalcDatabase? = null

        fun get(context: Context): OmniCalcDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    OmniCalcDatabase::class.java,
                    DB_NAME,
                )
                    // No destructive fallback: losing history silently would be
                    // a data bug, not a convenience (see docs/PRIVACY.md).
                    .build()
                    .also { instance = it }
            }
    }
}
