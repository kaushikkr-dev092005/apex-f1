package com.example

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.core.app.ApplicationProvider
import com.example.data.race.DemoRaceProvider
import com.example.ui.F1ViewModel
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="w412dp-h915dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ExploreJourneyTest {
    @get:Rule val compose=createComposeRule()
    private lateinit var vm:F1ViewModel
    @Before fun prepare() {
        org.robolectric.shadows.ShadowChoreographer.setPaused(false)
        org.robolectric.shadows.ShadowChoreographer.setFrameDelay(java.time.Duration.ofMillis(16))
        vm=F1ViewModel(ApplicationProvider.getApplicationContext<Application>())
        vm.updateReducedMotion(true)
        vm.race.accept(DemoRaceProvider().frame(0))
        compose.setContent {ApexTestContent(vm)}
    }
    @Test fun atlasOpensFromLiveAndShowsAllGeographicCircuits() {
        compose.onNodeWithText("2027 circuits").performScrollTo().performClick()
        compose.waitUntil(10000){org.robolectric.shadows.ShadowLooper.idleMainLooper();vm.circuitCatalog.size==24}
        compose.onNodeWithText("The world, raised.").assertIsDisplayed()
        compose.onNodeWithText("Find a circuit or country").performTextInput("Japan")
        compose.onNode(hasText("Japan") and !hasSetTextAction()).performClick()
        compose.onNodeWithContentDescription("Reset circuit sculpture").assertIsDisplayed()
        compose.onRoot().captureRoboImage(filePath="../docs/screenshots/circuit-detail.png")
    }
    @Test fun helmetHasWorkingVisorLayersAndAccessibleRotation() {
        compose.runOnIdle {vm.openExperience("helmets")}
        compose.onNodeWithText("The Helmet Vault.").assertIsDisplayed()
        compose.onRoot().captureRoboImage(filePath="../docs/screenshots/helmet-vault.png")
        compose.onNodeWithText("Open visor").performScrollTo().performClick()
        compose.onNodeWithText("Close visor").assertExists()
        compose.onNodeWithText("Explode layers").performScrollTo().performClick()
        compose.onNodeWithText("Assemble").assertExists()
        compose.onNodeWithContentDescription("Rotate model left").performScrollTo().performClick()
    }
    @Test fun garageRendersModelAndTogglesActualParts() {
        compose.runOnIdle {vm.openExperience("garage")}
        compose.onNodeWithText("Inside the machine.").assertIsDisplayed()
        compose.onRoot().captureRoboImage(filePath="../docs/screenshots/digital-garage.png")
        compose.onNodeWithText("Explode car").performScrollTo().performClick()
        compose.onNodeWithText("Assemble car").assertExists()
        compose.onNodeWithText("Show airflow").performScrollTo().performClick()
        compose.onNodeWithText("Hide airflow").assertExists()
    }
    @Test fun simulatorUpdatesPlanAndRejectsSameDryCompound() {
        compose.runOnIdle {vm.openExperience("strategy")}
        compose.onNodeWithContentDescription("Planned pit lap").performSemanticsAction(SemanticsActions.SetProgress){it(30f)}
        org.junit.Assert.assertEquals(30,vm.strategyPlan.pitLap)
        compose.onRoot().captureRoboImage(filePath="../docs/screenshots/strategy-lab.png")
        compose.onAllNodesWithText("Hard").onFirst().performScrollTo().performClick()
        org.junit.Assert.assertNotNull(com.example.data.explore.StrategyEngine.validate(vm.strategyPlan))
    }
    @Test fun timeMachineScrubsWithoutInventingLiveData() {
        compose.runOnIdle {vm.openExperience("replay")}
        compose.onNodeWithText("SIMULATED REPLAY").assertExists()
        compose.onNodeWithContentDescription("Replay time in milliseconds").performScrollTo().performSemanticsAction(SemanticsActions.SetProgress){it(25000f)}
        compose.onNodeWithText("00:25").assertExists()
        compose.onRoot().captureRoboImage(filePath="../docs/screenshots/time-machine.png")
    }
    @Test fun pitWallExplainsEvidenceAndCanReturnToLive() {
        compose.runOnIdle {vm.openExperience("pitwall")}
        compose.onNodeWithText("Why pit now?").performScrollTo().performClick()
        compose.onAllNodesWithText("DEMO OBSERVATION").onFirst().performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Live tab").performClick()
        compose.onNodeWithText("Monaco Grand Prix").assertIsDisplayed()
        org.junit.Assert.assertNull(vm.experience.value)
    }
    @Test fun palsCanCelebrateAndFinishAToyRace() {
        compose.runOnIdle {vm.openExperience("pals")}
        compose.onRoot().captureRoboImage(filePath="../docs/screenshots/paddock-pals.png")
        compose.onNodeWithText("Celebrate").performScrollTo().performClick()
        compose.onNodeWithText("Race VER").performScrollTo().performClick()
        compose.onNodeWithText("wins the playful sprint!",substring=true).assertExists()
    }
    @Test fun exhibitionRivalryAndIntelligenceAreReachable() {
        compose.runOnIdle {vm.openExperience("rivalry")}
        compose.onNodeWithText("Separated by fractions.").assertIsDisplayed()
        compose.onRoot().captureRoboImage(filePath="../docs/screenshots/rivalry-lab.png")
        compose.runOnIdle {vm.openExperience("intelligence")}
        compose.onNodeWithText("The story behind the signal.").assertIsDisplayed()
    }
    @Test fun scenarioDeepLinkRestoresAllHypotheticalInputs() {
        compose.runOnIdle {vm.route("apex://explore/strategy?pit=31&start=Soft&next=Hard&rain=40&loss=25&wear=1.25&sc=true")}
        compose.onNodeWithText("What if you called it?").assertIsDisplayed()
        org.junit.Assert.assertEquals(31,vm.strategyPlan.pitLap)
        org.junit.Assert.assertEquals(25.0,vm.strategyPlan.pitLossSeconds,0.0)
        org.junit.Assert.assertEquals(1.25,vm.strategyPlan.degradationFactor,0.0)
        org.junit.Assert.assertTrue(vm.strategyPlan.safetyCar)
    }
    @Test fun replayDeepLinkRestoresTimeAndDataMode() {
        compose.runOnIdle {vm.route("apex://explore/replay?mode=demo&t=25000")}
        compose.onNodeWithText("SIMULATED REPLAY").assertExists()
        compose.onNodeWithText("00:25").assertExists()
    }
}
