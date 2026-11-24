package com.example.data.race3d

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sqrt

const val RACE_GRID_SIZE = 22

data class RoutePoint(
    val x: Float, val y: Float, val z: Float,
    val forwardX: Float, val forwardZ: Float, val grade: Float,
    val width: Float, val bank: Float
)

data class GridSlot(val distance: Float, val lateral: Float)

class CircuitRoute private constructor(
    private val samples: FloatArray,
    val length: Float,
    val minimumWidth: Float,
    val grid: List<GridSlot>
) {
    val count = samples.size / 6
    val minX = (0 until count).minOf { x(it) }
    val maxX = (0 until count).maxOf { x(it) }
    val minZ = (0 until count).minOf { z(it) }
    val maxZ = (0 until count).maxOf { z(it) }
    val minY = (0 until count).minOf { samples[it * 6 + 2] }
    private val speedLimit = FloatArray(count) { index ->
        val before = sample(samples[index * 6] - 13f)
        val after = sample(samples[index * 6] + 13f)
        val angle = abs(atan2(
            before.forwardX * after.forwardZ - before.forwardZ * after.forwardX,
            before.forwardX * after.forwardX + before.forwardZ * after.forwardZ
        ))
        sqrt(23f * 26f / angle.coerceAtLeast(.045f)).coerceIn(18f, 83f)
    }.also { limits ->
        // Look ahead round the whole loop to set braking speeds into corners.
        for (step in count * 2 - 1 downTo 0) {
            val i = step % count
            val next = (i + 1) % count
            val gap = (if (next == 0) length else samples[next * 6]) - samples[i * 6]
            limits[i] = min(limits[i], sqrt(limits[next] * limits[next] + 2f * 12f * gap))
        }
    }

    fun x(index: Int) = samples[index * 6 + 1]
    fun height(index: Int) = samples[index * 6 + 2]
    fun z(index: Int) = samples[index * 6 + 3]

    private fun wrap(distance: Float): Float {
        val remainder = distance % length
        return if (remainder < 0f) remainder + length else remainder
    }

    private fun segment(wrapped: Float): Int {
        var low = 0
        var high = count
        while (low + 1 < high) {
            val mid = (low + high) / 2
            if (samples[mid * 6] <= wrapped) low = mid else high = mid
        }
        return low
    }

    fun sample(distance: Float): RoutePoint {
        val wrapped = wrap(distance)
        val index = segment(wrapped)
        val start = index * 6
        val end = ((index + 1) % count) * 6
        val span = (if (end == 0) length else samples[end]) - samples[start]
        val fraction = (wrapped - samples[start]) / span
        fun mix(column: Int) = samples[start + column] +
            (samples[end + column] - samples[start + column]) * fraction
        val dx = samples[end + 1] - samples[start + 1]
        val dz = samples[end + 3] - samples[start + 3]
        val horizontal = hypot(dx, dz).coerceAtLeast(.01f)
        return RoutePoint(
            mix(1), mix(2), mix(3), dx / horizontal, dz / horizontal,
            (samples[end + 2] - samples[start + 2]) / horizontal, mix(4), mix(5)
        )
    }

    fun pace(distance: Float): Float {
        val wrapped = wrap(distance)
        val index = segment(wrapped)
        val next = (index + 1) % count
        val span = (if (next == 0) length else samples[next * 6]) - samples[index * 6]
        val ratio = (wrapped - samples[index * 6]) / span
        return speedLimit[index] + (speedLimit[next] - speedLimit[index]) * ratio
    }

    companion object {
        fun decode(bytes: ByteArray): CircuitRoute {
            val input = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            require(input.int == 0x52585041) { "Invalid circuit route" }
            val count = input.int
            val length = input.float
            val width = input.float
            require(count in 10..3000 && length in 1000f..13000f && width in 7f..50f)
            require(input.remaining() == (count * 6 + RACE_GRID_SIZE * 7) * 4)
            val samples = FloatArray(count * 6) { input.float }
            require(samples.all { it.isFinite() } && samples[0] == 0f && samples[(count - 1) * 6] < length)
            require((0 until count).all { i ->
                samples[i * 6 + 4] >= width - .01f &&
                    (i == 0 || samples[i * 6] > samples[(i - 1) * 6])
            })
            val provisional = CircuitRoute(samples, length, width, emptyList())
            val grid = List(RACE_GRID_SIZE) {
                val chainage = input.float
                val x = input.float
                input.float // Source spawn height; bank and grade come from the road profile.
                val z = input.float
                repeat(3) { input.float } // Forward is sampled from the driving line.
                require(chainage in 0f..length && x.isFinite() && z.isFinite())
                val point = provisional.sample(chainage)
                val offset = (x - point.x) * -point.forwardZ + (z - point.z) * point.forwardX
                GridSlot(if (chainage > length / 2) chainage - length else chainage, offset)
            }
            return CircuitRoute(samples, length, width, grid)
        }
    }
}

data class DrivingInput(val steering: Float = 0f, val throttle: Boolean = false, val brake: Boolean = false)

