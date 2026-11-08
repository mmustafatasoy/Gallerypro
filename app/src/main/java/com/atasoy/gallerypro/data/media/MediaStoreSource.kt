package com.atasoy.gallerypro.data.media

import android.content.Context
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore

/**
 * MediaStore'dan hafif bir dizin okur: kimlik, tür, tarih, süre, boyut.
 * Piksel okunmaz; 10.000 öğe birkaç yüz milisaniyede gelir.
 * Görüntüleyici için dosya adı, boyut, döndürme ve klasör de okunur.
 */
class MediaStoreSource(private val context: Context) {

    fun queryAll(): List<MediaItem> {
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val projection = buildList {
            add(MediaStore.Files.FileColumns._ID)
            add(MediaStore.Files.FileColumns.MEDIA_TYPE)
            add(MediaStore.MediaColumns.DATE_TAKEN)
            add(MediaStore.MediaColumns.DATE_ADDED)
            add(MediaStore.MediaColumns.DATE_MODIFIED)
            add(MediaStore.MediaColumns.DURATION)
            add(MediaStore.MediaColumns.MIME_TYPE)
            add(MediaStore.MediaColumns.WIDTH)
            add(MediaStore.MediaColumns.HEIGHT)
            add(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
            add(MediaStore.MediaColumns.DISPLAY_NAME)
            add(MediaStore.MediaColumns.SIZE)
            add(MediaStore.MediaColumns.ORIENTATION)
            add(MediaStore.MediaColumns.RELATIVE_PATH)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                add(MediaStore.MediaColumns.IS_FAVORITE)
            }
        }.toTypedArray()

        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (?, ?)"
        val args = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )

        val result = ArrayList<MediaItem>(4096)
        context.contentResolver.query(collection, projection, selection, args, null)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val typeCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
            val takenCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
            val addedCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val modifiedCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            val durationCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DURATION)
            val mimeCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val widthCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
            val heightCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)
            val albumCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
            val nameCol = c.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeCol = c.getColumnIndex(MediaStore.MediaColumns.SIZE)
            val orientationCol = c.getColumnIndex(MediaStore.MediaColumns.ORIENTATION)
            val pathCol = c.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
            val favCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                c.getColumnIndex(MediaStore.MediaColumns.IS_FAVORITE)
            } else {
                -1
            }

            while (c.moveToNext()) {
                val taken = c.longOrZero(takenCol)
                val added = c.longOrZero(addedCol) * 1000L
                val modified = c.longOrZero(modifiedCol) * 1000L
                val time = when {
                    taken > 0L -> taken
                    added > 0L -> added
                    else -> modified
                }
                result += MediaItem(
                    id = c.getLong(idCol),
                    isVideo = c.getInt(typeCol) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO,
                    takenAtMillis = time,
                    durationMs = c.longOrZero(durationCol),
                    mimeType = c.getStringOrNull(mimeCol),
                    width = c.intOrZero(widthCol),
                    height = c.intOrZero(heightCol),
                    album = c.getStringOrNull(albumCol),
                    isFavorite = favCol >= 0 && c.intOrZero(favCol) == 1,
                    displayName = c.getStringOrNull(nameCol),
                    sizeBytes = c.longOrZero(sizeCol),
                    orientation = c.intOrZero(orientationCol),
                    relativePath = c.getStringOrNull(pathCol)
                )
            }
        }
        // En eski üstte, en yeni en altta (iOS düzeni).
        result.sortWith(compareBy<MediaItem>({ it.takenAtMillis }, { it.id }))
        return result
    }

    private fun Cursor.longOrZero(index: Int): Long = if (index < 0 || isNull(index)) 0L else getLong(index)
    private fun Cursor.intOrZero(index: Int): Int = if (index < 0 || isNull(index)) 0 else getInt(index)
    private fun Cursor.getStringOrNull(index: Int): String? = if (index < 0 || isNull(index)) null else getString(index)
}
