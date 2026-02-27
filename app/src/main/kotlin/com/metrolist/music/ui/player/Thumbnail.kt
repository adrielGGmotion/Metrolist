/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.metrolist.music.LocalListenTogetherManager
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.AlbumCoverTransitionKey
import com.metrolist.music.constants.CropAlbumArtKey
import com.metrolist.music.constants.HidePlayerThumbnailKey
import com.metrolist.music.constants.PlayerBackgroundStyle
import com.metrolist.music.constants.PlayerBackgroundStyleKey
import com.metrolist.music.constants.PlayerDesignStyle
import com.metrolist.music.constants.PlayerDesignStyleKey
import com.metrolist.music.constants.PlayerHorizontalPadding
import com.metrolist.music.constants.SeekExtraSeconds
import com.metrolist.music.constants.SwipeThumbnailKey
import com.metrolist.music.constants.ThumbnailCornerRadius
import com.metrolist.music.listentogether.RoomRole
import com.metrolist.music.ui.component.CastButton
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Pre-calculated thumbnail dimensions to avoid repeated calculations during recomposition.
 * All values are computed once and cached.
 */
@Immutable
data class ThumbnailDimensions(
    val itemWidth: Dp,
    val containerSize: Dp,
    val thumbnailSize: Dp,
    val cornerRadius: Dp
)

/**
 * Cached media items data to prevent recalculation on every recomposition.
 */
@Immutable
data class MediaItemsData(
    val items: List<MediaItem>,
    val currentIndex: Int
)

/**
 * Calculate thumbnail dimensions once based on container size.
 * This function is marked as @Stable to indicate it produces stable results.
 * In landscape mode, uses the smaller dimension (height) to ensure square thumbnail fits.
 */
@Stable
private fun calculateThumbnailDimensions(
    containerWidth: Dp,
    containerHeight: Dp = containerWidth,
    horizontalPadding: Dp = PlayerHorizontalPadding,
    cornerRadius: Dp = ThumbnailCornerRadius,
    isLandscape: Boolean = false
): ThumbnailDimensions {
    // In landscape, use height as the constraining dimension for a square thumbnail
    val effectiveSize = if (isLandscape) {
        minOf(containerWidth, containerHeight) - (horizontalPadding * 2)
    } else {
        containerWidth - (horizontalPadding * 2)
    }
    return ThumbnailDimensions(
        itemWidth = containerWidth,
        containerSize = containerWidth,
        thumbnailSize = effectiveSize,
        cornerRadius = cornerRadius * 2
    )
}

/**
 * Get media items for the thumbnail carousel.
 * Calculates previous, current, and next items based on shuffle mode.
 */
@Stable
private fun getMediaItems(
    player: Player,
    swipeThumbnail: Boolean
): MediaItemsData {
    val timeline = player.currentTimeline
    val currentIndex = player.currentMediaItemIndex
    val shuffleModeEnabled = player.shuffleModeEnabled
    
    val currentMediaItem = try {
        player.currentMediaItem
    } catch (e: Exception) { null }
    
    val previousMediaItem = if (swipeThumbnail && !timeline.isEmpty) {
        val previousIndex = timeline.getPreviousWindowIndex(
            currentIndex,
            Player.REPEAT_MODE_OFF,
            shuffleModeEnabled
        )
        if (previousIndex != C.INDEX_UNSET) {
            try { player.getMediaItemAt(previousIndex) } catch (e: Exception) { null }
        } else null
    } else null

    val nextMediaItem = if (swipeThumbnail && !timeline.isEmpty) {
        val nextIndex = timeline.getNextWindowIndex(
            currentIndex,
            Player.REPEAT_MODE_OFF,
            shuffleModeEnabled
        )
        if (nextIndex != C.INDEX_UNSET) {
            try { player.getMediaItemAt(nextIndex) } catch (e: Exception) { null }
        } else null
    } else null

    val items = listOfNotNull(previousMediaItem, currentMediaItem, nextMediaItem)
    val currentMediaIndex = items.indexOf(currentMediaItem)
    
    return MediaItemsData(items, currentMediaIndex)
}

/**
 * Get text color based on player background style.
 * Computed once per background style change.
 */
