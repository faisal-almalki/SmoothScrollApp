package com.densitech.scrollsmooth

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import androidx.lifecycle.Lifecycle

/**
 * Starts the app.
 *
 * That is the entire test, and it exists because the app shipped broken while
 * every check was green: `assembleDebug` proves the code compiles, and nothing
 * proved it runs. It crashed on launch for ten calls to `hiltViewModel()` in a
 * project that had never had Hilt's annotation processor — a failure a compiler
 * cannot see, because the annotations resolve fine and only the generated code
 * is missing at runtime.
 *
 * Anything that breaks first-launch — a missing dependency-injection setup, a
 * crash in an object initialiser, a resource that does not resolve, a
 * NetworkOnMainThread in a repository — fails here.
 */
@RunWith(AndroidJUnit4::class)
class AppLaunchTest {

    @Test
    fun appLaunchesAndReachesResumed() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            // RESUMED means onCreate, onStart and onResume all completed without
            // throwing — the composition ran and the first frame is on screen.
            assertEquals(Lifecycle.State.RESUMED, scenario.state)
        }
    }

    @Test
    fun launchesWithoutFirebaseConfigured() {
        // There is no google-services.json in this repository, so the app starts
        // with Firebase uninitialised. That is the supported state, not an edge
        // case, and it must not take the app down.
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            assertEquals(Lifecycle.State.RESUMED, scenario.state)
        }
    }

    @Test
    fun packageUnderTestIsTheApp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.densitech.scrollsmooth", context.packageName)
    }
}
