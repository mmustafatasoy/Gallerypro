package com.atasoy.gallerypro

import com.atasoy.gallerypro.data.clip.VectorMath
import com.atasoy.gallerypro.data.clip.WordPieceTokenizer
import com.atasoy.gallerypro.data.index.FrameLabels
import com.atasoy.gallerypro.data.index.SearchEngine
import com.atasoy.gallerypro.data.media.MediaItem
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import kotlin.math.sqrt

/** Cümleyle arama: belirteçleyici, vektör hesapları ve sorgu metni. */
class ClipLogicTest {

    // ---------------------------------------------------------------- belirteçleyici

    private val tinyVocab = listOf(
        "[PAD]", "[UNK]", "[CLS]", "[SEP]", "gün", "bat", "##ımı", "kum", "##sal", "##da", ",", "!", "Kedi", "'", "日"
    )
    private val tiny = WordPieceTokenizer(tinyVocab)

    @Test
    fun wordPiece_longestMatchAndPadding() {
        // [CLS]=2 kum ##sal ##da gün bat ##ımı [SEP]=3, kalanı [PAD]=0
        assertArrayEquals(intArrayOf(2, 7, 8, 9, 4, 5, 6, 3, 0, 0), tiny.encode("kumsalda gün batımı", 10))
    }

    @Test
    fun wordPiece_punctuationUnknownAndCase() {
        // Noktalama ayrı parça; bilinmeyen sözcük tek [UNK]; büyük harf korunur (cased model).
        assertArrayEquals(intArrayOf(2, 12, 10, 1, 11, 3), tiny.encode("Kedi, köpek!", 6))
        assertArrayEquals(intArrayOf(2, 1, 3), tiny.encode("kedi", 3))
    }

    @Test
    fun wordPiece_truncatesAndKeepsSep() {
        val ids = tiny.encode("gün gün gün gün gün gün", 5)
        assertArrayEquals(intArrayOf(2, 4, 4, 4, 3), ids)
    }

    @Test
    fun wordPiece_cleansControlAndSplitsCjk() {
        // Boş karakter ve görünmez boşluk (biçim karakteri) atılır; Çince karakter ayrı sözcük olur.
        assertArrayEquals(intArrayOf(2, 4, 14, 4, 3), tiny.encode("gün\u0000​日gün", 5))
    }

    /** CI'da Hugging Face belirteçleyicisinin çıktısıyla karşılaştırılır (modeller indirildiyse). */
    @Test
    fun wordPiece_matchesHuggingFace() {
        val vocab = File("src/main/assets/models/clip_text_vocab.txt")
        val cases = javaClass.classLoader?.getResource("clip/clip_tokens_test.json")
        assumeTrue("Modeller indirilmemiş; atlandı", vocab.exists() && cases != null)
        val tokenizer = WordPieceTokenizer(vocab.readLines())
        val json = cases!!.readText()
        val re = Regex("\"text\":\\s*\"((?:[^\"\\\\]|\\\\.)*)\"\\s*,\\s*\"ids\":\\s*\\[([^\\]]*)\\]")
        var count = 0
        for (m in re.findAll(json)) {
            val text = unescape(m.groupValues[1])
            val expected = m.groupValues[2].split(',').map { it.trim().toInt() }.toIntArray()
            assertArrayEquals(text, expected, tokenizer.encode(text, expected.size))
            count++
        }
        assertTrue("Örnek bulunamadı", count > 0)
    }

    private fun unescape(s: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                when (val n = s[i + 1]) {
                    'u' -> {
                        sb.append(s.substring(i + 2, i + 6).toInt(16).toChar())
                        i += 6
                        continue
                    }
                    'n' -> sb.append('\n')
                    't' -> sb.append('\t')
                    else -> sb.append(n)
                }
                i += 2
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }

    // ---------------------------------------------------------------- vektörler

    private fun unit(vararg x: Float): FloatArray {
        val n = sqrt(x.fold(0f) { a, v -> a + v * v })
        return FloatArray(x.size) { x[it] / n }
    }

    @Test
    fun vector_bytesRoundTrip() {
        val v = floatArrayOf(0.5f, -1.25f, 3e-4f, 0f)
        assertArrayEquals(v, VectorMath.fromBytes(VectorMath.toBytes(v)), 0f)
        assertEquals(16, VectorMath.toBytes(v).size)
    }

