package com.metrolist.music.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

enum class InteractionType {
    SONG,
    ALBUM,
    ARTIST,
    PLAYLIST
}

@Entity(tableName = "interaction_history")
data class InteractionHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: String,
    val type: InteractionType,
    val timestamp: Date = Date()
)
