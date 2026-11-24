package com.example.ui.screens

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.race.*
import com.example.platform.ShareService
import com.example.ui.F1ViewModel
import com.example.ui.DriverDoodle
import com.example.ui.components.*
import kotlin.math.roundToInt
import java.util.Locale

@Composable
fun TelemetryDetail(vm: F1ViewModel, id: String, onProfile: () -> Unit) {
    val driver = vm.drivers.first { it.id == id }
    val timing by vm.race.timing.collectAsStateWithLifecycle()
    val row = timing.find { it.driverId == id }
    var compare by rememberSaveable(id) { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    ApexLazyColumn(verticalArrangement = Arrangement.spacedBy(18.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(driver.name, fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.8).sp)
                    Text(driver.team + "  /  #" + driver.number, color = driver.color, fontSize = 12.sp)
                }
                TextButton(onClick = onProfile) { Text("Profile →") }
            }
        }
        item { LiveTelemetry(vm, id, compare) }
        item {
            SectionTitle("Compare a driver", if (compare != null) "Clear" else null) { compare = null }
            ApexLazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(vm.drivers.filter { it.id != id }, key = { it.id }) { d ->
                    FilterChip(compare == d.id, { compare = if (compare == d.id) null else d.id }, label = { Text(d.code) })
                }
            }
            InfoNote("Aligned to the same exhibition event clock. Chart shows the last 60 seconds, not a distance-aligned lap comparison. Gaps over one second break the trace.")
        }
        item {
            Panel {
                Eyebrow("LATEST COMPLETED LAP")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row?.sectorsMs?.forEachIndexed { i, sector ->
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Eyebrow("SECTOR ${i+1}")
                            Text(String.format(Locale.US, "%.3f", sector / 1000.0), fontSize = 19.sp, fontWeight = FontWeight.Medium)
                            Box(Modifier.fillMaxWidth().height(3.dp).background(Mint, RoundedCornerShape(2.dp)))
                        }
                    }
                }
                if (row != null) Text("Tyre ${row.tyre} · ${row.tyreAge} laps old · ${row.stops} pit stop · Gap ${formatGap(row.gapMs)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val other = timing.find { it.driverId == compare }
                if (row != null && other != null) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Eyebrow("LAP PACE COMPARISON · DEMO")
                    Text("${driver.code} ${formatLap(row.lastLapMs)}   /   ${vm.drivers.first { it.id == compare }.code} ${formatLap(other.lastLapMs)}", fontSize = 13.sp)
                    val delta = row.lastLapMs - other.lastLapMs
                    Text(String.format(Locale.US, "%+.3f s", delta / 1000.0), color = if (delta <= 0) Mint else Coral, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    row.sectorsMs.zip(other.sectorsMs).forEachIndexed { i, (a,b) -> Text("S${i+1}    " + String.format(Locale.US, "%+.3f s", (a-b)/1000.0), fontSize = 12.sp) }
                }
            }
        }
        item {
            Button(onClick = {
                if (!ShareService.moment(context, driver.name, row?.let { formatLap(it.lastLapMs) } ?: "NO LAP DATA", "Lap ${vm.race.session.value.lap} · Monaco exhibition · sample", "apex://driver/$id")) vm.message("Sharing is unavailable on this device.")
            }, Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(11.dp)) { Glyph("share", size = 17.dp); Spacer(Modifier.width(9.dp)); Text("Share timing snapshot") }
            Spacer(Modifier.height(10.dp))
            InfoNote("All telemetry on this screen is simulated. Real speed, RPM, pedal input, DRS and delta availability depends on the connected provider.")
        }
    }
}

