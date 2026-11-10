package com.atasoy.gallerypro

import com.atasoy.gallerypro.data.media.VideoTrimLogic
import com.atasoy.gallerypro.data.media.VideoTrimRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoTrimLogicTest {
    @Test fun requiresEnoughDuration() {
        assertNull(VideoTrimLogic.initial(0))
        assertNull(VideoTrimLogic.initial(999))
        assertNull(VideoTrimLogic.initial(1000))
        assertEquals(VideoTrimRange(0, 5_000), VideoTrimLogic.initial(5_000))
    }

    @Test fun rangeHandlesCannotCross() {
        val full = VideoTrimRange(0, 5_000)
        val first = VideoTrimLogic.moveStart(full, 10_000)
        assertEquals(4_000, first.startMs)
        assertEquals(5_000, first.endMs)
        val next = VideoTrimLogic.moveEnd(VideoTrimRange(2_000,5_000), -50, 5_000)
        assertEquals(3_000, next.endMs)
        assertEquals(5_000, VideoTrimLogic.moveEnd(full, 999_999, 5_000).endMs)
    }

    @Test fun fullLengthAndInvalidRangesDoNotExport() {
        assertFalse(VideoTrimLogic.canExport(VideoTrimRange(0, 5_000), 5_000))
        assertFalse(VideoTrimLogic.canExport(VideoTrimRange(-1, 2_000), 5_000))
        assertFalse(VideoTrimLogic.canExport(VideoTrimRange(1_000,6_000), 5_000))
        assertFalse(VideoTrimLogic.canExport(VideoTrimRange(1_000,1_999), 5_000))
        assertTrue(VideoTrimLogic.canExport(VideoTrimRange(1_000,4_000), 5_000))
    }

    @Test fun touchPositionIsClamped() {
        assertEquals(0L, VideoTrimLogic.position(-2f, 60_000))
        assertEquals(30_000L, VideoTrimLogic.position(0.5f, 60_000))
        assertEquals(60_000L, VideoTrimLogic.position(2f, 60_000))
    }
}
