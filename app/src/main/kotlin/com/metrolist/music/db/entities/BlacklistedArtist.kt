package com.metrolist.music.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blacklisted_artists")
data class BlacklistedArtist(
    @PrimaryKey val id: String,
    val name: String
)
