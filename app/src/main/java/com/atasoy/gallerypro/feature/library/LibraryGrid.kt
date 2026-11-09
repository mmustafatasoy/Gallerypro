package com.atasoy.gallerypro.feature.library

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.atasoy.gallerypro.R
import com.atasoy.gallerypro.data.media.Formatters
import com.atasoy.gallerypro.data.media.LocalGalleryImageLoader
import com.atasoy.gallerypro.data.media.MediaItem
import com.atasoy.gallerypro.data.media.MediaThumb
import com.atasoy.gallerypro.data.media.contentUri
import com.atasoy.gallerypro.designsystem.GalleryColors
import com.atasoy.gallerypro.designsystem.GalleryDimens
import com.atasoy.gallerypro.designsystem.GalleryMotion
import com.atasoy.gallerypro.designsystem.GalleryType
import com.atasoy.gallerypro.designsystem.Glyph
import com.atasoy.gallerypro.designsystem.icons.GalleryIcons
import java.time.ZoneId
import java.util.Locale

/** Satırı tam doldurmak için başa eklenen boş hücre sayısı (iOS: son satır her zaman dolu). */
fun leadingSpacers(count: Int, columns: Int): Int = (columns - count % columns) % columns

@Composable
fun LibraryGrid(
    items: List<MediaItem>,
    columns: Int,
    state: LazyGridState,
    contentPadding: PaddingValues,
    selecting: Boolean,
    selected: Set<Long>,
    onOpen: (MediaItem) -> Unit,
    onToggle: (MediaItem) -> Unit,
    onLongPress: (MediaItem) -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val leading = leadingSpacers(items.size, columns)
    val density = LocalDensity.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val thumbPx = remember(columns, screenWidthDp, density) {
        with(density) { MediaThumb.bucket((screenWidthDp.dp / columns).roundToPx()) }
    }
    val locale = remember { Locale.getDefault() }
    val zone = remember { ZoneId.systemDefault() }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = state,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(GalleryDimens.GridSpacing),
        verticalArrangement = Arrangement.spacedBy(GalleryDimens.GridSpacing),
        modifier = modifier
            .fillMaxSize()
            .pinchToChangeColumns(onZoomIn = onZoomIn, onZoomOut = onZoomOut)
    ) {
        items(count = leading, key = { "pad$it" }, contentType = { "pad" }) {
            Spacer(Modifier.aspectRatio(1f))
        }
        items(items, key = { it.id }, contentType = { "media" }) { item ->
            MediaCell(
                item = item,
                thumbPx = thumbPx,
                selecting = selecting,
                isSelected = item.id in selected,
                showBadges = columns <= 5,
                locale = locale,
                zone = zone,
                onClick = { if (selecting) onToggle(item) else onOpen(item) },
                onLongClick = { onLongPress(item) },
                modifier = Modifier.animateItem(
                    fadeInSpec = null,
                    fadeOutSpec = null,
                    placementSpec = GalleryMotion.standard()
                )
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaCell(
    item: MediaItem,
    thumbPx: Int,
    selecting: Boolean,
    isSelected: Boolean,
    showBadges: Boolean,
    locale: Locale,
    zone: ZoneId,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val imageLoader = LocalGalleryImageLoader.current
    val haptics = LocalHapticFeedback.current
    val uri = remember(item.id, item.isVideo) { item.contentUri() }
    val inset by animateFloatAsState(
        targetValue = if (isSelected) 0.9f else 1f,
        animationSpec = GalleryMotion.standard(),
        label = "cellInset"
    )
    val corner by animateDpAsState(
        targetValue = if (isSelected) 6.dp else 0.dp,
        animationSpec = GalleryMotion.standard(),
        label = "cellCorner"
    )
    val dateText = remember(item.takenAtMillis) { Formatters.longDate(item.takenAtMillis, zone, locale) }
    val description = if (item.isVideo) {
        stringResource(R.string.cd_video, dateText, Formatters.duration(item.durationMs))
    } else {
        stringResource(R.string.cd_photo, dateText)
    }
    val selectedText = stringResource(R.string.cd_selected)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(GalleryColors.CellPlaceholder)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
            .semantics {
                contentDescription = if (isSelected) "$description, $selectedText" else description
            }
    ) {
        AsyncImage(
            model = MediaThumb(uri, thumbPx, item.modifiedMillis),
            contentDescription = null,
            imageLoader = imageLoader,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = inset
                    scaleY = inset
                }
                .clip(RoundedCornerShape(corner))
        )

        if (showBadges && item.isVideo) {
            BasicText(
                text = Formatters.duration(item.durationMs),
                style = GalleryType.Duration.copy(
                    color = Color.White,
                    shadow = Shadow(Color(0x99000000), blurRadius = 6f)
                ),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 7.dp, bottom = 6.dp)
            )
        }
        if (showBadges && item.isFavorite && !selecting) {
            Glyph(
                icon = GalleryIcons.HeartFilled,
                tint = Color.White,
                size = 14.dp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 7.dp, bottom = 7.dp)
            )
        }
        if (selecting) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) GalleryColors.Accent else Color.White.copy(alpha = 0.35f))
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) Glyph(GalleryIcons.Check, Color.White, size = 13.dp)
            }
        }
    }
}

/**
 * İki parmakla sıkıştır/aç: sütun sayısını değiştirir. Tek parmak kaydırmaya karışmaz;
 * iki parmak varken olaylar tüketilir ki ızgara kaymasın.
 */
private fun Modifier.pinchToChangeColumns(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit
): Modifier = this.then(
    Modifier.pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var zoom = 1f
            var fired = false
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val fingers = event.changes.count { it.pressed }
                if (fingers >= 2) {
                    zoom *= event.calculateZoom()
                    if (!fired && zoom > 1.25f) {
                        onZoomIn()
                        fired = true
                    } else if (!fired && zoom < 0.8f) {
                        onZoomOut()
                        fired = true
                    }
                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                }
            } while (event.changes.any { it.pressed })
        }
    }
)
