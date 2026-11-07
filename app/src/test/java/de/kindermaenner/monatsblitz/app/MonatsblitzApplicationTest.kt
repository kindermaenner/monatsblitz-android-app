package de.kindermaenner.monatsblitz.app

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = MonatsblitzApplication::class)
class MonatsblitzApplicationTest {

    @Test
    fun onCreate_initializesAppContainer() {
        val app = ApplicationProvider.getApplicationContext<MonatsblitzApplication>()

        assertNotNull(app)
        assertNotNull(app.appContainer)
        assertNotNull(app.appContainer.database)
        assertNotNull(app.appContainer.playerRepository)
    }
}
