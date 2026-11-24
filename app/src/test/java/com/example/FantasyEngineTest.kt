package com.example

import com.example.data.fantasy.*
import com.example.data.mock.MockData
import org.junit.Assert.*
import org.junit.Test

class FantasyEngineTest {
    @Test fun captainDoublesOnlyTheDriverScore() {
        assertEquals(28, FantasyEngine.score("leclerc"))
        assertEquals(56, FantasyEngine.score("leclerc", true))
        assertEquals(20, FantasyEngine.score("piastri"))
        assertEquals(43, FantasyEngine.constructorScore("ferrari", MockData.drivers))
    }
    @Test fun breakdownAddsUpAndNoNegativePositionBonus() {
        val lines = FantasyEngine.breakdown(FantasyResult("sample", 3, 1), true)
        assertEquals(30, lines.sumOf { it.points })
        assertEquals(0, lines[1].points)
    }
    @Test fun validatesNamesDuplicateDriversAndUnknownEntities() {
        fun validate(name: String, ids: List<String>, constructor: String?) = FantasyEngine.validate(name, ids, constructor, MockData.drivers, MockData.constructors)
        assertNotNull(validate(" ", listOf("leclerc", "piastri"), "ferrari"))
        assertNotNull(validate("Team", listOf("leclerc", "leclerc"), "ferrari"))
        assertNotNull(validate("Team", listOf("leclerc", "unknown"), "ferrari"))
        assertNotNull(validate("Team", listOf("leclerc", "piastri"), null))
        assertNull(validate("Team", listOf("leclerc", "piastri"), "ferrari"))
    }
    @Test fun rejectsOverBudgetAndAcceptsExactBudget() {
        val drivers = MockData.drivers.take(2).map { it.copy(cost = 40.0) }
        val constructors = listOf(MockData.constructors.first().copy(cost = 20.0))
        assertNull(FantasyEngine.validate("Team", drivers.map { it.id }, constructors.first().id, drivers, constructors))
        assertNotNull(FantasyEngine.validate("Team", drivers.map { it.id }, constructors.first().id, drivers, constructors.map { it.copy(cost = 20.1) }))
    }
}
