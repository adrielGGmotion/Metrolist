package com.metrolist.music.lyrics

import android.content.Context
import android.util.Log
import com.metrolist.music.BuildConfig
import com.metrolist.music.constants.EnableAppleMusicKey
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLProtocol
import io.ktor.http.encodeURLQueryComponent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.util.regex.Pattern

object AppleMusicLyricsProvider : LyricsProvider {
    override val name = "Apple Music"

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
        defaultRequest {
            url {
                protocol = URLProtocol.HTTPS
                host = "lyrics.paxsenix.org"
            }
            headers.append("User-Agent", "MetroList/${BuildConfig.VERSION_NAME}")
        }
    }

    override fun isEnabled(context: Context): Boolean =
        context.dataStore[EnableAppleMusicKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int
    ): Result<String> {
        return try {
            val searchQuery = "$title $artist"
            Log.d("AppleMusicLyrics", "Searching for: $searchQuery")
            val searchResponse = client.get("apple-music/search") {
                url {
                    parameters.append("q", searchQuery)
                }
            }
            Log.d("AppleMusicLyrics", "Search response: ${searchResponse.status}")
            val responseBody = searchResponse.bodyAsText()
            Log.d("AppleMusicLyrics", "Search response body: $responseBody")

            val searchResults = Json.decodeFromString<AppleMusicSearchResponse>(responseBody).results
            Log.d("AppleMusicLyrics", "Received ${searchResults.size} search results")

            if (searchResults.isEmpty()) {
                return Result.failure(Exception("No results found"))
            }

            val bestMatch = findBestMatch(searchResults, title, artist)
                ?: return Result.failure(Exception("No suitable match found"))
            Log.d("AppleMusicLyrics", "Best match: ${bestMatch.songName} by ${bestMatch.artistName}")

            val lyricsResponse = client.get("apple-music/lyrics") {
                url {
                    parameters.append("id", bestMatch.id)
                    parameters.append("ttml", "false")
                }
            }
            Log.d("AppleMusicLyrics", "Lyrics response: ${lyricsResponse.status}")
            val lyricsData = lyricsResponse.body<AppleMusicLyricsResponse>()

            val lyricsText = lyricsData.elrcMultiPerson ?: lyricsData.elrc ?: lyricsData.lrc
            if (lyricsText.isNullOrEmpty()) {
                Log.d("AppleMusicLyrics", "No lyrics available for this track.")
                return Result.failure(Exception("No lyrics found for track"))
            }
            Log.d("AppleMusicLyrics", "Successfully fetched lyrics")
            // The API returns newlines as "\\n", so we need to replace them with actual newlines.
            Result.success(lyricsText.replace("\\n", "\n"))
        } catch (e: Exception) {
            Log.e("AppleMusicLyrics", "Error fetching lyrics", e)
            Result.failure(e)
        }
    }

    private fun findBestMatch(
        results: List<AppleMusicTrack>,
        songName: String,
        artistName: String,
        albumName: String? = null
    ): AppleMusicTrack? {
        return results.maxByOrNull { track ->
            calculateMatchScore(track, songName, artistName, albumName)
        }
    }

    private fun calculateMatchScore(
        track: AppleMusicTrack,
        songName: String,
        artistName: String,
        albumName: String? = null
    ): Double {
        var score = 0.0

        val songNorm = normalizeSongName(songName)
        val artistNorm = normalizeArtistName(artistName)
        val albumNorm = albumName?.let { normalizeSongName(it) }

        val trackSongNorm = normalizeSongName(track.songName)
        val trackArtistNorm = normalizeArtistName(track.artistName)
        val trackAlbumNorm = normalizeSongName(track.albumName)

        if (trackSongNorm == songNorm) {
            score += 100
        } else if (songNorm in trackSongNorm || trackSongNorm in songNorm) {
            score += 50
        } else {
            val songWords = songNorm.split(" ").toSet()
            val trackSongWords = trackSongNorm.split(" ").toSet()
            val commonWords = songWords.intersect(trackSongWords)
            if (commonWords.isNotEmpty()) {
                score += 25 * (commonWords.size.toDouble() / songWords.size.coerceAtLeast(1))
            }
        }
        if (trackArtistNorm == artistNorm) {
            score += 100
        } else if (artistNorm in trackArtistNorm || trackArtistNorm in artistNorm) {
            score += 50
        } else {
            val artistWords = artistNorm.split(" ").toSet()
            val trackArtistWords = trackArtistNorm.split(" ").toSet()
            val commonWords = artistWords.intersect(trackArtistWords)
            if (commonWords.isNotEmpty()) {
                score += 25 * (commonWords.size.toDouble() / artistWords.size.coerceAtLeast(1))
            }
        }
        if (albumNorm != null) {
            if (trackAlbumNorm == albumNorm) {
                score += 50
            } else if (albumNorm in trackAlbumNorm || trackAlbumNorm in albumNorm) {
                score += 25
            }
        }
        return score
    }

    private fun normalizeArtistName(name: String): String {
        var normalized = name.lowercase().trim()
        normalized = Regex("\\s+(&|and|et|,)\\s+").replace(normalized, " & ")
        normalized = Regex("\\s+").replace(normalized, " ")
        return normalized
    }

    private fun normalizeSongName(name: String): String {
        var normalized = name.lowercase().trim()
        normalized = Regex("\\s+").replace(normalized, " ")
        return normalized
    }
}
