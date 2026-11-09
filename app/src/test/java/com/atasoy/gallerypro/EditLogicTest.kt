package com.atasoy.gallerypro

import com.atasoy.gallerypro.data.edit.AdjustTool
import com.atasoy.gallerypro.data.edit.AutoCorrection
import com.atasoy.gallerypro.data.edit.ColorMath
import com.atasoy.gallerypro.data.edit.CropMath
import com.atasoy.gallerypro.data.edit.CropRatio
import com.atasoy.gallerypro.data.edit.CropRect
import com.atasoy.gallerypro.data.edit.EditFilter
import com.atasoy.gallerypro.data.edit.EditParams
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EditLogicTest {

    private val eps = 1e-4f

    // ---------------------------------------------------------------- ayarların saklanması

    @Test
    fun params_roundTrip() {
        val params = EditParams()
            .with(AdjustTool.Exposure, 18)
            .with(AdjustTool.Vignette, -25)
            .copy(
                auto = AutoCorrection(1.2f, 1.1f, 1.12f),
                filter = EditFilter.Nostalgia,
                angle = -7,
                ratio = CropRatio.SixteenNine,
                crop = CropRect(0.1f, 0.2f, 0.9f, 0.65f)
            )
        val decoded = EditParams.decode(params.encode())!!
        assertEquals(params.adjust, decoded.adjust)
        assertEquals(params.filter, decoded.filter)
        assertEquals(params.angle, decoded.angle)
        assertEquals(params.ratio, decoded.ratio)
        assertEquals(params.crop.left, decoded.crop.left, eps)
        assertEquals(params.crop.bottom, decoded.crop.bottom, eps)
        assertEquals(params.auto.gain, decoded.auto.gain, eps)
    }

    @Test
    fun params_decodeRejectsGarbage() {
        assertNull(EditParams.decode(null))
        assertNull(EditParams.decode(""))
        assertNull(EditParams.decode("v0;a=1"))
        assertNull(EditParams.decode("v1;a=1,2"))
    }

    @Test
    fun params_identityAndClamp() {
        assertTrue(EditParams().isIdentity)
        assertFalse(EditParams().with(AdjustTool.Sharpness, 5).isIdentity)
        assertFalse(EditParams(angle = 3).isIdentity)
        assertEquals(100, EditParams().with(AdjustTool.Contrast, 250).value(AdjustTool.Contrast))
        assertTrue(EditParams().with(AdjustTool.Vignette, 40).hasColor.not())
    }

    // ---------------------------------------------------------------- renk matrisleri

    @Test
    fun colorMath_identityAndConcat() {
        val id = ColorMath.identity()
        assertArrayEquals(id, ColorMath.full(EditParams()), eps)
        assertArrayEquals(id, ColorMath.filter(EditFilter.Original), eps)
        val m = ColorMath.concat(ColorMath.brightness(0.5f), ColorMath.contrast(2f))
        // Önce parlaklık (0,8 → 0,4), sonra kontrast ((0,4 − 0,5)·2 + 0,5 = 0,3).
        assertEquals(0.3f, ColorMath.apply(m, 0.8f, 0.8f, 0.8f)[0], eps)
    }

    @Test
    fun colorMath_cssFilters() {
        // Gri tonlama ve doygunluk 0: üç kanal eşit olur.
        val gray = ColorMath.apply(ColorMath.grayscale(1f), 0.9f, 0.2f, 0.1f)
        assertEquals(gray[0], gray[1], eps)
        assertEquals(gray[1], gray[2], eps)
        val desat = ColorMath.apply(ColorMath.saturate(0f), 0.9f, 0.2f, 0.1f)
        assertEquals(desat[0], desat[2], eps)
        // Beyaz, doygunluk ve renk döndürmede beyaz kalır.
        val white = ColorMath.apply(ColorMath.hueRotate(-14f), 1f, 1f, 1f)
        assertEquals(1f, white[0], 1e-3f)
        assertEquals(1f, white[2], 1e-3f)
        // Mono filtresinde renk kalmaz.
        val mono = ColorMath.apply(ColorMath.filter(EditFilter.Mono), 0.7f, 0.3f, 0.2f)
        assertEquals(mono[0], mono[2], eps)
    }

    @Test
    fun colorMath_adjustDirection() {
        val brighter = ColorMath.apply(ColorMath.adjust(EditParams().with(AdjustTool.Exposure, 60)), 0.4f, 0.4f, 0.4f)
        assertTrue(brighter[0] > 0.4f)
        val warmer = ColorMath.apply(ColorMath.adjust(EditParams().with(AdjustTool.Warmth, 80)), 0.5f, 0.5f, 0.5f)
        assertTrue("sıcaklık kırmızıyı maviden fazla artırır", warmer[0] > warmer[2])
        val android = ColorMath.toAndroid(ColorMath.contrast(2f))
        assertEquals(-127.5f, android[4], 1e-3f)
    }

    @Test
    fun colorMath_autoCorrection() {
        val dark = ColorMath.autoCorrection(meanLuma = 0.2f, stdLuma = 0.1f, meanSaturation = 0.2f)
        assertTrue(dark.gain > 1f)
        assertEquals(1.3f, dark.contrast, eps)
        assertEquals(1.25f, dark.saturation, eps)
        val bright = ColorMath.autoCorrection(0.9f, 0.3f, 0.6f)
        assertTrue(bright.gain < 1f)
        assertEquals(1f, bright.saturation, eps)
        // Otomatik 0 iken ölçüm etkisizdir.
        assertArrayEquals(ColorMath.identity(), ColorMath.adjust(EditParams(auto = dark)), eps)
    }

    @Test
    fun vignette_centerClearCornerDark() {
        assertEquals(0f, ColorMath.vignetteAlpha(0f, 0f, 100), eps)
        assertEquals(0.85f, ColorMath.vignetteAlpha(1f, 1f, 100), eps)
        assertEquals(0.425f, ColorMath.vignetteAlpha(-1f, 1f, -50), eps)
        assertEquals(0f, ColorMath.vignetteAlpha(1f, 1f, 0), eps)
    }

    // ---------------------------------------------------------------- kırpma

    @Test
    fun crop_coverScale() {
        assertEquals(1f, CropMath.coverScale(0f, 4000f, 3000f), eps)
        assertEquals(1.41421f, CropMath.coverScale(45f, 1000f, 1000f), 1e-3f)
        assertTrue(CropMath.coverScale(-10f, 4000f, 3000f) > 1f)
    }

    @Test
    fun crop_ratioRect() {
        val square = CropMath.ratioRect(1f, 4000f, 3000f)
        assertEquals(0.125f, square.left, eps)
        assertEquals(0.875f, square.right, eps)
        assertEquals(0f, square.top, eps)
        // Piksel cinsinden oran 1:1.
        assertEquals(1f, square.width * 4000f / (square.height * 3000f), eps)
        val wide = CropMath.ratioRect(16f / 9f, 3000f, 4000f)
        assertEquals(0f, wide.left, eps)
        assertEquals(16f / 9f, wide.width * 3000f / (wide.height * 4000f), 1e-3f)
        assertTrue(CropMath.ratioRect(null, 1f, 1f).isFull)
    }

    @Test
    fun crop_dragAndMove() {
        val free = CropMath.dragCorner(CropRect(), corner = 2, dx = -0.2f, dy = -0.1f, ratio = null, frameW = 400f, frameH = 300f)
        assertEquals(0.8f, free.right, eps)
        assertEquals(0.9f, free.bottom, eps)
        assertEquals(0f, free.left, eps)

        val start = CropMath.ratioRect(1f, 400f, 300f)
        val locked = CropMath.dragCorner(start, corner = 0, dx = 0.1f, dy = 0.05f, ratio = 1f, frameW = 400f, frameH = 300f)
        assertEquals(1f, locked.width * 400f / (locked.height * 300f), 1e-3f)
        assertEquals(start.right, locked.right, eps)
        assertEquals(start.bottom, locked.bottom, eps)

        val moved = CropMath.move(CropRect(0.1f, 0.1f, 0.5f, 0.5f), dx = 0.9f, dy = -0.5f)
        assertEquals(1f, moved.right, eps)
        assertEquals(0f, moved.top, eps)
        assertEquals(0.4f, moved.width, eps)

        val tiny = CropMath.dragCorner(CropRect(), corner = 2, dx = -5f, dy = -5f, ratio = null, frameW = 1f, frameH = 1f)
        assertEquals(CropMath.MinSize, tiny.width, eps)
        assertEquals(1000 to 500, CropMath.outputSize(CropRect(0f, 0f, 0.5f, 0.25f), 2000, 2000))
    }
}
