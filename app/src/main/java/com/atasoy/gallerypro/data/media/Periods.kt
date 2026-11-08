package com.atasoy.gallerypro.data.media

import androidx.compose.runtime.Immutable
import java.time.Instant
import java.time.ZoneId

/** Yıllar ve Aylar görünümündeki tek bir kart. */
@Immutable
data class Period(
    val year: Int,
    /** 1–12; yıl kartında null. */
    val month: Int?,
    val count: Int,
    /** Kapak: dönemin en yeni fotoğrafı (yoksa en yeni öğe). */
    val cover: MediaItem,
    /** Tümü ızgarasında dönemin ilk öğesinin sırası. */
    val firstIndex: Int
) {
    val key: String get() = if (month == null) "y$year" else "m$year-$month"
}

/**
 * Kronolojik sıralı listeyi (eskiden yeniye) dönemlere böler.
 * Saf Kotlin; birim testleri JVM'de çalışır.
 */
object PeriodGrouper {

    fun months(items: List<MediaItem>, zone: ZoneId): List<Period> =
        group(items, zone) { year, month -> year * 100 + month }

    fun years(items: List<MediaItem>, zone: ZoneId): List<Period> =
        group(items, zone) { year, _ -> year * 100 }

    private fun group(
        items: List<MediaItem>,
        zone: ZoneId,
        keyOf: (year: Int, month: Int) -> Int
    ): List<Period> {
        if (items.isEmpty()) return emptyList()
        val result = ArrayList<Period>()
        var currentKey = Int.MIN_VALUE
        var start = 0
        var cover: MediaItem? = null
        var count = 0

        fun flush(endExclusive: Int) {
            if (count == 0) return
            val first = items[start]
            val date = Instant.ofEpochMilli(first.takenAtMillis).atZone(zone)
            val isYear = currentKey % 100 == 0
            result += Period(
                year = date.year,
                month = if (isYear) null else date.monthValue,
                count = count,
                cover = cover ?: items[endExclusive - 1],
                firstIndex = start
            )
        }

        items.forEachIndexed { index, item ->
            val date = Instant.ofEpochMilli(item.takenAtMillis).atZone(zone)
            val key = keyOf(date.year, date.monthValue)
            if (key != currentKey) {
                flush(index)
                currentKey = key
                start = index
                cover = null
                count = 0
            }
            count++
            if (!item.isVideo) cover = item
        }
        flush(items.size)
        return result
    }
}
