package com.atasoy.gallerypro.feature.viewer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem as PlayerMediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import com.atasoy.gallerypro.R
import com.atasoy.gallerypro.data.media.Formatters
import com.atasoy.gallerypro.data.media.MediaItem
import com.atasoy.gallerypro.data.media.VideoTrimExporter
import com.atasoy.gallerypro.data.media.VideoTrimLogic
import com.atasoy.gallerypro.data.media.VideoTrimRange
import com.atasoy.gallerypro.data.media.contentUri
import com.atasoy.gallerypro.designsystem.GalleryColors
import com.atasoy.gallerypro.designsystem.GalleryDimens
import com.atasoy.gallerypro.designsystem.GalleryType
import com.atasoy.gallerypro.designsystem.GlassIconButton
import com.atasoy.gallerypro.designsystem.GlassTextButton
import com.atasoy.gallerypro.designsystem.GlassTone
import com.atasoy.gallerypro.designsystem.glass
import com.atasoy.gallerypro.designsystem.icons.GalleryIcons
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.RoundedRectangle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * Mevcut oynatıcıya dokunmadan aynı SharedVideoPlayer'ı kullanır.
 * 7e: iki işaretçiyle bölge seç, önizle, yeni MP4 olarak kaydet.
 */
@Composable
internal fun VideoTrimScreen(
    item: MediaItem,
    onSaved: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val backdrop = rememberLayerBackdrop()
    val scope = rememberCoroutineScope()
    val player: ExoPlayer = remember { SharedVideoPlayer.get(context) }
    var range by remember(item.id) {
        mutableStateOf(VideoTrimLogic.initial(item.durationMs))
    }
    val rangeNow by rememberUpdatedState(range)
    var previewMs by remember { mutableLongStateOf(0L) }
    var saving by remember { mutableStateOf(false) }
    var exportJob by remember { mutableStateOf<Job?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var playing by remember { mutableStateOf(false) }

    val saveFailed = stringResource(R.string.video_trim_error)
    val selectRange = stringResource(R.string.video_trim_select_range)
    val handleDescription = stringResource(R.string.video_trim_handles)

    DisposableEffect(item.id) {
        player.stop()
        player.clearMediaItems()
        player.volume = 1f
        player.setPlaybackSpeed(1f)
        player.setMediaItem(PlayerMediaItem.fromUri(item.contentUri()))
        player.prepare()
        player.pause()
        onDispose {
            exportJob?.cancel()
            player.stop()
            player.clearMediaItems()
        }
    }

    LaunchedEffect(player) {
        while (isActive) {
            val current = rangeNow
            if (current != null) {
                val pos = player.currentPosition
                previewMs = pos
                if (player.isPlaying && pos >= current.endMs) {
                    player.pause()
                    player.seekTo(current.startMs)
                }
                playing = player.isPlaying
            }
            delay(120)
        }
    }

    fun changeRange(next: VideoTrimRange) {
        if (saving) return
        range = next
        player.pause()
        player.seekTo(next.startMs)
        previewMs = next.startMs
        error = null
    }

    fun cancelOrClose() {
        if (saving) {
            exportJob?.cancel()
            exportJob = null
            saving = false
        } else {
            onClose()
        }
    }

    fun saveRange() {
        if (saving) return
        val chosen = range
        if (chosen == null || !VideoTrimLogic.canExport(chosen, item.durationMs)) {
            error = selectRange
            return
        }
        player.pause()
        saving = true
        error = null
        exportJob = scope.launch {
            try {
                if (VideoTrimExporter.export(context, item, chosen)) {
                    saving = false
                    onSaved()
                } else {
                    saving = false
                    error = saveFailed
                }
            } catch (_: CancellationException) {
                saving = false
            } catch (_: Exception) {
                saving = false
                error = saveFailed
            } finally {
                exportJob = null
            }
        }
    }

    BackHandler { cancelOrClose() }

    val frames by produceState(emptyList<ImageBitmap>(), item.id) {
        value = withContext(Dispatchers.IO) {
            extractFrames(context, item.contentUri(), item.durationMs,
                GalleryDimens.VideoTrimFramePixels)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(GalleryColors.DarkBackground)
            .layerBackdrop(backdrop)
    ) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .padding(horizontal = GalleryDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(GalleryDimens.VideoTrimGap)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(top = GalleryDimens.VideoTrimGap),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassIconButton(
                    backdrop = backdrop,
                    icon = GalleryIcons.Back,
                    contentDescription = stringResource(R.string.viewer_back),
                    tone = GlassTone.Dark,
                    onClick = ::cancelOrClose
                )
                BasicText(
                    text = stringResource(R.string.video_trim_title),
                    style = GalleryType.SectionTitle.copy(color = Color.White)
                )
                GlassTextButton(
                    backdrop = backdrop,
                    tone = GlassTone.Dark,
                    text = stringResource(if (saving) R.string.video_trim_cancel_export
                        else R.string.video_trim_save),
                    textColor = Color.White,
                    onClick = { if (saving) cancelOrClose() else saveRange() }
                )
            }
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f)
                    .clip(RoundedCornerShape(GalleryDimens.CardCorner))
                    .background(GalleryColors.DarkBackground),
                contentAlignment = Alignment.Center
            ) {
                PlayerSurface(
                    player = player,
                    surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
                    modifier = Modifier.fillMaxSize()
                )
                if (range != null && !saving) {
                    GlassIconButton(
                        backdrop = backdrop,
                        icon = if (playing) GalleryIcons.Pause else GalleryIcons.Play,
                        contentDescription = stringResource(if (playing) R.string.video_pause
                            else R.string.video_play),
                        tone = GlassTone.Dark,
                        onClick = {
                            if (player.isPlaying) player.pause()
                            else {
                                val selection = range ?: return@GlassIconButton
                                if (player.currentPosition !in selection.startMs..selection.endMs ||
                                    player.currentPosition >= selection.endMs - 100L
                                ) player.seekTo(selection.startMs)
                                player.play()
                            }
                        }
                    )
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth()
                    .glass(backdrop, RoundedRectangle(GalleryDimens.CardCorner),
                        GlassTone.Sheet, refraction = false)
                    .padding(GalleryDimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(GalleryDimens.VideoTrimGap)
            ) {
                val chosen = range
                if (chosen == null) {
                    BasicText(
                        stringResource(R.string.video_trim_short),
                        style = GalleryType.Body.copy(color = Color.White)
                    )
                } else {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BasicText(
                            stringResource(R.string.video_trim_start,
                                Formatters.duration(chosen.startMs)),
                            style = GalleryType.Chip.copy(color = Color.White)
                        )
                        BasicText(
                            stringResource(R.string.video_trim_end,
                                Formatters.duration(chosen.endMs)),
                            style = GalleryType.Chip.copy(color = Color.White)
                        )
                    }
                    TrimTimeline(
                        frames = frames,
                        durationMs = item.durationMs,
                        range = chosen,
                        contentDescription = handleDescription,
                        enabled = !saving,
                        onChange = ::changeRange
                    )
                    BasicText(
                        stringResource(R.string.video_trim_selected,
                            Formatters.duration(chosen.lengthMs)),
                        style = GalleryType.Button.copy(color = Color.White)
                    )
                    BasicText(
                        stringResource(R.string.video_trim_preview,
                            Formatters.duration(previewMs)),
                        style = GalleryType.Chip.copy(color = Color.White)
                    )
                }
                BasicText(
                    stringResource(R.string.video_trim_original_note),
                    style = GalleryType.Chip.copy(color = Color.White)
                )
                if (saving) {
                    BasicText(
                        stringResource(R.string.video_trim_processing),
                        style = GalleryType.Button.copy(color = Color.White)
                    )
                }
                if (error != null) {
                    BasicText(
                        error.orEmpty(),
                        style = GalleryType.Body.copy(color = GalleryColors.Delete)
                    )
                }
            }
            Spacer(Modifier.height(GalleryDimens.VideoTrimGap))
        }
    }
}

