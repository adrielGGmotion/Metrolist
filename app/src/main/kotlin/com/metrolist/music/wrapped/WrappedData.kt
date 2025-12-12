package com.metrolist.music.wrapped

import com.metrolist.music.db.MusicDatabase
import com.metrolist.innertube.YouTube
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

        val localTopSongs = database.mostPlayedSongs(fromTimeStamp, limit = 5).first()
        val localTopArtists = database.mostPlayedArtists(fromTimeStamp, limit = 5).first()
        val localTopAlbum = database.mostPlayedAlbums(fromTimeStamp, limit = 1).first().firstOrNull()
        val localTotalPlayTimeMillis = database.getTotalPlayTime(fromLocalDateTime)

        val history = YouTube.musicHistory().getOrNull()
        val remoteSongs = history?.sections?.flatMap { it.songs } ?: emptyList()

        val allSongs = (localTopSongs.map { song ->
            WrappedSong(
                id = song.song.id,
                title = song.song.title,
                artists = song.artists.map { artist -> WrappedArtist(id = artist.id, name = artist.name) },
                totalPlayTime = song.song.totalPlayTime
            )
        } + remoteSongs.map { song ->
            WrappedSong(
                id = song.id,
                title = song.title,
                artists = song.artists?.map { artist -> WrappedArtist(id = artist.id, name = artist.name) } ?: emptyList(),
                totalPlayTime = (song.duration ?: 0) * 1000L
            )
        }).groupBy { it.id }.map { it.value.maxByOrNull { song -> song.totalPlayTime }!! }
        val topSongs = allSongs.sortedByDescending { it.totalPlayTime }.take(5)

        val allArtists = (localTopArtists.map { artist ->
            WrappedArtist(id = artist.artist.id, name = artist.artist.name)
        } + remoteSongs.flatMap { it.artists ?: emptyList() }.map { artist ->
            WrappedArtist(id = artist.id, name = artist.name)
        }).groupBy { it.id }.map { it.value.first() }
        val topArtists = allArtists.take(5)

        val topAlbum = localTopAlbum

        val totalPlayTimeMillis = localTotalPlayTimeMillis + (remoteSongs.sumOf { it.duration ?: 0 } * 1000)
        val totalMinutes = (totalPlayTimeMillis / 60000).toInt()

        WrappedData(
            totalMinutes = totalMinutes,
            topSongs = topSongs,
            topArtists = topArtists,
            topAlbum = topAlbum
        )
    }
}
