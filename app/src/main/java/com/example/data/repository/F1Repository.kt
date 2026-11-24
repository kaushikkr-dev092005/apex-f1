package com.example.data.repository

import com.example.data.database.AppDatabase
import com.example.data.database.ChannelMessageEntity
import com.example.data.database.FantasyTeamEntity
import com.example.data.database.NewsEntity
import com.example.data.mock.MockData
import com.example.data.model.Circuit
import com.example.data.model.Constructor
import com.example.data.model.Driver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first

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
        if (currentNews.none { it.id.startsWith("apex-editorial-") }) {
            newsDao.insertNews(com.example.data.mock.EditorialFixtures.articles)
        }
        // Emit from database flow
        newsDao.getAllNews().collect {
            emit(it.filter { article -> article.id.startsWith("apex-editorial-") })
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

    suspend fun saveFantasyTeam(teamName: String, driver1Id: String, driver2Id: String, constructorId: String, budgetRemaining: Double, points: Int) {
        val entity = FantasyTeamEntity(
            teamName = teamName,
            driver1Id = driver1Id,
            driver2Id = driver2Id,
            constructorId = constructorId,
            budgetRemaining = budgetRemaining,
            totalPoints = points
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
        channelMessageDao.trimHistory(channelId)
    }
}
