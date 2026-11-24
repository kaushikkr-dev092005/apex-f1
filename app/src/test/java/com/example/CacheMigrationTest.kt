package com.example

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class CacheMigrationTest {
    @Test fun addingPublicDataCachePreservesExistingFantasyAndBookmarks() {
        val app=ApplicationProvider.getApplicationContext<Application>()
        val name="apex-migration-test"
        app.deleteDatabase(name)
        val file=app.getDatabasePath(name);file.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file,null).use {db->
            db.execSQL("CREATE TABLE fantasy_team (id INTEGER NOT NULL, teamName TEXT NOT NULL, driver1Id TEXT NOT NULL, driver2Id TEXT NOT NULL, constructorId TEXT NOT NULL, budgetRemaining REAL NOT NULL, totalPoints INTEGER NOT NULL, PRIMARY KEY(id))")
            db.execSQL("CREATE TABLE channel_messages (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, channelId TEXT NOT NULL, username TEXT NOT NULL, message TEXT NOT NULL, timestamp INTEGER NOT NULL, avatarColorHex TEXT NOT NULL)")
            db.execSQL("CREATE TABLE news_articles (id TEXT NOT NULL, title TEXT NOT NULL, summary TEXT NOT NULL, content TEXT NOT NULL, category TEXT NOT NULL, imageUrl TEXT NOT NULL, publishedAt TEXT NOT NULL, isBookmarked INTEGER NOT NULL, likesCount INTEGER NOT NULL, PRIMARY KEY(id))")
            db.execSQL("INSERT INTO fantasy_team VALUES(1,'My team','leclerc','piastri','ferrari',7.0,120)")
            db.execSQL("INSERT INTO news_articles VALUES('saved','Title','Summary','Body','TECH','','2024-01-01',1,0)")
            db.version=1
        }
        val room=Room.databaseBuilder(app,AppDatabase::class.java,name).addMigrations(AppDatabase.MIGRATION_1_2).allowMainThreadQueries().build()
        try {
            val db=room.openHelper.writableDatabase
            db.query("SELECT teamName FROM fantasy_team WHERE id=1").use {assertEquals(true,it.moveToFirst());assertEquals("My team",it.getString(0))}
            db.query("SELECT isBookmarked FROM news_articles WHERE id='saved'").use {assertEquals(true,it.moveToFirst());assertEquals(1,it.getInt(0))}
            db.query("SELECT count(*) FROM data_cache").use {it.moveToFirst();assertEquals(0,it.getInt(0))}
        } finally {room.close();app.deleteDatabase(name)}
    }
}
