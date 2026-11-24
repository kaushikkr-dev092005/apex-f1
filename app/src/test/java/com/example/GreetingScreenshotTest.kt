package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.F1MainApp
import com.example.ui.F1ViewModel
import com.example.data.race.DemoRaceProvider
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w412dp-h915dp-xhdpi", sdk = [35])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Before fun controlFrames() {
    org.robolectric.shadows.ShadowChoreographer.setPaused(false)
    org.robolectric.shadows.ShadowChoreographer.setFrameDelay(java.time.Duration.ofMillis(16))
  }

  @Test
  fun greeting_screenshot() {
    val vm = F1ViewModel(ApplicationProvider.getApplicationContext<Application>())
    vm.updateReducedMotion(true)
    vm.race.accept(DemoRaceProvider().frame(0))
    composeTestRule.setContent { ApexTestContent(vm) }

    composeTestRule.onRoot().captureRoboImage(filePath = "../docs/screenshots/live-dark.png")
  }

  @Test fun live_light_screenshot() {
    val vm = F1ViewModel(ApplicationProvider.getApplicationContext<Application>())
    vm.updateReducedMotion(true)
    vm.race.accept(DemoRaceProvider().frame(0))
    composeTestRule.setContent { ApexTestContent(vm, dark = false) }
    composeTestRule.onRoot().captureRoboImage(filePath = "../docs/screenshots/live-light.png")
  }

  @Test fun supporting_screens() {
    val vm = F1ViewModel(ApplicationProvider.getApplicationContext<Application>())
    vm.updateReducedMotion(true)
    vm.race.accept(DemoRaceProvider().frame(0))
    composeTestRule.setContent { ApexTestContent(vm) }
    listOf("News", "Fantasy", "Community", "Profile").forEach { tab ->
      composeTestRule.onNodeWithContentDescription("$tab tab").performClick()
      composeTestRule.mainClock.advanceTimeBy(500)
      composeTestRule.waitForIdle()
      if (tab == "News") composeTestRule.waitUntil(15000) {
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        vm.news.value.isNotEmpty() || vm.newsError.value
      }
      composeTestRule.onRoot().captureRoboImage(filePath = "../docs/screenshots/" + tab.lowercase() + "-dark.png")
    }
  }
}
