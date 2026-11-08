package com.atasoy.gallerypro.data.media

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.provider.MediaStore
import androidx.compose.runtime.Immutable
import androidx.core.content.ContextCompat
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** Bilgi panelinde gösterilen çekim ayrıntıları. Bilinmeyen alanlar null. */
@Immutable
data class MediaDetails(
    val make: String? = null,
    val model: String? = null,
    val lens: String? = null,
    val focalLength35mm: Int? = null,
    val focalLength: Double? = null,
    val fNumber: Double? = null,
    val exposureTime: Double? = null,
    val iso: Int? = null,
    val exposureBias: Double? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    /** Konum izni yoksa Android konumu gizler; panel izin isteyebilir. */
    val locationHidden: Boolean = false
) {
    val hasExposure: Boolean
        get() = iso != null || fNumber != null || exposureTime != null
    val hasLocation: Boolean
        get() = latitude != null && longitude != null
}

object MediaDetailsReader {

    fun hasLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_MEDIA_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /** Disk okur; IO iş parçacığında çağrılmalı. */
    fun read(context: Context, item: MediaItem): MediaDetails {
        val withLocation = hasLocationPermission(context)
        return runCatching {
            if (item.isVideo) readVideo(context, item, withLocation) else readImage(context, item, withLocation)
        }.getOrElse { MediaDetails(locationHidden = !withLocation) }
    }

    private fun readImage(context: Context, item: MediaItem, withLocation: Boolean): MediaDetails {
        val resolver = context.contentResolver
        val uri = item.contentUri()
        // Konum izni varsa orijinal (konumu gizlenmemiş) dosya istenir.
        val opened = if (withLocation) {
            runCatching { resolver.openFileDescriptor(MediaStore.setRequireOriginal(uri), "r") }.getOrNull()
                ?: resolver.openFileDescriptor(uri, "r")
        } else {
            resolver.openFileDescriptor(uri, "r")
        }
        val pfd = opened ?: return MediaDetails(locationHidden = !withLocation)

        return pfd.use { descriptor ->
            val exif = ExifInterface(descriptor.fileDescriptor)
            fun text(tag: String) = exif.getAttribute(tag)?.trim()?.takeIf { s -> s.isNotEmpty() }
            fun positiveDouble(tag: String) = exif.getAttributeDouble(tag, 0.0).takeIf { v -> v > 0.0 }
            fun positiveInt(tag: String) = exif.getAttributeInt(tag, 0).takeIf { v -> v > 0 }

            // Yerleşik EXIF okuyucusunda lens ve ISO etiketlerinin sabiti yok, adlarıyla okunur.
            // Konum float dizisi olarak gelir.
            val latLong = FloatArray(2)
            val hasLatLong = withLocation && exif.getLatLong(latLong)
            MediaDetails(
                make = text(ExifInterface.TAG_MAKE),
                model = text(ExifInterface.TAG_MODEL),
                lens = text("LensModel"),
                focalLength35mm = positiveInt(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM),
                focalLength = positiveDouble(ExifInterface.TAG_FOCAL_LENGTH),
                fNumber = positiveDouble(ExifInterface.TAG_F_NUMBER),
                exposureTime = positiveDouble(ExifInterface.TAG_EXPOSURE_TIME),
                iso = positiveInt("ISOSpeedRatings"),
                exposureBias = if (exif.getAttribute(ExifInterface.TAG_EXPOSURE_BIAS_VALUE) != null) {
                    exif.getAttributeDouble(ExifInterface.TAG_EXPOSURE_BIAS_VALUE, 0.0)
                } else {
                    null
                },
                latitude = if (hasLatLong && latLong[0] != 0f) latLong[0].toDouble() else null,
                longitude = if (hasLatLong && latLong[1] != 0f) latLong[1].toDouble() else null,
                locationHidden = !withLocation
            )
        }
    }

    private fun readVideo(context: Context, item: MediaItem, withLocation: Boolean): MediaDetails {
        if (!withLocation) return MediaDetails(locationHidden = true)
        val retriever = MediaMetadataRetriever()
        return try {
            val uri = runCatching { MediaStore.setRequireOriginal(item.contentUri()) }.getOrDefault(item.contentUri())
            retriever.setDataSource(context, uri)
            val location = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION)
                ?.let(DetailFormat::parseIso6709)
            MediaDetails(latitude = location?.first, longitude = location?.second)
        } finally {
            retriever.release()
        }
    }
}

