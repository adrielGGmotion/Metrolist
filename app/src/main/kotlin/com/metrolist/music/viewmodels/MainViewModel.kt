package com.metrolist.music.viewmodels

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {
    private val _showPlayer = MutableStateFlow(true)
    val showPlayer = _showPlayer.asStateFlow()

    fun setShowPlayer(show: Boolean) {
        _showPlayer.value = show
    }
}
