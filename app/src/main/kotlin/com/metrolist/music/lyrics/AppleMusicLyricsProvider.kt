package com.metrolist.music.lyrics

import android.content.Context
import com.metrolist.apple.AppleMusic
import com.metrolist.music.constants.EnableAppleMusicKey
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import timber.log.Timber

object AppleMusicLyricsProvider : LyricsProvider {
    override val name = "Apple Music"

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableAppleMusicKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
    ): Result<String> {
        return try {
            val lyrics = AppleMusic.getLyrics(title, artist)
            lyrics?.let { Result.success(it) } ?: Result.failure(Exception("Lyrics not found"))
        } catch (e: Exception) {
            Timber.e(e, "AppleMusicLyricsProvider: Error getting lyrics")
            Result.failure(e)
        }
    }
}
