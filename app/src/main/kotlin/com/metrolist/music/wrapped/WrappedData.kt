package com.metrolist.music.wrapped

import com.metrolist.music.db.MusicDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId

data class WrappedData(
    val totalMinutes: Int,
    val topSongs: List<WrappedSong>,
    val topArtists: List<WrappedArtist>,
    val topAlbum: com.metrolist.music.db.entities.Album?
)

suspend fun calculateWrappedData(database: MusicDatabase): WrappedData {
    return withContext(Dispatchers.IO) {
        val fromTimeStamp = System.currentTimeMillis() - 86400000L * 365
        val fromLocalDateTime = Instant.ofEpochMilli(fromTimeStamp).atZone(ZoneId.systemDefault()).toLocalDateTime()

        val topSongs = database.mostPlayedSongs(fromTimeStamp, limit = 5).first().map {
            WrappedSong(
                id = it.song.id,
                title = it.song.title,
                artists = it.artists.map { artist -> WrappedArtist(id = artist.id, name = artist.name) },
                totalPlayTime = it.song.totalPlayTime
            )
        }
        val topArtists = database.mostPlayedArtists(fromTimeStamp, limit = 5).first().map {
            WrappedArtist(
                id = it.artist.id,
                name = it.artist.name
            )
        }
        val topAlbum = database.mostPlayedAlbums(fromTimeStamp, limit = 1).first().firstOrNull()
        val totalPlayTimeMillis = database.getTotalPlayTime(fromLocalDateTime)
        val totalMinutes = (totalPlayTimeMillis / 60000).toInt()

        WrappedData(
            totalMinutes = totalMinutes,
            topSongs = topSongs,
            topArtists = topArtists,
            topAlbum = topAlbum
        )
    }
}
