package com.atasoy.gallerypro.feature.edit

import android.Manifest
import android.app.Activity
import android.app.RecoverableSecurityException
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.atasoy.gallerypro.R
import com.atasoy.gallerypro.data.LocalGalleryStores
import com.atasoy.gallerypro.data.edit.AdjustTool
import com.atasoy.gallerypro.data.edit.AutoCorrection
import com.atasoy.gallerypro.data.edit.CropMath
import com.atasoy.gallerypro.data.edit.CropRatio
import com.atasoy.gallerypro.data.edit.EditFilter
import com.atasoy.gallerypro.data.edit.EditParams
import com.atasoy.gallerypro.data.edit.EditRenderer
import com.atasoy.gallerypro.data.edit.EditSaver
import com.atasoy.gallerypro.data.edit.SaveResult
import com.atasoy.gallerypro.data.media.LocalGalleryImageLoader
import com.atasoy.gallerypro.data.media.MediaItem
import com.atasoy.gallerypro.data.media.MediaThumb
import com.atasoy.gallerypro.data.media.contentUri
import com.atasoy.gallerypro.designsystem.GalleryColors
import com.atasoy.gallerypro.designsystem.GalleryDimens
import com.atasoy.gallerypro.designsystem.GalleryType
import com.atasoy.gallerypro.designsystem.GlassCapsule
import com.atasoy.gallerypro.designsystem.GlassIconButton
import com.atasoy.gallerypro.designsystem.GlassTone
import com.atasoy.gallerypro.designsystem.glass
import com.atasoy.gallerypro.designsystem.icons.GalleryIcons
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private const val TabAdjust = 0
private const val TabFilters = 1
private const val TabCrop = 2

/** Ayarlar ekran döndürmede kaybolmasın diye metin olarak saklanır. */
private val ParamsSaver = Saver<EditParams, String>(
    save = { it.encode() },
    restore = { EditParams.decode(it) ?: EditParams() }
)

private class Preview(val image: ImageBitmap, val thumb: ImageBitmap, val auto: AutoCorrection)

