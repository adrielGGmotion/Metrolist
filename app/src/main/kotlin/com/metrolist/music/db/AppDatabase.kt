package com.metrolist.music.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.metrolist.music.db.dao.BlacklistedArtistDao
import com.metrolist.music.db.entities.BlacklistedArtist

@Database(entities = [BlacklistedArtist::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun blacklistedArtistDao(): BlacklistedArtistDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "metrolist_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
