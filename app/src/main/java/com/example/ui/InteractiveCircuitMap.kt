package com.example.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.race.*
import com.example.ui.components.*
import kotlin.math.hypot

@Composable
fun InteractiveCircuitMap(vm: F1ViewModel, onDriver: (String) -> Unit, modifier: Modifier = Modifier) {
    val locations by vm.race.locations.collectAsStateWithLifecycle()
    val selected by vm.selectedDriver.collectAsStateWithLifecycle()
    val health by vm.race.health.collectAsStateWithLifecycle()
    val currentLocations by rememberUpdatedState(locations)
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var mapSize by remember { mutableStateOf(IntSize.Zero) }
    val border = MaterialTheme.colorScheme.outlineVariant
    val text = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val background = MaterialTheme.colorScheme.surface
    val drsColor = Mint
    val drivers = vm.drivers
    val labelPaint = remember { Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL) } }
    fun project(x: Float, y: Float, width: Float, height: Float): Offset =
        Offset((x - .5f) * width * zoom + width / 2 + pan.x, (y - .5f) * height * zoom + height / 2 + pan.y)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Box(Modifier.fillMaxWidth().height(218.dp).clip(RoundedCornerShape(16.dp)).background(background)
            .border(1.dp, border.copy(alpha = .6f), RoundedCornerShape(16.dp))
            .onSizeChanged { mapSize = it }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                    val event = awaitPointerEvent()
                    val pressed = event.changes.count { it.pressed }
                    // At fit zoom, a single-finger swipe belongs to the page.
                    // Two fingers zoom the map; a zoomed map also permits one-finger pan.
                    if (pressed > 0 && (pressed >= 2 || zoom > 1.01f)) {
                    val centroid = event.calculateCentroid()
                    val delta = event.calculatePan()
                    val scale = event.calculateZoom()
                    val oldZoom = zoom
                    zoom = (zoom * scale).coerceIn(1f, 4f)
                    val center = Offset(mapSize.width / 2f, mapSize.height / 2f)
                    pan = centroid - center - (centroid - center - pan) * (zoom / oldZoom) + delta
                    val limit = mapSize.width * zoom
                    pan = Offset(pan.x.coerceIn(-limit, limit), pan.y.coerceIn(-limit, limit))
                    if (delta != Offset.Zero || scale != 1f) event.changes.forEach { it.consume() }
                    }
                    } while (event.changes.any { it.pressed })
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { tap ->
                    currentLocations.minByOrNull {
                        val p = project(it.x, it.y, mapSize.width.toFloat(), mapSize.height.toFloat())
                        hypot(tap.x - p.x, tap.y - p.y)
                    }?.let {
                        val p = project(it.x, it.y, mapSize.width.toFloat(), mapSize.height.toFloat())
                        if ((tap - p).getDistance() < 30.dp.toPx()) onDriver(it.driverId)
                    }
                }
            }.semantics { contentDescription = "Interactive schematic circuit. Pinch to zoom, drag to pan. Driver selection is also available below." }) {
            Canvas(Modifier.fillMaxSize()) {
                for (x in 0..12) drawLine(border.copy(alpha = .22f), Offset(x * size.width / 12, 0f), Offset(x * size.width / 12, size.height), .5f)
                for (y in 0..8) drawLine(border.copy(alpha = .22f), Offset(0f, y * size.height / 8), Offset(size.width, y * size.height / 8), .5f)
                val points = DemoCircuit.points.map { project(it.first, it.second, size.width, size.height) }
                val path = Path().apply { moveTo(points.first().x, points.first().y); points.drop(1).forEach { lineTo(it.x, it.y) } }
                drawPath(path, border.copy(alpha = .5f), style = Stroke(14.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawPath(path, text.copy(alpha = .7f), style = Stroke(2.3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                val drs = points.takeLast(3)
                drawLine(drsColor, drs.first(), drs[1], 3.dp.toPx(), StrokeCap.Round)
                val sf = points.first()
                drawLine(text, sf + Offset(-5.dp.toPx(),-4.dp.toPx()), sf + Offset(5.dp.toPx(),4.dp.toPx()), 3.dp.toPx())
                val ordered = locations.sortedBy { if (it.driverId == selected) 1 else 0 }
                val occupied = mutableListOf<Offset>()
                ordered.forEach { car ->
                    val driver = drivers.firstOrNull { it.id == car.driverId } ?: return@forEach
                    val p = project(car.x, car.y, size.width, size.height)
                    if (p.x !in 10f..(size.width - 10f) || p.y !in 10f..(size.height - 10f)) return@forEach
                    val active = selected == car.driverId
                    if (active) {
                        drawCircle(driver.color.copy(alpha = .15f), 15.dp.toPx(), p)
                        drawCircle(text, 8.dp.toPx(), p)
                    }
                    drawCircle(driver.color, (if (active) 5.5f else 4f).dp.toPx(), p)
                    val index = listOf("leclerc","piastri","sainz").indexOf(car.driverId)
                    val labelPosition = p + Offset(11.dp.toPx(), -11.dp.toPx())
                    val collision = occupied.any { (it - labelPosition).getDistance() < 32.dp.toPx() }
                    if (active || (index >= 0 && !collision)) {
                        labelPaint.textSize = 10.sp.toPx()
                        labelPaint.color = text.toArgb()
                        drawContext.canvas.nativeCanvas.drawText(driver.code, labelPosition.x, labelPosition.y, labelPaint)
                        occupied.add(labelPosition)
                    }
                }
            }
            Eyebrow("MONTE CARLO", modifier = Modifier.align(Alignment.TopStart).padding(12.dp), color = muted.copy(alpha = .55f))
            Row(Modifier.align(Alignment.BottomEnd).padding(5.dp).clip(RoundedCornerShape(8.dp)).background(background.copy(alpha=.95f))) {
                ActionIcon("minus", "Zoom out", { zoom = (zoom / 1.3f).coerceAtLeast(1f) })
                ActionIcon("plus", "Zoom in", { zoom = (zoom * 1.3f).coerceAtMost(4f) })
                ActionIcon("fit", "Reset circuit view", { zoom = 1f; pan = Offset.Zero })
            }
            if (locations.isEmpty()) Box(Modifier.fillMaxSize().background(background.copy(alpha = .7f)), contentAlignment = Alignment.Center) {
                Text("Driver coordinates unavailable", fontSize = 12.sp, color = muted)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Schematic · simulated positions", fontSize = 9.sp, color = muted)
            Text(if (health.connection == Connection.Live) "DEMO FEED · 4 Hz" else "POSITIONS " + health.connection.name.uppercase(), fontSize = 9.sp, color = if (health.connection == Connection.Live) Mint else Amber)
        }
        ApexLazyRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            items(drivers, key = { it.id }) { driver ->
                TextButton(onClick = { onDriver(driver.id) }, contentPadding = PaddingValues(horizontal = 8.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = if (driver.id == selected) driver.color else muted)) {
                    Text(driver.code, fontSize = 10.sp)
                }
            }
        }
    }
}
