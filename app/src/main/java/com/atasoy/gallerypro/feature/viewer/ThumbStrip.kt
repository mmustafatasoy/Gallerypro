package com.atasoy.gallerypro.feature.viewer

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.atasoy.gallerypro.data.media.LocalGalleryImageLoader
import com.atasoy.gallerypro.data.media.MediaItem
import com.atasoy.gallerypro.data.media.MediaThumb
import com.atasoy.gallerypro.data.media.contentUri
import com.atasoy.gallerypro.designsystem.GalleryMotion
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlin.math.abs

private val ThumbHeight = 42.dp
private val ThumbWidth = 26.dp
private val SelectedWidth = 42.dp
private val SelectedMargin = 6.dp
private val ThumbGap = 2.dp

/**
 * Alttaki küçük resim şeridi. Seçili kare geniş, diğerleri dar.
 * Parmakla kaydırırken ortadaki kare fotoğrafı değiştirir ([onScrub]);
 * fotoğraf değişince şerit kendiliğinden ortalanır.
 */
@Composable
internal fun ThumbStrip(
    items: List<MediaItem>,
    currentIndex: Int,
    onSelect: (Int) -> Unit,
    onScrub: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val imageLoader = LocalGalleryImageLoader.current
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentIndex.coerceAtLeast(0))
    val dragged by listState.interactionSource.collectIsDraggedAsState()
    var scrubbing by remember { mutableStateOf(false) }
    val scrub by rememberUpdatedState(onScrub)

    LaunchedEffect(dragged) {
        if (dragged) scrubbing = true
    }
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { inProgress ->
            if (!inProgress) scrubbing = false
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow { if (scrubbing) listState.centerIndex() else -1 }
            .filter { it >= 0 }
            .distinctUntilChanged()
            .collect { scrub(it) }
    }
    LaunchedEffect(currentIndex, scrubbing) {
        if (!scrubbing && currentIndex >= 0) listState.animateScrollToItem(currentIndex)
    }

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val sidePadding = ((maxWidth - SelectedWidth) / 2 - SelectedMargin).coerceAtLeast(0.dp)
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = sidePadding),
            horizontalArrangement = Arrangement.spacedBy(ThumbGap),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(ThumbHeight + 4.dp)
        ) {
            itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                val selected = index == currentIndex && !scrubbing
                val width by animateDpAsState(
                    targetValue = if (selected) SelectedWidth else ThumbWidth,
                    animationSpec = GalleryMotion.standard(),
                    label = "thumbWidth"
                )
                val margin by animateDpAsState(
                    targetValue = if (selected) SelectedMargin else 0.dp,
                    animationSpec = GalleryMotion.standard(),
                    label = "thumbMargin"
                )
                val uri = remember(item.id, item.isVideo) { item.contentUri() }
                val shape = RoundedCornerShape(4.dp)
                AsyncImage(
                    model = MediaThumb(uri, 128, item.modifiedMillis),
                    contentDescription = null,
                    imageLoader = imageLoader,
                    contentScale = ContentScale.Crop,
                    // Yay animasyonu hedefini biraz aşar; negatif boşluk/boyut Compose'da hata verir.
                    modifier = Modifier
                        .padding(horizontal = margin.coerceAtLeast(0.dp))
                        .size(width.coerceAtLeast(1.dp), ThumbHeight)
                        .graphicsLayer { alpha = if (selected) 1f else 0.85f }
                        .clip(shape)
                        .then(
                            if (selected) {
                                Modifier.border(1.5.dp, Color.White.copy(alpha = 0.9f), shape)
                            } else {
                                Modifier
                            }
                        )
                        .clickable(role = Role.Button) { onSelect(index) }
                )
            }
        }
    }
}

/** Görünür alanın ortasına en yakın öğe. */
private fun LazyListState.centerIndex(): Int {
    val info = layoutInfo
    val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
    return info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - center) }?.index ?: -1
}
