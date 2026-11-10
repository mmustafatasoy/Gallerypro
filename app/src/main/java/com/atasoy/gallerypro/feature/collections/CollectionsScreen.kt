package com.atasoy.gallerypro.feature.collections

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.atasoy.gallerypro.R
import com.atasoy.gallerypro.data.collections.Album
import com.atasoy.gallerypro.data.collections.CollectionKeys
import com.atasoy.gallerypro.data.collections.CollectionsContent
import com.atasoy.gallerypro.data.collections.CollectionsLogic
import com.atasoy.gallerypro.data.collections.DayGroup
import com.atasoy.gallerypro.data.collections.MediaType
import com.atasoy.gallerypro.data.collections.SectionId
import com.atasoy.gallerypro.data.collections.SectionOrder
import com.atasoy.gallerypro.data.collections.SectionSetting
import com.atasoy.gallerypro.data.collections.Summary
import com.atasoy.gallerypro.data.index.IndexScheduler
import com.atasoy.gallerypro.data.media.Formatters
import com.atasoy.gallerypro.data.media.LocalGalleryImageLoader
import com.atasoy.gallerypro.data.media.MediaDetailsReader
import com.atasoy.gallerypro.data.media.MediaItem
import com.atasoy.gallerypro.data.media.MediaThumb
import com.atasoy.gallerypro.data.media.contentUri
import com.atasoy.gallerypro.data.places.PlaceGroup
import com.atasoy.gallerypro.data.places.PlacesLogic
import com.atasoy.gallerypro.designsystem.GalleryColors
import com.atasoy.gallerypro.designsystem.GalleryDimens
import com.atasoy.gallerypro.designsystem.GalleryType
import com.atasoy.gallerypro.designsystem.GlassIconButton
import com.atasoy.gallerypro.designsystem.GlassTab
import com.atasoy.gallerypro.designsystem.GlassTabBar
import com.atasoy.gallerypro.designsystem.GlassTextButton
import com.atasoy.gallerypro.designsystem.GlassToast
import com.atasoy.gallerypro.designsystem.GlassTone
import com.atasoy.gallerypro.designsystem.Glyph
import com.atasoy.gallerypro.designsystem.ProgressiveBlurEdge
import com.atasoy.gallerypro.designsystem.glass
import com.atasoy.gallerypro.designsystem.icons.GalleryIcons
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.RoundedRectangle
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

/**
 * Koleksiyonlar sekmesi. Bölümler: Son Günler, Sabitlenenler, Albümler, Yerler,
 * Medya Türleri, Araçlar. "⋯" ile sıraları ve görünürlükleri değiştirilir.
 */
