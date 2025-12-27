package com.metrolist.music.ui.screens.wrapped

import android.util.Log
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AccountInfo
import com.metrolist.music.constants.ArtistSongSortType
import com.metrolist.music.db.DatabaseDao
import com.metrolist.music.db.entities.Artist
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.db.entities.SongWithStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.util.Calendar
import java.util.UUID

sealed class PlaylistCreationState {
    object Idle : PlaylistCreationState()
    object Creating : PlaylistCreationState()
    object Success : PlaylistCreationState()
}

class WrappedManager(
    private val databaseDao: DatabaseDao,
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow(WrappedState())
    val state = _state.asStateFlow()

    fun saveWrappedPlaylist() {
        if (_state.value.playlistCreationState != PlaylistCreationState.Idle) return

        _state.update { it.copy(playlistCreationState = PlaylistCreationState.Creating) }
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val playlistId = UUID.randomUUID().toString()
                    val newPlaylist = PlaylistEntity(
                        id = playlistId,
                        name = WrappedConstants.PLAYLIST_NAME,
                        thumbnailUrl = "android.resource://com.metrolist.music/drawable/ic_launcher_static_foreground",
                        bookmarkedAt = LocalDateTime.now(),
                        isEditable = true
                    )
                    databaseDao.insert(newPlaylist)

                    val createdPlaylist = databaseDao.playlist(playlistId).first()
                    if (createdPlaylist != null) {
                        val songIds = _state.value.topSongs.map { it.id }
                        databaseDao.addSongToPlaylist(createdPlaylist, songIds)
                    } else {
                        Log.e("WrappedManager", "Failed to retrieve created playlist with id: $playlistId")
                    }
                }
                _state.update { it.copy(playlistCreationState = PlaylistCreationState.Success) }
            } catch (e: Exception) {
                Log.e("WrappedManager", "Error saving wrapped playlist", e)
                _state.update { it.copy(playlistCreationState = PlaylistCreationState.Idle) }
            }
        }
    }

    private suspend fun generatePlaylistMap() {
        val topSongs = _state.value.topSongs
        val topArtists = _state.value.topArtists
        if (topSongs.isEmpty()) {
            Log.w("WrappedManager", "Cannot generate playlist map, top songs list is empty.")
            _state.update { it.copy(trackMap = emptyMap()) }
            return
        }

        val playlistMap = mutableMapOf<WrappedScreenType, String>()

        val topSong = topSongs.first()
        val randomTrack = topSongs.filter { it.id != topSong.id }.randomOrNull()?.id ?: topSongs.getOrNull(1)?.id ?: topSong.id
        playlistMap[WrappedScreenType.Welcome] = randomTrack
        playlistMap[WrappedScreenType.MinutesTease] = randomTrack
        playlistMap[WrappedScreenType.MinutesReveal] = randomTrack

        playlistMap[WrappedScreenType.TotalSongs] = topSong.id
        playlistMap[WrappedScreenType.TopSongReveal] = topSong.id
        playlistMap[WrappedScreenType.Top5Songs] = topSong.id

        val topArtistTrack = topArtists.firstOrNull()?.let { artist ->
            databaseDao.artistSongs(
                artistId = artist.id,
                sortType = ArtistSongSortType.PLAY_TIME,
                descending = true
            ).first().firstOrNull { it.id != topSong.id }?.id
        } ?: randomTrack
        playlistMap[WrappedScreenType.TotalArtists] = topArtistTrack
        playlistMap[WrappedScreenType.TopArtistReveal] = topArtistTrack
        playlistMap[WrappedScreenType.Top5Artists] = topArtistTrack

        playlistMap[WrappedScreenType.End] = "2-p9DM2Xvsc"

        Log.d("WrappedManager", "Generated Playlist Map: $playlistMap")
        _state.update { it.copy(trackMap = playlistMap) }
    }

    suspend fun prepare() {
        if (_state.value.isDataReady) return
        Log.d("WrappedManager", "Starting Wrapped data preparation")

        val fromTimestamp = Calendar.getInstance().apply {
            set(WrappedConstants.YEAR, Calendar.JANUARY, 1, 0, 0, 0)
        }.timeInMillis

        val toTimestamp = Calendar.getInstance().apply {
            set(WrappedConstants.YEAR, Calendar.DECEMBER, 31, 23, 59, 59)
        }.timeInMillis

        withContext(Dispatchers.IO) {
            val accountInfoDeferred = async { YouTube.accountInfo().getOrNull() }
            val topSongsDeferred = async { databaseDao.mostPlayedSongsStats(fromTimestamp, toTimeStamp = toTimestamp, limit = 5).first() }
            val topArtistsDeferred = async { databaseDao.mostPlayedArtists(fromTimestamp, toTimeStamp = toTimestamp, limit = 5).first() }
            val uniqueSongCountDeferred = async { databaseDao.getUniqueSongCountInRange(fromTimestamp, toTimestamp).first() }
            val uniqueArtistCountDeferred = async { databaseDao.getUniqueArtistCountInRange(fromTimestamp, toTimestamp).first() }
            val totalPlayTimeMsDeferred = async { databaseDao.getTotalPlayTimeInRange(fromTimestamp, toTimestamp).first() ?: 0L }

            val results = awaitAll(
                accountInfoDeferred,
                topSongsDeferred,
                topArtistsDeferred,
                uniqueSongCountDeferred,
                uniqueArtistCountDeferred,
                totalPlayTimeMsDeferred
            )

            _state.update {
                it.copy(
                    accountInfo = results[0] as AccountInfo?,
                    topSongs = results[1] as List<SongWithStats>,
                    topArtists = results[2] as List<Artist>,
                    uniqueSongCount = results[3] as Int,
                    uniqueArtistCount = results[4] as Int,
                    totalMinutes = (results[5] as Long) / 1000 / 60
                )
            }
        }

        generatePlaylistMap()
        _state.update { it.copy(isDataReady = true) }
        Log.d("WrappedManager", "Wrapped data preparation finished")
    }
}
