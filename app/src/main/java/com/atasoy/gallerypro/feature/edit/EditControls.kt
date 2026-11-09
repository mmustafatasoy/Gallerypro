package com.atasoy.gallerypro.feature.edit

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.atasoy.gallerypro.data.edit.ColorMath
import com.atasoy.gallerypro.data.edit.EditFilter
import com.atasoy.gallerypro.designsystem.GalleryColors
import com.atasoy.gallerypro.designsystem.GalleryDimens
import com.atasoy.gallerypro.designsystem.GalleryMotion
import com.atasoy.gallerypro.designsystem.GalleryType
import com.atasoy.gallerypro.designsystem.Glyph
import com.atasoy.gallerypro.designsystem.icons.GalleryIcons
import kotlin.math.abs
import kotlin.math.roundToInt

/** "+18", "0", "−12" */
internal fun signed(value: Int): String = when {
    value > 0 -> "+$value"
    value < 0 -> "−${-value}"
    else -> "0"
}

/** Aracın adı ve değeri: "POZLAMA  +18". */
@Composable
internal fun ToolLabel(name: String, value: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        BasicText(
            name,
            style = LabelStyle.copy(color = Color.White.copy(alpha = 0.75f))
        )
        if (value != null) {
            Spacer(Modifier.width(8.dp))
            BasicText(
                value,
                style = LabelStyle.copy(color = GalleryColors.EditAccent, fontFeatureSettings = "tnum")
            )
        }
    }
}

private val LabelStyle = GalleryType.Duration.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.08.em)

/**
 * Cetvel kaydırıcı. Parmak kaydıkça değer değişir (dokunmak tek başına değiştirmez),
 * her 10 adımda ve sıfırda titreşir, çift dokunmak sıfırlar.
 */
@Composable
internal fun RulerSlider(
    value: Int,
    range: IntRange,
    label: String,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val current by rememberUpdatedState(value)
    val change by rememberUpdatedState(onValueChange)
    var accumulated by remember { mutableFloatStateOf(0f) }
    val span = (range.last - range.first).toFloat()

    fun emit(next: Int) {
        val clamped = next.coerceIn(range.first, range.last)
        if (clamped != current) {
            if (clamped == 0 || clamped % 10 == 0) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            change(clamped)
        }
    }

    Canvas(
        modifier
            .fillMaxWidth()
            .height(GalleryDimens.EditRulerHeight)
            .semantics {
                contentDescription = label
                stateDescription = signed(value)
                progressBarRangeInfo = ProgressBarRangeInfo(value.toFloat(), range.first.toFloat()..range.last.toFloat(), steps = 0)
                setProgress { target ->
                    change(target.roundToInt().coerceIn(range.first, range.last))
                    true
                }
            }
            .pointerInput(range) {
                detectTapGestures(onDoubleTap = { emit(0) })
            }
            .pointerInput(range) {
                detectHorizontalDragGestures(
                    onDragStart = { accumulated = 0f },
                    onHorizontalDrag = { pointer, dx ->
                        pointer.consume()
                        // Cetvelin tamamı tüm aralığa denk gelir.
                        accumulated += dx / size.width * span
                        val steps = accumulated.toInt()
                        if (steps != 0) {
                            accumulated -= steps
                            emit(current + steps)
                        }
                    }
                )
            }
    ) {
        val w = size.width
        val tick = GalleryDimens.EditTickSpacing.toPx()
        val tickTop = 13.dp.toPx()
        val tickHeight = 14.dp.toPx()
        val fade = w * 0.15f
        var x = 0f
        while (x <= w) {
            val edge = minOf(x, w - x)
            val alpha = 0.5f * (edge / fade).coerceIn(0f, 1f)
            drawRect(Color.White.copy(alpha = alpha), Offset(x, tickTop), Size(1f, tickHeight))
            x += tick
        }
        val center = w / 2f
        drawRect(
            Color.White.copy(alpha = 0.9f),
            Offset(center - 1.dp.toPx(), 8.dp.toPx()),
            Size(2.dp.toPx(), 24.dp.toPx())
        )
        val thumbW = 5.dp.toPx()
        val thumbX = (value - range.first) / span * w
        drawRoundRect(
            GalleryColors.EditAccent,
            Offset((thumbX - thumbW / 2f).coerceIn(0f, w - thumbW), 0f),
            Size(thumbW, size.height),
            cornerRadius = CornerRadius(3.dp.toPx())
        )
    }
}

