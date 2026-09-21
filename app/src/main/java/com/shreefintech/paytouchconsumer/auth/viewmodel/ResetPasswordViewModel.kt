package com.shreefintech.paytouchconsumer.auth.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.utill.Utility
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ResetPasswordViewModel : ViewModel() {

    fun changePassword(
        context: Context,
        resetToken: String,
        newPassword: String,
        onLoading: () -> Unit,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(context)) {
            onError(context.getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.forgotCredentialReset(resetToken, "password", newPassword)
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
}
