package com.metrolist.music.ui.player

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.PlayerHorizontalPadding
import com.metrolist.music.constants.SliderStyle
import com.metrolist.music.extensions.togglePlayPause
import com.metrolist.music.extensions.toggleRepeatMode
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.models.Song
import com.metrolist.music.ui.component.BottomSheetState
import com.metrolist.music.ui.component.LocalBottomSheetPageState
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.PlayerSliderTrack
import com.metrolist.music.ui.component.resizableicon.ResizableIconButton
import com.metrolist.music.ui.menu.PlayerMenu
import com.metrolist.music.ui.utils.ShowMediaInfo
import com.metrolist.music.utils.makeTimeString
import me.saket.squiggles.SquigglySlider

@Composable
fun ColumnScope.PlayerMD3(
    mediaMetadata: MediaMetadata,
    currentSong: Song?,
    toggleLike: () -> Unit,
    sliderStyle: SliderStyle,
    sliderPosition: Long?,
    position: Long,
    duration: Long,
    onSliderChange: (Long) -> Unit,
    onSliderChangeFinished: () -> Unit,
    textButtonColor: Color,
    iconButtonColor: Color,
    TextBackgroundColor: Color,
    canSkipPrevious: Boolean,
    onSeekToPrevious: () -> Unit,
    playbackState: Int,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    canSkipNext: Boolean,
    onSeekToNext: () -> Unit,
    repeatMode: Int,
    onToggleRepeatMode: () -> Unit,
    onShowQueue: () -> Unit,
    onShowLyrics: () -> Unit,
    onShowSleepTimer: () -> Unit,
    navController: NavController,
    bottomSheetState: BottomSheetState
) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(horizontal = PlayerHorizontalPadding),
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            AnimatedContent(
                targetState = mediaMetadata.title,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "playerTitle",
            ) { title ->
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = TextBackgroundColor,
                    modifier =
                    Modifier
                        .basicMarquee()
                        .combinedClickable(
                            enabled = true,
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = {
                                if (mediaMetadata.album != null) {
                                    navController.navigate("album/${mediaMetadata.album.id}")
                                    bottomSheetState.collapseSoft()
                                }
                            },
                            onLongClick = {
                                val clip = ClipData.newPlainText("Copied Title", title)
                                clipboardManager.setPrimaryClip(clip)
                                Toast
                                    .makeText(context, "Copied Title", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        ),
                )
            }

            Spacer(Modifier.height(6.dp))

            val annotatedString = buildAnnotatedString {
                mediaMetadata.artists.forEachIndexed { index, artist ->
                    val tag = "artist_${artist.id.orEmpty()}"
                    pushStringAnnotation(tag = tag, annotation = artist.id.orEmpty())
                    withStyle(SpanStyle(color = TextBackgroundColor, fontSize = 16.sp)) {
                        append(artist.name)
                    }
                    pop()
                    if (index != mediaMetadata.artists.lastIndex) append(", ")
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee()
                    .padding(end = 12.dp)
            ) {
                var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                var clickOffset by remember { mutableStateOf<Offset?>(null) }
                Text(
                    text = annotatedString,
                    style = MaterialTheme.typography.titleMedium.copy(color = TextBackgroundColor),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { layoutResult = it },
                    modifier = Modifier
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val tapPosition = event.changes.firstOrNull()?.position
                                    if (tapPosition != null) {
                                        clickOffset = tapPosition
                                    }
                                }
                            }
                        }
                        .combinedClickable(
                            enabled = true,
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = {
                                //moving to temp val to avoid changes mid operation.
                                val tapPosition = clickOffset
                                val layout = layoutResult
                                if (tapPosition != null && layout != null) {
                                    val offset = layout.getOffsetForPosition(tapPosition)
                                    annotatedString
                                        .getStringAnnotations(offset, offset)
                                        .firstOrNull()
                                        ?.let { ann ->
                                            val artistId = ann.item
                                            if (artistId.isNotBlank()) {
                                                navController.navigate("artist/$artistId")
                                                bottomSheetState.collapseSoft()
                                            }
                                        }
                                }
                            },
                            onLongClick = {
                                val clip = ClipData.newPlainText("Copied Artist", annotatedString)
                                clipboardManager.setPrimaryClip(clip)
                                Toast
                                    .makeText(context, "Copied Artist", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        )
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                val intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "https://music.youtube.com/watch?v=${mediaMetadata.id}"
                    )
                }
                context.startActivity(Intent.createChooser(intent, null))
            }) {
                Icon(
                    painter = painterResource(R.drawable.share),
                    contentDescription = stringResource(R.string.share),
                    tint = TextBackgroundColor,
                )
            }

            IconButton(onClick = toggleLike) {
                Icon(
                    painter = painterResource(
                        if (currentSong?.song?.liked == true)
                            R.drawable.favorite
                        else R.drawable.favorite_border
                    ),
                    contentDescription = stringResource(R.string.like),
                    tint = if (currentSong?.song?.liked == true) MaterialTheme.colorScheme.error else TextBackgroundColor,
                )
            }
        }
    }

    Spacer(Modifier.height(12.dp))

    when (sliderStyle) {
        SliderStyle.DEFAULT -> {
            Slider(
                value = (sliderPosition ?: position).toFloat(),
                valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                onValueChange = {
                    onSliderChange(it.toLong())
                },
                onValueChangeFinished = {
                    onSliderChangeFinished()
                },
                colors = SliderDefaults.colors(
                    thumbColor = textButtonColor,
                    activeTrackColor = textButtonColor,
                    inactiveTrackColor = textButtonColor.copy(alpha = 0.4f),
                ),
                modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
            )
        }

        SliderStyle.SQUIGGLY -> {
            SquigglySlider(
                value = (sliderPosition ?: position).toFloat(),
                valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                onValueChange = {
                    onSliderChange(it.toLong())
                },
                onValueChangeFinished = {
                    onSliderChangeFinished()
                },
                colors = SliderDefaults.colors(
                    thumbColor = textButtonColor,
                    activeTrackColor = textButtonColor,
                    inactiveTrackColor = textButtonColor.copy(alpha = 0.4f),
                ),
                modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
                squigglesSpec =
                SquigglySlider.SquigglesSpec(
                    amplitude = if (isPlaying) (2.dp).coerceAtLeast(2.dp) else 0.dp,
                    strokeWidth = 3.dp,
                ),
            )
        }

        SliderStyle.SLIM -> {
            Slider(
                value = (sliderPosition ?: position).toFloat(),
                valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                onValueChange = {
                    onSliderChange(it.toLong())
                },
                onValueChangeFinished = {
                    onSliderChangeFinished()
                },
                thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                track = { sliderState ->
                    PlayerSliderTrack(
                        sliderState = sliderState,
                        colors = SliderDefaults.colors(
                            thumbColor = textButtonColor,
                            activeTrackColor = textButtonColor,
                            inactiveTrackColor = textButtonColor.copy(alpha = 0.4f),
                        )
                    )
                },
                modifier = Modifier.padding(horizontal = PlayerHorizontalPadding)
            )
        }
    }

    Spacer(Modifier.height(4.dp))

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(horizontal = PlayerHorizontalPadding + 4.dp),
    ) {
        Text(
            text = makeTimeString(sliderPosition ?: position),
            style = MaterialTheme.typography.labelMedium,
            color = TextBackgroundColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = if (duration != C.TIME_UNSET) makeTimeString(duration) else "",
            style = MaterialTheme.typography.labelMedium,
            color = TextBackgroundColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }

    Spacer(Modifier.height(12.dp))

    Row(
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {

        IconButton(
            onClick = onSeekToPrevious,
            enabled = canSkipPrevious,
        ) {
            Icon(
                painter = painterResource(R.drawable.skip_previous),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = TextBackgroundColor
            )
        }

        FilledIconButton(
            onClick = onTogglePlayPause,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = textButtonColor,
                contentColor = iconButtonColor
            ),
            modifier = Modifier
                .size(72.dp)
        ) {
            Icon(
                painter = painterResource(
                    when {
                        playbackState == Player.STATE_ENDED -> R.drawable.replay
                        isPlaying -> R.drawable.pause
                        else -> R.drawable.play
                    }
                ),
                contentDescription = null,
                modifier = Modifier.size(42.dp)
            )
        }

        IconButton(
            onClick = onSeekToNext,
            enabled = canSkipNext,
        ) {
            Icon(
                painter = painterResource(R.drawable.skip_next),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = TextBackgroundColor
            )
        }
    }

    Spacer(Modifier.height(12.dp))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(horizontal = PlayerHorizontalPadding),
    ) {
        IconButton(onClick = onShowSleepTimer) {
            Icon(
                painter = painterResource(R.drawable.bedtime),
                contentDescription = stringResource(R.string.sleep_timer),
                tint = TextBackgroundColor
            )
        }

        IconButton(onClick = onShowQueue) {
            Icon(
                painter = painterResource(R.drawable.queue_music),
                contentDescription = stringResource(R.string.queue),
                tint = TextBackgroundColor
            )
        }

        IconButton(onClick = onShowLyrics) {
            Icon(
                painter = painterResource(R.drawable.lyrics),
                contentDescription = stringResource(R.string.lyrics),
                tint = TextBackgroundColor
            )
        }

        IconButton(onClick = onToggleRepeatMode) {
            Icon(
                painter = painterResource(
                    when (repeatMode) {
                        Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_ALL -> R.drawable.repeat
                        Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                        else -> throw IllegalStateException()
                    }
                ),
                contentDescription = stringResource(R.string.repeat),
                tint = TextBackgroundColor,
                modifier = Modifier.size(24.dp)
            )
        }

        IconButton(onClick = {
            menuState.show {
                PlayerMenu(
                    mediaMetadata = mediaMetadata,
                    navController = navController,
                    playerBottomSheetState = bottomSheetState,
                    onShowDetailsDialog = {
                        mediaMetadata.id.let {
                            bottomSheetPageState.show {
                                ShowMediaInfo(it)
                            }
                        }
                    },
                    onDismiss = menuState::dismiss,
                )
            }
        }) {
            Icon(
                painter = painterResource(R.drawable.more_horiz),
                contentDescription = stringResource(R.string.more_options),
                tint = TextBackgroundColor
            )
        }
    }
}
