package com.example.data.race

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

/** One producer, independent UI subscriptions, bounded telemetry and event history. */
class RaceStore(private val provider: RaceProvider, driverIds: List<String>, private val now: () -> Long = System::currentTimeMillis) {
    private val _session = MutableStateFlow(RaceSession(demo = provider.isDemo))
    val session = _session.asStateFlow()
    private val _timing = MutableStateFlow<List<Timing>>(emptyList())
    val timing = _timing.asStateFlow()
    private val _locations = MutableStateFlow<List<CarLocation>>(emptyList())
    val locations = _locations.asStateFlow()
    private val _events = MutableStateFlow<List<RaceEvent>>(emptyList())
    val events = _events.asStateFlow()
    private val _health = MutableStateFlow(FeedHealth())
    val health = _health.asStateFlow()
    private val samples = driverIds.associateWith { MutableStateFlow<TelemetrySample?>(null) }
    private val history = driverIds.associateWith { MutableStateFlow<List<TelemetrySample>>(emptyList()) }
    private val dedup = EventDeduplicator()
    private var lastSequence = -1L
    private var producer: Job? = null
    private var monitor: Job? = null
    fun telemetry(id: String): StateFlow<TelemetrySample?> = samples.getValue(id).asStateFlow()
    fun history(id: String): StateFlow<List<TelemetrySample>> = history.getValue(id).asStateFlow()

    fun accept(packet: RacePacket): Boolean {
        if (!RaceValidation.valid(packet) || packet.sequence <= lastSequence) {
            _health.value = _health.value.copy(rejectedPackets = _health.value.rejectedPackets + 1)
            return false
        }
        lastSequence = packet.sequence
        packet.session?.let { _session.value = it.copy(demo = provider.isDemo) }
        packet.timing?.let { _timing.value = it }
        packet.locations?.let { _locations.value = it }
        packet.telemetry.forEach { sample ->
            samples[sample.driverId]?.value = sample
            history[sample.driverId]?.let { flow -> flow.value = (flow.value.takeLast(239) + sample) }
        }
        val fresh = packet.events.filter { dedup.accept(it.id) }
        if (fresh.isNotEmpty()) _events.value = (fresh.reversed() + _events.value).take(120)
        _health.value = _health.value.copy(connection = Connection.Live, lastArrivalMs = now(), lastEventMs = packet.eventTimeMs, retries = 0)
        return true
    }

    fun start(scope: CoroutineScope) {
        if (producer?.isActive == true) return
        producer = scope.launch {
            var attempt = 0
            while (isActive) {
                _health.value = _health.value.copy(connection = if (attempt == 0) Connection.Connecting else Connection.Reconnecting, retries = attempt)
                try {
                    (provider as? DemoRaceProvider)?.prepareSubscription()
                    provider.packets().collect { packet -> if (accept(packet)) attempt = 0 }
                    throw java.io.IOException("Race feed closed")
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) {
                    attempt++
                    _health.value = _health.value.copy(connection = Connection.Reconnecting, retries = attempt)
                    delay(reconnectDelay(attempt - 1, Random.nextDouble()))
                }
            }
        }
        monitor = scope.launch {
            while (isActive) {
                delay(1000)
                val health = _health.value
                if (health.connection != Connection.Reconnecting) _health.value = health.copy(connection = connectionAt(health.lastArrivalMs, now()))
            }
        }
    }
    fun stop() {
        producer?.cancel(); monitor?.cancel()
        producer = null; monitor = null
        _health.value = _health.value.copy(connection = Connection.Offline)
    }
    fun reconnect(scope: CoroutineScope) { stop(); start(scope) }
}
