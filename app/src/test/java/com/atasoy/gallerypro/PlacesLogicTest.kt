package com.atasoy.gallerypro

import com.atasoy.gallerypro.data.index.SearchEngine
import com.atasoy.gallerypro.data.media.MediaItem
import com.atasoy.gallerypro.data.places.CityIndex
import com.atasoy.gallerypro.data.places.PlaceEntry
import com.atasoy.gallerypro.data.places.PlaceNames
import com.atasoy.gallerypro.data.places.PlacesLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.util.Locale

/** Yerler: en yakın şehir, ad gösterimi, gruplama ve yer adıyla arama. */
class PlacesLogicTest {

    private val cities = CityIndex(
        CityIndex.parse(
            sequenceOf(
                "Istanbul\tIstanbul\tTR\t41.0138\t28.9497",
                "Kadıköy\tIstanbul\tTR\t40.9819\t29.0576",
                "Antalya\tAntalya\tTR\t36.9081\t30.6956",
                "Kemer\tAntalya\tTR\t36.5978\t30.5604",
                "Munich\tBavaria\tDE\t48.1374\t11.5755",
                "bozuk satır",
                "Eksik\tX\tTR\tenlem\t30.0"
            )
        )
    )

    @Test
    fun cityIndex_parsesAndFindsNearest() {
        assertEquals(5, cities.size)
        assertEquals("Kadıköy", cities.nearest(40.99, 29.03)?.name)
        assertEquals("Istanbul", cities.nearest(41.02, 28.95)?.name)
        assertEquals("Kemer", cities.nearest(36.60, 30.55)?.name)
        assertEquals("Munich", cities.nearest(48.14, 11.60)?.name)
    }

    @Test
    fun cityIndex_farAwayOrMissingIsNull() {
        // Karadeniz'in ortası: en yakın şehir 60 km'den uzak.
        assertNull(cities.nearest(43.0, 34.0))
        // Konumu silinmiş fotoğraflar 0,0 gelir.
        assertNull(cities.nearest(0.0, 0.0))
        assertNull(cities.nearest(Double.NaN, 29.0))
    }

    @Test
    fun cityIndex_distance() {
        val km = CityIndex.distanceKm(41.0138, 28.9497, 36.9081, 30.6956)
        assertTrue("İstanbul–Antalya ~480 km, bulunan $km", km in 470.0..490.0)
    }

    private fun place(city: String, admin: String, country: String) =
        PlaceEntry(modified = 0, lat = 1.0, lon = 1.0, city = city, admin = admin, country = country)

    @Test
    fun placeNames_lineAndSearchable() {
        assertEquals("Kadıköy, Istanbul", PlaceNames.cityLine(place("Kadıköy", "Istanbul", "TR")))
        assertEquals("Istanbul", PlaceNames.cityLine(place("İstanbul", "Istanbul", "TR")))
        assertEquals("Almanya", PlaceNames.country("DE", Locale.forLanguageTag("tr")))
        val text = PlaceNames.searchable(place("Munich", "Bavaria", "DE"))
        assertTrue(text, text.contains("Munich") && text.contains("Bavaria") && text.contains("Almanya") && text.contains("Germany"))
        assertEquals("", PlaceNames.searchable(PlaceEntry(0, null, null, "", "", "")))
    }

    private fun item(id: Long, taken: Long) = MediaItem(
        id = id, isVideo = false, takenAtMillis = taken, durationMs = 0L, mimeType = "image/jpeg",
        width = 4000, height = 3000, album = "Camera", isFavorite = false, relativePath = "DCIM/Camera/"
    )

    private val items = listOf(item(1, 100), item(2, 200), item(3, 300), item(4, 400), item(5, 500))
    private val places = mapOf(
        1L to place("Kadıköy", "Istanbul", "TR"),
        2L to place("Istanbul", "Istanbul", "TR"),
        3L to place("Kemer", "Antalya", "TR"),
        4L to PlaceEntry(0, null, null, "", "", "")
    )

    @Test
    fun placesLogic_groupsByRegionNewestFirst() {
        val groups = PlacesLogic.groups(items, places)
        assertEquals(listOf("Antalya", "Istanbul"), groups.map { it.title })
        assertEquals(2, groups[1].count)
        assertEquals(2L, groups[1].cover.id)
        val key = PlacesLogic.collectionKey(groups[1].key)
        assertEquals(groups[1].key, PlacesLogic.groupOf(key))
        assertEquals(listOf(1L, 2L), PlacesLogic.itemsIn(groups[1].key, items, places).map { it.id })
        assertNull(PlacesLogic.groupOf("album:DCIM/"))
    }

    @Test
    fun search_findsByCityRegionAndCountry() {
        val docs = SearchEngine.prepare(items, emptyMap(), emptyMap(), places)
        val zone = ZoneId.of("Europe/Istanbul")
        fun ids(q: String) = SearchEngine.search(q, docs, zone, 1000).map { it.id }.sorted()
        assertEquals(listOf(1L, 2L), ids("istanbul"))
        assertEquals(listOf(1L, 2L), ids("İSTANBUL"))
        assertEquals(listOf(1L), ids("kadıköy"))
        assertEquals(listOf(3L), ids("Antalya"))
        // Ülke adı cihaz diline göre: Türkçe "Türkiye" (İngilizcesi de CLDR 42'den beri "Türkiye").
        assertEquals(listOf(1L, 2L, 3L), ids("Türkiye"))
        assertEquals(listOf(1L, 2L), ids("istanbul türkiye"))
    }
}
