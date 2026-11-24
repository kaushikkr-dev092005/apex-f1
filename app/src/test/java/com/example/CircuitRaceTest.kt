package com.example

import com.example.data.race3d.CircuitRace
import com.example.data.race3d.CircuitRoute
import com.example.data.race3d.DrivingInput
import com.example.data.race3d.RACE_GRID_SIZE
import java.io.File
import kotlin.math.abs
import kotlin.math.hypot
import org.junit.Assert.*
import org.junit.Test

class CircuitRaceTest {
    private val assets = File("src/main/assets/race3d")

    @Test fun everySuppliedCircuitSupportsACompleteLiveriedGridRace() {
        val routes = assets.listFiles { file -> file.extension == "route" }.orEmpty()
        assertEquals(24, routes.size)
        for (file in routes) {
            val route = CircuitRoute.decode(file.readBytes())
            assertTrue(file.name, File(assets, file.nameWithoutExtension + ".mesh").isFile)
            assertEquals(file.name, RACE_GRID_SIZE, route.grid.size)
            assertTrue(file.name, route.minimumWidth >= 7f)
            val race = CircuitRace(route)
            var frame = race.frame()
            assertEquals(file.name, RACE_GRID_SIZE, frame.cars.size)
            repeat(2400) { index ->
                if (!frame.finished) {
                    race.advance(.1f, null, DrivingInput())
                    if (index % 10 == 0) {
                        frame = race.frame()
                        for (car in frame.cars) {
                            val sample = route.sample(car.distance)
                            val lateral = hypot(car.x - sample.x, car.z - sample.z)
                            assertTrue("${file.name}: car ${car.number} left modelled road",
                                lateral + 1.1f <= sample.width / 2f + .02f)
                            assertTrue(file.name, car.speed in 0f..90f && car.y.isFinite())
                        }
                    }
                }
            }
            assertTrue("${file.name} did not finish the race", frame.finished)
            assertEquals(file.name, (1..RACE_GRID_SIZE).toSet(), frame.order.map { it.number }.toSet())
            assertTrue(file.name, frame.order.zipWithNext().all { (a, b) -> a.finishTime!! <= b.finishTime!! })
        }
    }

    @Test fun manualThrottleSteeringAndBrakingChangeTheSelectedCar() {
        val route = CircuitRoute.decode(File(assets, "mc-1929.route").readBytes())
        val race = CircuitRace(route)
        val start = race.frame().cars.first()
        repeat(28) {
            race.advance(.1f, 1, DrivingInput(steering = 1f, throttle = true))
        }
        val accelerated = race.frame().cars.first()
        val center = route.sample(accelerated.distance)
        assertTrue(accelerated.speed > 10f)
        assertTrue(accelerated.distance > start.distance)
        assertTrue(hypot(accelerated.x - center.x, accelerated.z - center.z) + 1.1f <= center.width / 2)
        repeat(50) { race.advance(.1f, 1, DrivingInput(brake = true)) }
        assertTrue(race.frame().cars.first().speed < .1f)
        assertTrue(race.frame().cars[1].distance > start.distance)
    }

    @Test fun routeWrapKeepsTheStartFinishJoinContinuous() {
        val route = CircuitRoute.decode(File(assets, "jp-1962.route").readBytes())
        val before = route.sample(-.02f)
        val after = route.sample(route.length + .02f)
        assertTrue(hypot(before.x - after.x, before.z - after.z) < .2f)
        assertTrue(abs(before.y - after.y) < .2f)
    }
}
