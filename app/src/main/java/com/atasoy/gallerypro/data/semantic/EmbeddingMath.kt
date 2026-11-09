package com.atasoy.gallerypro.data.semantic

import java.util.PriorityQueue
import kotlin.math.sqrt

data class EmbeddingMatch(val id: Long, val score: Float)

/** Modelden bağımsız vektör işlemleri; geçersiz çıktılar sonuçlara katılmaz. */
object EmbeddingMath {

    /** Yeni birim vektör döndürür; çağıranın dizisini değiştirmez. */
    fun normalized(values: FloatArray): FloatArray? {
        val norm = sqrt(squaredNorm(values) ?: return null)
        return FloatArray(values.size) { (values[it].toDouble() / norm).toFloat() }
    }

    fun cosine(left: FloatArray, right: FloatArray): Float? {
        val norm = squaredNorm(left) ?: return null
        return similarity(left, norm, right)
    }

    /**
     * [minScore] dahil en iyi [limit] eşleşme. Eşit puanda küçük kimlik önce gelir.
     * Yalnızca en iyi sonuçları bellekte tutar; tüm kütüphaneyi sıralamaz.
     */
    fun rank(
        query: FloatArray,
        candidates: Map<Long, FloatArray>,
        minScore: Float,
        limit: Int,
        excludeId: Long? = null
    ): List<EmbeddingMatch> {
        if (limit <= 0 || candidates.isEmpty() || !minScore.isFinite()) return emptyList()
        val queryNorm = squaredNorm(query) ?: return emptyList()
        val best = PriorityQueue(minOf(limit, candidates.size, 128), WorstFirst)
        for ((id, vector) in candidates) {
            if (id == excludeId) continue
            val score = similarity(query, queryNorm, vector) ?: continue
            if (score < minScore) continue
            val match = EmbeddingMatch(id, score)
            if (best.size < limit) {
                best += match
            } else if (WorstFirst.compare(match, best.peek()) > 0) {
                best.poll()
                best += match
            }
        }
        return best.sortedWith(BestFirst)
    }

    private fun similarity(left: FloatArray, leftNorm: Double, right: FloatArray): Float? {
        if (left.size != right.size) return null
        val rightNorm = squaredNorm(right) ?: return null
        var dot = 0.0
        for (i in left.indices) dot += left[i].toDouble() * right[i].toDouble()
        return (dot / sqrt(leftNorm * rightNorm)).coerceIn(-1.0, 1.0).toFloat()
    }

    // Double toplama, Float.MAX_VALUE/MIN_VALUE içeren geçerli vektörleri de korur.
    private fun squaredNorm(values: FloatArray): Double? {
        if (values.isEmpty()) return null
        var total = 0.0
        for (value in values) {
            if (!value.isFinite()) return null
            total += value.toDouble() * value.toDouble()
        }
        return total.takeIf { it > 0.0 }
    }

    private val BestFirst = compareByDescending<EmbeddingMatch> { it.score }.thenBy { it.id }
    private val WorstFirst = compareBy<EmbeddingMatch> { it.score }.thenByDescending { it.id }
}
