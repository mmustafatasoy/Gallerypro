package com.atasoy.gallerypro.data.media

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** Kullanıcının verdiği medya erişimi. */
enum class MediaAccess { Full, Partial, None }

object MediaPermissions {

    /** İstenecek izinler, Android sürümüne göre. */
    fun required(): Array<String> = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        )
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO
        )
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun access(context: Context): MediaAccess {
        fun granted(p: String) = ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                (granted(Manifest.permission.READ_MEDIA_IMAGES) || granted(Manifest.permission.READ_MEDIA_VIDEO)) ->
                MediaAccess.Full

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                granted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) ->
                MediaAccess.Partial

            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU &&
                granted(Manifest.permission.READ_EXTERNAL_STORAGE) ->
                MediaAccess.Full

            else -> MediaAccess.None
        }
    }
}
