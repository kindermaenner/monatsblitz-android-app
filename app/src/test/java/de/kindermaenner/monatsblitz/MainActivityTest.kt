package de.kindermaenner.monatsblitz

import androidx.test.core.app.ApplicationProvider
import com.google.mlkit.common.sdkinternal.MlKitContext
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MainActivityTest {

    @Test
    fun activity_launchesAndPassesLifecycle() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        try {
            MlKitContext.initializeIfNeeded(context)
        } catch (_: Exception) {}

        val controller = Robolectric.buildActivity(MainActivity::class.java)
        controller.create().start().resume()
        val activity = controller.get()
        assertNotNull(activity)
        controller.pause().stop().destroy()
    }
}

