package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.explore.*
import com.example.data.race.*
import com.example.ui.explore.MeshFactory
import androidx.compose.ui.graphics.Color
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class ExploreLogicTest {
    @Test fun fullCalendarContains24DistinctGeographicCircuitsAnd10Sprints() {
        val app=ApplicationProvider.getApplicationContext<Application>()
        val circuits=app.assets.open("circuits-2027.json").bufferedReader().use {CircuitCatalog.parse(it.readText())}
        assertEquals(24,circuits.size)
        assertEquals(10,circuits.count {it.sprint})
        assertEquals(22,circuits.map {it.nation}.distinct().size)
        assertEquals(3,circuits.count {it.nation=="United States"})
        assertTrue(circuits.any {it.id=="es-2026"})
        assertTrue(circuits.first {it.id=="tr-2005"}.homologationPending)
        assertTrue(circuits.all {it.points.distinct().size>20})
        assertFalse(circuits.any {it.country.contains('?')||it.weekend.contains('?')})
    }
    @Test fun perspectiveChangesWithRotationAndRemainsFinite() {
        val vertex=Vec3(.5f,.2f,.4f)
        val a=Mini3D.project(vertex,0f,.4f)
        val b=Mini3D.project(vertex,1f,.4f)
        assertNotEquals(a.x,b.x)
        assertTrue(listOf(a,b).all {it.x.isFinite()&&it.y.isFinite()&&it.depth.isFinite()})
    }
    @Test fun proceduralModelsHaveBoundedGeometryAndRealSeparateParts() {
        val helmet=MeshFactory.helmet(Color.Red,2026,false,false)
        val car=MeshFactory.car(Color.Red,false)
        assertTrue(helmet.size<400&&car.size<500)
        assertTrue(helmet.map {it.part}.containsAll(listOf("shell","visor","liner")))
        assertTrue(car.map {it.part}.containsAll(listOf("Front wing","Rear wing","Floor","Power unit","Tyres")))
        assertTrue((helmet+car).all {face->face.vertices.all {it.x.isFinite()&&it.y.isFinite()&&it.z.isFinite()}})
        assertNotEquals(helmet,MeshFactory.helmet(Color.Red,2026,true,true))
    }
    @Test fun strategyIsDeterministicAndExactBaselineHasZeroDelta() {
        val plan=StrategyPlan(pitLap=26)
        val a=StrategyEngine.simulate(plan);val b=StrategyEngine.simulate(plan)
        assertEquals(a,b)
        assertEquals(0.0,a.medianDeltaSeconds,.00001)
        assertEquals(0.0,a.lowDeltaSeconds,.00001)
        assertEquals(58,a.lapDeltas.size)
    }
    @Test fun changingPitLapAndSafetyCarAssumptionChangesModelOutcome() {
        val plain=StrategyEngine.simulate(StrategyPlan())
        val neutral=StrategyEngine.simulate(StrategyPlan(safetyCar=true))
        assertEquals(22*.4,plain.medianDeltaSeconds-neutral.medianDeltaSeconds,.00001)
        val later=StrategyEngine.simulate(StrategyPlan(pitLap=42))
        assertNotEquals(plain.medianDeltaSeconds,later.medianDeltaSeconds)
        assertTrue(plain.lowDeltaSeconds<=plain.medianDeltaSeconds&&plain.medianDeltaSeconds<=plain.highDeltaSeconds)
    }
    @Test fun strategyRejectsInvalidCompoundsBoundsAndNaN() {
        assertNotNull(StrategyEngine.validate(StrategyPlan(next=Compound.Medium)))
        assertNotNull(StrategyEngine.validate(StrategyPlan(pitLap=58)))
        assertNotNull(StrategyEngine.validate(StrategyPlan(rainPercent=101)))
        assertNotNull(StrategyEngine.validate(StrategyPlan(pitLossSeconds=Double.NaN)))
    }
    @Test fun replayDoesNotUseFutureOrStalePositions() {
        val clip=ReplayClip("test",false,"source","2024-01-01T00:00:00Z",10000,listOf(ReplayPoint(1000,"a",0f,0f),ReplayPoint(5000,"a",.2f,.2f)),emptyList())
        assertTrue(ReplayEngine.at(clip,0).isEmpty())
        assertEquals(1000L,ReplayEngine.at(clip,1100).single().timeMs)
        assertTrue(ReplayEngine.at(clip,3000).isEmpty())
        assertEquals(5000L,ReplayEngine.at(clip,5000).single().timeMs)
    }
    @Test fun utcParsingHandlesProviderMicrosecondsAndTimezoneOffsets() {
        val utc=UtcTime.parse("2024-05-26T13:56:37.956000+00:00")
        assertEquals("2024-05-26T13:56:37.956Z",UtcTime.format(utc))
        assertEquals(utc,UtcTime.parse("2024-05-26T15:56:37.956+02:00"))
    }
    @Test fun standingsKeepFractionalPointsAndProviderRound() {
        val raw="""{"MRData":{"StandingsTable":{"StandingsLists":[{"season":"2026","round":"8","DriverStandings":[{"Driver":{"driverId":"sample","givenName":"A","familyName":"Driver","code":"DRV"},"Constructors":[{"name":"Team"}],"position":"1","points":"12.5","wins":"0"}]}]}}}"""
        val result=JolpicaNormalizer.standings(raw)
        assertEquals(12.5,result.rows.single().points,0.0)
        assertEquals(8,result.round)
        assertEquals("max_verstappen",JolpicaNormalizer.providerId("verstappen"))
    }
    @Test fun pitWallKeepsObservationsAssumptionsAndEstimatesSeparate() {
        val frame=DemoRaceProvider().frame(0)
        val context=PitWallContext("LEC",frame.timing!!.first(),TrackStatus.Green,true,frame.eventTimeMs,1000,StrategyPlan())
        val answer=PitWallEngine.explain("Why pit now?",context)
        assertTrue(answer.evidence.any {it.kind==EvidenceKind.Observed})
        assertTrue(answer.evidence.any {it.kind==EvidenceKind.Assumed})
        assertTrue(answer.evidence.any {it.kind==EvidenceKind.Estimated})
        assertTrue(answer.source.contains("simulated"))
        val weather=PitWallEngine.explain("Will it rain?",context)
        assertTrue(weather.evidence.any {it.kind==EvidenceKind.Unavailable})
        assertFalse(weather.evidence.any {it.kind==EvidenceKind.Observed&&it.text.contains("%")})
    }
    @Test fun aiGatewayRequiresHttpsAndRejectsUnrelatedEvidence() {
        assertTrue(GroundedAiClient.validBaseUrl("https://gateway.example"))
        assertFalse(GroundedAiClient.validBaseUrl("http://gateway.example"))
        assertFalse(GroundedAiClient.validBaseUrl("https://key:secret@gateway.example"))
        assertFalse(GroundedAiClient.validBaseUrl("https://gateway.example?key=secret"))
        val raw="""{"classification":"ai_interpretation_unverified","analysis":"A possible tradeoff.","citations":["missing"],"model":"test"}"""
        assertTrue(runCatching {GroundedAiClient.parseResponse(raw,setOf("e0"))}.isFailure)
        assertEquals(listOf("missing"),GroundedAiClient.parseResponse(raw,setOf("missing")).citations)
    }
    @Test fun gatewayTokenIsNotPersistedOrPackaged() {
        val app=ApplicationProvider.getApplicationContext<Application>()
        val vm=com.example.ui.F1ViewModel(app)
        vm.configureAi("https://gateway.example","memory-only-test-token")
        assertEquals("memory-only-test-token",vm.aiToken)
        val second=com.example.ui.F1ViewModel(app)
        assertEquals("",second.aiToken)
        assertEquals("https://gateway.example",second.aiEndpoint)
        assertFalse(app.getSharedPreferences("apex_preferences",0).all.values.any {it=="memory-only-test-token"})
    }
}
