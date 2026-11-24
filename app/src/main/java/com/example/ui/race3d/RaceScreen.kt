package com.example.ui.race3d

import android.app.ActivityManager
import android.content.Context
import android.opengl.GLSurfaceView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.data.explore.Circuit2027
import com.example.data.race3d.CircuitRace
import com.example.data.race3d.CircuitRaceFrame
import com.example.data.race3d.CircuitRoute
import com.example.data.race3d.DrivingInput
import com.example.ui.components.ApexLazyColumn
import com.example.ui.components.ChoiceTabs
import com.example.ui.components.Coral
import com.example.ui.components.Eyebrow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaceScreen(circuits: List<Circuit2027>, requestedTrack: String?) {
    val context = LocalContext.current
    var selectedId by rememberSaveable(requestedTrack) { mutableStateOf(requestedTrack ?: "mc-1929") }
    var choosingTrack by rememberSaveable { mutableStateOf(false) }
    val selected = circuits.firstOrNull { it.id == selectedId } ?: circuits.firstOrNull()
    var loaded by remember(selected?.id) { mutableStateOf<RaceAssets?>(null) }
    var loadError by remember(selected?.id) { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    LaunchedEffect(selected?.id, retry) {
        val id = selected?.id ?: return@LaunchedEffect
        loadError = false
        try {
            loaded = withContext(Dispatchers.IO) { RaceAssets.load(context.assets, id) }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            android.util.Log.e("APEX Race", "Loading circuit $id failed", error)
            loadError = true
        }
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Eyebrow("3D RACE SIMULATION", Coral)
                Text(selected?.circuit ?: "Race 3D", fontSize = 23.sp, fontWeight = FontWeight.ExtraBold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            OutlinedButton(onClick = { choosingTrack = true }, enabled = circuits.isNotEmpty()) {
                Text("Switch track")
            }
        }
        val assets = loaded
        if (assets != null && selected != null) {
            key(selected.id) { RaceWeekend(assets, choosingTrack) }
        } else if (loadError) {
            Text("This circuit couldn’t load.", color = MaterialTheme.colorScheme.error)
            Button(onClick = { retry++ }) { Text("Retry") }
        } else {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text("Loading circuit…")
        }
    }

    if (choosingTrack) {
        var query by rememberSaveable { mutableStateOf("") }
        val shown = circuits.filter { (it.circuit + " " + it.country + " " + it.nation).contains(query.trim(), true) }
        ModalBottomSheet(onDismissRequest = { choosingTrack = false }) {
            Text("Choose a circuit", Modifier.padding(horizontal = 20.dp), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(20.dp, 10.dp),
                label = { Text("Search 24 tracks") }, singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions())
            if (shown.isEmpty()) Text("No matching circuits.", Modifier.padding(20.dp))
            ApexLazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 410.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(shown, key = { it.id }) { circuit ->
                    ListItem(
                        headlineContent = { Text(circuit.circuit, fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text(circuit.country) },
                        trailingContent = { if (circuit.id == selected?.id) Text("Selected", color = Coral) },
                        modifier = Modifier.clickable {
                            selectedId = circuit.id
                            choosingTrack = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RaceWeekend(assets: RaceAssets, trackPickerOpen: Boolean) {
    var generation by remember { mutableIntStateOf(0) }
    val race = remember(assets, generation) { CircuitRace(assets.route) }
    var hud by remember(race) { mutableStateOf(race.frame()) }
    var active by remember(race) { mutableStateOf(false) }
    var drive by rememberSaveable { mutableStateOf(false) }
    var focus by rememberSaveable { mutableIntStateOf(5) }
    var camera by rememberSaveable { mutableStateOf(RaceCamera.Chase) }
    var steering by remember { mutableFloatStateOf(0f) }
    var gas by remember { mutableStateOf(false) }
    var brake by remember { mutableStateOf(false) }
    var gl by remember { mutableStateOf<RaceSurface?>(null) }
    var glError by remember { mutableStateOf<String?>(null) }
    var showCredits by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val supportsGles = remember(context) {
        (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
            .deviceConfigurationInfo.reqGlEsVersion >= 0x20000
    }
    val latestCamera by rememberUpdatedState(camera)
    val latestFocus by rememberUpdatedState(focus)
    val latestDrive by rememberUpdatedState(drive)
    val currentSurface by rememberUpdatedState(gl)
    LaunchedEffect(active, race, lifecycle, trackPickerOpen) {
        if (!active || trackPickerOpen) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var simulationTime = 0L
            var renderTime = 0L
            var hudTime = 0L
            while (active) {
                withFrameNanos { now ->
                    if (simulationTime != 0L) {
                        race.advance((now - simulationTime) / 1_000_000_000f,
                            if (latestDrive) latestFocus else null, DrivingInput(steering, gas, brake))
                    }
                    simulationTime = now
                    if (now - renderTime >= 32_000_000L) {
                        val frame = race.frame()
                        currentSurface?.show(RaceScene(frame, latestFocus, latestCamera))
                        renderTime = now
                        if (now - hudTime >= 150_000_000L || frame.finished) {
                            hud = frame
                            hudTime = now
                        }
                        if (frame.finished) active = false
                    }
                }
            }
        }
    }
    LaunchedEffect(gl, focus, camera, race, active) {
        gl?.show(RaceScene(race.frame(), focus, camera))
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                active = false
                gas = false
                brake = false
                steering = 0f
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val viewportHeight = (LocalConfiguration.current.screenHeightDp * .39f).coerceIn(192f, 330f).dp
    val shape = RoundedCornerShape(16.dp)
    Box(Modifier.fillMaxWidth().height(viewportHeight).clip(shape).background(Color(0xFF14191C))) {
        if (supportsGles && glError == null) {
            AndroidView(
                modifier = Modifier.fillMaxSize().semantics { contentDescription = "3D circuit and racing cars" },
                factory = {
                    RaceSurface(it, assets, RaceScene(race.frame(), focus, camera)) { message -> glError = message }
                        .also { surface -> gl = surface }
                },
                onRelease = { it.pauseRendering() },
                update = {}
            )
        } else {
            CircuitMap(assets.route, hud, focus, Modifier.fillMaxSize().padding(28.dp))
            Text("3D graphics unavailable. Map view is active.", Modifier.align(Alignment.BottomCenter).padding(8.dp),
                color = Color.White, fontSize = 12.sp)
        }
        Row(Modifier.align(Alignment.TopStart).padding(10.dp)
            .background(Color(0xDF101317), RoundedCornerShape(7.dp)).padding(horizontal = 9.dp, vertical = 6.dp)) {
            Text(when {
                hud.finished -> "FINISHED"
                active && !trackPickerOpen -> "RACING"
                hud.seconds > 0f -> "PAUSED"
                else -> "STARTING GRID"
            }, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        if (supportsGles && glError == null) {
            CircuitMap(assets.route, hud, focus, Modifier.align(Alignment.TopEnd).padding(10.dp)
                .size(88.dp).background(Color(0xDD101317), RoundedCornerShape(8.dp)).padding(8.dp))
        }
        if (drive) {
            Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                val driving = active && !trackPickerOpen
                HoldControl("Left", Modifier.weight(1f), driving) { steering = if (it) -1f else 0f }
                HoldControl("Right", Modifier.weight(1f), driving) { steering = if (it) 1f else 0f }
                HoldControl("Brake", Modifier.weight(1f), driving) { brake = it }
                HoldControl("Throttle", Modifier.weight(1.3f), driving) { gas = it }
            }
        }
    }

    val selectedCar = hud.cars.first { it.number == focus }
    val position = hud.order.indexOfFirst { it.number == focus } + 1
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        MetricText("POSITION", "P$position / ${assets.grid.size}")
        MetricText("SPEED", "${(selectedCar.speed * 3.6f).roundToInt()} km/h")
        MetricText("TIME", raceTime(selectedCar.finishTime ?: hud.seconds))
    }
    LinearProgressIndicator(
        progress = { (selectedCar.distance / assets.route.length).coerceIn(0f, 1f) },
        modifier = Modifier.fillMaxWidth().height(3.dp).semantics { contentDescription = "Lap progress" }
    )
    ChoiceTabs(RaceCamera.entries.map { it.label }, camera.label, { label ->
        camera = RaceCamera.entries.first { it.label == label }
    })
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = {
                gas = false
                brake = false
                steering = 0f
                if (hud.finished) {
                    generation++
                } else {
                    hud = race.frame()
                    active = !active
                }
            },
            modifier = Modifier.weight(1f)
        ) { Text(if (hud.finished) "New race" else if (active) "Pause" else if (hud.seconds > 0f) "Resume" else "Start race") }
        OutlinedButton(onClick = {
            gas = false
            brake = false
            steering = 0f
            generation++
        }, enabled = active || hud.seconds > 0f) { Text("Reset") }
    }
    ChoiceTabs(listOf("Watch AI", "Drive"), if (drive) "Drive" else "Watch AI", { selection ->
        drive = selection == "Drive"
        gas = false
        brake = false
        steering = 0f
    })
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        val livery = assets.grid[focus - 1]
        Eyebrow(if (drive) "CHOOSE A CAR TO DRIVE" else "CHOOSE A CAR TO FOLLOW", Coral)
        Text("${livery.team} #${livery.number}", fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            assets.grid.forEachIndexed { index, car ->
                val slot = index + 1
                val tint = Color(GRID_PAINT[index] or 0xFF000000.toInt())
                FilterChip(selected = focus == slot, onClick = { focus = slot },
                    modifier = Modifier.semantics { contentDescription = "${car.team} #${car.number}" },
                    label = { Text("#${car.number}") },
                    leadingIcon = { Box(Modifier.size(9.dp).background(tint, RoundedCornerShape(3.dp))) })
            }
        }
        val winner = assets.grid[hud.order.first().number - 1]
        Text(if (hud.finished) "Winner ${winner.team} #${winner.number} • ${raceTime(hud.order.first().finishTime ?: 0f)}"
            else if (drive) "Hold throttle, brake, or steer. Steering stays within the road."
            else "One-lap sprint • ${assets.grid.size} AI cars • ${String.format(Locale.US, "%.2f km", assets.route.length / 1000f)}",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = { showCredits = true }) { Text("Circuit and terrain credits") }
    }

    if (showCredits) {
        val license = remember(context) {
            context.assets.open("race3d/Mapzen-Terrain-Attribution.md").bufferedReader().use { it.readText() }
        }
        AlertDialog(onDismissRequest = { showCredits = false },
            title = { Text("Circuit and terrain credits") },
            text = {
                Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Supplied 2027 circuit pack. Circuit centerlines © Tomislav Bacinger, MIT. Elevation uses Mapzen Terrain Tiles and selected Copernicus GLO-90 samples via Open-Meteo. Surrounding scenery is illustrative.")
                    Text(license, fontSize = 12.sp)
                }
            },
            confirmButton = { TextButton(onClick = { showCredits = false }) { Text("Close") } })
    }
    DisposableEffect(gl, lifecycle) {
        val surface = gl
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> surface?.resumeRendering()
                Lifecycle.Event.ON_PAUSE -> surface?.pauseRendering()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) surface?.resumeRendering()
        onDispose {
            lifecycle.removeObserver(observer)
            surface?.pauseRendering()
        }
    }
}

private class RaceSurface(
    context: Context, assets: RaceAssets, initial: RaceScene, onError: (String) -> Unit
) : GLSurfaceView(context) {
    private val raceRenderer = RaceRenderer(context.assets, assets, initial) { message -> post { onError(message) } }
    private var paused = false

    init {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(true)
        preserveEGLContextOnPause = false
        setRenderer(raceRenderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    fun show(scene: RaceScene) {
        raceRenderer.scene = scene
        if (!paused) requestRender()
    }

    fun pauseRendering() {
        if (!paused) {
            paused = true
            onPause()
        }
    }

    fun resumeRendering() {
        if (paused) {
            paused = false
            onResume()
        }
        requestRender()
    }
}

@Composable
private fun MetricText(label: String, value: String) {
    Column {
        Eyebrow(label)
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

private fun raceTime(seconds: Float): String {
    val tenths = (seconds * 10).toInt()
    return String.format(Locale.US, "%d:%02d.%d", tenths / 600, tenths / 10 % 60, tenths % 10)
}

@Composable
private fun HoldControl(label: String, modifier: Modifier, enabled: Boolean, onHold: (Boolean) -> Unit) {
    val handler by rememberUpdatedState(onHold)
    var pressed by remember(enabled) { mutableStateOf(false) }
    Box(
        modifier.height(48.dp).clip(RoundedCornerShape(9.dp))
            .background(if (pressed) MaterialTheme.colorScheme.primary else Color(0xDF101317))
            .border(1.dp, Color(0x457C8996), RoundedCornerShape(9.dp))
            .semantics {
                role = Role.Button
                contentDescription = "$label, hold to use"
                if (enabled) onClick {
                    pressed = !pressed
                    handler(pressed)
                    true
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    pressed = true
                    handler(true)
                    try {
                        do {
                            val event = awaitPointerEvent()
                            event.changes.forEach { it.consume() }
                        } while (event.changes.any { it.id == down.id && it.pressed })
                    } finally {
                        pressed = false
                        handler(false)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 12.sp, color = when {
            !enabled -> Color(0xFFAEB7BE)
            pressed -> MaterialTheme.colorScheme.onPrimary
            else -> Color.White
        })
    }
}

@Composable
private fun CircuitMap(route: CircuitRoute, frame: CircuitRaceFrame, focus: Int, modifier: Modifier) {
    Canvas(modifier.semantics { contentDescription = "Circuit map and grid positions" }) {
        val scale = minOf(size.width / (route.maxX - route.minX), size.height / (route.maxZ - route.minZ)) * .94f
        val offsetX = size.width / 2 - (route.maxX + route.minX) / 2 * scale
        val offsetZ = size.height / 2 - (route.maxZ + route.minZ) / 2 * scale
        val path = Path().apply {
            moveTo(route.x(0) * scale + offsetX, route.z(0) * scale + offsetZ)
            for (index in 1 until route.count) lineTo(route.x(index) * scale + offsetX, route.z(index) * scale + offsetZ)
            close()
        }
        drawPath(path, Color(0xFFD0D5DB), style = Stroke(1.5.dp.toPx()))
        for (car in frame.cars) {
            val point = Offset(car.x * scale + offsetX, car.z * scale + offsetZ)
            if (car.number == focus) drawCircle(Color.White, 4.3.dp.toPx(), point)
            drawCircle(Color(GRID_PAINT[car.number - 1] or 0xFF000000.toInt()), 2.8.dp.toPx(), point)
        }
    }
}