/** Aynı film şeridinin iki büyük sürükleme kolu; aralık sınırları asla birbirini geçmez. */
@Composable
private fun TrimTimeline(
    frames: List<ImageBitmap>,
    durationMs: Long,
    range: VideoTrimRange,
    contentDescription: String,
    enabled: Boolean,
    onChange: (VideoTrimRange) -> Unit
) {
    val liveRange by rememberUpdatedState(range)
    val liveOnChange by rememberUpdatedState(onChange)
    val barShape = RoundedCornerShape(GalleryDimens.AlbumCorner)
    BoxWithConstraints(
        Modifier.fillMaxWidth()
            .height(GalleryDimens.VideoTrimStripHeight)
            .semantics { this.contentDescription = contentDescription }
            .pointerInput(durationMs, enabled) {
                var moveStart = true
                detectDragGestures(
                    onDragStart = { offset ->
                        val current = liveRange
                        val startX = size.width * current.startMs.toFloat() / durationMs
                        val endX = size.width * current.endMs.toFloat() / durationMs
                        moveStart = abs(offset.x - startX) <= abs(offset.x - endX)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        if (!enabled) return@detectDragGestures
                        val time = VideoTrimLogic.position(
                            change.position.x / size.width.coerceAtLeast(1), durationMs
                        )
                        val current = liveRange
                        liveOnChange(if (moveStart)
                            VideoTrimLogic.moveStart(current, time)
                        else VideoTrimLogic.moveEnd(current, time, durationMs))
                    }
                )
            }
    ) {
        Row(
            Modifier.fillMaxSize().clip(barShape)
                .background(GalleryColors.EditToolInner)
        ) {
            frames.forEach { frame ->
                Image(
                    bitmap = frame,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }
        val startFraction = (range.startMs.toFloat() / durationMs).coerceIn(0f, 1f)
        val endFraction = (range.endMs.toFloat() / durationMs).coerceIn(0f, 1f)
        Box(Modifier.fillMaxHeight().width(maxWidth * startFraction)
            .background(GalleryColors.PreviewScrim))
        Box(Modifier.fillMaxHeight().offset(x = maxWidth * endFraction)
            .fillMaxWidth().background(GalleryColors.PreviewScrim))
        Box(
            Modifier.offset(x = maxWidth * startFraction)
                .width(GalleryDimens.VideoTrimHandleWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(GalleryDimens.VideoTrimHandleRadius))
                .background(Color.White)
        )
        Box(
            Modifier.offset(x = (maxWidth * endFraction -
                GalleryDimens.VideoTrimHandleWidth).coerceAtLeast(0.dp))
                .width(GalleryDimens.VideoTrimHandleWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(GalleryDimens.VideoTrimHandleRadius))
                .background(Color.White)
        )
        Box(Modifier.fillMaxSize().border(
            GalleryDimens.GridSpacing,
            Color.White,
            barShape
        ))
    }
}
