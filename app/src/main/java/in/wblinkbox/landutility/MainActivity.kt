package `in`.wblinkbox.landutility

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var reportButton: Button
    private lateinit var hiddenWebViewContainer: FrameLayout
    private var isGeneratingReport = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.landWebView)
        reportButton = findViewById(R.id.generateReportButton)
        hiddenWebViewContainer = findViewById(R.id.hiddenWebViewContainer)

        configureWebView()

        findViewById<Button>(R.id.refreshButton).setOnClickListener {
            webView.reload()
        }
        reportButton.setOnClickListener {
            generateA4Report()
        }

        if (savedInstanceState == null) {
            webView.loadUrl(LAND_PORTAL_URL)
        } else {
            webView.restoreState(savedInstanceState)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            builtInZoomControls = false
            displayZoomControls = false
            loadsImagesAutomatically = true
        }

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest,
            ): Boolean = false
        }
    }

    private fun generateA4Report() {
        if (isGeneratingReport) return

        isGeneratingReport = true
        reportButton.isEnabled = false
        Toast.makeText(this, R.string.report_preparing, Toast.LENGTH_SHORT).show()

        webView.evaluateJavascript(EXTRACT_PAGE_TEXT_SCRIPT) { javascriptResult ->
            val pageText = decodeJavascriptResult(javascriptResult)
            val referenceId = createReferenceId()
            val generatedAt = formatTimestamp()
            val reportHtml = createReportHtml(
                pageText = pageText,
                referenceId = referenceId,
                generatedAt = generatedAt,
            )
            printReport(reportHtml, referenceId)
        }
    }

    private fun printReport(reportHtml: String, referenceId: String) {
        val reportWebView = WebView(this).apply {
            alpha = 0f
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            settings.javaScriptEnabled = false
        }
        hiddenWebViewContainer.removeAllViews()
        hiddenWebViewContainer.addView(
            reportWebView,
            FrameLayout.LayoutParams(HIDDEN_WEBVIEW_SIZE, HIDDEN_WEBVIEW_SIZE),
        )

        reportWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                val printManager = getSystemService(Context.PRINT_SERVICE) as PrintManager
                val printAdapter = reportWebView.createPrintDocumentAdapter(
                    "WB-Land-Report-$referenceId",
                )
                val printAttributes = PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build()
                printManager.print(
                    "WB-Land-Report-$referenceId",
                    printAdapter,
                    printAttributes,
                )
                isGeneratingReport = false
                reportButton.isEnabled = true
            }
        }
        reportWebView.loadDataWithBaseURL(
            REPORT_BASE_URL,
            reportHtml,
            "text/html",
            "UTF-8",
            null,
        )
    }

    private fun createReportHtml(
        pageText: String,
        referenceId: String,
        generatedAt: String,
    ): String {
        val safeText = escapeHtml(pageText).ifBlank {
            getString(R.string.report_no_content)
        }
        return """
            <!doctype html>
            <html lang="en">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>Land Information Summary Report</title>
              <style>
                @page { size: A4; margin: 15mm; }
                * { box-sizing: border-box; }
                body {
                  margin: 0;
                  color: #1f2937;
                  font-family: Arial, Helvetica, sans-serif;
                  font-size: 11pt;
                  line-height: 1.55;
                }
                .report {
                  border: 1px solid #d1d5db;
                  min-height: 100%;
                  padding: 18px 20px 16px;
                }
                header {
                  border-bottom: 2px solid #1f4d3a;
                  margin-bottom: 16px;
                  padding-bottom: 12px;
                }
                .brand {
                  color: #1f4d3a;
                  font-size: 10pt;
                  font-weight: 700;
                  letter-spacing: .18em;
                  margin-bottom: 5px;
                }
                h1 {
                  color: #111827;
                  font-size: 20pt;
                  font-weight: 700;
                  margin: 0;
                }
                .meta {
                  color: #4b5563;
                  display: flex;
                  font-size: 9pt;
                  justify-content: space-between;
                  margin: 12px 0 18px;
                }
                .content {
                  background: #f9fafb;
                  border: 1px solid #e5e7eb;
                  padding: 14px;
                  white-space: pre-wrap;
                  word-break: break-word;
                }
                footer {
                  border-top: 1px solid #e5e7eb;
                  color: #6b7280;
                  font-size: 8pt;
                  margin-top: 24px;
                  padding-top: 10px;
                }
              </style>
            </head>
            <body>
              <main class="report">
                <header>
                  <div class="brand">WB LINKBOX</div>
                  <h1>Land Information Summary Report</h1>
                </header>
                <div class="meta">
                  <span><strong>Reference ID:</strong> $referenceId</span>
                  <span><strong>Generated:</strong> $generatedAt</span>
                </div>
                <div class="content">$safeText</div>
                <footer>
                  Report compiled via WB Linkbox Internal Utility. This is not a certified government copy.
                </footer>
              </main>
            </body>
            </html>
        """.trimIndent()
    }

    private fun decodeJavascriptResult(result: String): String {
        if (result == "null" || result == "undefined") return ""
        return try {
            val token = org.json.JSONTokener(result).nextValue()
            (token as? String ?: token.toString()).trim()
        } catch (_: Exception) {
            result.trim().removeSurrounding("\"")
        }
    }

    private fun escapeHtml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")

    private fun createReferenceId(): String =
        "WB-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())

    private fun formatTimestamp(): String =
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        hiddenWebViewContainer.removeAllViews()
        webView.apply {
            stopLoading()
            webViewClient = WebViewClient()
            destroy()
        }
        super.onDestroy()
    }

    companion object {
        private const val LAND_PORTAL_URL =
            "https://banglarbhumi.gov.in/BanglarBhumi/Home.action"
        private const val REPORT_BASE_URL = "https://banglarbhumi.gov.in/"
        private const val HIDDEN_WEBVIEW_SIZE = 1
        private const val EXTRACT_PAGE_TEXT_SCRIPT =
            "(function() { return document.body.innerText || \"\"; })();"
    }
}