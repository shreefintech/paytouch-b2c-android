package com.shreefintech.paytouchconsumer.loadwallet

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ActivityHdfcWebViewBinding
import java.io.ByteArrayInputStream

class HdfcWebViewActivity : BaseActivity() {

    private lateinit var binding: ActivityHdfcWebViewBinding

    private val paymentUrl: String by lazy {
        intent.getStringExtra(EXTRA_URL) ?: ""
    }

    private val returnUrl: String by lazy {
        intent.getStringExtra(EXTRA_RETURN_URL) ?: ""
    }

    @Volatile
    private var hasReturned = false

    companion object {
        private const val EXTRA_URL = "hdfc_payment_url"
        private const val EXTRA_RETURN_URL = "hdfc_return_url"

        fun newIntent(
            context: Context,
            payUrl: String,
            returnUrl: String
        ): Intent =
            Intent(context, HdfcWebViewActivity::class.java).apply {
                putExtra(EXTRA_URL, payUrl)
                putExtra(EXTRA_RETURN_URL, returnUrl)
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHdfcWebViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWebView()
        onBack()

        if (paymentUrl.isNotEmpty()) {
            binding.webView.loadUrl(paymentUrl)
        } else {
            finish()
        }
    }

    override fun onDestroy() {
        binding.webView.stopLoading()
        binding.webView.destroy()
        super.onDestroy()
    }

    @Suppress("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = false
            setSupportMultipleWindows(false)
        }

        binding.webView.webViewClient = object : WebViewClient() {

            /**
             * Intercepts network/resource requests.
             *
             * Only the configured Return URL is treated as the
             * payment return URL.
             */
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {

                val url = request.url.toString()

                if (!hasReturned && isReturnUrl(url)) {
                    view.post {
                        if (!hasReturned) {
                            hasReturned = true
                            finish()
                        }
                    }

                    // Prevent WebView from loading the Return URL.
                    return WebResourceResponse(
                        "text/plain",
                        "UTF-8",
                        ByteArrayInputStream(ByteArray(0))
                    )
                }

                return null
            }

            /**
             * Handles user-initiated navigations such as
             * form submissions and link clicks.
             */
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {

                val url = request.url.toString()
                return interceptReturnUrl(url)
            }

            /**
             * Safety net for redirects that may not be caught
             * by shouldInterceptRequest().
             */
            override fun onPageStarted(
                view: WebView,
                url: String,
                favicon: Bitmap?
            ) {
                if (!interceptReturnUrl(url)) {
                    binding.pbPageLoad.visibility = View.VISIBLE
                }
            }

            override fun onPageFinished(
                view: WebView,
                url: String
            ) {
                binding.pbPageLoad.visibility = View.GONE
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                binding.pbPageLoad.visibility = View.GONE
            }
        }

        binding.webView.webChromeClient = object : WebChromeClient() {

            override fun onProgressChanged(
                view: WebView,
                newProgress: Int
            ) {
                binding.pbPageLoad.progress = newProgress
                binding.pbPageLoad.visibility =
                    if (newProgress < 100) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
            }
        }
    }

    /**
     * Strict Return URL matching.
     *
     * The URL is considered a Return URL only when:
     *
     * 1. Scheme matches
     * 2. Host matches
     * 3. Path starts with the configured Return URL path
     *
     * Example:
     *
     * Configured Return URL:
     * https://example.com/payment/return
     *
     * Matches:
     * https://example.com/payment/return
     * https://example.com/payment/return?status=success
     *
     * Does NOT match:
     * https://example.com/other
     * https://another.com/payment/return
     */
    private fun isReturnUrl(url: String): Boolean {
        if (returnUrl.isEmpty()) return false

        return try {
            val requestUri = url.toUri()
            val returnUri = returnUrl.toUri()

            requestUri.scheme.equals(
                returnUri.scheme,
                ignoreCase = true
            ) &&
                    requestUri.host.equals(
                        returnUri.host,
                        ignoreCase = true
                    ) &&
                    requestUri.path.orEmpty().startsWith(
                        returnUri.path.orEmpty()
                    )
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Handles the Return URL on the main thread.
     *
     * hasReturned prevents duplicate finish() calls when
     * multiple WebView callbacks detect the same Return URL.
     */
    private fun interceptReturnUrl(url: String): Boolean {
        if (hasReturned) return false

        if (isReturnUrl(url)) {
            hasReturned = true
            finish()
            return true
        }

        return false
    }

    private fun onBack() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {

                override fun handleOnBackPressed() {
                    if (binding.webView.canGoBack()) {
                        binding.webView.goBack()
                    } else {
                        AlertDialog.Builder(mActivity)
                            .setTitle(getString(R.string.msgLeavePaymentTitle))
                            .setMessage(getString(R.string.msgLeavePaymentBody))
                            .setPositiveButton(
                                getString(R.string.btnLeave)
                            ) { _, _ ->
                                finish()
                            }
                            .setNegativeButton(
                                getString(R.string.btnStay),
                                null
                            )
                            .show()
                    }
                }
            }
        )
    }
}