package de.kindermaenner.monatsblitz

import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MainActivityTest {

    @Test
    fun activity_launchesAndPassesLifecycle() {
        val controller = Robolectric.buildActivity(MainActivity::class.java)
        controller.create().start().resume()
        val activity = controller.get()
        assertNotNull(activity)
        controller.pause().stop().destroy()
    }
}
