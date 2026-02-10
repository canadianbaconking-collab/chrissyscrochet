package com.example.myapplication

import android.content.Context.MODE_PRIVATE
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.myapplication.ui.UiColors
import com.example.myapplication.ui.theme.MyApplicationTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlin.math.sqrt

enum class SymmetryMode { NONE, VERTICAL, HORIZONTAL, QUADRANT }
enum class ToolMode { BRUSH, REPLACE }
data class SizeMismatchState(val raw: RawPattern, val currentSize: Int)
private const val DEBUG_LAYOUT = false

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var pastelHexColors by remember { mutableStateOf(pastelPaletteHexes) }
                var metallicHexColors by remember { mutableStateOf(metallicPaletteHexes) }
                var brightHexColors by remember { mutableStateOf(brightPaletteHexes) }
                var customHexColors by remember { mutableStateOf(customPaletteHexes) }
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
                var showRenameTabsDialog by remember { mutableStateOf(false) }
                var hexInput by remember { mutableStateOf("") }
                var showLoadConfirmDialog by remember { mutableStateOf(false) }
                var pendingLoadName by remember { mutableStateOf<String?>(null) }
                var pendingDeleteName by remember { mutableStateOf<String?>(null) }
                var pendingMismatch by remember { mutableStateOf<SizeMismatchState?>(null) }
                var showNewConfirmDialog by remember { mutableStateOf(false) }
                var showDeletePatternDialog by remember { mutableStateOf(false) }
                var toolMode by remember { mutableStateOf(ToolMode.BRUSH) }
                var replaceSourceColor by remember { mutableStateOf<Color?>(null) }
                var pickSourceArmed by remember { mutableStateOf(false) }
                var showReplaceConfirmDialog by remember { mutableStateOf(false) }
                var filename by remember { mutableStateOf("") }
                var savedPatternsVersion by remember { mutableIntStateOf(0) }
                val context = LocalContext.current

                var scale by remember { mutableStateOf(1f) }
                var offset by remember { mutableStateOf(Offset.Zero) }
                var layoutSize by remember { mutableStateOf(IntSize.Zero) }
                var symmetryMode by remember { mutableStateOf(SymmetryMode.NONE) }
                var showMirrorOptions by remember { mutableStateOf(false) }
                var showComposeSplashOverlay by remember { mutableStateOf(true) }
                val paletteTabNamePrefs = remember(context) {
                    context.getSharedPreferences("palette_tab_names", MODE_PRIVATE)
                }
                var paletteLabels by remember {
                    mutableStateOf(loadPaletteTabNames(paletteTabNamePrefs))
                }
                var renameTabInputs by remember { mutableStateOf(paletteLabels) }

                LaunchedEffect(Unit) {
                    delay(420)
                    showComposeSplashOverlay = false
                }

                fun selectIndexForPalette(currentHex: String, palette: List<String>): Int {
                    val index = palette.indexOf(currentHex)
                    return if (index >= 0) index else 0
                }

                fun deriveGridSizeFromPattern(colors: List<Color>): Int {
                    val count = colors.size
                    if (count <= 0) return gridSize
                    val root = sqrt(count.toDouble()).toInt()
                    return if (root * root == count) root else gridSize
                }

                fun clearPendingLoadState() {
                    pendingLoadName = null
                    pendingMismatch = null
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
                    filename = ""
                }

                fun saveCurrentPattern(nameToSave: String): Boolean {
                    val savedName = savePattern(context, nameToSave, pattern, gridSize)
                    if (savedName != null) {
                        filename = savedName
                        savedPatternsVersion++
                        Toast.makeText(context, "Saved as $savedName", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to save", Toast.LENGTH_SHORT).show()
                    }
                    return savedName != null
                }

                fun exportCurrentPattern() {
                    val baseName = filename.trim().ifBlank {
                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                        "pattern_$timestamp"
                    }
                    val exported = exportPatternToJpg(context, baseName, pattern, gridSize)
                    if (exported) {
                        Toast.makeText(context, "Exported to Photos", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to export", Toast.LENGTH_SHORT).show()
                    }
                }

                fun deletePatternByName(name: String) {
                    val deleted = deleteSavedPattern(context, name)
                    if (deleted) {
                        savedPatternsVersion++
                        if (filename == name) {
                            filename = ""
                        }
                        Toast.makeText(context, "Deleted $name", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to delete $name", Toast.LENGTH_SHORT).show()
                    }
                }

                fun loadPatternByName(name: String) {
                    pendingMismatch = null
                    val raw = loadRawPattern(context, name)
                    if (raw == null) {
                        Toast.makeText(context, "Failed to load pattern", Toast.LENGTH_SHORT).show()
                        clearPendingLoadState()
                        return
                    }

                    if (raw.size == gridSize) {
                        updatePattern(raw.colors)
                        Toast.makeText(context, "Pattern loaded", Toast.LENGTH_SHORT).show()
                        filename = name
                        clearPendingLoadState()
                        return
                    }

                    pendingMismatch = SizeMismatchState(raw = raw, currentSize = gridSize)
                }

                if (showLoadConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = {
                            showLoadConfirmDialog = false
                            clearPendingLoadState()
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
                                clearPendingLoadState()
                            }) { Text("Cancel") }
                        }
                    )
                }

                if (pendingMismatch != null) {
                    val mismatch = pendingMismatch!!
                    AlertDialog(
                        onDismissRequest = {
                            clearPendingLoadState()
                        },
                        title = { Text("Pattern size mismatch") },
                        text = {
                            Text("Loaded ${mismatch.raw.size}x${mismatch.raw.size}, current ${mismatch.currentSize}x${mismatch.currentSize}")
                        },
                        confirmButton = {
                            Button(onClick = {
                                val raw = mismatch.raw
                                val loadedName = pendingLoadName
                                gridSize = raw.size
                                updatePattern(raw.colors)
                                scale = 1f
                                offset = Offset.Zero
                                Toast.makeText(context, "Pattern loaded", Toast.LENGTH_SHORT).show()
                                if (!loadedName.isNullOrBlank()) {
                                    filename = loadedName
                                }
                                clearPendingLoadState()
                            }) { Text("Switch to ${mismatch.raw.size}x${mismatch.raw.size} and load") }
                        },
                        dismissButton = {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = {
                                    val loadedName = pendingLoadName
                                    val transformed = transformPatternCenter(
                                        mismatch.raw,
                                        mismatch.currentSize,
                                        Color.White
                                    )
                                    updatePattern(transformed)
                                    Toast.makeText(context, "Pattern loaded", Toast.LENGTH_SHORT).show()
                                    if (!loadedName.isNullOrBlank()) {
                                        filename = loadedName
                                    }
                                    clearPendingLoadState()
                                }) { Text("Center on ${mismatch.currentSize}x${mismatch.currentSize}") }
                                TextButton(onClick = { clearPendingLoadState() }) { Text("Cancel") }
                            }
                        }
                    )
                }

                if (showDeletePatternDialog) {
                    val nameToDelete = pendingDeleteName
                    AlertDialog(
                        onDismissRequest = {
                            showDeletePatternDialog = false
                            pendingDeleteName = null
                        },
                        title = { Text("Delete pattern?") },
                        text = { Text("Delete ${nameToDelete.orEmpty()}?") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val target = pendingDeleteName
                                    showDeletePatternDialog = false
                                    pendingDeleteName = null
                                    if (!target.isNullOrBlank()) {
                                        deletePatternByName(target)
                                    }
                                }
                            ) { Text("Delete") }
                        },
                        dismissButton = {
                            Button(
                                onClick = {
                                    showDeletePatternDialog = false
                                    pendingDeleteName = null
                                }
                            ) { Text("Cancel") }
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

                if (showRenameTabsDialog) {
                    val sanitizedNames = renameTabInputs.map { sanitizePaletteTabName(it) }
                    val canSave = sanitizedNames.all { it != null }
                    AlertDialog(
                        onDismissRequest = { showRenameTabsDialog = false },
                        title = { Text("Rename palette tabs") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                renameTabInputs.forEachIndexed { index, label ->
                                    val isFieldValid = sanitizePaletteTabName(label) != null
                                    TextField(
                                        value = label,
                                        onValueChange = { updatedValue ->
                                            val updated = renameTabInputs.toMutableList()
                                            updated[index] = updatedValue.take(14)
                                            renameTabInputs = updated
                                        },
                                        label = { Text("Tab ${index + 1}") },
                                        singleLine = true,
                                        isError = !isFieldValid
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val namesToSave = sanitizedNames.mapNotNull { it }
                                    if (namesToSave.size == defaultPaletteTabNames.size) {
                                        val savedNames = savePaletteTabNames(paletteTabNamePrefs, namesToSave)
                                        paletteLabels = savedNames
                                        renameTabInputs = savedNames
                                        showRenameTabsDialog = false
                                    }
                                },
                                enabled = canSave
                            ) { Text("Save") }
                        },
                        dismissButton = {
                            Button(onClick = { showRenameTabsDialog = false }) { Text("Cancel") }
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
                        title = { Text("Name Pattern") },
                        text = {
                            TextField(
                                value = filename,
                                onValueChange = { filename = it },
                                label = { Text("Pattern name") },
                                singleLine = true
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val enteredName = filename.trim()
                                    if (enteredName.isNotBlank()) {
                                        if (saveCurrentPattern(enteredName)) {
                                            showSaveDialog = false
                                        }
                                    }
                                }
                            ) { Text("Save") }
                        },
                        dismissButton = {
                            Button(onClick = { showSaveDialog = false }) { Text("Cancel") }
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
                    val savedPatterns = remember(showLoadDialog, savedPatternsVersion) {
                        getSavedPatterns(context)
                    }
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
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(end = 2.dp),
                                                        horizontalArrangement = Arrangement.End
                                                    ) {
                                                        IconButton(
                                                            onClick = {
                                                                pendingDeleteName = savedPattern.name
                                                                showDeletePatternDialog = true
                                                            },
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Filled.Close,
                                                                contentDescription = "Delete ${savedPattern.name}"
                                                            )
                                                        }
                                                    }
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

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = UiColors.AppBg
                ) {
                    Scaffold(
                        containerColor = UiColors.AppBg,
                        contentColor = UiColors.TextPrimary
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .padding(innerPadding)
                                .fillMaxSize()
                                .then(
                                    if (DEBUG_LAYOUT) {
                                        Modifier.background(Color.Green.copy(alpha = 0.12f))
                                    } else {
                                        Modifier
                                    }
                                )
                        ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            TopAppBar(
                                title = {},
                                actions = {
                                    IconButton(onClick = { showNewConfirmDialog = true }) {
                                        Icon(Icons.Filled.Add, contentDescription = "New")
                                    }
                                    IconButton(onClick = {
                                        val currentName = filename.trim()
                                        if (currentName.isBlank()) {
                                            showSaveDialog = true
                                        } else {
                                            saveCurrentPattern(currentName)
                                        }
                                    }) {
                                        Icon(Icons.Filled.Save, contentDescription = "Save")
                                    }
                                    IconButton(onClick = { exportCurrentPattern() }) {
                                        Icon(Icons.Filled.Download, contentDescription = "Export JPG")
                                    }
                                    IconButton(onClick = { showLoadDialog = true }) {
                                        Icon(Icons.Filled.FolderOpen, contentDescription = "Load")
                                    }
                                    IconButton(
                                        onClick = {
                                            if (historyIndex > 0) {
                                                historyIndex--
                                                val newSize = deriveGridSizeFromPattern(history[historyIndex])
                                                if (newSize != gridSize) {
                                                    gridSize = newSize
                                                    scale = 1f
                                                    offset = Offset.Zero
                                                }
                                            }
                                        },
                                        enabled = historyIndex > 0
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                                    }
                                    IconButton(
                                        onClick = {
                                            if (historyIndex < history.lastIndex) {
                                                historyIndex++
                                                val newSize = deriveGridSizeFromPattern(history[historyIndex])
                                                if (newSize != gridSize) {
                                                    gridSize = newSize
                                                    scale = 1f
                                                    offset = Offset.Zero
                                                }
                                            }
                                        },
                                        enabled = historyIndex < history.lastIndex
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                                    }
                                }
                            )
                            
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
                            val latestPattern by rememberUpdatedState(pattern)
                            val latestToolMode by rememberUpdatedState(toolMode)
                            val latestSelectedColor by rememberUpdatedState(selectedColor)
                            val latestPickSourceArmed by rememberUpdatedState(pickSourceArmed)
                            val latestGridSize by rememberUpdatedState(gridSize)
                            val latestScale by rememberUpdatedState(scale)
                            val latestOffset by rememberUpdatedState(offset)
                            val latestLayoutSize by rememberUpdatedState(layoutSize)
                            val latestOnColorChange by rememberUpdatedState(onColorChange)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(24.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(UiColors.SurfaceBg)
                                        .then(
                                            if (DEBUG_LAYOUT) {
                                                Modifier.background(Color.Red.copy(alpha = 0.12f))
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .clip(RectangleShape)
                                        .onSizeChanged { layoutSize = it }
                                        .pointerInput(Unit) {
                                            detectTransformGestures { _, pan, zoom, _ ->
                                                scale = (scale * zoom).coerceIn(0.5f, 5f)
                                                offset += pan
                                            }
                                        }
                                        .pointerInput(gridSize) {
                                            var lastIndex: Int? = null
                                            detectDragGestures(
                                                onDragEnd = { lastIndex = null },
                                                onDragCancel = { lastIndex = null }
                                            ) { change, _ ->
                                                val position = change.position
                                                val currentLayoutSize = latestLayoutSize
                                                val currentGridSize = latestGridSize
                                                if (currentLayoutSize.width <= 0 || currentLayoutSize.height <= 0) {
                                                    change.consume()
                                                    return@detectDragGestures
                                                }
                                                val gridLayoutSize = currentLayoutSize.width.coerceAtMost(currentLayoutSize.height).toFloat()
                                                val currentScale = latestScale
                                                val currentOffset = latestOffset

                                                val transformedX = (position.x - currentOffset.x - (currentLayoutSize.width - gridLayoutSize * currentScale) / 2f) / currentScale
                                                val transformedY = (position.y - currentOffset.y - (currentLayoutSize.height - gridLayoutSize * currentScale) / 2f) / currentScale

                                                val col = (transformedX / (gridLayoutSize / currentGridSize)).toInt()
                                                val row = (transformedY / (gridLayoutSize / currentGridSize)).toInt()

                                                if (col in 0 until currentGridSize && row in 0 until currentGridSize) {
                                                    val index = row * currentGridSize + col
                                                    if (index in latestPattern.indices && index != lastIndex) {
                                                        if (latestToolMode == ToolMode.BRUSH) {
                                                            latestOnColorChange(index, latestSelectedColor)
                                                            lastIndex = index
                                                        }
                                                    }
                                                }
                                                change.consume()
                                            }
                                        }
                                        .pointerInput(gridSize) {
                                            detectTapGestures { tapOffset ->
                                                val currentLayoutSize = latestLayoutSize
                                                val currentGridSize = latestGridSize
                                                if (currentLayoutSize.width <= 0 || currentLayoutSize.height <= 0) {
                                                    return@detectTapGestures
                                                }
                                                val gridLayoutSize = currentLayoutSize.width.coerceAtMost(currentLayoutSize.height).toFloat()
                                                val currentScale = latestScale
                                                val currentOffset = latestOffset

                                                val transformedX = (tapOffset.x - currentOffset.x - (currentLayoutSize.width - gridLayoutSize * currentScale) / 2f) / currentScale
                                                val transformedY = (tapOffset.y - currentOffset.y - (currentLayoutSize.height - gridLayoutSize * currentScale) / 2f) / currentScale

                                                val col = (transformedX / (gridLayoutSize / currentGridSize)).toInt()
                                                val row = (transformedY / (gridLayoutSize / currentGridSize)).toInt()

                                                if (col in 0 until currentGridSize && row in 0 until currentGridSize) {
                                                    val index = row * currentGridSize + col
                                                    if (index in latestPattern.indices) {
                                                        if (latestToolMode == ToolMode.BRUSH) {
                                                            latestOnColorChange(index, latestSelectedColor)
                                                        } else if (latestToolMode == ToolMode.REPLACE && latestPickSourceArmed) {
                                                            replaceSourceColor = latestPattern[index]
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
                                        selectedColor = selectedColor
                                    )

                                    val axisColor = UiColors.Crosshair
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val strokeWidth = 2.dp.toPx()
                                        val gridBaseSize = size.width.coerceAtMost(size.height)
                                        val gridActualSize = gridBaseSize * scale
                                        val gridLeftX = (size.width - gridActualSize) / 2f + offset.x
                                        val gridTopY = (size.height - gridActualSize) / 2f + offset.y
                                        val gridRightX = gridLeftX + gridActualSize
                                        val gridBottomY = gridTopY + gridActualSize
                                        val centerX = (gridLeftX + gridRightX) / 2f
                                        val centerY = (gridTopY + gridBottomY) / 2f

                                        clipRect(
                                            left = gridLeftX,
                                            top = gridTopY,
                                            right = gridRightX,
                                            bottom = gridBottomY
                                        ) {
                                            drawLine(
                                                color = axisColor,
                                                start = Offset(x = centerX, y = gridTopY),
                                                end = Offset(x = centerX, y = gridBottomY),
                                                strokeWidth = strokeWidth
                                            )
                                            drawLine(
                                                color = axisColor,
                                                start = Offset(x = gridLeftX, y = centerY),
                                                end = Offset(x = gridRightX, y = centerY),
                                                strokeWidth = strokeWidth
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = toolMode == ToolMode.REPLACE,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 12.dp, vertical = 88.dp),
                            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(250)),
                            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(250))
                        ) {
                            val statusText = when {
                                pickSourceArmed -> "Tap a cell to pick source"
                                replaceSourceColor == null -> "Pick a source color"
                                else -> "Source set, choose target and apply"
                            }
                            Surface(shadowElevation = 6.dp, tonalElevation = 3.dp) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(statusText, style = MaterialTheme.typography.bodySmall)
                                    Row(
                                        modifier = Modifier.padding(top = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(onClick = { pickSourceArmed = true }) { Text("Pick Source") }
                                        Button(
                                            onClick = { showReplaceConfirmDialog = true },
                                            enabled = replaceSourceColor != null
                                        ) { Text("Apply Replace") }
                                    }
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = showMirrorOptions && toolMode == ToolMode.BRUSH,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 12.dp, vertical = 88.dp),
                            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(250)),
                            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(250))
                        ) {
                            val mirrorOptions = listOf(
                                "Off" to SymmetryMode.NONE,
                                "H" to SymmetryMode.HORIZONTAL,
                                "V" to SymmetryMode.VERTICAL,
                                "Both" to SymmetryMode.QUADRANT
                            )
                            Surface(shadowElevation = 6.dp, tonalElevation = 3.dp) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    mirrorOptions.forEach { (label, mode) ->
                                        FilterChip(
                                            selected = symmetryMode == mode,
                                            onClick = { symmetryMode = mode },
                                            label = { Text(label) }
                                        )
                                    }
                                }
                            }
                        }

                        NavigationBar(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .then(
                                    if (DEBUG_LAYOUT) {
                                        Modifier.background(Color.Blue.copy(alpha = 0.12f))
                                    } else {
                                        Modifier
                                    }
                                )
                        ) {
                            NavigationBarItem(
                                selected = paletteVisible,
                                onClick = {
                                    paletteVisible = true
                                    showMirrorOptions = false
                                },
                                icon = { Icon(Icons.Filled.ColorLens, contentDescription = "Palette") },
                                label = { Text("Palette") }
                            )
                            NavigationBarItem(
                                selected = toolMode == ToolMode.REPLACE,
                                onClick = {
                                    if (toolMode == ToolMode.REPLACE) {
                                        toolMode = ToolMode.BRUSH
                                        pickSourceArmed = false
                                    } else {
                                        toolMode = ToolMode.REPLACE
                                        showMirrorOptions = false
                                    }
                                },
                                icon = { Icon(Icons.Filled.SwapHoriz, contentDescription = "Replace") },
                                label = { Text("Replace") }
                            )
                            NavigationBarItem(
                                selected = showMirrorOptions,
                                onClick = {
                                    if (toolMode == ToolMode.BRUSH) {
                                        showMirrorOptions = !showMirrorOptions
                                    }
                                },
                                enabled = toolMode == ToolMode.BRUSH,
                                icon = { Icon(Icons.Filled.Flip, contentDescription = "Mirror") },
                                label = { Text("Mirror") }
                            )
                        }

                        AnimatedVisibility(
                            visible = showComposeSplashOverlay,
                            modifier = Modifier.fillMaxSize(),
                            enter = fadeIn(animationSpec = tween(80)),
                            exit = fadeOut(animationSpec = tween(180))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(UiColors.AppBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.cc_splash),
                                    contentDescription = "Splash Branding",
                                    modifier = Modifier.fillMaxWidth(0.9f),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                }

                    if (paletteVisible) {
                        ModalBottomSheet(onDismissRequest = { paletteVisible = false }) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.9f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Palette", style = MaterialTheme.typography.titleMedium)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = {
                                        renameTabInputs = paletteLabels
                                        showRenameTabsDialog = true
                                    }) {
                                        Text("Rename tabs")
                                    }
                                    IconButton(onClick = { paletteVisible = false }) {
                                        Icon(Icons.Filled.Close, contentDescription = "Close Palette")
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                LazyRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(paletteLabels.size) { index ->
                                        FilterChip(
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
                                            label = {
                                                Text(
                                                    paletteLabels[index],
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = {
                                            hexInput = paletteHexColors[selectedColorIndex]
                                            showEditHexDialog = true
                                        },
                                        modifier = Modifier.heightIn(min = 36.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) { Text("Edit Hex") }
                                }

                                ColorPalette(
                                    colorsHex = paletteHexColors,
                                    selectedIndex = selectedColorIndex,
                                    onColorSelected = { index ->
                                        selectedColorIndex = index
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
