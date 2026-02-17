/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.carousel.CarouselDefaults
import androidx.compose.material3.carousel.CarouselState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import com.metrolist.music.viewmodels.DailyDiscoverItem
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.innertube.models.YTItem
import com.metrolist.music.models.MediaMetadata
import com.metrolist.innertube.utils.parseCookieString
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.GridItemSize
import com.metrolist.music.constants.GridItemsSizeKey
import com.metrolist.music.constants.GridThumbnailHeight
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.constants.ListItemHeight
import com.metrolist.music.constants.ListThumbnailSize
import com.metrolist.music.constants.SmallGridThumbnailHeight
import com.metrolist.music.constants.ThumbnailCornerRadius
import com.metrolist.music.db.entities.Album
import com.metrolist.music.db.entities.Artist
import com.metrolist.music.db.entities.LocalItem
import com.metrolist.music.db.entities.Playlist
import com.metrolist.music.db.entities.Song
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.queues.LocalAlbumRadio
import com.metrolist.music.playback.queues.YouTubeAlbumRadio
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.ui.component.YouTubeListItem
import com.metrolist.music.ui.component.AlbumGridItem
import com.metrolist.music.ui.component.ArtistGridItem
import com.metrolist.music.ui.component.ChipsRow
import com.metrolist.music.ui.component.HideOnScrollFAB
import com.metrolist.music.ui.component.LocalBottomSheetPageState
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.NavigationTitle
import com.metrolist.music.ui.component.RandomizeGridItem
import com.metrolist.music.ui.component.SongGridItem
import com.metrolist.music.ui.component.SongListItem
import com.metrolist.music.ui.component.SpeedDialGridItem
import com.metrolist.music.ui.component.YouTubeGridItem
import com.metrolist.music.ui.component.shimmer.GridItemPlaceHolder
import com.metrolist.music.ui.component.shimmer.ShimmerHost
import com.metrolist.music.ui.component.shimmer.TextPlaceholder
import com.metrolist.music.ui.menu.AlbumMenu
import com.metrolist.music.ui.menu.ArtistMenu
import com.metrolist.music.ui.menu.SongMenu
import com.metrolist.music.ui.menu.YouTubeAlbumMenu
import com.metrolist.music.ui.menu.YouTubeArtistMenu
import com.metrolist.music.ui.menu.YouTubePlaylistMenu
import com.metrolist.music.ui.menu.YouTubeSongMenu
import com.metrolist.music.ui.utils.SnapLayoutInfoProvider
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.random.Random



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyDiscoverCard(
    dailyDiscover: com.metrolist.music.viewmodels.DailyDiscoverItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val database = LocalDatabase.current
    val playCount by database.getLifetimePlayCount(dailyDiscover.recommendation.id).collectAsState(initial = 0)

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = RoundedCornerShape(28.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(dailyDiscover.recommendation.thumbnail)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
            )
            
            if (maxWidth > 200.dp) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.3f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.6f),
                                    Color.Black.copy(alpha = 0.9f)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = dailyDiscover.recommendation.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = buildString {
                                append((dailyDiscover.recommendation as? SongItem)?.artists?.joinToString(", ") { it.name } ?: "")
                                if (playCount > 0) {
                                    append(" • $playCount plays")
                                }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    Text(
                        text = "Sounds like ${dailyDiscover.seed.title} by ${dailyDiscover.seed.artists.joinToString(", ") { it.name }}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    snackbarHostState: SnackbarHostState,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current

    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val quickPicks by viewModel.quickPicks.collectAsState()
    val forgottenFavorites by viewModel.forgottenFavorites.collectAsState()
    val keepListening by viewModel.keepListening.collectAsState()
    val similarRecommendations by viewModel.similarRecommendations.collectAsState()
    val accountPlaylists by viewModel.accountPlaylists.collectAsState()
    val homePage by viewModel.homePage.collectAsState()
    val explorePage by viewModel.explorePage.collectAsState()
    val dailyDiscover by viewModel.dailyDiscover.collectAsState()

    val allLocalItems by viewModel.allLocalItems.collectAsState()
    val allYtItems by viewModel.allYtItems.collectAsState()
    val speedDialItems by viewModel.speedDialItems.collectAsState()
    val selectedChip by viewModel.selectedChip.collectAsState()

    val isLoading: Boolean by viewModel.isLoading.collectAsState()
    val isMoodAndGenresLoading = isLoading && explorePage?.moodAndGenres == null
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isRandomizing by viewModel.isRandomizing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    val quickPicksLazyGridState = rememberLazyGridState()
    val forgottenFavoritesLazyGridState = rememberLazyGridState()

    val accountName by viewModel.accountName.collectAsState()
    val accountImageUrl by viewModel.accountImageUrl.collectAsState()
    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")

    val shouldShowWrappedCard by viewModel.showWrappedCard.collectAsState()
    val wrappedState by viewModel.wrappedManager.state.collectAsState()
    val isWrappedDataReady = wrappedState.isDataReady

    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }
    val url = if (isLoggedIn) accountImageUrl else null

    val scope = rememberCoroutineScope()
    val lazylistState = rememberLazyListState()
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)
    val currentGridHeight = if (gridItemSize == GridItemSize.BIG) GridThumbnailHeight else SmallGridThumbnailHeight
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    val wrappedDismissed by backStackEntry?.savedStateHandle?.getStateFlow("wrapped_seen", false)
        ?.collectAsState() ?: remember { mutableStateOf(false) }

    LaunchedEffect(wrappedDismissed) {
        if (wrappedDismissed) {
            viewModel.markWrappedAsSeen()
            scope.launch {
                snackbarHostState.showSnackbar("Found in Settings > Content")
            }
            backStackEntry?.savedStateHandle?.set("wrapped_seen", false) // Reset the value
        }
    }

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazylistState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { lazylistState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                val len = lazylistState.layoutInfo.totalItemsCount
                if (lastVisibleIndex != null && lastVisibleIndex >= len - 3) {
                    viewModel.loadMoreYouTubeItems(homePage?.continuation)
                }
            }
    }

    if (selectedChip != null) {
        BackHandler {
            // if a chip is selected, go back to the normal homepage first
            viewModel.toggleChip(selectedChip)
        }
    }

    val localGridItem: @Composable (LocalItem) -> Unit = {
        when (it) {
            is Song -> SongGridItem(
                song = it,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            if (it.id == mediaMetadata?.id) {
                                playerConnection.togglePlayPause()
                            } else {
                                playerConnection.playQueue(
                                    YouTubeQueue.radio(it.toMediaMetadata()),
                                )
                            }
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(
                                HapticFeedbackType.LongPress,
                            )
                            menuState.show {
                                SongMenu(
                                    originalSong = it,
                                    navController = navController,
                                    onDismiss = menuState::dismiss,
                                )
                            }
                        },
                    ),
                isActive = it.id == mediaMetadata?.id,
                isPlaying = isPlaying,
            )

            is Album -> AlbumGridItem(
                album = it,
                isActive = it.id == mediaMetadata?.album?.id,
                isPlaying = isPlaying,
                coroutineScope = scope,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            navController.navigate("album/${it.id}")
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuState.show {
                                AlbumMenu(
                                    originalAlbum = it,
                                    navController = navController,
                                    onDismiss = menuState::dismiss
                                )
                            }
                        }
                    )
            )

            is Artist -> ArtistGridItem(
                artist = it,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            navController.navigate("artist/${it.id}")
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(
                                HapticFeedbackType.LongPress,
                            )
                            menuState.show {
                                ArtistMenu(
                                    originalArtist = it,
                                    coroutineScope = scope,
                                    onDismiss = menuState::dismiss,
                                )
                            }
                        },
                    ),
            )

            is Playlist -> {}
        }
    }

    val ytGridItem: @Composable (YTItem) -> Unit = { item ->
        YouTubeGridItem(
            item = item,
            isActive = item.id in listOf(mediaMetadata?.album?.id, mediaMetadata?.id),
            isPlaying = isPlaying,
            coroutineScope = scope,
            thumbnailRatio = 1f,
            modifier = Modifier
                .combinedClickable(
                    onClick = {
                        when (item) {
                            is SongItem -> playerConnection.playQueue(
                                YouTubeQueue(
                                    item.endpoint ?: WatchEndpoint(
                                        videoId = item.id
                                    ), item.toMediaMetadata()
                                )
                            )

                            is AlbumItem -> navController.navigate("album/${item.id}")
                            is ArtistItem -> navController.navigate("artist/${item.id}")
                            is PlaylistItem -> {
                                if (item.id.startsWith("VL") || item.id.startsWith("PL") || item.id.startsWith("LL") || item.id.startsWith("RDC")) {
                                    navController.navigate("online_playlist/${item.id.removePrefix("VL")}")
                                } else {
                                    navController.navigate("local_playlist/${item.id}")
                                }
                            }
                        }
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuState.show {
                            when (item) {
                                is SongItem -> YouTubeSongMenu(
                                    song = item,
                                    navController = navController,
                                    onDismiss = menuState::dismiss
                                )

                                is AlbumItem -> YouTubeAlbumMenu(
                                    albumItem = item,
                                    navController = navController,
                                    onDismiss = menuState::dismiss
                                )

                                is ArtistItem -> YouTubeArtistMenu(
                                    artist = item,
                                    onDismiss = menuState::dismiss
                                )

                                is PlaylistItem -> YouTubePlaylistMenu(
                                    playlist = item,
                                    coroutineScope = scope,
                                    onDismiss = menuState::dismiss
                                )
                            }
                        }
                    }
                )
        )
    }

    LaunchedEffect(quickPicks) {
        quickPicksLazyGridState.scrollToItem(0)
    }

    LaunchedEffect(forgottenFavorites) {
        forgottenFavoritesLazyGridState.scrollToItem(0)
    }

    PullToRefreshBox(
        state = pullRefreshState,
        isRefreshing = isRefreshing,
        onRefresh = viewModel::refresh,
        indicator = {
            Indicator(
                isRefreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
            )
        }
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopStart
        ) {
        val horizontalLazyGridItemWidthFactor = if (maxWidth * 0.475f >= 320.dp) 0.475f else 0.9f
        val horizontalLazyGridItemWidth = maxWidth * horizontalLazyGridItemWidthFactor
        val quickPicksSnapLayoutInfoProvider = remember(quickPicksLazyGridState) {
            SnapLayoutInfoProvider(
                lazyGridState = quickPicksLazyGridState,
                positionInLayout = { layoutSize, itemSize ->
                    (layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f)
                }
            )
        }
        val forgottenFavoritesSnapLayoutInfoProvider = remember(forgottenFavoritesLazyGridState) {
            SnapLayoutInfoProvider(
                lazyGridState = forgottenFavoritesLazyGridState,
                positionInLayout = { layoutSize, itemSize ->
                    (layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f)
                }
            )
        }

        LazyColumn(
            state = lazylistState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
        ) {
            item {
                ChipsRow(
                    chips = homePage?.chips?.map { it to it.title } ?: emptyList(),
                    currentValue = selectedChip,
                    onValueUpdate = {
                        viewModel.toggleChip(it)
                    }
                )
            }

            if (selectedChip == null) {
                item(key = "wrapped_card") {
                    AnimatedVisibility(visible = shouldShowWrappedCard) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isWrappedDataReady) {
                                    val bbhFont = try {
                                        FontFamily(Font(R.font.bbh_bartle_regular))
                                    } catch (e: Exception) {
                                        FontFamily.Default
                                    }
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                                    ) {
                                        Text(
                                            text = stringResource(R.string.wrapped_ready_title),
                                            style = MaterialTheme.typography.headlineLarge.copy(
                                                fontFamily = bbhFont,
                                                textAlign = TextAlign.Center
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = stringResource(R.string.wrapped_ready_subtitle),
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                textAlign = TextAlign.Center
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(onClick = {
                                            navController.navigate("wrapped")
                                        }) {
                                            Text(stringResource(R.string.open))
                                        }
                                    }
                                } else {
                                    ContainedLoadingIndicator()
                                }
                            }
                        }
                    }
                }

                speedDialItems.takeIf { it.isNotEmpty() }?.let { items ->
                    item(key = "speed_dial_title") {
                        NavigationTitle(
                            title = stringResource(R.string.speed_dial),
                            modifier = Modifier.animateItem()
                        )
                    }

                    item(key = "speed_dial_list") {
                        val pagerState = rememberPagerState(pageCount = { (items.size + 8) / 9 })
                        val availableWidth = maxWidth - 32.dp 
                        val itemWidth = availableWidth / 3 
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem()
                        ) {
                            HorizontalPager(
                                state = pagerState,
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                pageSpacing = 16.dp, 
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(itemWidth * 3) 
                            ) { page ->
                                val pageStartIndex = page * 9
                                val pageItems = items.drop(pageStartIndex).take(9)
                                
                                Column(modifier = Modifier.fillMaxSize()) {
                                    for (row in 0 until 3) {
                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            for (col in 0 until 3) {
                                                val itemIndex = row * 3 + col
                                                
                                                val isRandomizeSlot = (page == 0 && itemIndex == 8)
                                                
                                                if (isRandomizeSlot) {
                                                     Box(
                                                        modifier = Modifier
                                                            .width(itemWidth)
                                                            .height(itemWidth)
                                                            .padding(4.dp)
                                                    ) {
                                                        RandomizeGridItem(
                                                            isLoading = isRandomizing,
                                                            onClick = {
                                                                scope.launch {
                                                                    val randomItem = viewModel.getRandomItem()
                                                                    if (randomItem != null) {
                                                                        when (randomItem) {
                                                                            is SongItem -> playerConnection.playQueue(
                                                                                YouTubeQueue(
                                                                                    randomItem.endpoint ?: WatchEndpoint(videoId = randomItem.id),
                                                                                    randomItem.toMediaMetadata()
                                                                                )
                                                                            )
                                                                            is AlbumItem -> navController.navigate("album/${randomItem.id}")
                                                                            is ArtistItem -> navController.navigate("artist/${randomItem.id}")
                                                                            is PlaylistItem -> {
                                                                                if (randomItem.id.startsWith("VL") || randomItem.id.startsWith("PL") || randomItem.id.startsWith("LL") || randomItem.id.startsWith("RDC")) {
                                                                                    navController.navigate("online_playlist/${randomItem.id.removePrefix("VL")}")
                                                                                } else {
                                                                                    navController.navigate("local_playlist/${randomItem.id}")
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            },
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    }
                                                } else if (itemIndex < pageItems.size) {
                                                    val item = pageItems[itemIndex]
                                                    val isPinned by database.speedDialDao.isPinned(item.id).collectAsState(initial = false)
                                                    
                                                    Box(
                                                        modifier = Modifier
                                                            .width(itemWidth)
                                                            .height(itemWidth)
                                                            .padding(4.dp) 
                                                    ) {
                                                        SpeedDialGridItem(
                                                            item = item,
                                                            isPinned = isPinned,
                                                            isActive = item.id in listOf(mediaMetadata?.album?.id, mediaMetadata?.id),
                                                            isPlaying = isPlaying,
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .combinedClickable(
                                                        onClick = {
                                                            when (item) {
                                                                is SongItem -> playerConnection.playQueue(
                                                                    YouTubeQueue(
                                                                        item.endpoint ?: WatchEndpoint(videoId = item.id), 
                                                                        item.toMediaMetadata()
                                                                    )
                                                                )
                                                                is AlbumItem -> navController.navigate("album/${item.id}")
                                                                is ArtistItem -> navController.navigate("artist/${item.id}")
                                                                is PlaylistItem -> {
                                                                    if (item.id.startsWith("VL") || item.id.startsWith("PL") || item.id.startsWith("LL") || item.id.startsWith("RDC")) {
                                                                        navController.navigate("online_playlist/${item.id.removePrefix("VL")}")
                                                                    } else {
                                                                        navController.navigate("local_playlist/${item.id}")
                                                                    }
                                                                }
                                                            }
                                                        },
                                                                    onLongClick = {
                                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                        menuState.show {
                                                                            when (item) {
                                                                                is SongItem -> YouTubeSongMenu(
                                                                                    song = item,
                                                                                    navController = navController,
                                                                                    onDismiss = menuState::dismiss
                                                                                )
                                                                                is AlbumItem -> YouTubeAlbumMenu(
                                                                                    albumItem = item,
                                                                                    navController = navController,
                                                                                    onDismiss = menuState::dismiss
                                                                                )
                                                                                is ArtistItem -> YouTubeArtistMenu(
                                                                                    artist = item,
                                                                                    onDismiss = menuState::dismiss
                                                                                )
                                                                                is PlaylistItem -> YouTubePlaylistMenu(
                                                                                    playlist = item,
                                                                                    coroutineScope = scope,
                                                                                    onDismiss = menuState::dismiss
                                                                                )
                                                                            }
                                                                        }
                                                                    }
                                                                )
                                                        )
                                                    }
                                                } else {
                                                    Spacer(modifier = Modifier.width(itemWidth))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            if (pagerState.pageCount > 1) {
                                Row(
                                    modifier = Modifier
                                        .height(24.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    repeat(pagerState.pageCount) { iteration ->
                                        val color = if (pagerState.currentPage == iteration) 
                                            MaterialTheme.colorScheme.primary 
                                        else 
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        Box(
                                            modifier = Modifier
                                                .padding(4.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                                .size(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                quickPicks?.takeIf { it.isNotEmpty() }?.let { quickPicks ->
                    item(key = "quick_picks_title") {
                        val quickPicksTitle = stringResource(R.string.quick_picks)
                        NavigationTitle(
                            title = quickPicksTitle,
                            modifier = Modifier.animateItem(),
                            onPlayAllClick = {
                                playerConnection.playQueue(
                                    ListQueue(
                                        title = quickPicksTitle,
                                        items = quickPicks.distinctBy { it.id }.map { it.toMediaItem() }
                                    )
                                )
                            }
                        )
                    }

                    item(key = "quick_picks_list") {
                        LazyHorizontalGrid(
                            state = quickPicksLazyGridState,
                            rows = GridCells.Fixed(4),
                            flingBehavior = rememberSnapFlingBehavior(quickPicksSnapLayoutInfoProvider),
                            contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)
                                .asPaddingValues(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(ListItemHeight * 4)
                                .animateItem()
                        ) {
                            items(
                                items = quickPicks.distinctBy { it.id },
                                key = { it.id }
                            ) { originalSong ->
                                // fetch song from database to keep updated
                                val song by database.song(originalSong.id)
                                    .collectAsState(initial = originalSong)

                                SongListItem(
                                    song = song!!,
                                    showInLibraryIcon = true,
                                    isActive = song!!.id == mediaMetadata?.id,
                                    isPlaying = isPlaying,
                                    isSwipeable = false,
                                    trailingContent = {
                                        IconButton(
                                            onClick = {
                                                menuState.show {
                                                    SongMenu(
                                                        originalSong = song!!,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss
                                                    )
                                                }
                                            }
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.more_vert),
                                                contentDescription = null
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .width(horizontalLazyGridItemWidth)
                                        .combinedClickable(
                                            onClick = {
                                                if (song!!.id == mediaMetadata?.id) {
                                                    playerConnection.togglePlayPause()
                                                } else {
                                                    playerConnection.playQueue(
                                                        YouTubeQueue.radio(
                                                            song!!.toMediaMetadata()
                                                        )
                                                    )
                                                }
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    SongMenu(
                                                        originalSong = song!!,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss
                                                    )
                                                }
                                            }
                                        )
                                )
                            }
                        }
                    }
                }

                dailyDiscover?.takeIf { it.isNotEmpty() }?.let { discoverList ->
                    item(key = "daily_discover_title") {
                        NavigationTitle(
                            title = "Your daily discover",
                            onPlayAllClick = {
                                val queueItems = discoverList.mapNotNull {
                                    (it.recommendation as? SongItem)?.toMediaMetadata()
                                }
                                
                                if (queueItems.isNotEmpty()) {
                                    playerConnection.playQueue(
                                        ListQueue(
                                            title = "Your daily discover",
                                            items = queueItems.map { it.toMediaItem() }
                                        )
                                    )
                                }
                            }
                        )
                    }

                    item(key = "daily_discover_content") {
                         Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(340.dp)
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                             val carouselState = rememberCarouselState { discoverList.size }
                             HorizontalMultiBrowseCarousel(
                                state = carouselState,
                                preferredItemWidth = 320.dp,
                                itemSpacing = 16.dp,
                                 modifier = Modifier
                                    .fillMaxWidth()
                                    .height(320.dp)
                             ) { i ->
                                 val item = discoverList[i]
                                 DailyDiscoverCard(
                                     dailyDiscover = item,
                                     onClick = {
                                         val mediaMetadata = (item.recommendation as? SongItem)?.toMediaMetadata()
                                         if (mediaMetadata != null) {
                                             playerConnection.playQueue(
                                                 YouTubeQueue(
                                                     (item.recommendation as? SongItem)?.endpoint ?: WatchEndpoint(videoId = item.recommendation.id),
                                                     mediaMetadata
                                                 )
                                             )
                                         }
                                     },
                                     modifier = Modifier.maskClip(MaterialTheme.shapes.extraLarge)
                                 )
                             }
                        }
                    }
                }

                keepListening?.takeIf { it.isNotEmpty() }?.let { keepListening ->
                    item(key = "keep_listening_title") {
                        NavigationTitle(
                            title = stringResource(R.string.keep_listening),
                            modifier = Modifier.animateItem()
                        )
                    }

                    item(key = "keep_listening_list") {
                        val rows = if (keepListening.size > 6) 2 else 1
                        LazyHorizontalGrid(
                            state = rememberLazyGridState(),
                            rows = GridCells.Fixed(rows),
                            contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)
                                .asPaddingValues(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((currentGridHeight + with(LocalDensity.current) {
                                    MaterialTheme.typography.bodyLarge.lineHeight.toDp() * 2 +
                                            MaterialTheme.typography.bodyMedium.lineHeight.toDp() * 2
                                }) * rows)
                                .animateItem()
                        ) {
                            items(keepListening) {
                                localGridItem(it)
                            }
                        }
                    }
                }

                accountPlaylists?.takeIf { it.isNotEmpty() }?.let { accountPlaylists ->
                    item(key = "account_playlists_title") {
                        NavigationTitle(
                            label = stringResource(R.string.your_youtube_playlists),
                            title = accountName,
                            thumbnail = {
                                if (url != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(url)
                                            .diskCachePolicy(CachePolicy.ENABLED)
                                            .diskCacheKey(url)
                                            .crossfade(false)
                                            .build(),
                                        placeholder = painterResource(id = R.drawable.person),
                                        error = painterResource(id = R.drawable.person),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(ListThumbnailSize)
                                            .clip(CircleShape)
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(id = R.drawable.person),
                                        contentDescription = null,
                                        modifier = Modifier.size(ListThumbnailSize)
                                    )
                                }
                            },
                            onClick = {
                                navController.navigate("account")
                            },
                            modifier = Modifier.animateItem()
                        )
                    }

                    item(key = "account_playlists_list") {
                        LazyRow(
                            contentPadding = WindowInsets.systemBars
                                .only(WindowInsetsSides.Horizontal)
                                .asPaddingValues(),
                            modifier = Modifier.animateItem()
                        ) {
                            items(
                                items = accountPlaylists.distinctBy { it.id },
                                key = { it.id },
                            ) { item ->
                                ytGridItem(item)
                            }
                        }
                    }
                }

                forgottenFavorites?.takeIf { it.isNotEmpty() }?.let { forgottenFavorites ->
                    item(key = "forgotten_favorites_title") {
                        val forgottenFavoritesTitle = stringResource(R.string.forgotten_favorites)
                        NavigationTitle(
                            title = forgottenFavoritesTitle,
                            modifier = Modifier.animateItem(),
                            onPlayAllClick = {
                                playerConnection.playQueue(
                                    ListQueue(
                                        title = forgottenFavoritesTitle,
                                        items = forgottenFavorites.distinctBy { it.id }.map { it.toMediaItem() }
                                    )
                                )
                            }
                        )
                    }

                    item(key = "forgotten_favorites_list") {
                        // take min in case list size is less than 4
                        val rows = min(4, forgottenFavorites.size)
                        LazyHorizontalGrid(
                            state = forgottenFavoritesLazyGridState,
                            rows = GridCells.Fixed(rows),
                            contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)
                                .asPaddingValues(),
                            flingBehavior = rememberSnapFlingBehavior(
                                forgottenFavoritesSnapLayoutInfoProvider
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(ListItemHeight * rows)
                                .animateItem()
                        ) {
                            items(
                                items = forgottenFavorites.distinctBy { it.id },
                                key = { it.id }
                            ) { originalSong ->
                                val song by database.song(originalSong.id)
                                    .collectAsState(initial = originalSong)

                                SongListItem(
                                    song = song!!,
                                    showInLibraryIcon = true,
                                    isActive = song!!.id == mediaMetadata?.id,
                                    isPlaying = isPlaying,
                                    isSwipeable = false,
                                    trailingContent = {
                                        IconButton(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    SongMenu(
                                                        originalSong = song!!,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss
                                                    )
                                                }
                                            }
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.more_vert),
                                                contentDescription = null
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .width(horizontalLazyGridItemWidth)
                                        .combinedClickable(
                                            onClick = {
                                                if (song!!.id == mediaMetadata?.id) {
                                                    playerConnection.togglePlayPause()
                                                } else {
                                                    playerConnection.playQueue(
                                                        YouTubeQueue.radio(
                                                            song!!.toMediaMetadata()
                                                        )
                                                    )
                                                }
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    SongMenu(
                                                        originalSong = song!!,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss
                                                    )
                                                }
                                            }
                                        )
                                )
                            }
                        }
                    }
                }

                similarRecommendations?.forEachIndexed { index, recommendation ->
                    item(key = "similar_to_title_$index") {
                        NavigationTitle(
                            label = stringResource(R.string.similar_to),
                            title = recommendation.title.title,
                            thumbnail = recommendation.title.thumbnailUrl?.let { thumbnailUrl ->
                                {
                                    val shape =
                                        if (recommendation.title is Artist) CircleShape else RoundedCornerShape(
                                            ThumbnailCornerRadius
                                        )
                                    AsyncImage(
                                        model = thumbnailUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(ListThumbnailSize)
                                            .clip(shape)
                                    )
                                }
                            },
                            onClick = {
                                when (recommendation.title) {
                                    is Song -> navController.navigate("album/${recommendation.title.album!!.id}")
                                    is Album -> navController.navigate("album/${recommendation.title.id}")
                                    is Artist -> navController.navigate("artist/${recommendation.title.id}")
                                    is Playlist -> {}
                                }
                            },
                            modifier = Modifier.animateItem()
                        )
                    }

                    item(key = "similar_to_list_$index") {
                        LazyRow(
                            contentPadding = WindowInsets.systemBars
                                .only(WindowInsetsSides.Horizontal)
                                .asPaddingValues(),
                            modifier = Modifier.animateItem()
                        ) {
                            items(recommendation.items) { item ->
                                ytGridItem(item)
                            }
                        }
                    }
                }
            }

            homePage?.sections?.forEachIndexed { index, section ->
                // Check if section contains songs for Play All functionality
                val sectionSongs = section.items.filterIsInstance<SongItem>()
                val hasPlayableSongs = sectionSongs.isNotEmpty()
                // Check if this section contains ONLY songs (like Quick picks, Trending songs)
                val isSongsOnlySection = section.items.isNotEmpty() && 
                    section.items.all { it is SongItem }

                item(key = "home_section_title_$index") {
                    NavigationTitle(
                        title = section.title,
                        label = section.label,
                        thumbnail = section.thumbnail?.let { thumbnailUrl ->
                            {
                                val shape =
                                    if (section.endpoint?.isArtistEndpoint == true) CircleShape else RoundedCornerShape(
                                        ThumbnailCornerRadius
                                    )
                                AsyncImage(
                                    model = thumbnailUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(ListThumbnailSize)
                                        .clip(shape)
                                )
                            }
                        },
                        onClick = section.endpoint?.let { endpoint ->
                            {
                                when {
                                    endpoint.browseId == "FEmusic_moods_and_genres" -> 
                                        navController.navigate("mood_and_genres")
                                    endpoint.params != null -> 
                                        navController.navigate("youtube_browse/${endpoint.browseId}?params=${endpoint.params}")
                                    else -> 
                                        navController.navigate("browse/${endpoint.browseId}")
                                }
                            }
                        },
                        onPlayAllClick = if (hasPlayableSongs) {
                            {
                                playerConnection.playQueue(
                                    ListQueue(
                                        title = section.title,
                                        items = sectionSongs.map { it.toMediaMetadata().toMediaItem() }
                                    )
                                )
                            }
                        } else null,
                        modifier = Modifier.animateItem()
                    )
                }

                if (isSongsOnlySection) {
                    // Render songs as a horizontal scrollable list (like Quick picks in YouTube Music)
                    item(key = "home_section_list_$index") {
                        LazyHorizontalGrid(
                            state = rememberLazyGridState(),
                            rows = GridCells.Fixed(4),
                            contentPadding = WindowInsets.systemBars
                                .only(WindowInsetsSides.Horizontal)
                                .asPaddingValues(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(ListItemHeight * 4)
                                .animateItem()
                        ) {
                            items(
                                items = sectionSongs.distinctBy { it.id },
                                key = { it.id }
                            ) { song ->
                                YouTubeListItem(
                                    item = song,
                                    isActive = song.id == mediaMetadata?.id,
                                    isPlaying = isPlaying,
                                    isSwipeable = false,
                                    trailingContent = {
                                        IconButton(
                                            onClick = {
                                                menuState.show {
                                                    YouTubeSongMenu(
                                                        song = song,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss
                                                    )
                                                }
                                            }
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.more_vert),
                                                contentDescription = null
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .width(horizontalLazyGridItemWidth)
                                        .combinedClickable(
                                            onClick = {
                                                if (song.id == mediaMetadata?.id) {
                                                    playerConnection.togglePlayPause()
                                                } else {
                                                    playerConnection.playQueue(
                                                        YouTubeQueue.radio(song.toMediaMetadata())
                                                    )
                                                }
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    YouTubeSongMenu(
                                                        song = song,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss
                                                    )
                                                }
                                            }
                                        )
                                )
                            }
                        }
                    }
                } else {
                    // Render mixed content as horizontal grid items (albums, playlists, artists, etc.)
                    item(key = "home_section_list_$index") {
                        LazyRow(
                            contentPadding = WindowInsets.systemBars
                                .only(WindowInsetsSides.Horizontal)
                                .asPaddingValues(),
                            modifier = Modifier.animateItem()
                        ) {
                            items(section.items) { item ->
                                ytGridItem(item)
                            }
                        }
                    }
                }
            }

            if (isLoading || homePage?.continuation != null && homePage?.sections?.isNotEmpty() == true) {
                item(key = "loading_shimmer") {
                    ShimmerHost(
                        modifier = Modifier.animateItem()
                    ) {
                        TextPlaceholder(
                            height = 36.dp,
                            modifier = Modifier
                                .padding(12.dp)
                                .width(250.dp),
                        )
                        LazyRow(
                            contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(),
                        ) {
                            items(4) {
                                GridItemPlaceHolder()
                            }
                        }
                    }
                }
            }

            if (selectedChip == null) {
                explorePage?.moodAndGenres?.let { moodAndGenres ->
                    item(key = "mood_and_genres_title") {
                        NavigationTitle(
                            title = stringResource(R.string.mood_and_genres),
                            onClick = {
                                navController.navigate("mood_and_genres")
                            },
                            modifier = Modifier.animateItem()
                        )
                    }
                    item(key = "mood_and_genres_list") {
                        LazyHorizontalGrid(
                            rows = GridCells.Fixed(4),
                            contentPadding = PaddingValues(6.dp),
                            modifier = Modifier
                                .height((MoodAndGenresButtonHeight + 12.dp) * 4 + 12.dp)
                                .animateItem()
                        ) {
                            items(moodAndGenres) {
                                MoodAndGenresButton(
                                    title = it.title,
                                    onClick = {
                                        navController.navigate("youtube_browse/${it.endpoint.browseId}?params=${it.endpoint.params}")
                                    },
                                    modifier = Modifier
                                        .padding(6.dp)
                                        .width(180.dp)
                                )
                            }
                        }
                    }
                }

                if (isMoodAndGenresLoading) {
                    item(key = "mood_and_genres_shimmer") {
                        ShimmerHost(
                            modifier = Modifier.animateItem()
                        ) {
                            TextPlaceholder(
                                height = 36.dp,
                                modifier = Modifier
                                    .padding(vertical = 12.dp, horizontal = 12.dp)
                                    .width(250.dp),
                            )

                            repeat(4) {
                                Row {
                                    repeat(2) {
                                        TextPlaceholder(
                                            height = MoodAndGenresButtonHeight,
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier
                                                .padding(horizontal = 12.dp)
                                                .width(200.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    }
}
}
