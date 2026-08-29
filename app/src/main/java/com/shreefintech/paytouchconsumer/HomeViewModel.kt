package com.shreefintech.paytouchconsumer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.auth.MessageItem
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.bearerToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    fun logout(onLoading: () -> Unit, onComplete: () -> Unit, onError: (String) -> Unit) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getApplication<Application>().getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.logout(bearerToken())
            .enqueue(object : Callback<MessageItem> {
                override fun onResponse(call: Call<MessageItem>, response: Response<MessageItem>) {
                    // /logout response body only ever contains "message" (no "success" key),
                    // so checking body?.success == true would always be false. isSuccessful alone is correct here.
                    if (response.isSuccessful) {
                        onComplete()
                    } else {
                        onError(ApiHelper.parseErrorMessage(getApplication(), response.code(), response.errorBody()?.string()))
                    }
                }
                override fun onFailure(call: Call<MessageItem>, t: Throwable) {
                    onError(t.localizedMessage ?: getApplication<Application>().getString(R.string.errGeneric))
                }
            })
    }
}
