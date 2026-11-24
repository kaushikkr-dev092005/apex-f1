package com.example

import com.example.data.notifications.*
import com.example.data.race.*
import org.junit.Assert.*
import org.junit.Test

class NotificationPolicyTest {
    private fun event(id: String, kind: EventKind = EventKind.Lap, driver: String = "leclerc") = RaceEvent(id, kind, "Sample", "Demo", 24, 1000, driver)
    private val prefs = AlertPreferences(enabled = setOf("lap", "red", "safety", "fastest"))
    @Test fun deduplicatesAndThrottlesLapNotifications() {
        val policy = NotificationPolicy()
        assertTrue(policy.shouldSend(event("a"), prefs, 100000, 12))
        assertFalse(policy.shouldSend(event("a"), prefs, 200000, 12))
        assertFalse(policy.shouldSend(event("b"), prefs, 110000, 12))
        assertTrue(policy.shouldSend(event("c"), prefs, 161000, 12))
    }
    @Test fun lapAlertsOnlyFollowSelectedDrivers() {
        assertFalse(NotificationPolicy().shouldSend(event("a", driver="piastri"), prefs, 100000, 12))
    }
    @Test fun overnightQuietHoursAndDaytimeRangeAreCorrect() {
        assertTrue(isQuietHour(23, 23, 7)); assertTrue(isQuietHour(6, 23, 7)); assertFalse(isQuietHour(7, 23, 7))
        assertTrue(isQuietHour(12, 10, 15)); assertFalse(isQuietHour(15, 10, 15))
        assertFalse(NotificationPolicy().shouldSend(event("a", EventKind.RedFlag), prefs.copy(quietEnabled=true), 100000, 23))
    }
    @Test fun criticalEventsOutrankRoutineDigestAndCooldown() {
        val policy = NotificationPolicy()
        assertFalse(policy.shouldSend(event("routine"), prefs.copy(digest=true), 100000, 12))
        assertTrue(policy.shouldSend(event("critical", EventKind.RedFlag), prefs.copy(digest=true), 100001, 12))
        assertTrue(policy.shouldSend(event("critical2", EventKind.RedFlag), prefs, 100002, 12))
    }
    @Test fun respectsDisabledCategories() {
        assertFalse(NotificationPolicy().shouldSend(event("a", EventKind.FastestLap), prefs.copy(enabled=emptySet()), 100000, 12))
    }
}
