package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
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

val pastelPaletteHexes = listOf(
    "#FFD1DC", // Pastel Pink
    "#FFE5B4", // Peach
    "#FFFACD", // Lemon Chiffon
    "#E0FFFF", // Light Cyan
    "#E6E6FA", // Lavender
    "#F0E68C", // Khaki
    "#D8BFD8", // Thistle
    "#D1E8E2", // Mint
    "#FADADD", // Light Rose
    "#C1E1C1", // Pastel Green
    "#B5EAD7", // Seafoam
    "#C7CEEA", // Periwinkle
    "#FFDAC1", // Apricot
    "#E2F0CB", // Pale Green
    "#BFD7EA", // Powder Blue
    "#F1CBFF"  // Lilac
)

val metallicPaletteHexes = listOf(
    "#C0C0C0", // Silver
    "#B87333", // Copper
    "#D4AF37", // Gold
    "#E5E4E2", // Platinum
    "#9C7A3C", // Bronze
    "#8A8D8F", // Steel
    "#A9A9A9", // Dark Silver
    "#6E6E6E", // Gunmetal
    "#D9D6CF", // Pewter
    "#B5A642", // Brass
    "#AAA9AD", // Tin
    "#7A7A7A", // Iron
    "#C5C3C6", // Nickel
    "#C4A484", // Satin Gold
    "#A67C52", // Bronze Brown
    "#C9B037"  // Trophy Gold
)

val brightPaletteHexes = crochetColorHexes

@Composable
fun ColorPalette(
    colorsHex: List<String>,
    selectedIndex: Int,
    onColorSelected: (Int) -> Unit
) {
    val selectedBorder = MaterialTheme.colorScheme.primary
    val unselectedBorder = MaterialTheme.colorScheme.outlineVariant
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(colorsHex) { index, colorHex ->
            val color = hexToColor(colorHex)
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(color)
                    .border(
                        width = if (index == selectedIndex) 4.dp else 2.dp,
                        color = if (index == selectedIndex) selectedBorder else unselectedBorder
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
