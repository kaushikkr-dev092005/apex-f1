package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.drawBehind
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
import com.example.data.model.Driver
import com.example.data.race.*
import com.example.platform.ShareService
import com.example.ui.F1ViewModel
import com.example.ui.DriverDoodle
import com.example.ui.InteractiveCircuitMap
import com.example.ui.components.*
import com.example.ui.theme.F1Red
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun LiveScreen(vm: F1ViewModel, onDriver: (String) -> Unit, onProfile: (String) -> Unit, onEvents: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf("Overview") }
    val driverId by vm.selectedDriver.collectAsStateWithLifecycle()
    val driver = vm.drivers.first { it.id == driverId }
    Column(Modifier.fillMaxSize()) {
        SessionHeader(vm)
        ApexLazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item(key = "hero") { FeaturedDriver(vm, driver) { onDriver(driver.id) } }
            item(key = "explore-shortcuts") {
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick={vm.openExperience("circuits")},Modifier.weight(1f),shape=RoundedCornerShape(10.dp)){Text("2027 circuits",fontSize=11.sp)}
                    OutlinedButton(onClick={vm.openExperience("pals")},Modifier.weight(1f),shape=RoundedCornerShape(10.dp)){Text("Paddock Pals",fontSize=11.sp)}
                }
            }
            item(key = "selection") {
                ApexLazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(vm.drivers.take(6), key = { it.id }) { d ->
                        val selected = d.id == driverId
                        Row(Modifier.height(44.dp).clip(RoundedCornerShape(8.dp))
                            .background(if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                            .border(1.dp, if (selected) d.color.copy(alpha = .65f) else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .semantics { contentDescription = "Select " + d.name }
                            .clickable { vm.selectDriver(d.id) }.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            Box(Modifier.size(3.dp, 13.dp).background(d.color, RoundedCornerShape(2.dp)))
                            Text(d.code, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            item(key = "tabs") { ChoiceTabs(listOf("Overview", "Timing", "Circuit"), tab, { tab = it }) }
            if (tab != "Timing") item(key = "circuit") {
                Column {
                    SectionTitle("The race, in motion", "Driver profile") { onProfile(driverId) }
                    InteractiveCircuitMap(vm = vm, onDriver = { vm.selectDriver(it) })
                }
            }
            if (tab != "Circuit") item(key = "timing") { TimingTower(vm, expanded = tab == "Timing", onDriver = onDriver, onExpand = { tab = "Timing" }) }
            item(key = "control") { RaceControlPreview(vm, onEvents) }
            item { InfoNote("Monaco exhibition · 2024-inspired sample roster. Timing, telemetry and locations are simulated. No live race feed is connected.") }
        }
    }
}

@Composable
private fun SessionHeader(vm: F1ViewModel) {
    val session by vm.race.session.collectAsStateWithLifecycle()
    val health by vm.race.health.collectAsStateWithLifecycle()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }
    val age = health.lastArrivalMs?.let { ((now - it).coerceAtLeast(0) / 1000) }
    val healthy = health.connection == Connection.Live
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Eyebrow("ROUND 08   /   MONACO", Coral)
                Text("Monaco Grand Prix", fontSize = 25.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.9).sp)
                Text("Circuit de Monaco · Race exhibition", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("24°", fontSize = 25.sp, fontWeight = FontWeight.Light)
                Eyebrow("SAMPLE AIR", modifier = Modifier.padding(top = 2.dp))
            }
        }
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surface).padding(start = 10.dp, end = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            val statusColor = when (session.status) { TrackStatus.RedFlag -> F1Red; TrackStatus.SafetyCar -> Amber; else -> Mint }
            Box(Modifier.size(5.dp).background(statusColor, RoundedCornerShape(3.dp)))
            Text(when (session.status) { TrackStatus.Green -> "TRACK CLEAR"; TrackStatus.RedFlag -> "RED FLAG"; TrackStatus.SafetyCar -> "SAFETY CAR"; TrackStatus.Finished -> "FINISHED" },
                Modifier.padding(start = 7.dp), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = statusColor, letterSpacing = .5.sp)
            Spacer(Modifier.weight(1f))
            Text("LAP ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            Text("${session.lap}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(" / ${session.totalLaps}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            Spacer(Modifier.width(12.dp))
            Text(if (healthy) "Demo · ${age ?: 0}s" else health.connection.name,
                color = if (healthy) MaterialTheme.colorScheme.onSurfaceVariant else Amber, fontSize = 9.sp)
            if (!healthy) ActionIcon("fit", "Reconnect race feed", vm::reconnect, Amber) else Spacer(Modifier.size(8.dp, 44.dp))
        }
        if (!healthy && health.lastArrivalMs != null) Text("Showing last received data · ${age ?: 0}s ago", Modifier.padding(top = 5.dp), color = Amber, fontSize = 10.sp)
    }
}

