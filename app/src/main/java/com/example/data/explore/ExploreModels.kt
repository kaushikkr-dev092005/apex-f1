package com.example.data.explore

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.*

data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(other: Vec3) = Vec3(x + other.x, y + other.y, z + other.z)
}
data class Projected(val x: Float, val y: Float, val depth: Float)

/** Perspective projection of actual vertices; no bitmap rotation or 3D SDK required. */
object Mini3D {
    fun project(v: Vec3, yaw: Float, pitch: Float, scale: Float = 1f): Projected {
        val x = v.x * cos(yaw) + v.z * sin(yaw)
        val z = -v.x * sin(yaw) + v.z * cos(yaw)
        val y = v.y * cos(pitch) - z * sin(pitch)
        val depth = v.y * sin(pitch) + z * cos(pitch)
        val perspective = 3.6f / (3.6f + depth).coerceAtLeast(.4f)
        return Projected(x * perspective * scale, y * perspective * scale, depth)
    }
}

data class Circuit2027(
    val id: String, val round: Int, val country: String, val circuit: String,
    val location: String, val weekend: String, val sprint: Boolean,
    val homologationPending: Boolean, val points: List<Pair<Float, Float>>
) {
    /** Race display name can be Miami/Las Vegas; the host nation remains explicit. */
    val nation:String get()=when(id){"us-2022","us-2012","us-2023"->"United States";"ae-2009"->"United Arab Emirates";"gb-1948"->"United Kingdom";else->country}
}
object CircuitCatalog {
    const val CALENDAR_SOURCE = "https://corp.formula1.com/2027-calendar-announced-with-10-sprint-events/"
    const val GEOMETRY_SOURCE = "https://github.com/bacinger/f1-circuits"
    fun parse(json: String): List<Circuit2027> {
        val array = JSONArray(json)
        require(array.length() == 24) { "Expected all 24 rounds" }
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            val pts = o.getJSONArray("points")
            val points = (0 until pts.length()).map { p ->
                val xy = pts.getJSONArray(p)
                xy.getDouble(0).toFloat() to xy.getDouble(1).toFloat()
            }
            require(points.size in 10..500 && points.all { it.first.isFinite() && it.second.isFinite() && abs(it.first) <= .51 && abs(it.second) <= .51 })
            Circuit2027(o.getString("id"), o.getInt("round"), o.getString("country"), o.getString("circuit"),
                o.getString("location"), o.getString("weekend"), o.getBoolean("sprint"), o.getBoolean("homologationPending"), points)
        }.also { require(it.map { c -> c.id }.distinct().size == 24 && it.map { c -> c.round } == (1..24).toList()) }
    }
}

enum class Compound(val paceOffset: Double, val degradation: Double) {
    Soft(-1.0, .065), Medium(0.0, .043), Hard(.65, .025)
}
data class StrategyPlan(
    val pitLap: Int = 23, val start: Compound = Compound.Medium, val next: Compound = Compound.Hard,
    val rainPercent: Int = 10, val pitLossSeconds: Double = 22.0,
    val degradationFactor: Double = 1.0, val safetyCar: Boolean = false, val laps: Int = 58
)
data class SimulationResult(val medianDeltaSeconds: Double, val lowDeltaSeconds: Double, val highDeltaSeconds: Double, val lapDeltas: List<Double>, val runs: Int)

/** Educational model, not a calibrated forecast or a circuit-specific regulation engine. */
object StrategyEngine {
    fun validate(p: StrategyPlan): String? = when {
        p.laps !in 10..100 || p.pitLap !in 1 until p.laps -> "Pit before the final lap."
        p.start == p.next -> "Choose two different dry compounds for this dry-race exercise."
        p.rainPercent !in 0..100 -> "Rain assumption must be 0–100%."
        !p.pitLossSeconds.isFinite() || p.pitLossSeconds !in 5.0..60.0 -> "Pit loss must be 5–60 seconds."
        !p.degradationFactor.isFinite() || p.degradationFactor !in .25..3.0 -> "Degradation multiplier must be 0.25–3.0."
        else -> null
    }
    fun simulate(p: StrategyPlan): SimulationResult {
        require(validate(p) == null) { validate(p).orEmpty() }
        val baseline = p.copy(pitLap = (p.laps * .46).toInt(), next = if (p.start == Compound.Hard) Compound.Medium else Compound.Hard, safetyCar = false)
        fun lap(plan: StrategyPlan, n: Int, rain: Boolean, wear: Double): Double {
            val compound = if (n <= plan.pitLap) plan.start else plan.next
            val age = if (n <= plan.pitLap) n - 1 else n - plan.pitLap - 1
            return 90.0 + compound.paceOffset + age * compound.degradation * wear - n * .025 +
                (if (n == plan.pitLap) plan.pitLossSeconds * if (plan.safetyCar) .6 else 1.0 else 0.0) +
                (if (rain && n > plan.laps * .7) 3.0 + compound.paceOffset * -.5 else 0.0)
        }
        val random = java.util.Random(2027L)
        val differences = ArrayList<Double>(80)
        val trace = MutableList(p.laps) { 0.0 }
        repeat(80) {
            val rain = random.nextDouble() < p.rainPercent / 100.0
            val wear = p.degradationFactor * (.8 + random.nextDouble() * .4)
            var delta = 0.0
            for (n in 1..p.laps) {
                delta += lap(p, n, rain, wear) - lap(baseline, n, rain, wear)
                trace[n - 1] += delta / 80.0
            }
            differences.add(delta)
        }
        differences.sort()
        return SimulationResult((differences[39] + differences[40]) / 2, differences[8], differences[71], trace, 80)
    }
}

