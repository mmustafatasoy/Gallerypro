package com.atasoy.gallerypro.feature.viewer

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.content.edit
import androidx.core.util.Consumer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import coil3.compose.AsyncImage
import com.atasoy.gallerypro.R
import com.atasoy.gallerypro.data.media.Formatters
import com.atasoy.gallerypro.data.media.LocalGalleryImageLoader
import com.atasoy.gallerypro.data.media.MediaItem
import com.atasoy.gallerypro.data.media.MediaThumb
import com.atasoy.gallerypro.data.media.contentUri
import com.atasoy.gallerypro.data.media.displayHeight
import com.atasoy.gallerypro.data.media.displayWidth
import com.atasoy.gallerypro.designsystem.GalleryDimens
import com.atasoy.gallerypro.designsystem.GalleryMotion
import com.atasoy.gallerypro.designsystem.GalleryType
import com.atasoy.gallerypro.designsystem.GlassCapsule
import com.atasoy.gallerypro.designsystem.GlassIconButton
import com.atasoy.gallerypro.designsystem.GlassTextButton
import com.atasoy.gallerypro.designsystem.GlassTone
import com.atasoy.gallerypro.designsystem.Glyph
import com.atasoy.gallerypro.designsystem.glass
import com.atasoy.gallerypro.designsystem.icons.GalleryIcons
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.Capsule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import androidx.media3.common.MediaItem as PlayerItem

/** Hız düğmesinin sırası (dokümandaki gibi): 1× → 1,5× → 2× → 0,5×. */
private val SpeedSteps = listOf(1f, 1.5f, 2f, 0.5f)
private const val SeekStepMs = 10_000L
private const val StripFrames = 10

/**
 * Uygulama boyunca tek ExoPlayer (doküman: "tek bir ExoPlayer örneği yeniden kullanılır").
 * Sadece ana iş parçacığından erişilir.
 */
internal object SharedVideoPlayer {
    private var player: ExoPlayer? = null

    fun get(context: Context): ExoPlayer =
        player ?: ExoPlayer.Builder(context.applicationContext).build().also { player = it }
}

/** Videonun kaldığı yer; uygulamanın kendi deposunda (Room 5. aşamada). */
private class VideoPositions(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("video_positions", Context.MODE_PRIVATE)

    fun get(id: Long): Long = prefs.getLong(id.toString(), 0L)

    fun set(id: Long, positionMs: Long) {
        prefs.edit {
            if (positionMs > 0L) putLong(id.toString(), positionMs) else remove(id.toString())
        }
    }
}

