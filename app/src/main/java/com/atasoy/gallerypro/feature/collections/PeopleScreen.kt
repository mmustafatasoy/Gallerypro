package com.atasoy.gallerypro.feature.collections

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.atasoy.gallerypro.R
import com.atasoy.gallerypro.data.media.LocalGalleryImageLoader
import com.atasoy.gallerypro.data.media.MediaItem
import com.atasoy.gallerypro.data.media.MediaThumb
import com.atasoy.gallerypro.data.media.contentUri
import com.atasoy.gallerypro.data.people.PeopleLogic
import com.atasoy.gallerypro.data.people.PeopleScanner
import com.atasoy.gallerypro.data.people.PeopleStore
import com.atasoy.gallerypro.data.people.PersonGroup
import com.atasoy.gallerypro.designsystem.GalleryColors
import com.atasoy.gallerypro.designsystem.GalleryDimens
import com.atasoy.gallerypro.designsystem.GalleryType
import com.atasoy.gallerypro.designsystem.GlassIconButton
import com.atasoy.gallerypro.designsystem.GlassTextButton
import com.atasoy.gallerypro.designsystem.GlassTone
import com.atasoy.gallerypro.designsystem.icons.GalleryIcons
import com.atasoy.gallerypro.feature.viewer.ViewerScreen
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Kişiler yalnızca kullanıcı istediğinde taranır. Yüz vektörleri ve isimler
 * uygulamanın özel belleğindedir. Hiçbir internet izni veya bulut işlem hattı yok.
 */
