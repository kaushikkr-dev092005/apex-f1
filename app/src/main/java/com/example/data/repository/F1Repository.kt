package com.example.data.repository

import com.example.data.database.AppDatabase
import com.example.data.database.ChannelMessageEntity
import com.example.data.database.FantasyTeamEntity
import com.example.data.database.NewsEntity
import com.example.data.mock.MockData
import com.example.data.model.Circuit
import com.example.data.model.Constructor
import com.example.data.model.Driver
import com.example.data.model.TelemetryData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlin.math.sin
import kotlin.random.Random

class F1Repository(private val database: AppDatabase) {

    private val newsDao = database.newsDao()
    private val fantasyTeamDao = database.fantasyTeamDao()
    private val channelMessageDao = database.channelMessageDao()

    // Standings & Lists
    fun getDrivers(): List<Driver> = MockData.drivers.sortedBy { it.position }
    
    fun getConstructors(): List<Constructor> = MockData.constructors.sortedBy { it.position }
    
    fun getCircuits(): List<Circuit> = MockData.circuits

    // News Operations
    fun getNews(): Flow<List<NewsEntity>> = flow {
        // Seed database if empty
        val currentNews = newsDao.getAllNews().first()
        if (currentNews.isEmpty()) {
            newsDao.insertNews(MockData.initialNews)
        }
        // Emit from database flow
        newsDao.getAllNews().collect {
            emit(it)
        }
    }

    suspend fun bookmarkNews(id: String, isBookmarked: Boolean) {
        newsDao.updateNewsBookmark(id, isBookmarked)
    }

    suspend fun likeNews(id: String) {
        newsDao.incrementLikes(id)
    }

    // Fantasy Team Operations
    fun getFantasyTeam(): Flow<FantasyTeamEntity?> = fantasyTeamDao.getFantasyTeam()

    suspend fun saveFantasyTeam(teamName: String, driver1Id: String, driver2Id: String, constructorId: String, budgetRemaining: Double) {
        val entity = FantasyTeamEntity(
            teamName = teamName,
            driver1Id = driver1Id,
            driver2Id = driver2Id,
            constructorId = constructorId,
            budgetRemaining = budgetRemaining,
            totalPoints = Random.nextInt(180, 240) // Give the team some initial points
        )
        fantasyTeamDao.insertFantasyTeam(entity)
    }

    suspend fun clearFantasyTeam() {
        fantasyTeamDao.clearFantasyTeam()
    }

    // Chat Channel Operations
    fun getChannelMessages(channelId: String): Flow<List<ChannelMessageEntity>> {
        return channelMessageDao.getMessagesForChannel(channelId)
    }

    suspend fun sendChannelMessage(channelId: String, username: String, messageText: String, colorHex: String) {
        val entity = ChannelMessageEntity(
            channelId = channelId,
            username = username,
            message = messageText,
            timestamp = System.currentTimeMillis(),
            avatarColorHex = colorHex
        )
        channelMessageDao.insertMessage(entity)
    }

