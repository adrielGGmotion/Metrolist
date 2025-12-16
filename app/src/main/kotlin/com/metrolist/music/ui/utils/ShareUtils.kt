package com.metrolist.music.ui.utils

import android.content.Context
import android.content.Intent

fun Context.shareText(text: String, title: String) {
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, title)
    startActivity(shareIntent)
}
