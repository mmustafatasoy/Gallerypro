package com.atasoy.gallerypro.feature.updates

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.atasoy.gallerypro.R
import com.atasoy.gallerypro.data.updates.GithubUpdates
import com.atasoy.gallerypro.data.updates.UpdateCheck
import com.atasoy.gallerypro.data.updates.UpdateDownloads
import com.atasoy.gallerypro.data.updates.UpdateInfo
import com.atasoy.gallerypro.designsystem.GalleryColors
import com.atasoy.gallerypro.designsystem.GalleryDimens
import com.atasoy.gallerypro.designsystem.GalleryType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

val LocalUpdateLauncher = staticCompositionLocalOf<() -> Unit> { {} }

private sealed interface UiState {
    data object Checking : UiState
    data object Latest : UiState
    data object Unavailable : UiState
    data object SignatureMismatch : UiState
    data class New(val info: UpdateInfo) : UiState
    data class Downloading(val pending: UpdateDownloads.Pending) : UiState
    data class Ready(val pending: UpdateDownloads.Pending) : UiState
}

@Composable
fun UpdatePrompt(
    initial: UpdateInfo?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<UiState>(UiState.Checking) }
    var permissionHint by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val pending = UpdateDownloads.pending(context)
        state = if (pending != null) {
            UiState.Downloading(pending)
        } else {
            when (val result = initial?.let { UpdateCheck.Available(it) }
                ?: GithubUpdates.check(context)) {
                is UpdateCheck.Available -> UiState.New(result.info)
                UpdateCheck.Current -> UiState.Latest
                UpdateCheck.DifferentSigner -> UiState.SignatureMismatch
                UpdateCheck.Failed -> UiState.Unavailable
            }
        }
    }

    val downloading = (state as? UiState.Downloading)?.pending
    LaunchedEffect(downloading?.id) {
        val item = downloading ?: return@LaunchedEffect
        while (isActive) {
            when (withContext(Dispatchers.IO) { UpdateDownloads.status(context, item.id) }) {
                1 -> {
                    val valid = UpdateDownloads.verify(context, item)
                    if (valid) state = UiState.Ready(item)
                    else {
                        UpdateDownloads.clear(context, delete = true)
                        state = UiState.Unavailable
                    }
                    break
                }
                -1 -> {
                    UpdateDownloads.clear(context, delete = true)
                    state = UiState.Unavailable
                    break
                }
                else -> delay(900)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth()
                .background(GalleryColors.LightBackground,
                    RoundedCornerShape(GalleryDimens.CardCorner))
                .padding(GalleryDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BasicText(stringResource(R.string.update_title),
                style = GalleryType.SectionTitle)
            val description = when (val current = state) {
                UiState.Checking -> stringResource(R.string.update_checking)
                UiState.Latest -> stringResource(R.string.update_latest)
                UiState.Unavailable -> stringResource(R.string.update_error)
                UiState.SignatureMismatch -> stringResource(R.string.update_signature_mismatch)
                is UiState.New -> stringResource(
                    R.string.update_found, current.info.versionName)
                is UiState.Downloading -> stringResource(R.string.update_downloading)
                is UiState.Ready -> stringResource(R.string.update_downloaded)
            }
            BasicText(description, style = GalleryType.Body.copy(
                color = GalleryColors.TextSecondary))
            if (permissionHint) {
                BasicText(stringResource(R.string.update_allow_install),
                    style = GalleryType.Subtitle)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                UpdateButton(stringResource(R.string.update_later), false, onDismiss)
                when (val current = state) {
                    is UiState.New -> {
                        UpdateButton(stringResource(R.string.update_download), true) {
                            scope.launch {
                                state = try {
                                    UiState.Downloading(withContext(Dispatchers.IO) {
                                        UpdateDownloads.start(context, current.info)
                                    })
                                } catch (_: Exception) { UiState.Unavailable }
                            }
                        }
                    }
                    is UiState.Ready -> {
                        UpdateButton(stringResource(R.string.update_install), true) {
                            // Eğer ayarlarda bilinmeyen kaynak izni yoksa Android kendi izin
                            // ekranını açar. Döndüğünde kullanıcı tekrar Kur'a dokunur.
                            permissionHint = !UpdateDownloads.install(context, current.pending)
                        }
                    }
                    UiState.Latest, UiState.SignatureMismatch, UiState.Unavailable -> {
                        UpdateButton(stringResource(R.string.update_retry), true) {
                            scope.launch {
                                state = UiState.Checking
                                state = when (val checked = GithubUpdates.check(context)) {
                                    is UpdateCheck.Available -> UiState.New(checked.info)
                                    UpdateCheck.Current -> UiState.Latest
                                    UpdateCheck.DifferentSigner -> UiState.SignatureMismatch
                                    UpdateCheck.Failed -> UiState.Unavailable
                                }
                            }
                        }
                    }
                    else -> Unit
                }
            }
        }
    }
}

@Composable
private fun UpdateButton(label: String, primary: Boolean, onClick: () -> Unit) {
    BasicText(
        label,
        modifier = Modifier
            .padding(start = 6.dp)
            .background(
                if (primary) GalleryColors.Accent else GalleryColors.GroupedFill,
                RoundedCornerShape(14.dp)
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        style = GalleryType.Button.copy(
            color = if (primary) GalleryColors.LightBackground else GalleryColors.TextPrimary,
            textAlign = TextAlign.Center
        )
    )
}
