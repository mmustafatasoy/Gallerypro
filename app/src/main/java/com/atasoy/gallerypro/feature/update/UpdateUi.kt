package com.atasoy.gallerypro.feature.update

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.atasoy.gallerypro.R
import com.atasoy.gallerypro.data.update.AvailableUpdate
import com.atasoy.gallerypro.data.update.UpdateCheck
import com.atasoy.gallerypro.data.update.UpdateDownload
import com.atasoy.gallerypro.data.update.UpdateManager
import com.atasoy.gallerypro.designsystem.GalleryColors
import com.atasoy.gallerypro.designsystem.GalleryDimens
import com.atasoy.gallerypro.designsystem.GalleryType

@Composable
fun UpdatePrompt(update: AvailableUpdate, onDownload: () -> Unit, onLater: () -> Unit) {
    UpdateDialog(
        message = stringResource(R.string.updates_available, update.version),
        description = update.notes.ifBlank { stringResource(R.string.updates_privacy) },
        action = stringResource(R.string.updates_download),
        onAction = onDownload,
        onLater = onLater
    )
}

@Composable
fun UpdateReadyPrompt(version: String, onInstall: () -> Unit, onLater: () -> Unit) {
    UpdateDialog(
        message = stringResource(R.string.updates_ready, version),
        description = stringResource(R.string.updates_unknown_sources),
        action = stringResource(R.string.updates_install),
        onAction = onInstall,
        onLater = onLater
    )
}

/** Galeri etkileşimini engellemeden kullanıcıya yalnızca yeni sürümde bir kez öneri sunulur. */
@Composable
private fun UpdateDialog(
    message: String,
    description: String,
    action: String,
    onAction: () -> Unit,
    onLater: () -> Unit
) {
    Box(
        Modifier.fillMaxSize().background(GalleryColors.PreviewScrim)
            .clickable(interactionSource = null, indication = null) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.fillMaxWidth().padding(GalleryDimens.ScreenPadding)
                .background(GalleryColors.LightBackground,
                    RoundedCornerShape(GalleryDimens.GroupedCorner))
                .padding(GalleryDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(GalleryDimens.CardSpacing)
        ) {
            BasicText(
                stringResource(R.string.updates_title),
                style = GalleryType.SectionTitle.copy(color = GalleryColors.TextPrimary)
            )
            BasicText(message, style = GalleryType.Body.copy(
                color = GalleryColors.TextPrimary, fontWeight = FontWeight.SemiBold))
            BasicText(description, style = GalleryType.Subtitle.copy(
                color = GalleryColors.TextSecondary))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                UpdateButton(stringResource(R.string.updates_later), false, onLater)
                Spacer(Modifier.width(GalleryDimens.CardSpacing))
                UpdateButton(action, true, onAction)
            }
        }
    }
}

@Composable
private fun UpdateButton(text: String, primary: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.background(
            if (primary) GalleryColors.Accent else GalleryColors.GroupedFill,
            RoundedCornerShape(GalleryDimens.AlbumCorner)
        ).clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text,
            style = GalleryType.Button.copy(
                color = if (primary) Color.White else GalleryColors.TextPrimary
            )
        )
    }
}

@Composable
fun SettingsUpdateSection(manager: UpdateManager) {
    val check by manager.check.collectAsStateWithLifecycle()
    val download by manager.download.collectAsStateWithLifecycle()
    val available = (check as? UpdateCheck.Available)?.release
    val ready = download as? UpdateDownload.Ready
    val active = download as? UpdateDownload.Downloading

    BasicText(
        stringResource(R.string.updates_title),
        style = GalleryType.Duration.copy(color = GalleryColors.TextSecondary),
        modifier = Modifier.padding(
            start = GalleryDimens.ScreenPadding + 16.dp, top = 22.dp, bottom = 8.dp
        )
    )
    Column(
        Modifier.fillMaxWidth().padding(horizontal = GalleryDimens.ScreenPadding)
            .background(GalleryColors.GroupedFill, RoundedCornerShape(GalleryDimens.GroupedCorner))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(GalleryDimens.CardSpacing)
    ) {
        val status = when {
            ready != null -> stringResource(R.string.updates_ready, ready.version)
            active != null -> stringResource(R.string.updates_downloading,
                if (active.total > 0) ((active.received.coerceAtLeast(0) * 100 / active.total)
                    .coerceIn(0, 100)).toInt() else 0)
            download is UpdateDownload.Failed -> stringResource(R.string.updates_download_failed)
            available != null -> stringResource(R.string.updates_available, available.version)
            check is UpdateCheck.Checking -> stringResource(R.string.updates_checking)
            check is UpdateCheck.Failed -> stringResource(R.string.updates_failed)
            check is UpdateCheck.UpToDate -> stringResource(R.string.updates_current)
            else -> stringResource(R.string.updates_checking)
        }
        BasicText(status, style = GalleryType.Body.copy(color = GalleryColors.TextPrimary))
        if (available != null && available.notes.isNotBlank()) {
            BasicText(available.notes, style = GalleryType.Subtitle.copy(color = GalleryColors.TextSecondary))
        }
        BasicText(stringResource(R.string.updates_privacy),
            style = GalleryType.Subtitle.copy(color = GalleryColors.TextSecondary))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(GalleryDimens.GridSpacing)) {
            UpdateButton(
                stringResource(R.string.updates_check),
                primary = false,
                onClick = { manager.checkNow(force = true) }
            )
        }
        when {
            ready != null -> {
                UpdateButton(stringResource(R.string.updates_install), true) { manager.install() }
                BasicText(stringResource(R.string.updates_unknown_sources),
                    style = GalleryType.Subtitle.copy(color = GalleryColors.TextSecondary))
            }
            available != null && active == null -> {
                UpdateButton(stringResource(R.string.updates_download), true) {
                    manager.beginDownload(available)
                    manager.dismiss(available)
                }
            }
        }
    }
}
