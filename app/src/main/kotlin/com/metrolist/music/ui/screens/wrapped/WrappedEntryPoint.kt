package com.metrolist.music.ui.screens.wrapped

import android.content.Context
import com.metrolist.music.db.DatabaseDao
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WrappedEntryPoint {
    fun databaseDao(): DatabaseDao
}

internal fun getDatabaseDao(context: Context): DatabaseDao {
    val hiltEntryPoint = EntryPointAccessors.fromApplication(
        context.applicationContext,
        WrappedEntryPoint::class.java
    )
    return hiltEntryPoint.databaseDao()
}
