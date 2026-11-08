package com.atasoy.gallerypro

import com.atasoy.gallerypro.data.media.Formatters
import com.atasoy.gallerypro.data.media.LibraryFilter
import com.atasoy.gallerypro.data.media.MediaItem
import com.atasoy.gallerypro.data.media.PeriodGrouper
import com.atasoy.gallerypro.data.media.filteredBy
import com.atasoy.gallerypro.feature.library.leadingSpacers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Locale

class LibraryLogicTest {

    private val utc = ZoneOffset.UTC

    private fun item(id: Long, y: Int, m: Int, d: Int, video: Boolean = false, fav: Boolean = false) = MediaItem(
        id = id,
        isVideo = video,
        takenAtMillis = LocalDateTime.of(y, m, d, 12, 0).toInstant(utc).toEpochMilli(),
        durationMs = if (video) 8_000 else 0,
        mimeType = if (video) "video/mp4" else "image/jpeg",
        width = 4000,
        height = 3000,
        album = "Camera",
        isFavorite = fav
    )

    private val sample = listOf(
        item(1, 2025, 12, 30),
        item(2, 2026, 10, 1),
        item(3, 2026, 10, 15, video = true),
        item(4, 2026, 11, 8, fav = true),
        item(5, 2026, 11, 8, video = true)
    )

    @Test
    fun months_groupChronologically() {
        val months = PeriodGrouper.months(sample, utc)
        assertEquals(listOf(2025 to 12, 2026 to 10, 2026 to 11), months.map { it.year to it.month })
        assertEquals(listOf(1, 2, 2), months.map { it.count })
        assertEquals(listOf(0, 1, 3), months.map { it.firstIndex })
    }

    @Test
    fun cover_prefersNewestPhotoOverVideo() {
        val months = PeriodGrouper.months(sample, utc)
        assertEquals(2L, months[1].cover.id)
        assertEquals(4L, months[2].cover.id)
    }

    @Test
    fun years_haveNoMonth() {
        val years = PeriodGrouper.years(sample, utc)
        assertEquals(listOf(2025, 2026), years.map { it.year })
        assertEquals(listOf(1, 4), years.map { it.count })
        assertNull(years[0].month)
    }

    @Test
    fun emptyInput_givesNoPeriods() {
        assertEquals(0, PeriodGrouper.months(emptyList(), utc).size)
    }

    @Test
    fun leadingSpacers_fillLastRow() {
        assertEquals(0, leadingSpacers(9, 3))
        assertEquals(2, leadingSpacers(10, 3))
        assertEquals(1, leadingSpacers(11, 3))
        assertEquals(0, leadingSpacers(7, 1))
        assertEquals(4, leadingSpacers(14, 9))
    }

    @Test
    fun duration_format() {
        assertEquals("0:08", Formatters.duration(8_000))
        assertEquals("1:12", Formatters.duration(72_000))
        assertEquals("1:02:03", Formatters.duration(3_723_000))
    }

    @Test
    fun filter_keepsRightItems() {
        assertEquals(listOf(4L), sample.filteredBy(LibraryFilter.Favorites).map { it.id })
        assertEquals(listOf(3L, 5L), sample.filteredBy(LibraryFilter.Videos).map { it.id })
        assertEquals(3, sample.filteredBy(LibraryFilter.Photos).size)
    }

    @Test
    fun monthName_turkish() {
        assertEquals("Kasım", Formatters.monthName(11, Locale.forLanguageTag("tr")))
    }
}