@Composable
private fun LiveTelemetry(vm: F1ViewModel, id: String, compare: String?) {
    val sampleFlow = remember(vm, id) { vm.race.telemetry(id) }
    val historyFlow = remember(vm, id) { vm.race.history(id) }
    val sample by sampleFlow.collectAsStateWithLifecycle()
    val history by historyFlow.collectAsStateWithLifecycle()
    val comparisonFlow = remember(vm, compare) { vm.race.history(compare ?: id) }
    val comparison by comparisonFlow.collectAsStateWithLifecycle()
    val health by vm.race.health.collectAsStateWithLifecycle()
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Panel {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Eyebrow("ON-BOARD / EXHIBITION", modifier = Modifier.weight(1f))
                Badge(if (health.connection == Connection.Live) "DEMO STREAM" else health.connection.name.uppercase(), if (health.connection == Connection.Live) Mint else Amber)
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(sample?.speedKph?.toString() ?: "—", fontSize = 68.sp, fontWeight = FontWeight.Light, letterSpacing = (-3).sp, style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"))
                Text("KM/H", Modifier.padding(start = 10.dp, bottom = 15.dp), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(bottom = 10.dp)) {
                    Eyebrow("GEAR")
                    Text(sample?.gear?.toString() ?: "—", fontSize = 38.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Pedal("THROTTLE", sample?.throttle, Mint)
            Pedal("BRAKE", sample?.brake, Coral)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("RPM  ${sample?.rpm ?: "—"}", fontSize = 11.sp)
                Text("DRS  " + when (sample?.drs) { true -> "OPEN"; false -> "CLOSED"; null -> "UNAVAILABLE" }, fontSize = 11.sp, color = if (sample?.drs == true) Mint else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Speed trace", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text("LAST 60 SEC", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                Text("━ " + vm.drivers.first { it.id == id }.code, fontSize = 10.sp, color = Coral)
                if (compare != null) Text("━ " + vm.drivers.first { it.id == compare }.code, fontSize = 10.sp, color = Mint)
            }
            SpeedChart(history, if (compare != null) comparison else emptyList())
        }
    }
}

@Composable
private fun Pedal(label: String, value: Float?, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Eyebrow(label, modifier = Modifier.width(72.dp))
        Box(Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.outlineVariant)) {
            Box(Modifier.fillMaxWidth((value ?: 0f).coerceIn(0f, 1f)).fillMaxHeight().background(color))
        }
        Text(if (value == null) "—" else "${(value * 100).roundToInt()}%", Modifier.width(37.dp), fontSize = 11.sp, color = color)
    }
}