@Composable
private fun FeaturedDriver(vm: F1ViewModel, driver: Driver, onOpen: () -> Unit) {
    val timing by vm.race.timing.collectAsStateWithLifecycle()
    val row = timing.find { it.driverId == driver.id }
    val reflection = Color(0xFFE49AA8)
    Box(Modifier.fillMaxWidth().height(212.dp).clip(PanelShape)
        .background(Brush.linearGradient(listOf(Color(0xFF47232F), Color(0xFF25171F), Color(0xFF251B23))))
        .border(1.dp, Coral.copy(alpha = .24f), PanelShape).clickable(onClickLabel = "Open " + driver.name + " telemetry", onClick = onOpen)
        .drawBehind {
            drawCircle(Brush.radialGradient(listOf(reflection.copy(alpha = .13f), Color.Transparent), center = Offset(size.width * .9f, size.height * .2f), radius = size.width * .7f), size.width * .7f, Offset(size.width * .9f, size.height * .2f))
            for (i in 0..4) drawLine(Color.White.copy(alpha = .035f), Offset(size.width * .65f + i * 26, 0f), Offset(size.width * .2f + i * 26, size.height), 1f)
        }) {
        Text("${driver.number}", Modifier.align(Alignment.TopEnd).padding(end = 18.dp), color = Coral.copy(alpha = .11f), fontSize = 126.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic)
        DriverDoodle(driver.code,driver.color,Modifier.align(Alignment.TopEnd).offset(x=5.dp,y=12.dp).size(149.dp,174.dp),vm.reducedMotion,onOpenProfile={vm.selectDriver(driver.id);vm.archiveDriver=null;vm.openExperience("career")})
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Badge("P${row?.position ?: "—"}", Color.White)
                Text(driver.team.uppercase(), color = reflection, fontSize = 9.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(18.dp))
            Text(driver.name.substringBefore(' ').uppercase(), color = Color(0xFFEAD6DB), fontSize = 12.sp, letterSpacing = 3.sp)
            Text(driver.name.substringAfter(' ').uppercase(), color = Color.White, fontSize = if (driver.name.substringAfter(' ').length > 9) 25.sp else 31.sp,
                fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, letterSpacing = (-1.4).sp)
        }
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color(0xFF160F16).copy(alpha = .86f)).padding(horizontal = 18.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1.3f)) {
                Eyebrow("LAST LAP", Color(0xFFB7A4AE))
                Text(row?.let { formatLap(it.lastLapMs) } ?: "—:—.———", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1).sp, style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"))
            }
            Column(Modifier.weight(.8f)) {
                Eyebrow("TO LEADER", Color(0xFFB7A4AE))
                Text(row?.let { if (it.position == 1) "Leading" else formatGap(it.gapMs) } ?: "—", fontSize = 14.sp, color = Color(0xFF51D6A2), fontWeight = FontWeight.Medium)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Eyebrow("SECTORS", Color(0xFFB7A4AE))
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    listOf(Color(0xFF51D6A2), Color(0xFF51D6A2), Color(0xFF51D6A2)).forEachIndexed { i, color ->
                        Box(Modifier.size(17.dp, 5.dp).background(if (row == null) Color.Gray else color, RoundedCornerShape(2.dp)).semantics { contentDescription = "Sector ${i+1}: demo completed" })
                    }
                }
            }
        }
    }
}

