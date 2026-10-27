package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FantasyTeamDao {
    @Query("SELECT * FROM fantasy_team WHERE id = 1")
    fun getFantasyTeam(): Flow<FantasyTeamEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFantasyTeam(team: FantasyTeamEntity)

    @Query("DELETE FROM fantasy_team WHERE id = 1")
    suspend fun clearFantasyTeam()
}

@Dao
interface ChannelMessageDao {
    @Query("SELECT * FROM channel_messages WHERE channelId = :channelId ORDER BY timestamp ASC")
    fun getMessagesForChannel(channelId: String): Flow<List<ChannelMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChannelMessageEntity)

    @Query("DELETE FROM channel_messages WHERE channelId = :channelId")
    suspend fun clearAllMessagesInChannel(channelId: String)
}

@Dao
interface NewsDao {
    @Query("SELECT * FROM news_articles ORDER BY publishedAt DESC")
    fun getAllNews(): Flow<List<NewsEntity>>

    @Query("SELECT * FROM news_articles WHERE id = :id")
    suspend fun getNewsById(id: String): NewsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNews(articles: List<NewsEntity>)

    @Query("UPDATE news_articles SET isBookmarked = :isBookmarked WHERE id = :id")
    suspend fun updateNewsBookmark(id: String, isBookmarked: Boolean)

    @Query("UPDATE news_articles SET likesCount = likesCount + 1 WHERE id = :id")
    suspend fun incrementLikes(id: String)
}
