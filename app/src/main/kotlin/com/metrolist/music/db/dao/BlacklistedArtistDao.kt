package com.metrolist.music.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.metrolist.music.db.entities.BlacklistedArtist
import kotlinx.coroutines.flow.Flow

@Dao
interface BlacklistedArtistDao {
    @Query("SELECT * FROM blacklisted_artists")
    fun getAll(): Flow<List<BlacklistedArtist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(artist: BlacklistedArtist)

    @Query("DELETE FROM blacklisted_artists WHERE id = :artistId")
    suspend fun delete(artistId: String)
}
