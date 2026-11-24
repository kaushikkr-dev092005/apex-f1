package com.example

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.ui.F1ViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowChoreographer
import org.robolectric.shadows.ShadowLooper
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w412dp-h915dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RaceScreenJourneyTest {
    @get:Rule val compose = createComposeRule()

    @Test fun raceViewLoadsMonacoAndSwitchesToSuzuka() {
        ShadowChoreographer.setPaused(false)
        ShadowChoreographer.setFrameDelay(Duration.ofMillis(16))
        val vm = F1ViewModel(ApplicationProvider.getApplicationContext<Application>())
        vm.updateReducedMotion(true)
        compose.setContent { ApexTestContent(vm) }
        compose.runOnIdle { vm.openExperience("race3d") }
        waitForGrid()
        compose.onNodeWithText("Circuit de Monaco").assertIsDisplayed()
        compose.onNodeWithText("Switch track").performClick()
        compose.onNodeWithText("Search 24 tracks").performTextInput("Suzuka")
        compose.onNodeWithText("Suzuka International Racing Course").performClick()
        waitForGrid()
        compose.onNodeWithText("Suzuka International Racing Course").assertIsDisplayed()
        compose.onNodeWithText("Drive").performScrollTo().performClick()
        compose.onNodeWithText("Throttle").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Cadillac #77").performScrollTo().performClick()
        compose.onNodeWithText("Cadillac #77").assertExists()
    }

    private fun waitForGrid() {
        compose.waitUntil(15_000) {
            ShadowLooper.idleMainLooper()
            compose.onAllNodesWithText("Start race").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
