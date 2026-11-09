package com.atasoy.gallerypro

import com.atasoy.gallerypro.data.semantic.EmbeddingMath
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class EmbeddingMathTest {

    @Test
    fun normalized_returnsUnitVectorWithoutChangingInput() {
        val values = floatArrayOf(3f, 4f)
        val result = EmbeddingMath.normalized(values)!!
        assertArrayEquals(floatArrayOf(0.6f, 0.8f), result, 0.000001f)
        assertArrayEquals(floatArrayOf(3f, 4f), values, 0f)
        assertNotSame(values, result)
    }

    @Test
    fun normalized_rejectsEmptyZeroAndNonfiniteValues() {
        for (values in invalidVectors()) assertNull(EmbeddingMath.normalized(values))
    }

    @Test
    fun cosine_hasExpectedAnglesAndIgnoresMagnitude() {
        assertEquals(1f, EmbeddingMath.cosine(floatArrayOf(2f, 0f), floatArrayOf(7f, 0f))!!, 0f)
        assertEquals(0f, EmbeddingMath.cosine(floatArrayOf(2f, 0f), floatArrayOf(0f, 7f))!!, 0f)
        assertEquals(-1f, EmbeddingMath.cosine(floatArrayOf(2f, 0f), floatArrayOf(-7f, 0f))!!, 0f)
        assertEquals(0.6f, EmbeddingMath.cosine(floatArrayOf(1f, 0f), floatArrayOf(3f, 4f))!!, 0.000001f)
    }

    @Test
    fun cosine_rejectsInvalidAndMismatchedVectors() {
        val valid = floatArrayOf(1f, 2f)
        for (invalid in invalidVectors()) {
            assertNull(EmbeddingMath.cosine(valid, invalid))
            assertNull(EmbeddingMath.cosine(invalid, valid))
        }
        assertNull(EmbeddingMath.cosine(valid, floatArrayOf(1f)))
    }

    @Test
    fun extremeFiniteValuesDoNotOverflowOrUnderflow() {
        val large = floatArrayOf(Float.MAX_VALUE, Float.MAX_VALUE)
        val small = floatArrayOf(Float.MIN_VALUE, Float.MIN_VALUE)
        assertEquals(1f, EmbeddingMath.cosine(large, small)!!, 0.000001f)
        assertArrayEquals(floatArrayOf(0.70710677f, 0.70710677f), EmbeddingMath.normalized(large), 0.000001f)
        assertArrayEquals(floatArrayOf(1f), EmbeddingMath.normalized(floatArrayOf(Float.MIN_VALUE)), 0f)
    }

    @Test
    fun cosineRemainsBoundedForFiniteVectors() {
        val random = Random(42)
        repeat(100) {
            val a = FloatArray(512) { random.nextFloat() * 2f - 1f }
            val b = FloatArray(512) { random.nextFloat() * 2f - 1f }
            val score = EmbeddingMath.cosine(a, b)!!
            assertTrue(score.isFinite() && score in -1f..1f)
            assertEquals(1f, EmbeddingMath.cosine(a, a)!!, 0.000001f)
        }
    }

    @Test
    fun rankAppliesTopKInclusiveThresholdAndExclusion() {
        val candidates = linkedMapOf(
            40L to floatArrayOf(3f, 4f),
            90L to floatArrayOf(-1f, 0f),
            50L to floatArrayOf(1f, 0f),
            10L to floatArrayOf(3f, 4f),
            20L to floatArrayOf(0f, 1f)
        )
        val query = floatArrayOf(2f, 0f)
        assertEquals(listOf(50L, 10L), EmbeddingMath.rank(query, candidates, 0.6f, 2).map { it.id })
        assertEquals(listOf(10L, 40L), EmbeddingMath.rank(query, candidates, 0.6f, 5, 50L).map { it.id })
        assertTrue(EmbeddingMath.rank(query, candidates, 1.01f, 5).isEmpty())
    }

    @Test
    fun rankTieOrderingDoesNotDependOnInputOrder() {
        val descending = (100L downTo 1L).associateWith { floatArrayOf(1f, 0f) }
        val ascending = descending.toSortedMap()
        val expected = listOf(1L, 2L, 3L)
        assertEquals(expected, EmbeddingMath.rank(floatArrayOf(1f, 0f), descending, -1f, 3).map { it.id })
        assertEquals(expected, EmbeddingMath.rank(floatArrayOf(1f, 0f), ascending, -1f, 3).map { it.id })
    }

    @Test
    fun rankDiscardsInvalidVectorsAndDoesNotMutateInputs() {
        val query = floatArrayOf(3f, 4f)
        val candidate = floatArrayOf(6f, 8f)
        val candidates = linkedMapOf(1L to candidate, 2L to floatArrayOf(1f))
        invalidVectors().forEachIndexed { i, values -> candidates[3L + i] = values }
        assertEquals(listOf(1L), EmbeddingMath.rank(query, candidates, -1f, 100).map { it.id })
        assertArrayEquals(floatArrayOf(3f, 4f), query, 0f)
        assertArrayEquals(floatArrayOf(6f, 8f), candidate, 0f)
    }

    @Test
    fun rankHandlesInvalidQueryThresholdAndLimit() {
        val candidates = mapOf(1L to floatArrayOf(1f, 0f))
        for (invalid in invalidVectors()) assertTrue(EmbeddingMath.rank(invalid, candidates, 0f, 1).isEmpty())
        for (limit in listOf(0, -1)) assertTrue(EmbeddingMath.rank(floatArrayOf(1f, 0f), candidates, 0f, limit).isEmpty())
        for (threshold in listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            assertTrue(EmbeddingMath.rank(floatArrayOf(1f, 0f), candidates, threshold, 1).isEmpty())
        }
        assertTrue(EmbeddingMath.rank(floatArrayOf(1f), emptyMap(), 0f, 1).isEmpty())
    }

    private fun invalidVectors() = listOf(
        floatArrayOf(), floatArrayOf(0f, -0f), floatArrayOf(Float.NaN, 1f),
        floatArrayOf(1f, Float.POSITIVE_INFINITY), floatArrayOf(Float.NEGATIVE_INFINITY, 1f)
    )
}
