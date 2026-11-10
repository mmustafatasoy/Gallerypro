package com.atasoy.gallerypro.feature.viewer

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil3.compose.AsyncImage
import com.atasoy.gallerypro.R
import com.atasoy.gallerypro.data.LocalGalleryStores
import com.atasoy.gallerypro.data.clip.VectorEntry
import com.atasoy.gallerypro.data.clip.VectorMath
import com.atasoy.gallerypro.data.collections.CollectionsLogic
import com.atasoy.gallerypro.data.index.IndexEntry
import com.atasoy.gallerypro.data.index.IndexScheduler
import com.atasoy.gallerypro.data.media.Formatters
import com.atasoy.gallerypro.data.media.LocalGalleryImageLoader
import com.atasoy.gallerypro.data.media.MediaDetails
import com.atasoy.gallerypro.data.media.MediaDetailsReader
import com.atasoy.gallerypro.data.media.MediaItem
import com.atasoy.gallerypro.data.media.MediaThumb
import com.atasoy.gallerypro.data.media.contentUri
import com.atasoy.gallerypro.data.media.copyToClipboard
import com.atasoy.gallerypro.data.media.openMap
import com.atasoy.gallerypro.data.media.openWith
import com.atasoy.gallerypro.data.media.setAs
import com.atasoy.gallerypro.data.media.shareItems
import com.atasoy.gallerypro.data.places.PlaceEntry
import com.atasoy.gallerypro.designsystem.GalleryColors
import com.atasoy.gallerypro.designsystem.GalleryDimens
import com.atasoy.gallerypro.designsystem.GalleryMotion
import com.atasoy.gallerypro.designsystem.GalleryType
import com.atasoy.gallerypro.designsystem.GlassCapsule
import com.atasoy.gallerypro.designsystem.GlassIconButton
import com.atasoy.gallerypro.designsystem.GlassMenu
import com.atasoy.gallerypro.designsystem.GlassMenuItem
import com.atasoy.gallerypro.designsystem.GlassTextButton
import com.atasoy.gallerypro.designsystem.GlassToast
import com.atasoy.gallerypro.designsystem.GlassTone
import com.atasoy.gallerypro.designsystem.Glyph
import com.atasoy.gallerypro.designsystem.LocalSearchLauncher
import com.atasoy.gallerypro.designsystem.glass
import com.atasoy.gallerypro.designsystem.icons.GalleryIcons
import com.atasoy.gallerypro.feature.edit.EditScreen
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.RoundedRectangle
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Bilgi panelinin ekran yüksekliğine oranı (tasarım: 563 / 915). */
private const val InfoPanelFraction = 0.62f

private enum class DragTarget { None, Info, Dismiss }

/** Görüntüleyicinin nereden açıldığı: alt araç çubuğu ve menü buna göre değişir. */
enum class ViewerMode { Normal, Hidden, Trash }

