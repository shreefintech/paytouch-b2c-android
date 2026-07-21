package com.shreefintech.paytouchconsumer.auth.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.enums.LoginMode
import com.shreefintech.paytouchconsumer.utill.Utility
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    fun login(
        mobile: String,
        credential: String,
        mode: LoginMode,
        onLoading: () -> Unit,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val error = validate(mobile, credential, mode)
        if (error != null) { onError(error); return }
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getApplication<Application>().getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        viewModelScope.launch {
            try {
                // API call wired here once the endpoint is ready
                withContext(Dispatchers.Main) { onSuccess() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.message ?: getApplication<Application>().getString(R.string.errGeneric))
                }
            }
        }
    }

    private fun validate(mobile: String, credential: String, mode: LoginMode): String? {
        val app = getApplication<Application>()
        if (mobile.isBlank()) return app.getString(R.string.msgMobileEmpty)
        if (mobile.length != 10 || !mobile.matches(Regex("[6-9][0-9]{9}"))) {
            return app.getString(R.string.msgMobileInvalid)
        }
        return when (mode) {
            LoginMode.PASSWORD -> when {
                credential.isBlank() -> app.getString(R.string.msgPasswordEmpty)
                credential.length < 8 -> app.getString(R.string.msgPasswordShort)
                else -> null
            }
            LoginMode.MPIN -> when {
                credential.isBlank() -> app.getString(R.string.msgMpinEmpty)
                !credential.matches(Regex("[0-9]{4}")) -> app.getString(R.string.msgMpinInvalid)
                else -> null
            }
        }
    }
}
