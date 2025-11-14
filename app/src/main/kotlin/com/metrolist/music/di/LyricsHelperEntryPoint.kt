package com.metrolist.music.di

import com.metrolist.music.lyrics.LyricsHelper
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

import com.metrolist.music.apple.AppleMusicApi

@EntryPoint
@InstallIn(SingletonComponent::class)
interface LyricsHelperEntryPoint {
    fun lyricsHelper(): LyricsHelper
    fun appleMusicApi(): AppleMusicApi
}