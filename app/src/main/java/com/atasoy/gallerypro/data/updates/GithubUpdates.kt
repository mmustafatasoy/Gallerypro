package com.atasoy.gallerypro.data.updates

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import com.atasoy.gallerypro.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

internal data class UpdateInfo(
    val versionCode: Long,
    val versionName: String,
    val sha256: String,
    val signerSha256: String,
    val sizeBytes: Long
)

internal sealed interface UpdateCheck {
    data class Available(val info: UpdateInfo) : UpdateCheck
    data object Current : UpdateCheck
    data object DifferentSigner : UpdateCheck
    data object Failed : UpdateCheck
}

internal object GithubUpdates {
    @Suppress("DEPRECATION")
    fun signingHash(context: Context): String? = runCatching {
        val p = context.packageManager.getPackageInfo(
            context.packageName, PackageManager.GET_SIGNING_CERTIFICATES
        )
        val signature = p.signingInfo?.apkContentsSigners?.firstOrNull() ?: return@runCatching null
        sha256(signature.toByteArray())
    }.getOrNull()

    fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }

    /** Güncelleme için sadece GitHub'daki sürüm JSON'u indirilir; medya ve yüz verileri gönderilmez. */
    suspend fun check(context: Context): UpdateCheck = withContext(Dispatchers.IO) {
        if (!BuildConfig.SELF_UPDATE_ENABLED) return@withContext UpdateCheck.Current
        try {
            val conn = URL(UpdatePolicy.ManifestUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 12000
            conn.readTimeout = 12000
            conn.setRequestProperty("User-Agent", "GalleryPro-Direct-Updates")
            val body = try {
                if (conn.responseCode != 200 || conn.contentLengthLong > 16384L) {
                    return@withContext UpdateCheck.Failed
                }
                conn.inputStream.bufferedReader().use { it.readText().take(16385) }
            } finally { conn.disconnect() }
            if (body.length > 16384) return@withContext UpdateCheck.Failed
            val json = JSONObject(body)
            val pkg = json.optString("packageName")
            val code = json.optLong("versionCode", -1)
            val sha = json.optString("sha256").lowercase()
            val signer = json.optString("signerSha256").lowercase()
            val name = json.optString("versionName").take(48)
            val size = json.optLong("sizeBytes", -1)
            if (pkg != UpdatePolicy.PackageName || code <= 0 || name.isBlank() ||
                !UpdatePolicy.isDigest(sha) || !UpdatePolicy.isDigest(signer) ||
                size !in 1_000_000L..1_500_000_000L) return@withContext UpdateCheck.Failed
            if (code <= BuildConfig.VERSION_CODE) return@withContext UpdateCheck.Current
            val localSigner = signingHash(context) ?: return@withContext UpdateCheck.Failed
            if (!UpdatePolicy.validUpgrade(pkg, BuildConfig.VERSION_CODE.toLong(),
                    code, sha, signer, localSigner)) {
                return@withContext UpdateCheck.DifferentSigner
            }
            UpdateCheck.Available(UpdateInfo(code, name, sha, signer, size))
        } catch (_: Exception) { UpdateCheck.Failed }
    }
}

/**
 * Android DownloadManager indirmeyi uygulama arka plandayken de sürdürür.
 * Kurulum otomatik/sessiz değildir: kullanıcı Android'in yükleme ekranını onaylar.
 */
internal object UpdateDownloads {
    private const val Pref = "gallerypro_direct_updates"
    private fun dm(context: Context) =
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    data class Pending(val id: Long, val info: UpdateInfo)

    fun pending(context: Context): Pending? {
        val p = context.getSharedPreferences(Pref, Context.MODE_PRIVATE)
        val id = p.getLong("id", -1)
        val code = p.getLong("code", -1)
        val hash = p.getString("hash", "").orEmpty()
        val signer = p.getString("signer", "").orEmpty()
        val size = p.getLong("size", -1)
        if (id <= 0 || code <= 0 || !UpdatePolicy.isDigest(hash) ||
            !UpdatePolicy.isDigest(signer) || size < 1_000_000) return null
        return Pending(id, UpdateInfo(code, "v" + code, hash, signer, size))
    }

