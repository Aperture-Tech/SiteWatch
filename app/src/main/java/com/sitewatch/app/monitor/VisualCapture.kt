package com.sitewatch.app.monitor

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Renders a URL in an offscreen [WebView] and returns a fixed-size bitmap of the
 * top of the page.
 *
 * Notes / constraints that drive this implementation:
 *  - WebView must be created and drawn on the **main thread**, so the whole
 *    capture runs under [Dispatchers.Main]; the calling worker runs on a
 *    background dispatcher and switches in here.
 *  - The view is never attached to a window, so we force a **software layer**
 *    ([View.LAYER_TYPE_SOFTWARE]) — otherwise `draw(Canvas)` produces a blank
 *    bitmap for hardware-accelerated content.
 *  - Page load is asynchronous; we capture a short settle delay after
 *    `onPageFinished` to let images/JS lay out, with an overall timeout.
 *  - We capture a fixed [VIEWPORT_WIDTH] x [VIEWPORT_HEIGHT] region (above the
 *    fold) so the resulting hash is comparable across checks and memory is bounded.
 */
@Singleton
class VisualCapture @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun capture(url: String): Bitmap = withContext(Dispatchers.Main) {
        try {
            withTimeout(CAPTURE_TIMEOUT_MS) { renderToBitmap(normalize(url)) }
        } catch (e: TimeoutCancellationException) {
            throw IOException("Timed out rendering page after ${CAPTURE_TIMEOUT_MS}ms")
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun renderToBitmap(url: String): Bitmap =
        suspendCancellableCoroutine { continuation ->
            val webView = WebView(context)
            val handler = Handler(Looper.getMainLooper())
            var settled = false

            fun cleanup() {
                handler.removeCallbacksAndMessages(null)
                webView.stopLoading()
                webView.destroy()
            }

            webView.layoutParams = ViewGroup.LayoutParams(VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            with(webView.settings) {
                javaScriptEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                domStorageEnabled = true
            }

            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, finishedUrl: String) {
                    if (settled || continuation.isCompleted) return
                    // Let late-arriving images / scripts paint before capturing.
                    handler.postDelayed({
                        if (settled || continuation.isCompleted) return@postDelayed
                        settled = true
                        val result = runCatching { drawToBitmap(view) }
                        cleanup()
                        result
                            .onSuccess { continuation.resume(it) }
                            .onFailure { continuation.resumeWithException(it) }
                    }, SETTLE_DELAY_MS)
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError,
                ) {
                    // Only fail on the main-frame navigation error, not subresources.
                    if (request.isForMainFrame && !continuation.isCompleted) {
                        cleanup()
                        continuation.resumeWithException(
                            IOException("WebView error loading $url: ${error.description}")
                        )
                    }
                }
            }

            continuation.invokeOnCancellation { cleanup() }
            webView.loadUrl(url)
        }

    private fun drawToBitmap(webView: WebView): Bitmap {
        webView.measure(
            View.MeasureSpec.makeMeasureSpec(VIEWPORT_WIDTH, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(VIEWPORT_HEIGHT, View.MeasureSpec.EXACTLY),
        )
        webView.layout(0, 0, webView.measuredWidth, webView.measuredHeight)

        val bitmap = Bitmap.createBitmap(VIEWPORT_WIDTH, VIEWPORT_HEIGHT, Bitmap.Config.ARGB_8888)
        webView.draw(Canvas(bitmap))
        return bitmap
    }

    private fun normalize(url: String): String =
        if (url.startsWith("http://", true) || url.startsWith("https://", true)) url else "https://$url"

    companion object {
        private const val VIEWPORT_WIDTH = 1080
        private const val VIEWPORT_HEIGHT = 1920
        private const val SETTLE_DELAY_MS = 1500L
        private const val CAPTURE_TIMEOUT_MS = 30_000L
    }
}