@Composable
private fun SpeedChart(samples: List<TelemetrySample>, comparison: List<TelemetrySample>) {
    val grid = MaterialTheme.colorScheme.outlineVariant
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val paint = remember { Paint(Paint.ANTI_ALIAS_FLAG) }
    val selectedColor = Coral
    val comparisonColor = Mint
    Canvas(Modifier.fillMaxWidth().height(170.dp).semantics { contentDescription = "Speed chart. 0 to 360 kilometres per hour over the last 60 seconds of simulated event time." }) {
        val left = 33.dp.toPx(); val bottom = size.height - 22.dp.toPx(); val top = 8.dp.toPx()
        val width = size.width - left - 4.dp.toPx(); val height = bottom - top
        paint.color = muted.toArgb(); paint.textSize = 9.sp.toPx()
        for (value in listOf(0, 180, 360)) {
            val y = bottom - value / 360f * height
            drawLine(grid.copy(alpha = .7f), Offset(left, y), Offset(size.width, y), .7f)
            drawContext.canvas.nativeCanvas.drawText("$value", 0f, y + 3.dp.toPx(), paint)
        }
        val end = maxOf(samples.lastOrNull()?.eventTimeMs ?: 0L, comparison.lastOrNull()?.eventTimeMs ?: 0L)
        fun trace(data: List<TelemetrySample>, color: Color) {
            val path = Path(); var previous: Long? = null
            data.filter { it.eventTimeMs >= end - 60000 }.forEach { sample ->
                val x = left + (sample.eventTimeMs - (end - 60000)) / 60000f * width
                val y = bottom - sample.speedKph / 360f * height
                if (previous == null || sample.eventTimeMs - previous!! > 1000) path.moveTo(x, y) else path.lineTo(x, y)
                previous = sample.eventTimeMs
            }
            drawPath(path, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        trace(comparison, comparisonColor); trace(samples, selectedColor)
        drawContext.canvas.nativeCanvas.drawText("−60s", left, size.height - 3.dp.toPx(), paint)
        drawContext.canvas.nativeCanvas.drawText("−30s", left + width / 2, size.height - 3.dp.toPx(), paint)
        drawContext.canvas.nativeCanvas.drawText("now", size.width - 22.dp.toPx(), size.height - 3.dp.toPx(), paint)
    }
}

@Composable
fun DriverProfile(vm: F1ViewModel, id: String, onTelemetry: () -> Unit) {
    val driver = vm.drivers.first { it.id == id }
    val preferences by vm.alerts.collectAsStateWithLifecycle()
    val following = id in preferences.followedDrivers
    ApexLazyColumn(verticalArrangement = Arrangement.spacedBy(18.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            Box(Modifier.fillMaxWidth().height(200.dp).clip(PanelShape).background(Brush.linearGradient(listOf(driver.color.copy(alpha=.22f), MaterialTheme.colorScheme.surface)))) {
                Text("${driver.number}", Modifier.align(Alignment.TopEnd).padding(end = 15.dp), color = driver.color.copy(alpha = .16f), fontSize = 125.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic)
                DriverDoodle(driver.code,driver.color,Modifier.align(Alignment.BottomEnd).size(160.dp,190.dp),vm.reducedMotion,onOpenProfile={vm.selectDriver(id);vm.archiveDriver=null;vm.openExperience("career")})
                Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                    Eyebrow(driver.country.uppercase(), Coral)
                    Text(driver.name.substringBefore(' '), fontSize = 20.sp)
                    Text(driver.name.substringAfter(' ').uppercase(), fontSize = 27.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic)
                    Text(driver.team, color = driver.color, fontSize = 12.sp)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { vm.followDriver(id) }, Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(11.dp)) { Text(if (following) "Following ✓" else "Follow driver") }
                OutlinedButton(onClick = onTelemetry, Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(11.dp)) { Text("Telemetry") }
            }
        }
        item {
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick={vm.selectDriver(id);vm.openExperience("helmets")},Modifier.weight(1f)){Text("Helmet Vault")}
                OutlinedButton(onClick={vm.selectDriver(id);vm.archiveDriver=null;vm.openExperience("career")},Modifier.weight(1f)){Text("Career Universe")}
            }
        }
        item {
            Eyebrow("EXHIBITION CHAMPIONSHIP · SAMPLE STATS")
            Spacer(Modifier.height(13.dp))
            Row(Modifier.fillMaxWidth()) {
                Metric("STANDING", "P${driver.position}", Modifier.weight(1f))
                Metric("POINTS", "${driver.points}", Modifier.weight(1f))
                Metric("WINS", "${driver.wins}", Modifier.weight(1f))
            }
        }
        item {
            Panel {
                SectionTitle("Driver notebook")
                Text("${driver.name} represents ${driver.team} in this 2024-inspired exhibition roster. Driver identities are used to demonstrate the product. Career totals, current-season results and qualifying history require a verified statistics provider.", fontSize = 13.sp, lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth()) {
                    Metric("PODIUMS · SAMPLE", "${driver.podiums}", Modifier.weight(1f))
                    Metric("POLES / RESULTS", "—", Modifier.weight(1f))
                }
                InfoNote("Open Career Universe for published season results. Sample points above are not current official standings; whole-career totals and recent-form analysis are not inferred.")
            }
        }
        item {
            val teammate = vm.drivers.firstOrNull { it.team == driver.team && it.id != driver.id }
            if (teammate != null) {
                Panel {
                    Eyebrow("TEAMMATE COMPARISON · SAMPLE POINTS")
                    listOf(driver, teammate).forEach { d ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(d.code, Modifier.width(45.dp), fontSize = 12.sp)
                            Box(Modifier.weight(1f).height(5.dp).background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(3.dp))) {
                                Box(Modifier.fillMaxWidth((d.points / maxOf(driver.points, teammate.points).toFloat()).coerceIn(0f,1f)).height(5.dp).background(d.color, RoundedCornerShape(3.dp)))
                            }
                            Text("${d.points}", Modifier.padding(start = 12.dp).width(35.dp), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