data class StandingRow(val id: String, val name: String, val code: String, val team: String, val position: Int, val points: Double, val wins: Int)
data class StandingSnapshot(val season: Int, val round: Int, val rows: List<StandingRow>)
data class SeasonResult(val round: Int, val race: String, val date: String, val grid: Int, val finish: Int, val points: Double, val status: String)
data class QualifyingResult(val round: Int, val q1Ms: Int?)
data class RivalrySnapshot(val left: List<SeasonResult>, val right: List<SeasonResult>, val qualifyingDeltaMs: Double?, val qualifyingPairs: Int)
data class DataSnapshot<T>(val data: T, val source: String, val fetchedAt: Long, val cached: Boolean, val stale: Boolean = false)
data class LoadState<T>(val snapshot: DataSnapshot<T>? = null, val loading: Boolean = false, val error: String? = null)

object JolpicaNormalizer {
    fun standings(raw: String): StandingSnapshot {
        val list = JSONObject(raw).getJSONObject("MRData").getJSONObject("StandingsTable").getJSONArray("StandingsLists")
        require(list.length() > 0) { "No published standings for this season" }
        val top = list.getJSONObject(0); val rows = top.getJSONArray("DriverStandings")
        return StandingSnapshot(top.getString("season").toInt(), top.getString("round").toInt(), (0 until rows.length()).map { i ->
            val o = rows.getJSONObject(i); val d = o.getJSONObject("Driver")
            StandingRow(d.getString("driverId"), d.getString("givenName") + " " + d.getString("familyName"), d.optString("code", "—"),
                o.getJSONArray("Constructors").optJSONObject(0)?.optString("name").orEmpty(), o.getString("position").toInt(), o.getString("points").toDouble(), o.getString("wins").toInt())
        }.also { data -> require(data.isNotEmpty() && data.size <= 40 && data.all { it.points.isFinite() && it.points >= 0 && it.position > 0 }) })
    }
    fun results(raw: String): List<SeasonResult> {
        val races = JSONObject(raw).getJSONObject("MRData").getJSONObject("RaceTable").getJSONArray("Races")
        require(races.length() <= 40)
        return (0 until races.length()).map { i ->
            val r = races.getJSONObject(i); val result = r.getJSONArray("Results").getJSONObject(0)
            SeasonResult(r.getString("round").toInt(), r.getString("raceName"), r.getString("date"), result.getString("grid").toInt(), result.getString("position").toInt(), result.getString("points").toDouble(), result.getString("status"))
        }.sortedBy { it.round }.also { rows ->
            require(rows.map {it.round}.distinct().size==rows.size && rows.all {it.round in 1..40 && it.grid in 0..40 && it.finish in 1..40 && it.points.isFinite() && it.points in 0.0..100.0}) {"Malformed season result"}
        }
    }
    fun qualifying(raw: String): List<QualifyingResult> {
        val races = JSONObject(raw).getJSONObject("MRData").getJSONObject("RaceTable").getJSONArray("Races")
        return (0 until races.length()).map { i ->
            val r = races.getJSONObject(i); val q = r.getJSONArray("QualifyingResults").getJSONObject(0)
            QualifyingResult(r.getString("round").toInt(), lapMs(q.optString("Q1")))
        }
    }
    private fun lapMs(s: String): Int? {
        val p = s.split(':'); if (p.size != 2) return null
        return ((p[0].toIntOrNull() ?: return null) * 60000 + ((p[1].toDoubleOrNull() ?: return null) * 1000).roundToInt()).takeIf { it in 10000..600000 }
    }
    fun providerId(id: String) = if (id == "verstappen") "max_verstappen" else id
}

data class ReplayPoint(val timeMs: Long, val driverId: String, val x: Float, val y: Float, val speedKph: Int? = null)
data class ReplayClip(val title: String, val demo: Boolean, val source: String, val startUtc: String, val durationMs: Long, val points: List<ReplayPoint>, val route: List<Pair<Float, Float>>, val fetchedAt: Long = System.currentTimeMillis())
object ReplayEngine {
    fun at(clip: ReplayClip, timeMs: Long): List<ReplayPoint> = clip.points.groupBy { it.driverId }.values.mapNotNull { samples ->
        samples.lastOrNull { it.timeMs <= timeMs }?.takeIf { timeMs - it.timeMs <= 1500 }
    }
}
