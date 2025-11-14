package com.metrolist.music.lyrics

import android.content.Context
import com.metrolist.music.apple.AppleMusicApi
import com.metrolist.music.constants.EnableAppleMusicKey // The key from Step 5
import com.metrolist.music.di.LyricsHelperEntryPoint
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppleMusicLyricsProvider : LyricsProvider {
    override val name = "Apple Music" // This is the name shown in the app

    override fun isEnabled(context: Context): Boolean =
        context.dataStore[EnableAppleMusicKey] ?: false // Default to false

    private fun getApi(context: Context): AppleMusicApi {
        return EntryPointAccessors.fromApplication(
            context.applicationContext,
            LyricsHelperEntryPoint::class.java
        ).appleMusicApi()
    }

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // We don't have a context here, so we must use the one from the helper
                val api = getApi(LyricsHelper.appContext)
                api.getLyrics(title, artist)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
