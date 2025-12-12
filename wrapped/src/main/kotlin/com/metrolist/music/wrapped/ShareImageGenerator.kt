package com.metrolist.music.wrapped

import android.content.Context
import android.graphics.Bitmap

class ShareImageGenerator(private val context: Context) {

    fun generateStatStory(stats: WrappedStats, stat: Stat): Bitmap {
        return createBitmapFromComposable(context) {
            StatStoryTemplate(stats, stat)
        }
    }

    fun generateListStory(stats: WrappedStats, listType: ListType): Bitmap {
        return createBitmapFromComposable(context) {
            ListStoryTemplate(stats, listType)
        }
    }

    fun generateReceipt(stats: WrappedStats): Bitmap {
        return createBitmapFromComposable(context) {
            ReceiptTemplate(stats)
        }
    }
}
