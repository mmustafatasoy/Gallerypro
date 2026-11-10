package com.atasoy.gallerypro.data.media

import android.content.ContentValues
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.media3.common.MediaItem as PlayerMediaItem
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Media3 Transformer sadece yerel geçici .mp4'e yazar. Bittiğinde MediaStore'a
 * IS_PENDING=1 ile ayrı video olarak kopyalanır, sonra yayımlanır.
 * Orijinal Uri üzerinde hiçbir yazma/silme yapılmaz; başarısız kopya silinir.
 */
object VideoTrimExporter {

    suspend fun export(
        context: Context,
        item: MediaItem,
        range: VideoTrimRange
    ): Boolean {
        if (!item.isVideo || !VideoTrimLogic.canExport(range, item.durationMs)) return false
        val appContext = context.applicationContext
        val output = File(appContext.cacheDir, "gallerypro-trim-${System.nanoTime()}.mp4")
        return try {
            // Media3 Transformer tüm metodları ve listener'ı aynı uygulama thread'inde çalışır.
            withContext(Dispatchers.Main.immediate) {
                suspendCancellableCoroutine<Unit> { continuation ->
                    val clipped = PlayerMediaItem.Builder()
                        .setUri(item.contentUri())
                        .setClippingConfiguration(
                            PlayerMediaItem.ClippingConfiguration.Builder()
                                .setStartPositionMs(range.startMs)
                                .setEndPositionMs(range.endMs)
                                .build()
                        ).build()
                    val edited = EditedMediaItem.Builder(clipped).build()
                    val transformer = Transformer.Builder(appContext)
                        .addListener(object : Transformer.Listener {
                            override fun onCompleted(composition: Composition, result: ExportResult) {
                                if (continuation.isActive) continuation.resume(Unit)
                            }

                            override fun onError(
                                composition: Composition,
                                result: ExportResult,
                                exception: ExportException
                            ) {
                                if (continuation.isActive) continuation.resumeWithException(exception)
                            }
                        }).build()
                    continuation.invokeOnCancellation {
                        Handler(Looper.getMainLooper()).post { transformer.cancel() }
                    }
                    try {
                        transformer.start(edited, output.absolutePath)
                    } catch (error: Exception) {
                        if (continuation.isActive) continuation.resumeWithException(error)
                    }
                }
            }
            withContext(Dispatchers.IO) { publish(appContext, item, output) }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        } finally {
            withContext(NonCancellable + Dispatchers.IO) { output.delete() }
        }
    }

    private fun publish(context: Context, item: MediaItem, output: File): Boolean {
        if (!output.isFile || output.length() <= 0L) return false
        val resolver = context.contentResolver
        val base = item.displayName?.substringBeforeLast('.')?.takeIf { it.isNotBlank() }
            ?: "video_${item.id}"
        val path = item.relativePath?.takeIf {
            it.startsWith("Movies/", true) || it.startsWith("DCIM/", true)
        } ?: "Movies/Gallerypro/"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME,
                "${base}_kirpildi_${System.currentTimeMillis()}.mp4")
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.MediaColumns.RELATIVE_PATH, path)
            put(MediaStore.MediaColumns.DATE_TAKEN, item.takenAtMillis)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        var target: android.net.Uri? = null
        return try {
            target = resolver.insert(
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values
            ) ?: return false
            val uri = target!!
            resolver.openOutputStream(uri, "w").use { stream ->
                checkNotNull(stream)
                output.inputStream().use { input -> input.copyTo(stream, 64 * 1024) }
            }
            check(resolver.update(uri, ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }, null, null) > 0)
            true
        } catch (e: Exception) {
            target?.let { runCatching { resolver.delete(it, null, null) } }
            false
        }
    }
}