/**
 * Tam ekran fotoğraf/video görüntüleyici (2. aşama).
 *
 * Koyu zemin + fotoğrafın bulanık kopyası (Android 12+), üstte cam geri / tarih / ⋯,
 * altta küçük resim şeridi ve cam araç çubuğu. Yukarı çekince Bilgi paneli,
 * aşağı çekince kapanır. [items] Kütüphanedeki sırayla aynıdır (en eski başta).
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun ViewerScreen(
    items: List<MediaItem>,
    startId: Long,
    onClose: (lastId: Long?) -> Unit,
    mode: ViewerMode = ViewerMode.Normal
) {
    val context = LocalContext.current
    val stores = LocalGalleryStores.current
    val captions = stores.captions
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val locale = remember { Locale.getDefault() }
    val zone = remember { ZoneId.systemDefault() }
    val backdrop = rememberLayerBackdrop()
    val imageLoader = LocalGalleryImageLoader.current

    val itemsState = rememberUpdatedState(items)
    val close by rememberUpdatedState(onClose)
    val startIndex = remember { items.indexOfFirst { it.id == startId }.coerceAtLeast(0) }
    val pagerState = rememberPagerState(initialPage = startIndex) { itemsState.value.size }
    var currentId by rememberSaveable { mutableLongStateOf(startId) }

    var chromeVisible by rememberSaveable { mutableStateOf(true) }
    var menuOpen by remember { mutableStateOf(false) }
    var slideshow by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var zoomed by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }
    // Açık video oynatıcı (3. aşama) ve onun kontrollerinin görünürlüğü.
    var playingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var videoChrome by remember { mutableStateOf(true) }
    // Açık düzenleme ekranı (4. aşama).
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }

    var infoProgress by remember { mutableFloatStateOf(0f) }
    var dismissOffset by remember { mutableFloatStateOf(0f) }
    var dragTarget by remember { mutableStateOf(DragTarget.None) }
    var motionJob by remember { mutableStateOf<Job?>(null) }

    val current = items.getOrNull(pagerState.currentPage)

    // ---------------------------------------------------------------- liste değişince (silme, filtre)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            itemsState.value.getOrNull(page)?.let { currentId = it.id }
        }
    }
    LaunchedEffect(items) {
        if (items.isEmpty()) {
            close(null)
            return@LaunchedEffect
        }
        val index = items.indexOfFirst { it.id == currentId }
        if (index >= 0) {
            if (index != pagerState.currentPage) pagerState.scrollToPage(index)
        } else {
            // Gösterilen öğe silindi: aynı yerdeki (bir sonraki) öğeye geç.
            val page = pagerState.currentPage.coerceIn(0, items.lastIndex)
            pagerState.scrollToPage(page)
            currentId = items[page].id
        }
    }

    LaunchedEffect(toast) {
        if (toast != null) {
            delay(2200)
            toast = null
        }
    }

    LaunchedEffect(slideshow) {
        while (slideshow) {
            delay(3000)
            val next = pagerState.currentPage + 1
            if (next < itemsState.value.size) pagerState.animateScrollToPage(next) else slideshow = false
        }
    }

    // ---------------------------------------------------------------- sistem çubukları
    ViewerSystemBars(
        hidden = when {
            editingId != null -> false
            playingId != null -> !videoChrome
            else -> !chromeVisible || slideshow
        }
    )

    // Gösterilen öğe değişirse (silme, şerit) açık video kapanır.
    LaunchedEffect(current?.id) {
        if (playingId != null && playingId != current?.id) playingId = null
    }

    // ---------------------------------------------------------------- işlemler
    val trashedText = stringResource(R.string.toast_trashed)
    val noAppText = stringResource(R.string.toast_no_app)
    val trashLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) toast = trashedText
    }
    val favoriteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { }
    var locationGranted by remember { mutableStateOf(MediaDetailsReader.hasLocationPermission(context)) }
    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        locationGranted = granted
        // İzin verilince fotoğrafların yerleri (şehir adları) arka planda okunur.
        if (granted) IndexScheduler.runNow(context)
    }

    val needsAndroid11 = stringResource(R.string.trash_needs_android11)
    val favoriteNeedsAndroid11 = stringResource(R.string.favorite_needs_android11)

    fun launchTrash(item: MediaItem) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val request = MediaStore.createTrashRequest(context.contentResolver, listOf(item.contentUri()), true)
            trashLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
        } else {
            toast = needsAndroid11
        }
    }

    fun askDelete(item: MediaItem) {
        // "Medya yönetimi" izni varsa Android ayrıca sormaz; o zaman cam onay kartı gösterilir.
        // Yoksa Android'in kendi onay penceresi tek onay olur.
        val canManage = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && MediaStore.canManageMedia(context)
        if (canManage) confirmDelete = true else launchTrash(item)
    }

    val restoredText = stringResource(R.string.toast_recovered)
    val deletedForeverText = stringResource(R.string.toast_deleted_forever)
    val hiddenText = stringResource(R.string.toast_hidden)
    val unhiddenText = stringResource(R.string.toast_unhidden)
    val videoEditText = stringResource(R.string.coming_video_edit)
    val recoverLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) toast = restoredText
    }
    val deleteForeverLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) toast = deletedForeverText
    }

    fun recover(item: MediaItem) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val request = MediaStore.createTrashRequest(context.contentResolver, listOf(item.contentUri()), false)
            recoverLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
        }
    }

    fun deleteForever(item: MediaItem) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val request = MediaStore.createDeleteRequest(context.contentResolver, listOf(item.contentUri()))
            deleteForeverLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
        }
    }

    fun edit(item: MediaItem) {
        if (item.isVideo) {
            toast = videoEditText
        } else {
            playingId = null
            editingId = item.id
        }
    }

    fun toggleFavorite(item: MediaItem) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val request = MediaStore.createFavoriteRequest(
                context.contentResolver,
                listOf(item.contentUri()),
                !item.isFavorite
            )
            favoriteLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
        } else {
            toast = favoriteNeedsAndroid11
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val heightPx = with(density) { maxHeight.toPx() }
        val panelHeight = maxHeight * InfoPanelFraction
        val panelHeightPx = heightPx * InfoPanelFraction
        val topInset = WindowInsets.statusBarsIgnoringVisibility.asPaddingValues().calculateTopPadding()
        val bottomInset = WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues().calculateBottomPadding()

        fun animateInfo(target: Float, velocity: Float = 0f) {
            motionJob?.cancel()
            motionJob = scope.launch {
                animate(infoProgress, target, velocity / panelHeightPx * -1f, GalleryMotion.standard()) { v, _ ->
                    infoProgress = v.coerceIn(0f, 1f)
                }
            }
        }

        fun animateDismissBack() {
            motionJob?.cancel()
            motionJob = scope.launch {
                animate(dismissOffset, 0f, animationSpec = GalleryMotion.standard()) { v, _ -> dismissOffset = v }
            }
        }

        fun onVerticalDrag(dy: Float) {
            if (dragTarget == DragTarget.None) {
                motionJob?.cancel()
                dragTarget = if (infoProgress > 0f || dy < 0f) DragTarget.Info else DragTarget.Dismiss
            }
            when (dragTarget) {
                DragTarget.Info -> infoProgress = (infoProgress - dy / panelHeightPx).coerceIn(0f, 1f)
                DragTarget.Dismiss -> dismissOffset = max(0f, dismissOffset + dy)
                DragTarget.None -> Unit
            }
        }

        fun onVerticalDragEnd(velocityY: Float) {
            val target = dragTarget
            dragTarget = DragTarget.None
            when (target) {
                DragTarget.Info -> {
                    val open = when {
                        velocityY < -700f -> true
                        velocityY > 700f -> false
                        else -> infoProgress > 0.5f
                    }
                    animateInfo(if (open) 1f else 0f, velocityY)
                }
                DragTarget.Dismiss -> {
                    if (dismissOffset > heightPx * 0.12f || velocityY > 1400f) {
                        close(currentId)
                    } else {
                        animateDismissBack()
                    }
                }
                DragTarget.None -> Unit
            }
        }

        BackHandler(enabled = editingId == null) {
            when {
                confirmDelete -> confirmDelete = false
                menuOpen -> menuOpen = false
                slideshow -> slideshow = false
                infoProgress > 0f -> animateInfo(0f)
                else -> close(currentId)
            }
        }

        // Sürükleme sırasında her karede değişen değerler sadece çizim aşamasında okunur.
        val chromeTarget = if (chromeVisible && !slideshow) 1f else 0f
        val chromeAnim = animateFloatAsState(chromeTarget, label = "chrome")
        val chromeAlpha: () -> Float = {
            chromeAnim.value * (1f - min(1f, dismissOffset / (heightPx * 0.08f)))
        }
        val chromeShown by remember(heightPx) { derivedStateOf { chromeAlpha() > 0.01f } }
        val pagerScrollEnabled by remember {
            derivedStateOf { !zoomed && infoProgress == 0f && dismissOffset == 0f }
        }
        val infoVisible by remember { derivedStateOf { infoProgress > 0f } }
        val infoOpen by remember { derivedStateOf { infoProgress > 0.5f } }

        // ---------------------------------------------------------------- camın arkası: zemin + fotoğraflar
        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 1f - min(1f, dismissOffset / (heightPx * 0.5f)) }
                    .background(Color.Black)
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && current != null) {
                    AsyncImage(
                        model = MediaThumb(current.contentUri(), 128, current.modifiedMillis),
                        contentDescription = null,
                        imageLoader = imageLoader,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = 1.15f
                                scaleY = 1.15f
                            }
                            .blur(60.dp)
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.38f))
                    )
                }
            }

            val playLabel = stringResource(R.string.viewer_play)
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                pageSpacing = 16.dp,
                userScrollEnabled = pagerScrollEnabled,
                key = { index -> itemsState.value.getOrNull(index)?.id ?: index.toLong() },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val s = 1f - 0.25f * min(1f, dismissOffset / heightPx)
                        translationY = dismissOffset
                        scaleX = s
                        scaleY = s
                    }
            ) { page ->
                val item = itemsState.value.getOrNull(page) ?: return@HorizontalPager
                ViewerPage(
                    item = item,
                    isCurrent = page == pagerState.currentPage,
                    infoProgress = { infoProgress },
                    infoPanelHeightPx = panelHeightPx,
                    playLabel = playLabel,
                    onTap = {
                        when {
                            slideshow -> slideshow = false
                            infoProgress > 0f -> animateInfo(0f)
                            else -> chromeVisible = !chromeVisible
                        }
                    },
                    onVerticalDrag = ::onVerticalDrag,
                    onVerticalDragEnd = ::onVerticalDragEnd,
                    onZoomChanged = { zoomed = it },
                    onPlayVideo = {
                        videoChrome = true
                        playingId = item.id
                    }
                )
            }
        }

        // ---------------------------------------------------------------- Bilgi paneli
        if (infoVisible && current != null) {
            val details by produceState<MediaDetails?>(null, current.id, locationGranted) {
                value = withContext(Dispatchers.IO) { MediaDetailsReader.read(context, current) }
            }
            // Akıllı Etiketler ve Benzer Fotoğraflar: cihaz içi dizinden (yalnızca normal görünümde).
            val smart by produceState<IndexEntry?>(null, current.id, mode) {
                if (mode != ViewerMode.Normal) return@produceState
                stores.index.entries.map { it[current.id] }.distinctUntilChanged().collect { value = it }
            }
            val similar by produceState(emptyList<MediaItem>(), current.id, items, mode) {
                if (mode != ViewerMode.Normal) return@produceState
                val list = items
                stores.index.vectors.collectLatest { vectors ->
                    value = withContext(Dispatchers.Default) { similarTo(current, list, vectors) }
                }
            }
            val place by produceState<PlaceEntry?>(null, current.id) {
                stores.index.places.map { it[current.id] }.distinctUntilChanged().collect { value = it }
            }
            val searchLauncher = LocalSearchLauncher.current
            // Metin alanı yerel durumla çalışır (yazarken imleç kaymasın); her değişiklik kaydedilir.
            var caption by remember(current.id) { mutableStateOf(captions.caption(current.id)) }
            // Panelin alt köşeleri ekranın dışında kalsın diye köşe kadar aşağı taşar.
            val bleed = GalleryDimens.SheetCorner
            val bleedPx = with(density) { bleed.toPx() }
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .offset { IntOffset(0, (bleedPx + (1f - infoProgress) * (panelHeightPx + bleedPx)).roundToInt()) }
                    .fillMaxWidth()
                    .height(panelHeight + bleed)
                    .glass(backdrop, RoundedRectangle(GalleryDimens.SheetCorner), GlassTone.Sheet, refraction = false)
            ) {
                InfoPanelContent(
                    item = current,
                    details = details,
                    caption = caption,
                    onCaptionChange = {
                        caption = it
                        captions.set(current.id, it)
                    },
                    onRequestLocation = { locationLauncher.launch(Manifest.permission.ACCESS_MEDIA_LOCATION) },
                    onOpenMap = { lat, lon ->
                        if (!context.openMap(lat, lon, current.displayName.orEmpty())) toast = noAppText
                    },
                    place = place,
                    smart = smart,
                    similar = similar,
                    onOpenSimilar = { other ->
                        val index = itemsState.value.indexOfFirst { it.id == other.id }
                        if (index >= 0) {
                            animateInfo(0f)
                            scope.launch { pagerState.scrollToPage(index) }
                        }
                    },
                    onSearchLabel = searchLauncher?.let { launcher -> { word: String -> launcher.open(word) } },
                    onHandleDrag = ::onVerticalDrag,
                    onHandleDragEnd = ::onVerticalDragEnd,
                    bottomPadding = bleed + 56.dp + 32.dp + bottomInset + 16.dp
                )
            }
        }

        // ---------------------------------------------------------------- üst çubuk
        if (chromeShown && current != null) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = topInset + 8.dp, start = 14.dp, end = 14.dp)
                    .graphicsLayer { alpha = chromeAlpha() }
            ) {
                GlassIconButton(
                    backdrop = backdrop,
                    icon = GalleryIcons.Back,
                    contentDescription = stringResource(R.string.viewer_back),
                    onClick = { if (infoProgress > 0f) animateInfo(0f) else close(currentId) },
                    tone = GlassTone.Dark,
                    size = GalleryDimens.ViewerTopButton,
                    iconSize = 24.dp,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                GlassCapsule(
                    backdrop = backdrop,
                    tone = GlassTone.Dark,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .height(GalleryDimens.ViewerTopButton)
                        .padding(horizontal = 66.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 18.dp)
                    ) {
                        BasicText(
                            text = dayTitle(current.takenAtMillis, zone, locale),
                            style = GalleryType.Button.copy(color = Color.White, fontWeight = FontWeight.Bold, lineHeight = 18.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        BasicText(
                            text = remember(current.takenAtMillis) { clock(current.takenAtMillis, zone, locale) },
                            style = GalleryType.Duration.copy(
                                color = Color.White.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Medium,
                                lineHeight = 15.sp
                            ),
                            maxLines = 1
                        )
                    }
                }
                if (mode != ViewerMode.Trash) {
                    GlassIconButton(
                        backdrop = backdrop,
                        icon = GalleryIcons.More,
                        contentDescription = stringResource(R.string.viewer_more),
                        onClick = { menuOpen = !menuOpen },
                        tone = GlassTone.Dark,
                        size = GalleryDimens.ViewerTopButton,
                        iconSize = 20.dp,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }

            if (current.isVideo && current.durationMs > 0) {
                GlassCapsule(
                    backdrop = backdrop,
                    tone = GlassTone.Dark,
                    modifier = Modifier
                        .padding(top = topInset + 70.dp, start = 16.dp)
                        .height(26.dp)
                        .graphicsLayer { alpha = chromeAlpha() * (1f - infoProgress) }
                ) {
                    Spacer(Modifier.width(8.dp))
                    Glyph(GalleryIcons.Play, Color.White, size = 12.dp)
                    Spacer(Modifier.width(5.dp))
                    BasicText(
                        Formatters.duration(current.durationMs),
                        style = GalleryType.Duration.copy(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.width(10.dp))
                }
            }
        }

        // ---------------------------------------------------------------- alt: şerit + araç çubuğu
        if (chromeShown && current != null) {
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = bottomInset + 32.dp)
                    .graphicsLayer { alpha = chromeAlpha() },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!infoVisible) {
                    ThumbStrip(
                        items = items,
                        currentIndex = pagerState.currentPage,
                        onSelect = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
                        onScrub = { index -> scope.launch { pagerState.scrollToPage(index) } }
                    )
                    Spacer(Modifier.height(18.dp))
                }
                if (mode == ViewerMode.Trash) {
                    TrashToolbar(
                        item = current,
                        backdrop = backdrop,
                        onRecover = { recover(current) },
                        onDelete = { deleteForever(current) }
                    )
                } else {
                    ViewerToolbar(
                        item = current,
                        infoOpen = infoOpen,
                        backdrop = backdrop,
                        onShare = {
                            if (!context.shareItems(listOf(current), context.getString(R.string.share_chooser))) toast = noAppText
                        },
                        onFavorite = { toggleFavorite(current) },
                        onInfo = { animateInfo(if (infoProgress > 0.5f) 0f else 1f) },
                        onEdit = { edit(current) },
                        onDelete = { askDelete(current) }
                    )
                }
            }
        }

        // ---------------------------------------------------------------- menü, onay, bildirim
        GlassMenu(
            backdrop = backdrop,
            visible = menuOpen,
            onDismiss = { menuOpen = false },
            tone = GlassTone.Dark,
            items = if (current == null) {
                emptyList()
            } else {
                listOf(
                    GlassMenuItem(stringResource(R.string.menu_copy), false) {
                        menuOpen = false
                        if (context.copyToClipboard(current, current.displayName.orEmpty())) {
                            toast = context.getString(R.string.toast_copied)
                        }
                    },
                    GlassMenuItem(stringResource(R.string.menu_open_with), false) {
                        menuOpen = false
                        if (!context.openWith(current, context.getString(R.string.menu_open_with))) toast = noAppText
                    },
                    GlassMenuItem(stringResource(R.string.menu_set_as), false) {
                        menuOpen = false
                        if (!context.setAs(current, context.getString(R.string.menu_set_as))) toast = noAppText
                    },
                    GlassMenuItem(stringResource(R.string.menu_slideshow), false) {
                        menuOpen = false
                        if (infoProgress > 0f) animateInfo(0f)
                        slideshow = true
                    },
                    if (mode == ViewerMode.Hidden) {
                        GlassMenuItem(stringResource(R.string.menu_unhide), false) {
                            menuOpen = false
                            stores.hidden.unhide(listOf(current.id))
                            toast = unhiddenText
                        }
                    } else {
                        GlassMenuItem(stringResource(R.string.menu_hide), false) {
                            menuOpen = false
                            stores.hidden.hide(listOf(current.id))
                            toast = hiddenText
                        }
                    }
                )
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = topInset + 64.dp, end = 14.dp)
        )

        AnimatedVisibility(
            visible = confirmDelete && current != null,
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
            visible = confirmDelete && current != null,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 12.dp, end = 12.dp, bottom = bottomInset + 28.dp)
        ) {
            val target = current
            if (target != null) {
                DeleteCard(
                    isVideo = target.isVideo,
                    backdrop = backdrop,
                    onConfirm = {
                        confirmDelete = false
                        launchTrash(target)
                    },
                    onCancel = { confirmDelete = false }
                )
            }
        }

        GlassToast(
            backdrop = backdrop,
            message = toast,
            tone = GlassTone.Dark,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = topInset + 70.dp)
        )

        // ---------------------------------------------------------------- video oynatıcı (3. aşama)
        val video = playingId?.let { id -> items.firstOrNull { it.id == id } }
        AnimatedVisibility(
            visible = video != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            val opened = remember { video }
            val target = video ?: opened
            if (target != null) {
                VideoPlayerScreen(
                    item = target,
                    title = dayTitle(target.takenAtMillis, zone, locale),
                    subtitle = remember(target.takenAtMillis) { clock(target.takenAtMillis, zone, locale) },
                    chromeVisible = videoChrome,
                    onChromeVisibleChange = { videoChrome = it },
                    onClose = { playingId = null },
                    toolbar = { glassBackdrop ->
                        if (mode == ViewerMode.Trash) {
                            TrashToolbar(
                                item = target,
                                backdrop = glassBackdrop,
                                onRecover = { recover(target) },
                                onDelete = { deleteForever(target) }
                            )
                        } else {
                            ViewerToolbar(
                                item = target,
                                infoOpen = false,
                                backdrop = glassBackdrop,
                                onShare = {
                                    if (!context.shareItems(listOf(target), context.getString(R.string.share_chooser))) {
                                        toast = noAppText
                                    }
                                },
                                onFavorite = { toggleFavorite(target) },
                                onInfo = {
                                    playingId = null
                                    animateInfo(1f)
                                },
                                onEdit = { edit(target) },
                                onDelete = { askDelete(target) }
                            )
                        }
                    }
                )
            }
        }

        // ---------------------------------------------------------------- düzenle (4. aşama)
        val editing = editingId?.let { id -> items.firstOrNull { it.id == id } }
        AnimatedVisibility(
            visible = editing != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            val opened = remember { editing }
            val target = editing ?: opened
            if (target != null) {
                EditScreen(
                    item = target,
                    onClose = { message ->
                        editingId = null
                        if (message != null) toast = message
                    }
                )
            }
        }
    }
}

/** Son Silinenler'de: Sil (kalıcı) · kalan gün · Kurtar. */
@Composable
private fun TrashToolbar(
    item: MediaItem,
    backdrop: com.kyant.backdrop.Backdrop,
    onRecover: () -> Unit,
    onDelete: () -> Unit
) {
    val now = remember { System.currentTimeMillis() }
    val days = CollectionsLogic.daysLeft(item.expiresMillis, now)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        GlassTextButton(
            backdrop = backdrop,
            text = stringResource(R.string.trash_delete),
            onClick = onDelete,
            tone = GlassTone.Dark,
            textColor = GalleryColors.Delete,
            height = GalleryDimens.ToolbarButton
        )
        if (item.expiresMillis > 0L) {
            BasicText(
                text = daysLeftText(days),
                style = GalleryType.Chip.copy(color = Color.White.copy(alpha = 0.8f))
            )
        }
        GlassTextButton(
            backdrop = backdrop,
            text = stringResource(R.string.trash_recover),
            onClick = onRecover,
            tone = GlassTone.Dark,
            textColor = Color.White,
            height = GalleryDimens.ToolbarButton
        )
    }
}

