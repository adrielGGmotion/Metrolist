package com.metrolist.apple

import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URLEncoder

object AppleMusic {
    private val client by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        isLenient = true
                        ignoreUnknownKeys = true
                    }
                )
            }
        }
    }

    private const val BASE_URL = "http://lyrics.paxsenix.dpdns.org/"

    suspend fun getLyrics(title: String, artist: String): Result<String> = runCatching {
        val search = URLEncoder.encode("$title $artist", "UTF-8")
        val searchResponse = client.get("$BASE_URL/searchAppleMusic.php?q=$search")
        val searchResult = searchResponse.body<List<AppleSearchResponse>>().firstOrNull()
            ?: throw IllegalStateException("No results found")

        val lyricsResponse = client.get("$BASE_URL/getAppleMusicLyrics.php?id=${searchResult.id}")
        val lyricsBody = lyricsResponse.bodyAsText()

        formatWordByWordLyrics(lyricsBody) ?: throw IllegalStateException("Lyrics unavailable")
    }

    private fun formatWordByWordLyrics(apiResponse: String): String? {
        return try {
            val data = Json.decodeFromString<PaxResponse>(apiResponse)
            if (data.content.isNullOrEmpty()) {
                return null
            }

            val lines = data.content
            when (data.type) {
                "Syllable" -> formatSyllableLyrics(lines).dropLast(1)
                "Line" -> formatLineLyrics(lines).dropLast(1)
                else -> null
            }
        } catch (e: Exception) {
            val data = Json.decodeFromString<List<PaxLyrics>>(apiResponse)
            formatSyllableLyrics(data)
        }
    }

    private fun formatSyllableLyrics(lyrics: List<PaxLyrics>): String {
        val syncedLyrics = StringBuilder()
        for (line in lyrics) {
            syncedLyrics.append("[${line.timestamp.toLrcTimestamp()}]")
            for (syllable in line.text) {
                val formattedBeginTimestamp = "<${syllable.timestamp!!.toLrcTimestamp()}>"
                syncedLyrics.append(formattedBeginTimestamp)
                syncedLyrics.append(syllable.text)
                if (!syllable.part) {
                    syncedLyrics.append(" ")
                }
            }
            syncedLyrics.append("\n")
        }
        return syncedLyrics.toString()
    }

    private fun formatLineLyrics(lyrics: List<PaxLyrics>): String {
        val syncedLyrics = StringBuilder()
        for (line in lyrics) {
            syncedLyrics.append("[${line.timestamp.toLrcTimestamp()}]${line.text[0].text}\n")
        }
        return syncedLyrics.toString()
    }

    private fun Double.toLrcTimestamp(): String {
        val totalSeconds = this.toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val milliseconds = ((this - totalSeconds) * 1000).toInt()
        return String.format("%02d:%02d.%03d", minutes, seconds, milliseconds)
    }
}

@Serializable
private data class AppleSearchResponse(
    val id: Long,
    val songName: String,
    val artistName: String,
    val artwork: String,
    val url: String
)

@Serializable
private data class PaxResponse(
    val type: String,
    val content: List<PaxLyrics>?
)

@Serializable
private data class PaxLyrics(
    val timestamp: Double,
    val text: List<PaxSyllable>,
    val oppositeTurn: Boolean,
    val background: Boolean,
    val backgroundText: List<PaxSyllable>
)

@Serializable
private data class PaxSyllable(
    val timestamp: Double?,
    val endtime: Double?,
    val text: String,
    val part: Boolean
)
