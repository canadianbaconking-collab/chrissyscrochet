package com.example.myapplication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PaletteTabPrefsTest {

    @Test
    fun loadPaletteTabNames_returnsDefaults_whenStorageEmpty() {
        val loaded = loadPaletteTabNames { _ -> null }
        assertEquals(defaultPaletteTabNames, loaded)
    }

    @Test
    fun saveThenLoadPaletteTabNames_roundTripsSanitizedValues() {
        val storage = mutableMapOf<String, String>()
        val input = listOf("  Soft  ", "Metallic Plus", "BrightBrightBright", "  ")

        savePaletteTabNames(input) { key, value ->
            storage[key] = value
        }
        val loaded = loadPaletteTabNames { key -> storage[key] }

        assertEquals("Soft", loaded[0])
        assertEquals("Metallic Plus", loaded[1])
        assertEquals("BrightBrightBr", loaded[2])
        assertEquals(defaultPaletteTabNames[3], loaded[3])
    }

    @Test
    fun sanitizePaletteTabName_rejectsBlank() {
        assertNull(sanitizePaletteTabName("   "))
    }
}
