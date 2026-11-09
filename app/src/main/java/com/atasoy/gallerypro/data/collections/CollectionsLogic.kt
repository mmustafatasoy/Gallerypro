package com.atasoy.gallerypro.data.collections

import androidx.compose.runtime.Immutable
import com.atasoy.gallerypro.data.media.MediaItem
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/** Koleksiyonlar ekranının bölümleri; kullanıcı sırasını değiştirip gizleyebilir. */
enum class SectionId { RecentDays, Pinned, Albums, MediaTypes, Utilities }

@Immutable
data class SectionSetting(val id: SectionId, val visible: Boolean)

/**
 * Açılabilen her koleksiyonun anahtarı. Metin olduğu için ekran döndürmede
 * (rememberSaveable) doğrudan saklanır.
 */
object CollectionKeys {
    const val RecentDays = "recent"
    const val Favorites = "favorites"
    const val Videos = "videos"
    const val RecentlySaved = "saved"
    const val Panoramas = "type:panoramas"
    const val Screenshots = "type:screenshots"
    const val Raw = "type:raw"
    const val Hidden = "hidden"
    const val Trash = "trash"
    const val Duplicates = "duplicates"
    const val Imports = "imports"

    /** Öğe listesi değil, tüm albümlerin ızgarası. */
    const val AllAlbums = "albums"

    private const val DayPrefix = "day:"
    private const val AlbumPrefix = "album:"

    fun day(epochDay: Long): String = "$DayPrefix$epochDay"
    fun album(path: String): String = "$AlbumPrefix$path"

    fun dayOf(key: String): Long? = if (key.startsWith(DayPrefix)) key.removePrefix(DayPrefix).toLongOrNull() else null
    fun albumOf(key: String): String? = if (key.startsWith(AlbumPrefix)) key.removePrefix(AlbumPrefix) else null
}

/** Medya Türleri listesindeki satırlar (yalnızca boş olmayanlar gösterilir). */
enum class MediaType(val key: String) {
    Videos(CollectionKeys.Videos),
    Panoramas(CollectionKeys.Panoramas),
    Screenshots(CollectionKeys.Screenshots),
    Raw(CollectionKeys.Raw)
}

/** Bir koleksiyonun öğe sayısı ve kapak fotoğrafı. */
@Immutable
data class Summary(val count: Int, val cover: MediaItem?) {
    companion object {
        val Empty = Summary(0, null)
    }
}

@Immutable
data class DayGroup(val epochDay: Long, val count: Int, val cover: MediaItem)

@Immutable
data class Album(
    val path: String,
    val name: String,
    val count: Int,
    val cover: MediaItem,
    val latestMillis: Long
)

@Immutable
data class MediaTypeCount(val type: MediaType, val count: Int)

@Immutable
data class CollectionsContent(
    val loading: Boolean = true,
    val recentDays: List<DayGroup> = emptyList(),
    val favorites: Summary = Summary.Empty,
    val videos: Summary = Summary.Empty,
    val recentlySaved: Summary = Summary.Empty,
    val albums: List<Album> = emptyList(),
    val mediaTypes: List<MediaTypeCount> = emptyList(),
    val hiddenCount: Int = 0,
    val trashCount: Int = 0,
    val duplicateCount: Int = 0,
    val importCount: Int = 0
)

/**
 * Dosya yolundan ve boyutlardan tür çıkarımı. Saf Kotlin; birim testleri JVM'de çalışır.
 * Selfie, Hareketli ve Portre için dosyanın içine bakmak gerekir (5. aşama dizinleyici).
 */
object MediaKinds {

    private val rawMimes = setOf("image/x-adobe-dng", "image/dng", "image/x-dcraw", "image/x-raw")
    private val rawExtensions = listOf(".dng", ".raw", ".cr2", ".cr3", ".nef", ".arw", ".orf", ".rw2", ".raf")

    private fun path(item: MediaItem): String = item.relativePath.orEmpty().lowercase(Locale.ROOT)

