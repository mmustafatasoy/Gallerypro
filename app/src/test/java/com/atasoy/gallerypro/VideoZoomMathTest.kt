package com.atasoy.gallerypro

import com.atasoy.gallerypro.feature.viewer.VideoZoomMath
import com.atasoy.gallerypro.feature.viewer.VideoZoomTransform
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoZoomMathTest {
    @Test fun zoomAroundCenterKeepsOffsetZero() {
        val z = VideoZoomMath.transform(
            VideoZoomTransform(), 2f, 0f, 0f, 50f, 100f,
            100f, 200f, 100f, 200f
        )
        assertEquals(2f, z.scale, 0.001f)
        assertEquals(0f, z.x, 0.001f)
        assertEquals(0f, z.y, 0.001f)
    }

    @Test fun pinchAnchorsFingersAndClampsToSurface() {
        val z = VideoZoomMath.transform(
            VideoZoomTransform(), 2f, 0f, 0f, 75f, 100f,
            100f, 200f, 100f, 200f
        )
        assertEquals(-25f, z.x, 0.001f)
        assertEquals(0f, z.y, 0.001f)
        val p = VideoZoomMath.pan(z, -1000f, 1000f, 100f, 200f, 100f, 200f)
        assertEquals(-50f, p.x, 0.001f)
        assertEquals(100f, p.y, 0.001f)
    }

    @Test fun cropAndFitKeepDifferentBounds() {
        val crop = VideoZoomMath.clamp(VideoZoomTransform(2f, 500f, 500f),
            100f, 200f, 150f, 200f)
        assertEquals(100f, crop.x, 0.001f)
        assertEquals(100f, crop.y, 0.001f)
        val fit = VideoZoomMath.clamp(VideoZoomTransform(2f, 500f, 500f),
            200f, 100f, 200f, 75f)
        assertEquals(100f, fit.x, 0.001f)
        assertEquals(25f, fit.y, 0.001f)
    }

    @Test fun pinchOutReturnsToDefaultAndCapsAtFourTimes() {
        val z = VideoZoomMath.transform(VideoZoomTransform(), 100f, 0f, 0f,
            50f, 50f, 100f, 100f, 100f, 100f)
        assertEquals(4f, z.scale, 0.001f)
        val reset = VideoZoomMath.transform(z, 0.1f, 0f, 0f,
            50f, 50f, 100f, 100f, 100f, 100f)
        assertEquals(VideoZoomTransform(), reset)
    }

    @Test fun invalidValuesDoNotPropagateIntoGraphicsLayer() {
        val initial = VideoZoomTransform()
        assertEquals(initial, VideoZoomMath.transform(initial, Float.NaN, 0f, 0f, 0f, 0f,
            100f, 100f, 100f, 100f))
        assertEquals(initial, VideoZoomMath.clamp(VideoZoomTransform(2f, Float.NaN, 0f),
            100f, 100f, 100f, 100f))
    }

    @Test fun zoomedViewCanPanButUnzoomedCannot() {
        val z = VideoZoomTransform()
        assertEquals(z, VideoZoomMath.pan(z, 80f, 20f, 100f, 200f, 100f, 200f))
    }
}
