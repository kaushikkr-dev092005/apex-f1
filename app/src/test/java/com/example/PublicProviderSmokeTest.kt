package com.example

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.explore.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** Explicit opt-in: real public historical requests, never a paid/live or AI endpoint. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class PublicProviderSmokeTest {
    @Test fun publishedStandingsSeasonResultsAndRecordedCoordinatesNormalizeAndCache():Unit = runBlocking {
        assumeTrue("Set APEX_NETWORK_TEST=1 for public provider smoke test",System.getenv("APEX_NETWORK_TEST")=="1")
        val app=ApplicationProvider.getApplicationContext<Application>()
        val db=Room.inMemoryDatabaseBuilder(app,AppDatabase::class.java).build()
        try {
            val repository=PublicF1Repository(db.dataCacheDao())
            val standings=repository.standings(force=true)
            assertEquals(2026,standings.data.season)
            assertTrue(standings.data.rows.isNotEmpty())
            assertTrue(repository.standings().cached)
            val results=repository.results("leclerc",2026)
            val qualifying=repository.qualifying("leclerc",2026)
            val clip=repository.monacoArchive()
            assertFalse(clip.demo)
            assertTrue(clip.points.size>100)
            assertTrue(clip.points.all {it.timeMs in 0..clip.durationMs&&it.x.isFinite()&&it.y.isFinite()})
            assertTrue(clip.points.any {it.speedKph!=null})
            val report=JSONObject().put("verifiedUtc",UtcTime.format(System.currentTimeMillis()))
                .put("standingsSeason",standings.data.season).put("standingsRound",standings.data.round)
                .put("driverCount",standings.data.rows.size).put("cacheReadVerified",true)
                .put("seasonRaceResultCount",results.data.size).put("qualifyingCount",qualifying.data.size)
                .put("replayTitle",clip.title).put("recordedCoordinateCount",clip.points.size)
                .put("recordedDurationMs",clip.durationMs).put("archiveSource",clip.source)
                .put("environment","Windows host / Robolectric API 35 / actual public HTTPS requests")
            File("../docs/measurements/public-provider-smoke.json").apply {parentFile?.mkdirs();writeText(report.toString(2))}
        } finally {db.close()}
    }
}