    fun isScreenshot(item: MediaItem): Boolean =
        !item.isVideo && (
            path(item).contains("screenshot") ||
                item.album.orEmpty().lowercase(Locale.ROOT).contains("screenshot")
            )

    fun isScreenRecording(item: MediaItem): Boolean {
        if (!item.isVideo) return false
        val p = path(item)
        return p.contains("screen record") || p.contains("screenrecord") || p.contains("screen_record")
    }

    /** Telefonun kamerasıyla çekilenler (DCIM altında, ekran görüntüsü değil). */
    fun isCamera(item: MediaItem): Boolean =
        path(item).startsWith("dcim/") && !isScreenshot(item) && !isScreenRecording(item)

    /** Kamerayla çekilmiş ve uzun kenarı kısa kenarın en az 2,5 katı olan fotoğraflar. */
    fun isPanorama(item: MediaItem): Boolean {
        if (item.isVideo || !isCamera(item)) return false
        val long = maxOf(item.width, item.height)
        val short = minOf(item.width, item.height)
        return short > 0 && long * 2 >= short * 5
    }

    fun isRaw(item: MediaItem): Boolean {
        if (item.isVideo) return false
        if (item.mimeType?.lowercase(Locale.ROOT) in rawMimes) return true
        val name = item.displayName.orEmpty().lowercase(Locale.ROOT)
        return rawExtensions.any { name.endsWith(it) }
    }

    /** İndirilenler, Bluetooth ve Quick Share / Nearby Share ile gelenler. */
    fun isImported(item: MediaItem): Boolean {
        val p = path(item)
        return p.startsWith("download/") || p.startsWith("bluetooth/") || p.contains("/bluetooth/") ||
            p.contains("quick share") || p.contains("nearby share")
    }

    /** Başka uygulamalardan kaydedilenler: kamera ve ekran görüntüsü dışındaki her şey. */
    fun isSaved(item: MediaItem): Boolean = !isCamera(item) && !isScreenshot(item) && !isScreenRecording(item)
}

object CollectionsLogic {

    /** Son Günler en fazla bu kadar gün gösterir. */
    const val MaxRecentDays = 12

    /** Son Günler ve Son Kaydedilenler için geriye bakılan süre. */
    const val RecentWindowDays = 30L

    private const val DayMillis = 86_400_000L

    /**
     * @param items gizlenenler hariç tüm öğeler, eskiden yeniye.
     */
    fun build(
        items: List<MediaItem>,
        hiddenCount: Int,
        trashCount: Int,
        nowMillis: Long,
        zone: ZoneId
    ): CollectionsContent {
        val favorites = ArrayList<MediaItem>()
        val videos = ArrayList<MediaItem>()
        val saved = ArrayList<MediaItem>()
        var panoramas = 0
        var screenshots = 0
        var raws = 0
        var imports = 0
        val savedSince = nowMillis - RecentWindowDays * DayMillis
        for (item in items) {
            if (item.isFavorite) favorites += item
            if (item.isVideo) videos += item
            if (MediaKinds.isSaved(item) && addedOrTaken(item) >= savedSince) saved += item
            if (MediaKinds.isPanorama(item)) panoramas++
            if (MediaKinds.isScreenshot(item)) screenshots++
            if (MediaKinds.isRaw(item)) raws++
            if (MediaKinds.isImported(item)) imports++
        }
        val types = buildList {
            if (videos.isNotEmpty()) add(MediaTypeCount(MediaType.Videos, videos.size))
            if (panoramas > 0) add(MediaTypeCount(MediaType.Panoramas, panoramas))
            if (screenshots > 0) add(MediaTypeCount(MediaType.Screenshots, screenshots))
            if (raws > 0) add(MediaTypeCount(MediaType.Raw, raws))
        }
        return CollectionsContent(
            loading = false,
            recentDays = recentDays(items, nowMillis, zone),
            favorites = summary(favorites),
            videos = summary(videos),
            recentlySaved = summary(saved),
            albums = albums(items),
            mediaTypes = types,
            hiddenCount = hiddenCount,
            trashCount = trashCount,
            duplicateCount = duplicates(items).size,
            importCount = imports
        )
    }

