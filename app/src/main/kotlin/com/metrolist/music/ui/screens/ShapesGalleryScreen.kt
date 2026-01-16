package com.metrolist.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.res.painterResource
import com.metrolist.music.R
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.metrolist.music.ui.shapes.ExpressiveShapes
import com.metrolist.music.ui.shapes.GeometricShapes

data class ShapeItem(val name: String, val shape: Shape)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShapesGalleryScreen(navController: NavController) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    
    val geometricShapes = listOf(
        ShapeItem("Circle", GeometricShapes.Circle),
        ShapeItem("Square", GeometricShapes.Square),
        ShapeItem("Pill", GeometricShapes.Pill),
        ShapeItem("Rectangle", GeometricShapes.Rectangle),
        ShapeItem("Slanted", ExpressiveShapes.Slanted),
        ShapeItem("Oval", ExpressiveShapes.Oval),
        ShapeItem("SemiCircle", ExpressiveShapes.SemiCircle),
        ShapeItem("Arch", ExpressiveShapes.Arch),
        ShapeItem("Fan", ExpressiveShapes.Fan),
        ShapeItem("Arrow", ExpressiveShapes.Arrow),
        ShapeItem("Triangle", GeometricShapes.Triangle),
        ShapeItem("Pentagon", GeometricShapes.Pentagon),
        ShapeItem("Hexagon", GeometricShapes.Hexagon),
        ShapeItem("Octagon", GeometricShapes.Octagon),
        ShapeItem("Diamond", ExpressiveShapes.Diamond),
        ShapeItem("Clamshell", ExpressiveShapes.Clamshell),
        ShapeItem("Gem", ExpressiveShapes.Gem),
        ShapeItem("Heart", ExpressiveShapes.Heart),
        ShapeItem("Bun", ExpressiveShapes.Bun),
        ShapeItem("Ghostish", ExpressiveShapes.Ghostish),
        ShapeItem("Puffy", ExpressiveShapes.Puffy),
        ShapeItem("Puffy Diamond", ExpressiveShapes.PuffyDiamond),
    )

    val cookieShapes = listOf(
        ShapeItem("Cookie 4", ExpressiveShapes.Cookie4Sided),
        ShapeItem("Cookie 6", ExpressiveShapes.Cookie6Sided),
        ShapeItem("Cookie 7", ExpressiveShapes.Cookie7Sided),
        ShapeItem("Cookie 9", ExpressiveShapes.Cookie9Sided),
        ShapeItem("Cookie 12", ExpressiveShapes.Cookie12Sided)
    )

    val expressiveShapes = listOf(
        ShapeItem("Clover 4", ExpressiveShapes.Clover4Leaf),
        ShapeItem("Clover 8", ExpressiveShapes.Clover8Leaf),
        ShapeItem("Flower", ExpressiveShapes.Flower),
        ShapeItem("Burst", ExpressiveShapes.Burst),
        ShapeItem("Soft Burst", ExpressiveShapes.SoftBurst),
        ShapeItem("Boom", ExpressiveShapes.Boom),
        ShapeItem("Soft Boom", ExpressiveShapes.SoftBoom),
        ShapeItem("Sunny", ExpressiveShapes.Sunny),
        ShapeItem("Very Sunny", ExpressiveShapes.VerySunny),
        ShapeItem("Pixel Circle", ExpressiveShapes.PixelCircle),
        ShapeItem("Pixel Triangle", ExpressiveShapes.PixelTriangle),
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Shapes Gallery") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item(span = { GridItemSpan(this.maxLineSpan) }) {
                Box(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        "Geometric Shapes",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            items(
                items = geometricShapes,
                key = { "geom_${it.name}" }
            ) { item ->
                ShapeCard(item)
            }

            item(span = { GridItemSpan(this.maxLineSpan) }) {
                Box(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        "Cookie Shapes",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            items(
                items = cookieShapes,
                key = { "cookie_${it.name}" }
            ) { item ->
                ShapeCard(item)
            }

            item(span = { GridItemSpan(this.maxLineSpan) }) {
                Box(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        "Expressive Shapes",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            items(
                items = expressiveShapes,
                key = { "expr_${it.name}" }
            ) { item ->
                ShapeCard(item)
            }
        }
    }
}

@Composable
fun ShapeCard(item: ShapeItem) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(item.shape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            // Optional: content inside
        }
        Text(
            text = item.name,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp),
            maxLines = 1
        )
    }
}
