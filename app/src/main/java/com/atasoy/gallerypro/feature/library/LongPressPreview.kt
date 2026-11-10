package com.atasoy.gallerypro.feature.library

import android.app.Activity
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import coil3.compose.AsyncImage
import com.atasoy.gallerypro.R
import com.atasoy.gallerypro.data.LocalGalleryStores
import com.atasoy.gallerypro.data.collections.Album
import com.atasoy.gallerypro.data.media.AlbumCopyLogic
import com.atasoy.gallerypro.data.media.LocalGalleryImageLoader
import com.atasoy.gallerypro.data.media.ManageMedia
import com.atasoy.gallerypro.data.media.MediaItem
import com.atasoy.gallerypro.data.media.MediaThumb
import com.atasoy.gallerypro.data.media.contentUri
import com.atasoy.gallerypro.data.media.copyItemToAlbum
import com.atasoy.gallerypro.data.media.shareItems
import com.atasoy.gallerypro.designsystem.GalleryColors
import com.atasoy.gallerypro.designsystem.GalleryDimens
import com.atasoy.gallerypro.designsystem.GalleryType
import com.atasoy.gallerypro.designsystem.GlassIconButton
import com.atasoy.gallerypro.designsystem.GlassMenu
import com.atasoy.gallerypro.designsystem.GlassMenuItem
import com.atasoy.gallerypro.designsystem.GlassToast
import com.atasoy.gallerypro.designsystem.GlassTone
import com.atasoy.gallerypro.designsystem.glass
import com.atasoy.gallerypro.designsystem.icons.GalleryIcons
import com.atasoy.gallerypro.feature.viewer.DeleteCard
import com.kyant.backdrop.Backdrop
import com.kyant.shapes.RoundedRectangle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Mevcut LibraryGrid combinedClickable ile çağrılır; koleksiyonlar da bu parçayı paylaşır. */
@Composable
internal fun LongPressPreview(
    item: MediaItem, albums: List<Album>, backdrop: Backdrop, onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val stores = LocalGalleryStores.current
    val scope = rememberCoroutineScope()
    var albumPicker by remember { mutableStateOf(false) }
    var deletePrompt by remember { mutableStateOf(false) }
    var copying by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    val albumOptions = remember(albums, item.isVideo) {
        albums.filter { AlbumCopyLogic.supported(it.path, item.isVideo) }
    }
    val doneText = stringResource(R.string.preview_copied)
    val errorText = stringResource(R.string.preview_failed)
    val shareTitle = stringResource(R.string.share_chooser)
    val needsAndroid11 = stringResource(R.string.trash_needs_android11)
    val favoriteNeedsAndroid11 = stringResource(R.string.favorite_needs_android11)
    val deleteMessage = stringResource(if (item.isVideo) R.string.viewer_delete_message_video else R.string.viewer_delete_message)
    val deleteAction = stringResource(if (item.isVideo) R.string.viewer_delete_video else R.string.viewer_delete_photo)

    val favoriteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        if (it.resultCode == Activity.RESULT_OK) onDismiss()
    }
    val trashLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        if (it.resultCode == Activity.RESULT_OK) onDismiss()
    }
    LaunchedEffect(notice) { if (notice != null) { delay(1800); notice = null } }
    BackHandler {
        when {
            copying -> Unit
            deletePrompt -> deletePrompt = false
            albumPicker -> albumPicker = false
            else -> onDismiss()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(GalleryColors.PreviewScrim)
            .clickable(enabled = !copying) { onDismiss() })
        AsyncImage(
            model = MediaThumb(item.contentUri(), 1024, item.modifiedMillis),
            imageLoader = LocalGalleryImageLoader.current,
            contentDescription = null, contentScale = ContentScale.Fit,
            modifier = Modifier.align(Alignment.Center).fillMaxWidth()
                .height(GalleryDimens.PreviewHeight).padding(horizontal = GalleryDimens.ScreenPadding)
        )
        GlassIconButton(
            backdrop = backdrop, icon = GalleryIcons.Close,
            contentDescription = stringResource(R.string.viewer_back),
            onClick = onDismiss, tone = GlassTone.Dark,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 48.dp, end = GalleryDimens.ScreenPadding)
        )
        GlassMenu(
            backdrop = backdrop, visible = !albumPicker && !deletePrompt && !copying,
            onDismiss = onDismiss, tone = GlassTone.Dark,
            modifier = Modifier.align(Alignment.BottomCenter)
                .navigationBarsPadding().padding(bottom = 24.dp),
            items = listOf(
                GlassMenuItem(stringResource(R.string.action_share), false) {
                    if (context.shareItems(listOf(item), shareTitle)) onDismiss()
                    else notice = context.getString(R.string.toast_no_app)
                },
                GlassMenuItem(
                    stringResource(if (item.isFavorite) R.string.viewer_unfavorite else R.string.viewer_favorite),
                    false
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val request = MediaStore.createFavoriteRequest(
                            context.contentResolver, listOf(item.contentUri()), !item.isFavorite
                        )
                        favoriteLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
                    } else notice = favoriteNeedsAndroid11
                },
                GlassMenuItem(stringResource(R.string.preview_add_album), false) { albumPicker = true },
                GlassMenuItem(stringResource(R.string.menu_hide), false) {
                    stores.hidden.hide(listOf(item.id)); onDismiss()
                },
                GlassMenuItem(stringResource(R.string.action_delete), false) { deletePrompt = true }
            )
        )
        if (albumPicker) {
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
                    .padding(horizontal = GalleryDimens.ScreenPadding, vertical = 24.dp)
                    .fillMaxWidth()
                    .glass(backdrop, RoundedRectangle(GalleryDimens.CardCorner), GlassTone.Sheet, refraction = false)
                    .padding(16.dp)
            ) {
                BasicText(stringResource(R.string.preview_select_album),
                    style = GalleryType.SectionTitle.copy(color = Color.White))
                if (albumOptions.isEmpty()) {
                    BasicText(stringResource(R.string.preview_no_albums),
                        style = GalleryType.Body.copy(color = Color.White),
                        modifier = Modifier.padding(vertical = 14.dp))
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = GalleryDimens.PreviewAlbumListHeight)) {
                        items(albumOptions, key = { it.path }) { album ->
                            BasicText(album.name,
                                style = GalleryType.Body.copy(color = Color.White),
                                modifier = Modifier.fillMaxWidth().clickable(enabled = !copying) {
                                    copying = true
                                    albumPicker = false
                                    scope.launch {
                                        val ok = withContext(Dispatchers.IO) {
                                            context.copyItemToAlbum(item, album.path)
                                        }
                                        copying = false
                                        notice = if (ok) doneText else errorText
                                        if (ok) { delay(900); onDismiss() } else albumPicker = true
                                    }
                                }.padding(horizontal = 8.dp, vertical = 14.dp))
                        }
                    }
                }
                BasicText(stringResource(R.string.viewer_cancel),
                    style = GalleryType.Button.copy(color = Color.White),
                    modifier = Modifier.align(Alignment.End)
                        .clickable { albumPicker = false }.padding(12.dp))
            }
        }
        if (deletePrompt) {
            DeleteCard(
                message = deleteMessage, action = deleteAction, backdrop = backdrop,
                showAccessHint = !ManageMedia.granted(context) && ManageMedia.supported,
                onGrantAccess = { deletePrompt = false; ManageMedia.request(context) },
                onConfirm = {
                    deletePrompt = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val request = MediaStore.createTrashRequest(
                            context.contentResolver, listOf(item.contentUri()), true
                        )
                        trashLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
                    } else notice = needsAndroid11
                },
                onCancel = { deletePrompt = false }
            )
        }
        GlassToast(
            backdrop = backdrop,
            message = if (copying) stringResource(R.string.preview_copying) else notice,
            tone = GlassTone.Dark,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 110.dp)
        )
    }
}
