package com.atasoy.gallerypro.data.places

import android.content.Context
import androidx.compose.runtime.Immutable
import com.atasoy.gallerypro.data.media.MediaItem
import java.util.Locale
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

/** Çevrimdışı şehir listesindeki bir yer (GeoNames, nüfusu 5.000+). [admin] il/eyalet, [country] ISO kodu. */
class City(val name: String, val admin: String, val country: String, val lat: Double, val lon: Double)

/**
 * Bir öğenin çekildiği yer. Konumu olmayan öğeler de boş olarak kaydedilir (her seferinde yeniden okunmasın).
 * [country] iki harfli ISO kodu; adı cihazın diline göre [PlaceNames.country] ile çevrilir.
 */
@Immutable
data class PlaceEntry(
    val modified: Long,
    val lat: Double?,
    val lon: Double?,
    val city: String,
    val admin: String,
    val country: String,
    val version: Int = PlaceVersion
) {
    val hasPlace: Boolean get() = city.isNotEmpty() || admin.isNotEmpty()

    /** Yerler bölümündeki grup: il/eyalet + ülke (ilçeler ayrı ayrı listelenmesin). */
    val groupKey: String get() = "${admin.ifEmpty { city }}|$country"
}

/** Yer okuma yöntemi değişince artırılır. */
const val PlaceVersion = 1

/**
 * En yakın şehri bulur: 1 derecelik ızgara hücrelerinde, çevresindeki 9 hücreye bakar.
 * [MaxKm]'den uzaktaysa (denizin ortası) yer yok sayılır. Saf Kotlin; JVM'de test edilir.
 */
class CityIndex(cities: List<City>) {

    private val grid = HashMap<Int, MutableList<City>>()

    init {
        for (c in cities) grid.getOrPut(cell(floor(c.lat).toInt(), floor(c.lon).toInt())) { ArrayList() } += c
    }

    val size: Int = cities.size

    fun nearest(lat: Double, lon: Double, maxKm: Double = MaxKm): City? {
        if (lat.isNaN() || lon.isNaN() || (lat == 0.0 && lon == 0.0)) return null
        val la = floor(lat).toInt()
        val lo = floor(lon).toInt()
        var best: City? = null
        var bestKm = Double.MAX_VALUE
        for (dy in -1..1) {
            for (dx in -1..1) {
                var x = lo + dx
                if (x < -180) x += 360
                if (x >= 180) x -= 360
                val list = grid[cell(la + dy, x)] ?: continue
                for (c in list) {
                    val d = distanceKm(lat, lon, c.lat, c.lon)
                    if (d < bestKm) {
                        bestKm = d
                        best = c
                    }
                }
            }
        }
        return if (bestKm <= maxKm) best else null
    }

    companion object {
        const val MaxKm = 60.0
        const val Asset = "places/cities.tsv"

        private fun cell(lat: Int, lon: Int): Int = (lat + 90) * 360 + (lon + 180)

        fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val p1 = Math.toRadians(lat1)
            val p2 = Math.toRadians(lat2)
            val dp = p2 - p1
            val dl = Math.toRadians(lon2 - lon1)
            val h = sin(dp / 2) * sin(dp / 2) + cos(p1) * cos(p2) * sin(dl / 2) * sin(dl / 2)
            return 2 * 6371.0 * asin(sqrt(h.coerceIn(0.0, 1.0)))
        }

        /** "ad<TAB>il<TAB>ülke<TAB>enlem<TAB>boylam" satırları. Bozuk satırlar atlanır. */
        fun parse(lines: Sequence<String>): List<City> {
            val out = ArrayList<City>(60_000)
            for (line in lines) {
                val f = line.split('\t')
                if (f.size < 5) continue
                val lat = f[3].toDoubleOrNull() ?: continue
                val lon = f[4].toDoubleOrNull() ?: continue
                out += City(f[0], f[1], f[2], lat, lon)
            }
            return out
        }

