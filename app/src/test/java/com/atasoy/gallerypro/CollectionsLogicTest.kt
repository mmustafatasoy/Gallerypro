package com.atasoy.gallerypro

import com.atasoy.gallerypro.data.collections.CollectionKeys
import com.atasoy.gallerypro.data.collections.CollectionsLogic
import com.atasoy.gallerypro.data.collections.MediaKinds
import com.atasoy.gallerypro.data.collections.MediaType
import com.atasoy.gallerypro.data.collections.SectionId
import com.atasoy.gallerypro.data.collections.SectionOrder
import com.atasoy.gallerypro.data.media.MediaItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class CollectionsLogicTest {

    private val utc = ZoneOffset.UTC
    private val now = LocalDateTime.of(2026, 11, 8, 18, 0).toInstant(utc).toEpochMilli()

    private fun at(y: Int, m: Int, d: Int, h: Int = 12): Long = LocalDateTime.of(y, m, d, h, 0).toInstant(utc).toEpochMilli()

    private fun item(
        id: Long,
        taken: Long,
        path: String = "DCIM/Camera/",
        video: Boolean = false,
        fav: Boolean = false,
        w: Int = 4000,
        h: Int = 3000,
        size: Long = id * 1000,
        name: String = "IMG_$id.jpg",
        mime: String = if (video) "video/mp4" else "image/jpeg",
        album: String? = null,
        added: Long = taken
    ) = MediaItem(
        id = id,
        isVideo = video,
        takenAtMillis = taken,
        durationMs = if (video) 8_000 else 0,
        mimeType = mime,
        width = w,
        height = h,
        album = album ?: path.trimEnd('/').substringAfterLast('/'),
        isFavorite = fav,
        displayName = name,
        sizeBytes = size,
        relativePath = path,
        addedMillis = added
    )

    // ---------------------------------------------------------------- türler

    @Test
    fun kinds_fromPathAndSize() {
        val shot = item(1, now, path = "Pictures/Screenshots/", w = 1080, h = 2400)
        val pano = item(2, now, w = 8000, h = 2000)
        val tall = item(3, now, w = 1080, h = 2400)
        val raw = item(4, now, name = "IMG_4.dng", mime = "image/x-adobe-dng")
        val download = item(5, now, path = "Download/")
        val recording = item(6, now, path = "DCIM/Screen recordings/", video = true)

        assertTrue(MediaKinds.isScreenshot(shot))
        assertFalse(MediaKinds.isCamera(shot))
        assertFalse(MediaKinds.isPanorama(shot))
        assertTrue(MediaKinds.isPanorama(pano))
        assertFalse("20:9 telefon oranı panorama değil", MediaKinds.isPanorama(tall))
        assertTrue(MediaKinds.isRaw(raw))
        assertTrue(MediaKinds.isImported(download))
        assertTrue(MediaKinds.isSaved(download))
        assertFalse(MediaKinds.isSaved(item(7, now)))
        assertTrue(MediaKinds.isScreenRecording(recording))
        assertFalse(MediaKinds.isCamera(recording))
    }

    // ---------------------------------------------------------------- Son Günler

    @Test
    fun recentDays_newestFirst_coverPrefersPhoto() {
        val items = listOf(
            item(1, at(2026, 9, 1)), // pencerenin dışında
            item(2, at(2026, 11, 6, 9)),
            item(3, at(2026, 11, 6, 10), video = true),
            item(4, at(2026, 11, 8, 8)),
            item(5, at(2026, 11, 8, 9), video = true)
        )
        val days = CollectionsLogic.recentDays(items, now, utc)
        assertEquals(2, days.size)
        assertEquals(CollectionsLogic.epochDay(at(2026, 11, 8), utc), days[0].epochDay)
        assertEquals(2, days[0].count)
        assertEquals(4L, days[0].cover.id)
        assertEquals(2L, days[1].cover.id)
        assertEquals(1L, CollectionsLogic.daysAgo(CollectionsLogic.epochDay(at(2026, 11, 7), utc), now, utc))
    }

    @Test
    fun recentDays_limitedToMax() {
        val items = (1..20).map { d -> item(d.toLong(), at(2026, 11, 8, 0) - (20 - d) * 86_400_000L) }
        val days = CollectionsLogic.recentDays(items, now, utc)
        assertEquals(CollectionsLogic.MaxRecentDays, days.size)
        assertEquals(20L, days.first().cover.id)
    }

    // ---------------------------------------------------------------- albümler ve kopyalar

    @Test
    fun albums_cameraFirstThenNewest() {
        val items = listOf(
            item(1, at(2026, 1, 1)),
            item(2, at(2026, 5, 1), path = "Pictures/WhatsApp/"),
            item(3, at(2026, 6, 1), path = "Download/"),
            item(4, at(2026, 2, 1))
        )
        val albums = CollectionsLogic.albums(items)
        assertEquals(listOf("DCIM/Camera/", "Download/", "Pictures/WhatsApp/"), albums.map { it.path })
        assertEquals(2, albums[0].count)
        assertEquals(4L, albums[0].cover.id)
        assertEquals("WhatsApp", albums[2].name)
    }

    @Test
    fun duplicates_sameSizeAndResolution() {
        val items = listOf(
            item(1, at(2026, 1, 1), size = 500),
            item(2, at(2026, 1, 2), size = 700),
            item(3, at(2026, 3, 1), size = 500),
            item(4, at(2026, 2, 1), size = 700, w = 3000, h = 4000), // döndürülmüş aynı fotoğraf
            item(5, at(2026, 4, 1), size = 900)
        )
        val dups = CollectionsLogic.duplicates(items)
        assertEquals(listOf(2L, 4L, 1L, 3L), dups.map { it.id })
        val content = CollectionsLogic.build(items, hiddenCount = 1, trashCount = 2, nowMillis = now, zone = utc)
        assertEquals(4, content.duplicateCount)
        assertEquals(1, content.hiddenCount)
        assertEquals(2, content.trashCount)
    }

    @Test
    fun build_countsAndMediaTypes() {
        val items = listOf(
            item(1, at(2026, 11, 1), fav = true),
            item(2, at(2026, 11, 2), video = true),
            item(3, at(2026, 11, 3), path = "Pictures/Screenshots/"),
            item(4, at(2026, 11, 4), path = "Download/"),
            item(5, at(2025, 1, 4), path = "Download/")
        )
        val content = CollectionsLogic.build(items, 0, 0, now, utc)
        assertEquals(1, content.favorites.count)
        assertEquals(1, content.videos.count)
        assertEquals(1, content.recentlySaved.count)
        assertEquals(4L, content.recentlySaved.cover?.id)
        assertEquals(2, content.importCount)
        assertEquals(listOf(MediaType.Videos, MediaType.Screenshots), content.mediaTypes.map { it.type })
    }

    @Test
    fun itemsFor_keys() {
        val a = item(1, at(2026, 11, 8, 9))
        val b = item(2, at(2026, 11, 7, 9), path = "Download/", fav = true)
        val hidden = item(3, at(2026, 11, 1))
        val trashed = item(4, at(2026, 10, 1))
        val items = listOf(b, a)
        fun ids(key: String) = CollectionsLogic.itemsFor(key, items, listOf(hidden), listOf(trashed), now, utc).map { it.id }

        assertEquals(listOf(1L), ids(CollectionKeys.day(CollectionsLogic.epochDay(a.takenAtMillis, utc))))
        assertEquals(listOf(2L), ids(CollectionKeys.album("Download/")))
        assertEquals(listOf(2L), ids(CollectionKeys.Favorites))
        assertEquals(listOf(3L), ids(CollectionKeys.Hidden))
        assertEquals(listOf(4L), ids(CollectionKeys.Trash))
        assertEquals(listOf(2L, 1L), ids(CollectionKeys.RecentDays))
        assertEquals(emptyList<Long>(), ids("bilinmeyen"))
    }

    @Test
    fun daysLeft_roundsUp() {
        assertEquals(30, CollectionsLogic.daysLeft(now + 30L * 86_400_000L, now))
        assertEquals(1, CollectionsLogic.daysLeft(now + 1000L, now))
        assertEquals(0, CollectionsLogic.daysLeft(now - 1000L, now))
        assertEquals(0, CollectionsLogic.daysLeft(0L, now))
    }

    // ---------------------------------------------------------------- bölüm sırası

    @Test
    fun sectionOrder_roundTripAndRepair() {
        val custom = SectionOrder.toggle(SectionOrder.move(SectionOrder.Default, SectionOrder.Default.indexOfFirst { it.id == SectionId.Utilities }, 0), SectionId.Albums)
        val decoded = SectionOrder.decode(SectionOrder.encode(custom))
        assertEquals(custom, decoded)
        assertEquals(SectionId.Utilities, decoded.first().id)
        assertFalse(decoded.first { it.id == SectionId.Albums }.visible)

        // Bilinmeyen ad atlanır, eksikler sona eklenir, tekrarlar yok sayılır.
        val repaired = SectionOrder.decode("Albums,Eski,-Pinned,Albums")
        assertEquals(
            listOf(SectionId.Albums, SectionId.Pinned, SectionId.RecentDays, SectionId.Places, SectionId.MediaTypes, SectionId.Utilities),
            repaired.map { it.id }
        )
        assertFalse(repaired[1].visible)
        assertEquals(SectionOrder.Default, SectionOrder.decode(null))
    }
}
