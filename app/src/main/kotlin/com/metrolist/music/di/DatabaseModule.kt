package com.metrolist.music.di

import android.content.Context
import com.metrolist.music.db.AppDatabase
import com.metrolist.music.db.dao.BlacklistedArtistDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideBlacklistedArtistDao(appDatabase: AppDatabase): BlacklistedArtistDao {
        return appDatabase.blacklistedArtistDao()
    }
}
