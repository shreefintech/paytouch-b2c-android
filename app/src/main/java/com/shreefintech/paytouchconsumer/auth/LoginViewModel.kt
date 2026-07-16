package com.shreefintech.paytouchconsumer.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shreefintech.paytouchconsumer.enums.LoginMode
import com.shreefintech.paytouchconsumer.utill.Utility
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
            onError("No internet connection")
            return
        }
        onLoading()
        viewModelScope.launch {
            try {
                // API call wired here once the endpoint is ready
                withContext(Dispatchers.Main) { onSuccess() }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError(e.message ?: "Something went wrong") }
            }
        }
    }

    private fun validate(mobile: String, credential: String, mode: LoginMode): String? {
        if (mobile.isBlank()) return "Please enter your mobile number"
        if (mobile.length != 10 || !mobile.matches(Regex("[6-9][0-9]{9}"))) {
            return "Enter a valid 10-digit mobile number"
        }
        return when (mode) {
            LoginMode.PASSWORD -> when {
                credential.isBlank() -> "Please enter your password"
                credential.length < 6 -> "Password must be at least 6 characters"
                else -> null
            }
            LoginMode.MPIN -> when {
                credential.isBlank() -> "Please enter your MPIN"
                !credential.matches(Regex("[0-9]{4}")) -> "MPIN must be exactly 4 digits"
                else -> null
            }
        }
    }
}
