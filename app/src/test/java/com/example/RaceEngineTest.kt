package com.example

import com.example.data.race.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import kotlinx.coroutines.awaitCancellation
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RaceEngineTest {
    private val ids = listOf("leclerc", "piastri", "sainz", "norris", "russell", "verstappen", "hamilton", "alonso", "perez", "hulkenberg")
    @Test fun demoFramesAreDeterministic() {
        assertEquals(DemoRaceProvider().frame(123), DemoRaceProvider().frame(123))
        assertTrue((0L..1000L).all { RaceValidation.valid(DemoRaceProvider().frame(it)) })
    }
    @Test fun rejectsMalformedTelemetryAndCoordinates() {
        val frame = DemoRaceProvider().frame(0)
        assertFalse(RaceValidation.valid(frame.copy(telemetry = listOf(frame.telemetry.first().copy(speedKph = 999)))))
        assertFalse(RaceValidation.valid(frame.copy(locations = listOf(CarLocation("leclerc", Float.NaN, .5f)))))
        assertFalse(RaceValidation.valid(frame.copy(timing = frame.timing!! + frame.timing.first())))
    }
    @Test fun rejectsDuplicateAndOldSequenceNumbers() {
        val store = RaceStore(DemoRaceProvider(), ids) { 1000L }
        assertTrue(store.accept(DemoRaceProvider().frame(4)))
        assertFalse(store.accept(DemoRaceProvider().frame(4)))
        assertFalse(store.accept(DemoRaceProvider().frame(3)))
        assertEquals(2, store.health.value.rejectedPackets)
    }
    @Test fun deduplicatesEventsAcrossPackets() {
        val store = RaceStore(DemoRaceProvider(), ids)
        val frame = DemoRaceProvider().frame(0)
        store.accept(frame)
        store.accept(frame.copy(sequence = 1))
        assertEquals(1, store.events.value.size)
    }
    @Test fun historiesAreBoundedDuringExtendedSession() {
        val store = RaceStore(DemoRaceProvider(), ids)
        val provider = DemoRaceProvider()
        repeat(4000) { tick ->
            val frame = provider.frame(tick.toLong())
            store.accept(frame.copy(events = listOf(RaceEvent("event-$tick", EventKind.Lap, "Sample", "Fixture", 24, frame.eventTimeMs))))
        }
        assertEquals(240, store.history("leclerc").value.size)
        assertEquals(120, store.events.value.size)
    }
    @Test fun freshnessTransitionsAreExplicit() {
        assertEquals(Connection.Connecting, connectionAt(null, 0))
        assertEquals(Connection.Live, connectionAt(1000, 2000))
        assertEquals(Connection.Stale, connectionAt(1000, 5000))
        assertEquals(Connection.Offline, connectionAt(1000, 10000))
    }
    @Test fun reconnectBackoffIsBoundedWithJitter() {
        assertEquals(800L, reconnectDelay(0, 0.0))
        assertEquals(1200L, reconnectDelay(0, 1.0))
        assertTrue(reconnectDelay(50, .5) <= 30000)
    }
    @Test fun redFlagStopsSimulatedCarsAndDisablesDrs() {
        val provider = DemoRaceProvider()
        assertEquals(provider.frame(281).locations, provider.frame(300).locations)
        assertTrue(provider.frame(300).telemetry.all { it.speedKph == 0 && !it.drs })
    }
    @Test fun startDoesNotCreateDuplicateSubscriptionsAndStopCleansUp() = runTest {
        var active = 0
        val provider = object : RaceProvider {
            override val isDemo = true
            override fun packets() = flow<RacePacket> {
                active++
                try { emit(DemoRaceProvider().frame(0)); awaitCancellation() } finally { active-- }
            }
        }
        val store = RaceStore(provider, ids)
        store.start(this); store.start(this); runCurrent()
        assertEquals(1, active)
        store.stop(); runCurrent()
        assertEquals(0, active)
        assertEquals(Connection.Offline, store.health.value.connection)
    }
    @Test fun interruptedFeedRecoversWithoutLosingHistory() = runTest {
        val provider = DemoRaceProvider()
        val store = RaceStore(provider, ids)
        store.start(this); runCurrent(); advanceTimeBy(1000); runCurrent()
        val count = store.history("leclerc").value.size
        provider.disconnected = true
        advanceTimeBy(1000); runCurrent()
        assertEquals(Connection.Reconnecting, store.health.value.connection)
        provider.disconnected = false
        store.reconnect(this); runCurrent()
        assertEquals(Connection.Live, store.health.value.connection)
        assertTrue(store.history("leclerc").value.size > count)
        store.stop()
    }
    @Test fun formattingKeepsMillisecondsAndUnitsHonest() {
        assertEquals("1:13.611", formatLap(73611))
        assertEquals("+1.284", formatGap(1284))
        assertEquals("LEADER", formatGap(0))
    }
}
