package com.example.data.notifications

import com.example.data.race.EventKind
import com.example.data.race.RaceEvent
import com.example.data.race.EventDeduplicator

data class AlertPreferences(
    val enabled: Set<String> = setOf("race", "red", "safety", "fastest", "finish"),
    val followedDrivers: Set<String> = setOf("leclerc"),
    val followedConstructors: Set<String> = setOf("ferrari"),
    val quietEnabled: Boolean = false,
    val quietStart: Int = 23,
    val quietEnd: Int = 7,
    val digest: Boolean = false,
    val throttle: Boolean = true
)

interface NotificationService {
    val remotePushConfigured: Boolean
    fun notify(event: RaceEvent): Boolean
}

fun category(kind: EventKind): String = when (kind) {
    EventKind.Start -> "race"
    EventKind.RedFlag -> "red"
    EventKind.SafetyCar, EventKind.YellowFlag, EventKind.GreenFlag -> "safety"
    EventKind.FastestLap -> "fastest"
    EventKind.Lap -> "lap"
    EventKind.Pit, EventKind.Position -> "position"
    EventKind.Finish -> "finish"
}

fun isQuietHour(hour: Int, start: Int, end: Int): Boolean = when {
    start == end -> true
    start < end -> hour in start until end
    else -> hour >= start || hour < end
}

class NotificationPolicy {
    private val seen = EventDeduplicator(512)
    private val lastSent = mutableMapOf<String, Long>()
    fun shouldSend(event: RaceEvent, prefs: AlertPreferences, arrival: Long, localHour: Int): Boolean {
        if (!seen.accept(event.id)) return false
        val kind = category(event.kind)
        if (kind !in prefs.enabled) return false
        if (event.kind == EventKind.Lap && event.driverId !in prefs.followedDrivers) return false
        if (prefs.quietEnabled && isQuietHour(localHour, prefs.quietStart, prefs.quietEnd)) return false
        val critical = event.kind in setOf(EventKind.RedFlag, EventKind.SafetyCar, EventKind.Finish)
        if (!critical && prefs.digest) return false // In-app timeline is the local digest; remote digest needs a backend.
        val key = "$kind:${event.driverId.orEmpty()}"
        if (!critical && prefs.throttle && arrival - (lastSent[key] ?: Long.MIN_VALUE / 2) < 60000) return false
        lastSent[key] = arrival
        return true
    }
}
