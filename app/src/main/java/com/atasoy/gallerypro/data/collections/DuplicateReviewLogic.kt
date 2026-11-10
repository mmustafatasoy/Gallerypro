package com.atasoy.gallerypro.data.collections

import com.atasoy.gallerypro.data.clip.VectorEntry
import com.atasoy.gallerypro.data.clip.VectorMath
import com.atasoy.gallerypro.data.clip.VectorVersion
import com.atasoy.gallerypro.data.media.MediaItem
import kotlin.math.abs

/**
 * Son Silinenler'e gönderme kararı verilene kadar yalnızca öneri sunulur.
 * Kalsın: en yüksek piksel sayısı; eşitlikte favori, dosya boyutu ve tarih öncelikli.
 */
data class DuplicateReviewGroup(
    val keep: MediaItem,
    val discard: List<MediaItem>
) {
    val members: List<MediaItem> get() = listOf(keep) + discard
}

/**
 * Kopya adaylarını mevcut CollectionsLogic.duplicates ile toplar; her fotoğraf çifti
 * güncel CLIP vektörleriyle >= 0,97 benzer olmadıkça aynı temizlik grubuna girmez.
 * Böylece yalnızca aynı boyut/çözünürlükle ilgisiz iki fotoğraf silinmek üzere seçilmez.
 */
object DuplicateReviewLogic {
    const val MinSimilarity = 0.97f
    private const val Dimensions = 512
    private const val AspectTolerance = 0.015

    fun groups(
        items: List<MediaItem>,
        vectors: Map<Long, VectorEntry>
    ): List<DuplicateReviewGroup> {
        if (items.isEmpty() || vectors.isEmpty()) return emptyList()

        // Var olan CollectionsLogic.duplicates adayları esas alınır; gizliler/çöpler
        // çağıranın MediaRepository.items akışından gelmediği için burada bulunmaz.
        val candidates = CollectionsLogic.duplicates(items)
            .asSequence()
            .filter { !it.isVideo && it.width > 0 && it.height > 0 && it.sizeBytes > 0 }
            .filter { item ->
                val v = vectors[item.id]
                v != null && v.modified == item.modifiedMillis &&
                    v.version >= VectorVersion && v.vector.size == Dimensions
            }
            .sortedWith(
                compareByDescending<MediaItem> { it.width.toLong() * it.height }
                    .thenByDescending { it.isFavorite }
                    .thenByDescending { it.sizeBytes }
                    .thenByDescending { it.takenAtMillis }
                    .thenByDescending { it.id }
            ).toList()

        val assigned = HashSet<Long>()
        val result = ArrayList<DuplicateReviewGroup>()
        for (anchor in candidates) {
            if (anchor.id in assigned) continue
            val group = ArrayList<MediaItem>()
            group.add(anchor)
            for (candidate in candidates) {
                if (candidate.id == anchor.id || candidate.id in assigned) continue
                if (!similarAspect(anchor, candidate)) continue
                val vec = vectors.getValue(candidate.id).vector
                // Aynı gruptaki her bir fotoğraf ile eşleşme zorunlu (zincirle
                // birbirinden farklı fotoğrafların yanlışlıkla gruplanması önlenir).
                if (group.all { member ->
                    VectorMath.dot(vectors.getValue(member.id).vector, vec) >= MinSimilarity
                }) group.add(candidate)
            }
            if (group.size > 1) {
                assigned.addAll(group.map { it.id })
                result.add(DuplicateReviewGroup(group.first(), group.drop(1)))
            }
        }
        return result.sortedByDescending { it.keep.takenAtMillis }
    }

    private fun similarAspect(a: MediaItem, b: MediaItem): Boolean {
        fun ratio(item: MediaItem): Double =
            minOf(item.width, item.height).toDouble() / maxOf(item.width, item.height)
        return abs(ratio(a) - ratio(b)) <= AspectTolerance
    }
}
