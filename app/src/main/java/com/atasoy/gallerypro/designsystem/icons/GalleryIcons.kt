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

    // ---------------------------------------------------------------- koleksiyonlar (4. aşama)

    val ChevronRight: ImageVector by lazy { icon("chevronRight") { stroke("M9 5l7 7-7 7", width = 2.4f) } }

    val Video: ImageVector by lazy {
        icon("video") { stroke("M3 6.5h10a2.5 2.5 0 0 1 2.5 2.5v6a2.5 2.5 0 0 1-2.5 2.5H3zM15.5 10.5l5-3v9l-5-3", width = 1.8f) }
    }

    val Download: ImageVector by lazy {
        icon("download") { stroke("M12 4v11M7.5 10.5L12 15l4.5-4.5M5 19.5h14", width = 1.8f) }
    }

    val Panorama: ImageVector by lazy {
        icon("panorama") { stroke("M3 7c6 1.5 12 1.5 18 0v10c-6-1.5-12-1.5-18 0z", width = 1.8f) }
    }

    val Screenshot: ImageVector by lazy {
        icon("screenshot") {
            stroke("M9 2.5h6a2.5 2.5 0 0 1 2.5 2.5v14a2.5 2.5 0 0 1-2.5 2.5H9A2.5 2.5 0 0 1 6.5 19V5A2.5 2.5 0 0 1 9 2.5zM10.5 18.5h3", width = 1.8f)
        }
    }

    val Raw: ImageVector by lazy {
        icon("raw") {
            stroke(
                "M6 6h12a2.5 2.5 0 0 1 2.5 2.5v7A2.5 2.5 0 0 1 18 18H6a2.5 2.5 0 0 1-2.5-2.5v-7A2.5 2.5 0 0 1 6 6z" +
                    "M7.5 15V9h2a1.5 1.5 0 0 1 0 3h-2M9.5 12l1.5 3M13 15l1.2-6 1.2 6M13.4 13h1.6",
                width = 1.8f
            )
        }
    }

    val Lock: ImageVector by lazy {
        icon("lock") {
            stroke(
                "M7.5 10.5h9a2.5 2.5 0 0 1 2.5 2.5v5a2.5 2.5 0 0 1-2.5 2.5h-9A2.5 2.5 0 0 1 5 18v-5a2.5 2.5 0 0 1 2.5-2.5z" +
                    "M8 10.5V8a4 4 0 0 1 8 0v2.5",
                width = 1.8f
            )
        }
    }

    val TrashOutline: ImageVector by lazy {
        icon("trashOutline") { stroke("M4 7h16M10 3.5h4M6 7l1 13h10l1-13M10 11v6M14 11v6", width = 1.8f) }
    }

    val Duplicate: ImageVector by lazy {
        icon("duplicate") {
            stroke(
                "M10.5 8h7A2.5 2.5 0 0 1 20 10.5v7a2.5 2.5 0 0 1-2.5 2.5h-7A2.5 2.5 0 0 1 8 17.5v-7A2.5 2.5 0 0 1 10.5 8z" +
                    "M16 8V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h2",
                width = 1.8f
            )
        }
    }

    val Grip: ImageVector by lazy { icon("grip") { stroke("M5 9h14M5 15h14", width = 2f) } }

    // ---------------------------------------------------------------- düzenle (4. aşama)

    val ToolAuto: ImageVector by lazy {
        icon("toolAuto") { stroke("M4 20L15 9M14 4v3M12.5 5.5h3M19 9v3M17.5 10.5h3M19 3v2M18 4h2", width = 1.8f) }
    }

    val ToolExposure: ImageVector by lazy {
        icon("toolExposure") {
            stroke(
                circle(12f, 12f, 4f) +
                    "M12 2.5v2M12 19.5v2M2.5 12h2M19.5 12h2M5.3 5.3l1.4 1.4M17.3 17.3l1.4 1.4M5.3 18.7l1.4-1.4M17.3 6.7l1.4-1.4",
                width = 1.8f
            )
        }
    }

    val ToolBrightness: ImageVector by lazy {
        icon("toolBrightness") { stroke(circle(12f, 12f, 8f) + "M12 4v16M12 8h3.5M12 12h5M12 16h3.5", width = 1.8f) }
    }

    val ToolContrast: ImageVector by lazy {
        icon("toolContrast") { stroke(circle(12f, 12f, 8f) + "M12 4v16M8.5 6.5l-2 2M10 9.5l-4.5 4.5M10 13.5l-3 3", width = 1.8f) }
    }

    val ToolSaturation: ImageVector by lazy {
        icon("toolSaturation") { stroke("M12 3.5s6 6.4 6 10.5a6 6 0 0 1-12 0c0-4.1 6-10.5 6-10.5z", width = 1.8f) }
    }

    val ToolWarmth: ImageVector by lazy {
        icon("toolWarmth") { stroke("M10 14.5V5a2 2 0 0 1 4 0v9.5a4 4 0 1 1-4 0zM12 9v7", width = 1.8f) }
    }

    val ToolVignette: ImageVector by lazy {
        icon("toolVignette") {
            stroke(
                "M6 5h12a2.5 2.5 0 0 1 2.5 2.5v9A2.5 2.5 0 0 1 18 19H6a2.5 2.5 0 0 1-2.5-2.5v-9A2.5 2.5 0 0 1 6 5z" +
                    "M12 8.2a5 3.8 0 1 1 0 7.6a5 3.8 0 1 1 0-7.6",
                width = 1.8f
            )
        }
    }

    val ToolSharpness: ImageVector by lazy { icon("toolSharpness") { stroke("M12 4l8.5 15h-17z", width = 1.8f) } }

    val TabAdjust: ImageVector by lazy {
        icon("tabAdjust") { stroke("M4 7h9.5M18.5 7H20M4 17h3.5M12.5 17H20" + circle(16f, 7f, 2.4f) + circle(10f, 17f, 2.4f)) }
    }

    val TabFilters: ImageVector by lazy {
        icon("tabFilters") { stroke(circle(12f, 8.5f, 5f) + circle(8.5f, 14.5f, 5f) + circle(15.5f, 14.5f, 5f), width = 1.8f) }
    }

    val TabCrop: ImageVector by lazy { icon("tabCrop") { stroke("M6 2.5V18h15.5M2.5 6H18v15.5") } }

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
