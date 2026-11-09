package com.atasoy.gallerypro.data.edit

import androidx.compose.runtime.Immutable
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** Ayarla sekmesindeki araçlar, tasarımdaki sırayla. Değerler −100…+100. */
enum class AdjustTool { Auto, Exposure, Brightness, Contrast, Saturation, Warmth, Vignette, Sharpness }

/** Filtreler sekmesi, tasarımdaki sırayla. */
enum class EditFilter { Original, Vivid, Dramatic, Warm, Cool, Mono, Silver, Nostalgia }

/** Kırp sekmesindeki oranlar. null = serbest. */
enum class CropRatio(val ratio: Float?) {
    Free(null),
    Square(1f),
    FourThree(4f / 3f),
    SixteenNine(16f / 9f),
    NineSixteen(9f / 16f)
}

/** Kırpma dikdörtgeni, düzeltilmiş çerçeveye göre 0…1 aralığında. */
@Immutable
data class CropRect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val isFull: Boolean get() = left <= Eps && top <= Eps && right >= 1f - Eps && bottom >= 1f - Eps

    private companion object {
        const val Eps = 0.0005f
    }
}

/**
 * "Otomatik" aracının fotoğrafa göre hesaplanan düzeltmesi (tam güçte).
 * Fotoğraftan bir kez ölçülür ve ayarlarla birlikte saklanır ki kaydederken aynı sonuç çıksın.
 */
@Immutable
data class AutoCorrection(val gain: Float = 1f, val contrast: Float = 1f, val saturation: Float = 1f)

/**
 * Bir fotoğrafın tüm düzenleme ayarları. Fotoğraf dosyası değil, bu ayarlar saklanır;
 * her açılışta orijinalden yeniden uygulanır (yıkıcı olmayan düzenleme).
 */
@Immutable
data class EditParams(
    /** [AdjustTool] sırasıyla 8 değer. */
    val adjust: List<Int> = List(AdjustTool.entries.size) { 0 },
    val auto: AutoCorrection = AutoCorrection(),
    val filter: EditFilter = EditFilter.Original,
    /** Düzeltme açısı, derece (−45…+45). */
    val angle: Int = 0,
    val ratio: CropRatio = CropRatio.Free,
    val crop: CropRect = CropRect()
) {
    fun value(tool: AdjustTool): Int = adjust.getOrElse(tool.ordinal) { 0 }

    fun with(tool: AdjustTool, value: Int): EditParams =
        copy(adjust = adjust.toMutableList().also { it[tool.ordinal] = value.coerceIn(-100, 100) })

    val hasColor: Boolean get() = filter != EditFilter.Original || AdjustTool.entries.any {
        it != AdjustTool.Vignette && it != AdjustTool.Sharpness && value(it) != 0
    }
    val hasGeometry: Boolean get() = angle != 0 || !crop.isFull
    val isIdentity: Boolean get() = filter == EditFilter.Original && angle == 0 && crop.isFull && adjust.all { it == 0 }

    /** Ayarları saklamak için kısa metin. */
    fun encode(): String = buildString {
        append("v1")
        append(";a=").append(adjust.joinToString(","))
        append(";o=").append(f(auto.gain)).append(',').append(f(auto.contrast)).append(',').append(f(auto.saturation))
        append(";f=").append(filter.name)
        append(";g=").append(angle)
        append(";r=").append(ratio.name)
        append(";c=").append(f(crop.left)).append(',').append(f(crop.top)).append(',')
            .append(f(crop.right)).append(',').append(f(crop.bottom))
    }

    companion object {
        private fun f(v: Float): String = String.format(Locale.ROOT, "%.5f", v)

        /** Bozuk veya eski biçimde null döner. */
        fun decode(text: String?): EditParams? {
            if (text.isNullOrBlank() || !text.startsWith("v1")) return null
            return runCatching {
                val parts = text.split(';').drop(1).associate { part ->
                    val i = part.indexOf('=')
                    part.substring(0, i) to part.substring(i + 1)
                }
                val adjust = parts.getValue("a").split(',').map { it.toInt().coerceIn(-100, 100) }
                require(adjust.size == AdjustTool.entries.size)
                val o = parts.getValue("o").split(',').map { it.toFloat() }
                val c = parts.getValue("c").split(',').map { it.toFloat() }
                EditParams(
                    adjust = adjust,
                    auto = AutoCorrection(o[0], o[1], o[2]),
                    filter = EditFilter.valueOf(parts.getValue("f")),
                    angle = parts.getValue("g").toInt().coerceIn(-45, 45),
                    ratio = CropRatio.valueOf(parts.getValue("r")),
                    crop = CropRect(c[0], c[1], c[2], c[3])
                )
            }.getOrNull()
        }
    }
}

