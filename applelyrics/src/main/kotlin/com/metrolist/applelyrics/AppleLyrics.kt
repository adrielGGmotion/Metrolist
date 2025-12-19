package com.metrolist.applelyrics

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.appendPathSegments
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber

object AppleLyrics {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    private const val baseUrl = "https://lyrics.paxsenix.org/"

    suspend fun getLyrics(
        title: String,
        artist: String,
        albumName: String,
        version: String
    ): Result<String> {
        Timber.d("Searching for lyrics for '$title' by '$artist'")
        val searchResult = searchSong(title, artist, albumName, version)

        return searchResult.fold(
            onSuccess = { song ->
                if (song != null) {
                    Timber.d("Found song: ${song.songName}, id: ${song.id}")
                    fetchLyrics(song.id, version)
                } else {
                    Timber.d("No song found for '$title' by '$artist'")
                    Result.failure(Exception("No song found"))
                }
            },
            onFailure = {
                Timber.e(it, "Failed to search for song")
                Result.failure(it)
            }
        )
    }

    private suspend fun searchSong(
        title: String,
        artist: String,
        albumName: String,
        version: String
    ): Result<Song?> {
        val query = "$title $artist $albumName"
        return try {
            Timber.d("Making search request to: $baseUrl")
            val response: List<Song> = client.get(baseUrl) {
                url {
                    appendPathSegments("apple-music/search")
                    parameters.append("q", query)
                }
                header("User-Agent", "metrolist/$version")
            }.body()
            Timber.d("Search successful, found ${response.size} results.")
            Result.success(findBestMatch(response, title, artist, albumName))
        } catch (e: Exception) {
            Timber.e(e, "Failed to search song")
            Result.failure(e)
        }
    }

    private fun findBestMatch(
        results: List<Song>,
        songName: String,
        artistName: String,
        albumName: String?
    ): Song? {
        if (results.isEmpty()) return null

        val songLower = songName.lowercase().trim()
        val artistLower = artistName.lowercase().trim()
        val albumLower = albumName?.lowercase()?.trim()

        for (track in results) {
            val trackSong = track.songName.lowercase().trim()
            val trackArtist = track.artistName.lowercase().trim()
            val trackAlbum = track.albumName.lowercase().trim()

            if (trackSong == songLower && trackArtist == artistLower) {
                if (albumLower != null && trackAlbum == albumLower) {
                    return track
                } else if (albumLower == null) {
                    return track
                }
            }
        }
        return results.firstOrNull()
    }

    private suspend fun fetchLyrics(id: String, version: String): Result<String> {
        return try {
            Timber.d("Fetching lyrics from: $baseUrl")
            val response: LyricsResponse = client.get(baseUrl) {
                url {
                    appendPathSegments("apple-music/lyrics")
                    parameters.append("id", id)
                    parameters.append("ttml", "false")
                }
                header("User-Agent", "metrolist/$version")
            }.body()

            val lyricsText = response.elrcMultiPerson
                ?: response.elrc
                ?: response.lrc

            if (lyricsText != null && lyricsText.isNotBlank()) {
                Timber.d("Successfully fetched lyrics.")
                Result.success(lyricsText.replace("\\n", "\n"))
            } else {
                Result.failure(Exception("Empty lyrics response"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch lyrics")
            Result.failure(e)
        }
    }
}

@Serializable
data class Song(
    val id: String,
    val songName: String,
    val artistName: String,
    val albumName: String
)

@Serializable
data class LyricsResponse(
    val lrc: String? = null,
    val elrc: String? = null,
    val elrcMultiPerson: String? = null
)