data class RaceCarPose(
    val number: Int, val x: Float, val y: Float, val z: Float,
    val forwardX: Float, val forwardZ: Float, val grade: Float, val bank: Float,
    val speed: Float, val distance: Float, val finishTime: Float?
)

data class CircuitRaceFrame(val cars: List<RaceCarPose>, val seconds: Float) {
    val order = cars.sortedWith(compareBy<RaceCarPose> { it.finishTime ?: Float.MAX_VALUE }
        .thenByDescending { it.distance })
    val finished = cars.all { it.finishTime != null }
}

/** Fixed-step, road-constrained sprint. Distance, speed and width are in metres and seconds. */
class CircuitRace(private val route: CircuitRoute) {
    private class Car(val number: Int, val spawn: GridSlot) {
        var speed = 0f
        var distance = spawn.distance
        var lateral = spawn.lateral
        var finishTime: Float? = null
    }

    private val cars = route.grid.mapIndexed { i, spawn -> Car(i + 1, spawn) }
    private val paces = FloatArray(cars.size) { index -> .929f + (index * 17 % 23) * .0038f }
    private var seconds = 0f
    private var remainder = 0f

    fun advance(elapsed: Float, controlledCar: Int?, input: DrivingInput) {
        remainder += elapsed.coerceIn(0f, .1f)
        val step = 1f / 60f
        while (remainder >= step) {
            tick(step, controlledCar, input)
            remainder -= step
        }
    }

    private fun tick(step: Float, controlledCar: Int?, input: DrivingInput) {
        if (cars.all { it.finishTime != null }) return
        seconds += step
        // Resolve the leading car first so a trailing car can respond to its movement.
        for (car in cars.sortedByDescending { it.distance }) {
            if (car.finishTime != null) continue
            val manual = car.number == controlledCar
            val point = route.sample(car.distance)
            val limit = (point.width / 2 - 1.25f).coerceAtLeast(1f)
            car.lateral = car.lateral.coerceIn(-limit, limit)
            val nearby = cars.filter { other ->
                other !== car && other.finishTime == null && abs(other.distance - car.distance) < 23f
            }
            val blocker = nearby.filter {
                it.distance > car.distance && abs(it.lateral - car.lateral) < 2.35f
            }.minByOrNull { it.distance }
            val desiredLane = if (manual) {
                car.lateral + input.steering.coerceIn(-1f, 1f) * step * 4.5f
            } else if (blocker != null) {
                if (blocker.lateral >= 0f) -limit * .82f else limit * .82f
            } else {
                if (seconds < 6f) car.spawn.lateral else car.spawn.lateral * .25f
            }
            val lateral = (car.lateral + (desiredLane - car.lateral)
                .coerceIn(-step * 4.5f, step * 4.5f)).coerceIn(-limit, limit)
            val lateralBlocked = nearby.any {
                abs(it.distance - car.distance) < 6.3f &&
                    abs(it.lateral - lateral) < 2.15f &&
                    abs(it.lateral - lateral) < abs(it.lateral - car.lateral)
            }
            if (!lateralBlocked) car.lateral = lateral

            val safePace = route.pace(car.distance)
            val target = if (manual) {
                when {
                    input.brake -> 0f
                    input.throttle -> safePace * 1.10f
                    else -> 0f
                }
            } else safePace * paces[car.number - 1]
            val nearestAhead = nearby.filter {
                it.distance > car.distance && abs(it.lateral - car.lateral) < 2.2f
            }.minByOrNull { it.distance }
            val gap = nearestAhead?.let { (it.distance - car.distance - 6.1f).coerceAtLeast(0f) }
            val follow = nearestAhead?.let { it.speed + (gap ?: 0f) * .9f } ?: Float.MAX_VALUE
            val acceleration = (12f - car.speed * .085f).coerceAtLeast(1f)
            val allowed = min(target, follow)
            val braking = if (manual && !input.throttle && !input.brake && nearestAhead == null) 3f else 21f
            car.speed += (allowed - car.speed).coerceIn(-step * braking, step * acceleration)
            val movement = if (gap == null) car.speed * step else min(car.speed * step, gap)
            car.speed = (movement / step).coerceAtLeast(0f)
            car.distance += movement
            if (car.distance >= route.length) car.finishTime = seconds
        }
    }

    fun frame(): CircuitRaceFrame = CircuitRaceFrame(cars.map { car ->
        val center = route.sample(car.distance)
        // Blend heading across source segments to avoid a jump at each sample.
        val before = route.sample(car.distance - 2.7f)
        val after = route.sample(car.distance + 2.7f)
        val dx = after.x - before.x
        val dz = after.z - before.z
        val span = hypot(dx, dz).coerceAtLeast(.01f)
        RaceCarPose(
            car.number, center.x - center.forwardZ * car.lateral,
            center.y + center.bank * car.lateral + .055f, center.z + center.forwardX * car.lateral,
            dx / span, dz / span, (after.y - before.y) / span, center.bank,
            car.speed, car.distance, car.finishTime
        )
    }, seconds)
}
