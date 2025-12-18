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
            })
        }
        defaultRequest {
            header("User-Agent", "Metrolist/1.0")
        }
    }

    private const val baseUrl = "https://lyrics.paxsenix.org/"

    suspend fun getLyrics(title: String, artist: String): Result<String> {
        Timber.d("Searching for lyrics for '$title' by '$artist'")
        val searchResult = searchSong(title, artist)

        return searchResult.fold(
            onSuccess = { song ->
                if (song != null) {
                    Timber.d("Found song: ${song.songName}, id: ${song.id}")
                    fetchLyrics(song.id)
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

    private suspend fun searchSong(title: String, artist: String): Result<Song?> {
        val query = "$title $artist"
        return try {
            Timber.d("Making search request to: $baseUrl")
            val response: List<Song> = client.get(baseUrl) {
                url {
                    appendPathSegments("searchAppleMusic.php")
                    parameters.append("q", query)
                }
            }.body()
            Timber.d("Search successful, found ${response.size} results.")
            // The user mentioned that the first result is not always the correct one.
            // For now, we will just take the first one, but this might need to be improved later.
            Result.success(response.firstOrNull())
        } catch (e: Exception) {
            Timber.e(e, "Failed to search song")
            Result.failure(e)
        }
    }

    private suspend fun fetchLyrics(id: String): Result<String> {
        return try {
            Timber.d("Fetching lyrics from: $baseUrl")
            val response: String = client.get(baseUrl) {
                url {
                    appendPathSegments("getAppleMusicLyrics.php")
                    parameters.append("id", id)
                }
            }.body()
            if (response.isNotBlank()) {
                Timber.d("Successfully fetched lyrics.")
                Result.success(response)
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
