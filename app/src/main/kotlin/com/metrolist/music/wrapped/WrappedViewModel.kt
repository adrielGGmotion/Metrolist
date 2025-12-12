package com.metrolist.music.wrapped

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.music.db.MusicDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WrappedViewModel @Inject constructor(
    private val database: MusicDatabase
) : ViewModel() {
    private val _wrappedData = MutableStateFlow<WrappedData?>(null)
    val wrappedData: StateFlow<WrappedData?> = _wrappedData

    init {
        viewModelScope.launch {
            _wrappedData.value = calculateWrappedData(database)
        }
    }
}
