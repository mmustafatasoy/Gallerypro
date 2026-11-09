package com.atasoy.gallerypro.data.media

import android.content.Context
import android.net.Uri
import android.util.Size
import androidx.compose.runtime.staticCompositionLocalOf
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.key.Keyer
import coil3.memory.MemoryCache
import coil3.request.Options
import coil3.request.crossfade

/**
 * Izgara için küçük resim isteği. Boyut kovalara yuvarlanır ki aynı kare
 * farklı sütun sayılarında önbellekten tekrar kullanılabilsin.
 * [version] dosya değişince (düzenleme) eski önbellek kullanılmasın diye anahtara girer.
 */
data class MediaThumb(val uri: Uri, val sizePx: Int, val version: Long = 0L) {
    companion object {
        private val buckets = intArrayOf(128, 192, 256, 384, 512, 768, 1080, 1440)

        fun bucket(px: Int): Int = buckets.firstOrNull { it >= px } ?: buckets.last()
    }
}

/**
 * Android'in kendi küçük resim önbelleğini (ContentResolver.loadThumbnail) kullanır;
 * orijinal dosya çözülmez, bu yüzden kaydırma hızlıdır. Videolar için ilk kareyi verir.
 */
class MediaThumbFetcher(
    private val context: Context,
    private val data: MediaThumb
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val bitmap = context.contentResolver.loadThumbnail(
            data.uri,
            Size(data.sizePx, data.sizePx),
            null
        )
        return ImageFetchResult(
            image = bitmap.asImage(),
            isSampled = true,
            dataSource = DataSource.DISK
        )
    }

    class Factory : Fetcher.Factory<MediaThumb> {
        override fun create(data: MediaThumb, options: Options, imageLoader: ImageLoader): Fetcher =
            MediaThumbFetcher(options.context, data)
    }
}

class MediaThumbKeyer : Keyer<MediaThumb> {
    override fun key(data: MediaThumb, options: Options): String = "${data.uri}#${data.sizePx}@${data.version}"
}

object GalleryImageLoader {
    fun create(context: Context): ImageLoader = ImageLoader.Builder(context)
        .components {
            add(MediaThumbKeyer())
            add(MediaThumbFetcher.Factory())
        }
        .memoryCache {
            MemoryCache.Builder()
                .maxSizePercent(context, 0.25)
                .build()
        }
        .crossfade(120)
        .build()
}

val LocalGalleryImageLoader = staticCompositionLocalOf<ImageLoader> {
    error("GalleryImageLoader sağlanmadı")
}
