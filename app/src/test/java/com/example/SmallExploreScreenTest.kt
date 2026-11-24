package com.example

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.ui.F1ViewModel
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="w360dp-h640dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SmallExploreScreenTest {
    @get:Rule val compose=createComposeRule()
    private fun start(dark:Boolean):F1ViewModel {
        org.robolectric.shadows.ShadowChoreographer.setPaused(false)
        org.robolectric.shadows.ShadowChoreographer.setFrameDelay(java.time.Duration.ofMillis(16))
        val vm=F1ViewModel(ApplicationProvider.getApplicationContext<Application>())
        vm.updateReducedMotion(true);vm.openExperience("circuits")
        compose.setContent {ApexTestContent(vm,dark)}
        compose.waitUntil(10000){org.robolectric.shadows.ShadowLooper.idleMainLooper();vm.circuitCatalog.size==24}
        return vm
    }
    @Test fun circuitDetailIsScrollableOnASmallPhone() {
        start(true)
        compose.onNodeWithText("Find a circuit or country").performTextInput("Japan")
        compose.onNode(hasText("Japan") and !hasSetTextAction()).performClick()
        compose.onNodeWithText("Geographic outline, simplified",substring=true).performScrollTo().assertIsDisplayed()
        compose.onRoot().captureRoboImage(filePath="../docs/screenshots/circuit-small-phone.png")
    }
    @Test fun lightThemeGalleryRemainsReadableOnASmallPhone() {
        start(false)
        compose.onNodeWithText("The world, raised.").assertIsDisplayed()
        compose.onRoot().captureRoboImage(filePath="../docs/screenshots/circuit-gallery-light.png")
    }
}
