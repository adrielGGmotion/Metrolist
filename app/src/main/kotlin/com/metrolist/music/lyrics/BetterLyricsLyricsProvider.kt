package com.metrolist.music.lyrics

import android.content.Context
import com.mostafaalagamy.metrolist.betterlyrics.BetterLyrics

object BetterLyricsLyricsProvider : LyricsProvider {
    override val name = "BetterLyrics"

    override fun isEnabled(context: Context) = true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
    ): Result<String> = BetterLyrics.getLyrics(title, artist, duration)
}
