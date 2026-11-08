package com.atasoy.gallerypro.designsystem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.atasoy.gallerypro.designsystem.icons.GalleryIcons
import com.kyant.backdrop.Backdrop
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle
import com.kyant.shapes.RoundedRectangularShape

/** Renklendirilebilir ikon. */
@Composable
fun Glyph(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    Image(
        imageVector = icon,
        contentDescription = null,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier.size(size)
    )
}

/** Basınca hafifçe küçülen cam yüzey; tüm cam butonların ortak gövdesi. */
@Composable
private fun PressableGlass(
    backdrop: Backdrop,
    tone: GlassTone,
    shape: RoundedRectangularShape,
    onClick: () -> Unit,
    modifier: Modifier,
    description: String?,
    content: @Composable BoxScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) GalleryMotion.PressedScale else 1f,
        animationSpec = GalleryMotion.press(),
        label = "glassPress"
    )
    val haptics = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .glass(backdrop, shape, tone, pressScale = { scale })
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            )
            .then(
                if (description != null) {
                    Modifier.semantics { contentDescription = description }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center,
        content = content
    )
}

/** Yuvarlak cam ikon butonu (geri, ⋯, ara, paylaş, sil). */
@Composable
fun GlassIconButton(
    backdrop: Backdrop,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: GlassTone = GlassTone.Light,
    size: Dp = GalleryDimens.TopButton,
    iconSize: Dp = 20.dp,
    tint: Color = tone.content
) {
    PressableGlass(
        backdrop = backdrop,
        tone = tone,
        shape = Capsule(),
        onClick = onClick,
        modifier = modifier.size(size),
        description = contentDescription
    ) {
        Glyph(icon, tint, size = iconSize)
    }
}

