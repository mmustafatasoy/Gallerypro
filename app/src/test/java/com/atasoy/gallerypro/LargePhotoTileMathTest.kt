package com.atasoy.gallerypro

import com.atasoy.gallerypro.feature.viewer.LargePhotoTileMath
import com.atasoy.gallerypro.feature.viewer.TileRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LargePhotoTileMathTest {
    @Test fun onlyLargeSupportedPhotosUseTiling() {
        assertTrue(LargePhotoTileMath.supported(12000, 9000, false, "image/jpeg"))
        assertTrue(LargePhotoTileMath.supported(8192, 8192, false, "image/png"))
        assertFalse(LargePhotoTileMath.supported(4096, 3500, false, "image/jpeg"))
        assertFalse(LargePhotoTileMath.supported(12000, 9000, true, "image/jpeg"))
        assertFalse(LargePhotoTileMath.supported(12000, 9000, false, "image/heic"))
    }

    @Test fun zoomedCenterMapsToOriginalPixelCoordinates() {
        val rect = LargePhotoTileMath.visibleRect(
            12000, 8000, 1200f, 800f, 4f, 0f, 0f, 0f
        )
        assertEquals(TileRect(4500, 3000, 7500, 5000), rect)
    }

    @Test fun panningMovesViewportAndClampsBounds() {
        val left = LargePhotoTileMath.visibleRect(
            12000, 8000, 1200f, 800f, 4f, 500f, 0f, 0f
        )!!
        val right = LargePhotoTileMath.visibleRect(
            12000, 8000, 1200f, 800f, 4f, -500f, 0f, 0f
        )!!
        assertTrue(left.left < right.left)
        assertNull(LargePhotoTileMath.visibleRect(
            12000, 8000, 1200f, 800f, 8f, 10000f, -10000f, 0.18f
        ))
    }

    @Test fun rotationIsReversible() {
        for (angle in listOf(0, 90, 180, 270)) {
            val display = TileRect(300, 100, 1300, 900)
            val raw = LargePhotoTileMath.toRawRect(display, 12000, 8000, angle)!!
            assertEquals(display, LargePhotoTileMath.toDisplayRect(raw, 12000, 8000, angle))
        }
        assertNull(LargePhotoTileMath.toRawRect(TileRect(100, 100, 500, 500), 12000, 8000, 45))
    }

    @Test fun sampledDecodeKeepsBitmapUnderBudget() {
        assertEquals(1, LargePhotoTileMath.sampleSize(1200, 900))
        assertEquals(2, LargePhotoTileMath.sampleSize(4096, 2000))
        assertEquals(8, LargePhotoTileMath.sampleSize(12000, 8000))
        assertEquals(32, LargePhotoTileMath.sampleSize(60000, 50000))
    }

    @Test fun lowZoomAndInvalidSizesNeedNoTile() {
        assertNull(LargePhotoTileMath.visibleRect(12000, 8000, 1200f, 800f, 1f, 0f, 0f))
        assertNull(LargePhotoTileMath.visibleRect(12000, 8000, 0f, 800f, 4f, 0f, 0f))
        assertNull(LargePhotoTileMath.visibleRect(0, 8000, 1200f, 800f, 4f, 0f, 0f))
    }
}
