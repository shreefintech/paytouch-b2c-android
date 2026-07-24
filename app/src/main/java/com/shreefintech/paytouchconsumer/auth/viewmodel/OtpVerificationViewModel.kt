package com.shreefintech.paytouchconsumer.auth.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.MessageResponse
import com.shreefintech.paytouchconsumer.utill.Utility
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class OtpVerificationViewModel : ViewModel() {

    fun sendOtp(
        context: Context,
        mobile: String,
        flowType: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(context)) {
            onError(context.getString(R.string.msgNoInternet))
            return
        }
        val call = if (flowType == Constant.FLOW_RESET_MPIN) {
            ApiClient.apiService.sendMpinOtp(mobile)
        } else {
            ApiClient.apiService.sendPasswordOtp(mobile)
        }
        enqueueOtp(context, call, onSuccess, onError)
    }

    fun verifyOtp(
        context: Context,
        mobile: String,
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
        val call = if (flowType == Constant.FLOW_RESET_MPIN) {
            ApiClient.apiService.verifyMpinOtp(mobile, otp)
        } else {
            ApiClient.apiService.verifyPasswordOtp(mobile, otp)
        }
        enqueueOtp(context, call, onSuccess, onError)
    }

    fun resendOtp(
        context: Context,
        mobile: String,
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
        val call = if (flowType == Constant.FLOW_RESET_MPIN) {
            ApiClient.apiService.sendMpinOtp(mobile)
        } else {
            ApiClient.apiService.sendPasswordOtp(mobile)
        }
        enqueueOtp(context, call, onSuccess, onError)
    }

    private fun enqueueOtp(
        context: Context,
        call: Call<MessageResponse>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        call.enqueue(object : Callback<MessageResponse> {
            override fun onResponse(call: Call<MessageResponse>, response: Response<MessageResponse>) {
                if (response.isSuccessful) onSuccess()
                else onError(ApiHelper.parseErrorMessage(context, response.code(), response.errorBody()?.string()))
            }

            override fun onFailure(call: Call<MessageResponse>, t: Throwable) {
                onError(t.localizedMessage ?: context.getString(R.string.err_generic))
            }
        })
    }
}
