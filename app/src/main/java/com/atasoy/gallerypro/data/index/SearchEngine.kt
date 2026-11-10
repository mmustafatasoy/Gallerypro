package com.atasoy.gallerypro.data.index

import com.atasoy.gallerypro.data.collections.MediaKinds
import com.atasoy.gallerypro.data.media.MediaItem
import java.time.Instant
import java.time.ZoneId
import kotlin.math.max
import kotlin.math.min

/**
 * Aramaya hazır, sadeleştirilmiş tek öğe. Alanlar boşlukla başlar (" kopek cimen") ki
 * " kop" gibi bir önek tek bir contains ile bulunsun.
 */
class SearchDoc(
    val item: MediaItem,
    val labels: String,
    val caption: String,
    val album: String,
    val text: String
)

enum class SearchKind { Any, Photos, Videos }

/** Sorgudan ayrılan tarih ve tür süzgeçleri; kalan sözcükler [words]. */
data class ParsedQuery(
    val words: List<String>,
    val year: Int? = null,
    val month: Int? = null,
    /** 0 bugün, 1 dün. */
    val daysAgo: Int? = null,
    val kind: SearchKind = SearchKind.Any,
    val screenshots: Boolean = false,
    val favorites: Boolean = false
) {
    val isEmpty: Boolean
        get() = words.isEmpty() && year == null && month == null && daysAgo == null &&
            kind == SearchKind.Any && !screenshots && !favorites
}

/**
 * Cihaz içi arama. Sıra (dokümandaki gibi): tarih ve tür süzgeçleri → etiket ve başlık →
 * fotoğraftaki yazı. Her sözcük bir yerde bulunmalı (VE); puanı yüksek olan önce gelir.
 * Türkçe ekler basitçe kırpılır: "kediler" → "kedi", "denizde" → "deniz".
 * Saf Kotlin; JVM'de test edilir.
 */
object SearchEngine {

    private const val MaxText = 800

    fun prepare(
        items: List<MediaItem>,
        index: Map<Long, IndexEntry>,
        captions: Map<Long, String>
    ): List<SearchDoc> {
        val labelCache = HashMap<String, String>()
        fun labelWords(label: String): String = labelCache.getOrPut(label) {
            buildString {
                append(SearchText.normalize(label))
                for (tr in LabelDictionary.turkish(label)) append(' ').append(SearchText.normalize(tr))
            }
        }
        return items.map { item ->
            val entry = index[item.id]
            SearchDoc(
                item = item,
                labels = " " + entry?.labels.orEmpty().joinToString(" ") { labelWords(it) },
                caption = " " + SearchText.normalize(captions[item.id].orEmpty()),
                album = " " + SearchText.normalize(item.album.orEmpty()),
                text = " " + SearchText.normalize(entry?.text.orEmpty()).take(MaxText)
            )
        }
    }

    // ---------------------------------------------------------------- sorgu çözümleme

    private val months = mapOf(
        "ocak" to 1, "subat" to 2, "mart" to 3, "nisan" to 4, "mayis" to 5, "haziran" to 6,
        "temmuz" to 7, "agustos" to 8, "eylul" to 9, "ekim" to 10, "kasim" to 11, "aralik" to 12,
        "january" to 1, "february" to 2, "march" to 3, "april" to 4, "may" to 5, "june" to 6,
        "july" to 7, "august" to 8, "september" to 9, "october" to 10, "november" to 11, "december" to 12
    )
    private val videoWords = setOf("video", "videolar", "videos", "videolari")
    private val photoWords = setOf("foto", "fotograf", "fotograflar", "fotolar", "photo", "photos", "resim", "resimler")
    private val screenshotWords = setOf("screenshot", "screenshots", "ekran")
    private val screenshotTail = setOf("goruntusu", "goruntuleri", "goruntu")
    private val favoriteWords = setOf("favori", "favoriler", "favorite", "favorites", "favourite", "favourites")
    private val todayWords = setOf("bugun", "today")
    private val yesterdayWords = setOf("dun", "yesterday")

    fun parse(query: String): ParsedQuery {
        var year: Int? = null
        var month: Int? = null
        var daysAgo: Int? = null
        var kind = SearchKind.Any
        var screenshots = false
        var favorites = false
        val words = ArrayList<String>()
        var afterScreen = false
        for (token in SearchText.tokens(query)) {
            val asYear = token.takeIf { it.length == 4 }?.toIntOrNull()
            when {
                asYear != null && asYear in 1970..2100 -> year = asYear
                token in months -> month = months.getValue(token)
                token in todayWords -> daysAgo = 0
                token in yesterdayWords -> daysAgo = 1
                token in videoWords -> kind = SearchKind.Videos
                token in photoWords -> kind = SearchKind.Photos
                token in screenshotWords -> {
                    screenshots = true
                    afterScreen = true
                    continue
                }
                afterScreen && token in screenshotTail -> Unit
                token in favoriteWords -> favorites = true
                else -> words += token
            }
            afterScreen = false
        }
        return ParsedQuery(words, year, month, daysAgo, kind, screenshots, favorites)
    }

    // ---------------------------------------------------------------- eşleştirme

