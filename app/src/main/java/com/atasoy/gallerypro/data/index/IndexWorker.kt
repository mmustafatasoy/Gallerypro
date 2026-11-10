package com.atasoy.gallerypro.data.index

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
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
import com.atasoy.gallerypro.data.clip.VectorMath
import com.atasoy.gallerypro.data.clip.VectorVersion
import com.atasoy.gallerypro.data.clip.VideoVectorVersion
import com.atasoy.gallerypro.data.media.MediaAccess
import com.atasoy.gallerypro.data.media.MediaDetailsReader
import com.atasoy.gallerypro.data.media.MediaItem
import com.atasoy.gallerypro.data.media.MediaPermissions
import com.atasoy.gallerypro.data.media.MediaStoreSource
import com.atasoy.gallerypro.data.media.contentUri
import com.atasoy.gallerypro.data.places.CityIndex
import com.atasoy.gallerypro.data.places.PlaceEntry
import com.atasoy.gallerypro.data.places.PlaceVersion
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
     * [clip] vektörü çıkarılır (etiketleri zaten olan eski öğeler). Videolar 3 kareden incelenir
     * (%10, %50, %90); kare alınamazsa videonun küçük resmi kullanılır.
     */
    fun index(context: Context, item: MediaItem, labels: Boolean, clip: ClipImageEncoder?): Pair<IndexEntry?, VectorEntry?> {
        val frames = (if (item.isVideo) runCatching { loadVideoFrames(context, item) }.getOrNull().orEmpty() else emptyList())
            .ifEmpty { listOfNotNull(runCatching { loadThumb(context, item) }.getOrNull()) }
        if (frames.isEmpty()) {
            return (if (labels) IndexEntry(item.id, item.modifiedMillis, emptyList(), "", indexVersionFor(item)) else null) to null
        }
        try {
            val vector = clip
                ?.let { encoder -> VectorMath.mean(frames.mapNotNull { encoder.embed(it) }) }
                ?.let { VectorEntry(item.modifiedMillis, it, vectorVersionFor(item)) }
            val entry = if (labels) analyze(item, frames) else null
            return entry to vector
        } finally {
            frames.forEach { it.recycle() }
        }
    }

    private fun analyze(item: MediaItem, frames: List<Bitmap>): IndexEntry {
        val perFrame = frames.map { frame ->
            runCatching {
                Tasks.await(labeler.process(InputImage.fromBitmap(frame, 0)), TimeoutSec, TimeUnit.SECONDS)
                    .map { it.text to it.confidence }
            }.getOrDefault(emptyList())
        }
        val text = if (item.isVideo) {
            ""
        } else {
            runCatching {
                Tasks.await(recognizer.process(InputImage.fromBitmap(frames.first(), 0)), TimeoutSec, TimeUnit.SECONDS).text
            }.getOrDefault("")
        }
        return IndexEntry(
            id = item.id,
            modified = item.modifiedMillis,
            labels = FrameLabels.merge(perFrame, MaxLabels),
            text = text.replace('\n', ' ').take(MaxText),
            version = indexVersionFor(item)
        )
    }

    /** Videonun 3 karesi, en çok 640 px (yazılım bitmap'i). */
    private fun loadVideoFrames(context: Context, item: MediaItem): List<Bitmap> {
        val out = ArrayList<Bitmap>(VideoFractions.size)
        MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource(context, item.contentUri())
            val durationMs = item.durationMs.takeIf { it > 0 }
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                ?: 0L
            val times = if (durationMs > 0) VideoFractions.map { (durationMs * it * 1000).toLong() } else listOf(0L)
            for (us in times) {
                val frame = runCatching {
                    retriever.getScaledFrameAtTime(us, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, ThumbPx, ThumbPx)
                }.getOrNull() ?: continue
                out += software(frame)
            }
        }
        return out
    }

    private fun loadThumb(context: Context, item: MediaItem): Bitmap =
        software(context.contentResolver.loadThumbnail(item.contentUri(), Size(ThumbPx, ThumbPx), null))

    private fun software(bitmap: Bitmap): Bitmap {
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
        val VideoFractions = listOf(0.1, 0.5, 0.9)
    }
}

/** Öğenin dizin sürümü: videolar 3 kareden incelendiği için ayrı sürüm taşır. */
internal fun indexVersionFor(item: MediaItem): Int = if (item.isVideo) VideoIndexVersion else IndexVersion

internal fun vectorVersionFor(item: MediaItem): Int = if (item.isVideo) VideoVectorVersion else VectorVersion

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
        val knownPlaces = store.places.value
        val alive = HashSet<Long>(items.size * 2)
        items.forEach { alive += it.id }
        store.remove((known.keys + knownVectors.keys + knownPlaces.keys).filter { it !in alive })

        // Yer adları: konum izni ve APK'da şehir listesi varsa. İzin sonradan verilirse o zaman okunur.
        val cities = if (MediaDetailsReader.hasLocationPermission(context)) CityIndex.load(context) else null
        fun needsPlace(item: MediaItem): Boolean {
            if (cities == null) return false
            val p = knownPlaces[item.id]
            return p == null || p.modified != item.modifiedMillis || p.version < PlaceVersion
        }

        // Cümleyle arama modeli APK'da yoksa yalnızca etiket ve yazı çıkarılır.
        val clip = ClipImageEncoder.createOrNull(context)
        fun needsLabels(item: MediaItem): Boolean {
            val e = known[item.id]
            return e == null || e.modified != item.modifiedMillis || e.version < indexVersionFor(item)
        }
        fun needsVector(item: MediaItem): Boolean {
            if (clip == null) return false
            val v = knownVectors[item.id]
            return v == null || v.modified != item.modifiedMillis || v.version < vectorVersionFor(item)
        }
        val todo = items
            .filter { needsLabels(it) || needsVector(it) || needsPlace(it) }
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
                val places = HashMap<Long, PlaceEntry>(BatchSize * 2)
                fun flush() {
                    store.put(batch.toList())
                    store.putVectors(HashMap(vectors))
                    store.putPlaces(HashMap(places))
                    batch.clear()
                    vectors.clear()
                    places.clear()
                }
                for (item in todo) {
                    if (isStopped) break
                    val labels = needsLabels(item)
                    val vector = needsVector(item)
                    if (labels || vector) {
                        // Uygulama ekrandayken yavaş gider: kaydırma ve fotoğraf açma öncelikli.
                        if (appVisible()) Thread.sleep(VisiblePauseMs)
                        val (entry, v) = indexer.index(context, item, labels, clip.takeIf { vector })
                        if (entry != null) batch += entry
                        if (v != null) vectors[item.id] = v
                    }
                    if (cities != null && needsPlace(item)) places[item.id] = readPlace(context, item, cities)
                    done++
                    if (batch.size + vectors.size + places.size >= BatchSize) {
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

    /** Fotoğrafın/videonun konumu ve en yakın şehir. Konum yoksa boş kayıt. */
    private fun readPlace(context: Context, item: MediaItem, cities: CityIndex): PlaceEntry {
        val details = MediaDetailsReader.read(context, item)
        val lat = details.latitude
        val lon = details.longitude
        val city = if (lat != null && lon != null) cities.nearest(lat, lon) else null
        return PlaceEntry(
            modified = item.modifiedMillis,
            lat = lat,
            lon = lon,
            city = city?.name.orEmpty(),
            admin = city?.admin.orEmpty(),
            country = city?.country.orEmpty()
        )
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
