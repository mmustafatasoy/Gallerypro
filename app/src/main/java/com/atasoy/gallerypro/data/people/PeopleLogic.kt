package com.atasoy.gallerypro.data.people

import kotlin.math.sqrt

/** Her yüz ayrı bir vektör: aynı fotoğraftaki iki farklı kişi tek gruba zorlanmaz. */
data class FaceVector(
    val photoId: Long,
    val faceIndex: Int,
    val modifiedMillis: Long,
    val vector: FloatArray
)

data class PersonGroup(
    val key: String,
    val representative: FaceVector,
    val photos: List<Long>,
    val faceCount: Int
)

/**
 * SFace FP32 128 boyutlu vektörleri cihazda gruplar. Korumacı eşik, yanlış
 * gruplamaları azaltmayı amaçlar; yüz eşleşmesi biyometrik kesinlik değildir.
 */
object PeopleLogic {
    const val Dimensions = 128
    const val SimilarityThreshold = 0.45f

    fun normalized(input: FloatArray): FloatArray? {
        if (input.size != Dimensions || input.any { !it.isFinite() }) return null
        val length = sqrt(input.sumOf { it.toDouble() * it }.toFloat())
        if (length < 0.000001f) return null
        return FloatArray(input.size) { input[it] / length }
    }

    fun cosine(a: FloatArray, b: FloatArray): Float {
        if (a.size != Dimensions || b.size != Dimensions) return -1f
        var sum = 0f
        for (i in a.indices) sum += a[i] * b[i]
        return sum
    }

    fun groups(faces: List<FaceVector>): List<PersonGroup> {
        data class Cluster(val members: MutableList<FaceVector>, val anchor: FaceVector)
        val clusters = mutableListOf<Cluster>()
        val candidates = faces.asSequence()
            .filter { it.faceIndex >= 0 && it.photoId > 0L && it.vector.size == Dimensions }
            .sortedWith(compareBy<FaceVector> { it.photoId }.thenBy { it.faceIndex })
            .toList()
        for (face in candidates) {
            val vec = normalized(face.vector) ?: continue
            val unit = face.copy(vector = vec)
            // Aynı fotoğraftaki iki farklı yüz tek kişi olamaz.
            val nearest = clusters.mapIndexedNotNull { index, cluster ->
                if (cluster.members.any { it.photoId == unit.photoId }) return@mapIndexedNotNull null
                val anchorVec = cluster.anchor.vector
                val similarity = cosine(vec, anchorVec)
                if (similarity < SimilarityThreshold) null else index to similarity
            }.maxByOrNull { it.second }
            if (nearest == null) clusters.add(Cluster(mutableListOf(unit), unit))
            else clusters[nearest.first].members.add(unit)
        }
        return clusters.map { c ->
            PersonGroup(
                key = "${c.anchor.photoId}:${c.anchor.faceIndex}",
                representative = c.anchor,
                photos = c.members.map { it.photoId }.distinct(),
                faceCount = c.members.size
            )
        }.sortedWith(compareByDescending<PersonGroup> { it.faceCount }.thenBy { it.key })
    }
}
