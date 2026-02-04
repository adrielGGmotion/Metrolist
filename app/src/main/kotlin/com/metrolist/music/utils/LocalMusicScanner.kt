package com.metrolist.music.utils

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.AlbumArtistMap
import com.metrolist.music.db.entities.AlbumEntity
import com.metrolist.music.db.entities.ArtistEntity
import com.metrolist.music.db.entities.SongAlbumMap
import com.metrolist.music.db.entities.SongArtistMap
import com.metrolist.music.db.entities.SongEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalMusicScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase
) {
    private val supportedExtensions = setOf("mp3", "m4a", "flac", "wav", "ogg", "opus")

    suspend fun scanLocalFiles(folderUris: Set<String>) = withContext(Dispatchers.IO) {
        Timber.d("Scanning local files from ${folderUris.size} folders")

        val allSongs = mutableListOf<LocalSongData>()

        folderUris.forEach { uriString ->
            try {
                val uri = Uri.parse(uriString)
                val folder = DocumentFile.fromTreeUri(context, uri)
                if (folder != null && folder.exists()) {
                    scanFolder(folder, allSongs)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error scanning folder: $uriString")
            }
        }

        saveToDatabase(allSongs)
    }

    private fun scanFolder(folder: DocumentFile, songs: MutableList<LocalSongData>) {
        folder.listFiles().forEach { file ->
            if (file.isDirectory) {
                scanFolder(file, songs)
            } else {
                val ext = file.name?.substringAfterLast('.', "")?.lowercase()
                if (ext in supportedExtensions) {
                    processFile(file)?.let { songs.add(it) }
                }
            }
        }
    }

    private fun processFile(file: DocumentFile): LocalSongData? {
        val uri = file.uri
        val retriever = MediaMetadataRetriever()
        return try {
            // DocumentFile uri works with MediaMetadataRetriever setDataSource(Context, Uri)
            retriever.setDataSource(context, uri)

            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: file.name?.substringBeforeLast('.') ?: "Unknown Title"
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown Artist"
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Unknown Album"
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L
            val yearStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
            val year = yearStr?.toIntOrNull()
            val trackNumberStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
            val trackNumber = trackNumberStr?.split("/")?.firstOrNull()?.toIntOrNull() ?: 0

            LocalSongData(
                uri = uri.toString(),
                title = title,
                artist = artist,
                album = album,
                durationSeconds = (durationMs / 1000).toInt(),
                year = year,
                trackNumber = trackNumber
            )
        } catch (e: Exception) {
            Timber.e(e, "Error processing file: ${file.uri}")
            null
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private suspend fun saveToDatabase(songs: List<LocalSongData>) {
        songs.forEach { songData ->
            try {
                database.withTransaction {
                    // 1. Insert/Update Artist
                    val artistId = "local:${songData.artist}"
                    val existingArtist = database.artist(artistId).firstOrNull()?.artist

                    if (existingArtist == null) {
                        database.insert(
                            ArtistEntity(
                                id = artistId,
                                name = songData.artist,
                                isLocal = true,
                                lastUpdateTime = LocalDateTime.now()
                            )
                        )
                    }

                    // 2. Insert/Update Album
                    val albumId = "local:${songData.album}:${songData.artist}"
                    val existingAlbum = database.album(albumId).firstOrNull()

                    if (existingAlbum == null) {
                        database.insert(
                            AlbumEntity(
                                id = albumId,
                                title = songData.album,
                                year = songData.year,
                                songCount = 1,
                                duration = songData.durationSeconds,
                                isLocal = true,
                                lastUpdateTime = LocalDateTime.now()
                            )
                        )
                        // Map Album to Artist
                         database.insert(
                            AlbumArtistMap(
                                albumId = albumId,
                                artistId = artistId,
                                order = 0
                            )
                        )
                    }

                    // 3. Insert/Update Song
                    val songId = songData.uri
                    val existingSong = database.getSongById(songId)

                    val songEntity = SongEntity(
                        id = songId,
                        title = songData.title,
                        duration = songData.durationSeconds,
                        albumId = albumId,
                        albumName = songData.album,
                        year = songData.year,
                        isLocal = true,
                        inLibrary = LocalDateTime.now(), // Auto-add to library
                        dateModified = LocalDateTime.now()
                    )

                    if (existingSong == null) {
                        database.insert(songEntity)
                    } else {
                        // Preserve user data
                        database.update(songEntity.copy(
                            liked = existingSong.song.liked,
                            likedDate = existingSong.song.likedDate,
                            totalPlayTime = existingSong.song.totalPlayTime,
                            inLibrary = existingSong.song.inLibrary ?: LocalDateTime.now() // Ensure it stays in library
                        ))
                    }

                    // 4. Map Song to Artist
                    database.insert(
                        SongArtistMap(
                            songId = songId,
                            artistId = artistId,
                            position = 0
                        )
                    )

                    // 5. Map Song to Album
                    database.insert(
                        SongAlbumMap(
                            songId = songId,
                            albumId = albumId,
                            index = songData.trackNumber
                        )
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error saving song to DB: ${songData.title}")
            }
        }
    }

    data class LocalSongData(
        val uri: String,
        val title: String,
        val artist: String,
        val album: String,
        val durationSeconds: Int,
        val year: Int?,
        val trackNumber: Int
    )
}
