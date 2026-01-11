/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.metrolist.music.lyrics

import android.content.Context
import com.metrolist.applemusic.AppleMusic
import com.metrolist.music.BuildConfig
import com.metrolist.music.constants.EnableAppleMusicKey
import com.metrolist.music.utils.dataStore
import kotlinx.coroutines.flow.first

object AppleMusicLyricsProvider : LyricsProvider {
    override val name = "Apple Music"

    override suspend fun isEnabled(context: Context): Boolean {
        val preferences = context.dataStore.data.first()
        return preferences[EnableAppleMusicKey] ?: true
    }

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> {
        val userAgent = "${BuildConfig.APPLICATION_ID}/${BuildConfig.VERSION_NAME}"
        return AppleMusic.fetchLyrics(title, artist, userAgent)
    }
}
