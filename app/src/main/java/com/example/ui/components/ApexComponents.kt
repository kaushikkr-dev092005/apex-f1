package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.F1Red

object ApexBrand { const val name = "APEX"; const val tagline = "Every fraction. Every feeling." }
val Coral: Color @Composable get() = if (MaterialTheme.colorScheme.background.luminance() > .5f) Color(0xFF9D3F56) else Color(0xFFE49AA8)
val Mint: Color @Composable get() = if (MaterialTheme.colorScheme.background.luminance() > .5f) Color(0xFF176B4A) else Color(0xFF51D6A2)
val Amber: Color @Composable get() = if (MaterialTheme.colorScheme.background.luminance() > .5f) Color(0xFF865100) else Color(0xFFFFBF69)
val Violet: Color @Composable get() = if (MaterialTheme.colorScheme.background.luminance() > .5f) Color(0xFF7944AD) else Color(0xFFC89BFF)
val PanelShape = RoundedCornerShape(20.dp)

@Composable
fun Eyebrow(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant, modifier: Modifier = Modifier) {
    Text(text, modifier, color, fontSize = 10.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
fun SectionTitle(title: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-.5).sp)
        if (action != null) TextButton(onClick = onAction) { Text(action, fontSize = 12.sp) }
    }
}

@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().clip(PanelShape).background(MaterialTheme.colorScheme.surface)
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .6f), PanelShape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

@Composable
fun Badge(text: String, color: Color = F1Red, dot: Boolean = false) {
    Row(Modifier.clip(RoundedCornerShape(5.dp)).background(color.copy(alpha = .12f)).padding(horizontal = 7.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        if (dot) Box(Modifier.size(5.dp).background(color, CircleShape))
        Text(text, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = .7.sp)
    }
}

@Composable
fun Metric(label: String, value: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurface) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Eyebrow(label)
        Text(value, color = color, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-.8).sp,
            style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"))
    }
}

