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
        Regex("""\s*\([^)]*\d{4}[^)]*\)""", RegexOption.IGNORE_CASE),
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
        
        // Try multiple search queries for better matching
        val searchQueries = buildList {
            add("$cleanedTitle $cleanedArtist")
            add(cleanedTitle) // Just title as fallback
            if (!album.isNullOrBlank()) {
                add("$cleanedTitle $cleanedArtist $album")
            }
        }
        
        var allResults: List<Pair<SearchResult, Double>> = emptyList()
        
        for (query in searchQueries) {
            if (allResults.isEmpty()) {
                Log.d(TAG, "Trying search query: $query")
                val searchResults = search(query)
                
                // If we got results, score and filter them
                if (searchResults.isNotEmpty()) {
                    allResults = scoreAndFilterResults(searchResults, title, artist, duration)
                }
            }
        }
        
        if (allResults.isEmpty()) {
            Log.w(TAG, "No tracks found for any query")
            throw IllegalStateException("No tracks found on Paxsenix")
        }
        
        // First, try to find exact title match with word timings
        // Only prioritize word timings if title also matches well
        for (item in allResults.take(3)) {
            val result = item.first
            val score = item.second
            
            Log.d(TAG, "Trying: ${result.displayName} (ID: ${result.id}, dur: ${result.duration}, score: $score)")
            
            val (lrc, hasWordTimings) = fetchLyricsForTrackWithType(result.id)
            
            if (lrc.isNotEmpty()) {
                Log.d(TAG, "Got lyrics, hasWordTimings=$hasWordTimings")
                
                if (!hasWordTimings) {
                    continue
                }
                
                return Result.success(lrc)
            }
        }
        
        // Return first valid result if no good title match found
        val firstResult = allResults.first().first
        Log.d(TAG, "Returning: ${firstResult.displayName}")
        val (lrc, hasWordTimings) = fetchLyricsForTrackWithType(firstResult.id)
        if (lrc.isNotEmpty() && hasWordTimings) {
            return Result.success(lrc)
        }
        Log.w(TAG, "No word-by-word lyrics found from Paxsenix, letting other providers handle it")
        return Result.failure(IllegalStateException("No word-by-word lyrics available from Paxsenix"))
    }
    
    private fun scoreAndFilterResults(
        results: List<SearchResult>,
        title: String,
        artist: String,
        duration: Int
    ): List<Pair<SearchResult, Double>> {
        val durationMs = duration * 1000
        val cleanupRegex = Regex("""\s*\(.*?\)|\s*\[.*?\]""")
        
        // Cleaned versions for fuzzy matching
        val cleanedTitle = title.replace(cleanupRegex, "").lowercase().trim()
        val cleanedArtist = cleanArtist(artist).lowercase()
        
        // Track if target has version markers
        val targetIsMixed = title.contains("mixed", ignoreCase = true)
        val targetIsRemix = title.contains("remix", ignoreCase = true)
        
        return results.map { result ->
            var score = 0.0
            
            val resultTitle = result.displayName
            val resultArtist = result.displayArtist
            
            // 1. Duration check (Strongest filter)
            result.duration?.let { d ->
                val diff = abs(d - durationMs)
                when {
                    diff <= 2000 -> score += 100 // Excellent match
                    diff <= 5000 -> score += 50  // Good match
                    diff <= 10000 -> score += 10 // Acceptable match
                    else -> score -= 50          // Likely wrong version (Mixed/Edit/etc)
                }
            }
            
            // 2. Title Match
            val resultTitleCleaned = resultTitle.replace(cleanupRegex, "").lowercase().trim()
            
            when {
                resultTitleCleaned == cleanedTitle -> score += 80
                resultTitleCleaned.contains(cleanedTitle) || cleanedTitle.contains(resultTitleCleaned) -> score += 40
            }
            
            // Penalize version mismatch
            val resultIsMixed = resultTitle.contains("mixed", ignoreCase = true)
            val resultIsRemix = resultTitle.contains("remix", ignoreCase = true)
            
            if (resultIsMixed && !targetIsMixed) score -= 60
            if (resultIsRemix && !targetIsRemix) score -= 40
            
            // 3. Artist Match
            val resultArtistLower = resultArtist.lowercase()
            val targetArtistPrimary = cleanedArtist
            
            when {
                resultArtistLower.contains(targetArtistPrimary) -> score += 50
                else -> {
                    // Try matching any word from the artist name
                    val artistWords = targetArtistPrimary.split(Regex("\\s+")).filter { it.length > 2 }
                    if (artistWords.any { resultArtistLower.contains(it) }) {
                        score += 25
                    }
                }
            }
            
            Log.v(TAG, "  Score for '${resultTitle}': $score (dur=${result.duration}, targetDur=$durationMs)")
            result to score
        }.sortedByDescending { it.second }.filter { it.second > 0 }.take(10)
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
        Log.d(TAG, "Lyrics response: type=$lyricsType")
        
        if (response.content.isEmpty()) {
            // Check if there are other fields we can use
            if (!response.elrcMultiPerson.isNullOrBlank()) {
                return@runCatching response.elrcMultiPerson
            }
            if (!response.elrc.isNullOrBlank()) {
                return@runCatching response.elrc
            }
            throw IllegalStateException("No lyrics found")
        }
        
        val hasWordLevel = lyricsType == "Syllable"
        Log.d(TAG, "Using content array as primary source, hasWordLevel=$hasWordLevel")

        if (!hasWordLevel) {
            // Non-synced: return as plain text with no timestamps
            val plain = response.content
                .map { line -> line.text.joinToString(" ") { it.text } }
                .filter { it.isNotBlank() }
                .joinToString("\n")
            Log.d(TAG, "Generated plain (non-synced) lyrics: ${response.content.size} lines")
            return@runCatching plain
        }

        val lrc = buildString {
            response.content.forEach { line ->
                val timeMs = line.timestamp
                val minutes = timeMs / 1000 / 60
                val seconds = (timeMs / 1000) % 60
                val centiseconds = (timeMs % 1000) / 10

                val agent = when {
                    line.background -> "{bg}"
                    line.oppositeTurn -> "{agent:v2}"
                    else -> "{agent:v1}"
                }

                val lineText = line.text.joinToString(" ") { it.text }

                if (lineText.isNotBlank()) {
                    appendLine(String.format("[%02d:%02d.%02d]%s%s", minutes, seconds, centiseconds, agent, lineText))

                    if (line.text.isNotEmpty()) {
                        val wordsData = line.text.joinToString("|") { word ->
                            "${word.text}:${word.timestamp.toDouble() / 1000}:${word.endtime.toDouble() / 1000}"
                        }
                        if (wordsData.isNotEmpty()) {
                            appendLine("<$wordsData>")
                        }
                    }
                }
            }
        }

        Log.d(TAG, "Generated ${response.content.size} lines from content array")
        return@runCatching lrc
    }

    private fun findBestMatch(
        results: List<SearchResult>,
        title: String,
        artist: String,
        duration: Int,
        album: String?
    ): SearchResult? {
        Log.d(TAG, "Finding best match for: title='$title', artist='$artist', duration=$duration, album=$album")
        val durationMs = duration * 1000
        
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
                val diff = abs(d - durationMs)
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

        val searchQueries = listOf(
            "$cleanedTitle $cleanedArtist",
            cleanedTitle
        )

        var plainFallback: String? = null

        for (query in searchQueries) {
            val results = search(query)
            if (results.isEmpty()) continue

            val scoredResults = scoreAndFilterResults(results, title, artist, duration)
            if (scoredResults.isEmpty()) continue

            for ((result, _) in scoredResults.take(3)) {
                Log.d(TAG, "Trying lyrics for: ${result.displayName}")
                val (lrc, hasWordTimings) = fetchLyricsForTrackWithType(result.id)
                if (lrc.isNotEmpty()) {
                    if (hasWordTimings) {
                        callback(lrc)
                        return
                    } else if (plainFallback == null) {
                        Log.d(TAG, "Storing plain lyrics as fallback from: ${result.displayName}")
                        plainFallback = lrc
                    }
                }
            }
            break
        }

        // No word-by-word found — offer plain lyrics as fallback option, like other providers do
        plainFallback?.let {
            Log.d(TAG, "Offering plain/non-synced lyrics as fallback")
            callback(it)
        }
    }
    
    /**
     * Convert TTML format to app format with v1/v2/bg support
     * TTML has native agent info via ttm:agent="v1" or ttm:agent="v2"
     */
    private fun convertTTMLToAppFormat(ttml: String): String {
        return try {
            val lines = mutableListOf<String>()
            
            // Parse <p> elements which contain the lyrics
            val pPattern = Regex("""<p[^>]*begin="(\d+(?:\.\d+)?)"[^>]*end="(\d+(?:\.\d+)?)"[^>]*>(.*?)</p>""", RegexOption.DOT_MATCHES_ALL)
            val spanPattern = Regex("""<span[^>]*begin="(\d+(?:\.\d+)?)"[^>]*end="(\d+(?:\.\d+)?)"[^>]*ttm:agent="([^"]+)"[^>]*>([^<]*)</span>""")
            val spanNoAgentPattern = Regex("""<span[^>]*begin="(\d+(?:\.\d+)?)"[^>]*end="(\d+(?:\.\d+)?)"[^>]*>([^<]*)</span>""")
            
            val matches = pPattern.findAll(ttml).toList()
            
            for (pMatch in matches) {
                val lineStart = pMatch.groupValues[1].toDouble()
                val lineEnd = pMatch.groupValues[2].toDouble()
                val content = pMatch.groupValues[3]
                
                // Get agent from p tag
                val pAgentMatch = Regex("""ttm:agent="([^"]+)"""").find(pMatch.value)
                val defaultAgent = pAgentMatch?.groupValues?.get(1) ?: "v1"
                
                // Find all spans with their agents
                val spansWithAgents = mutableListOf<Triple<String, Double, Double>>()
                
                // First get spans with explicit agents
                spanPattern.findAll(content).forEach { spanMatch ->
                    val start = spanMatch.groupValues[1].toDouble()
                    val end = spanMatch.groupValues[2].toDouble()
                    val agent = spanMatch.groupValues[3]
                    val text = spanMatch.groupValues[4]
                    if (text.isNotBlank()) {
                        spansWithAgents.add(Triple("$agent:$text", start, end))
                    }
                }
                
                // Get spans without agents and assign default
                spanNoAgentPattern.findAll(content).forEach { spanMatch ->
                    val start = spanMatch.groupValues[1].toDouble()
                    val end = spanMatch.groupValues[2].toDouble()
                    val text = spanMatch.groupValues[3]
                    if (text.isNotBlank() && !spansWithAgents.any { it.second == start }) {
                        spansWithAgents.add(Triple("$defaultAgent:$text", start, end))
                    }
                }
                
                if (spansWithAgents.isEmpty()) continue
                
                // Sort by start time
                spansWithAgents.sortBy { it.second }
                
                // Group by speaker
                val bySpeaker = spansWithAgents.groupBy { 
                    it.first.substringBefore(":")
                }
                
                val startMin = (lineStart / 60).toInt()
                val startSec = (lineStart % 60).toInt()
                val startCs = ((lineStart % 1) * 100).toInt()
                
                // Build output for each speaker on this line
                for ((speaker, spans) in bySpeaker) {
                    val speakerTag = when (speaker.lowercase()) {
                        "v1" -> "{agent:v1}"
                        "v2" -> "{agent:v2}"
                        "v1000" -> "{bg}"
                        else -> "{agent:$speaker}"
                    }
                    
                    val wordsData = spans.joinToString("|") { (textWithAgent, start, end) ->
                        val text = textWithAgent.substringAfter(":")
                        "$text:$start:$end"
                    }
                    
                    val plainText = spans.joinToString(" ") { it.first.substringAfter(":") }
                    
                    val lineTime = String.format("%02d:%02d.%02d", startMin, startSec, startCs)
                    lines.add("[$lineTime]$speakerTag$plainText")
                    if (wordsData.isNotEmpty()) {
                        lines.add("<$wordsData>")
                    }
                }
            }
            
            lines.joinToString("\n")
        } catch (e: Exception) {
            Log.e(TAG, "TTML conversion failed: ${e.message}")
            ""
        }
    }
    
    data class ManualSearchResult(
        val id: String,
        val title: String,
        val artist: String,
        val album: String?,
        val duration: Int?,
        val hasWordSync: Boolean,
        val lyrics: String?
    )
    
    /**
     * Manual search that returns both standard and word-by-word lyrics for user to choose
     */
    suspend fun manualSearch(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
    ): List<ManualSearchResult> {
        val cleanedTitle = cleanTitle(title)
        val cleanedArtist = cleanArtist(artist)
        
        val searchQuery = if (!album.isNullOrBlank()) {
            "$cleanedTitle $cleanedArtist $album"
        } else {
            "$cleanedTitle $cleanedArtist"
        }
        
        val results = search(searchQuery)
        
        if (results.isEmpty()) return emptyList()
        
        // Score and take top 5
        val scoredResults = scoreAndFilterResults(results, title, artist, duration)
        
        return scoredResults.take(5).map { (result, _) ->
            var hasWordSync = false
            var lyrics: String? = null
            
            try {
                val response = httpClient.get("/apple-music/lyrics") {
                    parameter("id", result.id)
                }.body<LyricsResponse>()
                
                hasWordSync = response.elrcMultiPerson != null
                lyrics = response.elrcMultiPerson ?: response.elrc
            } catch (e: Exception) {
                // Keep default values
            }
            
            ManualSearchResult(
                id = result.id,
                title = result.displayName,
                artist = result.displayArtist,
                album = result.albumName,
                duration = result.duration,
                hasWordSync = hasWordSync,
                lyrics = lyrics
            )
        }
    }
}
