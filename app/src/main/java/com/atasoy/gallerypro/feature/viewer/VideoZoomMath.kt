package com.atasoy.gallerypro.feature.viewer

import kotlin.math.max

/**
 * Video TextureView'u için büyütme/sürükleme hesabı.
 *
 * Fotoğrafın Fit hesabı yerine, gerçek video yüzeyi boyutları kullanılır:
 * dikey videoda var olan Crop, yatay videoda Fit düzeni korunur.
 * Tüm değerler piksel cinsindendir; video dosyasına müdahale edilmez.
 */
internal data class VideoZoomTransform(
    val scale: Float = 1f,
    val x: Float = 0f,
    val y: Float = 0f
)

internal object VideoZoomMath {
    private const val MaxScale = 4f

    fun transform(
        current: VideoZoomTransform,
        factor: Float,
        panX: Float,
        panY: Float,
        centroidX: Float,
        centroidY: Float,
        viewportWidth: Float,
        viewportHeight: Float,
        contentWidth: Float,
        contentHeight: Float
    ): VideoZoomTransform {
        if (!factor.isFinite() || factor <= 0f || !panX.isFinite() || !panY.isFinite() ||
            !centroidX.isFinite() || !centroidY.isFinite()) return current
        val newScale = (current.scale * factor).coerceIn(1f, MaxScale)
        if (newScale <= 1.01f) return VideoZoomTransform()
        val ratio = newScale / current.scale
        val x = (centroidX - viewportWidth / 2f) * (1f - ratio) + current.x * ratio + panX
        val y = (centroidY - viewportHeight / 2f) * (1f - ratio) + current.y * ratio + panY
        return clamp(VideoZoomTransform(newScale, x, y), viewportWidth, viewportHeight, contentWidth, contentHeight)
    }

    fun pan(
        current: VideoZoomTransform,
        dx: Float,
        dy: Float,
        viewportWidth: Float,
        viewportHeight: Float,
        contentWidth: Float,
        contentHeight: Float
    ): VideoZoomTransform {
        if (!dx.isFinite() || !dy.isFinite() || current.scale <= 1.01f) return current
        return clamp(
            current.copy(x = current.x + dx, y = current.y + dy),
            viewportWidth, viewportHeight, contentWidth, contentHeight
        )
    }

    fun clamp(
        current: VideoZoomTransform,
        viewportWidth: Float,
        viewportHeight: Float,
        contentWidth: Float,
        contentHeight: Float
    ): VideoZoomTransform {
        if (!viewportWidth.isFinite() || !viewportHeight.isFinite() ||
            !contentWidth.isFinite() || !contentHeight.isFinite() ||
            viewportWidth <= 0f || viewportHeight <= 0f ||
            contentWidth <= 0f || contentHeight <= 0f ||
            !current.x.isFinite() || !current.y.isFinite()) return VideoZoomTransform()
        val scale = current.scale.coerceIn(1f, MaxScale)
        if (!scale.isFinite() || scale <= 1.01f) return VideoZoomTransform()
        val xMax = max(0f, (contentWidth * scale - viewportWidth) / 2f)
        val yMax = max(0f, (contentHeight * scale - viewportHeight) / 2f)
        return VideoZoomTransform(scale, current.x.coerceIn(-xMax, xMax) + 0f,
            current.y.coerceIn(-yMax, yMax) + 0f)
    }
}
