package com.shreefintech.paytouchconsumer.auth.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.auth.ForgotCredentialVerifyItem
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
        val type = if (flowType == Constant.FLOW_RESET_MPIN) "mpin" else "password"
        ApiClient.apiService.forgotCredentialSendOtp(mobile, type)
            .enqueue(object : Callback<General<Any?>?> {
                override fun onResponse(call: Call<General<Any?>?>, response: Response<General<Any?>?>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        onSuccess()
                    } else {
                        onError(ApiHelper.parseErrorMessage(context, response.code(), response.errorBody()?.string()))
                    }
                }

                override fun onFailure(call: Call<General<Any?>?>, t: Throwable) {
                    onError(t.localizedMessage ?: context.getString(R.string.errGeneric))
                }
            })
    }

    fun verifyOtp(
        context: Context,
        mobile: String,
        otp: String,
        onLoading: () -> Unit,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(context)) {
            onError(context.getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.forgotCredentialVerifyOtp(mobile, otp)
            .enqueue(object : Callback<General<ForgotCredentialVerifyItem?>?> {
                override fun onResponse(
                    call: Call<General<ForgotCredentialVerifyItem?>?>,
                    response: Response<General<ForgotCredentialVerifyItem?>?>
                ) {
                    val resetToken = response.body()?.data?.resetToken
                    if (response.isSuccessful && resetToken != null) {
                        onSuccess(resetToken)
                    } else {
                        onError(ApiHelper.parseErrorMessage(context, response.code(), response.errorBody()?.string()))
                    }
                }

                override fun onFailure(call: Call<General<ForgotCredentialVerifyItem?>?>, t: Throwable) {
                    onError(t.localizedMessage ?: context.getString(R.string.errGeneric))
                }
            })
    }

    fun resendOtp(
        context: Context,
        mobile: String,
        flowType: String,
        onLoading: () -> Unit,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) = sendOtp(context, mobile, flowType, onLoading, onSuccess, onError)
}
