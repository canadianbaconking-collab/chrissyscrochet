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
        canvas.drawRect(
            x.toFloat(),
            y.toFloat(),
            (x + scale).toFloat(),
            (y + scale).toFloat(),
            paint
        )
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

fun savePattern(context: Context, filename: String, pattern: List<Color>, gridSize: Int): Boolean {
    return try {
        val dir = File(context.filesDir, "patterns")
        if (!dir.exists()) dir.mkdirs()

        val file = File(dir, "$filename.txt")
        file.writeText(pattern.joinToString(",") { colorToHex(it) })

        val bitmap = createPatternBitmap(pattern, gridSize)
        val thumbnailFile = File(dir, "${filename}_thumb.png")
        FileOutputStream(thumbnailFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bitmap.recycle()
        true
    } catch (e: IOException) {
        e.printStackTrace()
        false
    }
}

data class RawPattern(val size: Int, val colors: List<Color>)

private fun deriveSquareSizeOrNull(count: Int): Int? {
    if (count <= 0) return null
    val root = kotlin.math.sqrt(count.toDouble()).toInt()
    return if (root * root == count) root else null
}

/**
 * Loads the pattern from disk and derives its grid size by sqrt(colorCount).
 * Returns null if the file is missing/corrupt/non-square.
 */
fun loadRawPattern(context: Context, filename: String): RawPattern? {
    return try {
        val dir = File(context.filesDir, "patterns")
        val file = File(dir, "$filename.txt")
        if (!file.exists()) return null

        val text = file.readText().trim()
        if (text.isEmpty()) return null

        val colors: List<Color> = text.split(",").map { hexToColor(it) }
        val size = deriveSquareSizeOrNull(colors.size) ?: return null

        RawPattern(size = size, colors = colors)
    } catch (e: IOException) {
        e.printStackTrace()
        null
    } catch (e: Exception) {
        // hex parse or other unexpected issues
        e.printStackTrace()
        null
    }
}

/**
 * Deterministic center transform:
 * - If dstSize > srcSize: center-place src into dst, padColor elsewhere.
 * - If dstSize < srcSize: crop centered dst window from src.
 *
 * Offset rule is FLOOR for odd differences (consistent + deterministic):
 *   padOffset = (dst - src) / 2
 *   cropStart = (src - dst) / 2
 */
fun transformPatternCenter(
    raw: RawPattern,
    dstSize: Int,
    padColor: Color = Color.White
): List<Color> {
    val srcSize = raw.size
    val src = raw.colors

    if (dstSize <= 0) return emptyList()
    if (srcSize <= 0) return emptyList()
    if (src.size != srcSize * srcSize) return emptyList()

    // Identity
    if (srcSize == dstSize) return src

    // Pad (dst bigger): place src centered into dst
    if (dstSize > srcSize) {
        val out = MutableList(dstSize * dstSize) { padColor }
        val off = (dstSize - srcSize) / 2  // floor
        for (y in 0 until srcSize) {
            for (x in 0 until srcSize) {
                val dstX = x + off
                val dstY = y + off
                out[dstY * dstSize + dstX] = src[y * srcSize + x]
            }
        }
        return out
    }

    // Crop (dst smaller): take centered dst window from src
    run {
        val out = MutableList(dstSize * dstSize) { padColor }
        val start = (srcSize - dstSize) / 2 // floor
        for (y in 0 until dstSize) {
            for (x in 0 until dstSize) {
                val srcX = x + start
                val srcY = y + start
                out[y * dstSize + x] = src[srcY * srcSize + srcX]
            }
        }
        return out
    }
}


fun loadPattern(context: Context, filename: String, gridSize: Int): List<Color> {
    return try {
        val dir = File(context.filesDir, "patterns")
        val file = File(dir, "$filename.txt")
        if (!file.exists()) return emptyList()

        val colors: List<Color> = file.readText().split(",").map { hexToColor(it) }
        if (colors.size == gridSize * gridSize) colors else emptyList()
    } catch (e: IOException) {
        e.printStackTrace()
        emptyList()
    }
}

fun getSavedPatterns(context: Context): List<SavedPattern> {
    val dir = File(context.filesDir, "patterns")
    if (!dir.exists()) return emptyList()

    val files: Array<File> = dir.listFiles() ?: return emptyList()
    return files.filter { it.extension == "txt" }.map { file: File ->
        val name = file.nameWithoutExtension
        val thumbnailPath = File(dir, "${name}_thumb.png").absolutePath
        SavedPattern(name, thumbnailPath)
    }
}

fun exportPatternToImage(context: Context, filename: String, pattern: List<Color>, gridSize: Int): Boolean {
    return try {
        val bitmap = createPatternBitmap(pattern, gridSize, scale = 10)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$filename.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }

            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                context.contentResolver.openOutputStream(it).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out!!)
                }
            }
        } else {
            @Suppress("DEPRECATION")
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val file = File(picturesDir, "$filename.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        }
        bitmap.recycle()
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}