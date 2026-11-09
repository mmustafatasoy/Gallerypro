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

/** Arama ekranındaki durum kartı için. */
@Immutable
data class IndexStatus(
    val running: Boolean = false,
    val done: Int = 0,
    val total: Int = 0
) {
    val complete: Boolean get() = total > 0 && done >= total
}