    fun fileName(info: UpdateInfo) =
        "GalleryPro-" + info.versionCode + "-" + info.sha256.take(12) + ".apk"

    fun clear(context: Context, delete: Boolean = false) {
        if (delete) pending(context)?.let { runCatching { dm(context).remove(it.id) } }
        context.getSharedPreferences(Pref, Context.MODE_PRIVATE).edit().clear().apply()
    }

    fun start(context: Context, info: UpdateInfo): Pending {
        require(BuildConfig.SELF_UPDATE_ENABLED)
        require(info.versionCode > BuildConfig.VERSION_CODE && UpdatePolicy.isDigest(info.sha256))
        val previous = pending(context)
        if (previous != null && previous.info.versionCode == info.versionCode &&
            previous.info.sha256.equals(info.sha256, true)) return previous
        if (previous != null) clear(context, delete = true)
        val file = File(
            requireNotNull(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)),
            fileName(info)
        )
        if (file.exists()) file.delete()
        val request = DownloadManager.Request(Uri.parse(UpdatePolicy.ApkUrl))
            .setTitle("GalleryPro " + info.versionName)
            .setDescription("GalleryPro güncellemesi")
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(
                context, Environment.DIRECTORY_DOWNLOADS, fileName(info)
            )
        val id = dm(context).enqueue(request)
        context.getSharedPreferences(Pref, Context.MODE_PRIVATE).edit()
            .putLong("id", id).putLong("code", info.versionCode)
            .putString("hash", info.sha256).putString("signer", info.signerSha256)
            .putLong("size", info.sizeBytes).apply()
        return Pending(id, info)
    }

    /** 0 indiriliyor, 1 başarıyla indi, -1 hata */
    fun status(context: Context, id: Long): Int = runCatching {
        dm(context).query(DownloadManager.Query().setFilterById(id))?.use { cursor ->
            if (!cursor.moveToFirst()) return@use -1
            when (cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                DownloadManager.STATUS_SUCCESSFUL -> 1
                DownloadManager.STATUS_FAILED -> -1
                else -> 0
            }
        } ?: -1
    }.getOrDefault(-1)

    @Suppress("DEPRECATION")
    suspend fun verify(context: Context, pending: Pending): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val md = MessageDigest.getInstance("SHA-256")
                var bytes = 0L
                dm(context).openDownloadedFile(pending.id).use { descriptor ->
                    android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { input ->
                        val buffer = ByteArray(65536)
                        while (true) {
                            val len = input.read(buffer)
                            if (len == -1) break
                            bytes += len
                            if (bytes > pending.info.sizeBytes) return@withContext false
                            md.update(buffer, 0, len)
                        }
                    }
                }
                val digest = md.digest().joinToString("") { "%02x".format(it) }
                if (bytes != pending.info.sizeBytes ||
                    !digest.equals(pending.info.sha256, true)) return@withContext false
                val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    fileName(pending.info))
                val apk = context.packageManager.getPackageArchiveInfo(
                    file.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES
                ) ?: return@withContext false
                val signature = apk.signingInfo?.apkContentsSigners?.firstOrNull()
                    ?: return@withContext false
                apk.packageName == context.packageName &&
                    apk.longVersionCode == pending.info.versionCode &&
                    GithubUpdates.sha256(signature.toByteArray()).equals(
                        pending.info.signerSha256, true
                    ) &&
                    GithubUpdates.signingHash(context).equals(pending.info.signerSha256, true)
            } catch (_: Exception) { false }
        }

    /** Bu izin yalnızca direct APK manifestindedir, Play AAB'de YOKTUR. */
    fun install(context: Context, pending: Pending): Boolean {
        return try {
            if (!context.packageManager.canRequestPackageInstalls()) {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + context.packageName))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                false
            } else {
                val uri = dm(context).getUriForDownloadedFile(pending.id) ?: return false
                context.startActivity(
                    Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                        data = uri
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                )
                true
            }
        } catch (_: Exception) { false }
    }
}
