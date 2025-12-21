package com.metrolist.music.utils

import android.content.Context
import android.util.Log
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.utils.completed
import com.metrolist.lastfm.LastFM
import com.metrolist.music.constants.LastFMUseSendLikes
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.ArtistEntity
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.db.entities.PlaylistSongMap
import com.metrolist.music.db.entities.SongEntity
import com.metrolist.music.extensions.collectLatest
import com.metrolist.music.models.toMediaMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class SyncUtils @Inject constructor(
    @ApplicationContext context: Context,
    private val database: MusicDatabase,
) {
    private val TAG = "SyncUtils"
    private val syncScope = CoroutineScope(Dispatchers.IO)

    private val isSyncingLikedSongs = MutableStateFlow(false)
    private val isSyncingLibrarySongs = MutableStateFlow(false)
    private val isSyncingUploadedSongs = MutableStateFlow(false)
    private val isSyncingLikedAlbums = MutableStateFlow(false)
    private val isSyncingUploadedAlbums = MutableStateFlow(false)
    private val isSyncingArtists = MutableStateFlow(false)
    private val isSyncingPlaylists = MutableStateFlow(false)
    private var lastfmSendLikes = false

    init {
        context.dataStore.data
            .map { it[LastFMUseSendLikes] ?: false }
            .distinctUntilChanged()
            .collectLatest(syncScope){
                lastfmSendLikes = it
            }
    }

    fun runAllSyncs() {
        syncScope.launch {
            syncLikedSongs()
            syncLibrarySongs()
            syncUploadedSongs()
            syncLikedAlbums()
            syncUploadedAlbums()
            syncArtistsSubscriptions()
            syncSavedPlaylists()
        }
    }

    fun likeSong(s: SongEntity) {
        Log.d(TAG, "likeSong: songId=${s.id}, liked=${s.liked}")
        syncScope.launch {
            YouTube.likeVideo(s.id, s.liked)
            Log.d(TAG, "likeSong: YouTube.likeVideo finished.")
            if (lastfmSendLikes) {
                val dbSong = database.song(s.id).firstOrNull()
                Log.d(TAG, "likeSong: LastFM.setLoveStatus")
                LastFM.setLoveStatus(
                    artist = dbSong?.artists?.joinToString { a -> a.name } ?: "",
                    track = s.title,
                    love = s.liked
                )
            }
        }
    }

    suspend fun syncLikedSongs() {
        if (isSyncingLikedSongs.value) return
        isSyncingLikedSongs.value = true
        Log.d(TAG, "syncLikedSongs: Starting")
        try {
            YouTube.playlist("LM").completed().onSuccess { page ->
                val remoteSongs = page.songs
                val remoteIds = remoteSongs.map { it.id }
                val localSongs = database.likedSongsByNameAsc().first()
                Log.d(TAG, "syncLikedSongs: Remote songs count: ${remoteSongs.size}, Local songs count: ${localSongs.size}")

                localSongs.filterNot { it.id in remoteIds }.forEach {
                    try {
                        Log.d(TAG, "syncLikedSongs: Removing song locally: ${it.id}")
                        database.transaction { update(it.song.localToggleLike()) }
                    } catch (e: Exception) { Log.e(TAG, "syncLikedSongs: Failed to remove song", e) }
                }

                remoteSongs.forEachIndexed { index, song ->
                    try {
                        val dbSong = database.song(song.id).firstOrNull()
                        val timestamp = LocalDateTime.now().minusSeconds(index.toLong())
                        Log.d(TAG, "syncLikedSongs: Processing remote song: ${song.id}")
                        database.transaction {
                            if (dbSong == null) {
                                Log.d(TAG, "syncLikedSongs: Inserting new liked song: ${song.id}")
                                insert(song.toMediaMetadata()) { it.copy(liked = true, likedDate = timestamp) }
                            } else if (!dbSong.song.liked || dbSong.song.likedDate != timestamp) {
                                Log.d(TAG, "syncLikedSongs: Updating existing song to liked: ${song.id}")
                                update(dbSong.song.copy(liked = true, likedDate = timestamp))
                            }
                        }
                    } catch (e: Exception) { Log.e(TAG, "syncLikedSongs: Failed to process song", e) }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "syncLikedSongs: Exception", e)
        } finally {
            isSyncingLikedSongs.value = false
            Log.d(TAG, "syncLikedSongs: Finished")
        }
    }

    suspend fun syncLibrarySongs() {
        if (isSyncingLibrarySongs.value) return
        isSyncingLibrarySongs.value = true
        Log.d(TAG, "syncLibrarySongs: Starting")
        try {
            YouTube.library("FEmusic_liked_videos").completed().onSuccess { page ->
                val remoteSongs = page.items.filterIsInstance<SongItem>().reversed()
                val remoteIds = remoteSongs.map { it.id }.toSet()
                val localSongs = database.songsByNameAsc().first()
                val feedbackTokens = mutableListOf<String>()
                Log.d(TAG, "syncLibrarySongs: Remote songs count: ${remoteSongs.size}, Local songs count: ${localSongs.size}")

                localSongs.filterNot { it.id in remoteIds }.forEach {
                    if (it.song.libraryAddToken != null && it.song.libraryRemoveToken != null) {
                        Log.d(TAG, "syncLibrarySongs: Adding feedback token for removal: ${it.id}")
                        feedbackTokens.add(it.song.libraryAddToken)
                    } else {
                        try {
                            Log.d(TAG, "syncLibrarySongs: Removing song from library locally: ${it.id}")
                            database.transaction { update(it.song.toggleLibrary()) }
                        } catch (e: Exception) { Log.e(TAG, "syncLibrarySongs: Failed to remove song from library", e) }
                    }
                }
                feedbackTokens.chunked(20).forEach {
                    Log.d(TAG, "syncLibrarySongs: Sending feedback for removal: ${it.joinToString()}")
                    YouTube.feedback(it)
                }

                remoteSongs.forEach { song ->
                    try {
                        val dbSong = database.song(song.id).firstOrNull()
                        Log.d(TAG, "syncLibrarySongs: Processing remote song: ${song.id}")
                        database.transaction {
                            if (dbSong == null) {
                                Log.d(TAG, "syncLibrarySongs: Inserting new library song: ${song.id}")
                                insert(song.toMediaMetadata()) { it.toggleLibrary() }
                            } else {
                                if (dbSong.song.inLibrary == null) {
                                    Log.d(TAG, "syncLibrarySongs: Updating existing song to inLibrary: ${song.id}")
                                    update(dbSong.song.toggleLibrary())
                                }
                                Log.d(TAG, "syncLibrarySongs: Adding library tokens for song: ${song.id}, add=${song.libraryAddToken}, remove=${song.libraryRemoveToken}")
                                addLibraryTokens(song.id, song.libraryAddToken, song.libraryRemoveToken)
                            }
                        }
                    } catch (e: Exception) { Log.e(TAG, "syncLibrarySongs: Failed to process song", e) }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "syncLibrarySongs: Exception", e)
        } finally {
            isSyncingLibrarySongs.value = false
            Log.d(TAG, "syncLibrarySongs: Finished")
        }
    }

    suspend fun syncUploadedSongs() {
        if (isSyncingUploadedSongs.value) return
        isSyncingUploadedSongs.value = true
        try {
            YouTube.library("FEmusic_library_privately_owned_tracks", tabIndex = 1).completed().onSuccess { page ->
                val remoteSongs = page.items.filterIsInstance<SongItem>().reversed()
                val remoteIds = remoteSongs.map { it.id }.toSet()
                val localSongs = database.uploadedSongsByNameAsc().first()

                localSongs.filterNot { it.id in remoteIds }.forEach { database.update(it.song.toggleUploaded()) }

                remoteSongs.forEach { song ->
                    val dbSong = database.song(song.id).firstOrNull()
                    database.transaction {
                        if (dbSong == null) {
                            insert(song.toMediaMetadata()) { it.toggleUploaded() }
                        } else if (!dbSong.song.isUploaded) {
                            update(dbSong.song.toggleUploaded())
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "syncUploadedSongs: Exception", e)
        } finally {
            isSyncingUploadedSongs.value = false
        }
    }

    suspend fun syncLikedAlbums() {
        if (isSyncingLikedAlbums.value) return
        isSyncingLikedAlbums.value = true
        Log.d(TAG, "syncLikedAlbums: Starting")
        try {
            YouTube.library("FEmusic_liked_albums").completed().onSuccess { page ->
                val remoteAlbums = page.items.filterIsInstance<AlbumItem>().reversed()
                val remoteIds = remoteAlbums.map { it.id }.toSet()
                val localAlbums = database.albumsLikedByNameAsc().first()
                Log.d(TAG, "syncLikedAlbums: Remote albums count: ${remoteAlbums.size}, Local albums count: ${localAlbums.size}")


                localAlbums.filterNot { it.id in remoteIds }.forEach {
                    Log.d(TAG, "syncLikedAlbums: Removing album locally: ${it.id}")
                    database.update(it.album.localToggleLike())
                }

                remoteAlbums.forEach { album ->
                    val dbAlbum = database.album(album.id).firstOrNull()
                    Log.d(TAG, "syncLikedAlbums: Processing remote album: ${album.id}")
                    YouTube.album(album.browseId).onSuccess { albumPage ->
                        if (dbAlbum == null) {
                            Log.d(TAG, "syncLikedAlbums: Inserting new liked album: ${album.id}")
                            database.insert(albumPage)
                            database.album(album.id).firstOrNull()?.let { newDbAlbum ->
                                database.update(newDbAlbum.album.localToggleLike())
                            }
                        } else if (dbAlbum.album.bookmarkedAt == null) {
                            Log.d(TAG, "syncLikedAlbums: Updating existing album to liked: ${album.id}")
                            database.update(dbAlbum.album.localToggleLike())
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "syncLikedAlbums: Exception", e)
        } finally {
            isSyncingLikedAlbums.value = false
            Log.d(TAG, "syncLikedAlbums: Finished")
        }
    }

    suspend fun syncUploadedAlbums() {
        if (isSyncingUploadedAlbums.value) return
        isSyncingUploadedAlbums.value = true
        try {
            YouTube.library("FEmusic_library_privately_owned_releases", tabIndex = 1).completed().onSuccess { page ->
                val remoteAlbums = page.items.filterIsInstance<AlbumItem>().reversed()
                val remoteIds = remoteAlbums.map { it.id }.toSet()
                val localAlbums = database.albumsUploadedByNameAsc().first()

                localAlbums.filterNot { it.id in remoteIds }.forEach { database.update(it.album.toggleUploaded()) }

                remoteAlbums.forEach { album ->
                    val dbAlbum = database.album(album.id).firstOrNull()
                    YouTube.album(album.browseId).onSuccess { albumPage ->
                        if (dbAlbum == null) {
                            database.insert(albumPage)
                            database.album(album.id).firstOrNull()?.let { newDbAlbum ->
                                database.update(newDbAlbum.album.toggleUploaded())
                            }
                        } else if (!dbAlbum.album.isUploaded) {
                            database.update(dbAlbum.album.toggleUploaded())
                        }
                    }.onFailure { Log.e(TAG, "syncUploadedAlbums: Failed to get album page", it) }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "syncUploadedAlbums: Exception", e)
        } finally {
            isSyncingUploadedAlbums.value = false
        }
    }

    suspend fun syncArtistsSubscriptions() {
        if (isSyncingArtists.value) return
        isSyncingArtists.value = true
        try {
            YouTube.library("FEmusic_library_corpus_artists").completed().onSuccess { page ->
                val remoteArtists = page.items.filterIsInstance<ArtistItem>()
                val remoteIds = remoteArtists.map { it.id }.toSet()
                val localArtists = database.artistsBookmarkedByNameAsc().first()

                localArtists.filterNot { it.id in remoteIds }.forEach { database.update(it.artist.localToggleLike()) }

                remoteArtists.forEach { artist ->
                    val dbArtist = database.artist(artist.id).firstOrNull()
                    database.transaction {
                        if (dbArtist == null) {
                            insert(
                                ArtistEntity(
                                    id = artist.id,
                                    name = artist.title,
                                    thumbnailUrl = artist.thumbnail,
                                    channelId = artist.channelId,
                                    bookmarkedAt = LocalDateTime.now()
                                )
                            )
                        } else if (dbArtist.artist.bookmarkedAt == null) {
                            update(dbArtist.artist.localToggleLike())
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "syncArtistsSubscriptions: Exception", e)
        } finally {
            isSyncingArtists.value = false
        }
    }

    suspend fun syncSavedPlaylists() {
        if (isSyncingPlaylists.value) return
        isSyncingPlaylists.value = true
        try {
            YouTube.library("FEmusic_liked_playlists").completed().onSuccess { page ->
                val remotePlaylists = page.items.filterIsInstance<PlaylistItem>().filterNot { it.id == "LM" || it.id == "SE" }.reversed()
                val remoteIds = remotePlaylists.map { it.id }.toSet()
                val localPlaylists = database.playlistsByNameAsc().first()

                localPlaylists.filterNot { it.playlist.browseId in remoteIds }.filterNot { it.playlist.browseId == null }.forEach { database.update(it.playlist.localToggleLike()) }

                remotePlaylists.forEach { playlist ->
                    var playlistEntity = localPlaylists.find { it.playlist.browseId == playlist.id }?.playlist
                    if (playlistEntity == null) {
                        playlistEntity = PlaylistEntity(
                            name = playlist.title,
                            browseId = playlist.id,
                            thumbnailUrl = playlist.thumbnail,
                            isEditable = playlist.isEditable,
                            bookmarkedAt = LocalDateTime.now(),
                            remoteSongCount = playlist.songCountText?.let {
                                Regex("""\d+""").find(it)?.value?.toIntOrNull()
                            },
                            playEndpointParams = playlist.playEndpoint?.params,
                            shuffleEndpointParams = playlist.shuffleEndpoint?.params,
                            radioEndpointParams = playlist.radioEndpoint?.params
                        )
                        database.insert(playlistEntity)
                    } else {
                        database.update(playlistEntity, playlist)
                    }
                    syncPlaylist(playlist.id, playlistEntity.id)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "syncSavedPlaylists: Exception", e)
        } finally {
            isSyncingPlaylists.value = false
        }
    }

    private suspend fun syncPlaylist(browseId: String, playlistId: String) {
        try {
            YouTube.playlist(browseId).completed().onSuccess { page ->
                val songs = page.songs.map(SongItem::toMediaMetadata)
                val remoteIds = songs.map { it.id }
                val localIds = database.playlistSongs(playlistId).first().sortedBy { it.map.position }.map { it.song.id }

                if (remoteIds == localIds) return@onSuccess
                if (database.playlist(playlistId).firstOrNull() == null) return@onSuccess

                database.transaction {
                    clearPlaylist(playlistId)
                    val songEntities = songs.onEach { song ->
                        if (runBlocking { database.song(song.id).firstOrNull() } == null) {
                            insert(song)
                        }
                    }
                    val playlistSongMaps = songEntities.mapIndexed { position, song ->
                        PlaylistSongMap(songId = song.id, playlistId = playlistId, position = position, setVideoId = song.setVideoId)
                    }
                    playlistSongMaps.forEach { insert(it) }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "syncPlaylist: Exception", e)
        }
    }

    suspend fun clearAllSyncedContent() {
        try {
            val likedSongs = database.likedSongsByNameAsc().first()
            val librarySongs = database.songsByNameAsc().first()
            val likedAlbums = database.albumsLikedByNameAsc().first()
            val subscribedArtists = database.artistsBookmarkedByNameAsc().first()
            val savedPlaylists = database.playlistsByNameAsc().first()

            likedSongs.forEach {
                try { database.transaction { update(it.song.copy(liked = false, likedDate = null)) } } catch (e: Exception) { Log.e(TAG, "clearAllSyncedContent: Failed to clear liked song", e) }
            }
            librarySongs.forEach {
                if (it.song.inLibrary != null) {
                    try { database.transaction { update(it.song.copy(inLibrary = null)) } } catch (e: Exception) { Log.e(TAG, "clearAllSyncedContent: Failed to clear library song", e) }
                }
            }
            likedAlbums.forEach {
                try { database.transaction { update(it.album.copy(bookmarkedAt = null)) } } catch (e: Exception) { Log.e(TAG, "clearAllSyncedContent: Failed to clear liked album", e) }
            }
            subscribedArtists.forEach {
                try { database.transaction { update(it.artist.copy(bookmarkedAt = null)) } } catch (e: Exception) { Log.e(TAG, "clearAllSyncedContent: Failed to clear subscribed artist", e) }
            }
            savedPlaylists.forEach {
                if (it.playlist.browseId != null) {
                    try { database.transaction { delete(it.playlist) } } catch (e: Exception) { Log.e(TAG, "clearAllSyncedContent: Failed to clear saved playlist", e) }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "clearAllSyncedContent: Exception", e)
        }
    }
}
