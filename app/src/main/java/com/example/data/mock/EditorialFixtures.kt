package com.example.data.mock

import com.example.data.database.NewsEntity

/** Original educational demo copy, not reporting and not attributed to real journalists. */
object EditorialFixtures {
    val articles = listOf(
        NewsEntity("apex-editorial-1", "Monaco. No room for ordinary.",
            "A closer look at the precision, patience and strategy of a street-circuit weekend.",
            "On a street circuit, confidence is measured in millimetres. With barriers close and overtaking opportunities scarce, a qualifying lap can shape an entire weekend.\n\nTHE MARGIN THAT MATTERS\nA car that responds predictably through slow corners can be more valuable here than one with the highest straight-line speed. Mechanical grip, tyre preparation and driver trust become central to the setup.\n\nTHE LONG GAME\nTrack position makes pit-stop timing especially consequential. A clear gap in traffic can offer an opportunity; a poorly timed stop can undo a strong qualifying result. Race neutralisations introduce another layer of uncertainty.\n\nWATCH THE GAPS\nUse the timing tower to follow intervals, compare completed sectors, and check the age of the data. A changing gap tells you something about relative pace, but it cannot reveal an exact car location.\n\nThis is original APEX demonstration content. It describes general racing concepts and does not report a current race result.",
            "RACE WEEKEND", "", "2024-05-26T09:00:00Z"),
        NewsEntity("apex-editorial-2", "The undercut, explained in seconds.",
            "Why a fresh set of tyres can change the race before the next corner.",
            "An undercut is an attempt to gain track position by stopping before a nearby rival. Fresh tyres can allow the first driver to complete a faster out-lap while the other driver remains on older tyres.\n\nThe tactic depends on tyre warm-up, traffic, pit-lane loss and the remaining race distance. It is not guaranteed: a slow out-lap or a safety car can change the outcome.\n\nCompare lap times before and after each pit stop in the race-control timeline. The best comparison accounts for traffic and whether each lap includes a pit-lane entry or exit.\n\nOriginal educational demo article. No current team strategy or race result is claimed.",
            "ANALYSIS", "", "2024-05-25T15:00:00Z"),
        NewsEntity("apex-editorial-3", "Reading the speed trace.",
            "Braking points, corner exits and the shape of a fast lap.",
            "A speed trace plots speed against a shared time or distance axis. A steep drop generally marks braking; the recovery after a low-speed section reveals acceleration.\n\nTwo traces only form a useful comparison when their axes are aligned. This demo aligns drivers by simulated event time. A full lap comparison would require distance-aligned samples from an authorized data provider.\n\nAPEX breaks the line when a sample gap exceeds one second, rather than hiding missing information with a smooth curve.\n\nOriginal educational demo article; all displayed telemetry is simulated.",
            "TECHNICAL", "", "2024-05-24T12:00:00Z"),
        NewsEntity("apex-editorial-4", "Build a team. Back your instinct.",
            "Two drivers, one constructor and 100 million reasons to think strategically.",
            "APEX fantasy is an independent exhibition game. Select two different drivers and one constructor within a 100M credit budget. Your captain doubles that driver's score.\n\nA top-ten finish awards 25, 18, 15, 12, 10, 8, 6, 4, 2 or 1 points. Every position gained from the grid adds two points. A fastest lap adds three points. The constructor earns the sum of its listed drivers' unmultiplied scores.\n\nScores use a fixed, labeled sample result. Private league codes create local previews; cross-device competition needs a backend. This app has no affiliation with an official fantasy product.",
            "FANTASY", "", "2024-05-23T10:00:00Z")
    )
}
