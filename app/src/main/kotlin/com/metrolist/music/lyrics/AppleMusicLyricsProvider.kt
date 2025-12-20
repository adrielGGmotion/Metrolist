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
import io.ktor.http.encodedPath
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

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
        Log.d("AppleMusicLyrics", "-> New request for title: '$title', artist: '$artist'")
        return try {
            val searchQuery = "$title $artist"
            val searchUrl = "https://lyrics.paxsenix.org/apple-music/search?q=${searchQuery.encodeURLQueryComponent()}"
            Log.d("AppleMusicLyrics", "  Searching with URL: $searchUrl")

            val searchResponse = client.get { url { encodedPath = "apple-music/search"; parameters.append("q", searchQuery) } }
            Log.d("AppleMusicLyrics", "  Search response status: ${searchResponse.status}")
            val responseBody = searchResponse.bodyAsText()
            Log.d("AppleMusicLyrics", "  Search response body: $responseBody")

            val searchResults = Json.decodeFromString<AppleMusicSearchResponse>(responseBody).results
            Log.d("AppleMusicLyrics", "  Received ${searchResults.size} search results")

            if (searchResults.isEmpty()) {
                Log.d("AppleMusicLyrics", "<- No results found. Failing.")
                return Result.failure(Exception("No results found"))
            }

            val bestMatch = findBestMatch(searchResults, title, artist)
            if (bestMatch == null) {
                Log.d("AppleMusicLyrics", "<- No suitable match found after scoring. Failing.")
                return Result.failure(Exception("No suitable match found"))
            }
            Log.d("AppleMusicLyrics", "  Best match found: '${bestMatch.songName}' by '${bestMatch.artistName}' (ID: ${bestMatch.id})")

            val lyricsUrl = "https://lyrics.paxsenix.org/apple-music/lyrics?id=${bestMatch.id}&ttml=false"
            Log.d("AppleMusicLyrics", "  Fetching lyrics with URL: $lyricsUrl")

            val lyricsResponse = client.get { url { encodedPath = "apple-music/lyrics"; parameters.append("id", bestMatch.id); parameters.append("ttml", "false") } }
            Log.d("AppleMusicLyrics", "  Lyrics response status: ${lyricsResponse.status}")
            val lyricsResponseBody = lyricsResponse.bodyAsText()
            Log.d("AppleMusicLyrics", "  Lyrics response body: $lyricsResponseBody")
            val lyricsData = Json.decodeFromString<AppleMusicLyricsResponse>(lyricsResponseBody)


            val (lyricsText, format) = when {
                !lyricsData.elrcMultiPerson.isNullOrEmpty() -> lyricsData.elrcMultiPerson to "elrcMultiPerson"
                !lyricsData.elrc.isNullOrEmpty() -> lyricsData.elrc to "elrc"
                !lyricsData.lrc.isNullOrEmpty() -> lyricsData.lrc to "lrc"
                else -> null to null
            }

            if (lyricsText.isNullOrEmpty() || format == null) {
                Log.d("AppleMusicLyrics", "<- No lyrics available in response. Failing.")
                return Result.failure(Exception("No lyrics found for track"))
            }
            Log.d("AppleMusicLyrics", "  Successfully fetched lyrics using format '$format'")
            // The API returns newlines as "\\n", so we need to replace them with actual newlines.
            val finalLyrics = lyricsText.replace("\\n", "\n")
            Log.d("AppleMusicLyrics", "<- Returning lyrics successfully.")
            Result.success(finalLyrics)
        } catch (e: Exception) {
            Log.e("AppleMusicLyrics", "<- An error occurred: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun findBestMatch(
        results: List<AppleMusicTrack>,
        songName: String,
        artistName: String,
        albumName: String? = null
    ): AppleMusicTrack? {
        val scoredResults = results.map { track ->
            track to calculateMatchScore(track, songName, artistName, albumName)
        }.sortedByDescending { it.second }

        Log.d("AppleMusicLyrics", "  Top 3 matches:")
        scoredResults.take(3).forEachIndexed { index, (track, score) ->
            Log.d("AppleMusicLyrics", "    ${index + 1}. Score: $score - '${track.songName}' by '${track.artistName}'")
        }

        return scoredResults.firstOrNull()?.first
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
        return name.lowercase().trim()
            .replace(Regex("\\s+(&|and|et|,)\\s+"), " & ")
            .replace(Regex("\\s+"), " ")
    }

    private fun normalizeSongName(name: String): String {
        return name.lowercase().trim()
            .replace(Regex("\\s+"), " ")
    }
}
