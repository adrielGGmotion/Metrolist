/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.metrolist.music.R
import com.metrolist.music.ui.theme.MetrolistTheme

@Composable
fun ColorPickerContent(
    initialColor: Int,
    onColorSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedColor by remember { mutableStateOf(Color(initialColor)) }

    val presets = remember {
        listOf(
            Color(0xFFED5564), // Default Red
            Color(0xFFD32F2F), // Red
            Color(0xFFC2185B), // Pink
            Color(0xFF7B1FA2), // Purple
            Color(0xFF512DA8), // Deep Purple
            Color(0xFF303F9F), // Indigo
            Color(0xFF1976D2), // Blue
            Color(0xFF0288D1), // Light Blue
            Color(0xFF0097A7), // Cyan
            Color(0xFF00796B), // Teal
            Color(0xFF388E3C), // Green
            Color(0xFF689F38), // Light Green
            Color(0xFFFBC02D), // Yellow
            Color(0xFFFFA000), // Amber
            Color(0xFFF57C00), // Orange
            Color(0xFFE64A19), // Deep Orange
            Color(0xFF5D4037), // Brown
            Color(0xFF616161), // Gray
            Color(0xFF455A64), // Blue Gray
            Color(0xFF000000)  // Black
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = stringResource(R.string.theme),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Mockup Preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
        ) {
            MetrolistTheme(
                themeColor = selectedColor,
                darkTheme = true // Force dark for preview or follow system? Let's use dark for consistency with "Couleurs" dark bg
            ) {
                // Mockup content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        ) {
                             Icon(
                                 painter = painterResource(R.drawable.music_note),
                                 contentDescription = null,
                                 tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                 modifier = Modifier.align(Alignment.Center).size(20.dp)
                             )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Preview",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // List Items
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Box(
                                    modifier = Modifier
                                        .width(120.dp)
                                        .height(12.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.onSurface)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .width(80.dp)
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.tertiaryContainer)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Box(
                                    modifier = Modifier
                                        .width(100.dp)
                                        .height(12.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.onSurface)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .width(60.dp)
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Color List
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.width(4.dp)) // Start padding
            presets.forEach { color ->
                ColorTupleItem(
                    color = color,
                    isSelected = color == selectedColor,
                    onClick = {
                        selectedColor = color
                        onColorSelected(color.toArgb())
                    }
                )
            }
            Spacer(modifier = Modifier.width(4.dp)) // End padding
        }
    }
}

@Composable
fun ColorTupleItem(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Generate scheme to preview palette
    val scheme = rememberDynamicColorScheme(
        seedColor = color,
        isDark = true,
        style = PaletteStyle.TonalSpot
    )

    val primary = scheme.primary
    val secondary = scheme.secondary
    val tertiary = scheme.tertiary

    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .then(
                if (isSelected) {
                    Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                } else {
                    Modifier
                }
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val center = Offset(width / 2, height / 2)

            // Draw Top Half (Primary)
            drawArc(
                color = primary,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset.Zero,
                size = Size(width, height)
            )

            // Draw Bottom Left (Secondary)
            drawArc(
                color = secondary,
                startAngle = 90f,
                sweepAngle = 90f,
                useCenter = true,
                topLeft = Offset.Zero,
                size = Size(width, height)
            )

            // Draw Bottom Right (Tertiary)
            drawArc(
                color = tertiary,
                startAngle = 0f,
                sweepAngle = 90f,
                useCenter = true,
                topLeft = Offset.Zero,
                size = Size(width, height)
            )
        }
    }
}
