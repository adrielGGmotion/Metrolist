package com.metrolist.music.apple

import com.metrolist.music.extensions.substringBetween
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject

class AppleMusicApi @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json
) {
    fun getLyrics(title: String, artist: String): Result<String> {
        val query = "$title $artist"

        // 1. Build the search URL
        val searchUrl = "https://www.google.com".toHttpUrl()
            .newBuilder()
            .addPathSegment("search")
            .addQueryParameter("q", "site:music.apple.com $query lyrics")
            .build()

        // 2. Make the Google search request
        val searchRequest = Request.Builder()
            .url(searchUrl)
            // Use a realistic User-Agent
            .header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/104.0.0.0 Safari/537.36"
            )
            .build()

        val responseHtml: String
        try {
            responseHtml = okHttpClient.newCall(searchRequest).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")
                response.body?.string() ?: throw IOException("Empty response body")
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }

        // 3. Find the Apple Music URL from the Google search
        val appleMusicUrl = responseHtml.substringBetween(
            "https://music.apple.com/",
            "&amp;"
        )?.replace("?l=en-GB", "") ?: return Result.failure(Exception("No Apple Music URL found in search"))

        val fullAppleMusicUrl = "https://music.apple.com/$appleMusicUrl"

        // 4. Make the request to the Apple Music page
        val lyricsPageRequest = Request.Builder().url(fullAppleMusicUrl).build()
        val lyricsPageHtml: String
        try {
            lyricsPageHtml = okHttpClient.newCall(lyricsPageRequest).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")
                response.body?.string() ?: throw IOException("Empty response body")
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }

        // 5. Extract the JSON data from the HTML page
        val jsonData = lyricsPageHtml.substringBetween(
            "type=\"application/ld+json\">",
            "</script>"
        ) ?: return Result.failure(Exception("Could not find JSON data in Apple Music page"))

        // 6. Parse the JSON to get the LRC string
        return try {
            val parsed = json.decodeFromString<AppleMusicLyricsResponse>(jsonData)
            val lrcLyrics = parsed.data?.firstOrNull()?.attributes?.lrc
            if (lrcLyrics.isNullOrEmpty()) {
                Result.failure(Exception("Lyrics found but LRC string was empty"))
            } else {
                Result.success(lrcLyrics) // This is the synced LRC!
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
