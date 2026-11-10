package com.atasoy.gallerypro.feature.collections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.atasoy.gallerypro.R
import com.atasoy.gallerypro.data.collections.DuplicateReviewGroup
import com.atasoy.gallerypro.data.index.IndexScheduler
import com.atasoy.gallerypro.data.media.LocalGalleryImageLoader
import com.atasoy.gallerypro.data.media.MediaItem
import com.atasoy.gallerypro.data.media.MediaThumb
import com.atasoy.gallerypro.data.media.contentUri
import com.atasoy.gallerypro.designsystem.GalleryColors
import com.atasoy.gallerypro.designsystem.GalleryDimens
import com.atasoy.gallerypro.designsystem.GalleryType
import com.atasoy.gallerypro.designsystem.GlassTextButton
import com.kyant.backdrop.Backdrop

/** Kopyalar bölümü: işlem yalnızca kullanıcı cam onay kartında onay verirse yapılır. */
@Composable
internal fun DuplicateGroupsContent(
    viewModel: CollectionsViewModel,
    backdrop: Backdrop,
    candidates: List<MediaItem>,
    topInset: Dp,
    bottomInset: Dp,
    onOpen: (MediaItem) -> Unit,
    onReview: (DuplicateReviewGroup) -> Unit
) {
    val groups by viewModel.duplicateGroups.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val loader = LocalGalleryImageLoader.current
    if (groups.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(GalleryDimens.CardSpacing),
                modifier = Modifier.padding(horizontal = GalleryDimens.ScreenPadding)
            ) {
                BasicText(
                    stringResource(if (candidates.isEmpty()) R.string.collection_empty else R.string.duplicates_no_verified),
                    style = GalleryType.Body
                )
                if (candidates.isNotEmpty()) {
                    GlassTextButton(
                        backdrop = backdrop,
                        text = stringResource(R.string.settings_reindex),
                        onClick = { IndexScheduler.runNow(context) }
                    )
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(GalleryDimens.CardSpacing),
            contentPadding = PaddingValues(
                start = GalleryDimens.ScreenPadding,
                end = GalleryDimens.ScreenPadding,
                top = topInset + GalleryDimens.HeaderHeight,
                bottom = bottomInset + GalleryDimens.BottomFadeHeight
            )
        ) {
            items(groups, key = { it.keep.id }) { group ->
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .background(GalleryColors.GroupedFill, RoundedCornerShape(GalleryDimens.GroupedCorner))
                        .padding(GalleryDimens.ScreenPadding),
                    verticalArrangement = Arrangement.spacedBy(GalleryDimens.CardSpacing)
                ) {
                    BasicText(
                        context.resources.getQuantityString(
                            R.plurals.duplicates_group_count,
                            group.members.size, group.members.size
                        ),
                        style = GalleryType.SectionTitle
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(GalleryDimens.GridSpacing)) {
                        items(group.members, key = { it.id }) { item ->
                            Column(
                                modifier = Modifier.size(
                                    width = GalleryDimens.DuplicateThumb,
                                    height = GalleryDimens.DuplicateThumb + GalleryDimens.GroupedRowHeight
                                ),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AsyncImage(
                                    model = MediaThumb(item.contentUri(), 256, item.modifiedMillis),
                                    contentDescription = item.displayName,
                                    imageLoader = loader,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(GalleryDimens.DuplicateThumb)
                                        .clip(RoundedCornerShape(GalleryDimens.AlbumCorner))
                                        .clickable { onOpen(item) }
                                )
                                BasicText(
                                    stringResource(if (item.id == group.keep.id)
                                        R.string.duplicates_keep else R.string.duplicates_candidate),
                                    style = GalleryType.Chip.copy(
                                        color = if (item.id == group.keep.id) GalleryColors.Accent
                                            else GalleryColors.TextSecondary
                                    ),
                                    modifier = Modifier.padding(top = GalleryDimens.GridSpacing)
                                )
                            }
                        }
                    }
                    BasicText(
                        stringResource(
                            R.string.duplicates_keep_dimensions,
                            group.keep.width,
                            group.keep.height
                        ),
                        style = GalleryType.Subtitle
                    )
                    GlassTextButton(
                        backdrop = backdrop,
                        text = context.resources.getQuantityString(
                            R.plurals.duplicates_combine_action,
                            group.discard.size, group.discard.size
                        ),
                        onClick = { onReview(group) }
                    )
                }
            }
        }
    }
}
