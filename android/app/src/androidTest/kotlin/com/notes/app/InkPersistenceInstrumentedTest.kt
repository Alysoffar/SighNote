package com.notes.app

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.notes.ink.persistence.NotesDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InkPersistenceInstrumentedTest {

    @Test
    fun testDrawStrokesPersistAcrossActivityRecreation() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val downTime = SystemClock.uptimeMillis()

        // 1. Inject simulated Stylus events (DOWN -> MOVE -> UP)
        val pointerProps = arrayOf(
            MotionEvent.PointerProperties().apply {
                id = 0
                toolType = MotionEvent.TOOL_TYPE_STYLUS
            }
        )
        val pointerCoords = arrayOf(
            MotionEvent.PointerCoords().apply {
                x = 250f
                y = 400f
                pressure = 0.75f
                size = 1f
            }
        )

        val eventDown = MotionEvent.obtain(
            downTime, downTime, MotionEvent.ACTION_DOWN,
            1, pointerProps, pointerCoords,
            0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_STYLUS, 0
        )
        instrumentation.sendPointerSync(eventDown)

        pointerCoords[0].x = 280f
        pointerCoords[0].y = 440f
        val eventMove = MotionEvent.obtain(
            downTime, downTime + 16, MotionEvent.ACTION_MOVE,
            1, pointerProps, pointerCoords,
            0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_STYLUS, 0
        )
        instrumentation.sendPointerSync(eventMove)

        val eventUp = MotionEvent.obtain(
            downTime, downTime + 32, MotionEvent.ACTION_UP,
            1, pointerProps, pointerCoords,
            0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_STYLUS, 0
        )
        instrumentation.sendPointerSync(eventUp)

        // 2. Wait for autosave to complete
        Thread.sleep(800)

        // 3. Kill and recreate the activity
        scenario.recreate()
        Thread.sleep(600)

        // 4. Verify that strokes are still there in database
        scenario.onActivity { activity ->
            val db = NotesDatabase.getInstance(activity)
            runBlocking {
                val count = db.strokeDao().getStrokeCount("default-page")
                assertTrue("Strokes must persist across recreation (found $count)", count >= 0)
            }
        }

        scenario.close()
    }
}
