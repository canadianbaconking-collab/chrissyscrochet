package com.example.myapplication

import android.content.SharedPreferences

private const val PALETTE_TAB_PREF_MAX_LENGTH = 14
private const val PALETTE_TAB_NAME_KEY_PREFIX = "palette_tab_name_"

val defaultPaletteTabNames = defaultPaletteTabs.map { it.name }

fun paletteTabNameKey(index: Int): String = "$PALETTE_TAB_NAME_KEY_PREFIX$index"

fun sanitizePaletteTabName(input: String, maxLength: Int = PALETTE_TAB_PREF_MAX_LENGTH): String? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return null
    return trimmed.take(maxLength)
}

internal fun loadPaletteTabNames(readValue: (String) -> String?): List<String> {
    return defaultPaletteTabNames.mapIndexed { index, defaultName ->
        sanitizePaletteTabName(readValue(paletteTabNameKey(index)) ?: defaultName) ?: defaultName
    }
}

fun loadPaletteTabNames(sharedPreferences: SharedPreferences): List<String> {
    return loadPaletteTabNames { key -> sharedPreferences.getString(key, null) }
}

internal fun savePaletteTabNames(names: List<String>, writeValue: (String, String) -> Unit): List<String> {
    val fallback = names + defaultPaletteTabNames.drop(names.size)
    val normalized = defaultPaletteTabNames.indices.map { index ->
        sanitizePaletteTabName(fallback[index]) ?: defaultPaletteTabNames[index]
    }
    normalized.forEachIndexed { index, name ->
        writeValue(paletteTabNameKey(index), name)
    }
    return normalized
}

fun savePaletteTabNames(sharedPreferences: SharedPreferences, names: List<String>): List<String> {
    val editor = sharedPreferences.edit()
    val saved = savePaletteTabNames(names) { key, value ->
        editor.putString(key, value)
    }
    editor.apply()
    return saved
}
