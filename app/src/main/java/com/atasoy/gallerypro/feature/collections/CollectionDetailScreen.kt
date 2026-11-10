package com.atasoy.gallerypro.feature.collections

import android.app.Activity
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.atasoy.gallerypro.R
import com.atasoy.gallerypro.data.collections.Album
import com.atasoy.gallerypro.data.collections.CollectionKeys
import com.atasoy.gallerypro.data.collections.DuplicateReviewGroup
import com.atasoy.gallerypro.data.media.Formatters
import com.atasoy.gallerypro.data.media.MediaItem
import com.atasoy.gallerypro.data.media.ManageMedia
import com.atasoy.gallerypro.data.media.contentUri
import com.atasoy.gallerypro.data.memories.MemoryBuilder
import com.atasoy.gallerypro.designsystem.GalleryColors
import com.atasoy.gallerypro.designsystem.GalleryDimens
import com.atasoy.gallerypro.designsystem.GalleryType
import com.atasoy.gallerypro.designsystem.GlassIconButton
import com.atasoy.gallerypro.designsystem.GlassTextButton
import com.atasoy.gallerypro.designsystem.GlassToast
import com.atasoy.gallerypro.designsystem.ProgressiveBlurEdge
import com.atasoy.gallerypro.designsystem.icons.GalleryIcons
import com.atasoy.gallerypro.feature.library.ColumnSteps
import com.atasoy.gallerypro.feature.library.LibraryGrid
import com.atasoy.gallerypro.feature.library.LongPressPreview
import com.atasoy.gallerypro.feature.library.leadingSpacers
import com.atasoy.gallerypro.feature.viewer.DeleteCard
import com.atasoy.gallerypro.feature.viewer.ViewerMode
import com.atasoy.gallerypro.feature.viewer.ViewerScreen
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import java.util.Locale

/**
 * Bir koleksiyonun ızgarası: Kütüphane ile aynı hücreler, en yeni en altta,
 * iki parmakla sütun değişir. Dokununca görüntüleyici bu koleksiyonun öğeleriyle açılır.
 */
