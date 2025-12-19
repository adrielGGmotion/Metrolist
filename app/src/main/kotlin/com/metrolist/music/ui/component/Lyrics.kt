package com.metrolist.music.ui.component

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.palette.graphics.Palette
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.DarkModeKey
import com.metrolist.music.constants.LyricsClickKey
import com.metrolist.music.constants.LyricsRomanizeBelarusianKey
import com.metrolist.music.constants.LyricsRomanizeBulgarianKey
import com.metrolist.music.constants.LyricsRomanizeCyrillicByLineKey
import com.metrolist.music.constants.LyricsRomanizeChineseKey
import com.metrolist.music.constants.LyricsRomanizeJapaneseKey
import com.metrolist.music.constants.LyricsRomanizeKoreanKey
import com.metrolist.music.constants.LyricsRomanizeKyrgyzKey
import com.metrolist.music.constants.LyricsRomanizeRussianKey
import com.metrolist.music.constants.LyricsRomanizeSerbianKey
import com.metrolist.music.constants.LyricsRomanizeUkrainianKey
import com.metrolist.music.constants.LyricsRomanizeMacedonianKey
import com.metrolist.music.constants.LyricsScrollKey
import com.metrolist.music.constants.LyricsTextPositionKey
import com.metrolist.music.constants.PlayerBackgroundStyle
import com.metrolist.music.constants.PlayerBackgroundStyleKey
import com.metrolist.music.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.metrolist.music.lyrics.AppleMusicLyricsLine
import com.metrolist.music.lyrics.AppleMusicWord
import com.metrolist.music.lyrics.LyricsEntry
import com.metrolist.music.lyrics.LyricsUtils
import com.metrolist.music.lyrics.LyricsUtils.findCurrentLineIndex
import com.metrolist.music.lyrics.LyricsUtils.isBelarusian
import com.metrolist.music.lyrics.LyricsUtils.isChinese
import com.metrolist.music.lyrics.LyricsUtils.isJapanese
import com.metrolist.music.lyrics.LyricsUtils.isKorean
import com.metrolist.music.lyrics.LyricsUtils.isKyrgyz
import com.metrolist.music.lyrics.LyricsUtils.isRussian
import com.metrolist.music.lyrics.LyricsUtils.isSerbian
import com.metrolist.music.lyrics.LyricsUtils.isBulgarian
import com.metrolist.music.lyrics.LyricsUtils.isUkrainian
import com.metrolist.music.lyrics.LyricsUtils.isMacedonian
import com.metrolist.music.lyrics.LyricsUtils.parseLyrics
import com.metrolist.music.lyrics.LyricsUtils.romanizeCyrillic
import com.metrolist.music.lyrics.LyricsUtils.romanizeJapanese
import com.metrolist.music.lyrics.LyricsUtils.romanizeKorean
import com.metrolist.music.lyrics.LyricsUtils.romanizeChinese
import com.metrolist.music.ui.component.shimmer.ShimmerHost
import com.metrolist.music.ui.component.shimmer.TextPlaceholder
import com.metrolist.music.ui.screens.settings.DarkMode
import com.metrolist.music.ui.screens.settings.LyricsPosition
import com.metrolist.music.ui.utils.fadingEdge
import com.metrolist.music.utils.ComposeToImage
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow

sealed class ParsedLyrics {
    data class Standard(val lines: List<LyricsEntry>) : ParsedLyrics()
    data class AppleMusic(val lines: List<AppleMusicLyricsLine>) : ParsedLyrics()
}

@Composable
fun SyncedLyricWord(
    word: AppleMusicWord,
    position: Long,
    style: TextStyle,
    inactiveColor: Color,
    activeColor: Color
) {
    val textMeasurer = rememberTextMeasurer()
    val textLayoutResult = remember(word.word, style) {
        textMeasurer.measure(word.word, style)
    }
    val targetProgress = ((position - word.startTime).toFloat() / (word.endTime - word.startTime)).coerceIn(0f, 1f)
    val progress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = if (targetProgress > 0f && targetProgress < 1f) 80 else 0)
    )

    Text(
        text = word.word,
        style = style.copy(
            brush = Brush.linearGradient(
                colorStops = arrayOf(
                    0.0f to activeColor,
                    progress to activeColor,
                    progress to inactiveColor,
                    1.0f to inactiveColor
                ),
                start = Offset.Zero,
                end = Offset(x = textLayoutResult.size.width.toFloat(), y = 0f)
            )
        )
    )
}

