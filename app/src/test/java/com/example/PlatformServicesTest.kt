package com.example

import android.Manifest
import android.app.Activity
import android.app.Application
import android.app.NotificationManager
import android.content.Intent
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import com.example.data.race.*
import com.example.platform.AndroidNotificationService
import com.example.platform.ShareService
import com.example.platform.MomentKind
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.Implements
import org.robolectric.annotation.Implementation
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], shadows = [CacheFileProviderShadow::class])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PlatformServicesTest {
    private val app get() = ApplicationProvider.getApplicationContext<Application>()
    private fun event() = RaceEvent("device-test", EventKind.RedFlag, "Test suspension", "Local demo event", 24, 1000)
    @Test fun deniedNotificationPermissionDoesNotPretendToDeliver() {
        shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertFalse(AndroidNotificationService(app).notify(event()))
    }
    @Test fun localNotificationHasExplicitDemoTitleAndDeepLink() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertTrue(AndroidNotificationService(app).notify(event()))
        val notifications = shadowOf(app.getSystemService(NotificationManager::class.java)).allNotifications
        assertEquals(1, notifications.size)
        assertTrue(notifications.first().extras.getString("android.title")!!.startsWith("DEMO"))
        assertEquals("apex://race/event/device-test", shadowOf(notifications.first().contentIntent).savedIntent.dataString)
    }
    @Test fun shareCardCreatesRealPngAndUsesAndroidChooser() {
        val activityController = Robolectric.buildActivity(Activity::class.java).setup()
        val activity = activityController.get()
        val shared = ShareService.moment(activity, "Charles Leclerc", "1:13.611", "Sample lap · Monaco", "apex://driver/leclerc")
        assertTrue(org.robolectric.shadows.ShadowLog.getLogsForTag("APEX").joinToString { it.throwable?.stackTraceToString().orEmpty() }, shared)
        val png = File(activity.cacheDir, "shares/apex-moment.png")
        assertTrue(png.length() > 1000)
        val header = png.inputStream().use { input -> ByteArray(8).also { input.read(it) } }
        assertArrayEquals(byteArrayOf(-119,80,78,71,13,10,26,10), header)
        val example = File("../docs/screenshots/share-card.png")
        example.parentFile?.mkdirs()
        png.copyTo(example, overwrite = true)
        val chooser = shadowOf(activity).nextStartedActivity
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        @Suppress("DEPRECATION") val send = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        assertEquals("image/png", send.type)
        assertTrue(send.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        activityController.pause().stop().destroy()
    }
    @Test fun recordedAndHypotheticalSharesPreserveTheirClassification() {
        val controller=Robolectric.buildActivity(Activity::class.java).setup()
        val activity=controller.get()
        try {
            for(kind in listOf(MomentKind.Recorded,MomentKind.Hypothetical)) {
                assertTrue(ShareService.moment(activity,"Monaco lap / scenario","LAP 23","Source classification test","apex://explore/replay",eventName=if(kind==MomentKind.Recorded)"MONACO 2024"else"STRATEGY LAB",kind=kind))
                val chooser=shadowOf(activity).nextStartedActivity
                @Suppress("DEPRECATION") val send=chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
                assertTrue(send.getStringExtra(Intent.EXTRA_TEXT)!!.startsWith(kind.label))
                val png=File(activity.cacheDir,"shares/apex-moment.png")
                png.copyTo(File("../docs/screenshots/share-${kind.name.lowercase()}.png"),overwrite=true)
            }
        } finally {controller.pause().stop().destroy()}
    }
}

/** AndroidX assumes Android's '/' paths; host Windows uses '\\'. Keep the
 * platform URI boundary deterministic while exercising the real PNG and chooser.
 * Real URI grants still need verification on an Android device. */
@Implements(value = FileProvider::class, isInAndroidSdk = false)
class CacheFileProviderShadow {
    companion object {
        @JvmStatic @Implementation
        fun getUriForFile(context: Context, authority: String, file: File): Uri {
            val root = File(context.cacheDir, "shares").canonicalFile
            require(file.canonicalPath.startsWith(root.path + File.separator))
            require(authority == context.packageName + ".files")
            return Uri.Builder().scheme("content").authority(authority)
                .appendPath("race_moments").appendPath(file.name).build()
        }
    }
}