    @Test
    fun vector_nearestKeepsWindowAroundBest() {
        val q = unit(1f, 0f)
        val candidates = listOf(
            1L to unit(0.30f, 0.954f), // ~0,30
            2L to unit(0.27f, 0.963f), // ~0,27
            3L to unit(0.22f, 0.975f), // ~0,22: en iyiden 0,08 aşağıda → dışarıda
            4L to unit(0.10f, 0.995f) // eşiğin altında
        )
        val got = VectorMath.nearest(q, candidates, minSimilarity = 0.20f, window = 0.06f)
        assertEquals(listOf(1L, 2L), got.map { it.first })
        assertTrue(got[0].second > got[1].second)
    }

    @Test
    fun vector_nearestNothingAboveMinimum() {
        val q = unit(1f, 0f)
        val got = VectorMath.nearest(q, listOf(1L to unit(0.1f, 0.99f)), minSimilarity = 0.2f)
        assertTrue(got.isEmpty())
        assertTrue(VectorMath.nearest(q, emptyList()).isEmpty())
    }

    @Test
    fun vector_similarAboveThresholdBestFirstWithLimit() {
        val target = unit(1f, 0f)
        val candidates = listOf(
            1L to unit(0.82f, 0.572f), // ~0,82
            2L to unit(0.97f, 0.243f), // ~0,97
            3L to unit(0.60f, 0.80f), // ~0,60: benzemez
            4L to unit(0.90f, 0.436f) // ~0,90
        )
        assertEquals(listOf(2L, 4L, 1L), VectorMath.similar(target, candidates))
        assertEquals(listOf(2L, 4L), VectorMath.similar(target, candidates, limit = 2))
        assertTrue(VectorMath.similar(target, candidates, minSimilarity = 0.99f).isEmpty())
    }

    @Test
    fun vector_meanIsUnitLength() {
        val m = VectorMath.mean(listOf(unit(1f, 0f), unit(0f, 1f)))!!
        assertEquals(1f, VectorMath.dot(m, m), 1e-5f)
        assertEquals(m[0], m[1], 1e-6f)
        assertTrue(VectorMath.mean(emptyList()) == null)
        val single = unit(0.6f, 0.8f)
        assertArrayEquals(single, VectorMath.mean(listOf(single)), 0f)
    }

    @Test
    fun frameLabels_keepBestConfidenceAcrossFrames() {
        val frames = listOf(
            listOf("Dog" to 0.70f, "Grass" to 0.65f),
            listOf("Beach" to 0.90f, "Dog" to 0.95f),
            listOf("Sky" to 0.63f, "Grass" to 0.80f)
        )
        assertEquals(listOf("Dog", "Beach", "Grass", "Sky"), FrameLabels.merge(frames, 8))
        assertEquals(listOf("Dog", "Beach"), FrameLabels.merge(frames, 2))
        assertTrue(FrameLabels.merge(emptyList(), 8).isEmpty())
    }

    // ---------------------------------------------------------------- sorgu metni

    @Test
    fun semanticText_dropsFilterWordsKeepsOriginalSpelling() {
        assertEquals("Kumsalda gün batımı", SearchEngine.semanticText("Kumsalda gün batımı 2023"))
        assertEquals("köpek", SearchEngine.semanticText("Temmuz videolar köpek"))
        assertEquals("fatura", SearchEngine.semanticText("ekran görüntüsü fatura"))
        assertEquals("", SearchEngine.semanticText("2024 favoriler"))
        assertEquals("sunset", SearchEngine.semanticText("  sunset   today "))
    }

    @Test
    fun merge_keywordFirstWithoutDuplicates() {
        fun item(id: Long) = MediaItem(
            id = id, isVideo = false, takenAtMillis = id, durationMs = 0L, mimeType = "image/jpeg",
            width = 1, height = 1, album = null, isFavorite = false, relativePath = "DCIM/"
        )
        val merged = SearchEngine.merge(listOf(item(3), item(1)), listOf(item(1), item(7), item(3), item(5)))
        assertEquals(listOf(3L, 1L, 7L, 5L), merged.map { it.id })
    }
}
