package com.atasoy.gallerypro.designsystem.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Uygulamanın kendi çizgi ikonları (24 dp, çizgi 1,9, yuvarlak uç).
 * Siyah çizilir, kullanım yerinde ColorFilter.tint ile renklenir.
 */
object GalleryIcons {

    val Library: ImageVector by lazy {
        icon("library") {
            stroke(
                roundRect(3.5f, 3.5f, 7f, 7f, 1.8f) +
                    roundRect(13.5f, 3.5f, 7f, 7f, 1.8f) +
                    roundRect(3.5f, 13.5f, 7f, 7f, 1.8f) +
                    roundRect(13.5f, 13.5f, 7f, 7f, 1.8f)
            )
        }
    }

    val Collections: ImageVector by lazy {
        icon("collections") {
            stroke(roundRect(4f, 8.5f, 16f, 12f, 2.6f) + "M6.5 5.5h11M9 2.8h6")
        }
    }

    val Search: ImageVector by lazy {
        icon("search") {
            stroke(circle(11f, 11f, 6.5f) + "M16 16l4.5 4.5", width = 2f)
        }
    }

    val More: ImageVector by lazy {
        icon("more") {
            fill(circle(5f, 12f, 1.8f) + circle(12f, 12f, 1.8f) + circle(19f, 12f, 1.8f))
        }
    }

    val Check: ImageVector by lazy {
        icon("check") {
            stroke("M5 12.5l4.5 4.5L19 7.5", width = 2.6f)
        }
    }

    val HeartFilled: ImageVector by lazy {
        icon("heart") {
            fill(
                "M12 20.5s-7.5-4.6-9.6-9.2C0.9 7.9 3.1 4.5 6.6 4.5c2.2 0 3.7 1.3 5.4 3.2" +
                    "c1.7-1.9 3.2-3.2 5.4-3.2c3.5 0 5.7 3.4 4.2 6.8c-2.1 4.6-9.6 9.2-9.6 9.2z"
            )
        }
    }

    val Share: ImageVector by lazy {
        icon("share") {
            stroke(
                "M12 3.5v11.5M8 7.5l4-4 4 4" +
                    "M7 11H6a1.5 1.5 0 0 0-1.5 1.5v6A2.5 2.5 0 0 0 7 21h10a2.5 2.5 0 0 0 2.5-2.5v-6A1.5 1.5 0 0 0 18 11h-1"
            )
        }
    }

    val Trash: ImageVector by lazy {
        icon("trash") {
            stroke("M4 7h16M10 3.5h4M6 7l1 13h10l1-13M10 11v6M14 11v6")
        }
    }

    val Photos: ImageVector by lazy {
        icon("photos") {
            stroke(roundRect(3f, 5f, 18f, 14f, 3f) + "M3.5 15.5l5-4 4 3 3-2 5 3.5" + circle(16f, 9.2f, 1.6f))
        }
    }

    val Close: ImageVector by lazy {
        icon("close") {
            stroke("M6 6l12 12M18 6L6 18", width = 2.2f)
        }
    }

    val Back: ImageVector by lazy {
        icon("back") {
            stroke("M15 5l-7 7 7 7", width = 2.2f)
        }
    }

    val Heart: ImageVector by lazy {
        icon("heartOutline") {
            stroke(
                "M12 20.5s-7.5-4.6-9.6-9.2C0.9 7.9 3.1 4.5 6.6 4.5c2.2 0 3.7 1.3 5.4 3.2" +
                    "c1.7-1.9 3.2-3.2 5.4-3.2c3.5 0 5.7 3.4 4.2 6.8c-2.1 4.6-9.6 9.2-9.6 9.2z"
            )
        }
    }

    val Info: ImageVector by lazy {
        icon("info") {
            stroke(circle(12f, 12f, 9f) + "M12 11v6")
            fill(circle(12f, 7.6f, 1.2f))
        }
    }

    val Edit: ImageVector by lazy {
        icon("edit") {
            stroke("M4 7h9.5M18.5 7H20M4 17h3.5M12.5 17H20" + circle(16f, 7f, 2.4f) + circle(10f, 17f, 2.4f))
        }
    }

    val Play: ImageVector by lazy {
        icon("play") {
            fill("M8.5 5.2v13.6c0 0.8 0.9 1.3 1.6 0.9l10.4-6.8c0.6-0.4 0.6-1.4 0-1.8L10.1 4.3c-0.7-0.4-1.6 0.1-1.6 0.9z")
        }
    }

    val Pin: ImageVector by lazy {
        icon("pin") {
            stroke("M12 21s-6.5-6-6.5-11a6.5 6.5 0 0 1 13 0c0 5-6.5 11-6.5 11z" + circle(12f, 10f, 2.3f), width = 2f)
        }
    }

    val Pause: ImageVector by lazy {
        icon("pause") {
            fill(roundRect(6f, 4.5f, 4.2f, 15f, 1.3f) + roundRect(13.8f, 4.5f, 4.2f, 15f, 1.3f))
        }
    }

    val SoundOn: ImageVector by lazy {
        icon("soundOn") {
            fill("M4 9.5h3.2L11.5 6v12l-4.3-3.5H4z")
            stroke("M15 9.2a4 4 0 0 1 0 5.6M17.8 6.8a7.5 7.5 0 0 1 0 10.4")
        }
    }

    val SoundOff: ImageVector by lazy {
        icon("soundOff") {
            fill("M4 9.5h3.2L11.5 6v12l-4.3-3.5H4z")
            stroke("M15.5 9.5l5 5M20.5 9.5l-5 5")
        }
    }

    // ---------------------------------------------------------------- yardımcılar

    private class Builder(val inner: ImageVector.Builder) {
        fun stroke(path: String, width: Float = 1.9f) {
            inner.addPath(
                pathData = addPathNodes(path),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = width,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }

        fun fill(path: String) {
            inner.addPath(
                pathData = addPathNodes(path),
                fill = SolidColor(Color.Black)
            )
        }
    }

    private fun icon(name: String, block: Builder.() -> Unit): ImageVector {
        val builder = ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        Builder(builder).block()
        return builder.build()
    }

    private fun n(v: Float): String = if (v == v.toInt().toFloat()) v.toInt().toString() else v.toString()

    private fun circle(cx: Float, cy: Float, r: Float): String =
        "M${n(cx - r)} ${n(cy)}a${n(r)} ${n(r)} 0 1 0 ${n(2 * r)} 0a${n(r)} ${n(r)} 0 1 0 ${n(-2 * r)} 0z"

    private fun roundRect(x: Float, y: Float, w: Float, h: Float, r: Float): String {
        val hw = w - 2 * r
        val hh = h - 2 * r
        return "M${n(x + r)} ${n(y)}h${n(hw)}a${n(r)} ${n(r)} 0 0 1 ${n(r)} ${n(r)}" +
            "v${n(hh)}a${n(r)} ${n(r)} 0 0 1 ${n(-r)} ${n(r)}" +
            "h${n(-hw)}a${n(r)} ${n(r)} 0 0 1 ${n(-r)} ${n(-r)}" +
            "v${n(-hh)}a${n(r)} ${n(r)} 0 0 1 ${n(r)} ${n(-r)}z"
    }
}
