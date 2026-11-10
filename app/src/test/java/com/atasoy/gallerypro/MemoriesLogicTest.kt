package com.atasoy.gallerypro

import com.atasoy.gallerypro.data.clip.VectorEntry
import com.atasoy.gallerypro.data.media.MediaItem
import com.atasoy.gallerypro.data.memories.MemoryBuilder
import com.atasoy.gallerypro.data.memories.MemoryFormat
import com.atasoy.gallerypro.data.memories.MemoryKind
import com.atasoy.gallerypro.data.places.PlaceEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

/** Anılar: Bu Gün, geziler, yılın özeti ve tarih yazıları. */
class MemoriesLogicTest {

    private val zone = ZoneId.of("Europe/Istanbul")
    private val tr = Locale.forLanguageTag("tr")

    private fun at(y: Int, m: Int, d: Int, h: Int = 12): Long =
        LocalDateTime.of(y, m, d, h, 0).atZone(zone).toInstant().toEpochMilli()

    private fun item(id: Long, taken: Long, fav: Boolean = false, path: String = "DCIM/Camera/") = MediaItem(
        id = id, isVideo = false, takenAtMillis = taken, durationMs = 0L, mimeType = "image/jpeg",
        width = 4000, height = 3000, album = "Camera", isFavorite = fav, relativePath = path
    )

    private fun place(admin: String) = PlaceEntry(0, 1.0, 1.0, admin, admin, "TR")

    private val now = at(2026, 11, 10)

    @Test
    fun onThisDay_previousYearsSameDate() {
        val items = listOf(
            item(1, at(2024, 11, 10)), item(2, at(2025, 11, 10, 8)), item(3, at(2023, 11, 10)),
            item(4, at(2026, 11, 10)), // bu yıl: sayılmaz
            item(5, at(2025, 11, 9)),
            item(6, at(2022, 11, 10), path = "Pictures/Screenshots/")
        )
        val memories = MemoryBuilder.build(items, emptyMap(), emptyMap(), now, zone)
        val otd = memories.single { it.kind == MemoryKind.OnThisDay }
        assertEquals(listOf(3L, 1L, 2L), otd.itemIds)
        assertEquals("10 Kasım", MemoryFormat.dayMonth(otd.startDay, tr))
    }

    @Test
    fun onThisDay_needsThreePhotos() {
        val items = listOf(item(1, at(2024, 11, 10)), item(2, at(2025, 11, 10)))
        assertTrue(MemoryBuilder.build(items, emptyMap(), emptyMap(), now, zone).none { it.kind == MemoryKind.OnThisDay })
    }

    @Test
    fun trips_awayFromHomeConsecutiveDays() {
        val items = ArrayList<MediaItem>()
        val places = HashMap<Long, PlaceEntry>()
        var id = 1L
        // Ev: İstanbul, 30 fotoğraf.
        repeat(30) { items += item(id, at(2025, 3, 1 + it % 20)); places[id] = place("Istanbul"); id++ }
        // Gezi: Antalya 12–15 Temmuz, günde 3 fotoğraf (12 öğe), 14'ü boş.
        for (d in listOf(12, 13, 15)) repeat(4) { items += item(id, at(2025, 7, d, 9 + it)); places[id] = place("Antalya"); id++ }
        // Kısa çıkış: Bursa, 1 gün, 3 fotoğraf → gezi sayılmaz.
        repeat(3) { items += item(id, at(2025, 9, 5, 10 + it)); places[id] = place("Bursa"); id++ }

        val trips = MemoryBuilder.build(items, places, emptyMap(), now, zone).filter { it.kind == MemoryKind.Trip }
        assertEquals(1, trips.size)
        val trip = trips.single()
        assertEquals("Antalya", trip.place)
        assertEquals(12, trip.itemIds.size)
        assertEquals("12–15 Temmuz 2025", MemoryFormat.range(trip.startDay, trip.endDay, tr))
    }

    @Test
    fun years_pastYearsWithEnoughPhotos_favoritesFirstNoDuplicates() {
        val items = ArrayList<MediaItem>()
        // 40 fotoğraf, yıl boyunca 9'ar gün arayla (kimlik arttıkça tarih artar).
        repeat(40) { items += item(it + 1L, at(2025, 1, 1) + it * 9L * 86_400_000L, fav = it == 5) }
        repeat(10) { items += item(100L + it, at(2024, 1, 1 + it)) } // 10 < 20: özet yok
        repeat(5) { items += item(200L + it, at(2026, 2, 1 + it)) } // bu yıl: yok
        // 7 ve 8 neredeyse aynı kare.
        val v = FloatArray(4) { if (it == 0) 1f else 0f }
        val vectors = mapOf(7L to VectorEntry(0, v), 8L to VectorEntry(0, v.copyOf()))

        val years = MemoryBuilder.build(items, emptyMap(), vectors, now, zone).filter { it.kind == MemoryKind.Year }
        assertEquals(listOf(2025), years.map { it.year })
        val ids = years.single().itemIds
        assertEquals(30, ids.size)
        assertTrue(6L in ids)
        assertTrue(!(7L in ids && 8L in ids))
        assertEquals(ids.sorted(), ids) // eskiden yeniye (kimlikler tarihle artıyor)
    }

    @Test
    fun format_ranges() {
        fun d(y: Int, m: Int, dd: Int) = LocalDate.of(y, m, dd).toEpochDay()
        assertEquals("12 Temmuz 2025", MemoryFormat.range(d(2025, 7, 12), d(2025, 7, 12), tr))
        assertEquals("28 Haziran – 3 Temmuz 2025", MemoryFormat.range(d(2025, 6, 28), d(2025, 7, 3), tr))
        assertEquals("30 Aralık 2024 – 2 Ocak 2025", MemoryFormat.range(d(2024, 12, 30), d(2025, 1, 2), tr))
    }
}
