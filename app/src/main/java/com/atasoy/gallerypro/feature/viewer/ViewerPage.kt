package com.atasoy.gallerypro.feature.viewer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Precision
import com.atasoy.gallerypro.data.media.LocalGalleryImageLoader
import com.atasoy.gallerypro.data.media.MediaItem
import com.atasoy.gallerypro.data.media.MediaThumb
import com.atasoy.gallerypro.data.media.contentUri
import com.atasoy.gallerypro.data.media.displayHeight
import com.atasoy.gallerypro.data.media.displayWidth
import com.atasoy.gallerypro.designsystem.Glyph
import com.atasoy.gallerypro.designsystem.icons.GalleryIcons
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs
import kotlin.math.roundToInt

/** Küçük/orta fotoğraflarda mevcut Coil yakınlaştırma davranışı korunur. */
private const val HiResMaxPx = 4096

/**
 * Görüntüleyicideki tek sayfa: fotoğraf (yakınlaştırılabilir) veya video kapağı.
 *
 * Hareketler: dokun → kontrolleri gizle/göster, çift dokun → yakınlaştır,
 * iki parmak → yakınlaştır/kaydır, dikey sürükleme → [onVerticalDrag]
 * (yukarı: Bilgi paneli, aşağı: kapat). Yatay sürükleme sayfalayıcıya kalır.
 */
