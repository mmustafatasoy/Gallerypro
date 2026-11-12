package com.atasoy.gallerypro.data.media

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.min

/** Yalnızca seçilen fotoğrafın, cihazdaki orijinal dosyası okunur. */
internal object MotionPhotoReader {
    private val lock = Any()
    private val checked = LinkedHashMap<String, MotionPhotoSegment?>()

    fun locate(context: Context, item: MediaItem): MotionPhotoSegment? {
        if (item.isVideo || item.mimeType?.lowercase() !in
            setOf("image/jpeg", "image/jpg", "image/heic", "image/heif")) return null
        val key = "${item.id}:${item.modifiedMillis}:${item.sizeBytes}"
        synchronized(lock) { if (checked.containsKey(key)) return checked[key] }
        val found = runCatching {
            context.contentResolver.openFileDescriptor(item.contentUri(), "r")?.use { fd ->
                FileInputStream(fd.fileDescriptor).channel.use { channel ->
                    if (channel.size() <= 0) null else MotionPhotoLogic.locate(channel.size()) { offset, count ->
                        read(channel, offset, count)
                    }
                }
            }
        }.getOrNull()
        synchronized(lock) {
            checked[key] = found
            if (checked.size > 64) checked.remove(checked.keys.first())
        }
        return found
    }

    /** MP4 sadece cache'e aktarılır; orijinal fotoğraf/SEF asla değiştirilmez. */
    fun extract(context: Context, item: MediaItem, clip: MotionPhotoSegment): Uri? =
        runCatching {
            val folder = File(context.cacheDir, "motion_clips").apply { mkdirs() }
            val destination = File(folder, "motion_${item.id}_${item.modifiedMillis}.mp4")
            if (destination.isFile && destination.length() == clip.length) return@runCatching Uri.fromFile(destination)
            // Aynı fotoğrafın eski küçük kopyalarını temizle, diğer görselleri ellemiyoruz.
            folder.listFiles()?.filter { it.name.startsWith("motion_${item.id}_") &&
                it.name != destination.name }?.forEach { it.delete() }
            val pending = File(folder, "${destination.name}.part")
            try {
                context.contentResolver.openFileDescriptor(item.contentUri(), "r")?.use { fd ->
                    FileInputStream(fd.fileDescriptor).channel.use { source ->
                        if (clip.offset < 0L || clip.length < 65_536 ||
                            clip.length > 512L * 1024 * 1024 ||
                            clip.offset > source.size() - clip.length) throw IOException("Bad motion clip bounds")
                        FileOutputStream(pending).channel.use { output ->
                            val buf = ByteBuffer.allocateDirect(128 * 1024)
                            var position = clip.offset
                            val end = clip.offset + clip.length
                            while (position < end) {
                                buf.clear()
                                buf.limit(min(buf.capacity().toLong(), end - position).toInt())
                                val n = source.read(buf, position)
                                if (n <= 0) throw IOException("Incomplete embedded MP4")
                                position += n
                                buf.flip()
                                while (buf.hasRemaining()) output.write(buf)
                            }
                            output.force(true)
                        }
                    }
                } ?: throw IOException("Photo is inaccessible")
                if (!pending.renameTo(destination)) throw IOException("Unable to publish clip")
                Uri.fromFile(destination)
            } finally {
                pending.delete()
            }
        }.getOrNull()

    private fun read(channel: FileChannel, pos: Long, length: Int): ByteArray? {
        if (pos < 0L || length <= 0) return null
        val buffer = ByteBuffer.allocate(length)
        var at = 0
        while (at < length) {
            val n = channel.read(buffer, pos + at)
            if (n <= 0) break
            at += n
        }
        return buffer.array().copyOf(at)
    }
}
