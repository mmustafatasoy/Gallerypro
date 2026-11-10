package com.atasoy.gallerypro.data.clip

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.min

/**
 * Cümleyle arama modelleri (LiteRT). Dosyalar APK'nın `assets/models/` klasöründedir; derlemede
 * "modeller" sürümünden indirilir. Yoksa (örn. yerel derleme) cümleyle arama sessizce kapalı kalır.
 * Modeller sıkıştırılmadan paketlenir ve belleğe eşlenir; Java yığınına yüklenmez.
 */
object ClipAssets {
    const val ImageModel = "models/clip_image.tflite"
    const val TextModel = "models/clip_text.tflite"
    const val Vocab = "models/clip_text_vocab.txt"
    const val Dim = 512

    fun available(context: Context): Boolean = runCatching {
        context.assets.openFd(ImageModel).close()
        context.assets.openFd(TextModel).close()
        context.assets.open(Vocab).close()
        true
    }.getOrDefault(false)

    internal fun map(context: Context, name: String): MappedByteBuffer {
        val fd = context.assets.openFd(name)
        FileInputStream(fd.fileDescriptor).use { stream ->
            return stream.channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
        }
    }

    internal fun interpreter(context: Context, name: String, threads: Int): Interpreter =
        Interpreter(map(context, name), Interpreter.Options().setNumThreads(threads))
}

/** Fotoğraftan 512 boyutlu vektör. Dizinleyici kullanır; tek iş parçacığından çağrılır. */
class ClipImageEncoder private constructor(private val interpreter: Interpreter) : AutoCloseable {

    private val input: ByteBuffer = ByteBuffer.allocateDirect(Side * Side * 3 * 4).order(ByteOrder.nativeOrder())
    private val output = Array(1) { FloatArray(ClipAssets.Dim) }
    private val pixels = IntArray(Side * Side)

    /** Ortadan kare kırpılır, 224 px'e küçültülür (CLIP'in ön işlemi; renk düzeltmesi model içinde). */
    @Synchronized
    fun embed(bitmap: Bitmap): FloatArray? = runCatching {
        val side = min(bitmap.width, bitmap.height)
        if (side <= 0) return null
        val square = Bitmap.createBitmap(bitmap, (bitmap.width - side) / 2, (bitmap.height - side) / 2, side, side)
        val scaled = Bitmap.createScaledBitmap(square, Side, Side, true)
        scaled.getPixels(pixels, 0, Side, 0, 0, Side, Side)
        if (scaled !== square && scaled !== bitmap) scaled.recycle()
        if (square !== bitmap) square.recycle()
        input.rewind()
        for (p in pixels) {
            input.putFloat(((p shr 16) and 0xFF) / 255f)
            input.putFloat(((p shr 8) and 0xFF) / 255f)
            input.putFloat((p and 0xFF) / 255f)
        }
        input.rewind()
        interpreter.run(input, output)
        output[0].copyOf()
    }.getOrNull()

    override fun close() {
        runCatching { interpreter.close() }
    }

    companion object {
        private const val Side = 224

        fun createOrNull(context: Context): ClipImageEncoder? {
            if (!ClipAssets.available(context)) return null
            return runCatching { ClipImageEncoder(ClipAssets.interpreter(context, ClipAssets.ImageModel, threads = 2)) }.getOrNull()
        }
    }
}

/** Sorgu metninden (Türkçe dahil) 512 boyutlu vektör. Arama ekranı kullanır; ilk kullanımda yüklenir. */
class ClipTextEncoder(private val context: Context) {

    private var interpreter: Interpreter? = null
    private var tokenizer: WordPieceTokenizer? = null
    private var failed = false
    private var lastText: String? = null
    private var lastVector: FloatArray? = null

    val available: Boolean by lazy { ClipAssets.available(context) }

    @Synchronized
    fun embed(text: String): FloatArray? {
        if (!available || failed) return null
        val query = text.trim()
        if (query.isEmpty()) return null
        if (query == lastText) return lastVector
        return runCatching {
            val tok = tokenizer ?: WordPieceTokenizer(
                context.assets.open(ClipAssets.Vocab).bufferedReader().readLines()
            ).also { tokenizer = it }
            val model = interpreter ?: ClipAssets.interpreter(context, ClipAssets.TextModel, threads = 2)
                .also { interpreter = it }
            val ids = arrayOf(tok.encode(query, SeqLength))
            val output = Array(1) { FloatArray(ClipAssets.Dim) }
            model.run(ids, output)
            output[0]
        }.onFailure { failed = true }.getOrNull().also {
            lastText = query
            lastVector = it
        }
    }

    private companion object {
        const val SeqLength = 64
    }
}
