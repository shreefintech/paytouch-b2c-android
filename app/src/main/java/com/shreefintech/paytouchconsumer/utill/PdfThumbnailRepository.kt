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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private const val MAX_CACHE = 20

    private val cache = object : LinkedHashMap<String, Bitmap>(MAX_CACHE, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, Bitmap>) = size > MAX_CACHE
    }

    fun loadThumbnail(url: String, cacheDir: File, onResult: (Bitmap?) -> Unit): Job =
        scope.launch {
            val cached = synchronized(cache) { cache[url] }
            if (cached != null) {
                withContext(Dispatchers.Main) { onResult(cached) }
                return@launch
            }
            val bitmap = try {
                renderFirstPage(downloadToCache(url, cacheDir))
            } catch (e: Exception) {
                null
            }
            if (bitmap != null) synchronized(cache) { cache[url] = bitmap }
            withContext(Dispatchers.Main) { onResult(bitmap) }
        }

    private fun downloadToCache(url: String, cacheDir: File): File {
        val file = File(cacheDir, "kyc_thumb_${url.hashCode()}.pdf")
        if (file.exists() && file.length() > 0L) return file
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 60_000
        try {
            conn.connect()
            conn.inputStream.use { input -> FileOutputStream(file).use { input.copyTo(it) } }
        } finally {
            conn.disconnect()
        }
        return file
    }

    private fun renderFirstPage(file: File): Bitmap {
        val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(fd)
        try {
            val page = renderer.openPage(0)
            val scale = 400f / page.width
            val bmp = Bitmap.createBitmap(
                (page.width * scale).toInt(),
                (page.height * scale).toInt(),
                Bitmap.Config.ARGB_8888
            )
            Canvas(bmp).drawColor(Color.WHITE)
            page.render(bmp, null, Matrix().apply { setScale(scale, scale) }, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            return bmp
        } finally {
            renderer.close()
            fd.close()
        }
    }
}