@OptIn(kotlinx.coroutines.FlowPreview::class)
@Composable
internal fun ViewerPage(
    item: MediaItem,
    isCurrent: Boolean,
    infoProgress: () -> Float,
    infoPanelHeightPx: Float,
    playLabel: String,
    onTap: () -> Unit,
    onVerticalDrag: (Float) -> Unit,
    onVerticalDragEnd: (Float) -> Unit,
    onZoomChanged: (Boolean) -> Unit,
    onPlayVideo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageLoader = LocalGalleryImageLoader.current
    val scope = rememberCoroutineScope()
    val uri = remember(item.id, item.isVideo) { item.contentUri() }
    val zoom = remember(item.id) {
        ZoomState(maxScale = if (item.isVideo) 1f else 8f).apply {
            if (item.displayWidth > 0 && item.displayHeight > 0) {
                content = Size(item.displayWidth.toFloat(), item.displayHeight.toFloat())
            }
        }
    }
    val tap by rememberUpdatedState(onTap)
    val drag by rememberUpdatedState(onVerticalDrag)
    val dragEnd by rememberUpdatedState(onVerticalDragEnd)
    val zoomChanged by rememberUpdatedState(onZoomChanged)

    // Ekrandan çıkan sayfa sıfırlanır; ekrandaki sayfa yakınlaştırma durumunu bildirir.
    LaunchedEffect(isCurrent) {
        if (!isCurrent) {
            zoom.reset()
        } else {
            snapshotFlow { zoom.isZoomed }.collect { zoomChanged(it) }
        }
    }

    val hiRes by remember { derivedStateOf { zoom.scale > 1.6f } }
    // Yalnızca büyük ve Android'in bölgesel decode desteklediği medya biçimleri.
    val tileEnabled = LargePhotoTileMath.supported(
        item.width, item.height, item.isVideo, item.mimeType
    ) && item.orientation % 90 == 0

    // Düzenlenince kaynak dosya değişir; sürüm önbellek ve parça anahtarına girer.
    val version = item.modifiedMillis
    val fullRequest = remember(uri, version, tileEnabled) {
        ImageRequest.Builder(context)
            .data(uri)
            .memoryCacheKey("$uri@$version" + if (tileEnabled) "#preview" else "")
            .apply {
                if (tileEnabled) {
                    // 108 MP JPEG'nin tamamını belleğe açma: ekrana sığan önizleme.
                    size(LargePhotoTileMath.PreviewPx)
                    precision(Precision.INEXACT)
                }
            }
            .build()
    }

    var visibleTile by remember(item.id, version) { mutableStateOf<DecodedPhotoTile?>(null) }
    LaunchedEffect(item.id, version, isCurrent, tileEnabled) {
        if (!tileEnabled || !isCurrent) {
            visibleTile = null
            return@LaunchedEffect
        }
        // Pan/zoom sürerken her pikselde decode başlatma. Son konuma ait
        // isteği işle; önceki isteği collectLatest ile iptal et.
        snapshotFlow {
            LargePhotoTileMath.visibleRect(
                item.displayWidth, item.displayHeight,
                zoom.container.width, zoom.container.height,
                zoom.scale, zoom.offset.x, zoom.offset.y
            )
        }
            .distinctUntilChanged()
            .debounce(140)
            .collectLatest { region ->
                visibleTile = if (region == null) null
                else LargePhotoRegionDecoder.decode(context, item, region)
            }
    }
    val hiResRequest = remember(uri, version) {
        ImageRequest.Builder(context)
            .data(uri)
            .size(HiResMaxPx)
            .precision(Precision.INEXACT)
            .memoryCacheKey("$uri@$version#hi")
            .build()
    }

    Box(
        modifier
            .fillMaxSize()
            .onSizeChanged { zoom.container = Size(it.width.toFloat(), it.height.toFloat()) }
            .pointerInput(item.id) {
                detectTapGestures(
                    onTap = { tap() },
                    onDoubleTap = { position ->
                        if (!item.isVideo) scope.launch { zoom.toggleAt(position) }
                    }
                )
            }
            .pointerInput(item.id) {
                detectViewerGestures(
                    zoom = zoom,
                    zoomEnabled = !item.isVideo,
                    onVerticalDrag = { drag(it) },
                    onVerticalDragEnd = { dragEnd(it) },
                    onTransformEnd = { scope.launch { zoom.settle() } }
                )
            }
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoom.scale
                    scaleY = zoom.scale
                    translationX = zoom.offset.x
                    translationY = zoom.offset.y + infoShift(size.height, zoom.fitted.height, infoPanelHeightPx) * infoProgress()
                }
        ) {
            // Izgaradan gelen küçük resim hemen görünür; asıl görüntü üstüne yüklenir.
            AsyncImage(
                model = MediaThumb(uri, if (item.isVideo) 1080 else 384, item.modifiedMillis),
                contentDescription = null,
                imageLoader = imageLoader,
                contentScale = ContentScale.Fit,
                onSuccess = { state ->
                    if (item.isVideo) {
                        val image = state.result.image
                        zoom.content = Size(image.width.toFloat(), image.height.toFloat())
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            if (!item.isVideo) {
                AsyncImage(
                    model = fullRequest,
                    contentDescription = null,
                    imageLoader = imageLoader,
                    contentScale = ContentScale.Fit,
                    onSuccess = { state ->
                        val image = state.result.image
                        if (!tileEnabled) {
                            zoom.content = Size(image.width.toFloat(), image.height.toFloat())
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                if (hiRes && !tileEnabled) {
                    AsyncImage(
                        model = hiResRequest,
                        contentDescription = null,
                        imageLoader = imageLoader,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                // Kaynak önizleme arka planda kalır; yalnızca ekrandaki orijinal
                // piksel bölgesi GPU üzerinde aynı Fit koordinatlarında üstüne çizilir.
                // Zaten ViewerPage'in graphicsLayer'ı altında olduğundan cam tasarım
                // ve mevcut hareket kontrolleri hiçbir şekilde değişmez.
                val tile = visibleTile
                if (tileEnabled && tile != null) {
                    Canvas(Modifier.fillMaxSize()) {
                        val photoWidth = item.displayWidth.toFloat()
                        val photoHeight = item.displayHeight.toFloat()
                        val fitted = ZoomMath.fit(
                            Size(size.width, size.height),
                            Size(photoWidth, photoHeight)
                        )
                        val dx = (size.width - fitted.width) / 2f +
                            fitted.width * tile.displayRect.left / photoWidth
                        val dy = (size.height - fitted.height) / 2f +
                            fitted.height * tile.displayRect.top / photoHeight
                        val dw = fitted.width * tile.displayRect.width / photoWidth
                        val dh = fitted.height * tile.displayRect.height / photoHeight
                        if (dw > 0f && dh > 0f) {
                            drawImage(
                                image = tile.bitmap,
                                srcSize = IntSize(tile.bitmap.width, tile.bitmap.height),
                                dstOffset = IntOffset(dx.roundToInt(), dy.roundToInt()),
                                dstSize = IntSize(dw.roundToInt().coerceAtLeast(1),
                                    dh.roundToInt().coerceAtLeast(1)),
                                filterQuality = FilterQuality.Medium
                            )
                        }
                    }
                }
            }
        }

        if (item.isVideo) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        alpha = 1f - infoProgress()
                        translationY = infoShift(
                            zoom.container.height,
                            zoom.fitted.height,
                            infoPanelHeightPx
                        ) * infoProgress()
                    }
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.32f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.45f), CircleShape)
                    .clickable(role = Role.Button, onClick = onPlayVideo)
                    .semantics { contentDescription = playLabel },
                contentAlignment = Alignment.Center
            ) {
                Glyph(GalleryIcons.Play, Color.White, size = 34.dp)
            }
        }
    }
}

/**
 * Bilgi paneli açıkken fotoğrafın dikey kayması. Fotoğraf panelin üstündeki
 * alana sığıyorsa oraya ortalanır; sığmıyorsa alt kenarı panelin üstüne dayanır.
 */
internal fun infoShift(containerHeight: Float, fittedHeight: Float, panelHeight: Float): Float {
    val available = containerHeight - panelHeight
    return if (fittedHeight <= available) {
        -panelHeight / 2f
    } else {
        available - (containerHeight + fittedHeight) / 2f
    }
}

private enum class GestureMode { Undecided, Transform, Pan, Vertical, Ignore }

/**
 * Tek elle: yakınlaştırılmışsa kaydırma, değilse dikey sürükleme. Yatay hareket
 * tüketilmez, böylece sayfalayıcı sonraki/önceki fotoğrafa geçer. İki elle: yakınlaştırma.
 */
private suspend fun PointerInputScope.detectViewerGestures(
    zoom: ZoomState,
    zoomEnabled: Boolean,
    onVerticalDrag: (Float) -> Unit,
    onVerticalDragEnd: (Float) -> Unit,
    onTransformEnd: () -> Unit
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val tracker = VelocityTracker()
        tracker.addPosition(down.uptimeMillis, down.position)
        var mode = GestureMode.Undecided
        var travelled = Offset.Zero

        while (true) {
            val event = awaitPointerEvent()
            val pressed = event.changes.count { it.pressed }
            if (pressed == 0) break

            if (zoomEnabled && pressed >= 2 && mode != GestureMode.Vertical) {
                mode = GestureMode.Transform
                val zoomChange = event.calculateZoom()
                val panChange = event.calculatePan()
                if (zoomChange != 1f || panChange != Offset.Zero) {
                    zoom.transform(zoomChange, panChange, event.calculateCentroid(useCurrent = false))
                }
                event.changes.forEach { if (it.positionChanged()) it.consume() }
                continue
            }

            val change = event.changes.firstOrNull { it.pressed } ?: break
            val delta = change.positionChange()
            when (mode) {
                GestureMode.Undecided -> {
                    if (change.isConsumed) break
                    travelled += delta
                    if (travelled.getDistance() > viewConfiguration.touchSlop) {
                        mode = when {
                            zoom.isZoomed -> GestureMode.Pan
                            abs(travelled.y) > abs(travelled.x) * 1.2f -> GestureMode.Vertical
                            else -> GestureMode.Ignore
                        }
                        if (mode == GestureMode.Ignore) break
                        change.consume()
                    }
                }
                GestureMode.Pan, GestureMode.Transform -> {
                    if (zoom.isZoomed) {
                        zoom.pan(delta)
                        change.consume()
                    }
                }
                GestureMode.Vertical -> {
                    tracker.addPosition(change.uptimeMillis, change.position)
                    onVerticalDrag(delta.y)
                    change.consume()
                }
                GestureMode.Ignore -> break
            }
        }

        when (mode) {
            GestureMode.Vertical -> onVerticalDragEnd(tracker.calculateVelocity().y)
            GestureMode.Transform, GestureMode.Pan -> onTransformEnd()
            else -> Unit
        }
    }
}