@Composable
internal fun CollectionDetailScreen(
    viewModel: CollectionsViewModel,
    collectionKey: String,
    title: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val locale = remember { Locale.getDefault() }
    val backdrop = rememberLayerBackdrop()
    val itemsFlow = remember(collectionKey) { viewModel.items(collectionKey) }
    val items: List<MediaItem>? by itemsFlow.collectAsStateWithLifecycle(initialValue = null)
    val gridState = rememberLazyGridState()
    var columns by rememberSaveable { mutableIntStateOf(3) }
    var viewerId by rememberSaveable { mutableStateOf<Long?>(null) }
    var previewItem by remember { mutableStateOf<MediaItem?>(null) }
    var pendingMerge by remember { mutableStateOf<DuplicateReviewGroup?>(null) }
    var mergeNotice by remember { mutableStateOf<String?>(null) }
    val collectionsContent by viewModel.content.collectAsStateWithLifecycle()
    var startSlideshow by rememberSaveable { mutableStateOf(false) }
    var scrolledToEnd by rememberSaveable { mutableStateOf(false) }

    val isTrash = collectionKey == CollectionKeys.Trash
    val isDuplicates = collectionKey == CollectionKeys.Duplicates
    val mode = when (collectionKey) {
        CollectionKeys.Trash -> ViewerMode.Trash
        CollectionKeys.Hidden -> ViewerMode.Hidden
        else -> ViewerMode.Normal
    }

    val mergedText = stringResource(R.string.duplicates_moved_to_trash)
    val changedText = stringResource(R.string.duplicates_group_changed)
    val needsAndroid11 = stringResource(R.string.trash_needs_android11)
    val trashLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        pendingMerge = null
        if (result.resultCode == Activity.RESULT_OK) {
            mergeNotice = mergedText
            viewModel.refreshMedia()
        }
    }
    BackHandler(enabled = viewerId == null && pendingMerge == null && previewItem == null, onBack = onBack)
    BackHandler(enabled = pendingMerge != null) { pendingMerge = null }

    // İlk açılışta en alttan (en yeni) başla.
    val list = items
    LaunchedEffect(list?.size, columns) {
        if (list != null && list.isNotEmpty() && !scrolledToEnd) {
            gridState.scrollToItem(leadingSpacers(list.size, columns) + list.lastIndex)
            scrolledToEnd = true
        }
    }

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        Modifier
            .fillMaxSize()
            .background(GalleryColors.LightBackground)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .background(GalleryColors.LightBackground)
        ) {
            when {
                list == null -> Unit
                isTrash && !trashSupported -> CenterMessage(stringResource(R.string.trash_album_needs_android11))
                // Cam butonlu Kopyalar içeriğini bu layerBackdrop içine çizme.
                // Dışarıda kardeş öğe olarak çizilecek, yoksa döngüsel backdrop oluşur.
                isDuplicates -> Unit
                list.isEmpty() -> CenterMessage(stringResource(R.string.collection_empty))
                else -> LibraryGrid(
                    items = list,
                    columns = columns,
                    state = gridState,
                    contentPadding = PaddingValues(top = topInset + 128.dp, bottom = bottomInset + 40.dp),
                    selecting = false,
                    selected = emptySet(),
                    onOpen = { startSlideshow = false; viewerId = it.id },
                    onToggle = {},
                    onLongPress = {
                        if (isTrash || collectionKey == CollectionKeys.Hidden) {
                            startSlideshow = false
                            viewerId = it.id
                        } else previewItem = it
                    },
                    onZoomIn = {
                        val i = ColumnSteps.indexOf(columns).coerceAtLeast(0)
                        columns = ColumnSteps[(i - 1).coerceAtLeast(0)]
                    },
                    onZoomOut = {
                        val i = ColumnSteps.indexOf(columns).coerceAtLeast(0)
                        columns = ColumnSteps[(i + 1).coerceAtMost(ColumnSteps.lastIndex)]
                    }
                )
            }
        }

        if (isDuplicates && list != null) {
            DuplicateGroupsContent(
                viewModel = viewModel,
                backdrop = backdrop,
                candidates = list.orEmpty(),
                topInset = topInset,
                bottomInset = bottomInset,
                onOpen = { startSlideshow = false; viewerId = it.id },
                onReview = { pendingMerge = it }
            )
        }

        val subtitle = when {
            list == null -> ""
            isTrash && list.isNotEmpty() -> stringResource(R.string.trash_note)
            else -> context.resources.getQuantityString(R.plurals.items_count, list.size, Formatters.count(list.size, locale))
        }
        DetailHeader(
            backdrop = backdrop,
            title = title,
            subtitle = subtitle,
            topInset = topInset,
            onBack = onBack,
            playLabel = if (MemoryBuilder.isMemoryKey(collectionKey) && !list.isNullOrEmpty()) {
                stringResource(R.string.video_play)
            } else null,
            onPlay = {
                list?.firstOrNull()?.let { first ->
                    startSlideshow = true
                    viewerId = first.id
                }
            }
        )

        ProgressiveBlurEdge(
            backdrop = backdrop,
            height = bottomInset + 60.dp,
            tint = Color.White,
            fromBottom = true,
            tintIntensity = 0.3f,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        previewItem?.let { chosen ->
            LongPressPreview(chosen, collectionsContent.albums, backdrop, onDismiss = { previewItem = null })
        }

        pendingMerge?.let { proposed ->
            Box(Modifier.fillMaxSize()
                .background(GalleryColors.PreviewScrim)
                .clickable { pendingMerge = null }
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = GalleryDimens.ScreenPadding, vertical = GalleryDimens.CardSpacing)
            ) {
                DeleteCard(
                    message = context.resources.getQuantityString(
                        R.plurals.duplicates_merge_message,
                        proposed.discard.size, proposed.discard.size
                    ),
                    action = context.resources.getQuantityString(
                        R.plurals.duplicates_merge_action,
                        proposed.discard.size, proposed.discard.size
                    ),
                    backdrop = backdrop,
                    showAccessHint = !ManageMedia.granted(context) && ManageMedia.supported,
                    onGrantAccess = {
                        pendingMerge = null
                        ManageMedia.request(context)
                    },
                    onConfirm = {
                        val current = viewModel.duplicateGroups.value.firstOrNull {
                            it.keep.id == proposed.keep.id
                        }
                        val sameCandidates = current?.discard?.map { it.id }?.toSet() ==
                            proposed.discard.map { it.id }.toSet()
                        pendingMerge = null
                        if (current == null || !sameCandidates) {
                            mergeNotice = changedText
                        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                            mergeNotice = needsAndroid11
                        } else {
                            runCatching {
                                val request = MediaStore.createTrashRequest(
                                    context.contentResolver,
                                    proposed.discard.map { it.contentUri() },
                                    true
                                )
                                trashLauncher.launch(
                                    IntentSenderRequest.Builder(request.intentSender).build()
                                )
                            }.onFailure { mergeNotice = changedText }
                        }
                    },
                    onCancel = { pendingMerge = null }
                )
            }
        }

        GlassToast(
            backdrop = backdrop,
            message = mergeNotice,
            modifier = Modifier.align(Alignment.TopCenter)
                .padding(top = topInset + GalleryDimens.TopButton)
        )

        AnimatedVisibility(
            visible = viewerId != null && !list.isNullOrEmpty(),
            enter = fadeIn() + scaleIn(initialScale = 0.94f),
            exit = fadeOut() + scaleOut(targetScale = 0.94f),
            modifier = Modifier.fillMaxSize()
        ) {
            val shown = list.orEmpty()
            if (shown.isNotEmpty()) {
                val startId = remember { viewerId ?: shown.last().id }
                ViewerScreen(
                    items = shown,
                    startId = startId,
                    onClose = {
                        viewerId = null
                        startSlideshow = false
                    },
                    mode = mode,
                    startSlideshow = startSlideshow
                )
            }
        }
    }
}