@Stable
@Composable
private fun getTextColor(playerBackground: PlayerBackgroundStyle): Color {
    return when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.onBackground
        PlayerBackgroundStyle.BLUR -> Color.White
        PlayerBackgroundStyle.GRADIENT -> Color.White
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Thumbnail(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier,
    isPlayerExpanded: () -> Boolean = { true },
    isLandscape: Boolean = false,
    isListenTogetherGuest: Boolean = false,
    isExpressiveCoverBright: Boolean = false,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current
    val layoutDirection = LocalLayoutDirection.current

    // Collect states
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val error by playerConnection.error.collectAsState()
    val queueTitle by playerConnection.queueTitle.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()

    // Preferences - computed once
    // Disable swipe for Listen Together guests
    val swipeThumbnailPref by rememberPreference(SwipeThumbnailKey, true)
    val swipeThumbnail = swipeThumbnailPref && !isListenTogetherGuest
    val hidePlayerThumbnail by rememberPreference(HidePlayerThumbnailKey, false)
    val cropAlbumArt by rememberPreference(CropAlbumArtKey, false)
    val albumCoverTransition by rememberPreference(AlbumCoverTransitionKey, true)
    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.DEFAULT
    )
    val playerDesignStyle by rememberEnumPreference(
        key = PlayerDesignStyleKey,
        defaultValue = PlayerDesignStyle.MATERIAL_YOU
    )
    
    // Grid state
    val thumbnailLazyGridState = rememberLazyGridState()
    
    // Calculate media items data - memoized
    val mediaItemsData by remember(
        playerConnection.player.currentMediaItemIndex,
        playerConnection.player.shuffleModeEnabled,
        swipeThumbnail,
        mediaMetadata
    ) {
        derivedStateOf {
            getMediaItems(playerConnection.player, swipeThumbnail)
        }
    }
    
    val mediaItems = mediaItemsData.items
    val currentMediaIndex = mediaItemsData.currentIndex

    // Snap behavior - created once per grid state
    val thumbnailSnapLayoutInfoProvider = remember(thumbnailLazyGridState) {
        ThumbnailSnapLayoutInfoProvider(
            lazyGridState = thumbnailLazyGridState,
            positionInLayout = { layoutSize, itemSize ->
                (layoutSize / 2f - itemSize / 2f)
            },
            velocityThreshold = 500f
        )
    }

    // Current item tracking - derived state for efficiency
    val currentItem by remember { derivedStateOf { thumbnailLazyGridState.firstVisibleItemIndex } }
    val itemScrollOffset by remember { derivedStateOf { thumbnailLazyGridState.firstVisibleItemScrollOffset } }

    // Handle swipe to change song
    LaunchedEffect(itemScrollOffset) {
        if (!thumbnailLazyGridState.isScrollInProgress || !swipeThumbnail || itemScrollOffset != 0 || currentMediaIndex < 0) return@LaunchedEffect

        if (currentItem > currentMediaIndex && canSkipNext) {
            playerConnection.player.seekToNext()
        } else if (currentItem < currentMediaIndex && canSkipPrevious) {
            playerConnection.player.seekToPreviousMediaItem()
        }
    }

    // Update position when song changes
    LaunchedEffect(mediaMetadata, canSkipPrevious, canSkipNext) {
        val index = maxOf(0, currentMediaIndex)
        if (index >= 0 && index < mediaItems.size) {
            try {
                thumbnailLazyGridState.animateScrollToItem(index)
            } catch (e: Exception) {
                thumbnailLazyGridState.scrollToItem(index)
            }
        }
    }

    LaunchedEffect(playerConnection.player.currentMediaItemIndex) {
        val index = mediaItemsData.currentIndex
        if (index >= 0 && index != currentItem) {
            thumbnailLazyGridState.scrollToItem(index)
        }
    }

    // Seek effect state
    var showSeekEffect by remember { mutableStateOf(false) }
    var seekDirection by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .graphicsLayer {
                // Use hardware layer for entire Thumbnail to ensure smooth 120Hz animations
                compositingStrategy = CompositingStrategy.Offscreen
            }
    ) {
        // Error view
        AnimatedVisibility(
            visible = error != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .padding(32.dp)
                .align(Alignment.Center),
        ) {
            error?.let { playbackError ->
                PlaybackError(
                    error = playbackError,
                    retry = playerConnection.player::prepare,
                )
            }
        }

    // Main thumbnail view
    // For Expressive portrait: no status bar padding on container (art goes edge-to-edge)
    // but header is overlaid on top of art with its own status bar padding
    val isExpressivePortrait = playerDesignStyle == PlayerDesignStyle.EXPRESSIVE && !isLandscape
    
    // Pre-calculate text color based on background style and cover brightness for Expressive
    val textBackgroundColor = if (isExpressivePortrait) {
        if (isExpressiveCoverBright) Color.Black else Color.White
    } else {
        getTextColor(playerBackground)
    }

    AnimatedVisibility(
            visible = error == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (!isLandscape && !isExpressivePortrait) Modifier.statusBarsPadding()
                    else Modifier
                ),
        ) {
            if (isExpressivePortrait && albumCoverTransition) {
                // Expressive portrait with Scale Morph + Color Wash transition
                ExpressiveTransitionThumbnail(
                    mediaMetadata = mediaMetadata,
                    hidePlayerThumbnail = hidePlayerThumbnail,
                    textBackgroundColor = textBackgroundColor,
                    queueTitle = queueTitle,
                    swipeThumbnail = swipeThumbnail,
                    isListenTogetherGuest = isListenTogetherGuest,
                    isPlayerExpanded = isPlayerExpanded,
                    playerConnection = playerConnection,
                    context = context,
                    layoutDirection = layoutDirection,
                    onSeekEffect = { direction, show ->
                        seekDirection = direction
                        showSeekEffect = show
                    }
                )
            } else if (isExpressivePortrait) {
                // Expressive portrait: Box layout so header overlays the art (no transition)
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Thumbnail content fills the entire space
                    BoxWithConstraints(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Calculate dimensions once per size change
                        val dimensions = remember(maxWidth, maxHeight, isExpressivePortrait) {
                            calculateThumbnailDimensions(
                                containerWidth = maxWidth,
                                containerHeight = maxHeight,
                                horizontalPadding = 0.dp,
                                cornerRadius = 0.dp,
                                isLandscape = false
                            )
                        }

                        val onSeekCallback = remember {
                            { direction: String, showEffect: Boolean ->
                                seekDirection = direction
                                showSeekEffect = showEffect
                            }
                        }

                        val isScrollEnabled by remember(swipeThumbnail) {
                            derivedStateOf { swipeThumbnail && isPlayerExpanded() }
                        }

                        LazyHorizontalGrid(
                            state = thumbnailLazyGridState,
                            rows = GridCells.Fixed(1),
                            flingBehavior = rememberSnapFlingBehavior(thumbnailSnapLayoutInfoProvider),
                            userScrollEnabled = isScrollEnabled,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = mediaItems,
                                key = { item ->
                                    item.mediaId.ifEmpty { "unknown_${item.hashCode()}" }
                                }
                            ) { item ->
                                ThumbnailItem(
                                    item = item,
                                    dimensions = dimensions,
                                    hidePlayerThumbnail = hidePlayerThumbnail,
                                    cropAlbumArt = cropAlbumArt,
                                    textBackgroundColor = textBackgroundColor,
                                    layoutDirection = layoutDirection,
                                    onSeek = onSeekCallback,
                                    playerConnection = playerConnection,
                                    context = context,
                                    isLandscape = false,
                                    isListenTogetherGuest = isListenTogetherGuest,
                                    currentMediaId = mediaMetadata?.id,
                                    currentMediaThumbnail = mediaMetadata?.thumbnailUrl,
                                    isExpressivePortrait = true,
                                    playerBackground = playerBackground
                                )
                            }
                        }
                    }

                    // Now Playing header overlaid on top of art
                    Box(modifier = Modifier.statusBarsPadding()) {
                        ThumbnailHeader(
                            queueTitle = queueTitle,
                            albumTitle = mediaMetadata?.album?.title,
                            textColor = textBackgroundColor
                        )
                    }
                }
            } else {
                // Standard layout: Column with header above thumbnail
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = if (isLandscape) Arrangement.Center else Arrangement.Top
                ) {
                    // Now Playing header - hide in landscape mode
                    if (!isLandscape) {
                        ThumbnailHeader(
                            queueTitle = queueTitle,
                            albumTitle = mediaMetadata?.album?.title,
                            textColor = textBackgroundColor
                        )
                    }

                    // Thumbnail content
                    BoxWithConstraints(
                        contentAlignment = Alignment.Center,
                        modifier = if (isLandscape) {
                            Modifier.weight(1f, false)
                        } else {
                            Modifier.fillMaxSize()
                        }
                    ) {
                        // Calculate dimensions once per size change, considering landscape mode
                        val dimensions = remember(maxWidth, maxHeight, isLandscape) {
                            calculateThumbnailDimensions(
                                containerWidth = maxWidth,
                                containerHeight = maxHeight,
                                isLandscape = isLandscape
                            )
                        }

                        val onSeekCallback = remember {
                            { direction: String, showEffect: Boolean ->
                                seekDirection = direction
                                showSeekEffect = showEffect
                            }
                        }

                        val isScrollEnabled by remember(swipeThumbnail) {
                            derivedStateOf { swipeThumbnail && isPlayerExpanded() }
                        }

                        LazyHorizontalGrid(
                            state = thumbnailLazyGridState,
                            rows = GridCells.Fixed(1),
                            flingBehavior = rememberSnapFlingBehavior(thumbnailSnapLayoutInfoProvider),
                            userScrollEnabled = isScrollEnabled,
                            modifier = if (isLandscape) {
                                Modifier.size(dimensions.thumbnailSize + (PlayerHorizontalPadding * 2))
                            } else {
                                Modifier.fillMaxSize()
                            }
                        ) {
                            items(
                                items = mediaItems,
                                key = { item ->
                                    item.mediaId.ifEmpty { "unknown_${item.hashCode()}" }
                                }
                            ) { item ->
                                ThumbnailItem(
                                    item = item,
                                    dimensions = dimensions,
                                    hidePlayerThumbnail = hidePlayerThumbnail,
                                    cropAlbumArt = cropAlbumArt,
                                    textBackgroundColor = textBackgroundColor,
                                    layoutDirection = layoutDirection,
                                    onSeek = onSeekCallback,
                                    playerConnection = playerConnection,
                                    context = context,
                                    isLandscape = isLandscape,
                                    isListenTogetherGuest = isListenTogetherGuest,
                                    currentMediaId = mediaMetadata?.id,
                                    currentMediaThumbnail = mediaMetadata?.thumbnailUrl,
                                    isExpressivePortrait = false,
                                    playerBackground = playerBackground
                                )
                            }
                        }
                    }
                }
            }
        }

        // Seek effect
        LaunchedEffect(showSeekEffect) {
            if (showSeekEffect) {
                delay(1000)
                showSeekEffect = false
            }
        }

        AnimatedVisibility(
            visible = showSeekEffect,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            SeekEffectOverlay(seekDirection = seekDirection)
        }
    }
}