@Composable
private fun TimingTower(vm: F1ViewModel, expanded: Boolean, onDriver: (String) -> Unit, onExpand: () -> Unit) {
    val timing by vm.race.timing.collectAsStateWithLifecycle()
    val selected by vm.selectedDriver.collectAsStateWithLifecycle()
    val events by vm.race.events.collectAsStateWithLifecycle()
    val session by vm.race.session.collectAsStateWithLifecycle()
    var order by rememberSaveable { mutableStateOf("Race order") }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("Timing tower", if (expanded) null else "Full grid →", onExpand)
        if (expanded) ChoiceTabs(listOf("Race order", "Lap pace"), order, { order = it })
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            Eyebrow("POS", modifier = Modifier.width(29.dp))
            Eyebrow("DRIVER", modifier = Modifier.weight(1f))
            Eyebrow(if (order == "Lap pace") "LAST LAP" else "GAP", modifier = Modifier.width(78.dp))
            Eyebrow("TYRE", modifier = Modifier.width(35.dp))
        }
        Column(Modifier.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface)) {
            val rows = if (order == "Lap pace") timing.sortedBy { it.lastLapMs } else timing
            if (rows.isEmpty()) Text("Waiting for timing…", Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            rows.take(if (expanded) 20 else 5).forEachIndexed { index, row ->
                val driver = vm.drivers.firstOrNull { it.id == row.driverId } ?: return@forEachIndexed
                val active = selected == row.driverId
                Row(Modifier.fillMaxWidth().height(if (expanded) 65.dp else 53.dp)
                    .background(if (active) driver.color.copy(alpha = .08f) else Color.Transparent)
                    .clickable(onClickLabel = "Inspect " + driver.name) { onDriver(driver.id) }.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${row.position}".padStart(2, '0'), Modifier.width(28.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box(Modifier.width(3.dp).height(23.dp).background(driver.color, RoundedCornerShape(2.dp)))
                    val event=events.firstOrNull()
                    val pose=when {session.status==TrackStatus.RedFlag->"Disappointed";event?.driverId==driver.id&&event.kind==EventKind.FastestLap->"Celebrate";event?.driverId==driver.id&&event.kind==EventKind.Pit->"Adjust helmet";else->"Idle"}
                    DriverDoodle(driver.code,driver.color,Modifier.size(32.dp),vm.reducedMotion,pose,trigger=event?.id?.hashCode()?:0,onOpenProfile={vm.selectDriver(driver.id);vm.archiveDriver=null;vm.openExperience("career")})
                    Column(Modifier.weight(1f).padding(start = 9.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(driver.code, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
                            if (row.fastest) Text("FL", fontSize = 8.sp, color = Violet, fontWeight = FontWeight.Bold)
                        }
                        Text(if (expanded) "${driver.team} · ${row.stops} stop · tyre ${row.tyreAge} laps" else driver.team,
                            fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                    Column(Modifier.width(78.dp)) {
                        Text(if (order == "Lap pace") formatLap(row.lastLapMs) else formatGap(row.gapMs), fontSize = 11.sp,
                            fontWeight = FontWeight.Medium, color = if (row.position == 1) Mint else MaterialTheme.colorScheme.onSurface,
                            style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"))
                        if (expanded) Text("INT ${formatGap(row.intervalMs)}", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Box(Modifier.size(24.dp).border(1.5.dp, if (row.tyre == "M") Amber else MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                        Text(row.tyre, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (row.tyre == "M") Amber else MaterialTheme.colorScheme.onSurface)
                    }
                }
                if (index < rows.lastIndex) HorizontalDivider(Modifier.padding(horizontal = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f), thickness = .5.dp)
            }
        }
    }
}

@Composable
private fun RaceControlPreview(vm: F1ViewModel, onEvents: () -> Unit) {
    val events by vm.race.events.collectAsStateWithLifecycle()
    Column {
        SectionTitle("From race control", "Timeline →", onEvents)
        events.take(2).forEach { event -> EventRow(event) { onEvents() } }
    }
}

@Composable
fun EventRow(event: RaceEvent, onClick: () -> Unit) {
    val critical = event.kind == EventKind.RedFlag
    val color = when (event.kind) { EventKind.RedFlag -> F1Red; EventKind.SafetyCar, EventKind.YellowFlag -> Amber; EventKind.FastestLap -> Violet; else -> Mint }
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (critical) F1Red.copy(alpha = .11f) else Color.Transparent)
        .clickable(onClick = onClick).padding(vertical = 13.dp, horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(32.dp).background(color.copy(alpha = .12f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) { Glyph("flag", size = 17.dp, color = color) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(event.title, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = if (critical) color else MaterialTheme.colorScheme.onSurface)
            Text("Lap ${event.lap} · Exhibition event", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Glyph("arrow", size = 14.dp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun RaceControlScreen(vm: F1ViewModel, selectedId: String?) {
    val events by vm.race.events.collectAsStateWithLifecycle()
    var selected by remember(selectedId) { mutableStateOf(selectedId) }
    val context = LocalContext.current
    val event = events.find { it.id == selected }
    ApexLazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            Text("Every call. Every change.", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            InfoNote("Simulated race-control timeline. Event time is the exhibition clock; receipt time is measured on your device.")
        }
        if (selectedId?.startsWith("test-") == true && event == null) item { Panel { Badge("LOCAL TEST", F1Red); Text("Demo red flag", fontWeight = FontWeight.Bold); Text("Your notification opened race control successfully. This test was generated on your device.") } }
        if (event != null) item {
            Panel {
                Badge(event.kind.name.uppercase(), if (event.kind == EventKind.RedFlag) F1Red else Mint)
                Text(event.title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(event.detail, fontSize = 13.sp, lineHeight = 21.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Lap ${event.lap} · " + java.text.SimpleDateFormat("HH:mm:ss 'UTC'", Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(java.util.Date(event.eventTimeMs)), fontSize = 11.sp)
                OutlinedButton(onClick = {
                    if (!ShareService.moment(context, event.title, "LAP ${event.lap}", event.detail, "apex://race/event/${event.id}")) vm.message("Sharing is unavailable on this device.")
                }) { Glyph("share", size = 16.dp); Spacer(Modifier.width(8.dp)); Text("Share this moment") }
            }
        }
        items(events, key = { it.id }) { item -> EventRow(item) { selected = item.id } }
    }
}
