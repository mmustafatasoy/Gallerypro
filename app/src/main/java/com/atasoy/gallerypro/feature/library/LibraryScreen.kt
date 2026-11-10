package com.atasoy.gallerypro.feature.library

import android.app.Activity
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.atasoy.gallerypro.R
import com.atasoy.gallerypro.data.media.Formatters
import com.atasoy.gallerypro.data.media.LibraryFilter
import com.atasoy.gallerypro.data.media.ManageMedia
import com.atasoy.gallerypro.data.media.contentUri
import com.atasoy.gallerypro.data.media.shareItems
import com.atasoy.gallerypro.designsystem.GalleryColors
import com.atasoy.gallerypro.designsystem.GalleryDimens
import com.atasoy.gallerypro.designsystem.GalleryType
import com.atasoy.gallerypro.designsystem.GlassCapsule
import com.atasoy.gallerypro.designsystem.GlassIconButton
import com.atasoy.gallerypro.designsystem.GlassMenu
import com.atasoy.gallerypro.designsystem.GlassMenuItem
import com.atasoy.gallerypro.designsystem.GlassSegmented
import com.atasoy.gallerypro.designsystem.GlassTab
import com.atasoy.gallerypro.designsystem.GlassTabBar
import com.atasoy.gallerypro.designsystem.GlassTextButton
import com.atasoy.gallerypro.designsystem.GlassToast
import com.atasoy.gallerypro.designsystem.ProgressiveBlurEdge
import com.atasoy.gallerypro.designsystem.icons.GalleryIcons
import com.atasoy.gallerypro.feature.settings.SettingsScreen
import com.atasoy.gallerypro.feature.viewer.DeleteCard
import com.atasoy.gallerypro.feature.viewer.ViewerScreen
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    partialAccess: Boolean,
    onRequestMoreAccess: () -> Unit,
    onOpenCollections: () -> Unit
) {
    val content by viewModel.content.collectAsStateWithLifecycle()
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val locale = remember { Locale.getDefault() }
    val zone = remember { ZoneId.systemDefault() }
    val backdrop = rememberLayerBackdrop()

    val gridState = rememberLazyGridState()
    val monthsState = rememberLazyListState()
    val yearsState = rememberLazyListState()

    var toast by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(toast) {
        if (toast != null) {
            delay(2200)
            toast = null
        }
    }

    // Açık görüntüleyicinin başladığı öğe. Kapanınca ızgara son bakılan öğeyi gösterir.
    var viewerId by rememberSaveable { mutableStateOf<Long?>(null) }
    var revealId by remember { mutableStateOf<Long?>(null) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var manageGranted by remember { mutableStateOf(false) }
    LaunchedEffect(revealId) {
        val id = revealId ?: return@LaunchedEffect
        val index = content.items.indexOfFirst { it.id == id }
        if (index >= 0 && ui.mode == LibraryMode.All) {
            val leading = leadingSpacers(content.items.size, ui.columns)
            val target = leading + index
            val visible = gridState.layoutInfo.visibleItemsInfo
            // İlk ve son görünen satırlar yarım olabilir; öğe arada değilse kaydır.
            val shown = visible.size > 2 * ui.columns &&
                visible[ui.columns].index <= target && visible[visible.size - 1 - ui.columns].index >= target
            if (!shown) gridState.scrollToItem((target - ui.columns * 2).coerceAtLeast(0))
        }
        revealId = null
    }

    // Ay kartına dokununca Tümü ızgarasında o aya git.
    var pendingGridIndex by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(pendingGridIndex, ui.mode) {
        val target = pendingGridIndex
        if (target != null && ui.mode == LibraryMode.All) {
            val leading = leadingSpacers(content.items.size, ui.columns)
            gridState.scrollToItem(leading + target)
            pendingGridIndex = null
        }
    }
    var pendingMonthKey by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(pendingMonthKey, ui.mode) {
        val key = pendingMonthKey
        if (key != null && ui.mode == LibraryMode.Months) {
            val reversedIndex = content.months.asReversed().indexOfFirst { it.key == key }
            if (reversedIndex >= 0) monthsState.scrollToItem(reversedIndex)
            pendingMonthKey = null
        }
    }

    // En yeni fotoğraf en altta. Kullanıcı en alttaysa yeni öğe gelince de orada kal;
    // yukarı kaydırdıysa yerine dokunma.
    var followBottom by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.isScrollInProgress to gridState.canScrollForward }
            .collect { (scrolling, canScrollForward) ->
                if (scrolling) followBottom = !canScrollForward
            }
    }
    LaunchedEffect(content.items.size, ui.columns, ui.mode) {
        if (followBottom && pendingGridIndex == null && content.items.isNotEmpty() && ui.mode == LibraryMode.All) {
            val leading = leadingSpacers(content.items.size, ui.columns)
            gridState.scrollToItem(leading + content.items.lastIndex)
        }
    }

    val trashLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val n = ui.selected.size
            toast = context.resources.getQuantityString(R.plurals.items_trashed, n, Formatters.count(n, locale))
            viewModel.stopSelecting()
        }
    }

    BackHandler(enabled = confirmDelete) { confirmDelete = false }
    BackHandler(enabled = viewerId == null && !settingsOpen && !confirmDelete && (ui.selecting || ui.menuOpen || ui.mode != LibraryMode.All)) {
        when {
            ui.menuOpen -> viewModel.closeMenu()
            ui.selecting -> viewModel.stopSelecting()
            else -> viewModel.setMode(LibraryMode.All)
        }
    }

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bannerSpace = if (partialAccess) 52.dp else 0.dp
    // Son satır cam kontrollerin altına kadar uzanır: cam her zaman fotoğrafın üstünde durur.
    val listPadding = PaddingValues(
        top = topInset + 112.dp + bannerSpace,
        bottom = bottomInset + 104.dp
    )
    // Kartlarda sayaç çipi cam kontrollerin altında kalmasın.
    val cardPadding = PaddingValues(
        top = topInset + 112.dp + bannerSpace,
        bottom = bottomInset + 176.dp
    )

    // Başlığın altındaki tarih: ekranda görünen ilk satırın tarihi.
    val visibleDate by remember(content.items, ui.columns) {
        derivedStateOf {
            val items = content.items
            if (items.isEmpty()) {
                null
            } else {
                val leading = leadingSpacers(items.size, ui.columns)
                val index = (gridState.firstVisibleItemIndex - leading + ui.columns).coerceIn(0, items.lastIndex)
                items[index].takenAtMillis
            }
        }
    }

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
            when {
                content.loading -> CenterMessage(stringResource(R.string.library_loading), null)
                content.items.isEmpty() -> CenterMessage(
                    stringResource(R.string.library_empty_title),
                    stringResource(R.string.library_empty_body)
                )
                else -> AnimatedContent(
                    targetState = ui.mode,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "libraryMode"
                ) { mode ->
                    when (mode) {
                        LibraryMode.All -> LibraryGrid(
                            items = content.items,
                            columns = ui.columns,
                            state = gridState,
                            contentPadding = listPadding,
                            selecting = ui.selecting,
                            selected = ui.selected,
                            onOpen = { viewerId = it.id },
                            onToggle = { viewModel.toggle(it.id) },
                            onLongPress = {
                                if (ui.selecting) viewModel.toggle(it.id) else viewModel.startSelecting(it.id)
                            },
                            onZoomIn = viewModel::zoomIn,
                            onZoomOut = viewModel::zoomOut
                        )

                        LibraryMode.Months -> PeriodList(
                            periods = content.months,
                            isYears = false,
                            state = monthsState,
                            contentPadding = cardPadding,
                            onOpen = { period ->
                                pendingGridIndex = period.firstIndex
                                viewModel.setMode(LibraryMode.All)
                            }
                        )

                        LibraryMode.Years -> PeriodList(
                            periods = content.years,
                            isYears = true,
                            state = yearsState,
                            contentPadding = cardPadding,
                            onOpen = { period ->
                                pendingMonthKey = content.months.lastOrNull { it.year == period.year }?.key
                                viewModel.setMode(LibraryMode.Months)
                            }
                        )
                    }
                }
            }
        }

        // ---------------------------------------------------------------- üst: kademeli bulanıklık + başlık
        ProgressiveBlurEdge(
            backdrop = backdrop,
            height = topInset + GalleryDimens.HeaderHeight - 24.dp,
            tint = Color.White,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        val subtitle = when {
            ui.selecting -> if (ui.selected.isEmpty()) {
                stringResource(R.string.library_select_items)
            } else {
                context.resources.getQuantityString(
                    R.plurals.items_selected,
                    ui.selected.size,
                    Formatters.count(ui.selected.size, locale)
                )
            }
            content.items.isEmpty() -> ""
            ui.mode == LibraryMode.All -> {
                val date = visibleDate?.let { Formatters.longDate(it, zone, locale) }.orEmpty()
                val count = context.resources.getQuantityString(
                    R.plurals.items_count,
                    content.items.size,
                    Formatters.count(content.items.size, locale)
                )
                stringResource(R.string.library_subtitle, date, count)
            }
            else -> {
                val first = Instant.ofEpochMilli(content.items.first().takenAtMillis).atZone(zone).year
                val last = Instant.ofEpochMilli(content.items.last().takenAtMillis).atZone(zone).year
                if (first == last) first.toString() else stringResource(R.string.library_years_range, first, last)
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = topInset + 12.dp, start = GalleryDimens.TitleStartPadding, end = GalleryDimens.ScreenPadding),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(Modifier.weight(1f)) {
                BasicText(stringResource(R.string.library_title), style = GalleryType.LargeTitle, maxLines = 1)
                BasicText(
                    subtitle,
                    style = GalleryType.Subtitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                GlassTextButton(
                    backdrop = backdrop,
                    text = stringResource(if (ui.selecting) R.string.library_cancel else R.string.library_select),
                    onClick = { if (ui.selecting) viewModel.stopSelecting() else viewModel.startSelecting() }
                )
                GlassIconButton(
                    backdrop = backdrop,
                    icon = GalleryIcons.More,
                    contentDescription = stringResource(R.string.library_more),
                    onClick = viewModel::toggleMenu,
                    tint = GalleryColors.TextPrimary
                )
            }
        }

        if (partialAccess) {
            GlassCapsule(
                backdrop = backdrop,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = topInset + 104.dp, start = GalleryDimens.ScreenPadding, end = GalleryDimens.ScreenPadding)
                    .fillMaxWidth()
                    .height(40.dp)
            ) {
                BasicText(
                    stringResource(R.string.partial_access_text),
                    style = GalleryType.Chip.copy(color = GalleryColors.TextSecondary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                )
                BasicText(
                    stringResource(R.string.partial_access_action),
                    style = GalleryType.Chip.copy(color = GalleryColors.Accent),
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClick = onRequestMoreAccess)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }

        // ---------------------------------------------------------------- alt: geçiş + kontroller
        ProgressiveBlurEdge(
            backdrop = backdrop,
            height = bottomInset + GalleryDimens.BottomFadeHeight,
            tint = Color.White,
            fromBottom = true,
            tintIntensity = 0.42f,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        AnimatedVisibility(
            visible = !ui.selecting,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = bottomInset + 28.dp)
            ) {
                GlassSegmented(
                    backdrop = backdrop,
                    options = listOf(
                        stringResource(R.string.library_years),
                        stringResource(R.string.library_months),
                        stringResource(R.string.library_all)
                    ),
                    selectedIndex = ui.mode.ordinal,
                    onSelect = { viewModel.setMode(LibraryMode.entries[it]) }
                )
                Spacer(Modifier.height(14.dp))
                val comingSearch = stringResource(R.string.coming_search)
                GlassTabBar(
                    backdrop = backdrop,
                    tabs = listOf(
                        GlassTab(stringResource(R.string.tab_library), GalleryIcons.Library, selected = true) {},
                        GlassTab(
                            stringResource(R.string.tab_collections),
                            GalleryIcons.Collections,
                            selected = false,
                            onClick = onOpenCollections
                        )
                    ),
                    searchLabel = stringResource(R.string.tab_search),
                    onSearch = { toast = comingSearch },
                    modifier = Modifier.padding(horizontal = GalleryDimens.ScreenPadding)
                )
            }
        }

        AnimatedVisibility(
            visible = ui.selecting,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val needsAndroid11 = stringResource(R.string.trash_needs_android11)
            val shareTitle = stringResource(R.string.share_chooser)
            Row(
                modifier = Modifier
                    .padding(bottom = bottomInset + 28.dp, start = GalleryDimens.ScreenPadding, end = GalleryDimens.ScreenPadding)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassIconButton(
                    backdrop = backdrop,
                    icon = GalleryIcons.Share,
                    contentDescription = stringResource(R.string.action_share),
                    onClick = {
                        val chosen = content.items.filter { it.id in ui.selected }
                        if (chosen.isNotEmpty()) context.shareItems(chosen, shareTitle)
                    },
                    size = GalleryDimens.ToolbarButton,
                    iconSize = 24.dp,
                    tint = GalleryColors.Accent
                )
                GlassCapsule(
                    backdrop = backdrop,
                    modifier = Modifier
                        .weight(1f)
                        .height(GalleryDimens.ToolbarButton)
                ) {
                    BasicText(
                        text = if (ui.selected.isEmpty()) {
                            stringResource(R.string.library_select_items)
                        } else {
                            context.resources.getQuantityString(
                                R.plurals.items_selected,
                                ui.selected.size,
                                Formatters.count(ui.selected.size, locale)
                            )
                        },
                        style = GalleryType.Button.copy(color = GalleryColors.TextPrimary)
                    )
                }
                GlassIconButton(
                    backdrop = backdrop,
                    icon = GalleryIcons.Trash,
                    contentDescription = stringResource(R.string.action_delete),
                    onClick = {
                        if (ui.selected.isNotEmpty()) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                manageGranted = ManageMedia.granted(context)
                                confirmDelete = true
                            } else {
                                toast = needsAndroid11
                            }
                        }
                    },
                    size = GalleryDimens.ToolbarButton,
                    iconSize = 24.dp,
                    tint = GalleryColors.Delete
                )
            }
        }

        // ---------------------------------------------------------------- menü ve bildirim
        GlassMenu(
            backdrop = backdrop,
            visible = ui.menuOpen,
            onDismiss = viewModel::closeMenu,
            items = listOf(
                GlassMenuItem(stringResource(R.string.filter_all), ui.filter == LibraryFilter.All) {
                    viewModel.setFilter(LibraryFilter.All)
                },
                GlassMenuItem(stringResource(R.string.filter_favorites), ui.filter == LibraryFilter.Favorites) {
                    viewModel.setFilter(LibraryFilter.Favorites)
                },
                GlassMenuItem(stringResource(R.string.filter_photos), ui.filter == LibraryFilter.Photos) {
                    viewModel.setFilter(LibraryFilter.Photos)
                },
                GlassMenuItem(stringResource(R.string.filter_videos), ui.filter == LibraryFilter.Videos) {
                    viewModel.setFilter(LibraryFilter.Videos)
                },
                GlassMenuItem(stringResource(R.string.filter_edited), ui.filter == LibraryFilter.Edited) {
                    viewModel.setFilter(LibraryFilter.Edited)
                },
                GlassMenuItem(stringResource(R.string.settings_title), false) {
                    viewModel.closeMenu()
                    settingsOpen = true
                }
            ),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = topInset + 64.dp, end = GalleryDimens.ScreenPadding)
        )

        GlassToast(
            backdrop = backdrop,
            message = toast,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = topInset + 118.dp + bannerSpace)
        )

        // ---------------------------------------------------------------- görüntüleyici
        AnimatedVisibility(
            visible = viewerId != null && content.items.isNotEmpty(),
            enter = fadeIn() + scaleIn(initialScale = 0.94f),
            exit = fadeOut() + scaleOut(targetScale = 0.94f),
            modifier = Modifier.fillMaxSize()
        ) {
            val startId = remember { viewerId ?: content.items.last().id }
            ViewerScreen(
                items = content.items,
                startId = startId,
                onClose = { lastId ->
                    viewerId = null
                    revealId = lastId
                }
            )
        }

        // ---------------------------------------------------------------- silme onayı (seçilenler)
        AnimatedVisibility(
            visible = confirmDelete,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(interactionSource = null, indication = null) { confirmDelete = false }
            )
        }
        AnimatedVisibility(
            visible = confirmDelete,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 12.dp, end = 12.dp, bottom = 28.dp)
        ) {
            val count = ui.selected.size
            val res = context.resources
            DeleteCard(
                message = res.getQuantityString(R.plurals.delete_selected_message, count.coerceAtLeast(1), count),
                action = res.getQuantityString(R.plurals.delete_selected_action, count.coerceAtLeast(1), count),
                backdrop = backdrop,
                showAccessHint = !manageGranted && ManageMedia.supported,
                onGrantAccess = {
                    confirmDelete = false
                    ManageMedia.request(context)
                },
                onConfirm = {
                    confirmDelete = false
                    val chosen = content.items.filter { it.id in ui.selected }
                    if (chosen.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val pending = MediaStore.createTrashRequest(context.contentResolver, chosen.map { it.contentUri() }, true)
                        trashLauncher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
                    }
                },
                onCancel = { confirmDelete = false }
            )
        }

        // ---------------------------------------------------------------- ayarlar
        AnimatedVisibility(
            visible = settingsOpen,
            enter = fadeIn() + scaleIn(initialScale = 0.96f),
            exit = fadeOut() + scaleOut(targetScale = 0.96f),
            modifier = Modifier.fillMaxSize()
        ) {
            SettingsScreen(onClose = { settingsOpen = false })
        }
    }
}

@Composable
private fun CenterMessage(title: String, body: String?) {
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
