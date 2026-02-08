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

private fun hexToColor(hex: String): Color {
    val cleanHex = hex.removePrefix("#")
    return Color(android.graphics.Color.parseColor("#$cleanHex"))
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

    return dir.listFiles()?.filter { it.extension == "txt" }?.map { file ->
        val name = file.nameWithoutExtension
        val thumbnailPath = File(dir, "${name}_thumb.png").absolutePath
        SavedPattern(name, thumbnailPath)
    } ?: emptyList()
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