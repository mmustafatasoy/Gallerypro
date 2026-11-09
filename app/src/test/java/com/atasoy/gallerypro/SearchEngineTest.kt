package com.atasoy.gallerypro

import com.atasoy.gallerypro.data.index.IndexEntry
import com.atasoy.gallerypro.data.index.SearchEngine
import com.atasoy.gallerypro.data.index.SearchKind
import com.atasoy.gallerypro.data.index.SearchText
import com.atasoy.gallerypro.data.media.MediaItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class SearchEngineTest {

    private val zone = ZoneId.of("Europe/Istanbul")

    private fun at(y: Int, m: Int, d: Int): Long =
        LocalDateTime.of(y, m, d, 12, 0).atZone(zone).toInstant().toEpochMilli()

    private fun item(id: Long, taken: Long, video: Boolean = false, album: String? = "Camera", path: String = "DCIM/Camera/", fav: Boolean = false) =
        MediaItem(
            id = id, isVideo = video, takenAtMillis = taken, durationMs = 0L, mimeType = if (video) "video/mp4" else "image/jpeg",
            width = 4000, height = 3000, album = album, isFavorite = fav, relativePath = path
        )

    private val items = listOf(
        item(1, at(2024, 6, 10)),
        item(2, at(2025, 7, 2)),
        item(3, at(2025, 7, 20), video = true),
        item(4, at(2026, 1, 5), album = "Screenshots", path = "Pictures/Screenshots/"),
        item(5, at(2026, 11, 9), fav = true)
    )
    private val index = mapOf(
        1L to IndexEntry(1, 0, listOf("Dog", "Grass"), ""),
        2L to IndexEntry(2, 0, listOf("Beach", "Sky"), ""),
        3L to IndexEntry(3, 0, listOf("Beach", "Dog"), ""),
        4L to IndexEntry(4, 0, emptyList(), "Fatura tutarı: 1.250 TL\nSon ödeme tarihi"),
        5L to IndexEntry(5, 0, listOf("Cat"), "")
    )
    private val docs = SearchEngine.prepare(items, index, mapOf(5L to "Minnoş uyurken"))
    private val now = at(2026, 11, 9)

    private fun ids(q: String) = SearchEngine.search(q, docs, zone, now).map { it.id }

    @Test
    fun normalize_turkishAndCase() {
        assertEquals("kopek isik cicek", SearchText.normalize("KÖPEK  Işık, çiçek!"))
        assertEquals("istanbul", SearchText.normalize("İSTANBUL"))
        assertEquals(listOf("gun", "batimi"), SearchText.tokens("  gün-batımı "))
        assertEquals("", SearchText.normalize("..."))
    }

    @Test
    fun parse_dateAndKind() {
        val q = SearchEngine.parse("Temmuz 2025 videolar deniz")
        assertEquals(2025, q.year)
        assertEquals(7, q.month)
        assertEquals(SearchKind.Videos, q.kind)
        assertEquals(listOf("deniz"), q.words)
        val s = SearchEngine.parse("ekran görüntüleri fatura")
        assertTrue(s.screenshots)
        assertEquals(listOf("fatura"), s.words)
        assertNull(SearchEngine.parse("kask").month)
        assertTrue(SearchEngine.parse("  ").isEmpty)
    }

    @Test
    fun search_labelsInTurkishAndEnglish() {
        assertEquals(setOf(1L, 3L), ids("köpek").toSet())
        assertEquals(setOf(1L, 3L), ids("dog").toSet())
        assertEquals(setOf(2L, 3L), ids("plaj").toSet())
        // Önek ve Türkçe ek: "kumsalda" → "kumsal".
        assertEquals(setOf(2L, 3L), ids("kumsalda").toSet())
        assertEquals(setOf(1L, 3L), ids("köpekler").toSet())
    }

    @Test
    fun search_allWordsMustMatch() {
        assertEquals(listOf(3L), ids("köpek plaj"))
        assertEquals(emptyList<Long>(), ids("köpek kedi"))
    }

    @Test
    fun search_filters() {
        assertEquals(listOf(3L), ids("plaj video"))
        assertEquals(listOf(2L), ids("plaj fotoğraflar"))
        assertEquals(listOf(3L, 2L), ids("temmuz 2025"))
        assertEquals(listOf(5L), ids("bugün"))
        assertEquals(listOf(5L), ids("favoriler"))
        assertEquals(listOf(4L), ids("ekran görüntüsü"))
    }

    @Test
    fun search_ocrCaptionAndAlbum() {
        assertEquals(listOf(4L), ids("fatura"))
        assertEquals(listOf(4L), ids("ödeme"))
        assertEquals(listOf(5L), ids("minnos"))
        assertEquals(listOf(4L), ids("screenshots"))
    }

    @Test
    fun search_ranking_labelBeforeText() {
        val docs2 = SearchEngine.prepare(
            listOf(item(10, at(2026, 1, 1)), item(11, at(2025, 1, 1))),
            mapOf(10L to IndexEntry(10, 0, emptyList(), "deniz otobüsü saatleri"), 11L to IndexEntry(11, 0, listOf("Sea"), "")),
            emptyMap()
        )
        // Etiket eşleşmesi (eski fotoğraf) yazı eşleşmesinin (yeni fotoğraf) önüne geçer.
        assertEquals(listOf(11L, 10L), SearchEngine.search("deniz", docs2, zone, now).map { it.id })
    }

    @Test
    fun topLabels_byFrequency() {
        assertEquals(listOf("Beach", "Dog"), SearchEngine.topLabels(index.values, 2))
    }
}
