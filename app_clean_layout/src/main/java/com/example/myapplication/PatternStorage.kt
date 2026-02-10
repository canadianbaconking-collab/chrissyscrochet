package com.example.myapplication

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.graphics.Color
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

data class SavedPattern(val name: String, val thumbnailUrl: String)
data class RawPattern(val size: Int, val colors: List<Color>)

sealed class LoadRawPatternResult {
    data class Success(val raw: RawPattern) : LoadRawPatternResult()
    data class Error(val message: String) : LoadRawPatternResult()
}

private fun createPatternBitmap(pattern: List<Color>, gridSize: Int, scale: Int = 1): Bitmap {
    val size = gridSize * scale
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    pattern.forEachIndexed { index, color ->
        val x = (index % gridSize) * scale
        val y = (index / gridSize) * scale
        val paint = android.graphics.Paint().apply {
            this.color = android.graphics.Color.argb(
                (color.alpha * 255).toInt(),
                (color.red * 255).toInt(),
                (color.green * 255).toInt(),
                (color.blue * 255).toInt()
            )
        }
        canvas.drawRect(x.toFloat(), y.toFloat(), (x + scale).toFloat(), (y + scale).toFloat(), paint)
    }
    return bitmap
}

private fun colorToHex(color: Color): String {
    return String.format(
        "#%02X%02X%02X%02X",
        (color.alpha * 255).toInt(),
        (color.red * 255).toInt(),
        (color.green * 255).toInt(),
        (color.blue * 255).toInt()
    )
}

fun nextAvailablePatternName(requestedName: String, existingNames: Set<String>): String {
    val trimmed = requestedName.trim()
    if (trimmed.isEmpty()) return requestedName
    if (!existingNames.contains(trimmed)) return trimmed
    var suffix = 1
    while (true) {
        val candidate = "$trimmed ($suffix)"
        if (!existingNames.contains(candidate)) return candidate
        suffix++
    }
}

