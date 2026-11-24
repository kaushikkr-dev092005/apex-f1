package com.example.data.race

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.sin

interface RaceProvider {
    val isDemo: Boolean
    /** Cancellation must close the subscription and release transport resources. */
    fun packets(): Flow<RacePacket>
}

/** A schematic, authored for the demo; not surveyed circuit geometry. */
object DemoCircuit {
    val points = listOf(
        .12f to .79f, .20f to .62f, .21f to .52f, .17f to .43f,
        .28f to .30f, .43f to .23f, .54f to .18f, .62f to .23f,
        .64f to .33f, .73f to .35f, .79f to .29f, .83f to .36f,
        .77f to .44f, .85f to .49f, .89f to .59f, .78f to .67f,
        .66f to .69f, .58f to .64f, .54f to .69f, .47f to .67f,
        .39f to .74f, .35f to .82f, .28f to .79f, .21f to .88f,
        .14f to .88f, .12f to .79f
    )
    fun at(progress: Float): Pair<Float, Float> {
        val lengths = points.zipWithNext { a, b -> kotlin.math.hypot(b.first - a.first, b.second - a.second) }
        val distance = ((progress % 1f + 1f) % 1f) * lengths.sum()
        var traversed = 0f
        lengths.forEachIndexed { i, length ->
            if (traversed + length >= distance) {
                val fraction = (distance - traversed) / length
                return (points[i].first + (points[i + 1].first - points[i].first) * fraction) to
                    (points[i].second + (points[i + 1].second - points[i].second) * fraction)
            }
            traversed += length
        }
        return points.first()
    }
}

class DemoRaceProvider : RaceProvider {
    override val isDemo = true
    var disconnected = false
    private var tick = 0L
    private var sequence = 0L
    private var resumed = false
    private val ids = listOf("leclerc", "piastri", "sainz", "norris", "russell", "verstappen", "hamilton", "alonso", "perez", "hulkenberg")
    private val gaps = listOf(0, 1284, 2641, 3972, 5184, 6823, 8401, 12058, 15102, 18620)
    private val epoch = 1716732300000L // Fixed exhibition event clock. Arrival time remains device time.

    fun frame(t: Long, seq: Long = t): RacePacket {
        val eventTime = epoch + t * 250
        val cycle = t % 480
        val movingTick = t - (t / 480) * 40 - (cycle - 280).coerceIn(0, 40)
        val lap = (24 + movingTick / 312).toInt().coerceAtMost(78)
        val status = when (cycle) {
            in 240..279 -> TrackStatus.SafetyCar
            in 280..319 -> TrackStatus.RedFlag
            else -> TrackStatus.Green
        }
        val progress = (movingTick.coerceAtLeast(0) % 312) / 312f
        val rows = ids.mapIndexed { i, id ->
            val variation = ((t / 16 + i * 7) % 19).toInt() * 3
            val sectors = listOf(18672 + i * 19, 34581 + i * 28 + variation, 20358 + i * 21)
            val gap = gaps[i] + if (i == 0) 0 else variation
            val previousGap = if (i <= 1) 0 else gaps[i - 1] + ((t / 16 + (i - 1) * 7) % 19).toInt() * 3
            Timing(id, i + 1, sectors.sum(), gap,
                if (i == 0) 0 else gap - previousGap, sectors,
                if (i % 3 == 0) "H" else "M", 12 + (lap - 24), 1, i == (t / 16 % 10).toInt(), i == 0)
        }
        val telemetry = ids.mapIndexed { i, id ->
            val p = ((progress - i * .022f) + 1f) % 1f
            val wave = sin(p * 24f + i * .08f)
            val speed = if (status == TrackStatus.RedFlag) 0 else (208 + 98 * wave).toInt().coerceIn(70, if (status == TrackStatus.SafetyCar) 130 else 340)
            TelemetrySample(id, eventTime, speed,
                if (speed == 0) 0f else ((wave + 1f) / 2).coerceIn(0f, 1f), if (speed == 0) 1f else if (wave < -.35f) (-wave * .85f) else 0f,
                if (speed == 0) 0 else (speed / 44 + 1).coerceIn(1, 8), if (speed == 0) 0 else (7500 + speed * 15).coerceAtMost(13500), status == TrackStatus.Green && p > .88f)
        }
        val locations = ids.mapIndexed { i, id ->
            val point = DemoCircuit.at(progress - i * .022f)
            CarLocation(id, point.first, point.second)
        }
        val events = when {
            t == 0L -> listOf(RaceEvent("start", EventKind.Start, "A closer look at the race", "Deterministic Monaco exhibition. All timing, locations and events are simulated.", lap, eventTime))
            cycle == 240L -> listOf(RaceEvent("sc-${t / 480}", EventKind.SafetyCar, "Safety car deployed", "Demo race control: field neutralised. Simulated sector 2 incident.", lap, eventTime))
            cycle == 280L -> listOf(RaceEvent("red-${t / 480}", EventKind.RedFlag, "Red flag · session suspended", "Demo race control: session suspended while marshals clear the circuit.", lap, eventTime))
            cycle == 320L -> listOf(RaceEvent("green-${t / 480}", EventKind.GreenFlag, "Green flag · track clear", "Demo race control: the exhibition session has resumed.", lap, eventTime))
            t % 120 == 0L -> listOf(RaceEvent("fast-$t", EventKind.FastestLap, "Leclerc sets the pace", "Sample fastest lap: ${formatLap(rows.first().lastLapMs)}.", lap, eventTime, "leclerc"))
            t % 80 == 0L -> listOf(RaceEvent("pit-$t", EventKind.Pit, "Norris · pit stop", "Sample pit stop: medium tyres fitted. Timing snapshot only.", lap, eventTime, "norris"))
            t % 32 == 0L -> listOf(RaceEvent("lap-$t", EventKind.Lap, "Piastri · lap update", "Sample latest lap: ${formatLap(rows[1].lastLapMs)}.", lap, eventTime, "piastri"))
            else -> emptyList()
        }
        return RacePacket(seq, eventTime,
            if (t % 4 == 0L) RaceSession(lap = lap, status = status) else null,
            if (t % 4 == 0L) rows else null, locations, telemetry, events)
    }

    override fun packets(): Flow<RacePacket> = flow {
        while (true) {
            if (disconnected) throw java.io.IOException("Demo connection interrupted")
            // A subscription always starts with a complete snapshot, including after reconnection.
            val packet = frame(tick, sequence++)
            emit(if (!resumed) packet.copy(session = frame(tick - tick % 4).session, timing = frame(tick - tick % 4).timing) else packet)
            resumed = true
            tick++
            delay(250)
        }
    }
    fun prepareSubscription() { resumed = false }
}
