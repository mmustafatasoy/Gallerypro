package com.atasoy.gallerypro.feature.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.width
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
import com.atasoy.gallerypro.data.media.DetailFormat
import com.atasoy.gallerypro.data.media.Formatters
import com.atasoy.gallerypro.data.media.LocalGalleryImageLoader
import com.atasoy.gallerypro.data.media.MediaDetails
import com.atasoy.gallerypro.data.media.MediaItem
import com.atasoy.gallerypro.data.media.MediaThumb
import com.atasoy.gallerypro.data.media.contentUri
import com.atasoy.gallerypro.data.media.displayHeight
import com.atasoy.gallerypro.data.media.displayWidth
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
 * Sıra dokümandaki gibi: başlık → tarih → kamera kartı → konum kartı → dosya kartı.
 * Akıllı Etiketler kartı 5. aşamada (yapay zekâ) eklenecek.
 */
@Composable
internal fun InfoPanelContent(
    item: MediaItem,
    details: MediaDetails?,
    caption: String,
    onCaptionChange: (String) -> Unit,
    onRequestLocation: () -> Unit,
    onOpenMap: (Double, Double) -> Unit,
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

            BasicText(
                text = remember(item.takenAtMillis) { fullDate(item.takenAtMillis, zone, locale) },
                style = GalleryType.Body.copy(color = Color.White.copy(alpha = 0.82f), fontSize = 14.sp),
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 14.dp)
            )

            CameraCard(item, details, locale)

            if (details != null && (details.hasLocation || details.locationHidden)) {
                Spacer(Modifier.height(12.dp))
                LocationCard(item, details, locale, onRequestLocation, onOpenMap)
            }

            Spacer(Modifier.height(12.dp))
            FileCard(item)
        }
    }
}

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
                coordinates,
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
