package com.shreefintech.paytouchconsumer.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shreefintech.paytouchconsumer.utill.Utility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ResetPasswordViewModel(application: Application) : AndroidViewModel(application) {

    fun changePassword(
        newPassword: String,
        onLoading: () -> Unit,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError("No internet connection")
            return
        }
        onLoading()
        viewModelScope.launch {
            try {
                // TODO(PAYTOUCH-487): wire change-password API call
                // val body = JsonObject().apply { addProperty("password", newPassword) }
                // val response = ApiClient.apiService.changePassword(body)
                // withContext(Dispatchers.Main) {
                //     if (response.isSuccessful && response.body()?.success == true) {
                //         onSuccess()
                //     } else {
                //         onError(ApiHelper.parseErrorMessage(response))
                //     }
                // }
                withContext(Dispatchers.Main) { onSuccess() }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError(e.message ?: "Something went wrong") }
            }
        }
    }
}