/** Değerini halka olarak gösteren araç dairesi. */
@Composable
internal fun ToolCircle(
    icon: ImageVector,
    value: Int,
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    val accent = GalleryColors.EditAccent
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) GalleryMotion.PressedScale else 1f, GalleryMotion.press(), label = "toolPress")
    Box(
        Modifier
            .size(GalleryDimens.EditToolCircle)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = label
                stateDescription = signed(value)
                this.selected = selected
            }
            .drawBehind {
                drawCircle(Color.White.copy(alpha = if (selected) 0.35f else 0.14f))
                val pct = abs(value) / 100f
                if (pct > 0f) {
                    drawArc(
                        color = if (selected) accent else Color.White.copy(alpha = 0.8f),
                        startAngle = -90f,
                        sweepAngle = 360f * pct,
                        useCenter = true
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(GalleryDimens.EditToolInner)
                .background(GalleryColors.EditToolInner, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Glyph(icon, if (selected) accent else Color.White, size = 24.dp)
        }
    }
}

/** Filtre küçük resmi; seçiliyse vurgulu çerçeve. */
@Composable
internal fun FilterChip(
    thumb: ImageBitmap?,
    filter: EditFilter,
    name: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val accent = GalleryColors.EditAccent
    val shape = RoundedCornerShape(12.dp)
    val matrix = remember(filter) {
        if (filter == EditFilter.Original) null else ColorFilter.colorMatrix(ColorMatrix(ColorMath.toAndroid(ColorMath.filter(filter))))
    }
    Column(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.selected = selected },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(GalleryDimens.EditFilterWidth, GalleryDimens.EditFilterHeight)
                .border(if (selected) 2.5.dp else 0.5.dp, if (selected) accent else Color.White.copy(alpha = 0.3f), shape)
                .padding(if (selected) 2.5.dp else 0.5.dp)
                .clip(shape)
                .background(Color.White.copy(alpha = 0.08f))
        ) {
            if (thumb != null) {
                Image(
                    bitmap = thumb,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    colorFilter = matrix,
                    modifier = Modifier.matchParentSizeCompat()
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        BasicText(
            name,
            style = GalleryType.TabLabel.copy(color = if (selected) accent else Color.White.copy(alpha = 0.8f))
        )
    }
}

private fun Modifier.matchParentSizeCompat(): Modifier = this.then(Modifier.fillMaxWidth().fillMaxHeight())

/** Kırp oranı çipi. */
@Composable
internal fun RatioChip(name: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .height(34.dp)
            .clip(CircleShape)
            .background(if (selected) Color.White else Color.White.copy(alpha = 0.14f))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            name,
            style = GalleryType.Chip.copy(color = if (selected) GalleryColors.TextPrimary else Color.White)
        )
    }
}

/** Üstteki "SIFIRLA · ORİJİNAL" hapının bir düğmesi. */
@Composable
internal fun PillButton(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .height(32.dp)
            .clip(CircleShape)
            .background(if (selected) Color.White else Color.Transparent)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text,
            style = GalleryType.Duration.copy(
                color = if (selected) GalleryColors.TextPrimary else Color.White,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.04.em
            )
        )
    }
}

/** Sarı "Bitti" dairesi (cam değil, düz vurgu rengi). */
@Composable
internal fun DoneButton(description: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) GalleryMotion.PressedScale else 1f, GalleryMotion.press(), label = "donePress")
    Box(
        Modifier
            .size(GalleryDimens.ViewerTopButton)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(12.dp, CircleShape, ambientColor = Color(0x59000000), spotColor = Color(0x59000000))
            .background(GalleryColors.EditAccent, CircleShape)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Glyph(GalleryIcons.Check, GalleryColors.EditDoneIcon, size = 22.dp)
    }
}

/** Alttaki sekme düğmesi (Ayarla / Filtreler / Kırp). */
@Composable
internal fun EditTab(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) GalleryColors.TextPrimary else Color.White
    Column(
        Modifier
            .width(GalleryDimens.EditTabWidth)
            .fillMaxHeight()
            .clip(CircleShape)
            .background(if (selected) Color.White else Color.Transparent)
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Glyph(icon, color, size = 22.dp)
        Spacer(Modifier.height(2.dp))
        BasicText(label, style = GalleryType.TabLabel.copy(color = color))
    }
}

/** Kaydederken dönen yay. */
@Composable
internal fun Spinner(modifier: Modifier = Modifier) {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "spinner")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            androidx.compose.animation.core.tween(900, easing = androidx.compose.animation.core.LinearEasing)
        ),
        label = "spinnerAngle"
    )
    Canvas(modifier.size(36.dp)) {
        val stroke = 3.5.dp.toPx()
        drawArc(
            color = Color.White.copy(alpha = 0.25f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(stroke / 2f, stroke / 2f),
            size = Size(size.width - stroke, size.height - stroke),
            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke)
        )
        drawArc(
            color = Color.White,
            startAngle = angle - 90f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(stroke / 2f, stroke / 2f),
            size = Size(size.width - stroke, size.height - stroke),
            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )
    }
}
