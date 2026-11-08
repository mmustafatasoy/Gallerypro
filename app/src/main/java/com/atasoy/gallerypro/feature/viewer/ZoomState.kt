package com.atasoy.gallerypro.feature.viewer

import androidx.compose.animation.core.animate
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import com.atasoy.gallerypro.designsystem.GalleryMotion
import kotlin.math.max
import kotlin.math.min

/**
 * Yakınlaştırma geometrisi (saf, birim testli).
 *
 * Görüntü kabın ortasına sığdırılır (Fit), sonra merkez etrafında [scale] kadar
 * büyütülüp [offset] kadar kaydırılır: ekran = merkez + (nokta − merkez)·ölçek + kayma.
 */
object ZoomMath {

    /** İçeriği kaba oranını koruyarak sığdırır. */
    fun fit(container: Size, content: Size): Size {
        if (container.width <= 0f || container.height <= 0f || content.width <= 0f || content.height <= 0f) {
            return container
        }
        val s = min(container.width / content.width, container.height / content.height)
        return Size(content.width * s, content.height * s)
    }

    /** Verilen ölçekte izin verilen en büyük kayma (her eksende ±). */
    fun maxOffset(fitted: Size, container: Size, scale: Float): Offset = Offset(
        max(0f, (fitted.width * scale - container.width) / 2f),
        max(0f, (fitted.height * scale - container.height) / 2f)
    )

    fun clamp(offset: Offset, fitted: Size, container: Size, scale: Float): Offset {
        val m = maxOffset(fitted, container, scale)
        // "+ 0f" sınırdaki -0f'ı 0f yapar (Offset eşitliği bit düzeyinde karşılaştırılır).
        return Offset(offset.x.coerceIn(-m.x, m.x) + 0f, offset.y.coerceIn(-m.y, m.y) + 0f)
    }

    /**
     * [centroid] noktası parmakların altında kalacak şekilde yeni kayma.
     * t' = (q − c)(1 − z) + t·z + kaydırma; z = yeniÖlçek / eskiÖlçek.
     */
    fun zoomAround(
        offset: Offset,
        scale: Float,
        newScale: Float,
        centroid: Offset,
        container: Size,
        pan: Offset = Offset.Zero
    ): Offset {
        val z = newScale / scale
        val q = centroid - Offset(container.width / 2f, container.height / 2f)
        return q * (1f - z) + offset * z + pan
    }

    /** Çift dokunma ölçeği: en az 2,5×; ince uzun fotoğrafta ekranı dolduracak kadar. */
    fun doubleTapScale(fitted: Size, container: Size, maxScale: Float): Float {
        if (fitted.width <= 0f || fitted.height <= 0f) return 2.5f
        val fill = max(container.width / fitted.width, container.height / fitted.height)
        return max(2.5f, fill).coerceAtMost(maxScale)
    }
}

/** Tek bir sayfanın yakınlaştırma durumu. */
@Stable
class ZoomState(val maxScale: Float = 8f) {

    var scale by mutableFloatStateOf(1f)
        private set
    var offset by mutableStateOf(Offset.Zero)
        private set

    /** Sayfanın boyutu (px). */
    var container by mutableStateOf(Size.Zero)
    /** Görüntünün gerçek (döndürülmüş) piksel boyutu; yüklenince güncellenir. */
    var content by mutableStateOf(Size.Zero)

    val fitted: Size get() = ZoomMath.fit(container, content)
    val isZoomed: Boolean get() = scale > 1.01f

    fun transform(zoom: Float, pan: Offset, centroid: Offset) {
        val newScale = (scale * zoom).coerceIn(0.75f, maxScale)
        val newOffset = ZoomMath.zoomAround(offset, scale, newScale, centroid, container, pan)
        scale = newScale
        offset = if (newScale >= 1f) ZoomMath.clamp(newOffset, fitted, container, newScale) else newOffset
    }

    fun pan(delta: Offset) {
        offset = ZoomMath.clamp(offset + delta, fitted, container, scale)
    }

    /** Parmaklar kalkınca: 1×'in altına inildiyse geri yaylanır. */
    suspend fun settle() {
        if (scale < 1f) animateTo(1f, Offset.Zero)
    }

    suspend fun toggleAt(tap: Offset) {
        if (isZoomed) {
            animateTo(1f, Offset.Zero)
        } else {
            val target = ZoomMath.doubleTapScale(fitted, container, maxScale)
            val targetOffset = ZoomMath.clamp(
                ZoomMath.zoomAround(Offset.Zero, 1f, target, tap, container),
                fitted,
                container,
                target
            )
            animateTo(target, targetOffset)
        }
    }

    fun reset() {
        scale = 1f
        offset = Offset.Zero
    }

    private suspend fun animateTo(targetScale: Float, targetOffset: Offset) {
        val startScale = scale
        val startOffset = offset
        animate(0f, 1f, animationSpec = GalleryMotion.standard()) { t, _ ->
            scale = startScale + (targetScale - startScale) * t
            offset = lerp(startOffset, targetOffset, t)
        }
    }
}