/**
 * Expressive portrait thumbnail with Scale Morph + Color Wash transition.
 *
 * Uses a manual two-layer animation system (NOT AnimatedContent) to achieve a
 * truly dramatic, cinematic transition between album art:
 *
 * - Old image: scales up dramatically (1.0 -> 1.35x), rotates slightly (0 -> 4deg),
 *   translates upward, and fades to transparent
 * - New image: starts scaled up (1.3x), rotated (-3deg), translated downward, and
 *   transparent — then settles into its final position at 1.0x with no rotation
 * - A color wash overlay (extracted via Palette) sweeps across during the transition,
 *   creating a smooth bridge between the old and new color palettes
 *
 * The entire transition is driven by a single Animatable progress (0f -> 1f) over
 * ~1800ms, ensuring both layers stay perfectly synchronized. All transforms use
 * graphicsLayer for GPU-accelerated rendering with zero overdraw impact.
 *
 * Horizontal swipe gestures and double-tap seek are preserved.
 */
@Composable
private fun ExpressiveTransitionThumbnail(
    mediaMetadata: com.metrolist.music.models.MediaMetadata?,
    hidePlayerThumbnail: Boolean,
    textBackgroundColor: Color,
    queueTitle: String?,
    swipeThumbnail: Boolean,
    isListenTogetherGuest: Boolean,
    isPlayerExpanded: () -> Boolean,
    playerConnection: com.metrolist.music.playback.PlayerConnection,
    context: android.content.Context,
    layoutDirection: LayoutDirection,
    onSeekEffect: (String, Boolean) -> Unit,
) {
    val incrementalSeekSkipEnabled by rememberPreference(SeekExtraSeconds, defaultValue = false)

    val currentArtworkUri = mediaMetadata?.thumbnailUrl

    // Two-layer state: track previous and current artwork URIs independently
    var displayedUri by remember { mutableStateOf(currentArtworkUri) }
    var previousUri by remember { mutableStateOf<String?>(null) }
    val transitionProgress = remember { Animatable(1f) }

    // Easing for the dramatic morph curve
    val morphEasing = remember { CubicBezierEasing(0.22f, 1f, 0.36f, 1f) }

    // Color wash state
    var colorWashColor by remember { mutableStateOf(Color.Transparent) }
    var previousWashColor by remember { mutableStateOf(Color.Transparent) }
    val colorWashAlpha = remember { Animatable(0f) }

    // Trigger transition when artwork changes
    LaunchedEffect(currentArtworkUri) {
        if (currentArtworkUri == null) return@LaunchedEffect
        if (currentArtworkUri == displayedUri) return@LaunchedEffect

        // Snapshot the old image URI before swapping
        previousUri = displayedUri
        previousWashColor = colorWashColor

        // Extract palette from the NEW artwork for color wash
        launch(Dispatchers.Default) {
            try {
                val request = ImageRequest.Builder(context)
                    .data(currentArtworkUri)
                    .size(80, 80)
                    .allowHardware(false)
                    .memoryCacheKey("palette_transition_$currentArtworkUri")
                    .build()
                val result = context.imageLoader.execute(request)
                val bitmap = result.image?.toBitmap()
                if (bitmap != null) {
                    val palette = Palette.from(bitmap).generate()
                    val dominant = palette.getMutedColor(
                        palette.getDominantColor(0)
                    )
                    if (dominant != 0) {
                        colorWashColor = Color(dominant)
                    }
                }
            } catch (_: Exception) { }
        }

        // Swap to new artwork immediately so both layers have their URIs
        displayedUri = currentArtworkUri

        // If an animation was already running (rapid skip), we snap to the end
        // to avoid a jarring visual pop where the half-scaled image suddenly jumps to 1x
        if (transitionProgress.isRunning) {
            transitionProgress.snapTo(1f)
            previousUri = null
            colorWashAlpha.snapTo(0f)
        }

        // Drive the color wash: quick pulse that peaks mid-transition
        launch {
            colorWashAlpha.snapTo(0f)
            colorWashAlpha.animateTo(
                targetValue = 0.55f,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
            colorWashAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 1300, easing = FastOutSlowInEasing)
            )
        }

        // Drive the main morph: 0f (fully old) -> 1f (fully new)
        transitionProgress.snapTo(0f)
        transitionProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1800, easing = morphEasing)
        )
        // Clean up old layer after transition completes
        previousUri = null
    }

    // Swipe state
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(swipeThumbnail, isListenTogetherGuest) {
                    if (!swipeThumbnail || isListenTogetherGuest) return@pointerInput
                    var dragAccumulator = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { dragAccumulator = 0f },
                        onDragEnd = {
                            val threshold = size.width * 0.2f
                            if (dragAccumulator < -threshold && canSkipNext) {
                                playerConnection.player.seekToNext()
                            } else if (dragAccumulator > threshold && canSkipPrevious) {
                                playerConnection.player.seekToPreviousMediaItem()
                            }
                            dragAccumulator = 0f
                        },
                        onDragCancel = { dragAccumulator = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            dragAccumulator += dragAmount
                        }
                    )
                }
                .pointerInput(isListenTogetherGuest, layoutDirection) {
                    var lastTapTime = 0L
                    var skipMultiplier = 1
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            if (isListenTogetherGuest) return@detectTapGestures

                            val currentPosition = playerConnection.player.currentPosition
                            val duration = playerConnection.player.duration

                            val now = System.currentTimeMillis()
                            if (incrementalSeekSkipEnabled && now - lastTapTime < 1000) {
                                skipMultiplier++
                            } else {
                                skipMultiplier = 1
                            }
                            lastTapTime = now

                            val skipAmount = 5000 * skipMultiplier

                            val isLeftSide =
                                (layoutDirection == LayoutDirection.Ltr && offset.x < size.width / 2) ||
                                        (layoutDirection == LayoutDirection.Rtl && offset.x > size.width / 2)

                            if (isLeftSide) {
                                playerConnection.player.seekTo(
                                    (currentPosition - skipAmount).coerceAtLeast(0)
                                )
                                onSeekEffect(
                                    context.getString(R.string.seek_backward_dynamic, skipAmount / 1000),
                                    true
                                )
                            } else {
                                playerConnection.player.seekTo(
                                    (currentPosition + skipAmount).coerceAtMost(duration)
                                )
                                onSeekEffect(
                                    context.getString(R.string.seek_forward_dynamic, skipAmount / 1000),
                                    true
                                )
                            }
                        }
                    )
                }
        ) {
            // --- LAYER 1: Previous (outgoing) image ---
            // Scales up, rotates slightly, drifts up, fades out
            if (previousUri != null) {
                ExpressiveCoverLayer(
                    artworkUri = previousUri,
                    hidePlayerThumbnail = hidePlayerThumbnail,
                    textBackgroundColor = textBackgroundColor,
                    context = context,
                    showCastButton = false,
                    modifier = Modifier.graphicsLayer {
                        val progress = transitionProgress.value
                        if (progress >= 1f) {
                            alpha = 0f
                            return@graphicsLayer
                        }
                        alpha = (1f - progress * 1.5f).coerceIn(0f, 1f)
                        val outScale = 1f + (progress * 0.35f)         // 1.0 -> 1.35x
                        scaleX = outScale
                        scaleY = outScale
                        rotationZ = progress * 4f                 // 0 -> 4 degrees
                        translationY = -(progress * 120f)          // drift upward
                        clip = true
                    }
                )
            }

            // --- LAYER 2: Current (incoming) image ---
            // Starts scaled up, rotated opposite direction, translated down, then settles
            ExpressiveCoverLayer(
                artworkUri = displayedUri,
                hidePlayerThumbnail = hidePlayerThumbnail,
                textBackgroundColor = textBackgroundColor,
                context = context,
                showCastButton = true,
                modifier = Modifier.graphicsLayer {
                    val progress = transitionProgress.value
                    if (previousUri != null && progress < 1f) {
                        alpha = (progress * 1.8f).coerceIn(0f, 1f)
                        val inScale = 1.3f - (progress * 0.3f)   // 1.3x -> 1.0x
                        scaleX = inScale
                        scaleY = inScale
                        rotationZ = -3f * (1f - progress)       // -3deg -> 0deg
                        translationY = 80f * (1f - progress)       // 80px -> 0px
                    } else {
                        alpha = 1f
                        scaleX = 1f
                        scaleY = 1f
                        rotationZ = 0f
                        translationY = 0f
                    }
                    clip = true
                }
            )

            // --- LAYER 3: Color wash overlay ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.7f)
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                        alpha = colorWashAlpha.value
                    }
                    .drawWithContent {
                        val progress = transitionProgress.value
                        val blendedWashColor = if (previousWashColor != Color.Transparent) {
                            androidx.compose.ui.graphics.lerp(previousWashColor, colorWashColor, progress)
                        } else {
                            colorWashColor
                        }

                        // Draw the background radial gradient directly to avoid recomposition
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    blendedWashColor.copy(alpha = 0.7f),
                                    blendedWashColor.copy(alpha = 0.35f),
                                    Color.Transparent
                                ),
                                radius = size.width
                            )
                        )
                        
                        drawContent()

                        // Apply bottom fade
                        drawRect(
                            brush = Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Black,
                                    0.5f to Color.Black,
                                    1.0f to Color.Transparent
                                )
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
            )
        }

        // Now Playing header overlaid on top of art
        Box(modifier = Modifier.statusBarsPadding()) {
            ThumbnailHeader(
                queueTitle = queueTitle,
                albumTitle = mediaMetadata?.album?.title,
                textColor = textBackgroundColor
            )
        }
    }
}