@Composable
internal fun PeopleScreen(
    photos: List<MediaItem>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backdrop = rememberLayerBackdrop()
    val loader = LocalGalleryImageLoader.current
    val store = remember { PeopleStore(context.applicationContext) }
    val available = remember(photos) { photos.filter { !it.isVideo }.associateBy { it.id } }
    var groups by remember { mutableStateOf<List<PersonGroup>>(emptyList()) }
    var scanning by remember { mutableStateOf(false) }
    var scanJob by remember { mutableStateOf<Job?>(null) }
    var done by remember { mutableIntStateOf(0) }
    var total by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<String?>(null) }
    var viewerId by remember { mutableStateOf<Long?>(null) }
    var naming by remember { mutableStateOf<String?>(null) }
    var nameInput by remember { mutableStateOf("") }
    var nameRevision by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }

    val failureText = stringResource(R.string.people_scan_failed)
    LaunchedEffect(available.keys) {
        val loaded = withContext(Dispatchers.IO) { store.load() }
        groups = withContext(Dispatchers.Default) {
            PeopleLogic.groups(loaded.filter {
                available[it.photoId]?.modifiedMillis == it.modifiedMillis
            })
        }
    }

    fun scan() {
        if (scanning) return
        scanning = true
        done = 0
        total = available.size
        error = null
        scanJob = scope.launch {
            try {
                val vectors = PeopleScanner(context.applicationContext).scan(available.values.toList()) { count, size ->
                    withContext(Dispatchers.Main) { done = count; total = size }
                }
                withContext(Dispatchers.IO) { store.save(vectors) }
                groups = withContext(Dispatchers.Default) { PeopleLogic.groups(vectors) }
            } catch (_: CancellationException) {
                // Kullanıcının iptali: henüz tamamlanmamış indeks kaydedilmez.
            } catch (_: Exception) {
                error = failureText
            } catch (_: LinkageError) {
                error = failureText
            } finally {
                scanning = false
                scanJob = null
            }
        }
    }

    fun back() {
        when {
            viewerId != null -> viewerId = null
            naming != null -> naming = null
            selected != null -> selected = null
            scanning -> scanJob?.cancel()
            else -> onBack()
        }
    }

    BackHandler { back() }

    val selectedGroup = groups.firstOrNull { it.key == selected }
    val selectedPhotos = selectedGroup?.photos?.mapNotNull { available[it] }.orEmpty()
    val currentGroupName = remember(nameRevision, selectedGroup?.key) {
        selectedGroup?.let { store.name(it.key) }.orEmpty()
    }

    Box(Modifier.fillMaxSize().background(GalleryColors.LightBackground).layerBackdrop(backdrop)) {
        Column(
            Modifier.fillMaxSize().padding(GalleryDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(GalleryDimens.CardSpacing)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                GlassIconButton(
                    backdrop = backdrop,
                    icon = GalleryIcons.Back,
                    contentDescription = stringResource(R.string.viewer_back),
                    onClick = ::back,
                    tint = GalleryColors.TextPrimary
                )
                BasicText(
                    stringResource(R.string.section_people),
                    style = GalleryType.SectionTitle
                )
                GlassTextButton(
                    backdrop = backdrop,
                    text = stringResource(if (scanning) R.string.people_cancel else R.string.people_scan),
                    onClick = { if (scanning) scanJob?.cancel() else scan() },
                    textColor = GalleryColors.TextPrimary
                )
            }

            BasicText(
                stringResource(R.string.people_privacy),
                style = GalleryType.Subtitle,
                modifier = Modifier.padding(vertical = GalleryDimens.GridSpacing)
            )

            if (scanning) {
                BasicText(
                    stringResource(R.string.people_scan_progress, done, total),
                    style = GalleryType.Body
                )
            }
            if (error != null) BasicText(error.orEmpty(),
                style = GalleryType.Body.copy(color = GalleryColors.Delete))

            if (selectedGroup == null) {
                if (groups.isEmpty() && !scanning) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        BasicText(
                            stringResource(R.string.people_empty),
                            style = GalleryType.Body
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(GalleryDimens.CardSpacing)
                    ) {
                        items(groups, key = { it.key }) { group ->
                            val cover = available[group.representative.photoId]
                                ?: group.photos.firstNotNullOfOrNull { available[it] }
                            Row(
                                Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(GalleryDimens.GroupedCorner))
                                    .background(GalleryColors.GroupedFill)
                                    .clickable { selected = group.key }
                                    .padding(GalleryDimens.CardSpacing),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(GalleryDimens.CardSpacing)
                            ) {
                                if (cover != null) AsyncImage(
                                    model = MediaThumb(cover.contentUri(), 256, cover.modifiedMillis),
                                    contentDescription = null,
                                    imageLoader = loader,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(74.dp)
                                        .clip(RoundedCornerShape(GalleryDimens.AlbumCorner))
                                )
                                Column(Modifier.weight(1f)) {
                                    // nameRevision ile yerel yeniden adlandırma ekrana yansır.
                                    val label = remember(nameRevision, group.key) { store.name(group.key) }
                                    BasicText(
                                        label.ifBlank { stringResource(R.string.people_unnamed) },
                                        style = GalleryType.Body
                                    )
                                    BasicText(
                                        stringResource(R.string.people_photo_count, group.photos.size),
                                        style = GalleryType.Subtitle
                                    )
                                }
                                GlassIconButton(
                                    backdrop = backdrop,
                                    icon = GalleryIcons.Edit,
                                    contentDescription = stringResource(R.string.people_name),
                                    onClick = {
                                        selected = group.key
                                        naming = group.key
                                        nameInput = store.name(group.key)
                                    },
                                    tint = GalleryColors.TextPrimary
                                )
                            }
                        }
                    }
                }
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicText(
                        currentGroupName.ifBlank { stringResource(R.string.people_unnamed) },
                        style = GalleryType.SectionTitle
                    )
                    GlassTextButton(
                        backdrop = backdrop,
                        text = stringResource(R.string.people_name),
                        textColor = GalleryColors.TextPrimary,
                        onClick = {
                            naming = selectedGroup.key
                            nameInput = store.name(selectedGroup.key)
                        }
                    )
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(bottom = GalleryDimens.BottomFadeHeight),
                    horizontalArrangement = Arrangement.spacedBy(GalleryDimens.GridSpacing),
                    verticalArrangement = Arrangement.spacedBy(GalleryDimens.GridSpacing)
                ) {
                    gridItems(selectedPhotos, key = { it.id }) { photo ->
                        AsyncImage(
                            model = MediaThumb(photo.contentUri(), 320, photo.modifiedMillis),
                            imageLoader = loader,
                            contentDescription = photo.displayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(120.dp)
                                .clip(RoundedCornerShape(GalleryDimens.AlbumCorner))
                                .clickable { viewerId = photo.id }
                        )
                    }
                }
            }
        }

        val editing = naming
        if (editing != null) {
            Box(
                Modifier.fillMaxSize().background(GalleryColors.PreviewScrim)
                    .clickable { naming = null },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(GalleryDimens.ScreenPadding)
                        .clip(RoundedCornerShape(GalleryDimens.GroupedCorner))
                        .background(GalleryColors.LightBackground)
                        .clickable(enabled = false) {}
                        .padding(GalleryDimens.ScreenPadding),
                    verticalArrangement = Arrangement.spacedBy(GalleryDimens.CardSpacing)
                ) {
                    BasicText(stringResource(R.string.people_name), style = GalleryType.SectionTitle)
                    BasicTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it.take(100) },
                        singleLine = true,
                        textStyle = GalleryType.Body.copy(color = GalleryColors.TextPrimary),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                            .clip(RoundedCornerShape(GalleryDimens.AlbumCorner))
                            .background(GalleryColors.GroupedFill)
                            .padding(GalleryDimens.CardSpacing)
                    )
                    GlassTextButton(
                        backdrop = backdrop,
                        text = stringResource(R.string.collections_done),
                        textColor = GalleryColors.TextPrimary,
                        onClick = {
                            store.setName(editing, nameInput)
                            nameRevision++
                            naming = null
                        }
                    )
                }
            }
        }

        if (viewerId != null && selectedPhotos.isNotEmpty()) {
            ViewerScreen(
                items = selectedPhotos,
                startId = viewerId!!,
                onClose = { viewerId = null }
            )
        }
    }
}
