package com.shreefintech.paytouchconsumer.myaccount

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.getString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class DocPreviewViewModel(application: Application) : AndroidViewModel(application) {

    fun loadImage(
        url: String,
        onLoading: () -> Unit,
        onReady: (Bitmap) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bitmap = downloadBitmap(url)
                withContext(Dispatchers.Main) {
                    if (bitmap != null) onReady(bitmap)
                    else onError(getApplication<Application>().getString(R.string.error_failed_to_decode_image))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onError(getApplication<Application>().getString(R.string.error_failed_to_load_image, e.message))
                }
            }
        }
    }

    fun loadPdf(
        url: String,
        onLoading: () -> Unit,
        onReady: (File) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = downloadToCache(url)
                withContext(Dispatchers.Main) { onReady(file) }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { onError(e.message ?: "") }
            }
        }
    }

    private fun downloadBitmap(url: String): Bitmap? {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.doInput = true
        connection.connect()
        return connection.inputStream.use { BitmapFactory.decodeStream(it) }
    }

    private fun downloadToCache(url: String): File {
        val file = File(getApplication<Application>().cacheDir, "kyc_preview_${url.hashCode()}.pdf")
        if (file.exists() && file.length() > 0) return file
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 60_000
        connection.connect()
        connection.inputStream.use { input -> FileOutputStream(file).use { input.copyTo(it) } }
        return file
    }
}
