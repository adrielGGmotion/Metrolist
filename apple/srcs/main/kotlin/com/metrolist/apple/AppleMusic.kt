package com.metrolist.apple

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.URLBuilder
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber

object AppleMusic {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    suspend fun getLyrics(title: String, artist: String): AppleMusicLyrics? {
        return try {
            val searchQuery = "$title $artist"
            Timber.d("AppleMusic: Searching for '$searchQuery'")
            val searchUrl = URLBuilder("https://lyrics.paxsenix.dpdns.org/search").apply {
                parameters.append("q", searchQuery)
            }.build()

            val searchResults = client.get(searchUrl).body<List<SearchResult>>()
            Timber.d("AppleMusic: Search results: $searchResults")
            val bestMatchId = searchResults.firstOrNull()?.id ?: run {
                Timber.d("AppleMusic: No search results found")
                return null
            }

            Timber.d("AppleMusic: Fetching lyrics for id '$bestMatchId'")
            val lyrics = client.get("https://lyrics.paxsenix.dpdns.org/lyrics/$bestMatchId").body<AppleMusicLyrics>()
            Timber.d("AppleMusic: Lyrics received: $lyrics")
            lyrics
        } catch (e: Exception) {
            Timber.e(e, "AppleMusic: Error getting lyrics")
            null
        }
    }
}

@Serializable
data class SearchResult(
    val id: String
)

@Serializable
data class AppleMusicLyrics(
    val sync: String?,
    val plain: String?
)