@RequiresApi(Build.VERSION_CODES.M)
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@SuppressLint("UnusedBoxWithConstraintsScope", "StringFormatInvalid")
@Composable
fun Lyrics(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier,
    showLyrics: Boolean
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val menuState = LocalMenuState.current
    val density = LocalDensity.current
    val context = LocalContext.current
    val configuration = LocalConfiguration.current // Get configuration

    val landscapeOffset =
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val lyricsTextPosition by rememberEnumPreference(LyricsTextPositionKey, LyricsPosition.CENTER)
    val changeLyrics by rememberPreference(LyricsClickKey, true)
    val scrollLyrics by rememberPreference(LyricsScrollKey, true)
    val romanizeJapaneseLyrics by rememberPreference(LyricsRomanizeJapaneseKey, true)
    val romanizeKoreanLyrics by rememberPreference(LyricsRomanizeKoreanKey, true)
    val romanizeRussianLyrics by rememberPreference(LyricsRomanizeRussianKey, true)
    val romanizeUkrainianLyrics by rememberPreference(LyricsRomanizeUkrainianKey, true)
    val romanizeSerbianLyrics by rememberPreference(LyricsRomanizeSerbianKey, true)
    val romanizeBulgarianLyrics by rememberPreference(LyricsRomanizeBulgarianKey, true)
    val romanizeBelarusianLyrics by rememberPreference(LyricsRomanizeBelarusianKey, true)
    val romanizeKyrgyzLyrics by rememberPreference(LyricsRomanizeKyrgyzKey, true)
    val romanizeMacedonianLyrics by rememberPreference(LyricsRomanizeMacedonianKey, true)
    val romanizeCyrillicByLine by rememberPreference(LyricsRomanizeCyrillicByLineKey, false)
    val romanizeChineseLyrics by rememberPreference(LyricsRomanizeChineseKey, true)
    val scope = rememberCoroutineScope()

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val lyricsEntity by playerConnection.currentLyrics.collectAsState(initial = null)
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val lyrics = remember(lyricsEntity) { lyricsEntity?.lyrics?.trim() }

    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.DEFAULT
    )

    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
        if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
    }

    val parsedLyrics = remember(lyrics, scope) {
        if (lyrics == null || lyrics == LYRICS_NOT_FOUND) {
            null
        } else {
            val appleMusicLyrics = LyricsUtils.parseAppleMusicLyrics(lyrics)
            if (appleMusicLyrics.isNotEmpty()) {
                ParsedLyrics.AppleMusic(appleMusicLyrics)
            } else if (lyrics.startsWith("[")) {
                val parsedLines = parseLyrics(lyrics)
                val isRussianLyrics = romanizeRussianLyrics && !romanizeCyrillicByLine && isRussian(lyrics)
                val isUkrainianLyrics = romanizeUkrainianLyrics && !romanizeCyrillicByLine && isUkrainian(lyrics)
                val isSerbianLyrics = romanizeSerbianLyrics && !romanizeCyrillicByLine && isSerbian(lyrics)
                val isBulgarianLyrics = romanizeBulgarianLyrics && !romanizeCyrillicByLine && isBulgarian(lyrics)
                val isBelarusianLyrics = romanizeBelarusianLyrics && !romanizeCyrillicByLine && isBelarusian(lyrics)
                val isKyrgyzLyrics = romanizeKyrgyzLyrics && !romanizeCyrillicByLine && isKyrgyz(lyrics)
                val isMacedonianLyrics = romanizeMacedonianLyrics && !romanizeCyrillicByLine && isMacedonian(lyrics)
                ParsedLyrics.Standard(
                    parsedLines.map { entry ->
                        val newEntry = LyricsEntry(entry.time, entry.text)
                        if (romanizeJapaneseLyrics && isJapanese(entry.text) && !isChinese(entry.text)) {
                            scope.launch { newEntry.romanizedTextFlow.value = romanizeJapanese(entry.text) }
                        }
                        if (romanizeKoreanLyrics && isKorean(entry.text)) {
                            scope.launch { newEntry.romanizedTextFlow.value = romanizeKorean(entry.text) }
                        }
                        if (romanizeRussianLyrics && (if (romanizeCyrillicByLine) isRussian(entry.text) else isRussianLyrics)) {
                            scope.launch { newEntry.romanizedTextFlow.value = romanizeCyrillic(entry.text) }
                        } else if (romanizeUkrainianLyrics && (if (romanizeCyrillicByLine) isUkrainian(entry.text) else isUkrainianLyrics)) {
                            scope.launch { newEntry.romanizedTextFlow.value = romanizeCyrillic(entry.text) }
                        } else if (romanizeSerbianLyrics && (if (romanizeCyrillicByLine) isSerbian(entry.text) else isSerbianLyrics)) {
                            scope.launch { newEntry.romanizedTextFlow.value = romanizeCyrillic(entry.text) }
                        } else if (romanizeBulgarianLyrics && (if (romanizeCyrillicByLine) isBulgarian(entry.text) else isBulgarianLyrics)) {
                            scope.launch { newEntry.romanizedTextFlow.value = romanizeCyrillic(entry.text) }
                        } else if (romanizeBelarusianLyrics && (if (romanizeCyrillicByLine) isBelarusian(entry.text) else isBelarusianLyrics)) {
                            scope.launch { newEntry.romanizedTextFlow.value = romanizeCyrillic(entry.text) }
                        } else if (romanizeKyrgyzLyrics && (if (romanizeCyrillicByLine) isKyrgyz(entry.text) else isKyrgyzLyrics)) {
                            scope.launch { newEntry.romanizedTextFlow.value = romanizeCyrillic(entry.text) }
                        } else if (romanizeMacedonianLyrics && (if (romanizeCyrillicByLine) isMacedonian(entry.text) else isMacedonianLyrics)) {
                            scope.launch { newEntry.romanizedTextFlow.value = romanizeCyrillic(entry.text) }
                        } else if (romanizeChineseLyrics && isChinese(entry.text)) {
                            scope.launch { newEntry.romanizedTextFlow.value = romanizeChinese(entry.text) }
                        }
                        newEntry
                    }.let { listOf(LyricsEntry.HEAD_LYRICS_ENTRY) + it }
                )
            } else {
                val isRussianLyrics = romanizeRussianLyrics && !romanizeCyrillicByLine && isRussian(lyrics)
                val isUkrainianLyrics = romanizeUkrainianLyrics && !romanizeCyrillicByLine && isUkrainian(lyrics)
                val isSerbianLyrics = romanizeSerbianLyrics && !romanizeCyrillicByLine && isSerbian(lyrics)
                val isBulgarianLyrics = romanizeBulgarianLyrics && !romanizeCyrillicByLine && isBulgarian(lyrics)
                val isBelarusianLyrics = romanizeBelarusianLyrics && !romanizeCyrillicByLine && isBelarusian(lyrics)
                val isKyrgyzLyrics = romanizeKyrgyzLyrics && !romanizeCyrillicByLine && isKyrgyz(lyrics)
                val isMacedonianLyrics = romanizeMacedonianLyrics && !romanizeCyrillicByLine && isMacedonian(lyrics)
                ParsedLyrics.Standard(
                    lyrics.lines().mapIndexed { index, line ->
                        val newEntry = LyricsEntry(index * 100L, line)
                        if (romanizeJapaneseLyrics && isJapanese(line) && !isChinese(line)) {
                            scope.launch { newEntry.romanizedTextFlow.value = romanizeJapanese(line) }
                        }
                        if (romanizeKoreanLyrics && isKorean(line)) {
                            scope.launch { newEntry.romanizedTextFlow.value = romanizeKorean(line) }
                        }
                        if (romanizeRussianLyrics && (if (romanizeCyrillicByLine) isRussian(line) else isRussianLyrics)) {
                            scope.launch { newEntry.romanizedTextFlow.value = romanizeCyrillic(line) }
                        } else if (romanizeUkrainianLyrics && (if (romanizeCyrillicByLine) isUkrainian(line) else isUkrainianLyrics)) {
                            scope.launch { newEntry.romanizedTextFlow.value = romanizeCyrillic(line) }
                        } else if (romanizeSerbianLyrics && (if (romanizeCyrillicByLine) isSerbian(line) else isSerbianLyrics)) {
                            scope.launch { newEntry.romanizedTextFlow.value = romanizeCyrillic(line) }
                        } else if (romanizeBulgarianLyrics && (if (romanizeCyrillicByLine) isBulgarian(line) else isBulgarianLyrics)) {
                            scope.launch { newEntry.romanizedTextFlow.value = romanizeCyrillic(line) }
                        } else if (romanizeBelarusianLyrics && (if (romanizeCyrillicByLine) isBelarusian(line) else isBelarusianLyrics)) {
                            scope.launch { newEntry.romanizedTextFlow.value = romanizeCyrillic(line) }
                        } else if (romanizeKyrgyzLyrics && (if (romanizeCyrillicByLine) isKyrgyz(line) else isKyrgyzLyrics)) {
                            scope.launch { newEntry.romanizedTextFlow.value = romanizeCyrillic(line) }
                        } else if (romanizeMacedonianLyrics && (if (romanizeCyrillicByLine) isMacedonian(line) else isMacedonianLyrics)) {
                            scope.launch { newEntry.romanizedTextFlow.value = romanizeCyrillic(line) }
                        } else if (romanizeChineseLyrics && isChinese(line)) {
                            scope.launch { newEntry.romanizedTextFlow.value = romanizeChinese(line) }
                        }
                        newEntry
                    }
                )
            }
        }
    }

    val isSynced = parsedLyrics is ParsedLyrics.Standard || parsedLyrics is ParsedLyrics.AppleMusic

    val textColor = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.secondary
        else -> if (useDarkTheme) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary
    }

    var currentLineIndex by remember { mutableIntStateOf(-1) }
    val activeLineIndices = remember { mutableStateListOf<Int>() }
    var deferredCurrentLineIndex by rememberSaveable { mutableIntStateOf(0) }
    var previousLineIndex by rememberSaveable { mutableIntStateOf(0) }
    var lastPreviewTime by rememberSaveable { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var initialScrollDone by rememberSaveable { mutableStateOf(false) }
    var shouldScrollToFirstLine by rememberSaveable { mutableStateOf(true) }
    var isAppMinimized by rememberSaveable { mutableStateOf(false) }
    var showProgressDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var shareDialogData by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    var showColorPickerDialog by remember { mutableStateOf(false) }
    var previewBackgroundColor by remember { mutableStateOf(Color(0xFF242424)) }
    var previewTextColor by remember { mutableStateOf(Color.White) }
    var previewSecondaryTextColor by remember { mutableStateOf(Color.White.copy(alpha = 0.7f)) }
    var isSelectionModeActive by rememberSaveable { mutableStateOf(false) }
    val selectedIndices = remember { mutableStateListOf<Int>() }
    var showMaxSelectionToast by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()
    var isAnimating by remember { mutableStateOf(false) }
    var isAutoScrollEnabled by rememberSaveable { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }

    BackHandler(enabled = isSelectionModeActive) {
        isSelectionModeActive = false
        selectedIndices.clear()
    }

    val maxSelectionLimit = 5

    LaunchedEffect(showMaxSelectionToast) {
        if (showMaxSelectionToast) {
            Toast.makeText(context, context.getString(R.string.max_selection_limit, maxSelectionLimit), Toast.LENGTH_SHORT).show()
            showMaxSelectionToast = false
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                val visibleItemsInfo = lazyListState.layoutInfo.visibleItemsInfo
                val isCurrentLineVisible = visibleItemsInfo.any { it.index == currentLineIndex }
                if (isCurrentLineVisible) {
                    initialScrollDone = false
                }
                isAppMinimized = true
            } else if (event == Lifecycle.Event.ON_START) {
                isAppMinimized = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(parsedLyrics) {
        isSelectionModeActive = false
        selectedIndices.clear()
    }

    LaunchedEffect(lyrics) {
        if (lyrics.isNullOrEmpty() || !lyrics.startsWith("[")) {
            currentLineIndex = -1
            activeLineIndices.clear()
        } else {
            while (isActive) {
                delay(50)
                val sliderPosition = sliderPositionProvider()
                isSeeking = sliderPosition != null
                val position = sliderPosition ?: playerConnection.player.currentPosition
                currentPosition = position
                when (parsedLyrics) {
                    is ParsedLyrics.Standard -> {
                        currentLineIndex = findCurrentLineIndex(parsedLyrics.lines, position)
                        activeLineIndices.clear()
                        if (currentLineIndex != -1) {
                            activeLineIndices.add(currentLineIndex)
                        }
                    }
                    is ParsedLyrics.AppleMusic -> {
                        val newActiveIndices = parsedLyrics.lines.mapIndexedNotNull { index, line ->
                            val lineEndTime = line.words.lastOrNull()?.endTime ?: (line.time + 1)
                            if (position in line.time..lineEndTime) index else null
                        }
                        if (activeLineIndices != newActiveIndices) {
                            activeLineIndices.clear()
                            activeLineIndices.addAll(newActiveIndices)
                        }
                        currentLineIndex = newActiveIndices.lastOrNull() ?: parsedLyrics.lines.indexOfLast { it.time <= position }.coerceAtLeast(0)
                    }
                    else -> {
                        currentLineIndex = -1
                        activeLineIndices.clear()
                    }
                }
            }
        }
    }

    LaunchedEffect(isSeeking, lastPreviewTime) {
        if (isSeeking) {
            lastPreviewTime = 0L
        } else if (lastPreviewTime != 0L) {
            delay(LyricsPreviewTime)
            lastPreviewTime = 0L
        }
    }

    suspend fun performSmoothPageScroll(targetIndex: Int, duration: Int = 1500) {
        if (isAnimating) return
        isAnimating = true
        try {
            val itemInfo = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetIndex }
            if (itemInfo != null) {
                val viewportHeight = lazyListState.layoutInfo.viewportEndOffset - lazyListState.layoutInfo.viewportStartOffset
                val center = lazyListState.layoutInfo.viewportStartOffset + (viewportHeight / 2)
                val itemCenter = itemInfo.offset + itemInfo.size / 2
                val offset = itemCenter - center
                if (kotlin.math.abs(offset) > 10) {
                    lazyListState.animateScrollBy(value = offset.toFloat(), animationSpec = tween(durationMillis = duration))
                }
            } else {
                lazyListState.scrollToItem(targetIndex)
            }
        } finally {
            isAnimating = false
        }
    }

    LaunchedEffect(currentLineIndex, lastPreviewTime, initialScrollDone, isAutoScrollEnabled) {
        if (!isSynced) return@LaunchedEffect
        if (isAutoScrollEnabled) {
            if ((currentLineIndex == 0 && shouldScrollToFirstLine) || !initialScrollDone) {
                shouldScrollToFirstLine = false
                val initialCenterIndex = kotlin.math.max(0, currentLineIndex)
                performSmoothPageScroll(initialCenterIndex, 800)
                if (!isAppMinimized) {
                    initialScrollDone = true
                }
            } else if (currentLineIndex != -1) {
                deferredCurrentLineIndex = currentLineIndex
                if (isSeeking) {
                    val seekCenterIndex = kotlin.math.max(0, currentLineIndex - 1)
                    performSmoothPageScroll(seekCenterIndex, 500)
                } else if ((lastPreviewTime == 0L || currentLineIndex != previousLineIndex) && scrollLyrics) {
                    if (currentLineIndex != previousLineIndex) {
                        val delayNeeded = when (val p = parsedLyrics) {
                            is ParsedLyrics.AppleMusic -> {
                                val prevLine = p.lines.getOrNull(currentLineIndex - 1)
                                val lastWordEndTime = prevLine?.words?.lastOrNull()?.endTime
                                if (lastWordEndTime != null) {
                                    val position = sliderPositionProvider() ?: playerConnection.player.currentPosition
                                    (lastWordEndTime - position).coerceAtLeast(0L)
                                } else {
                                    0L
                                }
                            }
                            else -> 0L
                        }
                        if (delayNeeded > 0) {
                            delay(delayNeeded)
                        }
                        if (isActive) {
                            val centerTargetIndex = currentLineIndex
                            performSmoothPageScroll(centerTargetIndex, 1500)
                        }
                    }
                }
            }
        }
        if (currentLineIndex > 0) {
            shouldScrollToFirstLine = true
        }
        previousLineIndex = currentLineIndex
    }

    BoxWithConstraints(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier.fillMaxSize().padding(bottom = 12.dp)
    ) {
        if (lyrics == LYRICS_NOT_FOUND) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.lyrics_not_found),
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.alpha(0.5f)
                )
            }
        } else {
            LazyColumn(
                state = lazyListState,
                contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Top).add(WindowInsets(top = maxHeight / 3, bottom = maxHeight / 2)).asPaddingValues(),
                modifier = Modifier
                    .fadingEdge(vertical = 64.dp)
                    .nestedScroll(remember {
                        object : NestedScrollConnection {
                            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                                if (source == NestedScrollSource.UserInput) {
                                    isAutoScrollEnabled = false
                                }
                                if (!isSelectionModeActive) {
                                    lastPreviewTime = System.currentTimeMillis()
                                }
                                return super.onPostScroll(consumed, available, source)
                            }
                            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                                isAutoScrollEnabled = false
                                if (!isSelectionModeActive) {
                                    lastPreviewTime = System.currentTimeMillis()
                                }
                                return super.onPostFling(consumed, available)
                            }
                        }
                    })
            ) {
                val displayedCurrentLineIndex = if (!isAutoScrollEnabled) currentLineIndex else if (isSeeking || isSelectionModeActive) deferredCurrentLineIndex else currentLineIndex

                when (parsedLyrics) {
                    null -> {
                        item {
                            ShimmerHost {
                                repeat(10) {
                                    Box(
                                        contentAlignment = when (lyricsTextPosition) {
                                            LyricsPosition.LEFT -> Alignment.CenterStart
                                            LyricsPosition.CENTER -> Alignment.Center
                                            LyricsPosition.RIGHT -> Alignment.CenterEnd
                                        },
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp)
                                    ) {
                                        TextPlaceholder()
                                    }
                                }
                            }
                        }
                    }
                    is ParsedLyrics.Standard -> {
                        itemsIndexed(items = parsedLyrics.lines, key = { index, item -> "$index-${item.time}" }) { index, item ->
                            val isSelected = selectedIndices.contains(index)
                            val itemModifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .combinedClickable(
                                    enabled = true,
                                    onClick = {
                                        if (isSelectionModeActive) {
                                            if (isSelected) {
                                                selectedIndices.remove(index)
                                                if (selectedIndices.isEmpty()) {
                                                    isSelectionModeActive = false
                                                }
                                            } else {
                                                if (selectedIndices.size < maxSelectionLimit) {
                                                    selectedIndices.add(index)
                                                } else {
                                                    showMaxSelectionToast = true
                                                }
                                            }
                                        } else if (isSynced && changeLyrics) {
                                            playerConnection.player.seekTo(item.time)
                                            scope.launch {
                                                lazyListState.scrollToItem(index = index)
                                                val itemInfo = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
                                                if (itemInfo != null) {
                                                    val viewportHeight = lazyListState.layoutInfo.viewportEndOffset - lazyListState.layoutInfo.viewportStartOffset
                                                    val center = lazyListState.layoutInfo.viewportStartOffset + (viewportHeight / 2)
                                                    val itemCenter = itemInfo.offset + itemInfo.size / 2
                                                    val offset = itemCenter - center
                                                    if (kotlin.math.abs(offset) > 10) {
                                                        lazyListState.animateScrollBy(value = offset.toFloat(), animationSpec = tween(durationMillis = 1500))
                                                    }
                                                }
                                            }
                                            lastPreviewTime = 0L
                                        }
                                    },
                                    onLongClick = {
                                        if (!isSelectionModeActive) {
                                            isSelectionModeActive = true
                                            selectedIndices.add(index)
                                        } else if (!isSelected && selectedIndices.size < maxSelectionLimit) {
                                            selectedIndices.add(index)
                                        } else if (!isSelected) {
                                            showMaxSelectionToast = true
                                        }
                                    }
                                )
                                .background(if (isSelected && isSelectionModeActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent)
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                            val isActivelySinging = activeLineIndices.contains(index)
                            val alpha by animateFloatAsState(targetValue = if (!isSynced || (isSelectionModeActive && isSelected)) 1f else if (isActivelySinging) 1f else 0.3f, animationSpec = tween(durationMillis = 400))
                            val scale by animateFloatAsState(targetValue = if (isActivelySinging) 1.05f else 1f, animationSpec = tween(durationMillis = 400))
                            Column(
                                modifier = itemModifier.graphicsLayer {
                                    this.alpha = alpha
                                    this.scaleX = scale
                                    this.scaleY = scale
                                },
                                horizontalAlignment = when (lyricsTextPosition) {
                                    LyricsPosition.LEFT -> Alignment.Start
                                    LyricsPosition.CENTER -> Alignment.CenterHorizontally
                                    LyricsPosition.RIGHT -> Alignment.End
                                }
                            ) {
                                Text(
                                    text = item.text,
                                    fontSize = 24.sp,
                                    color = if (isActivelySinging && isSynced) textColor else textColor.copy(alpha = 0.5f),
                                    textAlign = when (lyricsTextPosition) {
                                        LyricsPosition.LEFT -> TextAlign.Left
                                        LyricsPosition.CENTER -> TextAlign.Center
                                        LyricsPosition.RIGHT -> TextAlign.Right
                                    },
                                    fontWeight = if (isActivelySinging && isSynced) FontWeight.ExtraBold else FontWeight.Bold
                                )
                                if (currentSong?.romanizeLyrics == true && (romanizeJapaneseLyrics || romanizeKoreanLyrics || romanizeRussianLyrics || romanizeUkrainianLyrics || romanizeSerbianLyrics || romanizeBulgarianLyrics || romanizeBelarusianLyrics || romanizeKyrgyzLyrics || romanizeMacedonianLyrics || romanizeChineseLyrics)) {
                                    val romanizedText by item.romanizedTextFlow.collectAsState()
                                    romanizedText?.let { romanized ->
                                        Text(
                                            text = romanized,
                                            fontSize = 18.sp,
                                            color = textColor.copy(alpha = 0.5f),
                                            textAlign = when (lyricsTextPosition) {
                                                LyricsPosition.LEFT -> TextAlign.Left
                                                LyricsPosition.CENTER -> TextAlign.Center
                                                LyricsPosition.RIGHT -> TextAlign.Right
                                            },
                                            fontWeight = FontWeight.Normal,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    is ParsedLyrics.AppleMusic -> {
                        itemsIndexed(items = parsedLyrics.lines, key = { index, item -> "$index-${item.time}" }) { index, item ->
                            val isSelected = selectedIndices.contains(index)
                            val itemModifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .combinedClickable(
                                    enabled = true,
                                    onClick = {
                                        if (isSelectionModeActive) {
                                            if (isSelected) {
                                                selectedIndices.remove(index)
                                                if (selectedIndices.isEmpty()) {
                                                    isSelectionModeActive = false
                                                }
                                            } else {
                                                if (selectedIndices.size < maxSelectionLimit) {
                                                    selectedIndices.add(index)
                                                } else {
                                                    showMaxSelectionToast = true
                                                }
                                            }
                                        } else if (isSynced && changeLyrics) {
                                            playerConnection.player.seekTo(item.time)
                                            scope.launch {
                                                lazyListState.scrollToItem(index = index)
                                                val itemInfo = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
                                                if (itemInfo != null) {
                                                    val viewportHeight = lazyListState.layoutInfo.viewportEndOffset - lazyListState.layoutInfo.viewportStartOffset
                                                    val center = lazyListState.layoutInfo.viewportStartOffset + (viewportHeight / 2)
                                                    val itemCenter = itemInfo.offset + itemInfo.size / 2
                                                    val offset = itemCenter - center
                                                    if (kotlin.math.abs(offset) > 10) {
                                                        lazyListState.animateScrollBy(value = offset.toFloat(), animationSpec = tween(durationMillis = 1500))
                                                    }
                                                }
                                            }
                                            lastPreviewTime = 0L
                                        }
                                    },
                                    onLongClick = {
                                        if (!isSelectionModeActive) {
                                            isSelectionModeActive = true
                                            selectedIndices.add(index)
                                        } else if (!isSelected && selectedIndices.size < maxSelectionLimit) {
                                            selectedIndices.add(index)
                                        } else if (!isSelected) {
                                            showMaxSelectionToast = true
                                        }
                                    }
                                )
                                .background(if (isSelected && isSelectionModeActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent)
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                            val isActivelySinging = activeLineIndices.contains(index)
                            val isBackground = item.speaker == "bg"
                            val alpha by animateFloatAsState(
                                targetValue = if (!isSynced || (isSelectionModeActive && isSelected)) 1f else if (isActivelySinging) 1f else if (isBackground) 0.2f else 0.3f,
                                animationSpec = tween(durationMillis = 400)
                            )
                            val scale by animateFloatAsState(
                                targetValue = if (isActivelySinging) (if (isBackground) 1.02f else 1.05f) else 1f,
                                animationSpec = tween(durationMillis = 400)
                            )
                            val horizontalAlignment = when (item.speaker) {
                                "v1" -> Alignment.End
                                "v2" -> Alignment.Start
                                else -> when (lyricsTextPosition) {
                                    LyricsPosition.LEFT -> Alignment.Start
                                    LyricsPosition.CENTER -> Alignment.CenterHorizontally
                                    LyricsPosition.RIGHT -> Alignment.End
                                }
                            }
                            Column(
                                modifier = itemModifier.graphicsLayer {
                                    this.alpha = alpha
                                    this.scaleX = scale
                                    this.scaleY = scale
                                },
                                horizontalAlignment = horizontalAlignment
                            ) {
                                FlowRow {
                                    item.words.forEach { word ->
                                        SyncedLyricWord(
                                            word = word,
                                            position = currentPosition,
                                            style = TextStyle(
                                                fontSize = if (isBackground) 18.sp else 24.sp,
                                                fontWeight = if (isActivelySinging && isSynced) FontWeight.ExtraBold else FontWeight.Bold,
                                                textAlign = when (lyricsTextPosition) {
                                                    LyricsPosition.LEFT -> TextAlign.Left
                                                    LyricsPosition.CENTER -> TextAlign.Center
                                                    LyricsPosition.RIGHT -> TextAlign.Right
                                                },
                                                fontStyle = if (isBackground) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal
                                            ),
                                            inactiveColor = textColor.copy(alpha = if (isBackground) 0.4f else 0.5f),
                                            activeColor = textColor
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
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

private const val METROLIST_AUTO_SCROLL_DURATION = 1500L
private const val METROLIST_INITIAL_SCROLL_DURATION = 1000L
private const val METROLIST_SEEK_DURATION = 800L
private const val METROLIST_FAST_SEEK_DURATION = 600L
val LyricsPreviewTime = 2.seconds
