package com.shreefintech.paytouchconsumer.auth.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shreefintech.paytouchconsumer.utill.Utility
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OtpVerificationViewModel(application: Application) : AndroidViewModel(application) {

    fun verifyOtp(
        otp: String,
        flowType: String,
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
                // TODO(PAYTOUCH-487): wire verify-OTP API call
                // val body = JsonObject().apply {
                //     addProperty("otp", otp)
                //     addProperty("flow_type", flowType)
                // }
                // val response = ApiClient.apiService.verifyOtp(body)
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

    fun resendOtp(
        flowType: String,
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
                // TODO(PAYTOUCH-487): wire resend-OTP API call
                // val body = JsonObject().apply { addProperty("flow_type", flowType) }
                // val response = ApiClient.apiService.resendOtp(body)
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