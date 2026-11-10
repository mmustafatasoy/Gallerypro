package com.atasoy.gallerypro.data.index

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Process
import android.util.Size
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.atasoy.gallerypro.GalleryApp
import com.atasoy.gallerypro.data.clip.ClipImageEncoder
import com.atasoy.gallerypro.data.clip.VectorEntry
import com.atasoy.gallerypro.data.clip.VectorVersion
import com.atasoy.gallerypro.data.media.MediaAccess
import com.atasoy.gallerypro.data.media.MediaItem
import com.atasoy.gallerypro.data.media.MediaPermissions
import com.atasoy.gallerypro.data.media.MediaStoreSource
import com.atasoy.gallerypro.data.media.contentUri
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.Closeable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Tek bir fotoğrafı ML Kit ile inceler: nesne etiketleri ve fotoğraftaki yazı.
 * Modeller uygulamanın içindedir; internet kullanılmaz. Orijinal değil, MediaStore'un
 * küçük kopyası okunur.
 */
class MediaIndexer : Closeable {

    // ML Kit tek, düşük öncelikli bir iş parçacığında çalışır; işlemcinin tamamını alıp arayüzü kasmasın.
    private val executor: ExecutorService = Executors.newSingleThreadExecutor { task ->
        Thread({
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            task.run()
        }, "gallery-index")
    }
    private val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder().setConfidenceThreshold(MinConfidence).setExecutor(executor).build()
    )
    private val recognizer = TextRecognition.getClient(
        TextRecognizerOptions.Builder().setExecutor(executor).build()
    )

    /**
     * Çalışan iş parçacığını bekletir; ana iş parçacığında çağrılmaz. [labels] false ise yalnızca
     * [clip] vektörü çıkarılır (etiketleri zaten olan eski öğeler).
     */
    fun index(context: Context, item: MediaItem, labels: Boolean, clip: ClipImageEncoder?): Pair<IndexEntry?, VectorEntry?> {
        val bitmap = runCatching { loadThumb(context, item) }.getOrNull()
            ?: return (if (labels) IndexEntry(item.id, item.modifiedMillis, emptyList(), "") else null) to null
        try {
            val vector = clip?.embed(bitmap)?.let { VectorEntry(item.modifiedMillis, it) }
            val entry = if (labels) analyze(item, bitmap) else null
            return entry to vector
        } finally {
            bitmap.recycle()
        }
    }

    private fun analyze(item: MediaItem, bitmap: Bitmap): IndexEntry {
        run {
            val image = InputImage.fromBitmap(bitmap, 0)
            val labels = runCatching {
                Tasks.await(labeler.process(image), TimeoutSec, TimeUnit.SECONDS)
                    .sortedByDescending { it.confidence }
                    .take(MaxLabels)
                    .map { it.text }
            }.getOrDefault(emptyList())
            val text = if (item.isVideo) {
                ""
            } else {
                runCatching {
                    Tasks.await(recognizer.process(image), TimeoutSec, TimeUnit.SECONDS).text
                }.getOrDefault("")
            }
            return IndexEntry(
                id = item.id,
                modified = item.modifiedMillis,
                labels = labels,
                text = text.replace('\n', ' ').take(MaxText)
            )
        }
    }

    private fun loadThumb(context: Context, item: MediaItem): Bitmap {
        val bitmap = context.contentResolver.loadThumbnail(item.contentUri(), Size(ThumbPx, ThumbPx), null)
        // ML Kit piksellere erişir; donanım bitmap'i yazılıma kopyalanır.
        if (bitmap.config == Bitmap.Config.HARDWARE) {
            val copy = bitmap.copy(Bitmap.Config.ARGB_8888, false)
            bitmap.recycle()
            return copy
        }
        return bitmap
    }

    override fun close() {
        runCatching { labeler.close() }
        runCatching { recognizer.close() }
        executor.shutdown()
    }

    private companion object {
        const val ThumbPx = 640
        const val MinConfidence = 0.62f
        const val MaxLabels = 8
        const val MaxText = 1500
        const val TimeoutSec = 20L
    }
}

