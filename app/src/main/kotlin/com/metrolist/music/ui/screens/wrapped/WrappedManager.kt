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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
    private val _messagePair = MutableStateFlow<MessagePair?>(null)
    val messagePair = _messagePair.asStateFlow()

    private val _totalMinutes = MutableStateFlow<Long?>(null)
    val totalMinutes = _totalMinutes.asStateFlow().filterNotNull()

    private val _accountInfo = MutableStateFlow<AccountInfo?>(null)
    val accountInfo = _accountInfo.asStateFlow()

    private val _topSongs = MutableStateFlow<List<SongWithStats>>(emptyList())
    val topSongs = _topSongs.asStateFlow()

    private val _topArtists = MutableStateFlow<List<Artist>>(emptyList())
    val topArtists = _topArtists.asStateFlow()

    private val _uniqueSongCount = MutableStateFlow(0)
    val uniqueSongCount = _uniqueSongCount.asStateFlow()

    private val _uniqueArtistCount = MutableStateFlow(0)
    val uniqueArtistCount = _uniqueArtistCount.asStateFlow()

    private val _isDataReady = MutableStateFlow(false)
    val isDataReady = _isDataReady.asStateFlow()

    private val _trackMap = MutableStateFlow<Map<WrappedScreenType, String?>>(emptyMap())
    val trackMap = _trackMap.asStateFlow()

    private val _playlistCreationState = MutableStateFlow<PlaylistCreationState>(PlaylistCreationState.Idle)
    val playlistCreationState = _playlistCreationState.asStateFlow()

    fun saveWrappedPlaylist() {
        if (_playlistCreationState.value != PlaylistCreationState.Idle) return

        scope.launch {
            _playlistCreationState.value = PlaylistCreationState.Creating
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
                        val songIds = _topSongs.value.map { it.id }
                        databaseDao.addSongToPlaylist(createdPlaylist, songIds)
                    } else {
                        Log.e("WrappedManager", "Failed to retrieve created playlist with id: $playlistId")
                    }
                }
                _playlistCreationState.value = PlaylistCreationState.Success
            } catch (e: Exception) {
                Log.e("WrappedManager", "Error saving wrapped playlist", e)
                _playlistCreationState.value = PlaylistCreationState.Idle
            }
        }
    }

    /**
     * Generates a map of screen types to song IDs for the Wrapped experience.
     * This function ensures that songs are unique across screens where possible
     * and that certain screens (like Tease/Reveal) share the same song.
     */
    private suspend fun generatePlaylistMap() {
        val topSongs = _topSongs.value
        val topArtists = _topArtists.value
        if (topSongs.isEmpty()) {
            Log.w("WrappedManager", "Cannot generate playlist map, top songs list is empty.")
            _trackMap.value = emptyMap()
            return
        }

        val playlistMap = mutableMapOf<WrappedScreenType, String>()

            // Group A: Random Track (Excluding Top 1)
            val topSong = topSongs.first()
            val randomTrack = topSongs.filter { it.id != topSong.id }.randomOrNull()?.id ?: topSongs.getOrNull(1)?.id ?: topSong.id
            playlistMap[WrappedScreenType.Welcome] = randomTrack
            playlistMap[WrappedScreenType.MinutesTease] = randomTrack
            playlistMap[WrappedScreenType.MinutesReveal] = randomTrack

            // Group B: Top 1 Song
            playlistMap[WrappedScreenType.TotalSongs] = topSong.id
            playlistMap[WrappedScreenType.TopSongReveal] = topSong.id
            playlistMap[WrappedScreenType.Top5Songs] = topSong.id

            // Group C: Top Artist's Track (Unique)
            val topArtistTrack = topArtists.firstOrNull()?.let { artist ->
                databaseDao.artistSongs(
                    artistId = artist.id,
                    sortType = ArtistSongSortType.PLAY_TIME,
                    descending = true
                ).first().firstOrNull { it.id != topSong.id }?.id
            } ?: randomTrack // Fallback to random track
            playlistMap[WrappedScreenType.TotalArtists] = topArtistTrack
            playlistMap[WrappedScreenType.TopArtistReveal] = topArtistTrack
            playlistMap[WrappedScreenType.Top5Artists] = topArtistTrack

            // Group D: Summary
            playlistMap[WrappedScreenType.End] = "2-p9DM2Xvsc"

            Log.d("WrappedManager", "Generated Playlist Map: $playlistMap")
            _trackMap.value = playlistMap
    }

    suspend fun prepare() {
        if (isDataReady.value) return
        _accountInfo.value = YouTube.accountInfo().getOrNull()

        val fromTimestamp = Calendar.getInstance().apply {
            set(WrappedConstants.YEAR, Calendar.JANUARY, 1, 0, 0, 0)
        }.timeInMillis

        val toTimestamp = Calendar.getInstance().apply {
            set(WrappedConstants.YEAR, Calendar.DECEMBER, 31, 23, 59, 59)
        }.timeInMillis

        val allSongs = databaseDao.mostPlayedSongsStats(fromTimestamp, toTimeStamp = toTimestamp, limit = -1).first()
        _topSongs.value = allSongs.take(5)
        val allArtists = databaseDao.mostPlayedArtists(fromTimestamp, toTimeStamp = toTimestamp, limit = -1).first()
        _topArtists.value = allArtists.take(5)
        _uniqueSongCount.value = databaseDao.getUniqueSongCountInRange(fromTimestamp, toTimestamp).first()
        _uniqueArtistCount.value = databaseDao.getUniqueArtistCountInRange(fromTimestamp, toTimestamp).first()

        val totalPlayTimeMs = databaseDao.getTotalPlayTimeInRange(fromTimestamp, toTimestamp).first() ?: 0L
        val totalMinutes = totalPlayTimeMs / 1000 / 60
        _totalMinutes.value = totalMinutes

        generatePlaylistMap()
        _isDataReady.value = true
    }
}