@Composable
fun ChoiceTabs(options: List<String>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(MaterialTheme.colorScheme.surface).padding(4.dp)) {
        options.forEach { option ->
            val active = option == selected
            Box(Modifier.weight(1f).heightIn(min = 40.dp).clip(RoundedCornerShape(8.dp))
                .background(if (active) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                .semantics { this.selected = active }
                .clickable(role = androidx.compose.ui.semantics.Role.Tab) { onSelect(option) }, contentAlignment = Alignment.Center) {
                Text(option, color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
    }
}

@Composable
fun BrandMark(modifier: Modifier = Modifier) {
    Canvas(modifier.size(27.dp, 22.dp)) {
        val red = F1Red
        val p = Path().apply { moveTo(0f, size.height); lineTo(size.width * .52f, 0f); lineTo(size.width * .8f, 0f); lineTo(size.width * .28f, size.height); close() }
        drawPath(p, red)
        val p2 = Path().apply { moveTo(size.width * .56f, size.height); lineTo(size.width * .8f, size.height * .5f); lineTo(size.width, size.height); close() }
        drawPath(p2, red)
    }
}

/** Small app-owned line icons keep the packaged asset set and dependency graph minimal. */
@Composable
fun Glyph(name: String, modifier: Modifier = Modifier, color: Color = LocalContentColor.current, size: Dp = 22.dp) {
    Canvas(modifier.size(size)) {
        withTransform({ scale(this@Canvas.size.width / 24, this@Canvas.size.height / 24, Offset.Zero) }) {
            val stroke = Stroke(1.65f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            fun line(a: Float, b: Float, c: Float, d: Float) = drawLine(color, Offset(a,b), Offset(c,d), 1.65f, StrokeCap.Round)
            fun path(vararg coords: Float) { drawPath(Path().apply { moveTo(coords[0], coords[1]); for (i in 2 until coords.size step 2) lineTo(coords[i], coords[i+1]) }, color, style = stroke) }
            when (name) {
                "Live" -> { drawArc(color, 180f, 180f, false, Offset(3f,5f), Size(18f,18f), style=stroke); line(12f,14f,17f,8f); line(4f,17f,20f,17f); drawCircle(color, 1.5f, Offset(12f,14f)) }
                "News" -> { drawRoundRect(color, Offset(4f,3f), Size(16f,18f), androidx.compose.ui.geometry.CornerRadius(2f), style=stroke); line(7f,7f,17f,7f); line(7f,11f,17f,11f); line(7f,15f,13f,15f) }
                "Fantasy" -> { path(7f,3f,17f,3f,16f,11f,12f,15f,8f,11f,7f,3f); path(7f,5f,3f,5f,4f,10f,8f,10f); path(17f,5f,21f,5f,20f,10f,16f,10f); line(12f,15f,12f,20f); line(8f,21f,16f,21f) }
                "Community" -> { drawCircle(color,3.2f,Offset(9f,8f),style=stroke); drawArc(color,180f,180f,false,Offset(2f,13f),Size(14f,12f),style=stroke); drawArc(color,260f,180f,false,Offset(13f,5f),Size(7f,7f),style=stroke); drawArc(color,255f,100f,false,Offset(14f,13f),Size(8f,10f),style=stroke) }
                "Profile" -> { drawCircle(color,9f,Offset(12f,12f),style=stroke); drawCircle(color,3f,Offset(12f,9f),style=stroke); drawArc(color,180f,180f,false,Offset(6f,14f),Size(12f,10f),style=stroke) }
                "bell" -> { path(5f,17f,6f,9f,8f,5f,12f,4f,16f,5f,18f,9f,19f,17f,5f,17f); drawArc(color,0f,180f,false,Offset(10f,18f),Size(4f,4f),style=stroke) }
                "back" -> { path(14f,5f,7f,12f,14f,19f); line(7f,12f,21f,12f) }
                "arrow" -> { path(9f,5f,16f,12f,9f,19f) }
                "share" -> { drawCircle(color,2.5f,Offset(18f,5f),style=stroke); drawCircle(color,2.5f,Offset(5f,12f),style=stroke); drawCircle(color,2.5f,Offset(18f,19f),style=stroke); line(7.5f,10.8f,15.5f,6.3f); line(7.5f,13.2f,15.5f,17.7f) }
                "bookmark" -> path(6f,21f,6f,3f,18f,3f,18f,21f,12f,16f,6f,21f)
                "search" -> { drawCircle(color,6.5f,Offset(10f,10f),style=stroke); line(15f,15f,21f,21f) }
                "close" -> { line(6f,6f,18f,18f); line(6f,18f,18f,6f) }
                "mic" -> { drawRoundRect(color,Offset(9f,3f),Size(6f,11f),androidx.compose.ui.geometry.CornerRadius(3f),style=stroke); drawArc(color,0f,180f,false,Offset(6f,8f),Size(12f,10f),style=stroke); line(12f,18f,12f,22f) }
                "muted" -> { path(9f,8f,9f,5f,12f,3f,15f,5f,15f,11f); drawArc(color,20f,160f,false,Offset(6f,8f),Size(12f,10f),style=stroke); line(12f,18f,12f,22f); line(3f,3f,21f,21f) }
                "send" -> { path(3f,3f,22f,12f,3f,21f,6f,12f,3f,3f); line(6f,12f,22f,12f) }
                "flag" -> { line(5f,3f,5f,21f); path(5f,4f,19f,4f,15f,8f,19f,12f,5f,12f) }
                "check" -> path(5f,12f,10f,17f,20f,6f)
                "plus" -> { line(5f,12f,19f,12f); line(12f,5f,12f,19f) }
                "minus" -> line(5f,12f,19f,12f)
                "fit" -> { path(4f,9f,4f,4f,9f,4f); path(15f,4f,20f,4f,20f,9f); path(20f,15f,20f,20f,15f,20f); path(9f,20f,4f,20f,4f,15f) }
                "more" -> { for (x in listOf(5f,12f,19f)) drawCircle(color,1.4f,Offset(x,12f)) }
                else -> { drawCircle(color,9f,Offset(12f,12f),style=stroke); line(12f,7f,12f,13f); drawCircle(color,1f,Offset(12f,17f)) }
            }
        }
    }
}

@Composable
fun ActionIcon(name: String, label: String, onClick: () -> Unit, color: Color = MaterialTheme.colorScheme.onSurface) {
    IconButton(onClick, Modifier.size(44.dp).semantics { contentDescription = label }) {
        Glyph(name, color = color)
    }
}

@Composable
fun DriverAvatar(color: Color, modifier: Modifier = Modifier, large: Boolean = false) {
    Canvas(modifier) {
        withTransform({ scale(size.width / 180f, size.height / 210f, Offset.Zero) }) {
            drawPath(Path().apply { moveTo(9f,210f); cubicTo(12f,158f,38f,151f,67f,146f); lineTo(113f,146f); cubicTo(142f,151f,168f,158f,171f,210f); close() }, Brush.linearGradient(listOf(color.copy(alpha=.7f),color.copy(alpha=.2f))))
            drawPath(Path().apply { moveTo(67f,143f); lineTo(73f,169f); lineTo(90f,181f); lineTo(107f,169f); lineTo(113f,143f); close() }, Color(0xFF202127))
            drawRoundRect(Brush.linearGradient(listOf(Color(0xFFFFF7F0),Color(0xFFB5B1B1))),Offset(35f,19f),Size(110f,135f),androidx.compose.ui.geometry.CornerRadius(55f,55f))
            drawPath(Path().apply { moveTo(39f,63f); cubicTo(72f,38f,115f,38f,141f,62f); lineTo(146f,91f); lineTo(35f,91f); close() }, color)
            drawPath(Path().apply { moveTo(34f,76f); cubicTo(66f,65f,121f,65f,147f,77f); lineTo(141f,111f); cubicTo(110f,120f,72f,120f,40f,110f); close() }, Brush.linearGradient(listOf(Color(0xFF293B42),Color(0xFF060B12))))
            drawPath(Path().apply { moveTo(39f,81f); cubicTo(67f,73f,117f,73f,140f,81f) }, Color(0xFF93C4C9).copy(alpha=.5f),style=Stroke(2f))
            drawPath(Path().apply { moveTo(39f,122f); lineTo(68f,130f); lineTo(76f,151f); lineTo(49f,145f); close() }, color)
            drawPath(Path().apply { moveTo(142f,122f); lineTo(112f,130f); lineTo(104f,151f); lineTo(132f,145f); close() }, color)
            drawRoundRect(Color(0xFF24242A), Offset(72f,130f), Size(36f,14f),androidx.compose.ui.geometry.CornerRadius(4f))
            drawLine(Color.White.copy(alpha=.22f),Offset(30f,174f),Offset(58f,210f),2f)
            drawLine(Color.White.copy(alpha=.22f),Offset(150f,174f),Offset(122f,210f),2f)
        }
    }
}

@Composable
fun InfoNote(text: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        Glyph("info", color = MaterialTheme.colorScheme.onSurfaceVariant, size = 15.dp)
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, lineHeight = 17.sp)
    }
}

@Composable
fun EmptyState(title: String, body: String, action: String? = null, onAction: () -> Unit = {}) {
    Column(Modifier.fillMaxWidth().padding(vertical = 30.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
        Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        if (action != null) OutlinedButton(onClick = onAction) { Text(action) }
    }
}
