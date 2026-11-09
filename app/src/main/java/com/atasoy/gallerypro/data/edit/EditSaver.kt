package com.atasoy.gallerypro.data.edit

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.media.ExifInterface
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.atasoy.gallerypro.data.media.MediaItem
import com.atasoy.gallerypro.data.media.contentUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

enum class SaveResult { Saved, SavedAsCopy, Restored, Failed }

/**
 * Düzenlemeyi kaydeder.
 *
 * Yerinde kaydetme (JPEG/PNG/WebP): orijinal ilk düzenlemede birebir yedeklenir, her kayıtta
 * yedekten yeniden işlenir, sonuç aynı dosyanın üstüne yazılır. Ayarlar sıfırlanıp kaydedilirse
 * yedek geri yazılır. Yazma izni (MediaStore.createWriteRequest) ekrandan önceden alınır.
 *
 * Kopya olarak kaydetme: HEIC/RAW/GIF gibi yerinde yazılamayan türlerde veya konum izni
 * yokken (yedekte konum kaybolmasın diye) orijinale dokunulmaz, yanına JPEG kopya eklenir.
 */
object EditSaver {

    private val InPlaceMimes = setOf("image/jpeg", "image/png", "image/webp")

    /** Düzenlenmiş dosyaya taşınan EXIF etiketleri (yön hariç; yön piksellere işlenir). */
    private val ExifTags = listOf(
        "DateTime", "DateTimeOriginal", "DateTimeDigitized",
        "SubSecTime", "SubSecTimeOriginal", "SubSecTimeDigitized",
        "OffsetTime", "OffsetTimeOriginal", "OffsetTimeDigitized",
        "Make", "Model", "LensMake", "LensModel",
        "FNumber", "ApertureValue", "MaxApertureValue", "ExposureTime", "ShutterSpeedValue",
        "ISOSpeedRatings", "FocalLength", "FocalLengthIn35mmFilm", "ExposureBiasValue",
        "ExposureProgram", "MeteringMode", "Flash", "WhiteBalance", "DigitalZoomRatio",
        "SceneCaptureType", "BrightnessValue", "Artist", "Copyright", "ImageDescription",
        "GPSLatitude", "GPSLatitudeRef", "GPSLongitude", "GPSLongitudeRef",
        "GPSAltitude", "GPSAltitudeRef", "GPSTimeStamp", "GPSDateStamp",
        "GPSImgDirection", "GPSImgDirectionRef", "GPSSpeed", "GPSSpeedRef", "GPSProcessingMethod"
    )

