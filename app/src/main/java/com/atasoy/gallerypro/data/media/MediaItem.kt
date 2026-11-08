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
    val isFavorite: Boolean,
    /** Dosya adı, ör. "IMG_20261108_064200.jpg". */
    val displayName: String? = null,
    /** Bayt cinsinden dosya boyutu. */
    val sizeBytes: Long = 0L,
    /** EXIF döndürmesi (0, 90, 180, 270). Genişlik/yükseklik döndürme öncesidir. */
    val orientation: Int = 0,
    /** Klasör, ör. "DCIM/Camera/". */
    val relativePath: String? = null
)

/** Ekranda görünen genişlik ve yükseklik (döndürme uygulanmış). */
val MediaItem.displayWidth: Int get() = if (orientation % 180 == 0) width else height
val MediaItem.displayHeight: Int get() = if (orientation % 180 == 0) height else width

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
