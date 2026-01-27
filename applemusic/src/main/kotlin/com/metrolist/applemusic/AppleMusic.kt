package com.metrolist.applemusic

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class AppleMusicSearchResult(
    @SerialName("songName") val songName: String?,
    @SerialName("artistName") val artistName: String?,
    @SerialName("albumName") val albumName: String?,
    @SerialName("id") val id: String?,
)

@Serializable
private data class AppleMusicLyricsResponse(
    @SerialName("elrcMultiPerson") val elrcMultiPerson: String?,
    @SerialName("elrc") val elrc: String?,
    @SerialName("lrc") val lrc: String?,
)

object AppleMusic {
    private val client by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }
    }

    suspend fun fetchLyrics(
        title: String,
        artist: String,
        userAgent: String,
    ): Result<String> {
        try {
            val searchQuery = "$title $artist"
            val searchUrl = URLBuilder("https://lyrics.paxsenix.org/apple-music/search").apply {
                parameters.append("q", searchQuery)
            }.build()

            val searchResults = client.get(searchUrl) {
                header("User-Agent", userAgent)
            }.body<List<AppleMusicSearchResult>>()

            if (searchResults.isEmpty()) {
                return Result.failure(Exception("No results found on Apple Music"))
            }

            val bestMatch = findBestMatch(searchResults, title, artist)
                ?: return Result.failure(Exception("No suitable match found on Apple Music"))

            val trackId = bestMatch.id
                ?: return Result.failure(Exception("Track ID is missing for the best match"))

            val lyricsUrl = URLBuilder("https://lyrics.paxsenix.org/apple-music/lyrics").apply {
                parameters.append("id", trackId)
                parameters.append("ttml", "false")
            }.build()

            val lyricsResponse = client.get(lyricsUrl) {
                header("User-Agent", userAgent)
            }.body<AppleMusicLyricsResponse>()

            val lyricsText = lyricsResponse.elrcMultiPerson
                ?: lyricsResponse.elrc
                ?: lyricsResponse.lrc
                ?: return Result.failure(Exception("No lyrics found for the selected track"))

            return Result.success(lyricsText)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    private fun normalizeArtistName(name: String): String {
        var normalized = name.lowercase().trim()
        normalized = normalized.replace(Regex("\\s+(&|and|et|,)\\s+"), " & ")
        return normalized.replace(Regex("\\s+"), " ")
    }

    private fun normalizeSongName(name: String): String {
        return name.lowercase().trim().replace(Regex("\\s+"), " ")
    }

    private fun calculateMatchScore(track: AppleMusicSearchResult, songName: String, artistName: String): Double {
        var score = 0.0
        val songNorm = normalizeSongName(songName)
        val artistNorm = normalizeArtistName(artistName)

        val trackSongNorm = normalizeSongName(track.songName ?: "")
        val trackArtistNorm = normalizeArtistName(track.artistName ?: "")

        if (trackSongNorm == songNorm) score += 100
        else if (songNorm in trackSongNorm || trackSongNorm in songNorm) score += 50
        else {
            val songWords = songNorm.split(" ").toSet()
            val trackSongWords = trackSongNorm.split(" ").toSet()
            val commonWords = songWords.intersect(trackSongWords)
            if (commonWords.isNotEmpty()) {
                score += 25 * (commonWords.size.toDouble() / songWords.size.coerceAtLeast(1))
            }
        }

        if (trackArtistNorm == artistNorm) score += 100
        else if (artistNorm in trackArtistNorm || trackArtistNorm in artistNorm) score += 50
        else {
            val artistWords = artistNorm.split(" ").toSet()
            val trackArtistWords = trackArtistNorm.split(" ").toSet()
            val commonWords = artistWords.intersect(trackArtistWords)
            if (commonWords.isNotEmpty()) {
                score += 25 * (commonWords.size.toDouble() / artistWords.size.coerceAtLeast(1))
            }
        }

        return score
    }

    private fun findBestMatch(
        results: List<AppleMusicSearchResult>,
        songName: String,
        artistName: String,
    ): AppleMusicSearchResult? {
        if (results.isEmpty()) return null

        return results.maxByOrNull { calculateMatchScore(it, songName, artistName) }
    }
}
