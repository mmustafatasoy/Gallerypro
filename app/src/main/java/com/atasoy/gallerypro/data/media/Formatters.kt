package com.atasoy.gallerypro.data.media

import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object Formatters {

    /** 8 sn → "0:08", 72 sn → "1:12", 3723 sn → "1:02:03". */
    fun duration(ms: Long): String {
        val total = (ms + 500) / 1000
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) {
            "%d:%02d:%02d".format(h, m, s)
        } else {
            "%d:%02d".format(m, s)
        }
    }

    fun count(n: Int, locale: Locale): String = NumberFormat.getIntegerInstance(locale).format(n)

    /** "8 Kasım 2026" */
    fun longDate(millis: Long, zone: ZoneId, locale: Locale): String =
        DateTimeFormatter.ofPattern("d MMMM yyyy", locale)
            .format(Instant.ofEpochMilli(millis).atZone(zone))

    /** "Kasım" — tek başına ay adı, büyük harfle. */
    fun monthName(month: Int, locale: Locale): String =
        java.time.Month.of(month).getDisplayName(TextStyle.FULL_STANDALONE, locale)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
}
