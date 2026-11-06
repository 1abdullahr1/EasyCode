package com.easycode.ide

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class EditorDeviceTest {
    @Test fun namedFiddleAndClearUndoInEachEditor() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val device = UiDevice.getInstance(instrumentation)
        fun element(selector: BySelector) = checkNotNull(device.wait(Until.findObject(selector), 8_000)) {
            "Missing $selector"
        }
        fun id(name: String) = By.res(context.packageName, name)
        fun openDrawer() {
            val toolbar = element(id("toolbar")).visibleBounds
            device.click(toolbar.left + (24 * context.resources.displayMetrics.density).toInt(), toolbar.centerY())
        }
        fun capture(name: String) {
            val directory = File(context.getExternalFilesDir(null), "review").apply { mkdirs() }
            assertTrue(device.takeScreenshot(File(directory, "$name.png")))
        }
        context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        capture("dark-editor")
        openDrawer()
        element(id("btn_drawer_new_fiddle")).click()
        val create = element(By.text("Create"))
        assertFalse(create.isEnabled)
        element(By.clazz("android.widget.EditText").focused(true)).text = "Theme review"
        create.click()
        element(By.text("Theme review"))

        for ((tab, clearLabel, source) in listOf(
            Triple("nav_html", "Clear HTML", "<p>EasyCode</p>"),
            Triple("nav_css", "Clear CSS", "p { color: blue; }"),
            Triple("nav_js", "Clear JavaScript", "console.log(42);")
        )) {
            element(id(tab)).click()
            val clear = element(By.desc(clearLabel))
            val editor = element(id("et_code"))
            editor.text = source
            clear.click()
            assertEquals("", editor.text.orEmpty())
            element(id("btn_undo")).click()
            assertEquals(source, editor.text)
        }

        openDrawer()
        element(id("btn_drawer_settings")).click()
        element(id("rb_theme_light")).click()
        element(id("rb_theme_light").checked(true))
        device.pressBack()
        element(By.text("Theme review"))
        assertEquals("console.log(42);", element(id("et_code")).text)
        capture("light-editor")
    }
}
