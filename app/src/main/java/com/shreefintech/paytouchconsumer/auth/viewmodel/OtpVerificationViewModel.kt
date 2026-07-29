package com.shreefintech.paytouchconsumer.auth.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.auth.MessageItem
import com.shreefintech.paytouchconsumer.utill.Utility
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class OtpVerificationViewModel : ViewModel() {

    fun sendOtp(
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
        enqueueMessage(context, call, onSuccess, onError)
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
        enqueueMessage(context, call, onSuccess, onError)
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
        enqueueMessage(context, call, onSuccess, onError)
    }

    private fun enqueueMessage(
        context: Context,
        call: Call<MessageItem>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        call.enqueue(object : Callback<MessageItem> {
            override fun onResponse(call: Call<MessageItem>, response: Response<MessageItem>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    onSuccess()
                } else {
                    onError(ApiHelper.parseErrorMessage(context, response.code(), response.errorBody()?.string()))
                }
            }

            override fun onFailure(call: Call<MessageItem>, t: Throwable) {
                onError(t.localizedMessage ?: context.getString(R.string.errGeneric))
            }
        })
    }
}
