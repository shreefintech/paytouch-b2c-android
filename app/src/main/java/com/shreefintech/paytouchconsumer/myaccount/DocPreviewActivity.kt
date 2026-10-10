package com.shreefintech.paytouchconsumer.myaccount

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.ScaleGestureDetector
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ActivityDocPreviewBinding
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility
import java.io.File

class DocPreviewActivity : BaseActivity() {

    private lateinit var binding: ActivityDocPreviewBinding
    private val viewModel: DocPreviewViewModel by viewModels()

    private var pdfRenderer: PdfRenderer? = null
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var currentPage = 0
    private var totalPages = 0

    private var scaleFactor = 1f
    private val minScale = 0.5f
    private val maxScale = 5f
    private lateinit var scaleDetector: ScaleGestureDetector

    companion object {
        private const val EXTRA_FILE_URL = "extra_file_url"
        private const val EXTRA_FILE_TITLE = "extra_file_title"

        fun start(context: Context, fileUrl: String, title: String = "") {
            context.startActivity(
                Intent(context, DocPreviewActivity::class.java).apply {
                    putExtra(EXTRA_FILE_URL, fileUrl)
                    putExtra(EXTRA_FILE_TITLE, title)
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = try {
            ActivityDocPreviewBinding.inflate(layoutInflater)
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
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val fileUrl = intent.getStringExtra(EXTRA_FILE_URL)?.trim()
        if (fileUrl.isNullOrEmpty()) {
            finish()
            return
        }

        setupToolbar()
        setupPinchToZoom()
        retryCallback = { loadFileFromUrl(fileUrl) }
        loadFileFromUrl(fileUrl)
    }

    private fun onClickListener(): View.OnClickListener {
        return View.OnClickListener {
            when (it) {
                binding.toolbar.ivBack -> {
                    if (Utility.stopClick()) return@OnClickListener
                    onBackPressedDispatcher.onBackPressed()
                }
                binding.btnPrevPage -> {
                    if (Utility.stopClick()) return@OnClickListener
                    if (currentPage > 0) {
                        currentPage--
                        renderPdfPage(currentPage)
                        updatePageLabel()
                    }
                }
                binding.btnNextPage -> {
                    if (Utility.stopClick()) return@OnClickListener
                    if (currentPage < totalPages - 1) {
                        currentPage++
                        renderPdfPage(currentPage)
                        updatePageLabel()
                    }
                }
            }
        }
    }

    private fun setupToolbar() {
        val listener = onClickListener()
        binding.toolbar.onClickListener = listener
        binding.onClickListener = listener
    }

    private fun setupPinchToZoom() {
        scaleDetector = ScaleGestureDetector(
            this,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    scaleFactor *= detector.scaleFactor
                    scaleFactor = scaleFactor.coerceIn(minScale, maxScale)
                    binding.imagePreview.scaleX = scaleFactor
                    binding.imagePreview.scaleY = scaleFactor
                    return true
                }
            })

        binding.imagePreview.setOnTouchListener { v, event ->
            scaleDetector.onTouchEvent(event)
            v.performClick()
            true
        }
    }

    private fun loadFileFromUrl(url: String) {
        if (!Utility.isInternetAvailable(mActivity)) {
            showNoInternet()
            return
        }
        hideNoInternet()
        if (isPdfUrl(url.lowercase())) {
            viewModel.loadPdf(
                url = url,
                onLoading = { showLoading(true) },
                onReady = { file -> showLoading(false); openPdfRenderer(file) },
                onError = { loadInWebView(Constant.URL_GOOGLE_DOC_VIEWER + Uri.encode(url)) }
            )
        } else {
            // CustomTarget bypasses Glide's ViewTarget dimension check, which would deadlock
            // because showLoading(true) hides layoutContent (GONE → zero dimensions on imagePreview).
            // Glide decodes by content, not URL extension — extension-less URLs work too.
            // Falls back to WebView if the URL is not an image Glide can decode.
            // Bounded target + CENTER_INSIDE: a no-arg CustomTarget decodes at original size, and a
            // 25 MP+ server image throws "Canvas: trying to draw too large bitmap". 2× width keeps zoom headroom.
            showLoading(true)
            val maxWidth = resources.displayMetrics.widthPixels * 2
            Glide.with(this)
                .asBitmap()
                .load(url)
                .downsample(DownsampleStrategy.CENTER_INSIDE)
                .into(object : CustomTarget<Bitmap>(maxWidth, Constant.PDF_MAX_BITMAP_HEIGHT_PX) {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        showLoading(false)
                        scaleFactor = 1f
                        binding.imagePreview.scaleX = 1f
                        binding.imagePreview.scaleY = 1f
                        binding.imagePreview.setImageBitmap(resource)
                        binding.imagePreview.visibility = View.VISIBLE
                        binding.webViewPreview.visibility = View.GONE
                        binding.layoutPdfControls.visibility = View.GONE
                    }
                    override fun onLoadCleared(placeholder: Drawable?) {}
                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        loadInWebView(url)
                    }
                })
        }
    }

