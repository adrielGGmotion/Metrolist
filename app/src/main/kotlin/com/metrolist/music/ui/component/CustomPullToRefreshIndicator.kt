package com.metrolist.music.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomPullToRefreshIndicator(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier
) {
    val scale = if (isRefreshing) 1f else min(1f, state.distanceFraction * 1.5f).coerceAtLeast(0f)

    Surface(
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale),
        shape = CircleShape,
        shadowElevation = 6.dp,
    ) {
        Box(
            modifier = Modifier.padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isRefreshing) {
                CircularProgressIndicator(strokeWidth = 2.dp)
            } else {
                CircularProgressIndicator(
                    progress = { state.distanceFraction.coerceAtMost(1f) },
                    strokeWidth = 2.dp
                )
            }
        }
    }
}
