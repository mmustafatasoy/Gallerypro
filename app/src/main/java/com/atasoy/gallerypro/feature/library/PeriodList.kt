package com.atasoy.gallerypro.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.atasoy.gallerypro.R
import com.atasoy.gallerypro.data.media.Formatters
import com.atasoy.gallerypro.data.media.LocalGalleryImageLoader
import com.atasoy.gallerypro.data.media.MediaThumb
import com.atasoy.gallerypro.data.media.Period
import com.atasoy.gallerypro.data.media.contentUri
import com.atasoy.gallerypro.designsystem.GalleryColors
import com.atasoy.gallerypro.designsystem.GalleryDimens
import com.atasoy.gallerypro.designsystem.GalleryType
import java.util.Locale

/**
 * Yıllar / Aylar görünümü: büyük kapak kartları. En yeni dönem en altta
 * (ters düzen), ekran açılınca oradan başlar.
 */
@Composable
fun PeriodList(
    periods: List<Period>,
    isYears: Boolean,
    state: LazyListState,
    contentPadding: PaddingValues,
    onOpen: (Period) -> Unit,
    modifier: Modifier = Modifier
) {
    val newestFirst = remember(periods) { periods.asReversed() }
    LazyColumn(
        state = state,
        reverseLayout = true,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(GalleryDimens.CardSpacing, Alignment.Bottom),
        modifier = modifier.fillMaxSize()
    ) {
        items(newestFirst, key = { it.key }) { period ->
            PeriodCard(
                period = period,
                isYear = isYears,
                onClick = { onOpen(period) },
                modifier = Modifier.padding(horizontal = 14.dp)
            )
        }
    }
}

@Composable
private fun PeriodCard(
    period: Period,
    isYear: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val imageLoader = LocalGalleryImageLoader.current
    val context = LocalContext.current
    val locale = remember { Locale.getDefault() }
    val shape = RoundedCornerShape(if (isYear) 30.dp else GalleryDimens.CardCorner)
    val countText = context.resources.getQuantityString(
        R.plurals.items_count,
        period.count,
        Formatters.count(period.count, locale)
    )
    val uri = remember(period.cover.id) { period.cover.contentUri() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(if (isYear) GalleryDimens.YearCardHeight else GalleryDimens.MonthCardHeight)
            .shadow(14.dp, shape, ambientColor = Color(0x33000000), spotColor = Color(0x33000000))
            .clip(shape)
            .background(GalleryColors.CellPlaceholder)
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        AsyncImage(
            model = MediaThumb(uri, 1080),
            contentDescription = null,
            imageLoader = imageLoader,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color(0x73000000),
                        0.4f to Color.Transparent,
                        0.7f to Color.Transparent,
                        1f to Color(0x4D000000)
                    )
                )
        )
        Column(Modifier.padding(start = 18.dp, top = 14.dp)) {
            if (isYear) {
                BasicText(period.year.toString(), style = GalleryType.YearTitle.copy(color = Color.White))
            } else {
                BasicText(
                    Formatters.monthName(period.month ?: 1, locale),
                    style = GalleryType.CardTitle.copy(color = Color.White)
                )
                BasicText(
                    period.year.toString(),
                    style = GalleryType.Subtitle.copy(color = Color.White.copy(alpha = 0.92f))
                )
            }
        }
        Box(
            modifier = Modifier
                .align(if (isYear) Alignment.BottomStart else Alignment.BottomEnd)
                .padding(12.dp)
                .height(32.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.86f))
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            BasicText(countText, style = GalleryType.Chip.copy(color = GalleryColors.TextPrimary))
        }
    }
}
