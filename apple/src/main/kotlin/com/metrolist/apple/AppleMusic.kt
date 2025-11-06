package com.metrolist.apple

import io.ktor.client.HttpClient
import io.ktor.client.statement.bodyAsText
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.URLBuilder
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.concurrent.TimeUnit

object AppleMusic {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun getLyrics(title: String, artist: String): String? {
        return try {
            val searchQuery = "$title $artist"
            Timber.d("AppleMusic: Searching for '$searchQuery'")
            val searchUrl = URLBuilder("http://lyrics.paxsenix.dpdns.org/searchAppleMusic.php").apply {
                parameters.append("q", searchQuery)
            }.build()

            val searchResponse = client.get(searchUrl).bodyAsText()
            val searchResults = json.decodeFromString<List<AppleSearchResponse>>(searchResponse)
            val bestMatchId = searchResults.firstOrNull()?.id ?: run {
                Timber.d("AppleMusic: No search results found")
                return null
            }

            Timber.d("AppleMusic: Fetching lyrics for id '$bestMatchId'")
            val lyricsResponse = client.get("http://lyrics.paxsenix.dpdns.org/getAppleMusicLyrics.php?id=$bestMatchId").bodyAsText()
            val paxResponse = json.decodeFromString<PaxResponse>(lyricsResponse)

            paxResponse.content?.let { formatSyllableLyrics(it) }
        } catch (e: Exception) {
            Timber.e(e, "AppleMusic: Error getting lyrics")
            null
        }
    }

    private fun formatSyllableLyrics(lyrics: List<PaxLyrics>): String {
        val syncedLyrics = StringBuilder()
        for (line in lyrics) {
            syncedLyrics.append("[${line.timestamp.toLrcTimestamp()}]")
            syncedLyrics.append("v1:")

            for (syllable in line.text) {
                val formattedBeginTimestamp = "<${syllable.timestamp.toLrcTimestamp()}>"
                val formattedEndTimestamp = "<${syllable.endtime.toLrcTimestamp()}>"
                if (!syncedLyrics.endsWith(formattedBeginTimestamp))
                    syncedLyrics.append(formattedBeginTimestamp)
                syncedLyrics.append(syllable.text)
                if (!syllable.part)
                    syncedLyrics.append(" ")
                syncedLyrics.append(formattedEndTimestamp)
            }
            syncedLyrics.append("\n")
        }
        return syncedLyrics.toString()
    }

    private fun Int?.toLrcTimestamp(): String {
        if (this == null) return ""
        val minutes = TimeUnit.MILLISECONDS.toMinutes(this.toLong())
        val seconds = TimeUnit.MILLISECONDS.toSeconds(this.toLong()) % 60
        val milliseconds = this % 1000
        return String.format("%02d:%02d.%03d", minutes, seconds, milliseconds)
    }
}

@Serializable
data class AppleSearchResponse(
    val id: String,
    val songName: String,
    val artistName: String,
    val artwork: String,
    val url: String
)

@Serializable
data class PaxResponse(
    val type: String,
    val content: List<PaxLyrics>?
)

@Serializable
data class PaxLyrics(
    val text: List<PaxLyricsLineDetails>,
    val timestamp: Int,
    val oppositeTurn: Boolean,
    val background: Boolean,
    val backgroundText: List<PaxLyricsLineDetails>,
    val endtime: Int
)

@Serializable
data class PaxLyricsLineDetails(
    val text: String,
    val part: Boolean,
    val timestamp: Int?,
    val endtime: Int?
)
