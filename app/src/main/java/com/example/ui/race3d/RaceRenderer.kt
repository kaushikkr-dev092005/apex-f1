package com.example.ui.race3d

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import com.example.data.race3d.CircuitRaceFrame
import com.example.data.race3d.RaceCarPose
import java.util.IdentityHashMap
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.hypot
import kotlin.math.sqrt

internal val GRID_PAINT = intArrayOf(
    0xF43949, 0xF77981, 0x4364DC, 0x607EFF, 0x40CEBD, 0x7DE2D7,
    0xF38C37, 0xFFB86B, 0x26966B, 0x50C29B, 0x709CEC, 0xEC82B5,
    0xE3E4E7, 0xC8D0D7, 0x6A8DF6, 0xADB7F5, 0x237CEF, 0x51BCEF,
    0xDF585D, 0xB4B8BF, 0xD7AE6A, 0xE3C28D
)

internal enum class RaceCamera(val label: String) { Chase("Chase"), Broadcast("TV"), Overview("Track") }

internal data class RaceScene(val frame: CircuitRaceFrame, val focus: Int, val camera: RaceCamera)

/** All GL calls stay on GLSurfaceView's render thread. Circuit buffers are shared across frames. */
internal class RaceRenderer(
    private val files: AssetManager,
    private val track: RaceAssets,
    initial: RaceScene,
    private val onError: (String) -> Unit
) : GLSurfaceView.Renderer {
    @Volatile var scene: RaceScene = initial
    private var errorReported = false
    private var program = 0
    private val uniforms = mutableMapOf<String, Int>()
    private val buffers = IdentityHashMap<MeshBatch, IntArray>()
    private val textureIds = IntArray(5)
    private val liveries = IntArray(track.grid.size)
    private var carbon = 0
    private var tyres = 0
    private var detailedLivery = 0
    private var detailedCar = -1
    private val textureNames = listOf("photo-asphalt", "paint", "paint", "photo-curb-red", "photo-curb-white")
    private val model = FloatArray(16)
    private val view = FloatArray(16)
    private val projection = FloatArray(16)
    private val viewProjection = FloatArray(16)
    private var aspect = 1f
    private var cameraX = 0f
    private var cameraY = 0f
    private var cameraZ = 0f
    private var aimX = 0f
    private var aimY = 0f
    private var aimZ = 0f
    private var previousCamera: RaceCamera? = null
    private var previousFocus = -1

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        try {
            program = 0
            uniforms.clear()
            buffers.clear()
            previousCamera = null
            detailedLivery = 0
            detailedCar = -1
            program = createProgram()
            glUseProgram(program)
            glUniform1i(uniform("uTexture"), 0)
            val textures = mutableMapOf<String, Int>()
            textureNames.forEachIndexed { i, name -> textureIds[i] = textures.getOrPut(name) { texture("$name.jpg") } }
            carbon = texture("carbon.webp")
            tyres = texture("tyre.webp")
            track.grid.forEachIndexed { i, car -> liveries[i] = texture(car.texture, sample = 2) }
            (track.road + track.ground + track.car + track.distantCar).forEach { mesh ->
                if (!buffers.containsKey(mesh)) upload(mesh)
            }
            glEnable(GL_DEPTH_TEST)
            glDepthFunc(GL_LEQUAL)
            glDisable(GL_CULL_FACE)
            glClearColor(.63f, .74f, .82f, 1f)
            check(glGetError() == GL_NO_ERROR) { "Could not upload the race scene" }
        } catch (error: Exception) {
            program = 0
            if (!errorReported) {
                errorReported = true
                onError(error.message ?: "The 3D renderer is unavailable.")
            }
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        aspect = width.toFloat() / height.coerceAtLeast(1)
        glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        if (program == 0) return
        val current = scene
        val focus = current.frame.cars.firstOrNull { it.number == current.focus } ?: return
        if (current.camera != RaceCamera.Overview && detailedCar != focus.number) {
            if (detailedLivery != 0) glDeleteTextures(1, intArrayOf(detailedLivery), 0)
            detailedCar = focus.number
            detailedLivery = texture(track.grid[focus.number - 1].texture)
        }
        setCamera(current.camera, focus)
        glUseProgram(program)
        glUniformMatrix4fv(uniform("uViewProjection"), 1, false, viewProjection, 0)
        glUniform3f(uniform("uCamera"), cameraX, cameraY, cameraZ)
        glUniform2f(uniform("uFog"), if (current.camera == RaceCamera.Overview) 2400f else 150f, 4300f)

        Matrix.setIdentityM(model, 0)
        glUniformMatrix4fv(uniform("uModel"), 1, false, model, 0)
        draw(track.ground)
        track.road.forEach { draw(it) }
        for (car in current.frame.cars) {
            val distance = hypot(car.x - focus.x, car.z - focus.z)
            if ((current.camera != RaceCamera.Overview && distance > 460f) || !inView(car)) continue
            setCarTransform(car)
            glUniformMatrix4fv(uniform("uModel"), 1, false, model, 0)
            val meshes = if (current.camera == RaceCamera.Overview || distance > 37f) track.distantCar else track.car
            val livery = if (car.number == detailedCar) detailedLivery else liveries[car.number - 1]
            meshes.forEach { draw(it, livery) }
        }
    }

    private fun inView(car: RaceCarPose): Boolean {
        val matrix = viewProjection
        val w = matrix[3] * car.x + matrix[7] * car.y + matrix[11] * car.z + matrix[15]
        val x = matrix[0] * car.x + matrix[4] * car.y + matrix[8] * car.z + matrix[12]
        val y = matrix[1] * car.x + matrix[5] * car.y + matrix[9] * car.z + matrix[13]
        // Allow for the model's extent around its centre near the view edges.
        val bound = w + 8f
        return bound > 0f && kotlin.math.abs(x) < bound && kotlin.math.abs(y) < bound
    }

    private fun setCamera(camera: RaceCamera, car: RaceCarPose) {
        val route = track.route
        val midpointX = (route.maxX + route.minX) / 2
        val midpointZ = (route.maxZ + route.minZ) / 2
        val span = maxOf(route.maxX - route.minX, route.maxZ - route.minZ)
        val overviewDistance = span * 1.12f / aspect.coerceIn(.65f, 1f)
        val behind = if (camera == RaceCamera.Broadcast) 24f else 10.5f
        val side = if (camera == RaceCamera.Broadcast) 15f else 0f
        val elevation = if (camera == RaceCamera.Broadcast) 20f else 4.8f
        val lookAhead = if (camera == RaceCamera.Broadcast) 23f else 20f
        val overview = camera == RaceCamera.Overview
        val desiredX = if (overview) midpointX else car.x - car.forwardX * behind - car.forwardZ * side
        val desiredY = if (overview) route.minY + overviewDistance else car.y + elevation
        val desiredZ = if (overview) midpointZ + overviewDistance * .33f
            else car.z - car.forwardZ * behind + car.forwardX * side
        val targetX = if (overview) midpointX else car.x + car.forwardX * lookAhead
        val targetY = if (overview) route.minY else car.y + car.grade * lookAhead + .25f
        val targetZ = if (overview) midpointZ else car.z + car.forwardZ * lookAhead
        val blend = if (camera != previousCamera || previousFocus != car.number || car.speed < .1f) 1f else .38f
        previousCamera = camera
        previousFocus = car.number
        cameraX += (desiredX - cameraX) * blend
        cameraY += (desiredY - cameraY) * blend
        cameraZ += (desiredZ - cameraZ) * blend
        aimX += (targetX - aimX) * blend
        aimY += (targetY - aimY) * blend
        aimZ += (targetZ - aimZ) * blend
        Matrix.setLookAtM(view, 0, cameraX, cameraY, cameraZ, aimX, aimY, aimZ, 0f, 1f, 0f)
        Matrix.perspectiveM(projection, 0, if (overview) 57f else 62f, aspect, if (overview) 5f else 1.2f, 8500f)
        Matrix.multiplyMM(viewProjection, 0, projection, 0, view, 0)
    }

    private fun setCarTransform(car: RaceCarPose) {
        val pitch = sqrt(1f + car.grade * car.grade)
        val fx = car.forwardX / pitch
        val fy = car.grade / pitch
        val fz = car.forwardZ / pitch
        val roll = sqrt(1f + car.bank * car.bank)
        val rx = car.forwardZ / roll
        val ry = -car.bank / roll
        val rz = -car.forwardX / roll
        val ux = fy * rz - fz * ry
        val uy = fz * rx - fx * rz
        val uz = fx * ry - fy * rx
        Matrix.setIdentityM(model, 0)
        model[0] = rx; model[1] = ry; model[2] = rz
        model[4] = ux; model[5] = uy; model[6] = uz
        model[8] = fx; model[9] = fy; model[10] = fz
        model[12] = car.x; model[13] = car.y; model[14] = car.z
    }

    private fun upload(mesh: MeshBatch) {
        val ids = IntArray(2)
        glGenBuffers(2, ids, 0)
        val vertexBytes = mesh.vertexCount * VERTEX_SIZE
        glBindBuffer(GL_ARRAY_BUFFER, ids[0])
        glBufferData(GL_ARRAY_BUFFER, vertexBytes, mesh.data.duplicate().apply { position(0) }, GL_STATIC_DRAW)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ids[1])
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, mesh.indexCount * 2,
            mesh.data.duplicate().apply { position(vertexBytes) }, GL_STATIC_DRAW)
        buffers[mesh] = ids
    }

    private fun draw(mesh: MeshBatch, livery: Int = 0) {
        val storage = buffers.getValue(mesh)
        glBindBuffer(GL_ARRAY_BUFFER, storage[0])
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, storage[1])
        glVertexAttribPointer(0, 3, GL_FLOAT, false, VERTEX_SIZE, 0)
        glVertexAttribPointer(1, 2, GL_SHORT, true, VERTEX_SIZE, 12)
        glVertexAttribPointer(2, 3, GL_BYTE, true, VERTEX_SIZE, 16)
        glVertexAttribPointer(3, 3, GL_UNSIGNED_BYTE, true, VERTEX_SIZE, 20)
        glUniform2f(uniform("uUvScale"), mesh.uvScaleX, mesh.uvScaleY)
        val kind = when (mesh.material) {
            in 0..5, 7 -> 1f
            8 -> 2f
            6 -> 3f
            else -> 0f
        }
        glUniform1f(uniform("uKind"), kind)
        val texture = when (mesh.material) {
            in 0..4 -> textureIds[mesh.material]
            5 -> carbon
            6 -> livery
            7 -> tyres
            else -> 0
        }
        if (texture != 0) glBindTexture(GL_TEXTURE_2D, texture)
        val tint = when (mesh.material) {
            1 -> 0xE9EAE1
            2 -> 0x242728
            else -> 0xFFFFFF
        }
        glUniform3f(uniform("uTint"), (tint shr 16 and 255) / 255f,
            (tint shr 8 and 255) / 255f, (tint and 255) / 255f)
        val marking = mesh.material in 1..2
        if (marking) {
            glEnable(GL_POLYGON_OFFSET_FILL)
            glPolygonOffset(-1f, -1f)
        }
        glDrawElements(GL_TRIANGLES, mesh.indexCount, GL_UNSIGNED_SHORT, 0)
        if (marking) glDisable(GL_POLYGON_OFFSET_FILL)
    }

    private fun texture(name: String, sample: Int = 1): Int {
        val options = BitmapFactory.Options().apply {
            inScaled = false
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        val bitmap = requireNotNull(files.open("race3d/$name").use { BitmapFactory.decodeStream(it, null, options) })
        val ids = IntArray(1)
        glGenTextures(1, ids, 0)
        glBindTexture(GL_TEXTURE_2D, ids[0])
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR)
        try { GLUtils.texImage2D(GL_TEXTURE_2D, 0, bitmap, 0) } finally { bitmap.recycle() }
        glGenerateMipmap(GL_TEXTURE_2D)
        return ids[0]
    }

    private fun uniform(name: String) = uniforms.getOrPut(name) { glGetUniformLocation(program, name) }

    private fun compile(kind: Int, file: String): Int {
        val handle = glCreateShader(kind)
        glShaderSource(handle, files.open("race3d/$file").bufferedReader().use { it.readText() })
        glCompileShader(handle)
        val status = IntArray(1)
        glGetShaderiv(handle, GL_COMPILE_STATUS, status, 0)
        check(status[0] == GL_TRUE) { "Race shader compile failed: ${glGetShaderInfoLog(handle)}" }
        return handle
    }

    private fun createProgram(): Int {
        val vertex = compile(GL_VERTEX_SHADER, "race.vert")
        val fragment = compile(GL_FRAGMENT_SHADER, "race.frag")
        val program = glCreateProgram()
        glAttachShader(program, vertex)
        glAttachShader(program, fragment)
        listOf("aPosition", "aUv", "aNormal", "aColor").forEachIndexed { index, attr ->
            glBindAttribLocation(program, index, attr)
            glEnableVertexAttribArray(index)
        }
        glLinkProgram(program)
        glDeleteShader(vertex)
        glDeleteShader(fragment)
        val status = IntArray(1)
        glGetProgramiv(program, GL_LINK_STATUS, status, 0)
        check(status[0] == GL_TRUE) { "Race shader link failed: ${glGetProgramInfoLog(program)}" }
        return program
    }
}
