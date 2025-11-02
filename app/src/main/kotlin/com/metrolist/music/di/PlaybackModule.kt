package com.metrolist.music.di

import android.app.Application
import com.metrolist.music.playback.sync.PlaybackServer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlaybackModule {
    @Provides
    @Singleton
    fun providePlaybackServer(application: Application): PlaybackServer {
        return PlaybackServer(application)
    }
}
