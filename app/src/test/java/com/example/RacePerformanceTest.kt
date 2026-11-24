package com.example

import com.example.data.race.*
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/** Host ingestion benchmark, explicitly not a device/render/network latency measurement. */
class RacePerformanceTest {
    @Test fun measureBoundedDemoIngestion() {
        val provider = DemoRaceProvider()
        val ids = provider.frame(0).telemetry.map { it.driverId }
        val store = RaceStore(provider, ids)
        repeat(300) { store.accept(provider.frame(it.toLong())) }
        val generation = mutableListOf<Long>()
        val ingestion = mutableListOf<Long>()
        repeat(2000) { index ->
            val started = System.nanoTime()
            val packet = provider.frame((index + 300).toLong())
            val generated = System.nanoTime()
            store.accept(packet)
            val finished = System.nanoTime()
            generation += generated - started
            ingestion += finished - generated
        }
        fun percentile(values: List<Long>, p: Double): Double = values.sorted()[(values.size * p).toInt().coerceAtMost(values.lastIndex)] / 1000000.0
        assertEquals(240, store.history("leclerc").value.size)
        val output = """{
          "workload": "2000 sequential packets after 300 warm-up packets; 10 drivers, telemetry and location at every packet, timing every fourth packet",
          "environment": "Host JVM unit test; no Android device or network",
          "generationP95Ms": ${percentile(generation, .95)},
          "ingestionP50Ms": ${percentile(ingestion, .50)},
          "ingestionP95Ms": ${percentile(ingestion, .95)},
          "ingestionP99Ms": ${percentile(ingestion, .99)},
          "telemetrySamplesPerDriver": ${store.history("leclerc").value.size},
          "eventHistorySize": ${store.events.value.size},
          "limits": "Does not measure UI rendering, device FPS, full-session heap stability, provider latency, server processing, or network delivery. Not an end-to-end real-time claim."
        }"""
        val file = File("../docs/measurements/demo-ingestion.json")
        file.parentFile?.mkdirs(); file.writeText(output)
        println(output)
    }
}
