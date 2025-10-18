package com.metrolist.music.viewmodels

import androidx.lifecycle.ViewModel
import com.metrolist.music.communication.CommunicationManager
import com.metrolist.music.discovery.NsdServiceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    val nsdServiceManager: NsdServiceManager,
    val communicationManager: CommunicationManager
) : ViewModel()
