package com.shreefintech.paytouchconsumer.loadwallet

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ActivityHdfcWebViewBinding
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
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
    private var gatewayHost: String? = null  // host of the initial HDFC payment URL

    companion object {
        private const val EXTRA_URL = "hdfc_payment_url"
        private const val EXTRA_RETURN_URL = "hdfc_return_url"
        private const val RETURN_PAGE_TEXT_JS = "(function(){return document.body ? document.body.innerText : '';})()"

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

        binding = try {
            ActivityHdfcWebViewBinding.inflate(layoutInflater)
        } catch (e: Exception) {
            // System WebView missing / disabled / mid-update — WebView constructor throws during inflate
            e.printStackTrace()
            ToastUtil.showDelete(mActivity, getString(R.string.errWebViewUnavailable), inWindow = false)
            finish()
            return
        }
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                maxOf(imeInsets.bottom, systemBars.bottom)
            )
            insets
        }
        setupWebView()
        onBack()

        if (paymentUrl.isNotEmpty()) {
            if (!Utility.isInternetAvailable(mActivity)) {
                ToastUtil.showDelete(mActivity, getString(R.string.msgNoInternet), inWindow = false)
                finish()
                return
            }
            binding.webView.loadUrl(paymentUrl)
        } else {
            finish()
        }
    }

    override fun onDestroy() {
        if (::binding.isInitialized) {
            binding.webView.stopLoading()
            binding.webView.destroy()
        }
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

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {

                val uri = request.url
                val url = uri.toString()

                // First check HDFC Return URL
                if (interceptReturnUrl(url)) {
                    return true
                }

                // Handle external schemes such as:
                // tez://
                // upi://
                // phonepe://
                // paytmmp://
                // intent://
                val scheme = uri.scheme

                if (
                    scheme != null &&
                    !scheme.equals("http", ignoreCase = true) &&
                    !scheme.equals("https", ignoreCase = true)
                ) {
                    openExternalUrl(uri)
                    return true
                }

                return false
            }

            @Suppress("DEPRECATION")
            override fun shouldOverrideUrlLoading(
                view: WebView,
                url: String
            ): Boolean {

                if (interceptReturnUrl(url)) {
                    return true
                }

                val uri = url.toUri()
                val scheme = uri.scheme

                if (
                    scheme != null &&
                    !scheme.equals("http", ignoreCase = true) &&
                    !scheme.equals("https", ignoreCase = true)
                ) {
                    openExternalUrl(uri)
                    return true
                }

                return false
            }

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
                checkReturnPage(view)
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

    private fun interceptReturnUrl(url: String): Boolean {
        if (hasReturned) return false

        if (isReturnUrl(url)) {
            hasReturned = true
            finish()
            return true
        }

        return false
    }

    // The backend's return page is plain JSON — {"success":true,"message":"Payment page closed...",
    // "data":{"client_reference":"…","order_id":"…"}} — and no return_url is sent, so detect it by content.
    // Closing hands back to LoadWalletActivity.onResume, which polls the pending top-up's status.
    private fun checkReturnPage(view: WebView) {
        if (hasReturned) return
        view.evaluateJavascript(RETURN_PAGE_TEXT_JS) { raw ->
            if (hasReturned || isFinishing || isDestroyed) return@evaluateJavascript
            // evaluateJavascript hands back a JSON-encoded string ("\"{...}\"") or "null"
            val text = (try {
                JSONTokener(raw ?: return@evaluateJavascript).nextValue() as? String
            } catch (e: JSONException) {
                e.printStackTrace()
                null
            })?.trim() ?: return@evaluateJavascript
            if (!text.startsWith("{")) return@evaluateJavascript
            val clientReference = try {
                JSONObject(text).optJSONObject("data")?.optString("client_reference")
            } catch (e: JSONException) {
                e.printStackTrace()
                null
            }
            if (!clientReference.isNullOrEmpty()) {
                hasReturned = true
                finish()
            }
        }
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

    private fun openExternalUrl(uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)

        } catch (e: Exception) {
            e.printStackTrace()
            ToastUtil.showDelete(mActivity, getString(R.string.errAppNotInstalled))
        }
    }
}