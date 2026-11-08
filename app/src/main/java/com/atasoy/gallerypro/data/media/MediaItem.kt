package com.atasoy.gallerypro.data.media

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.runtime.Immutable

/**
 * MediaStore'daki tek bir fotoğraf veya video. Uri tutulmaz (birim testlerde
 * Android sınıfı gerektirmesin diye); gerektiğinde [contentUri] ile üretilir.
 */
@Immutable
data class MediaItem(
    val id: Long,
    val isVideo: Boolean,
    /** Çekim zamanı (ms, UTC). Yoksa eklenme zamanı. */
    val takenAtMillis: Long,
    val durationMs: Long,
    val mimeType: String?,
    val width: Int,
    val height: Int,
    val album: String?,
    val isFavorite: Boolean
)

fun MediaItem.contentUri(): Uri = ContentUris.withAppendedId(
    if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
    id
)

enum class LibraryFilter { All, Favorites, Videos, Photos }

fun List<MediaItem>.filteredBy(filter: LibraryFilter): List<MediaItem> = when (filter) {
    LibraryFilter.All -> this
    LibraryFilter.Favorites -> filter { it.isFavorite }
    LibraryFilter.Videos -> filter { it.isVideo }
    LibraryFilter.Photos -> filter { !it.isVideo }
}
