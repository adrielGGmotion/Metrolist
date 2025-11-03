package com.metrolist.apple

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.URLBuilder
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
            val searchUrl = URLBuilder("https://lyrics.paxsenix.dpdns.org/search").apply {
                parameters.append("q", "$title $artist")
            }.build()

            val searchResults = client.get(searchUrl).body<List<SearchResult>>()
            val bestMatchId = searchResults.firstOrNull()?.id ?: return null

            client.get("https://lyrics.paxsenix.dpdns.org/lyrics/$bestMatchId").body<AppleMusicLyrics>()
        } catch (e: Exception) {
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
