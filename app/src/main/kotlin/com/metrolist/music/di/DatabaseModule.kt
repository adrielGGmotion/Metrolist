package com.metrolist.music.di

import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.dao.BlacklistedArtistDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    fun provideBlacklistedArtistDao(database: MusicDatabase): BlacklistedArtistDao {
        return database.blacklistedArtistDao()
    }
}
