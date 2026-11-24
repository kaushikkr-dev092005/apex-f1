package com.example.ui.race3d

import android.content.res.AssetManager
import com.example.data.race3d.CircuitRoute
import com.example.data.race3d.RACE_GRID_SIZE
import org.json.JSONArray
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.ceil
import kotlin.math.min

internal const val VERTEX_SIZE = 24

internal data class MeshBatch(
    val material: Int, val uvScaleX: Float, val uvScaleY: Float,
    val vertexCount: Int, val indexCount: Int, val data: ByteBuffer
)

internal data class GridLivery(val team: String, val number: Int, val texture: String)

internal data class RaceAssets(
    val route: CircuitRoute, val road: List<MeshBatch>,
    val ground: MeshBatch, val car: List<MeshBatch>, val distantCar: List<MeshBatch>,
    val grid: List<GridLivery>
) {
    companion object {
        fun load(assets: AssetManager, id: String): RaceAssets {
            require(id.matches(Regex("[a-z]{2}-[0-9]{4}")))
            val route = assets.open("race3d/$id.route").use { CircuitRoute.decode(it.readBytes()) }
            val road = readMesh(assets, "$id.mesh")
            val slots = JSONArray(assets.open("race3d/grid.json").bufferedReader().use { it.readText() })
            require(slots.length() == RACE_GRID_SIZE)
            val grid = List(slots.length()) { index ->
                val entry = slots.getJSONObject(index)
                val texture = entry.getString("texture")
                require(texture == "livery-${index + 1}.webp")
                GridLivery(entry.getString("team"), entry.getInt("number"), texture)
            }
            require(grid.map { it.number }.distinct().size == RACE_GRID_SIZE)
            return RaceAssets(route, road, groundMesh(route),
                readMesh(assets, "car-1.mesh"), readMesh(assets, "car-far.mesh"), grid)
        }

        private fun readMesh(assets: AssetManager, name: String): List<MeshBatch> {
            val input = assets.open("race3d/$name").use { stream ->
                ByteBuffer.wrap(stream.readBytes()).order(ByteOrder.LITTLE_ENDIAN)
            }
            require(input.int == 0x4d585041) { "Invalid race mesh" }
            val count = input.int
            require(count in 1..24)
            return List(count) {
                val material = input.int
                val vertices = input.int
                val indices = input.int
                val u = input.float
                val v = input.float
                require(material in 0..9 && vertices in 1..65535 && indices in 3..300_000)
                require(indices % 3 == 0 && u.isFinite() && v.isFinite() && u > 0f && v > 0f)
                val bytes = vertices * VERTEX_SIZE + indices * 2
                require(bytes <= input.remaining())
                val buffer = ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder())
                val view = input.slice()
                view.limit(bytes)
                buffer.put(view).rewind()
                input.position(input.position() + bytes)
                MeshBatch(material, u, v, vertices, indices, buffer)
            }.also { require(!input.hasRemaining()) }
        }

        private fun groundMesh(route: CircuitRoute): MeshBatch {
            // Surface below the supplied road elevations. Use the lower deck
            // near a crossover to keep the upper and lower sections visible.
            val spacing = 28f
            val left = route.minX - 220f
            val near = route.minZ - 220f
            val columns = ceil((route.maxX + 220f - left) / spacing).toInt() + 3
            val rows = ceil((route.maxZ + 220f - near) / spacing).toInt() + 3
            val vertices = columns * rows
            val indices = (columns - 1) * (rows - 1) * 6
            require(vertices < 65536)
            val buffer = ByteBuffer.allocateDirect(vertices * VERTEX_SIZE + indices * 2)
                .order(ByteOrder.nativeOrder())
            for (row in 0 until rows) {
                for (column in 0 until columns) {
                    val edge = row == 0 || column == 0 || row == rows - 1 || column == columns - 1
                    val x = when (column) {
                        0 -> left - 6000f
                        columns - 1 -> left + (columns - 2) * spacing + 6000f
                        else -> left + (column - 1) * spacing
                    }
                    val z = when (row) {
                        0 -> near - 6000f
                        rows - 1 -> near + (rows - 2) * spacing + 6000f
                        else -> near + (row - 1) * spacing
                    }
                    var height = route.minY - 4f
                    if (!edge) {
                        var closest = Float.MAX_VALUE
                        var closestHeight = height
                        var nearbyLow = Float.MAX_VALUE
                        for (i in 0 until route.count) {
                            val dx = x - route.x(i)
                            val dz = z - route.z(i)
                            val distance = dx * dx + dz * dz
                            if (distance < closest) {
                                closest = distance
                                closestHeight = route.height(i) - 4f
                            }
                            if (distance < 56f * 56f) nearbyLow = min(nearbyLow, route.height(i) - 3f)
                        }
                        height = min(closestHeight, nearbyLow)
                    }
                    buffer.putFloat(x).putFloat(height).putFloat(z)
                    buffer.putInt(0).put(0).put(127).put(0).put(0).putInt(-1)
                }
            }
            for (row in 0 until rows - 1) {
                for (column in 0 until columns - 1) {
                    val p = row * columns + column
                    buffer.putShort(p.toShort()).putShort((p + columns).toShort()).putShort((p + 1).toShort())
                    buffer.putShort((p + 1).toShort()).putShort((p + columns).toShort())
                        .putShort((p + columns + 1).toShort())
                }
            }
            buffer.rewind()
            return MeshBatch(8, 1f, 1f, vertices, indices, buffer)
        }
    }
}
