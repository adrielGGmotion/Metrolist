package com.metrolist.music.ui.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// -----------------------------------------------------------------------------
// Data Models (Local Definition for Compilation Safety)
// -----------------------------------------------------------------------------

data class ThemePalette(
    val name: String,
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val tertiary: Color
)

// Predefined Palette List (Using standard Material 3 Colors)
private val ThemePalettes = listOf(
    ThemePalette(
        name = "Blue",
        primary = Color(0xFF2196F3),
        onPrimary = Color.White,
        secondary = Color(0xFF42A5F5),
        tertiary = Color(0xFF64B5F6)
    ),
    ThemePalette(
        name = "Red",
        primary = Color(0xFFF44336),
        onPrimary = Color.White,
        secondary = Color(0xFFEF5350),
        tertiary = Color(0xFFE57373)
    ),
    ThemePalette(
        name = "Green",
        primary = Color(0xFF4CAF50),
        onPrimary = Color.White,
        secondary = Color(0xFF66BB6A),
        tertiary = Color(0xFF81C784)
    ),
    ThemePalette(
        name = "Purple",
        primary = Color(0xFF9C27B0),
        onPrimary = Color.White,
        secondary = Color(0xFFAB47BC),
        tertiary = Color(0xFFBA68C8)
    ),
    ThemePalette(
        name = "Orange",
        primary = Color(0xFFFF9800),
        onPrimary = Color.White,
        secondary = Color(0xFFFFA726),
        tertiary = Color(0xFFFFB74D)
    ),
    ThemePalette(
        name = "Teal",
        primary = Color(0xFF009688),
        onPrimary = Color.White,
        secondary = Color(0xFF26A69A),
        tertiary = Color(0xFF4DB6AC)
    ),
    ThemePalette(
        name = "Pink",
        primary = Color(0xFFE91E63),
        onPrimary = Color.White,
        secondary = Color(0xFFEC407A),
        tertiary = Color(0xFFF06292)
    ),
    ThemePalette(
        name = "Deep Purple",
        primary = Color(0xFF673AB7),
        onPrimary = Color.White,
        secondary = Color(0xFF7E57C2),
        tertiary = Color(0xFF9575CD)
    )
)

enum class ThemeMode {
    LIGHT, DARK, SYSTEM, PURE_BLACK
}

// -----------------------------------------------------------------------------
// Main Screen
// -----------------------------------------------------------------------------