/** Yazılı cam hap butonu ("Seç", "Vazgeç"). */
@Composable
fun GlassTextButton(
    backdrop: Backdrop,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: GlassTone = GlassTone.Light,
    textColor: Color = GalleryColors.Accent,
    height: Dp = GalleryDimens.TopButton
) {
    PressableGlass(
        backdrop = backdrop,
        tone = tone,
        shape = Capsule(),
        onClick = onClick,
        modifier = modifier.height(height),
        description = null
    ) {
        BasicText(
            text = text,
            style = GalleryType.Button.copy(color = textColor),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

/** Cam kapsül içinde içerik (ör. "3 öğe seçildi"). Tıklanmaz. */
@Composable
fun GlassCapsule(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    tone: GlassTone = GlassTone.Light,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier.glass(backdrop, Capsule(), tone),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        content = content
    )
}

/** "Yıllar · Aylar · Tümü" seçicisi; beyaz kabarcık seçilene kayar. */
@Composable
fun GlassSegmented(
    backdrop: Backdrop,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    tone: GlassTone = GlassTone.Light
) {
    val haptics = LocalHapticFeedback.current
    val segment = GalleryDimens.SegmentWidth
    val inner = 4.dp
    val indicatorX by animateDpAsState(
        targetValue = (segment + 2.dp) * selectedIndex,
        animationSpec = GalleryMotion.standard(),
        label = "segmentIndicator"
    )
    Box(
        modifier = modifier
            .height(GalleryDimens.SegmentedHeight)
            .glass(backdrop, Capsule(), tone, refraction = false)
            .padding(inner)
    ) {
        Box(
            Modifier
                .offset(x = indicatorX)
                .size(segment, GalleryDimens.SegmentHeight)
                .shadow(6.dp, CircleShape, clip = false, ambientColor = Color(0x22000000), spotColor = Color(0x22000000))
                .background(Color.White, CircleShape)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            options.forEachIndexed { index, label ->
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .size(segment, GalleryDimens.SegmentHeight)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = null,
                            indication = null,
                            role = Role.Tab
                        ) {
                            if (!isSelected) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelect(index)
                            }
                        }
                        .semantics { selected = isSelected },
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        text = label,
                        style = GalleryType.Segment.copy(
                            color = if (isSelected) GalleryColors.TextPrimary else Color(0xFF3A3A40),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

data class GlassTab(
    val label: String,
    val icon: ImageVector,
    val selected: Boolean,
    val onClick: () -> Unit
)

/** Alt sekme çubuğu + yanında ayrı cam arama dairesi. */
@Composable
fun GlassTabBar(
    backdrop: Backdrop,
    tabs: List<GlassTab>,
    searchLabel: String,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = GalleryColors.Accent
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(GalleryDimens.TabBarHeight)
                .glass(backdrop, Capsule(), GlassTone.Light)
                .padding(GalleryDimens.TabBarInner),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEach { tab ->
                val color = if (tab.selected) accent else GalleryColors.IconOnLight
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(if (tab.selected) GalleryColors.SelectedTabFill else Color.Transparent)
                        .clickable(
                            interactionSource = null,
                            indication = null,
                            role = Role.Tab,
                            onClick = tab.onClick
                        )
                        .semantics { selected = tab.selected },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Glyph(tab.icon, color, size = 24.dp)
                    Spacer(Modifier.height(2.dp))
                    BasicText(tab.label, style = GalleryType.TabLabel.copy(color = color))
                }
            }
        }
        GlassIconButton(
            backdrop = backdrop,
            icon = GalleryIcons.Search,
            contentDescription = searchLabel,
            onClick = onSearch,
            size = GalleryDimens.TabBarHeight,
            iconSize = 26.dp,
            tint = GalleryColors.IconOnLight
        )
    }
}

/** Ekranın üstünde kısa süre görünen cam bildirim. */
@Composable
fun GlassToast(
    backdrop: Backdrop,
    message: String?,
    modifier: Modifier = Modifier,
    tone: GlassTone = GlassTone.Light
) {
    AnimatedVisibility(
        visible = message != null,
        modifier = modifier,
        enter = fadeIn() + slideInVertically { -it / 2 },
        exit = fadeOut() + slideOutVertically { -it / 2 }
    ) {
        Row(
            modifier = Modifier
                .height(40.dp)
                .widthIn(max = 340.dp)
                .glass(backdrop, Capsule(), tone, refraction = false)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Glyph(GalleryIcons.Check, tone.content, size = 16.dp)
            BasicText(
                text = message.orEmpty(),
                maxLines = 1,
                style = GalleryType.Chip.copy(color = tone.content, fontWeight = FontWeight.SemiBold, fontSize = GalleryType.Button.fontSize)
            )
        }
    }
}

data class GlassMenuItem(
    val label: String,
    val checked: Boolean,
    val onClick: () -> Unit
)

/**
 * Sağ üstteki "⋯" menüsü. Ekranın tamamını kaplayan şeffaf alan dokununca kapatır.
 */
@Composable
fun GlassMenu(
    backdrop: Backdrop,
    visible: Boolean,
    items: List<GlassMenuItem>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    tone: GlassTone = GlassTone.Light
) {
    if (visible) {
        Box(
            Modifier
                .fillMaxSize()
                .clickable(interactionSource = null, indication = null, onClick = onDismiss)
        )
    }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn() + scaleIn(initialScale = 0.85f, transformOrigin = TransformOrigin(1f, 0f)),
        exit = fadeOut() + scaleOut(targetScale = 0.85f, transformOrigin = TransformOrigin(1f, 0f))
    ) {
        Column(
            modifier = Modifier
                .width(240.dp)
                .glass(backdrop, RoundedRectangle(22.dp), tone, refraction = false)
                .padding(vertical = 6.dp)
        ) {
            items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clickable(onClick = item.onClick)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                        if (item.checked) Glyph(GalleryIcons.Check, tone.content, size = 16.dp)
                    }
                    Spacer(Modifier.width(8.dp))
                    BasicText(
                        text = item.label,
                        style = GalleryType.Body.copy(color = tone.content, textAlign = TextAlign.Start)
                    )
                }
            }
        }
    }
}
