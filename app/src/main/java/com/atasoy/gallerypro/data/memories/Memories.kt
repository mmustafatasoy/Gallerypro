package com.atasoy.gallerypro.data.memories

import androidx.compose.runtime.Immutable
import com.atasoy.gallerypro.data.clip.VectorEntry
import com.atasoy.gallerypro.data.clip.VectorMath
import com.atasoy.gallerypro.data.collections.MediaKinds
import com.atasoy.gallerypro.data.media.MediaItem
import com.atasoy.gallerypro.data.places.PlaceEntry
import com.atasoy.gallerypro.data.places.PlaceNames
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class MemoryKind { OnThisDay, Trip, Year }

/**
 * Cihazda kurulan bir anı. Başlık ekranda [kind]'a göre yazılır: gezi → [place] + tarih,
 * "Bu Gün" → bugünün tarihi, yıl → [year]. [itemIds] eskiden yeniye.
 */
@Immutable
data class Memory(
    val key: String,
    val kind: MemoryKind,
    val place: String = "",
    val startDay: Long = 0,
    val endDay: Long = 0,
    val year: Int = 0,
    val itemIds: List<Long>,
    val cover: MediaItem
)

/**
 * Anılar: geziler (evden uzakta, art arda günlerde en az [TripMin] öğe), "Bu Gün" (önceki yıllarda bugün,
 * en az [OnThisDayMin]) ve yılın özeti (geçmiş her yıl için en iyi [YearPick] öğe: önce favoriler, sonra yıla
 * eşit yayılmış, ekran görüntüsü yok, neredeyse aynı kareler bir kez). Saf Kotlin; JVM'de test edilir.
 */
object MemoryBuilder {

    const val TripMin = 8
    const val OnThisDayMin = 3
    const val YearMin = 20
    const val YearPick = 30
    const val MaxTrips = 20
    const val DuplicateSimilarity = 0.95f

    private const val Prefix = "memory:"

    fun isMemoryKey(key: String): Boolean = key.startsWith(Prefix)

    /** [items] gizlenenler hariç tüm öğeler (sıra önemli değil). */
    fun build(
        items: List<MediaItem>,
        places: Map<Long, PlaceEntry>,
        vectors: Map<Long, VectorEntry>,
        nowMillis: Long,
        zone: ZoneId
    ): List<Memory> {
        if (items.isEmpty()) return emptyList()
        val sorted = items.sortedBy { it.takenAtMillis }
        val today = day(nowMillis, zone)
        val out = ArrayList<Memory>()
        onThisDay(sorted, today, zone)?.let { out += it }
        out += trips(sorted, places, zone).take(MaxTrips)
        out += years(sorted, vectors, today, zone)
        return out
    }

    // ---------------------------------------------------------------- Bu Gün

    private fun onThisDay(sorted: List<MediaItem>, today: Long, zone: ZoneId): Memory? {
        val date = LocalDate.ofEpochDay(today)
        val list = sorted.filter {
            val d = LocalDate.ofEpochDay(day(it.takenAtMillis, zone))
            d.year < date.year && d.monthValue == date.monthValue && d.dayOfMonth == date.dayOfMonth &&
                !MediaKinds.isScreenshot(it)
        }
        if (list.size < OnThisDayMin) return null
        return Memory(
            key = "${Prefix}otd:${date.monthValue}-${date.dayOfMonth}",
            kind = MemoryKind.OnThisDay,
            startDay = today,
            endDay = today,
            itemIds = list.map { it.id },
            cover = coverOf(list)
        )
    }

    // ---------------------------------------------------------------- geziler

    /** Ev: en çok fotoğraf çekilen yer. Gezi: evden farklı aynı yerde geçen art arda günler (en çok 1 gün ara). */
    internal fun trips(sorted: List<MediaItem>, places: Map<Long, PlaceEntry>, zone: ZoneId): List<Memory> {
        val counts = HashMap<String, Int>()
        for (item in sorted) {
            val p = places[item.id]?.takeIf { it.hasPlace } ?: continue
            counts[p.groupKey] = (counts[p.groupKey] ?: 0) + 1
        }
        if (counts.isEmpty()) return emptyList()
        val home = counts.maxBy { it.value }.key

        // Her günün baskın yeri.
        val byDay = sorted.groupBy { day(it.takenAtMillis, zone) }
        val dayPlace = HashMap<Long, PlaceEntry>()
        for ((d, list) in byDay) {
            val dayCounts = HashMap<String, Int>()
            val sample = HashMap<String, PlaceEntry>()
            for (item in list) {
                val p = places[item.id]?.takeIf { it.hasPlace } ?: continue
                dayCounts[p.groupKey] = (dayCounts[p.groupKey] ?: 0) + 1
                sample.putIfAbsent(p.groupKey, p)
            }
            val best = dayCounts.maxByOrNull { it.value }?.key ?: continue
            if (best != home) dayPlace[d] = sample.getValue(best)
        }

        val result = ArrayList<Memory>()
        var start = -1L
        var end = -1L
        var place: PlaceEntry? = null
        fun flush() {
            val p = place ?: return
            val list = (start..end).flatMap { byDay[it].orEmpty() }
            if (list.size >= TripMin) {
                result += Memory(
                    key = "${Prefix}trip:$start:$end:${p.groupKey}",
                    kind = MemoryKind.Trip,
                    place = PlaceNames.groupTitle(p),
                    startDay = start,
                    endDay = end,
                    itemIds = list.map { it.id },
                    cover = coverOf(list)
                )
            }
        }
        for (d in dayPlace.keys.sorted()) {
            val p = dayPlace.getValue(d)
            val current = place
            if (current != null && current.groupKey == p.groupKey && d - end <= 2) {
                end = d
            } else {
                flush()
                start = d
                end = d
                place = p
            }
        }
        flush()
        return result.sortedByDescending { it.endDay }
    }

