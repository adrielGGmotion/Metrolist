package com.metrolist.music.wrapped

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.metrolist.music.ui.theme.MetrolistTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WrappedActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val userName = intent.getStringExtra("USER_NAME") ?: "Guest"
        setContent {
            MetrolistTheme {
                WrappedPager(userName = userName)
            }
        }
    }
}
