package com.atasoy.gallerypro.data.media

import kotlin.math.min

/** Gömülü ISO-BMFF/MP4 verisinin ana JPEG/HEIC dosyasındaki konumu. */
internal data class MotionPhotoSegment(val offset: Long, val length: Long)

/**
 * Samsung Motion Photo (eski MotionPhoto_Data işaretli, yeni SEF işaretsiz)
 * ve dosya sonuna video ekleyen diğer JPEG Motion Photo'lar.
 *
 * XMP'ye güvenilmez: "ftyp" adayından başlayarak mp4 kutuları üzerinde
 * kaydırma yapılır; SEF trailer videoya dahil edilmez. Asıl JPG korunur.
 * [readAt] sadece istenen pencereyi okur; 108–200 MP JPG belleğe alınmaz.
 */
internal object MotionPhotoLogic {
    private const val Block = 64 * 1024
    private const val Overlap = 16
    private const val MaxTail = 128L * 1024 * 1024
    private const val MaxClip = 512L * 1024 * 1024
    private const val MinClip = 64 * 1024L
    private val Boxes = setOf("ftyp", "moov", "mdat", "free", "skip", "wide", "uuid", "sidx", "styp", "moof", "mfra", "meta")

    fun locate(length: Long, readAt: (Long, Int) -> ByteArray?): MotionPhotoSegment? {
        if (length < 128 * 1024) return null
        val start = maxOf(512L, length - MaxTail)
        var pos = start
        while (pos < length - 12) {
            val count = min(Block.toLong() + Overlap, length - pos).toInt()
            val buffer = readAt(pos, count) ?: break
            if (buffer.size < 12) break
            for (i in 4 until buffer.size - 4) {
                if (buffer[i] != 'f'.code.toByte() || buffer[i + 1] != 't'.code.toByte() ||
                    buffer[i + 2] != 'y'.code.toByte() || buffer[i + 3] != 'p'.code.toByte()) continue
                val candidate = pos + i - 4
                if (candidate < 512) continue
                val clip = validate(length, candidate, readAt)
                if (clip != null) return clip
            }
            pos += Block
        }
        return null
    }

    private fun validate(fileSize: Long, start: Long, readAt: (Long, Int) -> ByteArray?): MotionPhotoSegment? {
        var cursor = start
        var hasMovie = false
        var hasData = false
        var boxCount = 0
        while (cursor <= fileSize - 8 && boxCount++ < 128) {
            val h = readAt(cursor, 16) ?: break
            if (h.size < 8) break
            val type = String(h, 4, 4, Charsets.US_ASCII)
            if (type !in Boxes || (cursor == start && type != "ftyp")) break
            val shortSize = uint(h, 0)
            val atomSize = when (shortSize) {
                0L -> break // Trailer'ın tüm dosyayı yutmasına izin verme.
                1L -> if (h.size >= 16) ulong(h, 8) else break
                else -> shortSize
            }
            if (atomSize < (if (shortSize == 1L) 16L else 8L) ||
                atomSize > MaxClip || atomSize > fileSize - cursor) break
            if (type == "mdat") hasData = true
            if (type == "moov") hasMovie = true
            cursor += atomSize
        }
        val size = cursor - start
        return if (hasData && hasMovie && size >= MinClip && size <= MaxClip)
            MotionPhotoSegment(start, size) else null
    }

    private fun uint(data: ByteArray, index: Int): Long {
        var result = 0L
        for (i in 0..3) result = (result shl 8) or (data[index + i].toLong() and 0xffL)
        return result
    }

    private fun ulong(data: ByteArray, index: Int): Long {
        var result = 0L
        for (i in 0..7) {
            if (i == 0 && data[index] < 0) return Long.MAX_VALUE
            result = (result shl 8) or (data[index + i].toLong() and 0xffL)
        }
        return result
    }
}
