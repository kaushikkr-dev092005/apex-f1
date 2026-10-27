package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fantasy_team")
data class FantasyTeamEntity(
    @PrimaryKey val id: Int = 1, // Single active user fantasy team
    val teamName: String,
    val driver1Id: String,
    val driver2Id: String,
    val constructorId: String,
    val budgetRemaining: Double,
    val totalPoints: Int
)

@Entity(tableName = "channel_messages")
data class ChannelMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val channelId: String,
    val username: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val avatarColorHex: String = "#E10600"
)

@Entity(tableName = "news_articles")
data class NewsEntity(
    @PrimaryKey val id: String,
    val title: String,
    val summary: String,
    val content: String,
    val category: String, // "RACE REPORT", "TECH UPDATE", "FANTASY", "TRANSFERS"
    val imageUrl: String,
    val publishedAt: String,
    val isBookmarked: Boolean = false,
    val likesCount: Int = 0
)
