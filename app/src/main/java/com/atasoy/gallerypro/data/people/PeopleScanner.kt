package com.atasoy.gallerypro.data.people

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import com.atasoy.gallerypro.data.media.MediaItem
import com.atasoy.gallerypro.data.media.contentUri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.FaceDetectorYN
import org.opencv.objdetect.FaceRecognizerSF
import java.io.File
import kotlin.coroutines.coroutineContext

/**
 * Tam hassasiyetli OpenCV Zoo YuNet + SFace (FP32), iki model de cihaz içinde.
 * Kullanıcı "Tara" demeden fotoğrafları işlemez; bütün ağır işlemler Default/IO'da.
 * Her fotoğraf en fazla 640 piksel uzun kenarla yüklenir, native Mat'ler bırakılır.
 */
class PeopleScanner(private val context: Context) {
    private fun modelAsset(name: String): String {
        val root = File(context.filesDir, "people-face-models")
        root.mkdirs()
        val file = File(root, name)
        if (!file.exists() || file.length() < 100_000) {
            val temp = File(root, "$name.part")
            try {
                context.assets.open("face/$name").use { input ->
                    temp.outputStream().use { output -> input.copyTo(output, 64 * 1024) }
                }
                check(temp.length() > 100_000)
                check(temp.renameTo(file))
            } finally { temp.delete() }
        }
        return file.absolutePath
    }

    suspend fun scan(
        photos: List<MediaItem>,
        onProgress: suspend (Int, Int) -> Unit
    ): List<FaceVector> = withContext(Dispatchers.Default) {
        check(OpenCVLoader.initLocal()) { "OpenCV yüklenemedi" }
        val yunet = modelAsset("face_detection_yunet_2023mar.onnx")
        val sface = modelAsset("face_recognition_sface_2021dec.onnx")
        val detector = FaceDetectorYN.create(yunet, "", Size(320.0,320.0), 0.88f, 0.3f, 5000)
        val recognizer = FaceRecognizerSF.create(sface, "")
        val records = mutableListOf<FaceVector>()
        val allowed = photos.filter { !it.isVideo && it.id > 0L && it.mimeType?.startsWith("image/") == true }
        for ((index, photo) in allowed.withIndex()) {
            coroutineContext.ensureActive()
            try {
                records.addAll(readFaces(photo, detector, recognizer))
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { /* bozuk / desteklenmeyen bir fotoğraf taramayı durdurmasın */ }
            if (index % 8 == 0 || index + 1 == allowed.size) {
                onProgress(index + 1, allowed.size)
            }
        }
        records
    }

    private fun readFaces(
        item: MediaItem,
        detector: FaceDetectorYN,
        recognizer: FaceRecognizerSF
    ): List<FaceVector> {
        val bitmap = openSmallBitmap(item) ?: return emptyList()
        val rgba = Mat()
        val bgr = Mat()
        val detected = Mat()
        val aligned = Mat()
        val feature = Mat()
        try {
            Utils.bitmapToMat(bitmap, rgba)
            Imgproc.cvtColor(rgba, bgr, Imgproc.COLOR_RGBA2BGR)
            detector.setInputSize(Size(bgr.cols().toDouble(), bgr.rows().toDouble()))
            detector.detect(bgr, detected)
            val result = ArrayList<FaceVector>()
            for (i in 0 until minOf(detected.rows(), 10)) {
                val row = detected.row(i)
                try {
                    val values = FloatArray(detected.cols())
                    row.get(0,0,values)
                    // Çok küçük veya yanlış tespit edilen yüzleri dahil etme.
                    if (values.size < 15 || values[2] < 28f || values[3] < 28f) continue
                    recognizer.alignCrop(bgr, row, aligned)
                    recognizer.feature(aligned, feature)
                    val data = FloatArray(feature.rows() * feature.cols())
                    feature.get(0,0,data)
                    val normalized = PeopleLogic.normalized(data) ?: continue
                    result.add(FaceVector(item.id, i, item.modifiedMillis, normalized))
                } finally { row.release() }
            }
            return result
        } finally {
            bitmap.recycle()
            rgba.release()
            bgr.release()
            detected.release()
            aligned.release()
            feature.release()
        }
    }

    private fun openSmallBitmap(item: MediaItem): Bitmap? {
        val uri = item.contentUri()
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
        if (options.outWidth < 1 || options.outHeight < 1) return null
        val longest = maxOf(options.outWidth, options.outHeight)
        var sample = 1
        while (longest / sample > 640) sample *= 2
        val scaled = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, scaled)
        }
    }
}
