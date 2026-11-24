package com.example

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.data.race.DemoRaceProvider
import com.example.ui.F1MainApp
import com.example.ui.F1ViewModel
import com.example.ui.theme.MyApplicationTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w412dp-h915dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ApexJourneyTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var vm: F1ViewModel
    @Before fun launch() {
        org.robolectric.shadows.ShadowChoreographer.setPaused(false)
        org.robolectric.shadows.ShadowChoreographer.setFrameDelay(java.time.Duration.ofMillis(16))
        val app = ApplicationProvider.getApplicationContext<Application>()
        app.getSharedPreferences("apex_preferences", 0).edit().clear().commit()
        vm = F1ViewModel(app)
        vm.updateReducedMotion(true)
        vm.race.accept(DemoRaceProvider().frame(0))
        compose.setContent { ApexTestContent(vm) }
    }
    @Test fun liveSessionOpensDriverTelemetryAndProfile() {
        compose.onNodeWithText("Monaco Grand Prix").assertIsDisplayed()
        compose.onNodeWithText("LECLERC").performClick()
        compose.onNodeWithText("DRIVER TELEMETRY").assertIsDisplayed()
        compose.onNodeWithText("Profile →").performClick()
        compose.onNodeWithText("DRIVER PROFILE").assertIsDisplayed()
        compose.onNodeWithText("Following ✓").assertExists()
    }
    @Test fun navigationConnectsAllFiveDestinations() {
        listOf("News" to "The paddock journal.", "Fantasy" to "Back your instinct.", "Community" to "Find your paddock.", "Profile" to "Make the race yours.", "Live" to "Monaco Grand Prix").forEach { (tab, title) ->
            compose.onNodeWithContentDescription("$tab tab").performClick()
            compose.onNodeWithText(title).assertIsDisplayed()
        }
    }
    @Test fun bookmarkSurvivesNewsFilterChange() {
        compose.onNodeWithContentDescription("News tab").performClick()
        compose.waitUntil(10000) { org.robolectric.shadows.ShadowLooper.idleMainLooper(); vm.news.value.isNotEmpty() || vm.newsError.value }
        org.junit.Assert.assertFalse("Journal load: " + org.robolectric.shadows.ShadowLog.getLogsForTag("APEX").joinToString { it.throwable?.stackTraceToString().orEmpty() }, vm.newsError.value)
        compose.onAllNodesWithContentDescription("Save article").onFirst().performClick()
        compose.waitUntil(10000) { org.robolectric.shadows.ShadowLooper.idleMainLooper(); vm.news.value.any { it.isBookmarked } }
        compose.onNodeWithText("Saved").performClick()
        compose.onNodeWithContentDescription("Remove saved article").assertExists()
    }
    @Test fun buildsFantasyRosterAndExplainsItsScore() {
        compose.onNodeWithContentDescription("Fantasy tab").performClick()
        clickInList("Charles Leclerc")
        clickInList("Oscar Piastri")
        clickInList("Scuderia Ferrari")
        clickInList("Save exhibition lineup")
        compose.waitUntil(10000) { org.robolectric.shadows.ShadowLooper.idleMainLooper(); vm.fantasyTeam.value != null }
        clickInList("How I scored")
        compose.onNodeWithText("Every point, explained").performScrollTo().assertIsDisplayed()
    }
    private fun clickInList(text: String) {
        compose.onNodeWithTag("fantasy-feed").performScrollToNode(hasText(text))
        compose.onNodeWithText(text).performClick()
    }
    @Test fun voicePreviewNeverClaimsAnAudioConnection() {
        compose.onNodeWithContentDescription("Community tab").performClick()
        compose.onAllNodesWithText("Preview room").onFirst().performClick()
        compose.onNodeWithText("PREVIEW · NO AUDIO CONNECTION").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Leave preview").performScrollTo().performClick()
        compose.onNodeWithText("PREVIEW · NO AUDIO CONNECTION").assertDoesNotExist()
    }
    @Test fun themeAndNotificationPreferencesPersist() {
        vm.setAppearance("light")
        vm.toggleCategory("lap")
        val second = F1ViewModel(ApplicationProvider.getApplicationContext())
        org.junit.Assert.assertEquals("light", second.appearance.value)
        org.junit.Assert.assertTrue("lap" in second.alerts.value.enabled)
    }

    @Test fun redFlagDeepLinkOpensEventDetails() {
        compose.runOnIdle {
            vm.race.accept(DemoRaceProvider().frame(280))
            vm.route("apex://race/event/red-0")
        }
        compose.onNodeWithText("RACE CONTROL").assertIsDisplayed()
        compose.onNodeWithText("Demo race control: session suspended while marshals clear the circuit.").assertIsDisplayed()
    }
}
