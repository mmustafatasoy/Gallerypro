package com.atasoy.gallerypro

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.atasoy.gallerypro.data.LocalGalleryStores
import com.atasoy.gallerypro.data.media.LocalGalleryImageLoader
import com.atasoy.gallerypro.data.media.MediaAccess
import com.atasoy.gallerypro.data.media.MediaPermissions
import com.atasoy.gallerypro.feature.collections.CollectionsScreen
import com.atasoy.gallerypro.feature.collections.CollectionsViewModel
import com.atasoy.gallerypro.feature.library.LibraryScreen
import com.atasoy.gallerypro.feature.library.LibraryViewModel
import com.atasoy.gallerypro.feature.permission.PermissionScreen

@Composable
fun GalleryRoot(container: AppContainer) {
    val context = LocalContext.current
    var access by remember { mutableStateOf(MediaPermissions.access(context)) }
    var askedOnce by rememberSaveable { mutableStateOf(false) }
    var permanentlyDenied by remember { mutableStateOf(false) }
    // Önceki oturum çöktüyse ayrıntısı bir kez gösterilir.
    var crashReport by remember { mutableStateOf(CrashReporter.pending(context)) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        access = MediaPermissions.access(context)
        askedOnce = true
        val activity = context.findActivity()
        permanentlyDenied = access == MediaAccess.None && activity != null &&
            MediaPermissions.required().none { activity.shouldShowRequestPermissionRationale(it) }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        access = MediaPermissions.access(context)
    }

    val requestAccess: () -> Unit = { launcher.launch(MediaPermissions.required()) }

    CompositionLocalProvider(
        LocalGalleryImageLoader provides container.imageLoader,
        LocalGalleryStores provides container.stores
    ) {
        Box(Modifier.fillMaxSize()) {
            if (access == MediaAccess.None) {
                PermissionScreen(
                    showSettings = askedOnce && permanentlyDenied,
                    onRequest = requestAccess,
                    onOpenSettings = { context.openAppSettings() }
                )
            } else {
                LaunchedEffect(access) { container.mediaRepository.start() }
                val libraryViewModel: LibraryViewModel = viewModel {
                    LibraryViewModel(container.mediaRepository, container.edits)
                }
                val collectionsViewModel: CollectionsViewModel = viewModel {
                    CollectionsViewModel(container.mediaRepository, container.sections)
                }
                // Sekmeler arasında geçerken her sekmenin kaydırma yeri ve açık ekranı korunur.
                var tab by rememberSaveable { mutableIntStateOf(TabLibrary) }
                val tabStates = rememberSaveableStateHolder()
                Crossfade(targetState = tab, label = "tab") { current ->
                    tabStates.SaveableStateProvider(current) {
                        if (current == TabLibrary) {
                            LibraryScreen(
                                viewModel = libraryViewModel,
                                partialAccess = access == MediaAccess.Partial,
                                onRequestMoreAccess = requestAccess,
                                onOpenCollections = { tab = TabCollections }
                            )
                        } else {
                            CollectionsScreen(
                                viewModel = collectionsViewModel,
                                onOpenLibrary = { tab = TabLibrary }
                            )
                        }
                    }
                }
            }
            crashReport?.let { report ->
                CrashReportSheet(report) {
                    CrashReporter.clear(context)
                    crashReport = null
                }
            }
        }
    }
}

private const val TabLibrary = 0
private const val TabCollections = 1

private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

private fun Context.openAppSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}
