package com.shreefintech.paytouchconsumer.utill

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object PdfThumbnailRepository {

    // NOTE: module-scope never cancelled; tag-guard prevents stale UI updates. Upgrade to ViewModelScope if thumbnail load moves to a VM.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private const val MAX_CACHE = 20

    private val cache = object : LinkedHashMap<String, Bitmap>(MAX_CACHE, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, Bitmap>) = size > MAX_CACHE
    }

    fun loadThumbnail(url: String, cacheDir: File, authHeader: String? = null, onResult: (Bitmap?) -> Unit): Job =
        scope.launch {
            val cached = synchronized(cache) { cache[url] }
            if (cached != null) {
                withContext(Dispatchers.Main) { onResult(cached) }
                return@launch
            }
            val bitmap = try {
                renderFirstPage(downloadToCache(url, cacheDir, authHeader))
            } catch (e: Exception) {
                null
            } catch (e: OutOfMemoryError) {
                // No CoroutineExceptionHandler on this scope — an uncaught Error kills the process
                e.printStackTrace()
                null
            }
            if (bitmap != null) synchronized(cache) { cache[url] = bitmap }
            withContext(Dispatchers.Main) { onResult(bitmap) }
        }

    internal fun downloadToCache(url: String, cacheDir: File, authHeader: String? = null): File {
        val file = File(cacheDir, "kyc_pdf_${url.hashCode()}.pdf")
        // Validate cached file starts with PDF magic bytes — a previous failed download
        // (e.g. a 401 HTML response) could leave a non-empty but invalid file.
        if (file.exists() && file.length() > 0L && isValidPdf(file)) return file
        file.delete()
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 60_000
        if (authHeader != null) conn.setRequestProperty("Authorization", authHeader)
        try {
            conn.connect()
            conn.inputStream.use { input -> FileOutputStream(file).use { input.copyTo(it) } }
        } finally {
            conn.disconnect()
        }
        return file
    }

    private fun isValidPdf(file: File): Boolean = try {
        file.inputStream().use { stream ->
            val magic = ByteArray(4)
            stream.read(magic) == 4 &&
                magic[0] == '%'.code.toByte() && magic[1] == 'P'.code.toByte() &&
                magic[2] == 'D'.code.toByte() && magic[3] == 'F'.code.toByte()
        }
    } catch (e: Exception) { false }

    private fun renderFirstPage(file: File): Bitmap? {
        val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(fd)
        try {
            val page = renderer.openPage(0)
            try {
                // Invalid page size — no thumbnail; caller shows its placeholder.
                if (page.width <= 0 || page.height <= 0) return null
                val scale = Utility.pdfRenderScale(page.width, page.height, 400)
                val bmp = Bitmap.createBitmap(
                    (page.width * scale).toInt().coerceAtLeast(1),
                    (page.height * scale).toInt().coerceAtLeast(1),
                    Bitmap.Config.ARGB_8888
                )
                Canvas(bmp).drawColor(Color.WHITE)
                page.render(bmp, null, Matrix().apply { setScale(scale, scale) }, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                return bmp
            } finally {
                page.close()
            }
        } finally {
            renderer.close()
            fd.close()
        }
    }
}
