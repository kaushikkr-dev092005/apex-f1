package com.example.data.race

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Optional HTTPS fallback for an authorized normalization backend. Not enabled in the demo.
 * Provider entitlement, upstream credentials and licensing belong on that backend.
 */
class PollingRaceProvider(
    private val endpoint: String,
    private val sessionToken: () -> String? = { null },
    private val intervalMs: Long = 1000
) : RaceProvider {
    override val isDemo = false
    init { require(URL(endpoint).protocol == "https") { "Race endpoint must use HTTPS" }; require(intervalMs >= 250) }
    override fun packets(): Flow<RacePacket> = flow {
        while (true) {
            currentCoroutineContext().ensureActive()
            val connection = URL(endpoint).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                connection.setRequestProperty("Accept", "application/json")
                sessionToken()?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
                check(connection.responseCode in 200..299) { "Race endpoint unavailable (${connection.responseCode})" }
                val body = StringBuilder()
                connection.inputStream.bufferedReader().use { reader ->
                    val buffer = CharArray(4096)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = reader.read(buffer)
                        if (count < 0) break
                        check(body.length + count <= 1048576) { "Race snapshot exceeds size limit" }
                        body.append(buffer, 0, count)
                    }
                }
                emit(RaceJson.normalize(JSONObject(body.toString())))
            } finally { connection.disconnect() }
            delay(intervalMs)
        }
    }.flowOn(Dispatchers.IO)
}

object RaceJson {
    private fun <T> JSONArray.mapObjects(block: (JSONObject) -> T): List<T> = (0 until length()).map { block(getJSONObject(it)) }
    fun normalize(json: JSONObject): RacePacket {
        val eventTime = json.getLong("eventTimeMs")
        val packet = RacePacket(
            sequence = json.getLong("sequence"), eventTimeMs = eventTime,
            session = json.optJSONObject("session")?.let {
                RaceSession(it.getString("name"), it.getString("circuit"), it.getString("country"), it.getString("sessionType"), it.getInt("lap"), it.getInt("totalLaps"), TrackStatus.valueOf(it.getString("status")), demo = false)
            },
            timing = json.optJSONArray("timing")?.mapObjects { row ->
                val sectors = row.getJSONArray("sectorsMs")
                Timing(row.getString("driverId"), row.getInt("position"), row.getInt("lastLapMs"), row.getInt("gapMs"), row.getInt("intervalMs"),
                    (0 until sectors.length()).map { sectors.getInt(it) }, row.optString("tyre", "—"), row.optInt("tyreAge", 0), row.optInt("stops", 0), row.optBoolean("personalBest"), row.optBoolean("fastest"))
            },
            locations = json.optJSONArray("locations")?.mapObjects { CarLocation(it.getString("driverId"), it.getDouble("x").toFloat(), it.getDouble("y").toFloat(), it.optBoolean("approximate", false)) },
            telemetry = json.optJSONArray("telemetry")?.mapObjects {
                TelemetrySample(it.getString("driverId"), it.getLong("eventTimeMs"), it.getInt("speedKph"), it.getDouble("throttle").toFloat(), it.getDouble("brake").toFloat(), it.getInt("gear"), it.getInt("rpm"), it.getBoolean("drs"))
            } ?: emptyList(),
            events = json.optJSONArray("events")?.mapObjects {
                RaceEvent(it.getString("id"), EventKind.valueOf(it.getString("kind")), it.getString("title"), it.getString("detail"), it.getInt("lap"), it.getLong("eventTimeMs"), if (it.isNull("driverId")) null else it.optString("driverId").takeIf(String::isNotBlank))
            } ?: emptyList()
        )
        require(RaceValidation.valid(packet)) { "Malformed normalized race packet" }
        return packet
    }
}
