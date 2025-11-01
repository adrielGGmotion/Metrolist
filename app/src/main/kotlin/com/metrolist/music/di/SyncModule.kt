package com.metrolist.music.di

import com.metrolist.music.playback.sync.PlaybackServer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SyncModule {

    @Provides
    @Singleton
    fun providePlaybackServer(): PlaybackServer {
        return PlaybackServer()
    }
}
