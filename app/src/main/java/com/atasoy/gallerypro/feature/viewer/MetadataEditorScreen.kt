package com.atasoy.gallerypro.feature.viewer

import android.Manifest
import android.app.Activity
import android.app.RecoverableSecurityException
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.atasoy.gallerypro.R
import com.atasoy.gallerypro.data.LocalGalleryStores
import com.atasoy.gallerypro.data.edit.MetadataEditLogic
import com.atasoy.gallerypro.data.edit.MetadataExifWriter
import com.atasoy.gallerypro.data.edit.MetadataUpdate
import com.atasoy.gallerypro.data.media.MediaDetails
import com.atasoy.gallerypro.data.media.MediaDetailsReader
import com.atasoy.gallerypro.data.media.MediaItem
import com.atasoy.gallerypro.data.media.contentUri
import com.atasoy.gallerypro.designsystem.GalleryColors
import com.atasoy.gallerypro.designsystem.GalleryDimens
import com.atasoy.gallerypro.designsystem.GalleryType
import com.atasoy.gallerypro.designsystem.GlassIconButton
import com.atasoy.gallerypro.designsystem.GlassTextButton
import com.atasoy.gallerypro.designsystem.GlassTone
import com.atasoy.gallerypro.designsystem.glass
import com.atasoy.gallerypro.designsystem.icons.GalleryIcons
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.RoundedRectangle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 7d: Sadece JPEG. Yazma izni Android sistem penceresinden alınır.
 * Dosya ve orijinal yedeği MetadataExifWriter içinde IO iş parçacığında işlenir.
 */
