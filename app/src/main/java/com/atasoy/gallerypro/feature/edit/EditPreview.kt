package com.atasoy.gallerypro.feature.edit

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.atasoy.gallerypro.data.edit.AdjustTool
import com.atasoy.gallerypro.data.edit.ColorMath
import com.atasoy.gallerypro.data.edit.CropMath
import com.atasoy.gallerypro.data.edit.CropRect
import com.atasoy.gallerypro.data.edit.EditParams
import com.atasoy.gallerypro.designsystem.GalleryColors
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/** Renk matrisi önizlemede ColorFilter olarak GPU'da uygulanır (tüm Android sürümleri). */
internal fun colorFilterFor(params: EditParams): ColorFilter? =
    if (!params.hasColor) null else ColorFilter.colorMatrix(ColorMatrix(ColorMath.toAndroid(ColorMath.full(params))))

/**
 * Düzenleme önizlemesi. Kaydedicideki sırayla çizer: geometri → renk → keskinlik → vinyet.
 * Kırp sekmesinde tüm çerçeve görünür ve kırpma kutusu sürüklenir.
 */
@Composable
internal fun EditPreview(
    image: ImageBitmap,
    params: EditParams,
    showOriginal: Boolean,
    cropMode: Boolean,
    onCropChange: (CropRect) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val colorFilter = remember(params.filter, params.adjust, params.auto) { colorFilterFor(params) }
    val sharpness = params.value(AdjustTool.Sharpness)
    val sharpenEffect = remember(sharpness) {
        if (sharpness != 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) SharpenEffect.create(sharpness / 100f) else null
    }

    BoxWithConstraints(modifier) {
        val boxW = constraints.maxWidth.toFloat()
        val boxH = constraints.maxHeight.toFloat()
        val bw = image.width.toFloat()
        val bh = image.height.toFloat()
        val crop = params.crop
        val showFrame = cropMode || showOriginal
        val contentW = if (showFrame) bw else crop.width * bw
        val contentH = if (showFrame) bh else crop.height * bh
        if (boxW <= 0f || boxH <= 0f || contentW <= 0f || contentH <= 0f) return@BoxWithConstraints
        val k = min(boxW / contentW, boxH / contentH)
        val dw = contentW * k
        val dh = contentH * k
        val ox = ((boxW - dw) / 2f).roundToInt()
        val oy = ((boxH - dh) / 2f).roundToInt()
        val dwDp = with(density) { dw.toDp() }
        val dhDp = with(density) { dh.toDp() }
        val angle = params.angle.toFloat()
        val scale = CropMath.coverScale(angle, bw, bh)
        val applyEdits = !showOriginal

        Box(
            Modifier
                .offset { IntOffset(ox, oy) }
                .size(dwDp, dhDp)
                .clipToBounds()
                .then(
                    if (applyEdits && !cropMode && sharpenEffect != null) {
                        Modifier.graphicsLayer { renderEffect = sharpenEffect }
                    } else {
                        Modifier
                    }
                )
                .drawBehind {
                    withTransform({
                        scale(k, k, pivot = Offset.Zero)
                        if (!showFrame) translate(-crop.left * bw, -crop.top * bh)
                        if (applyEdits) {
                            rotate(angle, pivot = Offset(bw / 2f, bh / 2f))
                            scale(scale, scale, pivot = Offset(bw / 2f, bh / 2f))
                        }
                    }) {
                        drawImage(image, colorFilter = if (applyEdits) colorFilter else null)
                    }
                }
        )

        val vignette = params.value(AdjustTool.Vignette)
        if (applyEdits && !cropMode && vignette != 0) {
            Canvas(
                Modifier
                    .offset { IntOffset(ox, oy) }
                    .size(dwDp, dhDp)
            ) { drawVignette(vignette) }
        }

        if (applyEdits && cropMode) {
            CropOverlay(
                rect = crop,
                ratio = params.ratio.ratio,
                frameW = bw,
                frameH = bh,
                onChange = onCropChange,
                modifier = Modifier
                    .offset { IntOffset(ox, oy) }
                    .size(dwDp, dhDp)
            )
        }
    }
}

/** Kaydedicideki vinyetle aynı elips (köşeden geçen, %45'ten başlayan). */
private fun DrawScope.drawVignette(value: Int) {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return
    val color = if (value > 0) Color.Black else Color.White
    val alpha = abs(value) / 100f * ColorMath.VignetteMax
    val brush = Brush.radialGradient(
        ColorMath.VignetteStart to color.copy(alpha = 0f),
        1f to color.copy(alpha = alpha),
        center = Offset(w / 2f, h / 2f),
        radius = w / 2f * 1.4142135f
    )
    withTransform({ scale(1f, h / w, pivot = Offset(w / 2f, h / 2f)) }) {
        drawRect(brush, topLeft = Offset(0f, h / 2f - w / 2f), size = Size(w, w))
    }
}

