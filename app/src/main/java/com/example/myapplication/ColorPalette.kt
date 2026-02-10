package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class PaletteTabDefaults(
    val name: String,
    val colors: List<String>
)

private val basicsPaletteHexes = listOf(
    "#FF7A2A2F", // Muted Red
    "#FF9A4A22", // Muted Orange
    "#FF9B5A1D", // Muted Amber
    "#FF8A7421", // Muted Mustard
    "#FF6F7A1F", // Muted Yellow-Lime
    "#FF3F6A33", // Muted Green
    "#FF2F6A47", // Muted Kelly Green
    "#FF2A6B67", // Muted Teal
    "#FF2C5F8A", // Muted Blue
    "#FF3E3F8F", // Muted Indigo
    "#FF5A3D8F", // Muted Purple
    "#FF8A3A67"  // Muted Pink
)

private val earthPaletteHexes = listOf(
    "#FF2B1B12", // Deep Chocolate
    "#FF4A2E1F", // Umber
    "#FF6A3D2A", // Rich Brown
    "#FF8A4B3A", // Clay
    "#FFB04A2F", // Rust
    "#FF7A3F2A", // Burnt Sienna
    "#FF6B6A3B", // Universal Khaki
    "#FF7C7344", // Dry Olive
    "#FF2F4A2E", // Moss
    "#FF3F5A3C", // Forest Moss
    "#FF2A3A4A", // Stone Blue
    "#FF5A4A3A"  // Weathered Taupe
)

private val brightPaletteHexes = listOf(
    "#FFD0021B", // Strong Red
    "#FFFF4D00", // Vivid Orange
    "#FFFF8C00", // Amber Orange
    "#FFF2C500", // Rich Yellow
    "#FFE5FF00", // Sharp Yellow-Lime
    "#FF2ECC40", // Strong Green
    "#FF00A651", // Kelly Green
    "#FF00B3A4", // Bright Teal
    "#FF0084FF", // Strong Blue
    "#FF3A2BFF", // Bright Indigo
    "#FF7A00FF", // Bright Purple
    "#FFFF1493"  // Hot Pink
)

private val jewelPaletteHexes = listOf(
    "#FF0B6B3A", // Emerald
    "#FF0F7A46", // Deep Emerald
    "#FF123B8A", // Sapphire
    "#FF1B2F6B", // Royal Navy
    "#FF5A2A82", // Amethyst
    "#FF6C1F8F", // Royal Purple
    "#FF8A0E1E", // Ruby
    "#FFA3122A", // Garnet
    "#FF2A0F24", // Plum Noir
    "#FF3B1438", // Black Plum
    "#FF0B4F4A", // Teal Depth
    "#FF006B66"  // Deep Jade Teal
)

private val neonPaletteHexes = listOf(
    "#FF39FF14", // Acid Green
    "#FF7DFF00", // Volt Lime
    "#FF00FF66", // Electric Mint
    "#FF00FFD5", // Neon Aqua
    "#FF00E5FF", // Electric Cyan
    "#FF00A2FF", // Neon Blue
    "#FF2B5BFF", // Laser Blue
    "#FF7A00FF", // Hyper Violet
    "#FFB000FF", // Electric Purple
    "#FFFF2BD6", // Laser Magenta
    "#FFFF007A", // Neon Rose
    "#FFFFF000"  // Plasma Yellow
)

private val mermaidPaletteHexes = listOf(
    "#FF009E9E", // Abyss Teal
    "#FF00A6A6", // Shimmer Teal
    "#FF007C91", // Deep Aqua
    "#FF006D84", // Tidal Blue
    "#FF00C2A8", // Lagoon Glow
    "#FF1BBF8A", // Sea Glass Green
    "#FF00D4B2", // Biolume Mint
    "#FF1A4DFF", // Siren Blue
    "#FF2C66FF", // Cobalt Current
    "#FF5A3DFF", // Ocean Violet
    "#FF7A2FFF", // Pearl Purple
    "#FF00B8D9"  // Reef Glow
)

private val metallicPaletteHexes = listOf(
    // Liquid Chrome (highlight -> shadow)
    "#FFF7FAFF",
    "#FFD9E2EE",
    "#FFA8B3C2",
    "#FF6C7786",
    "#FF2B3440",
    // Gold (highlight -> shadow)
    "#FFFFF2B0",
    "#FFFFD36A",
    "#FFCC9A2E",
    "#FF8A5A12",
    "#FF3A2208",
    // Copper (highlight -> shadow)
    "#FFFFC9A6",
    "#FFFF8A4A",
    "#FFB14D2A",
    "#FF6B2A18",
    "#FF2A120C",
    // Rose Gold (highlight -> shadow)
    "#FFFFD6D1",
    "#FFFFA08C",
    "#FFCC6A5A",
    "#FF7A3A34",
    "#FF2A1412",
    // Gunmetal (highlight -> shadow)
    "#FFD8DEE8",
    "#FFA9B3C2",
    "#FF768395",
    "#FF4A5668",
    "#FF202833"
)

private val customPaletteHexes = listOf(
    "#FF111111", // Deep Outline
    "#FFFFFFFF", // True White
    "#FF1C1C1C", // Ink Black
    "#FFFFF4E8", // Warm Highlight
    "#FFC97A56", // Warm Skin
    "#FF8A5A3B", // Medium Skin / Wood
    "#FF5A3A24", // Dark Wood
    "#FF6E4B2A", // Walnut
    "#FF2E6F9E", // Denim Blue
    "#FFB22222", // Brick Red
    "#FF556B2F", // Olive Drab
    "#FF3A4A5A"  // Blue Gray Utility
)

const val CUSTOM_TAB_INDEX = 7

val defaultPaletteTabs = listOf(
    PaletteTabDefaults(name = "Basics", colors = basicsPaletteHexes),
    PaletteTabDefaults(name = "Earth", colors = earthPaletteHexes),
    PaletteTabDefaults(name = "Bright", colors = brightPaletteHexes),
    PaletteTabDefaults(name = "Jewel", colors = jewelPaletteHexes),
    PaletteTabDefaults(name = "Neon", colors = neonPaletteHexes),
    PaletteTabDefaults(name = "Mermaid", colors = mermaidPaletteHexes),
    PaletteTabDefaults(name = "Metallic", colors = metallicPaletteHexes),
    PaletteTabDefaults(name = "Custom", colors = customPaletteHexes)
)

@Composable
fun ColorPalette(
    colorsHex: List<String>,
    selectedIndex: Int,
    onColorSelected: (Int) -> Unit
) {
    val selectedBorder = MaterialTheme.colorScheme.primary
    val unselectedBorder = MaterialTheme.colorScheme.outlineVariant
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        colorsHex.chunked(4).forEachIndexed { rowIndex, rowColors ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowColors.forEachIndexed { colIndex, colorHex ->
                    val index = rowIndex * 4 + colIndex
                    val color = hexToColor(colorHex)
                    Box(
                        modifier = Modifier
                            .size(64.dp)
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
