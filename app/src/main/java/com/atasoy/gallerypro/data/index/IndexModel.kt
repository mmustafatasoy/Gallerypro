package com.atasoy.gallerypro.data.index

import androidx.compose.runtime.Immutable

/**
 * Bir fotoğrafın cihaz içi yapay zekâ dizini. [labels] ML Kit'in İngilizce etiketleridir
 * (Türkçesi [LabelDictionary] ile aranır), [text] fotoğraftaki yazıdır (OCR).
 * [modified] dosya değişince yeniden dizinlemek, [version] dizinleyici gelişince
 * eski kayıtları yenilemek içindir.
 */
@Immutable
data class IndexEntry(
    val id: Long,
    val modified: Long,
    val labels: List<String>,
    val text: String,
    val version: Int = IndexVersion
)

/** Dizinleyici değişince artırılır; eski sürümdeki kayıtlar sırayla yeniden işlenir. */
const val IndexVersion = 1

/** Videolar için ayrı sürüm: 2 = tek küçük resim yerine 3 kare. Fotoğraflar yeniden işlenmez. */
const val VideoIndexVersion = 2

/** Birden çok karenin (video) etiketlerini birleştirir: her etiketin en yüksek güveni, çoktan aza. Saf Kotlin. */
object FrameLabels {
    fun merge(frames: List<List<Pair<String, Float>>>, limit: Int): List<String> {
        val best = LinkedHashMap<String, Float>()
        for (frame in frames) for ((label, confidence) in frame) {
            val old = best[label]
            if (old == null || confidence > old) best[label] = confidence
        }
        return best.entries.sortedByDescending { it.value }.take(limit).map { it.key }
    }
}

/** Arama ekranındaki durum kartı için. */
@Immutable
data class IndexStatus(
    val running: Boolean = false,
    val done: Int = 0,
    val total: Int = 0
) {
    val complete: Boolean get() = total > 0 && done >= total
}
