package com.example

import android.content.Intent
import androidx.lifecycle.Lifecycle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.TimeUnit

/** Exercises the real launcher and ViewModel factory, rather than a test-only activity. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LaunchActivityTest {
    @Test fun launcherStartsAndResumesAfterBackgrounding() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup().visible()
        try {
            ShadowLooper.idleMainLooper(250, TimeUnit.MILLISECONDS)
            assertEquals(Lifecycle.State.RESUMED, controller.get().lifecycle.currentState)
            assertFalse(controller.get().isFinishing)
            controller.pause().stop()
            controller.restart().start().resume().visible()
            ShadowLooper.idleMainLooper(250, TimeUnit.MILLISECONDS)
            assertEquals(Lifecycle.State.RESUMED, controller.get().lifecycle.currentState)
        } finally { controller.pause().stop().destroy() }
    }

    @Test fun coldStartNotificationLinkUsesRealLauncher() {
        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("apex://race/event/test-launch"))
        val controller = Robolectric.buildActivity(MainActivity::class.java, intent).setup().visible()
        try {
            ShadowLooper.idleMainLooper(250, TimeUnit.MILLISECONDS)
            assertEquals(Lifecycle.State.RESUMED, controller.get().lifecycle.currentState)
            assertFalse(controller.get().isFinishing)
        } finally { controller.pause().stop().destroy() }
    }
}
