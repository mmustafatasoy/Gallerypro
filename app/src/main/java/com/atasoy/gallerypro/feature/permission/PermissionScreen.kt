package com.atasoy.gallerypro.feature.permission

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.atasoy.gallerypro.R
import com.atasoy.gallerypro.designsystem.GalleryColors
import com.atasoy.gallerypro.designsystem.GalleryType
import com.atasoy.gallerypro.designsystem.GlassTone
import com.atasoy.gallerypro.designsystem.Glyph
import com.atasoy.gallerypro.designsystem.glass
import com.atasoy.gallerypro.designsystem.icons.GalleryIcons
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.RoundedRectangle
import kotlin.math.cos
import kotlin.math.sin

/**
 * İzin istenen ilk ekran. Arkada yavaşça dolaşan renk lekeleri, önde cam kart:
 * izin verilmeden önce bile uygulamanın cam hissi görünür.
 */
@Composable
fun PermissionScreen(
    showSettings: Boolean,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val backdrop = rememberLayerBackdrop()
    val transition = rememberInfiniteTransition(label = "blobs")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Restart),
        label = "blobPhase"
    )

    Box(Modifier.fillMaxSize()) {
        Canvas(
            Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .background(Color(0xFFFFF7F0))
        ) {
            val w = size.width
            val h = size.height
            val blobs = listOf(
                Triple(Color(0xFFFF9A8B), Offset(0.25f, 0.28f), 0.55f),
                Triple(Color(0xFF7F5AF0), Offset(0.78f, 0.36f), 0.50f),
                Triple(Color(0xFF2193B0), Offset(0.35f, 0.72f), 0.55f),
                Triple(Color(0xFFFDA085), Offset(0.80f, 0.80f), 0.45f)
            )
            blobs.forEachIndexed { i, (color, base, r) ->
                val phase = t + i * 1.7f
                val center = Offset(
                    x = w * (base.x + 0.08f * cos(phase)),
                    y = h * (base.y + 0.06f * sin(phase * 1.3f))
                )
                val radius = w * r
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = 0.85f), color.copy(alpha = 0f)),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .glass(backdrop, RoundedRectangle(32.dp), GlassTone.Light, refraction = true)
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Glyph(GalleryIcons.Photos, GalleryColors.Accent, size = 34.dp)
            }
            BasicText(
                text = stringResource(R.string.permission_title),
                style = GalleryType.SectionTitle.copy(textAlign = TextAlign.Center)
            )
            BasicText(
                text = stringResource(
                    if (showSettings) R.string.permission_denied_hint else R.string.permission_body
                ),
                style = GalleryType.Body.copy(color = GalleryColors.TextSecondary, textAlign = TextAlign.Center)
            )
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(CircleShape)
                    .background(GalleryColors.Accent)
                    .clickable(role = Role.Button, onClick = if (showSettings) onOpenSettings else onRequest),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    text = stringResource(if (showSettings) R.string.permission_settings else R.string.permission_allow),
                    style = GalleryType.Button.copy(color = Color.White, fontSize = GalleryType.Body.fontSize)
                )
            }
        }
    }
}