    /** Kapak: en yeni fotoğraf; hiç fotoğraf yoksa en yeni video. */
    fun summary(items: List<MediaItem>): Summary =
        Summary(items.size, items.lastOrNull { !it.isVideo } ?: items.lastOrNull())

    fun epochDay(millis: Long, zone: ZoneId): Long =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().toEpochDay()

    /** Bugünden geriye, öğesi olan günler (en yeni başta). */
    fun recentDays(items: List<MediaItem>, nowMillis: Long, zone: ZoneId): List<DayGroup> {
        val today = epochDay(nowMillis, zone)
        val oldest = today - RecentWindowDays
        val result = ArrayList<DayGroup>()
        var currentDay = Long.MIN_VALUE
        var count = 0
        var cover: MediaItem? = null
        var newest: MediaItem? = null

        fun flush() {
            val last = newest ?: return
            result += DayGroup(currentDay, count, cover ?: last)
        }

        for (i in items.indices.reversed()) {
            val item = items[i]
            val day = epochDay(item.takenAtMillis, zone)
            if (day < oldest) break
            if (day != currentDay) {
                flush()
                if (result.size == MaxRecentDays) return result
                currentDay = day
                count = 0
                cover = null
                newest = item
            }
            count++
            if (cover == null && !item.isVideo) cover = item
        }
        flush()
        return if (result.size > MaxRecentDays) result.subList(0, MaxRecentDays) else result
    }

    /**
     * Klasöre göre albümler. Kamera albümü başta, sonra en son eklenen öğeye göre.
     */
    fun albums(items: List<MediaItem>): List<Album> {
        class Acc(val path: String, var name: String, var count: Int = 0, var cover: MediaItem? = null, var newest: MediaItem? = null)

        val map = LinkedHashMap<String, Acc>()
        for (item in items) {
            val path = item.relativePath?.takeIf { it.isNotBlank() } ?: continue
            val acc = map.getOrPut(path) { Acc(path, item.album ?: lastSegment(path)) }
            acc.count++
            acc.newest = item
            if (!item.isVideo) acc.cover = item
            if (acc.name.isBlank()) acc.name = lastSegment(path)
        }
        return map.values
            .map { acc ->
                val newest = acc.newest!!
                Album(acc.path, acc.name, acc.count, acc.cover ?: newest, newest.takenAtMillis)
            }
            .sortedWith(
                compareByDescending<Album> { it.path.lowercase(Locale.ROOT) == "dcim/camera/" }
                    .thenByDescending { it.latestMillis }
            )
    }

    private fun lastSegment(path: String): String = path.trimEnd('/').substringAfterLast('/')

    /**
     * Aynı bayt boyutu, aynı çözünürlük ve aynı türdeki öğeler kopya sayılır.
     * Kopyalar yan yana gelir; gruplar en yeni öğelerine göre sıralanır (en yeni en altta).
     */
    fun duplicates(items: List<MediaItem>): List<MediaItem> {
        val groups = items
            .filter { it.sizeBytes > 0L }
            .groupBy { "${it.isVideo}:${it.sizeBytes}:${minOf(it.width, it.height)}x${maxOf(it.width, it.height)}" }
            .values
            .filter { it.size > 1 }
        return groups
            .sortedBy { group -> group.maxOf { it.takenAtMillis } }
            .flatMap { group -> group.sortedWith(compareBy<MediaItem>({ it.takenAtMillis }, { it.id })) }
    }

    private fun addedOrTaken(item: MediaItem): Long = if (item.addedMillis > 0L) item.addedMillis else item.takenAtMillis

