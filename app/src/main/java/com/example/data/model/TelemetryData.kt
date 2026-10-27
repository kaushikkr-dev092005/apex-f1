package com.example.data.model

data class TelemetryData(
    val driverId: String,
    val speed: Int,           // km/h (e.g. 0 to 345)
    val gear: Int,            // 1 to 8
    val rpm: Int,             // 0 to 13500
    val throttle: Float,      // 0f to 1f (percentage)
    val brake: Float,         // 0f to 1f (percentage)
    val drs: Boolean,         // DRS flap open/closed
    val gForceX: Float,       // Lateral G-forces
    val gForceY: Float,       // Longitudinal G-forces
    val currentLap: Int,
    val lapProgress: Float,   // 0f to 1f (position around track)
    val sector: Int,          // 1, 2, 3
    val tyreCompound: String, // "Soft" (Red), "Medium" (Yellow), "Hard" (White), "Wet" (Blue)
    val tyreWear: Float,      // 0f to 100f
    val position: Int,        // Live position
    val sector1Time: String,  // Live/completed sector 1 time
    val sector2Time: String,  // Live/completed sector 2 time
    val sector3Time: String   // Live/completed sector 3 time
)
