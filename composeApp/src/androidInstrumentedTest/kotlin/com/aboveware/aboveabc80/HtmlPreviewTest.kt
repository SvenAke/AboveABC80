package com.aboveware.aboveabc80

import android.content.Context
import android.content.Intent
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.LinearLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class HtmlPreviewTest {
    @Test
    fun rendersHtmlWithoutScriptsAndDeletesTemporaryFile() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val directory = File(context.cacheDir, "html-previews").apply { mkdirs() }
        val file = File.createTempFile("preview-test-", ".html", directory)
        file.writeText("<html><body><h1>HTML PREVIEW</h1>" +
            "<script>document.body.textContent='SCRIPT EXECUTED';</script></body></html>")
        try {
            ActivityScenario.launch<HtmlPreviewActivity>(
                Intent(context, HtmlPreviewActivity::class.java)
                    .putExtra("path", file.absolutePath).putExtra("name", "test.html")
            ).use { scenario ->
                var rendered = false
                for (attempt in 0 until 20) {
                    val latch = CountDownLatch(1)
                    val result = AtomicReference("")
                    scenario.onActivity { activity ->
                        val root = activity.findViewById<ViewGroup>(android.R.id.content)
                            .getChildAt(0) as LinearLayout
                        val browser = root.getChildAt(1) as WebView
                        assertFalse(browser.settings.javaScriptEnabled)
                        assertFalse(browser.settings.allowFileAccess)
                        assertFalse(browser.settings.allowContentAccess)
                        assertTrue(browser.settings.blockNetworkLoads)
                        if (browser.progress == 100) {
                            // Enable evaluation only after the page has loaded with scripts disabled.
                            browser.settings.javaScriptEnabled = true
                            browser.evaluateJavascript("document.body.innerText") {
                                browser.settings.javaScriptEnabled = false
                                result.set(it)
                                latch.countDown()
                            }
                        } else {
                            latch.countDown()
                        }
                    }
                    assertTrue(latch.await(5, TimeUnit.SECONDS))
                    if (result.get().contains("HTML PREVIEW")) {
                        assertFalse(result.get().contains("SCRIPT EXECUTED"))
                        rendered = true
                        break
                    }
                    Thread.sleep(100)
                }
                assertTrue(rendered, "The page must render HTML, not show source tags")
            }
            assertFalse(file.exists())
        } finally {
            file.delete()
        }
    }
}
