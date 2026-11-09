package com.atasoy.gallerypro.data.index

import java.text.Normalizer
import java.util.Locale

/**
 * Arama metni sadeleştirme: küçük harf, Türkçe ve diğer aksanlar kaldırılır
 * ("Köpek" → "kopek", "IŞIK" → "isik"), harf ve rakam dışı her şey boşluk olur.
 * Böylece "kopek", "köpek" ve "KÖPEK" aynı sonucu verir. Saf Kotlin; JVM'de test edilir.
 */
object SearchText {

    private val marks = Regex("\\p{Mn}+")

    fun normalize(text: String): String {
        if (text.isEmpty()) return ""
        val decomposed = Normalizer.normalize(text, Normalizer.Form.NFD)
        val sb = StringBuilder(decomposed.length)
        var lastSpace = true
        for (raw in marks.replace(decomposed, "")) {
            val c = when (raw) {
                'ı', 'I' -> 'i'
                else -> raw.lowercaseChar()
            }
            if (c.isLetterOrDigit()) {
                sb.append(c)
                lastSpace = false
            } else if (!lastSpace) {
                sb.append(' ')
                lastSpace = true
            }
        }
        return sb.toString().trim()
    }

    fun tokens(text: String): List<String> =
        normalize(text).split(' ').filter { it.isNotEmpty() }

    /** "kedi" → "Kedi" (Türkçe i/İ kuralına uygun). */
    fun capitalize(word: String, locale: Locale): String =
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
}