/**
 * 4×5 renk matrisleri (satır düzeni, ofsetler 0…1). Formüller CSS Filter Effects
 * belirtimindekiyle aynıdır; tasarım tuvalindeki önizlemeyle birebir eşleşir.
 * Saf Kotlin; birim testleri JVM'de çalışır.
 */
object ColorMath {

    fun identity(): FloatArray = floatArrayOf(
        1f, 0f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, 1f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )

    /** Önce [first], sonra [then] uygulanan tek matris. */
    fun concat(first: FloatArray, then: FloatArray): FloatArray {
        val out = FloatArray(20)
        for (row in 0 until 4) {
            for (col in 0 until 5) {
                var v = 0f
                for (k in 0 until 4) v += then[row * 5 + k] * first[k * 5 + col]
                if (col == 4) v += then[row * 5 + 4]
                out[row * 5 + col] = v
            }
        }
        return out
    }

    private fun rgb(m: FloatArray, offset: Float = 0f): FloatArray = floatArrayOf(
        m[0], m[1], m[2], 0f, offset,
        m[3], m[4], m[5], 0f, offset,
        m[6], m[7], m[8], 0f, offset,
        0f, 0f, 0f, 1f, 0f
    )

    fun brightness(b: Float): FloatArray = rgb(floatArrayOf(b, 0f, 0f, 0f, b, 0f, 0f, 0f, b))

    fun contrast(c: Float): FloatArray = rgb(floatArrayOf(c, 0f, 0f, 0f, c, 0f, 0f, 0f, c), offset = 0.5f - 0.5f * c)

    fun saturate(s: Float): FloatArray = rgb(
        floatArrayOf(
            0.213f + 0.787f * s, 0.715f - 0.715f * s, 0.072f - 0.072f * s,
            0.213f - 0.213f * s, 0.715f + 0.285f * s, 0.072f - 0.072f * s,
            0.213f - 0.213f * s, 0.715f - 0.715f * s, 0.072f + 0.928f * s
        )
    )

    fun grayscale(amount: Float): FloatArray {
        val a = 1f - amount.coerceIn(0f, 1f)
        return rgb(
            floatArrayOf(
                0.2126f + 0.7874f * a, 0.7152f - 0.7152f * a, 0.0722f - 0.0722f * a,
                0.2126f - 0.2126f * a, 0.7152f + 0.2848f * a, 0.0722f - 0.0722f * a,
                0.2126f - 0.2126f * a, 0.7152f - 0.7152f * a, 0.0722f + 0.9278f * a
            )
        )
    }

    fun sepia(amount: Float): FloatArray {
        val a = 1f - amount.coerceIn(0f, 1f)
        return rgb(
            floatArrayOf(
                0.393f + 0.607f * a, 0.769f - 0.769f * a, 0.189f - 0.189f * a,
                0.349f - 0.349f * a, 0.686f + 0.314f * a, 0.168f - 0.168f * a,
                0.272f - 0.272f * a, 0.534f - 0.534f * a, 0.131f + 0.869f * a
            )
        )
    }

    fun hueRotate(degrees: Float): FloatArray {
        val r = Math.toRadians(degrees.toDouble())
        val c = cos(r).toFloat()
        val s = sin(r).toFloat()
        return rgb(
            floatArrayOf(
                0.213f + c * 0.787f - s * 0.213f, 0.715f - c * 0.715f - s * 0.715f, 0.072f - c * 0.072f + s * 0.928f,
                0.213f - c * 0.213f + s * 0.143f, 0.715f + c * 0.285f + s * 0.140f, 0.072f - c * 0.072f - s * 0.283f,
                0.213f - c * 0.213f - s * 0.787f, 0.715f - c * 0.715f + s * 0.715f, 0.072f + c * 0.928f + s * 0.072f
            )
        )
    }

    private fun chain(vararg steps: FloatArray): FloatArray =
        steps.fold(identity()) { acc, step -> concat(acc, step) }

    /** Tasarımdaki filtreler (CSS sırasıyla). */
    fun filter(filter: EditFilter): FloatArray = when (filter) {
        EditFilter.Original -> identity()
        EditFilter.Vivid -> chain(saturate(1.45f), contrast(1.08f))
        EditFilter.Dramatic -> chain(contrast(1.35f), saturate(0.8f), brightness(0.9f))
        EditFilter.Warm -> chain(sepia(0.28f), saturate(1.35f), brightness(1.03f))
        EditFilter.Cool -> chain(hueRotate(-14f), saturate(1.15f), brightness(1.04f))
        EditFilter.Mono -> chain(grayscale(1f), contrast(1.12f))
        EditFilter.Silver -> chain(grayscale(1f), brightness(1.1f), contrast(0.88f))
        EditFilter.Nostalgia -> chain(sepia(0.55f), contrast(0.9f), brightness(1.06f), saturate(0.9f))
    }

