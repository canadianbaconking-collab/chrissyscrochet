package com.example.myapplication

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.UiColors

@Composable
fun PatternGrid(
    modifier: Modifier = Modifier,
    pattern: List<Color>,
    gridSize: Int,
    showCrosshairGuides: Boolean = true,
    onColorChange: (Int, Color) -> Unit,
    selectedColor: Color
) {
    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .clipToBounds()
    ) {
        drawRect(color = UiColors.SurfaceBg)
        if (gridSize <= 0) return@Canvas
        val cellSize = size.minDimension / gridSize.toFloat()
        val gridWidth = cellSize * gridSize
        val left = (size.width - gridWidth) / 2f
        val top = (size.height - gridWidth) / 2f
        val gridBg = UiColors.GridBg
        val emptyCell = gridBg

        drawRect(
            color = gridBg,
            topLeft = androidx.compose.ui.geometry.Offset(left, top),
            size = androidx.compose.ui.geometry.Size(gridWidth, gridWidth)
        )

        val subtleLineWidth = 0.75.dp.toPx()
        for (i in 1 until gridSize) {
            val p = i * cellSize
            drawLine(
                color = UiColors.SubtleGridLine,
                start = androidx.compose.ui.geometry.Offset(left + p, top),
                end = androidx.compose.ui.geometry.Offset(left + p, top + gridWidth),
                strokeWidth = subtleLineWidth
            )
            drawLine(
                color = UiColors.SubtleGridLine,
                start = androidx.compose.ui.geometry.Offset(left, top + p),
                end = androidx.compose.ui.geometry.Offset(left + gridWidth, top + p),
                strokeWidth = subtleLineWidth
            )
        }

        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                val index = row * gridSize + col
                if (index !in pattern.indices) continue
                val x = left + col * cellSize
                val y = top + row * cellSize
                drawRect(
                    color = if (pattern[index] == UiColors.SurfaceBg) emptyCell else pattern[index],
                    topLeft = androidx.compose.ui.geometry.Offset(x, y),
                    size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
                )
            }
        }

        if (showCrosshairGuides) {
            val crosshairStroke = 2.dp.toPx()
            if (gridSize % 2 == 0) {
                val centerX = left + (gridSize / 2f) * cellSize
                val centerY = top + (gridSize / 2f) * cellSize
                drawLine(
                    color = UiColors.Crosshair,
                    start = androidx.compose.ui.geometry.Offset(centerX, top),
                    end = androidx.compose.ui.geometry.Offset(centerX, top + gridWidth),
                    strokeWidth = crosshairStroke
                )
                drawLine(
                    color = UiColors.Crosshair,
                    start = androidx.compose.ui.geometry.Offset(left, centerY),
                    end = androidx.compose.ui.geometry.Offset(left + gridWidth, centerY),
                    strokeWidth = crosshairStroke
                )
            } else {
                val center = gridSize / 2
                val centerRowBandTop = top + center * cellSize
                val centerColBandLeft = left + center * cellSize
                drawRect(
                    color = UiColors.Crosshair,
                    topLeft = androidx.compose.ui.geometry.Offset(left, centerRowBandTop),
                    size = androidx.compose.ui.geometry.Size(gridWidth, cellSize),
                    style = Stroke(width = crosshairStroke)
                )
                drawRect(
                    color = UiColors.Crosshair,
                    topLeft = androidx.compose.ui.geometry.Offset(centerColBandLeft, top),
                    size = androidx.compose.ui.geometry.Size(cellSize, gridWidth),
                    style = Stroke(width = crosshairStroke)
                )
            }
        }
    }
}
