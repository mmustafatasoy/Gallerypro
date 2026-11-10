package com.atasoy.gallerypro.feature.viewer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Matrix
import android.graphics.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.atasoy.gallerypro.data.media.MediaItem
import com.atasoy.gallerypro.data.media.contentUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Yalnızca göründüğü alanı işler; medyayı değiştirmez ve ağ kullanmaz. */
internal data class DecodedPhotoTile(val bitmap: ImageBitmap, val displayRect: TileRect)

internal object LargePhotoRegionDecoder {
    @Suppress("DEPRECATION")
    suspend fun decode(
        context: Context, item: MediaItem, visibleRect: TileRect
    ): DecodedPhotoTile? = withContext(Dispatchers.IO) {
        if (!LargePhotoTileMath.supported(item.width, item.height, item.isVideo, item.mimeType)) {
            return@withContext null
        }
        try {
            context.contentResolver.openInputStream(item.contentUri())?.use { input ->
                val decoder = BitmapRegionDecoder.newInstance(input, false) ?: return@use null
                try {
                    // EXIF / MediaStore boyut uyuşmazlığında yanlış parça çizme.
                    if (decoder.width != item.width || decoder.height != item.height) return@use null
                    val raw = LargePhotoTileMath.toRawRect(
                        visibleRect, decoder.width, decoder.height, item.orientation
                    ) ?: return@use null
                    val bitmap = decoder.decodeRegion(
                        Rect(raw.left, raw.top, raw.right, raw.bottom),
                        BitmapFactory.Options().apply {
                            inPreferredConfig = Bitmap.Config.ARGB_8888
                            inSampleSize = LargePhotoTileMath.sampleSize(raw.width, raw.height)
                        }
                    ) ?: return@use null
                    val rotated = if (item.orientation % 360 == 0) bitmap else {
                        val matrix = Matrix().apply { postRotate(item.orientation.toFloat()) }
                        val turned = Bitmap.createBitmap(
                            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                        )
                        if (turned !== bitmap) bitmap.recycle()
                        turned
                    }
                    val rect = LargePhotoTileMath.toDisplayRect(
                        raw, decoder.width, decoder.height, item.orientation
                    ) ?: return@use null
                    DecodedPhotoTile(rotated.asImageBitmap(), rect)
                } finally {
                    decoder.recycle()
                }
            }
        } catch (_: Exception) {
            // Hasarlı dosya veya erişim kısıtı: düşük çözünürlüklü önizleme açık kalır.
            null
        } catch (_: OutOfMemoryError) {
            null
        }
    }
}