    // Real-Time Telemetry Generator (Low Latency Stream)
    fun getTelemetryStream(driverId: String): Flow<TelemetryData> = flow {
        var progress = 0.0f
        var currentLap = 24
        var tyreWear = 12.4f
        var lastSector1Time = "28.412"
        var lastSector2Time = "32.109"
        var lastSector3Time = "19.980"

        val selectedDriver = MockData.drivers.firstOrNull { it.id == driverId } ?: MockData.drivers.first()
        val isVerstappenOrHamilton = selectedDriver.code == "VER" || selectedDriver.code == "HAM"

        while (true) {
            // Accelerate progress
            progress += 0.006f
            if (progress >= 1.0f) {
                progress = 0.0f
                currentLap++
                tyreWear += 1.2f
                if (tyreWear > 90f) tyreWear = 10.0f // pit stop!
                
                // Refresh times slightly
                lastSector1Time = "28.${Random.nextInt(310, 499)}"
                lastSector2Time = "32.${Random.nextInt(0, 199)}"
                lastSector3Time = "19.${Random.nextInt(780, 999)}"
            }

            // Simulate values based on track progress
            val speed: Int
            val gear: Int
            val rpm: Int
            val throttle: Float
            val brake: Float
            val drs: Boolean
            val gForceX: Float
            val gForceY: Float
            val sector: Int

            when {
                // Main Straight (DRS Zone)
                progress in 0.85f..1.0f || progress in 0.0f..0.15f -> {
                    sector = 1
                    throttle = 1.0f
                    brake = 0.0f
                    drs = progress in 0.90f..1.0f
                    
                    // Acceleration calculation
                    val speedFactor = if (progress >= 0.85f) (progress - 0.85f) / 0.15f else (progress + 0.15f) / 0.30f
                    speed = (260 + (speedFactor * 75)).toInt().coerceAtMost(335)
                    
                    gear = when {
                        speed < 280 -> 7
                        else -> 8
                    }
                    rpm = 10000 + ((speed % 50) * 60)
                    gForceX = sin(progress * 10) * 0.2f
                    gForceY = 1.2f // Accelerating
                }

                // Heavy Braking Zone for Turn 1
                progress in 0.15f..0.22f -> {
                    sector = 1
                    throttle = 0.0f
                    brake = 0.9f
                    drs = false
                    
                    val brakingFactor = (progress - 0.15f) / 0.07f
                    speed = (325 - (brakingFactor * 210)).toInt().coerceAtLeast(115)
                    
                    gear = when {
                        speed > 220 -> 5
                        speed > 160 -> 4
                        else -> 3
                    }
                    rpm = 8500 + ((speed % 40) * 50)
                    gForceX = 0.5f
                    gForceY = -4.5f // Intense deceleration Gs
                }

                // Complex Corners (Sector 2)
                progress in 0.22f..0.55f -> {
                    sector = 2
                    drs = false
                    
                    // Curvy section: fluctuate speed & feather pedals
                    val wave = sin(progress * 40f)
                    throttle = (0.3f + (wave * 0.3f)).coerceIn(0.1f, 0.7f)
                    brake = (0.1f - (wave * 0.1f)).coerceIn(0.0f, 0.4f)
                    
                    speed = (130 + (wave * 35)).toInt()
                    gear = if (speed > 150) 4 else 3
                    rpm = 7200 + (speed * 15)
                    gForceX = wave * 3.8f // High lateral cornering Gs
                    gForceY = wave * 0.8f
                }

                // Sector 2 Straight
                progress in 0.55f..0.70f -> {
                    sector = 2
                    throttle = 1.0f
                    brake = 0.0f
                    drs = false
                    
                    val speedFactor = (progress - 0.55f) / 0.15f
                    speed = (160 + (speedFactor * 130)).toInt()
                    gear = if (speed > 240) 7 else 6
                    rpm = 11000 + (speed * 5)
                    gForceX = sin(progress * 50) * 0.4f
                    gForceY = 0.8f
                }

                // Sector 3 Chicane & Final Corners
                else -> {
                    sector = 3
                    drs = false
                    
                    val exitFactor = (progress - 0.70f) / 0.15f
                    throttle = (0.5f + (sin(progress * 15) * 0.4f)).coerceIn(0.0f, 1.0f)
                    brake = if (throttle < 0.2f) 0.6f else 0.0f
                    
                    speed = (120 + (exitFactor * 110)).toInt()
                    gear = if (speed > 190) 5 else 4
                    rpm = 8000 + (speed * 10)
                    gForceX = sin(progress * 30) * 2.9f
                    gForceY = -0.5f
                }
            }

            val telemetry = TelemetryData(
                driverId = driverId,
                speed = speed + if (isVerstappenOrHamilton) Random.nextInt(2, 5) else 0,
                gear = gear,
                rpm = rpm.coerceAtMost(13500),
                throttle = throttle,
                brake = brake,
                drs = drs,
                gForceX = gForceX,
                gForceY = gForceY,
                currentLap = currentLap,
                lapProgress = progress,
                sector = sector,
                tyreCompound = if (isVerstappenOrHamilton) "Soft" else "Medium",
                tyreWear = tyreWear.coerceAtMost(100f),
                position = selectedDriver.position,
                sector1Time = lastSector1Time,
                sector2Time = lastSector2Time,
                sector3Time = lastSector3Time
            )

            emit(telemetry)
            delay(150) // High frequency, 150ms interval for extremely fluid, real-time feel!
        }
    }
}