/** Saf biçimlendirme işlevleri (birim testli). */
object DetailFormat {

    private fun decimal(pattern: String, locale: Locale) = DecimalFormat(pattern, DecimalFormatSymbols(locale))

    /** 1.8 → "ƒ1,8", 2.0 → "ƒ2" */
    fun fNumber(value: Double, locale: Locale): String = "ƒ" + decimal("0.#", locale).format(value)

    /** 0.008 → "1/125 s", 2.5 → "2,5 s" */
    fun exposure(seconds: Double, locale: Locale): String =
        if (seconds >= 1.0) {
            decimal("0.#", locale).format(seconds) + " s"
        } else {
            "1/" + (1.0 / seconds).roundToLong() + " s"
        }

    fun focal(mm: Double): String = "${mm.roundToInt()} mm"

    /** 0.0 → "0 ev", 0.33 → "+0,3 ev", -1.0 → "-1 ev" */
    fun bias(ev: Double, locale: Locale): String {
        if (abs(ev) < 0.05) return "0 ev"
        val sign = if (ev > 0) "+" else "-"
        return sign + decimal("0.#", locale).format(abs(ev)) + " ev"
    }

    /** 4000×3000 → "12", 1920×1080 → "2,1" */
    fun megapixels(width: Int, height: Int, locale: Locale): String {
        val mp = width.toLong() * height / 1_000_000.0
        return if (mp >= 10) mp.roundToInt().toString() else decimal("0.#", locale).format(mp)
    }

    /** SI birimleri (1000), Android ve iOS ile aynı. */
    fun fileSize(bytes: Long, locale: Locale): String = when {
        bytes >= 1_000_000_000L -> decimal("0.##", locale).format(bytes / 1_000_000_000.0) + " GB"
        bytes >= 1_000_000L -> decimal("0.#", locale).format(bytes / 1_000_000.0) + " MB"
        else -> (bytes / 1000.0).roundToInt().coerceAtLeast(1).toString() + " KB"
    }

    /** "41,0082° K · 28,9784° D" */
    fun coordinates(
        latitude: Double,
        longitude: Double,
        locale: Locale,
        north: String,
        south: String,
        east: String,
        west: String
    ): String {
        val f = decimal("0.0000", locale)
        val lat = f.format(abs(latitude)) + "° " + if (latitude >= 0) north else south
        val lon = f.format(abs(longitude)) + "° " + if (longitude >= 0) east else west
        return "$lat · $lon"
    }

    /** Videolardaki ISO 6709 konumu: "+41.0082+028.9784/" → (41.0082, 28.9784) */
    fun parseIso6709(value: String): Pair<Double, Double>? {
        val match = Regex("""([+-]\d+(?:\.\d+)?)([+-]\d+(?:\.\d+)?)""").find(value) ?: return null
        val lat = match.groupValues[1].toDoubleOrNull() ?: return null
        val lon = match.groupValues[2].toDoubleOrNull() ?: return null
        return if (lat == 0.0 && lon == 0.0) null else lat to lon
    }

    /** ("samsung", "SM-S911B") → "Samsung SM-S911B"; model markayla başlıyorsa tekrar etmez. */
    fun deviceName(make: String?, model: String?): String? {
        val m = make?.trim()?.takeIf { it.isNotEmpty() }
            ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        val d = model?.trim()?.takeIf { it.isNotEmpty() }
        return when {
            m == null -> d
            d == null -> m
            d.startsWith(m, ignoreCase = true) -> d
            else -> "$m $d"
        }
    }

    /** "image/jpeg" → "JPEG" */
    fun formatBadge(mimeType: String?): String? {
        val mime = mimeType?.lowercase(Locale.ROOT) ?: return null
        return when (mime) {
            "image/jpeg", "image/jpg" -> "JPEG"
            "image/heic", "image/heif", "image/heic-sequence", "image/heif-sequence" -> "HEIF"
            "image/avif" -> "AVIF"
            "image/x-adobe-dng" -> "RAW"
            "video/quicktime" -> "MOV"
            "video/x-matroska" -> "MKV"
            "video/3gpp" -> "3GP"
            else -> mime.substringAfter('/').substringAfter("x-").uppercase(Locale.ROOT).take(5)
        }
    }
}
