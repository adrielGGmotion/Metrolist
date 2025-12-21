package com.metrolist.music.lyrics

import android.content.Context
import com.metrolist.applelyrics.AppleLyrics
import com.metrolist.music.BuildConfig
import com.metrolist.music.constants.EnableAppleMusicKey
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get

object AppleMusicLyricsProvider : LyricsProvider {
    override val name = "Apple Music"

    override fun isEnabled(context: Context): Boolean =
        context.dataStore[EnableAppleMusicKey] ?: false

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        albumName: String?,
        duration: Int
    ): Result<String> =
        AppleLyrics.getLyrics(title, artist, albumName ?: "", BuildConfig.VERSION_NAME)
}
