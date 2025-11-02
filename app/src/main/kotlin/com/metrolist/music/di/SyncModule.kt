package com.metrolist.music.di

import com.metrolist.music.viewmodels.MainViewModel
import com.metrolist.sync.api.SyncState
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {
    @Binds
    @Singleton
    abstract fun bindSyncState(
        syncStateImpl: com.metrolist.music.sync.SyncStateImpl
    ): SyncState
}
