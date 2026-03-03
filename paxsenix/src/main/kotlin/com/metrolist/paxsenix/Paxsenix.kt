package com.metrolist.paxsenix

import android.content.Context
import android.util.Log
import com.metrolist.paxsenix.models.LyricsResponse
import com.metrolist.paxsenix.models.SearchResponse
import com.metrolist.paxsenix.models.SearchResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.math.abs

object Paxsenix {
    private const val TAG = "Paxsenix"
    private var client: HttpClient? = null
    private var appVersion: String = "Unknown"

    fun init(context: Context) {
        if (client != null) return // Already initialized
        
        appVersion = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
                ?: "Unknown"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get app version", e)
            "Unknown"
        }
        
        Log.d(TAG, "Initializing Paxsenix with version: $appVersion")
        
        client = HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 15000
                connectTimeoutMillis = 10000
            }
            install(ContentNegotiation) {
                json(
                    Json {
                        isLenient = true
                        ignoreUnknownKeys = true
                    },
                )
            }

            defaultRequest {
                url("https://lyrics.paxsenix.org")
                header("User-Agent", "Metrolist/$appVersion")
            }

            expectSuccess = true
        }
        
        Log.d(TAG, "Paxsenix HTTP client initialized")
    }

    private val httpClient: HttpClient
        get() = client ?: throw IllegalStateException("Paxsenix.init() must be called before using Paxsenix")

    // Patterns to clean from title
    private val titleCleanupPatterns = listOf(
        Regex("""\s*\(.*?(official|video|audio|lyrics|lyric|visualizer|hd|hq|4k|remaster|remix|live|acoustic|version|edit|extended|radio|clean|explicit).*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*\[.*?(official|video|audio|lyrics|lyric|visualizer|hd|hq|4k|remaster|remix|live|acoustic|version|edit|extended|radio|clean|explicit).*?\]""", RegexOption.IGNORE_CASE),
        Regex("""\s*【.*?】"""),
        Regex("""\s*\|.*$"""),
        Regex("""\s*-\s*(official|video|audio|lyrics|lyric|visualizer).*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*\(feat\..*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*\(ft\..*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*feat\..*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*ft\..*$""", RegexOption.IGNORE_CASE),
    )

    // Patterns to extract primary artist
    private val artistSeparators = listOf(" & ", " and ", ", ", " x ", " X ", " feat. ", " feat ", " ft. ", " ft ", " featuring ", " with ")

    private fun cleanTitle(title: String): String {
        var cleaned = title.trim()
        for (pattern in titleCleanupPatterns) {
            cleaned = cleaned.replace(pattern, "")
        }
        return cleaned.trim()
    }

    private fun cleanArtist(artist: String): String {
        var cleaned = artist.trim()
        // Get primary artist (first one before any separator)
        for (separator in artistSeparators) {
            if (cleaned.contains(separator, ignoreCase = true)) {
                cleaned = cleaned.split(separator, ignoreCase = true, limit = 2)[0]
                break
            }
        }
        return cleaned.trim()
    }

    private suspend fun search(query: String): List<SearchResult> = runCatching {
        Log.d(TAG, "Searching for: $query")
        val response = httpClient.get("/apple-music/search") {
            parameter("q", query)
        }.body<SearchResponse>()
        
        Log.d(TAG, "Search results count: ${response.size}")
        response.forEach { result ->
            Log.v(TAG, "  - ${result.displayName} by ${result.displayArtist} (ID: ${result.id}, Duration: ${result.duration})")
        }
        
        response
    }.getOrElse { e ->
        Log.e(TAG, "Search error: ${e.message}", e)
        emptyList()
    }

    suspend fun getLyrics(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
    ): Result<String> = runCatching {
        val cleanedTitle = cleanTitle(title)
        val cleanedArtist = cleanArtist(artist)
        
        Log.d(TAG, "getLyrics called: title='$title', artist='$artist', duration=$duration, album=$album")
        Log.d(TAG, "Cleaned: title='$cleanedTitle', artist='$cleanedArtist'")
        
        // Search with title, artist, and album for better matching
        val searchQuery = if (!album.isNullOrBlank()) {
            "$cleanedTitle $cleanedArtist $album"
        } else {
            "$cleanedTitle $cleanedArtist"
        }
        
        Log.d(TAG, "Final search query: $searchQuery")
        
        val results = search(searchQuery)
        
        if (results.isEmpty()) {
            Log.w(TAG, "No tracks found for query: $searchQuery")
            throw IllegalStateException("No tracks found on Paxsenix for query: $searchQuery")
        }
        
        // Score and sort results, then try up to 3 to find one with word-level lyrics
        val scoredResults = results.take(5).mapIndexed { index, result ->
            var score = 0.0
            
            val titleSim = if (result.displayName.equals(cleanedTitle, ignoreCase = true)) 1.0
            else if (result.displayName.contains(cleanedTitle, ignoreCase = true) || cleanedTitle.contains(result.displayName, ignoreCase = true)) 0.7
            else 0.0
            score += titleSim * 50
            
            val artistSim = if (result.displayArtist.equals(cleanedArtist, ignoreCase = true)) 1.0
            else if (result.displayArtist.contains(cleanedArtist, ignoreCase = true) || cleanedArtist.contains(result.displayArtist, ignoreCase = true)) 0.6
            else 0.0
            score += artistSim * 30
            
            result.duration?.let { d ->
                val diff = abs(d - duration)
                when {
                    diff <= 1000 -> score += 15
                    diff <= 3000 -> score += 10
                    diff <= 5000 -> score += 5
                }
            }
            
            score += (5 - index) * 2
            
            result to score
        }.sortedByDescending { it.second }
        
        // Try up to 3 results - prefer one with word-level lyrics
        for ((result, _) in scoredResults.take(3)) {
            Log.d(TAG, "Trying lyrics for: ${result.displayName} (ID: ${result.id})")
            
            val (lrc, hasWordTimings) = fetchLyricsForTrackWithType(result.id)
            
            if (lrc.isNotEmpty()) {
                Log.d(TAG, "Got lyrics for ${result.displayName}, hasWordTimings=$hasWordTimings")
                
                if (hasWordTimings) {
                    Log.d(TAG, "Found word-level lyrics for: ${result.displayName}")
                    return Result.success(lrc)
                }
            }
        }
        
        // Return whatever we got (even if line-only)
        val firstResult = scoredResults.first()
        Log.d(TAG, "Returning lyrics for: ${firstResult.first.displayName} (may be line-only)")
        return fetchLyricsForTrack(firstResult.first.id)
    }

    private suspend fun fetchLyricsForTrackWithType(id: String): Pair<String, Boolean> {
        val result = fetchLyricsForTrack(id)
        if (result.isSuccess) {
            val lrc = result.getOrNull()!!
            val hasWordTimings = lrc.contains("<") && lrc.contains(">")
            return lrc to hasWordTimings
        }
        return "" to false
    }

    private suspend fun fetchLyricsForTrack(id: String): Result<String> = runCatching {
        Log.d(TAG, "Fetching lyrics for track ID: $id")
        
        val response = httpClient.get("/apple-music/lyrics") {
            parameter("id", id)
        }.body<LyricsResponse>()
        
        val lyricsType = response.type
        val hasWordLevel = lyricsType == "Syllable"
        Log.d(TAG, "Lyrics response: type=$lyricsType, hasWordLevel=$hasWordLevel, content count=${response.content.size}")
        
        response.content.take(3).forEachIndexed { index, line ->
            Log.v(TAG, "  Line $index: timestamp=${line.timestamp}, textCount=${line.text.size}")
            if (hasWordLevel && line.text.size > 1 && index < 2) {
                line.text.take(3).forEach { word ->
                    Log.v(TAG, "    Word: '${word.text}' start=${word.timestamp} end=${word.endtime}")
                }
            }
        }
        
        if (response.content.isEmpty()) {
            throw IllegalStateException("No lyrics found")
        }
        
        val lrc = buildString {
            response.content.forEach { line ->
                val timeMs = line.timestamp
                val minutes = timeMs / 1000 / 60
                val seconds = (timeMs / 1000) % 60
                val centiseconds = (timeMs % 1000) / 10
                
                val lineText = line.text.joinToString(" ") { it.text }
                
                if (lineText.isNotBlank()) {
                    appendLine(String.format("[%02d:%02d.%02d]%s", minutes, seconds, centiseconds, lineText))
                    
                    // Add word timings ONLY for Syllable type (actual word-level lyrics)
                    if (hasWordLevel && line.text.size > 1) {
                        val wordsData = line.text.filter { it.timestamp > 0 }.joinToString("|") { word ->
                            "${word.text}:${word.timestamp.toDouble() / 1000}:${word.endtime.toDouble() / 1000}"
                        }
                        if (wordsData.isNotEmpty()) {
                            appendLine("<$wordsData>")
                        }
                    }
                }
            }
        }
        
        Log.d(TAG, "Parsed ${response.content.size} lines of lyrics")
        
        if (lrc.isBlank()) {
            throw IllegalStateException("No lyrics found")
        }
        
        lrc
    }

    private fun findBestMatch(
        results: List<SearchResult>,
        title: String,
        artist: String,
        duration: Int,
        album: String?
    ): SearchResult? {
        Log.d(TAG, "Finding best match for: title='$title', artist='$artist', duration=$duration, album=$album")
        
        // Calculate similarity between two strings (0.0 to 1.0)
        fun similarity(a: String, b: String): Double {
            val aLower = a.lowercase().trim()
            val bLower = b.lowercase().trim()
            if (aLower == bLower) return 1.0
            
            // Check if one contains the other
            if (aLower.contains(bLower) || bLower.contains(aLower)) {
                return 0.8
            }
            
            // Word-based similarity
            val wordsA = aLower.split(Regex("\\s+")).toSet()
            val wordsB = bLower.split(Regex("\\s+")).toSet()
            val intersection = wordsA.intersect(wordsB).size
            val union = wordsA.union(wordsB).size
            return if (union > 0) intersection.toDouble() / union else 0.0
        }
        
        // Score each result
        data class ScoredResult(val result: SearchResult, val score: Double)
        
        val scoredResults = results.map { result ->
            var score = 0.0
            
            // Title similarity (most important)
            val titleSim = similarity(result.displayName, title)
            score += titleSim * 50
            
            // Artist similarity (very important)
            val artistSim = similarity(result.displayArtist, artist)
            score += artistSim * 30
            
            // Duration match (if available)
            result.duration?.let { d ->
                val diff = abs(d - duration)
                when {
                    diff <= 1000 -> score += 15
                    diff <= 3000 -> score += 10
                    diff <= 5000 -> score += 5
                }
            }
            
            // Album match (if provided)
            if (!album.isNullOrBlank() && result.albumName != null) {
                val albumSim = similarity(result.albumName, album)
                score += albumSim * 10
            }
            
            Log.v(TAG, "  Scoring: ${result.displayName} by ${result.displayArtist} -> score=$score (titleSim=${"%.2f".format(titleSim)}, artistSim=${"%.2f".format(artistSim)})")
            
            ScoredResult(result, score)
        }
        
        val best = scoredResults.maxByOrNull { it.score }
        Log.d(TAG, "Best match: ${best?.result?.displayName} with score ${"%.2f".format(best?.score ?: 0.0)}")
        
        // Only return if score is reasonable (at least 40% match)
        return best?.takeIf { it.score >= 40 }?.result
    }

    suspend fun getAllLyrics(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
        callback: (String) -> Unit,
    ) {
        val cleanedTitle = cleanTitle(title)
        val cleanedArtist = cleanArtist(artist)
        
        val searchQuery = if (!album.isNullOrBlank()) {
            "$cleanedTitle $cleanedArtist $album"
        } else {
            "$cleanedTitle $cleanedArtist"
        }
        
        val results = search(searchQuery)
        
        if (results.isEmpty()) return
        
        // Score and sort results by match quality
        val scoredResults = results.take(5).mapIndexed { index, result ->
            var score = 0.0
            
            // Title similarity
            val titleSim = if (result.displayName.equals(cleanedTitle, ignoreCase = true)) 1.0
            else if (result.displayName.contains(cleanedTitle, ignoreCase = true) || cleanedTitle.contains(result.displayName, ignoreCase = true)) 0.7
            else 0.0
            score += titleSim * 50
            
            // Artist similarity
            val artistSim = if (result.displayArtist.equals(cleanedArtist, ignoreCase = true)) 1.0
            else if (result.displayArtist.contains(cleanedArtist, ignoreCase = true) || cleanedArtist.contains(result.displayArtist, ignoreCase = true)) 0.6
            else 0.0
            score += artistSim * 30
            
            // Prefer shorter durations (often the main version)
            result.duration?.let { d ->
                score += (100000 - minOf(d, 100000).toDouble()) / 100000 * 10
            }
            
            // Prioritize results that appear earlier in search (likely better match)
            score += (5 - index) * 2
            
            result to score
        }.sortedByDescending { it.second }
        
        // Fetch sequentially but try top candidates - faster because we prioritize best matches
        // and stop as soon as we find valid lyrics
        for ((result, _) in scoredResults.take(3)) {
            Log.d(TAG, "Trying lyrics for: ${result.displayName}")
            fetchLyricsForTrack(result.id).onSuccess { lyrics ->
                callback(lyrics)
                return
            }
        }
    }
}
