package com.atasoy.gallerypro.data.edit

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 7d: Yalnızca standart JPEG üzerinde EXIF tarih/konum düzenlenebilir. */
data class MetadataUpdate(
    val dateTime: LocalDateTime,
    val latitude: Double?,
    val longitude: Double?,
    val removeLocation: Boolean
) {
    fun epochMillis(zone: ZoneId): Long = dateTime.atZone(zone).toInstant().toEpochMilli()
    fun exifDateTime(): String = dateTime.format(DateTimeFormatter.ofPattern("uuuu:MM:dd HH:mm:ss", Locale.ROOT))
}

object MetadataEditLogic {
    private val displayFormat = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm", Locale.ROOT)

    fun isSupported(mime: String?, video: Boolean): Boolean =
        !video && mime?.lowercase(Locale.ROOT) == "image/jpeg"

    fun formatDate(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(millis).atZone(zone).format(displayFormat)

    /**
     * Alanlar boşsa konum değiştirilmez; 'Konumu kaldır' işaretliyse EXIF GPS silinir.
     * Geçersiz tarih, koordinatlardan birinin eksik olması veya aralık dışı değerler
     * için null döner; veriye dokunulmaz.
     */
    fun parse(
        date: String,
        latitude: String,
        longitude: String,
        removeLocation: Boolean
    ): MetadataUpdate? {
        val time = runCatching { LocalDateTime.parse(date.trim(), displayFormat) }.getOrNull()
            ?: return null
        if (time.year !in 1900..2100) return null
        if (removeLocation) return MetadataUpdate(time, null, null, true)
        val lat = latitude.trim()
        val lon = longitude.trim()
        if (lat.isEmpty() && lon.isEmpty()) return MetadataUpdate(time, null, null, false)
        if (lat.isEmpty() || lon.isEmpty()) return null
        val latValue = lat.replace(',', '.').toDoubleOrNull() ?: return null
        val lonValue = lon.replace(',', '.').toDoubleOrNull() ?: return null
        if (!latValue.isFinite() || !lonValue.isFinite() ||
            latValue !in -90.0..90.0 || lonValue !in -180.0..180.0
        ) return null
        return MetadataUpdate(time, latValue, lonValue, false)
    }
}