    /**
     * Ayarla sekmesinin renk kısmı: parlaklık → kontrast → doygunluk → sıcaklık.
     * Vinyet ve keskinlik renk matrisi değildir; ayrı uygulanır.
     */
    fun adjust(params: EditParams): FloatArray {
        val a = params.value(AdjustTool.Auto) / 100f
        val auto = params.auto
        val autoGain = 1f + (auto.gain - 1f) * a
        val autoContrast = (auto.contrast - 1f) * a
        val autoSaturation = (auto.saturation - 1f) * a
        val b = (1f + params.value(AdjustTool.Exposure) / 220f) *
            (1f + params.value(AdjustTool.Brightness) / 320f) * autoGain
        val c = 1f + params.value(AdjustTool.Contrast) / 160f + autoContrast
        val s = max(0f, 1f + params.value(AdjustTool.Saturation) / 100f + autoSaturation)
        val warm = params.value(AdjustTool.Warmth)
        val steps = mutableListOf(brightness(max(0f, b)), contrast(max(0f, c)), saturate(s))
        if (warm > 0) steps += sepia(warm / 260f)
        if (warm < 0) steps += hueRotate(warm * 0.18f)
        return chain(*steps.toTypedArray())
    }

    /** Önce filtre, sonra ayarlar (tasarımdaki sırayla). */
    fun full(params: EditParams): FloatArray = concat(filter(params.filter), adjust(params))

    /** Android ColorMatrix biçimi: ofsetler 0…255. */
    fun toAndroid(m: FloatArray): FloatArray = m.copyOf().also {
        it[4] *= 255f
        it[9] *= 255f
        it[14] *= 255f
        it[19] *= 255f
    }

    /** Matrisi tek bir 0…1 renge uygular (testler ve otomatik ayar için). */
    fun apply(m: FloatArray, r: Float, g: Float, b: Float): FloatArray = floatArrayOf(
        (m[0] * r + m[1] * g + m[2] * b + m[4]).coerceIn(0f, 1f),
        (m[5] * r + m[6] * g + m[7] * b + m[9]).coerceIn(0f, 1f),
        (m[10] * r + m[11] * g + m[12] * b + m[14]).coerceIn(0f, 1f)
    )

    /**
     * Vinyetin bir noktadaki gücü (0…0,85). [nx], [ny] merkeze göre −1…+1.
     * Tasarım: elips, %45'te şeffaf, köşede %85 karanlık.
     */
    fun vignetteAlpha(nx: Float, ny: Float, value: Int): Float {
        if (value == 0) return 0f
        val d = sqrt(nx * nx + ny * ny) / Sqrt2
        val t = ((d - VignetteStart) / (1f - VignetteStart)).coerceIn(0f, 1f)
        return abs(value) / 100f * VignetteMax * t
    }

    const val VignetteStart = 0.45f
    const val VignetteMax = 0.85f
    private const val Sqrt2 = 1.4142135f

    /**
     * Otomatik düzeltme: ortalama parlaklığı ~0,48'e, parlaklık sapmasını ~0,21'e çeker,
     * soluk fotoğraflarda doygunluğu biraz artırır. [meanLuma], [stdLuma], [meanSaturation] 0…1.
     */
    fun autoCorrection(meanLuma: Float, stdLuma: Float, meanSaturation: Float): AutoCorrection {
        val gain = if (meanLuma <= 0.001f) 1.6f else (0.48f / meanLuma).coerceIn(0.75f, 1.6f)
        val contrast = if (stdLuma <= 0.001f) 1.3f else (0.21f / stdLuma).coerceIn(0.9f, 1.3f)
        val saturation = when {
            meanSaturation < 0.25f -> 1.25f
            meanSaturation < 0.4f -> 1.12f
            else -> 1f
        }
        return AutoCorrection(gain, contrast, saturation)
    }
}

/**
 * Kırpma ve düzeltme geometrisi. Döndürülen fotoğraf, çerçevede boşluk kalmayacak
 * kadar büyütülür; kırpma dikdörtgeni bu çerçeveye göredir.
 */
object CropMath {

    /** Kırpma dikdörtgeninin en küçük kenarı (çerçeveye oranla). */
    const val MinSize = 0.1f

