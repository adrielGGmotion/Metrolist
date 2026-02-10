package com.metrolist.music.services

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.media3.common.Player
import androidx.palette.graphics.Palette
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.MainActivity
import com.metrolist.music.R
import com.metrolist.music.constants.FloatingLyricsAutoFetchKey
import com.metrolist.music.constants.FloatingLyricsBackgroundStyle
import com.metrolist.music.constants.FloatingLyricsBackgroundStyleKey
import com.metrolist.music.constants.FloatingLyricsOpacityKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.LyricsEntity
import com.metrolist.music.lyrics.LyricsHelper
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.playback.MusicService
import com.metrolist.music.playback.PlayerConnection
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.Lyrics
import com.metrolist.music.ui.component.MenuState
import com.metrolist.music.ui.theme.MetrolistTheme
import com.metrolist.music.ui.theme.PlayerColorExtractor
import com.metrolist.music.ui.theme.extractThemeColor
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.roundToInt

@AndroidEntryPoint
class FloatingLyricsService : Service(), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner, OnBackPressedDispatcherOwner {

    @Inject
    lateinit var database: MusicDatabase

    @Inject
    lateinit var lyricsHelper: LyricsHelper

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()
    private val dispatcher = OnBackPressedDispatcher {
        hideOverlay()
    }

    override val lifecycle: Lifecycle = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore
        get() = store
    override val onBackPressedDispatcher: OnBackPressedDispatcher
        get() = dispatcher

    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null
    private var removeView: ComposeView? = null
    private var playerConnection: PlayerConnection? = null
    