    private fun isPdfUrl(url: String) =
        url.endsWith(".pdf") || url.contains(".pdf?") || url.contains("type=pdf")

    // ─── PDF ───────────────────────────────────────────────────────────────────

    private fun openPdfRenderer(file: File) {
        try {
            fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(fileDescriptor!!)
            totalPages = pdfRenderer!!.pageCount

            if (totalPages == 0) {
                showError(getString(R.string.errPdfNoPages))
                return
            }

            currentPage = 0
            binding.imagePreview.visibility = View.VISIBLE
            binding.webViewPreview.visibility = View.GONE
            binding.layoutPdfControls.visibility = View.VISIBLE

            setupPdfNavigation()
            renderPdfPage(currentPage)
        } catch (e: Exception) {
            showError(getString(R.string.errCannotRenderPdf, e.message))
        }
    }

    private fun setupPdfNavigation() {
        updatePageLabel()
    }

    private fun renderPdfPage(pageIndex: Int) {
        val renderer = pdfRenderer ?: return
        val screenWidth = resources.displayMetrics.widthPixels
        try {
            val page = renderer.openPage(pageIndex)
            try {
                if (page.width <= 0 || page.height <= 0) {
                    showError(getString(R.string.errCannotRenderPdf, getString(R.string.errPdfInvalidPageDimensions)))
                    return
                }
                val scale = Utility.pdfRenderScale(page.width, page.height, screenWidth)
                val bitmap = Bitmap.createBitmap(
                    (page.width * scale).toInt().coerceAtLeast(1),
                    (page.height * scale).toInt().coerceAtLeast(1),
                    Bitmap.Config.ARGB_8888
                )
                Canvas(bitmap).drawColor(Color.WHITE)
                page.render(bitmap, null, Matrix().apply { setScale(scale, scale) }, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                scaleFactor = 1f
                binding.imagePreview.scaleX = 1f
                binding.imagePreview.scaleY = 1f
                binding.imagePreview.setImageBitmap(bitmap)

                binding.btnPrevPage.isEnabled = currentPage > 0
                binding.btnNextPage.isEnabled = currentPage < totalPages - 1
            } finally {
                page.close()
            }
        } catch (e: Exception) {
            showError(getString(R.string.errCannotRenderPdf, e.message))
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
            showError(getString(R.string.errCannotRenderPdf, e.message))
        }
    }

    private fun updatePageLabel() {
        binding.tvPageIndicator.text = getString(R.string.labelPageIndicator, currentPage + 1, totalPages)
    }

    // ─── WebView fallback ──────────────────────────────────────────────────────

    @Suppress("SetJavaScriptEnabled")
    private fun loadInWebView(url: String) {
        showLoading(true)

        binding.imagePreview.visibility = View.GONE
        binding.layoutPdfControls.visibility = View.GONE
        binding.webViewPreview.visibility = View.VISIBLE

        binding.webViewPreview.apply {
            settings.apply {
                javaScriptEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                builtInZoomControls = true
                displayZoomControls = false
                setSupportZoom(true)
                cacheMode = WebSettings.LOAD_DEFAULT
            }

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    showLoading(false)
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError
                ) {
                    showLoading(false)
                    showError(getString(R.string.errFailedToLoad, error.description))
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    binding.progressBarWeb.progress = newProgress
                    binding.progressBarWeb.visibility =
                        if (newProgress < 100) View.VISIBLE else View.GONE
                }
            }

            loadUrl(url)
        }
    }

    // ─── UI helpers ────────────────────────────────────────────────────────────

    private fun showLoading(show: Boolean) {
        binding.progressBarLoad.visibility = if (show) View.VISIBLE else View.GONE
        binding.layoutContent.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showError(message: String) {
        binding.progressBarLoad.visibility = View.GONE
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }

    // ─── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onDestroy() {
        pdfRenderer?.close()
        fileDescriptor?.close()
        if (::binding.isInitialized) binding.webViewPreview.destroy()
        super.onDestroy()
    }
}
