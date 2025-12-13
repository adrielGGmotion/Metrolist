package com.metrolist.music.wrapped

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.playback.MusicService
import com.metrolist.music.playback.PlayerConnection
import com.metrolist.music.ui.theme.MetrolistTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WrappedActivity : ComponentActivity() {
    @Inject
    lateinit var database: MusicDatabase
    private var playerConnection by mutableStateOf<PlayerConnection?>(null)
    private val serviceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                service: IBinder?,
            ) {
                if (service is MusicService.MusicBinder) {
                    playerConnection =
                        PlayerConnection(this@WrappedActivity, service, database, lifecycleScope)
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                playerConnection?.dispose()
                playerConnection = null
            }
        }

    override fun onStart() {
        super.onStart()
        startService(Intent(this, MusicService::class.java))
        bindService(
            Intent(this, MusicService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onStop() {
        unbindService(serviceConnection)
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val userName = intent.getStringExtra("USER_NAME") ?: "Guest"
        setContent {
            MetrolistTheme {
                CompositionLocalProvider(
                    LocalDatabase provides database,
                    LocalPlayerConnection provides playerConnection
                ) {
                    WrappedPager(userName = userName)
                }
            }
        }
    }
}
