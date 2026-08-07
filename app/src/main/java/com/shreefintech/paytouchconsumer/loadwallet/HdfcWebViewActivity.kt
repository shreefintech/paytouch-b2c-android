package com.shreefintech.paytouchconsumer.loadwallet

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
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

    private val paymentUrl: String by lazy {
        intent.getStringExtra(EXTRA_URL) ?: ""
    }
    private val returnUrl: String by lazy {
        intent.getStringExtra(EXTRA_RETURN_URL) ?: ""
    }

    companion object {
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

        setupWebView()
        onBack()

        if (paymentUrl.isNotEmpty()) {
            binding.webView.loadUrl(paymentUrl)
        } else {
            finish()
        }
    }

    @Suppress("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
        }

        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                binding.pbPageLoad.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                binding.pbPageLoad.visibility = View.GONE
            }

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val url = request.url.toString()
                if (returnUrl.isNotEmpty() && url.startsWith(returnUrl)) {
                    finish()
                    return true
                }
                return false
            }
        }

        binding.webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                binding.pbPageLoad.progress = newProgress
                binding.pbPageLoad.visibility =
                    if (newProgress < 100) View.VISIBLE else View.GONE
            }
        }
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