    // ---------------------------------------------------------------- yılın özeti

    internal fun years(sorted: List<MediaItem>, vectors: Map<Long, VectorEntry>, today: Long, zone: ZoneId): List<Memory> {
        val thisYear = LocalDate.ofEpochDay(today).year
        val byYear = sorted
            .filter { !MediaKinds.isScreenshot(it) && !MediaKinds.isScreenRecording(it) }
            .groupBy { LocalDate.ofEpochDay(day(it.takenAtMillis, zone)).year }
        return byYear.filter { (year, list) -> year < thisYear && list.size >= YearMin }
            .toSortedMap(compareByDescending { it })
            .map { (year, list) ->
                val picked = pick(list, vectors)
                Memory(
                    key = "${Prefix}year:$year",
                    kind = MemoryKind.Year,
                    year = year,
                    itemIds = picked.map { it.id },
                    cover = coverOf(picked)
                )
            }
    }

    /** Önce favoriler, sonra yıla eşit aralıklarla; neredeyse aynı kareler atlanır. Eskiden yeniye döner. */
    internal fun pick(list: List<MediaItem>, vectors: Map<Long, VectorEntry>, count: Int = YearPick): List<MediaItem> {
        val chosen = ArrayList<MediaItem>(count)
        fun duplicate(item: MediaItem): Boolean {
            val v = vectors[item.id]?.vector ?: return false
            return chosen.any { other ->
                val o = vectors[other.id]?.vector
                o != null && VectorMath.dot(v, o) >= DuplicateSimilarity
            }
        }
        fun tryAdd(item: MediaItem) {
            if (chosen.size < count && chosen.none { it.id == item.id } && !duplicate(item)) chosen += item
        }
        list.filter { it.isFavorite }.forEach(::tryAdd)
        if (chosen.size < count && list.isNotEmpty()) {
            // Eşit aralıklı örnekler; yetmezse aradakilerle doldurulur.
            val slots = count - chosen.size
            val step = list.size.toDouble() / slots
            for (i in 0 until slots) tryAdd(list[(i * step).toInt().coerceIn(0, list.lastIndex)])
            if (chosen.size < count) list.forEach(::tryAdd)
        }
        return chosen.sortedBy { it.takenAtMillis }
    }

    private fun coverOf(list: List<MediaItem>): MediaItem =
        list.firstOrNull { it.isFavorite && !it.isVideo } ?: list.firstOrNull { !it.isVideo } ?: list.first()

    private fun day(millis: Long, zone: ZoneId): Long =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().toEpochDay()
}

/** Anı kartlarındaki tarih yazıları. Saf Kotlin. */
object MemoryFormat {

    /** "12 Temmuz 2025", "12–15 Temmuz 2025", "28 Haziran – 3 Temmuz 2025", "30 Aralık 2024 – 2 Ocak 2025". */
    fun range(startDay: Long, endDay: Long, locale: Locale): String {
        val a = LocalDate.ofEpochDay(startDay)
        val b = LocalDate.ofEpochDay(endDay)
        val full = DateTimeFormatter.ofPattern("d MMMM yyyy", locale)
        return when {
            a == b -> full.format(a)
            a.year == b.year && a.month == b.month -> "${a.dayOfMonth}–${full.format(b)}"
            a.year == b.year -> "${DateTimeFormatter.ofPattern("d MMMM", locale).format(a)} – ${full.format(b)}"
            else -> "${full.format(a)} – ${full.format(b)}"
        }
    }

    /** "10 Kasım" */
    fun dayMonth(day: Long, locale: Locale): String =
        DateTimeFormatter.ofPattern("d MMMM", locale).format(LocalDate.ofEpochDay(day))
}
