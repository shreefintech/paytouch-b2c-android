package com.shreefintech.paytouchconsumer.myaccount

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ActivityDocPreviewBinding
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class DocPreviewActivity : BaseActivity() {

    private lateinit var binding: ActivityDocPreviewBinding

    private var pdfRenderer: PdfRenderer? = null
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var currentPage = 0
    private var totalPages = 0

    private var downloadJob: Job? = null

    private var scaleFactor = 1f
    private val minScale = 0.5f
    private val maxScale = 5f
    private lateinit var scaleDetector: ScaleGestureDetector

    companion object {
        const val EXTRA_FILE_URL = "extra_file_url"
        const val EXTRA_FILE_TITLE = "extra_file_title"

        fun start(context: Context, fileUrl: String, title: String = "") {
            val intent = Intent(context, DocPreviewActivity::class.java).apply {
                putExtra(EXTRA_FILE_URL, fileUrl)
                putExtra(EXTRA_FILE_TITLE, title)
            }
            context.startActivity(intent)
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
                ToastUtil.showDelete(this, getString(R.string.error_no_url_provided))
            finish()
            return
        }

        setupToolbar()
        setupPinchToZoom()
        retryCallback = { loadFileFromUrl(fileUrl) }
        loadFileFromUrl(fileUrl)
    }

    fun onClickListener(): View.OnClickListener {
        return View.OnClickListener {
            when (it) {
                binding.toolbar.ivBack -> {
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        }
    }

    private fun setupToolbar() {
        binding.toolbar.onClickListener = onClickListener()
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
            isPdfUrl(lower)   -> downloadAndShowPdf(url)
            isImageUrl(lower) -> downloadAndShowImage(url)
            else              -> loadInWebView(url)
        }
    }

    private fun isPdfUrl(url: String) =
        url.endsWith(".pdf") || url.contains(".pdf?") || url.contains("type=pdf")

    private fun isImageUrl(url: String): Boolean {
        val exts = listOf(".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp")
        return exts.any { url.contains(it) }
    }

    // ─── IMAGE ─────────────────────────────────────────────────────────────────

    private fun downloadAndShowImage(url: String) {
        showLoading(true)

        downloadJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val bitmap = downloadBitmap(url)
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    if (bitmap != null) {
                        showImagePreview(bitmap)
                    } else {
                        showError(getString(R.string.error_failed_to_decode_image))
                    }
                }
            } catch (e: Exception) {

                withContext(Dispatchers.Main) {
                    showLoading(false)
                    showError(getString(R.string.error_failed_to_load_image, e.message))
                }
            }
        }
    }

    private fun downloadBitmap(url: String): Bitmap? {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            doInput = true
            connect()
        }
        return connection.inputStream.use { BitmapFactory.decodeStream(it) }
    }

    private fun showImagePreview(bitmap: Bitmap) {
        scaleFactor = 1f
        binding.imagePreview.scaleX = 1f
        binding.imagePreview.scaleY = 1f
        binding.imagePreview.setImageBitmap(bitmap)
        binding.imagePreview.visibility = View.VISIBLE
        binding.webViewPreview.visibility = View.GONE
        binding.layoutPdfControls.visibility = View.GONE
    }

    // ─── PDF ───────────────────────────────────────────────────────────────────

    private fun downloadAndShowPdf(url: String) {
        showLoading(true)

        downloadJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val file = downloadToCache(url, "kyc_preview_${System.currentTimeMillis()}.pdf")
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    openPdfRenderer(file)
                }
            } catch (e: Exception) {

                withContext(Dispatchers.Main) {
                    showLoading(false)
                    loadInWebView("https://docs.google.com/gviewer?embedded=true&url=$url")
                }
            }
        }
    }

    private fun openPdfRenderer(file: File) {
        try {
            fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(fileDescriptor!!)
            totalPages = pdfRenderer!!.pageCount

            if (totalPages == 0) {
                showError(getString(R.string.error_pdf_no_pages))
                return
            }

            currentPage = 0
            binding.imagePreview.visibility = View.VISIBLE
            binding.webViewPreview.visibility = View.GONE
            binding.layoutPdfControls.visibility = View.VISIBLE

            setupPdfNavigation()
            renderPdfPage(currentPage)
        } catch (e: Exception) {
            showError(getString(R.string.error_cannot_render_pdf, e.message))
        }
    }

    private fun setupPdfNavigation() {
        updatePageLabel()

        binding.btnPrevPage.setOnClickListener {
            if (currentPage > 0) {
                currentPage--
                renderPdfPage(currentPage)
                updatePageLabel()
            }
        }

        binding.btnNextPage.setOnClickListener {
            if (currentPage < totalPages - 1) {
                currentPage++
                renderPdfPage(currentPage)
                updatePageLabel()
            }
        }
    }

    private fun renderPdfPage(pageIndex: Int) {
        val renderer = pdfRenderer ?: return

        val screenWidth = resources.displayMetrics.widthPixels
        val page = renderer.openPage(pageIndex)

        val scale = screenWidth.toFloat() / page.width
        val bitmapWidth = (page.width * scale).toInt()
        val bitmapHeight = (page.height * scale).toInt()

        val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val matrix = Matrix()
        matrix.setScale(scale, scale)

        page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()

        scaleFactor = 1f
        binding.imagePreview.scaleX = 1f
        binding.imagePreview.scaleY = 1f
        binding.imagePreview.setImageBitmap(bitmap)

        binding.btnPrevPage.isEnabled = currentPage > 0
        binding.btnNextPage.isEnabled = currentPage < totalPages - 1
    }

    private fun updatePageLabel() {
        binding.tvPageIndicator.text = getString(R.string.label_page_indicator, currentPage + 1, totalPages)
    }

    // ─── WebView fallback ──────────────────────────────────────────────────────

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
                    showError(getString(R.string.error_failed_to_load, error.description))
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

    // ─── File download helper ──────────────────────────────────────────────────

    private fun downloadToCache(url: String, fileName: String): File {
        val file = File(cacheDir, fileName)
        if (file.exists() && file.length() > 0) return file

        val connection = URL(url).openConnection() as HttpURLConnection
        connection.apply {
            connectTimeout = 15_000
            readTimeout = 60_000
            connect()
        }

        connection.inputStream.use { input: InputStream ->
            FileOutputStream(file).use { output ->
                val buffer = ByteArray(8 * 1024)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                }
            }
        }
        return file
    }

    // ─── UI helpers ────────────────────────────────────────────────────────────

    private fun showLoading(show: Boolean) {
        binding.progressBarLoad.visibility = if (show) View.VISIBLE else View.GONE
        binding.layoutContent.visibility   = if (show) View.GONE   else View.VISIBLE
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }

    // ─── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onDestroy() {
        downloadJob?.cancel()
        pdfRenderer?.close()
        fileDescriptor?.close()
        binding.webViewPreview.destroy()
        super.onDestroy()
    }
}