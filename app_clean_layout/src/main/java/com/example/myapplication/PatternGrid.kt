package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color

@Composable
fun PatternGrid(
    modifier: Modifier = Modifier,
    pattern: List<Color>,
    gridSize: Int,
    onColorChange: (Int, Color) -> Unit,
    selectedColor: Color
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(gridSize),
        modifier = modifier
            .aspectRatio(1f)
            .clipToBounds(),
        userScrollEnabled = false
    ) {
        itemsIndexed(pattern) { _, color ->
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .background(color)
            )
        }
    }
}