@Composable
private fun daysLeftText(days: Int): String =
    LocalContext.current.resources.getQuantityString(R.plurals.trash_days_left, days, days)

@Composable
internal fun ViewerToolbar(
    item: MediaItem,
    infoOpen: Boolean,
    backdrop: com.kyant.backdrop.Backdrop,
    onShare: () -> Unit,
    onFavorite: () -> Unit,
    onInfo: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        GlassIconButton(
            backdrop = backdrop,
            icon = GalleryIcons.Share,
            contentDescription = stringResource(R.string.action_share),
            onClick = onShare,
            tone = GlassTone.Dark,
            size = GalleryDimens.ToolbarButton,
            iconSize = 24.dp
        )
        GlassCapsule(
            backdrop = backdrop,
            tone = GlassTone.Dark,
            modifier = Modifier.height(GalleryDimens.ToolbarButton)
        ) {
            Spacer(Modifier.width(6.dp))
            ToolbarSlot(
                icon = if (item.isFavorite) GalleryIcons.HeartFilled else GalleryIcons.Heart,
                tint = if (item.isFavorite) GalleryColors.Favorite else Color.White,
                description = stringResource(if (item.isFavorite) R.string.viewer_unfavorite else R.string.viewer_favorite),
                highlighted = false,
                onClick = onFavorite
            )
            Spacer(Modifier.width(4.dp))
            ToolbarSlot(
                icon = GalleryIcons.Info,
                tint = Color.White,
                description = stringResource(R.string.viewer_info),
                highlighted = infoOpen,
                onClick = onInfo
            )
            Spacer(Modifier.width(4.dp))
            ToolbarSlot(
                icon = GalleryIcons.Edit,
                tint = Color.White,
                description = stringResource(R.string.viewer_edit),
                highlighted = false,
                onClick = onEdit
            )
            Spacer(Modifier.width(6.dp))
        }
        GlassIconButton(
            backdrop = backdrop,
            icon = GalleryIcons.Trash,
            contentDescription = stringResource(R.string.action_delete),
            onClick = onDelete,
            tone = GlassTone.Dark,
            size = GalleryDimens.ToolbarButton,
            iconSize = 24.dp
        )
    }
}

