package com.metrolist.music.lyrics

import android.content.Context
import com.metrolist.music.constants.EnableAppleMusicKey
import com.metrolist.music.db.entities.LyricsEntity
import com.metrolist.music.utils.dataStore
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.net.URLEncoder

@Serializable
private data class SearchResult(
    val id: String,
    val songName: String,
    val artistName: String,
)

@Serializable
private data class PaxResponse(
    val type: String,
    val content: List<Line>,
)

@Serializable
private data class Line(
    val time: Long,
    val words: List<Word> = emptyList(),
)

@Serializable
private data class Word(
    val time: Long,
    val string: String,
)

object AppleMusicLyricsProvider : LyricsProvider {
    override val name: String = "Apple Music"

    private val client =
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                    },
                )
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 15000
            }
            defaultRequest {
                header(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36"
                )
            }
        }

    private const val PRIMARY_URL = "http://lyrics.paxsenix.dpdns.org/"
    private const val FALLBACK_URL = "https://paxsenix.alwaysdata.net/"
    private val urls = listOf(PRIMARY_URL, FALLBACK_URL)

    private val json = Json { ignoreUnknownKeys = true }

    override fun isEnabled(context: Context): Boolean {
        // This is not ideal as it blocks the thread.
        // However, the LyricsProvider interface is not suspend-friendly.
        return runBlocking {
            context.dataStore.data.map { it[EnableAppleMusicKey] ?: true }.first()
        }
    }

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
    ): Result<String> {
        val query = URLEncoder.encode("$title $artist", "UTF-8")
        var searchResults: List<SearchResult>? = null

        // --- 1. Search for the song on all available sources ---
        for (url in urls) {
            try {
                val response: String = client.get("${url}searchAppleMusic.php?q=$query").body()
                searchResults = json.decodeFromString<List<SearchResult>>(response)
                // If we got a result, we can break
                if (searchResults != null) break
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch search results from $url")
            }
        }

        if (searchResults == null) {
            return Result.failure(Exception("Failed to fetch search results from all sources."))
        }

        // --- 2. Find the correct song ID from the results ---
        val songId =
            searchResults.find {
                it.songName.equals(title, ignoreCase = true) &&
                    it.artistName.contains(artist, ignoreCase = true)
            }?.id ?: return Result.failure(Exception("Song not found in search results."))

        // --- 3. Fetch the lyrics using the ID, with fallback ---
        var lyricsResponse: String? = null
        for (url in urls) {
            try {
                lyricsResponse = client.get("${url}getAppleMusicLyrics.php?id=$songId").body()
                // If we got a non-blank response, break
                if (!lyricsResponse.isNullOrBlank()) break
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch lyrics for id $songId from $url")
            }
        }

        if (lyricsResponse.isNullOrBlank()) {
            return Result.failure(Exception("Failed to fetch lyrics from all sources for id $songId."))
        }

        // --- 4. Parse the response ---
        val lrc =
            try {
                val paxResponse = json.decodeFromString<PaxResponse>(lyricsResponse)
                when (paxResponse.type) {
                    "Syllable", "Line" -> formatLrc(paxResponse.content)
                    else -> LyricsEntity.LYRICS_NOT_FOUND
                }
            } catch (e: Exception) {
                try {
                    val lines = json.decodeFromString<List<Line>>(lyricsResponse)
                    formatLrc(lines)
                } catch (e2: Exception) {
                    return Result.failure(Exception("Failed to parse lyrics response: $lyricsResponse"))
                }
            }

        return if (lrc.isBlank()) {
            Result.failure(Exception("Lyrics content is empty."))
        } else {
            Result.success(lrc)
        }
    }

    private fun formatLrc(lines: List<Line>): String {
        val lrcBuilder = StringBuilder()
        for (line in lines) {
            val minutes = line.time / 60000
            val seconds = (line.time % 60000) / 1000
            val millis = line.time % 1000
            lrcBuilder.append(String.format("[%02d:%02d.%02d]", minutes, seconds, millis / 10))

            if (line.words.isNotEmpty()) {
                val lineText = line.words.joinToString(separator = "") { it.string }
                lrcBuilder.append(lineText.trim())

                for (word in line.words) {
                    val wordMinutes = word.time / 60000
                    val wordSeconds = (word.time % 60000) / 1000
                    val wordMillis = word.time % 1000
                    lrcBuilder.append(String.format("<%02d:%02d.%02d>", wordMinutes, wordSeconds, wordMillis / 10))
                    lrcBuilder.append(word.string)
                }
            }
            lrcBuilder.append("\n")
        }
        return lrcBuilder.toString()
    }
}
