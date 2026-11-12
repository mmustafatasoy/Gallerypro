package com.atasoy.gallerypro.feature.library

/**
 * Izgaradaki dokunulan karenin konumundan tam ekranın merkezi/boyutuna
 * açılış geometrisi. Saf matematik; küçük ekran ve bozuk konumlarda güvenli.
 */
internal data class GalleryEntrance(
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val translationX: Float = 0f,
    val translationY: Float = 0f
)

internal object GalleryEntranceMath {
    fun start(
        left: Float, top: Float, width: Float, height: Float,
        viewportWidth: Float, viewportHeight: Float
    ): GalleryEntrance {
        if (!left.isFinite() || !top.isFinite() || !width.isFinite() ||
            !height.isFinite() || !viewportWidth.isFinite() || !viewportHeight.isFinite() ||
            width <= 0f || height <= 0f || viewportWidth <= 0f || viewportHeight <= 0f) {
            return GalleryEntrance()
        }
        val x = (left + width / 2f) - viewportWidth / 2f
        val y = (top + height / 2f) - viewportHeight / 2f
        return GalleryEntrance(
            scaleX = (width / viewportWidth).coerceIn(0.08f, 1f),
            scaleY = (height / viewportHeight).coerceIn(0.08f, 1f),
            translationX = x.coerceIn(-viewportWidth, viewportWidth),
            translationY = y.coerceIn(-viewportHeight, viewportHeight)
        )
    }
}
