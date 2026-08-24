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
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
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

        binding = ActivityDocPreviewBinding.inflate(layoutInflater)
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
        binding.btnPrevPage.setOnClickListener(listener)
        binding.btnNextPage.setOnClickListener(listener)
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
        val lower = url.lowercase()
        when {
            isPdfUrl(lower) -> viewModel.loadPdf(
                url = url,
                onLoading = { showLoading(true) },
                onReady = { file -> showLoading(false); openPdfRenderer(file) },
                onError = { loadInWebView(Constant.URL_GOOGLE_DOC_VIEWER + Uri.encode(url)) }
            )
            isImageUrl(lower) -> {
                showLoading(true)
                Glide.with(this)
                    .load(url)
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            showLoading(false)
                            showError(getString(R.string.errFailedToLoadImage, e?.message ?: ""))
                            return true
                        }

                        override fun onResourceReady(
                            resource: Drawable,
                            model: Any,
                            target: Target<Drawable>?,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            showLoading(false)
                            scaleFactor = 1f
                            binding.imagePreview.scaleX = 1f
                            binding.imagePreview.scaleY = 1f
                            binding.imagePreview.visibility = View.VISIBLE
                            binding.webViewPreview.visibility = View.GONE
                            binding.layoutPdfControls.visibility = View.GONE
                            return false
                        }
                    })
                    .into(binding.imagePreview)
            }
            else -> loadInWebView(url)
        }
    }

    private fun isPdfUrl(url: String) =
        url.endsWith(".pdf") || url.contains(".pdf?") || url.contains("type=pdf")

    private fun isImageUrl(url: String): Boolean {
        val exts = listOf(".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp")
        return exts.any { url.contains(it) }
    }

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
        val listener = onClickListener()
        binding.btnPrevPage.setOnClickListener(listener)
        binding.btnNextPage.setOnClickListener(listener)
    }

    private fun renderPdfPage(pageIndex: Int) {
        val renderer = pdfRenderer ?: return
        val screenWidth = resources.displayMetrics.widthPixels
        val page = renderer.openPage(pageIndex)

        val scale = screenWidth.toFloat() / page.width
        val bitmap = Bitmap.createBitmap(
            (page.width * scale).toInt(),
            (page.height * scale).toInt(),
            Bitmap.Config.ARGB_8888
        )
        Canvas(bitmap).drawColor(Color.WHITE)
        page.render(bitmap, null, Matrix().apply { setScale(scale, scale) }, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()

        scaleFactor = 1f
        binding.imagePreview.scaleX = 1f
        binding.imagePreview.scaleY = 1f
        binding.imagePreview.setImageBitmap(bitmap)

        binding.btnPrevPage.isEnabled = currentPage > 0
        binding.btnNextPage.isEnabled = currentPage < totalPages - 1
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
        binding.webViewPreview.destroy()
        super.onDestroy()
    }
}
