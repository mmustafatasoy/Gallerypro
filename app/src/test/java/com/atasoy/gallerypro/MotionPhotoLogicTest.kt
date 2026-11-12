package com.atasoy.gallerypro

import com.atasoy.gallerypro.data.media.MotionPhotoLogic
import com.atasoy.gallerypro.data.media.MotionPhotoSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MotionPhotoLogicTest {
    private fun box(name: String, data: ByteArray): ByteArray {
        val length = data.size + 8
        val bytes = byteArrayOf((length ushr 24).toByte(), (length ushr 16).toByte(),
            (length ushr 8).toByte(), length.toByte())
        return bytes + name.toByteArray(Charsets.US_ASCII) + data
    }
    private fun read(bytes: ByteArray): (Long, Int) -> ByteArray? = { offset, count ->
        if (offset < 0 || offset >= bytes.size) null
        else bytes.copyOfRange(offset.toInt(), minOf(bytes.size, offset.toInt() + count))
    }
    private fun sample(withSef: Boolean): Pair<ByteArray, Int> {
        val jpeg = ByteArray(180_000)
        jpeg[0] = 0xff.toByte()
        jpeg[1] = 0xd8.toByte()
        val ftyp = box("ftyp", "mp42\u0000\u0000\u0000\u0000isommp42".toByteArray(Charsets.US_ASCII))
        val video = ftyp + box("moov", ByteArray(32)) + box("mdat", ByteArray(96_000))
        val trailer = if (withSef) "SEFH....SEFT".toByteArray() else ByteArray(0)
        return (jpeg + "MotionPhoto_Data".toByteArray() + video + trailer) to (jpeg.size + 16)
    }

    @Test fun samsungMarkerAndSefTrailerAreExcluded() {
        val (file, offset) = sample(true)
        val segment = MotionPhotoLogic.locate(file.size.toLong(), read(file))!!
        assertEquals(offset.toLong(), segment.offset)
        assertEquals((file.size - offset - 12).toLong(), segment.length)
    }

    @Test fun newerSamsungWithoutMotionPhotoDataMarker() {
        val (file, offset) = sample(false)
        val noMarker = file.copyOfRange(0, offset - 16) + file.copyOfRange(offset, file.size)
        val segment = MotionPhotoLogic.locate(noMarker.size.toLong(), read(noMarker))!!
        assertEquals((offset - 16).toLong(), segment.offset)
        assertEquals((noMarker.size - segment.offset).toLong(), segment.length)
    }

    @Test fun plainJpegAndFakeFtypAreNotMotionClips() {
        val plain = ByteArray(240_000)
        assertNull(MotionPhotoLogic.locate(plain.size.toLong(), read(plain)))
        val fake = plain.clone()
        "ftyp".toByteArray().copyInto(fake, 1024)
        assertNull(MotionPhotoLogic.locate(fake.size.toLong(), read(fake)))
    }

    @Test fun truncatedMp4IsRejected() {
        val (file, offset) = sample(false)
        val cut = file.copyOf(offset + 512)
        assertNull(MotionPhotoLogic.locate(cut.size.toLong(), read(cut)))
    }
}