@Composable
fun ThemePickerScreen() {
    // State management
    var selectedPalette by remember { mutableStateOf(ThemePalettes.first()) }
    var selectedMode by remember { mutableStateOf(ThemeMode.SYSTEM) }
    var useMaterialYou by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Music Player Preview Mockup
        // We pass the currently selected palette to make it reactive
        MusicPlayerPreviewMockup(
            palette = selectedPalette,
            isDark = when (selectedMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.PURE_BLACK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            },
            isPureBlack = selectedMode == ThemeMode.PURE_BLACK
        )

        Spacer(modifier = Modifier.weight(1f))

        // 2. Controls Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Mode Selector Header
                Text(
                    text = "Appearance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Mode Selector Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ModeSelectorItem(
                        icon = Icons.Rounded.LightMode,
                        label = "Light",
                        selected = selectedMode == ThemeMode.LIGHT,
                        onClick = { selectedMode = ThemeMode.LIGHT }
                    )
                    ModeSelectorItem(
                        icon = Icons.Rounded.DarkMode,
                        label = "Dark",
                        selected = selectedMode == ThemeMode.DARK,
                        onClick = { selectedMode = ThemeMode.DARK }
                    )
                    ModeSelectorItem(
                        icon = Icons.Rounded.SettingsBrightness,
                        label = "System",
                        selected = selectedMode == ThemeMode.SYSTEM,
                        onClick = { selectedMode = ThemeMode.SYSTEM }
                    )
                    ModeSelectorItem(
                        icon = Icons.Rounded.Contrast,
                        label = "Black",
                        selected = selectedMode == ThemeMode.PURE_BLACK,
                        onClick = { selectedMode = ThemeMode.PURE_BLACK }
                    )
                }

                // Material You Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Material You",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Use system colors",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = useMaterialYou,
                        onCheckedChange = { useMaterialYou = it }
                    )
                }

                // Palette Selector (Only visible if Material You is OFF)
                if (!useMaterialYou) {
                    Text(
                        text = "Accent Color",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                    ) {
                        items(ThemePalettes) { palette ->
                            PaletteSelectorItem(
                                palette = palette,
                                selected = selectedPalette == palette,
                                onClick = { selectedPalette = palette }
                            )
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Component: Music Player Preview Mockup
// -----------------------------------------------------------------------------

@Composable
private fun MusicPlayerPreviewMockup(
    palette: ThemePalette,
    isDark: Boolean,
    isPureBlack: Boolean
) {
    // Determine background color based on mode
    val backgroundColor = when {
        isPureBlack -> Color.Black
        isDark -> Color(0xFF1C1C1E)
        else -> Color(0xFFF2F2F7)
    }

    val textColor = if (isDark) Color.White else Color.Black
    val secondaryTextColor = if (isDark) Color.Gray else Color.DarkGray

    // The Device Frame
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.7f),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // Album Art Placeholder (Large Rounded Rect)
            // Reactivity: Uses the Primary color
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(palette.primary)
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Inner accent circle
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(palette.tertiary.copy(alpha = 0.5f))
                )
            }

            // Text Info
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Song Title",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Artist Name",
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondaryTextColor
                )
            }

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(32.dp)
                )

                // Play Button (Solid Circle)
                // Reactivity: Uses Secondary color
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(palette.secondary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = palette.onPrimary, // High contrast
                        modifier = Modifier.size(32.dp)
                    )
                }

                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Component: Palette Selector Item (Canvas Drawing)
// -----------------------------------------------------------------------------

@Composable
private fun PaletteSelectorItem(
    palette: ThemePalette,
    selected: Boolean,
    onClick: () -> Unit
) {
    // Animate shape from Circle (50%) to Squircle (20%)
    val cornerRadiusPercent by animateDpAsState(
        targetValue = if (selected) 16.dp else 50.dp, // Approximation for Circle->Squircle size
        animationSpec = tween(300), 
        label = "ShapeAnimation"
    )
    
    // Animate border
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "BorderAnimation"
    )

    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(cornerRadiusPercent))
            .background(Color.Transparent)
            .border(
                width = if (selected) 3.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(cornerRadiusPercent)
            )
            .clickable { onClick() }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val center = Offset(w / 2, h / 2)
            val radius = w / 2

            // 1. Top Half (0 to 180 degrees) -> onPrimary
            // drawArc angles: 0 is 3 o'clock. 
            // 180 to 360 covers the top half in standard geometry, but let's be explicit
            // Standard Compose Arc: 0 is Right, 90 is Bottom. 
            // Top Half: 180 (Left) -> 360 (Right) clockwise.
            drawArc(
                color = palette.onPrimary,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true
            )

            // 2. Bottom Left (90 to 180 degrees) -> secondary
            // Compose: 90 is Bottom, 180 is Left.
            drawArc(
                color = palette.secondary,
                startAngle = 90f,
                sweepAngle = 90f,
                useCenter = true
            )

            // 3. Bottom Right (0 to 90 degrees) -> tertiary
            // Compose: 0 is Right, 90 is Bottom.
            drawArc(
                color = palette.tertiary,
                startAngle = 0f,
                sweepAngle = 90f,
                useCenter = true
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Component: Mode Selector Item
// -----------------------------------------------------------------------------

@Composable
private fun ModeSelectorItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(
                    if (selected) MaterialTheme.colorScheme.primaryContainer 
                    else MaterialTheme.colorScheme.surface
                )
                .border(
                    width = 1.dp, 
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
