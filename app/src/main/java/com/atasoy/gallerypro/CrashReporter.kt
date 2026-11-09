package com.atasoy.gallerypro

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atasoy.gallerypro.designsystem.GalleryType
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Beklenmedik kapanmaları yakalar: hata ayrıntısını uygulamanın kendi klasörüne yazar,
 * bir sonraki açılışta gösterir. İnternet kullanmaz; rapor yalnızca kullanıcı
 * "Paylaş" derse cihazdan çıkar.
 */
object CrashReporter {
    private const val FileName = "crash.txt"
    private const val MaxChars = 24_000

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { write(app, thread, error) }
            if (previous != null) {
                previous.uncaughtException(thread, error)
            } else {
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        }
    }

    fun pending(context: Context): String? = runCatching {
        val file = File(context.filesDir, FileName)
        if (file.isFile) file.readText().takeIf { it.isNotBlank() } else null
    }.getOrNull()

    fun clear(context: Context) {
        runCatching { File(context.filesDir, FileName).delete() }
    }

    private fun write(context: Context, thread: Thread, error: Throwable) {
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(Date())
        val text = buildString {
            append("Gallerypro ").append(versionName(context)).append('\n')
            append(time).append('\n')
            append(Build.MANUFACTURER).append(' ').append(Build.MODEL)
            append(" · Android ").append(Build.VERSION.RELEASE)
            append(" (API ").append(Build.VERSION.SDK_INT).append(")\n")
            append("Thread: ").append(thread.name).append("\n\n")
            append(Log.getStackTraceString(error))
        }
        File(context.filesDir, FileName).writeText(text.take(MaxChars))
    }

    private fun versionName(context: Context): String = runCatching {
        val pm = context.packageManager
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(context.packageName, 0)
        }
        info.versionName
    }.getOrNull() ?: "?"
}

/** Önceki oturum çöktüyse ayrıntıyı gösterir; kullanıcı paylaşabilir veya kapatabilir. */
@Composable
fun CrashReportSheet(report: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val title = stringResource(R.string.crash_title)
    BackHandler { onDismiss() }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(interactionSource = null, indication = null) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(Color(0xFF1C1C1E))
                .padding(20.dp)
        ) {
            BasicText(
                title,
                style = GalleryType.Body.copy(color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            )
            Spacer(Modifier.height(8.dp))
            BasicText(
                stringResource(R.string.crash_message),
                style = GalleryType.Body.copy(color = Color(0xB3FFFFFF))
            )
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
                    .verticalScroll(rememberScrollState())
                    .padding(10.dp)
            ) {
                BasicText(
                    report,
                    style = TextStyle(
                        color = Color(0xFFB8B8B8),
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ReportButton(stringResource(R.string.crash_close), Color(0xFF3A3A3C), Modifier.weight(1f), onDismiss)
                ReportButton(stringResource(R.string.crash_share), Color(0xFF0A84FF), Modifier.weight(1f)) {
                    shareReport(context, title, report)
                }
            }
        }
    }
}

@Composable
private fun ReportButton(text: String, fill: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(fill)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text,
            style = GalleryType.Button.copy(color = Color.White, textAlign = TextAlign.Center)
        )
    }
}

private fun shareReport(context: Context, title: String, report: String) {
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_SUBJECT, title)
        .putExtra(Intent.EXTRA_TEXT, report)
    runCatching {
        context.startActivity(Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
