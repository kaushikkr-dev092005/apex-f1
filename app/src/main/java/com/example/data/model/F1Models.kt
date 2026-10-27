package com.example.data.model

import androidx.compose.ui.graphics.Color

data class Driver(
    val id: String,
    val name: String,
    val number: Int,
    val code: String,
    val team: String,
    val teamColor: String,
    val points: Int,
    val position: Int,
    val wins: Int,
    val podiums: Int,
    val championships: Int,
    val country: String,
    val age: Int,
    val cost: Double, // in millions, e.g. 28.5 = $28.5m
    val bio: String
) {
    val color: Color
        get() = try {
            Color(android.graphics.Color.parseColor(teamColor))
        } catch (e: Exception) {
            Color(0xFFE10600)
        }
}

data class Constructor(
    val id: String,
    val name: String,
    val points: Int,
    val position: Int,
    val colorHex: String,
    val cost: Double, // in millions
    val baseCountry: String
) {
    val color: Color
        get() = try {
            Color(android.graphics.Color.parseColor(colorHex))
        } catch (e: Exception) {
            Color(0xFFE10600)
        }
}

data class DrsZone(
    val id: String,
    val name: String,
    val startNodeIndex: Int,
    val endNodeIndex: Int,
    val detectionPoint: String,
    val description: String
)

data class Circuit(
    val id: String,
    val name: String,
    val country: String,
    val length: String,
    val laps: Int,
    val lapRecord: String,
    val turns: Int,
    val drsZones: Int,
    val description: String,
    // Track coordinates for custom Canvas track drawing!
    // A list of normalized float points (x, y) representing the track layout.
    val trackPoints: List<Pair<Float, Float>>,
    val drsZonesList: List<DrsZone> = emptyList()
)
