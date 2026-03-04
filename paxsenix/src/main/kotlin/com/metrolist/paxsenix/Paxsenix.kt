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
        
        // Try multiple search queries for better matching
        val searchQueries = buildList {
            if (!album.isNullOrBlank()) {
                add("$cleanedTitle $cleanedArtist $album")
            }
            add("$cleanedTitle $cleanedArtist")
            add(cleanedTitle) // Just title as fallback
        }
        
        var allResults: List<Pair<SearchResult, Double>> = emptyList()
        
        for (query in searchQueries) {
            if (allResults.isEmpty()) {
                Log.d(TAG, "Trying search query: $query")
                val searchResults = search(query)
                
                // If we got results, score and filter them
                if (searchResults.isNotEmpty()) {
                    allResults = scoreAndFilterResults(searchResults, cleanedTitle, cleanedArtist, duration)
                }
            }
        }
        
        if (allResults.isEmpty()) {
            Log.w(TAG, "No tracks found for any query")
            throw IllegalStateException("No tracks found on Paxsenix")
        }
        
        // Try up to 3 results - prefer one with word-level lyrics
        for (item in allResults.take(3)) {
            val result = item.first
            Log.d(TAG, "Trying: ${result.displayName} (ID: ${result.id}, dur: ${result.duration})")
            
            val (lrc, hasWordTimings) = fetchLyricsForTrackWithType(result.id)
            
            if (lrc.isNotEmpty()) {
                Log.d(TAG, "Got lyrics, hasWordTimings=$hasWordTimings")
                
                if (hasWordTimings) {
                    return Result.success(lrc)
                }
            }
        }
        
        // Return first valid result
        val firstResult = allResults.first().first
        Log.d(TAG, "Returning: ${firstResult.displayName}")
        return fetchLyricsForTrack(firstResult.id)
    }
    
    private fun scoreAndFilterResults(
        results: List<SearchResult>,
        title: String,
        artist: String,
        duration: Int
    ): List<Pair<SearchResult, Double>> {
        return results.map { result ->
            var score = 0.0
            
            // Exact title match
            val titleLower = result.displayName.lowercase()
            val targetTitleLower = title.lowercase()
            
            when {
                titleLower == targetTitleLower -> score += 100
                titleLower.contains(targetTitleLower) || targetTitleLower.contains(titleLower) -> score += 70
                else -> {
                    // Fuzzy word matching
                    val titleWords = titleLower.split(Regex("\\s+")).filter { it.length > 2 }
                    val resultWords = titleLower.split(Regex("\\s+")).filter { it.length > 2 }
                    val matchingWords = titleWords.count { tw -> resultWords.any { rw -> rw.contains(tw) || tw.contains(rw) } }
                    score += (matchingWords.toDouble() / maxOf(titleWords.size, 1)) * 50
                }
            }
            
            // Artist match
            val artistLower = result.displayArtist.lowercase()
            val targetArtistLower = artist.lowercase()
            
            when {
                artistLower == targetArtistLower -> score += 50
                artistLower.contains(targetArtistLower) || targetArtistLower.contains(artistLower) -> score += 35
                else -> {
                    val artistWords = targetArtistLower.split(Regex("\\s+")).filter { it.length > 2 }
                    val matchingArtist = artistWords.count { aw -> artistLower.contains(aw) }
                    score += (matchingArtist.toDouble() / maxOf(artistWords.size, 1)) * 25
                }
            }
            
            // Duration match - very important!
            result.duration?.let { d ->
                val diff = abs(d - duration)
                when {
                    diff <= 1000 -> score += 40
                    diff <= 2000 -> score += 30
                    diff <= 3000 -> score += 20
                    diff <= 5000 -> score += 10
                    diff <= 10000 -> score += 5
                }
            }
            
            Log.v(TAG, "  Score for '${result.displayName}': $score")
            result to score
        }.sortedByDescending { it.second }.take(5)
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
        
        // Use elrcMultiPerson directly - it's already in v1:/v2:/bg: format
        val elrcMultiPerson = response.elrcMultiPerson
        if (!elrcMultiPerson.isNullOrBlank()) {
            Log.d(TAG, "Using elrcMultiPerson format (raw v1/v2/bg)")
            return@runCatching elrcMultiPerson
        }
        
        // Try TTML as fallback
        val ttml = response.ttmlContent
        if (!ttml.isNullOrBlank()) {
            Log.d(TAG, "Using TTML format as fallback")
            val converted = convertTTMLToAppFormat(ttml)
            if (converted.isNotBlank()) {
                return@runCatching converted
            }
        }
        
        // Fallback to regular content parsing
        val hasWordLevel = lyricsType == "Syllable"
        Log.d(TAG, "Using content fallback, hasWordLevel=$hasWordLevel")
        
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
    
    /**
     * Convert elrcMultiPerson format to app format
     * From: [00:00.000]v1: <00:00.000>I <00:00.154>promise...
     * To:   [00:00.00]{agent:v1}I promise...<I:0.000:0.154|promise:0.154:0.499|...>
     * 
     * From: [bg: <02:18.078>Yeah<02:19.341>]
     * To:   [02:18.07]{bg}Yeah <Yeah:138.078:139.341>
     */
    private fun convertElrcMultiPersonToAppFormat(elrc: String): String {
        val lines = elrc.lines().filter { it.isNotBlank() }
        
        return buildString {
            lines.forEach { line ->
                // Match patterns like:
                // [00:00.000]v1: <00:00.000>I <00:00.154>promise...
                // [bg: <02:18.078>Yeah<02:19.341>]
                
                val v1Match = Regex("""\[(\d{1,2}):(\d{2})\.(\d{3})\]v1:\s*(.+)""").find(line)
                val v2Match = Regex("""\[(\d{1,2}):(\d{2})\.(\d{3})\]v2:\s*(.+)""").find(line)
                val bgMatch = Regex("""\[bg:\s*<(\d{1,2}):(\d{2})\.(\d{3})>(.+?)<(\d{1,2}):(\d{2})\.(\d{3})>\]""").find(line)
                
                when {
                    v1Match != null -> {
                        val minutes = v1Match.groupValues[1]
                        val seconds = v1Match.groupValues[2]
                        val centis = v1Match.groupValues[3].take(2).padEnd(2, '0')
                        val content = v1Match.groupValues[4]
                        
                        val convertedContent = convertWordTimings(content)
                        appendLine("[$minutes:$seconds.$centis]{agent:v1}$convertedContent")
                    }
                    v2Match != null -> {
                        val minutes = v2Match.groupValues[1]
                        val seconds = v2Match.groupValues[2]
                        val centis = v2Match.groupValues[3].take(2).padEnd(2, '0')
                        val content = v2Match.groupValues[4]
                        
                        val convertedContent = convertWordTimings(content)
                        appendLine("[$minutes:$seconds.$centis]{agent:v2}$convertedContent")
                    }
                    bgMatch != null -> {
                        val bgMinutes = bgMatch.groupValues[1].toInt()
                        val bgSeconds = bgMatch.groupValues[2].toInt()
                        val bgCentis = bgMatch.groupValues[3].take(2).padEnd(2, '0')
                        val word = bgMatch.groupValues[4].trim()
                        val endMin = bgMatch.groupValues[5].toInt()
                        val endSec = bgMatch.groupValues[6].toInt()
                        val endCentis = bgMatch.groupValues[7].take(2).padEnd(2, '0')
                        
                        val startTime = bgMinutes * 60 + bgSeconds + bgCentis.toInt() / 100
                        val endTime = endMin * 60 + endSec + endCentis.toInt() / 100
                        
                        appendLine("[${bgMinutes.toString().padStart(2, '0')}:${bgSeconds.toString().padStart(2, '0')}.$bgCentis]{bg}$word <$word:$startTime:$endTime>")
                    }
                }
            }
        }
    }
    
    /**
     * Convert word timings from Paxsenix format to app format
     * From: <00:00.000>I <00:00.154>promise <00:00.499>that...
     * To:   I promise...<I:0.000:0.154|promise:0.154:0.499|that:0.499:...>
     */
    private fun convertWordTimings(content: String): String {
        val wordMatches = Regex("""<(\d{1,2}):(\d{2})\.(\d{3})>([^<]+)""").findAll(content).toList()
        
        if (wordMatches.isEmpty()) return content
        
        val words = wordMatches.map { match ->
            val startMin = match.groupValues[1].toInt()
            val startSec = match.groupValues[2].toInt()
            val startCs = match.groupValues[3]
            val text = match.groupValues[4]
            
            val startSeconds = startMin * 60 + startSec + startCs.toDouble() / 1000
            
            text to startSeconds
        }.toMutableList()
        
        // Calculate end times
        val wordsWithTimings = mutableListOf<String>()
        for (i in words.indices) {
            val (text, start) = words[i]
            val end = if (i + 1 < words.size) words[i + 1].second else start + 1.0
            wordsWithTimings.add("$text:$start:$end")
        }
        
        val plainText = words.joinToString(" ") { it.first }
        val timings = wordsWithTimings.joinToString("|")
        
        return "$plainText<$timings>"
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
                    lines.add("$lineTime]$speakerTag$plainText<$wordsData>")
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
        val scoredResults = scoreAndFilterResults(results, cleanedTitle, cleanedArtist, duration)
        
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
