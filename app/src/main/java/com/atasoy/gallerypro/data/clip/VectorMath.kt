package com.atasoy.gallerypro.data.clip

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Bir öğenin CLIP vektörü (512 boyutlu birim vektör) ve hangi dosya sürümünden çıkarıldığı. */
class VectorEntry(val modified: Long, val vector: FloatArray, val version: Int = VectorVersion)

/** Vektör çıkarma yöntemi değişince artırılır; eski vektörler yeniden hesaplanır. */
const val VectorVersion = 1

/** Saf Kotlin; JVM'de test edilir. */
object VectorMath {

    /** İki birim vektörün kosinüs benzerliği (= iç çarpım). */
    fun dot(a: FloatArray, b: FloatArray): Float {
        val n = minOf(a.size, b.size)
        var s = 0f
        for (i in 0 until n) s += a[i] * b[i]
        return s
    }

    fun toBytes(v: FloatArray): ByteArray {
        val buf = ByteBuffer.allocate(v.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        for (x in v) buf.putFloat(x)
        return buf.array()
    }

    fun fromBytes(bytes: ByteArray): FloatArray {
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        return FloatArray(bytes.size / 4) { buf.getFloat() }
    }

    /**
     * Sorguya anlamca en yakın öğeler. En iyi benzerlikten [window] kadar aşağısı ve en az [minSimilarity]
     * olanlar, en yakından uzağa, en çok [limit] tane. CLIP benzerlikleri dar bir aralıkta toplanır
     * (eşleşenler ~0,25–0,35); sabit eşik yerine en iyiye göre pencere daha kararlı sonuç verir.
     */
    fun nearest(
        query: FloatArray,
        candidates: List<Pair<Long, FloatArray>>,
        minSimilarity: Float = MinSimilarity,
        window: Float = Window,
        limit: Int = Limit
    ): List<Pair<Long, Float>> {
        if (candidates.isEmpty()) return emptyList()
        val scored = candidates.map { (id, v) -> id to dot(query, v) }
        val top = scored.maxOf { it.second }
        val threshold = maxOf(minSimilarity, top - window)
        return scored.filter { it.second >= threshold }
            .sortedByDescending { it.second }
            .take(limit)
    }

    const val MinSimilarity = 0.20f
    const val Window = 0.06f
    const val Limit = 300
}
