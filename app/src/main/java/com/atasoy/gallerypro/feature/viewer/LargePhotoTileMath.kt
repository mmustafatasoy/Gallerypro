package com.atasoy.gallerypro.feature.viewer

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** Orijinal fotoğrafın piksel koordinatlarında, sağ/alt kenar hariç dikdörtgen. */
internal data class TileRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

/** ZoomState / ZoomMath dönüşümleriyle aynı geometriyi kullanan saf Kotlin hesapları. */
internal object LargePhotoTileMath {
    const val ThresholdPx = 4096
    const val PreviewPx = 2048
    const val MaxDecodedSidePx = 2048
    const val MinTileScale = 1.25f
    private const val EdgePrefetch = 0.18f

    fun supported(width: Int, height: Int, isVideo: Boolean, mime: String?): Boolean =
        !isVideo && max(width, height) > ThresholdPx &&
            mime?.lowercase() in setOf("image/jpeg", "image/png", "image/webp")

    /** Yakınlaştırmada ekranda görünen ham piksel bölgesi, kenarlarda ön yüklemeyle. */
    fun visibleRect(
        imageWidth: Int, imageHeight: Int, screenWidth: Float, screenHeight: Float,
        zoom: Float, panX: Float, panY: Float, paddingRatio: Float = EdgePrefetch
    ): TileRect? {
        if (imageWidth < 1 || imageHeight < 1 ||
            screenWidth <= 0f || screenHeight <= 0f ||
            !screenWidth.isFinite() || !screenHeight.isFinite() ||
            !zoom.isFinite() || zoom < MinTileScale ||
            !panX.isFinite() || !panY.isFinite()
        ) return null
        val fitScale = min(screenWidth / imageWidth, screenHeight / imageHeight)
        if (fitScale <= 0f || !fitScale.isFinite()) return null
        val fitWidth = imageWidth * fitScale
        val fitHeight = imageHeight * fitScale
        val imageLeft = (screenWidth - fitWidth) / 2f
        val imageTop = (screenHeight - fitHeight) / 2f
        val canvasLeft = screenWidth / 2f + (-screenWidth / 2f - panX) / zoom
        val canvasTop = screenHeight / 2f + (-screenHeight / 2f - panY) / zoom
        val canvasRight = screenWidth / 2f + (screenWidth / 2f - panX) / zoom
        val canvasBottom = screenHeight / 2f + (screenHeight / 2f - panY) / zoom
        val sourceLeft = (canvasLeft - imageLeft) / fitScale
        val sourceTop = (canvasTop - imageTop) / fitScale
        val sourceRight = (canvasRight - imageLeft) / fitScale
        val sourceBottom = (canvasBottom - imageTop) / fitScale
        if (!sourceLeft.isFinite() || !sourceTop.isFinite() ||
            !sourceRight.isFinite() || !sourceBottom.isFinite()
        ) return null
        val extraX = (sourceRight - sourceLeft).coerceAtLeast(0f) * paddingRatio.coerceIn(0f, 0.5f)
        val extraY = (sourceBottom - sourceTop).coerceAtLeast(0f) * paddingRatio.coerceIn(0f, 0.5f)
        val left = floor((sourceLeft - extraX).coerceAtLeast(0f)).toInt()
        val top = floor((sourceTop - extraY).coerceAtLeast(0f)).toInt()
        val right = ceil((sourceRight + extraX).coerceAtMost(imageWidth.toFloat())).toInt()
        val bottom = ceil((sourceBottom + extraY).coerceAtMost(imageHeight.toFloat())).toInt()
        if (right <= left || bottom <= top) return null
        return TileRect(left, top, right, bottom)
    }

    /** Ekranda döndürülmüş fotoğraf koordinatından dosyadaki orijinal piksele. */
    fun toRawRect(rect: TileRect, rawWidth: Int, rawHeight: Int, orientation: Int): TileRect? =
        when (((orientation % 360) + 360) % 360) {
            0 -> rect
            90 -> TileRect(rect.top, rawHeight - rect.right, rect.bottom, rawHeight - rect.left)
            180 -> TileRect(rawWidth - rect.right, rawHeight - rect.bottom,
                rawWidth - rect.left, rawHeight - rect.top)
            270 -> TileRect(rawWidth - rect.bottom, rect.left, rawWidth - rect.top, rect.right)
            else -> null
        }?.takeIf {
            it.left >= 0 && it.top >= 0 && it.right <= rawWidth &&
                it.bottom <= rawHeight && it.width > 0 && it.height > 0
        }

    /** Ham JPEG parçasını döndürülmüş ekrandaki doğru koordinata geri yerleştirir. */
    fun toDisplayRect(rect: TileRect, rawWidth: Int, rawHeight: Int, orientation: Int): TileRect? =
        when (((orientation % 360) + 360) % 360) {
            0 -> rect
            90 -> TileRect(rawHeight - rect.bottom, rect.left, rawHeight - rect.top, rect.right)
            180 -> TileRect(rawWidth - rect.right, rawHeight - rect.bottom,
                rawWidth - rect.left, rawHeight - rect.top)
            270 -> TileRect(rect.top, rawWidth - rect.right, rect.bottom, rawWidth - rect.left)
            else -> null
        }

    /** 108 MP fotoğrafın bile her seferinde yalnızca en çok 2048px'lik bölümünü çözer. */
    fun sampleSize(width: Int, height: Int): Int {
        var sample = 1
        while (max(width / sample, height / sample) > MaxDecodedSidePx && sample < (1 shl 20)) {
            sample *= 2
        }
        return sample
    }
}
