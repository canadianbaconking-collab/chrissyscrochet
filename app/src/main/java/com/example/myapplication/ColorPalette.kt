package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// The final, refined 16-color palette.
val crochetColors = listOf(
    // Row 1
    Color(0xFF800020), // Burgundy
    Color(0xFFFF0000), // Bright Red
    Color(0xFFFF8C00), // Deep Orange
    Color(0xFFFFFF00), // Bright Yellow
    // Row 2
    Color(0xFF00CED1), // Dark Turquoise
    Color(0xFF009B4E), // Shamrock Green
    Color(0xFF1E90FF), // Blue (from hex value)
    Color(0xFF0000FF), // Dark Blue (from hex value)
    // Row 3
    Color(0xFFBF00FF), // Vivid Violet
    Color(0xFF800080), // Purple
    Color(0xFFFF00FF), // Magenta
    Color(0xFFFF69B4), // Dark Pink (Hot Pink)
    // Row 4
    Color.White,       // White
    Color(0xFF808080), // Grey
    Color.Black,       // Black
    Color(0xFF8B4513)  // Brown
)

@Composable
fun ColorPalette(
    onColorSelected: (Color) -> Unit,
    selectedColor: Color
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(crochetColors) { color ->
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .background(color)
                    .border(
                        width = if (color == selectedColor) 4.dp else 2.dp,
                        color = if (color == selectedColor) Color.Cyan else Color.Gray
                    )
                    .clickable { onColorSelected(color) }
            )
        }
    }
}