    /**
     * Bir koleksiyonun öğeleri, eskiden yeniye (ızgarada en yeni en altta).
     * [items] gizlenenler hariç tüm öğeler, [hidden] gizlenenler, [trashed] çöptekiler.
     */
    fun itemsFor(
        key: String,
        items: List<MediaItem>,
        hidden: List<MediaItem>,
        trashed: List<MediaItem>,
        nowMillis: Long,
        zone: ZoneId
    ): List<MediaItem> {
        CollectionKeys.dayOf(key)?.let { day ->
            return items.filter { epochDay(it.takenAtMillis, zone) == day }
        }
        CollectionKeys.albumOf(key)?.let { path ->
            return items.filter { it.relativePath == path }
        }
        return when (key) {
            CollectionKeys.RecentDays -> {
                val days = recentDays(items, nowMillis, zone).mapTo(HashSet()) { it.epochDay }
                items.filter { epochDay(it.takenAtMillis, zone) in days }
            }
            CollectionKeys.Favorites -> items.filter { it.isFavorite }
            CollectionKeys.Videos -> items.filter { it.isVideo }
            CollectionKeys.RecentlySaved -> {
                val since = nowMillis - RecentWindowDays * DayMillis
                items.filter { MediaKinds.isSaved(it) && addedOrTaken(it) >= since }
            }
            CollectionKeys.Panoramas -> items.filter(MediaKinds::isPanorama)
            CollectionKeys.Screenshots -> items.filter(MediaKinds::isScreenshot)
            CollectionKeys.Raw -> items.filter(MediaKinds::isRaw)
            CollectionKeys.Imports -> items.filter(MediaKinds::isImported)
            CollectionKeys.Duplicates -> duplicates(items)
            CollectionKeys.Hidden -> hidden
            CollectionKeys.Trash -> trashed
            else -> emptyList()
        }
    }

    /** Son Silinenler'de kalan gün (en az 0). */
    fun daysLeft(expiresMillis: Long, nowMillis: Long): Int =
        if (expiresMillis <= 0L) 0 else ((expiresMillis - nowMillis + DayMillis - 1) / DayMillis).toInt().coerceAtLeast(0)

    /** "Bugün", "Dün", hafta içi gün adı veya tarih seçimi için gün farkı. */
    fun daysAgo(day: Long, nowMillis: Long, zone: ZoneId): Long = epochDay(nowMillis, zone) - day
}

/** Bölüm sırası ve görünürlüğü: "RecentDays,-Pinned,..." ("-" gizli demek). */
object SectionOrder {

    val Default: List<SectionSetting> = SectionId.entries.map { SectionSetting(it, true) }

    fun encode(list: List<SectionSetting>): String =
        list.joinToString(",") { (if (it.visible) "" else "-") + it.id.name }

    /** Bilinmeyen adlar atlanır, eksik bölümler (yeni sürümde eklenenler) sona görünür olarak eklenir. */
    fun decode(text: String?): List<SectionSetting> {
        if (text.isNullOrBlank()) return Default
        val seen = HashSet<SectionId>()
        val result = ArrayList<SectionSetting>()
        for (token in text.split(',')) {
            val visible = !token.startsWith("-")
            val name = token.removePrefix("-").trim()
            val id = SectionId.entries.firstOrNull { it.name == name } ?: continue
            if (seen.add(id)) result += SectionSetting(id, visible)
        }
        for (id in SectionId.entries) {
            if (id !in seen) result += SectionSetting(id, true)
        }
        return result
    }

    fun move(list: List<SectionSetting>, from: Int, to: Int): List<SectionSetting> {
        if (from !in list.indices || to !in list.indices || from == to) return list
        val mutable = list.toMutableList()
        val item = mutable.removeAt(from)
        mutable.add(to, item)
        return mutable
    }

    fun toggle(list: List<SectionSetting>, id: SectionId): List<SectionSetting> =
        list.map { if (it.id == id) it.copy(visible = !it.visible) else it }
}
