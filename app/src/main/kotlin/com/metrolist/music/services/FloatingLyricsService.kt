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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
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
import com.metrolist.music.R
import com.metrolist.music.constants.FloatingLyricsBackgroundStyle
import com.metrolist.music.constants.FloatingLyricsBackgroundStyleKey
import com.metrolist.music.constants.FloatingLyricsOpacityKey
import com.metrolist.music.db.MusicDatabase
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
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.roundToInt

@AndroidEntryPoint
class FloatingLyricsService : Service(), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner, OnBackPressedDispatcherOwner {

    @Inject
    lateinit var database: MusicDatabase

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()
    private val dispatcher = OnBackPressedDispatcher {
        hideOverlay()
        stopSelf()
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
    private var playerConnection: PlayerConnection? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is MusicService.MusicBinder) {
                playerConnection = PlayerConnection(this@FloatingLyricsService, service, database, lifecycleScope)
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

        bindService(
            Intent(this, MusicService::class.java),
            serviceConnection,
            BIND_AUTO_CREATE
        )
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
                    
                    CompositionLocalProvider(
                        LocalPlayerConnection provides playerConnection,
                        LocalMenuState provides menuState,
                        LocalOnBackPressedDispatcherOwner provides this@FloatingLyricsService
                    ) {
                        var isMinimized by remember { mutableStateOf(false) }
                        
                        FloatingLyricsContainer(
                            onClose = {
                                shouldShow = false
                                hideOverlay()
                                stopSelf()
                            },
                            onDrag = { x, y ->
                                layoutParams.x += x.roundToInt()
                                layoutParams.y += y.roundToInt()
                                windowManager.updateViewLayout(this, layoutParams)
                            },
                            onResize = { widthDelta, heightDelta ->
                                if (!isMinimized) {
                                    layoutParams.width += widthDelta.roundToInt()
                                    layoutParams.height += heightDelta.roundToInt()
                                    windowManager.updateViewLayout(this, layoutParams)
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
                                windowManager.updateViewLayout(this, layoutParams)
                            },
                            mediaMetadata = mediaMetadata
                        )
                    }
                }
            }
        }

        windowManager.addView(overlayView, layoutParams)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    private fun hideOverlay() {
        if (overlayView != null) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            windowManager.removeView(overlayView)
            overlayView = null
        }
    }

    override fun onDestroy() {
        hideOverlay()
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
    onResize: (Float, Float) -> Unit,
    isMinimized: Boolean,
    onMinimizeToggle: () -> Unit,
    mediaMetadata: MediaMetadata?
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
                onMaximize = onMinimizeToggle,
                opacity = opacity
            )
        } else {
            FloatingLyricsCard(
                onClose = onClose,
                onDrag = onDrag,
                onResize = onResize,
                onMinimize = onMinimizeToggle,
                opacity = opacity,
                mediaMetadata = mediaMetadata
            )
        }
    }
}

@Composable
fun FloatingAppIcon(
    onDrag: (Float, Float) -> Unit,
    onMaximize: () -> Unit,
    opacity: Float
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .alpha(opacity)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x, dragAmount.y)
                    },
                    onDragEnd = {
                        // Optional: Add click handling to maximize if not dragged
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onMaximize) {
            Icon(
                painter = painterResource(R.drawable.lyrics), // Or app icon
                contentDescription = "Maximize",
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun FloatingLyricsCard(
    onClose: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onResize: (Float, Float) -> Unit,
    onMinimize: () -> Unit,
    opacity: Float,
    mediaMetadata: MediaMetadata?
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current
    val (backgroundStyle) = rememberEnumPreference(
        key = FloatingLyricsBackgroundStyleKey,
        defaultValue = FloatingLyricsBackgroundStyle.DEFAULT
    )

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
                MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f * opacity)
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
                                val gradientColorStops = if (gradientColors.size >= 3) {
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
                                        .background(Brush.verticalGradient(colorStops = gradientColorStops))
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
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount.x, dragAmount.y)
                            }
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
                    IconButton(onClick = onMinimize) {
                        Icon(
                            painter = painterResource(R.drawable.expand_more), // Use expand_more as minimize
                            contentDescription = "Minimize",
                            tint = if (backgroundStyle == FloatingLyricsBackgroundStyle.DEFAULT)
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else Color.White
                        )
                    }

                    // Close Button
                    IconButton(onClick = onClose) {
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
                    // Force white text for Blur/Gradient backgrounds, otherwise use default
                    val forceWhite = backgroundStyle != FloatingLyricsBackgroundStyle.DEFAULT
                    // We can't easily force white text in the Lyrics component without passing a parameter
                    // But since we are reusing the Lyrics component, it uses 'expressiveAccent' which defaults to primary/white
                    // based on 'playerBackground' constant.
                    // The Lyrics component reads 'PlayerBackgroundStyleKey'. We are in a separate window.
                    // To strictly enforce colors in Lyrics.kt without changing it too much, we might need a workaround.
                    // However, Lyrics.kt uses 'PlayerBackgroundStyleKey'.
                    // If we want the floating lyrics to adapt, we might need to override that preference locally 
                    // or rely on the theme primary color being white/light.
                    
                    // Since MetrolistTheme is used with 'darkTheme = true', 'expressiveAccent' usually picks Primary color.
                    // If we are in Blur/Gradient, we ideally want White text.
                    // For now, let's rely on the theme. 
                    
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
