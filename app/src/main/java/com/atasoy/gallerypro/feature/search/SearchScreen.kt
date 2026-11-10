package com.atasoy.gallerypro.feature.search

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.atasoy.gallerypro.R
import com.atasoy.gallerypro.data.index.IndexScheduler
import com.atasoy.gallerypro.data.index.IndexStatus
import com.atasoy.gallerypro.data.index.LabelDictionary
import com.atasoy.gallerypro.data.index.SearchText
import com.atasoy.gallerypro.data.media.Formatters
import com.atasoy.gallerypro.data.media.LocalGalleryImageLoader
import com.atasoy.gallerypro.data.media.MediaItem
import com.atasoy.gallerypro.data.media.MediaThumb
import com.atasoy.gallerypro.data.media.contentUri
import com.atasoy.gallerypro.designsystem.GalleryColors
import com.atasoy.gallerypro.designsystem.GalleryDimens
import com.atasoy.gallerypro.designsystem.GalleryType
import com.atasoy.gallerypro.designsystem.GlassIconButton
import com.atasoy.gallerypro.designsystem.GlassTone
import com.atasoy.gallerypro.designsystem.Glyph
import com.atasoy.gallerypro.designsystem.LocalSearchLauncher
import com.atasoy.gallerypro.designsystem.ProgressiveBlurEdge
import com.atasoy.gallerypro.designsystem.SearchLauncher
import com.atasoy.gallerypro.designsystem.glass
import com.atasoy.gallerypro.designsystem.icons.GalleryIcons
import com.atasoy.gallerypro.feature.viewer.ViewerScreen
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.Capsule
import java.util.Locale

private const val FilterAll = 0
private const val FilterPhotos = 1
private const val FilterVideos = 2

/**
 * Yapay zekâ ile arama (5. aşama, ilk adım). Altta yüzen cam arama kutusu ve kapatma dairesi;
 * klavye açılınca kutu klavyenin üstüne kalkar. Boşken: dizin durumu, öneriler, son aramalar.
 * Yazarken sonuçlar 300 ms gecikmeyle güncellenir; dokunulan sonuç görüntüleyicide açılır.
 */
