package com.example.data.fantasy

import com.example.data.model.Constructor
import com.example.data.model.Driver

data class ScoreLine(val label: String, val points: Int)
data class FantasyResult(val driverId: String, val finish: Int, val grid: Int, val fastestLap: Boolean = false)

/** APEX exhibition rules; independent of any official fantasy game. */
object FantasyEngine {
    private val finishPoints = listOf(25, 18, 15, 12, 10, 8, 6, 4, 2, 1)
    val sampleResults = listOf(
        FantasyResult("leclerc", 1, 1, true), FantasyResult("piastri", 2, 3),
        FantasyResult("sainz", 3, 2), FantasyResult("norris", 4, 4),
        FantasyResult("russell", 5, 5), FantasyResult("verstappen", 6, 6),
        FantasyResult("hamilton", 7, 7), FantasyResult("alonso", 8, 8),
        FantasyResult("perez", 9, 9), FantasyResult("hulkenberg", 10, 10)
    )
    fun breakdown(result: FantasyResult, captain: Boolean = false): List<ScoreLine> {
        require(result.finish > 0 && result.grid > 0)
        val base = listOf(
            ScoreLine("Finish P${result.finish}", finishPoints.getOrElse(result.finish - 1) { 0 }),
            ScoreLine("Positions gained · 2 pts each", (result.grid - result.finish).coerceAtLeast(0) * 2),
            ScoreLine("Fastest lap · 3 pts", if (result.fastestLap) 3 else 0)
        )
        return if (captain) base + ScoreLine("Captain · 2× driver score", base.sumOf { it.points }) else base
    }
    fun score(id: String, captain: Boolean = false): Int = sampleResults.find { it.driverId == id }?.let { breakdown(it, captain).sumOf { line -> line.points } } ?: 0
    fun validate(name: String, ids: List<String>, constructorId: String?, drivers: List<Driver>, constructors: List<Constructor>): String? {
        if (name.trim().length !in 2..30) return "Team name must be 2–30 characters."
        if (ids.size != 2 || ids.distinct().size != 2) return "Choose two different drivers."
        if (ids.any { id -> drivers.none { it.id == id } }) return "One of these drivers is unavailable."
        val constructor = constructors.find { it.id == constructorId } ?: return "Choose one constructor."
        if (ids.sumOf { id -> drivers.first { it.id == id }.cost } + constructor.cost > 100.0) return "Your team exceeds the 100M credit budget."
        return null
    }
    fun constructorScore(constructorId: String, drivers: List<Driver>): Int {
        val teamName = mapOf("redbull" to "Red Bull Racing", "mclaren" to "McLaren", "ferrari" to "Ferrari", "mercedes" to "Mercedes", "astonmartin" to "Aston Martin", "haas" to "Haas")[constructorId]
        return drivers.filter { it.team == teamName }.sumOf { score(it.id) }
    }
}
