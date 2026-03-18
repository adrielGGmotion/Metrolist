package com.metrolist.simpmusic

import com.metrolist.simpmusic.models.LyricsData
import com.metrolist.simpmusic.models.SimpMusicApiResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.math.abs

object SimpMusicLyrics {
    private const val BASE_URL = "https://api-lyrics.simpmusic.org/v1/"

    private val client by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        isLenient = true
                        ignoreUnknownKeys = true
                        explicitNulls = false
                    },
                )
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 15000
                connectTimeoutMillis = 10000
                socketTimeoutMillis = 15000
            }

            defaultRequest {
                url(BASE_URL)
                header(HttpHeaders.Accept, "application/json")
                header(HttpHeaders.UserAgent, "SimpMusicLyrics/1.0")
                header(HttpHeaders.ContentType, "application/json")
            }

            expectSuccess = false
        }
    }

    suspend fun getLyricsByVideoId(videoId: String): List<LyricsData> = runCatching {
        val response = client.get(BASE_URL + videoId)
        
        if (response.status == HttpStatusCode.OK) {
            val apiResponse = response.body<SimpMusicApiResponse>()
            if (apiResponse.success) {
                apiResponse.data
            } else {
                emptyList()
            }
        } else {
            emptyList()
        }
    }.getOrDefault(emptyList())

    suspend fun getLyrics(
        videoId: String,
        duration: Int = 0,
    ): Result<String> = runCatching {
        val tracks = getLyricsByVideoId(videoId)
        
        if (tracks.isEmpty()) {
            throw IllegalStateException("Lyrics unavailable")
        }

        // Filter tracks that match duration within tolerance (10 seconds)
        val validTracks = if (duration > 0) {
            tracks.filter { track ->
                abs((track.duration ?: 0) - duration) <= 10
            }
        } else {
            tracks
        }

        if (validTracks.isEmpty()) {
            throw IllegalStateException("Lyrics unavailable")
        }

        val bestMatch = if (duration > 0 && validTracks.size > 1) {
            validTracks.minByOrNull { track ->
                abs((track.duration ?: 0) - duration)
            }
        } else {
            validTracks.firstOrNull()
        }

        // Prioritize richSyncLyrics for word-by-word sync, then syncedLyrics, then plainLyrics
        val rawLyrics = bestMatch?.richSyncLyrics?.takeIf { it.isNotBlank() }
            ?: bestMatch?.syncedLyrics?.takeIf { it.isNotBlank() }
            ?: bestMatch?.plainLyrics?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Lyrics unavailable")
        
        cleanLyrics(rawLyrics)
    }
    
    /**
     * Clean lyrics - remove credits and normalize format
     */
    private fun cleanLyrics(lyrics: String): String {
        val lines = lyrics.lines()
        
        // Filter out credit lines - these are usually short lines at the end with specific patterns
        // Pattern: [00:00.00] Synced by Noob.exe or similar credit lines
        val cleaned = lines.filter { line ->
            val lower = line.lowercase().trim()
            
            // Skip lines that are clearly credits (short lines with credit patterns)
            val isCreditLine = lower.startsWith("synced by") ||
                lower.startsWith("lyrics by") ||
                lower.startsWith("noob.exe") ||
                lower.startsWith("lyrics powered by") ||
                (lower.startsWith("[") && lower.contains("noob.exe")) ||
                (lower.startsWith("[") && lower.contains("synced by") && line.length < 40)
            
            !isCreditLine
        }
        
        return cleaned.joinToString("\n").trim()
    }

    suspend fun getAllLyrics(
        videoId: String,
        duration: Int = 0,
        callback: (String) -> Unit,
    ) {
        val tracks = getLyricsByVideoId(videoId)
        var count = 0
        var plain = 0

        val sortedTracks = if (duration > 0) {
            tracks.sortedBy { abs((it.duration ?: 0) - duration) }
        } else {
            tracks
        }

        sortedTracks.forEach { track ->
            if (count <= 4) {
                // Check duration match - relaxed to 10 seconds or skip if duration is 0
                val durationMatch = duration <= 0 || abs((track.duration ?: 0) - duration) <= 10

                // Prioritize richSyncLyrics for word-by-word sync
                if (track.richSyncLyrics != null && track.richSyncLyrics.isNotBlank() && durationMatch) {
                    count++
                    callback(cleanLyrics(track.richSyncLyrics))
                } else if (track.syncedLyrics != null && track.syncedLyrics.isNotBlank() && durationMatch) {
                    count++
                    callback(cleanLyrics(track.syncedLyrics))
                }
                if (track.plainLyrics != null && track.plainLyrics.isNotBlank() && durationMatch && plain == 0) {
                    count++
                    plain++
                    callback(cleanLyrics(track.plainLyrics))
                }
            }
        }
    }
}
