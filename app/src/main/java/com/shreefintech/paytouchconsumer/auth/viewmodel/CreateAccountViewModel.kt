package com.shreefintech.paytouchconsumer.auth.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shreefintech.paytouchconsumer.utill.Utility
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreateAccountViewModel(application: Application) : AndroidViewModel(application) {

    fun register(
        mobile: String,
        email: String,
        referralCode: String,
        password: String,
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
                // TODO(PAYTOUCH-487): wire create-account API call
                // val body = JsonObject().apply {
                //     addProperty("mobile", mobile)
                //     addProperty("email", email)
                //     addProperty("password", password)
                //     if (referralCode.isNotEmpty()) addProperty("referral_code", referralCode)
                // }
                // val response = ApiClient.apiService.register(body)
                // withContext(Dispatchers.Main) {
                //     if (response.isSuccessful && response.body()?.success == true) {
                //         onSuccess()
                //     } else {
                //         onError(ApiHelper.parseErrorMessage(response))
                //     }
                // }
                withContext(Dispatchers.Main) { onSuccess() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError(e.message ?: "Something went wrong") }
            }
        }
    }
}