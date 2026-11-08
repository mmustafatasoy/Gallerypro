package com.atasoy.gallerypro

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.atasoy.gallerypro.data.media.DetailFormat
import com.atasoy.gallerypro.feature.viewer.ZoomMath
import com.atasoy.gallerypro.feature.viewer.infoShift
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class ViewerLogicTest {

    private val tr = Locale.forLanguageTag("tr-TR")
    private val container = Size(1000f, 2000f)
    private val fitted = Size(1000f, 750f)

    // ---------------------------------------------------------------- yakınlaştırma

    @Test
    fun fit_keepsAspectRatio() {
        assertEquals(fitted, ZoomMath.fit(container, Size(4000f, 3000f)))
    }

    @Test
    fun maxOffset_onlyWhereImageOverflows() {
        assertEquals(Offset(500f, 0f), ZoomMath.maxOffset(fitted, container, 2f))
        assertEquals(Offset(0f, 0f), ZoomMath.maxOffset(fitted, container, 1f))
    }

    @Test
    fun clamp_staysInsideImage() {
        assertEquals(Offset(500f, 0f), ZoomMath.clamp(Offset(900f, 300f), fitted, container, 2f))
        assertEquals(Offset(-500f, 0f), ZoomMath.clamp(Offset(-900f, -300f), fitted, container, 2f))
    }

    @Test
    fun zoomAround_keepsPointUnderFinger() {
        val center = Offset(500f, 1000f)
        assertEquals(Offset.Zero, ZoomMath.zoomAround(Offset.Zero, 1f, 2f, center, container))
        // Sol kenardaki nokta 2× yakınlaştırmada yerinde kalmalı: içerik 500 px sağa kayar.
        assertEquals(Offset(500f, 0f), ZoomMath.zoomAround(Offset.Zero, 1f, 2f, Offset(0f, 1000f), container))
    }

    @Test
    fun doubleTap_fillsScreenForWidePhotos() {
        assertEquals(2000f / 750f, ZoomMath.doubleTapScale(fitted, container, 8f), 0.001f)
        assertEquals(2.5f, ZoomMath.doubleTapScale(Size(1000f, 1500f), container, 8f), 0.001f)
    }

    @Test
    fun infoShift_centersSmallPhotosAndBottomAlignsTallOnes() {
        assertEquals(-620f, infoShift(2000f, 750f, 1240f), 0.001f)
        assertEquals(-1240f, infoShift(2000f, 2000f, 1240f), 0.001f)
    }

    // ---------------------------------------------------------------- bilgi paneli biçimleri

    @Test
    fun exposureValues() {
        assertEquals("ƒ1,8", DetailFormat.fNumber(1.8, tr))
        assertEquals("ƒ2", DetailFormat.fNumber(2.0, tr))
        assertEquals("1/125 s", DetailFormat.exposure(1.0 / 125, tr))
        assertEquals("2,5 s", DetailFormat.exposure(2.5, tr))
        assertEquals("0 ev", DetailFormat.bias(0.0, tr))
        assertEquals("+0,3 ev", DetailFormat.bias(0.33, tr))
        assertEquals("-1 ev", DetailFormat.bias(-1.0, Locale.US))
        assertEquals("24 mm", DetailFormat.focal(24.0))
    }

    @Test
    fun sizes() {
        assertEquals("12", DetailFormat.megapixels(4000, 3000, tr))
        assertEquals("2,1", DetailFormat.megapixels(1920, 1080, tr))
        assertEquals("3,2 MB", DetailFormat.fileSize(3_200_000L, tr))
        assertEquals("850 KB", DetailFormat.fileSize(850_000L, tr))
        assertEquals("1.5 GB", DetailFormat.fileSize(1_500_000_000L, Locale.US))
    }

    @Test
    fun coordinates() {
        assertEquals(
            "41,0082° K · 28,9784° D",
            DetailFormat.coordinates(41.0082, 28.9784, tr, "K", "G", "D", "B")
        )
        assertEquals(
            "33.8688° S · 151.2093° E",
            DetailFormat.coordinates(-33.8688, 151.2093, Locale.US, "N", "S", "E", "W")
        )
    }

    @Test
    fun videoLocation() {
        assertEquals(41.0082 to 28.9784, DetailFormat.parseIso6709("+41.0082+028.9784/"))
        assertEquals(-33.8688 to 151.2093, DetailFormat.parseIso6709("-33.8688+151.2093+012.000/"))
        assertNull(DetailFormat.parseIso6709("bilinmiyor"))
    }

    @Test
    fun deviceAndFormat() {
        assertEquals("Samsung SM-S911B", DetailFormat.deviceName("samsung", "SM-S911B"))
        assertEquals("Xiaomi 14", DetailFormat.deviceName("Xiaomi", "Xiaomi 14"))
        assertNull(DetailFormat.deviceName(null, " "))
        assertEquals("JPEG", DetailFormat.formatBadge("image/jpeg"))
        assertEquals("HEIF", DetailFormat.formatBadge("image/heic"))
        assertEquals("MP4", DetailFormat.formatBadge("video/mp4"))
        assertEquals("RAW", DetailFormat.formatBadge("image/x-adobe-dng"))
    }
}
