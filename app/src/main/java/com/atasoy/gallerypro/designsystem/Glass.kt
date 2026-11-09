package com.atasoy.gallerypro.designsystem

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.isRenderEffectSupported
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.RoundedRectangularShape

/**
 * Cam malzemesinin iki tonu. Değerler dokümandaki "Cam malzemesi" tablosundan.
 *
 * Android 13+: bulanıklık + kırılma (lens) + kenar ışığı.
 * Android 12: bulanıklık + kenar ışığı (kırılma yok, kütüphane kendisi atlar).
 * Android 10–11: bulanıklık yok; daha opak düz dolgu kullanılır.
 */
enum class GlassTone(
    val fill: Color,
    val fallbackFill: Color,
    val blur: Dp,
    val shadowAlpha: Float,
    val content: Color
) {
    Light(
        fill = Color.White.copy(alpha = 0.55f),
        fallbackFill = Color.White.copy(alpha = 0.90f),
        blur = GalleryDimens.GlassBlurLight,
        shadowAlpha = 0.14f,
        content = GalleryColors.TextPrimary
    ),
    Dark(
        fill = Color.White.copy(alpha = 0.13f),
        fallbackFill = Color(0xE62A2A2E),
        blur = GalleryDimens.GlassBlurDark,
        shadowAlpha = 0.30f,
        content = Color.White
    ),

    /** Bilgi paneli ve onay kartı gibi büyük koyu yüzeyler: rgba(22,22,28,.5), bulanıklık 40. */
    Sheet(
        fill = Color(0x8016161C),
        fallbackFill = Color(0xF21C1C22),
        blur = GalleryDimens.GlassBlurSheet,
        shadowAlpha = 0.30f,
        content = Color.White
    )
}

/**
 * Uygulamadaki TEK cam çizim noktası. Ekranlar doğrudan blur kodu yazmaz.
 *
 * @param pressScale basılıyken ölçek (1 = normal). Lambda olduğu için sadece
 * katman yeniden çizilir, bileşen yeniden oluşturulmaz.
 */
fun Modifier.glass(
    backdrop: Backdrop,
    shape: RoundedRectangularShape,
    tone: GlassTone,
    refraction: Boolean = true,
    pressScale: () -> Float = { 1f }
): Modifier = this.drawBackdrop(
    backdrop = backdrop,
    shape = { shape },
    effects = {
        vibrancy()
        blur(tone.blur.toPx())
        if (refraction && !GalleryPerformance.reducedEffects) {
            lens(GalleryDimens.LensHeight.toPx(), GalleryDimens.LensAmount.toPx())
        }
    },
    highlight = { Highlight.Default },
    shadow = {
        Shadow(
            radius = GalleryDimens.ShadowRadius,
            color = Color.Black.copy(alpha = tone.shadowAlpha)
        )
    },
    layerBlock = {
        val scale = pressScale()
        scaleX = scale
        scaleY = scale
    },
    onDrawSurface = {
        drawRect(if (isRenderEffectSupported()) tone.fill else tone.fallbackFill)
    }
)