@Composable
private fun ToolbarSlot(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    description: String,
    highlighted: Boolean,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .size(52.dp, 44.dp)
            .clip(CircleShape)
            .background(if (highlighted) Color.White.copy(alpha = 0.16f) else Color.Transparent)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Glyph(icon, tint, size = 25.dp)
    }
}

@Composable
private fun DeleteCard(
    isVideo: Boolean,
    backdrop: com.kyant.backdrop.Backdrop,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .glass(backdrop, RoundedRectangle(30.dp), GlassTone.Sheet, refraction = false)
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 14.dp)
    ) {
        BasicText(
            stringResource(if (isVideo) R.string.viewer_delete_message_video else R.string.viewer_delete_message),
            style = GalleryType.Body.copy(
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, bottom = 16.dp)
        )
        CardButton(
            text = stringResource(if (isVideo) R.string.viewer_delete_video else R.string.viewer_delete_photo),
            fill = GalleryColors.Delete,
            weight = FontWeight.Bold,
            onClick = onConfirm
        )
        Spacer(Modifier.height(8.dp))
        CardButton(
            text = stringResource(R.string.viewer_cancel),
            fill = Color.White.copy(alpha = 0.14f),
            weight = FontWeight.SemiBold,
            onClick = onCancel
        )
    }
}