/** Tüm albümler: iki sütunlu kapak ızgarası. */
@Composable
internal fun AllAlbumsScreen(
    albums: List<Album>,
    locale: Locale,
    onOpen: (Album) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val backdrop = rememberLayerBackdrop()
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    BackHandler(onBack = onBack)

    Box(
        Modifier
            .fillMaxSize()
            .background(GalleryColors.LightBackground)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .background(GalleryColors.LightBackground)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = topInset + 128.dp,
                    bottom = bottomInset + 40.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(albums, key = { it.path }) { album ->
                    AlbumTile(album, locale, onClick = { onOpen(album) })
                }
            }
        }
        DetailHeader(
            backdrop = backdrop,
            title = stringResource(R.string.section_albums),
            subtitle = context.resources.getQuantityString(R.plurals.albums_count, albums.size, Formatters.count(albums.size, locale)),
            topInset = topInset,
            onBack = onBack
        )
    }
}

/** Geri düğmesi + büyük başlık; arkasında kademeli bulanıklık. */
@Composable
private fun DetailHeader(
    backdrop: Backdrop,
    title: String,
    subtitle: String,
    topInset: androidx.compose.ui.unit.Dp,
    onBack: () -> Unit,
    playLabel: String? = null,
    onPlay: (() -> Unit)? = null
) {
    Box(Modifier.fillMaxWidth()) {
        ProgressiveBlurEdge(
            backdrop = backdrop,
            height = topInset + 140.dp,
            tint = Color.White
        )
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = topInset + 8.dp, start = 14.dp, end = GalleryDimens.ScreenPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassIconButton(
                    backdrop = backdrop,
                    icon = GalleryIcons.Back,
                    contentDescription = stringResource(R.string.viewer_back),
                    onClick = onBack,
                    tint = GalleryColors.TextPrimary
                )
                if (playLabel != null && onPlay != null) {
                    GlassTextButton(
                        backdrop = backdrop,
                        text = playLabel,
                        onClick = onPlay,
                        textColor = GalleryColors.TextPrimary
                    )
                }
            }
            Column(Modifier.padding(start = 6.dp, top = 8.dp)) {
                BasicText(
                    title,
                    style = GalleryType.LargeTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotEmpty()) {
                    BasicText(
                        subtitle,
                        style = GalleryType.Subtitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
