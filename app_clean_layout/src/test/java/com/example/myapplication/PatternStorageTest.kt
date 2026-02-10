package com.example.myapplication

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class PatternStorageTest {
    @Test
    fun nextAvailablePatternName_returnsBaseWhenUnused() {
        val out = nextAvailablePatternName("Rose", setOf("Tulip", "Lily"))
        assertEquals("Rose", out)
    }

    @Test
    fun nextAvailablePatternName_appendsNumericSuffix() {
        val out = nextAvailablePatternName(
            "Rose",
            setOf("Rose", "Rose (1)", "Rose (2)", "Tulip")
        )
        assertEquals("Rose (3)", out)
    }

    @Test
    fun transformPatternCenterPadsFrom2To4() {
        val src = listOf(
            Color(0xFF000001L), Color(0xFF000002L),
            Color(0xFF000003L), Color(0xFF000004L)
        )
        val raw = RawPattern(size = 2, colors = src)
        val out = transformPatternCenter(raw, 4, Color.White)

        assertEquals(16, out.size)
        assertEquals(Color.White, out[0])
        assertEquals(Color.White, out[3])

        assertEquals(Color(0xFF000001L), out[1 * 4 + 1])
        assertEquals(Color(0xFF000002L), out[1 * 4 + 2])
        assertEquals(Color(0xFF000003L), out[2 * 4 + 1])
        assertEquals(Color(0xFF000004L), out[2 * 4 + 2])
    }

    @Test
    fun transformPatternCenterCropsFrom5To3() {
        val src = List(25) { index -> Color(0xFF000000L + index.toLong()) }
        val raw = RawPattern(size = 5, colors = src)
        val out = transformPatternCenter(raw, 3, Color.White)

        assertEquals(9, out.size)
        // Center 3x3 window starts at (1,1) in 5x5
        assertEquals(src[1 * 5 + 1], out[0])
        assertEquals(src[1 * 5 + 3], out[2])
        assertEquals(src[3 * 5 + 1], out[6])
        assertEquals(src[3 * 5 + 3], out[8])
    }
}
