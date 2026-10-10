package com.aboveware.aboveabc80

import android.app.Activity
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import java.io.File

class HtmlPreviewActivity : Activity() {
    private var webView: WebView? = null
    private var previewFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val file = File(requireNotNull(intent.getStringExtra("path")))
            require(file.canonicalFile.parentFile == File(cacheDir, "html-previews").canonicalFile)
            previewFile = file
            val browser = WebView(this).apply {
                settings.javaScriptEnabled = false
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.blockNetworkLoads = true
                webViewClient = WebViewClient()
                loadDataWithBaseURL(null, file.readText(), "text/html", "UTF-8", null)
            }
            webView = browser
            setContentView(LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                addView(Button(this@HtmlPreviewActivity).apply {
                    text = "Stäng – ${intent.getStringExtra("name").orEmpty()}"
                    setOnClickListener { finish() }
                })
                addView(browser, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
                ))
            })
        } catch (e: Exception) {
            Abc80Log.wtf("Could not render HTML: ${e.message}")
            Toast.makeText(this, "Kunde inte förhandsvisa HTML-filen", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onDestroy() {
        webView?.destroy()
        if (isFinishing) previewFile?.let {
            if (it.exists() && !it.delete()) Abc80Log.wtf("Could not delete HTML preview ${it.name}")
        }
        super.onDestroy()
    }
}
