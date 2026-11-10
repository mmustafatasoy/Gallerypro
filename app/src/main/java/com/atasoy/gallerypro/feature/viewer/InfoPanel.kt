package com.atasoy.gallerypro.feature.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.atasoy.gallerypro.R
import com.atasoy.gallerypro.data.edit.MetadataEditLogic
import com.atasoy.gallerypro.data.index.IndexEntry
import com.atasoy.gallerypro.data.index.LabelDictionary
import com.atasoy.gallerypro.data.index.SearchText
import com.atasoy.gallerypro.data.media.DetailFormat
import com.atasoy.gallerypro.data.media.Formatters
import com.atasoy.gallerypro.data.media.LocalGalleryImageLoader
import com.atasoy.gallerypro.data.media.MediaDetails
import com.atasoy.gallerypro.data.media.MediaItem
import com.atasoy.gallerypro.data.media.MediaThumb
import com.atasoy.gallerypro.data.media.contentUri
import com.atasoy.gallerypro.data.media.displayHeight
import com.atasoy.gallerypro.data.media.displayWidth
import com.atasoy.gallerypro.data.places.PlaceEntry
import com.atasoy.gallerypro.data.places.PlaceNames
import com.atasoy.gallerypro.designsystem.GalleryType
import com.atasoy.gallerypro.designsystem.Glyph
import com.atasoy.gallerypro.designsystem.icons.GalleryIcons
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val CardShape = RoundedCornerShape(22.dp)
private val CardFill = Color.White.copy(alpha = 0.08f)
private val CardEdge = Color.White.copy(alpha = 0.16f)
private val Divider = Color.White.copy(alpha = 0.14f)
private val Secondary = Color.White.copy(alpha = 0.75f)
private val LinkBlue = Color(0xFF8CBCFF)

/**
 * Bilgi paneli içeriği (koyu cam yüzey dışarıda çizilir).
 * Sıra: başlık → tarih → kamera kartı → konum kartı → Akıllı Etiketler → Benzer Fotoğraflar → dosya kartı.
 * Son ikisi cihaz içi dizinden gelir; dizinde yoksa gösterilmez.
 */
