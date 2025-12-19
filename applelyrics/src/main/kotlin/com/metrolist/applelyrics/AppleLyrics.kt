package com.metrolist.applelyrics

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.appendPathSegments
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import kotlin.math.min

object AppleLyrics {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    private const val BASE_URL = "https://lyrics.paxsenix.org/"
    private const val ARTIST_WEIGHT = 0.5
    private const val TITLE_WEIGHT = 0.4
    private const val ALBUM_WEIGHT = 0.1
    private const val EXACT_TITLE_BONUS = 0.1
    private const val EXACT_ARTIST_BONUS = 0.05
    private const val CONFIDENCE_THRESHOLD = 0.7

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
            Timber.d("Making search request to: $BASE_URL")
            val response: List<Song> = client.get(BASE_URL) {
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
        val albumLower = albumName?.lowercase()?.trim() ?: ""
        val cleanedSongLower = cleanTitle(songLower)

        val bestMatch = results
            .map { track ->
                val trackSongLower = track.songName.lowercase().trim()
                val trackArtistLower = track.artistName.lowercase().trim()
                val trackAlbumLower = track.albumName.lowercase().trim()
                val cleanedTrackSongLower = cleanTitle(trackSongLower)

                val artistSimilarity = calculateSimilarity(artistLower, trackArtistLower)
                val titleSimilarity = calculateSimilarity(cleanedSongLower, cleanedTrackSongLower)
                val albumSimilarity = if (albumLower.isNotEmpty() && trackAlbumLower.isNotEmpty()) {
                    calculateSimilarity(albumLower, trackAlbumLower)
                } else {
                    1.0
                }

                var score = (artistSimilarity * ARTIST_WEIGHT) +
                        (titleSimilarity * TITLE_WEIGHT) +
                        (albumSimilarity * ALBUM_WEIGHT)

                if (songLower == trackSongLower) {
                    score += EXACT_TITLE_BONUS
                }
                if (artistLower == trackArtistLower) {
                    score += EXACT_ARTIST_BONUS
                }

                Pair(track, score)
            }
            .maxByOrNull { it.second }

        Timber.d("Best match for '$songName' is '${bestMatch?.first?.songName}' with score ${bestMatch?.second}")

        return if (bestMatch != null && bestMatch.second >= CONFIDENCE_THRESHOLD) {
            bestMatch.first
        } else {
            null
        }
    }

    private fun calculateSimilarity(s1: String, s2: String): Double {
        val maxLength = maxOf(s1.length, s2.length)
        if (maxLength == 0) return 1.0 // Both are empty, perfect match
        return 1.0 - (levenshteinDistance(s1, s2) / maxLength.toDouble())
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) {
            for (j in 0..s2.length) {
                when {
                    i == 0 -> dp[i][j] = j
                    j == 0 -> dp[i][j] = i
                    else -> {
                        val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                        dp[i][j] = minOf(
                            dp[i - 1][j] + 1,
                            dp[i][j - 1] + 1,
                            dp[i - 1][j - 1] + cost
                        )
                    }
                }
            }
        }
        return dp[s1.length][s2.length]
    }

    private fun cleanTitle(title: String): String {
        return title.replace(Regex("\\s*\\(.*?\\)|\\s*\\[.*?]"), "").trim()
    }

    private suspend fun fetchLyrics(id: String, version: String): Result<String> {
        return try {
            Timber.d("Fetching lyrics from: $BASE_URL")
            val response: LyricsResponse = client.get(BASE_URL) {
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