    // Visibility state for window content animation
    private var isWindowVisible by mutableStateOf(false)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is MusicService.MusicBinder) {
                playerConnection = PlayerConnection(this@FloatingLyricsService, service, database, lifecycleScope)
                
                // Auto-close on Stop/Idle state
                lifecycleScope.launch {
                    playerConnection?.playbackState?.collect { state ->
                        if (state == Player.STATE_IDLE || state == Player.STATE_ENDED) {
                            hideOverlay()
                        }
                    }
                }

                if (shouldShow) {
                    showOverlay()
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            playerConnection?.dispose()
            playerConnection = null
            stopSelf()
        }
    }

    private var shouldShow = false

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val intent = Intent(this, MusicService::class.java)
        val bound = bindService(intent, serviceConnection, 0)
        if (!bound) {
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        if (intent?.action == ACTION_SHOW) {
            shouldShow = true
            if (playerConnection != null) {
                showOverlay()
            }
        } else if (intent?.action == ACTION_HIDE) {
            shouldShow = false
            hideOverlay()
        }
        return START_NOT_STICKY
    }

    private fun showOverlay() {
        if (overlayView != null) return
        if (!Settings.canDrawOverlays(this)) return

        val initialWidth = 800
        val initialHeight = 1000

        val layoutParams = WindowManager.LayoutParams(
            initialWidth,
            initialHeight,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        layoutParams.gravity = Gravity.TOP or Gravity.START
        layoutParams.x = 100
        layoutParams.y = 200

        // Create Remove View (Dismiss Target)
        val removeParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            200, // Height for the bottom area
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, // Passes touches, just for display
            PixelFormat.TRANSLUCENT
        )
        removeParams.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        removeParams.y = 0

        var isDragging by mutableStateOf(false)
        var isHoveringRemove by mutableStateOf(false)

        removeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingLyricsService)
            setViewTreeViewModelStoreOwner(this@FloatingLyricsService)
            setViewTreeSavedStateRegistryOwner(this@FloatingLyricsService)
            setContent {
                MetrolistTheme(darkTheme = true) {
                    AnimatedVisibility(
                        visible = isDragging,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 32.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            val scale by animateFloatAsState(if (isHoveringRemove) 1.5f else 1f, label = "scale")
                            val alpha by animateFloatAsState(if (isHoveringRemove) 1f else 0.7f, label = "alpha")
                            
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    painter = painterResource(R.drawable.close), // or delete icon
                                    contentDescription = "Remove",
                                    tint = Color.White, // Always white for visibility
                                    modifier = Modifier
                                        .size(48.dp)
                                        .scale(scale)
                                        .alpha(alpha)
                                        .background(
                                            if (isHoveringRemove) Color.Red.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.5f),
                                            CircleShape
                                        )
                                        .padding(12.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Dismiss",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.alpha(alpha)
                                )
                            }
                        }
                    }
                }
            }
        }

        overlayView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingLyricsService)
            setViewTreeViewModelStoreOwner(this@FloatingLyricsService)
            setViewTreeSavedStateRegistryOwner(this@FloatingLyricsService)
            setContent {
                val playerConnection = playerConnection ?: return@setContent
                val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
                
                var themeColor by remember { mutableStateOf<Color?>(null) }
                val context = LocalContext.current

                LaunchedEffect(mediaMetadata?.thumbnailUrl) {
                    val url = mediaMetadata?.thumbnailUrl
                    if (url != null) {
                        withContext(Dispatchers.IO) {
                            val loader = ImageLoader(context)
                            val request = ImageRequest.Builder(context)
                                .data(url)
                                .allowHardware(false)
                                .build()
                            val result = loader.execute(request)
                            val bitmap = result.image?.toBitmap()
                            if (bitmap != null) {
                                themeColor = bitmap.extractThemeColor()
                            }
                        }
                    } else {
                        themeColor = null
                    }
                }

                MetrolistTheme(
                    darkTheme = true, // Force dark theme for floating overlay (usually better)
                    themeColor = themeColor ?: com.metrolist.music.ui.theme.DefaultThemeColor,
                    pureBlack = false
                ) {
                    val menuState = remember { MenuState() }
                    
                    val density = LocalDensity.current

                    CompositionLocalProvider(
                        LocalPlayerConnection provides playerConnection,
                        LocalMenuState provides menuState,
                        LocalOnBackPressedDispatcherOwner provides this@FloatingLyricsService
                    ) {
                        val visible = isWindowVisible
                        
                        // Handle exit animation and removal
                        LaunchedEffect(visible) {
                            if (!visible) {
                                removeOverlayView()
                                stopSelf()
                            }
                        }
                        
                        if (visible) {
                            var isMinimized by remember { mutableStateOf(false) }
                            
                            FloatingLyricsContainer(
                                onClose = {
                                    hideOverlay()
                                },
                                onDrag = { x, y ->
                                    layoutParams.x += x.roundToInt()
                                    layoutParams.y += y.roundToInt()
                                    
                                    // Ensure window stays within screen bounds (roughly)
                                    val screenWidth = resources.displayMetrics.widthPixels
                                    val screenHeight = resources.displayMetrics.heightPixels
                                    
                                    if (layoutParams.x < 0) layoutParams.x = 0
                                    if (layoutParams.y < 0) layoutParams.y = 0
                                    if (layoutParams.x > screenWidth - layoutParams.width) layoutParams.x = screenWidth - layoutParams.width
                                    if (layoutParams.y > screenHeight - layoutParams.height) layoutParams.y = screenHeight - layoutParams.height

                                    windowManager.updateViewLayout(this@apply, layoutParams)
                                    
                                    // Check for remove intersection
                                    // Convert dp to px manually or using density
                                    val offsetPx = with(density) { 32.dp.toPx() }
                                    
                                    val centerX = layoutParams.x + (if (isMinimized) offsetPx else 400f) // Half width (approx)
                                    val centerY = layoutParams.y + (if (isMinimized) offsetPx else 500f) // Half height (approx)
                                    
                                    // Threshold for bottom center
                                    val removeThresholdY = screenHeight - 400 // Bottom area
                                    val removeThresholdX = screenWidth / 2
                                    
                                    isHoveringRemove = (centerY > removeThresholdY) && 
                                                       (centerX > removeThresholdX - 200 && centerX < removeThresholdX + 200)
                                },
                                onDragStart = {
                                    isDragging = true
                                },
                                onDragEnd = {
                                    isDragging = false
                                    if (isHoveringRemove) {
                                        hideOverlay()
                                    }
                                    isHoveringRemove = false
                                },
                                onResize = { widthDelta, heightDelta ->
                                    if (!isMinimized) {
                                        val newWidth = layoutParams.width + widthDelta.roundToInt()
                                        val newHeight = layoutParams.height + heightDelta.roundToInt()
                                        
                                        // Minimum size constraints
                                        val minSize = with(density) { 200.dp.toPx().roundToInt() }
                                        
                                        layoutParams.width = newWidth.coerceAtLeast(minSize)
                                        layoutParams.height = newHeight.coerceAtLeast(minSize)
                                        
                                        windowManager.updateViewLayout(this@apply, layoutParams)
                                    }
                                },
                                isMinimized = isMinimized,
                                onMinimizeToggle = {
                                    isMinimized = !isMinimized
                                    if (isMinimized) {
                                        layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT
                                        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
                                    } else {
                                        layoutParams.width = initialWidth
                                        layoutParams.height = initialHeight
                                    }
                                    windowManager.updateViewLayout(this@apply, layoutParams)
                                },
                                onOpenApp = {
                                    hideOverlay()
                                    // Start activity
                                    val intent = Intent(context, MainActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                    }
                                    context.startActivity(intent)
                                },
                                mediaMetadata = mediaMetadata,
                                onLyricsMissing = { song ->
                                    // Auto-fetch logic
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        try {
                                            val result = lyricsHelper.getLyrics(song)
                                            if (result.lyrics != LyricsEntity.LYRICS_NOT_FOUND) {
                                                database.upsert(
                                                    LyricsEntity(
                                                        id = song.id,
                                                        lyrics = result.lyrics,
                                                        provider = result.provider
                                                    )
                                                )
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        windowManager.addView(removeView, removeParams)
        windowManager.addView(overlayView, layoutParams)
        
        // Trigger entrance animation
        isWindowVisible = true
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    private fun hideOverlay() {
        isWindowVisible = false
    }

    private fun removeOverlayView() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        if (overlayView != null) {
            overlayView?.disposeComposition()
            windowManager.removeView(overlayView)
            overlayView = null
        }
        if (removeView != null) {
            removeView?.disposeComposition()
            windowManager.removeView(removeView)
            removeView = null
        }
    }

    override fun onDestroy() {
        removeOverlayView()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        if (playerConnection != null) {
            unbindService(serviceConnection)
            playerConnection?.dispose()
        }
        super.onDestroy()
    }

    companion object {
        const val ACTION_SHOW = "SHOW"
        const val ACTION_HIDE = "HIDE"
    }
}

@Composable
fun FloatingLyricsContainer(
    onClose: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onResize: (Float, Float) -> Unit,
    isMinimized: Boolean,
    onMinimizeToggle: () -> Unit,
    mediaMetadata: MediaMetadata?,
    onOpenApp: () -> Unit,
    onLyricsMissing: (MediaMetadata) -> Unit = {}
) {
    val (opacity) = rememberPreference(FloatingLyricsOpacityKey, 1f)

    AnimatedContent(
        targetState = isMinimized,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "FloatingLyricsState"
    ) { minimized ->
        if (minimized) {
            FloatingAppIcon(
                onDrag = onDrag,
                onDragStart = onDragStart,
                onDragEnd = onDragEnd,
                onMaximize = onMinimizeToggle,
                opacity = opacity
            )
        } else {
            FloatingLyricsCard(
                onClose = onClose,
                onDrag = onDrag,
                onDragStart = onDragStart,
                onDragEnd = onDragEnd,
                onResize = onResize,
                onMinimize = onMinimizeToggle,
                opacity = opacity,
                mediaMetadata = mediaMetadata,
                onOpenApp = onOpenApp,
                onLyricsMissing = onLyricsMissing
            )
        }
    }
}

@Composable
fun FloatingAppIcon(
    onDrag: (Float, Float) -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onMaximize: () -> Unit,
    opacity: Float
) {
    var isDragging by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.1f else 1f,
        animationSpec = tween(200)
    )

    Box(
        modifier = Modifier
            .size(64.dp)
            .scale(scale)
            .alpha(opacity)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { 
                        isDragging = true
                        onDragStart() 
                    },
                    onDragEnd = { 
                        isDragging = false
                        onDragEnd() 
                    },
                    onDragCancel = { 
                        isDragging = false
                        onDragEnd() 
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x, dragAmount.y)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        NoRippleIconButton(onClick = onMaximize) {
            Icon(
                painter = painterResource(R.drawable.lyrics), // Or app icon
                contentDescription = "Maximize",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun FloatingLyricsCard(
    onClose: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onResize: (Float, Float) -> Unit,
    onMinimize: () -> Unit,
    opacity: Float,
    mediaMetadata: MediaMetadata?,
    onOpenApp: () -> Unit,
    onLyricsMissing: (MediaMetadata) -> Unit = {}
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current
    val (backgroundStyle) = rememberEnumPreference(
        key = FloatingLyricsBackgroundStyleKey,
        defaultValue = FloatingLyricsBackgroundStyle.DEFAULT
    )
    val (autoFetchLyrics) = rememberPreference(
        key = FloatingLyricsAutoFetchKey,
        defaultValue = false
    )

    // Observe lyrics state to trigger auto-fetch
    val lyricsEntity by playerConnection.currentLyrics.collectAsState(initial = null)
    
    LaunchedEffect(mediaMetadata, lyricsEntity, autoFetchLyrics) {
        if (mediaMetadata != null && autoFetchLyrics) {
            val hasNoLyrics = lyricsEntity == null || lyricsEntity?.lyrics == LyricsEntity.LYRICS_NOT_FOUND
            if (hasNoLyrics) {
                onLyricsMissing(mediaMetadata)
            }
        }
    }

    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()

    LaunchedEffect(mediaMetadata?.id, backgroundStyle) {
        if (backgroundStyle == FloatingLyricsBackgroundStyle.GRADIENT) {
            if (mediaMetadata?.thumbnailUrl != null) {
                withContext(Dispatchers.IO) {
                    val loader = ImageLoader(context)
                    val request = ImageRequest.Builder(context)
                        .data(mediaMetadata.thumbnailUrl)
                        .size(100, 100)
                        .allowHardware(false)
                        .build()
                    val result = loader.execute(request)
                    val bitmap = result.image?.toBitmap()
                    if (bitmap != null) {
                        val palette = Palette.from(bitmap).generate()
                        gradientColors = PlayerColorExtractor.extractGradientColors(
                            palette = palette,
                            fallbackColor = fallbackColor
                        )
                    }
                }
            }
        } else {
            gradientColors = emptyList()
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (backgroundStyle == FloatingLyricsBackgroundStyle.DEFAULT)
                MaterialTheme.colorScheme.surfaceContainer.copy(alpha = opacity)
            else
                Color.Transparent // Handle custom backgrounds in box
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(Modifier.fillMaxSize()) {
            // Background Layer
            if (backgroundStyle != FloatingLyricsBackgroundStyle.DEFAULT) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(opacity)
                ) {
                    when (backgroundStyle) {
                        FloatingLyricsBackgroundStyle.BLUR -> {
                            if (mediaMetadata?.thumbnailUrl != null) {
                                AsyncImage(
                                    model = mediaMetadata.thumbnailUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .blur(80.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.3f))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surfaceContainer)
                                )
                            }
                        }
                        FloatingLyricsBackgroundStyle.GRADIENT -> {
                            if (gradientColors.isNotEmpty()) {
                                val gradientColorStops: Array<Pair<Float, Color>> = if (gradientColors.size >= 3) {
                                    arrayOf(
                                        0.0f to gradientColors[0],
                                        0.5f to gradientColors[1],
                                        1.0f to gradientColors[2]
                                    )
                                } else {
                                    arrayOf(
                                        0.0f to (gradientColors.firstOrNull() ?: MaterialTheme.colorScheme.surface),
                                        1.0f to Color.Black
                                    )
                                }
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(Brush.verticalGradient(*gradientColorStops))
                                        .background(Color.Black.copy(alpha = 0.3f))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surfaceContainer)
                                )
                            }
                        }
                        else -> {}
                    }
                }
            }

            Column(Modifier.fillMaxSize()) {
                // Header & Drag Handle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp) // Taller header for content
                        .background(
                            if (backgroundStyle == FloatingLyricsBackgroundStyle.DEFAULT)
                                MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
                            else
                                Color.Black.copy(alpha = 0.2f) // Subtle header for custom backgrounds
                        )
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { onDragStart() },
                                onDragEnd = { onDragEnd() },
                                onDragCancel = { onDragEnd() },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    onDrag(dragAmount.x, dragAmount.y)
                                }
                            )
                        }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Album Art
                    AsyncImage(
                        model = mediaMetadata?.thumbnailUrl,
                        contentDescription = "Album Art",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { // Click to open player
                                onOpenApp()
                            }
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // Title & Artist
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = mediaMetadata?.title ?: "Unknown Title",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (backgroundStyle == FloatingLyricsBackgroundStyle.DEFAULT) 
                                MaterialTheme.colorScheme.onSurface 
                            else Color.White
                        )
                        Text(
                            text = mediaMetadata?.artists?.joinToString { it.name } ?: "Unknown Artist",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (backgroundStyle == FloatingLyricsBackgroundStyle.DEFAULT)
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else Color.White.copy(alpha = 0.7f)
                        )
                    }

                    // Minimize Button
                    NoRippleIconButton(onClick = onMinimize) {
                        Icon(
                            painter = painterResource(R.drawable.expand_more), // Use expand_more as minimize
                            contentDescription = "Minimize",
                            tint = if (backgroundStyle == FloatingLyricsBackgroundStyle.DEFAULT)
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else Color.White
                        )
                    }

                    // Close Button
                    NoRippleIconButton(onClick = onClose) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = "Close",
                            tint = if (backgroundStyle == FloatingLyricsBackgroundStyle.DEFAULT)
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else Color.White
                        )
                    }
                }

                // Content
                Box(
                    Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    Lyrics(
                        sliderPositionProvider = { playerConnection.player.currentPosition },
                        showLyrics = true,
                        modifier = Modifier.padding(bottom = 24.dp),
                        isFloating = true
                    )
                }
            }

            // Resize handle bottom right
            Icon(
                painter = painterResource(R.drawable.drag_handle),
                contentDescription = "Resize",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onResize(dragAmount.x, dragAmount.y)
                        }
                    },
                tint = if (backgroundStyle == FloatingLyricsBackgroundStyle.DEFAULT)
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                else Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun NoRippleIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