fun savePattern(context: Context, filename: String, pattern: List<Color>, gridSize: Int): String? {
    return try {
        val dir = File(context.filesDir, "patterns")
        if (!dir.exists()) dir.mkdirs()
        val existingNames = (dir.listFiles() ?: emptyArray())
            .filter { it.extension == "txt" }
            .map { it.nameWithoutExtension }
            .toSet()
        val actualName = nextAvailablePatternName(filename, existingNames)
        val file = File(dir, "$actualName.txt")
        file.writeText(pattern.joinToString(",") { colorToHex(it) })

        val bitmap = createPatternBitmap(pattern, gridSize)
        val thumbnailFile = File(dir, "${actualName}_thumb.png")
        FileOutputStream(thumbnailFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bitmap.recycle()
        actualName
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}

private fun deriveSquareSizeOrNull(count: Int): Int? {
    if (count <= 0) return null
    val root = kotlin.math.sqrt(count.toDouble()).toInt()
    return if (root * root == count) root else null
}

private fun parseHexStrict(value: String): Color? {
    val normalized = normalizeHexInput(value) ?: return null
    val hex = normalized.removePrefix("#")
    val argb = if (hex.length == 6) "FF$hex" else hex
    return runCatching { Color(argb.toLong(16)) }.getOrNull()
}

fun loadRawPatternResult(context: Context, filename: String): LoadRawPatternResult {
    return try {
        val dir = File(context.filesDir, "patterns")
        val file = File(dir, "$filename.txt")
        val text = file.readText().trim()
        if (text.isEmpty()) {
            return LoadRawPatternResult.Error("Could not load pattern. Pattern is empty.")
        }
        val colors = text.split(",").map { token ->
            parseHexStrict(token) ?: return LoadRawPatternResult.Error("Could not load pattern. Invalid color data.")
        }
        val size = deriveSquareSizeOrNull(colors.size)
            ?: return LoadRawPatternResult.Error("Could not load pattern. Pattern is not square.")
        LoadRawPatternResult.Success(RawPattern(size = size, colors = colors))
    } catch (e: IOException) {
        e.printStackTrace()
        LoadRawPatternResult.Error("Could not load pattern. Read failed.")
    } catch (e: Exception) {
        e.printStackTrace()
        LoadRawPatternResult.Error("Could not load pattern. Read failed.")
    }
}

fun loadRawPattern(context: Context, filename: String): RawPattern? {
    return when (val result = loadRawPatternResult(context, filename)) {
        is LoadRawPatternResult.Success -> result.raw
        is LoadRawPatternResult.Error -> null
    }
}

fun transformPatternCenter(raw: RawPattern, dstSize: Int, padColor: Color = Color.White): List<Color> {
    val srcSize = raw.size
    val src = raw.colors
    if (dstSize <= 0) return emptyList()
    if (srcSize <= 0) return emptyList()
    if (src.size != srcSize * srcSize) return emptyList()
    if (srcSize == dstSize) return src

    if (dstSize > srcSize) {
        val out = MutableList(dstSize * dstSize) { padColor }
        val off = (dstSize - srcSize) / 2
        for (y in 0 until srcSize) {
            for (x in 0 until srcSize) {
                val dstX = x + off
                val dstY = y + off
                out[dstY * dstSize + dstX] = src[y * srcSize + x]
            }
        }
        return out
    }

    val out = MutableList(dstSize * dstSize) { padColor }
    val start = (srcSize - dstSize) / 2
    for (y in 0 until dstSize) {
        for (x in 0 until dstSize) {
            val srcX = x + start
            val srcY = y + start
            out[y * dstSize + x] = src[srcY * srcSize + srcX]
        }
    }
    return out
}

fun loadPattern(context: Context, filename: String, gridSize: Int): List<Color> {
    return try {
        val dir = File(context.filesDir, "patterns")
        val file = File(dir, "$filename.txt")
        if (!file.exists()) return emptyList()
        val colors = file.readText().split(",").map { hexToColor(it) }
        if (colors.size == gridSize * gridSize) colors else emptyList()
    } catch (e: IOException) {
        e.printStackTrace()
        emptyList()
    }
}

fun getSavedPatterns(context: Context): List<SavedPattern> {
    val dir = File(context.filesDir, "patterns")
    if (!dir.exists()) return emptyList()
    val files = dir.listFiles() ?: return emptyList()
    return files.filter { it.extension == "txt" }.map { file ->
        val name = file.nameWithoutExtension
        val thumbnailPath = File(dir, "${name}_thumb.png").absolutePath
        SavedPattern(name, thumbnailPath)
    }
}

fun deleteSavedPattern(context: Context, filename: String): Boolean {
    val dir = File(context.filesDir, "patterns")
    if (!dir.exists()) return false
    val patternFile = File(dir, "$filename.txt")
    val thumbFile = File(dir, "${filename}_thumb.png")
    val patternDeletedOrMissing = !patternFile.exists() || patternFile.delete()
    val thumbDeletedOrMissing = !thumbFile.exists() || thumbFile.delete()
    return patternDeletedOrMissing && thumbDeletedOrMissing
}

private fun exportNameExists(context: Context, baseName: String): Boolean {
    val displayName = "$baseName.jpg"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val selection = "${MediaStore.Images.Media.DISPLAY_NAME} = ? AND ${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
        val args = arrayOf(displayName, "${Environment.DIRECTORY_PICTURES}%")
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            args,
            null
        )?.use { cursor ->
            return cursor.count > 0
        }
        return false
    }

    @Suppress("DEPRECATION")
    val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    return File(picturesDir, displayName).exists()
}

private fun nextAvailableExportName(context: Context, baseName: String): String {
    val trimmed = baseName.trim().ifEmpty { "pattern" }
    if (!exportNameExists(context, trimmed)) return trimmed
    var suffix = 1
    while (true) {
        val candidate = "${trimmed}_$suffix"
        if (!exportNameExists(context, candidate)) return candidate
        suffix++
    }
}

fun exportPatternToJpg(context: Context, filename: String, pattern: List<Color>, gridSize: Int): Boolean {
    return try {
        val actualName = nextAvailableExportName(context, filename)
        val bitmap = createPatternBitmap(pattern, gridSize, scale = 10)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$actualName.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                context.contentResolver.openOutputStream(it).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out!!)
                }
            } ?: return false
        } else {
            @Suppress("DEPRECATION")
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val file = File(picturesDir, "$actualName.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
        }
        bitmap.recycle()
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