/**
 * A single layer of the Expressive cover art with edge-to-edge rendering
 * and bottom gradient fade. Used by ExpressiveTransitionThumbnail to render
 * both the outgoing and incoming artwork layers independently.
 */
@Composable
private fun ExpressiveCoverLayer(
    artworkUri: String?,
    hidePlayerThumbnail: Boolean,
    textBackgroundColor: Color,
    context: android.content.Context,
    showCastButton: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.7f)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Black,
                            0.5f to Color.Black,
                            1.0f to Color.Transparent
                        )
                    ),
                    blendMode = BlendMode.DstIn
                )
            }
    ) {
        if (hidePlayerThumbnail) {
            HiddenThumbnailPlaceholder(
                textBackgroundColor = textBackgroundColor,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(artworkUri)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .networkCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (showCastButton) {
            CastButton(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                tintColor = textBackgroundColor
            )
        }
    }
}

/**
 * Header component showing "Now Playing" and queue/album title.
 */
@Composable
private fun ThumbnailHeader(
    queueTitle: String?,
    albumTitle: String?,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val listenTogetherManager = LocalListenTogetherManager.current
    val listenTogetherRoleState = listenTogetherManager?.role?.collectAsState(initial = RoomRole.NONE)
    val isListenTogetherGuest = listenTogetherRoleState?.value == RoomRole.GUEST
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 48.dp)
        ) {
            // Listen Together indicator
            if (listenTogetherRoleState?.value != RoomRole.NONE) {
                Text(
                    text = if (listenTogetherRoleState?.value == RoomRole.HOST) "Hosting Listen Together" else "Listening Together",
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor
                )
            } else {
                Text(
                    text = stringResource(R.string.now_playing),
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor
                )
            }
            val playingFrom = queueTitle ?: albumTitle
            if (!playingFrom.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = playingFrom,
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor.copy(alpha = 0.8f),
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }
        }
    }
}

