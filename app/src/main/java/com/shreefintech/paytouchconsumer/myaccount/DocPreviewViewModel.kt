package com.shreefintech.paytouchconsumer.myaccount

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.utill.PdfThumbnailRepository
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.getString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class DocPreviewViewModel(application: Application) : AndroidViewModel(application) {

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
                val file = PdfThumbnailRepository.downloadToCache(url, getApplication<Application>().cacheDir)
                withContext(Dispatchers.Main) { onReady(file) }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { onError(e.message ?: "") }
            }
        }
    }

}
