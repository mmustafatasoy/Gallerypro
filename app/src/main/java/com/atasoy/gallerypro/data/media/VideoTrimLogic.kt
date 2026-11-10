package com.atasoy.gallerypro.data.media

/** Zaman aralığı milisaniyedir. Görsel sürgü ile dışa aktarma aynı mantığı kullanır. */
data class VideoTrimRange(val startMs: Long, val endMs: Long) {
    val lengthMs: Long get() = endMs - startMs
}

/** Android bağımsız; yanlış veya sıfır uzunlukta dosya çıkarılmasını önler. */
object VideoTrimLogic {
    const val MinLengthMs = 1_000L

    fun initial(durationMs: Long): VideoTrimRange? =
        if (durationMs > MinLengthMs) VideoTrimRange(0L, durationMs) else null

    fun moveStart(range: VideoTrimRange, positionMs: Long): VideoTrimRange =
        range.copy(startMs = positionMs.coerceIn(0L, (range.endMs - MinLengthMs).coerceAtLeast(0L)))

    fun moveEnd(range: VideoTrimRange, positionMs: Long, durationMs: Long): VideoTrimRange =
        range.copy(endMs = positionMs.coerceIn(range.startMs + MinLengthMs, durationMs))

    fun canExport(range: VideoTrimRange, durationMs: Long): Boolean =
        durationMs > MinLengthMs && range.startMs >= 0 && range.endMs <= durationMs &&
            range.lengthMs >= MinLengthMs &&
            (range.startMs != 0L || range.endMs != durationMs)

    fun position(fraction: Float, durationMs: Long): Long =
        (fraction.coerceIn(0f, 1f) * durationMs).toLong().coerceIn(0L, durationMs)
}