/**
 * Individual thumbnail item in the carousel.
 */
@Composable
private fun ThumbnailItem(
    item: MediaItem,
    dimensions: ThumbnailDimensions,
    hidePlayerThumbnail: Boolean,
    cropAlbumArt: Boolean,
    textBackgroundColor: Color,
    layoutDirection: LayoutDirection,
    onSeek: (String, Boolean) -> Unit,
    playerConnection: com.metrolist.music.playback.PlayerConnection,
    context: android.content.Context,
    isLandscape: Boolean = false,
    isListenTogetherGuest: Boolean = false,
    currentMediaId: String? = null,
    currentMediaThumbnail: String? = null,
    isExpressivePortrait: Boolean = false,
    playerBackground: PlayerBackgroundStyle = PlayerBackgroundStyle.DEFAULT,
    modifier: Modifier = Modifier,
) {
    val incrementalSeekSkipEnabled by rememberPreference(SeekExtraSeconds, defaultValue = false)

    Box(
        modifier = modifier
            .then(
                if (isLandscape) {
                    Modifier.size(dimensions.thumbnailSize + (PlayerHorizontalPadding * 2))
                } else {
                    Modifier
                        .width(dimensions.itemWidth)
                        .fillMaxSize()
                }
            )
            .then(
                if (isExpressivePortrait) Modifier
                else Modifier.padding(horizontal = PlayerHorizontalPadding)
            )
            .then(
                // Offscreen compositing enables alpha effects (e.g. squiggly slider blending)
                // but it clips gradients drawn outside child bounds — skip it for Expressive
                if (isExpressivePortrait) Modifier
                else Modifier.graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                }
            )
            .pointerInput(isListenTogetherGuest, layoutDirection) {
                var lastTapTime = 0L
                var skipMultiplier = 1
                detectTapGestures(
                    onDoubleTap = { offset ->
                        if (isListenTogetherGuest) return@detectTapGestures

                        val currentPosition = playerConnection.player.currentPosition
                        val duration = playerConnection.player.duration

                        val now = System.currentTimeMillis()
                        if (incrementalSeekSkipEnabled && now - lastTapTime < 1000) {
                            skipMultiplier++
                        } else {
                            skipMultiplier = 1
                        }
                        lastTapTime = now

                        val skipAmount = 5000 * skipMultiplier

                        val isLeftSide = (layoutDirection == LayoutDirection.Ltr && offset.x < size.width / 2) ||
                                (layoutDirection == LayoutDirection.Rtl && offset.x > size.width / 2)

                        if (isLeftSide) {
                            playerConnection.player.seekTo((currentPosition - skipAmount).coerceAtLeast(0))
                            onSeek(context.getString(R.string.seek_backward_dynamic, skipAmount / 1000), true)
                        } else {
                            playerConnection.player.seekTo((currentPosition + skipAmount).coerceAtMost(duration))
                            onSeek(context.getString(R.string.seek_forward_dynamic, skipAmount / 1000), true)
                        }
                    }
                )
            },
        contentAlignment = Alignment.TopCenter
    ) {
        if (isExpressivePortrait) {
            // Expressive portrait:
            // - Album art spans the FULL height of the available space
            // - The bottom of the image fades to transparent, revealing the
            //   PlayerBackground (blur, gradient, or solid) below it smoothly.
            val artworkUriToUse = if (item.mediaId == currentMediaId && !currentMediaThumbnail.isNullOrBlank()) {
                currentMediaThumbnail
            } else {
                item.mediaMetadata.artworkUri?.toString()
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.7f) // Restrict image to top 70% of screen (stops before slider)
                    .align(Alignment.TopCenter)
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Black, // Start of Green area
                                    0.5f to Color.Black, // End of Green area (fully opaque)
                                    1.0f to Color.Transparent // Blue area: fades to transparent at bottom of the 70% box
                                )
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
            ) {
                if (hidePlayerThumbnail) {
                    HiddenThumbnailPlaceholder(
                        textBackgroundColor = textBackgroundColor,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(artworkUriToUse)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .networkCachePolicy(CachePolicy.ENABLED)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Cast button at top-right
                CastButton(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    tintColor = textBackgroundColor
                )
            }
        } else {
            // Standard: square image with corner radius
            Box(
                modifier = Modifier
                    .size(dimensions.thumbnailSize)
                    .clip(RoundedCornerShape(dimensions.cornerRadius))
            ) {
                if (hidePlayerThumbnail) {
                    HiddenThumbnailPlaceholder(textBackgroundColor = textBackgroundColor)
                } else {
                    val artworkUriToUse = if (item.mediaId == currentMediaId && !currentMediaThumbnail.isNullOrBlank()) {
                        currentMediaThumbnail
                    } else {
                        item.mediaMetadata.artworkUri?.toString()
                    }
                    ThumbnailImage(
                        artworkUri = artworkUriToUse,
                        cropArtwork = cropAlbumArt
                    )
                }

                CastButton(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    tintColor = textBackgroundColor
                )
            }
        }
    }
}

/**
 * Placeholder shown when thumbnail is hidden.
 */
@Composable
private fun HiddenThumbnailPlaceholder(
    textBackgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.small_icon),
            contentDescription = stringResource(R.string.hide_player_thumbnail),
            tint = textBackgroundColor.copy(alpha = 0.7f),
            modifier = Modifier.size(120.dp)
        )
    }
}

/**
 * Actual thumbnail image with caching and hardware layer rendering.
 */
@Composable
private fun ThumbnailImage(
    artworkUri: String?,
    cropArtwork: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                // Use offscreen compositing for hardware acceleration during animations
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(artworkUri)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .networkCachePolicy(CachePolicy.ENABLED)
                .build(),
            contentDescription = null,
            contentScale = if (cropArtwork) ContentScale.Crop else ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Seek effect overlay showing seek direction.
 */
@Composable
private fun SeekEffectOverlay(
    seekDirection: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = seekDirection,
        color = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            .padding(8.dp)
    )
}