/**
 * Kendi cam video oynatıcımız (3. aşama). Görüntü PlayerSurface (TextureView, cam
 * arkasındaki görüntüyü örnekleyebilsin diye); altta cam kapsül: oynat/duraklat,
 * film şeridi, ses. Üstte çözünürlük rozeti ve hız çipi.
 *
 * Hareketler: dokun → kontrolleri gizle/göster, sağa/sola çift dokun → ±10 sn,
 * aşağı çek → kapat. Oynarken uygulamadan çıkınca Resim içinde Resim (Android 12+).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun VideoPlayerScreen(
    item: MediaItem,
    title: String,
    subtitle: String,
    chromeVisible: Boolean,
    onChromeVisibleChange: (Boolean) -> Unit,
    onClose: () -> Unit,
    toolbar: @Composable (Backdrop) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val imageLoader = LocalGalleryImageLoader.current
    val scope = rememberCoroutineScope()
    val locale = remember { Locale.getDefault() }
    val backdrop = rememberLayerBackdrop()
    val uri = remember(item.id) { item.contentUri() }
    val player = remember { SharedVideoPlayer.get(context) }
    val positions = remember { VideoPositions(context) }
    val setChrome by rememberUpdatedState(onChromeVisibleChange)
    val chromeNow by rememberUpdatedState(chromeVisible)
    val close by rememberUpdatedState(onClose)

    var playing by remember { mutableStateOf(false) }
    var ended by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var firstFrame by remember { mutableStateOf(false) }
    var muted by remember { mutableStateOf(true) }
    var speedIndex by remember { mutableIntStateOf(0) }
    var videoSize by remember { mutableStateOf(VideoSize.UNKNOWN) }
    var format by remember { mutableStateOf<Format?>(null) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(item.durationMs) }
    var scrubbing by remember { mutableStateOf(false) }
    var seekHint by remember { mutableIntStateOf(0) }
    var seekHintKey by remember { mutableIntStateOf(0) }
    val dismiss = remember { Animatable(0f) }

    // ---------------------------------------------------------------- oynatıcı
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                ended = playbackState == Player.STATE_ENDED
                if (player.duration > 0L) durationMs = player.duration
            }

            override fun onVideoSizeChanged(size: VideoSize) {
                videoSize = size
                format = player.videoFormat
            }

            override fun onRenderedFirstFrame() {
                firstFrame = true
            }

            override fun onPlayerError(error: PlaybackException) {
                failed = true
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    DisposableEffect(item.id) {
        failed = false
        firstFrame = false
        player.setAudioAttributes(AudioAttributes.DEFAULT, false)
        player.volume = 0f
        player.setPlaybackSpeed(1f)
        player.setMediaItem(PlayerItem.fromUri(uri))
        player.prepare()
        val saved = positions.get(item.id)
        if (saved > 0L && (item.durationMs <= 0L || saved < item.durationMs - 1_000L)) player.seekTo(saved)
        player.playWhenReady = true
        onDispose {
            val pos = player.currentPosition
            val dur = player.duration
            positions.set(item.id, if (dur > 0L && pos >= dur - 1_000L) 0L else pos)
            player.stop()
            player.clearMediaItems()
        }
    }

    LaunchedEffect(playing) {
        while (playing) {
            positionMs = player.currentPosition
            delay(200)
        }
        positionMs = player.currentPosition
    }

    // Oynarken 3 sn dokunulmazsa kontroller kaybolur.
    LaunchedEffect(chromeVisible, playing, scrubbing) {
        if (chromeVisible && playing && !scrubbing) {
            delay(3000)
            setChrome(false)
        }
    }

    LaunchedEffect(seekHintKey) {
        if (seekHint != 0) {
            delay(650)
            seekHint = 0
        }
    }

    // Uygulamadan çıkınca (Resim içinde Resim değilse) duraklat; oynarken ekran kapanmasın.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { player.pause() }
    DisposableEffect(view, playing) {
        view.keepScreenOn = playing
        onDispose { view.keepScreenOn = false }
    }

    // ---------------------------------------------------------------- Resim içinde Resim
    val activity = remember(view) { view.context.findComponentActivity() }
    var inPip by remember { mutableStateOf(activity?.isInPictureInPictureMode == true) }
    DisposableEffect(activity) {
        val host = activity ?: return@DisposableEffect onDispose { }
        val listener = Consumer<PictureInPictureModeChangedInfo> { info -> inPip = info.isInPictureInPictureMode }
        host.addOnPictureInPictureModeChangedListener(listener)
        onDispose {
            host.removeOnPictureInPictureModeChangedListener(listener)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                runCatching {
                    host.setPictureInPictureParams(PictureInPictureParams.Builder().setAutoEnterEnabled(false).build())
                }
            }
        }
    }
    LaunchedEffect(activity, playing, videoSize) {
        val host = activity ?: return@LaunchedEffect
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching {
                host.setPictureInPictureParams(
                    PictureInPictureParams.Builder()
                        .setAspectRatio(pipRatio(videoSize))
                        .setAutoEnterEnabled(playing)
                        .build()
                )
            }
        }
    }

    // ---------------------------------------------------------------- işlemler
    fun togglePlay() {
        when {
            ended -> {
                player.seekTo(0L)
                player.play()
            }
            player.isPlaying -> player.pause()
            else -> player.play()
        }
    }

    fun toggleMute() {
        muted = !muted
        player.volume = if (muted) 0f else 1f
        // Sessizken müzik çalan uygulamayı durdurmamak için ses odağı sadece sesliyken istenir.
        player.setAudioAttributes(AudioAttributes.DEFAULT, !muted)
    }

    fun seekBy(deltaMs: Long) {
        val target = (player.currentPosition + deltaMs).coerceIn(0L, max(0L, durationMs))
        player.seekTo(target)
        positionMs = target
        seekHint = if (deltaMs < 0) -1 else 1
        seekHintKey++
    }

    var lastHapticSecond by remember { mutableLongStateOf(-1L) }
    fun seekToFraction(fraction: Float) {
        if (durationMs <= 0L) return
        val target = (durationMs * fraction.coerceIn(0f, 1f)).toLong()
        player.seekTo(target)
        positionMs = target
        val second = target / 1000L
        if (second != lastHapticSecond) {
            lastHapticSecond = second
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    BackHandler { close() }

    val frames by produceState(emptyList<ImageBitmap>(), item.id) {
        value = withContext(Dispatchers.IO) {
            extractFrames(context, uri, item.durationMs, with(density) { 48.dp.roundToPx() })
        }
    }

    // ---------------------------------------------------------------- ekran
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = 1f - min(1f, dismiss.value / (size.height * 0.6f)) }
            .background(Color.Black)
    ) {
        val topInset = WindowInsets.statusBarsIgnoringVisibility.asPaddingValues().calculateTopPadding()
        val bottomInset = WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues().calculateBottomPadding()

        // Görüntü boyutu: dikey video ekranı doldurur (Crop), yatay video sığdırılır (Fit).
        val srcW = if (videoSize.width > 0) videoSize.width * videoSize.pixelWidthHeightRatio else item.displayWidth.toFloat()
        val srcH = if (videoSize.height > 0) videoSize.height.toFloat() else item.displayHeight.toFloat()
        val boxW = maxWidth.value
        val boxH = maxHeight.value
        val scale = if (srcW > 0f && srcH > 0f) {
            if (srcH >= srcW) max(boxW / srcW, boxH / srcH) else min(boxW / srcW, boxH / srcH)
        } else {
            0f
        }
        val surfaceW = if (scale > 0f) (srcW * scale).dp else maxWidth
        val surfaceH = if (scale > 0f) (srcH * scale).dp else maxHeight

        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .clipToBounds()
                .offset { IntOffset(0, dismiss.value.roundToInt()) },
            contentAlignment = Alignment.Center
        ) {
            PlayerSurface(
                player = player,
                surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
                modifier = Modifier.requiredSize(surfaceW, surfaceH)
            )
            if (!firstFrame) {
                AsyncImage(
                    model = MediaThumb(uri, 1080),
                    contentDescription = null,
                    imageLoader = imageLoader,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.requiredSize(surfaceW, surfaceH)
                )
            }
        }

        // Dokunma katmanı (kontrollerin altında).
        Box(
            Modifier
                .fillMaxSize()
                .pointerInputTaps(
                    onTap = { setChrome(!chromeNow) },
                    onDoubleTap = { left -> seekBy(if (left) -SeekStepMs else SeekStepMs) }
                )
                .pointerInputDismiss(
                    onDrag = { dy -> scope.launch { dismiss.snapTo(max(0f, dismiss.value + dy)) } },
                    onEnd = { velocity ->
                        if (dismiss.value > with(density) { 120.dp.toPx() } || velocity > 1400f) {
                            close()
                        } else {
                            scope.launch { dismiss.animateTo(0f, GalleryMotion.standard()) }
                        }
                    }
                )
        )

        // ±10 sn göstergesi
        if (seekHint != 0) {
            GlassCapsule(
                backdrop = backdrop,
                tone = GlassTone.Dark,
                modifier = Modifier
                    .align(if (seekHint < 0) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 40.dp)
                    .height(40.dp)
            ) {
                BasicText(
                    stringResource(if (seekHint < 0) R.string.video_seek_back else R.string.video_seek_forward),
                    style = GalleryType.Button.copy(color = Color.White, fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
            }
        }

        if (failed) {
            BasicText(
                stringResource(R.string.video_error),
                style = GalleryType.Body.copy(color = Color.White),
                modifier = Modifier.align(Alignment.Center)
            )
        }

        AnimatedVisibility(
            visible = chromeVisible && !inPip,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(Modifier.fillMaxSize()) {
                // Okunabilirlik için üst ve alt koyu geçiş (tasarım: %35 / %40).
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent)))
                )
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(260.dp)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f))))
                )

                // Üst çubuk
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = topInset + 8.dp, start = 14.dp, end = 14.dp)
                ) {
                    GlassIconButton(
                        backdrop = backdrop,
                        icon = GalleryIcons.Back,
                        contentDescription = stringResource(R.string.viewer_back),
                        onClick = { close() },
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
                                title,
                                style = GalleryType.Button.copy(color = Color.White, fontWeight = FontWeight.Bold, lineHeight = 18.sp),
                                maxLines = 1
                            )
                            BasicText(
                                subtitle,
                                style = GalleryType.Duration.copy(
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 15.sp
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }

                // Rozet (sol) ve hız (sağ)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = topInset + 70.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val badge = formatBadge(format, videoSize)
                    if (badge != null) {
                        GlassCapsule(
                            backdrop = backdrop,
                            tone = GlassTone.Dark,
                            modifier = Modifier.height(28.dp)
                        ) {
                            BasicText(
                                badge,
                                style = GalleryType.Duration.copy(
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                modifier = Modifier.padding(horizontal = 11.dp)
                            )
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }
                    GlassTextButton(
                        backdrop = backdrop,
                        text = speedLabel(SpeedSteps[speedIndex], locale),
                        onClick = {
                            speedIndex = (speedIndex + 1) % SpeedSteps.size
                            player.setPlaybackSpeed(SpeedSteps[speedIndex])
                        },
                        tone = GlassTone.Dark,
                        textColor = Color.White,
                        height = 28.dp,
                        modifier = Modifier.semantics { contentDescription = context.getString(R.string.video_speed) }
                    )
                }

                // Duraklatınca ortada büyük cam oynat düğmesi
                if (!playing && !failed) {
                    GlassIconButton(
                        backdrop = backdrop,
                        icon = GalleryIcons.Play,
                        contentDescription = stringResource(R.string.video_play),
                        onClick = { togglePlay() },
                        tone = GlassTone.Dark,
                        size = 84.dp,
                        iconSize = 36.dp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // Alt: süre, cam kapsül (oynat · film şeridi · ses), araç çubuğu
                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = bottomInset + 32.dp)
                ) {
                    val timeStyle = GalleryType.Duration.copy(
                        color = Color.White,
                        fontFeatureSettings = "tnum",
                        shadow = Shadow(Color(0x99000000), blurRadius = 6f)
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 26.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BasicText(Formatters.duration(positionMs), style = timeStyle)
                        BasicText("-" + Formatters.duration(max(0L, durationMs - positionMs)), style = timeStyle)
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(
                        Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .height(60.dp)
                            .glass(backdrop, Capsule(), GlassTone.Dark, refraction = false)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CapsuleSlot(
                            icon = if (playing) GalleryIcons.Pause else GalleryIcons.Play,
                            description = stringResource(if (playing) R.string.video_pause else R.string.video_play),
                            onClick = { togglePlay() }
                        )
                        FilmStrip(
                            frames = frames,
                            progress = if (durationMs > 0L) positionMs.toFloat() / durationMs else 0f,
                            description = stringResource(R.string.video_seek),
                            onScrubStart = {
                                scrubbing = true
                                player.pause()
                            },
                            onScrub = { seekToFraction(it) },
                            onScrubEnd = {
                                scrubbing = false
                                player.play()
                            },
                            onTapAt = { seekToFraction(it) },
                            modifier = Modifier.weight(1f)
                        )
                        CapsuleSlot(
                            icon = if (muted) GalleryIcons.SoundOff else GalleryIcons.SoundOn,
                            description = stringResource(if (muted) R.string.video_sound_on else R.string.video_sound_off),
                            onClick = { toggleMute() }
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    toolbar(backdrop)
                }
            }
        }
    }
}

@Composable
private fun CapsuleSlot(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Glyph(icon, Color.White, size = 22.dp)
    }
}

/** Videodan çıkarılmış kareler; oynatılan kısım hafif koyu, beyaz çizgi oynatma yeri. */
@Composable
private fun FilmStrip(
    frames: List<ImageBitmap>,
    progress: Float,
    description: String,
    onScrubStart: () -> Unit,
    onScrub: (Float) -> Unit,
    onScrubEnd: () -> Unit,
    onTapAt: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    BoxWithConstraints(
        modifier
            .height(42.dp)
            .semantics { contentDescription = description }
            .pointerInputScrub(onScrubStart, onScrub, onScrubEnd, onTapAt)
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .clip(shape)
                .background(Color.White.copy(alpha = 0.08f))
                .border(1.dp, Color.White.copy(alpha = 0.35f), shape)
        ) {
            frames.forEach { frame ->
                Image(
                    bitmap = frame,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }
        val p = progress.coerceIn(0f, 1f)
        Box(
            Modifier
                .fillMaxHeight()
                .width(maxWidth * p)
                .clip(shape)
                .background(Color.Black.copy(alpha = 0.28f))
        )
        Box(
            Modifier
                .offset(x = maxWidth * p - 2.dp, y = (-2).dp)
                .width(4.dp)
                .height(46.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White)
        )
    }
}

// ---------------------------------------------------------------- hareket yardımcıları

// Hareket bloklarındaki geri çağrılar kalıcı durum nesnelerini okur; bloklar yeniden başlatılmaz.
private fun Modifier.pointerInputTaps(onTap: () -> Unit, onDoubleTap: (left: Boolean) -> Unit): Modifier =
    pointerInput("taps") {
        detectTapGestures(
            onTap = { onTap() },
            onDoubleTap = { position -> onDoubleTap(position.x < size.width / 2f) }
        )
    }

private fun Modifier.pointerInputDismiss(onDrag: (Float) -> Unit, onEnd: (Float) -> Unit): Modifier =
    pointerInput("dismiss") {
        val tracker = VelocityTracker()
        detectVerticalDragGestures(
            onDragStart = { tracker.resetTracking() },
            onDragEnd = { onEnd(tracker.calculateVelocity().y) },
            onDragCancel = { onEnd(0f) },
            onVerticalDrag = { change, amount ->
                tracker.addPosition(change.uptimeMillis, change.position)
                change.consume()
                onDrag(amount)
            }
        )
    }

private fun Modifier.pointerInputScrub(
    onStart: () -> Unit,
    onScrub: (Float) -> Unit,
    onEnd: () -> Unit,
    onTapAt: (Float) -> Unit
): Modifier = this
    .pointerInput("scrubTap") {
        detectTapGestures { position -> onTapAt(position.x / size.width) }
    }
    .pointerInput("scrubDrag") {
        detectHorizontalDragGestures(
            onDragStart = { position ->
                onStart()
                onScrub(position.x / size.width)
            },
            onDragEnd = { onEnd() },
            onDragCancel = { onEnd() },
            onHorizontalDrag = { change, _ ->
                change.consume()
                onScrub(change.position.x / size.width)
            }
        )
    }

// ---------------------------------------------------------------- yardımcılar

/** "4K · 30", "HD · 60 · HDR" */
private fun formatBadge(format: Format?, size: VideoSize): String? {
    val w = format?.width?.takeIf { it > 0 } ?: size.width
    val h = format?.height?.takeIf { it > 0 } ?: size.height
    if (w <= 0 || h <= 0) return null
    val short = min(w, h)
    val resolution = when {
        short >= 2160 -> "4K"
        short >= 1440 -> "1440p"
        short >= 1080 -> "HD"
        else -> "${short}p"
    }
    val parts = mutableListOf(resolution)
    val fps = format?.frameRate ?: Format.NO_VALUE.toFloat()
    if (fps > 0f) parts += fps.roundToInt().toString()
    val transfer = format?.colorInfo?.colorTransfer
    if (transfer == C.COLOR_TRANSFER_ST2084 || transfer == C.COLOR_TRANSFER_HLG) parts += "HDR"
    return parts.joinToString(" · ")
}

/** 1.5 → "1,5×" */
private fun speedLabel(speed: Float, locale: Locale): String =
    DecimalFormat("0.#", DecimalFormatSymbols(locale)).format(speed.toDouble()) + "×"

/** Resim içinde Resim oranı Android'in izin verdiği aralıkta (1:2,39 – 2,39:1) olmalı. */
private fun pipRatio(size: VideoSize): Rational {
    val w = (size.width * size.pixelWidthHeightRatio).roundToInt()
    val h = size.height
    if (w <= 0 || h <= 0) return Rational(9, 16)
    val r = w.toFloat() / h
    return when {
        r > 2.39f -> Rational(239, 100)
        r < 1f / 2.39f -> Rational(100, 239)
        else -> Rational(w, h)
    }
}

internal fun extractFrames(context: Context, uri: Uri, durationMs: Long, sizePx: Int): List<ImageBitmap> {
    if (durationMs <= 0L) return emptyList()
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        (0 until StripFrames).mapNotNull { i ->
            val timeUs = durationMs * 1000L * (2 * i + 1) / (2 * StripFrames)
            retriever.getScaledFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, sizePx, sizePx)
                ?.asImageBitmap()
        }
    } catch (_: Exception) {
        emptyList()
    } finally {
        retriever.release()
    }
}

private fun Context.findComponentActivity(): ComponentActivity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is ComponentActivity) return ctx
        if (ctx is Activity) return null
        ctx = ctx.baseContext
    }
    return null
}
