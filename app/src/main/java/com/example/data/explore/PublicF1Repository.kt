package com.example.data.explore

import com.example.data.database.DataCacheDao
import com.example.data.database.DataCacheEntity
import com.example.data.race.DemoCircuit
import com.example.data.race.DemoRaceProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object UtcTime {
    fun parse(value: String): Long {
        val match = Regex("^(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2})(?:\\.(\\d+))?(Z|[+-]\\d{2}:\\d{2})$").matchEntire(value) ?: error("Missing UTC timestamp")
        val zone = match.groupValues[3].let { if (it == "Z") "UTC" else "GMT$it" }
        val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone(zone); isLenient = false }.parse(match.groupValues[1]) ?: error("Invalid timestamp")
        return date.time + match.groupValues[2].take(3).padEnd(3, '0').toLong()
    }
    fun format(ms: Long) = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(java.util.Date(ms))
}

/** Public historical endpoints only. Paid live feeds and all provider secrets belong behind a backend. */
class PublicF1Repository(private val cache: DataCacheDao) {
    private val requestGate=Mutex()
    private var nextRequestNanos=0L
    private suspend fun <T> load(url: String, ttlMs: Long = 900_000, force: Boolean = false, parse: (String) -> T): DataSnapshot<T> = withContext(Dispatchers.IO) {
        val previous = cache.get(url)
        val now = System.currentTimeMillis()
        if (!force && previous != null && now - previous.fetchedAt < ttlMs) {
            return@withContext DataSnapshot(parse(previous.payload), previous.source, previous.fetchedAt, cached = true)
        }
        try {
            currentCoroutineContext().ensureActive()
            // Serialize admission only, not response parsing; prevent rapid UI changes creating bursts.
            requestGate.withLock {
                val remaining=nextRequestNanos-System.nanoTime()
                if(remaining>0)delay((remaining/1_000_000+1).coerceAtMost(450))
                nextRequestNanos=System.nanoTime()+450_000_000
            }
            val connection = URL(url).openConnection() as HttpURLConnection
            val raw = try {
                connection.connectTimeout = 10_000; connection.readTimeout = 12_000
                connection.setRequestProperty("User-Agent", "APEX/0.3.0 Android")
                connection.setRequestProperty("Accept", "application/json")
                val code = connection.responseCode
                if (code == 429) error("Provider rate limit reached. Please wait before retrying.")
                check(code in 200..299) { "Provider unavailable (HTTP $code)" }
                connection.inputStream.bufferedReader().use { reader ->
                    val out = StringBuilder(); val chars = CharArray(8192)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = reader.read(chars); if (count < 0) break
                        check(out.length + count <= 4_000_000) { "Response exceeds safe cache limit" }
                        out.append(chars, 0, count)
                    }
                    out.toString()
                }
            } finally { connection.disconnect() }
            val normalized = parse(raw) // Validate before replacing a known-good cache entry.
            val received = System.currentTimeMillis()
            cache.put(DataCacheEntity(url, raw, received, url)); cache.trim()
            DataSnapshot(normalized, url, received, cached = false)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) {
            if (previous != null) DataSnapshot(parse(previous.payload), previous.source, previous.fetchedAt, cached = true, stale = true)
            else throw failure
        }
    }
    suspend fun standings(force: Boolean = false) = load("https://api.jolpi.ca/ergast/f1/2026/driverstandings/?limit=100", force = force, parse = JolpicaNormalizer::standings)
    suspend fun results(id: String, season: Int, force: Boolean = false): DataSnapshot<List<SeasonResult>> {
        require(id.matches(Regex("[a-z_]+")) && season in 1950..2026)
        return load("https://api.jolpi.ca/ergast/f1/$season/drivers/${JolpicaNormalizer.providerId(id)}/results/?limit=100", ttlMs = 3_600_000, force = force, parse = JolpicaNormalizer::results)
    }
    suspend fun qualifying(id: String, season: Int): DataSnapshot<List<QualifyingResult>> {
        require(id.matches(Regex("[a-z_]+")) && season in 1950..2026)
        return load("https://api.jolpi.ca/ergast/f1/$season/drivers/${JolpicaNormalizer.providerId(id)}/qualifying/?limit=100", ttlMs = 3_600_000, parse = JolpicaNormalizer::qualifying)
    }
    private fun openUrl(endpoint: String, vararg params: Pair<String, String>) = "https://api.openf1.org/v1/$endpoint?" + params.joinToString("&") { (key, value) -> key + "=" + URLEncoder.encode(value, "UTF-8") }

    suspend fun monacoArchive(): ReplayClip = withContext(Dispatchers.IO) {
        val sessions = load(openUrl("sessions", "year" to "2024", "country_name" to "Monaco", "session_name" to "Race"), 86_400_000) { JSONArray(it) }.data
        check(sessions.length() > 0) { "No Monaco race archive is available" }
        val session = sessions.getJSONObject(0).getInt("session_key").toString()
        val laps = load(openUrl("laps", "session_key" to session, "driver_number" to "16", "lap_number" to "10"), 86_400_000) { JSONArray(it) }.data
        check(laps.length() > 0) { "Recorded lap is unavailable" }
        val lap = laps.getJSONObject(0)
        val start = UtcTime.parse(lap.getString("date_start"))
        val duration = (lap.getDouble("lap_duration") * 1000).toLong()
        check(duration in 30_000..180_000) { "Lap duration is missing or outside the supported window" }
        data class Raw(val time: Long, val id: String, val x: Float, val y: Float)
        val raw = mutableListOf<Raw>()
        val speeds = mutableMapOf<String, List<Pair<Long, Int>>>()
        var stale = false
        var oldest = Long.MAX_VALUE
        for ((number, id) in listOf("16" to "leclerc", "81" to "piastri")) {
            val common = arrayOf("session_key" to session, "driver_number" to number, "date>" to UtcTime.format(start), "date<" to UtcTime.format(start + duration))
            val locations = load(openUrl("location", *common), 86_400_000) { JSONArray(it) }
            stale = stale || locations.stale; oldest = minOf(oldest, locations.fetchedAt)
            val array = locations.data
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                val x = o.optDouble("x", Double.NaN).toFloat(); val y = o.optDouble("y", Double.NaN).toFloat()
                if (x.isFinite() && y.isFinite()) raw.add(Raw(UtcTime.parse(o.getString("date")) - start, id, x, y))
            }
            // No continuous polling or unbounded full-race telemetry download.
            kotlinx.coroutines.delay(450)
            val cars = load(openUrl("car_data", *common), 86_400_000) { JSONArray(it) }.data
            speeds[id] = (0 until cars.length()).mapNotNull { i ->
                val o = cars.getJSONObject(i); val speed = o.optInt("speed", -1)
                if (speed !in 0..400) null else (UtcTime.parse(o.getString("date")) - start) to speed
            }.sortedBy { it.first }
            kotlinx.coroutines.delay(450)
        }
        check(raw.size >= 10 && raw.size <= 4000) { "Not enough recorded coordinates for this lap" }
        val minX = raw.minOf { it.x }; val maxX = raw.maxOf { it.x }
        val minY = raw.minOf { it.y }; val maxY = raw.maxOf { it.y }
        val span = maxOf(maxX - minX, maxY - minY)
        check(span > 0f)
        val normalized = raw.sortedBy { it.time }.map { p ->
            val speed = speeds[p.id]?.lastOrNull { it.first <= p.time }?.takeIf { p.time - it.first < 1500 }?.second
            ReplayPoint(p.time, p.id, (p.x - (minX + maxX) / 2) / span, -(p.y - (minY + maxY) / 2) / span, speed)
        }.filter { it.timeMs in 0..duration }
        ReplayClip("Monaco 2024 · Lap 10", false, "https://api.openf1.org/v1/location?session_key=$session", UtcTime.format(start), duration,
            normalized, normalized.filter { it.driverId == "leclerc" }.map { it.x to it.y }, oldest).let {
            if (stale) it.copy(title = it.title + " · cached") else it
        }
    }

    companion object {
        fun demoReplay(): ReplayClip {
            val demo = DemoRaceProvider()
            val samples = (0L..120L).flatMap { i ->
                val packet = demo.frame(i * 2)
                packet.locations.orEmpty().filter { it.driverId in listOf("leclerc", "piastri") }.map { p ->
                    ReplayPoint(i * 500, p.driverId, p.x - .5f, p.y - .5f, packet.telemetry.find { it.driverId == p.driverId }?.speedKph)
                }
            }
            return ReplayClip("Monaco exhibition · 60 seconds", true, "APEX deterministic demo", "2024-05-26T13:25:00.000Z", 60_000, samples, DemoCircuit.points.map { (x,y) -> x - .5f to y - .5f })
        }
    }
}
