package com.example.data.race

enum class Connection { Connecting, Live, Reconnecting, Stale, Offline }
enum class TrackStatus { Green, SafetyCar, RedFlag, Finished }
data class RaceSession(
    val name: String = "Monaco Grand Prix",
    val circuit: String = "Circuit de Monaco",
    val country: String = "Monaco",
    val sessionType: String = "Race",
    val lap: Int = 24,
    val totalLaps: Int = 78,
    val status: TrackStatus = TrackStatus.Green,
    val demo: Boolean = true
)
data class Timing(
    val driverId: String, val position: Int, val lastLapMs: Int,
    val gapMs: Int, val intervalMs: Int, val sectorsMs: List<Int>,
    val tyre: String = "M", val tyreAge: Int = 12, val stops: Int = 1,
    val personalBest: Boolean = false, val fastest: Boolean = false
)
/** Coordinates come from the provider. Never derive real locations from timing gaps. */
data class CarLocation(val driverId: String, val x: Float, val y: Float, val approximate: Boolean = true)
data class TelemetrySample(
    val driverId: String, val eventTimeMs: Long, val speedKph: Int,
    val throttle: Float, val brake: Float, val gear: Int, val rpm: Int, val drs: Boolean
)
enum class EventKind { Start, RedFlag, SafetyCar, YellowFlag, FastestLap, Lap, Pit, Position, GreenFlag, Finish }
data class RaceEvent(
    val id: String, val kind: EventKind, val title: String, val detail: String,
    val lap: Int, val eventTimeMs: Long, val driverId: String? = null
)
data class RacePacket(
    val sequence: Long, val eventTimeMs: Long,
    val session: RaceSession? = null, val timing: List<Timing>? = null,
    val locations: List<CarLocation>? = null, val telemetry: List<TelemetrySample> = emptyList(),
    val events: List<RaceEvent> = emptyList()
)
data class FeedHealth(
    val connection: Connection = Connection.Connecting,
    val lastArrivalMs: Long? = null, val lastEventMs: Long? = null,
    val retries: Int = 0, val rejectedPackets: Int = 0
)

fun formatLap(ms: Int): String = java.lang.String.format(java.util.Locale.US, "%d:%02d.%03d", ms / 60000, (ms / 1000) % 60, ms % 1000)
fun formatGap(ms: Int): String = if (ms == 0) "LEADER" else java.lang.String.format(java.util.Locale.US, "+%.3f", ms / 1000.0)

object RaceValidation {
    fun valid(packet: RacePacket): Boolean = packet.sequence >= 0 && packet.eventTimeMs > 0 &&
        (packet.session?.let { it.lap in 1..it.totalLaps && it.totalLaps > 0 } != false) &&
        (packet.timing?.let { rows ->
            rows.map { it.driverId }.distinct().size == rows.size &&
            rows.map { it.position }.distinct().size == rows.size &&
            rows.all { it.driverId.isNotBlank() && it.position > 0 && it.lastLapMs > 0 && it.gapMs >= 0 && it.sectorsMs.size == 3 && it.sectorsMs.all { t -> t > 0 } }
        } != false) &&
        (packet.locations?.all { it.x.isFinite() && it.y.isFinite() && it.x in 0f..1f && it.y in 0f..1f } != false) &&
        packet.telemetry.all { it.eventTimeMs > 0 && it.speedKph in 0..400 && it.gear in 0..8 && it.rpm in 0..20000 && it.throttle in 0f..1f && it.brake in 0f..1f } &&
        packet.events.all { it.id.isNotBlank() && it.eventTimeMs > 0 && it.lap > 0 }
}

/** IDs are retained for the current session, with an explicit memory bound. */
class EventDeduplicator(private val capacity: Int = 1024) {
    private val ids = LinkedHashSet<String>()
    fun accept(id: String): Boolean {
        if (!ids.add(id)) return false
        if (ids.size > capacity) ids.remove(ids.first())
        return true
    }
}

fun connectionAt(lastArrival: Long?, now: Long): Connection = when {
    lastArrival == null -> Connection.Connecting
    now - lastArrival > 8000 -> Connection.Offline
    now - lastArrival > 3000 -> Connection.Stale
    else -> Connection.Live
}

fun reconnectDelay(attempt: Int, jitter: Double): Long =
    ((1000L * (1L shl attempt.coerceIn(0, 5))).coerceAtMost(30000L) * (0.8 + jitter.coerceIn(0.0, 1.0) * 0.4)).toLong()