@Composable
internal fun InfoPanelContent(
    item: MediaItem,
    details: MediaDetails?,
    caption: String,
    onCaptionChange: (String) -> Unit,
    dateMillis: Long,
    onAdjustMetadata: (() -> Unit)?,
    onRequestLocation: () -> Unit,
    onOpenMap: (Double, Double) -> Unit,
    place: PlaceEntry?,
    smart: IndexEntry?,
    similar: List<MediaItem>,
    onOpenSimilar: (MediaItem) -> Unit,
    onSearchLabel: ((String) -> Unit)?,
    onHandleDrag: (Float) -> Unit,
    onHandleDragEnd: (Float) -> Unit,
    bottomPadding: Dp,
    modifier: Modifier = Modifier
) {
    val locale = remember { Locale.getDefault() }
    val zone = remember { ZoneId.systemDefault() }
    val drag by rememberUpdatedState(onHandleDrag)
    val dragEnd by rememberUpdatedState(onHandleDragEnd)

    Column(modifier.fillMaxSize()) {
        // Tutamaç: aşağı çekince panel kapanır.
        Box(
            Modifier
                .fillMaxWidth()
                .height(28.dp)
                .pointerInput(Unit) {
                    val tracker = VelocityTracker()
                    detectVerticalDragGestures(
                        onDragStart = { tracker.resetTracking() },
                        onDragEnd = { dragEnd(tracker.calculateVelocity().y) },
                        onDragCancel = { dragEnd(0f) },
                        onVerticalDrag = { change, amount ->
                            tracker.addPosition(change.uptimeMillis, change.position)
                            change.consume()
                            drag(amount)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(38.dp, 5.dp)
                    .background(Color.White.copy(alpha = 0.35f), CircleShape)
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(PaddingValues(start = 16.dp, end = 16.dp, bottom = bottomPadding))
        ) {
            CaptionField(caption, onCaptionChange)

            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BasicText(
                    text = remember(dateMillis, zone, locale) { fullDate(dateMillis, zone, locale) },
                    style = GalleryType.Body.copy(color = Color.White.copy(alpha = 0.82f), fontSize = 14.sp),
                    modifier = Modifier.weight(1f)
                )
                if (onAdjustMetadata != null && MetadataEditLogic.isSupported(item.mimeType, item.isVideo)) {
                    BasicText(
                        stringResource(R.string.metadata_adjust),
                        style = GalleryType.Chip.copy(color = LinkBlue, fontWeight = FontWeight.Medium),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(role = Role.Button, onClick = onAdjustMetadata)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }
            }

            CameraCard(item, details, locale)

            if (details != null && (details.hasLocation || details.locationHidden)) {
                Spacer(Modifier.height(12.dp))
                LocationCard(item, details, place, locale, onRequestLocation, onOpenMap)
            }

            if (smart != null && (smart.labels.isNotEmpty() || smart.text.isNotBlank())) {
                Spacer(Modifier.height(12.dp))
                SmartLabelsCard(smart, locale, onSearchLabel)
            }

            if (similar.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                SimilarCard(similar, onOpenSimilar)
            }

            Spacer(Modifier.height(12.dp))
            FileCard(item)
        }
    }
}

/** Fotoğrafta tanınan nesneler (dokununca o sözcükle aranır) ve fotoğraftaki yazının başı. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SmartLabelsCard(entry: IndexEntry, locale: Locale, onSearch: ((String) -> Unit)?) {
    val turkishUi = locale.language == "tr"
    val words = remember(entry.labels, turkishUi) {
        entry.labels.map { SearchText.capitalize(LabelDictionary.display(it, turkishUi), locale) }.distinct()
    }
    val text = remember(entry.text) { entry.text.trim().replace(Regex("\\s+"), " ").take(MaxTextPreview) }
    Card {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            BasicText(
                stringResource(R.string.info_smart_labels),
                style = GalleryType.Duration.copy(color = Secondary)
            )
            if (words.isNotEmpty()) {
                FlowRow(
                    Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    words.forEach { word ->
                        BasicText(
                            word,
                            style = GalleryType.Chip.copy(color = Color.White),
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.14f))
                                .then(
                                    if (onSearch != null) {
                                        Modifier.clickable(role = Role.Button) { onSearch(word) }
                                    } else {
                                        Modifier
                                    }
                                )
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }
            if (text.isNotEmpty()) {
                BasicText(
                    stringResource(R.string.info_text_in_photo),
                    style = GalleryType.Duration.copy(color = Secondary),
                    modifier = Modifier.padding(top = if (words.isNotEmpty()) 14.dp else 8.dp)
                )
                BasicText(
                    if (entry.text.length > text.length) "$text…" else text,
                    style = GalleryType.Chip.copy(color = Color.White, fontWeight = FontWeight.Medium),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

/** Görünüşçe benzeyen fotoğraflar; dokununca görüntüleyici o fotoğrafa geçer. */
@Composable
private fun SimilarCard(list: List<MediaItem>, onOpen: (MediaItem) -> Unit) {
    val imageLoader = LocalGalleryImageLoader.current
    Card {
        BasicText(
            stringResource(R.string.info_similar),
            style = GalleryType.Duration.copy(color = Secondary),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(list, key = { it.id }) { other ->
                val uri = remember(other.id, other.isVideo) { other.contentUri() }
                AsyncImage(
                    model = MediaThumb(uri, 256, other.modifiedMillis),
                    contentDescription = null,
                    imageLoader = imageLoader,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(SimilarThumb)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable(role = Role.Button) { onOpen(other) }
                )
            }
        }
    }
}

private const val MaxTextPreview = 120
private val SimilarThumb = 76.dp

@Composable
private fun CaptionField(caption: String, onCaptionChange: (String) -> Unit) {
    val style = GalleryType.Body.copy(color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    BasicTextField(
        value = caption,
        onValueChange = onCaptionChange,
        singleLine = true,
        textStyle = style,
        cursorBrush = SolidColor(Color.White),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(horizontal = 4.dp),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (caption.isEmpty()) {
                    BasicText(
                        stringResource(R.string.info_caption_hint),
                        style = style.copy(color = Color.White.copy(alpha = 0.45f))
                    )
                }
                inner()
            }
        }
    )
}

@Composable
private fun Card(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(CardFill)
            .border(0.5.dp, CardEdge, CardShape)
    ) {
        content()
    }
}

@Composable
private fun CameraCard(item: MediaItem, details: MediaDetails?, locale: Locale) {
    val title = DetailFormat.deviceName(details?.make, details?.model)
        ?: stringResource(if (item.isVideo) R.string.info_video else R.string.info_photo)
    val badge = DetailFormat.formatBadge(item.mimeType)

    val focal = details?.focalLength35mm?.toDouble() ?: details?.focalLength
    val optics = listOfNotNull(
        focal?.let(DetailFormat::focal),
        details?.fNumber?.let { DetailFormat.fNumber(it, locale) }
    ).joinToString(" ")
    val lensLine = when {
        details?.lens != null && optics.isNotEmpty() -> "${details.lens} — $optics"
        details?.lens != null -> details.lens
        optics.isNotEmpty() -> optics
        else -> null
    }

    val w = item.displayWidth
    val h = item.displayHeight
    val sizeLine = listOfNotNull(
        if (item.isVideo && item.durationMs > 0) Formatters.duration(item.durationMs) else null,
        if (!item.isVideo && w > 0 && h > 0) {
            stringResource(R.string.info_megapixels, DetailFormat.megapixels(w, h, locale))
        } else {
            null
        },
        if (w > 0 && h > 0) "$w × $h" else null,
        if (item.sizeBytes > 0) DetailFormat.fileSize(item.sizeBytes, locale) else null
    ).joinToString(" · ")

    Card {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    title,
                    style = GalleryType.Button.copy(color = Color.White, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (badge != null) {
                    BasicText(
                        badge,
                        style = GalleryType.Duration.copy(
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.14f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            if (lensLine != null) {
                BasicText(
                    lensLine,
                    style = GalleryType.Chip.copy(color = Secondary, fontWeight = FontWeight.Medium),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            if (sizeLine.isNotEmpty()) {
                BasicText(
                    sizeLine,
                    style = GalleryType.Chip.copy(color = Secondary, fontWeight = FontWeight.Medium),
                    modifier = Modifier.padding(top = if (lensLine != null) 2.dp else 6.dp)
                )
            }
        }
        if (details != null && details.hasExposure) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Divider)
            )
            val cells = listOfNotNull(
                details.iso?.let { "ISO $it" },
                focal?.let(DetailFormat::focal),
                details.exposureBias?.let { DetailFormat.bias(it, locale) },
                details.fNumber?.let { DetailFormat.fNumber(it, locale) },
                details.exposureTime?.let { DetailFormat.exposure(it, locale) }
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                cells.forEachIndexed { index, text ->
                    if (index > 0) {
                        Box(
                            Modifier
                                .width(1.dp)
                                .height(18.dp)
                                .background(Divider)
                        )
                    }
                    BasicText(
                        text,
                        style = GalleryType.Duration.copy(color = Color.White.copy(alpha = 0.9f), textAlign = TextAlign.Center),
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationCard(
    item: MediaItem,
    details: MediaDetails,
    place: PlaceEntry?,
    locale: Locale,
    onRequestLocation: () -> Unit,
    onOpenMap: (Double, Double) -> Unit
) {
    val lat = details.latitude
    val lon = details.longitude
    if (lat == null || lon == null) {
        // İzin yok: Android konumu gizliyor.
        Card {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Glyph(GalleryIcons.Pin, Color.White, size = 18.dp)
                BasicText(
                    stringResource(R.string.info_location_permission),
                    style = GalleryType.Chip.copy(color = Color.White, fontWeight = FontWeight.Medium),
                    modifier = Modifier.weight(1f)
                )
                BasicText(
                    stringResource(R.string.info_location_allow),
                    style = GalleryType.Chip.copy(color = LinkBlue),
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClick = onRequestLocation)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }
        return
    }

    val imageLoader = LocalGalleryImageLoader.current
    val uri = remember(item.id, item.isVideo) { item.contentUri() }
    val coordinates = DetailFormat.coordinates(
        lat,
        lon,
        locale,
        north = stringResource(R.string.coord_north),
        south = stringResource(R.string.coord_south),
        east = stringResource(R.string.coord_east),
        west = stringResource(R.string.coord_west)
    )
    val mapLabel = stringResource(R.string.info_open_map)
    // Şehir listesinden bulunan yer adı varsa koordinat yerine o yazılır.
    val placeLine = place?.takeIf { it.hasPlace }?.let { p ->
        listOf(PlaceNames.cityLine(p), PlaceNames.country(p.country, locale)).filter { it.isNotEmpty() }.joinToString(" · ")
    }

    // İnternet izni olmadığı için harita karosu yok: kartın zemini fotoğrafın
    // bulanık kopyası, ortada fotoğraf iğnesi, altta koordinat ve "Haritada aç".
    Box(
        Modifier
            .fillMaxWidth()
            .height(176.dp)
            .clip(CardShape)
            .border(0.5.dp, Color.White.copy(alpha = 0.2f), CardShape)
    ) {
        AsyncImage(
            model = MediaThumb(uri, 256),
            contentDescription = null,
            imageLoader = imageLoader,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(28.dp)
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.28f))
        )
        AsyncImage(
            model = MediaThumb(uri, 256),
            contentDescription = null,
            imageLoader = imageLoader,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 32.dp)
                .size(52.dp)
                .shadow(10.dp, RoundedCornerShape(14.dp))
                .border(3.dp, Color.White, RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
        )
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(10.dp)
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xB31E1E24))
                .padding(start = 14.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Glyph(GalleryIcons.Pin, Color.White, size = 16.dp)
            BasicText(
                placeLine ?: coordinates,
                style = GalleryType.Chip.copy(color = Color.White),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            BasicText(
                mapLabel,
                style = GalleryType.Chip.copy(color = LinkBlue),
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(role = Role.Button) { onOpenMap(lat, lon) }
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun FileCard(item: MediaItem) {
    val name = item.displayName ?: return
    Card {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            BasicText(
                stringResource(R.string.info_file),
                style = GalleryType.Duration.copy(color = Secondary)
            )
            BasicText(
                name,
                style = GalleryType.Button.copy(color = Color.White),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
            val folder = item.relativePath?.trimEnd('/')
            if (!folder.isNullOrEmpty()) {
                BasicText(
                    folder,
                    style = GalleryType.Chip.copy(color = Secondary, fontWeight = FontWeight.Medium),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

/** "Cumartesi · 8 Kasım 2026 · 06:42" */
private fun fullDate(millis: Long, zone: ZoneId, locale: Locale): String {
    val time = Instant.ofEpochMilli(millis).atZone(zone)
    val text = DateTimeFormatter.ofPattern("EEEE · d MMMM yyyy · HH:mm", locale).format(time)
    return text.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
}