@Composable
internal fun MetadataEditorScreen(
    item: MediaItem,
    onSaved: (Long) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val edits = LocalGalleryStores.current.edits
    val backdrop = rememberLayerBackdrop()
    val scope = rememberCoroutineScope()
    val dateFormatHint = stringResource(R.string.metadata_date_hint)
    val invalidText = stringResource(R.string.metadata_invalid)
    val failedText = stringResource(R.string.metadata_failed)
    val permissionText = stringResource(R.string.metadata_permission_required)

    var dateText by remember(item.id) { mutableStateOf(MetadataEditLogic.formatDate(item.takenAtMillis)) }
    var latitude by remember(item.id) { mutableStateOf("") }
    var longitude by remember(item.id) { mutableStateOf("") }
    var locationTouched by remember(item.id) { mutableStateOf(false) }
    var removeLocation by remember(item.id) { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<MetadataUpdate?>(null) }

    val details by produceState<MediaDetails?>(null, item.id) {
        value = withContext(Dispatchers.IO) { MediaDetailsReader.read(context, item) }
    }
    LaunchedEffect(details) {
        val loaded = details
        if (!locationTouched && loaded != null) {
            latitude = loaded.latitude?.toString().orEmpty()
            longitude = loaded.longitude?.toString().orEmpty()
        }
    }

    fun save(update: MetadataUpdate) {
        if (saving) return
        saving = true
        error = null
        scope.launch {
            val successful = MetadataExifWriter.save(context, item, edits, update)
            saving = false
            pending = null
            if (successful) onSaved(update.epochMillis(java.time.ZoneId.systemDefault()))
            else error = failedText
        }
    }

    val writeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) pending?.let(::save)
        else { error = failedText; pending = null }
    }

    fun requestWrite(update: MetadataUpdate) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching {
                val request = MediaStore.createWriteRequest(
                    context.contentResolver, listOf(item.contentUri())
                )
                writeLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
            }.onFailure { error = failedText; pending = null }
        } else {
            try {
                context.contentResolver.openFileDescriptor(item.contentUri(), "rw")?.close()
                save(update)
            } catch (e: RecoverableSecurityException) {
                runCatching {
                    writeLauncher.launch(
                        IntentSenderRequest.Builder(e.userAction.actionIntent.intentSender).build()
                    )
                }.onFailure { error = failedText; pending = null }
            } catch (_: Exception) {
                error = failedText
                pending = null
            }
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) pending?.let(::requestWrite)
        else { error = permissionText; pending = null }
    }

    fun finish() {
        if (saving) return
        val update = MetadataEditLogic.parse(dateText, latitude, longitude, removeLocation)
        if (update == null) { error = invalidText; return }
        pending = update
        error = null
        if (!MediaDetailsReader.hasLocationPermission(context)) {
            runCatching { locationLauncher.launch(Manifest.permission.ACCESS_MEDIA_LOCATION) }
                .onFailure { error = permissionText; pending = null }
        } else requestWrite(update)
    }

    BackHandler { if (!saving) onClose() }

    Box(
        Modifier.fillMaxSize()
            .background(GalleryColors.DarkBackground)
            .layerBackdrop(backdrop)
    ) {
        Column(
            Modifier.fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = GalleryDimens.ScreenPadding, vertical = GalleryDimens.CardSpacing),
            verticalArrangement = Arrangement.spacedBy(GalleryDimens.CardSpacing)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassIconButton(
                    backdrop = backdrop,
                    icon = GalleryIcons.Back,
                    contentDescription = stringResource(R.string.viewer_back),
                    onClick = { if (!saving) onClose() },
                    tone = GlassTone.Dark,
                    tint = Color.White
                )
                BasicText(
                    stringResource(R.string.metadata_title),
                    style = GalleryType.SectionTitle.copy(color = Color.White)
                )
                GlassTextButton(
                    backdrop = backdrop,
                    text = stringResource(if (saving) R.string.metadata_saving else R.string.edit_done),
                    onClick = ::finish,
                    tone = GlassTone.Dark,
                    textColor = Color.White
                )
            }

            Column(
                Modifier.fillMaxWidth()
                    .glass(backdrop, RoundedRectangle(GalleryDimens.CardCorner), GlassTone.Sheet, refraction = false)
                    .padding(GalleryDimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(GalleryDimens.CardSpacing)
            ) {
                MetadataInput(
                    label = stringResource(R.string.metadata_date),
                    value = dateText,
                    hint = dateFormatHint,
                    keyboardType = KeyboardType.Text,
                    onChange = { dateText = it; error = null }
                )
                MetadataInput(
                    label = stringResource(R.string.metadata_latitude),
                    value = latitude,
                    hint = stringResource(R.string.metadata_coordinate_hint),
                    keyboardType = KeyboardType.Decimal,
                    onChange = { latitude = it; locationTouched = true; removeLocation = false; error = null }
                )
                MetadataInput(
                    label = stringResource(R.string.metadata_longitude),
                    value = longitude,
                    hint = stringResource(R.string.metadata_coordinate_hint),
                    keyboardType = KeyboardType.Decimal,
                    onChange = { longitude = it; locationTouched = true; removeLocation = false; error = null }
                )

                BasicText(
                    text = stringResource(if (removeLocation) R.string.metadata_remove_selected else R.string.metadata_remove_location),
                    style = GalleryType.Button.copy(
                        color = if (removeLocation) GalleryColors.Favorite else Color.White
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(GalleryDimens.AlbumCorner))
                        .clickable { removeLocation = !removeLocation; locationTouched = true; error = null }
                        .padding(vertical = GalleryDimens.CardSpacing)
                )
            }
            BasicText(
                stringResource(R.string.metadata_backup_note),
                style = GalleryType.Chip.copy(color = Color.White)
            )
            if (error != null) {
                BasicText(error.orEmpty(), style = GalleryType.Body.copy(color = GalleryColors.Delete))
            }
            Spacer(Modifier.height(GalleryDimens.TopButton))
        }
    }
}

/** Compose foundation girişleri; metinler Türkçe ve İngilizce string kaynaklarındadır. */
@Composable
private fun MetadataInput(
    label: String,
    value: String,
    hint: String,
    keyboardType: KeyboardType,
    onChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(GalleryDimens.GridSpacing)) {
        BasicText(label, style = GalleryType.Chip.copy(color = Color.White))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            textStyle = GalleryType.Body.copy(color = Color.White),
            cursorBrush = SolidColor(Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .height(GalleryDimens.MetadataInputHeight)
                .clip(RoundedCornerShape(GalleryDimens.AlbumCorner))
                .background(GalleryColors.EditToolInner)
                .padding(horizontal = GalleryDimens.ScreenPadding),
            decorationBox = { content ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        BasicText(hint, style = GalleryType.Chip.copy(color = GalleryColors.Chevron))
                    }
                    content()
                }
            }
        )
    }
}
