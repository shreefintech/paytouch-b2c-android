package com.shreefintech.paytouchconsumer.auth.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.utill.Utility
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OtpVerificationViewModel : ViewModel() {

    fun verifyOtp(
        context: Context,
        otp: String,
        flowType: String,
        onLoading: () -> Unit,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(context)) {
            onError(context.getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        viewModelScope.launch {
            try {
                // TODO(PAYTOUCH-487): wire verify-OTP API call
                withContext(Dispatchers.Main) { onSuccess() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.message ?: context.getString(R.string.errGeneric))
                }
            }
        }
    }

    fun resendOtp(
        context: Context,
        flowType: String,
        onLoading: () -> Unit,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(context)) {
            onError(context.getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        viewModelScope.launch {
            try {
                // TODO(PAYTOUCH-487): wire resend-OTP API call
                withContext(Dispatchers.Main) { onSuccess() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.message ?: context.getString(R.string.errGeneric))
                }
            }
        }
    }
}
