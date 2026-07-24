package com.shreefintech.paytouchconsumer.auth.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.MessageResponse
import com.shreefintech.paytouchconsumer.utill.Utility
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ResetMpinViewModel : ViewModel() {

    fun changeMpin(
        context: Context,
        mobile: String,
        newMpin: String,
        onLoading: () -> Unit,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(context)) {
            onError(context.getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.resetMpin(mobile, newMpin, newMpin)
            .enqueue(object : Callback<MessageResponse> {
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