@Composable
fun SearchScreen(viewModel: SearchViewModel, onClose: () -> Unit) {
    val context = LocalContext.current
    val locale = remember { Locale.getDefault() }
    val turkishUi = locale.language == "tr"
    val backdrop = rememberLayerBackdrop()
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = remember { FocusRequester() }

    val results by viewModel.results.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val recent by viewModel.recent.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val indexed by viewModel.indexedCount.collectAsStateWithLifecycle()
    val mediaCount by viewModel.mediaCount.collectAsStateWithLifecycle()
    val vectorCount by viewModel.vectorCount.collectAsStateWithLifecycle()
    val semantic = remember { viewModel.semanticAvailable }

    // Yazı alanı yerel durumla çalışır (hızlı yazarken harf kaybolmasın); arama ViewModel'de.
    var text by rememberSaveable { mutableStateOf(viewModel.query.value) }
    var filter by rememberSaveable { mutableIntStateOf(FilterAll) }
    var viewerId by rememberSaveable { mutableStateOf<Long?>(null) }
    var requested by remember { mutableStateOf(false) }

    fun setText(value: String) {
        text = value
        viewModel.setQuery(value)
    }

    LaunchedEffect(Unit) {
        // Bilgi panelindeki bir etiketten açıldıysa o sözcükle arar; yoksa klavye açılır.
        val pending = viewModel.takePendingQuery()
        if (pending != null) {
            setText(pending)
            filter = FilterAll
        } else {
            runCatching { focus.requestFocus() }
        }
    }
    // Arama sonucundan açılan görüntüleyicide etikete dokununca: görüntüleyici kapanır, o sözcük aranır.
    val searchFromViewer = remember {
        SearchLauncher { q ->
            if (q != null) {
                viewerId = null
                filter = FilterAll
                setText(q)
            }
        }
    }
    BackHandler(enabled = viewerId == null) { onClose() }

    val shown: List<MediaItem>? = results?.let { list ->
        when (filter) {
            FilterPhotos -> list.filter { !it.isVideo }
            FilterVideos -> list.filter { it.isVideo }
            else -> list
        }
    }

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val barSpace = bottomInset + 88.dp

    Box(
        Modifier
            .fillMaxSize()
            .background(GalleryColors.LightBackground)
            .clickable(interactionSource = null, indication = null) {}
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .background(GalleryColors.LightBackground)
        ) {
            if (shown == null) {
                EmptyState(
                    topInset = topInset,
                    bottomSpace = barSpace,
                    status = status,
                    indexed = indexed,
                    mediaCount = mediaCount,
                    requested = requested,
                    semantic = semantic,
                    sentences = vectorCount > 0,
                    suggestions = suggestions.map { LabelDictionary.display(it, turkishUi) },
                    recent = recent,
                    onIndexNow = {
                        requested = true
                        IndexScheduler.runNow(context)
                    },
                    onPick = { picked ->
                        setText(SearchText.capitalize(picked, locale))
                        keyboard?.hide()
                    },
                    onClearRecent = viewModel::clearRecent
                )
            } else {
                Results(
                    list = shown,
                    total = results?.size ?: 0,
                    query = text,
                    filter = filter,
                    incomplete = indexed < mediaCount,
                    topInset = topInset,
                    bottomSpace = barSpace,
                    locale = locale,
                    onFilter = { filter = it },
                    onOpen = { item ->
                        keyboard?.hide()
                        viewModel.remember(text)
                        viewerId = item.id
                    }
                )
            }
        }

        ProgressiveBlurEdge(
            backdrop = backdrop,
            height = topInset + 16.dp,
            tint = Color.White,
            modifier = Modifier.align(Alignment.TopCenter)
        )
        ProgressiveBlurEdge(
            backdrop = backdrop,
            height = barSpace,
            tint = Color.White,
            fromBottom = true,
            tintIntensity = 0.3f,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // ---------------------------------------------------------------- arama kutusu
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                Modifier
                    .weight(1f)
                    .height(52.dp)
                    .glass(backdrop, Capsule(), GlassTone.Light)
                    .padding(start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Glyph(GalleryIcons.Search, GalleryColors.IconOnLight, size = 20.dp)
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (text.isEmpty()) {
                        BasicText(
                            stringResource(if (semantic) R.string.search_hint_semantic else R.string.search_hint),
                            style = GalleryType.Body.copy(color = GalleryColors.TextTertiary),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = text,
                        onValueChange = ::setText,
                        singleLine = true,
                        textStyle = GalleryType.Body,
                        cursorBrush = SolidColor(GalleryColors.Accent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            keyboard?.hide()
                            viewModel.remember(text)
                        }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focus)
                    )
                }
                if (text.isNotEmpty()) {
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable { setText("") },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier
                                .size(20.dp)
                                .background(GalleryColors.TextTertiary.copy(alpha = 0.35f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Glyph(GalleryIcons.Close, Color.White, size = 12.dp)
                        }
                    }
                }
            }
            GlassIconButton(
                backdrop = backdrop,
                icon = GalleryIcons.Close,
                contentDescription = stringResource(R.string.search_close),
                onClick = onClose,
                size = 52.dp,
                iconSize = 20.dp,
                tint = GalleryColors.IconOnLight
            )
        }

        AnimatedVisibility(
            visible = viewerId != null && !shown.isNullOrEmpty(),
            enter = fadeIn() + scaleIn(initialScale = 0.94f),
            exit = fadeOut() + scaleOut(targetScale = 0.94f),
            modifier = Modifier.fillMaxSize()
        ) {
            val list = shown.orEmpty()
            if (list.isNotEmpty()) {
                val startId = remember { viewerId ?: list.first().id }
                CompositionLocalProvider(LocalSearchLauncher provides searchFromViewer) {
                    ViewerScreen(
                        items = list,
                        startId = startId,
                        onClose = { viewerId = null }
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------- boş hâl

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmptyState(
    topInset: androidx.compose.ui.unit.Dp,
    bottomSpace: androidx.compose.ui.unit.Dp,
    status: IndexStatus,
    indexed: Int,
    mediaCount: Int,
    requested: Boolean,
    semantic: Boolean,
    sentences: Boolean,
    suggestions: List<String>,
    recent: List<String>,
    onIndexNow: () -> Unit,
    onPick: (String) -> Unit,
    onClearRecent: () -> Unit
) {
    val locale = remember { Locale.getDefault() }
    val chips = listOf(
        stringResource(R.string.search_chip_videos),
        stringResource(R.string.search_chip_screenshots),
        stringResource(R.string.search_chip_favorites)
    ) + suggestions
    val examples = listOf(
        stringResource(R.string.search_sentence_1),
        stringResource(R.string.search_sentence_2),
        stringResource(R.string.search_sentence_3),
        stringResource(R.string.search_sentence_4)
    )
    LazyColumn(
        contentPadding = PaddingValues(top = topInset + 12.dp, bottom = bottomSpace + 12.dp)
    ) {
        item {
            BasicText(
                stringResource(R.string.tab_search),
                style = GalleryType.LargeTitle,
                modifier = Modifier.padding(start = GalleryDimens.TitleStartPadding, end = 20.dp, bottom = 14.dp)
            )
        }
        item {
            IndexCard(status, indexed, mediaCount, requested, semantic, locale, onIndexNow)
        }
        if (sentences) {
            item {
                SectionHeader(stringResource(R.string.search_sentences))
                ChipRow(examples, locale, onPick)
            }
        }
        item {
            SectionHeader(stringResource(R.string.search_suggested))
            ChipRow(chips, locale, onPick)
        }
        if (recent.isNotEmpty()) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(end = GalleryDimens.ScreenPadding),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.weight(1f)) { SectionHeader(stringResource(R.string.search_recent)) }
                    BasicText(
                        stringResource(R.string.search_clear),
                        style = GalleryType.Button.copy(color = GalleryColors.Accent),
                        modifier = Modifier
                            .padding(top = 18.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onClearRecent)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            items(recent.size) { i ->
                val q = recent[i]
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onPick(q) }
                        .padding(horizontal = GalleryDimens.ScreenPadding, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Glyph(GalleryIcons.Search, GalleryColors.TextTertiary, size = 18.dp)
                    Spacer(Modifier.width(12.dp))
                    BasicText(q, style = GalleryType.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(chips: List<String>, locale: Locale, onPick: (String) -> Unit) {
    FlowRow(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = GalleryDimens.ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        chips.forEach { chip ->
            Box(
                Modifier
                    .height(36.dp)
                    .clip(CircleShape)
                    .background(GalleryColors.GroupedFill)
                    .clickable { onPick(chip) }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    SearchText.capitalize(chip, locale),
                    style = GalleryType.Chip.copy(color = GalleryColors.TextPrimary)
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    BasicText(
        title,
        style = GalleryType.SectionTitle,
        modifier = Modifier.padding(start = GalleryDimens.ScreenPadding, end = GalleryDimens.ScreenPadding, top = 22.dp, bottom = 10.dp)
    )
}

/** "Akıllı arama" kartı: dizinin ne kadarının hazır olduğu ve "Şimdi incele". */
@Composable
private fun IndexCard(
    status: IndexStatus,
    indexed: Int,
    mediaCount: Int,
    requested: Boolean,
    semantic: Boolean,
    locale: Locale,
    onIndexNow: () -> Unit
) {
    val total = if (status.total > 0) status.total else mediaCount
    val done = if (status.running) status.done else indexed.coerceAtMost(total)
    val complete = total > 0 && done >= total
    val body = when {
        status.running -> stringResource(R.string.index_running, Formatters.count(done, locale), Formatters.count(total, locale))
        complete -> androidx.compose.ui.platform.LocalContext.current.resources.getQuantityString(
            R.plurals.index_ready, total, Formatters.count(total, locale)
        )
        done == 0 -> stringResource(R.string.index_empty)
        else -> stringResource(R.string.index_partial, Formatters.count(done, locale), Formatters.count(total, locale))
    }
    Column(
        Modifier
            .padding(horizontal = GalleryDimens.ScreenPadding)
            .fillMaxWidth()
            .clip(RoundedCornerShape(GalleryDimens.GroupedCorner))
            .background(GalleryColors.GroupedFill)
            .padding(16.dp)
    ) {
        BasicText(
            stringResource(R.string.index_title),
            style = GalleryType.Body.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(Modifier.height(4.dp))
        BasicText(body, style = GalleryType.Body.copy(color = GalleryColors.TextSecondary))
        if (semantic) {
            Spacer(Modifier.height(6.dp))
            BasicText(
                stringResource(R.string.index_semantic),
                style = GalleryType.Subtitle
            )
        }
        if (status.running || (total > 0 && !complete)) {
            Spacer(Modifier.height(12.dp))
            val fraction = if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else 0f
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(GalleryColors.TextTertiary.copy(alpha = 0.18f))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(GalleryColors.Accent)
                )
            }
        }
        if (!status.running && !complete && mediaCount > 0) {
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(if (requested) GalleryColors.TextTertiary.copy(alpha = 0.2f) else GalleryColors.Accent)
                    .clickable(enabled = !requested, onClick = onIndexNow)
                    .padding(horizontal = 16.dp, vertical = 9.dp)
            ) {
                BasicText(
                    stringResource(if (requested) R.string.index_queued else R.string.index_now),
                    style = GalleryType.Button.copy(color = if (requested) GalleryColors.TextSecondary else Color.White)
                )
            }
        }
    }
}

// -------------------------------------------------------------------- sonuçlar

@Composable
private fun Results(
    list: List<MediaItem>,
    total: Int,
    query: String,
    filter: Int,
    incomplete: Boolean,
    topInset: androidx.compose.ui.unit.Dp,
    bottomSpace: androidx.compose.ui.unit.Dp,
    locale: Locale,
    onFilter: (Int) -> Unit,
    onOpen: (MediaItem) -> Unit
) {
    val context = LocalContext.current
    val imageLoader = LocalGalleryImageLoader.current
    val density = LocalDensity.current
    val columns = 3
    val thumbPx = with(density) { (LocalConfiguration.current.screenWidthDp.dp / columns).roundToPx() }
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(top = topInset + 12.dp, bottom = bottomSpace + 12.dp),
        horizontalArrangement = Arrangement.spacedBy(GalleryDimens.GridSpacing),
        verticalArrangement = Arrangement.spacedBy(GalleryDimens.GridSpacing)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.padding(start = GalleryDimens.ScreenPadding, end = GalleryDimens.ScreenPadding, bottom = 10.dp)) {
                BasicText(
                    context.resources.getQuantityString(R.plurals.search_results_count, total, Formatters.count(total, locale)),
                    style = GalleryType.SectionTitle
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(stringResource(R.string.search_filter_all), filter == FilterAll) { onFilter(FilterAll) }
                    FilterChip(stringResource(R.string.search_filter_photos), filter == FilterPhotos) { onFilter(FilterPhotos) }
                    FilterChip(stringResource(R.string.search_filter_videos), filter == FilterVideos) { onFilter(FilterVideos) }
                }
                if (list.isEmpty()) {
                    Spacer(Modifier.height(40.dp))
                    BasicText(
                        stringResource(R.string.search_no_results, query.trim()),
                        style = GalleryType.SectionTitle,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (incomplete) {
                        Spacer(Modifier.height(8.dp))
                        BasicText(
                            stringResource(R.string.search_incomplete_hint),
                            style = GalleryType.Body.copy(color = GalleryColors.TextSecondary)
                        )
                    }
                }
            }
        }
        items(list, key = { it.id }) { item ->
            Box(
                Modifier
                    .aspectRatio(1f)
                    .background(GalleryColors.CellPlaceholder)
                    .clickable { onOpen(item) }
            ) {
                AsyncImage(
                    model = MediaThumb(item.contentUri(), thumbPx, item.modifiedMillis),
                    contentDescription = null,
                    imageLoader = imageLoader,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (item.isVideo) {
                    BasicText(
                        Formatters.duration(item.durationMs),
                        style = GalleryType.Duration.copy(color = Color.White),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(5.dp)
                            .clip(CircleShape)
                            .background(Color(0x66000000))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .height(32.dp)
            .clip(CircleShape)
            .background(if (selected) GalleryColors.TextPrimary else GalleryColors.GroupedFill)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            label,
            style = GalleryType.Chip.copy(color = if (selected) Color.White else GalleryColors.TextPrimary)
        )
    }
}