@Composable
private fun CardButton(text: String, fill: Color, weight: FontWeight, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(CircleShape)
            .background(fill)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        BasicText(text, style = GalleryType.Button.copy(color = Color.White, fontSize = 16.sp, fontWeight = weight))
    }
}

/** Görüntüleyici açıkken durum çubuğu simgeleri beyaz; kontroller gizlenince çubuklar da gizlenir. */
@Composable
private fun ViewerSystemBars(hidden: Boolean) {
    val view = LocalView.current
    val window = remember(view) { view.context.findActivity()?.window } ?: return
    DisposableEffect(window) {
        val controller = WindowCompat.getInsetsController(window, view)
        val lightStatus = controller.isAppearanceLightStatusBars
        val lightNavigation = controller.isAppearanceLightNavigationBars
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose {
            controller.show(WindowInsetsCompat.Type.systemBars())
            controller.isAppearanceLightStatusBars = lightStatus
            controller.isAppearanceLightNavigationBars = lightNavigation
        }
    }
    LaunchedEffect(window, hidden) {
        val controller = WindowCompat.getInsetsController(window, view)
        if (hidden) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/** "Bugün", "Dün" veya "8 Kasım 2026" */
@Composable
internal fun dayTitle(millis: Long, zone: ZoneId, locale: Locale): String {
    val date = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
    val today = LocalDate.now(zone)
    return when (date) {
        today -> stringResource(R.string.viewer_today)
        today.minusDays(1) -> stringResource(R.string.viewer_yesterday)
        else -> Formatters.longDate(millis, zone, locale)
    }
}

/** "06:42" */
internal fun clock(millis: Long, zone: ZoneId, locale: Locale): String =
    DateTimeFormatter.ofPattern("HH:mm", locale).format(Instant.ofEpochMilli(millis).atZone(zone))

/** Açık öğeye görünüşçe benzeyenler; aday yalnızca görüntüleyicideki liste (Kütüphane'de tümü). */
private fun similarTo(item: MediaItem, items: List<MediaItem>, vectors: Map<Long, VectorEntry>): List<MediaItem> {
    val target = vectors[item.id]?.vector ?: return emptyList()
    val byId = HashMap<Long, MediaItem>(items.size * 2)
    val candidates = ArrayList<Pair<Long, FloatArray>>(minOf(items.size, vectors.size))
    for (other in items) {
        if (other.id == item.id) continue
        val v = vectors[other.id] ?: continue
        byId[other.id] = other
        candidates += other.id to v.vector
    }
    return VectorMath.similar(target, candidates).mapNotNull { byId[it] }
}
