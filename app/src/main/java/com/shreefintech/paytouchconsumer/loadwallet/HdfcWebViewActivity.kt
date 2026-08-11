package com.shreefintech.paytouchconsumer.loadwallet

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ActivityHdfcWebViewBinding

class HdfcWebViewActivity : BaseActivity() {

    private lateinit var binding: ActivityHdfcWebViewBinding

    private val paymentUrl: String by lazy { intent.getStringExtra(EXTRA_URL) ?: "" }
    private val returnUrl: String by lazy { intent.getStringExtra(EXTRA_RETURN_URL) ?: "" }

    private var hasReturned = false
    private var gatewayHost: String? = null  // host of the initial HDFC payment URL

    companion object {
        private const val TAG = "HdfcWebView"
        private const val EXTRA_URL = "hdfc_payment_url"
        private const val EXTRA_RETURN_URL = "hdfc_return_url"

        fun newIntent(context: Context, payUrl: String, returnUrl: String): Intent =
            Intent(context, HdfcWebViewActivity::class.java).apply {
                putExtra(EXTRA_URL, payUrl)
                putExtra(EXTRA_RETURN_URL, returnUrl)
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHdfcWebViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        gatewayHost = if (paymentUrl.isNotEmpty()) Uri.parse(paymentUrl).host else null

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

            // Handles user-initiated navigations (e.g. form posts, link taps)
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val url = request.url.toString()
                return interceptReturnUrl(url)
            }

            // Catches server-side redirects that bypass shouldOverrideUrlLoading
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                if (!interceptReturnUrl(url)) {
                    binding.pbPageLoad.visibility = View.VISIBLE
                }
            }

            override fun onPageFinished(view: WebView, url: String) {
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
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                binding.pbPageLoad.progress = newProgress
                binding.pbPageLoad.visibility = if (newProgress < 100) View.VISIBLE else View.GONE
            }
        }
    }

    // Strategy 1: match the returnUrl prefix from the API response (when present).
    // Strategy 2: host-change detection — all HDFC gateway pages share the same host;
    //             a navigation to any other host is the merchant callback URL.
    // hasReturned guards against duplicate finish() calls across both strategies.
    private fun interceptReturnUrl(url: String): Boolean {
        if (hasReturned) return false

        if (returnUrl.isNotEmpty()) {
            if (url.startsWith(returnUrl)) {
                hasReturned = true
                finish()
                return true
            }
        }

        val urlHost = Uri.parse(url).host
        if (gatewayHost != null && urlHost != null && urlHost != gatewayHost) {
            hasReturned = true
            finish()
            return true
        }

        return false
    }

    private fun onBack() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.webView.canGoBack()) {
                    binding.webView.goBack()
                } else {
                    AlertDialog.Builder(mActivity)
                        .setTitle(getString(R.string.msgLeavePaymentTitle))
                        .setMessage(getString(R.string.msgLeavePaymentBody))
                        .setPositiveButton(getString(R.string.btnLeave)) { _, _ -> finish() }
                        .setNegativeButton(getString(R.string.btnStay), null)
                        .show()
                }
            }
        })
    }
}
