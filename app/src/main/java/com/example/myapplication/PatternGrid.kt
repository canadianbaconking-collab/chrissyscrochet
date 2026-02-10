package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.UiColors

@Composable
fun PatternGrid(
    modifier: Modifier = Modifier,
    pattern: List<Color>,
    gridSize: Int,
    onColorChange: (Int, Color) -> Unit,
    selectedColor: Color
) {
    val gridLineColor = UiColors.GridLine
    val centerLineColor = UiColors.Crosshair

    LazyVerticalGrid(
        columns = GridCells.Fixed(gridSize),
        modifier = modifier,
        userScrollEnabled = false
    ) {
        itemsIndexed(pattern) { index, color ->
            val row = index / gridSize
            val col = index % gridSize
            
            val isOddGrid = gridSize % 2 != 0
            val centerIndex = gridSize / 2
            val isCenterCell = isOddGrid && (row == centerIndex || col == centerIndex)

            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .background(color)
                    .border(
                        width = if (isCenterCell) 1.5.dp else 1.dp,
                        color = if (isCenterCell) centerLineColor else gridLineColor
                    )
                    .clickable {
                        // Eyedropper removed, always change color on click
                        onColorChange(index, selectedColor)
                    }
            )
        }
    }
}
