package com.atasoy.gallerypro.feature.settings

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.atasoy.gallerypro.BuildConfig
import com.atasoy.gallerypro.R
import com.atasoy.gallerypro.data.LocalGalleryStores
import com.atasoy.gallerypro.data.clip.ClipAssets
import com.atasoy.gallerypro.data.index.IndexScheduler
import com.atasoy.gallerypro.data.media.Formatters
import com.atasoy.gallerypro.designsystem.GalleryColors
import com.atasoy.gallerypro.designsystem.GalleryDimens
import com.atasoy.gallerypro.designsystem.GalleryPerformance
import com.atasoy.gallerypro.designsystem.GalleryType
import com.atasoy.gallerypro.designsystem.GlassIconButton
import com.atasoy.gallerypro.designsystem.Glyph
import com.atasoy.gallerypro.designsystem.ProgressiveBlurEdge
import com.atasoy.gallerypro.designsystem.icons.GalleryIcons
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Ayarlar ve Hakkında: sürüm, akıllı arama durumu ve "Yeniden incele", gizlilik notu,
 * açık kaynak lisansları (APK'daki assets/licenses). Kütüphane "⋯" menüsünden açılır.
 */
@Composable
fun SettingsScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val locale = remember { Locale.getDefault() }
    val backdrop = rememberLayerBackdrop()
    val stores = LocalGalleryStores.current
    val status by stores.index.status.collectAsStateWithLifecycle()
    val indexed by stores.index.entries.collectAsStateWithLifecycle()
    val vectors by stores.index.vectors.collectAsStateWithLifecycle()
    val semantic = remember { ClipAssets.available(context) }
    var requested by remember { mutableStateOf(false) }
    var openLicense by rememberSaveable { mutableStateOf<String?>(null) }

    val licenses by produceState(emptyList<Pair<String, String>>()) {
        value = withContext(Dispatchers.IO) { Licenses.list(context) }
    }

    BackHandler {
        if (openLicense != null) openLicense = null else onClose()
    }

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        Modifier
            .fillMaxSize()
            .background(GalleryColors.LightBackground)
            .clickable(interactionSource = null, indication = null) {}
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .background(GalleryColors.LightBackground)
        ) {
            val license = openLicense
            if (license != null) {
                val text by produceState("", license) {
                    value = withContext(Dispatchers.IO) { Licenses.read(context, license) }
                }
                LazyColumn(contentPadding = PaddingValues(top = topInset + 72.dp, bottom = bottomInset + 24.dp)) {
                    item {
                        BasicText(
                            text,
                            style = GalleryType.Subtitle.copy(fontSize = 12.sp, lineHeight = 17.sp, color = GalleryColors.TextPrimary),
                            modifier = Modifier.padding(horizontal = GalleryDimens.ScreenPadding)
                        )
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(top = topInset + 72.dp, bottom = bottomInset + 24.dp)) {
                    item {
                        Group {
                            RowItem(
                                stringResource(R.string.settings_version),
                                "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
                            )
                        }
                    }
                    item {
                        val total = indexed.size
                        val body = when {
                            status.running -> stringResource(
                                R.string.index_running,
                                Formatters.count(status.done, locale),
                                Formatters.count(status.total, locale)
                            )
                            total == 0 -> stringResource(R.string.settings_index_none)
                            semantic -> stringResource(
                                R.string.settings_index_semantic,
                                Formatters.count(total, locale),
                                Formatters.count(vectors.size, locale)
                            )
                            else -> stringResource(R.string.settings_index_labels, Formatters.count(total, locale))
                        }
                        SectionTitle(stringResource(R.string.index_title))
                        Group {
                            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                                BasicText(body, style = GalleryType.Body.copy(color = GalleryColors.TextSecondary))
                                Spacer(Modifier.height(12.dp))
                                Box(
                                    Modifier
                                        .clip(CircleShape)
                                        .background(if (requested || status.running) GalleryColors.TextTertiary.copy(alpha = 0.2f) else GalleryColors.Accent)
                                        .clickable(enabled = !requested && !status.running, role = Role.Button) {
                                            requested = true
                                            IndexScheduler.runNow(context)
                                        }
                                        .padding(horizontal = 16.dp, vertical = 9.dp)
                                ) {
                                    BasicText(
                                        stringResource(if (requested || status.running) R.string.index_queued else R.string.settings_reindex),
                                        style = GalleryType.Button.copy(
                                            color = if (requested || status.running) GalleryColors.TextSecondary else Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                    item {
                        SectionTitle(stringResource(R.string.settings_appearance))
                        Group {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable(role = Role.Switch) {
                                        GalleryPerformance.setFullEffects(context, !GalleryPerformance.fullEffects)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    BasicText(stringResource(R.string.settings_effects), style = GalleryType.Body)
                                    BasicText(
                                        stringResource(R.string.settings_effects_body),
                                        style = GalleryType.Subtitle.copy(fontSize = 13.sp, color = GalleryColors.TextTertiary),
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Switch(GalleryPerformance.fullEffects)
                            }
                        }
                    }
                    item {
                        SectionTitle(stringResource(R.string.settings_privacy))
                        Group {
                            BasicText(
                                stringResource(R.string.settings_privacy_body),
                                style = GalleryType.Body.copy(color = GalleryColors.TextSecondary),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                            )
                        }
                    }
                    if (licenses.isNotEmpty()) {
                        item {
                            SectionTitle(stringResource(R.string.settings_licenses))
                            Group {
                                licenses.forEachIndexed { i, (file, title) ->
                                    if (i > 0) Divider()
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable(role = Role.Button) { openLicense = file }
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        BasicText(title, style = GalleryType.Body, modifier = Modifier.weight(1f))
                                        Glyph(GalleryIcons.ChevronRight, GalleryColors.Chevron, size = 18.dp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        ProgressiveBlurEdge(
            backdrop = backdrop,
            height = topInset + 80.dp,
            tint = Color.White,
            modifier = Modifier.align(Alignment.TopCenter)
        )
        Row(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = topInset + 10.dp, start = GalleryDimens.ScreenPadding, end = GalleryDimens.ScreenPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassIconButton(
                backdrop = backdrop,
                icon = GalleryIcons.Back,
                contentDescription = stringResource(R.string.viewer_back),
                onClick = { if (openLicense != null) openLicense = null else onClose() },
                tint = GalleryColors.TextPrimary
            )
            Spacer(Modifier.width(12.dp))
            BasicText(
                openLicense?.let { file -> licenses.firstOrNull { it.first == file }?.second }
                    ?: stringResource(R.string.settings_title),
                style = GalleryType.SectionTitle.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold)
            )
        }
    }
}

/** Basit anahtar (Material yok): yeşil kapsül, beyaz top. */
@Composable
private fun Switch(on: Boolean) {
    Box(
        Modifier
            .width(50.dp)
            .height(30.dp)
            .clip(CircleShape)
            .background(if (on) GalleryColors.SwitchOn else GalleryColors.TextTertiary.copy(alpha = 0.35f))
            .padding(2.dp),
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    BasicText(
        text,
        style = GalleryType.Duration.copy(color = GalleryColors.TextSecondary),
        modifier = Modifier.padding(start = GalleryDimens.ScreenPadding + 16.dp, top = 22.dp, bottom = 8.dp)
    )
}

@Composable
private fun Group(content: @Composable () -> Unit) {
    Column(
        Modifier
            .padding(horizontal = GalleryDimens.ScreenPadding)
            .fillMaxWidth()
            .clip(RoundedCornerShape(GalleryDimens.GroupedCorner))
            .background(GalleryColors.GroupedFill)
    ) {
        content()
    }
}

@Composable
private fun Divider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp)
            .height(0.5.dp)
            .background(GalleryColors.TextTertiary.copy(alpha = 0.3f))
    )
}

@Composable
private fun RowItem(title: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        BasicText(title, style = GalleryType.Body)
        BasicText(value, style = GalleryType.Body.copy(color = GalleryColors.TextSecondary))
    }
}

/** APK'daki assets/licenses klasörü: dosya adı → başlık. */
object Licenses {
    private const val Dir = "licenses"

    private val titles = mapOf(
        "inter-OFL.txt" to "Inter (SIL Open Font License 1.1)",
        "clip-models.txt" to "CLIP modelleri (MIT, Apache-2.0)",
        "geonames.txt" to "GeoNames (CC BY 4.0)"
    )

    fun list(context: android.content.Context): List<Pair<String, String>> =
        runCatching { context.assets.list(Dir).orEmpty().toList() }.getOrDefault(emptyList())
            .sorted()
            .map { it to (titles[it] ?: it.removeSuffix(".txt")) }

    fun read(context: android.content.Context, file: String): String =
        runCatching { context.assets.open("$Dir/$file").bufferedReader().readText() }.getOrDefault("")
}