    /** [angleDeg] döndürmede [w]×[h] çerçeveyi tamamen kaplamak için gereken büyütme. */
    fun coverScale(angleDeg: Float, w: Float, h: Float): Float {
        if (w <= 0f || h <= 0f) return 1f
        val r = Math.toRadians(angleDeg.toDouble())
        val c = abs(cos(r)).toFloat()
        val s = abs(sin(r)).toFloat()
        return c + s * max(w / h, h / w)
    }

    /** Çerçeveye sığan, ortalanmış en büyük [ratio] oranlı dikdörtgen. */
    fun ratioRect(ratio: Float?, frameW: Float, frameH: Float): CropRect {
        if (ratio == null || frameW <= 0f || frameH <= 0f) return CropRect()
        val frameRatio = frameW / frameH
        return if (frameRatio > ratio) {
            val w = ratio / frameRatio
            CropRect((1f - w) / 2f, 0f, (1f + w) / 2f, 1f)
        } else {
            val h = frameRatio / ratio
            CropRect(0f, (1f - h) / 2f, 1f, (1f + h) / 2f)
        }
    }

    /** Dikdörtgeni taşı; çerçeveden çıkmaz. */
    fun move(rect: CropRect, dx: Float, dy: Float): CropRect {
        val x = dx.coerceIn(-rect.left, 1f - rect.right)
        val y = dy.coerceIn(-rect.top, 1f - rect.bottom)
        return CropRect(rect.left + x, rect.top + y, rect.right + x, rect.bottom + y)
    }

    /**
     * Köşeden sürükleme. [corner]: 0 sol üst, 1 sağ üst, 2 sağ alt, 3 sol alt.
     * Oran sabitse ([ratio], piksel cinsinden) karşı köşe yerinde kalır ve oran korunur.
     */
    fun dragCorner(
        rect: CropRect,
        corner: Int,
        dx: Float,
        dy: Float,
        ratio: Float?,
        frameW: Float,
        frameH: Float
    ): CropRect {
        val movesLeft = corner == 0 || corner == 3
        val movesTop = corner == 0 || corner == 1
        var left = rect.left
        var top = rect.top
        var right = rect.right
        var bottom = rect.bottom
        if (movesLeft) left = (left + dx).coerceIn(0f, right - MinSize) else right = (right + dx).coerceIn(left + MinSize, 1f)
        if (movesTop) top = (top + dy).coerceIn(0f, bottom - MinSize) else bottom = (bottom + dy).coerceIn(top + MinSize, 1f)
        if (ratio == null || frameW <= 0f || frameH <= 0f) return CropRect(left, top, right, bottom)

        // Oranı koru: normalleştirilmiş genişlik/yükseklik oranı = ratio * frameH / frameW.
        val k = ratio * frameH / frameW
        var w = right - left
        var h = bottom - top
        if (w / h > k) w = h * k else h = w / k
        // Karşı köşeye sabitle ve çerçeveye sığdır.
        val anchorX = if (movesLeft) rect.right else rect.left
        val anchorY = if (movesTop) rect.bottom else rect.top
        val maxW = if (movesLeft) anchorX else 1f - anchorX
        val maxH = if (movesTop) anchorY else 1f - anchorY
        if (w > maxW) {
            w = maxW
            h = w / k
        }
        if (h > maxH) {
            h = maxH
            w = h * k
        }
        val minSide = MinSize
        if (w < minSide && h < minSide) {
            // Çok küçüldüyse en küçük boyutta tut.
            if (k >= 1f) {
                h = minSide
                w = h * k
            } else {
                w = minSide
                h = w / k
            }
        }
        val l = if (movesLeft) anchorX - w else anchorX
        val t = if (movesTop) anchorY - h else anchorY
        return CropRect(l, t, l + w, t + h)
    }

    /** Kırpılmış çıktının piksel boyutu. */
    fun outputSize(crop: CropRect, sourceW: Int, sourceH: Int): Pair<Int, Int> {
        val w = (crop.width * sourceW).toInt().coerceAtLeast(1)
        val h = (crop.height * sourceH).toInt().coerceAtLeast(1)
        return w to h
    }

    /** Çerçeve oranı değişince (fotoğraf farklı boyutta açıldı) dikdörtgeni sınırlar. */
    fun clamp(rect: CropRect): CropRect {
        val l = rect.left.coerceIn(0f, 1f - MinSize)
        val t = rect.top.coerceIn(0f, 1f - MinSize)
        val r = rect.right.coerceIn(l + MinSize, 1f)
        val b = rect.bottom.coerceIn(t + MinSize, 1f)
        return CropRect(min(l, r), min(t, b), r, b)
    }
}