/**
 * Düzenle ekranı (4. aşama). Üstte Vazgeç · SIFIRLA/ORİJİNAL · Bitti; altta Ayarla,
 * Filtreler ve Kırp sekmeleri. Önizleme GPU'da, kayıt tam çözünürlükte (EditSaver).
 *
 * [onClose] kapanınca gösterilecek kısa bildirimle (veya null) çağrılır.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditScreen(
    item: MediaItem,
    onClose: (message: String?) -> Unit
) {
    val context = LocalContext.current
    val edits = LocalGalleryStores.current.edits
    val imageLoader = LocalGalleryImageLoader.current
    val scope = rememberCoroutineScope()
    val locale = remember { Locale.getDefault() }
    val backdrop = rememberLayerBackdrop()

    val initial = remember(item.id) { edits.params(item.id) ?: EditParams() }
    var params by rememberSaveable(item.id, stateSaver = ParamsSaver) { mutableStateOf(initial) }
    var tab by rememberSaveable { mutableIntStateOf(TabAdjust) }
    var tool by rememberSaveable { mutableIntStateOf(AdjustTool.Exposure.ordinal) }
    var compare by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }

    val preview by produceState<Preview?>(null, item.id) {
        value = withContext(Dispatchers.Default) {
            runCatching {
                val bitmap: Bitmap = EditRenderer.decodePreview(context, item, edits)
                Preview(
                    image = bitmap.asImageBitmap(),
                    thumb = EditRenderer.filterThumb(bitmap).asImageBitmap(),
                    auto = EditRenderer.measureAuto(bitmap)
                )
            }.getOrNull()
        }
    }

    // ---------------------------------------------------------------- kaydetme
    val savedCopyText = stringResource(R.string.edit_saved_copy)
    val restoredText = stringResource(R.string.edit_restored)
    val failedText = stringResource(R.string.edit_failed)

    fun runSave(inPlace: Boolean) {
        saving = true
        scope.launch {
            val result = EditSaver.save(context, item, params, edits, inPlace)
            imageLoader.memoryCache?.clear()
            saving = false
            onClose(
                when (result) {
                    SaveResult.Saved -> null
                    SaveResult.SavedAsCopy -> savedCopyText
                    SaveResult.Restored -> restoredText
                    SaveResult.Failed -> failedText
                }
            )
        }
    }

    val writeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) runSave(inPlace = true)
    }

    /** Aynı dosyanın üstüne yazmak için Android'den izin iste, sonra kaydet. */
    fun saveInPlace() {
        val uri = item.contentUri()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val request = MediaStore.createWriteRequest(context.contentResolver, listOf(uri))
            writeLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
        } else {
            scope.launch {
                val denied = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openFileDescriptor(uri, "rw")?.close()
                        null
                    } catch (e: RecoverableSecurityException) {
                        e
                    } catch (e: SecurityException) {
                        null
                    }
                }
                if (denied == null) {
                    runSave(inPlace = true)
                } else {
                    writeLauncher.launch(IntentSenderRequest.Builder(denied.userAction.actionIntent.intentSender).build())
                }
            }
        }
    }

    fun proceed(locationGranted: Boolean) {
        val restoring = params.isIdentity && edits.hasBackup(item.id)
        val inPlace = restoring || edits.hasBackup(item.id) || EditSaver.canSaveInPlace(item, locationGranted)
        if (inPlace) saveInPlace() else runSave(inPlace = false)
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> proceed(granted) }

    fun finish() {
        if (saving) return
        if (params == initial) {
            onClose(null)
            return
        }
        // Konum izni olmadan okunan orijinalde konum silinir; yedek eksik kalmasın diye önce iste.
        if (!EditSaver.hasLocationAccess(context) && !edits.hasBackup(item.id)) {
            locationLauncher.launch(Manifest.permission.ACCESS_MEDIA_LOCATION)
        } else {
            proceed(EditSaver.hasLocationAccess(context))
        }
    }

    fun requestClose() {
        when {
            saving -> Unit
            params != initial -> confirmDiscard = true
            else -> onClose(null)
        }
    }

    BackHandler {
        if (confirmDiscard) confirmDiscard = false else requestClose()
    }

    val topInset = WindowInsets.statusBarsIgnoringVisibility.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues().calculateBottomPadding()
    val controlsHeight = GalleryDimens.EditPanelHeight + 14.dp + GalleryDimens.TabBarHeight + 20.dp

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(interactionSource = null, indication = null) {}
    ) {
        // ---------------------------------------------------------------- camın arkası: zemin + fotoğraf
        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .background(Color.Black)
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                AsyncImage(
                    model = MediaThumb(item.contentUri(), 128, item.modifiedMillis),
                    contentDescription = null,
                    imageLoader = imageLoader,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = 1.2f
                            scaleY = 1.2f
                        }
                        .blur(50.dp)
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f))
                )
            }
            val shown = preview
            if (shown != null) {
                EditPreview(
                    image = shown.image,
                    params = params,
                    showOriginal = compare,
                    cropMode = tab == TabCrop,
                    onCropChange = { params = params.copy(crop = it) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = topInset + 72.dp,
                            bottom = bottomInset + controlsHeight + 12.dp,
                            start = 26.dp,
                            end = 26.dp
                        )
                )
            }
        }

        // ---------------------------------------------------------------- üst çubuk
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = topInset + 8.dp, start = 14.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            GlassIconButton(
                backdrop = backdrop,
                icon = GalleryIcons.Close,
                contentDescription = stringResource(R.string.edit_cancel),
                onClick = ::requestClose,
                tone = GlassTone.Dark,
                size = GalleryDimens.ViewerTopButton,
                iconSize = 20.dp
            )
            GlassCapsule(
                backdrop = backdrop,
                tone = GlassTone.Dark,
                modifier = Modifier.height(40.dp)
            ) {
                Spacer(Modifier.width(4.dp))
                PillButton(stringResource(R.string.edit_reset), selected = false) {
                    compare = false
                    params = EditParams(auto = params.auto)
                }
                Spacer(Modifier.width(2.dp))
                PillButton(stringResource(R.string.edit_original), selected = compare) { compare = !compare }
                Spacer(Modifier.width(4.dp))
            }
            DoneButton(stringResource(R.string.edit_done), onClick = ::finish)
        }

        // ---------------------------------------------------------------- alt: araçlar + sekmeler
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = bottomInset + 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(GalleryDimens.EditPanelHeight),
                contentAlignment = Alignment.TopCenter
            ) {
                when (tab) {
                    TabAdjust -> AdjustPanel(
                        params = params,
                        tool = AdjustTool.entries[tool],
                        locale = locale,
                        onSelectTool = { picked ->
                            tool = picked.ordinal
                            compare = false
                            // Otomatik ilk seçildiğinde fotoğrafa göre ölçülen düzeltme yarım güçte uygulanır.
                            val measured = preview?.auto
                            if (picked == AdjustTool.Auto && params.value(AdjustTool.Auto) == 0 && measured != null) {
                                params = params.copy(auto = measured).with(AdjustTool.Auto, 50)
                            }
                        },
                        onValue = { value ->
                            compare = false
                            val measured = preview?.auto
                            val current = AdjustTool.entries[tool]
                            params = if (current == AdjustTool.Auto && params.auto == AutoCorrection() && measured != null) {
                                params.copy(auto = measured).with(current, value)
                            } else {
                                params.with(current, value)
                            }
                        }
                    )
                    TabFilters -> FiltersPanel(
                        thumb = preview?.thumb,
                        selected = params.filter,
                        locale = locale,
                        onSelect = {
                            compare = false
                            params = params.copy(filter = it)
                        }
                    )
                    else -> CropPanel(
                        params = params,
                        onAngle = {
                            compare = false
                            params = params.copy(angle = it)
                        },
                        onRatio = { ratio ->
                            compare = false
                            val image = preview?.image
                            params = if (ratio == CropRatio.Free || image == null) {
                                params.copy(ratio = ratio)
                            } else {
                                params.copy(
                                    ratio = ratio,
                                    crop = CropMath.ratioRect(ratio.ratio, image.width.toFloat(), image.height.toFloat())
                                )
                            }
                        }
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier
                    .height(GalleryDimens.TabBarHeight)
                    .glass(backdrop, Capsule(), GlassTone.Dark)
                    .padding(GalleryDimens.TabBarInner),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                EditTab(GalleryIcons.TabAdjust, stringResource(R.string.edit_tab_adjust), tab == TabAdjust) { tab = TabAdjust }
                EditTab(GalleryIcons.TabFilters, stringResource(R.string.edit_tab_filters), tab == TabFilters) { tab = TabFilters }
                EditTab(GalleryIcons.TabCrop, stringResource(R.string.edit_tab_crop), tab == TabCrop) { tab = TabCrop }
            }
        }

        // ---------------------------------------------------------------- vazgeç onayı
        if (confirmDiscard) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(interactionSource = null, indication = null) { confirmDiscard = false }
            )
        }
        AnimatedVisibility(
            visible = confirmDiscard,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 12.dp, end = 12.dp, bottom = bottomInset + 28.dp)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .glass(backdrop, RoundedRectangle(30.dp), GlassTone.Sheet, refraction = false)
                    .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 14.dp)
            ) {
                BasicText(
                    stringResource(R.string.edit_discard_message),
                    style = GalleryType.Body.copy(
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
                SheetButton(stringResource(R.string.edit_discard), GalleryColors.Delete, FontWeight.Bold) {
                    confirmDiscard = false
                    onClose(null)
                }
                Spacer(Modifier.height(8.dp))
                SheetButton(stringResource(R.string.edit_keep), Color.White.copy(alpha = 0.14f), FontWeight.SemiBold) {
                    confirmDiscard = false
                }
            }
        }

        // ---------------------------------------------------------------- yükleniyor / kaydediliyor
        if (preview == null || saving) {
            Box(
                Modifier
                    .fillMaxSize()
                    .then(if (saving) Modifier.background(Color.Black.copy(alpha = 0.45f)) else Modifier)
                    .clickable(interactionSource = null, indication = null) {},
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spinner()
                    if (saving) {
                        Spacer(Modifier.height(12.dp))
                        BasicText(
                            stringResource(R.string.edit_saving),
                            style = GalleryType.Button.copy(color = Color.White)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetButton(text: String, fill: Color, weight: FontWeight, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(CircleShape)
            .background(fill)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        BasicText(text, style = GalleryType.Button.copy(color = Color.White, fontSize = 16.sp, fontWeight = weight))
    }
}

// -------------------------------------------------------------------- sekme içerikleri

@Composable
private fun toolName(tool: AdjustTool): String = stringResource(
    when (tool) {
        AdjustTool.Auto -> R.string.edit_tool_auto
        AdjustTool.Exposure -> R.string.edit_tool_exposure
        AdjustTool.Brightness -> R.string.edit_tool_brightness
        AdjustTool.Contrast -> R.string.edit_tool_contrast
        AdjustTool.Saturation -> R.string.edit_tool_saturation
        AdjustTool.Warmth -> R.string.edit_tool_warmth
        AdjustTool.Vignette -> R.string.edit_tool_vignette
        AdjustTool.Sharpness -> R.string.edit_tool_sharpness
    }
)

private fun toolIcon(tool: AdjustTool): ImageVector = when (tool) {
    AdjustTool.Auto -> GalleryIcons.ToolAuto
    AdjustTool.Exposure -> GalleryIcons.ToolExposure
    AdjustTool.Brightness -> GalleryIcons.ToolBrightness
    AdjustTool.Contrast -> GalleryIcons.ToolContrast
    AdjustTool.Saturation -> GalleryIcons.ToolSaturation
    AdjustTool.Warmth -> GalleryIcons.ToolWarmth
    AdjustTool.Vignette -> GalleryIcons.ToolVignette
    AdjustTool.Sharpness -> GalleryIcons.ToolSharpness
}

@Composable
private fun AdjustPanel(
    params: EditParams,
    tool: AdjustTool,
    locale: Locale,
    onSelectTool: (AdjustTool) -> Unit,
    onValue: (Int) -> Unit
) {
    val name = toolName(tool)
    val value = params.value(tool)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ToolLabel(name.uppercase(locale), signed(value))
        Spacer(Modifier.height(8.dp))
        RulerSlider(
            value = value,
            range = -100..100,
            label = name,
            onValueChange = onValue,
            modifier = Modifier.padding(horizontal = 36.dp)
        )
        Spacer(Modifier.height(16.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(AdjustTool.entries) { entry ->
                ToolCircle(
                    icon = toolIcon(entry),
                    value = params.value(entry),
                    selected = entry == tool,
                    label = toolName(entry),
                    onClick = { onSelectTool(entry) }
                )
            }
        }
    }
}

@Composable
private fun filterName(filter: EditFilter): String = stringResource(
    when (filter) {
        EditFilter.Original -> R.string.edit_filter_original
        EditFilter.Vivid -> R.string.edit_filter_vivid
        EditFilter.Dramatic -> R.string.edit_filter_dramatic
        EditFilter.Warm -> R.string.edit_filter_warm
        EditFilter.Cool -> R.string.edit_filter_cool
        EditFilter.Mono -> R.string.edit_filter_mono
        EditFilter.Silver -> R.string.edit_filter_silver
        EditFilter.Nostalgia -> R.string.edit_filter_nostalgia
    }
)

@Composable
private fun FiltersPanel(
    thumb: ImageBitmap?,
    selected: EditFilter,
    locale: Locale,
    onSelect: (EditFilter) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(4.dp))
        ToolLabel(filterName(selected).uppercase(locale), null)
        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(EditFilter.entries) { filter ->
                FilterChip(
                    thumb = thumb,
                    filter = filter,
                    name = filterName(filter),
                    selected = filter == selected,
                    onClick = { onSelect(filter) }
                )
            }
        }
    }
}

@Composable
private fun ratioName(ratio: CropRatio): String = when (ratio) {
    CropRatio.Free -> stringResource(R.string.edit_ratio_free)
    CropRatio.Square -> stringResource(R.string.edit_ratio_square)
    CropRatio.FourThree -> "4:3"
    CropRatio.SixteenNine -> "16:9"
    CropRatio.NineSixteen -> "9:16"
}

@Composable
private fun CropPanel(
    params: EditParams,
    onAngle: (Int) -> Unit,
    onRatio: (CropRatio) -> Unit
) {
    val straighten = stringResource(R.string.edit_straighten)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ToolLabel(straighten, signed(params.angle) + "°")
        Spacer(Modifier.height(8.dp))
        RulerSlider(
            value = params.angle,
            range = -45..45,
            label = straighten,
            onValueChange = onAngle,
            modifier = Modifier.padding(horizontal = 36.dp)
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CropRatio.entries.forEach { ratio ->
                RatioChip(ratioName(ratio), selected = ratio == params.ratio) { onRatio(ratio) }
            }
        }
    }
}