/** Kırpma kutusu: dışı karartılır, üçte bir ızgarası, köşe tutamaçları; köşeden boyutlanır, içinden taşınır. */
@Composable
private fun CropOverlay(
    rect: CropRect,
    ratio: Float?,
    frameW: Float,
    frameH: Float,
    onChange: (CropRect) -> Unit,
    modifier: Modifier = Modifier
) {
    val current by rememberUpdatedState(rect)
    val currentRatio by rememberUpdatedState(ratio)
    val update by rememberUpdatedState(onChange)
    Canvas(
        modifier.pointerInput(frameW, frameH) {
            val handle = 36.dp.toPx()
            var mode = ModeNone
            detectDragGestures(
                onDragStart = { pos ->
                    val r = current
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    val corners = listOf(
                        Offset(r.left * w, r.top * h),
                        Offset(r.right * w, r.top * h),
                        Offset(r.right * w, r.bottom * h),
                        Offset(r.left * w, r.bottom * h)
                    )
                    val nearest = corners.indices.minBy { (corners[it] - pos).getDistance() }
                    val inside = pos.x in r.left * w..r.right * w && pos.y in r.top * h..r.bottom * h
                    mode = when {
                        (corners[nearest] - pos).getDistance() <= handle -> nearest
                        inside -> ModeMove
                        else -> ModeNone
                    }
                },
                onDragEnd = { mode = ModeNone },
                onDragCancel = { mode = ModeNone },
                onDrag = { change, amount ->
                    if (mode == ModeNone) return@detectDragGestures
                    change.consume()
                    val dx = amount.x / size.width
                    val dy = amount.y / size.height
                    val next = if (mode == ModeMove) {
                        CropMath.move(current, dx, dy)
                    } else {
                        CropMath.dragCorner(current, mode, dx, dy, currentRatio, frameW, frameH)
                    }
                    update(next)
                }
            )
        }
    ) {
        val w = size.width
        val h = size.height
        val l = rect.left * w
        val t = rect.top * h
        val r = rect.right * w
        val b = rect.bottom * h
        val dim = GalleryColors.EditDim
        drawRect(dim, Offset.Zero, Size(w, t))
        drawRect(dim, Offset(0f, b), Size(w, h - b))
        drawRect(dim, Offset(0f, t), Size(l, b - t))
        drawRect(dim, Offset(r, t), Size(w - r, b - t))

        val grid = Color.White.copy(alpha = 0.5f)
        val thin = 1.dp.toPx()
        for (i in 1..2) {
            val x = l + (r - l) * i / 3f
            val y = t + (b - t) * i / 3f
            drawLine(grid, Offset(x, t), Offset(x, b), strokeWidth = thin)
            drawLine(grid, Offset(l, y), Offset(r, y), strokeWidth = thin)
        }
        drawRect(Color.White, Offset(l, t), Size(r - l, b - t), style = Stroke(1.5.dp.toPx()))

        val arm = 22.dp.toPx()
        val thick = 4.dp.toPx()
        val o = 3.dp.toPx()
        fun corner(x: Float, y: Float, sx: Float, sy: Float) {
            // (x, y) köşe; sx/sy kolların yönü (+1 sağ/aşağı, −1 sol/yukarı).
            val cx = x - sx * o
            val cy = y - sy * o
            drawRect(Color.White, Offset(if (sx > 0) cx else cx - arm, if (sy > 0) cy else cy - thick), Size(arm, thick))
            drawRect(Color.White, Offset(if (sx > 0) cx else cx - thick, if (sy > 0) cy else cy - arm), Size(thick, arm))
        }
        corner(l, t, 1f, 1f)
        corner(r, t, -1f, 1f)
        corner(r, b, -1f, -1f)
        corner(l, b, 1f, -1f)
    }
}

private const val ModeNone = -1
private const val ModeMove = 4

/** Android 13+: keskin olmayan maske (3×3), kaydedicideki formülün aynısı. */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private object SharpenEffect {
    private const val Source = """
uniform shader image;
uniform float amount;

half4 main(float2 p) {
    half4 c = image.eval(p);
    half4 s = image.eval(p + float2(-1.0, -1.0)) + image.eval(p + float2(0.0, -1.0)) + image.eval(p + float2(1.0, -1.0))
        + image.eval(p + float2(-1.0, 0.0)) + c + image.eval(p + float2(1.0, 0.0))
        + image.eval(p + float2(-1.0, 1.0)) + image.eval(p + float2(0.0, 1.0)) + image.eval(p + float2(1.0, 1.0));
    half4 blur = s / 9.0;
    half3 rgb = clamp(c.rgb + (c.rgb - blur.rgb) * half(amount), half3(0.0), half3(c.a));
    return half4(rgb, c.a);
}"""

    fun create(amount: Float): RenderEffect {
        val shader = RuntimeShader(Source)
        shader.setFloatUniform("amount", amount)
        return android.graphics.RenderEffect.createRuntimeShaderEffect(shader, "image").asComposeRenderEffect()
    }
}
