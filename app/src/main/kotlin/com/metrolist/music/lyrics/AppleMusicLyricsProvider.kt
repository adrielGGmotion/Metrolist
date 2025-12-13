package com.metrolist.music.lyrics

import android.content.Context
import com.mostafaalagamy.metrolist.applelyrics.AppleMusic
import com.mostafaalagamy.metrolist.applelyrics.LrcFormatter

object AppleMusicLyricsProvider : LyricsProvider {
    override val name = "Apple Music"

    override fun isEnabled(context: Context) = true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
    ): Result<String> =
        runCatching {
            val searchResults = AppleMusic.searchSong(title, artist)
                ?: throw IllegalStateException("No search results found for $title by $artist")

            val track = searchResults.firstOrNull()
                ?: throw IllegalStateException("No track found in search results for $title by $artist")

            val rawLyrics = AppleMusic.getLyrics(track.id)
                ?: throw IllegalStateException("Lyrics unavailable for track ID ${track.id}")
            
            LrcFormatter.formatLyrics(rawLyrics)
                ?: throw IllegalStateException("Failed to format lyrics for track ID ${track.id}")
        }
}
