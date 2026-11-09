package com.atasoy.gallerypro.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.runtimeShaderEffect
import com.kyant.backdrop.isRuntimeShaderSupported

private const val ProgressiveMaskShader = """
uniform shader content;
uniform float2 size;
layout(color) uniform half4 tint;
uniform float tintIntensity;
uniform float fromBottom;

half4 main(float2 coord) {
    float y = fromBottom > 0.5 ? size.y - coord.y : coord.y;
    float a = smoothstep(size.y, size.y * 0.45, y);
    return mix(content.eval(coord) * a, tint * a, tintIntensity);
}"""

/**
 * Üstten (veya alttan) azalarak kaybolan bulanıklık + renk geçişi.
 * Android 13+ gerçek kademeli bulanıklık; altında sadece renk geçişi.
 */
@Composable
fun ProgressiveBlurEdge(
    backdrop: Backdrop,
    height: Dp,
    tint: Color,
    modifier: Modifier = Modifier,
    fromBottom: Boolean = false,
    blurRadius: Dp = GalleryDimens.GlassBlurLight,
    tintIntensity: Float = 0.72f
) {
    if (isRuntimeShaderSupported() && !GalleryPerformance.reducedEffects) {
        Box(
            modifier
                .fillMaxWidth()
                .height(height)
                .drawPlainBackdrop(
                    backdrop = backdrop,
                    shape = { RectangleShape },
                    effects = {
                        blur(blurRadius.toPx())
                        runtimeShaderEffect(
                            key = "GalleryProgressiveMask",
                            shaderString = ProgressiveMaskShader,
                            uniformShaderName = "content"
                        ) {
                            setFloatUniform("size", size.width, size.height)
                            setColorUniform("tint", tint)
                            setFloatUniform("tintIntensity", tintIntensity)
                            setFloatUniform("fromBottom", if (fromBottom) 1f else 0f)
                        }
                    }
                )
        )
    } else {
        val colors = listOf(tint.copy(alpha = 0.94f), tint.copy(alpha = 0.7f), tint.copy(alpha = 0f))
        Box(
            modifier
                .fillMaxWidth()
                .height(height)
                .background(Brush.verticalGradient(if (fromBottom) colors.reversed() else colors))
        )
    }
}