    fun search(
        query: String,
        docs: List<SearchDoc>,
        zone: ZoneId,
        now: Long = System.currentTimeMillis()
    ): List<MediaItem> {
        val parsed = parse(query)
        if (parsed.isEmpty) return emptyList()
        val variants = parsed.words.map(::variants)
        val today = epochDay(now, zone)
        val scored = ArrayList<Pair<MediaItem, Float>>()
        for (doc in docs) {
            val item = doc.item
            if (!passesFilters(item, parsed, zone, today)) continue
            var total = 0f
            var all = true
            for (forms in variants) {
                val s = wordScore(doc, forms)
                if (s == 0f) {
                    all = false
                    break
                }
                total += s
            }
            if (all) scored += item to (if (variants.isEmpty()) 1f else total)
        }
        scored.sortWith(compareByDescending<Pair<MediaItem, Float>> { it.second }.thenByDescending { it.first.takenAtMillis })
        return scored.map { it.first }
    }

    /** Tarih ve tür süzgeçleri; cümleyle arama sonuçlarına da uygulanır. */
    fun matchesFilters(item: MediaItem, q: ParsedQuery, zone: ZoneId, now: Long = System.currentTimeMillis()): Boolean =
        passesFilters(item, q, zone, epochDay(now, zone))

    // ---------------------------------------------------------------- cümleyle arama

    /**
     * Cümleyle arama modeline gidecek metin: kullanıcının yazdığı sözcükler, süzgeç sözcükleri
     * ("2023", "temmuz", "videolar", "ekran görüntüsü"…) çıkarılmış. Model büyük harf ve Türkçe
     * karakterleri anladığı için metin sadeleştirilmez. Yalnızca süzgeç varsa boş döner.
     */
    fun semanticText(query: String): String {
        val kept = ArrayList<String>()
        var afterScreen = false
        for (raw in query.trim().split(Regex("\\s+"))) {
            if (raw.isEmpty()) continue
            val tokens = SearchText.tokens(raw)
            if (tokens.isEmpty()) continue
            val filter = tokens.all { t ->
                val asYear = t.takeIf { it.length == 4 }?.toIntOrNull()
                (asYear != null && asYear in 1970..2100) || t in months || t in todayWords ||
                    t in yesterdayWords || t in videoWords || t in photoWords || t in screenshotWords ||
                    t in favoriteWords || (afterScreen && t in screenshotTail)
            }
            afterScreen = tokens.any { it in screenshotWords }
            if (!filter) kept += raw
        }
        return kept.joinToString(" ")
    }

    /** Sözcük eşleşmeleri önce (kesin), ardından anlamca benzer olanlar; aynı öğe bir kez. */
    fun merge(keyword: List<MediaItem>, semantic: List<MediaItem>): List<MediaItem> {
        if (semantic.isEmpty()) return keyword
        val seen = HashSet<Long>(keyword.size * 2)
        val out = ArrayList<MediaItem>(keyword.size + semantic.size)
        for (item in keyword) if (seen.add(item.id)) out += item
        for (item in semantic) if (seen.add(item.id)) out += item
        return out
    }

    private fun passesFilters(item: MediaItem, q: ParsedQuery, zone: ZoneId, today: Long): Boolean {
        when (q.kind) {
            SearchKind.Videos -> if (!item.isVideo) return false
            SearchKind.Photos -> if (item.isVideo) return false
            SearchKind.Any -> Unit
        }
        if (q.screenshots && !MediaKinds.isScreenshot(item)) return false
        if (q.favorites && !item.isFavorite) return false
        if (q.year != null || q.month != null) {
            val date = Instant.ofEpochMilli(item.takenAtMillis).atZone(zone).toLocalDate()
            if (q.year != null && date.year != q.year) return false
            if (q.month != null && date.monthValue != q.month) return false
        }
        if (q.daysAgo != null && epochDay(item.takenAtMillis, zone) != today - q.daysAgo) return false
        return true
    }

    /** Sözcük ve Türkçe ek kırpılmış hâlleri (en az 4 harf kalır). */
    private fun variants(word: String): List<String> {
        val list = ArrayList<String>(5)
        list += word
        if (word.length >= 5) {
            for (cut in 1..min(4, word.length - 4)) list += word.dropLast(cut)
        }
        return list
    }

    private fun wordScore(doc: SearchDoc, forms: List<String>): Float {
        for ((i, form) in forms.withIndex()) {
            val s = fieldScore(doc, " $form")
            if (s > 0f) return if (i == 0) s else s * 0.9f
        }
        return 0f
    }

    private fun fieldScore(doc: SearchDoc, prefix: String): Float {
        var best = 0f
        if (doc.labels.contains(prefix)) best = 3f
        if (doc.caption.contains(prefix)) best = max(best, 3f)
        if (best == 0f && doc.album.contains(prefix)) best = 2f
        if (best == 0f && doc.text.contains(prefix)) best = 1f
        return best
    }

    private fun epochDay(millis: Long, zone: ZoneId): Long =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().toEpochDay()

    // ---------------------------------------------------------------- öneriler

    /** Dizinde en sık geçen etiketler (İngilizce adlarıyla), çoktan aza. */
    fun topLabels(index: Collection<IndexEntry>, limit: Int): List<String> {
        val counts = HashMap<String, Int>()
        for (entry in index) for (label in entry.labels) counts[label] = (counts[label] ?: 0) + 1
        return counts.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(limit)
            .map { it.key }
    }
}
