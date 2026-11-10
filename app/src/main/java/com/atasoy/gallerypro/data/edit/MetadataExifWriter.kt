package com.atasoy.gallerypro.data.edit

import android.content.ContentValues
import android.content.Context
import android.media.ExifInterface
import android.media.MediaScannerConnection
import android.provider.MediaStore
import com.atasoy.gallerypro.data.media.MediaDetailsReader
import com.atasoy.gallerypro.data.media.MediaItem
import com.atasoy.gallerypro.data.media.contentUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.ZoneId

/**
 * JPEG'yi doğrudan ExifInterface'a teslim etmez:
 * önce birebir kaynak kopyası, sonra geçici JPEG üzerinde EXIF, sonra MediaStore'a yazma.
 * 'files/originals/<id>' ilk orijinali saklar; var olan fotoğraf düzenleme yedeğine dokunmaz.
 * Yazma/EXIF hatasında o anki fotoğrafın geçici kopyasından geri yüklemeyi dener.
 *
 * MediaStore.createWriteRequest ve ACCESS_MEDIA_LOCATION izni UI'da önceden alınır.
 */
object MetadataExifWriter {
    suspend fun save(
        context: Context,
        item: MediaItem,
        edits: EditStore,
        update: MetadataUpdate,
        zone: ZoneId = ZoneId.systemDefault()
    ): Boolean = withContext(Dispatchers.IO) {
        if (!MetadataEditLogic.isSupported(item.mimeType, item.isVideo) ||
            !MediaDetailsReader.hasLocationPermission(context)
        ) return@withContext false

        val resolver = context.contentResolver
        val mediaUri = item.contentUri()
        val original = File.createTempFile("metadata-original-", ".jpg", context.cacheDir)
        val edited = File.createTempFile("metadata-edited-", ".jpg", context.cacheDir)
        val persistentBackup = edits.backupFile(item.id)
        var newBackup = false
        var writing = false
        try {
            // Konum izni olmadan alınan sansürlü görüntüyü hiçbir zaman yedekleme.
            resolver.openInputStream(MediaStore.setRequireOriginal(mediaUri)).use { input ->
                checkNotNull(input) { "Kaynak dosya okunamadı" }
                original.outputStream().use { output -> input.copyTo(output, 64 * 1024) }
            }
            check(original.length() > 0L)
            original.copyTo(edited, overwrite = true)

            val exif = ExifInterface(edited.absolutePath)
            val exifTime = update.exifDateTime()
            exif.setAttribute(ExifInterface.TAG_DATETIME, exifTime)
            exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, exifTime)
            exif.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, exifTime)
            // Tarih yerel saat olarak girildiğinden önceki çekime ait saat dilimini sil.
            exif.setAttribute("OffsetTime", null)
            exif.setAttribute("OffsetTimeOriginal", null)
            exif.setAttribute("OffsetTimeDigitized", null)

            when {
                update.removeLocation -> {
                    listOf(
                        ExifInterface.TAG_GPS_LATITUDE, ExifInterface.TAG_GPS_LATITUDE_REF,
                        ExifInterface.TAG_GPS_LONGITUDE, ExifInterface.TAG_GPS_LONGITUDE_REF,
                        ExifInterface.TAG_GPS_ALTITUDE, ExifInterface.TAG_GPS_ALTITUDE_REF,
                        "GPSDateStamp", "GPSTimeStamp", "GPSProcessingMethod"
                    ).forEach { exif.setAttribute(it, null) }
                }
                update.latitude != null && update.longitude != null ->
                    exif.setLatLong(update.latitude, update.longitude)
            }
            exif.saveAttributes()

            // EXIF yazımı gerçekten tamamlanmış mı? Dosyayı değiştirmeden önce kontrol et.
            check(ExifInterface(edited.absolutePath)
                .getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL) == exifTime)

            // EditSaver'ın kullandığı orijinal yedeği varsa silme / üzerine yazma.
            if (!persistentBackup.isFile) {
                edits.ensureDir()
                val part = File(persistentBackup.path + ".metadata-part")
                try {
                    original.copyTo(part, overwrite = true)
                    check(part.renameTo(persistentBackup))
                    newBackup = true
                } finally {
                    part.delete()
                }
            }

            writing = true
            resolver.openOutputStream(mediaUri, "wt").use { output ->
                checkNotNull(output) { "JPEG yazılamadı" }
                edited.inputStream().use { input -> input.copyTo(output, 64 * 1024) }
            }

            // MediaStore tarih sütunu erişime göre reddedilebilir; EXIF yine korunur.
            runCatching {
                resolver.update(
                    mediaUri,
                    ContentValues().apply {
                        put(MediaStore.MediaColumns.DATE_TAKEN, update.epochMillis(zone))
                    },
                    null, null
                )
            }
            scan(context, item)
            true
        } catch (_: Exception) {
            val restored = if (writing && original.length() > 0L) {
                runCatching {
                    resolver.openOutputStream(mediaUri, "wt").use { output ->
                        checkNotNull(output)
                        original.inputStream().use { input -> input.copyTo(output, 64 * 1024) }
                    }
                    scan(context, item)
                }.isSuccess
            } else !writing
            // Geri yükleme başarısızsa eldeki tek sağlam yedeği asla silme.
            if (newBackup && restored) persistentBackup.delete()
            false
        } finally {
            original.delete()
            edited.delete()
        }
    }

    @Suppress("DEPRECATION")
    private fun scan(context: Context, item: MediaItem) {
        val path = runCatching {
            context.contentResolver.query(
                item.contentUri(), arrayOf(MediaStore.MediaColumns.DATA), null, null, null
            )?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        }.getOrNull() ?: return
        MediaScannerConnection.scanFile(context, arrayOf(path), null, null)
    }
}
