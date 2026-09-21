package com.shreefintech.paytouchconsumer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
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
            .enqueue(object : Callback<General<Any?>?> {
                override fun onResponse(call: Call<General<Any?>?>, response: Response<General<Any?>?>) {
                    if (response.isSuccessful) {
                        onComplete()
                    } else {
                        onError(ApiHelper.parseErrorMessage(getApplication(), response.code(), response.errorBody()?.string()))
                    }
                }

                override fun onFailure(call: Call<General<Any?>?>, t: Throwable) {
                    onError(t.localizedMessage ?: getApplication<Application>().getString(R.string.errGeneric))
                }
            })
    }
}