@Composable
fun CollectionsScreen(
    viewModel: CollectionsViewModel,
    onOpenLibrary: () -> Unit
) {
    val content by viewModel.content.collectAsStateWithLifecycle()
    val sections by viewModel.sections.collectAsStateWithLifecycle()
    val places by viewModel.places.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // Yerler için fotoğrafların konumu gerekir (Android konumu izinsiz gizler).
    var locationGranted by remember { mutableStateOf(MediaDetailsReader.hasLocationPermission(context)) }
    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        locationGranted = granted
        if (granted) IndexScheduler.runNow(context)
    }
    val locale = remember { Locale.getDefault() }
    val zone = remember { ZoneId.systemDefault() }
    val backdrop = rememberLayerBackdrop()
    val listState = rememberLazyListState()

    var toast by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(toast) {
        if (toast != null) {
            delay(2200)
            toast = null
        }
    }

    // Açık ekranlar yığını (ör. "albums" → "album:DCIM/Camera/"). Satır sonuyla ayrılır.
    var stack by rememberSaveable { mutableStateOf("") }
    val route = stack.split('\n').filter { it.isNotEmpty() }
    fun push(key: String) {
        stack = (route + key).joinToString("\n")
    }
    fun pop() {
        stack = route.dropLast(1).joinToString("\n")
    }
    var customizing by rememberSaveable { mutableStateOf(false) }

    val unlockTitle = stringResource(R.string.hidden_unlock_title)
    val unlockSubtitle = stringResource(R.string.hidden_unlock_subtitle)
    fun openHidden() {
        val activity = context.findActivity() ?: return
        DeviceUnlock.authenticate(activity, unlockTitle, unlockSubtitle) { ok ->
            if (ok) push(CollectionKeys.Hidden)
        }
    }

    BackHandler(enabled = route.isEmpty()) {
        if (customizing) customizing = false else onOpenLibrary()
    }

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val now = remember(content) { System.currentTimeMillis() }

    Box(
        Modifier
            .fillMaxSize()
            .background(GalleryColors.LightBackground)
    ) {
        // ---------------------------------------------------------------- içerik (camın arkası)
        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .background(GalleryColors.LightBackground)
        ) {
            if (content.loading) {
                CenterMessage(stringResource(R.string.library_loading))
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(top = topInset + 106.dp, bottom = bottomInset + 128.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    var first = true
                    for (setting in sections) {
                        if (!setting.visible) continue
                        val top = if (first) 0.dp else 26.dp
                        when (setting.id) {
                            SectionId.RecentDays -> if (content.recentDays.isNotEmpty()) {
                                first = false
                                item(key = setting.id.name) {
                                    RecentDaysSection(
                                        days = content.recentDays,
                                        now = now,
                                        zone = zone,
                                        locale = locale,
                                        topPadding = top,
                                        onOpenAll = { push(CollectionKeys.RecentDays) },
                                        onOpen = { push(CollectionKeys.day(it.epochDay)) }
                                    )
                                }
                            }
                            SectionId.Pinned -> {
                                first = false
                                item(key = setting.id.name) {
                                    PinnedSection(content = content, topPadding = top, onOpen = ::push)
                                }
                            }
                            SectionId.Albums -> if (content.albums.isNotEmpty()) {
                                first = false
                                item(key = setting.id.name) {
                                    AlbumsSection(
                                        albums = content.albums,
                                        locale = locale,
                                        topPadding = top,
                                        onOpenAll = { push(CollectionKeys.AllAlbums) },
                                        onOpen = { push(CollectionKeys.album(it.path)) }
                                    )
                                }
                            }
                            SectionId.Places -> if (places.isNotEmpty() || !locationGranted) {
                                first = false
                                item(key = setting.id.name) {
                                    PlacesSection(
                                        places = places,
                                        locale = locale,
                                        topPadding = top,
                                        showPermission = !locationGranted,
                                        onAllow = {
                                            runCatching { locationLauncher.launch(Manifest.permission.ACCESS_MEDIA_LOCATION) }
                                        },
                                        onOpen = { push(PlacesLogic.collectionKey(it.key)) }
                                    )
                                }
                            }
                            SectionId.MediaTypes -> if (content.mediaTypes.isNotEmpty()) {
                                first = false
                                item(key = setting.id.name) {
                                    MediaTypesSection(
                                        content = content,
                                        locale = locale,
                                        topPadding = if (top == 0.dp) 0.dp else 28.dp,
                                        onOpen = ::push
                                    )
                                }
                            }
                            SectionId.Utilities -> {
                                first = false
                                item(key = setting.id.name) {
                                    UtilitiesSection(
                                        content = content,
                                        locale = locale,
                                        topPadding = if (top == 0.dp) 0.dp else 28.dp,
                                        onOpenHidden = ::openHidden,
                                        onOpen = ::push
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ---------------------------------------------------------------- üst: başlık
        ProgressiveBlurEdge(
            backdrop = backdrop,
            height = topInset + 118.dp,
            tint = Color.White,
            modifier = Modifier.align(Alignment.TopCenter)
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = topInset + 18.dp, start = GalleryDimens.TitleStartPadding, end = GalleryDimens.ScreenPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicText(
                stringResource(R.string.collections_title),
                style = GalleryType.LargeTitle,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            GlassIconButton(
                backdrop = backdrop,
                icon = GalleryIcons.More,
                contentDescription = stringResource(R.string.collections_customize),
                onClick = { customizing = !customizing },
                tint = GalleryColors.TextPrimary
            )
        }

        // ---------------------------------------------------------------- alt: sekmeler
        ProgressiveBlurEdge(
            backdrop = backdrop,
            height = bottomInset + 150.dp,
            tint = Color.White,
            fromBottom = true,
            tintIntensity = 0.42f,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        val comingSearch = stringResource(R.string.coming_search)
        GlassTabBar(
            backdrop = backdrop,
            tabs = listOf(
                GlassTab(stringResource(R.string.tab_library), GalleryIcons.Library, selected = false, onClick = onOpenLibrary),
                GlassTab(stringResource(R.string.tab_collections), GalleryIcons.Collections, selected = true) {}
            ),
            searchLabel = stringResource(R.string.tab_search),
            onSearch = { toast = comingSearch },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomInset + 28.dp, start = GalleryDimens.ScreenPadding, end = GalleryDimens.ScreenPadding)
        )

        // ---------------------------------------------------------------- özelleştir
        if (customizing) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.18f))
                    .clickable(interactionSource = null, indication = null) { customizing = false }
            )
        }
        AnimatedVisibility(
            visible = customizing,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 12.dp, end = 12.dp, bottom = bottomInset + 20.dp)
        ) {
            CustomizeSheet(
                backdrop = backdrop,
                sections = sections,
                onChange = viewModel::setSections,
                onDone = { customizing = false }
            )
        }

        GlassToast(
            backdrop = backdrop,
            message = toast,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = topInset + 82.dp)
        )

        // ---------------------------------------------------------------- açılan ekranlar
        route.forEachIndexed { index, entry ->
            key(index, entry) {
                val appear = remember { MutableTransitionState(false).apply { targetState = true } }
                AnimatedVisibility(
                    visibleState = appear,
                    enter = slideInHorizontally { it / 3 } + fadeIn(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (entry == CollectionKeys.AllAlbums) {
                        AllAlbumsScreen(
                            albums = content.albums,
                            locale = locale,
                            onOpen = { push(CollectionKeys.album(it.path)) },
                            onBack = ::pop
                        )
                    } else {
                        CollectionDetailScreen(
                            viewModel = viewModel,
                            collectionKey = entry,
                            title = collectionTitle(entry, content, places, now, zone, locale),
                            onBack = ::pop
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------- bölümler

@Composable
private fun SectionHeader(title: String, topPadding: Dp, onClick: (() -> Unit)?) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(start = 20.dp, end = 20.dp, top = topPadding, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(title, style = GalleryType.SectionTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (onClick != null) {
            Spacer(Modifier.width(4.dp))
            Glyph(GalleryIcons.ChevronRight, GalleryColors.Chevron, size = 18.dp)
        }
    }
}

@Composable
private fun RecentDaysSection(
    days: List<DayGroup>,
    now: Long,
    zone: ZoneId,
    locale: Locale,
    topPadding: Dp,
    onOpenAll: () -> Unit,
    onOpen: (DayGroup) -> Unit
) {
    val context = LocalContext.current
    val today = stringResource(R.string.viewer_today)
    val yesterday = stringResource(R.string.viewer_yesterday)
    Column {
        SectionHeader(stringResource(R.string.section_recent_days), topPadding, onOpenAll)
        LazyRow(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(days, key = { it.epochDay }) { day ->
                DayCard(
                    cover = day.cover,
                    title = dayLabel(day.epochDay, now, zone, locale, today, yesterday),
                    count = context.resources.getQuantityString(R.plurals.items_count, day.count, Formatters.count(day.count, locale)),
                    onClick = { onOpen(day) }
                )
            }
        }
    }
}

@Composable
private fun DayCard(cover: MediaItem, title: String, count: String, onClick: () -> Unit) {
    val imageLoader = LocalGalleryImageLoader.current
    val shape = RoundedCornerShape(GalleryDimens.DayCardCorner)
    val uri = remember(cover.id, cover.isVideo) { cover.contentUri() }
    Box(
        Modifier
            .size(GalleryDimens.DayCardWidth, GalleryDimens.DayCardHeight)
            .shadow(10.dp, shape, ambientColor = Color(0x1F000000), spotColor = Color(0x1F000000))
            .clip(shape)
            .background(GalleryColors.CellPlaceholder)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "$title, $count" }
    ) {
        AsyncImage(
            model = MediaThumb(uri, 512, cover.modifiedMillis),
            contentDescription = null,
            imageLoader = imageLoader,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0f to Color.Transparent, 0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.5f)))
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 14.dp, end = 10.dp, bottom = 12.dp)
        ) {
            BasicText(
                title,
                style = GalleryType.SectionTitle.copy(color = Color.White, fontSize = 17.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            BasicText(count, style = GalleryType.Duration.copy(color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.Medium))
        }
    }
}

/** Fotoğrafların çekildiği iller/eyaletler; en son gidilen önce. İzin yoksa izin kartı. */
@Composable
private fun PlacesSection(
    places: List<PlaceGroup>,
    locale: Locale,
    topPadding: Dp,
    showPermission: Boolean,
    onAllow: () -> Unit,
    onOpen: (PlaceGroup) -> Unit
) {
    val context = LocalContext.current
    Column {
        SectionHeader(stringResource(R.string.section_places), topPadding, null)
        if (places.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(places, key = { it.key }) { place ->
                    DayCard(
                        cover = place.cover,
                        title = place.title,
                        count = context.resources.getQuantityString(
                            R.plurals.items_count,
                            place.count,
                            Formatters.count(place.count, locale)
                        ),
                        onClick = { onOpen(place) }
                    )
                }
            }
        }
        if (showPermission) {
            Row(
                Modifier
                    .padding(horizontal = 20.dp)
                    .padding(top = if (places.isEmpty()) 0.dp else 10.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(GalleryDimens.GroupedCorner))
                    .background(GalleryColors.GroupedFill)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Glyph(GalleryIcons.Pin, GalleryColors.Accent, size = 20.dp)
                Spacer(Modifier.width(12.dp))
                BasicText(
                    stringResource(R.string.places_permission),
                    style = GalleryType.Body.copy(fontSize = 14.sp, color = GalleryColors.TextSecondary),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(10.dp))
                BasicText(
                    stringResource(R.string.places_allow),
                    style = GalleryType.Button.copy(color = Color.White),
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(GalleryColors.Accent)
                        .clickable(role = Role.Button, onClick = onAllow)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

private data class PinnedTile(val key: String, val title: String, val icon: ImageVector, val summary: Summary)

@Composable
private fun PinnedSection(content: CollectionsContent, topPadding: Dp, onOpen: (String) -> Unit) {
    val tiles = listOf(
        PinnedTile(CollectionKeys.Favorites, stringResource(R.string.col_favorites), GalleryIcons.HeartFilled, content.favorites),
        PinnedTile(CollectionKeys.Videos, stringResource(R.string.col_videos), GalleryIcons.Video, content.videos),
        PinnedTile(CollectionKeys.RecentlySaved, stringResource(R.string.col_recently_saved), GalleryIcons.Download, content.recentlySaved)
    )
    Column {
        SectionHeader(stringResource(R.string.section_pinned), topPadding, null)
        Column(
            Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            tiles.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { tile ->
                        PinnedCard(tile, onClick = { onOpen(tile.key) }, modifier = Modifier.weight(1f))
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PinnedCard(tile: PinnedTile, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val imageLoader = LocalGalleryImageLoader.current
    val cover = tile.summary.cover
    Box(
        modifier
            .height(GalleryDimens.PinnedCardHeight)
            .clip(RoundedCornerShape(GalleryDimens.PinnedCardCorner))
            .background(if (cover == null) GalleryColors.PinnedEmpty else GalleryColors.CellPlaceholder)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = tile.title }
    ) {
        if (cover != null) {
            val uri = remember(cover.id, cover.isVideo) { cover.contentUri() }
            AsyncImage(
                model = MediaThumb(uri, 512, cover.modifiedMillis),
                contentDescription = null,
                imageLoader = imageLoader,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0f to Color.Transparent, 0.4f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.45f)))
        )
        Box(
            Modifier
                .padding(10.dp)
                .size(GalleryDimens.PinnedBadge)
                .clip(CircleShape)
                .background(GalleryColors.BadgeFill)
                .border(0.5.dp, Color.White.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Glyph(tile.icon, Color.White, size = 16.dp)
        }
        BasicText(
            tile.title,
            style = GalleryType.Button.copy(color = Color.White, fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, end = 8.dp, bottom = 10.dp)
        )
    }
}

@Composable
private fun AlbumsSection(
    albums: List<Album>,
    locale: Locale,
    topPadding: Dp,
    onOpenAll: () -> Unit,
    onOpen: (Album) -> Unit
) {
    Column {
        SectionHeader(
            stringResource(R.string.section_albums),
            topPadding,
            if (albums.size > AlbumPreviewCount) onOpenAll else null
        )
        Column(
            Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            albums.take(AlbumPreviewCount).chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { album ->
                        AlbumTile(album, locale, onClick = { onOpen(album) }, modifier = Modifier.weight(1f))
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

private const val AlbumPreviewCount = 4

@Composable
internal fun AlbumTile(album: Album, locale: Locale, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val imageLoader = LocalGalleryImageLoader.current
    val uri = remember(album.cover.id, album.cover.isVideo) { album.cover.contentUri() }
    Column(
        modifier
            .clip(RoundedCornerShape(GalleryDimens.AlbumCorner))
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(GalleryDimens.AlbumCorner))
                .background(GalleryColors.CellPlaceholder)
        ) {
            AsyncImage(
                model = MediaThumb(uri, 512, album.cover.modifiedMillis),
                contentDescription = null,
                imageLoader = imageLoader,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.height(7.dp))
        BasicText(
            album.name,
            style = GalleryType.Body.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold, lineHeight = 18.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        BasicText(
            Formatters.count(album.count, locale),
            style = GalleryType.Subtitle.copy(fontSize = 13.sp, color = GalleryColors.TextTertiary)
        )
    }
}

private data class RowSpec(val icon: ImageVector, val title: String, val count: String, val onClick: () -> Unit)

@Composable
private fun GroupedList(rows: List<RowSpec>) {
    Column(
        Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(GalleryDimens.GroupedCorner))
            .background(GalleryColors.GroupedFill)
    ) {
        rows.forEachIndexed { index, row ->
            if (index > 0) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(GalleryColors.GroupedDivider)
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(GalleryDimens.GroupedRowHeight)
                    .clickable(role = Role.Button, onClick = row.onClick)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.width(26.dp), contentAlignment = Alignment.Center) {
                    Glyph(row.icon, GalleryColors.Accent, size = 22.dp)
                }
                Spacer(Modifier.width(12.dp))
                BasicText(
                    row.title,
                    style = GalleryType.Body,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (row.count.isNotEmpty()) {
                    BasicText(row.count, style = GalleryType.Subtitle.copy(color = GalleryColors.TextTertiary))
                    Spacer(Modifier.width(12.dp))
                }
                Glyph(GalleryIcons.ChevronRight, GalleryColors.ChevronSmall, size = 14.dp)
            }
        }
    }
}

@Composable
private fun MediaTypesSection(content: CollectionsContent, locale: Locale, topPadding: Dp, onOpen: (String) -> Unit) {
    val rows = content.mediaTypes.map { entry ->
        RowSpec(
            icon = mediaTypeIcon(entry.type),
            title = mediaTypeTitle(entry.type),
            count = Formatters.count(entry.count, locale),
            onClick = { onOpen(entry.type.key) }
        )
    }
    Column {
        SectionHeader(stringResource(R.string.section_media_types), topPadding, null)
        GroupedList(rows)
    }
}

private fun mediaTypeIcon(type: MediaType): ImageVector = when (type) {
    MediaType.Videos -> GalleryIcons.Video
    MediaType.Panoramas -> GalleryIcons.Panorama
    MediaType.Screenshots -> GalleryIcons.Screenshot
    MediaType.Raw -> GalleryIcons.Raw
}

@Composable
private fun mediaTypeTitle(type: MediaType): String = stringResource(
    when (type) {
        MediaType.Videos -> R.string.col_videos
        MediaType.Panoramas -> R.string.col_panoramas
        MediaType.Screenshots -> R.string.col_screenshots
        MediaType.Raw -> R.string.col_raw
    }
)

@Composable
private fun UtilitiesSection(
    content: CollectionsContent,
    locale: Locale,
    topPadding: Dp,
    onOpenHidden: () -> Unit,
    onOpen: (String) -> Unit
) {
    val rows = listOf(
        RowSpec(GalleryIcons.Lock, stringResource(R.string.col_hidden), "", onOpenHidden),
        RowSpec(
            GalleryIcons.TrashOutline,
            stringResource(R.string.col_trash),
            if (content.trashCount > 0) Formatters.count(content.trashCount, locale) else ""
        ) { onOpen(CollectionKeys.Trash) },
        RowSpec(
            GalleryIcons.Duplicate,
            stringResource(R.string.col_duplicates),
            if (content.duplicateCount > 0) Formatters.count(content.duplicateCount, locale) else ""
        ) { onOpen(CollectionKeys.Duplicates) },
        RowSpec(
            GalleryIcons.Download,
            stringResource(R.string.col_imports),
            if (content.importCount > 0) Formatters.count(content.importCount, locale) else ""
        ) { onOpen(CollectionKeys.Imports) }
    )
    Column {
        SectionHeader(stringResource(R.string.section_utilities), topPadding, null)
        GroupedList(rows)
    }
}

// -------------------------------------------------------------------- özelleştir

@Composable
private fun sectionTitle(id: SectionId): String = stringResource(
    when (id) {
        SectionId.RecentDays -> R.string.section_recent_days
        SectionId.Pinned -> R.string.section_pinned
        SectionId.Albums -> R.string.section_albums
        SectionId.MediaTypes -> R.string.section_media_types
        SectionId.Utilities -> R.string.section_utilities
        SectionId.Places -> R.string.section_places
    }
)

/** Bölümleri göster/gizle ve tutamaçtan sürükleyerek sırala. */
@Composable
private fun CustomizeSheet(
    backdrop: Backdrop,
    sections: List<SectionSetting>,
    onChange: (List<SectionSetting>) -> Unit,
    onDone: () -> Unit
) {
    val density = LocalDensity.current
    val rowPx = with(density) { GalleryDimens.SectionRowHeight.toPx() }
    val latest by rememberUpdatedState(sections)
    val update by rememberUpdatedState(onChange)
    var dragging by remember { mutableStateOf<SectionId?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val reorderLabel = stringResource(R.string.collections_reorder)

    Column(
        Modifier
            .fillMaxWidth()
            .glass(backdrop, RoundedRectangle(30.dp), GlassTone.Light, refraction = false)
            .padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicText(
                stringResource(R.string.collections_customize_title),
                style = GalleryType.SectionTitle.copy(fontSize = 18.sp),
                modifier = Modifier.weight(1f)
            )
            GlassTextButton(backdrop = backdrop, text = stringResource(R.string.collections_done), onClick = onDone)
        }
        sections.forEach { setting ->
            key(setting.id) {
                val isDragging = dragging == setting.id
                val name = sectionTitle(setting.id)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(GalleryDimens.SectionRowHeight)
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer { translationY = if (isDragging) dragOffset else 0f }
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDragging) Color.White.copy(alpha = 0.7f) else Color.Transparent),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clickable(role = Role.Checkbox) { update(SectionOrder.toggle(latest, setting.id)) }
                            .semantics(mergeDescendants = true) { selected = setting.visible }
                            .padding(start = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (setting.visible) GalleryColors.Accent else Color.Transparent)
                                .border(1.5.dp, if (setting.visible) GalleryColors.Accent else GalleryColors.ChevronSmall, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (setting.visible) Glyph(GalleryIcons.Check, Color.White, size = 14.dp)
                        }
                        Spacer(Modifier.width(12.dp))
                        BasicText(name, style = GalleryType.Body)
                    }
                    Box(
                        Modifier
                            .size(GalleryDimens.SectionRowHeight)
                            .semantics { contentDescription = "$name, $reorderLabel" }
                            .pointerInput(setting.id) {
                                detectDragGestures(
                                    onDragStart = {
                                        dragging = setting.id
                                        dragOffset = 0f
                                    },
                                    onDragEnd = {
                                        dragging = null
                                        dragOffset = 0f
                                    },
                                    onDragCancel = {
                                        dragging = null
                                        dragOffset = 0f
                                    },
                                    onDrag = { pointer, amount ->
                                        pointer.consume()
                                        dragOffset += amount.y
                                        val list = latest
                                        val from = list.indexOfFirst { it.id == setting.id }
                                        if (from >= 0) {
                                            val target = (from + (dragOffset / rowPx).roundToInt()).coerceIn(0, list.lastIndex)
                                            if (target != from) {
                                                update(SectionOrder.move(list, from, target))
                                                dragOffset -= (target - from) * rowPx
                                            }
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Glyph(GalleryIcons.Grip, GalleryColors.TextTertiary, size = 22.dp)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------- yardımcılar

@Composable
internal fun CenterMessage(title: String, body: String? = null) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BasicText(title, style = GalleryType.SectionTitle)
        if (body != null) {
            Spacer(Modifier.height(8.dp))
            BasicText(body, style = GalleryType.Body.copy(color = GalleryColors.TextSecondary))
        }
    }
}

/** "Bugün", "Dün", son bir haftada gün adı ("Perşembe"), daha eskiyse "8 Kasım". */
internal fun dayLabel(epochDay: Long, now: Long, zone: ZoneId, locale: Locale, today: String, yesterday: String): String {
    val ago = CollectionsLogic.daysAgo(epochDay, now, zone)
    val date = LocalDate.ofEpochDay(epochDay)
    return when {
        ago == 0L -> today
        ago == 1L -> yesterday
        ago in 2L..6L -> date.dayOfWeek.getDisplayName(TextStyle.FULL_STANDALONE, locale)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
        else -> DateTimeFormatter.ofPattern("d MMMM", locale).format(date)
    }
}

@Composable
private fun collectionTitle(
    key: String,
    content: CollectionsContent,
    places: List<PlaceGroup>,
    now: Long,
    zone: ZoneId,
    locale: Locale
): String {
    PlacesLogic.groupOf(key)?.let { group ->
        return places.firstOrNull { it.key == group }?.title ?: group.substringBefore('|')
    }
    CollectionKeys.dayOf(key)?.let { day ->
        return dayLabel(day, now, zone, locale, stringResource(R.string.viewer_today), stringResource(R.string.viewer_yesterday))
    }
    CollectionKeys.albumOf(key)?.let { path ->
        return content.albums.firstOrNull { it.path == path }?.name ?: path.trimEnd('/').substringAfterLast('/')
    }
    return stringResource(
        when (key) {
            CollectionKeys.RecentDays -> R.string.section_recent_days
            CollectionKeys.Favorites -> R.string.col_favorites
            CollectionKeys.Videos -> R.string.col_videos
            CollectionKeys.RecentlySaved -> R.string.col_recently_saved
            CollectionKeys.Panoramas -> R.string.col_panoramas
            CollectionKeys.Screenshots -> R.string.col_screenshots
            CollectionKeys.Raw -> R.string.col_raw
            CollectionKeys.Hidden -> R.string.col_hidden
            CollectionKeys.Trash -> R.string.col_trash
            CollectionKeys.Duplicates -> R.string.col_duplicates
            CollectionKeys.Imports -> R.string.col_imports
            else -> R.string.section_albums
        }
    )
}

internal fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

internal val trashSupported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
