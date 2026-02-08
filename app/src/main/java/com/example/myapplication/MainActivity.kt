package com.example.myapplication

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.myapplication.ui.theme.MyApplicationTheme
import java.io.File

enum class SymmetryMode { NONE, VERTICAL, HORIZONTAL, QUADRANT }
enum class ToolMode { BRUSH, REPLACE }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var pastelHexColors by remember { mutableStateOf(pastelPaletteHexes) }
                var metallicHexColors by remember { mutableStateOf(metallicPaletteHexes) }
                var brightHexColors by remember { mutableStateOf(brightPaletteHexes) }
                var customHexColors by remember { mutableStateOf(listOf(brightPaletteHexes.first())) }
                var selectedPaletteTab by remember { mutableIntStateOf(0) }
                var selectedColorIndex by remember { mutableIntStateOf(0) }
                val paletteHexColors = when (selectedPaletteTab) {
                    0 -> pastelHexColors
                    1 -> metallicHexColors
                    2 -> brightHexColors
                    else -> customHexColors
                }
                val selectedColor = hexToColor(paletteHexColors[selectedColorIndex])
                var gridSize by remember { mutableIntStateOf(36) }

                var history by remember { mutableStateOf(listOf(List(gridSize * gridSize) { Color.White })) }
                var historyIndex by remember { mutableIntStateOf(0) }
                val pattern by remember { derivedStateOf { history[historyIndex] } }

                var paletteVisible by remember { mutableStateOf(false) }
                var showSaveDialog by remember { mutableStateOf(false) }
                var showLoadDialog by remember { mutableStateOf(false) }
                var showEditHexDialog by remember { mutableStateOf(false) }
                var hexInput by remember { mutableStateOf("") }
                var showLoadConfirmDialog by remember { mutableStateOf(false) }
                var pendingLoadName by remember { mutableStateOf<String?>(null) }
                var showNewConfirmDialog by remember { mutableStateOf(false) }
                var toolMode by remember { mutableStateOf(ToolMode.BRUSH) }
                var replaceSourceColor by remember { mutableStateOf<Color?>(null) }
                var pickSourceArmed by remember { mutableStateOf(false) }
                var showReplaceConfirmDialog by remember { mutableStateOf(false) }
                var filename by remember { mutableStateOf("") }
                val context = LocalContext.current

                var scale by remember { mutableStateOf(1f) }
                var offset by remember { mutableStateOf(Offset.Zero) }
                var layoutSize by remember { mutableStateOf(IntSize.Zero) }
                var symmetryMode by remember { mutableStateOf(SymmetryMode.NONE) }
                var showGridLines by remember { mutableStateOf(true) }

                fun selectIndexForPalette(currentHex: String, palette: List<String>): Int {
                    val index = palette.indexOf(currentHex)
                    return if (index >= 0) index else 0
                }

                fun updatePattern(newPattern: List<Color>, reset: Boolean = false) {
                    val newHistory = if (reset) {
                        mutableListOf(newPattern)
                    } else {
                        history.take(historyIndex + 1).toMutableList().apply { add(newPattern) }
                    }

                    if (newHistory.size > 50) {
                        newHistory.removeAt(0)
                    }
                    
                    history = newHistory
                    historyIndex = newHistory.lastIndex
                }

                fun applyReplace(source: Color, target: Color) {
                    val updated = pattern.map { if (it == source) target else it }
                    updatePattern(updated)
                }
                
                fun changeGridSize(newSize: Int) {
                    gridSize = newSize
                    updatePattern(List(newSize * newSize) { Color.White }, reset = true)
                    scale = 1f
                    offset = Offset.Zero
                }

                fun startNewPattern() {
                    updatePattern(List(gridSize * gridSize) { Color.White }, reset = true)
                    scale = 1f
                    offset = Offset.Zero
                }

                fun loadPatternByName(name: String) {
                    val loadedPattern = loadPattern(context, name, gridSize)
                    if (loadedPattern.isNotEmpty()) {
                        if (loadedPattern.size == gridSize * gridSize) {
                            updatePattern(loadedPattern)
                            Toast.makeText(context, "Pattern loaded", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Pattern size mismatch", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Failed to load", Toast.LENGTH_SHORT).show()
                    }
                }

                if (showLoadConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = {
                            showLoadConfirmDialog = false
                            pendingLoadName = null
                        },
                        title = { Text("Load Pattern?") },
                        text = { Text("Loading will replace the current pattern.") },
                        confirmButton = {
                            Button(onClick = {
                                val name = pendingLoadName
                                showLoadConfirmDialog = false
                                pendingLoadName = null
                                if (name != null) {
                                    loadPatternByName(name)
                                }
                            }) { Text("Confirm") }
                        },
                        dismissButton = {
                            Button(onClick = {
                                showLoadConfirmDialog = false
                                pendingLoadName = null
                            }) { Text("Cancel") }
                        }
                    )
                }

                if (showEditHexDialog) {
                    val normalizedHex = normalizeHexInput(hexInput)
                    val isValid = normalizedHex != null
                    AlertDialog(
                        onDismissRequest = { showEditHexDialog = false },
                        title = { Text("Edit Hex") },
                        text = {
                            Column {
                                TextField(
                                    value = hexInput,
                                    onValueChange = { hexInput = it },
                                    label = { Text("Hex Color") },
                                    singleLine = true
                                )
                                if (!isValid) {
                                    Text(
                                        "Enter a valid hex color.",
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val newHex = normalizedHex
                                    if (newHex != null) {
                                        val updated = paletteHexColors.toMutableList()
                                        updated[selectedColorIndex] = newHex
                                        when (selectedPaletteTab) {
                                            0 -> pastelHexColors = updated
                                            1 -> metallicHexColors = updated
                                            2 -> brightHexColors = updated
                                            else -> customHexColors = updated
                                        }
                                        val shouldAddToCustom = if (selectedPaletteTab == 3) {
                                            !updated.contains(newHex)
                                        } else {
                                            !customHexColors.contains(newHex)
                                        }
                                        if (shouldAddToCustom) {
                                            customHexColors = customHexColors + newHex
                                        }
                                        showEditHexDialog = false
                                    }
                                },
                                enabled = isValid
                            ) { Text("Confirm") }
                        },
                        dismissButton = {
                            Button(onClick = { showEditHexDialog = false }) { Text("Cancel") }
                        }
                    )
                }

                if (showNewConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { showNewConfirmDialog = false },
                        title = { Text("New Pattern?") },
                        text = { Text("This will clear the current pattern.") },
                        confirmButton = {
                            Button(onClick = {
                                showNewConfirmDialog = false
                                startNewPattern()
                            }) { Text("Confirm") }
                        },
                        dismissButton = {
                            Button(onClick = { showNewConfirmDialog = false }) { Text("Cancel") }
                        }
                    )
                }

                if (showSaveDialog) {
                    AlertDialog(
                        onDismissRequest = { showSaveDialog = false },
                        title = { Text("Save Pattern") },
                        text = {
                            TextField(
                                value = filename,
                                onValueChange = { filename = it },
                                label = { Text("Filename") }
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (filename.isNotBlank()) {
                                        val success = savePattern(context, filename, pattern, gridSize)
                                        if (success) {
                                            Toast.makeText(context, "Pattern saved", Toast.LENGTH_SHORT).show()
                                            showSaveDialog = false
                                            filename = ""
                                        } else {
                                            Toast.makeText(context, "Failed to save", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            ) { Text("Save") }
                        },
                        dismissButton = {
                            Button(onClick = {
                                showSaveDialog = false
                                filename = ""
                            }) { Text("Cancel") }
                        }
                    )
                }

                if (showReplaceConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { showReplaceConfirmDialog = false },
                        title = { Text("Apply Replace?") },
                        text = { Text("Replace all matching colors in the pattern?") },
                        confirmButton = {
                            Button(onClick = {
                                val source = replaceSourceColor
                                showReplaceConfirmDialog = false
                                if (source != null) {
                                    applyReplace(source, selectedColor)
                                }
                            }) { Text("Confirm") }
                        },
                        dismissButton = {
                            Button(onClick = { showReplaceConfirmDialog = false }) { Text("Cancel") }
                        }
                    )
                }

                if (showLoadDialog) {
                    val savedPatterns = remember { getSavedPatterns(context) }
                    Dialog(onDismissRequest = { showLoadDialog = false }) {
                        Surface(modifier = Modifier.padding(16.dp)) {
                            Column {
                                Text(
                                    "Saved Patterns",
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                if (savedPatterns.isEmpty()) {
                                    Text(
                                        "No saved patterns",
                                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )
                                } else {
                                    LazyVerticalGrid(
                                        columns = GridCells.Adaptive(minSize = 128.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(8.dp)
                                    ) {
                                        items(savedPatterns) { savedPattern ->
                                            Card(
                                                modifier = Modifier.clickable {
                                                    pendingLoadName = savedPattern.name
                                                    showLoadDialog = false
                                                    showLoadConfirmDialog = true
                                                }
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    val bitmap = remember(savedPattern.thumbnailUrl) {
                                                        loadBitmapSafely(savedPattern.thumbnailUrl)
                                                    }
                                                    
                                                    DisposableEffect(bitmap) {
                                                        onDispose {
                                                            bitmap?.recycle()
                                                        }
                                                    }

                                                    if (bitmap != null) {
                                                        Image(
                                                            bitmap = bitmap.asImageBitmap(),
                                                            contentDescription = "Thumbnail",
                                                            modifier = Modifier.size(128.dp),
                                                            contentScale = ContentScale.Crop
                                                        )
                                                    } else {
                                                        Box(
                                                            modifier = Modifier.size(128.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text("No preview")
                                                        }
                                                    }
                                                    Text(
                                                        savedPattern.name,
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier.padding(4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Scaffold { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row {
                                    Button(onClick = { showNewConfirmDialog = true }) { Text("New") }
                                    Spacer(Modifier.width(8.dp))
                                    Button(onClick = { showSaveDialog = true }) { Text("Save") }
                                    Spacer(Modifier.width(8.dp))
                                    Button(onClick = { showLoadDialog = true }) { Text("Load") }
                                }
                                Row {
                                    IconButton(
                                        onClick = { if (historyIndex > 0) historyIndex-- },
                                        enabled = historyIndex > 0
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Undo, "Undo")
                                    }
                                    IconButton(
                                        onClick = { if (historyIndex < history.lastIndex) historyIndex++ },
                                        enabled = historyIndex < history.lastIndex
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Redo, "Redo")
                                    }
                                    IconButton(onClick = {
                                        val success = exportPatternToImage(context, "CrochetPattern", pattern, gridSize)
                                        Toast.makeText(
                                            context,
                                            if (success) "Saved to Pictures" else "Save failed",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }) {
                                        Icon(Icons.Filled.Save, "Export")
                                    }
                                }
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                val sizes = listOf(21, 25, 29, 36)
                                sizes.forEach { size ->
                                    TextButton(
                                        onClick = { changeGridSize(size) },
                                        enabled = gridSize != size
                                    ) {
                                        Text("$size x $size")
                                    }
                                }
                            }

                            val onColorChange: (Int, Color) -> Unit = label@{ index, color ->
                                if (toolMode == ToolMode.REPLACE) {
                                    if (pickSourceArmed && index in pattern.indices) {
                                        replaceSourceColor = pattern[index]
                                        pickSourceArmed = false
                                    }
                                    return@label
                                }
                                val newPattern = pattern.toMutableList()
                                val col = index % gridSize
                                val row = index / gridSize

                                newPattern[index] = color
                                
                                val isOddGrid = gridSize % 2 != 0
                                val center = gridSize / 2

                                when (symmetryMode) {
                                    SymmetryMode.VERTICAL -> {
                                        val mirroredCol = gridSize - 1 - col
                                        if (col != mirroredCol) {
                                            newPattern[row * gridSize + mirroredCol] = color
                                        }
                                    }
                                    SymmetryMode.HORIZONTAL -> {
                                        val mirroredRow = gridSize - 1 - row
                                        if (row != mirroredRow) {
                                            newPattern[mirroredRow * gridSize + col] = color
                                        }
                                    }
                                    SymmetryMode.QUADRANT -> {
                                        val mirroredCol = gridSize - 1 - col
                                        val mirroredRow = gridSize - 1 - row
                                        
                                        if (isOddGrid && (row == center || col == center)) {
                                            if (row == center && col != center) {
                                                newPattern[row * gridSize + mirroredCol] = color
                                            } else if (col == center && row != center) {
                                                 newPattern[mirroredRow * gridSize + col] = color
                                            }
                                        } else {
                                            if (col != mirroredCol) newPattern[row * gridSize + mirroredCol] = color
                                            if (row != mirroredRow) newPattern[mirroredRow * gridSize + col] = color
                                            if (col != mirroredCol && row != mirroredRow) newPattern[mirroredRow * gridSize + mirroredCol] = color
                                        }
                                    }
                                    SymmetryMode.NONE -> {}
                                }
                                updatePattern(newPattern)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(24.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RectangleShape)
                                        .onSizeChanged { layoutSize = it }
                                        .pointerInput(Unit) {
                                            detectTransformGestures { _, pan, zoom, _ ->
                                                scale = (scale * zoom).coerceIn(0.5f, 5f)
                                                offset += pan
                                            }
                                        }
                                        .pointerInput(Unit) {
                                            var lastIndex: Int? = null
                                            detectDragGestures(
                                                onDragEnd = { lastIndex = null },
                                                onDragCancel = { lastIndex = null }
                                            ) { change, _ ->
                                                val position = change.position
                                                val gridLayoutSize = layoutSize.width.coerceAtMost(layoutSize.height).toFloat()

                                                val transformedX = (position.x - offset.x - (layoutSize.width - gridLayoutSize * scale) / 2f) / scale
                                                val transformedY = (position.y - offset.y - (layoutSize.height - gridLayoutSize * scale) / 2f) / scale

                                                val col = (transformedX / (gridLayoutSize / gridSize)).toInt()
                                                val row = (transformedY / (gridLayoutSize / gridSize)).toInt()

                                                if (col in 0 until gridSize && row in 0 until gridSize) {
                                                    val index = row * gridSize + col
                                                    if (index in pattern.indices && index != lastIndex) {
                                                        if (toolMode == ToolMode.BRUSH) {
                                                            onColorChange(index, selectedColor)
                                                            lastIndex = index
                                                        }
                                                    }
                                                }
                                                change.consume()
                                            }
                                        }
                                        .pointerInput(Unit) {
                                            detectTapGestures { tapOffset ->
                                                val gridLayoutSize = layoutSize.width.coerceAtMost(layoutSize.height).toFloat()

                                                val transformedX = (tapOffset.x - offset.x - (layoutSize.width - gridLayoutSize * scale) / 2f) / scale
                                                val transformedY = (tapOffset.y - offset.y - (layoutSize.height - gridLayoutSize * scale) / 2f) / scale

                                                val col = (transformedX / (gridLayoutSize / gridSize)).toInt()
                                                val row = (transformedY / (gridLayoutSize / gridSize)).toInt()

                                                if (col in 0 until gridSize && row in 0 until gridSize) {
                                                    val index = row * gridSize + col
                                                    if (index in pattern.indices) {
                                                        if (toolMode == ToolMode.BRUSH) {
                                                            onColorChange(index, selectedColor)
                                                        } else if (toolMode == ToolMode.REPLACE && pickSourceArmed) {
                                                            replaceSourceColor = pattern[index]
                                                            pickSourceArmed = false
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    PatternGrid(
                                        modifier = Modifier.graphicsLayer(
                                            scaleX = scale,
                                            scaleY = scale,
                                            translationX = offset.x,
                                            translationY = offset.y
                                        ),
                                        pattern = pattern,
                                        gridSize = gridSize,
                                        onColorChange = onColorChange,
                                        selectedColor = selectedColor,
                                        showGridLines = showGridLines
                                    )

                                    if (showGridLines && gridSize % 2 == 0) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            val strokeWidth = 2.dp.toPx()
                                            val gridActualSize = size.width.coerceAtMost(size.height)
                                            val gridTopY = (size.height - gridActualSize) / 2f
                                            val gridBottomY = gridTopY + gridActualSize
                                            val gridLeftX = (size.width - gridActualSize) / 2f
                                            val gridRightX = gridLeftX + gridActualSize

                                            drawLine(
                                                color = Color.Black.copy(alpha = 0.5f),
                                                start = Offset(x = center.x, y = gridTopY),
                                                end = Offset(x = center.x, y = gridBottomY),
                                                strokeWidth = strokeWidth
                                            )
                                            drawLine(
                                                color = Color.Black.copy(alpha = 0.5f),
                                                start = Offset(x = gridLeftX, y = center.y),
                                                end = Offset(x = gridRightX, y = center.y),
                                                strokeWidth = strokeWidth
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Box(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            FloatingActionButton(onClick = { paletteVisible = !paletteVisible }) {
                                Icon(Icons.Filled.ColorLens, "Colors")
                            }
                        }

                        AnimatedVisibility(
                            visible = paletteVisible,
                            modifier = Modifier.align(Alignment.BottomCenter),
                            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)),
                            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300))
                        ) {
                            Surface(modifier = Modifier.fillMaxHeight(0.5f), shadowElevation = 8.dp) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (toolMode == ToolMode.BRUSH) {
                                                Button(
                                                    onClick = {
                                                        toolMode = ToolMode.BRUSH
                                                        pickSourceArmed = false
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                ) { Text("Brush") }
                                            } else {
                                                OutlinedButton(
                                                    onClick = {
                                                        toolMode = ToolMode.BRUSH
                                                        pickSourceArmed = false
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                ) { Text("Brush") }
                                            }
                                            if (toolMode == ToolMode.REPLACE) {
                                                Button(
                                                    onClick = { toolMode = ToolMode.REPLACE },
                                                    modifier = Modifier.weight(1f)
                                                ) { Text("Replace") }
                                            } else {
                                                OutlinedButton(
                                                    onClick = { toolMode = ToolMode.REPLACE },
                                                    modifier = Modifier.weight(1f)
                                                ) { Text("Replace") }
                                            }
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Button(
                                                onClick = { pickSourceArmed = true },
                                                enabled = toolMode == ToolMode.REPLACE,
                                                modifier = Modifier.weight(1f)
                                            ) { Text("Pick Source") }
                                            Button(
                                                onClick = { showReplaceConfirmDialog = true },
                                                enabled = toolMode == ToolMode.REPLACE && replaceSourceColor != null,
                                                modifier = Modifier.weight(1f)
                                            ) { Text("Apply Replace") }
                                        }
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp)
                                        ) {
                                            Text("Mirror")
                                            val mirrorEnabled = toolMode == ToolMode.BRUSH
                                            val mirrorOptions = listOf(
                                                "Off" to SymmetryMode.NONE,
                                                "Horizontal" to SymmetryMode.HORIZONTAL,
                                                "Vertical" to SymmetryMode.VERTICAL,
                                                "Both" to SymmetryMode.QUADRANT
                                            )
                                            LazyRow(
                                                modifier = Modifier.padding(top = 8.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                items(mirrorOptions) { (label, mode) ->
                                                    val selected = symmetryMode == mode
                                                    if (selected) {
                                                        Button(
                                                            onClick = { symmetryMode = mode },
                                                            enabled = mirrorEnabled
                                                        ) { Text(label) }
                                                    } else {
                                                        OutlinedButton(
                                                            onClick = { symmetryMode = mode },
                                                            enabled = mirrorEnabled
                                                        ) { Text(label) }
                                                    }
                                                }
                                            }
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Grid lines")
                                            Spacer(Modifier.width(12.dp))
                                            Switch(
                                                checked = showGridLines,
                                                onCheckedChange = { showGridLines = it }
                                            )
                                        }
                                        val statusText = if (toolMode == ToolMode.BRUSH) {
                                            "Brush mode"
                                        } else {
                                            when {
                                                pickSourceArmed -> "Replace: tap a cell to pick source"
                                                replaceSourceColor == null -> "Replace: pick a source color"
                                                else -> "Replace: source set, choose target and apply"
                                            }
                                        }
                                        Text(
                                            statusText,
                                            modifier = Modifier.padding(top = 8.dp),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    TabRow(
                                        selectedTabIndex = selectedPaletteTab,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        listOf("Pastel", "Metallic", "Bright", "Custom").forEachIndexed { index, label ->
                                            Tab(
                                                selected = selectedPaletteTab == index,
                                                onClick = {
                                                    val currentHex = paletteHexColors[selectedColorIndex]
                                                    selectedPaletteTab = index
                                                    val nextPalette = when (index) {
                                                        0 -> pastelHexColors
                                                        1 -> metallicHexColors
                                                        2 -> brightHexColors
                                                        else -> customHexColors
                                                    }
                                                    selectedColorIndex = selectIndexForPalette(currentHex, nextPalette)
                                                },
                                                text = { Text(label, maxLines = 1) },
                                                modifier = Modifier.padding(vertical = 8.dp)
                                            )
                                        }
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Palette", style = MaterialTheme.typography.titleMedium)
                                        Button(onClick = {
                                            hexInput = paletteHexColors[selectedColorIndex]
                                            showEditHexDialog = true
                                        }) { Text("Edit Hex") }
                                    }
                                    ColorPalette(
                                        colorsHex = paletteHexColors,
                                        selectedIndex = selectedColorIndex,
                                        onColorSelected = { index ->
                                            selectedColorIndex = index
                                            paletteVisible = false
                                        }
                                    )
                                }
                            }
                        }

                        
                    }
                }
            }
        }
    }
}

private fun loadBitmapSafely(path: String): Bitmap? {
    return try {
        val file = File(path)
        if (file.exists()) BitmapFactory.decodeFile(path) else null
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
