package com.atasoy.gallerypro.data.edit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageDecoder
import android.graphics.Paint
import com.atasoy.gallerypro.data.media.MediaItem
import com.atasoy.gallerypro.data.media.contentUri
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Düzenlemenin piksel tarafı. Önizleme ekranda GPU ile çizilir (renk matrisi,
 * Android 13+'ta AGSL keskinlik); kaydederken aynı formüller burada tam
 * çözünürlükte uygulanır: geometri → renk → keskinlik → vinyet.
 */
object EditRenderer {

    /** Önizlemenin uzun kenarı en fazla bu kadar olur. */
    const val PreviewMaxSide = 2048

    /** Filtre küçük resimlerinin uzun kenarı. */
    const val FilterThumbSide = 180

    /** Orijinal varsa yedekten (düzenlenmemiş hali), yoksa MediaStore'dan çözer. */
    private fun source(context: Context, item: MediaItem, store: EditStore): ImageDecoder.Source =
        if (store.hasBackup(item.id)) {
            ImageDecoder.createSource(store.backupFile(item.id))
        } else {
            ImageDecoder.createSource(context.contentResolver, item.contentUri())
        }

    fun decodePreview(context: Context, item: MediaItem, store: EditStore): Bitmap =
        decode(source(context, item, store)) { w, h -> sampleFor(max(w, h).toLong(), PreviewMaxSide.toLong()) }

    /**
     * Kaydetmek için tam çözünürlük. Bellek yetmeyecekse (ör. 200 MP) yarıya örneklenir;
     * sınır uygulamanın bellek payına göre hesaplanır.
     */
    fun decodeFull(source: ImageDecoder.Source): Bitmap {
        val maxPixels = maxEditablePixels()
        return decode(source) { w, h ->
            var n = 1
            while (w.toLong() * h / (n.toLong() * n) > maxPixels) n *= 2
            n
        }
    }

    fun maxEditablePixels(): Long {
        // Kaynak + çıktı + kodlama tamponu ≈ piksel başına 4 × 2,4 bayt.
        val budget = Runtime.getRuntime().maxMemory() * 0.6
        return (budget / (4.0 * 2.4)).toLong().coerceIn(4_000_000L, 64_000_000L)
    }

    private fun sampleFor(side: Long, limit: Long): Int {
        var n = 1
        while (side / n > limit) n *= 2
        return n
    }

    /**
     * EXIF yönü ImageDecoder tarafından uygulanır; örnekleme oranı yönden bağımsız olduğu
     * için en-boy oranı bozulmaz.
     */
    private fun decode(source: ImageDecoder.Source, sample: (w: Int, h: Int) -> Int): Bitmap =
        ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val n = sample(info.size.width, info.size.height)
            if (n > 1) decoder.setTargetSampleSize(n)
        }.let { bitmap ->
            if (bitmap.config == Bitmap.Config.ARGB_8888) bitmap else bitmap.copy(Bitmap.Config.ARGB_8888, false).also {
                bitmap.recycle()
            }
        }

    fun filterThumb(preview: Bitmap): Bitmap {
        val scale = FilterThumbSide.toFloat() / max(preview.width, preview.height)
        if (scale >= 1f) return preview
        return Bitmap.createScaledBitmap(
            preview,
            (preview.width * scale).toInt().coerceAtLeast(1),
            (preview.height * scale).toInt().coerceAtLeast(1),
            true
        )
    }

    /** "Otomatik" için ortalama parlaklık, parlaklık sapması ve doygunluk ölçümü. */
    fun measureAuto(bitmap: Bitmap): AutoCorrection {
        val w = bitmap.width
        val h = bitmap.height
        if (w == 0 || h == 0) return AutoCorrection()
        val step = max(1, max(w, h) / 160)
        var n = 0
        var sum = 0.0
        var sumSq = 0.0
        var sat = 0.0
        val row = IntArray(w)
        var y = 0
        while (y < h) {
            bitmap.getPixels(row, 0, w, 0, y, w, 1)
            var x = 0
            while (x < w) {
                val c = row[x]
                val r = (c shr 16 and 0xFF) / 255.0
                val g = (c shr 8 and 0xFF) / 255.0
                val b = (c and 0xFF) / 255.0
                val luma = 0.2126 * r + 0.7152 * g + 0.0722 * b
                val mx = maxOf(r, g, b)
                val mn = minOf(r, g, b)
                sum += luma
                sumSq += luma * luma
                sat += if (mx > 0.0) (mx - mn) / mx else 0.0
                n++
                x += step
            }
            y += step
        }
        val mean = sum / n
        val std = sqrt(max(0.0, sumSq / n - mean * mean))
        return ColorMath.autoCorrection(mean.toFloat(), std.toFloat(), (sat / n).toFloat())
    }

    /** Ayarları [src]'ye uygular; yeni bitmap döner, [src]'ye dokunmaz. */
    fun render(src: Bitmap, params: EditParams): Bitmap {
        val w = src.width
        val h = src.height
        val (ow, oh) = CropMath.outputSize(params.crop, w, h)
        val out = Bitmap.createBitmap(ow, oh, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
        if (params.hasColor) {
            paint.colorFilter = ColorMatrixColorFilter(ColorMatrix(ColorMath.toAndroid(ColorMath.full(params))))
        }
        val cx = w / 2f
        val cy = h / 2f
        val scale = CropMath.coverScale(params.angle.toFloat(), w.toFloat(), h.toFloat())
        canvas.translate(-params.crop.left * w, -params.crop.top * h)
        canvas.rotate(params.angle.toFloat(), cx, cy)
        canvas.scale(scale, scale, cx, cy)
        canvas.drawBitmap(src, 0f, 0f, paint)
        sharpen(out, params.value(AdjustTool.Sharpness))
        vignette(out, params.value(AdjustTool.Vignette))
        return out
    }

    /**
     * Keskin olmayan maske: c + k·(c − 3×3 ortalama). Eksi değer yumuşatır.
     * Satır satır çalışır; bellekte sadece üç satır tutulur.
     */
    private fun sharpen(bmp: Bitmap, value: Int) {
        if (value == 0) return
        val w = bmp.width
        val h = bmp.height
        if (w < 3 || h < 3) return
        val k = value / 100f
        var prev = IntArray(w)
        var cur = IntArray(w)
        var next = IntArray(w)
        val out = IntArray(w)
        val colR = IntArray(w)
        val colG = IntArray(w)
        val colB = IntArray(w)
        bmp.getPixels(cur, 0, w, 0, 0, w, 1)
        System.arraycopy(cur, 0, prev, 0, w)
        bmp.getPixels(next, 0, w, 0, 1, w, 1)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val a = prev[x]
                val b = cur[x]
                val c = next[x]
                colR[x] = (a shr 16 and 0xFF) + (b shr 16 and 0xFF) + (c shr 16 and 0xFF)
                colG[x] = (a shr 8 and 0xFF) + (b shr 8 and 0xFF) + (c shr 8 and 0xFF)
                colB[x] = (a and 0xFF) + (b and 0xFF) + (c and 0xFF)
            }
            for (x in 0 until w) {
                val l = if (x > 0) x - 1 else 0
                val r = if (x < w - 1) x + 1 else w - 1
                val p = cur[x]
                val pr = p shr 16 and 0xFF
                val pg = p shr 8 and 0xFF
                val pb = p and 0xFF
                val br = (colR[l] + colR[x] + colR[r]) / 9f
                val bg = (colG[l] + colG[x] + colG[r]) / 9f
                val bb = (colB[l] + colB[x] + colB[r]) / 9f
                val nr = (pr + k * (pr - br)).toInt().coerceIn(0, 255)
                val ng = (pg + k * (pg - bg)).toInt().coerceIn(0, 255)
                val nb = (pb + k * (pb - bb)).toInt().coerceIn(0, 255)
                out[x] = (p and -0x1000000) or (nr shl 16) or (ng shl 8) or nb
            }
            bmp.setPixels(out, 0, w, 0, y, w, 1)
            val t = prev
            prev = cur
            cur = next
            next = t
            if (y + 2 < h) bmp.getPixels(next, 0, w, 0, y + 2, w, 1) else System.arraycopy(cur, 0, next, 0, w)
        }
    }

    /** Ekrandaki vinyetle aynı elips; artı değer karartır, eksi değer aydınlatır. */
    private fun vignette(bmp: Bitmap, value: Int) {
        if (value == 0) return
        val w = bmp.width
        val h = bmp.height
        val row = IntArray(w)
        val halfW = w / 2f
        val halfH = h / 2f
        val darken = value > 0
        for (y in 0 until h) {
            val ny = (y + 0.5f - halfH) / halfH
            bmp.getPixels(row, 0, w, 0, y, w, 1)
            var changed = false
            for (x in 0 until w) {
                val nx = (x + 0.5f - halfW) / halfW
                val a = ColorMath.vignetteAlpha(nx, ny, value)
                if (a <= 0f) continue
                val p = row[x]
                var r = p shr 16 and 0xFF
                var g = p shr 8 and 0xFF
                var b = p and 0xFF
                if (darken) {
                    val f = 1f - a
                    r = (r * f).toInt()
                    g = (g * f).toInt()
                    b = (b * f).toInt()
                } else {
                    r = (r + (255 - r) * a).toInt()
                    g = (g + (255 - g) * a).toInt()
                    b = (b + (255 - b) * a).toInt()
                }
                row[x] = (p and -0x1000000) or (r shl 16) or (g shl 8) or b
                changed = true
            }
            if (changed) bmp.setPixels(row, 0, w, 0, y, w, 1)
        }
    }
}
