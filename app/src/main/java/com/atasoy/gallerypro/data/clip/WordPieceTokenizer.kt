package com.atasoy.gallerypro.data.clip

import java.text.Normalizer

/**
 * Çok dilli DistilBERT'in (büyük/küçük harf duyarlı, `distilbert-base-multilingual-cased`) belirteçleyicisi;
 * Hugging Face BertTokenizer'ın aynısı: metni temizle → Çince karakterleri ayır → boşluk ve noktalamadan böl →
 * WordPiece (en uzun eşleşme, devam parçaları "##"). Başa [CLS], sona [SEP], kalanı [PAD] (0).
 * Saf Kotlin; JVM'de test edilir (CI'da Hugging Face çıktısıyla karşılaştırılır).
 */
class WordPieceTokenizer(vocab: List<String>) {

    private val ids: Map<String, Int> = HashMap<String, Int>(vocab.size * 2).also { map ->
        vocab.forEachIndexed { i, token -> if (token.isNotEmpty()) map.putIfAbsent(token, i) }
    }
    private val pad = ids["[PAD]"] ?: 0
    private val unk = ids["[UNK]"] ?: 100
    private val cls = ids["[CLS]"] ?: 101
    private val sep = ids["[SEP]"] ?: 102

    /** [maxLength] uzunluğunda kimlik dizisi (dolgu 0). */
    fun encode(text: String, maxLength: Int): IntArray {
        val pieces = ArrayList<Int>()
        for (word in basicTokens(text)) {
            wordPiece(word, pieces)
            if (pieces.size >= maxLength - 2) break
        }
        val out = IntArray(maxLength) { pad }
        out[0] = cls
        val n = minOf(pieces.size, maxLength - 2)
        for (i in 0 until n) out[i + 1] = pieces[i]
        out[n + 1] = sep
        return out
    }

    internal fun basicTokens(text: String): List<String> {
        val nfc = Normalizer.normalize(text, Normalizer.Form.NFC)
        val sb = StringBuilder(nfc.length + 8)
        var i = 0
        while (i < nfc.length) {
            val cp = nfc.codePointAt(i)
            i += Character.charCount(cp)
            when {
                cp == 0 || cp == 0xFFFD || isControl(cp) -> Unit
                isWhitespace(cp) -> sb.append(' ')
                isChinese(cp) -> sb.append(' ').appendCodePoint(cp).append(' ')
                else -> sb.appendCodePoint(cp)
            }
        }
        val out = ArrayList<String>()
        for (token in sb.split(' ')) {
            if (token.isEmpty()) continue
            val current = StringBuilder()
            var j = 0
            while (j < token.length) {
                val cp = token.codePointAt(j)
                j += Character.charCount(cp)
                if (isPunctuation(cp)) {
                    if (current.isNotEmpty()) {
                        out += current.toString()
                        current.setLength(0)
                    }
                    out += String(Character.toChars(cp))
                } else {
                    current.appendCodePoint(cp)
                }
            }
            if (current.isNotEmpty()) out += current.toString()
        }
        return out
    }

    private fun wordPiece(word: String, out: MutableList<Int>) {
        if (word.codePointCount(0, word.length) > MaxWordChars) {
            out += unk
            return
        }
        val start = out.size
        var begin = 0
        while (begin < word.length) {
            var end = word.length
            var found = -1
            while (begin < end) {
                val sub = word.substring(begin, end)
                val key = if (begin > 0) "##$sub" else sub
                val id = ids[key]
                if (id != null) {
                    found = id
                    break
                }
                end = word.offsetByCodePoints(end, -1)
            }
            if (found < 0) {
                while (out.size > start) out.removeAt(out.size - 1)
                out += unk
                return
            }
            out += found
            begin = end
        }
    }

    private companion object {
        const val MaxWordChars = 100

        fun isWhitespace(cp: Int): Boolean =
            cp == ' '.code || cp == '\t'.code || cp == '\n'.code || cp == '\r'.code ||
                Character.getType(cp) == Character.SPACE_SEPARATOR.toInt()

        fun isControl(cp: Int): Boolean {
            if (cp == '\t'.code || cp == '\n'.code || cp == '\r'.code) return false
            return when (Character.getType(cp)) {
                Character.CONTROL.toInt(), Character.FORMAT.toInt(), Character.UNASSIGNED.toInt(),
                Character.PRIVATE_USE.toInt(), Character.SURROGATE.toInt() -> true
                else -> false
            }
        }

        fun isPunctuation(cp: Int): Boolean {
            if (cp in 33..47 || cp in 58..64 || cp in 91..96 || cp in 123..126) return true
            return when (Character.getType(cp)) {
                Character.CONNECTOR_PUNCTUATION.toInt(), Character.DASH_PUNCTUATION.toInt(),
                Character.START_PUNCTUATION.toInt(), Character.END_PUNCTUATION.toInt(),
                Character.INITIAL_QUOTE_PUNCTUATION.toInt(), Character.FINAL_QUOTE_PUNCTUATION.toInt(),
                Character.OTHER_PUNCTUATION.toInt() -> true
                else -> false
            }
        }

        fun isChinese(cp: Int): Boolean =
            cp in 0x4E00..0x9FFF || cp in 0x3400..0x4DBF || cp in 0x20000..0x2A6DF ||
                cp in 0x2A700..0x2B73F || cp in 0x2B740..0x2B81F || cp in 0x2B820..0x2CEAF ||
                cp in 0xF900..0xFAFF || cp in 0x2F800..0x2FA1F
    }
}