    fun hasLocationAccess(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.ACCESS_MEDIA_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** Bu öğe aynı dosyanın üstüne kaydedilebilir mi? */
    fun canSaveInPlace(item: MediaItem, locationGranted: Boolean): Boolean =
        locationGranted && item.mimeType?.lowercase(Locale.ROOT) in InPlaceMimes

    /** Konum bilgisi silinmeden okunacak adres (izin varsa). */
    private fun originalUri(context: Context, uri: Uri): Uri =
        if (hasLocationAccess(context)) MediaStore.setRequireOriginal(uri) else uri

    suspend fun save(
        context: Context,
        item: MediaItem,
        params: EditParams,
        store: EditStore,
        inPlace: Boolean
    ): SaveResult = withContext(Dispatchers.IO) {
        runCatching {
            if (params.isIdentity) restore(context, item, store) else render(context, item, params, store, inPlace)
        }.getOrElse { SaveResult.Failed }
    }

    private fun restore(context: Context, item: MediaItem, store: EditStore): SaveResult {
        val backup = store.backupFile(item.id)
        if (backup.isFile) {
            val uri = item.contentUri()
            context.contentResolver.openOutputStream(uri, "wt")!!.use { out ->
                backup.inputStream().use { it.copyTo(out) }
            }
            scan(context, uri)
        }
        store.clear(item.id)
        return SaveResult.Restored
    }

    private fun render(
        context: Context,
        item: MediaItem,
        params: EditParams,
        store: EditStore,
        inPlace: Boolean
    ): SaveResult {
        val resolver = context.contentResolver
        val uri = item.contentUri()
        val backup = store.backupFile(item.id)

        if (inPlace && !backup.isFile) {
            store.ensureDir()
            val partial = File(backup.path + ".part")
            resolver.openInputStream(originalUri(context, uri))!!.use { input ->
                partial.outputStream().use { input.copyTo(it) }
            }
            check(partial.renameTo(backup))
        }

        val source = if (backup.isFile) ImageDecoder.createSource(backup) else ImageDecoder.createSource(resolver, uri)
        val full = EditRenderer.decodeFull(source)
        val out = try {
            EditRenderer.render(full, params)
        } finally {
            full.recycle()
        }

        val mime = item.mimeType?.lowercase(Locale.ROOT)
        val format = when {
            !inPlace -> Bitmap.CompressFormat.JPEG
            mime == "image/png" -> Bitmap.CompressFormat.PNG
            mime == "image/webp" -> webp()
            else -> Bitmap.CompressFormat.JPEG
        }
        val temp = File(context.cacheDir, "edit-${item.id}.tmp")
        try {
            temp.outputStream().use { stream -> check(out.compress(format, JpegQuality, stream)) }
            if (format == Bitmap.CompressFormat.JPEG) {
                runCatching {
                    val exifSource = if (backup.isFile) {
                        backup.inputStream()
                    } else {
                        resolver.openInputStream(originalUri(context, uri))!!
                    }
                    exifSource.use { copyExif(ExifInterface(it), temp, out.width, out.height) }
                }
            }
            return if (inPlace) {
                resolver.openOutputStream(uri, "wt")!!.use { stream -> temp.inputStream().use { it.copyTo(stream) } }
                store.save(item.id, params)
                scan(context, uri)
                SaveResult.Saved
            } else {
                insertCopy(context, item, temp)
                SaveResult.SavedAsCopy
            }
        } finally {
            out.recycle()
            temp.delete()
        }
    }

    @Suppress("DEPRECATION")
    private fun webp(): Bitmap.CompressFormat =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP

    private fun copyExif(from: ExifInterface, file: File, width: Int, height: Int) {
        val to = ExifInterface(file.absolutePath)
        for (tag in ExifTags) {
            from.getAttribute(tag)?.let { to.setAttribute(tag, it) }
        }
        to.setAttribute("Orientation", "1")
        to.setAttribute("ImageWidth", width.toString())
        to.setAttribute("ImageLength", height.toString())
        to.setAttribute("PixelXDimension", width.toString())
        to.setAttribute("PixelYDimension", height.toString())
        to.setAttribute("Software", "Gallerypro")
        to.saveAttributes()
    }

    /** Orijinalin yanına (aynı klasör, aynı çekim zamanı) JPEG kopya ekler. */
    private fun insertCopy(context: Context, item: MediaItem, file: File) {
        val resolver = context.contentResolver
        val base = item.displayName?.substringBeforeLast('.')?.takeIf { it.isNotBlank() } ?: "IMG_${item.id}"
        val path = item.relativePath?.takeIf { it.startsWith("DCIM/") || it.startsWith("Pictures/") } ?: "Pictures/Gallerypro/"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "${base}_duzenlendi.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, path)
            put(MediaStore.MediaColumns.DATE_TAKEN, item.takenAtMillis)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val target = resolver.insert(collection, values) ?: error("insert")
        try {
            resolver.openOutputStream(target, "w")!!.use { stream -> file.inputStream().use { it.copyTo(stream) } }
            resolver.update(target, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        } catch (t: Throwable) {
            resolver.delete(target, null, null)
            throw t
        }
    }

    /** Boyut ve tarih sütunları yenilensin diye dosyayı yeniden taratır. */
    @Suppress("DEPRECATION")
    private fun scan(context: Context, uri: Uri) {
        val path = runCatching {
            context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DATA), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull() ?: return
        MediaScannerConnection.scanFile(context, arrayOf(path), null, null)
    }

    private const val JpegQuality = 95
}
