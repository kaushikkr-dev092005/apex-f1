package com.example.platform

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.FileProvider
import java.io.File

enum class MomentKind(val label:String,val footer:String) {
    Demo("DEMO","Exhibition data · timing graphic"),
    Recorded("RECORDED","Recorded historical data · timing graphic"),
    Hypothetical("HYPOTHETICAL","Model scenario · not a real-world prediction")
}

object ShareService {
    fun text(context: Context, title: String, body: String): Boolean = runCatching {
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"; putExtra(Intent.EXTRA_SUBJECT, title); putExtra(Intent.EXTRA_TEXT, body)
        }, "Share with the paddock")); true
    }.onFailure { android.util.Log.w("APEX", "Android text share unavailable", it) }.getOrDefault(false)

    /** Native PNG timing card, no video, image SDK, or remote asset dependency. */
    fun moment(context: Context, title: String, metric: String, detail: String, link: String, eventName:String="MONACO GRAND PRIX",kind:MomentKind=MomentKind.Demo): Boolean = runCatching {
        val bitmap = Bitmap.createBitmap(1080, 760, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(15, 12, 18))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.rgb(255, 61, 79)
        canvas.drawRect(0f, 0f, 1080f, 10f, paint)
        fun label(text: String, x: Float, y: Float, size: Float, color: Int, bold: Boolean = false) {
            paint.color = color; paint.textSize = size; paint.typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
            canvas.drawText(text, x, y, paint)
        }
        label("APEX  /  RACE MOMENT", 64f, 102f, 32f, Color.WHITE, true)
        label("$eventName · ${kind.label}".take(55), 64f, 187f, 27f, Color.LTGRAY)
        label(title.take(34), 64f, 280f, 44f, Color.WHITE, true)
        label(metric.take(22), 64f, 420f, 92f, Color.rgb(255, 103, 119), true)
        label(detail.take(64), 64f, 505f, 27f, Color.LTGRAY)
        label(kind.footer, 64f, 655f, 25f, Color.LTGRAY)
        label("Every fraction. Every feeling.", 64f, 703f, 23f, Color.GRAY)
        val directory = File(context.cacheDir, "shares").apply { mkdirs() }
        val file = File(directory, "apex-moment.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "${kind.label} · $title · $metric\n$link")
            clipData = ClipData.newRawUri("APEX timing moment", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, "Share race moment"))
        true
    }.onFailure { android.util.Log.w("APEX", "Race moment share unavailable", it) }.getOrDefault(false)
}
