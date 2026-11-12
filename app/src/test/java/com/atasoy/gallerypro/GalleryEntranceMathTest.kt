package com.atasoy.gallerypro

import com.atasoy.gallerypro.feature.library.GalleryEntrance
import com.atasoy.gallerypro.feature.library.GalleryEntranceMath
import org.junit.Assert.assertEquals
import org.junit.Test

class GalleryEntranceMathTest {
    @Test fun tappedCellStartsFromItsActualBounds() {
        val g = GalleryEntranceMath.start(40f, 200f, 100f, 100f, 400f, 800f)
        assertEquals(0.25f, g.scaleX, 0.0001f)
        assertEquals(0.125f, g.scaleY, 0.0001f)
        assertEquals(-110f, g.translationX, 0.0001f)
        assertEquals(-150f, g.translationY, 0.0001f)
    }
    @Test fun centeredCellStartsCentered() {
        val g = GalleryEntranceMath.start(150f, 350f, 100f, 100f, 400f, 800f)
        assertEquals(0f, g.translationX, 0.0001f)
        assertEquals(0f, g.translationY, 0.0001f)
    }
    @Test fun invalidAndZeroBoundsFallBackToSafeFullscreen() {
        assertEquals(GalleryEntrance(), GalleryEntranceMath.start(0f, 0f, 0f, 100f, 400f, 800f))
        assertEquals(GalleryEntrance(), GalleryEntranceMath.start(Float.NaN, 0f, 90f, 90f, 400f, 800f))
        assertEquals(GalleryEntrance(), GalleryEntranceMath.start(0f, 0f, 90f, 90f, Float.POSITIVE_INFINITY, 800f))
    }
    @Test fun extremeBoundsAreClamped() {
        val g = GalleryEntranceMath.start(-10000f, 10000f, 2f, 2f, 400f, 800f)
        assertEquals(0.08f, g.scaleX, 0.0001f)
        assertEquals(0.08f, g.scaleY, 0.0001f)
        assertEquals(-400f, g.translationX, 0.0001f)
        assertEquals(800f, g.translationY, 0.0001f)
    }
}