        /** APK'daki şehir listesi; derlemede indirilmediyse null (yerler kapalı kalır). Disk okur. */
        fun load(context: Context): CityIndex? = runCatching {
            context.assets.open(Asset).bufferedReader().useLines { CityIndex(parse(it)) }
        }.getOrNull()?.takeIf { it.size > 0 }
    }
}

/** Yer adlarının gösterimi ve aranabilir metni. */
object PlaceNames {

    fun country(code: String, locale: Locale): String {
        if (code.isEmpty()) return ""
        val region = runCatching { Locale.Builder().setRegion(code).build() }.getOrNull() ?: return code
        return region.getDisplayCountry(locale).ifEmpty { code }
    }

    /** "Kadıköy, Istanbul" (il, şehirle aynıysa bir kez). */
    fun cityLine(entry: PlaceEntry): String = when {
        entry.admin.isEmpty() -> entry.city
        entry.city.isEmpty() || same(entry.city, entry.admin) -> entry.admin
        else -> "${entry.city}, ${entry.admin}"
    }

    /** Yerler bölümündeki grup adı. */
    fun groupTitle(entry: PlaceEntry): String = entry.admin.ifEmpty { entry.city }

    /** Aramada eşleşecek adlar: şehir, il, ülkenin Türkçe ve İngilizce adı. */
    fun searchable(entry: PlaceEntry): String =
        if (!entry.hasPlace) {
            ""
        } else {
            listOf(
                entry.city,
                entry.admin,
                country(entry.country, Locale.forLanguageTag("tr")),
                country(entry.country, Locale.ENGLISH)
            ).filter { it.isNotEmpty() }.joinToString(" ")
        }

    private fun same(a: String, b: String): Boolean =
        a.equals(b, ignoreCase = true) || fold(a) == fold(b)

    private fun fold(s: String): String =
        java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace('ı', 'i')
            .lowercase(Locale.ROOT)
}

/** Yerler bölümündeki bir kart: bir il/eyalet ve oradaki öğeler. */
@Immutable
data class PlaceGroup(
    val key: String,
    val title: String,
    val country: String,
    val count: Int,
    val cover: MediaItem,
    val latestMillis: Long
)

/** Yerlere göre gruplama. Saf Kotlin; JVM'de test edilir. */
object PlacesLogic {

    private const val Prefix = "place:"

    fun collectionKey(groupKey: String): String = "$Prefix$groupKey"

    fun groupOf(collectionKey: String): String? =
        if (collectionKey.startsWith(Prefix)) collectionKey.removePrefix(Prefix) else null

    /** En son çekilen yer önce. [items] gizlenenler hariç tüm öğeler. */
    fun groups(
        items: List<MediaItem>,
        places: Map<Long, PlaceEntry>
    ): List<PlaceGroup> {
        if (places.isEmpty()) return emptyList()
        class Acc(val entry: PlaceEntry, var count: Int, var cover: MediaItem)
        val acc = LinkedHashMap<String, Acc>()
        for (item in items) {
            val p = places[item.id]?.takeIf { it.hasPlace } ?: continue
            val a = acc[p.groupKey]
            if (a == null) {
                acc[p.groupKey] = Acc(p, 1, item)
            } else {
                a.count++
                if (item.takenAtMillis > a.cover.takenAtMillis) a.cover = item
            }
        }
        return acc.map { (key, a) ->
            PlaceGroup(key, PlaceNames.groupTitle(a.entry), a.entry.country, a.count, a.cover, a.cover.takenAtMillis)
        }.sortedByDescending { it.latestMillis }
    }

    /** Bir yerdeki öğeler, [items] sırasıyla (eskiden yeniye). */
    fun itemsIn(
        groupKey: String,
        items: List<MediaItem>,
        places: Map<Long, PlaceEntry>
    ): List<MediaItem> =
        items.filter { places[it.id]?.takeIf { p -> p.hasPlace }?.groupKey == groupKey }
}
