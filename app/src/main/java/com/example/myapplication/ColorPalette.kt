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
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// The final, refined 16-color palette.
val crochetColorHexes = listOf(
    // Row 1
    "#800020", // Burgundy
    "#FF0000", // Bright Red
    "#FF8C00", // Deep Orange
    "#FFFF00", // Bright Yellow
    // Row 2
    "#00CED1", // Dark Turquoise
    "#009B4E", // Shamrock Green
    "#1E90FF", // Blue (from hex value)
    "#0000FF", // Dark Blue (from hex value)
    // Row 3
    "#BF00FF", // Vivid Violet
    "#800080", // Purple
    "#FF00FF", // Magenta
    "#FF69B4", // Dark Pink (Hot Pink)
    // Row 4
    "#FFFFFF", // White
    "#808080", // Grey
    "#000000", // Black
    "#8B4513"  // Brown
)

@Composable
fun ColorPalette(
    colorsHex: List<String>,
    selectedIndex: Int,
    onColorSelected: (Int) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(colorsHex) { index, colorHex ->
            val color = hexToColor(colorHex)
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .background(color)
                    .border(
                        width = if (index == selectedIndex) 4.dp else 2.dp,
                        color = if (index == selectedIndex) Color.Cyan else Color.Gray
                    )
                    .clickable { onColorSelected(index) }
            )
        }
    }
}

fun normalizeHexInput(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return null
    val hex = if (trimmed.startsWith("#")) trimmed.drop(1) else trimmed
    if (hex.length != 6 && hex.length != 8) return null
    if (!hex.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) return null
    return "#${hex.uppercase()}"
}

fun hexToColor(hex: String): Color {
    val normalized = normalizeHexInput(hex) ?: "#FFFFFFFF"
    val hexDigits = normalized.drop(1)
    val argb = if (hexDigits.length == 6) "FF$hexDigits" else hexDigits
    val value = argb.toLong(16)
    return Color(value)
}
