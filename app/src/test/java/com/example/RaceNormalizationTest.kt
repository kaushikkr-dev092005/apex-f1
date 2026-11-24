package com.example

import com.example.data.race.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RaceNormalizationTest {
    @Test fun normalizesHeartbeatAndDoesNotInventMissingLocations() {
        val packet = RaceJson.normalize(JSONObject("""{"sequence":1,"eventTimeMs":1716732300000}"""))
        assertNull(packet.locations)
        assertNull(packet.timing)
        assertTrue(packet.telemetry.isEmpty())
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsInvalidCoordinates() {
        RaceJson.normalize(JSONObject("""{"sequence":2,"eventTimeMs":1716732300000,"locations":[{"driverId":"leclerc","x":2,"y":0.5}]}"""))
    }
    @Test fun mapsTimingWithoutUsingItAsLocation() {
        val packet = RaceJson.normalize(JSONObject("""{"sequence":3,"eventTimeMs":1716732300000,"timing":[{"driverId":"leclerc","position":1,"lastLapMs":73611,"gapMs":0,"intervalMs":0,"sectorsMs":[18672,34581,20358]}]}"""))
        assertEquals(73611, packet.timing!!.first().lastLapMs)
        assertNull(packet.locations)
    }
}