/**
 * Arka planda dizinleme. Yalnızca yeni ve değişen öğeleri işler, silinenleri dizinden çıkarır.
 * Yarıda kesilirse (Android işleri en fazla ~10 dakika çalıştırır) işlenenler kalır,
 * sonraki çalışmada kaldığı yerden devam eder. En yeni fotoğraflar önce işlenir.
 */
class IndexWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
        val context = applicationContext
        if (MediaPermissions.access(context) == MediaAccess.None) return Result.success()
        val store = (context as GalleryApp).container.index
        store.ensureLoaded()

        val items = runCatching { MediaStoreSource(context).queryAll() }.getOrElse { return Result.retry() }
        val known = store.entries.value
        val knownVectors = store.vectors.value
        val alive = HashSet<Long>(items.size * 2)
        items.forEach { alive += it.id }
        store.remove((known.keys + knownVectors.keys).filter { it !in alive })

        // Cümleyle arama modeli APK'da yoksa yalnızca etiket ve yazı çıkarılır.
        val clip = ClipImageEncoder.createOrNull(context)
        fun needsLabels(item: MediaItem): Boolean {
            val e = known[item.id]
            return e == null || e.modified != item.modifiedMillis || e.version < IndexVersion
        }
        fun needsVector(item: MediaItem): Boolean {
            if (clip == null) return false
            val v = knownVectors[item.id]
            return v == null || v.modified != item.modifiedMillis || v.version < VectorVersion
        }
        val todo = items
            .filter { needsLabels(it) || needsVector(it) }
            .sortedByDescending { it.takenAtMillis }
        var done = items.size - todo.size
        store.setStatus(IndexStatus(running = todo.isNotEmpty(), done = done, total = items.size))
        if (todo.isEmpty()) {
            clip?.close()
            return Result.success()
        }

        try {
            MediaIndexer().use { indexer ->
                val batch = ArrayList<IndexEntry>(BatchSize)
                val vectors = HashMap<Long, VectorEntry>(BatchSize * 2)
                fun flush() {
                    store.put(batch.toList())
                    store.putVectors(HashMap(vectors))
                    batch.clear()
                    vectors.clear()
                }
                for (item in todo) {
                    if (isStopped) break
                    // Uygulama ekrandayken yavaş gider: kaydırma ve fotoğraf açma öncelikli.
                    if (appVisible()) Thread.sleep(VisiblePauseMs)
                    val (entry, vector) = indexer.index(context, item, needsLabels(item), clip.takeIf { needsVector(item) })
                    if (entry != null) batch += entry
                    if (vector != null) vectors[item.id] = vector
                    done++
                    if (batch.size + vectors.size >= BatchSize) {
                        flush()
                        store.setStatus(IndexStatus(running = true, done = done, total = items.size))
                    }
                }
                flush()
            }
        } catch (t: Throwable) {
            clip?.close()
            // ML Kit yüklenemezse (ör. cihaz desteklemiyor) uygulama etkilenmez; sonra yeniden denenir.
            store.setStatus(IndexStatus(running = false, done = done, total = items.size))
            return Result.retry()
        }
        clip?.close()
        store.setStatus(IndexStatus(running = false, done = done, total = items.size))
        return Result.success()
    }

    private fun appVisible(): Boolean {
        val info = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(info)
        return info.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
    }

    private companion object {
        const val BatchSize = 20
        const val VisiblePauseMs = 250L
    }
}

/** Dizinleme zamanlaması: telefon şarjdayken ve boştayken düzenli; istenirse hemen. */
object IndexScheduler {

    private const val Periodic = "index-periodic"
    private const val Now = "index-now"

    fun schedule(context: Context) {
        runCatching {
            val request = PeriodicWorkRequestBuilder<IndexWorker>(6, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresCharging(true)
                        .setRequiresDeviceIdle(true)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(Periodic, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }

    fun runNow(context: Context) {
        runCatching {
            val request = OneTimeWorkRequestBuilder<IndexWorker>()
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(Now, ExistingWorkPolicy.KEEP, request)
        }
    }
}
